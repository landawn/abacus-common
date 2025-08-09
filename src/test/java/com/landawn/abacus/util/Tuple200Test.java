package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

/**
 * Unit tests for the Tuple class using standard JUnit 5 assertions.
 */
public class Tuple200Test extends TestBase {

    @Test
    public void testOfFactories() {
        assertTrue(Tuple.of("a") instanceof Tuple.Tuple1);
        assertTrue(Tuple.of("a", 2) instanceof Tuple.Tuple2);
        assertTrue(Tuple.of("a", 2, true) instanceof Tuple.Tuple3);
        assertTrue(Tuple.of("a", 2, true, 4.0d) instanceof Tuple.Tuple4);
        assertTrue(Tuple.of("a", 2, true, 4.0d, 5L) instanceof Tuple.Tuple5);
        assertTrue(Tuple.of("a", 2, true, 4.0d, 5L, 'f') instanceof Tuple.Tuple6);
        assertTrue(Tuple.of("a", 2, true, 4.0d, 5L, 'f', (byte) 7) instanceof Tuple.Tuple7);
        assertTrue(Tuple.of("a", 2, true, 4.0d, 5L, 'f', (byte) 7, 8.0f) instanceof Tuple.Tuple8);
        assertTrue(Tuple.of("a", 2, true, 4.0d, 5L, 'f', (byte) 7, 8.0f, "i") instanceof Tuple.Tuple9);
    }

    @Test
    public void testCreateFromMapEntry() {
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("key", 100);
        Tuple.Tuple2<String, Integer> t2 = Tuple.create(entry);
        assertEquals("key", t2._1);
        assertEquals(100, t2._2);
    }

