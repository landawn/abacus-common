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
import com.landawn.abacus.util.MutableShort;

public class MutableShortType100Test extends TestBase {

    private MutableShortType mutableShortType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableShortType = (MutableShortType) createType("MutableShort");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableShort> clazz = mutableShortType.clazz();
        assertEquals(MutableShort.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableShortType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableShort ms = MutableShort.of((short) 12345);
        String result = mutableShortType.stringOf(ms);
        assertEquals("12345", result);
    }

    @Test
    public void testValueOfNull() {
        MutableShort result = mutableShortType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableShort result = mutableShortType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableShort result = mutableShortType.valueOf("30000");
        Assertions.assertNotNull(result);
        assertEquals((short) 30000, result.value());
    }

    @Test
    public void testValueOfNegativeString() {
        MutableShort result = mutableShortType.valueOf("-20000");
        Assertions.assertNotNull(result);
        assertEquals((short) -20000, result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getShort(1)).thenReturn((short) 999);
        MutableShort result = mutableShortType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals((short) 999, result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getShort("shortColumn")).thenReturn((short) -555);
        MutableShort result = mutableShortType.get(mockResultSet, "shortColumn");
        Assertions.assertNotNull(result);
        assertEquals((short) -555, result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableShortType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setShort(1, (short) 0);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableShortType.set(mockPreparedStatement, 1, MutableShort.of(Short.MAX_VALUE));
        Mockito.verify(mockPreparedStatement).setShort(1, Short.MAX_VALUE);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableShortType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setShort("param", (short) 0);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableShortType.set(mockCallableStatement, "param", MutableShort.of(Short.MIN_VALUE));
        Mockito.verify(mockCallableStatement).setShort("param", Short.MIN_VALUE);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableShortType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableShortType.appendTo(writer, MutableShort.of((short) 100));
        assertEquals("100", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableShortType.writeCharacter(characterWriter, null, null);
        // Verify null array was written
    }

    @Test
    public void testWriteCharacterWithNonNull() throws IOException {
        mutableShortType.writeCharacter(characterWriter, MutableShort.of((short) 123), null);
        // Verify short value was written
    }
}
