/*
 * Copyright (C) 2015 HaiYang Li
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * A utility class that provides a comprehensive dictionary of commonly used characters and strings,
 * including special characters, operators, SQL keywords, and mathematical functions.
 * This class serves as a centralized repository for string constants to avoid hardcoding
 * and improve code maintainability.
 * 
 * <p>All fields in this class are public static final constants representing either
 * single characters (prefixed with underscore) or their string equivalents.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String query = WD.SELECT + WD.SPACE + "*" + WD.SPACE + WD.FROM + WD.SPACE + "users";
 * String csv = "John" + WD.COMMA_SPACE + "Doe" + WD.COMMA_SPACE + "30";
 * }</pre>
 *
 */
@Beta
@Internal
public final class WD {

    private WD() {
        // singleton
    }

    /**
     * Represents the {@code null} character: {@code (char) 0}.
     * This is the character with ASCII value 0, often used as a string terminator in C.
     */
    public static final char CHAR_ZERO = (char) 0;

    /**
     * Represents the line feed character: {@code '\n'}.
     * Used for line breaks in Unix/Linux systems.
     */
    public static final char CHAR_LF = '\n';

    /**
     * Represents the carriage return character: {@code '\r'}.
     * Used for line breaks in older Mac systems and as part of Windows line breaks (\r\n).
     */
    public static final char CHAR_CR = '\r';

    /**
     * Represents the space character: {@code ' '}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char space = WD._SPACE;
     * String text = "Hello" + space + "World";
     * }</pre>
     */
    public static final char _SPACE = ' ';

    /**
     * Represents the space string: {@code " "}.
     * Useful for string concatenation operations.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String fullName = firstName + WD.SPACE + lastName;
     * }</pre>
     */
    public static final String SPACE = " ";

    /**
     * Represents the period character: {@code '.'}.
     * Commonly used as decimal point or dot notation.
     */
    public static final char _PERIOD = '.';

    /**
     * Represents the period string: {@code "."}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String filename = "document" + WD.PERIOD + "pdf";
     * }</pre>
     */
    public static final String PERIOD = ".";

    /**
     * Represents the comma character: {@code ','}.
     * Used as a separator in lists, CSV files, etc.
     */
    public static final char _COMMA = ',';

    /**
     * Represents the comma string: {@code ","}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String csv = value1 + WD.COMMA + value2 + WD.COMMA + value3;
     * }</pre>
     */
    public static final String COMMA = ",";

    /**
     * Represents comma followed by space: {@code ", "}.
     * Commonly used for readable list formatting.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String list = "apple" + WD.COMMA_SPACE + "banana" + WD.COMMA_SPACE + "orange";
     * }</pre>
     */
    public static final String COMMA_SPACE = ", ";

    /**
     * Represents the colon character: {@code ':'}.
     * Used in time notation, key-value pairs, etc.
     */
    public static final char _COLON = ':';

    /**
     * Represents the colon string: {@code ":"}.
     */
    public static final String COLON = ":";

    /**
     * Represents colon followed by space: {@code ": "}.
     * Commonly used in key-value formatting.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String keyValue = "Name" + WD.COLON_SPACE + "John Doe";
     * }</pre>
     */
    public static final String COLON_SPACE = ": ";

    /**
     * Represents the semicolon character: {@code ';'}.
     * Used as statement terminator in many programming languages.
     */
    public static final char _SEMICOLON = ';';

    /**
     * Represents the semicolon string: {@code ";"}.
     */
    public static final String SEMICOLON = ";";

    /**
     * Represents semicolon followed by space: {@code "; "}.
     * Used for separating items in a more formal list format.
     */
    public static final String SEMICOLON_SPACE = "; ";

    /**
     * Represents the backslash character: {@code '\\'}.
     * Used as escape character in many contexts.
     */
    public static final char _BACKSLASH = '\\';

    /**
     * Represents the backslash string: {@code "\\"}.
     * Note: The actual string contains a single backslash.
     */
    public static final String BACKSLASH = "\\";

    /**
     * Represents the single quotation mark character: {@code '\''}.
     * Used for character literals and string quoting.
     */
    public static final char _QUOTATION_S = '\'';

    /**
     * Represents the single quotation mark string: {@code "'"}.
     */
    public static final String QUOTATION_S = "'";

