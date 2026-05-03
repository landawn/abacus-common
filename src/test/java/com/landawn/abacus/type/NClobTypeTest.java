package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

public class NClobTypeTest extends TestBase {

    private NClobType nClobType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;
    private NClob mockNClob;

    @BeforeEach
    public void setUp() {
        nClobType = (NClobType) createType("NClob");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
        mockNClob = Mockito.mock(NClob.class);
    }

    @Test
    public void testClazz() {
        Class<NClob> clazz = nClobType.javaType();
        assertEquals(NClob.class, clazz);
    }

    @Test
    public void testStringOfThrowsException() {
        assertDoesNotThrow(() -> {
            nClobType.stringOf(mockNClob);
        });
    }

    @Test
    public void testValueOfThrowsException() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            nClobType.valueOf("test");
        });
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(mockNClob);

        NClob result = nClobType.get(mockResultSet, 1);
        Assertions.assertSame(mockNClob, result);
    }

    @Test
    public void testGetByIndexNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(null);

        NClob result = nClobType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(mockNClob);

        NClob result = nClobType.get(mockResultSet, "nclobColumn");
        Assertions.assertSame(mockNClob, result);
    }

    @Test
    public void testGetByLabelNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(null);

        NClob result = nClobType.get(mockResultSet, "nclobColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        nClobType.set(mockPreparedStatement, 1, mockNClob);
        Mockito.verify(mockPreparedStatement).setNClob(1, mockNClob);
    }

    @Test
    public void testSetPreparedStatementNull() throws SQLException {
        nClobType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNClob(1, (NClob) null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        nClobType.set(mockCallableStatement, "param", mockNClob);
        Mockito.verify(mockCallableStatement).setNClob("param", mockNClob);
    }

    @Test
    public void testSetCallableStatementNull() throws SQLException {
        nClobType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNClob("param", (NClob) null);
    }

    // Bug fix tests: finally block in stringOf must not suppress the original exception
    // when x.free() fails after a successful getSubString.

    @Test
    public void testStringOfNullReturnsNull() {
        assertNull(nClobType.stringOf(null));
    }

    @Test
    public void testStringOfSuccessCallsFree() throws SQLException {
        Mockito.when(mockNClob.length()).thenReturn(5L);
        Mockito.when(mockNClob.getSubString(1, 5)).thenReturn("hello");

        String result = nClobType.stringOf(mockNClob);

        assertEquals("hello", result);
        Mockito.verify(mockNClob).free();
    }

    /**
     * Bug: Before the fix, if x.free() threw a SQLException the new UncheckedSQLException was
     * thrown unconditionally from the finally block, which silently discarded the original result
     * (or the original exception) from the try block.
     *
     * After the fix: when the try block succeeds and then free() fails, the free exception is
     * thrown as a new exception (not suppressed). This test verifies that stringOf() still
     * propagates an exception in that scenario.
     */
    @Test
    public void testStringOfFreeThrowsSQLException_propagatesException() throws SQLException {
        Mockito.when(mockNClob.length()).thenReturn(3L);
        Mockito.when(mockNClob.getSubString(1, 3)).thenReturn("abc");
        Mockito.doThrow(new SQLException("free failed")).when(mockNClob).free();

        // After fix: free() failure when no primary exception → throws UncheckedSQLException.
        assertThrows(UncheckedSQLException.class, () -> nClobType.stringOf(mockNClob));
    }

    /**
     * Bug: Before the fix, if the try block threw (e.g., getSubString failed) AND free() also
     * threw, the finally block's new UncheckedSQLException would REPLACE the original exception.
     *
     * After the fix: the free() exception is added as a suppressed exception to the original
     * exception, so the original exception propagates and the free() failure is not lost.
     */
    @Test
    public void testStringOfGetSubStringFailsAndFreeAlsoFails_originalExceptionPropagates() throws SQLException {
        final SQLException originalSQLEx = new SQLException("getSubString failed");
        Mockito.when(mockNClob.length()).thenReturn(3L);
        Mockito.when(mockNClob.getSubString(1, 3)).thenThrow(originalSQLEx);
        Mockito.doThrow(new SQLException("free also failed")).when(mockNClob).free();

        UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> nClobType.stringOf(mockNClob));

        // The original SQL exception must be the cause
        assertEquals(originalSQLEx, thrown.getCause());

        // The free() failure must be recorded as suppressed, not replacing the original
        assertNotNull(thrown.getSuppressed());
        assertEquals(1, thrown.getSuppressed().length);
        Assertions.assertInstanceOf(UncheckedSQLException.class, thrown.getSuppressed()[0]);
    }

    /**
     * Bug: Before the fix, UnsupportedOperationException (NClob too large) thrown from the try
     * block would be replaced by the UncheckedSQLException from free() if free() also failed.
     *
     * After the fix: the UnsupportedOperationException propagates and the free() failure is
     * captured as a suppressed exception.
     */
    @Test
    public void testStringOfTooLargeNClobAndFreeAlsoFails_originalExceptionPropagates() throws SQLException {
        Mockito.when(mockNClob.length()).thenReturn((long) Integer.MAX_VALUE + 1L);
        Mockito.doThrow(new SQLException("free failed")).when(mockNClob).free();

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> nClobType.stringOf(mockNClob));

        // free() failure must be suppressed, not replacing the original
        assertNotNull(thrown.getSuppressed());
        assertEquals(1, thrown.getSuppressed().length);
        Assertions.assertInstanceOf(UncheckedSQLException.class, thrown.getSuppressed()[0]);
    }
}
