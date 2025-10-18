package com.landawn.abacus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.BufferedJSONWriter;
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
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(char[] expected, char[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(byte[] expected, byte[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(short[] expected, short[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(int[] expected, int[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(long[] expected, long[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(float[] expected, float[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static void assertHaveSameElements(double[] expected, double[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static <T> void assertHaveSameElements(T[] expected, T[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, T[] actual) {
        assertHaveSameElements(expected, N.asList(actual));
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, Collection<? extends T> actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + N.toString(expected) + ", Actual: " + N.toString(actual));
    }

    public static <T> Iterable<T> createIterable(final T... a) {
        return new Iterable<>() {
            @Override
            public java.util.Iterator<T> iterator() {
                return N.asList(a).iterator();
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
        return mock(BufferedJSONWriter.class);
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
