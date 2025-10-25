package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharSupplier2025Test extends TestBase {

    @Test
    public void testGetAsChar() {
        CharSupplier supplier = () -> 'A';

        assertEquals('A', supplier.getAsChar());
    }

    @Test
    public void testGetAsCharWithLambda() {
        CharSupplier supplier = () -> 'z';

        assertEquals('z', supplier.getAsChar());
        assertEquals('z', supplier.getAsChar());
    }

    @Test
    public void testGetAsCharWithAnonymousClass() {
        CharSupplier supplier = new CharSupplier() {
            @Override
            public char getAsChar() {
                return 'X';
            }
        };

        assertEquals('X', supplier.getAsChar());
    }

    @Test
    public void testZero() {
        assertEquals('\0', CharSupplier.ZERO.getAsChar());
        assertEquals('\0', CharSupplier.ZERO.getAsChar());
    }

    @Test
    public void testRandom() {
        char first = CharSupplier.RANDOM.getAsChar();
        char second = CharSupplier.RANDOM.getAsChar();

        assertTrue(first >= Character.MIN_VALUE && first <= Character.MAX_VALUE);
        assertTrue(second >= Character.MIN_VALUE && second <= Character.MAX_VALUE);
    }

    @Test
    public void testRandomProducesDifferentValues() {
        boolean foundDifferent = false;
        char first = CharSupplier.RANDOM.getAsChar();

        for (int i = 0; i < 100; i++) {
            if (CharSupplier.RANDOM.getAsChar() != first) {
                foundDifferent = true;
                break;
            }
        }

        assertTrue(foundDifferent);
    }

    @Test
    public void testStatefulSupplier() {
        final char[] counter = { 'a' };
        CharSupplier supplier = () -> counter[0]++;

        assertEquals('a', supplier.getAsChar());
        assertEquals('b', supplier.getAsChar());
        assertEquals('c', supplier.getAsChar());
    }

    @Test
    public void testBoundaryValues() {
        CharSupplier maxSupplier = () -> Character.MAX_VALUE;
        CharSupplier minSupplier = () -> Character.MIN_VALUE;

        assertEquals(Character.MAX_VALUE, maxSupplier.getAsChar());
        assertEquals(Character.MIN_VALUE, minSupplier.getAsChar());
    }

    @Test
    public void testMethodReference() {
        TestObject obj = new TestObject();
        CharSupplier supplier = obj::getValue;

        assertEquals('M', supplier.getAsChar());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharSupplier.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        char getValue() {
            return 'M';
        }
    }
}
