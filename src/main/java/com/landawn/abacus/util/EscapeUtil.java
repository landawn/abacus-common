/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * Note: it's copied from StringEscaperUtils in Apache Commons Lang under Apache License 2.0
 *
 * <p>Escapes and unescapes {@code String}s for
 * Java, Java Script, HTML and XML.</p>
 *
 * <p>#ThreadSafe#</p>
 */
@SuppressWarnings({ "java:S100", "java:S3878", "UnnecessaryUnicodeEscape", "SpellCheckingInspection" })
public final class EscapeUtil {
    /**
     * {@code \u000a} linefeed LF ('\n').
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     */
    static final char LF = '\n';

    /**
     * {@code \u000d} carriage return CR ('\r').
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     */
    static final char CR = '\r';

    /* ESCAPE TRANSLATORS */

    /**
     * Translator object for escaping Java.
     *
     * While {@link #escapeJava(String)} is the expected method of use, this
     * object allows the Java escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_JAVA = new LookupTranslator(new String[][] { { "\"", "\\\"" }, { "\\", "\\\\" }, })
            .with(new LookupTranslator(BeanArrays.JAVA_CTRL_CHARS_ESCAPE()))
            .with(JavaUnicodeEscaper.outsideOf(32, 0x7f));

    /**
     * Translator object for escaping EcmaScript/JavaScript.
     *
     * While {@link #escapeEcmaScript(String)} is the expected method of use, this
     * object allows the EcmaScript escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_ECMASCRIPT = new AggregateTranslator(
            new LookupTranslator(new String[][] { { "'", "\\'" }, { "\"", "\\\"" }, { "\\", "\\\\" }, { "/", "\\/" } }),
            new LookupTranslator(BeanArrays.JAVA_CTRL_CHARS_ESCAPE()), JavaUnicodeEscaper.outsideOf(32, 0x7f));

    /**
     * Translator object for escaping JSON.
     *
     * While {@link #escapeJson(String)} is the expected method of use, this
     * object allows the JSON escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_JSON = new AggregateTranslator(
            new LookupTranslator(new String[][] { { "\"", "\\\"" }, { "\\", "\\\\" }, { "/", "\\/" } }),
            new LookupTranslator(BeanArrays.JAVA_CTRL_CHARS_ESCAPE()), JavaUnicodeEscaper.outsideOf(32, 0x7f));

    /**
     * Translator object for escaping XML 1.0.
     *
     * While {@link #escapeXml10(String)} is the expected method of use, this
     * object allows the XML escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_XML10 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_ESCAPE()),
            new LookupTranslator(BeanArrays.APOS_ESCAPE()),
            new LookupTranslator(new String[][] { { "\u0000", Strings.EMPTY }, { "\u0001", Strings.EMPTY }, { "\u0002", Strings.EMPTY },
                    { "\u0003", Strings.EMPTY }, { "\u0004", Strings.EMPTY }, { "\u0005", Strings.EMPTY }, { "\u0006", Strings.EMPTY },
                    { "\u0007", Strings.EMPTY }, { "\u0008", Strings.EMPTY }, { "\u000b", Strings.EMPTY }, { "\u000c", Strings.EMPTY },
                    { "\u000e", Strings.EMPTY }, { "\u000f", Strings.EMPTY }, { "\u0010", Strings.EMPTY }, { "\u0011", Strings.EMPTY },
                    { "\u0012", Strings.EMPTY }, { "\u0013", Strings.EMPTY }, { "\u0014", Strings.EMPTY }, { "\u0015", Strings.EMPTY },
                    { "\u0016", Strings.EMPTY }, { "\u0017", Strings.EMPTY }, { "\u0018", Strings.EMPTY }, { "\u0019", Strings.EMPTY },
                    { "\u001a", Strings.EMPTY }, { "\u001b", Strings.EMPTY }, { "\u001c", Strings.EMPTY }, { "\u001d", Strings.EMPTY },
                    { "\u001e", Strings.EMPTY }, { "\u001f", Strings.EMPTY }, { "\ufffe", Strings.EMPTY }, { "\uffff", Strings.EMPTY } }),
            NumericBeanEscaper.between(0x7f, 0x84), NumericBeanEscaper.between(0x86, 0x9f), new UnicodeUnpairedSurrogateRemover());

    /**
     * Translator object for escaping XML 1.1.
     *
     * While {@link #escapeXml11(String)} is the expected method of use, this
     * object allows the XML escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_XML11 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_ESCAPE()),
            new LookupTranslator(BeanArrays.APOS_ESCAPE()),
            new LookupTranslator(new String[][] { { "\u0000", Strings.EMPTY }, { "\u000b", "&#11;" }, { "\u000c", "&#12;" }, { "\ufffe", Strings.EMPTY },
                    { "\uffff", Strings.EMPTY } }),
            NumericBeanEscaper.between(0x1, 0x8), NumericBeanEscaper.between(0xe, 0x1f), NumericBeanEscaper.between(0x7f, 0x84),
            NumericBeanEscaper.between(0x86, 0x9f), new UnicodeUnpairedSurrogateRemover());

    /**
     * Translator object for escaping HTML version 3.0.
     *
     * While {@link #escapeHtml3(String)} is the expected method of use, this
     * object allows the HTML escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_HTML3 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_ESCAPE()),
            new LookupTranslator(BeanArrays.ISO8859_1_ESCAPE()));

    /**
     * Translator object for escaping HTML version 4.0.
     *
     * While {@link #escapeHtml4(String)} is the expected method of use, this
     * object allows the HTML escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_HTML4 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_ESCAPE()),
            new LookupTranslator(BeanArrays.ISO8859_1_ESCAPE()), new LookupTranslator(BeanArrays.HTML40_EXTENDED_ESCAPE()));

    /**
     * Translator object for escaping individual Comma Separated Values.
     *
     * While {@link #escapeCsv(String)} is the expected method of use, this
     * object allows the CSV escaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator ESCAPE_CSV = new CsvEscaper();

    /* UNESCAPE TRANSLATORS */

    /**
     * Translator object for unescaping escaped Java.
     *
     * While {@link #unescapeJava(String)} is the expected method of use, this
     * object allows the Java unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    // TODO: throw "illegal character: \92" as an Exception if a \ on the end of the Java (as per the compiler)?
    public static final CharSequenceTranslator UNESCAPE_JAVA = new AggregateTranslator(new OctalUnescaper(), // .between('\1', '\377'),
            new UnicodeUnescaper(), new LookupTranslator(BeanArrays.JAVA_CTRL_CHARS_UNESCAPE()),
            new LookupTranslator(new String[][] { { "\\\\", "\\" }, { "\\\"", "\"" }, { "\\'", "'" }, { "\\", "" } }));

    /**
     * Translator object for unescaping escaped EcmaScript.
     *
     * While {@link #unescapeEcmaScript(String)} is the expected method of use, this
     * object allows the EcmaScript unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_ECMASCRIPT = UNESCAPE_JAVA;

    /**
     * Translator object for unescaping escaped JSON.
     *
     * While {@link #unescapeJson(String)} is the expected method of use, this
     * object allows the JSON unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_JSON = UNESCAPE_JAVA;

    /**
     * Translator object for unescaping escaped HTML 3.0.
     *
     * While {@link #unescapeHtml3(String)} is the expected method of use, this
     * object allows the HTML unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_HTML3 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_UNESCAPE()),
            new LookupTranslator(BeanArrays.ISO8859_1_UNESCAPE()), new NumericBeanUnescaper());

    /**
     * Translator object for unescaping escaped HTML 4.0.
     *
     * While {@link #unescapeHtml4(String)} is the expected method of use, this
     * object allows the HTML unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_HTML4 = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_UNESCAPE()),
            new LookupTranslator(BeanArrays.ISO8859_1_UNESCAPE()), new LookupTranslator(BeanArrays.HTML40_EXTENDED_UNESCAPE()), new NumericBeanUnescaper());

    /**
     * Translator object for unescaping escaped XML.
     *
     * While {@link #unescapeXml(String)} is the expected method of use, this
     * object allows the XML unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_XML = new AggregateTranslator(new LookupTranslator(BeanArrays.BASIC_UNESCAPE()),
            new LookupTranslator(BeanArrays.APOS_UNESCAPE()), new NumericBeanUnescaper());

    /**
     * Translator object for unescaping escaped Comma Separated Value entries.
     *
     * While {@link #unescapeCsv(String)} is the expected method of use, this
     * object allows the CSV unescaping functionality to be used
     * as the foundation for a custom translator.
     *
     */
    public static final CharSequenceTranslator UNESCAPE_CSV = new CsvUnescaper();

    /* Helper functions */

    /**
     * <p>{@code StringEscapeUtils} instances should NOT be constructed in
     * standard programming.</p>
     *
     * <p>Instead, the class should be used as:</p>
     * <pre>StringEscapeUtils.escapeJava("foo");</pre>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    private EscapeUtil() {
        // singleton.
    }

    // Java and JavaScript
    //--------------------------------------------------------------------------

    /**
     * <p>Escapes the characters in a {@code String} using Java String rules.</p>
     *
     * <p>Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.) </p>
     *
     * <p>So a tab becomes the characters {@code '\\'} and
     * {@code 't'}.</p>
     *
     * <p>The only difference between Java strings and JavaScript strings
     * is that in JavaScript, a single quote and forward-slash (/) are escaped.</p>
     *
     * <p>Example:</p>
     * <pre>
     * input string: He didn't say, "Stop!"
     * output string: He didn't say, \"Stop!\"
     * </pre>
     *
     * @param input String to escape values in, which may be null
     * @return String with escaped values, {@code null} if {@code null} string input
     */
    public static String escapeJava(final String input) {
        return ESCAPE_JAVA.translate(input);
    }

