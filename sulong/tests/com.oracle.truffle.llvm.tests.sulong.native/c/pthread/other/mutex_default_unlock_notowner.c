// unlock a currently by other thread locked mutex: undefined behaviour
// tested functions here: pthread_mutexattr_init, pthread_mutexattr_settype(),
// pthread_mutex_init(), pthread_mutex_lock, pthread_mutex_unlock

#include <pthread.h>
#include <stdio.h>

pthread_mutexattr_t attr;
pthread_mutex_t mutex;

void *unlock_mut(void *ptr);

int main() {
  int result;
  if ((result = pthread_mutexattr_init(&attr)) != 0) {
    printf("pthread_mutexattr_init() gives return code: %d\n", result);
  }
  if ((result = pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_DEFAULT)) != 0) {
    printf("pthread_mutexattr_settype() gives return code: %d\n", result);
  }
  if ((result = pthread_mutex_init(&mutex, &attr)) != 0) {
    printf("pthread_mutex_init() gives return code: %d\n", result);
  }
  if ((result = pthread_mutex_lock(&mutex)) != 0) {
    printf("pthread_mutex_lock() gives return code: %d\n", result);
  }
  pthread_t t;
  pthread_create(&t, NULL, unlock_mut, NULL);
  pthread_join(t, NULL);
  if ((result = pthread_mutex_unlock(&mutex)) != 0) {
    printf("pthread_mutex_unlock() gives return code: %d\n", result);
  }
  return 0;
}

void *unlock_mut(void *ptr) {
  int result = pthread_mutex_unlock(&mutex);
  printf("result of unlock: %d\n", result);
  pthread_exit(NULL);
}
