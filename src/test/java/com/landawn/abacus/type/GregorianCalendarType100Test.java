package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class GregorianCalendarType100Test extends TestBase {

    private GregorianCalendarType gregorianCalendarType;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        gregorianCalendarType = (GregorianCalendarType) createType(GregorianCalendar.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(GregorianCalendar.class, gregorianCalendarType.clazz());
    }

    @Test
    public void testValueOfObject() {
        assertNull(gregorianCalendarType.valueOf((Object) null));

        long timestamp = System.currentTimeMillis();
        GregorianCalendar result = gregorianCalendarType.valueOf((Object) timestamp);

        Date date = new Date();
        result = gregorianCalendarType.valueOf((Object) date);

        Calendar calendar = Calendar.getInstance();
        result = gregorianCalendarType.valueOf((Object) calendar);
    }

    @Test
    public void testValueOfString() {
        assertNull(gregorianCalendarType.valueOf((String) null));
        assertNull(gregorianCalendarType.valueOf(""));

    }

    @Test
    public void testValueOfCharArray() {
        assertNull(gregorianCalendarType.valueOf(null, 0, 0));

        char[] chars = new char[10];
        assertNull(gregorianCalendarType.valueOf(chars, 0, 0));

        String timestampStr = "1234567890123";
        char[] timestampChars = timestampStr.toCharArray();
        GregorianCalendar result = gregorianCalendarType.valueOf(timestampChars, 0, timestampChars.length);
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(resultSet.getTimestamp(1)).thenReturn(timestamp);

        GregorianCalendar result = gregorianCalendarType.get(resultSet, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(resultSet).getTimestamp(1);

        when(resultSet.getTimestamp(2)).thenReturn(null);
        assertNull(gregorianCalendarType.get(resultSet, 2));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(resultSet.getTimestamp("dateColumn")).thenReturn(timestamp);

        GregorianCalendar result = gregorianCalendarType.get(resultSet, "dateColumn");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(resultSet).getTimestamp("dateColumn");

        when(resultSet.getTimestamp("nullColumn")).thenReturn(null);
        assertNull(gregorianCalendarType.get(resultSet, "nullColumn"));
    }
}
