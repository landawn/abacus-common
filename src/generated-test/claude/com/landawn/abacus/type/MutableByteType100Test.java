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
import com.landawn.abacus.util.MutableByte;

public class MutableByteType100Test extends TestBase {

    private MutableByteType mutableByteType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableByteType = (MutableByteType) createType("MutableByte");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableByte> clazz = mutableByteType.clazz();
        assertEquals(MutableByte.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableByteType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableByte mb = MutableByte.of((byte) 42);
        String result = mutableByteType.stringOf(mb);
        assertEquals("42", result);
    }

    @Test
    public void testValueOfNull() {
        MutableByte result = mutableByteType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableByte result = mutableByteType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableByte result = mutableByteType.valueOf("42");
        Assertions.assertNotNull(result);
        assertEquals((byte) 42, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableByte result = mutableByteType.valueOf("-42");
        Assertions.assertNotNull(result);
        assertEquals((byte) -42, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getByte(1)).thenReturn((byte) 100);
        MutableByte result = mutableByteType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals((byte) 100, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getByte("byteColumn")).thenReturn((byte) -50);
        MutableByte result = mutableByteType.get(mockResultSet, "byteColumn");
        Assertions.assertNotNull(result);
        assertEquals((byte) -50, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableByteType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setByte(1, (byte) 0);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableByteType.set(mockPreparedStatement, 1, MutableByte.of((byte) 127));
        Mockito.verify(mockPreparedStatement).setByte(1, (byte) 127);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableByteType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setByte("param", (byte) 0);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableByteType.set(mockCallableStatement, "param", MutableByte.of((byte) -128));
        Mockito.verify(mockCallableStatement).setByte("param", (byte) -128);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableByteType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableByteType.appendTo(writer, MutableByte.of((byte) 99));
        assertEquals("99", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableByteType.writeCharacter(characterWriter, null, null);
        // Verify null array was written
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableByteType.writeCharacter(characterWriter, MutableByte.of((byte) 10), null);
        // Verify byte value was written
    }
}
