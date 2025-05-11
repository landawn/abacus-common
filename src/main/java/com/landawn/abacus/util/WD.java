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
 * A dictionary of common characters and strings.
 *
 */
@Beta
@Internal
public final class WD {

    private WD() {
        // singleton
    }

    /**
     * Represents character: {@code (char) 0}
     */
    public static final char CHAR_ZERO = (char) 0;

    /**
     * Represents character: {@code '\n'}
     */
    public static final char CHAR_LF = '\n';

    /**
     * Represents character: {@code '\r'}
     */
    public static final char CHAR_CR = '\r';

    /**
     * Represents character: {@code ' '}
     */
    public static final char _SPACE = ' ';

    /**
     * Represents string: {@code " "}
     */
    public static final String SPACE = " ";

    /**
     * Represents character: {@code '.'}
     */
    public static final char _PERIOD = '.';

    /**
     * Represents string: {@code "."}
     */
    public static final String PERIOD = ".";

    /**
     * Represents character: {@code ','}
     */
    public static final char _COMMA = ',';

    /**
     * Represents string: {@code ","}
     */
    public static final String COMMA = ",";

    /**
     * Represents string: {@code ", "}
     */
    public static final String COMMA_SPACE = ", ";

    /**
     * Represents character: {@code ':'}
     */
    public static final char _COLON = ':';

    /**
     * Represents string: {@code ":"}
     */
    public static final String COLON = ":";

    /**
     * Represents string: {@code ": "}
     */
    public static final String COLON_SPACE = ": ";

    /**
     * Represents character: {@code ';'}
     */
    public static final char _SEMICOLON = ';';

    /**
     * Represents string: {@code ";"}
     */
    public static final String SEMICOLON = ";";

    /**
     * Represents string: {@code "; "}
     */
    public static final String SEMICOLON_SPACE = "; ";

    /**
     * Represents character: {@code '\\'}
     */
    public static final char _BACKSLASH = '\\';

    /**
     * Represents string: {@code "\\"}
     */
    public static final String BACKSLASH = "\\";

    /**
     * Represents character: {@code '\''}
     */
    public static final char _QUOTATION_S = '\'';

    /**
     * Represents string: {@code "'"}
     */
    public static final String QUOTATION_S = "'";

    /**
     * Represents string: {@code " '"}
     */
    public static final String SPACE_QUOTATION_S = " '";

    /**
     * Represents string: {@code "' "}
     */
    public static final String QUOTATION_S_SPACE = "' ";

    /**
     * Represents character: {@code '"'}
     */
    public static final char _QUOTATION_D = '"';

    /**
     * Represents string: {@code "\""}
     */
    public static final String QUOTATION_D = "\"";

    /**
     * Represents string: {@code " \""}
     */
    public static final String SPACE_QUOTATION_D = " \"";

    /**
     * Represents string: {@code "\" "}
     */
    public static final String QUOTATION_D_SPACE = "\" ";

    /**
     * Represents character: {@code '&'}
     */
    public static final char _AMPERSAND = '&';

    /**
     * Represents string: {@code "&"}
     */
    public static final String AMPERSAND = "&";

    /**
     * Represents character: {@code '|'}
     */
    public static final char _VERTICALBAR = '|';

    /**
     * Represents string: {@code "|"}
     */
    public static final String VERTICALBAR = "|";

    /**
     * Represents string: {@code "||"}
     */
    public static final String PARALLEL = "||";

    /**
     * Represents character: {@code '_'}
     */
    public static final char _UNDERSCORE = '_';

    /**
     * Represents string: {@code "_"}
     */
    public static final String UNDERSCORE = "_";

    /**
     * Represents character: {@code '<'}
     */
    public static final char _LESS_THAN = '<';

    /**
     * Represents string: {@code "<"}
     */
    public static final String LESS_THAN = "<";

    /**
     * Represents character: {@code '>'}
     */
    public static final char _GREATER_THAN = '>';

    /**
     * Represents string: {@code ">"}
     */
    public static final String GREATER_THAN = ">";

    /**
     * Represents character: {@code '='}
     */
    public static final char _EQUAL = '=';

    /**
     * Represents string: {@code "="}
     */
    public static final String EQUAL = "=";

    /**
     * Represents character: {@code '+'}
     */
    public static final char _PLUS = '+';

    /**
     * Represents string: {@code "+"}
     */
    public static final String PLUS = "+";

    /**
     * Represents character: {@code '-'}
     */
    public static final char _MINUS = '-';

