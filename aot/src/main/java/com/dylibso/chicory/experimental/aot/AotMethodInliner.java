package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.experimental.aot.AotUtil.internalClassName;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.io.InputStreams;
import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

final class AotMethodInliner {

    private AotMethodInliner() {}

    public static byte[] createAotMethodsClass(String className) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = aotMethodsRemapper(writer, className);

        visitor =
                new ClassVisitor(Opcodes.ASM9, visitor) {
                    @Override
                    public void visit(
                            int version,
                            int access,
                            String name,
                            String signature,
                            String superName,
                            String[] interfaces) {
                        super.visit(
                                version,
                                Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                                internalClassName(className + "$AotMethods"),
                                null,
                                superName,
                                null);
                    }

                    @Override
                    public void visitEnd() {
                        visitNestHost(internalClassName(className));
                        super.visitEnd();
                    }
                };

        ClassReader reader = new ClassReader(getBytecode(AotMethods.class));
        reader.accept(visitor, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    public static ClassRemapper aotMethodsRemapper(ClassVisitor visitor, String className) {
        String targetInternalName = internalClassName(className + "$AotMethods");
        String originalInternalName = internalClassName(AotMethods.class.getName());
        return new ClassRemapper(
                visitor,
                new Remapper() {
                    @Override
                    public String map(String internalName) {
                        if (internalName.equals(originalInternalName)) {
                            return targetInternalName;
                        }
                        return super.map(internalName);
                    }
                });
    }

    private static byte[] getBytecode(Class<?> clazz) {
        var name = getInternalName(clazz) + ".class";
        try (var in = clazz.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Resource not found: " + name);
            }
            return InputStreams.readAllBytes(in);
        } catch (IOException e) {
            throw new ChicoryException("Could not load bytecode for " + clazz, e);
        }
    }
}
