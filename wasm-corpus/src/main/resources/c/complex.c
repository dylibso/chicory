#include <stdio.h>

int complicatedFunction(int a, int b) {
    int result = 0;

    // Perform a complex computation
    for (int i = 1; i <= a; i++) {
        for (int j = 1; j <= b; j++) {
            if (i % 2 == 0 && j % 2 == 0) {
                result += (i * i) / (j * j);
            } else if (i % 2 != 0 && j % 2 != 0) {
                result -= (i * i) * (j * j);
            } else {
                result += i * j;
            }
        }
    }

    return result;
}

int run() {
    int a = 7;
    int b = 4;
    int result = complicatedFunction(a, b);
    return result;
}
