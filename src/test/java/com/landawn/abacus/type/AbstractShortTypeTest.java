package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractShortTypeTest extends TestBase {

    private Type<Number> shortType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        shortType = createType("Short");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testStringOf() {
        assertNull(shortType.stringOf(null));
        assertEquals("123", shortType.stringOf((short) 123));
        assertEquals("-456", shortType.stringOf((short) -456));
        assertEquals("0", shortType.stringOf((short) 0));
        assertEquals("32767", shortType.stringOf(Short.MAX_VALUE));
        assertEquals("-32768", shortType.stringOf(Short.MIN_VALUE));
    }

    @Test
    public void testValueOfString() {
        assertEquals(null, shortType.valueOf(""));
        assertEquals(null, shortType.valueOf((String) null));
        assertEquals((short) 123, shortType.valueOf("123"));
        assertEquals((short) -456, shortType.valueOf("-456"));
        assertEquals((short) 789, shortType.valueOf("789L"));
        assertEquals((short) 100, shortType.valueOf("100l"));
        assertEquals((short) 200, shortType.valueOf("200F"));
        assertEquals((short) 300, shortType.valueOf("300f"));
        assertEquals((short) 400, shortType.valueOf("400D"));
        assertEquals((short) 500, shortType.valueOf("500d"));

        assertThrows(NumberFormatException.class, () -> shortType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> shortType.valueOf("12.34"));
    }

    @Test
    public void testValueOfCharArray() {
        assertEquals(null, shortType.valueOf(null, 0, 0));
        assertEquals(null, shortType.valueOf(new char[0], 0, 0));

        char[] chars = "12345".toCharArray();
        assertEquals((short) 12345, shortType.valueOf(chars, 0, 5));
        assertEquals((short) 234, shortType.valueOf(chars, 1, 3));
        assertEquals((short) 5, shortType.valueOf(chars, 4, 1));

        char[] negChars = "-789".toCharArray();
        assertEquals((short) -789, shortType.valueOf(negChars, 0, 4));

        // T4-02: out-of-range is an ArithmeticException on the char[] path too (unified overflow policy)
        char[] outOfRange = "40000".toCharArray();
        assertThrows(ArithmeticException.class, () -> shortType.valueOf(outOfRange, 0, 5));

        char[] negOutOfRange = "-40000".toCharArray();
        assertThrows(ArithmeticException.class, () -> shortType.valueOf(negOutOfRange, 0, 6));
    }

    // T4-02 / T4-01: the char[] path now reports overflow as ArithmeticException like the String path, keeps NFE for
    // malformed text, and accepts a radix prefix exactly as Numbers.toShort does.
    @Test
    public void reviewFixes20260906_charArray_overflowIsArithmetic_malformedIsNfe_onBothPaths() {
        for (String s : new String[] { "32768", "-32769", "40000", "2147483648", "0x8000", "0xFFFFFFFF" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(ArithmeticException.class, () -> shortType.valueOf(cbuf, 0, cbuf.length), s);
            assertThrows(ArithmeticException.class, () -> shortType.valueOf(s), s);
        }

        for (String s : new String[] { "12x", "L", " 1", "0x", "0x1G", "#" }) {
            char[] cbuf = s.toCharArray();
            assertThrows(NumberFormatException.class, () -> shortType.valueOf(cbuf, 0, cbuf.length), s);
            assertThrows(NumberFormatException.class, () -> shortType.valueOf(s), s);
        }

        assertEquals((short) 32767, shortType.valueOf("32767".toCharArray(), 0, 5));
        assertEquals((short) -32768, shortType.valueOf("-32768".toCharArray(), 0, 6));
        assertEquals((short) 1, shortType.valueOf("1L".toCharArray(), 0, 2));
    }

    @Test
    public void reviewFixes20260906_charArray_radixPrefix_matchesStringPath() {
        for (String s : new String[] { "0x1F", "#1F", "-0x1F", "+0X1f", "0x7FFF", "-0x8000" }) {
            char[] cbuf = s.toCharArray();
            assertEquals(shortType.valueOf(s), shortType.valueOf(cbuf, 0, cbuf.length), s);
        }

        assertEquals((short) 31, shortType.valueOf("0x1F".toCharArray(), 0, 4));
        assertEquals((short) 31, shortType.valueOf("#1F".toCharArray(), 0, 3));
        assertEquals((short) -31, shortType.valueOf("-0x1F".toCharArray(), 0, 5));
        assertEquals((short) 32767, shortType.valueOf("0x7FFF".toCharArray(), 0, 6));
        assertEquals((short) -32768, shortType.valueOf("-0x8000".toCharArray(), 0, 7));
        assertEquals((short) 31, shortType.valueOf("xx0x1Fyy".toCharArray(), 2, 4));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getShort(1)).thenReturn((short) 123);
        when(rs.getShort(2)).thenReturn((short) -456);

        assertEquals((short) 123, shortType.get(rs, 1));
        assertEquals((short) -456, shortType.get(rs, 2));

        verify(rs).getShort(1);
        verify(rs).getShort(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getShort("id")).thenReturn((short) 789);
        when(rs.getShort("count")).thenReturn((short) -100);

        assertEquals((short) 789, shortType.get(rs, "id"));
        assertEquals((short) -100, shortType.get(rs, "count"));

        verify(rs).getShort("id");
        verify(rs).getShort("count");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        shortType.set(stmt, 1, null);
        verify(stmt).setNull(1, Types.SMALLINT);

        shortType.set(stmt, 2, (short) 123);
        verify(stmt).setShort(2, (short) 123);

        shortType.set(stmt, 3, (short) -456);
        verify(stmt).setShort(3, (short) -456);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        shortType.set(stmt, "param1", null);
        verify(stmt).setNull("param1", Types.SMALLINT);

        shortType.set(stmt, "param2", (short) 789);
        verify(stmt).setShort("param2", (short) 789);

        shortType.set(stmt, "param3", (short) -100);
        verify(stmt).setShort("param3", (short) -100);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        shortType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        shortType.appendTo(sb, (short) 123);
        assertEquals("123", sb.toString());

        sb.setLength(0);
        shortType.appendTo(sb, (short) -456);
        assertEquals("-456", sb.toString());
    }

    @Test
    public void testSerializeTo() throws IOException {
        shortType.serializeTo(writer, null, null);
        verify(writer).write(any(char[].class));

        shortType.serializeTo(writer, (short) 123, null);
        verify(writer).write((short) 123);

        when(config.isWriteNullNumberAsZero()).thenReturn(true);
        shortType.serializeTo(writer, null, config);
        verify(writer).write((short) 0);
    }

    // Bug: appendTo previously called x.toString() for any Number,
    // diverging from stringOf which truncates to short via shortValue().
    @Test
    public void testAppendTo_TruncatesNonShortNumberToShortRange() throws IOException {
        StringBuilder sb = new StringBuilder();
        // 70000 as a Long would render "70000" via toString(), but this is a Short type;
        // 70000 truncated to short is 70000 - 65536 = 4464.
        final Long large = Long.valueOf(70000L);
        shortType.appendTo(sb, large);
        assertEquals(shortType.stringOf(large), sb.toString());
        assertEquals("4464", sb.toString());
    }
}
