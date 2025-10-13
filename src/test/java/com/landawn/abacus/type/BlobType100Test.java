package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BlobType100Test extends TestBase {

    private BlobType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BlobType) createType("Blob");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<Blob> result = type.clazz();
        assertEquals(Blob.class, result);
    }

    @Test
    public void testStringOf() {
        Blob blob = mock(Blob.class);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(blob));
    }

    @Test
    public void testStringOf_Null() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(null));
    }

    @Test
    public void testValueOf() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf("test"));
    }

    @Test
    public void testValueOf_Null() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(null));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob expectedBlob = mock(Blob.class);
        when(rs.getBlob(1)).thenReturn(expectedBlob);

        Blob result = type.get(rs, 1);

        assertEquals(expectedBlob, result);
        verify(rs).getBlob(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob(1)).thenReturn(null);

        Blob result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getBlob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob expectedBlob = mock(Blob.class);
        when(rs.getBlob("columnName")).thenReturn(expectedBlob);

        Blob result = type.get(rs, "columnName");

        assertEquals(expectedBlob, result);
        verify(rs).getBlob("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob("columnName")).thenReturn(null);

        Blob result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getBlob("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Blob blob = mock(Blob.class);

        type.set(stmt, 1, blob);

        verify(stmt).setBlob(1, blob);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setBlob(1, (Blob) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Blob blob = mock(Blob.class);

        type.set(stmt, "paramName", blob);

        verify(stmt).setBlob("paramName", blob);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setBlob("paramName", (Blob) null);
    }
}
