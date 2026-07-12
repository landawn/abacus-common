/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.exception.ParsingException;

/**
 * A very simple CSV parser that parses a single CSV line into fields.
 * It supports quoted values, escaped characters, and customizable delimiters.
 * This parser is released under a commercial-friendly license.
 *
 * <p>Key features:</p>
 * <ul>
 * <li>Customizable separator, quote, and escape characters</li>
 * <li>Handles quoted fields with embedded delimiters</li>
 * <li>Supports escaped characters within quoted fields</li>
 * <li>Options for strict quote handling and whitespace trimming</li>
 * <li>Can ignore quotation marks entirely for simple parsing</li>
 * </ul>
 *
 * <p><b>Divergences from RFC 4180 (opencsv-style dialect):</b></p>
 * <ul>
 * <li>Inside quoted fields, both RFC 4180 quote-doubling ({@code ""}) and backslash escapes
 *     ({@code \"} and {@code \\}) are recognized; RFC 4180 defines only the doubled-quote form</li>
 * <li>With the default settings, surrounding whitespace of unquoted fields is stripped and
 *     characters outside the quotes are kept as part of the field (RFC 4180 preserves whitespace
 *     and does not define text outside quotes)</li>
 * <li>This parser operates on a single line; multi-line records (quoted fields containing
 *     literal line breaks) must be joined by the caller before parsing</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic usage with default settings (comma separator, double-quote, backslash escape)
 * CsvParser parser = new CsvParser();
 * List<String> fields = parser.parseLine("John,Doe,30,\"New York, NY\"");
 * // Result: ["John", "Doe", "30", "New York, NY"]
 *
 * // Custom separator
 * CsvParser tabParser = new CsvParser('\t');
 * String[] tabFields = tabParser.parseLineToArray("John\tDoe\t30");
 * // Result: ["John", "Doe", "30"]
 *
 * // Parse with strict quotes (ignores characters outside quotes)
 * CsvParser strictParser = new CsvParser(',', '"', '\\', true);
 * List<String> result = strictParser.parseLine("\"clean\"dirty,\"text\"");
 * // Result: ["clean", "text"] - the unquoted "dirty" outside the quotes is ignored
 * }</pre>
 *
 * @author Glen Smith
 * @author Rainer Pruy
 * @see com.landawn.abacus.util.CsvUtil
 */
public class CsvParser {

    /**
     * The default separator character (comma).
     * Used if no separator is specified in the constructor.
     */
    public static final char DEFAULT_SEPARATOR = ',';

    /**
     * Suggested initial read size for string builders (inherited from opencsv).
     * Not used by this implementation; retained for API compatibility.
     */
    public static final int INITIAL_READ_SIZE = 1024;

    /**
     * Suggested extra buffer size when the exact line size is known (inherited from opencsv).
     * Not used by this implementation; retained for API compatibility.
     */
    public static final int READ_BUFFER_SIZE = 128;

    /**
     * The default quote character (double quote).
     * Used to enclose fields containing special characters.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * The default escape character (backslash).
     * Used inside quoted fields to escape the quote character or the escape character itself.
     * Other characters are not escaped.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = '\\';

    /**
     * The default strict quotes behavior.
     * When {@code false}, characters outside quotes are included in the field value.
     */
    public static final boolean DEFAULT_STRICT_QUOTES = false;

    /**
     * The default leading whitespace behavior ({@code true}).
     * When {@code true}, whitespace surrounding unquoted fields is stripped (leading and trailing)
     * and any whitespace characters immediately following a separator are skipped before the next field.
     */
    public static final boolean DEFAULT_IGNORE_LEADING_WHITESPACE = true;

    /**
     * The default quotation-handling behavior ({@code false}).
     * When {@code false}, quote characters are processed normally as field delimiters.
     */
    public static final boolean DEFAULT_IGNORE_QUOTATIONS = false;

    /**
     * The {@code null} character constant.
     * Used to indicate that a character parameter should be ignored.
     */
    public static final char NULL_CHARACTER = '\0';

    /**
     * This is the character that the CsvParser will treat as the separator.
     */
    private final char separator;
    /**
     * This is the character that the CsvParser will treat as the quotation character.
     */
    private final char quoteChar;
    /**
     * This is the character that the CsvParser will treat as the escape character.
     */
    private final char escape;
    /**
     * Determines if the field is between quotes (true) or between separators (false).
     */
    private final boolean strictQuotes;
    /**
     * Whether to ignore any leading white space at the start of the field.
     */
    private final boolean ignoreLeadingWhitespace;
    /**
     * Whether to skip over quotation characters when parsing.
     */
    private final boolean ignoreQuotations;

