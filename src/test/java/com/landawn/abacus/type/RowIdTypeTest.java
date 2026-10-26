package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class RowIdTypeTest extends TestBase {

    private RowIdType rowIdType;

    @BeforeEach
    public void setUp() {
        rowIdType = (RowIdType) createType("RowId");
    }

    @Test
    public void testClazz() {
        assertEquals(RowId.class, rowIdType.javaType());
    }

    @Test
    public void testImplementationHandlerPreservesJdbcValuesAndNulls() throws SQLException {
        final RowId value = mock(RowId.class);
        final Type<RowId> implementationType = TypeFactory.getType(value.getClass());
        final ResultSet rs = mock(ResultSet.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final CallableStatement callableStatement = mock(CallableStatement.class);
        when(rs.getRowId(1)).thenReturn(value);
        when(rs.getRowId("value")).thenReturn(value);

        assertEquals(value.getClass(), implementationType.javaType());
        assertSame(value, implementationType.get(rs, 1));
        assertSame(value, implementationType.get(rs, "value"));
        assertNull(implementationType.get(rs, 2));
        assertNull(implementationType.get(rs, "empty"));

        implementationType.set(statement, 1, value);
        implementationType.set(statement, 2, null);
        implementationType.set(callableStatement, "value", value);
        implementationType.set(callableStatement, "empty", null);

        verify(statement).setRowId(1, value);
        verify(statement).setRowId(2, null);
        verify(callableStatement).setRowId("value", value);
        verify(callableStatement).setRowId("empty", null);
        verifyNoInteractions(value);
    }

    @Test
    public void testIsSerializable() {
        assertFalse(rowIdType.isSerializable());
    }

    @Test
    public void testStringOf() {
        RowId rowId = mock(RowId.class);
        when(rowId.toString()).thenReturn("rowid123");
        assertEquals("rowid123", rowIdType.stringOf(rowId));

        assertNull(rowIdType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertThrows(UnsupportedOperationException.class, () -> rowIdType.valueOf("test"));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        RowId rowId = mock(RowId.class);
        when(rs.getRowId(1)).thenReturn(rowId);

        assertEquals(rowId, rowIdType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        RowId rowId = mock(RowId.class);
        when(rs.getRowId("column")).thenReturn(rowId);

        assertEquals(rowId, rowIdType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        RowId rowId = mock(RowId.class);

        rowIdType.set(stmt, 1, rowId);
        verify(stmt).setRowId(1, rowId);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        RowId rowId = mock(RowId.class);

        rowIdType.set(stmt, "param", rowId);
        verify(stmt).setRowId("param", rowId);
    }

    @Test
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        RowId rowId = mock(RowId.class);
        when(rowId.toString()).thenReturn("rowid123");
        final Type<RowId> implementationType = TypeFactory.getType(rowId.getClass());
        assertEquals("rowid123", implementationType.stringOf(rowId));
        implementationType.serializeTo(writer, rowId, config);

        implementationType.serializeTo(writer, null, config);
        verify(writer).write("rowid123");
        verify(writer).write(NULL_CHAR_ARRAY);
        verifyNoInteractions(config);
    }

    // FINDING R04-6 (2026-09-08): BlobType/ClobType/NClobType were given a valueOf(Object) identity/null override;
    // RowIdType was not, so both null and a genuine RowId fell through to AbstractType.valueOf(Object), which
    // renders the value with the handler of its own runtime class and feeds the text to the always-throwing
    // valueOf(String).
    @Test
    public void reviewFixes20260908_valueOfObjectReturnsSameInstanceOrNull() {
        final RowId value = mock(RowId.class);

        assertSame(value, rowIdType.valueOf(value));
        assertNull(rowIdType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectRejectsForeignValues() {
        assertThrows(UnsupportedOperationException.class, () -> rowIdType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> rowIdType.valueOf(42));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> rowIdType.valueOf("x"));
    }
}
