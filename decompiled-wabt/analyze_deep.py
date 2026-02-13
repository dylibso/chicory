#!/usr/bin/env python3
"""Deeper analysis: check epilogues, depth-4 blocks for func_801/804,
and the code between depth-3 blocks to understand if they're purely sequential."""

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

        print(f"\n{'='*60}")
        print(f"{name} epilogue (last 15 lines):")
        print(f"{'='*60}")
        for i in range(max(start_idx, end_idx - 14), end_idx + 1):
            print(f"  {i+1}: {lines[i].rstrip()}")

    # For func_801, look at depth-4 inside label_12
    print(f"\n{'='*60}")
    print("func_801 depth-4 inside label_12 (8651 lines):")
    print(f"{'='*60}")
    # label_12 starts at line 84453
    label_12_start = 84453 - 1
    label_12_end = find_block_end(lines, label_12_start)

    inner_depth = 0
    d4_blocks = []
    for i in range(label_12_start, label_12_end + 1):
        line = lines[i]
        if inner_depth == 1:
            m = re.match(r'\s+(label_\d+):\s*[\{w]', line)
            if m:
                be = find_block_end(lines, i)
                kind = "loop" if "while" in line else "block"
                d4_blocks.append((i, be, m.group(1), kind))
        inner_depth += line.count('{') - line.count('}')

    # Check for outer label references
    cumulative = 0
    for bs, be, lbl, kind in d4_blocks:
        sz = be - bs + 1
        cumulative += sz
        # Find labels defined and break targets
        defined = set()
        targets = set()
        for i in range(bs, be + 1):
            for m in re.finditer(r'(label_\d+)\s*:', lines[i]):
                defined.add(m.group(1))
            for m in re.finditer(r'(?:break|continue)\s+(label_\d+)', lines[i]):
                targets.add(m.group(1))
        outer = targets - defined
        returns = sum(1 for i in range(bs, be+1) if re.search(r'\breturn\b', lines[i]))
        status = "CLEAN" if not outer else f"OUTER: {outer}"
        ret_info = f", {returns} ret" if returns else ""
        print(f"  {lbl} ({kind}): {sz} lines, cumul {cumulative} - {status}{ret_info}")

    # For func_804, look at depth-4 inside label_3
    print(f"\n{'='*60}")
    print("func_804 depth-4 inside label_3 (7042 lines):")
    print(f"{'='*60}")
    # label_3 starts at line 93459
    label_3_start = 93459 - 1
    label_3_end = find_block_end(lines, label_3_start)

    inner_depth = 0
    d4_blocks = []
    for i in range(label_3_start, label_3_end + 1):
        line = lines[i]
        if inner_depth == 1:
            m = re.match(r'\s+(label_\d+):\s*[\{w]', line)
            if m:
                be = find_block_end(lines, i)
                kind = "loop" if "while" in line else "block"
                d4_blocks.append((i, be, m.group(1), kind))
        inner_depth += line.count('{') - line.count('}')

    cumulative = 0
    for bs, be, lbl, kind in d4_blocks:
        sz = be - bs + 1
        cumulative += sz
        defined = set()
        targets = set()
        for i in range(bs, be + 1):
            for m in re.finditer(r'(label_\d+)\s*:', lines[i]):
                defined.add(m.group(1))
            for m in re.finditer(r'(?:break|continue)\s+(label_\d+)', lines[i]):
                targets.add(m.group(1))
        outer = targets - defined
        returns = sum(1 for i in range(bs, be+1) if re.search(r'\breturn\b', lines[i]))
        status = "CLEAN" if not outer else f"OUTER: {outer}"
        ret_info = f", {returns} ret" if returns else ""
        print(f"  {lbl} ({kind}): {sz} lines, cumul {cumulative} - {status}{ret_info}")

if __name__ == "__main__":
    main()
