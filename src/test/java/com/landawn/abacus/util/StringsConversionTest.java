package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Strings conversion contracts, including regression cases. */
public class StringsConversionTest extends StringsTestSupport {

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("abc", Strings.defaultIfEmpty("abc", "default"));
        assertEquals(" ", Strings.defaultIfEmpty(" ", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("abc", ""));
    }

    @Test
    public void testDefaultIfEmpty_Empty() {
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
    }

    @Test
    public void testDefaultIfEmpty_EmptyDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, (String) null));
    }

    @Test
    public void testDefaultIfEmpty_NotEmpty() {
        assertEquals("hello", Strings.defaultIfEmpty("hello", "default"));
        assertEquals("   ", Strings.defaultIfEmpty("   ", "default"));
    }

    @Test
    public void testDefaultIfEmptySupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfEmpty("", supplier));
        assertEquals("abc", Strings.defaultIfEmpty("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", Fn.s(() -> "")));
    }

    @Test
    public void testDefaultIfEmpty_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfEmpty("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty("", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty(null, () -> "default"));
    }

    // defaultIfNull

    @Test
    public void testDefaultIfNull() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
        assertEquals("abc", Strings.defaultIfNull("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("abc", (String) null));
    }

    @Test
    public void testDefaultIfNull_NotNull() {
        assertEquals("hello", Strings.defaultIfNull("hello", "default"));
        assertEquals("", Strings.defaultIfNull("", "default"));
        assertEquals("   ", Strings.defaultIfNull("   ", "default"));
    }

    @Test
    public void testDefaultIfNull_Null() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
    }

    @Test
    public void testDefaultIfNull_NullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    public void testDefaultIfNullSupplier() {
        assertEquals("supplied", Strings.defaultIfNull((String) null, Fn.s(() -> "supplied")));
        assertEquals("abc", Strings.defaultIfNull("abc", Fn.s(() -> "supplied")));
        assertThrows(NullPointerException.class, () -> Strings.defaultIfNull((CharSequence) null, (Supplier<? extends CharSequence>) () -> null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("abc", (Supplier<String>) null));
    }

    // C-004: the supplier-returned-null message names the supplier, not a non-existent 'defaultValue' parameter
    @Test
    public void testC004_defaultIfSupplierMessagesNameTheSupplier() {
        final Supplier<String> nullSupplier = () -> null;

        final NullPointerException npe = assertThrows(NullPointerException.class, () -> Strings.defaultIfNull((String) null, nullSupplier));
        assertEquals("defaultValueSupplier returned null", npe.getMessage());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", (Supplier<String>) () -> ""));
        assertTrue(e.getMessage().contains("defaultValueSupplier"), e.getMessage());
        assertFalse(e.getMessage().contains("'defaultValue'"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank(" ", (Supplier<String>) () -> "  "));
        assertTrue(e.getMessage().contains("defaultValueSupplier"), e.getMessage());

        // the supplier is not consulted for a present value, and a good supplier still works
        assertEquals("x", Strings.defaultIfNull("x", nullSupplier));
        assertEquals("d", Strings.defaultIfBlank("\t", (Supplier<String>) () -> "d"));
    }

    @Test
    public void testC502_supplierOverloadsAreCollaboratorFirst() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("hello", (Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("hello", (Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("hello", (Supplier<String>) null));
        assertEquals("hello", Strings.defaultIfNull("hello", (Supplier<String>) () -> "x"));
        assertEquals("x", Strings.defaultIfBlank("  ", (Supplier<String>) () -> "x"));
    }

    @Test
    public void testDefaultIfNull_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfNull("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfNull(null, () -> "default"));
    }

    @Test
    public void testDefaultIfNull_SupplierNullDefault() {
        assertThrows(NullPointerException.class, () -> Strings.<String> defaultIfNull(null, () -> null));
    }

    @Test
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(""));
        assertNull(Strings.emptyToNull((String) null));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals("   ", Strings.emptyToNull("   "));
    }

    @Test
    public void testEmptyToNullString() {
        assertNull(Strings.emptyToNull((String) null));
        assertNull(Strings.emptyToNull(""));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals(" ", Strings.emptyToNull(" "));
    }

    @Test
    public void testFormatToPercentage() {
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("100.0%", Numbers.format(1.0, "0.0%"));
        assertEquals("0.0%", Numbers.format(0.0, "0.0%"));
        assertEquals("25.5%", Numbers.format(0.255, "0.0%"));

        assertEquals("50%", Numbers.format(0.5, "0%"));
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("51.6%", Numbers.format(0.5156, "0.0%"));
        assertEquals("50.00%", Numbers.format(0.5, "0.00%"));
        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("-25.0%", Numbers.format(-0.25, "0.0%"));
        assertEquals("-25%", Numbers.format(-0.25, "0%"));
    }

    @Test
    public void testFormatToPercentageWithScale() {
        assertEquals("50%", Numbers.format(0.5, "0%"));

        assertEquals("33.3%", Numbers.format(1.0 / 3.0, "0.0%"));

        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("12.3457%", Numbers.format(0.123457, "0.0000%"));

        assertEquals("66.67%", Numbers.format(2.0 / 3.0, "0.00%"));
    }

    // lenientFormat

    @Test
    public void testLenientFormat() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Value: 123", Strings.lenientFormat("Value: %s", 123));
        assertNotNull(Strings.lenientFormat("Test %s %s", "a"));
    }

    @Test
    public void testLenientFormat_arrayArgumentUsesDeepToString() {
        assertEquals("[n, m]", Strings.lenientFormat("%s", (Object) new String[] { "n", "m" }));
        assertEquals("[1, 2]", Strings.lenientFormat("%s", new int[] { 1, 2 }));
        assertEquals("[[1, 2], [3]]", Strings.lenientFormat("%s", (Object) new int[][] { { 1, 2 }, { 3 } }));
        // surplus array arguments are rendered the same way
        assertEquals("x: [[n, m]]", Strings.lenientFormat("x", (Object) new String[] { "n", "m" }));
    }

    @Test
    public void testLenientFormat_ConvertsArgumentsOnceInOrderWithoutMutatingCallerArray() {
        final List<Integer> conversions = new ArrayList<>();
        final Object first = new Object() {
            @Override
            public String toString() {
                conversions.add(1);
                return "first";
            }
        };
        final Object second = new Object() {
            @Override
            public String toString() {
                conversions.add(2);
                return "second";
            }
        };
        final Object third = new Object() {
            @Override
            public String toString() {
                conversions.add(3);
                return "third";
            }
        };
        final Object[] args = { first, second, third };

        assertEquals("first second: [third]", Strings.lenientFormat("%s %s", args));
        assertEquals(List.of(1, 2, 3), conversions);
        assertTrue(args[0] == first);
        assertTrue(args[1] == second);
        assertTrue(args[2] == third);
    }

    @Test
    public void testLenientFormat_EdgeCases() {
        assertEquals("hello world", Strings.lenientFormat("hello %s", "world"));
        assertEquals("hello world 123", Strings.lenientFormat("hello %s %s", "world", 123));
        assertEquals("hello", Strings.lenientFormat("hello"));
        assertEquals("null", Strings.lenientFormat(null));
    }

    @Test
    public void testLenientFormat_ExtraArgs() {
        String result = Strings.lenientFormat("hello %s", "world", "extra");
        assertTrue(result.contains("world"));
        assertTrue(result.contains("extra"));
    }

    @Test
    public void testLenientFormat_FewerArgs() {
        String result = Strings.lenientFormat("hello %s %s", "world");
        assertTrue(result.contains("world"));
        assertTrue(result.contains("%s"));
    }

    @Test
    public void testLenientFormat_FewerArgsThanPlaceholders() {
        String result = Strings.lenientFormat("hello %s %s", "world");
        assertNotNull(result);
        assertTrue(result.contains("world"));
    }

    @Test
    public void testLenientFormat_MoreArgsThanPlaceholders() {
        String result = Strings.lenientFormat("hello %s", "world", "extra");
        assertNotNull(result);
        assertTrue(result.contains("world"));
        assertTrue(result.contains("extra"));
    }

    @Test
    public void testLenientFormat_NoArgs() {
        assertEquals("hello", Strings.lenientFormat("hello"));
    }

    @Test
    public void testLenientFormat_NullTemplate() {
        String result = Strings.lenientFormat(null, "a", "b");
        assertEquals("null: [a, b]", result);
    }

    @Test
    public void testLenientFormat_otherDocumentedBehaviourUnchanged() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Hello New World", Strings.lenientFormat("Hello %s %s", "New", "World"));
        assertEquals("Hello World %s", Strings.lenientFormat("Hello %s %s", "World"));
        assertEquals("Hello (Object[])null", Strings.lenientFormat("Hello %s", (Object[]) null));
        assertEquals("null", Strings.lenientFormat(null));
        assertEquals("", Strings.lenientFormat(""));
        assertEquals("null1", Strings.lenientFormat("%s%s", null, 1));
    }

    // J-3 : lenientFormat differences from Guava
    @Test
    public void testLenientFormat_surplusArgumentsUseColonSeparator() {
        assertEquals("Hello World: [Extra]", Strings.lenientFormat("Hello %s", "World", "Extra"));
        assertEquals("Hello: [World]", Strings.lenientFormat("Hello", "World"));
        assertEquals("x: [1, 2]", Strings.lenientFormat("x", 1, 2));
        assertEquals("null: [arg1, arg2]", Strings.lenientFormat(null, "arg1", "arg2"));
    }

    @Test
    public void testLenientFormatNullArgumentIsRendered() {
        // lenientToString handles null before its try block; its recovery path dereferences the argument, so a
        // null reaching it would throw an NPE out of the catch and defeat the point of "lenient" formatting.
        assertEquals("value=null", Strings.lenientFormat("value=%s", (Object) null));
        assertEquals("a=null b=null", Strings.lenientFormat("a=%s b=%s", null, null));
        assertEquals("none: [null]", Strings.lenientFormat("none", (Object) null));
        assertEquals("null: [null]", Strings.lenientFormat(null, (Object) null));
    }

    @Test
    public void testC567_lenientFormatRecoversFromAThrowingToString() {
        final Object throwing = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        };

        final String result = Strings.lenientFormat("v=%s!", throwing);
        final String expectedPrefix = "v=<" + throwing.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(throwing));

        assertTrue(result.startsWith(expectedPrefix), result);
        assertTrue(result.endsWith(" threw java.lang.IllegalStateException>!"), result);

        // a surplus throwing argument is recovered the same way
        final String surplus = Strings.lenientFormat("x", throwing);
        assertTrue(surplus.startsWith("x: [<"), surplus);
        assertTrue(surplus.endsWith(" threw java.lang.IllegalStateException>]"), surplus);
    }

    @Test
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
        assertEquals("   ", Strings.nullToEmpty("   "));
    }

    @Test
    public void testNullToEmptyString() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
    }

    // toCharArray

    @Test
    public void testToCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        // Empty input returns the shared N.EMPTY_CHAR_ARRAY constant by design.
        assertSame(Strings.toCharArray(""), Strings.toCharArray(""));
        assertNull(Strings.toCharArray(null));
    }

    @Test
    public void testToCharArray_NonStringCharSequence() {
        // CharSequence that is not a String triggers the manual copy path
        StringBuilder sb = new StringBuilder("abc");
        char[] result = Strings.toCharArray(sb);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        // null input
        assertNull(Strings.toCharArray((CharSequence) null));

        // empty CharSequence
        assertArrayEquals(new char[0], Strings.toCharArray(new StringBuilder()));

        // String path (fast path)
        assertArrayEquals(new char[] { 'h', 'i' }, Strings.toCharArray("hi"));
    }

    // G072-03: toCharArray(StringBuilder) bulk copy returns the same fresh array as the charAt loop
    @Test
    public void testToCharArray_stringBuilderAndOtherCharSequences() {
        final String[] inputs = { "a", "ab", "caf\u00E9", "\uD83D\uDE00x\ud800", "The quick brown fox".repeat(100) };

        for (final String input : inputs) {
            final StringBuilder sb = new StringBuilder(input.length() + 16).append(input);
            final char[] fromBuilder = Strings.toCharArray(sb);
            assertArrayEquals(input.toCharArray(), fromBuilder);
            assertTrue(fromBuilder != Strings.toCharArray(sb));
            fromBuilder[0] = '#';
            assertEquals(input, sb.toString());
            assertArrayEquals(input.toCharArray(), Strings.toCharArray(new StringBuffer(input)));
            assertArrayEquals(input.toCharArray(), Strings.toCharArray(java.nio.CharBuffer.wrap(input)));
        }

        assertTrue(CommonUtil.EMPTY_CHAR_ARRAY == Strings.toCharArray(new StringBuilder()));
        assertNull(Strings.toCharArray(null));
    }

    // toCodePoints

    @Test
    public void testToCodePoints() {
        int[] codePoints = Strings.toCodePoints("abc");
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, codePoints);
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        // Empty input returns the shared N.EMPTY_INT_ARRAY constant by design.
        assertSame(Strings.toCodePoints(""), Strings.toCodePoints(""));
        assertNull(Strings.toCodePoints(null));
    }

    @Test
    public void testToCodePoints_MatchesStringCodePoints() {
        final java.util.Random rnd = new java.util.Random(20260902);
        final List<String> samples = new ArrayList<>();

        for (int i = 0; i < 2000; i++) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 0, n = rnd.nextInt(8); j < n; j++) {
                switch (rnd.nextInt(4)) {
                    case 0 -> sb.append((char) ('a' + rnd.nextInt(26)));
                    case 1 -> sb.appendCodePoint(0x1F600 + rnd.nextInt(16));
                    case 2 -> sb.append((char) (0xD800 + rnd.nextInt(0x400)));
                    default -> sb.append((char) (0xDC00 + rnd.nextInt(0x400)));
                }
            }
            samples.add(sb.toString());
        }

        for (final String s : samples) {
            assertArrayEquals(s.codePoints().toArray(), Strings.toCodePoints(new StringBuilder(s)), s);
            assertArrayEquals(s.codePoints().toArray(), Strings.toCodePoints(s), s);
        }
    }

    // D3 : toCodePoints(CharSequence) does not go through toString()
    @Test
    public void testToCodePoints_ReadsCharSequenceDirectly() {
        final StringBuilder sb = new StringBuilder("a😀b\uD83D"); // a, one pair, b, one lone high surrogate

        assertArrayEquals(new int[] { 'a', 0x1F600, 'b', 0xD83D }, Strings.toCodePoints(sb));
        assertArrayEquals(Strings.toCodePoints(sb.toString()), Strings.toCodePoints(sb));
        assertArrayEquals(new int[] { 0xDE00, 0x1F600 }, Strings.toCodePoints("\uDE00😀")); // lone low first
        assertNull(Strings.toCodePoints(null));
        assertSame(CommonUtil.EMPTY_INT_ARRAY, Strings.toCodePoints(""));
        assertSame(CommonUtil.EMPTY_INT_ARRAY, Strings.toCodePoints(new StringBuilder()));

        // A sequence whose toString() disagrees with charAt() proves the content, not toString(), is read.
        final CharSequence lying = new CharSequence() {
            @Override
            public int length() {
                return 2;
            }

            @Override
            public char charAt(final int index) {
                return index == 0 ? 'x' : 'y';
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                return toString().subSequence(start, end);
            }

            @Override
            public String toString() {
                return "ZZZ";
            }
        };

        assertArrayEquals(new int[] { 'x', 'y' }, Strings.toCodePoints(lying));
    }

    @Test
    public void testToUnicodeEscape_CodePoint() {
        assertEquals(Strings.toUnicodeEscape('A'), Strings.toUnicodeEscape(0x41));
        assertEquals("\\u20ac", Strings.toUnicodeEscape(0x20AC));
        assertEquals("\\ud83d\\ude00", Strings.toUnicodeEscape(0x1F600));
        assertEquals(Strings.toUnicodeEscape('\uD800'), Strings.toUnicodeEscape(0xD800));
        assertThrows(IllegalArgumentException.class, () -> Strings.toUnicodeEscape(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.toUnicodeEscape(Character.MAX_CODE_POINT + 1));
    }

    @Test
    public void testUnicodeEscaped() {
        assertEquals("\\u0041", Strings.toUnicodeEscape('A'));
        assertEquals("\\u0061", Strings.toUnicodeEscape('a'));
        assertEquals("\\u0031", Strings.toUnicodeEscape('1'));
    }

    @Test
    public void testUnicodeEscaped_EdgeCases() {
        assertEquals("\\u0041", Strings.toUnicodeEscape('A'));
        assertEquals("\\u0000", Strings.toUnicodeEscape('\0'));
        assertEquals("\\u000a", Strings.toUnicodeEscape('\n'));
        assertEquals("\\u0009", Strings.toUnicodeEscape('\t'));
        // Multi-byte chars
        String escaped = Strings.toUnicodeEscape('\u00E9'); // e with acute
        assertTrue(escaped.startsWith("\\u"));
    }

    // valueOf

    @Test
    public void testValueOf() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("hello", Strings.valueOf(new char[] { 'h', 'e', 'l', 'l', 'o' }));
    }

    @Test
    public void testValueOf_CharArray() {
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
        assertEquals("Hello World", Strings.valueOf("Hello World".toCharArray()));
    }

    @Test
    public void testValueOf_EmptyArray() {
        assertEquals("", Strings.valueOf(new char[0]));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(Strings.valueOf(null));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
    }

}
