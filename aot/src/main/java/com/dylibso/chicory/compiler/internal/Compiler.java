package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.asmType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callDispatchMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callIndirectMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callIndirectMethodType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.callMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.classNameForCallIndirect;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.classNameForDispatch;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.defaultValue;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeFunction;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeStatic;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitInvokeVirtual;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitJvmToLong;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.emitLongToJvm;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.hasTooManyParameters;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.internalClassName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.jvmReturnType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.localType;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.methodNameForFunc;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.methodTypeFor;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.rawMethodTypeFor;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.slotCount;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.valueMethodName;
import static com.dylibso.chicory.compiler.internal.CompilerUtil.valueMethodType;
import static com.dylibso.chicory.compiler.internal.EmitterMap.EMITTERS;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.AOT_INTERPRETER_MACHINE_CALL;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.CALL_HOST_FUNCTION;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.CALL_INDIRECT;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.CALL_INDIRECT_ON_INTERPRETER;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.CHECK_INTERRUPTION;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.INSTANCE_MEMORY;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.INSTANCE_TABLE;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.TABLE_INSTANCE;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.TABLE_REQUIRED_REF;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.THROW_CALL_STACK_EXHAUSTED;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.THROW_INDIRECT_CALL_TYPE_MISMATCH;
import static com.dylibso.chicory.compiler.internal.ShadedRefs.THROW_UNKNOWN_FUNCTION;
import static com.dylibso.chicory.compiler.internal.Shader.createShadedClass;
import static com.dylibso.chicory.compiler.internal.Shader.shadedClassRemapper;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;
import static org.objectweb.asm.commons.InstructionAdapter.OBJECT_TYPE;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.internal.CompilerInterpreterMachine;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassTooLargeException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

public final class Compiler {

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.$gen.CompiledMachine";
    private static final Type LONG_ARRAY_TYPE = Type.getType(long[].class);
    private static final Type INT_ARRAY_TYPE = Type.getType(int[].class);
    private static final Type AOT_INTERPRETER_MACHINE_TYPE =
            Type.getType(CompilerInterpreterMachine.class);
    private static final Type INSTANCE_TYPE = Type.getType(Instance.class);

