package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * The "name" custom section.
 */
public final class NameCustomSection extends CustomSection {

    private String moduleName;
    private final ArrayList<NameEntry> funcNames = new ArrayList<>();
    private final ArrayList<ListEntry<NameEntry>> localNames = new ArrayList<>();
    private final ArrayList<ListEntry<NameEntry>> labelNames = new ArrayList<>();
    private final ArrayList<NameEntry> tableNames = new ArrayList<>();
    private final ArrayList<NameEntry> memoryNames = new ArrayList<>();
    private final ArrayList<NameEntry> globalNames = new ArrayList<>();
    private final ArrayList<NameEntry> elementNames = new ArrayList<>();
    private final ArrayList<NameEntry> dataNames = new ArrayList<>();
    private final ArrayList<NameEntry> tagNames = new ArrayList<>();

    /**
     * Construct a new, empty section instance.
     */
    public NameCustomSection() {}

    /**
     * Construct a new instance.
     *
     * @param in the byte content of the section
     */
    public NameCustomSection(final WasmInputStream in) {
        this();

        while (in.peekRawByteOpt() != -1) {
            int id = in.rawByte();
            // discard subsection size
            try (WasmInputStream slice = in.slice(in.u32Long())) {
                // todo: IDs 4 and 10 are reserved for the Host GC spec
                switch (id) {
                    case 0:
                        {
                            setModuleName(slice.utf8());
                            break;
                        }
                    case 1:
                        {
                            oneLevelParse(slice, funcNames);
                            break;
                        }
                    case 2:
                        {
                            twoLevelParse(slice, localNames);
                            break;
                        }
                    case 3:
                        {
                            twoLevelParse(slice, labelNames);
                            break;
                        }
                    case 5:
                        {
                            oneLevelParse(slice, tableNames);
                            break;
                        }
                    case 6:
                        {
                            oneLevelParse(slice, memoryNames);
                            break;
                        }
                    case 7:
                        {
                            oneLevelParse(slice, globalNames);
                            break;
                        }
                    case 8:
                        {
                            oneLevelParse(slice, elementNames);
                            break;
                        }
                    case 9:
                        {
                            oneLevelParse(slice, dataNames);
                            break;
                        }
                    case 11:
                        {
                            oneLevelParse(slice, tagNames);
                            break;
                        }
                    default:
                        // ignore unknown subsection for forwards-compatibility
                }
            }
        }
    }

    public String name() {
        return "name";
    }

    /**
     * {@return the module name, or <code>null</code> if none is set}
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * {@return the name of the function with the given index, or <code>null</code> if none is set}
     */
    public String nameOfFunction(int functionIdx) {
        return oneLevelSearch(funcNames, functionIdx);
    }

    /**
     * {@return the number of function names in this section}
     * This value does not have any relationship to the function index of any particular entry;
     * it merely reflects the number of function names in this section.
     * Used for testing.
     */
    public int functionNameCount() {
        return funcNames.size();
    }

    /**
     * {@return the name of the local with the given index within the function with the given index, or <code>null</code> if none is set}
     */
    public String nameOfLocal(int functionIdx, int localIdx) {
        return twoLevelSearch(localNames, functionIdx, localIdx);
    }

    /**
     * {@return the name of the local with the given index within the function with the given index, or <code>null</code> if none is set}
     */
    public String nameOfLabel(int functionIdx, int labelIdx) {
        return twoLevelSearch(labelNames, functionIdx, labelIdx);
    }

    /**
     * {@return the name of the table with the given index, or <code>null</code> if none is set}
     */
    public String nameOfTable(int tableIdx) {
        return oneLevelSearch(tableNames, tableIdx);
    }

    /**
     * {@return the name of the memory with the given index, or <code>null</code> if none is set}
     */
    public String nameOfMemory(int memoryIdx) {
        return oneLevelSearch(memoryNames, memoryIdx);
    }

    /**
     * {@return the name of the global with the given index, or <code>null</code> if none is set}
     */
    public String nameOfGlobal(int globalIdx) {
        return oneLevelSearch(globalNames, globalIdx);
    }

    /**
     * {@return the name of the element with the given index, or <code>null</code> if none is set}
     */
    public String nameOfElement(int elementIdx) {
        return oneLevelSearch(elementNames, elementIdx);
    }

    /**
     * {@return the name of the data segment with the given index, or <code>null</code> if none is set}
     */
    public String nameOfData(int dataIdx) {
        return oneLevelSearch(dataNames, dataIdx);
    }

    /**
     * {@return the name of the tag with the given index, or <code>null</code> if none is set}
     */
    public String nameOfTag(int tagIdx) {
        return oneLevelSearch(tagNames, tagIdx);
    }

