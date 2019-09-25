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
// this testcase tests passing arguments to threads and getting return values
// passing the values directly
// tests pthread_create, pthread_exit, pthread_join

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

void *add_up_to(void *to);

int main() {
  pthread_t th1, th2;
  long val1 = 150;
  long val2 = 250;
  pthread_create(&th1, NULL, add_up_to, (void *)val1);
  pthread_create(&th2, NULL, add_up_to, (void *)val2);
  void *ret1, *ret2;
  pthread_join(th1, &ret1);
  pthread_join(th2, &ret2);
  long sum = ((long)ret1) + ((long)ret2);
  printf("thread1 returns sum %ld\n", (long)ret1);
  printf("thread2 returns sum %ld\n", (long)ret2);
  printf("%ld\n", sum);
  if (sum == 42700)
    return 0;
  else
    return 1;
}

void *add_up_to(void *to) {
  long stop = (long)to;
  long i = 1;
  long *sum = malloc(sizeof(long));
  *sum = 0;
  while (i <= stop) {
    *sum += i++;
  }
  pthread_exit((void *)*sum);
}
