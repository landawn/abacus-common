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
import static java.util.logging.Level.WARNING;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;

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
 */
public abstract class Strings {
    public static final String ELEMENT_SEPARATOR = ", ".intern();

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    /**
     * String with value {@code "null"}.
     */
    public static final String NULL_STRING = "null".intern();

    /**
     *
     * Char array with value {@code "['n', 'u', 'l', 'l']"}.
     */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /**
     * The empty String {@code ""}.
     * @since 2.0
     */
    public static final String EMPTY = N.EMPTY_STRING;

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
     * A regex pattern for recognizing blocks of whitespace characters. The
     * apparent convolutedness of the pattern serves the purpose of ignoring
     * "blocks" consisting of only a single space: the pattern is used only to
     * normalize whitespace, condensing "blocks" down to a single space, thus
     * matching the same would likely cause a great many noop replacements.
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(?: |\\u00A0|\\s|[\\s&&[^ ]])\\s*");

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

    private static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile("^([a-zA-Z_$][a-zA-Z\\d_$]*)$");

    private Strings() {
        // Utility class.
    }

    /**
     * Returns the string representation of the {@code char} array or null.
     *
     * @param value the character array.
     * @return a String or null
     * @see String#valueOf(char[])
     * @since 3.9
     */
    public static String valueOf(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }

