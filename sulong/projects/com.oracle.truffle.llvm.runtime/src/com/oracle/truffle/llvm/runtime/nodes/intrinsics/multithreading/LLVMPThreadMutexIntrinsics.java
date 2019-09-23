/*
 * Copyright (c) 2019, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime.nodes.intrinsics.multithreading;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LLVMPThreadMutexIntrinsics {
    public static class Mutex {
        public enum MutexType {
            DEFAULT_NORMAL,
            ERRORCHECK,
            RECURSIVE
        }

        private int lockCount;
        private Thread owner;
        private final MutexType type;
        private final ConcurrentLinkedQueue<Thread> waitingThreads;

        public Mutex(MutexType type) {
            this.type = (type != null ? type : MutexType.DEFAULT_NORMAL);
            this.waitingThreads = new ConcurrentLinkedQueue<>();
        }

        public MutexType getType() {
            return this.type;
        }

        public boolean hasLock(Thread thread) {
            return this.owner == thread;
        }

        public boolean lock() {
            synchronized (this) {
                // for all types: if not locked currently, success
                if (this.lockCount == 0) {
                    this.lockCount = 1;
                    this.owner = Thread.currentThread();
                    return true;
                }
                // at this point we know the mutex is currently locked

                // for errorcheck type: if locked currently, return false
                if (this.type == MutexType.ERRORCHECK) {
                    return false;
                }
                // for recursive type: if locked currently by this thread, increase lock count, success
                if (this.type == MutexType.RECURSIVE && this.owner == Thread.currentThread()) {
                    this.lockCount++;
                    return true;
                }
                // for default_normal type: we will block and wait for our turn
                this.waitingThreads.add(Thread.currentThread());
            }
            UtilThread.sleepUntilInterrupt();
            // it's our turn to lock the mutex now
            // lockCount is still at 1
            // owner already set by unlock
            return true;
        }

        public boolean tryLock() {
            synchronized (this) {
                if (this.type == MutexType.DEFAULT_NORMAL || this.type == MutexType.ERRORCHECK) {
                    if (lockCount != 0) {
                        return false;
                    }
                    this.lockCount = 1;
                    this.owner = Thread.currentThread();
                    return true;
                }
                if (this.type == MutexType.RECURSIVE) {
                    if (this.owner != Thread.currentThread()) {
                        return false;
                    }
                    this.lockCount++;
                    return true;
                }
            }
            // not reachable
            return false;
        }

        public boolean unlock() {
            synchronized (this) {
                if (this.type == MutexType.ERRORCHECK || this.type == MutexType.RECURSIVE) {
                    // errorcheck and recursive mutex only allow lock / unlock from the same thread
                    if (this.owner != Thread.currentThread()) {
                        return false;
                    }
                    if (this.type == MutexType.RECURSIVE) {
                        // keep lock, but decrease count by one
                        if (this.lockCount > 1) {
                            lockCount--;
                            return true;
                        }
                    }
                }
                // at this point we know we can unlock
                if (!this.waitingThreads.isEmpty()) {
                    // already leave lockCount at 1
                    // and set new owner here in this synchronized block
                    // to keep consistent states outside of synchronized blocks
                    Thread next = this.waitingThreads.poll();
                    // wait for thread to sleep to be able to catch interrupts
                    while (next.getState() != Thread.State.TIMED_WAITING) {
                        // just keep spinning
                        // if a thread is in wait-queue, it will only take a moment for it to also sleep
                        // and to listen for interrupts
                    }
                    next.interrupt();
                    this.owner = next;
                    return true;
                }
                // if no one currently waiting just reset to default unlocked state
                this.lockCount = 0;
                this.owner = null;
                return true;
            }
        }
    }

    // nothing to do because mutex attrs are not saved in context
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadMutexattrDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0;
        }
    }

    // nothing to do because mutex attrs are not saved in context, they are just integers
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadMutexattrInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    @NodeChild(type = LLVMExpressionNode.class, value = "type")
    public abstract static class LLVMPThreadMutexattrSettype extends LLVMBuiltin {
        @Child
        LLVMStoreNode store = null;

        // [EINVAL] when no valid type
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr, int type, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            // pthread_mutexattr_t is just an int
            // so we just write the int-value type to the address attr points to
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctx.getLanguage().getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I32);
            }
            // check if valid type
            if (type != ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_DEFAULT) && type != ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_ERRORCHECK) && type != ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_NORMAL) && type != ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_RECURSIVE)) {
                return ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.EINVAL);
            }
            // store type in attr variable
            store.executeWithTarget(attr, type);
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            UtilAccessCollectionWithBoundary.remove(ctx.mutexStorage, mutex);
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadMutexInit extends LLVMBuiltin {
        @Child
        LLVMLoadNode read = null;

        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, LLVMPointer attr, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            if (read == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                read = ctx.getLanguage().getNodeFactory().createLoadNode(LLVMInteropType.ValueKind.I32);
            }
            // we can use the address of the pointer here, bc a mutex
            // must only work when using the original variable, not a copy
            // so the address may never change
            int attrValue = 0;
            if (!attr.isNull()) {
                attrValue = (int) read.executeWithTarget(attr);
            }
            Mutex.MutexType mutexType = Mutex.MutexType.DEFAULT_NORMAL;
            if (attrValue == ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_ERRORCHECK)) {
                mutexType = Mutex.MutexType.ERRORCHECK;
            } else if (attrValue == ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.PTHREAD_MUTEX_RECURSIVE)) {
                mutexType = Mutex.MutexType.RECURSIVE;
            }
            UtilAccessCollectionWithBoundary.put(ctx.mutexStorage, mutex, new Mutex(mutexType));
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexLock extends LLVMBuiltin {
        // [EDEADLK] if errorcheck mutex and already owned
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Mutex mutexObj = null;
            synchronized (mutex) {
                mutexObj = (Mutex) UtilAccessCollectionWithBoundary.get(ctx.mutexStorage, mutex);
                if (mutexObj == null) {
                    // mutex is not initialized
                    // but it works anyway on most implementations
                    // so we will make it work here too, just using default type
                    mutexObj = new Mutex(Mutex.MutexType.DEFAULT_NORMAL);
                    UtilAccessCollectionWithBoundary.put(ctx.mutexStorage, mutex, mutexObj);
                }
                // lock only returns false when mutex is errorcheck type and current thread already holds it
            }
            return mutexObj.lock() ? 0 : ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.EDEADLK);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexTrylock extends LLVMBuiltin {
        // [EBUSY] when already locked
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Mutex mutexObj = null;
            synchronized (mutex) {
                mutexObj = (Mutex) UtilAccessCollectionWithBoundary.get(ctx.mutexStorage, mutex);
                if (mutexObj == null) {
                    // mutex is not initialized
                    // but it works anyway on most implementations
                    // so we will make it work here too, just using default type
                    mutexObj = new Mutex(Mutex.MutexType.DEFAULT_NORMAL);
                    UtilAccessCollectionWithBoundary.put(ctx.mutexStorage, mutex, mutexObj);
                }
            }
            return mutexObj.tryLock() ? 0 : ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.EBUSY);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexUnlock extends LLVMBuiltin {
        // [EPERM] when errorcheck or recursive mutex and not owned by calling thread
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Mutex mutexObj = (Mutex) UtilAccessCollectionWithBoundary.get(ctx.mutexStorage, mutex);
            if (mutexObj == null) {
                return 0;
            }
            return mutexObj.unlock() ? 0 : ctx.pthreadConstants.getConstant(UtilCConstants.CConstant.EPERM);
        }
    }
}
