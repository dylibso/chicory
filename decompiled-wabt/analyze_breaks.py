#!/usr/bin/env python3
"""Analyze break targets in depth-2 blocks to check if they reference outer labels.

If a depth-2 block only breaks to labels defined within itself (or to label_1/label_2
which are its direct parents), extraction into a helper is straightforward.
"""

import re

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_block_end(lines, start_idx):
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def find_labels_defined(lines, start_idx, end_idx):
    """Find all labels defined within a range."""
    labels = set()
    for i in range(start_idx, end_idx + 1):
        m = re.search(r'(label_\d+)\s*:', lines[i])
        if m:
            labels.add(m.group(1))
    return labels

def find_break_targets(lines, start_idx, end_idx):
    """Find all break/continue targets within a range."""
    targets = set()
    for i in range(start_idx, end_idx + 1):
        for m in re.finditer(r'(?:break|continue)\s+(label_\d+)', lines[i]):
            targets.add(m.group(1))
    return targets

def find_returns(lines, start_idx, end_idx):
    """Count return statements in a range."""
    count = 0
    for i in range(start_idx, end_idx + 1):
        if re.search(r'\breturn\b', lines[i]):
            count += 1
    return count

def analyze_func(lines, start_line, name):
    start_idx = start_line - 1
    # find end
    depth = 0
    end_idx = start_idx
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            end_idx = i
            break

    print(f"\n{'='*60}")
    print(f"{name}: lines {start_line}-{end_idx+1}")
    print(f"{'='*60}")

    # Find depth-2 blocks inside the first top-level block
    # First, find the first top-level block
    method_depth = 0
    first_top_start = None
    first_top_label = None
    for i in range(start_idx, end_idx + 1):
        line = lines[i]
        if method_depth == 1:
            m = re.match(r'\s+(label_\d+):\s*[\{w]', line)
            if m and first_top_start is None:
                first_top_start = i
                first_top_label = m.group(1)
                break
        method_depth += line.count('{') - line.count('}')

    if first_top_start is None:
        print("  No top-level block found")
        return

    first_top_end = find_block_end(lines, first_top_start)
    print(f"\nFirst top-level block: {first_top_label} at lines {first_top_start+1}-{first_top_end+1}")

    # Find depth-2 blocks
    block_depth = 0
    d2_blocks = []
    for i in range(first_top_start, first_top_end + 1):
        line = lines[i]
        if block_depth == 1:
            m = re.match(r'\s+(label_\d+):\s*\{', line)
            if m:
                block_end = find_block_end(lines, i)
                d2_blocks.append((i, block_end, m.group(1)))
            m2 = re.match(r'\s+(label_\d+):\s*while', line)
            if m2:
                block_end = find_block_end(lines, i)
                d2_blocks.append((i, block_end, m2.group(1) + " (loop)"))
        block_depth += line.count('{') - line.count('}')

    # For each depth-2 block, check if breaks go to outer labels
    # Outer labels = labels NOT defined within the block itself
    print(f"\nDepth-2 blocks ({len(d2_blocks)}):")
    for block_start, block_end, label in d2_blocks:
        size = block_end - block_start + 1
        defined = find_labels_defined(lines, block_start, block_end)
        targets = find_break_targets(lines, block_start, block_end)
        returns = find_returns(lines, block_start, block_end)
        outer = targets - defined
        status = "CLEAN" if not outer else f"OUTER: {outer}"
        ret_info = f", {returns} returns" if returns else ""
        print(f"  {label}: lines {block_start+1}-{block_end+1} ({size} lines) - {status}{ret_info}")

    # Also check for depth-3 blocks inside large depth-2 blocks
    for block_start, block_end, label in d2_blocks:
        size = block_end - block_start + 1
        if size > 3000:
            print(f"\n  Depth-3 inside {label} ({size} lines):")
            inner_depth = 0
            d3_blocks = []
            for i in range(block_start, block_end + 1):
                line = lines[i]
                if inner_depth == 1:
                    m = re.match(r'\s+(label_\d+):\s*\{', line)
                    if m:
                        be = find_block_end(lines, i)
                        d3_blocks.append((i, be, m.group(1)))
                    m2 = re.match(r'\s+(label_\d+):\s*while', line)
                    if m2:
                        be = find_block_end(lines, i)
                        d3_blocks.append((i, be, m2.group(1) + " (loop)"))
                inner_depth += line.count('{') - line.count('}')

            cumulative = 0
            for bs, be, lbl in d3_blocks:
                sz = be - bs + 1
                cumulative += sz
                defined = find_labels_defined(lines, bs, be)
                targets = find_break_targets(lines, bs, be)
                outer = targets - defined
                returns = find_returns(lines, bs, be)
                status = "CLEAN" if not outer else f"OUTER: {outer}"
                ret_info = f", {returns} returns" if returns else ""
                print(f"    {lbl}: {sz} lines, cumul {cumulative} - {status}{ret_info}")

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    funcs = [(2651, "func_41"), (84257, "func_801"), (93437, "func_804")]
    for start_line, name in funcs:
        analyze_func(lines, start_line, name)

if __name__ == "__main__":
    main()
