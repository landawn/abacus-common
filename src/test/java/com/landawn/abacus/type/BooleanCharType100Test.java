package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BooleanCharType100Test extends TestBase {

    private BooleanCharType type;

    @BeforeEach
    public void setUp() {
        type = (BooleanCharType) createType("BooleanChar");
    }

    @Test
    public void testClazz() {
        Class<Boolean> result = type.clazz();
        assertEquals(Boolean.class, result);
    }

    @Test
    public void testDefaultValue() {
        Boolean result = type.defaultValue();
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testIsNonQuotableCsvType() {
        boolean result = type.isNonQuotableCsvType();
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringOf_True() {
        String result = type.stringOf(Boolean.TRUE);
        assertEquals("Y", result);
    }

    @Test
    public void testStringOf_False() {
        String result = type.stringOf(Boolean.FALSE);
        assertEquals("N", result);
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        assertEquals("N", result);
    }

    @Test
    public void testValueOf_Y_Uppercase() {
        Boolean result = type.valueOf("Y");
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_Y_Lowercase() {
        Boolean result = type.valueOf("y");
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_N() {
        Boolean result = type.valueOf("N");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_Other() {
        Boolean result = type.valueOf("X");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_Null() {
        Boolean result = type.valueOf((String) null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_Y_Uppercase() {
        char[] chars = { 'Y' };
        Boolean result = type.valueOf(chars, 0, 1);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_CharArray_Y_Lowercase() {
        char[] chars = { 'y' };
        Boolean result = type.valueOf(chars, 0, 1);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_CharArray_N() {
        char[] chars = { 'N' };
        Boolean result = type.valueOf(chars, 0, 1);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Boolean result = type.valueOf(null, 0, 0);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] chars = { 'Y' };
        Boolean result = type.valueOf(chars, 0, 0);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_MultipleChars() {
        char[] chars = { 'Y', 'E', 'S' };
        Boolean result = type.valueOf(chars, 0, 3);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("Y");

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_N() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("N");

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.FALSE, result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("columnName")).thenReturn("y");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getString("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int_True() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, Boolean.TRUE);

        verify(stmt).setString(1, "Y");
    }

    @Test
    public void testSet_PreparedStatement_Int_False() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, Boolean.FALSE);

        verify(stmt).setString(1, "N");
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setNull(1, Types.VARCHAR);
    }

    @Test
    public void testSet_CallableStatement_String_True() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", Boolean.TRUE);

        verify(stmt).setString("paramName", "Y");
    }

    @Test
    public void testSet_CallableStatement_String_False() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", Boolean.FALSE);

        verify(stmt).setString("paramName", "N");
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setNull("paramName", Types.VARCHAR);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, Boolean.TRUE);
        assertEquals("Y", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, Boolean.FALSE);
        assertEquals("N", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("N", sw.toString());
    }

    @Test
    public void testWriteCharacter_NoQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn((char) 0);

        type.writeCharacter(mockWriter, Boolean.TRUE, config);

        verify(mockWriter).write("Y");
    }

    @Test
    public void testWriteCharacter_WithQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(mockWriter, Boolean.FALSE, config);

        verify(mockWriter, times(2)).write('\'');
        verify(mockWriter).write("N");
        verify(mockWriter, times(2)).write('\'');
    }

    @Test
    public void testWriteCharacter_NullConfig() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();

        type.writeCharacter(mockWriter, Boolean.TRUE, null);

        verify(mockWriter).write("Y");
    }
}
