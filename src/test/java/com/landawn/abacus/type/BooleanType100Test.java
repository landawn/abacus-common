package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BooleanType100Test extends TestBase {

    private BooleanType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BooleanType) createType("Boolean");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<Boolean> result = type.clazz();
        assertEquals(Boolean.class, result);
    }

    @Test
    public void testIsPrimitiveWrapper() {
        boolean result = type.isPrimitiveWrapper();
        Assertions.assertTrue(result);
    }

    @Test
    public void testGet_ResultSet_Int_Boolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(Boolean.TRUE);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(null);

        Boolean result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_Number() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(1);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_ZeroNumber() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(0);

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_Int_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("true");

        Boolean result = type.get(rs, 1);

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject(1);
    }

    @Test
    public void testGet_ResultSet_String_Boolean() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(Boolean.FALSE);

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(null);

        Boolean result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Number() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn(0L);

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_StringValue() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("false");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.FALSE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_YCharacter() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("Y");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject("columnName");
    }

    @Test
    public void testGet_ResultSet_String_OneCharacter() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("columnName")).thenReturn("1");

        Boolean result = type.get(rs, "columnName");

        assertEquals(Boolean.TRUE, result);
        verify(rs).getObject("columnName");
    }
}