    @Test
    public void testCreateFromObjectArray() {
        assertTrue(Tuple.create(new Object[0]) instanceof Tuple); // Specifically Tuple0.EMPTY
        assertEquals(1, Tuple.create(new Object[] { 1 }).arity());
        assertEquals(2, Tuple.create(new Object[] { 1, 2 }).arity());
        assertEquals(3, Tuple.create(new Object[] { 1, 2, 3 }).arity());
        assertEquals(4, Tuple.create(new Object[] { 1, 2, 3, 4 }).arity());
        assertEquals(5, Tuple.create(new Object[] { 1, 2, 3, 4, 5 }).arity());
        assertEquals(6, Tuple.create(new Object[] { 1, 2, 3, 4, 5, 6 }).arity());
        assertEquals(7, Tuple.create(new Object[] { 1, 2, 3, 4, 5, 6, 7 }).arity());
        assertEquals(8, Tuple.create(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8 }).arity());
        assertEquals(9, Tuple.create(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }).arity());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> Tuple.create(new Object[10]));
        assertEquals("Too many elements((10) to fill in Tuple.", exception.getMessage());
    }

    @Test
    public void testCreateFromCollection() {
        assertTrue(Tuple.create(Collections.emptyList()) instanceof Tuple); // Tuple0.EMPTY
        assertEquals(1, Tuple.create(Arrays.asList(1)).arity());
        assertEquals(2, Tuple.create(Arrays.asList(1, 2)).arity());
        assertEquals(3, Tuple.create(Arrays.asList(1, 2, 3)).arity());
        assertEquals(4, Tuple.create(Arrays.asList(1, 2, 3, 4)).arity());
        assertEquals(5, Tuple.create(Arrays.asList(1, 2, 3, 4, 5)).arity());
        assertEquals(6, Tuple.create(Arrays.asList(1, 2, 3, 4, 5, 6)).arity());
        assertEquals(7, Tuple.create(Arrays.asList(1, 2, 3, 4, 5, 6, 7)).arity());
        assertEquals(8, Tuple.create(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)).arity());
        assertEquals(9, Tuple.create(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)).arity());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> Tuple.create(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
        assertEquals("Too many elements((10) to fill in Tuple.", exception.getMessage());
    }

    @Test
    public void testToList() {
        assertEquals(List.of(1), Tuple.toList(Tuple.of(1)));
        assertEquals(List.of(1, 2), Tuple.toList(Tuple.of(1, 2)));
        assertEquals(List.of(1, 2, 3), Tuple.toList(Tuple.of(1, 2, 3)));
        assertEquals(List.of(1, 2, 3, 4), Tuple.toList(Tuple.of(1, 2, 3, 4)));
        assertEquals(List.of(1, 2, 3, 4, 5), Tuple.toList(Tuple.of(1, 2, 3, 4, 5)));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), Tuple.toList(Tuple.of(1, 2, 3, 4, 5, 6)));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), Tuple.toList(Tuple.of(1, 2, 3, 4, 5, 6, 7)));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), Tuple.toList(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8)));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), Tuple.toList(Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9)));
    }

    @Test
    public void testFlatten() {
        Tuple.Tuple2<Tuple.Tuple2<String, Integer>, Boolean> nested2 = Tuple.of(Tuple.of("a", 1), true);
        Tuple.Tuple3<String, Integer, Boolean> flat3 = Tuple.flatten(nested2);
        assertEquals(Tuple.of("a", 1, true), flat3);

        Tuple.Tuple3<Tuple.Tuple3<String, Integer, Boolean>, Double, Long> nested3 = Tuple.of(Tuple.of("a", 1, true), 4.0, 5L);
        Tuple.Tuple5<String, Integer, Boolean, Double, Long> flat5 = Tuple.flatten(nested3);
        assertEquals(Tuple.of("a", 1, true, 4.0, 5L), flat5);
    }

    @Test
    public void testTupleGenericMethods() throws Exception {
        Tuple.Tuple2<String, Integer> t2 = Tuple.of("val", 99);

        // map
        String mapped = t2.map(t -> t._1 + ":" + t._2);
        assertEquals("val:99", mapped);

        // accept
        final AtomicReference<Tuple> acceptedTuple = new AtomicReference<>();
        t2.accept(acceptedTuple::set);
        assertSame(t2, acceptedTuple.get());

        // filter
        Optional<Tuple.Tuple2<String, Integer>> filtered = t2.filter(t -> t._2 > 50);
        assertTrue(filtered.isPresent());
        assertSame(t2, filtered.get());

        Optional<Tuple.Tuple2<String, Integer>> notFiltered = t2.filter(t -> t._2 < 50);
        assertFalse(notFiltered.isPresent());
    }

    @Test
    public void testTuple2SpecificMethods() throws Exception {
        Tuple.Tuple2<String, Integer> t2 = Tuple.of("a", 1);

        // toPair
        assertEquals(Pair.of("a", 1), t2.toPair());

        // toEntry
        assertEquals(ImmutableEntry.of("a", 1), t2.toEntry());

        // reverse
        assertEquals(Tuple.of(1, "a"), t2.reverse());

        // map (BiFunction)
        String mapped = t2.map((a, b) -> a + b);
        assertEquals("a1", mapped);

        // accept (BiConsumer)
        final AtomicReference<String> accepted = new AtomicReference<>();
        t2.accept((a, b) -> accepted.set(a + b));
        assertEquals("a1", accepted.get());

        // filter (BiPredicate)
        assertTrue(t2.filter((a, b) -> b > 0).isPresent());
        assertFalse(t2.filter((a, b) -> b < 0).isPresent());
    }

    @Test
    public void testTuple3SpecificMethods() throws Exception {
        Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);

        // toTriple
        assertEquals(Triple.of("a", 1, true), t3.toTriple());

        // reverse
        assertEquals(Tuple.of(true, 1, "a"), t3.reverse());

        // map (TriFunction)
        String mapped = t3.map((a, b, c) -> a + b + c);
        assertEquals("a1true", mapped);

        // accept (TriConsumer)
        final AtomicReference<String> accepted = new AtomicReference<>();
        t3.accept((a, b, c) -> accepted.set(a + b + c));
        assertEquals("a1true", accepted.get());

        // filter (TriPredicate)
        assertTrue(t3.filter((a, b, c) -> c).isPresent());
        assertFalse(t3.filter((a, b, c) -> !c).isPresent());
    }

    @Test
    public void testArity() {
        assertEquals(0, Tuple.create(new Object[0]).arity());
        assertEquals(1, Tuple.of(1).arity());
        assertEquals(2, Tuple.of(1, 2).arity());
        assertEquals(3, Tuple.of(1, 2, 3).arity());
        assertEquals(4, Tuple.of(1, 2, 3, 4).arity());
        assertEquals(5, Tuple.of(1, 2, 3, 4, 5).arity());
        assertEquals(6, Tuple.of(1, 2, 3, 4, 5, 6).arity());
        assertEquals(7, Tuple.of(1, 2, 3, 4, 5, 6, 7).arity());
        assertEquals(8, Tuple.of(1, 2, 3, 4, 5, 6, 7, 8).arity());
        assertEquals(9, Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9).arity());
    }

    @Test
    public void testAnyNullAndAllNull() {
        Tuple.Tuple3<String, Integer, String> t_non_null = Tuple.of("a", 1, "c");
        Tuple.Tuple3<String, Integer, String> t_one_null = Tuple.of("a", null, "c");
        Tuple.Tuple3<String, Integer, String> t_all_null = Tuple.of(null, null, null);

        assertFalse(t_non_null.anyNull());
        assertFalse(t_non_null.allNull());

        assertTrue(t_one_null.anyNull());
        assertFalse(t_one_null.allNull());

        assertTrue(t_all_null.anyNull());
        assertTrue(t_all_null.allNull());
    }

    @Test
    public void testContains() {
        Tuple.Tuple4<String, Integer, String, Integer> t4 = Tuple.of("a", 1, null, 2);
        assertTrue(t4.contains("a"));
        assertTrue(t4.contains(1));
        assertTrue(t4.contains(null));
        assertTrue(t4.contains(2));
        assertFalse(t4.contains("b"));
        assertFalse(t4.contains(3));
    }

    @Test
    public void testToArray() {
        Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        Object[] array = t3.toArray();
        assertArrayEquals(new Object[] { "a", 1, true }, array);

        // Test with pre-sized array
        Object[] preSized = new Object[3];
        t3.toArray(preSized);
        assertArrayEquals(new Object[] { "a", 1, true }, preSized);

        // Test with smaller array
        Object[] smaller = new Object[1];
        Object[] result = t3.toArray(smaller);
        assertNotSame(smaller, result);
        assertArrayEquals(new Object[] { "a", 1, true }, result);
    }

    @Test
    public void testForEach() throws Exception {
        Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        List<Object> items = new ArrayList<>();

        Throwables.Consumer<Object, RuntimeException> consumer = items::add;
        t3.forEach(consumer);

        assertEquals(3, items.size());
        assertArrayEquals(new Object[] { "a", 1, true }, items.toArray());
    }

    @Test
    public void testEqualsAndHashCode() {
        // Equal Tuples
        Tuple.Tuple2<String, Integer> t1 = Tuple.of("a", 1);
        Tuple.Tuple2<String, Integer> t2 = Tuple.of("a", 1);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());

        // Different value
        Tuple.Tuple2<String, Integer> t3 = Tuple.of("b", 1);
        assertNotEquals(t1, t3);

        // Different type
        Tuple.Tuple3<String, Integer, Boolean> t4 = Tuple.of("a", 1, false);
        assertNotEquals(t1, t4);

        // Different order
        Tuple.Tuple2<Integer, String> t5 = Tuple.of(1, "a");
        assertNotEquals(t1, t5);

        // Null checks
        assertNotEquals(null, t1);

        // With nulls
        Tuple.Tuple2<String, Integer> t_null1 = Tuple.of(null, 1);
        Tuple.Tuple2<String, Integer> t_null2 = Tuple.of(null, 1);
        assertEquals(t_null1, t_null2);
        assertEquals(t_null1.hashCode(), t_null2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("(1)", Tuple.of(1).toString());
        assertEquals("(a, 2)", Tuple.of("a", 2).toString());
        assertEquals("(a, null, true)", Tuple.of("a", null, true).toString());
    }

    @Test
    public void testReverse() {
        assertEquals(Tuple.of("a", 1), Tuple.of(1, "a").reverse());
        assertEquals(Tuple.of(true, "a", 1), Tuple.of(1, "a", true).reverse());
        assertEquals(Tuple.of(4, 3, 2, 1), Tuple.of(1, 2, 3, 4).reverse());
        assertEquals(Tuple.of(5, 4, 3, 2, 1), Tuple.of(1, 2, 3, 4, 5).reverse());
        assertEquals(Tuple.of(6, 5, 4, 3, 2, 1), Tuple.of(1, 2, 3, 4, 5, 6).reverse());
        assertEquals(Tuple.of(7, 6, 5, 4, 3, 2, 1), Tuple.of(1, 2, 3, 4, 5, 6, 7).reverse());
        assertEquals(Tuple.of(8, 7, 6, 5, 4, 3, 2, 1), Tuple.of(1, 2, 3, 4, 5, 6, 7, 8).reverse());
        assertEquals(Tuple.of(9, 8, 7, 6, 5, 4, 3, 2, 1), Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9).reverse());
    }
}
