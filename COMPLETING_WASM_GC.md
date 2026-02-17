
# Task: complete the Wasm GC imlpementation in Chicory

This repository: Chicory a Wasm runtime in Java, for this step we focus on the interpreter(`runtime` module).
The full tests should run with a command like `mvn clean install -pl runtime-tests -am`

This is the official WebAssembly specification, this is the source of truth when in doubt, and should be driving the implementation:
https://webassembly.github.io/spec/core/_download/WebAssembly.pdf
I also cloned the wasm-spec here: `../wasm-spec` it includes the ML reference interpreter, feel free to look up as it's the ultimate verify for correctness.

In this folder `../wasm-gc-old-spec/proposals/gc` you can get an high level overview of Wasm GC and what it encompass implementing it, remember that this is old spec, and the official spec always win, this is just as an help.

Especially interesting(since is one of the first missing things) is the validation algorithm appendix:
https://webassembly.github.io/spec/core/appendix/algorithm.html

In this branch I already implemented the parsing logic of WasmGC and improved a little on the type semantics, but is not finished.

Code should be kep completely maintainable by human being, patches or workaround should be clearly marked, and, ideally they should only be temporary to get to the end result. Staying as close as possible to the spec should be ideal.

In this folder `../wastrel` there is another Wasm runtime that implements Wasm GC, use it as a reference design.
Verify the canonicalization of Wasm GC types and do the necessary changes to match with the Specification in first place and the Wastrel implementation when in doubt.

Keep API compatibility, deprecating old methods instead of removing when necessary, but try to find a way to have a consistent and readable codebase.

While adding wast files from the testsuite be mindful, try something that only incrementally needs to be implemented on top of what we have, and keep things failing or disable tests for things that are related of development that would be done later, remember that, by the end of the process all(most of) tests should be passing with as minimal exclusions as possible.

An ideal progress would be:
- review the current types resolution(ValType.build/resolve), fix it until we have fully working canonical types(look up Wastrel), keep the current `runtime-tests` to pass
- add the validation layer, fully imlpemented according to the validation algorithm, add tests that are verifying this aspect and get them to pass
- implement, in a very principled way, pack and unpack of arrays and struct, as a first iteration you can use byte arrays to back the implementation we can get back to it later, add the relevant wast files array*.wast, struct*.wast
- carefully verify the implementation so that it uses the host GC to clean up references, an option would be to store the references in a WeakMap in the Instance(write a test to verify the correct functionality of the WeakMap, the default jvm implementation is weak on values and not keys, so we might need to roll an implementation of it)
- keep going until you pass all wast generated junit tests in runtime-tests

## Practicalities

The Maven sub modules are not building and testing in isolation(since we use Maven plugin defined in the same build), remember to re-run `mvn install` in each sub-module touched by changes or use `-pl/-am`.

Iteratively add files in the 2 sections of `includedWast` in `runtime-tests/pom.xml`, the names are from the folder `testsuite/proposals/gc`.
Recompile with mvn install to get the JUnit tests generated, verify errors and compatibility with the spec as you go along.
The end goal is to add all of the files under the gc folder.

Keep this file organized and updated with the progress of the task so that it can be used by me to monitor the progress and by you to pick up the job at a later stage.

When you are at a checkpoint, or have a consistent bulk of work, do a commit, stay on this branch `finish-wasm-gc`.

## IMPORTANT: Performance constraint
Types should NOT add computation at runtime as much as possible. Subtyping checks, type lookups, etc. should be pre-computed or cached where feasible. The hot path in the interpreter must remain fast — avoid per-opcode type section lookups when they can be resolved at validation time or during module instantiation.

## Progress

### Phase 1: Type System Foundation — IN PROGRESS
- [x] 1A: Normalized abstract heap types in ValType (TypeIdxCode expansion, constructor normalization, isReference/isValidOpcode fixes, static instances)
- [x] 1B: Implemented GC subtyping hierarchy in matchesRef() with TypeSection-aware overload
- [x] 1C: Optimized TypeSection.getSubType() with flattened SubType[] array
- [x] 1D: Fixed validateTypes() for non-legacy types (validates struct/array/func types and supertype refs)
- [ ] Verify all existing tests still pass
