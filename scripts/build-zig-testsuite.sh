#! /bin/bash
set -eoux pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

ZIG_INSTALL="zig-install"
ZIG_VERSION="0.14.0"

ZIG_SOURCE="zig-source"

BINARYEN_VERSION="123"
BINARYEN_INSTALL="binaryen-install"

ZIG_TESTSUITE="zig-testsuite"

ZIG_MIRROR=$(${SCRIPT_DIR}/pick-zig-mirror.sh)

# Install Zig 
if [ ! -d "$ZIG_INSTALL" ]; then
    mkdir -p ${ZIG_INSTALL}

    ARCHIVE=zig-linux-x86_64-${ZIG_VERSION}.tar.xz
    curl -sSL "${ZIG_MIRROR}/${ARCHIVE}?source=github-chicory-nightly" -o "${ARCHIVE}"
    echo "473ec26806133cf4d1918caf1a410f8403a13d979726a9045b421b685031a982 ${ARCHIVE}" | sha256sum -c -
    tar -xJ --strip-components=1 -C "${ZIG_INSTALL}" -f "${ARCHIVE}"
fi

# Install Zig source
if [ ! -d "$ZIG_SOURCE" ]; then
    mkdir -p ${ZIG_SOURCE}

    ARCHIVE=zig-${ZIG_VERSION}.tar.xz
    curl -sSL "${ZIG_MIRROR}/${ARCHIVE}?source=github-chicory-nightly" -o "${ARCHIVE}"
    echo "c76638c03eb204c4432ae092f6fa07c208567e110fbd4d862d131a7332584046 ${ARCHIVE}" | sha256sum -c -
    tar -xJ --strip-components=1 -C "${ZIG_SOURCE}" -f "${ARCHIVE}"
fi

#Install Binaryen
if [ ! -d "$BINARYEN_INSTALL" ]; then
    mkdir -p ${BINARYEN_INSTALL}

    ARCHIVE=binaryen-version_${BINARYEN_VERSION}-x86_64-linux.tar.gz
    curl -sSL "https://github.com/WebAssembly/binaryen/releases/download/version_${BINARYEN_VERSION}/${ARCHIVE}" -o "${ARCHIVE}"
    echo "e959f2170af4c20c552e9de3a0253704d6a9d2766e8fdb88e4d6ac4bae9388fe ${ARCHIVE}" | sha256sum -c -
    tar -xz --strip-components=1 -C "${BINARYEN_INSTALL}" -f "${ARCHIVE}"
fi

PATH=${PWD}/${ZIG_INSTALL}:${PWD}/${BINARYEN_INSTALL}/bin:$PATH

# --test-no-exec allows building of the test Wasm binary without executing command.
(
    cd ${ZIG_SOURCE} && \
        patch --fuzz=0 --batch ./lib/compiler/test_runner.zig < ${SCRIPT_DIR}/patch/test_runner.patch && \
        zig test --test-no-exec -target wasm32-wasi --zig-lib-dir ./lib ./lib/std/std.zig
)

mkdir -p ${ZIG_TESTSUITE}
# We use find because the test.wasm will be something like ./zig-cache/o/dd6df1361b2134adc5eee9d027495436/test.wasm
cp $(find ${PWD}/${ZIG_SOURCE} -name test.wasm) ${PWD}/${ZIG_TESTSUITE}/test.wasm

# The generated test binary is large and produces skewed results in favor of the optimized compiler.
# We also generate a stripped, optimized binary with wasm-opt.
wasm-opt ${PWD}/${ZIG_TESTSUITE}/test.wasm -O3 --strip-dwarf -o ${PWD}/${ZIG_TESTSUITE}/test-opt.wasm
