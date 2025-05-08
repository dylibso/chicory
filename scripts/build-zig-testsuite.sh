#! /bin/bash
set -x

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

ZIG_INSTALL="zig-install"
ZIG_VERSION="0.14.0" # TODO: update me

ZIG_SOURCE="zig-source"

BINARYEN_VERSION="123"
BINARYEN_INSTALL="binaryen-install"

ZIG_TESTSUITE="zig-testsuite"

# Install Zig 
if [ ! -d "$ZIG_INSTALL" ]; then
    mkdir -p ${ZIG_INSTALL}
    curl -sSL https://ziglang.org/download/${ZIG_VERSION}/zig-linux-x86_64-${ZIG_VERSION}.tar.xz | tar -xJ --strip-components=1 -C ${ZIG_INSTALL}
fi

# Install Zig source
if [ ! -d "$ZIG_SOURCE" ]; then
    mkdir -p ${ZIG_SOURCE}
    curl -sSL https://ziglang.org/download/${ZIG_VERSION}/zig-${ZIG_VERSION}.tar.xz | tar -xJ --strip-components=1 -C ${ZIG_SOURCE}
fi

#Install Binaryen
if [ ! -d "$BINARYEN_INSTALL" ]; then
    mkdir -p ${BINARYEN_INSTALL}
    curl -sSL https://github.com/WebAssembly/binaryen/releases/download/version_${BINARYEN_VERSION}/binaryen-version_${BINARYEN_VERSION}-x86_64-linux.tar.gz | tar -xz --strip-components=1 -C ${BINARYEN_INSTALL}
fi

PATH=${PWD}/${ZIG_INSTALL}:${PWD}/${BINARYEN_INSTALL}/bin:$PATH

# --test-no-exec allows building of the test Wasm binary without executing command.
(
    cd ${ZIG_SOURCE} && \
        rm lib/compiler/test_runner.zig && \
        cp ${SCRIPT_DIR}/hack/test_runner.zig lib/compiler/test_runner.zig && \
        zig test --test-no-exec -target wasm32-wasi --zig-lib-dir ./lib ./lib/std/std.zig
)

mkdir -p ${ZIG_TESTSUITE}
# We use find because the test.wasm will be something like ./zig-cache/o/dd6df1361b2134adc5eee9d027495436/test.wasm
cp $(find ${PWD}/${ZIG_SOURCE} -name test.wasm) ${PWD}/${ZIG_TESTSUITE}/test.wasm

# The generated test binary is large and produces skewed results in favor of the optimized compiler.
# We also generate a stripped, optimized binary with wasm-opt.
wasm-opt ${PWD}/${ZIG_TESTSUITE}/test.wasm -O3 --strip-dwarf -o ${PWD}/${ZIG_TESTSUITE}/test-opt.wasm