    private static final MethodType CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, long[].class);

    private static final MethodType MACHINE_CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, int.class, long[].class);

    private static final int MAX_MACHINE_CALL_METHODS = 1024; // must be power of two
    // 1024*12 was empirically determined to work for the 50K small wasm functions.
    // So lets start there and halve it until we find a size that works.
    // This should give us the biggest class size possible.
    private static final int DEFAULT_MAX_FUNCTIONS_PER_CLASS = 1024 * 12;

    private final WasmClassLoader classLoader = new WasmClassLoader();
    private final String className;
    private final WasmModule module;
    private final WasmAnalyzer analyzer;
    private final int functionImports;
    private final InterpreterFallback interpreterFallback;
    private final List<FunctionType> functionTypes;
    private final Map<String, byte[]> extraClasses = new LinkedHashMap<>();
    private int maxFunctionsPerClass;
    private final HashSet<Integer> interpretedFunctions;

    private Compiler(
            WasmModule module,
            String className,
            int maxFunctionsPerClass,
            InterpreterFallback interpreterFallback,
            Set<Integer> interpretedFunctions) {
        this.className = requireNonNull(className, "className");
        this.module = requireNonNull(module, "module");
        this.analyzer = new WasmAnalyzer(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);

        if (interpretedFunctions == null || interpretedFunctions.isEmpty()) {
            this.interpretedFunctions = new HashSet<>();
            this.interpreterFallback =
                    requireNonNullElse(interpreterFallback, InterpreterFallback.WARN);
        } else if (interpreterFallback != InterpreterFallback.FAIL) {
            // if we are being given a set of interpreted functions, then any unlisted
            // function needs to trigger a failure.
            throw new IllegalArgumentException(
                    "InterpreterFallback must be set to FAIL if a fixed set of interpreted"
                            + " functions is provided");
        } else {
            this.interpretedFunctions = new HashSet<>(interpretedFunctions);
            this.interpreterFallback = InterpreterFallback.FAIL;
        }

        this.functionTypes = analyzer.functionTypes();
        this.maxFunctionsPerClass = maxFunctionsPerClass;
        compileExtraClasses();
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private String className;
        private int maxFunctionsPerClass;
        private InterpreterFallback interpreterFallback;
        private Set<Integer> interpretedFunctions;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder withMaxFunctionsPerClass(int maxFunctionsPerClass) {
            this.maxFunctionsPerClass = maxFunctionsPerClass;
            return this;
        }

        public Builder withInterpreterFallback(InterpreterFallback interpreterFallback) {
            this.interpreterFallback = interpreterFallback;
            return this;
        }

        public Builder withInterpretedFunctions(Set<Integer> interpretedFunctions) {
            this.interpretedFunctions = interpretedFunctions;
            return this;
        }

        public Compiler build() {
            var className = this.className;
            if (className == null) {
                className = DEFAULT_CLASS_NAME;
            }

            int maxFunctionsPerClass = this.maxFunctionsPerClass;
            if (maxFunctionsPerClass <= 0) {
                maxFunctionsPerClass = DEFAULT_MAX_FUNCTIONS_PER_CLASS;
            }
            return new Compiler(
                    module,
                    className,
                    maxFunctionsPerClass,
                    interpreterFallback,
                    interpretedFunctions);
        }
    }

    public CompilerResult compile() {
        var bytes = compileClass();
        var factory = createMachineFactory(bytes);

        Map<String, byte[]> classBytes = new LinkedHashMap<>();
        classBytes.put(className, bytes);
        classBytes.putAll(extraClasses);
        return new CompilerResult(factory, classBytes, Set.copyOf(interpretedFunctions));
    }

    private Function<Instance, Machine> createMachineFactory(byte[] classBytes) {
        try {
            var clazz = loadClass(classBytes).asSubclass(Machine.class);
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
        return loadClass(classLoader, classBytes);
    }

    private Class<?> loadClass(WasmClassLoader classLoader, byte[] classBytes) {
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

    private String loadExtraClass(byte[] bytes) {
        Class<?> clazz = loadClass(bytes);
        extraClasses.put(clazz.getName(), bytes);
        return clazz.getName();
    }

    private void compileExtraClasses() {
        loadExtraClass(createShadedClass(className));

        int totalFunctions = functionImports + module.functionSection().functionCount();

        // Emit the "${className}FuncGroup_${chunk}" classes:
        // We group the wasm functions into chunks to avoid MethodTooLargeException or
        // ClassTooLargeException.
        // The chunk size is dynamically adjusted based on the size of the class to be loaded and
        // the maximum size
        // that can be loaded without causing an exception.
        //
        // Example: wasm file has 1024 * 15 functions.  Then the first 12k functions will be located
        // in "${className}FuncGroup_0" and the last 3k functions will be located in
        // "${className}FuncGroup_1".
        // That is if no  MethodTooLargeException or ClassTooLargeException occur when generating
        // those two classes.
        // This can happen if for example a function requires alot of class constants etc.   Then
        // default
        // chunk size of 12k will be halved to 6k and the first 6k functions will be located in
        // "${className}FuncGroup_0", "${className}FuncGroup_1", and  "${className}FuncGroup_2" with
        // each class holding up to 6k of the functions.
        //
        var originalMaxFunctionsPerClass = maxFunctionsPerClass;
        while (true) {
            try {
                maxFunctionsPerClass =
                        loadChunkedClass(
                                totalFunctions,
                                maxFunctionsPerClass,
                                (start, end, chunkSize) -> {
                                    maxFunctionsPerClass = chunkSize;
                                    String className = classNameForFuncGroup(start);
                                    return compileExtraClass(
                                            className,
                                            emitFunctionGroup(
                                                    start, end, internalClassName(this.className)));
                                });
                break;
            } catch (MethodTooLargeException e) {
                String methodName = e.getMethodName();
                if (methodName.startsWith("func_")) {
                    // Add the method to interpreted function list... and try again.
                    var funcId = Integer.parseInt(methodName.substring("func_".length()));

                    String functionDescription = "WASM function index: " + funcId;
                    if (module.nameSection() != null) {
                        String name = module.nameSection().nameOfFunction(funcId);
                        if (name != null) {
                            functionDescription += String.format(" (name: %s)", name);
                        }
                    }

                    switch (interpreterFallback) {
                        case SILENT:
                            break;
                        case WARN:
                            System.err.println(
                                    "Warning: using interpreted mode for " + functionDescription);
                            break;
                        case FAIL:
                            throw new ChicoryException(
                                    "WASM function size exceeds the Java method size limits and"
                                        + " cannot be compiled to Java bytecode. It can only be run"
                                        + " in the interpreter. Either reduce the size of the"
                                        + " function or enable the interpreter fallback mode: "
                                            + functionDescription,
                                    e);
                    }

                    interpretedFunctions.add(funcId);
                    maxFunctionsPerClass = originalMaxFunctionsPerClass;
                } else {
                    throw e;
                }
            }
        }

        if (!functionTypes.isEmpty()) {
            loadExtraClass(compileMachineCallClass());
        }
    }

    interface ChunkedClassEmitter {
        byte[] emit(int start, int end, int chunkSize);
    }

    /**
     * Loads a chunked class based on the given size and chunk size.
     * <p>
     * This method attempts to load a class in chunks to avoid MethodTooLargeException or ClassTooLargeException.
     * It dynamically adjusts the chunk size based on the size of the class to be loaded and the maximum size
     * that can be loaded without causing an exception. The method returns the final chunk size used for loading.
     *
     * @param size      The total size of the class to be loaded.
     * @param chunkSize The initial chunk size to use for loading.
     * @param emitter   The ChunkedClassEmitter that generates the class bytes for a given chunk.
     * @return The final chunk size used for loading the class.
     */
    int loadChunkedClass(int size, int chunkSize, ChunkedClassEmitter emitter) {
        ArrayList<byte[]> generated = new ArrayList<byte[]>();
        WasmClassLoader classLoader = new WasmClassLoader();
        while (true) {
            try {
                int chunks = (size / chunkSize) + (size % chunkSize == 0 ? 0 : 1);
                for (int i = 0; i < chunks; i++) {
                    var start = i * chunkSize;
                    var end = min(start + chunkSize, size);

                    byte[] bytes = emitter.emit(start, end, chunkSize);
                    loadClass(classLoader, bytes);
                    generated.add(bytes);
                }
                break;
            } catch (ClassTooLargeException e) {
                chunkSize = chunkSize >> 1;
                if (chunkSize == 0) {
                    throw e;
                }
                generated.clear();
                classLoader = new WasmClassLoader();
            }
        }
        for (var bytes : generated) {
            loadExtraClass(bytes);
        }
        return chunkSize;
    }

    private String classNameForFuncGroup(int funcId) {
        return "FuncGroup_" + (funcId / maxFunctionsPerClass);
    }

    private Consumer<ClassVisitor> emitFunctionGroup(int start, int end, String internalClassName) {
        return (classWriter) -> {
            for (int i = start; i < end; i++) {
                FunctionBody body = null;
                try {
                    int funcId = i;
                    var type = functionTypes.get(funcId);

                    // is it an import function?
                    if (i < functionImports) {
                        emitFunction(
                                classWriter,
                                methodNameForFunc(funcId),
                                methodTypeFor(type),
                                true,
                                asm -> compileHostFunction(funcId, type, asm));

                    } else {
                        body = module.codeSection().getFunctionBody(i - functionImports);
                        var bodyCopy = body;

                        emitFunction(
                                classWriter,
                                methodNameForFunc(funcId),
                                methodTypeFor(type),
                                true,
                                asm ->
                                        compileFunction(
                                                internalClassName, funcId, type, bodyCopy, asm));

                        // call_xxx() bridges for boxed to native
                        emitFunction(
                                classWriter,
                                callMethodName(funcId),
                                CALL_METHOD_TYPE,
                                true,
                                asm -> compileCallFunction(funcId, type, asm));
                    }
                } catch (MethodTooLargeException e) {
                    throw handleMethodTooLarge(e, module);
                }
            }
        };
    }

    private byte[] compileClass() {
        var internalClassName = internalClassName(className);

        ClassWriter binaryWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classWriter = shadedClassRemapper(binaryWriter, className);

        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalClassName,
                null,
                getInternalName(Object.class),
                new String[] {getInternalName(Machine.class)});

        classWriter.visitSource("wasm", null);

        classWriter.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "instance",
                getDescriptor(Instance.class),
                null,
                null);

        if (!interpretedFunctions.isEmpty()) {
            classWriter.visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                    "compilerInterpreterMachine",
                    getDescriptor(CompilerInterpreterMachine.class),
                    null,
                    null);
        }

        // constructor
        emitFunction(
                classWriter,
                "<init>",
                methodType(void.class, Instance.class),
                false,
                asm -> compileConstructor(asm, internalClassName));

        // Machine.call() implementation
        emitFunction(
                classWriter,
                "call",
                methodType(long[].class, int.class, long[].class),
                false,
                asm -> compileMachineCall(internalClassName, asm));

        // call_indirect_xxx() bridges for native CALL_INDIRECT
        var allTypes = module.typeSection().types();
        for (int i = 0; i < allTypes.length; i++) {
            var typeId = i;
            var type = allTypes[i];
            emitFunction(
                    classWriter,
                    callIndirectMethodName(typeId),
                    callIndirectMethodType(type),
                    true,
                    asm -> compileCallIndirect(internalClassName, typeId, type, asm));
        }

        // value_xxx() bridges for multi-value return
        var returnTypes =
                functionTypes.stream()
                        .map(FunctionType::returns)
                        .filter(types -> types.size() > 1)
                        .collect(toSet());
        for (var types : returnTypes) {
            emitFunction(
                    classWriter,
                    valueMethodName(types),
                    valueMethodType(types),
                    true,
                    asm -> {
                        emitBoxArguments(asm, types);
                        asm.areturn(OBJECT_TYPE);
                    });
        }

        classWriter.visitEnd();

        try {
            return binaryWriter.toByteArray();
        } catch (MethodTooLargeException e) {
            throw handleMethodTooLarge(e, module);
        }
    }

    private static RuntimeException handleMethodTooLarge(
            MethodTooLargeException e, WasmModule module) {
        String name = e.getMethodName();
        if (name.startsWith("func_") && module.nameSection() != null) {
            int funcId = Integer.parseInt(name.split("_", -1)[1]);
            String function = module.nameSection().nameOfFunction(funcId);
            if (function != null) {
                name += " (" + function + ")";
            }
        }
        return new ChicoryException(
                String.format(
                        "JVM bytecode too large for WASM method: %s size=%d",
                        name, e.getCodeSize()),
                e);
    }

    private static void emitFunction(
            ClassVisitor classWriter,
            String methodName,
            MethodType methodType,
            boolean isStatic,
            Consumer<InstructionAdapter> consumer) {

        var methodWriter =
                classWriter.visitMethod(
                        Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                        methodName,
                        methodType.toMethodDescriptorString(),
                        null,
                        null);

        // uncomment if you ever want to troubleshoot invalid bytecode generation
        // methodWriter = new CheckMethodAdapter(methodWriter);

        methodWriter.visitCode();
        consumer.accept(new InstructionAdapter(methodWriter));
        methodWriter.visitMaxs(0, 0);
        methodWriter.visitEnd();
    }

    private static void emitCallSuper(InstructionAdapter asm) {
        asm.load(0, OBJECT_TYPE);
        asm.invokespecial(
                OBJECT_TYPE.getInternalName(), "<init>", getMethodDescriptor(VOID_TYPE), false);
    }

    private void compileConstructor(InstructionAdapter asm, String internalClassName) {
        emitCallSuper(asm);

        // this.instance = instance;
        asm.load(0, OBJECT_TYPE);
        asm.load(1, OBJECT_TYPE);
        asm.putfield(internalClassName, "instance", getDescriptor(Instance.class));

        if (!interpretedFunctions.isEmpty()) {

            asm.load(0, OBJECT_TYPE);
            asm.anew(AOT_INTERPRETER_MACHINE_TYPE);
            asm.dup();
            asm.load(1, OBJECT_TYPE);

            // construct int[] with the interpreted function ids
            var funcIds = new ArrayList<>(interpretedFunctions);
            asm.iconst(funcIds.size());
            asm.newarray(INT_TYPE);
            for (int i = 0; i < funcIds.size(); i++) {
                asm.dup();
                asm.iconst(i);
                asm.iconst(funcIds.get(i));
                asm.astore(INT_TYPE);
            }

            asm.invokespecial(
                    AOT_INTERPRETER_MACHINE_TYPE.getInternalName(),
                    "<init>",
                    getMethodDescriptor(VOID_TYPE, INSTANCE_TYPE, INT_ARRAY_TYPE),
                    false);
            asm.putfield(
                    internalClassName,
                    "compilerInterpreterMachine",
                    getDescriptor(CompilerInterpreterMachine.class));
        }

        asm.areturn(VOID_TYPE);
    }

    // implements the body of:
    // public long[] call(int var1, long[] var2)
    private void compileMachineCall(String internalClassName, InstructionAdapter asm) {

        // handle modules with no functions
        if (functionTypes.isEmpty()) {
            asm.load(1, INT_TYPE);
            emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
            asm.athrow();
            return;
        }

        if (!interpretedFunctions.isEmpty()) {
            Label invalid = new Label();
            int[] keys = interpretedFunctions.stream().mapToInt(x -> x).sorted().toArray();
            Label[] labels =
                    interpretedFunctions.stream().map(x -> new Label()).toArray(Label[]::new);
            asm.load(1, INT_TYPE);
            asm.lookupswitch(invalid, keys, labels);
            for (int i = 0; i < interpretedFunctions.size(); i++) {
                // case 0:
                //    return this.compilerInterpreterMachine.call(var1, var2);
                asm.mark(labels[i]);

                asm.load(0, OBJECT_TYPE);
                asm.getfield(
                        internalClassName,
                        "compilerInterpreterMachine",
                        getDescriptor(CompilerInterpreterMachine.class));
                asm.load(1, INT_TYPE);
                asm.load(2, OBJECT_TYPE);

                asm.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        AOT_INTERPRETER_MACHINE_TYPE.getInternalName(),
                        "call",
                        getMethodDescriptor(AOT_INTERPRETER_MACHINE_CALL),
                        false);
                asm.areturn(OBJECT_TYPE);
            }
            asm.mark(invalid);
        }

        // try block
        Label start = new Label();
        Label end = new Label();
        asm.visitTryCatchBlock(start, end, end, getInternalName(StackOverflowError.class));
        asm.mark(start);

        // prepare arguments
        asm.load(0, OBJECT_TYPE);
        asm.getfield(internalClassName, "instance", getDescriptor(Instance.class));
        asm.dup();
        emitInvokeVirtual(asm, INSTANCE_MEMORY);
        asm.load(1, INT_TYPE);
        asm.load(2, OBJECT_TYPE);

        // return MachineCall.call(instance, memory, funcId, args);
        asm.invokestatic(
                internalClassName + "MachineCall",
                "call",
                MACHINE_CALL_METHOD_TYPE.toMethodDescriptorString(),
                false);
        asm.areturn(OBJECT_TYPE);

        // catch StackOverflow
        asm.mark(end);
        emitInvokeStatic(asm, THROW_CALL_STACK_EXHAUSTED);
        asm.athrow();
    }

    private byte[] compileMachineCallClass() {
        ClassWriter binaryWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classWriter = shadedClassRemapper(binaryWriter, className);

        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalClassName(className + "MachineCall"),
                null,
                getInternalName(Object.class),
                null);

        // constructor
        emitFunction(
                classWriter,
                "<init>",
                methodType(void.class),
                false,
                asm -> {
                    emitCallSuper(asm);
                    asm.areturn(VOID_TYPE);
                });

        // static implementation for Machine.call()
        Consumer<InstructionAdapter> callMethod;
        if (functionTypes.size() < MAX_MACHINE_CALL_METHODS) {
            callMethod = asm -> compileMachineCallInvoke(asm, 0, functionTypes.size());
        } else {
            // Best value that worked with the 50K small wasm functions
            var maxMachineCallMethods = MAX_MACHINE_CALL_METHODS << 2;
            maxMachineCallMethods =
                    loadChunkedClass(
                            functionTypes.size(),
                            maxMachineCallMethods,
                            (start, end, chunkSize) ->
                                    compileExtraClass(
                                            classNameForDispatch(start),
                                            (cw) ->
                                                    emitFunction(
                                                            cw,
                                                            callDispatchMethodName(start),
                                                            MACHINE_CALL_METHOD_TYPE,
                                                            true,
                                                            asm ->
                                                                    compileMachineCallInvoke(
                                                                            asm, start, end))));
            callMethod = compileMachineCallDispatch(maxMachineCallMethods);
        }
        emitFunction(classWriter, "call", MACHINE_CALL_METHOD_TYPE, true, callMethod);

        return binaryWriter.toByteArray();
    }

    private byte[] compileExtraClass(String name, Consumer<ClassVisitor> consumer) {
        ClassWriter binaryWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classWriter = shadedClassRemapper(binaryWriter, className);
        String internalClassName = internalClassName(className + name);
        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalClassName,
                null,
                getInternalName(Object.class),
                null);
        consumer.accept(classWriter);
        return binaryWriter.toByteArray();
    }

    private Consumer<InstructionAdapter> compileMachineCallDispatch(int maxMachineCallMethods) {
        return (asm) -> {

            // load arguments
            asm.load(0, OBJECT_TYPE);
            asm.load(1, OBJECT_TYPE);
            asm.load(2, INT_TYPE);
            asm.load(3, OBJECT_TYPE);

            assert Integer.bitCount(maxMachineCallMethods) == 1; // power of two
            int shift = Integer.numberOfTrailingZeros(maxMachineCallMethods);

            // switch (funcId >> shift)
            Label[] labels = new Label[((functionTypes.size() - 1) >> shift) + 1];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = new Label();
            }

            asm.load(2, INT_TYPE);
            asm.iconst(shift);
            asm.shr(INT_TYPE);
            asm.tableswitch(0, labels.length - 1, labels[0], labels);

            // return call_dispatch_xxx(instance, memory, funcId, args);
            for (int i = 0; i < labels.length; i++) {
                asm.mark(labels[i]);
                asm.invokestatic(
                        internalClassName(className + classNameForDispatch(i << shift)),
                        callDispatchMethodName(i << shift),
                        MACHINE_CALL_METHOD_TYPE.toMethodDescriptorString(),
                        false);
                asm.areturn(OBJECT_TYPE);
            }
        };
    }

    private void compileMachineCallInvoke(InstructionAdapter asm, int start, int end) {
        // load arguments
        asm.load(0, OBJECT_TYPE);
        asm.load(1, OBJECT_TYPE);
        asm.load(3, OBJECT_TYPE);

        // switch (funcId)
        Label defaultLabel = new Label();
        Label hostLabel = new Label();
        Label[] labels = new Label[end - start];

        for (int id = start; id < end; id++) {
            labels[id - start] = (id < functionImports) ? hostLabel : new Label();
        }

        asm.load(2, INT_TYPE);
        asm.tableswitch(start, end - 1, defaultLabel, labels);

        // return call_xxx(instance, memory, args);
        for (int id = max(start, functionImports); id < end; id++) {
            asm.mark(labels[id - start]);
            asm.invokestatic(
                    internalClassName(className + classNameForFuncGroup(id)),
                    callMethodName(id),
                    CALL_METHOD_TYPE.toMethodDescriptorString(),
                    false);
            asm.areturn(OBJECT_TYPE);
        }

        // return instance.callHostFunction(funcId, args);
        if (functionImports > start) {
            asm.mark(hostLabel);
            asm.pop();
            asm.pop();
            asm.load(2, INT_TYPE);
            asm.load(3, OBJECT_TYPE);
            emitInvokeStatic(asm, CALL_HOST_FUNCTION);
            asm.areturn(OBJECT_TYPE);
        }

        // throw new InvalidException("unknown function " + funcId);
        asm.mark(defaultLabel);
        asm.load(2, INT_TYPE);
        emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
        asm.athrow();
    }

    // implements the body of:
    // public static long[] call_xxx(Memory memory, Instance instance, long[] args)
    private void compileCallFunction(int funcId, FunctionType type, InstructionAdapter asm) {

        if (hasTooManyParameters(type)) {
            asm.load(2, LONG_ARRAY_TYPE);
        } else {
            // unbox the arguments from long[]
            for (int i = 0; i < type.params().size(); i++) {
                var param = type.params().get(i);
                asm.load(2, OBJECT_TYPE);
                asm.iconst(i);
                asm.aload(LONG_TYPE);
                emitLongToJvm(asm, param);
            }
        }

        asm.load(1, OBJECT_TYPE);
        asm.load(0, OBJECT_TYPE);

        emitInvokeFunction(
                asm, internalClassName(className) + classNameForFuncGroup(funcId), funcId, type);

        // box the result into long[]
        Class<?> returnType = jvmReturnType(type);
        if (returnType == void.class) {
            asm.aconst(null);
        } else if (returnType != long[].class) {
            emitJvmToLong(asm, type.returns().get(0));
            asm.store(3, LONG_TYPE);
            asm.iconst(1);
            asm.newarray(LONG_TYPE);
            asm.dup();
            asm.iconst(0);
            asm.load(3, LONG_TYPE);
            asm.astore(LONG_TYPE);
        }
        asm.areturn(OBJECT_TYPE);
    }

    // implements the body of:
    // public static <TypeR> call_indirect_xxx(<TypeN> argN...,
    //      int funcTableIdx, int tableIdx, Memory memory, Instance instance)
    private void compileCallIndirect(
            String internalClassName, int typeId, FunctionType type, InstructionAdapter asm) {

        int slots = type.params().stream().mapToInt(CompilerUtil::slotCount).sum();
        if (hasTooManyParameters(type)) {
            slots = 1; // for long[]
        }

        List<Integer> validIds = new ArrayList<>();
        for (int i = 0; i < functionTypes.size(); i++) {
            if (type.equals(functionTypes.get(i))) {
                validIds.add(i);
            }
        }
        Label invalid = new Label();

        // extra params...
        int funcTableIdx = slots;
        int tableIdx = slots + 1;
        int memory = slots + 2;
        int instance = slots + 3;

        // local vars
        int table = slots + 4;
        int funcId = slots + 5;
        int refInstance = slots + 6;

        emitInvokeStatic(asm, CHECK_INTERRUPTION);

        // TableInstance table = instance.table(tableIdx);
        asm.load(instance, OBJECT_TYPE);
        asm.load(tableIdx, INT_TYPE);
        emitInvokeVirtual(asm, INSTANCE_TABLE);
        asm.store(table, OBJECT_TYPE);

        // int funcId = table.requiredRef(funcTableIdx);
        asm.load(table, OBJECT_TYPE);
        asm.load(funcTableIdx, INT_TYPE);
        emitInvokeVirtual(asm, TABLE_REQUIRED_REF);
        asm.store(funcId, INT_TYPE);

        // Instance refInstance = table.instance(funcTableIdx);
        asm.load(table, OBJECT_TYPE);
        asm.load(funcTableIdx, INT_TYPE);
        emitInvokeVirtual(asm, TABLE_INSTANCE);
        asm.store(refInstance, OBJECT_TYPE);

        Label local = new Label();
        Label other = new Label();

        // if (refInstance == null || refInstance == instance)
        asm.load(refInstance, OBJECT_TYPE);
        asm.ifnull(local);
        asm.load(refInstance, OBJECT_TYPE);
        asm.load(instance, OBJECT_TYPE);
        asm.ifacmpne(other);

        // local: call function in this module
        asm.mark(local);
        if (hasTooManyParameters(type)) {
            asm.load(0, LONG_ARRAY_TYPE);
        } else {
            int slot = 0;
            for (ValType param : type.params()) {
                asm.load(slot, asmType(param));
                slot += slotCount(param);
            }
        }
        asm.load(memory, OBJECT_TYPE);
        asm.load(instance, OBJECT_TYPE);

        // Can we fit the impl in a single method?
        if (validIds.size() <= MAX_MACHINE_CALL_METHODS) {

            int[] keys = validIds.stream().mapToInt(x -> x).toArray();
            Label[] labels = validIds.stream().map(x -> new Label()).toArray(Label[]::new);

            asm.load(funcId, INT_TYPE);
            asm.lookupswitch(invalid, keys, labels);

            for (int i = 0; i < validIds.size(); i++) {
                // case 0:
                //    return func_0(a, b, memory, callerInstance);
                asm.mark(labels[i]);
                emitInvokeFunction(
                        asm, internalClassName + classNameForFuncGroup(keys[i]), keys[i], type);
                asm.areturn(getType(jvmReturnType(type)));
            }

            asm.mark(invalid);
            emitInvokeStatic(asm, THROW_INDIRECT_CALL_TYPE_MISMATCH);
            asm.athrow();

        } else {
            var applyParams =
                    rawMethodTypeFor(type)
                            .appendParameterTypes(Memory.class, Instance.class, int.class);

            // Best value that worked with the 50K small wasm functions
            var maxMachineCallMethods = MAX_MACHINE_CALL_METHODS << 2;
            loadChunkedClass(
                    functionTypes.size(),
                    maxMachineCallMethods,
                    (start, end, chunkSize) ->
                            compileExtraClass(
                                    classNameForCallIndirect(typeId, start),
                                    (cw) -> {
                                        emitFunction(
                                                cw,
                                                "apply",
                                                applyParams,
                                                true,
                                                a ->
                                                        compileCallIndirectApply(
                                                                internalClassName,
                                                                type,
                                                                a,
                                                                start,
                                                                end));
                                    }));

            assert Integer.bitCount(maxMachineCallMethods) == 1; // power of two
            int shift = Integer.numberOfTrailingZeros(maxMachineCallMethods);

            // switch (funcId >> shift)
            Label[] labels = new Label[((functionTypes.size() - 1) >> shift) + 1];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = new Label();
            }

            asm.load(funcId, INT_TYPE);
            asm.iconst(shift);
            asm.shr(INT_TYPE);
            asm.tableswitch(0, labels.length - 1, labels[0], labels);

            // invoke the method that we are about to generate
            for (int i = 0; i < labels.length; i++) {
                asm.mark(labels[i]);
                asm.load(funcId, INT_TYPE);
                asm.invokestatic(
                        internalClassName + classNameForCallIndirect(typeId, i << shift),
                        "apply",
                        applyParams.toMethodDescriptorString(),
                        false);
                asm.areturn(getType(jvmReturnType(type)));
            }
        }

        // other: call function in another module
        asm.mark(other);

        if (hasTooManyParameters(type)) {
            asm.load(0, LONG_ARRAY_TYPE);
        } else {
            emitBoxArguments(asm, type.params());
        }
        asm.iconst(typeId);
        asm.load(funcId, INT_TYPE);
        asm.load(refInstance, OBJECT_TYPE);

        emitInvokeStatic(asm, CALL_INDIRECT);

        emitUnboxResult(type, asm);
    }

    private void compileCallIndirectApply(
            String internalClassName,
            FunctionType type,
            InstructionAdapter asm,
            int startFunc,
            int endFunc) {

        int slots = type.params().stream().mapToInt(CompilerUtil::slotCount).sum();
        if (hasTooManyParameters(type)) {
            slots = 1; // for long[]
        }

        // extra params...
        int memory = slots;
        int instance = slots + 1;
        int funcId = slots + 2;

        List<Integer> validIds = new ArrayList<>();
        for (int i = 0; i < functionTypes.size(); i++) {
            if (type.equals(functionTypes.get(i)) && startFunc <= i && i < endFunc) {
                validIds.add(i);
            }
        }
        Label invalid = new Label();

        int[] keys = validIds.stream().mapToInt(x -> x).toArray();
        Label[] labels = validIds.stream().map(x -> new Label()).toArray(Label[]::new);

        // push the call args on to the stack...
        for (int i = 0; i < type.params().size(); i++) {
            asm.load(i, asmType(type.params().get(i)));
        }
        asm.load(memory, OBJECT_TYPE);
        asm.load(instance, OBJECT_TYPE);

        // switch (funcId)
        asm.load(funcId, INT_TYPE);
        asm.lookupswitch(invalid, keys, labels);

        for (int i = 0; i < validIds.size(); i++) {
            // case 0:
            //    return func_0(a, b, memory, callerInstance);
            asm.mark(labels[i]);
            emitInvokeFunction(
                    asm, internalClassName + classNameForFuncGroup(keys[i]), keys[i], type);
            asm.areturn(getType(jvmReturnType(type)));
            asm.areturn(OBJECT_TYPE);
        }

        // throw new InvalidException("unknown function " + funcId);
        asm.mark(invalid);

        asm.load(funcId, INT_TYPE);
        emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
        asm.athrow();
    }

    // implements the body of:
    // public static <TypeR> func_xx(<TypeN> argN..., Memory memory, Instance instance)
    private static void compileHostFunction(int funcId, FunctionType type, InstructionAdapter asm) {

        int slot = type.params().stream().mapToInt(CompilerUtil::slotCount).sum();

        asm.load(slot + 1, OBJECT_TYPE); // instance
        asm.iconst(funcId);
        emitBoxArguments(asm, type.params());

        emitInvokeStatic(asm, CALL_HOST_FUNCTION);

        emitUnboxResult(type, asm);
    }

    private static void emitBoxArguments(InstructionAdapter asm, List<ValType> types) {
        int slot = 0;
        // box the arguments into long[]
        asm.iconst(types.size());
        asm.newarray(LONG_TYPE);
        for (int i = 0; i < types.size(); i++) {
            asm.dup();
            asm.iconst(i);
            ValType valType = types.get(i);
            asm.load(slot, asmType(valType));
            emitJvmToLong(asm, valType);
            asm.astore(LONG_TYPE);
            slot += slotCount(valType);
        }
    }

    private static void emitUnboxResult(FunctionType type, InstructionAdapter asm) {
        Class<?> returnType = jvmReturnType(type);
        if (returnType == void.class) {
            asm.areturn(VOID_TYPE);
        } else if (returnType == long[].class) {
            asm.areturn(OBJECT_TYPE);
        } else {
            // unbox the result from long[0]
            asm.iconst(0);
            asm.aload(LONG_TYPE);
            emitLongToJvm(asm, type.returns().get(0));
            asm.areturn(getType(returnType));
        }
    }

    // implements the body of:
    // public static <TypeR> func_xxx(<TypeN> ArgN..., Memory memory, Instance instance)
    private void compileFunction(
            String internalClassName,
            int funcId,
            FunctionType type,
            FunctionBody body,
            InstructionAdapter asm) {

        if (interpretedFunctions.contains(funcId)) {

            var slots = 0;
            if (hasTooManyParameters(type)) {
                asm.load(0, LONG_ARRAY_TYPE);
                slots = 1;
            } else {
                emitBoxArguments(asm, type.params());
                slots = type.params().stream().mapToInt(CompilerUtil::slotCount).sum();
            }

            var refInstance = slots + 1;

            asm.iconst(funcId);
            asm.load(refInstance, OBJECT_TYPE);
            emitInvokeStatic(asm, CALL_INDIRECT_ON_INTERPRETER);
            emitUnboxResult(type, asm);
            return;
        }

        var ctx =
                new Context(
                        internalClassName,
                        maxFunctionsPerClass,
                        analyzer.globalTypes(),
                        functionTypes,
                        module.typeSection().types(),
                        funcId,
                        type,
                        body);

        List<CompilerInstruction> instructions = analyzer.analyze(funcId);

        int localsCount = type.params().size();
        if (hasTooManyParameters(type)) {
            // unbox the arguments from long[]
            for (int i = 0; i < type.params().size(); i++) {
                var param = type.params().get(i);
                asm.load(0, OBJECT_TYPE);
                asm.iconst(i);
                asm.aload(LONG_TYPE);
                emitLongToJvm(asm, param);
                asm.store(ctx.localSlotIndex(i), asmType(param));
            }
            // since we just converted the arguments to long[].
            localsCount = 1;
        }

        // initialize local variables to their default values
        localsCount += body.localTypes().size();
        for (int i = type.params().size(); i < localsCount; i++) {
            var localType = localType(type, body, i);
            asm.visitLdcInsn(defaultValue(localType));
            asm.store(ctx.localSlotIndex(i), asmType(localType));
        }

        // allocate labels for all label targets
        Map<Long, Label> labels = new HashMap<>();
        for (var ins : instructions) {
            for (long target : ins.labelTargets()) {
                labels.put(target, new Label());
            }
        }

        // track targets to detect backward jumps
        Set<Long> visitedTargets = new HashSet<>();

        // compile the function body
        for (CompilerInstruction ins : instructions) {
            switch (ins.opcode()) {
                case LABEL:
                    Label label = labels.get(ins.operand(0));
                    if (label != null) {
                        asm.mark(label);
                        visitedTargets.add(ins.operand(0));
                    }
                    break;
                case GOTO:
                    if (visitedTargets.contains(ins.operand(0))) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    asm.goTo(labels.get(ins.operand(0)));
                    break;
                case IFEQ:
                    if (visitedTargets.contains(ins.operand(0))) {
                        throw new ChicoryException("Unexpected backward jump");
                    }
                    asm.ifeq(labels.get(ins.operand(0)));
                    break;
                case IFNE:
                    if (visitedTargets.contains(ins.operand(0))) {
                        Label skip = new Label();
                        asm.ifeq(skip);
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                        asm.goTo(labels.get(ins.operand(0)));
                        asm.mark(skip);

                    } else {
                        asm.ifne(labels.get(ins.operand(0)));
                    }
                    break;
                case SWITCH:
                    if (ins.operands().anyMatch(visitedTargets::contains)) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    // table switch using the last entry of the table as the default
                    Label[] table = new Label[ins.operandCount() - 1];
                    for (int i = 0; i < table.length; i++) {
                        table[i] = labels.get(ins.operand(i));
                    }
                    Label defaultLabel = labels.get(ins.operand(table.length));
                    asm.tableswitch(0, table.length - 1, defaultLabel, table);
                    break;
                default:
                    var emitter = EMITTERS.get(ins.opcode());
                    if (emitter == null) {
                        throw new ChicoryException("Unhandled opcode: " + ins.opcode());
                    }
                    emitter.emit(ctx, ins, asm);
            }
        }
    }
}
