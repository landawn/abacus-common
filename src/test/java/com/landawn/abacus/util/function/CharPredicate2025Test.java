package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        CharPredicate isVowel = c -> "aeiouAEIOU".indexOf(c) >= 0;

        assertTrue(isVowel.test('a'));
        assertFalse(isVowel.test('b'));
        assertTrue(isVowel.test('E'));
    }

    @Test
    public void testTestWithLambda() {
        CharPredicate isDigit = Character::isDigit;

        assertTrue(isDigit.test('5'));
        assertFalse(isDigit.test('a'));
        assertTrue(isDigit.test('0'));
    }

    @Test
    public void testTestWithAnonymousClass() {
        CharPredicate isUpperCase = new CharPredicate() {
            @Override
            public boolean test(char value) {
                return Character.isUpperCase(value);
            }
        };

        assertTrue(isUpperCase.test('A'));
        assertFalse(isUpperCase.test('a'));
    }

    @Test
    public void testOf() {
        CharPredicate original = c -> c > 'a';
        CharPredicate returned = CharPredicate.of(original);

        assertSame(original, returned);
    }

    @Test
    public void testNegate() {
        CharPredicate isDigit = Character::isDigit;
        CharPredicate isNotDigit = isDigit.negate();

        assertFalse(isNotDigit.test('5'));
        assertTrue(isNotDigit.test('a'));
    }

    @Test
    public void testAnd() {
        CharPredicate isLetter = Character::isLetter;
        CharPredicate isUpperCase = Character::isUpperCase;
        CharPredicate combined = isLetter.and(isUpperCase);

        assertTrue(combined.test('A'));
        assertFalse(combined.test('a'));
        assertFalse(combined.test('5'));
    }

    @Test
    public void testAndShortCircuit() {
        final boolean[] secondCalled = {false};
        CharPredicate alwaysFalse = c -> false;
        CharPredicate checkCalled = c -> {
            secondCalled[0] = true;
            return true;
        };

        CharPredicate combined = alwaysFalse.and(checkCalled);
        assertFalse(combined.test('a'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testOr() {
        CharPredicate isDigit = Character::isDigit;
        CharPredicate isLetter = Character::isLetter;
        CharPredicate combined = isDigit.or(isLetter);

        assertTrue(combined.test('5'));
        assertTrue(combined.test('a'));
        assertFalse(combined.test('!'));
    }

    @Test
    public void testOrShortCircuit() {
        final boolean[] secondCalled = {false};
        CharPredicate alwaysTrue = c -> true;
        CharPredicate checkCalled = c -> {
            secondCalled[0] = true;
            return false;
        };

        CharPredicate combined = alwaysTrue.or(checkCalled);
        assertTrue(combined.test('a'));
        assertFalse(secondCalled[0]);
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(CharPredicate.ALWAYS_TRUE.test('a'));
        assertTrue(CharPredicate.ALWAYS_TRUE.test('5'));
        assertTrue(CharPredicate.ALWAYS_TRUE.test('\0'));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(CharPredicate.ALWAYS_FALSE.test('a'));
        assertFalse(CharPredicate.ALWAYS_FALSE.test('5'));
        assertFalse(CharPredicate.ALWAYS_FALSE.test('\0'));
    }

    @Test
    public void testIsZero() {
        assertTrue(CharPredicate.IS_ZERO.test('\0'));
        assertFalse(CharPredicate.IS_ZERO.test('a'));
        assertFalse(CharPredicate.IS_ZERO.test('0'));
    }

    @Test
    public void testNotZero() {
        assertFalse(CharPredicate.NOT_ZERO.test('\0'));
        assertTrue(CharPredicate.NOT_ZERO.test('a'));
        assertTrue(CharPredicate.NOT_ZERO.test('0'));
    }

    @Test
    public void testEqual() {
        CharPredicate equalToA = CharPredicate.equal('a');

        assertTrue(equalToA.test('a'));
        assertFalse(equalToA.test('b'));
        assertFalse(equalToA.test('A'));
    }

    @Test
    public void testNotEqual() {
        CharPredicate notEqualToA = CharPredicate.notEqual('a');

        assertFalse(notEqualToA.test('a'));
        assertTrue(notEqualToA.test('b'));
        assertTrue(notEqualToA.test('A'));
    }

    @Test
    public void testGreaterThan() {
        CharPredicate greaterThanM = CharPredicate.greaterThan('m');

        assertTrue(greaterThanM.test('z'));
        assertFalse(greaterThanM.test('m'));
        assertFalse(greaterThanM.test('a'));
    }

    @Test
    public void testGreaterEqual() {
        CharPredicate greaterEqualM = CharPredicate.greaterEqual('m');

        assertTrue(greaterEqualM.test('z'));
        assertTrue(greaterEqualM.test('m'));
        assertFalse(greaterEqualM.test('a'));
    }

    @Test
    public void testLessThan() {
        CharPredicate lessThanM = CharPredicate.lessThan('m');

        assertFalse(lessThanM.test('z'));
        assertFalse(lessThanM.test('m'));
        assertTrue(lessThanM.test('a'));
    }

    @Test
    public void testLessEqual() {
        CharPredicate lessEqualM = CharPredicate.lessEqual('m');

        assertFalse(lessEqualM.test('z'));
        assertTrue(lessEqualM.test('m'));
        assertTrue(lessEqualM.test('a'));
    }

    @Test
    public void testBetween() {
        CharPredicate betweenAandZ = CharPredicate.between('a', 'z');

        assertTrue(betweenAandZ.test('m'));
        assertFalse(betweenAandZ.test('a'));
        assertFalse(betweenAandZ.test('z'));
        assertFalse(betweenAandZ.test('A'));
    }

    @Test
    public void testBoundaryValues() {
        CharPredicate isMax = CharPredicate.equal(Character.MAX_VALUE);
        CharPredicate isMin = CharPredicate.equal(Character.MIN_VALUE);

        assertTrue(isMax.test(Character.MAX_VALUE));
        assertTrue(isMin.test(Character.MIN_VALUE));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