    /**
     * Represents string: {@code "-"}
     */
    public static final String MINUS = "-";

    /**
     * Represents character: {@code '%'}
     */
    public static final char _PERCENT = '%';

    /**
     * Represents string: {@code "%"}
     */
    public static final String PERCENT = "%";

    /**
     * Represents character: {@code '/'}
     */
    public static final char _SLASH = '/';

    /**
     * Represents string: {@code "/"}
     */
    public static final String SLASH = "/";

    /**
     * Represents character: {@code '*'}
     */
    public static final char _ASTERISK = '*';

    /**
     * Represents string: {@code "*"}
     */
    public static final String ASTERISK = "*";

    /**
     * Represents character: {@code '?'}
     */
    public static final char _QUESTION_MARK = '?';

    /**
     * Represents string: {@code "?"}
     */
    public static final String QUESTION_MARK = "?";

    /**
     * Represents character: {@code '('}
     */
    public static final char _PARENTHESES_L = '(';

    /**
     * Represents string: {@code "("}
     */
    public static final String PARENTHESES_L = "(";

    /**
     * Represents string: {@code " ("}
     */
    public static final String SPACE_PARENTHESES_L = " (";

    /**
     * Represents character: {@code ')'}
     */
    public static final char _PARENTHESES_R = ')';

    /**
     * Represents string: {@code ")"}
     */
    public static final String PARENTHESES_R = ")";

    /**
     * Represents string: {@code ") "}
     */
    public static final String PARENTHESES_R_SPACE = ") ";

    /**
     * Represents character: {@code '['}
     */
    public static final char _BRACKET_L = '[';

    /**
     * Represents string: {@code "["}
     */
    public static final String BRACKET_L = "[";

    /**
     * Represents character: {@code ']'}
     */
    public static final char _BRACKET_R = ']';

    /**
     * Represents string: {@code "]"}
     */
    public static final String BRACKET_R = "]";

    /**
     * Represents character: {@code '{'}
     */
    public static final char _BRACE_L = '{';

    /**
     * Represents string: {@code "{"}
     */
    public static final String BRACE_L = "{";

    /**
     * Represents character: {@code '}'}
     */
    public static final char _BRACE_R = '}';

    /**
     * Represents string: {@code "}"}
     */
    public static final String BRACE_R = "}";

    /**
     * Represents character: {@code '^'}
     */
    public static final char _CIRCUMFLEX = '^';

    /**
     * Represents string: {@code "^"}
     */
    public static final String CIRCUMFLEX = "^";

    /**
     * Represents character: {@code '~'}
     */
    public static final char _UNARYBIT = '~';

    /**
     * Represents string: {@code "~"}
     */
    public static final String UNARYBIT = "~";

    /**
     * Represents character: {@code '$'}
     */
    public static final char _DOLLAR = '$';

    /**
     * Represents string: {@code "$"}
     */
    public static final String DOLLAR = "$";

    /**
     * Represents character: {@code '#'}
     */
    public static final char _SHARP = '#';

    /**
     * Represents string: {@code "#"}
     */
    public static final String SHARP = "#";

    /**
     * Represents character: {@code '!'}
     */
    public static final char _EXCLAMATION = '!';

    /**
     * Represents string: {@code "!"}
     */
    public static final String EXCLAMATION = "!";

    /**
     * Represents string: {@code "!="}
     */
    public static final String NOT_EQUAL = "!=";

    /**
     * Represents string: {@code "<>"}
     */
    public static final String NOT_EQUAL2 = "<>";

    /**
     * Represents string: {@code ">="}
     */
    public static final String GREATER_EQUAL = ">=";

    /**
     * Represents string: {@code "<="}
     */
    public static final String LESS_EQUAL = "<=";

    // --------------------SQL key words----------------------------
    /**
     * Represents string: {@code "WITH"}
     */
    public static final String WITH = "WITH";

    /**
     * Represents string: {@code "MERGE"}
     */
    public static final String MERGE = "MERGE";

    /**
     * Represents string: {@code "SELECT"}
     */
    public static final String SELECT = "SELECT";

    /**
     * Represents string: {@code "INSERT"}
     */
    public static final String INSERT = "INSERT";

    /**
     * Represents string: {@code "INTO"}
     */
    public static final String INTO = "INTO";

    /**
     * Represents string: {@code "UPDATE"}
     */
    public static final String UPDATE = "UPDATE";

