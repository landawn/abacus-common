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
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractIntegerType100Test extends TestBase {

    private Type<Number> integerType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        integerType = createType("Integer");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testStringOf() {
        assertNull(integerType.stringOf(null));
        assertEquals("123", integerType.stringOf(123));
        assertEquals("-456", integerType.stringOf(-456));
        assertEquals("0", integerType.stringOf(0));
        assertEquals("2147483647", integerType.stringOf(Integer.MAX_VALUE));
        assertEquals("-2147483648", integerType.stringOf(Integer.MIN_VALUE));
    }

    @Test
    public void testValueOfString() {
        assertEquals(null, integerType.valueOf(""));
        assertEquals(null, integerType.valueOf((String) null));
        assertEquals(123, integerType.valueOf("123"));
        assertEquals(-456, integerType.valueOf("-456"));
        assertEquals(789, integerType.valueOf("789L"));
        assertEquals(100, integerType.valueOf("100l"));
        assertEquals(200, integerType.valueOf("200F"));
        assertEquals(300, integerType.valueOf("300f"));
        assertEquals(400, integerType.valueOf("400D"));
        assertEquals(500, integerType.valueOf("500d"));

        assertThrows(NumberFormatException.class, () -> integerType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> integerType.valueOf("12.34"));
    }

    @Test
    public void testValueOfCharArray() {
        assertEquals(null, integerType.valueOf(null, 0, 0));
        assertEquals(null, integerType.valueOf(new char[0], 0, 0));

        char[] chars = "12345".toCharArray();
        assertEquals(12345, integerType.valueOf(chars, 0, 5));
        assertEquals(234, integerType.valueOf(chars, 1, 3));
        assertEquals(5, integerType.valueOf(chars, 4, 1));

        char[] negChars = "-789".toCharArray();
        assertEquals(-789, integerType.valueOf(negChars, 0, 4));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(123);
        when(rs.getObject(2)).thenReturn(-456);

        assertEquals(123, integerType.get(rs, 1));
        assertEquals(-456, integerType.get(rs, 2));

        verify(rs).getObject(1);
        verify(rs).getObject(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(789);
        when(rs.getObject("count")).thenReturn(-100);

        assertEquals(789, integerType.get(rs, "id"));
        assertEquals(-100, integerType.get(rs, "count"));

        verify(rs).getObject("id");
        verify(rs).getObject("count");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        integerType.set(stmt, 1, null);
        verify(stmt).setNull(1, Types.INTEGER);

        integerType.set(stmt, 2, 123);
        verify(stmt).setInt(2, 123);

        integerType.set(stmt, 3, -456);
        verify(stmt).setInt(3, -456);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        integerType.set(stmt, "param1", null);
        verify(stmt).setNull("param1", Types.INTEGER);

        integerType.set(stmt, "param2", 789);
        verify(stmt).setInt("param2", 789);

        integerType.set(stmt, "param3", -100);
        verify(stmt).setInt("param3", -100);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        integerType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        integerType.appendTo(sb, 123);
        assertEquals("123", sb.toString());

        sb.setLength(0);
        integerType.appendTo(sb, -456);
        assertEquals("-456", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        // Test null value
        integerType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        // Test with value
        integerType.writeCharacter(writer, 123, null);
        verify(writer).writeInt(123);

        // Test with writeNullNumberAsZero config
        when(config.writeNullNumberAsZero()).thenReturn(true);
        integerType.writeCharacter(writer, null, config);
        verify(writer).writeInt(0);
    }
}
