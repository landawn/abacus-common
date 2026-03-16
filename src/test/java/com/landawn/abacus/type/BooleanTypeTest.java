package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class BooleanTypeTest extends TestBase {

    private final BooleanType type = new BooleanType();

    @Test
    public void test_clazz() {
        assertEquals(Boolean.class, type.javaType());
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(type.isPrimitiveWrapper());
    }

    @Test
    public void test_isBoolean() {
        assertTrue(type.isBoolean());
    }

    @Test
    public void test_name() {
        assertEquals("Boolean", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("true", type.stringOf(Boolean.TRUE));
        assertEquals("false", type.stringOf(Boolean.FALSE));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_Object() {
        // Boolean input
        assertEquals(Boolean.TRUE, type.valueOf(Boolean.TRUE));
        assertEquals(Boolean.FALSE, type.valueOf(Boolean.FALSE));

        // Number input - positive
        assertEquals(Boolean.TRUE, type.valueOf(1));
        assertEquals(Boolean.TRUE, type.valueOf(100L));

        // Number input - zero and negative
        assertEquals(Boolean.FALSE, type.valueOf(0));
        assertEquals(Boolean.FALSE, type.valueOf(-1));

        // String input
        assertEquals(Boolean.TRUE, type.valueOf("true"));
        assertEquals(Boolean.TRUE, type.valueOf("Y"));
        assertEquals(Boolean.TRUE, type.valueOf("y"));
        assertEquals(Boolean.TRUE, type.valueOf("1"));
        assertEquals(Boolean.FALSE, type.valueOf("false"));
        assertEquals(Boolean.FALSE, type.valueOf("N"));

        // Null input
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(Boolean.TRUE, type.valueOf("true"));
        assertEquals(Boolean.TRUE, type.valueOf("TRUE"));
        assertEquals(Boolean.TRUE, type.valueOf("Y"));
        assertEquals(Boolean.TRUE, type.valueOf("y"));
        assertEquals(Boolean.TRUE, type.valueOf("1"));

        assertEquals(Boolean.FALSE, type.valueOf("false"));
        assertEquals(Boolean.FALSE, type.valueOf("FALSE"));
        assertEquals(Boolean.FALSE, type.valueOf("N"));

        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_charArray() {
        // Test "true"
        char[] trueChars = "true".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(trueChars, 0, 4));

        // Test "TRUE"
        char[] upperTrueChars = "TRUE".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(upperTrueChars, 0, 4));

        // Test "false"
        char[] falseChars = "false".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(falseChars, 0, 5));

        // Test with offset
        char[] mixedChars = "xxtrueyy".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(mixedChars, 2, 4));

        // Test null and empty
        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(trueChars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with Boolean value
        when(rs.getObject(1)).thenReturn(Boolean.TRUE);
        assertEquals(Boolean.TRUE, type.get(rs, 1));

        // Test with null
        when(rs.getObject(2)).thenReturn(null);
        assertNull(type.get(rs, 2));

        // Test with Integer value (conversion)
        when(rs.getObject(3)).thenReturn(1);
        assertEquals(Boolean.TRUE, type.get(rs, 3));

        // Test with String value (conversion)
        when(rs.getObject(4)).thenReturn("true");
        assertEquals(Boolean.TRUE, type.get(rs, 4));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with Boolean value
        when(rs.getObject("flag")).thenReturn(Boolean.FALSE);
        assertEquals(Boolean.FALSE, type.get(rs, "flag"));

        // Test with null
        when(rs.getObject("nullFlag")).thenReturn(null);
        assertNull(type.get(rs, "nullFlag"));

        // Test with conversion
        when(rs.getObject("intFlag")).thenReturn(0);
        assertEquals(Boolean.FALSE, type.get(rs, "intFlag"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with true
        type.set(stmt, 1, Boolean.TRUE);
        verify(stmt).setBoolean(1, true);

        // Test with false
        type.set(stmt, 2, Boolean.FALSE);
        verify(stmt).setBoolean(2, false);

        // Test with null
        type.set(stmt, 3, null);
        verify(stmt).setNull(3, java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with true
        type.set(stmt, "param1", Boolean.TRUE);
        verify(stmt).setBoolean("param1", true);

        // Test with false
        type.set(stmt, "param2", Boolean.FALSE);
        verify(stmt).setBoolean("param2", false);

        // Test with null
        type.set(stmt, "param3", null);
        verify(stmt).setNull("param3", java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test true
        type.appendTo(sw, Boolean.TRUE);
        assertEquals("true", sw.toString());

        // Test false
        sw = new StringWriter();
        type.appendTo(sw, Boolean.FALSE);
        assertEquals("false", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter_withoutConfig() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        // Test true
        type.writeCharacter(writer, Boolean.TRUE, null);
        verify(writer).write("true".toCharArray());

        // Test false
        type.writeCharacter(writer, Boolean.FALSE, null);
        verify(writer).write("false".toCharArray());

        // Test null
        type.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    public void test_writeCharacter_withConfig_writeNullAsFalse() throws Exception {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.isWriteNullBooleanAsFalse()).thenReturn(true);

        // Test null with writeNullBooleanAsFalse
        type.writeCharacter(writer, null, config);
        verify(writer).write("false".toCharArray());
    }

    @Test
    public void testClazz() {
        Class<Boolean> result = type.javaType();
        assertEquals(Boolean.class, result);
    }

    @Test
    public void testIsPrimitiveWrapper() {
        boolean result = type.isPrimitiveWrapper();
        Assertions.assertTrue(result);
    }

    @Test
    public void testGet_ResultSet_Int_Boolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(Boolean.TRUE);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        Boolean result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_Number() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(1);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_ZeroNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(0);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("true");

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_String_Boolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(Boolean.FALSE);

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(null);

        Boolean result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Number() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(0L);

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_StringValue() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("false");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_YCharacter() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("Y");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_OneCharacter() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("1");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject("columnName");
    }

}