    /**
     * <p>Escapes the characters in a {@code String} using EcmaScript String rules.</p>
     * <p>Escapes any values it finds into their EcmaScript String form.
     * Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.) </p>
     *
     * <p>So a tab becomes the characters {@code '\\'} and
     * {@code 't'}.</p>
     *
     * <p>The only difference between Java strings and EcmaScript strings
     * is that in EcmaScript, a single quote and forward-slash (/) are escaped.</p>
     *
     * <p>Note that EcmaScript is best known by the JavaScript and ActionScript dialects. </p>
     *
     * <p>Example:</p>
     * <pre>
     * input string: He didn't say, "Stop!"
     * output string: He didn\'t say, \"Stop!\"
     * </pre>
     *
     * @param input String to escape values in, which may be null
     * @return String with escaped values, {@code null} if {@code null} string input
     *
     */
    public static String escapeEcmaScript(final String input) {
        return ESCAPE_ECMASCRIPT.translate(input);
    }

    /**
     * <p>Escapes the characters in a {@code String} using JSON String rules.</p>
     * <p>Escapes any values it finds into their JSON String form.
     * Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.) </p>
     *
     * <p>So a tab becomes the characters {@code '\\'} and
     * {@code 't'}.</p>
     *
     * <p>The only difference between Java strings and JSON strings
     * is that in JSON, forward-slash (/) is escaped.</p>
     *
     * <p>See <a href="http://www.ietf.org/rfc/rfc4627.txt">rfc4627</a> for further details. </p>
     *
     * <p>Example:</p>
     * <pre>
     * input string: He didn't say, "Stop!"
     * output string: He didn't say, \"Stop!\"
     * </pre>
     *
     * @param input String to escape values in, which may be null
     * @return String with escaped values, {@code null} if {@code null} string input
     *
     */
    public static String escapeJson(final String input) {
        return ESCAPE_JSON.translate(input);
    }

    /**
     * <p>Unescapes any Java literals found in the {@code String}.
     * For example, it will turn a sequence of {@code '\'} and
     * {@code 'n'} into a newline character, unless the {@code '\'}
     * is preceded by another {@code '\'}.</p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return a new unescaped {@code String}, {@code null} if {@code null} string input
     */
    public static String unescapeJava(final String input) {
        return UNESCAPE_JAVA.translate(input);
    }

    /**
     * <p>Unescapes any EcmaScript literals found in the {@code String}.</p>
     *
     * <p>For example, it will turn a sequence of {@code '\'} and {@code 'n'}
     * into a newline character, unless the {@code '\'} is preceded by another
     * {@code '\'}.</p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return A new unescaped {@code String}, {@code null} if {@code null} string input
     * @see #unescapeJava(String)
     */
    public static String unescapeEcmaScript(final String input) {
        return UNESCAPE_ECMASCRIPT.translate(input);
    }

    /**
     * <p>Unescapes any JSON literals found in the {@code String}.</p>
     *
     * <p>For example, it will turn a sequence of {@code '\'} and {@code 'n'}
     * into a newline character, unless the {@code '\'} is preceded by another
     * {@code '\'}.</p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return A new unescaped {@code String}, {@code null} if {@code null} string input
     * @see #unescapeJava(String)
     */
    public static String unescapeJson(final String input) {
        return UNESCAPE_JSON.translate(input);
    }

    // HTML and XML
    //--------------------------------------------------------------------------

    /**
     * <p>Escapes the characters in a {@code String} using HTML entities.</p>
     *
     * <p>
     * For example:
     * </p>
     * <p><code>"bread" &amp; "butter"</code></p>
     * becomes:
     * <p>
     * <code>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</code>.
     * </p>
     *
     * <p>Supports all known HTML 4.0 entities, including funky accents.
     * Note that the commonly used apostrophe escape character (&amp;apos;)
     * is not a legal bean and so is not supported). </p>
     *
     * @param input the {@code String} to escape, which may be null
     * @return a new escaped {@code String}, {@code null} if {@code null} string input
     *
     * @see <a href="http://hotwired.lycos.com/webmonkey/reference/special_characters/">ISO Entities</a>
     * @see <a href="http://www.w3.org/TR/REC-html32#latin1">HTML 3.2 Character Entities for ISO Latin-1</a>
     * @see <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">HTML 4.0 Character bean references</a>
     * @see <a href="http://www.w3.org/TR/html401/charset.html#h-5.3">HTML 4.01 Character References</a>
     * @see <a href="http://www.w3.org/TR/html401/charset.html#code-position">HTML 4.01 Code positions</a>
     *
     */
    public static String escapeHtml4(final String input) {
        return ESCAPE_HTML4.translate(input);
    }

