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

public class LLVMPThreadKeyIntrinsics {
    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    @NodeChild(type = LLVMExpressionNode.class, value = "destructor")
    public abstract static class LLVMPThreadKeyCreate extends LLVMBuiltin {
        @Child
        LLVMStoreNode store = null;

        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer key, LLVMPointer destructor, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            // create store node
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctxRef.get().getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I32);
            }
            // TODO: store the current value in key, and increment this value by 1 (atomic int in storage e. g.)
            store.executeWithTarget(key, 1);
            // TODO: add new key-value to key-storage-thing, will be hashmap(key-value->key-map) full of hashmap(thread-id->specific-value)
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    public abstract static class LLVMPThreadGetspecific extends LLVMBuiltin {
        // no relevant error code handling here
        @Specialization
        protected LLVMPointer doIntrinsic(VirtualFrame frame, int key, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            int i = 15;
            // TODO: get specific-value (which is a pointer) from my key-storage in context and return that
            return null;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    @NodeChild(type = LLVMExpressionNode.class, value = "value")
    public abstract static class LLVMPThreadSetspecific extends LLVMBuiltin {
        // [EINVAL] if key is not valid
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, int key, LLVMPointer value, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            // TODO: save key-value->specific-value in my storage for the calling thread
            return 0;
        }
    }
}