    /**
     * Set the module name.
     *
     * @param moduleName the module name to set (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String setModuleName(final String moduleName) {
        try {
            return this.moduleName;
        } finally {
            this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        }
    }

    /**
     * Add a function name to this section.
     *
     * @param functionIdx the index of the function to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addFunctionName(int functionIdx, String name) {
        return oneLevelStore(funcNames, functionIdx, name);
    }

    /**
     * Add a local name to this section.
     *
     * @param functionIdx the index of the function containing the local
     * @param localIdx the index of the local to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addLocalName(int functionIdx, int localIdx, String name) {
        return twoLevelStore(localNames, functionIdx, localIdx, name);
    }

    /**
     * Add a label name to this section.
     *
     * @param functionIdx the index of the function containing the label
     * @param labelIdx the index of the label to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addLabelName(int functionIdx, int labelIdx, String name) {
        return twoLevelStore(labelNames, functionIdx, labelIdx, name);
    }

    /**
     * Add a table name to this section.
     *
     * @param tableIdx the index of the table to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addTableName(int tableIdx, String name) {
        return oneLevelStore(funcNames, tableIdx, name);
    }

    /**
     * Add a memory name to this section.
     *
     * @param memoryIdx the index of the memory to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addMemoryName(int memoryIdx, String name) {
        return oneLevelStore(funcNames, memoryIdx, name);
    }

    /**
     * Add a global name to this section.
     *
     * @param globalIdx the index of the global to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addGlobalName(int globalIdx, String name) {
        return oneLevelStore(funcNames, globalIdx, name);
    }

    /**
     * Add an element name to this section.
     *
     * @param elementIdx the index of the element to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addElementName(int elementIdx, String name) {
        return oneLevelStore(funcNames, elementIdx, name);
    }

    /**
     * Add a data segment name to this section.
     *
     * @param dataIdx the index of the data segment to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addDataName(int dataIdx, String name) {
        return oneLevelStore(funcNames, dataIdx, name);
    }

    /**
     * Add a tag name to this section.
     *
     * @param tagIdx the index of the tag to name
     * @param name the new name (must not be {@code null})
     * @return the previously set name, or {@code null} if there was none
     */
    public String addTagName(int tagIdx, String name) {
        return oneLevelStore(funcNames, tagIdx, name);
    }

    // parsing helpers

    private void oneLevelParse(final WasmInputStream in, final ArrayList<NameEntry> list) {
        int cnt = in.u31();
        for (int i = 0; i < cnt; i++) {
            oneLevelStore(list, in.u31(), in.utf8());
        }
    }

    private void twoLevelParse(
            final WasmInputStream in, final ArrayList<ListEntry<NameEntry>> list) {
        int listCnt = in.u31();
        for (int i = 0; i < listCnt; i++) {
            int groupIdx = in.u31();
            int cnt = in.u31();
            for (int j = 0; j < cnt; j++) {
                twoLevelStore(list, groupIdx, in.u31(), in.utf8());
            }
        }
    }

    // searching

    private static String oneLevelSearch(ArrayList<NameEntry> list, int searchIdx) {
        int idx = binarySearch(list, searchIdx, NameEntry::index);
        return idx < 0 ? null : list.get(idx).name();
    }

    private static String twoLevelSearch(
            ArrayList<ListEntry<NameEntry>> listList, int groupIdx, int subIdx) {
        int fi = binarySearch(listList, groupIdx, ListEntry::index);
        if (fi < 0) {
            return null;
        }
        ListEntry<NameEntry> subList = listList.get(fi);
        int li = binarySearch(subList, subIdx, NameEntry::index);
        return li < 0 ? null : subList.get(li).name;
    }

    private static String oneLevelStore(ArrayList<NameEntry> list, int storeIdx, String name) {
        Objects.requireNonNull(name);
        int idx = binarySearch(list, storeIdx, NameEntry::index);
        if (idx < 0) {
            // insert
            list.add(-idx - 1, new NameEntry(storeIdx, name));
            return null;
        } else {
            // replace
            return list.set(idx, new NameEntry(storeIdx, name)).name();
        }
    }

    private static String twoLevelStore(
            ArrayList<ListEntry<NameEntry>> listList, int groupIdx, int subIdx, String name) {
        Objects.requireNonNull(name);
        int fi = binarySearch(listList, groupIdx, ListEntry::index);
        ListEntry<NameEntry> subList;
        if (fi < 0) {
            // insert
            listList.add(-fi - 1, subList = new ListEntry<>(groupIdx));
        } else {
            subList = listList.get(fi);
        }
        int li = binarySearch(subList, subIdx, NameEntry::index);
        if (li < 0) {
            // insert
            subList.add(-li - 1, new NameEntry(subIdx, name));
            return null;
        } else {
            // replace
            return subList.set(li, new NameEntry(subIdx, name)).name();
        }
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

        NameEntry(final int index, final String name) {
            this.index = index;
            this.name = name;
        }

        int index() {
            return index;
        }

        String name() {
            return name;
        }

        public String toString() {
            return "[" + index + "] -> " + name;
        }
    }

    // this is never serialized.
    @SuppressWarnings("serial")
    static final class ListEntry<T> extends ArrayList<T> {
        private final int index;

        ListEntry(final int index) {
            this.index = index;
        }

        int index() {
            return index;
        }

        public String toString() {
            return "[" + index + "] -> " + super.toString();
        }
    }
}