    /**
     * Represents string: {@code "SET"}
     */
    public static final String SET = "SET";

    /**
     * Represents string: {@code "DELETE"}
     */
    public static final String DELETE = "DELETE";

    /**
     * Represents string: {@code "CREATE"}
     */
    public static final String CREATE = "CREATE";

    /**
     * Represents string: {@code "DROP"}
     */
    public static final String DROP = "DROP";

    /**
     * Represents string: {@code "SHOW"}
     */
    public static final String SHOW = "SHOW";

    /**
     * Represents string: {@code "DESCRIBE"}
     */
    public static final String DESCRIBE = "DESCRIBE";

    /**
     * Represents string: {@code "ALTER"}
     */
    public static final String ALTER = "ALTER";

    /**
     * Represents string: {@code "USE"}
     */
    public static final String USE = "USE";

    /**
     * Represents string: {@code "RENAME"}
     */
    public static final String RENAME = "RENAME";

    /**
     * Represents string: {@code "BEGIN TRANSACTION"}
     */
    public static final String BEGIN_TRANSACTION = "BEGIN TRANSACTION";

    /**
     * Represents string: {@code "START TRANSACTION"}
     */
    public static final String START_TRANSACTION = "START TRANSACTION";

    /**
     * Represents string: {@code "COMMIT"}
     */
    public static final String COMMIT = "COMMIT";

    /**
     * Represents string: {@code "ROLLBACK"}
     */
    public static final String ROLLBACK = "ROLLBACK";

    /**
     * Represents string: {@code "AS"}
     */
    public static final String AS = "AS";

    /**
     * Represents string: {@code "JOIN"}
     */
    public static final String JOIN = "JOIN";

    /**
     * Represents string: {@code "NATURAL"}
     */
    public static final String NATURAL = "NATURAL";

    /**
     * Represents string: {@code "INNER"}
     */
    public static final String INNER = "INNER";

    /**
     * Represents string: {@code "OUTER "}
     */
    public static final String OUTER = "OUTER ";

    /**
     * Represents string: {@code "LEFT JOIN"}
     */
    public static final String LEFT_JOIN = "LEFT JOIN";

    /**
     * Represents string: {@code "LEFT"}
     */
    public static final String LEFT = "LEFT";

    /**
     * Represents string: {@code "RIGHT JOIN"}
     */
    public static final String RIGHT_JOIN = "RIGHT JOIN";

    /**
     * Represents string: {@code "RIGHT"}
     */
    public static final String RIGHT = "RIGHT";

    /**
     * Represents string: {@code "FULL JOIN"}
     */
    public static final String FULL_JOIN = "FULL JOIN";

    /**
     * Represents string: {@code "FULL"}
     */
    public static final String FULL = "FULL";

    /**
     * Represents string: {@code "CROSS JOIN"}
     */
    public static final String CROSS_JOIN = "CROSS JOIN";

    /**
     * Represents string: {@code "INNER JOIN"}
     */
    public static final String INNER_JOIN = "INNER JOIN";

    /**
     * Represents string: {@code "NATURAL JOIN"}
     */
    public static final String NATURAL_JOIN = "NATURAL JOIN";

    /**
     * Represents string: {@code "CROSS"}
     */
    public static final String CROSS = "CROSS";

    /**
     * Represents string: {@code "ON"}
     */
    public static final String ON = "ON";

    /**
     * Represents string: {@code "USING"}
     */
    public static final String USING = "USING";

    /**
     * Represents string: {@code "WHERE"}
     */
    public static final String WHERE = "WHERE";

    /**
     * Represents string: {@code "GROUP BY"}
     */
    public static final String GROUP_BY = "GROUP BY";

    /**
     * Represents string: {@code "HAVING"}
     */
    public static final String HAVING = "HAVING";

    /**
     * Represents string: {@code "ORDER BY"}
     */
    public static final String ORDER_BY = "ORDER BY";

    /**
     * Represents string: {@code "LIMIT"}
     */
    public static final String LIMIT = "LIMIT";

    /**
     * Represents string: {@code "OFFSET"}
     */
    public static final String OFFSET = "OFFSET";

    /**
     * Represents string: {@code "FOR UPDATE"}
     */
    public static final String FOR_UPDATE = "FOR UPDATE";

    /**
     * Represents string: {@code "FETCH FIRST"}
     */
    public static final String FETCH_FIRST = "FETCH FIRST";

