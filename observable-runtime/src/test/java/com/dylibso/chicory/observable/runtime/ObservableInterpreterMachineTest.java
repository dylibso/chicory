package com.dylibso.chicory.observable.runtime;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

public class ObservableInterpreterMachineTest {

    @Test
    public void exportBasicInformation() throws InterruptedException {
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

        Thread.sleep(10000);
        // Assert
        // need something like this:
        // https://github.com/quarkusio/quarkus/blob/3ec1df9198ee37fd1488271e86ae3f5f2768ff23/integration-tests/opentelemetry/src/main/java/io/quarkus/it/opentelemetry/ExporterResource.java#L30
        //
        //        <dependency>
        //            <groupId>io.opentelemetry</groupId>
        //            <artifactId>opentelemetry-sdk-testing</artifactId>
        //        </dependency>
    }
}
