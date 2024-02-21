package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for the functionality of {@link MStack}.
 */
public class MStackTest {
    public MStackTest() {}

    @Test
    public void testNew() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        stack = new MStack(100);
        assertEquals(0, stack.depth());
    }

    @Test
    public void testI32() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        stack.pushI32(1234);
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        assertEquals(1234, stack.popI32());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushI32(1234);
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234, stack.peekI32());
        assertEquals(ValueType.I32, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI64);
        assertThrowsExactly(ChicoryException.class, stack::peekF32);
        assertThrowsExactly(ChicoryException.class, stack::peekF64);
        assertThrowsExactly(ChicoryException.class, stack::peekV128High);
        assertThrowsExactly(ChicoryException.class, stack::peekV128Low);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::peekExternRef);
        assertThrowsExactly(ChicoryException.class, stack::popI64);
        assertThrowsExactly(ChicoryException.class, stack::popF32);
        assertThrowsExactly(ChicoryException.class, stack::popF64);
        assertThrowsExactly(ChicoryException.class, stack::popV128Low);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popExternRef);
        assertEquals(2, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.I32, ValueType.I32)));
        assertEquals(1234, stack.popI32());
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        assertEquals(1234, stack.popI32());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }

    @Test
    public void testI64() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushI64(1234);
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234, stack.popI64());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushI64(1234);
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertEquals(1234, stack.peekI64());
        assertEquals(ValueType.I64, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI32);
        assertThrowsExactly(ChicoryException.class, stack::peekF32);
        assertThrowsExactly(ChicoryException.class, stack::peekF64);
        assertThrowsExactly(ChicoryException.class, stack::peekV128High);
        assertThrowsExactly(ChicoryException.class, stack::peekV128Low);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::peekExternRef);
        assertThrowsExactly(ChicoryException.class, stack::popI32);
        assertThrowsExactly(ChicoryException.class, stack::popF32);
        assertThrowsExactly(ChicoryException.class, stack::popF64);
        assertThrowsExactly(ChicoryException.class, stack::popV128Low);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popExternRef);
        assertEquals(2, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.I64, ValueType.I64)));
        assertEquals(1234, stack.popI64());
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234, stack.popI64());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }

    @Test
    public void testF32() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        stack.pushF32(1234f);
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        assertEquals(1234f, stack.popF32());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushF32(1234f);
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234f, stack.peekF32());
        assertEquals(ValueType.F32, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI64);
        assertThrowsExactly(ChicoryException.class, stack::peekI32);
        assertThrowsExactly(ChicoryException.class, stack::peekF64);
        assertThrowsExactly(ChicoryException.class, stack::peekV128High);
        assertThrowsExactly(ChicoryException.class, stack::peekV128Low);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::peekExternRef);
        assertThrowsExactly(ChicoryException.class, stack::popI64);
        assertThrowsExactly(ChicoryException.class, stack::popI32);
        assertThrowsExactly(ChicoryException.class, stack::popF64);
        assertThrowsExactly(ChicoryException.class, stack::popV128Low);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popExternRef);
        assertEquals(2, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.F32, ValueType.F32)));
        assertEquals(1234f, stack.popF32());
        assertEquals(1, stack.depth());
        assertEquals(1, stack.wordDepth());
        assertEquals(1234f, stack.popF32());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }

    @Test
    public void testF64() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushF64(1234d);
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234d, stack.popF64());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushF64(1234d);
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertEquals(1234d, stack.peekF64());
        assertEquals(ValueType.F64, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI32);
        assertThrowsExactly(ChicoryException.class, stack::peekF32);
        assertThrowsExactly(ChicoryException.class, stack::peekI64);
        assertThrowsExactly(ChicoryException.class, stack::peekV128High);
        assertThrowsExactly(ChicoryException.class, stack::peekV128Low);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::peekExternRef);
        assertThrowsExactly(ChicoryException.class, stack::popI32);
        assertThrowsExactly(ChicoryException.class, stack::popF32);
        assertThrowsExactly(ChicoryException.class, stack::popI64);
        assertThrowsExactly(ChicoryException.class, stack::popV128Low);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popExternRef);
        assertEquals(2, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.F64, ValueType.F64)));
        assertEquals(1234d, stack.popF64());
        assertEquals(1, stack.depth());
        assertEquals(2, stack.wordDepth());
        assertEquals(1234d, stack.popF64());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }

    @Test
    public void testV128() {
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushV128(1234, 5678);
        assertEquals(1, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertEquals(5678, stack.peekV128High());
        assertEquals(1234, stack.peekV128Low());
        assertEquals(1234, stack.popV128Low());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushV128(1234, 5678);
        assertEquals(1, stack.depth());
        assertEquals(4, stack.wordDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(8, stack.wordDepth());
        assertEquals(5678, stack.peekV128High());
        assertEquals(1234, stack.peekV128Low());
        assertEquals(ValueType.V128, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI32);
        assertThrowsExactly(ChicoryException.class, stack::peekF32);
        assertThrowsExactly(ChicoryException.class, stack::peekI64);
        assertThrowsExactly(ChicoryException.class, stack::peekF64);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::peekExternRef);
        assertThrowsExactly(ChicoryException.class, stack::popI32);
        assertThrowsExactly(ChicoryException.class, stack::popF32);
        assertThrowsExactly(ChicoryException.class, stack::popI64);
        assertThrowsExactly(ChicoryException.class, stack::popF64);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popExternRef);
        assertEquals(2, stack.depth());
        assertEquals(8, stack.wordDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.V128, ValueType.V128)));
        assertEquals(5678, stack.peekV128High());
        assertEquals(1234, stack.peekV128Low());
        assertEquals(1234, stack.popV128Low());
        assertEquals(1, stack.depth());
        assertEquals(4, stack.wordDepth());
        assertEquals(5678, stack.peekV128High());
        assertEquals(1234, stack.peekV128Low());
        assertEquals(1234, stack.popV128Low());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }

    @Test
    public void testExternRef() {
        ExternRef r1 = new ExternRef("Foo");
        ExternRef r2 = new ExternRef("Bar");
        MStack stack = new MStack();
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        stack.pushExternRef(r1);
        assertEquals(1, stack.depth());
        assertEquals(0, stack.wordDepth());
        assertEquals(1, stack.refDepth());
        assertEquals(r1, stack.popExternRef());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
        assertEquals(0, stack.refDepth());
        stack.pushExternRef(r2);
        assertEquals(1, stack.depth());
        assertEquals(0, stack.wordDepth());
        assertEquals(1, stack.refDepth());
        stack.dup();
        assertEquals(2, stack.depth());
        assertEquals(0, stack.wordDepth());
        assertEquals(2, stack.refDepth());
        assertEquals(r2, stack.peekExternRef());
        assertEquals(ValueType.ExternRef, stack.peekType());
        assertThrowsExactly(ChicoryException.class, stack::peekI32);
        assertThrowsExactly(ChicoryException.class, stack::peekF32);
        assertThrowsExactly(ChicoryException.class, stack::peekI64);
        assertThrowsExactly(ChicoryException.class, stack::peekF64);
        assertThrowsExactly(ChicoryException.class, stack::peekV128High);
        assertThrowsExactly(ChicoryException.class, stack::peekV128Low);
        assertThrowsExactly(ChicoryException.class, stack::peekFuncRef);
        assertThrowsExactly(ChicoryException.class, stack::popI32);
        assertThrowsExactly(ChicoryException.class, stack::popF32);
        assertThrowsExactly(ChicoryException.class, stack::popI64);
        assertThrowsExactly(ChicoryException.class, stack::popF64);
        assertThrowsExactly(ChicoryException.class, stack::popV128Low);
        assertThrowsExactly(ChicoryException.class, stack::popFuncRef);
        assertEquals(2, stack.depth());
        assertEquals(2, stack.refDepth());
        assertTrue(stack.checkTypes(List.of(ValueType.ExternRef, ValueType.ExternRef)));
        assertEquals(r2, stack.popExternRef());
        assertEquals(1, stack.depth());
        assertEquals(1, stack.refDepth());
        assertEquals(r2, stack.popExternRef());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.refDepth());
    }

    @Test
    @Disabled("Skipping function tests: function ref lookups are incomplete")
    public void testFuncRef() {}

    @Test
    public void testDrop() {
        MStack stack = new MStack();
        stack.pushI32(1234);
        stack.pushF32(4567f);
        stack.pushV128(100, 200);
        stack.pushI64(4321);
        assertEquals(4, stack.depth());
        assertTrue(
                stack.checkTypes(
                        List.of(ValueType.I32, ValueType.F32, ValueType.V128, ValueType.I64)));
        assertThrowsExactly(RuntimeException.class, () -> stack.drop(5));
        assertThrowsExactly(IllegalArgumentException.class, () -> stack.drop(-1));
        stack.drop(2);
        assertEquals(2, stack.depth());
        assertTrue(stack.checkTypes(List.of(ValueType.I32, ValueType.F32)));
        assertThrowsExactly(RuntimeException.class, () -> stack.drop(3));
        stack.drop(2);
        assertEquals(0, stack.depth());
        assertThrowsExactly(RuntimeException.class, () -> stack.drop(1));
        // now, try it with skip
        ExternRef foo = new ExternRef("Foo");
        stack.pushI32(1234);
        stack.pushF32(4567f);
        stack.pushV128(100, 200);
        stack.pushI64(4321);
        stack.pushExternRef(foo);
        stack.pushF32(5151f);
        stack.pushV128(400, 500);
        stack.pushI64(9090);
        assertEquals(8, stack.depth());
        assertTrue(
                stack.checkTypes(
                        List.of(
                                ValueType.I32,
                                ValueType.F32,
                                ValueType.V128,
                                ValueType.I64,
                                ValueType.ExternRef,
                                ValueType.F32,
                                ValueType.V128,
                                ValueType.I64)));
        stack.drop(2, 4);
        assertEquals(6, stack.depth());
        assertTrue(
                stack.checkTypes(
                        List.of(
                                ValueType.I32,
                                ValueType.F32,
                                ValueType.ExternRef,
                                ValueType.F32,
                                ValueType.V128,
                                ValueType.I64)));
        assertEquals(1, stack.refDepth());
        assertEquals(9, stack.wordDepth());
        assertEquals(9090, stack.popI64());
        assertEquals(7, stack.wordDepth());
        assertEquals(500, stack.peekV128High());
        assertEquals(400, stack.popV128Low());
        assertEquals(3, stack.wordDepth());
        assertEquals(5151f, stack.popF32());
        assertEquals(2, stack.wordDepth());
        assertEquals(foo, stack.popExternRef());
        assertEquals(0, stack.refDepth());
        assertEquals(4567f, stack.popF32());
        assertEquals(1, stack.wordDepth());
        assertEquals(1234, stack.popI32());
        assertEquals(0, stack.depth());
        assertEquals(0, stack.wordDepth());
    }
}
