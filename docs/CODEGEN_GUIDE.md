# Code Generation Guide: WIT to Java POJOs

This guide explains how Chicory's code generation framework automatically transforms WIT (WebAssembly Interface Types) definitions into type-safe Java classes (POJOs).

## Overview

The code generation system consists of three main components:

1. **RecordGenerator** - Generates POJO classes for WIT `record` types
2. **VariantGenerator** - Generates sealed variant classes for WIT `variant` types
3. **ComponentWrapperGenerator** - Generates typed wrapper methods for component exports

## Type Mappings: WIT to Java

### Primitive Types

| WIT Type | Java Type | Size |
|----------|-----------|------|
| `bool` | `boolean` | 1 bit |
| `s8` | `byte` | 1 byte |
| `u8` | `byte` | 1 byte |
| `s16` | `short` | 2 bytes |
| `u16` | `short` | 2 bytes |
| `s32` | `int` | 4 bytes |
| `u32` | `int` | 4 bytes |
| `s64` | `long` | 8 bytes |
| `u64` | `long` | 8 bytes |
| `f32` | `float` | 4 bytes |
| `f64` | `double` | 8 bytes |
| `char` | `char` | Unicode scalar |
| `string` | `String` | UTF-8 encoded |

### Composite Types

| WIT Type | Java Type | Generated Code |
|----------|-----------|-----------------|
| `record { ... }` | `class ClassName` | Full POJO with fields, constructors, getters/setters |
| `variant { case1, case2, ... }` | `sealed abstract class` | Abstract base + inner case classes |
| `list<T>` | `java.util.List<JavaType>` | Generic list with element type |
| `option<T>` | `Optional<JavaType>` | (Phase 12.6) Optional wrapper |

### Name Conversion Rules

WIT uses kebab-case (snake-case with hyphens), Java uses camelCase:

- WIT: `record user-config`
- Java: `class UserConfig`

- WIT: `name-field: string`
- Java: `private String nameField;`

- WIT: `export describe-person`
- Java: `public void describePerson(...)`

## Phase 1: Record POJOs (Phase 12.1 - RecordGenerator)

### Example WIT Definition

```wit
record person {
  name: string,
  age: s32,
  active: bool,
}

export create-person: func(name: string, age: s32) -> person;
```

### Generated Java Code

```java
package com.example.generated;

import com.dylibso.chicory.component.annotation.WitRecord;
import com.dylibso.chicory.component.annotation.WitField;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.component.CanonicalAbi;

@WitRecord("person")
public class Person {
    @WitField(order = 0)
    private String name;
    
    @WitField(order = 1)
    private int age;
    
    @WitField(order = 2)
    private boolean active;
    
    // No-arg constructor
    public Person() {}
    
    // All-args constructor
    public Person(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public boolean getActive() { return active; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setActive(boolean active) { this.active = active; }
    
    // Encode: Java → WASM (marshal to linear memory)
    public long[] encode(Memory memory) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        map.put("active", active);
        return CanonicalAbi.encode(map, createRecordType(), memory);
    }
    
    // Decode: WASM → Java (unmarshal from linear memory)
    public static Person decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createRecordType(), memory);
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Person result = new Person();
            result.name = (String) map.get("name");
            result.age = (int) map.get("age");
            result.active = (boolean) map.get("active");
            return result;
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }
    
    // Helper to describe type to CanonicalAbi
    private static RecordType createRecordType() {
        RecordType record = new RecordType("person");
        record.addField("name", PrimitiveType.STRING);
        record.addField("age", PrimitiveType.I32);
        record.addField("active", PrimitiveType.BOOL);
        return record;
    }
}
```

### Key Features

1. **Annotations**: `@WitRecord` and `@WitField` provide reflection-based metadata
2. **Constructors**: Both no-arg (for deserialization) and all-args (for convenience)
3. **Getters/Setters**: Standard JavaBean pattern for property access
4. **Encode/Decode**: Bidirectional marshalling using CanonicalAbi
5. **Type Metadata**: `createRecordType()` helper describes the structure

### Usage in Code

