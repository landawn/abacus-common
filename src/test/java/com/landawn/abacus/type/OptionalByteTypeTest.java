package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.u.OptionalByte;

public class OptionalByteTypeTest extends TestBase {

    private OptionalByteType optionalByteType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalByteType = (OptionalByteType) createType("OptionalByte");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalByte.class, optionalByteType.javaType());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalByteType.isComparable());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(optionalByteType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOfWithValue() {
        OptionalByte opt = OptionalByte.of((byte) 42);
        assertEquals("42", optionalByteType.stringOf(opt));
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalByteType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalByte empty = OptionalByte.empty();
        assertNull(optionalByteType.stringOf(empty));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalByte result = optionalByteType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalByte result = optionalByteType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalByte result = optionalByteType.valueOf("123");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 123, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalByte result = optionalByteType.valueOf("-50");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) -50, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalByte result = optionalByteType.valueOf(String.valueOf(Byte.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Byte.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalByte result = optionalByteType.valueOf(String.valueOf(Byte.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Byte.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalByteType.valueOf("abc"));
        assertThrows(ArithmeticException.class, () -> optionalByteType.valueOf("256"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithByte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn((byte) 42);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 42, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonByte() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalByte result = optionalByteType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 100, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn((byte) 25);

        OptionalByte result = optionalByteType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((byte) 25, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalByteType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.TINYINT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.TINYINT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalByte opt = OptionalByte.of((byte) 99);
        optionalByteType.set(stmt, 1, opt);
        verify(stmt).setByte(1, (byte) 99);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalByteType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.TINYINT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.TINYINT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalByte opt = OptionalByte.of((byte) -10);
        optionalByteType.set(stmt, "param", opt);
        verify(stmt).setByte("param", (byte) -10);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalByteType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalByte opt = OptionalByte.of((byte) 127);
        optionalByteType.appendTo(sb, opt);
        assertEquals("127", sb.toString());
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        optionalByteType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithEmpty() throws IOException {
        OptionalByte empty = OptionalByte.empty();
        optionalByteType.serializeTo(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithValue() throws IOException {
        OptionalByte opt = OptionalByte.of((byte) 64);
        optionalByteType.serializeTo(writer, opt, config);
        verify(writer).write((byte) 64);
    }

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws java.io.IOException {
        final com.landawn.abacus.util.BufferedJsonWriter jsonWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, config);
            return jsonWriter.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(jsonWriter);
        }
    }

    // T5-01 / T6-03 (2026-09-06): serializeTo ignored writeNullNumberAsZero for the optional numeric handlers.
    @Test
    public void reviewFixes20260906_serializeToHonoursWriteNullNumberAsZero() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig zero = com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullNumberAsZero(true);

        assertEquals("0", reviewFixes20260906_ser(optionalByteType, OptionalByte.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(optionalByteType, null, zero));
        assertEquals("0", reviewFixes20260906_ser(optionalByteType, OptionalByte.empty(), com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalByteType, OptionalByte.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalByteType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalByteType, OptionalByte.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalByteType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalByteType, OptionalByte.empty(), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("7", reviewFixes20260906_ser(optionalByteType, OptionalByte.of((byte) 7), zero));
        assertEquals("7", reviewFixes20260906_ser(optionalByteType, OptionalByte.of((byte) 7), null));
    }

    // T5-06 (2026-09-06, code change REJECTED - pinned): an empty-string column value is coerced by Numbers.toXxx(Object)
    // to a PRESENT zero, exactly as the non-optional handlers (IntegerType.get ...) answer, while valueOf("") is empty.
    @Test
    public void reviewFixes20260906_getEmptyStringColumnIsPresentZeroUnlikeValueOf() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("");
        when(rs.getObject("c")).thenReturn("");
        when(rs.getObject(2)).thenReturn(" ");
        when(rs.getObject(3)).thenReturn("7");
        when(rs.getObject(4)).thenReturn(null);

        assertTrue(optionalByteType.get(rs, 1).isPresent());
        assertEquals((byte) 0, optionalByteType.get(rs, 1).get());
        assertTrue(optionalByteType.get(rs, "c").isPresent());
        assertEquals((byte) 0, optionalByteType.get(rs, "c").get());
        assertThrows(NumberFormatException.class, () -> optionalByteType.get(rs, 2));
        assertEquals((byte) 7, optionalByteType.get(rs, 3).get());
        assertTrue(optionalByteType.get(rs, 4).isEmpty());

        assertTrue(optionalByteType.valueOf("").isEmpty());
        assertTrue(optionalByteType.valueOf((String) null).isEmpty());
        assertEquals((byte) 7, optionalByteType.valueOf("7").get());
    }

    // T5-03 (2026-09-06): out-of-range text throws ArithmeticException (Numbers.toXxx contract), not NumberFormatException.
    @Test
    public void reviewFixes20260906_valueOfOutOfRangeThrowsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> optionalByteType.valueOf("128"));
        assertThrows(ArithmeticException.class, () -> optionalByteType.valueOf("-129"));
        assertThrows(NumberFormatException.class, () -> optionalByteType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> optionalByteType.valueOf(" "));
        assertEquals((byte) 127, optionalByteType.valueOf("127").get());
    }
}
