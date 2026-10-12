package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.findFirstDouble;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static com.landawn.abacus.util.Strings.substringAfterAny;
import static com.landawn.abacus.util.Strings.substringBeforeAny;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;

/** Strings search contracts, including regression cases. */
public class StringsSearchTest extends StringsTestSupport {



    // shouldUseCharMembership: valueCount >= 8 && (long) strLength * valueCount >= 4096.
    // minLastIndexOfAll passes (startIndexFromBack + 1) as strLength, so with 8 candidates the fast path
    // starts exactly at startIndexFromBack == 511 (512 * 8 == 4096).
    private static final int MIN_VALUE_COUNT_FOR_FAST_PATH = 8;

    private static final int FIRST_FAST_PATH_START_INDEX = 511;




    /** Scalar reference: the minimum, over the candidates, of each candidate's last index at or before the bound. */
    private static int refMinLastIndexOfAll(final String str, final int startIndexFromBack, final char... valuesToFind) {
        if (Strings.isEmpty(str) || startIndexFromBack < 0 || CommonUtil.isEmpty(valuesToFind)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        final int start = Math.min(startIndexFromBack, str.length() - 1);
        int result = CommonUtil.INDEX_NOT_FOUND;

        for (final char ch : valuesToFind) {
            final int idx = str.lastIndexOf(ch, start);

            if (idx >= 0 && (result == CommonUtil.INDEX_NOT_FOUND || idx < result)) {
                result = idx;
            }
        }

        return result;
    }

    private static String repeatingString(final int length, final String alphabet) {
        final StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(i % alphabet.length()));
        }

