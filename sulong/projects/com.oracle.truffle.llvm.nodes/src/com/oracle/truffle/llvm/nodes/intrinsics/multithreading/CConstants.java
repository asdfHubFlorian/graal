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

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;

public class CConstants {
    private static LLVMContext ctx;
    private static int EBUSY;
    private static boolean loadedEBUSY = false;
    private static int EINVAL;
    private static boolean loadedEINVAL = false;
    private static int EDEADLK;
    private static boolean loadedEDEADLK = false;
    private static int EPERM;
    private static boolean loadedEPERM = false;
    private static int PTHREADMUTEXDEFAULT;
    private static boolean loadedPTHREADMUTEXDEFAULT = false;
    private static int PTHREADMUTEXERRORCHECK;
    private static boolean loadedPTHREADMUTEXERRORCHECK = false;
    private static int PTHREADMUTEXNORMAL;
    private static boolean loadedPTHREADMUTEXNORMAL = false;
    private static int PTHREADMUTEXRECURSIVE;
    private static boolean loadedPTHREADMUTEXRECURSIVE = false;


    static int getEBUSY() {
        if (loadedEBUSY) {
            return EBUSY;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEBUSY").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EBUSY = (int) callTarget.call(sp1);
        loadedEBUSY = true;
        return EBUSY;
    }

    static int getEINVAL() {
        if (loadedEINVAL) {
            return EINVAL;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEINVAL").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EINVAL = (int) callTarget.call(sp1);
        loadedEINVAL = true;
        return EINVAL;
    }

    static int getEDEADLK() {
        if (loadedEDEADLK) {
            return EDEADLK;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEDEADLK").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EDEADLK = (int) callTarget.call(sp1);
        loadedEDEADLK = true;
        return EDEADLK;
    }

    static int getEPERM() {
        if (loadedEPERM) {
            return EPERM;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEPERM").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EPERM = (int) callTarget.call(sp1);
        loadedEPERM = true;
        return EPERM;
    }

    static int getPTHREADMUTEXDEFAULT() {
        if (loadedPTHREADMUTEXDEFAULT) {
            return PTHREADMUTEXDEFAULT;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_DEFAULT").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXDEFAULT = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXDEFAULT = true;
        return PTHREADMUTEXDEFAULT;
    }

    static int getPTHREADMUTEXERRORCHECK() {
        if (loadedPTHREADMUTEXERRORCHECK) {
            return PTHREADMUTEXERRORCHECK;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_ERRORCHECK").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXERRORCHECK = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXERRORCHECK = true;
        return PTHREADMUTEXERRORCHECK;
    }

    static int getPTHREADMUTEXNORMAL() {
        if (loadedPTHREADMUTEXNORMAL) {
            return PTHREADMUTEXNORMAL;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_NORMAL").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXNORMAL = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXNORMAL = true;
        return PTHREADMUTEXNORMAL;
    }

    static int getPTHREADMUTEXRECURSIVE() {
        if (loadedPTHREADMUTEXRECURSIVE) {
            return PTHREADMUTEXRECURSIVE;
        }
        if (ctx == null) {
            ctx = LLVMLanguage.getLLVMContextReference().get();
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_RECURSIVE").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXRECURSIVE = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXRECURSIVE = true;
        return PTHREADMUTEXRECURSIVE;
    }
}
