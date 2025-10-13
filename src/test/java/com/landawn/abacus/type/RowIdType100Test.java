package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class RowIdType100Test extends TestBase {

    private RowIdType rowIdType;

    @BeforeEach
    public void setUp() {
        rowIdType = (RowIdType) createType("RowId");
    }

    @Test
    public void testClazz() {
        assertEquals(RowId.class, rowIdType.clazz());
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
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        RowId rowId = mock(RowId.class);
        when(rowId.toString()).thenReturn("rowid123");
        rowIdType.writeCharacter(writer, rowId, config);

        rowIdType.writeCharacter(writer, null, config);
    }
}
