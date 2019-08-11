package com.oracle.truffle.llvm.nodes.intrinsics.multithreading;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMExitException;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;

public class UtilStartThread {
    static class InitStartOfNewThread implements Runnable {
        private boolean isThread;
        private Object startRoutine;
        private Object arg;
        private TruffleLanguage.ContextReference<LLVMContext> ctxRef;
        private boolean exit;
        private LLVMExitException exitException;

        public InitStartOfNewThread(Object startRoutine, Object arg, TruffleLanguage.ContextReference<LLVMContext> ctxRef, boolean isThread) {
            this.startRoutine = startRoutine;
            this.arg = arg;
            this.ctxRef = ctxRef;
            this.exit = false;
            this.isThread = isThread;
        }

        @Override
        public void run() {
            // synchronized because i once got a null pointer exception from "Object retVal = ctxRef.get().pthreadCallTarget.call(startRoutine, arg);"
            synchronized (ctxRef.get()) {
                if (ctxRef.get().pthreadCallTarget == null) {
                    ctxRef.get().pthreadCallTarget = Truffle.getRuntime().createCallTarget(new RunNewThreadNode(LLVMLanguage.getLanguage()));
                }
            }
            // pthread_exit throws a control flow exception to stop the thread
            try {
                Object retVal = ctxRef.get().pthreadCallTarget.call(startRoutine, arg);
                // no null values in concurrent hash map allowed
                if (retVal == null) {
                    retVal = LLVMNativePointer.createNull();
                }
                UtilAccess.putLongObj(ctxRef.get().retValStorage, Thread.currentThread().getId(), retVal);
            } catch (PThreadExitException e) {
                // return value is written to retval storage in exit function before it throws this exception
            } catch (LLVMExitException e) {
                exit = true;
                exitException = e;
                // ctxRef.get().shutdownThreads();
                System.exit(e.getReturnCode());
            } finally {
                // call destructors from key create
                if (this.isThread) {
                    for (int key = 1; key <= ctxRef.get().curKeyVal; key++) {
                        LLVMPointer destructor;
                        if ((destructor = ctxRef.get().destructorStorage.get(key)) != null) {
                            Object keyVal = ctxRef.get().keyStorage.get(key).get(Thread.currentThread().getId());
                            if (keyVal != null) {
                                try {
                                    LLVMPointer keyValPointer = LLVMPointer.cast(keyVal);
                                    if (keyValPointer.isNull()) {
                                        continue;
                                    }
                                } catch (Exception e) {
                                }
                                ctxRef.get().keyStorage.get(key).remove(Thread.currentThread());
                                new InitStartOfNewThread(destructor, keyVal, this.ctxRef, false).run();
                            }
                        }
                    }
                }
            }
        }

        public boolean exitThrown() {
            return exit;
        }

        public LLVMExitException getExitException() {
            return exitException;
        }
    }

    static final class MyArgNode extends LLVMExpressionNode {
        private final FrameSlot slot;

        private MyArgNode(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return frame.getValue(slot);
        }
    }

    private static class RunNewThreadNode extends RootNode {
        @Child
        LLVMExpressionNode callNode = null;

        @CompilerDirectives.CompilationFinal
        FrameSlot functionSlot = null;

        @CompilerDirectives.CompilationFinal
        FrameSlot argSlot = null;

        @CompilerDirectives.CompilationFinal
        FrameSlot spSlot = null;

        private LLVMLanguage language;

        protected RunNewThreadNode(LLVMLanguage language) {
            super(language);
            this.language = language;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            LLVMStack.StackPointer sp = language.getContextReference().get().getThreadingStack().getStack().newFrame();
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                functionSlot = frame.getFrameDescriptor().findOrAddFrameSlot("function");
                argSlot = frame.getFrameDescriptor().findOrAddFrameSlot("arg");
                spSlot = frame.getFrameDescriptor().findOrAddFrameSlot("sp");

                callNode = getCurrentContext(LLVMLanguage.class).getNodeFactory().createFunctionCall(
                        new MyArgNode(functionSlot),
                        new LLVMExpressionNode[] {
                                new MyArgNode(spSlot), new MyArgNode(argSlot)
                        },
                        new FunctionType(PointerType.VOID, new Type[] {}, false),
                        null
                );
            }
            // copy arguments to frame
            final Object[] arguments = frame.getArguments();
            Object function = arguments[0];
            Object arg = arguments[1];
            frame.setObject(functionSlot, function);
            frame.setObject(argSlot, arg);
            frame.setObject(spSlot, sp);
            // execute it
            return callNode.executeGeneric(frame);
        }
    }
}
