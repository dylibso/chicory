package com.dylibso.chicory.wasm.types;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * The "name" custom section.
 */
public final class NameCustomSection extends CustomSection {

    private final Optional<String> moduleName;
    private final List<NameEntry> funcNames;
    private final List<ListEntry<NameEntry>> localNames;
    private final List<ListEntry<NameEntry>> labelNames;
    private final List<NameEntry> tableNames;
    private final List<NameEntry> memoryNames;
    private final List<NameEntry> globalNames;
    private final List<NameEntry> elementNames;
    private final List<NameEntry> dataNames;
    private final List<NameEntry> tagNames;

    private NameCustomSection(
            Optional<String> moduleName,
            List<NameEntry> funcNames,
            List<ListEntry<NameEntry>> localNames,
            List<ListEntry<NameEntry>> labelNames,
            List<NameEntry> tableNames,
            List<NameEntry> memoryNames,
            List<NameEntry> globalNames,
            List<NameEntry> elementNames,
            List<NameEntry> dataNames,
            List<NameEntry> tagNames) {
        this.moduleName = requireNonNull(moduleName);
        this.funcNames = List.copyOf(requireNonNull(funcNames));
        this.localNames = List.copyOf(requireNonNull(localNames));
        this.labelNames = List.copyOf(requireNonNull(labelNames));
        this.tableNames = List.copyOf(requireNonNull(tableNames));
        this.memoryNames = List.copyOf(requireNonNull(memoryNames));
        this.globalNames = List.copyOf(requireNonNull(globalNames));
        this.elementNames = List.copyOf(requireNonNull(elementNames));
        this.dataNames = List.copyOf(requireNonNull(dataNames));
        this.tagNames = List.copyOf(requireNonNull(tagNames));
    }

    /**
     * Parse a new instance from a byte array.
     *
     * @param bytes the byte content of the section
     */
    public static NameCustomSection parse(byte[] bytes) {
        String moduleName = null;
        List<NameEntry> funcNames = new ArrayList<>();
        List<ListEntry<NameEntry>> localNames = new ArrayList<>();
        List<ListEntry<NameEntry>> labelNames = new ArrayList<>();
        List<NameEntry> tableNames = new ArrayList<>();
        List<NameEntry> memoryNames = new ArrayList<>();
        List<NameEntry> globalNames = new ArrayList<>();
        List<NameEntry> elementNames = new ArrayList<>();
        List<NameEntry> dataNames = new ArrayList<>();
        List<NameEntry> tagNames = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        while (buf.hasRemaining()) {
            byte id = buf.get();
            // discard subsection size
            ByteBuffer slice = slice(buf, (int) Parser.readVarUInt32(buf));
            // todo: IDs 4 and 10 are reserved for the Host GC spec
            switch (id) {
                case 0:
                    assert (moduleName == null);
                    moduleName = Parser.readName(slice);
                    break;
                case 1:
                    oneLevelParse(slice, funcNames);
                    break;
                case 2:
                    twoLevelParse(slice, localNames);
                    break;
                case 3:
                    twoLevelParse(slice, labelNames);
                    break;
                case 5:
                    oneLevelParse(slice, tableNames);
                    break;
                case 6:
                    oneLevelParse(slice, memoryNames);
                    break;
                case 7:
                    oneLevelParse(slice, globalNames);
                    break;
                case 8:
                    oneLevelParse(slice, elementNames);
                    break;
                case 9:
                    oneLevelParse(slice, dataNames);
                    break;
                case 11:
                    oneLevelParse(slice, tagNames);
                    break;
                default:
                    // ignore unknown subsection for forwards-compatibility
            }
        }

        return new NameCustomSection(
                Optional.ofNullable(moduleName),
                funcNames,
                localNames,
                labelNames,
                tableNames,
                memoryNames,
                globalNames,
                elementNames,
                dataNames,
                tagNames);
    }

    @Override
    public String name() {
        return "name";
    }

    /**
     * @return the optional module name
     */
    public Optional<String> moduleName() {
        return moduleName;
    }

    /**
     * @return the name of the function with the given index, or {@code null} if none is set
     */
    public String nameOfFunction(int functionIdx) {
        return oneLevelSearch(funcNames, functionIdx);
    }

    /**
     * @return the number of function names in this section
     *         This value does not have any relationship to the function index of any particular entry;
     *         it merely reflects the number of function names in this section.
     *         Used for testing.
     */
    public int functionNameCount() {
        return funcNames.size();
    }

    /**
     * @return the name of the local with the given index within the function with the given index, or {@code null} if none is set
     */
    public String nameOfLocal(int functionIdx, int localIdx) {
        return twoLevelSearch(localNames, functionIdx, localIdx);
    }

    /**
     * @return the name of the local with the given index within the function with the given index, or {@code null} if none is set
     */
    public String nameOfLabel(int functionIdx, int labelIdx) {
        return twoLevelSearch(labelNames, functionIdx, labelIdx);
    }

