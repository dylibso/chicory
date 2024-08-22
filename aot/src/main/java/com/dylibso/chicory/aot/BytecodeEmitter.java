package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import org.objectweb.asm.MethodVisitor;

interface BytecodeEmitter {

    void emit(AotContext context, AnnotatedInstruction ins, MethodVisitor asm);
}
