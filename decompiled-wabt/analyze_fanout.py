#!/usr/bin/env python3
"""Find the depth at which the massive blocks finally fan out
into multiple children for func_801 and func_804."""

import re

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_block_end(lines, start_idx):
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def find_children(lines, start_idx, end_idx):
    """Find direct child labeled blocks."""
    depth = 0
    children = []
    for i in range(start_idx, end_idx + 1):
        line = lines[i]
        if depth == 1:
            m = re.match(r'\s+(label_\d+):\s*[\{w]', line)
            if m:
                be = find_block_end(lines, i)
                kind = "loop" if "while" in line else "block"
                children.append((i, be, m.group(1), kind))
        depth += line.count('{') - line.count('}')
    return children

def drill_down(lines, start_idx, end_idx, label, depth_level, max_depth=10):
    """Recursively drill into single-child blocks until we find a fanout."""
    children = find_children(lines, start_idx, end_idx)
    size = end_idx - start_idx + 1

    if len(children) != 1 or depth_level >= max_depth:
        # Found fanout or reached max depth
        print(f"\n  Fanout at depth {depth_level} in {label} ({size} lines): {len(children)} children")
        cumul = 0
        for bs, be, lbl, kind in children:
            sz = be - bs + 1
            cumul += sz
            # Check outer refs
            defined = set()
            targets = set()
            for i in range(bs, be + 1):
                for m2 in re.finditer(r'(label_\d+)\s*:', lines[i]):
                    defined.add(m2.group(1))
                for m2 in re.finditer(r'(?:break|continue)\s+(label_\d+)', lines[i]):
                    targets.add(m2.group(1))
            outer = targets - defined
            status = "CLEAN" if not outer else f"OUTER: {outer}"
            print(f"    {lbl} ({kind}): {sz} lines, cumul {cumul} - {status}")
        return

    # Single child - drill deeper
    child = children[0]
    print(f"  Depth {depth_level}: {label} -> {child[2]} ({child[3]}, {child[1]-child[0]+1} lines)")
    drill_down(lines, child[0], child[1], child[2], depth_level + 1, max_depth)

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    # func_801: label_12 starts at line 84453
    print("=" * 60)
    print("func_801: drilling into label_12")
    print("=" * 60)
    label_12_start = 84453 - 1
    label_12_end = find_block_end(lines, label_12_start)
    drill_down(lines, label_12_start, label_12_end, "label_12", 0)

    # func_804: label_3 starts at line 93459
    print("\n" + "=" * 60)
    print("func_804: drilling into label_3")
    print("=" * 60)
    label_3_start = 93459 - 1
    label_3_end = find_block_end(lines, label_3_start)
    drill_down(lines, label_3_start, label_3_end, "label_3", 0)

    # func_41: already known to fan out at depth-3 with 19 children
    print("\n" + "=" * 60)
    print("func_41: already fans out at depth-3 inside label_2")
    print("=" * 60)

if __name__ == "__main__":
    main()
