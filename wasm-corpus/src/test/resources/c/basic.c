#include <string.h>
#include <stdlib.h>

typedef struct Person {
    char name[50];
    int id;
} person;

int run() {
    person p1;
    strcpy(p1.name, "Benjamin");
    return strlen(p1.name) + 34;
}