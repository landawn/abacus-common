package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Behavior contracts for the October 2026 performance and allocation changes. */
public class StringsOptimizationRegressionTest {

    @Test
    void tokenSearchPreservesLocalBoundariesAndBounds() {
        final Random random = new Random(20261019);

        for (int test = 0; test < 500; test++) {
            final String delimiter = test % 3 == 0 ? "a".repeat(16 + random.nextInt(64)) + "b" : randomText(random, 1 + random.nextInt(24));
            final String token = test % 5 == 0 ? "" : test % 3 == 0 ? "a".repeat(1 + random.nextInt(24)) : randomText(random, random.nextInt(24));
            final String text = test % 3 == 0 ? "a".repeat(2048) + delimiter + token + delimiter + "a".repeat(64)
                    : randomText(random, 256) + delimiter + token + delimiter + randomText(random, 64);
            final int bound = random.nextInt(text.length() + 20) - 10;

            assertEquals(referenceToken(text, token, delimiter, bound, false), Strings.StrUtil.indexOfTokenIgnoreCase(text, token, delimiter, bound));
            assertEquals(referenceToken(text, token, delimiter, bound, true), Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, delimiter, bound));
        }

        assertEquals(3, Strings.StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        assertEquals(3, Strings.StrUtil.lastIndexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        final String text = "a".repeat(16384);
        final String delimiter = "a".repeat(4096) + "b";
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase(text, "a", delimiter));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, "a".repeat(16), delimiter));

        // Supplementary case pairs must still use code-point-aware region matching.
        final String upper = "\uD801\uDC00";
        final String lower = "\uD801\uDC28";
        assertEquals(upper.length(), Strings.StrUtil.indexOfTokenIgnoreCase(upper + "x" + upper, "X", lower));
        assertEquals(1, Strings.StrUtil.lastIndexOfTokenIgnoreCase("|" + upper.repeat(16) + "|", lower.repeat(16), "|"));
    }

    @Test
    void unicodeTokenSearchPreservesSupplementaryOffsetsOverlapsAndMalformedCompatibility() {
        final String upper = "\uD801\uDC00";
        final String lower = "\uD801\uDC28";
        final String[] tokens = { "", "a", lower.repeat(16), lower + "a".repeat(31), "\uD801", "\uDC00", "\uD801\uD801\uDC00" + "a".repeat(32) };
        final String[] delimiters = { "|", upper, "a".repeat(32) + upper, upper + "a".repeat(32), "\uDC00", "\uD801" };

        for (final String token : tokens) {
            for (final String delimiter : delimiters) {
                final String text = "x".repeat(256) + delimiter + token.toUpperCase(Locale.ROOT) + delimiter + delimiter + token + delimiter;
                for (final int bound : new int[] { Integer.MIN_VALUE, -1, 0, 1, 255, 256, 257, text.length() - token.length(), text.length(),
                        Integer.MAX_VALUE }) {
                    assertEquals(referenceToken(text, token, delimiter, bound, false), Strings.StrUtil.indexOfTokenIgnoreCase(text, token, delimiter, bound));
                    assertEquals(referenceToken(text, token, delimiter, bound, true),
                            Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, delimiter, bound));
                }
            }
        }

        // Preserve the JDK's comparison even when malformed sequences do not align as Unicode code points.
        final String paired = upper + "a".repeat(33);
        final String malformed = "\uD801" + upper + "a".repeat(32);
        for (final String[] pair : new String[][] { { paired, malformed }, { malformed, paired } }) {
            final String text = "z".repeat(512) + "|" + pair[0] + "|";
            assertEquals(513, Strings.StrUtil.indexOfTokenIgnoreCase(text, pair[1], "|"));
            assertEquals(513, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, pair[1], "|"));
        }

        // Dense rejected candidates, followed by a real match after switching to the streaming cursors.
        final String delimiter = "a".repeat(1024) + upper;
        final String text = "a".repeat(8192) + delimiter + "A" + delimiter;
        assertEquals(8192 + delimiter.length(), Strings.StrUtil.indexOfTokenIgnoreCase(text, "a", delimiter));
        assertEquals(8192 + delimiter.length(), Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, "a", delimiter));
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase(upper.repeat(4096), lower.repeat(32) + "x", "|"));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase(upper.repeat(4096), "x" + lower.repeat(32), "|"));

        final String boundary = upper + ":";
        final String field = upper.repeat(16);
        final String needle = lower.repeat(16);
        final String beforeWindow = "\uD801" + "z".repeat(4096) + boundary + field + boundary + "z".repeat(512);
        assertEquals(4097 + boundary.length(), Strings.StrUtil.indexOfTokenIgnoreCase(beforeWindow, needle, boundary, 4098));
        final String afterWindow = boundary + field + boundary + "z".repeat(4096) + "\uD801";
        assertEquals(boundary.length(), Strings.StrUtil.lastIndexOfTokenIgnoreCase(afterWindow, needle, boundary, 4));

        final String longBoundary = "x".repeat(32) + upper;
        assertEquals(longBoundary.length(), Strings.StrUtil.indexOfTokenIgnoreCase(longBoundary + field, needle, longBoundary));
        assertEquals(0, Strings.StrUtil.lastIndexOfTokenIgnoreCase(field + longBoundary, needle, longBoundary, 10));
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase(field + longBoundary, needle, longBoundary, 1));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase(longBoundary + field, needle, longBoundary, longBoundary.length() - 1));

        // With no pairs inside either region, surrogate code units stay literal even if a pair forms across the seam.
        final String unpairedDelimiter = "b".repeat(16) + "\uD801";
        final String unpairedToken = "\uDC00" + "a".repeat(16);
        final String acrossPair = "x".repeat(256) + unpairedDelimiter + unpairedToken + unpairedDelimiter;
        assertEquals(273, Strings.StrUtil.indexOfTokenIgnoreCase(acrossPair, unpairedToken, unpairedDelimiter));
        assertEquals(273, Strings.StrUtil.lastIndexOfTokenIgnoreCase(acrossPair, unpairedToken, unpairedDelimiter));
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase(acrossPair, "\uDC28" + "a".repeat(16), unpairedDelimiter));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase(acrossPair, "\uDC28" + "a".repeat(16), unpairedDelimiter));
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase("a".repeat(8192), "a", "a".repeat(512) + "\uD801"));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase("a".repeat(8192), "a", "a".repeat(512) + "\uD801"));
        assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase("a".repeat(8192), "a".repeat(16), "a".repeat(512) + "\uD801\uD801\uDC00"));
        assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase("a".repeat(8192), "a".repeat(16), "a".repeat(512) + "\uD801\uD801\uDC00"));
    }

    @Test
    void unicodeTokenSearchRestartsBeforeMalformedTokensAndDelimiterBoundaries() {
        final String upper = "\uD801\uDC00";
        final String lower = "\uD801\uDC28";
        final String paired = upper + "a".repeat(33);
        final String malformed = "\uD801" + upper + "a".repeat(32);
        final String padding = "z".repeat(512);
        final String asciiToken = "b".repeat(16);

        // Each malformed region compares equal under the JDK's regionMatches despite its different alignment.
        // The search must include candidates that start before the unpaired surrogate detected by a cursor.
        for (final String[] fixture : new String[][] { { padding + "|" + malformed + "|" + padding + "|" + paired + "|", paired, "|" },
                { padding + "|" + paired + "|" + padding + "|" + malformed + "|", paired, "|" },
                { padding + malformed + asciiToken + paired + padding, asciiToken, paired },
                { padding + paired + asciiToken + malformed + padding, asciiToken, paired }, { padding + malformed + malformed + padding, "", paired },
                { upper + "|" + lower.repeat(16) + "|" + padding, upper.repeat(16), "|" } }) {
            final String text = fixture[0];
            for (final int bound : new int[] { 0, 1, 2, 511, 512, 513, text.length() / 2, text.length() - 1, text.length() }) {
                assertEquals(referenceToken(text, fixture[1], fixture[2], bound, false),
                        Strings.StrUtil.indexOfTokenIgnoreCase(text, fixture[1], fixture[2], bound));
                assertEquals(referenceToken(text, fixture[1], fixture[2], bound, true),
                        Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, fixture[1], fixture[2], bound));
            }
        }
    }

    @Test
    void unicodeTokenSearchFindsNearbyMatchesWithMalformedTextFartherAway() {
        final String upper = "\uD801\uDC00";
        final String lower = "\uD801\uDC28";
        final String field = upper.repeat(16);
        final String needle = lower.repeat(16);
        final String padding = "z".repeat(16384);
        final String forward = "x|" + field + "|" + padding + "\uD801";
        final String reverse = "\uDC00" + padding + "|" + field + "|x";
        assertEquals(2, Strings.StrUtil.indexOfTokenIgnoreCase(forward, needle, "|"));
        assertEquals(reverse.length() - field.length() - 2, Strings.StrUtil.lastIndexOfTokenIgnoreCase(reverse, needle, "|"));

        final String delimiter = upper + ":";
        final String bmpToken = "abcdefghijklmnop";
        assertEquals(1 + delimiter.length(),
                Strings.StrUtil.indexOfTokenIgnoreCase("x" + delimiter + bmpToken + delimiter + padding + "\uD801", bmpToken, delimiter));
        final String reverseDelimiter = "\uDC00" + padding + delimiter + bmpToken + delimiter + "x";
        assertEquals(reverseDelimiter.length() - bmpToken.length() - delimiter.length() - 1,
                Strings.StrUtil.lastIndexOfTokenIgnoreCase(reverseDelimiter, bmpToken, delimiter));
    }

    @Test
    void unicodeTokenSearchPreservesPairsAcrossValidationWindows() {
        final String upper = "\uD801\uDC00";
        final String lower = "\uD801\uDC28";
        final String token = lower.repeat(16);
        for (final int offset : new int[] { 63, 64, 191, 192, 447, 448, 8191, 8192 }) {
            final String padding = "z".repeat(offset - 1);
            final String text = padding + "|" + upper.repeat(16) + "|" + padding;
            assertEquals(offset, Strings.StrUtil.indexOfTokenIgnoreCase(text, token, "|"));
            assertEquals(offset, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, "|"));
            assertEquals(-1, Strings.StrUtil.indexOfTokenIgnoreCase(text, token, "|", offset + 1));
            assertEquals(-1, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, "|", offset - 1));

            final String malformed = padding + "|\uD801" + upper + "a".repeat(32) + "|" + padding;
            final String compatible = upper + "a".repeat(33);
            assertEquals(offset, Strings.StrUtil.indexOfTokenIgnoreCase(malformed, compatible, "|"));
            assertEquals(offset, Strings.StrUtil.lastIndexOfTokenIgnoreCase(malformed, compatible, "|"));
        }
    }

    @Test
    void characterMembershipKeepsArgumentPriorityAndDuplicateSemantics() {
        final String text = "b" + "x".repeat(4096) + "a\uD800\uDC00";
        final char[] candidates = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' };
        assertEquals(4097, Strings.indexOfAny(text, candidates));
        assertEquals(4097, Strings.lastIndexOfAny(text, candidates));
        assertEquals(0, Strings.lastIndexOfAny(text, 4096, candidates));
        assertEquals(-1, Strings.indexOfAny(text, Integer.MAX_VALUE, candidates));
        assertEquals(-1, Strings.lastIndexOfAny(text, Integer.MIN_VALUE, candidates));

        final char[] duplicates = new char[32];
        Arrays.fill(duplicates, 'b');
        duplicates[3] = '\uD800';
        duplicates[7] = '\uDC00';
        assertTrue(Strings.containsAll(text, duplicates));
        duplicates[7] = '\uFFFF';
        assertFalse(Strings.containsAll(text, duplicates));
    }

    @Test
    void extractionHonorsWindowForEveryModeAndReturnsMutableLists() {
        final String text = "prefix [[a][b]] [c] [" + "z".repeat(4096) + "]";

        for (final Strings.DelimiterMatchMode mode : Strings.DelimiterMatchMode.values()) {
            for (int end = 7; end <= 20; end++) {
                for (int max = 0; max <= 4; max++) {
                    final List<IndexRange> ranges = Strings.substringIndicesBetween(text, 7, end, "[", "]", mode, max);
                    assertEquals(Strings.substringIndicesBetween(text.substring(0, end), 7, end, "[", "]", mode, max), ranges);
                    final List<String> expected = new ArrayList<>();
                    for (final IndexRange range : ranges) {
                        expected.add(text.substring(range.start(), range.end()));
                    }
                    assertEquals(expected, Strings.substringsBetween(text, 7, end, "[", "]", mode, max));
                }
            }
        }

        final List<String> empty = Strings.substringsBetween(null, 0, 0, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, 1);
        empty.add("mutable");
        assertEquals(List.of("mutable"), empty);
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween(null, 0, 1, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("", 0, 0, "[", "]", null, 0));
        assertNull(Strings.substringAfter(text, "zzz", 20));
        assertEquals("prefix ", Strings.substringBetween(text, "", 7));
    }

    @Test
    void splitLimitsKeepRawEmptyAndTrimCreatedEmptyTokensDistinct() {
        assertArrayEquals(new String[] { "a,,b," }, Strings.split(",,a,,b,", ',', 1));
        assertArrayEquals(new String[] { "a", "b,,c," }, Strings.split(",a,,b,,c,", ',', 2));
        assertArrayEquals(new String[] { "", "a,,b," }, Strings.splitPreserveAllTokens(",a,,b,", ',', 2));
        assertArrayEquals(new String[] { "a::b::" }, Strings.split("::::a::b::", "::", 1));
        assertArrayEquals(new String[] { "a", "b::::c::" }, Strings.split("::a::::b::::c::", "::", 2));
        assertArrayEquals(new String[] { "" }, Strings.split("::  ::", "::", 2, true));
        assertArrayEquals(new String[] { "a", "b \t c " }, Strings.splitOnWhitespace(" a \t b \t c ", 2));
        assertArrayEquals(new String[] { "", "a \t b " }, Strings.splitOnWhitespacePreserveAllTokens(" a \t b ", 2));
        assertArrayEquals(new String[] { "a\uD83D\uDE00b" }, Strings.split("\uD83D\uDE00a\uD83D\uDE00b", "\uD83D\uDE00", 1));
        assertNotSame(Strings.split("a", ',', 1), Strings.split("a", ',', 1));
    }

    @Test
    void longestCommonSubstringReusesEqualInputsAndKeepsSuffixArrayResults() {
        final String text = "ab\uD83D\uDE00".repeat(2048);
        assertSame(text, Strings.longestCommonSubstring(text, text));
        assertSame(text, Strings.longestCommonSubstring(text, new String(text)));
        assertEquals(text, Strings.longestCommonSubstring(new StringBuilder(text), new StringBuilder(text)));
        assertEquals(text, Strings.longestCommonSubstring("x" + text, "y" + text));
        assertEquals("a".repeat(4096), Strings.longestCommonSubstring("a".repeat(4096) + "x", "a".repeat(4096) + "y"));
        assertNull(Strings.longestCommonSubstring(null, text));
    }

    @Test
    void concatenationHandlesExactLargeLengthNullsAndUtf16() {
        final String[] parts = new String[12];
        Arrays.fill(parts, "x".repeat(131072));
        parts[1] = null;
        parts[3] = "\uD800";
        parts[4] = "\uDC00";
        final StringBuilder expected = new StringBuilder();
        for (final String part : parts) {
            if (part != null) {
                expected.append(part);
            }
        }
        assertEquals(expected.toString(), Strings.concatNullToEmpty(parts));
        assertEquals("", Strings.concatNullToEmpty(new String[12]));
    }

    @Test
    void shrinkingOperationsPreservePrefixesLimitsAndWhitespaceDefinitions() {
        assertEquals("", Strings.removeAll("a".repeat(8192), 'a'));
        assertEquals("aa", Strings.removeAll("a".repeat(8192), 2, 'a'));
        assertEquals("bc", Strings.removeAll("aaaabaca", 'a'));
        assertEquals("aa", Strings.replace("aaaaaa", 2, "aa", "", -1));
        assertEquals("aaaa", Strings.replace("aaaaaa", 0, "aa", "", 1));
        assertEquals("b", Strings.replaceIgnoreCase("aAaAbAA", 0, "aa", null, -1));
        assertEquals("", Strings.removeWhitespace(" \t\n\u2003".repeat(2048)));
        assertEquals("\u00A0x\u00A0", Strings.removeWhitespace(" \t\u00A0x\u00A0\u2003"));
        assertEquals("prefixab\u00A0\uD801\uDC00", Strings.removeWhitespace("prefix \ta\u2003b\u00A0\uD801\uDC00" + " \t\u2003".repeat(2048)));
        assertEquals("prefixab", Strings.removeWhitespace("prefix \ta\u2003b"));
        assertEquals("prefix", Strings.removeWhitespace("prefix" + " \t\u2003".repeat(2048)));
        assertEquals("x y", Strings.normalizeSpace("\u00A0\u2003x\u00A0\u00A0y \t"));
        assertEquals("", Strings.normalizeSpace("\u00A0\u2003\t".repeat(2048)));
        assertEquals("\uD800 x \uDC00", Strings.normalizeSpace(" \uD800\t x \uDC00 "));
    }

    @Test
    void unchangedTransformsReuseStringsWithoutChangingIgnoreCaseReplacement() {
        final String text = new String("Hello World");
        assertSame(text, Strings.capitalizeWords(text));
        assertSame(text, Strings.capitalizeWordsFully(text));
        assertSame(text, Strings.capitalizeWords(text, " "));
        assertSame(text, Strings.capitalizeWordsFully(text, " "));
        assertSame(text, Strings.replaceAll(text, "World", "World"));
        assertSame(text, Strings.lenientFormat(text));
        final String lower = new String("helloWorld");
        assertSame(lower, Strings.uncapitalize(lower));
        assertEquals("aa", Strings.replaceAllIgnoreCase("AA", "a", "a"));
        assertEquals("i\u0307stanbul", Strings.uncapitalize("\u0130stanbul"));
        assertEquals("(Object[])null", Strings.lenientFormat("%s", (Object[]) null));
    }

    @Test
    void strictUtf8AcceptsPairedSurrogatesAndStillReportsMalformedInput() {
        for (final String text : new String[] { "", "?", "?\uD83D\uDE00?", "\uD800\uDC00?\uDBFF\uDFFF", "\uFFFD?", "hello? \uD83D\uDE00".repeat(1024) }) {
            assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8Strict(text));
        }
        for (final String text : new String[] { "\uD800", "?\uDC00", "\uD800?", "\uD800\uD800\uDC00", "\uD800\uDC00\uDC00?" }) {
            final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Strings.getBytesUtf8Strict(text));
            assertTrue(error.getCause() instanceof CharacterCodingException);
        }
    }

    @Test
    void joinsPreserveConversionCountAndRawSurrogates() {
        final String text = new String("single");
        assertSame(text, Strings.join(List.of(text), ","));
        assertSame(text, Strings.join(List.of(text).iterator(), ","));
        final int[] conversions = { 0 };
        final Object value = new Object() {
            @Override
            public String toString() {
                conversions[0]++;
                return " value ";
            }
        };
        final Iterator<Object> iterator = Arrays.asList(value, value).iterator();
        assertEquals(" value , value ", Strings.join(iterator, ","));
        assertEquals(2, conversions[0]);
        final Object nullString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        assertEquals("null", Strings.join(List.of(nullString), ","));
        assertEquals("null", Strings.join(List.of(nullString).iterator(), ","));
        assertEquals("[null]", Strings.join(List.of(nullString), ",", "[", "]"));
        assertThrows(NullPointerException.class, () -> Strings.join(List.of(nullString), ",", "", "", true));
        final char[] chars = { 'x', '\uD800', '\uDC00', '\uD800', 'y' };
        assertEquals(new String(chars, 1, 3), Strings.join(chars, 1, 4, null, null, ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(chars, 0, 6, ""));
    }

    @Test
    void commonAffixesMatchPairwiseContractsForCustomSequencesAndSurrogates() {
        final Random random = new Random(18891);
        for (int test = 0; test < 1000; test++) {
            final String shared = randomText(random, random.nextInt(12));
            final CharSequence[] prefixes = new CharSequence[3 + random.nextInt(6)];
            final CharSequence[] suffixes = new CharSequence[prefixes.length];
            for (int i = 0; i < prefixes.length; i++) {
                prefixes[i] = new StringBuilder(shared + randomText(random, random.nextInt(8)));
                suffixes[i] = new StringBuilder(randomText(random, random.nextInt(8)) + shared);
            }
            String prefix = prefixes[0].toString();
            String suffix = suffixes[0].toString();
            for (int i = 1; i < prefixes.length; i++) {
                prefix = Strings.commonPrefix(prefix, prefixes[i]);
                suffix = Strings.commonSuffix(suffix, suffixes[i]);
            }
            assertEquals(prefix, Strings.commonPrefix(prefixes));
            assertEquals(suffix, Strings.commonSuffix(suffixes));
        }
        assertNull(Strings.commonPrefix(new CharSequence[] { "", null }));
        assertNull(Strings.commonSuffix(new CharSequence[] { "", null }));
        assertEquals("", Strings.commonPrefix(new CharSequence[] { "\uD800x", "\uD800", "\uD800\uDC00" }));
        assertEquals("", Strings.commonSuffix(new CharSequence[] { "x\uDC00", "\uDC00", "\uD800\uDC00" }));
    }

    @Test
    void unicodeCaseCacheKeepsExpansionsAndContextualSigma() {
        final String input = "\u039F\u03A3|\u039F\u03A3\u0391|\u00DF|\u0130|\uFB03|\u00E9|\uD801\uDC28|";
        final String expected = "\u03BF\u03C2|\u03BF\u03C3\u03B1|SS|i\u0307|FFI|\u00C9|\uD801\uDC00|";
        assertEquals(expected.repeat(128), Strings.swapCase(input.repeat(128)));

        final StringBuilder varied = new StringBuilder();
        final StringBuilder mapped = new StringBuilder();
        for (int cp = 128; cp <= Character.MAX_CODE_POINT; cp++) {
            if (Character.isUpperCase(cp) || Character.isTitleCase(cp) || Character.isLowerCase(cp)) {
                final String single = new String(Character.toChars(cp));
                varied.append(single).append('|');
                mapped.append(Character.isLowerCase(cp) ? single.toUpperCase(Locale.ROOT) : single.toLowerCase(Locale.ROOT)).append('|');
            }
        }
        assertEquals(mapped.toString(), Strings.swapCase(varied.toString()));

        // Exercise two/three/four distinct mappings, expansions, and reuse across ASCII text.
        for (final String[] pair : new String[][] { { "\u00E9 abc \u00E8 ", "\u00C9 ABC \u00C8 " },
                { "\u00DF abc \u0130 \uFB03 \u00DF \uFB03 \u0130 ", "SS ABC i\u0307 FFI SS FFI i\u0307 " },
                { "\u00E9 \u00E8 \u00EA \u00EB \u00E9 \u00EB ", "\u00C9 \u00C8 \u00CA \u00CB \u00C9 \u00CB " } }) {
            assertEquals(pair[1].repeat(128), Strings.swapCase(pair[0].repeat(128)));
        }
    }

    @Test
    void trimmedLinesMatchRegexLineBoundariesAndStringTrim() {
        final String input = " \talpha \r\n\r \nbeta\u000B \u000Cgamma\u0085\u00A0delta\u2028\u2029 \t";
        final String[] trimmed = Arrays.stream(input.split("\\R", -1)).map(String::trim).toArray(String[]::new);
        assertArrayEquals(trimmed, Strings.splitToLines(input, true, false));
        assertArrayEquals(Arrays.stream(trimmed).filter(s -> !s.isEmpty()).toArray(String[]::new), Strings.splitToLines(input, true, true));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines("", true, false));
        assertArrayEquals(new String[0], Strings.splitToLines(null, true, false));
    }

    @Test
    void centeringKeepsOddPaddingAndLoneSurrogateChars() {
        for (final char padding : new char[] { ' ', '\u00E9', '\u4E2D', '\uD800', '\uDC00' }) {
            for (int width = 1; width < 24; width++) {
                final int left = (width - 1) / 2;
                assertEquals(String.valueOf(padding).repeat(left) + "x" + String.valueOf(padding).repeat(width - left - 1),
                        Strings.center("x", width, padding));
            }
        }
        assertEquals("xxx", Strings.center(null, 3, 'x'));
    }

    private static String randomText(final Random random, final int length) {
        final String alphabet = "aAbBkK\u212A\u0130\u0131\u017F[]| \uD800\uDC00";
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return result.toString();
    }

    private static int referenceToken(final String text, final String token, final String delimiter, final int bound, final boolean reverse) {
        for (int i = 0; i <= token.length() - delimiter.length(); i++) {
            if (token.regionMatches(true, i, delimiter, 0, delimiter.length())) {
                return -1;
            }
        }
        final int from = reverse ? Math.min(bound, text.length() - token.length())
                : token.isEmpty() ? Math.min(Math.max(bound, 0), text.length()) : Math.max(bound, 0);
        final int step = reverse ? -1 : 1;
        for (int i = from; i >= 0 && i <= text.length() - token.length(); i += step) {
            final int end = i + token.length();
            if (text.regionMatches(true, i, token, 0, token.length())
                    && (i == 0 || i >= delimiter.length() && text.regionMatches(true, i - delimiter.length(), delimiter, 0, delimiter.length()))
                    && (end == text.length() || text.regionMatches(true, end, delimiter, 0, delimiter.length()))) {
                return i;
            }
        }
        return -1;
    }
}
