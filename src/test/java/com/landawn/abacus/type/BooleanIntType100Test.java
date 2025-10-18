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
public class BooleanIntType100Test extends TestBase {

    private BooleanIntType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BooleanIntType) createType("BooleanInt");
        writer = createCharacterWriter();
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
        assertEquals("1", result);
    }

    @Test
    public void testStringOf_False() {
        String result = type.stringOf(Boolean.FALSE);
        assertEquals("0", result);
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        assertEquals("0", result);
    }

    @Test
    public void testValueOf_One() {
        Boolean result = type.valueOf("1");
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_Zero() {
        Boolean result = type.valueOf("0");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_Other() {
        Boolean result = type.valueOf("2");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_Null() {
        Boolean result = type.valueOf((String) null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_One() {
        char[] chars = { '1' };
        Boolean result = type.valueOf(chars, 0, 1);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testValueOf_CharArray_Zero() {
        char[] chars = { '0' };
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
        char[] chars = { '1' };
        Boolean result = type.valueOf(chars, 0, 0);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testValueOf_CharArray_MultipleChars() {
        char[] chars = { '1', '0' };
        Boolean result = type.valueOf(chars, 0, 2);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testGet_ResultSet_Int_Positive() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(5);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getInt(1);
    }

    @Test
    public void testGet_ResultSet_Int_Zero() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.FALSE, result);
        verify(rs).getInt(1);
    }

    @Test
    public void testGet_ResultSet_Int_Negative() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(-1);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.FALSE, result);
        verify(rs).getInt(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("columnName")).thenReturn(1);

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getInt("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int_True() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, Boolean.TRUE);

        verify(stmt).setString(1, "1");
    }

    @Test
    public void testSet_PreparedStatement_Int_False() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, Boolean.FALSE);

        verify(stmt).setString(1, "0");
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setNull(1, Types.BOOLEAN);
    }

    @Test
    public void testSet_CallableStatement_String_True() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", Boolean.TRUE);

        verify(stmt).setString("paramName", "1");
    }

    @Test
    public void testSet_CallableStatement_String_False() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", Boolean.FALSE);

        verify(stmt).setString("paramName", "0");
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setNull("paramName", Types.BOOLEAN);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, Boolean.TRUE);
        assertEquals("1", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, Boolean.FALSE);
        assertEquals("0", sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("0", sw.toString());
    }

    @Test
    public void testWriteCharacter_NoQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn((char) 0);

        type.writeCharacter(mockWriter, Boolean.TRUE, config);

        verify(mockWriter).write("1");
    }

    @Test
    public void testWriteCharacter_WithQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('"');

        type.writeCharacter(mockWriter, Boolean.FALSE, config);

        verify(mockWriter).write("0");
        verify(mockWriter, times(2)).write('"');
    }

    @Test
    public void testWriteCharacter_NullConfig() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();

        type.writeCharacter(mockWriter, Boolean.TRUE, null);

        verify(mockWriter).write("1");
    }
}
