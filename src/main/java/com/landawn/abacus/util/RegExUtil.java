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

import java.util.regex.PatternSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.function.IntBiFunction;

/**
 * A comprehensive utility class providing high-performance, thread-safe methods for regular expression operations
 * on strings, including pattern matching, replacement, splitting, and extraction. This class combines the power
 * of Java's regular expression engine with convenient helper methods and pre-compiled patterns for common use cases,
 * making regex operations more accessible and efficient for everyday programming tasks.
 *
 * <p>This utility class addresses common pain points in regex usage by providing null-safe operations, pre-compiled
 * patterns for performance optimization, and intuitive method names that clearly express intent. It includes
 * specialized patterns for extracting numbers, dates, emails, URLs, and Java identifiers, along with flexible
 * replacement mechanisms using both string literals and functional transformations.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Thread Safety:</b> All methods are thread-safe and can be used safely in concurrent environments</li>
 *   <li><b>Null Safety:</b> Comprehensive {@code null} checking with predictable behavior for {@code null} inputs</li>
 *   <li><b>Performance Optimized:</b> Pre-compiled patterns and efficient algorithms for common operations</li>
 *   <li><b>Pre-defined Patterns:</b> Common regex patterns for numbers, dates, emails, URLs, and identifiers</li>
 *   <li><b>Functional Support:</b> Lambda-friendly replacement methods with function-based transformations</li>
 *   <li><b>Stream Integration:</b> Native support for Java 8+ streams for match processing</li>
 *   <li><b>Flexible Replacement:</b> Multiple replacement strategies including literal, functional, and indexed</li>
 *   <li><b>Comprehensive Coverage:</b> Complete set of regex operations for string processing</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Convenience Over Complexity:</b> Simplified API that handles common regex tasks without boilerplate</li>
 *   <li><b>Performance Over Simplicity:</b> Pre-compiled patterns and optimized algorithms for production use</li>
 *   <li><b>Safety Over Speed:</b> Comprehensive {@code null} checking and error handling for robust applications</li>
 *   <li><b>Functional Programming:</b> Lambda-friendly design for modern Java development patterns</li>
 *   <li><b>Apache Heritage:</b> Built on proven Apache Commons Lang foundations with enhancements</li>
 * </ul>
 *
 * <p><b>Pre-compiled Pattern Constants:</b>
 * <ul>
 *   <li><b>{@link #JAVA_IDENTIFIER_MATCHER}:</b> Matches valid Java identifiers (variables, methods, classes)</li>
 *   <li><b>{@link #NUMBER_FINDER}:</b> Extracts numeric values including integers and decimals</li>
 *   <li><b>{@link #EMAIL_ADDRESS_RFC_5322_MATCHER}:</b> Checks that an entire string looks like an email address (used by {@link Strings#isValidEmailAddress(CharSequence)})</li>
 *   <li><b>{@link #URL_MATCHER}:</b> Checks that an entire string looks like a URL (http, https, ftp, or file; heuristic — for strict validation see {@link Strings#isValidUrl(CharSequence)})</li>
 *   <li><b>{@link #HTTP_URL_MATCHER}:</b> Checks that an entire string looks like an HTTP/HTTPS URL (heuristic; for strict validation see {@link Strings#isValidHttpUrl(CharSequence)})</li>
 *   <li><b>{@link #DATE_MATCHER}:</b> Recognizes ISO-style date formats (yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd)</li>
 *   <li><b>{@link #WHITESPACE_MATCHER}:</b> Matches a string consisting entirely of whitespace</li>
 *   <li><b>{@link #LINE_SEPARATOR}:</b> Platform-independent line ending detection</li>
 * </ul>
 *
 * <p><b>Method Categories:</b>
 * <ul>
 *   <li><b>Pattern Matching:</b> {@code find()}, {@code matches()}, {@code countMatches()}</li>
 *   <li><b>Match Extraction:</b> {@code findFirst()}, {@code findLast()}, {@code findAll()}</li>
 *   <li><b>String Replacement:</b> {@code replaceFirst()}, {@code replaceLast()}, {@code replaceAll()}</li>
 *   <li><b>String Removal:</b> {@code removeFirst()}, {@code removeLast()}, {@code removeAll()} (replacement with {@code ""})</li>
 *   <li><b>String Splitting:</b> {@code split()}, {@code splitToLines()}</li>
 *   <li><b>Stream Operations:</b> {@code matchResults()}, {@code matchIndices()}</li>
 *   <li><b>Functional Replacement:</b> Methods accepting {@code Function} and {@code IntBiFunction} parameters</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic pattern matching and validation
 * boolean hasNumber = RegExUtil.find("Order #12345", RegExUtil.NUMBER_FINDER);
 * boolean isValidEmail = RegExUtil.matches("user@example.com", RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER);
 * boolean isJavaClass = RegExUtil.matches("MyClass", RegExUtil.JAVA_IDENTIFIER_MATCHER);
 *
 * // String cleaning and normalization
 * String normalized = RegExUtil.replaceAll("Hello    World", "\\s+", " ");
 * String cleaned = RegExUtil.replaceAll(text, RegExUtil.WHITESPACE_FINDER, " ");
 *
 * // Extracting and counting matches
 * int numberCount = RegExUtil.countMatches("1 apple, 2 oranges, 3 bananas", "\\d+");
 * String[] emails = RegExUtil.split("user1@a.com;user2@b.com", ";");
 *
 * // Functional replacement with transformations
 * String uppercased = RegExUtil.replaceAll("hello world", "\\w+", (String match) -> match.toUpperCase());
 * // The IntBiFunction replacer receives the (start, end) indices of each match
 * String indexed = RegExUtil.replaceAll("a b c", "\\w", (start, end) -> "[" + start + "]");
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Stream-based match processing
 * List<String> allNumbers = RegExUtil.matchResults("Price: $19.99, Tax: $2.50", "\\d+\\.\\d+")
 *     .map(MatchResult::group)
 *     .collect(Collectors.toList());
 *
 * // Complex replacement with context
 * String escapedSource = RegExUtil.replaceAll(sourceCode, RegExUtil.JAVA_IDENTIFIER_FINDER,
 *     identifier -> isReservedWord(identifier) ? escapeIdentifier(identifier) : identifier);
 *
 * // Line-by-line processing
 * String[] lines = RegExUtil.splitToLines(multilineText);
 * String processedLines = Arrays.stream(lines)
 *     .map(line -> RegExUtil.replaceAll(line, "\\btodo\\b", "DONE"))
 *     .collect(Collectors.joining("\n"));
 *
 * // Match indices for position-aware processing
 * IntStream positions = RegExUtil.matchIndices("The quick brown fox", "\\b\\w{5}\\b");
 * positions.forEach(pos -> System.out.println("5-letter word at position: " + pos));
 * }</pre>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li><b>Pattern Compilation:</b> Use pre-compiled Pattern objects for repeated operations</li>
 *   <li><b>Method Overloads:</b> Pattern-accepting methods are faster than string regex methods</li>
 *   <li><b>Stream Operations:</b> Lazy evaluation in stream methods for memory efficiency</li>
 *   <li><b>Replacement Strategies:</b> Functional replacements have slight overhead but offer flexibility</li>
 *   <li><b>Memory Usage:</b> Large text processing benefits from streaming approaches</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Static Methods:</b> All utility methods use only method-local mutable state and are thread-safe</li>
 *   <li><b>Pattern Objects:</b> Pre-compiled Pattern instances are thread-safe and reusable</li>
 *   <li><b>Matcher Objects:</b> Internal Matcher instances are not shared between threads</li>
 *   <li><b>No Mutable Shared State:</b> The shared pattern constants are immutable; matcher instances are created per invocation</li>
 * </ul>
 *
 * <p><b>Pattern Matching Methods:</b>
 * <ul>
 *   <li><b>{@code find()}:</b> Tests if pattern exists anywhere in the string</li>
 *   <li><b>{@code matches()}:</b> Tests if entire string matches the pattern</li>
 *   <li><b>{@code countMatches()}:</b> Returns the number of non-overlapping matches</li>
 *   <li><b>{@code matchResults()}:</b> Returns a Stream of MatchResult objects for processing</li>
 *   <li><b>{@code matchIndices()}:</b> Returns start positions of all matches as IntStream</li>
 * </ul>
 *
 * <p><b>Replacement Method Variants:</b>
 * <ul>
 *   <li><b>String Replacement:</b> Template replacements for first/all and literal replacement for last</li>
 *   <li><b>Function Replacement:</b> Transform matches using Function&lt;String, String&gt;</li>
 *   <li><b>Indexed Replacement:</b> Transform matches with access to the match's start and end indices via IntBiFunction</li>
 *   <li><b>First/Last/All:</b> Control which occurrences are replaced</li>
 * </ul>
 *
 * <p><b>Splitting Operations:</b>
 * <ul>
 *   <li><b>{@code split()}:</b> Split string around regex matches with optional limit</li>
 *   <li><b>{@code splitToLines()}:</b> Platform-independent line splitting with limit support</li>
 *   <li><b>Limit Parameter:</b> Controls maximum number of resulting array elements</li>
 *   <li><b>Empty Handling:</b> A {@code null} source yields an empty array and an empty source an array holding one
 *       empty string; the no-limit overloads discard trailing empty results ({@code limit == 0}), while a negative
 *       limit keeps them</li>
 * </ul>
 *
 * <p><b>Error Handling and Validation:</b>
 * <ul>
 *   <li><b>IllegalArgumentException:</b> Thrown when a required {@code regex} is {@code null} or empty, or a required {@code Pattern} is {@code null}</li>
 *   <li><b>PatternSyntaxException:</b> Propagated from compilation of a syntactically invalid regex</li>
 *   <li><b>Consistent Validation:</b> String regexes are compiled and validated even when the source is {@code null} or empty</li>
 *   <li><b>Null Safety:</b> A {@code null} source is never matched; it returns {@code null} or the method's empty result</li>
 *   <li><b>Parameter Validation:</b> Comprehensive checking of method parameters</li>
 * </ul>
 *
 * <p><b>{@code null} vs. empty source:</b> an <i>empty but non-{@code null}</i> source is matched
 * normally by every method, so a zero-width-capable pattern still reports its one empty match and the
 * class agrees with itself and with the JDK on empty input:
 * <pre>{@code
 * RegExUtil.find("", Pattern.compile("a*"));         // true
 * RegExUtil.findFirst("", Pattern.compile("a*"));    // ""
 * RegExUtil.findAll("", Pattern.compile("a*"));      // [""]      (one empty match)
 * RegExUtil.countMatches("", Pattern.compile("a*")); // 1
 * RegExUtil.replaceAll("", Pattern.compile("a*"), "X");  // "X"   (same as "".replaceAll("a*", "X"))
 * }</pre>
 * Earlier releases short-circuited empty sources too, which made {@code findAll}/{@code countMatches}/
 * {@code matchResults}/{@code matchIndices}/{@code replace*} report "no match" for inputs that
 * {@code find}/{@code findFirst}/{@code matches} reported as matching.
 *
 * <p>A {@code null} source, by contrast, is <b>never matched</b> by any method in this class: it is
 * short-circuited to that method's empty result <i>without</i> being handed to a matcher, so a
 * zero-width-capable pattern reports nothing for it. The rule is uniform, which matters only for a
 * pattern that can match the empty string:
 * <pre>{@code
 * RegExUtil.find(null, Pattern.compile("a*"));         // false
 * RegExUtil.matches(null, Pattern.compile("a*"));      // false
 * RegExUtil.findFirst(null, Pattern.compile("a*"));    // null   (not "")
 * RegExUtil.findAll(null, Pattern.compile("a*"));      // []
 * RegExUtil.countMatches(null, Pattern.compile("a*")); // 0
 * RegExUtil.replaceAll(null, Pattern.compile("a*"), "X");  // ""  (not "X")
 * RegExUtil.split(null, Pattern.compile("a*"));        // []     (empty array)
 * }</pre>
 * The empty result is {@code false}, {@code null}, {@code ""}, {@code 0}, or an empty
 * list/stream/array, whichever the method returns. Earlier releases split the class in two here -
 * {@code find}/{@code matches}/{@code findFirst}/{@code findLast} normalized {@code null} to
 * {@code ""} and matched it, so {@code find(null, "a*")} was {@code true} while
 * {@code countMatches(null, "a*")} was {@code 0}. Pass {@code ""} explicitly when a {@code null} and
 * an empty source must behave identically.
 *
 * <p><b>Integration with Java Regex API:</b>
 * <ul>
 *   <li><b>Pattern Compatibility:</b> All methods accept both String regex and Pattern objects</li>
 *   <li><b>MatchResult Interface:</b> Stream methods return standard MatchResult objects</li>
 *   <li><b>Flag Support:</b> Pattern flags can be specified through Pattern.compile()</li>
 *   <li><b>Group Extraction:</b> Full support for capturing groups in MatchResult objects</li>
 * </ul>
 *
 * <p><b>Common Regex Patterns and Use Cases:</b>
 * <ul>
 *   <li><b>Data Validation:</b> Email, phone number, URL, and identifier validation</li>
 *   <li><b>Text Extraction:</b> Pulling specific data types from unstructured text</li>
 *   <li><b>Code Processing:</b> Java identifier extraction and source code manipulation</li>
 *   <li><b>Data Cleaning:</b> Whitespace normalization and text standardization</li>
 *   <li><b>Log Processing:</b> Extracting structured data from log files</li>
 *   <li><b>Template Processing:</b> Variable substitution and template expansion</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use pre-compiled Pattern constants for frequently used regex patterns</li>
 *   <li>Prefer Pattern-accepting methods over String regex methods for better performance</li>
 *   <li>Use functional replacement methods for complex transformations</li>
 *   <li>Consider stream-based methods for large-scale text processing</li>
 *   <li>Cache compiled Pattern objects when processing multiple strings with the same regex</li>
 *   <li>Use specific methods (find vs matches) based on your exact requirements</li>
 *   <li>Validate regex patterns during development to avoid runtime PatternSyntaxException</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Compiling the same regex pattern repeatedly instead of caching Pattern objects</li>
 *   <li>Using matches() when find() would be sufficient for substring detection</li>
 *   <li>Ignoring {@code null} safety - always handle potential {@code null} inputs appropriately</li>
 *   <li>Using overly complex regex when simple string operations would suffice</li>
 *   <li>Creating unnecessary intermediate strings in functional replacement chains</li>
 *   <li>Using replaceAll() when replaceFirst() or replaceLast() would be more appropriate</li>
 * </ul>
 *
 * <p><b>Performance Optimization Tips:</b>
 * <ul>
 *   <li><b>Pattern Reuse:</b> Store frequently used Pattern objects in static final fields</li>
 *   <li><b>Lazy Compilation:</b> Compile patterns only when first used for startup performance</li>
 *   <li><b>Stream Processing:</b> Use stream methods for memory-efficient processing of large texts</li>
 *   <li><b>Specific Methods:</b> Use the most specific method for your use case (first/last/all)</li>
 *   <li><b>Limit Parameters:</b> Use split limits to avoid unnecessary array allocations</li>
 * </ul>
 *
 * <p><b>Usage Examples: Log File Processing</b></p>
 * <pre>{@code
 * public class LogProcessor {
 *     private static final Pattern TIMESTAMP_PATTERN =
 *         Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
 *     private static final Pattern ERROR_PATTERN =
 *         Pattern.compile("ERROR|FATAL", Pattern.CASE_INSENSITIVE);
 *
 *     public List<LogEntry> processLogFile(String logContent) {
 *         return Arrays.stream(RegExUtil.splitToLines(logContent))
 *             .filter(line -> RegExUtil.find(line, ERROR_PATTERN))
 *             .map(this::parseLogEntry)
 *             .collect(Collectors.toList());
 *     }
 *
 *     private LogEntry parseLogEntry(String line) {
 *         String timestamp = RegExUtil.matchResults(line, TIMESTAMP_PATTERN)
 *             .findFirst()
 *             .map(MatchResult::group)
 *             .orElse("Unknown");
 *
 *         String level = RegExUtil.matchResults(line, ERROR_PATTERN)
 *             .findFirst()
 *             .map(MatchResult::group)
 *             .orElse("INFO");
 *
 *         return new LogEntry(timestamp, level, line);
 *     }
 *
 *     public String anonymizeLog(String logContent) {
 *         // Replace email addresses with [EMAIL]
 *         String anonymized = RegExUtil.replaceAll(logContent, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER, "[EMAIL]");
 *
 *         // Replace numbers with [NUM]
 *         return RegExUtil.replaceAll(anonymized, RegExUtil.NUMBER_FINDER, "[NUM]");
 *     }
 * }
 * }</pre>
 *
 * <p><b>Compatibility and Migration:</b>
 * <ul>
 *   <li><b>Apache Commons:</b> Drop-in replacement for most Apache Commons Lang regex utilities</li>
 *   <li><b>JDK Compatibility:</b> Uses modern JDK stream and matcher APIs (Java 9+)</li>
 *   <li><b>Backward Compatibility:</b> Method signatures designed for easy migration from raw regex usage</li>
 *   <li><b>Future-Proof:</b> Designed to accommodate future Java regex enhancements</li>
 * </ul>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang under the Apache License 2.0.
 * Methods from these libraries may have been modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @see java.util.regex.Pattern
 * @see java.util.regex.Matcher
 * @see java.util.regex.MatchResult
 * @see java.util.stream.Stream
 * @see java.util.function.Function
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.function.IntBiFunction
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/package-summary.html">Java Regular Expressions Documentation</a>
 * @see <a href="https://quickref.me/regex.html">Regular Expression Quick Reference</a>
 * @see <a href="https://commons.apache.org/proper/commons-lang/">Apache Commons Lang</a>
 */
