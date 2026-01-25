package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractStringType100Test extends TestBase {

    private Type<String> stringType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        stringType = createType("String");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(String.class, stringType.clazz());
    }

    @Test
    public void testIsString() {
        assertTrue(stringType.isString());
    }

    @Test
    public void testStringOf() {
        assertNull(stringType.stringOf(null));
        assertEquals("", stringType.stringOf(""));
        assertEquals("hello", stringType.stringOf("hello"));
        assertEquals("world", stringType.stringOf("world"));
        assertEquals("  spaces  ", stringType.stringOf("  spaces  "));
    }

    @Test
    public void testValueOfString() {
        assertNull(stringType.valueOf((String) null));
        assertEquals("", stringType.valueOf(""));
        assertEquals("hello", stringType.valueOf("hello"));
        assertEquals("world", stringType.valueOf("world"));
        assertEquals("  spaces  ", stringType.valueOf("  spaces  "));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(stringType.valueOf(null, 0, 0));
        assertEquals("", stringType.valueOf(new char[0], 0, 0));
        assertEquals("", stringType.valueOf(new char[] { 'a', 'b', 'c' }, 0, 0));

        char[] chars = "hello world".toCharArray();
        assertEquals("hello world", stringType.valueOf(chars, 0, 11));
        assertEquals("hello", stringType.valueOf(chars, 0, 5));
        assertEquals("world", stringType.valueOf(chars, 6, 5));
        assertEquals("lo wo", stringType.valueOf(chars, 3, 5));
    }

    @Test
    public void testValueOfObject() throws SQLException {
        assertNull(stringType.valueOf((Object) null));

        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(5L);
        when(clob.getSubString(1, 5)).thenReturn("hello");

        assertEquals("hello", stringType.valueOf(clob));
        verify(clob).getSubString(1, 5);
        verify(clob).free();

        Clob errorClob = mock(Clob.class);
        when(errorClob.length()).thenThrow(new SQLException("Read error"));

        assertThrows(UncheckedSQLException.class, () -> stringType.valueOf(errorClob));

        Clob freeErrorClob = mock(Clob.class);
        when(freeErrorClob.length()).thenReturn(3L);
        when(freeErrorClob.getSubString(1, 3)).thenReturn("foo");
        doThrow(new SQLException("Free error")).when(freeErrorClob).free();

        assertEquals("123", stringType.valueOf(123));
        assertEquals("true", stringType.valueOf(true));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("value1");
        when(rs.getString(2)).thenReturn(null);
        when(rs.getString(3)).thenReturn("");

        assertEquals("value1", stringType.get(rs, 1));
        assertNull(stringType.get(rs, 2));
        assertEquals("", stringType.get(rs, 3));

        verify(rs).getString(1);
        verify(rs).getString(2);
        verify(rs).getString(3);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn("John");
        when(rs.getString("description")).thenReturn(null);
        when(rs.getString("notes")).thenReturn("");

        assertEquals("John", stringType.get(rs, "name"));
        assertNull(stringType.get(rs, "description"));
        assertEquals("", stringType.get(rs, "notes"));

        verify(rs).getString("name");
        verify(rs).getString("description");
        verify(rs).getString("notes");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        stringType.set(stmt, 1, "hello");
        verify(stmt).setString(1, "hello");

        stringType.set(stmt, 2, null);
        verify(stmt).setString(2, null);

        stringType.set(stmt, 3, "");
        verify(stmt).setString(3, "");
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        stringType.set(stmt, "param1", "world");
        verify(stmt).setString("param1", "world");

        stringType.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);

        stringType.set(stmt, "param3", "");
        verify(stmt).setString("param3", "");
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        stringType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        stringType.appendTo(sb, "hello");
        assertEquals("hello", sb.toString());

        sb.setLength(0);
        stringType.appendTo(sb, "");
        assertEquals("", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        stringType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        stringType.writeCharacter(writer, "", null);
        verify(writer).writeCharacter("");

        stringType.writeCharacter(writer, "hello", null);
        verify(writer).writeCharacter("hello");

        when(config.writeNullStringAsEmpty()).thenReturn(true);
        when(config.getStringQuotation()).thenReturn((char) 0);
        stringType.writeCharacter(writer, null, config);
        verify(writer, times(2)).writeCharacter("");

        when(config.getStringQuotation()).thenReturn('"');
        stringType.writeCharacter(writer, "world", config);
        verify(writer, times(2)).write('"');
        verify(writer).writeCharacter("world");
    }
}
