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
import com.landawn.abacus.util.MutableLong;

@Tag("new-test")
public class MutableLongType100Test extends TestBase {

    private MutableLongType mutableLongType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableLongType = (MutableLongType) createType("MutableLong");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableLong> clazz = mutableLongType.clazz();
        assertEquals(MutableLong.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableLongType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableLong ml = MutableLong.of(9876543210L);
        String result = mutableLongType.stringOf(ml);
        assertEquals("9876543210", result);
    }

    @Test
    public void testValueOfNull() {
        MutableLong result = mutableLongType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableLong result = mutableLongType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableLong result = mutableLongType.valueOf("123456789012345");
        Assertions.assertNotNull(result);
        assertEquals(123456789012345L, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableLong result = mutableLongType.valueOf("-9876543210");
        Assertions.assertNotNull(result);
        assertEquals(-9876543210L, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getLong(1)).thenReturn(999999999L);
        MutableLong result = mutableLongType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(999999999L, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getLong("longColumn")).thenReturn(-555555555L);
        MutableLong result = mutableLongType.get(mockResultSet, "longColumn");
        Assertions.assertNotNull(result);
        assertEquals(-555555555L, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableLongType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setLong(1, 0L);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableLongType.set(mockPreparedStatement, 1, MutableLong.of(Long.MAX_VALUE));
        Mockito.verify(mockPreparedStatement).setLong(1, Long.MAX_VALUE);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableLongType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setLong("param", 0L);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableLongType.set(mockCallableStatement, "param", MutableLong.of(Long.MIN_VALUE));
        Mockito.verify(mockCallableStatement).setLong("param", Long.MIN_VALUE);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableLongType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableLongType.appendTo(writer, MutableLong.of(100000L));
        assertEquals("100000", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableLongType.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableLongType.writeCharacter(characterWriter, MutableLong.of(123456L), null);
    }
}
