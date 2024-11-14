package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.experimental.aot.AotEmitterMap.EMITTERS;
import static com.dylibso.chicory.experimental.aot.AotMethodInliner.aotMethodsRemapper;
import static com.dylibso.chicory.experimental.aot.AotMethodInliner.createAotMethodsClass;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.CALL_HOST_FUNCTION;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.CALL_INDIRECT;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.CHECK_INTERRUPTION;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.INSTANCE_MEMORY;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.INSTANCE_TABLE;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.TABLE_INSTANCE;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.TABLE_REQUIRED_REF;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.THROW_CALL_STACK_EXHAUSTED;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.THROW_INDIRECT_CALL_TYPE_MISMATCH;
import static com.dylibso.chicory.experimental.aot.AotMethodRefs.THROW_UNKNOWN_FUNCTION;
import static com.dylibso.chicory.experimental.aot.AotUtil.asmType;
import static com.dylibso.chicory.experimental.aot.AotUtil.callIndirectMethodName;
import static com.dylibso.chicory.experimental.aot.AotUtil.callIndirectMethodType;
import static com.dylibso.chicory.experimental.aot.AotUtil.defaultValue;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeFunction;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitJvmToLong;
import static com.dylibso.chicory.experimental.aot.AotUtil.emitLongToJvm;
import static com.dylibso.chicory.experimental.aot.AotUtil.internalClassName;
import static com.dylibso.chicory.experimental.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.experimental.aot.AotUtil.localType;
import static com.dylibso.chicory.experimental.aot.AotUtil.methodNameFor;
import static com.dylibso.chicory.experimental.aot.AotUtil.methodTypeFor;
import static com.dylibso.chicory.experimental.aot.AotUtil.slotCount;
import static com.dylibso.chicory.experimental.aot.AotUtil.valueMethodName;
import static com.dylibso.chicory.experimental.aot.AotUtil.valueMethodType;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
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
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

public final class AotCompiler {

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.$gen.CompiledMachine";

