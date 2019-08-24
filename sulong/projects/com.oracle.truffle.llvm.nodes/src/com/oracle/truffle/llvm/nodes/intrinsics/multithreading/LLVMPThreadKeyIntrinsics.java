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
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.util.PThreadCConstants;

import java.util.concurrent.ConcurrentHashMap;

public class LLVMPThreadKeyIntrinsics {
    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    @NodeChild(type = LLVMExpressionNode.class, value = "destructor")
    public abstract static class LLVMPThreadKeyCreate extends LLVMBuiltin {
        @Child
        LLVMStoreNode store = null;

        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer key, LLVMPointer destructor, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            // create store node
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctx.getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I32);
            }
            synchronized (ctx) {
                store.executeWithTarget(key, ctx.curKeyVal + 1);
                // add new key-value to key-storage, which is a hashmap(key-value->hashmap(thread-id->specific-value))
                UtilAccess.put(ctx.keyStorage, ctx.curKeyVal + 1, new ConcurrentHashMap<>());
                UtilAccess.put(ctx.destructorStorage, ctx.curKeyVal + 1, destructor);
                // when a thread exits it loops up to curKeyVal for calling all destructors
                // so before we increment to x we want to be sure that there are already destructors and keys for that value in the context
                ctx.curKeyVal++;
            }
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    public abstract static class LLVMPThreadGetspecific extends LLVMBuiltin {
        // no relevant error code handling here
        @Specialization
        protected LLVMPointer doIntrinsic(VirtualFrame frame, int key, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            if (ctx.keyStorage.containsKey(key) && ctx.keyStorage.get(key).containsKey(Thread.currentThread().getId())) {
                return UtilAccess.get(UtilAccess.get(ctx.keyStorage, key), Thread.currentThread().getId());
            }
            return LLVMNativePointer.createNull();
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    @NodeChild(type = LLVMExpressionNode.class, value = "value")
    public abstract static class LLVMPThreadSetspecific extends LLVMBuiltin {
        // [EINVAL] if key is not valid
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, int key, LLVMPointer value, @CachedContext(LLVMLanguage.class) LLVMContext ctx) {
            if (!ctx.keyStorage.containsKey(key)) {
                return ctx.pthreadConstants.getConstant(PThreadCConstants.CConstant.EINVAL);
            }
            UtilAccess.put(UtilAccess.get(ctx.keyStorage, key), Thread.currentThread().getId(), value);
            return 0;
        }
    }
}
