package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Strings. */
@Tag("slow-test")
public class StringsPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G068 end ----
    // ---- perf review 2026-10-08 G069 begin ----
    @Nested
    class G069 {

        /**
         * Verbatim copy of the pristine ASCII algorithm behind Strings.toSnakeCase / toScreamingSnakeCase / toKebabCase
         * (Strings.toDelimitedCaseAscii), used as the reference for ASCII input.
         */
        private String referenceDelimitedCaseAscii(final String str, final char separator, final boolean toUpperCase) {
            if (str == null || str.isEmpty()) {
                return str;
            }

            final int len = str.length();
            final StringBuilder result = new StringBuilder(len + 16);

            for (int i = 0; i < len;) {
                final char ch = str.charAt(i);

                if (ch == '_' || ch == '-' || Character.isWhitespace(ch)) {
                    final boolean atStart = result.length() == 0;

                    do {
                        i++;
                    } while (i < len && (str.charAt(i) == '_' || str.charAt(i) == '-' || Character.isWhitespace(str.charAt(i))));

                    if (!atStart && i < len) {
                        result.append(separator);
                    }

                    continue;
                }

                if (isUpper(ch) && i > 0 && (isLower(str.charAt(i - 1)) || (i + 1 < len && isLower(str.charAt(i + 1)))) && result.length() > 0
                        && result.charAt(result.length() - 1) != separator) {
                    result.append(separator);
                }

                result.append(toUpperCase ? isLower(ch) ? (char) (ch - ('a' - 'A')) : ch : isUpper(ch) ? (char) (ch + ('a' - 'A')) : ch);
                i++;
            }

            return result.toString();
        }

        private boolean isLower(final char ch) {
            return ch >= 'a' && ch <= 'z';
        }

        private boolean isUpper(final char ch) {
            return ch >= 'A' && ch <= 'Z';
        }

        private void assertDelimitedCasesMatchReference(final String input) {
            assertEquals(referenceDelimitedCaseAscii(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(referenceDelimitedCaseAscii(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(referenceDelimitedCaseAscii(input, '-', false), Strings.toKebabCase(input), input);
        }

        // G069-01: ASCII input that is already in the target form converts to an equal string.
        @Test
        void testToDelimitedCase_alreadyInTargetForm() {
            for (final String snake : new String[] { "id", "first_name", "a_b_c", "x1_y2", "a.b$c", "1", "a1_2b", "v2_api_key", "name" }) {
                assertEquals(snake, Strings.toSnakeCase(snake));
                assertDelimitedCasesMatchReference(snake);
            }

            for (final String screaming : new String[] { "ID", "FIRST_NAME", "HTTP2_URL", "A_B_C", "X.Y$Z", "9", "MAX_VALUE" }) {
                assertEquals(screaming, Strings.toScreamingSnakeCase(screaming));
                assertDelimitedCasesMatchReference(screaming);
            }

            for (final String kebab : new String[] { "id", "first-name", "a-b-c", "x1-y2", "content-type" }) {
                assertEquals(kebab, Strings.toKebabCase(kebab));
                assertDelimitedCasesMatchReference(kebab);
            }

            // already-converted results stay stable (idempotence for ASCII input)
            for (final String camel : new String[] { "firstName", "HTTPStatusCode", "userID", "base64URL", "IOError", "a-b c_d" }) {
                final String snake = Strings.toSnakeCase(camel);
                final String screaming = Strings.toScreamingSnakeCase(camel);
                final String kebab = Strings.toKebabCase(camel);
                assertEquals(snake, Strings.toSnakeCase(snake));
                assertEquals(screaming, Strings.toScreamingSnakeCase(screaming));
                assertEquals(kebab, Strings.toKebabCase(kebab));
            }
        }

        // G069-01: input one step away from the target form is still converted.
        @Test
        void testToDelimitedCase_nearTargetFormStillConverted() {
            assertEquals("a", Strings.toSnakeCase("_a"));
            assertEquals("a", Strings.toSnakeCase("a_"));
            assertEquals("", Strings.toSnakeCase("_"));
            assertEquals("a_b", Strings.toSnakeCase("a__b"));
            assertEquals("a_b", Strings.toSnakeCase("a-b"));
            assertEquals("a_b", Strings.toSnakeCase("a b"));
            assertEquals("a_b", Strings.toSnakeCase("a\tb"));
            assertEquals("a_b", Strings.toSnakeCase("a\u001Fb"));
            assertEquals("a_b", Strings.toSnakeCase("a_-b"));
            assertEquals("a_b", Strings.toSnakeCase("aB"));
            assertEquals("ab", Strings.toSnakeCase("Ab"));
            assertEquals("first_name", Strings.toSnakeCase("first_Name"));
            assertEquals("A", Strings.toScreamingSnakeCase("a"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("aB"));
            assertEquals("A_BC", Strings.toScreamingSnakeCase("ABc"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("A__B"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("A-B"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("_A_B_"));
            assertEquals("first-name", Strings.toKebabCase("first_name"));
            assertEquals("a", Strings.toKebabCase("-a-"));
            assertEquals("a-b", Strings.toKebabCase("a--b"));
            assertEquals("a-b", Strings.toKebabCase("aB"));

            for (final String input : new String[] { "_a", "a_", "_", "-", " ", "a__b", "a-b", "a b", "a\tb", "a\u001Fb", "a_-b", "aB", "Ab", "first_Name", "a",
                    "A", "ABc", "A__B", "A-B", "_A_B_", "first_name", "-a-", "a--b", "a_ b", "a _b", "A1b", "1aB", "a.B" }) {
                assertDelimitedCasesMatchReference(input);
            }
        }

        // G069-01: every ASCII string up to length 6 over a small alphabet converts exactly like the reference.
        @Test
        void testToDelimitedCase_exhaustiveShortAsciiMatchesReference() {
            final char[] alphabet = { 'a', 'Z', '_', '-', ' ', '1', '\u001F' };
            final char[] buffer = new char[6];

            for (int len = 1; len <= 6; len++) {
                final int[] digits = new int[len];

                while (true) {
                    for (int i = 0; i < len; i++) {
                        buffer[i] = alphabet[digits[i]];
                    }

                    assertDelimitedCasesMatchReference(new String(buffer, 0, len));

                    int position = len - 1;

                    while (position >= 0 && ++digits[position] == alphabet.length) {
                        digits[position] = 0;
                        position--;
                    }

                    if (position < 0) {
                        break;
                    }
                }
            }
        }

        // G069-01: random ASCII strings and their already-converted forms (the fast path) match the reference; splitChar overloads too.
        @Test
        void testToDelimitedCase_randomAndReconvertedAsciiMatchesReference() {
            final String pool = "abcxyzABCXYZ0189__--  \t\n\r.$#\u001C";
            final Random random = new Random(20261008L);

            for (int n = 0; n < 20_000; n++) {
                final int len = random.nextInt(31);
                final StringBuilder sb = new StringBuilder(len);

                for (int i = 0; i < len; i++) {
                    final int mode = random.nextInt(10);
                    sb.append(mode < 4 ? (char) ('a' + random.nextInt(26))
                            : mode < 6 ? (char) ('A' + random.nextInt(26)) : pool.charAt(random.nextInt(pool.length())));
                }

                final String input = sb.toString();
                assertDelimitedCasesMatchReference(input);
                assertDelimitedCasesMatchReference(Strings.toSnakeCase(input));
                assertDelimitedCasesMatchReference(Strings.toScreamingSnakeCase(input));
                assertDelimitedCasesMatchReference(Strings.toKebabCase(input));

                final String dotted = input.replace('.', '-');
                assertEquals(referenceDelimitedCaseAscii(dotted, '_', false), Strings.toSnakeCase(input, '.'), input);
                assertEquals(referenceDelimitedCaseAscii(dotted, '_', true), Strings.toScreamingSnakeCase(input, '.'), input);
                assertEquals(referenceDelimitedCaseAscii(dotted, '-', false), Strings.toKebabCase(input, '.'), input);
            }
        }

        // G069-01: null/empty and non-ASCII input (anywhere in the string) keep their paths.
        @Test
        void testToDelimitedCase_nullEmptyAndNonAscii() {
            assertNull(Strings.toSnakeCase(null));
            assertNull(Strings.toScreamingSnakeCase(null));
            assertNull(Strings.toKebabCase(null));
            assertEquals("", Strings.toSnakeCase(""));
            assertEquals("", Strings.toScreamingSnakeCase(""));
            assertEquals("", Strings.toKebabCase(""));

            assertEquals("\u00E9a", Strings.toSnakeCase("\u00C9a"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a\u00C9"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a_\u00C9"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a_\u00E9"));
            assertEquals("A_\u00C9", Strings.toScreamingSnakeCase("a_\u00E9"));
            assertEquals("a-b\u00E9", Strings.toKebabCase("aB\u00E9"));
            assertEquals("a_b", Strings.toSnakeCase("a_\u2003b"));
            assertEquals("first_name", Strings.toSnakeCase("first\u2003name"));
            assertEquals("first_name_\u00E9", Strings.toSnakeCase("first_name_\u00E9"));
        }
    }

    // ---- perf review 2026-10-08 G069 end ----
    // ---- perf review 2026-10-08 G070 begin ----
    @Nested
    class G070 {
        // Alphabet for the split differential tests: delimiters, ASCII space, tab, a char <= ' ' that is not Java whitespace
        // (U+0001), a char <= ' ' that is Java whitespace (U+001F), NBSP (> ' ', not trimmed) and EM SPACE (Java whitespace,
        // > ' ', not trimmed; it also makes the string UTF-16).
        private final char[] splitAlphabet = { 'a', 'b', 'c', ',', ',', ':', ':', ' ', ' ', '\t', (char) 0x0001, (char) 0x001F, (char) 0x00A0, (char) 0x2003 };

        private String randomString(final Random random, final char[] alphabet, final int length) {
            final char[] chars = new char[length];

            for (int i = 0; i < length; i++) {
                chars[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(chars);
        }

        private String[] trimAll(final String[] tokens) {
            final String[] result = new String[tokens.length];

            for (int i = 0; i < tokens.length; i++) {
                result[i] = tokens[i].trim();
            }

            return result;
        }

        private String describe(final String str) {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < str.length(); i++) {
                sb.append(String.format("%04x ", (int) str.charAt(i)));
            }

            return sb.toString();
        }

        private boolean containsAllReference(final String str, final char[] valuesToFind) {
            for (final char ch : valuesToFind) {
                if (str.indexOf(ch) < 0) {
                    return false;
                }
            }

            return true;
        }

        // G070-02: split(..., trim = true) trims each selected token without the intermediate untrimmed substring; the result
        // must equal the untrimmed split with String.trim() applied to every element (trimming happens after token selection).
        @Test
        public void testSplitTrim_matchesTrimmedUntrimmedSplitForAllWorkers() {
            final Random random = new Random(20261008L);
            final int[] maxValues = { 1, 2, 3, 4, Integer.MAX_VALUE };

            for (int iteration = 0; iteration < 4000; iteration++) {
                final String str = randomString(random, splitAlphabet, random.nextInt(24));
                final int max = maxValues[random.nextInt(maxValues.length)];
                final String context = "str=" + describe(str) + " max=" + max;

                // char delimiter worker
                assertArrayEquals(trimAll(Strings.split(str, ',')), Strings.split(str, ',', true), context);
                assertArrayEquals(trimAll(Strings.split(str, ',', max)), Strings.split(str, ',', max, true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, ',')), Strings.splitPreserveAllTokens(str, ',', true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, ',', max)), Strings.splitPreserveAllTokens(str, ',', max, true), context);
                // a single-char String delimiter routes to the char worker
                assertArrayEquals(trimAll(Strings.split(str, ",")), Strings.split(str, ",", true), context);
                // multi-char String delimiter worker
                assertArrayEquals(trimAll(Strings.split(str, "::")), Strings.split(str, "::", true), context);
                assertArrayEquals(trimAll(Strings.split(str, "::", max)), Strings.split(str, "::", max, true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, "::")), Strings.splitPreserveAllTokens(str, "::", true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, "::", max)), Strings.splitPreserveAllTokens(str, "::", max, true), context);
                assertArrayEquals(trimAll(Strings.split(str, ", ", max)), Strings.split(str, ", ", max, true), context);
                // whitespace worker (tokens can still carry U+0001, which trim() removes)
                assertArrayEquals(trimAll(Strings.splitOnWhitespace(str)), Strings.splitOnWhitespace(str, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespace(str, max)), Strings.splitOnWhitespace(str, max, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespacePreserveAllTokens(str)), Strings.splitOnWhitespacePreserveAllTokens(str, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespacePreserveAllTokens(str, max)), Strings.splitOnWhitespacePreserveAllTokens(str, max, true),
                        context);
            }
        }

        // G070-02: fixed edge cases of the trim option: empty-token omission before trimming, whitespace-only tokens, the max
        // remainder, chars <= ' ' that are not Java whitespace, null/empty input, and the whole-string token.
        @Test
        public void testSplitTrim_edgeCases() {
            final String c1 = String.valueOf((char) 0x0001);
            final String c0 = String.valueOf((char) 0x0000);
            final String c2 = String.valueOf((char) 0x0002);
            final String c1f = String.valueOf((char) 0x001F);
            final String nbsp = String.valueOf((char) 0x00A0);
            final String cjk = String.valueOf((char) 0x4E00);

            assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
            assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
            assertArrayEquals(new String[] { "a", "b : c" }, Strings.split(" a : b : c ", ':', 2, true));
            assertArrayEquals(new String[] { "a", "b :: c" }, Strings.split(" a :: b :: c ", "::", 2, true));
            assertArrayEquals(new String[] { "a :: b" }, Strings.split(" a :: b ", "::", 1, true));
            assertArrayEquals(new String[] { "", "a", "", "" }, Strings.splitPreserveAllTokens(" , a ,  , ", ',', true));
            assertArrayEquals(new String[] { "", "x", "" }, Strings.splitPreserveAllTokens("::x::", "::", true));
            assertArrayEquals(new String[] { "x", "y" }, Strings.split(c1 + "x" + c1 + "," + c0 + "y" + c1f, ',', true));
            assertArrayEquals(new String[] { nbsp + "x" + nbsp }, Strings.split(" " + nbsp + "x" + nbsp + " ", ',', true));
            assertArrayEquals(new String[] { "x" + c1 + "y" }, Strings.splitOnWhitespace(" " + c1 + "x" + c1 + "y" + c2 + " ", true));
            assertArrayEquals(new String[] { cjk, "b" }, Strings.split(" " + cjk + " , b", ',', true));
            assertArrayEquals(new String[0], Strings.split(null, ',', true));
            assertArrayEquals(new String[0], Strings.split("", ',', true));
            assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', true));
            assertArrayEquals(new String[] { "" }, Strings.split("   ", ',', true));

            // change detector, not a contract: a whole-string token with nothing to trim is the input instance itself
            // (substring(0, length) returns this), as before the change
            final String whole = "abc";
            assertSame(whole, Strings.split(whole, ',', true)[0]);
            assertSame(whole, Strings.split(whole, "::", true)[0]);
            assertSame(whole, Strings.splitOnWhitespace(whole, true)[0]);
        }

        // G070-01: containsAll(String, char...) uses String.indexOf per candidate below 16 candidates (was: a membership set of
        // the whole string from 8 candidates on). Differential check against the indexOf reference on both sides of the
        // candidate-count (8, 16) and length (n * k >= 4096) thresholds, Latin-1 and UTF-16 strings, surrogates, duplicates.
        @Test
        public void testContainsAllChars_matchesIndexOfReferenceAroundThresholds() {
            final Random random = new Random(8L);
            final char[] latin = "abcdefghijklmnopqrstuvwxyz0123456789,;: \t".toCharArray();
            final char[] wide = { 'a', 'b', 'c', 'x', 'y', 'z', (char) 0x00E9, (char) 0x4E00, (char) 0x4E01, (char) 0xD83D, (char) 0xDE00, (char) 0xDC00,
                    (char) 0xFFFF };
            final int[] lengths = { 0, 1, 7, 255, 256, 257, 273, 274, 511, 512, 513, 600, 2000 };

            for (int iteration = 0; iteration < 3000; iteration++) {
                final char[] alphabet = random.nextBoolean() ? latin : wide;
                final int length = lengths[random.nextInt(lengths.length)];
                final String str = randomString(random, alphabet, length);
                final int count = random.nextInt(41);
                final char[] candidates = new char[count];

                for (int i = 0; i < count; i++) {
                    final int pick = random.nextInt(10);

                    if (pick < 6 && length > 0) {
                        candidates[i] = str.charAt(random.nextInt(length)); // present
                    } else if (pick < 8) {
                        candidates[i] = alphabet[random.nextInt(alphabet.length)]; // maybe present
                    } else {
                        candidates[i] = (char) random.nextInt(0x10000); // most likely absent
                    }
                }

                final boolean expected = count == 0 || (length > 0 && containsAllReference(str, candidates));
                assertEquals(expected, Strings.containsAll(str, candidates), "length=" + length + " count=" + count);
            }

            assertTrue(Strings.containsAll(null, new char[0]));
            assertTrue(Strings.containsAll("abc", (char[]) null));
            assertFalse(Strings.containsAll(null, 'a'));
            assertFalse(Strings.containsAll("", 'a'));

            final String longStr = Strings.repeat("abcdefghijklmnop", 64); // 1024 chars, n * k >= 4096 for k >= 4
            assertTrue(Strings.containsAll(longStr, "abcdefgh".toCharArray()));
            assertTrue(Strings.containsAll(longStr, "abcdefghijklmno".toCharArray()));
            assertTrue(Strings.containsAll(longStr, "abcdefghijklmnop".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "abcdefgz".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "zabcdefghijklmn".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "abcdefghijklmnoz".toCharArray()));
        }
    }

    // ---- perf review 2026-10-08 G070 end ----
    // ---- perf review 2026-10-08 G071 begin ----
    @Nested
    class G071 {

        /** Scalar reference: the code-unit-by-code-unit definition of containsNone(String, char...). */
        private boolean referenceContainsNone(final String str, final char... valuesToFind) {
            if (str == null || str.isEmpty() || valuesToFind == null || valuesToFind.length == 0) {
                return true;
            }

            for (int i = 0; i < str.length(); i++) {
                for (final char c : valuesToFind) {
                    if (str.charAt(i) == c) {
                        return false;
                    }
                }
            }

            return true;
        }

        private String filler(final int length, final Random random, final char[] alphabet) {
            final char[] chars = new char[length];

            for (int i = 0; i < length; i++) {
                chars[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(chars);
        }

        private String withCharAt(final String str, final int index, final char ch) {
            final char[] chars = str.toCharArray();
            chars[index] = ch;
            return new String(chars);
        }

        private void assertMatchesReference(final String str, final char[] valuesToFind) {
            final boolean expected = referenceContainsNone(str, valuesToFind);
            assertEquals(expected, Strings.containsNone(str, valuesToFind),
                    () -> "containsNone length=" + str.length() + " values=" + Arrays.toString(valuesToFind));
            assertEquals(!expected, Strings.containsAny(str, valuesToFind),
                    () -> "containsAny length=" + str.length() + " values=" + Arrays.toString(valuesToFind));
        }

        // G071-01: windowed containsNone(String, char...) equals the code-unit reference for every length, hit position and candidate order.
        @Test
        public void testContainsNoneChar_windowedSearchMatchesReference() {
            final Random random = new Random(20261008L);
            final char[] latin = "abcdefghijklmnopqrstuvwxyz 0123456789".toCharArray();
            final char[] utf16 = { 'a', 'b', '\u4e2d', '\u6587', '\u00e9', '\uD83D', '\uDE00', ' ' };
            final char[] candidatePool = { ',', '"', '\r', '\n', 'Q', '\u4e8c', '\uD83D', '\uDE00', '\u0100', '\uffff', '\u0000' };
            final int[] lengths = new int[160];

            for (int i = 0; i < 141; i++) {
                lengths[i] = i;
            }

            final int[] longLengths = { 191, 192, 193, 255, 256, 257, 511, 1000, 4097, 70_000 };
            System.arraycopy(longLengths, 0, lengths, 141, longLengths.length);
            final int lengthCount = 141 + longLengths.length;

            for (int li = 0; li < lengthCount; li++) {
                final int length = lengths[li];

                for (int round = 0; round < 6; round++) {
                    final String base = filler(length, random, round % 2 == 0 ? latin : utf16);
                    final int valueCount = 1 + random.nextInt(7);
                    final char[] valuesToFind = new char[valueCount];

                    for (int k = 0; k < valueCount; k++) {
                        valuesToFind[k] = candidatePool[random.nextInt(candidatePool.length)];
                    }

                    // no planted hit (the filler may still contain a surrogate candidate)
                    assertMatchesReference(base, valuesToFind);

                    if (length == 0) {
                        continue;
                    }

                    final int[] positions = { 0, 1, 31, 32, 33, 63, 64, 65, 95, 96, 127, 128, 129, length - 1, random.nextInt(length) };

                    for (final int position : positions) {
                        if (position < length) {
                            // plant the first, a middle and the last candidate
                            assertMatchesReference(withCharAt(base, position, valuesToFind[0]), valuesToFind);
                            assertMatchesReference(withCharAt(base, position, valuesToFind[valueCount / 2]), valuesToFind);
                            assertMatchesReference(withCharAt(base, position, valuesToFind[valueCount - 1]), valuesToFind);
                        }
                    }
                }
            }
        }

        // G071-01: the window doubling stops at 65,536 chars; a 300,000-char string runs through capped windows.
        @Test
        public void testContainsNoneChar_cappedWindowsOnVeryLongString() {
            final String base = Strings.repeat("abcdefgh", 37_500);
            final char[] csv = { ',', '"', '\r', '\n' };

            assertEquals(300_000, base.length());
            assertTrue(Strings.containsNone(base, csv));
            assertFalse(Strings.containsAny(base, csv));

            for (final int position : new int[] { 0, 31, 32, 63, 64, 65_535, 65_536, 131_071, 131_072, 196_607, 196_608, 250_000, 299_999 }) {
                for (final char ch : csv) {
                    final String str = withCharAt(base, position, ch);
                    assertFalse(Strings.containsNone(str, csv), () -> "position " + position + " char " + (int) ch);
                    assertTrue(Strings.containsAny(str, csv), () -> "position " + position + " char " + (int) ch);
                }
            }
        }

        // G071-01: scalar-head boundary (64/65 chars), candidates above U+00FF, surrogate code units, duplicates, >= 8 candidates.
        @Test
        public void testContainsNoneChar_boundariesAndCodeUnits() {
            final String s64 = Strings.repeat('x', 64);
            final String s65 = Strings.repeat('x', 65);

            assertTrue(Strings.containsNone(s64, 'a', 'b'));
            assertTrue(Strings.containsNone(s65, 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s64, 63, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 64, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 32, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 31, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(s65, 'a', 'x'));
            assertFalse(Strings.containsNone(s65, 'x', 'x', 'x'));

            // Latin-1 string, candidates that cannot occur in it
            final String latin = Strings.repeat("caf\u00e9 ", 50);
            assertTrue(Strings.containsNone(latin, '\u0100', '\u4e2d', '\uD83D', '\uffff'));
            assertFalse(Strings.containsNone(latin, '\u0100', '\u00e9'));

            // UTF-16 string: lone high surrogate, then a full pair, far beyond the scalar head
            final String utf16Base = Strings.repeat("\u4e2d\u6587", 100);
            assertTrue(Strings.containsNone(utf16Base, '\uD83D', '\uDE00', ','));
            assertFalse(Strings.containsNone(utf16Base + "\uD83D", ',', '\uD83D'));
            assertFalse(Strings.containsNone(utf16Base + "\uD83D\uDE00", ',', '\uDE00'));
            assertFalse(Strings.containsNone(utf16Base, ',', '\u6587'));

            // >= 8 candidates with length * count < 4096 (scalar region) on strings longer than 64 chars
            final char[] ten = "0123456789".toCharArray();
            final String letters = Strings.repeat("abcdefghij", 40);
            assertTrue(Strings.containsNone(letters, ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 399, '9'), ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 40, '9'), ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 200, '0'), ten));

            // >= 8 candidates with length * count >= 4096 (membership path, unchanged)
            final String longLetters = Strings.repeat("abcdefghij", 60);
            assertTrue(Strings.containsNone(longLetters, ten));
            assertFalse(Strings.containsNone(withCharAt(longLetters, 599, '5'), ten));
        }
    }

    // ---- perf review 2026-10-08 G071 end ----
    // ---- perf review 2026-10-08 G072 begin ----
    @Nested
    class G072 {
        private final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(1, 1), new Strings.DisplayWidthPolicy(2, 1) };

        private java.lang.reflect.Method nextBoundaryMethod;
        private java.lang.reflect.Method clusterWidthMethod;

        private String text(final int... codePoints) {
            final StringBuilder sb = new StringBuilder();

            for (final int codePoint : codePoints) {
                sb.appendCodePoint(codePoint);
            }

            return sb.toString();
        }

        private String hex(final int codePoint) {
            return "U+" + Integer.toHexString(codePoint).toUpperCase();
        }

        private int nextBoundary(final String str, final int clusterStart) throws ReflectiveOperationException {
            if (nextBoundaryMethod == null) {
                nextBoundaryMethod = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
                nextBoundaryMethod.setAccessible(true);
            }

            return (Integer) nextBoundaryMethod.invoke(null, str, clusterStart);
        }

        private int clusterWidth(final String str, final int clusterStart, final int clusterEnd, final Strings.DisplayWidthPolicy policy)
                throws ReflectiveOperationException {
            if (clusterWidthMethod == null) {
                clusterWidthMethod = Strings.class.getDeclaredMethod("graphemeClusterDisplayWidth", String.class, int.class, int.class,
                        Strings.DisplayWidthPolicy.class);
                clusterWidthMethod.setAccessible(true);
            }

            return (Integer) clusterWidthMethod.invoke(null, str, clusterStart, clusterEnd, policy);
        }

        /**
         * Width from the general grapheme segmentation alone: every cluster boundary and cluster width comes from the
         * private helpers that displayWidth uses for clusters it cannot measure without the property tables.
         */
        private int fullySegmentedDisplayWidth(final String str, final Strings.DisplayWidthPolicy policy) throws ReflectiveOperationException {
            long width = 0;

            for (int clusterStart = 0; clusterStart < str.length();) {
                final int clusterEnd = nextBoundary(str, clusterStart);
                width += clusterWidth(str, clusterStart, clusterEnd, policy);
                clusterStart = clusterEnd;
            }

            return (int) width;
        }

        private List<Integer> standaloneClusterCodePoints() {
            final List<Integer> result = new ArrayList<>();

            for (int codePoint = 0; codePoint <= 0x7F; codePoint++) {
                if (codePoint != '\r') {
                    result.add(codePoint);
                }
            }

            final int[][] ranges = { { 0x3041, 0x3096 }, { 0x309B, 0x30FF }, { 0x3400, 0x9FFF }, { 0xAC00, 0xD7A3 }, { 0xFF01, 0xFF5E } };

            for (final int[] range : ranges) {
                for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                    result.add(codePoint);
                }
            }

            return result;
        }

        // G072-01: ASCII-but-CR and common wide characters are one-character clusters before each other or the end.
        @Test
        public void testDisplayWidth_standaloneClusterCharsNeverJoinTheirNeighbour() throws ReflectiveOperationException {
            final List<Integer> standalone = standaloneClusterCodePoints();
            final int[] wideFollowers = { 0x3041, 0x3096, 0x309B, 0x30FF, 0x3400, 0x4E2D, 0x9FFF, 0xAC00, 0xAC01, 0xD7A3, 0xFF01, 0xFF5E };

            for (final int codePoint : standalone) {
                final byte graphemeBreak = Unicode17Data.graphemeBreakProperty(codePoint);

                assertTrue(graphemeBreak == Unicode17Data.GCB_OTHER || graphemeBreak == Unicode17Data.GCB_CONTROL || graphemeBreak == Unicode17Data.GCB_LF
                        || graphemeBreak == Unicode17Data.GCB_LV || graphemeBreak == Unicode17Data.GCB_LVT, hex(codePoint));
                assertEquals(Unicode17Data.INCB_NONE, Unicode17Data.indicConjunctBreakProperty(codePoint), hex(codePoint));

                final String single = text(codePoint);
                assertEquals(1, nextBoundary(single, 0), hex(codePoint));

                for (final Strings.DisplayWidthPolicy policy : policies) {
                    assertEquals(Strings.codePointDisplayWidth(codePoint, policy), clusterWidth(single, 0, 1, policy), hex(codePoint) + " " + policy);
                }

                for (final int follower : wideFollowers) {
                    assertEquals(1, nextBoundary(text(codePoint, follower), 0), hex(codePoint) + " " + hex(follower));
                }

                if (codePoint <= 0x7F) {
                    for (int follower = 0; follower <= 0x7F; follower++) {
                        assertEquals(1, nextBoundary(text(codePoint, follower), 0), hex(codePoint) + " " + hex(follower));
                    }
                }
            }

            // CR is excluded on purpose: CR LF is one cluster (UAX #29 GB3).
            assertEquals(2, nextBoundary("\r\n", 0));
        }

        // G072-01: text after a segmentation trigger measures exactly what the general segmentation measures.
        @Test
        public void testDisplayWidth_textAfterSegmentationTriggerMatchesFullSegmentation() throws ReflectiveOperationException {
            final int[] triggers = { 0x1F600, 0x0301, 0x00A9, 0x2122, 0x094D, 0x1100, 0x0600, 0x1F1FA, 0x200D, 0xFE0F, 0xD800, 0x0E33 };
            final int[] standalone = { 'a', 'Z', '0', ' ', '.', '~', '\n', '\t', 0x01, 0x1F, 0x7F, 0x3041, 0x3096, 0x309B, 0x30FF, 0x3400, 0x4E2D, 0x9FFF,
                    0xAC00, 0xAC01, 0xD7A3, 0xFF01, 0xFF5E };
            final int[] others = { '\r', 0x0300, 0x0301, 0x200D, 0x200C, 0xFE0F, 0xFE0E, 0x20E3, 0x1F600, 0x1F469, 0x1F4BB, 0x1F3FB, 0x1100, 0x1161, 0x11A8,
                    0xD7B0, 0xD7CB, 0x0915, 0x094D, 0x0937, 0x093E, 0x0E01, 0x0E31, 0x0E33, 0x0600, 0x0D4E, 0x1F1FA, 0x1F1F8, 0xFF76, 0xFF9E, 0xFF9F, 0x3099,
                    0x3040, 0x3097, 0xD7A4, 0xFF5F, 0x00A1, 0x00E9, 0x00AD, 0x2028, 0x2764, 0x231A, 0x102B, 0x1B35, 0x0BD7, 0x302E, 0xD800, 0xDC00, 0x20000,
                    0xE0100, 0x11A84 };
            final Random random = new Random(20261008L);

            for (int iteration = 0; iteration < 3_000; iteration++) {
                final StringBuilder value = new StringBuilder();
                value.appendCodePoint(triggers[random.nextInt(triggers.length)]);
                final int length = random.nextInt(48);

                for (int i = 0; i < length; i++) {
                    final int[] alphabet = random.nextInt(10) < 7 ? standalone : others;
                    value.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
                }

                final String str = value.toString();

                for (final Strings.DisplayWidthPolicy policy : policies) {
                    assertEquals(fullySegmentedDisplayWidth(str, policy), Strings.displayWidth(str, policy), "iteration=" + iteration + ", policy=" + policy);
                }
            }
        }

        // G072-01: explicit widths after a trigger (computed on the code before the shortcut), incl. joiners and CR LF.
        @Test
        public void testDisplayWidth_textAfterSegmentationTrigger_examples() {
            final Strings.DisplayWidthPolicy narrowEmoji = new Strings.DisplayWidthPolicy(1, 1);

            final String[] values = { text(0xA9) + " 2026 Acme", text(0x1F600) + "abc" + text(0x4E2D, 0x6587), "e" + text(0x301) + "xyz",
                    text(0xA9) + "\r\nab\r\n", text(0x2122) + "a" + text(0x301) + "b", text(0x1F600) + "a" + text(0x200D) + text(0x1F4BB),
                    text(0x1F600) + text(0xAC00, 0x11A8), text(0x1F600) + text(0xFF76, 0xFF9E), text(0x1F600) + "1" + text(0xFE0F, 0x20E3),
                    text(0x0915, 0x094D) + "x", text(0x1F1FA) + "a" + text(0x1F1FA, 0x1F1F8), text(0x0600) + "a", text(0x1F600) + "ab" + text(0x0600) + "cd",
                    text(0x1F600) + text(0xA1) + "a", text(0x1F600) + "a" + text(0x93E) + "b", text(0x1F600) + text(0x3041, 0x3099) + "z",
                    text(0x1F600, '\t', 0x01, 'x'), text(0x301, 0xFF01, 0xFF5E, 0x3400, 0x9FFF, 0xD7A3, '\r') };
            final int[] expectedDefault = { 11, 9, 4, 3, 3, 5, 4, 4, 4, 2, 5, 1, 6, 4, 5, 5, 3, 10 };
            final int[] expectedCjk = { 11, 9, 4, 3, 4, 5, 4, 4, 4, 2, 5, 1, 6, 5, 5, 5, 3, 10 };
            final int[] expectedNarrowEmoji = { 11, 8, 4, 3, 3, 3, 3, 3, 2, 2, 3, 1, 5, 3, 4, 4, 2, 10 };

            for (int i = 0; i < values.length; i++) {
                assertEquals(expectedDefault[i], Strings.displayWidth(values[i]), "case " + i);
                assertEquals(expectedCjk[i], Strings.displayWidth(values[i], Strings.DisplayWidthPolicy.CJK), "case " + i);
                assertEquals(expectedNarrowEmoji[i], Strings.displayWidth(values[i], narrowEmoji), "case " + i);

                final int target = expectedDefault[i] + 3;
                assertEquals(values[i] + "   ", Strings.padEndToDisplayWidth(values[i], target), "case " + i);
                assertTrue(Strings.displayWidth(Strings.padStartToDisplayWidth(values[i], target)) >= target, "case " + i);
            }

            // A long ASCII run after the trigger, and the same run before it.
            final String run = "abcdefghij".repeat(100);
            assertEquals(1002, Strings.displayWidth(text(0x1F600) + run));
            assertEquals(1002, Strings.displayWidth(run + text(0x1F600)));
            assertEquals(1001, Strings.displayWidth(text(0x1F600) + run, narrowEmoji));
        }
    }
}
