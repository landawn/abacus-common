package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Optional;

/**
 * Tests for the fixes of the 2026-09-02 review of {@link Strings}.
 *
 * <ul>
 *   <li><b>B1</b> the five JLS 17 §3.8 restricted identifiers ({@code permits}, {@code record},
 *       {@code sealed}, {@code var}, {@code yield}) are excluded from {@code TypeIdentifier} only, so they are
 *       rejected by {@code isValidJavaTypeIdentifier} and <em>accepted</em> by {@code isValidJavaIdentifier}:
 *       {@code class sealed {}} does not compile, while {@code int var = 1;} and {@code package com.foo.record;}
 *       do. (Before 2026-09-08 {@code isValidJavaIdentifier} rejected all five, so it answered {@code false} for
 *       field and package names that javac accepts.) The other contextual keywords ({@code module},
 *       {@code open}, {@code when}, ...) remain legal names for both.</li>
 *   <li><b>B2</b> the seven {@code Charset}-taking encoders ({@code getBytes}, {@code getBytesStrict},
 *       {@code base64EncodeString}, {@code base64EncodeStringStrict}, {@code base64UrlEncodeString},
 *       {@code base64UrlEncodeStringStrict}, {@code base64MimeEncodeString}) propagate the
 *       {@code UnsupportedOperationException} that {@code Charset.newEncoder()} / {@code String.getBytes} throw
 *       for a decode-only charset. That was undocumented; it is now documented and pinned here for both the
 *       strict and non-strict paths (they must agree).</li>
 *   <li><b>D1</b> {@code toCamelCase} / {@code toUpperCamelCase} capitalized a word's first letter with
 *       {@code String.toUpperCase}, while {@code capitalize}, {@code capitalizeWords} and
 *       {@code capitalizeWordsFully} use the Unicode 17 titlecase table. They now agree: U+01C6 becomes U+01C5
 *       (not U+01C4) and {@code "ß"} becomes {@code "Ss"} (not {@code "SS"}). Only the Lt digraphs and the
 *       one-to-many titlecase mappings are affected; ASCII and every other script are unchanged.</li>
 *   <li><b>D2</b> {@code substringBetween(String, IntUnaryOperator, int)} rejected an end index of {@code 0} while
 *       {@code substringBetween(String, int, int)} accepts it, so {@code substringBetween("abc", i -> -1, 0)} was
 *       {@code null} where {@code substringBetween("abc", -1, 0)} is {@code ""}. The guard is now {@code < 0}; the
 *       only inputs whose result changes are end {@code 0} with an operator returning {@code -1}, which now yield
 *       {@code ""}.</li>
 *   <li><b>D3</b> {@code toCodePoints(CharSequence)} streams the sequence directly instead of copying it through
 *       {@code toString()} first.</li>
 * </ul>
 */
