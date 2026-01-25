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

import com.landawn.abacus.exception.ParseException;

/**
 * A very simple CSV parser released under a commercial-friendly license.
 * This parser is designed to handle the parsing of a single CSV line into fields,
 * with support for quoted values, escaped characters, and customizable delimiters.
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
 * // Result: ["clean", "text"] - "dirty" is ignored
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
     * The initial read size for string builders.
     * Used for setting the initial capacity of internal buffers.
     */
    public static final int INITIAL_READ_SIZE = 1024;

    /**
     * Buffer size to add when the exact line size is known.
     * Provides extra space to avoid reallocation during parsing.
     */
    public static final int READ_BUFFER_SIZE = 128;

    /**
     * The default quote character (double quote).
     * Used to enclose fields containing special characters.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * The default escape character (backslash).
     * Used to escape quotes or other special characters within fields.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = '\\';

    /**
     * The default strict quotes behavior.
     * When {@code false}, characters outside quotes are included in the field value.
     */
    public static final boolean DEFAULT_STRICT_QUOTES = false;

    /**
     * The default leading whitespace behavior.
     * When {@code true}, leading whitespace is trimmed from unquoted fields.
     */
    public static final boolean DEFAULT_IGNORE_LEADING_WHITESPACE = true;

    /**
     * The default behavior for quotation handling.
     * When {@code false}, quote characters are processed normally.
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
     * Ignore any leading white space at the start of the field.
     */
    private final boolean ignoreLeadingWhiteSpace;
    /**
     * Skip over quotation characters when parsing.
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
     * // With strictQuotes=true: ["clean", "text"]
     * // With strictQuotes=false: ["cleandirty", "text"]
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
     * // With ignoreLeadingWhiteSpace=true: ["a", "b", "c"]
     * // With ignoreLeadingWhiteSpace=false: ["  a  ", "  b  ", "  c  "]
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     * @param strictQuotes if {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace if {@code true}, white space in front of a quote in a field is ignored
     */
    public CsvParser(final char separator, final char quoteChar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhiteSpace) {
        this(separator, quoteChar, escape, strictQuotes, ignoreLeadingWhiteSpace, DEFAULT_IGNORE_QUOTATIONS);
    }

    /**
     * Constructs a CsvParser with full control over all parsing options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parser that ignores quotes entirely
     * CsvParser parser = new CsvParser(',', '"', '\\', false, true, true);
     * List<String> fields = parser.parseLine("\"a\",\"b\",\"c\"");
     * // Result: ["\"a\"", "\"b\"", "\"c\""] - quotes are treated as regular characters
     * }</pre>
     *
     * @param separator the delimiter to use for separating entries
     * @param quoteChar the character to use for quoted elements
     * @param escape the character to use for escaping a separator or quote
     * @param strictQuotes if {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace if {@code true}, white space in front of a quote in a field is ignored
     * @param ignoreQuotations if {@code true}, treat quotations like any other character
     * @throws UnsupportedOperationException if separator, quote, and escape are not all different,
     *         or if separator is NULL_CHARACTER
     */
    public CsvParser(final char separator, final char quoteChar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhiteSpace,
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
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        this.ignoreQuotations = ignoreQuotations;
    }

    /**
     * Checks to see if any two of the three characters are the same.
     * This is because in opencsv the separator, quote, and escape characters must be different.
     *
     * @param separator The defined separator character
     * @param quoteChar The defined quotation character
     * @param escape The defined escape character
     * @return True, if any two of the three are the same.
     */
    private boolean anyCharactersAreTheSame(final char separator, final char quoteChar, final char escape) {
        return isSameCharacter(separator, quoteChar) || isSameCharacter(separator, escape) || isSameCharacter(quoteChar, escape);
    }

    /**
     * Checks that the two characters are the same and are not the defined NULL_CHARACTER.
     * @param c1 First character
     * @param c2 Second character
     * @return True if both characters are the same and are not the defined NULL_CHARACTER
     */
    private boolean isSameCharacter(final char c1, final char c2) {
        return c1 != NULL_CHARACTER && c1 == c2;
    }

    /**
     * Returns the separator character used by this parser.
     *
     * @return the separator character
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Returns the quotation character used by this parser.
     *
     * @return the quotation character
     */
    public char getQuoteChar() {
        return quoteChar;
    }

    /**
     * Returns the escape character used by this parser.
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
     * @return {@code true} if strict quotes mode is enabled
     */
    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    /**
     * Returns whether this parser ignores leading whitespace.
     * When {@code true}, whitespace before a quote character is ignored.
     *
     * @return {@code true} if leading whitespace is ignored
     */
    public boolean isIgnoreLeadingWhiteSpace() {
        return ignoreLeadingWhiteSpace;
    }

    /**
     * Returns whether this parser ignores quotation marks.
     * When {@code true}, quotes are treated as regular characters.
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
     * @param nextLine the line to be parsed
     * @return a List of String values, or an empty list if nextLine is null
     * @throws ParseException if the line contains an unterminated quoted field
     */
    public List<String> parseLine(final String nextLine) throws ParseException {
        return parseLine(nextLine, null);
    }

    /**
     * Parses a CSV line into an array of fields.
     * This is a convenience method that returns an array instead of a List.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * String[] fields = parser.parseLineToArray("a,b,c");
     * // Result: ["a", "b", "c"]
     * }</pre>
     *
     * @param nextLine the line to be parsed
     * @return an array of String values, or an empty array if nextLine is null
     * @throws ParseException if the line contains an unterminated quoted field
     */
    public String[] parseLineToArray(final String nextLine) throws ParseException {
        final List<String> ret = parseLine(nextLine, null);

        return ret.toArray(new String[0]);
    }

    /**
     * Parses a CSV line into a pre-allocated array.
     * This method is useful for performance when parsing many lines with the same
     * number of fields, as it avoids array allocation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CsvParser parser = new CsvParser();
     * String[] output = new String[4];
     * parser.parseLineToArray("a,b,c,d", output);
     * // output now contains: ["a", "b", "c", "d"]
     * }</pre>
     *
     * @param nextLine the line to be parsed
     * @param output the pre-allocated array to fill with parsed values
     * @throws ParseException if the line contains an unterminated quoted field
     * @throws IllegalArgumentException if output is null
     */
    public void parseLineToArray(final String nextLine, final String[] output) throws ParseException {
        N.checkArgNotNull(output, "output");

        parseLine(nextLine, output);
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine The string to parse
     * @return The comma-tokenized list of elements, or {@code null} if nextLine is null
     * @throws ParseException If bad things happen during the read
     */
    protected List<String> parseLine(final String nextLine, final String[] output) throws ParseException {
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
            for (int i = 0; i < len; i++) {
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
                        if (!strictQuotes && !sb.isEmpty() //
                                && i > 2 //not on the beginning of the line //NOSONAR
                                && nextLine.charAt(i - 1) != separator //not at the beginning of an escape sequence
                                && i < len - 1 && nextLine.charAt(i + 1) != separator //not at the end of an escape sequence
                        ) {
                            sb.append(c);
                        } else {
                            quoted = true;
                            inQuotes = !inQuotes;
                        }
                    }
                } else if (c == separator && !inQuotes(inQuotes)) {
                    if (!quoted && ignoreLeadingWhiteSpace) {
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

                    while (ignoreLeadingWhiteSpace && i < len - 1 && Character.isWhitespace(nextLine.charAt(i + 1))) {
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
                    throw new ParseException("Un-terminated quoted field at end of CSV line");
                }
            }

            if (!quoted && ignoreLeadingWhiteSpace) {
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
     * @return True if we should process as if we are inside quotes.
     */
    private boolean inQuotes(final boolean inQuotes) {
        return inQuotes && !ignoreQuotations;
    }

    /**
     * Checks to see if the character after the index is a quotation character.
     *
     * Precondition: the current character is a quote or an escape.
     *
     * @param nextLine The current line
     * @param inQuotes True if the current context is quoted
     * @param i Current index in line
     * @return True if the following character is a quote
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
     * Precondition: the current character is an escape.
     *
     * @param nextLine The current line
     * @param inQuotes True if the current context is quoted
     * @param i Current index in line
     * @return True if the following character is a quote
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
     * @param c Source character
     * @return True if the character could be escapable.
     */
    private boolean isCharacterEscapable(final char c) {
        return c == quoteChar || c == escape;
    }
}
