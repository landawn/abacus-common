package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class JdkOptionalLongTypeTest extends TestBase {

    private JdkOptionalLongType optionalLongType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalLongType = (JdkOptionalLongType) createType("JdkOptionalLong");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalLong.class, optionalLongType.javaType());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalLongType.isComparable());
    }

    @Test
    public void testCompare() {
        assertEquals(0, optionalLongType.compare(OptionalLong.of(1L), OptionalLong.of(1L)));
        assertTrue(optionalLongType.compare(OptionalLong.of(1L), OptionalLong.of(2L)) < 0);
        assertTrue(optionalLongType.compare(OptionalLong.of(2L), OptionalLong.of(1L)) > 0);
        assertTrue(optionalLongType.compare(OptionalLong.empty(), OptionalLong.of(1L)) < 0);
        assertEquals(0, optionalLongType.compare(OptionalLong.empty(), OptionalLong.empty()));
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(optionalLongType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf_Present() {
        OptionalLong opt = OptionalLong.of(42L);
        assertEquals("42", optionalLongType.stringOf(opt));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalLongType.stringOf(OptionalLong.empty()));
    }

    @Test
    public void testValueOf_EmptyString() {
        OptionalLong result = optionalLongType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        OptionalLong result = optionalLongType.valueOf("42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testValueOf_NegativeString() {
        OptionalLong result = optionalLongType.valueOf("-42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(-42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42L);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalLong result = optionalLongType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("long_column")).thenReturn(null);

        OptionalLong result = optionalLongType.get(rs, "long_column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Long() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("long_column")).thenReturn(42L);

        OptionalLong result = optionalLongType.get(rs, "long_column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalLongType.set(stmt, 1, OptionalLong.empty());
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.set(stmt, 1, opt);
        verify(stmt).setLong(1, 42L);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalLongType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalLongType.set(stmt, "param_name", OptionalLong.empty());
        verify(stmt).setNull("param_name", java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.set(stmt, "param_name", opt);
        verify(stmt).setLong("param_name", 42L);
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalLongType.appendTo(sb, OptionalLong.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.appendTo(sb, opt);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        optionalLongType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        optionalLongType.serializeTo(characterWriter, OptionalLong.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Present() throws IOException {
        OptionalLong opt = OptionalLong.of(42L);

        optionalLongType.serializeTo(characterWriter, opt, null);
        verify(characterWriter).write(42L);
    }

    @Test
    public void testSerializeTo_WithConfig() throws IOException {
        OptionalLong opt = OptionalLong.of(42L);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        optionalLongType.serializeTo(characterWriter, opt, config);
        verify(characterWriter).write(42L);
    }

    @Test
    public void testSerializeTo_HonorsWriteLongAsString() throws IOException {
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            optionalLongType.serializeTo(writer, OptionalLong.of(9007199254740993L),
                    com.landawn.abacus.parser.JsonSerConfig.create().setWriteLongAsString(true));
            assertEquals("\"9007199254740993\"", writer.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @SuppressWarnings("unchecked")
    private static String reviewFixes20260906_ser(final Type<?> type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config)
            throws java.io.IOException {
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

        assertEquals("0", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(optionalLongType, null, zero));
        assertEquals("0", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(),
                com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalLongType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalLongType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(),
                com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("7", reviewFixes20260906_ser(optionalLongType, OptionalLong.of(7L), zero));
        assertEquals("7", reviewFixes20260906_ser(optionalLongType, OptionalLong.of(7L), null));

        // the substituted zero is quoted under writeLongAsString exactly as MutableLongType/LongType do
        final com.landawn.abacus.parser.JsonSerConfig zeroLas = com.landawn.abacus.parser.JsonSerConfig.create()
                .setWriteNullNumberAsZero(true)
                .setWriteLongAsString(true);
        assertEquals("\"0\"", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(), zeroLas));
        assertEquals("\"0\"", reviewFixes20260906_ser(optionalLongType, null, zeroLas));
        assertEquals("\"7\"", reviewFixes20260906_ser(optionalLongType, OptionalLong.of(7L), zeroLas));
        // XML config has quotation 0, so the zero stays bare
        assertEquals("0", reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(),
                com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true).setWriteLongAsString(true)));
        assertEquals("null",
                reviewFixes20260906_ser(optionalLongType, OptionalLong.empty(), com.landawn.abacus.parser.JsonSerConfig.create().setWriteLongAsString(true)));
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

        assertTrue(optionalLongType.get(rs, 1).isPresent());
        assertEquals(0L, optionalLongType.get(rs, 1).getAsLong());
        assertTrue(optionalLongType.get(rs, "c").isPresent());
        assertEquals(0L, optionalLongType.get(rs, "c").getAsLong());
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalLongType.get(rs, 2));
        assertEquals(7L, optionalLongType.get(rs, 3).getAsLong());
        assertTrue(optionalLongType.get(rs, 4).isEmpty());

        assertTrue(optionalLongType.valueOf("").isEmpty());
        assertTrue(optionalLongType.valueOf((String) null).isEmpty());
        assertEquals(7L, optionalLongType.valueOf("7").getAsLong());
    }

    // T5-03 (2026-09-06): out-of-range text throws ArithmeticException (Numbers.toXxx contract), not NumberFormatException.
    @Test
    public void reviewFixes20260906_valueOfOutOfRangeThrowsArithmeticException() {
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> optionalLongType.valueOf("9223372036854775808"));
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> optionalLongType.valueOf("-9223372036854775809"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalLongType.valueOf("abc"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalLongType.valueOf(" "));
        assertEquals(Long.MAX_VALUE, optionalLongType.valueOf("9223372036854775807").getAsLong());
    }
}
