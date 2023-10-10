const VOWELS: &[char] = &['a', 'A', 'e', 'E', 'i', 'I', 'o', 'O', 'u', 'U'];

#[no_mangle]
pub extern fn count_vowels() -> i32 {
    let s = String::from("Hello World!");
    let mut count: i32 = 0;
    for ch in s.chars() {
        if VOWELS.contains(&ch) {
            count += 1;
        }
    }
    count
}