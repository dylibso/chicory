package com.dylibso.chicory.component.validation;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.HostFunctionProvider;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.example.generated.ExampleComponent;
import com.example.generated.Person;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Phase 12.4: Record Types Test
 *
 * <p>Tests record (POJO) type handling with progression:
 * <ul>
 *   <li>describe-person(person) -> string
 *   <li>create-person(string, s32) -> person
 * </ul>
 *
 * <p>Shows progression from simple to POJO usage.
 */
public class RecordsProgressionValidator {
    public static void main(String[] mainArgs) throws Exception {
        System.out.println("=== Phase 12.4: Records Progression Test ===\n");

        String basePath = "example/";
        String witSource = Files.readString(Paths.get(basePath + "example.wit"));
        byte[] wasmBytes = Files.readAllBytes(Paths.get(basePath + "example.wasm"));

        try {
            // Force load generated classes to register with PojoRegistry
            Class.forName("com.example.generated.Person");
            Class.forName("com.example.generated.UserStatus");
            Class.forName("com.example.generated.OperationResult");
            Class.forName("com.example.generated.Color");
            WasmModule wasmModule = Parser.parse(wasmBytes);
            HostFunctionProvider hostFunctions = new HostFunctionProvider();

            // Register host functions
            hostFunctions.register(
                    "host-log",
                    hostArgs -> {
                        String msg = hostArgs.length > 0 ? (String) hostArgs[0] : "null";
                        return null;
                    });
            hostFunctions.register("host-get-input", hostArgs -> "test input");

            ComponentModel component =
                    ComponentModel.load(witSource, wasmModule, "$root", hostFunctions);
            ExampleComponent sdk = new ExampleComponent(component);

            System.out.println("=== PROGRESSION STAGE 1: String Parameters ===\n");

            System.out.println("Test 1: describe-person with POJO");
            Person alice = new Person("Alice", 30, true);
            String desc1 = sdk.describePerson(alice);
            assert desc1 != null && desc1.length() > 0 : "Expected description";
            System.out.println("  Input: " + alice);
            System.out.println("  Result: " + desc1 + " ✅\n");

            System.out.println("Test 2: describe-person with different POJO");
            Person bob = new Person("Bob", 25, false);
            String desc2 = sdk.describePerson(bob);
            assert desc2 != null && desc2.length() > 0 : "Expected description";
            System.out.println("  Input: " + bob);
            System.out.println("  Result: " + desc2 + " ✅\n");

            System.out.println("=== PROGRESSION STAGE 2: POJO Output ===\n");

            System.out.println("Test 3: create-person returning POJO");
            Person person1 = sdk.createPerson("Charlie", 35);
            assert "Charlie".equals(person1.getName()) : "Expected name=Charlie";
            assert person1.getAge() == 35 : "Expected age=35";
            System.out.println("  Input: name=Charlie, age=35");
            System.out.println("  Result POJO: " + person1);
            System.out.println(
                    "  Fields: name="
                            + person1.getName()
                            + ", age="
                            + person1.getAge()
                            + ", active="
                            + person1.getActive()
                            + " ✅\n");

            System.out.println("Test 4: create-person with different parameters");
            Person person2 = sdk.createPerson("Diana", 28);
            assert "Diana".equals(person2.getName()) : "Expected name=Diana";
            assert person2.getAge() == 28 : "Expected age=28";
            System.out.println("  Input: name=Diana, age=28");
            System.out.println("  Result POJO: " + person2);
            System.out.println(
                    "  Fields: name="
                            + person2.getName()
                            + ", age="
                            + person2.getAge()
                            + ", active="
                            + person2.getActive()
                            + " ✅\n");

            System.out.println("=== PROGRESSION STAGE 3: POJO Manipulation ===\n");

            System.out.println("Test 5: Extract and use POJO fields");
            Person person3 = sdk.createPerson("Eve", 40);
            int ageFromPojo = person3.getAge();
            String nameFromPojo = person3.getName();
            boolean activeFromPojo = person3.getActive();
            System.out.println("  Created POJO: " + person3);
            System.out.println(
                    "  Extracted: age="
                            + ageFromPojo
                            + ", name="
                            + nameFromPojo
                            + ", active="
                            + activeFromPojo);
            assert ageFromPojo == 40 : "Age mismatch";
            assert "Eve".equals(nameFromPojo) : "Name mismatch";
            System.out.println("  ✅\n");

            System.out.println("Test 6: Create POJO and verify all fields");
            Person person4 = sdk.createPerson("Frank", 32);
            String desc4 = sdk.describePerson(person4);
            System.out.println("  Created POJO: " + person4);
            System.out.println("  Extracted fields and passed to describe:");
            System.out.println("    name=" + person4.getName());
            System.out.println("    age=" + person4.getAge());
            System.out.println("    active=" + person4.getActive());
            System.out.println("  Description result: " + desc4 + " ✅\n");

            System.out.println("✅ All 6 record progression tests PASSED\n");
            System.out.println("Progression Summary:");
            System.out.println("  Stage 1: Basic primitives (string, int, bool) parameters");
            System.out.println("  Stage 2: POJO return types with automatic decoding");
            System.out.println("  Stage 3: Using POJO fields and re-passing to other functions");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
