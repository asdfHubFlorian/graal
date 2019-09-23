// tests the get_specific and set_specific functions
// and destructor functionality

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

pthread_key_t key;

void destr(void *param) {
  printf("destructor invoked with param %d\n\n", *((int *)param));
}

void *t_func(void *param) {
  int *i = malloc(sizeof(int));
  *i = 15;
  pthread_setspecific(key, (void *)i);
  printf("thread sets 15 and gets: %d\n\n", *((int *)pthread_getspecific(key)));
  pthread_exit(NULL);
}

int main() {
  pthread_key_create(&key, destr);
  pthread_t t;
  pthread_create(&t, NULL, t_func, NULL);
  pthread_join(t, NULL);
}