    /**
     * Represents string: {@code "FETCH NEXT"}
     */
    public static final String FETCH_NEXT = "FETCH NEXT";

    /**
     * Represents string: {@code "ROWS"}
     */
    public static final String ROWS = "ROWS";

    /**
     * Represents string: {@code "ROWS ONLY"}
     */
    public static final String ROWS_ONLY = "ROWS ONLY";

    /**
     * Represents string: {@code "ROW_NEXT"}
     */
    public static final String ROW_NEXT = "ROW_NEXT";

    /**
     * Represents string: {@code "ROWNUM"}
     */
    public static final String ROWNUM = "ROWNUM";

    /**
     * Represents string: {@code "EXISTS"}
     */
    public static final String EXISTS = "EXISTS";

    /**
     * Represents string: {@code "LIKE"}
     */
    public static final String LIKE = "LIKE";

    /**
     * Represents string: {@code "AND"}
     */
    public static final String AND = "AND";

    /**
     * Represents string: {@code "&&"}
     */
    public static final String AND_OP = "&&";

    /**
     * Represents string: {@code "OR"}
     */
    public static final String OR = "OR";

    /**
     * Represents string: {@code "||"}
     */
    public static final String OR_OP = "||";

    /**
     * Represents string: {@code "XOR"}
     */
    public static final String XOR = "XOR";

    /**
     * Represents string: {@code "NOT"}
     */
    public static final String NOT = "NOT";

    /**
     * Represents string: {@code "BETWEEN"}
     */
    public static final String BETWEEN = "BETWEEN";

    /**
     * Represents string: {@code "IS"}
     */
    public static final String IS = "IS";

    /**
     * Represents string: {@code "IS NOT"}
     */
    public static final String IS_NOT = "IS NOT";

    /**
     * Represents string: {@code "NULL"}
     */
    public static final String NULL = "NULL";

    /**
     * Represents string: {@code "IS NULL"}
     */
    public static final String IS_NULL = "IS NULL";

    /**
     * Represents string: {@code "IS NOT NULL"}
     */
    public static final String IS_NOT_NULL = "IS NOT NULL";

    /**
     * Represents string: {@code "EMPTY"}
     */
    public static final String EMPTY = "EMPTY";

    /**
     * Represents string: {@code "IS EMPTY"}
     */
    public static final String IS_EMPTY = "IS EMPTY";

    /**
     * Represents string: {@code "IS NOT EMPTY"}
     */
    public static final String IS_NOT_EMPTY = "IS NOT EMPTY";

    /**
     * Represents string: {@code "BLANK"}
     */
    public static final String BLANK = "BLANK";

    /**
     * Represents string: {@code "IS BLANK"}
     */
    public static final String IS_BLANK = "IS BLANK";

    /**
     * Represents string: {@code "IS NOT BLANK"}
     */
    public static final String IS_NOT_BLANK = "IS NOT BLANK";

    /**
     * Represents string: {@code "NOT IN"}
     */
    public static final String NOT_IN = "NOT IN";

    /**
     * Represents string: {@code "NOT EXISTS"}
     */
    public static final String NOT_EXISTS = "NOT EXISTS";

    /**
     * Represents string: {@code "FROM"}
     */
    public static final String FROM = "FROM";

    /**
     * Represents string: {@code "ASC"}
     */
    public static final String ASC = "ASC";

    /**
     * Represents string: {@code "DESC"}
     */
    public static final String DESC = "DESC";

    /**
     * Represents string: {@code "VALUES"}
     */
    public static final String VALUES = "VALUES";

    /**
     * Represents string: {@code "DISTINCT"}
     */
    public static final String DISTINCT = "DISTINCT";

    /**
     * Represents string: {@code "DISTINCTROW"}
     */
    public static final String DISTINCTROW = "DISTINCTROW";

    /**
     * Represents string: {@code "UNIQUE"}
     */
    public static final String UNIQUE = "UNIQUE";

    /**
     * Represents string: {@code "TOP"}
     */
    public static final String TOP = "TOP";

    /**
     * Represents string: {@code "IN"}
     */
    public static final String IN = "IN";

    /**
     * Represents string: {@code "ANY"}
     */
    public static final String ANY = "ANY";

    /**
     * Represents string: {@code "ALL"}
     */
    public static final String ALL = "ALL";

    /**
     * Represents string: {@code "SOME"}
     */
    public static final String SOME = "SOME";

    /**
     * Represents string: {@code "UNION"}
     */
    public static final String UNION = "UNION";

