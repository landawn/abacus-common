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

package com.landawn.abacus.condition;

import static com.landawn.abacus.util.WD.ABS;
import static com.landawn.abacus.util.WD.ACOS;
import static com.landawn.abacus.util.WD.AMPERSAND;
import static com.landawn.abacus.util.WD.ASIN;
import static com.landawn.abacus.util.WD.ASTERISK;
import static com.landawn.abacus.util.WD.ATAN;
import static com.landawn.abacus.util.WD.AVG;
import static com.landawn.abacus.util.WD.CEIL;
import static com.landawn.abacus.util.WD.CIRCUMFLEX;
import static com.landawn.abacus.util.WD.COMMA_SPACE;
import static com.landawn.abacus.util.WD.CONCAT;
import static com.landawn.abacus.util.WD.COS;
import static com.landawn.abacus.util.WD.COUNT;
import static com.landawn.abacus.util.WD.EXP;
import static com.landawn.abacus.util.WD.FLOOR;
import static com.landawn.abacus.util.WD.LENGTH;
import static com.landawn.abacus.util.WD.LN;
import static com.landawn.abacus.util.WD.LOG;
import static com.landawn.abacus.util.WD.LOWER;
import static com.landawn.abacus.util.WD.LPAD;
import static com.landawn.abacus.util.WD.LTRIM;
import static com.landawn.abacus.util.WD.MAX;
import static com.landawn.abacus.util.WD.MIN;
import static com.landawn.abacus.util.WD.MINUS;
import static com.landawn.abacus.util.WD.MOD;
import static com.landawn.abacus.util.WD.PERCENT;
import static com.landawn.abacus.util.WD.PLUS;
import static com.landawn.abacus.util.WD.POWER;
import static com.landawn.abacus.util.WD.REPLACE;
import static com.landawn.abacus.util.WD.RPAD;
import static com.landawn.abacus.util.WD.RTRIM;
import static com.landawn.abacus.util.WD.SIGN;
import static com.landawn.abacus.util.WD.SIN;
import static com.landawn.abacus.util.WD.SLASH;
import static com.landawn.abacus.util.WD.SPACE;
import static com.landawn.abacus.util.WD.SQRT;
import static com.landawn.abacus.util.WD.SUBSTR;
import static com.landawn.abacus.util.WD.SUM;
import static com.landawn.abacus.util.WD.TAN;
import static com.landawn.abacus.util.WD.TRIM;
import static com.landawn.abacus.util.WD.UPPER;
import static com.landawn.abacus.util.WD.VERTICALBAR;
import static com.landawn.abacus.util.WD._QUOTATION_S;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 * The Class Expression.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Expression extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -1270437226702741887L;

    /** The Constant NULL_STRING. */
    static final String NULL_STRING = "null".intern();

    /** The Constant NULL_CHAR_ARRAY. */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /** The Constant TRUE. */
    static final String TRUE = Boolean.TRUE.toString().intern();

    /** The Constant TRUE_CHAR_ARRAY. */
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    /** The Constant FALSE. */
    static final String FALSE = Boolean.FALSE.toString().intern();

    /** The Constant FALSE_CHAR_ARRAY. */
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    /** The Constant LEFT_SHIFT. */
    private static final String LEFT_SHIFT = "<<";

    /** The Constant RIGTH_SHIFT. */
    private static final String RIGTH_SHIFT = ">>";

    /** The Constant NULL. */
    private static final String NULL = "NULL";

    /** The Constant EMPTY. */
    private static final String EMPTY = "BLANK";

    /** The Constant cachedExpression. */
    private static final Map<String, Expression> cachedExpression = new ConcurrentHashMap<>();

    /** The literal. */
    // For Kryo
    final String literal;

    /**
     * Instantiates a new expression.
     */
    // For Kryo
    Expression() {
        literal = null;
    }

    /**
     * Instantiates a new expression.
     *
     * @param literal
     */
    public Expression(String literal) {
        super(Operator.EMPTY);

        this.literal = literal;
    }

    /**
     * Gets the literal.
     *
     * @return
     */
    public String getLiteral() {
        return literal;
    }

    /**
     *
     * @param literal
     * @return
     */
    public static Expression of(String literal) {
        Expression expr = cachedExpression.get(literal);

        if (expr == null) {
            expr = new Expression(literal);

            cachedExpression.put(literal, expr);
        }

        return expr;
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String equal(String literal, Object value) {
        return link(Operator.EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String eq(String literal, Object value) {
        return link(Operator.EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String notEqual(String literal, Object value) {
        return link(Operator.NOT_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String ne(String literal, Object value) {
        return link(Operator.NOT_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String greaterThan(String literal, Object value) {
        return link(Operator.GREATER_THAN, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String gt(String literal, Object value) {
        return link(Operator.GREATER_THAN, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String greaterEqual(String literal, Object value) {
        return link(Operator.GREATER_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String ge(String literal, Object value) {
        return link(Operator.GREATER_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String lessThan(String literal, Object value) {
        return link(Operator.LESS_THAN, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String lt(String literal, Object value) {
        return link(Operator.LESS_THAN, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String lessEqual(String literal, Object value) {
        return link(Operator.LESS_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String le(String literal, Object value) {
        return link(Operator.LESS_EQUAL, literal, value);
    }

    /**
     *
     * @param literal
     * @param min
     * @param max
     * @return
     */
    public static String between(String literal, Object min, Object max) {
        return link(Operator.BETWEEN, literal, min, max);
    }

    /**
     *
     * @param literal
     * @param min
     * @param max
     * @return
     */
    public static String bt(String literal, Object min, Object max) {
        return link(Operator.BETWEEN, literal, min, max);
    }

    /**
     *
     * @param literal
     * @param value
     * @return
     */
    public static String like(String literal, String value) {
        return link(Operator.LIKE, literal, value);
    }

    /**
     * Checks if is null.
     *
     * @param literal
     * @return
     */
    public static String isNull(String literal) {
        return link2(Operator.IS, literal, NULL);
    }

    /**
     * Checks if is not null.
     *
     * @param literal
     * @return
     */
    public static String isNotNull(String literal) {
        return link2(Operator.IS_NOT, literal, NULL);
    }

    /**
     * Checks if is empty.
     *
     * @param literal
     * @return
     */
    public static String isEmpty(String literal) {
        return link2(Operator.IS, literal, EMPTY);
    }

    /**
     * Checks if is not empty.
     *
     * @param literal
     * @return
     */
    public static String isNotEmpty(String literal) {
        return link2(Operator.IS_NOT, literal, EMPTY);
    }

    /**
     *
     * @param literals
     * @return
     */
    @SafeVarargs
    public static String and(String... literals) {
        return link2(Operator.AND, literals);
    }

    /**
     *
     * @param literals
     * @return
     */
    @SafeVarargs
    public static String or(String... literals) {
        return link2(Operator.OR, literals);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String plus(Object... objects) {
        return link(PLUS, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String minus(Object... objects) {
        return link(MINUS, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String multi(Object... objects) {
        return link(ASTERISK, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String division(Object... objects) {
        return link(SLASH, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String modulus(Object... objects) {
        return link(PERCENT, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String lShift(Object... objects) {
        return link(LEFT_SHIFT, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String rShift(Object... objects) {
        return link(RIGTH_SHIFT, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String bitwiseAnd(Object... objects) {
        return link(AMPERSAND, objects);
    }

    /**
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String bitwiseOr(Object... objects) {
        return link(VERTICALBAR, objects);
    }

    /**
     * Bitwise X or.
     *
     * @param objects
     * @return
     */
    @SafeVarargs
    public static String bitwiseXOr(Object... objects) {
        return link(CIRCUMFLEX, objects);
    }

    /**
     *
     * @param operator
     * @param literal
     * @param value
     * @return
     */
    static String link(Operator operator, String literal, Object value) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(formalize(value));

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param operator
     * @param literal
     * @param min
     * @param max
     * @return
     */
    static String link(Operator operator, String literal, Object min, Object max) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(WD._PARENTHESES_L);
            sb.append(formalize(min));
            sb.append(WD.COMMA_SPACE);
            sb.append(formalize(max));
            sb.append(WD._PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param operator
     * @param literal
     * @param operatorPostfix
     * @return
     */
    static String link2(Operator operator, String literal, String operatorPostfix) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(literal);
            sb.append(WD._SPACE);
            sb.append(operator.getName());
            sb.append(WD._SPACE);
            sb.append(operatorPostfix);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param operator
     * @param literals
     * @return
     */
    static String link2(Operator operator, String... literals) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < literals.length; i++) {
                if (i > 0) {
                    sb.append(WD._SPACE);
                    sb.append(operator.getName());
                    sb.append(WD._SPACE);
                }

                sb.append(literals[i]);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param linkedSymbol
     * @param objects
     * @return
     */
    static String link(String linkedSymbol, Object... objects) {
        if (!(SPACE.equals(linkedSymbol) || COMMA_SPACE.equals(linkedSymbol))) {
            linkedSymbol = WD._SPACE + linkedSymbol + WD._SPACE;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            for (int i = 0; i < objects.length; i++) {
                if (i > 0) {
                    sb.append(linkedSymbol);
                }

                sb.append(formalize(objects[i]));
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param value
     * @return
     */
    public static String formalize(Object value) {
        if (value == null) {
            return NULL_STRING;
        }

        if (value instanceof String) {
            return (_QUOTATION_S + StringUtil.quoteEscaped((String) value) + _QUOTATION_S);
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Expression) {
            return ((Expression) value).getLiteral();
        } else {
            return (_QUOTATION_S + StringUtil.quoteEscaped(N.stringOf(value)) + _QUOTATION_S);
        }
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String count(String expression) {
        return function(COUNT, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String average(String expression) {
        return function(AVG, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String sum(String expression) {
        return function(SUM, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String min(String expression) {
        return function(MIN, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String max(String expression) {
        return function(MAX, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String abs(String expression) {
        return function(ABS, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String acos(String expression) {
        return function(ACOS, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String asin(String expression) {
        return function(ASIN, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String atan(String expression) {
        return function(ATAN, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String ceil(String expression) {
        return function(CEIL, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String cos(String expression) {
        return function(COS, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String exp(String expression) {
        return function(EXP, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String floor(String expression) {
        return function(FLOOR, expression);
    }

    /**
     *
     * @param b
     * @param x
     * @return
     */
    public static String log(String b, String x) {
        return function(LOG, b, x);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String ln(String expression) {
        return function(LN, expression);
    }

    /**
     *
     * @param n1
     * @param n2
     * @return
     */
    public static String mod(String n1, String n2) {
        return function(MOD, n1, n2);
    }

    /**
     *
     * @param n1
     * @param n2
     * @return
     */
    public static String power(String n1, String n2) {
        return function(POWER, n1, n2);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String sign(String expression) {
        return function(SIGN, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String sin(String expression) {
        return function(SIN, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String sqrt(String expression) {
        return function(SQRT, expression);
    }

    /**
     *
     * @param expression
     * @return
     */
    public static String tan(String expression) {
        return function(TAN, expression);
    }

    /**
     *
     * @param st1
     * @param st2
     * @return
     */
    public static String concat(String st1, String st2) {
        return function(CONCAT, st1, st2);
    }

    /**
     *
     * @param st
     * @param oldString
     * @param replacement
     * @return
     */
    public static String replace(String st, String oldString, String replacement) {
        return function(REPLACE, st, oldString, replacement);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String stringLength(String st) {
        return function(LENGTH, st);
    }

    /**
     *
     * @param st
     * @param fromIndex
     * @return
     */
    public static String subString(String st, int fromIndex) {
        return function(SUBSTR, st, fromIndex);
    }

    /**
     *
     * @param st
     * @param fromIndex
     * @param length
     * @return
     */
    public static String subString(String st, int fromIndex, int length) {
        return function(SUBSTR, st, fromIndex, length);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String trim(String st) {
        return function(TRIM, st);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String lTrim(String st) {
        return function(LTRIM, st);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String rTrim(String st) {
        return function(RTRIM, st);
    }

    /**
     *
     * @param st
     * @param length
     * @param padStr
     * @return
     */
    public static String lPad(String st, int length, String padStr) {
        return function(LPAD, st, length, padStr);
    }

    /**
     *
     * @param st
     * @param length
     * @param padStr
     * @return
     */
    public static String rPad(String st, int length, String padStr) {
        return function(RPAD, st, length, padStr);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String lower(String st) {
        return function(LOWER, st);
    }

    /**
     *
     * @param st
     * @return
     */
    public static String upper(String st) {
        return function(UPPER, st);
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        // TODO Auto-generated method stub
    }

    /**
     *
     * @param functionName
     * @param args
     * @return
     */
    private static String function(String functionName, Object... args) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(functionName);

            sb.append(WD._PARENTHESES_L);

            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(N.stringOf(args[i]));
            }

            sb.append(WD._PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        return literal;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (literal == null) ? 0 : literal.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {

        return (this == obj) || ((obj != null) && (obj instanceof Expression) && (N.equals(literal, ((Expression) obj).literal)));
    }

    /**
     * The Class Expr.
     */
    public static class Expr extends Expression {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 3865184718953833862L;

        /**
         * Instantiates a new expr.
         *
         * @param literal
         */
        public Expr(String literal) {
            super(literal);
        }
    }
}