    /**
     * <p>Escapes the characters in a {@code String} using HTML entities.</p>
     * <p>Supports only the HTML 3.0 entities. </p>
     *
     * @param input the {@code String} to escape, which may be null
     * @return a new escaped {@code String}, {@code null} if {@code null} string input
     *
     */
    public static String escapeHtml3(final String input) {
        return ESCAPE_HTML3.translate(input);
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Unescapes a string containing bean escapes to a string
     * containing the actual Unicode characters corresponding to the
     * escapes. Supports HTML 4.0 entities.</p>
     *
     * <p>For example, the string {@code "&lt;Fran&ccedil;ais&gt;"}
     * will become {@code "<Français>"}</p>
     *
     * <p>If a bean is unrecognized, it is left alone, and inserted
     * verbatim into the result string. e.g., {@code "&gt;&zzzz;x"} will
     * become {@code ">&zzzz;x"}.</p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return a new unescaped {@code String}, {@code null} if {@code null} string input
     *
     */
    public static String unescapeHtml4(final String input) {
        return UNESCAPE_HTML4.translate(input);
    }

    /**
     * <p>Unescapes a string containing bean escapes to a string
     * containing the actual Unicode characters corresponding to the
     * escapes. Supports only HTML 3.0 entities.</p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return a new unescaped {@code String}, {@code null} if {@code null} string input
     *
     */
    public static String unescapeHtml3(final String input) {
        return UNESCAPE_HTML3.translate(input);
    }

    /**
     * <p>Escapes the characters in a {@code String} using XML entities.</p>
     *
     * <p>For example: {@code "bread" & "butter"} =&gt;
     * {@code &quot;bread&quot; &amp; &quot;butter&quot;}.
     * </p>
     *
     * <p>Note that XML 1.0 is a text-only format: it cannot represent control
     * characters or unpaired Unicode surrogate codepoints, even after escaping.
     * {@code escapeXml10} will remove characters that do not fit in the
     * following ranges:</p>
     *
     * <p>{@code #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]}</p>
     *
     * <p>Though not strictly necessary, {@code escapeXml10} will escape
     * characters in the following ranges:</p>
     *
     * <p>{@code [#x7F-#x84] | [#x86-#x9F]}</p>
     *
     * <p>The returned string can be inserted into a valid XML 1.0 or XML 1.1
     * document. If you want to allow more non-text characters in an XML 1.1
     * document, use {@link #escapeXml11(String)}.</p>
     *
     * @param input the {@code String} to escape, which may be null
     * @return a new escaped {@code String}, {@code null} if {@code null} string input
     * @see #unescapeXml(java.lang.String)
     */
    public static String escapeXml10(final String input) {
        return ESCAPE_XML10.translate(input);
    }

    /**
     * <p>Escapes the characters in a {@code String} using XML entities.</p>
     *
     * <p>For example: {@code "bread" & "butter"} =&gt;
     * {@code &quot;bread&quot; &amp; &quot;butter&quot;}.
     * </p>
     *
     * <p>XML 1.1 can represent certain control characters, but it cannot represent
     * the {@code null} byte or unpaired Unicode surrogate codepoints, even after escaping.
     * {@code escapeXml11} will remove characters that do not fit in the following
     * ranges:</p>
     *
     * <p>{@code [#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]}</p>
     *
     * <p>{@code escapeXml11} will escape characters in the following ranges:</p>
     *
     * <p>{@code [#x1-#x8] | [#xB-#xC] | [#xE-#x1F] | [#x7F-#x84] | [#x86-#x9F]}</p>
     *
     * <p>The returned string can be inserted into a valid XML 1.1 document. Do not
     * use it for XML 1.0 documents.</p>
     *
     * @param input the {@code String} to escape, which may be null
     * @return a new escaped {@code String}, {@code null} if {@code null} string input
     * @see #unescapeXml(java.lang.String)
     */
    public static String escapeXml11(final String input) {
        return ESCAPE_XML11.translate(input);
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Unescapes a string containing XML bean escapes to a string
     * containing the actual Unicode characters corresponding to the
     * escapes.</p>
     *
     * <p>Supports only the five basic XML entities (gt, lt, quot, amp, apos).
     * Does not support DTDs or external entities.</p>
     *
     * <p>Note that numerical \\u Unicode codes are unescaped to their respective
     *    Unicode characters. This may change in future releases. </p>
     *
     * @param input the {@code String} to unescape, which may be null
     * @return a new unescaped {@code String}, {@code null} if {@code null} string input
     * @see #escapeXml10(String)
     * @see #escapeXml11(String)
     */
    public static String unescapeXml(final String input) {
        return UNESCAPE_XML.translate(input);
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Returns a {@code String} value for a CSV column enclosed in double quotes,
     * if required.</p>
     *
     * <p>If the value contains a comma, newline or double quote, then the
     *    String value is returned enclosed in double quotes.</p>
     *
     * <p>Any double quote characters in the value are escaped with another double quote.</p>
     *
     * <p>If the value does not contain a comma, newline or double quote, then the
     *    String value is returned unchanged.</p>
     *
     * see <a href="http://en.wikipedia.org/wiki/Comma-separated_values">Wikipedia</a> and
     * <a href="http://tools.ietf.org/html/rfc4180">RFC 4180</a>.
     *
     * @param input the input CSV column String, which may be null
     * @return
     * newline or double quote, {@code null} if {@code null} string input
     */
    public static String escapeCsv(final String input) {
        return ESCAPE_CSV.translate(input);
    }

    /**
     * <p>Returns a {@code String} value for an unescaped CSV column. </p>
     *
     * <p>If the value is enclosed in double quotes, and contains a comma, newline
     *    or double quote, then quotes are removed.
     * </p>
     *
     * <p>Any double quote escaped characters (a pair of double quotes) are unescaped
     *    to just one double quote. </p>
     *
     * <p>If the value is not enclosed in double quotes, or is and does not contain a
     *    comma, newline or double quote, then the String value is returned unchanged.</p>
     *
     * see <a href="http://en.wikipedia.org/wiki/Comma-separated_values">Wikipedia</a> and
     * <a href="http://tools.ietf.org/html/rfc4180">RFC 4180</a>.
     *
     * @param input the input CSV column String, which may be null
     * @return
     * quotes unescaped, {@code null} if {@code null} string input
     */
    public static String unescapeCsv(final String input) {
        return UNESCAPE_CSV.translate(input);
    }

    /**
     * An API for translating text.
     * Its core use is to escape and unescape text. Because escaping and unescaping
     * is completely contextual, the API does not present two separate signatures.
     *
     */
    public abstract static class CharSequenceTranslator {

        /** The Constant HEX_DIGITS. */
        static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        /**
         * Translate a set of codepoints, represented by an int index into a CharSequence,
         * into another set of codepoints. The number of codepoints consumed must be returned,
         * and the only IOExceptions thrown must be from interacting with the Writer so that
         * the top level API may reliably ignore StringWriter IOExceptions.
         *
         * @param input CharSequence that is being translated
         * @param index int representing the current point of translation
         * @param out Writer to translate the text to
         * @return int count of codepoints consumed
         * @throws IOException if and only if the Writer produces an IOException
         */
        public abstract int translate(CharSequence input, int index, Writer out) throws IOException;

        /**
         * Helper for non-Writer usage.
         * @param input CharSequence to be translated
         * @return String output of translation
         */
        public final String translate(final CharSequence input) {
            if (input == null) {
                return null;
            }
            try {
                final StringWriter writer = new StringWriter(input.length() * 2);
                translate(input, writer);
                return writer.toString();
            } catch (final IOException ioe) {
                // this should never ever happen while writing to a StringWriter
                throw new RuntimeException(ioe);
            }
        }

        /**
         * Translate an input onto a Writer. This is intentionally final as its algorithm is
         * tightly coupled with the abstract method of this class.
         *
         * @param input CharSequence that is being translated
         * @param out Writer to translate the text to
         * @throws IOException if and only if the Writer produces an IOException
         */
        public final void translate(final CharSequence input, final Writer out) throws IOException {
            if (out == null) {
                throw new IllegalArgumentException("The Writer must not be null");
            }
            if (input == null) {
                return;
            }
            int pos = 0;
            final int len = input.length();
            while (pos < len) {
                final int consumed = translate(input, pos, out);
                if (consumed == 0) {
                    // inlined implementation of Character.toChars(Character.codePointAt(input, pos))
                    // avoids allocating temp char arrays and duplicate checks
                    final char c1 = input.charAt(pos);
                    out.write(c1);
                    pos++;
                    if (Character.isHighSurrogate(c1) && pos < len) {
                        final char c2 = input.charAt(pos);
                        if (Character.isLowSurrogate(c2)) {
                            out.write(c2);
                            pos++;
                        }
                    }
                    continue;
                }
                // contract with translators is that they have to understand codepoints, and they just took care of a surrogate pair
                for (int pt = 0; pt < consumed; pt++) {
                    pos += Character.charCount(Character.codePointAt(input, pos));
                }
            }
        }

        /**
         * Helper method to create a merger of this translator with another set of
         * translators. Useful in customizing the standard functionality.
         *
         * @param translators CharSequenceTranslator array of translators to merge with this one
         * @return CharSequenceTranslator merging this translator with the others
         */
        public final CharSequenceTranslator with(final CharSequenceTranslator... translators) {
            final CharSequenceTranslator[] newArray = new CharSequenceTranslator[translators.length + 1];
            newArray[0] = this;
            System.arraycopy(translators, 0, newArray, 1, translators.length);
            return new AggregateTranslator(newArray);
        }

        /**
         * <p>Returns an upper case hexadecimal {@code String} for the given
         * character.</p>
         *
         * @param codepoint The codepoint to convert.
         * @return An upper case hexadecimal {@code String}
         */
        public static String hex(final int codepoint) {
            return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
        }
    }

    /**
     * Executes a sequence of translators one after the other. Execution ends whenever
     * the first translator consumes codepoints from the input.
     *
     */
    static class AggregateTranslator extends CharSequenceTranslator {

        /** The translators. */
        private final CharSequenceTranslator[] translators;

        /**
         * Specify the translators to be used at creation time.
         *
         * @param translators CharSequenceTranslator array to aggregate
         */
        public AggregateTranslator(final CharSequenceTranslator... translators) {
            this.translators = N.clone(translators);
        }

        /**
         * The first translator to consume codepoints from the input is the <i>winner</i>.
         * Execution stops with the number of consumed codepoints being returned.
         * {@inheritDoc}
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            for (final CharSequenceTranslator translator : translators) {
                final int consumed = translator.translate(input, index, out);
                if (consumed != 0) {
                    return consumed;
                }
            }
            return 0;
        }
    }

    /**
     * Translates codepoints to their Unicode escaped value suitable for Java source.
     *
     */
    static class JavaUnicodeEscaper extends UnicodeEscaper {

        /**
         * <p>
         * Constructs a {@code JavaUnicodeEscaper} above the specified value (exclusive).
         * </p>
         *
         * @param codepoint
         *            above which to escape
         * @return
         */
        public static JavaUnicodeEscaper above(final int codepoint) {
            return outsideOf(0, codepoint);
        }

        /**
         * <p>
         * Constructs a {@code JavaUnicodeEscaper} below the specified value (exclusive).
         * </p>
         *
         * @param codepoint
         *            below which to escape
         * @return
         */
        public static JavaUnicodeEscaper below(final int codepoint) {
            return outsideOf(codepoint, Integer.MAX_VALUE);
        }

        /**
         * <p>
         * Constructs a {@code JavaUnicodeEscaper} between the specified values (inclusive).
         * </p>
         *
         * @param codepointLow
         *            above which to escape
         * @param codepointHigh
         *            below which to escape
         * @return
         */
        public static JavaUnicodeEscaper between(final int codepointLow, final int codepointHigh) {
            return new JavaUnicodeEscaper(codepointLow, codepointHigh, true);
        }

        /**
         * <p>
         * Constructs a {@code JavaUnicodeEscaper} outside the specified values (exclusive).
         * </p>
         *
         * @param codepointLow
         *            below which to escape
         * @param codepointHigh
         *            above which to escape
         * @return
         */
        public static JavaUnicodeEscaper outsideOf(final int codepointLow, final int codepointHigh) {
            return new JavaUnicodeEscaper(codepointLow, codepointHigh, false);
        }

        /**
         * <p>
         * Constructs a {@code JavaUnicodeEscaper} for the specified range. This is the underlying method for the
         * other constructors/builders. The {@code below} and {@code above} boundaries are inclusive when
         * {@code between} is {@code true} and exclusive when it is {@code false}.
         * </p>
         *
         * @param below
         *            int value representing the lowest codepoint boundary
         * @param above
         *            int value representing the highest codepoint boundary
         * @param between
         *            whether to escape between the boundaries or outside them
         */
        public JavaUnicodeEscaper(final int below, final int above, final boolean between) {
            super(below, above, between);
        }

        /**
         * Converts the given codepoint to a hex string of the form {@code "\\uXXXX\\uXXXX"}.
         *
         * @param codepoint a Unicode code point
         * @return
         */
        @Override
        protected String toUtf16Escape(final int codepoint) {
            final char[] surrogatePair = Character.toChars(codepoint);
            return "\\u" + hex(surrogatePair[0]) + "\\u" + hex(surrogatePair[1]);
        }
    }

    /**
     * Translates codepoints to their XML numeric bean escaped value.
     *
     */
    static class NumericBeanEscaper extends CodePointTranslator {

        /** The below. */
        private final int below;

        /** The above. */
        private final int above;

        /** The between. */
        private final boolean between;

        /**
         * <p>Constructs a {@code NumericBeanEscaper} for the specified range. This is
         * the underlying method for the other constructors/builders. The {@code below}
         * and {@code above} boundaries are inclusive when {@code between} is
         * {@code true} and exclusive when it is {@code false}. </p>
         *
         * @param below int value representing the lowest codepoint boundary
         * @param above int value representing the highest codepoint boundary
         * @param between whether to escape between the boundaries or outside them
         */
        private NumericBeanEscaper(final int below, final int above, final boolean between) {
            this.below = below;
            this.above = above;
            this.between = between;
        }

        /**
         * <p>Constructs a {@code NumericBeanEscaper} for all characters. </p>
         */
        public NumericBeanEscaper() {
            this(0, Integer.MAX_VALUE, true);
        }

        /**
         * <p>Constructs a {@code NumericBeanEscaper} below the specified value (exclusive). </p>
         *
         * @param codepoint below which to escape
         * @return
         */
        public static NumericBeanEscaper below(final int codepoint) {
            return outsideOf(codepoint, Integer.MAX_VALUE);
        }

        /**
         * <p>Constructs a {@code NumericBeanEscaper} above the specified value (exclusive). </p>
         *
         * @param codepoint above which to escape
         * @return
         */
        public static NumericBeanEscaper above(final int codepoint) {
            return outsideOf(0, codepoint);
        }

        /**
         * <p>Constructs a {@code NumericBeanEscaper} between the specified values (inclusive). </p>
         *
         * @param codepointLow above which to escape
         * @param codepointHigh below which to escape
         * @return
         */
        public static NumericBeanEscaper between(final int codepointLow, final int codepointHigh) {
            return new NumericBeanEscaper(codepointLow, codepointHigh, true);
        }

        /**
         * <p>Constructs a {@code NumericBeanEscaper} outside the specified values (exclusive). </p>
         *
         * @param codepointLow below which to escape
         * @param codepointHigh above which to escape
         * @return
         */
        public static NumericBeanEscaper outsideOf(final int codepointLow, final int codepointHigh) {
            return new NumericBeanEscaper(codepointLow, codepointHigh, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean translate(final int codepoint, final Writer out) throws IOException {
            if (between) {
                if (codepoint < below || codepoint > above) {
                    return false;
                }
            } else {
                if (codepoint >= below && codepoint <= above) {
                    return false;
                }
            }

            out.write("&#");
            out.write(Integer.toString(codepoint, 10));
            out.write(';');
            return true;
        }
    }

    /**
     * Helper subclass to CharSequenceTranslator to remove unpaired surrogates.
     */
    static class UnicodeUnpairedSurrogateRemover extends CodePointTranslator {
        /**
         * Implementation of translate that throws out unpaired surrogates.
         * {@inheritDoc}
         */
        @Override
        public boolean translate(final int codepoint, final Writer out) {
            // It's a surrogate. Write nothing and say we've translated.
            return codepoint >= Character.MIN_SURROGATE && codepoint <= Character.MAX_SURROGATE;
        }
    }

    /**
     * Translates escaped Unicode values of the form {@code \\u+\d\d\d\d} back to Unicode.
     * It supports multiple {@code 'u'} characters and will work with or without the {@code +}.
     *
     */
    static class UnicodeUnescaper extends CharSequenceTranslator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            if (input.charAt(index) == '\\' && index + 1 < input.length() && input.charAt(index + 1) == 'u') {
                // consume optional additional 'u' chars
                int i = 2;
                while (index + i < input.length() && input.charAt(index + i) == 'u') {
                    i++;
                }

                if (index + i < input.length() && input.charAt(index + i) == '+') {
                    i++;
                }

                if (index + i + 4 <= input.length()) {
                    // Get 4 hex digits
                    final CharSequence unicode = input.subSequence(index + i, index + i + 4);

                    try {
                        final int value = Integer.parseInt(unicode.toString(), 16);
                        out.write((char) value);
                    } catch (final NumberFormatException nfe) {
                        throw new IllegalArgumentException("Unable to parse unicode value: " + unicode, nfe);
                    }
                    return i + 4;
                }
                throw new IllegalArgumentException(
                        "Less than 4 hex digits in unicode value: '" + input.subSequence(index, input.length()) + "' due to end of CharSequence");
            }
            return 0;
        }
    }

    /**
     * Translates codepoints to their Unicode escaped value.
     *
     */
    static class UnicodeEscaper extends CodePointTranslator {

        /** The below. */
        private final int below;

        /** The above. */
        private final int above;

        /** The between. */
        private final boolean between;

        /**
         * <p>Constructs a {@code UnicodeEscaper} for all characters. </p>
         */
        public UnicodeEscaper() {
            this(0, Integer.MAX_VALUE, true);
        }

        /**
         * <p>Constructs a {@code UnicodeEscaper} for the specified range. This is
         * the underlying method for the other constructors/builders. The {@code below}
         * and {@code above} boundaries are inclusive when {@code between} is
         * {@code true} and exclusive when it is {@code false}. </p>
         *
         * @param below int value representing the lowest codepoint boundary
         * @param above int value representing the highest codepoint boundary
         * @param between whether to escape between the boundaries or outside them
         */
        protected UnicodeEscaper(final int below, final int above, final boolean between) {
            this.below = below;
            this.above = above;
            this.between = between;
        }

        /**
         * <p>Constructs a {@code UnicodeEscaper} below the specified value (exclusive). </p>
         *
         * @param codepoint below which to escape
         * @return
         */
        public static UnicodeEscaper below(final int codepoint) {
            return outsideOf(codepoint, Integer.MAX_VALUE);
        }

        /**
         * <p>Constructs a {@code UnicodeEscaper} above the specified value (exclusive). </p>
         *
         * @param codepoint above which to escape
         * @return
         */
        public static UnicodeEscaper above(final int codepoint) {
            return outsideOf(0, codepoint);
        }

        /**
         * <p>Constructs a {@code UnicodeEscaper} outside the specified values (exclusive). </p>
         *
         * @param codepointLow below which to escape
         * @param codepointHigh above which to escape
         * @return
         */
        public static UnicodeEscaper outsideOf(final int codepointLow, final int codepointHigh) {
            return new UnicodeEscaper(codepointLow, codepointHigh, false);
        }

        /**
         * <p>Constructs a {@code UnicodeEscaper} between the specified values (inclusive). </p>
         *
         * @param codepointLow above which to escape
         * @param codepointHigh below which to escape
         * @return
         */
        public static UnicodeEscaper between(final int codepointLow, final int codepointHigh) {
            return new UnicodeEscaper(codepointLow, codepointHigh, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean translate(final int codepoint, final Writer out) throws IOException {
            if (between) {
                if (codepoint < below || codepoint > above) {
                    return false;
                }
            } else {
                if (codepoint >= below && codepoint <= above) {
                    return false;
                }
            }

            // TODO: Handle potential + sign per various Unicode escape implementations
            if (codepoint > 0xffff) {
                out.write(toUtf16Escape(codepoint));
            } else {
                out.write("\\u");
                out.write(HEX_DIGITS[(codepoint >> 12) & 15]);
                out.write(HEX_DIGITS[(codepoint >> 8) & 15]);
                out.write(HEX_DIGITS[(codepoint >> 4) & 15]);
                out.write(HEX_DIGITS[(codepoint) & 15]);
            }
            return true;
        }

        /**
         * Converts the given codepoint to a hex string of the form {@code "\\uXXXX"}.
         *
         * @param codepoint a Unicode code point
         * @return
         */
        protected String toUtf16Escape(final int codepoint) {
            return "\\u" + hex(codepoint);
        }
    }

    /**
     * Translate escaped octal Strings back to their octal values.
     *
     * For example, "\45" should go back to being the specific value (a %).
     *
     * Note that this currently only supports the viable range of octal for Java; namely
     * 1 to 377. This is because parsing Java is the main use case.
     *
     */
    static class OctalUnescaper extends CharSequenceTranslator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            final int remaining = input.length() - index - 1; // how many characters left, ignoring the first \
            final StringBuilder builder = new StringBuilder();
            if (input.charAt(index) == '\\' && remaining > 0 && isOctalDigit(input.charAt(index + 1))) {
                final int next = index + 1;
                final int next2 = index + 2;
                final int next3 = index + 3;

                // we know this is good as we checked it in the if block above
                builder.append(input.charAt(next));

                if (remaining > 1 && isOctalDigit(input.charAt(next2))) {
                    builder.append(input.charAt(next2));
                    if (remaining > 2 && isZeroToThree(input.charAt(next)) && isOctalDigit(input.charAt(next3))) {
                        builder.append(input.charAt(next3));
                    }
                }

                out.write(Integer.parseInt(builder.toString(), 8));
                return 1 + builder.length();
            }
            return 0;
        }

        /**
         * Checks if the given char is an octal digit. Octal digits are the character representations of the digits 0 to 7.
         * @param ch the char to check
         * @return {@code true} if the given char is the character representation of one of the digits from 0 to 7
         */
        private boolean isOctalDigit(final char ch) {
            return ch >= '0' && ch <= '7';
        }

        /**
         * Checks if the given char is the character representation of one of the digit from 0 to 3.
         * @param ch the char to check
         * @return {@code true} if the given char is the character representation of one of the digits from 0 to 3
         */
        private boolean isZeroToThree(final char ch) {
            return ch >= '0' && ch <= '3';
        }
    }

    /**
     * Translates XML numeric entities of the form &amp;#[xX]?\d+;? to
     * the specific codepoint.
     *
     * Note that the semicolon is optional.
     *
     */
    static class NumericBeanUnescaper extends CharSequenceTranslator {

        /**
         * The Enum OPTION.
         */
        public enum OPTION {

            /** The semicolon required. */
            semiColonRequired,
            /** The semicolon optional. */
            semiColonOptional,
            /** The error if no semicolon. */
            errorIfNoSemiColon
        }

        /** The options. */
        // TODO?: Create an OptionsSet class to hide some of the conditional logic below
        private final EnumSet<OPTION> options;

        /**
         * Create a UnicodeUnescaper.
         *
         * The constructor takes a list of options, only one type of which is currently
         * available (whether to allow, error or ignore the semicolon on the end of a
         * numeric bean to being missing).
         *
         * For example, to support numeric entities without a ';':
         *    new NumericBeanUnescaper(NumericBeanUnescaper.OPTION.semiColonOptional)
         * and to throw an IllegalArgumentException when they're missing:
         *    new NumericBeanUnescaper(NumericBeanUnescaper.OPTION.errorIfNoSemiColon)
         *
         * Note that the default behaviour is to ignore them.
         *
         * @param options to apply to this unescaper
         */
        public NumericBeanUnescaper(final OPTION... options) {
            if (options.length > 0) {
                this.options = EnumSet.copyOf(Arrays.asList(options));
            } else {
                this.options = EnumSet.copyOf(List.of(OPTION.semiColonRequired));
            }
        }

        /**
         * Whether the passed in option is currently set.
         *
         * @param option to check the state of
         * @return whether the option is set
         */
        public boolean isSet(final OPTION option) {
            return options != null && options.contains(option);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            final int seqEnd = input.length();
            // Uses -2 to ensure there is something after the &#
            if (input.charAt(index) == '&' && index < seqEnd - 2 && input.charAt(index + 1) == '#') {
                int start = index + 2;
                boolean isHex = false;

                final char firstChar = input.charAt(start);
                if (firstChar == 'x' || firstChar == 'X') {
                    start++;
                    isHex = true;

                    // Check there's more than just an x after the &#
                    if (start == seqEnd) {
                        return 0;
                    }
                }

                int end = start;
                // Note that this supports character codes without a ';' on the end
                while (end < seqEnd && (input.charAt(end) >= '0' && input.charAt(end) <= '9' || input.charAt(end) >= 'a' && input.charAt(end) <= 'f'
                        || input.charAt(end) >= 'A' && input.charAt(end) <= 'F')) {
                    end++;
                }

                final boolean semiNext = end != seqEnd && input.charAt(end) == ';';

                if (!semiNext) {
                    if (isSet(OPTION.semiColonRequired)) {
                        return 0;
                    } else if (isSet(OPTION.errorIfNoSemiColon)) {
                        throw new IllegalArgumentException("Semicolon required at end of numeric bean");
                    }
                }

                int beanValue;
                try {
                    if (isHex) {
                        beanValue = Integer.parseInt(input.subSequence(start, end).toString(), 16);
                    } else {
                        beanValue = Integer.parseInt(input.subSequence(start, end).toString(), 10);
                    }
                } catch (final NumberFormatException nfe) {
                    return 0;
                }

                if (beanValue > 0xFFFF) {
                    final char[] chrs = Character.toChars(beanValue);
                    out.write(chrs[0]);
                    out.write(chrs[1]);
                } else {
                    out.write(beanValue);
                }

                return 2 + end - start + (isHex ? 1 : 0) + (semiNext ? 1 : 0);
            }
            return 0;
        }
    }

    /**
     * Translates a value using a lookup table.
     *
     */
    static class LookupTranslator extends CharSequenceTranslator {

        /** The lookup map. */
        private final Map<String, String> lookupMap;

        /** The prefix set. */
        private final Set<Character> prefixSet;

        /** The shortest. */
        private final int shortest;

        /** The longest. */
        private final int longest;

        /**
         * Define the lookup table to be used in translation
         *
         * Note that, as of Lang 3.1, the key to the lookup table is converted to a java.lang.String.
         * This is because we need the key to support hashCode and equals(Object), allowing it to be the key for a HashMap. See LANG-882.
         *
         * @param lookup CharSequence[][] table of size [*][2]
         */
        public LookupTranslator(final CharSequence[]... lookup) {
            lookupMap = new HashMap<>();
            prefixSet = N.newHashSet();
            int _shortest = Integer.MAX_VALUE; // NOSONAR
            int _longest = 0; // NOSONAR
            if (lookup != null) {
                for (final CharSequence[] seq : lookup) {
                    lookupMap.put(seq[0].toString(), seq[1].toString());
                    prefixSet.add(seq[0].charAt(0));
                    final int sz = seq[0].length();
                    if (sz < _shortest) {
                        _shortest = sz;
                    }
                    if (sz > _longest) {
                        _longest = sz;
                    }
                }
            }
            shortest = _shortest;
            longest = _longest;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            // check if translation exists for the input at position index
            if (prefixSet.contains(input.charAt(index))) {
                int max = longest;
                if (index + longest > input.length()) {
                    max = input.length() - index;
                }
                // implement greedy algorithm by trying maximum match first
                for (int i = max; i >= shortest; i--) {
                    final CharSequence subSeq = input.subSequence(index, index + i);
                    final String result = lookupMap.get(subSeq.toString());

                    if (result != null) {
                        out.write(result);
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    /**
     * Helper subclass to CharSequenceTranslator to allow for translations that
     * will replace up to one character at a time.
     *
     */
    abstract static class CodePointTranslator extends CharSequenceTranslator {

        /**
         * Implementation of translate that maps onto the abstract translate (int, Writer) method.
         * {@inheritDoc}
         */
        @Override
        public final int translate(final CharSequence input, final int index, final Writer out) throws IOException {
            final int codepoint = Character.codePointAt(input, index);
            final boolean consumed = translate(codepoint, out);
            return consumed ? 1 : 0;
        }

        /**
         * Translate the specified codepoint into another.
         *
         * @param codepoint int character input to translate
         * @param out Writer to optionally push the translated output to
         * @return boolean whether translation occurred or not
         * @throws IOException if and only if the Writer produces an IOException
         */
        public abstract boolean translate(int codepoint, Writer out) throws IOException;

    }

    // TODO: Create a parent class - 'SinglePassTranslator' ?
    //       It would handle the index checking + length returning,

    /**
     * The Class CsvEscaper.
     */
    //       and could also have an optimization check method.
    static class CsvEscaper extends CharSequenceTranslator {

        /** The Constant CSV_DELIMITER. */
        private static final char CSV_DELIMITER = ',';

        /** The Constant CSV_QUOTE. */
        private static final char CSV_QUOTE = '"';

        /** The Constant CSV_QUOTE_STR. */
        private static final String CSV_QUOTE_STR = N.stringOf(CSV_QUOTE);

        /** The Constant CSV_SEARCH_CHARS. */
        private static final char[] CSV_SEARCH_CHARS = { CSV_DELIMITER, CSV_QUOTE, CR, LF };

        /**
         *
         * @param input
         * @param index
         * @param out
         * @return
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {

            if (index != 0) {
                throw new IllegalStateException("CsvEscaper should never reach the [1] index");
            }

            if (Strings.containsNone(input.toString(), CSV_SEARCH_CHARS)) {
                out.write(input.toString());
            } else {
                out.write(CSV_QUOTE);
                out.write(Strings.replaceAll(input.toString(), CSV_QUOTE_STR, CSV_QUOTE_STR + CSV_QUOTE_STR));
                out.write(CSV_QUOTE);
            }
            return Character.codePointCount(input, 0, input.length());
        }
    }

    /**
     * The Class CsvUnescaper.
     */
    static class CsvUnescaper extends CharSequenceTranslator {

        /** The Constant CSV_DELIMITER. */
        private static final char CSV_DELIMITER = ',';

        /** The Constant CSV_QUOTE. */
        private static final char CSV_QUOTE = '"';

        /** The Constant CSV_QUOTE_STR. */
        private static final String CSV_QUOTE_STR = N.stringOf(CSV_QUOTE);

        /** The Constant CSV_SEARCH_CHARS. */
        private static final char[] CSV_SEARCH_CHARS = { CSV_DELIMITER, CSV_QUOTE, CR, LF };

        /**
         *
         * @param input
         * @param index
         * @param out
         * @return
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public int translate(final CharSequence input, final int index, final Writer out) throws IOException {

            if (index != 0) {
                throw new IllegalStateException("CsvUnescaper should never reach the [1] index");
            }

            if (input.charAt(0) != CSV_QUOTE || input.charAt(input.length() - 1) != CSV_QUOTE) {
                out.write(input.toString());
                return Character.codePointCount(input, 0, input.length());
            }

            // strip quotes
            final String quoteless = input.subSequence(1, input.length() - 1).toString();

            if (Strings.containsAny(quoteless, CSV_SEARCH_CHARS)) {
                // deal with escaped quotes; i.e., ""
                out.write(Strings.replaceAll(quoteless, CSV_QUOTE_STR + CSV_QUOTE_STR, CSV_QUOTE_STR));
            } else {
                out.write(input.toString());
            }
            return Character.codePointCount(input, 0, input.length());
        }
    }

    /**
     * Class holding various bean data for HTML and XML - generally for use with
     * the LookupTranslator.
     * All arrays are of length [*][2].
     *
     */
    static class BeanArrays {
        private BeanArrays() {

        }

        /**
         * Mapping to escape <a href="https://secure.wikimedia.org/wikipedia/en/wiki/ISO/IEC_8859-1">ISO-8859-1</a>
         * characters to their named HTML 3.x equivalents.
         * @return
         */
        public static String[][] ISO8859_1_ESCAPE() {
            return ISO8859_1_ESCAPE.clone();
        }

        /** The Constant ISO8859_1_ESCAPE. */
        private static final String[][] ISO8859_1_ESCAPE = { { "\u00A0", "&nbsp;" }, // non-breaking space
                { "\u00A1", "&iexcl;" }, // inverted exclamation mark
                { "\u00A2", "&cent;" }, // cent sign
                { "\u00A3", "&pound;" }, // pound sign
                { "\u00A4", "&curren;" }, // currency sign
                { "\u00A5", "&yen;" }, // yen sign = yuan sign
                { "\u00A6", "&brvbar;" }, // broken bar = broken vertical bar
                { "\u00A7", "&sect;" }, // section sign
                { "\u00A8", "&uml;" }, // diaeresis = spacing diaeresis
                { "\u00A9", "&copy;" }, // © - copyright sign
                { "\u00AA", "&ordf;" }, // feminine ordinal indicator
                { "\u00AB", "&laquo;" }, // left-pointing double angle quotation mark = left pointing guillemet
                { "\u00AC", "&not;" }, // not sign
                { "\u00AD", "&shy;" }, // soft hyphen = discretionary hyphen
                { "\u00AE", "&reg;" }, // ® - registered trademark sign
                { "\u00AF", "&macr;" }, // macron = spacing macron = overline = APL overbar
                { "\u00B0", "&deg;" }, // degree sign
                { "\u00B1", "&plusmn;" }, // plus-minus sign = plus-or-minus sign
                { "\u00B2", "&sup2;" }, // superscript two = superscript digit two = squared
                { "\u00B3", "&sup3;" }, // superscript three = superscript digit three = cubed
                { "\u00B4", "&acute;" }, // acute accent = spacing acute
                { "\u00B5", "&micro;" }, // micro sign
                { "\u00B6", "&para;" }, // pilcrow sign = paragraph sign
                { "\u00B7", "&middot;" }, // middle dot = Georgian comma = Greek middle dot
                { "\u00B8", "&cedil;" }, // cedilla = spacing cedilla
                { "\u00B9", "&sup1;" }, // superscript one = superscript digit one
                { "\u00BA", "&ordm;" }, // masculine ordinal indicator
                { "\u00BB", "&raquo;" }, // right-pointing double angle quotation mark = right pointing guillemet
                { "\u00BC", "&frac14;" }, // vulgar fraction one quarter = fraction one quarter
                { "\u00BD", "&frac12;" }, // vulgar fraction one half = fraction one half
                { "\u00BE", "&frac34;" }, // vulgar fraction three quarters = fraction three quarters
                { "\u00BF", "&iquest;" }, // inverted question mark = turned question mark
                { "\u00C0", "&Agrave;" }, //  - uppercase A, grave accent
                { "\u00C1", "&Aacute;" }, //  - uppercase A, acute accent
                { "\u00C2", "&Acirc;" }, //  - uppercase A, circumflex accent
                { "\u00C3", "&Atilde;" }, //  - uppercase A, tilde
                { "\u00C4", "&Auml;" }, //  - uppercase A, umlaut
                { "\u00C5", "&Aring;" }, //  - uppercase A, ring
                { "\u00C6", "&AElig;" }, //  - uppercase AE
                { "\u00C7", "&Ccedil;" }, // Ç - uppercase C, cedilla
                { "\u00C8", "&Egrave;" }, // È - uppercase E, grave accent
                { "\u00C9", "&Eacute;" }, // É - uppercase E, acute accent
                { "\u00CA", "&Ecirc;" }, // Ê - uppercase E, circumflex accent
                { "\u00CB", "&Euml;" }, // Ë - uppercase E, umlaut
                { "\u00CC", "&Igrave;" }, // Ì - uppercase I, grave accent
                { "\u00CD", "&Iacute;" }, //  - uppercase I, acute accent
                { "\u00CE", "&Icirc;" }, //  - uppercase I, circumflex accent
                { "\u00CF", "&Iuml;" }, //  - uppercase I, umlaut
                { "\u00D0", "&ETH;" }, //  - uppercase Eth, Icelandic
                { "\u00D1", "&Ntilde;" }, // Ñ - uppercase N, tilde
                { "\u00D2", "&Ograve;" }, // Ò - uppercase O, grave accent
                { "\u00D3", "&Oacute;" }, // Ó - uppercase O, acute accent
                { "\u00D4", "&Ocirc;" }, // Ô - uppercase O, circumflex accent
                { "\u00D5", "&Otilde;" }, // Õ - uppercase O, tilde
                { "\u00D6", "&Ouml;" }, // Ö - uppercase O, umlaut
                { "\u00D7", "&times;" }, // multiplication sign
                { "\u00D8", "&Oslash;" }, // Ø - uppercase O, slash
                { "\u00D9", "&Ugrave;" }, // Ù - uppercase U, grave accent
                { "\u00DA", "&Uacute;" }, // Ú - uppercase U, acute accent
                { "\u00DB", "&Ucirc;" }, // Û - uppercase U, circumflex accent
                { "\u00DC", "&Uuml;" }, // Ü - uppercase U, umlaut
                { "\u00DD", "&Yacute;" }, //  - uppercase Y, acute accent
                { "\u00DE", "&THORN;" }, // Þ - uppercase THORN, Icelandic
                { "\u00DF", "&szlig;" }, // ß - lowercase sharps, German
                { "\u00E0", "&agrave;" }, // à - lowercase a, grave accent
                { "\u00E1", "&aacute;" }, // á - lowercase a, acute accent
                { "\u00E2", "&acirc;" }, // â - lowercase a, circumflex accent
                { "\u00E3", "&atilde;" }, // ã - lowercase a, tilde
                { "\u00E4", "&auml;" }, // ä - lowercase a, umlaut
                { "\u00E5", "&aring;" }, // å - lowercase a, ring
                { "\u00E6", "&aelig;" }, // æ - lowercase ae
                { "\u00E7", "&ccedil;" }, // ç - lowercase c, cedilla
                { "\u00E8", "&egrave;" }, // è - lowercase e, grave accent
                { "\u00E9", "&eacute;" }, // é - lowercase e, acute accent
                { "\u00EA", "&ecirc;" }, // ê - lowercase e, circumflex accent
                { "\u00EB", "&euml;" }, // ë - lowercase e, umlaut
                { "\u00EC", "&igrave;" }, // ì - lowercase i, grave accent
                { "\u00ED", "&iacute;" }, // í - lowercase i, acute accent
                { "\u00EE", "&icirc;" }, // î - lowercase i, circumflex accent
                { "\u00EF", "&iuml;" }, // ï - lowercase i, umlaut
                { "\u00F0", "&eth;" }, // ð - lowercase eth, Icelandic
                { "\u00F1", "&ntilde;" }, // ñ - lowercase n, tilde
                { "\u00F2", "&ograve;" }, // ò - lowercase o, grave accent
                { "\u00F3", "&oacute;" }, // ó - lowercase o, acute accent
                { "\u00F4", "&ocirc;" }, // ô - lowercase o, circumflex accent
                { "\u00F5", "&otilde;" }, // õ - lowercase o, tilde
                { "\u00F6", "&ouml;" }, // ö - lowercase o, umlaut
                { "\u00F7", "&divide;" }, // division sign
                { "\u00F8", "&oslash;" }, // ø - lowercase o, slash
                { "\u00F9", "&ugrave;" }, // ù - lowercase u, grave accent
                { "\u00FA", "&uacute;" }, // ú - lowercase u, acute accent
                { "\u00FB", "&ucirc;" }, // û - lowercase u, circumflex accent
                { "\u00FC", "&uuml;" }, // ü - lowercase u, umlaut
                { "\u00FD", "&yacute;" }, // ý - lowercase y, acute accent
                { "\u00FE", "&thorn;" }, // þ - lowercase thorn, Icelandic
                { "\u00FF", "&yuml;" }, // ÿ - lowercase y, umlaut
        };

        /**
         * Reverse of {@link #ISO8859_1_ESCAPE()} for unescaping purposes.
         * @return
         */
        public static String[][] ISO8859_1_UNESCAPE() {
            return ISO8859_1_UNESCAPE.clone();
        }

        /** The Constant ISO8859_1_UNESCAPE. */
        private static final String[][] ISO8859_1_UNESCAPE = invert(ISO8859_1_ESCAPE);

        /**
         * Mapping to escape additional <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">character bean
         * references</a>. Note that this must be used with {@link #ISO8859_1_ESCAPE()} to get the full list of
         * HTML 4.0 character entities.
         * @return
         */
        public static String[][] HTML40_EXTENDED_ESCAPE() {
            return HTML40_EXTENDED_ESCAPE.clone();
        }

        /** The Constant HTML40_EXTENDED_ESCAPE. */
        private static final String[][] HTML40_EXTENDED_ESCAPE = {
                // <!-- Latin Extended-B -->
                { "\u0192", "&fnof;" }, // latin small f with hook = function= florin, U+0192 ISOtech -->
                // <!-- Greek -->
                { "\u0391", "&Alpha;" }, // greek capital letter alpha, U+0391 -->
                { "\u0392", "&Beta;" }, // greek capital letter beta, U+0392 -->
                { "\u0393", "&Gamma;" }, // greek capital letter gamma,U+0393 ISOgrk3 -->
                { "\u0394", "&Delta;" }, // greek capital letter delta,U+0394 ISOgrk3 -->
                { "\u0395", "&Epsilon;" }, // greek capital letter epsilon, U+0395 -->
                { "\u0396", "&Zeta;" }, // greek capital letter zeta, U+0396 -->
                { "\u0397", "&Eta;" }, // greek capital letter eta, U+0397 -->
                { "\u0398", "&Theta;" }, // greek capital letter theta,U+0398 ISOgrk3 -->
                { "\u0399", "&Iota;" }, // greek capital letter iota, U+0399 -->
                { "\u039A", "&Kappa;" }, // greek capital letter kappa, U+039A -->
                { "\u039B", "&Lambda;" }, // greek capital letter lambda,U+039B ISOgrk3 -->
                { "\u039C", "&Mu;" }, // greek capital letter mu, U+039C -->
                { "\u039D", "&Nu;" }, // greek capital letter nu, U+039D -->
                { "\u039E", "&Xi;" }, // greek capital letter xi, U+039E ISOgrk3 -->
                { "\u039F", "&Omicron;" }, // greek capital letter omicron, U+039F -->
                { "\u03A0", "&Pi;" }, // greek capital letter pi, U+03A0 ISOgrk3 -->
                { "\u03A1", "&Rho;" }, // greek capital letter rho, U+03A1 -->
                // <!-- there is no Sigmaf, and no U+03A2 character either -->
                { "\u03A3", "&Sigma;" }, // greek capital letter sigma,U+03A3 ISOgrk3 -->
                { "\u03A4", "&Tau;" }, // greek capital letter tau, U+03A4 -->
                { "\u03A5", "&Upsilon;" }, // greek capital letter upsilon,U+03A5 ISOgrk3 -->
                { "\u03A6", "&Phi;" }, // greek capital letter phi,U+03A6 ISOgrk3 -->
                { "\u03A7", "&Chi;" }, // greek capital letter chi, U+03A7 -->
                { "\u03A8", "&Psi;" }, // greek capital letter psi,U+03A8 ISOgrk3 -->
                { "\u03A9", "&Omega;" }, // greek capital letter omega,U+03A9 ISOgrk3 -->
                { "\u03B1", "&alpha;" }, // greek small letter alpha,U+03B1 ISOgrk3 -->
                { "\u03B2", "&beta;" }, // greek small letter beta, U+03B2 ISOgrk3 -->
                { "\u03B3", "&gamma;" }, // greek small letter gamma,U+03B3 ISOgrk3 -->
                { "\u03B4", "&delta;" }, // greek small letter delta,U+03B4 ISOgrk3 -->
                { "\u03B5", "&epsilon;" }, // greek small letter epsilon,U+03B5 ISOgrk3 -->
                { "\u03B6", "&zeta;" }, // greek small letter zeta, U+03B6 ISOgrk3 -->
                { "\u03B7", "&eta;" }, // greek small letter eta, U+03B7 ISOgrk3 -->
                { "\u03B8", "&theta;" }, // greek small letter theta,U+03B8 ISOgrk3 -->
                { "\u03B9", "&iota;" }, // greek small letter iota, U+03B9 ISOgrk3 -->
                { "\u03BA", "&kappa;" }, // greek small letter kappa,U+03BA ISOgrk3 -->
                { "\u03BB", "&lambda;" }, // greek small letter lambda,U+03BB ISOgrk3 -->
                { "\u03BC", "&mu;" }, // greek small letter mu, U+03BC ISOgrk3 -->
                { "\u03BD", "&nu;" }, // greek small letter nu, U+03BD ISOgrk3 -->
                { "\u03BE", "&xi;" }, // greek small letter xi, U+03BE ISOgrk3 -->
                { "\u03BF", "&omicron;" }, // greek small letter omicron, U+03BF NEW -->
                { "\u03C0", "&pi;" }, // greek small letter pi, U+03C0 ISOgrk3 -->
                { "\u03C1", "&rho;" }, // greek small letter rho, U+03C1 ISOgrk3 -->
                { "\u03C2", "&sigmaf;" }, // greek small letter final sigma,U+03C2 ISOgrk3 -->
                { "\u03C3", "&sigma;" }, // greek small letter sigma,U+03C3 ISOgrk3 -->
                { "\u03C4", "&tau;" }, // greek small letter tau, U+03C4 ISOgrk3 -->
                { "\u03C5", "&upsilon;" }, // greek small letter upsilon,U+03C5 ISOgrk3 -->
                { "\u03C6", "&phi;" }, // greek small letter phi, U+03C6 ISOgrk3 -->
                { "\u03C7", "&chi;" }, // greek small letter chi, U+03C7 ISOgrk3 -->
                { "\u03C8", "&psi;" }, // greek small letter psi, U+03C8 ISOgrk3 -->
                { "\u03C9", "&omega;" }, // greek small letter omega,U+03C9 ISOgrk3 -->
                { "\u03D1", "&thetasym;" }, // greek small letter theta symbol,U+03D1 NEW -->
                { "\u03D2", "&upsih;" }, // greek upsilon with hook symbol,U+03D2 NEW -->
                { "\u03D6", "&piv;" }, // greek pi symbol, U+03D6 ISOgrk3 -->
                // <!-- General Punctuation -->
                { "\u2022", "&bull;" }, // bullet = black small circle,U+2022 ISOpub -->
                // <!-- bullet is NOT the same as bullet operator, U+2219 -->
                { "\u2026", "&hellip;" }, // horizontal ellipsis = three dot leader,U+2026 ISOpub -->
                { "\u2032", "&prime;" }, // prime = minutes = feet, U+2032 ISOtech -->
                { "\u2033", "&Prime;" }, // double prime = seconds = inches,U+2033 ISOtech -->
                { "\u203E", "&oline;" }, // overline = spacing overscore,U+203E NEW -->
                { "\u2044", "&frasl;" }, // fraction slash, U+2044 NEW -->
                // <!-- Letterlike Symbols -->
                { "\u2118", "&weierp;" }, // script capital P = power set= Weierstrass p, U+2118 ISOamso -->
                { "\u2111", "&image;" }, // blackletter capital I = imaginary part,U+2111 ISOamso -->
                { "\u211C", "&real;" }, // blackletter capital R = real part symbol,U+211C ISOamso -->
                { "\u2122", "&trade;" }, // trade mark sign, U+2122 ISOnum -->
                { "\u2135", "&alefsym;" }, // alef symbol = first transfinite cardinal,U+2135 NEW -->
                // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
                // same glyph could be used to depict both characters -->
                // <!-- Arrows -->
                { "\u2190", "&larr;" }, // leftwards arrow, U+2190 ISOnum -->
                { "\u2191", "&uarr;" }, // upwards arrow, U+2191 ISOnum-->
                { "\u2192", "&rarr;" }, // rightwards arrow, U+2192 ISOnum -->
                { "\u2193", "&darr;" }, // downwards arrow, U+2193 ISOnum -->
                { "\u2194", "&harr;" }, // left right arrow, U+2194 ISOamsa -->
                { "\u21B5", "&crarr;" }, // downwards arrow with corner leftwards= carriage return, U+21B5 NEW -->
                { "\u21D0", "&lArr;" }, // leftwards double arrow, U+21D0 ISOtech -->
                // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
                // arrow but also does not have any other character for that function.
                // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
                { "\u21D1", "&uArr;" }, // upwards double arrow, U+21D1 ISOamsa -->
                { "\u21D2", "&rArr;" }, // rightwards double arrow,U+21D2 ISOtech -->
                // <!-- ISO 10646 does not say this is the 'implies' character but does not
                // have another character with this function so ?rArr can be used for
                // 'implies' as ISOtech suggests -->
                { "\u21D3", "&dArr;" }, // downwards double arrow, U+21D3 ISOamsa -->
                { "\u21D4", "&hArr;" }, // left right double arrow,U+21D4 ISOamsa -->
                // <!-- Mathematical Operators -->
                { "\u2200", "&forall;" }, // for all, U+2200 ISOtech -->
                { "\u2202", "&part;" }, // partial differential, U+2202 ISOtech -->
                { "\u2203", "&exist;" }, // there exists, U+2203 ISOtech -->
                { "\u2205", "&empty;" }, // empty set = null set = diameter,U+2205 ISOamso -->
                { "\u2207", "&nabla;" }, // nabla = backward difference,U+2207 ISOtech -->
                { "\u2208", "&isin;" }, // element of, U+2208 ISOtech -->
                { "\u2209", "&notin;" }, // not an element of, U+2209 ISOtech -->
                { "\u220B", "&ni;" }, // contains as member, U+220B ISOtech -->
                // <!-- should there be a more memorable name than 'ni'? -->
                { "\u220F", "&prod;" }, // n-ary product = product sign,U+220F ISOamsb -->
                // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
                // though the same glyph might be used for both -->
                { "\u2211", "&sum;" }, // n-ary summation, U+2211 ISOamsb -->
                // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
                // though the same glyph might be used for both -->
                { "\u2212", "&minus;" }, // minus sign, U+2212 ISOtech -->
                { "\u2217", "&lowast;" }, // asterisk operator, U+2217 ISOtech -->
                { "\u221A", "&radic;" }, // square root = radical sign,U+221A ISOtech -->
                { "\u221D", "&prop;" }, // proportional to, U+221D ISOtech -->
                { "\u221E", "&infin;" }, // infinity, U+221E ISOtech -->
                { "\u2220", "&ang;" }, // angle, U+2220 ISOamso -->
                { "\u2227", "&and;" }, // logical and = wedge, U+2227 ISOtech -->
                { "\u2228", "&or;" }, // logical or = vee, U+2228 ISOtech -->
                { "\u2229", "&cap;" }, // intersection = cap, U+2229 ISOtech -->
                { "\u222A", "&cup;" }, // union = cup, U+222A ISOtech -->
                { "\u222B", "&int;" }, // integral, U+222B ISOtech -->
                { "\u2234", "&there4;" }, // therefore, U+2234 ISOtech -->
                { "\u223C", "&sim;" }, // tilde operator = varies with = similar to,U+223C ISOtech -->
                // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
                // the same glyph might be used to represent both -->
                { "\u2245", "&cong;" }, // approximately equal to, U+2245 ISOtech -->
                { "\u2248", "&asymp;" }, // almost equal to = asymptotic to,U+2248 ISOamsr -->
                { "\u2260", "&ne;" }, // not equal to, U+2260 ISOtech -->
                { "\u2261", "&equiv;" }, // identical to, U+2261 ISOtech -->
                { "\u2264", "&le;" }, // less-than or equal to, U+2264 ISOtech -->
                { "\u2265", "&ge;" }, // greater-than or equal to,U+2265 ISOtech -->
                { "\u2282", "&sub;" }, // subset of, U+2282 ISOtech -->
                { "\u2283", "&sup;" }, // superset of, U+2283 ISOtech -->
                // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
                // Symbol font encoding and is not included. Should it be, for symmetry?
                // It is in ISOamsn -->,
                { "\u2284", "&nsub;" }, // not a subset of, U+2284 ISOamsn -->
                { "\u2286", "&sube;" }, // subset of or equal to, U+2286 ISOtech -->
                { "\u2287", "&supe;" }, // superset of or equal to,U+2287 ISOtech -->
                { "\u2295", "&oplus;" }, // circled plus = direct sum,U+2295 ISOamsb -->
                { "\u2297", "&otimes;" }, // circled times = vector product,U+2297 ISOamsb -->
                { "\u22A5", "&perp;" }, // up tack = orthogonal to = perpendicular,U+22A5 ISOtech -->
                { "\u22C5", "&sdot;" }, // dot operator, U+22C5 ISOamsb -->
                // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
                // <!-- Miscellaneous Technical -->
                { "\u2308", "&lceil;" }, // left ceiling = apl upstile,U+2308 ISOamsc -->
                { "\u2309", "&rceil;" }, // right ceiling, U+2309 ISOamsc -->
                { "\u230A", "&lfloor;" }, // left floor = apl downstile,U+230A ISOamsc -->
                { "\u230B", "&rfloor;" }, // right floor, U+230B ISOamsc -->
                { "\u2329", "&lang;" }, // left-pointing angle bracket = bra,U+2329 ISOtech -->
                // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
                // mark' -->
                { "\u232A", "&rang;" }, // right-pointing angle bracket = ket,U+232A ISOtech -->
                // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
                // 'single right-pointing angle quotation mark' -->
                // <!-- Geometric Shapes -->
                { "\u25CA", "&loz;" }, // lozenge, U+25CA ISOpub -->
                // <!-- Miscellaneous Symbols -->
                { "\u2660", "&spades;" }, // black spade suit, U+2660 ISOpub -->
                // <!-- black here seems to mean filled as opposed to hollow -->
                { "\u2663", "&clubs;" }, // black club suit = shamrock,U+2663 ISOpub -->
                { "\u2665", "&hearts;" }, // black heart suit = valentine,U+2665 ISOpub -->
                { "\u2666", "&diams;" }, // black diamond suit, U+2666 ISOpub -->

                // <!-- Latin Extended-A -->
                { "\u0152", "&OElig;" }, // -- latin capital ligature OE,U+0152 ISOlat2 -->
                { "\u0153", "&oelig;" }, // -- latin small ligature oe, U+0153 ISOlat2 -->
                // <!-- ligature is a misnomer, this is a separate character in some languages -->
                { "\u0160", "&Scaron;" }, // -- latin capital letter S with caron,U+0160 ISOlat2 -->
                { "\u0161", "&scaron;" }, // -- latin small letter s with caron,U+0161 ISOlat2 -->
                { "\u0178", "&Yuml;" }, // -- latin capital letter Y with diaeresis,U+0178 ISOlat2 -->
                // <!-- Spacing Modifier Letters -->
                { "\u02C6", "&circ;" }, // -- modifier letter circumflex accent,U+02C6 ISOpub -->
                { "\u02DC", "&tilde;" }, // small tilde, U+02DC ISOdia -->
                // <!-- General Punctuation -->
                { "\u2002", "&ensp;" }, // en space, U+2002 ISOpub -->
                { "\u2003", "&emsp;" }, // em space, U+2003 ISOpub -->
                { "\u2009", "&thinsp;" }, // thin space, U+2009 ISOpub -->
                { "\u200C", "&zwnj;" }, // zero width non-joiner,U+200C NEW RFC 2070 -->
                { "\u200D", "&zwj;" }, // zero width joiner, U+200D NEW RFC 2070 -->
                { "\u200E", "&lrm;" }, // left-to-right mark, U+200E NEW RFC 2070 -->
                { "\u200F", "&rlm;" }, // right-to-left mark, U+200F NEW RFC 2070 -->
                { "\u2013", "&ndash;" }, // en dash, U+2013 ISOpub -->
                { "\u2014", "&mdash;" }, // em dash, U+2014 ISOpub -->
                { "\u2018", "&lsquo;" }, // left single quotation mark,U+2018 ISOnum -->
                { "\u2019", "&rsquo;" }, // right single quotation mark,U+2019 ISOnum -->
                { "\u201A", "&sbquo;" }, // single low-9 quotation mark, U+201A NEW -->
                { "\u201C", "&ldquo;" }, // left double quotation mark,U+201C ISOnum -->
                { "\u201D", "&rdquo;" }, // right double quotation mark,U+201D ISOnum -->
                { "\u201E", "&bdquo;" }, // double low-9 quotation mark, U+201E NEW -->
                { "\u2020", "&dagger;" }, // dagger, U+2020 ISOpub -->
                { "\u2021", "&Dagger;" }, // double dagger, U+2021 ISOpub -->
                { "\u2030", "&permil;" }, // per mille sign, U+2030 ISOtech -->
                { "\u2039", "&lsaquo;" }, // single left-pointing angle quotation mark,U+2039 ISO proposed -->
                // <!-- lsaquo is proposed but not yet ISO standardized -->
                { "\u203A", "&rsaquo;" }, // single right-pointing angle quotation mark,U+203A ISO proposed -->
                // <!-- rsaquo is proposed but not yet ISO standardized -->
                { "\u20AC", "&euro;" }, // -- euro sign, U+20AC NEW -->
        };

        /**
         * Reverse of {@link #HTML40_EXTENDED_ESCAPE()} for unescaping purposes.
         * @return
         */
        public static String[][] HTML40_EXTENDED_UNESCAPE() {
            return HTML40_EXTENDED_UNESCAPE.clone();
        }

        /** The Constant HTML40_EXTENDED_UNESCAPE. */
        private static final String[][] HTML40_EXTENDED_UNESCAPE = invert(HTML40_EXTENDED_ESCAPE);

        /**
         * Mapping to escape the basic XML and HTML character entities.
         *
         * Namely: {@code " & < >}
         * @return
         */
        public static String[][] BASIC_ESCAPE() {
            return BASIC_ESCAPE.clone();
        }

        /** The Constant BASIC_ESCAPE. */
        private static final String[][] BASIC_ESCAPE = { { "\"", "&quot;" }, // " - double-quote
                { "&", "&amp;" }, // & - ampersand
                { "<", "&lt;" }, // < - less-than
                { ">", "&gt;" }, // > - greater-than
        };

        /**
         * Reverse of {@link #BASIC_ESCAPE()} for unescaping purposes.
         * @return
         */
        public static String[][] BASIC_UNESCAPE() {
            return BASIC_UNESCAPE.clone();
        }

        /** The Constant BASIC_UNESCAPE. */
        private static final String[][] BASIC_UNESCAPE = invert(BASIC_ESCAPE);

        /**
         * Mapping to escape the apostrophe character to its XML character bean.
         * @return
         */
        public static String[][] APOS_ESCAPE() {
            return APOS_ESCAPE.clone();
        }

        /** The Constant APOS_ESCAPE. */
        private static final String[][] APOS_ESCAPE = { { "'", "&apos;" }, // XML apostrophe
        };

        /**
         * Reverse of {@link #APOS_ESCAPE()} for unescaping purposes.
         * @return
         */
        public static String[][] APOS_UNESCAPE() {
            return APOS_UNESCAPE.clone();
        }

        /** The Constant APOS_UNESCAPE. */
        private static final String[][] APOS_UNESCAPE = invert(APOS_ESCAPE);

        /**
         * Mapping to escape the Java control characters.
         *
         * Namely: {@code \b \n \t \f \r}
         * @return
         */
        public static String[][] JAVA_CTRL_CHARS_ESCAPE() {
            return JAVA_CTRL_CHARS_ESCAPE.clone();
        }

        /** The Constant JAVA_CTRL_CHARS_ESCAPE. */
        private static final String[][] JAVA_CTRL_CHARS_ESCAPE = { { "\b", "\\b" }, { "\n", "\\n" }, { "\t", "\\t" }, { "\f", "\\f" }, { "\r", "\\r" } };

        /**
         * Reverse of {@link #JAVA_CTRL_CHARS_ESCAPE()} for unescaping purposes.
         * @return
         */
        public static String[][] JAVA_CTRL_CHARS_UNESCAPE() {
            return JAVA_CTRL_CHARS_UNESCAPE.clone();
        }

        /** The Constant JAVA_CTRL_CHARS_UNESCAPE. */
        private static final String[][] JAVA_CTRL_CHARS_UNESCAPE = invert(JAVA_CTRL_CHARS_ESCAPE);

        /**
         * Used to invert an escape array into an unescaped array.
         *
         * @param array String[][] to be inverted
         * @return String[][] inverted array
         */
        public static String[][] invert(final String[][] array) {
            final String[][] newarray = new String[array.length][2];
            for (int i = 0; i < array.length; i++) {
                newarray[i][0] = array[i][1];
                newarray[i][1] = array[i][0];
            }
            return newarray;
        }

    }
}
