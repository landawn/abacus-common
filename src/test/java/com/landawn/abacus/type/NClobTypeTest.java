package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;

public class NClobTypeTest extends TestBase {

    private abstract static class TestNClob implements NClob {
        // Abstract test subtype is sufficient for verifying handler metadata.
    }

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
    public void testConcreteNClobSubtypeMetadata() {
        final NClobType subtype = new NClobType(TestNClob.class);

        assertEquals(TestNClob.class, subtype.javaType());
        assertEquals("TestNClob", subtype.name());
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
        Mockito.when(mockNClob.length()).thenReturn(Integer.MAX_VALUE + 1L);
        Mockito.doThrow(new SQLException("free failed")).when(mockNClob).free();

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> nClobType.stringOf(mockNClob));

        // free() failure must be suppressed, not replacing the original
        assertNotNull(thrown.getSuppressed());
        assertEquals(1, thrown.getSuppressed().length);
        Assertions.assertInstanceOf(UncheckedSQLException.class, thrown.getSuppressed()[0]);
    }

    @Test
    public void testStringOfPreservesPrimaryErrorWhenFreeFailsUnchecked() throws SQLException {
        AssertionError primaryFailure = new AssertionError();
        RuntimeException cleanupFailure = new IllegalStateException();
        Mockito.when(mockNClob.length()).thenThrow(primaryFailure);
        Mockito.doThrow(cleanupFailure).when(mockNClob).free();

        AssertionError thrown = assertThrows(AssertionError.class, () -> nClobType.stringOf(mockNClob));

        assertSame(primaryFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
    }

    // ---- review fixes 2026-09-06: T3-01 (empty lob) and T3-02 (valueOf(Object) must never free) ----

    /** A real (non-mock) NClob backed by the JDK's SerialClob, which rejects getSubString(1, 0) on an empty lob. */
    private static NClob nclobOver(final SerialClob delegate) {
        return (NClob) java.lang.reflect.Proxy.newProxyInstance(NClob.class.getClassLoader(), new Class<?>[] { NClob.class }, (proxy, method, args) -> {
            try {
                return method.invoke(delegate, args);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void reviewFixes20260906_stringOfZeroLengthLobReturnsEmptyWithoutReadingAndStillFrees() throws SQLException {
        Mockito.when(mockNClob.length()).thenReturn(0L);

        assertEquals("", nClobType.stringOf(mockNClob));

        Mockito.verify(mockNClob, Mockito.never()).getSubString(Mockito.anyLong(), Mockito.anyInt());
        Mockito.verify(mockNClob, Mockito.times(1)).free();
    }

    @Test
    public void reviewFixes20260906_stringOfEmptyRealLobReturnsEmptyAndFrees() throws SQLException {
        final SerialClob delegate = new SerialClob(new char[0]);

        assertEquals("", nClobType.stringOf(nclobOver(delegate)));

        // freed: SerialClob rejects every call after free()
        assertThrows(SerialException.class, delegate::length);
    }

    @Test
    public void reviewFixes20260906_stringOfNonEmptyRealLobStillReadsContent() throws SQLException {
        assertEquals("abc", nClobType.stringOf(nclobOver(new SerialClob("abc".toCharArray()))));
        assertEquals("中文😀", nClobType.stringOf(nclobOver(new SerialClob("中文😀".toCharArray()))));
    }

    @Test
    public void reviewFixes20260906_stringOfLengthOneStillGoesThroughGetSubString() throws SQLException {
        Mockito.when(mockNClob.length()).thenReturn(1L);
        Mockito.when(mockNClob.getSubString(1, 1)).thenReturn("x");

        assertEquals("x", nClobType.stringOf(mockNClob));

        Mockito.verify(mockNClob).getSubString(1, 1);
        Mockito.verify(mockNClob).free();
    }

    @Test
    public void reviewFixes20260906_valueOfObjectNClobReturnsSameInstanceWithoutTouchingIt() throws SQLException {
        assertSame(mockNClob, nClobType.valueOf((Object) mockNClob));

        Mockito.verify(mockNClob, Mockito.never()).free();
        Mockito.verify(mockNClob, Mockito.never()).length();
        Mockito.verify(mockNClob, Mockito.never()).getSubString(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectRealNClobRemainsUsableAfterwards() throws SQLException {
        final SerialClob delegate = new SerialClob("hello".toCharArray());
        final NClob nclob = nclobOver(delegate);

        assertSame(nclob, nClobType.valueOf((Object) nclob));
        assertEquals(5L, delegate.length());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectPlainClobIsRejectedWithoutBeingFreed() throws SQLException {
        final Clob plain = Mockito.mock(Clob.class);
        final SerialClob real = new SerialClob("ab".toCharArray());

        assertThrows(UnsupportedOperationException.class, () -> nClobType.valueOf((Object) plain));
        assertThrows(UnsupportedOperationException.class, () -> nClobType.valueOf((Object) real));

        Mockito.verify(plain, Mockito.never()).free();
        Mockito.verify(plain, Mockito.never()).length();
        assertEquals(2L, real.length());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectNonLobThrowsAndNullReturnsNull() {
        assertThrows(UnsupportedOperationException.class, () -> nClobType.valueOf((Object) "x"));
        assertThrows(UnsupportedOperationException.class, () -> nClobType.valueOf((Object) 42));
        assertNull(nClobType.valueOf((Object) null));
        // the String overload is unchanged
        assertThrows(UnsupportedOperationException.class, () -> nClobType.valueOf("x"));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectSubtypeHandlerAcceptsOnlyItsOwnClass() throws SQLException {
        final NClobType subtype = new NClobType(TestNClob.class);
        final TestNClob own = Mockito.mock(TestNClob.class);

        assertSame(own, subtype.valueOf((Object) own));
        assertThrows(UnsupportedOperationException.class, () -> subtype.valueOf((Object) mockNClob));

        Mockito.verify(own, Mockito.never()).free();
        Mockito.verify(mockNClob, Mockito.never()).free();
    }
}
