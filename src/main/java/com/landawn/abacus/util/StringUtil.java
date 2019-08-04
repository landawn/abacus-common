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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.RandomAccess;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.IntUnaryOperator;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * 
 * @author haiyangl
 *
 */
public abstract class StringUtil {
    // public static final String EMPTY_STRING = N.EMPTY_STRING;

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
        final List<String> delimiters = N.asList(" ", "  ", "   ", "\t", "\n", "\r", ",", ", ", ";", "; ", ":", ": ", " : ", "-", " - ", "_", " _ ", "#", "##",
                " # ", "=", "==", " = ", "|", " | ", "||", " || ", "&", "&&", "@", "@@", "$", "$$", "*", "**", "+", "++");

        for (String delimiter : delimiters) {
            splitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings(true));
            trimSplitterPool.put(delimiter, Splitter.with(delimiter).omitEmptyStrings(true).trim(true));
            preserveSplitterPool.put(delimiter, Splitter.with(delimiter));
            trimPreserveSplitterPool.put(delimiter, Splitter.with(delimiter).trim(true));

            if (delimiter.length() == 1) {
                char delimiterChar = delimiter.charAt(0);

                splitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings(true));
                trimSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).omitEmptyStrings(true).trim(true));
                preserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar));
                trimPreserveSplitterPool.put(delimiterChar, Splitter.with(delimiterChar).trim(true));
            }
        }
    }

    static final Field strValueField;
    static volatile boolean isStringCharsGettable = true;
    static final Constructor<String> sharedStringConstructor;

    static {
        Field tmp = null;

        strValueField = ((tmp != null) && tmp.getName().equals("value") && tmp.getType().equals(char[].class)) ? tmp : null;

        if (strValueField != null) {
            strValueField.setAccessible(true);
        }

        Constructor<String> tmpConstructor = null;

        try {
            tmpConstructor = String.class.getDeclaredConstructor(char[].class, boolean.class);
            tmpConstructor.setAccessible(true);
        } catch (Exception e) {
            // ignore.
        }

        sharedStringConstructor = tmpConstructor;
    }

    private StringUtil() {
        // Singleton. Utility class.
    }

    // Abbreviating
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Abbreviates a String using ellipses. This will turn
     * "Now is the time for all good men" into "Now is the time for..."
     * </p>
     *
     * <p>
     * Specifically:
     * </p>
     * <ul>
     * <li>If {@code str} is less than or equals to {@code maxWidth} characters
     * long, return it.</li>
     * <li>Else abbreviate it to {@code (substring(str, 0, max-3) + "...")}.</li>
     * <li>If {@code maxWidth} is less than {@code 4}, throw an
     * {@code IllegalArgumentException}.</li>
     * <li>In no case will it return a String of length greater than
     * {@code maxWidth}.</li>
     * </ul>
     *
     * <pre>
     * N.abbreviate(null, *)      = null
     * N.abbreviate("", 4)        = ""
     * N.abbreviate("abcdefg", 6) = "abc..."
     * N.abbreviate("abcdefg", 7) = "abcdefg"
     * N.abbreviate("abcdefg", 8) = "abcdefg"
     * N.abbreviate("abcdefg", 4) = "a..."
     * N.abbreviate("abcdefg", 3) = IllegalArgumentException
     * </pre>
     *
     * @param str
     *            the String to check, may be null
     * @param maxWidth
     *            maximum length of result String, must be at least 4
     * @return abbreviated String, {@code ""} if null or "" String input
     * @throws IllegalArgumentException
     *             if the width is too small
     * @since 2.0
     */
    public static String abbreviate(final String str, final int maxWidth) {
        if (maxWidth < 4) {
            throw new IllegalArgumentException("Minimum abbreviation width is 4");
        }

        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return str.length() <= maxWidth ? str : str.substring(0, maxWidth - 3) + "...";
    }

    public static String reverse(final String str) {
        if (N.isNullOrEmpty(str)) {
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
     * @return the reversed String, {@code null} if null String input
     * @since 2.0
     */
    public static String reverseDelimited(final String str, final char delimiter) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        final String[] strs = split(str, delimiter);

        N.reverse(strs);

        return join(strs, delimiter);
    }

    public static String reverseDelimited(final String str, final String delimiter) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        // could implement manually, but simple way is to reuse other,
        // probably slower, methods.
        final String[] strs = split(str, delimiter);

        N.reverse(strs);

        return Joiner.with(delimiter).reuseCachedBuffer(true).appendAll(strs).toString();
    }

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
    public static String padStart(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = N.EMPTY_STRING;
        } else if (str.length() >= minLength) {
            return str;
        }

        int delta = minLength - str.length();
        final char[] chars = new char[minLength];

        N.fill(chars, 0, delta, padChar);

        str.getChars(0, str.length(), chars, delta);

        return newString(chars, true);
    }

    public static String padStart(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = N.EMPTY_STRING;
        } else if (str.length() >= minLength) {
            return str;
        }

        int delta = ((minLength - str.length()) % padStr.length() == 0) ? ((minLength - str.length()) / padStr.length())
                : ((minLength - str.length()) % padStr.length() + 1);
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
    public static String padEnd(String str, final int minLength, final char padChar) {
        if (str == null) {
            str = N.EMPTY_STRING;
        } else if (str.length() >= minLength) {
            return str;
        }

        final char[] chars = new char[minLength];
        str.getChars(0, str.length(), chars, 0);

        N.fill(chars, str.length(), minLength, padChar);

        return newString(chars, true);
    }

    public static String padEnd(String str, final int minLength, final String padStr) {
        if (str == null) {
            str = N.EMPTY_STRING;
        } else if (str.length() >= minLength) {
            return str;
        }

        int delta = ((minLength - str.length()) % padStr.length() == 0) ? ((minLength - str.length()) / padStr.length())
                : ((minLength - str.length()) % padStr.length() + 1);

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

            return newString(array, true);
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

            return newString(array, true);
        }
    }

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

    public static String repeat(String str, final int n, String delimiter) {
        N.checkArgNotNegative(n, "n");

        str = str == null ? N.EMPTY_STRING : str;
        delimiter = delimiter == null ? N.EMPTY_STRING : delimiter;

        if (n == 0 || (N.isNullOrEmpty(str) && N.isNullOrEmpty(delimiter))) {
            return N.EMPTY_STRING;
        } else if (n == 1) {
            return str;
        }

        final int strLen = str.length();
        final int delimiterLen = delimiter.length();
        final int len = strLen + delimiterLen;
        if (Integer.MAX_VALUE / len < n) {
            throw new ArrayIndexOutOfBoundsException("Required array size too large: " + 1L * len * n);
        }

        final int size = len * n - delimiterLen;
        final char[] cbuf = new char[size];

        str.getChars(0, strLen, cbuf, 0);
        delimiter.getChars(0, delimiterLen, cbuf, strLen);

        int cnt = 0;

        for (cnt = len; cnt < size - cnt; cnt <<= 1) {
            N.copy(cbuf, 0, cbuf, cnt, cnt);
        }

        if (cnt < size) {
            N.copy(cbuf, 0, cbuf, cnt, size - cnt);
        }

        return newString(cbuf, true);
    }

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
     * StrUtil.toLowerCase(null)  = null
     * StrUtil.toLowerCase("")    = ""
     * StrUtil.toLowerCase("aBc") = "abc"
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
     * @return the lower cased String, {@code null} if null String input
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
     * StrUtil.toLowerCase(null, Locale.ENGLISH)  = null
     * StrUtil.toLowerCase("", Locale.ENGLISH)    = ""
     * StrUtil.toLowerCase("aBc", Locale.ENGLISH) = "abc"
     * </pre>
     *
     * @param str
     *            the String to lower case, may be null
     * @param locale
     *            the locale that defines the case transformation rules, must
     *            not be null
     * @return the lower cased String, {@code null} if null String input
     * @since 2.5
     */
    public static String toLowerCase(final String str, final Locale locale) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return str.toLowerCase(locale);
    }

    public static String toLowerCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                char ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (!Character.isUpperCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                            sb.append(WD._UNDERSCORE);
                        }
                    }

                    sb.append(Character.toLowerCase(ch));
                } else {
                    if (i > 0 && ((isAsciiNumeric(ch) && !isAsciiNumeric(str.charAt(i - 1))) || (isAsciiNumeric(str.charAt(i - 1)) && !isAsciiNumeric(ch)))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                            sb.append(WD._UNDERSCORE);
                        }
                    }

                    sb.append(ch);
                }
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

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
     * @return the upper cased String, {@code null} if null String input
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
     * @return the upper cased String, {@code null} if null String input
     * @since 2.5
     */
    public static String toUpperCase(final String str, final Locale locale) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return str.toUpperCase(locale);
    }

    public static String toUpperCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0, len = str.length(); i < len; i++) {
                char ch = str.charAt(i);

                if (Character.isUpperCase(ch)) {
                    if (i > 0 && (!Character.isUpperCase(str.charAt(i - 1)) || (i < len - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                            sb.append(WD._UNDERSCORE);
                        }
                    }

                    sb.append(ch);
                } else {
                    if (i > 0 && ((isAsciiNumeric(ch) && !isAsciiNumeric(str.charAt(i - 1))) || (isAsciiNumeric(str.charAt(i - 1)) && !isAsciiNumeric(ch)))) {
                        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != WD._UNDERSCORE) {
                            sb.append(WD._UNDERSCORE);
                        }
                    }

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
     * @param str
     * @return
     */
    public static String toCamelCase(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        if (str.indexOf(WD._UNDERSCORE) >= 0) {
            String[] substrs = StringUtil.split(str, WD._UNDERSCORE);
            final StringBuilder sb = Objectory.createStringBuilder();

            try {

                for (String substr : substrs) {
                    if (N.notNullOrEmpty(substr)) {
                        sb.append(StringUtil.toLowerCase(substr));
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
                    return StringUtil.uncapitalize(str);
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
     * @return the changed String, {@code null} if null String input
     */
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

        return newString(cbuf, true);
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
        final char[] chars = getCharsForReadOnly(str);

        try {
            char ch = 0;
            for (int i = 0, len = str.length(); i < len; i++) {
                ch = chars[i];

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
     *   CharUtils.unicodeEscaped(' ') = "\u0020"
     *   CharUtils.unicodeEscaped('A') = "\u0041"
     * </pre>
     *
     * @param ch
     *            the character to convert
     * @return the escaped Unicode string
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
     * @see Pattern
     * @see #trim(String)
     * @see <a
     *      href="http://www.w3.org/TR/xpath/#function-normalize-space">http://www.w3.org/TR/xpath/#function-normalize-space</a>
     * @param str
     *            the source String to normalize whitespaces from, may be null
     * @return the modified string with whitespace normalized, {@code null} if
     *         null String input
     *
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
     * N.replaceAll("aba", "a", null)  = "aba"
     * N.replaceAll("aba", "a", "")    = "b"
     * N.replaceAll("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @see #replaceAll(String text, String searchString, String replacement,
     *      int max)
     * @param str
     *            text to search and replace in, may be null
     * @param target
     *            the String to search for, may be null
     * @param replacement
     *            the String to replace it with, may be null
     * @return the text with any replacements processed, {@code null} if null
     *         String input
     */
    public static String replaceAll(final String str, final String target, final String replacement) {
        return replaceAll(str, 0, target, replacement);
    }

    public static String replaceAll(final String str, final int fromIndex, final String target, final String replacement) {
        return replace(str, fromIndex, target, replacement, -1);
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
     * replace("abaa", 0, "a", null, -1) = "abaa"
     * replace("abaa", 0, "a", "", -1)   = "b"
     * replace("abaa", 0, "a", "z", 0)   = "abaa"
     * replace("abaa", 0, "a", "z", 1)   = "zbaa"
     * replace("abaa", 0, "a", "z", 2)   = "zbza"
     * replace("abaa", 0, "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param str
     *            text to search and replace in, may be null
     * @param fromIndex
     * @param target
     *            the String to search for, may be null
     * @param replacement
     *            the String to replace it with, can't be null
     * @param max
     *            maximum number of values to replace, or {@code -1} if no
     *            maximum
     * @return the text with any replacements processed, {@code null} if null
     *         String input
     */
    public static String replace(final String str, final int fromIndex, final String target, final String replacement, int max) {
        return replace(str, fromIndex, target, replacement, max, false);
    }

    public static String replaceAllIgnoreCase(final String str, final String target, final String replacement) {
        return replaceAllIgnoreCase(str, 0, target, replacement);
    }

    public static String replaceAllIgnoreCase(final String str, final int fromIndex, final String target, final String replacement) {
        return replaceIgnoreCase(str, fromIndex, target, replacement, -1);
    }

    public static String replaceIgnoreCase(final String str, final int fromIndex, final String target, final String replacement, int max) {
        return replace(str, fromIndex, target, replacement, max, true);
    }

    private static String replace(final String str, final int fromIndex, final String target, final String replacement, int max, boolean ignoreCase) {
        if (replacement == null) {
            throw new IllegalArgumentException("Replacement can't be null");
        }

        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(target) || max == 0) {
            return str;
        }

        final String searchText = ignoreCase ? str.toLowerCase() : str;
        final String searchTarget = ignoreCase ? target.toLowerCase() : target;

        int start = fromIndex;
        int end = searchText.indexOf(searchTarget, start);

        if (end == N.INDEX_NOT_FOUND) {
            return str;
        }

        final StringBuilder sb = Objectory.createStringBuilder();
        final int substrLength = target.length();

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
     * @return the substring with the string removed if found, {@code null} if
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
     * @return the substring with the string removed if found, {@code null} if
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
     * @return the substring with the string removed if found, {@code null} if
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
     * @return the substring with the string removed if found, {@code null} if
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
     * @return the substring with the char removed if found, {@code null} if
     *         null String input
     * @since 2.1
     */
    public static String removeAll(final String str, final char removeChar) {
        return removeAll(str, 0, removeChar);
    }

    public static String removeAll(final String str, final int fromIndex, final char removeChar) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        int index = str.indexOf(removeChar, fromIndex);
        if (index == N.INDEX_NOT_FOUND) {
            return str;
        } else {
            final char[] chars = getCharsForReadOnly(str);
            final char[] cbuf = new char[str.length()];

            if (index > 0) {
                str.getChars(0, index, cbuf, 0);
            }

            int count = index;
            for (int i = index + 1, len = chars.length; i < len; i++) {
                if (chars[i] != removeChar) {
                    cbuf[count++] = chars[i];
                }
            }

            return count == chars.length ? str : new String(cbuf, 0, count);
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
     * @return the substring with the string removed if found, {@code null} if
     *         null String input
     * @since 2.1
     */
    public static String removeAll(final String str, final String removeStr) {
        return removeAll(str, 0, removeStr);
    }

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

        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings(true) : splitter).splitToArray(str);
    }

    /**
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static String[] split(final String str, final char delimiter, final boolean trim) {
        if (trim) {
            final Splitter splitter = trimSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings(true).trim(trim) : splitter).splitToArray(str);
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

        return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings(true) : splitter).splitToArray(str);
    }

    /**
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static String[] split(final String str, final String delimiter, final boolean trim) {
        if (trim) {
            final Splitter splitter = trimSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).omitEmptyStrings(true).trim(trim) : splitter).splitToArray(str);
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
        return Splitter.with(delimiter).omitEmptyStrings(true).limit(max).splitToArray(str);
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
        return Splitter.with(delimiter).omitEmptyStrings(true).trim(trim).limit(max).splitToArray(str);
    }

    /**
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
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static String[] splitPreserveAllTokens(final String str, final char delimiter, boolean trim) {
        if (trim) {
            final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        } else {
            return splitPreserveAllTokens(str, delimiter);
        }
    }

    /**
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
     * 
     * @param str
     * @param delimiter
     * @param trim
     * @return
     */
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, boolean trim) {
        if (trim) {
            final Splitter splitter = trimPreserveSplitterPool.get(delimiter);
            return (splitter == null ? Splitter.with(delimiter).trim(trim) : splitter).splitToArray(str);
        } else {
            return splitPreserveAllTokens(str, delimiter);
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
    public static String[] splitPreserveAllTokens(final String str, final String delimiter, final int max) {
        return Splitter.with(delimiter).limit(max).splitToArray(str);
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
     * StrUtil.trim(null)          = null
     * StrUtil.trim("")            = ""
     * StrUtil.trim("     ")       = ""
     * StrUtil.trim("abc")         = "abc"
     * StrUtil.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str
     *            the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        return N.isNullOrEmpty(str) || (str.charAt(0) != ' ' && str.charAt(str.length() - 1) != ' ') ? str : str.trim();
    }

    public static String[] trim(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = trim(strs[i]);
        }

        return res;
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
     * @return the trimmed String, {@code null} if only chars &lt;= 32, empty or
     *         null String input
     * @since 2.0
     */
    public static String trimToNull(String str) {
        str = trim(str);

        return N.isNullOrEmpty(str) ? null : str;
    }

    public static String[] trimToNull(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = trimToNull(strs[i]);
        }

        return res;
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
     * @return the trimmed String, or an empty String if {@code null} input
     * @since 2.0
     */
    public static String trimToEmpty(final String str) {
        return N.isNullOrEmpty(str) ? N.EMPTY_STRING : str.trim();
    }

    public static String[] trimToEmpty(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = trimToEmpty(strs[i]);
        }

        return res;
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
     * @return the stripped String, {@code null} if null String input
     */
    public static String strip(final String str) {
        return strip(str, null);
    }

    public static String[] strip(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = strip(strs[i]);
        }

        return res;
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
     * @return the stripped String, {@code null} if whitespace, empty or null
     *         String input
     * @since 2.0
     */
    public static String stripToNull(String str) {
        str = strip(str, null);

        return N.isNullOrEmpty(str) ? null : str;
    }

    public static String[] stripToNull(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = stripToNull(strs[i]);
        }

        return res;
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
     * @return the trimmed String, or an empty String if {@code null} input
     * @since 2.0
     */
    public static String stripToEmpty(final String str) {
        return N.isNullOrEmpty(str) ? N.EMPTY_STRING : strip(str, null);
    }

    public static String[] stripToEmpty(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = trimToEmpty(strs[i]);
        }

        return res;
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
     * @return the stripped String, {@code null} if null String input
     */
    public static String strip(final String str, final String stripChars) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        return stripEnd(stripStart(str, stripChars), stripChars);
    }

    public static String[] strip(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = strip(strs[i], stripChars);
        }

        return res;
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
     * @return the stripped String, {@code null} if null String input
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

    public static String[] stripStart(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = stripStart(strs[i], stripChars);
        }

        return res;
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
     * @return the stripped String, {@code null} if null String input
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

    public static String[] stripEnd(final String[] strs, final String stripChars) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = stripEnd(strs[i], stripChars);
        }

        return res;
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
     * @param strs
     *            String to be stripped
     * @return input text with diacritics removed
     *
     * @since 3.0
     */
    // See also Lucene's ASCIIFoldingFilter (Lucene 2.9) that replaces accented
    // characters by their unaccented equivalent (and uncommitted bug fix:
    // https://issues.apache.org/jira/browse/LUCENE-1343?focusedCommentId=12858907&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_12858907).
    public static String stripAccents(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final String decomposed = Normalizer.normalize(str, Normalizer.Form.NFD);
        // Note that this doesn't correctly remove ligatures...
        return pattern_accent.matcher(decomposed).replaceAll("");//$NON-NLS-1$
    }

    private static final Pattern pattern_accent = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$

    public static String[] stripAccents(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = stripAccents(strs[i]);
        }

        return res;
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

    public static String[] chomp(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = chomp(strs[i]);
        }

        return res;
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
     * StrUtil.chop(null)          = null
     * StrUtil.chop("")            = ""
     * StrUtil.chop("abc \r")      = "abc "
     * StrUtil.chop("abc\n")       = "abc"
     * StrUtil.chop("abc\r\n")     = "abc"
     * StrUtil.chop("abc")         = "ab"
     * StrUtil.chop("abc\nabc")    = "abc\nab"
     * StrUtil.chop("a")           = ""
     * StrUtil.chop("\r")          = ""
     * StrUtil.chop("\n")          = ""
     * StrUtil.chop("\r\n")        = ""
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

    public static String[] chop(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = chop(strs[i]);
        }

        return res;
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
     * @return the String without whitespaces, {@code null} if null String input
     */
    public static String deleteWhitespace(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final char[] chars = getCharsForReadOnly(str);
        final char[] cbuf = new char[chars.length];
        int count = 0;
        for (int i = 0, len = chars.length; i < len; i++) {
            if (!Character.isWhitespace(chars[i])) {
                cbuf[count++] = chars[i];
            }
        }

        return count == chars.length ? str : new String(cbuf, 0, count);
    }

    public static String[] deleteWhitespace(final String[] strs) {
        if (N.isNullOrEmpty(strs)) {
            return strs;
        }

        final String[] res = new String[strs.length];

        for (int i = 0, len = strs.length; i < len; i++) {
            res[i] = deleteWhitespace(strs[i]);
        }

        return res;
    }

    /**
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

    public static String wrapIfMissing(final String str, final String prefixSuffix) {
        N.checkArgNotNull(prefixSuffix);

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

    public static String wrap(final String str, final String prefixSuffix) {
        N.checkArgNotNull(prefixSuffix);

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

    public static String unwrap(final String str, final String prefixSuffix) {
        N.checkArgNotNull(prefixSuffix);

        return unwrap(str, prefixSuffix, prefixSuffix);
    }

    /** 
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

    public static boolean isLowerCase(final char ch) {
        return Character.isLowerCase(ch);
    }

    public static boolean isAsciiLowerCase(final char ch) {
        return (ch >= 'a') && (ch <= 'z');
    }

    public static boolean isUpperCase(final char ch) {
        return Character.isUpperCase(ch);
    }

    public static boolean isAsciiUpperCase(final char ch) {
        return (ch >= 'A') && (ch <= 'Z');
    }

    public static boolean isAllLowerCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isUpperCase(chars[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isUpperCase(cs.charAt(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAllUpperCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isLowerCase(chars[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isLowerCase(cs.charAt(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Copied from Apache Commons Lang: StringUtils#isMixedCase.
     * 
     * @param cs
     * @return
     */
    public static boolean isMixedCase(final CharSequence cs) {
        if (N.isNullOrEmpty(cs) || cs.length() == 1) {
            return false;
        }

        boolean containsUppercase = false;
        boolean containsLowercase = false;
        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (containsUppercase && containsLowercase) {
                    return true;
                } else if (Character.isUpperCase(chars[i])) {
                    containsUppercase = true;
                } else if (Character.isLowerCase(chars[i])) {
                    containsLowercase = true;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (containsUppercase && containsLowercase) {
                    return true;
                } else if (Character.isUpperCase(cs.charAt(i))) {
                    containsUppercase = true;
                } else if (Character.isLowerCase(cs.charAt(i))) {
                    containsLowercase = true;
                }
            }
        }

        return containsUppercase && containsLowercase;
    }

    /**
     *
     * @param ch
     * @return
     * @see Character#isDigit(char)
     */
    public static boolean isDigit(final char ch) {
        return Character.isDigit(ch);
    }

    /**
     *
     * @param ch
     * @return
     * @see Character#isLetter(char)
     */
    public static boolean isLetter(final char ch) {
        return Character.isLetter(ch);
    }

    /**
     *
     * @param ch
     * @return
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
     *   CharUtils.isAscii('a')  = true
     *   CharUtils.isAscii('A')  = true
     *   CharUtils.isAscii('3')  = true
     *   CharUtils.isAscii('-')  = true
     *   CharUtils.isAscii('\n') = true
     *   CharUtils.isAscii('&copy;') = false
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
     *   CharUtils.isAsciiPrintable('a')  = true
     *   CharUtils.isAsciiPrintable('A')  = true
     *   CharUtils.isAsciiPrintable('3')  = true
     *   CharUtils.isAsciiPrintable('-')  = true
     *   CharUtils.isAsciiPrintable('\n') = false
     *   CharUtils.isAsciiPrintable('&copy;') = false
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
     *   CharUtils.isAsciiControl('a')  = false
     *   CharUtils.isAsciiControl('A')  = false
     *   CharUtils.isAsciiControl('3')  = false
     *   CharUtils.isAsciiControl('-')  = false
     *   CharUtils.isAsciiControl('\n') = true
     *   CharUtils.isAsciiControl('&copy;') = false
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
     *   CharUtils.isAsciiAlpha('a')  = true
     *   CharUtils.isAsciiAlpha('A')  = true
     *   CharUtils.isAsciiAlpha('3')  = false
     *   CharUtils.isAsciiAlpha('-')  = false
     *   CharUtils.isAsciiAlpha('\n') = false
     *   CharUtils.isAsciiAlpha('&copy;') = false
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
     *   CharUtils.isAsciiAlphaUpper('a')  = false
     *   CharUtils.isAsciiAlphaUpper('A')  = true
     *   CharUtils.isAsciiAlphaUpper('3')  = false
     *   CharUtils.isAsciiAlphaUpper('-')  = false
     *   CharUtils.isAsciiAlphaUpper('\n') = false
     *   CharUtils.isAsciiAlphaUpper('&copy;') = false
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
     *   CharUtils.isAsciiAlphaLower('a')  = true
     *   CharUtils.isAsciiAlphaLower('A')  = false
     *   CharUtils.isAsciiAlphaLower('3')  = false
     *   CharUtils.isAsciiAlphaLower('-')  = false
     *   CharUtils.isAsciiAlphaLower('\n') = false
     *   CharUtils.isAsciiAlphaLower('&copy;') = false
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
     *   CharUtils.isAsciiNumeric('a')  = false
     *   CharUtils.isAsciiNumeric('A')  = false
     *   CharUtils.isAsciiNumeric('3')  = true
     *   CharUtils.isAsciiNumeric('-')  = false
     *   CharUtils.isAsciiNumeric('\n') = false
     *   CharUtils.isAsciiNumeric('&copy;') = false
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
     *   CharUtils.isAsciiAlphanumeric('a')  = true
     *   CharUtils.isAsciiAlphanumeric('A')  = true
     *   CharUtils.isAsciiAlphanumeric('3')  = true
     *   CharUtils.isAsciiAlphanumeric('-')  = false
     *   CharUtils.isAsciiAlphanumeric('\n') = false
     *   CharUtils.isAsciiAlphanumeric('&copy;') = false
     * </pre>
     *
     * @param ch
     *            the character to check
     * @return true if between 48 and 57 or 65 and 90 or 97 and 122 inclusive
     */
    public static boolean isAsciiAlphanumeric(final char ch) {
        return isAsciiAlpha(ch) || isAsciiNumeric(ch);
    }

    public static boolean isAsciiPrintable(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiPrintable(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiPrintable(cs.charAt(i)) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAsciiAlpha(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiAlpha(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiAlpha(cs.charAt(i)) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAsciiAlphaSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiAlpha(chars[i]) == false && chars[i] != ' ') {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiAlpha(cs.charAt(i)) == false && cs.charAt(i) != ' ') {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAsciiAlphanumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiAlphanumeric(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiAlphanumeric(cs.charAt(i)) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAsciiAlphanumericSpace(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiAlphanumeric(chars[i]) == false && chars[i] != ' ') {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiAlphanumeric(cs.charAt(i)) == false && cs.charAt(i) != ' ') {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isAsciiNumeric(final CharSequence cs) {
        if (N.isNullOrEmpty(cs)) {
            return false;
        }

        final int len = cs.length();

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (isAsciiNumeric(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (isAsciiNumeric(cs.charAt(i)) == false) {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isLetter(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isLetter(cs.charAt(i)) == false) {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isLetter(chars[i]) == false && chars[i] != ' ') {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isLetter(cs.charAt(i)) == false && cs.charAt(i) != ' ') {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isLetterOrDigit(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isLetterOrDigit(cs.charAt(i)) == false) {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isLetterOrDigit(chars[i]) == false && chars[i] != ' ') {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isLetterOrDigit(cs.charAt(i)) == false && cs.charAt(i) != ' ') {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isDigit(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isDigit(cs.charAt(i)) == false) {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isDigit(chars[i]) == false && chars[i] != ' ') {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isDigit(cs.charAt(i)) == false && cs.charAt(i) != ' ') {
                    return false;
                }
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

        if (cs.getClass().equals(String.class)) {
            final char[] chars = getCharsForReadOnly((String) cs);

            for (int i = 0; i < len; i++) {
                if (Character.isWhitespace(chars[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (Character.isWhitespace(cs.charAt(i)) == false) {
                    return false;
                }
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
        return createNumber(str).isPresent();
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
     * @param val
     * @return
     */
    public static boolean isAsciiDigtalNumber(final String str) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        final char[] chs = getCharsForReadOnly(str);

        int i = 0, num = 0;
        if (chs[i] == '+' || chs[i] == '-') {
            i++;
        }

        for (; i < chs.length && (chs[i] >= '0' && chs[i] <= '9'); i++) {
            num++;
        }

        if (i < chs.length && chs[i] == '.') {
            if (num == 0) {
                return false;
            } else {
                num = 0;
            }

            i++;
        }

        for (; i < chs.length && (chs[i] >= '0' && chs[i] <= '9'); i++) {
            num++;
        }

        if (num == 0) {
            return false;
        }

        if (i == chs.length) {
            return true;
        } else if (chs[i] != 'e' && chs[i] != 'E') {
            return false;
        } else {
            i++;
        }

        num = 0;
        if (i < chs.length && (chs[i] == '+' || chs[i] == '-')) {
            i++;
        }

        for (; i < chs.length && (chs[i] >= '0' && chs[i] <= '9'); i++) {
            num++;
        }

        if (num == 0) {
            return false;
        } else if (i == chs.length) {
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
     * @param val
     * @return
     */
    public static boolean isAsciiDigtalInteger(final String str) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        final char[] chs = getCharsForReadOnly(str);

        int i = 0, num = 0;
        if (chs[i] == '+' || chs[i] == '-') {
            i++;
        }

        for (; i < chs.length && (chs[i] >= '0' && chs[i] <= '9'); i++) {
            num++;
        }

        if (num == 0) {
            return false;
        }

        return i == chs.length;
    }

    public static int indexOf(final String str, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(targetChar);
    }

    public static int indexOf(final String str, final int fromIndex, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(targetChar, fromIndex);
    }

    public static int indexOf(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(substr);
    }

    public static int indexOf(final String str, final int fromIndex, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length() - fromIndex) {
            return N.INDEX_NOT_FOUND;
        }

        return str.indexOf(substr, fromIndex);
    }

    @SafeVarargs
    public static int indexOfAny(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return N.INDEX_NOT_FOUND;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = chs.length;
        final int chsLast = chsLen - 1;
        for (int i = 0; i < strLen; i++) {
            final char ch = str.charAt(i);
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
        outer: for (int i = 0; i < strLen; i++) {
            final char ch = str.charAt(i);
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

    public static int indexOf(final String str, final String substr, final String delimiter) {
        return indexOf(str, 0, substr, delimiter);
    }

    /**
     *
     * @param str
     * @param fromIndex
     *            the index from which to start the search.
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

    public static int indexOfIgnoreCase(final String str, final String substr) {
        return indexOfIgnoreCase(str, 0, substr);
    }

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
     * @param ordinal
     *            the n-th {@code searchStr} to find
     * @return the n-th index of the search String, {@code -1} (
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int ordinalIndexOf(final String str, final String substr, final int ordinal) {
        return ordinalIndexOf(str, substr, ordinal, false);
    }

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
     * @param fromIndex
     *            the index to start the search from. There is no restriction on
     *            the value of <code>fromIndex</code>. If it is greater than or
     *            equal to the length of this string, it has the same effect as
     *            if it were equal to one less than the length of this string:
     *            this entire string may be searched. If it is negative, it has
     *            the same effect as if it were -1: -1 is returned.
     * @param targetChar
     *            a character (Unicode code point).
     * @return the index of the last occurrence of the character in the
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

    public static int lastIndexOf(final String str, final String substr, final String delimiter) {
        return lastIndexOf(str, str.length(), substr, delimiter);
    }

    /**
     *
     * @param str
     * @param fromIndex
     *            the start index to traverse backwards from
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

    public static int lastIndexOfIgnoreCase(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr) || substr.length() > str.length()) {
            return N.INDEX_NOT_FOUND;
        }

        return lastIndexOfIgnoreCase(str, str.length(), substr);
    }

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
     * @param ordinal
     *            the n-th last {@code searchStr} to find
     * @return the n-th last index of the search CharSequence, {@code -1} (
     *         {@code N.INDEX_NOT_FOUND}) if no match or {@code null} or empty
     *         string input
     */
    public static int lastOrdinalIndexOf(final String str, final String substr, final int ordinal) {
        return ordinalIndexOf(str, substr, ordinal, true);
    }

    // Shared code between ordinalIndexOf(String,String,int) and
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

    public static boolean contains(final String str, final int targetChar) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        return indexOf(str, targetChar) != N.INDEX_NOT_FOUND;
    }

    public static boolean contains(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOf(str, substr) != N.INDEX_NOT_FOUND;
    }

    @SafeVarargs
    public static boolean containsAny(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return false;
        }

        return indexOfAny(str, chs) != N.INDEX_NOT_FOUND;
    }

    @SafeVarargs
    public static boolean containsOnly(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return false;
        }

        return indexOfAnyBut(str, chs) == N.INDEX_NOT_FOUND;
    }

    @SafeVarargs
    public static boolean containsNone(final String str, final char... chs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(chs)) {
            return true;
        }

        final int strLen = str.length();
        final int strLast = strLen - 1;
        final int chsLen = chs.length;
        final int chsLast = chsLen - 1;
        for (int i = 0; i < strLen; i++) {
            final char ch = str.charAt(i);
            for (int j = 0; j < chsLen; j++) {
                if (chs[j] == ch) {
                    if (Character.isHighSurrogate(ch)) {
                        if (j == chsLast) {
                            // missing low surrogate, fine, like
                            // String.indexOf(String)
                            return false;
                        }
                        if (i < strLast && chs[j + 1] == str.charAt(i + 1)) {
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

    public static boolean contains(final String str, final String substr, final String delimiter) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOf(str, substr, delimiter) != N.INDEX_NOT_FOUND;
    }

    public static boolean containsIgnoreCase(final String str, final String substr) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substr)) {
            return false;
        }

        return indexOfIgnoreCase(str, substr) != N.INDEX_NOT_FOUND;
    }

    // From org.springframework.util.StringUtils, under Apache License 2.0
    public static boolean containsWhitespace(final String str) {
        if (N.isNullOrEmpty(str)) {
            return false;
        }

        final char[] chars = getCharsForReadOnly(str);
        for (int i = 0, len = str.length(); i < len; i++) {
            if (Character.isWhitespace(chars[i])) {
                return true;
            }
        }

        return false;
    }

    public static boolean startsWith(final String str, final String prefix) {
        return startsWith(str, prefix, false);
    }

    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        return startsWith(str, prefix, true);
    }

    private static boolean startsWith(final String str, final String prefix, final boolean ignoreCase) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(prefix) || prefix.length() > str.length()) {
            return false;
        }

        return ignoreCase ? str.regionMatches(true, 0, prefix, 0, prefix.length()) : str.startsWith(prefix);
    }

    @SafeVarargs
    public static boolean startsWithAny(final String str, final String... substrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substrs)) {
            return false;
        }

        for (final String substr : substrs) {
            if (startsWith(str, substr)) {

                return true;
            }
        }

        return false;
    }

    public static boolean endsWith(final String str, final String suffix) {
        return endsWith(str, suffix, false);
    }

    public static boolean endsWithIgnoreCase(final String str, final String suffix) {
        return endsWith(str, suffix, true);
    }

    private static boolean endsWith(final String str, final String suffix, final boolean ignoreCase) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(suffix) || suffix.length() > str.length()) {
            return false;
        }

        final int strOffset = str.length() - suffix.length();

        return ignoreCase ? str.regionMatches(true, strOffset, suffix, 0, suffix.length()) : str.endsWith(suffix);
    }

    @SafeVarargs
    public static boolean endsWithAny(final String str, final String... substrs) {
        if (N.isNullOrEmpty(str) || N.isNullOrEmpty(substrs)) {
            return false;
        }

        for (final String searchString : substrs) {
            if (endsWith(str, searchString)) {
                return true;
            }
        }

        return false;
    }

    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
    }

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
     * @return the index where cs1 and cs2 begin to differ; -1 if they are equal
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
     * @return the index where the strings begin to differ; -1 if they are all
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
        for (int stringPos = 0; stringPos < shortestStrLen; stringPos++) {
            final char comparisonChar = strs[0].charAt(stringPos);
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
     */
    static boolean validSurrogatePairAt(final String str, final int index) {
        return index >= 0 && index <= (str.length() - 2) && Character.isHighSurrogate(str.charAt(index)) && Character.isLowSurrogate(str.charAt(index + 1));
    }

    public static int countMatches(final String str, final char ch) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        int count = 0;
        final char[] chs = getCharsForReadOnly(str);

        for (int i = 0, len = chs.length; i < len; i++) {
            if (chs[i] == ch) {
                count++;
            }
        }

        return count;
    }

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
     * Returns an empty <code>Optional</code> if {@code inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex}, 
     * otherwise an {@code Optional} with String value: {@code str.substring(exclusiveBeginIndex, exclusiveEndIndex)} is returned.
     * 
     * @param str
     * @param inclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    public static Optional<String> substring(String str, int inclusiveBeginIndex, int exclusiveEndIndex) {
        if (inclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || inclusiveBeginIndex > exclusiveEndIndex) {
            return Optional.<String> empty();
        }

        return Optional.of(str.substring(inclusiveBeginIndex, exclusiveEndIndex));
    }

    /**
     * Returns an empty <code>Optional</code> if {@code inclusiveBeginIndex < 0}, 
     * otherwise an {@code Optional} with String value: {@code str.substring(inclusiveBeginIndex)} is returned.
     * 
     * @param str
     * @param inclusiveBeginIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static Optional<String> substring(String str, int inclusiveBeginIndex) {
        if (inclusiveBeginIndex < 0) {
            return Optional.<String> empty();
        }

        return Optional.of(str.substring(inclusiveBeginIndex));
    }

    /**
     * Returns an empty <code>Optional</code> if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0}, 
     * otherwise an {@code Optional} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
     * 
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substring(String, int)
     */
    public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex) {
        if (N.isNullOrEmpty(str)) {
            return Optional.<String> empty();
        }

        return substring(str, str.indexOf(delimiterOfInclusiveBeginIndex));
    }

    /**
     * Returns an empty <code>Optional</code> if {@code N.isNullOrEmpty(str) || str.indexOf(delimiterOfInclusiveBeginIndex) < 0}, 
     * otherwise an {@code Optional} with String value: {@code str.substring(str.indexOf(delimiterOfInclusiveBeginIndex))} is returned.
     * 
     * @param str
     * @param delimiterOfInclusiveBeginIndex {@code inclusiveBeginIndex <- str.indexOf(delimiterOfInclusiveBeginIndex)}
     * @return
     * @see #substring(String, int)
     */
    public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex) {
        if (N.isNullOrEmpty(str)) {
            return Optional.<String> empty();
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
    public static Optional<String> substring(String str, int inclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (inclusiveBeginIndex < 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substring(String str, int inclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (inclusiveBeginIndex < 0) {
            return Optional.<String> empty();
        }

        return substring(str, inclusiveBeginIndex, str.indexOf(delimiterOfExclusiveEndIndex, inclusiveBeginIndex + 1));
    }

    /**
     * 
     * @param str
     * @param inclusiveBeginIndex
     * @param funcOfExclusiveEndIndex {@code exclusiveEndIndex <- funcOfExclusiveEndIndex.applyAsInt(inclusiveBeginIndex) if inclusiveBeginIndex >= 0}
     * @return
     * @see #substring(String, int, int)
     */
    public static Optional<String> substring(String str, int inclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (inclusiveBeginIndex < 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substring(String str, char delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substring(String str, String delimiterOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
        }

        return substring(str, str.lastIndexOf(delimiterOfInclusiveBeginIndex, exclusiveEndIndex - 1), exclusiveEndIndex);
    }

    /**
     * 
     * @param str
     * @param funcOfInclusiveBeginIndex {@code inclusiveBeginIndex <- funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex)) if exclusiveEndIndex > 0}
     * @param exclusiveEndIndex
     * @return
     * @see #substring(String, int, int)
     */
    public static Optional<String> substring(String str, IntUnaryOperator funcOfInclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
        }

        return substring(str, funcOfInclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    /**
     * Returns an empty <code>Optional</code> if {@code exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex}, 
     * otherwise an {@code Optional} with String value: {@code str.substring(exclusiveBeginIndex + 1, exclusiveEndIndex)} is returned.
     * 
     * @param str
     * @param exclusiveBeginIndex
     * @param exclusiveEndIndex
     * @return
     */
    public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveBeginIndex < 0 || exclusiveEndIndex < 0 || exclusiveBeginIndex >= exclusiveEndIndex) {
            return Optional.<String> empty();
        }

        return Optional.of(str.substring(exclusiveBeginIndex + 1, exclusiveEndIndex));
    }

    /**
     * 
     * @param str
     * @param exclusiveBeginIndex
     * @param delimiterOfExclusiveEndIndex {@code exclusiveEndIndex <- str.indexOf(delimiterOfExclusiveEndIndex, beginIndex + 1) if exclusiveBeginIndex >= 0}
     * @return
     * @see #substringBetween(String, int, int)
     */
    public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, char delimiterOfExclusiveEndIndex) {
        if (exclusiveBeginIndex < 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, String delimiterOfExclusiveEndIndex) {
        if (exclusiveBeginIndex < 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substringBetween(String str, int exclusiveBeginIndex, IntUnaryOperator funcOfExclusiveEndIndex) {
        if (exclusiveBeginIndex < 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substringBetween(String str, char delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substringBetween(String str, String delimiterOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
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
    public static Optional<String> substringBetween(String str, IntUnaryOperator funcOfExclusiveBeginIndex, int exclusiveEndIndex) {
        if (exclusiveEndIndex <= 0) {
            return Optional.<String> empty();
        }

        return substringBetween(str, funcOfExclusiveBeginIndex.applyAsInt(exclusiveEndIndex), exclusiveEndIndex);
    }

    /**
     * 
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>
     * 
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final char prefix, final char postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<IntPair>() : findAllIndicesBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * 
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>
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

        final char[] chs = getCharsForReadOnly(str);
        final Deque<Integer> queue = new LinkedList<>();

        for (int i = idx; i < toIndex; i++) {
            if (chs[i] == prefix) {
                queue.push(i + 1);
            } else if (chs[i] == postfix && queue.size() > 0) {
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
     * 
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>
     * 
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<IntPair> findAllIndicesBetween(final String str, final String prefix, final String postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<IntPair>() : findAllIndicesBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * 
     * <code>findAllIndicesBetween("3[a2[c]]2[a]", '[', ']') = [[2, 7], [10, 11]]</code>
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
     * 
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>
     * 
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final char prefix, final char postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<String>() : findAllSubstringsBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * 
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>
     * 
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final int fromIndex, final int toIndex, final char prefix, final char postfix) {
        final List<IntPair> points = findAllIndicesBetween(str, prefix, postfix);
        final List<String> res = new ArrayList<>(points.size());

        for (IntPair p : points) {
            res.add(str.substring(p._1, p._2));
        }

        return res;
    }

    /**
     * 
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>
     * 
     * @param str
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final String prefix, final String postfix) {
        return N.isNullOrEmpty(str) ? new ArrayList<String>() : findAllSubstringsBetween(str, 0, str.length(), prefix, postfix);
    }

    /**
     * 
     * <code>findAllSubstringsBetween("3[a2[c]]2[a]", '[', ']') = [a2[c], a]</code>
     * 
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param prefix
     * @param postfix
     * @return
     */
    public static List<String> findAllSubstringsBetween(final String str, final int fromIndex, final int toIndex, final String prefix, final String postfix) {
        final List<IntPair> points = findAllIndicesBetween(str, prefix, postfix);
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

    public static String join(final boolean[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final boolean[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final boolean[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final boolean[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final char[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final char[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final char[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final char[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final byte[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final byte[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final byte[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final byte[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final short[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final short[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final short[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final short[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final int[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final int[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final int[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final int[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final long[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final long[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final long[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final long[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final float[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final float[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final float[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final float[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final double[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final double[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final double[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final double[] a, final int fromIndex, final int toIndex, final char delimiter) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
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

    public static String join(final Object[] a) {
        return join(a, N.ELEMENT_SEPARATOR);
    }

    public static String join(final Object[] a, final char delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final Object[] a, final String delimiter) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_STRING;
        }

        return join(a, 0, a.length, delimiter);
    }

    public static String join(final Object[] a, final int fromIndex, final int toIndex, final char delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

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

    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter) {
        return join(a, fromIndex, toIndex, delimiter, false);
    }

    public static String join(final Object[] a, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        } else if (toIndex - fromIndex == 1) {
            return trim ? N.toString(a[fromIndex]).trim() : N.toString(a[fromIndex]);
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
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

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String join(final Collection<?> c) {
        return join(c, N.ELEMENT_SEPARATOR);
    }

    public static String join(final Collection<?> c, final char delimiter) {
        if (N.isNullOrEmpty(c)) {
            return N.EMPTY_STRING;
        }

        return join(c, 0, c.size(), delimiter);
    }

    public static String join(final Collection<?> c, final String delimiter) {
        if (N.isNullOrEmpty(c)) {
            return N.EMPTY_STRING;
        }

        return join(c, 0, c.size(), delimiter);
    }

    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final char delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

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

    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter) {
        return join(c, fromIndex, toIndex, delimiter, false);
    }

    public static String join(final Collection<?> c, final int fromIndex, final int toIndex, final String delimiter, final boolean trim) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c) || fromIndex == toIndex) {
            return N.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
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

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String joinEntries(final Map<?, ?> m) {
        return joinEntries(m, N.ELEMENT_SEPARATOR);
    }

    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter) {
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter) {
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD._EQUAL, trim);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, false);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final boolean trim) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, WD.EQUAL, trim);
    }

    public static String joinEntries(final Map<?, ?> m, final char entryDelimiter, final char keyValueDelimiter) {
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    public static String joinEntries(final Map<?, ?> m, final String entryDelimiter, final String keyValueDelimiter) {
        if (N.isNullOrEmpty(m)) {
            return N.EMPTY_STRING;
        }

        return joinEntries(m, 0, m.size(), entryDelimiter, keyValueDelimiter);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final char entryDelimiter, final char keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, false);
    }

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

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter) {
        return joinEntries(m, fromIndex, toIndex, entryDelimiter, keyValueDelimiter, false);
    }

    public static String joinEntries(final Map<?, ?> m, final int fromIndex, final int toIndex, final String entryDelimiter, final String keyValueDelimiter,
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
     * Returns <code>a + b</code>
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

    public static String concat(final String a, final String b, final String c) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d, final String e) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g,
            final String h) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).append(h).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    public static String concat(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h,
            final String i) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            return sb.append(a).append(b).append(c).append(d).append(e).append(f).append(g).append(h).append(i).toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

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
        return StringUtil.concat(N.toString(a), N.toString(b));
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c));
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d));
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e));
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f));
    }

    /**
     * 
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g));
    }

    /**
     * 
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g), N.toString(h));
    }

    /**
     * 
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
        return StringUtil.concat(N.toString(a), N.toString(b), N.toString(c), N.toString(d), N.toString(e), N.toString(f), N.toString(g), N.toString(h),
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
     * <p>
     * Convert a <code>String</code> to a <code>Integer</code>, handling hex
     * (0xhhhh) and octal (0dddd) notations. N.B. a leading zero means octal;
     * spaces are not trimmed.
     * </p>
     *
     * <p>
     * Returns an empty {@code OptionalInt} if the string is {@code null} or can't be parsed as {@code Integer}.
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static OptionalInt createInteger(final String str) {
        if (N.isNullOrEmpty(str)) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.decode(str));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * <p>
     * Convert a <code>String</code> to a <code>Long</code>; since 3.1 it
     * handles hex (0Xhhhh) and octal (0ddd) notations. N.B. a leading zero
     * means octal; spaces are not trimmed.
     * </p>
     *
     * <p>
     * Returns an empty {@code OptionalLong} if the string is {@code null} or can't be parsed as {@code Long}.
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static OptionalLong createLong(final String str) {
        if (N.isNullOrEmpty(str)) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.decode(str));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    // -----------------------------------------------------------------------
    /**
     * <p>
     * Convert a <code>String</code> to a <code>Float</code>.
     * </p>
     *
     * <p>
     * Returns an empty {@code OptionalFloat} if the string is {@code null} or can't be parsed as {@code Float}.
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static OptionalFloat createFloat(final String str) {
        if (N.isNullOrEmpty(str)) {
            return OptionalFloat.empty();
        }

        try {
            return OptionalFloat.of(Float.parseFloat(str));
        } catch (NumberFormatException e) {
            return OptionalFloat.empty();
        }
    }

    /**
     * <p>
     * Convert a <code>String</code> to a <code>Double</code>.
     * </p>
     *
     * <p>
     * <p>
     * Returns an empty {@code OptionalDouble} if the string is {@code null} or can't be parsed as {@code Double}.
     * </p>
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static OptionalDouble createDouble(final String str) {
        if (N.isNullOrEmpty(str)) {
            return OptionalDouble.empty();
        }

        try {
            return OptionalDouble.of(Double.parseDouble(str));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * <p>
     * Convert a <code>String</code> to a <code>BigInteger</code>; since 3.2 it
     * handles hex (0x or #) and octal (0) notations.
     * </p>
     *
     * <p>
     * Returns an empty {@code Optional} if the string is {@code null} or can't be parsed as {@code BigInteger}.
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static Optional<BigInteger> createBigInteger(final String str) {
        if (N.isNullOrEmptyOrBlank(str)) {
            return Optional.empty();
        }

        int pos = 0; // offset within string
        int radix = 10;
        boolean negate = false; // need to negate later?
        if (str.startsWith("-")) {
            negate = true;
            pos = 1;
        }
        if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) { // hex
            radix = 16;
            pos += 2;
        } else if (str.startsWith("#", pos)) { // alternative hex (allowed by Long/Integer)
            radix = 16;
            pos++;
        } else if (str.startsWith("0", pos) && str.length() > pos + 1) { // octal; so long as there are additional digits
            radix = 8;
            pos++;
        } // default is to treat as decimal

        try {
            final BigInteger value = new BigInteger(str.substring(pos), radix);
            return Optional.of(negate ? value.negate() : value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * Convert a <code>String</code> to a <code>BigDecimal</code>.
     * </p>
     *
     * <p>
     * Returns an empty {@code Optional} if the string is {@code null} or can't be parsed as {@code BigDecimal}.
     * </p>
     *
     * @param str a <code>String</code> to convert, may be null
     * @return
     */
    public static Optional<BigDecimal> createBigDecimal(final String str) {
        if (N.isNullOrEmptyOrBlank(str) || str.trim().startsWith("--")) {
            return Optional.empty();
        }

        try {
            return Optional.of(new BigDecimal(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean[] alphanumerics = new boolean[128];

    static {
        alphanumerics['0'] = true;
        alphanumerics['1'] = true;
        alphanumerics['2'] = true;
        alphanumerics['3'] = true;
        alphanumerics['4'] = true;
        alphanumerics['5'] = true;
        alphanumerics['6'] = true;
        alphanumerics['7'] = true;
        alphanumerics['8'] = true;
        alphanumerics['9'] = true;

        alphanumerics['+'] = true;
        alphanumerics['-'] = true;
        alphanumerics['.'] = true;
        alphanumerics['#'] = true;

        alphanumerics['x'] = true;
        alphanumerics['X'] = true;

        alphanumerics['e'] = true;
        alphanumerics['E'] = true;

        alphanumerics['a'] = true;
        alphanumerics['b'] = true;
        alphanumerics['c'] = true;
        alphanumerics['d'] = true;
        alphanumerics['e'] = true;
        alphanumerics['f'] = true;

        alphanumerics['l'] = true;
        alphanumerics['L'] = true;
        alphanumerics['f'] = true;
        alphanumerics['F'] = true;
        alphanumerics['d'] = true;
        alphanumerics['D'] = true;
    }

    /**
     * <p>
     * Turns a string value into a java.lang.Number.
     * </p>
     *
     * <p>
     * If the string starts with {@code 0x} or {@code -0x} (lower or upper case)
     * or {@code #} or {@code -#}, it will be interpreted as a hexadecimal
     * Integer - or Long, if the number of digits after the prefix is more than
     * 8 - or BigInteger if there are more than 16 digits.
     * </p>
     * <p>
     * Then, the value is examined for a type qualifier on the end, i.e. one of
     * <code>'f','F','d','D','l','L'</code>. If it is found, it starts trying to
     * create successively larger types from the type specified until one is
     * found that can represent the value.
     * </p>
     *
     * <p>
     * If a type specifier is not found, it will check for a decimal point and
     * then try successively larger types from <code>Integer</code> to
     * <code>BigInteger</code> and from <code>double</code> to
     * <code>BigDecimal</code>.
     * </p>
     *
     * <p>
     * Integral values with a leading {@code 0} will be interpreted as octal;
     * the returned number will be Integer, Long or BigDecimal as appropriate.
     * </p>
     *
     * <p>
     * Returns an empty {@code Optional} if the string is {@code null} or can't be parsed as {@code Number}.
     * </p>
     *
     *
     * @param str a String containing a number, may be null
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Optional<Number> createNumber(final String str) {
        if (N.isNullOrEmptyOrBlank(str)) {
            return Optional.empty();
        }

        char ch = 0;

        if (!(((ch = str.charAt(0)) < 128 && alphanumerics[ch]) && ((ch = str.charAt(str.length() - 1)) < 128 && alphanumerics[ch])
                && ((ch = str.charAt(str.length() / 2)) < 128 && alphanumerics[ch]))) {
            return Optional.empty();
        }

        // Need to deal with all possible hex prefixes here
        final String[] hex_prefixes = { "0x", "0X", "-0x", "-0X", "#", "-#" };
        int pfxLen = 0;
        for (final String pfx : hex_prefixes) {
            if (str.startsWith(pfx)) {
                pfxLen += pfx.length();
                break;
            }
        }
        if (pfxLen > 0) { // we have a hex number
            char firstSigDigit = 0; // strip leading zeroes

            for (int i = pfxLen; i < str.length(); i++) {
                firstSigDigit = str.charAt(i);
                if (firstSigDigit == '0') { // count leading zeroes
                    pfxLen++;
                } else {
                    break;
                }
            }

            final int hexDigits = str.length() - pfxLen;

            if (hexDigits > 16 || hexDigits == 16 && firstSigDigit > '7') { // too many for Long
                return (Optional) createBigInteger(str);
            } else if (hexDigits > 8 || hexDigits == 8 && firstSigDigit > '7') { // too many for an int
                return (Optional) createLong(str).boxed();
            } else {
                return (Optional) createInteger(str).boxed();
            }
        }

        final char lastChar = str.charAt(str.length() - 1);
        String mant;
        String dec;
        String exp;
        final int decPos = str.indexOf('.');
        final int expPos = str.indexOf('e') + str.indexOf('E') + 1; // assumes both not present
        // if both e and E are present, this is caught by the checks on expPos (which prevent IOOBE)
        // and the parsing which will detect if e or E appear in a number due to using the wrong offset

        Optional<? extends Number> op = null;

        if (decPos > -1) { // there is a decimal point
            if (expPos > -1) { // there is an exponent
                if (expPos < decPos || expPos > str.length()) { // prevents double exponent causing IOOBE
                    return Optional.empty();
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }

            mant = getMantissa(str, decPos);
        } else {
            if (expPos > -1) {
                if (expPos > str.length()) { // prevents double exponent causing IOOBE
                    return Optional.empty();
                }
                mant = getMantissa(str, expPos);
            } else {
                mant = getMantissa(str);
            }

            dec = null;
        }

        if (!Character.isDigit(lastChar) && lastChar != '.') {
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length() - 1);
            } else {
                exp = null;
            }

            //Requesting a specific type..
            final String numeric = str.substring(0, str.length() - 1);
            final boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
            switch (lastChar) {
                case 'l':
                case 'L':
                    if (dec == null && exp == null
                            && (numeric.charAt(0) == '-' && StringUtil.isNumeric(numeric.substring(1)) || StringUtil.isNumeric(numeric))) {

                        op = createLong(numeric).boxed();

                        if (op.isPresent()) {
                            return (Optional) op;
                        } else {
                            return (Optional) createBigInteger(numeric);
                        }
                    }

                    return Optional.empty();

                case 'f':
                case 'F':
                    try {
                        final Float f = Float.valueOf(str);

                        if (!(f.isInfinite() || f.floatValue() == 0.0F && !allZeros)) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float does not have the precision we want
                            return (Optional) Optional.of(f);
                        }

                    } catch (final NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                case 'd':
                case 'D':
                    try {
                        final Double d = Double.valueOf(str);

                        if (!(d.isInfinite() || d.floatValue() == 0.0D && !allZeros)) {
                            return (Optional) Optional.of(d);
                        }
                    } catch (final NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }

                    return (Optional) createBigDecimal(numeric);

                //$FALL-THROUGH$
                default:
                    return Optional.empty();
            }
        }

        //User doesn't have a preference on the return type, so let's start
        //small and go from there...
        if (expPos > -1 && expPos < str.length() - 1) {
            exp = str.substring(expPos + 1, str.length());
        } else {
            exp = null;
        }

        if (dec == null && exp == null) { // no decimal point and no exponent
            //Must be an Integer, Long, Biginteger          
            op = createInteger(str).boxed();

            if (op.isPresent()) {
                return (Optional) op;
            } else {
                op = createLong(str).boxed();

                if (op.isPresent()) {
                    return (Optional) op;
                } else {
                    return (Optional) createBigInteger(str);
                }
            }
        }

        //Must be a Float, Double, BigDecimal
        final boolean allZeros = isAllZeros(mant) && isAllZeros(exp);

        try {
            final Float f = Float.valueOf(str);
            final Double d = Double.valueOf(str);

            if (!f.isInfinite() && !(f.floatValue() == 0.0F && !allZeros) && f.toString().equals(d.toString())) {
                return (Optional) Optional.of(f);
            }

            if (!d.isInfinite() && !(d.doubleValue() == 0.0D && !allZeros)) {
                final Optional<BigDecimal> b = createBigDecimal(str);

                if (b.isPresent() && b.get().compareTo(BigDecimal.valueOf(d.doubleValue())) == 0) {
                    return (Optional) Optional.of(d);
                } else {
                    return (Optional) b;
                }
            }
        } catch (final NumberFormatException nfe) { // NOPMD
            // ignore the bad number
        }

        return (Optional) createBigDecimal(str);
    }

    /**
     * <p>Utility method for {@link #createNumber(java.lang.String)}.</p>
     *
     * <p>Returns mantissa of the given number.</p>
     *
     * @param str the string representation of the number
     * @return mantissa of the given number
     */
    private static String getMantissa(final String str) {
        return getMantissa(str, str.length());
    }

    /**
     * <p>Utility method for {@link #createNumber(java.lang.String)}.</p>
     *
     * <p>Returns mantissa of the given number.</p>
     *
     * @param str the string representation of the number
     * @param stopPos the position of the exponent or decimal point
     * @return mantissa of the given number
     */
    private static String getMantissa(final String str, final int stopPos) {
        final char firstChar = str.charAt(0);
        final boolean hasSign = firstChar == '-' || firstChar == '+';

        return hasSign ? str.substring(1, stopPos) : str.substring(0, stopPos);
    }

    private static boolean isAllZeros(final String str) {
        if (str == null) {
            return true;
        }

        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }

        return str.length() > 0;
    }

    /**
     * Copied from Google Guava
     * 
     * <br />
     * 
     * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced with
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
     * String.format} is not available. As an example, method implementations of the {@link
     * Preconditions} class use this formatter, for both of the reasons just discussed.
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
     * Returns a new sorted String if the specified {@code str} is not null or empty, otherwise the specified {@code str} is returned.
     * 
     * @param str
     * @return
     */
    public static String sort(String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        final char[] chs = str.toCharArray();
        Array.sort(chs);
        return StringUtil.newString(chs, true);
    }

    @Beta
    @Internal
    @Deprecated
    public static char[] getCharsForReadOnly(final String str) {
        if (isStringCharsGettable && strValueField != null && str.length() > 3) {
            try {
                final char[] chars = (char[]) strValueField.get(str);

                if (chars.length == str.length()) {
                    return chars;
                } else {
                    isStringCharsGettable = false;
                }

            } catch (Exception e) {
                // ignore.
                isStringCharsGettable = false;
            }
        }

        return str.toCharArray();
    }

    /**
     *
     * @param a
     *            the specified array should not be modified after it's used to
     *            create the new String.
     * @param share
     *            the same array will be shared with the new created ArrayList
     *            if it's true.
     * @return
     */
    @Internal
    static String newString(final char[] a, final boolean share) {
        if (share && sharedStringConstructor != null) {
            try {
                return sharedStringConstructor.newInstance(a, true);
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        } else {
            return String.valueOf(a);
        }
    }

    public static final class Strings extends StringUtil {

        private Strings() {
            // Singleton.
        }
    }
}
