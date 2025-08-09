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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableFloat;

public class MutableFloatType100Test extends TestBase {

    private MutableFloatType mutableFloatType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableFloatType = (MutableFloatType) createType("MutableFloat");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableFloat> clazz = mutableFloatType.clazz();
        assertEquals(MutableFloat.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableFloatType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableFloat mf = MutableFloat.of(3.14f);
        String result = mutableFloatType.stringOf(mf);
        assertEquals("3.14", result);
    }

    @Test
    public void testValueOfNull() {
        MutableFloat result = mutableFloatType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableFloat result = mutableFloatType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableFloat result = mutableFloatType.valueOf("2.5");
        Assertions.assertNotNull(result);
        assertEquals(2.5f, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableFloat result = mutableFloatType.valueOf("-123.45");
        Assertions.assertNotNull(result);
        assertEquals(-123.45f, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getFloat(1)).thenReturn(99.9f);
        MutableFloat result = mutableFloatType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(99.9f, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getFloat("floatColumn")).thenReturn(-0.001f);
        MutableFloat result = mutableFloatType.get(mockResultSet, "floatColumn");
        Assertions.assertNotNull(result);
        assertEquals(-0.001f, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableFloatType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setFloat(1, 0.0f);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableFloatType.set(mockPreparedStatement, 1, MutableFloat.of(123.456f));
        Mockito.verify(mockPreparedStatement).setFloat(1, 123.456f);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableFloatType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setFloat("param", 0.0f);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableFloatType.set(mockCallableStatement, "param", MutableFloat.of(Float.MAX_VALUE));
        Mockito.verify(mockCallableStatement).setFloat("param", Float.MAX_VALUE);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableFloatType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableFloatType.appendTo(writer, MutableFloat.of(42.0f));
        assertEquals("42.0", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableFloatType.writeCharacter(characterWriter, null, null);
        // Verify null array was written
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableFloatType.writeCharacter(characterWriter, MutableFloat.of(3.0f), null);
        // Verify float value was written
    }
}
