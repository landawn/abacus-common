package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;

public class TripleTest extends TestBase {

    private Triple<String, Integer, Boolean> triple;

    @BeforeEach
    public void setUp() {
        triple = new Triple<>();
    }

    @Test
    public void testGetAndSet() {
        Triple<String, Integer, Boolean> triple = Triple.of("L", 1, true);

        String oldLeft = triple.getAndSetLeft("new L");
        assertEquals("L", oldLeft);
        assertEquals("new L", triple.left());
        String newLeft = triple.setAndGetLeft("final L");
        assertEquals("final L", newLeft);
        assertEquals("final L", triple.left());

        Integer oldMiddle = triple.getAndSetMiddle(2);
        assertEquals(1, oldMiddle);
        assertEquals(2, triple.middle());
        Integer newMiddle = triple.setAndGetMiddle(3);
        assertEquals(3, newMiddle);
        assertEquals(3, triple.middle());

        Boolean oldRight = triple.getAndSetRight(false);
        assertTrue(oldRight);
        assertFalse(triple.right());
        Boolean newRight = triple.setAndGetRight(true);
        assertTrue(newRight);
        assertTrue(triple.right());
    }

    // --- of ---

    @Test
    public void testOf() {
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
    public void testOf_withDifferentTypes() {
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

    @Test
    public void testConditionalSetters() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        assertTrue(triple.setLeftIf((l, m, r) -> m == 1, "B"));
        assertEquals("B", triple.left());
        assertEquals(1, triple.middle());
        assertTrue(triple.right());

        assertFalse(triple.setLeftIf((l, m, r) -> m == 0, "C"));
        assertEquals("B", triple.left());
        assertEquals(1, triple.middle());
        assertTrue(triple.right());

        assertTrue(triple.setMiddleIf((l, m, r) -> r, 2));
        assertEquals("B", triple.left());
        assertEquals(2, triple.middle());
        assertTrue(triple.right());

        assertFalse(triple.setMiddleIf((l, m, r) -> !r, 3));
        assertEquals("B", triple.left());
        assertEquals(2, triple.middle());
        assertTrue(triple.right());

        assertTrue(triple.setRightIf((l, m, r) -> "B".equals(l), false));
        assertEquals("B", triple.left());
        assertEquals(2, triple.middle());
        assertFalse(triple.right());

        assertTrue(triple.setIf((l, m, r) -> m < 5, "X", 10, true));
        assertEquals("X", triple.left());
        assertEquals(10, triple.middle());
        assertTrue(triple.right());

        assertTrue(triple.setIf((l, m, r) -> m > 5, "Y", 0, false));
        assertEquals("Y", triple.left());
        assertEquals(0, triple.middle());
        assertFalse(triple.right());
    }

    @Test
    public void testFunctionalMethods() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        List<Object> collected = new ArrayList<>();
        triple.forEach(collected::add);
        assertEquals(List.of("A", 1, true), collected);

        List<Object> triAccepted = new ArrayList<>();
        triple.accept((l, m, r) -> {
            triAccepted.add(l);
            triAccepted.add(m);
            triAccepted.add(r);
        });
        assertEquals(List.of("A", 1, true), triAccepted);

        final List<Triple<String, Integer, Boolean>> tripleAccepted = new ArrayList<>();
        triple.accept(tripleAccepted::add);
        assertEquals(1, tripleAccepted.size());
        assertSame(triple, tripleAccepted.get(0));

        String mapped1 = triple.map((l, m, r) -> l + m + r);
        assertEquals("A1true", mapped1);

        String mapped2 = triple.map(t -> t.left() + ":" + t.middle() + ":" + t.right());
        assertEquals("A:1:true", mapped2);

        Optional<Triple<String, Integer, Boolean>> filtered1 = triple.filter((l, m, r) -> r);
        assertTrue(filtered1.isPresent());
        assertSame(triple, filtered1.get());

        Optional<Triple<String, Integer, Boolean>> filtered2 = triple.filter((l, m, r) -> m < 0);
        assertFalse(filtered2.isPresent());

        Optional<Triple<String, Integer, Boolean>> filtered3 = triple.filter(t -> t.left().equals("A"));
        assertTrue(filtered3.isPresent());
    }

