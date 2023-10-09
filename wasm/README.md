# wasmparser

This is a pure Java library that can parse Wasm binaries. This is in alpha at the moment.
We are working on coverage of the spec, tests, and the API may change. When things get settled
for a beta we will publish to maven central. For now this is available as Github source only.

## Usage

There are two ways you can interface with this library. The simplest way is to parse the whole
module using `parseModule`:

```java
var parser = new Parser("/tmp/code.wasm");
var module = parser.parseModule();
var customSection = module.getCustomSections()[0];
System.out.println("First custom section: " + customSection.getName());
```

The second is to use the `ParserListener` interface and the `parse()` method. In this mode you can also call
`includeSection(int sectionId)` for each section you wish to parse. It will skip all other
sections. This is useful for performance if you only want to parse a piece of the module.
If you don't call this method once it will parse all sections.

```java
var parser = new Parser("/tmp/code.wasm");

// include the custom sections, don't call this to receive all sections
parser.includeSection(SectionId.CUSTOM);
// parser.includeSection(SectionId.CODE); // call for each section you want

// implement the listener
parser.setListener(section -> {
    if (section.getSectionId() == SectionId.CUSTOM) {
        var customSection = (CustomSection) section;
        var name = customSection.getName();
        System.out.println("Got custom section with name: " + name);
    } else {
        fail("Should not have received section with id: " + section.getSectionId());
    }
});

// call parse() instead of parseModule()
parser.parse();
```