        return sb.toString();
    }

    private static char[] charsOf(final String s) {
        return s.toCharArray();
    }

    private static int bruteMaxFirst(final String str, final int fromIndex, final String[] candidates) {
        if (fromIndex > str.length()) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int best = -1;

        for (final String candidate : candidates) {
            if (candidate.length() > str.length() - from) {
                continue;
            }

            final int index = str.indexOf(candidate, from);

            if (index >= 0) {
                best = Math.max(best, index);
            }
        }

        return best;
    }

    private static int bruteMinFirst(final String str, final int fromIndex, final String[] candidates) {
        if (fromIndex > str.length()) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int best = -1;

        for (final String candidate : candidates) {
            if (candidate.length() > str.length() - from) {
                continue;
            }

            final int index = str.indexOf(candidate, from);

            if (index >= 0 && (best == -1 || index < best)) {
                best = index;
            }
        }

        return best;
    }

    private static int bruteMaxLast(final String str, final int startIndexFromBack, final String[] candidates) {
        final int from = Math.min(startIndexFromBack, str.length());
        int best = -1;

        for (final String candidate : candidates) {
            if (candidate.length() > str.length()) {
                continue;
            }

            final int index = str.lastIndexOf(candidate, from);

            if (index >= 0) {
                best = Math.max(best, index);
            }
        }

        return best;
    }

    private static int bruteMinLast(final String str, final int startIndexFromBack, final String[] candidates) {
        final int from = Math.min(startIndexFromBack, str.length());
        int best = -1;

        for (final String candidate : candidates) {
            if (candidate.length() > str.length()) {
                continue;
            }

            final int index = str.lastIndexOf(candidate, from);

            if (index >= 0 && (best == -1 || index < best)) {
                best = index;
            }
        }

        return best;
    }

    private static void assertIaeNamingDelimiter(final org.junit.jupiter.api.function.Executable executable) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(e.getMessage().contains("delimiter"), e.getMessage());
    }

    private static int countMatchesCharReference(final String str, final char ch) {
        int count = 0;

        for (int i = 0; str != null && i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }

        return count;
    }

    private static int minIndexOfAllStringReference(final String str, final int fromIndex, final String... valuesToFind) {
        if (str == null || fromIndex > str.length() || valuesToFind == null) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int result = -1;

        for (final String value : valuesToFind) {
            if (value == null) {
                continue;
            }

            final int found = value.isEmpty() ? from : str.indexOf(value, from);

            if (found >= 0 && (result < 0 || found < result)) {
                result = found;
            }
        }

        return result;
    }

    private static int minIndexOfAllCharReference(final String str, final int fromIndex, final char... valuesToFind) {
        if (str == null || fromIndex > str.length() || valuesToFind == null) {
            return -1;
        }

        final int from = Math.max(0, fromIndex);
        int result = -1;

        for (final char value : valuesToFind) {
            final int found = str.indexOf(value, from);

            if (found >= 0 && (result < 0 || found < result)) {
                result = found;
            }
        }

        return result;
    }

    protected static int indexOfAnyReference(final String str, int fromIndex, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return -1;
        }

        fromIndex = Math.max(0, fromIndex);

        for (final char value : values) {
            final int index = str.indexOf(value, fromIndex);

            if (index >= 0) {
                return index;
            }
        }

        return -1;
    }

    protected static int lastIndexOfAnyReference(final String str, int startIndexFromBack, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return -1;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        for (final char value : values) {
            final int index = str.lastIndexOf(value, startIndexFromBack);

            if (index >= 0) {
                return index;
            }
        }

        return -1;
    }

    protected static int minIndexOfAllReference(final String str, int fromIndex, final char[] values) {
        if (str == null || fromIndex > str.length() || values == null || values.length == 0) {
            return -1;
        }

        fromIndex = Math.max(0, fromIndex);
        int result = -1;

        for (final char value : values) {
            final int index = str.indexOf(value, fromIndex);

            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }

        return result;
    }

    protected static int maxLastIndexOfAllReference(final String str, int startIndexFromBack, final char[] values) {
        if (str == null || str.isEmpty() || startIndexFromBack < 0 || values == null || values.length == 0) {
            return -1;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);
        int result = -1;

        for (final char value : values) {
            result = Math.max(result, str.lastIndexOf(value, startIndexFromBack));
        }

        return result;
    }

    protected static int indexOfAnyButReference(final String str, int fromIndex, final char[] valuesToExclude) {
        if (str == null || str.isEmpty()) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        fromIndex = Math.max(0, fromIndex);

        if (valuesToExclude == null || valuesToExclude.length == 0) {
            return fromIndex < str.length() ? fromIndex : CommonUtil.INDEX_NOT_FOUND;
        }

        outer: for (int i = fromIndex; i < str.length(); i++) {
            for (final char excluded : valuesToExclude) {
                if (str.charAt(i) == excluded) {
                    continue outer;
                }
            }

            return i;
        }

        return CommonUtil.INDEX_NOT_FOUND;
    }

    protected static boolean containsAllCharsReference(final String str, final char[] values) {
        if (values == null || values.length == 0) {
            return true;
        }

        if (str == null || str.isEmpty()) {
            return false;
        }

        for (final char value : values) {
            if (str.indexOf(value) < 0) {
                return false;
            }
        }

        return true;
    }

    protected static boolean containsNoneCharsReference(final String str, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return true;
        }

        for (int i = 0; i < str.length(); i++) {
            for (final char value : values) {
                if (str.charAt(i) == value) {
                    return false;
                }
            }
        }

        return true;
    }

    @Test
    public void testAdaptiveCharacterSearches_ExhaustiveShortInputsAndThresholdBoundary() throws ReflectiveOperationException {
        final char[] alphabet = { '\0', 'a', 'b', '\uD800', '\uDC00', '\uFFFF' };
        final List<String> haystacks = shortStrings(alphabet, 3);
        final List<String> candidateStrings = shortStrings(alphabet, 2);

        for (final String haystack : haystacks) {
            final int[] searchStarts = { -2, 0, 1, haystack.length() - 1, haystack.length(), haystack.length() + 1 };

            for (final String candidateString : candidateStrings) {
                final char[] candidates = candidateString.toCharArray();

                for (final int searchStart : searchStarts) {
                    assertEquals(indexOfAnyReference(haystack, searchStart, candidates), Strings.indexOfAny(haystack, searchStart, candidates));
                    assertEquals(minIndexOfAllReference(haystack, searchStart, candidates), Strings.minIndexOfAll(haystack, searchStart, candidates));
                    assertEquals(lastIndexOfAnyReference(haystack, searchStart, candidates), Strings.lastIndexOfAny(haystack, searchStart, candidates));
                    assertEquals(maxLastIndexOfAllReference(haystack, searchStart, candidates), Strings.maxLastIndexOfAll(haystack, searchStart, candidates));
                }

                assertEquals(containsAllCharsReference(haystack, candidates), Strings.containsAll(haystack, candidates));
                assertEquals(containsNoneCharsReference(haystack, candidates), Strings.containsNone(haystack, candidates));
                assertEquals(!haystack.isEmpty() && candidates.length > 0 && !containsNoneCharsReference(haystack, candidates),
                        Strings.containsAny(haystack, candidates));
                assertEquals(haystack.isEmpty() || candidates.length > 0 && indexOfAnyButReference(haystack, 0, candidates) < 0,
                        Strings.containsOnly(haystack, candidates));
            }
        }

        final Method crossover = Strings.class.getDeclaredMethod("shouldUseCharMembership", int.class, int.class);
        crossover.setAccessible(true);
        assertFalse((boolean) crossover.invoke(null, 511, 8));
        assertTrue((boolean) crossover.invoke(null, 512, 8));
        assertFalse((boolean) crossover.invoke(null, 512, 7));

        final String thresholdHaystack = "a".repeat(512);
        final char[] orderedCandidates = { 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'a' };
        assertEquals(0, Strings.indexOfAny(thresholdHaystack, orderedCandidates));
        assertEquals(0, Strings.minIndexOfAll(thresholdHaystack, orderedCandidates));
        assertEquals(511, Strings.lastIndexOfAny(thresholdHaystack, orderedCandidates));
        assertEquals(511, Strings.maxLastIndexOfAll(thresholdHaystack, orderedCandidates));
        assertEquals(0, Strings.lastIndexOfAny("ab", 'a', 'b')); // candidate order, not the rightmost candidate
    }

    @Test
    public void testAdaptiveCharacterSearches_MatchReferenceContracts() {
        final String emoji = "\uD83D\uDE00";
        final String str = "a".repeat(700) + "q" + "b".repeat(700) + "x" + emoji + "z" + "a".repeat(700);
        final char[] values = { 'x', 'q', '\uFFFF', '\uD800', 'z', '\0', 'm', 'n', 'x', '\uDC00', 'a', 'b' };
        final int[] forwardStarts = { -10, 0, 699, 700, 701, 1400, str.length() - 1, str.length(), str.length() + 1 };
        final int[] backwardStarts = { -1, 0, 700, 1400, str.length() - 2, str.length() - 1, str.length(), str.length() + 10 };

        for (final int fromIndex : forwardStarts) {
            assertEquals(indexOfAnyReference(str, fromIndex, values), Strings.indexOfAny(str, fromIndex, values));
            assertEquals(minIndexOfAllReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values));
        }

        for (final int startIndexFromBack : backwardStarts) {
            assertEquals(lastIndexOfAnyReference(str, startIndexFromBack, values), Strings.lastIndexOfAny(str, startIndexFromBack, values));
            assertEquals(maxLastIndexOfAllReference(str, startIndexFromBack, values), Strings.maxLastIndexOfAll(str, startIndexFromBack, values));
        }

        final char[] mostlyAbsentValues = new char[256];

        for (int i = 0; i < mostlyAbsentValues.length - 1; i++) {
            mostlyAbsentValues[i] = (char) (0x0100 + i);
        }

        mostlyAbsentValues[mostlyAbsentValues.length - 1] = 'z';
        assertEquals(str.indexOf('z'), Strings.indexOfAny(str, mostlyAbsentValues));
        assertEquals(str.indexOf('z'), Strings.minIndexOfAll(str, mostlyAbsentValues));
        assertEquals(str.lastIndexOf('z'), Strings.lastIndexOfAny(str, mostlyAbsentValues));
        assertEquals(str.lastIndexOf('z'), Strings.maxLastIndexOfAll(str, mostlyAbsentValues));

        final Random random = new Random(0x5EAC4L);

        for (int testCase = 0; testCase < 100; testCase++) {
            final char[] haystackChars = new char[512 + random.nextInt(256)];
            final char[] candidateChars = new char[8 + random.nextInt(56)];

            for (int i = 0; i < haystackChars.length; i++) {
                haystackChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            for (int i = 0; i < candidateChars.length; i++) {
                candidateChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            candidateChars[random.nextInt(candidateChars.length)] = haystackChars[random.nextInt(haystackChars.length)];

            final String haystack = new String(haystackChars);
            final int fromIndex = random.nextInt(33) - 16;
            final int startIndexFromBack = haystack.length() - 1 - random.nextInt(16);

            assertEquals(indexOfAnyReference(haystack, fromIndex, candidateChars), Strings.indexOfAny(haystack, fromIndex, candidateChars));
            assertEquals(minIndexOfAllReference(haystack, fromIndex, candidateChars), Strings.minIndexOfAll(haystack, fromIndex, candidateChars));
            assertEquals(lastIndexOfAnyReference(haystack, startIndexFromBack, candidateChars),
                    Strings.lastIndexOfAny(haystack, startIndexFromBack, candidateChars));
            assertEquals(maxLastIndexOfAllReference(haystack, startIndexFromBack, candidateChars),
                    Strings.maxLastIndexOfAll(haystack, startIndexFromBack, candidateChars));
        }
    }

    @Test
    public void testAnyCandidateFamilies_CandidatePriority() {
        assertEquals(3, Strings.indexOfAny("a:b.c", '.', ':'));
        assertEquals(3, Strings.indexOfAny("a:b.c", ".", ":"));
        assertEquals(1, Strings.lastIndexOfAny("a.b:c", '.', ':'));
        assertEquals(1, Strings.lastIndexOfAny("a.b:c", ".", ":"));

        assertEquals(1, Strings.minIndexOfAll("a:b.c", '.', ':'));
        assertEquals(3, Strings.maxIndexOfAll("a:b.c", '.', ':'));
        assertEquals(1, Strings.minLastIndexOfAll("a.b:c", '.', ':'));
        assertEquals(3, Strings.maxLastIndexOfAll("a.b:c", '.', ':'));
        assertEquals(2, Strings.minIndexOfAll("hello", 'o', 'l'));
        assertEquals(4, Strings.indexOfAny("hello", 'o', 'l'));
        assertEquals(2, Strings.indexOfAny("hello", 'l', 'o'));
        assertEquals(4, Strings.maxIndexOfAll("hello", 'o', 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 'o', 'l'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 'o', 'l'));
        assertEquals(2, Strings.minIndexOfAll("hello", 1, 'o', 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 3, 'o', 'l'));
        assertEquals(-1, Strings.minIndexOfAll("hello", 'x', 'y'));
        assertEquals(-1, Strings.maxLastIndexOfAll((String) null, 'a', 'b'));

        assertEquals("c", Strings.substringAfterAny("a:b.c", '.', ':'));
        assertEquals("c", Strings.substringAfterAny("a:b.c", ".", ":"));
        assertEquals("a:b", Strings.substringBeforeAny("a:b.c", '.', ':'));
        assertEquals("a:b", Strings.substringBeforeAny("a:b.c", ".", ":"));
    }

    @Test
    public void testCharacterMembershipFastPaths_RandomizedDifferential() {
        final Random random = new Random(20260825L);

        for (int iteration = 0; iteration < 300; iteration++) {
            final char[] inputChars = new char[random.nextInt(601)];
            final char[] values = new char[random.nextInt(161)];

            for (int i = 0; i < inputChars.length; i++) {
                inputChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            for (int i = 0; i < values.length; i++) {
                values[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            if (inputChars.length > 0 && values.length > 0 && iteration % 3 == 0) {
                values[0] = inputChars[random.nextInt(inputChars.length)];
            }

            final String input = new String(inputChars);
            final int fromIndex = random.nextInt(input.length() + 41) - 20;
            assertEquals(indexOfAnyButReference(input, fromIndex, values), Strings.indexOfAnyBut(input, fromIndex, values), "iteration " + iteration);
            assertEquals(containsAllCharsReference(input, values), Strings.containsAll(input, values), "iteration " + iteration);
            assertEquals(containsNoneCharsReference(input, values), Strings.containsNone(input, values), "iteration " + iteration);
            assertEquals(!input.isEmpty() && values.length > 0 && indexOfAnyButReference(input, 0, values) < 0 || input.isEmpty(),
                    Strings.containsOnly(input, values), "iteration " + iteration);
            assertEquals(!input.isEmpty() && values.length > 0 && !containsNoneCharsReference(input, values), Strings.containsAny(input, values),
                    "iteration " + iteration);
        }

        final char[] highValues = { '\u0000', '\u003F', '\u0040', '\uD800', '\uDC00', '\uFFFF', '\uFFFF', 'x' };
        final String highInput = "x\u0000\uD800\uDC00\uFFFF".repeat(1024);
        assertEquals(indexOfAnyButReference(highInput, 0, highValues), Strings.indexOfAnyBut(highInput, highValues));
        assertEquals(containsAllCharsReference(highInput, highValues), Strings.containsAll(highInput, highValues));
        assertEquals(containsNoneCharsReference(highInput, highValues), Strings.containsNone(highInput, highValues));
        assertFalse(Strings.containsOnly(null, highValues));
        assertTrue(Strings.containsOnly("", highValues));
    }

    // contains

    @Test
    public void testContains() {
        assertTrue(Strings.contains("hello", 'e'));
        assertFalse(Strings.contains("hello", 'x'));
        assertFalse(Strings.contains(null, 'e'));

        assertTrue(Strings.contains("hello", "ell"));
        assertFalse(Strings.contains("hello", "xyz"));
        assertFalse(Strings.contains(null, "ell"));
    }

    @Test
    public void testContains_Char() {
        assertTrue(Strings.contains("abc", 'a'));
        assertTrue(Strings.contains("abc", 'c'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
    }

    @Test
    public void testContains_CharEdgeCases() {
        assertTrue(Strings.contains("abc", 'b'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
    }

    @Test
    public void testContains_String() {
        assertTrue(Strings.contains("abcdef", "abc"));
        assertTrue(Strings.contains("abcdef", "def"));
        assertFalse(Strings.contains("abcdef", "xyz"));
        assertFalse(Strings.contains(null, "abc"));
    }

    @Test
    public void testContains_StringEdgeCases() {
        assertTrue(Strings.contains("abc", "bc"));
        assertFalse(Strings.contains("abc", "xy"));
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("abc", null));
    }

    @Test
    public void testContainsChar() {
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
        assertTrue(Strings.contains("abc", 'a'));
        assertFalse(Strings.contains("abc", 'd'));
    }

    @Test
    public void testContainsString() {
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("", "a"));
        assertTrue(Strings.contains("abc", "a"));
        assertTrue(Strings.contains("abc", "ab"));
        assertFalse(Strings.contains("abc", "d"));
    }

    @Test
    public void testC538_tokenFamilyRejectsNullOrEmptyDelimiter() {
        for (final String delimiter : new String[] { null, "" }) {
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", "b", delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase("a,b", "B", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase("a,b", "B", delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken("a,b", "b", delimiter, 3));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", "B", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", "B", delimiter, 3));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken("a,b", "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsTokenIgnoreCase("a,b", "B", delimiter));

            // collaborator-first: even a null str or token does not short-circuit before the delimiter check
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfToken("a,b", null, delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase(null, null, delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.indexOfTokenIgnoreCase(null, "b", delimiter, -1));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfToken(null, "b", delimiter, -1));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.lastIndexOfTokenIgnoreCase("a,b", null, delimiter, 0));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken(null, "b", delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsTokenIgnoreCase(null, null, delimiter));
            assertIaeNamingDelimiter(() -> StrUtil.containsToken("", "", delimiter));
        }

        // the applesauce misuse from the review now fails loudly; the plain search is one call away
        assertThrows(IllegalArgumentException.class, () -> StrUtil.containsToken("applesauce", "apple", ""));
        assertTrue(Strings.contains("applesauce", "apple"));

        // a non-empty delimiter behaves as before, including null str/token and the empty-token field rule
        assertEquals(2, StrUtil.indexOfToken("a,b", "b", ","));
        assertEquals(6, StrUtil.indexOfToken("apple,banana,cherry", "banana", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken(null, "b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", null, ","));
        assertEquals(13, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("apple", "apple", ",", -1));
        assertEquals(4, StrUtil.lastIndexOfToken("a,b,", "", ","));
        assertEquals(3, StrUtil.indexOfToken("ab,", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfToken("ab", "", ",", 10));
        assertTrue(StrUtil.containsToken(",a", "", ","));
        assertFalse(StrUtil.containsToken("a,b", "", ","));
        assertFalse(StrUtil.containsToken(null, "a", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a,b", null, ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("Apple,Banana,Orange", "banana", ","));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        assertEquals(0, StrUtil.indexOfToken("aaa", "a", "aa"));
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(3, StrUtil.indexOfToken(EMOJI + "," + "b", "b", ","));
        assertEquals(0, StrUtil.indexOfToken("a" + EMOJI + "b", "a", EMOJI));
    }

    @Test
    public void testContains_WithDelimiter() {
        assertTrue(StrUtil.containsToken("a,b,c", "b", ","));
        assertFalse(StrUtil.containsToken("a,b,c", "d", ","));
        assertFalse(StrUtil.containsToken(null, "b", ","));
    }

    // containsAll

    @Test
    public void testContainsAll() {
        assertTrue(Strings.containsAll("hello world", 'h', 'e', 'l'));
        assertFalse(Strings.containsAll("hello", 'h', 'x'));
        assertTrue(Strings.containsAll("", new char[0]));

        assertTrue(Strings.containsAll("hello world", "hello", "world"));
        assertFalse(Strings.containsAll("hello", "hello", "xyz"));
    }

    @Test
    public void test_containsAll() {
        final String str = "abcD12";
        assertTrue(Strings.containsAll(str, 'a', 'D', '2'));
        assertTrue(Strings.containsAllIgnoreCase(str, "A", "d", "2"));
        assertFalse(Strings.containsAll(str, "a", "d", "2"));
    }

    @Test
    public void testContainsAll_CharArray() {
        assertTrue(Strings.containsAll("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsAll("abc", 'a', 'x'));
        assertTrue(Strings.containsAll("abc", (char[]) null));
        assertFalse(Strings.containsAll(null, 'a', 'b'));
        // Lone surrogate code units are treated as ordinary UTF-16 code units (no validation, no exception).
        assertFalse(Strings.containsAll("abc", '\uDC00', 'a')); // '\uDC00' is not present in "abc"
        assertFalse(Strings.containsAll("abc", '\uDFFF', 'a')); // '\uDFFF' is not present in "abc"
    }

    @Test
    public void testContainsAll_StringArray() {
        assertTrue(Strings.containsAll("abcdefg", "abc", "def"));
        assertFalse(Strings.containsAll("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAll(null, "abc", "def"));
    }

    @Test
    public void testContainsAllChars() {
        assertTrue(Strings.containsAll("abcd", 'a', 'b'));
        assertFalse(Strings.containsAll("abcd", 'a', 'e'));
        assertTrue(Strings.containsAll("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAll(null, 'a'));
    }

    @Test
    public void testContainsAllStrings() {
        assertTrue(Strings.containsAll("abcdef", "ab", "cd"));
        assertFalse(Strings.containsAll("abcdef", "ab", "gh"));
        assertTrue(Strings.containsAll("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAll(null, "a"));
        assertTrue(Strings.containsAll("", ""));
        assertTrue(Strings.containsAll("x", ""));
        assertFalse(Strings.containsAll("", "a"));
        assertEquals(Strings.contains("", ""), Strings.containsAll("", ""));
    }

    // containsAny

    @Test
    public void testContainsAny() {
        assertTrue(Strings.containsAny("hello", 'h', 'x'));
        assertFalse(Strings.containsAny("hello", 'x', 'y'));
        assertFalse(Strings.containsAny("", 'x'));

        assertTrue(Strings.containsAny("hello", "world", "ell"));
        assertFalse(Strings.containsAny("hello", "xyz", "abc"));
    }

    @Test
    public void testContainsAny_CharArray() {
        assertTrue(Strings.containsAny("abc", 'a', 'x'));
        assertTrue(Strings.containsAny("abc", 'c', 'y'));
        assertFalse(Strings.containsAny("abc", 'x', 'y'));
        assertFalse(Strings.containsAny(null, 'a', 'b'));
    }

    @Test
    public void testContainsAny_StringArray() {
        assertTrue(Strings.containsAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsAny("abcdef", "xyz", "def"));
        assertFalse(Strings.containsAny("abc", "xyz", "123"));
        assertFalse(Strings.containsAny(null, "abc", "def"));
    }

    @Test
    public void testContainsAnyChars() {
        assertTrue(Strings.containsAny("abcd", 'a', 'e'));
        assertFalse(Strings.containsAny("abcd", 'e', 'f'));
        assertFalse(Strings.containsAny("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAny(null, 'a'));
    }

    @Test
    public void testContainsAnyStrings() {
        assertTrue(Strings.containsAny("abcdef", "ab", "gh"));
        assertFalse(Strings.containsAny("abcdef", "gh", "ij"));
        assertFalse(Strings.containsAny("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAny(null, "a"));
        assertTrue(Strings.containsAny("", ""));
        assertTrue(Strings.containsAny("x", ""));
        assertFalse(Strings.containsAny("", "a"));
        assertEquals(Strings.contains("", ""), Strings.containsAny("", ""));
        assertTrue(Strings.containsAllIgnoreCase("", ""));
        assertTrue(Strings.containsAnyIgnoreCase("", ""));
    }

    // containsNone

    @Test
    public void testContainsNone() {
        assertTrue(Strings.containsNone("hello", 'x', 'y'));
        assertFalse(Strings.containsNone("hello", 'h', 'x'));
        assertTrue(Strings.containsNone("", 'x'));
        assertTrue(Strings.containsNone(null, 'x'));

        assertTrue(Strings.containsNone("hello", "xyz", "abc"));
        assertFalse(Strings.containsNone("hello", "world", "ell"));
    }

    @Test
    public void testContainsNone_CharArray() {
        assertTrue(Strings.containsNone("abc", 'x', 'y'));
        assertTrue(Strings.containsNone("", 'a'));
        assertTrue(Strings.containsNone(null, 'a'));
        assertFalse(Strings.containsNone("abc", 'a', 'x'));
    }

    @Test
    public void testContainsNone_StringArray() {
        assertTrue(Strings.containsNone("abc", "xyz", "123"));
        assertFalse(Strings.containsNone("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsNone(null, "abc"));
    }

    @Test
    public void testC559_containsNoneAndContainsOnlyDivergeFromCommonsOnSurrogates() {
        assertFalse(Strings.containsNone("\uD83D", '\uD83D', 'x'));
        assertTrue(StringUtils.containsNone("\uD83D", '\uD83D', 'x'));

        assertTrue(Strings.containsOnly("\uD83D", '\uD83D', '\uDE00', 'x'));
        assertFalse(StringUtils.containsOnly("\uD83D", '\uD83D', '\uDE00', 'x'));

        // no divergence without surrogates
        assertEquals(StringUtils.containsNone("hello", 'x', 'y'), Strings.containsNone("hello", 'x', 'y'));
        // "can differ": Commons reports the lone high surrogate when it is the LAST candidate, as this method does
        assertFalse(Strings.containsNone("\uD83D", 'x', '\uD83D'));
        assertFalse(StringUtils.containsNone("\uD83D", 'x', '\uD83D'));
        assertEquals(StringUtils.containsOnly("abab", 'a', 'b'), Strings.containsOnly("abab", 'a', 'b'));
        // a full pair supplied as both surrogates
        assertTrue(Strings.containsOnly(SMILE, '\uD83D', '\uDE00'));
        assertFalse(Strings.containsNone(SMILE, '\uDE00'));
    }

    @Test
    public void testContainsNoneChars() {
        assertTrue(Strings.containsNone("abcd", 'e', 'f'));
        assertFalse(Strings.containsNone("abcd", 'a', 'e'));
        assertTrue(Strings.containsNone("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertTrue(Strings.containsNone(null, 'a'));
        assertEquals(!Strings.containsAny(null, 'a'), Strings.containsNone(null, 'a'));
    }

    @Test
    public void testContainsNoneStrings() {
        assertTrue(Strings.containsNone("abcdef", "gh", "ij"));
        assertFalse(Strings.containsNone("abcdef", "ab", "gh"));
        assertTrue(Strings.containsNone("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertTrue(Strings.containsNone(null, "a"));
        assertFalse(Strings.containsNone("", ""));
        assertTrue(Strings.containsAny("", ""));
        assertTrue(Strings.containsNone("", "a"));
        assertFalse(Strings.containsNone("", "a", ""));
        assertFalse(Strings.containsNone("x", ""));
        assertTrue(Strings.containsNone("", (String[]) null));
        assertEquals(!Strings.containsAny("", ""), Strings.containsNone("", ""));
        assertEquals(!Strings.containsAny("x", ""), Strings.containsNone("x", ""));
        assertTrue(Strings.containsNone(null, ""));
        assertFalse(Strings.containsAny(null, ""));
        assertTrue(Strings.containsNone(null, (String[]) null));
        assertEquals(!Strings.containsAny(null, ""), Strings.containsNone(null, ""));
        assertEquals(!Strings.containsAny(null, "a"), Strings.containsNone(null, "a"));
    }

    // containsOnly

    @Test
    public void testContainsOnly() {
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'a', 'b'));
        assertFalse(Strings.containsOnly(null, 'a'));
    }

    @Test
    public void testContainsOnly_CaseSensitive() {
        assertFalse(Strings.containsOnly("ABC", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("ABC", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_EdgeCases() {
        assertTrue(Strings.containsOnly("a", 'a'));

        assertTrue(Strings.containsOnly("aaabbbccc", 'a', 'b', 'c'));

        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c', 'd', 'e'));

        assertTrue(Strings.containsOnly("aaaa", 'a'));

        assertTrue(Strings.containsOnly("abcd", 'a', 'b', 'c', 'd'));
    }

    @Test
    public void testContainsOnly_EmptyString() {
        assertTrue(Strings.containsOnly("", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("", 'x'));
        assertTrue(Strings.containsOnly("", new char[0]));
    }

    @Test
    public void testContainsOnly_EmptyValuesToFind() {
        assertFalse(Strings.containsOnly("abc", new char[0]));
        assertFalse(Strings.containsOnly("a", new char[0]));
        assertFalse(Strings.containsOnly(" ", new char[0]));

        assertFalse(Strings.containsOnly("abc", (char[]) null));
    }

    @Test
    public void testContainsOnly_LongStrings() {
        String longString = "a".repeat(1000);
        assertTrue(Strings.containsOnly(longString, 'a'));
        assertFalse(Strings.containsOnly(longString + "b", 'a'));

        String mixedLongString = "abcabc".repeat(100);
        assertTrue(Strings.containsOnly(mixedLongString, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(mixedLongString + "d", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_NullString() {
        assertFalse(Strings.containsOnly(null, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(null, 'x'));
        assertFalse(Strings.containsOnly(null));
    }

    @Test
    public void testContainsOnly_Numeric() {
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));
        assertTrue(Strings.containsOnly("000", '0'));
        assertTrue(Strings.containsOnly("1234567890", '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));

        assertFalse(Strings.containsOnly("123a", '1', '2', '3'));
        assertFalse(Strings.containsOnly("12.3", '1', '2', '3'));
    }

    @Test
    public void testContainsOnly_RepeatedValuesToFind() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c', 'a', 'b'));
        assertTrue(Strings.containsOnly("aaa", 'a', 'a', 'a'));
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c', 'a', 'b'));
    }

    @Test
    public void testContainsOnly_SingleCharacter() {
        assertTrue(Strings.containsOnly("a", 'a'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("x", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("a", 'b', 'c'));
    }

    @Test
    public void testContainsOnly_SpecialCharacters() {
        assertTrue(Strings.containsOnly(" ", ' '));
        assertTrue(Strings.containsOnly("\t\n", '\t', '\n'));
        assertTrue(Strings.containsOnly("!@#", '!', '@', '#'));
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));

        assertFalse(Strings.containsOnly("Hello World", 'H', 'e', 'l', 'o'));
        assertTrue(Strings.containsOnly("Hello", 'H', 'e', 'l', 'o'));
    }

    @Test
    public void testContainsOnly_StringContainsOnlySpecifiedChars() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abcabc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("cba", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_StringContainsOtherChars() {
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("axb", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("xyz", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("ab c", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("123", 'a', 'b', 'c'));
    }

    @Test
    public void testContainsOnly_TreatsSurrogateAsCodeUnit() {
        char highSurrogate = '\uD800';
        char lowSurrogate = '\uDC00';

        // No validation/exception: "test" contains characters outside the allowed set, so containsOnly is false.
        assertFalse(Strings.containsOnly("test", highSurrogate));
        assertFalse(Strings.containsOnly("test", lowSurrogate));
        assertFalse(Strings.containsOnly("test", 'a', highSurrogate, 'b'));

        // A string made up only of the given (surrogate) code units is matched as code units.
        assertTrue(Strings.containsOnly("\uD800\uD800", highSurrogate));
    }

    @Test
    public void testContainsOnly_UnicodeCharacters() {
        assertTrue(Strings.containsOnly("αβγ", 'α', 'β', 'γ'));
        assertFalse(Strings.containsOnly("αβγδ", 'α', 'β', 'γ'));
    }

    @Test
    public void testContainsOnly_Whitespace() {
        assertTrue(Strings.containsOnly("   ", ' '));
        assertTrue(Strings.containsOnly("\t\t\t", '\t'));
        assertTrue(Strings.containsOnly("\n\n", '\n'));
        assertTrue(Strings.containsOnly(" \t\n", ' ', '\t', '\n'));

        assertFalse(Strings.containsOnly("a b", 'a', 'b'));
        assertTrue(Strings.containsOnly("a b", 'a', 'b', ' '));
    }

    @Test
    public void testU2001_containsOnlyEmptyStringWithNullSetDivergesFromCommons() {
        assertTrue(Strings.containsOnly("", (char[]) null));
        assertTrue(Strings.containsOnly("", new char[0]));
        assertTrue(Strings.containsOnly("", 'a'));
        assertFalse(Strings.containsOnly(null, 'a'));
        assertFalse(Strings.containsOnly("a", (char[]) null));

        // Commons Lang: a null set is false before the empty-input rule is applied
        assertFalse(StringUtils.containsOnly("", (char[]) null));
        assertTrue(StringUtils.containsOnly("", new char[0]));
        assertTrue(StringUtils.containsOnly("", 'a'));
        assertFalse(StringUtils.containsOnly(null, 'a'));
    }



    // D-3: containsOnly with an empty allowed set
    @Test
    public void testD3_containsOnlyWithEmptyAllowedChars() {
        assertFalse(Strings.containsOnly("abc"));
        assertTrue(Strings.containsOnly(""));
        assertFalse(Strings.containsOnly((String) null));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c'));
        // The guard agrees with what delegating to indexOfAnyBut would produce.
        assertEquals(0, Strings.indexOfAnyBut("abc"));
    }

    @Test
    public void testC572_containsTokenDelegatesValidationAndNullRules() {
        for (final String delimiter : new String[] { null, "" }) {
            for (final String str : new String[] { null, "", "a,b" }) {
                for (final String token : new String[] { null, "", "b" }) {
                    assertEquals("'delimiter' cannot be null or empty",
                            assertThrows(IllegalArgumentException.class, () -> StrUtil.containsToken(str, token, delimiter)).getMessage());
                    assertEquals("'delimiter' cannot be null or empty",
                            assertThrows(IllegalArgumentException.class, () -> StrUtil.containsTokenIgnoreCase(str, token, delimiter)).getMessage());
                }
            }
        }

        assertFalse(StrUtil.containsToken(null, "a", ","));
        assertFalse(StrUtil.containsToken("a", null, ","));
        assertFalse(StrUtil.containsTokenIgnoreCase(null, "a", ","));
        assertFalse(StrUtil.containsTokenIgnoreCase("a", null, ","));
        assertTrue(StrUtil.containsToken("apple,banana", "banana", ","));
        assertFalse(StrUtil.containsToken("apple,banana", "BANANA", ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("apple,banana", "BANANA", ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("xANDy", "y", "and"));
        assertTrue(StrUtil.containsToken("", "", ","));
        assertTrue(StrUtil.containsToken(",a", "", ","));
        assertFalse(StrUtil.containsToken("a,b", "", ","));
        assertTrue(StrUtil.containsToken(SMILE + "," + GRIN, GRIN, ","));
    }

    // countMatches

    @Test
    public void testCountMatches_Char() {
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'c'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
        assertEquals(0, Strings.countMatches(null, 'a'));
    }

    @Test
    public void testCountMatches_CharEdgeCases() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
    }

    @Test
    public void testCountMatches_CodePointVersusChar() {
        assertEquals(3, Strings.countMatches("hello world", (int) 'l'));
        assertEquals(Strings.countMatches("hello world", 'l'), Strings.countMatches("hello world", (int) 'l'));

        final String twoFaces = "a😀😀";
        N.println(twoFaces);
        assertEquals(2, Strings.countMatches(twoFaces, 0x1F600));
        assertEquals(2, Strings.countMatches(twoFaces, '\uD83D'));
        assertEquals(2, Strings.countMatches(twoFaces, '\uDE00'));
        assertEquals(0, Strings.countMatches(twoFaces, 0x1F44D));

        assertEquals(0, Strings.countMatches(null, 0x1F600));
        assertEquals(0, Strings.countMatches("", 0x1F600));
        assertEquals(0, Strings.countMatches("abc", -1));
        assertEquals(0, Strings.countMatches("abc", Character.MAX_CODE_POINT + 1));
    }

    @Test
    public void testCountMatches_NonOverlapping() {
        // Documented as non-overlapping: aaaa contains 2 non-overlapping "aa"s.
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(3, Strings.countMatches("aaaaaa", "aa"));
        assertEquals(0, Strings.countMatches("", "aa"));
        assertEquals(0, Strings.countMatches("aaaa", ""));
        assertEquals(0, Strings.countMatches(null, "aa"));
        assertEquals(0, Strings.countMatches("aaaa", null));
    }

    @Test
    public void testCountMatches_String() {
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches(null, "abc"));
    }

    @Test
    public void testCountMatches_StringEdgeCases() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches("abc", null));
        assertEquals(0, Strings.countMatches("abc", ""));
    }

    @Test
    public void testCountMatchesChar() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(6, Strings.countMatches("aabaabaa", 'a'));
        assertEquals(2, Strings.countMatches("aabaabaa", 'b'));
    }



    // G073-01: countMatches(String, char) switches from indexOf hopping to a plain scan on dense input; pin both regimes.
    @Test
    public void testCountMatchesChar_sparseDenseAndMixedRegions() {
        assertEquals(0, Strings.countMatches(null, ','));
        assertEquals(0, Strings.countMatches("", ','));
        assertEquals(1, Strings.countMatches(",", ','));
        assertEquals(0, Strings.countMatches("abc", '\u4E2D'));
        assertEquals(1, Strings.countMatches("ab\u4E2D", '\u4E2D'));
        assertEquals(40, Strings.countMatches(",".repeat(40), ','));
        assertEquals(7, Strings.countMatches(",".repeat(7), ','));
        assertEquals(8, Strings.countMatches(",".repeat(8), ','));
        assertEquals(9, Strings.countMatches(",".repeat(9), ','));
        assertEquals(40, Strings.countMatches(",".repeat(20) + "x".repeat(1000) + ",".repeat(20), ','));
        assertEquals(20, Strings.countMatches("x".repeat(1000) + ",".repeat(20), ','));
        assertEquals(2, Strings.countMatches("\uD83D\uDE00\uD83D\uDE00", '\uD83D'));

        final Random random = new Random(0x6073L);
        final double[] densities = { 0.0, 0.01, 0.1, 0.24, 0.26, 0.5, 0.9, 1.0 };

        for (int testCase = 0; testCase < 2000; testCase++) {
            final int length = random.nextInt(testCase < 1000 ? 70 : 600);
            final StringBuilder sb = new StringBuilder(length);
            double density = densities[random.nextInt(densities.length)];

            for (int i = 0; i < length; i++) {
                if (random.nextInt(50) == 0) {
                    density = densities[random.nextInt(densities.length)];
                }

                sb.append(random.nextDouble() < density ? ',' : (random.nextInt(20) == 0 ? '\u4E2D' : (char) ('a' + random.nextInt(3))));
            }

            final String str = sb.toString();

            for (final char ch : new char[] { ',', 'a', '\u4E2D', 'z' }) {
                assertEquals(countMatchesCharReference(str, ch), Strings.countMatches(str, ch), str);
            }
        }
    }

    @Test
    public void testCountMatchesString() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(1, Strings.countMatches("abcdef", "def"));
    }

    @Test
    public void testEmptyNeedleUsesUtf16CodeUnitBoundaries() {
        final String emoji = new String(Character.toChars(0x1F600));

        assertEquals(0, Strings.ordinalIndexOf(emoji, "", 1));
        assertEquals(1, Strings.ordinalIndexOf(emoji, "", 2));
        assertEquals(2, Strings.ordinalIndexOf(emoji, "", 3));
        assertEquals(-1, Strings.ordinalIndexOf(emoji, "", 4));

        assertEquals(2, Strings.lastOrdinalIndexOf(emoji, "", 1));
        assertEquals(1, Strings.lastOrdinalIndexOf(emoji, "", 2));
        assertEquals(0, Strings.lastOrdinalIndexOf(emoji, "", 3));
        assertEquals(-1, Strings.lastOrdinalIndexOf(emoji, "", 4));

        assertArrayEquals(new int[] { 0, 1 }, Strings.indicesOf(emoji, "").toArray());
        assertArrayEquals(new int[] { 0, 1 }, Strings.indicesOfIgnoreCase(emoji, "").toArray());
    }

    @Test
    public void testEmptySearchStringClampsExplicitIndexToLength() {
        assertEquals(3, Strings.indexOf("abc", "", 3));
        assertEquals(3, Strings.indexOf("abc", "", 4));
        assertEquals(0, Strings.indexOf("", "", 1));

        assertEquals(3, Strings.indexOfIgnoreCase("abc", "", 4));
        assertEquals(0, Strings.indexOfIgnoreCase("", "", 1));

        assertEquals(3, Strings.lastIndexOf("abc", "", 3));
        assertEquals(3, Strings.lastIndexOf("abc", "", 4));
        assertEquals(0, Strings.lastIndexOf("", "", 0));

        assertEquals(3, Strings.lastIndexOfIgnoreCase("abc", "", 4));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("", "", 0));

        assertEquals(3, Strings.minLastIndexOfAll("abc", 4, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", 4, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", ""));
    }

    @Test
    public void testEndsWith() {
        assertTrue(Strings.endsWith("hello", "llo"));
        assertTrue(Strings.endsWith("test", "test"));
        assertTrue(Strings.endsWith("abcdef", "def"));
        assertTrue(Strings.endsWith("abc", "bc"));
        assertTrue(Strings.endsWith("abc", ""));
        assertTrue(Strings.endsWith("", ""));
        assertTrue(Strings.endsWith("Hello", "llo"));

        assertFalse(Strings.endsWith("ab", "abc"));
        assertFalse(Strings.endsWith("", "a"));
        assertFalse(Strings.endsWith("abcdef", "xyz"));
        assertFalse(Strings.endsWith("abc", "ab"));
        assertFalse(Strings.endsWith("Hello", "LLO"));
        assertFalse(Strings.endsWith("TEST", "test"));
        assertFalse(Strings.endsWith(null, "suffix"));
        assertFalse(Strings.endsWith(null, null));
        assertFalse(Strings.endsWith("test", null));
    }

    @Test
    public void testEndsWithAny() {
        assertTrue(Strings.endsWithAny("hello", "llo", "test", "world"));
        assertTrue(Strings.endsWithAny("test", "no", "st", "maybe"));
        assertTrue(Strings.endsWithAny("abc", "xyz", "bc", "def"));
        assertTrue(Strings.endsWithAny("abcdef", "def", "xyz"));
        assertTrue(Strings.endsWithAny("abc", "xy", "bc", "cd"));

        assertFalse(Strings.endsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.endsWithAny("xyz", "a", "b", "c"));
        assertFalse(Strings.endsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAny(null, "def"));
        assertFalse(Strings.endsWithAny(null, "a", "b"));
        assertFalse(Strings.endsWithAny(null, "bc"));
    }

    @Test
    public void testFindAllEmailAddresses() {
        List<String> emails = Strings.findAllEmailAddresses("Contact: test@example.com and admin@test.org");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("test@example.com"));
        assertTrue(emails.contains("admin@test.org"));
    }

    @Test
    public void testFindAllEmailAddresses_EdgeCases() {
        List<String> emails = Strings.findAllEmailAddresses("a@b.com and c@d.com");
        assertEquals(2, emails.size());
        assertEquals(0, Strings.findAllEmailAddresses("no email").size());
        assertEquals(0, Strings.findAllEmailAddresses(null).size());
    }

    // findFirstDouble

    @Test
    public void testFindFirstDouble() {
        assertEquals("123.45", Strings.findFirstDouble("abc123.45def"));
        assertNull(Strings.findFirstDouble("abc"));
        assertNull(Strings.findFirstDouble(null));
    }

    @Test
    public void testFindFirstDouble_EdgeCases() {
        assertEquals("12.34", Strings.findFirstDouble("abc12.34def"));
        assertEquals("-12.34", Strings.findFirstDouble("abc-12.34def"));
        assertEquals(".5", Strings.findFirstDouble(".5"));
        assertEquals(".5", Strings.findFirstDouble("x=.5"));
        assertEquals("-.5", Strings.findFirstDouble("x=-.5"));
        assertEquals(".5e2", Strings.findFirstDouble(".5e2", true));
        assertEquals("-.5e2", Strings.findFirstDouble("x=-.5e2", true));
        assertNull(Strings.findFirstDouble("abc"));
        assertNull(Strings.findFirstDouble(null));
        assertNull(Strings.findFirstDouble(""));
    }

    @Test
    public void testC536_numberFindersMatchAsciiDigitsOnly() {
        final String arabicIndic = "\u0663\u0664";
        assertTrue(Character.isDigit('\u0663'));
        assertNull(Strings.findFirstInteger(arabicIndic));
        assertNull(Strings.findFirstDouble("\u0663.\u0664"));
        assertNull(Strings.findFirstDouble("\u0663.\u0664", true));
        assertSame(arabicIndic, Strings.replaceFirstInteger(arabicIndic, "X"));
        assertSame(arabicIndic, Strings.replaceFirstDouble(arabicIndic, "X"));
        assertSame(arabicIndic, Strings.replaceFirstDouble(arabicIndic, "X", true));
        assertEquals("34", Strings.findFirstInteger("x\u0663" + "34"));
        assertEquals("12.5", Strings.findFirstDouble("\u0663 12.5"));
    }

    @Test
    public void testFindFirstDouble_WithScientific() {
        assertEquals("1.23e4", findFirstDouble("value=1.23e4", true));
        assertEquals("1.23E-4", findFirstDouble("small=1.23E-4", true));
        assertEquals("123.45", findFirstDouble("abc123.45def", true));
        assertNull(findFirstDouble("no numbers", true));
        assertNull(findFirstDouble("", true));
        assertNull(findFirstDouble(null, true));
    }

    // findFirstEmailAddress

    @Test
    public void testC569_emailFinderExtendsOverLabelCharactersAndSkipsOthers() {
        assertEquals("a@b.com-x", Strings.findFirstEmailAddress("a@b.com-x"));
        assertEquals("a@b.com9", Strings.findFirstEmailAddress("a@b.com9"));
        assertEquals(Arrays.asList("a@b.com-x", "a@b.com9"), Strings.findAllEmailAddresses("a@b.com-x and a@b.com9"));

        assertNull(Strings.findFirstEmailAddress("a@b.com_x"));
        assertNull(Strings.findFirstEmailAddress("a@b.com-"));
        assertNull(Strings.findFirstEmailAddress("a@b.com\u0301"));
        assertTrue(Strings.findAllEmailAddresses("x a@b.com- y").isEmpty());
        assertNull(Strings.findFirstEmailAddress("m\u00FCller@firma.de"));
        assertEquals("a@b.com", Strings.findFirstEmailAddress("mail a@b.com, thanks"));

        // a letter or digit that cannot extend the last label (non-ASCII, or after a bracketed literal) is skipped
        assertNull(Strings.findFirstEmailAddress("a@b.com\u00E9"));
        assertNull(Strings.findFirstEmailAddress("a@[1.2.3.4]x"));
        assertTrue(Strings.findAllEmailAddresses("x a@b.com\u0661 y").isEmpty());
        assertEquals("a@[1.2.3.4]", Strings.findFirstEmailAddress("a@[1.2.3.4] x"));
    }



    // C-436: no fragment of a different address is returned
    @Test
    public void testC436_emailFindingRespectsBoundaries() {
        assertNull(Strings.findFirstEmailAddress("Kontakt: m\u00FCller@firma.de"));
        assertNull(Strings.findFirstEmailAddress("j\u00F6rg@example.com"));
        assertNull(Strings.findFirstEmailAddress("user@mail.ex\u00E4mple.com"));
        assertNull(Strings.findFirstEmailAddress("a..b@c.com"));
        assertNull(Strings.findFirstEmailAddress("foo@bar@baz.com"));
        assertNull(Strings.findFirstEmailAddress("mu\u0308ller@x.com"));
        assertNull(Strings.findFirstEmailAddress("\uD801\uDC00249@a-b.com"));
        assertNull(Strings.findFirstEmailAddress("john@example.com-"));

        assertEquals("o'brien@example.com", Strings.findFirstEmailAddress("mail o'brien@example.com"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("{\"email\":\"john@x.com\"}"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("<john@x.com>"));
        assertEquals("john@x.com", Strings.findFirstEmailAddress("mailto:john@x.com"));
        assertEquals("bob@corp.io", Strings.findFirstEmailAddress("C:\\dir\\bob@corp.io"));
        assertEquals(Arrays.asList("test@gmail.orgg", "test2@gmail.cn"), Strings.findAllEmailAddresses("*** test@gmail.orgg&&^ test2@gmail.cn ((& "));
        assertEquals(EMOJI + " a@b.cc", EMOJI + " " + Strings.findFirstEmailAddress(EMOJI + " a@b.cc"));
    }

    @Test
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact: test@example.com"));
        assertNull(Strings.findFirstEmailAddress("No email here"));
        assertNull(Strings.findFirstEmailAddress(null));
    }

    @Test
    public void testFindFirstEmailAddress_EdgeCases() {
        assertEquals("test@gmail.com", Strings.findFirstEmailAddress("contact test@gmail.com for info"));
        assertNull(Strings.findFirstEmailAddress("no email here"));
        assertNull(Strings.findFirstEmailAddress(null));
        assertNull(Strings.findFirstEmailAddress(""));
    }

    // findFirstInteger

    @Test
    public void testFindFirstInteger() {
        assertEquals("123", Strings.findFirstInteger("abc123def"));
        assertNull(Strings.findFirstInteger("abc"));
        assertNull(Strings.findFirstInteger(null));
    }

    @Test
    public void testFindFirstInteger_EdgeCases() {
        assertEquals("123", Strings.findFirstInteger("abc123def"));
        assertEquals("-123", Strings.findFirstInteger("abc-123def"));
        assertNull(Strings.findFirstInteger("abc"));
        assertNull(Strings.findFirstInteger(null));
        assertNull(Strings.findFirstInteger(""));
    }

    @Test
    public void testFindFirstInteger_InsideDecimals() {
        assertEquals("12", Strings.findFirstInteger("12.99"));
        assertEquals("12", Strings.findFirstInteger("Price: $12.99"));
        assertEquals("1", Strings.findFirstInteger("1e10"));
        assertEquals("2", Strings.findFirstInteger("v2.0"));
        assertEquals("Price: $X.99", Strings.replaceFirstInteger("Price: $12.99", "X"));
        assertEquals("vX.0", Strings.replaceFirstInteger("v2.0", "X"));
    }

    @Test
    public void testFindFirstSciNumber() {
        assertEquals("1.23e10", Strings.findFirstDouble("value1.23e10test", true));
        assertEquals("4.56E-5", Strings.findFirstDouble("test4.56E-5", true));
        assertEquals("1e3", Strings.findFirstDouble("1e3test", true));

        assertEquals("-1.23e10", Strings.findFirstDouble("value-1.23e10test", true));

        assertNull(Strings.findFirstDouble("no sci numbers here", true));
        assertNull(Strings.findFirstDouble("", true));
        assertNull(Strings.findFirstDouble(null, true));
        assertEquals("123.45", Strings.findFirstDouble("123.45", true));

        assertEquals("1.23e10", Strings.findFirstDouble("first1.23e10second4.56e-5", true));
    }

    // indexOf



    // J-1: the empty-needle contract past the end of the string
    @Test
    public void testJ1_emptyNeedlePastTheEndOfTheString() {
        // indexOf / indexOfIgnoreCase clamp a beyond-length fromIndex to str.length()...
        assertEquals(3, Strings.indexOf("abc", "", 4));
        assertEquals(3, Strings.indexOf("abc", "", 3));
        assertEquals(3, Strings.indexOfIgnoreCase("abc", "", 4));
        assertEquals(3, Strings.lastIndexOf("abc", "", 4));

        // ...while the multi-candidate families report "no match at all" there.
        assertEquals(-1, Strings.indexOfAny("abc", 4, ""));
        assertEquals(-1, Strings.minIndexOfAll("abc", 4, ""));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 4, ""));

        // Exactly at the end they agree with indexOf again.
        assertEquals(3, Strings.indexOfAny("abc", 3, ""));
        assertEquals(3, Strings.minIndexOfAll("abc", 3, ""));
        assertEquals(3, Strings.maxIndexOfAll("abc", 3, ""));

        // And the in-range rows of the table are unchanged.
        assertEquals(0, Strings.indexOf("abc", ""));
        assertEquals(0, Strings.indexOfAny("abc", ""));
        assertEquals(0, Strings.minIndexOfAll("abc", ""));
        assertEquals(0, Strings.countMatches("abc", ""));
        assertEquals(List.of(0, 1, 2), Arrays.stream(Strings.indicesOf("abc", "").toArray()).boxed().toList());
    }

    @Test
    public void testIndexOf() {
        assertEquals(1, Strings.indexOf("hello", 'e'));
        assertEquals(-1, Strings.indexOf("hello", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'e'));
        assertEquals(-1, Strings.indexOf("", 'e'));

        assertEquals(1, Strings.indexOf("hello", "ell"));
        assertEquals(-1, Strings.indexOf("hello", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "ell"));
    }

    @Test
    public void test_indexOf() {
        assertEquals(0, Strings.lastIndexOf("aaaa", "aa", 0));
        assertEquals(1, Strings.lastIndexOf("aaaa", "aa", 1));
        assertEquals(2, Strings.lastIndexOf("aaaa", "aa", 2));
        assertEquals(2, Strings.lastIndexOf("aaaa", "aa", 3));
        assertEquals(0, "".indexOf(""));
        assertEquals(0, "".lastIndexOf(""));

        assertEquals(0, "abc".indexOf(""));
        assertEquals(3, "abc".lastIndexOf(""));

        assertEquals(4, StrUtil.indexOfToken("abc,,", "", ","));
        assertEquals(0, StrUtil.indexOfToken(",,abc", "", ","));
        assertEquals(1, StrUtil.indexOfToken(",,abc", "", ",", 1));
        assertEquals(-1, StrUtil.indexOfToken("abc,abc", "", ","));

        assertEquals(5, StrUtil.lastIndexOfToken("abc, aa, aa", "aa", ", ", 6));
        assertEquals(7, StrUtil.lastIndexOfToken("abc,aa,aa", "aa", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("aaaa", "aa", ",", 3));
        assertEquals(5, StrUtil.lastIndexOfToken("abc,,", "", ","));
        assertEquals(4, StrUtil.lastIndexOfToken("abc,,", "", ",", 4));
        assertEquals(5, StrUtil.lastIndexOfTokenIgnoreCase("abc,,", "", ","));
        assertEquals(4, StrUtil.lastIndexOfTokenIgnoreCase("abc,,", "", ",", 4));
        assertEquals(4, StrUtil.lastIndexOfTokenIgnoreCase("aEND", "", "end"));

        assertEquals(0, Strings.indexOf("abc,,", ""));
        assertEquals(1, Strings.indexOf("abc,,", "", 1));
        assertEquals(5, Strings.lastIndexOf("abc,,", ""));
        assertEquals(1, Strings.lastIndexOf("abc,,", "", 1));
        assertEquals(3, Strings.lastIndexOf("abc,,", "", 3));
        assertEquals(0, Strings.lastIndexOf("abc,,", "", 0));
        assertEquals(0, "aaa".indexOf("", -1));
        assertEquals(0, "aaa".indexOf("aa", -1));
        assertEquals(0, "aaa".indexOf(""));
        assertEquals(3, "aaa".indexOf("", 3));
        assertEquals(3, "aaa".indexOf("", 4));
        assertEquals(3, "aaa".lastIndexOf(""));
        assertEquals(0, "aaa".lastIndexOf("", 0));
        assertEquals(-1, "aaa".lastIndexOf("", -1));
        assertEquals(-1, "aaa".lastIndexOf("a", -1));

        assertEquals(5, Index.lastOfSubArray(Array.of(1, 2, 3, 4, 5), new int[] {}).orElseThrow());
        assertEquals(0, Index.lastOfSubArray(Array.of(1, 2, 3, 4, 5), 0, new int[] {}).orElseThrow());
    }

    @Test
    public void testIndexOf_Char() {
        assertEquals(0, Strings.indexOf("abc", 'a'));
        assertEquals(2, Strings.indexOf("abc", 'c'));
        assertEquals(-1, Strings.indexOf("abc", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'a'));
    }

    @Test
    public void testIndexOf_CharWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", 'a', 1));
        assertEquals(-1, Strings.indexOf("abc", 'a', 5));
        assertEquals(-1, Strings.indexOf(null, 'a', 0));
    }

    @Test
    public void testIndexOf_EmptyPattern() {
        // Mirrors String#indexOf semantics: empty pattern is found at index 0.
        assertEquals(0, Strings.indexOf("hello", ""));
        assertEquals(0, Strings.indexOf("", ""));
        // Null pattern -> NOT_FOUND.
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOf("hello", (String) null));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOf((String) null, ""));
    }

    @Test
    public void testIndexOf_String() {
        assertEquals(0, Strings.indexOf("abcdef", "abc"));
        assertEquals(3, Strings.indexOf("abcdef", "def"));
        assertEquals(-1, Strings.indexOf("abcdef", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "abc"));
    }

    @Test
    public void testIndexOf_StringWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", "abc", 1));
        assertEquals(-1, Strings.indexOf("abc", "abc", 5));
        assertEquals(-1, Strings.indexOf(null, "abc", 0));
    }

    @Test
    public void testC519_indexOfAtTheTerminalBoundary() {
        assertEquals(-1, Strings.indexOf("hello", "o", 5));
        assertEquals(5, Strings.indexOf("hello", "", 5));
        assertEquals(5, Strings.indexOf("hello", "", 99));
        assertEquals(-1, Strings.indexOf("hello", "o", 6));
    }

    @Test
    public void testIndexOfChar() {
        assertEquals(-1, Strings.indexOf(null, 'a'));
        assertEquals(-1, Strings.indexOf("", 'a'));
        assertEquals(0, Strings.indexOf("aabaabaa", 'a'));
        assertEquals(2, Strings.indexOf("aabaabaa", 'b'));
    }

    @Test
    public void testIndexOfString() {
        assertEquals(-1, Strings.indexOf(null, "a"));
        assertEquals(-1, Strings.indexOf("", "a"));
        assertEquals(0, Strings.indexOf("aabaabaa", "a"));
        assertEquals(2, Strings.indexOf("aabaabaa", "b"));
        assertEquals(1, Strings.indexOf("aabaabaa", "ab"));
    }



    /**
     * Contract pin for the delimiter-in-token rule now documented on the four {@code indexOfToken} javadocs: with a
     * NON-EMPTY delimiter, a token that itself contains the delimiter spans more than one field and can never match,
     * and the case-insensitive overloads apply that containment test case-insensitively.
     */
    @Test
    public void testIndexOfToken_TokenContainingDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ",", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", ",", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("a,b", "a,b", ",", 0));
        assertFalse(StrUtil.containsToken("x,a,b,y", "a,b", ","));
        // a plain search does find it
        assertEquals(2, Strings.indexOf("x,a,b,y", "a,b"));

        // C-538: an empty or null delimiter is rejected instead of degrading to a plain search
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfToken("x,a,b,y", "a,b", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfToken("x,a,b,y", "a,b", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", null));

        // the containment test of the IgnoreCase overloads is itself case-insensitive
        assertEquals(4, StrUtil.indexOfToken("pANDxandyANDq", "xandy", "AND"));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND"));
        assertEquals(4, StrUtil.indexOfToken("pANDxandyANDq", "xandy", "AND", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND", 0));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(3, Strings.indexOf("hello", 'l', 3));
        assertEquals(-1, Strings.indexOf("hello", 'e', 2));

        assertEquals(4, Strings.indexOf("hello hello", "o", 2));
        assertEquals(-1, Strings.indexOf("hello", "ell", 2));
    }

    @Test
    public void testIndexOf_Delimited_FromIndexDoesNotCreateBoundary() {
        assertEquals(10, StrUtil.indexOfToken("pineapple,apple", "apple", ",", 4));
        assertEquals(-1, StrUtil.indexOfToken("pineapple", "apple", ",", 4));
        assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineAPPLE,APPLE", "apple", ",", 4));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pineAPPLE", "apple", ",", 4));

        // A real multi-character separator may begin before the candidate-search lower bound.
        assertEquals(3, StrUtil.indexOfToken("a::b", "b", "::", 2));
        assertEquals(3, StrUtil.indexOfToken("a::b", "b", "::", 3));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("a::B", "b", "::", 2));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("a::B", "b", "::", 3));
    }

    @Test
    public void testIndexOf_Delimited_SeparatorOverlapsValue() {
        // "bab" | "a" | "bab" — first raw 'a' sits inside the leading separator
        assertEquals(3, StrUtil.indexOfToken("bababab", "a", "bab"));
        assertEquals(3, StrUtil.indexOfToken("bababab", "a", "bab", 0));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "a", "bab", 0));

        // Still rejects undelimited substrings
        assertEquals(-1, StrUtil.indexOfToken("bababab", "aba", "bab"));
        assertEquals(-1, StrUtil.indexOfToken("pineapple", "apple", ","));
        assertEquals(-1, StrUtil.indexOfToken("apple,ban,cherry", "banana", ","));

        // Multi-char separator, value only at end after separator
        assertEquals(4, StrUtil.indexOfToken("xy::z", "z", "::"));
        assertEquals(0, StrUtil.indexOfToken("z::xy", "z", "::"));
    }

    @Test
    public void testIndexOf_WithDelimiter() {
        // indexOf with delimiter searches for valueToFind as a whole token separated by delimiter
        int result = StrUtil.indexOfToken("a,b,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfToken(null, "b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", null, ","));
    }

    @Test
    public void testIndexOf_WithDelimiter_ValueAtEnd() {
        assertEquals(8, StrUtil.indexOfToken("one,two,three", "three", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken("one,two,three", "four", ",", 0));
    }

    @Test
    public void testIndexOf_WithDelimiterAndFromIndex() {
        int result = StrUtil.indexOfToken("a,b,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfToken(null, "b", ",", 0));
    }

    @Test
    public void testIndexOf_WithDelimiterAndFromIndex_ExtendedCoverage() {
        // C-538: an empty delimiter is rejected (a plain search is Strings.indexOf)
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfToken("a,b,c", "b", "", 0));

        // with delimiter, value found at start boundary
        assertEquals(2, StrUtil.indexOfToken("a,b,c", "b", ",", 0));

        // value at end boundary
        assertEquals(4, StrUtil.indexOfToken("a,b,c", "c", ",", 0));

        // value not found
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", "d", ",", 0));

        // null str
        assertEquals(-1, StrUtil.indexOfToken(null, "b", ",", 0));

        // null valueToFind
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", null, ",", 0));
    }

    // indexOfAny

    @Test
    public void testIndexOfAny() {
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'x'));
        assertEquals(-1, Strings.indexOfAny("hello", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny((String) null, Array.of('e')));
        assertEquals(-1, Strings.indexOfAny("", Array.of('e')));

        assertEquals(1, Strings.indexOfAny("hello", "ell", "xyz"));
        assertEquals(-1, Strings.indexOfAny("hello", "xyz", "abc"));
    }

    @Test
    public void testIndexOfAny_CharArray() {
        assertEquals(0, Strings.indexOfAny("abc", 'a', 'x'));
        assertEquals(1, Strings.indexOfAny("abc", 'b', 'c'));
        assertEquals(-1, Strings.indexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
    }

    @Test
    public void testIndexOfAny_CharArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, 'a'));
        assertEquals(-1, Strings.indexOfAny("abc", 5, 'a'));
    }



    /**
     * Contract pin for the "Passing exactly one character" note on {@code indexOfAny(String, char...)}: all four
     * overloads exist, so BOTH {@code char} overloads take part in the ambiguity and a single {@code char} has to be
     * passed as an explicit array. {@code indexOfAnyBut} has no {@code String} overloads and needs no workaround.
     */
    @Test
    public void testIndexOfAny_SingleCharacterNeedsExplicitArray() {
        // the documented workarounds
        assertEquals(0, Strings.indexOfAny("hello", new char[] { 'h' }));
        assertEquals(2, Strings.indexOf("hello", 'l'));
        // two or more characters resolve normally
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'o'));
        // indexOfAnyBut has no String overloads, so a single char compiles there
        assertEquals(1, Strings.indexOfAnyBut("hello", 'h'));
        assertEquals(3, Strings.lastIndexOfAny("hello", new char[] { 'l' }));
        assertEquals(4, Strings.minIndexOfAll("hello", new char[] { 'o' }));
        assertEquals(2, Strings.maxIndexOfAll("hello", new char[] { 'l' }));
        assertEquals(3, Strings.minLastIndexOfAll("hello", new char[] { 'l' }));
        assertEquals(3, Strings.maxLastIndexOfAll("hello", new char[] { 'l' }));

        // the overload set the note describes: both char overloads exist alongside both String ones
        for (final String name : new String[] { "indexOfAny", "lastIndexOfAny", "minIndexOfAll", "maxIndexOfAll", "minLastIndexOfAll", "maxLastIndexOfAll" }) {
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, char[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, int.class, char[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, String[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, int.class, String[].class), name);
        }
        Assertions.assertThrows(NoSuchMethodException.class, () -> Strings.class.getMethod("indexOfAnyBut", String.class, String[].class));
        Assertions.assertThrows(NoSuchMethodException.class, () -> Strings.class.getMethod("indexOfAnyBut", String.class, int.class, String[].class));
    }

    @Test
    public void testIndexOfAny_StringArray() {
        assertEquals(0, Strings.indexOfAny("abcdef", "abc", "xyz"));
        assertEquals(3, Strings.indexOfAny("abcdef", "def", "xyz"));
        assertEquals(-1, Strings.indexOfAny("abcdef", "xyz", "123"));
        assertEquals(-1, Strings.indexOfAny(null, "abc", "def"));
    }

    @Test
    public void testIndexOfAny_StringArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, "abc"));
        assertEquals(-1, Strings.indexOfAny("abc", 5, "abc"));
    }

    @Test
    public void testIndexOfAny_WithEmptyElementInVarargs() {
        // "" is tried first (per-needle order) and matches at fromIndex
        assertEquals(0, Strings.indexOfAny("hello world", 0, "", "world"));
        assertEquals(0, Strings.indexOfAny("hello world", 0, "", "xyz"));
        // a null element is still skipped, so "world" is found at 6
        assertEquals(6, Strings.indexOfAny("hello world", 0, null, "world"));
        // lastIndexOfAny: "" matches at min(startIndexFromBack, str.length()); null is skipped
        assertEquals(10, Strings.lastIndexOfAny("hello world", 10, "", "world"));
        assertEquals(6, Strings.lastIndexOfAny("hello world", 10, null, "world"));
        assertEquals(3, Strings.lastIndexOfAny("abc", ""));
        assertEquals(0, Strings.lastIndexOfAny("", ""));
        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));
        assertEquals(Strings.lastIndexOf("abc", ""), Strings.lastIndexOfAny("abc", ""));
        assertEquals(Strings.lastIndexOf("", ""), Strings.lastIndexOfAny("", ""));
        assertEquals(Strings.maxLastIndexOfAll("abc", ""), Strings.lastIndexOfAny("abc", ""));
    }

    @Test
    public void testIndexOfAny_WithFromIndex() {
        // basic found
        assertEquals(2, Strings.indexOfAny("hello", 0, 'l', 'z'));

        // fromIndex > 0 skips first occurrence
        assertEquals(3, Strings.indexOfAny("hello", 3, 'l', 'z'));

        // not found
        assertEquals(-1, Strings.indexOfAny("hello", 0, 'z', 'q'));

        // null/empty str
        assertEquals(-1, Strings.indexOfAny(null, 0, 'a'));
        assertEquals(-1, Strings.indexOfAny("", 0, 'a'));

        // empty valuesToFind
        assertEquals(-1, Strings.indexOfAny("hello", 0, new char[0]));

        // many chars (> 3) path - large char set
        assertEquals(0, Strings.indexOfAny("hello world", 0, 'h', 'e', 'l', 'o', ' '));
        assertEquals(-1, Strings.indexOfAny("hello world", 0, 'z', 'q', 'x', 'y', 'v'));
    }

    @Test
    public void testIndexOfAnyChars() {
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
        assertEquals(-1, Strings.indexOfAny("", 'a', 'b'));
        assertEquals(0, Strings.indexOfAny("zzabyycdxx", 'z', 'a'));
        assertEquals(3, Strings.indexOfAny("zzabyycdxx", 'b', 'd'));
    }

    @Test
    public void testIndexOfAnyStrings() {
        assertEquals(-1, Strings.indexOfAny(null, "ab", "cd"));
        assertEquals(-1, Strings.indexOfAny("", "ab", "cd"));
        assertEquals(2, Strings.indexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(6, Strings.indexOfAny("zzabyycdxx", "cd", "ef"));
    }

    @Test
    public void testC558_pastTheEndStartForwardIsMinusOneBackwardClamps() {
        assertEquals(-1, Strings.indexOfAny("abc", 4, ""));
        assertEquals(-1, Strings.minIndexOfAll("abc", 4, ""));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 4, ""));
        assertEquals(-1, Strings.indexOfAny("abc", Integer.MAX_VALUE, "", "a"));

        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));
        assertEquals(3, Strings.minLastIndexOfAll("abc", 100, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", 100, ""));

        // the single-needle families clamp in both directions
        assertEquals(3, Strings.indexOf("abc", "", 4));
        assertEquals(3, Strings.indexOfIgnoreCase("abc", "", 4));
        assertEquals(3, Strings.lastIndexOf("abc", "", 100));
        assertEquals(3, Strings.lastIndexOfIgnoreCase("abc", "", 100));

        // boundary: a start exactly at the length is in range for both
        assertEquals(3, Strings.indexOfAny("abc", 3, ""));
        assertEquals(3, Strings.lastIndexOfAny("abc", 3, ""));
        // empty haystack
        assertEquals(0, Strings.indexOfAny("", 0, ""));
        assertEquals(-1, Strings.indexOfAny("", 1, ""));
        assertEquals(0, Strings.lastIndexOfAny("", 5, ""));
    }

    // indexOfAnyBut

    @Test
    public void testIndexOfAnyBut() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 'a'));
        assertEquals(0, Strings.indexOfAnyBut("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 'a'));
        assertEquals(-1, Strings.indexOfAnyBut(null, 'a'));
    }

    @Test
    public void testIndexOfAnyBut_WithFromIndex() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 3, 'a'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 0, 'a'));
        assertEquals(2, Strings.indexOfAnyBut("abc", 2));
        assertEquals(-1, Strings.indexOfAnyBut("abc", 3));
    }

    @Test
    public void testC518_indexOfAnyButEmptyExclusionSetDivergesFromCommons() {
        assertEquals(0, Strings.indexOfAnyBut("bad"));
        assertEquals(0, Strings.indexOfAnyBut("bad", (char[]) null));
        assertEquals(1, Strings.indexOfAnyBut("bad", 1));
        assertEquals(1, Strings.indexOfAnyBut("bad", 1, (char[]) null));
        assertEquals(-1, Strings.indexOfAnyBut("bad", 3));
        assertEquals(-1, Strings.indexOfAnyBut(""));
        assertEquals(-1, Strings.indexOfAnyBut((String) null));
        assertEquals(-1, StringUtils.indexOfAnyBut("bad"));
        assertEquals(-1, StringUtils.indexOfAnyBut("bad", (char[]) null));
    }

    // indexOfDifference

    @Test
    public void testIndexOfDifference() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(-1, Strings.indexOfDifference(null, ""));
    }



    // C-025 / C-028: indexOfDifference results (the removed tail was unreachable)
    @Test
    public void testC025_indexOfDifference() {
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(2, Strings.indexOfDifference("abxyz", "ab"));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(-1, Strings.indexOfDifference(null, ""));
        // a code-unit index that can fall inside a surrogate pair, unlike lengthOfCommonPrefix
        assertEquals(1, Strings.indexOfDifference("😀", "😁"));
        assertEquals(0, Strings.lengthOfCommonPrefix("😀", "😁"));
    }

    @Test
    public void testIndexOfDifference_Array() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef", "abc123"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
    }

    @Test
    public void testIndexOfDifference_EdgeCases() {
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", null));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(2, Strings.indexOfDifference("abc", "abx"));
        assertEquals(3, Strings.indexOfDifference("abc", "abcd"));
    }

    @Test
    public void testIndexOfDifference_EmptyArrays() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference((String[]) null));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[0]));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { "abc" }));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { null, null }));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { "", "" }));
    }

    @Test
    public void testIndexOfDifference_Varargs() {
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot", "i am a maniac"));
        assertEquals(1, Strings.indexOfDifference("abc", "abc", "a"));
    }

    @Test
    public void testIndexOfDifference_VarArgs_EdgeCases() {
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz", "abc"));
        assertEquals(1, Strings.indexOfDifference("abc", "axc"));
        assertEquals(-1, Strings.indexOfDifference((String[]) null));
        assertEquals(-1, Strings.indexOfDifference());
    }

    @Test
    public void testU2001_indexOfDifferenceNullVsEmptyDivergesFromCommons() {
        assertEquals(-1, Strings.indexOfDifference((String) null, ""));
        assertEquals(-1, Strings.indexOfDifference("", (String) null));
        assertEquals(-1, Strings.indexOfDifference((String) null, (String) null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(0, Strings.indexOfDifference((String) null, "a"));
        assertEquals(0, Strings.indexOfDifference("a", ""));

        // the table's warning describes Commons Lang: 0 when exactly one side is null, -1 only for two nulls
        assertEquals(0, StringUtils.indexOfDifference(null, ""));
        assertEquals(0, StringUtils.indexOfDifference("", null));
        assertEquals(-1, StringUtils.indexOfDifference(null, null));
        assertEquals(-1, StringUtils.indexOfDifference("", ""));
    }

    @Test
    public void testIndexOfDifferenceTwoStrings() {
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", ""));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(2, Strings.indexOfDifference("abcde", "abxyz"));
        assertEquals(0, Strings.indexOfDifference("abcde", "xyz"));
    }

    @Test
    public void testIndexOfDifferenceVarArgs() {
        assertEquals(-1, Strings.indexOfDifference());
        assertEquals(-1, Strings.indexOfDifference((String[]) null));
        assertEquals(-1, Strings.indexOfDifference("abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(0, Strings.indexOfDifference(null, null, "abc"));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(1, Strings.indexOfDifference("abc", "a"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot"));
    }

    // indicesOf

    @Test
    public void testIndicesOf() {
        int[] indices = Strings.indicesOf("abcabc", "abc").toArray();
        assertArrayEquals(new int[] { 0, 3 }, indices);
        int[] indices2 = Strings.indicesOf("abc", "xyz").toArray();
        assertArrayEquals(new int[0], indices2);
    }

    @Test
    public void test_indicesOf() {
        {
            String ret = Strings.indicesOf("abca", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 1).join(", ");
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOf("abcA", "a").join(", ");
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 0).join(", ");
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 1).join(", ");
            assertEquals("", ret);
        }

        {
            String ret = Strings.indicesOfIgnoreCase("abca", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 1).join(", ");
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOfIgnoreCase("abcA", "a").join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 0).join(", ");
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 1).join(", ");
            assertEquals("3", ret);
        }

        {
            String ret = Strings.indicesOf("abca", "").join(", ");
            assertEquals("0, 1, 2, 3", ret);

            ret = Strings.indicesOf("abca", null).join(", ");
            assertEquals("", ret);

            ret = Strings.indicesOf(null, "").join(", ");
            assertEquals("", ret);

            ret = Strings.indicesOf("", null).join(", ");
            assertEquals("", ret);
        }

        {
            String ret = RegExUtil.matchIndices("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", "));
            assertEquals("14, 33", ret);

            ret = RegExUtil.matchResults("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .map(MatchResult::group)
                    .collect(Collectors.joining(", "));
            assertEquals("xyz@gmail.com, 123@outlook.cn", ret);
        }

    }

    @Test
    public void testIndicesOf_negativeFromIndexAndEmptyTargetBounds() {
        // regression: a negative fromIndex threw IllegalArgumentException (siblings clamp to 0), and an
        // empty valueToFind with fromIndex >= length emitted an out-of-range index
        assertArrayEquals(new int[] { 0, 3 }, Strings.indicesOf("abcabc", "a", -1).toArray());
        assertArrayEquals(new int[] { 0, 3 }, Strings.indicesOfIgnoreCase("abcAbc", "a", -2).toArray());
        assertEquals(0, Strings.indicesOf("abc", "", 3).count());
        assertEquals(0, Strings.indicesOf("abc", "", 4).count());

        // documented behavior unchanged
        assertArrayEquals(new int[] { 2, 3 }, Strings.indicesOf("abcA", "", 2).toArray());
        assertArrayEquals(new int[] { 3 }, Strings.indicesOf("abcabc", "a", 2).toArray());
    }

    @Test
    public void testIndicesOf_WithFromIndex() {
        int[] indices = Strings.indicesOf("abcabc", "abc", 1).toArray();
        assertNotNull(indices);
        assertEquals(1, indices.length);
        assertEquals(3, indices[0]);
    }

    @Test
    public void test_lastIndex() {
        assertEquals(2, "aba".lastIndexOf("a", 3));
        assertEquals(2, "aba".lastIndexOf("a", 2));
        assertEquals(0, "aba".lastIndexOf("a", 1));
        assertEquals(0, "aba".lastIndexOf("a", 0));
        assertEquals(-1, "aba".lastIndexOf("a", -1));
    }

    // lastIndexOf

    @Test
    public void testLastIndexOf() {
        assertEquals(3, Strings.lastIndexOf("hello", 'l'));
        assertEquals(-1, Strings.lastIndexOf("hello", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'l'));

        assertEquals(1, Strings.lastIndexOf("hello", "ell"));
        assertEquals(-1, Strings.lastIndexOf("hello", "xyz"));
    }

    @Test
    public void testLastIndexOf_Char() {
        assertEquals(3, Strings.lastIndexOf("abcabc", 'a'));
        assertEquals(5, Strings.lastIndexOf("abcabc", 'c'));
        assertEquals(-1, Strings.lastIndexOf("abc", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
    }

    @Test
    public void testLastIndexOf_CharWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", 'a', 2));
        assertEquals(-1, Strings.lastIndexOf("abc", 'c', 1));
    }

    @Test
    public void testLastIndexOf_String() {
        assertEquals(3, Strings.lastIndexOf("abcabc", "abc"));
        assertEquals(0, Strings.lastIndexOf("abc", "abc"));
        assertEquals(-1, Strings.lastIndexOf("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOf(null, "abc"));
    }

    @Test
    public void testLastIndexOf_StringWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOf("abc", "abc", -1));
    }

    @Test
    public void testLastIndexOfChar() {
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
        assertEquals(-1, Strings.lastIndexOf("", 'a'));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", 'a'));
        assertEquals(5, Strings.lastIndexOf("aabaabaa", 'b'));
    }

    @Test
    public void testLastIndexOfString() {
        assertEquals(-1, Strings.lastIndexOf(null, "a"));
        assertEquals(-1, Strings.lastIndexOf("", "a"));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", "a"));
        assertEquals(4, Strings.lastIndexOf("aabaabaa", "ab"));
    }



    /**
     * Contract pin for the delimiter-in-token rule now documented on the four {@code lastIndexOfToken} javadocs:
     * with a NON-EMPTY delimiter, a token that itself contains the delimiter spans more than one field and can never
     * match, and the case-insensitive overloads apply that containment test case-insensitively.
     */
    @Test
    public void testLastIndexOfToken_TokenContainingDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ",", 7));
        assertEquals(-1, StrUtil.lastIndexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("a,b", "a,b", ",", 3));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "A,B", ",", 7));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("a,b", "a,b", ",", 3));
        // a plain search does find it
        assertEquals(2, Strings.lastIndexOf("x,a,b,y", "a,b"));

        // C-538: an empty or null delimiter is rejected instead of degrading to a plain search
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.lastIndexOfToken("x,a,b,y", "a,b", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "A,B", ""));

        // the containment test of the IgnoreCase overloads is itself case-insensitive
        assertEquals(4, StrUtil.lastIndexOfToken("pANDxandyANDq", "xandy", "AND"));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND"));
        assertEquals(4, StrUtil.lastIndexOfToken("pANDxandyANDq", "xandy", "AND", 13));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND", 13));
    }

    @Test
    public void testLastIndexOf_WithDelimiter() {
        int result = StrUtil.lastIndexOfToken("a,b,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfToken(null, "b", ","));
    }



    /**
     * Regression: lastIndexOf(str, value, delimiter, startIndexFromBack) must not
     * return a position past startIndexFromBack. Before the fix, the leading-
     * delimiter branch returned the start-of-delimiter position from
     * String.lastIndexOf(), then added delimiter.length() — pushing the result
     * past the user-supplied search bound when the value was at the end of the string.
     * Per the documented example: lastIndexOf("test value test", "test", " ", 10) == 0.
     */
    @Test
    public void testLastIndexOf_WithDelimiter_doesNotExceedStartIndexFromBack() {
        // Documented example from Javadoc.
        assertEquals(0, StrUtil.lastIndexOfToken("test value test", "test", " ", 10));

        // The trailing "hello" at position 6 is the right answer when bounded at 10,
        // not the trailing "hello" at position 12.
        assertEquals(6, StrUtil.lastIndexOfToken("hello hello hello", "hello", " ", 10));

        // Multi-char delimiter case: the trailing token must respect the bound.
        assertEquals(0, StrUtil.lastIndexOfToken("abc :: xyz :: abc", "abc", " :: ", 10));

        // Within-bounds matches still resolve correctly.
        assertEquals(13, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ",", 20));
        assertEquals(0, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ",", 10));
    }

    @Test
    public void testLastIndexOf_WithDelimiterAndFromBack() {
        int result = StrUtil.lastIndexOfToken("a,b,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfToken(null, "b", ",", 0));
    }

    @Test
    public void testLastIndexOfAllWithOverlappingPatterns() {
        String text = "ababababab";
        Assertions.assertEquals(8, Strings.maxLastIndexOfAll(text, "ab", "ba"));
        Assertions.assertEquals(7, Strings.minLastIndexOfAll(text, "ab", "ba"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll(text, "xyz", "qrs"));
    }

    // lastIndexOfAny

    @Test
    public void testLastIndexOfAny() {
        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

    }

    @Test
    public void testLastIndexOfAny_CharArray() {
        assertEquals(0, Strings.lastIndexOfAny("abc", 'a', 'c'));
        assertEquals(5, Strings.lastIndexOfAny("abcabc", new char[] { 'c' }));
        assertEquals(-1, Strings.lastIndexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
    }

    @Test
    public void testLastIndexOfAny_StringArray() {
        assertEquals(3, Strings.lastIndexOfAny("abcabc", "abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", "xyz", "123"));
        assertEquals(-1, Strings.lastIndexOfAny(null, "abc"));
    }

    @Test
    public void testLastIndexOfAny_TreatsSurrogateAsCodeUnit() {
        // Lone surrogate code units are treated as ordinary UTF-16 code units (no validation, no exception).
        assertEquals(0, Strings.lastIndexOfAny("abc", '\uDC00', 'a'));
        assertEquals(0, Strings.lastIndexOfAny("abc", 2, '\uDC00', 'a'));
        // A lone surrogate that does not occur in the string simply does not match.
        assertEquals(-1, Strings.lastIndexOfAny("abc", '\uDC00', '\uDFFF'));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndex() {
        // basic usage
        assertEquals(4, Strings.lastIndexOfAny("hello", 10, 'o', 'x'));
        assertEquals(1, Strings.lastIndexOfAny("hello", 10, 'e', 'x'));
        // startIndexFromBack limits search
        assertEquals(-1, Strings.lastIndexOfAny("hello", 0, 'l', 'o'));
        assertEquals(0, Strings.lastIndexOfAny("hello", 0, 'h'));
        // null / empty
        assertEquals(-1, Strings.lastIndexOfAny(null, 5, 'a'));
        assertEquals(-1, Strings.lastIndexOfAny("hello", 5, new char[0]));
        // many chars (triggers set-based path)
        assertEquals(10, Strings.lastIndexOfAny("hello world", 10, 'a', 'b', 'c', 'd')); // 'd' is at index 10
        assertEquals(9, Strings.lastIndexOfAny("hello world", 10, 'l', 'r', 'o', 'x', 'y', 'z'));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndexFromBack_Char() {
        int result = Strings.lastIndexOfAny("hello world", 10, 'o', 'd');
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfAny(null, 0, Array.of('o')));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndexFromBack_String() {
        int result = Strings.lastIndexOfAny("hello world hello", 16, "hello", "xyz");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfAny(null, 0, "hello"));
    }

    @Test
    public void testLastIndexOfAnyChars() {
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] { 'a' }));
        assertEquals(1, Strings.lastIndexOfAny("zzabyycdxx", 'z', 'x'));
        assertEquals(7, Strings.lastIndexOfAny("zzabyycdxx", 'd', 'x'));
    }

    @Test
    public void testC523_lastIndexOfAnyNegativeStartAndMaxIndexOfAllFromIndex() {
        assertEquals(-1, Strings.lastIndexOfAny("abc", -1, "a"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", -1, ""));
        assertEquals(-1, Strings.lastIndexOfAny("abc", Integer.MIN_VALUE, "a", "b"));
        assertEquals(0, Strings.lastIndexOfAny("abc", 0, "a", "b"));
        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));

        assertEquals(2, Strings.maxIndexOfAll("abc", -5, "c", "a"));
        assertEquals(2, Strings.maxIndexOfAll("abc", 0, "c", "a"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 4, "a"));
        assertEquals(3, Strings.maxIndexOfAll("abc", 3, ""));
        assertEquals(3, Strings.maxIndexOfAll("abcabc", 1, "a", "c"));
    }

    @Test
    public void testLastIndexOfAnyStrings() {
        assertEquals(-1, Strings.lastIndexOfAny(null, "ab"));
        assertEquals(-1, Strings.lastIndexOfAny("", "ab"));
        assertEquals(2, Strings.lastIndexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(8, Strings.lastIndexOfAny("zzabyycdxx", "dd", "xx"));
    }

    @Test
    public void testLastIndexOfWithDelimiter_tokenAtStartShadowedByLaterOccurrence() {
        // regression: the start-of-string fallback only tested the LAST plain occurrence, so a valid
        // token at index 0 shadowed by a later non-delimited occurrence was missed
        assertEquals(0, StrUtil.lastIndexOfToken("ab,abx", "ab", ","));
        assertEquals(0, StrUtil.lastIndexOfTokenIgnoreCase("ab,abx", "AB", ","));

        // unchanged behavior
        assertEquals(3, StrUtil.lastIndexOfToken("ab,ab", "ab", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("xab,abx", "ab", ","));
    }

    @Test
    public void testLastOrdinalIndexOf() {
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("aabaabaa", "a", 6));
        assertEquals(2, Strings.lastOrdinalIndexOf("aabaabaa", "b", 2));
        assertEquals(1, Strings.lastOrdinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("aabaabaa", "c", 1));
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.lastOrdinalIndexOf("abcabc", "abc", 1));
        assertEquals(0, Strings.lastOrdinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("abcabc", "abc", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("", "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf("", null, 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, null, 1));
    }

    @Test
    public void testLastOrdinalIndexOf_EdgeCases() {
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(6, Strings.lastOrdinalIndexOf("aabaabaa", "a", 2));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
        assertEquals(-1, Strings.lastOrdinalIndexOf("aabaabaa", "a", 10));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "a", 1));
    }

    // maxIndexOfAll



    // D-4: saturating early-exit bounds in the max* families; == retained in the min* families
    @Test
    public void testD4_maxFamilySaturatingBound() {
        // The String forms can legitimately reach index == length via an empty needle; the >= bound covers it.
        assertEquals(3, Strings.maxIndexOfAll("abc", 3, ""));
        assertEquals(3, Strings.maxLastIndexOfAll("abc", 3, ""));
        assertEquals(2, Strings.maxIndexOfAll("abc", 0, "a", "c"));
        assertEquals(2, Strings.maxIndexOfAll("abc", 0, 'a', 'c'));
        assertEquals(2, Strings.maxLastIndexOfAll("abcabc", 2, 'a', 'c'));
        assertEquals(5, Strings.maxLastIndexOfAll("abcabc", 5, 'a', 'c'));
    }

    @Test
    public void testmaxIndexOfAll() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.maxIndexOfAll("abcdefg", "def", "abc"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    public void testMaxIndexOfAll() {
        assertEquals(6, Strings.maxIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.maxIndexOfAll("hello world", "xyz", "abc"));
    }

    @Test
    public void testMaxIndexOfAll_CharMembershipFastPath() {
        // shouldUseCharMembership requires >= 8 candidates and length * count >= 4096,
        // so this input exercises the bitset path rather than the per-candidate indexOf loop.
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 600; i++) {
            sb.append((char) ('a' + (i * 7) % 12));
        }

        final String str = sb.toString();
        final char[] candidates = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'z', 'y' };

        for (final int fromIndex : new int[] { -5, 0, 1, 7, 123, 599, 600 }) {
            int expected = CommonUtil.INDEX_NOT_FOUND;

            for (final char ch : candidates) {
                expected = Math.max(expected, str.indexOf(ch, Math.max(0, fromIndex)));
            }

            assertEquals(expected, Strings.maxIndexOfAll(str, fromIndex, candidates), "fromIndex=" + fromIndex);
        }

        // duplicates and candidates far outside the membership word range must not disturb the result
        assertEquals(2, Strings.maxIndexOfAll("abcabc", 0, 'a', 'a', 'b', 'b', 'c', 'c', '中', '￿', 'z', 'a'));
        // a candidate that never occurs contributes nothing
        assertEquals(-1, Strings.maxIndexOfAll("abcabc", 0, 'x', 'y', 'z', 'w', 'v', 'u', 't', 's', 'r', 'q'));
    }

    @Test
    public void testmaxIndexOfAll_WithFromIndex() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 5, "abc"));
    }

    @Test
    public void testMaxIndexOfAll_WithFromIndex() {
        int result = Strings.maxIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(6, result);
    }

    @Test
    public void testD4_minMaxFamiliesMatchBruteForceAcrossTheCharMembershipGate() {
        // shouldUseCharMembership triggers at >= 8 candidates and length*count >= 4096, so both the scalar
        // and the bitset paths are exercised here.
        final Random rnd = new Random(4L);

        for (final int alphabetSize : new int[] { 3, 12 }) {
            for (final int len : new int[] { 0, 1, 5, 40, 600 }) {
                for (int t = 0; t < 60; t++) {
                    final StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < len; i++) {
                        sb.append((char) ('a' + rnd.nextInt(alphabetSize)));
                    }

                    final String str = sb.toString();
                    final int candidateCount = 1 + rnd.nextInt(14);
                    final char[] chars = new char[candidateCount];
                    final String[] strs = new String[candidateCount];

                    for (int i = 0; i < candidateCount; i++) {
                        chars[i] = (char) ('a' + rnd.nextInt(alphabetSize + 2));
                        strs[i] = String.valueOf(chars[i]);
                    }

                    for (final int from : new int[] { 0, 1, len / 2, len, len + 5 }) {
                        assertEquals(bruteMaxFirst(str, from, strs), Strings.maxIndexOfAll(str, from, chars));
                        assertEquals(bruteMaxFirst(str, from, strs), Strings.maxIndexOfAll(str, from, strs));
                        assertEquals(bruteMinFirst(str, from, strs), Strings.minIndexOfAll(str, from, chars));
                        assertEquals(bruteMinFirst(str, from, strs), Strings.minIndexOfAll(str, from, strs));
                        assertEquals(bruteMaxLast(str, from, strs), Strings.maxLastIndexOfAll(str, from, chars));
                        assertEquals(bruteMaxLast(str, from, strs), Strings.maxLastIndexOfAll(str, from, strs));
                        assertEquals(bruteMinLast(str, from, strs), Strings.minLastIndexOfAll(str, from, chars));
                        assertEquals(bruteMinLast(str, from, strs), Strings.minLastIndexOfAll(str, from, strs));
                    }
                }
            }
        }
    }

    // maxLastIndexOfAll

    @Test
    public void testC520_charLastIndexOfAllStartIndexRules() {
        assertEquals(-1, Strings.minLastIndexOfAll("hello", -1, 'l'));
        assertEquals(-1, Strings.maxLastIndexOfAll("hello", -1, 'l'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 100, 'l', 'o'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 100, 'l', 'o'));
        assertEquals(3, Strings.minLastIndexOfAll("hello", 5, 'l', 'o'));
        assertEquals(4, Strings.maxLastIndexOfAll("hello", 4, 'l', 'o'));
        assertEquals(2, Strings.maxLastIndexOfAll("hello", 2, 'l', 'o'));
    }

    @Test
    public void testmaxLastIndexOfAll() {
        assertEquals(6, Strings.maxLastIndexOfAll("abcabcabc", "abc"));
        assertEquals(6, Strings.maxLastIndexOfAll("abcdefabc", "abc", "def"));
    }

    @Test
    public void testMaxLastIndexOfAll() {
        int result = Strings.maxLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.maxLastIndexOfAll(null, "a"));
    }

    @Test
    public void testmaxLastIndexOfAllWithIndex() {
        Assertions.assertEquals(7, Strings.maxLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.maxLastIndexOfAll("Hello World", 6, "o", "World"));
        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("Hello World", 3, "o", "H"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("test", 100, "test"));
    }

    // minIndexOfAll

    @Test
    public void testC521_indexOfAllFamilyEmptyHaystackRowsByOverload() {
        assertEquals(-1, Strings.minIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.maxIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.minLastIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.maxLastIndexOfAll("", new char[] { 'a' }));
        assertEquals(-1, Strings.indexOfAny("", new char[] { 'a' }));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] { 'a' }));
        assertEquals(-1, Strings.indexOfAnyBut("", new char[] { 'a' }));

        assertEquals(0, Strings.minIndexOfAll("", ""));
        assertEquals(0, Strings.maxIndexOfAll("", ""));
        assertEquals(0, Strings.minLastIndexOfAll("", ""));
        assertEquals(0, Strings.maxLastIndexOfAll("", ""));
        assertEquals(0, Strings.indexOfAny("", ""));
        assertEquals(0, Strings.lastIndexOfAny("", ""));
        assertEquals(-1, Strings.minIndexOfAll("", "a"));
        assertEquals(-1, Strings.minIndexOfAll((String) null, ""));
    }

    @Test
    public void testminIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.minIndexOfAll("abcdefg", "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    public void testMinIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.minIndexOfAll("hello world", "xyz", "abc"));
        assertEquals(-1, Strings.minIndexOfAll(null, "a"));
    }



    // G073-02: minIndexOfAll bounds each later candidate's search by the current hit; pin matches that straddle the
    // bound, empty/null/overlong candidates and every fromIndex against an unbounded reference.
    @Test
    public void testMinIndexOfAll_boundedSearchMatchesUnboundedReference() {
        assertEquals(1, Strings.minIndexOfAll("abcd", "c", "bcd"));
        assertEquals(1, Strings.minIndexOfAll("abcd", "c", "bcde", "bc"));
        assertEquals(2, Strings.minIndexOfAll("abcd", "c", "bcde", "cd"));
        assertEquals(0, Strings.minIndexOfAll("abcd", "d", "b", ""));
        assertEquals(2, Strings.minIndexOfAll("abcd", 2, "d", "zz", "", "c"));
        assertEquals(1, Strings.minIndexOfAll("aaab", 1, "b", "aab"));
        assertEquals(3, Strings.minIndexOfAll("aaab", 2, "b", "aab"));
        assertEquals(1, Strings.minIndexOfAll("aaab", 1, "b", "aa"));
        assertEquals(1, Strings.minIndexOfAll("abcd", 'c', 'b', 'z'));
        assertEquals(2, Strings.minIndexOfAll("abcd", 'c', 'd', 'z'));
        assertEquals(1, Strings.minIndexOfAll("x\u4E2D\u4E2D", 0, 'q', '\u4E2D', 'z'));

        final Random random = new Random(0x6073_02L);
        final String alphabet = "abcab\u4E2D";

        for (int testCase = 0; testCase < 3000; testCase++) {
            // Lengths past 256 reach the ranged search, which is used only when the skipped tail is long.
            final int length = testCase < 2000 ? random.nextInt(12) : 250 + random.nextInt(700);
            final StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            final String str = sb.toString();
            final String[] values = new String[1 + random.nextInt(5)];

            for (int i = 0; i < values.length; i++) {
                final int kind = random.nextInt(10);

                if (kind == 0) {
                    values[i] = null;
                } else if (kind == 1) {
                    values[i] = "";
                } else {
                    final int valueLength = 1 + random.nextInt(kind == 2 ? 15 : 4);
                    final StringBuilder value = new StringBuilder(valueLength);

                    for (int j = 0; j < valueLength; j++) {
                        value.append((alphabet + "z").charAt(random.nextInt(alphabet.length() + 1)));
                    }

                    values[i] = value.toString();
                }
            }

            final char[] chars = new char[1 + random.nextInt(7)];

            for (int i = 0; i < chars.length; i++) {
                chars[i] = (alphabet + "z").charAt(random.nextInt(alphabet.length() + 1));
            }

            for (int fromIndex = -2; fromIndex <= length + 1; fromIndex += length < 20 || fromIndex < 20 ? 1 : 1 + random.nextInt(40)) {
                assertEquals(minIndexOfAllStringReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values), str);
                assertEquals(minIndexOfAllCharReference(str, fromIndex, chars), Strings.minIndexOfAll(str, fromIndex, chars), str);
            }

            for (final int fromIndex : new int[] { length - 1, length, length + 1 }) {
                assertEquals(minIndexOfAllStringReference(str, fromIndex, values), Strings.minIndexOfAll(str, fromIndex, values), str);
                assertEquals(minIndexOfAllCharReference(str, fromIndex, chars), Strings.minIndexOfAll(str, fromIndex, chars), str);
            }

            assertEquals(minIndexOfAllStringReference(str, 0, values), Strings.minIndexOfAll(str, values), str);
            assertEquals(minIndexOfAllCharReference(str, 0, chars), Strings.minIndexOfAll(str, chars), str);
        }

        final String longStr = "abcd" + "e".repeat(400) + "bcd";
        assertEquals(1, Strings.minIndexOfAll(longStr, "c", "bcd"));
        assertEquals(2, Strings.minIndexOfAll(longStr, "c", "cde", "zz"));
        assertEquals(1, Strings.minIndexOfAll(longStr, "c", "bcde", "abcdX"));
        assertEquals(2, Strings.minIndexOfAll(longStr, 2, "d", "cd", "abcd"));
        assertEquals(2, Strings.minIndexOfAll(longStr, 2, "d", ""));
        assertEquals(0, Strings.minIndexOfAll(longStr, "e", "abcd"));
        assertEquals(3, Strings.minIndexOfAll(longStr, "e", "de", "abcde".repeat(2)));
        assertEquals(4, Strings.minIndexOfAll(longStr, "e", "e".repeat(400), "e".repeat(401)));
        assertEquals(404, Strings.minIndexOfAll(longStr, 5, "bcd", "e".repeat(401), "cd"));
        assertEquals(3, Strings.minIndexOfAll(longStr, 'd', 'z', 'x'));
        assertEquals(1, Strings.minIndexOfAll(longStr, 'd', 'z', 'b'));
    }

    @Test
    public void testminIndexOfAll_WithFromIndex() {
        assertEquals(3, Strings.minIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", 5, "abc"));
    }

    @Test
    public void testMinIndexOfAll_WithFromIndex() {
        int result = Strings.minIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(1, result);
    }

    // minLastIndexOfAll

    @Test
    public void testD4_minFamilyKeepsTheSentinelBelowTheBound() {
        // A <= bound would break out on the initial -1 before any candidate was tried; these would then
        // all return -1 instead of a real index.
        assertEquals(0, Strings.minIndexOfAll("abc", 0, 'a'));
        assertEquals(0, Strings.minIndexOfAll("abc", 0, "a"));
        assertEquals(0, Strings.minLastIndexOfAll("abc", 2, 'a'));
        assertEquals(0, Strings.minLastIndexOfAll("abc", 2, "a"));
        assertEquals(1, Strings.minIndexOfAll("abc", 1, 'b', 'c'));
        // minLastIndexOfAll is the minimum over candidates of each candidate's LAST index, not the earliest
        // occurrence of any candidate: with only 'c' that is lastIndexOf('c', 5) == 5.
        assertEquals(5, Strings.minLastIndexOfAll("abcabc", 5, 'c'));
        assertEquals(3, Strings.minLastIndexOfAll("abcabc", 5, 'a', 'c'));
        // Nothing found still yields -1 rather than an early false positive.
        assertEquals(-1, Strings.minIndexOfAll("abc", 0, 'z'));
        assertEquals(-1, Strings.minLastIndexOfAll("abc", 2, 'z'));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 0, 'z'));
        assertEquals(-1, Strings.maxLastIndexOfAll("abc", 2, 'z'));
    }

    @Test
    public void testminLastIndexOfAll() {
        assertEquals(3, Strings.minLastIndexOfAll("abcdefabc", "abc", "def"));
        assertEquals(6, Strings.minLastIndexOfAll("abcabcabc", "abc"));
    }

    @Test
    public void testMinLastIndexOfAll() {
        int result = Strings.minLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.minLastIndexOfAll(null, "a"));
    }



    /**
     * The fast path must agree with the scalar path on both sides of the exact {@code shouldUseCharMembership}
     * crossover, so the optimization cannot be "verified" only where it never runs.
     */
    @Test
    public void testMinLastIndexOfAll_agreesAcrossTheExactFastPathCrossover() {
        final String str = repeatingString(600, "abcdefghij");
        final char[] eight = charsOf("cdefghij");
        final char[] seven = charsOf("defghij");

        // 511 * 8 == 4088 < 4096 -> scalar; 512 * 8 == 4096 -> fast path. Same answer either way.
        assertEquals(refMinLastIndexOfAll(str, FIRST_FAST_PATH_START_INDEX - 1, eight), Strings.minLastIndexOfAll(str, FIRST_FAST_PATH_START_INDEX - 1, eight));
        assertEquals(refMinLastIndexOfAll(str, FIRST_FAST_PATH_START_INDEX, eight), Strings.minLastIndexOfAll(str, FIRST_FAST_PATH_START_INDEX, eight));

        // Fewer than 8 candidates never reaches the fast path, whatever the length.
        assertEquals(refMinLastIndexOfAll(str, str.length(), seven), Strings.minLastIndexOfAll(str, str.length(), seven));

        // Sanity: the crossover constants above really are the boundary of the gate.
        assertTrue((long) FIRST_FAST_PATH_START_INDEX * MIN_VALUE_COUNT_FOR_FAST_PATH < 4096);
        assertTrue((long) (FIRST_FAST_PATH_START_INDEX + 1) * MIN_VALUE_COUNT_FOR_FAST_PATH >= 4096);
    }

    @Test
    public void testMinLastIndexOfAll_fastPathAllCandidatesPresent() {
        // "abcdefghij" repeated; the last occurrence of each of the 10 letters lies in the final block.
        final String str = repeatingString(1000, "abcdefghij");
        final char[] all = charsOf("abcdefghij");

        // Last block starts at 990: 'a'@990 ... 'j'@999. The minimum of those last indices is 'a' at 990.
        assertEquals(990, Strings.minLastIndexOfAll(str, str.length(), all));
        assertEquals(refMinLastIndexOfAll(str, str.length(), all), Strings.minLastIndexOfAll(str, str.length(), all));
    }



    /** A candidate larger than every character in the string sizes the bitset; it must simply never match. */
    @Test
    public void testMinLastIndexOfAll_fastPathCandidateAboveEveryCharacterInTheString() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] withHighCandidate = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', (char) 0xFFFF };

        assertEquals(refMinLastIndexOfAll(str, str.length(), withHighCandidate), Strings.minLastIndexOfAll(str, str.length(), withHighCandidate));
        assertEquals(990, Strings.minLastIndexOfAll(str, str.length(), withHighCandidate));
    }

    @Test
    public void testMinLastIndexOfAll_fastPathDuplicateCandidatesDoNotChangeTheResult() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] withDuplicates = charsOf("aaaaaaaab");
        final char[] deduplicated = charsOf("ab");

        assertEquals(Strings.minLastIndexOfAll(str, str.length(), deduplicated), Strings.minLastIndexOfAll(str, str.length(), withDuplicates));
        assertEquals(refMinLastIndexOfAll(str, str.length(), withDuplicates), Strings.minLastIndexOfAll(str, str.length(), withDuplicates));
    }



    /** A match at index 0 short-circuits the scalar loop; the fast path must reach the same answer. */
    @Test
    public void testMinLastIndexOfAll_fastPathMatchAtIndexZero() {
        final String str = "z" + repeatingString(999, "abcdefghij");
        final char[] withZ = charsOf("zabcdefgh");

        assertEquals(0, Strings.minLastIndexOfAll(str, str.length(), withZ));
        assertEquals(refMinLastIndexOfAll(str, str.length(), withZ), Strings.minLastIndexOfAll(str, str.length(), withZ));
    }

    @Test
    public void testMinLastIndexOfAll_fastPathNoCandidatePresent() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] absent = charsOf("ABCDEFGHIJKL");

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(str, str.length(), absent));
        assertEquals(refMinLastIndexOfAll(str, str.length(), absent), Strings.minLastIndexOfAll(str, str.length(), absent));
    }

    @Test
    public void testMinLastIndexOfAll_fastPathRespectsStartIndexFromBack() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] all = charsOf("abcdefghij");

        for (final int start : new int[] { 511, 512, 700, 995, 999, 1000, Integer.MAX_VALUE }) {
            assertEquals(refMinLastIndexOfAll(str, start, all), Strings.minLastIndexOfAll(str, start, all), "startIndexFromBack=" + start);
        }
    }

    @Test
    public void testMinLastIndexOfAll_fastPathSomeCandidatesPresent() {
        final String str = repeatingString(1000, "abcdefghij");
        // Only 'b' and 'c' occur; the others are outside the alphabet.
        final char[] mixed = charsOf("bcXYZWVU");

        assertEquals(refMinLastIndexOfAll(str, str.length(), mixed), Strings.minLastIndexOfAll(str, str.length(), mixed));
        assertEquals(991, Strings.minLastIndexOfAll(str, str.length(), mixed)); // last 'b'
    }

    @Test
    public void testMinLastIndexOfAll_fastPathTreatsSurrogatesAsPlainCodeUnits() {
        // "x" + high surrogate + low surrogate, repeated; candidates include the bare surrogate code units.
        final String block = "x" + (char) 0xD83D + (char) 0xDE00;
        final String str = repeatingString(1200, block);
        final char[] candidates = { 'x', (char) 0xD83D, (char) 0xDE00, 'a', 'b', 'c', 'd', 'e', 'f', 'g' };

        assertEquals(refMinLastIndexOfAll(str, str.length(), candidates), Strings.minLastIndexOfAll(str, str.length(), candidates));
    }



    /** Guard against the fast path being silently disabled: the gated and ungated results must agree. */
    @Test
    public void testMinLastIndexOfAll_gatedAndUngatedInputsGiveTheSameAnswerForTheSameContent() {
        final char[] eight = charsOf("abcdefgh");
        final String shortStr = repeatingString(100, "abcdefgh"); // below the gate
        final String longStr = repeatingString(4096, "abcdefgh"); // above the gate

        // Both strings end with a complete "abcdefgh" block, so the minimum of the last indices is the 'a'
        // of that final block in each case.
        assertEquals(shortStr.length() - 8, Strings.minLastIndexOfAll(shortStr, shortStr.length(), eight));
        assertEquals(longStr.length() - 8, Strings.minLastIndexOfAll(longStr, longStr.length(), eight));
        assertEquals(refMinLastIndexOfAll(shortStr, shortStr.length(), eight), Strings.minLastIndexOfAll(shortStr, shortStr.length(), eight));
        assertEquals(refMinLastIndexOfAll(longStr, longStr.length(), eight), Strings.minLastIndexOfAll(longStr, longStr.length(), eight));
    }



    /** The no-index overload delegates with {@code N.len(str)} and must behave the same. */
    @Test
    public void testMinLastIndexOfAll_noIndexOverloadMatchesTheIndexedOne() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] all = charsOf("abcdefghij");

        assertEquals(Strings.minLastIndexOfAll(str, str.length(), all), Strings.minLastIndexOfAll(str, all));
    }

    @Test
    public void testMinLastIndexOfAll_nullEmptyAndNegativeAreUnchanged() {
        final char[] many = charsOf("abcdefghij");

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(null, 100, many));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll("", 100, many));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(repeatingString(1000, "abc"), -1, many));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(repeatingString(1000, "abc"), 100, new char[0]));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(repeatingString(1000, "abc"), 100, (char[]) null));
    }

    @Test
    public void testMinLastIndexOfAll_randomizedDifferentialAgainstScalarReference() {
        final Random rnd = new Random(20260830L);

        for (int it = 0; it < 4000; it++) {
            final int len = 1 + rnd.nextInt(it % 5 == 0 ? 900 : 40);
            final int alphabetSize = 1 + rnd.nextInt(20);
            final StringBuilder sb = new StringBuilder(len);

            for (int i = 0; i < len; i++) {
                sb.append((char) ('a' + rnd.nextInt(alphabetSize)));
            }

            final String str = sb.toString();
            final char[] values = new char[1 + rnd.nextInt(20)];

            for (int i = 0; i < values.length; i++) {
                values[i] = (char) ('a' + rnd.nextInt(alphabetSize + 4));
            }

            final int start = rnd.nextInt(len + 4) - 2;

            assertEquals(refMinLastIndexOfAll(str, start, values), Strings.minLastIndexOfAll(str, start, values),
                    "len=" + len + " start=" + start + " count=" + values.length);
        }
    }



    /** The {@code String...} overload has no fast path; it must be unaffected by this change. */
    @Test
    public void testMinLastIndexOfAll_stringOverloadUnaffected() {
        final String str = repeatingString(1000, "abcdefghij");

        assertEquals(990, Strings.minLastIndexOfAll(str, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(str, "XX", "YY"));
    }

    @Test
    public void testminLastIndexOfAllWithIndex() {
        Assertions.assertEquals(6, Strings.minLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(4, Strings.minLastIndexOfAll("Hello World", 5, "o", "World"));
        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("Hello World", 3, "o", "World"));

        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.minLastIndexOfAll("test", 100, "test"));
    }

    // ordinalIndexOf

    @Test
    public void testOrdinalIndexOf() {
        assertEquals(0, Strings.ordinalIndexOf("", "", 1));
        assertEquals(-1, Strings.ordinalIndexOf("", "", 2));
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(5, Strings.ordinalIndexOf("aabaabaa", "b", 2));
        assertEquals(4, Strings.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(0, Strings.ordinalIndexOf("abcabc", "abc", 1));
        assertEquals(3, Strings.ordinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.ordinalIndexOf("abcabc", "abc", 3));
        assertEquals(-1, Strings.ordinalIndexOf(null, "abc", 1));
        assertEquals(0, Strings.ordinalIndexOf("abc", "", 1));
        assertEquals(1, Strings.ordinalIndexOf("abc", "", 2));
        assertEquals(2, Strings.ordinalIndexOf("abc", "", 3));
        // Empty needle matches at every UTF-16 code-unit boundary, including length.
        assertEquals(3, Strings.ordinalIndexOf("abc", "", 4));
        assertEquals(-1, Strings.ordinalIndexOf("abc", "", 5));
        assertEquals(3, Strings.lastOrdinalIndexOf("abc", "", 1));
        assertEquals(2, Strings.lastOrdinalIndexOf("abc", "", 2));
        assertEquals(0, Strings.lastOrdinalIndexOf("abc", "", 4));
        assertEquals(-1, Strings.lastOrdinalIndexOf("abc", "", 5));
        assertEquals(0, Strings.countMatches("abc", ""));
        assertEquals(-1, Strings.ordinalIndexOf("", null, 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, null, 1));
    }

    @Test
    public void testOrdinalIndexOf_EdgeCases() {
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(1, Strings.ordinalIndexOf("aabaabaa", "a", 2));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(-1, Strings.ordinalIndexOf("aabaabaa", "a", 10));
        assertEquals(-1, Strings.ordinalIndexOf(null, "a", 1));
        assertEquals(-1, Strings.ordinalIndexOf("abc", null, 1));
    }

    @Test
    public void testOrdinalIndexOfBehaviourUnchangedForValidOrdinals() {
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(4, Strings.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.ordinalIndexOf("aabaabaa", "c", 1));
        assertEquals(3, Strings.ordinalIndexOf("abc", "", 4));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.ordinalIndexOf("abc", "", 5));
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(1, Strings.lastOrdinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(0, Strings.lastOrdinalIndexOf("abc", "", 4));
    }

    @Test
    public void testOrdinalIndexOfRejectsNonPositiveOrdinalWithTheStandardMessage() {
        for (final int ordinal : new int[] { 0, -1, Integer.MIN_VALUE }) {
            final IllegalArgumentException first = assertThrows(IllegalArgumentException.class, () -> Strings.ordinalIndexOf("abc", "a", ordinal));
            assertEquals("'ordinal' cannot be zero or negative: " + ordinal, first.getMessage());

            final IllegalArgumentException last = assertThrows(IllegalArgumentException.class, () -> Strings.lastOrdinalIndexOf("abc", "a", ordinal));
            assertEquals("'ordinal' cannot be zero or negative: " + ordinal, last.getMessage());
        }

        // The ordinal is validated before the null/short-input short-circuits, exactly as before.
        assertThrows(IllegalArgumentException.class, () -> Strings.ordinalIndexOf(null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.lastOrdinalIndexOf(null, null, 0));
    }

    @Test
    public void test_regularExpression() {
        assertEquals("123", Strings.findFirstInteger("abc123"));
        assertEquals("-12.34e+5", Strings.findFirstDouble("abc-12.34e+5xyz", true));
        assertEquals(Double.parseDouble("-12.34e+5"), Numbers.extractFirstDouble("abc-12.34e+5xyz", true).get());

        assertEquals(false, RegExUtil.INTEGER_MATCHER.matcher("123 456").find());
        assertEquals(true, RegExUtil.INTEGER_FINDER.matcher("123 456").find());

        assertEquals(false, RegExUtil.INTEGER_MATCHER.matcher("123 456").matches());
        assertEquals(false, RegExUtil.INTEGER_FINDER.matcher("123 456").matches());

        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("123@email.com").matches());
        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("123@email.com").find());

        assertEquals(false, RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher(" 123@email.com ").find());
        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher(" 123@email.com ").find());
    }

    @Test
    public void testStartsWith() {
        assertTrue(Strings.startsWith("hello", "hel"));
        assertTrue(Strings.startsWith("test", "test"));
        assertTrue(Strings.startsWith("abcdef", "abc"));
        assertTrue(Strings.startsWith("abc", "ab"));
        assertTrue(Strings.startsWith("abc", ""));
        assertTrue(Strings.startsWith("", ""));
        assertTrue(Strings.startsWith("Hello", "Hello"));

        assertFalse(Strings.startsWith("ab", "abc"));
        assertFalse(Strings.startsWith("", "a"));
        assertFalse(Strings.startsWith("abcdef", "xyz"));
        assertFalse(Strings.startsWith("abc", "bc"));
        assertFalse(Strings.startsWith("Hello", "hello"));
        assertFalse(Strings.startsWith("TEST", "test"));
        assertFalse(Strings.startsWith(null, "prefix"));
        assertFalse(Strings.startsWith(null, null));
        assertFalse(Strings.startsWith("test", null));
        assertFalse(Strings.startsWith(null, "abc"));
    }

    @Test
    public void testStartsWithAny() {
        assertTrue(Strings.startsWithAny("hello", "hel", "test", "world"));
        assertTrue(Strings.startsWithAny("test", "no", "te", "maybe"));
        assertTrue(Strings.startsWithAny("abc", "xyz", "abc", "def"));
        assertTrue(Strings.startsWithAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.startsWithAny("abc", "xy", "ab", "cd"));

        assertFalse(Strings.startsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.startsWithAny("xyz", "a", "b", "c"));
        assertFalse(Strings.startsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAny(null, "abc"));
        assertFalse(Strings.startsWithAny(null, "a", "b"));
        assertFalse(Strings.startsWithAny(null, "ab"));
        assertFalse(Strings.startsWithAny("test", (String[]) null));
        assertFalse(Strings.startsWithAny("test"));
    }

    @Test
    public void testU2001_startsWithAnyAndEndsWithAnyOnEmptyHaystackDivergeFromCommons() {
        assertTrue(Strings.startsWithAny("", ""));
        assertTrue(Strings.endsWithAny("", ""));
        assertTrue(Strings.startsWithAny("", "x", ""));
        assertTrue(Strings.endsWithAny("", "x", ""));
        assertFalse(Strings.startsWithAny("", "x"));
        assertFalse(Strings.endsWithAny("", "x"));
        assertFalse(Strings.startsWithAny(null, ""));
        assertFalse(Strings.endsWithAny(null, ""));

        // Commons Lang returns false for an empty haystack even when a search string is ""
        assertFalse(StringUtils.startsWithAny("", ""));
        assertFalse(StringUtils.endsWithAny("", ""));
        assertFalse(StringUtils.startsWithAny("", "x", ""));

        // the single-value forms agree on both sides
        assertTrue(Strings.startsWith("", ""));
        assertTrue(Strings.endsWith("", ""));
        assertTrue(StringUtils.startsWith("", ""));
        assertTrue(StringUtils.endsWith("", ""));
        assertTrue(Strings.startsWithAny("a", ""));
        assertTrue(StringUtils.startsWithAny("a", ""));
    }

    @Test
    public void testStrUtilEmptyNeedleWithOversizedSearchIndex() {
        // C-538: an empty or null delimiter is rejected; the plain search is Strings.indexOf("ab", "", 10) == 2.
        assertEquals(2, Strings.indexOf("ab", "", 10));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfToken("ab", "", "", 10));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.indexOfTokenIgnoreCase("ab", "", null, 10));

        // Delimited search uses the same clamp, then still requires the position to be an empty field.
        assertEquals(3, StrUtil.indexOfToken("ab,", "", ",", 10));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("ab,", "", ",", 10));
        assertEquals(0, StrUtil.indexOfToken("", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfToken("ab", "", ",", 10));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("ab", "", ",", 10));

        // Backward searches already clamp oversized indices and follow the same empty-field rule.
        assertEquals(3, StrUtil.lastIndexOfToken("ab,", "", ",", 10));
        assertEquals(3, StrUtil.lastIndexOfTokenIgnoreCase("ab,", "", ",", 10));
        assertEquals(-1, StrUtil.lastIndexOfToken("ab", "", ",", 10));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("ab", "", ",", 10));
    }

}
