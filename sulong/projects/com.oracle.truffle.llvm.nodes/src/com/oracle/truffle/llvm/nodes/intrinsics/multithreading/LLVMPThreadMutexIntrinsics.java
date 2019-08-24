/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.intrinsics.multithreading;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.util.PThreadCConstants;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class LLVMPThreadMutexIntrinsics {
    public static class Mutex {
        public enum MutexType {
            DEFAULT_NORMAL,
            ERRORCHECK,
            RECURSIVE
        }

        protected ReentrantLock internLock;
        private final MutexType type;
        private final ConcurrentLinkedQueue<Thread> waitingThreads;

        public Mutex(MutexType type) {
            this.internLock = new ReentrantLock();
            this.waitingThreads = new ConcurrentLinkedQueue<>();
            if (type != null) {
                this.type = type;
            } else {
                this.type = MutexType.DEFAULT_NORMAL;
            }
        }

        public MutexType getType() {
            return this.type;
        }

        // TODO: boundary to all collection stuff
        public boolean lock() {
            if (this.internLock.isHeldByCurrentThread()) {
                if (this.type == MutexType.DEFAULT_NORMAL) {
                    // deadlock until another thread unlocks it
                    // is not defined in spec, but the native behavior
                    while (true) {
                        try {
                            if (!waitingThreads.contains(Thread.currentThread())) {
                                waitingThreads.add(Thread.currentThread());
                            }
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                if (this.type == MutexType.ERRORCHECK) {
                    return false;
                }
            }
            // default_normal mutexes should be able to be unlocked by another thread
            // javas reentrant lock class does not allow this though (illegal monitor state exception)
            // so when unlock from not owning thread comes to a default_normal mutex
            // the intern lock gets replaced by a new unlocked one, and the waiting threads get interrupted
            // so they will call "lock" on the new intern lock

            // this is not in the spec, but in the native implementation
            // and this behavior is needed for running the benchmark "threadring" form the shootout suite

            if (this.type == MutexType.DEFAULT_NORMAL) {
                while (true) {
                    try {
                        if (!waitingThreads.contains(Thread.currentThread())) {
                            waitingThreads.add(Thread.currentThread());
                        }
                        internLock.lockInterruptibly();
                    } catch (InterruptedException e) {
                        continue;
                    }
                    waitingThreads.remove(Thread.currentThread());
                    return true;
                }
            }
            internLock.lock();
            return true;
        }

        public boolean tryLock() {
            if (this.type == MutexType.RECURSIVE) {
                return internLock.tryLock();
            }
            // if it's not a recursive mutex and currently owned trylock shall return false
            if (internLock.isHeldByCurrentThread()) {
                return false;
            }
            // if it's not currently owned we can just call trylock
            return internLock.tryLock();
        }

        public boolean unlock() {
            if (!internLock.isHeldByCurrentThread()) {
                // in spec undefined, my native implementation unlocks and returns 0 when unlocking not-locked / not-owned default_normal mutexes
                if (this.type == MutexType.DEFAULT_NORMAL) {
                    internLock = new ReentrantLock();
                    for (Thread t : this.waitingThreads) {
                        t.interrupt();
                    }
                    return true;
                }
                return false;
            }
            internLock.unlock();
            return true;
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
            // create store node
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctx.getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I32);
            }
            // check if valid type
            if (type != ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_DEFAULT) && type != ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_ERRORCHECK) && type != ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_NORMAL) && type != ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_RECURSIVE)) {
                return ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EINVAL);
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
            UtilAccess.remove(ctx.mutexStorage, mutex);
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
                read = ctx.getNodeFactory().createLoadNode(LLVMInteropType.ValueKind.I32);
            }
            // we can use the address of the pointer here, bc a mutex
            // must only work when using the original variable, not a copy
            // so the address may never change
            int attrValue = 0;
            if (!attr.isNull()) {
                attrValue = (int) read.executeWithTarget(attr);
            }
            Mutex.MutexType mutexType = Mutex.MutexType.DEFAULT_NORMAL;
            if (attrValue == ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_ERRORCHECK)) {
                mutexType = Mutex.MutexType.ERRORCHECK;
            } else if (attrValue == ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.PTHREAD_MUTEX_RECURSIVE)) {
                mutexType = Mutex.MutexType.RECURSIVE;
            }
            UtilAccess.put(ctx.mutexStorage, mutex, new Mutex(mutexType));
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
                mutexObj = (Mutex) UtilAccess.get(ctx.mutexStorage, mutex);
                if (mutexObj == null) {
                    // mutex is not initialized
                    // but it works anyway on most implementations
                    // so we will make it work here too, just using default type
                    mutexObj = new Mutex(Mutex.MutexType.DEFAULT_NORMAL);
                    UtilAccess.put(ctx.mutexStorage, mutex, mutexObj);
                }
                // lock only returns false when mutex is errorcheck type and current thread already holds it
            }
            return mutexObj.lock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EDEADLK);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexTrylock extends LLVMBuiltin {
        // [EBUSY] when already locked
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Mutex mutexObj = null;
            synchronized (mutex) {
                mutexObj = (Mutex) UtilAccess.get(ctx.mutexStorage, mutex);
                if (mutexObj == null) {
                    // mutex is not initialized
                    // but it works anyway on most implementations
                    // so we will make it work here too, just using default type
                    mutexObj = new Mutex(Mutex.MutexType.DEFAULT_NORMAL);
                    UtilAccess.put(ctx.mutexStorage, mutex, mutexObj);
                }
            }
            return mutexObj.tryLock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EBUSY);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadMutexUnlock extends LLVMBuiltin {
        // [EPERM] when errorcheck or recursive mutex and not owned by calling thread
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Mutex mutexObj = (Mutex) UtilAccess.get(ctx.mutexStorage, mutex);
            if (mutexObj == null) {
                return 0;
            }
            return mutexObj.unlock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EPERM);
        }
    }
}
