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
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableChar;

@Tag("new-test")
public class MutableCharType100Test extends TestBase {

    private MutableCharType mutableCharType;
    private CharacterWriter characterWriter;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        mutableCharType = (MutableCharType) createType("MutableChar");
        characterWriter = createCharacterWriter();
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testClazz() {
        Class<MutableChar> clazz = mutableCharType.clazz();
        assertEquals(MutableChar.class, clazz);
    }

    @Test
    public void testStringOfNull() {
        String result = mutableCharType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        MutableChar mc = MutableChar.of('A');
        String result = mutableCharType.stringOf(mc);
        assertEquals("A", result);
    }

    @Test
    public void testValueOfNull() {
        MutableChar result = mutableCharType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        MutableChar result = mutableCharType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidString() {
        MutableChar result = mutableCharType.valueOf("X");
        Assertions.assertNotNull(result);
        assertEquals('X', result.value());
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getInt(1)).thenReturn((int) 'Z');
        MutableChar result = mutableCharType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals('Z', result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getInt("charColumn")).thenReturn((int) '9');
        MutableChar result = mutableCharType.get(mockResultSet, "charColumn");
        Assertions.assertNotNull(result);
        assertEquals('9', result.value());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableCharType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setInt(1, 0);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableCharType.set(mockPreparedStatement, 1, MutableChar.of('M'));
        Mockito.verify(mockPreparedStatement).setInt(1, 'M');
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableCharType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setInt("param", 0);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableCharType.set(mockCallableStatement, "param", MutableChar.of('!'));
        Mockito.verify(mockCallableStatement).setInt("param", '!');
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableCharType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        mutableCharType.appendTo(writer, MutableChar.of('Q'));
        assertEquals("Q", writer.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        mutableCharType.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacterWithNonNullNoQuotation() throws IOException {
        JsonXmlSerializationConfig<?> config = Mockito.mock(JsonXmlSerializationConfig.class);
        Mockito.when(config.getCharQuotation()).thenReturn((char) 0);
        mutableCharType.writeCharacter(characterWriter, MutableChar.of('R'), config);
    }

    @Test
    public void testWriteCharacterWithNonNullWithQuotation() throws IOException {
        JsonXmlSerializationConfig<?> config = Mockito.mock(JsonXmlSerializationConfig.class);
        Mockito.when(config.getCharQuotation()).thenReturn('"');
        mutableCharType.writeCharacter(characterWriter, MutableChar.of('S'), config);
    }
}
