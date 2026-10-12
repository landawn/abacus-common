package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.Function;

@Tag("unit")
class DeepReviewNKeyCachingRegressionTest extends TestBase {

    @Test
    void arrayMinByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.minBy(elements, extractor), true);
    }

    @Test
    void iterableMinByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.minBy(Arrays.asList(elements), extractor), true);
    }

    @Test
    void iteratorMinByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.minBy(Arrays.asList(elements).iterator(), extractor), true);
    }

    @Test
    void arrayMaxByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.maxBy(elements, extractor), false);
    }

    @Test
    void iterableMaxByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.maxBy(Arrays.asList(elements), extractor), false);
    }

    @Test
    void iteratorMaxByCachesAnInstanceRepeatedInTheFirstComparison() {
        assertRepeatedInstanceCached((elements, extractor) -> N.maxBy(Arrays.asList(elements).iterator(), extractor), false);
    }

    @Test
    void equalDistinctInstancesStillHaveTheirOwnKeys() {
        final Element first = new Element(1);
        final Element equal = new Element(1);
        final Element larger = new Element(2);
        final Map<Element, Integer> calls = new IdentityHashMap<>();

        assertSame(first, N.minBy(new Element[] { first, equal, larger }, element -> {
            calls.merge(element, 1, Integer::sum);
            return element.value();
        }));

        assertEquals(3, calls.size());
        calls.values().forEach(count -> assertEquals(1, count));
    }

    private static void assertRepeatedInstanceCached(final BiFunction<Element[], Function<Element, Integer>, Element> scan, final boolean minimum) {
        final Element repeated = new Element(1);
        final Element larger = new Element(2);
        final Map<Element, Integer> calls = new IdentityHashMap<>();

        assertSame(minimum ? repeated : larger, scan.apply(new Element[] { repeated, repeated, larger }, element -> {
            calls.merge(element, 1, Integer::sum);
            return element.value();
        }));

        assertEquals(1, calls.get(repeated));
        assertEquals(1, calls.get(larger));
    }

    private record Element(int value) {
    }
}
