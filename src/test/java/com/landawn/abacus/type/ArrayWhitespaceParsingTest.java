package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Array;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ArrayWhitespaceParsingTest extends TestBase {
    @Test
    void mixedSeparatorsKeepEveryNumericAndBooleanElement() {
        assertArrayEquals(new int[] { 1, 2, 3 }, TypeFactory.<int[]> getType(int[].class).valueOf("[1,2, 3]"));
        assertArrayEquals(new boolean[] { true, false, true }, TypeFactory.<boolean[]> getType(boolean[].class).valueOf("[true,false, true]"));
        assertArrayEquals(new Integer[] { 1, null, 3 }, TypeFactory.<Integer[]> getType(Integer[].class).valueOf("[ 1, null,\t3 ]"));
        assertArrayEquals(new Boolean[] { true, null, false }, TypeFactory.<Boolean[]> getType(Boolean[].class).valueOf("[true, null,\n false]"));
        for (Class<?> arrayClass : new Class<?>[] { byte[].class, short[].class, int[].class, long[].class, float[].class, double[].class, Byte[].class,
                Short[].class, Integer[].class, Long[].class, Float[].class, Double[].class }) {
            Type<?> type = TypeFactory.getType(arrayClass);
            Object array = type.valueOf(" \t[ -1,0, 1,\n2\u2003] \n");
            assertEquals(4, Array.getLength(array), type.name());
            for (int i = 0; i < 4; i++) {
                assertEquals(i - 1, ((Number) Array.get(array, i)).intValue());
            }
        }
    }

    @Test
    void structuralWhitespaceDoesNotAlterQuotedCharacters() {
        Type<char[]> chars = TypeFactory.getType(char[].class);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, chars.valueOf("['a', 'b',  'c']"));
        assertArrayEquals(new char[] { ',', ' ', '\t', '\'', '\\', '\u6C49' }, chars.valueOf("[ ',',  ' ', '\\t', '\\'', '\\\\', '\\u6C49' ]"));
        Type<Character[]> boxed = TypeFactory.getType(Character[].class);
        assertArrayEquals(new Character[] { 'a', null, ' ', ',' }, boxed.valueOf("[ 'a', null,  ' ', ',' ]"));
        char[] boundaries = { 0, Character.MAX_VALUE, '\uD83D', '\uDE00', '"', '\n' };
        assertArrayEquals(boundaries, chars.valueOf(chars.stringOf(boundaries)));
    }

    @Test
    void nullBlankAndEmptyArraysRemainDistinct() {
        for (Class<?> arrayClass : new Class<?>[] { boolean[].class, byte[].class, char[].class, short[].class, int[].class, long[].class, float[].class,
                double[].class, Boolean[].class, Byte[].class, Character[].class, Short[].class, Integer[].class, Long[].class, Float[].class,
                Double[].class }) {
            Type<?> type = TypeFactory.getType(arrayClass);
            assertNull(type.valueOf((String) null));
            assertNull(type.valueOf(""));
            for (String input : new String[] { "[]", "[ ]", "[\t\n\u2003]", "  []  " }) {
                assertEquals(0, Array.getLength(type.valueOf(input)), type.name() + ": " + input);
            }
            for (String input : new String[] { "[,1]", "[1,,2]", "[1, ]" }) {
                assertThrows(IllegalArgumentException.class, () -> type.valueOf(input), type.name() + ": " + input);
            }
        }
    }

}
