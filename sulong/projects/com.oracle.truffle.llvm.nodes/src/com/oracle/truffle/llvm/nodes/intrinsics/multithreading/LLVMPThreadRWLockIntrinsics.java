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

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.util.PThreadCConstants;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LLVMPThreadRWLockIntrinsics {
    public static class RWLock {
        private final ReadWriteLock readWriteLock;
        private Thread writeOwner;

        public RWLock() {
            this.readWriteLock = new ReentrantReadWriteLock();
        }

        public void readLock() {
            try {
                this.readWriteLock.readLock().lockInterruptibly();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean tryReadLock() {
            return this.readWriteLock.readLock().tryLock();
        }

        public boolean writeLock() {
            if (this.writeOwner == Thread.currentThread()) {
                return false;
            }
            try {
                this.readWriteLock.writeLock().lockInterruptibly();
                this.writeOwner = Thread.currentThread();
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        public boolean tryWriteLock() {
            return this.readWriteLock.writeLock().tryLock();
        }

        public void unlock() {
            // exception handling, because in pthread you unlock read and write at once
            // but here it's two separate locks
            // so if you only have a readlock and want to unlock, the writeLock.unlock() will fail with a exception and vice versa
            try {
                this.readWriteLock.readLock().unlock();
            } catch (Exception e) {
            }
            try {
                this.readWriteLock.writeLock().unlock();
                this.writeOwner = null;
            } catch (Exception e) {
            }
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            UtilAccess.remove(ctx.rwlockStorage, rwlock);
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadRWLockInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, LLVMPointer attr, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            UtilAccess.put(ctx.rwlockStorage, rwlock, new RWLock());
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockRdlock extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            RWLock rwlockObj = (RWLock) UtilAccess.get(ctx.rwlockStorage, rwlock);
            if (rwlockObj == null) {
                // rwlock is not initialized
                // but it works anyway on most implementations
                // so we init it here
                rwlockObj = new RWLock();
                UtilAccess.put(ctx.rwlockStorage, rwlock, rwlockObj);
            }
            rwlockObj.readLock();
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockTryrdlock extends LLVMBuiltin {
        // [EBUSY] when not lock not possible now
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            RWLock rwlockObj = (RWLock) UtilAccess.get(ctx.rwlockStorage, rwlock);
            if (rwlockObj == null) {
                // rwlock is not initialized
                // but it works anyway on most implementations
                // so we init it here
                rwlockObj = new RWLock();
                UtilAccess.put(ctx.rwlockStorage, rwlock, rwlockObj);
            }
            return rwlockObj.tryReadLock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EBUSY);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockWrlock extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            RWLock rwlockObj = (RWLock) UtilAccess.get(ctx.rwlockStorage, rwlock);
            if (rwlockObj == null) {
                // rwlock is not initialized
                // but it works anyway on most implementations
                // so we init it here
                rwlockObj = new RWLock();
                UtilAccess.put(ctx.rwlockStorage, rwlock, rwlockObj);
            }
            return rwlockObj.writeLock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EDEADLK);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockTrywrlock extends LLVMBuiltin {
        // [EBUSY] when not lock not possible now
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            RWLock rwlockObj = (RWLock) UtilAccess.get(ctx.rwlockStorage, rwlock);
            if (rwlockObj == null) {
                // rwlock is not initialized
                // but it works anyway on most implementations
                // so we init it here
                rwlockObj = new RWLock();
                UtilAccess.put(ctx.rwlockStorage, rwlock, rwlockObj);
            }
            return rwlockObj.tryWriteLock() ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EBUSY);
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "rwlock")
    public abstract static class LLVMPThreadRWLockUnlock extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer rwlock, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            RWLock rwlockObj = (RWLock) UtilAccess.get(ctx.rwlockStorage, rwlock);
            if (rwlockObj == null) {
                // rwlock is not initialized, but no error code specified
                return 0;
            }
            rwlockObj.unlock();
            return 0;
        }
    }
}
