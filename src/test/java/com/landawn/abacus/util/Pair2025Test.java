package com.landawn.abacus.util;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class Pair2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        Pair<String, Integer> pair = new Pair<>();
        assertNull(pair.left());
        assertNull(pair.right());
    }

    @Test
    public void testOf() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertEquals("Hello", pair.left());
        assertEquals(42, pair.right());
    }

    @Test
    public void testOf_withNullValues() {
        Pair<String, Integer> pair = Pair.of(null, null);
        assertNull(pair.left());
        assertNull(pair.right());
    }

    @Test
    public void testCreate_fromMapEntry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("age", 25);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        Pair<String, Integer> pair = Pair.create(entry);
        assertEquals("age", pair.left());
        assertEquals(25, pair.right());
    }

    @Test
    public void testEmptyArray() {
        Pair<String, Integer>[] array = Pair.emptyArray();
        assertNotNull(array);
        assertEquals(0, array.length);
    }

    @Test
    public void testLeft() {
        Pair<String, Integer> pair = Pair.of("Test", 100);
        assertEquals("Test", pair.left());
    }

    @Test
    public void testLeft_withNull() {
        Pair<String, Integer> pair = Pair.of(null, 100);
        assertNull(pair.left());
    }

    @Test
    public void testRight() {
        Pair<String, Integer> pair = Pair.of("Test", 100);
        assertEquals(100, pair.right());
    }

    @Test
    public void testRight_withNull() {
        Pair<String, Integer> pair = Pair.of("Test", null);
        assertNull(pair.right());
    }

    @Test
    public void testGetLeft() {
        Pair<String, Integer> pair = Pair.of("Deprecated", 50);
        assertEquals("Deprecated", pair.getLeft());
    }

    @Test
    public void testSetLeft() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        pair.setLeft("New");
        assertEquals("New", pair.left());
    }

    @Test
    public void testSetLeft_withNull() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        pair.setLeft(null);
        assertNull(pair.left());
    }

    @Test
    public void testGetRight() {
        Pair<String, Integer> pair = Pair.of("Test", 75);
        assertEquals(75, pair.getRight());
    }

    @Test
    public void testSetRight() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        pair.setRight(20);
        assertEquals(20, pair.right());
    }

    @Test
    public void testSetRight_withNull() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        pair.setRight(null);
        assertNull(pair.right());
    }

    @Test
    public void testSet() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        pair.set("New", 20);
        assertEquals("New", pair.left());
        assertEquals(20, pair.right());
    }

    @Test
    public void testSet_withNullValues() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        pair.set(null, null);
        assertNull(pair.left());
        assertNull(pair.right());
    }

    @Test
    public void testGetAndSetLeft() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        String oldLeft = pair.getAndSetLeft("New");
        assertEquals("Old", oldLeft);
        assertEquals("New", pair.left());
    }

    @Test
    public void testGetAndSetLeft_withNull() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        String oldLeft = pair.getAndSetLeft(null);
        assertEquals("Old", oldLeft);
        assertNull(pair.left());
    }

    @Test
    public void testSetAndGetLeft() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        String newLeft = pair.setAndGetLeft("New");
        assertEquals("New", newLeft);
        assertEquals("New", pair.left());
    }

    @Test
    public void testSetAndGetLeft_withNull() {
        Pair<String, Integer> pair = Pair.of("Old", 10);
        String newLeft = pair.setAndGetLeft(null);
        assertNull(newLeft);
        assertNull(pair.left());
    }

    @Test
    public void testGetAndSetRight() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        Integer oldRight = pair.getAndSetRight(20);
        assertEquals(10, oldRight);
        assertEquals(20, pair.right());
    }

    @Test
    public void testGetAndSetRight_withNull() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        Integer oldRight = pair.getAndSetRight(null);
        assertEquals(10, oldRight);
        assertNull(pair.right());
    }

    @Test
    public void testSetAndGetRight() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        Integer newRight = pair.setAndGetRight(20);
        assertEquals(20, newRight);
        assertEquals(20, pair.right());
    }

    @Test
    public void testSetAndGetRight_withNull() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        Integer newRight = pair.setAndGetRight(null);
        assertNull(newRight);
        assertNull(pair.right());
    }

    @Test
    public void testSetLeftIf_predicateTrue() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        // Predicate is evaluated on current (left, right)
        boolean updated = pair.setLeftIf((l, r) -> r > 5, "World");

        assertTrue(updated);
        assertEquals("World", pair.left());
    }

    @Test
    public void testSetLeftIf_predicateFalse() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        boolean updated = pair.setLeftIf((l, r) -> r > 20, "World");

        assertFalse(updated);
        assertEquals("Hello", pair.left());
    }

    @Test
    public void testSetLeftIf_withException() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        assertThrows(RuntimeException.class, () -> {
            pair.setLeftIf((l, r) -> {
                throw new RuntimeException("Test exception");
            }, "World");
        });

        // state should remain unchanged after exception
        assertEquals("Hello", pair.left());
        assertEquals(10, pair.right());
    }

    @Test
    public void testSetRightIf_predicateTrue() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        boolean updated = pair.setRightIf((l, r) -> l.length() > 3, 20);

        assertTrue(updated);
        assertEquals(20, pair.right());
    }

    @Test
    public void testSetRightIf_predicateFalse() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        // make predicate clearly false for the current state
        boolean updated = pair.setRightIf((l, r) -> r < 0, 30);

        assertFalse(updated);
        assertEquals(10, pair.right());
    }

    @Test
    public void testSetRightIf_withException() {
        Pair<String, Integer> pair = Pair.of("Hello", 10);
        assertThrows(RuntimeException.class, () -> {
            pair.setRightIf((p, newVal) -> {
                throw new RuntimeException("Test exception");
            }, 20);
        });
    }

    @Test
    public void testSetIf_predicateTrue() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        // Predicate is evaluated on current left/right: ("Hello", 10)
        boolean updated = pair.setIf((l, r) -> l.length() + r < 20, "World", 20); // 5 + 10 < 20 => true

        assertTrue(updated);
        assertEquals("World", pair.left());
        assertEquals(20, pair.right());
    }

    @Test
    public void testSetIf_predicateFalse() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        // Make the predicate false for the current state ("Hello", 10)
        boolean updated = pair.setIf((l, r) -> r < 0, "World", 20); // 10 < 0 => false

        assertFalse(updated);
        assertEquals("Hello", pair.left());
        assertEquals(10, pair.right());
    }

    @Test
    public void testSetIf_withException() throws Exception {
        Pair<String, Integer> pair = Pair.of("Hello", 10);

        assertThrows(RuntimeException.class, () -> {
            pair.setIf((l, r) -> {
                throw new RuntimeException("Test exception");
            }, "World", 20);
        });

        // Optional but good to assert state is unchanged after exception
        assertEquals("Hello", pair.left());
        assertEquals(10, pair.right());
    }

    @Test
    public void testReverse() {
        Pair<String, Integer> original = Pair.of("Hello", 42);
        Pair<Integer, String> reversed = original.swap();
        assertEquals(42, reversed.left());
        assertEquals("Hello", reversed.right());
        assertEquals("Hello", original.left());
        assertEquals(42, original.right());
    }

    @Test
    public void testReverse_withNulls() {
        Pair<String, Integer> original = Pair.of(null, null);
        Pair<Integer, String> reversed = original.swap();
        assertNull(reversed.left());
        assertNull(reversed.right());
    }

    @Test
    public void testCopy() {
        Pair<String, Integer> original = Pair.of("Hello", 42);
        Pair<String, Integer> copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.left(), copy.left());
        assertEquals(original.right(), copy.right());
    }

    @Test
    public void testCopy_shallowCopy() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        Pair<String, List<Integer>> original = Pair.of("Numbers", list);
        Pair<String, List<Integer>> copy = original.copy();

        assertSame(original.right(), copy.right());

        copy.right().add(2);
        assertEquals(2, original.right().size());
    }

    @Test
    public void testToArray() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        Object[] array = pair.toArray();
        assertEquals(2, array.length);
        assertEquals("Hello", array[0]);
        assertEquals(42, array[1]);
    }

    @Test
    public void testToArray_withNulls() {
        Pair<String, Integer> pair = Pair.of(null, null);
        Object[] array = pair.toArray();
        assertEquals(2, array.length);
        assertNull(array[0]);
        assertNull(array[1]);
    }

    @Test
    public void testToArray_withTypeParameter_sufficientSize() {
        Pair<String, String> pair = Pair.of("Hello", "World");
        String[] existing = new String[3];
        String[] result = pair.toArray(existing);
        assertSame(existing, result);
        assertEquals("Hello", result[0]);
        assertEquals("World", result[1]);
        assertNull(result[2]);
    }

    @Test
    public void testToArray_withTypeParameter_insufficientSize() {
        Pair<String, String> pair = Pair.of("Hello", "World");
        String[] small = new String[1];
        String[] result = pair.toArray(small);
        assertNotSame(small, result);
        assertEquals(2, result.length);
        assertEquals("Hello", result[0]);
        assertEquals("World", result[1]);
    }

    @Test
    public void testForEach() {
        Pair<String, String> pair = Pair.of("Hello", "World");
        List<Object> collected = new ArrayList<>();
        pair.forEach(collected::add);
        assertEquals(2, collected.size());
        assertEquals("Hello", collected.get(0));
        assertEquals("World", collected.get(1));
    }

    @Test
    public void testForEach_withException() {
        Pair<String, String> pair = Pair.of("Hello", "World");
        assertThrows(RuntimeException.class, () -> {
            pair.forEach(s -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testAccept_biConsumer() {
        Pair<String, Integer> pair = Pair.of("Age", 25);
        Map<String, Integer> map = new HashMap<>();
        pair.accept(map::put);
        assertEquals(1, map.size());
        assertEquals(25, map.get("Age"));
    }

    @Test
    public void testAccept_biConsumer_withException() {
        Pair<String, Integer> pair = Pair.of("Age", 25);
        assertThrows(RuntimeException.class, () -> {
            pair.accept((l, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testAccept_consumer() {
        Pair<String, Integer> pair = Pair.of("Score", 100);
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        pair.accept((Throwables.Consumer<Pair<String, Integer>, RuntimeException>) pairs::add);
        assertEquals(1, pairs.size());
        assertSame(pair, pairs.get(0));
    }

    @Test
    public void testAccept_consumer_withException() {
        Pair<String, Integer> pair = Pair.of("Score", 100);
        assertThrows(RuntimeException.class, () -> {
            pair.accept(p -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testMap_biFunction() {
        Pair<Integer, Integer> dimensions = Pair.of(10, 20);
        Integer area = dimensions.map((width, height) -> width * height);
        assertEquals(200, area);
    }

    @Test
    public void testMap_biFunction_stringConcatenation() {
        Pair<String, String> names = Pair.of("John", "Doe");
        String fullName = names.map((first, last) -> first + " " + last);
        assertEquals("John Doe", fullName);
    }

    @Test
    public void testMap_biFunction_withException() {
        Pair<String, Integer> pair = Pair.of("Test", 10);
        assertThrows(RuntimeException.class, () -> {
            pair.map((l, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testMap_function() {
        Pair<String, Integer> entry = Pair.of("Temperature", 25);
        String formatted = entry.map(p -> p.left() + ": " + p.right() + "°C");
        assertEquals("Temperature: 25°C", formatted);
    }

    @Test
    public void testMap_function_nullCheck() {
        Pair<String, Integer> entry = Pair.of("Temperature", 25);
        Boolean isValid = entry.map(p -> p.left() != null && p.right() != null);
        assertTrue(isValid);
    }

    @Test
    public void testMap_function_withException() {
        Pair<String, Integer> pair = Pair.of("Test", 10);
        assertThrows(RuntimeException.class, () -> {
            pair.map(p -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testFilter_biPredicate_true() {
        Pair<String, Integer> pair = Pair.of("Hello", 5);
        Optional<Pair<String, Integer>> filtered = pair.filter((left, right) -> left.length() == right);
        assertTrue(filtered.isPresent());
        assertSame(pair, filtered.get());
    }

    @Test
    public void testFilter_biPredicate_false() {
        Pair<String, Integer> pair = Pair.of("Hello", 5);
        Optional<Pair<String, Integer>> filtered = pair.filter((left, right) -> right > 10);
        assertFalse(filtered.isPresent());
    }

    @Test
    public void testFilter_biPredicate_withException() {
        Pair<String, Integer> pair = Pair.of("Hello", 5);
        assertThrows(RuntimeException.class, () -> {
            pair.filter((l, r) -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testFilter_predicate_true() {
        Pair<String, Integer> pair = Pair.of("Test", 10);
        Optional<Pair<String, Integer>> filtered = pair.filter(p -> p.left().startsWith("T") && p.right() % 2 == 0);
        assertTrue(filtered.isPresent());
        assertSame(pair, filtered.get());
    }

    @Test
    public void testFilter_predicate_false() {
        Pair<String, Integer> pair = Pair.of("Test", 10);
        Optional<Pair<String, Integer>> filtered = pair.filter(p -> p.left().length() + p.right() > 20);
        assertFalse(filtered.isPresent());
    }

    @Test
    public void testFilter_predicate_withException() {
        Pair<String, Integer> pair = Pair.of("Test", 10);
        assertThrows(RuntimeException.class, () -> {
            pair.filter(p -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testToTuple() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        Tuple2<String, Integer> tuple = pair.toTuple();
        assertNotNull(tuple);
        assertEquals("Hello", tuple._1);
        assertEquals(42, tuple._2);
    }

    @Test
    public void testToImmutableEntry() {
        Pair<String, Integer> pair = Pair.of("Age", 30);
        ImmutableEntry<String, Integer> entry = pair.toImmutableEntry();
        assertEquals("Age", entry.getKey());
        assertEquals(30, entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(31));
    }

    @Test
    public void testGetKey() {
        Pair<String, Integer> pair = Pair.of("key", 100);
        assertEquals("key", pair.getKey());
    }

    @Test
    public void testGetKey_withNull() {
        Pair<String, Integer> pair = Pair.of(null, 100);
        assertNull(pair.getKey());
    }

    @Test
    public void testGetValue() {
        Pair<String, Integer> pair = Pair.of("key", 100);
        assertEquals(100, pair.getValue());
    }

    @Test
    public void testGetValue_withNull() {
        Pair<String, Integer> pair = Pair.of("key", null);
        assertNull(pair.getValue());
    }

    @Test
    public void testSetValue() {
        Pair<String, Integer> pair = Pair.of("key", 100);
        Integer oldValue = pair.setValue(200);
        assertEquals(100, oldValue);
        assertEquals(200, pair.right());
    }

    @Test
    public void testSetValue_withNull() {
        Pair<String, Integer> pair = Pair.of("key", 100);
        Integer oldValue = pair.setValue(null);
        assertEquals(100, oldValue);
        assertNull(pair.right());
    }

    @Test
    public void testHashCode_consistent() {
        Pair<String, Integer> pair1 = Pair.of("Hello", 42);
        Pair<String, Integer> pair2 = Pair.of("Hello", 42);
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    public void testHashCode_different() {
        Pair<String, Integer> pair1 = Pair.of("Hello", 42);
        Pair<String, Integer> pair2 = Pair.of("Hello", 43);
        assertNotEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    public void testHashCode_withNulls() {
        Pair<String, Integer> pair1 = Pair.of(null, null);
        Pair<String, Integer> pair2 = Pair.of(null, null);
        assertEquals(pair1.hashCode(), pair2.hashCode());
    }

    @Test
    public void testEquals_same() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertTrue(pair.equals(pair));
    }

    @Test
    public void testEquals_equal() {
        Pair<String, Integer> pair1 = Pair.of("Hello", 42);
        Pair<String, Integer> pair2 = Pair.of("Hello", 42);
        assertTrue(pair1.equals(pair2));
        assertTrue(pair2.equals(pair1));
    }

    @Test
    public void testEquals_notEqual_differentLeft() {
        Pair<String, Integer> pair1 = Pair.of("Hello", 42);
        Pair<String, Integer> pair2 = Pair.of("World", 42);
        assertFalse(pair1.equals(pair2));
    }

    @Test
    public void testEquals_notEqual_differentRight() {
        Pair<String, Integer> pair1 = Pair.of("Hello", 42);
        Pair<String, Integer> pair2 = Pair.of("Hello", 43);
        assertFalse(pair1.equals(pair2));
    }

    @Test
    public void testEquals_notEqual_differentType() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertFalse(pair.equals("Hello"));
    }

    @Test
    public void testEquals_null() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertFalse(pair.equals(null));
    }

    @Test
    public void testEquals_withNulls() {
        Pair<String, Integer> pair1 = Pair.of(null, null);
        Pair<String, Integer> pair2 = Pair.of(null, null);
        assertTrue(pair1.equals(pair2));
    }

    @Test
    public void testEquals_oneNull() {
        Pair<String, Integer> pair1 = Pair.of(null, 42);
        Pair<String, Integer> pair2 = Pair.of("Hello", 42);
        assertFalse(pair1.equals(pair2));
    }

    @Test
    public void testToString() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        assertEquals("(Hello, 42)", pair.toString());
    }

    @Test
    public void testToString_withNull() {
        Pair<String, String> pair = Pair.of(null, "World");
        assertEquals("(null, World)", pair.toString());
    }

    @Test
    public void testToString_bothNull() {
        Pair<String, Integer> pair = Pair.of(null, null);
        assertEquals("(null, null)", pair.toString());
    }

    @Test
    public void testMapEntryCompatibility() {
        Pair<String, Integer> pair = Pair.of("test", 123);
        Map.Entry<String, Integer> entry = pair;
        assertEquals("test", entry.getKey());
        assertEquals(123, entry.getValue());

        Integer oldValue = entry.setValue(456);
        assertEquals(123, oldValue);
        assertEquals(456, entry.getValue());
    }
}
