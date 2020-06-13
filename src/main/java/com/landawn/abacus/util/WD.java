/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * A table of constant Strings/chars.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Beta
@Internal
public final class WD {

    /**
     * Instantiates a new wd.
     */
    private WD() {
        // singleton
    }

    /** The Constant CHAR_0. */
    // ...
    public static final char CHAR_0 = (char) 0;

    /**
     * {@code} linefeed LF ('\n').
     * 
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences for
     *      Character and String Literals</a>
     * @since 2.2
     */
    public static final char CHAR_LF = '\n';
    /**
     * {@code} carriage return CR ('\r').
     * 
     * @see <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF: Escape Sequences for
     *      Character and String Literals</a>
     * @since 2.2
     */
    public static final char CHAR_CR = '\r';

    /**
     * Field _SPACE.
     */
    public static final char _SPACE = ' ';

    /**
     * Field SPACE. (value is "" "")
     */
    public static final String SPACE = " ";

    /**
     * Field _PERIOD.
     */
    public static final char _PERIOD = '.';

    /**
     * Field PERIOD. (value is ""."")
     */
    public static final String PERIOD = ".";

    /**
     * Field _COMMA.
     */
    public static final char _COMMA = ',';

    /**
     * Field COMMA. (value is "","")
     */
    public static final String COMMA = ",";

    /**
     * Field COMMA_SPACE. (value is "", "")
     */
    public static final String COMMA_SPACE = ", ";

    /**
     * Field _COLON.
     */
    public static final char _COLON = ':';

    /**
     * Field COLON. (value is "":"")
     */
    public static final String COLON = ":";

    /** Field COLON_SPACE. */
    public static final String COLON_SPACE = ": ";

    /**
     * Field _SEMICOLON.
     */
    public static final char _SEMICOLON = ';';

    /**
     * Field SEMICOLON. (value is "";"")
     */
    public static final String SEMICOLON = ";";

    /**
     * Field SEMICOLON_SPACE. (value is ""; "")
     */
    public static final String SEMICOLON_SPACE = "; ";

    /**
     * Field _BACKSLASH.
     */
    public static final char _BACKSLASH = '\\';

    /**
     * Field BACKSLASH. (value is ""\\"")
     */
    public static final String BACKSLASH = "\\";

    /**
     * Field _QUOTATION_S.
     */
    public static final char _QUOTATION_S = '\'';

    /**
     * Field QUOTATION_S. (value is ""'"")
     */
    public static final String QUOTATION_S = "'";

    /**
     * Field SPACE_QUOTATION_S. (value is "" '"")
     */
    public static final String SPACE_QUOTATION_S = " '";

    /**
     * Field QUOTATION_S_SPACE. (value is ""' "")
     */
    public static final String QUOTATION_S_SPACE = "' ";

    /**
     * Field _QUOTATION_D.
     */
    public static final char _QUOTATION_D = '"';

    /**
     * Field QUOTATION_D. (value is ""\""")
     */
    public static final String QUOTATION_D = "\"";

    /**
     * Field SPACE_QUOTATION_D. (value is "" \""")
     */
    public static final String SPACE_QUOTATION_D = " \"";

    /**
     * Field QUOTATION_D_SPACE. (value is ""\" "")
     */
    public static final String QUOTATION_D_SPACE = "\" ";

    /**
     * Field _AMPERSAND.
     */
    public static final char _AMPERSAND = '&';

    /**
     * Field AMPERSAND. (value is ""&"")
     */
    public static final String AMPERSAND = "&";

    /**
     * Field _VERTICALBAR.
     */
    public static final char _VERTICALBAR = '|';

    /**
     * Field VERTICALBAR. (value is ""|"")
     */
    public static final String VERTICALBAR = "|";

    /**
     * Field PARALLEL. (value is ""||"")
     */
    public static final String PARALLEL = "||";

    /**
     * Field _UNDERSCORE.
     */
    public static final char _UNDERSCORE = '_';

    /**
     * Field UNDERSCORE. (value is ""_"")
     */
    public static final String UNDERSCORE = "_";

    /**
     * Field _LESS_THAN.
     */
    public static final char _LESS_THAN = '<';

    /**
     * Field LESS_THAN. (value is ""<"")
     */
    public static final String LESS_THAN = "<";

    /**
     * Field _GREATER_THAN.
     */
    public static final char _GREATER_THAN = '>';

