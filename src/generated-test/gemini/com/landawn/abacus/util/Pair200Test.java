package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

/**
 * Unit tests for the Pair class.
 */
public class Pair200Test extends TestBase {

    @Test
    public void testConstructorsAndFactories() {
        // Default constructor
        Pair<String, Integer> p1 = new Pair<>();
        assertNull(p1.left());
        assertNull(p1.right());

        // of(l, r) factory
        Pair<String, Integer> p2 = Pair.of("hello", 123);
        assertEquals("hello", p2.left());
        assertEquals(123, p2.right());

        // create(Map.Entry) factory
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("world", 456);
        Pair<String, Integer> p3 = Pair.create(entry);
        assertEquals("world", p3.left());
        assertEquals(456, p3.right());
    }

    @Test
    public void testEmptyArray() {
        Pair<String, Integer>[] emptyArray = Pair.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testGettersAndSetters() {
        Pair<String, Integer> pair = Pair.of("initialLeft", 1);

        // Getters
        assertEquals("initialLeft", pair.left());
        assertEquals(1, pair.right());

        // Setters
        pair.setLeft("newLeft");
        assertEquals("newLeft", pair.left());

        pair.setRight(2);
        assertEquals(2, pair.right());

        pair.set("finalLeft", 3);
        assertEquals("finalLeft", pair.left());
        assertEquals(3, pair.right());
    }

    @Test
    public void testGetAndSet() {
        Pair<String, Integer> pair = Pair.of("left", 1);

        // Left
        String oldLeft = pair.getAndSetLeft("newLeft");
        assertEquals("left", oldLeft);
        assertEquals("newLeft", pair.left());

        String newLeft = pair.setAndGetLeft("finalLeft");
        assertEquals("finalLeft", newLeft);
        assertEquals("finalLeft", pair.left());

        // Right
        Integer oldRight = pair.getAndSetRight(2);
        assertEquals(1, oldRight);
        assertEquals(2, pair.right());

        Integer newRight = pair.setAndGetRight(3);
        assertEquals(3, newRight);
        assertEquals(3, pair.right());
    }

    @Test
    public void testConditionalSetters() throws Exception {
        Pair<String, Integer> pair = Pair.of("left", 1);

        // setLeftIf
        boolean leftSet = pair.setLeftIf("newLeft", (p, newL) -> p.right() == 1);
        assertTrue(leftSet);
        assertEquals("newLeft", pair.left());

        boolean leftNotSet = pair.setLeftIf("anotherLeft", (p, newL) -> p.right() == 0);
        assertFalse(leftNotSet);
        assertEquals("newLeft", pair.left());

        // setRightIf
        boolean rightSet = pair.setRightIf(100, (p, newR) -> newR > 50);
        assertTrue(rightSet);
        assertEquals(100, pair.right());

        boolean rightNotSet = pair.setRightIf(10, (p, newR) -> newR > 50);
        assertFalse(rightNotSet);
        assertEquals(100, pair.right());

        // setIf
        boolean bothSet = pair.setIf("finalLeft", 999, (p, nL, nR) -> nL.length() > 5 && nR > 900);
        assertTrue(bothSet);
        assertEquals("finalLeft", pair.left());
        assertEquals(999, pair.right());

        boolean bothNotSet = pair.setIf("short", 0, (p, nL, nR) -> nL.length() > 5 && nR > 900);
        assertFalse(bothNotSet);
        assertEquals("finalLeft", pair.left());
        assertEquals(999, pair.right());
    }

    @Test
    public void testUtilityAndConversionMethods() {
        Pair<String, Integer> pair = Pair.of("A", 1);

        // reverse
        Pair<Integer, String> reversed = pair.reverse();
        assertEquals(1, reversed.left());
        assertEquals("A", reversed.right());

        // copy
        Pair<String, Integer> copy = pair.copy();
        assertEquals(pair, copy);
        assertNotSame(pair, copy);

        // toTuple
        Tuple.Tuple2<String, Integer> tuple = pair.toTuple();
        assertEquals(Tuple.of("A", 1), tuple);

        // toImmutableEntry
        ImmutableEntry<String, Integer> immutableEntry = pair.toImmutableEntry();
        assertEquals(ImmutableEntry.of("A", 1), immutableEntry);
    }

    @Test
    public void testToArray() {
        Pair<String, Integer> pair = Pair.of("A", 1);

        // toArray()
        Object[] objArray = pair.toArray();
        assertArrayEquals(new Object[] { "A", 1 }, objArray);

        // toArray(A[]) with sufficient size
        Object[] strArray = new Object[2];
        pair.toArray(strArray);
        assertArrayEquals(new Object[] { "A", 1 }, strArray);

        // toArray(A[]) with insufficient size
        Object[] smallArray = new Object[1];
        Object[] newArray = pair.toArray(smallArray);
        assertNotSame(smallArray, newArray);
        assertEquals(2, newArray.length);
        assertArrayEquals(new Object[] { "A", 1 }, newArray);
    }

    @Test
    public void testFunctionalMethods() throws Exception {
        Pair<String, Integer> pair = Pair.of("A", 1);

        // forEach
        List<Object> collected = new ArrayList<>();
        pair.forEach(collected::add);
        assertEquals(List.of("A", 1), collected);

        // accept(BiConsumer)
        List<Object> biAccepted = new ArrayList<>();
        pair.accept((l, r) -> {
            biAccepted.add(l);
            biAccepted.add(r);
        });
        assertEquals(List.of("A", 1), biAccepted);

        // accept(Consumer)
        final List<Pair<String, Integer>> pairAccepted = new ArrayList<>();
        pair.accept(e -> pairAccepted.add(e));
        assertEquals(1, pairAccepted.size());
        assertSame(pair, pairAccepted.get(0));

        // map(BiFunction)
        String mapped1 = pair.map((l, r) -> l + r);
        assertEquals("A1", mapped1);

        // map(Function)
        String mapped2 = pair.map(p -> p.left() + ":" + p.right());
        assertEquals("A:1", mapped2);

        // filter(BiPredicate)
        Optional<Pair<String, Integer>> filtered1 = pair.filter((l, r) -> r > 0);
        assertTrue(filtered1.isPresent());
        assertSame(pair, filtered1.get());

        Optional<Pair<String, Integer>> filtered2 = pair.filter((l, r) -> r < 0);
        assertFalse(filtered2.isPresent());

        // filter(Predicate)
        Optional<Pair<String, Integer>> filtered3 = pair.filter(p -> p.left().equals("A"));
        assertTrue(filtered3.isPresent());
        assertSame(pair, filtered3.get());
    }

    @Test
    public void testMapEntryMethods() {
        Pair<String, Integer> pair = Pair.of("key", 10);

        // getKey (deprecated)
        assertEquals("key", pair.getKey());

        // getValue (deprecated)
        assertEquals(10, pair.getValue());

        // setValue (deprecated)
        Integer oldValue = pair.setValue(20);
        assertEquals(10, oldValue);
        assertEquals(20, pair.right());
        assertEquals(20, pair.getValue());
    }

    @Test
    public void testEqualsAndHashCode() {
        Pair<String, Integer> p1 = Pair.of("A", 1);
        Pair<String, Integer> p2 = Pair.of("A", 1);
        Pair<String, Integer> p3 = Pair.of("B", 1);
        Pair<String, Integer> p4 = Pair.of("A", 2);
        Pair<String, Integer> p5 = Pair.of(null, null);
        Pair<String, Integer> p6 = Pair.of(null, null);

        // Reflexive
        assertEquals(p1, p1);

        // Symmetric
        assertEquals(p1, p2);
        assertEquals(p2, p1);

        // HashCode
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p5.hashCode(), p6.hashCode());

        // Unequal
        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
        assertNotEquals(p3, p4);
        assertNotEquals(p1.hashCode(), p3.hashCode());
        assertNotEquals(p1.hashCode(), p4.hashCode());

        // Nulls
        assertEquals(p5, p6);
        assertNotEquals(p1, p5);

        // Other types
        assertNotEquals("A", p1);
        assertNotEquals(null, p1);
    }

    @Test
    public void testToString() {
        Pair<String, Integer> pair = Pair.of("hello", 123);
        assertEquals("(hello, 123)", pair.toString());

        Pair<String, Integer> pairWithNull = Pair.of(null, 123);
        assertEquals("(null, 123)", pairWithNull.toString());
    }
}
