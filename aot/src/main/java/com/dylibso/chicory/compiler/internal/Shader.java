package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.internalClassName;
import static org.objectweb.asm.Type.getInternalName;

import com.dylibso.chicory.wasm.ChicoryException;
import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * The Shader class is responsible for creating a shaded version of the Shaded class.
 */
final class Shader {

    private Shader() {}

    public static byte[] createShadedClass(String className) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = shadedClassRemapper(writer, className);

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
                                internalClassName(className + "Shaded"),
                                null,
                                superName,
                                null);
                    }
                };

        ClassReader reader = new ClassReader(getBytecode(Shaded.class));
        reader.accept(visitor, ClassReader.SKIP_FRAMES);

        return writer.toByteArray();
    }

    public static ClassRemapper shadedClassRemapper(ClassVisitor visitor, String className) {
        String targetInternalName = internalClassName(className + "Shaded");
        String originalInternalName = internalClassName(Shaded.class.getName());
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
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ChicoryException("Could not load bytecode for " + clazz, e);
        }
    }
}
