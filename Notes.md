
- issues:

changing those lines:
    // const have_tty = std.io.getStdErr().isTty();
    const have_tty = false;

in zig-source/lib/compiler/test_runner.zig

results in:
com.dylibso.chicory.runtime.WasmRuntimeException: out of bounds memory access: attempted to access address: 1953719668 but limit is: 31260672 and size: 4