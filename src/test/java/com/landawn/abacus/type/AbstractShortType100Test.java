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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractShortType100Test extends TestBase {

    private Type<Number> shortType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        shortType = createType("Short");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
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

        char[] outOfRange = "40000".toCharArray();
        assertThrows(NumberFormatException.class, () -> shortType.valueOf(outOfRange, 0, 5));

        char[] negOutOfRange = "-40000".toCharArray();
        assertThrows(NumberFormatException.class, () -> shortType.valueOf(negOutOfRange, 0, 6));
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
    public void testWriteCharacter() throws IOException {
        shortType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        shortType.writeCharacter(writer, (short) 123, null);
        verify(writer).write((short) 123);

        when(config.writeNullNumberAsZero()).thenReturn(true);
        shortType.writeCharacter(writer, null, config);
        verify(writer).write((short) 0);
    }
}
