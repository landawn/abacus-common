package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import org.joda.time.chrono.BuddhistChronology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JodaMutableDateTimeTypeTest extends TestBase {

    private JodaMutableDateTimeType mutableDateTimeType;

    @BeforeEach
    public void setUp() {
        mutableDateTimeType = (JodaMutableDateTimeType) createType("JodaMutableDateTime");
    }

    @Test
    public void testClazz() {
        assertEquals(MutableDateTime.class, mutableDateTimeType.javaType());
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(mutableDateTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        MutableDateTime result = mutableDateTimeType.valueOf(millis);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date(1703502645123L);
        MutableDateTime result = mutableDateTimeType.valueOf(date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getMillis());
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25T10:30:45Z";
        MutableDateTime result = mutableDateTimeType.valueOf((Object) str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(mutableDateTimeType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(mutableDateTimeType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        long before = System.currentTimeMillis();
        MutableDateTime result = mutableDateTimeType.valueOf("sysTime");
        long after = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getMillis() >= before);
        assertTrue(result.getMillis() <= after);
    }

    @Test
    public void testValueOf_String_ISO8601DateTime() {
        String str = "2023-12-25T10:30:45Z";
        MutableDateTime result = mutableDateTimeType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601Timestamp() {
        String str = "2023-12-25T10:30:45.123Z";
        MutableDateTime result = mutableDateTimeType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(mutableDateTimeType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(mutableDateTimeType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_NumericString() {
        long millis = 1703502645123L;
        char[] cbuf = String.valueOf(millis).toCharArray();
        MutableDateTime result = mutableDateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25T10:30:45Z";
        char[] cbuf = str.toCharArray();
        MutableDateTime result = mutableDateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        MutableDateTime result = mutableDateTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(mutableDateTimeType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp("datetime_column")).thenReturn(timestamp);

        MutableDateTime result = mutableDateTimeType.get(rs, "datetime_column");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByLabel_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("datetime_column")).thenReturn(null);

        assertNull(mutableDateTimeType.get(rs, "datetime_column"));
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        MutableDateTime dateTime = new MutableDateTime(1703502645123L);

        mutableDateTimeType.set(stmt, 1, dateTime);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        mutableDateTimeType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        MutableDateTime dateTime = new MutableDateTime(1703502645123L);

        mutableDateTimeType.set(stmt, "param_name", dateTime);
        verify(stmt).setTimestamp(eq("param_name"), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        mutableDateTimeType.set(stmt, "param_name", null);
        verify(stmt).setTimestamp("param_name", null);
    }

    // --- review fixes 2026-09-06 (T9-01..T9-04) ---

    @Test
    public void reviewFixes20260906_T901_compactOffsetAndFractionShapesParseLikeTimestamp() {
        final TimestampType tsType = new TimestampType();

        for (final String s : new String[] { "2024-01-01T00:00:00+0000", "2024-01-01T00:00:00-0800", "2024-01-01 00:00:00.1234", "2024-01-01T00:00:00.1234" }) {
            final MutableDateTime result = mutableDateTimeType.valueOf(s);
            assertEquals(tsType.valueOf(s).getTime(), result.getMillis(), s);
            assertEquals(DateTimeZone.getDefault(), result.getZone(), s);
            assertEquals(tsType.valueOf(s).getTime(), mutableDateTimeType.valueOf(s.toCharArray(), 0, s.length()).getMillis(), s);
        }

        assertEquals(1704067200000L, mutableDateTimeType.valueOf("2024-01-01T00:00:00+0000").getMillis());
        assertEquals(1704096000000L, mutableDateTimeType.valueOf("2024-01-01T00:00:00-0800").getMillis());
        assertEquals(1704096000123L, mutableDateTimeType.valueOf("2024-01-01 00:00:00.1234").getMillis());
        assertEquals(1704096000123L, mutableDateTimeType.valueOf("2024-01-01T00:00:00.1234").getMillis());

        // keep-alive: 'Z' shapes (re-zoned to the default zone so equals() holds), lower-case leniency
        assertEquals(new MutableDateTime(1704067200000L), mutableDateTimeType.valueOf("2024-01-01T00:00:00Z"));
        assertEquals(new MutableDateTime(1704067200123L), mutableDateTimeType.valueOf("2024-01-01T00:00:00.123Z"));
        assertEquals(1704067200000L, mutableDateTimeType.valueOf("2024-01-01t00:00:00z").getMillis());
        assertEquals(1704067200123L, mutableDateTimeType.valueOf("2024-01-01T00:00:00.123z").getMillis());

        assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf("2024-13-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf("2024-01-01 00:00:00Z"));
        assertNull(mutableDateTimeType.valueOf(""));
        assertNull(mutableDateTimeType.valueOf("null"));
    }

    @Test
    public void reviewFixes20260906_T902_T903_numericGrammarAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf("0x1F4A0"));
        assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf("0x1F4A0".toCharArray(), 0, 7));

        for (final String s : new String[] { "1700000000000L", "1700000000000d", "12345L", "99999999999999999999", "9223372036854775808",
                "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> mutableDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, mutableDateTimeType.valueOf(s).getMillis(), s);
            assertEquals(expected, mutableDateTimeType.valueOf(s.toCharArray(), 0, s.length()).getMillis(), s);
        }
    }

    @Test
    public void reviewFixes20260906_T904_valueOfObjectCopiesZoneAndChronology() {
        final MutableDateTime kolkata = new MutableDateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"));
        final MutableDateTime result = mutableDateTimeType.valueOf((Object) kolkata);
        assertNotSame(kolkata, result);
        assertEquals(kolkata, result);
        assertEquals(DateTimeZone.forID("Asia/Kolkata"), result.getZone());
        assertEquals(1700000000123L, result.getMillis());

        // mutating the result must not touch the source (a copy, never the argument itself)
        result.addHours(1);
        assertEquals(1700000000123L, kolkata.getMillis());

        final DateTime buddhist = new DateTime(1700000000123L, BuddhistChronology.getInstance(DateTimeZone.forID("Asia/Bangkok")));
        final MutableDateTime buddhistResult = mutableDateTimeType.valueOf((Object) buddhist);
        assertEquals(buddhist.getChronology(), buddhistResult.getChronology());
        assertEquals(1700000000123L, buddhistResult.getMillis());

        // a DateTime source (cross-type) keeps its zone
        final DateTime dtKolkata = new DateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"));
        assertEquals(DateTimeZone.forID("Asia/Kolkata"), mutableDateTimeType.valueOf((Object) dtKolkata).getZone());

        // a Joda Instant has no zone: default-zone result, like the Number / Date branches
        final MutableDateTime fromInstant = mutableDateTimeType.valueOf((Object) new Instant(1700000000123L));
        assertEquals(new MutableDateTime(1700000000123L), fromInstant);
        assertEquals(DateTimeZone.getDefault(), fromInstant.getZone());

        assertNull(mutableDateTimeType.valueOf((Object) null));
        assertEquals(new MutableDateTime(1700000000123L), mutableDateTimeType.valueOf((Object) 1700000000123L));
        assertEquals(new MutableDateTime(1700000000123L), mutableDateTimeType.valueOf((Object) new Date(1700000000123L)));
    }
}
