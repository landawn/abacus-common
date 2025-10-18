package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class WD2025Test extends TestBase {

    @Test
    public void testCharZero() {
        assertEquals((char) 0, WD.CHAR_ZERO);
    }

    @Test
    public void testCharLF() {
        assertEquals('\n', WD.CHAR_LF);
    }

    @Test
    public void testCharCR() {
        assertEquals('\r', WD.CHAR_CR);
    }

    @Test
    public void testSpace() {
        assertEquals(' ', WD._SPACE);
        assertEquals(" ", WD.SPACE);
    }

    @Test
    public void testPeriod() {
        assertEquals('.', WD._PERIOD);
        assertEquals(".", WD.PERIOD);
    }

    @Test
    public void testComma() {
        assertEquals(',', WD._COMMA);
        assertEquals(",", WD.COMMA);
        assertEquals(", ", WD.COMMA_SPACE);
    }

    @Test
    public void testColon() {
        assertEquals(':', WD._COLON);
        assertEquals(":", WD.COLON);
        assertEquals(": ", WD.COLON_SPACE);
    }

    @Test
    public void testSemicolon() {
        assertEquals(';', WD._SEMICOLON);
        assertEquals(";", WD.SEMICOLON);
        assertEquals("; ", WD.SEMICOLON_SPACE);
    }

    @Test
    public void testBackslash() {
        assertEquals('\\', WD._BACKSLASH);
        assertEquals("\\", WD.BACKSLASH);
    }

    @Test
    public void testQuotations() {
        assertEquals('\'', WD._QUOTATION_S);
        assertEquals("'", WD.QUOTATION_S);
        assertEquals('"', WD._QUOTATION_D);
        assertEquals("\"", WD.QUOTATION_D);
    }

    @Test
    public void testOperators() {
        assertEquals('&', WD._AMPERSAND);
        assertEquals("&", WD.AMPERSAND);
        assertEquals('|', WD._VERTICALBAR);
        assertEquals("|", WD.VERTICALBAR);
    }

    @Test
    public void testUnderscore() {
        assertEquals('_', WD._UNDERSCORE);
        assertEquals("_", WD.UNDERSCORE);
    }

    @Test
    public void testComparisons() {
        assertEquals('<', WD._LESS_THAN);
        assertEquals("<", WD.LESS_THAN);
        assertEquals('>', WD._GREATER_THAN);
        assertEquals(">", WD.GREATER_THAN);
        assertEquals('=', WD._EQUAL);
        assertEquals("=", WD.EQUAL);
    }

    @Test
    public void testArithmetic() {
        assertEquals('+', WD._PLUS);
        assertEquals("+", WD.PLUS);
        assertEquals('-', WD._MINUS);
        assertEquals("-", WD.MINUS);
        assertEquals('*', WD._ASTERISK);
        assertEquals("*", WD.ASTERISK);
        assertEquals('/', WD._SLASH);
        assertEquals("/", WD.SLASH);
        assertEquals('%', WD._PERCENT);
        assertEquals("%", WD.PERCENT);
    }

    @Test
    public void testBrackets() {
        assertEquals('(', WD._PARENTHESES_L);
        assertEquals("(", WD.PARENTHESES_L);
        assertEquals(')', WD._PARENTHESES_R);
        assertEquals(")", WD.PARENTHESES_R);
        assertEquals('[', WD._BRACKET_L);
        assertEquals("[", WD.BRACKET_L);
        assertEquals(']', WD._BRACKET_R);
        assertEquals("]", WD.BRACKET_R);
        assertEquals('{', WD._BRACE_L);
        assertEquals("{", WD.BRACE_L);
        assertEquals('}', WD._BRACE_R);
        assertEquals("}", WD.BRACE_R);
    }

    @Test
    public void testSQL_SELECT() {
        assertEquals("SELECT", WD.SELECT);
        assertNotNull(WD.SELECT);
    }

    @Test
    public void testSQL_INSERT() {
        assertEquals("INSERT", WD.INSERT);
        assertEquals("INTO", WD.INTO);
    }

    @Test
    public void testSQL_UPDATE() {
        assertEquals("UPDATE", WD.UPDATE);
        assertEquals("SET", WD.SET);
    }

    @Test
    public void testSQL_DELETE() {
        assertEquals("DELETE", WD.DELETE);
    }

    @Test
    public void testSQL_FROM_WHERE() {
        assertEquals("FROM", WD.FROM);
        assertEquals("WHERE", WD.WHERE);
    }

    @Test
    public void testSQL_Joins() {
        assertEquals("JOIN", WD.JOIN);
        assertEquals("LEFT JOIN", WD.LEFT_JOIN);
        assertEquals("RIGHT JOIN", WD.RIGHT_JOIN);
        assertEquals("INNER JOIN", WD.INNER_JOIN);
        assertEquals("FULL JOIN", WD.FULL_JOIN);
        assertEquals("CROSS JOIN", WD.CROSS_JOIN);
    }

    @Test
    public void testSQL_Clauses() {
        assertEquals("GROUP BY", WD.GROUP_BY);
        assertEquals("ORDER BY", WD.ORDER_BY);
        assertEquals("HAVING", WD.HAVING);
        assertEquals("LIMIT", WD.LIMIT);
        assertEquals("OFFSET", WD.OFFSET);
    }

    @Test
    public void testSQL_Logical() {
        assertEquals("AND", WD.AND);
        assertEquals("OR", WD.OR);
        assertEquals("NOT", WD.NOT);
    }

    @Test
    public void testSQL_Comparisons() {
        assertEquals("!=", WD.NOT_EQUAL);
        assertEquals("<>", WD.NOT_EQUAL2);
        assertEquals(">=", WD.GREATER_EQUAL);
        assertEquals("<=", WD.LESS_EQUAL);
    }

    @Test
    public void testSQL_NULL() {
        assertEquals("NULL", WD.NULL);
        assertEquals("IS NULL", WD.IS_NULL);
        assertEquals("IS NOT NULL", WD.IS_NOT_NULL);
    }

    @Test
    public void testSQL_Aggregate() {
        assertEquals("COUNT", WD.COUNT);
        assertEquals("SUM", WD.SUM);
        assertEquals("AVG", WD.AVG);
        assertEquals("MIN", WD.MIN);
        assertEquals("MAX", WD.MAX);
    }

    @Test
    public void testSQL_Math() {
        assertEquals("ABS", WD.ABS);
        assertEquals("CEIL", WD.CEIL);
        assertEquals("FLOOR", WD.FLOOR);
        assertEquals("SQRT", WD.SQRT);
        assertEquals("POWER", WD.POWER);
    }

    @Test
    public void testSQL_String() {
        assertEquals("LENGTH", WD.LENGTH);
        assertEquals("CONCAT", WD.CONCAT);
        assertEquals("TRIM", WD.TRIM);
        assertEquals("UPPER", WD.UPPER);
        assertEquals("LOWER", WD.LOWER);
    }

    @Test
    public void testIntegration_SQLQuery() {
        String query = WD.SELECT + WD.SPACE + "*" + WD.SPACE + WD.FROM + WD.SPACE + "users";
        assertEquals("SELECT * FROM users", query);
    }

    @Test
    public void testIntegration_CSVFormat() {
        String csv = "John" + WD.COMMA_SPACE + "Doe" + WD.COMMA_SPACE + "30";
        assertEquals("John, Doe, 30", csv);
    }
}