    /**
     * Represents space followed by single quotation mark: {@code " '"}.
     * Useful for formatting quoted text.
     */
    public static final String SPACE_QUOTATION_S = " '";

    /**
     * Represents single quotation mark followed by space: {@code "' "}.
     * Useful for formatting quoted text.
     */
    public static final String QUOTATION_S_SPACE = "' ";

    /**
     * Represents the double quotation mark character: {@code '"'}.
     * Used for string literals in many programming languages.
     */
    public static final char _QUOTATION_D = '"';

    /**
     * Represents the double quotation mark string: {@code "\""}.
     */
    public static final String QUOTATION_D = "\"";

    /**
     * Represents space followed by double quotation mark: {@code " \""}.
     * Useful for formatting quoted text.
     */
    public static final String SPACE_QUOTATION_D = " \"";

    /**
     * Represents double quotation mark followed by space: {@code "\" "}.
     * Useful for formatting quoted text.
     */
    public static final String QUOTATION_D_SPACE = "\" ";

    /**
     * Represents the ampersand character: {@code '&'}.
     * Used as bitwise AND operator and HTML entity prefix.
     */
    public static final char _AMPERSAND = '&';

    /**
     * Represents the ampersand string: {@code "&"}.
     */
    public static final String AMPERSAND = "&";

    /**
     * Represents the vertical bar character: {@code '|'}.
     * Used as bitwise OR operator and pipe symbol.
     */
    public static final char _VERTICALBAR = '|';

    /**
     * Represents the vertical bar string: {@code "|"}.
     */
    public static final String VERTICALBAR = "|";

    /**
     * Represents double vertical bars: {@code "||"}.
     * Used as logical OR operator in SQL and some programming languages.
     */
    public static final String PARALLEL = "||";

    /**
     * Represents the underscore character: {@code '_'}.
     * Commonly used in identifiers and SQL wildcards.
     */
    public static final char _UNDERSCORE = '_';

    /**
     * Represents the underscore string: {@code "_"}.
     */
    public static final String UNDERSCORE = "_";

    /**
     * Represents the hyphen character: {@code '-'}.
     */
    public static final char _HYPHEN = '-';

    /**
     * Represents the hyphen string: {@code "-"}.
     */
    public static final String HYPHEN = "-";

    /**
     * Represents the less than character: {@code '<'}.
     * Used as comparison operator and XML/HTML tag delimiter.
     */
    public static final char _LESS_THAN = '<';

    /**
     * Represents the less than string: {@code "<"}.
     */
    public static final String LESS_THAN = "<";

    /**
     * Represents the greater than character: {@code '>'}.
     * Used as comparison operator and XML/HTML tag delimiter.
     */
    public static final char _GREATER_THAN = '>';

    /**
     * Represents the greater than string: {@code ">"}.
     */
    public static final String GREATER_THAN = ">";

    /**
     * Represents the equal sign character: {@code '='}.
     * Used as assignment and equality operator.
     */
    public static final char _EQUAL = '=';

    /**
     * Represents the equal sign string: {@code "="}.
     */
    public static final String EQUAL = "=";

    /**
     * Represents the plus character: {@code '+'}.
     * Used as addition operator and positive sign.
     */
    public static final char _PLUS = '+';

    /**
     * Represents the plus string: {@code "+"}.
     */
    public static final String PLUS = "+";

    /**
     * Represents the minus character: {@code '-'}.
     * Used as subtraction operator and negative sign.
     */
    public static final char _MINUS = '-';

    /**
     * Represents the minus string: {@code "-"}.
     */
    public static final String MINUS = "-";

    /**
     * Represents the percent character: {@code '%'}.
     * Used as modulo operator and percentage symbol.
     */
    public static final char _PERCENT = '%';

    /**
     * Represents the percent string: {@code "%"}.
     */
    public static final String PERCENT = "%";

    /**
     * Represents the forward slash character: {@code '/'}.
     * Used as division operator and path separator.
     */
    public static final char _SLASH = '/';

    /**
     * Represents the forward slash string: {@code "/"}.
     */
    public static final String SLASH = "/";

    /**
     * Represents the asterisk character: {@code '*'}.
     * Used as multiplication operator and wildcard.
     */
    public static final char _ASTERISK = '*';

