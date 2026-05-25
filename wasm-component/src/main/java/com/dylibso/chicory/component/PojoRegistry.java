package com.dylibso.chicory.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping WIT record names to their corresponding POJO classes.
 * Generated code registers POJO classes here so that TypedExportFunction can
 * automatically convert Maps back to POJOs when needed.
 */
public class PojoRegistry {
    private static final Map<String, Class<?>> recordClasses = new HashMap<>();

    /**
     * Register a POJO class for a given WIT record name.
     * Called by generated code during initialization.
     *
     * @param recordName The WIT record name (e.g., "person")
     * @param pojoClass The corresponding POJO class (e.g., Person.class)
     */
    public static void register(String recordName, Class<?> pojoClass) {
        recordClasses.put(recordName, pojoClass);
    }

    /**
     * Get the POJO class for a given WIT record name.
     *
     * @param recordName The WIT record name
     * @return The POJO class, or null if not registered
     */
    public static Class<?> getClass(String recordName) {
        return recordClasses.get(recordName);
    }

    /**
     * Convert a Map to a POJO instance if a corresponding class is registered.
     *
     * @param recordName The WIT record name
     * @param map The Map to convert
     * @return A POJO instance, or the original Map if no class is registered
     */
    public static Object mapToPojo(String recordName, Map<String, Object> map) {
        Class<?> pojoClass = getClass(recordName);
        if (pojoClass == null) {
            return map;
        }

        try {
            // Try to find a constructor that accepts the map fields
            Object instance = pojoClass.getDeclaredConstructor().newInstance();

            // Use reflection to set fields
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
            // If conversion fails, return the map
            return map;
        }
    }

    /**
     * Clear the registry (useful for testing).
     */
    public static void clear() {
        recordClasses.clear();
    }
}
