package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for EscapeFilename. */
@Tag("slow-test")
public class EscapeFilenamePerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G025 end ----
    // ---- perf review 2026-10-08 G026 begin ----
    @Nested
    class G026 {

        private static final char BACKSLASH = (char) 92;

        private static final String NUL_MESSAGE = "Null byte present in file/path name. There are no "
                + "known legitimate use cases for such data, but several injection attacks may use it";

        /**
         * Builds a String from UTF-16 code units (keeps this source ASCII-only).
         */
        private String chars(final int... codeUnits) {
            final StringBuilder sb = new StringBuilder(codeUnits.length);

            for (final int codeUnit : codeUnits) {
                sb.append((char) codeUnit);
            }

            return sb.toString();
        }

        private String allChars() {
            final StringBuilder sb = new StringBuilder(Character.MAX_VALUE + 1);

            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                sb.append((char) c);
            }

            return sb.toString();
        }

        /**
         * The inline control-char table of EscapeUtil.ESCAPE_XML10.
         */
        private String[][] xml10ControlTable() {
            final List<String[]> rows = new ArrayList<>();

            for (int c = 0; c <= 0x1f; c++) {
                if (c != 0x9 && c != 0xa && c != 0xd) {
                    rows.add(new String[] { chars(c), "" });
                }
            }

            rows.add(new String[] { chars(0xfffe), "" });
            rows.add(new String[] { chars(0xffff), "" });

            return rows.toArray(new String[0][]);
        }

        /**
         * The inline control-char table of EscapeUtil.ESCAPE_XML11.
         */
        private String[][] xml11ControlTable() {
            return new String[][] { { chars(0), "" }, { chars(0xb), "&#11;" }, { chars(0xc), "&#12;" }, { chars(0xfffe), "" }, { chars(0xffff), "" } };
        }

        private String[][] javaQuoteTable() {
            return new String[][] { { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH } };
        }

        private String[][] ecmaQuoteTable() {
            return new String[][] { { "'", BACKSLASH + "'" }, { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH },
                    { "/", BACKSLASH + "/" } };
        }

        private String[][] jsonQuoteTable() {
            return new String[][] { { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH }, { "/", BACKSLASH + "/" } };
        }

        /**
         * The plain greedy longest-match lookup, without the prefix/suffix pre-filters and without the one-char fast path.
         */
        private final class ReferenceLookup extends EscapeUtil.CharSequenceTranslator {
            private final Map<String, String> map = new HashMap<>();
            private int shortest = Integer.MAX_VALUE;
            private int longest = 0;

            ReferenceLookup(final String[][] table) {
                for (final String[] row : table) {
                    map.put(row[0], row[1]);
                    shortest = Math.min(shortest, row[0].length());
                    longest = Math.max(longest, row[0].length());
                }
            }

            @Override
            public int translate(final CharSequence input, final int index, final java.io.Writer out) throws java.io.IOException {
                for (int i = Math.min(longest, input.length() - index); i >= shortest; i--) {
                    final String result = map.get(input.subSequence(index, index + i).toString());

                    if (result != null) {
                        out.write(result);
                        return Character.codePointCount(input, index, index + i);
                    }
                }

                return 0;
            }
        }

        private String randomText(final Random random, final char[] alphabet, final int maxLength) {
            final char[] text = new char[random.nextInt(maxLength + 1)];

            for (int i = 0; i < text.length; i++) {
                text[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(text);
        }

        private void assertSameTranslation(final EscapeUtil.CharSequenceTranslator expected, final EscapeUtil.CharSequenceTranslator actual,
                final char[] alphabet, final long seed) throws java.io.IOException {
            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                final String one = chars(c);
                final java.io.StringWriter expectedOut = new java.io.StringWriter();
                final java.io.StringWriter actualOut = new java.io.StringWriter();

                assertEquals(expected.translate(one, 0, expectedOut), actual.translate(one, 0, actualOut), one);
                assertEquals(expectedOut.toString(), actualOut.toString(), one);
            }

            final String all = allChars();
            assertEquals(expected.translate(all), actual.translate(all));
            assertEquals(expected.translate(new StringBuilder(all)), actual.translate(new StringBuilder(all)));

            final Random random = new Random(seed);

            for (int trial = 0; trial < 2000; trial++) {
                final String text = randomText(random, alphabet, 24);
                assertEquals(expected.translate(text), actual.translate(text), text);
            }
        }

        private char[] alphabetOf(final String[][]... tables) {
            final StringBuilder sb = new StringBuilder("az 9.&;#");
            sb.append(chars(0xe9, 0x4e2d, 0xd83d, 0xde00, 0xd800, 0xdfff, 0x7f, 0x85, 0x0, 0x1f, 0x2022));

            for (final String[][] table : tables) {
                for (final String[] row : table) {
                    sb.append(row[0]);
                }
            }

            return sb.toString().toCharArray();
        }

        // G026-01: one-char-key LookupTranslator tables now find a match by binary search; every char must translate as the plain map lookup does
        @Test
        public void testLookupTranslator_singleCharTablesMatchReference() throws java.io.IOException {
            final String[][][] tables = { EscapeUtil.EntityArrays.basicEscape(), EscapeUtil.EntityArrays.aposEscape(),
                    EscapeUtil.EntityArrays.iso8859_1Escape(), EscapeUtil.EntityArrays.html40ExtendedEscape(), EscapeUtil.EntityArrays.javaCtrlCharsEscape(),
                    javaQuoteTable(), ecmaQuoteTable(), jsonQuoteTable(), xml10ControlTable(), xml11ControlTable(),
                    // duplicate key (the last row wins), lone surrogates as keys, an empty replacement
                    { { "a", "1" }, { "b", "2" }, { "a", "3" } }, { { chars(0xd800), "H" }, { chars(0xdc00), "L" }, { "x", "" } } };

            long seed = 20261008L;

            for (final String[][] table : tables) {
                assertSameTranslation(new ReferenceLookup(table), new EscapeUtil.LookupTranslator(table), alphabetOf(table), seed++);
            }

            // tables that are not one-char-only keep the general path; spot-check them against the reference too
            final String[][] mixed = { { "a", "1" }, { "ab", "2" }, { chars(0xd83d, 0xde00), "S" } };
            assertSameTranslation(new ReferenceLookup(mixed), new EscapeUtil.LookupTranslator(mixed), alphabetOf(mixed), seed);
            assertEquals("abc", new EscapeUtil.LookupTranslator().translate("abc"));
            assertEquals("abc", new EscapeUtil.LookupTranslator((CharSequence[][]) null).translate("abc"));
        }

        // G026-01: the public escape translators (all built from one-char tables) against reference compositions of the plain lookup
        @Test
        public void testEscapeTranslators_matchReferenceCompositions() throws java.io.IOException {
            final ReferenceLookup basic = new ReferenceLookup(EscapeUtil.EntityArrays.basicEscape());
            final ReferenceLookup apos = new ReferenceLookup(EscapeUtil.EntityArrays.aposEscape());
            final ReferenceLookup iso = new ReferenceLookup(EscapeUtil.EntityArrays.iso8859_1Escape());
            final ReferenceLookup html40 = new ReferenceLookup(EscapeUtil.EntityArrays.html40ExtendedEscape());
            final ReferenceLookup ctrl = new ReferenceLookup(EscapeUtil.EntityArrays.javaCtrlCharsEscape());
            final char[] alphabet = alphabetOf(EscapeUtil.EntityArrays.basicEscape(), EscapeUtil.EntityArrays.aposEscape(),
                    EscapeUtil.EntityArrays.javaCtrlCharsEscape(), ecmaQuoteTable(), xml10ControlTable(),
                    new String[][] { { chars(0xc0, 0xff, 0x3b1, 0x20ac), "" } });

            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(javaQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_JAVA, alphabet, 1L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(ecmaQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_ECMASCRIPT, alphabet, 2L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(jsonQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_JSON, alphabet, 3L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, apos, new ReferenceLookup(xml10ControlTable()),
                    EscapeUtil.NumericEntityEscaper.between(0x7f, 0x84), EscapeUtil.NumericEntityEscaper.between(0x86, 0x9f),
                    new EscapeUtil.UnicodeUnpairedSurrogateRemover()), EscapeUtil.ESCAPE_XML10, alphabet, 4L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(basic, apos, new ReferenceLookup(xml11ControlTable()), EscapeUtil.NumericEntityEscaper.between(0x1, 0x8),
                            EscapeUtil.NumericEntityEscaper.between(0xe, 0x1f), EscapeUtil.NumericEntityEscaper.between(0x7f, 0x84),
                            EscapeUtil.NumericEntityEscaper.between(0x86, 0x9f), new EscapeUtil.UnicodeUnpairedSurrogateRemover()),
                    EscapeUtil.ESCAPE_XML11, alphabet, 5L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, iso), EscapeUtil.ESCAPE_HTML3, alphabet, 6L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, iso, html40), EscapeUtil.ESCAPE_HTML4, alphabet, 7L);

            // a few literal results, and the static helpers that delegate to these translators
            assertEquals("say " + BACKSLASH + "\"hi" + BACKSLASH + "\"" + BACKSLASH + "n", EscapeUtil.escapeJava("say \"hi\"\n"));
            assertEquals("D&eacute;j&agrave; &ldquo;x&rdquo; &amp; &lt;",
                    EscapeUtil.escapeHtml4("D" + chars(0xe9) + "j" + chars(0xe0) + " " + chars(0x201c) + "x" + chars(0x201d) + " & <"));
            assertEquals("It&apos;s &quot;A&quot;", EscapeUtil.escapeXml10("It's \"A\"" + chars(0x0, 0xffff)));
            assertEquals("&#11;&#1;&#127;", EscapeUtil.escapeXml11(chars(0xb, 0x1, 0x7f, 0x0)));
            assertEquals(BACKSLASH + "'" + BACKSLASH + "/" + BACKSLASH + "t", EscapeUtil.escapeEcmaScript("'/\t"));
            assertEquals(BACKSLASH + "/" + BACKSLASH + "u00E9", EscapeUtil.escapeJson("/" + chars(0xe9)));
            assertEquals("&eacute;" + chars(0x201c), EscapeUtil.escapeHtml3(chars(0xe9, 0x201c)));
            assertNull(EscapeUtil.escapeJava(null));
            assertEquals("", EscapeUtil.escapeHtml4(""));
        }

        // G026-01: a null Writer still fails at the write of a matched one-char key, and nothing is consumed for an unmatched char
        @Test
        public void testLookupTranslator_singleCharTable_nullWriterAndIndexErrors() throws java.io.IOException {
            final EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(EscapeUtil.EntityArrays.basicEscape());

            assertThrows(NullPointerException.class, () -> translator.translate("<", 0, null));
            assertEquals(0, translator.translate("a", 0, null));
            assertThrows(IndexOutOfBoundsException.class, () -> translator.translate("<", 1, new java.io.StringWriter()));
            assertThrows(IndexOutOfBoundsException.class, () -> translator.translate("<", -1, new java.io.StringWriter()));

            final java.io.StringWriter out = new java.io.StringWriter();
            assertEquals(1, translator.translate("x<y", 1, out));
            assertEquals("&lt;", out.toString());
        }

        // G026-02: backslash-u escapes are now decoded in place; results, consumed counts and error messages are unchanged
        @Test
        public void testUnicodeUnescaper_decodesInPlaceWithSameErrors() throws java.io.IOException {
            final String u = BACKSLASH + "u";

            assertEquals("A", EscapeUtil.unescapeJava(u + "0041"));
            assertEquals("x" + chars(0xe9) + "y", EscapeUtil.unescapeJava("x" + BACKSLASH + "uu+00e9y"));
            assertEquals(chars(0xabcd, 0xabcd), EscapeUtil.unescapeJava(u + "ABCD" + u + "abcd"));
            assertEquals(chars(0xd83d, 0xde00), EscapeUtil.unescapeJson(u + "D83D" + u + "de00"));
            assertEquals("qQ", EscapeUtil.UNESCAPE_JAVA.translate(new StringBuilder("q" + u + "0051")));
            assertEquals(chars(0x4e2d, 0x6587) + " " + BACKSLASH + "\n",
                    EscapeUtil.unescapeJava(u + "4e2d" + u + "6587 " + BACKSLASH + BACKSLASH + BACKSLASH + "n"));

            final java.io.StringWriter out = new java.io.StringWriter();
            assertEquals(9, EscapeUtil.UNESCAPE_JAVA.translate("ab" + BACKSLASH + "uuu+0042cd", 2, out));
            assertEquals("B", out.toString());

            final String[][] invalid = { { "ab" + u + "12G4cd", "12G4" }, { u + "+00x1", "00x1" }, { BACKSLASH + "uuu+ 123", " 123" }, { u + "-123", "-123" },
                    { u + chars(0x660, 0x660, 0x664, 0x661), chars(0x660, 0x660, 0x664, 0x661) }, { u + "123" + chars(0xff10), "123" + chars(0xff10) } };

            for (final String[] row : invalid) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava(row[0]));
                assertEquals("Unable to parse unicode value: " + row[1], e.getMessage(), row[0]);
            }

            final IllegalArgumentException shortEscape = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("x" + u + "12"));
            assertEquals("Less than 4 hex digits in unicode value: '" + u + "12' due to end of CharSequence", shortEscape.getMessage());

            // the text before a malformed escape is already written; nothing of the escape itself is
            final java.io.StringWriter partial = new java.io.StringWriter();
            assertThrows(IllegalArgumentException.class, () -> EscapeUtil.UNESCAPE_JAVA.translate("ok" + u + "00Z0", partial));
            assertEquals("ok", partial.toString());

            // a CharSequence whose subSequence is not a String: the message text still comes from that subsequence
            final String text = "z" + u + "12x4";
            final CharSequence custom = new CharSequence() {
                @Override
                public int length() {
                    return text.length();
                }

                @Override
                public char charAt(final int index) {
                    return text.charAt(index);
                }

                @Override
                public CharSequence subSequence(final int start, final int end) {
                    return new StringBuilder(text.substring(start, end));
                }

                @Override
                public String toString() {
                    return text;
                }
            };

            final IllegalArgumentException customError = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.UNESCAPE_JAVA.translate(custom));
            assertEquals("Unable to parse unicode value: 12x4", customError.getMessage());
        }

        // G026-03: the NUL scan now uses String.indexOf; every entry point keeps its result and its IAE (type, message, which part is checked)
        @Test
        public void testFilenameUtil_nullByteScanSameOutcomes() {
            final String nul = chars(0);
            final String cjk = chars(0x4e2d, 0x6587);
            final String[] poisoned = { nul, nul + "a.txt", "a" + nul + ".txt", "dir/a.txt" + nul, "C:/dir/" + nul + "/f.txt", cjk + nul + ".txt",
                    "/" + chars(0xe9) + "/x" + nul, "~" + nul, "dir" + nul + "/a.txt" };

            for (final String path : poisoned) {
                // these scan the whole filename first
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize(path, true)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalizeNoEndSeparator(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getBaseName(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.removeExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.indexOfExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.changeExtension(path, "log")).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, "txt")).getMessage(), path);
                assertEquals(NUL_MESSAGE,
                        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, new String[] { "txt" })).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, Arrays.asList("txt"))).getMessage(),
                        path);
            }

            // these check only the part they return
            assertEquals("", FilenameUtil.getPrefix("a" + nul + ".txt"));
            assertEquals("/", FilenameUtil.getPrefix("/x" + nul));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPrefix("~" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPrefix("~ab" + nul + "/x")).getMessage());
            assertEquals("dir/", FilenameUtil.getPath("dir/a" + nul + ".txt"));
            assertEquals("dir", FilenameUtil.getPathNoEndSeparator("dir/a" + nul + ".txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPath("dir" + nul + "/a.txt")).getMessage());
            assertEquals(NUL_MESSAGE,
                    assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPathNoEndSeparator("dir" + nul + "/a.txt")).getMessage());
            assertEquals("C:/dir/", FilenameUtil.getFullPath("C:/dir/" + nul + "f.txt"));
            assertEquals("C:/dir", FilenameUtil.getFullPathNoEndSeparator("C:/dir/" + nul + "f.txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPath("C:/d" + nul + "/f.txt")).getMessage());
            assertEquals(NUL_MESSAGE,
                    assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPathNoEndSeparator("C:/d" + nul + "/f.txt")).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPathNoEndSeparator("~" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.changeExtension("a.txt", "lo" + nul + "g")).getMessage());

            // no NUL: unchanged results, Latin-1 and UTF-16 strings, empty and null
            assertEquals("txt", FilenameUtil.getExtension(cjk + "/" + cjk + ".txt"));
            assertEquals(cjk + ".txt", FilenameUtil.getName("a/" + cjk + ".txt"));
            assertEquals(cjk, FilenameUtil.getBaseName("a" + BACKSLASH + cjk + ".txt"));
            assertEquals(cjk + ".log", FilenameUtil.changeExtension(cjk + ".txt", ".log"));
            assertEquals("a/" + cjk + "/", FilenameUtil.normalize("a/./b/../" + cjk + "//", true));
            assertTrue(FilenameUtil.isExtension(cjk + ".java", "java"));
            assertTrue(FilenameUtil.isExtension(cjk, new String[0]));
            assertFalse(FilenameUtil.isExtension("x." + cjk, Arrays.asList("txt", null)));
            assertEquals(4, FilenameUtil.indexOfExtension("file.txt"));
            assertEquals(-1, FilenameUtil.indexOfExtension("a.txt/b"));
            assertEquals("", FilenameUtil.getExtension(""));
            assertEquals("", FilenameUtil.normalize(""));
            assertNull(FilenameUtil.getExtension(null));
            assertNull(FilenameUtil.normalize(null));
            assertFalse(FilenameUtil.isExtension(null, "txt"));
            assertEquals("~user/", FilenameUtil.getPrefix("~user"));

            final StringBuilder longPath = new StringBuilder();

            while (longPath.length() < 5000) {
                longPath.append("/segment").append(longPath.length() % 7);
            }

            assertEquals("txt", FilenameUtil.getExtension(longPath + "/f.txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getExtension(longPath + "/f.txt" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(cjk + longPath + nul + "/f.txt")).getMessage());
        }
    }
}