public class StringsRegressionETest extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // B1 : isValidJavaIdentifier and the JLS 17 restricted identifiers
    // ---------------------------------------------------------------------------------------------

    private static final String[] RESTRICTED = { "permits", "record", "sealed", "var", "yield" };

    @Test
    public void testIsValidJavaIdentifier_Jls17RestrictedIdentifiers() {
        for (final String s : RESTRICTED) {
            // JLS 3.8 excludes the five from TypeIdentifier ONLY, so they are ordinary Identifiers: a field,
            // parameter, method or package segment may be named "record" or "var". Only a TYPE may not.
            assertTrue(Strings.isValidJavaIdentifier(s), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(s), s);
            // Neither predicate leans on SourceVersion for this: none of the five is a keyword.
            assertFalse(Strings.isJavaKeyword(s), s + " is not a keyword");
            // The match is exact, so a name that merely contains one is still a legal type name.
            assertTrue(Strings.isValidJavaTypeIdentifier(s + "1"), s + "1");
            assertTrue(Strings.isValidJavaTypeIdentifier("_" + s), "_" + s);
            assertTrue(Strings.isValidJavaTypeIdentifier("my" + Strings.capitalize(s)), "my" + s);
            // ... and case-sensitive, like javac.
            assertTrue(Strings.isValidJavaTypeIdentifier(Strings.capitalize(s)), Strings.capitalize(s));
            assertTrue(Strings.isValidJavaTypeIdentifier(s.toUpperCase(java.util.Locale.ROOT)), s.toUpperCase(java.util.Locale.ROOT));
        }
    }

    @Test
    public void testIsValidJavaIdentifier_RestrictedAsCharSequence() {
        for (final String s : RESTRICTED) {
            assertTrue(Strings.isValidJavaIdentifier(new StringBuilder(s)), s);
            assertTrue(Strings.isValidJavaIdentifier(java.nio.CharBuffer.wrap(s)), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(new StringBuilder(s)), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(java.nio.CharBuffer.wrap(s)), s);
        }
    }

    @Test
    public void testIsValidJavaIdentifier_ContextualKeywords() {
        // JLS 17 §3.9 contextual keywords that are NOT excluded from TypeIdentifier: all legal type names.
        for (final String s : new String[] { "when", "module", "open", "requires", "exports", "opens", "to", "uses", "provides", "with", "transitive" }) {
            assertTrue(Strings.isValidJavaIdentifier(s), s);
            assertTrue(Strings.isValidJavaTypeIdentifier(s), s);
            assertFalse(Strings.isJavaKeyword(s), s);
        }

        assertFalse(Strings.isValidJavaIdentifier("non-sealed")); // fails the character test, not the list
        assertFalse(Strings.isValidJavaIdentifier("_")); // a keyword since Java 9
        assertFalse(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier("true"));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertFalse(Strings.isValidJavaIdentifier(null));

        // Everything a keyword/character test rejects is rejected by the type-name predicate too.
        assertFalse(Strings.isValidJavaTypeIdentifier("non-sealed"));
        assertFalse(Strings.isValidJavaTypeIdentifier("_"));
        assertFalse(Strings.isValidJavaTypeIdentifier("class"));
        assertFalse(Strings.isValidJavaTypeIdentifier("true"));
        assertFalse(Strings.isValidJavaTypeIdentifier(""));
        assertFalse(Strings.isValidJavaTypeIdentifier(null));
    }

    // ---------------------------------------------------------------------------------------------
    // B2 : decode-only charsets
    // ---------------------------------------------------------------------------------------------

    /** A charset with no encoder, or {@code null} if this JDK ships none. JDK 25 ships ISO-2022-CN and x-JISAutoDetect. */
    private static Charset decodeOnlyCharset() {
        for (final Charset cs : Charset.availableCharsets().values()) {
            if (!cs.canEncode()) {
                return cs;
            }
        }

        return null;
    }

    @Test
    public void testEncoders_DecodeOnlyCharsetThrows() {
        final Charset cs = decodeOnlyCharset();
        assumeTrue(cs != null, "this JDK has no decode-only charset");

        // Non-strict and strict paths must fail the same way: the JDK's own String.getBytes(Charset) throws UOE.
        assertThrows(UnsupportedOperationException.class, () -> "a".getBytes(cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytes("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytesStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64EncodeString("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64EncodeStringStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64UrlEncodeString("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64UrlEncodeStringStrict("a", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.base64MimeEncodeString("a", cs));

        // getBytes* consult the charset for an empty (non-null) string too, exactly like String.getBytes.
        assertThrows(UnsupportedOperationException.class, () -> "".getBytes(cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytes("", cs));
        assertThrows(UnsupportedOperationException.class, () -> Strings.getBytesStrict("", cs));
    }

    @Test
    public void testEncoders_PrimaryFirstShortCircuitsDecodeOnlyCharset() {
        final Charset cs = decodeOnlyCharset();
        assumeTrue(cs != null, "this JDK has no decode-only charset");

        assertNull(Strings.getBytes(null, cs));
        assertNull(Strings.getBytesStrict(null, cs));

        for (final String s : new String[] { null, "" }) {
            assertEquals("", Strings.base64EncodeString(s, cs));
            assertEquals("", Strings.base64EncodeStringStrict(s, cs));
            assertEquals("", Strings.base64UrlEncodeString(s, cs));
            assertEquals("", Strings.base64UrlEncodeStringStrict(s, cs));
            assertEquals("", Strings.base64MimeEncodeString(s, cs));
        }
    }

    @Test
    public void testEncoders_UnmappableCharacters() {
        // The strict contract for a charset that CAN encode is unchanged: unmappable -> IAE, lenient -> replaced.
        assertThrows(IllegalArgumentException.class, () -> Strings.getBytesStrict("é", StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[] { '?' }, Strings.getBytes("é", StandardCharsets.US_ASCII));
        assertEquals("Pw==", Strings.base64EncodeString("é", StandardCharsets.US_ASCII));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64EncodeStringStrict("é", StandardCharsets.US_ASCII));
    }

    // ---------------------------------------------------------------------------------------------
    // D1 : camel case capitalizes with the titlecase mapping, like capitalize()
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testToCamelCase_TitlecaseNotUppercase() {
        // U+01C6 dž -> U+01C5 Dž (titlecase), not U+01C4 DŽ (uppercase)
        assertEquals("ǅen", Strings.toUpperCamelCase("ǆen"));
        assertEquals("ǅen", Strings.toUpperCamelCase("ǄEN")); // lowercased first, then titlecased
        assertEquals("ǅen", Strings.toUpperCamelCase("ǅen"));
        assertEquals("ǅ", Strings.toUpperCamelCase("Ǆ"));
        assertEquals("xǅen", Strings.toCamelCase("x_ǆen"));
        assertEquals("xǅen", Strings.toCamelCase("x ǄEN"));
        assertEquals("xǅen", Strings.toCamelCase("x-ǆen", '-'));
        // The first word of camelCase is lowercased, never capitalized, so it is unaffected.
        assertEquals("ǆen", Strings.toCamelCase("ǄEN"));
        assertEquals("ǆenX", Strings.toCamelCase("ǄEN_x"));

        // ß -> "Ss" (titlecase), not "SS" (uppercase); the result grows by one code unit either way.
        assertEquals("Sseta", Strings.toUpperCamelCase("ßeta"));
        assertEquals("SsetaName", Strings.toUpperCamelCase("ßeta_name"));
        assertEquals("xSseta", Strings.toCamelCase("x_ßeta"));
        assertEquals("ßetaName", Strings.toCamelCase("ßeta_name"));
        // The first LETTER is capitalized even when it is not the first code point.
        assertEquals("2Ssx", Strings.toUpperCamelCase("2ßx"));
        assertEquals("a2Ssx", Strings.toCamelCase("a_2ßx"));
    }

    @Test
    public void testToCamelCase_AgreesWithCapitalize() {
        // Every capitalized camel-case word that starts with a letter must be byte-identical to what
        // capitalize()/capitalizeWordsFully() produce for the same word: the Lt digraphs, the one-to-many
        // titlecase mappings, and ordinary letters. (Words starting with a digit differ by design: camel case
        // capitalizes the first LETTER, "2beta" -> "2Beta", while capitalize() maps the first code point and
        // leaves "2beta" alone; that case is covered by camelCase_capitalizesWithTitlecaseNotUppercase.)
        final String[] words = { "ǆen", "ǄEN", "ǅen", "ǉax", "ǌax", "ǳord", "ßeta", "ŉame", "ᾀx", "ևx", "ﬁle", "İstanbul", "ΣΣ", "été", "straße", "𐐨eseret",
                "hello", "WORLD", "x" };

        for (final String w : words) {
            final String expected = Strings.capitalize(w.toLowerCase(java.util.Locale.ROOT));
            assertEquals(expected, Strings.toUpperCamelCase(w), w);
            assertEquals(Strings.capitalizeWordsFully(w), Strings.toUpperCamelCase(w), w);
            assertEquals("a" + expected, Strings.toCamelCase("a_" + w), w);
            assertEquals("a" + expected, Strings.toCamelCase("a " + w), w);
        }
    }

    @Test
    public void testToCamelCase_AsciiAndUnicodePathsAgree() {
        // Forcing the Unicode path with one non-ASCII character must not change how ASCII words are capitalized.
        assertEquals("vérsion2Beta", Strings.toCamelCase("vérsion_2beta"));
        assertEquals("É2Beta", Strings.toUpperCamelCase("é_2beta"));
        assertEquals("helloWörld", Strings.toCamelCase("hello_wörld"));
        assertEquals("HelloWörld", Strings.toUpperCamelCase("HELLO_WÖRLD"));
        assertEquals("xmlParseré", Strings.toCamelCase("XMLParseré"));
        // and the ASCII fast path itself is untouched
        assertEquals("firstName", Strings.toCamelCase("FIRST_NAME"));
        assertEquals("FirstName", Strings.toUpperCamelCase("first name"));
        assertEquals("version2Beta", Strings.toCamelCase("version_2beta"));
        assertEquals("XmlParser", Strings.toUpperCamelCase("XMLParser"));
    }

    // ---------------------------------------------------------------------------------------------
    // D3 : toCodePoints(CharSequence) does not go through toString()
    // ---------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------
    // D2 : substringBetween(String, IntUnaryOperator, int) accepts an end index of 0
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testSubstringBetween_OperatorBeginEndZero() {
        // The one changed shape: end 0 with a computed begin of -1 is the empty range [0, 0).
        assertEquals("", Strings.substringBetween("abc", i -> -1, 0));
        assertEquals("", Strings.substringBetween("", i -> -1, 0));
        assertEquals(Optional.of(""), StrUtil.substringBetween("abc", i -> -1, 0));

        // Parity with the plain-index form for every begin at end 0.
        for (int begin = -3; begin <= 3; begin++) {
            final int b = begin;
            assertEquals(Strings.substringBetween("abc", b, 0), Strings.substringBetween("abc", i -> b, 0), "begin=" + b);
            assertEquals(Strings.substringBetween("", b, 0), Strings.substringBetween("", i -> b, 0), "begin=" + b + " on empty");
        }

        // The operator really is consulted at end 0 (it was skipped before), and it receives the clamped end 0.
        final int[] seen = { Integer.MIN_VALUE };
        assertEquals("", Strings.substringBetween("abc", i -> {
            seen[0] = i;
            return -1;
        }, 0));
        assertEquals(0, seen[0]);
    }

    @Test
    public void testSubstringBetween_OperatorBeginInvalid() {
        // Any computed begin other than -1 cannot precede an end of 0.
        assertNull(Strings.substringBetween("abc", i -> 0, 0));
        assertNull(Strings.substringBetween("abc", i -> 1, 0));
        assertNull(Strings.substringBetween("abc", i -> -2, 0));
        // A negative end is still rejected before the operator runs.
        final int[] calls = { 0 };
        assertNull(Strings.substringBetween("abc", i -> {
            calls[0]++;
            return -1;
        }, -1));
        assertEquals(0, calls[0]);
        assertNull(Strings.substringBetween(null, i -> -1, 0));
        assertEquals(Optional.empty(), StrUtil.substringBetween("abc", i -> 0, 0));
        // The operator is still validated first, even for a negative end.
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween("abc", (IntUnaryOperator) null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (IntUnaryOperator) null, 0));
    }

    @Test
    public void testSubstringBetween_OperatorBeginPositiveEnds() {
        // Behaviour for a positive end is untouched by the guard change.
        assertEquals("Hello", Strings.substringBetween("Hello World", i -> -1, 5));
        assertEquals("World", Strings.substringBetween("Hello World", i -> i - 6, 11));
        assertEquals("", Strings.substringBetween("Hello", i -> 2, 3));
        assertEquals("lo", Strings.substringBetween("Hello", i -> i - 3, 100));
        assertNull(Strings.substringBetween("Hello", i -> 5, 3));
        assertNull(Strings.substringBetween("Hello", i -> -2, 3));
    }
}
