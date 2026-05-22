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

    fn describe_person(p: Person) -> String {
        format!("{} is {} years old (active: {})", p.name, p.age, p.active)
    }

    fn create_person(name: String, age: i32) -> Person {
        Person {
            name,
            age,
            active: age >= 18,
        }
    }

    fn get_result() -> OperationResult {
        OperationResult::Ok("Success!".to_string())
    }

    fn pick_color(index: i32) -> Color {
        match index % 3 {
            0 => Color::Red,
            1 => Color::Green,
            _ => Color::Blue,
        }
    }

    fn repeat_string(text: String, count: i32) -> Vec<String> {
        (0..count).map(|_| text.clone()).collect()
    }

    fn sum_numbers(numbers: Vec<i32>) -> i32 {
        numbers.iter().sum()
    }

    fn get_names(people: Vec<Person>) -> Vec<String> {
        people.iter().map(|p| p.name.clone()).collect()
    }
}
