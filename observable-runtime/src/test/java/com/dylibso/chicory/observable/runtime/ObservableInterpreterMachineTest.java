package com.dylibso.chicory.observable.runtime;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class ObservableInterpreterMachineTest {

    @Test
    public void exportBasicInformation() {
        // Arrange
        var instance =
                Instance.builder(
                                Parser.parse(
                                        ObservableInterpreterMachineTest.class.getResourceAsStream(
                                                "/compiled/basic.c.wasm")))
                        .withMachineFactory(ObservableInterpreterMachine::new)
                        .build();

        // Act
        instance.exports().function("run").apply();

        // Assert
        // need something like this:
        // https://github.com/open-telemetry/opentelemetry-java-examples/blob/main/telemetry-testing/src/test/java/io/opentelemetry/example/telemetry/ApplicationTest.java
        // ???
    }
}
