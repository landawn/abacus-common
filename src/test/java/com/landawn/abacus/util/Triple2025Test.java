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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class Triple2025Test extends TestBase {

    private Triple<String, Integer, Boolean> triple;

    @BeforeEach
    public void setUp() {
        triple = new Triple<>();
    }

    @Test
    public void testDefaultConstructor() {
        assertNull(triple.left());
        assertNull(triple.middle());
        assertNull(triple.right());
    }

    @Test
    public void testOfMethod() {
        Triple<String, Integer, Boolean> t = Triple.of("test", 42, true);
        assertEquals("test", t.left());
        assertEquals(42, t.middle());
        assertEquals(true, t.right());

        Triple<String, Integer, Boolean> nullTriple = Triple.of(null, null, null);
        assertNull(nullTriple.left());
        assertNull(nullTriple.middle());
        assertNull(nullTriple.right());
    }

    @Test
    public void testEmptyArray() {
        Triple<String, Integer, Boolean>[] emptyArray = Triple.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);

        assertSame(emptyArray, Triple.emptyArray());
    }

    @Test
    public void testGettersAndSetters() {
        triple.setLeft("hello");
        triple.setMiddle(123);
        triple.setRight(true);

        assertEquals("hello", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void testDeprecatedGetLeft() {
        triple.setLeft("test");
        assertEquals("test", triple.getLeft());
    }

    @Test
    public void testDeprecatedGetMiddle() {
        triple.setMiddle(100);
        assertEquals(100, triple.getMiddle());
    }

    @Test
    public void testDeprecatedGetRight() {
        triple.setRight(false);
        assertEquals(false, triple.getRight());
    }

    @Test
    public void testSetMethod() {
        triple.set("world", 456, false);
        assertEquals("world", triple.left());
        assertEquals(456, triple.middle());
        assertEquals(false, triple.right());
    }

    @Test
    public void testGetAndSetLeft() {
        triple.setLeft("initial");
        String oldValue = triple.getAndSetLeft("new");

        assertEquals("initial", oldValue);
        assertEquals("new", triple.left());
    }

    @Test
    public void testSetAndGetLeft() {
        String newValue = triple.setAndGetLeft("setValue");

        assertEquals("setValue", newValue);
        assertEquals("setValue", triple.left());
    }

    @Test
    public void testGetAndSetMiddle() {
        triple.setMiddle(100);
        Integer oldValue = triple.getAndSetMiddle(200);

        assertEquals(100, oldValue);
        assertEquals(200, triple.middle());
    }

    @Test
    public void testSetAndGetMiddle() {
        Integer newValue = triple.setAndGetMiddle(300);

        assertEquals(300, newValue);
        assertEquals(300, triple.middle());
    }

    @Test
    public void testGetAndSetRight() {
        triple.setRight(true);
        Boolean oldValue = triple.getAndSetRight(false);

        assertEquals(true, oldValue);
        assertEquals(false, triple.right());
    }

    @Test
    public void testSetAndGetRight() {
        Boolean newValue = triple.setAndGetRight(true);

        assertEquals(true, newValue);
        assertEquals(true, triple.right());
    }

    @Test
    public void testSetLeftIf() throws Exception {
        // Assume triple is Triple<String, Integer, Boolean>
        triple.setLeft("original");

        // First update: predicate true when left is "original"
        boolean result = triple.setLeftIf("new", (l, m, r) -> "original".equals(l));
        assertTrue(result);
        assertEquals("new", triple.left());

        // Second update: predicate false now that left != "original"
        result = triple.setLeftIf("a", (l, m, r) -> "original".equals(l));
        assertFalse(result);
        assertEquals("new", triple.left());
    }

    @Test
    public void testSetMiddleIf() throws Exception {
        triple.setMiddle(10);

        // First update: predicate true when middle is 10
        boolean result = triple.setMiddleIf(20, (l, m, r) -> m == 10);
        assertTrue(result);
        assertEquals(20, triple.middle());

        // Second update: predicate false now that middle != 10
        result = triple.setMiddleIf(5, (l, m, r) -> m == 10);
        assertFalse(result);
        assertEquals(20, triple.middle());
    }

    @Test
    public void testSetRightIf() throws Exception {
        triple.setRight(true);

        // First update: predicate true when right is true
        boolean result = triple.setRightIf(false, (l, m, r) -> r);
        assertTrue(result);
        assertEquals(false, triple.right());

        // Second update: predicate false now that right is false
        result = triple.setRightIf(true, (l, m, r) -> r);
        assertFalse(result);
        assertEquals(false, triple.right());
    }

    @Test
    public void testSetIf() throws Exception {
        triple.set("old", 1, true);

        // First update: predicate true for the initial state
        boolean result = triple.setIf("new", 2, false, (l, m, r) -> m > 0 && r);
        assertTrue(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(false, triple.right());

        // Second update: predicate false for the new state
        result = triple.setIf("newer", 0, true, (l, m, r) -> m > 0 && r);
        assertFalse(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(false, triple.right());
    }

    @Test
    public void testReverse() {
        triple.set("left", 42, true);
        Triple<Boolean, Integer, String> reversed = triple.swap();

        assertEquals(true, reversed.left());
        assertEquals(42, reversed.middle());
        assertEquals("left", reversed.right());

        assertEquals("left", triple.left());
        assertEquals(42, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void testCopy() {
        triple.set("original", 42, true);
        Triple<String, Integer, Boolean> copy = triple.copy();

        assertEquals(triple.left(), copy.left());
        assertEquals(triple.middle(), copy.middle());
        assertEquals(triple.right(), copy.right());
        assertNotSame(triple, copy);

        copy.setLeft("modified");
        assertEquals("original", triple.left());
    }

    @Test
    public void testToArray() {
        triple.set("test", 123, false);
        Object[] array = triple.toArray();

        assertEquals(3, array.length);
        assertEquals("test", array[0]);
        assertEquals(123, array[1]);
        assertEquals(false, array[2]);
    }

    @Test
    public void testToArrayWithParameter() {
        triple.set("test", 123, true);

        Object[] exactArray = new Object[3];
        Object[] result = triple.toArray(exactArray);
        assertSame(exactArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(true, result[2]);

        Object[] smallArray = new Object[1];
        result = triple.toArray(smallArray);
        assertNotSame(smallArray, result);
        assertEquals(3, result.length);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(true, result[2]);

        Object[] largeArray = new Object[5];
        result = triple.toArray(largeArray);
        assertSame(largeArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(true, result[2]);
    }

    @Test
    public void testForEach() throws Exception {
        triple.set("value1", 2, true);
        StringBuilder sb = new StringBuilder();

        triple.forEach(obj -> sb.append(obj).append(","));

        assertEquals("value1,2,true,", sb.toString());
    }

    @Test
    public void testAcceptTriConsumer() throws Exception {
        triple.set("test", 100, false);
        StringBuilder sb = new StringBuilder();

        triple.accept((left, middle, right) -> sb.append(left).append(":").append(middle).append(":").append(right));

        assertEquals("test:100:false", sb.toString());
    }

    @Test
    public void testAcceptConsumer() throws Exception {
        triple.set("test", 100, true);
        StringBuilder sb = new StringBuilder();

        triple.accept(t -> sb.append(t.left()).append("-").append(t.middle()).append("-").append(t.right()));

        assertEquals("test-100-true", sb.toString());
    }

    @Test
    public void testMapTriFunction() throws Exception {
        triple.set("Hello", 5, true);

        String result = triple.map((left, middle, right) -> left + " has " + middle + " letters: " + right);

        assertEquals("Hello has 5 letters: true", result);
    }

    @Test
    public void testMapFunction() throws Exception {
        triple.set("Hello", 5, true);

        Integer result = triple.map(t -> t.left().length() + t.middle());

        assertEquals(10, result);
    }

    @Test
    public void testFilterTriPredicate() throws Exception {
        triple.set("test", 4, true);

        Optional<Triple<String, Integer, Boolean>> result = triple.filter((left, middle, right) -> left.length() == middle && right);
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter((left, middle, right) -> left.length() != middle);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFilterPredicate() throws Exception {
        triple.set("test", 4, true);

        Optional<Triple<String, Integer, Boolean>> result = triple.filter(t -> t.middle() > 0);
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter(t -> t.middle() < 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testToTuple() {
        triple.set("tuple", 42, false);
        Tuple3<String, Integer, Boolean> tuple = triple.toTuple();

        assertEquals("tuple", tuple._1);
        assertEquals(42, tuple._2);
        assertEquals(false, tuple._3);
    }

    @Test
    public void testHashCode() {
        Triple<String, Integer, Boolean> t1 = Triple.of("test", 123, true);
        Triple<String, Integer, Boolean> t2 = Triple.of("test", 123, true);
        Triple<String, Integer, Boolean> t3 = Triple.of("test", 124, true);
        Triple<String, Integer, Boolean> t4 = Triple.of("other", 123, true);
        Triple<String, Integer, Boolean> t5 = Triple.of("test", 123, false);

        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1.hashCode(), t4.hashCode());
        assertNotEquals(t1.hashCode(), t5.hashCode());

        Triple<String, Integer, Boolean> nullLeft = Triple.of(null, 123, true);
        Triple<String, Integer, Boolean> nullMiddle = Triple.of("test", null, true);
        Triple<String, Integer, Boolean> nullRight = Triple.of("test", 123, null);
        Triple<String, Integer, Boolean> allNull = Triple.of(null, null, null);

        assertDoesNotThrow(() -> nullLeft.hashCode());
        assertDoesNotThrow(() -> nullMiddle.hashCode());
        assertDoesNotThrow(() -> nullRight.hashCode());
        assertDoesNotThrow(() -> allNull.hashCode());
    }

    @Test
    public void testEquals() {
        Triple<String, Integer, Boolean> t1 = Triple.of("test", 123, true);
        Triple<String, Integer, Boolean> t2 = Triple.of("test", 123, true);
        Triple<String, Integer, Boolean> t3 = Triple.of("test", 124, true);
        Triple<String, Integer, Boolean> t4 = Triple.of("other", 123, true);
        Triple<String, Integer, Boolean> t5 = Triple.of("test", 123, false);

        assertEquals(t1, t1);

        assertEquals(t1, t2);
        assertEquals(t2, t1);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);

        assertNotEquals(t1, null);
        assertNotEquals(t1, "not a triple");

        Triple<String, Integer, Boolean> nullLeft1 = Triple.of(null, 123, true);
        Triple<String, Integer, Boolean> nullLeft2 = Triple.of(null, 123, true);
        Triple<String, Integer, Boolean> nullMiddle1 = Triple.of("test", null, true);
        Triple<String, Integer, Boolean> nullMiddle2 = Triple.of("test", null, true);
        Triple<String, Integer, Boolean> nullRight1 = Triple.of("test", 123, null);
        Triple<String, Integer, Boolean> nullRight2 = Triple.of("test", 123, null);

        assertEquals(nullLeft1, nullLeft2);
        assertEquals(nullMiddle1, nullMiddle2);
        assertEquals(nullRight1, nullRight2);
        assertNotEquals(nullLeft1, nullMiddle1);
        assertNotEquals(nullMiddle1, nullRight1);
    }

    @Test
    public void testToString() {
        triple.set("hello", 42, true);
        assertEquals("(hello, 42, true)", triple.toString());

        triple.set(null, null, null);
        assertEquals("(null, null, null)", triple.toString());

        triple.set("test", null, null);
        assertEquals("(test, null, null)", triple.toString());

        triple.set(null, 100, null);
        assertEquals("(null, 100, null)", triple.toString());

        triple.set(null, null, false);
        assertEquals("(null, null, false)", triple.toString());
    }

    @Test
    public void testMutableBehavior() {
        Triple<String, Integer, Boolean> t = Triple.of("initial", 1, true);

        t.setLeft("modified");
        t.setMiddle(2);
        t.setRight(false);

        assertEquals("modified", t.left());
        assertEquals(2, t.middle());
        assertEquals(false, t.right());
    }

    @Test
    public void testWithDifferentTypes() {
        Triple<Double, String, Character> tripleA = Triple.of(3.14, "pi", 'A');
        assertEquals(3.14, tripleA.left());
        assertEquals("pi", tripleA.middle());
        assertEquals('A', tripleA.right());

        Triple<Boolean, Character, Long> tripleB = Triple.of(true, 'B', 100L);
        assertEquals(true, tripleB.left());
        assertEquals('B', tripleB.middle());
        assertEquals(100L, tripleB.right());

        Triple<StringBuilder, Thread, Integer> customTriple = Triple.of(new StringBuilder("test"), Thread.currentThread(), 42);
        assertNotNull(customTriple.left());
        assertNotNull(customTriple.middle());
        assertNotNull(customTriple.right());
    }

    @Test
    public void testLeftMethod() {
        triple.setLeft("leftValue");
        assertEquals("leftValue", triple.left());
    }

    @Test
    public void testMiddleMethod() {
        triple.setMiddle(999);
        assertEquals(999, triple.middle());
    }

    @Test
    public void testRightMethod() {
        triple.setRight(false);
        assertEquals(false, triple.right());
    }

    @Test
    public void testSetLeft() {
        triple.setLeft("newLeft");
        assertEquals("newLeft", triple.left());

        triple.setLeft(null);
        assertNull(triple.left());
    }

    @Test
    public void testSetMiddle() {
        triple.setMiddle(555);
        assertEquals(555, triple.middle());

        triple.setMiddle(null);
        assertNull(triple.middle());
    }

    @Test
    public void testSetRight() {
        triple.setRight(true);
        assertEquals(true, triple.right());

        triple.setRight(null);
        assertNull(triple.right());
    }

    @Test
    public void testGetAndSetLeftWithNull() {
        triple.setLeft("initial");
        String oldValue = triple.getAndSetLeft(null);

        assertEquals("initial", oldValue);
        assertNull(triple.left());
    }

    @Test
    public void testGetAndSetMiddleWithNull() {
        triple.setMiddle(100);
        Integer oldValue = triple.getAndSetMiddle(null);

        assertEquals(100, oldValue);
        assertNull(triple.middle());
    }

    @Test
    public void testGetAndSetRightWithNull() {
        triple.setRight(true);
        Boolean oldValue = triple.getAndSetRight(null);

        assertEquals(true, oldValue);
        assertNull(triple.right());
    }

    @Test
    public void testSetAndGetLeftWithNull() {
        String newValue = triple.setAndGetLeft(null);

        assertNull(newValue);
        assertNull(triple.left());
    }

    @Test
    public void testSetAndGetMiddleWithNull() {
        Integer newValue = triple.setAndGetMiddle(null);

        assertNull(newValue);
        assertNull(triple.middle());
    }

    @Test
    public void testSetAndGetRightWithNull() {
        Boolean newValue = triple.setAndGetRight(null);

        assertNull(newValue);
        assertNull(triple.right());
    }
}
