#!/usr/bin/env python3
"""Analyze the structure of too-large methods in Wat2WasmMachine.java.

Finds top-level and depth-2 labeled blocks to identify natural split points.
"""

import re
import sys

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_func_end(lines, start_idx):
    """Find the end line index of a method starting at start_idx."""
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def analyze_blocks(lines, start_idx, end_idx, target_depth):
    """Find labeled blocks at a given depth inside a method."""
    depth = 0
    blocks = []
    for i in range(start_idx, end_idx + 1):
        line = lines[i]
        if depth == target_depth:
            m = re.match(r'\s+(label_\d+):\s*\{', line)
            if m:
                blocks.append((i + 1, m.group(1), "block"))
            m2 = re.match(r'\s+(label_\d+):\s*while', line)
            if m2:
                blocks.append((i + 1, m2.group(1), "loop"))
        depth += line.count('{') - line.count('}')
    return blocks

def find_block_end(lines, start_idx):
    """Find end of a block starting with { at start_idx."""
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    # The 3 too-large functions (1-indexed line numbers)
    funcs = [(2651, "func_41"), (84257, "func_801"), (93437, "func_804")]

    for start_line, name in funcs:
        start_idx = start_line - 1
        end_idx = find_func_end(lines, start_idx)
        size = end_idx - start_idx + 1

        print(f"\n{'='*60}")
        print(f"{name}: lines {start_line}-{end_idx+1} ({size} lines)")
        print(f"{'='*60}")

        # Top-level blocks (depth 1 = inside method body)
        top_blocks = analyze_blocks(lines, start_idx, end_idx, 1)
        print(f"\nTop-level blocks ({len(top_blocks)}):")
        for line_no, label, kind in top_blocks:
            block_end = find_block_end(lines, line_no - 1)
            block_size = block_end - (line_no - 1) + 1
            print(f"  {label} ({kind}) at line {line_no}, size {block_size} lines")

        # Depth-2 blocks (inside the first top-level block)
        if top_blocks:
            first_block_start = top_blocks[0][0] - 1
            first_block_end = find_block_end(lines, first_block_start)
            d2_blocks = analyze_blocks(lines, first_block_start, first_block_end, 2)
            print(f"\nDepth-2 blocks inside {top_blocks[0][1]} ({len(d2_blocks)}):")
            cumulative = 0
            for line_no, label, kind in d2_blocks:
                block_end = find_block_end(lines, line_no - 1)
                block_size = block_end - (line_no - 1) + 1
                cumulative += block_size
                print(f"  {label} ({kind}) at line {line_no}, size {block_size} lines, cumulative {cumulative}")

        # Count locals
        local_count = 0
        for i in range(start_idx, min(start_idx + 50, end_idx)):
            if re.match(r'\s+(int|long|float|double) local\d+', lines[i]):
                local_count += 1
        print(f"\nLocals: {local_count}")

        # Signature
        print(f"Signature: {lines[start_idx].strip()[:120]}")

if __name__ == "__main__":
    main()
