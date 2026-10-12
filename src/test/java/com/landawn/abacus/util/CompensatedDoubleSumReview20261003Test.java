package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class CompensatedDoubleSumReview20261003Test extends TestBase {
    @ParameterizedTest
    @MethodSource("compensatedPrefixes")
    void finiteOverflowPreservesTheCompensatedPrefix(final double extreme, final double residual) {
        final double[] values = { extreme, residual, extreme, -extreme, -extreme };
        BigDecimal exact = BigDecimal.ZERO;

        for (final double value : values) {
            // Preserve the actual binary64 operands rather than their shortest decimal spellings.
            exact = exact.add(new BigDecimal(value));
        }

        assertEquals(residual, exact.doubleValue());
        assertEquals(residual / values.length, N.average(values));
        assertRoutes(values, exact.doubleValue());
    }

    static Stream<Arguments> compensatedPrefixes() {
        return Stream.of(Double.MAX_VALUE, -Double.MAX_VALUE)
                .flatMap(extreme -> Stream.of(1d, -1d, Math.ulp(Double.MAX_VALUE) / 4, -Math.ulp(Double.MAX_VALUE) / 4, 1e292, -1e292)
                        .map(residual -> Arguments.of(extreme, residual)));
    }

    @ParameterizedTest
    @MethodSource("compatibilityCases")
    void preservesOrdinaryEmptyAndNonfiniteBehavior(final double[] values, final double expected) {
        assertRoutes(values, expected);
    }

    static Stream<Arguments> compatibilityCases() {
        return Stream.of(Arguments.of(new double[0], 0d), Arguments.of(new double[] { 1, 2, 3 }, 6d),
                Arguments.of(new double[] { Double.MAX_VALUE, Double.MAX_VALUE }, Double.POSITIVE_INFINITY),
                Arguments.of(new double[] { -Double.MAX_VALUE, -Double.MAX_VALUE }, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[] { Double.MAX_VALUE, 1, Double.MAX_VALUE, Double.POSITIVE_INFINITY, -Double.MAX_VALUE }, Double.POSITIVE_INFINITY),
                Arguments.of(new double[] { -Double.MAX_VALUE, -1, -Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.MAX_VALUE }, Double.NEGATIVE_INFINITY),
                Arguments.of(new double[] { Double.MAX_VALUE, 1, Double.MAX_VALUE, Double.NaN }, Double.NaN),
                Arguments.of(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, Double.NaN));
    }

    private static void assertRoutes(final double[] values, final double expected) {
        final Double[] boxed = Arrays.stream(values).boxed().toArray(Double[]::new);
        final List<Double> list = Arrays.asList(boxed);
        final double[] padded = new double[values.length + 2];
        Arrays.fill(padded, Double.NaN);
        System.arraycopy(values, 0, padded, 1, values.length);
        final Double[] paddedBoxed = Arrays.stream(padded).boxed().toArray(Double[]::new);
        final List<Double> paddedList = Arrays.asList(paddedBoxed);

        assertAll(() -> assertEquals(expected, N.sum(values), "primitive array"),
                () -> assertEquals(expected, N.sum(padded, 1, padded.length - 1), "primitive range"),
                () -> assertEquals(expected, N.sumDouble(boxed), "boxed array"),
                () -> assertEquals(expected, N.sumDouble(paddedBoxed, 1, padded.length - 1), "boxed range"),
                () -> assertEquals(expected, N.sumDouble(boxed, Double::doubleValue), "mapped array"),
                () -> assertEquals(expected, N.sumDouble(paddedBoxed, 1, padded.length - 1, Double::doubleValue), "mapped array range"),
                () -> assertEquals(expected, N.sumDouble(new ArrayList<>(paddedList), 1, padded.length - 1), "random-access range"),
                () -> assertEquals(expected, N.sumDouble(new LinkedList<>(paddedList), 1, padded.length - 1), "linked range"),
                () -> assertEquals(expected, N.sumDouble(new LinkedList<>(paddedList), 1, padded.length - 1, Double::doubleValue), "mapped linked range"),
                () -> assertEquals(expected, N.sumDouble(oneShot(list)), "one-shot iterable"),
                () -> assertEquals(expected, N.sumDouble(oneShot(list), Double::doubleValue), "mapped one-shot iterable"),
                () -> assertEquals(expected, Iterables.sumDouble(oneShot(list)).orElseZero(), "Iterables sum"),
                () -> assertEquals(expected, Iterables.sumDouble(oneShot(list), Double::doubleValue).orElseZero(), "mapped Iterables sum"));
    }

    private static <T> Iterable<T> oneShot(final List<T> values) {
        return new Iterable<>() {
            private boolean consumed;

            @Override
            public Iterator<T> iterator() {
                assertFalse(consumed, "The sum must not request a second iterator");
                consumed = true;
                return values.iterator();
            }
        };
    }
}
