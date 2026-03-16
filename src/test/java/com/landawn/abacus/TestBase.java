package com.landawn.abacus;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Iterators;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.TriIterator;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.TypeReference;

public abstract class TestBase {

    public static final char[] NULL_CHAR_ARRAY = "null".toCharArray();
    public static final char[] TRUE_CHAR_ARRAY = "true".toCharArray();
    public static final char[] FALSE_CHAR_ARRAY = "false".toCharArray();

    public static void assertHaveSameElements(boolean[] expected, boolean[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(char[] expected, char[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(byte[] expected, byte[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(short[] expected, short[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(int[] expected, int[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(long[] expected, long[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(float[] expected, float[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(double[] expected, double[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static <T> void assertHaveSameElements(T[] expected, T[] actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, T[] actual) {
        assertHaveSameElements(expected, N.toList(actual));
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, Collection<? extends T> actual) {
        assertTrue(N.containsSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertFloatArrayEquals(float[] expected, float[] actual, float delta) {
        assertEquals(expected.length, actual.length, "Array lengths differ");

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], delta, "Array elements at index " + i + " differ");
        }
    }

    public static void assertDoubleArrayEquals(double[] expected, double[] actual, double delta) {
        assertEquals(expected.length, actual.length, "Array lengths differ");

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], delta, "Array elements at index " + i + " differ");
        }
    }

    public static <T> void assertListOfListsEquals(List<List<T>> expected, List<List<T>> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Outer list sizes differ");

        for (int i = 0; i < expected.size(); i++) {
            assertIterableEquals(expected.get(i), actual.get(i), "Inner list at index " + i + " differs");
        }
    }

    public static <T> void assertListOfArraysEquals(List<T[]> expected, List<T[]> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "List sizes differ");

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i), "Array at index " + i + " differs");
        }
    }

    public static void assertListOfBooleanArraysEquals(List<boolean[]> expected, List<boolean[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfCharArraysEquals(List<char[]> expected, List<char[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfByteArraysEquals(List<byte[]> expected, List<byte[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfShortArraysEquals(List<short[]> expected, List<short[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfIntArraysEquals(List<int[]> expected, List<int[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfLongArraysEquals(List<long[]> expected, List<long[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfFloatArraysEquals(List<float[]> expected, List<float[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static void assertListOfDoubleArraysEquals(List<double[]> expected, List<double[]> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), actual.get(i));
        }
    }

    public static <T> void assertMapEquals(Map<T, Integer> expected, Map<T, Integer> actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Map sizes differ. Expected: " + expected + ", Actual: " + actual);

        for (Entry<T, Integer> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()), "Actual map missing key: " + entry.getKey() + ". Actual map: " + actual);
            assertEquals(entry.getValue(), actual.get(entry.getKey()),
                    "Value for key " + entry.getKey() + " differs. Expected: " + entry.getValue() + ", Actual: " + actual.get(entry.getKey()));
        }
    }

    public static <T> Iterable<T> createIterable(final T... a) {
        return new Iterable<>() {
            @Override
            public java.util.Iterator<T> iterator() {
                return N.toList(a).iterator();
            }
        };
    }

    public static <T> Iterable<T> createIterable(final Collection<? extends T> c) {
        return new Iterable<>() {
            @Override
            public java.util.Iterator<T> iterator() {
                return (Iterator<T>) c.iterator();
            }
        };
    }

    public static <A, B> BiIterator<A, B> createBiIterator(final Iterable<Pair<A, B>> iterable) {
        if (iterable == null) {
            return BiIterator.empty();
        }

        final Iterator<Pair<A, B>> iter = iterable.iterator();

        return BiIterator.of(Iterators.map(iter, e -> N.newEntry(e.left(), e.right())));
    }

    public static <A, B, C> TriIterator<A, B, C> createTriIterator(final Iterable<Triple<A, B, C>> iterable) {
        if (iterable == null) {
            return TriIterator.empty();
        }

        final Iterator<Triple<A, B, C>> iter = iterable.iterator();

        return TriIterator.generate(() -> iter.hasNext(), t -> {
            final Triple<A, B, C> e = iter.next();
            N.println(e);
            t.setLeft(e.left());
            t.setMiddle(e.middle());
            t.setRight(e.right());
        });
    }

    protected static <T> Type<T> createType(Class<T> typeClass) {
        return Type.of(typeClass);
    }

    protected static <T> Type<T> createType(TypeReference<T> typeRef) {
        return typeRef.type();
    }

    protected static <T extends Type> T createType(String typeName) {
        return (T) Type.of(typeName);
    }

    protected static CharacterWriter createCharacterWriter() {
        return mock(BufferedJsonWriter.class);
    }

    public static void unmap(MappedByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        try {
            buffer.force(); // flush changes to disk

            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);

            Method cleanMethod = cleaner.getClass().getMethod("clean");
            cleanMethod.setAccessible(true);
            cleanMethod.invoke(cleaner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmap the buffer", e);
        }
    }
}
