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

        public void broadcast() {
            if (condition != null) {
                condition.signalAll();
            }
        }

        public void signal() {
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
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            UtilAccess.removeLLVMPointerObj(ctxRef.get().condStorage, cond);
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadCondInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, LLVMPointer attr, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            // we can use the address of the pointer here, bc a cond
            // must only work when using the original variable, not a copy
            // so the address may never change
            // cond is pointer, where equal works on address
            Object condObj = UtilAccess.getLLVMPointerObj(ctxRef.get().condStorage, cond);
            if (condObj == null) {
                UtilAccess.putLLVMPointerObj(ctxRef.get().condStorage, cond, new Cond());
            }
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "cond")
    public abstract static class LLVMPThreadCondBroadcast extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            Cond condObj = (Cond) UtilAccess.getLLVMPointerObj(ctxRef.get().condStorage, cond);
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
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            Cond condObj = (Cond) UtilAccess.getLLVMPointerObj(ctxRef.get().condStorage, cond);
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
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer cond, LLVMPointer mutex, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            Cond condObj = (Cond) UtilAccess.getLLVMPointerObj(ctxRef.get().condStorage, cond);
            LLVMPThreadMutexIntrinsics.Mutex mutexObj = (LLVMPThreadMutexIntrinsics.Mutex) UtilAccess.getLLVMPointerObj(ctxRef.get().mutexStorage, mutex);
            if (condObj == null) {
                // init and then wait
                condObj = new Cond();
                UtilAccess.putLLVMPointerObj(ctxRef.get().condStorage, cond, condObj);
            }
            return condObj.cWait(mutexObj) ? 0 : CConstants.getEPERM();
        }
    }
}
