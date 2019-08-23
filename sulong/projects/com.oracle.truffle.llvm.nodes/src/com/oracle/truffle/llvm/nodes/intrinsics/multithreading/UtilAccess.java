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
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

import java.util.concurrent.ConcurrentMap;

public class UtilAccess {
    @CompilerDirectives.TruffleBoundary
    public static void putIntObj(ConcurrentMap<Integer, Object> c, int key, Object value) {
        c.put(key, value);
    }

    @CompilerDirectives.TruffleBoundary
    public static void putLLVMPointerObj(ConcurrentMap<LLVMPointer, Object> c, LLVMPointer key, Object value) {
        c.put(key, value);
    }

    @CompilerDirectives.TruffleBoundary
    public static void putLongObj(ConcurrentMap<Long, Object> c, long key, Object value) {
        c.put(key, value);
    }

    @CompilerDirectives.TruffleBoundary
    public static void putLongThread(ConcurrentMap<Long, Thread> c, long key, Thread value) {
        c.put(key, value);
    }

    @CompilerDirectives.TruffleBoundary
    static Object getIntObj(ConcurrentMap<Integer, Object> c, LLVMPointer key) {
        return c.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    static Object getLLVMPointerObj(ConcurrentMap<LLVMPointer, Object> c, LLVMPointer key) {
        return c.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    static Object getLongObj(ConcurrentMap<Long, Object> c, long key) {
        return c.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    static Thread getLongThread(ConcurrentMap<Long, Thread> c, long key) {
        return c.get(key);
    }

    @CompilerDirectives.TruffleBoundary
    public static void removeIntObj(ConcurrentMap<Integer, Object> c, LLVMPointer key) {
        c.remove(key);
    }

    @CompilerDirectives.TruffleBoundary
    public static void removeLLVMPointerObj(ConcurrentMap<LLVMPointer, Object> c, LLVMPointer key) {
        c.remove(key);
    }

    @CompilerDirectives.TruffleBoundary
    public static void removeLongObj(ConcurrentMap<Long, Object> c, long key) {
        c.remove(key);
    }

    @CompilerDirectives.TruffleBoundary
    public static void removeLongThread(ConcurrentMap<Long, Thread> c, long key) {
        c.remove(key);
    }
}