**Creating and sending a POJO to WASM:**
```java
ComponentModel component = ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);

// Create POJO instance
Person person = new Person("Alice", 30, true);

// Call WASM export with POJO
// (Phase 12.2: automatic encoding)
Object result = component.callExport("create-person", "Alice", 30);
Person created = Person.decode((long[]) result, component.getMemory());
```

**Receiving a POJO from WASM:**
```java
Object result = component.callExport("get-person");
Person person = Person.decode((long[]) result, component.getMemory());
System.out.println(person.getName()); // Alice
```

## Phase 2: Variant POJOs (Phase 12.1 - VariantGenerator)

### Example WIT Definition

```wit
variant color {
  red,
  green,
  blue,
}

variant result {
  ok(string),
  err(s32),
}

export pick-color: func(index: u32) -> color;
export get-result: func() -> result;
```

### Generated Java Code (Sealed Class Pattern)

```java
package com.example.generated;

import com.dylibso.chicory.component.annotation.WitVariant;
import com.dylibso.chicory.component.annotation.WitCase;

@WitVariant("color")
public sealed abstract class Color {
    
    @WitCase(discriminant = 0)
    public static final class Red extends Color {
        public String toString() { return "Color.Red"; }
    }
    
    @WitCase(discriminant = 1)
    public static final class Green extends Color {
        public String toString() { return "Color.Green"; }
    }
    
    @WitCase(discriminant = 2)
    public static final class Blue extends Color {
        public String toString() { return "Color.Blue"; }
    }
    
    // Case name helper
    public String getCaseName() {
        if (this instanceof Red) return "red";
        if (this instanceof Green) return "green";
        if (this instanceof Blue) return "blue";
        return "unknown";
    }
    
    // Encode/decode methods (similar to records)
    public long[] encode(Memory memory) throws Exception { ... }
    public static Color decode(long[] encoded, Memory memory) throws Exception { ... }
}

// Variant with data
@WitVariant("result")
public sealed abstract class Result {
    
    @WitCase(discriminant = 0)
    public static final class Ok extends Result {
        public String value;
        public Ok(String value) { this.value = value; }
    }
    
    @WitCase(discriminant = 1)
    public static final class Err extends Result {
        public int value;
        public Err(int value) { this.value = value; }
    }
    
    public String getCaseName() { ... }
    public long[] encode(Memory memory) throws Exception { ... }
    public static Result decode(long[] encoded, Memory memory) throws Exception { ... }
}
```

### Key Features

1. **Sealed Classes**: `sealed abstract class` enforces exhaustive pattern matching
2. **Case Classes**: Inner `static final class` for each variant case
3. **Discriminant**: `@WitCase(discriminant = N)` marks the variant index
4. **Case Data**: Inner classes can hold associated data (for cases with values)
5. **Encode/Decode**: Full round-trip marshalling support

### Usage in Code

**Receiving variants from WASM:**
```java
Object result = component.callExport("pick-color", 1);
Color color = (Color) result; // Already decoded by ComponentModel

if (color instanceof Color.Green) {
    System.out.println("Green selected");
}

// Pattern matching (Java 21+)
String name = switch (color) {
    case Color.Red -> "Red";
    case Color.Green -> "Green";
    case Color.Blue -> "Blue";
};
```

**Sending variants to WASM:**
```java
Result.Ok success = new Result.Ok("Operation complete");
Object result = component.callExport("process", success);
// (Phase 12.2: auto-encoding POJOs)
```

## Phase 3: Typed Component Wrapper (Phase 12.3)

### Current vs. New Approach

**Current (Untyped):**
```java
Object result = component.callExport("filter-high-value-people", people, 28);
List<Person> filtered = (List<Person>) result;
```

**Phase 12.3 (Typed SDK):**
```java
// Auto-generated typed method with full type safety
List<Person> filtered = component.filterHighValuePeople(people, 28);
```

### Generated Component Wrapper Example

The `ComponentWrapperGenerator` will generate:

