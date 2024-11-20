---
sidebar_position: 4
sidebar_label: CLI
title: CLI
---
# Install and use the CLI

The experimental Chicory CLI is available for download on Maven at the link:

```
https://repo1.maven.org/maven2/com/dylibso/chicory/cli/<version>/cli-<version>.sh
```

you can download the latest version and use it locally by typing:

```bash
export VERSION=$(curl -sS https://api.github.com/repos/dylibso/chicory/tags --header "Accept: application/json" | jq -r '.[0].name')
curl -L -o chicory https://repo1.maven.org/maven2/com/dylibso/chicory/cli-experimental/${VERSION}/cli-experimental-${VERSION}.sh
chmod a+x chicory
./chicory
```

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT

docs.FileOps.writeResult("docs/experimental", "4-cli.md.result", "empty");
```
-->
