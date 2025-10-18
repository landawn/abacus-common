package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharToBooleanFunction2025Test extends TestBase {

    @Test
    public void testApplyAsBoolean() {
        CharToBooleanFunction isVowel = value -> "aeiouAEIOU".indexOf(value) >= 0;

        assertTrue(isVowel.applyAsBoolean('a'));
        assertFalse(isVowel.applyAsBoolean('b'));
        assertTrue(isVowel.applyAsBoolean('E'));
    }

    @Test
    public void testApplyAsBooleanWithLambda() {
        CharToBooleanFunction isDigit = Character::isDigit;

        assertTrue(isDigit.applyAsBoolean('5'));
        assertFalse(isDigit.applyAsBoolean('a'));
        assertTrue(isDigit.applyAsBoolean('0'));
    }

    @Test
    public void testApplyAsBooleanWithAnonymousClass() {
        CharToBooleanFunction isUpperCase = new CharToBooleanFunction() {
            @Override
            public boolean applyAsBoolean(char value) {
                return Character.isUpperCase(value);
            }
        };

        assertTrue(isUpperCase.applyAsBoolean('A'));
        assertFalse(isUpperCase.applyAsBoolean('a'));
    }

    @Test
    public void testDefault() {
        assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('Y'));
        assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('y'));
        assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('1'));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('N'));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('n'));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('0'));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('x'));
    }

    @Test
    public void testDefaultWithBoundaryValues() {
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean(Character.MAX_VALUE));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean(Character.MIN_VALUE));
    }

    @Test
    public void testIsLetter() {
        CharToBooleanFunction isLetter = Character::isLetter;

        assertTrue(isLetter.applyAsBoolean('a'));
        assertTrue(isLetter.applyAsBoolean('Z'));
        assertFalse(isLetter.applyAsBoolean('5'));
        assertFalse(isLetter.applyAsBoolean('!'));
    }

    @Test
    public void testIsDigit() {
        CharToBooleanFunction isDigit = Character::isDigit;

        assertTrue(isDigit.applyAsBoolean('0'));
        assertTrue(isDigit.applyAsBoolean('9'));
        assertFalse(isDigit.applyAsBoolean('a'));
        assertFalse(isDigit.applyAsBoolean('Z'));
    }

    @Test
    public void testIsWhitespace() {
        CharToBooleanFunction isWhitespace = Character::isWhitespace;

        assertTrue(isWhitespace.applyAsBoolean(' '));
        assertTrue(isWhitespace.applyAsBoolean('\t'));
        assertTrue(isWhitespace.applyAsBoolean('\n'));
        assertFalse(isWhitespace.applyAsBoolean('a'));
    }

    @Test
    public void testIsUpperCase() {
        CharToBooleanFunction isUpperCase = Character::isUpperCase;

        assertTrue(isUpperCase.applyAsBoolean('A'));
        assertTrue(isUpperCase.applyAsBoolean('Z'));
        assertFalse(isUpperCase.applyAsBoolean('a'));
        assertFalse(isUpperCase.applyAsBoolean('5'));
    }

    @Test
    public void testIsLowerCase() {
        CharToBooleanFunction isLowerCase = Character::isLowerCase;

        assertTrue(isLowerCase.applyAsBoolean('a'));
        assertTrue(isLowerCase.applyAsBoolean('z'));
        assertFalse(isLowerCase.applyAsBoolean('A'));
        assertFalse(isLowerCase.applyAsBoolean('5'));
    }

    @Test
    public void testInRange() {
        CharToBooleanFunction inRange = value -> value >= 'a' && value <= 'z';

        assertTrue(inRange.applyAsBoolean('m'));
        assertTrue(inRange.applyAsBoolean('a'));
        assertTrue(inRange.applyAsBoolean('z'));
        assertFalse(inRange.applyAsBoolean('A'));
        assertFalse(inRange.applyAsBoolean('5'));
    }

    @Test
    public void testWithSpecialCharacters() {
        CharToBooleanFunction isSpecial = value -> "!@#$%^&*()".indexOf(value) >= 0;

        assertTrue(isSpecial.applyAsBoolean('!'));
        assertTrue(isSpecial.applyAsBoolean('*'));
        assertFalse(isSpecial.applyAsBoolean('a'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharToBooleanFunction isMax = value -> value == Character.MAX_VALUE;
        CharToBooleanFunction isMin = value -> value == Character.MIN_VALUE;

        assertTrue(isMax.applyAsBoolean(Character.MAX_VALUE));
        assertTrue(isMin.applyAsBoolean(Character.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        CharToBooleanFunction isLetter = Character::isLetter;

        assertTrue(isLetter.applyAsBoolean('a'));
        assertFalse(isLetter.applyAsBoolean('5'));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharToBooleanFunction.class.getAnnotation(FunctionalInterface.class));
    }
}
