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
public final class SK {

    private SK() {
        // singleton
    }

    /**
     * Represents the null character: {@code (char) 0}.
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
     */
    public static final char _SPACE = ' ';

    /**
     * Represents the space string: {@code " "}.
     * Useful for string concatenation operations.
     * 
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
     */
    public static final String COMMA = ",";

    /**
     * Represents comma followed by space: {@code ", "}.
     * Commonly used for readable list formatting.
     * 
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
    public static final char _SINGLE_QUOTE = '\'';

    /**
     * Represents the single quotation mark string: {@code "'"}.
     */
    public static final String SINGLE_QUOTE = "'";

    /**
     * Represents space followed by single quotation mark: {@code " '"}.
     * Useful for formatting quoted text.
     */
    public static final String SPACE_SINGLE_QUOTE = " '";

    /**
     * Represents single quotation mark followed by space: {@code "' "}.
     * Useful for formatting quoted text.
     */
    public static final String SINGLE_QUOTE_SPACE = "' ";

    /**
     * Represents the double quotation mark character: {@code '"'}.
     * Used for string literals in many programming languages.
     */
    public static final char _DOUBLE_QUOTE = '"';

    /**
     * Represents the double quotation mark string: {@code "\""}.
     */
    public static final String DOUBLE_QUOTE = "\"";

    /**
     * Represents space followed by double quotation mark: {@code " \""}.
     * Useful for formatting quoted text.
     */
    public static final String SPACE_DOUBLE_QUOTE = " \"";

    /**
     * Represents double quotation mark followed by space: {@code "\" "}.
     * Useful for formatting quoted text.
     */
    public static final String DOUBLE_QUOTE_SPACE = "\" ";

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
    public static final char _VERTICAL_BAR = '|';

    /**
     * Represents the vertical bar string: {@code "|"}.
     */
    public static final String VERTICAL_BAR = "|";

    /**
     * Represents double vertical bars: {@code "||"}.
     * Used as logical OR operator in SQL and some programming languages.
     */
    public static final String DOUBLE_PIPE = "||";

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
    public static final char _PARENTHESIS_L = '(';

    /**
     * Represents the left parenthesis string: {@code "("}.
     */
    public static final String PARENTHESIS_L = "(";

    /**
     * Represents space followed by left parenthesis: {@code " ("}.
     * Useful for formatting function calls.
     */
    public static final String SPACE_PARENTHESIS_L = " (";

    /**
     * Represents the right parenthesis character: {@code ')'}.
     * Used for grouping and function calls.
     */
    public static final char _PARENTHESIS_R = ')';

    /**
     * Represents the right parenthesis string: {@code ")"}.
     */
    public static final String PARENTHESIS_R = ")";

    /**
     * Represents right parenthesis followed by space: {@code ") "}.
     * Useful for formatting function calls.
     */
    public static final String PARENTHESIS_R_SPACE = ") ";

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
     * Represents the left curly brace character: <code>'{'</code>.
     * Used for block delimiters and object literals.
     */
    public static final char _BRACE_L = '{';

    /**
     * Represents the left curly brace string: <code>"{"</code>.
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
    public static final char _TILDE = '~';

    /**
     * Represents the tilde string: {@code "~"}.
     */
    public static final String TILDE = "~";

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
    public static final char _HASH = '#';

    /**
     * Represents the hash/pound string: {@code "#"}.
     */
    public static final String HASH = "#";

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
    public static final String NOT_EQUAL_ANSI = "<>";

    /**
     * Represents the greater than or equal operator: {@code ">="}.
     * Used for comparison operations.
     */
    public static final String GREATER_THAN_OR_EQUAL = ">=";

    /**
     * Represents the less than or equal operator: {@code "<="}.
     * Used for comparison operations.
     */
    public static final String LESS_THAN_OR_EQUAL = "<=";

    // --------------------SQL key words----------------------------
    /**
     * SQL CALL keyword: {@code "CALL"}.
     * Used for calling stored procedures.
     *
     */
    public static final String CALL = "CALL";

    /**
     * SQL WITH keyword: {@code "WITH"}.
     * Used for common table expressions (CTEs).
     * 
     */
    public static final String WITH = "WITH";

