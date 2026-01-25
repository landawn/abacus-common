package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BlobInputStreamType100Test extends TestBase {

    private BlobInputStreamType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BlobInputStreamType) createType("BlobInputStream");
        writer = createCharacterWriter();
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        when(rs.getBlob(1)).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, 1);

        assertEquals(expectedStream, result);
        verify(rs).getBlob(1);
        verify(blob).getBinaryStream();
    }

    @Test
    public void testGet_ResultSet_Int_NullBlob() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob(1)).thenReturn(null);

        InputStream result = type.get(rs, 1);

        assertNull(result);
        verify(rs).getBlob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        when(rs.getBlob("columnName")).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, "columnName");

        assertEquals(expectedStream, result);
        verify(rs).getBlob("columnName");
        verify(blob).getBinaryStream();
    }

    @Test
    public void testGet_ResultSet_String_NullBlob() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob("columnName")).thenReturn(null);

        InputStream result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getBlob("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        type.set(stmt, 1, stream);

        verify(stmt).setBlob(1, stream);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setBlob(1, (InputStream) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        type.set(stmt, "paramName", stream);

        verify(stmt).setBlob("paramName", stream);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setBlob("paramName", (InputStream) null);
    }

    @Test
    public void testSet_PreparedStatement_Int_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        type.set(stmt, 1, stream, 100);

        verify(stmt).setBlob(1, stream, 100L);
    }

    @Test
    public void testSet_PreparedStatement_Int_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null, 100);

        verify(stmt).setBlob(1, null, 100L);
    }

    @Test
    public void testSet_CallableStatement_String_Int() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        type.set(stmt, "paramName", stream, 100);

        verify(stmt).setBlob("paramName", stream, 100L);
    }

    @Test
    public void testSet_CallableStatement_String_Int_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null, 100);

        verify(stmt).setBlob("paramName", null, 100L);
    }

    @Test
    public void testBlob2InputStream() throws SQLException {
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = BlobInputStreamType.blobToInputStream(blob);

        assertEquals(expectedStream, result);
        verify(blob).getBinaryStream();
    }

    @Test
    public void testBlob2InputStream_Null() throws SQLException {
        InputStream result = BlobInputStreamType.blobToInputStream(null);

        Assertions.assertNull(result);
    }
}
