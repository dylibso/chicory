#!/usr/bin/env python3
"""
Split the 3 too-large methods in Wat2WasmMachine.java so they compile.

Approach:
  - For each function, locals become int[]/long[] arrays shared across helpers.
  - The function body is split into helper methods at natural block boundaries.
  - Outer 'break label_N' become 'return N' in helpers; caller checks and breaks.
  - Structure stays as close to original as possible.

Only does what's needed, nothing more.
"""

import re
import sys

SOURCE = "src/main/java/com/dylibso/chicory/wabt/com/dylibso/chicory/wabt/Wat2WasmMachine.java"

def find_block_end(lines, start_idx):
    depth = 0
    for i in range(start_idx, len(lines)):
        depth += lines[i].count('{') - lines[i].count('}')
        if depth == 0:
            return i
    return len(lines) - 1

def find_children_at_depth(lines, start_idx, end_idx, target_depth):
    """Find labeled blocks at a given nesting depth relative to start_idx."""
    depth = 0
    children = []
    for i in range(start_idx, end_idx + 1):
        line = lines[i]
        if depth == target_depth:
            m = re.match(r'(\s+)(label_\d+):\s*(\{|while)', line)
            if m:
                be = find_block_end(lines, i)
                children.append((i, be, m.group(2)))
        depth += line.count('{') - line.count('}')
    return children

def drill_to_fanout(lines, start_idx, end_idx, max_depth=15):
    """Drill through single-child nesting until finding multiple children or a leaf."""
    for d in range(1, max_depth):
        children = find_children_at_depth(lines, start_idx, end_idx, d)
        if len(children) != 1:
            return d, children
        # Single child, continue drilling
    return max_depth, []

def parse_locals(lines, func_start, func_end):
    """Parse local variable declarations from a function. Returns (int_locals, long_locals)."""
    int_locals = []  # (name, orig_line_idx)
    long_locals = []
    args = []

    # Parse args from function signature
    sig_line = lines[func_start]
    for m in re.finditer(r'(int|long|float|double)\s+(arg\d+)', sig_line):
        typ, name = m.group(1), m.group(2)
        if typ == 'long':
            long_locals.append(name)
        else:
            int_locals.append(name)
        args.append(name)

    # Parse local declarations
    for i in range(func_start + 1, min(func_start + 60, func_end)):
        m = re.match(r'\s+(int|long)\s+(local\d+)\s*=\s*', lines[i])
        if m:
            if m.group(1) == 'int':
                int_locals.append(m.group(2))
            else:
                long_locals.append(m.group(2))

    return int_locals, long_locals, args

def replace_locals_with_arrays(text, int_locals, long_locals):
    """Replace local variable references with array access."""
    for idx, name in enumerate(int_locals):
        text = re.sub(r'\b' + name + r'\b', f'iL[{idx}]', text)
    for idx, name in enumerate(long_locals):
        text = re.sub(r'\b' + name + r'\b', f'lL[{idx}]', text)
    return text

def find_outer_labels(lines, block_start, block_end):
    """Find labels that are referenced by break/continue but not defined within the block."""
    defined = set()
    targets = set()
    for i in range(block_start, block_end + 1):
        for m in re.finditer(r'(label_\d+)\s*:', lines[i]):
            defined.add(m.group(1))
        for m in re.finditer(r'(?:break|continue)\s+(label_\d+)', lines[i]):
            targets.add(m.group(1))
    return targets - defined

def replace_outer_breaks(text, outer_labels, label_to_code):
    """Replace 'break label_X;' for outer labels with 'return CODE;'"""
    for label in outer_labels:
        code = label_to_code.get(label, 99)
        text = re.sub(r'break\s+' + label + r'\s*;', f'return {code};', text)
        text = re.sub(r'continue\s+' + label + r'\s*;', f'return {code};', text)
    return text

def replace_returns(text, return_code=-1):
    """Replace 'return expr;' with storing result and returning special code."""
    # Replace 'return iL[X];' -> 'return RETURN_CODE;' (result already in array)
    text = re.sub(r'return\s+iL\[(\d+)\];', f'return {return_code};', text)
    # Replace 'return expr;' where expr is more complex
    # These shouldn't appear in the helpers since locals are in arrays
    return text

