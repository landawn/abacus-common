package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class JUDateType100Test extends TestBase {

    private JUDateType dateType;

    @BeforeEach
    public void setUp() {
        dateType = (JUDateType) createType("JUDate");
    }

    @Test
    public void testClazz() {
        assertEquals(Date.class, dateType.clazz());
    }

    @Test
    public void testDeclaringName() {
        assertEquals("java.util.Date", dateType.declaringName());
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(dateType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        Date result = dateType.valueOf((Object) millis);
        assertNotNull(result);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date(1703502645123L);
        Date result = dateType.valueOf((Object) date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime());
        assertNotSame(date, result);
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25 10:30:45";
        Date result = dateType.valueOf((Object) str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(dateType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(dateType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        long before = System.currentTimeMillis();
        Date result = dateType.valueOf("sysTime");
        long after = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getTime() >= before);
        assertTrue(result.getTime() <= after);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(dateType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(dateType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_NumericString() {
        long millis = 1703502645123L;
        char[] cbuf = String.valueOf(millis).toCharArray();
        Date result = dateType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25 10:30:45";
        char[] cbuf = str.toCharArray();
        Date result = dateType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        Date result = dateType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTime());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(dateType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp("date_column")).thenReturn(timestamp);

        Date result = dateType.get(rs, "date_column");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTime());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("date_column")).thenReturn(null);

        assertNull(dateType.get(rs, "date_column"));
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Date date = new Date(1703502645123L);

        dateType.set(stmt, 1, date);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_PreparedStatement_WithTimestamp() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Timestamp timestamp = new Timestamp(1703502645123L);

        dateType.set(stmt, 1, timestamp);
        verify(stmt).setTimestamp(1, timestamp);
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        dateType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Date date = new Date(1703502645123L);

        dateType.set(stmt, "param_name", date);
        verify(stmt).setTimestamp(eq("param_name"), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement_WithTimestamp() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Timestamp timestamp = new Timestamp(1703502645123L);

        dateType.set(stmt, "param_name", timestamp);
        verify(stmt).setTimestamp("param_name", timestamp);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        dateType.set(stmt, "param_name", null);
        verify(stmt).setTimestamp("param_name", null);
    }
}