    /**
     * Represents the asterisk string: {@code "*"}.
     */
    public static final String ASTERISK = "*";

    /**
     * Represents the question mark character: {@code '?'}.
     * Used in ternary operators and SQL parameters.
     */
    public static final char _QUESTION_MARK = '?';

    /**
     * Represents the question mark string: {@code "?"}.
     */
    public static final String QUESTION_MARK = "?";

    /**
     * Represents the left parenthesis character: {@code '('}.
     * Used for grouping and function calls.
     */
    public static final char _PARENTHESES_L = '(';

    /**
     * Represents the left parenthesis string: {@code "("}.
     */
    public static final String PARENTHESES_L = "(";

    /**
     * Represents space followed by left parenthesis: {@code " ("}.
     * Useful for formatting function calls.
     */
    public static final String SPACE_PARENTHESES_L = " (";

    /**
     * Represents the right parenthesis character: {@code ')'}.
     * Used for grouping and function calls.
     */
    public static final char _PARENTHESES_R = ')';

    /**
     * Represents the right parenthesis string: {@code ")"}.
     */
    public static final String PARENTHESES_R = ")";

    /**
     * Represents right parenthesis followed by space: {@code ") "}.
     * Useful for formatting function calls.
     */
    public static final String PARENTHESES_R_SPACE = ") ";

    /**
     * Represents the left square bracket character: {@code '['}.
     * Used for array indexing and character classes.
     */
    public static final char _BRACKET_L = '[';

    /**
     * Represents the left square bracket string: {@code "["}.
     */
    public static final String BRACKET_L = "[";

    /**
     * Represents the right square bracket character: {@code ']'}.
     * Used for array indexing and character classes.
     */
    public static final char _BRACKET_R = ']';

    /**
     * Represents the right square bracket string: {@code "]"}.
     */
    public static final String BRACKET_R = "]";

    /**
     * Represents the left curly brace character: '{'.
     * Used for block delimiters and object literals.
     */
    public static final char _BRACE_L = '{';

    /**
     * Represents the left curly brace string: "{".
     */
    public static final String BRACE_L = "{";

    /**
     * Represents the right curly brace character: {@code '}'}.
     * Used for block delimiters and object literals.
     */
    public static final char _BRACE_R = '}';

    /**
     * Represents the right curly brace string: {@code "}"}.
     */
    public static final String BRACE_R = "}";

    /**
     * Represents the circumflex character: {@code '^'}.
     * Used as bitwise XOR operator and exponentiation symbol.
     */
    public static final char _CIRCUMFLEX = '^';

    /**
     * Represents the circumflex string: {@code "^"}.
     */
    public static final String CIRCUMFLEX = "^";

    /**
     * Represents the tilde character: {@code '~'}.
     * Used as bitwise NOT operator and home directory symbol.
     */
    public static final char _UNARYBIT = '~';

    /**
     * Represents the tilde string: {@code "~"}.
     */
    public static final String UNARYBIT = "~";

    /**
     * Represents the dollar sign character: {@code '$'}.
     * Used in variable references and regular expressions.
     */
    public static final char _DOLLAR = '$';

    /**
     * Represents the dollar sign string: {@code "$"}.
     */
    public static final String DOLLAR = "$";

    /**
     * Represents the hash/pound character: {@code '#'}.
     * Used for comments and CSS selectors.
     */
    public static final char _SHARP = '#';

    /**
     * Represents the hash/pound string: {@code "#"}.
     */
    public static final String SHARP = "#";

    /**
     * Represents the exclamation mark character: {@code '!'}.
     * Used as logical NOT operator.
     */
    public static final char _EXCLAMATION = '!';

    /**
     * Represents the exclamation mark string: {@code "!"}.
     */
    public static final String EXCLAMATION = "!";

    /**
     * Represents the not equal operator: {@code "!="}.
     * Used for inequality comparison in many programming languages.
     */
    public static final String NOT_EQUAL = "!=";

    /**
     * Represents the SQL not equal operator: {@code "<>"}.
     * Alternative syntax for inequality in SQL.
     */
    public static final String NOT_EQUAL2 = "<>";

    /**
     * Represents the greater than or equal operator: {@code ">="}.
     * Used for comparison operations.
     */
    public static final String GREATER_EQUAL = ">=";

