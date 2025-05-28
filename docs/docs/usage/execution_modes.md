---
sidebar_position: 110
sidebar_label: Execution modes
title: Execution modes
---

## Overview

| Mode | Performance | Dynamic Module Loading | Requirements | Output Format | Ideal Use Case |
|---|---|---|---|---|---|
| **Interpreter** | 🐢 Slow | ✅ Supported | None | None - fully interpreted | Default mode; highly portable; suitable for development and environments requiring dynamic loading. |
| **Runtime Compilation** | 🐇 Fast | ✅ Supported | Requires reflection and ASM dependency | In-memory Java Bytecode | Enhanced performance; suitable when dynamic loading is needed and the usage of reflection is fine. |
| **Build time Compilation** | 🐇 Fast | ❌ Not Supported | Build-time tools(e.g., Maven or Gradle plugins) | Plain Java Bytecode | Optimal performance; no dynamic loading; ideal for production with static modules. |

## Summary

- **Interpreter**: Executes WebAssembly (Wasm) modules directly without prior compilation. It's the default mode in Chicory, offering maximum portability and simplicity. However, it has slower execution speed compared to compiled modes.

- **Runtime Compilation**: Compiles Wasm modules to Java bytecode at runtime, resulting in fast execution. This mode requires one additional dependency on [ASM](https://asm.ow2.io/) and uses reflection, which can complicate deployment. It fully supports dynamic module loading.

- **Build time Compilation**: Compiles Wasm modules to Java bytecode during the build process using tools like Maven or Gradle plugins. This mode offers the best performance and eliminates the need for dynamic loading and additional dependencies, making it ideal for production environments with static modules.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/usage", "execution_modes.md.result", "empty");
```
-->
