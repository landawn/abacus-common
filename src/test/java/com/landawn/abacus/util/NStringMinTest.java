package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;

public class NStringMinTest extends AbstractParserTest {

    private static final Random rand = new Random();

    protected void changeCPUCoreNum(final int c) {
    }

    @Test
    public void test_min_max_median() {
        final int len = 17;

        {
            assertEquals('1', N.min('1', '2'));
            assertEquals('1', N.min('2', '1'));
            assertEquals('2', N.max('1', '2'));
            assertEquals('2', N.max('2', '2'));
            assertEquals('1', N.lowerMedian('1', '2'));
            assertEquals('1', N.lowerMedian('2', '1'));

            assertEquals('1', N.min('1', '2', '3'));
            assertEquals('1', N.min('2', '1', '3'));
            assertEquals('1', N.min('3', '2', '1'));

            assertEquals('3', N.max('1', '2', '3'));
            assertEquals('3', N.max('2', '1', '3'));
            assertEquals('3', N.max('3', '2', '1'));

            assertEquals('2', N.median('1', '2', '3'));
            assertEquals('2', N.median('2', '1', '3'));
            assertEquals('2', N.median('3', '2', '1'));
            assertEquals('2', N.median('1', '3', '2'));

            char[] a = new char[len];
            for (int i = 0; i < len; i++) {
                a[i] = (char) rand.nextInt(1000);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of('1');
            assertEquals('1', N.lowerMedian(a));

            a = Array.of('2', '1');
            assertEquals('1', N.lowerMedian(a));

            a = Array.of('2', '1', '3');
            assertEquals('2', N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
            assertEquals((byte) 1, N.min((byte) 2, (byte) 1));
            assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
            assertEquals((byte) 2, N.max((byte) 2, (byte) 2));
            assertEquals((byte) 1, N.lowerMedian((byte) 1, (byte) 2));
            assertEquals((byte) 1, N.lowerMedian((byte) 2, (byte) 1));

            assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 1, N.min((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 1, N.min((byte) 3, (byte) 2, (byte) 1));

            assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 3, N.max((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 3, N.max((byte) 3, (byte) 2, (byte) 1));

            assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 2, N.median((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 2, N.median((byte) 3, (byte) 2, (byte) 1));
            assertEquals((byte) 2, N.median((byte) 1, (byte) 3, (byte) 2));

            byte[] a = new byte[len];
            for (int i = 0; i < len; i++) {
                a[i] = (byte) rand.nextInt(127);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            assertTrue(count <= (a.length) / 2);

            a = Array.of((byte) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((byte) 2, (byte) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((byte) 2, (byte) 1, (byte) 3);
            assertEquals(2, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((short) 1, N.min((short) 1, (short) 2));
            assertEquals((short) 1, N.min((short) 2, (short) 1));
            assertEquals((short) 2, N.max((short) 1, (short) 2));
            assertEquals((short) 2, N.max((short) 2, (short) 2));
            assertEquals((short) 1, N.lowerMedian((short) 1, (short) 2));
            assertEquals((short) 1, N.lowerMedian((short) 2, (short) 1));

            assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
            assertEquals((short) 1, N.min((short) 2, (short) 1, (short) 3));
            assertEquals((short) 1, N.min((short) 3, (short) 2, (short) 1));

            assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
            assertEquals((short) 3, N.max((short) 2, (short) 1, (short) 3));
            assertEquals((short) 3, N.max((short) 3, (short) 2, (short) 1));

            assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
            assertEquals((short) 2, N.median((short) 2, (short) 1, (short) 3));
            assertEquals((short) 2, N.median((short) 3, (short) 2, (short) 1));
            assertEquals((short) 2, N.median((short) 1, (short) 3, (short) 2));

            short[] a = new short[len];
            for (int i = 0; i < len; i++) {
                a[i] = (short) rand.nextInt(127);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((short) 1);
            assertEquals((short) 1, N.lowerMedian(a));

            a = Array.of((short) 2, (short) 1);
            assertEquals((short) 1, N.lowerMedian(a));

            a = Array.of((short) 2, (short) 1, (short) 3);
            assertEquals(2, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min(1, 2));
            assertEquals(1, N.min(2, 1));
            assertEquals(2, N.max(1, 2));
            assertEquals(2, N.max(2, 2));
            assertEquals(1, N.lowerMedian(1, 2));
            assertEquals(1, N.lowerMedian(2, 1));

            assertEquals(1, N.min(1, 2, 3));
            assertEquals(1, N.min(2, 1, 3));
            assertEquals(1, N.min(3, 2, 1));

            assertEquals(3, N.max(1, 2, 3));
            assertEquals(3, N.max(2, 1, 3));
            assertEquals(3, N.max(3, 2, 1));

            assertEquals(2, N.median(1, 2, 3));
            assertEquals(2, N.median(2, 1, 3));
            assertEquals(2, N.median(3, 2, 1));
            assertEquals(2, N.median(1, 3, 2));

            int[] a = new int[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt();
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of(1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of(2, 1);
            assertEquals(1, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min((long) 1, (long) 2));
            assertEquals(1, N.min((long) 2, (long) 1));
            assertEquals(2, N.max((long) 1, (long) 2));
            assertEquals(2, N.max((long) 2, (long) 2));
            assertEquals(1, N.lowerMedian((long) 1, (long) 2));
            assertEquals(1, N.lowerMedian((long) 2, (long) 1));

            assertEquals(1, N.min((long) 1, (long) 2, (long) 3));
            assertEquals(1, N.min((long) 2, (long) 1, (long) 3));
            assertEquals(1, N.min((long) 3, (long) 2, (long) 1));

            assertEquals(3, N.max((long) 1, (long) 2, (long) 3));
            assertEquals(3, N.max((long) 2, (long) 1, (long) 3));
            assertEquals(3, N.max((long) 3, (long) 2, (long) 1));

            assertEquals(2, N.median((long) 1, (long) 2, (long) 3));
            assertEquals(2, N.median((long) 2, (long) 1, (long) 3));
            assertEquals(2, N.median((long) 3, (long) 2, (long) 1));
            assertEquals(2, N.median((long) 1, (long) 3, (long) 2));

            long[] a = new long[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final long min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final long max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final long median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((long) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((long) 2, (long) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((long) 2, (long) 1, (long) 3);
            assertEquals(2, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min((float) 1, (float) 2));
            assertEquals(1, N.min((float) 2, (float) 1));
            assertEquals(2, N.max((float) 1, (float) 2));
            assertEquals(2, N.max((float) 2, (float) 2));
            assertEquals(1, N.lowerMedian((float) 1, (float) 2));
            assertEquals(1, N.lowerMedian((float) 2, (float) 1));

            assertEquals(1, N.min((float) 1, (float) 2, (float) 3));
            assertEquals(1, N.min((float) 2, (float) 1, (float) 3));
            assertEquals(1, N.min((float) 3, (float) 2, (float) 1));

            assertEquals(3, N.max((float) 1, (float) 2, (float) 3));
            assertEquals(3, N.max((float) 2, (float) 1, (float) 3));
            assertEquals(3, N.max((float) 3, (float) 2, (float) 1));

            assertEquals(2, N.median((float) 1, (float) 2, (float) 3));
            assertEquals(2, N.median((float) 2, (float) 1, (float) 3));
            assertEquals(2, N.median((float) 3, (float) 2, (float) 1));
            assertEquals(2, N.median((float) 1, (float) 3, (float) 2));

            float[] a = new float[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final float min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final float max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final float median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((float) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((float) 2, (float) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((float) 2, (float) 1, (float) 3);
            assertEquals(2, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min((double) 1, (double) 2));
            assertEquals(1, N.min((double) 2, (double) 1));
            assertEquals(2, N.max((double) 1, (double) 2));
            assertEquals(2, N.max((double) 2, (double) 2));
            assertEquals(1, N.lowerMedian((double) 1, (double) 2));
            assertEquals(1, N.lowerMedian((double) 2, (double) 1));

            assertEquals(1, N.min((double) 1, (double) 2, (double) 3));
            assertEquals(1, N.min((double) 2, (double) 1, (double) 3));
            assertEquals(1, N.min((double) 3, (double) 2, (double) 1));

            assertEquals(3, N.max((double) 1, (double) 2, (double) 3));
            assertEquals(3, N.max((double) 2, (double) 1, (double) 3));
            assertEquals(3, N.max((double) 3, (double) 2, (double) 1));

            assertEquals(2, N.median((double) 1, (double) 2, (double) 3));
            assertEquals(2, N.median((double) 2, (double) 1, (double) 3));
            assertEquals(2, N.median((double) 3, (double) 2, (double) 1));
            assertEquals(2, N.median((double) 1, (double) 3, (double) 2));

            double[] a = new double[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final double min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final double max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final double median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((double) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((double) 2, (double) 1);
            assertEquals(1, N.lowerMedian(a));

            a = Array.of((double) 2, (double) 1, (double) 3);
            assertEquals(2, N.lowerMedian(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {

            List<Integer> a = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                a.add(rand.nextInt());
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a.get(i) >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a.get(i) <= max);
            }

            final int median = N.lowerMedian(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a.get(i) < median) {
                    count++;
                }
            }

            assertTrue(count <= len / 2);

            a = CommonUtil.toList(1);
            assertEquals(1, N.lowerMedian(a).intValue());

            a = CommonUtil.toList(2, 1);
            assertEquals(1, N.lowerMedian(a).intValue());

            a = CommonUtil.toList(2, 1, 3);
            assertEquals(2, N.lowerMedian(a).intValue());

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.lowerMedian(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(5, N.lowerMedian(1, 2, 3, 4, 5, 6, 7, 8, 9));

            assertEquals(5, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8));

            assertEquals(4, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7));

            assertEquals(3, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6));

            assertEquals(3, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5));

            assertEquals(4, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6));

            assertEquals(4, N.lowerMedian(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5));
        }

        {
            assertEquals(5, Median.of(1, 2, 3, 4, 5, 6, 7, 8, 9).left().intValue());

            assertEquals(5, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7).left().intValue());

            assertEquals(3, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6).left().intValue());

            assertEquals(3, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5).left().intValue());
        }

        {
            assertEquals(true, Median.of(1, 2, 3, 4, 5, 6, 7, 8, 9).right().isEmpty());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8).right().isEmpty());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7).right().isEmpty());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6).right().get());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5).right().isEmpty());

            assertEquals(5, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6).right().get());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5).right().isEmpty());
        }
    }
}
