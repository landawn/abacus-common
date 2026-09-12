package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple3;

public class TripleTest extends TestBase {

    @Test
    public void testOfAndAccessors() {
        Triple<String, Integer, Boolean> triple = Triple.of("Hello", 42, true);
        assertEquals("Hello", triple.left());
        assertEquals("Hello", triple.getLeft());
        assertEquals(42, triple.middle());
        assertEquals(42, triple.getMiddle());
        assertEquals(true, triple.right());
        assertEquals(true, triple.getRight());

        Triple<String, Integer, Boolean> empty = new Triple<>();
        assertNull(empty.left());
        assertNull(empty.middle());
        assertNull(empty.right());

        Triple<String, Integer, Boolean> n = Triple.of(null, null, null);
        assertNull(n.left());
        assertNull(n.middle());
        assertNull(n.right());

        Triple<Double, String, Character> typed = Triple.of(3.14, "pi", 'A');
        assertEquals(3.14, typed.left());
        assertEquals("pi", typed.middle());
        assertEquals('A', typed.right());

        Triple<String, Integer, Double> zeros = Triple.of("", 0, 0.0);
        assertEquals("", zeros.left());
        assertEquals(0, zeros.middle());
        assertEquals(0.0, zeros.right());

        Triple<Integer, Integer, Integer> negative = Triple.of(-1, -2, -3);
        assertEquals(-1, negative.left());
        assertEquals(-2, negative.middle());
        assertEquals(-3, negative.right());

        Triple<Long, Double, Float> extreme = Triple.of(Long.MAX_VALUE, Double.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, extreme.left());
        assertEquals(Double.MAX_VALUE, extreme.middle());
        assertEquals(Float.MAX_VALUE, extreme.right());

        Triple<String, Integer, Boolean>[] a1 = Triple.emptyArray();
        Triple<Double, Long, Character>[] a2 = Triple.emptyArray();
        assertEquals(0, a1.length);
        assertSame(a1, a2);
    }

    @Test
    public void testGetAndSet() {
        Triple<String, Integer, Boolean> triple = Triple.of("old", 1, true);
        triple.setLeft("left");
        assertEquals("left", triple.left());
        triple.setMiddle(2);
        assertEquals(2, triple.middle());
        triple.setRight(false);
        assertEquals(false, triple.right());
        triple.set("final", 3, true);
        assertEquals("final", triple.left());
        assertEquals(3, triple.middle());
        assertEquals(true, triple.right());
        triple.set(null, null, null);
        assertNull(triple.left());
        assertNull(triple.middle());
        assertNull(triple.right());

        triple = Triple.of("left", 1, true);
        assertEquals("left", triple.getAndSetLeft("newLeft"));
        assertEquals("newLeft", triple.left());
        assertEquals("nextLeft", triple.setAndGetLeft("nextLeft"));
        assertEquals("nextLeft", triple.getAndSetLeft(null));
        assertNull(triple.left());
        assertNull(triple.setAndGetLeft(null));

        triple.set("x", 1, true);
        assertEquals(1, triple.getAndSetMiddle(2));
        assertEquals(2, triple.middle());
        assertEquals(3, triple.setAndGetMiddle(3));
        assertEquals(3, triple.getAndSetMiddle(null));
        assertNull(triple.middle());
        assertNull(triple.setAndGetMiddle(null));

        triple.set("x", 1, true);
        assertEquals(true, triple.getAndSetRight(false));
        assertEquals(false, triple.right());
        assertEquals(true, triple.setAndGetRight(true));
        assertEquals(true, triple.getAndSetRight(null));
        assertNull(triple.right());
        assertNull(triple.setAndGetRight(null));
    }

