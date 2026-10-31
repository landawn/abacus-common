package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

public class StructTypeTest extends TestBase {

    private final Type<Struct> type = TypeFactory.getType(Struct.class);

    @Test
    public void testValueOfDoesNotAccessValues() {
        final Struct value = mock(Struct.class);
        final Object foreignValue = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("Foreign values must not be serialized");
            }
        };

        assertSame(value, type.valueOf(value));
        assertNull(type.valueOf((Object) null));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf(foreignValue));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf((Object) "value"));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("value"));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf((String) null));

        verifyNoInteractions(value);
    }

    static Stream<Arguments> stringOfValues() {
        return Stream.of(Arguments.of(new Object[] { "Main St", 123, null }, "[\"Main St\", 123, null]"),
                Arguments.of(new Object[] { new int[] { 1, 2 }, new Object[] { "three", null } }, "[[1, 2], [\"three\", null]]"),
                Arguments.of(new Object[0], "[]"), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("stringOfValues")
    public void testStringOf(final Object[] attributes, final String expected) throws SQLException {
        final Struct value = mock(Struct.class);
        when(value.getAttributes()).thenReturn(attributes);
        final Type<Struct> implementationType = TypeFactory.getType(value.getClass());

        // N.stringOf(value) must format the attributes rather than recurse into this handler indefinitely.
        assertEquals(expected, type.stringOf(value));
        assertEquals(expected, implementationType.stringOf(value));
        assertEquals(expected, N.stringOf(value));
        verify(value, times(3)).getAttributes();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfWrapsSqlException() throws SQLException {
        final Struct value = mock(Struct.class);
        final SQLException failure = new SQLException("Cannot read JDBC value", "HY000", 17);
        when(value.getAttributes()).thenThrow(failure);
        final Type<Struct> implementationType = TypeFactory.getType(value.getClass());

        for (final Type<Struct> handler : java.util.List.of(type, implementationType)) {
            final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> handler.stringOf(value));
            assertSame(failure, thrown.getCause());
            assertEquals("HY000", thrown.getSQLState());
            assertEquals(17, thrown.getErrorCode());
        }
        verify(value, times(2)).getAttributes();
        verifyNoMoreInteractions(value);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testJdbcTransferPreservesValuesAndNulls(final boolean implementationClass) throws SQLException {
        final Struct value = mock(Struct.class);
        final Type<Struct> jdbcType = implementationClass ? TypeFactory.getType(value.getClass()) : type;
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getObject(1)).thenReturn(value);
        when(rs.getObject("value")).thenReturn(value);

        assertSame(value, jdbcType.get(rs, 1));
        assertSame(value, jdbcType.get(rs, "value"));
        assertNull(jdbcType.get(rs, 2));
        assertNull(jdbcType.get(rs, "empty"));

        jdbcType.set(statement, 1, value);
        jdbcType.set(statement, 2, null);
        jdbcType.set(callableStatement, "value", value);
        jdbcType.set(callableStatement, "empty", null);

        verify(statement).setObject(1, value);
        verify(statement).setObject(2, null);
        verify(callableStatement).setObject("value", value);
        verify(callableStatement).setObject("empty", null);
        verifyNoInteractions(value);
    }

    @Test
    public void testJdbcErrorsPropagate() throws SQLException {
        final SQLException failure = new SQLException("JDBC failure");
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getObject(1)).thenThrow(failure);
        when(rs.getObject("value")).thenThrow(failure);
        doThrow(failure).when(statement).setObject(1, null);
        doThrow(failure).when(callableStatement).setObject("value", null);

        assertSame(failure, assertThrows(SQLException.class, () -> type.get(rs, 1)));
        assertSame(failure, assertThrows(SQLException.class, () -> type.get(rs, "value")));
        assertSame(failure, assertThrows(SQLException.class, () -> type.set(statement, 1, null)));
        assertSame(failure, assertThrows(SQLException.class, () -> type.set(callableStatement, "value", null)));
    }

    @Test
    public void testNonStructJdbcValuesAreRejected() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("mapped value");
        when(rs.getObject("value")).thenReturn("mapped value");

        assertThrows(ClassCastException.class, () -> type.get(rs, 1));
        assertThrows(ClassCastException.class, () -> type.get(rs, "value"));
    }

    @Test
    public void testNullJdbcArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> type.get(null, 1));
        assertThrows(NullPointerException.class, () -> type.get(null, "value"));
        assertThrows(NullPointerException.class, () -> type.set((PreparedStatement) null, 1, null));
        assertThrows(NullPointerException.class, () -> type.set((CallableStatement) null, "value", null));
    }

    // ---- deep review 2026-10-27 G135 begin ----
    // G135 (G019-02, doc fix): the attribute array is written by the JSON serializer, which rejects a nested JDBC value
    // (Struct, java.sql.Array, Ref, SQLXML, RowId) with ParsingException "Unsupported class ..." although each of them formats
    // on its own. Pinned as documented.
    static java.util.Map<String, Object> nestedJdbcValues() throws SQLException {
        final Struct struct = mock(Struct.class);
        when(struct.getAttributes()).thenReturn(new Object[] { "Main St", 123 });
        final java.sql.Array array = mock(java.sql.Array.class);
        when(array.getArray()).thenReturn(new int[] { 1, 2 });
        final java.sql.Ref ref = mock(java.sql.Ref.class);
        when(ref.getObject()).thenReturn("referenced");
        final java.sql.SQLXML sqlXml = mock(java.sql.SQLXML.class);
        when(sqlXml.getString()).thenReturn("<a/>");
        final java.sql.RowId rowId = mock(java.sql.RowId.class);

        final java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("Struct", struct);
        values.put("Array", array);
        values.put("Ref", ref);
        values.put("SQLXML", sqlXml);
        values.put("RowId", rowId);
        return values;
    }

    @Test
    public void testStringOf_nestedJdbcValueThrowsParsingException() throws SQLException {
        final java.util.Map<String, String> directText = java.util.Map.of("Struct", "[\"Main St\", 123]", "Array", "[1, 2]", "Ref", "referenced", "SQLXML",
                "<a/>");

        for (final java.util.Map.Entry<String, Object> e : nestedJdbcValues().entrySet()) {
            final Object nested = e.getValue();

            // Passed directly, the value is formatted by its own handler ...
            if (directText.containsKey(e.getKey())) {
                assertEquals(directText.get(e.getKey()), N.stringOf(nested), e.getKey());
            } else {
                assertEquals(nested.toString(), N.stringOf(nested), e.getKey());
            }

            // ... but as an attribute, or inside a collection or array attribute, it is rejected.
            for (final Object[] attributes : new Object[][] { { "Alice", nested }, { java.util.List.of(nested) }, { new Object[] { nested } } }) {
                final Struct value = mock(Struct.class);
                when(value.getAttributes()).thenReturn(attributes);
                final Type<Struct> implementationType = TypeFactory.getType(value.getClass());

                final com.landawn.abacus.exception.ParsingException thrown = assertThrows(com.landawn.abacus.exception.ParsingException.class,
                        () -> type.stringOf(value), e.getKey());
                org.junit.jupiter.api.Assertions.assertTrue(thrown.getMessage().startsWith("Unsupported class: "), thrown.getMessage());
                assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> implementationType.stringOf(value), e.getKey());
                assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> N.stringOf(value), e.getKey());
            }
        }

        // A Blob attribute is rejected the same way.
        final Struct withBlob = mock(Struct.class);
        when(withBlob.getAttributes()).thenReturn(new Object[] { mock(java.sql.Blob.class) });
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> type.stringOf(withBlob));
    }
    // ---- deep review 2026-10-27 G135 end ----
}