```java
public class ExampleComponentWrapper {
    private ComponentModel component;
    
    public ExampleComponentWrapper(ComponentModel component) {
        this.component = component;
    }
    
    // Typed method for each export
    public Person createPerson(String name, int age) throws Exception {
        Object result = component.callExport("create-person", name, age);
        return Person.decode((long[]) result, component.getMemory());
    }
    
    public Color pickColor(int index) throws Exception {
        Object result = component.callExport("pick-color", index);
        return (Color) result; // Already decoded
    }
    
    public List<Person> filterHighValuePeople(List<Person> people, int minAge) 
            throws Exception {
        Object result = component.callExport("filter-high-value-people", people, minAge);
        return (List<Person>) result;
    }
}
```

## Annotations Reference

### @WitRecord
Marks a class as a generated WIT record POJO.
```java
@WitRecord("person")
public class Person { ... }
```

**Uses:**
- Reflection-based code generation tools
- Runtime type inspection
- IDE support and documentation

### @WitField
Marks a field as part of a WIT record, with ordering information.
```java
@WitField(order = 0)
private String name;
```

**Attributes:**
- `order` - Field position in the WIT definition (0-based)

### @WitVariant
Marks a class as a generated WIT variant.
```java
@WitVariant("color")
public sealed abstract class Color { ... }
```

### @WitCase
Marks an inner class as a variant case.
```java
@WitCase(discriminant = 0)
public static final class Red extends Color { ... }
```

**Attributes:**
- `discriminant` - Variant case index as defined in WIT (0-based)

## Marshalling: How encode/decode Works

### Encoding (Java → WASM)

1. **Create Map** of field names → Java values
2. **Call CanonicalAbi.encode()** with:
   - The Map of field values
   - RecordType describing the structure
   - Memory context for string encoding
3. **Returns** `long[]` with encoded values and pointers

### Decoding (WASM → Java)

1. **Call CanonicalAbi.decode()** with:
   - Encoded `long[]` from WASM
   - RecordType describing the structure
   - Memory context for string decoding
2. **Receives** Map<String, Object> with decoded field values
3. **Extract fields** from Map and build Java object
4. **Return** fully-typed POJO instance

### Example Flow

```
Java Object → encode(Map) → CanonicalAbi.encode() → Linear Memory
                                                          ↓
Component Export Call
                                                          ↓
WASM Response ← CanonicalAbi.decode() ← decode(Map) ← Java Object
```


## Optional Types (Phase 12.6)

WIT supports `option<T>` for nullable values. Chicory generates `Optional<T>` fields in POJOs.

### WIT to Java Type Mapping

| WIT Type | Java Type | Usage |
|----------|-----------|-------|
| `option<s32>` | `Optional<Integer>` | May or may not have an integer |
| `option<string>` | `Optional<String>` | May or may not have a string |
| `option<record>` | `Optional<RecordName>` | May or may not have a record |
| `option<list<T>>` | `Optional<List<T>>` | May or may not have a list |

### Generated Record with Optional Fields

**WIT Definition:**
```wit
record user-config {
  timeout: option<s32>,
  api-key: option<string>,
  enabled: bool,
}
```

**Generated Java:**
```java
@WitRecord("user-config")
public class UserConfig {
    @WitField(order = 0)
    private Optional<Integer> timeout;
    
    @WitField(order = 1)
    private Optional<String> apiKey;
    
    @WitField(order = 2)
    private boolean enabled;
    
    public UserConfig(Optional<Integer> timeout, Optional<String> apiKey, boolean enabled) {
        this.timeout = timeout;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }
    
    public Optional<Integer> getTimeout() { return timeout; }
    public Optional<String> getApiKey() { return apiKey; }
    public boolean isEnabled() { return enabled; }
    
    public void setTimeout(Optional<Integer> timeout) { this.timeout = timeout; }
    public void setApiKey(Optional<String> apiKey) { this.apiKey = apiKey; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
```

### Optional Value Patterns

**Check and use with ifPresent:**
```java
config.getTimeout().ifPresent(timeout -> {
    System.out.println("Timeout: " + timeout + "ms");
});
```

**Provide default value:**
```java
int timeoutMs = config.getTimeout().orElse(5000);
```

**Transform optional value:**
```java
Optional<String> upperKey = config.getApiKey().map(String::toUpperCase);
```

