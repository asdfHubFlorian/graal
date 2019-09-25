/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
// thread 0 returns with 0
// thread i joins / waitsfor thread i-1, and returns return-value(thread i-1)+1
// return value should be NUM_THREADS-1
// warning: int casted to void-pointer (different size)
// tests pthread_create, pthread_exit, pthread_join (also in not main-thread)

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define NUM_THREADS 5

void *join_and_wait(void *ptr);
void *wait(void *ptr);

int main() {
  pthread_t threads[NUM_THREADS];
  pthread_create(&threads[0], NULL, wait, NULL);
  int i;
  for (i = 1; i < NUM_THREADS; i++) {
    pthread_create(&threads[i], NULL, join_and_wait, (void *)threads[i - 1]);
  }
  int retval;
  pthread_join(threads[NUM_THREADS - 1], (void *)&retval);
  printf("now value is %d\n", retval);
  return retval;
}

void *join_and_wait(void *ptr) {
  int retval;
  pthread_join((pthread_t)ptr, (void *)&retval);
  printf("thread with retval %d finished\n", retval);
  sleep(1);
  pthread_exit((void *)retval + 1);
}

void *wait(void *ptr) {
  sleep(1);
  pthread_exit(0);
}
