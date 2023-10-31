package com.dylibso.chicory.api;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum ChicoryProxyFactory {
    DYNAMIC;

    public <T> T createProxy(Class<T> iface) {

        var modulePath = deriveModulePath(iface);
        var classLoader = iface.getClassLoader();
        var moduleInstance =
                com.dylibso.chicory.runtime.Module.build(resolveModule(classLoader, modulePath))
                        .instantiate();
        var functionMethods =
                Stream.of(iface.getMethods()) //
                        .filter(method -> method.isAnnotationPresent(WasmFunction.class)) //
                        .collect(
                                Collectors.toMap(
                                        m -> m.toString().hashCode(),
                                        createExportedFunctionExtractor(moduleInstance)));
        var interfaces = new Class[] {iface, ChicoryProxy.class};
        var invocationHandler = new ChicoryProxyInvocationHandler(moduleInstance, functionMethods);
        return iface.cast(Proxy.newProxyInstance(classLoader, interfaces, invocationHandler));
    }

    public static InputStream resolveModule(ClassLoader classLoader, String modulePath) {

        var moduleStream = classLoader.getResourceAsStream(modulePath);
        if (moduleStream != null) {
            // load wasm module from classpath
            return moduleStream;
        }

        File wasmFile = new File(modulePath);
        if (wasmFile.exists() && wasmFile.isFile()) {
            try {
                // load wasm module from file
                return new FileInputStream(wasmFile);
            } catch (FileNotFoundException e) {
                throw new ChicoryException("Could not read wasm from file", e);
            }
        }

        throw new ChicoryException("Could not find wasm module from modulePath: " + modulePath);
    }

    private static Function<Method, ExportFunction> createExportedFunctionExtractor(
            Instance moduleInstance) {

        return method -> {
            var wasmFunctionAnno = method.getAnnotation(WasmFunction.class);
            var exportedFunction = moduleInstance.getExport(wasmFunctionAnno.value());

            if (exportedFunction == null) {
                throw new ChicoryException(
                        String.format("Method %s is not an exported function", method));
            }

            return exportedFunction;
        };
    }

    public static String deriveModulePath(Class<?> iface) {

        var wasmModuleAnno = iface.getAnnotation(WasmModule.class);
        if (wasmModuleAnno == null) {
            throw new IllegalArgumentException(
                    String.format("%s must be annotated with %s", iface, WasmModule.class));
        }

        var modulePath = wasmModuleAnno.value();
        if (modulePath == null) {
            throw new ChicoryException(String.format("Could not infer module path from %s", iface));
        }
        return modulePath;
    }

    static class ChicoryProxyInvocationHandler extends ObjectInvocationHandler {

        private final Instance moduleInstance;

        private final Map<Integer, ExportFunction> methodHashToExportedFunctions;

        public ChicoryProxyInvocationHandler(
                Instance moduleInstance,
                Map<Integer, ExportFunction> methodHashToExportedFunctions) {
            this.moduleInstance = moduleInstance;
            this.methodHashToExportedFunctions = methodHashToExportedFunctions;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            ExportFunction exportedFunction =
                    methodHashToExportedFunctions.get(method.toString().hashCode());

            if (exportedFunction == null) {
                // support for equals, hashcode & toString
                return super.invoke(proxy, method, args);
            }

            return invokeInternal(
                    moduleInstance,
                    exportedFunction,
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args);
        }
    }

    public interface ChicoryProxy {
        // marker interface to detect chicory proxies
    }

    public static class WasmInvocationAdapter {

        protected Object invokeInternal(
                Instance moduleInstance,
                ExportFunction exportedFunction,
                Class<?> returnType,
                Class<?>[] parameterTypes,
                Object[] args) {

            var alloc = moduleInstance.getExport("alloc");
            var memory = moduleInstance.getMemory();

            List<ValueWrapper> inputs = new ArrayList<>();
            int valueCount;
            try {
                valueCount =
                        collectInputParameterValues(parameterTypes, args, alloc, memory, inputs);
                Value[] inputValues = createValuesFromInput(valueCount, inputs);
                Value[] outputValues = exportedFunction.apply(inputValues);
                return extractOutputFromValues(returnType, outputValues);
            } finally {
                try {
                    var dealloc = moduleInstance.getExport("dealloc");
                    for (var input : inputs) {
                        if (input instanceof StringValue) {
                            var stringValue = (StringValue) input;
                            dealloc.apply(stringValue.getPtrValue(), stringValue.getLenValue());
                        }
                    }
                } catch (ChicoryException ignore) {
                    // could not dealloc
                }
            }
        }

        protected Object extractOutputFromValues(Class<?> returnType, Value[] outputValues) {
            if (int.class.equals(returnType) || Integer.class.equals(returnType)) {
                return outputValues[0].asInt();
            }

            return null;
        }

        protected Value[] createValuesFromInput(int valueCount, List<ValueWrapper> inputs) {
            Value[] inputValues = new Value[valueCount];
            int valueIndex = 0;
            for (var input : inputs) {
                if (input instanceof StringValue) {
                    var stringValue = (StringValue) input;
                    inputValues[valueIndex++] = stringValue.getPtrValue();
                    inputValues[valueIndex++] = stringValue.getLenValue();
                }
                // TODO support for add other input types
            }
            return inputValues;
        }

        protected int collectInputParameterValues(
                Class<?>[] paramTypes,
                Object[] args,
                ExportFunction alloc,
                Memory memory,
                List<? super ValueWrapper> inputs) {

            int valueCount = 0;
            for (int i = 0, n = paramTypes.length; i < n; i++) {
                Class<?> paramType = paramTypes[i];
                if (String.class.equals(paramType)) {
                    var string = (String) args[i];
                    var len = string.getBytes(StandardCharsets.UTF_8).length;
                    Value lenValue = Value.i32(len);
                    var ptr = alloc.apply(lenValue)[0].asInt();
                    Value ptrValue = Value.i32(ptr);
                    memory.put(ptr, string);

                    inputs.add(new StringValue(ptrValue, lenValue));

                    valueCount += 2;
                } else if (Number.class.isAssignableFrom(paramType)) {

                    if (Integer.class.equals(paramType)) {
                        inputs.add(
                                new RawValue(Value.i32(Long.parseLong(String.valueOf(args[i])))));
                    } else if (Long.class.equals(paramType)) {
                        inputs.add(
                                new RawValue(Value.i64(Long.parseLong(String.valueOf(args[i])))));
                    } else if (Float.class.equals(paramType)) {
                        inputs.add(
                                new RawValue(Value.f32(Long.parseLong(String.valueOf(args[i])))));
                    } else if (Double.class.equals(paramType)) {
                        inputs.add(
                                new RawValue(Value.f64(Long.parseLong(String.valueOf(args[i])))));
                    } else {
                        throw new IllegalArgumentException(
                                "Unsupported parameter type " + paramType);
                    }

                    valueCount++;
                }

                // TODO support for add other input types
            }
            return valueCount;
        }
    }

    public static class ObjectInvocationHandler extends WasmInvocationAdapter
            implements InvocationHandler {

        public static final Method HASH_CODE;

        public static final Method EQUALS;

        public static final Method TO_STRING;

        static {
            Class<Object> object = Object.class;
            try {
                HASH_CODE = object.getDeclaredMethod("hashCode");
                EQUALS = object.getDeclaredMethod("equals", object);
                TO_STRING = object.getDeclaredMethod("toString");
            } catch (NoSuchMethodException e) {
                // Never happens.
                throw new Error(e);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.equals(HASH_CODE)) {
                return objectHashCode(proxy);
            }

            if (method.equals(EQUALS)) {
                return objectEquals(proxy, args[0]);
            }

            if (method.equals(TO_STRING)) {
                return objectToString(proxy);
            }

            throw new UnsupportedOperationException(method.getName());
        }

        public String objectClassName(Object obj) {
            return obj.getClass().getName();
        }

        public int objectHashCode(Object obj) {
            return System.identityHashCode(obj);
        }

        public boolean objectEquals(Object obj, Object other) {
            return obj == other;
        }

        public String objectToString(Object obj) {
            return objectClassName(obj) + '@' + Integer.toHexString(objectHashCode(obj));
        }
    }

    public interface ValueWrapper {}

    static class RawValue implements ValueWrapper {

        private final Value value;

        public RawValue(Value value) {
            this.value = value;
        }

        public Value getValue() {
            return value;
        }
    }

    static class StringValue implements ValueWrapper {

        private final Value ptrValue;

        private final Value lenValue;

        public StringValue(Value ptrValue, Value lenValue) {
            this.ptrValue = ptrValue;
            this.lenValue = lenValue;
        }

        public Value getLenValue() {
            return lenValue;
        }

        public Value getPtrValue() {
            return ptrValue;
        }
    }
}
