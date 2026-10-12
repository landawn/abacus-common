package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.concatNullToEmpty;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.joinEntries;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.reverseDelimited;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Strings transformation contracts, including regression cases. */
public class StringsTransformationTest extends StringsTestSupport {

    private static final char COMBINING_ACUTE = (char) 0x0301;

    private static final char LOW_WOMAN = (char) 0xDC69;

    private static final char LOW_GIRL = (char) 0xDC67;

    private static final char HIGH_FLAG = (char) 0xD83C;

    private static final char LOW_RI_U = (char) 0xDDFA;

    private static final char LOW_RI_S = (char) 0xDDF8;

    protected static String reverseDelimitedReference(final String str, final char delimiter) {
        final String[] parts = Strings.splitPreserveAllTokens(str, delimiter);
        final StringBuilder result = new StringBuilder(str.length());

        for (int i = parts.length - 1; i >= 0; i--) {
            if (i < parts.length - 1) {
                result.append(delimiter);
            }

            result.append(parts[i]);
        }

        return result.toString();
    }

    @Test
    public void testClusterMethodsNullAndEmpty() {
        assertEquals(null, Strings.reverse(null));
        assertEquals(null, Strings.rotate(null, 1));
        assertEquals(null, Strings.sort(null));
        assertEquals(null, Strings.shuffle(null));
        assertEquals("", Strings.reverse(""));
        assertEquals("", Strings.rotate("", 1));
        assertEquals("", Strings.sort(""));
        assertEquals("", Strings.shuffle(""));
        // a single supplementary character is one code point, so all four are no-ops
        String grin = "" + HIGH_EMOJI + (char) 0xDE00;
        assertEquals(grin, Strings.reverse(grin));
        assertEquals(grin, Strings.rotate(grin, 1));
        assertEquals(grin, Strings.sort(grin));
        assertEquals(grin, Strings.shuffle(grin));
    }

    // reverse

    @Test
    public void testReverse() {
        assertEquals("cba", Strings.reverse("abc"));
        assertEquals("", Strings.reverse(""));
        assertNull(Strings.reverse(null));
    }

    @Test
    public void testReverse_SurrogatePairsPreserved() {
        // Single supplementary code-point should NOT be split into two malformed surrogates.
        // U+1F600 = grinning face emoji.
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji + "b";
        final String reversed = Strings.reverse(input);
        assertEquals("b" + emoji + "a", reversed);
        // Round-trip
        assertEquals(input, Strings.reverse(reversed));
    }

    @Test
    public void testC530_reverseCanFuseLoneSurrogates() {
        final String x = "a\uDE00\uD83Db";
        final String reversed = Strings.reverse(x);
        assertEquals("b\uD83D\uDE00a", reversed);
        assertEquals(4, x.codePointCount(0, x.length()));
        assertEquals(3, reversed.codePointCount(0, reversed.length()));
        assertNotEquals(x, Strings.reverse(reversed));
        assertEquals("a\uD83D\uDE00b", Strings.reverse(reversed));

        // well-formed input round-trips by code point
        assertEquals("b" + EMOJI + "a", Strings.reverse("a" + EMOJI + "b"));
        assertEquals("a" + EMOJI + "b", Strings.reverse(Strings.reverse("a" + EMOJI + "b")));
    }

    @Test
    public void testReverseKeepsSurrogatePairsButBreaksGraphemeClusters() {
        // Surrogate pairs survive (this part is, and stays, documented).
        assertEquals("" + HIGH_EMOJI + LOW_MAN + "a", Strings.reverse("a" + HIGH_EMOJI + LOW_MAN));

        // Combining sequence: the mark ends up in front of a different base (here, of nothing).
        assertEquals(COMBINING_ACUTE + "efac", Strings.reverse("cafe" + COMBINING_ACUTE));

        // Regional-indicator pair: reversing turns one flag into another.
        assertEquals("" + HIGH_FLAG + LOW_RI_S + HIGH_FLAG + LOW_RI_U, Strings.reverse("" + HIGH_FLAG + LOW_RI_U + HIGH_FLAG + LOW_RI_S));

        // ZWJ sequence: the joiners survive but the members are re-ordered.
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;
        assertEquals("" + HIGH_EMOJI + LOW_GIRL + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_MAN, Strings.reverse(family));
    }

