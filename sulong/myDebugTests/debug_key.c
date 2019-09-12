#include <pthread.h>
#include <stdio.h>

pthread_key_t key;

int main() {
	pthread_key_create(&key, NULL);
	printf("%d", key);
	pthread_getspecific(key);
}
