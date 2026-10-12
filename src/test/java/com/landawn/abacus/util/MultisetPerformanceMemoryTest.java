package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Multiset. */
@Tag("slow-test")
public class MultisetPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G043 end ----
    // ---- perf review 2026-10-08 G045 begin ----
    @Nested
    class G045 {

        // G045-01: the no-arg constructor builds a fresh, empty HashMap backing map of its own
        @Test
        void testNoArgConstructor_freshEmptyHashMapBacking() {
            final Multiset<String> first = new Multiset<>();
            final Multiset<String> second = new Multiset<>();

            assertTrue(first.isEmpty());
            assertEquals(0, first.size());
            assertEquals(0, first.countOfDistinctElements());
            assertEquals(0L, first.sumOfOccurrences());
            assertEquals("{}", first.toString());
            assertSame(new HashMap<String, Object>().keySet().getClass(), first.elementSet().getClass());
            assertSame(HashMap.class, first.toMap().getClass());
            assertNotSame(first.elementSet(), second.elementSet());

            assertEquals(0, first.add("a", 2));
            first.add("b");
            assertEquals(2, first.getCount("a"));
            assertEquals(3, first.size());
            assertEquals(0, second.getCount("a"));
            assertTrue(second.isEmpty());
            assertSame(HashMap.class, first.toMap().getClass());
            assertEquals(Map.of("a", 2, "b", 1), first.toMap());
        }

        // G045-01: HashMap semantics of the default backing map (null element, equals-based keys, foreign-typed lookup keys)
        @Test
        void testNoArgConstructor_hashMapSemantics() {
            final Multiset<String> multiset = new Multiset<>();

            assertTrue(multiset.add(null));
            assertEquals(1, multiset.add(null, 2));
            multiset.add(new String("x"));
            multiset.add(new String("x"));

            assertEquals(3, multiset.getCount(null));
            assertEquals(2, multiset.getCount("x"));
            assertEquals(2, multiset.countOfDistinctElements());
            assertEquals(5, multiset.size());
            assertEquals(0, multiset.getCount(Integer.valueOf(42)));
            assertFalse(multiset.contains(Integer.valueOf(42)));
            assertEquals(0, multiset.removeAllOccurrencesOf(Integer.valueOf(42)));

            final Multiset<String> viaClass = new Multiset<>(HashMap.class);
            viaClass.add("x", 2);
            viaClass.add(null, 3);
            assertEquals(viaClass, multiset);
            assertEquals(multiset, viaClass);
            assertEquals(viaClass.hashCode(), multiset.hashCode());

            assertEquals(3, multiset.removeAllOccurrencesOf((Object) null));
            assertEquals(1, multiset.removeAndGetCount("x", 1));
            assertEquals(0, multiset.removeAndGetCount("x", 5));
            assertTrue(multiset.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> multiset.add("y", -1));
            assertTrue(multiset.isEmpty());
        }

        // G045-01: factories that delegate to the no-arg constructor still return independent HashMap-backed multisets
        @Test
        void testNoArgConstructor_viaFactories() {
            final Multiset<String> fromNullIterator = Multiset.create((Iterator<String>) null);
            assertTrue(fromNullIterator.isEmpty());
            assertSame(HashMap.class, fromNullIterator.toMap().getClass());

            final Multiset<String> fromIterator = Multiset.create(Arrays.asList("b", "a", "b").iterator());
            assertEquals(2, fromIterator.getCount("b"));
            assertEquals(1, fromIterator.getCount("a"));
            assertSame(HashMap.class, fromIterator.toMap().getClass());

            final Multiset<String> emptyOf = Multiset.of();
            final Multiset<String> otherEmptyOf = Multiset.of();
            assertTrue(emptyOf.isEmpty());
            assertSame(HashMap.class, emptyOf.toMap().getClass());
            emptyOf.add("z");
            assertTrue(otherEmptyOf.isEmpty());

            final Multiset<String> fromN = CommonUtil.newMultiset();
            assertTrue(fromN.isEmpty());
            assertSame(HashMap.class, fromN.toMap().getClass());

            final Multiset<String> collected = com.landawn.abacus.util.stream.Stream.of("b", "a", "b", "c")
                    .collect(com.landawn.abacus.util.stream.Collectors.toMultiset());
            assertEquals(Map.of("a", 1, "b", 2, "c", 1), collected.toMap());
            assertSame(HashMap.class, collected.toMap().getClass());

            final Multiset<String> streamed = com.landawn.abacus.util.stream.Stream.of("q", "q").toMultiset();
            assertEquals(2, streamed.getCount("q"));
            assertSame(HashMap.class, streamed.toMap().getClass());
        }
    }
}
