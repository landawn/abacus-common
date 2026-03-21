package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WDTest extends TestBase {

    @Test
    public void testCharZero() {
        assertEquals((char) 0, SK.CHAR_ZERO);
    }

    @Test
    public void testCharLF() {
        assertEquals('\n', SK.CHAR_LF);
    }

    @Test
    public void testCharCR() {
        assertEquals('\r', SK.CHAR_CR);
    }

    @Test
    public void testSpace() {
        assertEquals(' ', SK._SPACE);
        assertEquals(" ", SK.SPACE);
    }

    @Test
    public void testPeriod() {
        assertEquals('.', SK._PERIOD);
        assertEquals(".", SK.PERIOD);
    }

    @Test
    public void testComma() {
        assertEquals(',', SK._COMMA);
        assertEquals(",", SK.COMMA);
        assertEquals(", ", SK.COMMA_SPACE);
    }

    @Test
    public void testColon() {
        assertEquals(':', SK._COLON);
        assertEquals(":", SK.COLON);
        assertEquals(": ", SK.COLON_SPACE);
    }

    @Test
    public void testSemicolon() {
        assertEquals(';', SK._SEMICOLON);
        assertEquals(";", SK.SEMICOLON);
        assertEquals("; ", SK.SEMICOLON_SPACE);
    }

    @Test
    public void testBackslash() {
        assertEquals('\\', SK._BACKSLASH);
        assertEquals("\\", SK.BACKSLASH);
    }

    @Test
    public void testQuotations() {
        assertEquals('\'', SK._SINGLE_QUOTE);
        assertEquals("'", SK.SINGLE_QUOTE);
        assertEquals('"', SK._DOUBLE_QUOTE);
        assertEquals("\"", SK.DOUBLE_QUOTE);
    }

    @Test
    public void testOperators() {
        assertEquals('&', SK._AMPERSAND);
        assertEquals("&", SK.AMPERSAND);
        assertEquals('|', SK._VERTICAL_BAR);
        assertEquals("|", SK.VERTICAL_BAR);
    }

    @Test
    public void testUnderscore() {
        assertEquals('_', SK._UNDERSCORE);
        assertEquals("_", SK.UNDERSCORE);
    }

    @Test
    public void testComparisons() {
        assertEquals('<', SK._LESS_THAN);
        assertEquals("<", SK.LESS_THAN);
        assertEquals('>', SK._GREATER_THAN);
        assertEquals(">", SK.GREATER_THAN);
        assertEquals('=', SK._EQUAL);
        assertEquals("=", SK.EQUAL);
    }

    @Test
    public void testArithmetic() {
        assertEquals('+', SK._PLUS);
        assertEquals("+", SK.PLUS);
        assertEquals('-', SK._MINUS);
        assertEquals("-", SK.MINUS);
        assertEquals('*', SK._ASTERISK);
        assertEquals("*", SK.ASTERISK);
        assertEquals('/', SK._SLASH);
        assertEquals("/", SK.SLASH);
        assertEquals('%', SK._PERCENT);
        assertEquals("%", SK.PERCENT);
    }

    @Test
    public void testBrackets() {
        assertEquals('(', SK._PARENTHESIS_L);
        assertEquals("(", SK.PARENTHESIS_L);
        assertEquals(')', SK._PARENTHESIS_R);
        assertEquals(")", SK.PARENTHESIS_R);
        assertEquals('[', SK._BRACKET_L);
        assertEquals("[", SK.BRACKET_L);
        assertEquals(']', SK._BRACKET_R);
        assertEquals("]", SK.BRACKET_R);
        assertEquals('{', SK._BRACE_L);
        assertEquals("{", SK.BRACE_L);
        assertEquals('}', SK._BRACE_R);
        assertEquals("}", SK.BRACE_R);
    }

    @Test
    public void testSQL_SELECT() {
        assertEquals("SELECT", SK.SELECT);
        assertNotNull(SK.SELECT);
    }

    @Test
    public void testSQL_INSERT() {
        assertEquals("INSERT", SK.INSERT);
        assertEquals("INTO", SK.INTO);
    }

    @Test
    public void testSQL_UPDATE() {
        assertEquals("UPDATE", SK.UPDATE);
        assertEquals("SET", SK.SET);
    }

    @Test
    public void testSQL_DELETE() {
        assertEquals("DELETE", SK.DELETE);
    }

    @Test
    public void testSQL_FROM_WHERE() {
        assertEquals("FROM", SK.FROM);
        assertEquals("WHERE", SK.WHERE);
    }

    @Test
    public void testSQL_Joins() {
        assertEquals("JOIN", SK.JOIN);
        assertEquals("LEFT JOIN", SK.LEFT_JOIN);
        assertEquals("RIGHT JOIN", SK.RIGHT_JOIN);
        assertEquals("INNER JOIN", SK.INNER_JOIN);
        assertEquals("FULL JOIN", SK.FULL_JOIN);
        assertEquals("CROSS JOIN", SK.CROSS_JOIN);
    }

    @Test
    public void testSQL_Clauses() {
        assertEquals("GROUP BY", SK.GROUP_BY);
        assertEquals("ORDER BY", SK.ORDER_BY);
        assertEquals("HAVING", SK.HAVING);
        assertEquals("LIMIT", SK.LIMIT);
        assertEquals("OFFSET", SK.OFFSET);
    }

    @Test
    public void testSQL_Logical() {
        assertEquals("AND", SK.AND);
        assertEquals("OR", SK.OR);
        assertEquals("NOT", SK.NOT);
    }

    @Test
    public void testSQL_Comparisons() {
        assertEquals("!=", SK.NOT_EQUAL);
        assertEquals("<>", SK.NOT_EQUAL_ANSI);
        assertEquals(">=", SK.GREATER_THAN_OR_EQUAL);
        assertEquals("<=", SK.LESS_THAN_OR_EQUAL);
    }

    @Test
    public void testSQL_NULL() {
        assertEquals("NULL", SK.NULL);
        assertEquals("IS NULL", SK.IS_NULL);
        assertEquals("IS NOT NULL", SK.IS_NOT_NULL);
    }

    @Test
    public void testSQL_Aggregate() {
        assertEquals("COUNT", SK.COUNT);
        assertEquals("SUM", SK.SUM);
        assertEquals("AVG", SK.AVG);
        assertEquals("MIN", SK.MIN);
        assertEquals("MAX", SK.MAX);
    }

    @Test
    public void testSQL_Math() {
        assertEquals("ABS", SK.ABS);
        assertEquals("CEIL", SK.CEIL);
        assertEquals("FLOOR", SK.FLOOR);
        assertEquals("SQRT", SK.SQRT);
        assertEquals("POWER", SK.POWER);
    }

    @Test
    public void testSQL_String() {
        assertEquals("LENGTH", SK.LENGTH);
        assertEquals("CONCAT", SK.CONCAT);
        assertEquals("TRIM", SK.TRIM);
        assertEquals("UPPER", SK.UPPER);
        assertEquals("LOWER", SK.LOWER);
    }

    @Test
    public void testIntegration_SQLQuery() {
        String query = SK.SELECT + SK.SPACE + "*" + SK.SPACE + SK.FROM + SK.SPACE + "users";
        assertEquals("SELECT * FROM users", query);
    }

    @Test
    public void testIntegration_CSVFormat() {
        String csv = "John" + SK.COMMA_SPACE + "Doe" + SK.COMMA_SPACE + "30";
        assertEquals("John, Doe, 30", csv);
    }
}
