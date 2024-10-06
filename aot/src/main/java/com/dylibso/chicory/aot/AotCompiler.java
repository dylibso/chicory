package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotEmitterMap.EMITTERS;
import static com.dylibso.chicory.aot.AotMethodRefs.CALL_INDIRECT;
import static com.dylibso.chicory.aot.AotMethodRefs.CHECK_INTERRUPTION;
import static com.dylibso.chicory.aot.AotMethodRefs.INSTANCE_CALL_HOST_FUNCTION;
import static com.dylibso.chicory.aot.AotMethodRefs.INSTANCE_MEMORY;
import static com.dylibso.chicory.aot.AotMethodRefs.INSTANCE_TABLE;
import static com.dylibso.chicory.aot.AotMethodRefs.TABLE_INSTANCE;
import static com.dylibso.chicory.aot.AotMethodRefs.TABLE_REF;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_CALL_STACK_EXHAUSTED;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_INDIRECT_CALL_TYPE_MISMATCH;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_TRAP_EXCEPTION;
import static com.dylibso.chicory.aot.AotMethodRefs.THROW_UNKNOWN_FUNCTION;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodName;
import static com.dylibso.chicory.aot.AotUtil.callIndirectMethodType;
import static com.dylibso.chicory.aot.AotUtil.defaultValue;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeFunction;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeStatic;
import static com.dylibso.chicory.aot.AotUtil.emitInvokeVirtual;
import static com.dylibso.chicory.aot.AotUtil.emitJvmToLong;
import static com.dylibso.chicory.aot.AotUtil.emitLongToJvm;
import static com.dylibso.chicory.aot.AotUtil.emitPop;
import static com.dylibso.chicory.aot.AotUtil.jvmReturnType;
import static com.dylibso.chicory.aot.AotUtil.jvmTypes;
import static com.dylibso.chicory.aot.AotUtil.loadTypeOpcode;
import static com.dylibso.chicory.aot.AotUtil.localType;
import static com.dylibso.chicory.aot.AotUtil.methodNameFor;
import static com.dylibso.chicory.aot.AotUtil.methodTypeFor;
import static com.dylibso.chicory.aot.AotUtil.slotCount;
import static com.dylibso.chicory.aot.AotUtil.storeTypeOpcode;
import static com.dylibso.chicory.wasm.types.Instruction.EMPTY_OPERANDS;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.objectweb.asm.Type.VOID_TYPE;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.PrintWriter;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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

    private static final Instruction FUNCTION_SCOPE =
            new Instruction(-1, OpCode.NOP, EMPTY_OPERANDS);

    private static final MethodType CALL_METHOD_TYPE =
            methodType(long[].class, Instance.class, Memory.class, long[].class);

    private final Module module;
    private final List<ValueType> globalTypes;
    private final int functionImports;
    private final List<FunctionType> functionTypes;

    private AotCompiler(Module module) {
        this.module = requireNonNull(module, "module");
        this.globalTypes = getGlobalTypes(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
        this.functionTypes = getFunctionTypes(module);
    }

    public static CompilerResult compileModule(Module module) {
        return compileModule(module, DEFAULT_CLASS_NAME);
    }

    public static CompilerResult compileModule(Module module, String className) {
        var compiler = new AotCompiler(module);
        var bytes = compiler.compileClass(className, module.functionSection());
        var factory = compiler.createMachineFactory(bytes);
        return new CompilerResult(factory, new TreeMap<>(Map.of(className, bytes)));
    }

    private static List<ValueType> getGlobalTypes(Module module) {
        var importedGlobals =
                module.importSection().stream()
                        .filter(GlobalImport.class::isInstance)
                        .map(GlobalImport.class::cast)
                        .map(GlobalImport::type);

        var globals = Stream.of(module.globalSection().globals()).map(Global::valueType);

        return Stream.concat(importedGlobals, globals).collect(toUnmodifiableList());
    }

    private static List<FunctionType> getFunctionTypes(Module module) {
        var importedFunctions =
                module.importSection().stream()
                        .filter(FunctionImport.class::isInstance)
                        .map(FunctionImport.class::cast)
                        .map(function -> module.typeSection().types()[function.typeIndex()]);

        var functions = module.functionSection();
        var moduleFunctions =
                IntStream.range(0, functions.functionCount())
                        .mapToObj(i -> functions.getFunctionType(i, module.typeSection()));

        return Stream.concat(importedFunctions, moduleFunctions).collect(toUnmodifiableList());
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
            var classLoader = new ByteArrayClassLoader(getClass().getClassLoader());
            var clazz = classLoader.loadFromBytes(classBytes);
            // force initialization to run JVM verifier
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        } catch (VerifyError e) {
            // run ASM verifier to help with debugging
            try {
                var writer = new PrintWriter(System.out, false, UTF_8);
                CheckClassAdapter.verify(new ClassReader(classBytes), true, writer);
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
    }

    private byte[] compileClass(String className, FunctionSection functions) {
        var internalClassName = className.replace('.', '/');

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                internalClassName,
                null,
                getInternalName(Object.class),
                new String[] {getInternalName(Machine.class)});

        classWriter.visitSource("wasm", "wasm");

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
            return classWriter.toByteArray();
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
            emitInvokeVirtual(asm, INSTANCE_CALL_HOST_FUNCTION);
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

        emitInvokeVirtual(asm, INSTANCE_CALL_HOST_FUNCTION);

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
                        globalTypes,
                        functionTypes,
                        module.typeSection().types(),
                        funcId,
                        type,
                        body);

        // initialize local variables to their default values
        int localsCount = type.params().size() + body.localTypes().size();
        for (int i = type.params().size(); i < localsCount; i++) {
            var localType = localType(type, body, i);
            asm.visitLdcInsn(defaultValue(localType));
            asm.visitVarInsn(storeTypeOpcode(localType), ctx.localSlotIndex(i));
        }

        // allocate labels for all label targets
        Map<Integer, Label> labels = new HashMap<>();
        for (AnnotatedInstruction ins : body.instructions()) {
            if (ins.labelTrue() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.put(ins.labelTrue(), new Label());
            }
            if (ins.labelFalse() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.put(ins.labelFalse(), new Label());
            }
            for (int label : ins.labelTable()) {
                labels.put(label, new Label());
            }
        }

        // fake instruction to use for the function's implicit block
        ctx.enterScope(FUNCTION_SCOPE, FunctionType.of(List.of(), type.returns()));

        // compile the function body
        int exitBlockDepth = -1;
        for (int idx = 0; idx < body.instructions().size(); idx++) {
            var ins = body.instructions().get(idx);

            Label label = labels.get(idx);
            if (label != null) {
                asm.visitLabel(label);
            }

            // skip instructions after unconditional control transfer
            if (exitBlockDepth >= 0) {
                if (ins.depth() > exitBlockDepth) {
                    continue;
                }
                if (ins.opcode() != OpCode.ELSE && ins.opcode() != OpCode.END) {
                    continue;
                }

                exitBlockDepth = -1;
                if (ins.opcode() == OpCode.END) {
                    ctx.scopeRestoreStackSize();
                }
            }

            switch (ins.opcode()) {
                case NOP:
                    break;
                case BLOCK:
                case LOOP:
                    ctx.enterScope(ins.scope(), blockType(ins));
                    break;
                case END:
                    ctx.exitScope(ins.scope());
                    break;
                case UNREACHABLE:
                    exitBlockDepth = ins.depth();
                    emitInvokeStatic(asm, THROW_TRAP_EXCEPTION);
                    asm.visitInsn(Opcodes.ATHROW);
                    break;
                case RETURN:
                    exitBlockDepth = ins.depth();
                    emitReturn(asm, type, ctx);
                    break;
                case IF:
                    ctx.popStackSize();
                    ctx.enterScope(ins.scope(), blockType(ins));
                    asm.visitJumpInsn(Opcodes.IFEQ, labels.get(ins.labelFalse()));
                    // use the same starting stack sizes for both sides of the branch
                    if (body.instructions().get(ins.labelFalse() - 1).opcode() == OpCode.ELSE) {
                        ctx.pushStackSizesStack();
                    }
                    break;
                case ELSE:
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    ctx.popStackSizesStack();
                    break;
                case BR:
                    exitBlockDepth = ins.depth();
                    if (ins.labelTrue() < idx) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    emitUnwindStack(asm, type, body, ins, ins.labelTrue(), ctx);
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    break;
                case BR_IF:
                    ctx.popStackSize();
                    Label falseLabel = new Label();
                    asm.visitJumpInsn(Opcodes.IFEQ, falseLabel);
                    if (ins.labelTrue() < idx) {
                        emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    }
                    emitUnwindStack(asm, type, body, ins, ins.labelTrue(), ctx);
                    asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTrue()));
                    asm.visitLabel(falseLabel);
                    break;
                case BR_TABLE:
                    exitBlockDepth = ins.depth();
                    ctx.popStackSize();
                    emitInvokeStatic(asm, CHECK_INTERRUPTION);
                    // skip table switch if it only has a default
                    if (ins.labelTable().size() == 1) {
                        asm.visitInsn(Opcodes.POP);
                        emitUnwindStack(asm, type, body, ins, ins.labelTable().get(0), ctx);
                        asm.visitJumpInsn(Opcodes.GOTO, labels.get(ins.labelTable().get(0)));
                        break;
                    }
                    // collect unique target labels
                    Map<Integer, Label> targets = new TreeMap<>();
                    Label[] table = new Label[ins.labelTable().size() - 1];
                    for (int i = 0; i < table.length; i++) {
                        table[i] =
                                targets.computeIfAbsent(ins.labelTable().get(i), x -> new Label());
                    }
                    // table switch using the last entry of the label table as the default
                    int defaultTarget = ins.labelTable().get(ins.labelTable().size() - 1);
                    Label defaultLabel = targets.computeIfAbsent(defaultTarget, x -> new Label());
                    asm.visitTableSwitchInsn(0, table.length - 1, defaultLabel, table);
                    // generate separate unwinds for each target
                    targets.forEach(
                            (target, tableLabel) -> {
                                asm.visitLabel(tableLabel);
                                emitUnwindStack(asm, type, body, ins, target, ctx);
                                asm.visitJumpInsn(Opcodes.GOTO, labels.get(target));
                            });
                    break;
                default:
                    var emitter = EMITTERS.get(ins.opcode());
                    if (emitter == null) {
                        throw new ChicoryException(
                                "JVM compilation failed: opcode is not supported: " + ins.opcode());
                    }
                    emitter.emit(ctx, ins, asm);
            }
        }

        // implicit return at end of function
        emitReturn(asm, type, ctx);

        if (ctx.stackSizesStack().size() != 1) {
            throw new RuntimeException("Bad stack sizes stack: " + ctx.stackSizesStack());
        }
        if (!ctx.stackSizes().isEmpty()) {
            throw new RuntimeException("Stack sizes not empty: " + ctx.stackSizes());
        }
    }

    private static void emitReturn(MethodVisitor asm, FunctionType type, AotContext ctx) {
        if (type.returns().size() > 1) {
            asm.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    ctx.internalClassName(),
                    valueMethodName(type.returns()),
                    valueMethodType(type.returns()).toMethodDescriptorString(),
                    false);
        }
        asm.visitInsn(returnTypeOpcode(type));
        for (int i = 0; i < type.returns().size(); i++) {
            ctx.popStackSize();
        }
    }

    private void emitUnwindStack(
            MethodVisitor asm,
            FunctionType functionType,
            FunctionBody body,
            AnnotatedInstruction ins,
            int label,
            AotContext ctx) {

        boolean forward = true;
        var target = body.instructions().get(label);
        if (target.address() <= ins.address()) {
            // the loop block is the instruction before the target
            target = body.instructions().get(label - 1);
            forward = false;
        }
        var scope = target.scope();

        FunctionType blockType;
        if (scope.opcode() == OpCode.END) {
            // special scope for the function's implicit block
            scope = FUNCTION_SCOPE;
            blockType = functionType;
        } else {
            blockType = blockType(scope);
        }

        var types = forward ? blockType.returns() : blockType.params();

        // for a backward jump, the initial loop parameters are dropped
        var stackSizes = new ArrayDeque<>(ctx.stackSizes());
        int dropCount = stackSizes.size() - ctx.scopeStackSize(scope);

        // do not drop the return values for a forward jump
        if (forward) {
            dropCount -= types.size();
        }

        if (dropCount == 0) {
            return;
        }

        // save result values
        int slot = ctx.tempSlot();
        for (int i = types.size() - 1; i >= 0; i--) {
            ValueType type = types.get(i);
            asm.visitVarInsn(storeTypeOpcode(type), slot);
            slot += slotCount(type);
            stackSizes.pop();
        }

        // drop intervening values
        for (int i = 0; i < dropCount; i++) {
            emitPop(asm, stackSizes.pop());
        }

        // restore result values
        for (ValueType type : types) {
            slot -= slotCount(type);
            asm.visitVarInsn(loadTypeOpcode(type), slot);
        }
    }

    private FunctionType blockType(Instruction ins) {
        var typeId = (int) ins.operand(0);
        if (typeId == 0x40) {
            return FunctionType.empty();
        }
        if (ValueType.isValid(typeId)) {
            return FunctionType.returning(ValueType.forId(typeId));
        }
        return module.typeSection().types()[typeId];
    }

    public static String callMethodName(int functId) {
        return "call_" + functId;
    }

    private static MethodType valueMethodType(List<ValueType> types) {
        return methodType(long[].class, jvmTypes(types));
    }

    private static String valueMethodName(List<ValueType> types) {
        return "value_"
                + types.stream()
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .collect(joining("_"));
    }

    private static int returnTypeOpcode(FunctionType type) {
        Class<?> returnType = jvmReturnType(type);
        if (returnType == long[].class) {
            return Opcodes.ARETURN;
        }
        if (returnType == int.class) {
            return Opcodes.IRETURN;
        }
        if (returnType == long.class) {
            return Opcodes.LRETURN;
        }
        if (returnType == float.class) {
            return Opcodes.FRETURN;
        }
        if (returnType == double.class) {
            return Opcodes.DRETURN;
        }
        if (returnType == void.class) {
            return Opcodes.RETURN;
        }
        throw new ChicoryException("Unsupported return type: " + returnType.getName());
    }
}
