/*
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.landawn.abacus.exception.ParseException;

/**
 * A very simple CSV parser released under a commercial-friendly license.
 * This just implements splitting a single line into fields.
 *
 * @author Glen Smith
 * @author Rainer Pruy
 */
public class CSVParser {

    /**
     * The default separator to use if none is supplied to the constructor.
     */
    public static final char DEFAULT_SEPARATOR = ',';
    /**
     * The average size of a line read by opencsv (used for setting the size of StringBuilders).
     */
    public static final int INITIAL_READ_SIZE = 1024;
    /**
     * In most cases we know the size of the line we want to read.  In that case we will set the initial read
     * to that plus an buffer size.
     */
    public static final int READ_BUFFER_SIZE = 128;
    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';
    /**
     * The default escape character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = '\\';
    /**
     * The default strict quote behavior to use if none is supplied to the
     * constructor.
     */
    public static final boolean DEFAULT_STRICT_QUOTES = false;
    /**
     * The default leading whitespace behavior to use if none is supplied to the
     * constructor.
     */
    public static final boolean DEFAULT_IGNORE_LEADING_WHITESPACE = true;
    /**
     * If the quote character is set to {@code null} then there is no quote character.
     */
    public static final boolean DEFAULT_IGNORE_QUOTATIONS = false;
    /**
     * This is the "null" character - if a value is set to this then it is ignored.
     */
    public static final char NULL_CHARACTER = '\0';

