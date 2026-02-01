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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class Pair200Test extends TestBase {

    @Test
    public void testConstructorsAndFactories() {
        Pair<String, Integer> p1 = new Pair<>();
        assertNull(p1.left());
        assertNull(p1.right());

        Pair<String, Integer> p2 = Pair.of("hello", 123);
        assertEquals("hello", p2.left());
        assertEquals(123, p2.right());

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

        assertEquals("initialLeft", pair.left());
        assertEquals(1, pair.right());

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

        String oldLeft = pair.getAndSetLeft("newLeft");
        assertEquals("left", oldLeft);
        assertEquals("newLeft", pair.left());

        String newLeft = pair.setAndGetLeft("finalLeft");
        assertEquals("finalLeft", newLeft);
        assertEquals("finalLeft", pair.left());

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

        // left depends on current right == 1
        boolean leftSet = pair.setLeftIf("newLeft", (l, r) -> r == 1);
        assertTrue(leftSet);
        assertEquals("newLeft", pair.left());
        assertEquals(1, pair.right()); // right unchanged

        // predicate is false: right is still 1, not 0
        boolean leftNotSet = pair.setLeftIf("anotherLeft", (l, r) -> r == 0);
        assertFalse(leftNotSet);
        assertEquals("newLeft", pair.left());
        assertEquals(1, pair.right());

        // right depends on current left value
        boolean rightSet = pair.setRightIf(100, (l, r) -> "newLeft".equals(l));
        assertTrue(rightSet);
        assertEquals("newLeft", pair.left());
        assertEquals(100, pair.right());

        // predicate is false: right is 100, not < 50
        boolean rightNotSet = pair.setRightIf(10, (l, r) -> r < 50);
        assertFalse(rightNotSet);
        assertEquals("newLeft", pair.left());
        assertEquals(100, pair.right());

        // both set if current right == 100
        boolean bothSet = pair.setIf("finalLeft", 999, (l, r) -> r == 100);
        assertTrue(bothSet);
        assertEquals("finalLeft", pair.left());
        assertEquals(999, pair.right());

        // predicate always false here
        boolean bothNotSet = pair.setIf("short", 0, (l, r) -> r < 0);
        assertFalse(bothNotSet);
        assertEquals("finalLeft", pair.left());
        assertEquals(999, pair.right());
    }

    @Test
    public void testUtilityAndConversionMethods() {
        Pair<String, Integer> pair = Pair.of("A", 1);

        Pair<Integer, String> reversed = pair.swap();
        assertEquals(1, reversed.left());
        assertEquals("A", reversed.right());

        Pair<String, Integer> copy = pair.copy();
        assertEquals(pair, copy);
        assertNotSame(pair, copy);

        Tuple.Tuple2<String, Integer> tuple = pair.toTuple();
        assertEquals(Tuple.of("A", 1), tuple);

        ImmutableEntry<String, Integer> immutableEntry = pair.toImmutableEntry();
        assertEquals(ImmutableEntry.of("A", 1), immutableEntry);
    }

    @Test
    public void testToArray() {
        Pair<String, Integer> pair = Pair.of("A", 1);

        Object[] objArray = pair.toArray();
        assertArrayEquals(new Object[] { "A", 1 }, objArray);

        Object[] strArray = new Object[2];
        pair.toArray(strArray);
        assertArrayEquals(new Object[] { "A", 1 }, strArray);

        Object[] smallArray = new Object[1];
        Object[] newArray = pair.toArray(smallArray);
        assertNotSame(smallArray, newArray);
        assertEquals(2, newArray.length);
        assertArrayEquals(new Object[] { "A", 1 }, newArray);
    }

    @Test
    public void testFunctionalMethods() throws Exception {
        Pair<String, Integer> pair = Pair.of("A", 1);

        List<Object> collected = new ArrayList<>();
        pair.forEach(collected::add);
        assertEquals(List.of("A", 1), collected);

        List<Object> biAccepted = new ArrayList<>();
        pair.accept((l, r) -> {
            biAccepted.add(l);
            biAccepted.add(r);
        });
        assertEquals(List.of("A", 1), biAccepted);

        final List<Pair<String, Integer>> pairAccepted = new ArrayList<>();
        pair.accept(e -> pairAccepted.add(e));
        assertEquals(1, pairAccepted.size());
        assertSame(pair, pairAccepted.get(0));

        String mapped1 = pair.map((l, r) -> l + r);
        assertEquals("A1", mapped1);

        String mapped2 = pair.map(p -> p.left() + ":" + p.right());
        assertEquals("A:1", mapped2);

        Optional<Pair<String, Integer>> filtered1 = pair.filter((l, r) -> r > 0);
        assertTrue(filtered1.isPresent());
        assertSame(pair, filtered1.get());

        Optional<Pair<String, Integer>> filtered2 = pair.filter((l, r) -> r < 0);
        assertFalse(filtered2.isPresent());

        Optional<Pair<String, Integer>> filtered3 = pair.filter(p -> p.left().equals("A"));
        assertTrue(filtered3.isPresent());
        assertSame(pair, filtered3.get());
    }

    @Test
    public void testMapEntryMethods() {
        Pair<String, Integer> pair = Pair.of("key", 10);

        assertEquals("key", pair.getKey());

        assertEquals(10, pair.getValue());

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

        assertEquals(p1, p1);

        assertEquals(p1, p2);
        assertEquals(p2, p1);

        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p5.hashCode(), p6.hashCode());

        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
        assertNotEquals(p3, p4);
        assertNotEquals(p1.hashCode(), p3.hashCode());
        assertNotEquals(p1.hashCode(), p4.hashCode());

        assertEquals(p5, p6);
        assertNotEquals(p1, p5);

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
