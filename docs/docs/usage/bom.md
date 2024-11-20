---
sidebar_position: 6
sidebar_label: BOM
title: BOM
---
# Bill of Materials

To keep the versions of different Chicory artifacts aligned in your project you can use the provided
[Maven BOM file](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms).

Import it in the `dependencyManagement` section of your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.dylibso.chicory</groupId>
            <artifactId>bom</artifactId>
            <version>${chicory.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

and you can use any Chicory dependency without declaring the version number again in the build:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>runtime</artifactId>
</dependency>
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/usage", "bom.md.result", "empty");
```
-->
