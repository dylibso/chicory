package com.dylibso.chicory.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping WIT record and variant names to their corresponding POJO classes.
 * Generated code registers POJO classes here so that TypedExportFunction can
 * automatically convert Maps/VariantValues back to POJOs when needed.
 */
public class PojoRegistry {
    private static final Map<String, Class<?>> recordClasses = new HashMap<>();
    private static final Map<String, Class<?>> variantClasses = new HashMap<>();

    public static void register(String recordName, Class<?> pojoClass) {
        recordClasses.put(recordName, pojoClass);
    }

    public static void registerVariant(String variantName, Class<?> pojoClass) {
        variantClasses.put(variantName, pojoClass);
    }

    public static Class<?> getClass(String recordName) {
        return recordClasses.get(recordName);
    }

    public static Class<?> getVariantClass(String variantName) {
        return variantClasses.get(variantName);
    }

    public static Object mapToPojo(String recordName, Map<String, Object> map) {
        Class<?> pojoClass = getClass(recordName);
        if (pojoClass == null) {
            return map;
        }

        try {
            Object instance = pojoClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                try {
                    java.lang.reflect.Field field = pojoClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(instance, fieldValue);
                } catch (NoSuchFieldException e) {
                    // Skip if field doesn't exist
                }
            }
            return instance;
        } catch (Exception e) {
            return map;
        }
    }

    public static Object variantValueToPojo(String variantName, VariantValue variantValue) {
        Class<?> variantClass = getVariantClass(variantName);
        if (variantClass == null) {
            return variantValue;
        }

        try {
            String caseName = variantValue.caseName();
            Object data = variantValue.data();

            // Handle empty case name
            if (caseName == null || caseName.isEmpty()) {
                return variantValue;
            }

            // Find the inner class that matches this case
            Class<?>[] innerClasses = variantClass.getDeclaredClasses();

            // Build expected class name: capitalize first letter
            String expectedClassName =
                    Character.toUpperCase(caseName.charAt(0))
                            + (caseName.length() > 1 ? caseName.substring(1) : "");

            for (Class<?> innerClass : innerClasses) {
                if (innerClass.getSimpleName().equals(expectedClassName)) {
                    // Found matching inner class - try to instantiate
                    try {
                        // Try with data parameter if available
                        if (data != null) {
                            try {
                                return innerClass
                                        .getDeclaredConstructor(data.getClass())
                                        .newInstance(data);
                            } catch (NoSuchMethodException e) {
                                // Try Object parameter
                                try {
                                    return innerClass
                                            .getDeclaredConstructor(Object.class)
                                            .newInstance(data);
                                } catch (NoSuchMethodException e2) {
                                    // Fall through to no-arg
                                }
                            }
                        }

                        // Try no-arg constructor
                        return innerClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        // Couldn't instantiate
                        break;
                    }
                }
            }

            // Couldn't convert - return original
            return variantValue;
        } catch (Exception e) {
            return variantValue;
        }
    }

    public static void clear() {
        recordClasses.clear();
        variantClasses.clear();
    }
}
