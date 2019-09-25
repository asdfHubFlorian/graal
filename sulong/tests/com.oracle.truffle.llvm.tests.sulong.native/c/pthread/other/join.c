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
// joining a thread that increments an int and sleeps 5 seconds
// tests pthread_create, pthread_join

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

void *inc_a_lot(void *ptr);

int main() {
  pthread_t th1;
  int val = 0;
  pthread_create(&th1, NULL, inc_a_lot, (void *)&val);
  pthread_join(th1, NULL);
  printf("now value is %d\n", val);
  return val;
}

void *inc_a_lot(void *ptr) {
  int i = 1;
  while (i <= 5) {
    *((int *)ptr) += i++;
    sleep(1);
  }
  pthread_exit(NULL);
}
