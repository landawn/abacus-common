/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.regex.PatternSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * A flexible string splitting utility that divides strings into parts based on configurable delimiters
 * and patterns. This final class provides a fluent builder-pattern API for parsing text with extensive
 * customization options including whitespace handling, empty string management, result limiting, and
 * type conversion.
 *
 * <p>Splitter supports multiple delimiter types including single characters, multi-character strings,
 * and regular expression patterns. It provides sophisticated preprocessing options such as trimming
 * whitespace, stripping Unicode whitespace, omitting empty results, and limiting the number of splits.
 * The class integrates seamlessly with the Stream API and Collections Framework for functional
 * programming patterns and efficient data processing pipelines.</p>
 *
 * <p>Configuration methods mutate this splitter and return {@code this}; instances are not
 * thread-safe while being configured. Complete configuration before sharing an instance, and do
 * not mutate a source {@link CharSequence} while consuming an iterator or stream derived from it.</p>
 *
 * <p><b>Configuration methods return {@code this}, not a copy.</b> {@code omitEmptyStrings()},
 * {@code trimResults()}, {@code stripResults()} and {@code limit(int)} reconfigure the receiver in place, so
 * every holder of that instance sees the change. Do not derive a "variant" from a shared splitter:</p>
 * <pre>{@code
 * Splitter base = Splitter.with(',');
 * Splitter trimmed = base.trimResults();
 * // trimmed == base -- there is no second splitter, and base now trims too.
 * }</pre>
 * <p>Configure each splitter in a single fluent chain from its factory method instead.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Multiple Delimiter Types:</b> Characters, strings, and regex patterns for flexible parsing</li>
 *   <li><b>Whitespace Handling:</b> Built-in trimming and Unicode-aware whitespace stripping</li>
 *   <li><b>Empty String Management:</b> Option to omit empty results from split operations</li>
 *   <li><b>Result Limiting:</b> Control maximum number of splits with configurable limits</li>
 *   <li><b>Type Conversion:</b> Direct conversion to target types with Type/Class support</li>
 *   <li><b>Stream Integration:</b> Lazy evaluation with Stream API for memory-efficient processing</li>
 *   <li><b>Collection Flexibility:</b> Output to any Collection type with custom suppliers</li>
 *   <li><b>Map Parsing:</b> Specialized MapSplitter for key-value pair extraction</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Simple delimiter-separated text:</b> Formats that do not require CSV-style quoting or escaping</li>
 *   <li><b>Configuration Parsing:</b> Property files, command-line arguments, and settings</li>
 *   <li><b>Log Analysis:</b> Extracting fields from structured log entries</li>
 *   <li><b>Data Import/Export:</b> Converting between string formats and structured data</li>
 *   <li><b>Text Processing:</b> Natural language processing and document analysis</li>
 *   <li><b>Protocol Parsing:</b> Network protocols and structured message formats</li>
 *   <li><b>Template Processing:</b> Extracting components from templated strings</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic string splitting
 * List<String> parts = Splitter.with(",").split("apple,banana,cherry");
 * // Result: ["apple", "banana", "cherry"]
 *
 * // Advanced configuration with preprocessing
 * List<String> cleaned = Splitter.with(",")
 *     .trimResults()
 *     .omitEmptyStrings()
 *     .split("  apple,  , banana , cherry  ");
 * // Result: ["apple", "banana", "cherry"]
 *
 * // Limited splitting with pattern delimiter
 * List<String> limited = Splitter.pattern("\\s+")
 *     .limit(3)
 *     .split("one two three four five");
 * // Result: ["one", "two", "three four five"]
 *
 * // Type conversion with target classes
 * List<Integer> numbers = Splitter.with(";")
 *     .split("1;2;3;4", Integer.class);
 * // Result: [1, 2, 3, 4]
 *
 * // Stream processing (the same pipeline also scales to large inputs)
 * String text = "INFO ready\nERROR failed";
 * long count = Splitter.with("\n")
 *     .splitToStream(text)
 *     .filter(line -> !line.isEmpty())
 *     .map(String::trim)
 *     .filter(line -> line.startsWith("ERROR"))
 *     .count();
 *
 * // Map parsing with key-value pairs
 * Map<String, Integer> config = Splitter.MapSplitter
 *     .with(";", "=")
 *     .trimResults()
 *     .split("timeout=30; retries=3; buffer=1024", String.class, Integer.class);
 * // Result: {timeout=30, retries=3, buffer=1024}
 * }</pre>
 *
 * <p><b>Factory Methods:</b>
 * <ul>
 *   <li>{@link #withDefault()} - Default comma-and-space splitting using {@link #DEFAULT_DELIMITER}</li>
 *   <li>{@link #forLines()} - Line-based splitting for text processing</li>
 *   <li>{@link #with(char)} - Single character delimiter</li>
 *   <li>{@link #with(CharSequence)} - Multi-character string delimiter</li>
 *   <li>{@link #with(Pattern)} - Regular expression pattern delimiter</li>
 *   <li>{@link #pattern(CharSequence)} - Create pattern from regex string</li>
 * </ul>
 *
 * <p><b>Configuration Options:</b>
 * <ul>
 *   <li>{@link #omitEmptyStrings()} - Skip empty results in output</li>
 *   <li>{@link #trimResults()} - Remove leading/trailing space characters only</li>
 *   <li>{@link #stripResults()} - Remove leading/trailing whitespace per {@link Character#isWhitespace(char)}</li>
 *   <li>{@link #limit(int)} - Limit maximum number of splits performed</li>
 * </ul>
 *
 * <p><b>Output Methods:</b>
 * <ul>
 *   <li><b>Lists:</b> {@code split()}, {@code splitToImmutableList()}</li>
 *   <li><b>Arrays:</b> {@code splitToArray()}, with type conversion support</li>
 *   <li><b>Custom Collections:</b> {@code splitToCollection(source, supplier)} with Collection suppliers</li>
 *   <li><b>Existing containers:</b> {@code splitInto(source, output)} for a Collection, Map, or {@code String[]}</li>
 *   <li><b>Streams:</b> {@code splitToStream()} for lazy evaluation and functional processing</li>
 *   <li><b>Type Conversion:</b> {@code split(source, targetType)} with Class or Type parameters</li>
 * </ul>
 *
 * <p><b>Advanced Operations:</b>
 * <ul>
 *   <li>{@link #splitThenApply(CharSequence, Function)} - Split and transform in one operation</li>
 *   <li>{@link #splitThenAccept(CharSequence, Consumer)} - Split and process with side effects</li>
 *   <li>{@link #splitThenForEach(CharSequence, Consumer)} - Lazy per-element processing</li>
 * </ul>
 *
 * <p><b>Delimiter Types and Behavior:</b>
 * <ul>
 *   <li><b>Character Delimiter:</b> Fast single-character splitting with O(n) performance</li>
 *   <li><b>String Delimiter:</b> Multi-character literal string matching</li>
 *   <li><b>Pattern Delimiter:</b> Full regex support with capturing groups and lookarounds</li>
 *   <li><b>Whitespace Splitting:</b> pass the ready-made {@link #WHITE_SPACE_PATTERN} to {@link #with(Pattern)}
 *       for Unicode-aware whitespace handling</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li>Character splitting: O(n) time, O(k) space where n is input length, k is result count</li>
 *   <li>String splitting: O(n*m) time where m is delimiter length</li>
 *   <li>Pattern splitting: runtime depends on the supplied {@link Pattern}, including any regex backtracking</li>
 *   <li>Memory usage: Lazy evaluation reduces memory footprint for stream operations</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * Splitter instances are safe to reuse across threads after configuration is complete and no further
 * configuration methods are invoked:
 * <ul>
 *   <li>Configuration methods mutate the instance and are not safe for concurrent use</li>
 *   <li>Once configured, split operations read the configured state without mutating it</li>
 *   <li>A configured instance can be shared between threads for read-only use <b>provided it is safely
 *       published</b> &mdash; the configuration fields are neither {@code final} nor {@code volatile}, so a
 *       reader thread that obtains the instance through a data race may observe the unconfigured defaults.
 *       Publishing it through a {@code static final} field, a {@code final} instance field written in a
 *       constructor, or any other action that establishes a happens-before edge is sufficient</li>
 *   <li>Because configuration mutates in place, a shared instance must not be reconfigured after publication</li>
 * </ul>
 *
 * <p><b>Whitespace Handling Details:</b>
 * <ul>
 *   <li><b>trimResults():</b> Removes only the space character (' ') from the start and end</li>
 *   <li><b>stripResults():</b> Removes leading and trailing whitespace as defined by {@link Character#isWhitespace(char)}</li>
 *   <li><b>Both methods:</b> Applied after splitting but before empty string filtering</li>
 *   <li><b>Unicode Support:</b> to <i>split on</i> Unicode whitespace, use
 *       {@code Splitter.with(Splitter.WHITE_SPACE_PATTERN)}</li>
 * </ul>
 *
 * <p><b>Type Conversion Support:</b>
 * <ul>
 *   <li>Automatic conversion using {@link com.landawn.abacus.type.Type} system</li>
 *   <li>Support for primitives, wrapper types, collections, and custom types</li>
 *   <li>Error handling for invalid conversions with descriptive exceptions</li>
 *   <li>Null handling according to target type nullability</li>
 * </ul>
 *
 * <p><b>MapSplitter Integration:</b>
 * The nested {@link MapSplitter} class provides specialized functionality for parsing
 * key-value pair strings:
 * <pre>{@code
 * Map<String, String> properties = Splitter.MapSplitter
 *     .with(",", "=")
 *     .split("name=John,age=30,city=NYC");
 * }</pre>
 *
 * <p><b>Stream Integration Patterns:</b>
 * <ul>
 *   <li><b>Lazy Evaluation:</b> {@code splitToStream()} for memory-efficient processing</li>
 *   <li><b>Parallel Processing:</b> Convert to parallel streams for CPU-intensive operations</li>
 *   <li><b>Pipeline Composition:</b> Chain with other stream operations for complex transformations</li>
 *   <li><b>Collector Integration:</b> Use with custom collectors for specialized aggregations</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li>Throws {@link IllegalArgumentException} for invalid configuration parameters</li>
 *   <li>Throws {@link IllegalArgumentException} for {@code null} required parameters</li>
 *   <li>Handles {@code null} input strings gracefully (returns empty results)</li>
 *   <li>Type conversion errors propagate with descriptive messages</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>A fully configured Splitter may be cached and reused, but never reconfigure a cached instance &mdash;
 *       configuration mutates it in place for every holder</li>
 *   <li>Use {@code splitToStream()} for large inputs to minimize memory usage</li>
 *   <li>Complete all configuration before publishing an instance for concurrent read-only use</li>
 *   <li>Use appropriate delimiter types based on parsing requirements</li>
 *   <li>Consider {@code limit()} for performance when only first few splits are needed</li>
 * </ul>
 *
 * <p><b>Integration Points:</b>
 * <ul>
 *   <li><b>{@link Joiner}:</b> Complementary class for string joining operations</li>
 *   <li><b>{@link Stream}:</b> Functional programming and lazy evaluation support</li>
 *   <li><b>{@link Type}:</b> Type system integration for automatic conversions</li>
 *   <li><b>Collections Framework:</b> Full compatibility with all collection types</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li>Streaming operations minimize memory footprint</li>
 *   <li>Reusing configured instances avoids repeated setup allocations</li>
 *   <li>Lazy evaluation defers allocation until needed</li>
 *   <li>Consider streaming for very large input texts</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs String.split():</b> More configuration options and type safety</li>
 *   <li><b>vs Pattern.split():</b> Fluent API and additional preprocessing options</li>
 *   <li><b>vs StringTokenizer:</b> Modern API with functional programming support</li>
 *   <li><b>vs Google Guava Splitter:</b> Similar API with additional type conversion features</li>
 * </ul>
 *
 * <p><b>Constants:</b>
 * <ul>
 *   <li>{@link #WHITE_SPACE_PATTERN} - Compiled regex for Unicode whitespace matching, to be passed to
 *       {@link #with(Pattern)}</li>
 * </ul>
 *
 * @see Joiner
 * @see MapSplitter
 * @see Pattern
 * @see Stream
 * @see Type
 * @see String#split(String)
 * @see java.util.StringTokenizer
 */
@SuppressWarnings("java:S1192")
public final class Splitter {

    /**
     * The default delimiter ({@code ", "}) used to split delimited text when {@link #withDefault()} is used.
     * @see Joiner#DEFAULT_DELIMITER
     */
    public static final String DEFAULT_DELIMITER = Joiner.DEFAULT_DELIMITER;

    /**
     * The default delimiter ({@code "="}) used to separate keys and values in key-value pairs
     * when {@link MapSplitter#withDefault()} is used.
     * @see Joiner#DEFAULT_KEY_VALUE_DELIMITER
     */
    public static final String DEFAULT_KEY_VALUE_DELIMITER = Joiner.DEFAULT_KEY_VALUE_DELIMITER;

    /**
     * A compiled regular expression pattern that matches one or more whitespace characters.
     * Uses Unicode character class to properly handle all Unicode whitespace characters.
     */
    public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final SubstringFunc defaultSubstringFunc = (source, start, end) -> source.subSequence(start, end).toString();

    private static final SubstringFunc trimSubstringFunc = (source, start, end) -> {
        while (start < end && source.charAt(start) == ' ') {
            start++;
        }

        while (end > start && source.charAt(end - 1) == ' ') {
            end--;
        }

        return start >= end ? Strings.EMPTY : source.subSequence(start, end).toString();
    };

    private static final SubstringFunc stripSubstringFunc = (source, start, end) -> {
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        return start >= end ? Strings.EMPTY : source.subSequence(start, end).toString();
    };

    private final Strategy strategy;
    private boolean omitEmptyStrings = false;
    private boolean trimResults = false;
    private boolean stripResults = false;
    private int limit = Integer.MAX_VALUE;

    /**
     * Creates a splitter backed by the specified splitting strategy.
     *
     * @param strategy the strategy used to locate separators
     */
    Splitter(final Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns a new Splitter instance configured with the default delimiter: ", " (comma followed by space).
     * This delimiter is useful for simple comma-and-space-separated lists. It does not implement
     * CSV quoting or escaping rules.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> parts = Splitter.withDefault().split("apple, banana, cherry");
     * // Returns ["apple", "banana", "cherry"]
     * }</pre>
     *
     * @return a new Splitter instance configured with the default delimiter ", ".
     * @see #with(CharSequence)
     * @see #forLines()
     * @see Joiner#withDefault()
     */
    @Beta
    public static Splitter withDefault() {
        return with(DEFAULT_DELIMITER);
    }

    /**
     * Returns a new Splitter instance configured to split text by line separators.
     * Splitting uses {@link RegExUtil#LINE_SEPARATOR}, that is the regex {@code \R}, so it recognizes every
     * Unicode line terminator: {@code \n}, {@code \r}, {@code \r\n} (as a single separator), and also
     * the vertical tab {@code U+000B}, the form feed {@code U+000C}, the next-line character {@code U+0085},
     * the line separator {@code U+2028} and the paragraph separator {@code U+2029}.
     * Useful for splitting multi-line text into individual lines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String multiLine = "line1\nline2\r\nline3\rline4";
     * List<String> lines = Splitter.forLines().split(multiLine);
     * // Returns ["line1", "line2", "line3", "line4"]
     * }</pre>
     *
     * @return a new Splitter instance configured to split by line separators.
     * @see #with(Pattern)
     * @see #withDefault()
     */
    @Beta
    public static Splitter forLines() {
        return with(RegExUtil.LINE_SEPARATOR);
    }

    /**
     * Returns a new Splitter instance that uses the specified character as a delimiter.
     * This is the most efficient option when splitting by a single character.
     *
     * <p><b>The delimiter is one UTF-16 code unit, not one code point.</b> A supplementary character (such as
     * an emoji) is two {@code char}s and cannot be passed here; pass it to {@link #with(CharSequence)} instead,
     * which matches the pair. Passing a lone surrogate splits <i>inside</i> any surrogate pair that contains
     * it, leaving unpaired halves in the result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> parts = Splitter.with(',').split("apple,banana,cherry");
     * // Returns ["apple", "banana", "cherry"]
     * }</pre>
     *
     * @param delimiter the character to use as a delimiter for splitting.
     * @return a new Splitter instance configured with the specified character delimiter.
     * @see #with(CharSequence)
     * @see #with(Pattern)
     */
    public static Splitter with(final char delimiter) {
        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new SplitIterator(source, omitEmptyStrings, trim, strip, limit) {
                @Override
                boolean nextSeparator() {
                    for (int i = start; i < sourceLen; i++) {
                        if (source.charAt(i) == delimiter) {
                            separatorStart = i;
                            separatorEnd = i + 1;

                            return true;
                        }
                    }

                    return false;
                }
            };
        });
    }

    /**
     * Returns a new Splitter instance that uses the specified character sequence as a delimiter.
     * The delimiter is treated as a literal string, not as a pattern.
     * Its contents are captured when this method is called, so subsequent mutations to a mutable
     * {@link CharSequence} do not change the configured delimiter.
     * If the delimiter is a single character, this method delegates to the more
     * efficient single-character version.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> parts = Splitter.with("::").split("a::b::c");
     * // Returns ["a", "b", "c"]
     *
     * List<String> words = Splitter.with(" AND ").split("red AND green AND blue");
     * // Returns ["red", "green", "blue"]
     * }</pre>
     *
     * @param delimiter the character sequence to use as a delimiter for splitting, not {@code null} or empty.
     * @return a new Splitter instance configured with the specified delimiter.
     * @throws IllegalArgumentException if the specified delimiter is {@code null} or empty.
     * @see #with(char)
     * @see #with(Pattern)
     * @see #pattern(CharSequence)
     */
    public static Splitter with(final CharSequence delimiter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, cs.delimiter);

        final String delimiterStr = delimiter.toString();

        if (delimiterStr.length() == 1) {
            return with(delimiterStr.charAt(0));
        } else {
            // Extract the delimiter's characters once here rather than per iterator: the delimiter is immutable,
            // so one copy serves every split instead of allocating a fresh char[] on each call. The array is
            // shared by every iterator this Splitter creates and is only ever read, which keeps a configured
            // Splitter safe for concurrent read-only use (it is published through the final `strategy` field).
            @SuppressWarnings("deprecation")
            final char[] delimiterChars = InternalUtil.getCharsForReadOnly(delimiterStr);

            return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
                if (source == null) {
                    return ObjIterator.empty();
                }

                return new SplitIterator(source, omitEmptyStrings, trim, strip, limit) {
                    private final int delimiterLen = delimiterChars.length;

                    @Override
                    boolean nextSeparator() {
                        for (int i = start, last = sourceLen - delimiterLen; i <= last; i++) {
                            if (source.charAt(i) == delimiterChars[0] && match(i)) {
                                separatorStart = i;
                                separatorEnd = i + delimiterLen;

                                return true;
                            }
                        }

                        return false;
                    }

                    private boolean match(final int index) {
                        for (int i = 1; i < delimiterLen; i++) {
                            if (source.charAt(index + i) != delimiterChars[i]) {
                                return false;
                            }
                        }

                        return true;
                    }
                };
            });
        }
    }

    /**
     * Returns a new Splitter instance that uses the specified regular expression pattern as a delimiter.
     * The pattern is applied using Java's regular expression engine. The pattern must not match
     * the empty input string.
     *
     * <p><b>Zero-length matches are permitted</b> and split at the position they match, which is only checked
     * against the <i>empty</i> input: a pattern such as {@code \b} or {@code (?=,)} matches no characters yet
     * does not match {@code ""}, so it is accepted. Such a pattern behaves unlike {@link String#split(String)},
     * which suppresses a zero-length match at index 0 &mdash; this class does not, so a leading empty element
     * appears:</p>
     * <pre>{@code
     * Splitter.with(Pattern.compile("\\b")).split("ab cd");   // returns ["", "ab", " ", "cd", ""]
     * Arrays.asList("ab cd".split("\\b", -1));                // returns ["ab", " ", "cd", ""]
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pattern comma = Pattern.compile(",");
     * List<String> parts = Splitter.with(comma).split("a,b,c");
     * // Returns ["a", "b", "c"]
     *
     * Pattern whitespace = Pattern.compile("\\s+");
     * List<String> words = Splitter.with(whitespace).split("one  two   three");
     * // Returns ["one", "two", "three"]
     * }</pre>
     *
     * @param delimiter the Pattern to use as a delimiter for splitting, not {@code null}.
     * @return a new Splitter instance configured with the specified pattern delimiter.
     * @throws IllegalArgumentException if the specified delimiter is {@code null}, or if the pattern matches the
     *         empty input string (a pattern that merely has zero-length <i>matches</i> on non-empty input, such
     *         as {@code \b}, is accepted &mdash; see above).
     * @see #pattern(CharSequence)
     * @see #with(CharSequence)
     */
    public static Splitter with(final Pattern delimiter) throws IllegalArgumentException {
        N.checkArgNotNull(delimiter, cs.delimiter);
        N.checkArgument(!delimiter.matcher("").matches(), "Delimiter pattern must not match the empty input string: %s", delimiter);

        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new SplitIterator(source, omitEmptyStrings, trim, strip, limit) {
                private final Matcher matcher = delimiter.matcher(source);

                @Override
                boolean nextSeparator() {
                    // The matcher carries its own scan position and needs no re-positioning: after an ordinary
                    // match it resumes at the match end, which is exactly `start`; after a zero-width match it
                    // resumes one character further, which is what keeps the scan making progress.
                    if (start < sourceLen && matcher.find()) {
                        separatorStart = matcher.start();
                        separatorEnd = matcher.end();

                        return true;
                    }

                    return false;
                }
            };
        });
    }

    /**
     * Returns a new Splitter instance that uses the specified regular expression as a delimiter.
     * This is a convenience method that compiles the provided regular expression
     * string into a Pattern and then creates a Splitter with it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> words = Splitter.pattern("\\s+").split("one  two   three");
     * // Returns ["one", "two", "three"]
     *
     * List<String> parts = Splitter.pattern("[,;]").split("a,b;c");
     * // Returns ["a", "b", "c"]
     * }</pre>
     *
     * @param delimiterRegex the regular expression to use as a delimiter for splitting, not {@code null} or empty.
     * @return a new Splitter instance configured with the compiled pattern delimiter.
     * @throws IllegalArgumentException if the specified delimiter regex is {@code null} or empty, or if the
     *         resulting pattern matches the empty input string (see {@link #with(Pattern)} for how zero-length
     *         matches on non-empty input are treated).
     * @throws PatternSyntaxException if {@code delimiterRegex} is not a valid regular expression
     * @see #with(Pattern)
     * @see #with(CharSequence)
     */
    public static Splitter pattern(final CharSequence delimiterRegex) throws IllegalArgumentException, PatternSyntaxException {
        N.checkArgNotEmpty(delimiterRegex, cs.delimiterRegex);

        return with(Pattern.compile(delimiterRegex.toString()));
    }

    /**
     * Configures this Splitter to omit empty strings from the results when the specified
     * parameter is {@code true}. Empty strings can occur when there are consecutive delimiters
     * or when delimiters appear at the beginning or end of the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Splitter.with(",").omitEmptyStrings(true).split("a,,b,");    // returns ["a", "b"]
     * Splitter.with(",").omitEmptyStrings(false).split("a,,b,");   // returns ["a", "", "b", ""]
     * }</pre>
     *
     * <p>{@link #omitEmptyStrings()} can only turn this on, so {@code omitEmptyStrings(false)} is the only way
     * to turn it back off.</p>
     *
     * @param omitEmptyStrings {@code true} to omit empty strings from results, {@code false} to include them.
     * @return this Splitter instance for method chaining.
     * @see #omitEmptyStrings()
     */
    public Splitter omitEmptyStrings(final boolean omitEmptyStrings) {
        this.omitEmptyStrings = omitEmptyStrings;

        return this;
    }

    /**
     * Configures this Splitter to omit empty strings from the results.
     * Empty strings can occur when there are consecutive delimiters or when
     * delimiters appear at the beginning or end of the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> result = Splitter.with(",")
     *     .omitEmptyStrings()
     *     .split("a,,b,");
     * // Returns ["a", "b"] instead of ["a", "", "b", ""]
     * }</pre>
     *
     * @return this Splitter instance for method chaining.
     * @see #trimResults()
     * @see #stripResults()
     */
    public Splitter omitEmptyStrings() {
        omitEmptyStrings = true;

        return this;
    }

    /**
     * Configures this Splitter to trim leading and trailing spaces from each
     * resulting substring when the specified parameter is {@code true}. Only space
     * characters (not all whitespace) are trimmed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Splitter.with(",").trim(true).split("a , b , c");    // returns ["a", "b", "c"]
     * Splitter.with(",").trim(false).split("a , b , c");   // returns ["a ", " b ", " c"]
     * }</pre>
     *
     * <p><b>This is not {@link String#trim()}.</b> Only the space character {@code U+0020} is removed; tabs,
     * carriage returns and line feeds are kept. That differs from {@link String#trim()} and
     * {@link Strings#trim(String)} (which remove every character {@code <=} {@code U+0020}) and from Guava's
     * {@code Splitter.trimResults()} (which removes Unicode whitespace). Use {@link #stripResults()} for
     * whitespace-aware trimming:</p>
     * <pre>{@code
     * Splitter.with(',').trimResults().split("a	, b");    // returns ["a	", "b"] -- the tab survives
     * Splitter.with(',').stripResults().split("a	, b");   // returns ["a", "b"]
     * }</pre>
     *
     * <p>{@link #trimResults()} can only turn trim mode on, so {@code trim(false)} is the only way to turn it
     * back off without switching to {@link #stripResults() strip mode}.</p>
     *
     * @param trim {@code true} to trim spaces from results, {@code false} to leave them as-is. Passing
     *        {@code true} also turns {@link #stripResults() strip mode} off.
     * @return this Splitter instance for method chaining.
     * @see #trimResults()
     * @see #stripResults()
     */
    public Splitter trim(final boolean trim) {
        trimResults = trim;

        if (trim) {
            stripResults = false;
        }

        return this;
    }

    /**
     * Configures this Splitter to trim leading and trailing spaces from each
     * resulting substring. Only space characters (not all whitespace) are trimmed.
     * To recognize other leading and trailing whitespace characters, use {@link #stripResults()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> result = Splitter.with(",")
     *     .trimResults()
     *     .split("a , b , c");
     * // Returns ["a", "b", "c"] instead of ["a ", " b ", " c"]
     * }</pre>
     *
     * <p><b>This is not {@link String#trim()}.</b> Only the space character {@code U+0020} is removed; tabs,
     * carriage returns and line feeds are kept. That differs from {@link String#trim()} and
     * {@link Strings#trim(String)} (which remove every character {@code <=} {@code U+0020}) and from Guava's
     * {@code Splitter.trimResults()} (which removes Unicode whitespace). Use {@link #stripResults()} for
     * whitespace-aware trimming:</p>
     * <pre>{@code
     * Splitter.with(',').trimResults().split("a	, b");    // returns ["a	", "b"] -- the tab survives
     * Splitter.with(',').stripResults().split("a	, b");   // returns ["a", "b"]
     * }</pre>
     *
     * <p>Trimming and stripping are mutually exclusive: this method switches the splitter to trim mode,
     * turning {@link #stripResults() strip mode} off. The last of the two called wins, matching
     * {@link Joiner#trimBeforeAppend()} / {@link Joiner#stripBeforeAppend()}.</p>
     *
     * @return this Splitter instance for method chaining.
     * @see #stripResults()
     * @see #omitEmptyStrings()
     * @see Joiner#trimBeforeAppend()
     */
    public Splitter trimResults() {
        trimResults = true;
        stripResults = false;

        return this;
    }

    /**
     * Configures this Splitter to remove leading and trailing whitespace characters
     * from each resulting substring when the specified parameter is {@code true}. Whitespace
     * is identified by {@link Character#isWhitespace(char)}, including spaces, tabs, and newlines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Splitter.with(",").strip(true).split("a\t,\nb ,\tc");   // returns ["a", "b", "c"]
     * Splitter.with(",").strip(false).split("a\t, b ,c");     // returns ["a\t", " b ", "c"]
     * }</pre>
     *
     * <p>{@link #stripResults()} can only turn strip mode on, so {@code strip(false)} is the only way to turn
     * it back off without switching to {@link #trimResults() trim mode}.</p>
     *
     * @param strip {@code true} to strip whitespace from results, {@code false} to leave them as-is. Passing
     *        {@code true} also turns {@link #trimResults() trim mode} off.
     * @return this Splitter instance for method chaining.
     * @see Character#isWhitespace(char)
     * @see #stripResults()
     * @see #trimResults()
     */
    public Splitter strip(final boolean strip) {
        stripResults = strip;

        if (strip) {
            trimResults = false;
        }

        return this;
    }

    /**
     * Configures this Splitter to remove leading and trailing whitespace characters
     * from each resulting substring. Whitespace is identified by
     * {@link Character#isWhitespace(char)}, including spaces, tabs, and newlines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> result = Splitter.with(",")
     *     .stripResults()
     *     .split("a\t,\nb\t,\tc");
     * // Returns ["a", "b", "c"] with surrounding whitespace removed
     * }</pre>
     *
     * <p>Note that {@link Character#isWhitespace(char)} does <i>not</i> treat the non-breaking space
     * {@code U+00A0} as whitespace, so a value wrapped in non-breaking spaces is left untouched. To
     * <i>split on</i> Unicode whitespace instead, pass {@link #WHITE_SPACE_PATTERN} to {@link #with(Pattern)}.</p>
     *
     * <p>Trimming and stripping are mutually exclusive: this method switches the splitter to strip mode,
     * turning {@link #trimResults() trim mode} off. The last of the two called wins, matching
     * {@link Joiner#stripBeforeAppend()} / {@link Joiner#trimBeforeAppend()}.</p>
     *
     * @return this Splitter instance for method chaining.
     * @see #trimResults()
     * @see #omitEmptyStrings()
     * @see Character#isWhitespace(char)
     * @see Joiner#stripBeforeAppend()
     */
    public Splitter stripResults() {
        stripResults = true;
        trimResults = false;

        return this;
    }

    /**
     * Configures this Splitter to limit the maximum number of substrings to return when splitting.
     * If the limit is reached, the remainder of the input string will be included in the last
     * substring, without further splitting.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> result = Splitter.with(",")
     *     .limit(2)
     *     .split("a,b,c,d");
     * // Returns ["a", "b,c,d"]
     * }</pre>
     *
     * <p><b>Interaction with {@link #omitEmptyStrings()}.</b> The limit counts the substrings this splitter
     * actually <i>returns</i>. Before the final substring starts, each token is trimmed or stripped,
     * then dropped if it is empty, and only retained tokens count toward the limit. The final substring
     * starts at the next retained token and includes the remaining input without further splitting;
     * internal delimiters and empty fields remain, while its outer whitespace is still trimmed or stripped
     * if configured. That differs from {@code String.split(regex, limit)}, which counts every field including
     * the empty ones:</p>
     * <pre>{@code
     * Splitter.with(",").omitEmptyStrings().limit(2).split(",,a,b,c");   // returns ["a", "b,c"]
     * Splitter.with(",").omitEmptyStrings().limit(2).split("a,,,b,c");   // returns ["a", "b,c"]
     * Splitter.with(",").omitEmptyStrings().limit(2).split("a,b,,c");   // returns ["a", "b,,c"]
     * Splitter.with(",").omitEmptyStrings().limit(1).split(",");         // returns []
     * ",,a,b,c".split(",", 2);                                           // returns ["", ",a,b,c"]
     * }</pre>
     *
     * <p>Without {@link #omitEmptyStrings()} empty fields count toward the limit and remain in the final
     * substring; configured trimming or stripping still applies to that substring's outer whitespace:</p>
     * <pre>{@code
     * Splitter.with(",").limit(2).split(",,a,b,c");                      // returns ["", ",a,b,c"]
     * }</pre>
     *
     * @param limit the maximum number of substrings to return; must be positive.
     * @return this Splitter instance for method chaining.
     * @throws IllegalArgumentException if the provided limit is not a positive integer.
     * @see #split(CharSequence)
     * @see #omitEmptyStrings()
     */
    public Splitter limit(final int limit) throws IllegalArgumentException {
        N.checkArgPositive(limit, cs.limit);

        this.limit = limit;

        return this;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a List of strings. This is the primary splitting method
     * that produces an ArrayList containing all split substrings.
     *
     * <p>The splitting behavior is controlled by this Splitter's configuration,
     * including the delimiter type, whether to omit empty strings, whether to trim
     * or strip whitespace, and any configured limit on the number of results.</p>
     *
     * <p><b>A {@code null} source and an empty source differ.</b> A {@code null} source yields no elements at
     * all, while an empty source yields exactly one element &mdash; the empty string &mdash; because an empty
     * input still contains one (empty) field. Enable {@link #omitEmptyStrings()} to drop it. This matches
     * {@code String.split} and Guava's {@code Splitter}, and applies to every {@code split*} method on this
     * class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> parts = Splitter.with(",").split("a,b,c");
     * // Returns ["a", "b", "c"]
     *
     * Splitter.with(",").split((CharSequence) null);         // returns [] (size 0)
     * Splitter.with(",").split("");                          // returns [""] (size 1)
     * Splitter.with(",").omitEmptyStrings().split("");       // returns [] (size 0)
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @return a new ArrayList containing the split results; returns an empty list if source is {@code null}, and
     *         a single-element list holding the empty string if source is empty (unless
     *         {@link #omitEmptyStrings()} is configured).
     * @see #splitToCollection(CharSequence, Supplier)
     * @see #split(CharSequence, Function)
     * @see #split(CharSequence, Class)
     * @see #splitToArray(CharSequence)
     * @see #splitInto(CharSequence, Collection)
     * @see #splitToStream(CharSequence)
     */
    public List<String> split(final CharSequence source) {
        final List<String> result = new ArrayList<>();

        splitInto(source, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * converts each resulting substring to the specified target type using the
     * type system's valueOf method. This is useful for parsing strings into
     * primitive wrappers, enums, or other types with standard string conversion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Splitter.with(",").split("10,20,30", Integer.class);
     * // Returns [10, 20, 30]
     *
     * List<BigDecimal> amounts = Splitter.with(";").split("1.5;2.75;3.25", BigDecimal.class);
     * // Returns [1.5, 2.75, 3.25]
     * }</pre>
     *
     * <p><b>Empty tokens.</b> An empty token is converted by the selected target type's {@code valueOf} method.
     * String targets preserve {@code ""}; boxed numeric targets such as Integer yield {@code null}, while
     * primitive numeric targets yield zero. Other target types follow their own conversion rules. An empty {@code source} is
     * itself one empty token (see {@link #split(CharSequence)}), so splitting {@code ""} yields one element, not
     * none. Configure {@link #omitEmptyStrings()} to drop empty tokens instead of converting them:</p>
     * <pre>{@code
     * Splitter.with(",").split("1,,3", Integer.class);                      // returns [1, null, 3]
     * Splitter.with(",").split("", Integer.class);                          // returns [null]  (size 1)
     * Splitter.with(",").omitEmptyStrings().split("1,,3", Integer.class);   // returns [1, 3]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Class representing the type to convert each substring to, not {@code null}.
     * @return a new List containing the converted results.
     * @throws IllegalArgumentException if targetType is {@code null}.
     * @throws RuntimeException if resolving the requested conversion type or converting a split token fails
     * @see #split(CharSequence, Type)
     * @see #splitToCollection(CharSequence, Class, Supplier)
     * @see #splitToArray(CharSequence, Class)
     */
    public <T> List<T> split(final CharSequence source, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final Type<T> type = Type.of(targetType);

        return split(source, type);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * converts each resulting substring to the specified target type using the
     * provided Type instance for conversion. This method is useful when working
     * with the type system directly or when Class objects are insufficient (e.g.,
     * for generic types).
     *
     * <p>The Type instance provides more fine-grained control over the conversion
     * process compared to using Class objects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split and convert to List<Integer> using Type
     * Type<Integer> intType = N.typeOf(Integer.class);
     * List<Integer> numbers = Splitter.with(",").split("1,2,3", intType);
     * // Returns [1, 2, 3]
     * }</pre>
     *
     * <p><b>Empty tokens.</b> An empty token is converted by the selected target type's {@code valueOf} method.
     * String targets preserve {@code ""}; boxed numeric targets such as Integer yield {@code null}, while
     * primitive numeric targets yield zero. Other target types follow their own conversion rules. An empty {@code source} is
     * itself one empty token (see {@link #split(CharSequence)}), so splitting {@code ""} yields one element, not
     * none. Configure {@link #omitEmptyStrings()} to drop empty tokens instead of converting them:</p>
     * <pre>{@code
     * Splitter.with(",").split("1,,3", Integer.class);                      // returns [1, null, 3]
     * Splitter.with(",").split("", Integer.class);                          // returns [null]  (size 1)
     * Splitter.with(",").omitEmptyStrings().split("1,,3", Integer.class);   // returns [1, 3]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Type instance used for converting strings to the target type.
     * @return a new List containing the converted results.
     * @throws IllegalArgumentException if targetType is {@code null}.
     * @throws RuntimeException if converting a split token fails
     * @see #split(CharSequence, Class)
     * @see #splitToCollection(CharSequence, Type, Supplier)
     */
    public <T> List<T> split(final CharSequence source, final Type<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final List<T> result = new ArrayList<>();

        splitInto(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided mapping function to each resulting substring. This allows
     * transformation of split strings into a different type in a single operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Splitter.with(",").split("1,2,3", Integer::parseInt);
     * // Returns [1, 2, 3]
     * }</pre>
     *
     * @param <T> the type of elements in the result list.
     * @param source the CharSequence to split; may be {@code null}.
     * @param mapper a function to apply to each split string.
     * @return a new List containing the mapped results.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     * @see #split(CharSequence)
     * @see #split(CharSequence, Class)
     * @see #splitThenApply(CharSequence, Function)
     */
    public <T> List<T> split(final CharSequence source, final Function<? super String, ? extends T> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        final ObjIterator<String> iter = iterate(source);
        final List<T> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(mapper.apply(iter.next()));
        }

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results in a Collection created by the provided supplier. This
     * method allows control over the type of collection used to store the results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LinkedHashSet<String> uniqueParts = Splitter.with(",").splitToCollection("a,b,a,c", LinkedHashSet::new);
     * // Returns a LinkedHashSet containing ["a", "b", "c"]
     * }</pre>
     *
     * @param <C> the type of Collection to return.
     * @param source the CharSequence to split; may be {@code null}.
     * @param supplier a Supplier that creates a new Collection instance to hold the results.
     * @return the Collection created by the supplier, populated with the split results.
     * @throws IllegalArgumentException if {@code supplier} is {@code null} or returns {@code null}.
     * @throws UnsupportedOperationException if the supplied Collection rejects insertion and at least one
     *         element is produced.
     * @see #split(CharSequence)
     * @see #splitToCollection(CharSequence, Class, Supplier)
     */
    public <C extends Collection<String>> C splitToCollection(final CharSequence source, final Supplier<? extends C> supplier)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(supplier, cs.supplier);

        final C result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

        splitInto(source, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and returns
     * the results in a Collection created by the provided supplier. This provides
     * control over both the conversion type and the collection type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<Integer> uniqueNumbers = Splitter.with(",").splitToCollection("1,2,1,3", Integer.class, HashSet::new);
     * // Returns a HashSet containing {1, 2, 3}
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param <C> the type of Collection to return.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Class representing the type to convert each substring to.
     * @param supplier a Supplier that creates a new Collection instance to hold the results.
     * @return the Collection created by the supplier, populated with the converted results.
     * @throws IllegalArgumentException if targetType is {@code null}, or if {@code supplier} is {@code null} or returns {@code null}.
     * @throws RuntimeException if resolving the requested conversion type or converting a split token fails
     * @throws NullPointerException if a converted element is {@code null} and the destination rejects it
     * @throws ClassCastException if a converted element has a type that the destination cannot accept or compare
     * @throws UnsupportedOperationException if the supplied Collection rejects insertion and at least one
     *         element is produced.
     */
    public <T, C extends Collection<T>> C splitToCollection(final CharSequence source, final Class<? extends T> targetType,
            final Supplier<? extends C> supplier)
            throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(supplier, cs.supplier);

        final C result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

        splitInto(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type using the
     * provided Type instance, and returns the results in a Collection created
     * by the provided supplier. This method provides maximum flexibility by
     * allowing control over both the Type-based conversion and the collection type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split into a TreeSet of Integers using Type
     * Type<Integer> intType = N.typeOf(Integer.class);
     * TreeSet<Integer> uniqueNumbers = Splitter.with(",").splitToCollection("3,1,2,1,3", intType, TreeSet::new);
     * // Returns sorted unique values: [1, 2, 3]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param <C> the type of Collection to return.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Type instance used for converting strings to the target type.
     * @param supplier a Supplier that creates a new Collection instance to hold the results.
     * @return the Collection created by the supplier, populated with the converted results.
     * @throws IllegalArgumentException if targetType is {@code null}, or if {@code supplier} is {@code null} or returns {@code null}.
     * @throws RuntimeException if converting a split token fails
     * @throws NullPointerException if a converted element is {@code null} and the destination rejects it
     * @throws ClassCastException if a converted element has a type that the destination cannot accept or compare
     * @throws UnsupportedOperationException if the supplied Collection rejects insertion and at least one
     *         element is produced.
     */
    public <T, C extends Collection<T>> C splitToCollection(final CharSequence source, final Type<? extends T> targetType, final Supplier<? extends C> supplier)
            throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(supplier, cs.supplier);

        final C result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

        splitInto(source, targetType, result);

        return result;
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * adds the resulting substrings to the provided output collection. This method
     * is useful when you want to append split results to an existing collection
     * rather than creating a new one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> allParts = new ArrayList<>();
     * Splitter.with(",").splitInto("a,b", allParts);
     * Splitter.with(";").splitInto("c;d", allParts);
     * // allParts now contains ["a", "b", "c", "d"]
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @param output the Collection to add the split results to, not {@code null}.
     * @throws IllegalArgumentException if output is {@code null}.
     * @throws UnsupportedOperationException if {@code output} rejects insertion (for example an immutable or
     *         fixed-size collection) and at least one element is produced.
     * @see #split(CharSequence)
     * @see #splitToCollection(CharSequence, Supplier)
     */
    public void splitInto(final CharSequence source, final Collection<String> output) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(iter.next());
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and adds
     * the converted values to the provided output collection. This method is
     * useful for appending parsed values to an existing collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = new ArrayList<>();
     * Splitter.with(",").splitInto("1,2,3", Integer.class, numbers);
     * Splitter.with(";").splitInto("4;5;6", Integer.class, numbers);
     * // numbers now contains [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Class representing the type to convert each substring to.
     * @param output the Collection to add the converted results to.
     * @throws IllegalArgumentException if targetType or output is {@code null}.
     * @throws RuntimeException if resolving the requested conversion type or converting a split token fails
     * @throws NullPointerException if a converted element is {@code null} and the destination rejects it
     * @throws ClassCastException if a converted element has a type that the destination cannot accept or compare
     * @throws UnsupportedOperationException if {@code output} rejects insertion and at least one element is
     *         produced.
     */
    public <T> void splitInto(final CharSequence source, final Class<? extends T> targetType, final Collection<T> output)
            throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(output, cs.output);

        final Type<T> type = Type.of(targetType);

        splitInto(source, type, output);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type using the
     * provided Type instance, and adds the converted values to the provided
     * output collection. This method provides Type-based conversion for appending
     * to existing collections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Double> results = new ArrayList<>();
     * Type<Double> doubleType = N.typeOf(Double.class);
     * Splitter.with(",").splitInto("1.5,2.7,3.9", doubleType, results);
     * // results now contains [1.5, 2.7, 3.9]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Type instance used for converting strings to the target type.
     * @param output the Collection to add the converted results to.
     * @throws IllegalArgumentException if targetType or output is {@code null}.
     * @throws RuntimeException if converting a split token fails
     * @throws NullPointerException if a converted element is {@code null} and the destination rejects it
     * @throws ClassCastException if a converted element has a type that the destination cannot accept or compare
     * @throws UnsupportedOperationException if {@code output} rejects insertion and at least one element is
     *         produced.
     */
    public <T> void splitInto(final CharSequence source, final Type<? extends T> targetType, final Collection<T> output)
            throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(targetType.valueOf(iter.next()));
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * populates the provided String array with the results. If the array is larger
     * than the number of split results, remaining elements are left unchanged.
     * If the array is smaller than the number of split results, only the first
     * array.length results are stored. This method is useful when you want to
     * reuse an existing array or have pre-allocated storage.
     *
     * <p>An empty {@code output} array is accepted and stores nothing, which is the zero case of "store only
     * the first {@code output.length} results".</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] parts = new String[3];
     * Splitter.with(",").splitInto("a,b,c,d", parts);
     * // parts now contains ["a", "b", "c"] (4th element "d" is not stored)
     *
     * Splitter.with(",").splitInto("a,b", new String[0]);   // no-op
     * }</pre>
     *
     * <p><b>Slots beyond the result count keep their previous contents</b>, and this method reports no count,
     * so a reused array can leave stale elements in place. Clear the array first, or use
     * {@link #splitToArray(CharSequence)} when the number of results is not known in advance.</p>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @param output the String array to populate with split results, not {@code null}; an empty array is a no-op.
     * @throws IllegalArgumentException if output is {@code null}.
     * @see #splitToArray(CharSequence)
     * @see #splitInto(CharSequence, Collection)
     */
    public void splitInto(final CharSequence source, final String[] output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        final ObjIterator<String> iter = iterate(source);

        for (int i = 0, len = output.length; i < len && iter.hasNext(); i++) {
            output[i] = iter.next();
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as an ImmutableList. The returned list cannot be modified,
     * providing a safe, read-only view of the split results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> parts = Splitter.with(",").splitToImmutableList("a,b,c");
     * // Returns an immutable list ["a", "b", "c"]
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @return an ImmutableList containing the split results.
     * @see #split(CharSequence)
     * @see #splitToImmutableList(CharSequence, Class)
     */
    public ImmutableList<String> splitToImmutableList(final CharSequence source) {
        return ImmutableList.wrap(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts each resulting substring to the specified target type, and returns
     * the results as an ImmutableList. The returned list cannot be modified,
     * providing a safe, read-only view of the converted results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> numbers = Splitter.with(",")
     *     .splitToImmutableList("1,2,3", Integer.class);
     * // Returns an immutable list [1, 2, 3]
     * }</pre>
     *
     * @param <T> the target type for conversion.
     * @param source the CharSequence to split; may be {@code null}.
     * @param targetType the Class representing the type to convert each substring to, not {@code null}.
     * @return an ImmutableList containing the converted results.
     * @throws IllegalArgumentException if targetType is {@code null}.
     * @throws RuntimeException if resolving the requested conversion type or converting a split token fails
     * @see #splitToImmutableList(CharSequence)
     * @see #split(CharSequence, Class)
     */
    public <T> ImmutableList<T> splitToImmutableList(final CharSequence source, final Class<? extends T> targetType)
            throws IllegalArgumentException, RuntimeException {
        return ImmutableList.wrap(split(source, targetType));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a String array. This is useful when an array is
     * preferred over a List.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] parts = Splitter.with(",").splitToArray("a,b,c");
     * // Returns ["a", "b", "c"]
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @return a String array containing the split results; returns an empty array if source is {@code null}.
     * @see #split(CharSequence)
     * @see #splitToArray(CharSequence, Function)
     * @see #splitToArray(CharSequence, Class)
     * @see #splitInto(CharSequence, String[])
     */
    public String[] splitToArray(final CharSequence source) {
        final List<String> substrs = split(source);

        return substrs.toArray(new String[0]);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * applies the provided mapping function to each resulting substring, and
     * returns the results as a String array. This combines splitting, mapping,
     * and array conversion in a single operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] upper = Splitter.with(",")
     *     .splitToArray("a,b,c", String::toUpperCase);
     * // Returns ["A", "B", "C"]
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @param mapper a function to apply to each split string.
     * @return a String array containing the mapped results.
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    public String[] splitToArray(final CharSequence source, final Function<? super String, ? extends String> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        final List<String> substrs = split(source, mapper);

        return substrs.toArray(new String[0]);
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration,
     * converts the results to the specified array type, and returns the array.
     * The array type must be an array class (e.g., String[].class, Integer[].class).
     * Each split substring is automatically converted to the array's component type.
     *
     * <p>This method handles both primitive arrays (e.g., int[], double[]) and
     * object arrays (e.g., Integer[], String[]).</p>
     *
     * <p><b>Multi-dimensional array types are accepted but rarely what you want.</b> The component type is
     * taken one level down, so each <i>token</i> is converted to the nested array type rather than the whole
     * input being reshaped: {@code splitToArray("a,b", String[][].class)} yields {@code [["a"], ["b"]]}, and a
     * nested primitive type whose conversion rejects the token propagates that failure
     * ({@code splitToArray("a,b", int[][].class)} throws {@link NumberFormatException}).</p>
     *
     * <p><b>Empty tokens:</b> a primitive component type cannot hold {@code null}, so an empty token converts
     * to that type's zero value, whereas the boxed component type converts it to {@code null}. Configure
     * {@link #omitEmptyStrings()} if empty tokens should be dropped instead of converted. Note that an empty
     * {@code source} is itself one empty token (see {@link #split(CharSequence)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] numbers = Splitter.with(",").splitToArray("1,2,3", Integer[].class);
     * // Returns [1, 2, 3]
     *
     * Splitter.with(",").splitToArray("a,b", String[][].class);   // returns [["a"], ["b"]]
     *
     * int[] primitives = Splitter.with(";").splitToArray("10;20;30", int[].class);
     * // Returns [10, 20, 30]
     *
     * Splitter.with(",").splitToArray("1,,3", int[].class);       // returns [1, 0, 3]
     * Splitter.with(",").splitToArray("1,,3", Integer[].class);   // returns [1, null, 3]
     * }</pre>
     *
     * @param <T> the array type.
     * @param source the CharSequence to split; may be {@code null}.
     * @param arrayType the Class object representing the desired array type.
     * @return an array of the specified type containing the split and converted results.
     * @throws IllegalArgumentException if arrayType is {@code null} or not an array type.
     * @throws RuntimeException if resolving the requested conversion type or converting a split token fails
     */
    @SuppressWarnings("unchecked")
    public <T> T splitToArray(final CharSequence source, final Class<T> arrayType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(arrayType, cs.arrayType);

        final Class<?> eleCls = arrayType.getComponentType();

        N.checkArgument(eleCls != null, "'arrayType' must be an array type, but got: %s", arrayType);

        final List<String> substrs = split(source);

        if (eleCls.equals(String.class) || eleCls.equals(Object.class)) {
            return (T) substrs.toArray((Object[]) N.newArray(eleCls, substrs.size()));
        } else {
            final Type<?> eleType = Type.of(eleCls);
            final Object a = N.newArray(eleCls, substrs.size());

            if (ClassUtil.isPrimitiveType(eleCls)) {
                for (int i = 0, len = substrs.size(); i < len; i++) {
                    Array.set(a, i, eleType.valueOf(substrs.get(i)));
                }
            } else {
                final Object[] objArray = (Object[]) a;

                for (int i = 0, len = substrs.size(); i < len; i++) {
                    objArray[i] = eleType.valueOf(substrs.get(i));
                }
            }

            return (T) a;
        }
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * returns the results as a Stream of strings. This allows for lazy evaluation
     * and further stream operations on the split results without creating an
     * intermediate collection.
     *
     * <p>The stream evaluation is lazy - substrings are produced on-demand as
     * the stream is consumed, making this memory-efficient for large inputs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long count = Splitter.with(",")
     *     .splitToStream("a,b,c,d,e")
     *     .filter(s -> s.length() > 0)
     *     .count();
     * // Returns 5
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @return a Stream containing the split results; returns an empty stream if source is {@code null}.
     * @see #split(CharSequence)
     * @see #splitThenForEach(CharSequence, Consumer)
     */
    public Stream<String> splitToStream(final CharSequence source) {
        return Stream.of(iterate(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided function to the resulting list of strings. This is
     * useful for transforming or aggregating the split results in a single operation,
     * combining the split and transformation steps.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String joined = Splitter.with(",")
     *     .splitThenApply("a,b,c", list -> String.join("-", list));
     * // Returns "a-b-c"
     *
     * int sum = Splitter.with(";")
     *     .splitThenApply("1;2;3", list -> list.stream()
     *         .mapToInt(Integer::parseInt)
     *         .sum());
     * // Returns 6
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param source the CharSequence to split; may be {@code null}.
     * @param converter a function that transforms the list of split strings into a result.
     * @return the result of applying the converter function to the split results.
     * @throws IllegalArgumentException if {@code converter} is {@code null}.
     * @see #split(CharSequence)
     * @see #splitThenAccept(CharSequence, Consumer)
     */
    public <R> R splitThenApply(final CharSequence source, final Function<? super List<String>, R> converter) throws IllegalArgumentException {
        N.checkArgNotNull(converter, cs.converter);

        return converter.apply(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * passes the resulting list of strings to the provided consumer. This is
     * useful for performing side effects with the split results, such as logging
     * or validation operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Splitter.with(",").splitThenAccept("a,b,c", list -> {
     *     System.out.println("Split into " + list.size() + " parts");
     *     list.forEach(System.out::println);
     * });
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @param consumer a consumer that processes the list of split strings.
     * @throws IllegalArgumentException if {@code consumer} is {@code null}.
     * @see #split(CharSequence)
     * @see #splitThenApply(CharSequence, Function)
     * @see #splitThenForEach(CharSequence, Consumer)
     */
    public void splitThenAccept(final CharSequence source, final Consumer<? super List<String>> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer, cs.consumer);

        consumer.accept(split(source));
    }

    /**
     * Splits the specified CharSequence using this Splitter's configuration and
     * applies the provided action to each resulting substring. This method processes
     * each substring as it is produced, without creating an intermediate collection,
     * making it memory-efficient for large inputs.
     *
     * <p>This method provides lazy evaluation - the action is applied to each
     * substring immediately as it's split, without storing all results in memory.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Splitter.with(",").splitThenForEach("a,b,c", part -> {
     *     System.out.println("Processing: " + part);
     * });
     * // Prints each part as it's split
     * }</pre>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @param action the Consumer to apply to each resulting substring.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @see #splitToStream(CharSequence)
     * @see #splitThenAccept(CharSequence, Consumer)
     */
    @Beta
    public void splitThenForEach(final CharSequence source, final Consumer<? super String> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        iterate(source).forEachRemaining(action);
    }

    /**
     * Creates an iterator over the substrings produced by splitting the specified
     * source CharSequence using this Splitter's configuration. This method provides
     * lazy evaluation of the split operation, producing substrings on-demand as the
     * iterator is consumed.
     *
     * <p>This is an internal method used by other split methods to avoid creating
     * intermediate collections when not necessary. The iterator respects all
     * configured options including delimiter type, omitEmptyStrings, trimResults,
     * stripResults, and limit settings.</p>
     *
     * <p>The returned iterator does not support the {@code remove()} operation. It
     * produces substrings lazily, making it memory-efficient for large inputs.</p>
     *
     * @param source the CharSequence to split; may be {@code null}.
     * @return an ObjIterator that produces split substrings; returns an empty iterator if source is {@code null}.
     * @see #splitToStream(CharSequence)
     * @see #split(CharSequence)
     */
    ObjIterator<String> iterate(final CharSequence source) {
        return strategy.split(source, omitEmptyStrings, trimResults, stripResults, limit);
    }

    /**
     * A specialized splitter for creating maps from strings. This class splits
     * strings into key-value pairs using two levels of delimiters: one for
     * separating entries and another for separating keys from values within
     * each entry.
     *
     * <p>The MapSplitter provides a fluent API for parsing delimited key-value
     * strings into Map objects, with support for various configuration options
     * such as trimming, stripping whitespace, and omitting empty entries.</p>
     *
     * <p>Two behaviours are fixed when a MapSplitter is created:</p>
     * <ul>
     *   <li>Empty entry strings are omitted &mdash; call {@link #omitEmptyStrings(boolean)
     *       omitEmptyStrings(false)} to keep them</li>
     *   <li>Each entry is split into at most two parts, so the <i>first</i> key-value delimiter
     *       separates the key from the value and any further occurrences stay inside the value</li>
     * </ul>
     *
     * <p>An entry that contains no key-value delimiter at all causes an
     * {@link IllegalArgumentException}. An entry whose key part is empty is accepted, producing an entry under
     * the empty-string key.</p>
     *
     * <p><b>Duplicate keys.</b> Every {@code split*} method that produces a {@code Map} inserts entries in
     * encounter order, so a later entry silently replaces an earlier one with an equal key &mdash;
     * last-one-wins. Two textually different keys can also collide once they are converted, because equality is
     * decided on the <i>converted</i> key. The {@code splitToStream}/{@code splitToEntryStream} methods do not
     * build a {@code Map} and therefore preserve every entry, duplicates included:</p>
     * <pre>{@code
     * MapSplitter.with(",", "=").split("a=1,a=2");                                // {a=2}
     * MapSplitter.with(",", "=").split("1=a,01=b", Integer.class, String.class);  // {1=b} -- keys collide
     * MapSplitter.with(",", "=").splitToStream("a=1,a=2").toList();               // [a=1, a=2]
     * MapSplitter.with(",", "=").split("=1");                                     // {""="1"}
     * }</pre>
     *
     * <p>When an output {@code Map} is supplied by the caller through {@link #splitInto}, that map's own rules
     * apply on top: a null-hostile or immutable map may reject an insertion, and a {@code SortedMap} reorders
     * the result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = MapSplitter.with(",", "=")
     *     .split("a=1,b=2,c=3");
     * // Returns {a=1, b=2, c=3}
     *
     * Map<Integer, String> typed = MapSplitter.with(";", ":")
     *     .trimResults()
     *     .split("1 : apple ; 2 : banana", Integer.class, String.class);
     * // Returns {1=apple, 2=banana}
     * }</pre>
     *
     */
    public static final class MapSplitter {

        /** The entry splitter. */
        private final Splitter entrySplitter;

        /** The key value splitter. */
        private final Splitter keyValueSplitter;

        /**
         * Creates a map splitter from the specified entry and key-value splitters.
         * Empty entries are omitted, and each entry is divided into at most two
         * key-value parts.
         *
         * <p><b>Takes ownership of both arguments and reconfigures them in place</b> &mdash;
         * {@code entrySplitter} gets {@link Splitter#omitEmptyStrings()} and {@code keyValueSplitter} gets
         * {@link Splitter#limit(int) limit(2)}. {@code Splitter} configuration mutates the receiver, so callers
         * must pass freshly created splitters that nothing else holds a reference to; every factory on this class
         * does. Never hand a cached or shared {@code Splitter} to this constructor.</p>
         *
         * @param entrySplitter the splitter used to separate map entries; must be freshly created, and is
         *        reconfigured by this constructor
         * @param keyValueSplitter the splitter used to separate each key from its value; must be freshly created,
         *        and is reconfigured by this constructor
         */
        MapSplitter(final Splitter entrySplitter, final Splitter keyValueSplitter) {
            this.entrySplitter = entrySplitter;
            this.entrySplitter.omitEmptyStrings();
            this.keyValueSplitter = keyValueSplitter;
            this.keyValueSplitter.limit(2);
        }

        /**
         * Returns a new MapSplitter instance configured with the default entry and key-value delimiters.
         * The default entry delimiter is ", " (comma followed by space) and the
         * default key-value delimiter is "=" (equals sign).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.withDefault().split("name=John, age=30, city=NYC");
         * // Returns {name=John, age=30, city=NYC}
         * }</pre>
         *
         * @return a new MapSplitter instance with default delimiters ", " and "=".
         * @see #with(CharSequence, CharSequence)
         * @see Joiner#withDefault()
         */
        @Beta
        public static MapSplitter withDefault() {
            return with(DEFAULT_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER);
        }

        /**
         * Returns a new MapSplitter instance with the specified entry and key-value delimiters.
         * The entry delimiter separates different key-value pairs, while the
         * key-value delimiter separates keys from values within each pair.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.with(",", "=").split("a=1,b=2,c=3");
         * // Returns {a=1, b=2, c=3}
         *
         * Map<String, String> config = MapSplitter.with(";", ":").split("host:localhost;port:8080");
         * // Returns {host=localhost, port=8080}
         * }</pre>
         *
         * @param entryDelimiter the delimiter that separates entries (key-value pairs), not {@code null} or empty.
         * @param keyValueDelimiter the delimiter that separates keys from values, not {@code null} or empty.
         * @return a new MapSplitter instance with the specified delimiters.
         * @throws IllegalArgumentException if either delimiter is {@code null} or empty.
         * @see #with(Pattern, Pattern)
         * @see #pattern(CharSequence, CharSequence)
         * @see Splitter#with(CharSequence)
         */
        public static MapSplitter with(final CharSequence entryDelimiter, final CharSequence keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * Returns a new MapSplitter instance with the specified entry and key-value delimiter patterns.
         * The patterns are used as regular expressions for splitting.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pattern comma = Pattern.compile("[,;]");
         * Pattern equals = Pattern.compile("[=:]");
         * Map<String, String> map = MapSplitter.with(comma, equals)
         *     .split("a=1,b:2;c=3");
         * // Returns {a=1, b=2, c=3}
         * }</pre>
         *
         * @param entryDelimiter the Pattern that separates entries (key-value pairs), not {@code null}.
         * @param keyValueDelimiter the Pattern that separates keys from values, not {@code null}.
         * @return a new MapSplitter instance with the specified pattern delimiters.
         * @throws IllegalArgumentException if either delimiter is {@code null}, or if either pattern can match an
         *         empty string.
         * @see #with(CharSequence, CharSequence)
         * @see #pattern(CharSequence, CharSequence)
         * @see Splitter#with(Pattern)
         */
        public static MapSplitter with(final Pattern entryDelimiter, final Pattern keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * Returns a new MapSplitter instance with the specified entry and key-value delimiter
         * regular expressions. The regular expressions are compiled into Patterns
         * and used for splitting.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.pattern("[,;]", "[=:]")
         *     .split("a=1,b:2;c=3");
         * // Returns {a=1, b=2, c=3}
         * }</pre>
         *
         * @param entryDelimiterRegex the regular expression that separates entries, not {@code null} or empty.
         * @param keyValueDelimiterRegex the regular expression that separates keys from values, not {@code null} or empty.
         * @return a new MapSplitter instance with the compiled pattern delimiters.
         * @throws IllegalArgumentException if either regex is {@code null} or empty, or if the compiled patterns can
         *         match an empty string.
         * @throws PatternSyntaxException if either delimiter regex is not a valid regular expression
         * @see #with(Pattern, Pattern)
         * @see #with(CharSequence, CharSequence)
         * @see Splitter#pattern(CharSequence)
         */
        public static MapSplitter pattern(final CharSequence entryDelimiterRegex, final CharSequence keyValueDelimiterRegex)
                throws IllegalArgumentException, PatternSyntaxException {
            return new MapSplitter(Splitter.pattern(entryDelimiterRegex), Splitter.pattern(keyValueDelimiterRegex));
        }

        /**
         * Configures this MapSplitter to omit empty entry strings when the
         * specified parameter is {@code true}. This applies to the entry splitting phase.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * MapSplitter.with(",", "=").omitEmptyStrings(true).split("a=1,,b=2");  // returns {a=1, b=2}
         * }</pre>
         *
         * <p>Omitting empty entries is on by default for a {@code MapSplitter}, so
         * {@code omitEmptyStrings(false)} is the only way to turn it
         * off; {@link #omitEmptyStrings()} can only turn it back on.</p>
         *
         * @param omitEmptyStrings {@code true} to omit empty entry strings; {@code false} to keep them, in which
         *        case an empty entry — having no key-value delimiter — makes the split throw
         *        {@link IllegalArgumentException}.
         * @return this MapSplitter instance for method chaining.
         * @see #omitEmptyStrings()
         */
        public MapSplitter omitEmptyStrings(final boolean omitEmptyStrings) {
            entrySplitter.omitEmptyStrings(omitEmptyStrings);

            return this;
        }

        /**
         * Configures this MapSplitter to omit empty entry strings.
         * This applies to the entry splitting phase, filtering out entry
         * strings that are empty after splitting by the entry delimiter.
         *
         * <p>Omitting empty entries is already enabled when a MapSplitter is created, so calling
         * this method only restores the default after {@link #omitEmptyStrings(boolean)
         * omitEmptyStrings(false)} has turned it off.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.with(",", "=")
         *     .omitEmptyStrings()
         *     .split("a=1,,b=2");
         * // Returns {a=1, b=2} (empty entry between commas is omitted)
         * }</pre>
         *
         * @return this MapSplitter instance for method chaining.
         * @see #trimResults()
         * @see #stripResults()
         */
        public MapSplitter omitEmptyStrings() {
            entrySplitter.omitEmptyStrings();

            return this;
        }

        /**
         * Configures this MapSplitter to trim spaces from both entries and
         * key-value pairs when the specified parameter is {@code true}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * MapSplitter.with(",", "=").trim(true).split("a = 1 , b = 2");  // returns {a=1, b=2}
         * }</pre>
         *
         * <p>{@link #trimResults()} can only turn trim mode on, so {@code trim(false)} is the only way to turn
         * it back off without switching to {@link #stripResults() strip mode}.</p>
         *
         * @param trim {@code true} to trim spaces, {@code false} to leave them as-is. Passing {@code true} also
         *        turns {@link #stripResults() strip mode} off.
         * @return this MapSplitter instance for method chaining.
         * @see #trimResults()
         * @see Splitter#trimResults()
         */
        public MapSplitter trim(final boolean trim) {
            entrySplitter.trim(trim);
            keyValueSplitter.trim(trim);

            return this;
        }

        /**
         * Configures this MapSplitter to trim leading and trailing spaces from
         * both entries and key-value pairs. This ensures clean keys and values
         * without surrounding spaces. Only space characters are trimmed, not all
         * whitespace.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.with(",", "=")
         *     .trimResults()
         *     .split("a = 1 , b = 2");
         * // Returns {a=1, b=2} (spaces around keys and values are removed)
         * }</pre>
         *
         * <p><b>This is not {@link String#trim()}.</b> Only the space character {@code U+0020} is removed from
         * keys and values; tabs, carriage returns and line feeds are kept. Use {@link #stripResults()} for
         * whitespace-aware trimming. Trimming and stripping are mutually exclusive &mdash; the last of the two
         * called wins. See {@link Splitter#trimResults()} for the full rationale.</p>
         *
         * @return this MapSplitter instance for method chaining.
         * @see #stripResults()
         * @see #omitEmptyStrings()
         * @see Splitter#trimResults()
         */
        public MapSplitter trimResults() {
            entrySplitter.trimResults();
            keyValueSplitter.trimResults();

            return this;
        }

        /**
         * Configures this MapSplitter to strip all leading and trailing whitespace
         * characters from both entries and key-value pairs when the specified
         * parameter is {@code true}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * MapSplitter.with(",", "=").strip(true).split("a\t=\n1\t, b = 2");  // returns {a=1, b=2}
         * }</pre>
         *
         * <p>{@link #stripResults()} can only turn strip mode on, so {@code strip(false)} is the only way to
         * turn it back off without switching to {@link #trimResults() trim mode}.</p>
         *
         * @param strip {@code true} to strip whitespace, {@code false} to leave it as-is. Passing {@code true}
         *        also turns {@link #trimResults() trim mode} off.
         * @return this MapSplitter instance for method chaining.
         * @see Character#isWhitespace(char)
         * @see #stripResults()
         * @see Splitter#stripResults()
         */
        public MapSplitter strip(final boolean strip) {
            entrySplitter.strip(strip);
            keyValueSplitter.strip(strip);

            return this;
        }

        /**
         * Configures this MapSplitter to strip all leading and trailing whitespace
         * characters from both entries and key-value pairs. This removes all forms
         * of whitespace as defined by {@link Character#isWhitespace(char)}, including
         * spaces, tabs, newlines, and other Unicode whitespace characters.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.with(",", "=")
         *     .stripResults()
         *     .split("a\t=\n1\t,\tb\t=\t2");
         * // Returns {a=1, b=2} (all whitespace around keys and values is removed)
         * }</pre>
         *
         * <p>Trimming and stripping are mutually exclusive: this method switches the splitter to strip mode,
         * turning {@link #trimResults() trim mode} off. The last of the two called wins.</p>
         *
         * @return this MapSplitter instance for method chaining.
         * @see #trimResults()
         * @see #omitEmptyStrings()
         * @see Character#isWhitespace(char)
         * @see Splitter#stripResults()
         */
        public MapSplitter stripResults() {
            entrySplitter.stripResults();
            keyValueSplitter.stripResults();

            return this;
        }

        /**
         * Sets the maximum number of map entries to produce when splitting.
         *
         * <p><b>Limit Semantics - "Up To N":</b>
         * <ul>
         *   <li>{@code limit(N)} means: produce <b>AT MOST N</b> map entries</li>
         *   <li>If input has fewer than N pairs: returns all pairs</li>
         *   <li>If input has exactly N pairs: returns all N pairs - but the N-th entry always extends to the
         *       end of the input, so anything left after it (a trailing entry delimiter, or trailing empty
         *       entries) is absorbed into that entry's <i>value</i> rather than discarded. While empty entry
         *       strings are omitted - the default, see {@link #omitEmptyStrings(boolean)} - a leading or internal
         *       empty entry is not absorbed, because it is dropped before the limit is counted:
         *       {@code limit(2).split("a=1,b=2,")} gives key {@code b} the value {@code "2,"}, not {@code "2"},
         *       while {@code limit(2).split("a=1,,b=2")} gives it {@code "2"}. After
         *       {@code omitEmptyStrings(false)} that internal empty entry is kept and counted, and the same input
         *       yields the key {@code ",b"} instead</li>
         *   <li>If input has more than N pairs: the remaining input is absorbed into the N-th <i>entry</i> (it is
         *       not discarded), and that whole entry is then split on its own <i>first</i> key-value delimiter. So
         *       the residue lands in the value when the N-th entry's delimiter precedes it -
         *       {@code limit(2).split("a=1,b=2,c=3")} gives key {@code b} the value {@code "2,c=3"} - but in the
         *       key when it does not: {@code limit(2).split("a=1,b,c=2")} gives the key {@code "b,c"} the value
         *       {@code "2"}</li>
         *   <li>The limit caps the number of entries <i>parsed</i>, not the size of the returned map: two
         *       parsed entries whose keys are equal collapse into one, so the map can be smaller than N -
         *       see <b>Duplicate keys</b> in the class javadoc</li>
         * </ul>
         *
         * <p><b>Common Confusion:</b>
         * <ul>
         *   <li>{@code limit(2)} does NOT mean "return 1 entry"</li>
         *   <li>{@code limit(2)} means "return up to 2 entries"</li>
         *   <li>This is consistent with {@code String.split()} limit behavior</li>
         * </ul>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * MapSplitter splitter = MapSplitter.with(",", "=");
         *
         * // Input has more entries than limit
         * Map<String, String> firstTwo = splitter.limit(2).split("a=1,b=2,c=3,d=4");
         * // Returns: {a=1, b=2,c=3,d=4} - 2 entries; the last value absorbs the remaining input
         *
         * // Input has exactly limit entries
         * Map<String, String> exactlyTwo = splitter.limit(2).split("a=1,b=2");
         * // Returns: {a=1, b=2} - all 2 entries
         *
         * // Input has fewer entries than limit
         * Map<String, String> fewerThanFive = splitter.limit(5).split("a=1,b=2");
         * // Returns: {a=1, b=2} - all available entries (only 2)
         *
         * // Input is empty
         * Map<String, String> empty = splitter.limit(2).split("");
         * // Returns: {} - empty map
         *
         * // Combined with other options
         * Map<String, String> combined = splitter.limit(2).trimResults().omitEmptyStrings()
         *     .split(" a = 1 , , b = 2 , c = 3 ");
         * // Returns: {a=1, b="2 , c = 3"}. Empty entries are dropped before the limit is counted, so the
         * // final entry starts at the first non-empty entry and absorbs the remaining input from there.
         * }</pre>
         *
         * <p><b>Common Mistakes:</b></p>
         * <pre>{@code
         * MapSplitter splitter = MapSplitter.with(",", "=");
         * Map<String, String> result = splitter.limit(2).split("a=1,b=2,c=3");
         * assert result.size() == 2;   // limit(2) means up to two map entries
         *
         * String[] parts = "a,b,c".split(",", 2);
         * assert Arrays.equals(new String[] { "a", "b,c" }, parts);
         * // Both APIs limit the number of produced results; MapSplitter produces map entries.
         * }</pre>
         *
         * @param limit the maximum number of entries to return; must be &gt; 0.
         * @return this MapSplitter for method chaining.
         * @throws IllegalArgumentException if {@code limit} &lt;= 0.
         * @see Splitter#limit(int)
         */
        public MapSplitter limit(final int limit) throws IllegalArgumentException {
            N.checkArgPositive(limit, cs.limit);

            entrySplitter.limit(limit);

            return this;
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * using this MapSplitter's configuration. The order of entries is preserved
         * in the returned LinkedHashMap, maintaining insertion order.
         *
         * <p>This is the primary splitting method for creating maps from delimited
         * strings. Each entry is split by the entry delimiter, then each entry is
         * split by the key-value delimiter to produce key-value pairs.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> map = MapSplitter.with(",", "=")
         *     .split("name=John,age=30,city=NYC");
         * // Returns {name=John, age=30, city=NYC}
         * }</pre>
         *
         * @param source the CharSequence to split into a map; may be {@code null}.
         * @return a LinkedHashMap containing the parsed key-value pairs; returns an empty map if source is {@code null}.
         * @throws IllegalArgumentException if any entry string cannot be properly parsed into a key-value pair.
         * @see #splitToMap(CharSequence, Supplier)
         * @see #split(CharSequence, Class, Class)
         * @see #splitInto(CharSequence, Map)
         * @see #splitToImmutableMap(CharSequence)
         * @see #splitToStream(CharSequence)
         */
        public Map<String, String> split(final CharSequence source) throws IllegalArgumentException {
            final LinkedHashMap<String, String> result = new LinkedHashMap<>();

            splitInto(source, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types using this MapSplitter's configuration. Each key and
         * value string is automatically converted to the target types using the type
         * system's valueOf method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<Integer, Double> map = MapSplitter.with(",", ":")
         *     .split("1:1.5,2:2.5,3:3.5", Integer.class, Double.class);
         * // Returns {1=1.5, 2=2.5, 3=3.5}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Class representing the type to convert keys to, not {@code null}
         * @param valueType the Class representing the type to convert values to, not {@code null}
         * @return a LinkedHashMap containing the parsed and converted key-value pairs
         * @throws IllegalArgumentException if keyType or valueType is {@code null}, or if any entry string cannot be
         *         properly parsed into a key-value pair.
         * @throws RuntimeException if resolving a requested conversion type or converting a key or value fails
         * @see #split(CharSequence)
         * @see #split(CharSequence, Type, Type)
         * @see #splitToMap(CharSequence, Class, Class, Supplier)
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Class<K> keyType, final Class<V> valueType)
                throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);

            final Type<K> typeOfKey = Type.of(keyType);
            final Type<V> typeOfValue = Type.of(valueType);

            return split(source, typeOfKey, typeOfValue);
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types using the provided Type instances for conversion.
         * This method is useful when working with the type system directly or when
         * Class objects are insufficient for conversion needs.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Type<String> strType = N.typeOf(String.class);
         * Type<Integer> intType = N.typeOf(Integer.class);
         * Map<String, Integer> config = MapSplitter.with(",", "=")
         *     .split("timeout=30,retry=3", strType, intType);
         * // Returns {"timeout"=30, "retry"=3}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Type instance used for converting strings to keys, not {@code null}
         * @param valueType the Type instance used for converting strings to values, not {@code null}
         * @return a LinkedHashMap containing the parsed and converted key-value pairs
         * @throws IllegalArgumentException if keyType or valueType is {@code null}, or if any entry string cannot be
         *         properly parsed into a key-value pair.
         * @throws RuntimeException if converting a key or value fails
         * @see #split(CharSequence, Class, Class)
         * @see #splitToMap(CharSequence, Type, Type, Supplier)
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Type<K> keyType, final Type<V> valueType)
                throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);

            final LinkedHashMap<K, V> result = new LinkedHashMap<>();

            splitInto(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * using this MapSplitter's configuration and returns the results in a Map
         * created by the provided supplier. This allows control over the Map
         * implementation used to store results.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TreeMap<String, String> sorted = MapSplitter.with(",", "=").splitToMap("z=3,a=1,m=2", TreeMap::new);
         * // Returns a TreeMap with entries sorted by key
         * }</pre>
         *
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the parsed key-value pairs
         * @throws IllegalArgumentException if {@code supplier} is {@code null} or returns {@code null}, or if any
         *         entry string cannot be properly parsed into a key-value pair.
         * @throws UnsupportedOperationException if the supplied Map rejects insertion and at least one entry is
         *         produced.
         */
        public <M extends Map<String, String>> M splitToMap(final CharSequence source, final Supplier<? extends M> supplier)
                throws IllegalArgumentException, UnsupportedOperationException {
            N.checkArgNotNull(supplier, cs.supplier);

            final M result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

            splitInto(source, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types, and returns the results in a Map created by the
         * provided supplier. This provides control over both the conversion types and
         * the Map implementation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * TreeMap<String, Integer> sorted = MapSplitter.with(",", "=")
         *     .splitToMap("z=3,a=1,m=2", String.class, Integer.class, TreeMap::new);
         * // Returns a TreeMap with entries sorted by key
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the converted key-value pairs
         * @throws IllegalArgumentException if {@code keyType} or {@code valueType} is {@code null}, if {@code supplier}
         *         is {@code null} or returns {@code null}, or if any entry string cannot be properly parsed into a
         *         key-value pair.
         * @throws RuntimeException if resolving a requested conversion type or converting a key or value fails
         * @throws NullPointerException if a converted key or value is {@code null} and the destination rejects it
         * @throws ClassCastException if a converted key or value has a type that the destination cannot accept or compare
         * @throws UnsupportedOperationException if the destination map rejects insertion and an entry is produced
         */
        public <K, V, M extends Map<K, V>> M splitToMap(final CharSequence source, final Class<K> keyType, final Class<V> valueType,
                final Supplier<? extends M> supplier)
                throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(supplier, cs.supplier);

            final M result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

            splitInto(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * using the provided Type instances, and returns the results in a Map created
         * by the provided supplier. This method provides maximum flexibility with
         * Type-based conversion and custom Map implementation.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Type<String> strType = N.typeOf(String.class);
         * Type<Integer> intType = N.typeOf(Integer.class);
         * TreeMap<String, Integer> sorted = MapSplitter.with(",", "=")
         *     .splitToMap("z=3,a=1,m=2", strType, intType, TreeMap::new);
         * // Returns a TreeMap sorted by keys: {a=1, m=2, z=3}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the type of Map to return
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Type instance used for converting strings to keys
         * @param valueType the Type instance used for converting strings to values
         * @param supplier a Supplier that creates a new Map instance to hold the results
         * @return the Map created by the supplier, populated with the converted key-value pairs
         * @throws IllegalArgumentException if {@code keyType} or {@code valueType} is {@code null}, if {@code supplier}
         *         is {@code null} or returns {@code null}, or if any entry string cannot be properly parsed into a
         *         key-value pair.
         * @throws RuntimeException if converting a key or value fails
         * @throws NullPointerException if a converted key or value is {@code null} and the destination rejects it
         * @throws ClassCastException if a converted key or value has a type that the destination cannot accept or compare
         * @throws UnsupportedOperationException if the destination map rejects insertion and an entry is produced
         */
        public <K, V, M extends Map<K, V>> M splitToMap(final CharSequence source, final Type<K> keyType, final Type<V> valueType,
                final Supplier<? extends M> supplier)
                throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(supplier, cs.supplier);

            final M result = N.checkArgNotNull(supplier.get(), "The Supplier must not return null");

            splitInto(source, keyType, valueType, result);

            return result;
        }

        /**
         * Splits the specified CharSequence into string key-value pairs and adds
         * them to the provided output map. This method is useful for appending
         * parsed entries to an existing map.
         *
         * <p>Each entry must contain at least one key-value delimiter; an entry with no
         * key-value delimiter causes an {@link IllegalArgumentException}. Because the
         * key-value splitting is limited to two parts, if an entry contains more than one
         * key-value delimiter, the first occurrence separates the key from the value and the
         * value retains the remaining text (including any further delimiters).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> config = new HashMap<>();
         * MapSplitter.with(",", "=").splitInto("a=1,b=2", config);
         * MapSplitter.with(";", ":").splitInto("c:3;d:4", config);
         * // config now contains {a=1, b=2, c=3, d=4}
         * }</pre>
         *
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param output the Map to add the parsed key-value pairs to
         * @throws IllegalArgumentException if output is {@code null}, or if any entry string cannot be properly
         *         parsed into a key-value pair.
         * @throws UnsupportedOperationException if {@code output} rejects insertion (for example an immutable
         *         map) and at least one entry is produced.
         */
        public void splitInto(final CharSequence source, final Map<String, String> output) throws IllegalArgumentException, UnsupportedOperationException {
            N.checkArgNotNull(output, cs.output);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (!keyValueIter.hasNext()) {
                    throw new IllegalArgumentException(invalidEntryMessage(entryString));
                }

                key = keyValueIter.next();

                if (!keyValueIter.hasNext()) {
                    throw new IllegalArgumentException(invalidEntryMessage(entryString));
                }

                value = keyValueIter.next();

                output.put(key, value);
            }
        }

        /**
         * Splits the specified CharSequence into key-value pairs, converts them to
         * the specified types, and adds them to the provided output map. This method
         * is useful for appending converted entries to an existing map.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<Integer, String> data = new HashMap<>();
         * MapSplitter.with(",", ":").splitInto("1:apple,2:banana", Integer.class, String.class, data);
         * // data now contains {1=apple, 2=banana}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Class representing the type to convert keys to
         * @param valueType the Class representing the type to convert values to
         * @param output the Map to add the converted key-value pairs to
         * @throws IllegalArgumentException if keyType, valueType, or output is {@code null}, or if any entry string
         *         cannot be properly parsed into a key-value pair.
         * @throws RuntimeException if resolving a requested conversion type or converting a key or value fails
         * @throws NullPointerException if a converted key or value is {@code null} and the destination rejects it
         * @throws ClassCastException if a converted key or value has a type that the destination cannot accept or compare
         * @throws UnsupportedOperationException if the destination map rejects insertion and an entry is produced
         */
        public <K, V> void splitInto(final CharSequence source, final Class<K> keyType, final Class<V> valueType, final Map<K, V> output)
                throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(output, cs.output);

            final Type<K> typeOfKey = Type.of(keyType);
            final Type<V> typeOfValue = Type.of(valueType);

            splitInto(source, typeOfKey, typeOfValue, output);
        }

        /**
         * Splits the specified CharSequence into key-value pairs, converts them using
         * the provided Type instances, and adds them to the provided output map. This
         * method provides Type-based conversion for appending to existing maps.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Integer> config = new LinkedHashMap<>();
         * Type<String> strType = N.typeOf(String.class);
         * Type<Integer> intType = N.typeOf(Integer.class);
         *
         * MapSplitter.with(",", "=").splitInto("port=8080,timeout=30", strType, intType, config);
         * MapSplitter.with(";", ":").splitInto("retry:3;delay:500", strType, intType, config);
         * // config now contains {port=8080, timeout=30, retry=3, delay=500}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Type instance used for converting strings to keys
         * @param valueType the Type instance used for converting strings to values
         * @param output the Map to add the converted key-value pairs to
         * @throws IllegalArgumentException if keyType, valueType, or output is {@code null}, or if any entry string
         *         cannot be properly parsed into a key-value pair.
         * @throws RuntimeException if converting a key or value fails
         * @throws NullPointerException if a converted key or value is {@code null} and the destination rejects it
         * @throws ClassCastException if a converted key or value has a type that the destination cannot accept or compare
         * @throws UnsupportedOperationException if the destination map rejects insertion and an entry is produced
         */
        public <K, V> void splitInto(final CharSequence source, final Type<K> keyType, final Type<V> valueType, final Map<K, V> output)
                throws IllegalArgumentException, RuntimeException, NullPointerException, ClassCastException, UnsupportedOperationException {
            N.checkArgNotNull(keyType, cs.keyType);
            N.checkArgNotNull(valueType, cs.valueType);
            N.checkArgNotNull(output, cs.output);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (!keyValueIter.hasNext()) {
                    throw new IllegalArgumentException(invalidEntryMessage(entryString));
                }

                key = keyValueIter.next();

                if (!keyValueIter.hasNext()) {
                    throw new IllegalArgumentException(invalidEntryMessage(entryString));
                }

                value = keyValueIter.next();

                output.put(keyType.valueOf(key), valueType.valueOf(value));
            }
        }

        /**
         * Splits the specified CharSequence into a map of string key-value pairs
         * and returns the results as an ImmutableMap. The returned map cannot be modified,
         * providing a safe, read-only view of the parsed entries.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableMap<String, String> config = MapSplitter.with(",", "=")
         *     .splitToImmutableMap("host=localhost,port=8080");
         * // Returns an immutable map {host=localhost, port=8080}
         * }</pre>
         *
         * @param source the CharSequence to split into a map; may be {@code null}
         * @return an ImmutableMap containing the parsed key-value pairs
         * @throws IllegalArgumentException if any entry string cannot be properly parsed into a key-value pair.
         * @see #split(CharSequence)
         * @see #splitToImmutableMap(CharSequence, Class, Class)
         */
        public ImmutableMap<String, String> splitToImmutableMap(final CharSequence source) throws IllegalArgumentException {
            return ImmutableMap.wrap(split(source));
        }

        /**
         * Splits the specified CharSequence into a map with keys and values converted
         * to the specified types, and returns the results as an ImmutableMap.
         * The returned map cannot be modified, providing a safe, read-only view
         * of the converted entries.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableMap<Integer, String> data = MapSplitter.with(",", ":")
         *     .splitToImmutableMap("1:apple,2:banana", Integer.class, String.class);
         * // Returns an immutable map {1=apple, 2=banana}
         * }</pre>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param keyType the Class representing the type to convert keys to, not {@code null}
         * @param valueType the Class representing the type to convert values to, not {@code null}
         * @return an ImmutableMap containing the parsed and converted key-value pairs
         * @throws IllegalArgumentException if keyType or valueType is {@code null}, or if any entry string cannot be
         *         properly parsed into a key-value pair.
         * @throws RuntimeException if resolving a requested conversion type or converting a key or value fails
         * @see #splitToImmutableMap(CharSequence)
         * @see #split(CharSequence, Class, Class)
         */
        public <K, V> ImmutableMap<K, V> splitToImmutableMap(final CharSequence source, final Class<K> keyType, final Class<V> valueType)
                throws IllegalArgumentException, RuntimeException {
            return ImmutableMap.wrap(split(source, keyType, valueType));
        }

        /**
         * Splits the specified CharSequence into a Stream of Map.Entry objects.
         * Each entry represents a parsed key-value pair. This allows for lazy
         * evaluation and further stream operations on the entries without creating
         * an intermediate map.
         *
         * <p>The stream evaluation is lazy - entries are produced on-demand as the
         * stream is consumed, making this memory-efficient for large inputs.</p>
         *
         * <p><b>Do not reconfigure this {@code MapSplitter} while a returned stream is still unconsumed.</b>
         * The entry-level configuration is captured when the stream is created, while the key/value-level
         * configuration is read as each entry is consumed, so a change made in between would apply to only
         * half the pipeline.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * long count = MapSplitter.with(",", "=")
         *     .splitToStream("a=1,b=2,c=3")
         *     .filter(e -> e.getValue().equals("2"))
         *     .count();
         * // Returns 1
         * }</pre>
         *
         * <p>During traversal, the returned stream throws {@link IllegalArgumentException} if an entry
         * does not yield both a key and a value under the configured key-value splitter.</p>
         *
         * @param source the CharSequence to split into entries; may be {@code null}
         * @return a Stream of Map.Entry objects containing the parsed key-value pairs; returns an empty stream if source is {@code null}
         * @see #split(CharSequence)
         * @see #splitToEntryStream(CharSequence)
         */
        public Stream<Map.Entry<String, String>> splitToStream(final CharSequence source) {
            return Stream.of(new ObjIteratorEx<>() {
                private final ObjIterator<String> iter = entrySplitter.iterate(source);
                private Map.Entry<String, String> next;

                /**
                 * {@inheritDoc}
                 * @throws IllegalArgumentException if the next entry does not yield both a key and a value under the configured key-value splitter
                 */
                @Override
                public boolean hasNext() throws IllegalArgumentException {
                    if (next == null && iter.hasNext()) {
                        final String entryString = iter.next();
                        final ObjIterator<String> keyValueIter = keyValueSplitter.iterate(entryString);

                        if (!keyValueIter.hasNext()) {
                            throw new IllegalArgumentException(invalidEntryMessage(entryString));
                        }

                        final String key = keyValueIter.next();

                        if (!keyValueIter.hasNext()) {
                            throw new IllegalArgumentException(invalidEntryMessage(entryString));
                        }

                        next = new ImmutableEntry<>(key, keyValueIter.next());
                    }

                    return next != null;
                }

                /**
                 * {@inheritDoc}
                 * @throws IllegalArgumentException if the next entry does not yield both a key and a value under the configured key-value splitter
                 * @throws NoSuchElementException if no split token or map entry remains
                 */
                @Override
                public Map.Entry<String, String> next() throws IllegalArgumentException, NoSuchElementException {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Map.Entry<String, String> result = next;
                    next = null;
                    return result;
                }
            });
        }

        /**
         * Splits the specified CharSequence into an EntryStream of string key-value
         * pairs. EntryStream provides specialized operations for working with
         * key-value pairs, such as filtering by keys or values, mapping entries,
         * and collecting to maps.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, String> filtered = MapSplitter.with(",", "=")
         *     .splitToEntryStream("a=1,b=2,c=3")
         *     .filter(e -> !e.getKey().equals("b"))
         *     .toMap();
         * // Returns {a=1, c=3}
         * }</pre>
         *
         * <p>During traversal, the returned stream throws {@link IllegalArgumentException} if an entry
         * does not yield both a key and a value under the configured key-value splitter.</p>
         *
         * @param source the CharSequence to split into entries; may be {@code null}
         * @return an EntryStream containing the parsed key-value pairs; returns an empty EntryStream if source is {@code null}
         * @see #splitToStream(CharSequence)
         */
        public EntryStream<String, String> splitToEntryStream(final CharSequence source) {
            //noinspection resource
            return splitToStream(source).mapToEntry(Fn.identity());
        }

        /**
         * Splits the specified CharSequence into a map and applies the provided
         * function to transform the resulting map. This is useful for converting
         * or aggregating the map in a single operation, combining the split and
         * transformation steps.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * int valueSum = MapSplitter.with(",", "=")
         *     .splitThenApply("a=1,b=2,c=3", map ->
         *         map.values().stream()
         *             .mapToInt(Integer::parseInt)
         *             .sum());
         * // Returns 6
         * }</pre>
         *
         * @param <T> the type of the result
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param converter a function that transforms the parsed map into a result.
         * @return the result of applying the converter function to the parsed map
         * @throws IllegalArgumentException if the callback is {@code null}, or an entry does not yield both a key and a value
         * @see #split(CharSequence)
         * @see #splitThenAccept(CharSequence, Consumer)
         */
        public <T> T splitThenApply(final CharSequence source, final Function<? super Map<String, String>, T> converter) throws IllegalArgumentException {
            N.checkArgNotNull(converter, cs.converter);

            return converter.apply(split(source));
        }

        /**
         * Splits the specified CharSequence into a map and passes it to the provided
         * consumer. This is useful for performing side effects with the parsed map,
         * such as logging, validation, or populating external data structures.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * MapSplitter.with(",", "=").splitThenAccept("a=1,b=2", map -> {
         *     System.out.println("Parsed " + map.size() + " entries");
         *     map.forEach((k, v) -> System.out.println(k + " -> " + v));
         * });
         * }</pre>
         *
         * @param source the CharSequence to split into a map; may be {@code null}
         * @param consumer a consumer that processes the parsed map.
         * @throws IllegalArgumentException if the callback is {@code null}, or an entry does not yield both a key and a value
         * @see #split(CharSequence)
         * @see #splitThenApply(CharSequence, Function)
         */
        public void splitThenAccept(final CharSequence source, final Consumer<? super Map<String, String>> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer, cs.consumer);

            consumer.accept(split(source));
        }

        private static String invalidEntryMessage(final String entryString) {
            return "Invalid map entry String: \"" + entryString + "\". It does not contain the key-value delimiter";
        }
    }

    /**
     * The splitting engine shared by every {@link Strategy}. It owns the whole token policy &mdash;
     * trimming/stripping, {@link Splitter#omitEmptyStrings() empty-token filtering} and
     * {@link Splitter#limit(int) limiting} &mdash; and delegates only the question "where does the next
     * separator sit" to a subclass, so the character, string and pattern engines cannot drift apart.
     *
     * <p>The order of those three steps is the contract: a token is materialized (and therefore trimmed or
     * stripped) first, then dropped if it is empty and empty tokens are omitted, and only then counted
     * against the limit. Tokens dropped by {@code omitEmptyStrings()} therefore neither consume the limit
     * nor survive inside the final element.</p>
     */
    abstract static class SplitIterator extends ObjIterator<String> {

        /** The sequence being split; never {@code null}. */
        final CharSequence source;

        /** {@code source.length()}, hoisted out of the scan loop. */
        final int sourceLen;

        /**
         * Start of the token currently being built, which is also the index the separator scan resumes from:
         * it is always the position just past the previously located separator.
         */
        int start = 0;

        /** Start of the separator located by the most recent {@link #nextSeparator()} call that returned {@code true}. */
        int separatorStart = 0;

        /** End (exclusive) of that separator. */
        int separatorEnd = 0;

        private final SubstringFunc substringFunc;

        private final boolean omitEmptyStrings;

        private final int limit;

        private String next = null;

        private int cnt = 0;

        private boolean done = false;

        SplitIterator(final CharSequence source, final boolean omitEmptyStrings, final boolean trim, final boolean strip, final int limit) {
            this.source = source;
            sourceLen = source.length();
            substringFunc = strip ? stripSubstringFunc : (trim ? trimSubstringFunc : defaultSubstringFunc);
            this.omitEmptyStrings = omitEmptyStrings;
            this.limit = limit;
        }

        /**
         * Locates the next separator at or after {@link #start} and reports it through {@link #separatorStart}
         * and {@link #separatorEnd}.
         *
         * <p>Implementations must guarantee progress, either by reporting a {@code separatorEnd} greater than
         * the {@code start} they were called with, or by advancing their own scan position. The literal engines
         * do the former; the pattern engine relies on the latter, because a zero-width match reports
         * {@code separatorEnd == separatorStart == start} and it is {@link java.util.regex.Matcher#find()}
         * itself that resumes one character further next time.</p>
         *
         * @return {@code true} if a separator was found; {@code false} if the rest of the input is the final token
         */
        abstract boolean nextSeparator();

        @Override
        public boolean hasNext() {
            while (next == null && !done) {
                final int tokenStart = start;
                final int tokenEnd;

                if (nextSeparator()) {
                    tokenEnd = separatorStart;
                    start = separatorEnd;
                } else {
                    tokenEnd = sourceLen;
                    done = true;
                }

                String token = substringFunc.substring(source, tokenStart, tokenEnd);

                if (omitEmptyStrings && token.isEmpty()) {
                    // Dropped before the limit is consulted, so an omitted token neither consumes the limit nor
                    // gets absorbed into the final element below - the latter would push separator characters
                    // back into a result the caller asked to be free of empty fields.
                    continue;
                }

                if (!done && limit - cnt == 1) {
                    // This is the last token the limit allows, so it takes the remainder of the input. Re-running
                    // substringFunc over the widened range trims/strips the newly added tail. The widened token
                    // cannot be empty: widening only adds characters to the right of the non-blank character that
                    // made the un-widened token non-empty, and trimming never removes that character.
                    token = substringFunc.substring(source, tokenStart, sourceLen);
                    done = true;
                }

                next = token;
            }

            return next != null;
        }

        /**
         * {@inheritDoc}
         * @throws NoSuchElementException if no split token or map entry remains
         */
        @Override
        public String next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }

            final String result = next;
            next = null;
            cnt++;

            return result;
        }
    }

    /**
     * The Interface Strategy defines the core splitting behavior for different delimiter types.
     * Each delimiter type (character, string, pattern) implements this interface to provide
     * its specific splitting logic.
     */
    interface Strategy {

        /**
         * Splits the specified CharSequence according to the strategy's delimiter type
         * and the provided configuration options. This method returns an iterator that
         * produces split substrings lazily on-demand.
         *
         * @param toSplit the CharSequence to be split; may be {@code null}
         * @param omitEmptyStrings {@code true} to omit empty strings from results
         * @param trim {@code true} to trim leading and trailing spaces from each substring
         * @param strip {@code true} to strip leading and trailing whitespace from each substring
         * @param limit the maximum number of substrings to produce
         * @return an ObjIterator that produces split substrings; returns an empty iterator if toSplit is null
         */
        ObjIterator<String> split(CharSequence toSplit, boolean omitEmptyStrings, boolean trim, final boolean strip, int limit);
    }

    /**
     * The Interface SubstringFunc defines a function for extracting substrings with
     * optional preprocessing. Different implementations can trim or strip whitespace
     * from the extracted substring before returning it.
     */
    interface SubstringFunc {

        /**
         * Extracts a substring from the specified source CharSequence, optionally
         * processing it (e.g., trimming or stripping whitespace) before returning.
         * The behavior depends on the specific implementation.
         *
         * @param source the source CharSequence to extract from
         * @param start the starting index (inclusive) of the substring
         * @param end the ending index (exclusive) of the substring
         * @return the extracted and optionally processed substring
         */
        String substring(CharSequence source, int start, int end);
    }
}
