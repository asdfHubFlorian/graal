package com.oracle.truffle.llvm.nodes.intrinsics.multithreading;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.LLVMNativeFunctions;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

import java.util.concurrent.ConcurrentHashMap;

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
            synchronized (ctxRef.get()) {
                store.executeWithTarget(key, ctxRef.get().curKeyVal);
                // add new key-value to key-storage-thing, will be hashmap(key-value->key-map) full of hashmap(thread-id->specific-value)
                // TODO: use util function with boundary
                ctxRef.get().keyStorage.put(ctxRef.get().curKeyVal, new ConcurrentHashMap<>());
                ctxRef.get().destructorStorage.put(ctxRef.get().curKeyVal, destructor);
                ctxRef.get().curKeyVal++;
            }
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    public abstract static class LLVMPThreadGetspecific extends LLVMBuiltin {
        // no relevant error code handling here
        @Specialization
        protected LLVMPointer doIntrinsic(VirtualFrame frame, int key, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            if (ctxRef.get().keyStorage.containsKey(key) && ctxRef.get().keyStorage.get(key).containsKey(Thread.currentThread().getId())) {
                return ctxRef.get().keyStorage.get(key).get(Thread.currentThread().getId());
            }
            return LLVMNativePointer.createNull();
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "key")
    @NodeChild(type = LLVMExpressionNode.class, value = "value")
    public abstract static class LLVMPThreadSetspecific extends LLVMBuiltin {
        // [EINVAL] if key is not valid
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, int key, LLVMPointer value, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            if (!ctxRef.get().keyStorage.containsKey(key)) {
                return CConstants.getEINVAL();
            }
            ctxRef.get().keyStorage.get(key).put(Thread.currentThread().getId(), value);
            return 0;
        }
    }
}
