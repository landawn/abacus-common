package com.landawn.abacus.type;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialClob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

public class ClobTypeTest extends TestBase {

    private final ClobType type = new ClobType();

    @Test
    public void testClazz() {
        Class<Clob> result = type.javaType();
        assertEquals(Clob.class, result);
    }

    @Test
    public void testConcreteClobSubclassMetadata() {
        final ClobType subtype = new ClobType(SerialClob.class);

        assertEquals(SerialClob.class, subtype.javaType());
        assertEquals("SerialClob", subtype.name());
    }

    @Test
    public void testStringOf() {
        Clob clob = mock(Clob.class);
        type.stringOf(clob);
        assertNotNull(clob);
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf((String) null));
    }

    @Test
    public void testValueOf_Null() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob expectedClob = mock(Clob.class);
        when(rs.getClob(1)).thenReturn(expectedClob);

        Clob result = type.get(rs, 1);

        assertEquals(expectedClob, result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob(1)).thenReturn(null);

        Clob result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob expectedClob = mock(Clob.class);
        when(rs.getClob("columnName")).thenReturn(expectedClob);

        Clob result = type.get(rs, "columnName");

        assertEquals(expectedClob, result);
        verify(rs).getClob("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob("columnName")).thenReturn(null);

        Clob result = type.get(rs, "columnName");

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
        Clob clob = mock(Clob.class);

        type.set(stmt, 1, clob);

        verify(stmt).setClob(1, clob);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setClob(1, (Clob) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Clob clob = mock(Clob.class);

        type.set(stmt, "paramName", clob);

        verify(stmt).setClob("paramName", clob);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setClob("paramName", (Clob) null);
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    /**
     * Bug fix: when getSubString() throws and free() also throws, the original
     * exception must not be lost.  The free() exception is attached as suppressed.
     */
    @Test
    public void testStringOf_getSubStringThrows_freeAlsoThrows_primaryExceptionNotLost() throws SQLException {
        final Clob clob = mock(Clob.class);
        final SQLException getSubStringException = new SQLException("getSubString failed");
        final SQLException freeException = new SQLException("free failed");

        when(clob.length()).thenReturn(5L);
        when(clob.getSubString(1, 5)).thenThrow(getSubStringException);
        doThrow(freeException).when(clob).free();

        final UncheckedSQLException thrown = Assertions.assertThrows(UncheckedSQLException.class, () -> type.stringOf(clob));

        // The primary exception (from getSubString) must be the one that propagates
        assertSame(getSubStringException, thrown.getCause());

        // The free() exception must be attached as a suppressed exception, not lost
        final Throwable[] suppressed = thrown.getSuppressed();
        Assertions.assertEquals(1, suppressed.length);
        Assertions.assertInstanceOf(UncheckedSQLException.class, suppressed[0]);
        assertSame(freeException, ((UncheckedSQLException) suppressed[0]).getCause());
    }

    /**
     * Bug fix: when getSubString() succeeds but free() throws, the free() exception
     * is propagated (there is no primary exception to suppress it against).
     */
    @Test
    public void testStringOf_getSubStringSucceeds_freeThrows_freeExceptionPropagates() throws SQLException {
        final Clob clob = mock(Clob.class);
        final SQLException freeException = new SQLException("free failed");

        when(clob.length()).thenReturn(5L);
        when(clob.getSubString(1, 5)).thenReturn("hello");
        doThrow(freeException).when(clob).free();

        final UncheckedSQLException thrown = Assertions.assertThrows(UncheckedSQLException.class, () -> type.stringOf(clob));
        assertSame(freeException, thrown.getCause());
    }

    /**
     * When getSubString() succeeds and free() also succeeds, the content is returned.
     */
    @Test
    public void testStringOf_successPath() throws SQLException {
        final Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(5L);
        when(clob.getSubString(1, 5)).thenReturn("hello");

        final String result = type.stringOf(clob);

        Assertions.assertEquals("hello", result);
        verify(clob).free();
    }

    /**
     * When the clob is too large (length > Integer.MAX_VALUE), an UnsupportedOperationException
     * is thrown.  The free() exception (if any) is attached as suppressed.
     */
    @Test
    public void testStringOf_clobTooLarge_throwsUnsupportedOperation() throws SQLException {
        final Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn((long) Integer.MAX_VALUE + 1L);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(clob));
        verify(clob).free();
    }

    @Test
    public void testMultipleOperations() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        Clob clob1 = mock(Clob.class);
        Clob clob2 = mock(Clob.class);

        when(rs.getClob(1)).thenReturn(clob1);
        when(rs.getClob(2)).thenReturn(clob2);

        Clob result1 = type.get(rs, 1);
        Clob result2 = type.get(rs, 2);

        assertEquals(clob1, result1);
        assertEquals(clob2, result2);

        type.set(stmt, 1, result1);
        type.set(stmt, 2, result2);

        verify(stmt).setClob(1, clob1);
        verify(stmt).setClob(2, clob2);
    }

}
