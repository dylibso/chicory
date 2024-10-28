package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotEmitterMap.EMITTERS;
import static com.dylibso.chicory.aot.AotMethodInliner.aotMethodsRemapper;
import static com.dylibso.chicory.aot.AotMethodInliner.createAotMethodsClass;
import static com.dylibso.chicory.aot.AotMethodRefs.CALL_HOST_FUNCTION;
import static com.dylibso.chicory.aot.AotMethodRefs.CALL_INDIRECT;
import static com.dylibso.chicory.aot.AotMethodRefs.CHECK_INTERRUPTION;
import static com.dylibso.chicory.aot.AotMethodRefs.INSTANCE_MEMORY;
import static com.dylibso.chicory.aot.AotMethodRefs.INSTANCE_TABLE;
import static com.dylibso.chicory.aot.AotMethodRefs.TABLE_INSTANCE;
import static com.dylibso.chicory.aot.AotMethodRefs.TABLE_REF;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_CALL_STACK_EXHAUSTED;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_INDIRECT_CALL_TYPE_MISMATCH;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_UNKNOWN_FUNCTION;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodName;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodType;
import static com.dylibso.chicory.aot.AotUtil.defaultValue;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeFunction;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.aot.AotUtil.emitJvmToLong;
import static com.dylibso.chicory.aot.AotUtil.emitLongToJvm;
import static com.dylibso.chicory.aot.AotUtil.internalClassName;
import static com.dylibso.chicory.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.aot.AotUtil.loadTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.localType;
import static com.dylibso.chicory.aot.AotUtil.methodNameFor;
import static com.dylibso.chicory.aot.AotUtil.methodTypeFor;
import static com.dylibso.chicory.aot.AotUtil.returnTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.slotCount;
import static com.dylibso.chicory.aot.AotUtil.storeTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.valueMethodName;
import static com.dylibso.chicory.aot.AotUtil.valueMethodType;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionSection;
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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

public final class AotCompiler {

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.$gen.CompiledMachine";