**Chain operations with flatMap:**
```java
Optional<String> result = config.getApiKey()
    .filter(key -> key.length() > 5)
    .map(key -> "Valid: " + key);
```

### Encoding Optional Fields

When encoding a record with optional fields:

1. **Present value** (`Optional.of(value)`)
   - WIT tag = 1
   - Value encoded normally
   
2. **Absent value** (`Optional.empty()`)
   - WIT tag = 0
   - No value encoded (zero bytes)

### Best Practices with Optionals

1. **Never call get() without isPresent() check**
   ```java
   // ❌ Avoid - throws NoSuchElementException if empty
   String key = config.getApiKey().get();
   
   // ✅ Prefer
   String key = config.getApiKey().orElse("default");
   ```

2. **Use Optional methods instead of null checks**
   ```java
   // ❌ Avoid traditional null checking
   if (config.getApiKey() != null && config.getApiKey().isPresent()) {
       // ...
   }
   
   // ✅ Prefer Optional methods
   config.getApiKey().ifPresent(key -> {
       // use key
   });
   ```

3. **Leverage Optional for cleaner code**
   ```java
   // Chainable operations
   Optional<String> result = config.getApiKey()
       .filter(k -> !k.isEmpty())
       .map(String::trim)
       .map(k -> "API-" + k);
   ```

### Nested Optional Types

Chicory supports complex optional types:

```wit
record batch {
  items: option<list<person>>,
  metadata: option<record user-info>,
}
```

**Generated:**
```java
private Optional<List<Person>> items;
private Optional<UserInfo> metadata;

// Usage
batch.getItems().ifPresent(people -> {
    System.out.println("Processing " + people.size() + " items");
});
```

### Optional in Variant Cases

Variants can have optional data:

```wit
variant process-result {
  success(option<string>),
  error(option<s32>),
}
```

**Generated:**
```java
public sealed abstract class ProcessResult {
    public static final class Success extends ProcessResult {
        private Optional<String> data;
    }
    
    public static final class Error extends ProcessResult {
        private Optional<Integer> code;
    }
}
```

## Best Practices

### 1. Always Use POJOs for Type Safety
```java
// ❌ Avoid
Object result = component.callExport("add", 3, 5);
int sum = ((Number) result).intValue();

// ✅ Prefer (Phase 12.3)
int sum = component.add(3, 5);
```

### 2. Handle Optional Fields (Phase 12.6)
```java
public class UserConfig {
    @WitField(order = 0)
    private Optional<Integer> timeout;
    
    public Optional<Integer> getTimeout() {
        return timeout;
    }
}
```

### 3. Organize by Domain
```
generated/
  ├─ person/
  │  ├─ Person.java
  │  └─ UserConfig.java
  ├─ operations/
  │  ├─ Color.java
  │  └─ Result.java
  └─ ExampleComponentWrapper.java
```

## Troubleshooting

### Issue: "Invalid decode result type"
**Cause**: Encoded value not properly decoded by CanonicalAbi
**Solution**: Verify Memory context is passed correctly and WIT structure matches

### Issue: ClassCastException on decode
**Cause**: Field type mismatch between WIT and Java type mapping
**Solution**: Check that `witTypeToJavaType()` correctly maps WIT → Java for your types

### Issue: String fields are null or garbage
**Cause**: Memory context not passed or cleared between calls
**Solution**: Ensure `component.getMemory()` is called with active context

## Phases and Roadmap

| Phase | Feature | Status |
|-------|---------|--------|
| 11 | Code generation framework | ✅ Complete |
| 12.1 | Documentation & JavaDoc | 🔄 In Progress |
| 12.2 | Bidirectional POJO usage | 🔄 In Progress |
| 12.3 | Typed component wrapper SDK | 📅 Planned |
| 12.4 | Reorganized test suites | 📅 Planned |
| 12.5 | Multi-WIT with imports | 📅 Planned |
| 12.6 | Optional<T> type support | ✅ Complete |

## Multi-WIT Organization (Phase 12.5)

Phase 12.5 enables modular WIT development by supporting explicit `import` statements for loading types from multiple files.

### Motivation

