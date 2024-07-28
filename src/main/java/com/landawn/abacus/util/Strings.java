/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.Stream;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
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
 */
@SuppressWarnings({ "java:S1694" })
public abstract sealed class Strings permits Strings.StringUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Strings.class);

    /**
     * String with value {@code "null"}.
     */
    @Beta
    public static final String NULL_STRING = "null".intern();

    /**
     *
     * Char array with value {@code "['n', 'u', 'l', 'l']"}.
     */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /**
     * The empty String {@code ""}.
     */
    public static final String EMPTY_STRING = "".intern();

    //    /**
    //     * The empty String {@code ""}.
    //     * @deprecated Use {@link #EMPTY_STRING} instead
    //     */
    //    @Deprecated
    //    public static final String EMPTY = EMPTY_STRING;

    /**
     * A String for a space character.
     *
     * @since 3.2
     */
    public static final String SPACE = WD.SPACE;

    /**
     * A String for linefeed LF ("\n").
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 3.2
     */
    public static final String LF = "\n";

    /**
     * A String for carriage return CR ("\r").
     *
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 3.2
     */
    public static final String CR = "\r";

    @Beta
    public static final char CHAR_ZERO = (char) 0;
    @Beta
    public static final char CHAR_SPACE = SPACE.charAt(0);
    @Beta
    public static final char CHAR_LF = LF.charAt(0);
    @Beta
    public static final char CHAR_CR = CR.charAt(0);

    /**
     * Field COMMA_SPACE. (value is "", "")
     */
    public static final String COMMA_SPACE = ", ".intern();

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

    /**
     * A regex pattern for recognizing blocks of whitespace characters. The
     * apparent convolutedness of the pattern serves the purpose of ignoring
     * "blocks" consisting of only a single space: the pattern is used only to
     * normalize whitespace, condensing "blocks" down to a single space, thus
     * matching the same would likely cause a great many noop replacements.
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(?: |\\u00A0|\\s|[\\s&&[^ ]])\\s*");//NOSONAR

    private static final Map<Object, Splitter> splitterPool = new HashMap<>();

    private static final Map<Object, Splitter> trimSplitterPool = new HashMap<>();

    private static final Map<Object, Splitter> preserveSplitterPool = new HashMap<>();

    private static final Map<Object, Splitter> trimPreserveSplitterPool = new HashMap<>();

    static {
        final List<String> delimiters = Array.asList(" ", "  ", "   ", "\t", "\n", "\r", ",", ", ", ";", "; ", ":", ": ", " : ", "-", " - ", "_", " _ ", "#",
                "##", " # ", "=", "==", " = ", "|", " | ", "||", " || ", "&", "&&", "@", "@@", "$", "$$", "*", "**", "+", "++");

        for (String delimiter : delimiters) {
            splitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings());
            trimSplitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings().trimResults());
            preserveSplitterPool.put(delimiter, Splitter.with(delimiter));
            trimPreserveSplitterPool.put(delimiter, Splitter.with(delimiter).trimResults());

            if (delimiter.length() == 1) {
                char delimiterChar = delimiter.charAt(0);

                splitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings());
                trimSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings().trimResults());
                preserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar));
                trimPreserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).trimResults());
            }
        }
    }

    private static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*)$", Pattern.UNICODE_CHARACTER_CLASS);

    // https://www.baeldung.com/java-email-validation-regex
    // https://owasp.org/www-community/OWASP_Validation_Regex_Repository
    // https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
    private static final Pattern EMAIL_ADDRESS_RFC_5322_PATTERN = Pattern.compile(
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])", //NOSONAR
            Pattern.UNICODE_CHARACTER_CLASS);

    // https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url
    private static final Pattern URL_PATTERN = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)", //NOSONAR
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)", //NOSONAR
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Encoder BASE64_ENCODER = java.util.Base64.getEncoder();

    private static final Decoder BASE64_DECODER = java.util.Base64.getDecoder();

    private static final Encoder BASE64_URL_ENCODER = java.util.Base64.getUrlEncoder();

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
     * Returns a new UUID String  UUID.
     *
     * @return
     * @see UUID#randomUUID().
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the string representation of the {@code char} array or null.
     *
     * @param value the character array.
     * @return a String or null
     * @see String#valueOf(char[])
     * @see N#toString(Object)
     * @since 3.9
     */
    public static String valueOf(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static boolean isValidJavaIdentifier(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        return JAVA_IDENTIFIER_PATTERN.matcher(str).matches();
    }

    /**
     *
     *
     * @param str
     * @return
     * @see #findFirstEmailAddress(String)
     * @see #findAllEmailAddresses(String)
     */
    public static boolean isValidEmailAddress(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        return EMAIL_ADDRESS_RFC_5322_PATTERN.matcher(str).matches();
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static boolean isValidUrl(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        return URL_PATTERN.matcher(str).matches();
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static boolean isValidHttpUrl(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        return HTTP_URL_PATTERN.matcher(str).matches();
    }

    /**
     * Checks if the specified {@code CharSequence} is null or empty.
     *
     * @param cs
     * @return
     */
    public static boolean isEmpty(final CharSequence cs) {
        return (cs == null) || (cs.length() == 0);
    }

    /**
     * Checks if the specified {@code CharSequence} is null or blank.
     *
     *
     * @param cs
     * @return
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
     *
     * @param cs
     * @return
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return (cs != null) && (cs.length() > 0);
    }

    /**
     *
     * @param cs
     * @return
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
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isAllEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) && isEmpty(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean isAllEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) && isEmpty(b) && isEmpty(c);
    }

    /**
     * <p>Checks if all of the CharSequences are empty ("") or null.</p>
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
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null
     * @see Strings#isAllEmpty(CharSequence...)
     * @since 3.6
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
     *
     * @param css
     * @return
     * @see Strings#isAllEmpty(Collection)
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
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isAllBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) && isBlank(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean isAllBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) && isBlank(b) && isBlank(c);
    }

    /**
     * <p>Checks if all of the CharSequences are empty (""), null or whitespace only.</p>
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
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null or whitespace only
     * @see Strings#isAllBlank(CharSequence...)
     * @since 3.6
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
     *
     * @param css
     * @return
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
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isAnyEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) || isEmpty(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean isAnyEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) || isEmpty(b) || isEmpty(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or null.</p>
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
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null
     * @see Strings#isAnyEmpty(CharSequence...)
     * @since 3.2
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
     *
     * @param css
     * @return
     * @see Strings#isAnyEmpty(CharSequence...)
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
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isAnyBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) || isBlank(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean isAnyBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) || isBlank(b) || isBlank(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or null or whitespace only.</p>
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
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null or whitespace only
     * @see Strings#isAnyBlank(CharSequence...)
     * @since 3.2
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
     *
     * @param css
     * @return
     * @see Strings#isAnyBlank(Collection)
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
     *
     * @param <T>
     * @param a
     * @param b
     * @return the first value from specified parameters which is not empty, or {@code null} if there are no non-empty values
     * @see #firstNonEmpty(CharSequence...)
     */
    public static <T extends CharSequence> T firstNonEmpty(final T a, final T b) {
        return isEmpty(a) ? (isEmpty(b) ? null : b) : a;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return the first value from specified parameters which is not empty, or {@code null} if there are no non-empty values
     * @see #firstNonEmpty(CharSequence...)
     */
    public static <T extends CharSequence> T firstNonEmpty(final T a, final T b, final T c) {
        return isEmpty(a) ? (isEmpty(b) ? (isEmpty(c) ? null : c) : b) : a;
    }

    /**
     * <p>Returns the first value in the array which is not empty.</p>
     *
     * <p>If all values are empty or the array is {@code null}
     * or empty then {@code null} is returned.</p>
     *
     * <pre>
     * Strings.firstNonEmpty(null, null, null)   = null
     * Strings.firstNonEmpty(null, null, "")     = null
     * Strings.firstNonEmpty(null, "", " ")      = " "
     * Strings.firstNonEmpty("abc")              = "abc"
     * Strings.firstNonEmpty(null, "xyz")        = "xyz"
     * Strings.firstNonEmpty("", "xyz")          = "xyz"
     * Strings.firstNonEmpty(null, "xyz", "abc") = "xyz"
     * Strings.firstNonEmpty()                   = null
     * </pre>
     *
     * @param <T> the specific kind of CharSequence
     * @param css the values to test, may be {@code null} or empty
     * @return the first value from {@code css} which is not empty, or {@code null} if there is no non-empty value.
     * @see StrUtil#firstNonEmpty(CharSequence...)
     * @since 3.8
     */
    @MayReturnNull
    @SafeVarargs
    public static <T extends CharSequence> T firstNonEmpty(final T... css) {
        if (N.isEmpty(css)) {
            return null;
        }

        for (final T val : css) {
            if (isNotEmpty(val)) {
                return val;
            }
        }

        return null;
    }

    /**
     *
     * @param <T>
     * @param css
     * @return the first value from {@code css} which is not empty, or {@code null} if there is no non-empty value.
     * @see StrUtil#firstNonEmpty(CharSequence...)
     */
    @MayReturnNull
    public static <T extends CharSequence> T firstNonEmpty(final Iterable<? extends T> css) {
        if (N.isEmpty(css)) {
            return null;
        }

        for (final T val : css) {
            if (isNotEmpty(val)) {
                return val;
            }
        }

        return null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return the first value from specified parameters which is not blank, or {@code null} if there are no non-blank values
     * @see #firstNonBlank(CharSequence...)
     */
    public static <T extends CharSequence> T firstNonBlank(final T a, final T b) {
        return isBlank(a) ? (isBlank(b) ? null : b) : a;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return the first value from specified parameters which is not blank, or {@code null} if there are no non-blank values
     * @see #firstNonBlank(CharSequence...)
     */
    public static <T extends CharSequence> T firstNonBlank(final T a, final T b, final T c) {
        return isBlank(a) ? (isBlank(b) ? (isBlank(c) ? null : c) : b) : a;
    }

    /**
     *
     * @param <T>
     * @param css
     * @return the first value from {@code css} which is not empty, or {@code null} if there is no non-blank value.
     * @see StrUtil#firstNonBlank(CharSequence...)
     */
    @MayReturnNull
    @SafeVarargs
    public static <T extends CharSequence> T firstNonBlank(final T... css) {
        if (N.isEmpty(css)) {
            return null;
        }

        for (final T val : css) {
            if (isNotBlank(val)) {
                return val;
            }
        }

        return null;
    }

    /**
     *
     * @param <T>
     * @param css
     * @return the first value from {@code css} which is not empty, or {@code null} if there is no non-blank value.
     * @see StrUtil#firstNonEmpty(CharSequence...)
     */
    @MayReturnNull
    public static <T extends CharSequence> T firstNonBlank(final Iterable<? extends T> css) {
        if (N.isEmpty(css)) {
            return null;
        }

        for (final T val : css) {
            if (isNotBlank(val)) {
                return val;
            }
        }

        return null;
    }

    /**
     * Same as {@code N.defaultIfEmpty(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see Strings#defaultIfEmpty(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final T defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    /**
     *
     * @param <T>
     * @param str
     * @param getterForDefaultStr
     * @return
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final Supplier<? extends T> getterForDefaultStr) {
        if (isEmpty(str)) {
            return getterForDefaultStr.get();
        }

        return str;
    }

    /**
     * Same as {@code N.defaultIfBlank(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see Strings#defaultIfBlank(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final T defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     *
     * @param <T>
     * @param str
     * @param getterForDefaultStr
     * @return
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final Supplier<? extends T> getterForDefaultStr) {
        if (isBlank(str)) {
            return getterForDefaultStr.get();
        }

        return str;
    }

    /**
     * Converts the specified String to an empty String {@code ""} if it's {@code null}, otherwise just returns itself.
     *
     *
     * @param str
     * @return
     */
    public static String nullToEmpty(final String str) {
        return str == null ? EMPTY_STRING : str;
    }

    /**
     * Converts each {@code null} String element in the specified String array to an empty String {@code ""}.
     *
     *
     * @param strs
     * @see N#nullToEmpty(String[])
     * @see N#nullToEmptyForEach(String[])
     */
    public static void nullToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null ? EMPTY_STRING : strs[i];
        }
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String emptyToNull(final String str) {
        return str == null || str.length() == 0 ? null : str;
    }

    /**
     *
     *
     * @param strs
     */
    public static void emptyToNull(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null || strs[i].length() == 0 ? null : strs[i];
        }
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String blankToEmpty(final String str) {
        return isBlank(str) ? EMPTY_STRING : str;
    }

    /**
     *
     *
     * @param strs
     */
    public static void blankToEmpty(final String[] strs) {
        if (N.isEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = isBlank(strs[i]) ? EMPTY_STRING : strs[i];
        }
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String blankToNull(final String str) {
        return isBlank(str) ? null : str;
    }

    /**
     *
     *
     * @param strs
     */
    public static void blankToNull(final String[] strs) {
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
     * @param str the String to check, may be null
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String, {@code null} if null String input
     * @throws IllegalArgumentException if the width is too small
     * @since 2.0
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
     * @param str the String to check, may be null
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     * @since 2.0
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
     * Strings.abbreviate("abcdefg", null, *)  = "abcdefg"
     * Strings.abbreviate("abcdefg", ".", 5)   = "abcd."
     * Strings.abbreviate("abcdefg", ".", 7)   = "abcdefg"
     * Strings.abbreviate("abcdefg", ".", 8)   = "abcdefg"
     * Strings.abbreviate("abcdefg", "..", 4)  = "ab.."
     * Strings.abbreviate("abcdefg", "..", 3)  = "a.."
     * Strings.abbreviate("abcdefg", "..", 2)  = IllegalArgumentException
     * Strings.abbreviate("abcdefg", "...", 3) = IllegalArgumentException
     * </pre>
     *
     * @param str the String to check, may be null
     * @param abbrevMarker the String used as replacement marker
     * @param maxWidth maximum length of result String, must be at least {@code abbrevMarker.length + 1}
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     * @since 3.6
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
     * Strings.abbreviate("abcdefghijklmno", null, *, *)    = "abcdefghijklmno"
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
     * @param str the String to check, may be null
     * @param abbrevMarker the String used as replacement marker
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be at least 4
     * @return abbreviated String
     * @throws IllegalArgumentException if the width is too small
     * @since 3.6
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

        if (isNotEmpty(str) && EMPTY_STRING.equals(abbrevMarker) && maxWidth > 0) {
            return Strings.largestSubstring(str, 0, maxWidth);
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
     * <li>Neither the String for abbreviation nor the replacement String are null or empty </li>
     * <li>The length to truncate to is less than the length of the supplied String</li>
     * <li>The length to truncate to is greater than 0</li>
     * <li>The abbreviated String will have enough room for the length supplied replacement String
     * and the first and last characters of the supplied String for abbreviation</li>
     * </ul>
     * <p>Otherwise, the returned String will be the same as the supplied String for abbreviation.
     * </p>
     *
     * <pre>
     * Strings.abbreviateMiddle(null, null, 0)      = null
     * Strings.abbreviateMiddle("abc", null, 0)      = "abc"
     * Strings.abbreviateMiddle("abc", ".", 0)      = "abc"
     * Strings.abbreviateMiddle("abc", ".", 3)      = "abc"
     * Strings.abbreviateMiddle("abcdef", ".", 4)     = "ab.f"
     * </pre>
     *
     * @param str the String to abbreviate, may be null
     * @param middle the String to replace the middle characters with, may be null
     * @param length the length to abbreviate {@code str} to.
     * @return the abbreviated String if the above criteria is met, or the original String supplied for abbreviation.
     * @since 2.5
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
     * @param str the String to center, may be null
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
     * @param str the String to center, may be null
     * @param size the int size of new String.
     * @param padChar the character to pad the new String with
     * @return centered String
     * @throws IllegalArgumentException
     * @since 2.0
     */
    public static String center(String str, final int size, final char padChar) throws IllegalArgumentException {
        N.checkArgNotNegative(size, "size");

        if (str == null) {
            str = EMPTY_STRING;
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
     * @param str the String to center, may be null
     * @param minLength the minimum size of new String.
     * @param padStr the String to pad the new String with, must not be null or empty
     * @return centered String
     * @throws IllegalArgumentException
     */
    public static String center(String str, final int minLength, String padStr) throws IllegalArgumentException {
        N.checkArgNotNegative(minLength, "minLength");
        // N.checkArgNotEmpty(padStr, "padStr");

        if (str == null) {
            str = EMPTY_STRING;
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
     *
     * @param str
     * @param minLength
     * @return
     */
    public static String padStart(final String str, final int minLength) {
        return padStart(str, minLength, WD._SPACE);
    }

    /**
     *
     * @param str
     * @param minLength
     * @param padChar
     * @return the specified String if its length is bigger than {@code minLength}
     */
    @SuppressWarnings("deprecation")
    public static String padStart(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = EMPTY_STRING;
        }

        if (str.length() >= minLength) {
            return str;
        }

        int delta = minLength - str.length();
        final char[] chars = new char[minLength];

        N.fill(chars, 0, delta, padChar);

        str.getChars(0, str.length(), chars, delta);

        return InternalUtil.newString(chars, true);
    }

    /**
     *
     * @param str
     * @param minLength
     * @param padStr
     * @return the specified String if its length is bigger than {@code minLength}
     */
    public static String padStart(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = EMPTY_STRING;
        }

        if (str.length() >= minLength) {
            return str;
        }

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
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
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
     *
     * @param str
     * @param minLength
     * @return
     */
    public static String padEnd(final String str, final int minLength) {
        return padEnd(str, minLength, WD._SPACE);
    }

    /**
     *
     * @param str
     * @param minLength
     * @param padChar
     * @return the specified String if its length is bigger than {@code minLength}
     */
    @SuppressWarnings("deprecation")
    public static String padEnd(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = EMPTY_STRING;
        }

        if (str.length() >= minLength) {
            return str;
        }

        final char[] chars = new char[minLength];
        str.getChars(0, str.length(), chars, 0);

        N.fill(chars, str.length(), minLength, padChar);

        return InternalUtil.newString(chars, true);
    }

    /**
     *
     * @param str
     * @param minLength
     * @param padStr
     * @return the specified String if its length is bigger than {@code minLength}
     */
    public static String padEnd(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = EMPTY_STRING;
        }

        if (str.length() >= minLength) {
            return str;
        }

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
                StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(str);

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
     *
     *
     * @param ch
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public static String repeat(final char ch, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_STRING;
        } else if (n == 1) {
            return String.valueOf(ch);
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

        return String.valueOf(ch).repeat(n);
    }

    /**
     *
     * @param ch
     * @param n
     * @param delimiter
     * @return
     * @throws IllegalArgumentException
     */
    public static String repeat(final char ch, final int n, final char delimiter) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        return repeat(String.valueOf(ch), n, String.valueOf(delimiter));
    }

    /**
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public static String repeat(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        if (N.isEmpty(str)) {
            return EMPTY_STRING;
        }

        return str.repeat(n);
    }

    /**
     *
     * @param str
     * @param n
     * @param delimiter
     * @return
     * @throws IllegalArgumentException
     */
    public static String repeat(final String str, final int n, final String delimiter) throws IllegalArgumentException {
        if (N.isEmpty(delimiter)) {
            return repeat(str, n);
        }

        return repeat(str, n, delimiter, EMPTY_STRING, EMPTY_STRING);
    }

    /**
     *
     *
     * @param str
     * @param n
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("deprecation")
    public static String repeat(String str, final int n, String delimiter, String prefix, String suffix) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        str = str == null ? EMPTY_STRING : str;
        delimiter = delimiter == null ? EMPTY_STRING : delimiter;
        prefix = prefix == null ? EMPTY_STRING : prefix;
        suffix = suffix == null ? EMPTY_STRING : suffix;

        if (n == 0 || (isEmpty(str) && isEmpty(delimiter))) {
            return prefix + suffix;
        } else if (n == 1) {
            return prefix + str + suffix;
        }

        final int strLen = str.length();
        final int delimiterLen = delimiter.length();
        final int prefixLen = prefix.length();
        final int suffixLen = suffix.length();
        final int pieceLen = strLen + delimiterLen;

        if ((Integer.MAX_VALUE - prefixLen - suffixLen) / pieceLen < n) {
            throw new ArrayIndexOutOfBoundsException("Required array size too large: " + 1L * pieceLen * n);
        }

        final int size = pieceLen * n - delimiterLen + prefixLen + suffixLen;
        final char[] cbuf = new char[size];

        prefix.getChars(0, prefixLen, cbuf, 0);
        str.getChars(0, strLen, cbuf, prefixLen);
        delimiter.getChars(0, delimiterLen, cbuf, strLen + prefixLen);

        int filledLen = pieceLen;
        int lenToFill = size - (prefixLen + suffixLen);

        for (; filledLen <= lenToFill - filledLen; filledLen <<= 1) {
            N.copy(cbuf, prefixLen, cbuf, filledLen + prefixLen, filledLen);
        }

        if (filledLen < lenToFill) {
            N.copy(cbuf, prefixLen, cbuf, filledLen + prefixLen, size - filledLen - prefixLen - suffixLen);
        }

        suffix.getChars(0, suffixLen, cbuf, size - suffixLen);

        return InternalUtil.newString(cbuf, true);
    }

    /**
     * Returns the byte array returned by {@code String#getBytes(IOUtil.DEFAULT_CHARSET)}, or {@code null} if the specified String is {@code null}.
     *
     * @param string
     * @return
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
     *
     *
     * @param source
     * @return {@code null} if {@code (source == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static char[] toCharArray(final CharSequence source) {
        if (source == null) {
            return null; // NOSONAR
        } else if (source.length() == 0) {
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
     * code point. Isolated surrogate code units (i.e. a high surrogate not followed by a low surrogate or
     * a low surrogate not preceded by a high surrogate) will be returned as-is.</p>
     *
     * <pre>
     * Strings.toCodePoints(null)   =  null
     * Strings.toCodePoints("")     =  []  // empty array
     * </pre>
     *
     * @param str the character sequence to convert
     * @return an array of code points. {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     * @since 3.6
     */
    @MayReturnNull
    public static int[] toCodePoints(final CharSequence str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0) {
            return N.EMPTY_INT_ARRAY;
        }

        final String s = str.toString();
        final int[] result = new int[s.codePointCount(0, s.length())];
        int index = 0;

        for (int i = 0; i < result.length; i++) {
            result[i] = s.codePointAt(index);
            index += Character.charCount(result[i]);
        }

        return result;
    }

    /**
     * To lower case.
     *
     * @param ch
     * @return
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
     * locale (e.g. {@link Locale#ENGLISH}).
     * </p>
     *
     * @param str
     *            the String to lower case, may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toLowerCase(final String str) {
        if (str == null || str.length() == 0) {
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
     *            the String to lower case, may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return the specified String if it's {@code null} or empty.
     * @since 2.5
     */
    public static String toLowerCase(final String str, final Locale locale) {
        if (str == null || str.length() == 0) {
            return str;
        }

        return str.toLowerCase(locale);
    }

    /**
     * To lower case with underscore.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {//NOSONAR
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
     */
    public static char toUpperCase(final char ch) {
        return Character.toUpperCase(ch);
    }

    // Case conversion
    // -----------------------------------------------------------------------
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
     * locale (e.g. {@link Locale#ENGLISH}).
     * </p>
     *
     * @param str the String to upper case, may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toUpperCase(final String str) {
        if (str == null || str.length() == 0) {
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
     *            the String to upper case, may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return the specified String if it's {@code null} or empty.
     * @since 2.5
     */
    public static String toUpperCase(final String str, final Locale locale) {
        if (str == null || str.length() == 0) {
            return str;
        }

        return str.toUpperCase(locale);
    }

    /**
     * To upper case with underscore.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {//NOSONAR
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
     *
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String toCamelCase(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        if (str.indexOf(WD._UNDERSCORE) >= 0) {
            String[] substrs = Strings.split(str, WD._UNDERSCORE);
            final StringBuilder sb = Objectory.createStringBuilder();

            try {

                for (String substr : substrs) {
                    if (isNotEmpty(substr)) {
                        sb.append(Strings.toLowerCase(substr));
                        if (sb.length() > substr.length()) {
                            sb.setCharAt(sb.length() - substr.length(), Character.toTitleCase(substr.charAt(0)));
                        }
                    }
                }

                return sb.toString();
            } finally {
                Objectory.recycle(sb);
            }
        }

        for (int i = 0, len = str.length(); i < len; i++) {
            if (Character.isLowerCase(str.charAt(i))) {
                if (i == 1) {
                    return Strings.uncapitalize(str);
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

    /**
     *
     * @param ch
     * @return
     */
    public static char swapCase(final char ch) {
        return Character.isUpperCase(ch) || Character.isTitleCase(ch) ? Character.toLowerCase(ch)
                : (Character.isLowerCase(ch) ? Character.toUpperCase(ch) : ch);
    }

    /**
     * <p>
     * Swaps the case of a String changing upper and title case to lower case,
     * and lower case to upper case.
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
     * @param str the String to swap case, may be null
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
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String uncapitalize(final String str) {
        if (str == null || str.length() == 0) {
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
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String capitalize(final String str) {
        if (str == null || str.length() == 0) {
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
        N.checkArgNotEmpty(delimiter, "delimiter"); // NOSONAR

        if (str == null || str.length() == 0) {
            return str;
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);

        N.applyToEach(words, Strings::capitalize);

        return join(words, delimiter);
    }

    /**
     * Capitalize all the words from the specified split by {@code delimiter}.
     *
     * @param str
     * @param delimiter
     * @param excludedWords
     * @return the specified String if it's {@code null} or empty.
     * @throws IllegalArgumentException
     * @see #convertWords(String, String, Collection, Function)
     */
    public static String capitalizeFully(final String str, final String delimiter, final String... excludedWords) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, "delimiter"); // NOSONAR

        if (str == null || str.length() == 0) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return capitalizeFully(str, delimiter);
        }

        return capitalizeFully(str, delimiter, N.toSet(excludedWords));
    }

    /**
     *
     *
     * @param str
     * @param delimiter
     * @param excludedWords
     * @return
     * @throws IllegalArgumentException
     * @see #convertWords(String, String, Collection, Function)
     */
    public static String capitalizeFully(final String str, final String delimiter, final Collection<String> excludedWords) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, "delimiter"); // NOSONAR

        if (str == null || str.length() == 0) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return capitalizeFully(str, delimiter);
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);
        final Collection<String> excludedWordSet = excludedWords instanceof Set || (excludedWords.size() <= 3 && words.length <= 3) ? excludedWords
                : N.newHashSet(excludedWords);

        N.applyToEach(words, e -> excludedWordSet.contains(e) ? e : capitalize(e));

        return join(words, delimiter);
    }

    /**
     * Converts all the words from the specified split by {@code ' '} by the specified Function {@code converter}.
     *
     * @param str
     * @param converter
     * @return the specified String if it's {@code null} or empty.
     */
    public static String convertWords(final String str, final Function<? super String, String> converter) {
        return convertWords(str, " ", converter);
    }

    /**
     * Converts all the words from the specified split by {@code delimiter} by the specified Function {@code converter}.
     *
     * @param str
     * @param delimiter
     * @param converter
     * @return the specified String if it's {@code null} or empty.
     * @throws IllegalArgumentException
     */
    public static String convertWords(final String str, final String delimiter, final Function<? super String, String> converter)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, "delimiter"); // NOSONAR

        if (str == null || str.length() == 0) {
            return str;
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);

        for (int i = 0, len = words.length; i < len; i++) {
            words[i] = converter.apply(words[i]);
        }

        return join(words, delimiter);
    }

    /**
     * Converts all the words from the specified split by {@code delimiter} by the specified Function {@code converter}.
     *
     * @param str
     * @param delimiter
     * @param excludedWords
     * @param converter
     * @return
     * @throws IllegalArgumentException
     */
    public static String convertWords(final String str, final String delimiter, final Collection<String> excludedWords,
            final Function<? super String, String> converter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, "delimiter"); // NOSONAR

        if (str == null || str.length() == 0) {
            return str;
        }

        if (N.isEmpty(excludedWords)) {
            return convertWords(str, delimiter, converter);
        }

        final String[] words = splitPreserveAllTokens(str, delimiter);
        final Collection<String> excludedWordSet = excludedWords instanceof Set || (excludedWords.size() <= 3 && words.length <= 3) ? excludedWords
                : N.newHashSet(excludedWords);

        N.applyToEach(words, e -> excludedWordSet.contains(e) ? e : converter.apply(e));

        return join(words, delimiter);
    }

    /**
     * Replace ''' or '"' with '\'' or '\"' if the previous char of the
     * quotation is not '\'. original String is returned if the specified String
     * is {@code null} or empty.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String quoteEscaped(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            char ch = 0;
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if ((ch == _BACKSLASH) && (i < (len - 1))) {
                    sb.append(ch);
                    sb.append(str.charAt(++i));
                } else if ((ch == _QUOTATION_S) || (ch == _QUOTATION_D)) {
                    sb.append(_BACKSLASH);
                    sb.append(ch);
                } else {
                    sb.append(ch);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param str
     * @param quoteChar it should be {@code "} or {@code '}.
     * @return the specified String if it's {@code null} or empty.
     */
    public static String quoteEscaped(final String str, final char quoteChar) {
        if (str == null || str.length() == 0) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            char ch = 0;
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if ((ch == _BACKSLASH) && (i < (len - 1))) {
                    sb.append(ch);
                    sb.append(str.charAt(++i));
                } else if (ch == quoteChar) {
                    sb.append(_BACKSLASH);
                    sb.append(ch);
                } else {
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
     * @param str the source String to normalize whitespaces from, may be null
     * @return
     *         null String input
     * @see Pattern
     * @see #trim(String)
     * @see <a
     *      href="http://www.w3.org/TR/xpath/#function-normalize-space">http://www.w3.org/TR/xpath/#function-normalize-space</a>
     * @since 3.0
     */
    public static String normalizeSpace(final String str) {
        if (str == null || str.length() == 0) {
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
     * Strings.replaceAll("any", null, *)    = "any"
     * Strings.replaceAll("any", *, null)    = "any"
     * Strings.replaceAll("any", "", *)      = "any"
     * Strings.replaceAll("aba", "a", null)  = "b"
     * Strings.replaceAll("aba", "a", "")    = "b"
     * Strings.replaceAll("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param str text to search and replace in, may be null
     * @param target the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @return
     */
    public static String replaceAll(final String str, final String target, final String replacement) {
        return replaceAll(str, 0, target, replacement);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceAll(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String, once.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * Strings.replaceOnce(null, *, *)        = null
     * Strings.replaceOnce("", *, *)          = ""
     * Strings.replaceOnce("any", null, *)    = "any"
     * Strings.replaceOnce("any", *, null)    = "any"
     * Strings.replaceOnce("any", "", *)      = "any"
     * Strings.replaceOnce("aba", "a", null)  = "ba"
     * Strings.replaceOnce("aba", "a", "")    = "ba"
     * Strings.replaceOnce("aba", "a", "z")   = "zba"
     * </pre>
     *
     * @param str
     * @param target
     * @param replacement the String to replace with, may be null
     * @return the text with any replacements processed,
     *  {@code null} if null String input
     */
    public static String replaceFirst(final String str, final String target, final String replacement) {
        return replaceFirst(str, 0, target, replacement);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceFirst(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, 1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String, once.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * Strings.replaceOnce(null, *, *)        = null
     * Strings.replaceOnce("", *, *)          = ""
     * Strings.replaceOnce("any", null, *)    = "any"
     * Strings.replaceOnce("any", *, null)    = "any"
     * Strings.replaceOnce("any", "", *)      = "any"
     * Strings.replaceOnce("aba", "a", null)  = "ba"
     * Strings.replaceOnce("aba", "a", "")    = "ba"
     * Strings.replaceOnce("aba", "a", "z")   = "zba"
     * </pre>
     *
     * @param str
     * @param target
     * @param replacement the String to replace with, may be null
     * @return the text with any replacements processed,
     *  {@code null} if null String input
     * @deprecated Use {@link #replaceFirst(String,String,String)} instead
     */
    @Deprecated
    public static String replaceOnce(final String str, final String target, final String replacement) {
        return replaceFirst(str, target, replacement);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     * @deprecated Use {@link #replaceFirst(String,int,String,String)} instead
     */
    @Deprecated
    public static String replaceOnce(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceFirst(str, fromIndex, target, replacement);
    }

    /**
     * <p>
     * Replaces a String with another String inside a larger String, for the
     * first {@code max} values of the search String.
     * </p>
     *
     * <p>
     * A {@code null} reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * replace(null, *, *, *)         = null
     * replace("", *, *, *)           = ""
     * replace("any", null, *, *)     = "any"
     * replace("any", "", *, *)       = "any"
     * replace("any", *, *, 0)        = "any"
     * replace("abaa", 0, "a", null, -1) = "b"
     * replace("abaa", 0, "a", "", -1)   = "b"
     * replace("abaa", 0, "a", "z", 0)   = "abaa"
     * replace("abaa", 0, "a", "z", 1)   = "zbaa"
     * replace("abaa", 0, "a", "z", 2)   = "zbza"
     * replace("abaa", 0, "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param str text to search and replace in, may be null
     * @param fromIndex
     * @param target the String to search for, may be null
     * @param replacement the String to replace it with, can't be null
     * @param max maximum number of values to replace, or {@code -1} if no
     *            maximum
     * @return
     *         String input
     */
    public static String replace(final String str, final int fromIndex, final String target, final String replacement, int max) {
        return replace(str, fromIndex, target, replacement, max, false);
    }

    /**
     * Replace all ignore case.
     *
     * @param str
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceAllIgnoreCase(final String str, final String target, final String replacement) {
        return replaceAllIgnoreCase(str, 0, target, replacement);
    }

    /**
     * Replace all ignore case.
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceAllIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, -1);
    }

    /**
     * Replace once ignore case.
     *
     * @param str
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceFirstIgnoreCase(final String str, final String target, final String replacement) {
        return replaceFirstIgnoreCase(str, 0, target, replacement);
    }

    /**
     * Replace once ignore case.
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceFirstIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, 1);
    }

    /**
     * Replace once ignore case.
     *
     * @param str
     * @param target
     * @param replacement
     * @return
     * @deprecated Use {@link #replaceFirstIgnoreCase(String,String,String)} instead
     */
    @Deprecated
    public static String replaceOnceIgnoreCase(final String str, final String target, final String replacement) {
        return replaceFirstIgnoreCase(str, target, replacement);
    }

    /**
     * Replace once ignore case.
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     * @deprecated Use {@link #replaceFirstIgnoreCase(String,int,String,String)} instead
     */
    @Deprecated
    public static String replaceOnceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceFirstIgnoreCase(str, fromIndex, target, replacement);
    }

    /**
     * Replace ignore case.
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @param max
     * @return
     */
    public static String replaceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement, int max) {
        return replace(str, fromIndex, target, replacement, max, true);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @param max
     * @param ignoreCase
     * @return
     */
    private static String replace(final String str, final int fromIndex, final String target, String replacement, int max, boolean ignoreCase) {
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

        final StringBuilder sb = Objectory.createStringBuilder();
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

    /**
     * Replaces each substring of the source String that matches the given
     * regular expression with the given replacement using the
     * {@link Pattern#DOTALL} option. DOTALL is also know as single-line mode in
     * Perl. This call is also equivalent to:
     * <ul>
     * <li>{@code source.replaceAll(&quot;(?s)&quot; + regex, replacement)}</li>
     * <li>
     * {@code Pattern.compile(regex, Pattern.DOTALL).filter(source).replaceAll(replacement)}
     * </li>
     * </ul>
     *
     * @param source
     *            the source string
     * @param regex
     *            the regular expression to which this string is to be matched
     * @param replacement
     *            the string to be substituted for each match
     * @return The resulting {@code String}
     * @see String#replaceAll(String, String)
     * @see Pattern#DOTALL
     * @since 3.2
     */
    public static String replacePattern(final String source, final String regex, final String replacement) {
        return Pattern.compile(regex, Pattern.DOTALL).matcher(source).replaceAll(replacement);
    }

    /**
     * Replace the substring at {@code str.substring(fromIndex, toIndex)} with the specified {@code replacement}
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return
     * @throws IndexOutOfBoundsException
     * @see #largestSubstring(String, int, int)
     */
    public static String replace(final String str, final int fromIndex, final int toIndex, final String replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (N.isEmpty(str)) {
            return replacement == null ? str : replacement;
        } else if (fromIndex == toIndex && N.isEmpty(replacement)) {
            return str;
        }

        return str.substring(0, fromIndex) + N.nullToEmpty(replacement) + str.substring(toIndex);
    }

    /**
     *
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @param replacement
     * @return
     * @see #substringBetween(String, String, String)
     */
    public static String replaceBetween(String str, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex, final String replacement) {
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
     *
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param replacement
     * @return
     * @see #substringAfter(String, String)
     */
    public static String replaceAfter(String str, String delimiterOfExclusiveBeginIndex, final String replacement) {
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
     *
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @param replacement
     * @return
     *
     * @see #substringBefore(String, String)
     */
    public static String replaceBefore(String str, String delimiterOfExclusiveEndIndex, final String replacement) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return str;
        }

        int endIndex = str.indexOf(delimiterOfExclusiveEndIndex);

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
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for and remove, may be null
     * @return
     *         null String input
     * @since 2.1
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
     * Case insensitive removal of a substring if it is at the beginning of a
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
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     * @since 2.4
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
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for and remove, may be null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     * @since 2.1
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
     * Case insensitive removal of a substring if it is at the end of a source
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
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return the specified String if it's {@code null} or empty, or removal String is {@code null} or empty.
     * @since 2.4
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
     *            the source String to search, may be null
     * @param removeChar
     *            the char to search for and remove, may be null
     * @return the specified String if it's {@code null} or empty.
     * @since 2.1
     */
    public static String removeAll(final String str, final char removeChar) {
        return removeAll(str, 0, removeChar);
    }

    /**
     * Removes the all.
     *
     * @param str
     * @param fromIndex
     * @param removeChar
     * @return the specified String if it's {@code null} or empty.
     */
    public static String removeAll(final String str, final int fromIndex, final char removeChar) {
        if (str == null || str.length() == 0) {
            return str;
        }

        int index = str.indexOf(removeChar, fromIndex);
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
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for and remove, may be null
     * @return the specified String if it's {@code null} or empty.
     * @since 2.1
     */
    public static String removeAll(final String str, final String removeStr) {
        return removeAll(str, 0, removeStr);
    }

    /**
     * Removes the all.
     *
     * @param str
     * @param fromIndex
     * @param removeStr
     * @return the specified String if it's {@code null} or empty.
     */
    public static String removeAll(final String str, final int fromIndex, final String removeStr) {
        if (isEmpty(str) || isEmpty(removeStr)) {
            return str;
        }

        return replace(str, fromIndex, removeStr, EMPTY_STRING, -1);
    }

    /**
     * Removes each substring of the source String that matches the given
     * regular expression using the DOTALL option.
     *
     * @param source
     *            the source string
     * @param regex
     *            the regular expression to which this string is to be matched
     * @return The resulting {@code String}
     * @see String#replaceAll(String, String)
     * @see Pattern#DOTALL
     * @since 3.2
     */
    public static String removePattern(final String source, final String regex) {
        return replacePattern(source, regex, EMPTY_STRING);
    }

    /**
     *
     * @param str
     * @param delimiter
     * @return
     */
    public static String[] split(final String str, final char delimiter) {
        final Splitter splitter = splitterPool.get(delimiter);

        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings() : splitter).splitToArray(str);
    }

    /**
     *
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] split(final String str, final char delimiter, final boolean trim) {
        if (trim) {
            final Splitter splitter = trimSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings().trim(trim) : splitter).splitToArray(str);
        } else {
            return split(str, delimiter);
        }
    }

    /**
     *
     * @param str
     * @param delimiter
     * @return
     */
    public static String[] split(final String str, final String delimiter) {
        final Splitter splitter = splitterPool.get(delimiter);

        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings() : splitter).splitToArray(str);
    }

    /**
     *
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] split(final String str, final String delimiter, final boolean trim) {
        if (trim) {
            final Splitter splitter = trimSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings().trim(trim) : splitter).splitToArray(str);
        } else {
            return split(str, delimiter);
        }
    }

    /**
     *
     * @param str
     * @param delimiter
     * @param max
     * @return
     */
    public static String[] split(final String str, final String delimiter, final int max) {
        return Splitter.with(delimiter).omitEmptyStrings().limit(max).splitToArray(str);
    }

    /**
     *
     * @param str
     * @param delimiter
     * @param max
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] split(final String str, final String delimiter, final int max, final boolean trim) {
        return Splitter.with(delimiter).omitEmptyStrings().trim(trim).limit(max).splitToArray(str);
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @return
     */
    public static String[] splitPreserveAllTokens(final String str, final char delimiter) {
        final Splitter splitter = preserveSplitterPool.get(delimiter);

        return (splitter == null ? Splitter.with(delimiter) : splitter).splitToArray(str);
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] splitPreserveAllTokens(final String str, final char delimiter, boolean trim) {
        if (trim) {
            final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        } else {
            return splitPreserveAllTokens(str, delimiter);
        }
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @return
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter) {
        final Splitter splitter = preserveSplitterPool.get(delimiter);

        return (splitter == null ? Splitter.with(delimiter) : splitter).splitToArray(str);
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, boolean trim) {
        if (trim) {
            final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        } else {
            return splitPreserveAllTokens(str, delimiter);
        }
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @param max
     * @return
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final int max) {
        return Splitter.with(delimiter).limit(max).splitToArray(str);
    }

    /**
     * Split preserve all tokens.
     *
     * @param str
     * @param delimiter
     * @param max
     * @param trim
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final int max, boolean trim) {
        return Splitter.with(delimiter).trim(trim).limit(max).splitToArray(str);
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Removes control characters (char &lt;= 32) from both ends of this String,
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
     *            the String to be trimmed, may be null
     * @return
     */
    public static String trim(final String str) {
        return isEmpty(str) || (str.charAt(0) != ' ' && str.charAt(str.length() - 1) != ' ') ? str : str.trim();
    }

    /**
     *
     *
     * @param strs
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
     * Removes control characters (char &lt;= 32) from both ends of this String
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
     *            the String to be trimmed, may be null
     * @return
     *         null String input
     * @since 2.0
     */
    public static String trimToNull(String str) {
        str = trim(str);

        return isEmpty(str) ? null : str;
    }

    /**
     *
     *
     * @param strs
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
     *            the String to be trimmed, may be null
     * @return
     * @since 2.0
     */
    public static String trimToEmpty(final String str) {
        return isEmpty(str) ? EMPTY_STRING : str.trim();
    }

    /**
     *
     *
     * @param strs
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
     *            the String to remove whitespace from, may be null
     * @return
     */
    public static String strip(final String str) {
        return strip(str, null);
    }

    /**
     *
     *
     * @param strs
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
     *            the String to be stripped, may be null
     * @return
     *         String input
     * @since 2.0
     */
    public static String stripToNull(String str) {
        str = strip(str, null);

        return isEmpty(str) ? null : str;
    }

    /**
     *
     *
     * @param strs
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
     *            the String to be stripped, may be null
     * @return
     * @since 2.0
     */
    public static String stripToEmpty(final String str) {
        return isEmpty(str) ? EMPTY_STRING : strip(str, null);
    }

    /**
     *
     *
     * @param strs
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
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the characters to remove, null treated as whitespace
     * @return the specified String if it's {@code null} or empty.
     */
    public static String strip(final String str, final String stripChars) {
        if (str == null || str.length() == 0) {
            return str;
        }

        return stripEnd(stripStart(str, stripChars), stripChars);
    }

    /**
     *
     *
     * @param strs
     * @param stripChars
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
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the characters to remove, null treated as whitespace
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
     *
     *
     * @param strs
     * @param stripChars
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
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the set of characters to remove, null treated as whitespace
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
     *
     *
     * @param strs
     * @param stripChars
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
     * For instance, '&agrave;' will be replaced by 'a'.
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
     * @return input text with diacritics removed. {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     * @since 3.0
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
        return STRIP_ACCENTS_PATTERN.matcher(decomposed).replaceAll(EMPTY_STRING);
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
     *
     *
     * @param strs
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
     *            the String to chomp a newline from, may be null
     * @return String without newline, {@code null} if null String input
     */
    public static String chomp(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        if (str.length() == 1) {
            final char ch = str.charAt(0);

            if (ch == CHAR_CR || ch == CHAR_LF) {
                return EMPTY_STRING;
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
     *
     *
     * @param strs
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
     *            the String to chop last character from, may be null
     * @return String without last character, {@code null} if null String input
     */
    public static String chop(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        final int strLen = str.length();

        if (strLen < 2) {
            return EMPTY_STRING;
        }

        final int lastIdx = strLen - 1;

        if (str.charAt(lastIdx) == CHAR_LF && str.charAt(lastIdx - 1) == CHAR_CR) {
            return str.substring(0, lastIdx - 1);
        } else {
            return str.substring(0, lastIdx);
        }
    }

    /**
     *
     *
     * @param strs
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
     * @param str the String to truncate, may be null
     * @param maxWidth maximum length of result String, must be positive
     * @return truncated String, {@code null} if null String input
     * @throws IllegalArgumentException If {@code maxWidth} is less than {@code 0}
     * @since 3.5
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
     * @param str the String to truncate, may be null
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be positive
     * @return truncated String, {@code null} if null String input
     * @throws IllegalArgumentException If {@code offset} or {@code maxWidth} is less than {@code 0}
     * @since 3.5
     */
    @MayReturnNull
    public static String truncate(final String str, final int offset, final int maxWidth) throws IllegalArgumentException {
        N.checkArgNotNegative(offset, "offset");
        N.checkArgNotNegative(maxWidth, "maxWidth");

        if (str == null) {
            return null;
        } else if (str.length() <= offset || maxWidth == 0) {
            return EMPTY_STRING;
        } else if (str.length() - offset <= maxWidth) {
            return offset == 0 ? str : str.substring(offset);
        } else {
            return str.substring(offset, offset + maxWidth);
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
     *            the String to delete whitespace from, may be null
     * @return the specified String if it's {@code null} or empty.
     */
    public static String deleteWhitespace(final String str) {
        if (str == null || str.length() == 0) {
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
     *
     *
     * @param strs
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
     * Append if missing.
     *
     * @param str
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static String appendIfMissing(final String str, final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        if (str == null || str.length() == 0) {
            return suffix;
        } else if (str.endsWith(suffix)) {
            return str;
        } else {
            return str + suffix;
        }
    }

    /**
     *
     *
     * @param str
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static String appendIfMissingIgnoreCase(final String str, final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        if (str == null || str.length() == 0) {
            return suffix;
        } else if (Strings.endsWithIgnoreCase(str, suffix)) {
            return str;
        } else {
            return str + suffix;
        }
    }

    /**
     * Prepend if missing.
     *
     * @param str
     * @param prefix
     * @return
     * @throws IllegalArgumentException
     */
    public static String prependIfMissing(final String str, final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        if (str == null || str.length() == 0) {
            return prefix;
        } else if (str.startsWith(prefix)) {
            return str;
        } else {
            return prefix + str;
        }
    }

    /**
     *
     *
     * @param str
     * @param prefix
     * @return
     * @throws IllegalArgumentException
     */
    public static String prependIfMissingIgnoreCase(final String str, final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        if (str == null || str.length() == 0) {
            return prefix;
        } else if (Strings.startsWithIgnoreCase(str, prefix)) {
            return str;
        } else {
            return prefix + str;
        }
    }

    /**
     * Wrap if missing.
     *
     * @param str
     * @param prefixSuffix
     * @return
     */
    public static String wrapIfMissing(final String str, final String prefixSuffix) {
        return wrapIfMissing(str, prefixSuffix, prefixSuffix);
    }

    /**
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
     * @param str
     * @param prefix
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static String wrapIfMissing(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (str == null || str.length() == 0) {
            return prefix + suffix;
        } else if (str.startsWith(prefix)) {
            return (str.length() - prefix.length() >= suffix.length() && str.endsWith(suffix)) ? str : str + suffix;
        } else if (str.endsWith(suffix)) {
            return prefix + str;
        } else {
            return concat(prefix, str, suffix);
        }
    }

    /**
     *
     * @param str
     * @param prefixSuffix
     * @return
     */
    public static String wrap(final String str, final String prefixSuffix) {
        return wrap(str, prefixSuffix, prefixSuffix);
    }

    /**
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
     * @param str
     * @param prefix
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static String wrap(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (str == null || str.length() == 0) {
            return prefix + suffix;
        } else {
            return concat(prefix, str, suffix);
        }
    }

    /**
     *
     * @param str
     * @param prefixSuffix
     * @return
     */
    public static String unwrap(final String str, final String prefixSuffix) {
        return unwrap(str, prefixSuffix, prefixSuffix);
    }

    /**
     * <p>
     * Unwraps the specified string {@code str} if and only if it's wrapped by the specified {@code prefix} and {@code suffix}
     * </p>
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
     * @param str
     * @param prefix
     * @param suffix
     * @return the specified String if it's {@code null} or empty.
     * @throws IllegalArgumentException
     */
    public static String unwrap(final String str, final String prefix, final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (str == null || str.length() == 0) {
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
     * @return true if is lower case
     */
    public static boolean isLowerCase(final char ch) {
        return Character.isLowerCase(ch);
    }

    /**
     * Checks if is ascii lower case.
     *
     * @param ch
     * @return true if is ascii lower case
     */
    public static boolean isAsciiLowerCase(final char ch) {
        return (ch >= 'a') && (ch <= 'z');
    }

    /**
     * Checks if is upper case.
     *
     * @param ch
     * @return true if is upper case
     */
    public static boolean isUpperCase(final char ch) {
        return Character.isUpperCase(ch);
    }

    /**
     * Checks if is ascii upper case.
     *
     * @param ch
     * @return true if is ascii upper case
     */
    public static boolean isAsciiUpperCase(final char ch) {
        return (ch >= 'A') && (ch <= 'Z');
    }

    /**
     * Checks if is all lower case.
     *
     * @param cs
     * @return true if is all lower case
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
     * Checks if is all upper case.
     *
     * @param cs
     * @return true if is all upper case
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
     * @param cs
     * @return true if is mixed case
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
     * @return true if is digit
     * @see Character#isDigit(char)
     */
    public static boolean isDigit(final char ch) {
        return Character.isDigit(ch);
    }

    /**
     * Checks if is letter.
     *
     * @param ch
     * @return true if is letter
     * @see Character#isLetter(char)
     */
    public static boolean isLetter(final char ch) {
        return Character.isLetter(ch);
    }

    /**
     * Checks if is letter or digit.
     *
     * @param ch
     * @return true if is letter or digit
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
     * @return true if less than 128
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
     * @return true if between 32 and 126 inclusive
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
     * @return true if less than 32 or equals 127
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
     * @return true if between 65 and 90 or 97 and 122 inclusive
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
     * @return true if between 65 and 90 inclusive
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
     * @return true if between 97 and 122 inclusive
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
     * @return true if between 48 and 57 inclusive
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
     * @return true if between 48 and 57 or 65 and 90 or 97 and 122 inclusive
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
     * @return true if is ascii printable. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
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
     * @return true if is ascii alpha, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
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
     * @return true if is ascii alpha space, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
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
     * @return true if is ascii alphanumeric, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
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
     * @return true if is ascii alphanumeric space, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
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
     * @return true if is ascii numeric, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     * @since 3.0 Changed signature from isAlpha(String) to
     *        isAlpha(CharSequence)
     * @since 3.0 Changed "" to return false and not true
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters and space, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     * @since 3.0 Changed signature from isAlphaSpace(String) to
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters or digits, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     * @since 3.0 Changed signature from isAlphanumeric(String) to
     *        isAlphanumeric(CharSequence)
     * @since 3.0 Changed "" to return false and not true
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters, digits or space, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     * @since 3.0 Changed signature from isAlphanumericSpace(String) to
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
     * is not a Unicode digit and returns false.
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
     * Long.parseLong, e.g. if the value is outside the range for int or long
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains digits, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null} or empty.
     * @since 3.0 Changed signature from isNumeric(String) to
     *        isNumeric(CharSequence)
     * @since 3.0 Changed "" to return false and not true
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
     * A decimal point is not a Unicode digit and returns false.
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains digits or space, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     * @since 3.0 Changed signature from isNumericSpace(String) to
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
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains whitespace, and is non-null. {@code false} is returned if the specified {@code CharSequence} is {@code null}.
     * @since 2.0
     * @since 3.0 Changed signature from isWhitespace(String) to
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
     * Checks whether the String a valid Java number. <code>true</code> is
     * returned if there is a number which can be initialized by
     * <code>createNumber</code> with specified String.
     * </p>
     *
     * <p>
     * <code>Null</code> and empty String will return <code>false</code>.
     * </p>
     *
     * @param str            the <code>String</code> to check
     * @return <code>true</code> if the string is a correctly formatted number
     * @see Numbers#isNumber(String)
     * @see Numbers#isCreatable(String)
     * @see Numbers#isParsable(String)
     * @since 3.3 the code supports hex {@code 0Xhhh} and octal {@code 0ddd}
     *        validation
     * @deprecated use {@link Numbers#isNumber(String)} instead
     */
    @Deprecated
    public static boolean isNumber(final String str) {
        return Numbers.isNumber(str);
    }

    /**
     * <code>true</code> is returned if the specified <code>str</code> only
     * includes characters ('0' ~ '9', '.', '-', '+', 'e').
     * <code>false</code> is return if the specified String is null/empty, or contains empty chars.
     *
     *  "0" => true
     *  " 0.1 " => false
     *  "abc" => false
     *  "1 a" => false
     *  "2e10" => true
     *  "2E-10" => true
     *
     * @param str
     * @return true if is ascii digtal number
     */
    public static boolean isAsciiDigtalNumber(final String str) {
        if (str == null || str.length() == 0) {
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

        if (count == 0) {
            return false;
        } else if (i == len) { //NOSONAR
            return true;
        } else {
            return false;
        }
    }

    /**
     * <code>true</code> is returned if the specified <code>str</code> only
     * includes characters ('0' ~ '9', '-', '+' ).
     * <code>false</code> is return if the specified String is null/empty, or contains empty chars.
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
     * @return true if is ascii digtal integer
     */
    public static boolean isAsciiDigtalInteger(final String str) {
        if (str == null || str.length() == 0) {
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
     *
     * @param str
     * @param charValueToFind
     * @return
     */
    public static int indexOf(final String str, final int charValueToFind) {
        if (str == null || str.length() == 0) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(charValueToFind);
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @param fromIndex
     * @return
     */
    public static int indexOf(final String str, final int charValueToFind, final int fromIndex) {
        if (str == null || str.length() == 0) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(charValueToFind, fromIndex);
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static int indexOf(final String str, final String valueToFind) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(valueToFind);
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static int indexOf(final String str, final String valueToFind, final int fromIndex) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(valueToFind, fromIndex);
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static int indexOfAny(final String str, final char... valuesToFind) {
        return indexOfAny(str, valuesToFind, 0);
    }

    /**
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     */
    public static int indexOfAny(final String str, final char[] valuesToFind, final int fromIndex) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = valuesToFind.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        for (int i = fromIndex < 0 ? 0 : fromIndex; i < strLen; i++) {
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (valuesToFind[j] == ch) {
                    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                        // ch is a supplementary character
                        if (valuesToFind[j + 1] == str.charAt(i + 1)) {
                            return i;
                        }
                    } else {
                        return i;
                    }
                }
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #smallestIndicesOfAll(String, String...)
     */
    @SafeVarargs
    public static int indexOfAny(final String str, final String... valuesToFind) {
        return indexOfAny(str, valuesToFind, 0);
    }

    /**
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     * @see #smallestIndicesOfAll(String, String[], int)
     */
    public static int indexOfAny(final String str, final String[] valuesToFind, final int fromIndex) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        int idx = 0;

        for (int i = 0, len = valuesToFind.length; i < len; i++) {
            if (isEmpty(valuesToFind[i])) {
                continue;
            }

            idx = indexOf(str, valuesToFind[i], fromIndex);

            if (idx != N.INDEX_NOT_FOUND) {
                return idx;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Index of any but.
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static int indexOfAnyBut(final String str, final char... valuesToFind) {
        return indexOfAnyBut(str, valuesToFind, 0);
    }

    /**
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     */
    public static int indexOfAnyBut(final String str, final char[] valuesToFind, final int fromIndex) {
        if (str == null || str.length() == 0) {
            return N.INDEX_NOT_FOUND;
        }

        if (N.isEmpty(valuesToFind)) {
            return 0;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = valuesToFind.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        outer: for (int i = fromIndex < 0 ? 0 : fromIndex; i < strLen; i++) {//NOSONAR
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (valuesToFind[j] == ch) {
                    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                        if (valuesToFind[j + 1] == str.charAt(i + 1)) {
                            continue outer;
                        }
                    } else {
                        continue outer;
                    }
                }
            }

            return i;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @return
     */
    public static int indexOf(final String str, final String valueToFind, final String delimiter) {
        return indexOf(str, valueToFind, delimiter, 0);
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @param fromIndex the index from which to start the search.
     * @return
     */
    public static int indexOf(final String str, final String valueToFind, final String delimiter, final int fromIndex) {
        if (isEmpty(delimiter)) {
            return indexOf(str, valueToFind, fromIndex);
        }

        if (str == null || valueToFind == null) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();

        final int index = str.indexOf(valueToFind, fromIndex);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = valueToFind.length();
        final int delimiterLen = delimiter.length();

        if ((index == fromIndex || (index - fromIndex >= delimiterLen && delimiter.equals(str.substring(index - delimiterLen, index))))
                && (index + strLen == len
                        || (len >= index + strLen + delimiterLen && delimiter.equals(str.substring(index + strLen, index + strLen + delimiterLen))))) {
            return index;
        }

        int idx = str.indexOf(delimiter + valueToFind + delimiter, index);

        if (idx >= 0) {
            return idx + delimiterLen;
        } else {
            idx = str.indexOf(delimiter + valueToFind, index);

            if (idx >= 0 && idx + delimiterLen + strLen == len) {
                return idx + delimiterLen;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Index of ignore case.
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind) {
        return indexOfIgnoreCase(str, valueToFind, 0);
    }

    /**
     * Index of ignore case.
     *
     * @param str
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, final int fromIndex) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = str.length(), substrLen = valueToFind.length(), end = len - substrLen + 1; i < end; i++) {
            if (str.regionMatches(true, i, valueToFind, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, final String delimiter) {
        return indexOfIgnoreCase(str, valueToFind, delimiter, 0);
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @param fromIndex the index from which to start the search.
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final String valueToFind, final String delimiter, final int fromIndex) {
        if (isEmpty(delimiter)) {
            return indexOfIgnoreCase(str, valueToFind, fromIndex);
        }

        if (str == null || valueToFind == null) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();

        final int index = indexOfIgnoreCase(str, valueToFind, fromIndex);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = valueToFind.length();
        final int delimiterLen = delimiter.length();

        if ((index == fromIndex || (index - fromIndex >= delimiterLen && delimiter.equalsIgnoreCase(str.substring(index - delimiterLen, index))))
                && (index + strLen == len || (len >= index + strLen + delimiterLen
                        && delimiter.equalsIgnoreCase(str.substring(index + strLen, index + strLen + delimiterLen))))) {
            return index;
        }

        int idx = indexOfIgnoreCase(str, delimiter + valueToFind + delimiter, index);

        if (idx >= 0) {
            return idx + delimiterLen;
        } else {
            idx = indexOfIgnoreCase(str, delimiter + valueToFind, index);

            if (idx >= 0 && idx + delimiterLen + strLen == len) {
                return idx + delimiterLen;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param str
     * @param charValueToFind
     * @return
     */
    public static int lastIndexOf(final String str, final int charValueToFind) {
        if (str == null || str.length() == 0) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(charValueToFind);
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified character, searching backward starting at the specified index.
     * For values of <code>ch</code> in the range from 0 to 0xFFFF (inclusive),
     * the index returned is the largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * {@code (this.charAt(<i>k</i>) == ch) && (<i>k</i> <= fromIndex)}
     * </pre>
     *
     * </blockquote> is true. For other values of <code>ch</code>, it is the
     * largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * {@code (this.codePointAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)}
     * </pre>
     *
     * </blockquote> is true. In either case, if no such character occurs in
     * this string at or before position <code>fromIndex</code>, then
     * <code>-1</code> is returned.
     *
     * <p>
     * All indices are specified in <code>char</code> values (Unicode code
     * units).
     *
     * @param str
     * @param charValueToFind a character (Unicode code point).
     * @param startIndexFromBack the index to start the search from. There is no restriction on
     *            the value of <code>fromIndex</code>. If it is greater than or
     *            equal to the length of this string, it has the same effect as
     *            if it were equal to one less than the length of this string:
     *            this entire string may be searched. If it is negative, it has
     *            the same effect as if it were -1: -1 is returned.
     * @return
     *         character sequence represented by this object that is less than
     *         or equal to <code>fromIndex</code>, or <code>-1</code> if the
     *         character does not occur before that point.
     */
    public static int lastIndexOf(final String str, final int charValueToFind, final int startIndexFromBack) {
        if (str == null || str.length() == 0 || startIndexFromBack < 0) {
            return N.INDEX_NOT_FOUND;
        }

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
     * Returns the index within <code>str</code> of the last occurrence of the
     * specified <code>substr</code>, searching backward starting at the
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
     * @param startIndexFromBack
     * @return
     */
    public static int lastIndexOf(final String str, final String valueToFind, final int startIndexFromBack) {
        if (str == null || valueToFind == null || startIndexFromBack < 0 || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(valueToFind, startIndexFromBack);
    }

    /**
     * Last index of.
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @return
     */
    public static int lastIndexOf(final String str, final String valueToFind, final String delimiter) {
        return lastIndexOf(str, valueToFind, delimiter, str.length());
    }

    /**
     * Last index of.
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @param startIndexFromBack the start index to traverse backwards from
     * @return
     */
    public static int lastIndexOf(final String str, final String valueToFind, final String delimiter, final int startIndexFromBack) {
        if (isEmpty(delimiter)) {
            return lastIndexOf(str, valueToFind, startIndexFromBack);
        } else if (str == null || valueToFind == null) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();

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

        if (index == 0 && (index + valueToFind.length() == len || (len >= index + valueToFind.length() + delimiter.length()
                && delimiter.equals(str.substring(index + valueToFind.length(), index + valueToFind.length() + delimiter.length()))))) {
            return index;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of ignore case.
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static int lastIndexOfIgnoreCase(final String str, final String valueToFind) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return lastIndexOfIgnoreCase(str, valueToFind, str.length());
    }

    /**
     * Last index of ignore case.
     *
     * @param str
     * @param valueToFind
     * @param startIndexFromBack
     * @return
     */
    public static int lastIndexOfIgnoreCase(final String str, final String valueToFind, final int startIndexFromBack) {
        if (str == null || valueToFind == null || valueToFind.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, str.length() - valueToFind.length()), substrLen = valueToFind.length(); i >= 0; i--) {
            if (str.regionMatches(true, i, valueToFind, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of any.
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static int lastIndexOfAny(final String str, final char... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        int idx = 0;

        for (char ch : valuesToFind) {
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
                    if (i > 0 && j > 0 && Character.isHighSurrogate(ch = str.charAt(i - 1))) {
                        // ch is a supplementary character
                        if (valuesToFind[j - 1] == ch) {
                            return i - 1;
                        }
                    } else {
                        return i;
                    }
                }
            }
        }

        return N.INDEX_NOT_FOUND;

    }

    /**
     * Last index of any.
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #largestIndicesOfAll(String, String...)
     */
    @SafeVarargs
    public static int lastIndexOfAny(final String str, final String... valuesToFind) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        int idx = 0;

        for (String substr : valuesToFind) {
            if (substr == null) {
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
     * <p>Find the smallest index of any of a set of potential substrings.</p>
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #indexOfAny(String, String...)
     */
    @SafeVarargs
    public static int smallestIndicesOfAll(final String str, final String... valuesToFind) {
        return smallestIndicesOfAll(str, valuesToFind, 0);
    }

    /**
     * <p>Find the smallest index of any of a set of potential substrings from {@code fromIndex}.</p>
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     * @see #indexOfAny(String, String[], int)
     */
    public static int smallestIndicesOfAll(final String str, final String[] valuesToFind, final int fromIndex) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (String substr : valuesToFind) {
            if (substr == null || (fromIndex + substr.length() > len)) {
                continue;
            }

            int tmp = str.indexOf(substr, fromIndex);

            result = tmp >= 0 && (result == N.INDEX_NOT_FOUND || tmp < result) ? tmp : result;

            if (result == fromIndex) {
                break;
            }
        }

        return result;
    }

    /**
     * <p>Find the largest index of any of a set of potential substrings.</p>
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #indexOfAny(String, String...)
     */
    @SafeVarargs
    public static int largestIndicesOfAll(final String str, final String... valuesToFind) {
        return largestIndicesOfAll(str, valuesToFind, 0);
    }

    /**
     * <p>Find the largest index of any of a set of potential substrings.</p>
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     * @see #indexOfAny(String, String[], int)
     */
    public static int largestIndicesOfAll(final String str, final String[] valuesToFind, final int fromIndex) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        final List<String> sortedSubstrs = Stream.of(valuesToFind) //
                .filter(it -> !(it == null || (fromIndex + it.length() > len)))
                .sortedByInt(N::len)
                .toList();

        int result = N.INDEX_NOT_FOUND;

        for (String substr : sortedSubstrs) {
            result = N.max(result, str.indexOf(substr, fromIndex));

            if (result == len - substr.length()) {
                break;
            }
        }

        return result;
    }

    /**
     * <p>Find the smallest last index of any of a set of potential substrings from {@code fromIndex}.</p>
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #indexOfAny(String, String...)
     */
    @SafeVarargs
    public static int smallestLastindicesOfAll(final String str, final String... valuesToFind) {
        return smallestLastindicesOfAll(str, valuesToFind, N.len(str));
    }

    /**
     * <p>Find the smallest last index of any of a set of potential substrings from {@code fromIndex}.</p>
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     * @see #indexOfAny(String, String[], int)
     */
    public static int smallestLastindicesOfAll(final String str, final String[] valuesToFind, final int fromIndex) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (String substr : valuesToFind) {
            if (substr == null || substr.length() > len) {
                continue;
            }

            int tmp = str.lastIndexOf(substr, fromIndex);

            result = tmp >= 0 && (result == N.INDEX_NOT_FOUND || tmp < result) ? tmp : result;

            if (result == 0) {
                break;
            }
        }

        return result;
    }

    /**
     * <p>Find the largest index among the first index of any of a set of potential substrings.</p>
     *
     * @param str
     * @param valuesToFind
     * @return
     * @see #indexOfAny(String, String...)
     */
    @SafeVarargs
    public static int largestLastindicesOfAll(final String str, final String... valuesToFind) {
        return largestLastindicesOfAll(str, valuesToFind, N.len(str));
    }

    /**
     * <p>Find the largest index among the first index of any of a set of potential substrings.</p>
     *
     *
     * @param str
     * @param valuesToFind
     * @param fromIndex
     * @return
     * @see #indexOfAny(String, String[], int)
     */
    public static int largestLastindicesOfAll(final String str, final String[] valuesToFind, final int fromIndex) {
        if (str == null || N.isEmpty(valuesToFind)) {
            return N.INDEX_NOT_FOUND;
        }

        final int len = str.length();
        int result = N.INDEX_NOT_FOUND;

        for (String substr : valuesToFind) {
            if (substr == null || substr.length() > len) {
                continue;
            }

            result = N.max(result, str.lastIndexOf(substr, fromIndex));

            if (result == fromIndex) {
                break;
            }
        }

        return result;
    }

    /**
     * <p>
     * Finds the n-th index within a String, handling {@code null}.
     * </p>
     *
     * @param str
     * @param valueToFind
     * @param ordinal the n-th {@code searchStr} to find
     * @return
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int ordinalIndexOf(final String str, final String valueToFind, final int ordinal) {
        return ordinalIndexOf(str, valueToFind, ordinal, false);
    }

    /**
     * <p>
     * Finds the n-th last index within a String, handling {@code null}.
     * </p>
     *
     * @param str
     * @param valueToFind
     * @param ordinal the n-th last {@code searchStr} to find
     * @return
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int lastOrdinalIndexOf(final String str, final String valueToFind, final int ordinal) {
        return ordinalIndexOf(str, valueToFind, ordinal, true);
    }

    /**
     *
     * @param str
     * @param charValueToFind
     * @return
     * @see N#occurrencesOf(String, char)
     */
    @SuppressWarnings("deprecation")
    public static int countMatches(final String str, final int charValueToFind) {
        if (str == null || str.length() == 0) {
            return 0;
        }

        int occurrences = 0;

        for (char e : InternalUtil.getCharsForReadOnly(str)) {
            if (e == charValueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     * @see N#occurrencesOf(String, String)
     */
    public static int countMatches(final String str, final String valueToFind) {
        if (str == null || valueToFind == null) {
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

    /**
     *
     * @param str
     * @param charValueToFind
     * @return
     */
    public static boolean contains(final String str, final int charValueToFind) {
        if (str == null || str.length() == 0) {
            return false;
        }

        return indexOf(str, charValueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static boolean contains(final String str, final String valueToFind) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOf(str, valueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @return
     */
    public static boolean contains(final String str, final String valueToFind, final String delimiter) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOf(str, valueToFind, delimiter) != N.INDEX_NOT_FOUND;
    }

    /**
     * Contains ignore case.
     *
     * @param str
     * @param valueToFind
     * @return
     */
    public static boolean containsIgnoreCase(final String str, final String valueToFind) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOfIgnoreCase(str, valueToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valueToFind
     * @param delimiter
     * @return
     */
    public static boolean containsIgnoreCase(final String str, final String valueToFind, final String delimiter) {
        if (str == null || valueToFind == null) {
            return false;
        }

        return indexOfIgnoreCase(str, valueToFind, delimiter) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAny(final String str, final char... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAny(str, valuesToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAny(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAny(str, valuesToFind) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAnyIgnoreCase(final String str, final String... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        } else if (valuesToFind.length == 1) {
            return containsIgnoreCase(str, valuesToFind[0]);
        } else if (valuesToFind.length == 2) {
            if (containsIgnoreCase(str, valuesToFind[0])) {
                return true;
            }

            return containsIgnoreCase(str, valuesToFind[1]);
        }

        final String sourceText = str.toLowerCase();

        for (String searchStr : valuesToFind) {
            if (isNotEmpty(searchStr) && indexOf(sourceText, searchStr.toLowerCase()) != N.INDEX_NOT_FOUND) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAll(final String str, final char... valuesToFind) {
        if (N.isEmpty(valuesToFind)) {
            return true;
        } else if (str == null || str.length() == 0) {
            return false;
        }

        for (char ch : valuesToFind) {
            if (str.indexOf(ch) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAll(final String str, final String... valuesToFind) {
        if (N.isEmpty(valuesToFind)) {
            return true;
        } else if (str == null || str.length() == 0) {
            return false;
        }

        for (String searchStr : valuesToFind) {
            if (!Strings.contains(str, searchStr)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsAllIgnoreCase(final String str, final String... valuesToFind) {
        if (N.isEmpty(valuesToFind)) {
            return true;
        } else if (str == null || str.length() == 0) {
            return false;
        }

        for (String searchStr : valuesToFind) {
            if (!Strings.containsIgnoreCase(str, searchStr)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsOnly(final String str, final char... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return false;
        }

        return indexOfAnyBut(str, valuesToFind) == N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param valuesToFind
     * @return
     */
    @SafeVarargs
    public static boolean containsNone(final String str, final char... valuesToFind) {
        if (isEmpty(str) || N.isEmpty(valuesToFind)) {
            return true;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = valuesToFind.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (valuesToFind[j] == ch) {
                    if (Character.isHighSurrogate(ch)) {
                        if ((j == chsLast) || (i < strLast && valuesToFind[j + 1] == str.charAt(i + 1))) {
                            return false;
                        }
                    } else {
                        // ch is in the Basic Multilingual Plane
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     *
     * @param str
     * @return
     */
    // From org.springframework.util.StringUtils, under Apache License 2.0
    public static boolean containsWhitespace(final String str) {
        if (str == null || str.length() == 0) {
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
     *
     * @param str
     * @param prefix
     * @return
     */
    public static boolean startsWith(final String str, final String prefix) {
        return startsWith(str, prefix, false);
    }

    /**
     * Starts with ignore case.
     *
     * @param str
     * @param prefix
     * @return
     */
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        return startsWith(str, prefix, true);
    }

    /**
     *
     * @param str
     * @param prefix
     * @param ignoreCase
     * @return
     */
    private static boolean startsWith(final String str, final String prefix, final boolean ignoreCase) {
        if (str == null || prefix == null || prefix.length() > str.length()) {
            return false;
        }

        return ignoreCase ? str.regionMatches(true, 0, prefix, 0, prefix.length()) : str.startsWith(prefix);
    }

    /**
     * Starts with any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
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
     * Starts with any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
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
     *
     * @param str
     * @param suffix
     * @return
     */
    public static boolean endsWith(final String str, final String suffix) {
        return endsWith(str, suffix, false);
    }

    /**
     * Ends with ignore case.
     *
     * @param str
     * @param suffix
     * @return
     */
    public static boolean endsWithIgnoreCase(final String str, final String suffix) {
        return endsWith(str, suffix, true);
    }

    /**
     * Ends with any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
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
     * Ends with ignore case.
     *
     * @param str
     * @param substrs
     * @return
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
     *
     * @param str
     * @param suffix
     * @param ignoreCase
     * @return
     */
    private static boolean endsWith(final String str, final String suffix, final boolean ignoreCase) {
        if (str == null || suffix == null || suffix.length() > str.length()) {
            return false;
        }

        final int strOffset = str.length() - suffix.length();

        return ignoreCase ? str.regionMatches(true, strOffset, suffix, 0, suffix.length()) : str.endsWith(suffix);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
    }

    /**
     * Equals ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equalsIgnoreCase(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.equalsIgnoreCase(b));
    }

    /**
     * Equals with any.
     *
     * @param str
     * @param searchStrings
     * @return
     */
    @SafeVarargs
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
     * Equals with any.
     *
     * @param str
     * @param searchStrs
     * @return
     */
    @SafeVarargs
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

        for (String searchStr : searchStrs) {
            if (equals(sourceText, searchStr.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compare ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static int compareIgnoreCase(final String a, final String b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));
    }

    /**
     * <p>
     * Compares two Strings, and returns the index at which the Stringss begin
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
     *            the first String, may be null
     * @param b
     *            the second String, may be null
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
        for (int len = N.min(a.length(), b.length()); i < len; i++) {
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
     * Strings.indexOfDifference(new String[] {"abc", null, null}) = 0
     * Strings.indexOfDifference(new String[] {null, null, "abc"}) = 0
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
    @SafeVarargs
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
        for (int i = 0; i < arrayLen; i++) {
            if (strs[i] == null) {
                shortestStrLen = 0;
            } else {
                shortestStrLen = Math.min(strs[i].length(), shortestStrLen);
                longestStrLen = Math.max(strs[i].length(), longestStrLen);
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
            // we compared all of the characters up to the length of the
            // shortest string and didn't find a match, but the string lengths
            // vary, so return the length of the shortest string.
            return shortestStrLen;
        }

        return firstDiff;
    }

    // --------- from Google Guava

    /**
     * Note: copy rights: Google Guava.
     *
     * Returns the longest string {@code prefix} such that
     * {@code a.toString().startsWith(prefix) && b.toString().startsWith(prefix)}
     * , taking care not to split surrogate pairs. If {@code a} and {@code b}
     * have no common prefix, returns the empty string.
     *
     * @param a
     * @param b
     * @return
     */
    public static String commonPrefix(final String a, final String b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY_STRING;
        }

        int maxPrefixLength = Math.min(a.length(), b.length());
        int p = 0;

        while (p < maxPrefixLength && a.charAt(p) == b.charAt(p)) {
            p++;
        }

        if (validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1)) {
            p--;
        }

        if (p == a.length()) {
            return a;
        } else if (p == b.length()) {
            return b;
        } else {
            return a.substring(0, p);
        }
    }

    /**
     *
     * @param strs
     * @return
     */
    @SafeVarargs
    public static String commonPrefix(final String... strs) {
        if (N.isEmpty(strs)) {
            return EMPTY_STRING;
        }

        if (strs.length == 1) {
            return isEmpty(strs[0]) ? EMPTY_STRING : strs[0];
        }

        String commonPrefix = commonPrefix(strs[0], strs[1]);

        if (isEmpty(commonPrefix)) {
            return EMPTY_STRING;
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
     * Note: copy rights: Google Guava.
     *
     * Returns the longest string {@code suffix} such that
     * {@code a.toString().endsWith(suffix) && b.toString().endsWith(suffix)},
     * taking care not to split surrogate pairs. If {@code a} and {@code b} have
     * no common suffix, returns the empty string.
     *
     * @param a
     * @param b
     * @return
     */
    public static String commonSuffix(final String a, final String b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY_STRING;
        }

        final int aLength = a.length();
        final int bLength = b.length();
        int maxSuffixLength = Math.min(aLength, bLength);
        int s = 0;

        while (s < maxSuffixLength && a.charAt(aLength - s - 1) == b.charAt(bLength - s - 1)) {
            s++;
        }

        if (validSurrogatePairAt(a, aLength - s - 1) || validSurrogatePairAt(b, bLength - s - 1)) {
            s--;
        }

        if (s == aLength) {
            return a;
        } else if (s == bLength) {
            return b;
        } else {
            return a.substring(aLength - s, aLength);
        }
    }

    /**
     *
     * @param strs
     * @return
     */
    @SafeVarargs
    public static String commonSuffix(final String... strs) {
        if (N.isEmpty(strs)) {
            return EMPTY_STRING;
        }

        if (strs.length == 1) {
            return isEmpty(strs[0]) ? EMPTY_STRING : strs[0];
        }

        String commonSuffix = commonSuffix(strs[0], strs[1]);

        if (isEmpty(commonSuffix)) {
            return EMPTY_STRING;
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
     * Note: copy rights: Google Guava.
     *
     * True when a valid surrogate pair starts at the given {@code index} in the
     * given {@code string}. Out-of-range indexes return false.
     *
     * @param str
     * @param index
     * @return
     */
    static boolean validSurrogatePairAt(final String str, final int index) {
        return index >= 0 && index <= (str.length() - 2) && Character.isHighSurrogate(str.charAt(index)) && Character.isLowSurrogate(str.charAt(index + 1));
    }

    /**
     *
     * @param a
     * @param b
     * @return an empty String {@code ""} is {@code a} or {@code b} is empty or {@code null}.
     */
    public static String longestCommonSubstring(final String a, final String b) {
        if (isEmpty(a) || isEmpty(b)) {
            return EMPTY_STRING;
        }

        final int lenA = N.len(a);
        final int lenB = N.len(b);

        final int[] dp = new int[lenB + 1];
        int endIndex = 0;
        int maxLen = 0;

        if (lenA > 16 || lenB > 16) {
            final char[] chsA = a.toCharArray();
            final char[] chsB = b.toCharArray();

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
            return EMPTY_STRING;
        }

        return a.substring(endIndex - maxLen, endIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex)}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @return
     * @see StrUtil#substring(String, int)
     * @see #substringAfter(String, int)
     * @see #substring(String, int, int)
     * @see #largestSubstring(String, int, int)
     * @deprecated Use {@link #substringAfter(String,int)} instead
     */
    @Deprecated
    @MayReturnNull
    public static String substring(String str, int inclusiveBeginIndex) {
        return substringAfter(str, inclusiveBeginIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex)}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @return
     * @see #substring(String, int)
     * @see StrUtil#substring(String, int)
     * @see #substring(String, int, int)
     * @see #largestSubstring(String, int, int)
     */
    @MayReturnNull
    public static String substringAfter(String str, int inclusiveBeginIndex) {
        if (str == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        return str.substring(inclusiveBeginIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex, min(exclusiveEndIndex, str.length()))}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     * @see StrUtil#substring(String, int, int)
     * @see #largestSubstring(String, int, int)
     * @deprecated Use {@link #largestSubstring(String,int,int)} instead
     */
    @Deprecated
    @MayReturnNull
    public static String substring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
        return largestSubstring(str, inclusiveBeginIndex, exclusiveEndIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex || inclusiveBeginIndex > str.length())},
     * otherwise returns: {@code str.substring(inclusiveBeginIndex, min(exclusiveEndIndex, str.length()))}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     * @see StrUtil#substring(String, int, int)
     * @see #substring(String, int, int)
     */
    @MayReturnNull
    public static String largestSubstring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex || inclusiveBeginIndex > str.length()) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, N.min(exclusiveEndIndex, str.length()));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || inclusiveBeginIndex < 0)}, or {@code funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) < 0}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return {@code null} if {@code (str == null || inclusiveBeginIndex < 0)}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     */
    @MayReturnNull
    public static String substring(final String str, final int inclusiveBeginIndex, final IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0) {
            return null;
        }

        return largestSubstring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex));
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
     * Returns {@code null} which means it doesn't exist if {@code (str == null || exclusiveEndIndex < 0)}, or {@code funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex) < 0}.
     *
     *
     * @param str
     * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     */
    @MayReturnNull
    public static String substring(final String str, final IntUnaryOperator funcOfInclusiveBeginIndex, final int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex < 0) {
            return null;
        }

        return largestSubstring(str, funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
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
     * Returns {@code null} which means it doesn't exist if {@code (str == null || str.length() == 0)}, or {@code str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise returns: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return {@code null} if {@code (str == null || str.length() == 0)}. (auto-generated java doc for return)
     * @see #substringAfter(String, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, char delimiterOfInclusiveBeginIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        return substringAfter(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || delimiterOfInclusiveBeginIndex == null)}, or {@code str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise returns: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return {@code null} if {@code (str == null || delimiterOfInclusiveBeginIndex == null)}. (auto-generated java doc for return)
     * @see #substringAfter(String, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, String delimiterOfInclusiveBeginIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfInclusiveBeginIndex.length() == 0) {
            return str;
        }

        return substringAfter(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || str.length() == 0 || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}, or {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1)}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1)}
     * @return {@code null} if {@code (str == null || str.length() == 0 || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1);

        // inconsistant behavior
        //    if (index < 0 && str.charAt(inclusiveBeginIndex) == delimiterOfExclusiveEndIndex) {
        //        return EMPTY_STRING;
        //    }

        return largestSubstring(str, inclusiveBeginIndex, index);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}, or {@code str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) < 0}.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return EMPTY_STRING;
        }

        return largestSubstring(str, inclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1));
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || str.length() == 0 || exclusiveEndIndex < 0)}, or {@code str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) < 0}.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || str.length() == 0 || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveEndIndex < 0) {
            return null;
        }

        return largestSubstring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code (str == null || delimiterOfInclusiveBeginIndex == null || exclusiveEndIndex < 0)}, or {@code str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) < 0}.
     *
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - exclusiveEndIndex - delimiterOfInclusiveBeginIndex.length()) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfInclusiveBeginIndex == null || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
     * @see #largestSubstring(String, int, int)
     * @deprecated
     */
    @MayReturnNull
    @Deprecated
    public static String substring(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfInclusiveBeginIndex.length() == 0) {
            return EMPTY_STRING;
        }

        return largestSubstring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - delimiterOfInclusiveBeginIndex.length()),
                exclusiveEndIndex);

    }

    /**
     * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return {@code null} if {@code (str == null || str.length() == 0)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfter(String str, char delimiterOfExclusiveBeginIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveBeginIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfter(String str, String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.length() == 0) {
            return str;
        }

        int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0}, or {@code str.indexOf(delimiterOfExclusiveBeginIndex) < 0 || str.indexOf(delimiterOfExclusiveBeginIndex) + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex}
     * otherwise returns {@code substring(str, index + delimiterOfExclusiveBeginIndex.length(), exclusiveEndIndex)};
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0)} OR {@code (index < 0 || index + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfter(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.length() == 0) {
            return largestSubstring(str, 0, exclusiveEndIndex);
        } else if (exclusiveEndIndex == 0) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0 || index + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex) {
            return null;
        }

        return largestSubstring(str, index + delimiterOfExclusiveBeginIndex.length(), exclusiveEndIndex);
    }

    /**
     * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return {@code null} if {@code (str == null || str.length() == 0)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfterLast(String str, char delimiterOfExclusiveBeginIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfterLast(String str, String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.length() == 0) {
            return EMPTY_STRING;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex);

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
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0)} OR {@code (index < 0 || index + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringAfterLast(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfExclusiveBeginIndex.length() == 0) {
            return EMPTY_STRING;
        } else if (exclusiveEndIndex == 0) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - delimiterOfExclusiveBeginIndex.length());

        if (index < 0 || index + delimiterOfExclusiveBeginIndex.length() > exclusiveEndIndex) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length(), exclusiveEndIndex);
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveBeginIndex
     * @return {@code null} if {@code (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex))}. (auto-generated java doc for return)
     * @see #substringAfter(String, String)
     */
    @MayReturnNull
    public static String substringAfterAny(String str, char... delimitersOfExclusiveBeginIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex)) {
            return null;
        }

        int index = -1;

        for (char delimiterOfExclusiveBeginIndex : delimitersOfExclusiveBeginIndex) {
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
     * @return {@code null} if {@code (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex))}. (auto-generated java doc for return)
     * @see #substringAfter(String, String)
     */
    @MayReturnNull
    public static String substringAfterAny(String str, String... delimitersOfExclusiveBeginIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveBeginIndex)) {
            return null;
        }

        String substr = null;

        for (String delimiterOfExclusiveBeginIndex : delimitersOfExclusiveBeginIndex) {
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
     * @return {@code null} if {@code (str == null)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBefore(String str, char delimiterOfExclusiveEndIndex) {
        if (str == null) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveEndIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBefore(String str, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return EMPTY_STRING;
        }

        int endIndex = str.indexOf(delimiterOfExclusiveEndIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBefore(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return EMPTY_STRING;
        } else if (inclusiveBeginIndex == str.length()) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1);

        if (index < 0) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, index);
    }

    /**
     * Returns the substring before last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || str.length() == 0)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBeforeLast(String str, char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null)} OR {@code (index < 0)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBeforeLast(String str, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return str;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length())} OR {@code (index < 0 || index < inclusiveBeginIndex)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static String substringBeforeLast(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || inclusiveBeginIndex < 0 || inclusiveBeginIndex > str.length()) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return str.substring(inclusiveBeginIndex);
        } else if (inclusiveBeginIndex == str.length()) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (index < 0 || index < inclusiveBeginIndex) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, index);
    }

    /**
     * Returns the substring before any of {@code delimitersOfExclusiveEndIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimitersOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || N.isEmpty(delimitersOfExclusiveEndIndex))}. (auto-generated java doc for return)
     * @see #substringBefore(String, String)
     */
    @MayReturnNull
    public static String substringBeforeAny(String str, char... delimitersOfExclusiveEndIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveEndIndex)) {
            return null;
        }

        int index = -1;

        for (char delimiterOfExclusiveEndIndex : delimitersOfExclusiveEndIndex) {
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
     * @return {@code null} if {@code (str == null || N.isEmpty(delimitersOfExclusiveEndIndex))}. (auto-generated java doc for return)
     * @see #substringBefore(String, String)
     */
    @MayReturnNull
    public static String substringBeforeAny(String str, String... delimitersOfExclusiveEndIndex) {
        if (str == null || N.isEmpty(delimitersOfExclusiveEndIndex)) {
            return null;
        }

        String substr = null;

        for (String delimiterOfExclusiveEndIndex : delimitersOfExclusiveEndIndex) {
            substr = substringBefore(str, delimiterOfExclusiveEndIndex);

            if (substr != null) {
                return substr;
            }
        }

        return null;
    }

    /**
     * Returns {@code null} which means it doesn't exist if {@code str == null || exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex || exclusiveBeginIndex >= str.length()},
     * Otherwise returns: {@code str.substring(exclusiveBeginIndex + 1, min(exclusiveEndIndex, str.length()))}.
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    @MayReturnNull
    public static String substringBetween(String str, int exclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex
                || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        return str.substring(exclusiveBeginIndex + 1, N.min(exclusiveEndIndex, str.length()));
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length())}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, int exclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, exclusiveBeginIndex + 1));
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length())}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, int exclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, exclusiveBeginIndex + 1));
    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || exclusiveEndIndex <= 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, char delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex <= 0) {
            return null;
        }

        return substringBetween(str, str.indexOf(delimiterOfExclusiveBeginIndex), exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0)} OR {@code (index < 0)} OR {@code (exclusiveBeginIndex > exclusiveEndIndex)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        final int exclusiveBeginIndex = index + delimiterOfExclusiveBeginIndex.length();

        if (exclusiveBeginIndex > exclusiveEndIndex) {
            return null;
        }

        return str.substring(exclusiveBeginIndex, exclusiveEndIndex);
    }

    /**
     *
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || str.length() <= 1)} OR {@code (startIndex < 0)} OR {@code (endIndex < 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, char delimiterOfExclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
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

    /**
     *
     * @param str
     * @param tag
     * @return
     * @see #substringBetween(String, String, String)
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, String tag) {
        return substringBetween(str, tag, tag);
    }

    /**
     *
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        return substringBetween(str, 0, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     *
     *
     * @param str
     * @param fromIndex start index for {@code delimiterOfExclusive}. {@code str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex)}
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveEndIndex == null || fromIndex < 0 || fromIndex > str.length())} OR {@code (startIndex < 0)} OR {@code (endIndex < 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, int fromIndex, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveEndIndex == null || fromIndex < 0 || fromIndex > str.length()) {
            return null;
        }

        int startIndex = fromIndex == 0 ? str.indexOf(delimiterOfExclusiveBeginIndex) : str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

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
     * <code>substringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<String> substringsBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        return isEmpty(str) ? new ArrayList<>() : substringsBetween(str, 0, str.length(), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * <code>substringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<String> substringsBetween(final String str, final int fromIndex, final int toIndex, final char delimiterOfExclusiveBeginIndex,
            final char delimiterOfExclusiveEndIndex) {
        final List<int[]> substringIndices = substringIndicesBetween(str, fromIndex, toIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
        final List<String> res = new ArrayList<>(substringIndices.size());

        for (int[] e : substringIndices) {
            res.add(str.substring(e[0], e[1]));
        }

        return res;
    }

    /**
     * <code>substringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<String> substringsBetween(final String str, final String delimiterOfExclusiveBeginIndex, final String delimiterOfExclusiveEndIndex) {
        return isEmpty(str) ? new ArrayList<>() : substringsBetween(str, 0, str.length(), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * <code>substringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<String> substringsBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        final List<int[]> substringIndices = substringIndicesBetween(str, fromIndex, toIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);

        final List<String> res = new ArrayList<>(substringIndices.size());

        for (int[] e : substringIndices) {
            res.add(str.substring(e[0], e[1]));
        }

        return res;
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return {@code null} if {@code (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length())}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, int exclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || exclusiveBeginIndex < 0 || exclusiveBeginIndex >= str.length()) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(exclusiveBeginIndex));
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
     * @return {@code null} if {@code (str == null || exclusiveEndIndex < 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex < 0) {
            return null;
        }

        return substringBetween(str, funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
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
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveBeginIndex.length() > str.length())} OR {@code (index < 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, String delimiterOfExclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveBeginIndex.length() > str.length()) {
            return null;
        }

        final int index = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        final int exclusiveBeginIndex = index + delimiterOfExclusiveBeginIndex.length();

        return substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(exclusiveBeginIndex));
    }

    /**
     *
     * @param str
     * @param funcOfExclusiveBeginIndex (exclusiveBeginIndex <- funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex))
     * @param delimiterOfExclusiveEndIndex (exclusiveEndIndex <- str.lastIndexOf(delimiterOfExclusiveEndIndex))
     * @return {@code null} if {@code (str == null || delimiterOfExclusiveEndIndex == null || delimiterOfExclusiveEndIndex.length() > str.length())} OR {@code (exclusiveEndIndex < 0)}. (auto-generated java doc for return)
     * @see #substringBetween(String, int, int)
     */
    @MayReturnNull
    public static String substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null || delimiterOfExclusiveEndIndex.length() > str.length()) {
            return null;
        }

        final int exclusiveEndIndex = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (exclusiveEndIndex < 0) {
            return null;
        }

        return substringBetween(str, funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    /**
     * <code>substringIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<int[]> substringIndicesBetween(final String str, final char delimiterOfExclusiveBeginIndex, final char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0) {
            return new ArrayList<>(0);
        }

        return substringIndicesBetween(str, 0, str.length(), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * <code>substringIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<int[]> substringIndicesBetween(final String str, final int fromIndex, final int toIndex, final char delimiterOfExclusiveBeginIndex,
            final char delimiterOfExclusiveEndIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (str == null || str.length() == 0) {
            return new ArrayList<>(0);
        }

        final List<int[]> res = new ArrayList<>();

        int idx = str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

        if (idx < 0) {
            return res;
        }

        final Deque<Integer> queue = new LinkedList<>();
        char ch = 0;

        for (int i = idx; i < toIndex; i++) {
            ch = str.charAt(i);

            if (ch == delimiterOfExclusiveBeginIndex) {
                queue.push(i + 1);
            } else if (ch == delimiterOfExclusiveEndIndex && queue.size() > 0) {
                final int startIndex = queue.pop();

                if (res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                    while (res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                        res.remove(res.size() - 1);//NOSONAR
                    }
                }

                res.add(new int[] { startIndex, i });
            }
        }

        return res;
    }

    /**
     * <code>substringIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static List<int[]> substringIndicesBetween(final String str, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) {
        if (str == null || isEmpty(delimiterOfExclusiveBeginIndex) || isEmpty(delimiterOfExclusiveEndIndex)) {
            return new ArrayList<>(0);
        }

        return substringIndicesBetween(str, 0, str.length(), delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     * <code>substringIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<int[]> substringIndicesBetween(final String str, final int fromIndex, final int toIndex, final String delimiterOfExclusiveBeginIndex,
            final String delimiterOfExclusiveEndIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (str == null || isEmpty(delimiterOfExclusiveBeginIndex) || isEmpty(delimiterOfExclusiveEndIndex)) {
            return new ArrayList<>(0);
        }

        final List<int[]> res = new ArrayList<>();

        int idx = str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

        if (idx < 0) {
            return res;
        }

        final Deque<Integer> queue = new LinkedList<>();
        queue.add(idx + delimiterOfExclusiveBeginIndex.length());
        int next = -1;

        for (int i = idx + delimiterOfExclusiveBeginIndex.length(), len = toIndex; i < len;) {
            if (queue.size() == 0) {
                idx = next >= i ? next : str.indexOf(delimiterOfExclusiveBeginIndex, i);

                if (idx < 0) {
                    break;
                } else {
                    queue.add(idx + delimiterOfExclusiveBeginIndex.length());
                    i = idx + delimiterOfExclusiveBeginIndex.length();
                }
            }

            idx = str.indexOf(delimiterOfExclusiveEndIndex, i);

            if (idx < 0) {
                break;
            } else {
                final int endIndex = idx;
                idx = res.size() > 0 ? Math.max(res.get(res.size() - 1)[1] + delimiterOfExclusiveEndIndex.length(), queue.peekLast()) : queue.peekLast();

                while ((idx = str.indexOf(delimiterOfExclusiveBeginIndex, idx)) >= 0 && idx < endIndex) {
                    queue.push(idx + delimiterOfExclusiveBeginIndex.length());
                    idx = idx + delimiterOfExclusiveBeginIndex.length();
                }

                if (idx > 0) {
                    next = idx;
                }

                final int startIndex = queue.pop();

                if (res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                    while (res.size() > 0 && startIndex < res.get(res.size() - 1)[0]) {
                        res.remove(res.size() - 1);
                    }
                }

                res.add(new int[] { startIndex, endIndex });

                i = endIndex + delimiterOfExclusiveEndIndex.length();
            }
        }

        return res;
    }

    /**
     *
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return
     * @see N#replaceRange(String, int, int, String)
     * @deprecated Replaced By {@code N.replaceRange(String, int, int, String)}
     */
    @Deprecated
    @Beta
    public static String replaceRange(final String str, final int fromIndex, final int toIndex, final String replacement) {
        return N.replaceRange(str, fromIndex, toIndex, replacement);
    }

    /**
     *
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex
     * @return
     * @see N#moveRange(String, int, int, int)
     * @deprecated Replaced By {@code N.moveRange(String, int, int, int)}
     */
    @Deprecated
    @Beta
    public static String moveRange(final String str, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        return N.moveRange(str, fromIndex, toIndex, newPositionStartIndex);
    }

    /**
     *
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     * @see N#deleteRange(String, int, int)
     * @deprecated Replaced By {@code N.deleteRange(String, int, int)}
     */
    @Deprecated
    @Beta
    public static String deleteRange(String str, final int fromIndex, final int toIndex) {
        return N.deleteRange(str, fromIndex, toIndex);
    }

    /**
     *
     * @param str
     * @return
     */
    public static OptionalChar firstChar(final String str) {
        if (str == null || str.length() == 0) {
            return OptionalChar.empty();
        }

        return OptionalChar.of(str.charAt(0));
    }

    /**
     *
     * @param str
     * @return
     */
    public static OptionalChar lastChar(final String str) {
        if (str == null || str.length() == 0) {
            return OptionalChar.empty();
        }

        return OptionalChar.of(str.charAt(str.length() - 1));
    }

    /**
     * Returns at most first {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or null, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static String firstChars(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        if (str == null || str.length() == 0 || n == 0) {
            return EMPTY_STRING;
        } else if (str.length() <= n) {
            return str;
        } else {
            return str.substring(0, n);
        }
    }

    /**
     * Returns at most last {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or null, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static String lastChars(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        if (str == null || str.length() == 0 || n == 0) {
            return EMPTY_STRING;
        } else if (str.length() <= n) {
            return str;
        } else {
            return str.substring(str.length() - n);
        }
    }

    /**
     *
     * @param a
     * @return
     */
    public static String join(final boolean[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final boolean[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final boolean[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final char[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final char[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final char[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final byte[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final byte[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final byte[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final short[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final short[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final short[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final int[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final int[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final int[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final long[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final long[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final long[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final float[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final float[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final float[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final double[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final double[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final double[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

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
     *
     * @param a
     * @return
     */
    public static String join(final Object[] a) {
        return join(a, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final Object[] a, final char delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param delimiter
     * @return
     */
    public static String join(final Object[] a, final String delimiter) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     *
     * @param a
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     */
    public static String join(final Object[] a, final String delimiter, final String prefix, final String suffix) {
        return join(a, 0, N.len(a), delimiter, prefix, suffix, false);
    }

    /**
     *
     *
     * @param a
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String join(final Object[] a, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        return join(a, 0, N.len(a), delimiter, prefix, suffix, trim);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter, final boolean trim)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return toString(a[fromIndex], trim);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(toString(a[i], trim));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        return join(a, fromIndex, toIndex, delimiter, null, null, trim);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isEmpty(a) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && isEmpty(prefix) && isEmpty(suffix)) {
            return toString(a[fromIndex], trim);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(toString(a[i], trim));
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(toString(a[i], trim));
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
     *
     * @param c
     * @return
     */
    public static String join(final Iterable<?> c) {
        return join(c, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param c
     * @param delimiter
     * @return
     */
    public static String join(final Iterable<?> c, final char delimiter) {
        return join(c == null ? null : c.iterator(), delimiter);
    }

    /**
     *
     * @param c
     * @param delimiter
     * @return
     */
    public static String join(final Iterable<?> c, final String delimiter) {
        return join(c == null ? null : c.iterator(), delimiter);
    }

    /**
     *
     *
     * @param c
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     */
    public static String join(final Iterable<?> c, final String delimiter, final String prefix, final String suffix) {
        return join(c == null ? null : c.iterator(), delimiter, prefix, suffix);
    }

    /**
     *
     *
     * @param c
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String join(final Iterable<?> c, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        return join(c == null ? null : c.iterator(), delimiter, prefix, suffix, trim);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

    /**
     *
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter, final boolean trim)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (Object e : c) {
                if (i++ > fromIndex) {
                    sb.append(delimiter);
                }

                if (i > fromIndex) {
                    sb.append(toString(e, trim));
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
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        return join(c, fromIndex, toIndex, delimiter, null, null, trim);
    }

    /**
     *
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (c instanceof List && c instanceof RandomAccess) {
                final List<?> list = (List<?>) c;

                if (isEmpty(delimiter)) {
                    for (int i = fromIndex; i < toIndex; i++) {
                        sb.append(toString(list.get(i), trim));
                    }
                } else {
                    for (int i = fromIndex; i < toIndex; i++) {
                        if (i > fromIndex) {
                            sb.append(delimiter);
                        }

                        sb.append(toString(list.get(i), trim));
                    }
                }
            } else {
                int i = 0;
                if (isEmpty(delimiter)) {
                    for (Object e : c) {
                        if (i++ >= fromIndex) {
                            sb.append(toString(e, trim));
                        }

                        if (i >= toIndex) {
                            break;
                        }
                    }
                } else {
                    for (Object e : c) {
                        if (i++ > fromIndex) {
                            sb.append(delimiter);
                        }

                        if (i > fromIndex) {
                            sb.append(toString(e, trim));
                        }

                        if (i >= toIndex) {
                            break;
                        }
                    }
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
     *
     *
     * @param iter
     * @return
     */
    public static String join(final Iterator<?> iter) {
        return join(iter, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     *
     * @param iter
     * @param delimiter
     * @return
     */
    public static String join(final Iterator<?> iter, final char delimiter) {
        if (iter == null) {
            return EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (iter.hasNext()) {
                sb.append(N.toString(iter.next()));
            }

            while (iter.hasNext()) {
                sb.append(delimiter);

                sb.append(N.toString(iter.next()));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     *
     * @param iter
     * @param delimiter
     * @return
     */
    public static String join(final Iterator<?> iter, final String delimiter) {
        return join(iter, delimiter, EMPTY_STRING, EMPTY_STRING, false);
    }

    /**
     *
     *
     * @param iter
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     */
    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix) {
        return join(iter, delimiter, prefix, suffix, false);
    }

    /**
     *
     *
     * @param iter
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        if (iter == null) {
            return EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            if (isEmpty(delimiter)) {
                while (iter.hasNext()) {
                    if (trim) {
                        sb.append(N.toString(iter.next()).trim());
                    } else {
                        sb.append(N.toString(iter.next()));
                    }
                }
            } else {
                if (iter.hasNext()) {
                    sb.append(N.toString(iter.next()));
                }

                while (iter.hasNext()) {
                    sb.append(delimiter);

                    if (trim) {
                        sb.append(N.toString(iter.next()).trim());
                    } else {
                        sb.append(N.toString(iter.next()));
                    }
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
     *
     * @param m
     * @return
     */
    public static String joinEntries(final Map<?, ?> m) {
        return joinEntries(m, Strings.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param m
     * @param entryDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    /**
     *
     * @param m
     * @param entryDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    /**
     *
     * @param m
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter, final char keyValueDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    /**
     *
     * @param m
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter) {
        if (N.isEmpty(m)) {
            return EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    /**
     *
     *
     * @param m
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param prefix
     * @param suffix
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, false);
    }

    /**
     *
     *
     * @param m
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix,
            final boolean trim) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, trim);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param m
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> String joinEntries(final Map<K, V> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix,
            final String suffix, final boolean trim, final Function<? super K, ?> keyMapper, final Function<? super V, ?> valueMapper)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper, "keyMapper");
        N.checkArgNotNull(valueMapper, "valueMapper");

        if (N.isEmpty(m)) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            int i = 0;

            for (Map.Entry<K, V> entry : m.entrySet()) {
                if (i++ > 0) {
                    sb.append(entryDelimiter);
                }

                if (trim) {
                    sb.append(N.toString(keyMapper.apply(entry.getKey())).trim());
                    sb.append(keyValueDelimiter);
                    sb.append(N.toString(valueMapper.apply(entry.getValue())).trim());
                } else {
                    sb.append(N.toString(keyMapper.apply(entry.getKey())));
                    sb.append(keyValueDelimiter);
                    sb.append(N.toString(valueMapper.apply(entry.getValue())));
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
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    /**
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param trim
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD._EQUAL, trim);
    }

    /**
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    /**
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param trim
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD.EQUAL, null, null, trim);
    }

    /**
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, false);
    }

    /**
     *
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter,
            final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isEmpty(m) || fromIndex == toIndex) {
            return EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;

            for (Map.Entry<?, ?> entry : m.entrySet()) {
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
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, null, null, false);
    }

    /**
     *
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
            final String prefix, final String suffix, final boolean trim) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isEmpty(m) || fromIndex == toIndex) {
            if (isEmpty(prefix) && isEmpty(suffix)) {
                return EMPTY_STRING;
            } else if (isEmpty(prefix)) {
                return suffix;
            } else if (isEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (isNotEmpty(prefix)) {
                sb.append(prefix);
            }

            int i = 0;

            for (Map.Entry<?, ?> entry : m.entrySet()) {
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
     * Returns <code>nullToEmpty(a) + nullToEmpty(b)</code>.
     *
     * @param a
     * @param b
     * @return
     */
    public static String concat(final String a, final String b) {
        if (N.isEmpty(a)) {
            return N.isEmpty(b) ? Strings.EMPTY_STRING : b;
        } else {
            return N.isEmpty(b) ? a : a.concat(b);
        }
    }

    /**
     * Returns <code>nullToEmpty(a) + nullToEmpty(b) + nullToEmpty(c)</code>.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static String concat(final String a, final String b, final String c) {
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c));
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d) {
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d));
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e) {
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e));
    }

    /**
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
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f));
    }

    /**
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
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f),
                nullToEmpty(g));
    }

    /**
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
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f), nullToEmpty(g),
                nullToEmpty(h));
    }

    /**
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
     * @return
     */
    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h,
            final String i) {
        return String.join(Strings.EMPTY_STRING, nullToEmpty(a), nullToEmpty(b), nullToEmpty(c), nullToEmpty(d), nullToEmpty(e), nullToEmpty(f), nullToEmpty(g),
                nullToEmpty(h), nullToEmpty(i));
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static String concat(final String... a) {
        if (N.isEmpty(a)) {
            return EMPTY_STRING;
        } else if (a.length == 1) {
            return nullToEmpty(a[0]);
        } else if (a.length == 2) {
            return concat(a[0], a[1]);
        } else if (a.length == 3) {
            return concat(a[0], a[1], a[2]);
        }

        final String[] b = Strings.copyThenApplyToEach(a, Fn.nullToEmpty());

        return String.join(Strings.EMPTY_STRING, b);
    }

    /**
     * Returns {@code N.toString(a) + N.toString(b)}.
     *
     * @param a
     * @param b
     * @return
     */
    public static String concat(final Object a, final Object b) {
        return Strings.concat(N.toString(a), N.toString(b));
    }

    /**
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
     * Returns {@code N.toString(a) + N.toString(b) + N.toString(c) + N.toString(d) + N.toString(e) + N.toString(f) + N.toString(g)}.
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
     * Returns {@code N.toString(a) + N.toString(b) + N.toString(c) + N.toString(d) + N.toString(e) + N.toString(f) + N.toString(g) + N.toString(h)}.
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
     * Returns {@code N.toString(a) + N.toString(b) + N.toString(c) + N.toString(d) + N.toString(e) + N.toString(f) + N.toString(g) + N.toString(h) + N.toString(i)}.
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
     * @return
     * @see #concat(Object, Object)
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
            return NULL_STRING;
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
     *     non-null values are converted to strings using {@link Object#toString()}.
     * @return
     * @since 25.1
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
            int placeholderStart = template.indexOf("%s", templateStart);
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
            sb.append(" [");
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
    private static String lenientToString(Object obj) {
        try {
            return String.valueOf(obj);
        } catch (Exception e) {
            // Default toString() behavior - see Object.toString()
            String objectToString = obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
            // Logger is created inline with fixed name to avoid forcing Proguard to create another class.
            // Logger.getLogger("com.google.common.base.Strings").log(WARNING, "Exception during lenientFormat for " + objectToString, e);

            LOGGER.warn("Exception during lenientFormat for " + objectToString, e); //NOSONAR

            return "<" + objectToString + " threw " + e.getClass().getName() + ">";
        }
    }

    /**
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    public static String reverse(final String str) {
        if (N.len(str) <= 1) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(str);

            return sb.reverse().toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * <p>
     * Reverses a String that is delimited by a specific character.
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
     *            the String to reverse, may be null
     * @param delimiter
     *            the delimiter character to use
     * @return the specified String if it's {@code null} or empty.
     * @since 2.0
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
     *
     * @param str
     * @param delimiter
     * @return the specified String if it's {@code null} or empty.
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
     * Returns a new sorted String if the specified {@code str} is not null or empty, otherwise the specified {@code str} is returned.
     *
     * @param str
     * @return the specified String if it's {@code null} or empty.
     */
    @SuppressWarnings("deprecation")
    public static String sort(String str) {
        if (N.len(str) <= 1) {
            return str;
        }

        final char[] chs = str.toCharArray();
        N.sort(chs);
        return InternalUtil.newString(chs, true);
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
     * @param str the String to rotate, may be null
     * @param shift number of time to shift (positive : right shift, negative : left shift)
     * @return the rotated String,
     *          or the original String if {@code shift == 0},
     *          or {@code null} if null String input
     * @since 3.5
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

        return Strings.substringAfter(str, offset) + Strings.largestSubstring(str, 0, offset);
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String shuffle(final String str) {
        return shuffle(str, N.RAND);
    }

    /**
     *
     *
     * @param str
     * @param rnd
     * @return the specified String if it's {@code null} or empty.
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
     * Strings.overlay("abcdef", null, 2, 4)     = "abef"
     * Strings.overlay("abcdef", "", 2, 4)       = "abef"
     * Strings.overlay("abcdef", "zzzz", 2, 4)   = "abzzzzef"
     * </pre>
     *
     * @param str the String to do overlaying in, may be null
     * @param overlay the String to overlay, may be null
     * @param start the position to start overlaying at
     * @param end the position to stop overlaying before
     * @return overlayed String, {@code ""} if null String input
     * @throws IndexOutOfBoundsException
     * @since 2.0
     */
    public static String overlay(String str, String overlay, int start, int end) throws IndexOutOfBoundsException {
        N.checkFromToIndex(start, end, N.len(str));

        if (overlay == null) {
            overlay = EMPTY_STRING;
        }

        if (str == null || str.length() == 0) {
            return overlay;
        }

        //        final int len = str.length();
        //
        //        if (start < 0) {
        //            start = 0;
        //        }
        //
        //        if (start > len) {
        //            start = len;
        //        }
        //
        //        if (end < 0) {
        //            end = 0;
        //        }
        //
        //        if (end > len) {
        //            end = len;
        //        }
        //
        //        if (start > end) {
        //            final int temp = start;
        //            start = end;
        //            end = temp;
        //        }

        return str.substring(0, start) + overlay + str.substring(end);
    }

    /**
     * Returns a <code>Boolean</code> with a value represented by the specified
     * string. The <code>Boolean</code> returned represents a true value if the
     * string argument is not <code>null</code> and is equal, ignoring case, to
     * the string {@code "true"}.
     *
     * @param str
     *            a string.
     * @return
     */
    public static boolean parseBoolean(final String str) {
        return Strings.isEmpty(str) ? false : Boolean.parseBoolean(str);
    }

    /**
     * Parses the char.
     *
     * @param str
     * @return
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
     * Base 64 encode.
     *
     * @param binaryData
     * @return
     */
    public static String base64Encode(final byte[] binaryData) {
        if (N.isEmpty(binaryData)) {
            return Strings.EMPTY_STRING;
        }

        return BASE64_ENCODER.encodeToString(binaryData);
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String base64EncodeString(final String str) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY_STRING;
        }

        return BASE64_ENCODER.encodeToString(str.getBytes()); // NOSONAR
    }

    /**
     *
     *
     * @param str
     * @return
     */
    public static String base64EncodeUtf8String(final String str) {
        if (Strings.isEmpty(str)) {
            return Strings.EMPTY_STRING;
        }

        return BASE64_ENCODER.encodeToString(str.getBytes(Charsets.UTF_8));
    }

    /**
     * Base 64 decode.
     *
     * @param base64String
     * @return
     */
    public static byte[] base64Decode(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return BASE64_DECODER.decode(base64String);
    }

    /**
     * Base 64 decode to string.
     *
     * @param base64String
     * @return
     */
    public static String base64DecodeToString(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY_STRING;
        }

        return new String(base64Decode(base64String)); // NOSONAR
    }

    /**
     *
     *
     * @param base64String
     * @return
     */
    public static String base64DecodeToUtf8String(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY_STRING;
        }

        return new String(base64Decode(base64String), Charsets.UTF_8);
    }

    /**
     * Base 64 url encode.
     *
     * @param binaryData
     * @return
     */
    public static String base64UrlEncode(final byte[] binaryData) {
        if (N.isEmpty(binaryData)) {
            return Strings.EMPTY_STRING;
        }

        return BASE64_URL_ENCODER.encodeToString(binaryData);
    }

    /**
     * Base 64 url decode.
     *
     * @param base64String
     * @return
     */
    public static byte[] base64UrlDecode(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return BASE64_URL_DECODER.decode(base64String);
    }

    /**
     * Base 64 url decode to string.
     *
     * @param base64String
     * @return
     */
    public static String base64UrlDecodeToString(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY_STRING;
        }

        return new String(BASE64_URL_DECODER.decode(base64String)); // NOSONAR
    }

    /**
     *
     *
     * @param base64String
     * @return
     */
    public static String base64UrlDecodeToUtf8String(final String base64String) {
        if (Strings.isEmpty(base64String)) {
            return Strings.EMPTY_STRING;
        }

        return new String(BASE64_URL_DECODER.decode(base64String), Charsets.UTF_8);
    }

    /**
     *
     * @param parameters
     * @return
     * @see URLEncodedUtil#encode(Object)
     */
    public static String urlEncode(final Object parameters) {
        return URLEncodedUtil.encode(parameters);
    }

    /**
     *
     * @param parameters
     * @param charset
     * @return
     * @see URLEncodedUtil#encode(Object, Charset)
     */
    public static String urlEncode(final Object parameters, final Charset charset) {
        return URLEncodedUtil.encode(parameters, charset);
    }

    /**
     *
     * @param urlQuery
     * @return
     * @see URLEncodedUtil#decode(String)
     */
    public static Map<String, String> urlDecode(final String urlQuery) {
        return URLEncodedUtil.decode(urlQuery);
    }

    /**
     *
     * @param urlQuery
     * @param charset
     * @return
     * @see URLEncodedUtil#decode(String, Charset)
     */
    public static Map<String, String> urlDecode(final String urlQuery, final Charset charset) {
        return URLEncodedUtil.decode(urlQuery, charset);
    }

    /**
     *
     *
     * @param <T>
     * @param urlQuery
     * @param targetType
     * @return
     * @see URLEncodedUtil#decode(String, Class)
     */
    public static <T> T urlDecode(final String urlQuery, final Class<? extends T> targetType) {
        return URLEncodedUtil.decode(urlQuery, targetType);
    }

    /**
     *
     *
     * @param <T>
     * @param urlQuery
     * @param charset
     * @param targetType
     * @return
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
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
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
     * Returns whether or not the <code>octet</code> is in the base 64 alphabet.
     *
     * @param octet
     *            The value to test
     * @return <code>true</code> if the value is defined in the the base 64 alphabet, <code>false</code> otherwise.
     * @since 1.4
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
     * @return <code>true</code> if all bytes are valid characters in the Base64 alphabet or if the byte array is empty;
     *         <code>false</code>, otherwise
     * @since 1.5
     */
    public static boolean isBase64(final byte[] arrayOctet) {
        for (byte element : arrayOctet) {
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
     * @return <code>true</code> if all characters in the String are valid characters in the Base64 alphabet or if
     *         the String is empty; <code>false</code>, otherwise
     *  @since 1.5
     */
    public static boolean isBase64(final String base64) {
        return isBase64(getBytes(base64, IOUtil.DEFAULT_CHARSET));
    }

    /**
     * Return the first found email address or {@code null} if there is no emal address found the specified String.
     *
     * @param str
     * @return
     * @see #isValidEmailAddress(String)
     * @see #findAllEmailAddresses(String)
     */
    public static String findFirstEmailAddress(final String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        final Matcher matcher = EMAIL_ADDRESS_RFC_5322_PATTERN.matcher(str);

        // ^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
        // Matcher matcher = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+").matcher(str);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    /**
     * Return all the found email addresses or an empty {@code List} if there is no emal address found the specified String.
     *
     * @param str
     * @return
     * @see #isValidEmailAddress(String)
     * @see #findAllEmailAddresses(String)
     */
    public static List<String> findAllEmailAddresses(final String str) {
        if (str == null || str.length() == 0) {
            return new ArrayList<>();
        }

        final Matcher matcher = EMAIL_ADDRESS_RFC_5322_PATTERN.matcher(str);

        final List<String> result = new ArrayList<>();

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param converter
     * @throws IllegalArgumentException
     * @see #copyThenApplyToEach(CharSequence[], Function)
     * @see N#applyToEach(Object[], com.landawn.abacus.util.Throwables.Function)
     */
    @Beta
    public static <T extends CharSequence> void applyToEach(final T[] a, final Function<? super T, ? extends T> converter) throws IllegalArgumentException {
        N.checkArgNotNull(converter);

        if (N.isEmpty(a)) {
            return;
        }

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = converter.apply(a[i]);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param converter
     * @return
     * @throws IllegalArgumentException
     * @see #copyThenTrim(String[])
     * @see #copyThenStrip(String[])
     * @see N#copyThenApplyToEach(Object[], com.landawn.abacus.util.Throwables.Function)
     */
    @Beta
    @MayReturnNull
    public static <T extends CharSequence> T[] copyThenApplyToEach(final T[] a, final Function<? super T, ? extends T> converter)
            throws IllegalArgumentException {
        N.checkArgNotNull(converter);

        if (a == null) {
            return null; // NOSONAR
        } else if (a.length == 0) {
            return a.clone();
        }

        final T[] copy = a.clone();

        for (int i = 0, len = a.length; i < len; i++) {
            copy[i] = converter.apply(a[i]);
        }

        return a;
    }

    /**
     *
     * @param strs
     * @return
     * @see #copyThenApplyToEach(CharSequence[], Function)
     * @see Fn#trim()
     * @see Fn#trimToEmpty()
     * @see Fn#trimToNull()
     */
    @Beta
    @MayReturnNull
    public static String[] copyThenTrim(final String[] strs) {
        return copyThenApplyToEach(strs, Fn.trim());
    }

    /**
     *
     * @param strs
     * @return
     * @see #copyThenApplyToEach(CharSequence[], Function)
     * @see Fn#strip()
     * @see Fn#stripToEmpty()
     * @see Fn#stripToNull()
     */
    @Beta
    @MayReturnNull
    public static String[] copyThenStrip(final String[] strs) {
        return copyThenApplyToEach(strs, Fn.strip());
    }

    /**
     * @deprecated repalced by {@code Strings}
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
         * @see Strings#substringAfter(String, int)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfter(str, inclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#largestSubstring(String, int, int)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.largestSubstring(str, inclusiveBeginIndex, exclusiveEndIndex));
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
        public static Optional<String> substring(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
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
        public static Optional<String> substring(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex) {
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
         * @see Strings#substringAfter(String, int)
         */
        @Beta
        public static String substringOrElse(String str, int inclusiveBeginIndex, final String defaultStr) {
            final String ret = Strings.substringAfter(str, inclusiveBeginIndex);

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
         * @see Strings#largestSubstring(String, int, int)
         */
        @Beta
        public static String substringOrElse(String str, int inclusiveBeginIndex, int exclusiveEndIndex, final String defaultStr) {
            final String ret = Strings.largestSubstring(str, inclusiveBeginIndex, exclusiveEndIndex);

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
        public static String substringOrElse(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex, final String defaultStr) {
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
        public static String substringOrElse(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex, final String defaultStr) {
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
         * @see Strings#substringAfter(String, int)
         */
        @Beta
        public static String substringOrElseItself(String str, int inclusiveBeginIndex) {
            final String ret = Strings.substringAfter(str, inclusiveBeginIndex);

            return ret == null ? str : ret;
        }

        /**
         * Returns the substring if it exists, otherwise returns {@code str} itself.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#largestSubstring(String, int, int)
         */
        @Beta
        public static String substringOrElseItself(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
            final String ret = Strings.largestSubstring(str, inclusiveBeginIndex, exclusiveEndIndex);

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
        public static String substringOrElseItself(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
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
        public static String substringOrElseItself(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringAfter(String str, char delimiterOfExclusiveBeginIndex) {
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
        public static Optional<String> substringAfter(String str, String delimiterOfExclusiveBeginIndex) {
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
        public static Optional<String> substringAfter(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringAfterLast(String str, char delimiterOfExclusiveBeginIndex) {
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
        public static Optional<String> substringAfterLast(String str, String delimiterOfExclusiveBeginIndex) {
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
        public static Optional<String> substringAfterLast(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterAny(String, char...)
         */
        @Beta
        public static Optional<String> substringAfterAny(String str, char... delimitersOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterAny(str, delimitersOfExclusiveBeginIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterAny(String, String...)
         */
        @Beta
        public static Optional<String> substringAfterAny(String str, String... delimitersOfExclusiveBeginIndex) {
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
        public static Optional<String> substringBefore(String str, char delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBefore(String str, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBefore(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBeforeLast(String str, char delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBeforeLast(String str, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBeforeLast(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeLast(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeAny(String, char...)
         */
        @Beta
        public static Optional<String> substringBeforeAny(String str, char... delimitersOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeAny(str, delimitersOfExclusiveEndIndex));
        }

        /**
         * Returns {@code Optional<String>} with value of the substring if it exists, otherwise returns an empty {@code Optional<String>}.
         *
         * @param str
         * @param delimitersOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeAny(String, String...)
         */
        @Beta
        public static Optional<String> substringBeforeAny(String str, String... delimitersOfExclusiveEndIndex) {
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
        public static String substringAfterOrElse(String str, String delimiterOfExclusiveBeginIndex, String defaultStr) {
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
        public static String substringAfterLastOrElse(String str, String delimiterOfExclusiveBeginIndex, String defaultStr) {
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
        public static String substringBeforeOrElse(String str, String delimiterOfExclusiveEndIndex, String defaultStr) {
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
        public static String substringBeforeLastOrElse(String str, String delimiterOfExclusiveEndIndex, String defaultStr) {
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
        public static String substringAfterOrElseItself(String str, char delimiterOfExclusiveBeginIndex) {
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
        public static String substringAfterOrElseItself(String str, String delimiterOfExclusiveBeginIndex) {
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
        public static String substringAfterOrElseItself(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static String substringAfterLastOrElseItself(String str, char delimiterOfExclusiveBeginIndex) {
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
        public static String substringAfterLastOrElseItself(String str, String delimiterOfExclusiveBeginIndex) {
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
        public static String substringAfterLastOrElseItself(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static String substringBeforeOrElseItself(String str, char delimiterOfExclusiveEndIndex) {
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
        public static String substringBeforeOrElseItself(String str, String delimiterOfExclusiveEndIndex) {
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
        public static String substringBeforeOrElseItself(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
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
        public static String substringBeforeLastOrElseItself(String str, char delimiterOfExclusiveEndIndex) {
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
        public static String substringBeforeLastOrElseItself(String str, String delimiterOfExclusiveEndIndex) {
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
        public static String substringBeforeLastOrElseItself(String str, int exclusiveEndIndex, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, char delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, char delimiterOfExclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, String tag) {
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
        public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, int fromIndex, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
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
        public static Optional<String> substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, funcOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param <T>
         * @param a
         * @param b
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonEmpty(final T a, final T b) {
            return Strings.isNotEmpty(a) ? Optional.of(a) : (Strings.isNotEmpty(b) ? Optional.of(b) : Optional.<T> empty());
        }

        /**
         *
         * @param <T>
         * @param a
         * @param b
         * @param c
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonEmpty(final T a, final T b, final T c) {
            return Strings.isNotEmpty(a) ? Optional.of(a)
                    : (Strings.isNotEmpty(b) ? Optional.of(b) : (Strings.isNotEmpty(c) ? Optional.of(c) : Optional.<T> empty()));
        }

        /**
         *
         * @param <T>
         * @param a
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonEmpty(final T... a) {
            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            for (T e : a) {
                if (Strings.isNotEmpty(e)) {
                    return Optional.of(e);
                }
            }

            return Optional.empty();
        }

        /**
         *
         * @param <T>
         * @param a
         * @param b
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonBlank(final T a, final T b) {
            return Strings.isNotBlank(a) ? Optional.of(a) : (Strings.isNotBlank(b) ? Optional.of(b) : Optional.<T> empty());
        }

        /**
         *
         * @param <T>
         * @param a
         * @param b
         * @param c
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonBlank(final T a, final T b, final T c) {
            return Strings.isNotBlank(a) ? Optional.of(a)
                    : (Strings.isNotBlank(b) ? Optional.of(b) : (Strings.isNotBlank(c) ? Optional.of(c) : Optional.<T> empty()));
        }

        /**
         *
         * @param <T>
         * @param a
         * @return
         */
        public static <T extends CharSequence> Optional<T> firstNonBlank(final T... a) {
            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            for (T e : a) {
                if (Strings.isNotBlank(e)) {
                    return Optional.of(e);
                }
            }

            return Optional.empty();
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.OptionalInt.empty();
            }

            try {
                return u.OptionalInt.of(Numbers.createInteger(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.OptionalLong.empty();
            }

            try {
                return u.OptionalLong.of(Numbers.createLong(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.OptionalFloat.empty();
            }

            try {
                return u.OptionalFloat.of(Numbers.createFloat(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.OptionalDouble.empty();
            }

            try {
                return u.OptionalDouble.of(Numbers.createDouble(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createBigInteger(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createBigDecimal(str));
            } catch (NumberFormatException e) {
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
            if (Numbers.quickCheckForIsCreatable(str) == false) {
                return u.Optional.empty();
            }

            try {
                return u.Optional.of(Numbers.createNumber(str));
            } catch (NumberFormatException e) {
                return u.Optional.empty();
            }
        }
    }
}
