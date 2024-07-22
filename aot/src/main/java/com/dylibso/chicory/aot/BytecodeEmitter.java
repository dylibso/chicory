package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.Instruction;
import org.objectweb.asm.MethodVisitor;

@FunctionalInterface
interface BytecodeEmitter {

    void emit(AotContext context, Instruction ins, MethodVisitor asm);
}