    /**
     * @return the name of the table with the given index, or {@code null} if none is set
     */
    public String nameOfTable(int tableIdx) {
        return oneLevelSearch(tableNames, tableIdx);
    }

    /**
     * @return the name of the memory with the given index, or {@code null} if none is set
     */
    public String nameOfMemory(int memoryIdx) {
        return oneLevelSearch(memoryNames, memoryIdx);
    }

    /**
     * @return the name of the global with the given index, or {@code null} if none is set
     */
    public String nameOfGlobal(int globalIdx) {
        return oneLevelSearch(globalNames, globalIdx);
    }

    /**
     * @return the name of the element with the given index, or {@code null} if none is set
     */
    public String nameOfElement(int elementIdx) {
        return oneLevelSearch(elementNames, elementIdx);
    }

    /**
     * @return the name of the data segment with the given index, or {@code null} if none is set
     */
    public String nameOfData(int dataIdx) {
        return oneLevelSearch(dataNames, dataIdx);
    }

    /**
     * @return the name of the tag with the given index, or {@code null} if none is set
     */
    public String nameOfTag(int tagIdx) {
        return oneLevelSearch(tagNames, tagIdx);
    }

    // parsing helpers

    private static void oneLevelParse(ByteBuffer slice, List<NameEntry> list) {
        int cnt = (int) Parser.readVarUInt32(slice);
        for (int i = 0; i < cnt; i++) {
            oneLevelStore(list, (int) Parser.readVarUInt32(slice), Parser.readName(slice));
        }
    }

    private static void twoLevelParse(ByteBuffer slice, List<ListEntry<NameEntry>> list) {
        int listCnt = (int) Parser.readVarUInt32(slice);
        for (int i = 0; i < listCnt; i++) {
            int groupIdx = (int) Parser.readVarUInt32(slice);
            int cnt = (int) Parser.readVarUInt32(slice);
            for (int j = 0; j < cnt; j++) {
                twoLevelStore(
                        list, groupIdx, (int) Parser.readVarUInt32(slice), Parser.readName(slice));
            }
        }
    }

    private static ByteBuffer slice(ByteBuffer buf, int size) {
        int pos = buf.position();
        int lim = buf.limit();
        try {
            buf.limit(pos + size);
            return buf.slice();
        } finally {
            buf.limit(lim);
            buf.position(pos + size);
        }
    }

    // searching

    private static String oneLevelSearch(List<NameEntry> list, int searchIdx) {
        int idx = binarySearch(list, searchIdx, NameEntry::index);
        return idx < 0 ? null : list.get(idx).name();
    }

    private static String twoLevelSearch(
            List<ListEntry<NameEntry>> listList, int groupIdx, int subIdx) {
        int fi = binarySearch(listList, groupIdx, ListEntry::index);
        if (fi < 0) {
            return null;
        }
        ListEntry<NameEntry> subList = listList.get(fi);
        int li = binarySearch(subList, subIdx, NameEntry::index);
        return li < 0 ? null : subList.get(li).name;
    }

    private static String oneLevelStore(List<NameEntry> list, int storeIdx, String name) {
        requireNonNull(name);
        int idx = binarySearch(list, storeIdx, NameEntry::index);
        if (idx < 0) {
            // insert
            list.add(-idx - 1, new NameEntry(storeIdx, name));
            return null;
        }
        // replace
        return list.set(idx, new NameEntry(storeIdx, name)).name();
    }

    private static String twoLevelStore(
            List<ListEntry<NameEntry>> listList, int groupIdx, int subIdx, String name) {
        requireNonNull(name);
        int fi = binarySearch(listList, groupIdx, ListEntry::index);
        ListEntry<NameEntry> subList;
        if (fi < 0) {
            // insert
            subList = new ListEntry<>(groupIdx);
            listList.add(-fi - 1, subList);
        } else {
            subList = listList.get(fi);
        }
        int li = binarySearch(subList, subIdx, NameEntry::index);
        if (li < 0) {
            // insert
            subList.add(-li - 1, new NameEntry(subIdx, name));
            return null;
        }
        // replace
        return subList.set(li, new NameEntry(subIdx, name)).name();
    }

    private static <T> int binarySearch(List<T> list, int idx, ToIntFunction<T> indexExtractor) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = Integer.compare(indexExtractor.applyAsInt(list.get(mid)), idx);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                // found
                return mid;
            }
        }
        // not found
        return -low - 1;
    }

    // name map support

    static final class NameEntry {
        private final int index;
        private final String name;

        NameEntry(int index, String name) {
            this.index = index;
            this.name = name;
        }

        int index() {
            return index;
        }

        String name() {
            return name;
        }

        @Override
        public String toString() {
            return "[" + index + "] -> " + name;
        }
    }

    // this is never serialized.
    @SuppressWarnings("serial")
    static final class ListEntry<T> extends ArrayList<T> {
        private final int index;

        ListEntry(int index) {
            this.index = index;
        }

        int index() {
            return index;
        }

        @Override
        public String toString() {
            return "[" + index + "] -> " + super.toString();
        }
    }
}
