import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class Template {

    private Template() {
    }

    private static Class moduleClass() {
        return Template.class;
    }

    public static Machine create(Instance instance) {
        return null;
    }

    private static class Loader {
        private static WasmModule MODULE;

        static {
            Class clazz = moduleClass();
            try (InputStream in = clazz.getResourceAsStream(clazz.getSimpleName() + ".meta")) {
                MODULE = Parser.parse(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load AOT WASM module", e);
            }
        }
    }

    public static WasmModule load() {
        return Loader.MODULE;
    }

    public static Instance.Builder builder() {
        return Instance.builder(load()).withMachineFactory((instance) -> create(instance));
    }

}