    /**
     * Field GREATER_THAN. (value is ""."")
     */
    public static final String GREATER_THAN = ">";

    /**
     * Field _EQUAL.
     */
    public static final char _EQUAL = '=';

    /**
     * Field EQUAL. (value is ""="")
     */
    public static final String EQUAL = "=";

    /**
     * Field _PLUS.
     */
    public static final char _PLUS = '+';

    /**
     * Field PLUS. (value is ""+"")
     */
    public static final String PLUS = "+";

    /**
     * Field _MINUS.
     */
    public static final char _MINUS = '-';

    /**
     * Field MINUS. (value is ""-"")
     */
    public static final String MINUS = "-";

    /**
     * Field _PERCENT.
     */
    public static final char _PERCENT = '%';

    /**
     * Field PERCENT. (value is ""%"")
     */
    public static final String PERCENT = "%";

    /**
     * Field _SLASH.
     */
    public static final char _SLASH = '/';

    /**
     * Field SLASH. (value is ""/"")
     */
    public static final String SLASH = "/";

    /**
     * Field _ASTERISK.
     */
    public static final char _ASTERISK = '*';

    /**
     * Field ASTERISK. (value is ""*"")
     */
    public static final String ASTERISK = "*";

    /**
     * Field _QUESTION_MARK.
     */
    public static final char _QUESTION_MARK = '?';

    /**
     * Field QUESTION_MARK. (value is ""?"")
     */
    public static final String QUESTION_MARK = "?";

    /**
     * Field _PARENTHESES_L.
     */
    public static final char _PARENTHESES_L = '(';

    /**
     * Field PARENTHESES_L. (value is ""("")
     */
    public static final String PARENTHESES_L = "(";

    /**
     * Field SPACE_PARENTHESES_L. (value is "" ("")
     */
    public static final String SPACE_PARENTHESES_L = " (";

    /**
     * Field _PARENTHESES_R.
     */
    public static final char _PARENTHESES_R = ')';

    /**
     * Field PARENTHESES_R. (value is "")"")
     */
    public static final String PARENTHESES_R = ")";

    /**
     * Field PARENTHESES_R_SPACE. (value is "") "")
     */
    public static final String PARENTHESES_R_SPACE = ") ";

    /**
     * Field _BRACKET_L.
     */
    public static final char _BRACKET_L = '[';

    /**
     * Field BRACKET_L. (value is ""["")
     */
    public static final String BRACKET_L = "[";

    /**
     * Field _BRACKET_R.
     */
    public static final char _BRACKET_R = ']';

    /**
     * Field BRACKET_R. (value is ""]"")
     */
    public static final String BRACKET_R = "]";

    /**
     * Field _BRACE_L.
     */
    public static final char _BRACE_L = '{';

    /**
     * Field BRACE_L. (value is ""{"")
     */
    public static final String BRACE_L = "{";

    /**
     * Field _BRACE_R.
     */
    public static final char _BRACE_R = '}';

    /**
     * Field BRACE_R. (value is ""}"")
     */
    public static final String BRACE_R = "}";

    /**
     * Field _CIRCUMFLEX.
     */
    public static final char _CIRCUMFLEX = '^';

    /**
     * Field CIRCUMFLEX. (value is ""^"")
     */
    public static final String CIRCUMFLEX = "^";

    /**
     * Field _UNARYBIT.
     */
    public static final char _UNARYBIT = '~';

    /**
     * Field UNARYBIT. (value is ""~"")
     */
    public static final String UNARYBIT = "~";

    /**
     * Field _DOLLAR.
     */
    public static final char _DOLLAR = '$';

    /**
     * Field DOLLAR. (value is ""$"")
     */
    public static final String DOLLAR = "$";

    /**
     * Field _SHARP.
     */
    public static final char _SHARP = '#';

    /**
     * Field SHARP. (value is ""#"")
     */
    public static final String SHARP = "#";

    /**
     * Field _EXCLAMATION.
     */
    public static final char _EXCLAMATION = '!';

    /**
     * Field EXCLAMATION.
     */
    public static final String EXCLAMATION = "!";

    /**
     * Field NOT_EQUAL.
     */
    public static final String NOT_EQUAL = "<>";

    /**
     * Field NOT_EQUAL2.
     */
    public static final String NOT_EQUAL2 = "!=";

