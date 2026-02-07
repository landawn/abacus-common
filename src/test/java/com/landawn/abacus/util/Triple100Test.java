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

@Tag("new-test")
public class Triple100Test extends TestBase {

    private Triple<String, Integer, Double> triple;

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
        Triple<String, Integer, Double> t = Triple.of("test", 42, 3.14);
        assertEquals("test", t.left());
        assertEquals(42, t.middle());
        assertEquals(3.14, t.right());

        Triple<String, Integer, Double> nullTriple = Triple.of(null, null, null);
        assertNull(nullTriple.left());
        assertNull(nullTriple.middle());
        assertNull(nullTriple.right());
    }

    @Test
    public void testEmptyArray() {
        Triple<String, Integer, Double>[] emptyArray = Triple.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);

        assertSame(emptyArray, Triple.emptyArray());
    }

    @Test
    public void testGettersAndSetters() {
        triple.setLeft("hello");
        triple.setMiddle(123);
        triple.setRight(45.67);

        assertEquals("hello", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(45.67, triple.right());

        assertEquals("hello", triple.left());
        assertEquals(123, triple.middle());
        assertEquals(45.67, triple.right());
    }

    @Test
    public void testSetMethod() {
        triple.set("world", 456, 78.9);
        assertEquals("world", triple.left());
        assertEquals(456, triple.middle());
        assertEquals(78.9, triple.right());
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
        triple.setRight(1.1);
        Double oldValue = triple.getAndSetRight(2.2);

        assertEquals(1.1, oldValue);
        assertEquals(2.2, triple.right());
    }

    @Test
    public void testSetAndGetRight() {
        Double newValue = triple.setAndGetRight(3.3);

        assertEquals(3.3, newValue);
        assertEquals(3.3, triple.right());
    }

    @Test
    public void testSetLeftIf() throws Exception {
        // Assume triple is Triple<String, Integer, Double>
        triple.set("original", 1, 0.1);

        // Predicate uses current (left, middle, right)
        boolean result = triple.setLeftIf((l, m, r) -> l.equals("original"), "new");
        assertTrue(result);
        assertEquals("new", triple.left());
        assertEquals(1, triple.middle());
        assertEquals(0.1, triple.right(), 0.0);

        // Now left is "new", so predicate should be false
        result = triple.setLeftIf((l, m, r) -> l.equals("original"), "another");
        assertFalse(result);
        assertEquals("new", triple.left());
        assertEquals(1, triple.middle());
        assertEquals(0.1, triple.right(), 0.0);
    }

    @Test
    public void testSetMiddleIf() throws Exception {
        triple.set("left", 10, 0.1);

        boolean result = triple.setMiddleIf((l, m, r) -> m == 10, 20);
        assertTrue(result);
        assertEquals("left", triple.left());
        assertEquals(20, triple.middle());
        assertEquals(0.1, triple.right(), 0.0);

        // Now middle is 20, so this should not update
        result = triple.setMiddleIf((l, m, r) -> m == 10, 5);
        assertFalse(result);
        assertEquals("left", triple.left());
        assertEquals(20, triple.middle());
        assertEquals(0.1, triple.right(), 0.0);
    }

    @Test
    public void testSetRightIf() throws Exception {
        triple.set("left", 1, 1.0);

        boolean result = triple.setRightIf((l, m, r) -> r == 1.0, 2.0);
        assertTrue(result);
        assertEquals("left", triple.left());
        assertEquals(1, triple.middle());
        assertEquals(2.0, triple.right(), 0.0);

        // Now right is 2.0, so this should not update
        result = triple.setRightIf((l, m, r) -> r == 1.0, 0.5);
        assertFalse(result);
        assertEquals("left", triple.left());
        assertEquals(1, triple.middle());
        assertEquals(2.0, triple.right(), 0.0);
    }

    @Test
    public void testSetIf() throws Exception {
        triple.set("old", 1, 0.1);

        boolean result = triple.setIf((l, m, r) -> m == 1 && r == 0.1, "new", 2, 0.2);
        assertTrue(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(0.2, triple.right(), 0.0);

        // Now middle/right changed, so predicate should fail
        result = triple.setIf((l, m, r) -> m == 1 && r == 0.1, "newer", 0, 0.0);
        assertFalse(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(0.2, triple.right(), 0.0);
    }

    @Test
    public void testReverse() {
        triple.set("left", 100, 99.9);
        Triple<Double, Integer, String> reversed = triple.swap();

        assertEquals(99.9, reversed.left());
        assertEquals(100, reversed.middle());
        assertEquals("left", reversed.right());

        assertEquals("left", triple.left());
        assertEquals(100, triple.middle());
        assertEquals(99.9, triple.right());
    }

    @Test
    public void testCopy() {
        triple.set("original", 42, 3.14);
        Triple<String, Integer, Double> copy = triple.copy();

        assertEquals(triple.left(), copy.left());
        assertEquals(triple.middle(), copy.middle());
        assertEquals(triple.right(), copy.right());
        assertNotSame(triple, copy);

        copy.setLeft("modified");
        assertEquals("original", triple.left());
    }

    @Test
    public void testToArray() {
        triple.set("test", 123, 4.56);
        Object[] array = triple.toArray();

        assertEquals(3, array.length);
        assertEquals("test", array[0]);
        assertEquals(123, array[1]);
        assertEquals(4.56, array[2]);
    }

    @Test
    public void testToArrayWithParameter() {
        triple.set("test", 123, 4.56);

        Object[] exactArray = new Object[3];
        Object[] result = triple.toArray(exactArray);
        assertSame(exactArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(4.56, result[2]);

        Object[] smallArray = new Object[2];
        result = triple.toArray(smallArray);
        assertNotSame(smallArray, result);
        assertEquals(3, result.length);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(4.56, result[2]);

        Object[] largeArray = new Object[5];
        result = triple.toArray(largeArray);
        assertSame(largeArray, result);
        assertEquals("test", result[0]);
        assertEquals(123, result[1]);
        assertEquals(4.56, result[2]);
    }

    @Test
    public void testForEach() throws Exception {
        triple.set("value1", 2, 3.0);
        StringBuilder sb = new StringBuilder();

        triple.forEach(obj -> sb.append(obj).append(","));

        assertEquals("value1,2,3.0,", sb.toString());
    }

    @Test
    public void testAcceptTriConsumer() throws Exception {
        triple.set("test", 100, 2.5);
        StringBuilder sb = new StringBuilder();

        triple.accept((left, middle, right) -> sb.append(left).append(":").append(middle).append(":").append(right));

        assertEquals("test:100:2.5", sb.toString());
    }

    @Test
    public void testAcceptConsumer() throws Exception {
        triple.set("test", 100, 2.5);
        StringBuilder sb = new StringBuilder();

        triple.accept(t -> sb.append(t.left()).append("-").append(t.middle()).append("-").append(t.right()));

        assertEquals("test-100-2.5", sb.toString());
    }

    @Test
    public void testMapTriFunction() throws Exception {
        triple.set("Hello", 5, 2.0);

        String result = triple.map((left, middle, right) -> left + " has " + middle + " letters times " + right);

        assertEquals("Hello has 5 letters times 2.0", result);
    }

    @Test
    public void testMapFunction() throws Exception {
        triple.set("Hello", 5, 2.0);

        Double result = triple.map(t -> t.left().length() + t.middle() + t.right());

        assertEquals(12.0, result);
    }

    @Test
    public void testFilterTriPredicate() throws Exception {
        triple.set("test", 4, 4.0);

        Optional<Triple<String, Integer, Double>> result = triple.filter((left, middle, right) -> left.length() == middle && middle.equals(right.intValue()));
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter((left, middle, right) -> left.length() != middle);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFilterPredicate() throws Exception {
        triple.set("test", 4, 1.0);

        Optional<Triple<String, Integer, Double>> result = triple.filter(t -> t.middle() > 0 && t.right() > 0);
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter(t -> t.middle() < 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testToTuple() {
        triple.set("tuple", 42, 3.14);
        Tuple3<String, Integer, Double> tuple = triple.toTuple();

        assertEquals("tuple", tuple._1);
        assertEquals(42, tuple._2);
        assertEquals(3.14, tuple._3);
    }

    @Test
    public void testHashCode() {
        Triple<String, Integer, Double> t1 = Triple.of("test", 123, 4.5);
        Triple<String, Integer, Double> t2 = Triple.of("test", 123, 4.5);
        Triple<String, Integer, Double> t3 = Triple.of("test", 123, 4.6);
        Triple<String, Integer, Double> t4 = Triple.of("test", 124, 4.5);
        Triple<String, Integer, Double> t5 = Triple.of("other", 123, 4.5);

        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1.hashCode(), t4.hashCode());
        assertNotEquals(t1.hashCode(), t5.hashCode());

        Triple<String, Integer, Double> nullLeft = Triple.of(null, 123, 4.5);
        Triple<String, Integer, Double> nullMiddle = Triple.of("test", null, 4.5);
        Triple<String, Integer, Double> nullRight = Triple.of("test", 123, null);
        Triple<String, Integer, Double> allNull = Triple.of(null, null, null);

        assertDoesNotThrow(() -> nullLeft.hashCode());
        assertDoesNotThrow(() -> nullMiddle.hashCode());
        assertDoesNotThrow(() -> nullRight.hashCode());
        assertDoesNotThrow(() -> allNull.hashCode());
    }

    @Test
    public void testEquals() {
        Triple<String, Integer, Double> t1 = Triple.of("test", 123, 4.5);
        Triple<String, Integer, Double> t2 = Triple.of("test", 123, 4.5);
        Triple<String, Integer, Double> t3 = Triple.of("test", 123, 4.6);
        Triple<String, Integer, Double> t4 = Triple.of("test", 124, 4.5);
        Triple<String, Integer, Double> t5 = Triple.of("other", 123, 4.5);

        assertEquals(t1, t1);

        assertEquals(t1, t2);
        assertEquals(t2, t1);

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);

        assertNotEquals(t1, null);
        assertNotEquals(t1, "not a triple");

        Triple<String, Integer, Double> nullLeft1 = Triple.of(null, 123, 4.5);
        Triple<String, Integer, Double> nullLeft2 = Triple.of(null, 123, 4.5);
        Triple<String, Integer, Double> nullMiddle1 = Triple.of("test", null, 4.5);
        Triple<String, Integer, Double> nullMiddle2 = Triple.of("test", null, 4.5);
        Triple<String, Integer, Double> nullRight1 = Triple.of("test", 123, null);
        Triple<String, Integer, Double> nullRight2 = Triple.of("test", 123, null);

        assertEquals(nullLeft1, nullLeft2);
        assertEquals(nullMiddle1, nullMiddle2);
        assertEquals(nullRight1, nullRight2);
        assertNotEquals(nullLeft1, nullMiddle1);
        assertNotEquals(nullMiddle1, nullRight1);
    }

    @Test
    public void testToString() {
        triple.set("hello", 42, 3.14);
        assertEquals("(hello, 42, 3.14)", triple.toString());

        triple.set(null, null, null);
        assertEquals("(null, null, null)", triple.toString());

        triple.set("test", null, 1.0);
        assertEquals("(test, null, 1.0)", triple.toString());

        triple.set(null, 100, null);
        assertEquals("(null, 100, null)", triple.toString());
    }

    @Test
    public void testMutableBehavior() {
        Triple<String, Integer, Double> t = Triple.of("initial", 1, 1.0);

        t.setLeft("modified");
        t.setMiddle(2);
        t.setRight(2.0);

        assertEquals("modified", t.left());
        assertEquals(2, t.middle());
        assertEquals(2.0, t.right());
    }

    @Test
    public void testWithDifferentTypes() {
        Triple<Boolean, Character, Long> boolCharLong = Triple.of(true, 'A', 100L);
        assertEquals(true, boolCharLong.left());
        assertEquals('A', boolCharLong.middle());
        assertEquals(100L, boolCharLong.right());

        Triple<Byte, Short, Float> numericTriple = Triple.of((byte) 1, (short) 2, 3.0f);
        assertEquals((byte) 1, numericTriple.left());
        assertEquals((short) 2, numericTriple.middle());
        assertEquals(3.0f, numericTriple.right());

        Triple<StringBuilder, Thread, Exception> customTriple = Triple.of(new StringBuilder("test"), Thread.currentThread(), new RuntimeException("test"));
        assertNotNull(customTriple.left());
        assertNotNull(customTriple.middle());
        assertNotNull(customTriple.right());
    }

    @Test
    public void testComplexPredicateScenarios() throws Exception {
        // Assume triple is Triple<String, Integer, Double>
        triple.set("abc", 3, 3.0);

        boolean result = triple.setIf((l, m, r) -> l.length() == m && m == r.intValue() && "defgh".length() > l.length(), "defgh", 5, 5.0);
        // For the initial state: "abc", 3, 3.0
        // l.length() == 3, m == 3, r.intValue() == 3, and "defgh".length() == 5 > 3
        // => predicate is true, so the update should happen.

        assertTrue(result);
        assertEquals("defgh", triple.left());
        assertEquals(5, triple.middle());
        assertEquals(5.0, triple.right(), 0.0);
    }

    @Test
    public void testEdgeCases() {
        Triple<String, Integer, Double> emptyStringTriple = Triple.of("", 0, 0.0);
        assertEquals("", emptyStringTriple.left());
        assertEquals(0, emptyStringTriple.middle());
        assertEquals(0.0, emptyStringTriple.right());

        Triple<Integer, Integer, Integer> negativeTriple = Triple.of(-1, -2, -3);
        assertEquals(-1, negativeTriple.left());
        assertEquals(-2, negativeTriple.middle());
        assertEquals(-3, negativeTriple.right());

        Triple<Long, Double, Float> extremeTriple = Triple.of(Long.MAX_VALUE, Double.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, extremeTriple.left());
        assertEquals(Double.MAX_VALUE, extremeTriple.middle());
        assertEquals(Float.MAX_VALUE, extremeTriple.right());
    }
}
