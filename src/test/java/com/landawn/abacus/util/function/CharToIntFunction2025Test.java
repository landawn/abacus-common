package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharToIntFunction2025Test extends TestBase {

    @Test
    public void testApplyAsInt() {
        CharToIntFunction toInt = value -> (int) value;

        assertEquals(97, toInt.applyAsInt('a'));
        assertEquals(65, toInt.applyAsInt('A'));
        assertEquals(48, toInt.applyAsInt('0'));
    }

    @Test
    public void testApplyAsIntWithLambda() {
        CharToIntFunction toUnicode = value -> (int) value;

        assertEquals(97, toUnicode.applyAsInt('a'));
        assertEquals(122, toUnicode.applyAsInt('z'));
    }

    @Test
    public void testApplyAsIntWithAnonymousClass() {
        CharToIntFunction toDigitValue = new CharToIntFunction() {
            @Override
            public int applyAsInt(char value) {
                return Character.isDigit(value) ? Character.getNumericValue(value) : -1;
            }
        };

        assertEquals(5, toDigitValue.applyAsInt('5'));
        assertEquals(0, toDigitValue.applyAsInt('0'));
        assertEquals(-1, toDigitValue.applyAsInt('a'));
    }

    @Test
    public void testDefault() {
        assertEquals(97, CharToIntFunction.DEFAULT.applyAsInt('a'));
        assertEquals(65, CharToIntFunction.DEFAULT.applyAsInt('A'));
        assertEquals(48, CharToIntFunction.DEFAULT.applyAsInt('0'));
    }

    @Test
    public void testDefaultWithBoundaryValues() {
        assertEquals((int) Character.MAX_VALUE, CharToIntFunction.DEFAULT.applyAsInt(Character.MAX_VALUE));
        assertEquals((int) Character.MIN_VALUE, CharToIntFunction.DEFAULT.applyAsInt(Character.MIN_VALUE));
    }

    @Test
    public void testWideningConversion() {
        CharToIntFunction widen = value -> value;

        assertEquals(97, widen.applyAsInt('a'));
        assertEquals(65535, widen.applyAsInt(Character.MAX_VALUE));
        assertEquals(0, widen.applyAsInt(Character.MIN_VALUE));
    }

    @Test
    public void testCharacterCodePoint() {
        CharToIntFunction toCodePoint = value -> (int) value;

        assertEquals(97, toCodePoint.applyAsInt('a'));
        assertEquals(122, toCodePoint.applyAsInt('z'));
        assertEquals(65, toCodePoint.applyAsInt('A'));
        assertEquals(90, toCodePoint.applyAsInt('Z'));
    }

    @Test
    public void testDigitToValue() {
        CharToIntFunction digitToValue = value -> Character.isDigit(value) ? value - '0' : -1;

        assertEquals(0, digitToValue.applyAsInt('0'));
        assertEquals(5, digitToValue.applyAsInt('5'));
        assertEquals(9, digitToValue.applyAsInt('9'));
        assertEquals(-1, digitToValue.applyAsInt('a'));
    }

    @Test
    public void testWithSpecialCharacters() {
        CharToIntFunction toInt = value -> (int) value;

        assertEquals(10, toInt.applyAsInt('\n'));
        assertEquals(9, toInt.applyAsInt('\t'));
        assertEquals(32, toInt.applyAsInt(' '));
        assertEquals(0, toInt.applyAsInt('\0'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharToIntFunction toInt = value -> (int) value;

        assertEquals((int) Character.MAX_VALUE, toInt.applyAsInt(Character.MAX_VALUE));
        assertEquals((int) Character.MIN_VALUE, toInt.applyAsInt(Character.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        CharToIntFunction toInt = CharToIntFunction2025Test::charToInt;

        assertEquals(97, toInt.applyAsInt('a'));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharToIntFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static int charToInt(char value) {
        return (int) value;
    }
}
