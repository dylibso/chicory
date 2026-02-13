#!/usr/bin/env python3
"""Analyze repetitive patterns in the too-large methods to estimate
whether extracting common patterns into helpers could save enough bytecode."""

import re

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_func_end(lines, start_idx):
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    funcs = [(2651, "func_41"), (84257, "func_801"), (93437, "func_804")]

    for start_line, name in funcs:
        start_idx = start_line - 1
        end_idx = find_func_end(lines, start_idx)
        body = "".join(lines[start_idx:end_idx+1])

        # Pattern: (int) X < 0 ? X : X + N  (address calculation)
        addr_pattern = r'\(int\)\s*\S+\s*<\s*0\s*\?\s*\S+\s*:\s*\S+\s*\+\s*\d+'
        addr_count = len(re.findall(addr_pattern, body))

        # Pattern: memory.readInt(...)
        read_int_count = body.count('memory.readInt(')
        read_count = body.count('memory.read(')
        write_i32_count = body.count('memory.writeI32(')
        write_long_count = body.count('memory.writeLong(')

        # Pattern: instance.getMachine().call(
        call_count = body.count('instance.getMachine().call(')

        # Pattern: long[] callArgs_
        call_args_count = len(re.findall(r'long\[\] callArgs_', body))

        # Pattern: OpcodeImpl.
        opcode_impl_count = body.count('OpcodeImpl.')

        # Pattern: instance.global(
        global_count = body.count('instance.global(')

        size = end_idx - start_idx + 1
        print(f"\n{name} ({size} lines):")
        print(f"  Address calculations (X < 0 ? X : X + N): {addr_count}")
        print(f"  memory.readInt: {read_int_count}")
        print(f"  memory.read: {read_count}")
        print(f"  memory.writeI32: {write_i32_count}")
        print(f"  memory.writeLong: {write_long_count}")
        print(f"  instance.getMachine().call: {call_count}")
        print(f"  callArgs array allocations: {call_args_count}")
        print(f"  OpcodeImpl calls: {opcode_impl_count}")
        print(f"  instance.global: {global_count}")

        # Estimate bytecode savings from extracting addr pattern:
        # Inline: ~10 bytecodes per occurrence
        # Helper: ~4 bytecodes per occurrence
        # Savings: ~6 bytecodes * count
        savings = addr_count * 6
        print(f"  Estimated bytecode savings from addr helper: ~{savings} bytes")
        print(f"  (64KB limit = 65536 bytes)")

if __name__ == "__main__":
    main()