    // reverseDelimited

    @Test
    public void testReverseDelimited() {
        assertEquals("c,b,a", Strings.reverseDelimited("a,b,c", ','));
        assertNull(Strings.reverseDelimited(null, ','));
    }



    /**
     * reverseDelimited(String, String) validates its required delimiter before short-circuiting on a
     * null/empty/single-character input, like split/splitPreserveAllTokens/mapWords/capitalizeWords.
     */
    @Test
    public void testReverseDelimited_delimiterValidatedBeforeInputShortCircuit() {
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited(null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("ab", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited(null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", ""));

        // A valid delimiter still short-circuits on null/empty/single-character input.
        assertNull(Strings.reverseDelimited(null, " "));
        assertEquals("", Strings.reverseDelimited("", " "));
        assertEquals("a", Strings.reverseDelimited("a", " "));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
        assertEquals("C::B::A", Strings.reverseDelimited("A::B::C", "::"));

        // The char overload has no required-argument check and is unaffected.
        assertNull(Strings.reverseDelimited(null, '.'));
        assertEquals("a", Strings.reverseDelimited("a", '.'));
    }

    @Test
    public void testReverseDelimited_StringDelimiter() {
        assertNull(Strings.reverseDelimited(null, "."));
        assertEquals("", Strings.reverseDelimited("", "."));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
        assertEquals("abc", Strings.reverseDelimited("abc", "."));
    }

    @Test
    public void testReverseDelimitedAndConcatenationOptimizations_EdgeAndRandomizedCoverage() {
        final char delimiter = '|';
        final char[] alphabet = { 'a', 'b', ' ', delimiter, '\u0000', '\uD83D', '\uDE00' };
        final Random random = new Random(20260828L);

        for (int iteration = 0; iteration < 500; iteration++) {
            final String input = randomString(random, alphabet, random.nextInt(81));
            assertEquals(reverseDelimitedReference(input, delimiter), Strings.reverseDelimited(input, delimiter), "iteration " + iteration);
            assertEquals(reverseDelimitedReference(input, delimiter), Strings.reverseDelimited(input, String.valueOf(delimiter)), "iteration " + iteration);
        }

        final String noDelimiter = "unchanged";
        assertTrue(noDelimiter == Strings.reverseDelimited(noDelimiter, delimiter));
        assertEquals("|a||", Strings.reverseDelimited("||a|", delimiter));
        assertEquals("c::b::a", Strings.reverseDelimited("a::b::c", "::"));

        assertEquals("abc", Strings.concatNullToEmpty("a", null, "b", "", "c"));
        assertEquals("abcdefghi", Strings.concatNullToEmpty("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        final String[] many = { null, "a", "", "b", null, "c", "d", "", "e", "f", null, "g" };
        assertEquals(concatNullToEmptyReference(many), Strings.concatNullToEmpty(many));
        assertEquals("", Strings.concatNullToEmpty(new String[12]));

        final String emoji = new String(Character.toChars(0x1F600));
        assertEquals(emoji + emoji + "x", Strings.padStart("x", 4, emoji));
        assertEquals("x" + emoji + emoji, Strings.padEnd("x", 4, emoji));
        assertEquals("<ab::ab::ab>", Strings.repeat("ab", 3, "::", "<", ">"));
        assertEquals("[true||false||true]", Strings.join(new boolean[] { true, false, true }, 0, 3, "||", "[", "]"));
        assertEquals("[ a ||null|| b ]", Strings.join(new Object[] { " a ", null, " b " }, 0, 3, "||", "[", "]", false));
        final Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put("x", 1);
        entries.put("y", 2);
        assertEquals("x=1, y=2", Strings.joinEntries(entries, ", ", "="));
        assertEquals("A 1: [2]", Strings.lenientFormat("%s %s", "A", 1, 2));
        assertEquals("b" + emoji + "a", Strings.reverse("a" + emoji + "b"));
    }

    @Test
    public void testReverseDelimitedChar() {
        assertNull(Strings.reverseDelimited(null, '.'));

        assertEquals("", Strings.reverseDelimited("", '.'));

        assertEquals("a", Strings.reverseDelimited("a", '.'));

        assertEquals("abc", Strings.reverseDelimited("abc", '.'));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", '.'));

        assertEquals("d.c.b.a", Strings.reverseDelimited("a.b.c.d", '.'));

        assertEquals("String.lang.java", Strings.reverseDelimited("java.lang.String", '.'));

        assertEquals("b..a", Strings.reverseDelimited("a..b", '.'));
        assertEquals(".b.a.", Strings.reverseDelimited(".a.b.", '.'));
        assertEquals("b.a.", Strings.reverseDelimited(".a.b", '.'));
        assertEquals(".b.a", Strings.reverseDelimited("a.b.", '.'));
        assertEquals("..", Strings.reverseDelimited("..", '.'));

        final String withEmptySegments = ".a..b.";
        assertEquals(withEmptySegments, Strings.reverseDelimited(Strings.reverseDelimited(withEmptySegments, '.'), '.'));
    }

    @Test
    public void testC531_reverseDelimitedSelfOverlappingDelimiter() {
        assertEquals(" :::", Strings.reverseDelimited(":: :", "::"));
        assertEquals("::: ", Strings.reverseDelimited(" :::", "::"));
        assertNotEquals(":: :", Strings.reverseDelimited(Strings.reverseDelimited(":: :", "::"), "::"));

        // the char overload is an involution
        assertEquals(":: :", Strings.reverseDelimited(Strings.reverseDelimited(":: :", ':'), ':'));
        assertEquals("::a::::b::", Strings.reverseDelimited(Strings.reverseDelimited("::a::::b::", "::"), "::"));
    }

    @Test
    public void testReverseDelimitedString() {
        assertNull(Strings.reverseDelimited(null, "."));

        assertEquals("", Strings.reverseDelimited("", "."));

        assertEquals("a", Strings.reverseDelimited("a", "."));

        assertEquals("abc", Strings.reverseDelimited("abc", "."));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));

        assertEquals("c::b::a", Strings.reverseDelimited("a::b::c", "::"));

        assertEquals("world|hello", Strings.reverseDelimited("hello|world", "|"));

        assertEquals("b..a", Strings.reverseDelimited("a..b", "."));
        assertEquals("::b::a::", Strings.reverseDelimited("::a::b::", "::"));
        assertEquals("b::::a", Strings.reverseDelimited("a::::b", "::"));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", ""));

        final String withEmptySegments = "::a::::b::";
        assertEquals(withEmptySegments, Strings.reverseDelimited(Strings.reverseDelimited(withEmptySegments, "::"), "::"));
    }

    @Test
    public void testReverseDelimited_Char() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", '.'));
        assertEquals("a.b.c", reverseDelimited("a.b.c", 'x'));
        assertEquals("", reverseDelimited("", '.'));
        assertNull(reverseDelimited(null, '.'));
        assertEquals("a", reverseDelimited("a", '.'));
    }

    @Test
    public void testReverseDelimited_String() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", "."));
        assertEquals("789->456->123", reverseDelimited("123->456->789", "->"));
        assertEquals("a.b.c", reverseDelimited("a.b.c", "xyz"));
        assertEquals("", reverseDelimited("", "."));
        assertNull(reverseDelimited(null, "."));
    }

    // rotate

    @Test
    public void testRotate() {
        assertEquals("cab", Strings.rotate("abc", 1));
        assertEquals("bca", Strings.rotate("abc", -1));
        assertEquals("abc", Strings.rotate("abc", 3));
        assertNull(Strings.rotate(null, 1));
    }

    @Test
    public void testRotate_LargeAndNegativeShifts() {
        assertEquals("abcdefg", Strings.rotate("abcdefg", 0));
        assertEquals("abcdefg", Strings.rotate("abcdefg", 7));
        assertEquals("abcdefg", Strings.rotate("abcdefg", -7));
        assertEquals("fgabcde", Strings.rotate("abcdefg", 2));
        assertEquals("cdefgab", Strings.rotate("abcdefg", -2));
        assertEquals("fgabcde", Strings.rotate("abcdefg", 9));
        assertEquals("cdefgab", Strings.rotate("abcdefg", -9));
        assertNull(Strings.rotate(null, 5));
        assertEquals("", Strings.rotate("", 5));
    }

    @Test
    public void testRotate_String() {
        assertNull(Strings.rotate(null, 1));
        assertEquals("", Strings.rotate("", 1));
        assertEquals("oHell", Strings.rotate("Hello", 1));
        assertEquals("elloH", Strings.rotate("Hello", -1));
    }

    @Test
    public void testRotateAndSortBreakGraphemeClusters() {
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;

        // rotate moves one code point, leaving a dangling ZWJ at the end.
        assertEquals("" + HIGH_EMOJI + LOW_GIRL + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ, Strings.rotate(family, 1));

        // sort orders by code point, so the two joiners migrate to the front.
        assertEquals("" + ZWJ + ZWJ + HIGH_EMOJI + LOW_GIRL + HIGH_EMOJI + LOW_MAN + HIGH_EMOJI + LOW_WOMAN, Strings.sort(family));

        // ... while still never splitting a surrogate pair.
        assertEquals(COMBINING_ACUTE + "cafe", Strings.rotate("cafe" + COMBINING_ACUTE, 1));
    }

    @Test
    public void testRotate_SupplementaryCharactersPreserved() {
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji + "b";

        assertEquals("ba" + emoji, rotate(input, 1));
        assertEquals(emoji + "b" + "a", rotate(input, -1));
        assertEquals(emoji + "b" + "a", rotate(input, 2));
        assertEquals(input, rotate(input, 3));
        assertEquals(input, rotate(input, -3));
        assertEquals("ba" + emoji, rotate(input, Integer.MIN_VALUE));
    }

    @Test
    public void testShuffle() {
        String shuffled = Strings.shuffle("abcdef");
        assertNotNull(shuffled);
        assertEquals(6, shuffled.length());
        assertNull(Strings.shuffle(null));
    }

    @Test
    public void testShuffleIsAPermutationOfCodePointsOnly() {
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;
        String shuffled = Strings.shuffle(family, new java.util.Random(7));

        int[] before = family.codePoints().sorted().toArray();
        int[] after = shuffled.codePoints().sorted().toArray();
        assertTrue(java.util.Arrays.equals(before, after), "shuffle must permute the same code points");
        assertEquals(family.length(), shuffled.length());
    }

    @Test
    public void testShuffle_SupplementaryCharactersPreserved() {
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji;
        final Random splitPairIfCodeUnitBased = new Random(0) {
            @Override
            public int nextInt(final int bound) {
                return bound > 1 ? 1 : 0;
            }
        };

        assertEquals(input, shuffle(input, splitPairIfCodeUnitBased));
        final Random unusedForNoOp = new Random(0) {
            @Override
            public int nextInt(final int bound) {
                throw new AssertionError("A no-op shuffle must not consume randomness");
            }
        };
        // The generator is required even when there is at most one Unicode code point to shuffle.
        for (final String noOp : new String[] { null, "", "a", emoji }) {
            assertEquals(noOp, shuffle(noOp, unusedForNoOp));
            assertThrows(IllegalArgumentException.class, () -> shuffle(noOp, null));
        }
        assertThrows(IllegalArgumentException.class, () -> shuffle("ab", null));
    }

    @Test
    public void testShuffle_WithRandom() {
        Random rnd = new Random(12345);
        String original = "abcdefghijk";
        String shuffled = shuffle(original, rnd);
        assertEquals(original.length(), shuffled.length());

        for (char c : original.toCharArray()) {
            assertTrue(shuffled.indexOf(c) >= 0);
        }
    }

    @Test
    public void testSort() {
        assertEquals("abc", Strings.sort("cba"));
        assertEquals("", Strings.sort(""));
        assertNull(Strings.sort(null));
    }

    @Test
    public void testSort_String() {
        assertNull(Strings.sort(null));
        assertEquals("", Strings.sort(""));
        assertEquals("abcde", Strings.sort("edcba"));
        assertEquals("aabbc", Strings.sort("abcba"));
    }

    @Test
    public void testSort_SupplementaryCharactersPreserved() {
        final String grinningFace = new String(Character.toChars(0x1F600));
        final String rocket = new String(Character.toChars(0x1F680));

        assertEquals("a" + grinningFace + rocket, sort(rocket + "a" + grinningFace));
        assertEquals(grinningFace, sort(grinningFace));
    }

}