def split_function(lines, func_start, func_name, func_id):
    """Split a single too-large function. Returns (new_func_text, helper_methods_text)."""
    func_end = find_block_end(lines, func_start)
    func_lines = lines[func_start:func_end + 1]
    func_size = func_end - func_start + 1

    print(f"\nProcessing {func_name} ({func_size} lines, {func_start+1}-{func_end+1})")

    # Parse locals
    int_locals, long_locals, args = parse_locals(lines, func_start, func_end)
    print(f"  int locals ({len(int_locals)}): {int_locals}")
    print(f"  long locals ({len(long_locals)}): {long_locals}")

    # Find the depth where the code fans out
    fanout_depth, children = drill_to_fanout(lines, func_start, func_end)
    print(f"  Fanout at depth {fanout_depth}: {len(children)} children")

    if len(children) < 2:
        # Need to go deeper or handle single massive block
        # For func_804: drill deeper into the single child
        print(f"  WARNING: No fanout found, trying alternative approach")
        # Find the largest block and check its children at depth+1
        for extra in range(1, 10):
            children = find_children_at_depth(lines, func_start, func_end, fanout_depth + extra)
            if len(children) >= 2:
                fanout_depth += extra
                print(f"  Found fanout at depth {fanout_depth}: {len(children)} children")
                break

    if len(children) < 2:
        print(f"  ERROR: Could not find fanout for {func_name}")
        return None, None

    # Print children
    for cs, ce, cl in children:
        sz = ce - cs + 1
        outer = find_outer_labels(lines, cs, ce)
        print(f"    {cl}: lines {cs+1}-{ce+1} ({sz} lines) {'OUTER: ' + str(outer) if outer else 'CLEAN'}")

    # Determine split points: roughly equal halves
    total_child_lines = sum(ce - cs + 1 for cs, ce, cl in children)
    cumul = 0
    split_idx = len(children) // 2
    for i, (cs, ce, cl) in enumerate(children):
        cumul += ce - cs + 1
        if cumul > total_child_lines // 2:
            split_idx = i + 1
            break

    # If split produces parts that are still too large, split into thirds
    group1_size = sum(children[i][1] - children[i][0] + 1 for i in range(split_idx))
    group2_size = sum(children[i][1] - children[i][0] + 1 for i in range(split_idx, len(children)))

    if group1_size > 4000 or group2_size > 4000:
        # Split into thirds
        split_idx1 = len(children) // 3
        split_idx2 = 2 * len(children) // 3
        if split_idx1 == 0: split_idx1 = 1
        if split_idx2 <= split_idx1: split_idx2 = split_idx1 + 1
        groups = [
            children[:split_idx1],
            children[split_idx1:split_idx2],
            children[split_idx2:]
        ]
    else:
        groups = [
            children[:split_idx],
            children[split_idx:]
        ]

    for gi, g in enumerate(groups):
        gsz = sum(ce - cs + 1 for cs, ce, cl in g)
        print(f"  Group {gi}: {len(g)} blocks, {gsz} lines")

    # Collect all outer labels across all children
    all_outer_labels = set()
    for cs, ce, cl in children:
        all_outer_labels.update(find_outer_labels(lines, cs, ce))

    # Map each outer label to a return code
    label_to_code = {}
    for i, label in enumerate(sorted(all_outer_labels), start=1):
        label_to_code[label] = i
    print(f"  Outer label mapping: {label_to_code}")

    # Find the code region to split: from first child start to last child end
    region_start = children[0][0]
    region_end = children[-1][1]

    # Find code before first child (inside the parent block, between parent opening and first child)
    # and code after last child (between last child and parent closing)
    # We need the parent block boundaries
    # The parent is at fanout_depth-1 relative to the function

    # For the main function:
    # - Keep everything before region_start
    # - Replace region with calls to helpers
    # - Keep everything after region_end

    # Build the main function
    sig = lines[func_start].rstrip()

    # Build array declarations
    arr_decls = f"        int[] iL = new int[{len(int_locals)}];\n"
    arr_decls += f"        long[] lL = new long[{len(long_locals)}];\n"

    # Init args into arrays
    arg_inits = ""
    for arg in args:
        if arg in int_locals:
            idx = int_locals.index(arg)
            arg_inits += f"        iL[{idx}] = {arg};\n"
        elif arg in long_locals:
            idx = long_locals.index(arg)
            arg_inits += f"        lL[{idx}] = {arg};\n"

    # Code before region (locals → arrays, skip local declarations)
    pre_region = []
    for i in range(func_start + 1, region_start):
        line = lines[i]
        # Skip local declarations (they're replaced by array init)
        if re.match(r'\s+(int|long)\s+local\d+\s*=\s*', line):
            continue
        pre_region.append(line)
    pre_text = "".join(pre_region)
    pre_text = replace_locals_with_arrays(pre_text, int_locals, long_locals)

    # Code after region
    post_region = []
    for i in range(region_end + 1, func_end + 1):
        post_region.append(lines[i])
    post_text = "".join(post_region)
    post_text = replace_locals_with_arrays(post_text, int_locals, long_locals)

    # Build helper calls
    helper_calls = ""
    memory_param = "memory" if "memory" in sig else "memory"
    instance_param = "instance" if "instance" in sig else "instance"

    for gi in range(len(groups)):
        helper_calls += f"        {{\n"
        helper_calls += f"            int _status = {func_name}_{gi}(iL, lL, {memory_param}, {instance_param});\n"
        for label, code in sorted(label_to_code.items(), key=lambda x: x[1]):
            helper_calls += f"            if (_status == {code}) break {label};\n"
        helper_calls += f"            if (_status == -1) return iL[{int_locals.index(int_locals[-1]) if int_locals else 0}];\n"
        helper_calls += f"        }}\n"

    # Find the return variable from the epilogue
    # Look for 'return localN;' pattern in post_text
    ret_match = re.search(r'return iL\[(\d+)\];', post_text)
    if ret_match:
        ret_idx = ret_match.group(1)
        print(f"  Return value: iL[{ret_idx}]")

    # Build the new main function
    # We need the original args in the signature (not replaced)
    new_main = sig + "\n"
    new_main += arr_decls
    new_main += arg_inits
    new_main += pre_text
    new_main += helper_calls
    # Code between region and the closing braces (glue code + epilogue)
    # Find code between last child end and the enclosing block close
    # This is the "glue" after the last group - goes into the main function

    # Actually, let me include the between-child glue code in the helpers
    # The region from children[0] start to children[-1] end includes all children
    # and the glue between them

    new_main += post_text

    # Build helper methods
    helpers = ""
    for gi, group in enumerate(groups):
        g_start = group[0][0]
        g_end = group[-1][1]

        # Include glue code between this group's last child and the next group's first child
        # (or the region end if this is the last group)
        if gi < len(groups) - 1:
            next_start = groups[gi + 1][0][0]
            g_end_with_glue = next_start - 1
        else:
            g_end_with_glue = region_end

        # Extract the code for this group
        group_text = "".join(lines[g_start:g_end_with_glue + 1])

        # Replace locals with array access
        group_text = replace_locals_with_arrays(group_text, int_locals, long_locals)

        # Replace outer breaks with return codes
        outer_in_group = set()
        for cs, ce, cl in group:
            outer_in_group.update(find_outer_labels(lines, cs, ce))
        group_text = replace_outer_breaks(group_text, outer_in_group, label_to_code)

        # Replace return statements
        group_text = replace_returns(group_text)

        helper_sig = (
            f"    private static int {func_name}_{gi}("
            f"int[] iL, long[] lL, "
            f"com.dylibso.chicory.runtime.Memory memory, "
            f"com.dylibso.chicory.runtime.Instance instance)"
        )

        helpers += f"\n{helper_sig} {{\n"
        helpers += group_text
        helpers += f"        return 0;\n"
        helpers += f"    }}\n"

    return new_main, helpers

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    # The 3 too-large functions (0-indexed line numbers)
    funcs = [
        (2651 - 1, "func_41", 41),
        (84257 - 1, "func_801", 801),
        (93437 - 1, "func_804", 804),
    ]

    # Process in reverse order (so line numbers don't shift)
    replacements = []
    all_helpers = []

    for func_start, func_name, func_id in reversed(funcs):
        func_end = find_block_end(lines, func_start)
        result = split_function(lines, func_start, func_name, func_id)
        if result[0] is None:
            print(f"FAILED to split {func_name}")
            continue
        new_main, helpers = result
        replacements.append((func_start, func_end, new_main))
        all_helpers.append(helpers)

    # Apply replacements (reverse order to preserve line numbers)
    for func_start, func_end, new_main in replacements:
        lines[func_start:func_end + 1] = [new_main]

    # Insert helpers before the closing brace of the class
    # Find the last line (class closing brace)
    class_end = len(lines) - 1
    for i in range(len(lines) - 1, -1, -1):
        if lines[i].strip() == '}':
            class_end = i
            break

    helper_text = "\n".join(all_helpers)
    lines.insert(class_end, helper_text + "\n")

    # Write output
    output = SOURCE + ".split"
    with open(output, 'w') as f:
        f.writelines(lines)
    print(f"\nWrote split source to {output}")
    print(f"Total lines: {len(lines)}")

if __name__ == "__main__":
    main()
