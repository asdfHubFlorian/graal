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
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

public class LLVMPThreadThreadIntrinsics {
    @NodeChild(type = LLVMExpressionNode.class, value = "thread")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    @NodeChild(type = LLVMExpressionNode.class, value = "startRoutine")
    @NodeChild(type = LLVMExpressionNode.class, value = "arg")
    public abstract static class LLVMPThreadCreate extends LLVMBuiltin {
        @Child
        LLVMStoreNode store = null;

        // no relevant error codes here
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer thread, LLVMPointer attr, LLVMPointer startRoutine, LLVMPointer arg, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            // create store node
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctx.getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I64);
            }
            // create thread for execution of function
            UtilStartThread.InitStartOfNewThread init = new UtilStartThread.InitStartOfNewThread(startRoutine, arg, ctx, true);
            Thread t = ctx.getEnv().createThread(init);
            // store current id in thread variable
            store.executeWithTarget(thread, t.getId());
            // store thread with thread id in context
            UtilAccess.put(ctx.threadStorage, t.getId(), t);
            // start thread
            t.start();
            return 0;
        }
    }

    // no error codes defined here
    @NodeChild(type = LLVMExpressionNode.class, value = "retval")
    public abstract static class LLVMPThreadExit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, Object retval, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            // save return value in context for join calls
            UtilAccess.put(ctx.retValStorage, Thread.currentThread().getId(), retval);
            // stop this thread
            throw new PThreadExitException();
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "th")
    @NodeChild(type = LLVMExpressionNode.class, value = "threadReturn")
    public abstract static class LLVMPThreadJoin extends LLVMBuiltin {
        @Child LLVMStoreNode storeNode;

        // no error codes here
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, long th, LLVMPointer threadReturn, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            if (storeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                storeNode = ctx.getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.POINTER);
            }
            try {
                // join thread
                Thread thread = UtilAccess.get(ctx.threadStorage, th);
                if (thread == null) {
                    return 0;
                }
                thread.join();
                // get return value
                Object retVal = UtilAccess.get(ctx.retValStorage, th);
                // store return value at "threadReturn" pointer
                if (!threadReturn.isNull()) {
                    storeNode.executeWithTarget(threadReturn, retVal);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "onceControl")
    @NodeChild(type = LLVMExpressionNode.class, value = "initRoutine")
    public abstract static class LLVMPThreadOnce extends LLVMBuiltin {
        // no relevant error code handling here
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer onceControl, LLVMPointer initRoutine, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            synchronized (ctx) {
                if (ctx.onceStorage.contains(onceControl)) {
                    return 0;
                }
                ctx.onceStorage.add(onceControl);
            }
            UtilStartThread.InitStartOfNewThread init = new UtilStartThread.InitStartOfNewThread(initRoutine, null, ctx, false);
            init.run();
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "t1")
    @NodeChild(type = LLVMExpressionNode.class, value = "t2")
    public abstract static class LLVMPThreadEqual extends LLVMBuiltin {
        // no error codes here
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, long t1, long t2) {
            return t1 == t2 ? 1 : 0;
        }
    }

    public abstract static class LLVMPThreadSelf extends LLVMBuiltin {
        // no error codes here
        @Specialization
        protected long doIntrinsic(VirtualFrame frame) {
            return Thread.currentThread().getId();
        }
    }

    // just dummy function that does nothing, my implementation does not support attributes
    // but was needed for the execution of "pigz"
    // which was using an attribute to make the threads joinable, what they already are by default here
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadAttrDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0;
        }
    }

    // also just dummy function
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadAttrInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0; // TODO: maybe new storage for attrs and support some stuff like detach and priority?
        }
    }

    // also just dummy function
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    @NodeChild(type = LLVMExpressionNode.class, value = "detachstate")
    public abstract static class LLVMPThreadAttrSetdetachstate extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr, int detachstate) {
            return 0;
        }
    }
}
