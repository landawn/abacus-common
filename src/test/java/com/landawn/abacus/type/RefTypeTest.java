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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Ref;
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

public class RefTypeTest extends TestBase {

    private RefType refType;

    @BeforeEach
    public void setUp() {
        refType = (RefType) createType("Ref");
    }

    @Test
    public void testClazz() {
        assertEquals(Ref.class, refType.javaType());
    }

    @Test
    public void testImplementationHandlerPreservesJdbcValuesAndNulls() throws SQLException {
        final Ref value = mock(Ref.class);
        final Type<Ref> implementationType = TypeFactory.getType(value.getClass());
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getRef(1)).thenReturn(value);
        when(rs.getRef("value")).thenReturn(value);

        assertEquals(value.getClass(), implementationType.javaType());
        assertSame(value, implementationType.get(rs, 1));
        assertSame(value, implementationType.get(rs, "value"));
        assertNull(implementationType.get(rs, 2));
        assertNull(implementationType.get(rs, "empty"));

        implementationType.set(statement, 1, value);
        implementationType.set(statement, 2, null);
        implementationType.set(callableStatement, "value", value);
        implementationType.set(callableStatement, "empty", null);

        verify(statement).setRef(1, value);
        verify(statement).setRef(2, null);
        verify(callableStatement).setObject("value", value);
        verify(callableStatement).setObject("empty", null);
        verifyNoInteractions(value);
    }

    @Test
    public void testIsSerializable() {
        assertFalse(refType.isSerializable());
    }

    static Stream<Arguments> stringOfValues() {
        return Stream.of(Arguments.of("referenced value", "referenced value"), Arguments.of(42, "42"),
                Arguments.of(new Object[] { "a", 1, null }, "[\"a\", 1, null]"), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("stringOfValues")
    public void testStringOf(final Object payload, final String expected) throws SQLException {
        final Ref value = mock(Ref.class);
        when(value.getObject()).thenReturn(payload);
        final Type<Ref> implementationType = TypeFactory.getType(value.getClass());

        assertEquals(expected, refType.stringOf(value));
        assertEquals(expected, implementationType.stringOf(value));
        assertEquals(expected, N.stringOf(value));
        verify(value, times(3)).getObject();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testStringOfNull() {
        assertNull(refType.stringOf(null));
    }

    @Test
    public void testStringOfWrapsSqlException() throws SQLException {
        final Ref value = mock(Ref.class);
        final SQLException failure = new SQLException("Cannot read JDBC value", "HY000", 17);
        when(value.getObject()).thenThrow(failure);
        final Type<Ref> implementationType = TypeFactory.getType(value.getClass());

        for (final Type<Ref> handler : java.util.List.of(refType, implementationType)) {
            final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> handler.stringOf(value));
            assertSame(failure, thrown.getCause());
            assertEquals("HY000", thrown.getSQLState());
            assertEquals(17, thrown.getErrorCode());
        }
        verify(value, times(2)).getObject();
        verifyNoMoreInteractions(value);
    }

    @Test
    public void testValueOf() {
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf("test"));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Ref ref = mock(Ref.class);
        when(rs.getRef(1)).thenReturn(ref);

        assertEquals(ref, refType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Ref ref = mock(Ref.class);
        when(rs.getRef("column")).thenReturn(ref);

        assertEquals(ref, refType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Ref ref = mock(Ref.class);

        refType.set(stmt, 1, ref);
        verify(stmt).setRef(1, ref);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Ref ref = mock(Ref.class);

        refType.set(stmt, "param", ref);
        verify(stmt).setObject("param", ref);
    }

    // FINDING R04-6 (2026-09-08): BlobType/ClobType/NClobType were given a valueOf(Object) identity/null override;
    // RefType was not, so both null and a genuine Ref fell through to AbstractType.valueOf(Object), which
    // renders the value with the handler of its own runtime class and feeds the text to the always-throwing
    // valueOf(String).
    @Test
    public void reviewFixes20260908_valueOfObjectReturnsSameInstanceOrNull() {
        final Ref value = mock(Ref.class);

        assertSame(value, refType.valueOf(value));
        assertNull(refType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf(42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf("x"));
    }
}