    private static final MethodType CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, long[].class);

    private final AotClassLoader classLoader = new AotClassLoader();
    private final String className;
    private final Module module;
    private final AotAnalyzer analyzer;
    private final int functionImports;
    private final List<FunctionType> functionTypes;
    private final Map<String, byte[]> extraClasses;

    private AotCompiler(Module module, String className) {
        this.className = requireNonNull(className, "className");
        this.module = requireNonNull(module, "module");
        this.analyzer = new AotAnalyzer(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
        this.functionTypes = analyzer.functionTypes();
        this.extraClasses = compileExtraClasses();
    }

    public static CompilerResult compileModule(Module module) {
        return compileModule(module, DEFAULT_CLASS_NAME);
    }

    public static CompilerResult compileModule(Module module, String className) {
        var compiler = new AotCompiler(module, className);

        var bytes = compiler.compileClass(module.functionSection());
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
        return classes;
    }

    private byte[] compileClass(FunctionSection functions) {
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

        emitConstructor(classWriter, internalClassName);

        // Machine.call() implementation
        emitFunction(
                classWriter,
                "call",
                methodType(long[].class, int.class, long[].class),
                false,
                asm -> compileMachineCall(internalClassName, asm));

        // call_xxx() bridges for boxed to native
        for (int i = 0; i < functions.functionCount(); i++) {
            var funcId = functionImports + i;
            var type = functionTypes.get(funcId);
            emitFunction(
                    classWriter,
                    callMethodName(funcId),
                    CALL_METHOD_TYPE,
                    true,
                    asm -> compileCallFunction(internalClassName, funcId, type, asm));
        }

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
        for (int i = 0; i < functions.functionCount(); i++) {
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
                        asm.visitInsn(Opcodes.ARETURN);
                    });
        }

        classWriter.visitEnd();

        try {
            return binaryWriter.toByteArray();
        } catch (MethodTooLargeException e) {
            throw new ChicoryException(
                    String.format(
                            "JVM bytecode too large for WASM method: %s size=%d",
                            e.getMethodName(), e.getCodeSize()),
                    e);
        }
    }

    private static void emitFunction(
            ClassVisitor classWriter,
            String methodName,
            MethodType methodType,
            boolean isStatic,
            Consumer<MethodVisitor> consumer) {

        var methodWriter =
                classWriter.visitMethod(
                        Opcodes.ACC_PUBLIC | (isStatic ? Opcodes.ACC_STATIC : 0),
                        methodName,
                        methodType.toMethodDescriptorString(),
                        null,
                        null);

        // optimize instruction size to avoid method size limits
        methodWriter = new InstructionAdapter(methodWriter);

        methodWriter.visitCode();
        consumer.accept(methodWriter);
        methodWriter.visitMaxs(0, 0);
        methodWriter.visitEnd();
    }

    private static void emitConstructor(ClassVisitor writer, String internalClassName) {
        var cons =
                writer.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        methodType(void.class, Instance.class).toMethodDescriptorString(),
                        null,
                        null);
        cons.visitCode();

        // super();
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                getInternalName(Object.class),
                "<init>",
                getMethodDescriptor(VOID_TYPE),
                false);

        // this.instance = instance;
        cons.visitVarInsn(Opcodes.ALOAD, 0);
        cons.visitVarInsn(Opcodes.ALOAD, 1);
        cons.visitFieldInsn(
                Opcodes.PUTFIELD, internalClassName, "instance", getDescriptor(Instance.class));

        cons.visitInsn(Opcodes.RETURN);
        cons.visitMaxs(0, 0);
        cons.visitEnd();
    }

    private void compileMachineCall(String internalClassName, MethodVisitor asm) {
        // handle modules with no functions
        if (functionTypes.isEmpty()) {
            asm.visitVarInsn(Opcodes.ILOAD, 1);
            emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
            asm.visitInsn(Opcodes.ATHROW);
            return;
        }

        // try block
        Label start = new Label();
        Label end = new Label();
        asm.visitTryCatchBlock(start, end, end, getInternalName(StackOverflowError.class));
        asm.visitLabel(start);

        // prepare arguments
        asm.visitVarInsn(Opcodes.ALOAD, 0);
        asm.visitFieldInsn(
                Opcodes.GETFIELD, internalClassName, "instance", getDescriptor(Instance.class));
        asm.visitInsn(Opcodes.DUP);
        emitInvokeVirtual(asm, INSTANCE_MEMORY);
        asm.visitVarInsn(Opcodes.ALOAD, 2);

        // switch (funcId)
        Label defaultLabel = new Label();
        Label hostLabel = new Label();
        Label[] labels = new Label[functionTypes.size()];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = (i < functionImports) ? hostLabel : new Label();
        }

        asm.visitVarInsn(Opcodes.ILOAD, 1);
        asm.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

        // return call_xxx(instance, memory, args);
        for (int i = functionImports; i < labels.length; i++) {
            asm.visitLabel(labels[i]);
            asm.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    internalClassName,
                    callMethodName(i),
                    CALL_METHOD_TYPE.toMethodDescriptorString(),
                    false);
            asm.visitInsn(Opcodes.ARETURN);
        }

        // return instance.callHostFunction(funcId, args);
        if (functionImports > 0) {
            asm.visitLabel(hostLabel);
            asm.visitInsn(Opcodes.POP);
            asm.visitInsn(Opcodes.POP);
            asm.visitVarInsn(Opcodes.ILOAD, 1);
            asm.visitVarInsn(Opcodes.ALOAD, 2);
            emitInvokeStatic(asm, CALL_HOST_FUNCTION);
            asm.visitInsn(Opcodes.ARETURN);
        }

        // throw new InvalidException("unknown function " + funcId);
        asm.visitLabel(defaultLabel);
        asm.visitVarInsn(Opcodes.ILOAD, 1);
        emitInvokeStatic(asm, THROW_UNKNOWN_FUNCTION);
        asm.visitInsn(Opcodes.ATHROW);

        // catch StackOverflow
        asm.visitLabel(end);
        emitInvokeStatic(asm, THROW_CALL_STACK_EXHAUSTED);
        asm.visitInsn(Opcodes.ATHROW);
    }

    private static void compileCallFunction(
            String internalClassName, int funcId, FunctionType type, MethodVisitor asm) {
        // unbox the arguments from long[]
        for (int i = 0; i < type.params().size(); i++) {
            var param = type.params().get(i);
            asm.visitVarInsn(Opcodes.ALOAD, 2);
            asm.visitLdcInsn(i);
            asm.visitInsn(Opcodes.LALOAD);
            emitLongToJvm(asm, param);
        }

        asm.visitVarInsn(Opcodes.ALOAD, 1);
        asm.visitVarInsn(Opcodes.ALOAD, 0);

        emitInvokeFunction(asm, internalClassName, funcId, type);

        // box the result into long[]
        Class<?> returnType = jvmReturnType(type);
        if (returnType == void.class) {
            asm.visitLdcInsn(0);
            asm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
        } else if (returnType != long[].class) {
            emitJvmToLong(asm, type.returns().get(0));
            asm.visitVarInsn(Opcodes.LSTORE, 3);
            asm.visitLdcInsn(1);
            asm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
            asm.visitInsn(Opcodes.DUP);
            asm.visitLdcInsn(0);
            asm.visitVarInsn(Opcodes.LLOAD, 3);
            asm.visitInsn(Opcodes.LASTORE);
        }
        asm.visitInsn(Opcodes.ARETURN);
    }

    private void compileCallIndirect(
            String internalClassName, int typeId, FunctionType type, MethodVisitor asm) {
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
        asm.visitVarInsn(Opcodes.ALOAD, instance);
        asm.visitVarInsn(Opcodes.ILOAD, tableIdx);
        emitInvokeVirtual(asm, INSTANCE_TABLE);
        asm.visitVarInsn(Opcodes.ASTORE, table);

        // int funcId = tableRef(table, funcTableIdx);
        asm.visitVarInsn(Opcodes.ALOAD, table);
        asm.visitVarInsn(Opcodes.ILOAD, funcTableIdx);
        emitInvokeStatic(asm, TABLE_REF);
        asm.visitVarInsn(Opcodes.ISTORE, funcId);

        // Instance refInstance = table.instance(funcTableIdx);
        asm.visitVarInsn(Opcodes.ALOAD, table);
        asm.visitVarInsn(Opcodes.ILOAD, funcTableIdx);
        emitInvokeVirtual(asm, TABLE_INSTANCE);
        asm.visitVarInsn(Opcodes.ASTORE, refInstance);

        Label local = new Label();
        Label other = new Label();

        // if (refInstance == null || refInstance == instance)
        asm.visitVarInsn(Opcodes.ALOAD, refInstance);
        asm.visitJumpInsn(Opcodes.IFNULL, local);
        asm.visitVarInsn(Opcodes.ALOAD, refInstance);
        asm.visitVarInsn(Opcodes.ALOAD, instance);
        asm.visitJumpInsn(Opcodes.IF_ACMPNE, other);

        // local: call function in this module
        asm.visitLabel(local);

        int slot = 0;
        for (ValueType param : type.params()) {
            asm.visitVarInsn(loadTypeOpcode(param), slot);
            slot += slotCount(param);
        }
        asm.visitVarInsn(Opcodes.ALOAD, memory);
        asm.visitVarInsn(Opcodes.ALOAD, instance);

        List<Integer> validIds = new ArrayList<>();
        for (int i = 0; i < functionTypes.size(); i++) {
            if (type.equals(functionTypes.get(i))) {
                validIds.add(i);
            }
        }

        Label invalid = new Label();
        int[] keys = validIds.stream().mapToInt(x -> x).toArray();
        Label[] labels = validIds.stream().map(x -> new Label()).toArray(Label[]::new);

        asm.visitVarInsn(Opcodes.ILOAD, funcId);
        asm.visitLookupSwitchInsn(invalid, keys, labels);

        for (int i = 0; i < validIds.size(); i++) {
            asm.visitLabel(labels[i]);
            emitInvokeFunction(asm, internalClassName, keys[i], type);
            asm.visitInsn(returnTypeOpcode(type));
        }

        asm.visitLabel(invalid);
        emitInvokeStatic(asm, THROW_INDIRECT_CALL_TYPE_MISMATCH);
        asm.visitInsn(Opcodes.ATHROW);

        // other: call function in another module
        asm.visitLabel(other);

        emitBoxArguments(asm, type.params());
        asm.visitLdcInsn(typeId);
        asm.visitVarInsn(Opcodes.ILOAD, funcId);
        asm.visitVarInsn(Opcodes.ALOAD, refInstance);

        emitInvokeStatic(asm, CALL_INDIRECT);

        emitUnboxResult(type, asm);
    }

    private static void compileHostFunction(int funcId, FunctionType type, MethodVisitor asm) {
        int slot = type.params().stream().mapToInt(AotUtil::slotCount).sum();

        asm.visitVarInsn(Opcodes.ALOAD, slot + 1); // instance
        asm.visitLdcInsn(funcId);
        emitBoxArguments(asm, type.params());

        emitInvokeStatic(asm, CALL_HOST_FUNCTION);

        emitUnboxResult(type, asm);
    }

    private static void emitBoxArguments(MethodVisitor asm, List<ValueType> types) {
        int slot = 0;
        // box the arguments into long[]
        asm.visitLdcInsn(types.size());
        asm.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG); // long
        for (int i = 0; i < types.size(); i++) {
            asm.visitInsn(Opcodes.DUP);
            asm.visitLdcInsn(i);
            ValueType valueType = types.get(i);
            asm.visitVarInsn(loadTypeOpcode(valueType), slot);
            emitJvmToLong(asm, valueType);
            asm.visitInsn(Opcodes.LASTORE);
            slot += slotCount(valueType);
        }
    }

    private static void emitUnboxResult(FunctionType type, MethodVisitor asm) {
        Class<?> returnType = jvmReturnType(type);
        if (returnType == void.class) {
            asm.visitInsn(Opcodes.RETURN);
        } else if (returnType == long[].class) {
            asm.visitInsn(Opcodes.ARETURN);
        } else {
            // unbox the result from long[0]
            asm.visitLdcInsn(0);
            asm.visitInsn(Opcodes.LALOAD);
            emitLongToJvm(asm, type.returns().get(0));
            asm.visitInsn(returnTypeOpcode(type));
        }
    }

    private void compileFunction(
            String internalClassName,
            int funcId,
            FunctionType type,
            FunctionBody body,
            MethodVisitor asm) {

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
            asm.visitVarInsn(storeTypeOpcode(localType), ctx.localSlotIndex(i));
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
                        asm.visitLabel(label);
                        visitedTargets.add(ins.operand(0));
                    }
                    break;
                case GOTO:
                    if (visitedTargets.contains(ins.operand(0))) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.operand(0)));
                    break;
                case IFEQ:
                    if (visitedTargets.contains(ins.operand(0))) {
                        throw new ChicoryException("Unexpected backward jump");
                    }
                    asm.visitJumpInsn(Opcodes.IFEQ, labels.get(ins.operand(0)));
                    break;
                case IFNE:
                    if (visitedTargets.contains(ins.operand(0))) {
                        Label skip = new Label();
                        asm.visitJumpInsn(Opcodes.IFEQ, skip);
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                        asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.operand(0)));
                        asm.visitLabel(skip);

                    } else {
                        asm.visitJumpInsn(Opcodes.IFNE, labels.get(ins.operand(0)));
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
                    asm.visitTableSwitchInsn(0, table.length - 1, defaultLabel, table);
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
