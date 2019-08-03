package com.oracle.truffle.llvm.nodes.intrinsics.multithreading;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMBuiltin;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;

import javax.rmi.CORBA.Util;

public class LLVMPThreadThreadIntrinsics {
    @NodeChild(type = LLVMExpressionNode.class, value = "thread")
    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    @NodeChild(type = LLVMExpressionNode.class, value = "startRoutine")
    @NodeChild(type = LLVMExpressionNode.class, value = "arg")
    public abstract static class LLVMPThreadCreate extends LLVMBuiltin {
        @Child
        LLVMStoreNode store = null;

        // no relevant error code handling here
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer thread, LLVMPointer attr, LLVMPointer startRoutine, LLVMPointer arg, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            // create store node
            if (store == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                store = ctxRef.get().getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.I64);
            }
            // create thread for execution of function
            UtilStartThread.InitStartOfNewThread init = new UtilStartThread.InitStartOfNewThread(startRoutine, arg, ctxRef);
            Thread t = ctxRef.get().getEnv().createThread(init);
            // store cur id in thread var
            store.executeWithTarget(thread, t.getId());
            // store thread with thread id in context
            UtilAccess.putLongThread(ctxRef.get().threadStorage, t.getId(), t);
            // start thread
            t.start();
            return 0;
        }
    }

    // no error codes defined here
    @NodeChild(type = LLVMExpressionNode.class, value = "retval")
    public abstract static class LLVMPThreadExit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, Object retval, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            // save return value in context for join calls
            UtilAccess.putLongObj(ctxRef.get().retValStorage, Thread.currentThread().getId(), retval);
            // stop this thread
            throw new PThreadExitException();
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "th")
    @NodeChild(type = LLVMExpressionNode.class, value = "threadReturn")
    public abstract static class LLVMPThreadJoin extends LLVMBuiltin {
        @Child LLVMStoreNode storeNode;

        @Specialization
        protected int doIntrinsic(VirtualFrame frame, long th, LLVMPointer threadReturn, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            if (storeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                storeNode = ctxRef.get().getNodeFactory().createStoreNode(LLVMInteropType.ValueKind.POINTER);
            }
            try {
                // join thread
                Thread thread = UtilAccess.getLongThread(ctxRef.get().threadStorage, th);
                if (thread == null) {
                    // TODO: error code handling
                    return 5;
                }
                thread.join();
                // get return value
                Object retVal = UtilAccess.getLongObj(ctxRef.get().retValStorage, th);
                // store return value at ptr
                if (!threadReturn.isNull() && retVal != null) {
                    storeNode.executeWithTarget(threadReturn, retVal);
                }
            } catch (Exception e) {
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
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer onceControl, LLVMPointer initRoutine, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> ctxRef) {
            synchronized (ctxRef.get()) {
                if (ctxRef.get().onceStorage.containsKey(onceControl)) {
                    return 0;
                }
                ctxRef.get().onceStorage.put(onceControl, new Object());
            }
            UtilStartThread.InitStartOfNewThread init = new UtilStartThread.InitStartOfNewThread(initRoutine, null, ctxRef);
            init.run();
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "t1")
    @NodeChild(type = LLVMExpressionNode.class, value = "t2")
    public abstract static class LLVMPThreadEqual extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, long t1, long t2) {
            return t1 == t2 ? 0 : 1;
        }
    }

    public abstract static class LLVMPThreadSelf extends LLVMBuiltin {
        @Specialization
        protected long doIntrinsic(VirtualFrame frame) {
            return Thread.currentThread().getId();
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadAttrDestroy extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0;
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    public abstract static class LLVMPThreadAttrInit extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr) {
            return 0; // TODO: maybe new storage for attrs and support some stuff like detach and priority?
        }
    }

    @NodeChild(type = LLVMExpressionNode.class, value = "attr")
    @NodeChild(type = LLVMExpressionNode.class, value = "detachstate")
    public abstract static class LLVMPThreadAttrSetdetachstate extends LLVMBuiltin {
        @Specialization
        protected int doIntrinsic(VirtualFrame frame, LLVMPointer attr, int detachstate) {
            return 0;
        }
    }
}
