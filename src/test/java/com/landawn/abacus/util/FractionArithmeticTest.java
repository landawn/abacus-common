package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FractionArithmeticTest extends TestBase {
    @Test
    public void reducesBeforeCheckingFinalRange() {
        assertEquals(Fraction.ONE, Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE, true));
        assertEquals(Fraction.ZERO, Fraction.of(0, Integer.MIN_VALUE, true));
        assertEquals(Fraction.of(1073741824, 1), Fraction.of(Integer.MIN_VALUE, -2, true));
        assertEquals(Fraction.of(Integer.MAX_VALUE, 1), Fraction.ofMixed(Integer.MAX_VALUE, 0, 2, true));
        assertEquals(Fraction.of(Integer.MIN_VALUE, 1), Fraction.ofMixed(Integer.MIN_VALUE, 0, 2, true));
        assertThrows(ArithmeticException.class, () -> Fraction.ofMixed(Integer.MAX_VALUE, 0, 2, false));
        assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MIN_VALUE, -2, false));
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, Integer.MIN_VALUE, true));
        assertThrows(ArithmeticException.class, () -> Fraction.of(0, 0, true));

        Fraction minimum = Fraction.of(Integer.MIN_VALUE, 2);
        assertEquals(Fraction.of(1073741824, 1), minimum.negate());
        assertEquals(Fraction.of(1073741824, 1), minimum.abs());
        assertEquals(Fraction.of(-1, 1073741824), minimum.invert());
        Fraction wholeMinimum = Fraction.of(Integer.MIN_VALUE, 1);
        assertEquals(Fraction.ONE, wholeMinimum.dividedBy(wholeMinimum));
        assertEquals(Fraction.of(Integer.MAX_VALUE, 6), Fraction.of(Integer.MAX_VALUE, 2).add(Fraction.of(-Integer.MAX_VALUE, 3)));
        assertEquals(Fraction.ONE, Fraction.of(2, 4).multipliedBy(Fraction.of(4, 2)));
        assertEquals(Fraction.ONE, Fraction.of(Integer.MAX_VALUE, 1).add(Fraction.of(-2147483646, 1)));
        assertThrows(ArithmeticException.class, () -> wholeMinimum.subtract(Fraction.ONE));
    }

    @Test
    public void arithmeticCanonicalizesIdentityOperationsWithoutChangingFactories() {
        Fraction f = Fraction.of(2, 4);
        Fraction half = Fraction.of(1, 2);
        assertEquals(4, f.denominator());
        assertEquals(4, Fraction.of("2/4").denominator());
        assertEquals(half, f.add(Fraction.ZERO));
        assertEquals(half, Fraction.ZERO.add(f));
        assertEquals(half, f.subtract(Fraction.ZERO));
        assertEquals(half, f.multipliedBy(Fraction.ONE));
        assertEquals(half, f.dividedBy(Fraction.ONE));
        assertEquals(half, f.abs());
        assertEquals(half, f.negate().negate());
        assertEquals(half, f.invert().invert());
        assertEquals(half, f.pow(1));
        Fraction zero = Fraction.of(0, 17);
        assertEquals(Fraction.ZERO, zero.abs());
        assertEquals(Fraction.ZERO, zero.negate());
        assertEquals(Fraction.ZERO, zero.pow(1));
        assertThrows(ArithmeticException.class, zero::invert);
        assertThrows(ArithmeticException.class, () -> zero.dividedBy(zero));
        assertThrows(IllegalArgumentException.class, () -> f.add(null));
        assertThrows(IllegalArgumentException.class, () -> f.subtract(null));
        assertThrows(IllegalArgumentException.class, () -> f.multipliedBy(null));
        assertThrows(IllegalArgumentException.class, () -> f.dividedBy(null));
    }

    @Test
    public void binaryArithmeticMatchesIndependentBigIntegerOracle() {
        Random random = new Random(2);
        for (int i = 0; i < 4000; i++) {
            int an = i < 2000 ? random.nextInt(20001) - 10000 : random.nextInt();
            int ad = i < 2000 ? random.nextInt(10000) + 1 : random.nextInt(Integer.MAX_VALUE) + 1;
            int bn = i < 2000 ? random.nextInt(20001) - 10000 : random.nextInt();
            int bd = i < 2000 ? random.nextInt(10000) + 1 : random.nextInt(Integer.MAX_VALUE) + 1;
            Fraction a = Fraction.of(an, ad);
            Fraction b = Fraction.of(bn, bd);
            BigInteger na = BigInteger.valueOf(an), da = BigInteger.valueOf(ad);
            BigInteger nb = BigInteger.valueOf(bn), db = BigInteger.valueOf(bd);
            verify(() -> a.add(b), na.multiply(db).add(nb.multiply(da)), da.multiply(db));
            verify(() -> a.subtract(b), na.multiply(db).subtract(nb.multiply(da)), da.multiply(db));
            verify(() -> a.multipliedBy(b), na.multiply(nb), da.multiply(db));
            verify(() -> a.dividedBy(b), na.multiply(db), da.multiply(nb));
        }
    }

    @Test
    public void powersMatchExactOracleIncludingExtremeExponents() {
        assertEquals(Fraction.of(Integer.MIN_VALUE, 1), Fraction.of(-2, 1).pow(31));
        assertEquals(Fraction.ONE, Fraction.of(-2, 2).pow(Integer.MIN_VALUE));
        assertEquals(Fraction.ONE, Fraction.of(2, 2).pow(Integer.MAX_VALUE));
        assertEquals(Fraction.ZERO, Fraction.of(0, 7).pow(Integer.MAX_VALUE));
        assertEquals(Fraction.ONE, Fraction.ZERO.pow(0));
        assertThrows(ArithmeticException.class, () -> Fraction.ZERO.pow(Integer.MIN_VALUE));
        for (int n = -9; n <= 9; n++) {
            for (int d = 1; d <= 9; d++) {
                for (int power = -8; power <= 8; power++) {
                    Fraction a = Fraction.of(n, d);
                    BigInteger numerator = BigInteger.valueOf(n).pow(Math.abs(power));
                    BigInteger denominator = BigInteger.valueOf(d).pow(Math.abs(power));
                    int exponent = power;
                    verify(() -> a.pow(exponent), power < 0 ? denominator : numerator, power < 0 ? numerator : denominator);
                }
            }
        }
    }

    // pow(1) returns reduce(), so "equals itself" holds by identity only when the receiver is already
    // reduced; for unreduced terms the result is equal in value but not under the term-based equals.
    @Test
    public void powOfOneReducesSoAnUnreducedReceiverIsNotEqualToItsOwnFirstPower() {
        Fraction unreduced = Fraction.of(2, 4);
        Fraction result = unreduced.pow(1);
        assertEquals(Fraction.of(1, 2), result);
        assertNotEquals(unreduced, result);
        assertEquals(0, result.compareTo(unreduced));

        Fraction reduced = Fraction.of(3, 5);
        assertSame(reduced, reduced.pow(1));
    }

    @Test
    public void negateOfZeroReturnsTheSharedZeroConstant() {
        assertSame(Fraction.ZERO, Fraction.of(0, 5).negate());
        assertSame(Fraction.ZERO, Fraction.of(0, Integer.MAX_VALUE).negate());
        assertSame(Fraction.ZERO, Fraction.ZERO.negate());
        assertEquals(Fraction.of(-1, 2), Fraction.of(2, 4).negate());
    }

    // The denominator is always positive, so numerator / denominator always fits in an int: longValue()
    // can never widen its way to a value intValue() cannot reach.
    @Test
    public void intValueAndLongValueNeverDiffer() {
        int[] terms = { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1073741824, -3, -1, 0, 1, 3, 1073741824, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };
        for (int n : terms) {
            for (int d : terms) {
                if (d == 0) {
                    continue;
                }
                Fraction f;
                try {
                    f = Fraction.of(n, d);
                } catch (ArithmeticException outOfIntRange) {
                    continue;
                }
                assertEquals((long) f.intValue(), f.longValue(), "terms " + f);
            }
        }

        Random random = new Random(11);
        for (int i = 0; i < 20000; i++) {
            int d = random.nextInt();
            if (d == 0) {
                continue;
            }
            Fraction f;
            try {
                f = Fraction.of(random.nextInt(), d);
            } catch (ArithmeticException outOfIntRange) {
                continue;
            }
            assertEquals((long) f.intValue(), f.longValue(), "terms " + f);
        }
    }

    private static void verify(Supplier<Fraction> operation, BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) {
            assertThrows(ArithmeticException.class, operation::get);
            return;
        }
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        boolean fits = numerator.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && numerator.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                && denominator.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
        if (!fits) {
            assertThrows(ArithmeticException.class, operation::get);
        } else {
            Fraction result = assertDoesNotThrow(operation::get);
            assertEquals(numerator.intValueExact(), result.numerator());
            assertEquals(denominator.intValueExact(), result.denominator());
        }
    }
}
