package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the fixes of the 2026-08-30d review of {@link Strings}.
 *
 * <ul>
 *   <li><b>D-1</b> {@code minLastIndexOfAll(String, int, char...)} gained the char-membership fast path that its
 *       six siblings already had; it was doing one {@code lastIndexOf} scan per candidate.</li>
 *   <li><b>D-2</b> {@code join(Iterable, ...)} now passes a known {@code Collection} size through as a buffer
 *       capacity hint instead of discarding it at the {@code Iterator} boundary.</li>
 *   <li><b>D-3</b> the {@code substringsBetween}/{@code substringIndicesBetween} overloads now declare the
 *       unchecked exceptions their javadoc documents.</li>
 *   <li><b>D-4</b> {@code ordinalIndexOf} validates through {@code N.checkArgPositive} instead of a hand-rolled
 *       message.</li>
 *   <li><b>J-1</b> {@code isValidUrl}'s empty-port-delimiter / user-info rejections apply to {@code file} only.</li>
 *   <li><b>J-2</b> every {@code max}-taking {@code split*} overload validates {@code max} before a {@code null}
 *       or empty {@code str} can short-circuit (now documented on all twelve).</li>
 *   <li><b>J-3</b> {@code join(T[])} is equivalent to {@code join(a, 0, a.length, ELEMENT_SEPARATOR)} for all
 *       nine array types (now documented on all nine).</li>
 * </ul>
 */
public class StringsRegressionBTest extends TestBase {

    // shouldUseCharMembership: valueCount >= 8 && (long) strLength * valueCount >= 4096.
    // minLastIndexOfAll passes (startIndexFromBack + 1) as strLength, so with 8 candidates the fast path
    // starts exactly at startIndexFromBack == 511 (512 * 8 == 4096).
    private static final int MIN_VALUE_COUNT_FOR_FAST_PATH = 8;
    private static final int FIRST_FAST_PATH_START_INDEX = 511;

    // ---------------------------------------------------------------- D-1

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

