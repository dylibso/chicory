int run(int n) {
    if (n <= 0) {
        return 1;
    }
    return run(n - 1) + run(n - 1);
}
