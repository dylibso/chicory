use std::alloc::Layout;
use std::sync::atomic::{AtomicU32, Ordering};
use std::sync::{Arc, Mutex, Condvar};
use std::collections::VecDeque;

#[unsafe(no_mangle)]
pub extern "C" fn __malloc(size: usize, align: usize) -> *mut u8 {
    let Ok(layout) = Layout::from_size_align(size, align) else {
        return std::ptr::null_mut();
    };
    unsafe {
        std::alloc::alloc(layout)
    }
}

// Thread pool
static POOL: std::sync::OnceLock<ThreadPool> = std::sync::OnceLock::new();

type Job = Box<dyn FnOnce() + Send>;

struct ThreadPool {
    mutex: Mutex<PoolState>,
    work_available: Condvar,
    worker_available: Condvar,
}

struct PoolState {
    idle_workers: u32,
    shutdown: bool,
    work_queue: VecDeque<Job>,
}

fn get_pool() -> &'static ThreadPool {
    POOL.get_or_init(|| ThreadPool {
        mutex: Mutex::new(PoolState {
            idle_workers: 0,
            shutdown: false,
            work_queue: VecDeque::new(),
        }),
        work_available: Condvar::new(),
        worker_available: Condvar::new(),
    })
}

/// Register the calling thread in the pool. Loops until shutdown.
#[no_mangle]
pub extern "C" fn register_thread() {
    let pool = get_pool();
    
    loop {
        let job: Option<Job>;
        {
            let mut state = pool.mutex.lock().unwrap();
            state.idle_workers += 1;
            pool.worker_available.notify_one();
            
            // Wait for work OR shutdown
            state = pool.work_available.wait_while(state, |s| {
                s.work_queue.is_empty() && !s.shutdown
            }).unwrap();
            
            state.idle_workers -= 1;
            
            if state.shutdown && state.work_queue.is_empty() {
                return; // Exit the loop
            }
            
            job = state.work_queue.pop_front();
        }
        
        if let Some(j) = job {
            j();
        }
    }
}

/// Run a closure on a pooled thread. Blocks if no worker is available.
fn run_on_pool<F: FnOnce() + Send + 'static>(f: F) {
    let pool = get_pool();
    let mut state = pool.mutex.lock().unwrap();
    
    // Wait for an idle worker
    state = pool.worker_available.wait_while(state, |s| s.idle_workers == 0).unwrap();
    
    state.work_queue.push_back(Box::new(f));
    pool.work_available.notify_one();
}

#[no_mangle]
pub extern "C" fn parallel_sieve_bitset(limit: usize) -> usize {
    let num_words = (limit + 32) / 32;
    let sieve: Arc<Vec<AtomicU32>> = Arc::new(
        (0..num_words).map(|_| AtomicU32::new(0)).collect()
    );
    
    let sqrt_limit = (limit as f64).sqrt() as usize;
    let pending = Arc::new(AtomicU32::new((sqrt_limit - 1) as u32));
    let done_lock = Arc::new(Mutex::new(()));
    let done = Arc::new(Condvar::new());
    
    for p in 2..=sqrt_limit {
        let sieve = Arc::clone(&sieve);
        let pending = Arc::clone(&pending);
        let done_lock = Arc::clone(&done_lock);
        let done = Arc::clone(&done);
        
        run_on_pool(move || {
            let word = p / 32;
            let bit = 1u32 << (p % 32);
            if sieve[word].load(Ordering::Relaxed) & bit == 0 {
                for multiple in (p * p..=limit).step_by(p) {
                    let w = multiple / 32;
                    let b = 1u32 << (multiple % 32);
                    sieve[w].fetch_or(b, Ordering::Relaxed);
                }
            }
            
            if pending.fetch_sub(1, Ordering::SeqCst) == 1 {
                let _lock = done_lock.lock().unwrap();
                done.notify_one();
            }
        });
    }
    
    // Wait for all jobs
    let lock = done_lock.lock().unwrap();
    drop(done.wait_while(lock, |_| pending.load(Ordering::SeqCst) > 0).unwrap());
    
    // Count primes
    (2..=limit)
        .filter(|&i| sieve[i / 32].load(Ordering::Relaxed) & (1 << (i % 32)) == 0)
        .count()
}

/// Shutdown the pool, releasing all worker threads.
#[no_mangle]
pub extern "C" fn shutdown() {
    let pool = get_pool();
    let mut state = pool.mutex.lock().unwrap();
    state.shutdown = true;
    pool.work_available.notify_all(); // Wake all waiting workers
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::thread;

    #[test]
    fn test_parallel_sieve() {
        // Spawn some worker threads
        let handles: Vec<_> = (0..4)
            .map(|_| thread::spawn(|| register_thread()))
            .collect();
                
        // Run the sieve
        let count = parallel_sieve_bitset(1_000_000);
        
        assert_eq!(count, 78498, "Expected 78498 primes up to 1,000,000");

        shutdown();
        
        // Wait for all workers to exit
        for h in handles {
            h.join().unwrap();
        }
    }
}

