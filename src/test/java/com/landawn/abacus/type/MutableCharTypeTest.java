package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableChar;

public class MutableCharTypeTest extends TestBase {

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
        Class<MutableChar> clazz = mutableCharType.javaType();
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
        Mockito.when(mockResultSet.getString(1)).thenReturn("Z");
        MutableChar result = mutableCharType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals('Z', result.value());
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getString("charColumn")).thenReturn("9");
        MutableChar result = mutableCharType.get(mockResultSet, "charColumn");
        Assertions.assertNotNull(result);
        assertEquals('9', result.value());
    }

    @Test
    public void testGetByIndexWithNullString() throws SQLException {
        Mockito.when(mockResultSet.getString(2)).thenReturn(null);
        Assertions.assertNull(mutableCharType.get(mockResultSet, 2));
    }

    @Test
    public void testGetByLabelWithEmptyString() throws SQLException {
        Mockito.when(mockResultSet.getString("emptyChar")).thenReturn("");
        Assertions.assertNull(mutableCharType.get(mockResultSet, "emptyChar"));
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        mutableCharType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNull(1, Types.VARCHAR);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        mutableCharType.set(mockPreparedStatement, 1, MutableChar.of('M'));
        Mockito.verify(mockPreparedStatement).setString(1, "M");
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        mutableCharType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNull("param", Types.VARCHAR);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        mutableCharType.set(mockCallableStatement, "param", MutableChar.of('!'));
        Mockito.verify(mockCallableStatement).setString("param", "!");
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
    public void testSerializeToWithNull() throws IOException {
        assertDoesNotThrow(() -> {
            mutableCharType.serializeTo(characterWriter, null, null);
        });
    }

    @Test
    public void testSerializeToWithNonNullNoQuotation() throws IOException {
        JsonXmlSerConfig<?> config = Mockito.mock(JsonXmlSerConfig.class);
        Mockito.when(config.getCharQuotation()).thenReturn((char) 0);
        mutableCharType.serializeTo(characterWriter, MutableChar.of('R'), config);
        assertNotNull(config);
    }

    @Test
    public void testSerializeToWithNonNullWithQuotation() throws IOException {
        JsonXmlSerConfig<?> config = Mockito.mock(JsonXmlSerConfig.class);
        Mockito.when(config.getCharQuotation()).thenReturn('"');
        mutableCharType.serializeTo(characterWriter, MutableChar.of('S'), config);
        assertNotNull(config);
    }

    @Test
    public void testSerializeToEscapesSingleQuoteWhenQuotedBySingleQuote() throws IOException {
        JsonXmlSerConfig<?> config = Mockito.mock(JsonXmlSerConfig.class);
        Mockito.when(config.getCharQuotation()).thenReturn('\'');
        mutableCharType.serializeTo(characterWriter, MutableChar.of('\''), config);
        Mockito.verify(characterWriter, Mockito.times(2)).write('\'');
        Mockito.verify(characterWriter).write('\\');
        Mockito.verify(characterWriter).writeCharacter('\'');
    }
}
