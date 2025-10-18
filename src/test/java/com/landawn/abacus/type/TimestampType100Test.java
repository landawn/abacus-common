package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class TimestampType100Test extends TestBase {

    private TimestampType timestampType;

    @BeforeEach
    public void setUp() {
        timestampType = (TimestampType) createType("Timestamp");
    }

    @Test
    public void testClazz() {
        assertEquals(Timestamp.class, timestampType.clazz());
    }

    @Test
    public void testValueOfObject() {
        Long time = 123456789L;
        Timestamp result = timestampType.valueOf(time);
        assertNotNull(result);
        assertEquals(time.longValue(), result.getTime());

        Date date = new Date();
        result = timestampType.valueOf(date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime());

        assertNull(timestampType.valueOf((Object) null));

        result = timestampType.valueOf((Object) "2023-01-01 12:00:00");
        assertNotNull(result);
    }

    @Test
    public void testValueOfString() {
        Timestamp result = timestampType.valueOf("2023-01-01 12:00:00");
        assertNotNull(result);

        result = timestampType.valueOf("sysTime");
        assertNotNull(result);

        assertNull(timestampType.valueOf((String) null));
        assertNull(timestampType.valueOf(""));
    }

    @Test
    public void testValueOfCharArray() {
        char[] chars = "123456789".toCharArray();
        Timestamp result = timestampType.valueOf(chars, 0, chars.length);
        assertNotNull(result);
        assertEquals(123456789L, result.getTime());

        chars = "2023-01-01 12:00:00".toCharArray();
        result = timestampType.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(timestampType.valueOf(null, 0, 0));
        assertNull(timestampType.valueOf(new char[0], 0, 0));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        assertEquals(timestamp, timestampType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp("column")).thenReturn(timestamp);

        assertEquals(timestamp, timestampType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        timestampType.set(stmt, 1, timestamp);
        verify(stmt).setTimestamp(1, timestamp);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        timestampType.set(stmt, "param", timestamp);
        verify(stmt).setTimestamp("param", timestamp);
    }
}
