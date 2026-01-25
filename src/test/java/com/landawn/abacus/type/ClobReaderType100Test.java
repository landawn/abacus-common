package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Clob;
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
public class ClobReaderType100Test extends TestBase {

    private ClobReaderType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ClobReaderType) createType("ClobReader");
        writer = createCharacterWriter();
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("test content");

        when(rs.getClob(1)).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = type.get(rs, 1);

        assertEquals(expectedReader, result);
        verify(rs).getClob(1);
        verify(clob).getCharacterStream();
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
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("column data");

        when(rs.getClob("columnName")).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = type.get(rs, "columnName");

        assertEquals(expectedReader, result);
        verify(rs).getClob("columnName");
        verify(clob).getCharacterStream();
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
    public void testClob2Reader() throws SQLException {
        Clob clob = mock(Clob.class);
        Reader expectedReader = new StringReader("clob content");
        when(clob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = ClobReaderType.clobToReader(clob);

        assertEquals(expectedReader, result);
        verify(clob).getCharacterStream();
    }

    @Test
    public void testClob2Reader_Null() throws SQLException {
        Reader result = ClobReaderType.clobToReader(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testClob2Reader_MultipleInvocations() throws SQLException {
        Clob clob = mock(Clob.class);
        Reader reader1 = new StringReader("first");
        Reader reader2 = new StringReader("second");

        when(clob.getCharacterStream()).thenReturn(reader1, reader2);

        Reader result1 = ClobReaderType.clobToReader(clob);
        Reader result2 = ClobReaderType.clobToReader(clob);

        assertEquals(reader1, result1);
        assertEquals(reader2, result2);
        verify(clob, times(2)).getCharacterStream();
    }
}
