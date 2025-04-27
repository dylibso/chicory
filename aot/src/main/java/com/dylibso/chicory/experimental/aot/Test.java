package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class Test {

    InterpreterMachine interpreterMachine;

    public Test(Instance foo) {
        this.interpreterMachine = new InterpreterMachine(foo);
    }

    public long[] call(int var1, long[] var2) {
        return this.interpreterMachine.call(var1, var2);
    }

    public static void main(String[] args) throws IOException {
        var writer = new StringWriter();
        var bytes = Test.class.getResourceAsStream("Test.class").readAllBytes();
        ClassReader cr = new ClassReader(bytes);
        cr.accept(new TraceClassVisitor(new PrintWriter(writer)), 0);
        writer.append("\n");
        System.out.println(writer);
    }
}