    /**
     * SQL MERGE keyword: {@code "MERGE"}.
     * Used for merge (upsert) operations that insert or update based on conditions.
     *
     */
    public static final String MERGE = "MERGE";

    /**
     * SQL SELECT keyword: {@code "SELECT"}.
     * Used to query data from database tables.
     * 
     */
    public static final String SELECT = "SELECT";

    /**
     * SQL INSERT keyword: {@code "INSERT"}.
     * Used to insert new rows into database tables.
     *
     */
    public static final String INSERT = "INSERT";

    /**
     * SQL INTO keyword: {@code "INTO"}.
     * Used with INSERT statements to specify the target table.
     *
     */
    public static final String INTO = "INTO";

    /**
     * SQL UPDATE keyword: {@code "UPDATE"}.
     * Used to modify existing rows in database tables.
     *
     */
    public static final String UPDATE = "UPDATE";

    /**
     * SQL SET keyword: {@code "SET"}.
     * Used with UPDATE statements to specify column values to be modified.
     *
     */
    public static final String SET = "SET";

    /**
     * SQL DELETE keyword: {@code "DELETE"}.
     * Used to remove rows from database tables.
     *
     */
    public static final String DELETE = "DELETE";

    /**
     * SQL CREATE keyword: {@code "CREATE"}.
     * Used to create database objects such as tables, views, indexes, and databases.
     *
     */
    public static final String CREATE = "CREATE";

    /**
     * SQL DROP keyword: {@code "DROP"}.
     * Used to permanently remove database objects such as tables, views, or indexes.
     *
     */
    public static final String DROP = "DROP";

    /**
     * SQL SHOW keyword: {@code "SHOW"}.
     * Used to display database information such as tables, databases, or columns.
     *
     */
    public static final String SHOW = "SHOW";

    /**
     * SQL DESCRIBE keyword: {@code "DESCRIBE"}.
     * Used to show the structure and columns of a table.
     *
     */
    public static final String DESCRIBE = "DESCRIBE";

    /**
     * SQL ALTER keyword: {@code "ALTER"}.
     * Used to modify the structure of existing database objects like tables.
     *
     */
    public static final String ALTER = "ALTER";

    /**
     * SQL USE keyword: {@code "USE"}.
     * Used to select a specific database for subsequent operations.
     *
     */
    public static final String USE = "USE";

    /**
     * SQL RENAME keyword: {@code "RENAME"}.
     * Used to rename database objects such as tables or columns.
     *
     */
    public static final String RENAME = "RENAME";

    /**
     * SQL BEGIN TRANSACTION statement: {@code "BEGIN TRANSACTION"}.
     * Used to start a new database transaction for grouping operations.
     *
     */
    public static final String BEGIN_TRANSACTION = "BEGIN TRANSACTION";

    /**
     * SQL START TRANSACTION statement: {@code "START TRANSACTION"}.
     * Alternative syntax to begin a database transaction, commonly used in MySQL.
     *
     */
    public static final String START_TRANSACTION = "START TRANSACTION";

    /**
     * SQL COMMIT keyword: {@code "COMMIT"}.
     * Used to permanently save all changes made in the current transaction.
     *
     */
    public static final String COMMIT = "COMMIT";

    /**
     * SQL ROLLBACK keyword: {@code "ROLLBACK"}.
     * Used to undo all changes made in the current transaction.
     *
     */
    public static final String ROLLBACK = "ROLLBACK";

    /**
     * SQL AS keyword: {@code "AS"}.
     * Used to create aliases for tables or columns in queries.
     *
     */
    public static final String AS = "AS";

    /**
     * SQL JOIN keyword: {@code "JOIN"}.
     * Used to combine rows from two or more tables based on a related column.
     *
     */
    public static final String JOIN = "JOIN";

    /**
     * SQL NATURAL keyword: {@code "NATURAL"}.
     * Used with JOIN to automatically join tables based on columns with identical names.
     *
     */
    public static final String NATURAL = "NATURAL";

    /**
     * SQL INNER keyword: {@code "INNER"}.
     * Used with JOIN to return rows that have matching values in both tables.
     *
     */
    public static final String INNER = "INNER";

