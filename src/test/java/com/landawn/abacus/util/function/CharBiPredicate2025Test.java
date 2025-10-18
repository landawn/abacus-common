package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharBiPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        CharBiPredicate predicate = (t, u) -> t > u;

        assertTrue(predicate.test('z', 'a'));
        assertFalse(predicate.test('a', 'z'));
        assertFalse(predicate.test('m', 'm'));
    }

    @Test
    public void testTestWithLambda() {
        CharBiPredicate bothVowels = (t, u) -> "aeiou".indexOf(t) >= 0 && "aeiou".indexOf(u) >= 0;

        assertTrue(bothVowels.test('a', 'e'));
        assertFalse(bothVowels.test('a', 'b'));
    }

    @Test
    public void testTestWithAnonymousClass() {
        CharBiPredicate predicate = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t == u;
            }
        };

        assertTrue(predicate.test('a', 'a'));
        assertFalse(predicate.test('a', 'b'));
    }

    @Test
    public void testNegate() {
        CharBiPredicate equal = (t, u) -> t == u;
        CharBiPredicate notEqual = equal.negate();

        assertFalse(notEqual.test('a', 'a'));
        assertTrue(notEqual.test('a', 'b'));
    }

    @Test
    public void testAnd() {
        CharBiPredicate firstIsLetter = (t, u) -> Character.isLetter(t);
        CharBiPredicate secondIsDigit = (t, u) -> Character.isDigit(u);
        CharBiPredicate combined = firstIsLetter.and(secondIsDigit);

        assertTrue(combined.test('a', '5'));
        assertFalse(combined.test('5', '5'));
        assertFalse(combined.test('a', 'b'));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = {false};
        CharBiPredicate alwaysFalse = (t, u) -> false;
        CharBiPredicate checkCalled = (t, u) -> {
            secondCalled[0] = true;
            return true;
        };

        CharBiPredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test('a', 'b'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        CharBiPredicate equal = (t, u) -> t == u;
        CharBiPredicate bothDigits = (t, u) -> Character.isDigit(t) && Character.isDigit(u);
        CharBiPredicate combined = equal.or(bothDigits);

        assertTrue(combined.test('a', 'a'));
        assertTrue(combined.test('5', '9'));
        assertFalse(combined.test('a', 'b'));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = {false};
        CharBiPredicate alwaysTrue = (t, u) -> true;
        CharBiPredicate checkCalled = (t, u) -> {
            secondCalled[0] = true;
            return false;
        };

        CharBiPredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test('a', 'b'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(CharBiPredicate.ALWAYS_TRUE.test('a', 'b'));
        assertTrue(CharBiPredicate.ALWAYS_TRUE.test('\0', '\0'));
        assertTrue(CharBiPredicate.ALWAYS_TRUE.test('Z', 'z'));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(CharBiPredicate.ALWAYS_FALSE.test('a', 'b'));
        assertFalse(CharBiPredicate.ALWAYS_FALSE.test('\0', '\0'));
        assertFalse(CharBiPredicate.ALWAYS_FALSE.test('Z', 'z'));
    }

    @Test
    public void testEqual() {
        assertTrue(CharBiPredicate.EQUAL.test('a', 'a'));
        assertFalse(CharBiPredicate.EQUAL.test('a', 'b'));
        assertTrue(CharBiPredicate.EQUAL.test('\0', '\0'));
    }

    @Test
    public void testNotEqual() {
        assertFalse(CharBiPredicate.NOT_EQUAL.test('a', 'a'));
        assertTrue(CharBiPredicate.NOT_EQUAL.test('a', 'b'));
        assertFalse(CharBiPredicate.NOT_EQUAL.test('\0', '\0'));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(CharBiPredicate.GREATER_THAN.test('z', 'a'));
        assertFalse(CharBiPredicate.GREATER_THAN.test('a', 'z'));
        assertFalse(CharBiPredicate.GREATER_THAN.test('m', 'm'));
    }

    @Test
    public void testGreaterEqual() {
        assertTrue(CharBiPredicate.GREATER_EQUAL.test('z', 'a'));
        assertFalse(CharBiPredicate.GREATER_EQUAL.test('a', 'z'));
        assertTrue(CharBiPredicate.GREATER_EQUAL.test('m', 'm'));
    }

    @Test
    public void testLessThan() {
        assertFalse(CharBiPredicate.LESS_THAN.test('z', 'a'));
        assertTrue(CharBiPredicate.LESS_THAN.test('a', 'z'));
        assertFalse(CharBiPredicate.LESS_THAN.test('m', 'm'));
    }

    @Test
    public void testLessEqual() {
        assertFalse(CharBiPredicate.LESS_EQUAL.test('z', 'a'));
        assertTrue(CharBiPredicate.LESS_EQUAL.test('a', 'z'));
        assertTrue(CharBiPredicate.LESS_EQUAL.test('m', 'm'));
    }

    @Test
    public void testWithBoundaryValues() {
        assertTrue(CharBiPredicate.EQUAL.test(Character.MAX_VALUE, Character.MAX_VALUE));
        assertTrue(CharBiPredicate.EQUAL.test(Character.MIN_VALUE, Character.MIN_VALUE));
        assertTrue(CharBiPredicate.LESS_THAN.test(Character.MIN_VALUE, Character.MAX_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharBiPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
