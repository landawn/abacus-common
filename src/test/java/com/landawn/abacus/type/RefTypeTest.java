package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

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
    public void testIsSerializable() {
        assertFalse(refType.isSerializable());
    }

    @Test
    public void testStringOf() {
        Ref ref = mock(Ref.class);
        assertThrows(UnsupportedOperationException.class, () -> refType.stringOf(ref));
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

        assertSame(value, refType.valueOf((Object) value));
        assertNull(refType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf((Object) 42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> refType.valueOf("x"));
    }
}