    @Test
    public void testSetIf() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("Hello", 10, true);
        assertTrue(triple.setLeftIf((l, m, r) -> m > 5, "World"));
        assertEquals("World", triple.left());
        assertFalse(triple.setLeftIf((l, m, r) -> m > 20, "Nope"));
        assertEquals("World", triple.left());
        assertTrue(triple.setMiddleIf((l, m, r) -> l.length() > 3, 20));
        assertEquals(20, triple.middle());
        assertFalse(triple.setMiddleIf((l, m, r) -> m < 0, 1));
        assertEquals(20, triple.middle());
        assertTrue(triple.setRightIf((l, m, r) -> r, false));
        assertEquals(false, triple.right());
        assertFalse(triple.setRightIf((l, m, r) -> r, true));
        assertEquals(false, triple.right());
        assertTrue(triple.setIf((l, m, r) -> m == 20, "final", 999, true));
        assertEquals("final", triple.left());
        assertEquals(999, triple.middle());
        assertEquals(true, triple.right());
        assertFalse(triple.setIf((l, m, r) -> m < 0, "x", 0, false));
        assertEquals("final", triple.left());
        assertEquals(999, triple.middle());
        assertEquals(true, triple.right());

        Triple<String, Integer, Boolean> frozen = Triple.of("Hello", 10, true);
        assertThrows(RuntimeException.class, () -> frozen.setLeftIf((l, m, r) -> {
            throw new RuntimeException("boom");
        }, "World"));
        assertThrows(RuntimeException.class, () -> frozen.setMiddleIf((l, m, r) -> {
            throw new RuntimeException("boom");
        }, 20));
        assertThrows(RuntimeException.class, () -> frozen.setRightIf((l, m, r) -> {
            throw new RuntimeException("boom");
        }, false));
        assertThrows(RuntimeException.class, () -> frozen.setIf((l, m, r) -> {
            throw new RuntimeException("boom");
        }, "World", 20, false));
        assertEquals("Hello", frozen.left());
        assertEquals(10, frozen.middle());
        assertEquals(true, frozen.right());
    }

    @Test
    public void testAcceptAndMap() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("A", 1, true);
        List<Object> collected = new ArrayList<>();
        triple.forEach(collected::add);
        assertEquals(List.of("A", 1, true), collected);
        Triple.of((String) null, (Integer) null, (Boolean) null).forEach(collected::add);
        assertNull(collected.get(3));
        assertNull(collected.get(4));
        assertNull(collected.get(5));
        assertThrows(RuntimeException.class, () -> triple.forEach(s -> {
            throw new RuntimeException("boom");
        }));

        List<Object> tri = new ArrayList<>();
        triple.accept((l, m, r) -> {
            tri.add(l);
            tri.add(m);
            tri.add(r);
        });
        assertEquals(List.of("A", 1, true), tri);
        List<Triple<String, Integer, Boolean>> accepted = new ArrayList<>();
        triple.accept(t -> accepted.add(t));
        assertSame(triple, accepted.get(0));
        assertThrows(RuntimeException.class, () -> triple.accept((l, m, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> triple.accept(t -> {
            throw new RuntimeException("boom");
        }));

        assertEquals("A1true", triple.map((l, m, r) -> l + m + r));
        assertEquals("A:1:true", triple.map(t -> t.left() + ":" + t.middle() + ":" + t.right()));
        assertEquals(Integer.valueOf(10), Triple.of("Hello", 5, true).map(t -> t.left().length() + t.middle()));
        assertThrows(RuntimeException.class, () -> triple.map((l, m, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> triple.map(t -> {
            throw new RuntimeException("boom");
        }));
    }

    @Test
    public void testFilter() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("test", 4, true);
        assertSame(triple, triple.filter((l, m, r) -> l.length() == m && r).get());
        assertFalse(triple.filter((l, m, r) -> l.length() != m).isPresent());
        assertSame(triple, triple.filter(t -> t.middle() > 0).get());
        assertFalse(triple.filter(t -> t.middle() < 0).isPresent());
        assertThrows(RuntimeException.class, () -> triple.filter((l, m, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> triple.filter(t -> {
            throw new RuntimeException("boom");
        }));
    }

    @Test
    public void testCopyAndConvert() {
        Triple<String, Integer, Boolean> original = Triple.of("Hello", 42, true);
        Triple<String, Integer, Boolean> copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        copy.setLeft("modified");
        assertEquals("Hello", original.left());
        assertEquals(Triple.of(null, null, null), Triple.of(null, null, null).copy());

        List<Integer> list = new ArrayList<>();
        list.add(1);
        Triple<String, List<Integer>, Boolean> shallow = Triple.of("Numbers", list, true).copy();
        assertSame(list, shallow.middle());
        shallow.middle().add(2);
        assertEquals(2, list.size());

        Object[] array = original.toArray();
        assertEquals(3, array.length);
        assertEquals("Hello", array[0]);
        assertEquals(42, array[1]);
        assertEquals(true, array[2]);
        assertNull(Triple.of(null, null, null).toArray()[0]);

        Triple<String, Integer, Boolean> values = Triple.of("test", 123, true);
        Object[] exact = values.toArray(new Object[3]);
        assertSame(exact, values.toArray(exact));
        assertEquals("test", exact[0]);
        assertEquals(123, exact[1]);
        assertEquals(true, exact[2]);
        Object[] grown = values.toArray(new Object[1]);
        assertEquals(3, grown.length);
        assertEquals("test", grown[0]);
        Object[] large = new Object[5];
        assertSame(large, values.toArray(large));
        assertEquals("test", large[0]);
        assertEquals(123, large[1]);
        assertEquals(true, large[2]);
        assertNull(Triple.of(null, null, null).toArray(new Object[3])[0]);

        Tuple3<String, Integer, Boolean> tuple = original.toTuple();
        assertEquals("Hello", tuple._1);
        assertEquals(42, tuple._2);
        assertEquals(true, tuple._3);
        Tuple3<String, Integer, Boolean> converted = Triple.of("A", 1, false).toTuple();
        assertEquals("A", converted._1);
        assertEquals(1, converted._2);
        assertEquals(false, converted._3);
        Tuple3<String, Integer, Boolean> nulls = Triple.<String, Integer, Boolean> of(null, null, null).toTuple();
        assertNull(nulls._1);
        assertNull(nulls._2);
        assertNull(nulls._3);
    }

    @Test
    public void testEqualsHashCodeToString() {
        Triple<String, Integer, Boolean> a = Triple.of("Hello", 42, true);
        Triple<String, Integer, Boolean> b = Triple.of("Hello", 42, true);
        Triple<String, Integer, Boolean> c = Triple.of("Hello", 43, true);
        Triple<String, Integer, Boolean> d = Triple.of("World", 42, true);
        Triple<String, Integer, Boolean> e = Triple.of("Hello", 42, false);
        Triple<String, Integer, Boolean> n1 = Triple.of(null, null, null);
        Triple<String, Integer, Boolean> n2 = Triple.of(null, null, null);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(n1, n2);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
        assertNotEquals(a, n1);
        assertNotEquals(a, "Hello");
        assertNotEquals(a, null);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(n1.hashCode(), n2.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertEquals("(Hello, 42, true)", a.toString());
        assertEquals("(null, null, null)", n1.toString());
        assertEquals("(test, null, null)", Triple.of("test", null, null).toString());
        assertEquals("(null, 100, null)", Triple.of(null, 100, null).toString());
        assertEquals("(null, null, false)", Triple.of(null, null, false).toString());

        Triple<String, Integer, Boolean> nullLeft = Triple.of(null, 123, true);
        Triple<String, Integer, Boolean> nullMiddle = Triple.of("test", null, true);
        Triple<String, Integer, Boolean> nullRight = Triple.of("test", 123, null);
        assertEquals(nullLeft, Triple.of(null, 123, true));
        assertEquals(nullMiddle, Triple.of("test", null, true));
        assertEquals(nullRight, Triple.of("test", 123, null));
        assertNotEquals(nullLeft, nullMiddle);
        assertNotEquals(nullMiddle, nullRight);
        nullLeft.hashCode();
        nullMiddle.hashCode();
        nullRight.hashCode();

        int[] nums = { 1, 2 };
        Object[] nested = { new int[] { 3, 4 } };
        Triple<int[], Object[], String> first = Triple.of(nums, nested, "value");
        Triple<int[], Object[], String> second = Triple.of(new int[] { 1, 2 }, new Object[] { new int[] { 3, 4 } }, "value");
        assertNotEquals(first, second);
        Triple<int[], Object[], String> sameRefs = Triple.of(nums, nested, "value");
        assertEquals(first, sameRefs);
        assertEquals(first.hashCode(), sameRefs.hashCode());
    }
}