    /**
     * Represents the less than or equal operator: {@code "<="}.
     * Used for comparison operations.
     */
    public static final String LESS_EQUAL = "<=";

    // --------------------SQL key words----------------------------
    /**
     * SQL WITH keyword: {@code "WITH"}.
     * Used for common table expressions (CTEs).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String cte = WD.WITH + " temp_table AS (SELECT * FROM users)";
     * }</pre>
     */
    public static final String WITH = "WITH";

    /**
     * SQL MERGE keyword: {@code "MERGE"}.
     * Used for merge operations in SQL.
     */
    public static final String MERGE = "MERGE";

    /**
     * SQL SELECT keyword: {@code "SELECT"}.
     * Used to query data from database tables.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String query = WD.SELECT + " * " + WD.FROM + " users";
     * }</pre>
     */
    public static final String SELECT = "SELECT";

    /**
     * SQL INSERT keyword: {@code "INSERT"}.
     * Used to insert data into database tables.
     */
    public static final String INSERT = "INSERT";

    /**
     * SQL INTO keyword: {@code "INTO"}.
     * Used with INSERT statements.
     */
    public static final String INTO = "INTO";

    /**
     * SQL UPDATE keyword: {@code "UPDATE"}.
     * Used to modify existing data in database tables.
     */
    public static final String UPDATE = "UPDATE";

    /**
     * SQL SET keyword: {@code "SET"}.
     * Used with UPDATE statements to specify column values.
     */
    public static final String SET = "SET";

    /**
     * SQL DELETE keyword: {@code "DELETE"}.
     * Used to remove data from database tables.
     */
    public static final String DELETE = "DELETE";

    /**
     * SQL CREATE keyword: {@code "CREATE"}.
     * Used to create database objects like tables, views, etc.
     */
    public static final String CREATE = "CREATE";

    /**
     * SQL DROP keyword: {@code "DROP"}.
     * Used to remove database objects.
     */
    public static final String DROP = "DROP";

    /**
     * SQL SHOW keyword: {@code "SHOW"}.
     * Used to display database information.
     */
    public static final String SHOW = "SHOW";

    /**
     * SQL DESCRIBE keyword: {@code "DESCRIBE"}.
     * Used to show table structure.
     */
    public static final String DESCRIBE = "DESCRIBE";

    /**
     * SQL ALTER keyword: {@code "ALTER"}.
     * Used to modify database object structure.
     */
    public static final String ALTER = "ALTER";

    /**
     * SQL USE keyword: {@code "USE"}.
     * Used to select a database.
     */
    public static final String USE = "USE";

    /**
     * SQL RENAME keyword: {@code "RENAME"}.
     * Used to rename database objects.
     */
    public static final String RENAME = "RENAME";

    /**
     * SQL BEGIN TRANSACTION statement: {@code "BEGIN TRANSACTION"}.
     * Used to start a database transaction.
     */
    public static final String BEGIN_TRANSACTION = "BEGIN TRANSACTION";

    /**
     * SQL START TRANSACTION statement: {@code "START TRANSACTION"}.
     * Alternative syntax to begin a transaction.
     */
    public static final String START_TRANSACTION = "START TRANSACTION";

    /**
     * SQL COMMIT keyword: {@code "COMMIT"}.
     * Used to save transaction changes permanently.
     */
    public static final String COMMIT = "COMMIT";

    /**
     * SQL ROLLBACK keyword: {@code "ROLLBACK"}.
     * Used to undo transaction changes.
     */
    public static final String ROLLBACK = "ROLLBACK";

    /**
     * SQL AS keyword: {@code "AS"}.
     * Used for aliasing in queries.
     */
    public static final String AS = "AS";

    /**
     * SQL JOIN keyword: {@code "JOIN"}.
     * Used to combine rows from multiple tables.
     */
    public static final String JOIN = "JOIN";

    /**
     * SQL NATURAL keyword: {@code "NATURAL"}.
     * Used with JOIN for natural joins.
     */
    public static final String NATURAL = "NATURAL";

    /**
     * SQL INNER keyword: {@code "INNER"}.
     * Used with JOIN for inner joins.
     */
    public static final String INNER = "INNER";

    /**
     * SQL OUTER keyword with trailing space: {@code "OUTER "}.
     * Used with JOIN for outer joins.
     */
    public static final String OUTER = "OUTER ";