    /**
     * SQL OUTER keyword with trailing space: {@code "OUTER"}.
     * Used with LEFT, RIGHT, or FULL keywords to specify outer join operations.
     *
     */
    public static final String OUTER = "OUTER";

    /**
     * SQL LEFT JOIN clause: {@code "LEFT JOIN"}.
     * Returns all rows from the left table and matching rows from the right table; unmatched rows return NULL.
     *
     */
    public static final String LEFT_JOIN = "LEFT JOIN";

    /**
     * SQL LEFT keyword: {@code "LEFT"}.
     * Used in LEFT JOIN or LEFT OUTER JOIN operations to specify the left table.
     *
     */
    public static final String LEFT = "LEFT";

    /**
     * SQL RIGHT JOIN clause: {@code "RIGHT JOIN"}.
     * Returns all rows from the right table and matching rows from the left table; unmatched rows return NULL.
     *
     */
    public static final String RIGHT_JOIN = "RIGHT JOIN";

    /**
     * SQL RIGHT keyword: {@code "RIGHT"}.
     * Used in RIGHT JOIN or RIGHT OUTER JOIN operations to specify the right table.
     *
     */
    public static final String RIGHT = "RIGHT";

    /**
     * SQL FULL JOIN clause: {@code "FULL JOIN"}.
     * Returns all rows from both tables; unmatched rows return NULL for missing sides.
     *
     */
    public static final String FULL_JOIN = "FULL JOIN";

    /**
     * SQL FULL keyword: {@code "FULL"}.
     * Used in FULL JOIN or FULL OUTER JOIN operations to return all rows from both tables.
     *
     */
    public static final String FULL = "FULL";

    /**
     * SQL CROSS JOIN clause: {@code "CROSS JOIN"}.
     * Returns the Cartesian product of both tables, combining every row from the first with every row from the second.
     *
     */
    public static final String CROSS_JOIN = "CROSS JOIN";

    /**
     * SQL INNER JOIN clause: {@code "INNER JOIN"}.
     * Returns only rows with matching values in both tables.
     *
     */
    public static final String INNER_JOIN = "INNER JOIN";

    /**
     * SQL NATURAL JOIN clause: {@code "NATURAL JOIN"}.
     * Automatically joins tables based on all columns with identical names and compatible types.
     *
     */
    public static final String NATURAL_JOIN = "NATURAL JOIN";

    /**
     * SQL CROSS keyword: {@code "CROSS"}.
     * Used in CROSS JOIN operations to produce the Cartesian product of two tables.
     *
     */
    public static final String CROSS = "CROSS";

    /**
     * SQL ON keyword: {@code "ON"}.
     * Used to specify the join condition that determines how tables are related.
     *
     */
    public static final String ON = "ON";

    /**
     * SQL USING keyword: {@code "USING"}.
     * Alternative to ON for specifying join columns when they have the same name in both tables.
     *
     */
    public static final String USING = "USING";

    /**
     * SQL WHERE keyword: {@code "WHERE"}.
     * Used to filter query results.
     * 
     */
    public static final String WHERE = "WHERE";

    /**
     * SQL GROUP BY clause: {@code "GROUP BY"}.
     * Used to group rows that have the same values in specified columns, often with aggregate functions.
     *
     */
    public static final String GROUP_BY = "GROUP BY";

    /**
     * SQL PARTITION BY clause: {@code "PARTITION BY"}.
     * Used in window functions to divide result sets into partitions for computation.
     *
     */
    public static final String PARTITION_BY = "PARTITION BY";

    /**
     * SQL HAVING keyword: {@code "HAVING"}.
     * Used to filter grouped results based on aggregate function conditions.
     *
     */
    public static final String HAVING = "HAVING";

    /**
     * SQL ORDER BY clause: {@code "ORDER BY"}.
     * Used to sort query results by one or more columns in ascending or descending order.
     *
     */
    public static final String ORDER_BY = "ORDER BY";

    /**
     * SQL LIMIT keyword: {@code "LIMIT"}.
     * Used to limit the maximum number of rows returned by a query.
     *
     */
    public static final String LIMIT = "LIMIT";

