import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.gen.Module;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import org.junit.jupiter.api.Test;

class BasicTest {

    class TestModule extends Module {
        private final Instance instance;

        public TestModule() {
            instance =
                    Instance.builder(
                                    Parser.parse(
                                            BasicTest.class.getResourceAsStream(
                                                    "/compiled/extism-runtime.wasm")))
                            .build();
        }

        @Override
        public Instance instance() {
            return this.instance;
        }
    }

    @Test
    public void basicModule() {
        // Arrange
        var extismModule = new TestModule();

        // Act
        var ptr = extismModule.alloc(1);

        // Assert
        assertTrue(ptr > 0);
        extismModule.free(ptr);
    }
}
