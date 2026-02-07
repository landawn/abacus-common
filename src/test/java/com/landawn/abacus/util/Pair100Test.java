package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class Pair100Test extends TestBase {

    private Pair<String, Integer> pair;

    @BeforeEach
    public void setUp() {
        pair = new Pair<>();
    }

    @Test
    public void testDefaultConstructor() {
        assertNull(pair.left());
        assertNull(pair.right());
    }

    @Test
    public void testOfMethod() {
        Pair<String, Integer> p = Pair.of("test", 42);
        assertEquals("test", p.left());
        assertEquals(42, p.right());

        Pair<String, Integer> nullPair = Pair.of(null, null);
        assertNull(nullPair.left());
        assertNull(nullPair.right());
    }

    @Test
    public void testCreateFromMapEntry() {
        Map.Entry<String, Integer> entry = new Map.Entry<>() {
            @Override
            public String getKey() {
                return "key";
            }

            @Override
            public Integer getValue() {
                return 100;
            }

            @Override
            public Integer setValue(Integer value) {
                throw new UnsupportedOperationException();
            }
        };

        Pair<String, Integer> p = Pair.create(entry);
        assertEquals("key", p.left());
        assertEquals(100, p.right());
    }

    @Test
    public void testEmptyArray() {
        Pair<String, Integer>[] emptyArray = Pair.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);

        assertSame(emptyArray, Pair.emptyArray());
    }

    @Test
    public void testGettersAndSetters() {
        pair.setLeft("hello");
        pair.setRight(123);

        assertEquals("hello", pair.left());
        assertEquals(123, pair.right());

        assertEquals("hello", pair.left());
        assertEquals(123, pair.right());
    }

    @Test
    public void testSetMethod() {
        pair.set("world", 456);
        assertEquals("world", pair.left());
        assertEquals(456, pair.right());
    }

    @Test
    public void testGetAndSetLeft() {
        pair.setLeft("initial");
        String oldValue = pair.getAndSetLeft("new");

        assertEquals("initial", oldValue);
        assertEquals("new", pair.left());
    }

    @Test
    public void testSetAndGetLeft() {
        String newValue = pair.setAndGetLeft("setValue");

        assertEquals("setValue", newValue);
        assertEquals("setValue", pair.left());
    }

    @Test
    public void testGetAndSetRight() {
        pair.setRight(100);
        Integer oldValue = pair.getAndSetRight(200);

        assertEquals(100, oldValue);
        assertEquals(200, pair.right());
    }

    @Test
    public void testSetAndGetRight() {
        Integer newValue = pair.setAndGetRight(300);

        assertEquals(300, newValue);
        assertEquals(300, pair.right());
    }

    @Test
    public void testSetLeftIf() throws Exception {
        // assuming pair is something like Pair<String, Integer>
        pair.set("original", 10);

        boolean result = pair.setLeftIf((l, r) -> r > 5, "new");
        assertTrue(result);
        assertEquals("new", pair.left());
        assertEquals(10, pair.right()); // right unchanged

        result = pair.setLeftIf((l, r) -> r > 20, "a");
        assertFalse(result);
        assertEquals("new", pair.left()); // still "new"
        assertEquals(10, pair.right()); // still 10
    }

    @Test
    public void testSetRightIf() throws Exception {
        pair.set("Hello", 10);

        boolean result = pair.setRightIf((l, r) -> l.length() > 3, 20);
        assertTrue(result);
        assertEquals("Hello", pair.left()); // left unchanged
        assertEquals(20, pair.right());

        result = pair.setRightIf((l, r) -> r < 10, 5);
        assertFalse(result);
        assertEquals("Hello", pair.left());
        assertEquals(20, pair.right()); // unchanged because predicate was false
    }

    @Test
    public void testSetIf() throws Exception {
        pair.set("old", 1);

        boolean result = pair.setIf((l, r) -> r < 5, "new", 2);
        assertTrue(result);
        assertEquals("new", pair.left());
        assertEquals(2, pair.right());

        result = pair.setIf((l, r) -> r > 10, "newer", 0);
        assertFalse(result);
        assertEquals("new", pair.left()); // unchanged
        assertEquals(2, pair.right()); // unchanged
    }

    @Test
    public void testReverse() {
        pair.set("left", 100);
        Pair<Integer, String> reversed = pair.swap();

        assertEquals(100, reversed.left());
        assertEquals("left", reversed.right());

        assertEquals("left", pair.left());
        assertEquals(100, pair.right());
    }

    @Test
    public void testCopy() {
        pair.set("original", 42);
        Pair<String, Integer> copy = pair.copy();

        assertEquals(pair.left(), copy.left());
        assertEquals(pair.right(), copy.right());
        assertNotSame(pair, copy);

        copy.setLeft("modified");
        assertEquals("original", pair.left());
    }

    @Test
    public void testToArray() {
        pair.set("test", 123);
        Object[] array = pair.toArray();

        assertEquals(2, array.length);
        assertEquals("test", array[0]);
        assertEquals(123, array[1]);
    }

    @Test
    public void testToArrayWithParameter() {
        pair.set("test", 123);

        Object[] exactArray = new Object[2];
        Object[] result = pair.toArray(exactArray);
        assertSame(exactArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);

        Object[] smallArray = new Object[1];
        result = pair.toArray(smallArray);
        assertNotSame(smallArray, result);
        assertEquals(2, result.length);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);

        Object[] largeArray = new Object[5];
        result = pair.toArray(largeArray);
        assertSame(largeArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
    }

    @Test
    public void testForEach() throws Exception {
        pair.set("value1", 2);
        StringBuilder sb = new StringBuilder();

        pair.forEach(obj -> sb.append(obj).append(","));

        assertEquals("value1,2,", sb.toString());
    }

    @Test
    public void testAcceptBiConsumer() throws Exception {
        pair.set("test", 100);
        StringBuilder sb = new StringBuilder();

        pair.accept((left, right) -> sb.append(left).append(":").append(right));

        assertEquals("test:100", sb.toString());
    }

    @Test
    public void testAcceptConsumer() throws Exception {
        pair.set("test", 100);
        StringBuilder sb = new StringBuilder();

        pair.accept(p -> sb.append(p.left()).append("-").append(p.right()));

        assertEquals("test-100", sb.toString());
    }

    @Test
    public void testMapBiFunction() throws Exception {
        pair.set("Hello", 5);

        String result = pair.map((left, right) -> left + " has " + right + " letters");

        assertEquals("Hello has 5 letters", result);
    }

    @Test
    public void testMapFunction() throws Exception {
        pair.set("Hello", 5);

        Integer result = pair.map(p -> p.left().length() + p.right());

        assertEquals(10, result);
    }

    @Test
    public void testFilterBiPredicate() throws Exception {
        pair.set("test", 4);

        Optional<Pair<String, Integer>> result = pair.filter((left, right) -> left.length() == right);
        assertTrue(result.isPresent());
        assertSame(pair, result.get());

        result = pair.filter((left, right) -> left.length() != right);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFilterPredicate() throws Exception {
        pair.set("test", 4);

        Optional<Pair<String, Integer>> result = pair.filter(p -> p.right() > 0);
        assertTrue(result.isPresent());
        assertSame(pair, result.get());

        result = pair.filter(p -> p.right() < 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testToTuple() {
        pair.set("tuple", 42);
        Tuple2<String, Integer> tuple = pair.toTuple();

        assertEquals("tuple", tuple._1);
        assertEquals(42, tuple._2);
    }

    @Test
    public void testToImmutableEntry() {
        pair.set("key", 100);
        ImmutableEntry<String, Integer> entry = pair.toImmutableEntry();

        assertEquals("key", entry.getKey());
        assertEquals(100, entry.getValue());
    }

    @Test
    public void testMapEntryMethods() {
        pair.set("mapKey", 200);

        assertEquals("mapKey", pair.getKey());
        assertEquals(200, pair.getValue());

        Integer oldValue = pair.setValue(300);
        assertEquals(200, oldValue);
        assertEquals(300, pair.right());
    }

    @Test
    public void testHashCode() {
        Pair<String, Integer> p1 = Pair.of("test", 123);
        Pair<String, Integer> p2 = Pair.of("test", 123);
        Pair<String, Integer> p3 = Pair.of("test", 124);
        Pair<String, Integer> p4 = Pair.of("other", 123);

        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1.hashCode(), p3.hashCode());
        assertNotEquals(p1.hashCode(), p4.hashCode());

        Pair<String, Integer> nullLeft = Pair.of(null, 123);
        Pair<String, Integer> nullRight = Pair.of("test", null);
        Pair<String, Integer> bothNull = Pair.of(null, null);

        assertDoesNotThrow(() -> nullLeft.hashCode());
        assertDoesNotThrow(() -> nullRight.hashCode());
        assertDoesNotThrow(() -> bothNull.hashCode());
    }

    @Test
    public void testEquals() {
        Pair<String, Integer> p1 = Pair.of("test", 123);
        Pair<String, Integer> p2 = Pair.of("test", 123);
        Pair<String, Integer> p3 = Pair.of("test", 124);
        Pair<String, Integer> p4 = Pair.of("other", 123);

        assertEquals(p1, p1);

        assertEquals(p1, p2);
        assertEquals(p2, p1);

        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);

        assertNotEquals(p1, null);
        assertNotEquals(p1, "not a pair");

        Pair<String, Integer> nullLeft1 = Pair.of(null, 123);
        Pair<String, Integer> nullLeft2 = Pair.of(null, 123);
        Pair<String, Integer> nullRight1 = Pair.of("test", null);
        Pair<String, Integer> nullRight2 = Pair.of("test", null);

        assertEquals(nullLeft1, nullLeft2);
        assertEquals(nullRight1, nullRight2);
        assertNotEquals(nullLeft1, nullRight1);
    }

    @Test
    public void testToString() {
        pair.set("hello", 42);
        assertEquals("(hello, 42)", pair.toString());

        pair.set(null, null);
        assertEquals("(null, null)", pair.toString());

        pair.set("test", null);
        assertEquals("(test, null)", pair.toString());

        pair.set(null, 100);
        assertEquals("(null, 100)", pair.toString());
    }

    @Test
    public void testMutableBehavior() {
        Pair<String, Integer> p = Pair.of("initial", 1);

        p.setLeft("modified");
        p.setRight(2);

        assertEquals("modified", p.left());
        assertEquals(2, p.right());
    }

    @Test
    public void testWithDifferentTypes() {
        Pair<Double, String> doublePair = Pair.of(3.14, "pi");
        assertEquals(3.14, doublePair.left());
        assertEquals("pi", doublePair.right());

        Pair<Boolean, Character> boolCharPair = Pair.of(true, 'A');
        assertEquals(true, boolCharPair.left());
        assertEquals('A', boolCharPair.right());

        Pair<StringBuilder, Thread> customPair = Pair.of(new StringBuilder("test"), Thread.currentThread());
        assertNotNull(customPair.left());
        assertNotNull(customPair.right());
    }
}
