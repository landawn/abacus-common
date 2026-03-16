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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class PairTest extends TestBase {

    private Pair<String, Integer> pair;

    @BeforeEach
    public void setUp() {
        pair = new Pair<>();
    }

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

    // from(Map.Entry)
    @Test
    public void testFrom() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 99);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        Pair<String, Integer> pair = Pair.from(entry);
        assertEquals("key", pair.left());
        assertEquals(99, pair.right());
    }

    @Test
    public void testCreate_fromMapEntry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("age", 25);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        Pair<String, Integer> pair = Pair.from(entry);
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
    public void testEmptyArray_SameInstance() {
        Pair<String, Integer>[] array1 = Pair.emptyArray();
        Pair<String, Integer>[] array2 = Pair.emptyArray();
        assertSame(array1, array2);
    }

    @Test
    public void testEquals_withMapEntry() {
        Pair<String, Integer> pair = Pair.of("key", 42);
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<>("key", 42);
        assertTrue(pair.equals(entry));

        Map.Entry<String, Integer> differentEntry = new AbstractMap.SimpleImmutableEntry<>("key", 99);
        assertFalse(pair.equals(differentEntry));
    }

    @Test
    public void testForEach_withNullElements() {
        Pair<String, String> pair = Pair.of(null, null);
        List<Object> collected = new ArrayList<>();
        pair.forEach(collected::add);
        assertEquals(2, collected.size());
        assertNull(collected.get(0));
        assertNull(collected.get(1));
    }

    @Test
    public void testCopy_withNulls() {
        Pair<String, Integer> original = Pair.of(null, null);
        Pair<String, Integer> copy = original.copy();
        assertNotSame(original, copy);
        assertNull(copy.left());
        assertNull(copy.right());
        assertEquals(original, copy);
    }

    @Test
    public void testSwap_thenSwapBack() {
        Pair<String, Integer> original = Pair.of("Hello", 42);
        Pair<Integer, String> swapped = original.swap();
        Pair<String, Integer> swappedBack = swapped.swap();
        assertEquals(original, swappedBack);
    }

    @Test
    public void testToTuple_withNulls() {
        Pair<String, Integer> pair = Pair.of(null, null);
        Tuple2<String, Integer> tuple = pair.toTuple();
        assertNull(tuple._1);
        assertNull(tuple._2);
    }

    @Test
    public void testToImmutableEntry_withNulls() {
        Pair<String, Integer> pair = Pair.of(null, null);
        ImmutableEntry<String, Integer> entry = pair.toImmutableEntry();
        assertNull(entry.getKey());
        assertNull(entry.getValue());
    }

    @Test
    public void testToArray_objectArray_withNulls() {
        Pair<String, String> pair = Pair.of(null, null);
        String[] result = pair.toArray(new String[2]);
        assertNull(result[0]);
        assertNull(result[1]);
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

    // swap()
    @Test
    public void testSwap() {
        Pair<String, Integer> original = Pair.of("Hello", 42);
        Pair<Integer, String> swapped = original.swap();
        assertEquals(42, swapped.left());
        assertEquals("Hello", swapped.right());
        // original unchanged
        assertEquals("Hello", original.left());
        assertEquals(42, original.right());
    }

    @Test
    public void testSwap_withNulls() {
        Pair<String, Integer> original = Pair.of(null, null);
        Pair<Integer, String> swapped = original.swap();
        assertNull(swapped.left());
        assertNull(swapped.right());
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

    @Test
    public void testConstructorsAndFactories() {
        Pair<String, Integer> p1 = new Pair<>();
        assertNull(p1.left());
        assertNull(p1.right());

        Pair<String, Integer> p2 = Pair.of("hello", 123);
        assertEquals("hello", p2.left());
        assertEquals(123, p2.right());

        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("world", 456);
        Pair<String, Integer> p3 = Pair.from(entry);
        assertEquals("world", p3.left());
        assertEquals(456, p3.right());
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
        boolean leftSet = pair.setLeftIf((l, r) -> r == 1, "newLeft");
        assertTrue(leftSet);
        assertEquals("newLeft", pair.left());
        assertEquals(1, pair.right()); // right unchanged

        // predicate is false: right is still 1, not 0
        boolean leftNotSet = pair.setLeftIf((l, r) -> r == 0, "anotherLeft");
        assertFalse(leftNotSet);
        assertEquals("newLeft", pair.left());
        assertEquals(1, pair.right());

        // right depends on current left value
        boolean rightSet = pair.setRightIf((l, r) -> "newLeft".equals(l), 100);
        assertTrue(rightSet);
        assertEquals("newLeft", pair.left());
        assertEquals(100, pair.right());

        // predicate is false: right is 100, not < 50
        boolean rightNotSet = pair.setRightIf((l, r) -> r < 50, 10);
        assertFalse(rightNotSet);
        assertEquals("newLeft", pair.left());
        assertEquals(100, pair.right());

        // both set if current right == 100
        boolean bothSet = pair.setIf((l, r) -> r == 100, "finalLeft", 999);
        assertTrue(bothSet);
        assertEquals("finalLeft", pair.left());
        assertEquals(999, pair.right());

        // predicate always false here
        boolean bothNotSet = pair.setIf((l, r) -> r < 0, "short", 0);
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

        Pair<String, Integer> p = Pair.from(entry);
        assertEquals("key", p.left());
        assertEquals(100, p.right());
    }

    @Test
    public void testSetMethod() {
        pair.set("world", 456);
        assertEquals("world", pair.left());
        assertEquals(456, pair.right());
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
    public void testMapEntryEqualsCompatibility() {
        Pair<String, Integer> pair = Pair.of("k", 1);
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("k", 1);

        assertTrue(pair.equals(entry));
        assertTrue(entry.equals(pair));
    }

    @Test
    public void testMapEntryHashCodeCompatibility() {
        Pair<String, Integer> pair = Pair.of("k", 1);
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("k", 1);

        assertEquals(entry.hashCode(), pair.hashCode());
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