    /**
     * Represents string: {@code "UNION ALL"}
     */
    public static final String UNION_ALL = "UNION ALL";

    /**
     * Represents string: {@code "INTERSECT"}
     */
    public static final String INTERSECT = "INTERSECT";

    /**
     * Represents string: {@code "EXCEPT"}
     */
    public static final String EXCEPT = "EXCEPT";

    /**
     * Represents string: {@code "MINUS"}
     */
    public static final String EXCEPT2 = "MINUS";

    /**
     * Represents string: {@code "AVG"}
     */
    public static final String AVG = "AVG";

    /**
     * Represents string: {@code "COUNT"}
     */
    public static final String COUNT = "COUNT";

    /**
     * Represents string: {@code "SUM"}
     */
    public static final String SUM = "SUM";

    /**
     * Represents string: {@code "MIN"}
     */
    public static final String MIN = "MIN";

    /**
     * Represents string: {@code "MAX"}
     */
    public static final String MAX = "MAX";

    /**
     * Represents string: {@code "ABS"}
     */
    public static final String ABS = "ABS";

    /**
     * Represents string: {@code "ACOS"}
     */
    public static final String ACOS = "ACOS";

    /**
     * Represents string: {@code "ASIN"}
     */
    public static final String ASIN = "ASIN";

    /**
     * Represents string: {@code "ATAN"}
     */
    public static final String ATAN = "ATAN";

    /**
     * Represents string: {@code "ATAN2"}
     */
    public static final String ATAN2 = "ATAN2";

    /**
     * Represents string: {@code "CEIL"}
     */
    public static final String CEIL = "CEIL";

    /**
     * Represents string: {@code "COS"}
     */
    public static final String COS = "COS";

    /**
     * Represents string: {@code "EXP"}
     */
    public static final String EXP = "EXP";

    /**
     * Represents string: {@code "FLOOR"}
     */
    public static final String FLOOR = "FLOOR";

    /**
     * Represents string: {@code "LOG"}
     */
    public static final String LOG = "LOG";

    /**
     * Represents string: {@code "LN"}
     */
    public static final String LN = "LN";

    /**
     * Represents string: {@code "MOD"}
     */
    public static final String MOD = "MOD";

    /**
     * Represents string: {@code "POWER"}
     */
    public static final String POWER = "POWER";

    /**
     * Represents string: {@code "SIGN"}
     */
    public static final String SIGN = "SIGN";

    /**
     * Represents string: {@code "SIN"}
     */
    public static final String SIN = "SIN";

    /**
     * Represents string: {@code "SQRT"}
     */
    public static final String SQRT = "SQRT";

    /**
     * Represents string: {@code "TAN"}
     */
    public static final String TAN = "TAN";

    /**
     * Represents string: {@code "LENGTH"}
     */
    public static final String LENGTH = "LENGTH";

    /**
     * Represents string: {@code "CONCAT"}
     */
    public static final String CONCAT = "CONCAT";

    /**
     * Represents string: {@code "TRIM"}
     */
    public static final String TRIM = "TRIM";

    /**
     * Represents string: {@code "LTRIM"}
     */
    public static final String LTRIM = "LTRIM";

    /**
     * Represents string: {@code "RTRIM"}
     */
    public static final String RTRIM = "RTRIM";

    /**
     * Represents string: {@code "LPAD"}
     */
    public static final String LPAD = "LPAD";

    /**
     * Represents string: {@code "RPAD"}
     */
    public static final String RPAD = "RPAD";

    /**
     * Represents string: {@code "REPLACE"}
     */
    public static final String REPLACE = "REPLACE";

    /**
     * Represents string: {@code "SUBSTR"}
     */
    public static final String SUBSTR = "SUBSTR";

    /**
     * Represents string: {@code "UPPER"}
     */
    public static final String UPPER = "UPPER";

    /**
     * Represents string: {@code "LOWER"}
     */
    public static final String LOWER = "LOWER";

    /**
     * Represents string: {@code "CAST"}
     */
    public static final String CAST = "CAST";

    /**
     * Represents string: {@code "CURRENT_TIME"}
     */
    public static final String CURRENT_TIME = "CURRENT_TIME";

    /**
     * Represents string: {@code "CURRENT_DATE"}
     */
    public static final String CURRENT_DATE = "CURRENT_DATE";

    /**
     * Represents string: {@code "CURRENT_TIMESTAMP"}
     */
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    // --------------------SQL key words----------------------------
}
