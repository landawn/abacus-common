package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DoubleListReplaceTest extends DoubleListTestSupport {

    @Test
    public void testReplaceRange() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleList replacement = DoubleList.of(10.1, 10.2);
            list.replaceRange(1, 3, replacement);

            assertEquals(5, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(10.1, list.get(1), DELTA);
            assertEquals(10.2, list.get(2), DELTA);
            assertEquals(4.4, list.get(3), DELTA);
            assertEquals(5.5, list.get(4), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            double[] replacement = { 10.1, 10.2 };
            list.replaceRange(1, 3, replacement);

            assertEquals(5, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(10.1, list.get(1), DELTA);
            assertEquals(10.2, list.get(2), DELTA);
            assertEquals(4.4, list.get(3), DELTA);
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
            dl.replaceRange(1, 4, new double[] { 20.0, 30.0 });
            assertEquals(4, dl.size());
            assertEquals(20.0, dl.get(1), DELTA);
            assertEquals(30.0, dl.get(2), DELTA);
            assertEquals(5.0, dl.get(3), DELTA);
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0, 5.0);
            dl.replaceRange(1, 3, DoubleList.of(20.0, 30.0, 40.0));
            assertEquals(6, dl.size());
            assertEquals(20.0, dl.get(1), DELTA);
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            DoubleList replacement = new DoubleList();
            list.replaceRange(1, 3, replacement);

            assertEquals(2, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            double[] replacement = {};
            list.replaceRange(1, 3, replacement);

            assertEquals(2, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            list.replaceRange(1, 3, new DoubleList());
            assertEquals(3, list.size());
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
            assertEquals(5.5, list.get(2), DELTA);
        }
    }

    @Test
    public void testReplaceAll() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
            list.replaceAll(x -> x * 2.0);

            assertEquals(2.0, list.get(0), DELTA);
            assertEquals(4.0, list.get(1), DELTA);
            assertEquals(6.0, list.get(2), DELTA);
            assertEquals(8.0, list.get(3), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

            int count = list.replaceAll(2.2, 20.2);
            assertEquals(2, count);
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(20.2, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
            assertEquals(20.2, list.get(3), DELTA);
            assertEquals(5.5, list.get(4), DELTA);
        }
    }

    @Test
    public void testReplaceAll_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        int count = list.replaceAll(Double.NaN, 0.0);
        assertEquals(2, count);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
    }

    @Test
    public void testReplaceAll_Null() {
        DoubleList nonEmpty = DoubleList.of(1.0, 2.0, 3.0);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.DoubleUnaryOperator) null));

        DoubleList empty = new DoubleList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.DoubleUnaryOperator) null));
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        boolean result = list.replaceIf(x -> x > 3.0, 99.9);

        assertTrue(result);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(99.9, list.get(2), DELTA);
        assertEquals(99.9, list.get(3), DELTA);
        assertEquals(99.9, list.get(4), DELTA);
    }

    @Test
    public void testReplaceIf_Infinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, 2.2, Double.NEGATIVE_INFINITY });
        boolean result = list.replaceIf(Double::isInfinite, 0.0);
        assertTrue(result);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(0.0, list.get(3), DELTA);
    }
}
