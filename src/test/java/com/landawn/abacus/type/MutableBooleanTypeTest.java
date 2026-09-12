package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.landawn.abacus.util.MutableBoolean;

public class MutableBooleanTypeTest extends TestBase {

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
        Class<MutableBoolean> clazz = mutableBooleanType.javaType();
        assertEquals(MutableBoolean.class, clazz);
    }

    @Test
    public void testIsComparable() {
        boolean isComparable = mutableBooleanType.isComparable();
        Assertions.assertTrue(isComparable);
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
    public void testStringOfNull() {
        String result = mutableBooleanType.stringOf(null);
        Assertions.assertNull(result);
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
    public void testValueOfSingleCharLenientParsing() {
        // "Y"/"y"/"1" parse as true, consistent with BooleanType/AtomicBooleanType/OptionalBooleanType
        Assertions.assertTrue(mutableBooleanType.valueOf("Y").value());
        Assertions.assertTrue(mutableBooleanType.valueOf("y").value());
        Assertions.assertTrue(mutableBooleanType.valueOf("1").value());
        Assertions.assertFalse(mutableBooleanType.valueOf("N").value());
        Assertions.assertFalse(mutableBooleanType.valueOf("0").value());
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
        Mockito.verify(mockPreparedStatement).setNull(1, Types.BOOLEAN);
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
        Mockito.verify(mockCallableStatement).setNull("param", Types.BOOLEAN);
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
    public void testSerializeToWithNull() throws IOException {
        assertDoesNotThrow(() -> {
            mutableBooleanType.serializeTo(characterWriter, null, null);
        });
    }

    @Test
    public void testSerializeToWithNullHonorsWriteNullBooleanAsFalse() throws IOException {
        JsonXmlSerConfig<?> config = Mockito.mock(JsonXmlSerConfig.class);
        Mockito.when(config.isWriteNullBooleanAsFalse()).thenReturn(true);

        mutableBooleanType.serializeTo(characterWriter, null, config);

        Mockito.verify(characterWriter).write(FALSE_CHAR_ARRAY);
    }

    @Test
    public void testSerializeToWithTrue() throws IOException {
        assertDoesNotThrow(() -> {
            mutableBooleanType.serializeTo(characterWriter, MutableBoolean.of(true), null);
        });
    }

    @Test
    public void testSerializeToWithFalse() throws IOException {
        assertDoesNotThrow(() -> {
            mutableBooleanType.serializeTo(characterWriter, MutableBoolean.of(false), null);
        });
    }

    // Bug: valueOf used isEmpty (no trim), so " Y" / "  " diverged from AtomicBooleanType.
    @Test
    public void testValueOf_trimsAndTreatsBlankAsNull() {
        Assertions.assertNull(mutableBooleanType.valueOf("   "));
        Assertions.assertTrue(mutableBooleanType.valueOf(" Y").getValue());
        Assertions.assertTrue(mutableBooleanType.valueOf(" 1 ").getValue());
        Assertions.assertTrue(mutableBooleanType.valueOf(" true ").getValue());
        Assertions.assertFalse(mutableBooleanType.valueOf(" false ").getValue());
    }

    // Finding 120 (2026-09-08): the blank guard is Strings.isBlank (Character.isWhitespace) but the padding was
    // removed with String.trim (characters <= ' ' only), so an IDEOGRAPHIC SPACE followed by "true" reached
    // Boolean.valueOf with its padding still attached and came back false. parseBoolean now strips with both
    // definitions, so the guard and the strip finally agree.
    @Test
    public void reviewFixes20260908_valueOfStripsUnicodeWhitespaceLikeIsBlank() {
        // U+3000 IDEOGRAPHIC SPACE: Unicode whitespace, but above ' ', so String.trim() used to keep it.
        final String wide = String.valueOf((char) 0x3000);
        // U+0001 START OF HEADING: below ' ', but not Unicode whitespace, so String.trim() used to drop it.
        final String ctrl = String.valueOf((char) 0x0001);

        Assertions.assertNull(mutableBooleanType.valueOf(wide));
        Assertions.assertNull(mutableBooleanType.valueOf(wide + wide));

        Assertions.assertTrue(mutableBooleanType.valueOf(wide + "true").getValue());
        Assertions.assertTrue(mutableBooleanType.valueOf(wide + "true" + wide).getValue());
        Assertions.assertTrue(mutableBooleanType.valueOf(wide + "Y").getValue());
        Assertions.assertTrue(mutableBooleanType.valueOf(wide + "1" + wide).getValue());
        Assertions.assertFalse(mutableBooleanType.valueOf(wide + "false" + wide).getValue());

        // Control-character padding keeps working: dropping it is what String.trim() did, and parseBoolean still does.
        Assertions.assertTrue(mutableBooleanType.valueOf(ctrl + "true" + ctrl).getValue());
        Assertions.assertFalse(mutableBooleanType.valueOf(ctrl).getValue());

        // The plain Boolean handler reads the same text the same way.
        assertEquals(Boolean.TRUE, Type.of(Boolean.class).valueOf(wide + "true"));
    }

    // T5-02 / R-T05 (2026-09-06): MutableBoolean was the only boolean handler that quoted its CSV value.
    @Test
    public void reviewFixes20260906_csvQuoteNotRequiredLikeBoolean() throws IOException {
        Assertions.assertFalse(mutableBooleanType.isCsvQuoteRequired());
        assertEquals(Type.of(Boolean.class).isCsvQuoteRequired(), mutableBooleanType.isCsvQuoteRequired());
        assertEquals(Type.of("OptionalBoolean").isCsvQuoteRequired(), mutableBooleanType.isCsvQuoteRequired());

        final com.landawn.abacus.util.BufferedCsvWriter csvWriter = com.landawn.abacus.util.Objectory.createBufferedCsvWriter();

        try {
            com.landawn.abacus.util.CsvUtil.writeField(csvWriter, null, MutableBoolean.of(true));
            csvWriter.write(',');
            com.landawn.abacus.util.CsvUtil.writeField(csvWriter, null, Boolean.TRUE);
            csvWriter.write(',');
            com.landawn.abacus.util.CsvUtil.writeField(csvWriter, null, MutableBoolean.of(false));
            csvWriter.write(',');
            com.landawn.abacus.util.CsvUtil.writeField(csvWriter, null, null);
            assertEquals("true,true,false,null", csvWriter.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(csvWriter);
        }
    }
}
