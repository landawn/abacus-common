package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.N;

public class SQLArrayTypeTest extends TestBase {

    private SQLArrayType sqlArrayType;

    @BeforeEach
    public void setUp() {
        sqlArrayType = (SQLArrayType) createType("SQLArray");
    }

    @Test
    public void testClazz() {
        assertEquals(Array.class, sqlArrayType.javaType());
    }

    @Test
    public void testImplementationHandlerPreservesJdbcValuesAndNulls() throws SQLException {
        final Array value = mock(Array.class);
        final Type<Array> implementationType = TypeFactory.getType(value.getClass());
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getArray(1)).thenReturn(value);
        when(rs.getArray("value")).thenReturn(value);

        assertEquals(value.getClass(), implementationType.javaType());
        assertSame(value, implementationType.get(rs, 1));
        assertSame(value, implementationType.get(rs, "value"));
        assertNull(implementationType.get(rs, 2));
        assertNull(implementationType.get(rs, "empty"));

        implementationType.set(statement, 1, value);
        implementationType.set(statement, 2, null);
        implementationType.set(callableStatement, "value", value);
        implementationType.set(callableStatement, "empty", null);

        verify(statement).setArray(1, value);
        verify(statement).setArray(2, null);
        verify(callableStatement).setObject("value", value);
        verify(callableStatement).setObject("empty", null);
        verifyNoInteractions(value);
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sqlArrayType.isSerializable());
    }

    static Stream<Arguments> stringOfValues() {
        return Stream.of(Arguments.of(new int[] { 1, 2 }, "[1, 2]"), Arguments.of(new Object[] { "a", 1, null }, "[\"a\", 1, null]"),
                Arguments.of(new Object[] { new int[] { 1, 2 }, new Object[] { "three", null } }, "[[1, 2], [\"three\", null]]"),
                Arguments.of(new Object[0], "[]"), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("stringOfValues")
    public void testStringOf(final Object payload, final String expected) throws SQLException {
        final Array value = mock(Array.class);
        when(value.getArray()).thenReturn(payload);
        final Type<Array> implementationType = TypeFactory.getType(value.getClass());

        assertEquals(expected, sqlArrayType.stringOf(value));
        assertEquals(expected, implementationType.stringOf(value));
        assertEquals(expected, N.stringOf(value));
        verify(value, times(3)).getArray();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testStringOfNull() {
        assertNull(sqlArrayType.stringOf(null));
    }

    @Test
    public void testStringOfWrapsSqlException() throws SQLException {
        final Array value = mock(Array.class);
        final SQLException failure = new SQLException("Cannot read JDBC value", "HY000", 17);
        when(value.getArray()).thenThrow(failure);
        final Type<Array> implementationType = TypeFactory.getType(value.getClass());

        for (final Type<Array> handler : java.util.List.of(sqlArrayType, implementationType)) {
            final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> handler.stringOf(value));
            assertSame(failure, thrown.getCause());
            assertEquals("HY000", thrown.getSQLState());
            assertEquals(17, thrown.getErrorCode());
        }
        verify(value, times(2)).getArray();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testValueOf() {
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.valueOf("test"));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Array array = mock(Array.class);
        when(rs.getArray(1)).thenReturn(array);

        assertEquals(array, sqlArrayType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Array array = mock(Array.class);
        when(rs.getArray("column")).thenReturn(array);

        assertEquals(array, sqlArrayType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Array array = mock(Array.class);

        sqlArrayType.set(stmt, 1, array);
        verify(stmt).setArray(1, array);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Array array = mock(Array.class);

        sqlArrayType.set(stmt, "param", array);
        verify(stmt).setObject("param", array);
    }

    // FINDING R04-6 (2026-09-08): BlobType/ClobType/NClobType were given a valueOf(Object) identity/null override;
    // SQLArrayType was not, so both null and a genuine Array fell through to AbstractType.valueOf(Object), which
    // renders the value with the handler of its own runtime class and feeds the text to the always-throwing
    // valueOf(String).
    @Test
    public void reviewFixes20260908_valueOfObjectReturnsSameInstanceOrNull() {
        final Array value = mock(Array.class);

        assertSame(value, sqlArrayType.valueOf(value));
        assertNull(sqlArrayType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.valueOf(42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> sqlArrayType.valueOf("x"));
    }

    // ---- deep review 2026-10-27 G135 begin ----
    // G135 (G019-02, doc fix): the Java array is written by the JSON serializer, which rejects a nested JDBC value (Struct,
    // java.sql.Array, Ref, SQLXML, RowId) with ParsingException "Unsupported class ...", also in a typed payload such as
    // Struct[]. Pinned as documented.
    @Test
    public void testStringOf_nestedJdbcValueThrowsParsingException() throws SQLException {
        for (final java.util.Map.Entry<String, Object> e : StructTypeTest.nestedJdbcValues().entrySet()) {
            final Object nested = e.getValue();
            // Typed payload, e.g. Struct[] or RowId[], as a driver returns for an ARRAY of such values.
            final Class<?> jdbcInterface = switch (e.getKey()) {
                case "Struct" -> java.sql.Struct.class;
                case "Array" -> Array.class;
                case "Ref" -> java.sql.Ref.class;
                case "SQLXML" -> java.sql.SQLXML.class;
                default -> java.sql.RowId.class;
            };
            final Object[] typedPayload = (Object[]) java.lang.reflect.Array.newInstance(jdbcInterface, 1);
            typedPayload[0] = nested;

            for (final Object payload : new Object[] { new Object[] { "a", nested }, typedPayload, new Object[] { java.util.List.of(nested) },
                    new Object[] { java.util.Map.of("key", nested) } }) {
                final Array value = mock(Array.class);
                when(value.getArray()).thenReturn(payload);
                final Type<Array> implementationType = TypeFactory.getType(value.getClass());

                final com.landawn.abacus.exception.ParsingException thrown = assertThrows(com.landawn.abacus.exception.ParsingException.class,
                        () -> sqlArrayType.stringOf(value), e.getKey());
                org.junit.jupiter.api.Assertions.assertTrue(thrown.getMessage().startsWith("Unsupported class: "), thrown.getMessage());
                assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> implementationType.stringOf(value), e.getKey());
                assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> N.stringOf(value), e.getKey());
            }
        }
    }
    // ---- deep review 2026-10-27 G135 end ----
}