    /**
     * SQL OFFSET keyword: {@code "OFFSET"}.
     * Used to skip a specified number of rows before returning results, useful for pagination.
     *
     */
    public static final String OFFSET = "OFFSET";

    /**
     * SQL FOR UPDATE clause: {@code "FOR UPDATE"}.
     * Used to lock selected rows for update within a transaction, preventing other sessions from modifying them.
     *
     */
    public static final String FOR_UPDATE = "FOR UPDATE";

    /**
     * SQL FETCH FIRST clause: {@code "FETCH FIRST"}.
     * Standard SQL syntax for limiting the number of rows returned, used with ROWS ONLY.
     *
     */
    public static final String FETCH_FIRST = "FETCH FIRST";

    /**
     * SQL FETCH NEXT clause: {@code "FETCH NEXT"}.
     * Used with OFFSET to fetch the next set of rows for pagination, similar to FETCH FIRST.
     *
     */
    public static final String FETCH_NEXT = "FETCH NEXT";

    /**
     * SQL ROWS keyword: {@code "ROWS"}.
     * Used with FETCH clauses to specify row limits and in window function frame specifications.
     *
     */
    public static final String ROWS = "ROWS";

    /**
     * SQL ROWS ONLY clause: {@code "ROWS ONLY"}.
     * Used to complete FETCH FIRST or FETCH NEXT syntax in standard SQL pagination.
     *
     */
    public static final String ROWS_ONLY = "ROWS ONLY";

    /**
     * SQL ROWNUM keyword: {@code "ROWNUM"}.
     * Oracle-specific pseudo-column that assigns a sequential number to each row in a result set.
     *
     */
    public static final String ROWNUM = "ROWNUM";

    /**
     * SQL EXISTS keyword: {@code "EXISTS"}.
     * Used to test whether a subquery returns any rows; returns true if at least one row exists.
     *
     */
    public static final String EXISTS = "EXISTS";

    /**
     * SQL LIKE keyword: {@code "LIKE"}.
     * Used for pattern matching in WHERE clauses with wildcards (% for multiple characters, _ for single character).
     *
     */
    public static final String LIKE = "LIKE";

    /**
     * SQL AND keyword: {@code "AND"}.
     * Logical AND operator that combines multiple conditions; returns true only if all conditions are true.
     *
     */
    public static final String AND = "AND";

    /**
     * Logical AND operator symbol: {@code "&&"}.
     * Alternative AND syntax supported in MySQL and some other databases.
     *
     */
    public static final String AND_OP = "&&";

    /**
     * SQL OR keyword: {@code "OR"}.
     * Logical OR operator that combines multiple conditions; returns true if at least one condition is true.
     *
     */
    public static final String OR = "OR";

    /**
     * Logical OR operator symbol: {@code "||"}.
     * Alternative OR syntax in some databases; also used as string concatenation in Oracle and PostgreSQL.
     *
     */
    public static final String OR_OP = "||";

    /**
     * SQL XOR keyword: {@code "XOR"}.
     * Exclusive OR operator that returns true if exactly one of the conditions is true, but not both.
     *
     */
    public static final String XOR = "XOR";

    /**
     * SQL NOT keyword: {@code "NOT"}.
     * Logical NOT operator that negates a condition; returns true if the condition is false.
     *
     */
    public static final String NOT = "NOT";

    /**
     * SQL BETWEEN keyword: {@code "BETWEEN"}.
     * Used to filter values within an inclusive range (includes boundary values).
     *
     */
    public static final String BETWEEN = "BETWEEN";

    /**
     * SQL IS keyword: {@code "IS"}.
     * Used for NULL comparisons; standard way to test for NULL values.
     *
     */
    public static final String IS = "IS";

    /**
     * SQL IS NOT clause: {@code "IS NOT"}.
     * Used to test for non-NULL values; returns true if the value is not NULL.
     *
     */
    public static final String IS_NOT = "IS NOT";

    /**
     * SQL NULL keyword: {@code "NULL"}.
     * Represents the absence of a value or unknown value in a database.
     *
     */
    public static final String NULL = "NULL";

    /**
     * SQL IS NULL clause: {@code "IS NULL"}.
     * Tests whether a column or expression contains a NULL value.
     *
     */
    public static final String IS_NULL = "IS NULL";

