package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FractionTest extends TestBase {

    private static final int SKIP = 500;

    @Test
    public void testConstants() {
        N.println(Fraction.of(1, 2));
        N.println(Fraction.of(2, 4));
        assertEquals(0, Fraction.ZERO.getNumerator());
        assertEquals(1, Fraction.ZERO.getDenominator());

        assertEquals(1, Fraction.ONE.getNumerator());
        assertEquals(1, Fraction.ONE.getDenominator());

        assertEquals(1, Fraction.ONE_HALF.getNumerator());
        assertEquals(2, Fraction.ONE_HALF.getDenominator());

        assertEquals(1, Fraction.ONE_THIRD.getNumerator());
        assertEquals(3, Fraction.ONE_THIRD.getDenominator());

        assertEquals(2, Fraction.TWO_THIRDS.getNumerator());
        assertEquals(3, Fraction.TWO_THIRDS.getDenominator());

        assertEquals(1, Fraction.ONE_QUARTER.getNumerator());
        assertEquals(4, Fraction.ONE_QUARTER.getDenominator());

        assertEquals(2, Fraction.TWO_QUARTERS.getNumerator());
        assertEquals(4, Fraction.TWO_QUARTERS.getDenominator());

        assertEquals(3, Fraction.THREE_QUARTERS.getNumerator());
        assertEquals(4, Fraction.THREE_QUARTERS.getDenominator());

        assertEquals(1, Fraction.ONE_FIFTH.getNumerator());
        assertEquals(5, Fraction.ONE_FIFTH.getDenominator());

        assertEquals(2, Fraction.TWO_FIFTHS.getNumerator());
        assertEquals(5, Fraction.TWO_FIFTHS.getDenominator());

        assertEquals(3, Fraction.THREE_FIFTHS.getNumerator());
        assertEquals(5, Fraction.THREE_FIFTHS.getDenominator());

        assertEquals(4, Fraction.FOUR_FIFTHS.getNumerator());
        assertEquals(5, Fraction.FOUR_FIFTHS.getDenominator());
    }

    @Test
    public void testFactory_int_int() {
        Fraction f = null;

        f = Fraction.of(0, 1);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(0, 2);
        assertEquals(0, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(1, 1);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 1);
        assertEquals(2, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(23, 345);
        assertEquals(23, f.getNumerator());
        assertEquals(345, f.getDenominator());

        f = Fraction.of(22, 7);
        assertEquals(22, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f = Fraction.of(-6, 10);
        assertEquals(-6, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f = Fraction.of(6, -10);
        assertEquals(-6, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f = Fraction.of(-6, -10);
        assertEquals(6, f.getNumerator());
        assertEquals(10, f.getDenominator());

        try {
            f = Fraction.of(1, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(2, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-3, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(4, Integer.MIN_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
        try {
            f = Fraction.of(1, Integer.MIN_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testFactory_int_int_int() {
        Fraction f = null;

        f = Fraction.of(0, 0, 2);
        assertEquals(0, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(2, 0, 2);
        assertEquals(4, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(0, 1, 2);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(1, 1, 2);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        try {
            f = Fraction.of(1, -6, -10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(1, -6, -10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(1, -6, -10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(-1, 6, 10);
        assertEquals(-16, f.getNumerator());
        assertEquals(10, f.getDenominator());

        try {
            f = Fraction.of(-1, -6, 10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-1, 6, -10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-1, -6, -10);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(0, 1, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(1, 2, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-1, -3, 0);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(Integer.MAX_VALUE, 1, 2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-Integer.MAX_VALUE, 1, 2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(-1, 0, Integer.MAX_VALUE);
        assertEquals(-Integer.MAX_VALUE, f.getNumerator());
        assertEquals(Integer.MAX_VALUE, f.getDenominator());

        try {
            f = Fraction.of(0, 4, Integer.MIN_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
        try {
            f = Fraction.of(1, 1, Integer.MAX_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
        try {
            f = Fraction.of(-1, 2, Integer.MAX_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testReducedFactory_int_int() {
        Fraction f = null;

        f = Fraction.of(0, 1, true);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(1, 1, true);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 1, true);
        assertEquals(2, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(22, 7, true);
        assertEquals(22, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f = Fraction.of(-6, 10, true);
        assertEquals(-3, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of(6, -10, true);
        assertEquals(-3, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of(-6, -10, true);
        assertEquals(3, f.getNumerator());
        assertEquals(5, f.getDenominator());

        try {
            f = Fraction.of(1, 0, true);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(2, 0, true);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-3, 0, true);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(0, 2, true);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 2, true);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 4, true);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(15, 10, true);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(121, 22, true);
        assertEquals(11, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(-2, Integer.MIN_VALUE, true);
        assertEquals(1, f.getNumerator());
        assertEquals(-(Integer.MIN_VALUE / 2), f.getDenominator());

        try {
            f = Fraction.of(-7, Integer.MIN_VALUE, true);
            fail("Expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(Integer.MIN_VALUE, 2, true);
        assertEquals(Integer.MIN_VALUE / 2, f.getNumerator());
        assertEquals(1, f.getDenominator());
    }

    @Test
    public void testFactory_double() {
        Fraction f = null;

        try {
            f = Fraction.of(Double.NaN);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(Double.POSITIVE_INFINITY);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(Double.NEGATIVE_INFINITY);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of((double) Integer.MAX_VALUE + 1);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(0.0d);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(1.0d);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(0.5d);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(-0.875d);
        assertEquals(-7, f.getNumerator());
        assertEquals(8, f.getDenominator());

        f = Fraction.of(1.25d);
        assertEquals(5, f.getNumerator());
        assertEquals(4, f.getDenominator());

        f = Fraction.of(0.66666d);
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(1.0d / 10001d);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        Fraction f2 = null;
        for (int i = 1; i <= 100; i++) {
            for (int j = 1; j <= i; j++) {
                try {
                    f = Fraction.of((double) j / (double) i);
                } catch (final ArithmeticException ex) {
                    System.err.println(j + " " + i);
                    throw ex;
                }
                f2 = Fraction.of(j, i, true);
                assertEquals(f2.getNumerator(), f.getNumerator());
                assertEquals(f2.getDenominator(), f.getDenominator());
            }
        }
        for (int i = 1001; i <= 10000; i += SKIP) {
            for (int j = 1; j <= i; j++) {
                try {
                    f = Fraction.of((double) j / (double) i);
                } catch (final ArithmeticException ex) {
                    System.err.println(j + " " + i);
                    throw ex;
                }
                f2 = Fraction.of(j, i, true);
                assertEquals(f2.getNumerator(), f.getNumerator());
                assertEquals(f2.getDenominator(), f.getDenominator());
            }
        }
    }

    @Test
    public void testFactory_String_double() {
        Fraction f = null;

        f = Fraction.of("0.0");
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of("0.2");
        assertEquals(1, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of("0.5");
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of("0.66666");
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        try {
            f = Fraction.of("2.3R");
            fail("Expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("2147483648");
            fail("Expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of(".");
            fail("Expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }
    }

    @Test
    public void testFactory_String_proper() {
        Fraction f = null;

        f = Fraction.of("0 0/1");
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of("1 1/5");
        assertEquals(6, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of("7 1/2");
        assertEquals(15, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of("1 2/4");
        assertEquals(6, f.getNumerator());
        assertEquals(4, f.getDenominator());

        f = Fraction.of("-7 1/2");
        assertEquals(-15, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of("-1 2/4");
        assertEquals(-6, f.getNumerator());
        assertEquals(4, f.getDenominator());

        try {
            f = Fraction.of("2 3");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("a 3");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("2 b/4");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("2 ");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of(" 3");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of(" ");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }
    }

    @Test
    public void testFactory_String_improper() {
        Fraction f = null;

        f = Fraction.of("0/1");
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of("1/5");
        assertEquals(1, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f = Fraction.of("1/2");
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of("2/3");
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of("7/3");
        assertEquals(7, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of("2/4");
        assertEquals(2, f.getNumerator());
        assertEquals(4, f.getDenominator());

        try {
            f = Fraction.of("2/d");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("2e/3");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("2/");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }

        try {
            f = Fraction.of("/");
            fail("expecting NumberFormatException");
        } catch (final NumberFormatException ex) {
        }
    }

    @Test
    public void testGets() {
        Fraction f = null;

        f = Fraction.of(3, 5, 6);
        assertEquals(23, f.getNumerator());
        assertEquals(3, f.getProperWhole());
        assertEquals(5, f.getProperNumerator());
        assertEquals(6, f.getDenominator());

        f = Fraction.of(-3, 5, 6);
        assertEquals(-23, f.getNumerator());
        assertEquals(-3, f.getProperWhole());
        assertEquals(5, f.getProperNumerator());
        assertEquals(6, f.getDenominator());

        f = Fraction.of(Integer.MIN_VALUE, 0, 1);
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(Integer.MIN_VALUE, f.getProperWhole());
        assertEquals(0, f.getProperNumerator());
        assertEquals(1, f.getDenominator());
    }

    @Test
    public void testConversions() {
        Fraction f = null;

        f = Fraction.of(3, 7, 8);
        assertEquals(3, f.intValue());
        assertEquals(3L, f.longValue());
        assertEquals(3.875f, f.floatValue(), 0.00001f);
        assertEquals(3.875d, f.doubleValue(), 0.00001d);
    }

    @Test
    public void testReduce() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        Fraction result = f.reduce();
        assertEquals(2, result.getNumerator());
        assertEquals(3, result.getDenominator());

        f = Fraction.of(-2, -3);
        result = f.reduce();
        assertEquals(2, result.getNumerator());
        assertEquals(3, result.getDenominator());

        f = Fraction.of(2, -3);
        result = f.reduce();
        assertEquals(-2, result.getNumerator());
        assertEquals(3, result.getDenominator());

        f = Fraction.of(-2, 3);
        result = f.reduce();
        assertEquals(-2, result.getNumerator());
        assertEquals(3, result.getDenominator());
        assertSame(f, result);

        f = Fraction.of(2, 3);
        result = f.reduce();
        assertEquals(2, result.getNumerator());
        assertEquals(3, result.getDenominator());
        assertSame(f, result);

        f = Fraction.of(0, 1);
        result = f.reduce();
        assertEquals(0, result.getNumerator());
        assertEquals(1, result.getDenominator());
        assertSame(f, result);

        f = Fraction.of(0, 100);
        result = f.reduce();
        assertEquals(0, result.getNumerator());
        assertEquals(1, result.getDenominator());
        assertSame(result, Fraction.ZERO);

        f = Fraction.of(Integer.MIN_VALUE, 2);
        result = f.reduce();
        assertEquals(Integer.MIN_VALUE / 2, result.getNumerator());
        assertEquals(1, result.getDenominator());
    }

    @Test
    public void testInvert() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        f = f.invert();
        assertEquals(75, f.getNumerator());
        assertEquals(50, f.getDenominator());

        f = Fraction.of(4, 3);
        f = f.invert();
        assertEquals(3, f.getNumerator());
        assertEquals(4, f.getDenominator());

        f = Fraction.of(-15, 47);
        f = f.invert();
        assertEquals(-47, f.getNumerator());
        assertEquals(15, f.getDenominator());

        f = Fraction.of(0, 3);
        try {
            f = f.invert();
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(Integer.MIN_VALUE, 1);
        try {
            f = f.invert();
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(Integer.MAX_VALUE, 1);
        f = f.invert();
        assertEquals(1, f.getNumerator());
        assertEquals(Integer.MAX_VALUE, f.getDenominator());
    }

    @Test
    public void testNegate() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        f = f.negate();
        assertEquals(-50, f.getNumerator());
        assertEquals(75, f.getDenominator());

        f = Fraction.of(-50, 75);
        f = f.negate();
        assertEquals(50, f.getNumerator());
        assertEquals(75, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        f = f.negate();
        assertEquals(Integer.MIN_VALUE + 2, f.getNumerator());
        assertEquals(Integer.MAX_VALUE, f.getDenominator());

        f = Fraction.of(Integer.MIN_VALUE, 1);
        try {
            f = f.negate();
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testAbs() {
        Fraction f = null;

        f = Fraction.of(50, 75);
        f = f.abs();
        assertEquals(50, f.getNumerator());
        assertEquals(75, f.getDenominator());

        f = Fraction.of(-50, 75);
        f = f.abs();
        assertEquals(50, f.getNumerator());
        assertEquals(75, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE, 1);
        f = f.abs();
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE, -1);
        f = f.abs();
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(Integer.MIN_VALUE, 1);
        try {
            f = f.abs();
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testPow() {
        Fraction f = null;

        f = Fraction.of(3, 5);
        assertEquals(Fraction.ONE, f.pow(0));

        f = Fraction.of(3, 5);
        assertSame(f, f.pow(1));
        assertEquals(f, f.pow(1));

        f = Fraction.of(3, 5);
        f = f.pow(2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(3);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(-1);
        assertEquals(5, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(3, 5);
        f = f.pow(-2);
        assertEquals(25, f.getNumerator());
        assertEquals(9, f.getDenominator());

        f = Fraction.of(6, 10);
        assertEquals(Fraction.ONE, f.pow(0));

        f = Fraction.of(6, 10);
        assertEquals(f, f.pow(1));
        assertFalse(f.pow(1).equals(Fraction.of(3, 5)));

        f = Fraction.of(6, 10);
        f = f.pow(2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(3);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(-1);
        assertEquals(10, f.getNumerator());
        assertEquals(6, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(-2);
        assertEquals(25, f.getNumerator());
        assertEquals(9, f.getDenominator());

        f = Fraction.of(0, 1231);
        f = f.pow(1);
        assertTrue(0 == f.compareTo(Fraction.ZERO));
        assertEquals(0, f.getNumerator());
        assertEquals(1231, f.getDenominator());
        f = f.pow(2);
        assertTrue(0 == f.compareTo(Fraction.ZERO));
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        try {
            f = f.pow(-1);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
        try {
            f = f.pow(Integer.MIN_VALUE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(1, 1);
        f = f.pow(0);
        assertEquals(f, Fraction.ONE);
        f = f.pow(1);
        assertEquals(f, Fraction.ONE);
        f = f.pow(-1);
        assertEquals(f, Fraction.ONE);
        f = f.pow(Integer.MAX_VALUE);
        assertEquals(f, Fraction.ONE);
        f = f.pow(Integer.MIN_VALUE);
        assertEquals(f, Fraction.ONE);

        f = Fraction.of(Integer.MAX_VALUE, 1);
        try {
            f = f.pow(2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(Integer.MIN_VALUE, 1);
        try {
            f = f.pow(3);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f = Fraction.of(65536, 1);
        try {
            f = f.pow(2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testAdd() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 5);
        f = f1.add(f2);
        assertEquals(4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.add(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(3, 5);
        f = f1.add(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.add(f2);
        assertEquals(-1, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE - 1, 1);
        f2 = Fraction.ONE;
        f = f1.add(f2);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 2);
        f = f1.add(f2);
        assertEquals(11, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f1 = Fraction.of(3, 8);
        f2 = Fraction.of(1, 6);
        f = f1.add(f2);
        assertEquals(13, f.getNumerator());
        assertEquals(24, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(1, 5);
        f = f1.add(f2);
        assertSame(f2, f);
        f = f2.add(f1);
        assertSame(f2, f);

        f1 = Fraction.of(-1, 13 * 13 * 2 * 2);
        f2 = Fraction.of(-2, 13 * 17 * 2);
        f = f1.add(f2);
        assertEquals(13 * 13 * 17 * 2 * 2, f.getDenominator());
        assertEquals(-17 - 2 * 13 * 2, f.getNumerator());

        try {
            f.add(null);
            fail("expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
        }

        f1 = Fraction.of(1, 32768 * 3);
        f2 = Fraction.of(1, 59049);
        f = f1.add(f2);
        assertEquals(52451, f.getNumerator());
        assertEquals(1934917632, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, 3);
        f2 = Fraction.ONE_THIRD;
        f = f1.add(f2);
        assertEquals(Integer.MIN_VALUE + 1, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE - 1, 1);
        f2 = Fraction.ONE;
        f = f1.add(f2);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        try {
            f = f.add(Fraction.ONE);
            fail("expecting ArithmeticException but got: " + f.toString());
        } catch (final ArithmeticException ex) {
        }

        f1 = Fraction.of(Integer.MIN_VALUE, 5);
        f2 = Fraction.of(-1, 5);
        try {
            f = f1.add(f2);
            fail("expecting ArithmeticException but got: " + f.toString());
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-Integer.MAX_VALUE, 1);
            f = f.add(f);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(-Integer.MAX_VALUE, 1);
            f = f.add(f);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f1 = Fraction.of(3, 327680);
        f2 = Fraction.of(2, 59049);
        try {
            f = f1.add(f2);
            fail("expecting ArithmeticException but got: " + f.toString());
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testSubtract() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 5);
        f = f1.subtract(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(7, 5);
        f2 = Fraction.of(2, 5);
        f = f1.subtract(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(3, 5);
        f = f1.subtract(f2);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.subtract(f2);
        assertEquals(7, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(4, 5);
        f = f1.subtract(f2);
        assertEquals(-4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(-4, 5);
        f = f1.subtract(f2);
        assertEquals(4, f.getNumerator());
        assertEquals(5, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(1, 2);
        f = f1.subtract(f2);
        assertEquals(1, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(1, 5);
        f = f2.subtract(f1);
        assertSame(f2, f);

        try {
            f.subtract(null);
            fail("expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
        }

        f1 = Fraction.of(1, 32768 * 3);
        f2 = Fraction.of(1, 59049);
        f = f1.subtract(f2);
        assertEquals(-13085, f.getNumerator());
        assertEquals(1934917632, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, 3);
        f2 = Fraction.ONE_THIRD.negate();
        f = f1.subtract(f2);
        assertEquals(Integer.MIN_VALUE + 1, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE, 1);
        f2 = Fraction.ONE;
        f = f1.subtract(f2);
        assertEquals(Integer.MAX_VALUE - 1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        try {
            f1 = Fraction.of(1, Integer.MAX_VALUE);
            f2 = Fraction.of(1, Integer.MAX_VALUE - 1);
            f = f1.subtract(f2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f1 = Fraction.of(Integer.MIN_VALUE, 5);
        f2 = Fraction.of(1, 5);
        try {
            f = f1.subtract(f2);
            fail("expecting ArithmeticException but got: " + f.toString());
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(Integer.MIN_VALUE, 1);
            f = f.subtract(Fraction.ONE);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f = Fraction.of(Integer.MAX_VALUE, 1);
            f = f.subtract(Fraction.ONE.negate());
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f1 = Fraction.of(3, 327680);
        f2 = Fraction.of(2, 59049);
        try {
            f = f1.subtract(f2);
            fail("expecting ArithmeticException but got: " + f.toString());
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testMultiply() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.multipliedBy(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(6, 10);
        f2 = Fraction.of(6, 10);
        f = f1.multipliedBy(f2);
        assertEquals(9, f.getNumerator());
        assertEquals(25, f.getDenominator());
        f = f.multipliedBy(f2);
        assertEquals(27, f.getNumerator());
        assertEquals(125, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(-2, 5);
        f = f1.multipliedBy(f2);
        assertEquals(-6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(-3, 5);
        f2 = Fraction.of(-2, 5);
        f = f1.multipliedBy(f2);
        assertEquals(6, f.getNumerator());
        assertEquals(25, f.getDenominator());

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(2, 7);
        f = f1.multipliedBy(f2);
        assertSame(Fraction.ZERO, f);

        f1 = Fraction.of(2, 7);
        f2 = Fraction.ONE;
        f = f1.multipliedBy(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f1 = Fraction.of(Integer.MAX_VALUE, 1);
        f2 = Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f = f1.multipliedBy(f2);
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        try {
            f.multipliedBy(null);
            fail("expecting IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
        }

        try {
            f1 = Fraction.of(1, Integer.MAX_VALUE);
            f = f1.multipliedBy(f1);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        try {
            f1 = Fraction.of(1, -Integer.MAX_VALUE);
            f = f1.multipliedBy(f1);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testDivide() {
        Fraction f = null;
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        f = f1.dividedBy(f2);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f1 = Fraction.of(3, 5);
        f2 = Fraction.ZERO;
        try {
            f = f1.dividedBy(f2);
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }

        f1 = Fraction.of(0, 5);
        f2 = Fraction.of(2, 7);
        f = f1.dividedBy(f2);
        assertSame(Fraction.ZERO, f);

        f1 = Fraction.of(2, 7);
        f2 = Fraction.ONE;
        f = f1.dividedBy(f2);
        assertEquals(2, f.getNumerator());
        assertEquals(7, f.getDenominator());

        f1 = Fraction.of(1, Integer.MAX_VALUE);
        f = f1.dividedBy(f1);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f1 = Fraction.of(Integer.MIN_VALUE, Integer.MAX_VALUE);
        f2 = Fraction.of(1, Integer.MAX_VALUE);
        f = f1.dividedBy(f2);
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        try {
            f.dividedBy(null);
            fail("IllegalArgumentException");
        } catch (final IllegalArgumentException ex) {
        }

        try {
            f1 = Fraction.of(1, Integer.MAX_VALUE);
            f = f1.dividedBy(f1.invert());
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
        try {
            f1 = Fraction.of(1, -Integer.MAX_VALUE);
            f = f1.dividedBy(f1.invert());
            fail("expecting ArithmeticException");
        } catch (final ArithmeticException ex) {
        }
    }

    @Test
    public void testEquals() {
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        assertFalse(f1.equals(null));
        assertFalse(f1.equals(new Object()));
        assertFalse(f1.equals(Integer.valueOf(6)));

        f1 = Fraction.of(3, 5);
        f2 = Fraction.of(2, 5);
        assertFalse(f1.equals(f2));
        assertTrue(f1.equals(f1));
        assertTrue(f2.equals(f2));

        f2 = Fraction.of(3, 5);
        assertTrue(f1.equals(f2));

        f2 = Fraction.of(6, 10);
        assertFalse(f1.equals(f2));
    }

    @Test
    public void testHashCode() {
        final Fraction f1 = Fraction.of(3, 5);
        Fraction f2 = Fraction.of(3, 5);

        assertTrue(f1.hashCode() == f2.hashCode());

        f2 = Fraction.of(2, 5);
        assertTrue(f1.hashCode() != f2.hashCode());

        f2 = Fraction.of(6, 10);
        assertTrue(f1.hashCode() != f2.hashCode());
    }

    @Test
    public void testCompareTo() {
        Fraction f1 = null;
        Fraction f2 = null;

        f1 = Fraction.of(3, 5);
        assertTrue(f1.compareTo(f1) == 0);

        try {
            f1.compareTo(null);
            fail("expecting NullPointerException");
        } catch (final NullPointerException ex) {
        }

        f2 = Fraction.of(2, 5);
        assertTrue(f1.compareTo(f2) > 0);
        assertTrue(f2.compareTo(f2) == 0);

        f2 = Fraction.of(4, 5);
        assertTrue(f1.compareTo(f2) < 0);
        assertTrue(f2.compareTo(f2) == 0);

        f2 = Fraction.of(3, 5);
        assertTrue(f1.compareTo(f2) == 0);
        assertTrue(f2.compareTo(f2) == 0);

        f2 = Fraction.of(6, 10);
        assertTrue(f1.compareTo(f2) == 0);
        assertTrue(f2.compareTo(f2) == 0);

        f2 = Fraction.of(-1, 1, Integer.MAX_VALUE);
        assertTrue(f1.compareTo(f2) > 0);
        assertTrue(f2.compareTo(f2) == 0);

    }

    @Test
    public void testToString() {
        Fraction f = null;

        f = Fraction.of(3, 5);
        final String str = f.toString();
        assertEquals("3/5", str);
        assertSame(str, f.toString());

        f = Fraction.of(7, 5);
        assertEquals("7/5", f.toString());

        f = Fraction.of(4, 2);
        assertEquals("4/2", f.toString());

        f = Fraction.of(0, 2);
        assertEquals("0/2", f.toString());

        f = Fraction.of(2, 2);
        assertEquals("2/2", f.toString());

        f = Fraction.of(Integer.MIN_VALUE, 0, 1);
        assertEquals("-2147483648/1", f.toString());

        f = Fraction.of(-1, 1, Integer.MAX_VALUE);
        assertEquals("-2147483648/2147483647", f.toString());
    }

    @Test
    public void testToProperString() {
        Fraction f = null;

        f = Fraction.of(3, 5);
        final String str = f.toProperString();
        assertEquals("3/5", str);
        assertSame(str, f.toProperString());

        f = Fraction.of(7, 5);
        assertEquals("1 2/5", f.toProperString());

        f = Fraction.of(14, 10);
        assertEquals("1 4/10", f.toProperString());

        f = Fraction.of(4, 2);
        assertEquals("2", f.toProperString());

        f = Fraction.of(0, 2);
        assertEquals("0", f.toProperString());

        f = Fraction.of(2, 2);
        assertEquals("1", f.toProperString());

        f = Fraction.of(-7, 5);
        assertEquals("-1 2/5", f.toProperString());

        f = Fraction.of(Integer.MIN_VALUE, 0, 1);
        assertEquals("-2147483648", f.toProperString());

        f = Fraction.of(-1, 1, Integer.MAX_VALUE);
        assertEquals("-1 1/2147483647", f.toProperString());

        assertEquals("-1", Fraction.of(-1).toProperString());
    }

    @Test
    public void test_of_twoArgs() {
        Fraction f = Fraction.of(3, 4);
        assertEquals(3, f.numerator());
        assertEquals(4, f.denominator());

        Fraction f2 = Fraction.of(-5, 8);
        assertEquals(-5, f2.numerator());
        assertEquals(8, f2.denominator());

        Fraction f3 = Fraction.of(0, 1);
        assertEquals(0, f3.numerator());
        assertEquals(1, f3.denominator());
    }

    @Test
    public void test_of_twoArgs_zeroDenominator() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
    }

    @Test
    public void test_of_threeArgs_withoutReduce() {
        Fraction f = Fraction.of(2, 4, false);
        assertEquals(2, f.numerator());
        assertEquals(4, f.denominator());
    }

    @Test
    public void test_of_threeArgs_withReduce() {
        Fraction f = Fraction.of(2, 4, true);
        assertEquals(1, f.numerator());
        assertEquals(2, f.denominator());

        Fraction f2 = Fraction.of(6, 8, true);
        assertEquals(3, f2.numerator());
        assertEquals(4, f2.denominator());
    }

    @Test
    public void test_of_threeArgs_negativeDenominator() {
        Fraction f = Fraction.of(3, -4, false);
        assertEquals(-3, f.numerator());
        assertEquals(4, f.denominator());

        Fraction f2 = Fraction.of(-3, -4, false);
        assertEquals(3, f2.numerator());
        assertEquals(4, f2.denominator());
    }

    @Test
    public void test_of_threeArgs_zeroNumerator() {
        Fraction f = Fraction.of(0, 5, true);
        assertSame(Fraction.ZERO, f);
    }

    @Test
    public void test_of_threeArgs_minValueDenominator() {
        Fraction f = Fraction.of(2, Integer.MIN_VALUE, true);
        assertEquals(-1, f.numerator());
        assertEquals(-(Integer.MIN_VALUE / 2), f.denominator());
        N.println(f);
        N.println(Fraction.of(3, -4, false));
    }

    @Test
    public void test_of_threeArgs_minValueBoth() {
        Fraction f = Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE, true);
        assertEquals(1, f.numerator());
        assertEquals(1, f.denominator());
    }

    @Test
    public void test_of_threeArgs_zeroDenominator() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0, true));
    }

    @Test
    public void test_of_mixedFraction() {
        Fraction f = Fraction.of(1, 3, 4);
        assertEquals(7, f.numerator());
        assertEquals(4, f.denominator());

        Fraction f2 = Fraction.of(2, 1, 3);
        assertEquals(7, f2.numerator());
        assertEquals(3, f2.denominator());
    }

    @Test
    public void test_of_mixedFraction_negative() {
        Fraction f = Fraction.of(-1, 1, 2);
        assertEquals(-3, f.numerator());
        assertEquals(2, f.denominator());
    }

    @Test
    public void test_of_mixedFraction_zeroDenominator() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 2, 0));
    }

    @Test
    public void test_of_mixedFraction_negativeDenominator() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 1, -2));
    }

    @Test
    public void test_of_mixedFraction_negativeNumerator() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, -1, 2));
    }

    @Test
    public void test_of_mixedFraction_withReduce() {
        Fraction f = Fraction.of(1, 2, 4, true);
        assertEquals(3, f.numerator());
        assertEquals(2, f.denominator());
    }

    @Test
    public void test_of_mixedFraction_overflow() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MAX_VALUE, 1, 2, false));
    }

    @Test
    public void test_of_double() {
        Fraction f = Fraction.of(0.5);
        assertEquals(1, f.numerator());
        assertEquals(2, f.denominator());

        Fraction f2 = Fraction.of(0.25);
        assertEquals(1, f2.numerator());
        assertEquals(4, f2.denominator());
    }

    @Test
    public void test_of_double_withWholeNumber() {
        Fraction f = Fraction.of(3.5);
        assertEquals(7, f.numerator());
        assertEquals(2, f.denominator());
    }

    @Test
    public void test_of_double_negative() {
        Fraction f = Fraction.of(-0.5);
        assertEquals(-1, f.numerator());
        assertEquals(2, f.denominator());
    }

    @Test
    public void test_of_double_tooLarge() {
        assertThrows(ArithmeticException.class, () -> Fraction.of((double) Integer.MAX_VALUE + 1));
    }

    @Test
    public void test_of_double_nan() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NaN));
    }

    @Test
    public void test_of_string_fraction() {
        Fraction f = Fraction.of("3/4");
        assertEquals(3, f.numerator());
        assertEquals(4, f.denominator());
    }

    @Test
    public void test_of_string_mixed() {
        Fraction f = Fraction.of("1 2/3");
        assertEquals(5, f.numerator());
        assertEquals(3, f.denominator());
    }

    @Test
    public void test_of_string_decimal() {
        Fraction f = Fraction.of("0.5");
        assertEquals(1, f.numerator());
        assertEquals(2, f.denominator());
    }

    @Test
    public void test_of_string_wholeNumber() {
        Fraction f = Fraction.of("5");
        assertEquals(5, f.numerator());
        assertEquals(1, f.denominator());
    }

    @Test
    public void test_of_string_negative() {
        Fraction f = Fraction.of("-3/4");
        assertEquals(-3, f.numerator());
        assertEquals(4, f.denominator());
    }

    @Test
    public void test_of_string_null() {
        assertThrows(IllegalArgumentException.class, () -> Fraction.of(null));
    }

    @Test
    public void test_of_string_invalid() {
        assertThrows(NumberFormatException.class, () -> Fraction.of("invalid"));
    }

    @Test
    public void test_of_string_mixedInvalid() {
        assertThrows(NumberFormatException.class, () -> Fraction.of("1 2"));
    }

    @Test
    public void test_numerator() {
        Fraction f = Fraction.of(7, 4);
        assertEquals(7, f.numerator());
        assertEquals(7, f.getNumerator());
    }

    @Test
    public void test_denominator() {
        Fraction f = Fraction.of(3, 8);
        assertEquals(8, f.denominator());
        assertEquals(8, f.getDenominator());
    }

    @Test
    public void test_properNumerator() {
        Fraction f1 = Fraction.of(7, 4);
        assertEquals(3, f1.properNumerator());
        assertEquals(3, f1.getProperNumerator());

        Fraction f2 = Fraction.of(-7, 4);
        assertEquals(3, f2.properNumerator());
        assertEquals(3, f2.getProperNumerator());

        Fraction f3 = Fraction.of(3, 4);
        assertEquals(3, f3.properNumerator());
    }

    @Test
    public void test_properWhole() {
        Fraction f1 = Fraction.of(7, 4);
        assertEquals(1, f1.properWhole());
        assertEquals(1, f1.getProperWhole());

        Fraction f2 = Fraction.of(-7, 4);
        assertEquals(-1, f2.properWhole());
        assertEquals(-1, f2.getProperWhole());

        Fraction f3 = Fraction.of(3, 4);
        assertEquals(0, f3.properWhole());
    }

    @Test
    public void test_intValue() {
        Fraction f1 = Fraction.of(7, 4);
        assertEquals(1, f1.intValue());

        Fraction f2 = Fraction.of(-10, 3);
        assertEquals(-3, f2.intValue());

        Fraction f3 = Fraction.of(3, 4);
        assertEquals(0, f3.intValue());
    }

    @Test
    public void test_longValue() {
        Fraction f1 = Fraction.of(7, 4);
        assertEquals(1L, f1.longValue());

        Fraction f2 = Fraction.of(-10, 3);
        assertEquals(-3L, f2.longValue());
    }

    @Test
    public void test_floatValue() {
        Fraction f1 = Fraction.of(1, 3);
        assertEquals(1.0f / 3.0f, f1.floatValue(), 0.0001f);

        Fraction f2 = Fraction.of(3, 4);
        assertEquals(0.75f, f2.floatValue(), 0.0001f);
    }

    @Test
    public void test_doubleValue() {
        Fraction f1 = Fraction.of(1, 3);
        assertEquals(1.0 / 3.0, f1.doubleValue(), 0.0000001);

        Fraction f2 = Fraction.of(22, 7);
        assertEquals(22.0 / 7.0, f2.doubleValue(), 0.0000001);
    }

    @Test
    public void test_reduce() {
        Fraction f1 = Fraction.of(6, 8);
        Fraction r1 = f1.reduce();
        assertEquals(3, r1.numerator());
        assertEquals(4, r1.denominator());

        Fraction f2 = Fraction.of(7, 13);
        Fraction r2 = f2.reduce();
        assertSame(f2, r2);

        Fraction f3 = Fraction.of(0, 5);
        Fraction r3 = f3.reduce();
        assertSame(Fraction.ZERO, r3);
    }

    @Test
    public void test_invert() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction i1 = f1.invert();
        assertEquals(4, i1.numerator());
        assertEquals(3, i1.denominator());

        Fraction f2 = Fraction.of(-2, 5);
        Fraction i2 = f2.invert();
        assertEquals(-5, i2.numerator());
        assertEquals(2, i2.denominator());
    }

    @Test
    public void test_invert_zero() {
        Fraction f = Fraction.of(0, 1);
        assertThrows(ArithmeticException.class, () -> f.invert());
    }

    @Test
    public void test_invert_minValue() {
        Fraction f = Fraction.of(Integer.MIN_VALUE, 1);
        assertThrows(ArithmeticException.class, () -> f.invert());
    }

    @Test
    public void test_negate() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction n1 = f1.negate();
        assertEquals(-3, n1.numerator());
        assertEquals(4, n1.denominator());

        Fraction f2 = Fraction.of(-2, 5);
        Fraction n2 = f2.negate();
        assertEquals(2, n2.numerator());
        assertEquals(5, n2.denominator());
    }

    @Test
    public void test_negate_minValue() {
        Fraction f = Fraction.of(Integer.MIN_VALUE, 1);
        assertThrows(ArithmeticException.class, () -> f.negate());
    }

    @Test
    public void test_abs() {
        Fraction f1 = Fraction.of(-3, 4);
        Fraction a1 = f1.abs();
        assertEquals(3, a1.numerator());
        assertEquals(4, a1.denominator());

        Fraction f2 = Fraction.of(2, 5);
        Fraction a2 = f2.abs();
        assertSame(f2, a2);

        Fraction f3 = Fraction.of(0, 1);
        Fraction a3 = f3.abs();
        assertSame(f3, a3);
    }

    @Test
    public void test_pow() {
        Fraction f = Fraction.of(2, 3);

        Fraction p0 = f.pow(0);
        assertSame(Fraction.ONE, p0);

        Fraction p1 = f.pow(1);
        assertSame(f, p1);

        Fraction p2 = f.pow(2);
        assertEquals(4, p2.numerator());
        assertEquals(9, p2.denominator());

        Fraction p3 = f.pow(3);
        assertEquals(8, p3.numerator());
        assertEquals(27, p3.denominator());
    }

    @Test
    public void test_pow_negative() {
        Fraction f = Fraction.of(2, 3);
        Fraction p = f.pow(-1);
        assertEquals(3, p.numerator());
        assertEquals(2, p.denominator());

        Fraction p2 = f.pow(-2);
        assertEquals(9, p2.numerator());
        assertEquals(4, p2.denominator());
    }

    @Test
    public void test_pow_zero_negativeExponent() {
        Fraction f = Fraction.of(0, 1);
        assertThrows(ArithmeticException.class, () -> f.pow(-1));
    }

    @Test
    public void test_pow_minValue() {
        Fraction f = Fraction.of(2, 3);
        assertThrows(ArithmeticException.class, () -> f.pow(Integer.MIN_VALUE));
    }

    @Test
    public void test_add() {
        Fraction f1 = Fraction.of(1, 2);
        Fraction f2 = Fraction.of(1, 3);
        Fraction sum = f1.add(f2);
        assertEquals(5, sum.numerator());
        assertEquals(6, sum.denominator());

        Fraction f3 = Fraction.of(1, 4);
        Fraction f4 = Fraction.of(1, 4);
        Fraction sum2 = f3.add(f4);
        assertEquals(1, sum2.numerator());
        assertEquals(2, sum2.denominator());
    }

    @Test
    public void test_add_zero() {
        Fraction f1 = Fraction.of(0, 1);
        Fraction f2 = Fraction.of(3, 4);
        Fraction sum = f1.add(f2);
        assertSame(f2, sum);

        Fraction sum2 = f2.add(f1);
        assertSame(f2, sum2);
    }

    @Test
    public void test_add_null() {
        Fraction f = Fraction.of(1, 2);
        assertThrows(IllegalArgumentException.class, () -> f.add(null));
    }

    @Test
    public void test_add_sameDenominator() {
        Fraction f1 = Fraction.of(1, 6);
        Fraction f2 = Fraction.of(2, 6);
        Fraction sum = f1.add(f2);
        assertEquals(1, sum.numerator());
        assertEquals(2, sum.denominator());
    }

    @Test
    public void test_subtract() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction f2 = Fraction.of(1, 2);
        Fraction diff = f1.subtract(f2);
        assertEquals(1, diff.numerator());
        assertEquals(4, diff.denominator());

        Fraction f3 = Fraction.of(1, 2);
        Fraction f4 = Fraction.of(1, 3);
        Fraction diff2 = f3.subtract(f4);
        assertEquals(1, diff2.numerator());
        assertEquals(6, diff2.denominator());
    }

    @Test
    public void test_subtract_zero() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction f2 = Fraction.of(0, 1);
        Fraction diff = f1.subtract(f2);
        assertSame(f1, diff);

        Fraction f3 = Fraction.of(0, 1);
        Fraction f4 = Fraction.of(3, 4);
        Fraction diff2 = f3.subtract(f4);
        assertEquals(-3, diff2.numerator());
        assertEquals(4, diff2.denominator());
    }

    @Test
    public void test_subtract_null() {
        Fraction f = Fraction.of(1, 2);
        assertThrows(IllegalArgumentException.class, () -> f.subtract(null));
    }

    @Test
    public void test_multipliedBy() {
        Fraction f1 = Fraction.of(2, 3);
        Fraction f2 = Fraction.of(3, 4);
        Fraction prod = f1.multipliedBy(f2);
        assertEquals(1, prod.numerator());
        assertEquals(2, prod.denominator());

        Fraction f3 = Fraction.of(5, 7);
        Fraction f4 = Fraction.of(7, 11);
        Fraction prod2 = f3.multipliedBy(f4);
        assertEquals(5, prod2.numerator());
        assertEquals(11, prod2.denominator());
    }

    @Test
    public void test_multipliedBy_zero() {
        Fraction f1 = Fraction.of(0, 1);
        Fraction f2 = Fraction.of(3, 4);
        Fraction prod = f1.multipliedBy(f2);
        assertSame(Fraction.ZERO, prod);

        Fraction prod2 = f2.multipliedBy(f1);
        assertSame(Fraction.ZERO, prod2);
    }

    @Test
    public void test_multipliedBy_null() {
        Fraction f = Fraction.of(1, 2);
        assertThrows(IllegalArgumentException.class, () -> f.multipliedBy(null));
    }

    @Test
    public void test_dividedBy() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction f2 = Fraction.of(1, 2);
        Fraction quot = f1.dividedBy(f2);
        assertEquals(3, quot.numerator());
        assertEquals(2, quot.denominator());

        Fraction f3 = Fraction.of(2, 3);
        Fraction f4 = Fraction.of(4, 5);
        Fraction quot2 = f3.dividedBy(f4);
        assertEquals(5, quot2.numerator());
        assertEquals(6, quot2.denominator());
    }

    @Test
    public void test_dividedBy_zero() {
        Fraction f1 = Fraction.of(3, 4);
        Fraction f2 = Fraction.of(0, 1);
        assertThrows(ArithmeticException.class, () -> f1.dividedBy(f2));
    }

    @Test
    public void test_dividedBy_null() {
        Fraction f = Fraction.of(1, 2);
        assertThrows(IllegalArgumentException.class, () -> f.dividedBy(null));
    }

    @Test
    public void test_compareTo() {
        Fraction f1 = Fraction.of(1, 2);
        Fraction f2 = Fraction.of(2, 4);
        Fraction f3 = Fraction.of(3, 4);

        assertEquals(0, f1.compareTo(f2));
        assertTrue(f1.compareTo(f3) < 0);
        assertTrue(f3.compareTo(f1) > 0);

        Fraction f4 = Fraction.of(1, 2);
        assertEquals(0, f1.compareTo(f4));
    }

    @Test
    public void test_compareTo_negative() {
        Fraction f1 = Fraction.of(-1, 2);
        Fraction f2 = Fraction.of(1, 2);
        assertTrue(f1.compareTo(f2) < 0);
        assertTrue(f2.compareTo(f1) > 0);
    }

    @Test
    public void test_equals() {
        Fraction f1 = Fraction.of(1, 2);
        Fraction f2 = Fraction.of(1, 2);
        Fraction f3 = Fraction.of(2, 4);

        assertTrue(f1.equals(f2));
        assertFalse(f1.equals(f3));
        assertTrue(f1.equals(f1));
        assertFalse(f1.equals(null));
        assertFalse(f1.equals("not a fraction"));
    }

    @Test
    public void test_hashCode() {
        Fraction f1 = Fraction.of(1, 2);
        Fraction f2 = Fraction.of(1, 2);
        Fraction f3 = Fraction.of(2, 4);

        assertEquals(f1.hashCode(), f2.hashCode());
        assertNotEquals(f1.hashCode(), f3.hashCode());
    }

    @Test
    public void test_hashCode_cached() {
        Fraction f = Fraction.of(1, 2);
        int hash1 = f.hashCode();
        int hash2 = f.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_toString() {
        Fraction f1 = Fraction.of(3, 4);
        assertEquals("3/4", f1.toString());

        Fraction f2 = Fraction.of(8, 4);
        assertEquals("8/4", f2.toString());

        Fraction f3 = Fraction.of(-1, 2);
        assertEquals("-1/2", f3.toString());
    }

    @Test
    public void test_toString_cached() {
        Fraction f = Fraction.of(1, 2);
        String str1 = f.toString();
        String str2 = f.toString();
        assertEquals(str1, str2);
    }

    @Test
    public void test_toProperString() {
        Fraction f1 = Fraction.of(0, 1);
        assertEquals("0", f1.toProperString());

        Fraction f2 = Fraction.of(3, 4);
        assertEquals("3/4", f2.toProperString());

        Fraction f3 = Fraction.of(7, 4);
        assertEquals("1 3/4", f3.toProperString());

        Fraction f4 = Fraction.of(8, 4);
        assertEquals("2", f4.toProperString());

        Fraction f5 = Fraction.of(-7, 4);
        assertEquals("-1 3/4", f5.toProperString());

        Fraction f6 = Fraction.of(4, 4);
        assertEquals("1", f6.toProperString());

        Fraction f7 = Fraction.of(-4, 4);
        assertEquals("-1", f7.toProperString());
    }

    @Test
    public void test_toProperString_cached() {
        Fraction f = Fraction.of(7, 4);
        String str1 = f.toProperString();
        String str2 = f.toProperString();
        assertEquals(str1, str2);
    }

    @Test
    public void test_constants() {
        assertEquals(0, Fraction.ZERO.numerator());
        assertEquals(1, Fraction.ZERO.denominator());

        assertEquals(1, Fraction.ONE.numerator());
        assertEquals(1, Fraction.ONE.denominator());

        assertEquals(1, Fraction.ONE_HALF.numerator());
        assertEquals(2, Fraction.ONE_HALF.denominator());

        assertEquals(1, Fraction.ONE_THIRD.numerator());
        assertEquals(3, Fraction.ONE_THIRD.denominator());

        assertEquals(2, Fraction.TWO_THIRDS.numerator());
        assertEquals(3, Fraction.TWO_THIRDS.denominator());

        assertEquals(1, Fraction.ONE_QUARTER.numerator());
        assertEquals(4, Fraction.ONE_QUARTER.denominator());

        assertEquals(2, Fraction.TWO_QUARTERS.numerator());
        assertEquals(4, Fraction.TWO_QUARTERS.denominator());

        assertEquals(3, Fraction.THREE_QUARTERS.numerator());
        assertEquals(4, Fraction.THREE_QUARTERS.denominator());

        assertEquals(1, Fraction.ONE_FIFTH.numerator());
        assertEquals(5, Fraction.ONE_FIFTH.denominator());

        assertEquals(2, Fraction.TWO_FIFTHS.numerator());
        assertEquals(5, Fraction.TWO_FIFTHS.denominator());

        assertEquals(3, Fraction.THREE_FIFTHS.numerator());
        assertEquals(5, Fraction.THREE_FIFTHS.denominator());

        assertEquals(4, Fraction.FOUR_FIFTHS.numerator());
        assertEquals(5, Fraction.FOUR_FIFTHS.denominator());
    }

    @Test
    public void test_edgeCases_largeValues() {
        Fraction f = Fraction.of(Integer.MAX_VALUE, 1);
        assertEquals(Integer.MAX_VALUE, f.numerator());
        assertEquals(1, f.denominator());
    }

    @Test
    public void test_edgeCases_reduction() {
        Fraction f = Fraction.of(Integer.MAX_VALUE / 2, Integer.MAX_VALUE, true);
        assertEquals(Integer.MAX_VALUE / 2, f.numerator());
        assertEquals(Integer.MAX_VALUE, f.denominator());
    }

    @Test
    public void test_addSub_CommonFactorBranchAndReducedResult() {
        Fraction left = Fraction.of(1, 6);
        Fraction right = Fraction.of(1, 15);

        Fraction sum = left.add(right);
        Fraction diff = left.subtract(right);

        assertEquals(Fraction.of(7, 30), sum);
        assertEquals(Fraction.of(1, 10), diff);
    }

}
