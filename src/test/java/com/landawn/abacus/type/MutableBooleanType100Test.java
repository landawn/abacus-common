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
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableBoolean;

@Tag("new-test")
public class MutableBooleanType100Test extends TestBase {

    private MutableBooleanType mutableBooleanType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableBooleanType = (MutableBooleanType) createType("MutableBoolean");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableBoolean> clazz = mutableBooleanType.clazz();
        assertEquals(MutableBoolean.class, clazz);
    }

    @Test
    public void testIsComparable() {
        boolean isComparable = mutableBooleanType.isComparable();
        Assertions.assertTrue(isComparable);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableBooleanType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        String result = mutableBooleanType.stringOf(mb);
        assertEquals("true", result);
    }

    @Test
    public void testStringOfFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        String result = mutableBooleanType.stringOf(mb);
        assertEquals("false", result);
    }

    @Test
    public void testValueOfNull() {
        MutableBoolean result = mutableBooleanType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableBoolean result = mutableBooleanType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfTrue() {
        MutableBoolean result = mutableBooleanType.valueOf("true");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.value());
    }

    @Test
    public void testValueOfFalse() {
        MutableBoolean result = mutableBooleanType.valueOf("false");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.value());
    }

    @Test
    public void testGetByIndexTrue() throws SQLException {
        Mockito.when(mockResultSet.getBoolean(1)).thenReturn(true);
        MutableBoolean result = mutableBooleanType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.value());
    }

    @Test
    public void testGetByIndexFalse() throws SQLException {
        Mockito.when(mockResultSet.getBoolean(1)).thenReturn(false);
        MutableBoolean result = mutableBooleanType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.value());
    }

    @Test
    public void testGetByLabelTrue() throws SQLException {
        Mockito.when(mockResultSet.getBoolean("boolColumn")).thenReturn(true);
        MutableBoolean result = mutableBooleanType.get(mockResultSet, "boolColumn");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.value());
    }

    @Test
    public void testGetByLabelFalse() throws SQLException {
        Mockito.when(mockResultSet.getBoolean("boolColumn")).thenReturn(false);
        MutableBoolean result = mutableBooleanType.get(mockResultSet, "boolColumn");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableBooleanType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setBoolean(1, false);
    }

    @Test
    public void testSetPreparedStatementWithTrue() throws SQLException {
        mutableBooleanType.set(mockPreparedStatement, 1, MutableBoolean.of(true));
        Mockito.verify(mockPreparedStatement).setBoolean(1, true);
    }

    @Test
    public void testSetPreparedStatementWithFalse() throws SQLException {
        mutableBooleanType.set(mockPreparedStatement, 1, MutableBoolean.of(false));
        Mockito.verify(mockPreparedStatement).setBoolean(1, false);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableBooleanType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setBoolean("param", false);
    }

    @Test
    public void testSetCallableStatementWithTrue() throws SQLException {
        mutableBooleanType.set(mockCallableStatement, "param", MutableBoolean.of(true));
        Mockito.verify(mockCallableStatement).setBoolean("param", true);
    }

    @Test
    public void testSetCallableStatementWithFalse() throws SQLException {
        mutableBooleanType.set(mockCallableStatement, "param", MutableBoolean.of(false));
        Mockito.verify(mockCallableStatement).setBoolean("param", false);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableBooleanType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithTrue() throws IOException {
        StringWriter writer = new StringWriter();
        mutableBooleanType.appendTo(writer, MutableBoolean.of(true));
        assertEquals("true", writer.toString());
    }

    @Test
    public void testAppendToWithFalse() throws IOException {
        StringWriter writer = new StringWriter();
        mutableBooleanType.appendTo(writer, MutableBoolean.of(false));
        assertEquals("false", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableBooleanType.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacterWithTrue() throws IOException {
        mutableBooleanType.writeCharacter(characterWriter, MutableBoolean.of(true), null);
    }

    @Test
    public void testWriteCharacterWithFalse() throws IOException {
        mutableBooleanType.writeCharacter(characterWriter, MutableBoolean.of(false), null);
    }
}