    // --- emptyArray ---

    @Test
    public void testEmptyArray() {
        Triple<String, Integer, Boolean>[] emptyArray = Triple.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);

        assertSame(emptyArray, Triple.emptyArray());
    }

    @Test
    public void testEmptyArray_DifferentTypeParams() {
        Triple<String, Integer, Boolean>[] arr1 = Triple.emptyArray();
        Triple<Double, Long, Character>[] arr2 = Triple.emptyArray();
        assertSame(arr1, arr2);
    }

    // --- left ---

    @Test
    public void testLeft() {
        triple.setLeft("leftValue");
        assertEquals("leftValue", triple.left());
    }

    // --- Constructor tests ---

    @Test
    public void testDefaultConstructor() {
        assertNull(triple.left());
        assertNull(triple.middle());
        assertNull(triple.right());
    }

    @Test
    public void testLeft_null() {
        assertNull(triple.left());
    }

    @Test
    public void testConstructorsAndFactories() {
        Triple<String, Integer, Boolean> t1 = new Triple<>();
        assertNull(t1.left());
        assertNull(t1.middle());
        assertNull(t1.right());

        Triple<String, Integer, Boolean> t2 = Triple.of("A", 1, true);
        assertEquals("A", t2.left());
        assertEquals(1, t2.middle());
        assertTrue(t2.right());
    }

    // --- middle ---

    @Test
    public void testMiddle() {
        triple.setMiddle(999);
        assertEquals(999, triple.middle());
    }

    @Test
    public void testMiddle_null() {
        assertNull(triple.middle());
    }

    // --- right ---

    @Test
    public void testRight() {
        triple.setRight(false);
        assertEquals(false, triple.right());
    }

    @Test
    public void testRight_null() {
        assertNull(triple.right());
    }

    // --- getLeft ---

    @Test
    public void testGetLeft() {
        triple.setLeft("test");
        assertEquals("test", triple.getLeft());
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

    // --- setLeft ---

    @Test
    public void testSetLeft() {
        triple.setLeft("newLeft");
        assertEquals("newLeft", triple.left());

        triple.setLeft(null);
        assertNull(triple.left());
    }

    // --- getMiddle ---

    @Test
    public void testGetMiddle() {
        triple.setMiddle(100);
        assertEquals(100, triple.getMiddle());
    }

    // --- setMiddle ---

    @Test
    public void testSetMiddle() {
        triple.setMiddle(555);
        assertEquals(555, triple.middle());

        triple.setMiddle(null);
        assertNull(triple.middle());
    }

    // --- getRight ---

    @Test
    public void testGetRight() {
        triple.setRight(false);
        assertEquals(false, triple.getRight());
    }

    // --- setRight ---

    @Test
    public void testSetRight() {
        triple.setRight(true);
        assertEquals(true, triple.right());

        triple.setRight(null);
        assertNull(triple.right());
    }

    // --- set ---

    @Test
    public void testSet() {
        triple.set("world", 456, false);
        assertEquals("world", triple.left());
        assertEquals(456, triple.middle());
        assertEquals(false, triple.right());
    }

    @Test
    public void testSet_nullValues() {
        triple.set(null, null, null);
        assertNull(triple.left());
        assertNull(triple.middle());
        assertNull(triple.right());
    }

    // --- getAndSetLeft ---

    @Test
    public void testGetAndSetLeft() {
        triple.setLeft("initial");
        String oldValue = triple.getAndSetLeft("new");

        assertEquals("initial", oldValue);
        assertEquals("new", triple.left());
    }

    @Test
    public void testGetAndSetLeft_withNull() {
        triple.setLeft("initial");
        String oldValue = triple.getAndSetLeft(null);

        assertEquals("initial", oldValue);
        assertNull(triple.left());
    }

    // --- setAndGetLeft ---

    @Test
    public void testSetAndGetLeft() {
        String newValue = triple.setAndGetLeft("setValue");

        assertEquals("setValue", newValue);
        assertEquals("setValue", triple.left());
    }

    @Test
    public void testSetAndGetLeft_withNull() {
        String newValue = triple.setAndGetLeft(null);

        assertNull(newValue);
        assertNull(triple.left());
    }

    // --- getAndSetMiddle ---

    @Test
    public void testGetAndSetMiddle() {
        triple.setMiddle(100);
        Integer oldValue = triple.getAndSetMiddle(200);

        assertEquals(100, oldValue);
        assertEquals(200, triple.middle());
    }

    @Test
    public void testGetAndSetMiddle_withNull() {
        triple.setMiddle(100);
        Integer oldValue = triple.getAndSetMiddle(null);

        assertEquals(100, oldValue);
        assertNull(triple.middle());
    }

    // --- setAndGetMiddle ---

    @Test
    public void testSetAndGetMiddle() {
        Integer newValue = triple.setAndGetMiddle(300);

        assertEquals(300, newValue);
        assertEquals(300, triple.middle());
    }

    @Test
    public void testSetAndGetMiddle_withNull() {
        Integer newValue = triple.setAndGetMiddle(null);

        assertNull(newValue);
        assertNull(triple.middle());
    }

    // --- getAndSetRight ---

    @Test
    public void testGetAndSetRight() {
        triple.setRight(true);
        Boolean oldValue = triple.getAndSetRight(false);

        assertEquals(true, oldValue);
        assertEquals(false, triple.right());
    }

    @Test
    public void testGetAndSetRight_withNull() {
        triple.setRight(true);
        Boolean oldValue = triple.getAndSetRight(null);

        assertEquals(true, oldValue);
        assertNull(triple.right());
    }

    // --- setAndGetRight ---

    @Test
    public void testSetAndGetRight() {
        Boolean newValue = triple.setAndGetRight(true);

        assertEquals(true, newValue);
        assertEquals(true, triple.right());
    }

    @Test
    public void testSetAndGetRight_withNull() {
        Boolean newValue = triple.setAndGetRight(null);

        assertNull(newValue);
        assertNull(triple.right());
    }

    // --- setLeftIf ---

    @Test
    public void testSetLeftIf() throws Exception {
        triple.setLeft("original");

        boolean result = triple.setLeftIf((l, m, r) -> "original".equals(l), "new");
        assertTrue(result);
        assertEquals("new", triple.left());

        result = triple.setLeftIf((l, m, r) -> "original".equals(l), "a");
        assertFalse(result);
        assertEquals("new", triple.left());
    }

    @Test
    public void testSetLeftIf_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.setLeftIf((l, m, r) -> {
                throw new RuntimeException("Test exception");
            }, "new");
        });
        assertEquals("test", triple.left());
    }

    // --- setMiddleIf ---

    @Test
    public void testSetMiddleIf() throws Exception {
        triple.setMiddle(10);

        boolean result = triple.setMiddleIf((l, m, r) -> m == 10, 20);
        assertTrue(result);
        assertEquals(20, triple.middle());

        result = triple.setMiddleIf((l, m, r) -> m == 10, 5);
        assertFalse(result);
        assertEquals(20, triple.middle());
    }

    @Test
    public void testSetMiddleIf_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.setMiddleIf((l, m, r) -> {
                throw new RuntimeException("Test exception");
            }, 100);
        });
        assertEquals(42, triple.middle());
    }

    // --- setRightIf ---

    @Test
    public void testSetRightIf() throws Exception {
        triple.setRight(true);

        boolean result = triple.setRightIf((l, m, r) -> r, false);
        assertTrue(result);
        assertEquals(false, triple.right());

        result = triple.setRightIf((l, m, r) -> r, true);
        assertFalse(result);
        assertEquals(false, triple.right());
    }

    @Test
    public void testSetRightIf_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.setRightIf((l, m, r) -> {
                throw new RuntimeException("Test exception");
            }, false);
        });
        assertEquals(true, triple.right());
    }

    // --- setIf ---

    @Test
    public void testSetIf() throws Exception {
        triple.set("old", 1, true);

        boolean result = triple.setIf((l, m, r) -> m > 0 && r, "new", 2, false);
        assertTrue(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(false, triple.right());

        result = triple.setIf((l, m, r) -> m > 0 && r, "newer", 0, true);
        assertFalse(result);
        assertEquals("new", triple.left());
        assertEquals(2, triple.middle());
        assertEquals(false, triple.right());
    }

    @Test
    public void testSetIf_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.setIf((l, m, r) -> {
                throw new RuntimeException("Test exception");
            }, "new", 100, false);
        });
        assertEquals("test", triple.left());
        assertEquals(42, triple.middle());
        assertEquals(true, triple.right());
    }

    // --- swap ---

    @Test
    public void testSwap() {
        triple.set("left", 42, true);
        Triple<Boolean, Integer, String> swapped = triple.swap();

        assertEquals(true, swapped.left());
        assertEquals(42, swapped.middle());
        assertEquals("left", swapped.right());

        // Original should be unchanged
        assertEquals("left", triple.left());
        assertEquals(42, triple.middle());
        assertEquals(true, triple.right());
    }

    @Test
    public void testSwap_thenSwapBack() {
        Triple<String, Integer, Boolean> original = Triple.of("left", 42, true);
        Triple<Boolean, Integer, String> swapped = original.swap();
        Triple<String, Integer, Boolean> swappedBack = swapped.swap();
        assertEquals(original, swappedBack);
    }

    @Test
    public void testSwap_withNullElements() {
        triple.set(null, null, null);
        Triple<Boolean, Integer, String> swapped = triple.swap();

        assertNull(swapped.left());
        assertNull(swapped.middle());
        assertNull(swapped.right());
    }

    // --- copy ---

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
    public void testCopy_withNullElements() {
        triple.set(null, null, null);
        Triple<String, Integer, Boolean> copy = triple.copy();

        assertNull(copy.left());
        assertNull(copy.middle());
        assertNull(copy.right());
        assertNotSame(triple, copy);
    }

    @Test
    public void testCopy_shallowCopy() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        Triple<String, List<Integer>, Boolean> original = Triple.of("Numbers", list, true);
        Triple<String, List<Integer>, Boolean> copy = original.copy();

        assertSame(original.middle(), copy.middle());

        copy.middle().add(2);
        assertEquals(2, original.middle().size());
    }

    // --- toArray() ---

    @Test
    public void testToArray() {
        triple.set("test", 123, false);
        Object[] array = triple.toArray();

        assertEquals(3, array.length);
        assertEquals("test", array[0]);
        assertEquals(123, array[1]);
        assertEquals(false, array[2]);
    }

    // --- toArray(A[]) ---

    @Test
    public void testToArray_withParameter() {
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
    public void testToArray_withNullElements() {
        triple.set(null, null, null);
        Object[] array = triple.toArray();
        assertEquals(3, array.length);
        assertNull(array[0]);
        assertNull(array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testForEach_withNullElements() {
        triple.set(null, null, null);
        List<Object> collected = new ArrayList<>();
        triple.forEach(collected::add);
        assertEquals(3, collected.size());
        assertNull(collected.get(0));
        assertNull(collected.get(1));
        assertNull(collected.get(2));
    }

    // --- forEach ---

    @Test
    public void testForEach() throws Exception {
        triple.set("value1", 2, true);
        StringBuilder sb = new StringBuilder();

        triple.forEach(obj -> sb.append(obj).append(","));

        assertEquals("value1,2,true,", sb.toString());
    }

    @Test
    public void testForEach_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.forEach(o -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    // --- accept(TriConsumer) ---

    @Test
    public void testAccept_triConsumer() throws Exception {
        triple.set("test", 100, false);
        StringBuilder sb = new StringBuilder();

        triple.accept((left, middle, right) -> sb.append(left).append(":").append(middle).append(":").append(right));

        assertEquals("test:100:false", sb.toString());
    }

    // --- accept(Consumer) ---

    @Test
    public void testAccept_consumer() throws Exception {
        triple.set("test", 100, true);
        StringBuilder sb = new StringBuilder();

        triple.accept(t -> sb.append(t.left()).append("-").append(t.middle()).append("-").append(t.right()));

        assertEquals("test-100-true", sb.toString());
    }

    @Test
    public void testAccept_triConsumer_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.accept((l, m, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testAccept_consumer_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.accept(t -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    // --- map(TriFunction) ---

    @Test
    public void testMap_triFunction() throws Exception {
        triple.set("Hello", 5, true);

        String result = triple.map((left, middle, right) -> left + " has " + middle + " letters: " + right);

        assertEquals("Hello has 5 letters: true", result);
    }

    // --- map(Function) ---

    @Test
    public void testMap_function() throws Exception {
        triple.set("Hello", 5, true);

        Integer result = triple.map(t -> t.left().length() + t.middle());

        assertEquals(10, result);
    }

    @Test
    public void testMap_triFunction_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.map((l, m, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testMap_function_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.map(t -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    // --- filter(TriPredicate) ---

    @Test
    public void testFilter_triPredicate() throws Exception {
        triple.set("test", 4, true);

        Optional<Triple<String, Integer, Boolean>> result = triple.filter((left, middle, right) -> left.length() == middle && right);
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter((left, middle, right) -> left.length() != middle);
        assertFalse(result.isPresent());
    }

    // --- filter(Predicate) ---

    @Test
    public void testFilter_predicate() throws Exception {
        triple.set("test", 4, true);

        Optional<Triple<String, Integer, Boolean>> result = triple.filter(t -> t.middle() > 0);
        assertTrue(result.isPresent());
        assertSame(triple, result.get());

        result = triple.filter(t -> t.middle() < 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFilter_triPredicate_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.filter((l, m, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testFilter_predicate_withException() {
        triple.set("test", 42, true);
        assertThrows(RuntimeException.class, () -> {
            triple.filter(t -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    // --- toTuple ---

    @Test
    public void testToTuple() {
        triple.set("tuple", 42, false);
        Tuple3<String, Integer, Boolean> tuple = triple.toTuple();

        assertEquals("tuple", tuple._1);
        assertEquals(42, tuple._2);
        assertEquals(false, tuple._3);
    }

    @Test
    public void testToTuple_withNulls() {
        triple.set(null, null, null);
        Tuple3<String, Integer, Boolean> tuple = triple.toTuple();
        assertNull(tuple._1);
        assertNull(tuple._2);
        assertNull(tuple._3);
    }

    // --- hashCode ---

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

    // --- equals ---

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
    public void testEqualsAndHashCode() {
        Triple<String, Integer, Boolean> t1 = Triple.of("A", 1, true);
        Triple<String, Integer, Boolean> t2 = Triple.of("A", 1, true);
        Triple<String, Integer, Boolean> t3 = Triple.of("B", 1, true);
        Triple<String, Integer, Boolean> t4 = Triple.of("A", 2, true);
        Triple<String, Integer, Boolean> t5 = Triple.of("A", 1, false);
        Triple<String, Integer, Boolean> t6 = Triple.of(null, null, null);
        Triple<String, Integer, Boolean> t7 = Triple.of(null, null, null);

        assertEquals(t1, t1);

        assertEquals(t1, t2);
        assertEquals(t2, t1);

        assertEquals(t1.hashCode(), t2.hashCode());
        assertEquals(t6.hashCode(), t7.hashCode());

        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1.hashCode(), t4.hashCode());
        assertNotEquals(t1.hashCode(), t5.hashCode());

        assertEquals(t6, t7);
        assertNotEquals(t1, t6);

        assertNotEquals("A", t1);
        assertNotEquals(null, t1);
    }

    // --- toString ---

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

    // --- Composite / integration tests ---

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
    public void testUtilityAndConversionMethods() {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        Triple<Boolean, Integer, String> reversed = triple.swap();
        assertEquals(true, reversed.left());
        assertEquals(1, reversed.middle());
        assertEquals("A", reversed.right());

        Triple<String, Integer, Boolean> copy = triple.copy();
        assertEquals(triple, copy);
        assertNotSame(triple, copy);

        Tuple.Tuple3<String, Integer, Boolean> tuple = triple.toTuple();
        assertEquals(Tuple.of("A", 1, true), tuple);
    }

}
