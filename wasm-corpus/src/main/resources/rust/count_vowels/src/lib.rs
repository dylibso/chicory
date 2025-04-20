const VOWELS: &[char] = &['a', 'A', 'e', 'E', 'i', 'I', 'o', 'O', 'u', 'U'];

use std::mem;
use std::slice;
use std::str;

#[no_mangle]
pub extern "C" fn alloc(len: i32) -> *const u8 {
    let mut buf = Vec::with_capacity(len as usize);
    let ptr = buf.as_mut_ptr();
    // tell Rust not to clean this up
    mem::forget(buf);
    ptr
}

#[no_mangle]
pub unsafe extern "C" fn dealloc(ptr: &mut u8, len: i32) {
    let _ = Vec::from_raw_parts(ptr, 0, len as usize);
}

#[no_mangle]
pub extern "C" fn count_vowels(ptr: i32, len: i32) -> i32 {
    let bytes = unsafe { slice::from_raw_parts(ptr as *const u8, len as usize) };
    let s = str::from_utf8(bytes).unwrap();
    let mut count: i32 = 0;
    for ch in s.chars() {
        if VOWELS.contains(&ch) {
            count += 1;
        }
    }
    count
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_count_vowels() {
        count_vowels(0, -1);
    }


}