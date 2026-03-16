package com.landawn.abacus.type;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CalendarTypeTest extends TestBase {

    private final CalendarType type = new CalendarType();

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    @Test
    public void testClazz() {
        Class<Calendar> result = type.javaType();
        assertEquals(Calendar.class, result);
    }

    @Test
    public void testValueOf_Object_Number() {
        long timeMillis = System.currentTimeMillis();
        Calendar result = type.valueOf(timeMillis);

        Assertions.assertNotNull(result);
        assertEquals(timeMillis, result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date();
        Calendar result = type.valueOf(date);

        Assertions.assertNotNull(result);
        assertEquals(date.getTime(), result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_Calendar() {
        Calendar original = Calendar.getInstance();
        original.set(2023, Calendar.JANUARY, 15, 10, 30, 45);

        Calendar result = type.valueOf(original);

        Assertions.assertNotNull(result);
        Assertions.assertNotSame(original, result);
        assertEquals(original.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_String() {
        String dateString = "2023-06-15";
        Calendar result = type.valueOf((Object) dateString);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_Object_Null() {
        Calendar result = type.valueOf((Object) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_Null() {
        Calendar result = type.valueOf((String) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Calendar result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_SysTime() {
        long beforeTime = System.currentTimeMillis();
        Calendar result = type.valueOf("sysTime");
        long afterTime = System.currentTimeMillis();

        Assertions.assertNotNull(result);
        long resultTime = result.getTimeInMillis();
        Assertions.assertTrue(resultTime >= beforeTime);
        Assertions.assertTrue(resultTime <= afterTime);
    }

    @Test
    public void testValueOf_String_DateFormat() {
        String dateString = "2023-12-25 15:30:45";
        Calendar result = type.valueOf(dateString);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, result.get(Calendar.MONTH));
        assertEquals(25, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Calendar result = type.valueOf(null, 0, 0);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] chars = new char[0];
        Calendar result = type.valueOf(chars, 0, 0);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Timestamp() {
        long timestamp = 1234567890123L;
        char[] chars = String.valueOf(timestamp).toCharArray();

        Calendar result = type.valueOf(chars, 0, chars.length);

        Assertions.assertNotNull(result);
        assertEquals(timestamp, result.getTimeInMillis());
    }

    @Test
    public void testValueOf_CharArray_DateString() {
        String dateString = "2023-06-15 10:20:30";
        char[] chars = dateString.toCharArray();

        Calendar result = type.valueOf(chars, 0, chars.length);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_CharArray_PartialString() {
        String fullString = "prefix2023-06-15suffix";
        char[] chars = fullString.toCharArray();

        Calendar result = type.valueOf(chars, 6, 10);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        Calendar result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(rs).getTimestamp(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        Calendar result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getTimestamp(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1234567890000L);
        when(rs.getTimestamp("dateColumn")).thenReturn(timestamp);

        Calendar result = type.get(rs, "dateColumn");

        Assertions.assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(rs).getTimestamp("dateColumn");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("dateColumn")).thenReturn(null);

        Calendar result = type.get(rs, "dateColumn");

        Assertions.assertNull(result);
        verify(rs).getTimestamp("dateColumn");
    }

}
