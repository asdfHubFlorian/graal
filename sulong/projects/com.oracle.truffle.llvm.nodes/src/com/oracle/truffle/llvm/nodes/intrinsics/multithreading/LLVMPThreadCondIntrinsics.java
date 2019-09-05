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

// does differ from the pthreads cond vars, so if you want to use cond vars disable those intrinsics
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

import java.util.concurrent.locks.Condition;

public class LLVMPThreadCondIntrinsics {
    public static class Cond {
        public enum Type {
            DEFAULT
        }

        private Condition condition;
        private LLVMPThreadMutexIntrinsics.Mutex curMutex;
        private final Type type;

        public Cond() {
            this.condition = null;
            this.curMutex = null;
            this.type = Type.DEFAULT;
        }

        // condition differs from pthread cond var
        // cond var can broadcast and signal even if mutex is not owned
        // condition can't, it leads to a IllegalMonitorStateException
        public void broadcast() {
            // if (!curMutex.internLock.isHeldByCurrentThread()) {
                // return;
            // }
            if (condition != null) {
                condition.signalAll();
            }
        }

        // condition differs from pthread cond var
        // cond var can broadcast and signal even if mutex is not owned
        // condition can't, it leads to a IllegalMonitorStateException
        public void signal() {
            // if (curMutex != null && !curMutex.internLock.isHeldByCurrentThread()) {
                // return;
            // }
            if (condition != null) {
                condition.signal();
            }
        }

        public boolean cWait(LLVMPThreadMutexIntrinsics.Mutex mutex) {
            if (this.curMutex == null) {
                this.curMutex = mutex;
                this.condition = mutex.internLock.newCondition();
            } else if (this.curMutex != mutex) {
                this.curMutex = mutex;
                this.condition = mutex.internLock.newCondition();
            }
            if (!this.curMutex.internLock.isHeldByCurrentThread()) {
                return false;
            }
            try {
                this.condition.await();
            } catch (InterruptedException e) {
            }
            return true;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    public abstract static class LLVMPThreadCondDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            UtilAccess.remove(ctx.condStorage, cond);
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadCondInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, LLVMPointer attr, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            // we can use the address of the pointer here, because a cond
            // must only work when using the original variable, not a copy
            // so the address may never change
            // cond is a pointer, where equal compares the address
            UtilAccess.put(ctx.condStorage, cond, new Cond());
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    public abstract static class LLVMPThreadCondBroadcast extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Cond condObj = (Cond) UtilAccess.get(ctx.condStorage, cond);
            if (condObj == null) {
                return 0; // cannot broadcast to cond that does not exist yet, but no errors specified in spec
            }
            condObj.broadcast();
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    public abstract static class LLVMPThreadCondSignal extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Cond condObj = (Cond) UtilAccess.get(ctx.condStorage, cond);
            if (condObj == null) {
                return 0; // cannot signal to cond that does not exist yet, but no errors specified in spec
            }
            condObj.signal();
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    @NodeChild(type = LLVMExpressionNode.class, value = "mutex")
    public abstract static class LLVMPThreadCondWait extends LLVMBuiltin {
        // EPERM when mutex is not currently hold
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            Cond condObj = (Cond) UtilAccess.get(ctx.condStorage, cond);
            LLVMPThreadMutexIntrinsics.Mutex mutexObj = (LLVMPThreadMutexIntrinsics.Mutex) UtilAccess.get(ctx.mutexStorage, mutex);
            if (condObj == null) {
                // init and then wait
                condObj = new Cond();
                UtilAccess.put(ctx.condStorage, cond, condObj);
            }
            if (mutexObj == null) {
                mutexObj = new LLVMPThreadMutexIntrinsics.Mutex(LLVMPThreadMutexIntrinsics.Mutex.MutexType.DEFAULT_NORMAL);
                UtilAccess.put(ctx.mutexStorage, mutex, mutexObj);
            }
            return condObj.cWait(mutexObj) ? 0 : ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EPERM);
        }
    }
}
