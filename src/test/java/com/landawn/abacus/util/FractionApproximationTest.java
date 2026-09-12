package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FractionApproximationTest extends TestBase {
    @Test
    public void boundedApproximationMatchesExhaustiveExactDecimalOracle() {
        for (double value : new double[] { 1000000000.25, -1000000000.25, 1000000000.3333334, -1000000000.3333334, 100000.99991, 2000000.99991, 0.00005,
                Math.nextDown(0.00005), Math.nextUp(0.00005), -0.00005, Math.PI, Double.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 300000.99991,
                -300000.99991, 474798.4647483182, 2147483646.5, -2147483646.5, 1073741824.5 }) {
            assertEquals(oracle(value), Fraction.of(value), "value=" + value);
        }
    }

    @Test
    public void binaryMidpointsRangeAndDecimalEntryPoint() {
        assertEquals(Fraction.of(1, 10000), Fraction.of(0.00005));
        assertEquals(Fraction.of(0, 1), Fraction.of(Math.nextDown(0.00005)));
        assertEquals(Fraction.of(1000000000, 1), Fraction.of(1000000000.25));
        assertEquals(Fraction.of(-1000000000, 1), Fraction.of(-1000000000.25));
        assertEquals(Fraction.of(0, 1), Fraction.of(-0.0));
        assertEquals(Fraction.of(0.00005), Fraction.of("0.00005"));
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Math.nextUp((double) Integer.MAX_VALUE),
                Math.nextDown((double) Integer.MIN_VALUE) }) {
            assertThrows(ArithmeticException.class, () -> Fraction.of(invalid));
        }
    }

    // Enumerate every denominator independently of continued fractions. BigDecimal(double) preserves
    // the exact binary input; integer numerators on either side suffice for each fixed denominator.
    private static Fraction oracle(double value) {
        BigDecimal exact = new BigDecimal(value);
        BigDecimal bestError = null;
        int bestP = 0;
        int bestQ = 1;
        for (int q = 1; q <= 10000; q++) {
            BigDecimal scaled = exact.multiply(BigDecimal.valueOf(q));
            long floor = scaled.setScale(0, RoundingMode.FLOOR).longValueExact();
            for (long candidate : new long[] { floor, floor + 1 }) {
                int p = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, candidate));
                BigDecimal error = scaled.subtract(BigDecimal.valueOf(p)).abs();
                int comparison = bestError == null ? -1 : error.multiply(BigDecimal.valueOf(bestQ)).compareTo(bestError.multiply(BigDecimal.valueOf(q)));
                if (comparison < 0 || (comparison == 0 && (q < bestQ || (q == bestQ && Math.abs((long) p) < Math.abs((long) bestP))))) {
                    bestError = error;
                    bestP = p;
                    bestQ = q;
                }
            }
        }
        return Fraction.of(bestP, bestQ, true);
    }
}
