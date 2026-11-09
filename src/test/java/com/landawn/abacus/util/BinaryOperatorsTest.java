package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Flushable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Coverage gaps for several small classes in the util package: AppendableWriter, the summary-statistics
 * state constructors, BinaryOperators "to first" factories, ImmutableSortedMap, MutableShort, MutableInt and Holder.
 */
public class BinaryOperatorsTest extends TestBase {

    @SuppressWarnings("deprecation")
    @Test
    public void testBinaryOperatorsToFirst() {
        final List<String> first = new ArrayList<>(Arrays.asList("a", "b"));
        assertSame(first, BinaryOperators.<String, List<String>> ofRemoveAllFromFirst().apply(first, Arrays.asList("a")));
        assertEquals(List.of("b"), first);

        final Map<String, Integer> m = new HashMap<>(Map.of("k", 1));
        assertSame(m, BinaryOperators.<String, Integer, Map<String, Integer>> ofPutAllToFirst().apply(m, Map.of("m", 2)));
        assertEquals(Map.of("k", 1, "m", 2), m);

        final Joiner j = Joiner.with(",").append("a");
        assertSame(j, BinaryOperators.ofMergeToFirst().apply(j, Joiner.with(",").append("b")));
        assertEquals("a,b", j.toString());

        final StringBuilder sb = new StringBuilder("a");
        assertSame(sb, BinaryOperators.ofAppendToFirst().apply(sb, new StringBuilder("b")));
        assertEquals("ab", sb.toString());
    }
}
