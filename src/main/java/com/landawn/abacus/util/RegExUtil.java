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
 * Utility class providing helper methods for processing Strings using regular expressions.
 * This class offers a comprehensive set of pre-compiled patterns for common use cases like
 * finding numbers, dates, emails, URLs, etc., as well as methods for pattern matching,
 * replacement, splitting, and more.
 * 
 * <p>All methods in this class are thread-safe and null-safe unless otherwise specified.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Check if string contains a number
 * if (RegExUtil.find("Price: $99.99", RegExUtil.NUMBER_FINDER.pattern())) {
 *     // Found a number
 * }
 * 
 * // Replace all whitespace
 * String cleaned = RegExUtil.replaceAll("Hello   World", "\\s+", " ");
 * // Result: "Hello World"
 * }</pre>
 * 
 * @see java.util.regex.Pattern
 * @see java.util.regex.Matcher
 * @see com.landawn.abacus.util.Strings
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/package-summary.html">Java Regular Expressions</a>
 * @see <a href="https://quickref.me/regex.html">quickref</a>
 */
public final class RegExUtil {

    /**
     * A regular expression {@link Pattern} that matches valid Java identifiers within a string.
     * <p>
     * This pattern captures sequences that follow Java's identifier naming rules: starting with
     * a letter (uppercase or lowercase), underscore, or dollar sign, followed by any number of
     * letters, digits, underscores, or dollar signs. It is useful for extracting variable names,
     * method names, class names, and other identifiers from Java source code or text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code ([a-zA-Z_$]} — capturing group starting with a letter, underscore, or dollar sign</li>
     *   <li>{@code [a-zA-Z\\d_$]*)} — followed by zero or more letters, digits, underscores, or dollar signs</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "myVariable"}</li>
     *   <li>{@code "_privateField"}</li>
     *   <li>{@code "$specialVar"}</li>
     *   <li>{@code "className123"}</li>
     *   <li>{@code "MAX_VALUE"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "123invalid"} (starts with digit)</li>
     *   <li>{@code "my-variable"} (contains hyphen)</li>
     *   <li>{@code "class"} (Java keyword, but matches pattern - validation needed separately)</li>
     * </ul>
     *
     * <p><b>Note:</b> This pattern matches the syntactic structure of Java identifiers but does not
     * validate against Java reserved keywords. For complete validation, use this pattern in conjunction
     * with keyword checking.</p>
     *
     * @see #JAVA_IDENTIFIER_MATCHER
     * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.8">Java Language Specification - Identifiers</a>
     * @see java.util.regex.Pattern
     */
    public static final Pattern JAVA_IDENTIFIER_FINDER = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*)");


    /**
     * A regular expression {@link Pattern} that matches signed integers within a string.
     * <p>
     * This pattern captures sequences of digits that may be preceded by an optional plus or minus sign.
     * It is useful for extracting integer values (both positive and negative) from arbitrary text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code ([+-]?} — capturing group with optional plus or minus sign</li>
     *   <li>{@code \\d+)} — followed by one or more digits</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "+123"}</li>
     *   <li>{@code "-456"}</li>
     *   <li>{@code "0"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "12.34"} (contains decimal point)</li>
     *   <li>{@code "abc"} (contains letters)</li>
     *   <li>{@code "1.5e10"} (scientific notation)</li>
     * </ul>
     *
     * <p><b>Note:</b> This pattern matches integer values but does not validate for overflow
     * or underflow of specific integer types like {@code int} or {@code long}. Additional
     * validation may be needed when converting to specific numeric types.</p>
     *
     * @see #POSITIVE_INTEGER_FINDER
     * @see #NEGATIVE_INTEGER_FINDER
     * @see #INTEGER_MATCHER
     * @see #NUMBER_FINDER
     * @see java.util.regex.Pattern
     */
    public static final Pattern INTEGER_FINDER = Pattern.compile("([+-]?\\d+)");

    /**
     * A regular expression {@link Pattern} that matches positive (unsigned) integers within a string.
     * <p>
     * This pattern captures sequences of one or more digits without any sign prefix.
     * It is useful for extracting positive integer values from arbitrary text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\d+} — matches one or more digits</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "1234"}</li>
     *   <li>{@code "0"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern POSITIVE_INTEGER_FINDER = Pattern.compile("\\d+");

    /**
     * A regular expression {@link Pattern} that matches negative integers within a string.
     * <p>
     * This pattern captures sequences of digits that are prefixed with a minus sign.
     * It is useful for extracting negative integer values from arbitrary text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code -} — matches the minus sign</li>
     *   <li>{@code \\d+} — matches one or more digits</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "-7"}</li>
     *   <li>{@code "-1234"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
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

    /**
     * A regular expression {@link Pattern} that matches positive (unsigned) numbers including decimals.
     * <p>
     * This pattern captures numeric values that may include:
     * <ul>
     *   <li>An optional integer part (zero or more digits)</li>
     *   <li>An optional decimal point</li>
     *   <li>A fractional part (one or more digits)</li>
     * </ul>
     * It can match numbers like {@code .25}, {@code 3.14}, or {@code 100}.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\d*} — zero or more digits (the optional integer part)</li>
     *   <li>{@code \\.?} — optional decimal point</li>
     *   <li>{@code \\d+} — one or more digits (required)</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "3.14"}</li>
     *   <li>{@code "0.99"}</li>
     *   <li>{@code ".25"}</li>
     *   <li>{@code "100."}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern POSITIVE_NUMBER_FINDER = Pattern.compile("\\d*\\.?\\d+");

    /**
     * A regular expression {@link Pattern} that matches negative numbers including decimals.
     * <p>
     * This pattern captures negative numeric values that may include:
     * <ul>
     *   <li>A required minus sign prefix</li>
     *   <li>An optional integer part (zero or more digits)</li>
     *   <li>An optional decimal point</li>
     *   <li>A fractional part (one or more digits)</li>
     * </ul>
     * It can match numbers like {@code -.25}, {@code -3.14}, or {@code -100}.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code -} — required minus sign</li>
     *   <li>{@code \\d*} — zero or more digits (the optional integer part)</li>
     *   <li>{@code \\.?} — optional decimal point</li>
     *   <li>{@code \\d+} — one or more digits (required)</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "-7"}</li>
     *   <li>{@code "-3.14"}</li>
     *   <li>{@code "-0.99"}</li>
     *   <li>{@code "-.25"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
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

    /**
     * A regular expression {@link Pattern} that matches phone numbers within a string.
     * <p>
     * This pattern matches sequences of digits and spaces that are at least 3 characters long,
     * optionally prefixed with a plus sign for international dialing codes.
     * It is useful for extracting basic phone numbers from text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\+?} — optional plus sign for international code</li>
     *   <li>{@code [\\d\\s]{3,}} — at least 3 digits or spaces</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "123 456 7890"}</li>
     *   <li>{@code "+1 234 567 8900"}</li>
     *   <li>{@code "555-1234"} (note: hyphens not matched, only digits and spaces)</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern PHONE_NUMBER_FINDER = Pattern.compile("\\+?[\\d\\s]{3,}");

    /**
     * A regular expression {@link Pattern} that matches phone numbers with country codes.
     * <p>
     * This pattern matches phone numbers with an optional plus sign, followed by digits and spaces,
     * with an optional opening parenthesis, and requires at least 10 digits/spaces after that.
     * It is designed to match longer phone numbers that include country and area codes.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\+?} — optional plus sign for international code</li>
     *   <li>{@code [\\d\\s]+} — one or more digits or spaces</li>
     *   <li>{@code \\(?} — optional opening parenthesis</li>
     *   <li>{@code [\\d\\s]{10,}} — at least 10 digits or spaces</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "+1 (234) 567 8900"}</li>
     *   <li>{@code "+44 20 1234 5678"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern PHONE_NUMBER_WITH_CODE_FINDER = Pattern.compile("\\+?[\\d\\s]+\\(?[\\d\\s]{10,}");

    /**
     * A regular expression {@link Pattern} that matches dates in YYYY-MM-DD format with flexible separators.
     * <p>
     * This pattern captures dates from the years 1900-2099 with month and day values, using consistent
     * separators (hyphen, space, forward slash, or period). It validates basic date format structure
     * but does not perform full date validation (e.g., February 30th would match the pattern).
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (19|20)} — captures years starting with 19 or 20 (1900-2099)</li>
     *   <li>{@code \\d\\d} — followed by two more digits to complete the year</li>
     *   <li>{@code ([- /.])} — capturing group for separator: hyphen, space, forward slash, or period</li>
     *   <li>{@code (0[1-9]|1[012])} — month: 01-09 or 10-12</li>
     *   <li>{@code \\2} — backreference ensuring the same separator is used</li>
     *   <li>{@code (0[1-9]|[12][0-9]|3[01])} — day: 01-09, 10-29, or 30-31</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "2023-12-25"}</li>
     *   <li>{@code "1999/01/01"}</li>
     *   <li>{@code "2000.02.29"}</li>
     *   <li>{@code "1995 06 15"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "1899-12-25"} (year before 1900)</li>
     *   <li>{@code "2023-13-01"} (invalid month)</li>
     *   <li>{@code "2023-12-32"} (invalid day)</li>
     *   <li>{@code "2023/12-25"} (inconsistent separators)</li>
     * </ul>
     *
     * <p><b>Note:</b> This pattern performs basic format validation but does not validate actual
     * date validity (leap years, month-specific day limits). For complete date validation,
     * use this pattern in conjunction with proper date parsing libraries.</p>
     *
     * @see #DATE_MATCHER
     * @see #DATE_TIME_FINDER
     * @see #TIME_FINDER
     * @see java.time.LocalDate
     * @see java.util.regex.Pattern
     */
    public static final Pattern DATE_FINDER = Pattern.compile("(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])");

    /**
     * A regular expression {@link Pattern} that matches time in HH:MM:SS format (24-hour format).
     * <p>
     * This pattern captures time values in 24-hour format with hours, minutes, and seconds separated by colons.
     * It validates that hours are in the range 00-23, and minutes and seconds are in the range 00-59.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code ([01]\\d|2[0-3])} — capturing group for hours: 00-19 or 20-23</li>
     *   <li>{@code :} — literal colon separator</li>
     *   <li>{@code ([0-5]\\d)} — capturing group for minutes: 00-59</li>
     *   <li>{@code :} — literal colon separator</li>
     *   <li>{@code ([0-5]\\d)} — capturing group for seconds: 00-59</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "00:00:00"}</li>
     *   <li>{@code "12:30:45"}</li>
     *   <li>{@code "23:59:59"}</li>
     *   <li>{@code "09:15:30"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "24:00:00"} (invalid hour)</li>
     *   <li>{@code "12:60:30"} (invalid minute)</li>
     *   <li>{@code "12:30:60"} (invalid second)</li>
     *   <li>{@code "9:15:30"} (missing leading zero)</li>
     * </ul>
     *
     * <p><b>Note:</b> This pattern requires leading zeros for single-digit values and enforces
     * strict 24-hour format validation. For matching times within larger text, this pattern
     * can be used with find operations.</p>
     *
     * @see #TIME_MATCHER
     * @see #DATE_TIME_FINDER
     * @see #DATE_FINDER
     * @see java.time.LocalTime
     * @see java.util.regex.Pattern
     */
    public static final Pattern TIME_FINDER = Pattern.compile("([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)");

    /**
     * A regular expression {@link Pattern} that matches date and time in YYYY-MM-DD HH:MM:SS format.
     * <p>
     * This pattern captures combined date and time values with the date in YYYY-MM-DD format using flexible
     * separators (hyphen, space, forward slash, or period) followed by a space and time in 24-hour HH:MM:SS format.
     * It validates years from 1900-2099, months 01-12, days 01-31, hours 00-23, and minutes/seconds 00-59.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (19|20)} — capturing group for century: 19 or 20 (years 1900-2099)</li>
     *   <li>{@code \\d\\d} — two more digits to complete the year</li>
     *   <li>{@code ([- /.])} — capturing group for date separator: hyphen, space, forward slash, or period</li>
     *   <li>{@code (0[1-9]|1[012])} — capturing group for month: 01-09 or 10-12</li>
     *   <li>{@code \\2} — backreference ensuring the same date separator is used</li>
     *   <li>{@code (0[1-9]|[12][0-9]|3[01])} — capturing group for day: 01-09, 10-29, or 30-31</li>
     *   <li>{@code } — literal space separating date and time</li>
     *   <li>{@code ([01]\\d|2[0-3])} — capturing group for hours: 00-19 or 20-23</li>
     *   <li>{@code :} — literal colon separator</li>
     *   <li>{@code ([0-5]\\d)} — capturing group for minutes: 00-59</li>
     *   <li>{@code :} — literal colon separator</li>
     *   <li>{@code ([0-5]\\d)} — capturing group for seconds: 00-59</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "2023-12-25 14:30:45"}</li>
     *   <li>{@code "1999/01/01 00:00:00"}</li>
     *   <li>{@code "2000.02.29 23:59:59"}</li>
     *   <li>{@code "1995 06 15 09:15:30"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "1899-12-25 12:00:00"} (year before 1900)</li>
     *   <li>{@code "2023-13-01 12:00:00"} (invalid month)</li>
     *   <li>{@code "2023-12-32 12:00:00"} (invalid day)</li>
     *   <li>{@code "2023/12-25 12:00:00"} (inconsistent date separators)</li>
     *   <li>{@code "2023-12-25 24:00:00"} (invalid hour)</li>
     *   <li>{@code "2023-12-25  12:00:00"} (multiple spaces between date and time)</li>
     * </ul>
     *
     * <p><b>Note:</b> This pattern performs basic format validation but does not validate actual
     * date-time validity (leap years, month-specific day limits). The date and time portions must
     * be separated by exactly one space. For complete validation, use this pattern in conjunction
     * with proper date-time parsing libraries.</p>
     *
     * @see #DATE_TIME_MATCHER
     * @see #DATE_FINDER
     * @see #TIME_FINDER
     * @see java.time.LocalDateTime
     * @see java.util.regex.Pattern
     */
    public static final Pattern DATE_TIME_FINDER = Pattern
            .compile("(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01]) ([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)");

    /**
     * A regular expression {@link Pattern} that matches bank card numbers.
     * <p>
     * This pattern matches sequences that look like credit/debit card numbers,
     * typically consisting of 16 digits optionally separated by spaces or hyphens
     * in groups of 4 digits (e.g., 1234-5678-9012-3456).
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (?:\\d{4}[-\\s]?){3}} — three groups of 4 digits, each optionally followed by a hyphen or space</li>
     *   <li>{@code \\d{4}} — final group of 4 digits</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "1234 5678 9012 3456"}</li>
     *   <li>{@code "1234-5678-9012-3456"}</li>
     *   <li>{@code "1234567890123456"}</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This pattern does not validate that the card number is legitimate,
     * it only checks the format.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern BANK_CARD_NUMBER_FINDER = Pattern.compile("(?:\\d{4}[-\\s]?){3}\\d{4}");

    /**
     * A regular expression {@link Pattern} that matches email addresses according to RFC 5322 specification.
     * <p>
     * This is a comprehensive pattern that validates email addresses according to the official RFC 5322 standard.
     * It handles various valid email formats including quoted local parts, IP address domains, and special characters.
     * </p>
     *
     * <p>The pattern validates:</p>
     * <ul>
     *   <li>Local part: alphanumeric characters and special characters {@code !#$%&'*+/=?^_`{|}~-}</li>
     *   <li>Domain part: standard domain names or IP addresses in brackets</li>
     *   <li>Quoted strings in the local part with escaped characters</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "user@example.com"}</li>
     *   <li>{@code "john.doe@company.co.uk"}</li>
     *   <li>{@code "user+tag@example.com"}</li>
     *   <li>{@code "\"quoted.user\"@example.com"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     * @see <a href="https://www.baeldung.com/java-email-validation-regex">Baeldung Email Validation</a>
     * @see <a href="https://owasp.org/www-community/OWASP_Validation_Regex_Repository">OWASP Validation Regex Repository</a>
     * @see <a href="https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression">Stack Overflow Email Validation</a>
     */
    public static final Pattern EMAIL_ADDRESS_RFC_5322_FINDER = Pattern.compile(
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    /**
     * A regular expression {@link Pattern} that matches complete URLs.
     * <p>
     * This pattern validates URLs that must match from start to end of the string.
     * It supports HTTP, HTTPS, FTP, and FILE protocols and ensures the URL
     * contains a valid host and optional path components.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code ^} — start of string anchor</li>
     *   <li>{@code (https?|ftp|file)} — protocol: http, https, ftp, or file</li>
     *   <li>{@code ://} — protocol separator</li>
     *   <li>{@code [^\\s/$.?#]} — first character of host (not whitespace, slash, dollar, dot, question mark, or hash)</li>
     *   <li>{@code .} — any character (typically part of the host)</li>
     *   <li>{@code [^\\s]*} — zero or more non-whitespace characters (rest of URL)</li>
     *   <li>{@code $} — end of string anchor</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "http://example.com"}</li>
     *   <li>{@code "https://www.example.com/path/to/resource"}</li>
     *   <li>{@code "ftp://ftp.example.com/file.txt"}</li>
     *   <li>{@code "file://localhost/path/to/file"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "http://.example.com"} — starts with dot after protocol</li>
     *   <li>{@code "http://example .com"} — contains whitespace</li>
     *   <li>{@code "example.com"} — missing protocol</li>
     *   <li>{@code "Visit http://example.com today"} — doesn't match entire string</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This pattern requires the entire input string to be a valid URL
     * due to the start {@code ^} and end {@code $} anchors. For finding URLs within text,
     * use {@link #HTTP_URL_FINDER} or create a pattern without anchors.</p>
     *
     * @see java.util.regex.Pattern
     * @see #HTTP_URL_FINDER
     * @see #URL_MATCHER
     */
    public static final Pattern URL_FINDER = Pattern.compile("^(https?|ftp|file)://[^\\s/$.?#].[^\\s]*$");

    /**
     * A regular expression {@link Pattern} that matches HTTP and HTTPS URLs.
     * <p>
     * This pattern specifically matches web URLs using HTTP or HTTPS protocols.
     * It supports hosts, ports, paths, query strings, and fragments.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code https?} — http or https scheme</li>
     *   <li>{@code :\\/\\/} — separator</li>
     *   <li>{@code (?:[-\\w.])+} — host/domain with word characters, hyphens, and dots</li>
     *   <li>{@code (?:\\:[0-9]+)?} — optional port number</li>
     *   <li>{@code (?:\\/(?:[\\w\\/_.])*} — optional path</li>
     *   <li>{@code (?:\\?(?:[\\w&=%.])*)?} — optional query string</li>
     *   <li>{@code (?:\\#(?:[\\w.])*)?)?} — optional fragment/anchor</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "http://www.example.com"}</li>
     *   <li>{@code "https://api.example.com:8443/v1/users?id=123&name=test"}</li>
     *   <li>{@code "http://localhost:3000/path/to/resource#section"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern HTTP_URL_FINDER = Pattern
            .compile("https?:\\/\\/(?:[-\\w.])+(?:\\:[0-9]+)?(?:\\/(?:[\\w\\/_.])*(?:\\?(?:[\\w&=%.])*)?(?:\\#(?:[\\w.])*)?)?");

    /**
     * A regular expression {@link Pattern} that matches alphanumeric strings without spaces.
     * <p>
     * This pattern matches sequences of letters (a-z, A-Z) and digits (0-9) with no spaces or other characters.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [a-zA-Z0-9]+} — one or more alphanumeric characters</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "ABC123"}</li>
     *   <li>{@code "test123"}</li>
     *   <li>{@code "HelloWorld"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern ALPHANUMERIC_FINDER = Pattern.compile("[a-zA-Z0-9]+");

    /**
     * A regular expression {@link Pattern} that matches alphanumeric strings with spaces.
     * <p>
     * This pattern matches sequences of letters (a-z, A-Z), digits (0-9), and whitespace characters.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [a-zA-Z0-9\\s]+} — one or more alphanumeric characters or whitespace</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "ABC 123"}</li>
     *   <li>{@code "Hello World 123"}</li>
     *   <li>{@code "test 456"}</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern ALPHANUMERIC_SPACE_FINDER = Pattern.compile("[a-zA-Z0-9\\s]+");

    /**
     * A regular expression {@link Pattern} that matches duplicate words within a string.
     * <p>
     * This pattern uses a word boundary and backreference to find words that appear
     * more than once in the text. It matches each occurrence of a word that has a duplicate elsewhere.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (\\b\\w+\\b)} — captures a complete word</li>
     *   <li>{@code (?=.*\\b\\1\\b)} — lookahead to check if the same word appears again later</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String text = "the quick brown fox jumps over the lazy dog";
     * Matcher matcher = RegExUtil.DUPLICATES_FINDER.matcher(text);
     * // Will match "the" (appears twice)
     * }</pre>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern DUPLICATES_FINDER = Pattern.compile("(\\b\\w+\\b)(?=.*\\b\\1\\b)");

    /**
     * A regular expression {@link Pattern} that matches whitespace sequences.
     * <p>
     * This pattern matches one or more consecutive whitespace characters including
     * spaces, tabs, line breaks, etc.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\s+} — one or more whitespace characters</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code " "} (single space)</li>
     *   <li>{@code "   "} (multiple spaces)</li>
     *   <li>{@code "\t"} (tab)</li>
     *   <li>{@code "\n"} (newline)</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern WHITESPACE_FINDER = Pattern.compile("\\s+");

    /**
     * Pattern that matches an entire string if it is a valid Java identifier.
     * This is the anchored version of {@link #JAVA_IDENTIFIER_FINDER} that requires the entire string to match.
     *
     * @see #JAVA_IDENTIFIER_FINDER
     * @see <a href="https://stackoverflow.com/questions/1449817/what-are-some-of-the-most-useful-regular-expressions-for-programmers">Stack Overflow Useful Regex</a>
     */
    public static final Pattern JAVA_IDENTIFIER_MATCHER = Pattern.compile("^" + JAVA_IDENTIFIER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a signed or unsigned integer.
     * This is the anchored version of {@link #INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #INTEGER_FINDER
     */
    public static final Pattern INTEGER_MATCHER = Pattern.compile("^" + INTEGER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a positive (unsigned) integer.
     * This is the anchored version of {@link #POSITIVE_INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #POSITIVE_INTEGER_FINDER
     */
    public static final Pattern POSITIVE_INTEGER_MATCHER = Pattern.compile("^" + POSITIVE_INTEGER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a negative integer.
     * This is the anchored version of {@link #NEGATIVE_INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #NEGATIVE_INTEGER_FINDER
     */
    public static final Pattern NEGATIVE_INTEGER_MATCHER = Pattern.compile("^" + NEGATIVE_INTEGER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a signed or unsigned number (integer or decimal).
     * This is the anchored version of {@link #NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #NUMBER_FINDER
     */
    public static final Pattern NUMBER_MATCHER = Pattern.compile("^" + NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a positive number (integer or decimal).
     * This is the anchored version of {@link #POSITIVE_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #POSITIVE_NUMBER_FINDER
     */
    public static final Pattern POSITIVE_NUMBER_MATCHER = Pattern.compile("^" + POSITIVE_NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a negative number (integer or decimal).
     * This is the anchored version of {@link #NEGATIVE_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #NEGATIVE_NUMBER_FINDER
     */
    public static final Pattern NEGATIVE_NUMBER_MATCHER = Pattern.compile("^" + NEGATIVE_NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a number in standard or scientific notation.
     * This is the anchored version of {@link #SCIENTIFIC_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #SCIENTIFIC_NUMBER_FINDER
     */
    public static final Pattern SCIENTIFIC_NUMBER_MATCHER = Pattern.compile("^" + SCIENTIFIC_NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a phone number.
     * This is the anchored version of {@link #PHONE_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #PHONE_NUMBER_FINDER
     */
    public static final Pattern PHONE_NUMBER_MATCHER = Pattern.compile("^" + PHONE_NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a phone number with country code.
     * This is the anchored version of {@link #PHONE_NUMBER_WITH_CODE_FINDER} that requires the entire string to match.
     *
     * @see #PHONE_NUMBER_WITH_CODE_FINDER
     */
    public static final Pattern PHONE_NUMBER_WITH_CODE_MATCHER = Pattern.compile("^" + PHONE_NUMBER_WITH_CODE_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a date in yyyy-MM-dd format.
     * This is the anchored version of {@link #DATE_FINDER} that requires the entire string to match.
     *
     * @see #DATE_FINDER
     */
    public static final Pattern DATE_MATCHER = Pattern.compile("^" + DATE_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a time in HH:mm:ss format.
     * This is the anchored version of {@link #TIME_FINDER} that requires the entire string to match.
     *
     * @see #TIME_FINDER
     */
    public static final Pattern TIME_MATCHER = Pattern.compile("^" + TIME_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a date-time in yyyy-MM-dd HH:mm:ss format.
     * This is the anchored version of {@link #DATE_TIME_FINDER} that requires the entire string to match.
     *
     * @see #DATE_TIME_FINDER
     */
    public static final Pattern DATE_TIME_MATCHER = Pattern.compile("^" + DATE_TIME_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a bank card number.
     * This is the anchored version of {@link #BANK_CARD_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #BANK_CARD_NUMBER_FINDER
     */
    public static final Pattern BANK_CARD_NUMBER_MATCHER = Pattern.compile("^" + BANK_CARD_NUMBER_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a valid email address according to RFC 5322.
     * This is the anchored version of {@link #EMAIL_ADDRESS_RFC_5322_FINDER} that requires the entire string to match.
     *
     * @see #EMAIL_ADDRESS_RFC_5322_FINDER
     */
    public static final Pattern EMAIL_ADDRESS_RFC_5322_MATCHER = Pattern.compile("^" + EMAIL_ADDRESS_RFC_5322_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is a URL.
     * This is the anchored version of {@link #URL_FINDER} that requires the entire string to match.
     *
     * @see #URL_FINDER
     */
    public static final Pattern URL_MATCHER = Pattern.compile("^" + URL_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it is an HTTP or HTTPS URL.
     * This is the anchored version of {@link #HTTP_URL_FINDER} that requires the entire string to match.
     *
     * @see #HTTP_URL_FINDER
     */
    public static final Pattern HTTP_URL_MATCHER = Pattern.compile("^" + HTTP_URL_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it consists only of alphanumeric characters (no spaces).
     * This is the anchored version of {@link #ALPHANUMERIC_FINDER} that requires the entire string to match.
     *
     * @see #ALPHANUMERIC_FINDER
     */
    public static final Pattern ALPHANUMERIC_MATCHER = Pattern.compile("^" + ALPHANUMERIC_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it consists only of alphanumeric characters and spaces.
     * This is the anchored version of {@link #ALPHANUMERIC_SPACE_FINDER} that requires the entire string to match.
     *
     * @see #ALPHANUMERIC_SPACE_FINDER
     */
    public static final Pattern ALPHANUMERIC_SPACE_MATCHER = Pattern.compile("^" + ALPHANUMERIC_SPACE_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it contains duplicate words.
     * This is the anchored version of {@link #DUPLICATES_FINDER} that requires the entire string to match.
     *
     * @see #DUPLICATES_FINDER
     */
    public static final Pattern DUPLICATES_MATCHER = Pattern.compile("^" + DUPLICATES_FINDER.pattern() + "$");

    /**
     * Pattern that matches an entire string if it consists only of whitespace.
     * This is the anchored version of {@link #WHITESPACE_FINDER} that requires the entire string to match.
     *
     * @see #WHITESPACE_FINDER
     */
    public static final Pattern WHITESPACE_MATCHER = Pattern.compile("^" + WHITESPACE_FINDER.pattern() + "$");

    /**
     * A regular expression {@link Pattern} that matches any line separator sequence.
     * <p>
     * This pattern uses {@code \R} which is a Unicode linebreak matcher that handles
     * all common line break sequences across different operating systems and Unicode standards.
     * </p>
     *
     * <p>Matches:</p>
     * <ul>
     *   <li>{@code \n} — Line Feed (LF, Unix/Linux/Mac)</li>
     *   <li>{@code \r\n} — Carriage Return + Line Feed (CRLF, Windows)</li>
     *   <li>{@code \r} — Carriage Return (CR, old Mac)</li>
     *   <li>Other Unicode line terminators</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] lines = RegExUtil.LINE_SEPARATOR.split("line1\nline2\r\nline3");
     * // Returns: ["line1", "line2", "line3"]
     * }</pre>
     *
     * @see java.util.regex.Pattern
     * @see #splitToLines(String)
     */
    public static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

    static final Pattern CAMEL_CASE_SEPARATOR = Pattern.compile("[_\\-\\s]");

    private RegExUtil() {
        // Singleton for utility class.
    }

    /**
     * Checks whether the given regular expression pattern can be found anywhere in the source string.
     * <p>
     * This method searches the entire source string for at least one occurrence of the specified
     * regular expression pattern. It returns {@code true} if the pattern is found, {@code false} otherwise.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * boolean result = RegExUtil.find("Hello World 123", "\\d+");
     * // Returns: true (digits found)
     *
     * boolean hasEmail = RegExUtil.find("Contact: user@example.com", "\\w+@\\w+\\.\\w+");
     * // Returns: true (email pattern found)
     *
     * boolean noMatch = RegExUtil.find("abc def", "\\d+");
     * // Returns: false (no digits found)
     *
     * boolean emptySource = RegExUtil.find("", "test");
     * // Returns: false (empty string has no content)
     *
     * boolean nullSource = RegExUtil.find(null, "\\w+");
     * // Returns: false (null treated as empty string)
     * }</pre>
     *
     * <p><b>Performance Note:</b> If you need to use the same regex pattern multiple times,
     * consider pre-compiling it with {@link Pattern#compile(String)} and using
     * {@link #find(String, Pattern)} to avoid recompilation overhead.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param regex the regular expression string to search for; must not be {@code null} or empty
     * @return {@code true} if the pattern is found in the source, {@code false} otherwise
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty
     * @see #find(String, Pattern)
     * @see #matches(String, String)
     * @see #findFirst(String, String)
     * @see #countMatches(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     */
    public static boolean find(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return Pattern.compile(regex).matcher(checkSourceString(source)).find();
    }

    private static String checkSourceString(String source) {
        return Strings.nullToEmpty(source);
    }

    /**
     * Checks whether the given compiled {@link Pattern} can be found anywhere in the source string.
     * <p>
     * This method searches the entire source string for at least one occurrence of the specified
     * pattern. It returns {@code true} if the pattern is found, {@code false} otherwise.
     * This is more efficient than {@link #find(String, String)} when the same pattern is used
     * multiple times, as it avoids recompiling the pattern on each invocation.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * boolean result = RegExUtil.find("Hello World 123", digitPattern);
     * // Returns: true (digits found)
     *
     * Pattern emailPattern = Pattern.compile("\\w+@\\w+\\.\\w+");
     * boolean hasEmail = RegExUtil.find("Contact: user@example.com", emailPattern);
     * // Returns: true (email pattern found)
     *
     * Pattern numberPattern = Pattern.compile("\\d+");
     * boolean noMatch = RegExUtil.find("abc def", numberPattern);
     * // Returns: false (no digits found)
     *
     * boolean emptySource = RegExUtil.find("", digitPattern);
     * // Returns: false (empty string has no content)
     *
     * boolean nullSource = RegExUtil.find(null, digitPattern);
     * // Returns: false (null treated as empty string)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is preferred over {@link #find(String, String)}
     * when performing multiple searches with the same pattern, as pattern compilation is
     * an expensive operation.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param pattern the compiled regex pattern to search for; must not be {@code null}
     * @return {@code true} if the pattern is found in the source, {@code false} otherwise
     * @throws IllegalArgumentException if {@code pattern} is {@code null}
     * @see #find(String, String)
     * @see #matches(String, Pattern)
     * @see #findFirst(String, Pattern)
     * @see #countMatches(String, Pattern)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     */
    public static boolean find(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        return pattern.matcher(checkSourceString(source)).find();
    }

    /**
     * Checks whether the entire source string matches the given regular expression pattern.
     * <p>
     * This method attempts to match the entire source string against the specified regex pattern.
     * Unlike {@link #find(String, String)}, which searches for the pattern anywhere in the string,
     * this method requires the entire string to match the pattern from beginning to end.
     * It returns {@code true} only if the whole string matches, {@code false} otherwise.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * boolean result = RegExUtil.matches("12345", "\\d+");
     * // Returns: true (entire string is digits)
     *
     * boolean partial = RegExUtil.matches("abc123def", "\\d+");
     * // Returns: false (contains digits but also letters)
     *
     * boolean emailMatch = RegExUtil.matches("user@example.com", "\\w+@\\w+\\.\\w+");
     * // Returns: true (entire string is an email)
     *
     * boolean noMatch = RegExUtil.matches("Hello World", "^Hello$");
     * // Returns: false (string contains more than "Hello")
     *
     * boolean emptyMatch = RegExUtil.matches("", ".*");
     * // Returns: true (empty string matches .*)
     *
     * boolean nullMatch = RegExUtil.matches(null, ".*");
     * // Returns: true (null treated as empty string, matches .*)
     * }</pre>
     *
     * <p><b>Note:</b> This method implicitly adds anchors (^ and $) to the pattern, so you don't
     * need to include them in your regex. If you want to find a pattern anywhere in the string,
     * use {@link #find(String, String)} instead.</p>
     *
     * @param source the input text to match; may be {@code null} (treated as empty string)
     * @param regex the regular expression string to match against; must not be {@code null} or empty
     * @return {@code true} if the entire source string matches the pattern, {@code false} otherwise
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty
     * @see #matches(String, Pattern)
     * @see #find(String, String)
     * @see Pattern#matches(String, CharSequence)
     * @see Matcher#matches()
     */
    public static boolean matches(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return Pattern.matches(regex, checkSourceString(source));
    }

    /**
     * Checks whether the entire source string matches the given compiled {@link Pattern}.
     * <p>
     * This method attempts to match the entire source string against the specified pattern.
     * Unlike {@link #find(String, Pattern)}, which searches for the pattern anywhere in the string,
     * this method requires the entire string to match the pattern from beginning to end.
     * It returns {@code true} only if the whole string matches, {@code false} otherwise.
     * This is more efficient than {@link #matches(String, String)} when the same pattern is used
     * multiple times.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * boolean result = RegExUtil.matches("12345", digitPattern);
     * // Returns: true (entire string is digits)
     *
     * boolean partial = RegExUtil.matches("abc123def", digitPattern);
     * // Returns: false (contains digits but also letters)
     *
     * Pattern emailPattern = Pattern.compile("\\w+@\\w+\\.\\w+");
     * boolean emailMatch = RegExUtil.matches("user@example.com", emailPattern);
     * // Returns: true (entire string is an email)
     *
     * Pattern helloPattern = Pattern.compile("^Hello$");
     * boolean noMatch = RegExUtil.matches("Hello World", helloPattern);
     * // Returns: false (string contains more than "Hello")
     *
     * Pattern anyPattern = Pattern.compile(".*");
     * boolean emptyMatch = RegExUtil.matches("", anyPattern);
     * // Returns: true (empty string matches .*)
     *
     * boolean nullMatch = RegExUtil.matches(null, anyPattern);
     * // Returns: true (null treated as empty string, matches .*)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is preferred over {@link #matches(String, String)}
     * when performing multiple matches with the same pattern, as pattern compilation is
     * an expensive operation.</p>
     *
     * @param source the input text to match; may be {@code null} (treated as empty string)
     * @param pattern the compiled regex pattern to match against; must not be {@code null}
     * @return {@code true} if the entire source string matches the pattern, {@code false} otherwise
     * @throws IllegalArgumentException if {@code pattern} is {@code null}
     * @see #matches(String, String)
     * @see #find(String, Pattern)
     * @see Matcher#matches()
     * @see Pattern#compile(String)
     */
    public static boolean matches(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        return pattern.matcher(checkSourceString(source)).matches();
    }

    /**
     * Finds the first match of the given regular expression in the specified input text.
     * <p>
     * This is a convenience method that compiles the regex string into a {@link Pattern}
     * and then searches for the first occurrence. If a match is found, the matched substring
     * is returned. If no match is found, {@code null} is returned.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.findFirst("abc123xyz456", "\\d+");
     * // Returns: "123"
     *
     * String email = RegExUtil.findFirst("Contact: john@example.com or jane@test.org", 
     *                                   "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
     * // Returns: "john@example.com"
     *
     * String noResult = RegExUtil.findFirst("abc", "\\d+");
     * // Returns: null (pattern not found)
     *
     * String word = RegExUtil.findFirst("", "\\b\\w+\\b");
     * // Returns: null (empty string has no matches)
     *
     * String nullSource = RegExUtil.findFirst(null, "\\d+");
     * // Returns: null (null source treated as empty)
     * }</pre>
     *
     * <p><b>Performance Note:</b> If you need to use the same regex pattern multiple times,
     * consider using {@link #findFirst(String, Pattern)} instead to avoid recompiling
     * the pattern on each call.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param regex the regular expression string to match; must not be {@code null} or empty
     * @return the first matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty
     * @see #findFirst(String, Pattern)
     * @see #findLast(String, String)
     * @see #find(String, String)
     * @see #matchResults(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     * @see Matcher#group()
     */
    public static String findFirst(final String source, final String regex) {
        N.checkArgNotEmpty(regex, cs.regex);

        return findFirst(source, Pattern.compile(regex));
    }

    /**
     * Finds the first match of the given {@link Pattern} in the specified input text.
     * <p>
     * This method searches through the input string to find the first occurrence that matches
     * the provided regular expression pattern. If a match is found, the matched substring is
     * returned. If no match is found, {@code null} is returned.
     * </p> 
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * String result = RegExUtil.findFirst("abc123xyz456", digitPattern);
     * // Returns: "123"
     *
     * Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
     * String email = RegExUtil.findFirst("Contact: john@example.com or jane@test.org", emailPattern);
     * // Returns: "john@example.com"
     *
     * Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
     * String word = RegExUtil.findFirst("", wordPattern);
     * // Returns: null (empty string has no matches)
     *
     * Pattern noMatch = Pattern.compile("xyz");
     * String noResult = RegExUtil.findFirst("abc123", noMatch);
     * // Returns: null (pattern not found)
     * String nullSource = RegExUtil.findFirst(null, digitPattern);
     * // Returns: null (null source treated as empty)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is more efficient than {@link #find(String, String)}
     * when you need both to check for a match and retrieve the matched text, as it avoids
     * recompiling the pattern and performs the search in a single operation.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param pattern the compiled regex pattern to match; must not be {@code null}
     * @return the first matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code pattern} is {@code null}
     * @see #findLast(String, Pattern)
     * @see #find(String, Pattern)
     * @see #matchResults(String, Pattern)
     * @see Matcher#find()
     * @see Matcher#group()
     * @see Pattern#compile(String)
     */
    public static String findFirst(final String source, final Pattern pattern) {
        N.checkArgNotNull(pattern, cs.pattern);

        Matcher matcher = pattern.matcher(checkSourceString(source));
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Finds the last match of the given regular expression in the specified input text.
     * <p>
     * This is a convenience method that compiles the regex string into a {@link Pattern}
     * and then searches for the last occurrence. It iterates through all matches and returns
     * the final one found. If no match is found, {@code null} is returned.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.findLast("abc123xyz456pqr789", "\\d+");
     * // Returns: "789"
     *
     * String word = RegExUtil.findLast("hello world java", "\\b\\w+\\b");
     * // Returns: "java"
     *
     * String email = RegExUtil.findLast("Contact: john@example.com or jane@test.org",
     *                                  "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
     * // Returns: "jane@test.org"
     *
     * String noResult = RegExUtil.findLast("abc", "\\d+");
     * // Returns: null (pattern not found)
     *
     * String nullSource = RegExUtil.findLast(null, "\\d+");
     * // Returns: null (null source treated as empty)
     *
     * String empty = RegExUtil.findLast("", "\\w+");
     * // Returns: null (empty string has no matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> If you need to use the same regex pattern multiple times,
     * consider using {@link #findLast(String, Pattern)} instead to avoid recompiling
     * the pattern on each call. This method iterates through all matches to find the last one,
     * so it may be less efficient than {@link #findFirst(String, String)} for very long strings.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param regex the regular expression string to match; must not be {@code null} or empty
     * @return the last matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty
     * @see #findLast(String, Pattern)
     * @see #findFirst(String, String)
     * @see #find(String, String)
     * @see #matchResults(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     * @see Matcher#group()
     */
    public static String findLast(final String source, final String regex) {
        N.checkArgNotEmpty(regex, cs.regex);

        return findLast(source, Pattern.compile(regex));
    }

    /**
     * Finds the last match of the given {@link Pattern} in the specified input text.
     * <p>
     * This method searches through the input string to find the last occurrence that matches
     * the provided regular expression pattern. It iterates through all matches and returns
     * the final one found. If no match is found, {@code null} is returned.
     * </p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * String result = RegExUtil.findLast("abc123xyz456pqr789", digitPattern);
     * // Returns: "789"
     *
     * Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
     * String word = RegExUtil.findLast("hello world java", wordPattern);
     * // Returns: "java"
     *
     * Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
     * String email = RegExUtil.findLast("Contact: john@example.com or jane@test.org", emailPattern);
     * // Returns: "jane@test.org"
     *
     * Pattern noMatch = Pattern.compile("xyz");
     * String noResult = RegExUtil.findLast("abc123", noMatch);
     * // Returns: null (pattern not found)
     *
     * String nullSource = RegExUtil.findLast(null, digitPattern);
     * // Returns: null (null source treated as empty)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method iterates through all matches in the string
     * to find the last one, so it may be less efficient than {@link #findFirst(String, Pattern)}
     * for very long strings with many matches. For better performance with large texts,
     * consider using alternative approaches if you only need to check for existence.</p>
     *
     * @param source the input text to search; may be {@code null} (treated as empty string)
     * @param pattern the compiled regex pattern to match; must not be {@code null}
     * @return the last matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code pattern} is {@code null}
     * @see #findFirst(String, Pattern)
     * @see #find(String, Pattern)
     * @see #matchResults(String, Pattern)
     * @see Matcher#find()
     * @see Matcher#group()
     * @see Pattern#compile(String)
     */
    public static String findLast(final String source, final Pattern pattern) {
        N.checkArgNotNull(pattern, cs.pattern);

        Matcher matcher = pattern.matcher(checkSourceString(source));
        String lastMatch = null;

        while (matcher.find()) {
            lastMatch = matcher.group();
        }

        return lastMatch;
    }

    /**
     * Removes the first substring of the source string that matches the given regular expression.
     * This is equivalent to {@code replaceFirst(source, regex, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeFirst("Hello123World456", "\\d+");
     * // Returns: "HelloWorld456"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the first replacement processed,  or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see #replaceFirst(String, String, String)
     * @see String#replaceFirst(String, String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String source, final String regex) throws IllegalArgumentException {
        return replaceFirst(source, regex, Strings.EMPTY);
    }

    /**
     * Removes the first substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceFirst(source, pattern, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeFirst("Hello   World   !", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "HelloWorld   !"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceFirst(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceFirst(String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceFirst(source, pattern, Strings.EMPTY);
    }

    /**
     * Removes the last substring of the source string that matches the given regular expression.
     * This is equivalent to {@code replaceLast(source, regex, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeLast("Hello123World456", "\\d+");
     * // Returns: "Hello123World"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the last replacement processed,  or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see #replaceFirst(String, String, String)
     * @see java.util.regex.Pattern
     */
    @Beta
    public static String removeLast(final String source, final String regex) throws IllegalArgumentException {
        return replaceLast(source, regex, Strings.EMPTY);
    }

    /**
     * Removes the last substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceLast(source, pattern, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeLast("Hello   World   !", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "Hello   World!"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceLast(String, Pattern, String)
     * @see java.util.regex.Pattern
     */
    @Beta
    public static String removeLast(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceLast(source, pattern, Strings.EMPTY);
    }

    /**
     * Removes each substring of the source string that matches the given regular expression.
     * This is equivalent to {@code replaceAll(source, regex, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeAll("Hello123World456", "\\d+");
     * // Returns: "HelloWorld"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with any removes processed,  or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see #replaceAll(String, String, String)
     * @see String#replaceAll(String, String)
     */
    public static String removeAll(final String source, final String regex) throws IllegalArgumentException {
        return replaceAll(source, regex, Strings.EMPTY);
    }

    /**
     * Removes each substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceAll(source, pattern, "")}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.removeAll("Hello   World", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "HelloWorld"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @return the source string with any removes processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #replaceAll(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceAll(String)
     */
    public static String removeAll(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceAll(source, pattern, Strings.EMPTY);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression with the given replacement.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("Hello123World456", "\\d+", "XXX");
     * // Returns: "HelloXXXWorld456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final String replacement) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return replaceFirst(source, Pattern.compile(regex), Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression
     * with the result of applying the given function to the matched substring.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("hello world", "\\b\\w", match -> match.toUpperCase());
     * // Returns: "Hello world"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression
     * with the result of applying the given function to the start and end indices of the match.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern with the given replacement.
     * This method is more efficient than {@link #replaceFirst(String, String, String)} when using the same pattern multiple times.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceFirst("Hello123World456", pattern, "XXX");
     * // Returns: "HelloXXXWorld456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceFirst(Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern with the given replacer.
     * This method is a {@code null} safe equivalent to:
     * {@code pattern.matcher(checkSourceString(source)).replaceFirst(replacer)}
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w");
     * String result = RegExUtil.replaceFirst("hello world", pattern, match -> match.toUpperCase());
     * // Returns: "Hello world"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceFirst(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern with the given replacer.
     * This method is a {@code null} safe equivalent to:
     * {@code pattern.matcher(checkSourceString(source)).replaceFirst(replacer)}
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceFirst("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceFirst(matcher -> replacer.apply(matcher.start(), matcher.end()));
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacement}.
     * This method finds the rightmost match in the string and replaces only that occurrence.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("Hello123World456", "\\d+", "XXX");
     * // Returns: "Hello123WorldXXX"
     * }</pre>
     *
     * @param source the source string to search in
     * @param regex the regular expression pattern to search for
     * @param replacement the replacement string
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see Matcher#replaceFirst(String)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final String replacement) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceLast(source, Pattern.compile(regex), replacement);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     * The replacer function receives the matched substring and returns the replacement string.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("hello world hello", "hello", match -> match.toUpperCase());
     * // Returns: "hello world HELLO"
     * }</pre>
     *
     * @param source the source string to search in
     * @param regex the regular expression pattern to search for
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     * The replacer function receives the start and end indices of the match and returns the replacement string.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc123def[9-12]"
     * }</pre>
     *
     * @param source the source string to search in
     * @param regex the regular expression pattern to search for
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacement}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceLast("Hello123World456", pattern, "XXX");
     * // Returns: "Hello123WorldXXX"
     * }</pre>
     *
     * @param source the source string to search in
     * @param pattern the pre-compiled regular expression pattern to search for
     * @param replacement the replacement string
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(String)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(checkSourceString(source));

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
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("hello");
     * String result = RegExUtil.replaceLast("hello world hello", pattern, match -> match.toUpperCase());
     * // Returns: "hello world HELLO"
     * }</pre>
     *
     * @param source the source string to search in
     * @param pattern the pre-compiled regular expression pattern to search for
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(checkSourceString(source));

        for (int start = -1, end = -1, i = source.length(); i >= 0; i--) {
            if (matcher.find(i)) {
                if (start < 0 || (matcher.start() < start && matcher.end() >= end)) {
                    start = matcher.start();
                    end = matcher.end();
                } else {
                    return Strings.replaceRange(source, start, end, replacer.apply(source.substring(start, end)));
                }
            } else if (start >= 0) {
                return Strings.replaceRange(source, start, end, replacer.apply(source.substring(start, end)));
            }
        }

        return source;
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replace it with the specified {@code replacer}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceLast("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc123def[9-12]"
     * }</pre>
     *
     * @param source the source string to search in
     * @param pattern the pre-compiled regular expression pattern to search for
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#replaceFirst(Function)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(checkSourceString(source));

        for (int start = -1, end = -1, i = source.length(); i >= 0; i--) {
            if (matcher.find(i)) {
                if (start < 0 || (matcher.start() < start && matcher.end() >= end)) {
                    start = matcher.start();
                    end = matcher.end();
                } else {
                    return Strings.replaceRange(source, start, end, replacer.apply(start, end));
                }
            } else if (start >= 0) {
                return Strings.replaceRange(source, start, end, replacer.apply(start, end));
            }
        }

        return source;
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.
     *
     * This method is a {@code null} safe equivalent to:
     * <ul>
     *  <li>{@code source.replaceAll(regex, replacement)}</li>
     *  <li>{@code Pattern.compile(regex).matcher(checkSourceString(source)).replaceAll(replacement)}</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("Hello   World", "\\s+", " ");
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for each match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final String replacement) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceAll(source, Pattern.compile(regex), Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the result of applying the given function to the matched substring.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("hello world", "\\b\\w", match -> match.toUpperCase());
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the result of applying the given function to the start and end indices of the match.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("abc123def", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern with the given replacement.
     * This method is more efficient than {@link #replaceAll(String, String, String)} when using the same pattern multiple times.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\s+");
     * String result = RegExUtil.replaceAll("Hello   World", pattern, " ");
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for each match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see java.util.regex.Matcher#replaceAll(String)
     * @see java.util.regex.Pattern
     */
    public static String replaceAll(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceAll(Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the matched substring.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w");
     * String result = RegExUtil.replaceAll("hello world", pattern, match -> match.toUpperCase());
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceAll(matcher -> replacer.apply(source.substring(matcher.start(), matcher.end())));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the start and end indices of the match.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceAll("abc123def", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression to which this string is to be matched
     * @param replacer The function to be applied to the match result of this matcher that returns a replacement string.
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Strings.EMPTY;
        }

        return pattern.matcher(checkSourceString(source)).replaceAll(matcher -> replacer.apply(matcher.start(), matcher.end()));
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int count = RegExUtil.countMatches("Hello World", "l");
     * // Returns: 3
     * 
     * int digitCount = RegExUtil.countMatches("abc123def456", "\\d+");
     * // Returns: 2 (two groups of digits)
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression pattern to be counted
     * @return the number of occurrences of the specified pattern in the string, or 0 if the input source string is {@code null} or empty
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see #countMatches(String, Pattern)
     */
    public static int countMatches(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (Strings.isEmpty(source)) {
            return 0;
        }

        return countMatches(source, Pattern.compile(regex));
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     * This method is more efficient than {@link #countMatches(String, String)} when using the same pattern multiple times.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * int count = RegExUtil.countMatches("abc123def456ghi789", pattern);
     * // Returns: 3
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return the number of occurrences of the specified pattern in the string, or 0 if the input source string is {@code null} or empty
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see #countMatches(String, String)
     */
    public static int countMatches(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return 0;
        }

        final Matcher matcher = pattern.matcher(checkSourceString(source));
        int occurrences = 0;

        while (matcher.find()) {
            occurrences++;
        }

        return occurrences;
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string.
     * Returns a stream of {@link MatchResult} objects representing each match found.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Stream<MatchResult> matches = RegExUtil.matchResults("abc123def456", "\\d+");
     * matches.forEach(match -> System.out.println(match.group())); 
     * // Prints: 123, 456
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression pattern to be counted
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (Strings.isEmpty(source)) {
            return Stream.empty();
        }

        return matchResults(source, Pattern.compile(regex));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string.
     * Returns a stream of {@link MatchResult} objects representing each match found.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w+@\\w+\\.\\w+\\b");
     * Stream<MatchResult> matches = RegExUtil.matchResults("Contact: john@example.com, jane@test.org", pattern);
     * matches.forEach(match -> System.out.println(match.group()));
     * // Prints: john@example.com, jane@test.org
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern.
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return Stream.empty();
        }

        return pattern.matcher(checkSourceString(source)).results();
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     * This is useful when you need to know the positions where matches occur in the string.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * IntStream indices = RegExUtil.matchIndices("Hello World", "l");
     * // Returns stream of: 2, 3, 9 (positions of 'l' in the string)
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression pattern to be counted
     * @return a stream of start indices for each subsequence of the input sequence that matches the pattern
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see Matcher#results()
     * @see Strings#indicesOf(String, String)
     * @see Strings#indicesOf(String, String, int)
     */
    public static IntStream matchIndices(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (Strings.isEmpty(source)) {
            return IntStream.empty();
        }

        return matchIndices(source, Pattern.compile(regex));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     * This is useful when you need to know the positions where matches occur in the string.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * IntStream indices = RegExUtil.matchIndices("abc123def456ghi", pattern);
     * // Returns stream of: 3, 9 (starting positions of number sequences)
     * }</pre>
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
        N.checkArgNotNull(pattern, cs.pattern);

        if (Strings.isEmpty(source)) {
            return IntStream.empty();
        }

        return pattern.matcher(checkSourceString(source)).results().mapToInt(MatchResult::start);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] parts = RegExUtil.split("one,two,three", ",");
     * // Returns: ["one", "two", "three"]
     * 
     * String[] words = RegExUtil.split("Hello   World", "\\s+");
     * // Returns: ["Hello", "World"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param regex the regular expression to split by
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#split(String)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final String regex) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return source.split(regex);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression with a limit.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * The limit parameter controls the number of times the pattern is applied and therefore affects the length of the resulting array.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String[] parts = RegExUtil.split("one,two,three,four", ",", 3);
     * // Returns: ["one", "two", "three,four"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param regex the regular expression to split by
     * @param limit the result threshold. A non-positive limit indicates no limit.
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty
     * @see String#split(String, int)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final String regex, final int limit) throws IllegalArgumentException {
        N.checkArgNotEmpty(regex, cs.regex);

        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return source.split(regex, limit);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression pattern.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * This method is more efficient than {@link #split(String, String)} when using the same pattern multiple times.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\s+");
     * String[] words = RegExUtil.split("Hello   World   Java", pattern);
     * // Returns: ["Hello", "World", "Java"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param pattern the regular expression pattern to split by
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#split(String)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return pattern.split(source);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression pattern with a limit.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * The limit parameter controls the number of times the pattern is applied.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile(",");
     * String[] parts = RegExUtil.split("a,b,c,d", pattern, 3);
     * // Returns: ["a", "b", "c,d"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param pattern the regular expression pattern to split by
     * @param limit the result threshold. A non-positive limit indicates no limit.
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the pattern is {@code null}
     * @see String#split(String, int)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final Pattern pattern, final int limit) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return pattern.split(source, limit);
    }

    /** 
     * Splits the given string into an array of lines, using the default line separator.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * This method handles all types of line breaks (CR, LF, CRLF, etc.) across different platforms.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\r\nLine 3";
     * String[] lines = RegExUtil.splitToLines(text);
     * // Returns: ["Line 1", "Line 2", "Line 3"]
     * }</pre>
     *
     * @param source the string to be split into lines, may be {@code null} or empty
     * @return an array of strings computed by splitting the source string into lines.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @see #splitToLines(String, int)
     * @see Pattern#split(CharSequence)
     */
    public static String[] splitToLines(final String source) {
        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return LINE_SEPARATOR.split(source);
    }

    /** 
     * Splits the given string into an array of lines, with a specified limit on the number of lines.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * The limit parameter controls how many lines are returned.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\nLine 3\nLine 4";
     * String[] lines = RegExUtil.splitToLines(text, 3);
     * // Returns: ["Line 1", "Line 2", "Line 3\nLine 4"]
     * }</pre>
     *
     * @param source the string to be split into lines, may be {@code null} or empty
     * @param limit the result threshold. A non-positive limit indicates no limit.
     * @return an array of strings computed by splitting the source string into lines.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @see #splitToLines(String)
     * @see Pattern#split(CharSequence, int)
     */
    public static String[] splitToLines(final String source, final int limit) {
        if (source == null) {
            return N.EMPTY_STRING_ARRAY;
        } else if (source.isEmpty()) {
            return new String[] { Strings.EMPTY };
        }

        return LINE_SEPARATOR.split(source, limit);
    }
}