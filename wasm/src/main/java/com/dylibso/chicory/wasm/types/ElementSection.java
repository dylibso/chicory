package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ElementSection extends Section {
    private final ArrayList<Element> elements;

    /**
     * Construct a new, empty section instance.
     */
    public ElementSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of elements to reserve space for
     */
    public ElementSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private ElementSection(ArrayList<Element> elements) {
        super(SectionId.ELEMENT);
        this.elements = elements;
    }

    public Element[] elements() {
        return elements.toArray(Element[]::new);
    }

    public int elementCount() {
        return elements.size();
    }

    public Element getElement(int idx) {
        return elements.get(idx);
    }

    /**
     * Add an element definition to this section.
     *
     * @param element the element to add to this section (must not be {@code null})
     * @return the index of the newly-added element
     */
    public int addElement(Element element) {
        Objects.requireNonNull(element, "element");
        int idx = elements.size();
        elements.add(element);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var elementCount = in.u31();
        elements.ensureCapacity(elements.size() + elementCount);

        for (var i = 0; i < elementCount; i++) {
            addElement(parseSingleElement(in));
        }
        assert in.peekRawByteOpt() == -1;
    }

    private static Element parseSingleElement(WasmInputStream in) {
        // Elements are actually fairly complex to parse.
        // See https://webassembly.github.io/spec/core/binary/modules.html#element-section

        int flags = in.u32();

        // Active elements have bit 0 clear
        boolean active = (flags & 0b001) == 0;
        // Declarative elements are non-active elements with bit 1 set
        boolean declarative = !active && (flags & 0b010) != 0;
        // Otherwise, it's passive
        boolean passive = !active && !declarative;

        // Now, characteristics for parsing
        // Does the (active) segment have a table index, or is it always 0?
        boolean hasTableIdx = active && (flags & 0b010) != 0;
        // Is the type always funcref, or do we have to read the type?
        boolean alwaysFuncRef = active && !hasTableIdx;
        // Are initializers expressions or function indices?
        boolean exprInit = (flags & 0b100) != 0;
        // Is the type encoded as an elemkind?
        boolean hasElemKind = !exprInit && !alwaysFuncRef;
        // Is the type encoded as a reftype?
        boolean hasRefType = exprInit && !alwaysFuncRef;

        // the table index is assumed to be zero
        int tableIdx = 0;
        List<Instruction> offset = List.of();

        if (active) {
            if (hasTableIdx) {
                tableIdx = in.u31();
            }
            offset = List.of(Parser.parseExpression(in));
        }
        // common path
        ValueType type;
        if (alwaysFuncRef) {
            type = ValueType.FuncRef;
        } else if (hasElemKind) {
            int ek = in.rawByte();
            switch (ek) {
                case 0x00:
                    {
                        type = ValueType.FuncRef;
                        break;
                    }
                default:
                    {
                        throw new ChicoryException("Invalid element kind");
                    }
            }
        } else {
            assert hasRefType;
            type = ValueType.refTypeForId(in.u8());
        }
        int initCnt = in.u31();
        List<List<Instruction>> inits = new ArrayList<>(initCnt);
        if (exprInit) {
            // read the expressions directly from the stream
            for (int i = 0; i < initCnt; i++) {
                inits.add(List.of(Parser.parseExpression(in)));
            }
        } else {
            // read function references, and compose them as instruction lists
            for (int i = 0; i < initCnt; i++) {
                inits.add(
                        List.of(
                                new Instruction(-1, OpCode.REF_FUNC, new long[] {in.u31()}),
                                new Instruction(-1, OpCode.END, new long[0])));
            }
        }
        if (declarative) {
            return new DeclarativeElement(type, inits);
        } else if (passive) {
            return new PassiveElement(type, inits);
        } else {
            assert active;
            return new ActiveElement(type, inits, tableIdx, offset);
        }
    }
}
