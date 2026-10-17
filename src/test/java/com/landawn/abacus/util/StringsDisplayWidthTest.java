package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.substring;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.DisplayWidthPolicy;

/** Strings displaywidth contracts, including regression cases. */
public class StringsDisplayWidthTest extends StringsTestSupport {

    private static int codePoint0(final String str) {
        return str.codePointAt(0);
    }

    protected void assertCodePointRangesAreSortedAndDisjoint(final String fieldName) throws ReflectiveOperationException {
        final Field field = Unicode17Data.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final int[] ranges = (int[]) field.get(null);

        assertEquals(0, ranges.length % 2);
        int previousEnd = -1;

        for (int i = 0; i < ranges.length; i += 2) {
            final int start = ranges[i];
            final int end = ranges[i + 1];
            assertTrue(Character.isValidCodePoint(start));
            assertTrue(Character.isValidCodePoint(end));
            assertTrue(start <= end);
            assertTrue(start > previousEnd);
            previousEnd = end;
        }
    }

    protected void assertPropertyRangesAreSortedAndDisjoint(final String fieldName) throws ReflectiveOperationException {
        final Field field = Unicode17Data.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final int[] ranges = (int[]) field.get(null);

        assertEquals(0, ranges.length % 3);
        int previousEnd = -1;

        for (int i = 0; i < ranges.length; i += 3) {
            final int start = ranges[i];
            final int end = ranges[i + 1];
            assertTrue(Character.isValidCodePoint(start));
            assertTrue(Character.isValidCodePoint(end));
            assertTrue(start <= end);
            assertTrue(start > previousEnd);
            assertTrue(ranges[i + 2] > 0);
            previousEnd = end;
        }
    }

    protected static int fullySegmentedDisplayWidth(final String str, final Strings.DisplayWidthPolicy policy, final Method nextBoundary,
            final Method clusterWidth) throws ReflectiveOperationException {
        long result = 0;

        for (int clusterStart = 0; clusterStart < str.length();) {
            final int clusterEnd = (Integer) nextBoundary.invoke(null, str, clusterStart);
            result += (Integer) clusterWidth.invoke(null, str, clusterStart, clusterEnd, policy);

            if (result >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }

            clusterStart = clusterEnd;
        }

        return (int) result;
    }

    @Test
    public void testDisplayWidth_FastPathClassification() throws ReflectiveOperationException {
        final Method method = Strings.class.getDeclaredMethod("needsGraphemeClusterSegmentation", int.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(null, (int) 'A'));
        assertFalse((Boolean) method.invoke(null, 0x4E2D)); // Han ideograph
        assertFalse((Boolean) method.invoke(null, 0x20000)); // supplementary Han ideograph
        assertFalse((Boolean) method.invoke(null, 0xAC00)); // first precomposed Hangul syllable
        assertFalse((Boolean) method.invoke(null, 0xD7A3)); // last precomposed Hangul syllable
        assertFalse((Boolean) method.invoke(null, 0xFF01)); // first fullwidth ASCII form
        assertFalse((Boolean) method.invoke(null, 0xFF5E)); // last fullwidth ASCII form

        assertTrue((Boolean) method.invoke(null, 0xE0100)); // ideographic variation selector
        assertTrue((Boolean) method.invoke(null, 0x1100)); // decomposed Hangul leading jamo
        assertTrue((Boolean) method.invoke(null, 0x1161)); // decomposed Hangul medial jamo
        assertTrue((Boolean) method.invoke(null, 0x1F600)); // emoji
        assertTrue((Boolean) method.invoke(null, 0x1F1FA)); // regional indicator
        assertTrue((Boolean) method.invoke(null, 0x094D)); // Devanagari virama
    }

