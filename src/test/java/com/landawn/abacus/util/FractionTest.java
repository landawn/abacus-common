package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FractionTest extends TestBase {

    private static final int SKIP = 500;

    @Test
    public void testGets() {
        Fraction f = null;

        f = Fraction.of(3, 5, 6);
        assertEquals(23, f.getNumerator());
        assertEquals(23, f.numerator());
        assertEquals(3, f.getProperWhole());
        assertEquals(5, f.getProperNumerator());
        assertEquals(6, f.getDenominator());
        assertEquals(6, f.denominator());

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

        Fraction proper = Fraction.of(3, 4);
        assertEquals(0, proper.intValue());
        assertEquals(0.75f, proper.floatValue(), 0.0001f);
        assertEquals(22.0 / 7.0, Fraction.of(22, 7).doubleValue(), 0.0000001);
    }

    @Test
    public void testPredicates() {
        Fraction unreducedZero = Fraction.of(0, 5);
        assertTrue(unreducedZero.isZero());
        assertFalse(unreducedZero.isPositive());
        assertFalse(unreducedZero.isNegative());
        assertTrue(unreducedZero.isInteger());

        Fraction positive = Fraction.of(3, 4);
        assertFalse(positive.isZero());
        assertTrue(positive.isPositive());
        assertFalse(positive.isNegative());
        assertFalse(positive.isInteger());

        Fraction negative = Fraction.of(-3, 4);
        assertFalse(negative.isZero());
        assertFalse(negative.isPositive());
        assertTrue(negative.isNegative());
        assertFalse(negative.isInteger());

        Fraction unreducedInteger = Fraction.of(6, 3);
        assertFalse(unreducedInteger.isZero());
        assertTrue(unreducedInteger.isPositive());
        assertFalse(unreducedInteger.isNegative());
        assertTrue(unreducedInteger.isInteger());
    }

    @Test
    public void test_of_threeArgs_withoutReduce() {
        Fraction f = Fraction.of(2, 4, false);
        assertEquals(2, f.numerator());
        assertEquals(4, f.denominator());
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
    }

    @Test
    public void test_of_threeArgs_minValueBoth() {
        Fraction f = Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE, true);
        assertEquals(1, f.numerator());
        assertEquals(1, f.denominator());
    }

    @Test
    public void test_of_mixedFraction_withReduce() {
        Fraction f = Fraction.of(1, 2, 4, true);
        assertEquals(3, f.numerator());
        assertEquals(2, f.denominator());
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
    public void testFactory_int_int() {
        Fraction f = null;

        f = Fraction.of(0, 1);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(3, 4);
        assertEquals(3, f.getNumerator());
        assertEquals(4, f.getDenominator());

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

        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(2, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-3, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(4, Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, Integer.MIN_VALUE));
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

        assertThrows(ArithmeticException.class, () -> Fraction.of(1, -6, -10));

        f = Fraction.of(-1, 6, 10);
        assertEquals(-16, f.getNumerator());
        assertEquals(10, f.getDenominator());

        f = Fraction.of(1, 3, 4);
        assertEquals(7, f.getNumerator());
        assertEquals(4, f.getDenominator());
        f = Fraction.of(-1, 1, 2);
        assertEquals(-3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -6, 10));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-1, 6, -10));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -6, -10));

        assertThrows(ArithmeticException.class, () -> Fraction.of(0, 1, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 2, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-1, -3, 0));

        assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MAX_VALUE, 1, 2));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-Integer.MAX_VALUE, 1, 2));

        f = Fraction.of(-1, 0, Integer.MAX_VALUE);
        assertEquals(-Integer.MAX_VALUE, f.getNumerator());
        assertEquals(Integer.MAX_VALUE, f.getDenominator());

        assertThrows(ArithmeticException.class, () -> Fraction.of(0, 4, Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 1, Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> Fraction.of(-1, 2, Integer.MAX_VALUE));
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

        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0, true));

        assertThrows(ArithmeticException.class, () -> Fraction.of(2, 0, true));

        assertThrows(ArithmeticException.class, () -> Fraction.of(-3, 0, true));

        f = Fraction.of(0, 2, true);
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 2, true);
        assertEquals(1, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(2, 4, true);
        assertEquals(1, f.getNumerator());
        assertEquals(2, f.getDenominator());
        assertSame(Fraction.ZERO, Fraction.of(0, 5, true));

        f = Fraction.of(15, 10, true);
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(121, 22, true);
        assertEquals(11, f.getNumerator());
        assertEquals(2, f.getDenominator());

        f = Fraction.of(-2, Integer.MIN_VALUE, true);
        assertEquals(1, f.getNumerator());
        assertEquals(-(Integer.MIN_VALUE / 2), f.getDenominator());

        assertThrows(ArithmeticException.class, () -> Fraction.of(-7, Integer.MIN_VALUE, true));

        f = Fraction.of(Integer.MIN_VALUE, 2, true);
        assertEquals(Integer.MIN_VALUE / 2, f.getNumerator());
        assertEquals(1, f.getDenominator());
    }

    @Test
    public void testFactory_double() {
        Fraction f = null;

        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NaN));

        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.POSITIVE_INFINITY));

        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NEGATIVE_INFINITY));

        assertThrows(ArithmeticException.class, () -> Fraction.of((double) Integer.MAX_VALUE + 1));

        assertThrows(ArithmeticException.class, () -> Fraction.of(Math.nextDown((double) Integer.MIN_VALUE)));

        f = Fraction.of(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

        f = Fraction.of(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(1, f.getDenominator());

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

        // 1/10001 is not representable within the denominator bound of 10000, but 1/10000 is, and it
        // is four orders of magnitude closer than 0/1 (error 1e-8 vs 1e-4). This assertion used to
        // expect 0/1: the continued-fraction loop returned the last convergent that fit the bound and
        // never considered the boundary semiconvergent, which is the closer of the two. Do not revert.
        f = Fraction.of(1.0d / 10001d);
        assertEquals(1, f.getNumerator());
        assertEquals(10000, f.getDenominator());

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
    public void testFactory_double_numeratorOverflow() {
        // An exact numerator outside int now selects the nearest admissible fraction. Equally
        // close integer candidates choose the numerator toward zero, never a wrapped numerator.
        assertEquals(Fraction.of(2147483646, 1), Fraction.of(2147483646.5d));
        assertEquals(Fraction.of(-2147483646, 1), Fraction.of(-2147483646.5d));
        assertEquals(Fraction.of(1073741824, 1), Fraction.of(1073741824.5d));

        // Boundary just below overflow must still succeed: 1073741823.5 == 2147483647/2,
        // numerator exactly Integer.MAX_VALUE.
        final Fraction f = Fraction.of(1073741823.5d);
        assertEquals(Integer.MAX_VALUE, f.getNumerator());
        assertEquals(2, f.getDenominator());
        assertEquals(-Integer.MAX_VALUE, Fraction.of(-1073741823.5d).getNumerator());
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

        assertThrows(NumberFormatException.class, () -> Fraction.of("2.3R"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("2147483648"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("."));
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

        assertThrows(NumberFormatException.class, () -> Fraction.of("2 3"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("a 3"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("2 b/4"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("2 "));

        assertThrows(NumberFormatException.class, () -> Fraction.of(" 3"));

        assertThrows(NumberFormatException.class, () -> Fraction.of(" "));
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

        assertThrows(NumberFormatException.class, () -> Fraction.of("2/d"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("2e/3"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("2/"));

        assertThrows(NumberFormatException.class, () -> Fraction.of("/"));
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

        final Fraction dividend = Fraction.of(3, 5);
        final Fraction zero = Fraction.ZERO;
        assertThrows(ArithmeticException.class, () -> dividend.dividedBy(zero));

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

        f = Fraction.of(7, 13);
        assertSame(f, f.reduce());

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

        f = Fraction.of(6, 8);
        result = f.reduce();
        assertEquals(3, result.getNumerator());
        assertEquals(4, result.getDenominator());

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
        assertEquals(3, f.getNumerator());
        assertEquals(2, f.getDenominator());

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
        assertEquals(-2, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(-50, 75);
        f = f.negate();
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

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
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());
        Fraction alreadyPositive = Fraction.of(2, 5);
        assertSame(alreadyPositive, alreadyPositive.abs());
        Fraction zero = Fraction.of(0, 1);
        assertSame(zero, zero.abs());

        f = Fraction.of(-50, 75);
        f = f.abs();
        assertEquals(2, f.getNumerator());
        assertEquals(3, f.getDenominator());

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
        assertEquals(f.reduce(), f.pow(1));
        assertEquals(Fraction.of(3, 5), f.pow(1));

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
        assertEquals(5, f.getNumerator());
        assertEquals(3, f.getDenominator());

        f = Fraction.of(6, 10);
        f = f.pow(-2);
        assertEquals(25, f.getNumerator());
        assertEquals(9, f.getDenominator());

        f = Fraction.of(0, 1231);
        f = f.pow(1);
        assertTrue(0 == f.compareTo(Fraction.ZERO));
        assertEquals(0, f.getNumerator());
        assertEquals(1, f.getDenominator());
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
    public void test_pow_minValue() {
        Fraction f = Fraction.of(2, 3);
        assertThrows(ArithmeticException.class, () -> f.pow(Integer.MIN_VALUE));
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
    public void test_addSub_CommonFactorBranchAndReducedResult() {
        Fraction left = Fraction.of(1, 6);
        Fraction right = Fraction.of(1, 15);

        Fraction sum = left.add(right);
        Fraction diff = left.subtract(right);

        assertEquals(Fraction.of(7, 30), sum);
        assertEquals(Fraction.of(1, 10), diff);
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
    public void test_compareTo_negative() {
        Fraction f1 = Fraction.of(-1, 2);
        Fraction f2 = Fraction.of(1, 2);
        assertTrue(f1.compareTo(f2) < 0);
        assertTrue(f2.compareTo(f1) > 0);
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
    public void test_toProperString_cached() {
        Fraction f = Fraction.of(7, 4);
        String str1 = f.toProperString();
        String str2 = f.toProperString();
        assertEquals(str1, str2);
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
    public void test_hashCode_cached() {
        Fraction f = Fraction.of(1, 2);
        int hash1 = f.hashCode();
        int hash2 = f.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_toString_cached() {
        Fraction f = Fraction.of(1, 2);
        String str1 = f.toString();
        String str2 = f.toString();
        assertEquals(str1, str2);
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
    public void testConstants() {
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
}
