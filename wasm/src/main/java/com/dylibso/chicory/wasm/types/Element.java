package com.dylibso.chicory.wasm.types;

public interface Element {

    ElemType getElemType();

    int getSize();

    enum ElemType {
        Type(0),
        Func(1),
        Table(2),
        Mem(3),
        Global(4),
        Elem(5),
        Data(6),
        Start(7);

        private final int id;

        ElemType(int id) {
            this.id = id;
        }
    }
}
