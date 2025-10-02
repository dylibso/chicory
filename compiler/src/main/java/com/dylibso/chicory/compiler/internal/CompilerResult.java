package com.dylibso.chicory.compiler.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public final class CompilerResult implements Serializable {

    private ClassCollector collector;
    private Set<Integer> interpretedFunctions;

    public CompilerResult(ClassCollector collector, Set<Integer> interpretedFunctions) {
        this.collector = collector;
        this.interpretedFunctions = interpretedFunctions;
    }

    public Map<String, byte[]> classBytes() {
        return collector.classBytes();
    }

    public ClassCollector collector() {
        return this.collector;
    }

    public Set<Integer> interpretedFunctions() {
        return interpretedFunctions;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Write collector type and data
        if (collector instanceof ClassLoadingCollector) {
            oos.writeUTF("ClassLoadingCollector");
            ClassLoadingCollector clc = (ClassLoadingCollector) collector;
            oos.writeObject(clc.classBytes());
            oos.writeObject(clc.mainClassName());
        } else if (collector instanceof ByteClassCollector) {
            oos.writeUTF("ByteClassCollector");
            ByteClassCollector bcc = (ByteClassCollector) collector;
            oos.writeObject(bcc.classBytes());
            oos.writeObject(bcc.mainClassName());
        } else {
            oos.writeUTF("Unknown");
            oos.writeObject(collector.classBytes());
            oos.writeObject(collector.mainClassName());
        }

        // Write interpreted functions set
        oos.writeObject(interpretedFunctions);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // Read collector type and reconstruct collector
        String collectorType = ois.readUTF();
        @SuppressWarnings("unchecked")
        Map<String, byte[]> classBytes = (Map<String, byte[]>) ois.readObject();
        String mainClassName = (String) ois.readObject();

        ClassCollector reconstructedCollector;
        if ("ClassLoadingCollector".equals(collectorType)
                || "ByteClassCollector".equals(collectorType)) {
            reconstructedCollector = new ClassLoadingCollector();
            if (mainClassName != null) {
                reconstructedCollector.putMainClass(mainClassName, classBytes.get(mainClassName));
            }
            for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
                if (!entry.getKey().equals(mainClassName)) {
                    reconstructedCollector.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            // Fallback to ByteClassCollector for unknown types
            reconstructedCollector = new ByteClassCollector();
            if (mainClassName != null) {
                reconstructedCollector.putMainClass(mainClassName, classBytes.get(mainClassName));
            }
            for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
                if (!entry.getKey().equals(mainClassName)) {
                    reconstructedCollector.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Read interpreted functions set
        @SuppressWarnings("unchecked")
        Set<Integer> reconstructedInterpretedFunctions = (Set<Integer>) ois.readObject();

        // Assign reconstructed fields directly (no reflection)
        this.collector = reconstructedCollector;
        this.interpretedFunctions = reconstructedInterpretedFunctions;
    }
}