    /**
     * SQL LEFT JOIN clause: {@code "LEFT JOIN"}.
     * Returns all records from left table and matched records from right table.
     */
    public static final String LEFT_JOIN = "LEFT JOIN";

    /**
     * SQL LEFT keyword: {@code "LEFT"}.
     * Used in LEFT JOIN operations.
     */
    public static final String LEFT = "LEFT";

    /**
     * SQL RIGHT JOIN clause: {@code "RIGHT JOIN"}.
     * Returns all records from right table and matched records from left table.
     */
    public static final String RIGHT_JOIN = "RIGHT JOIN";

    /**
     * SQL RIGHT keyword: {@code "RIGHT"}.
     * Used in RIGHT JOIN operations.
     */
    public static final String RIGHT = "RIGHT";

    /**
     * SQL FULL JOIN clause: {@code "FULL JOIN"}.
     * Returns all records when there's a match in either table.
     */
    public static final String FULL_JOIN = "FULL JOIN";

    /**
     * SQL FULL keyword: {@code "FULL"}.
     * Used in FULL JOIN operations.
     */
    public static final String FULL = "FULL";

    /**
     * SQL CROSS JOIN clause: {@code "CROSS JOIN"}.
     * Returns the Cartesian product of both tables.
     */
    public static final String CROSS_JOIN = "CROSS JOIN";

    /**
     * SQL INNER JOIN clause: {@code "INNER JOIN"}.
     * Returns records with matching values in both tables.
     */
    public static final String INNER_JOIN = "INNER JOIN";

    /**
     * SQL NATURAL JOIN clause: {@code "NATURAL JOIN"}.
     * Joins tables based on columns with same names.
     */
    public static final String NATURAL_JOIN = "NATURAL JOIN";

    /**
     * SQL CROSS keyword: {@code "CROSS"}.
     * Used in CROSS JOIN operations.
     */
    public static final String CROSS = "CROSS";

    /**
     * SQL ON keyword: {@code "ON"}.
     * Used to specify join conditions.
     */
    public static final String ON = "ON";

    /**
     * SQL USING keyword: {@code "USING"}.
     * Alternative to ON for specifying join columns.
     */
    public static final String USING = "USING";

    /**
     * SQL WHERE keyword: {@code "WHERE"}.
     * Used to filter query results.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String query = WD.SELECT + " * " + WD.FROM + " users " + WD.WHERE + " age > 18";
     * }</pre>
     */
    public static final String WHERE = "WHERE";

    /**
     * SQL GROUP BY clause: {@code "GROUP BY"}.
     * Used to group rows with same values.
     */
    public static final String GROUP_BY = "GROUP BY";

    /**
     * SQL PARTITION BY clause: {@code "PARTITION BY"}.
     * Used in window functions to define partitions.
     */
    public static final String PARTITION_BY = "PARTITION BY";

    /**
     * SQL HAVING keyword: {@code "HAVING"}.
     * Used to filter grouped results.
     */
    public static final String HAVING = "HAVING";

    /**
     * SQL ORDER BY clause: {@code "ORDER BY"}.
     * Used to sort query results.
     */
    public static final String ORDER_BY = "ORDER BY";

    /**
     * SQL LIMIT keyword: {@code "LIMIT"}.
     * Used to limit the number of returned rows.
     */
    public static final String LIMIT = "LIMIT";

    /**
     * SQL OFFSET keyword: {@code "OFFSET"}.
     * Used to skip a number of rows.
     */
    public static final String OFFSET = "OFFSET";

    /**
     * SQL FOR UPDATE clause: {@code "FOR UPDATE"}.
     * Used to lock selected rows for update.
     */
    public static final String FOR_UPDATE = "FOR UPDATE";

    /**
     * SQL FETCH FIRST clause: {@code "FETCH FIRST"}.
     * Standard SQL syntax for limiting results.
     */
    public static final String FETCH_FIRST = "FETCH FIRST";

    /**
     * SQL FETCH NEXT clause: {@code "FETCH NEXT"}.
     * Used with OFFSET for pagination.
     */
    public static final String FETCH_NEXT = "FETCH NEXT";

    /**
     * SQL ROWS keyword: {@code "ROWS"}.
     * Used with FETCH clauses.
     */
    public static final String ROWS = "ROWS";

