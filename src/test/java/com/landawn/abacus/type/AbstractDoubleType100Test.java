package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractDoubleType100Test extends TestBase {
    private Type<Double> type;
    private CharacterWriter writer;

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
        type = createType(Double.class);
        writer = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOf_DoubleValue() {
        assertEquals("0.0", type.stringOf(0.0));
        assertEquals("1.23", type.stringOf(1.23));
        assertEquals("-45.67", type.stringOf(-45.67));
        assertEquals("1.7976931348623157E308", type.stringOf(Double.MAX_VALUE));
        assertEquals("4.9E-324", type.stringOf(Double.MIN_VALUE));
    }

    @Test
    public void testStringOf_SpecialValues() {
        assertEquals("NaN", type.stringOf(Double.NaN));
        assertEquals("Infinity", type.stringOf(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", type.stringOf(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testValueOf_String_Null() {
        Double result = type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Double result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_String_ValidDouble() {
        assertEquals(0.0, type.valueOf("0.0"));
        assertEquals(1.23, type.valueOf("1.23"));
        assertEquals(-45.67, type.valueOf("-45.67"));
        assertEquals(1.0E10, type.valueOf("1.0E10"));
        assertEquals(-3.14E-5, type.valueOf("-3.14E-5"));
    }

    @Test
    public void testValueOf_String_SpecialValues() {
        assertEquals(Double.NaN, type.valueOf("NaN"));
        assertEquals(Double.POSITIVE_INFINITY, type.valueOf("Infinity"));
        assertEquals(Double.NEGATIVE_INFINITY, type.valueOf("-Infinity"));
    }

    @Test
    public void testValueOf_String_WithSuffix() {
        assertEquals(42.5, type.valueOf("42.5l"));
        assertEquals(42.5, type.valueOf("42.5L"));
        assertEquals(42.5, type.valueOf("42.5f"));
        assertEquals(42.5, type.valueOf("42.5F"));
        assertEquals(42.5, type.valueOf("42.5d"));
        assertEquals(42.5, type.valueOf("42.5D"));
    }

    @Test
    public void testValueOf_String_InvalidFormat() {
        assertThrows(NumberFormatException.class, () -> type.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("12.34.56"));
    }

    @Test
    public void testValueOf_String_SingleCharSuffix() {
        assertEquals(1.0, type.valueOf("1"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("l"));
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(123.45);
        assertEquals(123.45, type.get(resultSet, 1));
        verify(resultSet).getObject(1);
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        when(resultSet.getObject("price")).thenReturn(99.99);
        assertEquals(99.99, type.get(resultSet, "price"));
        verify(resultSet).getObject("price");
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setNull(1, Types.DOUBLE);
    }

    @Test
    public void testSet_PreparedStatement_DoubleValue() throws SQLException {
        type.set(preparedStatement, 1, 42.5);
        verify(preparedStatement).setDouble(1, 42.5);
    }

    @Test
    public void testSet_PreparedStatement_NumberValue() throws SQLException {
        type.set(preparedStatement, 1, 100d);
        verify(preparedStatement).setDouble(1, 100.0);

        type.set(preparedStatement, 2, 42d);
        verify(preparedStatement).setDouble(2, 42.0);

        type.set(preparedStatement, 3, 3.14d);
        verify(preparedStatement).setDouble(3, 3.14);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setNull("param", Types.DOUBLE);
    }

    @Test
    public void testSet_CallableStatement_DoubleValue() throws SQLException {
        type.set(callableStatement, "param", -5.5);
        verify(callableStatement).setDouble("param", -5.5);
    }

    @Test
    public void testSet_CallableStatement_NumberValue() throws SQLException {
        type.set(callableStatement, "param", 127d);
        verify(callableStatement).setDouble("param", 127.0);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_DoubleValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, 42.5);
        assertEquals("42.5", sb.toString());
    }

    @Test
    public void testAppendTo_NumberValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, 100d);
        assertEquals("100.0", sb.toString());
    }

    @Test
    public void testAppendTo_SpecialValues() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Double.NaN);
        assertEquals("NaN", sb.toString());

        sb = new StringBuilder();
        type.appendTo(sb, Double.POSITIVE_INFINITY);
        assertEquals("Infinity", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null_NoConfig() throws IOException {
        type.writeCharacter(writer, null, null);
    }

    @Test
    public void testWriteCharacter_DoubleValue_NoConfig() throws IOException {
        type.writeCharacter(writer, 42.5, null);
    }

    @Test
    public void testWriteCharacter_Null_WithWriteNullNumberAsZero() throws IOException {
        when(config.writeNullNumberAsZero()).thenReturn(true);
        type.writeCharacter(writer, null, config);
    }

    @Test
    public void testWriteCharacter_Null_WithoutWriteNullNumberAsZero() throws IOException {
        when(config.writeNullNumberAsZero()).thenReturn(false);
        type.writeCharacter(writer, null, config);
    }

    @Test
    public void testWriteCharacter_NumberValue() throws IOException {
        type.writeCharacter(writer, 100d, config);

        type.writeCharacter(writer, 3.14d, config);
    }

    @Test
    public void testWriteCharacter_SpecialValues() throws IOException {
        type.writeCharacter(writer, Double.NaN, config);

        type.writeCharacter(writer, Double.POSITIVE_INFINITY, config);
    }
}
