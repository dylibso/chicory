import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dylibso.chicory.gen.Module;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

class MultiReturnsTest {

    class TestModule extends Module {
        private final Instance instance;

        public TestModule() {
            instance =
                    Instance.builder(
                                    Parser.parse(
                                            MultiReturnsTest.class.getResourceAsStream(
                                                    "/compiled/multi-returns.wat.wasm")))
                            .build();
        }

        @Override
        public Instance instance() {
            return this.instance;
        }
    }

    @Test
    public void multiReturnsModule() {
        // Arrange
        var multiReturnsModule = new TestModule();

        // Act
        var result = multiReturnsModule.example(1L);

        // Assert
        assertEquals(2, result.length);
        assertEquals(1L, result[0].asLong());
        assertEquals(1L, result[1].asLong());
    }
}
