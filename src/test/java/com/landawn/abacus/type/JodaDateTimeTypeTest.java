package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

public class JodaDateTimeTypeTest extends TestBase {

    private JodaDateTimeType dateTimeType;

    @BeforeEach
    public void setUp() {
        dateTimeType = (JodaDateTimeType) createType("JodaDateTime");
    }

    @Test
    public void testClazz() {
        assertEquals(DateTime.class, dateTimeType.javaType());
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(dateTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        DateTime result = dateTimeType.valueOf(millis);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date(1703502645123L);
        DateTime result = dateTimeType.valueOf(date);
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

    // --- review fixes 2026-09-06 (T9-01..T9-04) ---

    @Test
    public void reviewFixes20260906_T901_compactOffsetAndFractionShapesParseLikeTimestamp() {
        // 24-char text that is NOT "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" used to be forced through the fixed Joda formatter
        // (dispatch on length) and threw; it must reach the default parser like every sibling handler.
        final TimestampType tsType = new TimestampType();

        for (final String s : new String[] { "2024-01-01T00:00:00+0000", "2024-01-01T00:00:00-0800", "2024-01-01 00:00:00.1234", "2024-01-01T00:00:00.1234" }) {
            final DateTime result = dateTimeType.valueOf(s);
            assertEquals(tsType.valueOf(s).getTime(), result.getMillis(), s);
            assertEquals(DateTimeZone.getDefault(), result.getZone(), s);

            final char[] cbuf = s.toCharArray();
            assertEquals(tsType.valueOf(s).getTime(), dateTimeType.valueOf(cbuf, 0, cbuf.length).getMillis(), s);
        }

        assertEquals(1704067200000L, dateTimeType.valueOf("2024-01-01T00:00:00+0000").getMillis());
        assertEquals(1704096000000L, dateTimeType.valueOf("2024-01-01T00:00:00-0800").getMillis());
        assertEquals(1704096000123L, dateTimeType.valueOf("2024-01-01 00:00:00.1234").getMillis());
        assertEquals(1704096000123L, dateTimeType.valueOf("2024-01-01T00:00:00.1234").getMillis());
    }

    @Test
    public void reviewFixes20260906_T901_zShapesAndLeniencyKept() {
        // the fixed-formatter fast path still serves the 20/24-char 'Z' shapes, with Joda's lower-case leniency
        assertEquals(1704067200000L, dateTimeType.valueOf("2024-01-01T00:00:00Z").getMillis());
        assertEquals(1704067200123L, dateTimeType.valueOf("2024-01-01T00:00:00.123Z").getMillis());
        assertEquals(1704067200000L, dateTimeType.valueOf("2024-01-01t00:00:00z").getMillis());
        assertEquals(1704067200123L, dateTimeType.valueOf("2024-01-01T00:00:00.123z").getMillis());
        assertEquals(DateTimeZone.getDefault(), dateTimeType.valueOf("2024-01-01T00:00:00Z").getZone());
        assertEquals(new DateTime(1704067200000L), dateTimeType.valueOf("2024-01-01T00:00:00Z"));

        // 'Z'-suffixed text the fixed formatter rejects falls through and is still reported as IAE
        assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf("2024-13-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf("2024-01-01 00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf("2024-01-01T00:00:00 UTC"));

        assertNull(dateTimeType.valueOf(""));
        assertNull(dateTimeType.valueOf("null"));
        assertNull(dateTimeType.valueOf((String) null));
    }

    @Test
    public void reviewFixes20260906_T902_hexAndTypeSuffixRejectedOnBothOverloads() {
        // "0x1F4A0" was accepted as 128160 ms (Numbers.toLong grammar); the siblings reject it
        assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf("0x1F4A0"));
        assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf("0x1F4A0".toCharArray(), 0, 7));

        // a type suffix is rejected by BOTH overloads (the char[] fast path used to strip it)
        for (final String s : new String[] { "1700000000000L", "1700000000000d", "1700000000000f", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // plain and signed digits stay accepted on both overloads
        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, dateTimeType.valueOf(s).getMillis(), s);
            assertEquals(expected, dateTimeType.valueOf(s.toCharArray(), 0, s.length()).getMillis(), s);
        }
    }

    @Test
    public void reviewFixes20260906_T903_overflowingNumericTextIsIllegalArgument() {
        // used to escape as ArithmeticException("long overflow") from Numbers.toLong / parseLong(char[])
        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> dateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertEquals(Long.MAX_VALUE, dateTimeType.valueOf("9223372036854775807").getMillis());
        assertEquals(Long.MAX_VALUE, dateTimeType.valueOf("9223372036854775807".toCharArray(), 0, 19).getMillis());
    }

    @Test
    public void reviewFixes20260906_T904_valueOfObjectKeepsZoneAndChronology() {
        final DateTime kolkata = new DateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"));
        final DateTime result = dateTimeType.valueOf((Object) kolkata);
        assertEquals(kolkata, result);
        assertEquals(DateTimeZone.forID("Asia/Kolkata"), result.getZone());
        assertEquals(1700000000123L, result.getMillis());

        // non-ISO chronology: the instant used to shift by 543 years through the Buddhist-year text
        final DateTime buddhist = new DateTime(1700000000123L, BuddhistChronology.getInstance(DateTimeZone.forID("Asia/Bangkok")));
        final DateTime buddhistResult = dateTimeType.valueOf((Object) buddhist);
        assertEquals(buddhist, buddhistResult);
        assertEquals(buddhist.getChronology(), buddhistResult.getChronology());
        assertEquals(1700000000123L, buddhistResult.getMillis());

        // cross-type: a MutableDateTime source keeps its zone too
        final MutableDateTime mutableKolkata = new MutableDateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"));
        final DateTime fromMutable = dateTimeType.valueOf((Object) mutableKolkata);
        assertEquals(DateTimeZone.forID("Asia/Kolkata"), fromMutable.getZone());
        assertEquals(1700000000123L, fromMutable.getMillis());

        // a Joda Instant has no zone: stays on the default-zone path (not UTC)
        final DateTime fromInstant = dateTimeType.valueOf((Object) new Instant(1700000000123L));
        assertEquals(new DateTime(1700000000123L), fromInstant);
        assertEquals(DateTimeZone.getDefault(), fromInstant.getZone());

        // unchanged branches
        assertNull(dateTimeType.valueOf((Object) null));
        assertEquals(new DateTime(1700000000123L), dateTimeType.valueOf((Object) 1700000000123L));
        assertEquals(new DateTime(1700000000123L), dateTimeType.valueOf((Object) new Date(1700000000123L)));
    }
}