As WIT projects grow, organizing all types in a single file becomes unwieldy. Multi-WIT support allows:
- **Separation of concerns**: Common types, operations, models in separate files
- **Code reuse**: Share type definitions across multiple world definitions
- **Maintainability**: Easier to find and modify types in modular structure

### Syntax

**File-level imports** use the syntax: `import <path>;` (without quotes or .wit extension)

```wit
// types/common.wit
record person {
  name: string,
  age: s32,
  active: bool,
}

// types/operations.wit
variant operation-result {
  ok(string),
  err(s32),
}

// main.wit
import types/common;
import types/operations;

package example:example;

world example {
  export describe-person: func(p: person) -> string;
  export process: func(op: operation-result) -> bool;
}
```

### File Organization Best Practices

```
types/
  ├─ common.wit          # Core data types (person, config, status)
  ├─ operations.wit      # Variants for operation results, colors, states
  ├─ models.wit          # Complex composites (user-batch, aggregates)
  └─ api.wit             # API-specific types (requests, responses)

main.wit               # World definition that imports all types
```

**Guideline**: Split types into separate files when:
- **Size**: Any file exceeds 100-150 lines
- **Coupling**: Types from one file are used by many others
- **Domain**: Natural domain boundaries exist (e.g., auth types vs business types)
- **Reuse**: Same types needed in multiple world definitions

### Generated Code Structure

With multi-WIT organization, generated POJOs are grouped by their source:

```
generated/
  ├─ Person.java             # from types/common.wit
  ├─ UserConfig.java
  ├─ OperationResult.java    # from types/operations.wit
  ├─ Color.java
  ├─ UserBatch.java          # from types/models.wit
  └─ ExampleComponent.java    # from main.wit world definition
```

All classes can reference each other across files due to unified type registry.

### How Import Resolution Works

1. **Parse imports** from main WIT: `import types/common;` → `types/common.wit`
2. **Load imported files** recursively (handles transitive imports)
3. **Check for circular imports** (A → B → A throws error)
4. **Merge type registries** from all files
5. **Parse world definition** from main WIT using merged types

### Example: Transitive Imports

```wit
// types/common.wit
record person { ... }

// types/models.wit
import types/common;
record user-batch {
  people: list<person>,
  ...
}

// main.wit
import types/models;
// person type is available even though not directly imported!
world example {
  export batch-process: func(batch: user-batch) -> s32;
  // user-batch can use person from transitive import
}
```

### Error Handling

**Circular Imports**: Detected and reported
```
IllegalArgumentException: Circular import detected: types/a.wit
```

**Missing Files**: Clear error message
```
IllegalArgumentException: Imported file not found: types/missing (resolved to: types/missing.wit)
```

### Code Generation Integration

The existing generators (RecordGenerator, VariantGenerator, ComponentWrapperGenerator) work seamlessly with multi-WIT:

```java
Map<String, String> witFiles = new HashMap<>();
witFiles.put("main.wit", mainWitContent);
witFiles.put("types/common.wit", commonWitContent);
witFiles.put("types/operations.wit", operationsWitContent);

WitParser parser = new WitParser();
ComponentDefinition definition = parser.parseMultiFile(
    witFiles.get("main.wit"),
    witFiles,
    Paths.get("."));

// All types from all files are available
RecordGenerator rg = new RecordGenerator(definition);
rg.generateAllRecords(); // Generates Person, UserBatch, etc.
```

### Limitations & Not Included

The following features are deferred to future phases:

- ❌ **Interface organization**: WASI-style `interface name { ... }`
- ❌ **Package namespacing**: `wasi:cli@version` versioned packages
- ❌ **Type visibility**: Private/public type exports
- ❌ **Circular dependency resolution**: You must avoid circular imports
- ❌ **Network imports**: Cannot import from HTTP/URLs


## Further Reading

- [WIT Component Model Spec](https://component-model.bytecodealliance.org/)
- [CanonicalAbi Implementation](../wasm-component/src/main/java/com/dylibso/chicory/component/CanonicalAbi.java)
- [ComponentModel Implementation](../wasm-component/src/main/java/com/dylibso/chicory/component/ComponentModel.java)
- [Generated POJOs Example](../wasm-component/src/test/java/com/example/generated/)