    /**
     * Field GREATER_EQUAL.
     */
    public static final String GREATER_EQUAL = ">=";

    /**
     * Field LESS_EQUAL.
     */
    public static final String LESS_EQUAL = "<=";

    // --------------------SQL key words----------------------------
    /**
     * Field WITH.
     */
    public static final String WITH = "WITH";

    /**
     * Field MERGE.
     */
    public static final String MERGE = "MERGE";

    /**
     * Field SELECT.
     */
    public static final String SELECT = "SELECT";

    /**
     * Field INSERT.
     */
    public static final String INSERT = "INSERT";

    /**
     * Field INTO.
     */
    public static final String INTO = "INTO";

    /**
     * Field UPDATE.
     */
    public static final String UPDATE = "UPDATE";

    /**
     * Field SET.
     */
    public static final String SET = "SET";

    /**
     * Field DELETE.
     */
    public static final String DELETE = "DELETE";

    /**
     * Field CREATE.
     */
    public static final String CREATE = "CREATE";

    /**
     * Field DROP.
     */
    public static final String DROP = "DROP";

    /**
     * Field SHOW.
     */
    public static final String SHOW = "SHOW";

    /**
     * Field DESCRIBE.
     */
    public static final String DESCRIBE = "DESCRIBE";

    /**
     * Field ALTER.
     */
    public static final String ALTER = "ALTER";

    /**
     * Field USE.
     */
    public static final String USE = "USE";

    /**
     * Field RENAME.
     */
    public static final String RENAME = "RENAME";

    /**
     * Field BEGIN_TRANSACTION.
     */
    public static final String BEGIN_TRANSACTION = "BEGIN TRANSACTION";

    /**
     * Field START_TRANSACTION.
     */
    public static final String START_TRANSACTION = "START TRANSACTION";

    /**
     * Field COMMIT.
     */
    public static final String COMMIT = "COMMIT";

    /**
     * Field ROLLBACK.
     */
    public static final String ROLLBACK = "ROLLBACK";

    /**
     * Field AS.
     */
    public static final String AS = "AS";

    /**
     * Field JOIN.
     */
    public static final String JOIN = "JOIN";

    /**
     * Field NATURAL.
     */
    public static final String NATURAL = "NATURAL";

    /**
     * Field INNER.
     */
    public static final String INNER = "INNER";

    /**
     * Field OUTER .
     */
    public static final String OUTER = "OUTER ";

    /**
     * Field LEFT_JOIN.
     */
    public static final String LEFT_JOIN = "LEFT JOIN";

    /**
     * Field LEFT.
     */
    public static final String LEFT = "LEFT";

    /**
     * Field RIGHT_JOIN.
     */
    public static final String RIGHT_JOIN = "RIGHT JOIN";

    /**
     * Field RIGHT.
     */
    public static final String RIGHT = "RIGHT";

    /**
     * Field FULL_JOIN.
     */
    public static final String FULL_JOIN = "FULL JOIN";

    /**
     * Field FULL.
     */
    public static final String FULL = "FULL";

    /**
     * Field CROSS_JOIN.
     */
    public static final String CROSS_JOIN = "CROSS JOIN";

    /**
     * Field INNER_JOIN.
     */
    public static final String INNER_JOIN = "INNER JOIN";

    /**
     * Field NATURAL_JOIN.
     */
    public static final String NATURAL_JOIN = "NATURAL JOIN";

    /**
     * Field CROSS.
     */
    public static final String CROSS = "CROSS";

    /**
     * Field ON.
     */
    public static final String ON = "ON";

    /**
     * Field USING.
     */
    public static final String USING = "USING";

    /**
     * Field WHERE.
     */
    public static final String WHERE = "WHERE";

    /**
     * Field GROUP_BY.
     */
    public static final String GROUP_BY = "GROUP BY";

    /**
     * Field HAVING.
     */
    public static final String HAVING = "HAVING";

    /**
     * Field ORDER_BY.
     */
    public static final String ORDER_BY = "ORDER BY";

    /**
     * Field LIMIT.
     */
    public static final String LIMIT = "LIMIT";

    /**
     * Field OFFSET.
     */
    public static final String OFFSET = "OFFSET";

    /**
     * Field FOR_UPDATE.
     */
    public static final String FOR_UPDATE = "FOR UPDATE";

