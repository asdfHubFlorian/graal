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
package com.oracle.truffle.llvm.runtime.util;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;

public class PThreadCConstants {
    private final LLVMContext ctx;
    private int EBUSY;
    private boolean loadedEBUSY = false;
    private int EINVAL;
    private boolean loadedEINVAL = false;
    private int EDEADLK;
    private boolean loadedEDEADLK = false;
    private int EPERM;
    private boolean loadedEPERM = false;
    private int PTHREADMUTEXDEFAULT;
    private boolean loadedPTHREADMUTEXDEFAULT = false;
    private int PTHREADMUTEXERRORCHECK;
    private boolean loadedPTHREADMUTEXERRORCHECK = false;
    private int PTHREADMUTEXNORMAL;
    private boolean loadedPTHREADMUTEXNORMAL = false;
    private int PTHREADMUTEXRECURSIVE;
    private boolean loadedPTHREADMUTEXRECURSIVE = false;

    public PThreadCConstants(LLVMContext ctx) {
        this.ctx = ctx;
    }

    public int getEBUSY() {
        if (loadedEBUSY) {
            return EBUSY;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEBUSY").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EBUSY = (int) callTarget.call(sp1);
        loadedEBUSY = true;
        return EBUSY;
    }

    public int getEINVAL() {
        if (loadedEINVAL) {
            return EINVAL;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEINVAL").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EINVAL = (int) callTarget.call(sp1);
        loadedEINVAL = true;
        return EINVAL;
    }

    public int getEDEADLK() {
        if (loadedEDEADLK) {
            return EDEADLK;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEDEADLK").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EDEADLK = (int) callTarget.call(sp1);
        loadedEDEADLK = true;
        return EDEADLK;
    }

    public int getEPERM() {
        if (loadedEPERM) {
            return EPERM;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getEPERM").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        EPERM = (int) callTarget.call(sp1);
        loadedEPERM = true;
        return EPERM;
    }

    public int getPTHREADMUTEXDEFAULT() {
        if (loadedPTHREADMUTEXDEFAULT) {
            return PTHREADMUTEXDEFAULT;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_DEFAULT").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXDEFAULT = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXDEFAULT = true;
        return PTHREADMUTEXDEFAULT;
    }

    public int getPTHREADMUTEXERRORCHECK() {
        if (loadedPTHREADMUTEXERRORCHECK) {
            return PTHREADMUTEXERRORCHECK;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_ERRORCHECK").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXERRORCHECK = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXERRORCHECK = true;
        return PTHREADMUTEXERRORCHECK;
    }

    public int getPTHREADMUTEXNORMAL() {
        if (loadedPTHREADMUTEXNORMAL) {
            return PTHREADMUTEXNORMAL;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_NORMAL").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXNORMAL = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXNORMAL = true;
        return PTHREADMUTEXNORMAL;
    }

    public int getPTHREADMUTEXRECURSIVE() {
        if (loadedPTHREADMUTEXRECURSIVE) {
            return PTHREADMUTEXRECURSIVE;
        }
        RootCallTarget callTarget = ctx.getGlobalScope().getFunction("@__sulong_getPTHREAD_MUTEX_RECURSIVE").getLLVMIRFunction();
        LLVMStack.StackPointer sp1 =  ctx.getThreadingStack().getStack().newFrame();
        PTHREADMUTEXRECURSIVE = (int) callTarget.call(sp1);
        loadedPTHREADMUTEXRECURSIVE = true;
        return PTHREADMUTEXRECURSIVE;
    }
}