    /**
     * SQL IS NOT NULL clause: {@code "IS NOT NULL"}.
     * Tests whether a column or expression does not contain a NULL value.
     *
     */
    public static final String IS_NOT_NULL = "IS NOT NULL";

    /**
     * EMPTY keyword: {@code "EMPTY"}.
     * Custom keyword used for checking if a collection or string is empty (zero length).
     *
     */
    public static final String EMPTY = "EMPTY";

    /**
     * IS EMPTY clause: {@code "IS EMPTY"}.
     * Custom clause for checking if a collection or string is empty (zero length).
     *
     */
    public static final String IS_EMPTY = "IS EMPTY";

    /**
     * IS NOT EMPTY clause: {@code "IS NOT EMPTY"}.
     * Custom clause for checking if a collection or string is not empty (has content).
     *
     */
    public static final String IS_NOT_EMPTY = "IS NOT EMPTY";

    /**
     * BLANK keyword: {@code "BLANK"}.
     * Custom keyword for checking if a string is blank (empty or contains only whitespace).
     *
     */
    public static final String BLANK = "BLANK";

    /**
     * IS BLANK clause: {@code "IS BLANK"}.
     * Custom clause for checking if a string is blank (empty or contains only whitespace).
     *
     */
    public static final String IS_BLANK = "IS BLANK";

    /**
     * IS NOT BLANK clause: {@code "IS NOT BLANK"}.
     * Custom clause for checking if a string is not blank (has non-whitespace content).
     *
     */
    public static final String IS_NOT_BLANK = "IS NOT BLANK";

    /**
     * SQL NOT IN clause: {@code "NOT IN"}.
     * Tests if a value is not present in a specified list or subquery.
     *
     */
    public static final String NOT_IN = "NOT IN";

    /**
     * SQL NOT EXISTS clause: {@code "NOT EXISTS"}.
     * Tests whether a subquery returns no rows; returns true if the subquery is empty.
     *
     */
    public static final String NOT_EXISTS = "NOT EXISTS";

    /**
     * SQL NOT LIKE clause: {@code "NOT LIKE"}.
     * Tests for pattern non-matching with wildcards; returns true if pattern doesn't match.
     *
     */
    public static final String NOT_LIKE = "NOT LIKE";

    /**
     * SQL FROM keyword: {@code "FROM"}.
     * Specifies the source table(s) from which to retrieve data in a query.
     *
     */
    public static final String FROM = "FROM";

    /**
     * SQL ASC keyword: {@code "ASC"}.
     * Specifies ascending sort order (lowest to highest).
     *
     */
    public static final String ASC = "ASC";

    /**
     * SQL DESC keyword: {@code "DESC"}.
     * Specifies descending sort order (highest to lowest).
     *
     */
    public static final String DESC = "DESC";

    /**
     * SQL VALUES keyword: {@code "VALUES"}.
     * Used in INSERT statements to specify the values to be inserted.
     *
     */
    public static final String VALUES = "VALUES";

    /**
     * SQL DISTINCT keyword: {@code "DISTINCT"}.
     * Removes duplicate rows from query results, returning only unique rows.
     *
     */
    public static final String DISTINCT = "DISTINCT";

    /**
     * SQL DISTINCTROW keyword: {@code "DISTINCTROW"}.
     * MySQL-specific keyword similar to DISTINCT for removing duplicate rows.
     *
     */
    public static final String DISTINCTROW = "DISTINCTROW";

    /**
     * SQL UNIQUE keyword: {@code "UNIQUE"}.
     * Constraint that ensures all values in a column are unique (no duplicates).
     *
     */
    public static final String UNIQUE = "UNIQUE";

    /**
     * SQL TOP keyword: {@code "TOP"}.
     * SQL Server and MS Access syntax for limiting the number of returned rows.
     *
     */
    public static final String TOP = "TOP";

    /**
     * SQL IN keyword: {@code "IN"}.
     * Tests if a value matches any value in a specified list or subquery.
     *
     */
    public static final String IN = "IN";

    /**
     * SQL ANY keyword: {@code "ANY"}.
     * Used with comparison operators and subqueries; returns true if any subquery value satisfies the condition.
     *
     */
    public static final String ANY = "ANY";