    /**
     * SQL ROWS ONLY clause: {@code "ROWS ONLY"}.
     * Used to complete FETCH syntax.
     */
    public static final String ROWS_ONLY = "ROWS ONLY";

    /**
     * SQL ROW_NEXT keyword: {@code "ROW_NEXT"}.
     * Used in window functions.
     */
    public static final String ROW_NEXT = "ROW_NEXT";

    /**
     * SQL ROWNUM keyword: {@code "ROWNUM"}.
     * Oracle-specific pseudo-column for row numbering.
     */
    public static final String ROWNUM = "ROWNUM";

    /**
     * SQL EXISTS keyword: {@code "EXISTS"}.
     * Used to test for existence of rows.
     */
    public static final String EXISTS = "EXISTS";

    /**
     * SQL LIKE keyword: {@code "LIKE"}.
     * Used for pattern matching in WHERE clauses.
     */
    public static final String LIKE = "LIKE";

    /**
     * SQL AND keyword: {@code "AND"}.
     * Logical AND operator in SQL.
     */
    public static final String AND = "AND";

    /**
     * Logical AND operator symbol: {@code "&&"}.
     * Alternative AND syntax in some databases.
     */
    public static final String AND_OP = "&&";

    /**
     * SQL OR keyword: {@code "OR"}.
     * Logical OR operator in SQL.
     */
    public static final String OR = "OR";

    /**
     * Logical OR operator symbol: {@code "||"}.
     * Alternative OR syntax in some databases.
     */
    public static final String OR_OP = "||";

    /**
     * SQL XOR keyword: {@code "XOR"}.
     * Exclusive OR operator in SQL.
     */
    public static final String XOR = "XOR";

    /**
     * SQL NOT keyword: {@code "NOT"}.
     * Logical NOT operator in SQL.
     */
    public static final String NOT = "NOT";

    /**
     * SQL BETWEEN keyword: {@code "BETWEEN"}.
     * Used to filter values within a range.
     */
    public static final String BETWEEN = "BETWEEN";

    /**
     * SQL IS keyword: {@code "IS"}.
     * Used with NULL comparisons.
     */
    public static final String IS = "IS";

    /**
     * SQL IS NOT clause: {@code "IS NOT"}.
     * Used for negative NULL comparisons.
     */
    public static final String IS_NOT = "IS NOT";

    /**
     * SQL NULL keyword: {@code "NULL"}.
     * Represents absence of value.
     */
    public static final String NULL = "NULL";

    /**
     * SQL IS NULL clause: {@code "IS NULL"}.
     * Tests for NULL values.
     */
    public static final String IS_NULL = "IS NULL";

    /**
     * SQL IS NOT NULL clause: {@code "IS NOT NULL"}.
     * Tests for non-NULL values.
     */
    public static final String IS_NOT_NULL = "IS NOT NULL";

    /**
     * EMPTY keyword: {@code "EMPTY"}.
     * Custom keyword for empty checks.
     */
    public static final String EMPTY = "EMPTY";

    /**
     * IS EMPTY clause: {@code "IS EMPTY"}.
     * Custom clause for checking empty values.
     */
    public static final String IS_EMPTY = "IS EMPTY";

    /**
     * IS NOT EMPTY clause: {@code "IS NOT EMPTY"}.
     * Custom clause for checking non-empty values.
     */
    public static final String IS_NOT_EMPTY = "IS NOT EMPTY";

    /**
     * BLANK keyword: {@code "BLANK"}.
     * Custom keyword for blank checks.
     */
    public static final String BLANK = "BLANK";

    /**
     * IS BLANK clause: {@code "IS BLANK"}.
     * Custom clause for checking blank values.
     */
    public static final String IS_BLANK = "IS BLANK";

    /**
     * IS NOT BLANK clause: {@code "IS NOT BLANK"}.
     * Custom clause for checking non-blank values.
     */
    public static final String IS_NOT_BLANK = "IS NOT BLANK";

    /**
     * SQL NOT IN clause: {@code "NOT IN"}.
     * Tests if value is not in a list.
     */
    public static final String NOT_IN = "NOT IN";

    /**
     * SQL NOT EXISTS clause: {@code "NOT EXISTS"}.
     * Tests for non-existence of rows.
     */
    public static final String NOT_EXISTS = "NOT EXISTS";