public final class RegExUtil {

    /**
     * A regular expression {@link Pattern} that matches valid Java identifiers within a string.
     * <p>
     * This pattern captures sequences that follow Java's identifier naming rules: starting with
     * a character accepted by {@link Character#isJavaIdentifierStart(int)}, followed by any number of
     * characters accepted by {@link Character#isJavaIdentifierPart(int)}. It is useful for extracting variable names,
     * method names, class names, and other identifiers from Java source code or text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \p{javaJavaIdentifierStart}} — a Java identifier-start character</li>
     *   <li>{@code \p{javaJavaIdentifierPart}*} — zero or more Java identifier-part characters</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "myVariable"}</li>
     *   <li>{@code "_privateField"}</li>
     *   <li>{@code "$specialVar"}</li>
     *   <li>{@code "className123"}</li>
     *   <li>{@code "MAX_VALUE"}</li>
     *   <li>{@code "变量"} (Unicode letters)</li>
     *   <li>{@code "class"} (the pattern is syntactic and does not exclude Java keywords)</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "123invalid"} (not matched as a whole token; only {@code "invalid"} is matched)</li>
     *   <li>{@code "my-variable"} (not matched as a whole token; {@code "my"} and {@code "variable"} are matched separately)</li>
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
    public static final Pattern JAVA_IDENTIFIER_FINDER = Pattern.compile("(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)");

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
     *   <li>{@code "12.34"} (not matched as a whole token; {@code "12"} and {@code "34"} are matched separately)</li>
     *   <li>{@code "abc"} (contains no digits — no match at all)</li>
     *   <li>{@code "1.5e10"} (not matched as a whole token; the digit runs are matched separately)</li>
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
     *   <li>An integer part (one or more digits) with an optional fractional part</li>
     *   <li>Or a leading-dot fraction (for example {@code .5})</li>
     * </ul>
     * It can be used to extract signed or unsigned integers and floating-point numbers from text.
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [+-]?} — optional sign</li>
     *   <li>{@code (?:\\d+(?:\\.\\d*)?|\\.\\d+)} — digits with an optional fraction
     *       ({@code 123}, {@code 123.45}, {@code 123.}), or a leading-dot fraction ({@code .5})</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "-3.14"}</li>
     *   <li>{@code "+0.99"}</li>
     *   <li>{@code ".5"} / {@code "-.5"} / {@code "+.25"}</li>
     *   <li>{@code "100."} (trailing-dot form; parses as {@code 100.0})</li>
     * </ul>
     *
     * <p><strong>Note:</strong> A leading-dot mantissa keeps its sign, so callers that parse the
     * matched text (for example {@link Numbers#extractFirstDouble(String)}) see {@code .5} as
     * {@code 0.5} and {@code x=-.5} as {@code -0.5}.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern NUMBER_FINDER = Pattern.compile("([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))");

    /**
     * A regular expression {@link Pattern} that matches positive (unsigned) numbers including decimals.
     * <p>
     * This pattern captures numeric values that may include:
     * <ul>
     *   <li>An integer part with an optional fractional part ({@code 42}, {@code 3.14})</li>
     *   <li>or a leading-dot fraction with no integer part ({@code .25})</li>
     * </ul>
     * It can match numbers like {@code .25}, {@code 3.14}, or {@code 100}.
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (?:...)} — a non-capturing group around the two alternatives, so they stay one unit
     *       when the constant is spliced into a larger expression</li>
     *   <li>{@code \\d+(?:\\.\\d+)?} — an integer part with an optional fractional part</li>
     *   <li>{@code |} — or</li>
     *   <li>{@code \\.\\d+} — a leading-dot fraction (no integer part)</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "3.14"}</li>
     *   <li>{@code "0.99"}</li>
     *   <li>{@code ".25"}</li>
     * </ul>
     *
     * <p><strong>Note:</strong> The match must end with at least one digit, so a trailing dot
     * is not included; for example, in {@code "100."} only {@code "100"} is matched.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern POSITIVE_NUMBER_FINDER = Pattern.compile("(?:\\d+(?:\\.\\d+)?|\\.\\d+)");

    /**
     * A regular expression {@link Pattern} that matches negative numbers including decimals.
     * <p>
     * This pattern captures negative numeric values that may include:
     * <ul>
     *   <li>A required minus sign prefix</li>
     *   <li>An integer part with an optional fractional part ({@code -7}, {@code -3.14})</li>
     *   <li>or a leading-dot fraction with no integer part ({@code -.25})</li>
     * </ul>
     * It can match numbers like {@code -.25}, {@code -3.14}, or {@code -100}.
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code -} — required minus sign</li>
     *   <li>{@code (?:\\d+(?:\\.\\d+)?|\\.\\d+)} — an integer part with an optional fractional part, or a
     *       leading-dot fraction; the group makes the sign apply to both forms</li>
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
     * <p><strong>Note:</strong> The match must end with at least one digit, so a trailing dot
     * is not included; for example, in {@code "-100."} only {@code "-100"} is matched. {@link #NUMBER_FINDER}
     * deliberately differs here - it accepts the trailing-dot form {@code "-100."} in full.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern NEGATIVE_NUMBER_FINDER = Pattern.compile("-(?:\\d+(?:\\.\\d+)?|\\.\\d+)");

    /**
     * A regular expression {@link Pattern} that matches numbers in standard or scientific notation.
     * <p>
     * This pattern supports:
     * <ul>
     *   <li>Optional leading '+' or '-' sign</li>
     *   <li>An integer or decimal part, including a leading-dot fraction (e.g., {@code 123}, {@code 3.14}, {@code .5})</li>
     *   <li>An optional exponent part with 'e' or 'E', followed by an optional sign and digits (e.g., {@code e+10}, {@code E-5})</li>
     * </ul>
     * It can be used to extract integers, floating-point numbers, and scientific notation numbers from text.
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code [+-]?} — optional sign</li>
     *   <li>{@code (?:\\d+(?:\\.\\d*)?|\\.\\d+)} — digits with an optional fraction, or a leading-dot fraction</li>
     *   <li>{@code (?:[eE][+-]?\\d+)?} — optional exponent part (e.g., {@code e10}, {@code E-3})</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "42"}</li>
     *   <li>{@code "-3.14"}</li>
     *   <li>{@code "+6.022e23"}</li>
     *   <li>{@code "1E-9"}</li>
     *   <li>{@code ".5"} / {@code "-.5"} / {@code ".5e2"} / {@code "-.5e2"}</li>
     *   <li>{@code "100."} (trailing-dot form; parses as {@code 100.0})</li>
     * </ul>
     *
     * <p><strong>Note:</strong> A leading-dot mantissa keeps its sign and any following exponent, so
     * callers that parse the matched text (for example {@link Numbers#extractFirstDouble(String, boolean)})
     * see {@code .5e2} as {@code 50.0} and {@code x=-.5e2} as {@code -50.0}.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern SCIENTIFIC_NUMBER_FINDER = Pattern.compile("([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)");

    /**
     * A regular expression {@link Pattern} that matches phone numbers within a string.
     * <p>
     * This pattern matches sequences of digits and spaces that contain at least three digits,
     * optionally prefixed with a plus sign for international dialing codes.
     * It is useful for extracting basic phone numbers from text.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\+?} — optional plus sign for international code</li>
     *   <li>a lookahead requiring at least three digits</li>
     *   <li>{@code [\\d\\s]{3,}} — the digits and whitespace making up the match</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "123 456 7890"}</li>
     *   <li>{@code "+1 234 567 8900"}</li>
     *   <li>{@code "123 456"} (digits and spaces only; hyphens are not matched)</li>
     * </ul>
     *
     * <p><b>Performance:</b> the lookahead {@code (?=(?:\\s*\\d){3})} is re-run from its own start at
     * every candidate position, so {@code find()} over a long run of whitespace that never reaches three digits
     * costs time quadratic in the input length: measured about 10&nbsp;ms at 2&nbsp;KB of spaces but roughly
     * 2.2&nbsp;seconds at 31&nbsp;KB, i.e. about 4x for every doubling. Do not run this pattern against input of
     * unbounded or attacker-controlled size; bound the length first. The anchored {@link #PHONE_NUMBER_MATCHER}
     * has a single start position and stays linear.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern PHONE_NUMBER_FINDER = Pattern.compile("\\+?(?=(?:\\s*\\d){3})[\\d\\s]{3,}");

    /**
     * A regular expression {@link Pattern} that matches phone numbers with country codes.
     * <p>
     * This pattern matches phone numbers written with an optional plus sign and then only digits and
     * spaces. The lookahead requires at least 10 digits overall, but the two consuming pieces
     * ({@code [\\d\\s]+} plus {@code [\\d\\s]{10,}}) together require at least <b>eleven</b> characters drawn
     * from {@code [\\d\\s]}, so a bare ten-digit number such as {@code "5551234567"} does <b>not</b> match - a
     * separator or an eleventh digit is needed.
     * It is designed to match longer phone numbers that include country and area codes.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code \\+?} — optional plus sign for international code</li>
     *   <li>a lookahead requiring at least ten digits overall</li>
     *   <li>{@code [\\d\\s]+} — one or more digits or spaces</li>
     *   <li>{@code \\(?} — a single optional opening parenthesis (vestigial; see the note below)</li>
     *   <li>{@code [\\d\\s]{10,}} — at least 10 trailing digits or spaces</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "+1 234 567 8900"}</li>
     *   <li>{@code "+44 20 1234 5678"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "(123) 456 7890"} — parenthesised numbers are <b>not</b> supported</li>
     *   <li>{@code "+1-234-567-8900"} — hyphens are not matched</li>
     *   <li>{@code "5551234567"} — exactly ten digits with no separator is one character too short</li>
     * </ul>
     *
     * <p><b>Note:</b> the {@code \\(?} fragment cannot make a parenthesised number match, because the
     * closing {@code )} is absent from every consuming character class. It only ever admits a single
     * unbalanced {@code (}. Treat this as a coarse finder for space-separated digit runs; use a
     * dedicated phone-number library for real validation.</p>
     *
     * <p><b>Performance:</b> this pattern has two superlinear shapes. Its lookahead
     * {@code (?=(?:[\\s(]*\\d){10})} is re-run from its own start at every candidate position, so
     * {@code find()} over a long run of whitespace that never reaches ten digits costs time quadratic in the input
     * length (about 11&nbsp;ms at 2&nbsp;KB of spaces, roughly 3.1&nbsp;seconds at 31&nbsp;KB). Separately, because
     * {@code [\\d\\s]+} and {@code [\\d\\s]{10,}} draw from the same character class, a whole-string match that
     * ultimately fails has to try every split of the run between them: {@link #PHONE_NUMBER_WITH_CODE_MATCHER} on a
     * long digit run followed by one non-digit measured about 14&nbsp;ms at 2&nbsp;KB and roughly 5.5&nbsp;seconds at
     * 31&nbsp;KB. Both shapes are about 4x for every doubling. Do not run this pattern against input of unbounded or
     * attacker-controlled size; bound the length first.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern PHONE_NUMBER_WITH_CODE_FINDER = Pattern.compile("\\+?(?=(?:[\\s(]*\\d){10})[\\d\\s]+\\(?[\\d\\s]{10,}");

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
     *   <li>{@code " "} — literal space separating date and time</li>
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
     * This pattern implements a commonly used ASCII, RFC 5322-inspired subset. It handles quoted local parts,
     * IPv4 domain literals, and many permitted special characters, but it is not a complete RFC parser and does
     * not support every valid address (for example, internationalized local parts or IPv6 domain literals).
     * </p>
     *
     * <p>The pattern validates:</p>
     * <ul>
     *   <li>Local part: alphanumeric characters and special characters {@code !#$%&'*+/=?^_`{|}~-}</li>
     *   <li>Domain part: standard domain names or IP addresses in brackets</li>
     *   <li>Quoted strings in the local part with escaped characters</li>
     * </ul>
     *
     * <p><b>Note on the domain-literal character class:</b> the general-address-literal branch uses the
     * {@code dtext} set of RFC 5322 &sect;3.4.1 (decimal 33-90 and 94-126, i.e. printable ASCII except
     * {@code [}, {@code \} and {@code ]}, which must be escaped inside a domain literal), extended with the
     * obsolete DEL that the upstream expression allowed. The widely copied form of this regex writes the second
     * range as {@code x53-x7f} instead of {@code x5e-x7f}; because that overlaps the first range it re-admits
     * exactly the three characters the class exists to exclude, so {@code a@[1.2.3.x:a]b]} was accepted.</p>
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
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x5e-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])",
            Pattern.CASE_INSENSITIVE);

    /**
     * A regular expression {@link Pattern} that finds URLs within a larger text.
     * <p>
     * This coarse extraction pattern searches for URLs in input text and supports HTTP, HTTPS, FTP, and FILE
     * schemes case-insensitively. It does not perform full URI syntax or host validation.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (https?|ftp|file)} — protocol: http, https, ftp, or file</li>
     *   <li>{@code ://} — protocol separator</li>
     *   <li>{@code [^\\s/$.?#]} — first character of host (not whitespace, slash, dollar, dot, question mark, or hash)</li>
     *   <li>{@code [^\\s]*} — zero or more non-whitespace characters (rest of URL)</li>
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
     *   <li>{@code "http://example .com"} — not matched as a whole; only {@code "http://example"} is matched</li>
     *   <li>{@code "example.com"} — missing protocol</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This pattern can find URLs within larger text strings. To require
     * the entire input string to be a URL, use {@link #URL_MATCHER} instead. For matching only
     * HTTP and HTTPS URLs, see {@link #HTTP_URL_FINDER}. For validating an entire string as a URL,
     * prefer {@link Strings#isValidUrl(CharSequence)} (strict {@code java.net.URI} parsing plus
     * scheme/host checks) over this regex heuristic.</p>
     *
     * @see java.util.regex.Pattern
     * @see #HTTP_URL_FINDER
     * @see #URL_MATCHER
     * @see Strings#isValidUrl(CharSequence)
     */
    public static final Pattern URL_FINDER = Pattern.compile("(https?|ftp|file)://[^\\s/$.?#][^\\s]*", Pattern.CASE_INSENSITIVE);

