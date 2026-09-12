package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;

public class PairTest extends TestBase {

    @Test
    public void testOfAndAccessors() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertEquals("Hello", pair.left());
        assertEquals("Hello", pair.getLeft());
        assertEquals("Hello", pair.getKey());
        assertEquals(42, pair.right());
        assertEquals(42, pair.getRight());
        assertEquals(42, pair.getValue());

        Pair<String, Integer> empty = new Pair<>();
        assertNull(empty.left());
        assertNull(empty.right());

        Pair<String, Integer> n = Pair.of(null, null);
        assertNull(n.left());
        assertNull(n.right());
        assertNull(n.getKey());
        assertNull(n.getValue());

        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("age", 25);
        Pair<String, Integer> from = Pair.from(entry);
        assertEquals("age", from.left());
        assertEquals(25, from.right());

        Pair<String, Integer>[] a1 = Pair.emptyArray();
        Pair<String, Integer>[] a2 = Pair.emptyArray();
        assertEquals(0, a1.length);
        assertSame(a1, a2);
    }

    @Test
    public void testGetAndSet() {
        Pair<String, Integer> pair = Pair.of("old", 1);
        pair.setLeft("left");
        assertEquals("left", pair.left());
        pair.setRight(2);
        assertEquals(2, pair.right());
        pair.set("final", 3);
        assertEquals("final", pair.left());
        assertEquals(3, pair.right());
        pair.set(null, null);
        assertNull(pair.left());
        assertNull(pair.right());

        pair = Pair.of("left", 1);
        assertEquals("left", pair.getAndSetLeft("newLeft"));
        assertEquals("newLeft", pair.left());
        assertEquals("nextLeft", pair.setAndGetLeft("nextLeft"));
        assertEquals("nextLeft", pair.getAndSetLeft(null));
        assertNull(pair.left());
        assertNull(pair.setAndGetLeft(null));

        pair.set("x", 1);
        assertEquals(1, pair.getAndSetRight(2));
        assertEquals(2, pair.right());
        assertEquals(3, pair.setAndGetRight(3));
        assertEquals(3, pair.setValue(4));
        assertEquals(4, pair.right());
        assertEquals(4, pair.getAndSetRight(null));
        assertNull(pair.right());
        assertNull(pair.setAndGetRight(null));
        assertNull(pair.setValue(null));
    }

    @Test
    public void testSetIf() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        assertTrue(pair.setLeftIf((l, r) -> r > 5, "World"));
        assertEquals("World", pair.left());
        assertFalse(pair.setLeftIf((l, r) -> r > 20, "Nope"));
        assertEquals("World", pair.left());
        assertTrue(pair.setRightIf((l, r) -> l.length() > 3, 20));
        assertEquals(20, pair.right());
        assertFalse(pair.setRightIf((l, r) -> r < 0, 1));
        assertEquals(20, pair.right());
        assertTrue(pair.setIf((l, r) -> r == 20, "final", 999));
        assertEquals("final", pair.left());
        assertEquals(999, pair.right());
        assertFalse(pair.setIf((l, r) -> r < 0, "x", 0));
        assertEquals("final", pair.left());
        assertEquals(999, pair.right());

        Pair<String, Integer> frozen = Pair.of("Hello", 10);
        assertThrows(RuntimeException.class, () -> frozen.setLeftIf((l, r) -> {
            throw new RuntimeException("boom");
        }, "World"));
        assertThrows(RuntimeException.class, () -> frozen.setRightIf((l, r) -> {
            throw new RuntimeException("boom");
        }, 20));
        assertThrows(RuntimeException.class, () -> frozen.setIf((l, r) -> {
            throw new RuntimeException("boom");
        }, "World", 20));
        assertEquals("Hello", frozen.left());
        assertEquals(10, frozen.right());
    }

    @Test
    public void testAcceptAndMap() throws Exception {
        Pair<String, Integer> pair = Pair.of("A", 1);
        List<Object> collected = new ArrayList<>();
        pair.forEach(collected::add);
        assertEquals(List.of("A", 1), collected);
        Pair.of((String) null, (Integer) null).forEach(collected::add);
        assertNull(collected.get(2));
        assertNull(collected.get(3));
        assertThrows(RuntimeException.class, () -> pair.forEach(s -> {
            throw new RuntimeException("boom");
        }));

        List<Object> bi = new ArrayList<>();
        pair.accept((l, r) -> {
            bi.add(l);
            bi.add(r);
        });
        assertEquals(List.of("A", 1), bi);
        List<Pair<String, Integer>> accepted = new ArrayList<>();
        pair.accept(p -> accepted.add(p));
        assertSame(pair, accepted.get(0));
        assertThrows(RuntimeException.class, () -> pair.accept((l, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> pair.accept(p -> {
            throw new RuntimeException("boom");
        }));

        assertEquals("A1", pair.map((l, r) -> l + r));
        assertEquals("A:1", pair.map(p -> p.left() + ":" + p.right()));
        assertEquals(Integer.valueOf(200), Pair.of(10, 20).map((w, h) -> w * h));
        assertThrows(RuntimeException.class, () -> pair.map((l, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> pair.map(p -> {
            throw new RuntimeException("boom");
        }));
    }

    @Test
    public void testFilter() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 5);
        assertSame(pair, pair.filter((l, r) -> l.length() == r).get());
        assertFalse(pair.filter((l, r) -> r > 10).isPresent());
        assertSame(pair, pair.filter(p -> p.left().startsWith("H")).get());
        assertFalse(pair.filter(p -> p.right() < 0).isPresent());
        assertThrows(RuntimeException.class, () -> pair.filter((l, r) -> {
            throw new RuntimeException("boom");
        }));
        assertThrows(RuntimeException.class, () -> pair.filter(p -> {
            throw new RuntimeException("boom");
        }));
    }

    @Test
    public void testCopyAndConvert() {
        Pair<String, Integer> original = Pair.of("Hello", 42);
        Pair<String, Integer> copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(Pair.of(null, null), Pair.of(null, null).copy());

        List<Integer> list = new ArrayList<>();
        list.add(1);
        Pair<String, List<Integer>> shallow = Pair.of("Numbers", list).copy();
        assertSame(list, shallow.right());
        shallow.right().add(2);
        assertEquals(2, list.size());

        Object[] array = original.toArray();
        assertEquals(2, array.length);
        assertEquals("Hello", array[0]);
        assertEquals(42, array[1]);
        assertNull(Pair.of(null, null).toArray()[0]);

        Pair<String, String> strings = Pair.of("Hello", "World");
        String[] exact = strings.toArray(new String[2]);
        assertSame(exact, strings.toArray(exact));
        assertEquals("Hello", exact[0]);
        assertEquals("World", exact[1]);
        String[] grown = strings.toArray(new String[1]);
        assertEquals(2, grown.length);
        assertEquals("Hello", grown[0]);
        String[] large = new String[3];
        assertSame(large, strings.toArray(large));
        assertEquals("Hello", large[0]);
        assertEquals("World", large[1]);
        assertNull(large[2]);
        assertNull(Pair.of((String) null, (String) null).toArray(new String[2])[0]);

        Tuple2<String, Integer> tuple = original.toTuple();
        assertEquals("Hello", tuple._1);
        assertEquals(42, tuple._2);
        assertEquals(Tuple.of("A", 1), Pair.of("A", 1).toTuple());

        ImmutableEntry<String, Integer> entry = original.toImmutableEntry();
        assertEquals("Hello", entry.getKey());
        assertEquals(42, entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(1));
        assertNull(Pair.of(null, null).toImmutableEntry().getKey());
    }

    @Test
    public void testEqualsHashCodeToString() {
        Pair<String, Integer> a = Pair.of("Hello", 42);
        Pair<String, Integer> b = Pair.of("Hello", 42);
        Pair<String, Integer> c = Pair.of("Hello", 43);
        Pair<String, Integer> d = Pair.of("World", 42);
        Pair<String, Integer> n1 = Pair.of(null, null);
        Pair<String, Integer> n2 = Pair.of(null, null);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(n1, n2);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, n1);
        assertNotEquals(a, "Hello");
        assertNotEquals(a, null);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(n1.hashCode(), n2.hashCode());
        assertEquals("(Hello, 42)", a.toString());
        assertEquals("(null, World)", Pair.of(null, "World").toString());
        assertEquals("(null, null)", n1.toString());

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("Hello", 42);
        assertTrue(a.equals(entry));
        assertTrue(entry.equals(a));
        assertEquals(entry.hashCode(), a.hashCode());
        assertFalse(a.equals(new AbstractMap.SimpleEntry<>("Hello", 99)));

        int[] key = { 1, 2 };
        Pair<int[], String> pair = Pair.of(key, "value");
        Map.Entry<int[], String> sameRef = new AbstractMap.SimpleEntry<>(key, "value");
        Map.Entry<int[], String> equalContent = new AbstractMap.SimpleEntry<>(new int[] { 1, 2 }, "value");
        assertEquals(sameRef, pair);
        assertEquals(pair, sameRef);
        assertEquals(sameRef.hashCode(), pair.hashCode());
        assertNotEquals(equalContent, pair);
        assertNotEquals(pair, equalContent);
    }
}
