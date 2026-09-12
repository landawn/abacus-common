package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NarrowSumTest extends TestBase {

    @Test
    void charSumChecksTheFinalIntBoundaryAndCountsUtf16Units() {
        final char[] values = new char[32770];
        Arrays.fill(values, 0, 32768, Character.MAX_VALUE);
        values[32768] = 32767;
        assertEquals(Integer.MAX_VALUE, N.sum(values, 0, 32769));
        assertEquals(Integer.MAX_VALUE, N.sum(values));
        values[32769] = 1;
        assertThrows(ArithmeticException.class, () -> N.sum(values));
        assertThrows(ArithmeticException.class, () -> N.sum(values, 0, values.length));
        assertEquals(32768, N.sum(values, 32768, values.length));
        assertEquals(0xD83D + 0xDE00, N.sum('\uD83D', '\uDE00'));
    }

    @Test
    void shortSumChecksBothBoundariesAfterCancellation() {
        final short[] positive = new short[65541];
        Arrays.fill(positive, 0, 65538, Short.MAX_VALUE);
        positive[65538] = 1;
        assertEquals(Integer.MAX_VALUE, N.sum(positive));
        positive[65539] = 1;
        assertThrows(ArithmeticException.class, () -> N.sum(positive));
        assertThrows(ArithmeticException.class, () -> N.sum(positive, 0, 65540));
        positive[65540] = -1;
        assertEquals(Integer.MAX_VALUE, N.sum(positive));

        final short[] negative = new short[65538];
        Arrays.fill(negative, 0, 65536, Short.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, N.sum(negative));
        negative[65536] = -1;
        assertThrows(ArithmeticException.class, () -> N.sum(negative));
        assertThrows(ArithmeticException.class, () -> N.sum(negative, 0, 65537));
        negative[65537] = 1;
        assertEquals(Integer.MIN_VALUE, N.sum(negative));
        assertEquals(0, N.sum(negative, 65536, negative.length));
    }

    @Test
    void byteSumChecksBothBoundariesAfterCancellation() {
        final byte[] values = new byte[16909323];
        Arrays.fill(values, 0, 16909320, Byte.MAX_VALUE);
        values[16909320] = 7;
        assertEquals(Integer.MAX_VALUE, N.sum(values));
        values[16909321] = 1;
        assertThrows(ArithmeticException.class, () -> N.sum(values));
        assertThrows(ArithmeticException.class, () -> N.sum(values, 0, 16909322));
        values[16909322] = -1;
        assertEquals(Integer.MAX_VALUE, N.sum(values));

        Arrays.fill(values, (byte) 0);
        Arrays.fill(values, 0, 16777216, Byte.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, N.sum(values));
        values[16777216] = -1;
        assertThrows(ArithmeticException.class, () -> N.sum(values));
        assertThrows(ArithmeticException.class, () -> N.sum(values, 0, 16777217));
        values[16777217] = 1;
        assertEquals(Integer.MIN_VALUE, N.sum(values));
        assertEquals(0, N.sum(values, 16777216, values.length));
    }

    @Test
    void narrowSumsPreserveNullEmptyAndRangeValidation() {
        assertEquals(0, N.sum((char[]) null));
        assertEquals(0, N.sum((byte[]) null));
        assertEquals(0, N.sum((short[]) null));
        assertEquals(0, N.sum((char[]) null, 0, 0));
        assertEquals(0, N.sum((byte[]) null, 0, 0));
        assertEquals(0, N.sum((short[]) null, 0, 0));
        assertEquals(0, N.sum(new char[0]));
        assertEquals(0, N.sum(new byte[0]));
        assertEquals(0, N.sum(new short[0]));
        assertEquals(0, N.sum(new char[] { 'x' }, 1, 1));
        assertEquals(0, N.sum(new byte[] { 1 }, 1, 1));
        assertEquals(0, N.sum(new short[] { 1 }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum((char[]) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum((byte[]) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum((short[]) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new char[] { 'x' }, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new byte[] { 1 }, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new short[] { 1 }, 0, 2));
    }
}
