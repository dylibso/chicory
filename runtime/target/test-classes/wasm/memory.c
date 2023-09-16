#include <string.h>
#include <stdlib.h>

int run() {
    char *test = (char*) malloc(12*sizeof(char));
    strcpy(test, "testingonly");
    return strlen(test);
}