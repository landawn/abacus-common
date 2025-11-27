package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class ArrayUtilTest extends AbstractTest {

    @Test
    public void test_parallel_sort_perf() {
        final int arrayLength = 1000;
        final int loopNum = 1;
        {
            final int[] a = Array.random(arrayLength);
            Profiler.run(1, loopNum, 3, "Arrays.sort(int[])", () -> Arrays.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.sort(int[])", () -> CommonUtil.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "Arrays.parallelSort(int[])", () -> Arrays.parallelSort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.parallelSort(int[])", () -> CommonUtil.parallelSort(a.clone())).printResult();
        }

        {
            final long[] a = LongList.random(arrayLength).toArray();
            Profiler.run(1, loopNum, 3, "Arrays.sort(long[])", () -> Arrays.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.sort(long[])", () -> CommonUtil.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "Arrays.parallelSort(long[])", () -> Arrays.parallelSort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.parallelSort(long[])", () -> CommonUtil.parallelSort(a.clone())).printResult();
        }

        {
            final double[] a = DoubleList.random(arrayLength).toArray();
            Profiler.run(1, loopNum, 3, "Arrays.sort(double[])", () -> Arrays.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.sort(double[])", () -> CommonUtil.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "Arrays.parallelSort(double[])", () -> Arrays.parallelSort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.parallelSort(double[])", () -> CommonUtil.parallelSort(a.clone())).printResult();
        }

        {
            final String[] a = new String[2000];
            for (int i = 0; i < a.length; i++) {
                a[i] = Strings.uuid();
            }

            Profiler.run(1, loopNum, 3, "Arrays.sort(Object[])", () -> Arrays.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.sort(Object[])", () -> CommonUtil.sort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "Arrays.parallelSort(Object[])", () -> Arrays.parallelSort(a.clone())).printResult();
            Profiler.run(1, loopNum, 3, "N.parallelSort(Object[])", () -> CommonUtil.parallelSort(a.clone())).printResult();
        }
    }

    @Test
    public void test_transpose() {

        final int[][] a = { { 1, 2, 3, 4 }, { 5, 5, 5, 5 } };
        final int[][] b = Array.transpose(a);

        N.println(CommonUtil.deepToString(a));
        N.println(Strings.repeat("=", 80));
        N.println(CommonUtil.deepToString(b));

        final Integer[][] c = Array.box(a);

        final Integer[][] d = Array.transpose(c);
        N.println(Strings.repeat("=", 80));
        N.println(CommonUtil.deepToString(d));
    }

    @Test
    public void test_removeElementAll() {

        int[] a = { 1, 2, 3, 3, 4, 5, 5, 5, 5 };

        a = N.removeAllOccurrences(a, 3);

        N.println(a);

        a = N.removeAllOccurrences(a, 4);

        N.println(a);
        a = N.removeAllOccurrences(a, 5);

        N.println(a);
    }

    @Test
    public void test_clone() {
        final int[] a = { 1, 2, 3 };
        final int[] b = a.clone();
        N.println(a);
        N.println(b);

        assertFalse(a == b);
        assertTrue(a[1] == b[1]);
        a[1] = 0;
        assertFalse(a[1] == b[1]);

        N.println(a);
        N.println(b);

        final String[] strs = { null, null, "abc" };

        N.println(CommonUtil.indexOf(strs, "abc"));

        N.println(strs[0] instanceof String);

        N.println(Array.of(WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.WEDNESDAY).clone());
    }

    @Test
    public void test_repeat() {
        Byte[] a = Array.repeat(Byte.valueOf(Byte.MIN_VALUE), 10);
        N.println(a);

        a = Array.repeatNonNull(Byte.MIN_VALUE, 10);
        N.println(a);

        final byte[] b = Array.repeat(Byte.MAX_VALUE, 10);
        N.println(b);
    }

}
