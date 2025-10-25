package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharTriPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        CharTriPredicate allLetters = (a, b, c) -> Character.isLetter(a) && Character.isLetter(b) && Character.isLetter(c);

        assertTrue(allLetters.test('a', 'b', 'c'));
        assertFalse(allLetters.test('a', '5', 'c'));
        assertFalse(allLetters.test('1', 'b', 'c'));
    }

    @Test
    public void testTestWithLambda() {
        CharTriPredicate allSame = (a, b, c) -> a == b && b == c;

        assertTrue(allSame.test('a', 'a', 'a'));
        assertFalse(allSame.test('a', 'a', 'b'));
    }

    @Test
    public void testTestWithAnonymousClass() {
        CharTriPredicate predicate = new CharTriPredicate() {
            @Override
            public boolean test(char a, char b, char c) {
                return a == b && b == c;
            }
        };

        assertTrue(predicate.test('x', 'x', 'x'));
        assertFalse(predicate.test('x', 'x', 'y'));
    }

    @Test
    public void testNegate() {
        CharTriPredicate allSame = (a, b, c) -> a == b && b == c;
        CharTriPredicate notAllSame = allSame.negate();

        assertFalse(notAllSame.test('a', 'a', 'a'));
        assertTrue(notAllSame.test('a', 'a', 'b'));
    }

    @Test
    public void testAnd() {
        CharTriPredicate allLetters = (a, b, c) -> Character.isLetter(a) && Character.isLetter(b) && Character.isLetter(c);
        CharTriPredicate allLowerCase = (a, b, c) -> Character.isLowerCase(a) && Character.isLowerCase(b) && Character.isLowerCase(c);
        CharTriPredicate combined = allLetters.and(allLowerCase);

        assertTrue(combined.test('a', 'b', 'c'));
        assertFalse(combined.test('A', 'b', 'c'));
        assertFalse(combined.test('1', 'b', 'c'));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = { false };
        CharTriPredicate alwaysFalse = (a, b, c) -> false;
        CharTriPredicate checkCalled = (a, b, c) -> {
            secondCalled[0] = true;
            return true;
        };

        CharTriPredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test('a', 'b', 'c'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        CharTriPredicate hasDigit = (a, b, c) -> Character.isDigit(a) || Character.isDigit(b) || Character.isDigit(c);
        CharTriPredicate allSame = (a, b, c) -> a == b && b == c;
        CharTriPredicate combined = hasDigit.or(allSame);

        assertTrue(combined.test('a', '5', 'c'));
        assertTrue(combined.test('x', 'x', 'x'));
        assertFalse(combined.test('a', 'b', 'c'));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = { false };
        CharTriPredicate alwaysTrue = (a, b, c) -> true;
        CharTriPredicate checkCalled = (a, b, c) -> {
            secondCalled[0] = true;
            return false;
        };

        CharTriPredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test('a', 'b', 'c'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(CharTriPredicate.ALWAYS_TRUE.test('a', 'b', 'c'));
        assertTrue(CharTriPredicate.ALWAYS_TRUE.test('\0', '\0', '\0'));
        assertTrue(CharTriPredicate.ALWAYS_TRUE.test('X', 'Y', 'Z'));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(CharTriPredicate.ALWAYS_FALSE.test('a', 'b', 'c'));
        assertFalse(CharTriPredicate.ALWAYS_FALSE.test('\0', '\0', '\0'));
        assertFalse(CharTriPredicate.ALWAYS_FALSE.test('X', 'Y', 'Z'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharTriPredicate allSame = (a, b, c) -> a == b && b == c;

        assertFalse(allSame.test(Character.MIN_VALUE, 'M', Character.MAX_VALUE));
        assertTrue(allSame.test(Character.MAX_VALUE, Character.MAX_VALUE, Character.MAX_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharTriPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