    /**
     * Field FETCH_FIRST. (value is ""FETCH FIRST"")
     */
    public static final String FETCH_FIRST = "FETCH FIRST";

    /**
     * Field FETCH_NEXT. (value is ""FETCH NEXT"")
     */
    public static final String FETCH_NEXT = "FETCH NEXT";

    /**
     * Field ROWS. (value is ""ROWS"")
     */
    public static final String ROWS = "ROWS";

    /**
     * Field ROWS_ONLY. (value is ""ROWS ONLY"")
     */
    public static final String ROWS_ONLY = "ROWS ONLY";

    /**
     * Field ROW_NEXT. (value is ""ROW_NEXT"")
     */
    public static final String ROW_NEXT = "ROW_NEXT";

    /**
     * Field ROWNUM.
     */
    public static final String ROWNUM = "ROWNUM";

    /**
     * Field EXISTS.
     */
    public static final String EXISTS = "EXISTS";

    /**
     * Field LIKE.
     */
    public static final String LIKE = "LIKE";

    /**
     * Field AND.
     */
    public static final String AND = "AND";

    /**
     * Field AND2.
     */
    public static final String AND_OP = "&&";

    /**
     * Field OR.
     */
    public static final String OR = "OR";

    /**
     * Field OR2.
     */
    public static final String OR_OP = "||";

    /**
     * Field XOR.
     */
    public static final String XOR = "XOR";

    /**
     * Field NOT.
     */
    public static final String NOT = "NOT";

    /**
     * Field BETWEEN.
     */
    public static final String BETWEEN = "BETWEEN";

    /**
     * Field IS.
     */
    public static final String IS = "IS";

    /**
     * Field IS_NOT.
     */
    public static final String IS_NOT = "IS NOT";

    /**
     * Field NULL.
     */
    public static final String NULL = "NULL";

    /**
     * Field IS_NULL.
     */
    public static final String IS_NULL = "IS NULL";

    /**
     * Field IS_NOT_NULL.
     */
    public static final String IS_NOT_NULL = "IS NOT NULL";

    /**
     * Field EMPTY.
     */
    public static final String EMPTY = "EMPTY";

    /**
     * Field EMPTY.
     */
    public static final String IS_EMPTY = "IS EMPTY";

    /**
     * Field IS_NOT_EMPTY.
     */
    public static final String IS_NOT_EMPTY = "IS NOT EMPTY";

    /**
     * Field BLANK.
     */
    public static final String BLANK = "BLANK";

    /**
     * Field IS_BLANK.
     */
    public static final String IS_BLANK = "IS BLANK";

    /**
     * Field IS_NOT_BLANK.
     */
    public static final String IS_NOT_BLANK = "IS NOT BLANK";

    /**
     * Field NOT_IN.
     */
    public static final String NOT_IN = "NOT IN";

    /**
     * Field NOT_EXISTS.
     */
    public static final String NOT_EXISTS = "NOT EXISTS";

    /**
     * Field FROM.
     */
    public static final String FROM = "FROM";

    /**
     * Field ASC.
     */
    public static final String ASC = "ASC";

    /**
     * Field DESC.
     */
    public static final String DESC = "DESC";

    /**
     * Field VALUES.
     */
    public static final String VALUES = "VALUES";

    /**
     * Field DISTINCT.
     */
    public static final String DISTINCT = "DISTINCT";

    /**
     * Field DISTINCTROW.
     */
    public static final String DISTINCTROW = "DISTINCTROW";

    /**
     * Field UNIQUE.
     */
    public static final String UNIQUE = "UNIQUE";

    /**
     * Field TOP.
     */
    public static final String TOP = "TOP";

    /**
     * Field IN.
     */
    public static final String IN = "IN";

    /**
     * Field ANY.
     */
    public static final String ANY = "ANY";

    /**
     * Field ALL.
     */
    public static final String ALL = "ALL";

    /**
     * Field SOME.
     */
    public static final String SOME = "SOME";

    /**
     * Field UNION.
     */
    public static final String UNION = "UNION";

    /**
     * Field UNION_ALL.
     */
    public static final String UNION_ALL = "UNION ALL";

    /**
     * Field INTERSECT.
     */
    public static final String INTERSECT = "INTERSECT";

    /**
     * Field EXCEPT.
     */
    public static final String EXCEPT = "EXCEPT";

