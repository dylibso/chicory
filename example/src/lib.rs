wit_bindgen::generate!({
    path: "example.wit",
    world: "example",
    exports: {
        world: Example,
    }
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
}
