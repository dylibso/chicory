import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StringDebugTest {
    public static void main(String[] args) throws Exception {
        // Force load generated classes
        Class.forName("com.example.generated.Person");
        Class.forName("com.example.generated.UserStatus");
        Class.forName("com.example.generated.OperationResult");
        Class.forName("com.example.generated.Color");
        
        String witSource = Files.readString(Paths.get("example/example.wit"));
        byte[] wasmBytes = Files.readAllBytes(Paths.get("example/example.wasm"));
        WasmModule wasmModule = Parser.parse(wasmBytes);
        
        HostFunctionProvider hostFunctions = new HostFunctionProvider();
        hostFunctions.register("host-log", hostArgs -> null);
        hostFunctions.register("host-get-input", hostArgs -> "test input");
        
        ComponentModel component = ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
        
        System.out.println("Testing string export: add(5, 3)");
        try {
            Object result = component.callExport("add", 5, 3);
            System.out.println("✅ add(5, 3) = " + result);
        } catch (Exception e) {
            System.out.println("❌ add failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\nTesting string export: is-positive(42)");
        try {
            Object result = component.callExport("is-positive", 42);
            System.out.println("✅ is-positive(42) = " + result);
        } catch (Exception e) {
            System.out.println("❌ is-positive failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\nTesting string export: greet(\"World\")");
        try {
            Object result = component.callExport("greet", "World");
            System.out.println("✅ greet(\"World\") = " + result);
        } catch (Exception e) {
            System.out.println("❌ greet failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
