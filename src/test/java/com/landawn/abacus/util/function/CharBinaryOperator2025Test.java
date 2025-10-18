package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharBinaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsChar() {
        CharBinaryOperator max = (left, right) -> left > right ? left : right;

        assertEquals('z', max.applyAsChar('z', 'a'));
        assertEquals('m', max.applyAsChar('a', 'm'));
    }

    @Test
    public void testApplyAsCharWithLambda() {
        CharBinaryOperator min = (left, right) -> left < right ? left : right;

        assertEquals('a', min.applyAsChar('z', 'a'));
        assertEquals('a', min.applyAsChar('a', 'm'));
    }

    @Test
    public void testApplyAsCharWithAnonymousClass() {
        CharBinaryOperator max = new CharBinaryOperator() {
            @Override
            public char applyAsChar(char left, char right) {
                return left > right ? left : right;
            }
        };

        assertEquals('z', max.applyAsChar('a', 'z'));
        assertEquals('m', max.applyAsChar('m', 'a'));
    }

    @Test
    public void testMin() {
        CharBinaryOperator min = (left, right) -> left < right ? left : right;

        assertEquals('a', min.applyAsChar('a', 'z'));
        assertEquals('A', min.applyAsChar('Z', 'A'));
        assertEquals('m', min.applyAsChar('m', 'm'));
    }

    @Test
    public void testMax() {
        CharBinaryOperator max = (left, right) -> left > right ? left : right;

        assertEquals('z', max.applyAsChar('a', 'z'));
        assertEquals('Z', max.applyAsChar('Z', 'A'));
        assertEquals('m', max.applyAsChar('m', 'm'));
    }

    @Test
    public void testFirstNonZero() {
        CharBinaryOperator firstNonZero = (left, right) -> left != '\0' ? left : right;

        assertEquals('a', firstNonZero.applyAsChar('a', 'b'));
        assertEquals('b', firstNonZero.applyAsChar('\0', 'b'));
        assertEquals('\0', firstNonZero.applyAsChar('\0', '\0'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharBinaryOperator max = (left, right) -> left > right ? left : right;

        assertEquals(Character.MAX_VALUE, max.applyAsChar(Character.MIN_VALUE, Character.MAX_VALUE));
        assertEquals(Character.MIN_VALUE, max.applyAsChar(Character.MIN_VALUE, Character.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        CharBinaryOperator max = CharBinaryOperator2025Test::maxChar;

        assertEquals('z', max.applyAsChar('a', 'z'));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharBinaryOperator.class.getAnnotation(FunctionalInterface.class));
    }

    private static char maxChar(char left, char right) {
        return left > right ? left : right;
    }
}