    /**
     * This is the character that the CSVParser will treat as the separator.
     */
    private final char separator;
    /**
     * This is the character that the CSVParser will treat as the quotation character.
     */
    private final char quotechar;
    /**
     * This is the character that the CSVParser will treat as the escape character.
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
    private String pending;
    private boolean inField = false;

    /**
     * Constructs CSVParser using a comma for the separator.
     */
    public CSVParser() {
        this(DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVParser with supplied separator.
     *
     * @param separator The delimiter to use for separating entries.
     */
    public CSVParser(final char separator) {
        this(separator, DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVParser with supplied separator and quote char.
     *
     * @param separator The delimiter to use for separating entries
     * @param quotechar The character to use for quoted elements
     */
    public CSVParser(final char separator, final char quotechar) {
        this(separator, quotechar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVParser with supplied separator and quote char.
     *
     * @param separator The delimiter to use for separating entries
     * @param quotechar The character to use for quoted elements
     * @param escape The character to use for escaping a separator or quote
     */
    public CSVParser(final char separator, final char quotechar, final char escape) {
        this(separator, quotechar, escape, DEFAULT_STRICT_QUOTES);
    }

    /**
     * Constructs CSVParser with supplied separator and quote char.
     * Allows setting the "strict quotes" flag.
     *
     * @param separator The delimiter to use for separating entries
     * @param quotechar The character to use for quoted elements
     * @param escape The character to use for escaping a separator or quote
     * @param strictQuotes If {@code true}, characters outside the quotes are ignored
     */
    public CSVParser(final char separator, final char quotechar, final char escape, final boolean strictQuotes) {
        this(separator, quotechar, escape, strictQuotes, DEFAULT_IGNORE_LEADING_WHITESPACE);
    }

    /**
     * Constructs CSVParser with supplied separator and quote char.
     * Allows setting the "strict quotes" and "ignore leading whitespace" flags.
     *
     * @param separator The delimiter to use for separating entries
     * @param quotechar The character to use for quoted elements
     * @param escape The character to use for escaping a separator or quote
     * @param strictQuotes If {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace If {@code true}, white space in front of a quote in a field is ignored
     */
    public CSVParser(final char separator, final char quotechar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhiteSpace) {
        this(separator, quotechar, escape, strictQuotes, ignoreLeadingWhiteSpace, DEFAULT_IGNORE_QUOTATIONS);
    }

    /**
     * Constructs CSVParser with supplied separator and quote char.
     * Allows setting the "strict quotes" and "ignore leading whitespace" flags.
     *
     * @param separator The delimiter to use for separating entries
     * @param quotechar The character to use for quoted elements
     * @param escape The character to use for escaping a separator or quote
     * @param strictQuotes If {@code true}, characters outside the quotes are ignored
     * @param ignoreLeadingWhiteSpace If {@code true}, white space in front of a quote in a field is ignored
     * @param ignoreQuotations If {@code true}, treat quotations like any other character.
     */
    public CSVParser(final char separator, final char quotechar, final char escape, final boolean strictQuotes, final boolean ignoreLeadingWhiteSpace,
            final boolean ignoreQuotations) {
        if (anyCharactersAreTheSame(separator, quotechar, escape)) {
            throw new UnsupportedOperationException("The separator, quote, and escape characters must be different!");
        }

        if (separator == NULL_CHARACTER) {
            throw new UnsupportedOperationException("The separator character must be defined!");
        }

        this.separator = separator;
        this.quotechar = quotechar;
        this.escape = escape;
        this.strictQuotes = strictQuotes;
        this.ignoreLeadingWhiteSpace = ignoreLeadingWhiteSpace;
        this.ignoreQuotations = ignoreQuotations;
    }

    /**
     * @return The default separator for this parser.
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * @return The default quotation character for this parser.
     */
    public char getQuotechar() {
        return quotechar;
    }

    /**
     * @return The default escape character for this parser.
     */
    public char getEscape() {
        return escape;
    }

    /**
     * @return The default strictQuotes setting for this parser.
     */
    public boolean isStrictQuotes() {
        return strictQuotes;
    }

    /**
     * @return The default ignoreLeadingWhiteSpace setting for this parser.
     */
    public boolean isIgnoreLeadingWhiteSpace() {
        return ignoreLeadingWhiteSpace;
    }

    /**
     * @return The default ignoreQuotation setting for this parser.
     */
    public boolean isIgnoreQuotations() {
        return ignoreQuotations;
    }

    /**
     * Checks to see if any two of the three characters are the same.
     * This is because in opencsv the separator, quote, and escape characters
     * must the different.
     *
     * @param separator The defined separator character
     * @param quotechar The defined quotation cahracter
     * @param escape The defined escape character
     * @return True if any two of the three are the same.
     */
    private boolean anyCharactersAreTheSame(final char separator, final char quotechar, final char escape) {
        return isSameCharacter(separator, quotechar) || isSameCharacter(separator, escape) || isSameCharacter(quotechar, escape);
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
     * @return True if something was left over from last call(s)
     */
    public boolean isPending() {
        return pending != null;
    }

    /**
     * Parses an incoming String and returns an array of elements.
     * This method is used when all data is contained in a single line.
     *
     * @param nextLine Line to be parsed.
     * @return The comma-tokenized list of elements, or {@code null} if nextLine is null
     * @throws ParseException If bad things happen during the read
     */
    public List<String> parseLine(final String nextLine) throws ParseException {
        return parseLine(nextLine, false);
    }

    /**
     * Parses an incoming String and returns an array of elements.
     * This method is used when the data spans multiple lines.
     *
     * @param nextLine Current line to be processed
     * @return The comma-tokenized list of elements, or {@code null} if nextLine is null
     * @throws ParseException If bad things happen during the read
     */
    public List<String> parseLineMulti(final String nextLine) throws ParseException {
        return parseLine(nextLine, true);
    }

    /**
     *
     *
     * @param nextLine
     * @return
     * @throws ParseException
     */
    public String[] parseLineToArray(final String nextLine) throws ParseException {
        final List<String> ret = parseLine(nextLine);

        return ret.toArray(new String[ret.size()]);
    }

    /**
     *
     *
     * @param nextLine
     * @return
     * @throws ParseException
     */
    public String[] parseLineMultiToArray(final String nextLine) throws ParseException {
        final List<String> ret = parseLineMulti(nextLine);

        return ret.toArray(new String[ret.size()]);
    }

    /**
     *
     *
     * @param nextLine
     * @param output
     * @return the specified output parameter: {@code output}.
     * @throws ParseException
     */
    public String[] parseLineToArray(final String nextLine, final String[] output) throws ParseException {
        final List<String> ret = parseLine(nextLine);

        return ret.toArray(output);
    }

    /**
     *
     *
     * @param nextLine
     * @param output
     * @return the specified output parameter: {@code output}.
     * @throws ParseException
     */
    public String[] parseLineMultiToArray(final String nextLine, final String[] output) throws ParseException {
        final List<String> ret = parseLineMulti(nextLine);

        return ret.toArray(output);
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine The string to parse
     * @param multi Does it take multiple lines to form a single record.
     * @return The comma-tokenized list of elements, or {@code null} if nextLine is null
     * @throws ParseException If bad things happen during the read
     */
    protected List<String> parseLine(final String nextLine, final boolean multi) throws ParseException {
        if (!multi && pending != null) {
            pending = null;
        }

        if (nextLine == null) {
            if (pending != null) {
                final String s = pending;
                pending = null;
                return Arrays.asList(s);
            }

            return N.<String> emptyList();
        }

        final List<String> tokensOnThisLine = new ArrayList<>();
        StringBuilder sb = new StringBuilder(nextLine.length() + READ_BUFFER_SIZE);
        boolean inQuotes = false;
        if (pending != null) {
            sb.append(pending);
            pending = null;
            inQuotes = !ignoreQuotations;//true;
        }
        for (int i = 0; i < nextLine.length(); i++) {

            final char c = nextLine.charAt(i);
            if (c == escape) {
                if (isNextCharacterEscapable(nextLine, inQuotes(inQuotes), i)) {
                    i = appendNextCharacterAndAdvanceLoop(nextLine, sb, i);
                }
            } else if (c == quotechar) {
                if (isNextCharacterEscapedQuote(nextLine, inQuotes(inQuotes), i)) {
                    i = appendNextCharacterAndAdvanceLoop(nextLine, sb, i);
                } else {
                    inQuotes = !inQuotes;

                    if (atStartOfField(sb)) {
                        // continue?
                    }

                    // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                    if (!strictQuotes) {
                        if (i > 2 //not on the beginning of the line  //NOSONAR
                                && nextLine.charAt(i - 1) != separator //not at the beginning of an escape sequence
                                && nextLine.length() > (i + 1) && nextLine.charAt(i + 1) != separator //not at the   end of an escape sequence
                        ) {

                            if (ignoreLeadingWhiteSpace && sb.length() > 0 && Strings.isWhitespace(sb)) {
                                sb.setLength(0);
                            } else {
                                sb.append(c);
                            }

                        }
                    }
                }
                inField = !inField;
            } else if (c == separator && !(inQuotes && !ignoreQuotations)) {
                tokensOnThisLine.add(sb.toString());
                sb.setLength(0);
                inField = false;
            } else {
                if (!strictQuotes || (inQuotes && !ignoreQuotations)) {
                    sb.append(c);
                    inField = true;
                }
            }

        }
        // line is done - check status
        if ((inQuotes && !ignoreQuotations)) {
            if (multi) {
                // continuing a quoted section, re-append newline
                sb.append('\n');
                pending = sb.toString();
                sb = null; // this partial content is not to be added to field list yet
            } else {
                throw new ParseException("Un-terminated quoted field at end of CSV line");
            }
            if (inField) {
                // continue?
            }
        } else {
            inField = false;
        }

        if (sb != null) {
            tokensOnThisLine.add(sb.toString());
        }

        return tokensOnThisLine;
    }

    private boolean atStartOfField(final StringBuilder sb) {
        return sb.length() == 0;
    }

    /**
     * Appends the next character in the line to the string buffer.
     *
     * @param line Line to process
     * @param sb Contains the processed character
     * @param i Current position in the line.
     * @return New position in the line.
     */
    private int appendNextCharacterAndAdvanceLoop(final String line, final StringBuilder sb, int i) {
        sb.append(line.charAt(i + 1));
        i++;
        return i;
    }

    /**
     * Determines if we can process as if we were in quotes.
     *
     * @param inQuotes Are we currently in quotes.
     * @return True if we should process as if we are inside quotes.
     */
    private boolean inQuotes(final boolean inQuotes) {
        return (inQuotes && !ignoreQuotations) || inField;
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
        return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1) // there is indeed another character to check.
                && isCharacterQuoteCharacter(nextLine.charAt(i + 1));
    }

    /**
     * Checks to see if the passed in character is the defined quotation character.
     *
     * @param c Source character
     * @return True if c is the defined quotation character
     */
    private boolean isCharacterQuoteCharacter(final char c) {
        return c == quotechar;
    }

    /**
     * Checks to see if the character is the defined escape character.
     *
     * @param c Source character
     * @return True if the character is the defined escape character
     */
    private boolean isCharacterEscapeCharacter(final char c) {
        return c == escape;
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
        return isCharacterQuoteCharacter(c) || isCharacterEscapeCharacter(c);
    }

    /**
     * Checks to see if the character after the current index in a String is an
     * escapable character.
     * Meaning the next character is either a quotation character or the escape
     * char and you are inside quotes.
     *
     * Precondition: the current character is an escape.
     *
     * @param nextLine The current line
     * @param inQuotes True if the current context is quoted
     * @param i Current index in line
     * @return True if the following character is a quote
     */
    protected boolean isNextCharacterEscapable(final String nextLine, final boolean inQuotes, final int i) {
        return inQuotes // we are in quotes, therefore there can be escaped quotes in here.
                && nextLine.length() > (i + 1) // there is indeed another character to check.
                && isCharacterEscapable(nextLine.charAt(i + 1));
    }

}
