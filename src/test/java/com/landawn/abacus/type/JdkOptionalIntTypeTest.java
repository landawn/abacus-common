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
import java.util.OptionalInt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class JdkOptionalIntTypeTest extends TestBase {

    private JdkOptionalIntType optionalIntType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        optionalIntType = (JdkOptionalIntType) createType("JdkOptionalInt");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(OptionalInt.class, optionalIntType.javaType());
    }

    @Test
    public void testIsComparable() {
        assertTrue(optionalIntType.isComparable());
    }

    @Test
    public void testCompare() {
        assertEquals(0, optionalIntType.compare(OptionalInt.of(1), OptionalInt.of(1)));
        assertTrue(optionalIntType.compare(OptionalInt.of(1), OptionalInt.of(2)) < 0);
        assertTrue(optionalIntType.compare(OptionalInt.of(2), OptionalInt.of(1)) > 0);
        assertTrue(optionalIntType.compare(OptionalInt.empty(), OptionalInt.of(1)) < 0);
        assertEquals(0, optionalIntType.compare(OptionalInt.empty(), OptionalInt.empty()));
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(optionalIntType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf_Present() {
        OptionalInt opt = OptionalInt.of(42);
        assertEquals("42", optionalIntType.stringOf(opt));
    }

    @Test
    public void testStringOf_Empty() {
        assertNull(optionalIntType.stringOf(OptionalInt.empty()));
    }

    @Test
    public void testValueOf_EmptyString() {
        OptionalInt result = optionalIntType.valueOf("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_ValidString() {
        OptionalInt result = optionalIntType.valueOf("42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testValueOf_NegativeString() {
        OptionalInt result = optionalIntType.valueOf("-42");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(-42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByIndex_OtherNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42L);

        OptionalInt result = optionalIntType.get(rs, 1);
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("int_column")).thenReturn(null);

        OptionalInt result = optionalIntType.get(rs, "int_column");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Integer() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("int_column")).thenReturn(42);

        OptionalInt result = optionalIntType.get(rs, "int_column");
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testSet_PreparedStatement_Empty() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        optionalIntType.set(stmt, 1, OptionalInt.empty());
        verify(stmt).setNull(1, java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_PreparedStatement_Present() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.set(stmt, 1, opt);
        verify(stmt).setInt(1, 42);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalIntType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_CallableStatement_Empty() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        optionalIntType.set(stmt, "param_name", OptionalInt.empty());
        verify(stmt).setNull("param_name", java.sql.Types.INTEGER);
    }

    @Test
    public void testSet_CallableStatement_Present() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.set(stmt, "param_name", opt);
        verify(stmt).setInt("param_name", 42);
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringBuilder sb = new StringBuilder();

        optionalIntType.appendTo(sb, OptionalInt.empty());
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_Present() throws IOException {
        StringBuilder sb = new StringBuilder();
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.appendTo(sb, opt);
        assertEquals("42", sb.toString());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        optionalIntType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        optionalIntType.serializeTo(characterWriter, OptionalInt.empty(), null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_Present() throws IOException {
        OptionalInt opt = OptionalInt.of(42);

        optionalIntType.serializeTo(characterWriter, opt, null);
        verify(characterWriter).writeInt(42);
    }

    @Test
    public void testSerializeTo_WithConfig() throws IOException {
        OptionalInt opt = OptionalInt.of(42);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        optionalIntType.serializeTo(characterWriter, opt, config);
        verify(characterWriter).writeInt(42);
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

        assertEquals("0", reviewFixes20260906_ser(optionalIntType, OptionalInt.empty(), zero));
        assertEquals("0", reviewFixes20260906_ser(optionalIntType, null, zero));
        assertEquals("0", reviewFixes20260906_ser(optionalIntType, OptionalInt.empty(), com.landawn.abacus.parser.XmlSerConfig.create().setWriteNullNumberAsZero(true)));
        assertEquals("null", reviewFixes20260906_ser(optionalIntType, OptionalInt.empty(), com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalIntType, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", reviewFixes20260906_ser(optionalIntType, OptionalInt.empty(), null));
        assertEquals("null", reviewFixes20260906_ser(optionalIntType, null, null));
        assertEquals("null", reviewFixes20260906_ser(optionalIntType, OptionalInt.empty(), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullBooleanAsFalse(true)));
        assertEquals("7", reviewFixes20260906_ser(optionalIntType, OptionalInt.of(7), zero));
        assertEquals("7", reviewFixes20260906_ser(optionalIntType, OptionalInt.of(7), null));
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

        assertTrue(optionalIntType.get(rs, 1).isPresent());
        assertEquals(0, optionalIntType.get(rs, 1).getAsInt());
        assertTrue(optionalIntType.get(rs, "c").isPresent());
        assertEquals(0, optionalIntType.get(rs, "c").getAsInt());
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalIntType.get(rs, 2));
        assertEquals(7, optionalIntType.get(rs, 3).getAsInt());
        assertTrue(optionalIntType.get(rs, 4).isEmpty());

        assertTrue(optionalIntType.valueOf("").isEmpty());
        assertTrue(optionalIntType.valueOf((String) null).isEmpty());
        assertEquals(7, optionalIntType.valueOf("7").getAsInt());
    }

    // T5-03 (2026-09-06): out-of-range text throws ArithmeticException (Numbers.toXxx contract), not NumberFormatException.
    @Test
    public void reviewFixes20260906_valueOfOutOfRangeThrowsArithmeticException() {
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> optionalIntType.valueOf("2147483648"));
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> optionalIntType.valueOf("-2147483649"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalIntType.valueOf("abc"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> optionalIntType.valueOf(" "));
        assertEquals(Integer.MAX_VALUE, optionalIntType.valueOf("2147483647").getAsInt());
    }
}