    /**
     * Field EXCEPT2.
     */
    public static final String EXCEPT2 = "MINUS";

    /**
     * Field AVG. (value is ""AVG"")
     */
    public static final String AVG = "AVG";

    /**
     * Field COUNT. (value is ""COUNT"")
     */
    public static final String COUNT = "COUNT";

    /**
     * Field SUM. (value is ""SUM"")
     */
    public static final String SUM = "SUM";

    /**
     * Field MIN. (value is ""MIN"")
     */
    public static final String MIN = "MIN";

    /**
     * Field MAX. (value is ""MAX"")
     */
    public static final String MAX = "MAX";

    /**
     * Field ABS. (value is ""ABS"")
     */
    public static final String ABS = "ABS";

    /**
     * Field ACOS. (value is ""ACOS"")
     */
    public static final String ACOS = "ACOS";

    /**
     * Field ASIN. (value is ""ASIN"")
     */
    public static final String ASIN = "ASIN";

    /**
     * Field ATAN. (value is ""ATAN"")
     */
    public static final String ATAN = "ATAN";

    /**
     * Field ATAN2. (value is ""ATAN2"")
     */
    public static final String ATAN2 = "ATAN2";

    /**
     * Field CEIL. (value is ""CEIL"")
     */
    public static final String CEIL = "CEIL";

    /**
     * Field COS. (value is ""COS"")
     */
    public static final String COS = "COS";

    /**
     * Field EXP. (value is ""EXP"")
     */
    public static final String EXP = "EXP";

    /**
     * Field FLOOR. (value is ""FLOOR"")
     */
    public static final String FLOOR = "FLOOR";

    /**
     * Field LOG. (value is ""LOG"")
     */
    public static final String LOG = "LOG";

    /**
     * Field LN. (value is ""LN"")
     */
    public static final String LN = "LN";

    /**
     * Field MOD. (value is ""MOD"")
     */
    public static final String MOD = "MOD";

    /**
     * Field POWER. (value is ""POWER"")
     */
    public static final String POWER = "POWER";

    /**
     * Field SIGN. (value is ""SIGN"")
     */
    public static final String SIGN = "SIGN";

    /**
     * Field SIN. (value is ""SIN"")
     */
    public static final String SIN = "SIN";

    /**
     * Field SQRT. (value is ""SQRT"")
     */
    public static final String SQRT = "SQRT";

    /**
     * Field TAN. (value is ""TAN"")
     */
    public static final String TAN = "TAN";

    /**
     * Field LENGTH. (value is ""LENGTH"")
     */
    public static final String LENGTH = "LENGTH";

    /**
     * Field CONCAT. (value is ""CONCAT"")
     */
    public static final String CONCAT = "CONCAT";

    /**
     * Field TRIM. (value is ""TRIM"")
     */
    public static final String TRIM = "TRIM";

    /**
     * Field LTRIM. (value is ""TRIM"")
     */
    public static final String LTRIM = "LTRIM";

    /**
     * Field RTRIM. (value is ""RTRIM"")
     */
    public static final String RTRIM = "RTRIM";

    /**
     * Field LPAD. (value is ""LPAD"")
     */
    public static final String LPAD = "LPAD";

    /**
     * Field RPAD. (value is ""RPAD"")
     */
    public static final String RPAD = "RPAD";

    /**
     * Field REPLACE. (value is ""REPLACE"")
     */
    public static final String REPLACE = "REPLACE";

    /**
     * Field SUBSTR. (value is ""SUBSTR"")
     */
    public static final String SUBSTR = "SUBSTR";

    /**
     * Field UPPER. (value is ""upper"")
     */
    public static final String UPPER = "UPPER";

    /**
     * Field LOWER. (value is ""lower"")
     */
    public static final String LOWER = "LOWER";

    /**
     * Field CAST. (value is ""CAST"")
     */
    public static final String CAST = "CAST";

    /**
     * Field CURRENT_TIME. (value is ""CURRENT_TIME"")
     */
    public static final String CURRENT_TIME = "CURRENT_TIME";

    /**
     * Field CURRENT_DATE. (value is ""CURRENT_DATE"")
     */
    public static final String CURRENT_DATE = "CURRENT_DATE";

    /**
     * Field CURRENT_TIMESTAMP. (value is ""CURRENT_TIMESTAMP"")
     */
    public static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

    // --------------------SQL key words----------------------------
}
