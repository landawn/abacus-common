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
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

/**
 * Unit tests for the Triple class.
 */
public class Triple200Test extends TestBase {

    @Test
    public void testConstructorsAndFactories() {
        // Default constructor
        Triple<String, Integer, Boolean> t1 = new Triple<>();
        assertNull(t1.left());
        assertNull(t1.middle());
        assertNull(t1.right());

        // of(l, m, r) factory
        Triple<String, Integer, Boolean> t2 = Triple.of("A", 1, true);
        assertEquals("A", t2.left());
        assertEquals(1, t2.middle());
        assertTrue(t2.right());
    }

    @Test
    public void testEmptyArray() {
        Triple<String, Integer, Boolean>[] emptyArray = Triple.emptyArray();
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testGettersAndSetters() {
        Triple<String, Integer, Boolean> triple = Triple.of("L", 1, true);

        // Getters
        assertEquals("L", triple.left());
        assertEquals(1, triple.middle());
        assertTrue(triple.right());

        // Setters
        triple.setLeft("New L");
        assertEquals("New L", triple.left());

        triple.setMiddle(2);
        assertEquals(2, triple.middle());

        triple.setRight(false);
        assertFalse(triple.right());

        // set(l, m, r)
        triple.set("Final L", 3, true);
        assertEquals("Final L", triple.left());
        assertEquals(3, triple.middle());
        assertTrue(triple.right());
    }

    @Test
    public void testGetAndSet() {
        Triple<String, Integer, Boolean> triple = Triple.of("L", 1, true);

        // Left
        String oldLeft = triple.getAndSetLeft("new L");
        assertEquals("L", oldLeft);
        assertEquals("new L", triple.left());
        String newLeft = triple.setAndGetLeft("final L");
        assertEquals("final L", newLeft);
        assertEquals("final L", triple.left());

        // Middle
        Integer oldMiddle = triple.getAndSetMiddle(2);
        assertEquals(1, oldMiddle);
        assertEquals(2, triple.middle());
        Integer newMiddle = triple.setAndGetMiddle(3);
        assertEquals(3, newMiddle);
        assertEquals(3, triple.middle());

        // Right
        Boolean oldRight = triple.getAndSetRight(false);
        assertTrue(oldRight);
        assertFalse(triple.right());
        Boolean newRight = triple.setAndGetRight(true);
        assertTrue(newRight);
        assertTrue(triple.right());
    }

    @Test
    public void testConditionalSetters() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        // setLeftIf
        assertTrue(triple.setLeftIf("B", (t, l) -> t.middle() == 1));
        assertEquals("B", triple.left());
        assertFalse(triple.setLeftIf("C", (t, l) -> t.middle() == 0));
        assertEquals("B", triple.left());

        // setMiddleIf
        assertTrue(triple.setMiddleIf(2, (t, m) -> t.right()));
        assertEquals(2, triple.middle());
        assertFalse(triple.setMiddleIf(3, (t, m) -> !t.right()));
        assertEquals(2, triple.middle());

        // setRightIf
        assertTrue(triple.setRightIf(false, (t, r) -> t.left().equals("B")));
        assertFalse(triple.right());

        // setIf
        assertTrue(triple.setIf("X", 10, true, (t, l, m, r) -> m > 5));
        assertEquals("X", triple.left());
        assertEquals(10, triple.middle());
        assertTrue(triple.right());

        assertTrue(triple.setIf("Y", 0, false, (t, l, m, r) -> m < 5));
        assertEquals("Y", triple.left());
    }

    @Test
    public void testUtilityAndConversionMethods() {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        // reverse
        Triple<Boolean, Integer, String> reversed = triple.reverse();
        assertEquals(true, reversed.left());
        assertEquals(1, reversed.middle());
        assertEquals("A", reversed.right());

        // copy
        Triple<String, Integer, Boolean> copy = triple.copy();
        assertEquals(triple, copy);
        assertNotSame(triple, copy);

        // toTuple
        Tuple.Tuple3<String, Integer, Boolean> tuple = triple.toTuple();
        assertEquals(Tuple.of("A", 1, true), tuple);
    }

    @Test
    public void testToArray() {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        // toArray()
        Object[] objArray = triple.toArray();
        assertArrayEquals(new Object[] { "A", 1, true }, objArray);

        // toArray(A[]) with sufficient size
        Object[] sizedArray = new Object[3];
        triple.toArray(sizedArray);
        assertArrayEquals(new Object[] { "A", 1, true }, sizedArray);

        // toArray(A[]) with insufficient size
        Object[] smallArray = new Object[1];
        Object[] newArray = triple.toArray(smallArray);
        assertNotSame(smallArray, newArray);
        assertEquals(3, newArray.length);
        assertArrayEquals(new Object[] { "A", 1, true }, newArray);
    }

    @Test
    public void testFunctionalMethods() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);

        // forEach
        List<Object> collected = new ArrayList<>();
        triple.forEach(collected::add);
        assertEquals(List.of("A", 1, true), collected);

        // accept(TriConsumer)
        List<Object> triAccepted = new ArrayList<>();
        triple.accept((l, m, r) -> {
            triAccepted.add(l);
            triAccepted.add(m);
            triAccepted.add(r);
        });
        assertEquals(List.of("A", 1, true), triAccepted);

        // accept(Consumer)
        final List<Triple<String, Integer, Boolean>> tripleAccepted = new ArrayList<>();
        triple.accept(tripleAccepted::add);
        assertEquals(1, tripleAccepted.size());
        assertSame(triple, tripleAccepted.get(0));

        // map(TriFunction)
        String mapped1 = triple.map((l, m, r) -> l + m + r);
        assertEquals("A1true", mapped1);

        // map(Function)
        String mapped2 = triple.map(t -> t.left() + ":" + t.middle() + ":" + t.right());
        assertEquals("A:1:true", mapped2);

        // filter(TriPredicate)
        Optional<Triple<String, Integer, Boolean>> filtered1 = triple.filter((l, m, r) -> r);
        assertTrue(filtered1.isPresent());
        assertSame(triple, filtered1.get());

        Optional<Triple<String, Integer, Boolean>> filtered2 = triple.filter((l, m, r) -> m < 0);
        assertFalse(filtered2.isPresent());

        // filter(Predicate)
        Optional<Triple<String, Integer, Boolean>> filtered3 = triple.filter(t -> t.left().equals("A"));
        assertTrue(filtered3.isPresent());
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

        // Reflexive
        assertEquals(t1, t1);

        // Symmetric
        assertEquals(t1, t2);
        assertEquals(t2, t1);

        // HashCode
        assertEquals(t1.hashCode(), t2.hashCode());
        assertEquals(t6.hashCode(), t7.hashCode());

        // Unequal
        assertNotEquals(t1, t3);
        assertNotEquals(t1, t4);
        assertNotEquals(t1, t5);
        assertNotEquals(t1.hashCode(), t3.hashCode());
        assertNotEquals(t1.hashCode(), t4.hashCode());
        assertNotEquals(t1.hashCode(), t5.hashCode());

        // Nulls
        assertEquals(t6, t7);
        assertNotEquals(t1, t6);

        // Other types
        assertNotEquals("A", t1);
        assertNotEquals(null, t1);
    }

    @Test
    public void testToString() {
        Triple<String, Integer, Boolean> triple = Triple.of("hello", 123, false);
        assertEquals("(hello, 123, false)", triple.toString());

        Triple<String, Integer, Boolean> tripleWithNull = Triple.of("hello", null, true);
        assertEquals("(hello, null, true)", tripleWithNull.toString());
    }
}
