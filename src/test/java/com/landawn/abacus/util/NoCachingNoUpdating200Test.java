package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class NoCachingNoUpdating200Test extends TestBase {

    @Nested
    @DisplayName("DisposableArray Tests")
    public class DisposableArrayTest {

        private NoCachingNoUpdating.DisposableArray<String> disposableArray;
        private String[] sourceArray;

        @BeforeEach
        public void setUp() {
            sourceArray = new String[] { "a", "b", "c" };
            disposableArray = NoCachingNoUpdating.DisposableArray.wrap(sourceArray);
        }

        @Test
        @DisplayName("Should create a DisposableArray of the specified type and length")
        public void testCreate() {
            NoCachingNoUpdating.DisposableArray<Integer> created = NoCachingNoUpdating.DisposableArray.create(Integer.class, 5);
            assertNotNull(created);
            assertEquals(5, created.length());
        }

        @Test
        @DisplayName("Should wrap an existing array")
        public void testWrap() {
            assertNotNull(disposableArray);
            assertEquals(sourceArray.length, disposableArray.length());
        }

        @Test
        @DisplayName("Should get the element at the specified index")
        public void testGet() {
            assertEquals("a", disposableArray.get(0));
            assertEquals("b", disposableArray.get(1));
            assertEquals("c", disposableArray.get(2));
        }

        @Test
        @DisplayName("Should return the correct length of the array")
        public void testLength() {
            assertEquals(3, disposableArray.length());
        }

        @Test
        @DisplayName("Should copy the contents to a target array")
        public void testToArray() {
            String[] target = new String[3];
            disposableArray.toArray(target);
            assertArrayEquals(sourceArray, target);
        }

        @Test
        @DisplayName("Should create a copy of the internal array")
        public void testCopy() {
            String[] copy = disposableArray.copy();
            assertNotSame(sourceArray, copy);
            assertArrayEquals(sourceArray, copy);
        }

        @Test
        @DisplayName("Should convert the array to a List")
        public void testToList() {
            List<String> list = disposableArray.toList();
            assertEquals(Arrays.asList(sourceArray), list);
        }

        @Test
        @DisplayName("Should convert the array to a Set")
        public void testToSet() {
            Set<String> set = disposableArray.toSet();
            assertEquals(new HashSet<>(Arrays.asList(sourceArray)), set);
        }

        @Test
        @DisplayName("Should convert the array to a specified collection")
        public void testToCollection() {
            ArrayList<String> collection = disposableArray.toCollection(ArrayList::new);
            assertEquals(Arrays.asList(sourceArray), collection);
        }

        @Test
        @DisplayName("Should iterate over each element")
        public void testForEach() throws Exception {
            List<String> result = new ArrayList<>();
            disposableArray.foreach(result::add);
            assertEquals(Arrays.asList(sourceArray), result);
        }

        @Test
        @DisplayName("Should apply a function to the array")
        public void testApply() throws Exception {
            Integer length = disposableArray.apply(arr -> arr.length);
            assertEquals(sourceArray.length, length);
        }

        @Test
        @DisplayName("Should accept a consumer for the array")
        public void testAccept() throws Exception {
            final List<String[]> captured = new ArrayList<>();
            disposableArray.accept(captured::add);
            assertEquals(1, captured.size());
            assertSame(sourceArray, captured.get(0));
        }

        @Test
        @DisplayName("Should join array elements with a delimiter")
        public void testJoinWithDelimiter() {
            assertEquals("a,b,c", disposableArray.join(","));
        }

        @Test
        @DisplayName("Should join array elements with delimiter, prefix, and suffix")
        public void testJoinWithDelimiterPrefixSuffix() {
            assertEquals("[a,b,c]", disposableArray.join(",", "[", "]"));
        }

        @Test
        @DisplayName("Should provide a working iterator")
        public void testIterator() {
            List<String> result = new ArrayList<>();
            disposableArray.iterator().forEachRemaining(result::add);
            assertEquals(Arrays.asList(sourceArray), result);
        }

        @Test
        @DisplayName("Should return a string representation of the array")
        public void testToString() {
            assertEquals(Arrays.toString(sourceArray), disposableArray.toString());
        }
    }

    @Nested
    @DisplayName("DisposableObjArray Tests")
    public class DisposableObjArrayTest {

        @Test
        @DisplayName("Should create a DisposableObjArray of the specified length")
        public void testCreate() {
            NoCachingNoUpdating.DisposableObjArray created = NoCachingNoUpdating.DisposableObjArray.create(5);
            assertNotNull(created);
            assertEquals(5, created.length());
        }

        @Test
        @DisplayName("Should wrap an existing object array")
        public void testWrap() {
            Object[] source = new Object[] { 1, "test", true };
            NoCachingNoUpdating.DisposableObjArray wrapped = NoCachingNoUpdating.DisposableObjArray.wrap(source);
            assertNotNull(wrapped);
            assertEquals(3, wrapped.length());
        }

        @Test
        @DisplayName("Should throw UnsupportedOperationException for create with component type")
        public void testCreateWithComponentType() {
            assertThrows(UnsupportedOperationException.class, () -> NoCachingNoUpdating.DisposableObjArray.create(String.class, 5));
        }
    }

    @Nested
    @DisplayName("DisposableBooleanArray Tests")
    public class DisposableBooleanArrayTest extends TestBase {

        private boolean[] sourceArray = { true, false, true };
        private NoCachingNoUpdating.DisposableBooleanArray disposableArray = NoCachingNoUpdating.DisposableBooleanArray.wrap(sourceArray);

        @Test
        @DisplayName("Should get the element at the specified index")
        public void testGet() {
            assertTrue(disposableArray.get(0));
            assertTrue(disposableArray.get(2));
        }

        @Test
        @DisplayName("Should return a copy of the array")
        public void testCopy() {
            boolean[] copy = disposableArray.copy();
            assertNotSame(sourceArray, copy);
            assertArrayEquals(sourceArray, copy);
        }

        @Test
        @DisplayName("Should box the primitive array to a wrapper array")
        public void testBox() {
            Boolean[] boxed = disposableArray.box();
            assertArrayEquals(new Boolean[] { true, false, true }, boxed);
        }
    }

    @Nested
    @DisplayName("DisposableCharArray Tests")
    public class DisposableCharArrayTest extends TestBase {

        private char[] sourceArray = { 'a', 'b', 'c' };
        private NoCachingNoUpdating.DisposableCharArray disposableArray = NoCachingNoUpdating.DisposableCharArray.wrap(sourceArray);

        @Test
        @DisplayName("Should calculate the sum of char values")
        public void testSum() {
            assertEquals(97 + 98 + 99, disposableArray.sum());
        }

        @Test
        @DisplayName("Should calculate the average of char values")
        public void testAverage() {
            assertEquals((97 + 98 + 99) / 3.0, disposableArray.average());
        }

        @Test
        @DisplayName("Should find the minimum char value")
        public void testMin() {
            assertEquals('a', disposableArray.min());
        }

        @Test
        @DisplayName("Should find the maximum char value")
        public void testMax() {
            assertEquals('c', disposableArray.max());
        }
    }

    @Nested
    @DisplayName("DisposableIntArray Tests")
    public class DisposableIntArrayTest extends TestBase {
        private int[] sourceArray = { 10, 20, 30 };
        private NoCachingNoUpdating.DisposableIntArray disposableArray = NoCachingNoUpdating.DisposableIntArray.wrap(sourceArray);

        @Test
        @DisplayName("Should calculate the sum of int values")
        public void testSum() {
            assertEquals(60, disposableArray.sum());
        }

        @Test
        @DisplayName("Should calculate the average of int values")
        public void testAverage() {
            assertEquals(20.0, disposableArray.average());
        }

        @Test
        @DisplayName("Should find the minimum int value")
        public void testMin() {
            assertEquals(10, disposableArray.min());
        }

        @Test
        @DisplayName("Should find the maximum int value")
        public void testMax() {
            assertEquals(30, disposableArray.max());
        }
    }

    @Nested
    @DisplayName("DisposableDeque Tests")
    public class DisposableDequeTest extends TestBase {
        private NoCachingNoUpdating.DisposableDeque<Integer> disposableDeque;
        private Deque<Integer> sourceDeque;

        @BeforeEach
        public void setup() {
            sourceDeque = new ArrayDeque<>(Arrays.asList(1, 2, 3));
            disposableDeque = NoCachingNoUpdating.DisposableDeque.wrap(sourceDeque);
        }

        @Test
        @DisplayName("Should create a DisposableDeque with a given capacity")
        public void testCreate() {
            NoCachingNoUpdating.DisposableDeque<String> created = NoCachingNoUpdating.DisposableDeque.create(10);
            assertNotNull(created);
            assertEquals(0, created.size());
        }

        @Test
        @DisplayName("Should wrap an existing Deque")
        public void testWrap() {
            assertEquals(3, disposableDeque.size());
        }

        @Test
        @DisplayName("Should return the first element")
        public void testGetFirst() {
            assertEquals(1, disposableDeque.getFirst());
        }

        @Test
        @DisplayName("Should return the last element")
        public void testGetLast() {
            assertEquals(3, disposableDeque.getLast());
        }

        @Test
        @DisplayName("Should throw exception when getting first from empty deque")
        public void testGetFirst_Empty() {
            NoCachingNoUpdating.DisposableDeque<Integer> emptyDeque = NoCachingNoUpdating.DisposableDeque.wrap(new ArrayDeque<>());
            assertThrows(NoSuchElementException.class, emptyDeque::getFirst);
        }
    }

    @Nested
    @DisplayName("DisposableEntry Tests")
    public class DisposableEntryTest extends TestBase {

        private NoCachingNoUpdating.DisposableEntry<String, Integer> disposableEntry;
        private Map.Entry<String, Integer> sourceEntry;

        @BeforeEach
        public void setup() {
            sourceEntry = new AbstractMap.SimpleEntry<>("key", 123);
            disposableEntry = NoCachingNoUpdating.DisposableEntry.wrap(sourceEntry);
        }

        @Test
        @DisplayName("Should wrap a Map.Entry")
        public void testWrap() {
            assertEquals("key", disposableEntry.getKey());
            assertEquals(123, disposableEntry.getValue());
        }

        @Test
        @DisplayName("Should throw UnsupportedOperationException on setValue")
        public void testSetValue() {
            assertThrows(UnsupportedOperationException.class, () -> disposableEntry.setValue(456));
        }

        @Test
        @DisplayName("Should create a copy of the entry")
        public void testCopy() {
            Map.Entry<String, Integer> copy = disposableEntry.copy();
            assertNotSame(sourceEntry, copy);
            assertEquals(sourceEntry, copy);
        }

        @Test
        @DisplayName("Should apply a BiFunction to key and value")
        public void testApplyBiFunction() throws Exception {
            String result = disposableEntry.apply((k, v) -> k + ":" + v);
            assertEquals("key:123", result);
        }

        @Test
        @DisplayName("Should accept a BiConsumer for key and value")
        public void testAcceptBiConsumer() throws Exception {
            List<String> result = new ArrayList<>();
            disposableEntry.accept((k, v) -> result.add(k + "=" + v));
            assertEquals(1, result.size());
            assertEquals("key=123", result.get(0));
        }

        @Test
        @DisplayName("Should return a string representation of the entry")
        public void testToString() {
            assertEquals("key=123", disposableEntry.toString());
        }
    }

    @Nested
    @DisplayName("DisposablePair Tests")
    public class DisposablePairTest extends TestBase {
        private NoCachingNoUpdating.DisposablePair<String, Integer> disposablePair;
        private Pair<String, Integer> sourcePair;

        @BeforeEach
        public void setup() {
            sourcePair = Pair.of("left", 100);
            disposablePair = NoCachingNoUpdating.DisposablePair.wrap(sourcePair);
        }

        @Test
        @DisplayName("Should wrap an existing pair")
        public void testWrap() {
            assertEquals("left", disposablePair.left());
            assertEquals(100, disposablePair.right());
        }

        @Test
        @DisplayName("Should copy the pair")
        public void testCopy() {
            Pair<String, Integer> copy = disposablePair.copy();
            assertNotSame(sourcePair, copy);
            assertEquals(sourcePair, copy);
        }

        @Test
        @DisplayName("Should apply a BiFunction")
        public void testApply() throws Exception {
            String result = disposablePair.apply((l, r) -> l + r);
            assertEquals("left100", result);
        }

        @Test
        @DisplayName("Should accept a BiConsumer")
        public void testAccept() throws Exception {
            List<Object> captured = new ArrayList<>();
            disposablePair.accept((l, r) -> {
                captured.add(l);
                captured.add(r);
            });
            assertEquals(Arrays.asList("left", 100), captured);
        }

        @Test
        @DisplayName("Should return a string representation of the pair")
        public void testToString() {
            assertEquals("[left, 100]", disposablePair.toString());
        }
    }

    @Nested
    @DisplayName("DisposableTriple Tests")
    public class DisposableTripleTest extends TestBase {

        private NoCachingNoUpdating.DisposableTriple<String, Integer, Boolean> disposableTriple;
        private Triple<String, Integer, Boolean> sourceTriple;

        @BeforeEach
        public void setup() {
            sourceTriple = Triple.of("left", 200, true);
            disposableTriple = NoCachingNoUpdating.DisposableTriple.wrap(sourceTriple);
        }

        @Test
        @DisplayName("Should wrap an existing triple")
        public void testWrap() {
            assertEquals("left", disposableTriple.left());
            assertEquals(200, disposableTriple.middle());
            assertTrue(disposableTriple.right());
        }

        @Test
        @DisplayName("Should copy the triple")
        public void testCopy() {
            Triple<String, Integer, Boolean> copy = disposableTriple.copy();
            assertNotSame(sourceTriple, copy);
            assertEquals(sourceTriple, copy);
        }

        @Test
        @DisplayName("Should apply a TriFunction")
        public void testApply() throws Exception {
            String result = disposableTriple.apply((l, m, r) -> l + m + r);
            assertEquals("left200true", result);
        }

        @Test
        @DisplayName("Should accept a TriConsumer")
        public void testAccept() throws Exception {
            List<Object> captured = new ArrayList<>();
            disposableTriple.accept((l, m, r) -> {
                captured.add(l);
                captured.add(m);
                captured.add(r);
            });
            assertEquals(Arrays.asList("left", 200, true), captured);
        }

        @Test
        @DisplayName("Should return a string representation of the triple")
        public void testToString() {
            assertEquals("[left, 200, true]", disposableTriple.toString());
        }
    }

    @Nested
    @DisplayName("Timed Tests")
    public class TimedTest extends TestBase {
        private NoCachingNoUpdating.Timed<String> timed;
        private final long timestamp = System.currentTimeMillis();
        private final String value = "test-value";

        @BeforeEach
        public void setup() {
            timed = NoCachingNoUpdating.Timed.of(value, timestamp);
        }

        @Test
        @DisplayName("Should create a Timed object with correct value and timestamp")
        public void testOf() {
            assertEquals(value, timed.value());
            assertEquals(timestamp, timed.timestamp());
        }

        @Test
        @DisplayName("Should return the correct value")
        public void testValue() {
            assertEquals("test-value", timed.value());
        }

        @Test
        @DisplayName("Should return the correct timestamp")
        public void testTimestamp() {
            assertEquals(timestamp, timed.timestamp());
        }

        @Test
        @DisplayName("Should have consistent hash code")
        public void testHashCode() {
            NoCachingNoUpdating.Timed<String> sameTimed = NoCachingNoUpdating.Timed.of(value, timestamp);
            assertEquals(timed.hashCode(), sameTimed.hashCode());
        }

        @Test
        @DisplayName("Should be equal to another Timed object with the same value and time")
        public void testEquals() {
            NoCachingNoUpdating.Timed<String> sameTimed = NoCachingNoUpdating.Timed.of(value, timestamp);
            NoCachingNoUpdating.Timed<String> differentTime = NoCachingNoUpdating.Timed.of(value, timestamp + 1);
            NoCachingNoUpdating.Timed<String> differentValue = NoCachingNoUpdating.Timed.of("other", timestamp);

            assertTrue(timed.equals(sameTimed));
            assertFalse(timed.equals(differentTime));
            assertFalse(timed.equals(differentValue));
            assertFalse(timed.equals(null));
            assertFalse(timed.equals(new Object()));
        }

        @Test
        @DisplayName("Should have a correct string representation")
        public void testToString() {
            String expected = timestamp + ": " + value;
            assertEquals(expected, timed.toString());
        }
    }

}