    /**
     * A regular expression {@link Pattern} that matches HTTP and HTTPS URLs.
     * <p>
     * This is the HTTP(S)-only counterpart of {@link #URL_FINDER}: the same coarse host/path/query/fragment
     * heuristic, restricted to the {@code http} and {@code https} schemes. It is a syntactic prefilter, not
     * full URI validation.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code https?} — http or https scheme</li>
     *   <li>{@code ://} — protocol separator</li>
     *   <li>{@code [^\\s/$.?#]} — first character of host (not whitespace, slash, dollar, dot, question mark, or hash)</li>
     *   <li>{@code [^\\s]*} — zero or more non-whitespace characters (rest of URL, including {@code -}, {@code %}, {@code +}, {@code ~})</li>
     * </ul>
     *
     * <p>Example matches:</p>
     * <ul>
     *   <li>{@code "http://www.example.com"}</li>
     *   <li>{@code "https://example.com/foo-bar"}</li>
     *   <li>{@code "https://example.com/search?q=hello+world"}</li>
     *   <li>{@code "https://example.com/a%20b"}</li>
     *   <li>{@code "https://api.example.com:8443/v1/users?id=123&name=test"}</li>
     *   <li>{@code "http://localhost:3000/path/to/resource#section"}</li>
     *   <li>{@code "https://example.com?view=compact#summary"}</li>
     * </ul>
     *
     * <p>Example non-matches:</p>
     * <ul>
     *   <li>{@code "http://.example.com"} — starts with a dot after the protocol (same as {@link #URL_FINDER})</li>
     *   <li>{@code "ftp://example.com"} — not an HTTP(S) scheme</li>
     * </ul>
     *
     * @see java.util.regex.Pattern
     * @see #URL_FINDER
     * @see #HTTP_URL_MATCHER
     * @see Strings#isValidHttpUrl(CharSequence)
     */
    public static final Pattern HTTP_URL_FINDER = Pattern.compile("https?://[^\\s/$.?#][^\\s]*", Pattern.CASE_INSENSITIVE);

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
     * The {@code \\s} class is <b>ASCII</b>-only (exactly {@code [ \\t\\n\\x0B\\f\\r]}), so NBSP
     * ({@code U+00A0}) and the other Unicode space separators do <b>not</b> match.
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
     * more than once in the text. It matches an occurrence that has the same word later in the input.
     * Matching is case-sensitive, Unicode-aware, and can detect duplicates separated by line terminators.
     * </p>
     *
     * <p>Regex breakdown:</p>
     * <ul>
     *   <li>{@code (\\b\\w+\\b)} — captures a complete word</li>
     *   <li>{@code (?=[\\s\\S]*\\b\\1\\b)} — lookahead to check if the same word appears again later</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "the quick brown fox jumps over the lazy dog";
     * Matcher matcher = RegExUtil.DUPLICATES_FINDER.matcher(text);
     * // Will match "the" (appears twice)
     * }</pre>
     *
     * <p><b>Performance:</b> the {@code [\\s\\S]*} lookahead rescans the remainder of the input for every word,
     * so the cost grows far faster than the input does. Measured on text made only of distinct words (the worst
     * case, since every lookahead runs to the end and fails): about 55&nbsp;ms at 2.4&nbsp;KB, but roughly
     * 4.8&nbsp;seconds at 47&nbsp;KB. Do not run this pattern against input of unbounded or attacker-controlled
     * size; collect words into a {@code Set} instead when the text may be large.</p>
     *
     * @see java.util.regex.Pattern
     */
    public static final Pattern DUPLICATES_FINDER = Pattern.compile("(\\b\\w+\\b)(?=[\\s\\S]*\\b\\1\\b)", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * A regular expression {@link Pattern} that matches whitespace sequences.
     * <p>
     * This pattern matches one or more consecutive <b>ASCII</b> whitespace characters — exactly
     * {@code [ \\t\\n\\x0B\\f\\r]}. It is <i>not</i> Unicode-aware: NBSP ({@code U+00A0}), {@code U+2028}
     * and the other Unicode space and line separators do <b>not</b> match. Compile your own
     * {@code Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS)} if you need those, and see
     * {@link #LINE_SEPARATOR} ({@code \\R}), which <i>is</i> Unicode-aware.
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
     * Pattern that matches an entire string with Java identifier syntax; keywords are not excluded.
     * This is the anchored version of {@link #JAVA_IDENTIFIER_FINDER} that requires the entire string to match.
     *
     * @see #JAVA_IDENTIFIER_FINDER
     * @see <a href="https://stackoverflow.com/questions/1449817/what-are-some-of-the-most-useful-regular-expressions-for-programmers">Stack Overflow Useful Regex</a>
     */
    public static final Pattern JAVA_IDENTIFIER_MATCHER = matchEntire(JAVA_IDENTIFIER_FINDER);

    /**
     * Pattern that matches an entire string if it is a signed or unsigned integer.
     * This is the anchored version of {@link #INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #INTEGER_FINDER
     */
    public static final Pattern INTEGER_MATCHER = matchEntire(INTEGER_FINDER);

    /**
     * Pattern that matches an entire string if it is a positive (unsigned) integer.
     * This is the anchored version of {@link #POSITIVE_INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #POSITIVE_INTEGER_FINDER
     */
    public static final Pattern POSITIVE_INTEGER_MATCHER = matchEntire(POSITIVE_INTEGER_FINDER);

    /**
     * Pattern that matches an entire string if it is a negative integer.
     * This is the anchored version of {@link #NEGATIVE_INTEGER_FINDER} that requires the entire string to match.
     *
     * @see #NEGATIVE_INTEGER_FINDER
     */
    public static final Pattern NEGATIVE_INTEGER_MATCHER = matchEntire(NEGATIVE_INTEGER_FINDER);

    /**
     * Pattern that matches an entire string if it is a signed or unsigned number (integer or decimal).
     * This is the anchored version of {@link #NUMBER_FINDER} that requires the entire string to match.
     * Leading-dot forms such as {@code ".5"} and {@code "-.5"} match, as do trailing-dot forms such as {@code "1."}.
     * The narrower {@link #POSITIVE_NUMBER_MATCHER} and {@link #NEGATIVE_NUMBER_MATCHER} deliberately disagree on the
     * trailing-dot form: {@code "100."} and {@code "-100."} match here but not there.
     *
     * @see #NUMBER_FINDER
     */
    public static final Pattern NUMBER_MATCHER = matchEntire(NUMBER_FINDER);

    /**
     * Pattern that matches an entire string if it is a positive number (integer or decimal).
     * This is the anchored version of {@link #POSITIVE_NUMBER_FINDER} that requires the entire string to match.
     * The string must end with a digit, so the trailing-dot form {@code "100."} does <b>not</b> match, although
     * {@link #NUMBER_MATCHER} accepts it.
     *
     * @see #POSITIVE_NUMBER_FINDER
     */
    public static final Pattern POSITIVE_NUMBER_MATCHER = matchEntire(POSITIVE_NUMBER_FINDER);

    /**
     * Pattern that matches an entire string if it is a negative number (integer or decimal).
     * This is the anchored version of {@link #NEGATIVE_NUMBER_FINDER} that requires the entire string to match.
     * The string must end with a digit, so the trailing-dot form {@code "-100."} does <b>not</b> match, although
     * {@link #NUMBER_MATCHER} accepts it.
     *
     * @see #NEGATIVE_NUMBER_FINDER
     */
    public static final Pattern NEGATIVE_NUMBER_MATCHER = matchEntire(NEGATIVE_NUMBER_FINDER);

    /**
     * Pattern that matches an entire string if it is a number in standard or scientific notation.
     * This is the anchored version of {@link #SCIENTIFIC_NUMBER_FINDER} that requires the entire string to match.
     * Leading-dot forms such as {@code ".5"}, {@code "-.5"}, {@code ".5e2"}, and {@code "-.5e2"} match.
     *
     * @see #SCIENTIFIC_NUMBER_FINDER
     */
    public static final Pattern SCIENTIFIC_NUMBER_MATCHER = matchEntire(SCIENTIFIC_NUMBER_FINDER);

    /**
     * Pattern that matches an entire string if it is a phone number.
     * This is the anchored version of {@link #PHONE_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #PHONE_NUMBER_FINDER
     */
    public static final Pattern PHONE_NUMBER_MATCHER = matchEntire(PHONE_NUMBER_FINDER);

    /**
     * Pattern that matches an entire string if it is a phone number with country code.
     * This is the anchored version of {@link #PHONE_NUMBER_WITH_CODE_FINDER} that requires the entire string to match.
     *
     * @see #PHONE_NUMBER_WITH_CODE_FINDER
     */
    public static final Pattern PHONE_NUMBER_WITH_CODE_MATCHER = matchEntire(PHONE_NUMBER_WITH_CODE_FINDER);

    /**
     * Pattern that matches an entire string if it is a date accepted by {@link #DATE_FINDER}, including its supported separators.
     * This is the anchored version of {@link #DATE_FINDER} that requires the entire string to match.
     *
     * @see #DATE_FINDER
     */
    public static final Pattern DATE_MATCHER = matchEntire(DATE_FINDER);

    /**
     * Pattern that matches an entire string if it is a time in HH:mm:ss format.
     * This is the anchored version of {@link #TIME_FINDER} that requires the entire string to match.
     *
     * @see #TIME_FINDER
     */
    public static final Pattern TIME_MATCHER = matchEntire(TIME_FINDER);

    /**
     * Pattern that matches an entire string if it is a date-time accepted by {@link #DATE_TIME_FINDER}, including its supported date separators.
     * This is the anchored version of {@link #DATE_TIME_FINDER} that requires the entire string to match.
     *
     * @see #DATE_TIME_FINDER
     */
    public static final Pattern DATE_TIME_MATCHER = matchEntire(DATE_TIME_FINDER);

    /**
     * Pattern that matches an entire string if it is a bank card number.
     * This is the anchored version of {@link #BANK_CARD_NUMBER_FINDER} that requires the entire string to match.
     *
     * @see #BANK_CARD_NUMBER_FINDER
     */
    public static final Pattern BANK_CARD_NUMBER_MATCHER = matchEntire(BANK_CARD_NUMBER_FINDER);

    /**
     * Pattern that matches an entire string against the RFC 5322-inspired subset described by
     * {@link #EMAIL_ADDRESS_RFC_5322_FINDER}; it is not a complete email-address validator.
     * This is the anchored version of {@link #EMAIL_ADDRESS_RFC_5322_FINDER} that requires the entire string to match.
     *
     * @see #EMAIL_ADDRESS_RFC_5322_FINDER
     * @see Strings#isValidEmailAddress(CharSequence)
     */
    public static final Pattern EMAIL_ADDRESS_RFC_5322_MATCHER = matchEntire(EMAIL_ADDRESS_RFC_5322_FINDER);

    /**
     * Pattern that matches an entire string if it looks like a URL.
     * This is the anchored version of {@link #URL_FINDER} that requires the entire string to match.
     *
     * <p>This remains a coarse regex heuristic (it accepts, for example, {@code "http://foo_bar.com"}).
     * For strict validation based on {@code java.net.URI} parsing, use
     * {@link Strings#isValidUrl(CharSequence)} instead.</p>
     *
     * @see #URL_FINDER
     * @see Strings#isValidUrl(CharSequence)
     */
    public static final Pattern URL_MATCHER = matchEntire(URL_FINDER);

    /**
     * Pattern that matches an entire string if it looks like an HTTP or HTTPS URL.
     * This is the anchored version of {@link #HTTP_URL_FINDER} that requires the entire string to match.
     *
     * <p>This remains a coarse regex heuristic. For strict validation based on {@code java.net.URI}
     * parsing, use {@link Strings#isValidHttpUrl(CharSequence)} instead.</p>
     *
     * @see #HTTP_URL_FINDER
     * @see Strings#isValidHttpUrl(CharSequence)
     */
    public static final Pattern HTTP_URL_MATCHER = matchEntire(HTTP_URL_FINDER);

    /**
     * Pattern that matches an entire string if it consists only of alphanumeric characters (no spaces).
     * This is the anchored version of {@link #ALPHANUMERIC_FINDER} that requires the entire string to match.
     *
     * @see #ALPHANUMERIC_FINDER
     */
    public static final Pattern ALPHANUMERIC_MATCHER = matchEntire(ALPHANUMERIC_FINDER);

    /**
     * Pattern that matches an entire string if it consists only of alphanumeric characters and spaces.
     * This is the anchored version of {@link #ALPHANUMERIC_SPACE_FINDER} that requires the entire string to match.
     *
     * @see #ALPHANUMERIC_SPACE_FINDER
     */
    public static final Pattern ALPHANUMERIC_SPACE_MATCHER = matchEntire(ALPHANUMERIC_SPACE_FINDER);

    /**
     * Pattern that matches the entire input if it contains a word that occurs again later.
     * Matching is case-sensitive, Unicode-aware, and spans line terminators.
     *
     * <p><b>Performance:</b> this wraps {@link #DUPLICATES_FINDER} in a further {@code [\s\S]*}
     * lookahead, so it inherits that pattern's cost and adds an outer scan on top - it is strictly
     * slower. The finder alone measured about 55&nbsp;ms on 2.4&nbsp;KB of distinct words but roughly
     * 4.8&nbsp;seconds on 47&nbsp;KB. Do not run either against input of unbounded or attacker-controlled
     * size; collect the words into a {@code Set} instead.</p>
     *
     * @see #DUPLICATES_FINDER
     */
    public static final Pattern DUPLICATES_MATCHER = Pattern.compile("\\A(?=[\\s\\S]*" + DUPLICATES_FINDER.pattern() + ")[\\s\\S]*\\z",
            DUPLICATES_FINDER.flags());

    /**
     * Pattern that matches an entire string if it consists only of whitespace.
     * This is the anchored version of {@link #WHITESPACE_FINDER} that requires the entire string to match.
     *
     * @see #WHITESPACE_FINDER
     */
    public static final Pattern WHITESPACE_MATCHER = matchEntire(WHITESPACE_FINDER);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] lines = RegExUtil.LINE_SEPARATOR.split("line1\nline2\r\nline3");
     * // Returns: ["line1", "line2", "line3"]
     * }</pre>
     *
     * @see java.util.regex.Pattern
     * @see #splitToLines(String)
     */
    public static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

    private static Pattern matchEntire(final Pattern pattern) {
        return Pattern.compile("\\A(?:" + pattern.pattern() + ")\\z", pattern.flags());
    }

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
     * <p><b>Usage Examples:</b></p>
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
     * // Returns: false ("test" needs four characters; a pattern such as "a*" would return true)
     *
     * boolean nullSource = RegExUtil.find(null, "\\w+");
     * // Returns: false (a null source never matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> If you need to use the same regex pattern multiple times,
     * consider pre-compiling it with {@link Pattern#compile(String)} and using
     * {@link #find(String, Pattern)} to avoid recompilation overhead.</p>
     *
     * @param source the input text to search; may be {@code null}, which never matches
     * @param regex the regular expression string to search for; must not be {@code null} or empty
     * @return {@code true} if the pattern is found in the source, {@code false} otherwise
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #find(String, Pattern)
     * @see #matches(String, String)
     * @see #findFirst(String, String)
     * @see #countMatches(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     */
    public static boolean find(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return find(source, Pattern.compile(regex));
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
     * <p><b>Usage Examples:</b></p>
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
     * // Returns: false (\d+ needs at least one digit; a pattern such as "a*" would return true)
     *
     * boolean nullSource = RegExUtil.find(null, digitPattern);
     * // Returns: false (a null source never matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is preferred over {@link #find(String, String)}
     * when performing multiple searches with the same pattern, as pattern compilation is
     * an expensive operation.</p>
     *
     * @param source the input text to search; may be {@code null}, which never matches
     * @param pattern the compiled regex pattern to search for; must not be {@code null}
     * @return {@code true} if the pattern is found in the source, {@code false} otherwise
     * @throws IllegalArgumentException if {@code pattern} is {@code null}.
     * @see #find(String, String)
     * @see #matches(String, Pattern)
     * @see #findFirst(String, Pattern)
     * @see #countMatches(String, Pattern)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     */
    public static boolean find(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return false;
        }

        return pattern.matcher(source).find();
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
     * <p><b>Usage Examples:</b></p>
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
     * // Returns: false (a null source never matches, not even ".*")
     * }</pre>
     *
     * <p><b>Note:</b> This method requires the entire input to match the pattern (as if it were
     * anchored), so explicit {@code ^} and {@code $} anchors are unnecessary. If you want to find a
     * pattern anywhere in the string, use {@link #find(String, String)} instead.</p>
     *
     * @param source the input text to match; may be {@code null}, which never matches
     * @param regex the regular expression string to match against; must not be {@code null} or empty
     * @return {@code true} if the entire source string matches the pattern, {@code false} otherwise
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #matches(String, Pattern)
     * @see #find(String, String)
     * @see Pattern#matches(String, CharSequence)
     * @see Matcher#matches()
     */
    public static boolean matches(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return matches(source, Pattern.compile(regex));
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
     * <p><b>Usage Examples:</b></p>
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
     * // Returns: false (a null source never matches, not even ".*")
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is preferred over {@link #matches(String, String)}
     * when performing multiple matches with the same pattern, as pattern compilation is
     * an expensive operation.</p>
     *
     * @param source the input text to match; may be {@code null}, which never matches
     * @param pattern the compiled regex pattern to match against; must not be {@code null}
     * @return {@code true} if the entire source string matches the pattern, {@code false} otherwise
     * @throws IllegalArgumentException if {@code pattern} is {@code null}.
     * @see #matches(String, String)
     * @see #find(String, Pattern)
     * @see Matcher#matches()
     * @see Pattern#compile(String)
     */
    public static boolean matches(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return false;
        }

        return pattern.matcher(source).matches();
    }

    /**
     * Finds the first match of the given regular expression in the specified input text.
     * <p>
     * This is a convenience method that compiles the regex string into a {@link Pattern}
     * and then searches for the first occurrence. If a match is found, the matched substring
     * is returned. If no match is found, {@code null} is returned.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.findFirst("abc123xyz456", "\\d+");
     * // Returns: "123"
     *
     * String email = RegExUtil.findFirst("Contact: john@example.com or jane@test.org",
     *                                   "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
     * // Returns: "john@example.com"
     *
     * String noResult = RegExUtil.findFirst("abc", "\\d+");
     * // Returns: null (pattern not found)
     *
     * String word = RegExUtil.findFirst("", "\\b\\w+\\b");
     * // Returns: null (\b\w+\b needs at least one word character; "a*" would return "")
     *
     * String nullSource = RegExUtil.findFirst(null, "\\d+");
     * // Returns: null (a null source never matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> If you need to use the same regex pattern multiple times,
     * consider using {@link #findFirst(String, Pattern)} instead to avoid recompiling
     * the pattern on each call.</p>
     *
     * @param source the input text to search; may be {@code null}, which never matches
     * @param regex the regular expression string to match; must not be {@code null} or empty
     * @return the first matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #findFirst(String, Pattern)
     * @see #findLast(String, String)
     * @see #find(String, String)
     * @see #matchResults(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     * @see Matcher#group()
     */
    @MayReturnNull
    public static String findFirst(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * String result = RegExUtil.findFirst("abc123xyz456", digitPattern);
     * // Returns: "123"
     *
     * Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
     * String email = RegExUtil.findFirst("Contact: john@example.com or jane@test.org", emailPattern);
     * // Returns: "john@example.com"
     *
     * Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
     * String word = RegExUtil.findFirst("", wordPattern);
     * // Returns: null (\b\w+\b needs at least one word character; "a*" would return "")
     *
     * Pattern noMatch = Pattern.compile("xyz");
     * String noResult = RegExUtil.findFirst("abc123", noMatch);
     * // Returns: null (pattern not found)
     * String nullSource = RegExUtil.findFirst(null, digitPattern);
     * // Returns: null (a null source never matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method is preferred over {@link #findFirst(String, String)}
     * when the same pattern is used multiple times, as it avoids recompiling the pattern on each call.</p>
     *
     * @param source the input text to search; may be {@code null}, which never matches
     * @param pattern the compiled regex pattern to match; must not be {@code null}
     * @return the first matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code pattern} is {@code null}.
     * @see #findLast(String, Pattern)
     * @see #find(String, Pattern)
     * @see #matchResults(String, Pattern)
     * @see Matcher#find()
     * @see Matcher#group()
     * @see Pattern#compile(String)
     */
    @MayReturnNull
    public static String findFirst(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return null; // NOSONAR
        }

        final Matcher matcher = pattern.matcher(source);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.findLast("abc123xyz456pqr789", "\\d+");
     * // Returns: "789"
     *
     * String word = RegExUtil.findLast("hello world java", "\\b\\w+\\b");
     * // Returns: "java"
     *
     * String email = RegExUtil.findLast("Contact: john@example.com or jane@test.org",
     *                                  "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
     * // Returns: "jane@test.org"
     *
     * String noResult = RegExUtil.findLast("abc", "\\d+");
     * // Returns: null (pattern not found)
     *
     * String nullSource = RegExUtil.findLast(null, "\\d+");
     * // Returns: null (a null source never matches)
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
     * @param source the input text to search; may be {@code null}, which never matches
     * @param regex the regular expression string to match; must not be {@code null} or empty
     * @return the last matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #findLast(String, Pattern)
     * @see #findFirst(String, String)
     * @see #find(String, String)
     * @see #matchResults(String, String)
     * @see Pattern#compile(String)
     * @see Matcher#find()
     * @see Matcher#group()
     */
    @MayReturnNull
    public static String findLast(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern digitPattern = Pattern.compile("\\d+");
     * String result = RegExUtil.findLast("abc123xyz456pqr789", digitPattern);
     * // Returns: "789"
     *
     * Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
     * String word = RegExUtil.findLast("hello world java", wordPattern);
     * // Returns: "java"
     *
     * Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
     * String email = RegExUtil.findLast("Contact: john@example.com or jane@test.org", emailPattern);
     * // Returns: "jane@test.org"
     *
     * Pattern noMatch = Pattern.compile("xyz");
     * String noResult = RegExUtil.findLast("abc123", noMatch);
     * // Returns: null (pattern not found)
     *
     * String nullSource = RegExUtil.findLast(null, digitPattern);
     * // Returns: null (a null source never matches)
     * }</pre>
     *
     * <p><b>Performance Note:</b> This method iterates through all matches in the string
     * to find the last one, so it may be less efficient than {@link #findFirst(String, Pattern)}
     * for very long strings with many matches. For better performance with large texts,
     * consider using alternative approaches if you only need to check for existence.</p>
     *
     * @param source the input text to search; may be {@code null}, which never matches
     * @param pattern the compiled regex pattern to match; must not be {@code null}
     * @return the last matched substring, or {@code null} if no match is found
     * @throws IllegalArgumentException if {@code pattern} is {@code null}.
     * @see #findFirst(String, Pattern)
     * @see #find(String, Pattern)
     * @see #matchResults(String, Pattern)
     * @see Matcher#find()
     * @see Matcher#group()
     * @see Pattern#compile(String)
     */
    @MayReturnNull
    public static String findLast(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return null; // NOSONAR
        }

        final Matcher matcher = pattern.matcher(source);
        String lastMatch = null;

        while (matcher.find()) {
            lastMatch = matcher.group();
        }

        return lastMatch;
    }

    /**
     * Finds all the substrings of the given string that match the specified regular expression
     * and returns them as a {@code List}. Each element of the returned list is the matched text
     * (i.e. {@link MatchResult#group()}) of one occurrence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> matches = RegExUtil.findAll("abc123def456", "\\d+");
     * // matches contains: ["123", "456"]
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null} or empty
     * @param regex the regular expression to match against; must not be {@code null} or empty
     * @return a list containing the matched text of each occurrence, in order of appearance;
     *         an empty list is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #matchResults(String, String)
     * @see #findFirst(String, String)
     */
    public static List<String> findAll(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return findAll(source, Pattern.compile(regex));
    }

    /**
     * Finds all the substrings of the given string that match the specified compiled pattern
     * and returns them as a {@code List}. Each element of the returned list is the matched text
     * (i.e. {@link MatchResult#group()}) of one occurrence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w+@\\w+\\.\\w+\\b");
     * List<String> matches = RegExUtil.findAll("Contact: john@example.com, jane@test.org", pattern);
     * // matches contains: ["john@example.com", "jane@test.org"]
     * }</pre>
     *
     * @param source the string to be searched, may be {@code null} or empty
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return a list containing the matched text of each occurrence, in order of appearance;
     *         an empty list is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see #matchResults(String, Pattern)
     * @see #findFirst(String, Pattern)
     */
    public static List<String> findAll(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        final List<String> result = new ArrayList<>();

        if (source == null) {
            return result;
        }

        final Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    /**
     * Removes the first substring of the source string that matches the given regular expression.
     * This is equivalent to {@code replaceFirst(source, regex, "")}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeFirst("Hello123World456", "\\d+");
     * // Returns: "HelloWorld456"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the first match removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceFirst(String, String, String)
     * @see String#replaceFirst(String, String)
     * @see java.util.regex.Pattern
     */
    public static String removeFirst(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        return replaceFirst(source, regex, Strings.EMPTY);
    }

    /**
     * Removes the first substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceFirst(source, pattern, "")}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeFirst("Hello   World   !", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "HelloWorld   !"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return the source string with the first match removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeLast("Hello123World456", "\\d+");
     * // Returns: "Hello123World"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with the last match removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceLast(String, String, String)
     * @see java.util.regex.Pattern
     */
    @Beta
    public static String removeLast(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        return replaceLast(source, regex, Strings.EMPTY);
    }

    /**
     * Removes the last substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceLast(source, pattern, "")}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeLast("Hello   World   !", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "Hello   World!"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return the source string with the last match removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeAll("Hello123World456", "\\d+");
     * // Returns: "HelloWorld"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @return the source string with any matching substrings removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceAll(String, String, String)
     * @see String#replaceAll(String, String)
     */
    public static String removeAll(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        return replaceAll(source, regex, Strings.EMPTY);
    }

    /**
     * Removes each substring of the source string that matches the given regular expression pattern.
     * This is equivalent to {@code replaceAll(source, pattern, "")}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.removeAll("Hello   World", RegExUtil.WHITESPACE_FINDER);
     * // Returns: "HelloWorld"
     * }</pre>
     *
     * @param source source string to remove from, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return the source string with any matching substrings removed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see #replaceAll(String, Pattern, String)
     * @see java.util.regex.Matcher#replaceAll(String)
     */
    public static String removeAll(final String source, final Pattern pattern) throws IllegalArgumentException {
        return replaceAll(source, pattern, Strings.EMPTY);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression with the given replacement.
     *
     * <p><b>Note:</b> The {@code replacement} string is interpreted by {@link java.util.regex.Matcher#replaceFirst(String)};
     * a dollar sign ({@code $}) followed by a digit denotes a back-reference to a capturing group (e.g. {@code $1}),
     * and a backslash ({@code \}) escapes the following character. To use a literal {@code $} or {@code \}, escape it
     * with a preceding backslash, or use {@link #replaceFirst(String, String, Function)} which treats its result literally.
     * (This differs from {@link #replaceLast(String, String, String)}, whose replacement is always literal.)</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("Hello123World456", "\\d+", "XXX");
     * // Returns: "HelloXXXWorld456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     *        (group references such as {@code $1} are interpreted; see note above)
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty, or if a match is found
     *         and {@code replacement} is malformed: a {@code $} followed by neither a digit nor a well-formed
     *         {@code {name}} (a trailing {@code $} included), a {@code ${name}} naming a group {@code regex} does not
     *         declare, or a trailing {@code \}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @throws IndexOutOfBoundsException if a match is found and {@code replacement} references a capturing group that {@code regex} does not have.
     * @see String#replaceFirst(String, String)
     */
    public static String replaceFirst(final String source, final String regex, final String replacement)
            throws IllegalArgumentException, PatternSyntaxException, IndexOutOfBoundsException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceFirst(source, Pattern.compile(regex), Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression
     * with the result of applying the given function to the matched substring.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("hello world", "\\b\\w", match -> match.toUpperCase());
     * // Returns: "Hello world"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer the non-null function applied to the matched substring; a {@code null} result removes the match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceFirst(String, Pattern, Function)
     */
    public static String replaceFirst(final String source, final String regex, final Function<String, String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression
     * with the result of applying the given function to the start and end indices of the match.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceFirst("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer the non-null function applied to the match's start and end indices; a {@code null} result removes the match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceFirst(String, Pattern, IntBiFunction)
     */
    public static String replaceFirst(final String source, final String regex, final IntBiFunction<String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceFirst(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern with the given replacement.
     * This method is more efficient than {@link #replaceFirst(String, String, String)} when using the same pattern multiple times.
     *
     * <p><b>Note:</b> The {@code replacement} string is interpreted by {@link java.util.regex.Matcher#replaceFirst(String)};
     * a dollar sign ({@code $}) followed by a digit denotes a back-reference to a capturing group (e.g. {@code $1}),
     * and a backslash ({@code \}) escapes the following character. To use a literal {@code $} or {@code \}, escape it
     * with a preceding backslash, or use {@link #replaceFirst(String, Pattern, Function)} which treats its result literally.
     * (This differs from {@link #replaceLast(String, Pattern, String)}, whose replacement is always literal.)</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceFirst("Hello123World456", pattern, "XXX");
     * // Returns: "HelloXXXWorld456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for the first match
     *        (group references such as {@code $1} are interpreted; see note above)
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}, or if a match is found
     *         and {@code replacement} is malformed: a {@code $} followed by neither a digit nor a well-formed
     *         {@code {name}} (a trailing {@code $} included), a {@code ${name}} naming a group {@code pattern} does not
     *         declare, or a trailing {@code \}.
     * @throws IndexOutOfBoundsException if a match is found and {@code replacement} references a capturing group that {@code pattern} does not have.
     * @see java.util.regex.Matcher#replaceFirst(String)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final String replacement)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return Strings.EMPTY;
        }

        return pattern.matcher(source).replaceFirst(Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the matched substring.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w");
     * String result = RegExUtil.replaceFirst("hello world", pattern, match -> match.toUpperCase());
     * // Returns: "Hello world"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @param replacer the non-null function applied to the matched substring; a {@code null} result removes the match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see java.util.regex.Matcher#replaceFirst(java.util.function.Function)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        // quoteReplacement: the function result is a literal replacement, not a template -
        // unquoted '$'/'\' in it would be (mis)interpreted as group references/escapes.
        return pattern.matcher(source)
                .replaceFirst(matcher -> Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(source.substring(matcher.start(), matcher.end())))));
    }

    /**
     * Replaces the first substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the start and end indices of the match.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceFirst("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def456"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @param replacer the non-null function applied to the match's start and end indices; a {@code null} result removes the match
     * @return the source string with the first replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see #replaceFirst(String, String, IntBiFunction)
     */
    public static String replaceFirst(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        // quoteReplacement: the function result is a literal replacement, not a template.
        return pattern.matcher(source).replaceFirst(matcher -> Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(matcher.start(), matcher.end()))));
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacement}.
     * This method finds the rightmost match in the string and replaces only that occurrence.
     * Note that, unlike {@link #replaceFirst(String, String, String)} and {@link #replaceAll(String, String, String)},
     * the replacement is treated as a literal string: group references such as {@code $1} are not interpreted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("Hello123World456", "\\d+", "XXX");
     * // Returns: "Hello123WorldXXX"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param regex the regular expression pattern to search for; must not be {@code null} or empty
     * @param replacement the replacement string
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceLast(String, Pattern, String)
     * @see #replaceFirst(String, String, String)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final String replacement)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceLast(source, Pattern.compile(regex), replacement);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacer}.
     * The replacer function receives the matched substring and returns the replacement string.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("hello world hello", "hello", match -> match.toUpperCase());
     * // Returns: "hello world HELLO"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param regex the regular expression pattern to search for; must not be {@code null} or empty
     * @param replacer the non-null function applied to the matched substring; a {@code null} result removes the match
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceLast(String, Pattern, Function)
     * @see #replaceFirst(String, String, Function)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final Function<String, String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacer}.
     * The replacer function receives the start and end indices of the match and returns the replacement string.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceLast("abc123def456", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc123def[9-12]"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param regex the regular expression pattern to search for; must not be {@code null} or empty
     * @param replacer the non-null function applied to the match's start and end indices; a {@code null} result removes the match
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceLast(String, Pattern, IntBiFunction)
     * @see #replaceFirst(String, String, IntBiFunction)
     */
    @Beta
    public static String replaceLast(final String source, final String regex, final IntBiFunction<String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceLast(source, Pattern.compile(regex), replacer);
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacement}.
     * Note that, unlike {@link #replaceFirst(String, Pattern, String)} and {@link #replaceAll(String, Pattern, String)},
     * the replacement is treated as a literal string: group references such as {@code $1} are not interpreted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceLast("Hello123World456", pattern, "XXX");
     * // Returns: "Hello123WorldXXX"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param pattern the pre-compiled regular expression pattern to search for; must not be {@code null}
     * @param replacement the replacement string
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see #replaceLast(String, String, String)
     * @see #replaceFirst(String, Pattern, String)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final String replacement) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(source);
        int start = -1;
        int end = -1;

        // Forward scan for the LAST match (same iteration semantics as findLast): reverse find(i)
        // probing could land mid-match and replace a shorter sub-match that forward iteration
        // would never report (e.g. "(ab)+" on "ababab" replaced only the trailing "ab").
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }

        if (start >= 0) {
            return Strings.replaceRange(source, start, end, replacement);
        }

        return source;
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacer}.
     * The replacer function receives the matched substring and returns the replacement string.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("hello");
     * String result = RegExUtil.replaceLast("hello world hello", pattern, match -> match.toUpperCase());
     * // Returns: "hello world HELLO"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param pattern the pre-compiled regular expression pattern to search for; must not be {@code null}
     * @param replacer the non-null function applied to the matched substring; a {@code null} result removes the match
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see #replaceLast(String, String, Function)
     * @see #replaceFirst(String, Pattern, Function)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(source);
        int start = -1;
        int end = -1;

        // Forward scan for the LAST match (same iteration semantics as findLast).
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }

        if (start >= 0) {
            return Strings.replaceRange(source, start, end, replacer.apply(source.substring(start, end)));
        }

        return source;
    }

    /**
     * Searches for the last occurrence of the specified {@code regex} pattern in the specified source string, and replaces it with the specified {@code replacer}.
     * The replacer function receives the start and end indices of the match and returns the replacement string.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceLast("abc123def456", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc123def[9-12]"
     * }</pre>
     *
     * @param source the source string to search in, which may be null
     * @param pattern the pre-compiled regular expression pattern to search for; must not be {@code null}
     * @param replacer the non-null function applied to the match's start and end indices; a {@code null} result removes the match
     * @return the source string with the last replacement processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see #replaceLast(String, String, IntBiFunction)
     * @see #replaceFirst(String, Pattern, IntBiFunction)
     */
    @Beta
    public static String replaceLast(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        final Matcher matcher = pattern.matcher(source);
        int start = -1;
        int end = -1;

        // Forward scan for the LAST match (same iteration semantics as findLast).
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
        }

        if (start >= 0) {
            return Strings.replaceRange(source, start, end, replacer.apply(start, end));
        }

        return source;
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the given replacement.
     *
     * This method is a {@code null} safe equivalent to
     * {@code source.replaceAll(regex, replacement)} for a non-{@code null} {@code source}.
     *
     * <p>A {@code null} source short-circuits to {@code ""}; it is <b>not</b> treated as the empty string and
     * then matched, so this is <i>not</i> equivalent to
     * {@code Pattern.compile(regex).matcher(Strings.nullToEmpty(source)).replaceAll(replacement)} - that form
     * would let a pattern matching the empty input produce output, e.g.
     * {@code replaceAll(null, "a*", "X")} is {@code ""} here and {@code "X"} there. This follows the
     * "null vs. empty source" rule stated in the class javadoc.</p>
     *
     * <p><b>Note:</b> The {@code replacement} string is interpreted by {@link java.util.regex.Matcher#replaceAll(String)};
     * a dollar sign ({@code $}) followed by a digit denotes a back-reference to a capturing group (e.g. {@code $1}),
     * and a backslash ({@code \}) escapes the following character. To use a literal {@code $} or {@code \}, escape it
     * with a preceding backslash, or use {@link #replaceAll(String, String, Function)} which treats its result literally.
     * (This differs from {@link #replaceLast(String, String, String)}, whose replacement is always literal.)</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("Hello   World", "\\s+", " ");
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacement the string to be substituted for each match
     *        (group references such as {@code $1} are interpreted; see note above)
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty, or if a match is found
     *         and {@code replacement} is malformed: a {@code $} followed by neither a digit nor a well-formed
     *         {@code {name}} (a trailing {@code $} included), a {@code ${name}} naming a group {@code regex} does not
     *         declare, or a trailing {@code \}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @throws IndexOutOfBoundsException if a match is found and {@code replacement} references a capturing group that {@code regex} does not have.
     * @see String#replaceAll(String, String)
     */
    public static String replaceAll(final String source, final String regex, final String replacement)
            throws IllegalArgumentException, PatternSyntaxException, IndexOutOfBoundsException {
        N.checkArgNotEmpty(regex, cs.regex);

        return replaceAll(source, Pattern.compile(regex), Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the result of applying the given function to the matched substring.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("hello world", "\\b\\w", match -> match.toUpperCase());
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer the non-null function applied to each matched substring; a {@code null} result removes that match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceAll(String, Pattern, Function)
     */
    public static String replaceAll(final String source, final String regex, final Function<String, String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression
     * with the result of applying the given function to the start and end indices of the match.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = RegExUtil.replaceAll("abc123def", "\\d+", (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param regex the regular expression to which this string is to be matched
     * @param replacer the non-null function applied to each match's start and end indices; a {@code null} result removes that match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code regex} is {@code null} or empty, or if {@code replacer} is {@code null}.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #replaceAll(String, Pattern, IntBiFunction)
     */
    public static String replaceAll(final String source, final String regex, final IntBiFunction<String> replacer)
            throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);
        N.checkArgNotNull(replacer, cs.replacer);

        return replaceAll(source, Pattern.compile(regex), replacer);
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern with the given replacement.
     * This method is more efficient than {@link #replaceAll(String, String, String)} when using the same pattern multiple times.
     *
     * <p><b>Note:</b> The {@code replacement} string is interpreted by {@link java.util.regex.Matcher#replaceAll(String)};
     * a dollar sign ({@code $}) followed by a digit denotes a back-reference to a capturing group (e.g. {@code $1}),
     * and a backslash ({@code \}) escapes the following character. To use a literal {@code $} or {@code \}, escape it
     * with a preceding backslash, or use {@link #replaceAll(String, Pattern, Function)} which treats its result literally.
     * (This differs from {@link #replaceLast(String, Pattern, String)}, whose replacement is always literal.)</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\s+");
     * String result = RegExUtil.replaceAll("Hello   World", pattern, " ");
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the regular expression pattern to which this string is to be matched
     * @param replacement the string to be substituted for each match
     *        (group references such as {@code $1} are interpreted; see note above)
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if the pattern is {@code null}, or if a match is found
     *         and {@code replacement} is malformed: a {@code $} followed by neither a digit nor a well-formed
     *         {@code {name}} (a trailing {@code $} included), a {@code ${name}} naming a group {@code pattern} does not
     *         declare, or a trailing {@code \}.
     * @throws IndexOutOfBoundsException if a match is found and {@code replacement} references a capturing group that {@code pattern} does not have.
     * @see java.util.regex.Matcher#replaceAll(String)
     * @see java.util.regex.Pattern
     */
    public static String replaceAll(final String source, final Pattern pattern, final String replacement)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return Strings.EMPTY;
        }

        return pattern.matcher(source).replaceAll(Strings.nullToEmpty(replacement));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the matched substring.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w");
     * String result = RegExUtil.replaceAll("hello world", pattern, match -> match.toUpperCase());
     * // Returns: "Hello World"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @param replacer the non-null function applied to each matched substring; a {@code null} result removes that match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see #replaceAll(String, String, Function)
     * @see java.util.regex.Matcher#replaceAll(java.util.function.Function)
     */
    public static String replaceAll(final String source, final Pattern pattern, final Function<String, String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        // quoteReplacement: the function result is a literal replacement, not a template -
        // unquoted '$'/'\' in it would be (mis)interpreted as group references/escapes.
        return pattern.matcher(source)
                .replaceAll(matcher -> Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(source.substring(matcher.start(), matcher.end())))));
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression pattern
     * with the result of applying the given function to the start and end indices of the match.
     * The string returned by the replacer is used as a literal replacement: dollar signs and backslashes
     * in it are not treated as group references or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * String result = RegExUtil.replaceAll("abc123def", pattern, (start, end) -> "[" + start + "-" + end + "]");
     * // Returns: "abc[3-6]def"
     * }</pre>
     *
     * @param source source string to search and replace in, which may be null
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @param replacer the non-null function applied to each match's start and end indices; a {@code null} result removes that match
     * @return the source string with any replacements processed, or an empty String {@code ""} if the input source string is {@code null}.
     * @throws IllegalArgumentException if {@code pattern} or {@code replacer} is {@code null}.
     * @see #replaceAll(String, String, IntBiFunction)
     */
    public static String replaceAll(final String source, final Pattern pattern, final IntBiFunction<String> replacer) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);
        N.checkArgNotNull(replacer, cs.replacer);

        if (source == null) {
            return Strings.EMPTY;
        }

        // quoteReplacement: the function result is a literal replacement, not a template.
        return pattern.matcher(source).replaceAll(matcher -> Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(matcher.start(), matcher.end()))));
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     *
     * <p><b>Usage Examples:</b></p>
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
     * @return the number of occurrences of the specified pattern in the string, or 0 if the input source string is {@code null}
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see #countMatches(String, Pattern)
     */
    public static int countMatches(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return countMatches(source, Pattern.compile(regex));
    }

    /**
     * Counts the number of occurrences of the specified pattern in the given string.
     * This method is more efficient than {@link #countMatches(String, String)} when using the same pattern multiple times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * int count = RegExUtil.countMatches("abc123def456ghi789", pattern);
     * // Returns: 3
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the regular expression pattern to be counted
     * @return the number of occurrences of the specified pattern in the string, or 0 if the input source string is {@code null}
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see #countMatches(String, String)
     */
    public static int countMatches(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
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
     * Returns a stream of {@link MatchResult} objects representing each match found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<MatchResult> matches = RegExUtil.matchResults("abc123def456", "\\d+");
     * matches.forEach(match -> System.out.println(match.group()));
     * // Prints: 123, 456
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression to match against; must not be {@code null} or empty
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern;
     *         an empty stream is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return matchResults(source, Pattern.compile(regex));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string.
     * Returns a stream of {@link MatchResult} objects representing each match found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\b\\w+@\\w+\\.\\w+\\b");
     * Stream<MatchResult> matches = RegExUtil.matchResults("Contact: john@example.com, jane@test.org", pattern);
     * matches.forEach(match -> System.out.println(match.group()));
     * // Prints: john@example.com, jane@test.org
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return a stream of match results for each subsequence of the input sequence that matches the pattern;
     *         an empty stream is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see Matcher#results()
     */
    public static Stream<MatchResult> matchResults(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return Stream.empty();
        }

        return pattern.matcher(source).results();
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     * This is useful when you need to know the positions where matches occur in the string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntStream indices = RegExUtil.matchIndices("Hello World", "l");
     * // Returns stream of: 2, 3, 9 (positions of 'l' in the string)
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param regex the regular expression to match against; must not be {@code null} or empty
     * @return a stream of start indices for each subsequence of the input sequence that matches the pattern;
     *         an empty stream is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see Matcher#results()
     * @see Strings#indicesOf(String, String)
     * @see Strings#indicesOf(String, String, int)
     */
    public static IntStream matchIndices(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return matchIndices(source, Pattern.compile(regex));
    }

    /**
     * Finds all the occurrences of the specified pattern in the given string and returns a stream of start indices.
     * This is useful when you need to know the positions where matches occur in the string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\d+");
     * IntStream indices = RegExUtil.matchIndices("abc123def456ghi", pattern);
     * // Returns stream of: 3, 9 (starting positions of number sequences)
     * }</pre>
     *
     * @param source the string to be checked, may be {@code null} or empty
     * @param pattern the compiled regular expression pattern to match against; must not be {@code null}
     * @return a stream of start indices for each subsequence of the input sequence that matches the pattern;
     *         an empty stream is returned if the input source string is {@code null}
     * @throws IllegalArgumentException if the pattern is {@code null}.
     * @see Matcher#results()
     * @see Strings#indicesOf(String, String)
     * @see Strings#indicesOf(String, String, int)
     */
    public static IntStream matchIndices(final String source, final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern, cs.pattern);

        if (source == null) {
            return IntStream.empty();
        }

        return pattern.matcher(source).results().mapToInt(MatchResult::start);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] parts = RegExUtil.split("one,two,three", ",");
     * // Returns: ["one", "two", "three"]
     *
     * String[] words = RegExUtil.split("Hello   World", "\\s+");
     * // Returns: ["Hello", "World"]
     *
     * String[] trailing = RegExUtil.split("a,b,,", ",");
     * // Returns: ["a", "b"] - trailing empty strings are discarded
     * }</pre>
     *
     * <p>This overload splits at {@code limit == 0}, so <b>trailing empty strings are removed</b>; call
     * {@link #split(String, String, int)} with a negative limit to keep them.</p>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param regex the regular expression to split by
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see String#split(String)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final String regex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return split(source, Pattern.compile(regex));
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression with a limit.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * The limit parameter controls the number of times the pattern is applied and therefore affects the length of the resulting array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] parts = RegExUtil.split("one,two,three,four", ",", 3);
     * // Returns: ["one", "two", "three,four"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param regex the regular expression to split by
     * @param limit the result threshold. A positive value limits the result length; zero discards trailing empty strings;
     *              a negative value preserves trailing empty strings. Both zero and negative values allow unlimited splits.
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the {@code regex} is {@code null} or empty.
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     * @see String#split(String, int)
     * @see Splitter#with(Pattern)
     * @see Splitter#split(CharSequence)
     */
    public static String[] split(final String source, final String regex, final int limit) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(regex, cs.regex);

        return split(source, Pattern.compile(regex), limit);
    }

    /**
     * Splits the given string into an array of strings based on the specified regular expression pattern.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * This method is more efficient than {@link #split(String, String)} when using the same pattern multiple times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile("\\s+");
     * String[] words = RegExUtil.split("Hello   World   Java", pattern);
     * // Returns: ["Hello", "World", "Java"]
     * }</pre>
     *
     * <p>This overload splits at {@code limit == 0}, so <b>trailing empty strings are removed</b> - for example
     * {@code split("a,b,,", Pattern.compile(","))} is {@code ["a", "b"]}; call
     * {@link #split(String, Pattern, int)} with a negative limit to keep them.</p>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param pattern the regular expression pattern to split by
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the pattern is {@code null}.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern pattern = Pattern.compile(",");
     * String[] parts = RegExUtil.split("a,b,c,d", pattern, 3);
     * // Returns: ["a", "b", "c,d"]
     * }</pre>
     *
     * @param source the string to be split, may be {@code null} or empty
     * @param pattern the regular expression pattern to split by
     * @param limit the result threshold. A positive value limits the result length; zero discards trailing empty strings;
     *              a negative value preserves trailing empty strings. Both zero and negative values allow unlimited splits.
     * @return an array of strings computed by splitting the source string around matches of the given regular expression.
     *         An empty array is returned if the input source string is {@code null}, or an array containing an empty string if the input source string is empty
     * @throws IllegalArgumentException if the pattern is {@code null}.
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
     * Splits the given string into an array of lines using {@link #LINE_SEPARATOR}.
     * If the string is {@code null}, an empty array is returned. If the string is empty, an array containing an empty string is returned.
     * This method handles all types of line breaks (CR, LF, CRLF, etc.) across different platforms.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\r\nLine 3";
     * String[] lines = RegExUtil.splitToLines(text);
     * // Returns: ["Line 1", "Line 2", "Line 3"]
     * }</pre>
     *
     * <p>This overload splits at {@code limit == 0}, so <b>trailing empty lines are removed</b>: a source that is
     * nothing but line terminators, such as {@code "\n"}, yields a <i>zero-length</i> array, and
     * {@code splitToLines("a\nb\n")} is {@code ["a", "b"]}. Call {@link #splitToLines(String, int)} with a
     * negative limit to keep the trailing empties.</p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\nLine 3\nLine 4";
     * String[] lines = RegExUtil.splitToLines(text, 3);
     * // Returns: ["Line 1", "Line 2", "Line 3\nLine 4"]
     * }</pre>
     *
     * @param source the string to be split into lines, may be {@code null} or empty
     * @param limit the result threshold. A positive value limits the result length; zero discards trailing empty strings;
     *              a negative value preserves trailing empty strings. Both zero and negative values allow unlimited splits.
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
