#!/usr/bin/env python3
"""Split 3 too-large methods in Wat2WasmMachine.java.

Converts locals to int[]/long[] arrays, extracts large blocks into helpers,
handles outer break/continue with return codes (0=normal, 1+=outer label).
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

def find_outer_labels(lines, start, end):
    defined = set()
    referenced = {}
    for i in range(start, end + 1):
        for m in re.finditer(r'(label_\d+)\s*:', lines[i]):
            defined.add(m.group(1))
        for m in re.finditer(r'(break|continue)\s+(label_\d+)', lines[i]):
            label, kind = m.group(2), m.group(1)
            if label not in referenced:
                referenced[label] = kind
    return {l: k for l, k in referenced.items() if l not in defined}

def replace_locals(text, int_map, long_map):
    for name in sorted(int_map.keys(), key=len, reverse=True):
        text = re.sub(r'\b' + name + r'\b', f'iL[{int_map[name]}]', text)
    for name in sorted(long_map.keys(), key=len, reverse=True):
        text = re.sub(r'\b' + name + r'\b', f'lL[{long_map[name]}]', text)
    return text

def build_init(int_map, long_map, args):
    code = f"        int[] iL = new int[{len(int_map)}];\n"
    code += f"        long[] lL = new long[{len(long_map)}];\n"
    for a in args:
        if a in int_map:
            code += f"        iL[{int_map[a]}] = {a};\n"
        elif a in long_map:
            code += f"        lL[{long_map[a]}] = {a};\n"
    return code

def extract_block_content(lines, label_line, helper_name, int_map, long_map):
    block_end = find_block_end(lines, label_line)
    cs = label_line + 1
    ce = block_end - 1
    if cs > ce:
        return None, None, cs, ce

    outer = find_outer_labels(lines, cs, ce)
    label_codes = {}
    code = 1
    for label in sorted(outer.keys()):
        label_codes[label] = code
        code += 1

    content = "".join(lines[cs:ce + 1])
    content = replace_locals(content, int_map, long_map)
    for label, ocode in label_codes.items():
        content = re.sub(r'break\s+' + label + r'\s*;', f'return {ocode};', content)
        content = re.sub(r'continue\s+' + label + r'\s*;', f'return {ocode};', content)

    helper = (f"\n    private static int {helper_name}(int[] iL, long[] lL, "
              f"com.dylibso.chicory.runtime.Memory memory, "
              f"com.dylibso.chicory.runtime.Instance instance) {{\n")
    helper += content
    helper += f"        return 0;\n    }}\n"

    indent = re.match(r'(\s*)', lines[label_line]).group(1) + "    "
    dispatch = f"{indent}int _s = {helper_name}(iL, lL, memory, instance);\n"
    for label, ocode in sorted(label_codes.items(), key=lambda x: x[1]):
        kind = outer[label]
        dispatch += f"{indent}if (_s == {ocode}) {kind} {label};\n"

    print(f"  {helper_name}: extracted {ce-cs+1} lines, {len(outer)} outer labels: {outer}")
    return helper, dispatch, cs, ce

def extract_siblings(lines, range_start, range_end, helper_name, int_map, long_map):
    outer = find_outer_labels(lines, range_start, range_end)
    label_codes = {}
    code = 1
    for label in sorted(outer.keys()):
        label_codes[label] = code
        code += 1

    content = "".join(lines[range_start:range_end + 1])
    content = replace_locals(content, int_map, long_map)
    for label, ocode in label_codes.items():
        content = re.sub(r'break\s+' + label + r'\s*;', f'return {ocode};', content)
        content = re.sub(r'continue\s+' + label + r'\s*;', f'return {ocode};', content)

    helper = (f"\n    private static int {helper_name}(int[] iL, long[] lL, "
              f"com.dylibso.chicory.runtime.Memory memory, "
              f"com.dylibso.chicory.runtime.Instance instance) {{\n")
    helper += content
    helper += f"        return 0;\n    }}\n"

    indent = re.match(r'(\s*)', lines[range_start]).group(1)
    dispatch = f"{indent}int _s = {helper_name}(iL, lL, memory, instance);\n"
    for label, ocode in sorted(label_codes.items(), key=lambda x: x[1]):
        kind = outer[label]
        dispatch += f"{indent}if (_s == {ocode}) {kind} {label};\n"

    print(f"  {helper_name}: extracted {range_end-range_start+1} lines, {len(outer)} outer labels: {outer}")
    return helper, dispatch

def main():
    with open(SOURCE) as f:
        lines = f.readlines()

    functions = [
        {
            'name': 'func_41', 'start': 2651,
            'int_map': {'arg0':0,'local1':1,'local2':2,'local3':3,'local4':4,'local5':5,
                        'local6':6,'local7':7,'local8':8,'local9':9,'local10':10,
                        'local11':11,'local12':12,'local13':13,'local14':14,'local15':15,
                        'local16':16,'local17':17},
            'long_map': {'local18':0,'local19':1},
            'args': ['arg0'],
            'extractions': [
                {'type':'siblings','start':2693,'last_start':5125,'helper':'func_41_helper0'},
            ],
        },
        {
            'name': 'func_801', 'start': 84257,
            'int_map': {'arg0':0,'arg1':1,'arg2':2,'arg3':3,'local4':4,'local5':5,
                        'local6':6,'local7':7,'local8':8,'local9':9,'local10':10,
                        'local11':11,'local12':12,'local13':13,'local15':14,'local16':15,
                        'local17':16,'local18':17,'local19':18,'local20':19,
                        'local21':20,'local22':21,'local23':22},
            'long_map': {'local14':0,'local24':1,'local25':2},
            'args': ['arg0','arg1','arg2','arg3'],
            'extractions': [
                {'type':'block','line':84655,'helper':'func_801_helper0'},
                {'type':'block','line':90911,'helper':'func_801_helper1'},
            ],
        },
        {
            'name': 'func_804', 'start': 93437,
            'int_map': {'arg0':0,'arg1':1,'arg2':2,'local3':3,'local4':4,'local5':5,
                        'local6':6,'local7':7,'local8':8,'local9':9,'local10':10,
                        'local11':11,'local12':12,'local14':13,'local15':14,'local16':15},
            'long_map': {'local13':0},
            'args': ['arg0','arg1','arg2'],
            'extractions': [
                {'type':'block','line':93678,'helper':'func_804_helper0'},
            ],
        },
    ]

    all_helpers = []

    for func in reversed(functions):
        fs = func['start'] - 1
        fe = find_block_end(lines, fs)
        print(f"\n{'='*60}\n{func['name']} (lines {fs+1}-{fe+1}, {fe-fs+1} lines)\n{'='*60}")

        func_helpers = []
        for ext in reversed(func['extractions']):
            if ext['type'] == 'block':
                ll = ext['line'] - 1
                h, d, cs, ce = extract_block_content(lines, ll, ext['helper'], func['int_map'], func['long_map'])
                if h:
                    func_helpers.append(h)
                    lines[cs:ce+1] = [d]
                    fe = find_block_end(lines, fs)
                    print(f"    func now {fe-fs+1} lines")
            elif ext['type'] == 'siblings':
                rs = ext['start'] - 1
                ls = ext['last_start'] - 1
                re_end = find_block_end(lines, ls)
                h, d = extract_siblings(lines, rs, re_end, ext['helper'], func['int_map'], func['long_map'])
                func_helpers.append(h)
                lines[rs:re_end+1] = [d]
                fe = find_block_end(lines, fs)
                print(f"    func now {fe-fs+1} lines")

        # Phase 2: replace locals in remaining body
        sig = lines[fs]
        body = []
        for line in lines[fs+1:fe+1]:
            if re.match(r'\s+(int|long)\s+local\d+\s*=\s*', line):
                continue
            body.append(line)
        body_text = replace_locals("".join(body), func['int_map'], func['long_map'])
        init = build_init(func['int_map'], func['long_map'], func['args'])
        new_func = sig + init + body_text
        lines[fs:fe+1] = [new_func]
        all_helpers.extend(reversed(func_helpers))
        print(f"  Final: {new_func.count(chr(10))+1} lines")

    # Insert helpers before class closing brace
    for i in range(len(lines)-1, -1, -1):
        if lines[i].strip() == '}':
            lines.insert(i, "\n".join(all_helpers) + "\n")
            break

    with open(SOURCE, 'w') as f:
        f.writelines(lines)
    print(f"\nDone. Total lines: {sum(l.count(chr(10)) for l in lines)}")

if __name__ == '__main__':
    main()
