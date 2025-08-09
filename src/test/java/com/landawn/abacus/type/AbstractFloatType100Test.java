package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.landawn.abacus.util.CharacterWriter;

public class AbstractFloatType100Test extends TestBase {

    private AbstractFloatType floatType;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        floatType = createType("Float");
        writer = createCharacterWriter();
    }

    @Test
    public void testStringOf() {
        // Test null value
        assertNull(floatType.stringOf(null));

        // Test float value
        Float floatValue = 3.14f;
        assertEquals("3.14", floatType.stringOf(floatValue));

        // Test various Number types
        assertEquals("3.14", floatType.stringOf(Double.valueOf(3.14)));
        assertEquals("42", floatType.stringOf(Integer.valueOf(42)));
        assertEquals("100", floatType.stringOf(Long.valueOf(100L)));
    }

    @Test
    public void testValueOf() {
        // Test null/empty string
        assertNull(floatType.valueOf(null));
        assertNull(floatType.valueOf(""));
        assertThrows(NumberFormatException.class, () -> assertNull(floatType.valueOf("  ")));

        // Test valid float strings
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14"));
        assertEquals(Float.valueOf(-10.5f), floatType.valueOf("-10.5"));
        assertEquals(Float.valueOf(0.0f), floatType.valueOf("0.0"));

        // Test strings with suffixes
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14f"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14F"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14d"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14D"));
        assertEquals(Float.valueOf(100f), floatType.valueOf("100l"));
        assertEquals(Float.valueOf(100f), floatType.valueOf("100L"));

        // Test invalid strings
        assertThrows(NumberFormatException.class, () -> floatType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> floatType.valueOf("12.34.56"));
        assertThrows(NumberFormatException.class, () -> floatType.valueOf("1a"));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(3.14f);
        when(rs.getObject(2)).thenReturn(0.0f);
        when(rs.wasNull()).thenReturn(false);

        assertEquals(Float.valueOf(3.14f), floatType.get(rs, 1));
        assertEquals(Float.valueOf(0.0f), floatType.get(rs, 2));

        verify(rs).getObject(1);
        verify(rs).getObject(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("price")).thenReturn(19.99f);
        when(rs.getObject("discount")).thenReturn(0.15f);

        assertEquals(Float.valueOf(19.99f), floatType.get(rs, "price"));
        assertEquals(Float.valueOf(0.15f), floatType.get(rs, "discount"));

        verify(rs).getObject("price");
        verify(rs).getObject("discount");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test null value
        floatType.set(stmt, 1, null);
        verify(stmt).setNull(1, Types.FLOAT);

        // Test float value
        floatType.set(stmt, 2, 3.14f);
        verify(stmt).setFloat(2, 3.14f);

        // Test other Number types
        floatType.set(stmt, 3, Float.valueOf(2.718f));
        verify(stmt).setFloat(3, 2.718f);

        floatType.set(stmt, 4, 42f);
        verify(stmt).setFloat(4, 42.0f);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test null value
        floatType.set(stmt, "param1", null);
        verify(stmt).setNull("param1", Types.FLOAT);

        // Test float value
        floatType.set(stmt, "param2", 3.14f);
        verify(stmt).setFloat("param2", 3.14f);

        // Test other Number types
        floatType.set(stmt, "param3", Double.valueOf(2.718));
        verify(stmt).setFloat("param3", 2.718f);

        floatType.set(stmt, "param4", Integer.valueOf(42));
        verify(stmt).setFloat("param4", 42.0f);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        // Test null value
        floatType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        // Test float value
        sb.setLength(0);
        floatType.appendTo(sb, 3.14f);
        assertEquals("3.14", sb.toString());

        // Test other Number types
        sb.setLength(0);
        floatType.appendTo(sb, Double.valueOf(2.718));
        assertEquals("2.718", sb.toString());

        // Test IOException handling
        Appendable errorAppendable = mock(Appendable.class);
        when(errorAppendable.append(anyString())).thenThrow(new IOException("Test exception"));
        assertThrows(IOException.class, () -> floatType.appendTo(errorAppendable, null));
    }

    //    @Test
    //    public void testWriteCharacter() throws IOException {
    //        // Test null value without config
    //        floatType.writeCharacter(writer, null, null);
    //        verify(writer).write("null".toCharArray());
    //
    //        // Test null value with writeNullNumberAsZero config
    //        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
    //        when(config.writeNullNumberAsZero()).thenReturn(true);
    //        reset(writer);
    //        floatType.writeCharacter(writer, null, config);
    //        verify(writer).write(0.0f);
    //
    //        // Test float value
    //        reset(writer);
    //        floatType.writeCharacter(writer, 3.14f, null);
    //        verify(writer).write(3.14f);
    //
    //        // Test other Number types
    //        reset(writer);
    //        floatType.writeCharacter(writer, Double.valueOf(2.718), null);
    //        verify(writer).write(2.718f);
    //
    //        reset(writer);
    //        floatType.writeCharacter(writer, Integer.valueOf(42), null);
    //        verify(writer).write(42.0f);
    //
    //        // Test with config but writeNullNumberAsZero false
    //        reset(writer);
    //        when(config.writeNullNumberAsZero()).thenReturn(false);
    //        floatType.writeCharacter(writer, null, config);
    //        verify(writer).write(NULL_CHAR_ARRAY);
    //    }

}