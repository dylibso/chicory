#!/usr/bin/env python3
"""Analyze call argument arities and patterns in the 3 too-large methods
to determine the best helper methods to extract."""

import re

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_block_end(lines, start_idx):
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
        end_idx = find_block_end(lines, start_idx)
        func_lines = lines[start_idx:end_idx+1]
        body = "".join(func_lines)

        # Find callArgs patterns: long[] callArgs_N = new long[K];
        arities = {}
        for line in func_lines:
            m = re.search(r'long\[\]\s+callArgs_\d+\s*=\s*new\s+long\[(\d+)\]', line)
            if m:
                k = int(m.group(1))
                arities[k] = arities.get(k, 0) + 1

        print(f"\n{name}:")
        print(f"  Call arities: {dict(sorted(arities.items()))}")
        print(f"  Total calls: {sum(arities.values())}")

        # Count calls that capture return value vs void calls
        # Pattern: result = instance.getMachine().call(...) => has return
        # Pattern: instance.getMachine().call(...) => void
        ret_calls = len(re.findall(r'=\s*instance\.getMachine\(\)\.call\(', body))
        void_calls = body.count('instance.getMachine().call(') - ret_calls
        print(f"  Void calls: {void_calls}, Return calls: {ret_calls}")

        # Check how many lines each call pattern takes
        # Pattern: new long[K] + K assignments + call = K+2 lines minimum
        total_call_lines = 0
        for k, count in arities.items():
            total_call_lines += count * (k + 2)
        print(f"  Estimated call pattern lines: {total_call_lines} / {end_idx - start_idx + 1}")

if __name__ == "__main__":
    main()
