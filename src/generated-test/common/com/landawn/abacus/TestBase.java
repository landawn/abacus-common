package com.landawn.abacus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collection;

import org.junit.jupiter.api.Tag;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

@Tag("base-test")
public abstract class TestBase {

    public static final char[] NULL_CHAR_ARRAY = "null".toCharArray();;

    public static void assertHaveSameElements(boolean[] expected, boolean[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(char[] expected, char[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(byte[] expected, byte[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(short[] expected, short[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(int[] expected, int[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(long[] expected, long[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(float[] expected, float[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static void assertHaveSameElements(double[] expected, double[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static <T> void assertHaveSameElements(T[] expected, T[] actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, T[] actual) {
        assertHaveSameElements(expected, N.asList(actual));
    }

    public static <T> void assertHaveSameElements(Collection<? extends T> expected, Collection<? extends T> actual) {
        assertTrue(N.haveSameElements(expected, actual), "Expected: " + expected + ", Actual: " + actual);
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

}
