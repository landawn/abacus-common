/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class ArraySortPerformanceTest extends AbstractTest {

    @Test
    public void test_00() {
        System.out.println("test nothing");
    }

    //    @Test
    //    public void test_sort_big_long_array() {
    //        final Random rand = new Random();
    //        final long[] array = new long[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = rand.nextLong();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> N.sort(array)).printResult();
    //    }
    //
    //    @Test
    //    public void test_sort_big_long_array_by_parallel_sort() {
    //        final Random rand = new Random();
    //        final long[] array = new long[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = rand.nextLong();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> N.parallelSort(array)).printResult();
    //    }
    //
    //    @Test
    //    public void test_sort_big_long_array_by_parallel_sort_2() {
    //        final Random rand = new Random();
    //        final long[] array = new long[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = rand.nextLong();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            // Arrays.parallelSort(array);
    //
    //        }).printResult();
    //    }
    //
    //    @Test
    //    public void test_sort_big_string_array() {
    //        final String[] array = new String[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = Strings.uuid();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> N.sort(array)).printResult();
    //    }
    //
    //    @Test
    //    public void test_sort_big_string_array_by_parallel_sort() {
    //        final String[] array = new String[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = Strings.uuid();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> N.parallelSort(array)).printResult();
    //    }
    //
    //    @Test
    //    public void test_sort_big_string_array_by_parallel_sort_2() {
    //        final String[] array = new String[1000 * 10];
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            for (int i = 0, len = array.length; i < len; i++) {
    //                array[i] = Strings.uuid();
    //            }
    //
    //        }).printResult();
    //
    //        Profiler.run(1, 1, 1, () -> {
    //            // Arrays.parallelSort(array);
    //
    //        }).printResult();
    //    }

}
