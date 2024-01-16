package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.wasm.types.Value;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HostImportsTest {

    @Test
    public void overridesNestedImports() {
        // Arrange
        var wasiImports =
                new HostImports(
                        new HostFunction[] {
                            new HostFunction(
                                    (Memory memory, Value... args) -> new Value[] {Value.i32(1)},
                                    "mod",
                                    "field",
                                    List.of(),
                                    List.of())
                        });
        wasiImports.setName("wasi");
        var myFunction =
                new HostFunction(
                        (Memory memory, Value... args) -> new Value[] {Value.i32(2)},
                        "mod",
                        "field",
                        List.of(),
                        List.of());
        myFunction.setOverride(true);
        var myHostImports = new HostImports(new HostFunction[] {myFunction});
        myHostImports.setDelegates(new HostImports[] {wasiImports});

        // Act
        var res = myHostImports.resolve();

        // Assert
        assertEquals(1, res.getFunctions().length);
        assertEquals(2, res.getFunctions()[0].getHandle().apply(null)[0].asInt());
    }

    @Test
    public void dontOverridesNestedImports() {
        // Arrange
        var wasiImports =
                new HostImports(
                        new HostFunction[] {
                            new HostFunction(
                                    (Memory memory, Value... args) -> new Value[] {Value.i32(1)},
                                    "mod",
                                    "field",
                                    List.of(),
                                    List.of())
                        });
        wasiImports.setName("wasi");
        var myFunction =
                new HostFunction(
                        (Memory memory, Value... args) -> new Value[] {Value.i32(2)},
                        "mod",
                        "field",
                        List.of(),
                        List.of());
        myFunction.setOverride(false);
        var myHostImports = new HostImports(new HostFunction[] {myFunction});
        myHostImports.setDelegates(new HostImports[] {wasiImports});

        // Act
        var res = myHostImports.resolve();

        // Assert
        assertEquals(1, res.getFunctions().length);
        assertEquals(1, res.getFunctions()[0].getHandle().apply(null)[0].asInt());
    }
}
