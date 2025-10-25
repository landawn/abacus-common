package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharTernaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsChar() {
        CharTernaryOperator max = (a, b, c) -> (char) Math.max(a, Math.max(b, c));

        assertEquals('z', max.applyAsChar('a', 'z', 'm'));
        assertEquals('Z', max.applyAsChar('A', 'B', 'Z'));
    }

    @Test
    public void testApplyAsCharWithLambda() {
        CharTernaryOperator min = (a, b, c) -> (char) Math.min(a, Math.min(b, c));

        assertEquals('a', min.applyAsChar('a', 'z', 'm'));
        assertEquals('A', min.applyAsChar('A', 'B', 'Z'));
    }

    @Test
    public void testApplyAsCharWithAnonymousClass() {
        CharTernaryOperator middle = new CharTernaryOperator() {
            @Override
            public char applyAsChar(char a, char b, char c) {
                if ((a >= b && a <= c) || (a >= c && a <= b))
                    return a;
                if ((b >= a && b <= c) || (b >= c && b <= a))
                    return b;
                return c;
            }
        };

        assertEquals('m', middle.applyAsChar('a', 'm', 'z'));
        assertEquals('m', middle.applyAsChar('z', 'm', 'a'));
    }

    @Test
    public void testMin() {
        CharTernaryOperator min = (a, b, c) -> (char) Math.min(a, Math.min(b, c));

        assertEquals('a', min.applyAsChar('z', 'a', 'm'));
        assertEquals('A', min.applyAsChar('Z', 'M', 'A'));
    }

    @Test
    public void testMax() {
        CharTernaryOperator max = (a, b, c) -> (char) Math.max(a, Math.max(b, c));

        assertEquals('z', max.applyAsChar('a', 'z', 'm'));
        assertEquals('Z', max.applyAsChar('A', 'M', 'Z'));
    }

    @Test
    public void testFirstNonZero() {
        CharTernaryOperator firstNonZero = (a, b, c) -> a != '\0' ? a : (b != '\0' ? b : c);

        assertEquals('a', firstNonZero.applyAsChar('a', 'b', 'c'));
        assertEquals('b', firstNonZero.applyAsChar('\0', 'b', 'c'));
        assertEquals('c', firstNonZero.applyAsChar('\0', '\0', 'c'));
        assertEquals('\0', firstNonZero.applyAsChar('\0', '\0', '\0'));
    }

    @Test
    public void testWithSpecialCharacters() {
        CharTernaryOperator max = (a, b, c) -> (char) Math.max(a, Math.max(b, c));

        assertEquals(' ', max.applyAsChar('\n', '\t', ' '));
    }

    @Test
    public void testWithBoundaryValues() {
        CharTernaryOperator max = (a, b, c) -> (char) Math.max(a, Math.max(b, c));

        assertEquals(Character.MAX_VALUE, max.applyAsChar(Character.MIN_VALUE, 'M', Character.MAX_VALUE));
        assertEquals(Character.MIN_VALUE, max.applyAsChar(Character.MIN_VALUE, Character.MIN_VALUE, Character.MIN_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharTernaryOperator.class.getAnnotation(FunctionalInterface.class));
    }
}
