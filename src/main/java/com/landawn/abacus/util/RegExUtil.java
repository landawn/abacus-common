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

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.IntBiFunction;

/**
 * Note: Copied from Apache Commons Lang under Apache License V2.
 *
 * <br />
 *
 * <p>Helpers to process Strings using regular expressions.</p>
 * @see java.util.regex.Pattern
 * @see java.util.regex.Matcher
 * @see com.landawn.abacus.util.Strings
 */
public final class RegExUtil {

    /**
     * Pattern for a Java identifier.
     * <p>
     * A Java identifier is a letter, currency character, or connecting punctuation character followed by
     * zero or more Java letters, currency characters, connecting punctuation characters, digits,
     * or one of the following special characters: {@code $} or {@code _}.
     * </p>
     */
    public static final Pattern JAVA_IDENTIFIER_FINDER = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*)");

    /**
     * A regular expression {@link Pattern} that matches integers within a string.
     * <p>
     * This pattern captures sequences of digits that may optionally be prefixed
     * with a plus or minus sign. It is useful for extracting signed or unsigned
     * integer values from arbitrary text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [+-]?} — matches an optional '+' or '-' sign</li>
     *   <li>{@code \\d+} — matches one or more digits</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "-7"}</li>
     *   <li>{@code "+1234"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern INTEGER_FINDER = Pattern.compile("([+-]?\\d+)");
    public static final Pattern POSITIVE_INTEGER_FINDER = Pattern.compile("\\d+");
    public static final Pattern NEGATIVE_INTEGER_FINDER = Pattern.compile("-\\d+");

    /**
     * A regular expression {@link Pattern} that matches integer and decimal numbers in a string.
     * <p>
     * This pattern captures numeric values that may include:
     * <ul>
     *   <li>An optional leading '+' or '-' sign</li>
     *   <li>An integer part (one or more digits)</li>
     *   <li>An optional fractional part, starting with a dot and followed by one or more digits</li>
     * </ul>
     * It can be used to extract signed or unsigned integers and floating-point numbers from text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [+-]?} — optional sign</li>
     *   <li>{@code \\d+} — one or more digits (the integer part)</li>
     *   <li>{@code (\\.\\d+)?} — optional decimal part (a dot followed by one or more digits)</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "-3.14"}</li>
     *   <li>{@code "+0.99"}</li>
     *   <li>{@code "100."} (not matched)</li>
     *   <li>{@code ".25"} (not matched)</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This pattern does not match numbers like {@code .25} or {@code 100.}
     * because a digit before the dot is required and the fractional part must have at least one digit after the dot.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern NUMBER_FINDER = Pattern.compile("([+-]?\\d+(\\.\\d+)?)");
    public static final Pattern POSITIVE_NUMBER_FINDER = Pattern.compile("\\d*\\.?\\d+");
    public static final Pattern NEGATIVE_NUMBER_FINDER = Pattern.compile("-\\d*\\.?\\d+");

    /**
     * A regular expression {@link Pattern} that matches numbers in standard or scientific notation.
     * <p>
     * This pattern supports:
     * <ul>
     *   <li>Optional leading '+' or '-' sign</li>
     *   <li>An integer or decimal part (e.g., {@code 123}, {@code 3.14})</li>
     *   <li>An optional exponent part with 'e' or 'E', followed by an optional sign and digits (e.g., {@code e+10}, {@code E-5})</li>
     * </ul>
     * It can be used to extract integers, floating-point numbers, and scientific notation numbers from text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [+-]?} — optional sign</li>
     *   <li>{@code \\d+} — one or more digits (integer part)</li>
     *   <li>{@code (\\.\\d+)?} — optional decimal part</li>
     *   <li>{@code ([eE][+-]?\\d+)?} — optional exponent part (e.g., {@code e10}, {@code E-3})</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "-3.14"}</li>
     *   <li>{@code "+6.022e23"}</li>
     *   <li>{@code "1E-9"}</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This pattern requires at least one digit before the decimal point, 
     * so values like {@code .5} are not matched. It also allows optional exponent notation, but only 
     * when preceded by a valid base number.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern SCIENTIFIC_NUMBER_FINDER = Pattern.compile("([+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)");

    public static final Pattern PHONE_NUMBER_FINDER = Pattern.compile("\\+?[\\d\\s]{3,}");
    public static final Pattern PHONE_NUMBER_WITH_CODE_FINDER = Pattern.compile("\\+?[\\d\\s]+\\(?[\\d\\s]{10,}");

    /**
     * Pattern for date in format yyyy-MM-dd or yyyy/MM/dd or yyyy.MM.dd or yyyy MM dd.
     * <br /> year is from 1900 to 2099.
     */
    public static final Pattern DATE_FINDER = Pattern.compile("(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])");
    /**
     * Pattern for time in format HH:mm:ss.
     * <br /> hour is from 00 to 23, minute and second are from 00 to 59.
     */
    public static final Pattern TIME_FINDER = Pattern.compile("([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)");
    /**
     * Pattern for date and time in format yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss or yyyy.MM.dd HH:mm:ss or yyyy MM dd HH:mm:ss.
     * <br /> year is from 1900 to 2099, hour is from 00 to 23, minute and second are from 00 to 59.
     */
    public static final Pattern DATE_TIME_FINDER = Pattern
            .compile("(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01]) ([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)");

    public static final Pattern BANK_CARD_NUMBER_FINDER = Pattern.compile("(?:\\d{4}[-\\s]?){3}\\d{4}");

    // https://www.baeldung.com/java-email-validation-regex
    // https://owasp.org/www-community/OWASP_Validation_Regex_Repository
    // https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
    public static final Pattern EMAIL_ADDRESS_RFC_5322_FINDER = Pattern.compile(
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    // https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url
    public static final Pattern URL_FINDER = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
    public static final Pattern HTTP_URL_FINDER = Pattern.compile("[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");

    /**
     * Pattern for alphanumeric string without space.
     */
    public static final Pattern ALPHANUMERIC_FINDER = Pattern.compile("\\+?[\\d\\s]+\\(?[\\d\\s]{10,}");
    /**
     * Pattern for alphanumeric string with space.
     */
    public static final Pattern ALPHANUMERIC_SPACE_FINDER = Pattern.compile("\\+?[\\d\\s]+\\(?[\\d\\s]{10,}");
    /**
     * Pattern for duplicate words.
     */
    public static final Pattern DUPLICATES_FINDER = Pattern.compile("(\\b\\w+\\b)(?=.*\\b\\1\\b)");

    // https://stackoverflow.com/questions/1449817/what-are-some-of-the-most-useful-regular-expressions-for-programmers
    public static final Pattern JAVA_IDENTIFIER_MATCHER = Pattern.compile("^" + JAVA_IDENTIFIER_FINDER.pattern() + "$");
    public static final Pattern INTEGER_MATCHER = Pattern.compile("^" + INTEGER_FINDER.pattern() + "$");
    public static final Pattern POSITIVE_INTEGER_MATCHER = Pattern.compile("^" + POSITIVE_INTEGER_FINDER.pattern() + "$");
    public static final Pattern NEGATIVE_INTEGER_MATCHER = Pattern.compile("^" + NEGATIVE_INTEGER_FINDER.pattern() + "$");
    public static final Pattern NUMBER_MATCHER = Pattern.compile("^" + NUMBER_FINDER.pattern() + "$");
    public static final Pattern POSITIVE_NUMBER_MATCHER = Pattern.compile("^" + POSITIVE_NUMBER_FINDER.pattern() + "$");
    public static final Pattern NEGATIVE_NUMBER_MATCHER = Pattern.compile("^" + NEGATIVE_NUMBER_FINDER.pattern() + "$");
    public static final Pattern SCIENTIFIC_NUMBER_MATCHER = Pattern.compile("^" + SCIENTIFIC_NUMBER_FINDER.pattern() + "$");

    public static final Pattern PHONE_NUMBER_MATCHER = Pattern.compile("^" + PHONE_NUMBER_FINDER.pattern() + "$");
    public static final Pattern PHONE_NUMBER_WITH_CODE_MATCHER = Pattern.compile("^" + PHONE_NUMBER_WITH_CODE_FINDER.pattern() + "$");

    public static final Pattern DATE_MATCHER = Pattern.compile("^" + DATE_FINDER.pattern() + "$");
    public static final Pattern TIME_MATCHER = Pattern.compile("^" + TIME_FINDER.pattern() + "$");
    public static final Pattern DATE_TIME_MATCHER = Pattern.compile("^" + DATE_TIME_FINDER.pattern() + "$");

    public static final Pattern BANK_CARD_NUMBER_MATCHER = Pattern.compile("^" + BANK_CARD_NUMBER_FINDER.pattern() + "$");

    public static final Pattern EMAIL_ADDRESS_RFC_5322_MATCHER = Pattern.compile("^" + EMAIL_ADDRESS_RFC_5322_FINDER.pattern() + "$");

    public static final Pattern URL_MATCHER = Pattern.compile("^" + URL_FINDER.pattern() + "$");
    public static final Pattern HTTP_URL_MATCHER = Pattern.compile("^" + HTTP_URL_FINDER.pattern() + "$");

    public static final Pattern ALPHANUMERIC_MATCHER = Pattern.compile("^" + ALPHANUMERIC_FINDER.pattern() + "$");
    public static final Pattern ALPHANUMERIC_SPACE_MATCHER = Pattern.compile("^" + ALPHANUMERIC_SPACE_FINDER.pattern() + "$");

    public static final Pattern DUPLICATES_MATCHER = Pattern.compile("^" + DUPLICATES_FINDER.pattern() + "$");

    private static final String PATTERNS_STR = "pattern";

    private RegExUtil() {
        // Singleton for utility class.
    }

    /**
     * <p>Removes each substring of the source string that matches the given regular expression.</p>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with any removes processed,  {@code null} if {@code null} String input
     * @see #replaceAll(String, String, String)
     * @see String#replaceAll(String, String)
     */
    public static String removeAll(final String source, final String regex) {
        return replaceAll(source, regex, Strings.EMPTY);
    }

    /**
     * <p>Removes each substring of the source string that matches the given regular expression pattern.</p>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @return the source string with any removes processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceAll(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceAll(String)
     */
    public static String removeAll(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceAll(source, pattern, Strings.EMPTY);
    }

    /**
     * <p>Removes the first substring of the source string that matches the given regular expression.</p>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the first replacement processed,  {@code null} if {@code null} String input
     * @see #replaceFirst(String, String, String)
     * @see String#replaceFirst(String, String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String source, final String regex) {
        return replaceFirst(source, regex, Strings.EMPTY);
    }

    /**
     * <p>Removes the first substring of the source string that matches the given regular expression pattern.</p>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceFirst(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceFirst(String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceFirst(source, pattern, Strings.EMPTY);
    }

    /**
     * <p>Removes the last substring of the source string that matches the given regular expression.</p>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the last replacement processed,  {@code null} if {@code null} String input
     * @see #replaceFirst(String, String, String)
     * @see java.util.regex.Pattern
     */
    @Beta
    public static String removeLast(final String source, final String regex) {
        return replaceLast(source, regex, Strings.EMPTY);
    }

    /**
     * <p>Removes the last substring of the source string that matches the given regular expression pattern.</p>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @return the source string with the last replacement processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceLast(String, Pattern, String)
     * @see java.util.regex.Pattern
     */
    @Beta
    public static String removeLast(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceLast(source, pattern, Strings.EMPTY);
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code source.replaceAll(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(source).replaceAll(replacement)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for each match
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final String replacement) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceAll(source, Pattern.compile(regex), Strings.nullToEmpty(replacement));
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code source.replaceAll(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(source).replaceAll(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final Function<String, String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code source.replaceAll(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(source).replaceAll(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final IntBiFunction<String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression pattern with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(source).replaceAll(replacement)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for each match
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceAll(String)
     * @see java.util.regex.Pattern
     */
    public static String replaceAll(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceAll(Strings.nullToEmpty(replacement));
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code Pattern.compile(regex).matcher(source).replaceAll(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceAll(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())));
    }

    /**
     * <p>Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code Pattern.compile(regex).matcher(source).replaceAll(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceAll(matcher -> replacer.apply(matcher.start(), matcher.end()));
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression with the given replacement.</p>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final String replacement) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceFirst(source, Pattern.compile(regex), N.nullToEmpty(replacement));
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression with the given replacement.</p>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final Function<String, String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression with the given replacement.</p>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final IntBiFunction<String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression pattern with the given replacement.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(source).replaceFirst(replacement)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceFirst(Strings.nullToEmpty(replacement));
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression pattern with the given replacer.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(source).replaceFirst(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceFirst(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())));
    }

    /**
     * <p>Replaces the first substring of the source string that matches the given regular expression pattern with the given replacer.</p>
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code pattern.matcher(source).replaceFirst(replacer)}</li>
     * </ul>
     *
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, {@code null} if {@code null} String input
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        return pattern.matcher(source).replaceFirst(matcher -> replacer.apply(matcher.start(), matcher.end()));
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacement}.
     *
     * @param source
     * @param regex
     * @param replacement
     * @return
     * @see Matcher#replaceFirst(String)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final String replacement) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceLast(source, Pattern.compile(regex), replacement);
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * @param source
     * @param regex
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final Function<String, String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * @param source
     * @param regex
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final IntBiFunction<String> replacer) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return source;
        }

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacement}.
     *
     * @param source
     * @param pattern
     * @param replacement
     * @return
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(String)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        final Matcher matcher = pattern.matcher(source);

        for (int start = -1, end = -1, i = source.length(); i >= 0; i--) {
            if (matcher.find(i)) {
                if (start < 0 || (matcher.start() < start && matcher.end() >= end)) {
                    start = matcher.start();
                    end = matcher.end();
                } else {
                    return Strings.replaceRange(source, start, end, replacement);
                }
            } else if (start >= 0) {
                return Strings.replaceRange(source, start, end, replacement);
            }
        }

        return source;
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * @param source
     * @param pattern
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        final Matcher matcher = pattern.matcher(source);

        for (int start = -1, end = -1, i = source.length(); i >= 0; i--) {
            if (matcher.find(i)) {
                if (start < 0 || (matcher.start() < start && matcher.end() >= end)) {
                    start = matcher.start();
                    end = matcher.end();
                } else {
                    return Strings.replaceRange(source, start, end, replacer.apply(source.substring(matcher.start(), matcher.end())));
                }
            } else if (start >= 0) {
                return Strings.replaceRange(source, start, end, replacer.apply(source.substring(matcher.start(), matcher.end())));
            }
        }

        return source;
    }

    /**
     * Searches the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * @param source
     * @param pattern
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return source;
        }

        final Matcher matcher = pattern.matcher(source);

        for (int start = -1, end = -1, i = source.length(); i >= 0; i--) {
            if (matcher.find(i)) {
                if (start < 0 || (matcher.start() < start && matcher.end() >= end)) {
                    start = matcher.start();
                    end = matcher.end();
                } else {
                    return Strings.replaceRange(source, start, end, replacer.apply(matcher.start(), matcher.end()));
                }
            } else if (start >= 0) {
                return Strings.replaceRange(source, start, end, replacer.apply(matcher.start(), matcher.end()));
            }
        }

        return source;
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regexToFind the regular expression pattern to be counted
     * @return the number of occurrences of the specified pattern in the string, or 0 if the string is {@code null} or empty
     * @see #countMatches(String, String)
     */
    public static int countMatches(final String source, final String regexToFind) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regexToFind)) {
            return 0;
        }

        return countMatches(source, Pattern.compile(regexToFind));
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return the number of occurrences of the specified pattern in the string, or 0 if the string is {@code null} or empty
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #countMatches(String, String)
     */
    public static int countMatches(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return 0;
        }

        final Matcher matcher = pattern.matcher(source);
        int occurrences = 0;

        while (matcher.find()) {
            occurrences++;
        }

        return occurrences;
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regexToFind the regular expression pattern to be counted
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern.
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final String regexToFind) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regexToFind)) {
            return Stream.empty();
        }

        return matchResults(source, Pattern.compile(regexToFind));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return Stream.empty();
        }

        return pattern.matcher(source).results();
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regexToFind the regular expression pattern to be counted
     * @return a stream of start indices for each subsequence of the input sequence that matches the pattern
     * @see Matcher#results()
     * @see Strings#indicesOf(String, String)
     * @see Strings#indicesOf(String, String, int)
     */
    public static IntStream matchIndices(final String source, final String regexToFind) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regexToFind)) {
            return IntStream.empty();
        }

        return matchIndices(source, Pattern.compile(regexToFind));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return a stream of start indices for each subsequence of the input sequence that matches the pattern
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#results()
     * @see Strings#indicesOf(String, String)
     * @see Strings#indicesOf(String, String, int)
     */
    public static IntStream matchIndices(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, PATTERNS_STR);

        if (Strings.isEmpty(source)) {
            return IntStream.empty();
        }

        return pattern.matcher(source).results().mapToInt(MatchResult::start);
    }

    /**
     * Checks if the specified string matches the given regular expression.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression to match
     * @return {@code true} if the string matches the regex, {@code false} otherwise
     * @see Pattern#matches(String, CharSequence)
     */
    public static boolean matches(final String source, final String regex) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return false;
        }

        return Pattern.matches(regex, source);
    }

    /**
     * Checks if the specified string contains a substring that matches the given regular expression.
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression to find
     * @return {@code true} if the string contains a match, {@code false} otherwise
     * @see Pattern#find(CharSequence)
     */
    public static boolean find(final String source, final String regex) {
        if (Strings.isEmpty(source) || Strings.isEmpty(regex)) {
            return false;
        }

        return Pattern.compile(regex).matcher(source).find();
    }
}