    /**
     * SQL ALL keyword: {@code "ALL"}.
     * Used with comparison operators and subqueries; returns true if all subquery values satisfy the condition.
     *
     */
    public static final String ALL = "ALL";

    /**
     * SQL SOME keyword: {@code "SOME"}.
     * Synonym for ANY; used with comparison operators and subqueries.
     *
     */
    public static final String SOME = "SOME";

    /**
     * SQL UNION keyword: {@code "UNION"}.
     * Combines results of multiple SELECT queries, removing duplicate rows.
     *
     */
    public static final String UNION = "UNION";

    /**
     * SQL UNION ALL clause: {@code "UNION ALL"}.
     * Combines results of multiple SELECT queries, including all duplicate rows.
     *
     */
    public static final String UNION_ALL = "UNION ALL";

    /**
     * SQL INTERSECT keyword: {@code "INTERSECT"}.
     * Returns only rows that appear in both query results (set intersection).
     *
     */
    public static final String INTERSECT = "INTERSECT";

    /**
     * SQL EXCEPT keyword: {@code "EXCEPT"}.
     * Returns rows from the first query that are not in the second query (set difference).
     *
     */
    public static final String EXCEPT = "EXCEPT";

    /**
     * SQL MINUS keyword: {@code "MINUS"}.
     * Oracle's equivalent of EXCEPT; returns rows from first query not in second query.
     *
     */
    public static final String EXCEPT_MINUS = "MINUS";

    /**
     * SQL AVG function: {@code "AVG"}.
     * Calculates the average (arithmetic mean) of numeric values in a column.
     *
     */
    public static final String AVG = "AVG";

    /**
     * SQL COUNT function: {@code "COUNT"}.
     * Counts the number of rows or non-NULL values in a result set.
     *
     */
    public static final String COUNT = "COUNT";

    /**
     * SQL SUM function: {@code "SUM"}.
     * Calculates the sum (total) of numeric values in a column.
     *
     */
    public static final String SUM = "SUM";

    /**
     * SQL MIN function: {@code "MIN"}.
     * Returns the minimum (smallest) value in a column.
     *
     */
    public static final String MIN = "MIN";

    /**
     * SQL MAX function: {@code "MAX"}.
     * Returns the maximum (largest) value in a column.
     *
     */
    public static final String MAX = "MAX";

    /**
     * SQL ABS function: {@code "ABS"}.
     * Returns the absolute (positive) value of a number, removing any negative sign.
     *
     */
    public static final String ABS = "ABS";

    /**
     * SQL ACOS function: {@code "ACOS"}.
     * Returns the arc cosine (inverse cosine) of a number in radians; input must be between -1 and 1.
     *
     */
    public static final String ACOS = "ACOS";

    /**
     * SQL ASIN function: {@code "ASIN"}.
     * Returns the arc sine (inverse sine) of a number in radians; input must be between -1 and 1.
     *
     */
    public static final String ASIN = "ASIN";

    /**
     * SQL ATAN function: {@code "ATAN"}.
     * Returns the arc tangent (inverse tangent) of a number in radians.
     *
     */
    public static final String ATAN = "ATAN";

    /**
     * SQL ATAN2 function: {@code "ATAN2"}.
     * Returns the arc tangent of y/x in radians, using the signs to determine the quadrant.
     *
     */
    public static final String ATAN2 = "ATAN2";

    /**
     * SQL CEIL function: {@code "CEIL"}.
     * Rounds a number up to the nearest integer (ceiling function).
     *
     */
    public static final String CEIL = "CEIL";

    /**
     * SQL COS function: {@code "COS"}.
     * Returns the cosine of an angle in radians.
     *
     */
    public static final String COS = "COS";

    /**
     * SQL EXP function: {@code "EXP"}.
     * Returns e (Euler's number, approximately 2.71828) raised to the power of the given number.
     *
     */
    public static final String EXP = "EXP";

    /**
     * SQL FLOOR function: {@code "FLOOR"}.
     * Rounds a number down to the nearest integer (floor function).
     *
     */
    public static final String FLOOR = "FLOOR";

