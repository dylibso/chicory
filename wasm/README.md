# wasm

This is a pure Java library that can parse Wasm binaries. This is in alpha at the moment.
We are working on coverage of the spec, tests, and the API may change. When things get settled
for a beta we will publish to maven central. For now this is available as Github source only.

## Usage

There are two ways you can interface with this library. The simplest way is to parse the whole
module using `Parser.parse`:

<!--
```java
//DEPS com.dylibso.chicory:wasm-corpus:999-SNAPSHOT
//DEPS com.dylibso.chicory:wasm:999-SNAPSHOT
```
-->

<!--
```java
var readmeResults = "readmes/wasm/current";
new File(readmeResults).mkdirs();

public void writeResultFile(String name, String content) throws Exception {
  FileWriter fileWriter = new FileWriter(new File(".").toPath().resolve(readmeResults).resolve(name).toFile());
  PrintWriter printWriter = new PrintWriter(fileWriter);
  printWriter.print(content);
  printWriter.flush();
  printWriter.close();
}
```
-->

```java
import com.dylibso.chicory.wasm.Parser;

var is = ClassLoader.getSystemClassLoader().getResourceAsStream("compiled/count_vowels.rs.wasm");
var module = Parser.parse(is);
var customSection = module.customSections().get(0);
System.out.println("First custom section: " + customSection.name());
```

<!--
```java
writeResultFile("parser-base.result", customSection.name() + "\n");
```
-->

The second is to use the `ParserListener` interface and the `parse()` method. In this mode you can also call
`includeSection(int sectionId)` for each section you wish to parse. It will skip all other
sections. This is useful for performance if you only want to parse a piece of the module.
If you don't call this method once it will parse all sections.

```java
import com.dylibso.chicory.wasm.ParserListener;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.SectionId;

var parser = new Parser();

// include the custom sections, don't call this to receive all sections
parser.includeSection(SectionId.CUSTOM);
// parser.includeSection(SectionId.CODE); // call for each section you want

String result = "";
// implement the listener
ParserListener listener = section -> {
    if (section.sectionId() == SectionId.CUSTOM) {
        var customSection = (CustomSection) section;
        var name = customSection.name();
        result += name + "\n";
        System.out.println("Got custom section with name: " + name);
    } else {
        throw new RuntimeException("Should not have received section with id: " + section.sectionId());
    }
};

// call parse()
var is = ClassLoader.getSystemClassLoader().getResourceAsStream("compiled/count_vowels.rs.wasm");
parser.parse(is, listener);
```
<!--
```java
writeResultFile("parser-listener.result", result);
```
-->
