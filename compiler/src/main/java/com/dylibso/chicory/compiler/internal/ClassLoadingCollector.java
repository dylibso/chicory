package com.dylibso.chicory.compiler.internal;

import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.ChicoryException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * A {@link ClassCollector} that stores the classes in an ordered map.
 *
 * <ul>
 *   <li> It loads the given bytes into a classloader for verification as they are inserted.
 *   <li> It resolves a given class to bytes by looking into classpath.
 *   <li> It optionally generates a MachineFactory with the internal classloader
 * </ul>
 */
public class ClassLoadingCollector implements ClassCollector {
    private final WasmClassLoader classLoader;
    private LinkedHashMap<String, byte[]> classBytes = new LinkedHashMap<>();
    private String mainClass;
    private Function<Instance, Machine> machineFactory;

    public ClassLoadingCollector() {
        this.classLoader = new WasmClassLoader();
    }

    @Override
    public String mainClassName() {
        return mainClass;
    }

    @Override
    public void putMainClass(String className, byte[] bytes) {
        this.mainClass = className;
        loadClass(bytes);

        // Ensure the main class comes first in order
        // this is not strictly necessary, but it is often
        // enforced in test cases.

        var classBytes = new LinkedHashMap<String, byte[]>();
        classBytes.put(className, bytes);
        classBytes.putAll(this.classBytes);

        this.classBytes = classBytes;
    }

    /**
     * It may throw if the class is invalid
     * (e.g., VerifyError)
     */
    @Override
    public void put(String className, byte[] bytes) {
        loadClass(bytes);
        classBytes.put(className, bytes);
    }

    @Override
    public void putAll(ClassCollector collector) {
        var classBytes = collector.classBytes();
        this.classBytes.putAll(classBytes);
        for (var bytes : classBytes.values()) {
            loadClass(bytes);
        }
    }

    @Override
    public Map<String, byte[]> classBytes() {
        return Collections.unmodifiableMap(classBytes);
    }

    @Override
    public byte[] resolve(Class<?> clazz) {
        return Shader.getBytecode(clazz);
    }

    public Function<Instance, Machine> machineFactory() {
        if (this.machineFactory == null) {
            Objects.requireNonNull(mainClass);
            this.machineFactory = createMachineFactory(mainClass);
        }
        return this.machineFactory;
    }

    private Function<Instance, Machine> createMachineFactory(String name) {
        try {
            var clazz = classLoader.loadClass(name).asSubclass(Machine.class);
            // convert constructor to factory interface
            var constructor = clazz.getConstructor(Instance.class);
            var handle = publicLookup().unreflectConstructor(constructor);
            @SuppressWarnings("unchecked")
            Function<Instance, Machine> function = asInterfaceInstance(Function.class, handle);
            return function;
        } catch (ReflectiveOperationException e) {
            throw new ChicoryException(e);
        }
    }

    private Class<?> loadClass(byte[] classBytes) {
        try {
            var clazz = classLoader.loadFromBytes(classBytes);
            // force initialization to run JVM verifier
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (VerifyError e) {
            // run ASM verifier to help with debugging
            try {
                var out = new StringWriter().append("ASM verifier:\n\n");
                CheckClassAdapter.verify(new ClassReader(classBytes), true, new PrintWriter(out));
                e.addSuppressed(new RuntimeException(out.toString()));
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }
}