    // G075-01: the East Asian display-width shortcut must agree with the Unicode 17 property tables it bypasses.
    @Test
    public void testCodePointDisplayWidth_commonWideRangesMatchPropertyTables() {
        final int[][] ranges = { { 0x3041, 0x3096 }, { 0x309B, 0x30FF }, { 0x3400, 0x9FFF }, { 0xAC00, 0xD7A3 }, { 0xFF01, 0xFF5E } };
        final Strings.DisplayWidthPolicy narrow = new Strings.DisplayWidthPolicy(1, 1);

        for (final int[] range : ranges) {
            for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                final String hex = Integer.toHexString(codePoint);
                assertTrue(Unicode17Data.isEastAsianWideOrFullwidth(codePoint), hex);
                assertFalse(Unicode17Data.isEmojiPresentation(codePoint), hex);
                assertFalse(Unicode17Data.isZeroWidthCategory(codePoint), hex);

                if (codePoint < 0xAC00 || codePoint > 0xD7A3) {
                    assertEquals(Unicode17Data.GCB_OTHER, Unicode17Data.graphemeBreakProperty(codePoint), hex);
                    assertEquals(Unicode17Data.INCB_NONE, Unicode17Data.indicConjunctBreakProperty(codePoint), hex);
                    assertFalse(Unicode17Data.isExtendedPictographic(codePoint), hex);
                }

                assertEquals(2, Strings.codePointDisplayWidth(codePoint), hex);
                assertEquals(2, Strings.codePointDisplayWidth(codePoint, Strings.DisplayWidthPolicy.CJK), hex);
                assertEquals(2, Strings.codePointDisplayWidth(codePoint, narrow), hex);
                assertEquals(2, Strings.displayWidth(new String(Character.toChars(codePoint))), hex);
            }
        }
    }

    // displayWidth

    @Test
    public void testC568_canonicallyEquivalentSpellingsMeasureTheSame() {
        final String[] prefixes = { "", "k", "\u0915", "\u0B95" };
        int checked = 0;

        for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (Character.getType(codePoint) == Character.SURROGATE || !Character.isDefined(codePoint)) {
                continue;
            }

            final String str = cp(codePoint);
            final String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);

            if (nfd.equals(str)) {
                continue;
            }

            final String nfc = Normalizer.normalize(str, Normalizer.Form.NFC);

            // DEFAULT policy only: under CJK an East Asian Ambiguous precomposed letter (U+00E0 = 2) already differs from
            // its narrow base + zero-width mark decomposition (1), independently of the C-568 cluster rule.
            for (final String prefix : prefixes) {
                assertEquals(Strings.displayWidth(prefix + nfc), Strings.displayWidth(prefix + nfd),
                        () -> String.format("U+%04X with prefix %s", codePoint0(nfc), escape(prefix)));
            }

            checked++;
        }

        assertTrue(checked > 2000, "decomposable code points checked: " + checked);

        // random NFC / NFD pairs over Indic, Thai, kana and Hangul material
        final String[] pool = { "\u0915", "\u093E", "\u094D", "\u0937", "\u0995", "\u09CB", "\u09CC", "\u0B92", "\u0B94", "\u0BCA", "\u0BCC", "\u0C95",
                "\u0CC7", "\u0CCB", "\u0D15", "\u0D4A", "\u0D4C", "\u0DDA", "\u0DDD", "\u0E01", "\u0E33", "\u1B05", "\u1B06", "\u1B3B", "\uD55C", "\u304B",
                "\u3099", "\u304C", "\uFF76", "\uFF9E", "a", "\u00E9", "\u0301", cp(0x1138E), cp(0x113C5), cp(0x11347), cp(0x1134B), "\u1B3A", "\u1B35",
                "\u1B3B", "\u1B11", "\u0BD7", "k" };
        final Random random = new Random(568L);

        for (int iteration = 0; iteration < 50_000; iteration++) {
            final StringBuilder sb = new StringBuilder();
            final int count = 1 + random.nextInt(5);

            for (int k = 0; k < count; k++) {
                sb.append(pool[random.nextInt(pool.length)]);
            }

            final String nfc = Normalizer.normalize(sb, Normalizer.Form.NFC);
            final String nfd = Normalizer.normalize(sb, Normalizer.Form.NFD);
            assertEquals(Strings.displayWidth(nfc), Strings.displayWidth(nfd), () -> escape(nfc) + " vs " + escape(nfd));
        }
    }

    @Test
    public void testC568_clusterWidthDoesNotDependOnPartOrderAndPrependIsNeverTheBase() {
        // R4-01: a base-less spacing mark and a halfwidth sound mark both count, in either order
        assertEquals(1, Strings.displayWidth("\u093E"));
        assertEquals(2, Strings.displayWidth("\u093E\uFF9E"));
        assertEquals(2, Strings.displayWidth("\uFF9E\u093E"));
        assertEquals(2, Strings.displayWidth("\uFF9E\uFF9E"));
        assertEquals(3, Strings.displayWidth("\u093E\uFF9E\uFF9F"));
        assertEquals(1, Strings.displayWidth("\uFF9E"));

        // R4-02: a visible Prepend is drawn on the consonant that follows - never the base, nothing attaches to it
        assertEquals(1, Strings.displayWidth("\u0D4E\u093E"));
        assertEquals(1, Strings.displayWidth("\u0D4E\uFF9E"));
        assertEquals(1, Strings.displayWidth("\u0D4E"));
        assertEquals(1, Strings.displayWidth("\u0D4Ea"));
        assertEquals(2, Strings.displayWidth("\u0D4E\u0D15\u0D3E"));
        assertEquals(1, Strings.displayWidth("\u0600\u093E"));

        // the non-zero-class and canonically fused spacing marks add no cell (R4-03 javadoc)
        assertEquals(1, Strings.displayWidth("\u1B24\u1B44"));
        assertEquals(1, Strings.displayWidth("\u0B92\u0BD7"));
        assertEquals(Strings.displayWidth(cp(0x11383)), Strings.displayWidth(cp(0x11382) + cp(0x113C9)));
        assertEquals(Strings.displayWidth(cp(0x11391)), Strings.displayWidth(cp(0x11390) + cp(0x113C9)));
        assertEquals(1, Strings.displayWidth(cp(0x11382) + cp(0x113C9)));

        // an attached sound mark after a wide emoji follows the emoji policy of its base
        final String grinningSoundMark = SMILE + "\uFF9E";
        assertEquals(3, Strings.displayWidth(grinningSoundMark));
        assertEquals(2, Strings.displayWidth(grinningSoundMark, new DisplayWidthPolicy(1, 1)));
    }

    @Test
    public void testDisplayWidth_AsciiUncheckedFastPathExhaustive() {
        final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(2, 1) };

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (int codePoint = 0; codePoint <= 0x7F; codePoint++) {
                final int expected = codePoint <= 0x1F || codePoint == 0x7F ? 0 : 1;
                assertEquals(expected, Strings.codePointDisplayWidth(codePoint, policy), "U+" + Integer.toHexString(codePoint));
                assertEquals(expected, Strings.displayWidth(String.valueOf((char) codePoint), policy), "U+" + Integer.toHexString(codePoint));
            }
        }

        final StringBuilder printableAscii = new StringBuilder(95);
        for (char ch = 0x20; ch < 0x7F; ch++) {
            printableAscii.append(ch);
        }
        assertEquals(95, Strings.displayWidth(printableAscii.toString()));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(0x110000));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth('a', null));
    }

    // G075-01: code points at the edges of the shortcut ranges and mixed with marks, joiners and surrogates.
    @Test
    public void testDisplayWidth_commonWideRangesBoundariesAndClusters() {
        final int[][] codePoints = { { 0x3040 }, { 0x3097 }, { 0x3098 }, { 0x3099 }, { 0x309A }, { 0x3100 }, { 0x33FF }, { 0xA000 }, { 0xFF00 }, { 0xFF5F },
                { 0x3399, 0x9FFF, 0xA48C, 0xA48D }, { 0x33FF, 0x3400 }, { 0x9FFF, 0xA000 }, { 0xABFF, 0xAC00, 0xD7A3, 0xD7A4 }, { 0x3099, 0x4E00 },
                { 0x4E00, 0x3099 }, { 0x4E00, 0x0301 }, { 0x304B, 0x3099 }, { 0x61, 0x4E00, 0x62 }, { 0x30A2, 0xFF9E }, { 0x4E00, 0xFE0F },
                { 0x3042, 0x200D, 0x3042 }, { 0xAC00, 0x11A8 }, { 0x0915, 0x094D, 0x4E00 }, { 0x3400, 0x9FFF, 0x3041, 0x3096, 0x309B, 0x30FF, 0xFF01, 0xFF5E },
                { 0x0E01, 0x4E00 }, { 0x4E00, 0x0E33 }, { 0x30AB, 0x309A }, { 0x1F600, 0x4E00 }, { 0x4E00, 0x200D, 0x1F600 }, { 0x0600, 0x4E00 },
                { 0x4E00, 0xE0001 }, { 0x3040, 0x3097, 0x3098, 0x3099, 0x309A, 0x3100, 0x33FF, 0xA000, 0xFF00, 0xFF5F } };
        final int[] expected = { 1, 1, 1, 0, 0, 1, 2, 2, 1, 2, 7, 4, 4, 6, 2, 2, 2, 2, 4, 3, 2, 4, 2, 3, 16, 3, 2, 2, 4, 4, 2, 2, 11 };
        final Strings.DisplayWidthPolicy narrow = new Strings.DisplayWidthPolicy(1, 1);

        assertEquals(codePoints.length, expected.length);

        for (int i = 0; i < codePoints.length; i++) {
            final StringBuilder sb = new StringBuilder();

            for (final int codePoint : codePoints[i]) {
                sb.appendCodePoint(codePoint);
            }

            final String value = sb.toString();
            final String message = Arrays.toString(codePoints[i]);

            assertEquals(expected[i], Strings.displayWidth(value), message);
            assertEquals(expected[i], Strings.displayWidth(value, Strings.DisplayWidthPolicy.CJK), message);
            // An emoji takes the policy's emoji width; everything else here is policy-independent.
            assertEquals(expected[i] - (value.codePoints().anyMatch(codePoint -> codePoint >= 0x1F000 && codePoint <= 0x1FAFF) ? 1 : 0),
                    Strings.displayWidth(value, narrow), message);

            final String padding = expected[i] < 8 ? Strings.repeat(' ', 8 - expected[i]) : "";
            assertEquals(value + padding, Strings.padEndToDisplayWidth(value, 8), message);
            assertEquals(padding + value, Strings.padStartToDisplayWidth(value, 8), message);
        }
    }

    @Test
    public void testDisplayWidth_ConfigurableTerminalPolicy() {
        final Strings.DisplayWidthPolicy singleColumnEmoji = new Strings.DisplayWidthPolicy(1, 1);

        assertEquals(1, Strings.codePointDisplayWidth(0x00A1));
        assertEquals(2, Strings.codePointDisplayWidth(0x00A1, Strings.DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.codePointDisplayWidth(0x1F1FA));
        assertEquals(1, Strings.codePointDisplayWidth(0x1F1FA, singleColumnEmoji));
        assertEquals(1, Strings.codePointDisplayWidth(0x1F600, singleColumnEmoji));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(1, Strings.displayWidth("\u2764\uFE0F", singleColumnEmoji));
        assertEquals("¡ ", Strings.padEndToDisplayWidth("¡", 2));
        assertEquals("¡", Strings.padEndToDisplayWidth("¡", 2, Strings.DisplayWidthPolicy.CJK));

        assertThrows(IllegalArgumentException.class, () -> new Strings.DisplayWidthPolicy(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new Strings.DisplayWidthPolicy(1, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.displayWidth(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth('a', null));
        assertEquals("", Strings.padEndToDisplayWidth(null, 0));
        assertEquals("", Strings.padStartToDisplayWidth(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth(null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth(null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth(null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth(null, 0, null));
    }

    @Test
    public void testDisplayWidth_FormatAndHangulJamo() {
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(Character.MAX_CODE_POINT + 1));
        assertEquals(1, Strings.codePointDisplayWidth(0xD800)); // Java's valid code-point range includes surrogate values
        assertEquals(0, Strings.codePointDisplayWidth(0x200D)); // ZWJ, FORMAT
        assertEquals(0, Strings.codePointDisplayWidth(0x200C)); // ZWNJ, FORMAT
        assertEquals(0, Strings.codePointDisplayWidth(0x2028)); // Unicode line separator
        assertEquals(0, Strings.codePointDisplayWidth(0x2029)); // Unicode paragraph separator
        assertEquals(0, Strings.displayWidth("\u2028\u2029"));
        assertEquals(0, Strings.codePointDisplayWidth(0x00AD)); // soft hyphen is invisible without line layout
        assertEquals(0, Strings.codePointDisplayWidth(0x1161)); // Hangul medial V
        assertEquals(0, Strings.codePointDisplayWidth(0x11AB)); // Hangul final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7C6)); // last Extended-B medial V
        assertEquals(1, Strings.codePointDisplayWidth(0xD7C7)); // unassigned gap after Extended-B medial V
        assertEquals(1, Strings.codePointDisplayWidth(0xD7CA)); // unassigned gap before Extended-B final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7CB)); // first Extended-B final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7FB)); // last Extended-B final T
        assertEquals(1, Strings.codePointDisplayWidth(0xD7FC)); // unassigned gap after Extended-B final T
        assertEquals(1, Strings.codePointDisplayWidth(0xD7FF)); // end of the unassigned gap
        assertEquals(2, Strings.codePointDisplayWidth(0x1100)); // Hangul leading L
        assertEquals(2, Strings.codePointDisplayWidth(0x1112)); // Hangul leading HIEUH
        assertEquals(2, Strings.codePointDisplayWidth(0x231A)); // Unicode East Asian Width W and default emoji presentation
        assertEquals(2, Strings.codePointDisplayWidth(0x1F600)); // grinning face: Emoji_Presentation
        assertEquals(2, Strings.codePointDisplayWidth(0x1F1FA)); // regional indicator U: Emoji_Presentation, not EAW W/F
        assertEquals(2, Strings.codePointDisplayWidth(0x1F3FD)); // medium skin-tone modifier
        assertEquals(1, Strings.codePointDisplayWidth(0x2764)); // text-default heavy black heart
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0F)); // emoji variation selector: zero-width, cluster promoter only
        assertEquals(0, Strings.codePointDisplayWidth(0x20E3)); // keycap mark: zero-width, cluster promoter only
        assertEquals(1, Strings.codePointDisplayWidth(0x093E)); // Devanagari spacing vowel sign is visible when standalone
        assertEquals(1, Strings.displayWidth("\u093E"));
        assertEquals(2, Strings.displayWidth("\u0915\u093E")); // C-568: an Mc after a visible base takes a cell of its own
        assertEquals(2, Strings.displayWidth("한"));
        assertEquals(2, Strings.displayWidth(java.text.Normalizer.normalize("한", java.text.Normalizer.Form.NFD)));
        assertEquals(1, Strings.codePointDisplayWidth('a'));
        assertEquals(2, Strings.codePointDisplayWidth('中'));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F600))), Strings.codePointDisplayWidth(0x1F600));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F1FA))), Strings.codePointDisplayWidth(0x1F1FA));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F3FD))), Strings.codePointDisplayWidth(0x1F3FD));
        assertEquals(Strings.displayWidth("\u2764"), Strings.codePointDisplayWidth(0x2764));
        assertEquals("⌚", Strings.padEndToDisplayWidth("⌚", 2));
    }

    @Test
    public void testDisplayWidth_LateClusterPreservesAdditivePrefix() {
        final String asciiPrefix = "a".repeat(4096);
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String supplementaryHan = new String(Character.toChars(0x20000));
        final String ideographicVariationSelector = new String(Character.toChars(0xE0100));

        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\u0301"));
        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\u200D"));
        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\r\n\u0301"));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + "1\uFE0F\u20E3"));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + supplementaryHan + ideographicVariationSelector));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + "\uAC00\u11A8"));
        assertEquals(4098, Strings.displayWidth(asciiPrefix + womanTechnologist));
        assertEquals(4097, Strings.displayWidth(asciiPrefix + womanTechnologist, new Strings.DisplayWidthPolicy(1, 1)));
    }

    @Test
    public void testDisplayWidth_PrefixOptimizationMatchesFullSegmentation() throws ReflectiveOperationException {
        final Method nextBoundary = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
        final Method clusterWidth = Strings.class.getDeclaredMethod("graphemeClusterDisplayWidth", String.class, int.class, int.class,
                Strings.DisplayWidthPolicy.class);
        nextBoundary.setAccessible(true);
        clusterWidth.setAccessible(true);

        final String supplementaryHan = new String(Character.toChars(0x20000));
        final String[] additivePrefixes = { "", "a", "abc", "a".repeat(64), "\r\n", "a".repeat(64) + "\r\n", "\u4E2D\uFF21\uAC00",
                "a".repeat(64) + supplementaryHan };
        final String[] sequenceSensitiveSuffixes = { "\u0301", "\u200D", "\uFE0F", "\u1100\u1161\u11A8", "\uD83D\uDC69\u200D\uD83D\uDCBB", "\u0915\u094D\u0937",
                "\uD83C\uDDFA\uD83C\uDDF8", "\u0600a", new String(Character.toChars(0xE0100)) };
        final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(1, 1) };

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (final String prefix : additivePrefixes) {
                for (final String suffix : sequenceSensitiveSuffixes) {
                    final String value = prefix + suffix;
                    assertEquals(fullySegmentedDisplayWidth(value, policy, nextBoundary, clusterWidth), Strings.displayWidth(value, policy));
                }
            }
        }

        final int[] alphabet = { 'a', '\r', '\n', 0x0301, 0x0600, 0x0915, 0x094D, 0x0937, 0x1100, 0x1161, 0x11A8, 0x200D, 0x2764, 0xFE0F, 0x20E3, 0xAC00,
                0x4E2D, 0xFF21, 0x1F1FA, 0x1F1F8, 0x1F469, 0x1F4BB, 0x1F3FD, 0x20000, 0xE0100, 0xD800, 0xDC00 };
        final Random random = new Random(20260830L);

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (int iteration = 0; iteration < 1_000; iteration++) {
                final StringBuilder value = new StringBuilder();
                final int length = random.nextInt(41);

                for (int i = 0; i < length; i++) {
                    value.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
                }

                assertEquals(fullySegmentedDisplayWidth(value.toString(), policy, nextBoundary, clusterWidth), Strings.displayWidth(value.toString(), policy),
                        "iteration=" + iteration + ", policy=" + policy);
            }
        }
    }

    @Test
    public void testDisplayWidth_StringClusters() {
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String thumbsUpMediumSkinTone = "\uD83D\uDC4D\uD83C\uDFFD";
        final String usFlag = "\uD83C\uDDFA\uD83C\uDDF8";
        final String keycapOne = "1\uFE0F\u20E3";
        final String devanagariConjunct = "\u0915\u094D\u0937";
        final String cjkIdeographicVariationSequence = "\u4E2D" + new String(Character.toChars(0xE0100));

        assertEquals(0, Strings.displayWidth((String) null));
        assertEquals(0, Strings.displayWidth(""));
        assertEquals(3, Strings.displayWidth("abc"));
        assertEquals(4, Strings.displayWidth("\u4E2D\u6587"));
        assertEquals(1, Strings.displayWidth("\u00A1")); // East Asian Width A remains narrow.
        assertEquals(2, Strings.displayWidth("\u231A")); // Default emoji presentation.
        assertEquals(1, Strings.displayWidth("e\u0301"));
        assertEquals(1, Strings.displayWidth("A\u3099")); // Zero-width EAW-W combining mark does not promote a narrow base.
        assertEquals(0, Strings.displayWidth("\u0301"));
        assertEquals(0, Strings.displayWidth("\u200D"));
        assertEquals(2, Strings.displayWidth(java.text.Normalizer.normalize("\uD55C", java.text.Normalizer.Form.NFD)));

        assertEquals(2, Strings.displayWidth(womanTechnologist));
        assertEquals(2, Strings.displayWidth(thumbsUpMediumSkinTone));
        assertEquals(2, Strings.displayWidth(usFlag));
        assertEquals(2, Strings.displayWidth(keycapOne));
        assertEquals(1, Strings.displayWidth("\u2764"));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(2, Strings.displayWidth("1\uFE0F")); // standardized emoji-presentation sequence
        assertEquals(1, Strings.displayWidth("a\uFE0F")); // unsupported variation selector does not promote
        assertEquals(1, Strings.displayWidth("a\u20E3")); // keycap mark requires a valid keycap sequence
        assertEquals(1, Strings.displayWidth("1\u20E3")); // unqualified keycap sequence is not promoted
        assertEquals(1, Strings.displayWidth("a\uFE0F\u20E3"));
        assertEquals(1, Strings.displayWidth("\uD83D"));
        assertEquals(2, Strings.displayWidth(cjkIdeographicVariationSequence));
        assertEquals(2, Strings.codePointDisplayWidth(0x0915) + Strings.codePointDisplayWidth(0x094D) + Strings.codePointDisplayWidth(0x0937));
        assertEquals(1, Strings.displayWidth(devanagariConjunct));
        assertEquals(devanagariConjunct + " ", Strings.padEndToDisplayWidth(devanagariConjunct, 2));

        // Common standalone East Asian scalars share the additive fast path with ASCII.
        assertEquals(3, Strings.displayWidth("abc"));
        assertEquals(4, Strings.displayWidth("ab中"));
        assertEquals(2, Strings.displayWidth("한"));
        assertEquals(4, Strings.displayWidth("ＡＢ"));
        assertEquals(1, Strings.displayWidth("e\u0301"));
        // Segmentation path: summing code-point widths over-counts a ZWJ cluster (2+0+2 vs 2).
        assertEquals(4, Strings.codePointDisplayWidth(0x1F469) + Strings.codePointDisplayWidth(0x200D) + Strings.codePointDisplayWidth(0x1F4BB));
        assertEquals(2, Strings.displayWidth(womanTechnologist));
    }

    @Test
    public void testC532_hangulSyllableBlocksMeasureAsOneCluster() {
        final String ll = "\u1100\u1100";
        final String lSyllable = "\u1100\uAC00";
        final String nfd = Normalizer.normalize(lSyllable, Normalizer.Form.NFD);
        assertEquals("\u1100\u1100\u1161", nfd);

        assertEquals(2, Strings.displayWidth(ll));
        assertEquals(2, Strings.displayWidth(lSyllable));
        assertEquals(2, Strings.displayWidth(nfd));
        assertEquals(Strings.displayWidth(lSyllable), Strings.displayWidth(nfd));
        assertEquals(2, Strings.displayWidth("\u1107\uC0C1"));
        assertEquals(4, Strings.displayWidth("\uAC00\uAC00"));
        assertEquals(4, Strings.displayWidth("a" + ll + "b"));

        assertEquals(2, Strings.displayWidth(ll, DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.displayWidth(lSyllable, DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.displayWidth("\u1107\uC0C1", DisplayWidthPolicy.CJK));
        assertEquals(4, Strings.displayWidth("\uAC00\uAC00", DisplayWidthPolicy.CJK));
        assertEquals(4, Strings.displayWidth("a" + ll + "b", DisplayWidthPolicy.CJK));

        assertEquals(ll + "  ", Strings.padEndToDisplayWidth(ll, 4));
        assertEquals("  " + ll, Strings.padStartToDisplayWidth(ll, 4));

        // a per-code-point sum would say 4 for the two leading jamo
        assertEquals(2, Strings.codePointDisplayWidth(0x1100));
        assertEquals(0, Strings.codePointDisplayWidth(0x1161));
    }

    @Test
    public void testPadEndByDisplayWidth() {
        assertEquals("ab   ", Strings.padEndToDisplayWidth("ab", 5));
        assertEquals("abcde", Strings.padEndToDisplayWidth("abcde", 3));
        assertEquals("中文 ", Strings.padEndToDisplayWidth("中文", 5));
        assertEquals("   ", Strings.padEndToDisplayWidth(null, 3));
        // R-16: a negative minDisplayWidth is now rejected (was silently accepted before)
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth("ab", -1));

        // Cluster-aware: ZWJ emoji and keycaps are one display unit (width 2), not a sum of code points.
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String keycapOne = "1\uFE0F\u20E3";
        assertEquals(2, Strings.displayWidth(womanTechnologist));
        assertEquals(womanTechnologist, Strings.padEndToDisplayWidth(womanTechnologist, 2));
        assertEquals(womanTechnologist + "  ", Strings.padEndToDisplayWidth(womanTechnologist, 4));
        assertEquals(keycapOne, Strings.padEndToDisplayWidth(keycapOne, 2));
    }

    @Test
    public void testPadToDisplayWidth_padSpaceAbsorbedByBoundaryCluster() {
        // A leading Extend/SpacingMark code point pulls the adjacent pad space into its own grapheme cluster. A width-2
        // Extend (the emoji modifier) then measures max(1, 2) with the space, so that space used to add no column at all;
        // since C-568 a spacing mark adds its own cell after the space, and a Prepend still adds none to its cluster.
        final String emojiModifier = new String(Character.toChars(0x1F3FB)); // EMOJI MODIFIER FITZPATRICK TYPE-1-2 (Extend, width 2)
        final String devanagariAa = new String(Character.toChars(0x093E)); // DEVANAGARI VOWEL SIGN AA (SpacingMark, width 1)
        final String malayalamDotReph = new String(Character.toChars(0x0D4E)); // MALAYALAM LETTER DOT REPH (Prepend, width 1)
        final String zhongWen = new String(Character.toChars(0x4E2D)) + new String(Character.toChars(0x6587));

        final String[] inputs = { emojiModifier, devanagariAa, malayalamDotReph, emojiModifier + "ab", devanagariAa + "ka", "ab" + malayalamDotReph };

        for (int minWidth = 0; minWidth <= 8; minWidth++) {
            for (final String s : inputs) {
                final String padStarted = Strings.padStartToDisplayWidth(s, minWidth);
                final String padEnded = Strings.padEndToDisplayWidth(s, minWidth);

                assertTrue(Strings.displayWidth(padStarted) >= minWidth, "padStartToDisplayWidth width " + Strings.displayWidth(padStarted) + " < " + minWidth);
                assertTrue(Strings.displayWidth(padEnded) >= minWidth, "padEndToDisplayWidth width " + Strings.displayWidth(padEnded) + " < " + minWidth);
                assertTrue(padStarted.endsWith(s), "padStartToDisplayWidth must keep the input as a suffix");
                assertTrue(padEnded.startsWith(s), "padEndToDisplayWidth must keep the input as a prefix");
            }
        }

        // The exact off-by-one that used to occur.
        assertEquals(4, Strings.displayWidth(Strings.padStartToDisplayWidth(emojiModifier, 4)));
        assertEquals(3, Strings.displayWidth(Strings.padStartToDisplayWidth(devanagariAa, 3)));
        assertEquals(3, Strings.displayWidth(Strings.padEndToDisplayWidth(malayalamDotReph, 3)));

        // Ordinary text keeps landing exactly on minDisplayWidth, with no compensating space.
        assertEquals("   ab", Strings.padStartToDisplayWidth("ab", 5));
        assertEquals("ab   ", Strings.padEndToDisplayWidth("ab", 5));
        assertEquals(6, Strings.displayWidth(Strings.padStartToDisplayWidth(zhongWen, 6)));
        assertEquals(6, Strings.displayWidth(Strings.padEndToDisplayWidth(zhongWen, 6)));
        assertEquals("  " + zhongWen, Strings.padStartToDisplayWidth(zhongWen, 6));
    }

    @Test
    public void testPadToDisplayWidth_postconditionAcrossAllCodePoints() {
        for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) {
                continue;
            }

            final String s = new String(Character.toChars(codePoint));
            final int width = Strings.displayWidth(s);

            // Only widths just above the input width can expose an absorbed pad space.
            for (int minWidth = width + 1; minWidth <= width + 2; minWidth++) {
                if (Strings.displayWidth(Strings.padStartToDisplayWidth(s, minWidth)) < minWidth) {
                    fail("padStartToDisplayWidth fell short for U+" + Integer.toHexString(codePoint) + " at minWidth=" + minWidth);
                }

                if (Strings.displayWidth(Strings.padEndToDisplayWidth(s, minWidth)) < minWidth) {
                    fail("padEndToDisplayWidth fell short for U+" + Integer.toHexString(codePoint) + " at minWidth=" + minWidth);
                }
            }
        }
    }

    @Test
    public void testC568_padToDisplayWidthIsExactAroundSpacingComponents() {
        for (int width = 0; width <= 5; width++) {
            for (final String str : new String[] { "\u093E\uFF9E", "\uFF9E", "\u093E", "\u0D4E", "\u0D4E\u093E", "\u0E33", "\u302E", "\uD83C\uDFFB",
                    "\u1B35\uFF9E\uAC00", "k\u093E" }) {
                final int expected = Math.max(width, Strings.displayWidth(str));
                assertEquals(expected, Strings.displayWidth(Strings.padStartToDisplayWidth(str, width)), escape(str) + " start " + width);
                assertEquals(expected, Strings.displayWidth(Strings.padEndToDisplayWidth(str, width)), escape(str) + " end " + width);
            }
        }

        // seeded fuzz over the code points whose clusters can absorb or add a column: the padded result is always
        // exactly the requested width (or the input's own width when that is larger)
        final int[] pool = { 'a', ' ', 0x0915, 0x093E, 0x094D, 0x0937, 0x0E01, 0x0E33, 0x0EB3, 0x0B92, 0x0BD7, 0x1B05, 0x1B35, 0x1B44, 0x302E, 0x0D4E, 0x0600,
                0xFF76, 0xFF9E, 0xFF9F, 0x0301, 0x200D, 0xFE0F, 0x2764, 0x1F600, 0x1F3FB, 0x4E2D, 0x1100, 0x1161, 0x11A8, 0x16FF0 };
        final Random random = new Random(5680L);

        for (int iteration = 0; iteration < 20_000; iteration++) {
            final StringBuilder sb = new StringBuilder();
            final int count = 1 + random.nextInt(5);

            for (int k = 0; k < count; k++) {
                sb.appendCodePoint(pool[random.nextInt(pool.length)]);
            }

            final String str = sb.toString();
            final int width = random.nextInt(9);
            final int expected = Math.max(width, Strings.displayWidth(str));
            assertEquals(expected, Strings.displayWidth(Strings.padStartToDisplayWidth(str, width)), escape(str) + " start " + width);
            assertEquals(expected, Strings.displayWidth(Strings.padEndToDisplayWidth(str, width)), escape(str) + " end " + width);
        }
    }

    @Test
    public void testC568_spacingComponentsAddACell() {
        for (final DisplayWidthPolicy policy : new DisplayWidthPolicy[] { DisplayWidthPolicy.DEFAULT, DisplayWidthPolicy.CJK }) {
            // halfwidth katakana + voiced / semi-voiced sound marks
            assertEquals(2, Strings.displayWidth("\uFF76\uFF9E", policy));
            assertEquals(2, Strings.displayWidth("\uFF8A\uFF9F", policy));
            assertEquals(4, Strings.displayWidth("\uFF76\uFF9E\uFF77\uFF9E", policy));
            assertEquals(3, Strings.displayWidth("\uFF76\uFF9E\uFF9E", policy));
            assertEquals(1, Strings.displayWidth("\uFF9E", policy));
            assertEquals(2, Strings.displayWidth(" \uFF9E", policy));

            // spacing vowel signs (Mc, ccc 0) and Thai / Lao AM after a visible base
            assertEquals(2, Strings.displayWidth("\u0E01\u0E33", policy)); // Thai KO KAI + SARA AM
            assertEquals(2, Strings.displayWidth("\u0E01\u0E48\u0E33", policy));
            assertEquals(2, Strings.displayWidth("\u0E81\u0EB3", policy)); // Lao KO + AM
            assertEquals(2, Strings.displayWidth("\u0915\u093E", policy)); // Devanagari KA + AA
            assertEquals(2, Strings.displayWidth("\u0915\u093F", policy)); // KA + I (drawn on the left, still a cell)
            assertEquals(2, Strings.displayWidth("\u0915\u093E\u0903", policy)); // at most one extra cell per cluster
            assertEquals(2, Strings.displayWidth("\u1780\u17B6", policy)); // Khmer KA + AA
            assertEquals(2, Strings.displayWidth("\u1000\u102C", policy)); // Myanmar KA + AA (two clusters by UAX #29)
            assertEquals(4, Strings.displayWidth("\u0915\u093E\u0916\u093F", policy));

            // never a third cell after a wide base
            assertEquals(2, Strings.displayWidth("\u4E2D\u093E", policy));
            assertEquals(2, Strings.displayWidth("\uAC00\u093E", policy));

            // unchanged: no visible base, Prepend, conjuncts, non-spacing marks
            assertEquals(1, Strings.displayWidth("\u093E", policy));
            assertEquals(1, Strings.displayWidth("\u0E33", policy));
            assertEquals(1, Strings.displayWidth("\u0D4Ea", policy)); // MALAYALAM LETTER DOT REPH (Prepend) + a
            assertEquals(1, Strings.displayWidth("\u0600" + "1", policy)); // Cf Prepend
            assertEquals(1, Strings.displayWidth("\u0915\u094D\u0937", policy)); // conjunct KSSA stays its base width
            assertEquals(2, Strings.displayWidth("\u0915\u094D\u0937\u093E", policy));
            assertEquals(1, Strings.displayWidth("e\u0301", policy));
            assertEquals(1, Strings.displayWidth("A\u3099", policy));
            assertEquals(1, Strings.displayWidth("\u1B13\u1B44", policy)); // spacing virama (ccc 9) adds no cell
            assertEquals(2, Strings.displayWidth("\uAC00\u302E", policy)); // Hangul tone mark (Mc, ccc 224, wide)

            // canonically fused spacing marks fold into their letter
            assertEquals(1, Strings.displayWidth("\u1B06", policy));
            assertEquals(1, Strings.displayWidth("\u1B05\u1B35", policy));
            // after a non-spacing vowel sign the same mark composes into a spacing vowel sign and adds the cell
            assertEquals(2, Strings.displayWidth("k\u1B3B", policy));
            assertEquals(2, Strings.displayWidth("k\u1B3A\u1B35", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BD7", policy)); // U+0BD7 after a consonant it does not fuse with
            assertEquals(1, Strings.displayWidth("\u0B94", policy));
            assertEquals(1, Strings.displayWidth("\u0B92\u0BD7", policy));
            assertEquals(1, Strings.displayWidth("\u0BCC", policy));
            assertEquals(1, Strings.displayWidth("\u0BC6\u0BD7", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BCC", policy));
            assertEquals(2, Strings.displayWidth("\u0B95\u0BC6\u0BD7", policy));
            assertEquals(2, Strings.displayWidth("\u0995\u09CB", policy));
            assertEquals(2, Strings.displayWidth("\u0995\u09C7\u09BE", policy));
            assertEquals(Strings.displayWidth(cp(0x1138E), policy), Strings.displayWidth(cp(0x1138B) + cp(0x113C2), policy));
            assertEquals(Strings.displayWidth(cp(0x1D15F), policy), Strings.displayWidth(cp(0x1D158) + cp(0x1D165), policy));

            // emoji / flag / Hangul clusters unchanged
            assertEquals(2, Strings.displayWidth("\uD83D\uDC69\u200D\uD83D\uDCBB", policy));
            assertEquals(2, Strings.displayWidth("\uD83C\uDDFA\uD83C\uDDF8", policy));
            assertEquals(2, Strings.displayWidth("1\uFE0F\u20E3", policy));
            assertEquals(2, Strings.displayWidth("\uD83D\uDC4D\uD83C\uDFFD", policy));
            assertEquals(2, Strings.displayWidth("\uD55C", policy));
            assertEquals(2, Strings.displayWidth("\u1112\u1161\u11AB", policy));
            assertEquals(2, Strings.displayWidth("\u1100\uAC00", policy));
        }

        // the pad helpers now see the right width
        assertEquals("\uFF76\uFF9E  ", Strings.padEndToDisplayWidth("\uFF76\uFF9E", 4));
        assertEquals("  \uFF76\uFF9E", Strings.padStartToDisplayWidth("\uFF76\uFF9E", 4));
        assertEquals("\u0E01\u0E33  ", Strings.padEndToDisplayWidth("\u0E01\u0E33", 4));
        assertEquals("\u0915\u093E", Strings.padEndToDisplayWidth("\u0915\u093E", 2));
        assertEquals(4, Strings.displayWidth(Strings.padStartToDisplayWidth("\u093E", 4)));
    }

    @Test
    public void testC535_textPresentationSelectorNeverNarrows() {
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0E));
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0F));
        assertEquals(2, Strings.displayWidth("\u231A\uFE0E"));
        assertEquals(2, Strings.displayWidth("\u231A"));
        assertEquals(1, Strings.displayWidth("\u2764\uFE0E"));
        assertEquals(1, Strings.displayWidth("\u2764"));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(2, Strings.displayWidth(EMOJI + "\uFE0E"));
        assertEquals(1, Strings.displayWidth("a\uFE0E"));
    }

    @Test
    public void testPadStartByDisplayWidth() {
        // normal
        assertEquals("   ab", Strings.padStartToDisplayWidth("ab", 5));
        // wide CJK characters count as 2 each (display width 4 -> 1 space)
        assertEquals(" 中文", Strings.padStartToDisplayWidth("中文", 5));
        // already wide enough
        assertEquals("abcde", Strings.padStartToDisplayWidth("abcde", 3));
        assertEquals("abc", Strings.padStartToDisplayWidth("abc", 3));
        // null treated as empty, then padded
        assertEquals("   ", Strings.padStartToDisplayWidth(null, 3));
        assertEquals("", Strings.padStartToDisplayWidth(null, 0));
        // empty input
        assertEquals("  ", Strings.padStartToDisplayWidth("", 2));
        // negative width throws
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth("ab", -1));

        // Cluster-aware: ZWJ emoji is one display unit (width 2), so two spaces bring it to width 4.
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        assertEquals("  " + womanTechnologist, Strings.padStartToDisplayWidth(womanTechnologist, 4));
        assertEquals(womanTechnologist, Strings.padStartToDisplayWidth(womanTechnologist, 2));
    }

    @Test
    public void testUnicode17ExtendedGraphemeClusterConformance() throws Exception {
        final String resourceName = "/GraphemeBreakTest-17.0.0.txt";
        final InputStream input = StringsTest.class.getResourceAsStream(resourceName);
        assertNotNull(input, "Missing Unicode conformance resource: " + resourceName);

        final Method boundaryMethod = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
        boundaryMethod.setAccessible(true);
        int testCaseCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                final int commentStart = line.indexOf('#');
                final String testData = (commentStart < 0 ? line : line.substring(0, commentStart)).trim();

                if (testData.isEmpty()) {
                    continue;
                }

                final StringBuilder sampleBuilder = new StringBuilder();
                final List<Integer> expectedBoundaries = new ArrayList<>();
                int utf16Offset = 0;

                for (final String token : testData.split("\\s+")) {
                    if ("\u00F7".equals(token)) {
                        expectedBoundaries.add(utf16Offset);
                    } else if (!"\u00D7".equals(token)) {
                        final int codePoint = Integer.parseInt(token, 16);
                        sampleBuilder.appendCodePoint(codePoint);
                        utf16Offset += Character.charCount(codePoint);
                    }
                }

                final String sample = sampleBuilder.toString();
                final List<Integer> actualBoundaries = new ArrayList<>();
                actualBoundaries.add(0);

                for (int clusterStart = 0; clusterStart < sample.length();) {
                    final int clusterEnd = (Integer) boundaryMethod.invoke(null, sample, clusterStart);
                    assertTrue(clusterEnd > clusterStart && clusterEnd <= sample.length(),
                            "Invalid boundary for GraphemeBreakTest line " + lineNumber + ": " + testData);
                    actualBoundaries.add(clusterEnd);
                    clusterStart = clusterEnd;
                }

                assertEquals(expectedBoundaries, actualBoundaries, "GraphemeBreakTest line " + lineNumber + ": " + testData);
                testCaseCount++;
            }
        }

        assertEquals(766, testCaseCount, "Unexpected Unicode 17 grapheme-break test count");
    }

    @Test
    public void testUnicodeDisplayWidthTables_AreSortedAndDisjoint() throws ReflectiveOperationException {
        assertEquals("17.0.0", Strings.DISPLAY_WIDTH_UNICODE_VERSION);
        assertEquals(Strings.DISPLAY_WIDTH_UNICODE_VERSION, Unicode17Data.UNICODE_VERSION);
        assertCodePointRangesAreSortedAndDisjoint("ZERO_WIDTH_CATEGORY_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EAST_ASIAN_WIDE_OR_FULLWIDTH_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EAST_ASIAN_AMBIGUOUS_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EMOJI_PRESENTATION_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EMOJI_VARIATION_BASE_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EXTENDED_PICTOGRAPHIC_RANGES");
        assertPropertyRangesAreSortedAndDisjoint("GRAPHEME_BREAK_RANGES");
        assertPropertyRangesAreSortedAndDisjoint("INDIC_CONJUNCT_BREAK_RANGES");
    }

}
