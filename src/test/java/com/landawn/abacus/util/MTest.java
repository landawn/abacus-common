package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Consumer;

public class MTest {
    static final Random rand = new Random();

    @Test
    public void test_sort_int() {
        final int len = 100_000_000;
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = rand.nextInt();
        }

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));

        assertTrue(N.isSorted(a));
    }

    @Test
    public void test_sort_long() {
        final int len = 100_000_000;
        final long[] a = new long[len];

        for (int i = 0; i < len; i++) {
            a[i] = rand.nextLong();
        }

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));

        assertTrue(N.isSorted(a));
    }

    @Test
    public void test_sort_float() {
        final int len = 100_000_000;
        final float[] a = new float[len];

        for (int i = 0; i < len; i++) {
            a[i] = rand.nextFloat();
        }

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));

        assertTrue(N.isSorted(a));
    }

    @Test
    public void test_sort_double() {
        final int len = 100_000_000;
        final double[] a = new double[len];

        for (int i = 0; i < len; i++) {
            a[i] = rand.nextDouble();
        }

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));

        assertTrue(N.isSorted(a));
    }

    @Test
    public void test_sort_string() {
        final int len = 10_000_000;
        final String[] a = new String[len];

        for (int i = 0; i < len; i++) {
            a[i] = Strings.guid();
        }

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));

        assertTrue(N.isSorted(a));
    }

    @Test
    public void test_pennySort() {
        final File file = new File("C:\\Users\\haiyangl\\Downloads\\pennyinput");
        final String[] a = new String[10000000];
        final MutableInt idx = new MutableInt();

        IOUtil.forLines(file, (Consumer<String>) line -> {
            if (line != null) {
                a[idx.value()] = line;
                idx.increment();
            }
        });

        N.println("Sorting");
        long startTime = System.currentTimeMillis();
        // N.sort(a);
        N.parallelSort(a);
        N.println("Took: " + (System.currentTimeMillis() - startTime));
    }

}
