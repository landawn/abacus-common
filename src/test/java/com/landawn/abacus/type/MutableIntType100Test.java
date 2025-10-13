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
import com.landawn.abacus.util.MutableInt;

@Tag("new-test")
public class MutableIntType100Test extends TestBase {

    private MutableIntType mutableIntType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableIntType = (MutableIntType) createType("MutableInt");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableInt> clazz = mutableIntType.clazz();
        assertEquals(MutableInt.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableIntType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableInt mi = MutableInt.of(42);
        String result = mutableIntType.stringOf(mi);
        assertEquals("42", result);
    }

    @Test
    public void testValueOfNull() {
        MutableInt result = mutableIntType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableInt result = mutableIntType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableInt result = mutableIntType.valueOf("12345");
        Assertions.assertNotNull(result);
        assertEquals(12345, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableInt result = mutableIntType.valueOf("-98765");
        Assertions.assertNotNull(result);
        assertEquals(-98765, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getInt(1)).thenReturn(999);
        MutableInt result = mutableIntType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(999, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getInt("intColumn")).thenReturn(-555);
        MutableInt result = mutableIntType.get(mockResultSet, "intColumn");
        Assertions.assertNotNull(result);
        assertEquals(-555, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableIntType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setInt(1, 0);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableIntType.set(mockPreparedStatement, 1, MutableInt.of(2147483647));
        Mockito.verify(mockPreparedStatement).setInt(1, 2147483647);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableIntType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setInt("param", 0);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableIntType.set(mockCallableStatement, "param", MutableInt.of(Integer.MIN_VALUE));
        Mockito.verify(mockCallableStatement).setInt("param", Integer.MIN_VALUE);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableIntType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableIntType.appendTo(writer, MutableInt.of(100));
        assertEquals("100", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableIntType.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableIntType.writeCharacter(characterWriter, MutableInt.of(123), null);
    }
}