    private static final MethodType CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, long[].class);

    private static final MethodType MACHINE_CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, int.class, long[].class);

    private final AotClassLoader classLoader = new AotClassLoader();
    private final String className;
    private final WasmModule module;
    private final AotAnalyzer analyzer;
    private final int functionImports;
    private final List<FunctionType> functionTypes;
    private final Map<String, byte[]> extraClasses;

    private AotCompiler(WasmModule module, String className) {
        this.className = requireNonNull(className, "className");
        this.module = requireNonNull(module, "module");
        this.analyzer = new AotAnalyzer(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
        this.functionTypes = analyzer.functionTypes();
        this.extraClasses = compileExtraClasses();
    }

    public static CompilerResult compileModule(WasmModule module) {
        return compileModule(module, DEFAULT_CLASS_NAME);
    }

    public static CompilerResult compileModule(WasmModule module, String className) {
        var compiler = new AotCompiler(module, className);

        var bytes = compiler.compileClass();
        var factory = compiler.createMachineFactory(bytes);

        Map<String, byte[]> classBytes = new LinkedHashMap<>();
        classBytes.put(className, bytes);
        classBytes.putAll(compiler.extraClasses);
        return new CompilerResult(factory, classBytes);
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

    private void loadExtraClass(Map<String, byte[]> classes, byte[] bytes) {
        Class<?> clazz = loadClass(bytes);
        classes.put(clazz.getName(), bytes);
    }

    private Map<String, byte[]> compileExtraClasses() {
        Map<String, byte[]> classes = new LinkedHashMap<>();
        loadExtraClass(classes, createAotMethodsClass(className));
        if (!functionTypes.isEmpty()) {
            loadExtraClass(classes, compileMachineCallClass());
        }
        return classes;
    }

    private byte[] compileClass() {
        var internalClassName = internalClassName(className);

        ClassWriter binaryWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classWriter = aotMethodsRemapper(binaryWriter, className);

        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalClassName,
                null,
                getInternalName(Object.class),
                new String[] {getInternalName(Machine.class)});

        classWriter.visitSource("wasm", null);

        for (String name : extraClasses.keySet()) {
            classWriter.visitNestMember(internalClassName(name));
        }

        classWriter.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "instance",
                getDescriptor(Instance.class),
                null,
                null);

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

        // func_xxx() bridges for native to host functions
        for (int i = 0; i < functionImports; i++) {
            int funcId = i;
            var type = functionTypes.get(funcId);
            emitFunction(
                    classWriter,
                    methodNameFor(funcId),
                    methodTypeFor(type),
                    true,
                    asm -> compileHostFunction(funcId, type, asm));
        }

        // func_xxx() native function implementations
        for (int i = 0; i < module.functionSection().functionCount(); i++) {
            var funcId = functionImports + i;
            var type = functionTypes.get(funcId);
            var body = module.codeSection().getFunctionBody(i);

            emitFunction(
                    classWriter,
                    methodNameFor(funcId),
                    methodTypeFor(type),
                    true,
                    asm -> compileFunction(internalClassName, funcId, type, body, asm));
        }

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
            String name = e.getMethodName();
            if (name.startsWith("func_") && module.nameSection() != null) {
                int funcId = Integer.parseInt(name.split("_", -1)[1]);
                String function = module.nameSection().nameOfFunction(funcId);
                if (function != null) {
                    name += " (" + function + ")";
                }
            }
            throw new ChicoryException(
                    String.format(
                            "JVM bytecode too large for WASM method: %s size=%d",
                            name, e.getCodeSize()),
                    e);
        }
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

    private static void compileConstructor(InstructionAdapter asm, String internalClassName) {
        emitCallSuper(asm);

        // this.instance = instance;
        asm.load(0, OBJECT_TYPE);
        asm.load(1, OBJECT_TYPE);
        asm.putfield(internalClassName, "instance", getDescriptor(Instance.class));

        asm.areturn(VOID_TYPE);
    }

    private void compileMachineCall(String internalClassName, InstructionAdapter asm) {
        // handle modules with no functions
        if (functionTypes.isEmpty()) {
            asm.load(1, INT_TYPE);
            emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
            asm.athrow();
            return;
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

        // return $MachineCall.call(instance, memory, funcId, args);
        asm.invokestatic(
                internalClassName + "$MachineCall",
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
        ClassVisitor classWriter = aotMethodsRemapper(binaryWriter, className);

        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                internalClassName(className + "$MachineCall"),
                null,
                getInternalName(Object.class),
                null);

        classWriter.visitNestHost(internalClassName(className));

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
        emitFunction(
                classWriter,
                "call",
                MACHINE_CALL_METHOD_TYPE,
                true,
                this::compileMachineCallInvoke);

        // call_xxx() bridges for boxed to native
        for (int i = 0; i < module.functionSection().functionCount(); i++) {
            var funcId = functionImports + i;
            var type = functionTypes.get(funcId);
            emitFunction(
                    classWriter,
                    callMethodName(funcId),
                    CALL_METHOD_TYPE,
                    true,
                    asm -> compileCallFunction(funcId, type, asm));
        }

        return binaryWriter.toByteArray();
    }

    private void compileMachineCallInvoke(InstructionAdapter asm) {
        // load arguments
        asm.load(0, OBJECT_TYPE);
        asm.load(1, OBJECT_TYPE);
        asm.load(3, OBJECT_TYPE);

        // switch (funcId)
        Label defaultLabel = new Label();
        Label hostLabel = new Label();
        Label[] labels = new Label[functionTypes.size()];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = (i < functionImports) ? hostLabel : new Label();
        }

        asm.load(2, INT_TYPE);
        asm.tableswitch(0, labels.length - 1, defaultLabel, labels);

        // return call_xxx(instance, memory, args);
        for (int i = functionImports; i < labels.length; i++) {
            asm.mark(labels[i]);
            asm.invokestatic(
                    internalClassName(className + "$MachineCall"),
                    callMethodName(i),
                    CALL_METHOD_TYPE.toMethodDescriptorString(),
                    false);
            asm.areturn(OBJECT_TYPE);
        }

        // return instance.callHostFunction(funcId, args);
        if (functionImports > 0) {
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

    private void compileCallFunction(int funcId, FunctionType type, InstructionAdapter asm) {
        // unbox the arguments from long[]
        for (int i = 0; i < type.params().size(); i++) {
            var param = type.params().get(i);
            asm.load(2, OBJECT_TYPE);
            asm.iconst(i);
            asm.aload(LONG_TYPE);
            emitLongToJvm(asm, param);
        }

        asm.load(1, OBJECT_TYPE);
        asm.load(0, OBJECT_TYPE);

        emitInvokeFunction(asm, internalClassName(className), funcId, type);

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

    private void compileCallIndirect(
            String internalClassName, int typeId, FunctionType type, InstructionAdapter asm) {
        int slots = type.params().stream().mapToInt(AotUtil::slotCount).sum();
        int funcTableIdx = slots;
        int tableIdx = slots + 1;
        int memory = slots + 2;
        int instance = slots + 3;
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

        int slot = 0;
        for (ValueType param : type.params()) {
            asm.load(slot, asmType(param));
            slot += slotCount(param);
        }
        asm.load(memory, OBJECT_TYPE);
        asm.load(instance, OBJECT_TYPE);

        List<Integer> validIds = new ArrayList<>();
        for (int i = 0; i < functionTypes.size(); i++) {
            if (type.equals(functionTypes.get(i))) {
                validIds.add(i);
            }
        }

        Label invalid = new Label();
        int[] keys = validIds.stream().mapToInt(x -> x).toArray();
        Label[] labels = validIds.stream().map(x -> new Label()).toArray(Label[]::new);

        asm.load(funcId, INT_TYPE);
        asm.lookupswitch(invalid, keys, labels);

        for (int i = 0; i < validIds.size(); i++) {
            asm.mark(labels[i]);
            emitInvokeFunction(asm, internalClassName, keys[i], type);
            asm.areturn(getType(jvmReturnType(type)));
        }

        asm.mark(invalid);
        emitInvokeStatic(asm, THROW_INDIRECT_CALL_TYPE_MISMATCH);
        asm.athrow();

        // other: call function in another module
        asm.mark(other);

        emitBoxArguments(asm, type.params());
        asm.iconst(typeId);
        asm.load(funcId, INT_TYPE);
        asm.load(refInstance, OBJECT_TYPE);

        emitInvokeStatic(asm, CALL_INDIRECT);

        emitUnboxResult(type, asm);
    }

    private static void compileHostFunction(int funcId, FunctionType type, InstructionAdapter asm) {
        int slot = type.params().stream().mapToInt(AotUtil::slotCount).sum();

        asm.load(slot + 1, OBJECT_TYPE); // instance
        asm.iconst(funcId);
        emitBoxArguments(asm, type.params());

        emitInvokeStatic(asm, CALL_HOST_FUNCTION);

        emitUnboxResult(type, asm);
    }

    private static void emitBoxArguments(InstructionAdapter asm, List<ValueType> types) {
        int slot = 0;
        // box the arguments into long[]
        asm.iconst(types.size());
        asm.newarray(LONG_TYPE);
        for (int i = 0; i < types.size(); i++) {
            asm.dup();
            asm.iconst(i);
            ValueType valueType = types.get(i);
            asm.load(slot, asmType(valueType));
            emitJvmToLong(asm, valueType);
            asm.astore(LONG_TYPE);
            slot += slotCount(valueType);
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

    private void compileFunction(
            String internalClassName,
            int funcId,
            FunctionType type,
            FunctionBody body,
            InstructionAdapter asm) {

        var ctx =
                new AotContext(
                        internalClassName,
                        analyzer.globalTypes(),
                        functionTypes,
                        module.typeSection().types(),
                        funcId,
                        type,
                        body);

        List<AotInstruction> instructions = analyzer.analyze(funcId);

        // initialize local variables to their default values
        int localsCount = type.params().size() + body.localTypes().size();
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
        for (AotInstruction ins : instructions) {
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

    private static String callMethodName(int functId) {
        return "call_" + functId;
    }
}
