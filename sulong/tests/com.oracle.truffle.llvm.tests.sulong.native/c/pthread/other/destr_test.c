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
