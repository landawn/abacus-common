/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import static com.landawn.abacus.util.WD._BACKSLASH;
import static com.landawn.abacus.util.WD._QUOTATION_D;
import static com.landawn.abacus.util.WD._QUOTATION_S;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 *
 * <br />
 * <br />
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. For example, if
 * user tries to reverse a {@code null} or empty String. The input String will be
 * returned. But exception will be thrown if try to add element to a {@code null} Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 *
 *
 * @see {@code Joiner}, {@code Splitter}
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.URLEncodedUtil
 * @see com.landawn.abacus.util.AppendableWriter
 * @see com.landawn.abacus.util.StringWriter
 * @see com.landawn.abacus.util.RegExUtil
 * @see com.landawn.abacus.util.IEEE754rUtil
 */
@SuppressWarnings({ "java:S1694", "UnnecessaryUnicodeEscape" })
public abstract sealed class Strings permits Strings.StringUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Strings.class);

    /**
     * String with value {@code "null"}.
     */
    @Beta
    public static final String NULL = "null";

    /**
     *
     * Char array with value {@code "['n', 'u', 'l', 'l']"}.
     */
    static final char[] NULL_CHAR_ARRAY = NULL.toCharArray();

    /**
     * The empty String {@code ""}.
     */
    public static final String EMPTY = "";

    //    /**
    //     * The empty String {@code ""}.
    //     * @deprecated Use {@link #EMPTY_STRING} instead
    //     */
    //    @Deprecated
    //    public static final String EMPTY = EMPTY_STRING;

    /**
     * A String for a space character: {@code " "}.
     *
     */
    public static final String SPACE = WD.SPACE;

    /**
     * A String for linefeed LF ("\n").
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     */
    public static final String LF = "\n";

    /**
     * A String for carriage return CR ("\r").
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     */
    public static final String CR = "\r";

    /**
     * Carriage return followed by line feed. This is the line ending used on Windows.
     */
    public static final String CR_LF = "\r\n";

    @Beta
    public static final char CHAR_ZERO = (char) 0;
    @Beta
    public static final char CHAR_SPACE = WD._SPACE;
    @Beta
    public static final char CHAR_LF = LF.charAt(0);
    @Beta
    public static final char CHAR_CR = CR.charAt(0);

    /**
     * Field COMMA_SPACE (value is {@code ", "})
     */
    public static final String COMMA_SPACE = WD.COMMA_SPACE;

    /**
     * Value is {@code ", "}
     */
    public static final String ELEMENT_SEPARATOR = COMMA_SPACE;

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    static final String TRUE = Boolean.TRUE.toString().intern();

    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    static final String FALSE = Boolean.FALSE.toString().intern();

    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    static final String BACKSLASH_ASTERISK = "*";

    static final String STR_FOR_EMPTY_ARRAY = "[]";

    // java.lang.ExceptionInInitializerError: Exception java.lang.NoClassDefFoundError: Could not initialize class com.landawn.abacus.util.WD [in thread "main"]
    //    static final Set<String> CASE_INSENSITIVE_KEYWORDS = Set.of(" ", ", ", ";", ":", ":", ":", "=", "|", "&", "@", "$", "*", "+", "-", "_", "#", "!", "<", ">",
    //            "~", "^", "%", "\"", "'", "`", "{", "}", "[", "]", "(", ")", "?", "/", "\\", ".", ",", ";", ":", "!", "@", "#", "$", "%", "^", "&", "*");

    //    static final Set<String> CASE_INSENSITIVE_KEYWORDS;
    //
    //    static {
    //        final String[] strs = java.util.stream.Stream.of(WD.class.getDeclaredFields())
    //                .filter(it -> Modifier.isPublic(it.getModifiers()) && Modifier.isStatic(it.getModifiers()) && Modifier.isFinal(it.getModifiers())
    //                        && it.getType() == String.class)
    //                .map(it -> {
    //                    try {
    //                        return (String) it.get(null);
    //                    } catch (IllegalArgumentException | IllegalAccessException e) {
    //                        throw new RuntimeException(e);
    //                    }
    //                })
    //                .filter(it -> it.length() <= 3 && it.toUpperCase().equals(it.toLowerCase()))
    //                .toArray(String[]::new);
    //
    //        CASE_INSENSITIVE_KEYWORDS = Set.of(strs);
    //
    //        N.println(CASE_INSENSITIVE_KEYWORDS);
    //    }

    /**
     * A regex pattern for recognizing blocks of whitespace characters.
     * The apparent convolutedness of the pattern serves the purpose of ignoring
     * "blocks" consisting of only a single space: the pattern is used only to
     * normalize whitespace, condensing "blocks" down to a single space, thus
     * matching the same would likely cause a great many noop replacements.
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(?: |\\u00A0|\\s|[\\s&&[^ ]])\\s*");//NOSONAR

    private static final Splitter lineSplitter = Splitter.forLines();
    private static final Splitter trimLineSplitter = Splitter.forLines().trimResults();
    private static final Splitter omitEmptyLinesLineSplitter = Splitter.forLines().omitEmptyStrings();
    private static final Splitter trimAndOmitEmptyLinesLineSplitter = Splitter.forLines().trimResults().omitEmptyStrings();

    //    private static final Map<Object, Splitter> splitterPool = new HashMap<>();
    //
    //    private static final Map<Object, Splitter> trimSplitterPool = new HashMap<>();
    //
    //    private static final Map<Object, Splitter> preserveSplitterPool = new HashMap<>();
    //
    //    private static final Map<Object, Splitter> trimPreserveSplitterPool = new HashMap<>();
    //
    //    static {
    //        final List<String> delimiters = Array.asList(" ", "  ", "   ", "\t", "\n", "\r", ",", ", ", ";", "; ", ":", ": ", " : ", "-", " - ", "_", " _ ", "#",
    //                "##", " # ", "=", "==", " = ", "|", " | ", "||", " || ", "&", "&&", "@", "@@", "$", "$$", "*", "**", "+", "++");
    //
    //        for (final String delimiter : delimiters) {
    //            splitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings());
    //            trimSplitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings().trimResults());
    //            preserveSplitterPool.put(delimiter, Splitter.with(delimiter));
    //            trimPreserveSplitterPool.put(delimiter, Splitter.with(delimiter).trimResults());
    //
    //            if (delimiter.length() == 1) {
    //                final char delimiterChar = delimiter.charAt(0);
    //
    //                splitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings());
    //                trimSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings().trimResults());
    //                preserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar));
    //                trimPreserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).trimResults());
    //            }
    //        }
    //    }

    private static final Encoder BASE64_ENCODER = java.util.Base64.getEncoder();

    private static final Decoder BASE64_DECODER = java.util.Base64.getDecoder();

    private static final Encoder BASE64_URL_ENCODER = java.util.Base64.getUrlEncoder().withoutPadding();

    private static final Decoder BASE64_URL_DECODER = java.util.Base64.getUrlDecoder();

    private Strings() {
        // Utility class.
    }

    /**
     * Returns a new UUID String without '-'.
     *
     * @return
     * @see UUID#randomUUID().
     */
    public static String guid() {
        return uuid().replace("-", "");
    }

    /**
     * Returns a new UUID String.
     *
     * @return
     * @see UUID#randomUUID().
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Converts the provided character array into a String.
     *
     * @param value The character array to be converted. It can be {@code null}.
     * @return A String representation of the character array. Returns {@code null} if <i>value</i> is {@code null}.
     * @see String#valueOf(char[])
     * @see N#toString(Object)
     */
    public static String valueOf(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Checks if the given CharSequence is a Java keyword.
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is a Java keyword, {@code false} otherwise.
     */
    public static boolean isKeyword(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        return SourceVersion.isKeyword(cs);
    }

    /**
     * Checks if the given CharSequence is a valid Java identifier.
     *
     * A valid Java identifier must start with a letter, a currency character ($), or a connecting character such as underscore (_).
     * Identifiers cannot start with a number, and they cannot be a Java keyword or boolean literal (true or false).
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is a valid Java identifier, {@code false} otherwise.
     */
    public static boolean isValidJavaIdentifier(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        return RegExUtil.JAVA_IDENTIFIER_MATCHER.matcher(cs).matches();
    }

    /**
     * Checks if the given CharSequence is a valid email address.
     *
     * This method uses a regular expression (RFC 5322) to validate the email address. It checks for the general form of an email address
     * which is "local-part@domain". The local-part can contain alphanumeric characters and special characters like !, #, $, %, &, ', *, +, -, /, =, ?, ^, _, `, {, |, } and ~.
     * The domain part contains at least one dot (.) and can contain alphanumeric characters as well as hyphens (-).
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is a valid email address, {@code false} otherwise.
     * @see #findFirstEmailAddress(CharSequence)
     * @see #findAllEmailAddresses(CharSequence)
     */
    public static boolean isValidEmailAddress(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        return RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher(cs).matches();
    }

    /**
     * Checks if the given CharSequence is a valid URL.
     *
     * This method uses a regular expression to validate the URL. It checks for the general form of a URL
     * which includes protocol, domain, port, path, query parameters, and fragment identifier.
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is a valid URL, {@code false} otherwise.
     */
    public static boolean isValidUrl(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        return RegExUtil.URL_FINDER.matcher(cs).matches();
    }

    /**
     * Checks if the given CharSequence is a valid HTTP URL.
     *
     * This method uses a regular expression to validate the URL. It checks for the general form of a URL
     * which includes protocol, domain, port, path, query parameters, and fragment identifier.
     * The URL must start with http:// or https://.
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is a valid HTTP URL, {@code false} otherwise.
     */
    public static boolean isValidHttpUrl(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        return RegExUtil.HTTP_URL_FINDER.matcher(cs).matches();
    }

    /**
     * Checks if the specified {@code CharSequence} is {@code null} or empty.
     *
     * @param cs
     * @return
     */
    public static boolean isEmpty(final CharSequence cs) {
        return (cs == null) || (cs.isEmpty());
    }

    /**
     * Checks if the given CharSequence is {@code null} or contains only whitespace characters.
     *
     * @param cs The CharSequence to be checked. It can be {@code null} or empty.
     * @return {@code true} if the CharSequence is {@code null} or contains only whitespace characters, {@code false} otherwise.
     */
    public static boolean isBlank(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }

        if (cs instanceof String) {
            return ((String) cs).isBlank();
        }

        for (int i = 0, len = cs.length(); i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the given CharSequence is not {@code null} and not empty.
     *
     * @param cs The CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if the CharSequence is not {@code null} and not empty, {@code false} otherwise.
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return (cs != null) && (!cs.isEmpty());
    }

    /**
     * Checks if the given CharSequence is not {@code null} and contains non-whitespace characters.
     *
     * @param cs The CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if the CharSequence is not {@code null} and contains non-whitespace characters, {@code false} otherwise.
     */
    @Beta
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    //    /**
    //     *
    //     * @param cs
    //     * @return
    //     */
    //    public static boolean notBlank(final CharSequence cs) {
    //        return !isBlank(cs);
    //    }
    //
    //    /**
    //     *
    //     * @param cs
    //     * @return
    //     */
    //    @Beta
    //    public static boolean notEmpty(final CharSequence cs) {
    //        return (cs != null) && (cs.length() > 0);
    //    }

    /**
     * Checks if both of the provided CharSequences are empty or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if both CharSequences are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAllEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) && isEmpty(b);
    }

    /**
     * Checks if all the provided CharSequences are empty or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @param c The third CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if all CharSequences are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAllEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) && isEmpty(b) && isEmpty(c);
    }

    /**
     * <p>Checks if all the CharSequences are empty ("") or {@code null}.</p>
     *
     * <pre>
     * Strings.isAllEmpty(null)             = true
     * Strings.isAllEmpty(null, "")         = true
     * Strings.isAllEmpty(new String[] {})  = true
     * Strings.isAllEmpty(null, "foo")      = false
     * Strings.isAllEmpty("", "bar")        = false
     * Strings.isAllEmpty("bob", "")        = false
     * Strings.isAllEmpty("  bob  ", null)  = false
     * Strings.isAllEmpty(" ", "bar")       = false
     * Strings.isAllEmpty("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be {@code null} or empty
     * @return {@code true} if all the CharSequences are empty or null
     * @see Strings#isAllEmpty(CharSequence...)
     */
    public static boolean isAllEmpty(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotEmpty(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all the provided CharSequences in the Iterable are empty or {@code null}.
     *
     * @param css The Iterable of CharSequences to be checked. It can be {@code null}.
     * @return {@code true} if all CharSequences in the Iterable are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAllEmpty(final Iterable<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotEmpty(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if both of the provided CharSequences are blank or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if both CharSequences are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAllBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) && isBlank(b);
    }

    /**
     * Checks if all the provided CharSequences are blank or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @param c The third CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if all CharSequences are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAllBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) && isBlank(b) && isBlank(c);
    }

    /**
     * <p>Checks if all the CharSequences are empty (""), {@code null} or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * Strings.isAllBlank(null)             = true
     * Strings.isAllBlank(null, "foo")      = false
     * Strings.isAllBlank(null, null)       = true
     * Strings.isAllBlank("", "bar")        = false
     * Strings.isAllBlank("bob", "")        = false
     * Strings.isAllBlank("  bob  ", null)  = false
     * Strings.isAllBlank(" ", "bar")       = false
     * Strings.isAllBlank("foo", "bar")     = false
     * Strings.isAllBlank(new String[] {})  = true
     * </pre>
     *
     * @param css the CharSequences to check, may be {@code null} or empty
     * @return {@code true} if all the CharSequences are empty or {@code null} or whitespace only
     * @see Strings#isAllBlank(CharSequence...)
     */
    public static boolean isAllBlank(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotBlank(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all the provided CharSequences in the Iterable are blank or {@code null}.
     *
     * @param css The Iterable of CharSequences to be checked. It can be {@code null}.
     * @return {@code true} if all CharSequences in the Iterable are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAllBlank(final Iterable<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotBlank(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if any of the provided CharSequences are empty or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if any of the CharSequences are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAnyEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) || isEmpty(b);
    }

    /**
     * Checks if any of the provided CharSequences are empty or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @param c The third CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if any of the CharSequences are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAnyEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) || isEmpty(b) || isEmpty(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or {@code null}.</p>
     *
     * <pre>
     * Strings.isAnyEmpty((String) null)    = true
     * Strings.isAnyEmpty((String[]) null)  = false
     * Strings.isAnyEmpty(null, "foo")      = true
     * Strings.isAnyEmpty("", "bar")        = true
     * Strings.isAnyEmpty("bob", "")        = true
     * Strings.isAnyEmpty("  bob  ", null)  = true
     * Strings.isAnyEmpty(" ", "bar")       = false
     * Strings.isAnyEmpty("foo", "bar")     = false
     * Strings.isAnyEmpty(new String[]{})   = false
     * Strings.isAnyEmpty(new String[]{""}) = true
     * </pre>
     *
     * @param css the CharSequences to check, may be {@code null} or empty
     * @return {@code true} if any of the CharSequences are empty or null
     * @see Strings#isAnyEmpty(CharSequence...)
     */
    public static boolean isAnyEmpty(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if any of the provided CharSequences in the Iterable are empty or {@code null}.
     *
     * @param css The Iterable of CharSequences to be checked. It can be {@code null}.
     * @return {@code true} if any CharSequences in the Iterable are {@code null} or empty, {@code false} otherwise.
     */
    public static boolean isAnyEmpty(final Iterable<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if any of the provided CharSequences are blank or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if any of the CharSequences are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAnyBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) || isBlank(b);
    }

    /**
     * Checks if any of the provided CharSequences are blank or {@code null}.
     *
     * @param a The first CharSequence to be checked. It can be {@code null}.
     * @param b The second CharSequence to be checked. It can be {@code null}.
     * @param c The third CharSequence to be checked. It can be {@code null}.
     * @return {@code true} if any of the CharSequences are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAnyBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) || isBlank(b) || isBlank(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or {@code null} or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * Strings.isAnyBlank((String) null)    = true
     * Strings.isAnyBlank((String[]) null)  = false
     * Strings.isAnyBlank(null, "foo")      = true
     * Strings.isAnyBlank(null, null)       = true
     * Strings.isAnyBlank("", "bar")        = true
     * Strings.isAnyBlank("bob", "")        = true
     * Strings.isAnyBlank("  bob  ", null)  = true
     * Strings.isAnyBlank(" ", "bar")       = true
     * Strings.isAnyBlank(new String[] {})  = false
     * Strings.isAnyBlank(new String[]{""}) = true
     * Strings.isAnyBlank("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be {@code null} or empty
     * @return {@code true} if any of the CharSequences are empty or {@code null} or whitespace only
     * @see Strings#isAnyBlank(CharSequence...)
     */
    public static boolean isAnyBlank(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if any of the provided CharSequences in the Iterable are blank or {@code null}.
     *
     * @param css The Iterable of CharSequences to be checked. It can be {@code null}.
     * @return {@code true} if any CharSequences in the Iterable are {@code null} or blank, {@code false} otherwise.
     */
    public static boolean isAnyBlank(final Iterable<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the input string is wrapped with the specified prefix and suffix string.
     *
     * @param str The input string to be checked.
     * @param prefixSuffix The string that should be the prefix and suffix of the input string.
     * @return {@code true} if the input string starts and ends with the prefixSuffix string, {@code false} otherwise.
     * @throws IllegalArgumentException if prefixSuffix is empty.
     */
    public static boolean isWrappedWith(final String str, final String prefixSuffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefixSuffix, cs.prefixSuffix);

        return str != null && str.length() >= prefixSuffix.length() * 2 && str.startsWith(prefixSuffix) && str.endsWith(prefixSuffix);
    }

    /**
     * Checks if the input string is wrapped with the specified prefix and suffix string.
     *
     * @param str The input string to be checked.
     * @param prefix The string that should be the prefix of the input string.
     * @param suffix The string that should be the suffix of the input string.
     * @return {@code true} if the input string starts with the prefix and ends with the suffix, {@code false} otherwise.
     * @throws IllegalArgumentException if prefix or suffix is empty.
     */
    public static boolean isWrappedWith(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);
        N.checkArgNotEmpty(suffix, cs.suffix);

        return str != null && str.length() >= prefix.length() + suffix.length() && str.startsWith(prefix) && str.endsWith(suffix);
    }

    /**
     * Returns the specified default value if the given {@code charSequence} is {@code null}, otherwise returns the {@code charSequence} itself.
     *
     * @param <T> the type of {@code CharSequence}
     * @param str the {@code charSequence} to check for {@code null}
     * @param defaultForNull the default value to return if {@code str} is {@code null}
     * @return {@code str} if it is not {@code null}, otherwise {@code defaultForNull}
     * @throws IllegalArgumentException if the specified default value is {@code null}.
     * @see #defaultIfEmpty(CharSequence, CharSequence)
     * @see #defaultIfBlank(CharSequence, CharSequence)
     * @see N#defaultIfNull(Object, Object)
     */
    public static <T extends CharSequence> T defaultIfNull(final T str, final T defaultForNull) throws IllegalArgumentException {
        N.checkArgNotNull(defaultForNull, cs.defaultValue);

        return str == null ? defaultForNull : str;
    }

    /**
     * Returns the default value provided by specified {@code Supplier} if the specified {@code charSequence} is {@code null}, otherwise returns the {@code charSequence} itself.
     *
     * @param <T> the type of {@code CharSequence}
     * @param str the {@code charSequence} to check for {@code null}
     * @param supplierForDefault
     * @return
     * @throws IllegalArgumentException if default value provided by specified {@code Supplier} is {@code null} when the specified {@code charSequence} is {@code null}.
     * @see #defaultIfEmpty(CharSequence, Supplier)
     * @see #defaultIfBlank(CharSequence, Supplier)
     * @see N#defaultIfNull(Object, Supplier)
     */
    public static <T extends CharSequence> T defaultIfNull(final T str, final Supplier<? extends T> supplierForDefault) throws IllegalArgumentException {
        if (str == null) {
            return N.checkArgNotNull(supplierForDefault.get(), cs.defaultValue);
        }

        return str;
    }

    /**
     * Returns the specified default value if the specified {@code charSequence} is empty, otherwise returns the {@code charSequence} itself.
     *
     * @param <T>
     * @param str
     * @param defaultForEmpty
     * @return
     * @throws IllegalArgumentException if the specified default charSequence value is empty.
     * @see #defaultIfNull(CharSequence, CharSequence)
     * @see #defaultIfBlank(CharSequence, CharSequence)
     * @see #firstNonEmpty(String, String)
     * @see N#defaultIfEmpty(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final T defaultForEmpty) throws IllegalArgumentException {
        N.checkArgNotEmpty(defaultForEmpty, cs.defaultValue);

        return isEmpty(str) ? defaultForEmpty : str;
    }

    /**
     * Returns the default value provided by specified {@code Supplier} if the specified {@code charSequence} is empty, otherwise returns the {@code charSequence} itself.
     *
     * @param <T>
     * @param str
     * @param supplierForDefault
     * @return
     * @throws IllegalArgumentException if default value provided by specified {@code Supplier} is empty when the specified {@code charSequence} is empty.
     * @see #defaultIfNull(CharSequence, Supplier)
     * @see #defaultIfBlank(CharSequence, Supplier)
     * @see #firstNonEmpty(String, String)
     * @see N#defaultIfEmpty(CharSequence, Supplier)
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final Supplier<? extends T> supplierForDefault) {
        if (isEmpty(str)) {
            return N.checkArgNotEmpty(supplierForDefault.get(), cs.defaultValue);
        }

        return str;
    }

    /**
     * Returns the specified default value if the specified {@code charSequence} is blank, otherwise returns the {@code charSequence} itself.
     *
     * @param <T>
     * @param str
     * @param defaultForBlank
     * @return
     * @throws IllegalArgumentException if the specified default charSequence value is bank.
     * @see #defaultIfNull(CharSequence, CharSequence)
     * @see #defaultIfEmpty(CharSequence, CharSequence)
     * @see #firstNonBlank(String, String)
     * @see N#defaultIfBlank(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final T defaultForBlank) throws IllegalArgumentException {
        N.checkArgNotBlank(defaultForBlank, cs.defaultValue);

        return isBlank(str) ? defaultForBlank : str;
    }

    /**
     * Returns the default value provided by specified {@code Supplier} if the specified {@code charSequence} is blank, otherwise returns the {@code charSequence} itself.
     *
     * @param <T>
     * @param str
     * @param supplierForDefault
     * @return
     * @throws IllegalArgumentException if default value provided by specified {@code Supplier} is blank when the specified {@code charSequence} is blank.
     * @see #defaultIfNull(CharSequence, Supplier)
     * @see #defaultIfEmpty(CharSequence, Supplier)
     * @see #firstNonBlank(String, String)
     * @see N#defaultIfBlank(CharSequence, Supplier)
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final Supplier<? extends T> supplierForDefault) {
        if (isBlank(str)) {
            return N.checkArgNotBlank(supplierForDefault.get(), cs.defaultValue);
        }

        return str;
    }

    /**
     * Returns the first non-empty String from the given two Strings.
     *
     * @param a The first String to be checked. It can be {@code null} or empty.
     * @param b The second String to be checked. It can be {@code null} or empty.
     * @return The first non-empty String from the given two String. If both are empty, returns an empty string {@code ""}.
     * @see N#firstNonEmpty(CharSequence, CharSequence)
     */
    public static String firstNonEmpty(final String a, final String b) {
        return isEmpty(a) ? (isEmpty(b) ? EMPTY : b) : a;
    }

    /**
     * Returns the first non-empty String from the given three Strings.
     *
     * @param a The first String to be checked. It can be {@code null} or empty.
     * @param b The second String to be checked. It can be {@code null} or empty.
     * @param c The third String to be checked. It can be {@code null} or empty.
     * @return The first non-empty String from the given three Strings. If all are empty, returns an empty string {@code ""}.
     * @see N#firstNonEmpty(CharSequence, CharSequence, CharSequence)
     */
    public static String firstNonEmpty(final String a, final String b, final String c) {
        return isEmpty(a) ? (isEmpty(b) ? (isEmpty(c) ? EMPTY : c) : b) : a;
    }

    /**
     * <p>Returns the first value in the array which is not empty.</p>
     *
     * <p>If all values are empty or the array is {@code null} or empty then an empty string {@code ""} is returned.</p>
     *
     * <pre>
     * Strings.firstNonEmpty(null, {@code null}, null)   = ""
     * Strings.firstNonEmpty(null, {@code null}, "")     = ""
     * Strings.firstNonEmpty(null, "", " ")      = " "
     * Strings.firstNonEmpty("abc")              = "abc"
     * Strings.firstNonEmpty(null, "xyz")        = "xyz"
     * Strings.firstNonEmpty("", "xyz")          = "xyz"
     * Strings.firstNonEmpty(null, "xyz", "abc") = "xyz"
     * Strings.firstNonEmpty()                   = ""
     * </pre>
     *
     * @param css the values to test, may be {@code null} or empty
     * @return the first value from {@code css} which is not empty, or an empty string {@code ""} if there is no non-empty value.
     * @see N#firstNonEmpty(CharSequence...)
     */
    public static String firstNonEmpty(final String... css) {
        if (N.isEmpty(css)) {
            return EMPTY;
        }

        for (final String val : css) {
            if (isNotEmpty(val)) {
                return val;
            }
        }

        return EMPTY;
    }

    /**
     * Returns the first non-empty String from the given Iterable of Strings.
     *
     * @param css The Iterable of Strings to be checked. It can be {@code null}.
     * @return The first non-empty String from the given Iterable. If all Strings are empty or the Iterable is {@code null}, returns an empty string {@code ""}.
     * @see N#firstNonEmpty(Iterable)
     */
    public static String firstNonEmpty(final Iterable<String> css) {
        if (N.isEmpty(css)) {
            return EMPTY;
        }

        for (final String val : css) {
            if (isNotEmpty(val)) {
                return val;
            }
        }

        return EMPTY;
    }

    /**
     * Returns the first non-blank String from the given two Strings.
     *
     * @param a The first String to be checked. It can be {@code null} or empty.
     * @param b The second String to be checked. It can be {@code null} or empty.
     * @return The first non-blank String from the given two Strings. If both are blank, returns an empty string {@code ""}.
     * @see N#firstNonBlank(CharSequence, CharSequence)
     */
    public static String firstNonBlank(final String a, final String b) {
        return isBlank(a) ? (isBlank(b) ? EMPTY : b) : a;
    }

    /**
     * Returns the first non-blank String from the given three Strings.
     *
     * @param a The first String to be checked. It can be {@code null} or empty.
     * @param b The second String to be checked. It can be {@code null} or empty.
     * @param c The third String to be checked. It can be {@code null} or empty.
     * @return The first non-blank String from the given three Strings. If all are blank, returns an empty string {@code ""}.
     * @see N#firstNonBlank(CharSequence, CharSequence, CharSequence)
     */
    public static String firstNonBlank(final String a, final String b, final String c) {
        return isBlank(a) ? (isBlank(b) ? (isBlank(c) ? EMPTY : c) : b) : a;
    }

    /**
     * Returns the first non-blank String from the given Strings.
     *
     * @param css The Strings to be checked. They can be {@code null} or empty.
     * @return The first non-blank String from the given Strings. If all are blank, returns an empty string {@code ""}.
     * @see N#firstNonBlank(CharSequence...)
     */
    public static String firstNonBlank(final String... css) {
        if (N.isEmpty(css)) {
            return EMPTY;
        }

        for (final String val : css) {
            if (isNotBlank(val)) {
                return val;
            }
        }

        return EMPTY;
    }

    /**
     * Returns the first non-blank String from the given Iterable of Strings, or an empty string {@code ""} if all Strings are blank or the Iterable is {@code null}.
     *
     * @param css The Iterable of Strings to be checked. It can be {@code null}.
     * @return The first non-blank String from the given Iterable. If all Strings are blank or the Iterable is {@code null}, returns an empty string {@code ""}.
     * @see N#firstNonBlank(Iterable)
     */
    public static String firstNonBlank(final Iterable<String> css) {
        if (N.isEmpty(css)) {
            return EMPTY;
        }

        for (final String val : css) {
            if (isNotBlank(val)) {
                return val;
            }
        }

        return EMPTY;
    }

    /**
     * Converts the specified String to an empty String {@code ""} if it's {@code null}, otherwise returns the original string.
     *
     * @param str The input string to be checked. It can be {@code null}.
     * @return An empty string if the input string is {@code null}, otherwise the original string.
     * @see N#nullToEmpty(String)
     */
    public static String nullToEmpty(final String str) {
        return str == null ? EMPTY : str;
    }

    /**
     * Converts each {@code null} String element in the specified String array to an empty String {@code ""}.
     * Do nothing if the input array is {@code null} or empty.
     *
     * @param strs The input string array to be checked. Each {@code null} element in the array will be converted to an empty string. It can be {@code null} or empty.
     * @see N#nullToEmpty(String[])
     * @see N#nullToEmptyForEach(String[])
     */
    public static void nullToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null ? EMPTY : strs[i];
        }
    }

    /**
     * Converts the specified String to {@code null} if it's empty, otherwise returns the original string.
     *
     * @param str The input string to be checked. It can be {@code null} or empty.
     * @return {@code null} if the input string is empty, otherwise the original string.
     */
    public static <T extends CharSequence> T emptyToNull(final T str) {
        return str == null || str.isEmpty() ? null : str;
    }

    /**
     * Converts each empty String element in the specified String array to {@code null}.
     * Do nothing if the input array is {@code null} or empty.
     *
     * @param strs The input string array to be checked. Each empty element in the array will be converted to {@code null}. It can be {@code null} or empty.
     */
    public static <T extends CharSequence> void emptyToNull(final T[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null || strs[i].isEmpty() ? null : strs[i];
        }
    }

    /**
     * Converts the specified String to an empty String {@code ""} if it's blank, otherwise returns the original string.
     *
     * @param str The input string to be checked. It can be {@code null} or empty.
     * @return An empty string if the input string is blank, otherwise the original string.
     */
    public static String blankToEmpty(final String str) {
        return isBlank(str) ? EMPTY : str;
    }

    /**
     * Converts each blank String element in the specified String array to an empty String {@code ""}.
     * Do nothing if the input array is {@code null} or empty.
     *
     * @param strs The input string array to be checked. Each blank element in the array will be converted to an empty string. It can be {@code null} or empty.
     */
    public static void blankToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = isBlank(strs[i]) ? EMPTY : strs[i];
        }
    }

    /**
     * Converts the specified String to {@code null} if it's blank, otherwise returns the original string.
     *
     * @param str The input string to be checked. It can be {@code null} or empty.
     * @return {@code null} if the input string is blank, otherwise the original string.
     */
    public static <T extends CharSequence> T blankToNull(final T str) {
        return isBlank(str) ? null : str;
    }

    /**
     * Converts each blank String element in the specified String array to {@code null}.
     * Do nothing if the input array is {@code null} or empty.
     *
     * @param strs The input string array to be checked. Each blank element in the array will be converted to {@code null}. It can be {@code null} or empty.
     */
    public static <T extends CharSequence> void blankToNull(final T[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = isBlank(strs[i]) ? null : strs[i];
        }
    }

    /**
     * <p>Abbreviates a String using ellipses. This will turn
     * "Now is the time for all good men" into "...is the time for..."</p>
     *
     * <p>Works like {@code abbreviate(String, int)}, but allows you to specify
     * a "left edge" offset.  Note that this left edge is not necessarily going to
     * be the leftmost character in the result, or the first character following the
     * ellipses, but it will appear somewhere in the result.
     *
     * <p>In no case will it return a String of length greater than
     * {@code maxWidth}.</p>
     *
     * <pre>
     * Strings.abbreviate(null, 0, 4)                  = null
     * Strings.abbreviate("", 0, 4)                  = ""
     * Strings.abbreviate("abcdefghijklmno", -1, 10) = "abcdefg..."
     * Strings.abbreviate("abcdefghijklmno", 0, 10)  = "abcdefg..."
     * Strings.abbreviate("abcdefghijklmno", 1, 10)  = "abcdefg..."
     * Strings.abbreviate("abcdefghijklmno", 4, 10)  = "abcdefg..."
     * Strings.abbreviate("abcdefghijklmno", 5, 10)  = "...fghi..."
     * Strings.abbreviate("abcdefghijklmno", 6, 10)  = "...ghij..."
     * Strings.abbreviate("abcdefghijklmno", 8, 10)  = "...ijklmno"
     * Strings.abbreviate("abcdefghijklmno", 10, 10) = "...ijklmno"
     * Strings.abbreviate("abcdefghijklmno", 12, 10) = "...ijklmno"
     * Strings.abbreviate("abcdefghij", 0, 3)        = IllegalArgumentException
     * Strings.abbreviate("abcdefghij", 5, 6)        = IllegalArgumentException
     * </pre>
     *
     * @param str the String to check, which may be null
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the width is too small
     * @deprecated
     */
    @Deprecated
    static String abbreviate(final String str, final int offset, final int maxWidth) {
        return abbreviate(str, "...", offset, maxWidth);
    }

    // Abbreviating
    //-----------------------------------------------------------------------

    /**
     * <p>Abbreviates a String using ellipses. This will turn
     * "Now is the time for all good men" into "Now is the time for..."</p>
     *
     * <p>Specifically:</p>
     * <ul>
     *   <li>If the number of characters in {@code str} is less than or equal to
     *       {@code maxWidth}, return {@code str}.</li>
     *   <li>Else abbreviate it to {@code (substring(str, 0, max-3) + "...")}.</li>
     *   <li>If {@code maxWidth} is less than {@code 4}, throw an
     *       {@code IllegalArgumentException}.</li>
     *   <li>In no case will it return a String of length greater than
     *       {@code maxWidth}.</li>
     * </ul>
     *
     * <pre>
     * Strings.abbreviate(null, 4)        = null
     * Strings.abbreviate("", 4)        = ""
     * Strings.abbreviate("abcdefg", 6) = "abc..."
     * Strings.abbreviate("abcdefg", 7) = "abcdefg"
     * Strings.abbreviate("abcdefg", 8) = "abcdefg"
     * Strings.abbreviate("abcdefg", 4) = "a..."
     * Strings.abbreviate("abcdefg", 3) = IllegalArgumentException
     * </pre>
     *
     * @param str the String to check, which may be null
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     */
    public static String abbreviate(final String str, final int maxWidth) {
        return abbreviate(str, "...", 0, maxWidth);
    }

    /**
     * <p>Abbreviates a String using another given String as replacement marker. This will turn
     * "Now is the time for all good men" into "Now is the time for..." if "..." was defined
     * as the replacement marker.</p>
     *
     * <p>Specifically:</p>
     * <ul>
     *   <li>If the number of characters in {@code str} is less than or equal to
     *       {@code maxWidth}, return {@code str}.</li>
     *   <li>Else abbreviate it to {@code (substring(str, 0, max-abbrevMarker.length) + abbrevMarker)}.</li>
     *   <li>If {@code maxWidth} is less than {@code abbrevMarker.length + 1}, throw an
     *       {@code IllegalArgumentException}.</li>
     *   <li>In no case will it return a String of length greater than
     *       {@code maxWidth}.</li>
     * </ul>
     *
     * <pre>
     * Strings.abbreviate(null, "...", 4)        = null
     * Strings.abbreviate("", "...", 4)        = ""
     * Strings.abbreviate("abcdefg", {@code null}, *)  = "abcdefg"
     * Strings.abbreviate("abcdefg", ".", 5)   = "abcd."
     * Strings.abbreviate("abcdefg", ".", 7)   = "abcdefg"
     * Strings.abbreviate("abcdefg", ".", 8)   = "abcdefg"
     * Strings.abbreviate("abcdefg", "..", 4)  = "ab.."
     * Strings.abbreviate("abcdefg", "..", 3)  = "a.."
     * Strings.abbreviate("abcdefg", "..", 2)  = IllegalArgumentException
     * Strings.abbreviate("abcdefg", "...", 3) = IllegalArgumentException
     * </pre>
     *
     * @param str the String to check, which may be null
     * @param abbrevMarker the String used as replacement marker
     * @param maxWidth maximum length of result String, must be at least {@code abbrevMarker.length + 1}
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     */
    public static String abbreviate(final String str, final String abbrevMarker, final int maxWidth) {
        return abbreviate(str, abbrevMarker, 0, maxWidth);
    }

    /**
     * <p>Abbreviates a String using a given replacement marker. This will turn
     * "Now is the time for all good men" into "...is the time for..." if "..." was defined
     * as the replacement marker.</p>
     *
     * <p>Works like {@code abbreviate(String, String, int)}, but allows you to specify
     * a "left edge" offset.  Note that this left edge is not necessarily going to
     * be the leftmost character in the result, or the first character following the
     * replacement marker, but it will appear somewhere in the result.
     *
     * <p>In no case will it return a String of length greater than {@code maxWidth}.</p>
     *
     * <pre>
     * Strings.abbreviate(null, "...", 0, 4)                  = null
     * Strings.abbreviate("", "...", 0, 4)                  = ""
     * Strings.abbreviate("abcdefghijklmno", {@code null}, *, *)    = "abcdefghijklmno"
     * Strings.abbreviate("abcdefghijklmno", "---", -1, 10) = "abcdefg---"
     * Strings.abbreviate("abcdefghijklmno", ",", 0, 10)    = "abcdefghi,"
     * Strings.abbreviate("abcdefghijklmno", ",", 1, 10)    = "abcdefghi,"
     * Strings.abbreviate("abcdefghijklmno", ",", 2, 10)    = "abcdefghi,"
     * Strings.abbreviate("abcdefghijklmno", "::", 4, 10)   = "::efghij::"
     * Strings.abbreviate("abcdefghijklmno", "...", 6, 10)  = "...ghij..."
     * Strings.abbreviate("abcdefghijklmno", "*", 9, 10)    = "*ghijklmno"
     * Strings.abbreviate("abcdefghijklmno", "'", 10, 10)   = "'ghijklmno"
     * Strings.abbreviate("abcdefghijklmno", "!", 12, 10)   = "!ghijklmno"
     * Strings.abbreviate("abcdefghij", "abra", 0, 4)       = IllegalArgumentException
     * Strings.abbreviate("abcdefghij", "...", 5, 6)        = IllegalArgumentException
     * </pre>
     *
     * @param str the String to check, which may be null
     * @param abbrevMarker the String used as replacement marker
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     * @deprecated
     */
    @Deprecated
    static String abbreviate(final String str, final String abbrevMarker, int offset, final int maxWidth) {
        final int abbrevMarkerLength = N.len(abbrevMarker);
        final int minAbbrevWidth = abbrevMarkerLength + 1;
        final int minAbbrevWidthOffset = abbrevMarkerLength + abbrevMarkerLength + 1;

        if (maxWidth < minAbbrevWidth) {
            throw new IllegalArgumentException(String.format("Minimum abbreviation width is %d", minAbbrevWidth));
        }

        if (isNotEmpty(str) && EMPTY.equals(abbrevMarker) && maxWidth > 0) {
            return Strings.substring(str, 0, maxWidth);
        } else if (isAnyEmpty(str, abbrevMarker)) {
            return str;
        }

        final int strLen = str.length();

        if (strLen <= maxWidth) {
            return str;
        }

        if (offset > strLen) {
            offset = strLen;
        }

        if (strLen - offset < maxWidth - abbrevMarkerLength) {
            offset = strLen - (maxWidth - abbrevMarkerLength);
        }

        if (offset <= abbrevMarkerLength + 1) {
            return str.substring(0, maxWidth - abbrevMarkerLength) + abbrevMarker;
        }

        if (maxWidth < minAbbrevWidthOffset) {
            throw new IllegalArgumentException(String.format("Minimum abbreviation width with offset is %d", minAbbrevWidthOffset));
        }

        if (offset + maxWidth - abbrevMarkerLength < strLen) {
            return abbrevMarker + abbreviate(str.substring(offset), abbrevMarker, maxWidth - abbrevMarkerLength);
        }

        return abbrevMarker + str.substring(strLen - (maxWidth - abbrevMarkerLength));
    }

    /**
     * <p>Abbreviates a String to the length passed, replacing the middle characters with the supplied
     * replacement String.</p>
     *
     * <p>This abbreviation only occurs if the following criteria is met:</p>
     * <ul>
     * <li>Neither the String for abbreviation nor the replacement String are {@code null} or empty </li>
     * <li>The length to truncate to is less than the length of the supplied String</li>
     * <li>The length to truncate to is greater than 0</li>
     * <li>The abbreviated String will have enough room for the length supplied replacement String
     * and the first and last characters of the supplied String for abbreviation</li>
     * </ul>
     * <p>Otherwise, the returned String will be the same as the supplied String for abbreviation.
     * </p>
     *
     * <pre>
     * Strings.abbreviateMiddle(null, {@code null}, 0)      = null
     * Strings.abbreviateMiddle("abc", {@code null}, 0)      = "abc"
     * Strings.abbreviateMiddle("abc", ".", 0)      = "abc"
     * Strings.abbreviateMiddle("abc", ".", 3)      = "abc"
     * Strings.abbreviateMiddle("abcdef", ".", 4)     = "ab.f"
     * </pre>
     *
     * @param str the String to abbreviate, which may be null
     * @param middle the String to replace the middle characters with, which may be null
     * @param length the length to abbreviate {@code str} to.
     * @return the abbreviated String if the above criteria is met, or the original String supplied for abbreviation.
     */
    public static String abbreviateMiddle(final String str, final String middle, final int length) {
        if (isAnyEmpty(str, middle) || length >= str.length() || length < middle.length() + 2) {
            return str;
        }

        final int targetSting = length - middle.length();
        final int startOffset = targetSting / 2 + targetSting % 2;
        final int endOffset = str.length() - targetSting / 2;

        return str.substring(0, startOffset) + middle + str.substring(endOffset);
    }

    // Centering
    //-----------------------------------------------------------------------

    /**
     * <p>Centers a String in a larger String of size {@code size}
     * using the space character (' ').</p>
     *
     * <p>If the size is less than the String length, the original String is returned.
     *
     * <p>Equivalent to {@code center(str, size, " ")}.</p>
     *
     * <pre>
     * Strings.center(null, 4)     = "    "
     * Strings.center("", 4)     = "    "
     * Strings.center("ab", 4)   = " ab "
     * Strings.center("abcd", 2) = "abcd"
     * Strings.center("a", 4)    = " a  "
     * </pre>
     *
     * @param str the String to center, which may be null
     * @param size the int size of new String
     * @return centered String
     */
    public static String center(final String str, final int size) {
        return center(str, size, ' ');
    }

    /**
     * <p>Centers a String in a larger String of size {@code size}.
     * Uses a supplied character as the value to pad the String with.</p>
     *
     * <p>If the size is less than the String length, the String is returned.
     *
     * <pre>
     * Strings.center(null, 4, ' ')     = "    "
     * Strings.center("", 4, ' ')     = "    "
     * Strings.center("ab", 4, ' ')   = " ab "
     * Strings.center("abcd", 2, ' ') = "abcd"
     * Strings.center("a", 4, ' ')    = " a  "
     * Strings.center("a", 4, 'y')    = "yayy"
     * </pre>
     *
     * @param str the String to center, which may be null
     * @param size the int size of new String.
     * @param padChar the character to pad the new String with
     * @return centered String
     * @throws IllegalArgumentException
     */
    public static String center(String str, final int size, final char padChar) throws IllegalArgumentException {
        N.checkArgNotNegative(size, cs.size);

        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= size) {
            return str;
        }

        final int strLen = str.length();
        final int pads = size - strLen;

        str = padStart(str, strLen + pads / 2, padChar);
        return padEnd(str, size, padChar);
    }

    /**
     * <p>Centers a String in a larger String of size {@code minLength}.
     * Uses a supplied String as the value to pad the String with.</p>
     *
     * <p>If the size is less than the String length, the String is returned.
     *
     * <pre>
     * Strings.center(null, 4, " ")     = "    "
     * Strings.center("", 4, " ")     = "    "
     * Strings.center("ab", 4, " ")   = " ab "
     * Strings.center("abcd", 2, " ") = "abcd"
     * Strings.center("a", 4, " ")    = " a  "
     * Strings.center("a", 4, "yz")   = "yzayz"
     * Strings.center("abc", 7, "")   = "  abc  "
     * </pre>
     *
     * @param str the String to center, which may be null
     * @param minLength the minimum size of new String.
     * @param padStr the String to pad the new String with, must not be {@code null} or empty
     * @return centered String
     * @throws IllegalArgumentException
     */
    public static String center(String str, final int minLength, String padStr) throws IllegalArgumentException {
        N.checkArgNotNegative(minLength, cs.minLength);
        // N.checkArgNotEmpty(padStr, "padStr");

        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= minLength) {
            return str;
        }

        if (isEmpty(padStr)) {
            padStr = " ";
        }

        final int strLen = str.length();
        final int pads = minLength - strLen;

        str = padStart(str, strLen + pads / 2, padStr);
        return padEnd(str, minLength, padStr);
    }

    /**
     * Pads the given string from the start (left) with spaces until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null} or empty.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @return A new string that is a copy of the original string padded with leading spaces so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padStart(final String str, final int minLength) {
        return padStart(str, minLength, WD._SPACE);
    }

    /**
     * Pads the given string from the start (left) with the specified character until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null}, in which case it will be treated as an empty string.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @param padChar The character to be used for padding.
     * @return A new string that is a copy of the original string padded with the padChar so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padStart(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= minLength) {
            return str;
        }

        final String padStr = Strings.repeat(padChar, minLength - str.length());

        return concat(padStr, str);
    }

    /**
     * Pads the given string from the start (left) with the specified string until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null}, in which case it will be treated as an empty string.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @param padStr The string to be used for padding.
     * @return A new string that is a copy of the original string padded with the padStr so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padStart(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= minLength) {
            return str;
        }

        @SuppressWarnings("DuplicateExpressions")
        final int delta = ((minLength - str.length()) % padStr.length() == 0) ? ((minLength - str.length()) / padStr.length())
                : ((minLength - str.length()) / padStr.length() + 1);
        switch (delta) {
            case 1:
                return padStr + str;

            case 2:
                return padStr + padStr + str;

            case 3:
                return padStr + padStr + padStr + str;

            default: {
                final StringBuilder sb = Objectory.createStringBuilder(str.length() + (padStr.length() * delta));

                try {
                    //noinspection StringRepeatCanBeUsed
                    for (int i = 0; i < delta; i++) {
                        sb.append(padStr);
                    }

                    sb.append(str);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
            }
        }
    }

    /**
     * Pads the given string from the end (right) with spaces until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null}, in which case it will be treated as an empty string.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @return A new string that is a copy of the original string padded with trailing spaces so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padEnd(final String str, final int minLength) {
        return padEnd(str, minLength, WD._SPACE);
    }

    /**
     * Pads the given string from the end (right) with the specified character until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null}, in which case it will be treated as an empty string.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @param padChar The character to be used for padding.
     * @return A new string that is a copy of the original string padded with the padChar so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padEnd(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= minLength) {
            return str;
        }

        final String padStr = Strings.repeat(padChar, minLength - str.length());

        return concat(str, padStr);
    }

    /**
     * Pads the given string from the end (right) with the specified string until the string reaches the specified minimum length.
     * If the length of the given string is already greater than or equal to the specified minimum length, the original string is returned.
     *
     * @param str The string to be padded. It can be {@code null}, in which case it will be treated as an empty string.
     * @param minLength The minimum length the string should have after padding. Must be non-negative.
     * @param padStr The string to be used for padding.
     * @return A new string that is a copy of the original string padded with the padStr so that it reaches the specified minimum length.
     *         If the original string is already greater than or equal to the specified minimum length, the original string is returned.
     */
    public static String padEnd(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = EMPTY;
        }

        if (str.length() >= minLength) {
            return str;
        }

        @SuppressWarnings("DuplicateExpressions")
        final int delta = ((minLength - str.length()) % padStr.length() == 0) ? ((minLength - str.length()) / padStr.length())
                : ((minLength - str.length()) / padStr.length() + 1);

        switch (delta) {
            case 1:
                return str + padStr;

            case 2:
                return str + padStr + padStr;

            case 3:
                return str + padStr + padStr + padStr;

            default: {
                final StringBuilder sb = Objectory.createStringBuilder(str.length() + (padStr.length() * delta));

                try {
                    sb.append(str);

                    //noinspection StringRepeatCanBeUsed
                    for (int i = 0; i < delta; i++) {
                        sb.append(padStr);
                    }

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
            }
        }
    }

    /**
     * Repeats the given character a specified number of times and returns the resulting string.
     *
     * @param ch The character to be repeated.
     * @param n The number of times the character should be repeated. Must be non-negative.
     * @return A string consisting of the given character repeated n times.
     * @throws IllegalArgumentException if n is negative.
     */
    public static String repeat(final char ch, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return EMPTY;
        } else if (n == 1) {
            return N.stringOf(ch);
        }

        //    if (n < 16) {
        //        final char[] array = new char[n];
        //        Arrays.fill(array, ch);
        //
        //        return InternalUtil.newString(array, true);
        //    } else {
        //        final char[] array = new char[n];
        //        array[0] = ch;
        //
        //        int cnt = 1;
        //
        //        for (; cnt < n - cnt; cnt <<= 1) {
        //            N.copy(array, 0, array, cnt, cnt);
        //        }
        //
        //        if (cnt < n) {
        //            N.copy(array, 0, array, cnt, n - cnt);
        //        }
        //
        //        return InternalUtil.newString(array, true);
        //    }

        return N.stringOf(ch).repeat(n);
    }

    /**
     * Repeats the given character a specified number of times, separated by a specified delimiter, and returns the resulting string.
     *
     * @param ch The character to be repeated.
     * @param n The number of times the character should be repeated. Must be non-negative.
     * @param delimiter The character used to separate the repeated characters.
     * @return A string consisting of the given character repeated n times, separated by the delimiter.
     * @throws IllegalArgumentException if n is negative.
     * @see #repeat(char, int, char)
     * @see #repeat(String, int, String)
     */
    public static String repeat(final char ch, final int n, final char delimiter) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        return repeat(N.stringOf(ch), n, N.stringOf(delimiter));
    }

    /**
     * Repeats the given string a specified number of times and returns the resulting string.
     *
     * @param str The string to be repeated. It can be {@code null} or empty.
     * @param n The number of times the string should be repeated. Must be non-negative.
     * @return A string consisting of the given string repeated n times.
     * @throws IllegalArgumentException if n is negative.
     * @see #repeat(char, int, char)
     * @see #repeat(String, int, String)
     */
    public static String repeat(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (N.isEmpty(str) || n == 0) {
            return EMPTY;
        } else if (n == 1) {
            return str;
        }

        return str.repeat(n);
    }

    /**
     * Repeats the given string a specified number of times, separated by a specified delimiter, and returns the resulting string.
     *
     * @param str The string to be repeated. It can be {@code null} or empty.
     * @param n The number of times the string should be repeated. Must be non-negative.
     * @param delimiter The string used to separate the repeated strings.
     * @return A string consisting of the given string repeated n times, separated by the delimiter.
     * @throws IllegalArgumentException if n is negative.
     */
    public static String repeat(final String str, final int n, final String delimiter) throws IllegalArgumentException {
        if (N.isEmpty(delimiter)) {
            return repeat(str, n);
        }

        return repeat(str, n, delimiter, EMPTY, EMPTY);
    }

    /**
     * Repeats the given string a specified number of times, separated by a specified delimiter, and returns the resulting string.
     * The resulting string is also prefixed and suffixed with the provided strings.
     *
     * @param str The string to be repeated. It can be {@code null} or empty.
     * @param n The number of times the string should be repeated. Must be non-negative.
     * @param delimiter The string used to separate the repeated strings.
     * @param prefix The string to be added at the start of the resulting string.
     * @param suffix The string to be added at the end of the resulting string.
     * @return A string consisting of the prefix, the given string repeated n times separated by the delimiter, and the suffix.
     * @throws IllegalArgumentException if n is negative.
     */
    public static String repeat(String str, final int n, String delimiter, String prefix, String suffix) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        str = str == null ? EMPTY : str;
        delimiter = delimiter == null ? EMPTY : delimiter;
        prefix = prefix == null ? EMPTY : prefix;
        suffix = suffix == null ? EMPTY : suffix;

        if (n == 0 || (isEmpty(str) && isEmpty(delimiter))) {
            return concat(prefix + suffix);
        } else if (n == 1) {
            return concat(prefix, str, suffix);
        }

        return join(Array.repeat(str, n), delimiter, prefix, suffix);
    }

    /**
     * Returns the byte array returned by {@code String.getBytes()}, or {@code null} if the specified String is {@code null}.
     *
     * @param string The input string to be converted. It can be {@code null}.
     * @return A byte array representation of the input string using the default charset, or {@code null} if the input string is {@code null}.
     */
    @MayReturnNull
    public static byte[] getBytes(final String string) {
        return string == null ? null : string.getBytes();
    }

    /**
     * Returns the byte array returned by {@code String#getBytes(Charset)}, or {@code null} if the specified String is {@code null}.
     *
     * @param string
     * @param charset
     * @return the encoded bytes
     */
    @MayReturnNull
    public static byte[] getBytes(final String string, final Charset charset) {
        return string == null ? null : string.getBytes(charset);
    }

    /**
     * Returns the byte array returned by {@code String#getBytes(Charsets.UTF_8)}, or {@code null} if the specified String is {@code null}.
     *
     * @param string
     * @return
     */
    @MayReturnNull
    public static byte[] getBytesUtf8(final String string) {
        return getBytes(string, Charsets.UTF_8);
    }

    /**
     * Returns the char array of the specified CharSequence, or {@code null} if the specified String is {@code null}.
     *
     * @param source The input CharSequence to be converted. It can be {@code null}.
     * @return A char array representation of the input CharSequence. Returns {@code null} if the input CharSequence is {@code null}.
     */
    @MayReturnNull
    public static char[] toCharArray(final CharSequence source) {
        if (source == null) {
            return null; // NOSONAR
        } else if (source.isEmpty()) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (source instanceof String) {
            return ((String) source).toCharArray();
        }

        final int len = N.len(source);

        final char[] array = new char[len];

        for (int i = 0; i < len; i++) {
            array[i] = source.charAt(i);
        }

        return array;
    }

    /**
     * <p>Converts a {@code CharSequence} into an array of code points.</p>
     *
     * <p>Valid pairs of surrogate code units will be converted into a single supplementary
     * code point. Isolated surrogate code units (i.e., a high surrogate not followed by a low surrogate or
     * a low surrogate not preceded by a high surrogate) will be returned as-is.</p>
     *
     * <pre>
     * Strings.toCodePoints(null)   =  null
     * Strings.toCodePoints("")     =  []  // empty array
     * </pre>
     *
     * @param str the character sequence to convert
     * @return
     */
    @MayReturnNull
    public static int[] toCodePoints(final CharSequence str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty()) {
            return N.EMPTY_INT_ARRAY;
        }

        final String s = str.toString();
        //    final int[] result = new int[s.codePointCount(0, s.length())];
        //    int index = 0;
        //
        //    for (int i = 0; i < result.length; i++) {
        //        result[i] = s.codePointAt(index);
        //        index += Character.charCount(result[i]);
        //    }
        //
        //    return result;

        return s.codePoints().toArray();
    }

    // Case conversion
    // -----------------------------------------------------------------------

    /**
     * To lower case.
     *
     * @param ch
     * @return
     * @see Character#toLowerCase(char)
     */
    public static char toLowerCase(final char ch) {
        return Character.toLowerCase(ch);
    }

    /**
     * <p>
     * Converts a String to lower case as per {@link String#toLowerCase()}.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.toLowerCase(null)  = null
     * Strings.toLowerCase("")    = ""
     * Strings.toLowerCase("aBc") = "abc"
     * </pre>
     *
     * <p>
     * <strong>Note:</strong> As described in the documentation for
     * {@link String#toLowerCase()}, the result of this method is affected by
     * the current locale. For platform-independent case transformations, the
     * method {@link #toLowerCase(String, Locale)} should be used with a specific
     * locale (e.g., {@link Locale#ENGLISH}).
     * </p>
     *
     * @param str
     *            the String to lower case, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toLowerCase(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.toLowerCase();
    }

    /**
     * <p>
     * Converts a String to lower case as per {@link String#toLowerCase(Locale)}
     * .
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.toLowerCase(null, Locale.ENGLISH)  = null
     * Strings.toLowerCase("", Locale.ENGLISH)    = ""
     * Strings.toLowerCase("aBc", Locale.ENGLISH) = "abc"
     * </pre>
     *
     * @param str
     *            the String to lower case, which may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toLowerCase(final String str, final Locale locale) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.toLowerCase(locale);
    }

    /**
     * Converts the given string to lower case with underscores.
     * If the input string is {@code null} or empty, it returns the input string.
     *
     * @param str the input string to be converted
     * @return the converted string in lower case with underscores
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + 16);
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {//NOSONAR
                            sb.append(WD._UNDERSCORE);
                        }
                    }

                    sb.append(Character.toLowerCase(ch));
                } else {
                    //    if (i > 0 && ((isAsciiNumeric(ch) && !isAsciiNumeric(str.charAt(i - 1))) || (isAsciiNumeric(str.charAt(i - 1)) && !isAsciiNumeric(ch)))) {
                    //        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                    //            sb.append(WD._UNDERSCORE);
                    //        }
                    //    }

                    sb.append(ch);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * To upper case.
     *
     * @param ch
     * @return
     * @see Character#toUpperCase(char)
     */
    public static char toUpperCase(final char ch) {
        return Character.toUpperCase(ch);
    }

    /**
     * <p>
     * Converts a String to upper case as per {@link String#toUpperCase()}.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.toUpperCase(null)  = null
     * Strings.toUpperCase("")    = ""
     * Strings.toUpperCase("aBc") = "ABC"
     * </pre>
     *
     * <p>
     * <strong>Note:</strong> As described in the documentation for
     * {@link String#toUpperCase()}, the result of this method is affected by
     * the current locale. For platform-independent case transformations, the
     * method {@link #toLowerCase(String, Locale)} should be used with a specific
     * locale (e.g., {@link Locale#ENGLISH}).
     * </p>
     *
     * @param str the String to upper case, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toUpperCase(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.toUpperCase();
    }

    /**
     * <p>
     * Converts a String to upper case as per {@link String#toUpperCase(Locale)}
     * .
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.toUpperCase(null, Locale.ENGLISH)  = null
     * Strings.toUpperCase("", Locale.ENGLISH)    = ""
     * Strings.toUpperCase("aBc", Locale.ENGLISH) = "ABC"
     * </pre>
     *
     * @param str
     *            the String to upper case, which may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toUpperCase(final String str, final Locale locale) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.toUpperCase(locale);
    }

    /**
     * Converts the given string to upper case with underscores.
     * If the input string is {@code null} or empty, it returns the input string.
     *
     * @param str the input string to be converted
     * @return the converted string in upper case with underscores
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + 16);
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {//NOSONAR
                            sb.append(WD._UNDERSCORE);
                        }
                    }

                    sb.append(ch);
                } else {
                    //    if (i > 0 && ((isAsciiNumeric(ch) && !isAsciiNumeric(str.charAt(i - 1))) || (isAsciiNumeric(str.charAt(i - 1)) && !isAsciiNumeric(ch)))) {
                    //        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                    //            sb.append(WD._UNDERSCORE);
                    //        }
                    //    }

                    sb.append(Character.toUpperCase(ch));
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Converts the specified string to camel case.
     * 
     * <p>
     * <code>
     * Strings.toCamelCase("first_name")  ==> "firstName"
     * </code>
     * </p>
     *
     * @param str The input string to be converted. It can be {@code null} or empty.
     * @return A camel case representation of the input string. Returns the original string if it's {@code null} or empty.
     */
    public static String toCamelCase(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        String[] substrs = null;
        int idx = str.indexOf('_');

        if (idx < 0) {
            idx = str.indexOf('-');
        }

        if (idx >= 0) {
            substrs = RegExUtil.split(str, RegExUtil.CAMEL_CASE_SEPARATOR);
        }

        return toCamelCase(str, idx, substrs);
    }

    /**
     * Converts the specified string to camel case.
     * 
     * <p>
     * <code>
     * Strings.toCamelCase("first_name")  ==> "firstName"
     * </code>
     * </p>
     *
     * @param str The input string to be converted. It can be {@code null} or empty.
     * @param splitChar The character used to split the input string.
     * @return A camel case representation of the input string. Returns the original string if it's {@code null} or empty.
     */
    public static String toCamelCase(final String str, final char splitChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        String[] substrs = null;
        final int idx = str.indexOf(splitChar);

        if (idx >= 0) {
            substrs = Strings.split(str, splitChar);
        }

        return toCamelCase(str, idx, substrs);
    }

    private static String toCamelCase(final String str, final int firstSplitorIndex, final String[] substrs) {
        if (firstSplitorIndex >= 0) {
            final StringBuilder sb = Objectory.createStringBuilder(str.length());

            try {
                boolean first = true;

                for (final String substr : substrs) {
                    if (isNotEmpty(substr)) {
                        sb.append(substr.toLowerCase());

                        if (!first) {
                            sb.setCharAt(sb.length() - substr.length(), Character.toUpperCase(substr.charAt(0)));
                        } else {
                            first = false;
                        }
                    }
                }

                return sb.toString();
            } finally {
                Objectory.recycle(sb);
            }
        } else {
            for (int i = 0, len = str.length(); i < len; i++) {
                if (Character.isLowerCase(str.charAt(i))) {
                    if (i == 1) {
                        return str.substring(0, 1).toLowerCase() + str.substring(1);
                    } else if (i > 1) {
                        return str.substring(0, i - 1).toLowerCase() + str.substring(i - 1);
                    }

                    break;
                } else if ((i + 1) == str.length()) {
                    return str.toLowerCase();
                }
            }

            return str;
        }
    }

    /**
     * Converts the specified string to Pascal case.
     * 
     * <p>
     * <code>
     * Strings.toPascalCase("first_name")  ==> "FirstName"
     * </code>
     * </p>
     *
     * @param str The input string to be converted. It can be {@code null} or empty.
     * @return A camel case representation of the input string. Returns the original string if it's {@code null} or empty.
     */
    public static String toPascalCase(final String str) {
        return toPascalCase(str, WD._UNDERSCORE);
    }

    /**
     * Converts the specified string to Pascal case.
     * 
     * <p>
     * <code>
     * Strings.toPascalCase("first_name")  ==> "FirstName"
     * </code>
     * </p>
     *
     * @param str The input string to be converted. It can be {@code null} or empty.
     * @param splitChar The character used to split the input string.
     * @return A camel case representation of the input string. Returns the original string if it's {@code null} or empty.
     */
    public static String toPascalCase(final String str, final char splitChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.indexOf(splitChar) >= 0) {
            final String[] substrs = Strings.split(str, splitChar);
            final StringBuilder sb = Objectory.createStringBuilder(str.length());

            try {
                for (final String substr : substrs) {
                    if (isNotEmpty(substr)) {
                        sb.append(substr.toLowerCase());
                        sb.setCharAt(sb.length() - substr.length(), Character.toUpperCase(substr.charAt(0)));
                    }
                }

                return sb.toString();
            } finally {
                Objectory.recycle(sb);
            }
        }

        if (Character.isLowerCase(str.charAt(0))) {
            if (str.length() == 1) {
                return str.toUpperCase();
            } else {
                return str.substring(0, 1).toUpperCase() + str.substring(1);
            }
        }

        return str;
    }

    /**
     * Swaps the case of a character changing upper and title case to lower case, and lower case to upper case.
     *
     * @param ch The input character to be case-swapped.
     * @return The case-swapped representation of the input character.
     */
    public static char swapCase(final char ch) {
        return Character.isUpperCase(ch) || Character.isTitleCase(ch) ? Character.toLowerCase(ch)
                : (Character.isLowerCase(ch) ? Character.toUpperCase(ch) : ch);
    }

    /**
     * <p>
     * Swaps the case of a String changing upper and title case to lower case, and lower case to upper case.
     * </p>
     *
     * <ul>
     * <li>Upper case character converts to Lower case</li>
     * <li>Title case character converts to Lower case</li>
     * <li>Lower case character converts to Upper case</li>
     * </ul>
     *
     * <p>
     * For a word based algorithm, see
     * {@link org.apache.commons.lang3.text.WordUtils#swapCase(String)}. A
     * {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.swapCase(null)                 = null
     * Strings.swapCase("")                   = ""
     * Strings.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     *
     * <p>
     * NOTE: This method changed in Lang version 2.0. It no longer performs a
     * word based algorithm. If you only use ASCII, you will notice no change.
     * That functionality is available in
     * org.apache.commons.lang3.text.WordUtils.
     * </p>
     *
     * @param str the String to swap case, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String swapCase(final String str) {
        if (isEmpty(str)) {
            return str;
        }

        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        int oldCodepoint, newCodePoint;

        for (int i = 0; i < strLen; i += Character.charCount(newCodePoint)) {
            oldCodepoint = str.codePointAt(i);

            if (Character.isUpperCase(oldCodepoint) || Character.isTitleCase(oldCodepoint)) {
                newCodePoint = Character.toLowerCase(oldCodepoint);
            } else if (Character.isLowerCase(oldCodepoint)) {
                newCodePoint = Character.toUpperCase(oldCodepoint);
            } else {
                newCodePoint = oldCodepoint;
            }

            newCodePoints[outOffset++] = newCodePoint;
        }

        return new String(newCodePoints, 0, outOffset);
    }

    // Copied from Apache commons Lang under Apache License v2.

    /**
     * Converts the first character of the given string to lower case.
     * If the string is {@code null} or empty, the original string is returned.
     *
     * @param str The string to be uncapitalized. It can be {@code null} or empty.
     * @return A string with its first character converted to lower case.
     *         If the original string is already starting with a lower case character, the original string is returned.
     */
    public static String uncapitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final int firstCodePoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodePoint);

        if (firstCodePoint == newCodePoint) {
            // already uncapitalize
            return str;
        }

        final int strLen = str.length();

        final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first code point

        for (int inOffset = Character.charCount(firstCodePoint); inOffset < strLen;) {
            final int codePoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codePoint; // copy the remaining ones
            inOffset += Character.charCount(codePoint);
        }

        return new String(newCodePoints, 0, outOffset);
    }

    // Copied from Apache commons Lang under Apache License v2.

    /**
     * Converts the first character of the given string to upper case.
     * If the string is {@code null} or empty, the original string is returned.
     *
     * @param str The string to be capitalized. It can be {@code null} or empty.
     * @return A string with its first character converted to upper case.
     *         If the original string is already starting with an upper case character, the original string is returned.
     */
    public static String capitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toTitleCase(firstCodepoint);

        if (firstCodepoint == newCodePoint) {
            // already capitalized
            return str;
        }

        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint; // copy the first code point

        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen;) {
            final int codePoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codePoint; // copy the remaining ones
            inOffset += Character.charCount(codePoint);
        }

        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * Capitalize all the words from the specified split by {@code ' '}.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String capitalizeFully(final String str) {
        return capitalizeFully(str, " ");
    }

    /**
     * Capitalize all the words from the specified split by {@code delimiter}.
     *
     * @param str
     * @param delimiter
     * @return the specified String if it's {@code null} or empty.
     * @throws IllegalArgumentException
     * @see #convertWords(String, String, Collection, Function)
     */
    public static String capitalizeFully(final String str, final String delimiter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter); // NOSONAR

        if (str == null || str.isEmpty()) {
            return str;
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);

        for (int i = 0; i < words.length; i++) {
            words[i] = capitalize(words[i]);
        }

        return join(words, delimiter);
    }

    /**  
     * Capitalizes all the words in the given string, split by the provided delimiter, excluding the specified words except the first word.
     *
     * @param str The string to be processed. If it's {@code null} or empty, the method will return the input string.
     * @param delimiter The delimiter used to split the string into words. It must not be empty.
     * @param excludedWords An array of words to be excluded from capitalization. If it's {@code null} or empty, all words will be capitalized.
     * @return The processed string with all non-excluded words capitalized.
     * @throws IllegalArgumentException if the provided delimiter is empty.
     */
    public static String capitalizeFully(final String str, final String delimiter, final String... excludedWords) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter); // NOSONAR

        if (str == null || str.isEmpty()) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return capitalizeFully(str, delimiter);
        }

        return capitalizeFully(str, delimiter, N.toSet(excludedWords));
    }

    /**
     * Capitalizes all the words in the given string, split by the provided delimiter, excluding the words in the excludedWords collection except the first word.
     *
     * @param str The string to be processed. If it's {@code null} or empty, the method will return the input string.
     * @param delimiter The delimiter used to split the string into words. It must not be empty.
     * @param excludedWords A collection of words to be excluded from capitalization. If it's {@code null} or empty, all words will be capitalized.
     * @return The processed string with all non-excluded words capitalized.
     * @throws IllegalArgumentException if the provided delimiter is empty.
     * @see #convertWords(String, String, Collection, Function)
     */
    public static String capitalizeFully(final String str, final String delimiter, final Collection<String> excludedWords) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter); // NOSONAR

        if (str == null || str.isEmpty()) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return capitalizeFully(str, delimiter);
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);
        final Collection<String> excludedWordSet = excludedWords instanceof Set || (excludedWords.size() <= 3 && words.length <= 3) ? excludedWords
                : N.newHashSet(excludedWords);

        for (int i = 0, len = words.length; i < len; i++) {
            words[i] = i != 0 && excludedWordSet.contains(words[i]) ? words[i] : capitalize(words[i]);
        }

        return join(words, delimiter);
    }

    /**
     * Converts all the words in the given string using the provided converter function.
     * The words are identified by splitting the string on space characters.
     *
     * @param str The string to be processed. If it's {@code null} or empty, the method will return the input string.
     * @param converter The function used to convert each word. This function should accept a string and return a string.
     * @return The processed string with all words converted using the provided converter function.
     */
    public static String convertWords(final String str, final Function<? super String, String> converter) {
        return convertWords(str, " ", converter);
    }

    /**
     * Converts all the words from the specified string, split by the provided delimiter, using the provided converter function.
     *
     * @param str The string to be processed. If it's {@code null} or empty, the method will return the input string.
     * @param delimiter The delimiter used to split the string into words. It must not be empty.
     * @param converter The function used to convert each word.
     * @return The processed string with all words converted using the provided converter function.
     * @throws IllegalArgumentException if the provided delimiter is empty.
     */
    public static String convertWords(final String str, final String delimiter, final Function<? super String, String> converter)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter); // NOSONAR

        if (str == null || str.isEmpty()) {
            return str;
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);

        for (int i = 0, len = words.length; i < len; i++) {
            words[i] = converter.apply(words[i]);
        }

        return join(words, delimiter);
    }

    /**
     * Converts all the words from the specified string, split by the provided delimiter, using the provided converter function.
     * Words that are present in the excludedWords collection are not converted.
     *
     * @param str The string to be processed. If it's {@code null} or empty, the method will return the input string.
     * @param delimiter The delimiter used to split the string into words. It must not be empty.
     * @param excludedWords A collection of words to be excluded from conversion. If it's {@code null} or empty, all words will be converted.
     * @param converter The function used to convert each word. If a word is in the excludedWords collection, it will not be converted.
     * @return The processed string with all non-excluded words converted using the provided converter function.
     * @throws IllegalArgumentException if the provided delimiter is empty.
     */
    public static String convertWords(final String str, final String delimiter, final Collection<String> excludedWords,
            final Function<? super String, String> converter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter); // NOSONAR

        if (str == null || str.isEmpty()) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return convertWords(str, delimiter, converter);
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);
        final Collection<String> excludedWordSet = excludedWords instanceof Set || (excludedWords.size() <= 3 && words.length <= 3) ? excludedWords
                : N.newHashSet(excludedWords);

        for (int i = 0, len = words.length; i < len; i++) {
            words[i] = excludedWordSet.contains(words[i]) ? words[i] : converter.apply(words[i]);
        }

        return join(words, delimiter);
    }

    /**
     * Replaces ''' or '"' with <i>\'</i> or <i>\"</i> if the previous char of the
     * quotation is not '\'. The original String is returned if the specified String
     * is {@code null} or empty.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String quoteEscaped(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + 16);

        try {
            char ch = 0;
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if ((ch == _BACKSLASH) && (i < (len - 1))) {
                    sb.append(ch);
                    sb.append(str.charAt(++i));
                } else {
                    if ((ch == _QUOTATION_S) || (ch == _QUOTATION_D)) {
                        sb.append(_BACKSLASH);
                    }
                    sb.append(ch);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Escapes the specified quotation character in the given string if it is not already escaped.
     *
     * @param str the input string to be processed, may be {@code null} or empty
     * @param quoteChar the quotation character to be escaped, should be either {@code "} or {@code '}
     * @return the processed string with the specified quotation character escaped, or the original string if it is {@code null} or empty
     */
    public static String quoteEscaped(final String str, final char quoteChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + 16);

        try {
            char ch = 0;
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if ((ch == _BACKSLASH) && (i < (len - 1))) {
                    sb.append(ch);
                    sb.append(str.charAt(++i));
                } else {
                    if (ch == quoteChar) {
                        sb.append(_BACKSLASH);
                    }
                    sb.append(ch);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    // --------------------------------------------------------------------------

    /**
     * <p>
     * Converts the char to the unicode format '\u0020'.
     * </p>
     *
     * <p>
     * This format is the Java source code format.
     * </p>
     *
     * <pre>
     *   Strings.unicodeEscaped(' ') = "\u0020"
     *   Strings.unicodeEscaped('A') = "\u0041"
     * </pre>
     *
     * @param ch
     *            the character to convert
     * @return
     */
    public static String unicodeEscaped(final char ch) {
        if (ch < 0x10) {
            return "\\u000" + Integer.toHexString(ch);
        } else if (ch < 0x100) {
            return "\\u00" + Integer.toHexString(ch);
        } else if (ch < 0x1000) {
            return "\\u0" + Integer.toHexString(ch);
        }

        return "\\u" + Integer.toHexString(ch);
    }

    /**
     * <p>
     * Similar to <a
     * href="http://www.w3.org/TR/xpath/#function-normalize-space">
     * http://www.w3.org/TR/xpath/#function-normalize -space</a>
     * </p>
     * <p>
     * The function returns the argument string with whitespace normalized by
     * using <code>{@link #trim(String)}</code> to remove leading and trailing
     * whitespace and then replacing sequences of whitespace characters by a
     * single space.
     * </p>
     * In XML Whitespace characters are the same as those allowed by the <a
     * href="http://www.w3.org/TR/REC-xml/#NT-S">S</a> production, which is S
     * ::= (#x20 | #x9 | #xD | #xA)+
     * <p>
     * Java's regexp pattern \s defines whitespace as [ \t\n\x0B\f\r]
     *
     * <p>
     * For reference:
     * </p>
     * <ul>
     * <li>\x0B = vertical tab</li>
     * <li>\f = #xC = form feed</li>
     * <li>#x20 = space</li>
     * <li>#x9 = \t</li>
     * <li>#xA = \n</li>
     * <li>#xD = \r</li>
     * </ul>
     *
     * <p>
     * The difference is that Java's whitespace includes vertical tab and form
     * feed, which this functional will also normalize. Additionally
     * <code>{@link #trim(String)}</code> removes control characters (char &lt;=
     * 32) from both ends of this String.
     * </p>
     *
     * @param str the source String to normalize whitespaces from, which may be null
     * @return
     *         {@code null} String input
     * @see Pattern
     * @see #trim(String)
     * @see <a
     *      href="http://www.w3.org/TR/xpath/#function-normalize-space">http://www.w3.org/TR/xpath/#function-normalize-space</a>
     */
    public static String normalizeSpace(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return WHITESPACE_PATTERN.matcher(str.trim()).replaceAll(WD.SPACE);
    }

    /**
     * <p>
     * Replaces all occurrences of a String within another String.
     * </p>
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * Strings.replaceAll(null, *, *)        = null
     * Strings.replaceAll("", *, *)          = ""
     * Strings.replaceAll("any", {@code null}, *)    = "any"
     * Strings.replaceAll("any", *, null)    = "any"
     * Strings.replaceAll("any", "", *)      = "any"
     * Strings.replaceAll("aba", "a", null)  = "b"
     * Strings.replaceAll("aba", "a", "")    = "b"
     * Strings.replaceAll("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param str text to search and replace in, which may be null
     * @param target the String to search for, which may be null
     * @param replacement the String to replace it with, which may be null
     * @return
     */
    public static String replaceAll(final String str, final String target, final String replacement) {
        return replaceAll(str, 0, target, replacement);
    }

    /**
     * Replaces all occurrences of a target string in the input string with a replacement string, starting from a specified index.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with all occurrences of the target string replaced with the replacement string, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceAll(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, -1);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string.
     *
     * <pre>
     * Strings.replaceFirst(null, *, *)        = null
     * Strings.replaceFirst("", *, *)          = ""
     * Strings.replaceFirst("any", {@code null}, *)    = "any"
     * Strings.replaceFirst("any", *, null)    = "any"
     * Strings.replaceFirst("any", "", *)      = "any"
     * Strings.replaceFirst("aba", "a", null)  = "ba"
     * Strings.replaceFirst("aba", "a", "")    = "ba"
     * Strings.replaceFirst("aba", "a", "z")   = "zba"
     * </pre>
     *
     * @param str
     * @param target
     * @param replacement the String to replace with, which may be null
     * @return the text with any replacements processed,
     *  {@code null} if {@code null} String input
     */
    public static String replaceFirst(final String str, final String target, final String replacement) {
        return replaceFirst(str, 0, target, replacement);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string, starting from a specified index.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with the first occurrence of the target string replaced with the replacement string, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceFirst(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, 1);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string.
     *
     * <pre>
     * Strings.replaceOnce(null, *, *)        = null
     * Strings.replaceOnce("", *, *)          = ""
     * Strings.replaceOnce("any", {@code null}, *)    = "any"
     * Strings.replaceOnce("any", *, null)    = "any"
     * Strings.replaceOnce("any", "", *)      = "any"
     * Strings.replaceOnce("aba", "a", null)  = "ba"
     * Strings.replaceOnce("aba", "a", "")    = "ba"
     * Strings.replaceOnce("aba", "a", "z")   = "zba"
     * </pre>
     *
     * @param str The input string where the replacement should occur. It can be {@code null}.
     * @param target The string to be replaced. It can be {@code null}.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with the first occurrence of the target string replaced with the replacement string.
     *         If the input string, target string, or replacement string is {@code null}, the method returns the original string.
     * @deprecated Use {@link #replaceFirst(String, String, String)} instead
     */
    @Deprecated
    public static String replaceOnce(final String str, final String target, final String replacement) {
        return replaceFirst(str, target, replacement);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string, starting from a specified index.
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     * @deprecated Use {@link #replaceFirst(String, int, String, String)} instead
     */
    @Deprecated
    public static String replaceOnce(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceFirst(str, fromIndex, target, replacement);
    }

    /**
     * Replaces the last occurrence of a target string in the input string with a replacement string.
     *
     * <pre>
     * Strings.replaceLast(null, *, *)        = null
     * Strings.replaceLast("", *, *)          = ""
     * Strings.replaceLast("any", {@code null}, *)    = "any"
     * Strings.replaceLast("any", *, null)    = "any"
     * Strings.replaceLast("any", "", *)      = "any"
     * Strings.replaceLast("aba", "a", null)  = "ab"
     * Strings.replaceLast("aba", "a", "")    = "ab"
     * Strings.replaceLast("aba", "a", "z")   = "abz"
     * </pre>
     *
     * @param str
     * @param target
     * @param replacement the String to replace with, which may be null
     * @return A new string with the last occurrence of the target string replaced with the replacement string.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceLast(final String str, final String target, final String replacement) {
        return replaceLast(str, N.len(str), target, replacement);
    }

    /**
     * Replaces the last occurrence of a target string in the input string with a replacement string, starting from a specified index backward.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param startIndexFromBack The index to start the search from, searching backward.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with the last occurrence of the target string replaced with the replacement string, starting from the specified index backward.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceLast(final String str, final int startIndexFromBack, final String target, final String replacement) {
        if (isEmpty(str) || isEmpty(target) || startIndexFromBack < 0) {
            return str;
        }

        final int lastIndex = lastIndexOf(str, target, startIndexFromBack);

        if (lastIndex < 0) {
            return str;
        }

        return Strings.replaceRange(str, lastIndex, lastIndex + N.len(target), replacement);
    }

    /**
     * Replaces occurrences of a target string in the input string with a replacement string, starting from a specified index and up to a maximum number of replacements.
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * replace(null, *, *, *)         = null
     * replace("", *, *, *)           = ""
     * replace("any", {@code null}, *, *)     = "any"
     * replace("any", "", *, *)       = "any"
     * replace("any", *, *, 0)        = "any"
     * replace("abaa", 0, "a", {@code null}, -1) = "b"
     * replace("abaa", 0, "a", "", -1)   = "b"
     * replace("abaa", 0, "a", "z", 0)   = "abaa"
     * replace("abaa", 0, "a", "z", 1)   = "zbaa"
     * replace("abaa", 0, "a", "z", 2)   = "zbza"
     * replace("abaa", 0, "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param str The input string where the replacement should occur. It can be {@code null}.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null}.
     * @param replacement The string to replace the target string. It cannot be {@code null}.
     * @param max The maximum number of replacements. If it's -1, all occurrences will be replaced.
     * @return A new string with occurrences of the target string replaced with the replacement string, starting from the specified index and up to the maximum number of replacements.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replace(final String str, final int fromIndex, final String target, final String replacement, final int max) {
        return replace(str, fromIndex, target, replacement, max, false);
    }

    /**
     * Replaces all occurrences of a target string in the input string with a replacement string, ignoring case considerations.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with all occurrences of the target string replaced with the replacement string, ignoring case considerations.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceAllIgnoreCase(final String str, final String target, final String replacement) {
        return replaceAllIgnoreCase(str, 0, target, replacement);
    }

    /**
     * Replaces all occurrences of a target string in the input string with a replacement string, ignoring case considerations, starting from a specified index.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with all occurrences of the target string replaced with the replacement string, ignoring case considerations, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceAllIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, -1);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string, ignoring case considerations.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with the first occurrence of the target string replaced with the replacement string, ignoring case considerations.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceFirstIgnoreCase(final String str, final String target, final String replacement) {
        return replaceFirstIgnoreCase(str, 0, target, replacement);
    }

    /**
     * Replaces the first occurrence of a target string in the input string with a replacement string, ignoring case considerations, starting from a specified index.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @return A new string with the first occurrence of the target string replaced with the replacement string, ignoring case considerations, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceFirstIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, 1);
    }

    //    /**
    //     * Replaces the first occurrence of a target string in the input string with a replacement string, ignoring case considerations.
    //     *
    //     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
    //     * @param target The string to be replaced. It can be {@code null} or empty.
    //     * @param replacement The string to replace the target string. It can be {@code null}.
    //     * @return A new string with the first occurrence of the target string replaced with the replacement string, ignoring case considerations.
    //     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
    //     * @deprecated Use {@link #replaceFirstIgnoreCase(String, String, String)} instead
    //     */
    //    @Deprecated
    //    public static String replaceOnceIgnoreCase(final String str, final String target, final String replacement) {
    //        return replaceFirstIgnoreCase(str, target, replacement);
    //    }
    //
    //    /**
    //     * Replaces the first occurrence of a target string in the input string with a replacement string, ignoring case considerations, starting from a specified index.
    //     *
    //     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
    //     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
    //     * @param target The string to be replaced. It can be {@code null} or empty.
    //     * @param replacement The string to replace the target string. It can be {@code null}.
    //     * @return A new string with the first occurrence of the target string replaced with the replacement string, ignoring case considerations, starting from the specified index.
    //     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
    //     * @deprecated Use {@link #replaceFirstIgnoreCase(String, int, String, String)} instead
    //     */
    //    @Deprecated
    //    public static String replaceOnceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
    //        return replaceFirstIgnoreCase(str, fromIndex, target, replacement);
    //    }

    /**
     * Replaces occurrences of a target string in the input string with a replacement string, ignoring case considerations, starting from a specified index and up to a maximum number of replacements.
     *
     * @param str The input string where the replacement should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the search for the target string. It should be a non-negative integer.
     * @param target The string to be replaced. It can be {@code null} or empty.
     * @param replacement The string to replace the target string. It can be {@code null}.
     * @param max The maximum number of replacements. If it's -1, all occurrences will be replaced.
     * @return A new string with occurrences of the target string replaced with the replacement string, ignoring case considerations, starting from the specified index and up to the maximum number of replacements.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the target string is not found, the input string is returned unchanged.
     */
    public static String replaceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement, final int max) {
        return replace(str, fromIndex, target, replacement, max, true);
    }

    private static String replace(final String str, final int fromIndex, final String target, String replacement, int max, final boolean ignoreCase) {
        // TODO
        //    if (replacement == null) {
        //        throw new IllegalArgumentException("Replacement can't be null");
        //    }

        if (isEmpty(str) || isEmpty(target) || max == 0) {
            return str;
        }

        if (replacement == null) {
            replacement = "";
        }

        final String searchText = ignoreCase ? str.toLowerCase() : str;
        final String searchTarget = ignoreCase ? target.toLowerCase() : target;

        int end = searchText.indexOf(searchTarget, fromIndex);

        if (end == N.INDEX_NOT_FOUND) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length() + (N.len(replacement) - N.len(target)) * (N.min(16, max)));
        final int substrLength = target.length();
        sb.append(str, 0, fromIndex);
        int start = fromIndex;

        try {
            while (end != N.INDEX_NOT_FOUND) {
                sb.append(str, start, end).append(replacement);
                start = end + substrLength;

                if (--max == 0) {
                    break;
                }

                end = searchText.indexOf(searchTarget, start);
            }

            sb.append(str, start, str.length());

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    //    /**
    //     * Replaces each substring of the source String that matches the given
    //     * regular expression with the given replacement using the
    //     * {@link Pattern#DOTALL} option. DOTALL is also know as single-line mode in
    //     * Perl. This call is also equivalent to:
    //     * <ul>
    //     * <li>{@code source.replaceAll(&quot;(?s)&quot; + regex, replacement)}</li>
    //     * <li>
    //     * {@code Pattern.compile(regex, Pattern.DOTALL).filter(source).replaceAll(replacement)}
    //     * </li>
    //     * </ul>
    //     *
    //     * @param source
    //     *            the source string
    //     * @param regex
    //     *            the regular expression to which this string is to be matched
    //     * @param replacement
    //     *            the string to be substituted for each match
    //     * @return The resulting {@code String}
    //     * @see String#replaceAll(String, String)
    //     * @see Pattern#DOTALL
    //     * @see #replaceAllByPattern(String, String, String)
    //     * @deprecated replaced by {@link #replaceAllByPattern(String, String, String)}
    //     */
    //    @Deprecated
    //    public static String replacePattern(final String source, final String regex, final String replacement) {
    //        return Pattern.compile(regex, Pattern.DOTALL).matcher(source).replaceAll(replacement);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceAll(replacement)}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacement
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceAll(String, String, String)}
    //     * @see Matcher#replaceAll(String)
    //     * @see RegExUtil#replaceAll(String, String, String)
    //     * @see RegExUtil#replaceAll(String, Pattern, String)
    //     */
    //    @Beta
    //    public static String replaceAllByPattern(final String source, final String regex, final String replacement) {
    //        return RegExUtil.replaceAll(source, regex, replacement);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceAll(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())))}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceAll(String, String, Function)}
    //     * @see Matcher#replaceAll(Function)
    //     * @see RegExUtil#replaceAll(String, String, Function)
    //     * @see RegExUtil#replaceAll(String, Pattern, Function)
    //     */
    //    @Beta
    //    public static String replaceAllByPattern(final String source, final String regex, final Function<String, String> replacer) {
    //        return RegExUtil.replaceAll(source, regex, replacer);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceAll(matcher -> replacer.apply(matcher.start(), matcher.end()))}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceAll(String, String, IntBiFunction)}
    //     * @see Matcher#replaceAll(Function)
    //     * @see RegExUtil#replaceAll(String, String, IntBiFunction)
    //     * @see RegExUtil#replaceAll(String, Pattern, IntBiFunction)
    //     */
    //    @Beta
    //    public static String replaceAllByPattern(final String source, final String regex, final IntBiFunction<String> replacer) {
    //        return RegExUtil.replaceAll(source, regex, replacer);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceFirst(replacement)}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacement
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceFirst(String, String, String)}
    //     * @see Matcher#replaceFirst(String)
    //     * @see RegExUtil#replaceFirst(String, String, String)
    //     * @see RegExUtil#replaceFirst(String, Pattern, String)
    //     */
    //    @Beta
    //    public static String replaceFirstByPattern(final String source, final String regex, final String replacement) {
    //        return RegExUtil.replaceFirst(source, regex, replacement);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceFirst(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())))}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceFirst(String, String, Function)}
    //     * @see Matcher#replaceFirst(Function)
    //     * @see RegExUtil#replaceFirst(String, String, Function)
    //     * @see RegExUtil#replaceFirst(String, Pattern, Function)
    //     */
    //    @Beta
    //    public static String replaceFirstByPattern(final String source, final String regex, final Function<String, String> replacer) {
    //        return RegExUtil.replaceFirst(source, regex, replacer);
    //    }
    //
    //    /**
    //     * @implNote equivalent to {@code Pattern.compile(regex).matcher(source).replaceFirst(matcher -> replacer.apply(matcher.start(), matcher.end()))}
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @see Matcher#replaceFirst(Function)
    //     * @see RegExUtil#replaceFirst(String, String, IntBiFunction)
    //     * @see RegExUtil#replaceFirst(String, Pattern, IntBiFunction)
    //     * @deprecated replaced by {@link RegExUtil#replaceFirst(String, String, IntBiFunction)}
    //     */
    //    @Beta
    //    public static String replaceFirstByPattern(final String source, final String regex, final IntBiFunction<String> replacer) {
    //        return RegExUtil.replaceFirst(source, regex, replacer);
    //    }
    //
    //    /**
    //     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacement}.
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacement
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceLast(String, String, String)}
    //     * @see RegExUtil#replaceLast(String, String, String)
    //     * @see RegExUtil#replaceLast(String, Pattern, String)
    //     */
    //    @Beta
    //    public static String replaceLastByPattern(final String source, final String regex, final String replacement) {
    //        return RegExUtil.replaceLast(source, regex, replacement);
    //    }
    //
    //    /**
    //     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceLast(String, String, Function)}
    //     * @see RegExUtil#replaceLast(String, String, Function)
    //     * @see RegExUtil#replaceLast(String, Pattern, Function)
    //     */
    //    @Beta
    //    public static String replaceLastByPattern(final String source, final String regex, final Function<String, String> replacer) {
    //        return RegExUtil.replaceLast(source, regex, replacer);
    //    }
    //
    //    /**
    //     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
    //     *
    //     * @param source
    //     * @param regex
    //     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
    //     * @return
    //     * @deprecated replaced by {@link RegExUtil#replaceLast(String, String, IntBiFunction)}
    //     * @see RegExUtil#replaceLast(String, String, IntBiFunction)
    //     * @see RegExUtil#replaceLast(String, Pattern, IntBiFunction)
    //     */
    //    @Beta
    //    public static String replaceLastByPattern(final String source, final String regex, final IntBiFunction<String> replacer) {
    //        return RegExUtil.replaceLast(source, regex, replacer);
    //    }

    /**
     * Replace the substring specified by the start and end indices with the specified {@code replacement}
     *
     * @param str The input string where the replacement should occur. It cannot be {@code null}.
     * @param fromIndex The start index of the substring to be replaced. It should be a non-negative integer and less than the length of the input string.
     * @param toIndex The end index of the substring to be replaced. It should be a non-negative integer, greater than the start index and less than or equal to the length of the input string.
     * @param replacement The string to replace the substring. It cannot be {@code null}.
     * @return A new string with the specified substring replaced with the replacement string.
     * @throws IndexOutOfBoundsException If the start or end index is out of the string bounds.
     * @deprecated Use {@link #replaceRange(String, int, int, String)} instead of this method.
     * @see #replaceRange(String, int, int, String)
     */
    @Deprecated
    public static String replace(final String str, final int fromIndex, final int toIndex, final String replacement) throws IndexOutOfBoundsException {
        return replaceRange(str, fromIndex, toIndex, replacement);
    }

    /**
     * Replaces the substring between two specified delimiters in the given string with a replacement string.
     * The delimiters themselves are not included in the replaced substring.
     *
     * @param str The string to be processed.
     * @param delimiterOfExclusiveBeginIndex The delimiter after which the replacement should start.
     * @param delimiterOfExclusiveEndIndex The delimiter before which the replacement should end.
     * @param replacement The string to replace the substring between the delimiters. If it's {@code null}, the substring between the delimiters will be removed.
     * @return The processed string with the substring between the delimiters replaced with the replacement string.
     *         If the input string is {@code null} or either of the delimiters is {@code null}, the original string is returned.
     * @see #substringBetween(String, String, String)
     */
    public static String replaceBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex,
            final String replacement) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveEndIndex == null) {
            return str;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0) {
            return str;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return str;
        }

        return replace(str, startIndex, endIndex, replacement);
    }

    /**
     * Replaces the substring after a specified delimiter in the given string with a replacement string.
     * The delimiter itself is not included in the replaced substring.
     *
     * @param str The string to be processed.
     * @param delimiterOfExclusiveBeginIndex The delimiter after which the replacement should start.
     * @param replacement The string to replace the substring after the delimiter. If it's {@code null}, the substring after the delimiter will be removed.
     * @return The processed string with the substring after the delimiter replaced with the replacement string.
     *         If the input string is {@code null} or the delimiter is {@code null}, the original string is returned.
     */
    public static String replaceAfter(final String str, final String delimiterOfExclusiveBeginIndex, final String replacement) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return str;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0) {
            return str;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        return replace(str, startIndex, str.length(), replacement);
    }

    /**
     * Replaces the substring before a specified delimiter in the given string with a replacement string.
     * The delimiter itself is not included in the replaced substring.
     *
     * @param str The string to be processed.
     * @param delimiterOfExclusiveEndIndex The delimiter before which the replacement should end.
     * @param replacement The string to replace the substring before the delimiter. If it's {@code null}, the substring before the delimiter will be removed.
     * @return The processed string with the substring before the delimiter replaced with the replacement string.
     *         If the input string is {@code null} or the delimiter is {@code null}, the original string is returned.
     */
    public static String replaceBefore(final String str, final String delimiterOfExclusiveEndIndex, final String replacement) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return str;
        }

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex);

        if (endIndex < 0) {
            return str;
        }

        return replace(str, 0, endIndex, replacement);
    }

    // Remove
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Removes a substring only if it is at the beginning of a source string,
     * otherwise returns the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string. A {@code null} search string
     * will return the source string.
     * </p>
     *
     * <pre>
     * Strings.removeStart(null, *)      = null
     * Strings.removeStart("", *)        = ""
     * Strings.removeStart(*, null)      = *
     * Strings.removeStart("www.domain.com", "www.")   = "domain.com"
     * Strings.removeStart("domain.com", "www.")       = "domain.com"
     * Strings.removeStart("www.domain.com", "domain") = "www.domain.com"
     * Strings.removeStart("abc", "")    = "abc"
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeStr
     *            the String to search for and remove, which may be null
     * @return
     *         {@code null} String input
     */
    public static String removeStart(final String str, final String removeStr) {
        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        if (str.startsWith(removeStr)) {
            return str.substring(removeStr.length());
        }

        return str;
    }

    /**
     * <p>
     * Case-insensitive removal of a substring if it is at the beginning of a
     * source string, otherwise returns the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string. A {@code null} search string
     * will return the source string.
     * </p>
     *
     * <pre>
     * Strings.removeStartIgnoreCase(null, *)      = null
     * Strings.removeStartIgnoreCase("", *)        = ""
     * Strings.removeStartIgnoreCase(*, null)      = *
     * Strings.removeStartIgnoreCase("www.domain.com", "www.")   = "domain.com"
     * Strings.removeStartIgnoreCase("www.domain.com", "WWW.")   = "domain.com"
     * Strings.removeStartIgnoreCase("domain.com", "www.")       = "domain.com"
     * Strings.removeStartIgnoreCase("www.domain.com", "domain") = "www.domain.com"
     * Strings.removeStartIgnoreCase("abc", "")    = "abc"
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     */
    public static String removeStartIgnoreCase(final String str, final String removeStr) {
        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        if (startsWithIgnoreCase(str, removeStr)) {
            return str.substring(removeStr.length());
        }

        return str;
    }

    /**
     * <p>
     * Removes a substring only if it is at the end of a source string,
     * otherwise returns the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string. A {@code null} search string
     * will return the source string.
     * </p>
     *
     * <pre>
     * Strings.removeEnd(null, *)      = null
     * Strings.removeEnd("", *)        = ""
     * Strings.removeEnd(*, null)      = *
     * Strings.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
     * Strings.removeEnd("www.domain.com", ".com")   = "www.domain"
     * Strings.removeEnd("www.domain.com", "domain") = "www.domain.com"
     * Strings.removeEnd("abc", "")    = "abc"
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeStr
     *            the String to search for and remove, which may be null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     */
    public static String removeEnd(final String str, final String removeStr) {
        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        if (str.endsWith(removeStr)) {
            return str.substring(0, str.length() - removeStr.length());
        }

        return str;
    }

    /**
     * <p>
     * Case-insensitive removal of a substring if it is at the end of a source
     * string, otherwise returns the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string. A {@code null} search string
     * will return the source string.
     * </p>
     *
     * <pre>
     * Strings.removeEndIgnoreCase(null, *)      = null
     * Strings.removeEndIgnoreCase("", *)        = ""
     * Strings.removeEndIgnoreCase(*, null)      = *
     * Strings.removeEndIgnoreCase("www.domain.com", ".com.")  = "www.domain.com"
     * Strings.removeEndIgnoreCase("www.domain.com", ".com")   = "www.domain"
     * Strings.removeEndIgnoreCase("www.domain.com", "domain") = "www.domain.com"
     * Strings.removeEndIgnoreCase("abc", "")    = "abc"
     * Strings.removeEndIgnoreCase("www.domain.com", ".COM") = "www.domain")
     * Strings.removeEndIgnoreCase("www.domain.COM", ".com") = "www.domain")
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     */
    public static String removeEndIgnoreCase(final String str, final String removeStr) {
        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        if (endsWithIgnoreCase(str, removeStr)) {
            return str.substring(0, str.length() - removeStr.length());
        }

        return str;
    }

    /**
     * <p>
     * Removes all occurrences of a character from within the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string.
     * </p>
     *
     * <pre>
     * Strings.remove(null, *)       = null
     * Strings.remove("", *)         = ""
     * Strings.remove("queued", 'u') = "qeed"
     * Strings.remove("queued", 'z') = "queued"
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeChar
     *            the char to search for and remove, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String removeAll(final String str, final char removeChar) {
        return removeAll(str, 0, removeChar);
    }

    /**
     * Removes all occurrences of a specified character from the input string, starting from a specified index.
     *
     * @param str The input string where the removal should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the removal. It should be a non-negative integer.
     * @param removeChar The character to be removed.
     * @return A new string with all occurrences of the specified character removed, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the character is not found, the input string is returned unchanged.
     */
    public static String removeAll(final String str, final int fromIndex, final char removeChar) {
        // N.checkIndex(fromIndex, N.len(str));

        if (str == null || str.isEmpty()) {
            return str;
        }

        final int index = str.indexOf(removeChar, fromIndex);
        if (index == N.INDEX_NOT_FOUND) {
            return str;
        } else {
            final char[] cbuf = new char[str.length()];

            if (index > 0) {
                str.getChars(0, index, cbuf, 0);
            }

            int count = index;
            char ch = 0;

            for (int i = index + 1, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (ch != removeChar) {
                    cbuf[count++] = ch;
                }
            }

            return count == cbuf.length ? str : new String(cbuf, 0, count);
        }
    }

    /**
     * <p>
     * Removes all occurrences of a substring from within the source string.
     * </p>
     *
     * <p>
     * A {@code null} source string will return {@code null}. An empty ("")
     * source string will return the empty string. A {@code null} remove string
     * will return the source string. An empty ("") remove string will return
     * the source string.
     * </p>
     *
     * <pre>
     * Strings.removeAll(null, *)        = null
     * Strings.removeAll("", *)          = ""
     * Strings.removeAll(*, null)        = *
     * Strings.removeAll(*, "")          = *
     * Strings.removeAll("queued", "ue") = "qd"
     * Strings.removeAll("queued", "zz") = "queued"
     * </pre>
     *
     * @param str
     *            the source String to search, which may be null
     * @param removeStr
     *            the String to search for and remove, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String removeAll(final String str, final String removeStr) {
        return removeAll(str, 0, removeStr);
    }

    /**
     * Removes all occurrences of a specified string from the input string, starting from a specified index.
     *
     * @param str The input string where the removal should occur. It can be {@code null} or empty.
     * @param fromIndex The index from which to start the removal. It should be a non-negative integer.
     * @param removeStr The string to be removed. It can be {@code null} or empty.
     * @return A new string with all occurrences of the specified string removed, starting from the specified index.
     *         If the input string is {@code null}, the method returns {@code null}. If the input string is empty, or the string to be removed is not found, the input string is returned unchanged.
     */
    public static String removeAll(final String str, final int fromIndex, final String removeStr) {
        //  N.checkIndex(fromIndex, N.len(str));

        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        return replace(str, fromIndex, removeStr, EMPTY, -1);
    }

    //    /**
    //     * Removes each substring of the source String that matches the given regular expression.
    //     *
    //     * @param source the source string
    //     * @param regex the regular expression to which this string is to be matched
    //     * @return The resulting {@code String}
    //     * @deprecated replaced by {@link RegExUtil#removeAll(String, String)}
    //     * @see String#replaceAll(String, String)
    //     * @see #replaceAllByPattern(String, String, String)
    //     * @see RegExUtil#removeAll(String, String)
    //     */
    //    @Beta
    //    public static String removeAllByPattern(final String source, final String regex) {
    //        return RegExUtil.removeAll(source, regex);
    //    }
    //
    //    /**
    //     * Removes the first substring of the source string that matches the given regular expression.
    //     *
    //     * @param source the source string
    //     * @param regex the regular expression to which this string is to be matched
    //     * @return The resulting {@code String}
    //     * @see String#replaceFirst(String, String)
    //     * @see #replaceFirstByPattern(String, String, String)
    //     * @see RegExUtil#removeFirst(String, String)
    //     */
    //    @Beta
    //    public static String removeFirstByPattern(final String source, final String regex) {
    //        return RegExUtil.removeFirst(source, regex);
    //    }
    //
    //    /**
    //     * Removes the last substring of the source string that matches the given regular expression.
    //     *
    //     * @param source the source string
    //     * @param regex the regular expression to which this string is to be matched
    //     * @return The resulting {@code String}
    //     * @see #replaceLastByPattern(String, String, String)
    //     * @see RegExUtil#removeLast(String, String)
    //     */
    //    @Beta
    //    public static String removeLastByPattern(final String source, final String regex) {
    //        return RegExUtil.removeLast(source, regex);
    //    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter character.
     *
     * @param str The string to be split.
     * @param delimiter The character used as the delimiter for splitting the string.
     * @return An array of substrings derived from the input string, split based on the delimiter character.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     */
    public static String[] split(final String str, final char delimiter) {
        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        //    final Splitter splitter = splitterPool.get(delimiter);
        //
        //    return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings() : splitter).splitToArray(str);

        return splitWorker(str, delimiter, Integer.MAX_VALUE, false, false);
    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter character.
     * If the trim parameter is {@code true}, it trims leading and trailing whitespace from each substring.
     *
     * @param str The string to be split.
     * @param delimiter The character used as the delimiter for splitting the string.
     * @param trim A boolean that determines whether to trim leading and trailing whitespace from each substring.
     * @return An array of substrings derived from the input string, split based on the delimiter character and optionally trimmed.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     */
    public static String[] split(final String str, final char delimiter, final boolean trim) {
        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        //    if (trim) {
        //        final Splitter splitter = trimSplitterPool.get(delimiter);
        //        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings().trim(trim) : splitter).splitToArray(str);
        //    } else {
        //        return split(str, delimiter);
        //    }

        return splitWorker(str, delimiter, Integer.MAX_VALUE, trim, false);
    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter string.
     *
     * @param str The string to be split.
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @return An array of substrings derived from the input string, split based on the delimiter string.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     */
    public static String[] split(final String str, final String delimiter) {
        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        //    final Splitter splitter = splitterPool.get(delimiter);
        //
        //    return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings() : splitter).splitToArray(str);

        return splitWorker(str, delimiter, Integer.MAX_VALUE, false, false);
    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter string.
     * If the trim parameter is {@code true}, it trims leading and trailing whitespace from each substring.
     *
     * @param str The string to be split.
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param trim A boolean that determines whether to trim leading and trailing whitespace from each substring.
     * @return An array of substrings derived from the input string, split based on the delimiter string and optionally trimmed.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     */
    public static String[] split(final String str, final String delimiter, final boolean trim) {
        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        //    if (trim) {
        //        final Splitter splitter = trimSplitterPool.get(delimiter);
        //        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings().trim(trim) : splitter).splitToArray(str);
        //    } else {
        //        return split(str, delimiter);
        //    }

        return splitWorker(str, delimiter, Integer.MAX_VALUE, trim, false);
    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter string.
     * The split operation will stop after reaching the specified maximum limit of substrings.
     *
     * @param str The string to be split.
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param max The maximum number of substrings to be included in the resulting array.
     *            If the string contains more delimiters, the last substring will contain all remaining text.
     * @return An array of substrings derived from the input string, split based on the delimiter string.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     * @throws IllegalArgumentException if the max parameter is not a positive integer.
     */
    public static String[] split(final String str, final String delimiter, final int max) throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        if (max == 1) {
            return new String[] { str };
        }

        //    return Splitter.with(delimiter).omitEmptyStrings().limit(max).splitToArray(str);

        return splitWorker(str, delimiter, max, false, false);
    }

    /**
     * Splits the given string into an array of substrings, using the specified delimiter string.
     * The split operation will stop after reaching the specified maximum limit of substrings.
     * If the trim parameter is {@code true}, it trims leading and trailing whitespace from each substring.
     *
     * @param str The string to be split.
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param max The maximum number of substrings to be included in the resulting array.
     *            If the string contains more delimiters, the last substring will contain all remaining text.
     * @param trim A boolean that determines whether to trim leading and trailing whitespace from each substring.
     * @return An array of substrings derived from the input string, split based on the delimiter string, limited by the max parameter and optionally trimmed.
     *         If the input string is {@code null} or empty, the method will return an empty String array.
     * @throws IllegalArgumentException if the max parameter is not a positive integer.
     */
    public static String[] split(final String str, final String delimiter, final int max, final boolean trim) throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (isEmpty(str)) {
            return N.EMPTY_STRING_ARRAY;
        }

        if (max == 1) {
            return new String[] { trim ? str.trim() : str };
        }

        //    return Splitter.with(delimiter).omitEmptyStrings().trim(trim).limit(max).splitToArray(str);

        return splitWorker(str, delimiter, max, trim, false);
    }

    /**
     * Splits the provided text into an array, separator specified,
     * preserving all tokens, including empty tokens created by adjacent
     * separators. This is an alternative to using StringTokenizer.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as separators for empty tokens.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     * <pre>
     * Strings.splitPreserveAllTokens(null, *)         = []
     * Strings.splitPreserveAllTokens("", *)           = [""]
     * Strings.splitPreserveAllTokens("a.b.c", '.')    = ["a", "b", "c"]
     * Strings.splitPreserveAllTokens("a..b.c", '.')   = ["a", "", "b", "c"]
     * Strings.splitPreserveAllTokens("a:b:c", '.')    = ["a:b:c"]
     * Strings.splitPreserveAllTokens("a\tb\nc", null) = ["a", "b", "c"]
     * Strings.splitPreserveAllTokens("a b c", ' ')    = ["a", "b", "c"]
     * Strings.splitPreserveAllTokens("a b c ", ' ')   = ["a", "b", "c", ""]
     * Strings.splitPreserveAllTokens("a b c  ", ' ')  = ["a", "b", "c", "", ""]
     * Strings.splitPreserveAllTokens(" a b c", ' ')   = ["", "a", "b", "c"]
     * Strings.splitPreserveAllTokens("  a b c", ' ')  = ["", "", "a", "b", "c"]
     * Strings.splitPreserveAllTokens(" a b c ", ' ')  = ["", "a", "b", "c", ""]
     * </pre>
     *
     * @param str  the String to parse, may be {@code null}
     * @param delimiter the character used as the delimite
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     */
    public static String[] splitPreserveAllTokens(final String str, final char delimiter) {
        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        //    final Splitter splitter = preserveSplitterPool.get(delimiter);
        //
        //    return (splitter == null ? Splitter.with(delimiter) : splitter).splitToArray(str);

        return splitWorker(str, delimiter, Integer.MAX_VALUE, false, true);
    }

    /**
     * Splits the provided text into an array, separator specified,
     * preserving all tokens, including empty tokens created by adjacent
     * separators. This is an alternative to using StringTokenizer.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as separators for empty tokens.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     * @param str the String to parse, may be {@code null}
     * @param delimiter the character used as the delimiter
     * @param trim If {@code true}, leading and trailing whitespace is removed from each substring.
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     */
    public static String[] splitPreserveAllTokens(final String str, final char delimiter, final boolean trim) {
        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        //    if (trim) {
        //        final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
        //        return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        //    } else {
        //        return splitPreserveAllTokens(str, delimiter);
        //    }

        return splitWorker(str, delimiter, Integer.MAX_VALUE, trim, true);
    }

    /**
     * Splits the provided text into an array, separators specified,
     * preserving all tokens, including empty tokens created by adjacent
     * separators. This is an alternative to using StringTokenizer.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as separators for empty tokens.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}. A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     * <pre>
     * Strings.splitPreserveAllTokens(null, *)           = []
     * Strings.splitPreserveAllTokens("", *)             = [""]
     * Strings.splitPreserveAllTokens("abc def", null)   = ["abc", "def"]
     * Strings.splitPreserveAllTokens("abc def", " ")    = ["abc", "def"]
     * Strings.splitPreserveAllTokens("abc  def", " ")   = ["abc", "", "def"]
     * Strings.splitPreserveAllTokens("ab:cd:ef", ":")   = ["ab", "cd", "ef"]
     * Strings.splitPreserveAllTokens("ab:cd:ef:", ":")  = ["ab", "cd", "ef", ""]
     * Strings.splitPreserveAllTokens("ab:cd:ef::", ":") = ["ab", "cd", "ef", "", ""]
     * Strings.splitPreserveAllTokens("ab::cd:ef", ":")  = ["ab", "", "cd", "ef"]
     * Strings.splitPreserveAllTokens(":cd:ef", ":")     = ["", "cd", "ef"]
     * Strings.splitPreserveAllTokens("::cd:ef", ":")    = ["", "", "cd", "ef"]
     * Strings.splitPreserveAllTokens(":cd:ef:", ":")    = ["", "cd", "ef", ""]
     * </pre>
     *
     * @param str  the String to parse, may be {@code null}
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter) {
        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        //    final Splitter splitter = preserveSplitterPool.get(delimiter);
        //
        //    return (splitter == null ? Splitter.with(delimiter) : splitter).splitToArray(str);

        return splitWorker(str, delimiter, Integer.MAX_VALUE, false, true);
    }

    /**
     * Splits the provided text into an array, separators specified,
     * preserving all tokens, including empty tokens created by adjacent
     * separators. This is an alternative to using StringTokenizer.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as separators for empty tokens.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}. A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     *
     * @param str  the String to parse, may be {@code null}
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param trim If {@code true}, leading and trailing whitespace is removed from each substring.
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final boolean trim) {
        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        //    if (trim) {
        //        final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
        //        return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        //    } else {
        //        return splitPreserveAllTokens(str, delimiter);
        //    }

        return splitWorker(str, delimiter, Integer.MAX_VALUE, trim, true);
    }

    /**
     * Splits the provided text into an array with a maximum length,
     * separators specified.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>If more than {@code max} delimited substrings are found, the last
     * returned string includes all characters after the first {@code max - 1}
     * returned strings (including separator characters).</p>
     *
     * <p>A {@code null} input String returns {@code null}. A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     *
     * @param str  the String to parse, may be {@code null}
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param max The maximum number of substrings to be included in the resulting array.
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     * @throws IllegalArgumentException if the max parameter is not a positive integer.
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final int max) throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        if (max == 1) {
            return new String[] { str };
        }

        // return Splitter.with(delimiter).limit(max).splitToArray(str);

        return splitWorker(str, delimiter, max, false, true);
    }

    /**
     * Splits the provided text into an array with a maximum length,
     * separators specified.
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>If more than {@code max} delimited substrings are found, the last
     * returned string includes all characters after the first {@code max - 1}
     * returned strings (including separator characters).</p>
     *
     * <p>A {@code null} input String returns {@code null}. A {@code null} separatorChars splits on whitespace.</p>
     *
     * <p>An empty String array {@code []} will be returned if the input string {@code null}.</p>
     * <p>A String array with single empty String: {@code [""]} will be returned if the input string is empty.</p>
     *
     *
     * @param str  the String to parse, may be {@code null}
     * @param delimiter The string used as the delimiter for splitting the string. {@code null} for splitting on whitespace.
     * @param max The maximum number of substrings to be included in the resulting array.
     * @param trim If {@code true}, leading and trailing whitespace is removed from each substring.
     * @return an array of parsed Strings. An empty String array {@code []} will be returned if the input string {@code null},
     *         or a String array with single empty String: {@code [""]} will be returned if the input string is empty.
     * @throws IllegalArgumentException if the max parameter is not a positive integer.
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final int max, final boolean trim) throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { EMPTY };
        }

        if (max == 1) {
            return new String[] { trim ? str.trim() : str };
        }

        // return Splitter.with(delimiter).trim(trim).limit(max).splitToArray(str);

        return splitWorker(str, delimiter, max, trim, true);
    }

    // Copied from Apache Commons Lang 3.17.0 under Apache License 2.0
    private static String[] splitWorker(final String str, final char delimiter, final int max, final boolean trim, final boolean preserveAllTokens)
            throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return preserveAllTokens ? new String[] { EMPTY } : N.EMPTY_STRING_ARRAY;
        }

        final int len = str.length();
        final List<String> substrs = new ArrayList<>();

        int sizePlus1 = 1;
        int i = 0;
        int start = 0;
        boolean match = false;
        boolean lastMatch = false;

        while (i < len) {
            if (str.charAt(i) == delimiter) {
                if (match || preserveAllTokens) {
                    lastMatch = true;
                    if (sizePlus1++ == max) {
                        i = len;
                        lastMatch = false;
                    }
                    substrs.add(str.substring(start, i));
                    match = false;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }

        if (match || preserveAllTokens && lastMatch) {
            substrs.add(str.substring(start, i));
        }

        final String[] ret = substrs.toArray(N.EMPTY_STRING_ARRAY);

        if (trim) {
            trim(ret);
        }

        return ret;
    }

    // Copied from Apache Commons Lang 3.17.0 under Apache License 2.0
    private static String[] splitWorker(final String str, final String delimiter, final int max, final boolean trim, final boolean preserveAllTokens)
            throws IllegalArgumentException {
        N.checkArgPositive(max, cs.max);

        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return preserveAllTokens ? new String[] { EMPTY } : N.EMPTY_STRING_ARRAY;
        }

        if (N.len(delimiter) == 1) {
            return splitWorker(str, delimiter.charAt(0), max, trim, preserveAllTokens);
        }

        final int len = str.length();
        final List<String> substrs = new ArrayList<>();

        int sizePlus1 = 1;
        int i = 0;
        int start = 0;
        boolean match = false;
        boolean lastMatch = false;

        if (delimiter == null) {
            // Null separator means use whitespace
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        substrs.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            // standard case
            while (i < len) {
                if (delimiter.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        substrs.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }

        if (match || preserveAllTokens && lastMatch) {
            substrs.add(str.substring(start, i));
        }

        final String[] ret = substrs.toArray(N.EMPTY_STRING_ARRAY);

        if (trim) {
            trim(ret);
        }

        return ret;
    }

    /**
     * Splits the given string into an array of substrings, each of which is a line of text from the original string.
     * The string is split at line terminators, which can be the carriage return character ('\r'), the newline character ('\n'), or the carriage return followed immediately by the newline character.
     *
     * @param str The string to be split. If it's {@code null}, the method will return an empty String array.
     * @return An array of substrings derived from the input string, each of which is a line of text.
     *         If the input string is {@code null}, return an empty String array. If the input string is empty, return an String array with one empty String inside.
     */
    public static String[] splitToLines(final String str) {
        if (str == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return lineSplitter.splitToArray(str);
    }

    /**
     * Splits the given string into an array of substrings, each of which is a line of text from the original string.
     * The string is split at line terminators, which can be the carriage return character ('\r'), the newline character ('\n'), or the carriage return followed immediately by the newline character.
     * If the trim parameter is {@code true}, leading and trailing whitespace is removed from each line of text.
     * If the omitEmptyLines parameter is {@code true}, empty lines (after trimming, if the trim parameter is true) are not included in the resulting array.
     *
     * @param str The string to be split.
     * @param trim A boolean that determines whether to trim leading and trailing whitespace from each line of text.
     * @param omitEmptyLines A boolean that determines whether to omit empty lines from the resulting array.
     * @return An array of substrings derived from the input string, each of which is a line of text, optionally trimmed and with empty lines optionally omitted.
     *         If the input string is {@code null} or {@code str.length() == 0 && omitEmptyLines}, return an empty String array. If the input string is empty, return an String array with one empty String inside.
     */
    public static String[] splitToLines(final String str, final boolean trim, final boolean omitEmptyLines) {
        if (str == null || (str.isEmpty() && omitEmptyLines)) {
            return N.EMPTY_STRING_ARRAY;
        } else if (str.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        if (trim) {
            if (omitEmptyLines) {
                return trimAndOmitEmptyLinesLineSplitter.splitToArray(str);
            } else {
                return trimLineSplitter.splitToArray(str);
            }
        } else if (omitEmptyLines) {
            return omitEmptyLinesLineSplitter.splitToArray(str);
        } else {
            return lineSplitter.splitToArray(str);
        }
    }

    // -----------------------------------------------------------------------

    /**
     * <p>
     * Removes space characters (char &lt;= 32) from both ends of this String,
     * handling {@code null} by returning {@code null}.
     * </p>
     *
     * <p>
     * The String is trimmed using {@link String#trim()}. Trim removes start and
     * end characters &lt;= 32. To strip whitespace use {@link #strip(String)}.
     * </p>
     *
     * <p>
     * To trim your choice of characters, use the {@link #strip(String, String)}
     * methods.
     * </p>
     *
     * <pre>
     * Strings.trim(null)          = null
     * Strings.trim("")            = ""
     * Strings.trim("     ")       = ""
     * Strings.trim("abc")         = "abc"
     * Strings.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, which may be null
     * @return
     */
    public static String trim(final String str) {
        return isEmpty(str) || (str.charAt(0) != ' ' && str.charAt(str.length() - 1) != ' ') ? str : str.trim();
    }

    /**
     * Trims leading and trailing space characters from each string in the provided array.
     * This method uses {@link String#trim()} to remove space characters.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be trimmed. Each string in the array will be updated in-place.
     */
    public static void trim(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = trim(strs[i]);
        }
    }

    /**
     * <p>
     * Removes space characters (char &lt;= 32) from both ends of this String
     * returning {@code null} if the String is empty ("") after the trim or if
     * it is {@code null}.
     *
     * <p>
     * The String is trimmed using {@link String#trim()}. Trim removes start and
     * end characters &lt;= 32. To strip whitespace use
     * {@link #stripToNull(String)}.
     * </p>
     *
     * <pre>
     * Strings.trimToNull(null)          = null
     * Strings.trimToNull("")            = null
     * Strings.trimToNull("     ")       = null
     * Strings.trimToNull("abc")         = "abc"
     * Strings.trimToNull("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, which may be null
     * @return
     *         {@code null} String input
     */
    public static String trimToNull(String str) {
        str = trim(str);

        return isEmpty(str) ? null : str;
    }

    /**
     * Trims leading and trailing whitespace from each string in the provided array and sets the string to {@code null} if it is empty after trimming.
     * This method uses {@link String#trim()} to remove whitespace.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be trimmed. Each string in the array will be updated in-place.
     */
    public static void trimToNull(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = trimToNull(strs[i]);
        }
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String
     * returning an empty String ("") if the String is empty ("") after the trim
     * or if it is {@code null}.
     *
     * <p>
     * The String is trimmed using {@link String#trim()}. Trim removes start and
     * end characters &lt;= 32. To strip whitespace use
     * {@link #stripToEmpty(String)}.
     * </p>
     *
     * <pre>
     * Strings.trimToEmpty(null)          = ""
     * Strings.trimToEmpty("")            = ""
     * Strings.trimToEmpty("     ")       = ""
     * Strings.trimToEmpty("abc")         = "abc"
     * Strings.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, which may be null
     * @return
     */
    public static String trimToEmpty(final String str) {
        return isEmpty(str) ? EMPTY : str.trim();
    }

    /**
     * Trims leading and trailing whitespace from each string in the provided array.
     * If a string becomes {@code null} after trimming, it is set to empty.
     * This method uses {@link String#trim()} to remove whitespace.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be trimmed. Each string in the array will be updated in-place.
     */
    public static void trimToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = trimToEmpty(strs[i]);
        }
    }

    // Stripping
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Strips whitespace from the start and end of a String.
     * </p>
     *
     * <p>
     * This is similar to {@link #trim(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * Strings.strip(null)     = null
     * Strings.strip("")       = ""
     * Strings.strip("   ")    = ""
     * Strings.strip("abc")    = "abc"
     * Strings.strip("  abc")  = "abc"
     * Strings.strip("abc  ")  = "abc"
     * Strings.strip(" abc ")  = "abc"
     * Strings.strip(" ab c ") = "ab c"
     * </pre>
     *
     * @param str
     *            the String to remove whitespace from, which may be null
     * @return
     */
    public static String strip(final String str) {
        return strip(str, null);
    }

    /**
     * Strips whitespace from the start and end of each string in the provided array.
     * This method uses {@link #strip(String)} to remove whitespace.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     */
    public static void strip(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strip(strs[i]);
        }
    }

    /**
     * <p>
     * Strips whitespace from the start and end of a String returning
     * {@code null} if the String is empty ("") after the strip.
     * </p>
     *
     * <p>
     * This is similar to {@link #trimToNull(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * Strings.stripToNull(null)     = null
     * Strings.stripToNull("")       = null
     * Strings.stripToNull("   ")    = null
     * Strings.stripToNull("abc")    = "abc"
     * Strings.stripToNull("  abc")  = "abc"
     * Strings.stripToNull("abc  ")  = "abc"
     * Strings.stripToNull(" abc ")  = "abc"
     * Strings.stripToNull(" ab c ") = "ab c"
     * </pre>
     *
     * @param str
     *            the String to be stripped, which may be null
     * @return
     *         String input
     */
    public static String stripToNull(String str) {
        str = strip(str, null);

        return isEmpty(str) ? null : str;
    }

    /**
     * Strips whitespace from the start and end of each string in the provided array and sets the string to {@code null} if it is empty after stripping.
     * This method uses {@link #strip(String)} to remove whitespace.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     */
    public static void stripToNull(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = stripToNull(strs[i]);
        }
    }

    /**
     * <p>
     * Strips whitespace from the start and end of a String returning an empty
     * String if {@code null} input.
     * </p>
     *
     * <p>
     * This is similar to {@link #trimToEmpty(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * Strings.stripToEmpty(null)     = ""
     * Strings.stripToEmpty("")       = ""
     * Strings.stripToEmpty("   ")    = ""
     * Strings.stripToEmpty("abc")    = "abc"
     * Strings.stripToEmpty("  abc")  = "abc"
     * Strings.stripToEmpty("abc  ")  = "abc"
     * Strings.stripToEmpty(" abc ")  = "abc"
     * Strings.stripToEmpty(" ab c ") = "ab c"
     * </pre>
     *
     * @param str
     *            the String to be stripped, which may be null
     * @return
     */
    public static String stripToEmpty(final String str) {
        return isEmpty(str) ? EMPTY : strip(str, null);
    }

    /**
     * Strips leading and trailing whitespace from each string in the provided array.
     * If a string becomes {@code null} after stripping, it is set to an empty string.
     * This method uses {@link #strip(String)} to remove whitespace.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     */
    public static void stripToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = stripToEmpty(strs[i]);
        }
    }

    /**
     * <p>
     * Strips any of a set of characters from the start and end of a String.
     * This is similar to {@link String#trim()} but allows the characters to be
     * stripped to be controlled.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}. An empty string ("")
     * input returns the empty string.
     * </p>
     *
     * <p>
     * If the stripChars String is {@code null}, whitespace is stripped as
     * defined by {@link Character#isWhitespace(char)}. Alternatively use
     * {@link #strip(String)}.
     * </p>
     *
     * <pre>
     * Strings.strip(null, *)          = null
     * Strings.strip("", *)            = ""
     * Strings.strip("abc", null)      = "abc"
     * Strings.strip("  abc", null)    = "abc"
     * Strings.strip("abc  ", null)    = "abc"
     * Strings.strip(" abc ", null)    = "abc"
     * Strings.strip("  abcyx", "xyz") = "  abc"
     * </pre>
     *
     * @param str
     *            the String to remove characters from, which may be null
     * @param stripChars
     *            the characters to remove, {@code null} treated as whitespace
     * @return the specified String if it's {@code null} or empty.
     */
    public static String strip(final String str, final String stripChars) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return stripEnd(stripStart(str, stripChars), stripChars);
    }

    /**
     * Strips the specified characters from the start and end of each string in the provided array.
     * This method uses {@link #strip(String, String)} to remove the specified characters.
     * If the input array is {@code null} or empty, or the stripChars is {@code null}, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     * @param stripChars The set of characters to be stripped from the strings. If {@code null}, the method behaves as {@link #strip(String)}.
     */
    public static void strip(final String[] strs, final String stripChars) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strip(strs[i], stripChars);
        }
    }

    /**
     * Strips whitespace from the start of a String.
     * 
     * @param str
     * @return
     */
    public static String stripStart(final String str) {
        return stripStart(str, null);
    }

    /**
     * <p>
     * Strips any of a set of characters from the start of a String.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}. An empty string ("")
     * input returns the empty string.
     * </p>
     *
     * <p>
     * If the stripChars String is {@code null}, whitespace is stripped as
     * defined by {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * Strings.stripStart(null, *)          = null
     * Strings.stripStart("", *)            = ""
     * Strings.stripStart("abc", "")        = "abc"
     * Strings.stripStart("abc", null)      = "abc"
     * Strings.stripStart("  abc", null)    = "abc"
     * Strings.stripStart("abc  ", null)    = "abc  "
     * Strings.stripStart(" abc ", null)    = "abc "
     * Strings.stripStart("yxabc  ", "xyz") = "abc  "
     * </pre>
     *
     * @param str
     *            the String to remove characters from, which may be null
     * @param stripChars
     *            the characters to remove, {@code null} treated as whitespace
     * @return the specified String if it's {@code null} or empty.
     */
    public static String stripStart(final String str, final String stripChars) {
        if (isEmpty(str) || (stripChars != null && stripChars.isEmpty())) {
            return str;
        }

        final int strLen = str.length();
        int start = 0;
        if (stripChars == null) {
            while (start != strLen && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else {
            while (start != strLen && stripChars.indexOf(str.charAt(start)) != N.INDEX_NOT_FOUND) {
                start++;
            }
        }

        return start == 0 ? str : str.substring(start);
    }

    /**
     * Strips the specified characters from the start of each string in the provided array.
     * This method uses {@link #stripStart(String, String)} to remove the specified characters.
     * If the input array is {@code null} or empty, or the stripChars is {@code null}, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     * @param stripChars The set of characters to be stripped from the start of the strings. If {@code null}, the method behaves as {@link #stripStart(String, String)}.
     * @see #stripStart(String, String)
     */
    public static void stripStart(final String[] strs, final String stripChars) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = stripStart(strs[i], stripChars);
        }
    }

    /**
     * <p>
     * Strips whitespace from the end of a String.
     * </p>
     * 
     * @param str
     * @return
     */
    public static String stripEnd(final String str) {
        return stripEnd(str, null);
    }

    /**
     * <p>
     * Strips any of a set of characters from the end of a String.
     * </p>
     *
     * <p>
     * A {@code null} input String returns {@code null}. An empty string ("")
     * input returns the empty string.
     * </p>
     *
     * <p>
     * If the stripChars String is {@code null}, whitespace is stripped as
     * defined by {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * Strings.stripEnd(null, *)          = null
     * Strings.stripEnd("", *)            = ""
     * Strings.stripEnd("abc", "")        = "abc"
     * Strings.stripEnd("abc", null)      = "abc"
     * Strings.stripEnd("  abc", null)    = "  abc"
     * Strings.stripEnd("abc  ", null)    = "abc"
     * Strings.stripEnd(" abc ", null)    = " abc"
     * Strings.stripEnd("  abcyx", "xyz") = "  abc"
     * Strings.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * @param str
     *            the String to remove characters from, which may be null
     * @param stripChars
     *            the set of characters to remove, {@code null} treated as whitespace
     * @return the specified String if it's {@code null} or empty.
     */
    public static String stripEnd(final String str, final String stripChars) {
        if (isEmpty(str) || (stripChars != null && stripChars.isEmpty())) {
            return str;
        }

        int end = str.length();

        if (stripChars == null) {
            while (end > 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else {
            while (end > 0 && stripChars.indexOf(str.charAt(end - 1)) != N.INDEX_NOT_FOUND) {
                end--;
            }
        }

        return end == str.length() ? str : str.substring(0, end);
    }

    /**
     * Strips the specified characters from the end of each string in the provided array.
     * This method uses {@link #stripEnd(String, String)} to remove the specified characters.
     * If the input array is {@code null} or empty, or the stripChars is {@code null}, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     * @param stripChars The set of characters to be stripped from the end of the strings. If {@code null}, the method behaves as {@link #stripEnd(String, String)}.
     * @see #stripEnd(String, String)
     */
    public static void stripEnd(final String[] strs, final String stripChars) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = stripEnd(strs[i], stripChars);
        }
    }

    /**
     * <p>
     * Removes diacritics (~= accents) from a string. The case will not be
     * altered.
     * </p>
     * <p>
     * For instance, <i>&agrave;</i> will be replaced by 'a'.
     * </p>
     * <p>
     * Note that ligatures will be left as is.
     * </p>
     *
     * <pre>
     * Strings.stripAccents(null)                = null
     * Strings.stripAccents("")                  = ""
     * Strings.stripAccents("control")           = "control"
     * Strings.stripAccents("&eacute;clair")     = "eclair"
     * </pre>
     *
     * @param str
     * @return
     */
    // See also Lucene's ASCIIFoldingFilter (Lucene 2.9) that replaces accented
    // characters by their unaccented equivalent (and uncommitted bug fix:
    // https://issues.apache.org/jira/browse/LUCENE-1343?focusedCommentId=12858907&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12858907).
    @MayReturnNull
    public static String stripAccents(final String str) {
        if (str == null) {
            return null;
        }
        final StringBuilder decomposed = new StringBuilder(Normalizer.normalize(str, Normalizer.Form.NFD));
        convertRemainingAccentCharacters(decomposed);
        // Note that this doesn't correctly remove ligatures...
        return STRIP_ACCENTS_PATTERN.matcher(decomposed).replaceAll(EMPTY);
    }

    /**
     * Pattern used in {@link #stripAccents(String)}.
     */
    private static final Pattern STRIP_ACCENTS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+"); //$NON-NLS-1$

    private static void convertRemainingAccentCharacters(final StringBuilder decomposed) {
        char ch = 0;

        for (int i = 0; i < decomposed.length(); i++) {
            ch = decomposed.charAt(i);

            if (ch == '\u0141') {
                decomposed.setCharAt(i, 'L');
            } else if (ch == '\u0142') {
                decomposed.setCharAt(i, 'l');
            }
        }
    }

    /**
     * Strips accents from each string in the provided array.
     * This method uses {@link #stripAccents(String)} to remove accents.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be stripped. Each string in the array will be updated in-place.
     */
    public static void stripAccents(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = stripAccents(strs[i]);
        }
    }

    // Chomping
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Removes one newline from end of a String if it's there, otherwise leave
     * it alone. A newline is &quot;{@code \n} &quot;, &quot;{@code \r}&quot;,
     * or &quot;{@code \r\n}&quot;.
     * </p>
     *
     * <p>
     * NOTE: This method changed in 2.0. It now more closely matches Perl chomp.
     * </p>
     *
     * <pre>
     * Strings.chomp(null)          = null
     * Strings.chomp("")            = ""
     * Strings.chomp("abc \r")      = "abc "
     * Strings.chomp("abc\n")       = "abc"
     * Strings.chomp("abc\r\n")     = "abc"
     * Strings.chomp("abc\r\n\r\n") = "abc\r\n"
     * Strings.chomp("abc\n\r")     = "abc\n"
     * Strings.chomp("abc\n\rabc")  = "abc\n\rabc"
     * Strings.chomp("\r")          = ""
     * Strings.chomp("\n")          = ""
     * Strings.chomp("\r\n")        = ""
     * </pre>
     *
     * @param str
     *            the String to chomp a newline from, which may be null
     * @return String without newline, {@code null} if {@code null} String input
     */
    public static String chomp(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.length() == 1) {
            final char ch = str.charAt(0);

            if (ch == CHAR_CR || ch == CHAR_LF) {
                return EMPTY;
            }

            return str;
        }

        int lastIdx = str.length() - 1;
        final char last = str.charAt(lastIdx);

        if (last == CHAR_LF) {
            if (str.charAt(lastIdx - 1) == CHAR_CR) {
                lastIdx--;
            }
        } else if (last != CHAR_CR) {
            lastIdx++;
        }

        return lastIdx == str.length() ? str : str.substring(0, lastIdx);
    }

    /**
     * Removes one newline from end of each string in the provided array if it's there, otherwise leaves it alone.
     * A newline is "\n", "\r", or "\r\n".
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be chomped. Each string in the array will be updated in-place.
     */
    public static void chomp(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = chomp(strs[i]);
        }
    }

    // Chopping
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Remove the last character from a String.
     * </p>
     *
     * <p>
     * If the String ends in {@code \r\n}, then remove both of them.
     * </p>
     *
     * <pre>
     * Strings.chop(null)          = null
     * Strings.chop("")            = ""
     * Strings.chop("abc \r")      = "abc "
     * Strings.chop("abc\n")       = "abc"
     * Strings.chop("abc\r\n")     = "abc"
     * Strings.chop("abc")         = "ab"
     * Strings.chop("abc\nabc")    = "abc\nab"
     * Strings.chop("a")           = ""
     * Strings.chop("\r")          = ""
     * Strings.chop("\n")          = ""
     * Strings.chop("\r\n")        = ""
     * </pre>
     *
     * @param str
     *            the String to chop last character from, which may be null
     * @return String without last character, {@code null} if {@code null} String input
     */
    public static String chop(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final int strLen = str.length();

        if (strLen < 2) {
            return EMPTY;
        }

        final int lastIdx = strLen - 1;

        if (str.charAt(lastIdx) == CHAR_LF && str.charAt(lastIdx - 1) == CHAR_CR) {
            return str.substring(0, lastIdx - 1);
        } else {
            return str.substring(0, lastIdx);
        }
    }

    /**
     * Removes the last character from each string in the provided array.
     * If a string in the array ends in "\r\n", both characters are removed.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be chopped. Each string in the array will be updated in-place.
     */
    public static void chop(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = chop(strs[i]);
        }
    }

    /**
     * <p>Truncates a String. This will turn
     * "Now is the time for all good men" into "Now is the time for".</p>
     *
     * <p>Specifically:</p>
     * <ul>
     *   <li>If {@code str} is less than {@code maxWidth} characters
     *       long, return it.</li>
     *   <li>Else truncate it to {@code substring(str, 0, maxWidth)}.</li>
     *   <li>If {@code maxWidth} is less than {@code 0}, throw an
     *       {@code IllegalArgumentException}.</li>
     *   <li>In no case will it return a String of length greater than
     *       {@code maxWidth}.</li>
     * </ul>
     *
     * <pre>
     * Strings.truncate(null, 0)       = null
     * Strings.truncate(null, 2)       = null
     * Strings.truncate("", 4)         = ""
     * Strings.truncate("abcdefg", 4)  = "abcd"
     * Strings.truncate("abcdefg", 6)  = "abcdef"
     * Strings.truncate("abcdefg", 7)  = "abcdefg"
     * Strings.truncate("abcdefg", 8)  = "abcdefg"
     * Strings.truncate("abcdefg", -1) = throws an IllegalArgumentException
     * </pre>
     *
     * @param str the String to truncate, which may be null
     * @param maxWidth maximum length of result String, must be positive
     * @return truncated String, {@code null} if {@code null} String input
     * @throws IllegalArgumentException If {@code maxWidth} is less than {@code 0}
     */
    public static String truncate(final String str, final int maxWidth) {
        return truncate(str, 0, maxWidth);
    }

    /**
     * <p>Truncates a String. This will turn
     * "Now is the time for all good men" into "is the time for all".</p>
     *
     * <p>Works like {@code truncate(String, int)}, but allows you to specify
     * a "left edge" offset.
     *
     * <p>Specifically:</p>
     * <ul>
     *   <li>If {@code str} is less than {@code maxWidth} characters
     *       long, return it.</li>
     *   <li>Else truncate it to {@code substring(str, offset, maxWidth)}.</li>
     *   <li>If {@code maxWidth} is less than {@code 0}, throw an
     *       {@code IllegalArgumentException}.</li>
     *   <li>If {@code offset} is less than {@code 0}, throw an
     *       {@code IllegalArgumentException}.</li>
     *   <li>In no case will it return a String of length greater than
     *       {@code maxWidth}.</li>
     * </ul>
     *
     * <pre>
     * Strings.truncate(null, 0, 0) = null
     * Strings.truncate(null, 2, 4) = null
     * Strings.truncate("", 0, 10) = ""
     * Strings.truncate("", 2, 10) = ""
     * Strings.truncate("abcdefghij", 0, 3) = "abc"
     * Strings.truncate("abcdefghij", 5, 6) = "fghij"
     * Strings.truncate("raspberry peach", 10, 15) = "peach"
     * Strings.truncate("abcdefghijklmno", 0, 10) = "abcdefghij"
     * Strings.truncate("abcdefghijklmno", -1, 10) = throws an IllegalArgumentException
     * Strings.truncate("abcdefghijklmno", Integer.MIN_VALUE, 10) = throws an IllegalArgumentException
     * Strings.truncate("abcdefghijklmno", Integer.MIN_VALUE, Integer.MAX_VALUE) = throws an IllegalArgumentException
     * Strings.truncate("abcdefghijklmno", 0, Integer.MAX_VALUE) = "abcdefghijklmno"
     * Strings.truncate("abcdefghijklmno", 1, 10) = "bcdefghijk"
     * Strings.truncate("abcdefghijklmno", 2, 10) = "cdefghijkl"
     * Strings.truncate("abcdefghijklmno", 3, 10) = "defghijklm"
     * Strings.truncate("abcdefghijklmno", 4, 10) = "efghijklmn"
     * Strings.truncate("abcdefghijklmno", 5, 10) = "fghijklmno"
     * Strings.truncate("abcdefghijklmno", 5, 5) = "fghij"
     * Strings.truncate("abcdefghijklmno", 5, 3) = "fgh"
     * Strings.truncate("abcdefghijklmno", 10, 3) = "klm"
     * Strings.truncate("abcdefghijklmno", 10, Integer.MAX_VALUE) = "klmno"
     * Strings.truncate("abcdefghijklmno", 13, 1) = "n"
     * Strings.truncate("abcdefghijklmno", 13, Integer.MAX_VALUE) = "no"
     * Strings.truncate("abcdefghijklmno", 14, 1) = "o"
     * Strings.truncate("abcdefghijklmno", 14, Integer.MAX_VALUE) = "o"
     * Strings.truncate("abcdefghijklmno", 15, 1) = ""
     * Strings.truncate("abcdefghijklmno", 15, Integer.MAX_VALUE) = ""
     * Strings.truncate("abcdefghijklmno", Integer.MAX_VALUE, Integer.MAX_VALUE) = ""
     * Strings.truncate("abcdefghij", 3, -1) = throws an IllegalArgumentException
     * Strings.truncate("abcdefghij", -2, 4) = throws an IllegalArgumentException
     * </pre>
     *
     * @param str the String to truncate, which may be null
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be positive
     * @return truncated String, {@code null} if {@code null} String input
     * @throws IllegalArgumentException If {@code offset} or {@code maxWidth} is less than {@code 0}
     */
    @MayReturnNull
    public static String truncate(final String str, final int offset, final int maxWidth) throws IllegalArgumentException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxWidth, cs.maxWidth);

        if (str == null) {
            return null;
        } else if (str.length() <= offset || maxWidth == 0) {
            return EMPTY;
        } else if (str.length() - offset <= maxWidth) {
            return offset == 0 ? str : str.substring(offset);
        } else {
            return str.substring(offset, offset + maxWidth);
        }
    }

    /**
     * Truncates each string in the provided array to the specified maximum width.
     * If a string in the array has a length less than or equal to the maxWidth, it remains unchanged.
     * If a string in the array has a length greater than the maxWidth, it is truncated to the maxWidth.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be truncated. Each string in the array will be updated in-place.
     * @param maxWidth The maximum length for each string. Must be non-negative.
     * @throws IllegalArgumentException If maxWidth is less than 0.
     */
    public static void truncate(final String[] strs, final int maxWidth) {
        N.checkArgNotNegative(maxWidth, cs.maxWidth);

        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = truncate(strs[i], maxWidth);
        }
    }

    /**
     * Truncates each string in the provided array to the specified maximum width starting from the given offset.
     * If a string in the array has a length less than or equal to the maxWidth, it remains unchanged.
     * If a string in the array has a length greater than the maxWidth, it is truncated to the maxWidth.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be truncated. Each string in the array will be updated in-place.
     * @param offset The starting index from where the string needs to be truncated.
     * @param maxWidth The maximum length for each string starting from the offset. Must be non-negative.
     * @throws IllegalArgumentException If maxWidth or offset is less than 0.
     */
    public static void truncate(final String[] strs, final int offset, final int maxWidth) {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxWidth, cs.maxWidth);

        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = truncate(strs[i], offset, maxWidth);
        }
    }

    // Delete
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Deletes all white spaces from a String as defined by
     * {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * Strings.deleteWhitespace(null)         = null
     * Strings.deleteWhitespace("")           = ""
     * Strings.deleteWhitespace("abc")        = "abc"
     * Strings.deleteWhitespace("   ab  c  ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to delete whitespace from, which may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String deleteWhitespace(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final int len = str.length();
        final char[] cbuf = new char[len];
        int count = 0;
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = str.charAt(i);

            if (!Character.isWhitespace(ch)) {
                cbuf[count++] = ch;
            }
        }

        return count == cbuf.length ? str : new String(cbuf, 0, count);
    }

    /**
     * Deletes all white spaces from each string in the provided array.
     * White spaces are determined by {@link Character#isWhitespace(char)}.
     * If the input array is {@code null} or empty, the method does nothing.
     *
     * @param strs The array of strings to be processed. Each string in the array will be updated in-place.
     */
    public static void deleteWhitespace(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = deleteWhitespace(strs[i]);
        }
    }

    /**
     * Appends the specified suffix to the input string if it is not already present at the end of the string.
     * If the input string is {@code null} or empty, the suffix is returned as is.
     *
     * @param str The string to which the suffix should be appended. May be {@code null} or empty.
     * @param suffix The suffix to append to the string. Must not be empty.
     * @return The input string with the suffix appended if it was not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the suffix is empty.
     */
    public static String appendIfMissing(final String str, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(suffix, cs.suffix);

        if (str == null || str.isEmpty()) {
            return suffix;
        } else if (str.endsWith(suffix)) {
            return str;
        } else {
            return str + suffix;
        }
    }

    /**
     * Appends the specified suffix to the input string if it is not already present at the end of the string.
     * The check for the presence of the suffix is case-insensitive.
     * If the input string is {@code null} or empty, the suffix is returned as is.
     *
     * @param str The string to which the suffix should be appended. May be {@code null} or empty.
     * @param suffix The suffix to append to the string. Must not be empty.
     * @return The input string with the suffix appended if it was not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the suffix is empty.
     */
    public static String appendIfMissingIgnoreCase(final String str, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(suffix, cs.suffix);

        if (str == null || str.isEmpty()) {
            return suffix;
        } else if (Strings.endsWithIgnoreCase(str, suffix)) {
            return str;
        } else {
            return str + suffix;
        }
    }

    /**
     * Prepends the specified prefix to the input string if it is not already present at the start of the string.
     * If the input string is {@code null} or empty, the prefix is returned as is.
     *
     * @param str The string to which the prefix should be prepended. May be {@code null} or empty.
     * @param prefix The prefix to prepend to the string. Must not be empty.
     * @return The input string with the prefix prepended if it was not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefix is empty.
     */
    public static String prependIfMissing(final String str, final String prefix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);

        if (str == null || str.isEmpty()) {
            return prefix;
        } else if (str.startsWith(prefix)) {
            return str;
        } else {
            return concat(prefix, str);
        }
    }

    /**
     * Prepends the specified prefix to the input string if it is not already present at the start of the string.
     * The check for the presence of the prefix is case-insensitive.
     * If the input string is {@code null} or empty, the prefix is returned as is.
     *
     * @param str The string to which the prefix should be prepended. May be {@code null} or empty.
     * @param prefix The prefix to prepend to the string. Must not be empty.
     * @return The input string with the prefix prepended if it was not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefix is empty.
     */
    public static String prependIfMissingIgnoreCase(final String str, final String prefix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);

        if (str == null || str.isEmpty()) {
            return prefix;
        } else if (Strings.startsWithIgnoreCase(str, prefix)) {
            return str;
        } else {
            return concat(prefix, str);
        }
    }

    /**
     * Wraps the input string with the specified prefix and suffix if they are not already present.
     * <li>If the input string is {@code null} or empty, the prefix and suffix are concatenated and returned.</li>
     * <li>If the input string already starts with the prefix and ends with the suffix, the original string is returned.</li>
     * <li>Else if the input string already starts with the prefix but not ends with the suffix, then {@code str + prefixSuffix} is returned.</li>
     * <li>Else if the input string ends with the suffix, then {@code prefixSuffix + str} is returned.</li>
     * <li>Otherwise, the prefix and suffix are added to the start and end of the string respectively.</li>
     *
     * @param str The string to be wrapped. May be {@code null} or empty.
     * @param prefixSuffix The string to be used as both the prefix and suffix for wrapping. Must not be empty.
     * @return The input string wrapped with the prefixSuffix at both ends if they were not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefixSuffix is empty.
     */
    public static String wrapIfMissing(final String str, final String prefixSuffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefixSuffix, cs.prefixSuffix);

        return wrapIfMissing(str, prefixSuffix, prefixSuffix);
    }

    /**
     * Wraps the input string with the specified prefix and suffix if they are not already present.
     * <li>If the input string is {@code null} or empty, the prefix and suffix are concatenated and returned.</li>
     * <li>If the input string already starts with the prefix and ends with the suffix, the original string is returned.</li>
     * <li>Else if the input string already starts with the prefix but not ends with the suffix, then {@code str + suffix} is returned.</li>
     * <li>Else if the input string ends with the suffix, then {@code prefix + str} is returned.</li>
     * <li>Otherwise, the prefix and suffix are added to the start and end of the string respectively.</li>
     *
     * <pre>
     * Strings.wrapIfMissing(null, "[", "]") -> "[]"
     * Strings.wrapIfMissing("", "[", "]") -> "[]"
     * Strings.wrapIfMissing("[", "[", "]") -> "[]"
     * Strings.wrapIfMissing("]", "[", "]") -> "[]"
     * Strings.wrapIfMissing("abc", "[", "]") -> "[abc]"
     * Strings.wrapIfMissing("a", "aa", "aa") -> "aaaaa"
     * Strings.wrapIfMissing("aa", "aa", "aa") -> "aaaa"
     * Strings.wrapIfMissing("aaa", "aa", "aa") -> "aaaaa"
     * Strings.wrapIfMissing("aaaa", "aa", "aa") -> "aaaa"
     * </pre>
     *
     * @param str The string to be wrapped. May be {@code null} or empty.
     * @param prefix The string to be used as the prefix for wrapping. Must not be empty.
     * @param suffix The string to be used as the suffix for wrapping. Must not be empty.
     * @return The input string wrapped with the prefix and suffix at both ends if they were not already present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefix or suffix is empty.
     */
    public static String wrapIfMissing(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);
        N.checkArgNotEmpty(suffix, cs.suffix);

        if (isEmpty(str)) {
            return concat(prefix, suffix);
        } else if (str.startsWith(prefix)) {
            return (str.length() - prefix.length() >= suffix.length() && str.endsWith(suffix)) ? str : concat(str + suffix);
        } else if (str.endsWith(suffix)) {
            return concat(prefix, str);
        } else {
            return concat(prefix, str, suffix);
        }
    }

    /**
     * Wraps the input string with the specified prefix and suffix.
     * If the input string is {@code null}, it will be treated as an empty string.
     *
     * @param str The string to be wrapped. May be {@code null}.
     * @param prefixSuffix The string to be used as both the prefix and suffix for wrapping. Must not be empty.
     * @return The input string wrapped with the prefixSuffix at both ends.
     * @throws IllegalArgumentException If the prefixSuffix is empty.
     */
    public static String wrap(final String str, final String prefixSuffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefixSuffix, cs.prefixSuffix);

        return wrap(str, prefixSuffix, prefixSuffix);
    }

    /**
     * Wraps the input string with the specified prefix and suffix.
     * If the input string is {@code null}, it will be treated as an empty string.
     *
     * <pre>
     * Strings.wrap(null, "[", "]") -> "[]"
     * Strings.wrap("", "[", "]") -> "[]"
     * Strings.wrap("[", "[", "]") -> "[[]"
     * Strings.wrap("]", "[", "]") -> "[]]"
     * Strings.wrap("abc", "[", "]") -> "[abc]"
     * Strings.wrap("a", "aa", "aa") -> "aaaaa"
     * Strings.wrap("aa", "aa", "aa") -> "aaaaaa"
     * Strings.wrap("aaa", "aa", "aa") -> "aaaaaaa"
     * </pre>
     *
     * @param str The string to be wrapped. May be {@code null}.
     * @param prefix The string to be used as the prefix for wrapping. Must not be empty.
     * @param suffix The string to be used as the suffix for wrapping. Must not be empty.
     * @return The input string wrapped with the prefix and suffix at both ends.
     * @throws IllegalArgumentException If the prefix or suffix is empty.
     */
    public static String wrap(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);
        N.checkArgNotEmpty(suffix, cs.suffix);

        if (isEmpty(str)) {
            return concat(prefix, suffix);
        } else {
            return concat(prefix, str, suffix);
        }
    }

    /**
     * Unwraps the input string if it is wrapped by the specified prefixSuffix at both ends.
     * If the input string is {@code null} or empty, the original string is returned.
     * If the input string is not wrapped by the prefixSuffix, the original string is returned.
     * If the input string is wrapped by the prefixSuffix, the prefixSuffix is removed from both ends.
     *
     * @param str The string to be unwrapped. May be {@code null} or empty.
     * @param prefixSuffix The string used as both the prefix and suffix for unwrapping. Must not be empty.
     * @return The input string with the prefixSuffix removed from both ends if they were present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefixSuffix is empty.
     */
    public static String unwrap(final String str, final String prefixSuffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefixSuffix, cs.prefixSuffix);

        return unwrap(str, prefixSuffix, prefixSuffix);
    }

    /**
     * Unwraps the input string if it is wrapped by the specified prefix and suffix.
     * If the input string is {@code null} or empty, the original string is returned.
     * If the input string is not wrapped by the prefix and suffix, the original string is returned.
     * If the input string is wrapped by the prefix and suffix, the prefix and suffix are removed from both ends.
     *
     * <pre>
     * Strings.unwrap(null, "[", "]") -> ""
     * Strings.unwrap("", "[", "]") -> ""
     * Strings.unwrap("[", "[", "]") -> "["
     * Strings.unwrap("]", "[", "]") -> "["
     * Strings.unwrap("[abc]", "[", "]") -> "abc"
     * Strings.unwrap("aaaaa", "aa", "aa") -> "a"
     * Strings.unwrap("aa", "aa", "aa") -> "aa"
     * Strings.unwrap("aaa", "aa", "aa") -> "aaa"
     * Strings.unwrap("aaaa", "aa", "aa") -> ""
     * </pre>
     *
     * @param str The string to be unwrapped. May be {@code null} or empty.
     * @param prefix The string used as the prefix for unwrapping. Must not be empty.
     * @param suffix The string used as the suffix for unwrapping. Must not be empty.
     * @return The input string with the prefix and suffix removed from both ends if they were present; otherwise, the original string.
     * @throws IllegalArgumentException If the prefix or suffix is empty.
     */
    public static String unwrap(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotEmpty(prefix, cs.prefix);
        N.checkArgNotEmpty(suffix, cs.suffix);

        if (str == null || str.isEmpty()) {
            return str;
        } else if (str.length() - prefix.length() >= suffix.length() && str.startsWith(prefix) && str.endsWith(suffix)) {
            return str.substring(prefix.length(), str.length() - suffix.length());
        } else {
            return str;
        }
    }

    /**
     * Checks if is lower case.
     *
     * @param ch
     * @return {@code true} if is lower case
     */
    public static boolean isLowerCase(final char ch) {
        return Character.isLowerCase(ch);
    }

    /**
     * Checks if is ascii lower case.
     *
     * @param ch
     * @return {@code true} if is ascii lower case
     */
    public static boolean isAsciiLowerCase(final char ch) {
        return (ch >= 'a') && (ch <= 'z');
    }

    /**
     * Checks if is upper case.
     *
     * @param ch
     * @return {@code true} if is upper case
     */
    public static boolean isUpperCase(final char ch) {
        return Character.isUpperCase(ch);
    }

    /**
     * Checks if is ascii upper case.
     *
     * @param ch
     * @return {@code true} if is ascii upper case
     */
    public static boolean isAsciiUpperCase(final char ch) {
        return (ch >= 'A') && (ch <= 'Z');
    }

    /**
     * Checks if all characters in the given CharSequence are lower case.
     *
     * @param cs the CharSequence to check, which may be null
     * @return {@code true} if all characters are lower case or the CharSequence is empty; {@code false} otherwise
     */
    public static boolean isAllLowerCase(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isLowerCase(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all characters in the given CharSequence are upper case.
     *
     * @param cs the CharSequence to check, which may be null
     * @return {@code true} if all characters are upper case or the CharSequence is empty; {@code false} otherwise
     */
    public static boolean isAllUpperCase(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isUpperCase(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Copied from Apache Commons Lang: StringUtils#isMixedCase.
     *
     * Checks if the given CharSequence is mixed case.
     * A CharSequence is considered mixed case if it contains both uppercase and lowercase characters.
     * If the CharSequence is empty or only contains a single character, it is not considered mixed case.
     *
     * @param cs The CharSequence to check. It may be {@code null}.
     * @return {@code true} if the CharSequence is mixed case, {@code false} otherwise.
     */
    public static boolean isMixedCase(final CharSequence cs) {
        if (isEmpty(cs) || cs.length() == 1) {
            return false;
        }

        boolean containsUppercase = false;
        boolean containsLowercase = false;
        final int len = cs.length();

        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (Character.isUpperCase(ch)) {
                containsUppercase = true;
            } else if (Character.isLowerCase(ch)) {
                containsLowercase = true;
            }

            if (containsUppercase && containsLowercase) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if is digit.
     *
     * @param ch
     * @return {@code true} if is digit
     * @see Character#isDigit(char)
     */
    public static boolean isDigit(final char ch) {
        return Character.isDigit(ch);
    }

    /**
     * Checks if is letter.
     *
     * @param ch
     * @return {@code true} if is letter
     * @see Character#isLetter(char)
     */
    public static boolean isLetter(final char ch) {
        return Character.isLetter(ch);
    }

    /**
     * Checks if is letter or digit.
     *
     * @param ch
     * @return {@code true} if is letter or digit
     * @see Character#isLetterOrDigit(char)
     */
    public static boolean isLetterOrDigit(final char ch) {
        return Character.isLetterOrDigit(ch);
    }

    // --------------------------------------------------------------------------

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit.
     * </p>
     *
     * <pre>
     *   Strings.isAscii('a')  = true
     *   Strings.isAscii('A')  = true
     *   Strings.isAscii('3')  = true
     *   Strings.isAscii('-')  = true
     *   Strings.isAscii('\n') = true
     *   Strings.isAscii('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if less than 128
     */
    public static boolean isAscii(final char ch) {
        return ch < 128;
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit printable.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiPrintable('a')  = true
     *   Strings.isAsciiPrintable('A')  = true
     *   Strings.isAsciiPrintable('3')  = true
     *   Strings.isAsciiPrintable('-')  = true
     *   Strings.isAsciiPrintable('\n') = false
     *   Strings.isAsciiPrintable('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 32 and 126 inclusive
     */
    public static boolean isAsciiPrintable(final char ch) {
        return ch > 31 && ch < 127;
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit control.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiControl('a')  = false
     *   Strings.isAsciiControl('A')  = false
     *   Strings.isAsciiControl('3')  = false
     *   Strings.isAsciiControl('-')  = false
     *   Strings.isAsciiControl('\n') = true
     *   Strings.isAsciiControl('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if less than 32 or equals 127
     */
    public static boolean isAsciiControl(final char ch) {
        return ch < 32 || ch == 127;
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit alphabetic.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiAlpha('a')  = true
     *   Strings.isAsciiAlpha('A')  = true
     *   Strings.isAsciiAlpha('3')  = false
     *   Strings.isAsciiAlpha('-')  = false
     *   Strings.isAsciiAlpha('\n') = false
     *   Strings.isAsciiAlpha('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 65 and 90 or 97 and 122 inclusive
     */
    public static boolean isAsciiAlpha(final char ch) {
        return isAsciiAlphaUpper(ch) || isAsciiAlphaLower(ch);
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit alphabetic upper case.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiAlphaUpper('a')  = false
     *   Strings.isAsciiAlphaUpper('A')  = true
     *   Strings.isAsciiAlphaUpper('3')  = false
     *   Strings.isAsciiAlphaUpper('-')  = false
     *   Strings.isAsciiAlphaUpper('\n') = false
     *   Strings.isAsciiAlphaUpper('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 65 and 90 inclusive
     */
    public static boolean isAsciiAlphaUpper(final char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit alphabetic lower case.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiAlphaLower('a')  = true
     *   Strings.isAsciiAlphaLower('A')  = false
     *   Strings.isAsciiAlphaLower('3')  = false
     *   Strings.isAsciiAlphaLower('-')  = false
     *   Strings.isAsciiAlphaLower('\n') = false
     *   Strings.isAsciiAlphaLower('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 97 and 122 inclusive
     */
    public static boolean isAsciiAlphaLower(final char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit numeric.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiNumeric('a')  = false
     *   Strings.isAsciiNumeric('A')  = false
     *   Strings.isAsciiNumeric('3')  = true
     *   Strings.isAsciiNumeric('-')  = false
     *   Strings.isAsciiNumeric('\n') = false
     *   Strings.isAsciiNumeric('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 48 and 57 inclusive
     */
    public static boolean isAsciiNumeric(final char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * <p>
     * Checks whether the character is ASCII 7 bit numeric.
     * </p>
     *
     * <pre>
     *   Strings.isAsciiAlphanumeric('a')  = true
     *   Strings.isAsciiAlphanumeric('A')  = true
     *   Strings.isAsciiAlphanumeric('3')  = true
     *   Strings.isAsciiAlphanumeric('-')  = false
     *   Strings.isAsciiAlphanumeric('\n') = false
     *   Strings.isAsciiAlphanumeric('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return {@code true} if between 48 and 57 or 65 and 90 or 97 and 122 inclusive
     */
    public static boolean isAsciiAlphanumeric(final char ch) {
        return isAsciiAlpha(ch) || isAsciiNumeric(ch);
    }

    /**
     * Checks if the CharSequence contains only ASCII printable characters.
     *
     * <p>{@code null} will return {@code false}.
     * An empty CharSequence (length()=0) will return {@code true}.</p>
     *
     * <pre>
     * Strings.isAsciiPrintable(null)     = false
     * Strings.isAsciiPrintable("")       = true
     * Strings.isAsciiPrintable(" ")      = true
     * Strings.isAsciiPrintable("Ceki")   = true
     * Strings.isAsciiPrintable("ab2c")   = true
     * Strings.isAsciiPrintable("!ab-c~") = true
     * Strings.isAsciiPrintable("\u0020") = true
     * Strings.isAsciiPrintable("\u0021") = true
     * Strings.isAsciiPrintable("\u007e") = true
     * Strings.isAsciiPrintable("\u007f") = false
     * Strings.isAsciiPrintable("Ceki G\u00fclc\u00fc") = false
     * </pre>
     *
     * @param cs
     * @return {@code true} if is ascii printable. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     */
    public static boolean isAsciiPrintable(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!isAsciiPrintable(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is ascii alpha.
     *
     * @param cs
     * @return {@code true} if is ascii alpha, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     */
    public static boolean isAsciiAlpha(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!isAsciiAlpha(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is ascii alpha space.
     *
     * @param cs
     * @return {@code true} if is ascii alpha space, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     */
    public static boolean isAsciiAlphaSpace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (!isAsciiAlpha(ch) && ch != ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is ascii alphanumeric.
     *
     * @param cs
     * @return {@code true} if is ascii alphanumeric, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     */
    public static boolean isAsciiAlphanumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!isAsciiAlphanumeric(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is ascii alphanumeric space.
     *
     * @param cs
     * @return {@code true} if is ascii alphanumeric space, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     */
    public static boolean isAsciiAlphanumericSpace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (!isAsciiAlphanumeric(ch) && ch != ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is ascii numeric.
     *
     * @param cs
     * @return {@code true} if is ascii numeric, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     */
    public static boolean isAsciiNumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!isAsciiNumeric(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    // Character Tests
    // -----------------------------------------------------------------------

    /**
     * <p>
     * Checks if the CharSequence contains only Unicode letters.
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * Strings.isAlpha(null)   = false
     * Strings.isAlpha("")     = false
     * Strings.isAlpha("  ")   = false
     * Strings.isAlpha("abc")  = true
     * Strings.isAlpha("ab2c") = false
     * Strings.isAlpha("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains letters, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     *        isAlpha(CharSequence)
     */
    public static boolean isAlpha(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isLetter(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Checks if the CharSequence contains only Unicode letters and space (' ').
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * Strings.isAlphaSpace(null)   = false
     * Strings.isAlphaSpace("")     = true
     * Strings.isAlphaSpace("  ")   = true
     * Strings.isAlphaSpace("abc")  = true
     * Strings.isAlphaSpace("ab c") = true
     * Strings.isAlphaSpace("ab2c") = false
     * Strings.isAlphaSpace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains letters and space, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     *        isAlphaSpace(CharSequence)
     */
    public static boolean isAlphaSpace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (!Character.isLetter(ch) && ch != ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Checks if the CharSequence contains only Unicode letters or digits.
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * Strings.isAlphanumeric(null)   = false
     * Strings.isAlphanumeric("")     = false
     * Strings.isAlphanumeric("  ")   = false
     * Strings.isAlphanumeric("abc")  = true
     * Strings.isAlphanumeric("ab c") = false
     * Strings.isAlphanumeric("ab2c") = true
     * Strings.isAlphanumeric("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains letters or digits, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     *        isAlphanumeric(CharSequence)
     */
    public static boolean isAlphanumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isLetterOrDigit(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Checks if the CharSequence contains only Unicode letters, digits or space
     * ({@code ' '}).
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * Strings.isAlphanumericSpace(null)   = false
     * Strings.isAlphanumericSpace("")     = true
     * Strings.isAlphanumericSpace("  ")   = true
     * Strings.isAlphanumericSpace("abc")  = true
     * Strings.isAlphanumericSpace("ab c") = true
     * Strings.isAlphanumericSpace("ab2c") = true
     * Strings.isAlphanumericSpace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains letters, digits or space, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     *        isAlphanumericSpace(CharSequence)
     */
    public static boolean isAlphanumericSpace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (!Character.isLetterOrDigit(ch) && ch != ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * Checks if the CharSequence contains only Unicode digits. A decimal point
     * is not a Unicode digit and returns {@code false}.
     * </p>
     *
     * <p>
     * {@code null} will return {@code false}. An empty CharSequence
     * (length()=0) will return {@code false}.
     * </p>
     *
     * <p>
     * Note that the method does not allow for a leading sign, either positive
     * or negative. Also, if a String passes the numeric test, it may still
     * generate a NumberFormatException when parsed by Integer.parseInt or
     * Long.parseLong, e.g., if the value is outside the range for int or long
     * respectively.
     * </p>
     *
     * <pre>
     * Strings.isNumeric(null)   = false
     * Strings.isNumeric("")     = false
     * Strings.isNumeric("  ")   = false
     * Strings.isNumeric("123")  = true
     * Strings.isNumeric("12 3") = false
     * Strings.isNumeric("ab2c") = false
     * Strings.isNumeric("12-3") = false
     * Strings.isNumeric("12.3") = false
     * Strings.isNumeric("-123") = false
     * Strings.isNumeric("+123") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains digits, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     *        isNumeric(CharSequence)
     */
    public static boolean isNumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the CharSequence contains only Unicode digits or space
     * ({@code ' '}).
     * A decimal point is not a Unicode digit and returns {@code false}.
     *
     * <p>{@code null} will return {@code false}.
     * An empty CharSequence (length()=0) will return {@code true}.</p>
     *
     * <pre>
     * Strings.isNumericSpace(null)   = false
     * Strings.isNumericSpace("")     = true
     * Strings.isNumericSpace("  ")   = true
     * Strings.isNumericSpace("123")  = true
     * Strings.isNumericSpace("12 3") = true
     * Strings.isNumericSpace("\u0967\u0968\u0969")  = true
     * Strings.isNumericSpace("\u0967\u0968 \u0969")  = true
     * Strings.isNumericSpace("ab2c") = false
     * Strings.isNumericSpace("12-3") = false
     * Strings.isNumericSpace("12.3") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains digits or space, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     *        isNumericSpace(CharSequence)
     */
    public static boolean isNumericSpace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();
        char ch = 0;

        for (int i = 0; i < len; i++) {
            ch = cs.charAt(i);

            if (!Character.isDigit(ch) && ch != ' ') {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the CharSequence contains only whitespace.
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <p>{@code null} will return {@code false}.
     * An empty CharSequence (length()=0) will return {@code true}.</p>
     *
     * <pre>
     * Strings.isWhitespace(null)   = false
     * Strings.isWhitespace("")     = true
     * Strings.isWhitespace("  ")   = true
     * Strings.isWhitespace("abc")  = false
     * Strings.isWhitespace("ab2c") = false
     * Strings.isWhitespace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, which may be null
     * @return {@code true} if only contains whitespace, and is {@code non-null}. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     *        isWhitespace(CharSequence)
     */
    public static boolean isWhitespace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Note: It's copied from NumberUtils in Apache Commons Lang under Apache
     * License 2.0
     *
     * <p>
     * Checks whether the String a valid Java number. {@code true} is
     * returned if there is a number which can be initialized by
     * {@code createNumber} with specified String.
     * </p>
     *
     * <p>
     * {@code Null} and empty String will return {@code false}.
     * </p>
     *
     * @param str the {@code String} to check
     * @return {@code true} if the string is a correctly formatted number
     * @see Numbers#isNumber(String)
     * @see Numbers#isCreatable(String)
     * @see Numbers#isParsable(String)
     *        validation
     * @deprecated use {@link Numbers#isNumber(String)} instead
     */
    @Deprecated
    public static boolean isNumber(final String str) {
        return Numbers.isNumber(str);
    }

    /**
     * {@code true} is returned if the specified {@code str} only
     * includes characters ('0' ~ '9', '.', '-', '+', 'e').
     * {@code false} is return if the specified String is null/empty, or contains empty chars.
     *
     *  "0" => true
     *  " 0.1 " => false
     *  "abc" => false
     *  "1 a" => false
     *  "2e10" => true
     *  "2E-10" => true
     *
     * @param str
     * @return {@code true} if is ascii digital number
     */
    public static boolean isAsciiDigitalNumber(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        int count = 0;
        final int len = str.length();
        char ch = str.charAt(0);
        int i = 0;

        if (ch == '+' || ch == '-') {
            i++;
        }

        for (; i < len; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                count++;
            } else {
                break;
            }
        }

        if (i < len && str.charAt(i) == '.') {
            if (count == 0) {
                return false;
            } else {
                count = 0;
            }

            i++;
        }

        for (; i < len; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                count++;
            } else {
                break;
            }
        }

        if (count == 0) {
            return false;
        }

        if (i == len) {
            return true;
        }

        ch = str.charAt(i);

        if (ch != 'e' && ch != 'E') {
            return false;
        } else {
            i++;
        }

        count = 0;

        if (i < len && (str.charAt(i) == '+' || str.charAt(i) == '-')) {
            i++;
        }

        for (; i < len; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                count++;
            } else {
                break;
            }
        }

        //NOSONAR
        return (count != 0) && (i == len);
    }

    /**
     * {@code true} is returned if the specified {@code str} only
     * includes characters ('0' ~ '9', '-', '+' ).
     * {@code false} is return if the specified String is null/empty, or contains empty chars.
     *
     *  "-123" => true
     *  "+123" => true
     *  "123" => true
     *  "+0" => true
     *  "-0" => true
     *  "0" => true
     *  " 0.1 " => false
     *  "abc" => false
     *  "1 a" => false
     *  "2e10" => false
     *
     * @param str
     * @return {@code true} if is ascii digital integer
     */
    public static boolean isAsciiDigitalInteger(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        int count = 0;

        final int len = str.length();
        char ch = str.charAt(0);
        int i = 0;

        if (ch == '+' || ch == '-') {
            i++;
        }

        for (; i < len; i++) {
            ch = str.charAt(i);

            if (ch >= '0' && ch <= '9') {
                count++;
            } else {
                break;
            }
        }

        if (count == 0) {
            return false;
        }

        return i == len;
    }

    /**
     * Returns the index within this string of the first occurrence of the specified character.
     * If a character with value {@code charValueToFind} occurs in the character sequence represented by
     * this {@code String} object, then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param charValueToFind The Unicode code of the character to be found.
     * @return The index of the first occurrence of the character in the character sequence represented by this object,
     *         or -1 if the character does not occur.
     */
    public static int indexOf(final String str, final int charValueToFind) {
        if (str == null || str.isEmpty()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(charValueToFind);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified character, starting the search at the specified index.
     * If a character with value {@code charValueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param charValueToFind The Unicode code of the character to be found.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the character in the character sequence represented by this object,
     *         or -1 if the character does not occur.
     */
    public static int indexOf(final String str, final int charValueToFind, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (str == null || str.isEmpty()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(charValueToFind, fromIndex);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOf(final String str, final String valueToFind) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(valueToFind);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring, starting the search at the specified index.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOf(final String str, final String valueToFind, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (str == null || valueToFind == null || valueToFind.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(valueToFind, fromIndex);
    }

    /**
     * Returns the index within the input string of the first occurrence of any specified character.
     * If a character within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToFind The array of characters to be found.
     * @return The index of the first occurrence of any character in the character sequence represented by this object,
     *         or -1 if none of the characters occur.
     */
    public static int indexOfAny(final String str, final char... valuesToFind) {
        return indexOfAny(str, 0, valuesToFind);
    }

    /**
     * Returns the index within the input string of the first occurrence of any specified character, starting the search at the specified index.
     * If a character within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param fromIndex The index to start the search from.
     * @param valuesToFind The array of characters to be found.
     * @return The index of the first occurrence of any character in the character sequence represented by this object,
     *         or -1 if none of the characters occur.
     */
    public static int indexOfAny(final String str, int fromIndex, final char... valuesToFind) {
        fromIndex = Math.max(0, fromIndex);

        checkInputChars(valuesToFind, cs.valuesToFind, true);

        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = str.length();
        char ch = 0;

        for (int i = fromIndex; i < strLen; i++) {
            ch = str.charAt(i);

            for (final char c : valuesToFind) {
                if (c == ch) {
                    // checked by checkInputChars

                    //    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                    //        // ch is a supplementary character
                    //        if (valuesToFind[j + 1] == str.charAt(i + 1)) {
                    //            return i;
                    //        }
                    //    } else {
                    //        return i;
                    //    }

                    return i;
                }
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the first occurrence of any specified substring.
     * If a substring within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToFind The array of substrings to be found.
     * @return The index of the first occurrence of any substring in the character sequence represented by this object,
     *         or -1 if none of the substrings occur.
     */
    public static int indexOfAny(final String str, final String... valuesToFind) {
        return indexOfAny(str, 0, valuesToFind);
    }

    /**
     * Returns the index within the input string of the first occurrence of any specified substring, starting the search at the specified index.
     * If a substring within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param fromIndex The index to start the search from.
     * @param valuesToFind The array of substrings to be found.
     * @return The index of the first occurrence of any substring in the character sequence represented by this object,
     *         or -1 if none of the substrings occur.
     */
    public static int indexOfAny(final String str, int fromIndex, final String... valuesToFind) {
        fromIndex = Math.max(0, fromIndex);

        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        int idx = 0;

        for (final String element : valuesToFind) {
            if (isEmpty(element)) {
                continue;
            }

            idx = indexOf(str, element, fromIndex);

            if (idx != N.INDEX_NOT_FOUND) {
                return idx;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the first occurrence of any character
     * that is not in the specified array of characters to exclude.
     * If a character not within the array {@code valuesToExclude} occurs in the character
     * sequence represented by the input {@code String} object, then the index of the first
     * such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToExclude The array of characters to exclude from the search.
     * @return The index of the first occurrence of any character not in the array of characters
     *         to exclude, or -1 if all characters are in the array or the string is {@code null} or empty.
     */
    public static int indexOfAnyBut(final String str, final char... valuesToExclude) {
        return indexOfAnyBut(str, 0, valuesToExclude);
    }

    /**
     * Returns the index within the input string of the first occurrence of any character
     * that is not in the specified array of characters to exclude, starting the search at the specified index.
     * If a character not within the array {@code valuesToExclude} occurs in the character
     * sequence represented by the input {@code String} object, then the index of the first
     * such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param fromIndex The index to start the search from.
     * @param valuesToExclude The array of characters to exclude from the search.
     * @return The index of the first occurrence of any character not in the array of characters
     *         to exclude, or -1 if all characters are in the array or the string is {@code null} or empty.
     */
    public static int indexOfAnyBut(final String str, int fromIndex, final char... valuesToExclude) {
        fromIndex = Math.max(0, fromIndex);

        checkInputChars(valuesToExclude, cs.valuesToExclude, true);

        if (str == null || str.isEmpty()) {
            return N.INDEX_NOT_FOUND;
        }

        if (N.isEmpty(valuesToExclude)) {
            return 0;
        }

        final int strLen = str.length();
        char ch = 0;

        outer: for (int i = fromIndex; i < strLen; i++) {//NOSONAR
            ch = str.charAt(i);

            for (final char c : valuesToExclude) {
                if (c == ch) {
                    // checked by checkInputChars

                    //    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                    //        if (valuesToExclude[j + 1] == str.charAt(i + 1)) {
                    //            continue outer;
                    //        }
                    //    } else {
                    //        continue outer;
                    //    }

                    continue outer;
                }
            }

            return i;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring,
     * using the specified delimiter to separate the search.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to separate the search.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOf(final String str, final String valueToFind, final String delimiter) {
        return indexOf(str, valueToFind, delimiter, 0);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring,
     * using the specified delimiter to separate the search, starting the search at the specified index.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to separate the search.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOf(final String str, final String valueToFind, final String delimiter, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (isEmpty(delimiter)) {
            return indexOf(str, valueToFind, fromIndex);
        }

        if (str == null || valueToFind == null) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        final int targetLen = fromIndex > 0 ? len - fromIndex : len;
        final int substrLen = valueToFind.length();
        final int delimiterLen = delimiter.length();

        if (targetLen < substrLen || (targetLen > substrLen && targetLen - substrLen < delimiterLen)) {
            return N.INDEX_NOT_FOUND;
        }

        final int index = str.indexOf(valueToFind, fromIndex);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        if ((index == fromIndex || (index - fromIndex >= delimiterLen && delimiter.equals(str.substring(index - delimiterLen, index))))
                && (index + substrLen == len
                        || (len >= index + substrLen + delimiterLen && delimiter.equals(str.substring(index + substrLen, index + substrLen + delimiterLen))))) {
            return index;
        }

        int idx = str.indexOf(delimiter + valueToFind + delimiter, index);

        if (idx >= 0) {
            return idx + delimiterLen;
        } else {
            idx = str.indexOf(delimiter + valueToFind, index);

            if (idx >= 0 && idx + delimiterLen + substrLen == len) {
                return idx + delimiterLen;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring, ignoring case considerations.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind) {
        return indexOfIgnoreCase(str, valueToFind, 0);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring, ignoring case considerations,
     * starting the search at the specified index.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (str == null || valueToFind == null || valueToFind.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        // performance optimization
        if (valueToFind.length() <= 3 && valueToFind.toUpperCase().equals(valueToFind.toLowerCase())) {
            return str.indexOf(valueToFind, fromIndex);
        }

        for (int i = fromIndex, len = str.length(), substrLen = valueToFind.length(), end = len - substrLen + 1; i < end; i++) {
            if (str.regionMatches(true, i, valueToFind, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring, ignoring case considerations.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to be used for the search.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object, or -1 if the substring does not occur.
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, final String delimiter) {
        return indexOfIgnoreCase(str, valueToFind, delimiter, 0);
    }

    /**
     * Returns the index within the input string of the first occurrence of the specified substring, ignoring case considerations.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the first such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to be used for the search.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the substring in the character sequence represented by this object, or -1 if the substring does not occur.
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, final String delimiter, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (isEmpty(delimiter)) {
            return indexOfIgnoreCase(str, valueToFind, fromIndex);
        }

        if (str == null || valueToFind == null) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        final int targetLen = fromIndex > 0 ? len - fromIndex : len;
        final int substrLen = valueToFind.length();
        final int delimiterLen = delimiter.length();

        if (targetLen < substrLen || (targetLen > substrLen && targetLen - substrLen < delimiterLen)) {
            return N.INDEX_NOT_FOUND;
        }

        final int index = indexOfIgnoreCase(str, valueToFind, fromIndex);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        if ((index == fromIndex || (index - fromIndex >= delimiterLen && delimiter.equalsIgnoreCase(str.substring(index - delimiterLen, index))))
                && (index + substrLen == len || (len >= index + substrLen + delimiterLen
                        && delimiter.equalsIgnoreCase(str.substring(index + substrLen, index + substrLen + delimiterLen))))) {
            return index;
        }

        int idx = indexOfIgnoreCase(str, delimiter + valueToFind + delimiter, index);

        if (idx >= 0) {
            return idx + delimiterLen;
        } else {
            idx = indexOfIgnoreCase(str, delimiter + valueToFind, index);

            if (idx >= 0 && idx + delimiterLen + substrLen == len) {
                return idx + delimiterLen;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within this string of the last occurrence of the specified character.
     * If a character with value {@code charValueToFind} occurs in the character sequence represented by
     * this {@code String} object, then the index of the last such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param charValueToFind The Unicode code of the character to be found.
     * @return The index of the last occurrence of the character in the character sequence represented by this object, or -1 if the character does not occur.
     */
    public static int lastIndexOf(final String str, final int charValueToFind) {
        if (str == null || str.isEmpty()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(charValueToFind);
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified character, searching backward starting at the specified index.
     * For values of {@code ch} in the range from 0 to 0xFFFF (inclusive),
     * the index returned is the largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * {@code (this.charAt(<i>k</i>) == ch) && (<i>k</i> <= fromIndex)}
     * </pre>
     *
     * </blockquote> is {@code true}. For other values of {@code ch}, it is the
     * largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * {@code (this.codePointAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)}
     * </pre>
     *
     * </blockquote> is {@code true}. In either case, if no such character occurs in
     * this string at or before position {@code fromIndex}, then
     * {@code -1} is returned.
     *
     * <p>
     * All indices are specified in {@code char} values (Unicode code
     * units).
     *
     * @param str
     * @param charValueToFind a character (Unicode code point).
     * @param startIndexFromBack the index to start the search from. There is no restriction on
     *            the value of {@code fromIndex}. If it is greater than or
     *            equal to the length of this string, it has the same effect as
     *            if it were equal to one less than the length of this string:
     *            this entire string may be searched. If it is negative, it has
     *            the same effect as if it were -1: -1 is returned.
     * @return
     *         character sequence represented by this object that is less than
     *         or equal to {@code fromIndex}, or {@code -1} if the
     *         character does not occur before that point.
     */
    public static int lastIndexOf(final String str, final int charValueToFind, int startIndexFromBack) {
        if (str == null || str.isEmpty() || startIndexFromBack < 0) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        return str.lastIndexOf(charValueToFind, startIndexFromBack);
    }

    /**
     * Last index of.
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static int lastIndexOf(final String str, final String valueToFind) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(valueToFind);
    }

    /**
     * Returns the index within {@code str} of the last occurrence of the
     * specified {@code substr}, searching backward starting at the
     * specified index.
     *
     * <p>
     * The returned index is the largest value <i>k</i> for which: <blockquote>
     *
     * <pre>
     * {@code <i>k</i> <= fromIndex && str.startsWith(substr, <i>k</i>)}
     * </pre>
     *
     * </blockquote> If no such value of <i>k</i> exists, then {@code -1} is
     * returned.
     *
     * @param str
     * @param valueToFind
     * @param startIndexFromBack The index to start the search from, searching backward.
     * @return
     */
    public static int lastIndexOf(final String str, final String valueToFind, int startIndexFromBack) {
        if (str == null || valueToFind == null || startIndexFromBack < 0 || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        return str.lastIndexOf(valueToFind, startIndexFromBack);
    }

    /**
     * Returns the index within the input string of the last occurrence of the specified substring,
     * using the specified delimiter to separate the search.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to separate the search.
     * @return The index of the last occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int lastIndexOf(final String str, final String valueToFind, final String delimiter) {
        return lastIndexOf(str, valueToFind, delimiter, str.length());
    }

    /**
     * Returns the index within the input string of the last occurrence of the specified substring,
     * using the specified delimiter to separate the search, starting the search at the specified index.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param delimiter The delimiter to separate the search.
     * @param startIndexFromBack The index to start the search from, searching backward.
     * @return The index of the last occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int lastIndexOf(final String str, final String valueToFind, final String delimiter, int startIndexFromBack) {
        if (isEmpty(delimiter)) {
            return lastIndexOf(str, valueToFind, startIndexFromBack);
        } else if (str == null || valueToFind == null || startIndexFromBack < 0) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        final int len = str.length();
        final int substrLen = valueToFind.length();
        final int delimiterLen = delimiter.length();

        if (len < substrLen || (len > substrLen && len - substrLen < delimiterLen)) {
            return N.INDEX_NOT_FOUND;
        }

        int index = str.lastIndexOf(delimiter + valueToFind, startIndexFromBack);

        if (index >= 0 && index + delimiter.length() + valueToFind.length() == len) {
            return index + delimiter.length();
        }

        index = str.lastIndexOf(delimiter + valueToFind + delimiter, startIndexFromBack);

        if (index >= 0) {
            return index + delimiter.length();
        }

        index = str.lastIndexOf(valueToFind, startIndexFromBack);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        //noinspection DuplicateExpressions
        if (index == 0 && (index + valueToFind.length() == len || (len >= index + valueToFind.length() + delimiter.length()
                && delimiter.equals(str.substring(index + valueToFind.length(), index + valueToFind.length() + delimiter.length()))))) {
            return index;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the last occurrence of the specified substring, ignoring case considerations.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @return The index of the last occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int lastIndexOfIgnoreCase(final String str, final String valueToFind) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return lastIndexOfIgnoreCase(str, valueToFind, str.length());
    }

    /**
     * Returns the index within the input string of the last occurrence of the specified substring, ignoring case considerations,
     * searching backward starting at the specified index.
     * If a substring with value {@code valueToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The substring to be found.
     * @param startIndexFromBack The index to start the search from, searching backward.
     * @return The index of the last occurrence of the substring in the character sequence represented by this object,
     *         or -1 if the substring does not occur.
     */
    public static int lastIndexOfIgnoreCase(final String str, final String valueToFind, int startIndexFromBack) {
        if (str == null || valueToFind == null || startIndexFromBack < 0 || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        for (int i = N.min(startIndexFromBack, str.length() - valueToFind.length()), substrLen = valueToFind.length(); i >= 0; i--) {
            if (str.regionMatches(true, i, valueToFind, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index within the input string of the last occurrence of any specified character.
     * If a character within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     * The returned index may not be the biggest or smallest last index of the character within the array {@code valuesToFind} occurring the specified string,
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToFind The array of characters to be found.
     * @return The index of the last occurrence of any character in the character sequence represented by this object,
     *         or -1 if none of the characters occur.
     * @see #smallestLastIndexOfAll(String, String[])
     * @see #smallestLastIndexOfAll(String, int, String[])
     * @see #largestLastIndexOfAll(String, String[])
     * @see #largestLastIndexOfAll(String, int, String[])
     */
    public static int lastIndexOfAny(final String str, final char... valuesToFind) {
        checkInputChars(valuesToFind, cs.valuesToFind, true);

        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        int idx = 0;

        for (final char ch : valuesToFind) {
            idx = str.lastIndexOf(ch);

            if (idx != N.INDEX_NOT_FOUND) {
                return idx;
            }
        }

        final int strLen = str.length();
        final int chsLen = valuesToFind.length;
        char ch = 0;

        for (int i = strLen - 1; i >= 0; i--) {
            ch = str.charAt(i);

            for (int j = chsLen - 1; j >= 0; j--) {
                if (valuesToFind[j] == ch) {
                    // checked by checkInputChars

                    //    if (i > 0 && j > 0 && Character.isHighSurrogate(ch = str.charAt(i - 1))) {
                    //        // ch is a supplementary character
                    //        if (valuesToFind[j - 1] == ch) {
                    //            return i - 1;
                    //        }
                    //    } else {
                    //        return i;
                    //    }

                    return i;
                }
            }
        }

        return N.INDEX_NOT_FOUND;

    }

    /**
     * Returns the index within the input string of the last occurrence of any specified substring.
     * If a substring within the array {@code valuesToFind} occurs in the character sequence represented by the input {@code String} object,
     * then the index of the last such occurrence is returned.
     * The returned index may not be the biggest last index of the substring within the array {@code valuesToFind} occurring the specified string,
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToFind The array of substrings to be found.
     * @return The index of the last occurrence of any substring in the character sequence represented by this object,
     *         or -1 if none of the substrings occur.
     * @see #smallestLastIndexOfAll(String, String[])
     * @see #smallestLastIndexOfAll(String, int, String[])
     * @see #largestLastIndexOfAll(String, String[])
     * @see #largestLastIndexOfAll(String, int, String[])
     */
    public static int lastIndexOfAny(final String str, final String... valuesToFind) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        int idx = 0;

        for (final String substr : valuesToFind) {
            if (substr == null || substr.length() > len) {
                continue;
            }

            idx = str.lastIndexOf(substr);

            if (idx != N.INDEX_NOT_FOUND) {
                return idx;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    // Shared code between ordinalIndexOf(String,String,int) and

    /**
     * Ordinal index of.
     *
     * @param str
     * @param substr
     * @param ordinal
     * @param isLastIndex
     * @return
     */
    // lastOrdinalIndexOf(String,String,int)
    private static int ordinalIndexOf(final String str, final String substr, final int ordinal, final boolean isLastIndex) {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal(" + ordinal + ") must be >= 1");
        }

        if (str == null || substr == null || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        int fromIndex = isLastIndex ? str.length() : 0;

        for (int found = 0; fromIndex >= 0;) {
            fromIndex = isLastIndex ? str.lastIndexOf(substr, fromIndex) : str.indexOf(substr, fromIndex);

            if (fromIndex < 0) {
                return N.INDEX_NOT_FOUND;
            }

            if (++found >= ordinal) {
                break;
            }

            fromIndex = isLastIndex ? (fromIndex - substr.length()) : (fromIndex + substr.length());
        }

        return fromIndex;
    }

    /**
     * Finds the smallest index of all specified substrings within the input string.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the beginning of the string.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valuesToFind The array of substrings to be found.
     * @return The smallest index of all substrings within the input string or -1 if none of the substrings occur.
     * @see #indexOfAny(String, String[])
     * @see #indexOfAny(String, int, String[])
     */
    public static int smallestIndexOfAll(final String str, final String... valuesToFind) {
        return smallestIndexOfAll(str, 0, valuesToFind);
    }

    /**
     * Finds the smallest index of all substrings in {@code valuesToFind} within the input string from {@code fromIndex}.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the specified {@code fromIndex}.
     *
     * @param str
     * @param fromIndex
     * @param valuesToFind
     * @return The smallest index of all substrings within the input string from {@code fromIndex} or -1 if none of the substrings occur.
     * @see #indexOfAny(String, String[])
     * @see #indexOfAny(String, int, String[])
     */
    public static int smallestIndexOfAll(final String str, int fromIndex, final String... valuesToFind) {
        if (str == null || fromIndex > str.length() || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        fromIndex = Math.max(0, fromIndex);

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (final String substr : valuesToFind) {
            if (substr == null || (substr.length() > len - fromIndex)) {
                continue;
            }

            final int tmp = indexOf(str, substr, fromIndex);

            result = tmp >= 0 && (result == N.INDEX_NOT_FOUND || tmp < result) ? tmp : result;

            if (result == fromIndex) {
                break;
            }
        }

        return result;
    }

    /**
     * Finds the largest index of all substrings in {@code valuesToFind} within the input string.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the beginning of the string.
     *
     * @param str
     * @param valuesToFind
     * @return The largest index of all substrings within the input string or -1 if none of the substrings occur.
     * @see #indexOfAny(String, String[])
     * @see #indexOfAny(String, int, String[])
     */
    public static int largestIndexOfAll(final String str, final String... valuesToFind) {
        return largestIndexOfAll(str, 0, valuesToFind);
    }

    /**
     * Finds the largest index of all substrings in {@code valuesToFind} within the input string from {@code fromIndex}.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the specified {@code fromIndex}.
     *
     * @param str
     * @param fromIndex
     * @param valuesToFind
     * @return The largest index of all substrings within the input string from {@code fromIndex} or -1 if none of the substrings occur.
     * @see #indexOfAny(String, String[])
     * @see #indexOfAny(String, int, String[])
     */
    public static int largestIndexOfAll(final String str, final int fromIndex, final String... valuesToFind) {
        if (str == null || fromIndex > str.length() || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int fromIndexToUse = Math.max(0, fromIndex);

        final int len = str.length();
        final List<String> subStringsSortedByLen = Stream.of(valuesToFind) //
                .filter(it -> !(it == null || (fromIndexToUse + it.length() > len)))
                .sortedByInt(N::len)
                .toList();

        int result = N.INDEX_NOT_FOUND;

        for (final String substr : subStringsSortedByLen) {
            if (result >= 0 && substr.length() >= len - result) {
                continue;
            }

            result = N.max(result, indexOf(str, substr, fromIndex));

            if (result == len - substr.length()) {
                break;
            }
        }

        return result;
    }

    /**
     * Finds the smallest last index of all substrings in {@code valuesToFind} within the input string.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the back of the string.
     *
     * @param str The string to search within. It may be {@code null}.
     * @param valuesToFind The substrings to find within the string. These may be empty or {@code null}.
     * @return The smallest last index of all substrings within the input string or -1 if none of the substrings occur.
     * @see #lastIndexOfAny(String, String[])
     * @see #lastIndexOfAny(String, char[])
     */
    public static int smallestLastIndexOfAll(final String str, final String... valuesToFind) {
        return smallestLastIndexOfAll(str, N.len(str), valuesToFind);
    }

    /**
     * Finds the smallest last index of all substrings in {@code valuesToFind} within the input string from {@code startIndexFromBack}.
     * Each substring in the array {@code valuesToFind} is only searched at most once from {@code startIndexFromBack} from the back of the string.
     *
     * @param str
     * @param startIndexFromBack
     * @param valuesToFind
     * @return The smallest index of all substrings within the input string from {@code startIndexFromBack} or -1 if none of the substrings occur.
     * @see #lastIndexOfAny(String, String[])
     * @see #lastIndexOfAny(String, char[])
     */
    public static int smallestLastIndexOfAll(final String str, int startIndexFromBack, final String... valuesToFind) {
        if (str == null || startIndexFromBack < 0 || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (final String substr : valuesToFind) {
            if (substr == null || substr.length() > len) {
                continue;
            }

            final int tmp = lastIndexOf(str, substr, startIndexFromBack);

            result = tmp >= 0 && (result == N.INDEX_NOT_FOUND || tmp < result) ? tmp : result;

            if (result == 0) {
                break;
            }
        }

        return result;
    }

    /**
     * Finds the largest last index of all substrings in {@code valuesToFind} within the input string.
     * Each substring in the array {@code valuesToFind} is only searched at most once from the back of the string.
     *
     * @param str
     * @param valuesToFind
     * @return The largest last index of all substrings within the input string or -1 if none of the substrings occur.
     * @see #lastIndexOfAny(String, String[])
     * @see #lastIndexOfAny(String, char[])
     */
    public static int largestLastIndexOfAll(final String str, final String... valuesToFind) {
        return largestLastIndexOfAll(str, N.len(str), valuesToFind);
    }

    /**
     * Finds the largest last index of all substrings in {@code valuesToFind} within the input string from {@code startIndexFromBack}.
     * Each substring in the array {@code valuesToFind} is only searched at most once from {@code startIndexFromBack} from the back of the string.
     *
     * @param str
     * @param startIndexFromBack
     * @param valuesToFind
     * @return The largest index of all substrings within the input string from {@code startIndexFromBack} or -1 if none of the substrings occur.
     * @see #lastIndexOfAny(String, String[])
     * @see #lastIndexOfAny(String, char[])
     */
    public static int largestLastIndexOfAll(final String str, int startIndexFromBack, final String... valuesToFind) {
        if (str == null || startIndexFromBack < 0 || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (final String substr : valuesToFind) {
            if (substr == null || substr.length() > len) {
                continue;
            }

            result = N.max(result, str.lastIndexOf(substr, startIndexFromBack));

            if (result == startIndexFromBack) {
                break;
            }
        }

        return result;
    }

    /**
     * Finds the n-th occurrence of the specified value within the input string.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The value to be found.
     * @param ordinal The n-th occurrence to find.
     * @return The index of the n-th occurrence of the specified value within the input string,
     *         or -1 if the value does not occur as many times as requested.
     */
    public static int ordinalIndexOf(final String str, final String valueToFind, final int ordinal) {
        return ordinalIndexOf(str, valueToFind, ordinal, false);
    }

    /**
     * Finds the n-th last occurrence of the specified value within the input string.
     *
     * @param str The string to be checked. May be {@code null} or empty.
     * @param valueToFind The value to be found.
     * @param ordinal The n-th last occurrence to find.
     * @return The index of the n-th last occurrence of the specified value within the input string,
     *         or -1 if the value does not occur as many times as requested.
     */
    public static int lastOrdinalIndexOf(final String str, final String valueToFind, final int ordinal) {
        return ordinalIndexOf(str, valueToFind, ordinal, true);
    }

    /**
     * Returns a stream of indices of all occurrences of the specified substring.
     *
     * <pre>
     * <code>
     * Strings.indicesOf("abca", "a").join(", ") ==> "0, 3"
     * Strings.indicesOf("abcA", "a").join(", ") ==> "0"
     * Strings.indicesOf("abcA", null).join(", ") ==> ""
     * Strings.indicesOf(null, "").join(", ") ==> ""
     * Strings.indicesOf("", null).join(", ") ==> ""
     * Strings.indicesOf("abcA", "").join(", ") ==> "0, 1, 2, 3"
     * Strings.indicesOfIgnoreCase("abcA", "a").join(", ") ==> "0, 3"
     * </code>
     * </pre>
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found, may be {@code null}
     * @return a stream of indices of all occurrences of the specified substring, or an empty stream if the substring is not found
     * @see RegExUtil#matchIndices(String, String)
     * @see RegExUtil#matchIndices(String, Pattern)
     * @see IntStream#ofIndices(Object, int, int, com.landawn.abacus.util.function.ObjIntFunction)
     */
    public static IntStream indicesOf(final String str, final String valueToFind) {
        return indicesOf(str, valueToFind, 0);
    }

    /**
     * Returns a stream of indices of all occurrences of the specified substring, starting the search at the specified index.
     *
     * <pre>
     * <code>
     * Strings.indicesOf("abca", "a").join(", ") ==> "0, 3"
     * Strings.indicesOf("abcA", "a").join(", ") ==> "0"
     * Strings.indicesOf("abcA", null).join(", ") ==> ""
     * Strings.indicesOf(null, "").join(", ") ==> ""
     * Strings.indicesOf("", null).join(", ") ==> ""
     * Strings.indicesOf("abcA", "").join(", ") ==> "0, 1, 2, 3"
     * Strings.indicesOfIgnoreCase("abcA", "a").join(", ") ==> "0, 3"
     * </code>
     * </pre>
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found, may be {@code null}
     * @param fromIndex the index to start the search from
     * @return a stream of indices of all occurrences of the specified substring, or an empty stream if the substring is not found
     * @see RegExUtil#matchIndices(String, String)
     * @see RegExUtil#matchIndices(String, Pattern)
     * @see IntStream#ofIndices(Object, int, int, com.landawn.abacus.util.function.ObjIntFunction)
     */
    public static IntStream indicesOf(final String str, final String valueToFind, final int fromIndex) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return IntStream.empty();
        }

        final int increment = Math.max(valueToFind.length(), 1);

        return IntStream.ofIndices(str, fromIndex, increment, (s, from) -> s.indexOf(valueToFind, from));
    }

    /**
     * Returns a stream of indices of all occurrences of the specified substring, ignoring case considerations.
     *
     * <pre>
     * <code>
     * Strings.indicesOf("abca", "a").join(", ") ==> "0, 3"
     * Strings.indicesOf("abcA", "a").join(", ") ==> "0"
     * Strings.indicesOf("abcA", null).join(", ") ==> ""
     * Strings.indicesOf(null, "").join(", ") ==> ""
     * Strings.indicesOf("", null).join(", ") ==> ""
     * Strings.indicesOf("abcA", "").join(", ") ==> "0, 1, 2, 3"
     * Strings.indicesOfIgnoreCase("abcA", "a").join(", ") ==> "0, 3"
     * </code>
     * </pre>
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found, may be {@code null}
     * @return a stream of indices of all occurrences of the specified substring, or an empty stream if the substring is not found
     * @see RegExUtil#matchIndices(String, String)
     * @see RegExUtil#matchIndices(String, Pattern)
     * @see IntStream#ofIndices(Object, int, int, com.landawn.abacus.util.function.ObjIntFunction)
     */
    public static IntStream indicesOfIgnoreCase(final String str, final String valueToFind) {
        return indicesOfIgnoreCase(str, valueToFind, 0);
    }

    /**
     * Returns a stream of indices of all occurrences of the specified substring, starting the search at the specified index, ignoring case considerations.
     *
     * <pre>
     * <code>
     * Strings.indicesOf("abca", "a").join(", ") ==> "0, 3"
     * Strings.indicesOf("abcA", "a").join(", ") ==> "0"
     * Strings.indicesOf("abcA", null).join(", ") ==> ""
     * Strings.indicesOf(null, "").join(", ") ==> ""
     * Strings.indicesOf("", null).join(", ") ==> ""
     * Strings.indicesOf("abcA", "").join(", ") ==> "0, 1, 2, 3"
     * Strings.indicesOfIgnoreCase("abcA", "a").join(", ") ==> "0, 3"
     * </code>
     * </pre>
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found, may be {@code null}
     * @param fromIndex the index to start the search from
     * @return a stream of indices of all occurrences of the specified substring, or an empty stream if the substring is not found
     * @see RegExUtil#matchIndices(String, String)
     * @see RegExUtil#matchIndices(String, Pattern)
     * @see IntStream#ofIndices(Object, int, int, com.landawn.abacus.util.function.ObjIntFunction)
     */
    public static IntStream indicesOfIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return IntStream.empty();
        }

        final int strLen = str.length();
        final int increment = Math.max(valueToFind.length(), 1);
        final int end = strLen - increment + 1;

        return IntStream.ofIndices(str, fromIndex, increment, (s, from) -> {
            for (int idx = from; idx < end; idx++) {
                if (str.regionMatches(true, idx, valueToFind, 0, increment)) {
                    return idx;
                }
            }

            return N.INDEX_NOT_FOUND;
        });
    }

    /**
     * Counts the number of occurrences of the specified character in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param charValueToFind the character to be counted
     * @return the number of occurrences of the specified character in the string, or 0 if the string is {@code null} or empty
     * @see N#occurrencesOf(String, char)
     */
    @SuppressWarnings("deprecation")
    public static int countMatches(final String str, final char charValueToFind) {
        if (isEmpty(str)) {
            return 0;
        }

        int occurrences = 0;

        for (final char e : InternalUtil.getCharsForReadOnly(str)) {
            if (e == charValueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Counts the number of occurrences of the specified substring in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be counted
     * @return the number of occurrences of the specified substring in the string, or 0 if the string is {@code null} or empty
     * @see N#occurrencesOf(String, String)
     */
    public static int countMatches(final String str, final String valueToFind) {
        if (isEmpty(str) || isEmpty(valueToFind)) {
            return 0;
        }

        int occurrences = 0;

        for (int len = N.len(str), substrLen = N.len(valueToFind), index = 0, fromIndex = 0, toIndex = len - substrLen; fromIndex <= toIndex;) {
            index = str.indexOf(valueToFind, fromIndex);

            if (index < 0) {
                break;
            } else {
                fromIndex = index + substrLen;
                occurrences++;
            }
        }

        return occurrences;
    }

    //    /**
    //     * Counts the number of occurrences of the specified pattern in the given string.
    //     *
    //     * @param str the string to be checked, may be {@code null} or empty
    //     * @param regexToFind the regular expression pattern to be counted
    //     * @return the number of occurrences of the specified pattern in the string, or 0 if the string is {@code null} or empty
    //     * @deprecated replaced by {@link RegExUtil#countMatches(String, String)}
    //     * @see RegExUtil#countMatches(String, String)
    //     * @see RegExUtil#countMatches(String, Pattern)
    //     */
    //    @Beta
    //    public static int countMatchesByPattern(final String str, final String regexToFind) {
    //        return RegExUtil.countMatches(str, regexToFind);
    //    }

    /**
     * Checks if the specified character is present in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param charValueToFind the character to be found
     * @return {@code true} if the character is found in the string, {@code false} otherwise
     */
    public static boolean contains(final String str, final char charValueToFind) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        return indexOf(str, charValueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if the specified substring is present in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found
     * @return {@code true} if the substring is found in the string, {@code false} otherwise
     */
    public static boolean contains(final String str, final String valueToFind) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOf(str, valueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if the specified substring is present in the given string, considering the specified delimiter.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found
     * @param delimiter the delimiter to be considered
     * @return {@code true} if the substring is found in the string considering the delimiter, {@code false} otherwise
     */
    public static boolean contains(final String str, final String valueToFind, final String delimiter) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOf(str, valueToFind, delimiter) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if the specified substring is present in the given string, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found
     * @return {@code true} if the substring is found in the string, ignoring case considerations, {@code false} otherwise
     */
    public static boolean containsIgnoreCase(final String str, final String valueToFind) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOfIgnoreCase(str, valueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if the specified substring is present in the given string, considering the specified delimiter and ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valueToFind the substring to be found
     * @param delimiter the delimiter to be considered
     * @return {@code true} if the substring is found in the string considering the delimiter and ignoring case considerations, {@code false} otherwise
     */
    public static boolean containsIgnoreCase(final String str, final String valueToFind, final String delimiter) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOfIgnoreCase(str, valueToFind, delimiter) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if all the specified characters are present in the given string.
     * The method returns {@code true} if the specified character array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of characters to be found
     * @return {@code true} if all the characters are found in the given string or the specified {@code valuesToFind} char array is {@code null} or empty, {@code false} otherwise.
     */
    public static boolean containsAll(final String str, final char... valuesToFind) {
        checkInputChars(valuesToFind, cs.valuesToFind, true);

        if (N.isEmpty(valuesToFind)) {
            return true;
        }

        if (str == null || str.isEmpty()) {
            return false;
        }

        for (final char ch : valuesToFind) {
            if (str.indexOf(ch) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all the specified substrings are present in the given string.
     * The method returns {@code true} if the specified substring array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be found
     * @return {@code true} if all the substrings are found in the given string or the specified substring array is {@code null} or empty, {@code false} otherwise
     */
    public static boolean containsAll(final String str, final String... valuesToFind) {
        if (N.isEmpty(valuesToFind)) {
            return true;
        }

        if (str == null || str.isEmpty()) {
            return false;
        }

        for (final String searchStr : valuesToFind) {
            if (!Strings.contains(str, searchStr)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all the specified substrings are present in the given string, ignoring case considerations.
     * The method returns {@code true} if the specified substring array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be found
     * @return {@code true} if all the substrings are found in the given string or the specified substring array is {@code null} or empty, ignoring case considerations, {@code false} otherwise
     */
    public static boolean containsAllIgnoreCase(final String str, final String... valuesToFind) {
        if (N.isEmpty(valuesToFind)) {
            return true;
        }

        if (str == null || str.isEmpty()) {
            return false;
        }

        for (final String searchStr : valuesToFind) {
            if (!Strings.containsIgnoreCase(str, searchStr)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if any of the specified characters are present in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of characters to be found
     * @return {@code true} if any of the characters are found in the given string, {@code false} otherwise if not or if the specified {@code valuesToFind} char array is {@code null} or empty
     * @see #containsNone(String, char[])
     */
    public static boolean containsAny(final String str, final char... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAny(str, valuesToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if any of the specified substrings are present in the given string.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be found
     * @return {@code true} if any of the substrings are found in the given string, {@code false} otherwise if not or if the specified substrings array is {@code null} or empty
     * @see #containsNone(String, String[])
     */
    public static boolean containsAny(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAny(str, valuesToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if any of the specified substrings are present in the given string, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be found
     * @return {@code true} if any of the substrings are found in the given string, ignoring case considerations, {@code false} otherwise if not or if the specified substrings array is {@code null} or empty
     * @see #containsNoneIgnoreCase(String, String[])
     */
    public static boolean containsAnyIgnoreCase(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        if (valuesToFind.length == 1) {
            return containsIgnoreCase(str, valuesToFind[0]);
        } else if (valuesToFind.length == 2) {
            if (containsIgnoreCase(str, valuesToFind[0])) {
                return true;
            }

            return containsIgnoreCase(str, valuesToFind[1]);
        }

        final String sourceText = str.toLowerCase();

        for (final String searchStr : valuesToFind) {
            if (isNotEmpty(searchStr) && indexOf(sourceText, searchStr.toLowerCase()) != N.INDEX_NOT_FOUND) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if none of the specified characters are present in the given string.
     * The method returns {@code true} if the string is {@code null} or empty, or if the specified character array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of characters to be checked
     * @return {@code true} if none of the characters are found in the given string or if the given string is {@code null} or empty, or if the specified {@code valuesToFind} char array is {@code null} or empty, {@code false} otherwise
     * @see #containsAny(String, char[])
     */
    public static boolean containsNone(final String str, final char... valuesToFind) {
        checkInputChars(valuesToFind, cs.valuesToFind, true);

        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return true;
        }

        final int strLen = str.length();
        char ch = 0;

        for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);

            for (final char c : valuesToFind) {
                if (c == ch) {
                    // checked by checkInputChars

                    //    if (Character.isHighSurrogate(ch)) {
                    //        if ((j == chsLast) || (i < strLast && valuesToFind[j + 1] == str.charAt(i + 1))) {
                    //            return false;
                    //        }
                    //    } else {
                    //        // ch is in the Basic Multilingual Plane
                    //        return false;
                    //    }

                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if none of the specified substrings are present in the given string.
     * The method returns {@code true} if the string is {@code null} or empty, or if the specified substring array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be checked
     * @return {@code true} if none of the substrings are found in the given string or if the given string is {@code null} or empty, or if the specified {@code valuesToFind} char array is {@code null} or empty, {@code false} otherwise
     * @see #containsAny(String, String[])
     */
    public static boolean containsNone(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return true;
        }

        return !containsAny(str, valuesToFind);
    }

    /**
     * Checks if none of the specified substrings are present in the given string, ignoring case considerations.
     * The method returns {@code true} if the string is {@code null} or empty, or if the specified substring array is empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the array of substrings to be checked
     * @return {@code true} if none of the substrings are found in the given string or if the given string is {@code null} or empty, or if the specified {@code valuesToFind} char array is {@code null} or empty, ignoring case considerations, {@code false} otherwise
     * @see #containsAnyIgnoreCase(String, String[])
     */
    public static boolean containsNoneIgnoreCase(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return true;
        }

        return !containsAnyIgnoreCase(str, valuesToFind);
    }

    /**
     * Checks if the given string contains only the specified character.
     * The method returns {@code true} if the string is {@code null} or empty.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param valuesToFind the character to be checked
     * @return {@code true} if the given string contains only the specified character or the given string is {@code null} or empty, {@code false} otherwise
     */
    public static boolean containsOnly(final String str, final char... valuesToFind) {
        if (isEmpty(str)) {
            return true;
        } else if (N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAnyBut(str, valuesToFind) == N.INDEX_NOT_FOUND;
    }

    /**
     * Checks if the given string contains any whitespace characters.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @return {@code true} if the string contains any whitespace characters, {@code false} otherwise
     */
    // From org.springframework.util.StringUtils, under Apache License 2.0
    public static boolean containsWhitespace(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0, len = str.length(); i < len; i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string starts with the specified prefix.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param prefix the prefix to be checked
     * @return {@code true} if the string starts with the specified prefix, {@code false} otherwise
     */
    public static boolean startsWith(final String str, final String prefix) {
        return startsWith(str, prefix, false);
    }

    /**
     * Checks if the given string starts with the specified prefix, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param prefix the prefix to be checked
     * @return {@code true} if the string starts with the specified prefix, ignoring case considerations, {@code false} otherwise
     */
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        return startsWith(str, prefix, true);
    }

    /**
     * Checks if the given string starts with the specified prefix, with an option to ignore case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param prefix the prefix to be checked
     * @param ignoreCase if {@code true}, the comparison is case-insensitive
     * @return {@code true} if the string starts with the specified prefix, considering the case sensitivity option, {@code false} otherwise
     */
    private static boolean startsWith(final String str, final String prefix, final boolean ignoreCase) {
        if (str == null || prefix == null || prefix.length() > str.length()) {
            return false;
        }

        return ignoreCase ? str.regionMatches(true, 0, prefix, 0, prefix.length()) : str.startsWith(prefix);
    }

    /**
     * Checks if the given string starts with any of the specified substrings.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param substrs the array of substrings to be checked
     * @return {@code true} if the string starts with any of the specified substrings, {@code false} otherwise
     */
    public static boolean startsWithAny(final String str, final String... substrs) {
        if (str == null || N.isEmpty(substrs)) {
            return false;
        }

        for (final String substr : substrs) {
            if (startsWith(str, substr)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string starts with any of the specified substrings, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param substrs the array of substrings to be checked
     * @return {@code true} if the string starts with any of the specified substrings, ignoring case considerations, {@code false} otherwise
     */
    public static boolean startsWithAnyIgnoreCase(final String str, final String... substrs) {
        if (str == null || N.isEmpty(substrs)) {
            return false;
        }

        for (final String substr : substrs) {
            if (startsWithIgnoreCase(str, substr)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string ends with the specified suffix.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param suffix the suffix to be checked
     * @return {@code true} if the string ends with the specified suffix, {@code false} otherwise
     */
    public static boolean endsWith(final String str, final String suffix) {
        return endsWith(str, suffix, false);
    }

    /**
     * Checks if the given string ends with the specified suffix, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param suffix the suffix to be checked
     * @return {@code true} if the string ends with the specified suffix, ignoring case considerations, {@code false} otherwise
     */
    public static boolean endsWithIgnoreCase(final String str, final String suffix) {
        return endsWith(str, suffix, true);
    }

    /**
     * Checks if the given string ends with any of the specified substrings.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param substrs the array of substrings to be checked
     * @return {@code true} if the string ends with any of the specified substrings, {@code false} otherwise
     */
    public static boolean endsWithAny(final String str, final String... substrs) {
        if (str == null || N.isEmpty(substrs)) {
            return false;
        }

        for (final String searchString : substrs) {
            if (endsWith(str, searchString)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string ends with any of the specified substrings, ignoring case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param substrs the array of substrings to be checked
     * @return {@code true} if the string ends with any of the specified substrings, ignoring case considerations, {@code false} otherwise
     */
    public static boolean endsWithAnyIgnoreCase(final String str, final String... substrs) {
        if (str == null || N.isEmpty(substrs)) {
            return false;
        }

        for (final String searchString : substrs) {
            if (endsWithIgnoreCase(str, searchString)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string ends with the specified suffix, with an option to ignore case considerations.
     *
     * @param str the string to be checked, may be {@code null} or empty
     * @param suffix the suffix to be checked
     * @param ignoreCase if {@code true}, the comparison is case-insensitive
     * @return {@code true} if the string ends with the specified suffix, considering the case sensitivity option, {@code false} otherwise
     */
    private static boolean endsWith(final String str, final String suffix, final boolean ignoreCase) {
        if (str == null || suffix == null || suffix.length() > str.length()) {
            return false;
        }

        final int strOffset = str.length() - suffix.length();

        return ignoreCase ? str.regionMatches(true, strOffset, suffix, 0, suffix.length()) : str.endsWith(suffix);
    }

    /**
     * Compares two strings for equality.
     *
     * @param a the first string to compare, which may be null
     * @param b the second string to compare, which may be null
     * @return {@code true} if the strings are equal, {@code false} otherwise
     */
    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b != null && a.length() == b.length() && a.equals(b));
    }

    /**
     * Compares two strings for equality, ignoring case considerations.
     *
     * @param a the first string to compare, which may be null
     * @param b the second string to compare, which may be null
     * @return {@code true} if the strings are equal, ignoring case considerations, {@code false} otherwise
     */
    public static boolean equalsIgnoreCase(final String a, final String b) {
        return (a == null) ? b == null : (a.equalsIgnoreCase(b));
    }

    /**
     * Checks if the given string is equal to any of the specified search strings.
     *
     * @param str the string to be checked, which may be null
     * @param searchStrings the array of strings to compare against, may be {@code null} or empty
     * @return {@code true} if the string is equal to any of the specified search strings, {@code false} otherwise
     */
    public static boolean equalsAny(final String str, final String... searchStrings) {
        if (N.isEmpty(searchStrings)) {
            return false;
        }

        for (final String searchString : searchStrings) {
            if (equals(str, searchString)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given string is equal to any of the specified search strings, ignoring case considerations.
     *
     * @param str the string to be checked, which may be null
     * @param searchStrs the array of strings to compare against, may be {@code null} or empty
     * @return {@code true} if the string is equal to any of the specified search strings, ignoring case considerations, {@code false} otherwise
     */
    public static boolean equalsAnyIgnoreCase(final String str, final String... searchStrs) {
        if (N.isEmpty(searchStrs)) {
            return false;
        } else if (searchStrs.length == 1) {
            return equalsIgnoreCase(str, searchStrs[0]);
        } else if (searchStrs.length == 2) {
            if (equalsIgnoreCase(str, searchStrs[0])) {
                return true;
            }

            return equalsIgnoreCase(str, searchStrs[1]);
        }

        final String sourceText = str.toLowerCase();

        for (final String searchStr : searchStrs) {
            if (equals(sourceText, searchStr.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compares two strings lexicographically, ignoring case considerations. Null is considered less than any string.
     *
     * @param a the first string to compare, which may be null
     * @param b the second string to compare, which may be null
     * @return a negative integer, zero, or a positive integer as the first string is less than, equal to, or greater than the second string, ignoring case considerations
     */
    public static int compareIgnoreCase(final String a, final String b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));
    }

    /**
     * <p>
     * Compares two Strings, and returns the index at which the Strings begin
     * to differ.
     * </p>
     *
     * <p>
     * For example,
     * {@code indexOfDifference("i am a machine", "i am a robot") -> 7}
     * </p>
     *
     * <pre>
     * Strings.indexOfDifference(null, null) = -1
     * Strings.indexOfDifference("", "") = -1
     * Strings.indexOfDifference("", "abc") = 0
     * Strings.indexOfDifference("abc", "") = 0
     * Strings.indexOfDifference("abc", "abc") = -1
     * Strings.indexOfDifference("ab", "abxyz") = 2
     * Strings.indexOfDifference("abcde", "abxyz") = 2
     * Strings.indexOfDifference("abcde", "xyz") = 0
     * </pre>
     *
     * @param a
     *            the first String, which may be null
     * @param b
     *            the second String, which may be null
     * @return
     */
    public static int indexOfDifference(final String a, final String b) {
        if (N.equals(a, b) || (isEmpty(a) && isEmpty(b))) {
            return N.INDEX_NOT_FOUND;
        }

        if (isEmpty(a) || isEmpty(b)) {
            return 0;
        }

        int i = 0;
        for (final int len = N.min(a.length(), b.length()); i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                break;
            }
        }

        if (i < b.length() || i < a.length()) {
            return i;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * <p>
     * Compares all Strings in an array and returns the index at which the
     * Strings begin to differ.
     * </p>
     *
     * <p>
     * For example,
     * <code>indexOfDifference(new String[] {"i am a machine", "i am a robot"}) -&gt; 7</code>
     * </p>
     *
     * <pre>
     * Strings.indexOfDifference(null) = -1
     * Strings.indexOfDifference(new String[] {}) = -1
     * Strings.indexOfDifference(new String[] {"abc"}) = -1
     * Strings.indexOfDifference(new String[] {null, null}) = -1
     * Strings.indexOfDifference(new String[] {"", ""}) = -1
     * Strings.indexOfDifference(new String[] {"", null}) = -1
     * Strings.indexOfDifference(new String[] {"abc", {@code null}, null}) = 0
     * Strings.indexOfDifference(new String[] {null, {@code null}, "abc"}) = 0
     * Strings.indexOfDifference(new String[] {"", "abc"}) = 0
     * Strings.indexOfDifference(new String[] {"abc", ""}) = 0
     * Strings.indexOfDifference(new String[] {"abc", "abc"}) = -1
     * Strings.indexOfDifference(new String[] {"abc", "a"}) = 1
     * Strings.indexOfDifference(new String[] {"ab", "abxyz"}) = 2
     * Strings.indexOfDifference(new String[] {"abcde", "abxyz"}) = 2
     * Strings.indexOfDifference(new String[] {"abcde", "xyz"}) = 0
     * Strings.indexOfDifference(new String[] {"xyz", "abcde"}) = 0
     * Strings.indexOfDifference(new String[] {"i am a machine", "i am a robot"}) = 7
     * </pre>
     *
     * @param strs
     *            array of Strings, entries may be null
     * @return
     *         equal or null/empty
     */
    public static int indexOfDifference(final String... strs) {
        if (N.isEmpty(strs) || strs.length == 1) {
            return N.INDEX_NOT_FOUND;
        }

        final int arrayLen = strs.length;
        int shortestStrLen = Integer.MAX_VALUE;
        int longestStrLen = 0;

        // find the min and max string lengths; this avoids checking to make
        // sure we are not exceeding the length of the string each time through
        // the bottom loop.
        for (final String str : strs) {
            if (str == null) {
                shortestStrLen = 0;
            } else {
                shortestStrLen = Math.min(str.length(), shortestStrLen);
                longestStrLen = Math.max(str.length(), longestStrLen);
            }
        }

        // handle lists containing all nulls or all empty strings
        if (longestStrLen == 0) {
            return N.INDEX_NOT_FOUND;
        }

        if (shortestStrLen == 0) {
            return 0;
        }

        // find the position with the first difference across all strings
        int firstDiff = -1;
        char comparisonChar = 0;

        for (int stringPos = 0; stringPos < shortestStrLen; stringPos++) {
            comparisonChar = strs[0].charAt(stringPos);

            for (int arrayPos = 1; arrayPos < arrayLen; arrayPos++) {
                if (strs[arrayPos].charAt(stringPos) != comparisonChar) {
                    firstDiff = stringPos;
                    break;
                }
            }

            if (firstDiff != -1) {
                break;
            }
        }

        if (firstDiff == -1 && shortestStrLen != longestStrLen) {
            // we compared all the characters up to the length of the
            // shortest string and didn't find a match, but the string lengths
            // vary, so return the length of the shortest string.
            return shortestStrLen;
        }

        return firstDiff;
    }

    // --------- from Google Guava

    /**
     * Returns the length of the common prefix between two CharSequences.
     * If either CharSequence is empty, returns 0.
     *
     * @param a the first CharSequence to compare, which may be null
     * @param b the second CharSequence to compare, which may be null
     * @return the length of the common prefix, or 0 if either CharSequence is empty
     */
    public static int lengthOfCommonPrefix(final CharSequence a, final CharSequence b) {
        if (isEmpty(a) || isEmpty(b)) {
            return 0;
        }

        final int maxPrefixLength = Math.min(a.length(), b.length());
        int cnt = 0;

        while (cnt < maxPrefixLength && a.charAt(cnt) == b.charAt(cnt)) {
            cnt++;
        }

        if (validSurrogatePairAt(a, cnt - 1) || validSurrogatePairAt(b, cnt - 1)) {
            cnt--;
        }

        return cnt;
    }

    /**
     * Returns the length of the common suffix between two CharSequences.
     * If either CharSequence is empty, returns 0.
     *
     * @param a the first CharSequence to compare, which may be null
     * @param b the second CharSequence to compare, which may be null
     * @return the length of the common suffix, or 0 if either CharSequence is empty
     */
    public static int lengthOfCommonSuffix(final CharSequence a, final CharSequence b) {
        if (isEmpty(a) || isEmpty(b)) {
            return 0;
        }

        final int aLength = a.length();
        final int bLength = b.length();
        final int maxSuffixLength = Math.min(aLength, bLength);
        int cnt = 0;

        while (cnt < maxSuffixLength && a.charAt(aLength - cnt - 1) == b.charAt(bLength - cnt - 1)) {
            cnt++;
        }

        if (validSurrogatePairAt(a, aLength - cnt - 1) || validSurrogatePairAt(b, bLength - cnt - 1)) {
            cnt--;
        }

        return cnt;
    }

    /**
     * Note: copy from Google Guava under Apache License v2
     *
     * Returns the longest string {@code prefix} such that
     * {@code a.toString().startsWith(prefix) && b.toString().startsWith(prefix)}
     * , taking care not to split surrogate pairs. If {@code a} and {@code b}
     * have no common prefix, returns the empty string.
     *
     * @param a the first CharSequence to compare
     * @param b the second CharSequence to compare
     * @return the longest common prefix, or an empty string if there is no common prefix
     */
    public static String commonPrefix(final CharSequence a, final CharSequence b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY;
        }

        final int commonPrefixLen = lengthOfCommonPrefix(a, b);

        if (commonPrefixLen == a.length()) {
            return a.toString();
        } else if (commonPrefixLen == b.length()) {
            return b.toString();
        } else {
            return a.subSequence(0, commonPrefixLen).toString();
        }
    }

    /**
     * Returns the longest common prefix among an array of CharSequences.
     * If the array is empty, the method will return an empty string.
     * If any CharSequence is empty or {@code null}, the method will return an empty string.
     *
     * @param strs The array of CharSequences to compare.
     * @return The longest common prefix among the given CharSequences. Returns an empty string if the array is empty or any CharSequence is empty or {@code null}.
     */
    public static String commonPrefix(final CharSequence... strs) {
        if (N.isEmpty(strs)) {
            return EMPTY;
        }

        if (strs.length == 1) {
            return isEmpty(strs[0]) ? EMPTY : strs[0].toString();
        } else if (isAnyEmpty(strs)) {
            return EMPTY;
        }

        String commonPrefix = commonPrefix(strs[0], strs[1]);

        if (isEmpty(commonPrefix)) {
            return EMPTY;
        }

        for (int i = 2, len = strs.length; i < len; i++) {
            commonPrefix = commonPrefix(commonPrefix, strs[i]);

            if (isEmpty(commonPrefix)) {
                return commonPrefix;
            }
        }

        return commonPrefix;
    }

    /**
     * Note: copy from Google Guava under Apache License v2
     *
     * Returns the longest string {@code suffix} such that
     * {@code a.toString().endsWith(suffix) && b.toString().endsWith(suffix)},
     * taking care not to split surrogate pairs. If {@code a} and {@code b} have
     * no common suffix, returns the empty string.
     *
     * @param a the first CharSequence to compare
     * @param b the second CharSequence to compare
     * @return the longest common suffix, or an empty string if there is no common suffix
     */
    public static String commonSuffix(final CharSequence a, final CharSequence b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY;
        }

        final int aLength = a.length();
        final int commonSuffixLen = lengthOfCommonSuffix(a, b);

        if (commonSuffixLen == aLength) {
            return a.toString();
        } else if (commonSuffixLen == b.length()) {
            return b.toString();
        } else {
            return a.subSequence(aLength - commonSuffixLen, aLength).toString();
        }
    }

    /**
     * Returns the longest common suffix between the given CharSequences.
     * If any CharSequence is empty or {@code null}, the method will return an empty string.
     *
     * @param strs The CharSequences to compare.
     * @return The longest common suffix between the given CharSequences. Returns an empty string if any CharSequence is empty or {@code null}.
     */
    public static String commonSuffix(final CharSequence... strs) {
        if (N.isEmpty(strs)) {
            return EMPTY;
        }

        if (strs.length == 1) {
            return isEmpty(strs[0]) ? EMPTY : strs[0].toString();
        } else if (isAnyEmpty(strs)) {
            return EMPTY;
        }

        String commonSuffix = commonSuffix(strs[0], strs[1]);

        if (isEmpty(commonSuffix)) {
            return EMPTY;
        }

        for (int i = 2, len = strs.length; i < len; i++) {
            commonSuffix = commonSuffix(commonSuffix, strs[i]);

            if (isEmpty(commonSuffix)) {
                return commonSuffix;
            }
        }

        return commonSuffix;
    }

    // --------- from Google Guava

    /**
     * Note: copy from Google Guava under Apache License v2
     *
     * True when a valid surrogate pair starts at the given {@code index} in the
     * given {@code string}. Out-of-range indexes return {@code false}.
     *
     * @param str
     * @param index
     * @return
     */
    static boolean validSurrogatePairAt(final CharSequence str, final int index) {
        return index >= 0 && index <= (str.length() - 2) && Character.isHighSurrogate(str.charAt(index)) && Character.isLowSurrogate(str.charAt(index + 1));
    }

    /**
     * Returns the longest common substring between two given CharSequences.
     * If either CharSequence is empty or {@code null}, the method will return an empty string.
     *
     * @param a The first CharSequence.
     * @param b The second CharSequence.
     * @return an empty String {@code ""} is {@code a} or {@code b} is empty or {@code null}.
     */
    public static String longestCommonSubstring(final CharSequence a, final CharSequence b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY;
        }

        final int lenA = N.len(a);
        final int lenB = N.len(b);

        char[] charsToCheck = null;
        if (lenA < lenB) {
            charsToCheck = a.toString().toCharArray();
        } else {
            charsToCheck = b.toString().toCharArray();
        }

        checkInputChars(charsToCheck, lenA < lenB ? "a" : "b", true);

        final int[] dp = new int[lenB + 1];
        int endIndex = 0;
        int maxLen = 0;

        if (lenA > 16 || lenB > 16) {
            final char[] chsA = lenA < lenB ? charsToCheck : a.toString().toCharArray();
            final char[] chsB = lenA < lenB ? b.toString().toCharArray() : charsToCheck;

            for (int i = 1; i <= lenA; i++) {
                for (int j = lenB; j > 0; j--) {
                    if (chsA[i - 1] == chsB[j - 1]) {
                        dp[j] = 1 + dp[j - 1];

                        if (dp[j] > maxLen) {
                            maxLen = dp[j];
                            endIndex = i;
                        }
                    } else {
                        dp[j] = 0;
                    }
                }
            }
        } else {
            for (int i = 1; i <= lenA; i++) {
                for (int j = lenB; j > 0; j--) {
                    if (a.charAt(i - 1) == b.charAt(j - 1)) {
                        dp[j] = 1 + dp[j - 1];

                        if (dp[j] > maxLen) {
                            maxLen = dp[j];
                            endIndex = i;
                        }
                    } else {
                        dp[j] = 0;
                    }
                }
            }
        }

        if (maxLen == 0) {
            return EMPTY;
        }

        return a.subSequence(endIndex - maxLen, endIndex).toString();
    }

    /**
     * Returns the first character of the given string as an OptionalChar.
     * If the string is {@code null} or empty, an empty OptionalChar is returned.
     *
     * @param str the input string
     * @return an OptionalChar containing the first character of the string, or an empty OptionalChar if the string is {@code null} or empty
     */
    public static OptionalChar firstChar(final String str) {
        if (str == null || str.isEmpty()) {
            return OptionalChar.empty();
        }

        return OptionalChar.of(str.charAt(0));
    }

    /**
     * Returns the last character of the given string as an OptionalChar.
     * If the string is {@code null} or empty, an empty OptionalChar is returned.
     *
     * @param str the input string
     * @return an OptionalChar containing the last character of the string, or an empty OptionalChar if the string is {@code null} or empty
     */
    public static OptionalChar lastChar(final String str) {
        if (str == null || str.isEmpty()) {
            return OptionalChar.empty();
        }

        return OptionalChar.of(str.charAt(str.length() - 1));
    }

    /**
     * Returns at most first {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or {@code null}, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static String firstChars(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (str == null || str.isEmpty() || n == 0) {
            return EMPTY;
        } else if (str.length() <= n) {
            return str;
        } else {
            return str.substring(0, n);
        }
    }

    /**
     * Returns at most last {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or {@code null}, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static String lastChars(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (str == null || str.isEmpty() || n == 0) {
            return EMPTY;
        } else if (str.length() <= n) {
            return str;
        } else {
            return str.substring(str.length() - n);
        }
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex)}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @return
     * @see StrUtil#substring(String, int)
     * @see #substring(String, int, int)
     * @see #substringAfter(String, char)
     */
    @MayReturnNull
    public static String substring(final String str, final int inclusiveBeginIndex) {
        if (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        return str.substring(inclusiveBeginIndex);
    }

    //    /**
    //     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())},
    //     * otherwise returns: {@code str.substring(inclusiveBeginIndex)}.
    //     *
    //     * @param str
    //     * @param inclusiveBeginIndex
    //     * @return
    //     * @see #substring(String, int)
    //     * @see StrUtil#substring(String, int)
    //     * @see #substring(String, int, int)
    //     * @see #largestSubstring(String, int, int)
    //     */
    //    @MayReturnNull
    //    public static String substringAfter(String str, int inclusiveBeginIndex) {
    //        if (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
    //            return null;
    //        }
    //
    //        return str.substring(inclusiveBeginIndex);
    //    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex, min(exclusiveEndIndex, str.length()))}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     * @see StrUtil#substring(String, int, int)
     */
    @MayReturnNull
    public static String substring(final String str, final int inclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex || inclusiveBeginIndex > str.length()) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, N.min(exclusiveEndIndex, str.length()));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || inclusiveBeginIndex < 0)}, or {@code funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) < 0}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return
     * @see StrUtil#substring(String, int, IntUnaryOperator)
     * @see #substring(String, int, int)
     */
    @MayReturnNull
    public static String substring(final String str, final int inclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0) {
            return null;
        }

        return substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex));
    }

    //    /**
    //     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0)}, or {@code funcOfExclusiveEndIndex.apply(str, inclusiveBeginIndex) < 0}.
    //     *
    //     * @param str
    //     * @param inclusiveBeginIndex
    //     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.apply(str, inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
    //     * @return {@code null} if {@code (str == null || inclusiveBeginIndex < 0)}. (auto-generated java doc for return)
    //     * @see #substring(String, int, int)
    //     */
    //    @MayReturnNull
    //    @Beta
    //    public static String substring(final String str, final int inclusiveBeginIndex, final BiFunction<? super String, Integer, Integer> funcOfExclusiveEndIndex) {
    //        if (str == null || inclusiveBeginIndex < 0) {
    //            return null;
    //        }
    //
    //        return substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex.apply(str, inclusiveBeginIndex));
    //    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || exclusiveEndIndex < 0)}, or {@code funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex) < 0}.
     *
     *
     * @param str
     * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see StrUtil#substring(String, IntUnaryOperator, int)
     * @see #substring(String, int, int)
     */
    @MayReturnNull
    public static String substring(final String str, final IntUnaryOperator funcOfInclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex < 0) {
            return null;
        }

        return substring(str, funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    //    /**
    //     * Returns {@code null} which means it doesn't exist if {@code (str == null || exclusiveEndIndex < 0)}, or {@code funcOfInclusiveBeginIndex.apply(str, exclusiveEndIndex) < 0}.
    //     *
    //     *
    //     * @param str
    //     * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.apply(str, exclusiveEndIndex)) if exclusiveEndIndex > 0}
    //     * @param exclusiveEndIndex
    //     * @return {@code null} if {@code (str == null || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
    //     * @see #substring(String, int, int)
    //     */
    //    @MayReturnNull
    //    @Beta
    //    public static String substring(final String str, final BiFunction<? super String, Integer, Integer> funcOfInclusiveBeginIndex, final int exclusiveEndIndex) {
    //        if (str == null || exclusiveEndIndex < 0) {
    //            return null;
    //        }
    //
    //        return substring(str, funcOfInclusiveBeginIndex.apply(str, exclusiveEndIndex), exclusiveEndIndex);
    //    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || str.length() == 0)}, or {@code str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise returns: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substringAfter(String, char)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final char delimiterOfInclusiveBeginIndex) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        return substring(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || delimiterOfInclusiveBeginIndex == null)}, or {@code str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise returns: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substringAfter(String, String)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final String delimiterOfInclusiveBeginIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfInclusiveBeginIndex.isEmpty()) {
            return str;
        }

        return substring(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || str.length() == 0 || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}, or {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1)}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1)}
     * @return
     * @see #substring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final int inclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        if (str == null || str.isEmpty() || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1);

        // inconsistant behavior
        //    if (index < 0 && str.charAt(inclusiveBeginIndex) == delimiterOfExclusiveEndIndex) {
        //        return EMPTY_STRING;
        //    }

        return substring(str, inclusiveBeginIndex, index);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || delimiterOfExclusiveEndIndex == {@code null} || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}, or {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) < 0}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return EMPTY;
        }

        return substring(str, inclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || str.length() == 0 || exclusiveEndIndex < 0)}, or {@code str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) < 0}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final char delimiterOfInclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || str.isEmpty() || exclusiveEndIndex < 0) {
            return null;
        }

        return substring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == {@code null} || delimiterOfInclusiveBeginIndex == {@code null} || exclusiveEndIndex < 0)}, or {@code str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) < 0}.
     *
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - exclusiveEndIndex - delimiterOfInclusiveBeginIndex.length()) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(final String str, final String delimiterOfInclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfInclusiveBeginIndex.isEmpty()) {
            return EMPTY;
        }

        return substring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - delimiterOfInclusiveBeginIndex.length()), exclusiveEndIndex);
    }

    /**
     * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfter(final String str, final char delimiterOfExclusiveBeginIndex) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + 1);
    }

    /**
     * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfter(final String str, final String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return str;
        }

        final int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code str == {@code null} || delimiterOfExclusiveBeginIndex == {@code null} || exclusiveEndIndex < 0}, or {@code str.indexOf(delimiterOfExclusiveBeginIndex) < 0 || str.indexOf(delimiterOfExclusiveBeginIndex) + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex}
     * otherwise returns {@code substring(str, index + delimiterOfExclusiveBeginIndex.length(), exclusiveEndIndex)};
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfter(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return substring(str, 0, exclusiveEndIndex);
        } else if (exclusiveEndIndex == 0) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0 || index + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex) {
            return null;
        }

        return substring(str, index + delimiterOfExclusiveBeginIndex.length(), exclusiveEndIndex);
    }

    /**
     * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, ignoring case considerations. Otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @Beta
    @MayReturnNull
    public static String substringAfterIgnoreCase(final String str, final String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return str;
        }

        final int index = indexOfIgnoreCase(str, delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfterLast(final String str, final char delimiterOfExclusiveBeginIndex) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        final int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + 1);
    }

    /**
     * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfterLast(final String str, final String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return EMPTY;
        }

        final int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} searched from the {@code exclusiveEndIndex} from the back of given string if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringAfterLast(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return EMPTY;
        } else if (exclusiveEndIndex == 0) {
            return null;
        }

        final int lengthOfDelimiter = delimiterOfExclusiveBeginIndex.length();

        final int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - lengthOfDelimiter);

        if (index < 0 || index + lengthOfDelimiter > exclusiveEndIndex) {
            return null;
        }

        return str.substring(index + lengthOfDelimiter, exclusiveEndIndex);
    }

    /**
     * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} if it exists, ignoring case considerations. Otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    @Beta
    @MayReturnNull
    public static String substringAfterLastIgnoreCase(final String str, final String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.isEmpty()) {
            return EMPTY;
        }

        final int index = lastIndexOfIgnoreCase(str, delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveBeginIndex
     * @return
     * @see #substringAfter(String, String)
     */
    @MayReturnNull
    public static String substringAfterAny(final String str, final char... delimitersOfExclusiveBeginIndex) {
        checkInputChars(delimitersOfExclusiveBeginIndex, cs.delimitersOfExclusiveBeginIndex, true);

        if (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex)) {
            return null;
        }

        int index = -1;

        for (final char delimiterOfExclusiveBeginIndex : delimitersOfExclusiveBeginIndex) {
            index = str.indexOf(delimiterOfExclusiveBeginIndex);

            if (index >= 0) {
                return str.substring(index + 1);
            }
        }

        return null;
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveBeginIndex
     * @return
     * @see #substringAfter(String, String)
     */
    @MayReturnNull
    public static String substringAfterAny(final String str, final String... delimitersOfExclusiveBeginIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex)) {
            return null;
        }

        String substr = null;

        for (final String delimiterOfExclusiveBeginIndex : delimitersOfExclusiveBeginIndex) {
            substr = substringAfter(str, delimiterOfExclusiveBeginIndex);

            if (substr != null) {
                return substr;
            }
        }

        return null;
    }

    /**
     * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBefore(final String str, final char delimiterOfExclusiveEndIndex) {
        if (str == null) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveEndIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(0, index);
    }

    /**
     * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBefore(final String str, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return EMPTY;
        }

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(0, endIndex);
    }

    /**
     * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBefore(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return EMPTY;
        } else if (inclusiveBeginIndex == str.length()) {
            return null;
        }

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, endIndex);
    }

    /**
     * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, ignoring case considerations. Otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @Beta
    @MayReturnNull
    public static String substringBeforeIgnoreCase(final String str, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return EMPTY;
        }

        final int endIndex = indexOfIgnoreCase(str, delimiterOfExclusiveEndIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(0, endIndex);
    }

    /**
     * Returns the substring before last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBeforeLast(final String str, final char delimiterOfExclusiveEndIndex) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        final int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(0, index);
    }

    /**
     * Returns the substring before last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBeforeLast(final String str, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return str;
        }

        final int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(0, index);
    }

    /**
     * Returns the substring before last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBeforeLast(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return str.substring(inclusiveBeginIndex);
        } else if (inclusiveBeginIndex == str.length()) {
            return null;
        }

        final int endIndex = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (endIndex < 0 || endIndex < inclusiveBeginIndex) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, endIndex);
    }

    /**
     * Returns the substring before last {@code delimiterOfExclusiveBeginIndex} if it exists, ignoring case considerations. Otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    @Beta
    @MayReturnNull
    public static String substringBeforeLastIgnoreCase(final String str, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.isEmpty()) {
            return str;
        }

        final int index = lastIndexOfIgnoreCase(str, delimiterOfExclusiveEndIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(0, index);
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveEndIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveEndIndex
     * @return
     * @see #substringBefore(String, String)
     */
    @MayReturnNull
    public static String substringBeforeAny(final String str, final char... delimitersOfExclusiveEndIndex) {
        checkInputChars(delimitersOfExclusiveEndIndex, cs.delimitersOfExclusiveEndIndex, true);

        if (str == null || N.isEmpty(delimitersOfExclusiveEndIndex)) {
            return null;
        }

        int index = -1;

        for (final char delimiterOfExclusiveEndIndex : delimitersOfExclusiveEndIndex) {
            index = str.indexOf(delimiterOfExclusiveEndIndex);

            if (index >= 0) {
                return str.substring(0, index);
            }
        }

        return null;
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveEndIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveEndIndex
     * @return
     * @see #substringBefore(String, String)
     */
    @MayReturnNull
    public static String substringBeforeAny(final String str, final String... delimitersOfExclusiveEndIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveEndIndex)) {
            return null;
        }

        String substr = null;

        for (final String delimiterOfExclusiveEndIndex : delimitersOfExclusiveEndIndex) {
            substr = substringBefore(str, delimiterOfExclusiveEndIndex);

            if (substr != null) {
                return substr;
            }
        }

        return null;
    }

    /**
     * Returns the substring between the two specified {@code exclusiveBeginIndex} and {@code exclusiveEndIndex}.
     * If {@code str == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= exclusiveEndIndex || exclusiveBeginIndex >= str.length()}, {@code null} is returned.
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBetween(final String str, final int exclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= exclusiveEndIndex || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        return str.substring(exclusiveBeginIndex + 1, N.min(exclusiveEndIndex, str.length()));
    }

    /**
     * Returns the substring between the two specified {@code exclusiveBeginIndex} and {@code delimiterOfExclusiveEndIndex}.
     * If {@code str == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= str.length()}, {@code null} is returned.
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final int exclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        final int startIndex = exclusiveBeginIndex + 1;
        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /**
     * Returns the substring between the two specified {@code exclusiveBeginIndex} and {@code delimiterOfExclusiveEndIndex}.
     * If {@code str == null || delimiterOfExclusiveEndIndex == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= str.length()}, {@code null} is returned.
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final int exclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        final int startIndex = exclusiveBeginIndex + 1;
        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /**
     * Returns the substring between the specified {@code delimiterOfExclusiveBeginIndex} and {@code exclusiveEndIndex}.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final char delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex <= 0) {
            return null;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0 || startIndex >= exclusiveEndIndex) {
            return null;
        }

        startIndex += 1;

        if (startIndex > exclusiveEndIndex) {
            return null;
        }

        return str.substring(startIndex, exclusiveEndIndex);
    }

    /**
     * Returns the substring between the specified {@code delimiterOfExclusiveBeginIndex} and {@code exclusiveEndIndex}.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0 || startIndex > exclusiveEndIndex) {
            return null;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        if (startIndex > exclusiveEndIndex) {
            return null;
        }

        return str.substring(startIndex, exclusiveEndIndex);
    }

    /**
     * Returns substring between the specified {@code delimiterOfExclusiveBeginIndex} and {@code delimiterOfExclusiveEndIndex}.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() <= 1) {
            return null;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0) {
            return null;
        }

        // even delimiterOfExclusiveBeginIndex and delimiterOfExclusiveEndIndex are equal, but should consider them as different chars. see: substringBetween(String str, String tag)
        //    if (delimiterOfExclusiveBeginIndex == delimiterOfExclusiveEndIndex) {
        //        return EMPTY_STRING;
        //    }

        startIndex += 1;

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /*
     * Returns a substring from the given string that is between the two specified delimiters.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str
     * @param tag
     * @return
     * @see #substringBetween(String, String, String)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final String delimiter) {
        return substringBetween(str, delimiter, delimiter);
    }

    /**
     * Returns a substring from the given string that is between the two specified delimiters.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str The string from which to extract the substring.
     * @param delimiterOfExclusiveBeginIndex The delimiter after which the substring starts.
     * @param delimiterOfExclusiveEndIndex The delimiter before which the substring ends.
     * @return The substring between the two delimiters. Returns {@code null} if the delimiters are not found.
     */
    @MayReturnNull
    public static String substringBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        return substringBetween(str, 0, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * Returns a substring from the given string that is between the two specified delimiters.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str
     * @param fromIndex start index for {@code delimiterOfExclusive}. {@code str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex)}
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return The substring between the two delimiters. Returns {@code null} if the delimiters are not found.
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final int fromIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveEndIndex == null || fromIndex > str.length()) {
            return null;
        }

        int startIndex = fromIndex <= 0 ? str.indexOf(delimiterOfExclusiveBeginIndex) : str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

        if (startIndex < 0) {
            return null;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        final int endIndex = str.indexOf(delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /**
     * Returns a substring from the given string that is between the two specified delimiters, ignoring case considerations.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str
     * @param delimiter
     * @return The substring between the two delimiters. Returns {@code null} if the delimiters are not found.
     * @see #substringBetweenIgnoreCaes(String, String, String) 
     */
    @Beta
    @MayReturnNull
    public static String substringBetweenIgnoreCaes(final String str, final String delimiter) {
        return substringBetweenIgnoreCaes(str, delimiter, delimiter);
    }

    /**
     * Returns a substring from the given string that is between the two specified delimiters, ignoring case considerations.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str The string from which to extract the substring.
     * @param delimiterOfExclusiveBeginIndex The delimiter after which the substring starts.
     * @param delimiterOfExclusiveEndIndex The delimiter before which the substring ends.
     * @return The substring between the two delimiters. Returns {@code null} if the delimiters are not found.
     * @see #substringBetweenIgnoreCaes(String, int, String, String)
     */
    @Beta
    @MayReturnNull
    public static String substringBetweenIgnoreCaes(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        return substringBetweenIgnoreCaes(str, 0, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * Returns a substring from the given string that is between the two specified delimiters, ignoring case considerations.
     * The substring does not include the delimiters themselves.
     * If the delimiters are not found, this method will return {@code null}.
     *
     * @param str
     * @param fromIndex start index for {@code delimiterOfExclusiveBeginIndex}. {@code str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex)}
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return The substring between the two delimiters. Returns {@code null} if the delimiters are not found.
     */
    @Beta
    @MayReturnNull
    public static String substringBetweenIgnoreCaes(final String str, final int fromIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveEndIndex == null || fromIndex > str.length()) {
            return null;
        }

        int startIndex = fromIndex <= 0 ? indexOfIgnoreCase(str, delimiterOfExclusiveBeginIndex)
                : indexOfIgnoreCase(str, delimiterOfExclusiveBeginIndex, fromIndex);

        if (startIndex < 0) {
            return null;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        final int endIndex = indexOfIgnoreCase(str, delimiterOfExclusiveEndIndex, startIndex);

        if (endIndex < 0) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex)}
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final int exclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < -1 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        final int endIndex = N.min(funcOfExclusiveEndIndex.applyAsInt(exclusiveBeginIndex), str.length());

        if (endIndex < 0 || endIndex <= exclusiveBeginIndex) {
            return null;
        }

        return str.substring(exclusiveBeginIndex + 1, endIndex);
    }

    //    /**
    //     *
    //     * @param str
    //     * @param exclusiveBeginIndex
    //     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.apply(str, exclusiveBeginIndex) if inclusiveBeginIndex >= 0}
    //     * @return {@code null} if {@code (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length())}. (auto-generated java doc for return)
    //     * @see #substringBetween(String, int, int)
    //     */
    //    @MayReturnNull
    //    @Beta
    //    public static String substringBetween(String str, int exclusiveBeginIndex, final BiFunction<? super String, Integer, Integer> funcOfExclusiveEndIndex) {
    //        if (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length()) {
    //            return null;
    //        }
    //
    //        return substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex.apply(str, exclusiveBeginIndex));
    //    }

    /**
     *
     * @param str
     * @param funcOfExclusiveBeginIndex {@code exclusiveBeginIndex <- funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex >= 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final IntUnaryOperator funcOfExclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex <= 0) {
            return null;
        }

        final int endIndex = N.min(exclusiveEndIndex, str.length());
        int startIndex = funcOfExclusiveBeginIndex.applyAsInt(endIndex);

        if (startIndex < -1 || startIndex >= endIndex) {
            return null;
        }

        return str.substring(startIndex + 1, endIndex);
    }

    //    /**
    //     *
    //     * @param str
    //     * @param funcOfExclusiveBeginIndex {@code exclusiveBeginIndex <- funcOfExclusiveBeginIndex.apply(str, exclusiveEndIndex)) if exclusiveEndIndex >= 0}
    //     * @param exclusiveEndIndex
    //     * @return {@code null} if {@code (str == null || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
    //     * @see #substringBetween(String, int, int)
    //     */
    //    @MayReturnNull
    //    @Beta
    //    public static String substringBetween(String str, final BiFunction<? super String, Integer, Integer> funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
    //        if (str == null || exclusiveEndIndex < 0) {
    //            return null;
    //        }
    //
    //        return substringBetween(str, funcOfExclusiveBeginIndex.apply(str, exclusiveEndIndex), exclusiveEndIndex);
    //    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param funcOfExclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final String delimiterOfExclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveBeginIndex.length() >= str.length()) {
            return null;
        }

        int startIndex = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (startIndex < 0) {
            return null;
        }

        startIndex += delimiterOfExclusiveBeginIndex.length();

        final int endIndex = N.min(funcOfExclusiveEndIndex.applyAsInt(startIndex), str.length());

        if (endIndex < 0 || endIndex < startIndex) {
            return null;
        }

        return str.substring(startIndex, endIndex);
    }

    /**
     * Returns a substring from the given string, starting from a specified index and ending before a specified delimiter.
     * The starting index is determined by applying the provided IntUnaryOperator function on the ending index of the substring.
     * The ending index is the last occurrence of the specified delimiter in the string.
     * If the string or the delimiter is {@code null}, or if the delimiter's length is greater than the string's length, or if the ending index is less than 0, the method returns {@code null}.
     *
     * @param str The string from which to extract the substring. It can be {@code null}.
     * @param funcOfExclusiveBeginIndex The function to determine the starting index of the substring. It should not be {@code null}.
     * @param delimiterOfExclusiveEndIndex The delimiter before which the substring ends. It should not be {@code null}.
     * @return The extracted substring. Returns {@code null} if the input string is {@code null}, the function is {@code null}, the delimiter is {@code null}, the delimiter's length is greater than the string's length, or the ending index is less than 0.
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(final String str, final IntUnaryOperator funcOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || delimiterOfExclusiveEndIndex.length() >= str.length()) {
            return null;
        }

        final int endIndex = N.min(str.lastIndexOf(delimiterOfExclusiveEndIndex), str.length());

        if (endIndex < 0) {
            return null;
        }

        final int startIndex = funcOfExclusiveBeginIndex.applyAsInt(endIndex);

        if (startIndex < -1 || startIndex >= endIndex) {
            return null;
        }

        return str.substring(startIndex + 1, endIndex);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p> 
     * 
     * @param str the string to search in, may be null
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     */
    public static List<String> substringsBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        return substringsBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p> 
     * 
     * @param str the string to search in, may be null
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IllegalArgumentException if extractStrategy is null
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     */
    public static List<String> substringsBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex,
            final ExtractStrategy extractStrategy) {
        return substringsBetween(str, String.valueOf(delimiterOfExclusiveBeginIndex), String.valueOf(delimiterOfExclusiveEndIndex), extractStrategy);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p> 
     * 
     * @param str the string to search in, may be null
     * @param fromIndex the index to start the search from (inclusive)
     * @param toIndex the index to end the search at (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     */
    public static List<String> substringsBetween(final String str, final int fromIndex, final int toIndex, final char delimiterOfExclusiveBeginIndex,
            final char delimiterOfExclusiveEndIndex) {
        return substringsBetween(str, fromIndex, toIndex, String.valueOf(delimiterOfExclusiveBeginIndex), String.valueOf(delimiterOfExclusiveEndIndex),
                ExtractStrategy.DEFAULT, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p> 
     * 
     * @param str the string to search in, may be null
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     */
    public static List<String> substringsBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        return substringsBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p>
     * 
     * <p>The {@code extractStrategy} parameter controls how nested delimiters are handled:
     * <ul>
     *   <li>{@code ExtractStrategy.DEFAULT} - Simple sequential matching of begin/end delimiters</li>
     *   <li>{@code ExtractStrategy.STACK_BASED} -  Stack-based approach strategy used to extract substring between two delimiters</li>
     *   <li>{@code ExtractStrategy.IGNORE_NESTED} -Stack-based approach strategy used to extract substring between two delimiters but nested substrings are ignored</li>
     * </ul>
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>
     * <code>
     * // String:   3 [ a 2 [ c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = ["a2[c", "a"].
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = ["c", "a2[c]", "a"].
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["a2[c]", "a"].
     * </code>
     * <code>
     * // String:   3 [ a 2 c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = ["a2[c", "a"].
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = ["a2[c", "a"].
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["a2[c", "a"].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] ] c ]
     * // Index:    0 1 2 3 4 5 6 7 8
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT, Integer.MAX_VALUE) = ["[b[a"].
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED, Integer.MAX_VALUE) = ["a", "b[a]", "[b[a]]c"].
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED, Integer.MAX_VALUE) = ["[b[a]]c"].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] [ c ] d ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT) = ["[b[a", "c"].
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED) = ["a", "c", "b[a][c]d"].
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["b[a][c]d"].
     * </code>
     * </pre>
     * 
     * @param str the string to search in, may be null
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IllegalArgumentException if extractStrategy is null
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringsBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<String> substringsBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex,
            final ExtractStrategy extractStrategy) {
        return substringsBetween(str, 0, N.len(str), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, extractStrategy, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p> 
     * 
     * @param str the string to search in, may be null
     * @param fromIndex the index to start the search from (inclusive)
     * @param toIndex the index to end the search at (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     */
    public static List<String> substringsBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        return substringsBetween(str, fromIndex, toIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT,
                Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters.</p>
     * 
     * <p>The {@code extractStrategy} parameter controls how nested delimiters are handled:
     * <ul>
     *   <li>{@code ExtractStrategy.DEFAULT} - Simple sequential matching of begin/end delimiters</li>
     *   <li>{@code ExtractStrategy.STACK_BASED} -  Stack-based approach strategy used to extract substring between two delimiters</li>
     *   <li>{@code ExtractStrategy.IGNORE_NESTED} -Stack-based approach strategy used to extract substring between two delimiters but nested substrings are ignored</li>
     * </ul>
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>
     * <code>
     * // String:   3 [ a 2 [ c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = ["a2[c", "a"].
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = ["c", "a2[c]", "a"].
     * substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["a2[c]", "a"].
     * </code>
     * <code>
     * // String:   3 [ a 2 c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = ["a2[c", "a"].
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = ["a2[c", "a"].
     * substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["a2[c", "a"].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] ] c ]
     * // Index:    0 1 2 3 4 5 6 7 8
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT, Integer.MAX_VALUE) = ["[b[a"].
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED, Integer.MAX_VALUE) = ["a", "b[a]", "[b[a]]c"].
     * substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED, Integer.MAX_VALUE) = ["[b[a]]c"].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] [ c ] d ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT) = ["[b[a", "c"].
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED) = ["a", "c", "b[a][c]d"].
     * substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["b[a][c]d"].
     * </code>
     * </pre>
     * 
     * @param str the string to search in, may be null
     * @param fromIndex the index to start the search from (inclusive)
     * @param toIndex the index to end the search at (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @param maxCount the maximum number of substrings to extract; if {@code -1}, all matching substrings will be extracted
     * @return a list of matched substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @throws IllegalArgumentException if max is negative or extractStrategy is null
     * @see #substringsBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<String> substringsBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex, final ExtractStrategy extractStrategy, final int maxCount) {

        final List<int[]> substringIndices = substringIndicesBetween(str, fromIndex, toIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex,
                extractStrategy, maxCount);

        final List<String> res = new ArrayList<>(substringIndices.size());

        for (final int[] e : substringIndices) {
            res.add(str.substring(e[0], e[1]));
        }

        return res;

    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p>
     * 
     * @param str
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        return substringIndicesBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT);
    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p>
     * 
     * @param str
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @throws IllegalArgumentException if extractStrategy is null
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex,
            final ExtractStrategy extractStrategy) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }

        return substringIndicesBetween(str, 0, str.length(), String.valueOf(delimiterOfExclusiveBeginIndex), String.valueOf(delimiterOfExclusiveEndIndex),
                extractStrategy, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p> 
     *
     * @param str the string to search in, may be null
     * @param fromIndex the starting index to search from (inclusive)
     * @param toIndex the ending index to search until (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive) 
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final int fromIndex, final int toIndex, final char delimiterOfExclusiveBeginIndex,
            final char delimiterOfExclusiveEndIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }

        return substringIndicesBetween(str, fromIndex, toIndex, String.valueOf(delimiterOfExclusiveBeginIndex), String.valueOf(delimiterOfExclusiveEndIndex),
                ExtractStrategy.DEFAULT, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p>
     * 
     * @param str
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        return substringIndicesBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT);
    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p>
     *
     * <p>This method searches the input string for occurrences of text between the specified beginning
     * and ending delimiters, and returns a list of int arrays where each array contains the start and end
     * indices of a matched substring (exclusive of the delimiters themselves).</p>
     *
     *
     * <p>The {@code extractStrategy} parameter controls how nested delimiters are handled:
     * <ul>
     *   <li>{@code ExtractStrategy.DEFAULT} - Simple sequential matching of begin/end delimiters</li>
     *   <li>{@code ExtractStrategy.STACK_BASED} -  Stack-based approach strategy used to extract substring between two delimiters</li>
     *   <li>{@code ExtractStrategy.IGNORE_NESTED} -Stack-based approach strategy used to extract substring between two delimiters but nested substrings are ignored</li>
     * </ul>
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>
     * <code>
     * // String:   3 [ a 2 [ c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = [[2, 6], [10, 11]].
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = [[5, 6], [2, 7], [10, 11]].
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[2, 7], [10, 11]].
     * </code>
     * <code>
     * // String:   3 [ a 2 c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = [[2, 5], [9, 10]].
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = [[2, 5], [9, 10]].
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[2, 5], [9, 10]].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] ] c ]
     * // Index:    0 1 2 3 4 5 6 7 8
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT) = [[1, 5]].
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED) = [[4, 5], [2, 6], [1, 8]].
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[1, 8]].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] [ c ] d ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT) = [[1, 5], [7, 8]].
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED) = [[[4, 5], [7, 8], [2, 10]]].
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[2, 10]].
     * </code>
     * </pre>
     *
     * @param str the string to search in, may be null
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @throws IllegalArgumentException if extractStrategy is null
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex,
            final ExtractStrategy extractStrategy) {
        if (str == null || isEmpty(delimiterOfExclusiveBeginIndex) || isEmpty(delimiterOfExclusiveEndIndex)) {
            return new ArrayList<>();
        }

        return substringIndicesBetween(str, 0, str.length(), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, extractStrategy, Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters and returns their indices.</p> 
     *
     * @param str the string to search in, may be null
     * @param fromIndex the starting index to search from (inclusive)
     * @param toIndex the ending index to search until (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive) 
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringIndicesBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) throws IndexOutOfBoundsException {
        return substringIndicesBetween(str, fromIndex, toIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex, ExtractStrategy.DEFAULT,
                Integer.MAX_VALUE);
    }

    /**
     * <p>Finds all substrings between specified delimiters within a given range and returns their indices.</p>
     *
     * <p>This method searches the input string for occurrences of text between the specified beginning
     * and ending delimiters, and returns a list of int arrays where each array contains the start and end
     * indices of a matched substring (exclusive of the delimiters themselves).</p>
     *
     * <p>The search is performed within the specified range from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive).</p>
     *
     * <p>The {@code extractStrategy} parameter controls how nested delimiters are handled:
     * <ul>
     *   <li>{@code ExtractStrategy.DEFAULT} - Simple sequential matching of begin/end delimiters</li>
     *   <li>{@code ExtractStrategy.STACK_BASED} -  Stack-based approach strategy used to extract substring between two delimiters</li>
     *   <li>{@code ExtractStrategy.IGNORE_NESTED} -Stack-based approach strategy used to extract substring between two delimiters but nested substrings are ignored</li>
     * </ul>
     * </p>
     *
     * <p>Example usage:</p>
     * <pre>
     * <code>
     * // String:   3 [ a 2 [ c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = [[2, 6], [10, 11]].
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = [[5, 6], [2, 7], [10, 11]].
     * substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[2, 7], [10, 11]].
     * </code>
     * <code>
     * // String:   3 [ a 2 c ] ] 2 [ a ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = [[2, 6], [9, 10]].
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = [[2, 6], [9, 10]].
     * substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[2, 6], [9, 10]].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] ] c ]
     * // Index:    0 1 2 3 4 5 6 7 8
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT, Integer.MAX_VALUE) = [[1, 5]].
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED, Integer.MAX_VALUE) = [[4, 5], [2, 6], [1, 8]].
     * substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED, Integer.MAX_VALUE) = [[1, 8]].
     * </code>
     * <code>
     * // String:   [ [ b [ a ] [ c ] d ]
     * // Index:    0 1 2 3 4 5 6 7 8 9 10 11
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT) = [[1, 5], [7, 8]].
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED) = [[4, 5], [2, 6], [1, 8]].
     * substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED) = [[1, 8]].
     * </code>
     * </pre>
     *
     * @param str the string to search in, may be null
     * @param fromIndex the index to start the search from (inclusive)
     * @param toIndex the index to end the search at (exclusive)
     * @param delimiterOfExclusiveBeginIndex the string marking the beginning of the substring (non-inclusive)
     * @param delimiterOfExclusiveEndIndex the string marking the end of the substring (non-inclusive)
     * @param extractStrategy the strategy to use for handling nested delimiters
     * @param maxCount the maximum number of matches to find
     * @return a list of int arrays containing the start and end indices (exclusive of delimiters) of each
     *         matching substring, or an empty list if no match is found or the input is null
     * @throws IndexOutOfBoundsException if the indices are invalid
     * @throws IllegalArgumentException if max is negative or extractStrategy is null
     *
     * @see #substringIndicesBetween(String, String, String, ExtractStrategy)
     * @see #substringsBetween(String, int, int, String, String, ExtractStrategy, int)
     */
    public static List<int[]> substringIndicesBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex, final ExtractStrategy extractStrategy, final int maxCount) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));
        N.checkArgNotNegative(maxCount, cs.max);
        N.checkArgNotNull(extractStrategy, cs.extractStrategy);

        if (str == null || isEmpty(delimiterOfExclusiveBeginIndex) || isEmpty(delimiterOfExclusiveEndIndex) || maxCount == 0) {
            return new ArrayList<>();
        }

        int idx = str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

        if (idx < 0) {
            return new ArrayList<>();
        }

        final List<int[]> res = new ArrayList<>(N.min(maxCount, 10));

        final int lengthOfDelimiterOfExclusiveBeginIndex = delimiterOfExclusiveBeginIndex.length();
        final int lengthOfDelimiterOfExclusiveEndIndex = delimiterOfExclusiveEndIndex.length();

        idx += lengthOfDelimiterOfExclusiveBeginIndex;

        if (extractStrategy == ExtractStrategy.DEFAULT) {
            int endIndex = -1;

            do {
                endIndex = indexOf(str, delimiterOfExclusiveEndIndex, idx);

                if (endIndex < 0 || endIndex >= toIndex) {
                    break;
                }

                res.add(new int[] { idx, endIndex });

                if (res.size() >= maxCount) {
                    break;
                }

                idx = indexOf(str, delimiterOfExclusiveBeginIndex, endIndex + lengthOfDelimiterOfExclusiveEndIndex);

                if (idx < 0) {
                    break;
                }

                idx += lengthOfDelimiterOfExclusiveBeginIndex;
            } while (idx < toIndex);
        } else {
            final Deque<Integer> queue = new LinkedList<>();

            queue.add(idx);
            int next = -1;

            for (int i = idx; i < toIndex;) {
                if (queue.size() == 0) {
                    idx = next >= i ? next : str.indexOf(delimiterOfExclusiveBeginIndex, i);

                    if (idx < 0) {
                        break;
                    } else {
                        idx += lengthOfDelimiterOfExclusiveBeginIndex;
                        queue.add(idx);
                        i = idx;
                    }
                }

                idx = str.indexOf(delimiterOfExclusiveEndIndex, i);

                if (idx < 0) {
                    break;
                } else {
                    final int endIndex = idx;
                    //noinspection DataFlowIssue
                    idx = res.size() > 0 ? Math.max(res.get(res.size() - 1)[1] + lengthOfDelimiterOfExclusiveEndIndex, queue.peekLast()) : queue.peekLast();

                    while ((idx = str.indexOf(delimiterOfExclusiveBeginIndex, idx)) >= 0 && idx < endIndex) {
                        idx += lengthOfDelimiterOfExclusiveBeginIndex;
                        queue.push(idx);
                    }

                    if (idx > 0) {
                        next = idx;
                    }

                    final int startIndex = queue.pop();

                    if (extractStrategy == ExtractStrategy.IGNORE_NESTED && res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                        while (res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                            res.remove(res.size() - 1);
                        }
                    }

                    res.add(new int[] { startIndex, endIndex });

                    if (res.size() >= maxCount) {
                        break;
                    }

                    i = endIndex + lengthOfDelimiterOfExclusiveEndIndex;
                }
            }
        }

        return res;
    }

    /**
     * Returns a new String with the specified range replaced with the replacement String.
     * <br />
     * The original String remains unchanged.
     *
     * @param str the original string
     * @param fromIndex the initial index of the range to be replaced, inclusive
     * @param toIndex the final index of the range to be replaced, exclusive
     * @param replacement the string to replace the specified range in the original string
     * @return a new string with the specified range replaced by the replacement string.
     * @throws IndexOutOfBoundsException if the range is out of the string bounds
     * @see N#replaceRange(String, int, int, String)
     */
    @Beta
    public static String replaceRange(final String str, final int fromIndex, final int toIndex, final String replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (N.isEmpty(str)) {
            return replacement == null ? EMPTY : replacement;
        } else if (fromIndex == toIndex && N.isEmpty(replacement)) {
            return str;
        }

        if (N.isEmpty(replacement)) {
            return str.substring(0, fromIndex) + str.substring(toIndex);
        } else {
            return str.substring(0, fromIndex) + N.nullToEmpty(replacement) + str.substring(toIndex);
        }
    }

    /**
     * Returns a new string with the specified range moved to the new position.
     * The new position specified by {@code newPositionStartIndexAfterMove} is the start index of the specified range after the move operation, not before the move operation.
     * <br />
     * The original String remains unchanged.
     *
     * @param str the original string to be modified
     * @param fromIndex the initial index of the range to be moved, inclusive
     * @param toIndex the final index of the range to be moved, exclusive
     * @param newPositionStartIndexAfterMove the start index of the specified range after the move operation, not before the move operation. 
     *          It must in the range: [0, array.length - (toIndex - fromIndex)]
     * @return a new string with the specified range moved to the new position. An empty String is returned if the specified String is {@code null} or empty.
     * @throws IndexOutOfBoundsException if the range is out of the string bounds or newPositionStartIndexAfterMove is invalid
     * @see N#moveRange(String, int, int, int)
     */
    @Beta
    public static String moveRange(final String str, final int fromIndex, final int toIndex, final int newPositionStartIndexAfterMove)
            throws IndexOutOfBoundsException {
        final int len = N.len(str);
        N.checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndexAfterMove, len);

        if (N.isEmpty(str)) {
            return EMPTY;
        }

        if (fromIndex == toIndex || fromIndex == newPositionStartIndexAfterMove) {
            return str;
        }

        if (newPositionStartIndexAfterMove < fromIndex) {
            return Strings.concat(str.substring(0, newPositionStartIndexAfterMove), str.substring(fromIndex, toIndex),
                    str.substring(newPositionStartIndexAfterMove, fromIndex), str.substring(toIndex));
        } else {
            final int m = toIndex + (newPositionStartIndexAfterMove - fromIndex);

            return Strings.concat(str.substring(0, fromIndex), str.substring(toIndex, m), str.substring(fromIndex, toIndex), str.substring(m));
        }
    }

    /**
     * Returns a new String with the specified range of chars removed
     * <br />
     * The original String remains unchanged.
     *
     * @param str the input string from which a range of characters are to be deleted
     * @param fromIndex the initial index of the range to be deleted, inclusive
     * @param toIndex the final index of the range to be deleted, exclusive
     * @return a new string with the specified range of characters deleted. An empty String is returned if the specified String is {@code null} or empty.
     * @throws IndexOutOfBoundsException if the range is out of the string bounds
     * @see N#deleteRange(String, int, int)
     */
    @Beta
    public static String deleteRange(final String str, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        final int len = N.len(str);

        N.checkFromToIndex(fromIndex, toIndex, len);

        if (N.isEmpty(str)) {
            return EMPTY;
        }

        if (fromIndex == toIndex || fromIndex >= len) {
            return str;
        } else if (toIndex - fromIndex >= len) {
            return Strings.EMPTY;
        }

        return Strings.concat(str.substring(0, fromIndex) + str.substring(toIndex));
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(boolean[], int, int, String, String, String)
     */
    public static String join(final boolean[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(boolean[], int, int, String, String, String)
     */
    public static String join(final boolean[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(boolean[], int, int, String, String, String)
     */
    public static String join(final boolean[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(boolean[], int, int, String, String, String)
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(boolean[], int, int, String, String, String)
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 5 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 5 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(char[], int, int, String, String, String)
     */
    public static String join(final char[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(char[], int, int, String, String, String)
     */
    public static String join(final char[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(char[], int, int, String, String, String)
     */
    public static String join(final char[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(char[], int, int, String, String, String)
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 2));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(char[], int, int, String, String, String)
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 1 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 1 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(byte[], int, int, String, String, String)
     */
    public static String join(final byte[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(byte[], int, int, String, String, String)
     */
    public static String join(final byte[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(byte[], int, int, String, String, String)
     */
    public static String join(final byte[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(byte[], int, int, String, String, String)
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 5));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(byte[], int, int, String, String, String)
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 4 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 4 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(short[], int, int, String, String, String)
     */
    public static String join(final short[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(short[], int, int, String, String, String)
     */
    public static String join(final short[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(short[], int, int, String, String, String)
     */
    public static String join(final short[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(short[], int, int, String, String, String)
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(short[], int, int, String, String, String)
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 5 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 5 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(int[], int, int, String, String, String)
     */
    public static String join(final int[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(int[], int, int, String, String, String)
     */
    public static String join(final int[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(int[], int, int, String, String, String)
     */
    public static String join(final int[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(int[], int, int, String, String, String)
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 7));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(int[], int, int, String, String, String)
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(long[], int, int, String, String, String)
     */
    public static String join(final long[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(long[], int, int, String, String, String)
     */
    public static String join(final long[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(long[], int, int, String, String, String)
     */
    public static String join(final long[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(long[], int, int, String, String, String)
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 7));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(long[], int, int, String, String, String)
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(float[], int, int, String, String, String)
     */
    public static String join(final float[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(float[], int, int, String, String, String)
     */
    public static String join(final float[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(float[], int, int, String, String, String)
     */
    public static String join(final float[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(float[], int, int, String, String, String)
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 7));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(float[], int, int, String, String, String)
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty and <i>prefix'/'suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(double[], int, int, String, String, String)
     */
    public static String join(final double[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(double[], int, int, String, String, String)
     */
    public static String join(final double[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(double[], int, int, String, String, String)
     */
    public static String join(final double[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(double[], int, int, String, String, String)
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 7));

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(a[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(double[], int, int, String, String, String)
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter)));

        try {
            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be empty.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 6 + N.len(delimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(a[i]);
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(a[i]);
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(Object[], String, String, String, boolean)
     */
    public static String join(final Object[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(Object[], String, String, String, boolean)
     */
    public static String join(final Object[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty
     * @see #join(Object[], String, String, String, boolean)
     */
    public static String join(final Object[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix
     * @param suffix
     * @return
     * @see #join(Object[], String, String, String, boolean)
     */
    public static String join(final Object[] a, final String delimiter, final String prefix, final String suffix) {
        return join(a, 0, N.len(a), delimiter, prefix, suffix, false);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a The array containing the elements to join together. It can be {@code null}.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     */
    public static String join(final Object[] a, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        return join(a, 0, N.len(a), delimiter, prefix, suffix, trim);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Object[], int, int, String, String, String, boolean)
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(Object[], int, int, String, String, String, boolean)
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter, final boolean trim)
            throws IndexOutOfBoundsException {
        //    N.checkFromToIndex(fromIndex, toIndex, N.len(a));
        //
        //    if (N.isEmpty(a) || fromIndex == toIndex) {
        //        return EMPTY_STRING;
        //    } else if (toIndex - fromIndex == 1) {
        //        return toString(a[fromIndex], trim);
        //    }
        //
        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 16));
        //
        //    try {
        //        for (int i = fromIndex; i < toIndex; i++) {
        //            if (i > fromIndex) {
        //                sb.append(delimiter);
        //            }
        //
        //            sb.append(toString(a[i], trim));
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        return join(a, fromIndex, toIndex, N.stringOf(delimiter), trim);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Object[], int, int, String, String, String, boolean)
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

    /**
     * Joins the elements of the provided array into a single String.
     *
     * @param a
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Object[], int, int, String, String, String, boolean)
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        return join(a, fromIndex, toIndex, delimiter, null, null, trim);
    }

    /** 
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be {@code null}.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix) {
        return join(a, fromIndex, toIndex, delimiter, prefix, suffix, false);
    }

    /**
     * Joins the elements of the provided array into a single String.
     * The elements are selected from the specified range of the array.
     *
     * @param a The array containing the elements to join together. It can be {@code null}.
     * @param fromIndex The start index in the array from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the array up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the array size.
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return toString(a[fromIndex], trim);
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 16 + N.len(delimiter), N.len(prefix), N.len(suffix)));
        //
        //    try {
        //        if (isNotEmpty(prefix)) {
        //            sb.append(prefix);
        //        }
        //
        //        if (isEmpty(delimiter)) {
        //            for (int i = fromIndex; i < toIndex; i++) {
        //                sb.append(toString(a[i], trim));
        //            }
        //        } else {
        //            for (int i = fromIndex; i < toIndex; i++) {
        //                if (i > fromIndex) {
        //                    sb.append(delimiter);
        //                }
        //
        //                sb.append(toString(a[i], trim));
        //            }
        //        }
        //
        //        if (isNotEmpty(suffix)) {
        //            sb.append(suffix);
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        final int len = toIndex - fromIndex;
        final String[] elements = new String[len];

        for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
            elements[j] = toString(a[i], trim);
        }

        elements[0] = concat(prefix, elements[0]);
        elements[len - 1] = concat(elements[len - 1], suffix);

        return String.join(nullToEmpty(delimiter), elements);
    }

    /**
     * Joins the elements of the provided Iterable into a single String.
     *
     * @param c
     * @return The concatenated string. Returns an empty string if the specified Iterable is {@code null} or empty
     * @see #join(Iterable, String, String, String, boolean)
     */
    public static String join(final Iterable<?> c) {
        return join(c, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided Iterable into a single String.
     *
     * @param c
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified Iterable is {@code null} or empty
     * @see #join(Iterable, String, String, String, boolean)
     */
    public static String join(final Iterable<?> c, final char delimiter) {
        return join(c == null ? null : c.iterator(), delimiter);
    }

    /**
     * Joins the elements of the provided Iterable into a single String.
     *
     * @param c
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified Iterable is {@code null} or empty
     * @see #join(Iterable, String, String, String, boolean)
     */
    public static String join(final Iterable<?> c, final String delimiter) {
        return join(c == null ? null : c.iterator(), delimiter);
    }

    /**
     * Joins the elements of the provided Iterable into a single String.
     *
     * @param c
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix
     * @param suffix
     * @return The concatenated string. Returns an empty string if the specified Iterable is {@code null} or empty and <i>prefix, suffix</i> are empty.
     * @see #join(Iterable, String, String, String, boolean)
     */
    public static String join(final Iterable<?> c, final String delimiter, final String prefix, final String suffix) {
        return join(c == null ? null : c.iterator(), delimiter, prefix, suffix);
    }

    /**
     * Joins the elements of the provided Iterable into a single String.
     *
     * @param c The Iterable containing the elements to join together. It can be {@code null}.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified Iterable is {@code null} or empty and <i>prefix, suffix</i> are empty.
     */
    public static String join(final Iterable<?> c, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        if (c instanceof final Collection<?> coll) { // NOSONAR
            return join(coll, 0, coll.size(), delimiter, prefix, suffix, trim);
        } else {
            return join(c == null ? null : c.iterator(), delimiter, prefix, suffix, trim);
        }
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified Collection is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Collection, int, int, String, String, String, boolean)
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter The delimiter that separates each element.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified Collection is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException
     * @see #join(Collection, int, int, String, String, String, boolean)
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter, final boolean trim)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return EMPTY;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 16));
        //
        //    try {
        //        int i = 0;
        //        for (final Object e : c) {
        //            if (i++ > fromIndex) {
        //                sb.append(delimiter);
        //            }
        //
        //            if (i > fromIndex) {
        //                sb.append(toString(e, trim));
        //            }
        //
        //            if (i >= toIndex) {
        //                break;
        //            }
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        return join(c, fromIndex, toIndex, N.stringOf(delimiter), trim);
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified Collection is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Collection, int, int, String, String, String, boolean)
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified Collection is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #join(Collection, int, int, String, String, String, boolean)
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        return join(c, fromIndex, toIndex, delimiter, null, null, trim);
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     * The elements are selected from the specified range of the Collection.
     *
     * @param c The Collection containing the elements to join together. It can be {@code null}.
     * @param fromIndex The start index in the Collection from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the Collection up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified Collection is {@code null} or empty or {@code fromIndex == toIndex}.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the Collection size.
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix) {
        return join(c, fromIndex, toIndex, delimiter, prefix, suffix, false);
    }

    /**
     * Joins the elements of the provided Collection into a single String.
     * The elements are selected from the specified range of the Collection.
     *
     * @param c The Collection containing the elements to join together. It can be {@code null}.
     * @param fromIndex The start index in the Collection from which to start joining elements. It must be a non-negative integer.
     * @param toIndex The end index in the Collection up to which to join elements. It must be a non-negative integer and not less than fromIndex.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified array is {@code null} or empty or {@code fromIndex == toIndex} and <i>prefix, suffix</i> are empty.
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of the range of the Collection size.
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 16 + N.len(delimiter), N.len(prefix), N.len(suffix)));
        //
        //    try {
        //        if (isNotEmpty(prefix)) {
        //            sb.append(prefix);
        //        }
        //
        //        if (c instanceof List && c instanceof RandomAccess) {
        //            final List<?> list = (List<?>) c;
        //
        //            if (isEmpty(delimiter)) {
        //                for (int i = fromIndex; i < toIndex; i++) {
        //                    sb.append(toString(list.get(i), trim));
        //                }
        //            } else {
        //                for (int i = fromIndex; i < toIndex; i++) {
        //                    if (i > fromIndex) {
        //                        sb.append(delimiter);
        //                    }
        //
        //                    sb.append(toString(list.get(i), trim));
        //                }
        //            }
        //        } else {
        //            int i = 0;
        //            if (isEmpty(delimiter)) {
        //                for (final Object e : c) {
        //                    if (i++ >= fromIndex) {
        //                        sb.append(toString(e, trim));
        //                    }
        //
        //                    if (i >= toIndex) {
        //                        break;
        //                    }
        //                }
        //            } else {
        //                for (final Object e : c) {
        //                    if (i++ > fromIndex) {
        //                        sb.append(delimiter);
        //                    }
        //
        //                    if (i > fromIndex) {
        //                        sb.append(toString(e, trim));
        //                    }
        //
        //                    if (i >= toIndex) {
        //                        break;
        //                    }
        //                }
        //            }
        //        }
        //
        //        if (isNotEmpty(suffix)) {
        //            sb.append(suffix);
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        final int len = toIndex - fromIndex;
        final String[] elements = new String[len];

        if (c instanceof final List<?> list && c instanceof RandomAccess) {

            for (int i = fromIndex, j = 0; i < toIndex; i++, j++) {
                elements[j] = toString(list.get(i), trim);
            }
        } else {
            int i = 0, j = 0;

            for (final Object e : c) {
                if (i++ >= fromIndex) {
                    elements[j++] = toString(e, trim);
                }

                if (i >= toIndex) {
                    break;
                }
            }
        }

        elements[0] = concat(prefix, elements[0]);
        elements[len - 1] = concat(elements[len - 1], suffix);

        return String.join(nullToEmpty(delimiter), elements);
    }

    /**
     * Joins the elements of the provided Iterator into a single String.
     *
     * @param iter
     * @return The concatenated string. Returns an empty string if the specified Iterator is {@code null} or empty
     * @see #join(Iterator, String, String, String, boolean)
     */
    public static String join(final Iterator<?> iter) {
        return join(iter, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the elements of the provided Iterator into a single String.
     *
     * @param iter
     * @param delimiter The delimiter that separates each element.
     * @return The concatenated string. Returns an empty string if the specified Iterator is {@code null} or empty
     * @see #join(Iterator, String, String, String, boolean)
     */
    public static String join(final Iterator<?> iter, final char delimiter) {
        if (iter == null) {
            return EMPTY;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder();
        //
        //    try {
        //        if (iter.hasNext()) {
        //            sb.append(N.toString(iter.next()));
        //        }
        //
        //        while (iter.hasNext()) {
        //            sb.append(delimiter);
        //
        //            sb.append(N.toString(iter.next()));
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        return join(iter, N.stringOf(delimiter));
    }

    /**
     * Joins the elements of the provided Iterator into a single String.
     *
     * @param iter
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if the specified Iterator is {@code null} or empty
     * @see #join(Iterator, String, String, String, boolean)
     */
    public static String join(final Iterator<?> iter, final String delimiter) {
        return join(iter, delimiter, EMPTY, EMPTY, false);
    }

    /**
     * Joins the elements of the provided Iterator into a single String.
     *
     * @param iter The Iterator containing the elements to join together. It can be {@code null}.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @return The concatenated string. Returns an empty string if the specified Iterator is {@code null} or empty and <i>prefix, suffix</i> are empty.
     * @see #join(Iterator, String, String, String, boolean)
     */
    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix) {
        return join(iter, delimiter, prefix, suffix, false);
    }

    /**
     * Joins the elements of the provided Iterator into a single String.
     *
     * @param iter The Iterator containing the elements to join together. It can be {@code null}.
     * @param delimiter The delimiter that separates each element. It can be empty, in which case the elements are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if the specified Iterator is {@code null} or empty and <i>prefix, suffix</i> are empty.
     */
    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        if (iter == null) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        }

        //    final StringBuilder sb = Objectory.createStringBuilder();
        //
        //    try {
        //        if (isNotEmpty(prefix)) {
        //            sb.append(prefix);
        //        }
        //
        //        if (isEmpty(delimiter)) {
        //            while (iter.hasNext()) {
        //                if (trim) {
        //                    sb.append(N.toString(iter.next()).trim());
        //                } else {
        //                    sb.append(N.toString(iter.next()));
        //                }
        //            }
        //        } else {
        //            if (iter.hasNext()) {
        //                sb.append(N.toString(iter.next()));
        //            }
        //
        //            while (iter.hasNext()) {
        //                sb.append(delimiter);
        //
        //                if (trim) {
        //                    sb.append(N.toString(iter.next()).trim());
        //                } else {
        //                    sb.append(N.toString(iter.next()));
        //                }
        //            }
        //        }
        //
        //        if (isNotEmpty(suffix)) {
        //            sb.append(suffix);
        //        }
        //
        //        return sb.toString();
        //    } finally {
        //        Objectory.recycle(sb);
        //    }

        final List<?> list = N.toList(iter);

        return join(list, 0, list.size(), delimiter, prefix, suffix, trim);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m) {
        return joinEntries(m, Strings.ELEMENT_SEPARATOR);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @param entryDelimiter The delimiter that separates each entry
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @param entryDelimiter The delimiter that separates each entry
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @param entryDelimiter The delimiter that separates each entry
     * @param keyValueDelimiter
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter, final char keyValueDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param keyValueDelimiter
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     *
     * @param m
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param keyValueDelimiter
     * @param prefix
     * @param suffix
     * @return
     * @see #joinEntries(Map, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, false);
    }

    /**
     * Joins the entries of the provided Map into a single String.
     * The <i>entryDelimiter</i> separates each entry and <i>keyValueDelimiter</i> separates the key and value within each entry.
     * The <i>prefix</i> and <i>suffix</i> are added to each entry before joining.
     *
     * @param m The Map containing the entries to join together. It can be empty.
     * @param entryDelimiter The delimiter that separates each entry. It can be empty, in which case the entries are concatenated without any delimiter.
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, trims the string representations of each element.
     * @return The concatenated string. Returns an empty string if 'm' is {@code null} or empty and <i>prefix'/'suffix</i> are empty.
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix,
            final boolean trim) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, trim);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @param trim
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD._EQUAL, trim);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @param trim
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD.EQUAL, null, null, trim);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @param keyValueDelimiter
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, false);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter The delimiter that separates each entry
     * @param keyValueDelimiter
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isEmpty(m) || fromIndex == toIndex) {
            return EMPTY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(toIndex - fromIndex, 32));

        try {
            int i = 0;

            for (final Map.Entry<?, ?> entry : m.entrySet()) {
                if (i++ > fromIndex) {
                    sb.append(entryDelimiter);
                }

                if (i > fromIndex) {
                    sb.append(toString(entry.getKey(), trim));
                    sb.append(keyValueDelimiter);
                    sb.append(toString(entry.getValue(), trim));
                }

                if (i >= toIndex) {
                    break;
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param keyValueDelimiter
     * @return
     * @see #joinEntries(Map, int, int, String, String, String, String, boolean)
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, false);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     * Each entry is represented as "key + keyValueDelimiter + value".
     * The entries are joined with the specified entryDelimiter.
     *
     * @param m The Map containing the entries to join together. It can be empty.
     * @param fromIndex The start index in the entry set from which to start joining entries. It should be non-negative and no larger than the size of the map.
     * @param toIndex The end index in the entry set up to which to join entries. It should be non-negative, no larger than the size of the map, and not less than fromIndex.
     * @param entryDelimiter The delimiter that separates each entry. It can be empty, in which case the entries are concatenated without any delimiter.
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param trim If {@code true}, leading and trailing whitespace of each entry will be removed.
     * @return The concatenated string. Returns an empty string if 'm' is {@code null} or empty and <i>prefix'/'suffix</i> are empty.
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
            final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, null, null, trim);
    }

    /**     
     * Joins the entries of the provided map into a string, using the specified delimiters.
     * Each entry is represented as "key + keyValueDelimiter + value".
     * The entries are joined with the specified entryDelimiter.
     *
     * @param m The Map containing the entries to join together. It can be empty.
     * @param fromIndex The start index in the entry set from which to start joining entries. It should be non-negative and no larger than the size of the map.
     * @param toIndex The end index in the entry set up to which to join entries. It should be non-negative, no larger than the size of the map, and not less than fromIndex.
     * @param entryDelimiter The delimiter that separates each entry. It can be empty, in which case the entries are concatenated without any delimiter.
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @return The concatenated string. Returns an empty string if 'm' is {@code null} or empty and <i>prefix'/'suffix</i> are empty.
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
            final String prefix, final String suffix) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, prefix, suffix, false);
    }

    /**
     * Joins the entries of the provided map into a string, using the specified delimiters.
     * The entries are taken from the specified range of the map's entry set (fromIndex, inclusive, to toIndex, exclusive).
     * Each entry is represented as "key + keyValueDelimiter + value".
     * The entries are joined with the specified entryDelimiter.
     * If the trim flag is set to {@code true}, the leading and trailing whitespace of each entry will be removed.
     *
     * @param m The Map containing the entries to join together. It can be empty.
     * @param fromIndex The start index in the entry set from which to start joining entries. It should be non-negative and no larger than the size of the map.
     * @param toIndex The end index in the entry set up to which to join entries. It should be non-negative, no larger than the size of the map, and not less than fromIndex.
     * @param entryDelimiter The delimiter that separates each entry. It can be empty, in which case the entries are concatenated without any delimiter.
     * @param keyValueDelimiter The delimiter that separates the key and value within each entry. It can be empty, in which case the key and value are concatenated without any delimiter.
     * @param prefix The prefix to be added at the beginning. It can be empty.
     * @param suffix The suffix to be added at the end. It can be empty.
     * @param trim If {@code true}, leading and trailing whitespace of each entry will be removed.
     * @return The concatenated string. Returns an empty string if 'm' is {@code null} or empty and <i>prefix'/'suffix</i> are empty.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range.
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
            final String prefix, final String suffix, final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isEmpty(m) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder(
                calculateBufferSize(toIndex - fromIndex, 32 + N.len(entryDelimiter) + N.len(keyValueDelimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            int i = 0;

            for (final Map.Entry<?, ?> entry : m.entrySet()) {
                if (i++ > fromIndex) {
                    sb.append(entryDelimiter);
                }

                if (i > fromIndex) {
                    if (trim) {
                        sb.append(N.toString(entry.getKey()).trim());
                        sb.append(keyValueDelimiter);
                        sb.append(N.toString(entry.getValue()).trim());
                    } else {
                        sb.append(N.toString(entry.getKey()));
                        sb.append(keyValueDelimiter);
                        sb.append(N.toString(entry.getValue()));
                    }
                }

                if (i >= toIndex) {
                    break;
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Joins the entries of the provided Iterable into a single String using the specified delimiters.
     * This method is designed to handle collections of key-value pairs or map entries.
     *
     * @param <T> the type of elements in the iterable
     * @param c the iterable whose elements are to be joined
     * @param entryDelimiter the delimiter to use between entries
     * @param keyValueDelimiter the delimiter to use between keys and values
     * @param keyExtractor function to extract keys from elements
     * @param valueExtractor function to extract values from elements
     * @return a string representation of the iterable's elements
     * @throws IllegalArgumentException if any parameter is null
     */
    public static <T> String joinEntries(final Iterable<? extends T> c, final String entryDelimiter, final String keyValueDelimiter,
            final Function<? super T, ?> keyExtractor, final Function<? super T, ?> valueExtractor) throws IllegalArgumentException {
        return joinEntries(c, entryDelimiter, keyValueDelimiter, EMPTY, EMPTY, false, keyExtractor, valueExtractor);
    }

    /**
     * <p>Joins the elements of the provided {@code Iterable} into a single String using the specified delimiters.
     * This method is designed to handle collections of key-value pairs or map entries.</p>
     * 
     * <p>Each element in the iterable is converted to a string. The {@code keyValueDelimiter} is used 
     * to separate keys and values within each element (if applicable), while the {@code entryDelimiter} 
     * is used to separate the elements themselves.</p>
     * 
     * <p>The resulting string is surrounded by the {@code prefix} and {@code suffix}.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * List&lt;Map.Entry&lt;String, Integer&gt;&gt; entries = ...
     * String result = Strings.joinEntries(entries, ";", "=", "[", "]");
     * // result could be: "[key1=1;key2=2;key3=3]"
     * </pre>
     *
     * @param <T> the type of elements in the iterable
     * @param c the iterable whose elements are to be joined
     * @param entryDelimiter the delimiter to use between entries
     * @param keyValueDelimiter the delimiter to use between keys and values
     * @param prefix the string to place at the start of the result
     * @param suffix the string to place at the end of the result
     * @return a string representation of the iterable's elements
     * @throws IllegalArgumentException if any parameter is null
     * 
     * @see #join(Iterable)
     * @see #join(Iterable, String)
     * @see #joinEntries(Map, String, String)
     */
    public static <T> String joinEntries(final Iterable<? extends T> c, final String entryDelimiter, final String keyValueDelimiter, final String prefix,
            final String suffix, final boolean trim, final Function<? super T, ?> keyExtractor, final Function<? super T, ?> valueExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotNull(valueExtractor, cs.valueExtractor);

        if (N.isEmpty(c)) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return concat(prefix, suffix);
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(c instanceof Collection coll ? coll.size() : 16,
                32 + N.len(entryDelimiter) + N.len(keyValueDelimiter), N.len(prefix), N.len(suffix)));

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            int i = 0;

            for (T e : c) {
                if (i++ > 0) {
                    sb.append(entryDelimiter);
                }

                if (trim) {
                    sb.append(N.toString(keyExtractor.apply(e)).trim());
                    sb.append(keyValueDelimiter);
                    sb.append(N.toString(valueExtractor.apply(e)).trim());
                } else {
                    sb.append(N.toString(keyExtractor.apply(e)));
                    sb.append(keyValueDelimiter);
                    sb.append(N.toString(valueExtractor.apply(e)));
                }
            }

            if (isNotEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @return
     */
    public static String concat(final String a, final String b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? Strings.EMPTY : b;
        } else {
            return N.isEmpty(b) ? a : a.concat(b);
        }
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static String concat(final String a, final String b, final String c) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f), nullToEmpty(g));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @param h
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g,
            final String h) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f), nullToEmpty(g),
                nullToEmpty(h));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @param h
     * @param i
     * @return The concatenated string. Returns {@code ""} if all input Strings are {@code null} or empty..
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h,
            final String i) {
        return String.join(Strings.EMPTY, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f), nullToEmpty(g),
                nullToEmpty(h), nullToEmpty(i));
    }

    /**
     * Concatenates the given strings into a single string.
     * {@code Null} strings are converted to empty strings before concatenation.
     *
     * @param a
     * @return
     */
    public static String concat(final String... a) {
        final int len = N.len(a);

        switch (len) {
            case 0:
                return EMPTY;

            case 1:
                return nullToEmpty(a[0]);

            case 2:
                return concat(a[0], a[1]);

            case 3:
                return concat(a[0], a[1], a[2]);

            case 4:
                return concat(a[0], a[1], a[2], a[3]);

            case 5:
                return concat(a[0], a[1], a[2], a[3], a[4]);

            case 6:
                return concat(a[0], a[1], a[2], a[3], a[4], a[5]);

            case 7:
                return concat(a[0], a[1], a[2], a[3], a[4], a[5], a[6]);

            default: {
                final String[] b = N.copyThenReplaceAll(a, Fn.nullToEmpty());

                return String.join(Strings.EMPTY, b);
            }
        }
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @return
     */
    public static String concat(final Object a, final Object b) {
        return Strings.concat(N.toString(a), N.toString(b));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d, final Object e) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d, final Object e, final Object f) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d, final Object e, final Object f, final Object g) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @param f
     * @param g
     * @param h
     * @return
     * @see #concat(Object, Object)
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d, final Object e, final Object f, final Object g,
            final Object h) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g), N.toString(h));
    }

    /**
     * Concatenates the string representations of the provided objects into a single string.
     * {@code Null} objects are converted to empty "" strings before concatenation.
     *
     * @param a The first object to concatenate. It can be {@code null}.
     * @param b The second object to concatenate. It can be {@code null}.
     * @param c The third object to concatenate. It can be {@code null}.
     * @param d The fourth object to concatenate. It can be {@code null}.
     * @param e The fifth object to concatenate. It can be {@code null}.
     * @param f The sixth object to concatenate. It can be {@code null}.
     * @param g The seventh object to concatenate. It can be {@code null}.
     * @param h The eighth object to concatenate. It can be {@code null}.
     * @return The concatenated string. Returns {@code ""} if all input objects are {@code null} or empty.
     */
    public static String concat(final Object a, final Object b, final Object c, final Object d, final Object e, final Object f, final Object g, final Object h,
            final Object i) {
        return Strings.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g), N.toString(h),
                N.toString(i));
    }

    //    /**
    //     *
    //     * @param a
    //     * @return
    //     * @see #concat(Object, Object)
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SafeVarargs
    //    public static String concat(final Object... a) {
    //        if (N.isEmpty(a)) {
    //            return EMPTY_STRING;
    //        } else if (a.getClass().equals(String[].class)) {
    //            return Strings.concat((String[]) a);
    //        }
    //
    //        final StringBuilder sb = ObjectFactory.createStringBuilder();
    //
    //        try {
    //            for (Object e : a) {
    //                sb.append(N.toString(e));
    //            }
    //            return sb.toString();
    //        } finally {
    //            ObjectFactory.recycle(sb);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param c
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public static String concat(final Collection<?> c) {
    //        if (N.isEmpty(c)) {
    //            return EMPTY_STRING;
    //        }
    //
    //        final StringBuilder sb = ObjectFactory.createStringBuilder();
    //
    //        try {
    //            for (Object e : c) {
    //                sb.append(N.toString(e));
    //            }
    //            return sb.toString();
    //        } finally {
    //            ObjectFactory.recycle(sb);
    //        }
    //    }

    private static String toString(final Object e, final boolean trim) {
        if (e == null) {
            return NULL;
        }

        if (trim) {
            return N.toString(e).trim();
        } else {
            return N.toString(e);
        }
    }

    /**
     * Copied from Google Guava
     *
     * <br />
     *
     * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced by
     * the corresponding argument value from {@code args}; or, if the placeholder and argument counts
     * do not match, returns a best-effort form of that string. Will not throw an exception under
     * normal conditions.
     *
     * <p><b>Note:</b> For most string-formatting needs, use {@link String#format String.format},
     * {@link java.io.PrintWriter#format PrintWriter.format}, and related methods. These support the
     * full range of <a
     * href="https://docs.oracle.com/javase/9/docs/api/java/util/Formatter.html#syntax">format
     * specifiers</a>, and alert you to usage errors by throwing {@link
     * java.util.IllegalFormatException}.
     *
     * <p>In certain cases, such as outputting debugging information or constructing a message to be
     * used for another unchecked exception, an exception during string formatting would serve little
     * purpose except to supplant the real information you were trying to provide. These are the cases
     * this method is made for; it instead generates a best-effort string with all supplied argument
     * values present. This method is also useful in environments such as GWT where {@code
     * String.format} is not available. As an example, method implementations of the {@code Preconditions} class use this formatter, for both of the reasons just discussed.
     *
     * <p><b>Warning:</b> Only the exact two-character placeholder sequence {@code "%s"} is
     * recognized.
     *
     * @param template a string containing zero or more {@code "%s"} placeholder sequences. {@code
     *     null} is treated as the four-character string {@code "null"}.
     * @param args the arguments to be substituted into the message template. The first argument
     *     specified is substituted for the first occurrence of {@code "%s"} in the template, and so
     *     forth. A {@code null} argument is converted to the four-character string {@code "null"};
     *     {@code non-null} values are converted to strings using {@link Object#toString()}.
     * @return
     */
    // TODO(diamondm) consider using Arrays.toString() for array parameters
    public static String lenientFormat(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (args == null) {
            args = new Object[] { "(Object[])null" };
        } else {
            for (int i = 0; i < args.length; i++) {
                args[i] = lenientToString(args[i]);
            }
        }

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;
        while (i < args.length) {
            final int placeholderStart = template.indexOf("%s", templateStart);
            if (placeholderStart == -1) {
                break;
            }
            sb.append(template, templateStart, placeholderStart);
            sb.append(args[i++]);
            templateStart = placeholderStart + 2;
        }
        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(": [");
            sb.append(args[i++]);
            while (i < args.length) {
                sb.append(", ");
                sb.append(args[i++]);
            }
            sb.append(']');
        }

        final String result = sb.toString();
        Objectory.recycle(sb);
        return result;
    }

    /**
     * Lenient to string.
     *
     * @param obj
     * @return
     */
    private static String lenientToString(final Object obj) {
        try {
            return String.valueOf(obj);
        } catch (final Exception e) {
            // Default toString() behavior - see Object.toString()
            final String objectToString = obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
            // Logger is created inline with fixed name to avoid forcing Proguard to create another class.
            // Logger.getLogger("com.google.common.base.Strings").log(WARNING, "Exception during lenientFormat for " + objectToString, e);

            LOGGER.warn("Exception during lenientFormat for " + objectToString, e); //NOSONAR

            return "<" + objectToString + " threw " + e.getClass().getName() + ">";
        }
    }

    /**
     * Reverses the characters in the given string.
     *
     * @param str The string to be reversed. May be {@code null} or empty.
     * @return A new string with the characters reversed. If the input string is {@code null} or empty or its length <= 1, the input string is returned.
     */
    public static String reverse(final String str) {
        if (N.len(str) <= 1) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder(str.length());

        try {
            sb.append(str);

            return sb.reverse().toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * <p>
     * Reverses a String delimited by a specific character.
     * </p>
     *
     * <p>
     * The Strings between the delimiters are not reversed. Thus
     * java.lang.String becomes String.lang.java (if the delimiter is
     * {@code '.'}).
     * </p>
     *
     * <pre>
     * Strings.reverseDelimited(null, *)      = null
     * Strings.reverseDelimited("", *)        = ""
     * Strings.reverseDelimited("a.b.c", 'x') = "a.b.c"
     * Strings.reverseDelimited("a.b.c", ".") = "c.b.a"
     * </pre>
     *
     * @param str
     *            the String to reverse, which may be null
     * @param delimiter
     *            the delimiter character to use
     * @return the specified String if it's {@code null} or empty. If the input string is {@code null} or empty or its length <= 1, the input string is returned.
     */
    public static String reverseDelimited(final String str, final char delimiter) {
        if (N.len(str) <= 1) {
            return str;
        }

        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        final String[] strs = split(str, delimiter);

        N.reverse(strs);

        return join(strs, delimiter);
    }

    /**
     * Reverses the order of delimited elements in a string.
     *
     * @param str The string to be reversed. May be {@code null} or empty.
     * @param delimiter The delimiter that separates the elements in the string.
     * @return The reversed string. If the input string is {@code null} or empty or its length <= 1, the input string is returned.
     */
    public static String reverseDelimited(final String str, final String delimiter) {
        if (N.len(str) <= 1) {
            return str;
        }

        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        final String[] strs = split(str, delimiter);

        N.reverse(strs);

        return join(strs, delimiter);
    }

    /**
     * Returns a new sorted String if the specified {@code str} is not {@code null} or empty, otherwise the specified {@code str} is returned.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty. If the input string is {@code null} or empty or its length <= 1, the input string is returned.
     */
    public static String sort(final String str) {
        if (N.len(str) <= 1) {
            return str;
        }

        final char[] chs = str.toCharArray();
        N.sort(chs);
        return String.valueOf(chs);
    }

    // Rotating (circular shift)
    //-----------------------------------------------------------------------

    /**
     * <p>Rotate (circular shift) a String of {@code shift} characters.</p>
     * <ul>
     *  <li>If {@code shift > 0}, right circular shift (ex : ABCDEF =&gt; FABCDE)</li>
     *  <li>If {@code shift < 0}, left circular shift (ex : ABCDEF =&gt; BCDEFA)</li>
     * </ul>
     *
     * <pre>
     * Strings.rotate(null, *)        = null
     * Strings.rotate("", *)          = ""
     * Strings.rotate("abcdefg", 0)   = "abcdefg"
     * Strings.rotate("abcdefg", 2)   = "fgabcde"
     * Strings.rotate("abcdefg", -2)  = "cdefgab"
     * Strings.rotate("abcdefg", 7)   = "abcdefg"
     * Strings.rotate("abcdefg", -7)  = "abcdefg"
     * Strings.rotate("abcdefg", 9)   = "fgabcde"
     * Strings.rotate("abcdefg", -9)  = "cdefgab"
     * </pre>
     *
     * @param str the String to rotate, which may be null
     * @param shift number of time to shift (positive : right shift, negative : left shift)
     * @return the rotated String,
     *          or the original String if {@code shift == 0},
     *          or {@code null} if {@code null} String input
     */
    public static String rotate(final String str, final int shift) {
        final int strLen = N.len(str);

        if (strLen <= 1 || shift == 0 || shift % strLen == 0) {
            return str;
        }

        int offset = -(shift % strLen);

        if (offset < 0) {
            offset = str.length() + offset;
        }

        if (offset < 0) {
            offset = 0;
        }

        return substring(str, offset) + Strings.substring(str, 0, offset);
    }

    /**
     * Shuffles the characters in the given string.
     *
     * @param str The string to be shuffled. May be {@code null} or empty.
     * @return A new string with the characters shuffled. If the input string is {@code null} or empty, the input string is returned.
     */
    public static String shuffle(final String str) {
        return shuffle(str, N.RAND);
    }

    /**
     * Shuffles the characters in the given string using the provided Random instance.
     *
     * @param str The string to be shuffled. May be {@code null} or empty.
     * @param rnd The Random instance used to shuffle the characters.
     * @return A new string with the characters shuffled. If the input string is {@code null} or empty, the input string is returned.
     */
    public static String shuffle(final String str, final Random rnd) {
        final int strLen = N.len(str);

        if (strLen <= 1) {
            return str;
        }

        final char[] chars = str.toCharArray();

        N.shuffle(chars, rnd);

        return String.valueOf(chars);
    }

    // Overlay
    //-----------------------------------------------------------------------

    /**
     * <p>Overlays part of a String with another String.</p>
     *
     * <pre>
     * Strings.overlay(null, "abc", 0, 0)          = "abc"
     * Strings.overlay("", "abc", 0, 0)          = "abc"
     * Strings.overlay("abcdef", {@code null}, 2, 4)     = "abef"
     * Strings.overlay("abcdef", "", 2, 4)       = "abef"
     * Strings.overlay("abcdef", "zzzz", 2, 4)   = "abzzzzef"
     * </pre>
     *
     * @param str the String to do overlaying in, which may be null
     * @param overlay the String to overlay, which may be null
     * @param start the position to start overlaying at
     * @param end the position to stop overlaying before
     * @return overlayed String, {@code ""} if {@code null} String input
     * @throws IndexOutOfBoundsException
     * @see #replace(String, int, int, String)
     * @see N#replaceRange(String, int, int, String)
     * @deprecated replaced by {@code replace(String, int, int, String)}
     */
    @Deprecated
    public static String overlay(final String str, final String overlay, final int start, final int end) throws IndexOutOfBoundsException {
        return replace(str, start, end, overlay);
    }

    /**
     * Parses the string argument as a boolean.
     * The boolean returned represents a {@code true} value if the string argument is not {@code null} and is equal, ignoring case, to the string "true".
     *
     * @param str The string to be parsed. May be {@code null}.
     * @return The boolean represented by the string argument.
     * @see Boolean#parseBoolean(String)
     */
    public static boolean parseBoolean(final String str) {
        return !Strings.isEmpty(str) && Boolean.parseBoolean(str);
    }

    /**
     * Parses the string argument as a char.
     * If the specified String is {@code null} or empty, Returns the char represented by the string argument is {@code '\0'}.
     * Else if the length of the specified String is 1, it returns the char represented by the string argument.
     * Otherwise, it returns the char represented by the integer value of the string argument.
     *
     * @param str The string to be parsed. May be {@code null}.
     * @return The char represented by the string argument.
     */
    public static char parseChar(final String str) {
        return Strings.isEmpty(str) ? CHAR_ZERO : ((str.length() == 1) ? str.charAt(0) : (char) Integer.parseInt(str));
    }

    /**
     * Returns the value by calling {@code Byte.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code byte} is
     * returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code byte}.
     * @see Numbers#toByte(String)
     * @deprecated replaced by {@code Numbers.toByte(String)}
     */
    @Deprecated
    public static byte parseByte(final String str) throws NumberFormatException {
        return Numbers.toByte(str);
    }

    /**
     * Returns the value by calling {@code Short.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code short} is
     * returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code short}.
     * @see Numbers#toShort(String)
     * @deprecated replaced by {@code Numbers.toShort(String)}
     */
    @Deprecated
    public static short parseShort(final String str) throws NumberFormatException {
        return Numbers.toShort(str);
    }

    /**
     * Returns the value by calling {@code Integer.valueOf(String)} if
     * {@code str} is not {@code null}, otherwise, the default value 0 for
     * {@code int} is returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code int}.
     * @see Numbers#toInt(String)
     * @deprecated replaced by {@code Numbers.toInt(String)}
     */
    @Deprecated
    public static int parseInt(final String str) throws NumberFormatException {
        return Numbers.toInt(str);
    }

    /**
     * Returns the value by calling {@code Long.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code long} is
     * returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code long}.
     * @see Numbers#toLong(String)
     * @deprecated replaced by {@code Numbers.toLong(String)}
     */
    @Deprecated
    public static long parseLong(final String str) throws NumberFormatException {
        return Numbers.toLong(str);
    }

    /**
     * Returns the value by calling {@code Float.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0f for {@code float} is
     * returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code float}.
     * @see Numbers#toFloat(String)
     * @deprecated replaced by {@code Numbers.toFloat(String)}
     */
    @Deprecated
    public static float parseFloat(final String str) throws NumberFormatException {
        return Numbers.toFloat(str);
    }

    /**
     * Returns the value by calling {@code Double.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0d for {@code double} is
     * returned.
     *
     * @param str
     * @return
     * @throws NumberFormatException If the string is not a parsable {@code double}.
     * @see Numbers#toDouble(String)
     * @deprecated replaced by {@code Numbers.toDouble(String)}
     */
    @Deprecated
    public static double parseDouble(final String str) throws NumberFormatException {
        return Numbers.toDouble(str);
    }

    /**
     * Encodes the given binary data into a Base64 encoded string.
     *
     * @param binaryData The byte array to be encoded.
     * @return The Base64 encoded string, or an empty String {@code ""} if the input byte array is {@code null} or empty.
     */
    public static String base64Encode(final byte[] binaryData) {
        if (N.isEmpty(binaryData)) {
            return Strings.EMPTY;
        }

        return BASE64_ENCODER.encodeToString(binaryData);
    }

    /**
     * Encodes the given string into a Base64 encoded string.
     *
     * @param str The string to be encoded.
     * @return The Base64 encoded string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64EncodeString(final String str) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        return BASE64_ENCODER.encodeToString(str.getBytes()); // NOSONAR
    }

    /**
     * Encodes the given string into a Base64 encoded string using UTF-8 encoding.
     *
     * @param str The string to be encoded.
     * @return The Base64 encoded string, or an empty String {@code ""} if the input string is {@code null} or empty.
     * @see #base64EncodeString(String, Charset)
     */
    public static String base64EncodeUtf8String(final String str) {
        return base64EncodeString(str, Charsets.UTF_8);
    }

    /**
     * Encodes the given string to a Base64 encoded string using the specified charset.
     *
     * @param str The string to be encoded.
     * @param charset The charset to be used to encode the input string.
     * @return The Base64 encoded string.
     * @see String#getBytes(Charset)
     */
    public static String base64EncodeString(final String str, final Charset charset) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        return BASE64_ENCODER.encodeToString(str.getBytes(charset)); // NOSONAR
    }

    /**
     * Decodes the given Base64 encoded string to a byte array.
     *
     * @param base64String The Base64 encoded string to be decoded.
     * @return The decoded byte array, or an empty byte array if the input string is {@code null} or empty.
     */
    public static byte[] base64Decode(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return BASE64_DECODER.decode(base64String);
    }

    /**
     * Decodes the given Base64 encoded string to its original string representation.
     *
     * @param base64String The Base64 encoded string to be decoded.
     * @return The decoded string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64DecodeToString(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY;
        }

        return new String(base64Decode(base64String)); // NOSONAR
    }

    /**
     * Decodes the given Base64 URL encoded string to a UTF-8 string.
     *
     * @param base64String The Base64 URL encoded string to be decoded.
     * @return The decoded UTF-8 string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64DecodeToUtf8String(final String base64String) {
        return base64DecodeToString(base64String, Charsets.UTF_8);
    }

    /**
     * Decodes the given Base64 encoded string to a string using the specified charset.
     *
     * @param base64String The Base64 encoded string to be decoded.
     * @param charset The charset to be used to decode the resulting byte array.
     * @return The decoded string.
     * @see String#String(byte[], Charset)
     */
    public static String base64DecodeToString(final String base64String, final Charset charset) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY;
        }

        return new String(base64Decode(base64String), charset); // NOSONAR
    }

    /**
     * Encodes the given byte array to a Base64 URL encoded string.
     *
     * @param binaryData The byte array to be encoded.
     * @return The Base64 URL encoded string, or an empty String {@code ""} if the input byte array is {@code null} or empty.
     */
    public static String base64UrlEncode(final byte[] binaryData) {
        if (N.isEmpty(binaryData)) {
            return Strings.EMPTY;
        }

        return BASE64_URL_ENCODER.encodeToString(binaryData);
    }

    /**
     * Decodes the given Base64 URL encoded string to a byte array.
     *
     * @param base64String The Base64 URL encoded string to be decoded.
     * @return The decoded byte array, an empty byte array if the input string is {@code null} or empty.
     */
    public static byte[] base64UrlDecode(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return BASE64_URL_DECODER.decode(base64String);
    }

    /**
     * Decodes the given Base64 URL encoded string to a regular string.
     *
     * @param base64String The Base64 URL encoded string to be decoded.
     * @return The decoded string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64UrlDecodeToString(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY;
        }

        return new String(BASE64_URL_DECODER.decode(base64String)); // NOSONAR
    }

    /**
     * Decodes the given Base64 URL encoded string to a UTF-8 string.
     *
     * @param base64String The Base64 URL encoded string to be decoded.
     * @return The decoded UTF-8 string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64UrlDecodeToUtf8String(final String base64String) {
        return base64UrlDecodeToString(base64String, Charsets.UTF_8);
    }

    /**
     * Decodes the given Base64 URL encoded string to a string using the specified charset.
     *
     * @param base64String The Base64 URL encoded string to be decoded.
     * @param charset The charset to be used to decode the based decoded {@code bytes}
     * @return The decoded string, or an empty String {@code ""} if the input string is {@code null} or empty.
     */
    public static String base64UrlDecodeToString(final String base64String, final Charset charset) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY;
        }

        return new String(BASE64_URL_DECODER.decode(base64String), charset);
    }

    /**
     * Encodes the given parameters into a URL-encoded string.
     *
     * @param parameters The parameters to be URL-encoded.
     * @return The URL-encoded string representation of the parameters.
     * @see URLEncodedUtil#encode(Object)
     */
    public static String urlEncode(final Object parameters) {
        return URLEncodedUtil.encode(parameters);
    }

    /**
     * Encodes the given parameters into a URL-encoded string using the specified charset.
     *
     * @param parameters The parameters to be URL-encoded.
     * @param charset The charset to be used for encoding.
     * @return The URL-encoded string representation of the parameters.
     * @see URLEncodedUtil#encode(Object, Charset)
     */
    public static String urlEncode(final Object parameters, final Charset charset) {
        return URLEncodedUtil.encode(parameters, charset);
    }

    /**
     * Decodes the given URL query string into a map of key-value pairs.
     *
     * @param urlQuery The URL query string to be decoded.
     * @return A map containing the decoded key-value pairs from the URL query string.
     * @see URLEncodedUtil#decode(String)
     */
    public static Map<String, String> urlDecode(final String urlQuery) {
        return URLEncodedUtil.decode(urlQuery);
    }

    /**
     * Decodes the given URL query string into a map of key-value pairs using the specified charset.
     *
     * @param urlQuery The URL query string to be decoded.
     * @param charset The charset to be used for decoding.
     * @return A map containing the decoded key-value pairs from the URL query string.
     * @see URLEncodedUtil#decode(String, Charset)
     */
    public static Map<String, String> urlDecode(final String urlQuery, final Charset charset) {
        return URLEncodedUtil.decode(urlQuery, charset);
    }

    /**
     * Decodes the given URL query string into an object of the specified type.
     *
     * @param <T> The type of the object to be returned.
     * @param urlQuery The URL query string to be decoded.
     * @param targetType The class of the object to be returned.
     * @return An object of the specified type containing the decoded data from the URL query string.
     * @see URLEncodedUtil#decode(String, Class)
     */
    public static <T> T urlDecode(final String urlQuery, final Class<? extends T> targetType) {
        return URLEncodedUtil.decode(urlQuery, targetType);
    }

    /**
     * Decodes a URL query string into an object of the specified type.
     * The query string is expected to be in <i>application/x-www-form-urlencoded</i> format.
     *
     * @param <T> The type of the object to be returned.
     * @param urlQuery The URL query string to be decoded.
     * @param charset The charset to be used for decoding.
     * @param targetType The class of the object to be returned.
     * @return An object of type T that represents the decoded URL query string.
     * @see URLEncodedUtil#decode(String, Charset, Class)
     */
    public static <T> T urlDecode(final String urlQuery, final Charset charset, final Class<? extends T> targetType) {
        return URLEncodedUtil.decode(urlQuery, charset, targetType);
    }

    /**
     * This array is a lookup table that translates Unicode characters drawn from the "Base64 Alphabet" (as specified
     * in Table 1 of RFC 2045) into their 6-bit positive integer equivalents. Characters that are not in the Base64
     * alphabet but fall within the bounds of the array are translated to -1.
     * <p>
     * Note: '+' and '-' both decode to 62. '/' and '_' both decode to 63. This means decoder seamlessly handles both
     * URL_SAFE and STANDARD base64. (The encoder, on the other hand, needs to know ahead of time what to emit).
     * </p>
     * <p>
     * Thanks to "commons" project in ws.apache.org for this code.
     * <a href="http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/">commons</a>
     * </p>
     */
    private static final byte[] DECODE_TABLE = {
            //   0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 00-0f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 10-1f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, 62, -1, 63, // 20-2f + - /
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, // 30-3f 0-9
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, // 40-4f A-O
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, // 50-5f P-Z _
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, // 60-6f a-o
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 // 70-7a p-z
    };

    /**
     * Byte used to pad output.
     */
    protected static final byte PAD_DEFAULT = '='; // Allow static access to default

    /**
     * Returns whether the {@code octet} is in the base 64 alphabet.
     *
     * @param octet
     *            The value to test
     * @return {@code true} if the value is defined in the base 64 alphabet, {@code false} otherwise.
     */
    public static boolean isBase64(final byte octet) {
        return octet == PAD_DEFAULT || (octet >= 0 && octet < DECODE_TABLE.length && DECODE_TABLE[octet] != -1);
    }

    /**
     * Tests a given byte array to see if it contains only valid characters within the Base64 alphabet. Currently the
     * method treats whitespace as valid.
     *
     * @param arrayOctet
     *            byte array to test
     * @return {@code true} if all bytes are valid characters in the Base64 alphabet or if the byte array is empty;
     *         {@code false}, otherwise
     */
    public static boolean isBase64(final byte[] arrayOctet) {
        for (final byte element : arrayOctet) {
            if (!isBase64(element) && !Character.isWhitespace(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a given String to see if it contains only valid characters within the Base64 alphabet. Currently the
     * method treats whitespace as valid.
     *
     * @param base64
     *            String to test
     * @return {@code true} if all characters in the String are valid characters in the Base64 alphabet or if
     *         the String is empty; {@code false}, otherwise
     */
    public static boolean isBase64(final String base64) {
        return isBase64(getBytes(base64, Charsets.DEFAULT));
    }

    /**
     * Searches for the first occurrence of an email address within the given CharSequence.
     *
     * This method uses a regular expression to find an email address in the input CharSequence.
     * If an email address is found, it is returned; otherwise, the method returns {@code null}.
     *
     * @param cs The CharSequence to be searched. It can be {@code null} or empty.
     * @return The first email address found in the CharSequence, or {@code null} if no email address is found.
     * @see #isValidEmailAddress(CharSequence)
     * @see #findAllEmailAddresses(CharSequence)
     */
    public static String findFirstEmailAddress(final CharSequence cs) {
        if (isEmpty(cs)) {
            return null;
        }

        final Matcher matcher = RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher(cs);

        // ^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
        // Matcher matcher = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(str);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Finds all the email addresses in the given CharSequence.
     *
     * This method uses a regular expression to find all occurrences of email addresses in the input CharSequence.
     * It returns a list of all found email addresses. If no email address is found, it returns an empty list.
     *
     * @param cs The CharSequence to be searched. It can be {@code null} or empty.
     * @return A list of all found email addresses, or an empty list if no email address is found.
     * @see #isValidEmailAddress(CharSequence)
     * @see #findFirstEmailAddress(CharSequence)
     */
    public static List<String> findAllEmailAddresses(final CharSequence cs) {
        if (isEmpty(cs)) {
            return new ArrayList<>();
        }

        final Matcher matcher = RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher(cs);

        final List<String> result = new ArrayList<>();

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    //    /**
    //     * Applies the given function to each element of the provided array of CharSequences.
    //     * The function should take a CharSequence as input and return a CharSequence.
    //     *
    //     * @param <T> The type of the elements in the array, which extends CharSequence.
    //     * @param a The array of CharSequences to which the function will be applied.
    //     * @param converter The function to apply to each element of the array.
    //     * @throws IllegalArgumentException if the converter function is {@code null}.
    //     * @see #copyThenTrim(String[])
    //     * @see #copyThenStrip(String[])
    //     * @see #copyThenSetAll(CharSequence[], Function)
    //     * @see N#setAll(Object[], java.util.function.IntFunction)
    //     * @see N#setAll(Object[], Throwables.IntObjFunction)
    //     * @see N#replaceAll(Object[], java.util.function.UnaryOperator)
    //     */
    //    @Beta
    //    public static <T extends CharSequence> void setAll(final T[] a, final Function<? super T, ? extends T> converter) throws IllegalArgumentException {
    //        N.checkArgNotNull(converter);
    //
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = converter.apply(a[i]);
    //        }
    //    }
    //
    //    /**
    //     * Creates a copy of the given array and sets all elements in the copy using the provided converter function.
    //     * If the specified array is {@code null}, returns {@code null}.
    //     * If the specified array is empty, returns itself.
    //     *
    //     * @param <T> The type of the elements in the array, which extends CharSequence.
    //     * @param a The array of CharSequences to which the function will be applied. May be {@code null}.
    //     * @param converter The function to apply to each element of the array.
    //     * @return A new array with the transformed elements. Returns {@code null} if the input array is {@code null}.
    //     * @see #copyThenTrim(String[])
    //     * @see #copyThenStrip(String[])
    //     * @see N#copyThenSetAll(Object[], java.util.function.IntFunction)
    //     * @see N#copyThenSetAll(Object[], Throwables.IntObjFunction)
    //     */
    //    @Beta
    //    @MayReturnNull
    //    public static <T extends CharSequence> T[] copyThenSetAll(final T[] a, final Function<? super T, ? extends T> converter) {
    //        N.checkArgNotNull(converter);
    //
    //        if (a == null) {
    //            return null; // NOSONAR
    //        } else if (a.length == 0) {
    //            return a.clone();
    //        }
    //
    //        final T[] copy = a.clone();
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            copy[i] = converter.apply(a[i]);
    //        }
    //
    //        return a;
    //    }

    /**
     * Creates a copy of the given array of strings and trims each string in the array.
     * Trimming a string removes any leading or trailing whitespace.
     *
     * @param strs The array of strings to be copied and trimmed. May be {@code null}.
     * @return A new array with the trimmed strings. Returns {@code null} if the input array is {@code null}.
     * @see N#copyThenReplaceAll(Object[], java.util.function.UnaryOperator)
     * @see Fn#trim()
     * @see Fn#trimToEmpty()
     * @see Fn#trimToNull()
     */
    @Beta
    @MayReturnNull
    public static String[] copyThenTrim(final String[] strs) {
        return N.copyThenReplaceAll(strs, Fn.trim());
    }

    /**
     * Creates a copy of the given array of strings and strips each string in the array.
     * Stripping a string removes any leading or trailing whitespace.
     *
     * @param strs The array of strings to be copied and stripped. May be {@code null}.
     * @return A new array with the stripped strings. Returns {@code null} if the input array is {@code null}.
     * @see N#copyThenReplaceAll(Object[], java.util.function.UnaryOperator)
     * @see Fn#strip()
     * @see Fn#stripToEmpty()
     * @see Fn#stripToNull()
     */
    @Beta
    @MayReturnNull
    public static String[] copyThenStrip(final String[] strs) {
        return N.copyThenReplaceAll(strs, Fn.strip());
    }

    //    /**
    //     * Formats the given value as a percentage string.
    //     *
    //     * @param value the value to be formatted as a percentage
    //     * @return the formatted percentage string
    //     * @see Numbers#format(double, String)
    //     * @deprecated replaced by {@link Numbers#format(Double, String)}.
    //     */
    //    public static String formatToPercentage(final double value) {
    //        return Numbers.round(value * 100, 2) + "%";
    //    }
    //
    //    /**
    //     * Formats the given value as a percentage string with the specified scale.
    //     *
    //     * @param value the value to be formatted as a percentage
    //     * @param scale the number of decimal places to include in the formatted percentage
    //     * @return the formatted percentage string
    //     * @see Numbers#format(double, String)
    //     * @deprecated replaced by {@link Numbers#format(Double, String)}.
    //     */
    //    public static String formatToPercentage(final double value, final int scale) {
    //        N.checkArgNotNegative(scale, cs.scale);
    //        switch (scale) {
    //            case 0:
    //                return Numbers.format(value, "0%");
    //            case 1:
    //                return Numbers.format(value, "0.0%");
    //            case 2:
    //                return Numbers.format(value, "0.00%");
    //            case 3:
    //                return Numbers.format(value, "0.000%");
    //            case 4:
    //                return Numbers.format(value, "0.0000%");
    //            case 5:
    //                return Numbers.format(value, "0.00000%");
    //            case 6:
    //                return Numbers.format(value, "0.000000%");
    //            default:
    //                return Numbers.format(value, Strings.concat("0." + Strings.repeat("0", scale)) + "%");
    //        }
    //    }

    /**
     * Extracts the first occurrence of an integer from the given string.
     *
     * @param str The string to extract the integer from. It can be {@code null} or empty.
     * @return The extracted integer as a string, or an empty string {@code ""} if no integer is found.
     * @see #replaceFirstInteger(String, String)
     * @see Numbers#extractFirstInt(String)
     * @see Numbers#extractFirstLong(String)
     */
    public static String extractFirstInteger(final String str) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        final Matcher matcher = RegExUtil.INTEGER_FINDER.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return Strings.EMPTY;
    }

    /**
     * Extracts the first occurrence of a double from the given string.
     *
     * @param str The string to extract the double from. It can be {@code null} or empty.
     * @return The extracted double as a string, or an empty string {@code ""} if no double is found.
     * @see #extractFirstInteger(String)
     * @see #replaceFirstDouble(String, String)
     * @see Numbers#extractFirstDouble(String)
     */
    public static String extractFirstDouble(final String str) {
        return extractFirstDouble(str, false);
    }

    /**
     * Extracts the first occurrence of a double from the given string.
     *
     * @param str The string to extract the double from. It can be {@code null} or empty.
     * @param includingCientificNumber If {@code true}, it will also include scientific numbers in the search.
     * @return The extracted double as a string, or an empty string {@code ""} if no double is found.
     * @see #extractFirstInteger(String)
     * @see #extractFirstDouble(String)
     * @see #replaceFirstDouble(String, String)
     * @see Numbers#extractFirstDouble(String, boolean)
     */
    public static String extractFirstDouble(final String str, final boolean includingCientificNumber) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        final Matcher matcher = (includingCientificNumber ? RegExUtil.SCIENTIFIC_NUMBER_FINDER : RegExUtil.NUMBER_FINDER).matcher(str);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return Strings.EMPTY;
    }

    /**
     * Replaces the first occurrences of integer in the given string with the specified replacement string.
     *
     * @param str The string to be modified. It can be {@code null} or empty.
     * @param replacement The string to replace the integer with.
     * @return The modified string with the first integer replaced by the specified replacement string.
     * @see #extractFirstInteger(String)
     */
    public static String replaceFirstInteger(final String str, final String replacement) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        return RegExUtil.INTEGER_FINDER.matcher(str).replaceFirst(replacement);
    }

    /**
     * Replaces the first occurrences of double in the given string with the specified replacement string.
     *
     * @param str The string to be modified. It can be {@code null} or empty.
     * @param replacement The string to replace the double with.
     * @return The modified string with the first double replaced by the specified replacement string.
     * @see #extractFirstDouble(String)
     */
    public static String replaceFirstDouble(final String str, final String replacement) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        return RegExUtil.NUMBER_FINDER.matcher(str).replaceFirst(replacement);
    }

    /**
     * Replaces the first occurrences of double in the given string with the specified replacement string.
     *
     * @param str The string to be modified. It can be {@code null} or empty.
     * @param replacement The string to replace the double with.
     * @param includingCientificNumber If {@code true}, it will also include scientific numbers in the search.
     * @return The modified string with the first double replaced by the specified replacement string.
     * @see #extractFirstDouble(String, boolean)
     */
    public static String replaceFirstDouble(final String str, final String replacement, final boolean includingCientificNumber) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY;
        }

        return (includingCientificNumber ? RegExUtil.SCIENTIFIC_NUMBER_FINDER : RegExUtil.NUMBER_FINDER).matcher(str).replaceFirst(replacement);
    }

    static void checkInputChars(final char[] chs, final String parameterName, final boolean canBeNullOrEmpty) {
        if (!canBeNullOrEmpty && N.isEmpty(chs)) {
            throw new IllegalArgumentException("Input char array or String parameter '" + parameterName + "' can't be null or empty");
        }

        for (final char ch : chs) {
            if (Character.isLowSurrogate(ch) || Character.isHighSurrogate(ch)) {
                throw new IllegalArgumentException("Element char in the input char array or String parameter '" + parameterName
                        + "' can't be low-surrogate or high-surrogate code unit. Please consider using String or String array instead if input parameter is char array");
            }
        }
    }

    static int calculateBufferSize(final int len, final int elementPlusDelimiterLen) {
        return len > Integer.MAX_VALUE / elementPlusDelimiterLen ? Integer.MAX_VALUE : len * elementPlusDelimiterLen;
    }

    static int calculateBufferSize(final int len, final int elementPlusDelimiterLen, final int prefixLen, final int suffixLen) {
        return len > (Integer.MAX_VALUE - prefixLen - suffixLen) / elementPlusDelimiterLen ? Integer.MAX_VALUE
                : len * elementPlusDelimiterLen + prefixLen + suffixLen;
    }

    public enum ExtractStrategy {
        /**
         * Default strategy used to extract substring between two delimiters.
         * <p>
         * <code>substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT) = ["a2[c", "a"]</code>.
         * </p>
         */
        DEFAULT,

        /**
         * Stack-based approach strategy used to extract substring between two delimiters.
         * <p>
         * <code>substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED) = ["c", "a2[c]", "a"]</code>.
         * </p>
         */
        STACK_BASED,

        /**
         * Stack-based approach strategy used to extract substring between two delimiters but nested substrings are ignored.
         * <p>
         * <code>substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED) = ["a2[c]", "a"]</code>.
         * </p>
         */
        IGNORE_NESTED
    }

    /**
     * @deprecated replaced by {@code Strings}
     */
    @Deprecated
    @Beta
    public static final class StringUtil extends Strings {
        private StringUtil() {
            // Utility class.
        }
    }

    public static final class StrUtil {
        private StrUtil() {
            // Utility class.
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @return
         * @see Strings#substring(String, int)
         */
        public static Optional<String> substring(final String str, final int inclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, int, int)
         */
        public static Optional<String> substring(final String str, final int inclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param funcOfExclusiveEndIndex
         * @return
         * @see Strings#substring(String, int, IntUnaryOperator)
         */
        public static Optional<String> substring(final String str, final int inclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex));
        }

        //        /**
        //         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //         *
        //         * @param str
        //         * @param inclusiveBeginIndex
        //         * @param funcOfExclusiveEndIndex
        //         * @return
        //         * @see #substring(String, int, int)
        //         */
        //        @Beta
        //        public static Optional<String> substring(final String str, final int inclusiveBeginIndex,
        //                final BiFunction<? super String, Integer, Integer> funcOfExclusiveEndIndex) {
        //            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex));
        //        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param funcOfInclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, IntUnaryOperator, int)
         */
        public static Optional<String> substring(final String str, final IntUnaryOperator funcOfInclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex));
        }

        //        /**
        //         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //         *
        //         * @param str
        //         * @param funcOfInclusiveBeginIndex
        //         * @param exclusiveEndIndex
        //         * @return
        //         * @see #substring(String, int, int)
        //         */
        //        @Beta
        //        public static Optional<String> substring(final String str, final BiFunction<? super String, Integer, Integer> funcOfInclusiveBeginIndex,
        //                final int exclusiveEndIndex) {
        //            return Optional.ofNullable(Strings.substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex));
        //        }

        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @return
        //     * @see Strings#substring(String, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex) {
        //        return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex));
        //    }
        //
        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @return
        //     * @see Strings#substring(String, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex) {
        //        return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex));
        //    }
        //
        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, int, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        //        return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        //    }
        //
        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, int, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        //        return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        //    }
        //
        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, char, int)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        //        return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex));
        //    }
        //
        //    /**
        //     * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, String, int)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        //        return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex));
        //    }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param defaultStr
         * @return
         * @see Strings#substringAfter(String, char)
         */
        @Beta
        public static String substringOrElse(final String str, final int inclusiveBeginIndex, final String defaultStr) {
            final String ret = Strings.substring(str, inclusiveBeginIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @param defaultStr
         * @return
         * @see Strings#substring(String, int, int)
         */
        @Beta
        public static String substringOrElse(final String str, final int inclusiveBeginIndex, final int exclusiveEndIndex, final String defaultStr) {
            final String ret = Strings.substring(str, inclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param funcOfExclusiveEndIndex
         * @param defaultStr
         * @return
         * @see Strings#substring(String, int, IntUnaryOperator)
         */
        @Beta
        public static String substringOrElse(final String str, final int inclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex,
                final String defaultStr) {
            final String ret = Strings.substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param funcOfInclusiveBeginIndex
         * @param exclusiveEndIndex
         * @param defaultStr
         * @return
         * @see Strings#substring(String, IntUnaryOperator, int)
         */
        @Beta
        public static String substringOrElse(final String str, final IntUnaryOperator funcOfInclusiveBeginIndex, final int exclusiveEndIndex,
                final String defaultStr) {
            final String ret = Strings.substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? defaultStr : ret;
        }

        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElse(String str, char delimiterOfInclusiveBeginIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElse(String str, String delimiterOfInclusiveBeginIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, int, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElse(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, int, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElse(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, char, int)
        //     * @deprecated
        //     */
        //    @Beta
        //    public static String substringOrElse(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code defaultStr}.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @param defaultStr
        //     * @return
        //     * @see Strings#substring(String, String, int)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElse(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex, final String defaultStr) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex);
        //
        //        return ret == null ? defaultStr : ret;
        //    }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, char)
         */
        @Beta
        public static String substringOrElseItself(final String str, final int inclusiveBeginIndex) {
            final String ret = Strings.substring(str, inclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, int, int)
         */
        @Beta
        public static String substringOrElseItself(final String str, final int inclusiveBeginIndex, final int exclusiveEndIndex) {
            final String ret = Strings.substring(str, inclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param funcOfExclusiveEndIndex
         * @return
         * @see Strings#substring(String, int, IntUnaryOperator)
         */
        @Beta
        public static String substringOrElseItself(final String str, final int inclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
            final String ret = Strings.substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param funcOfInclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, IntUnaryOperator, int)
         */
        @Beta
        public static String substringOrElseItself(final String str, final IntUnaryOperator funcOfInclusiveBeginIndex, final int exclusiveEndIndex) {
            final String ret = Strings.substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? str : ret;
        }

        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @return
        //     * @see Strings#substring(String, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, char delimiterOfInclusiveBeginIndex) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex);
        //
        //        return ret == null ? str : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @return
        //     * @see Strings#substring(String, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, String delimiterOfInclusiveBeginIndex) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex);
        //
        //        return ret == null ? str : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, int, char)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        //        final String ret = Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex);
        //
        //        return ret == null ? str : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param inclusiveBeginIndex
        //     * @param delimiterOfExclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, int, String)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        //        final String ret = Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex);
        //
        //        return ret == null ? str : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, char, int)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex);
        //
        //        return ret == null ? str : ret;
        //    }
        //
        //    /**
        //     * Returns the substring if it exists, otherwise returns {@code str} itself.
        //     *
        //     * @param str
        //     * @param delimiterOfInclusiveBeginIndex
        //     * @param exclusiveEndIndex
        //     * @return
        //     * @see Strings#substring(String, String, int)
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @Beta
        //    public static String substringOrElseItself(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        //        final String ret = Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex);
        //
        //        return ret == null ? str : ret;
        //    }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, char)
         */
        @Beta
        public static Optional<String> substringAfter(final String str, final char delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfter(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, String)
         */
        @Beta
        public static Optional<String> substringAfter(final String str, final String delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfter(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringAfter(String, String, int)
         */
        @Beta
        public static Optional<String> substringAfter(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringAfter(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static Optional<String> substringAfterLast(final String str, final char delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static Optional<String> substringAfterLast(final String str, final String delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringAfterLast(String, String, int)
         */
        @Beta
        public static Optional<String> substringAfterLast(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterAny(String, char[])
         */
        @Beta
        public static Optional<String> substringAfterAny(final String str, final char... delimitersOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterAny(str, delimitersOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterAny(String, String[])
         */
        @Beta
        public static Optional<String> substringAfterAny(final String str, final String... delimitersOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterAny(str, delimitersOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static Optional<String> substringBefore(final String str, final char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBefore(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static Optional<String> substringBefore(final String str, final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBefore(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, int, String)
         */
        @Beta
        public static Optional<String> substringBefore(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBefore(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static Optional<String> substringBeforeLast(final String str, final char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static Optional<String> substringBeforeLast(final String str, final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, int, String)
         */
        @Beta
        public static Optional<String> substringBeforeLast(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeLast(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeAny(String, char[])
         */
        @Beta
        public static Optional<String> substringBeforeAny(final String str, final char... delimitersOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeAny(str, delimitersOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeAny(String, String[])
         */
        @Beta
        public static Optional<String> substringBeforeAny(final String str, final String... delimitersOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeAny(str, delimitersOfExclusiveEndIndex));
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param defaultStr
         * @return
         * @see Strings#substringAfter(String, String)
         */
        @Beta
        public static String substringAfterOrElse(final String str, final String delimiterOfExclusiveBeginIndex, final String defaultStr) {
            final String ret = Strings.substringAfter(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param defaultStr
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static String substringAfterLastOrElse(final String str, final String delimiterOfExclusiveBeginIndex, final String defaultStr) {
            final String ret = Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @param defaultStr
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static String substringBeforeOrElse(final String str, final String delimiterOfExclusiveEndIndex, final String defaultStr) {
            final String ret = Strings.substringBefore(str, delimiterOfExclusiveEndIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code defaultStr}.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @param defaultStr
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static String substringBeforeLastOrElse(final String str, final String delimiterOfExclusiveEndIndex, final String defaultStr) {
            final String ret = Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex);

            return ret == null ? defaultStr : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, char)
         */
        @Beta
        public static String substringAfterOrElseItself(final String str, final char delimiterOfExclusiveBeginIndex) {
            final String ret = Strings.substringAfter(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, String)
         */
        @Beta
        public static String substringAfterOrElseItself(final String str, final String delimiterOfExclusiveBeginIndex) {
            final String ret = Strings.substringAfter(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringAfter(String, String)
         */
        @Beta
        public static String substringAfterOrElseItself(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            final String ret = Strings.substringAfter(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static String substringAfterLastOrElseItself(final String str, final char delimiterOfExclusiveBeginIndex) {
            final String ret = Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static String substringAfterLastOrElseItself(final String str, final String delimiterOfExclusiveBeginIndex) {
            final String ret = Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        @Beta
        public static String substringAfterLastOrElseItself(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            final String ret = Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static String substringBeforeOrElseItself(final String str, final char delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBefore(str, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static String substringBeforeOrElseItself(final String str, final String delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBefore(str, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        @Beta
        public static String substringBeforeOrElseItself(final String str, final int inclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBefore(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static String substringBeforeLastOrElseItself(final String str, final char delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static String substringBeforeLastOrElseItself(final String str, final String delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param exclusiveEndIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        @Beta
        public static String substringBeforeLastOrElseItself(final String str, final int exclusiveEndIndex, final String delimiterOfExclusiveEndIndex) {
            final String ret = Strings.substringBeforeLast(str, exclusiveEndIndex, delimiterOfExclusiveEndIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(final String str, final int exclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, int, char)
         */
        public static Optional<String> substringBetween(final String str, final int exclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, int, String)
         */
        public static Optional<String> substringBetween(final String str, final int exclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, char, int)
         */
        public static Optional<String> substringBetween(final String str, final char delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, String, int)
         */
        public static Optional<String> substringBetween(final String str, final String delimiterOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, char, char)
         */
        public static Optional<String> substringBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param tag
         * @return
         * @see #substringBetween(String, String, String)
         * @see #substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(final String str, final String tag) {
            return substringBetween(str, tag, tag);
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, String, String)
         */
        public static Optional<String> substringBetween(final String str, final String delimiterOfExclusiveBeginIndex,
                final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param fromIndex
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see #substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(final String str, final int fromIndex, final String delimiterOfExclusiveBeginIndex,
                final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, fromIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param funcOfExclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, int, IntUnaryOperator)
         */
        public static Optional<String> substringBetween(final String str, final int exclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex));
        }

        //        /**
        //         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //         *
        //         * @param str
        //         * @param exclusiveBeginIndex
        //         * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.apply(str, exclusiveBeginIndex) if inclusiveBeginIndex >= 0}
        //         * @return
        //         * @see #substringBetween(String, int, int)
        //         */
        //        @Beta
        //        public static Optional<String> substringBetween(final String str, final int exclusiveBeginIndex,
        //                final BiFunction<? super String, Integer, Integer> funcOfExclusiveEndIndex) {
        //            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex));
        //        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param funcOfExclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, IntUnaryOperator, int)
         */
        public static Optional<String> substringBetween(final String str, final IntUnaryOperator funcOfExclusiveBeginIndex, final int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, funcOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        //        /**
        //         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}
        //         *
        //         * @param str
        //         * @param funcOfExclusiveBeginIndex {@code exclusiveBeginIndex <- funcOfExclusiveBeginIndex.apply(str, exclusiveEndIndex)) if exclusiveEndIndex >= 0}
        //         * @param exclusiveEndIndex
        //         * @return
        //         * @see #substringBetween(String, int, int)
        //         */
        //        @Beta
        //        public static Optional<String> substringBetween(final String str, final BiFunction<? super String, Integer, Integer> funcOfExclusiveBeginIndex,
        //                final int exclusiveEndIndex) {
        //            return Optional.ofNullable(Strings.substringBetween(str, funcOfExclusiveBeginIndex, exclusiveEndIndex));
        //        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param funcOfExclusiveEndIndex
         * @return
         * @see #substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(final String str, final String delimiterOfExclusiveBeginIndex,
                final IntUnaryOperator funcOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, funcOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param funcOfExclusiveBeginIndex (exclusiveBeginIndex <- funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex))
         * @param delimiterOfExclusiveEndIndex (exclusiveEndIndex <- str.lastIndexOf(delimiterOfExclusiveEndIndex))
         * @return
         * @see #substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(final String str, final IntUnaryOperator funcOfExclusiveBeginIndex,
                final String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, funcOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns an empty {@code OptionalInt} if the specified string is blank or a invalid integer string. Otherwise returns {@code OptionalInt} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createInteger(String)
         */
        @Beta
        public static u.OptionalInt createInteger(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.OptionalInt.empty();
            }

            try {
                return u.OptionalInt.of(Numbers.createInteger(str));
            } catch (final NumberFormatException e) {
                return u.OptionalInt.empty();
            }
        }

        /**
         * Returns an empty {@code OptionalLong} if the specified string is blank or a invalid long string. Otherwise returns {@code OptionalLong} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createLong(String)
         */
        @Beta
        public static u.OptionalLong createLong(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.OptionalLong.empty();
            }

            try {
                return u.OptionalLong.of(Numbers.createLong(str));
            } catch (final NumberFormatException e) {
                return u.OptionalLong.empty();
            }
        }

        /**
         * Returns an empty {@code OptionalFloat} if the specified string is blank or a invalid float string. Otherwise returns {@code OptionalFloat} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createFloat(String)
         */
        @Beta
        public static u.OptionalFloat createFloat(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.OptionalFloat.empty();
            }

            try {
                return u.OptionalFloat.of(Numbers.createFloat(str));
            } catch (final NumberFormatException e) {
                return u.OptionalFloat.empty();
            }
        }

        /**
         * Returns an empty {@code OptionalDouble} if the specified string is blank or a invalid double string. Otherwise returns {@code OptionalDouble} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createDouble(String)
         */
        @Beta
        public static u.OptionalDouble createDouble(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.OptionalDouble.empty();
            }

            try {
                return u.OptionalDouble.of(Numbers.createDouble(str));
            } catch (final NumberFormatException e) {
                return u.OptionalDouble.empty();
            }
        }

        /**
         * Returns an empty {@code Optional<BigInteger>} if the specified string is blank or a invalid {@code BigInteger} string. Otherwise returns {@code Optional<BigInteger>} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createBigInteger(String)
         */
        @Beta
        public static u.Optional<BigInteger> createBigInteger(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createBigInteger(str));
            } catch (final NumberFormatException e) {
                return u.Optional.empty();
            }
        }

        /**
         * Returns an empty {@code Optional<BigDecimal>} if the specified string is blank or a invalid {@code BigDecimal} string. Otherwise returns {@code Optional<BigDecimal>} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createBigDecimal(String)
         */
        @Beta
        public static u.Optional<BigDecimal> createBigDecimal(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createBigDecimal(str));
            } catch (final NumberFormatException e) {
                return u.Optional.empty();
            }
        }

        /**
         * Returns an empty {@code Optional<Number>} if the specified string is blank or a invalid number string. Otherwise returns {@code Optional<Number>} with value converted from the specified String.
         *
         * @param str
         * @return
         * @see Numbers#createNumber(String)
         */
        @Beta
        public static u.Optional<Number> createNumber(final String str) {
            if (!Numbers.quickCheckForIsCreatable(str)) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createNumber(str));
            } catch (final NumberFormatException e) {
                return u.Optional.empty();
            }
        }
    }
}