    public static boolean isValidJavaIdentifier(String str) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        return JAVA_IDENTIFIER_PATTERN.matcher(str).matches();
    }

    /**
     * Same as {@code N.isNullOrEmpty(CharSequence)}.
     *
     * @param cs
     * @return
     * @see N#isNullOrEmpty(CharSequence)
     */
    public static boolean isEmpty(final CharSequence cs) {
        return (cs == null) || (cs.length() == 0);
    }

    /**
     * Same as {@code N.isNullOrEmptyOrBlank(CharSequence)}.
     *
     * @param cs
     * @return
     * @see N#isNullOrEmptyOrBlank(CharSequence)
     */
    public static boolean isBlank(final CharSequence cs) {
        return N.isNullOrEmptyOrBlank(cs);
    }

    /**
     * Same as {@code N.notNullOrEmpty(CharSequence)}.
     *
     * @param cs
     * @return
     * @see N#notNullOrEmpty(CharSequence)
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return (cs != null) && (cs.length() > 0);
    }

    /**
     * Same as {@code N.notNullOrEmptyOrBlank(CharSequence)}.
     *
     * @param cs
     * @return
     * @see N#notNullOrEmptyOrBlank(CharSequence)
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return N.notNullOrEmptyOrBlank(cs);
    }

    /**
     * <p>Checks if all of the CharSequences are empty ("") or null.</p>
     *
     * <pre>
     * StringUtil.isAllEmpty(null)             = true
     * StringUtil.isAllEmpty(null, "")         = true
     * StringUtil.isAllEmpty(new String[] {})  = true
     * StringUtil.isAllEmpty(null, "foo")      = false
     * StringUtil.isAllEmpty("", "bar")        = false
     * StringUtil.isAllEmpty("bob", "")        = false
     * StringUtil.isAllEmpty("  bob  ", null)  = false
     * StringUtil.isAllEmpty(" ", "bar")       = false
     * StringUtil.isAllEmpty("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null
     * @since 3.6
     */
    public static boolean isAllEmpty(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotEmpty(cs)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isAllEmpty(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
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
     * <p>Checks if all of the CharSequences are empty (""), null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtil.isAllBlank(null)             = true
     * StringUtil.isAllBlank(null, "foo")      = false
     * StringUtil.isAllBlank(null, null)       = true
     * StringUtil.isAllBlank("", "bar")        = false
     * StringUtil.isAllBlank("bob", "")        = false
     * StringUtil.isAllBlank("  bob  ", null)  = false
     * StringUtil.isAllBlank(" ", "bar")       = false
     * StringUtil.isAllBlank("foo", "bar")     = false
     * StringUtil.isAllBlank(new String[] {})  = true
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null or whitespace only
     * @since 3.6
     */
    public static boolean isAllBlank(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (isNotBlank(cs)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isAllBlank(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
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
     * <p>Checks if any of the CharSequences are empty ("") or null.</p>
     *
     * <pre>
     * StringUtil.isAnyEmpty((String) null)    = true
     * StringUtil.isAnyEmpty((String[]) null)  = false
     * StringUtil.isAnyEmpty(null, "foo")      = true
     * StringUtil.isAnyEmpty("", "bar")        = true
     * StringUtil.isAnyEmpty("bob", "")        = true
     * StringUtil.isAnyEmpty("  bob  ", null)  = true
     * StringUtil.isAnyEmpty(" ", "bar")       = false
     * StringUtil.isAnyEmpty("foo", "bar")     = false
     * StringUtil.isAnyEmpty(new String[]{})   = false
     * StringUtil.isAnyEmpty(new String[]{""}) = true
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null
     * @since 3.2
     */
    public static boolean isAnyEmpty(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAnyEmpty(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
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
     * <p>Checks if any of the CharSequences are empty ("") or null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtil.isAnyBlank((String) null)    = true
     * StringUtil.isAnyBlank((String[]) null)  = false
     * StringUtil.isAnyBlank(null, "foo")      = true
     * StringUtil.isAnyBlank(null, null)       = true
     * StringUtil.isAnyBlank("", "bar")        = true
     * StringUtil.isAnyBlank("bob", "")        = true
     * StringUtil.isAnyBlank("  bob  ", null)  = true
     * StringUtil.isAnyBlank(" ", "bar")       = true
     * StringUtil.isAnyBlank(new String[] {})  = false
     * StringUtil.isAnyBlank(new String[]{""}) = true
     * StringUtil.isAnyBlank("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null or whitespace only
     * @since 3.2
     */
    public static boolean isAnyBlank(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAnyBlank(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
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
     * Same as {@code N.defaultIfNullOrEmpty(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see N#defaultIfNullOrEmpty(CharSequence, CharSequence)
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
     * Same as {@code N.defaultIfNullOrEmptyOrBlank(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see N#defaultIfNullOrEmptyOrBlank(CharSequence, CharSequence)
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

    @SafeVarargs
    public static <T extends CharSequence> T firstNonBlank(final T... values) {
        if (values != null) {
            for (final T val : values) {
                if (isNotBlank(val)) {
                    return val;
                }
            }
        }

        return null;
    }

    /**
     * <p>Returns the first value in the array which is not empty.</p>
     *
     * <p>If all values are empty or the array is {@code null}
     * or empty then {@code null} is returned.</p>
     *
     * <pre>
     * StringUtil.firstNonEmpty(null, null, null)   = null
     * StringUtil.firstNonEmpty(null, null, "")     = null
     * StringUtil.firstNonEmpty(null, "", " ")      = " "
     * StringUtil.firstNonEmpty("abc")              = "abc"
     * StringUtil.firstNonEmpty(null, "xyz")        = "xyz"
     * StringUtil.firstNonEmpty("", "xyz")          = "xyz"
     * StringUtil.firstNonEmpty(null, "xyz", "abc") = "xyz"
     * StringUtil.firstNonEmpty()                   = null
     * </pre>
     *
     * @param <T> the specific kind of CharSequence
     * @param values the values to test, may be {@code null} or empty
     * @return the first value from {@code values} which is not empty,
     *  or {@code null} if there are no non-empty values
     * @since 3.8
     */
    @SafeVarargs
    public static <T extends CharSequence> T firstNonEmpty(final T... values) {
        if (values != null) {
            for (final T val : values) {
                if (isNotEmpty(val)) {
                    return val;
                }
            }
        }
        return null;
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
     * StringUtil.abbreviate(null, 4)        = null
     * StringUtil.abbreviate("", 4)        = ""
     * StringUtil.abbreviate("abcdefg", 6) = "abc..."
     * StringUtil.abbreviate("abcdefg", 7) = "abcdefg"
     * StringUtil.abbreviate("abcdefg", 8) = "abcdefg"
     * StringUtil.abbreviate("abcdefg", 4) = "a..."
     * StringUtil.abbreviate("abcdefg", 3) = IllegalArgumentException
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

    public static String nullToEmpty(String str) {
        return str == null ? N.EMPTY_STRING : str;
    }

    public static void nullToEmpty(String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null ? N.EMPTY_STRING : strs[i];
        }
    }

    public static String emptyToNull(String str) {
        return str == null || str.length() > 0 ? str : null;
    }

    public static void emptyToNull(String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return;
        }

        for (int i = 0, len = strs.length; i < len; i++) {
            strs[i] = strs[i] == null || strs[i].length() > 0 ? strs[i] : null;
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
     * StringUtil.abbreviate(null, 0, 4)                  = null
     * StringUtil.abbreviate("", 0, 4)                  = ""
     * StringUtil.abbreviate("abcdefghijklmno", -1, 10) = "abcdefg..."
     * StringUtil.abbreviate("abcdefghijklmno", 0, 10)  = "abcdefg..."
     * StringUtil.abbreviate("abcdefghijklmno", 1, 10)  = "abcdefg..."
     * StringUtil.abbreviate("abcdefghijklmno", 4, 10)  = "abcdefg..."
     * StringUtil.abbreviate("abcdefghijklmno", 5, 10)  = "...fghi..."
     * StringUtil.abbreviate("abcdefghijklmno", 6, 10)  = "...ghij..."
     * StringUtil.abbreviate("abcdefghijklmno", 8, 10)  = "...ijklmno"
     * StringUtil.abbreviate("abcdefghijklmno", 10, 10) = "...ijklmno"
     * StringUtil.abbreviate("abcdefghijklmno", 12, 10) = "...ijklmno"
     * StringUtil.abbreviate("abcdefghij", 0, 3)        = IllegalArgumentException
     * StringUtil.abbreviate("abcdefghij", 5, 6)        = IllegalArgumentException
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
     * StringUtil.abbreviate(null, "...", 4)        = null
     * StringUtil.abbreviate("", "...", 4)        = ""
     * StringUtil.abbreviate("abcdefg", null, *)  = "abcdefg"
     * StringUtil.abbreviate("abcdefg", ".", 5)   = "abcd."
     * StringUtil.abbreviate("abcdefg", ".", 7)   = "abcdefg"
     * StringUtil.abbreviate("abcdefg", ".", 8)   = "abcdefg"
     * StringUtil.abbreviate("abcdefg", "..", 4)  = "ab.."
     * StringUtil.abbreviate("abcdefg", "..", 3)  = "a.."
     * StringUtil.abbreviate("abcdefg", "..", 2)  = IllegalArgumentException
     * StringUtil.abbreviate("abcdefg", "...", 3) = IllegalArgumentException
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
     * StringUtil.abbreviate(null, "...", 0, 4)                  = null
     * StringUtil.abbreviate("", "...", 0, 4)                  = ""
     * StringUtil.abbreviate("abcdefghijklmno", null, *, *)    = "abcdefghijklmno"
     * StringUtil.abbreviate("abcdefghijklmno", "---", -1, 10) = "abcdefg---"
     * StringUtil.abbreviate("abcdefghijklmno", ",", 0, 10)    = "abcdefghi,"
     * StringUtil.abbreviate("abcdefghijklmno", ",", 1, 10)    = "abcdefghi,"
     * StringUtil.abbreviate("abcdefghijklmno", ",", 2, 10)    = "abcdefghi,"
     * StringUtil.abbreviate("abcdefghijklmno", "::", 4, 10)   = "::efghij::"
     * StringUtil.abbreviate("abcdefghijklmno", "...", 6, 10)  = "...ghij..."
     * StringUtil.abbreviate("abcdefghijklmno", "*", 9, 10)    = "*ghijklmno"
     * StringUtil.abbreviate("abcdefghijklmno", "'", 10, 10)   = "'ghijklmno"
     * StringUtil.abbreviate("abcdefghijklmno", "!", 12, 10)   = "!ghijklmno"
     * StringUtil.abbreviate("abcdefghij", "abra", 0, 4)       = IllegalArgumentException
     * StringUtil.abbreviate("abcdefghij", "...", 5, 6)        = IllegalArgumentException
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

        if (N.notNullOrEmpty(str) && N.EMPTY_STRING.equals(abbrevMarker) && maxWidth > 0) {
            return Strings.substring(str, 0, maxWidth);
        } else if (N.anyNullOrEmpty(str, abbrevMarker)) {
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
     * StringUtil.abbreviateMiddle(null, null, 0)      = null
     * StringUtil.abbreviateMiddle("abc", null, 0)      = "abc"
     * StringUtil.abbreviateMiddle("abc", ".", 0)      = "abc"
     * StringUtil.abbreviateMiddle("abc", ".", 3)      = "abc"
     * StringUtil.abbreviateMiddle("abcdef", ".", 4)     = "ab.f"
     * </pre>
     *
     * @param str the String to abbreviate, may be null
     * @param middle the String to replace the middle characters with, may be null
     * @param length the length to abbreviate {@code str} to.
     * @return the abbreviated String if the above criteria is met, or the original String supplied for abbreviation.
     * @since 2.5
     */
    public static String abbreviateMiddle(final String str, final String middle, final int length) {
        if (N.anyNullOrEmpty(str, middle) || length >= str.length() || length < middle.length() + 2) {
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
     * StringUtil.center(null, 4)     = "    "
     * StringUtil.center("", 4)     = "    "
     * StringUtil.center("ab", 4)   = " ab "
     * StringUtil.center("abcd", 2) = "abcd"
     * StringUtil.center("a", 4)    = " a  "
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
     * StringUtil.center(null, 4, ' ')     = "    "
     * StringUtil.center("", 4, ' ')     = "    "
     * StringUtil.center("ab", 4, ' ')   = " ab "
     * StringUtil.center("abcd", 2, ' ') = "abcd"
     * StringUtil.center("a", 4, ' ')    = " a  "
     * StringUtil.center("a", 4, 'y')    = "yayy"
     * </pre>
     *
     * @param str the String to center, may be null
     * @param size the int size of new String.
     * @param padChar the character to pad the new String with
     * @return centered String
     * @since 2.0
     */
    public static String center(String str, final int size, final char padChar) {
        N.checkArgNotNegative(size, "size");

        if (str == null) {
            str = N.EMPTY_STRING;
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
     * StringUtil.center(null, 4, " ")     = "    "
     * StringUtil.center("", 4, " ")     = "    "
     * StringUtil.center("ab", 4, " ")   = " ab "
     * StringUtil.center("abcd", 2, " ") = "abcd"
     * StringUtil.center("a", 4, " ")    = " a  "
     * StringUtil.center("a", 4, "yz")   = "yzayz"
     * StringUtil.center("abc", 7, "")   = "  abc  "
     * </pre>
     *
     * @param str the String to center, may be null
     * @param minLength the minimum size of new String.
     * @param padStr the String to pad the new String with, must not be null or empty
     * @return centered String
     */
    public static String center(String str, final int minLength, String padStr) {
        N.checkArgNotNegative(minLength, "minLength");
        // N.checkArgNotNullOrEmpty(padStr, "padStr");

        if (str == null) {
            str = N.EMPTY_STRING;
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
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String padStart(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = N.EMPTY_STRING;
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
     * @return
     */
    public static String padStart(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = N.EMPTY_STRING;
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
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String padEnd(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = N.EMPTY_STRING;
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
     * @return
     */
    public static String padEnd(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = N.EMPTY_STRING;
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
     * @param ch
     * @param n
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String repeat(final char ch, final int n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_STRING;
        } else if (n == 1) {
            return String.valueOf(ch);
        }

        if (n < 16) {
            final char[] array = new char[n];
            Arrays.fill(array, ch);

            return InternalUtil.newString(array, true);
        } else {
            final char[] array = new char[n];
            array[0] = ch;

            int cnt = 1;

            for (; cnt < n - cnt; cnt <<= 1) {
                N.copy(array, 0, array, cnt, cnt);
            }

            if (cnt < n) {
                N.copy(array, 0, array, cnt, n - cnt);
            }

            return InternalUtil.newString(array, true);
        }
    }

    /**
     *
     * @param ch
     * @param n
     * @param delimiter
     * @return
     */
    public static String repeat(final char ch, final int n, final char delimiter) {
        return repeat(String.valueOf(ch), n, String.valueOf(delimiter));
    }

    /**
     *
     * @param str
     * @param repeat
     * @return
     */
    public static String repeat(final String str, final int repeat) {
        return repeat(str, repeat, N.EMPTY_STRING);
    }

    /**
     *
     * @param str
     * @param n
     * @param delimiter
     * @return
     */
    public static String repeat(final String str, final int n, final String delimiter) {
        return repeat(str, n, delimiter, N.EMPTY_STRING, N.EMPTY_STRING);
    }

    /**
     *
     * @param str
     * @param n
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String repeat(String str, final int n, String delimiter, String prefix, String suffix) {
        N.checkArgNotNegative(n, "n");

        str = str == null ? N.EMPTY_STRING : str;
        delimiter = delimiter == null ? N.EMPTY_STRING : delimiter;
        prefix = prefix == null ? N.EMPTY_STRING : prefix;
        suffix = suffix == null ? N.EMPTY_STRING : suffix;

        if (n == 0 || (N.isNullOrEmpty(str) && N.isNullOrEmpty(delimiter))) {
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
     * StringUtil.toLowerCase(null)  = null
     * StringUtil.toLowerCase("")    = ""
     * StringUtil.toLowerCase("aBc") = "abc"
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
     * @return
     */
    public static String toLowerCase(final String str) {
        if (N.isNullOrEmpty(str)) {
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
     * StringUtil.toLowerCase(null, Locale.ENGLISH)  = null
     * StringUtil.toLowerCase("", Locale.ENGLISH)    = ""
     * StringUtil.toLowerCase("aBc", Locale.ENGLISH) = "abc"
     * </pre>
     *
     * @param str
     *            the String to lower case, may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return
     * @since 2.5
     */
    public static String toLowerCase(final String str, final Locale locale) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return str.toLowerCase(locale);
    }

    /**
     * To lower case with underscore.
     *
     * @param str
     * @return
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
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
     * N.toUpperCase(null)  = null
     * N.toUpperCase("")    = ""
     * N.toUpperCase("aBc") = "ABC"
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
     * @param str
     *            the String to upper case, may be null
     * @return
     */
    public static String toUpperCase(final String str) {
        if (N.isNullOrEmpty(str)) {
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
     * N.toUpperCase(null, Locale.ENGLISH)  = null
     * N.toUpperCase("", Locale.ENGLISH)    = ""
     * N.toUpperCase("aBc", Locale.ENGLISH) = "ABC"
     * </pre>
     *
     * @param str
     *            the String to upper case, may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return
     * @since 2.5
     */
    public static String toUpperCase(final String str, final Locale locale) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return str.toUpperCase(locale);
    }

    /**
     * To upper case with underscore.
     *
     * @param str
     * @return
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();
        char ch = 0;

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (Character.isLowerCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
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
     * @return
     */
    public static String toCamelCase(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        if (str.indexOf(WD._UNDERSCORE) >= 0) {
            String[] substrs = Strings.split(str, WD._UNDERSCORE);
            final StringBuilder sb = Objectory.createStringBuilder();

            try {

                for (String substr : substrs) {
                    if (N.notNullOrEmpty(substr)) {
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

    public static char[] toCharArray(final CharSequence source) {
        final int len = N.len(source);

        if (len == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        if (source instanceof String) {
            return ((String) source).toCharArray();
        }

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
     * StringUtil.toCodePoints(null)   =  []  // empty array
     * StringUtil.toCodePoints("")     =  []  // empty array
     * </pre>
     *
     * @param str the character sequence to convert
     * @return an empty array is the specified String {@code str} is null or empty.
     * @since 3.6
     */
    public static int[] toCodePoints(final CharSequence str) {
        if (N.isNullOrEmpty(str)) {
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
     *
     * @param ch
     * @return
     */
    public static char swapCase(final char ch) {
        return Character.isLowerCase(ch) ? Character.toUpperCase(ch) : Character.toLowerCase(ch);
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
     * N.swapCase(null)                 = null
     * N.swapCase("")                   = ""
     * N.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
     * </pre>
     *
     * <p>
     * NOTE: This method changed in Lang version 2.0. It no longer performs a
     * word based algorithm. If you only use ASCII, you will notice no change.
     * That functionality is available in
     * org.apache.commons.lang3.text.WordUtils.
     * </p>
     *
     * @param str
     *            the String to swap case, may be null
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String swapCase(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final char[] cbuf = str.toCharArray();
        char ch = 0;
        for (int i = 0, len = cbuf.length; i < len; i++) {
            ch = cbuf[i];

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                cbuf[i] = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
                cbuf[i] = Character.toUpperCase(ch);
            }
        }

        return InternalUtil.newString(cbuf, true);
    }

    /**
     *
     * @param str
     * @return
     */
    public static String capitalize(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        char ch = str.charAt(0);

        if (Character.isTitleCase(ch)) {
            return str;
        }

        if (str.length() == 1) {
            return String.valueOf(Character.toTitleCase(ch));
        } else {
            return Character.toTitleCase(ch) + str.substring(1);
        }
    }

    /**
     *
     * @param str
     * @return
     */
    public static String uncapitalize(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        char ch = str.charAt(0);

        if (Character.isLowerCase(ch)) {
            return str;
        }

        if (str.length() == 1) {
            return String.valueOf(Character.toLowerCase(ch));
        } else {
            return Character.toLowerCase(ch) + str.substring(1);
        }
    }

    /**
     * Replace ''' or '"' with '\'' or '\"' if the previous char of the
     * quotation is not '\'. original String is returned if the specified String
     * is {@code null} or empty.
     *
     * @param str
     * @return
     */
    public static String quoteEscaped(final String str) {
        if (N.isNullOrEmpty(str)) {
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
     * @return
     */
    public static String quoteEscaped(final String str, final char quoteChar) {
        if (N.isNullOrEmpty(str)) {
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
     *   StringUtil.unicodeEscaped(' ') = "\u0020"
     *   StringUtil.unicodeEscaped('A') = "\u0041"
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
        if (N.isNullOrEmpty(str)) {
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
     * N.replaceAll(null, *, *)        = null
     * N.replaceAll("", *, *)          = ""
     * N.replaceAll("any", null, *)    = "any"
     * N.replaceAll("any", *, null)    = "any"
     * N.replaceAll("any", "", *)      = "any"
     * N.replaceAll("aba", "a", null)  = "b"
     * N.replaceAll("aba", "a", "")    = "b"
     * N.replaceAll("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param str text to search and replace in, may be null
     * @param target the String to search for, may be null
     * @param replacement the String to replace it with, may be null
     * @return
     *         String input
     * @see #replaceAll(String text, String searchString, String replacement,
     *      int max)
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
     * StringUtil.replaceOnce(null, *, *)        = null
     * StringUtil.replaceOnce("", *, *)          = ""
     * StringUtil.replaceOnce("any", null, *)    = "any"
     * StringUtil.replaceOnce("any", *, null)    = "any"
     * StringUtil.replaceOnce("any", "", *)      = "any"
     * StringUtil.replaceOnce("aba", "a", null)  = "ba"
     * StringUtil.replaceOnce("aba", "a", "")    = "ba"
     * StringUtil.replaceOnce("aba", "a", "z")   = "zba"
     * </pre>
     *
     * @see #replace(String text, String searchString, String replacement, int max)
     * @param text text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement the String to replace with, may be null
     * @return the text with any replacements processed,
     *  {@code null} if null String input
     */
    public static String replaceOnce(final String str, final String target, final String replacement) {
        return replaceOnce(str, 0, target, replacement);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceOnce(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, 1);
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
    public static String replaceOnceIgnoreCase(final String str, final String target, final String replacement) {
        return replaceOnceIgnoreCase(str, 0, target, replacement);
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
    public static String replaceOnceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, 1);
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

        if (replacement == null) {
            replacement = "";
        }

        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(target) || max == 0) {
            return str;
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
     * N.removeStart(null, *)      = null
     * N.removeStart("", *)        = ""
     * N.removeStart(*, null)      = *
     * N.removeStart("www.domain.com", "www.")   = "domain.com"
     * N.removeStart("domain.com", "www.")       = "domain.com"
     * N.removeStart("www.domain.com", "domain") = "www.domain.com"
     * N.removeStart("abc", "")    = "abc"
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
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(removeStr)) {
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
     * N.removeStartIgnoreCase(null, *)      = null
     * N.removeStartIgnoreCase("", *)        = ""
     * N.removeStartIgnoreCase(*, null)      = *
     * N.removeStartIgnoreCase("www.domain.com", "www.")   = "domain.com"
     * N.removeStartIgnoreCase("www.domain.com", "WWW.")   = "domain.com"
     * N.removeStartIgnoreCase("domain.com", "www.")       = "domain.com"
     * N.removeStartIgnoreCase("www.domain.com", "domain") = "www.domain.com"
     * N.removeStartIgnoreCase("abc", "")    = "abc"
     * </pre>
     *
     * @param str
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return
     *         null String input
     * @since 2.4
     */
    public static String removeStartIgnoreCase(final String str, final String removeStr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(removeStr)) {
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
     * N.removeEnd(null, *)      = null
     * N.removeEnd("", *)        = ""
     * N.removeEnd(*, null)      = *
     * N.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
     * N.removeEnd("www.domain.com", ".com")   = "www.domain"
     * N.removeEnd("www.domain.com", "domain") = "www.domain.com"
     * N.removeEnd("abc", "")    = "abc"
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
    public static String removeEnd(final String str, final String removeStr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(removeStr)) {
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
     * N.removeEndIgnoreCase(null, *)      = null
     * N.removeEndIgnoreCase("", *)        = ""
     * N.removeEndIgnoreCase(*, null)      = *
     * N.removeEndIgnoreCase("www.domain.com", ".com.")  = "www.domain.com"
     * N.removeEndIgnoreCase("www.domain.com", ".com")   = "www.domain"
     * N.removeEndIgnoreCase("www.domain.com", "domain") = "www.domain.com"
     * N.removeEndIgnoreCase("abc", "")    = "abc"
     * N.removeEndIgnoreCase("www.domain.com", ".COM") = "www.domain")
     * N.removeEndIgnoreCase("www.domain.COM", ".com") = "www.domain")
     * </pre>
     *
     * @param str
     *            the source String to search, may be null
     * @param removeStr
     *            the String to search for (case insensitive) and remove, may be
     *            null
     * @return
     *         null String input
     * @since 2.4
     */
    public static String removeEndIgnoreCase(final String str, final String removeStr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(removeStr)) {
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
     * N.remove(null, *)       = null
     * N.remove("", *)         = ""
     * N.remove("queued", 'u') = "qeed"
     * N.remove("queued", 'z') = "queued"
     * </pre>
     *
     * @param str
     *            the source String to search, may be null
     * @param removeChar
     *            the char to search for and remove, may be null
     * @return
     *         null String input
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
     * @return
     */
    public static String removeAll(final String str, final int fromIndex, final char removeChar) {
        if (N.isNullOrEmpty(str)) {
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
     * N.remove(null, *)        = null
     * N.remove("", *)          = ""
     * N.remove(*, null)        = *
     * N.remove(*, "")          = *
     * N.remove("queued", "ue") = "qd"
     * N.remove("queued", "zz") = "queued"
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
    public static String removeAll(final String str, final String removeStr) {
        return removeAll(str, 0, removeStr);
    }

    /**
     * Removes the all.
     *
     * @param str
     * @param fromIndex
     * @param removeStr
     * @return
     */
    public static String removeAll(final String str, final int fromIndex, final String removeStr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(removeStr)) {
            return str;
        }

        return replace(str, fromIndex, removeStr, N.EMPTY_STRING, -1);
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
        return replacePattern(source, regex, N.EMPTY_STRING);
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
     * @deprecated {@code Splitter} is recommended.
     */
    @Deprecated
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
     * @deprecated {@code Splitter} is recommended.
     */
    @Deprecated
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
     * @deprecated {@code Splitter} is recommended.
     */
    @Deprecated
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
     * @deprecated {@code Splitter} is recommended.
     */
    @Deprecated
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
     * StringUtil.trim(null)          = null
     * StringUtil.trim("")            = ""
     * StringUtil.trim("     ")       = ""
     * StringUtil.trim("abc")         = "abc"
     * StringUtil.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, may be null
     * @return
     */
    public static String trim(final String str) {
        return N.isNullOrEmpty(str) || (str.charAt(0) != ' ' && str.charAt(str.length() - 1) != ' ') ? str : str.trim();
    }

    public static void trim(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.trimToNull(null)          = null
     * N.trimToNull("")            = null
     * N.trimToNull("     ")       = null
     * N.trimToNull("abc")         = "abc"
     * N.trimToNull("    abc    ") = "abc"
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

        return N.isNullOrEmpty(str) ? null : str;
    }

    public static void trimToNull(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.trimToEmpty(null)          = ""
     * N.trimToEmpty("")            = ""
     * N.trimToEmpty("     ")       = ""
     * N.trimToEmpty("abc")         = "abc"
     * N.trimToEmpty("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, may be null
     * @return
     * @since 2.0
     */
    public static String trimToEmpty(final String str) {
        return N.isNullOrEmpty(str) ? N.EMPTY_STRING : str.trim();
    }

    public static void trimToEmpty(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.strip(null)     = null
     * N.strip("")       = ""
     * N.strip("   ")    = ""
     * N.strip("abc")    = "abc"
     * N.strip("  abc")  = "abc"
     * N.strip("abc  ")  = "abc"
     * N.strip(" abc ")  = "abc"
     * N.strip(" ab c ") = "ab c"
     * </pre>
     *
     * @param str
     *            the String to remove whitespace from, may be null
     * @return
     */
    public static String strip(final String str) {
        return strip(str, null);
    }

    public static void strip(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.stripToNull(null)     = null
     * N.stripToNull("")       = null
     * N.stripToNull("   ")    = null
     * N.stripToNull("abc")    = "abc"
     * N.stripToNull("  abc")  = "abc"
     * N.stripToNull("abc  ")  = "abc"
     * N.stripToNull(" abc ")  = "abc"
     * N.stripToNull(" ab c ") = "ab c"
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

        return N.isNullOrEmpty(str) ? null : str;
    }

    public static void stripToNull(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.stripToEmpty(null)     = ""
     * N.stripToEmpty("")       = ""
     * N.stripToEmpty("   ")    = ""
     * N.stripToEmpty("abc")    = "abc"
     * N.stripToEmpty("  abc")  = "abc"
     * N.stripToEmpty("abc  ")  = "abc"
     * N.stripToEmpty(" abc ")  = "abc"
     * N.stripToEmpty(" ab c ") = "ab c"
     * </pre>
     *
     * @param str
     *            the String to be stripped, may be null
     * @return
     * @since 2.0
     */
    public static String stripToEmpty(final String str) {
        return N.isNullOrEmpty(str) ? N.EMPTY_STRING : strip(str, null);
    }

    public static void stripToEmpty(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.strip(null, *)          = null
     * N.strip("", *)            = ""
     * N.strip("abc", null)      = "abc"
     * N.strip("  abc", null)    = "abc"
     * N.strip("abc  ", null)    = "abc"
     * N.strip(" abc ", null)    = "abc"
     * N.strip("  abcyx", "xyz") = "  abc"
     * </pre>
     *
     * @param str
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the characters to remove, null treated as whitespace
     * @return
     */
    public static String strip(final String str, final String stripChars) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return stripEnd(stripStart(str, stripChars), stripChars);
    }

    public static void strip(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.stripStart(null, *)          = null
     * N.stripStart("", *)            = ""
     * N.stripStart("abc", "")        = "abc"
     * N.stripStart("abc", null)      = "abc"
     * N.stripStart("  abc", null)    = "abc"
     * N.stripStart("abc  ", null)    = "abc  "
     * N.stripStart(" abc ", null)    = "abc "
     * N.stripStart("yxabc  ", "xyz") = "abc  "
     * </pre>
     *
     * @param str
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the characters to remove, null treated as whitespace
     * @return
     */
    public static String stripStart(final String str, final String stripChars) {
        if (N.isNullOrEmpty(str) || (stripChars != null && stripChars.isEmpty())) {
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

    public static void stripStart(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.stripEnd(null, *)          = null
     * N.stripEnd("", *)            = ""
     * N.stripEnd("abc", "")        = "abc"
     * N.stripEnd("abc", null)      = "abc"
     * N.stripEnd("  abc", null)    = "  abc"
     * N.stripEnd("abc  ", null)    = "abc"
     * N.stripEnd(" abc ", null)    = " abc"
     * N.stripEnd("  abcyx", "xyz") = "  abc"
     * N.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * @param str
     *            the String to remove characters from, may be null
     * @param stripChars
     *            the set of characters to remove, null treated as whitespace
     * @return
     */
    public static String stripEnd(final String str, final String stripChars) {
        if (N.isNullOrEmpty(str) || (stripChars != null && stripChars.isEmpty())) {
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

    public static void stripEnd(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.stripAccents(null)                = null
     * N.stripAccents("")                  = ""
     * N.stripAccents("control")           = "control"
     * N.stripAccents("&eacute;clair")     = "eclair"
     * </pre>
     *
     * @param str
     * @return input text with diacritics removed
     * @since 3.0
     */
    // See also Lucene's ASCIIFoldingFilter (Lucene 2.9) that replaces accented
    // characters by their unaccented equivalent (and uncommitted bug fix:
    // https://issues.apache.org/jira/browse/LUCENE-1343?focusedCommentId=12858907&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12858907).
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

    public static void stripAccents(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * N.chomp(null)          = null
     * N.chomp("")            = ""
     * N.chomp("abc \r")      = "abc "
     * N.chomp("abc\n")       = "abc"
     * N.chomp("abc\r\n")     = "abc"
     * N.chomp("abc\r\n\r\n") = "abc\r\n"
     * N.chomp("abc\n\r")     = "abc\n"
     * N.chomp("abc\n\rabc")  = "abc\n\rabc"
     * N.chomp("\r")          = ""
     * N.chomp("\n")          = ""
     * N.chomp("\r\n")        = ""
     * </pre>
     *
     * @param str
     *            the String to chomp a newline from, may be null
     * @return String without newline, {@code null} if null String input
     */
    public static String chomp(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        if (str.length() == 1) {
            final char ch = str.charAt(0);

            if (ch == N.CHAR_CR || ch == N.CHAR_LF) {
                return N.EMPTY_STRING;
            }

            return str;
        }

        int lastIdx = str.length() - 1;
        final char last = str.charAt(lastIdx);

        if (last == N.CHAR_LF) {
            if (str.charAt(lastIdx - 1) == N.CHAR_CR) {
                lastIdx--;
            }
        } else if (last != N.CHAR_CR) {
            lastIdx++;
        }

        return lastIdx == str.length() ? str : str.substring(0, lastIdx);
    }

    public static void chomp(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * StringUtil.chop(null)          = null
     * StringUtil.chop("")            = ""
     * StringUtil.chop("abc \r")      = "abc "
     * StringUtil.chop("abc\n")       = "abc"
     * StringUtil.chop("abc\r\n")     = "abc"
     * StringUtil.chop("abc")         = "ab"
     * StringUtil.chop("abc\nabc")    = "abc\nab"
     * StringUtil.chop("a")           = ""
     * StringUtil.chop("\r")          = ""
     * StringUtil.chop("\n")          = ""
     * StringUtil.chop("\r\n")        = ""
     * </pre>
     *
     * @param str
     *            the String to chop last character from, may be null
     * @return String without last character, {@code null} if null String input
     */
    public static String chop(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final int strLen = str.length();

        if (strLen < 2) {
            return N.EMPTY_STRING;
        }

        final int lastIdx = strLen - 1;

        if (str.charAt(lastIdx) == N.CHAR_LF && str.charAt(lastIdx - 1) == N.CHAR_CR) {
            return str.substring(0, lastIdx - 1);
        } else {
            return str.substring(0, lastIdx);
        }
    }

    public static void chop(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     * StringUtil.truncate(null, 0)       = null
     * StringUtil.truncate(null, 2)       = null
     * StringUtil.truncate("", 4)         = ""
     * StringUtil.truncate("abcdefg", 4)  = "abcd"
     * StringUtil.truncate("abcdefg", 6)  = "abcdef"
     * StringUtil.truncate("abcdefg", 7)  = "abcdefg"
     * StringUtil.truncate("abcdefg", 8)  = "abcdefg"
     * StringUtil.truncate("abcdefg", -1) = throws an IllegalArgumentException
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
     * StringUtil.truncate(null, 0, 0) = null
     * StringUtil.truncate(null, 2, 4) = null
     * StringUtil.truncate("", 0, 10) = ""
     * StringUtil.truncate("", 2, 10) = ""
     * StringUtil.truncate("abcdefghij", 0, 3) = "abc"
     * StringUtil.truncate("abcdefghij", 5, 6) = "fghij"
     * StringUtil.truncate("raspberry peach", 10, 15) = "peach"
     * StringUtil.truncate("abcdefghijklmno", 0, 10) = "abcdefghij"
     * StringUtil.truncate("abcdefghijklmno", -1, 10) = throws an IllegalArgumentException
     * StringUtil.truncate("abcdefghijklmno", Integer.MIN_VALUE, 10) = throws an IllegalArgumentException
     * StringUtil.truncate("abcdefghijklmno", Integer.MIN_VALUE, Integer.MAX_VALUE) = throws an IllegalArgumentException
     * StringUtil.truncate("abcdefghijklmno", 0, Integer.MAX_VALUE) = "abcdefghijklmno"
     * StringUtil.truncate("abcdefghijklmno", 1, 10) = "bcdefghijk"
     * StringUtil.truncate("abcdefghijklmno", 2, 10) = "cdefghijkl"
     * StringUtil.truncate("abcdefghijklmno", 3, 10) = "defghijklm"
     * StringUtil.truncate("abcdefghijklmno", 4, 10) = "efghijklmn"
     * StringUtil.truncate("abcdefghijklmno", 5, 10) = "fghijklmno"
     * StringUtil.truncate("abcdefghijklmno", 5, 5) = "fghij"
     * StringUtil.truncate("abcdefghijklmno", 5, 3) = "fgh"
     * StringUtil.truncate("abcdefghijklmno", 10, 3) = "klm"
     * StringUtil.truncate("abcdefghijklmno", 10, Integer.MAX_VALUE) = "klmno"
     * StringUtil.truncate("abcdefghijklmno", 13, 1) = "n"
     * StringUtil.truncate("abcdefghijklmno", 13, Integer.MAX_VALUE) = "no"
     * StringUtil.truncate("abcdefghijklmno", 14, 1) = "o"
     * StringUtil.truncate("abcdefghijklmno", 14, Integer.MAX_VALUE) = "o"
     * StringUtil.truncate("abcdefghijklmno", 15, 1) = ""
     * StringUtil.truncate("abcdefghijklmno", 15, Integer.MAX_VALUE) = ""
     * StringUtil.truncate("abcdefghijklmno", Integer.MAX_VALUE, Integer.MAX_VALUE) = ""
     * StringUtil.truncate("abcdefghij", 3, -1) = throws an IllegalArgumentException
     * StringUtil.truncate("abcdefghij", -2, 4) = throws an IllegalArgumentException
     * </pre>
     *
     * @param str the String to truncate, may be null
     * @param offset left edge of source String
     * @param maxWidth maximum length of result String, must be positive
     * @return truncated String, {@code null} if null String input
     * @throws IllegalArgumentException If {@code offset} or {@code maxWidth} is less than {@code 0}
     * @since 3.5
     */
    public static String truncate(final String str, final int offset, final int maxWidth) {
        N.checkArgNotNegative(offset, "offset");
        N.checkArgNotNegative(maxWidth, "maxWidth");

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

    // Delete
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Deletes all white spaces from a String as defined by
     * {@link Character#isWhitespace(char)}.
     * </p>
     *
     * <pre>
     * N.deleteWhitespace(null)         = null
     * N.deleteWhitespace("")           = ""
     * N.deleteWhitespace("abc")        = "abc"
     * N.deleteWhitespace("   ab  c  ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to delete whitespace from, may be null
     * @return
     */
    public static String deleteWhitespace(final String str) {
        if (N.isNullOrEmpty(str)) {
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

    public static void deleteWhitespace(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
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
     */
    public static String appendIfMissing(final String str, final String suffix) {
        N.checkArgNotNull(suffix);

        if (N.isNullOrEmpty(str)) {
            return suffix;
        } else if (str.endsWith(suffix)) {
            return str;
        } else {
            return str + suffix;
        }
    }

    /**
     *
     * @param str
     * @param suffix
     * @return
     */
    public static String appendIfMissingIgnoreCase(final String str, final String suffix) {
        N.checkArgNotNull(suffix);

        if (N.isNullOrEmpty(str)) {
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
     */
    public static String prependIfMissing(final String str, final String prefix) {
        N.checkArgNotNull(prefix);

        if (N.isNullOrEmpty(str)) {
            return prefix;
        } else if (str.startsWith(prefix)) {
            return str;
        } else {
            return prefix + str;
        }
    }

    public static String prependIfMissingIgnoreCase(final String str, final String prefix) {
        N.checkArgNotNull(prefix);

        if (N.isNullOrEmpty(str)) {
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
     * N.wrapIfMissing(null, "[", "]") -> "[]"
     * N.wrapIfMissing("", "[", "]") -> "[]"
     * N.wrapIfMissing("[", "[", "]") -> "[]"
     * N.wrapIfMissing("]", "[", "]") -> "[]"
     * N.wrapIfMissing("abc", "[", "]") -> "[abc]"
     * N.wrapIfMissing("a", "aa", "aa") -> "aaaaa"
     * N.wrapIfMissing("aa", "aa", "aa") -> "aaaa"
     * N.wrapIfMissing("aaa", "aa", "aa") -> "aaaaa"
     * N.wrapIfMissing("aaaa", "aa", "aa") -> "aaaa"
     * </pre>
     *
     * @param str
     * @param prefix
     * @param suffix
     * @return
     */
    public static String wrapIfMissing(final String str, final String prefix, final String suffix) {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (N.isNullOrEmpty(str)) {
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
     * N.wrap(null, "[", "]") -> "[]"
     * N.wrap("", "[", "]") -> "[]"
     * N.wrap("[", "[", "]") -> "[[]"
     * N.wrap("]", "[", "]") -> "[]]"
     * N.wrap("abc", "[", "]") -> "[abc]"
     * N.wrap("a", "aa", "aa") -> "aaaaa"
     * N.wrap("aa", "aa", "aa") -> "aaaaaa"
     * N.wrap("aaa", "aa", "aa") -> "aaaaaaa"
     * </pre>
     *
     * @param str
     * @param prefix
     * @param suffix
     * @return
     */
    public static String wrap(final String str, final String prefix, final String suffix) {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (N.isNullOrEmpty(str)) {
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
     *
     * <p>
     * Unwraps the specified string {@code str} if and only if it's wrapped by the specified {@code prefix} and {@code suffix}
     * </p>
     *
     * <pre>
     * N.unwrap(null, "[", "]") -> ""
     * N.unwrap("", "[", "]") -> ""
     * N.unwrap("[", "[", "]") -> "["
     * N.unwrap("]", "[", "]") -> "["
     * N.unwrap("[abc]", "[", "]") -> "abc"
     * N.unwrap("aaaaa", "aa", "aa") -> "a"
     * N.unwrap("aa", "aa", "aa") -> "aa"
     * N.unwrap("aaa", "aa", "aa") -> "aaa"
     * N.unwrap("aaaa", "aa", "aa") -> ""
     * </pre>
     *
     * @param str
     * @param prefix
     * @param suffix
     * @return
     */
    public static String unwrap(final String str, final String prefix, final String suffix) {
        N.checkArgNotNull(prefix);
        N.checkArgNotNull(suffix);

        if (N.isNullOrEmpty(str)) {
            return N.EMPTY_STRING;
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
     * @return true, if is lower case
     */
    public static boolean isLowerCase(final char ch) {
        return Character.isLowerCase(ch);
    }

    /**
     * Checks if is ascii lower case.
     *
     * @param ch
     * @return true, if is ascii lower case
     */
    public static boolean isAsciiLowerCase(final char ch) {
        return (ch >= 'a') && (ch <= 'z');
    }

    /**
     * Checks if is upper case.
     *
     * @param ch
     * @return true, if is upper case
     */
    public static boolean isUpperCase(final char ch) {
        return Character.isUpperCase(ch);
    }

    /**
     * Checks if is ascii upper case.
     *
     * @param ch
     * @return true, if is ascii upper case
     */
    public static boolean isAsciiUpperCase(final char ch) {
        return (ch >= 'A') && (ch <= 'Z');
    }

    /**
     * Checks if is all lower case.
     *
     * @param cs
     * @return true, if is all lower case
     */
    public static boolean isAllLowerCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (Character.isUpperCase(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is all upper case.
     *
     * @param cs
     * @return true, if is all upper case
     */
    public static boolean isAllUpperCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        for (int i = 0; i < len; i++) {
            if (Character.isLowerCase(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Copied from Apache Commons Lang: StringUtils#isMixedCase.
     *
     * @param cs
     * @return true, if is mixed case
     */
    public static boolean isMixedCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs) || cs.length() == 1) {
            return false;
        }

        boolean containsUppercase = false;
        boolean containsLowercase = false;
        final int len = cs.length();

        char ch = 0;

        for (int i = 0; i < len; i++) {
            if (containsUppercase && containsLowercase) {
                return true;
            }

            ch = cs.charAt(i);

            if (Character.isUpperCase(ch)) {
                containsUppercase = true;
            } else if (Character.isLowerCase(ch)) {
                containsLowercase = true;
            }
        }

        return containsUppercase && containsLowercase;
    }

    /**
     * Checks if is digit.
     *
     * @param ch
     * @return true, if is digit
     * @see Character#isDigit(char)
     */
    public static boolean isDigit(final char ch) {
        return Character.isDigit(ch);
    }

    /**
     * Checks if is letter.
     *
     * @param ch
     * @return true, if is letter
     * @see Character#isLetter(char)
     */
    public static boolean isLetter(final char ch) {
        return Character.isLetter(ch);
    }

    /**
     * Checks if is letter or digit.
     *
     * @param ch
     * @return true, if is letter or digit
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
     *   StringUtil.isAscii('a')  = true
     *   StringUtil.isAscii('A')  = true
     *   StringUtil.isAscii('3')  = true
     *   StringUtil.isAscii('-')  = true
     *   StringUtil.isAscii('\n') = true
     *   StringUtil.isAscii('&copy;') = false
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
     *   StringUtil.isAsciiPrintable('a')  = true
     *   StringUtil.isAsciiPrintable('A')  = true
     *   StringUtil.isAsciiPrintable('3')  = true
     *   StringUtil.isAsciiPrintable('-')  = true
     *   StringUtil.isAsciiPrintable('\n') = false
     *   StringUtil.isAsciiPrintable('&copy;') = false
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
     *   StringUtil.isAsciiControl('a')  = false
     *   StringUtil.isAsciiControl('A')  = false
     *   StringUtil.isAsciiControl('3')  = false
     *   StringUtil.isAsciiControl('-')  = false
     *   StringUtil.isAsciiControl('\n') = true
     *   StringUtil.isAsciiControl('&copy;') = false
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
     *   StringUtil.isAsciiAlpha('a')  = true
     *   StringUtil.isAsciiAlpha('A')  = true
     *   StringUtil.isAsciiAlpha('3')  = false
     *   StringUtil.isAsciiAlpha('-')  = false
     *   StringUtil.isAsciiAlpha('\n') = false
     *   StringUtil.isAsciiAlpha('&copy;') = false
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
     *   StringUtil.isAsciiAlphaUpper('a')  = false
     *   StringUtil.isAsciiAlphaUpper('A')  = true
     *   StringUtil.isAsciiAlphaUpper('3')  = false
     *   StringUtil.isAsciiAlphaUpper('-')  = false
     *   StringUtil.isAsciiAlphaUpper('\n') = false
     *   StringUtil.isAsciiAlphaUpper('&copy;') = false
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
     *   StringUtil.isAsciiAlphaLower('a')  = true
     *   StringUtil.isAsciiAlphaLower('A')  = false
     *   StringUtil.isAsciiAlphaLower('3')  = false
     *   StringUtil.isAsciiAlphaLower('-')  = false
     *   StringUtil.isAsciiAlphaLower('\n') = false
     *   StringUtil.isAsciiAlphaLower('&copy;') = false
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
     *   StringUtil.isAsciiNumeric('a')  = false
     *   StringUtil.isAsciiNumeric('A')  = false
     *   StringUtil.isAsciiNumeric('3')  = true
     *   StringUtil.isAsciiNumeric('-')  = false
     *   StringUtil.isAsciiNumeric('\n') = false
     *   StringUtil.isAsciiNumeric('&copy;') = false
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
     *   StringUtil.isAsciiAlphanumeric('a')  = true
     *   StringUtil.isAsciiAlphanumeric('A')  = true
     *   StringUtil.isAsciiAlphanumeric('3')  = true
     *   StringUtil.isAsciiAlphanumeric('-')  = false
     *   StringUtil.isAsciiAlphanumeric('\n') = false
     *   StringUtil.isAsciiAlphanumeric('&copy;') = false
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
     * Checks if is ascii printable.
     *
     * @param cs
     * @return true, if is ascii printable
     */
    public static boolean isAsciiPrintable(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @return true, if is ascii alpha
     */
    public static boolean isAsciiAlpha(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @return true, if is ascii alpha space
     */
    public static boolean isAsciiAlphaSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @return true, if is ascii alphanumeric
     */
    public static boolean isAsciiAlphanumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @return true, if is ascii alphanumeric space
     */
    public static boolean isAsciiAlphanumericSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @return true, if is ascii numeric
     */
    public static boolean isAsciiNumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * N.isAlpha(null)   = false
     * N.isAlpha("")     = false
     * N.isAlpha("  ")   = false
     * N.isAlpha("abc")  = true
     * N.isAlpha("ab2c") = false
     * N.isAlpha("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters, and is non-null
     * @since 3.0 Changed signature from isAlpha(String) to
     *        isAlpha(CharSequence)
     * @since 3.0 Changed "" to return false and not true
     */
    public static boolean isAlpha(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * N.isAlphaSpace(null)   = false
     * N.isAlphaSpace("")     = false
     * N.isAlphaSpace("  ")   = true
     * N.isAlphaSpace("abc")  = true
     * N.isAlphaSpace("ab c") = true
     * N.isAlphaSpace("ab2c") = false
     * N.isAlphaSpace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters and space, and is non-null
     * @since 3.0 Changed signature from isAlphaSpace(String) to
     *        isAlphaSpace(CharSequence)
     */
    public static boolean isAlphaSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * N.isAlphanumeric(null)   = false
     * N.isAlphanumeric("")     = false
     * N.isAlphanumeric("  ")   = false
     * N.isAlphanumeric("abc")  = true
     * N.isAlphanumeric("ab c") = false
     * N.isAlphanumeric("ab2c") = true
     * N.isAlphanumeric("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters or digits, and is non-null
     * @since 3.0 Changed signature from isAlphanumeric(String) to
     *        isAlphanumeric(CharSequence)
     * @since 3.0 Changed "" to return false and not true
     */
    public static boolean isAlphanumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * N.isAlphanumericSpace(null)   = false
     * N.isAlphanumericSpace("")     = false
     * N.isAlphanumericSpace("  ")   = true
     * N.isAlphanumericSpace("abc")  = true
     * N.isAlphanumericSpace("ab c") = true
     * N.isAlphanumericSpace("ab2c") = true
     * N.isAlphanumericSpace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains letters, digits or space, and is
     *         non-null
     * @since 3.0 Changed signature from isAlphanumericSpace(String) to
     *        isAlphanumericSpace(CharSequence)
     */
    public static boolean isAlphanumericSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * N.isNumeric(null)   = false
     * N.isNumeric("")     = false
     * N.isNumeric("  ")   = false
     * N.isNumeric("123")  = true
     * N.isNumeric("12 3") = false
     * N.isNumeric("ab2c") = false
     * N.isNumeric("12-3") = false
     * N.isNumeric("12.3") = false
     * N.isNumeric("-123") = false
     * N.isNumeric("+123") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains digits, and is non-null
     * @since 3.0 Changed signature from isNumeric(String) to
     *        isNumeric(CharSequence)
     * @since 3.0 Changed "" to return false and not true
     */
    public static boolean isNumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * <p>
     * Checks if the CharSequence contains only Unicode digits or space (
     * {@code ' '}). A decimal point is not a Unicode digit and returns false.
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * N.isNumericSpace(null)   = false
     * N.isNumericSpace("")     = false
     * N.isNumericSpace("  ")   = true
     * N.isNumericSpace("123")  = true
     * N.isNumericSpace("12 3") = true
     * N.isNumericSpace("ab2c") = false
     * N.isNumericSpace("12-3") = false
     * N.isNumericSpace("12.3") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains digits or space, and is non-null
     * @since 3.0 Changed signature from isNumericSpace(String) to
     *        isNumericSpace(CharSequence)
     */
    public static boolean isNumericSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * <p>
     * Checks if the CharSequence contains only whitespace.
     * </p>
     *
     * <p>
     * {@code null} or empty CharSequence (length()=0) will return {@code false}
     * .
     * </p>
     *
     * <pre>
     * N.isWhitespace(null)   = false
     * N.isWhitespace("")     = false
     * N.isWhitespace("  ")   = true
     * N.isWhitespace("abc")  = false
     * N.isWhitespace("ab2c") = false
     * N.isWhitespace("ab-c") = false
     * </pre>
     *
     * @param cs
     *            the CharSequence to check, may be null
     * @return {@code true} if only contains whitespace, and is non-null
     * @since 2.0
     * @since 3.0 Changed signature from isWhitespace(String) to
     *        isWhitespace(CharSequence)
     */
    public static boolean isWhitespace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
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
     * @param str
     *            the <code>String</code> to check
     * @return <code>true</code> if the string is a correctly formatted number
     * @since 3.3 the code supports hex {@code 0Xhhh} and octal {@code 0ddd}
     *        validation
     */
    public static boolean isNumber(final String str) {
        return Numbers.createNumber(str).isPresent();
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
     * @return true, if is ascii digtal number
     */
    public static boolean isAsciiDigtalNumber(final String str) {
        if (N.isNullOrEmpty(str)) {
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
        } else if (i == len) {
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
     * @return true, if is ascii digtal integer
     */
    public static boolean isAsciiDigtalInteger(final String str) {
        if (N.isNullOrEmpty(str)) {
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
     * @param targetChar
     * @return
     */
    public static int indexOf(final String str, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(targetChar);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param targetChar
     * @return
     */
    public static int indexOf(final String str, final int fromIndex, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(targetChar, fromIndex);
    }

    /**
     *
     * @param str
     * @param substr
     * @return
     */
    public static int indexOf(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(substr);
    }

    /**
     *
     * @param str
     * @param fromIndex
     * @param substr
     * @return
     */
    public static int indexOf(final String str, final int fromIndex, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(substr, fromIndex);
    }

    /**
     * Index of any.
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static int indexOfAny(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = chs.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (chs[j] == ch) {
                    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                        // ch is a supplementary character
                        if (chs[j + 1] == str.charAt(i + 1)) {
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
     * Index of any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
    public static int indexOfAny(final String str, final String... substrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substrs)) {
            return N.INDEX_NOT_FOUND;
        }

        int result = N.INDEX_NOT_FOUND;
        int tmp = 0;

        for (String substr : substrs) {
            if (N.isNullOrEmpty(substr)) {
                continue;
            }

            tmp = indexOf(str, substr);

            if (tmp == N.INDEX_NOT_FOUND) {
                continue;
            } else if (result == N.INDEX_NOT_FOUND || tmp < result) {
                result = tmp;
            }
        }

        return result;
    }

    /**
     * Index of any but.
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static int indexOfAnyBut(final String str, final char... chs) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        if (N.isNullOrEmpty(chs)) {
            return 0;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = chs.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        outer: for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (chs[j] == ch) {
                    if (i < strLast && j < chsLast && Character.isHighSurrogate(ch)) {
                        if (chs[j + 1] == str.charAt(i + 1)) {
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
     * @param substr
     * @param delimiter
     * @return
     */
    public static int indexOf(final String str, final String substr, final String delimiter) {
        return indexOf(str, 0, substr, delimiter);
    }

    /**
     *
     * @param str
     * @param fromIndex the index from which to start the search.
     * @param substr
     * @param delimiter
     * @return
     */
    public static int indexOf(final String str, final int fromIndex, final String substr, final String delimiter) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return N.INDEX_NOT_FOUND;
        }

        int index = str.indexOf(substr, fromIndex);

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        if (index + substr.length() == str.length()) {
            return index;
        } else if (str.length() >= index + substr.length() + delimiter.length()) {
            for (int i = 0, j = index + substr.length(), seperatorLen = delimiter.length(); i < seperatorLen;) {
                if (delimiter.charAt(i++) != str.charAt(j++)) {
                    return N.INDEX_NOT_FOUND;
                }
            }

            return index;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Index of ignore case.
     *
     * @param str
     * @param substr
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final String substr) {
        return indexOfIgnoreCase(str, 0, substr);
    }

    /**
     * Index of ignore case.
     *
     * @param str
     * @param fromIndex
     * @param substr
     * @return
     */
    public static int indexOfIgnoreCase(final String str, final int fromIndex, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = str.length(), substrLen = substr.length(), end = len - substrLen + 1; i < end; i++) {
            if (str.regionMatches(true, i, substr, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * <p>
     * Finds the n-th index within a String, handling {@code null}.
     * </p>
     *
     * @param str
     * @param substr
     * @param ordinal the n-th {@code searchStr} to find
     * @return
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int ordinalIndexOf(final String str, final String substr, final int ordinal) {
        return ordinalIndexOf(str, substr, ordinal, false);
    }

    /**
     * Last index of.
     *
     * @param str
     * @param targetChar
     * @return
     */
    public static int lastIndexOf(final String str, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(targetChar);
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified character, searching backward starting at the specified index.
     * For values of <code>ch</code> in the range from 0 to 0xFFFF (inclusive),
     * the index returned is the largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * (this.charAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)
     * </pre>
     *
     * </blockquote> is true. For other values of <code>ch</code>, it is the
     * largest value <i>k</i> such that: <blockquote>
     *
     * <pre>
     * (this.codePointAt(<i>k</i>) == ch) && (<i>k</i> &lt;= fromIndex)
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
     * @param fromIndex the index to start the search from. There is no restriction on
     *            the value of <code>fromIndex</code>. If it is greater than or
     *            equal to the length of this string, it has the same effect as
     *            if it were equal to one less than the length of this string:
     *            this entire string may be searched. If it is negative, it has
     *            the same effect as if it were -1: -1 is returned.
     * @param targetChar a character (Unicode code point).
     * @return
     *         character sequence represented by this object that is less than
     *         or equal to <code>fromIndex</code>, or <code>-1</code> if the
     *         character does not occur before that point.
     */
    public static int lastIndexOf(final String str, final int fromIndex, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(targetChar, fromIndex);
    }

    /**
     * Last index of.
     *
     * @param str
     * @param substr
     * @return
     */
    public static int lastIndexOf(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(substr);
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
     * <i>k</i> &lt;= fromIndex && str.startsWith(substr, <i>k</i>)
     * </pre>
     *
     * </blockquote> If no such value of <i>k</i> exists, then {@code -1} is
     * returned.
     *
     * @param str
     * @param fromIndex
     * @param substr
     * @return
     */
    public static int lastIndexOf(final String str, final int fromIndex, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.lastIndexOf(substr, fromIndex);
    }

    /**
     * Last index of any.
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static int lastIndexOfAny(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return N.INDEX_NOT_FOUND;
        }

        int result = N.INDEX_NOT_FOUND;
        int tmp = 0;

        for (char ch : chs) {
            tmp = str.lastIndexOf(ch);

            if (tmp == N.INDEX_NOT_FOUND) {
                continue;
            } else if (result == N.INDEX_NOT_FOUND || tmp > result) {
                result = tmp;
            }
        }

        return result;
    }

    /**
     * Last index of any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
    public static int lastIndexOfAny(final String str, final String... substrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substrs)) {
            return N.INDEX_NOT_FOUND;
        }

        int result = N.INDEX_NOT_FOUND;
        int tmp = 0;

        for (String substr : substrs) {
            if (N.isNullOrEmpty(substr)) {
                continue;
            }

            tmp = str.lastIndexOf(substr);

            if (tmp == N.INDEX_NOT_FOUND) {
                continue;
            } else if (result == N.INDEX_NOT_FOUND || tmp > result) {
                result = tmp;
            }
        }

        return result;
    }

    /**
     * Last index of.
     *
     * @param str
     * @param substr
     * @param delimiter
     * @return
     */
    public static int lastIndexOf(final String str, final String substr, final String delimiter) {
        return lastIndexOf(str, str.length(), substr, delimiter);
    }

    /**
     * Last index of.
     *
     * @param str
     * @param fromIndex the start index to traverse backwards from
     * @param substr
     * @param delimiter
     * @return
     */
    public static int lastIndexOf(final String str, final int fromIndex, final String substr, final String delimiter) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return N.INDEX_NOT_FOUND;
        }

        // int index = str.lastIndexOf(substr, min(fromIndex, str.length() -
        // 1)); // Refer to String.lastIndexOf(String, int). the result is same
        // as below line.
        int index = str.lastIndexOf(substr, N.min(fromIndex, str.length()));

        if (index < 0) {
            return N.INDEX_NOT_FOUND;
        }

        if (index + substr.length() == str.length()) {
            return index;
        } else if (str.length() >= index + substr.length() + delimiter.length()) {
            for (int i = 0, j = index + substr.length(), len = delimiter.length(); i < len;) {
                if (delimiter.charAt(i++) != str.charAt(j++)) {
                    return N.INDEX_NOT_FOUND;
                }
            }

            return index;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of ignore case.
     *
     * @param str
     * @param substr
     * @return
     */
    public static int lastIndexOfIgnoreCase(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return lastIndexOfIgnoreCase(str, str.length(), substr);
    }

    /**
     * Last index of ignore case.
     *
     * @param str
     * @param fromIndex
     * @param substr
     * @return
     */
    public static int lastIndexOfIgnoreCase(final String str, final int fromIndex, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, str.length() - substr.length()), substrLen = substr.length(); i >= 0; i--) {
            if (str.regionMatches(true, i, substr, 0, substrLen)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * <p>
     * Finds the n-th last index within a String, handling {@code null}.
     * </p>
     *
     * @param str
     * @param substr
     * @param ordinal the n-th last {@code searchStr} to find
     * @return
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int lastOrdinalIndexOf(final String str, final String substr, final int ordinal) {
        return ordinalIndexOf(str, substr, ordinal, true);
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

        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
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
     *
     * @param str
     * @param substr
     * @return
     * @see N#occurrencesOf(String, String)
     */
    public static int occurrencesOf(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return 0;
        }

        int occurrences = 0;

        for (int len = N.len(str), substrLen = N.len(substr), index = 0, fromIndex = 0, toIndex = len - substrLen; fromIndex <= toIndex;) {
            index = str.indexOf(substr, fromIndex);

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
     * @param targetChar
     * @return
     */
    public static boolean contains(final String str, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        return indexOf(str, targetChar) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param substr
     * @return
     */
    public static boolean contains(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOf(str, substr) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param substr
     * @param delimiter
     * @return
     */
    public static boolean contains(final String str, final String substr, final String delimiter) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOf(str, substr, delimiter) != N.INDEX_NOT_FOUND;
    }

    /**
     * Contains ignore case.
     *
     * @param str
     * @param substr
     * @return
     */
    public static boolean containsIgnoreCase(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOfIgnoreCase(str, substr) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static boolean containsAny(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return false;
        }

        return indexOfAny(str, chs) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param searchStrs
     * @return
     */
    @SafeVarargs
    public static boolean containsAny(final String str, final String... searchStrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(searchStrs)) {
            return false;
        }

        return indexOfAny(str, searchStrs) != N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param searchStrs
     * @return
     */
    @SafeVarargs
    public static boolean containsAnyIgnoreCase(final String str, final String... searchStrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(searchStrs)) {
            return false;
        } else if (searchStrs.length == 1) {
            return containsIgnoreCase(str, searchStrs[0]);
        } else if (searchStrs.length == 2) {
            if (containsIgnoreCase(str, searchStrs[0])) {
                return true;
            }

            return containsIgnoreCase(str, searchStrs[1]);
        }

        final String sourceText = str.toLowerCase();

        for (String searchStr : searchStrs) {
            if (N.notNullOrEmpty(searchStr) && indexOf(sourceText, searchStr.toLowerCase()) != N.INDEX_NOT_FOUND) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static boolean containsOnly(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return false;
        }

        return indexOfAnyBut(str, chs) == N.INDEX_NOT_FOUND;
    }

    /**
     *
     * @param str
     * @param chs
     * @return
     */
    @SafeVarargs
    public static boolean containsNone(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return true;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = chs.length;
        final int chsLast = chsLen - 1;
        char ch = 0;

        for (int i = 0; i < strLen; i++) {
            ch = str.charAt(i);

            for (int j = 0; j < chsLen; j++) {
                if (chs[j] == ch) {
                    if (Character.isHighSurrogate(ch)) {
                        if ((j == chsLast) || (i < strLast && chs[j + 1] == str.charAt(i + 1))) {
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
        if (N.isNullOrEmpty(str)) {
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
        if (str == null || N.isNullOrEmpty(substrs)) {
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
     * Ends with any.
     *
     * @param str
     * @param substrs
     * @return
     */
    @SafeVarargs
    public static boolean endsWithAny(final String str, final String... substrs) {
        if (str == null || N.isNullOrEmpty(substrs)) {
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
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
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
        if (N.isNullOrEmpty(searchStrings)) {
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
        if (N.isNullOrEmpty(searchStrs)) {
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
     * N.indexOfDifference(null, null) = -1
     * N.indexOfDifference("", "") = -1
     * N.indexOfDifference("", "abc") = 0
     * N.indexOfDifference("abc", "") = 0
     * N.indexOfDifference("abc", "abc") = -1
     * N.indexOfDifference("ab", "abxyz") = 2
     * N.indexOfDifference("abcde", "abxyz") = 2
     * N.indexOfDifference("abcde", "xyz") = 0
     * </pre>
     *
     * @param a
     *            the first String, may be null
     * @param b
     *            the second String, may be null
     * @return
     */
    public static int indexOfDifference(final String a, final String b) {
        if (N.equals(a, b) || (N.isNullOrEmpty(a) && N.isNullOrEmpty(b))) {
            return N.INDEX_NOT_FOUND;
        }

        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
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
     * N.indexOfDifference(null) = -1
     * N.indexOfDifference(new String[] {}) = -1
     * N.indexOfDifference(new String[] {"abc"}) = -1
     * N.indexOfDifference(new String[] {null, null}) = -1
     * N.indexOfDifference(new String[] {"", ""}) = -1
     * N.indexOfDifference(new String[] {"", null}) = -1
     * N.indexOfDifference(new String[] {"abc", null, null}) = 0
     * N.indexOfDifference(new String[] {null, null, "abc"}) = 0
     * N.indexOfDifference(new String[] {"", "abc"}) = 0
     * N.indexOfDifference(new String[] {"abc", ""}) = 0
     * N.indexOfDifference(new String[] {"abc", "abc"}) = -1
     * N.indexOfDifference(new String[] {"abc", "a"}) = 1
     * N.indexOfDifference(new String[] {"ab", "abxyz"}) = 2
     * N.indexOfDifference(new String[] {"abcde", "abxyz"}) = 2
     * N.indexOfDifference(new String[] {"abcde", "xyz"}) = 0
     * N.indexOfDifference(new String[] {"xyz", "abcde"}) = 0
     * N.indexOfDifference(new String[] {"i am a machine", "i am a robot"}) = 7
     * </pre>
     *
     * @param strs
     *            array of Strings, entries may be null
     * @return
     *         equal or null/empty
     */
    @SafeVarargs
    public static int indexOfDifference(final String... strs) {
        if (N.isNullOrEmpty(strs) || strs.length == 1) {
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
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_STRING;
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
            return a.toString();
        } else if (p == b.length()) {
            return b.toString();
        } else {
            return a.subSequence(0, p).toString();
        }
    }

    /**
     *
     * @param strs
     * @return
     */
    @SafeVarargs
    public static String commonPrefix(final String... strs) {
        if (N.isNullOrEmpty(strs)) {
            return N.EMPTY_STRING;
        }

        if (strs.length == 1) {
            return N.isNullOrEmpty(strs[0]) ? N.EMPTY_STRING : strs[0];
        }

        String commonPrefix = commonPrefix(strs[0], strs[1]);

        if (N.isNullOrEmpty(commonPrefix)) {
            return N.EMPTY_STRING;
        }

        for (int i = 2, len = strs.length; i < len; i++) {
            commonPrefix = commonPrefix(commonPrefix, strs[i]);

            if (N.isNullOrEmpty(commonPrefix)) {
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
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_STRING;
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
            return a.toString();
        } else if (s == bLength) {
            return b.toString();
        } else {
            return a.subSequence(aLength - s, aLength).toString();
        }
    }

    /**
     *
     * @param strs
     * @return
     */
    @SafeVarargs
    public static String commonSuffix(final String... strs) {
        if (N.isNullOrEmpty(strs)) {
            return N.EMPTY_STRING;
        }

        if (strs.length == 1) {
            return N.isNullOrEmpty(strs[0]) ? N.EMPTY_STRING : strs[0];
        }

        String commonSuffix = commonSuffix(strs[0], strs[1]);

        if (N.isNullOrEmpty(commonSuffix)) {
            return N.EMPTY_STRING;
        }

        for (int i = 2, len = strs.length; i < len; i++) {
            commonSuffix = commonSuffix(commonSuffix, strs[i]);

            if (N.isNullOrEmpty(commonSuffix)) {
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
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_STRING;
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
            return N.EMPTY_STRING;
        }

        return a.substring(endIndex - maxLen, endIndex);
    }

    /**
     *
     * @param str
     * @param ch
     * @return
     */
    public static int countMatches(final String str, final char ch) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        int count = 0;

        for (int i = 0, len = str.length(); i < len; i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * @param str
     * @param substr
     * @return
     */
    public static int countMatches(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return 0;
        }

        int count = 0;
        int index = 0;

        while ((index = str.indexOf(substr, index)) != N.INDEX_NOT_FOUND) {
            count++;
            index += substr.length();
        }

        return count;
    }

    /**
     * Returns {@code null} if {@code inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex},
     * otherwise the {@code substring} with String value: {@code str.substring(exclusiveBeginIndex, exclusiveEndIndex)} is returned.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    public static String substring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex) {
            return null;
        }

        return str.substring(inclusiveBeginIndex, exclusiveEndIndex);
    }

    /**
     * Returns {@code null} if {@code inclusiveBeginIndex < 0},
     * otherwise the {@code substring} with String value: {@code str.substring(inclusiveBeginIndex)} is returned.
     *
     * @param str
     * @param inclusiveBeginIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, int inclusiveBeginIndex) {
        if (str == null || inclusiveBeginIndex < 0) {
            return null;
        }

        return str.substring(inclusiveBeginIndex);
    }

    /**
     * Returns {@code null} if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise the {@code substring} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substring(String, int)
     */
    public static String substring(String str, char delimiterOfInclusiveBeginIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        return substring(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns {@code null} if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
     * otherwise the {@code substring} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substring(String, int)
     */
    public static String substring(String str, String delimiterOfInclusiveBeginIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null) {
            return null;
        }

        return substring(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || inclusiveBeginIndex < 0) {
            return null;
        }

        return substring(str, inclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1));
    }

    /**
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0 || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        if (delimiterOfExclusiveEndIndex.length() == 0) {
            return str.substring(inclusiveBeginIndex, inclusiveBeginIndex);
        } else {
            return substring(str, inclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1));
        }
    }

    /**
     *
     * @param str
     * @param inclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || inclusiveBeginIndex < 0) {
            return null;
        }

        return substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex));
    }

    /**
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveEndIndex <= 0) {
            return null;
        }

        return substring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || delimiterOfInclusiveBeginIndex == null || exclusiveEndIndex < 0) {
            return null;
        }

        if (delimiterOfInclusiveBeginIndex.length() == 0) {
            return str.substring(exclusiveEndIndex, exclusiveEndIndex);
        } else if (exclusiveEndIndex == 0) {
            return null;
        } else {
            return substring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
        }
    }

    /**
     *
     * @param str
     * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static String substring(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || exclusiveEndIndex < 0) {
            return null;
        }

        return substring(str, funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    /**
     * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @return
     */
    public static String substringAfter(String str, String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveBeginIndex);

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
    public static String substringAfterLast(String str, String delimiterOfExclusiveBeginIndex) {
        if (str == null || delimiterOfExclusiveBeginIndex == null) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(index + delimiterOfExclusiveBeginIndex.length());
    }

    /**
     * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
     *
     * @param str
     * @param delimiterOfExclusiveEndIndex
     * @return
     */
    public static String substringBefore(String str, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        int index = str.indexOf(delimiterOfExclusiveEndIndex);

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
    public static String substringBeforeLast(String str, String delimiterOfExclusiveEndIndex) {
        if (str == null || delimiterOfExclusiveEndIndex == null) {
            return null;
        }

        int index = str.lastIndexOf(delimiterOfExclusiveEndIndex);

        if (index < 0) {
            return null;
        }

        return str.substring(0, index);
    }

    /**
     * Returns {@code null} if {@code exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex},
     * otherwise the {@code substring} with String value: {@code str.substring(exclusiveBeginIndex + 1, exclusiveEndIndex)} is returned.
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    public static String substringBetween(String str, int exclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex) {
            return null;
        }

        return str.substring(exclusiveBeginIndex + 1, exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, beginIndex + 1) if exclusiveBeginIndex >= 0}
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, int exclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveBeginIndex < 0) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, exclusiveBeginIndex + 1));
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, beginIndex + 1) if exclusiveBeginIndex >= 0}
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, int exclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveBeginIndex < 0 || delimiterOfExclusiveEndIndex == null || delimiterOfExclusiveEndIndex.length() == 0) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, exclusiveBeginIndex + 1));
    }

    /**
     *
     * @param str
     * @param exclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, int exclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveBeginIndex < 0) {
            return null;
        }

        return substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex.applyAsInt(exclusiveBeginIndex));
    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex {@code exclusiveBeginIndex <- str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, char delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveEndIndex <= 0) {
            return null;
        }

        return substringBetween(str, str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex {@code exclusiveBeginIndex <- str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1) + delimiterOfExclusiveBeginIndex.length() - 1 if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveBeginIndex.length() == 0
                || exclusiveEndIndex <= 0) {
            return null;
        }

        final int index = str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1);
        final int exclusiveBeginIndex = index >= 0 ? index + delimiterOfExclusiveBeginIndex.length() - 1 : index;

        return substringBetween(str, exclusiveBeginIndex, exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param funcOfExclusiveBeginIndex {@code exclusiveBeginIndex <- funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (str == null || str.length() == 0 || exclusiveEndIndex <= 0) {
            return null;
        }

        return substringBetween(str, funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveBeginIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, char delimiterOfExclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0) {
            return null;
        }

        final int start = str.indexOf(delimiterOfExclusiveBeginIndex);

        if (start < 0) {
            return null;
        }

        final int end = str.indexOf(delimiterOfExclusiveEndIndex, start + 1);

        if (end < 0) {
            return null;
        }

        return substringBetween(str, start, end);
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
     * @param str
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveBeginIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        return substringBetween(str, 0, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex);
    }

    /**
     *
     * @param str
     * @param fromIndex start index for {@code delimiterOfExclusive}. {@code str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex)}
     * @param delimiterOfExclusiveBeginIndex
     * @param delimiterOfExclusiveBeginIndex
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static String substringBetween(String str, int fromIndex, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (str == null || str.length() == 0 || delimiterOfExclusiveBeginIndex == null || delimiterOfExclusiveBeginIndex.length() == 0
                || delimiterOfExclusiveEndIndex == null || delimiterOfExclusiveEndIndex.length() == 0) {
            return null;
        }

        final int start = fromIndex == 0 ? str.indexOf(delimiterOfExclusiveBeginIndex) : str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex);

        if (start < 0) {
            return null;
        }

        final int end = str.indexOf(delimiterOfExclusiveEndIndex, start + delimiterOfExclusiveBeginIndex.length());

        if (end < 0) {
            return null;
        }

        return substringBetween(str, start + delimiterOfExclusiveBeginIndex.length() - 1, end);
    }

    /**
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final char prefix, final char postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<>() : findAllIndicesBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final int fromIndex, final int toIndex, final char prefix, final char postfix) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        final List<IntPair> res = new ArrayList<>();

        if (N.isNullOrEmpty(str)) {
            return res;
        }

        int idx = str.indexOf(prefix, fromIndex);

        if (idx < 0) {
            return res;
        }

        final Deque<Integer> queue = new LinkedList<>();
        char ch = 0;

        for (int i = idx; i < toIndex; i++) {
            ch = str.charAt(i);

            if (ch == prefix) {
                queue.push(i + 1);
            } else if (ch == postfix && queue.size() > 0) {
                final int startIndex = queue.pop();

                if (res.size() > 0 && startIndex < res.get(res.size() - 1)._1) {
                    while (res.size() > 0 && startIndex < res.get(res.size() - 1)._1) {
                        res.remove(res.size() - 1);
                    }
                }

                res.add(IntPair.of(startIndex, i));
            }
        }

        return res;
    }

    /**
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final String prefix, final String postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<>() : findAllIndicesBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final int fromIndex, final int toIndex, final String prefix, final String postfix) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        final List<IntPair> res = new ArrayList<>();

        if (N.isNullOrEmpty(str)) {
            return res;
        }

        int idx = str.indexOf(prefix, fromIndex);

        if (idx < 0) {
            return res;
        }

        final Deque<Integer> queue = new LinkedList<>();
        queue.add(idx + prefix.length());
        int next = -1;

        for (int i = idx + prefix.length(), len = toIndex; i < len;) {
            if (queue.size() == 0) {
                idx = next >= i ? next : str.indexOf(prefix, i);

                if (idx < 0) {
                    break;
                } else {
                    queue.add(idx + prefix.length());
                    i = idx + prefix.length();
                }
            }

            idx = str.indexOf(postfix, i);

            if (idx < 0) {
                break;
            } else {
                final int endIndex = idx;
                idx = res.size() > 0 ? Math.max(res.get(res.size() - 1)._2 + postfix.length(), queue.peekLast()) : queue.peekLast();

                while ((idx = str.indexOf(prefix, idx)) >= 0 && idx < endIndex) {
                    queue.push(idx + prefix.length());
                    idx = idx + prefix.length();
                }

                if (idx > 0) {
                    next = idx;
                }

                final int startIndex = queue.pop();

                if (res.size() > 0 && startIndex < res.get(res.size() - 1)._1) {
                    while (res.size() > 0 && startIndex < res.get(res.size() - 1)._1) {
                        res.remove(res.size() - 1);
                    }
                }

                res.add(IntPair.of(startIndex, endIndex));

                i = endIndex + postfix.length();
            }
        }

        return res;
    }

    /**
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final char prefix, final char postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<>() : findAllSubstringsBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final int fromIndex, final int toIndex, final char prefix, final char postfix) {
        final List<IntPair> points = findAllIndicesBetween(str, fromIndex, toIndex, prefix, postfix);
        final List<String> res = new ArrayList<>(points.size());

        for (IntPair p : points) {
            res.add(str.substring(p._1, p._2));
        }

        return res;
    }

    /**
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final String prefix, final String postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<>() : findAllSubstringsBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final int fromIndex, final int toIndex, final String prefix, final String postfix) {
        final List<IntPair> points = findAllIndicesBetween(str, fromIndex, toIndex, prefix, postfix);
        final List<String> res = new ArrayList<>(points.size());

        for (IntPair p : points) {
            res.add(str.substring(p._1, p._2));
        }

        return res;
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
     * Returns the first {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or null, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     */
    public static String firstChars(final String str, final int n) {
        N.checkArgNotNegative(n, "n");

        if (str == null || str.length() == 0 || n == 0) {
            return N.EMPTY_STRING;
        } else if (str.length() <= n) {
            return str;
        } else {
            return str.substring(0, n);
        }
    }

    /**
     * Returns the last {@code n} chars of the specified {@code String} if its length is bigger than {@code n},
     * or an empty String {@code ""} if {@code str} is empty or null, or itself it's length equal to or less than {@code n}.
     *
     * @param str
     * @param n
     * @return
     */
    public static String lastChars(final String str, final int n) {
        N.checkArgNotNegative(n, "n");

        if (str == null || str.length() == 0 || n == 0) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final char[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final byte[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final short[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final int[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final long[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final float[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public static String join(final double[] a, final int fromIndex, final int toIndex, final String delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.isNullOrEmpty(delimiter)) {
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final Object[] a, final String delimiter, final String prefix, final String suffix) {
        return join(a, 0, N.len(a), delimiter, prefix, suffix, false);
    }

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter, final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return trim ? N.toString(a[fromIndex]).trim() : N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (i > fromIndex) {
                    sb.append(delimiter);
                }

                sb.append(trim ? N.toString(a[i]).trim() : N.toString(a[i]));
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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            if (N.isNullOrEmpty(prefix) && N.isNullOrEmpty(suffix)) {
                return N.EMPTY_STRING;
            } else if (N.isNullOrEmpty(prefix)) {
                return suffix;
            } else if (N.isNullOrEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        } else if (toIndex - fromIndex == 1 && N.isNullOrEmpty(prefix) && N.isNullOrEmpty(suffix)) {
            return trim ? N.toString(a[fromIndex]).trim() : N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.notNullOrEmpty(prefix)) {
                sb.append(prefix);
            }

            if (N.isNullOrEmpty(delimiter)) {
                for (int i = fromIndex; i < toIndex; i++) {
                    sb.append(trim ? N.toString(a[i]).trim() : N.toString(a[i]));
                }
            } else {
                for (int i = fromIndex; i < toIndex; i++) {
                    if (i > fromIndex) {
                        sb.append(delimiter);
                    }

                    sb.append(trim ? N.toString(a[i]).trim() : N.toString(a[i]));
                }
            }

            if (N.notNullOrEmpty(suffix)) {
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

    public static String join(final Iterable<?> c, final String delimiter, final String prefix, final String suffix) {
        return join(c == null ? null : c.iterator(), delimiter, prefix, suffix);
    }

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
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param trim
     * @return
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter, final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (Object e : c) {
                if (i++ > fromIndex) {
                    sb.append(delimiter);
                }

                if (i > fromIndex) {
                    sb.append(trim ? N.toString(e).trim() : N.toString(e));
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
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param trim
     * @return
     */
    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final String prefix, final String suffix,
            final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c) || fromIndex == toIndex) {
            if (N.isNullOrEmpty(prefix) && N.isNullOrEmpty(suffix)) {
                return N.EMPTY_STRING;
            } else if (N.isNullOrEmpty(prefix)) {
                return suffix;
            } else if (N.isNullOrEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.notNullOrEmpty(prefix)) {
                sb.append(prefix);
            }

            if (c instanceof List && c instanceof RandomAccess) {
                final List<?> list = (List<?>) c;

                if (N.isNullOrEmpty(delimiter)) {
                    for (int i = fromIndex; i < toIndex; i++) {
                        sb.append(trim ? N.toString(list.get(i)).trim() : N.toString(list.get(i)));
                    }
                } else {
                    for (int i = fromIndex; i < toIndex; i++) {
                        if (i > fromIndex) {
                            sb.append(delimiter);
                        }

                        sb.append(trim ? N.toString(list.get(i)).trim() : N.toString(list.get(i)));
                    }
                }
            } else {
                int i = 0;
                if (N.isNullOrEmpty(delimiter)) {
                    for (Object e : c) {
                        if (i++ >= fromIndex) {
                            sb.append(trim ? N.toString(e).trim() : N.toString(e));
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
                            sb.append(trim ? N.toString(e).trim() : N.toString(e));
                        }

                        if (i >= toIndex) {
                            break;
                        }
                    }
                }
            }

            if (N.notNullOrEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String join(final Iterator<?> iter) {
        return join(iter, Strings.ELEMENT_SEPARATOR);
    }

    public static String join(final Iterator<?> iter, final char delimiter) {
        if (iter == null) {
            return N.EMPTY_STRING;
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

    public static String join(final Iterator<?> iter, final String delimiter) {
        return join(iter, delimiter, EMPTY, EMPTY, false);
    }

    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix) {
        return join(iter, delimiter, prefix, suffix, false);
    }

    public static String join(final Iterator<?> iter, final String delimiter, final String prefix, final String suffix, final boolean trim) {
        if (iter == null) {
            return N.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.notNullOrEmpty(prefix)) {
                sb.append(prefix);
            }

            if (N.isNullOrEmpty(delimiter)) {
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

            if (N.notNullOrEmpty(suffix)) {
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
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
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
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, false);
    }

    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter, final String prefix, final String suffix,
            final boolean trim) {
        return joinEntries(m, 0, N.size(m), entryDelimiter, keyValueDelimiter, prefix, suffix, trim);
    }

    public static <K, V, E extends Exception, E2 extends Exception> String joinEntries(final Map<K, V> m, final String entryDelimiter,
            final String keyValueDelimiter, final String prefix, final String suffix, final boolean trim, final Throwables.Function<? super K, ?, E> keyMapper,
            final Throwables.Function<? super V, ?, E2> valueMapper) throws E, E2 {
        N.checkArgNotNull(keyMapper, "keyMapper");
        N.checkArgNotNull(valueMapper, "valueMapper");

        if (N.isNullOrEmpty(m)) {
            if (N.isNullOrEmpty(prefix) && N.isNullOrEmpty(suffix)) {
                return N.EMPTY_STRING;
            } else if (N.isNullOrEmpty(prefix)) {
                return suffix;
            } else if (N.isNullOrEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.notNullOrEmpty(prefix)) {
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

            if (N.notNullOrEmpty(suffix)) {
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
     * @param m
     * @param fromIndex
     * @param toIndex
     * @param entryDelimiter
     * @param keyValueDelimiter
     * @param trim
     * @return
     */
    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter,
            final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isNullOrEmpty(m) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;

            for (Map.Entry<?, ?> entry : m.entrySet()) {
                if (i++ > fromIndex) {
                    sb.append(entryDelimiter);
                }

                if (i > fromIndex) {
                    sb.append(trim ? N.toString(entry.getKey()).trim() : N.toString(entry.getKey()));
                    sb.append(keyValueDelimiter);
                    sb.append(trim ? N.toString(entry.getValue()).trim() : N.toString(entry.getValue()));
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

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
            final String prefix, final String suffix, final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(m));

        if (N.isNullOrEmpty(m) || fromIndex == toIndex) {
            if (N.isNullOrEmpty(prefix) && N.isNullOrEmpty(suffix)) {
                return N.EMPTY_STRING;
            } else if (N.isNullOrEmpty(prefix)) {
                return suffix;
            } else if (N.isNullOrEmpty(suffix)) {
                return prefix;
            } else {
                return prefix + suffix;
            }
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            if (N.notNullOrEmpty(prefix)) {
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

            if (N.notNullOrEmpty(suffix)) {
                sb.append(suffix);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Returns <code>a + b</code>.
     *
     * @param a
     * @param b
     * @return
     */
    public static String concat(final String a, final String b) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static String concat(final String a, final String b, final String c) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).append(h).toString();
        } finally {
            Objectory.recycle(sb);
        }
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
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).append(h).append(i).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static String concat(final String... a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        } else if (a.length == 1) {
            return N.toString(a[0]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (String e : a) {
                sb.append(e);
            }
            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
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
    //        if (N.isNullOrEmpty(a)) {
    //            return N.EMPTY_STRING;
    //        } else if (a.getClass().equals(String[].class)) {
    //            return StringUtil.concat((String[]) a);
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
    //        if (N.isNullOrEmpty(c)) {
    //            return N.EMPTY_STRING;
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
            Logger.getLogger("com.google.common.base.Strings").log(WARNING, "Exception during lenientFormat for " + objectToString, e);
            return "<" + objectToString + " threw " + e.getClass().getName() + ">";
        }
    }

    /**
     *
     * @param str
     * @return
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
     * N.reverseDelimited(null, *)      = null
     * N.reverseDelimited("", *)        = ""
     * N.reverseDelimited("a.b.c", 'x') = "a.b.c"
     * N.reverseDelimited("a.b.c", ".") = "c.b.a"
     * </pre>
     *
     * @param str
     *            the String to reverse, may be null
     * @param delimiter
     *            the delimiter character to use
     * @return
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
     * @return
     */
    public static String reverseDelimited(final String str, final String delimiter) {
        if (N.len(str) <= 1) {
            return str;
        }

        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        final String[] strs = split(str, delimiter);

        N.reverse(strs);

        return Joiner.with(delimiter).reuseCachedBuffer().appendAll(strs).toString();
    }

    /**
     * Returns a new sorted String if the specified {@code str} is not null or empty, otherwise the specified {@code str} is returned.
     *
     * @param str
     * @return
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
     * StringUtil.rotate(null, *)        = null
     * StringUtil.rotate("", *)          = ""
     * StringUtil.rotate("abcdefg", 0)   = "abcdefg"
     * StringUtil.rotate("abcdefg", 2)   = "fgabcde"
     * StringUtil.rotate("abcdefg", -2)  = "cdefgab"
     * StringUtil.rotate("abcdefg", 7)   = "abcdefg"
     * StringUtil.rotate("abcdefg", -7)  = "abcdefg"
     * StringUtil.rotate("abcdefg", 9)   = "fgabcde"
     * StringUtil.rotate("abcdefg", -9)  = "cdefgab"
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

        return Strings.substring(str, offset) + Strings.substring(str, 0, offset);
    }

    public static String shuffle(final String str) {
        return shuffle(str, N.RAND);
    }

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
     * StringUtil.overlay(null, "abc", 0, 0)          = "abc"
     * StringUtil.overlay("", "abc", 0, 0)          = "abc"
     * StringUtil.overlay("abcdef", null, 2, 4)     = "abef"
     * StringUtil.overlay("abcdef", "", 2, 4)       = "abef"
     * StringUtil.overlay("abcdef", "zzzz", 2, 4)   = "abzzzzef"
     * </pre>
     *
     * @param str the String to do overlaying in, may be null
     * @param overlay the String to overlay, may be null
     * @param start the position to start overlaying at
     * @param end the position to stop overlaying before
     * @return overlayed String, {@code ""} if null String input
     * @since 2.0
     */
    public static String overlay(String str, String overlay, int start, int end) {
        N.checkFromToIndex(start, end, N.len(str));

        if (overlay == null) {
            overlay = N.EMPTY_STRING;
        }

        if (N.isNullOrEmpty(str)) {
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

    public static final class StringUtil extends Strings {
        private StringUtil() {
            // Utility class.
        }
    }

    public static final class MoreStringUtil {
        private MoreStringUtil() {
            // Utility class.
        }

        /**
         * Returns an empty <code>Optional</code> if {@code inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex},
         * otherwise an {@code Optional} with String value: {@code str.substring(exclusiveBeginIndex, exclusiveEndIndex)} is returned.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, int, int)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns an empty <code>Optional</code> if {@code inclusiveBeginIndex < 0},
         * otherwise an {@code Optional} with String value: {@code str.substring(inclusiveBeginIndex)} is returned.
         *
         * @param str
         * @param inclusiveBeginIndex
         * @return
         * @see Strings#substring(String, int)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex));
        }

        /**
         * Returns an empty <code>Optional</code> if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
         * otherwise an {@code Optional} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
         *
         * @param str
         * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
         * @return
         * @see Strings#substring(String, char)
         */
        public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex));
        }

        /**
         * Returns an empty <code>Optional</code> if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0},
         * otherwise an {@code Optional} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
         *
         * @param str
         * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
         * @return
         * @see Strings#substring(String, String)
         */
        public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex));
        }

        /**
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substring(String, int, char)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1) if inclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substring(String, int, String)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param inclusiveBeginIndex
         * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substring(String, int, IntUnaryOperator)
         */
        public static Optional<String> substring(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, char, int)
         */
        public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, String, int)
         */
        public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, delimiterOfInclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substring(String, IntUnaryOperator, int)
         */
        public static Optional<String> substring(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         * Returns the substring after first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfter(String, String)
         */
        public static Optional<String> substringAfter(String str, String delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfter(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns the substring after last {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringAfterLast(String, String)
         */
        public static Optional<String> substringAfterLast(String str, String delimiterOfExclusiveBeginIndex) {
            return Optional.ofNullable(Strings.substringAfterLast(str, delimiterOfExclusiveBeginIndex));
        }

        /**
         * Returns the substring before first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBefore(String, String)
         */
        public static Optional<String> substringBefore(String str, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBefore(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns the substring last first {@code delimiterOfExclusiveBeginIndex} if it exists, otherwise return {@code null} String.
         *
         * @param str
         * @param delimiterOfExclusiveEndIndex
         * @return
         * @see Strings#substringBeforeLast(String, String)
         */
        public static Optional<String> substringBeforeLast(String str, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBeforeLast(str, delimiterOfExclusiveEndIndex));
        }

        /**
         * Returns an empty <code>Optional</code> if {@code exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex},
         * otherwise an {@code Optional} with String value: {@code str.substring(exclusiveBeginIndex + 1, exclusiveEndIndex)} is returned.
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
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, beginIndex + 1) if exclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substringBetween(String, int, char)
         */
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, beginIndex + 1) if exclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substringBetween(String, int, String)
         */
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param exclusiveBeginIndex
         * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
         * @return
         * @see Strings#substringBetween(String, int, IntUnaryOperator)
         */
        public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, exclusiveBeginIndex, funcOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex {@code exclusiveBeginIndex <- str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1) if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, char, int)
         */
        public static Optional<String> substringBetween(String str, char delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex {@code exclusiveBeginIndex <- str.lastIndexOf(delimiterOfExclusiveBeginIndex, exclusiveEndIndex - 1) + delimiterOfExclusiveBeginIndex.length() - 1 if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, String, int)
         */
        public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param funcOfExclusiveBeginIndex {@code exclusiveBeginIndex <- funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
         * @param exclusiveEndIndex
         * @return
         * @see Strings#substringBetween(String, IntUnaryOperator, int)
         */
        public static Optional<String> substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, funcOfExclusiveBeginIndex, exclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringBetween(String, char, char)
         */
        public static Optional<String> substringBetween(String str, char delimiterOfExclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
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
         *
         * @param str
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see Strings#substringBetween(String, String, String)
         */
        public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param str
         * @param fromIndex start index for {@code delimiterOfExclusive}. {@code str.indexOf(delimiterOfExclusiveBeginIndex, fromIndex)}
         * @param delimiterOfExclusiveBeginIndex
         * @param delimiterOfExclusiveBeginIndex
         * @return
         * @see #substringBetween(String, int, int)
         */
        public static Optional<String> substringBetween(String str, int fromIndex, String delimiterOfExclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
            return Optional.ofNullable(Strings.substringBetween(str, fromIndex, delimiterOfExclusiveBeginIndex, delimiterOfExclusiveEndIndex));
        }

        /**
         *
         * @param strs
         * @return
         * @see N#copyThenApply(Object[], com.landawn.abacus.util.Throwables.Function)
         * @see Fn#trim()
         * @see Fn#trimToEmpty()
         * @see Fn#trimToNull()
         */
        @Beta
        public static String[] copyThenTrim(final String[] strs) {
            return N.copyThenApply(strs, Fn.trim());
        }

        /**
         *
         * @param strs
         * @return
         * @see N#copyThenApply(Object[], com.landawn.abacus.util.Throwables.Function)
         * @see Fn#strip()
         * @see Fn#stripToEmpty()
         * @see Fn#stripToNull()
         */
        @Beta
        public static String[] copyThenStrip(final String[] strs) {
            return N.copyThenApply(strs, Fn.strip());
        }
    }
}