    /**
     * Constructs a CsvParser using default settings.
     * Uses comma as separator, double-quote for quoting, and backslash for escaping.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * List<String> fields = parser.parseLine("a,b,c");
     * }</pre>
     *
     */
    public CsvParser() {
        this(DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs a CsvParser with a custom separator.
     * Uses default quote (") and escape (\) characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser('|');
     * List<String> fields = parser.parseLine("a|b|c");
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     */
    public CsvParser(final char separator) {
        this(separator, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs a CsvParser with custom separator and quote characters.
     * Uses default escape character (\).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '\'');
     * List<String> fields = parser.parseLine("a,'b,c',d");
     * // Result: ["a", "b,c", "d"]
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     */
    public CsvParser(final char separator, final char quoteChar) {
        this(separator, quoteChar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs a CsvParser with custom separator, quote, and escape characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\');
     * List<String> fields = parser.parseLine("a,\"b\\\"c\",d");
     * // Result: ["a", "b\"c", "d"]
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     */
    public CsvParser(final char separator, final char quoteChar, final char escape) {
        this(separator, quoteChar, escape, DEFAULT_STRICT_QUOTES);
    }

    /**
     * Constructs a CsvParser with custom settings including strict quotes mode.
     * In strict quotes mode, characters outside the quotes are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\', true);
     * List<String> fields = parser.parseLine("\"clean\"dirty,\"text\"");
     * // With strictQuotes=true: ["clean", "text"] - "dirty" outside the quotes is ignored
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     * @param strictQuotes if {@code true}, characters outside the quotes are ignored
     */
    public CsvParser(final char separator, final char quoteChar, final char escape, final boolean strictQuotes) {
        this(separator, quoteChar, escape, strictQuotes, DEFAULT_IGNORE_LEADING_WHITESPACE);
    }

    /**
     * Constructs a CsvParser with custom settings including whitespace handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\', false, true);
     * List<String> fields = parser.parseLine("  a  ,  b  ,  c  ");
     * // With ignoreLeadingWhitespace=true: ["a", "b", "c"]
     * // With ignoreLeadingWhitespace=false: ["  a  ", "  b  ", "  c  "]
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     * @param strictQuotes if {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhitespace if {@code true}, surrounding whitespace is stripped from unquoted
     *        fields and whitespace immediately after a separator is skipped
     */
    public CsvParser(final char separator, final char quoteChar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhitespace) {
        this(separator, quoteChar, escape, strictQuotes, ignoreLeadingWhitespace, DEFAULT_IGNORE_QUOTATIONS);
    }

    /**
     * Constructs a CsvParser with full control over all parsing options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parser that removes quote characters but does not let quoted regions protect separators
     * CsvParser parser = new CsvParser(',', '"', '\\', false, true, true);
     * List<String> fields = parser.parseLine("\"a\",\"b\",\"c\"");
     * // Result: ["a", "b", "c"] - quote characters are still consumed, but the
     * // regions they delimit are not treated as quoted (e.g. embedded separators
     * // would not be protected)
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     * @param strictQuotes if {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhitespace if {@code true}, surrounding whitespace is stripped from unquoted
     *        fields and whitespace immediately after a separator is skipped
     * @param ignoreQuotations if {@code true}, quote characters are consumed but quoted regions do not
     *        protect separators or escapes
     * @throws UnsupportedOperationException if any two of separator, quoteChar, and escape are the same
     *         non-{@link #NULL_CHARACTER} characters, or if separator is {@link #NULL_CHARACTER}
     */
    public CsvParser(final char separator, final char quoteChar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhitespace,
            final boolean ignoreQuotations) {
        if (anyCharactersAreTheSame(separator, quoteChar, escape)) {
            throw new UnsupportedOperationException("The separator, quote, and escape characters must be different!");
        }

        if (separator == NULL_CHARACTER) {
            throw new UnsupportedOperationException("The separator character must be defined!");
        }

        this.separator = separator;
        this.quoteChar = quoteChar;
        this.escape = escape;
        this.strictQuotes = strictQuotes;
        this.ignoreLeadingWhitespace = ignoreLeadingWhitespace;
        this.ignoreQuotations = ignoreQuotations;
    }

    /**
     * Checks to see if any two of the three characters are the same.
     * This is because in opencsv the separator, quote, and escape characters must be different.
     *
     * @param separator the defined separator character
     * @param quoteChar the defined quotation character
     * @param escape the defined escape character
     * @return {@code true} if any two of the three are the same, {@code false} otherwise
     */
    private boolean anyCharactersAreTheSame(final char separator, final char quoteChar, final char escape) {
        return isSameCharacter(separator, quoteChar) || isSameCharacter(separator, escape) || isSameCharacter(quoteChar, escape);
    }

    /**
     * Checks that the two characters are the same and are not the defined {@code NULL_CHARACTER}.
     *
     * @param c1 the first character
     * @param c2 the second character
     * @return {@code true} if both characters are the same and are not the defined {@code NULL_CHARACTER}
     */
    private boolean isSameCharacter(final char c1, final char c2) {
        return c1 != NULL_CHARACTER && c1 == c2;
    }

    /**
     * Returns the separator character used by this parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser('|');
     * char sep = parser.getSeparator();   // returns '|'
     * }</pre>
     *
     * @return the separator character
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Returns the quotation character used by this parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '\'');
     * char quote = parser.getQuoteChar();   // returns '\''
     * }</pre>
     *
     * @return the quotation character
     */
    public char getQuoteChar() {
        return quoteChar;
    }

    /**
     * Returns the escape character used by this parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * char esc = parser.getEscape();   // returns '\\'
     * }</pre>
     *
     * @return the escape character
     */
    public char getEscape() {
        return escape;
    }

    /**
     * Returns whether this parser uses strict quotes mode.
     * In strict quotes mode, characters outside quotes are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\', true);
     * boolean strict = parser.isStrictQuotes();   // returns true
     * }</pre>
     *
     * @return {@code true} if strict quotes mode is enabled
     */
    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    /**
     * Returns whether this parser ignores leading whitespace.
     * When {@code true}, surrounding whitespace is stripped from unquoted fields and whitespace
     * immediately after a separator is skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\', false, true);
     * boolean ignoreWs = parser.isIgnoreLeadingWhitespace();   // returns true
     * }</pre>
     *
     * @return {@code true} if leading (and trailing) whitespace around unquoted fields is ignored
     */
    public boolean isIgnoreLeadingWhitespace() {
        return ignoreLeadingWhitespace;
    }

    /**
     * Returns whether this parser ignores quoted-region protection.
     * When {@code true}, quote characters are consumed but quoted regions do not protect separators or escapes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser(',', '"', '\\', false, true, true);
     * boolean ignoreQuotes = parser.isIgnoreQuotations();   // returns true
     * }</pre>
     *
     * @return {@code true} if quotations are ignored
     */
    public boolean isIgnoreQuotations() {
        return ignoreQuotations;
    }

    /**
     * Parses a CSV line into a list of fields.
     * This method handles quoted fields, escaped characters, and separators according
     * to the parser's configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * List<String> fields = parser.parseLine("John,\"Doe, Jr.\",30,\"New York\"");
     * // Result: ["John", "Doe, Jr.", "30", "New York"]
     * }</pre>
     *
     * @param nextLine the CSV line to be parsed; may be {@code null}
     * @return a {@code List} of parsed field values; an empty list if {@code nextLine} is {@code null}
     * @throws ParsingException if the line contains an unterminated quoted field
     */
    public List<String> parseLine(final String nextLine) throws ParsingException {
        return parseLine(nextLine, null);
    }

    /**
     * Parses a CSV line into an array of fields.
     * This is a convenience method equivalent to {@link #parseLine(String)} that returns an array
     * instead of a {@code List}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * String[] fields = parser.parseLineToArray("a,b,c");
     * // Result: ["a", "b", "c"]
     * }</pre>
     *
     * @param nextLine the CSV line to be parsed; may be {@code null}
     * @return an array of parsed field values; an empty array if {@code nextLine} is {@code null}
     * @throws ParsingException if the line contains an unterminated quoted field
     */
    public String[] parseLineToArray(final String nextLine) throws ParsingException {
        final List<String> ret = parseLine(nextLine, null);

        return ret.toArray(new String[0]);
    }

    /**
     * Parses a CSV line into a pre-allocated array.
     * Parsed field values are written into {@code output} starting at index 0.
     * This variant avoids array allocation and is useful when parsing many lines
     * that have the same, known number of fields.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * String[] output = new String[4];
     * parser.parseLineToArray("a,b,c,d", output);
     * // output now contains: ["a", "b", "c", "d"]
     * }</pre>
     *
     * @param nextLine the CSV line to be parsed; may be {@code null} (no values are written to {@code output})
     * @param output the pre-allocated array to receive parsed field values; must not be {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws ParsingException if the line contains an unterminated quoted field
     * @throws ArrayIndexOutOfBoundsException if {@code output} is too small to hold all parsed fields
     */
    public void parseLineToArray(final String nextLine, final String[] output) throws ParsingException {
        N.checkArgNotNull(output, "output");

        parseLine(nextLine, output);
    }

    /**
     * Parses a CSV line and returns the parsed fields, optionally writing them into a pre-allocated array.
     *
     * <p>If {@code output} is {@code null}, the parsed values are collected into a new {@code List} which
     * is returned. If {@code output} is not {@code null}, parsed values are written into it starting at
     * index 0 and {@code null} is returned. If {@code nextLine} is {@code null}, an empty list is returned
     * (and {@code output} is not written to).</p>
     *
     * @param nextLine the CSV line to parse; may be {@code null}
     * @param output an optional pre-allocated array to receive parsed values; may be {@code null}
     * @return the {@code List} of parsed field values when {@code output} is {@code null};
     *         {@code null} when {@code output} is provided; an empty list if {@code nextLine} is {@code null}
     * @throws ParsingException if the line contains an unterminated quoted field
     * @throws ArrayIndexOutOfBoundsException if {@code output} is non-{@code null} and too small to hold all parsed fields
     */
    protected List<String> parseLine(final String nextLine, final String[] output) throws ParsingException {
        if (nextLine == null) {
            return N.emptyList();
        }

        final int len = nextLine.length();
        final boolean isOutputNull = output == null;
        final List<String> tokensOnThisLine = isOutputNull ? new ArrayList<>() : null;

        boolean inQuotes = false;
        boolean quoted = false;
        int idx = 0;

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;

            // Skip leading whitespace of the first field (mirrors the post-separator skip below), but
            // never skip the separator itself (a whitespace separator marks an empty first field). Without
            // this, leading whitespace before a quoted first field lands in sb and makes the opening quote
            // be mis-classified as embedded (see the isEmbedded check), corrupting or failing the parse.
            while (ignoreLeadingWhitespace && i < len && nextLine.charAt(i) != separator && Character.isWhitespace(nextLine.charAt(i))) {
                i++;
            }

            for (; i < len; i++) {
                final char c = nextLine.charAt(i);

                if (c == escape) {
                    if (isNextCharacterEscapable(nextLine, inQuotes(inQuotes), i)) {
                        sb.append(nextLine.charAt(++i));
                    } else {
                        sb.append(c); // escape character is not followed by a quote or escape, so append it.
                    }
                } else if (c == quoteChar) {
                    if (isNextCharacterEscapedQuote(nextLine, inQuotes(inQuotes), i)) {
                        sb.append(nextLine.charAt(++i));
                    } else {
                        // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                        boolean isEmbedded = !strictQuotes && !sb.isEmpty() //
                                && i > 0 //not on the beginning of the line //NOSONAR
                                && nextLine.charAt(i - 1) != separator //not at the beginning of an escape sequence
                                && i < len - 1 && nextLine.charAt(i + 1) != separator; //not at the end of an escape sequence

                        // A quote inside a quoted field followed by only whitespace up to the next
                        // separator (or end of line) is the CLOSING quote, not an embedded one:
                        // a,"b" ,c must parse like its end-of-line twin a,b,"c" (see the repair below).
                        if (isEmbedded && inQuotes(inQuotes) && ignoreLeadingWhitespace) {
                            int j = i + 1;

                            while (j < len && nextLine.charAt(j) != separator && Character.isWhitespace(nextLine.charAt(j))) {
                                j++;
                            }

                            if (j >= len || nextLine.charAt(j) == separator) {
                                isEmbedded = false;
                                i = j - 1; // consume the whitespace between the closing quote and the separator
                            }
                        }

                        if (isEmbedded) {
                            sb.append(c);
                        } else {
                            quoted = true;
                            inQuotes = !inQuotes;
                        }
                    }
                } else if (c == separator && !inQuotes(inQuotes)) {
                    if (!quoted && ignoreLeadingWhitespace) {
                        if (isOutputNull) {
                            tokensOnThisLine.add(Strings.strip(sb.toString()));
                        } else {
                            output[idx++] = Strings.strip(sb.toString());
                        }
                    } else {
                        if (isOutputNull) {
                            tokensOnThisLine.add(sb.toString());
                        } else {
                            output[idx++] = sb.toString();
                        }
                    }

                    // Skip whitespace between the separator and the next field, but never skip the
                    // separator itself: when the separator is a whitespace character (e.g. tab),
                    // consuming it here would silently swallow empty fields ("a\t\tb" -> ["a","b"]).
                    while (ignoreLeadingWhitespace && i < len - 1 && nextLine.charAt(i + 1) != separator && Character.isWhitespace(nextLine.charAt(i + 1))) {
                        i++;
                    }

                    sb.setLength(0);
                    quoted = false;
                    inQuotes = false;
                } else {
                    if (!strictQuotes || inQuotes(inQuotes)) {
                        sb.append(c);
                    }
                }
            }

            // line is done - check status
            if (inQuotes(inQuotes)) {
                int lastQuoteIndex = sb.lastIndexOf(String.valueOf(quoteChar));

                if (lastQuoteIndex >= 0 && Strings.isBlank(sb.substring(lastQuoteIndex + 1))) {
                    // Tested with: new CsvParser().parseLine("a,  b  ,  \"c\" ");
                    sb.setLength(lastQuoteIndex); // remove the last quote character
                } else {
                    throw new ParsingException("Un-terminated quoted field at end of CSV line: " + nextLine);
                }
            }

            if (!quoted && ignoreLeadingWhitespace) {
                if (isOutputNull) {
                    tokensOnThisLine.add(Strings.strip(sb.toString()));
                } else {
                    output[idx++] = Strings.strip(sb.toString());
                }
            } else {
                if (isOutputNull) {
                    tokensOnThisLine.add(sb.toString());
                } else {
                    output[idx++] = sb.toString();
                }
            }
        } finally {
            Objectory.recycle(sb);
        }

        return tokensOnThisLine;
    }

    /**
     * Determines if we can process as if we were in quotes.
     *
     * @param inQuotes whether the parser is currently inside quotes
     * @return {@code true} if we should process as if we are inside quotes, {@code false} otherwise
     */
    private boolean inQuotes(final boolean inQuotes) {
        return inQuotes && !ignoreQuotations;
    }

    /**
     * Checks to see if the character after the index is a quotation character.
     *
     * <p>Precondition: the current character is a quote or an escape.</p>
     *
     * @param nextLine the current line
     * @param inQuotes {@code true} if the current context is quoted
     * @param i the current index in the line
     * @return {@code true} if the following character is a quote, {@code false} otherwise
     */
    private boolean isNextCharacterEscapedQuote(final String nextLine, final boolean inQuotes, final int i) {
        return inQuotes // we are in quotes; therefore, there can be escaped quotes in here.
                && i < nextLine.length() - 1 // there is indeed another character to check.
                && nextLine.charAt(i + 1) == quoteChar; // the next character is a quote.;
    }

    /**
     * Checks to see if the character after the current index in a String is an
     * escapable character.
     * Meaning the next character is either a quotation character or the escape
     * char if it's inside quotes.
     *
     * <p>Precondition: the current character is an escape.</p>
     *
     * @param nextLine the current line
     * @param inQuotes {@code true} if the current context is quoted
     * @param i the current index in the line
     * @return {@code true} if the following character is escapable, {@code false} otherwise
     */
    private boolean isNextCharacterEscapable(final String nextLine, final boolean inQuotes, final int i) {
        return inQuotes // we are in quotes; therefore, there can be escaped quotes in here.
                && i < nextLine.length() - 1 // there is indeed another character to check.
                && isCharacterEscapable(nextLine.charAt(i + 1));
    }

    /**
     * Checks to see if the character passed in could be escapable.
     * Escapable characters for opencsv are the quotation character or the
     * escape character.
     *
     * @param c the source character
     * @return {@code true} if the character could be escapable, {@code false} otherwise
     */
    private boolean isCharacterEscapable(final char c) {
        return c == quoteChar || c == escape;
    }
}
