package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableDouble;

@Tag("new-test")
public class MutableDoubleType100Test extends TestBase {

    private MutableDoubleType mutableDoubleType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableDoubleType = (MutableDoubleType) createType("MutableDouble");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableDouble> clazz = mutableDoubleType.clazz();
        assertEquals(MutableDouble.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableDoubleType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableDouble md = MutableDouble.of(3.14159);
        String result = mutableDoubleType.stringOf(md);
        assertEquals("3.14159", result);
    }

    @Test
    public void testValueOfNull() {
        MutableDouble result = mutableDoubleType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableDouble result = mutableDoubleType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableDouble result = mutableDoubleType.valueOf("2.71828");
        Assertions.assertNotNull(result);
        assertEquals(2.71828, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableDouble result = mutableDoubleType.valueOf("-123.456");
        Assertions.assertNotNull(result);
        assertEquals(-123.456, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getDouble(1)).thenReturn(99.99);
        MutableDouble result = mutableDoubleType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(99.99, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getDouble("doubleColumn")).thenReturn(-0.0001);
        MutableDouble result = mutableDoubleType.get(mockResultSet, "doubleColumn");
        Assertions.assertNotNull(result);
        assertEquals(-0.0001, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableDoubleType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setDouble(1, 0.0);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableDoubleType.set(mockPreparedStatement, 1, MutableDouble.of(1234.5678));
        Mockito.verify(mockPreparedStatement).setDouble(1, 1234.5678);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableDoubleType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setDouble("param", 0.0);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableDoubleType.set(mockCallableStatement, "param", MutableDouble.of(Double.MAX_VALUE));
        Mockito.verify(mockCallableStatement).setDouble("param", Double.MAX_VALUE);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableDoubleType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableDoubleType.appendTo(writer, MutableDouble.of(42.0));
        assertEquals("42.0", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableDoubleType.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableDoubleType.writeCharacter(characterWriter, MutableDouble.of(3.0), null);
    }
}
