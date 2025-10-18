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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
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
        assertNull(floatType.stringOf(null));

        Float floatValue = 3.14f;
        assertEquals("3.14", floatType.stringOf(floatValue));

        assertEquals("3.14", floatType.stringOf(Double.valueOf(3.14)));
        assertEquals("42", floatType.stringOf(Integer.valueOf(42)));
        assertEquals("100", floatType.stringOf(Long.valueOf(100L)));
    }

    @Test
    public void testValueOf() {
        assertNull(floatType.valueOf(null));
        assertNull(floatType.valueOf(""));
        assertThrows(NumberFormatException.class, () -> assertNull(floatType.valueOf("  ")));

        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14"));
        assertEquals(Float.valueOf(-10.5f), floatType.valueOf("-10.5"));
        assertEquals(Float.valueOf(0.0f), floatType.valueOf("0.0"));

        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14f"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14F"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14d"));
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14D"));
        assertEquals(Float.valueOf(100f), floatType.valueOf("100l"));
        assertEquals(Float.valueOf(100f), floatType.valueOf("100L"));

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

        floatType.set(stmt, 1, null);
        verify(stmt).setNull(1, Types.FLOAT);

        floatType.set(stmt, 2, 3.14f);
        verify(stmt).setFloat(2, 3.14f);

        floatType.set(stmt, 3, Float.valueOf(2.718f));
        verify(stmt).setFloat(3, 2.718f);

        floatType.set(stmt, 4, 42f);
        verify(stmt).setFloat(4, 42.0f);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        floatType.set(stmt, "param1", null);
        verify(stmt).setNull("param1", Types.FLOAT);

        floatType.set(stmt, "param2", 3.14f);
        verify(stmt).setFloat("param2", 3.14f);

        floatType.set(stmt, "param3", Double.valueOf(2.718));
        verify(stmt).setFloat("param3", 2.718f);

        floatType.set(stmt, "param4", Integer.valueOf(42));
        verify(stmt).setFloat("param4", 42.0f);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        floatType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        floatType.appendTo(sb, 3.14f);
        assertEquals("3.14", sb.toString());

        sb.setLength(0);
        floatType.appendTo(sb, Double.valueOf(2.718));
        assertEquals("2.718", sb.toString());

        Appendable errorAppendable = mock(Appendable.class);
        when(errorAppendable.append(anyString())).thenThrow(new IOException("Test exception"));
        assertThrows(IOException.class, () -> floatType.appendTo(errorAppendable, null));
    }

}
