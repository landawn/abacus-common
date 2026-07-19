package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BlobInputStreamTypeTest extends TestBase {

    private final BlobInputStreamType type = new BlobInputStreamType();

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException, IOException {
        ResultSet rs = mock(ResultSet.class);
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        when(rs.getBlob(1)).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, 1);

        assertArrayEquals(new byte[] { 1, 2, 3 }, result.readAllBytes());
        result.close();
        verify(rs).getBlob(1);
        verify(blob).getBinaryStream();
        verify(blob).free();
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
    public void testGet_ResultSet_String() throws SQLException, IOException {
        ResultSet rs = mock(ResultSet.class);
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });

        when(rs.getBlob("columnName")).thenReturn(blob);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, "columnName");

        assertArrayEquals(new byte[] { 1, 2, 3 }, result.readAllBytes());
        result.close();
        verify(rs).getBlob("columnName");
        verify(blob).getBinaryStream();
        verify(blob).free();
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
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
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
    public void testBlob2InputStream() throws SQLException, IOException {
        Blob blob = mock(Blob.class);
        InputStream expectedStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = BlobInputStreamType.blobToInputStream(blob);

        assertArrayEquals(new byte[] { 1, 2, 3 }, result.readAllBytes());
        result.close();
        result.close();
        verify(blob).getBinaryStream();
        verify(blob, times(1)).free();
    }

    @Test
    public void testBlob2InputStream_Null() throws SQLException {
        InputStream result = BlobInputStreamType.blobToInputStream(null);

        Assertions.assertNull(result);
    }

    @Test
    public void testBlob2InputStream_FreesBlobWhenOpeningFails() throws SQLException {
        final Blob blob = mock(Blob.class);
        final SQLException openFailure = new SQLException("open");
        final SQLException freeFailure = new SQLException("free");
        when(blob.getBinaryStream()).thenThrow(openFailure);
        doThrow(freeFailure).when(blob).free();

        final SQLException thrown = assertThrows(SQLException.class, () -> BlobInputStreamType.blobToInputStream(blob));

        assertSame(openFailure, thrown);
        assertSame(freeFailure, thrown.getSuppressed()[0]);
        verify(blob).free();
    }

    @Test
    public void testBlob2InputStream_FreesBlobWhenDelegateCloseFails() throws SQLException, IOException {
        final Blob blob = mock(Blob.class);
        final InputStream delegate = mock(InputStream.class);
        final IOException closeFailure = new IOException("close");
        final SQLException freeFailure = new SQLException("free");
        when(blob.getBinaryStream()).thenReturn(delegate);
        doThrow(closeFailure).when(delegate).close();
        doThrow(freeFailure).when(blob).free();

        final InputStream result = BlobInputStreamType.blobToInputStream(blob);
        final IOException thrown = assertThrows(IOException.class, result::close);

        assertSame(closeFailure, thrown);
        assertSame(freeFailure, thrown.getSuppressed()[0]);
        verify(blob).free();
    }

}
