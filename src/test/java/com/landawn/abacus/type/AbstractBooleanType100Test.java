package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractBooleanType100Test extends TestBase {
    private Type<Boolean> type;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Boolean.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testIsBoolean() {
        assertTrue(type.isBoolean());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(type.isNonQuotableCsvType());
    }

    @Test
    public void testStringOf_Null() {
        assertEquals(null, type.stringOf(null));
    }

    @Test
    public void testStringOf_True() {
        assertEquals("true", type.stringOf(Boolean.TRUE));
    }

    @Test
    public void testStringOf_False() {
        assertEquals("false", type.stringOf(Boolean.FALSE));
    }

    @Test
    public void testValueOf_NullObject() {
        Boolean result = type.valueOf((Object) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_BooleanObject() {
        assertEquals(Boolean.TRUE, type.valueOf(Boolean.TRUE));
        assertEquals(Boolean.FALSE, type.valueOf(Boolean.FALSE));
    }

    @Test
    public void testValueOf_PositiveNumber() {
        assertEquals(Boolean.TRUE, type.valueOf(1));
        assertEquals(Boolean.TRUE, type.valueOf(100L));
        assertEquals(Boolean.FALSE, type.valueOf(0.1));
    }

    @Test
    public void testValueOf_ZeroNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(0));
        assertEquals(Boolean.FALSE, type.valueOf(0L));
        assertEquals(Boolean.FALSE, type.valueOf(0.0));
    }

    @Test
    public void testValueOf_NegativeNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(-1));
        assertEquals(Boolean.FALSE, type.valueOf(-100L));
    }

    @Test
    public void testValueOf_CharSequence_Y() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "Y"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "y"));
    }

    @Test
    public void testValueOf_CharSequence_1() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "1"));
    }

    @Test
    public void testValueOf_CharSequence_True() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "true"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "TRUE"));
    }

    @Test
    public void testValueOf_CharSequence_False() {
        assertEquals(Boolean.FALSE, type.valueOf((Object) "false"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "FALSE"));
    }

    @Test
    public void testValueOf_CharSequence_Other() {
        assertEquals(Boolean.FALSE, type.valueOf((Object) "N"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "0"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "abc"));
    }

    @Test
    public void testValueOf_String_Null() {
        Boolean result = type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Boolean result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_String_SingleChar() {
        assertEquals(Boolean.TRUE, type.valueOf("Y"));
        assertEquals(Boolean.TRUE, type.valueOf("y"));
        assertEquals(Boolean.TRUE, type.valueOf("1"));
        assertEquals(Boolean.FALSE, type.valueOf("N"));
        assertEquals(Boolean.FALSE, type.valueOf("0"));
    }

    @Test
    public void testValueOf_String_Boolean() {
        assertEquals(Boolean.TRUE, type.valueOf("true"));
        assertEquals(Boolean.TRUE, type.valueOf("TRUE"));
        assertEquals(Boolean.FALSE, type.valueOf("false"));
        assertEquals(Boolean.FALSE, type.valueOf("FALSE"));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Boolean result = type.valueOf(null, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        Boolean result = type.valueOf(cbuf, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_True() {
        char[] cbuf = "true".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));

        cbuf = "TRUE".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));

        cbuf = "TrUe".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));
    }

    @Test
    public void testValueOf_CharArray_False() {
        char[] cbuf = "false".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(cbuf, 0, 5));

        cbuf = "xyz".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(cbuf, 0, 3));
    }

    @Test
    public void testValueOf_CharArray_Offset() {
        char[] cbuf = "xxtrueyy".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 2, 4));
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(true);
        assertEquals(Boolean.TRUE, type.get(resultSet, 1));
        verify(resultSet).getObject(1);
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        when(resultSet.getObject("active")).thenReturn(false);
        assertEquals(Boolean.FALSE, type.get(resultSet, "active"));
        verify(resultSet).getObject("active");
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setNull(1, java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSet_PreparedStatement_True() throws SQLException {
        type.set(preparedStatement, 1, Boolean.TRUE);
        verify(preparedStatement).setBoolean(1, true);
    }

    @Test
    public void testSet_PreparedStatement_False() throws SQLException {
        type.set(preparedStatement, 1, Boolean.FALSE);
        verify(preparedStatement).setBoolean(1, false);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setNull("param", java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSet_CallableStatement_True() throws SQLException {
        type.set(callableStatement, "param", Boolean.TRUE);
        verify(callableStatement).setBoolean("param", true);
    }

    @Test
    public void testSet_CallableStatement_False() throws SQLException {
        type.set(callableStatement, "param", Boolean.FALSE);
        verify(callableStatement).setBoolean("param", false);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_True() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Boolean.TRUE);
        assertEquals("true", sb.toString());
    }

    @Test
    public void testAppendTo_False() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Boolean.FALSE);
        assertEquals("false", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null_NoConfig() throws IOException {
        type.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacter_True_NoConfig() throws IOException {
        type.writeCharacter(characterWriter, Boolean.TRUE, null);
    }

    @Test
    public void testWriteCharacter_False_NoConfig() throws IOException {
        type.writeCharacter(characterWriter, Boolean.FALSE, null);
    }

    @Test
    public void testWriteCharacter_Null_WithWriteNullBooleanAsFalse() throws IOException {
        when(config.writeNullBooleanAsFalse()).thenReturn(true);
        type.writeCharacter(characterWriter, null, config);
    }

    @Test
    public void testWriteCharacter_Null_WithoutWriteNullBooleanAsFalse() throws IOException {
        when(config.writeNullBooleanAsFalse()).thenReturn(false);
        type.writeCharacter(characterWriter, null, config);
    }
}