    /**
     * SQL NOT LIKE clause: {@code "NOT LIKE"}.
     * Tests for pattern matching with negation.
     */
    public static final String NOT_LIKE = "NOT LIKE";

    /**
     * SQL FROM keyword: {@code "FROM"}.
     * Specifies tables in queries.
     */
    public static final String FROM = "FROM";

    /**
     * SQL ASC keyword: {@code "ASC"}.
     * Ascending sort order.
     */
    public static final String ASC = "ASC";

    /**
     * SQL DESC keyword: {@code "DESC"}.
     * Descending sort order.
     */
    public static final String DESC = "DESC";

    /**
     * SQL VALUES keyword: {@code "VALUES"}.
     * Used in INSERT statements.
     */
    public static final String VALUES = "VALUES";

    /**
     * SQL DISTINCT keyword: {@code "DISTINCT"}.
     * Removes duplicate rows from results.
     */
    public static final String DISTINCT = "DISTINCT";

    /**
     * SQL DISTINCTROW keyword: {@code "DISTINCTROW"}.
     * MySQL-specific distinct syntax.
     */
    public static final String DISTINCTROW = "DISTINCTROW";

    /**
     * SQL UNIQUE keyword: {@code "UNIQUE"}.
     * Constraint for unique values.
     */
    public static final String UNIQUE = "UNIQUE";

    /**
     * SQL TOP keyword: {@code "TOP"}.
     * SQL Server syntax for limiting results.
     */
    public static final String TOP = "TOP";

    /**
     * SQL IN keyword: {@code "IN"}.
     * Tests if value is in a list.
     */
    public static final String IN = "IN";

    /**
     * SQL ANY keyword: {@code "ANY"}.
     * Used with subqueries for comparisons.
     */
    public static final String ANY = "ANY";

    /**
     * SQL ALL keyword: {@code "ALL"}.
     * Used with subqueries for comparisons.
     */
    public static final String ALL = "ALL";

    /**
     * SQL SOME keyword: {@code "SOME"}.
     * Synonym for ANY in SQL.
     */
    public static final String SOME = "SOME";

    /**
     * SQL UNION keyword: {@code "UNION"}.
     * Combines results of multiple queries.
     */
    public static final String UNION = "UNION";

    /**
     * SQL UNION ALL clause: {@code "UNION ALL"}.
     * Combines results including duplicates.
     */
    public static final String UNION_ALL = "UNION ALL";

    /**
     * SQL INTERSECT keyword: {@code "INTERSECT"}.
     * Returns common rows from multiple queries.
     */
    public static final String INTERSECT = "INTERSECT";

    /**
     * SQL EXCEPT keyword: {@code "EXCEPT"}.
     * Returns rows from first query not in second.
     */
    public static final String EXCEPT = "EXCEPT";

    /**
     * SQL MINUS keyword: {@code "MINUS"}.
     * Oracle's equivalent of EXCEPT.
     */
    public static final String EXCEPT2 = "MINUS";

    /**
     * SQL AVG function: {@code "AVG"}.
     * Calculates average value.
     */
    public static final String AVG = "AVG";

    /**
     * SQL COUNT function: {@code "COUNT"}.
     * Counts number of rows.
     */
    public static final String COUNT = "COUNT";

    /**
     * SQL SUM function: {@code "SUM"}.
     * Calculates sum of values.
     */
    public static final String SUM = "SUM";

    /**
     * SQL MIN function: {@code "MIN"}.
     * Returns minimum value.
     */
    public static final String MIN = "MIN";

    /**
     * SQL MAX function: {@code "MAX"}.
     * Returns maximum value.
     */
    public static final String MAX = "MAX";

    /**
     * SQL ABS function: {@code "ABS"}.
     * Returns absolute value.
     */
    public static final String ABS = "ABS";

    /**
     * SQL ACOS function: {@code "ACOS"}.
     * Returns arc cosine of a number.
     */
    public static final String ACOS = "ACOS";

    /**
     * SQL ASIN function: {@code "ASIN"}.
     * Returns arc sine of a number.
     */
    public static final String ASIN = "ASIN";

    /**
     * SQL ATAN function: {@code "ATAN"}.
     * Returns arc tangent of a number.
     */
    public static final String ATAN = "ATAN";

