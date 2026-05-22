wit_bindgen::generate!({
    path: "example.wit",
    world: "example",
    exports: {
        world: Example,
    },
});

pub struct Example;

impl Guest for Example {
    fn add(a: i32, b: i32) -> i32 {
        a + b
    }

    fn multiply(a: i64, b: i64) -> i64 {
        a * b
    }

    fn is_positive(x: i32) -> bool {
        x > 0
    }

    fn greet(name: String) -> String {
        format!("Hello, {}!", name)
    }

    fn process_text(text: String) -> String {
        text.to_uppercase()
    }

    fn test_host_call_log(msg: String) {
        host_log(&msg);
    }

    fn test_host_call_get_input() -> String {
        host_get_input()
    }
}