    @Test
    public void testMinLastIndexOfAll_fastPathNoCandidatePresent() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] absent = charsOf("ABCDEFGHIJKL");

        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(str, str.length(), absent));
        assertEquals(refMinLastIndexOfAll(str, str.length(), absent), Strings.minLastIndexOfAll(str, str.length(), absent));
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
    public void testMinLastIndexOfAll_fastPathDuplicateCandidatesDoNotChangeTheResult() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] withDuplicates = charsOf("aaaaaaaab");
        final char[] deduplicated = charsOf("ab");

        assertEquals(Strings.minLastIndexOfAll(str, str.length(), deduplicated), Strings.minLastIndexOfAll(str, str.length(), withDuplicates));
        assertEquals(refMinLastIndexOfAll(str, str.length(), withDuplicates), Strings.minLastIndexOfAll(str, str.length(), withDuplicates));
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
    public void testMinLastIndexOfAll_fastPathRespectsStartIndexFromBack() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] all = charsOf("abcdefghij");

        for (final int start : new int[] { 511, 512, 700, 995, 999, 1000, Integer.MAX_VALUE }) {
            assertEquals(refMinLastIndexOfAll(str, start, all), Strings.minLastIndexOfAll(str, start, all), "startIndexFromBack=" + start);
        }
    }

    @Test
    public void testMinLastIndexOfAll_fastPathTreatsSurrogatesAsPlainCodeUnits() {
        // "x" + high surrogate + low surrogate, repeated; candidates include the bare surrogate code units.
        final String block = "x" + (char) 0xD83D + (char) 0xDE00;
        final String str = repeatingString(1200, block);
        final char[] candidates = { 'x', (char) 0xD83D, (char) 0xDE00, 'a', 'b', 'c', 'd', 'e', 'f', 'g' };

        assertEquals(refMinLastIndexOfAll(str, str.length(), candidates), Strings.minLastIndexOfAll(str, str.length(), candidates));
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

    /** A match at index 0 short-circuits the scalar loop; the fast path must reach the same answer. */
    @Test
    public void testMinLastIndexOfAll_fastPathMatchAtIndexZero() {
        final String str = "z" + repeatingString(999, "abcdefghij");
        final char[] withZ = charsOf("zabcdefgh");

        assertEquals(0, Strings.minLastIndexOfAll(str, str.length(), withZ));
        assertEquals(refMinLastIndexOfAll(str, str.length(), withZ), Strings.minLastIndexOfAll(str, str.length(), withZ));
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

    /** The no-index overload delegates with {@code N.len(str)} and must behave the same. */
    @Test
    public void testMinLastIndexOfAll_noIndexOverloadMatchesTheIndexedOne() {
        final String str = repeatingString(1000, "abcdefghij");
        final char[] all = charsOf("abcdefghij");

        assertEquals(Strings.minLastIndexOfAll(str, str.length(), all), Strings.minLastIndexOfAll(str, all));
    }

    /** The {@code String...} overload has no fast path; it must be unaffected by this change. */
    @Test
    public void testMinLastIndexOfAll_stringOverloadUnaffected() {
        final String str = repeatingString(1000, "abcdefghij");

        assertEquals(990, Strings.minLastIndexOfAll(str, "a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.minLastIndexOfAll(str, "XX", "YY"));
    }

    // ---------------------------------------------------------------- D-2

    /** A Collection whose {@code size()} disagrees with its iterator: the size is only a capacity hint. */
    private static final class LyingSizeCollection extends AbstractCollection<String> {
        private final List<String> elements;
        private final int reportedSize;

        LyingSizeCollection(final List<String> elements, final int reportedSize) {
            this.elements = elements;
            this.reportedSize = reportedSize;
        }

        @Override
        public Iterator<String> iterator() {
            return elements.iterator();
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }

    /** A plain {@code Iterable} that is not a {@code Collection}, so no size is available. */
    private static Iterable<String> iterableOf(final List<String> elements) {
        return elements::iterator;
    }

    @Test
    public void testJoinIterable_sizeIsOnlyACapacityHint_underReportedSize() {
        final List<String> elements = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals("a, b, c, d, e", Strings.join(new LyingSizeCollection(elements, 1), ", "));
        assertEquals("[a|b|c|d|e]", Strings.join(new LyingSizeCollection(elements, 0), "|", "[", "]"));
    }

    @Test
    public void testJoinIterable_sizeIsOnlyACapacityHint_overReportedAndNegativeSize() {
        final List<String> elements = Arrays.asList("a", "b");

        assertEquals("a, b", Strings.join(new LyingSizeCollection(elements, 1_000_000), ", "));
        // A negative size must not reach new StringBuilder(int); calculateBufferSize clamps it.
        assertEquals("a, b", Strings.join(new LyingSizeCollection(elements, -1), ", "));
        assertEquals("<a-b>", Strings.join(new LyingSizeCollection(elements, -5), "-", "<", ">"));
    }

    @Test
    public void testJoinIterable_nonCollectionIterableStillWorks() {
        final List<String> elements = Arrays.asList(" a ", " b ");

        assertEquals(" a ,  b ", Strings.join(iterableOf(elements), ", "));
        assertEquals("[ a | b ]", Strings.join(iterableOf(elements), "|", "[", "]"));
        assertEquals("[a|b]", Strings.join(iterableOf(elements), "|", "[", "]", true));
    }

    @Test
    public void testJoinIterable_nullAndEmptyUnchanged() {
        assertEquals("", Strings.join((Iterable<?>) null, ", "));
        assertEquals("", Strings.join(new ArrayList<>(), ", "));
        assertEquals("Start:  :End", Strings.join((Iterable<?>) null, ", ", "Start: ", " :End"));
        assertEquals("[]", Strings.join(new ArrayList<>(), ", ", "[", "]"));
        assertEquals("]", Strings.join((Iterable<?>) null, ", ", "", "]"));
        assertEquals("[", Strings.join((Iterable<?>) null, ", ", "[", ""));
        assertEquals("", Strings.join(iterableOf(new ArrayList<>()), ", "));
    }

    @Test
    public void testJoinIterable_agreesWithArrayIteratorAndCollectionRange() {
        final Random rnd = new Random(830L);
        final String[] delimiters = { ", ", "-", "", null };
        final String[] affixes = { "", "[", null };

        for (int it = 0; it < 800; it++) {
            final int len = rnd.nextInt(6);
            final List<Object> list = new ArrayList<>();

            for (int i = 0; i < len; i++) {
                list.add(rnd.nextInt(4) == 0 ? null : (rnd.nextBoolean() ? " s" + i + " " : (Object) i));
            }

            final Object[] array = list.toArray();
            final String delimiter = delimiters[rnd.nextInt(delimiters.length)];
            final String prefix = affixes[rnd.nextInt(affixes.length)];
            final String suffix = affixes[rnd.nextInt(affixes.length)];
            final boolean trim = rnd.nextBoolean();

            final List<String> asStrings = new ArrayList<>();

            for (final Object o : list) {
                asStrings.add(o == null ? null : String.valueOf(o));
            }

            final String fromArray = Strings.join(array, 0, len, delimiter, prefix, suffix, trim);

            assertEquals(fromArray, Strings.join(list, delimiter, prefix, suffix, trim), "Iterable(List)");
            assertEquals(fromArray, Strings.join(list.iterator(), delimiter, prefix, suffix, trim), "Iterator");
            assertEquals(fromArray, Strings.join(list, 0, len, delimiter, prefix, suffix, trim), "Collection range");
            assertEquals(fromArray, Strings.join(iterableOf(asStrings), delimiter, prefix, suffix, trim), "non-Collection Iterable");
            assertEquals(fromArray, Strings.join(new LyingSizeCollection(asStrings, 1), delimiter, prefix, suffix, trim), "Collection with a lying size()");
        }
    }

    @Test
    public void testJoinIterable_largeCollectionMatchesArray() {
        final int n = 5000;
        final String[] array = new String[n];

        for (int i = 0; i < n; i++) {
            array[i] = "element" + i;
        }

        final List<String> list = Arrays.asList(array);
        final LinkedHashSet<String> set = new LinkedHashSet<>(list);

        assertEquals(Strings.join(array, ", "), Strings.join(list, ", "));
        assertEquals(Strings.join(array, ", "), Strings.join(set, ", "));
        assertEquals(Strings.join(array, ", "), Strings.join(list.iterator(), ", "));
        assertEquals(Strings.join(array, ", ", "[", "]"), Strings.join(list, ", ", "[", "]"));
    }

    @Test
    public void testJoinIterable_singleElementAndNullElement() {
        assertEquals("only", Strings.join(Arrays.asList("only"), ", "));
        assertEquals("null", Strings.join(java.util.Collections.singletonList(null), ", "));
        assertEquals("[only]", Strings.join(Arrays.asList("only"), ", ", "[", "]"));
        assertEquals("a, null, b", Strings.join(Arrays.asList("a", null, "b"), ", "));
    }

    // ---------------------------------------------------------------- D-3

    @Test
    public void testSubstringsBetweenDeclaredExceptionsAreActuallyThrown() {
        // Range overloads: a null input has length zero, so only (0, 0) is a valid range.
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween(null, 0, 1, '[', ']'));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween(null, 0, 1, "[", "]"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween("abc", 0, 4, "[", "]"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween("abc", 0, 4, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringIndicesBetween(null, 0, 1, '[', ']'));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringIndicesBetween(null, 0, 1, "[", "]"));

        // Mode-taking overloads reject a null mode collaborator-first.
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", '[', ']', null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", "[", "]", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween("a[b]c", '[', ']', null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween("a[b]c", "[", "]", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(null, 0, 0, "[", "]", null, 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", 0, 5, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, -1));

        // ... and the no-mode overloads still cannot throw, because they supply a non-null constant.
        assertEquals(0, Strings.substringsBetween(null, '[', ']').size());
        assertEquals(0, Strings.substringsBetween(null, "[", "]").size());
        assertEquals(1, Strings.substringsBetween("a[b]c", '[', ']').size());
    }

    // ---------------------------------------------------------------- D-4

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

    // ---------------------------------------------------------------- J-1

    @Test
    public void testIsValidUrl_emptyPortDelimiterAndUserInfoAcceptedOutsideFileScheme() {
        // RFC 3986 allows an empty port, so http/https/ftp accept an authority ending in ':'.
        assertTrue(Strings.isValidUrl("http://host:/"));
        assertTrue(Strings.isValidUrl("https://host:"));
        assertTrue(Strings.isValidUrl("ftp://host:/"));
        assertTrue(Strings.isValidHttpUrl("http://host:/"));

        // ... which means a Windows drive letter is NOT rejected outside the file scheme.
        assertTrue(Strings.isValidUrl("http://C:/path"));

        // The file scheme rejects both, which is what the extra guard exists for.
        assertFalse(Strings.isValidUrl("file://host:/"));
        assertFalse(Strings.isValidUrl("file://host:"));
        assertFalse(Strings.isValidUrl("file://C:/path"));

        // User-info is likewise accepted for http/https/ftp and rejected for file.
        assertTrue(Strings.isValidUrl("http://user:pw@host/"));
        assertTrue(Strings.isValidHttpUrl("http://user:pw@host/"));
        assertFalse(Strings.isValidUrl("file://user@host/path"));

        // A valid file URL with no authority still passes.
        assertTrue(Strings.isValidUrl("file:///C:/Users/doc.txt"));
    }

    // ---------------------------------------------------------------- J-2

    @Test
    public void testEveryMaxTakingSplitOverloadValidatesMaxBeforeNullOrEmptyStr() {
        for (final String str : new String[] { null, "" }) {
            for (final int max : new int[] { 0, -1 }) {
                assertMaxRejected(() -> Strings.split(str, ',', max), max);
                assertMaxRejected(() -> Strings.split(str, ',', max, true), max);
                assertMaxRejected(() -> Strings.split(str, ",", max), max);
                assertMaxRejected(() -> Strings.split(str, ",", max, true), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ',', max), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ',', max, true), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max), max);
                assertMaxRejected(() -> Strings.splitPreserveAllTokens(str, ",", max, true), max);
                assertMaxRejected(() -> Strings.splitOnWhitespace(str, max), max);
                assertMaxRejected(() -> Strings.splitOnWhitespace(str, max, true), max);
                assertMaxRejected(() -> Strings.splitOnWhitespacePreserveAllTokens(str, max), max);
                assertMaxRejected(() -> Strings.splitOnWhitespacePreserveAllTokens(str, max, true), max);
            }
        }
    }

    private static void assertMaxRejected(final org.junit.jupiter.api.function.Executable call, final int max) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertEquals("'max' cannot be zero or negative: " + max, e.getMessage());
    }

    @Test
    public void testSplitPreserveAllTokensValidatesDelimiterBeforeNullOrEmptyStr() {
        for (final String str : new String[] { null, "" }) {
            for (final String delimiter : new String[] { null, "" }) {
                assertDelimiterRejected(() -> Strings.split(str, delimiter));
                assertDelimiterRejected(() -> Strings.split(str, delimiter, true));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, true));
                // For the max-taking String-delimiter overloads the max is checked first, then the delimiter.
                assertDelimiterRejected(() -> Strings.split(str, delimiter, 3));
                assertDelimiterRejected(() -> Strings.split(str, delimiter, 3, true));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, 3));
                assertDelimiterRejected(() -> Strings.splitPreserveAllTokens(str, delimiter, 3, true));
            }
        }
    }

    private static void assertDelimiterRejected(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertEquals("'delimiter' cannot be null or empty", e.getMessage());
    }

    /** The documented order for the two-collaborator overloads is max first, then delimiter. */
    @Test
    public void testMaxIsValidatedBeforeDelimiter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Strings.split(null, (String) null, 0));
        assertEquals("'max' cannot be zero or negative: 0", e.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens(null, (String) null, 0, true));
        assertEquals("'max' cannot be zero or negative: 0", e2.getMessage());
    }

    /** A valid max still short-circuits on a null/empty input exactly as documented. */
    @Test
    public void testValidMaxStillShortCircuitsOnNullOrEmptyStr() {
        assertArrayEquals(new String[0], Strings.split(null, ',', 3));
        assertArrayEquals(new String[0], Strings.split("", ',', 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespace(null, 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespace("", 3, true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ',', 3));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', 3));
        assertArrayEquals(new String[0], Strings.splitOnWhitespacePreserveAllTokens(null, 3));
        assertArrayEquals(new String[] { "" }, Strings.splitOnWhitespacePreserveAllTokens("", 3, true));
    }

    // ---------------------------------------------------------------- J-3

    @Test
    public void testJoinArrayEqualsExplicitRangeWithElementSeparator() {
        final String sep = Strings.ELEMENT_SEPARATOR;
        final Random rnd = new Random(1830L);

        for (int it = 0; it < 200; it++) {
            final int len = rnd.nextInt(6);
            final boolean[] booleans = new boolean[len];
            final char[] chars = new char[len];
            final byte[] bytes = new byte[len];
            final short[] shorts = new short[len];
            final int[] ints = new int[len];
            final long[] longs = new long[len];
            final float[] floats = new float[len];
            final double[] doubles = new double[len];
            final Object[] objects = new Object[len];

            for (int i = 0; i < len; i++) {
                booleans[i] = rnd.nextBoolean();
                chars[i] = (char) ('a' + rnd.nextInt(26));
                bytes[i] = (byte) rnd.nextInt(256);
                shorts[i] = (short) rnd.nextInt(65536);
                ints[i] = rnd.nextInt();
                longs[i] = rnd.nextLong();
                floats[i] = rnd.nextFloat();
                doubles[i] = rnd.nextDouble();
                objects[i] = rnd.nextInt(3) == 0 ? null : "v" + i;
            }

            assertEquals(Strings.join(booleans, 0, len, sep), Strings.join(booleans));
            assertEquals(Strings.join(chars, 0, len, sep), Strings.join(chars));
            assertEquals(Strings.join(bytes, 0, len, sep), Strings.join(bytes));
            assertEquals(Strings.join(shorts, 0, len, sep), Strings.join(shorts));
            assertEquals(Strings.join(ints, 0, len, sep), Strings.join(ints));
            assertEquals(Strings.join(longs, 0, len, sep), Strings.join(longs));
            assertEquals(Strings.join(floats, 0, len, sep), Strings.join(floats));
            assertEquals(Strings.join(doubles, 0, len, sep), Strings.join(doubles));
            assertEquals(Strings.join(objects, 0, len, sep), Strings.join(objects));
        }

        // A null array returns "" instead of throwing, unlike the range overload's checkFromToIndex contract.
        assertEquals("", Strings.join((boolean[]) null));
        assertEquals("", Strings.join((char[]) null));
        assertEquals("", Strings.join((byte[]) null));
        assertEquals("", Strings.join((short[]) null));
        assertEquals("", Strings.join((int[]) null));
        assertEquals("", Strings.join((long[]) null));
        assertEquals("", Strings.join((float[]) null));
        assertEquals("", Strings.join((double[]) null));
        assertEquals("", Strings.join((Object[]) null));
    }

    // ---------------------------------------------------------------- cs.ordinal

    @Test
    public void testCsOrdinalConstantMatchesItsName() {
        assertNotNull(cs.ordinal);
        assertEquals("ordinal", cs.ordinal);
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
}