    /**
     * SQL ATAN2 function: {@code "ATAN2"}.
     * Returns arc tangent of two numbers.
     */
    public static final String ATAN2 = "ATAN2";

    /**
     * SQL CEIL function: {@code "CEIL"}.
     * Rounds up to nearest integer.
     */
    public static final String CEIL = "CEIL";

    /**
     * SQL COS function: {@code "COS"}.
     * Returns cosine of a number.
     */
    public static final String COS = "COS";

    /**
     * SQL EXP function: {@code "EXP"}.
     * Returns e raised to a power.
     */
    public static final String EXP = "EXP";

    /**
     * SQL FLOOR function: {@code "FLOOR"}.
     * Rounds down to nearest integer.
     */
    public static final String FLOOR = "FLOOR";

    /**
     * SQL LOG function: {@code "LOG"}.
     * Returns logarithm of a number.
     */
    public static final String LOG = "LOG";

    /**
     * SQL LN function: {@code "LN"}.
     * Returns natural logarithm.
     */
    public static final String LN = "LN";

    /**
     * SQL MOD function: {@code "MOD"}.
     * Returns remainder of division.
     */
    public static final String MOD = "MOD";

    /**
     * SQL POWER function: {@code "POWER"}.
     * Raises number to a power.
     */
    public static final String POWER = "POWER";

    /**
     * SQL SIGN function: {@code "SIGN"}.
     * Returns sign of a number.
     */
    public static final String SIGN = "SIGN";

    /**
     * SQL SIN function: {@code "SIN"}.
     * Returns sine of a number.
     */
    public static final String SIN = "SIN";

    /**
     * SQL SQRT function: {@code "SQRT"}.
     * Returns square root.
     */
    public static final String SQRT = "SQRT";

    /**
     * SQL TAN function: {@code "TAN"}.
     * Returns tangent of a number.
     */
    public static final String TAN = "TAN";

    /**
     * SQL LENGTH function: {@code "LENGTH"}.
     * Returns string length.
     */
    public static final String LENGTH = "LENGTH";

    /**
     * SQL CONCAT function: {@code "CONCAT"}.
     * Concatenates strings.
     */
    public static final String CONCAT = "CONCAT";

    /**
     * SQL TRIM function: {@code "TRIM"}.
     * Removes leading and trailing spaces.
     */
    public static final String TRIM = "TRIM";

    /**
     * SQL LTRIM function: {@code "LTRIM"}.
     * Removes leading spaces.
     */
    public static final String LTRIM = "LTRIM";

    /**
     * SQL RTRIM function: {@code "RTRIM"}.
     * Removes trailing spaces.
     */
    public static final String RTRIM = "RTRIM";

    /**
     * SQL LPAD function: {@code "LPAD"}.
     * Left-pads string to specified length.
     */
    public static final String LPAD = "LPAD";

    /**
     * SQL RPAD function: {@code "RPAD"}.
     * Right-pads string to specified length.
     */
    public static final String RPAD = "RPAD";

    /**
     * SQL REPLACE function: {@code "REPLACE"}.
     * Replaces occurrences of substring.
     */
    public static final String REPLACE = "REPLACE";

    /**
     * SQL SUBSTR function: {@code "SUBSTR"}.
     * Extracts substring from string.
     */
    public static final String SUBSTR = "SUBSTR";

    /**
     * SQL UPPER function: {@code "UPPER"}.
     * Converts string to uppercase.
     */
    public static final String UPPER = "UPPER";

    /**
     * SQL LOWER function: {@code "LOWER"}.
     * Converts string to lowercase.
     */
    public static final String LOWER = "LOWER";

    /**
     * SQL CAST function: {@code "CAST"}.
     * Converts data type.
     */
    public static final String CAST = "CAST";

    /**
     * SQL CURRENT_TIME function: {@code "CURRENT_TIME"}.
     * Returns current time.
     */
    public static final String CURRENT_TIME = "CURRENT_TIME";

    /**
     * SQL CURRENT_DATE function: {@code "CURRENT_DATE"}.
     * Returns current date.
     */
    public static final String CURRENT_DATE = "CURRENT_DATE";

    /**
     * SQL CURRENT_TIMESTAMP function: {@code "CURRENT_TIMESTAMP"}.
     * Returns current date and time.
     */
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    // --------------------SQL key words----------------------------
}
