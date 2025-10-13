package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Fraction2025Test extends TestBase {

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
}