    /**
     * SQL LOG function: {@code "LOG"}.
     * Returns the logarithm of a number; base depends on database (often base 10 or natural log).
     *
     */
    public static final String LOG = "LOG";

    /**
     * SQL LN function: {@code "LN"}.
     * Returns the natural logarithm (base e) of a number.
     *
     */
    public static final String LN = "LN";

    /**
     * SQL MOD function: {@code "MOD"}.
     * Returns the remainder (modulo) of dividing one number by another.
     *
     */
    public static final String MOD = "MOD";

    /**
     * SQL POWER function: {@code "POWER"}.
     * Raises a number to the power of another number (exponentiation).
     *
     */
    public static final String POWER = "POWER";

    /**
     * SQL SIGN function: {@code "SIGN"}.
     * Returns the sign of a number: -1 for negative, 0 for zero, 1 for positive.
     *
     */
    public static final String SIGN = "SIGN";

    /**
     * SQL SIN function: {@code "SIN"}.
     * Returns the sine of an angle in radians.
     *
     */
    public static final String SIN = "SIN";

    /**
     * SQL SQRT function: {@code "SQRT"}.
     * Returns the square root of a number.
     *
     */
    public static final String SQRT = "SQRT";

    /**
     * SQL TAN function: {@code "TAN"}.
     * Returns the tangent of an angle in radians.
     *
     */
    public static final String TAN = "TAN";

    /**
     * SQL LENGTH function: {@code "LENGTH"}.
     * Returns the number of characters in a string.
     *
     */
    public static final String LENGTH = "LENGTH";

    /**
     * SQL CONCAT function: {@code "CONCAT"}.
     * Concatenates (joins) two or more strings together.
     *
     */
    public static final String CONCAT = "CONCAT";

    /**
     * SQL TRIM function: {@code "TRIM"}.
     * Removes leading and trailing whitespace from a string.
     *
     */
    public static final String TRIM = "TRIM";

    /**
     * SQL LTRIM function: {@code "LTRIM"}.
     * Removes leading (left-side) whitespace from a string.
     *
     */
    public static final String LTRIM = "LTRIM";

    /**
     * SQL RTRIM function: {@code "RTRIM"}.
     * Removes trailing (right-side) whitespace from a string.
     *
     */
    public static final String RTRIM = "RTRIM";

    /**
     * SQL LPAD function: {@code "LPAD"}.
     * Left-pads a string to a specified length with a given character or string.
     *
     */
    public static final String LPAD = "LPAD";

    /**
     * SQL RPAD function: {@code "RPAD"}.
     * Right-pads a string to a specified length with a given character or string.
     *
     */
    public static final String RPAD = "RPAD";

    /**
     * SQL REPLACE function: {@code "REPLACE"}.
     * Replaces all occurrences of a substring with another substring.
     *
     */
    public static final String REPLACE = "REPLACE";

    /**
     * SQL SUBSTR function: {@code "SUBSTR"}.
     * Extracts a substring from a string starting at a specified position.
     *
     */
    public static final String SUBSTR = "SUBSTR";

    /**
     * SQL UPPER function: {@code "UPPER"}.
     * Converts all characters in a string to uppercase.
     *
     */
    public static final String UPPER = "UPPER";

    /**
     * SQL LOWER function: {@code "LOWER"}.
     * Converts all characters in a string to lowercase.
     *
     */
    public static final String LOWER = "LOWER";

    /**
     * SQL CAST function: {@code "CAST"}.
     * Converts a value from one data type to another.
     *
     */
    public static final String CAST = "CAST";

    /**
     * SQL CURRENT_TIME function: {@code "CURRENT_TIME"}.
     * Returns the current time (without date) from the database server.
     *
     */
    public static final String CURRENT_TIME = "CURRENT_TIME";

    /**
     * SQL CURRENT_DATE function: {@code "CURRENT_DATE"}.
     * Returns the current date (without time) from the database server.
     *
     */
    public static final String CURRENT_DATE = "CURRENT_DATE";

    /**
     * SQL CURRENT_TIMESTAMP function: {@code "CURRENT_TIMESTAMP"}.
     * Returns the current date and time (timestamp) from the database server.
     *
     */
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    // --------------------SQL key words----------------------------
}
