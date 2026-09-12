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
import com.landawn.abacus.util.u.OptionalShort;

public class OptionalShortTypeTest extends TestBase {

    private OptionalShortType optionalShortType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        optionalShortType = (OptionalShortType) createType("OptionalShort");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalShort.class, optionalShortType.javaType());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalShortType.isComparable());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(optionalShortType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOfWithValue() {
        OptionalShort opt = OptionalShort.of((short) 1234);
        assertEquals("1234", optionalShortType.stringOf(opt));
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(optionalShortType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmpty() {
        OptionalShort empty = OptionalShort.empty();
        assertNull(optionalShortType.stringOf(empty));
    }

    @Test
    public void testValueOfWithNull() {
        OptionalShort result = optionalShortType.valueOf(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithEmptyString() {
        OptionalShort result = optionalShortType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfWithValidString() {
        OptionalShort result = optionalShortType.valueOf("5678");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 5678, result.get());
    }

    @Test
    public void testValueOfWithNegativeValue() {
        OptionalShort result = optionalShortType.valueOf("-1000");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) -1000, result.get());
    }

    @Test
    public void testValueOfWithMaxValue() {
        OptionalShort result = optionalShortType.valueOf(String.valueOf(Short.MAX_VALUE));
        assertNotNull(result);
        assertEquals(Short.MAX_VALUE, result.get());
    }

    @Test
    public void testValueOfWithMinValue() {
        OptionalShort result = optionalShortType.valueOf(String.valueOf(Short.MIN_VALUE));
        assertNotNull(result);
        assertEquals(Short.MIN_VALUE, result.get());
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> optionalShortType.valueOf("not-a-number"));
        assertThrows(ArithmeticException.class, () -> optionalShortType.valueOf("40000"));
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetFromResultSetByIndexWithShort() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn((short) 42);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 42, result.get());
    }

    @Test
    public void testGetFromResultSetByIndexWithNonShort() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(100);

        OptionalShort result = optionalShortType.get(rs, 1);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 100, result.get());
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("column")).thenReturn((short) 999);

        OptionalShort result = optionalShortType.get(rs, "column");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals((short) 999, result.get());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        optionalShortType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetPreparedStatementWithEmpty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.set(stmt, 1, empty);
        verify(stmt).setNull(1, java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalShort opt = OptionalShort.of((short) 777);
        optionalShortType.set(stmt, 1, opt);
        verify(stmt).setShort(1, (short) 777);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        optionalShortType.set(stmt, "param", null);
        verify(stmt).setNull("param", java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetCallableStatementWithEmpty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.set(stmt, "param", empty);
        verify(stmt).setNull("param", java.sql.Types.SMALLINT);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalShort opt = OptionalShort.of((short) -333);
        optionalShortType.set(stmt, "param", opt);
        verify(stmt).setShort("param", (short) -333);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        optionalShortType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.appendTo(sb, empty);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalShort opt = OptionalShort.of((short) 12345);
        optionalShortType.appendTo(sb, opt);
        assertEquals("12345", sb.toString());
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        optionalShortType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithEmpty() throws IOException {
        OptionalShort empty = OptionalShort.empty();
        optionalShortType.serializeTo(writer, empty, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithValue() throws IOException {
        OptionalShort opt = OptionalShort.of((short) 2023);
        optionalShortType.serializeTo(writer, opt, config);
        verify(writer).write((short) 2023);
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

        assertEquals("0", reviewFixes20260906_ser(optionalShortType, OptionalShort.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(optionalShortType, null, zero));
        assertEquals("0", reviewFixes20260906_ser(optionalShortType, OptionalShort.empty(), com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalShortType, OptionalShort.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalShortType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalShortType, OptionalShort.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalShortType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalShortType, OptionalShort.empty(), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("7", reviewFixes20260906_ser(optionalShortType, OptionalShort.of((short) 7), zero));
        assertEquals("7", reviewFixes20260906_ser(optionalShortType, OptionalShort.of((short) 7), null));
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

        assertTrue(optionalShortType.get(rs, 1).isPresent());
        assertEquals((short) 0, optionalShortType.get(rs, 1).get());
        assertTrue(optionalShortType.get(rs, "c").isPresent());
        assertEquals((short) 0, optionalShortType.get(rs, "c").get());
        assertThrows(NumberFormatException.class, () -> optionalShortType.get(rs, 2));
        assertEquals((short) 7, optionalShortType.get(rs, 3).get());
        assertTrue(optionalShortType.get(rs, 4).isEmpty());

        assertTrue(optionalShortType.valueOf("").isEmpty());
        assertTrue(optionalShortType.valueOf((String) null).isEmpty());
        assertEquals((short) 7, optionalShortType.valueOf("7").get());
    }

    // T5-03 (2026-09-06): out-of-range text throws ArithmeticException (Numbers.toXxx contract), not NumberFormatException.
    @Test
    public void reviewFixes20260906_valueOfOutOfRangeThrowsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> optionalShortType.valueOf("32768"));
        assertThrows(ArithmeticException.class, () -> optionalShortType.valueOf("-32769"));
        assertThrows(NumberFormatException.class, () -> optionalShortType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> optionalShortType.valueOf(" "));
        assertEquals((short) 32767, optionalShortType.valueOf("32767").get());
    }
}
