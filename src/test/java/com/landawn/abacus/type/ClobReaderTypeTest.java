package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ClobReaderTypeTest extends TestBase {

    private final ClobReaderType type = new ClobReaderType();

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException, IOException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("test content");

        when(rs.getClob(1)).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = type.get(rs, 1);

        final StringWriter output = new StringWriter();
        result.transferTo(output);
        assertEquals("test content", output.toString());
        result.close();
        verify(rs).getClob(1);
        verify(clob).getCharacterStream();
        verify(clob).free();
    }

    @Test
    public void testGet_ResultSet_Int_NullClob() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob(1)).thenReturn(null);

        Reader result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException, IOException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("column data");

        when(rs.getClob("columnName")).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = type.get(rs, "columnName");

        final StringWriter output = new StringWriter();
        result.transferTo(output);
        assertEquals("column data", output.toString());
        result.close();
        verify(rs).getClob("columnName");
        verify(clob).getCharacterStream();
        verify(clob).free();
    }

    @Test
    public void testGet_ResultSet_String_NullClob() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob("columnName")).thenReturn(null);

        Reader result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getClob("columnName");
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
        Reader reader = new StringReader("test data");

        type.set(stmt, 1, reader);

        verify(stmt).setClob(1, reader);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setClob(1, (Reader) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Reader reader = new StringReader("parameter data");

        type.set(stmt, "paramName", reader);

        verify(stmt).setClob("paramName", reader);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setClob("paramName", (Reader) null);
    }

    @Test
    public void testSet_PreparedStatement_Int_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Reader reader = new StringReader("sized data");

        type.set(stmt, 1, reader, 100);

        verify(stmt).setClob(1, reader, 100L);
    }

    @Test
    public void testSet_PreparedStatement_Int_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null, 100);

        verify(stmt).setClob(1, null, 100L);
    }

    @Test
    public void testSet_CallableStatement_String_Int() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Reader reader = new StringReader("sized parameter");

        type.set(stmt, "paramName", reader, 200);

        verify(stmt).setClob("paramName", reader, 200L);
    }

    @Test
    public void testSet_CallableStatement_String_Int_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null, 200);

        verify(stmt).setClob("paramName", null, 200L);
    }

    @Test
    public void testClob2Reader() throws SQLException, IOException {
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("clob content");
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = ClobReaderType.clobToReader(clob);

        final StringWriter output = new StringWriter();
        result.transferTo(output);
        assertEquals("clob content", output.toString());
        result.close();
        result.close();
        verify(clob).getCharacterStream();
        verify(clob, times(1)).free();
    }

    @Test
    public void testClob2Reader_Null() throws SQLException {
        Reader result = ClobReaderType.clobToReader(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testClob2Reader_MultipleInvocations() throws SQLException, IOException {
        Clob clob = mock(Clob.class);
        Reader reader1 = new StringReader("first");
        Reader reader2 = new StringReader("second");

        when(clob.getCharacterStream()).thenReturn(reader1, reader2);

        Reader result1 = ClobReaderType.clobToReader(clob);
        Reader result2 = ClobReaderType.clobToReader(clob);

        final StringWriter output1 = new StringWriter();
        final StringWriter output2 = new StringWriter();
        result1.transferTo(output1);
        result2.transferTo(output2);
        assertEquals("first", output1.toString());
        assertEquals("second", output2.toString());
        result1.close();
        result2.close();
        verify(clob, times(2)).getCharacterStream();
        verify(clob, times(2)).free();
    }

    @Test
    public void testClob2Reader_FreesClobWhenOpeningFails() throws SQLException {
        final Clob clob = mock(Clob.class);
        final SQLException openFailure = new SQLException("open");
        when(clob.getCharacterStream()).thenThrow(openFailure);

        assertSame(openFailure, assertThrows(SQLException.class, () -> ClobReaderType.clobToReader(clob)));
        verify(clob).free();
    }

    @Test
    public void testClob2Reader_FreesClobWhenDelegateCloseFails() throws SQLException, IOException {
        final Clob clob = mock(Clob.class);
        final Reader delegate = mock(Reader.class);
        final IOException closeFailure = new IOException("close");
        when(clob.getCharacterStream()).thenReturn(delegate);
        doThrow(closeFailure).when(delegate).close();

        final Reader result = ClobReaderType.clobToReader(clob);

        assertSame(closeFailure, assertThrows(IOException.class, result::close));
        verify(clob).free();
    }

}
