# Regenerating WIT Java Wrapper Classes

The Chicory Component Model uses code generation to create type-safe Java wrapper classes from WIT (WebAssembly Interface Types) files. These wrapper classes (like `Person.java`, `Color.java`, `ExampleComponent.java`, etc.) enable full IDE autocomplete and compile-time type safety.

## When to Regenerate

Regenerate the wrapper classes whenever you:
- Modify the WIT file (`example.wit`)
- Add new functions, records, or variants to your component interface
- Want to keep generated code in sync with WIT definitions

## How to Regenerate

### Option 1: Run from IDE (Recommended for Development)

1. Open `RegenerateWitWrappers.java` in your IDE:  
   `wasm-component/src/test/java/com/dylibso/chicory/component/validation/RegenerateWitWrappers.java`
2. Locate the `main()` method
3. Right-click and select **Run main()** (or press Ctrl+Shift+F10 on IntelliJ)
4. Check the console output for success

### Option 2: Maven Command Line

```bash
# Regenerate from project root
mvn exec:java -Dexec.mainClass="com.dylibso.chicory.component.validation.RegenerateWitWrappers" -pl wasm-component
```

### Option 3: Bash Script

Create an executable shell script (e.g., `regen-wrappers.sh`):

```bash
#!/bin/bash
cd "$(dirname "$0")"
mvn exec:java -Dexec.mainClass="com.dylibso.chicory.component.validation.RegenerateWitWrappers" -pl wasm-component
```

Then run: `./regen-wrappers.sh`

## What Gets Generated

The tool scans for `.wit` files in the `example/` directory and generates:

- **Record classes** (POJOs): `Person.java`, `UserStatus.java`
- **Variant classes** (sealed classes): `Color.java`, `OperationResult.java`
- **Component wrapper**: `ExampleComponent.java` (type-safe SDK)

Generated files are written to: `wasm-component/src/test/java/com/example/generated/`

## Output Example

```
=== WIT Wrapper Regenerator ===

Scanning for WIT files in: example
Output root: wasm-component/src/test/java
Package name: com.example.generated

Found 1 WIT file(s):
  - example/example.wit

Regenerating from: example.wit
  ✅ Success

=== Summary ===
Successful: 1
Failed: 0

✅ All wrapper classes regenerated successfully!
Generated files are in: wasm-component/src/test/java/com/example/generated
```

## Features

- ✅ Auto-discovers all `.wit` files in the example directory
- ✅ Generates records, variants, and component wrappers
- ✅ Outputs to `src/test/java/com/example/generated/` (not committed to git)
- ✅ Clear error messages if WIT parsing fails
- ✅ No need for manual Maven plugin configuration
- ✅ Parameter names automatically converted from kebab-case to camelCase

## Troubleshooting

### Issue: "No .wit files found"

**Cause:** The tool is looking in the `example/` directory from the project root.

**Solution:** Ensure your WIT file is at `example/example.wit` or update the `WIT_DIRECTORY` constant in the regenerator.

### Issue: WIT parsing error

**Cause:** Your WIT file has syntax errors.

**Solution:** Fix the syntax errors in your WIT file and try again.

### Issue: Generated code has compilation errors

**Cause:** The WIT type contains features not yet supported by the code generator.

**Solution:** Check the error message and simplify the WIT definition or file an issue.

## Customization

To change the directories or package name, edit the constants in `RegenerateWitWrappers.java`:

```java
private static final String WIT_DIRECTORY = "example";
private static final String OUTPUT_ROOT = "wasm-component/src/test/java";
private static final String PACKAGE_NAME = "com.example.generated";
```

## IDE Integration

### IntelliJ IDEA

1. Right-click on `RegenerateWitWrappers.java`
2. Select **Create 'RegenerateWitWrappers.main()' Run Configuration**
3. (Optional) Assign a keyboard shortcut for quick access
4. Press the shortcut to regenerate anytime

### VS Code

1. Open the file in VS Code
2. Click the "Run" code lens above `main()`
3. Or use the Java Test Runner extension

## Manual Code Generation (Advanced)

If you need to generate code programmatically:

```java
import com.dylibso.chicory.component.WitParser;
import com.dylibso.chicory.component.codegen.WitToJavaGenerator;
import java.nio.file.*;

String witSource = Files.readString(Path.of("my-custom.wit"));
var definition = new WitParser().parse(witSource);
var generator = new WitToJavaGenerator(definition, Path.of("target/generated"), "com.myapp");
generator.generate();
```

## Notes

- Generated files are in `target/generated-sources/` during build and should not be manually edited
- Parameter names in the generated component wrapper are automatically converted from WIT's kebab-case (e.g., `min-age`) to Java's camelCase (e.g., `minAge`)
- The tool creates the full package directory structure, so you only need to specify the root directory
