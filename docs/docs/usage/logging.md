---
id: Logging
sidebar_position: 1
sidebar_label: Logging
---

### Logging

For maximum compatibility and to avoid external dependencies we use, by default, the JDK Platform Logging (JEP 264).
You can configure it by providing a `logging.properties` using the `java.util.logging.config.file` property and [here](https://docs.oracle.com/cd/E57471_01/bigData.100/data_processing_bdd/src/rdp_logging_config.html) you can find the possible configurations.

For more advanced configuration scenarios we encourage you to provide an alternative, compatible, adapter:

- [slf4j](https://www.slf4j.org/manual.html#jep264)
- [log4j2](https://logging.apache.org/log4j/2.x/log4j-jpl.html)

It's also possible to provide a custom `com.dylibso.chicory.log.Logger` implementation if JDK Platform Logging is not available or doesn't fit.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
docs.FileOps.writeResult("docs/usage", "logging.md.result", "empty");
```
-->
