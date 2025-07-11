package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;

public class DateType100Test extends TestBase {

    private DateType dateType;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dateType = (DateType) createType(Date.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Date.class, dateType.clazz());
    }

    @Test
    public void testValueOfObject() {
        // Test with Number
        long timestamp = System.currentTimeMillis();
        Date date = dateType.valueOf((Object) timestamp);
        assertNotNull(date);
        assertEquals(timestamp, date.getTime());

        // Test with java.util.Date
        java.util.Date utilDate = new java.util.Date();
        Date sqlDate = dateType.valueOf((Object) utilDate);
        assertNotNull(sqlDate);
        assertEquals(utilDate.getTime(), sqlDate.getTime());

        // Test with null
        assertNull(dateType.valueOf((Object) null));
    }

    @Test
    public void testValueOfString() {
        // Test with null and empty string
        assertNull(dateType.valueOf((String) null));
        assertNull(dateType.valueOf(""));

        // Test with "sysTime" would require mocking Dates.currentDate()
        // Test with date string would require mocking Dates.parseDate()
    }

    @Test
    public void testValueOfCharArray() {
        // Test with null
        assertNull(dateType.valueOf(null, 0, 0));

        // Test with empty length
        char[] chars = new char[10];
        assertNull(dateType.valueOf(chars, 0, 0));

        // Test with numeric string
        String timestampStr = "1234567890123";
        char[] timestampChars = timestampStr.toCharArray();
        Date date = dateType.valueOf(timestampChars, 0, timestampChars.length);
        // Result depends on Dates.createDate() implementation
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        Date expectedDate = new Date(System.currentTimeMillis());
        when(resultSet.getDate(1)).thenReturn(expectedDate);

        Date result = dateType.get(resultSet, 1);
        assertEquals(expectedDate, result);
        verify(resultSet).getDate(1);
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        Date expectedDate = new Date(System.currentTimeMillis());
        when(resultSet.getDate("dateColumn")).thenReturn(expectedDate);

        Date result = dateType.get(resultSet, "dateColumn");
        assertEquals(expectedDate, result);
        verify(resultSet).getDate("dateColumn");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        dateType.set(preparedStatement, 1, date);
        verify(preparedStatement).setDate(1, date);

        dateType.set(preparedStatement, 2, null);
        verify(preparedStatement).setDate(2, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        dateType.set(callableStatement, "dateParam", date);
        verify(callableStatement).setDate("dateParam", date);

        dateType.set(callableStatement, "nullParam", null);
        verify(callableStatement).setDate("nullParam", null);
    }
}
