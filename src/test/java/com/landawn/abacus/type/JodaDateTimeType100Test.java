package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class JodaDateTimeType100Test extends TestBase {

    private JodaDateTimeType dateTimeType;

    @BeforeEach
    public void setUp() {
        dateTimeType = (JodaDateTimeType) createType("JodaDateTime");
    }

    @Test
    public void testClazz() {
        assertEquals(DateTime.class, dateTimeType.clazz());
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(dateTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        DateTime result = dateTimeType.valueOf((Object) millis);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date(1703502645123L);
        DateTime result = dateTimeType.valueOf((Object) date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getMillis());
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25T10:30:45Z";
        DateTime result = dateTimeType.valueOf((Object) str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(dateTimeType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(dateTimeType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        long before = System.currentTimeMillis();
        DateTime result = dateTimeType.valueOf("sysTime");
        long after = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getMillis() >= before);
        assertTrue(result.getMillis() <= after);
    }

    @Test
    public void testValueOf_String_ISO8601DateTime() {
        String str = "2023-12-25T10:30:45Z";
        DateTime result = dateTimeType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601Timestamp() {
        String str = "2023-12-25T10:30:45.123Z";
        DateTime result = dateTimeType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(dateTimeType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(dateTimeType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_NumericString() {
        long millis = 1703502645123L;
        char[] cbuf = String.valueOf(millis).toCharArray();
        DateTime result = dateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25T10:30:45Z";
        char[] cbuf = str.toCharArray();
        DateTime result = dateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        DateTime result = dateTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(dateTimeType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp("datetime_column")).thenReturn(timestamp);

        DateTime result = dateTimeType.get(rs, "datetime_column");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("datetime_column")).thenReturn(null);

        assertNull(dateTimeType.get(rs, "datetime_column"));
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        DateTime dateTime = new DateTime(1703502645123L);

        dateTimeType.set(stmt, 1, dateTime);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        dateTimeType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        DateTime dateTime = new DateTime(1703502645123L);

        dateTimeType.set(stmt, "param_name", dateTime);
        verify(stmt).setTimestamp(eq("param_name"), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        dateTimeType.set(stmt, "param_name", null);
        verify(stmt).setTimestamp("param_name", null);
    }
}
