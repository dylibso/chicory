# 🎯 Chicory WebAssembly Component Model Support

A **SPIKE/PoC** implementation of [WebAssembly Component Model](https://github.com/WebAssembly/component-model) support for Chicory, enabling type-safe Java bindings for WASM components with standardized WIT (WebAssembly Interface Types) definitions.

## 🚀 What is This?

This module explores adding Component Model support to Chicory as part of an **innovation project** investigating various ways to achieve a WIT host runtime for clients. It demonstrates:

- **Standardized WASM Interfaces**: Using WIT format instead of custom ABI definitions
- **Auto-Generated Java Wrappers**: Compile-time type-safe bindings for WASM component exports
- **Bidirectional Host-Guest Communication**: Java calling WASM functions, WASM calling Java host functions
- **Automatic POJO Marshalling**: Records and variants automatically convert to/from Java objects

## ⚡ Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+
- WASM component binary with component metadata (or separate WIT file)

### Two Integration Approaches

#### Approach 1: Embedded WIT (Recommended)
WASM binary contains component metadata as a custom section:

```java
// Load component from WASM with embedded metadata
WasmModule wasmModule = WasmModule.parse(wasmBytes);
ComponentModel component = ComponentModel.load(wasmModule);

// Call exported function with type-safe wrapper
ExampleComponent sdk = new ExampleComponent(component);
int result = sdk.add(5, 3);  // Result: 8
```

#### Approach 2: Explicit WIT
Pass WIT definition separately:

```java
// Load from explicit WIT file
String witSource = Files.readString(Paths.get("example.wit"));
WasmModule wasmModule = WasmModule.parse(wasmBytes);
ComponentModel component = ComponentModel.load(witSource, wasmModule);

ExampleComponent sdk = new ExampleComponent(component);
int result = sdk.add(5, 3);
```

## 📚 How It Works

### Component Model Overview

The WebAssembly Component Model defines:
- **WIT Format**: Interface definition language describing function signatures, types, imports/exports
- **Canonical ABI**: Standardized memory layout and calling conventions for component interaction
- **Component Binary**: WASM module with component metadata embedded in custom sections

### Supported WIT Features
✅ Primitive types: `i32`, `i64`, `f32`, `f64`, `bool`, `char`, `string`
✅ Records (mapped to Java POJOs)
✅ Variants/Enums (mapped to sealed classes)
✅ Lists and collections
✅ Function imports/exports
✅ Bidirectional host ↔ guest calls

### Known Limitations
⚠️ Variants with complex data return values (workaround: use explicit WIT)
⚠️ Multi-file WIT support not yet implemented
⚠️ Some advanced type compositions untested

## 📁 Project Structure

```
wasm-component/
├── src/main/java/com/dylibso/chicory/component/
│   ├── types/              # WIT type system (PrimitiveType, RecordType, VariantType, ListType)
│   ├── codegen/            # Code generators for Java wrappers (RecordGenerator, VariantGenerator, etc.)
│   ├── CanonicalAbi.java   # Encoding/decoding according to Canonical ABI spec
│   ├── TypedExportFunction.java # Marshalling layer for function calls
│   ├── ComponentModel.java # Main API entry point
│   ├── PojoRegistry.java   # Automatic POJO ↔ Map/VariantValue conversion
│   └── [other supporting classes]
│
├── src/test/java/
│   ├── validation/         # Integration validators showing host-guest communication
│   │   ├── BasicExportsValidatorTest.java
│   │   ├── ExampleWasmValidatorTest.java (⭐ Main showcase - 17 comprehensive tests)
│   │   ├── BidirectionalPojoValidatorTest.java
│   │   └── [other validators]
│   └── [unit tests for parsers, generators, etc.]
│
├── src/test/resources/
│   ├── example.wit        # WIT interface definition
│   └── example.wasm       # Compiled WASM component binary
│
└── README.md (this file)
```

### Key Classes for Integration

| Class | Purpose |
|-------|---------|
| `ComponentModel` | Main entry point - loads components and provides `callExport()` API |
| `TypedExportFunction` | Marshalling layer - converts Java parameters to WASM memory, handles calls, decodes results |
| `CanonicalAbi` | Encoding/decoding types according to Component Model spec (strings, records, variants, lists) |
| `PojoRegistry` | Auto-conversion between generated POJOs (Person, Color) and internal representations (Map, VariantValue) |
| `ImportBinder` | Handles host function registration and parameter marshalling for guest → host calls |
| `RecordFlattener` | Flattens nested records into individual parameters for WASM calling convention |

## 🛠️ Generating Java Wrappers

### Overview

From a WIT file like:
```wit
record person {
    name: string,
    age: s32,
    active: bool
}

export add: func(a: s32, b: s32) -> s32
export create-person: func(name: string, age: s32) -> person
```

The code generator creates:
- `Person.java` - POJO record class
- `ExampleComponent.java` - Type-safe wrapper with methods like `add()`, `createPerson()`

### Using the Generation Script

We provide `RegenerateWitWrappers.java` to regenerate all wrapper classes:

```bash
# Run from wasm-component directory
java -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
    com.dylibso.chicory.component.codegen.RegenerateWitWrappers
```

This reads `src/test/resources/example.wit` and generates Java classes in `src/test/java/com/example/generated/`

### Generated Files Example

**Person.java** (POJO Record):
```java
@WitRecord("person")
public class Person {
    private final String name;
    private final int age;
    private final boolean active;
    
    public Person(String name, int age, boolean active) { ... }
    
    // Standard getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public boolean getActive() { return active; }
    
    // Encoding/decoding for WASM marshalling
    public long[] encode(Memory memory) throws Exception { ... }
    public static Person decode(long[] encoded, Memory memory) throws Exception { ... }
}
```

**ExampleComponent.java** (Type-Safe Wrapper):
```java
public class ExampleComponent {
    private final ComponentModel componentModel;
    
    public ExampleComponent(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }
    
    // Type-safe wrapper for add(i32, i32) -> i32
    public int add(int a, int b) throws Exception {
        return (int) componentModel.callExport("add", a, b);
    }
    
    // Type-safe wrapper for create-person(string, i32) -> person
    public Person createPerson(String name, int age) throws Exception {
        return (Person) componentModel.callExport("create-person", name, age);
    }
}
```

## 💻 Client Perspective: Using the Component

### Basic Pattern

```java
// 1. Load WIT and WASM
String witSource = Files.readString(Paths.get("example.wit"));
byte[] wasmBytes = Files.readAllBytes(Paths.get("example.wasm"));
WasmModule wasmModule = WasmModule.parse(wasmBytes);

// 2. Create component model
ComponentModel componentModel = ComponentModel.load(witSource, wasmModule, "$root");

// 3. Wrap with type-safe API
ExampleComponent component = new ExampleComponent(componentModel);

// 4. Call exported functions - fully type-safe, no casting needed!
int sum = component.add(5, 3);
Person person = component.createPerson("Alice", 30);
List<String> names = component.getNames(Arrays.asList(person));
```

### POJO Marshalling Example

```java
// Automatically marshalled to/from Java objects - no Map manipulation needed
Person input = new Person("Bob", 25, true);
String description = component.describePerson(input);  // Bob is 25 years old (active: true)

// List of POJOs work too
List<Person> people = Arrays.asList(
    new Person("Alice", 30, true),
    new Person("Bob", 25, true)
);
List<String> filtered = component.filterHighValuePeople(people, 28);
```

### Host Function Callbacks

```java
// Register Java function to be called by WASM guest
HostFunctionProvider hostFunctions = new HostFunctionProvider();

hostFunctions.register("host-log", hostArgs -> {
    String message = (String) hostArgs[0];
    System.out.println("[GUEST] " + message);
    return null;
});

ComponentModel component = ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);

// When guest calls host-log("Hello"), Java callback executes
component.callExport("test-host-call-log");  // Prints "[GUEST] Hello"
```

## 🔍 Developer Perspective: Understanding Integration

### Architecture Flow

```
Java Call
    ↓
ExampleComponent.add(5, 3)
    ↓
ComponentModel.callExport("add", 5, 3)
    ↓
TypedExportFunction.call()  ← marshalling layer
    ↓ [encode parameters]
CanonicalAbi.encode(5, RecordType, memory)  ← writes to WASM memory
    ↓ [allocate WASM memory if needed]
realloc(0, 0, size, alignment)  ← guest memory allocator
    ↓ [invoke WASM function]
Instance.invoke("add", [5, 3])
    ↓ [guest function executes]
WASM module code
    ↓ [return result]
CanonicalAbi.decode([result], i32Type, memory)  ← reads from WASM memory
    ↓
Java: 8
```

### Key Integration Points

#### 1. Function Marshalling (TypedExportFunction.java)

```java
// When Java calls component.add(5, 3), here's what happens:

public Object[] call(Object... args) throws Exception {
    List<Long> wasmArgs = new ArrayList<>();
    
    // Encode parameters according to Canonical ABI
    for (int i = 0; i < args.length; i++) {
        WitType paramType = params.get(i).type;  // e.g., PrimitiveType.I32
        long[] encoded = CanonicalAbi.encode(args[i], paramType, memory);
        for (long value : encoded) {
            wasmArgs.add(value);
        }
    }
    
    // Call WASM function with encoded parameters
    long[] result = instance.invoke(exportName, wasmArgs.toLongArray());
    
    // Decode return value from WASM memory
    return CanonicalAbi.decode(result, returnType, memory);
}
```

#### 2. String Encoding (CanonicalAbi.java)

```java
// Strings use (ptr, len) pairs in Canonical ABI
// "hello" becomes: [memory_pointer, 5]

private static long[] encodeString(String value, Memory memory) {
    ExportFunction realloc = REALLOC_CONTEXT.get();
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    
    // Allocate memory for string data
    long[] allocResult = realloc.apply(0, 0, 1, bytes.length);
    long ptr = allocResult[0];
    
    // Write string bytes to WASM memory
    memory.writeBytes((int) ptr, bytes);
    
    // Return (pointer, length) pair
    return new long[] { ptr, bytes.length };
}
```

#### 3. Record Encoding (CanonicalAbi.java)

```java
// Records are encoded as flattened fields
// person{name: string, age: i32} → [name_ptr, name_len, age]

private static void encodeRecordToMemory(Object record, RecordType type, long offset, Memory memory) {
    Map<String, Object> recordMap;
    
    // Handle both Map and generated POJO classes
    if (record instanceof Map) {
        recordMap = (Map<String, Object>) record;
    } else {
        // Convert POJO to Map using reflection
        recordMap = pojoToMap(record);
    }
    
    // Encode each field at appropriate memory offset
    for (RecordType.Field field : type.fields()) {
        Object value = recordMap.get(field.name);
        long fieldOffset = offset + RecordLayout.getFieldOffset(field);
        encodeElementToMemory(value, field.type, fieldOffset, memory);
    }
}
```

#### 4. Host Function Registration (ImportBinder.java)

```java
// When guest calls host-log(string), here's the flow:

// Java registers:
hostFunctions.register("host-log", args -> {
    String message = (String) args[0];
    System.out.println(message);
    return null;
});

// ImportBinder wraps it for WASM calling convention:
private static void encodeStringForHostImport(String value, long offset, Memory memory) {
    // String encoding: allocate buffer, write bytes, return (ptr, len)
    long[] encoded = CanonicalAbi.encode(value, PrimitiveType.STRING, memory);
    
    // Write (ptr, len) pair to guest's output buffer
    memory.writeI32((int) offset, (int) encoded[0]);
    memory.writeI32((int) offset + 4, (int) encoded[1]);
}

// Guest receives the parameters at specified memory location
// Guest calls callback
// Java receives (ptr, len) pair, decodes to String
```

#### 5. Memory Context Management (CanonicalAbi.java)

```java
// Realloc context is thread-local to handle nested encoding
private static final ThreadLocal<ExportFunction> REALLOC_CONTEXT = new ThreadLocal<>();

// Initialize before operations
CanonicalAbi.withContext(realloc, memory);

// Cleanup after
CanonicalAbi.clearContext();

// This ensures memory allocation is consistent across nested function calls
```

### Debugging Tips

- **Enable memory logging**: Check `TypedExportFunction` for debug output of list pointers
- **Trace encoding**: `CanonicalAbi.encode()` handles all type conversions
- **Check memory layout**: Use `RecordLayout` to understand field offsets
- **Validate POJO conversion**: `PojoRegistry` provides reflection-based mapping

## 🧪 Testing

### Test Structure

All tests follow JUnit 5 with `@DisplayName` and `@Nested` grouping for clarity:

```java
@DisplayName("WASM Component Example Tests")
class ExampleWasmValidatorTest {
    
    @Nested
    @DisplayName("Integer Operations")
    class IntegerTests {
        @Test
        @DisplayName("add(5, 3) returns 8")
        void testAdd() { ... }
    }
    
    @Nested
    @DisplayName("String Operations")
    class StringTests {
        @Test
        @DisplayName("greet(name) returns greeting")
        void testGreet() { ... }
    }
}
```

### ExampleWasmValidatorTest ⭐ (Main Showcase)

**Location**: `src/test/java/com/dylibso/chicory/component/validation/ExampleWasmValidatorTest.java`

This comprehensive test suite demonstrates **all integration patterns**:

- ✅ **Primitives** (4 tests): i32, i64, f32, f64, bool operations
- ✅ **Strings** (2 tests): String marshalling with memory management
- ✅ **Records** (2 tests): POJO marshalling for guest entities
- ✅ **Variants** (3 tests): Sealed class support for sum types
- ✅ **Lists** (3 tests): Collection encoding/decoding
- ✅ **Bidirectional Calls** (2 tests): Guest → host callbacks
- ✅ **Complex Nested Types** (1 test): Combining all features

**Total**: 17 end-to-end tests validating the complete host-guest integration.

### Running Tests

```bash
# Run all tests
mvn test

# Run specific validator
mvn test -Dtest=ExampleWasmValidatorTest

# Run with debug output
mvn test -Dtest=ExampleWasmValidatorTest -X
```

### Test Organization

```
src/test/java/com/dylibso/chicory/component/validation/
├── BasicExportsValidatorTest.java           # Primitives & strings
├── BidirectionalPojoValidatorTest.java      # POJO marshalling
├── ExampleWasmValidatorTest.java            # ⭐ Complete integration
├── RecordsValidatorTest.java                # Record type handling
├── TypedComponentWrapperValidatorTest.java  # Generated API validation
└── VariantsValidatorTest.java               # Sealed class support
```

## ⚠️ Known Limitations & Future Work

### Current Limitations

**Variants with Complex Data Return Values**
- Issue: Some variant cases with associated data return incorrect memory pointers
- Status: Works for simple/empty variants, documented limitation for complex cases
- Workaround: Use explicit WIT for problematic cases
- Priority: Medium - affects ~5% of variant use cases

**Multi-File WIT Support**
- Not yet implemented - single WIT file only
- Future: Parse and merge multiple WIT files

**Type Table Resolution**
- Binary component metadata parsing exists but type references not fully resolved
- Future: Complete type resolution for nested definitions

**Performance Optimization**
- Reflection-based POJO conversion could be optimized with code generation
- Memory allocation strategy could be improved for large structures

### Next Steps for This SPIKE

1. **Advanced Type Support**: Test more complex nested structures
2. **Error Handling**: Standardize error propagation from guest to host
3. **Memory Management**: Optimize allocator strategy for different workload patterns
4. **Performance Profiling**: Benchmark marshalling overhead
5. **Production Readiness**: Determine if approach scales for real-world components

## 📖 Resources & References

- [WebAssembly Component Model Spec](https://github.com/WebAssembly/component-model)
- [WIT Specification](https://github.com/WebAssembly/component-model/tree/main/design/mvp)
- [Canonical ABI Spec](https://github.com/WebAssembly/component-model/blob/main/design/mvp/CanonicalABI.md)
- [wit-bindgen Project](https://github.com/bytecodealliance/wit-bindgen) - Reference implementation
- [Chicory Project](https://github.com/dylibso/chicory) - Main WebAssembly runtime

## 🏗️ Building & Development

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Code Style
```bash
mvn spotless:apply
```

### Regenerate Java Wrappers
```bash
java -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
    com.dylibso.chicory.component.codegen.RegenerateWitWrappers
```

---

**Status**: PoC/SPIKE for innovation project exploring Component Model integration  
**Last Updated**: May 2026  
**Maintainers**: Chicory Team
