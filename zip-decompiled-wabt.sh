#!/bin/bash
set -e

cd "$(dirname "$0")"
rm -f decompiled-wabt.zip

zip -r decompiled-wabt.zip \
  decompiled-wabt/src \
  decompiled-wabt/pom.xml \
  decompiled-wabt/build.sh \
  decompiled-wabt/run.sh \
  decompiled-wabt/Reproducer.md \
  -x '*.class'
