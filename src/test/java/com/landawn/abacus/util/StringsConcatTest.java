package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.concat;
import static com.landawn.abacus.util.Strings.concatNullToEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class StringsConcatTest extends StringsTestSupport {

    @Test
    public void testConcat() {
        assertEquals("abcdef", Strings.concat("abc", "def"));
        assertEquals("HelloWorld", concat("Hello", "World"));
        assertEquals("Hello", concat("Hello", ""));
        assertEquals("World", concat("", "World"));
        assertEquals("", concat("", ""));
        assertEquals("ab", concat("a", "b"));

        assertEquals("abc", Strings.concat("a", "b", "c"));
        assertEquals("HelloWorldTest", concat("Hello", "World", "Test"));
        assertEquals("HelloWorld", concat("Hello", "World", ""));
        assertEquals("HelloTest", concat("Hello", "", "Test"));

        assertEquals("Hello World!!", Strings.concat("Hello", " ", "World", "!", "!"));
        assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        assertEquals("AC", Strings.concat("A", "", "", "", "C"));
        assertEquals("abcde", concat("a", "b", "c", "d", "e"));

        assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        assertEquals("123456", Strings.concat("1", "2", "3", "4", "5", "6"));
        assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));

        assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        assertEquals("abcdefghi", concat("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", "9", "!", "!"));
    }

    @Test
    public void testConcat_Objects() {
        assertEquals("Hello123", Strings.concat("Hello", 123));
        assertEquals("42 is the answer", Strings.concat(42, " is the answer"));
        assertEquals("3.14true", Strings.concat(3.14, true));
        assertEquals("12", concat(1, 2));
        assertEquals("true42", concat(true, 42));
        assertEquals("1null3", concat(1, null, 3));
        assertEquals("truenull", Strings.concat(true, null));

        assertEquals("Hello World", Strings.concat("Hello", " ", "World"));
        assertEquals("123", Strings.concat(1, 2, 3));
        assertEquals("Value: 42!", Strings.concat("Value: ", 42, "!"));

        assertEquals("ABCD", Strings.concat("A", "B", "C", "D"));
        assertEquals("1+2=3", Strings.concat(1, "+", 2, "=3"));
        assertEquals("Result: 10 out of 20", Strings.concat("Result: ", 10, " out of ", 20));

        assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        assertEquals("1 2 3", Strings.concat(1, " ", 2, " ", 3));
        assertEquals("Sum of 1+2=3", Strings.concat("Sum of ", 1, "+", 2, "=3"));

        assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        assertEquals("123456", Strings.concat(1, 2, 3, 4, 5, 6));
        assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        assertEquals("1234567", Strings.concat(1, 2, 3, 4, 5, 6, 7));
        assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        assertEquals("12345678", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8));
        assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        assertEquals("123456789", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void testConcatNullToEmpty() {
        assertEquals("", Strings.concatNullToEmpty());
        assertEquals("A", Strings.concatNullToEmpty("A"));
        assertEquals("abc", Strings.concatNullToEmpty("abc", null));
        assertEquals("def", Strings.concatNullToEmpty(null, "def"));
        assertEquals("", Strings.concatNullToEmpty((String[]) null));
        assertEquals("a", Strings.concatNullToEmpty("a", null, ""));
        assertEquals("", Strings.concatNullToEmpty(null, null));
        assertEquals("Hello", concatNullToEmpty("Hello", null));
        assertEquals("World", concatNullToEmpty(null, "World"));
        assertEquals("Hello", concatNullToEmpty("Hello", null, null));
        assertEquals("", concatNullToEmpty(null, null, null));
        assertEquals("123", Strings.concatNullToEmpty("1", null, "2", null, "3"));
        assertEquals("", Strings.concatNullToEmpty(null, null, null, null, null));
        assertEquals("", Strings.concatNullToEmpty(null, null, null, null, null, null));
        assertEquals("abc", concatNullToEmpty("a", null, "b", null, "c"));
        assertEquals("", concatNullToEmpty(new String[] {}));
        assertEquals("", concatNullToEmpty((String[]) null));
        assertEquals("b", concatNullToEmpty(null, "b"));

        assertEquals("Hello World", Strings.concatNullToEmpty(new String[] { "Hello", " ", "World" }));
        assertEquals("ABCD", Strings.concatNullToEmpty(new String[] { "A", "B", "C", "D" }));
        assertEquals("HelloWorld", Strings.concatNullToEmpty(new String[] { "Hello", null, "World" }));
        assertEquals("", Strings.concatNullToEmpty(new String[] { null, null }));
        assertEquals("", Strings.concatNullToEmpty(new String[0]));
        assertEquals("A", Strings.concatNullToEmpty(new String[] { "A" }));
        assertEquals("AB", Strings.concatNullToEmpty(new String[] { "A", "B" }));
        assertEquals("ABC", Strings.concatNullToEmpty(new String[] { "A", "B", "C" }));

        final String[] largeArray = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        assertEquals("ABCDEFGHIJ", Strings.concatNullToEmpty(largeArray));
        final String[] mixedArray = { "A", null, "B", null, "C", "D", "E", "F", "G", "H" };
        assertEquals("ABCDEFGH", Strings.concatNullToEmpty(mixedArray));

        for (int i = 2; i <= 7; i++) {
            final String[] arr = new String[i];
            Arrays.fill(arr, "X");
            assertEquals("X".repeat(i), Strings.concatNullToEmpty(arr));
        }
    }

    @Test
    public void testConcatAndCapacityOptimizations_AllDispatchAndOverflowBoundaries() {
        final String[] alphabet = { null, "", "a", "\uD83D\uDE00", "xyz" };

        for (int length = 0; length <= 32; length++) {
            final String[] values = new String[length];
            for (int i = 0; i < length; i++) {
                values[i] = alphabet[(length * 3 + i) % alphabet.length];
            }
            assertEquals(concatNullToEmptyReference(values), Strings.concatNullToEmpty(values), "length=" + length);
        }

        final int[] counts = { Integer.MIN_VALUE, -1, 0, 1, 2, 17, Integer.MAX_VALUE };
        final long[] lengths = { Long.MIN_VALUE, -1, 0, 1, 31, 1_048_576, Integer.MAX_VALUE, Long.MAX_VALUE };
        final int[] affixLengths = { Integer.MIN_VALUE, -1, 0, 1, 31, 1_048_576, Integer.MAX_VALUE };

        for (final int count : counts) {
            for (final long elementLength : lengths) {
                for (final long delimiterLength : lengths) {
                    for (final int prefixLength : affixLengths) {
                        for (final int suffixLength : new int[] { -1, 0, 7, Integer.MAX_VALUE }) {
                            assertEquals(capacityReference(count, elementLength, delimiterLength, prefixLength, suffixLength),
                                    Strings.calculateBufferSize(count, elementLength, delimiterLength, prefixLength, suffixLength));
                        }
                    }
                }
            }
        }
    }
}
