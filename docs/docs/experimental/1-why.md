---
sidebar_position: 1
sidebar_label: Why
title: Why
---
# Why?

Chicory is a young project and we acknowledge that we are exploring and spearheading in many aspects the usage of Web Assembly on the JVM.

Since there is(always) some degree of experimentation going on, and we want to have feedback by the community and early users, we decide to publish also `experimental` modules; everyone is welcome to try things out and report back the experience.

Please note that if "something works" for you, its very unlekely that it will be removed completely, in most cases, expect slight public API changes or modules rename to happen.

The goal of having `experimental` modules is because they are not stabilized, we are not 100% confident in the design and we want to be able to perform breaking changes without respecting SemVer.

This includes renaming artifactIDs, classes, methods and rework their usage according to the feedback and development progress.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/experimental", "1-why.md.result", "empty");
```
-->
