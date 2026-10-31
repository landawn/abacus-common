package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class LocalTimeTypeTest extends TestBase {

    private LocalTimeType localTimeType;

    @BeforeEach
    public void setUp() {
        localTimeType = (LocalTimeType) createType("LocalTime");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalTime.class, localTimeType.javaType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(localTimeType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidLocalTime() {
        LocalTime time = LocalTime.of(10, 30, 45);
        String result = localTimeType.stringOf(time);
        assertNotNull(result);
        assertEquals("10:30:45", result);
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(localTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        LocalTime result = localTimeType.valueOf(millis);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "10:30:45";
        LocalTime result = localTimeType.valueOf((Object) str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(localTimeType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(localTimeType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        LocalTime before = LocalTime.now();
        LocalTime result = localTimeType.valueOf("sysTime");
        LocalTime after = LocalTime.now();

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.equals(before));
        assertTrue(result.isBefore(after) || result.equals(after));
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        LocalTime result = localTimeType.valueOf(String.valueOf(millis));
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601() {
        String str = "10:30:45";
        LocalTime result = localTimeType.valueOf(str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_ParsesLocalTimeToString() {
        // Every form produced by LocalTime.toString() must round-trip through valueOf.
        LocalTime[] values = { //
                LocalTime.of(10, 30), // minute precision, "10:30"
                LocalTime.of(10, 30, 45), // seconds, "10:30:45"
                LocalTime.of(10, 30, 45, 123000000), // millis
                LocalTime.of(10, 30, 45, 123456789), // nanos
                LocalTime.MIDNIGHT, // "00:00"
                LocalTime.NOON, //
                LocalTime.now() };

        for (LocalTime value : values) {
            String text = value.toString();
            LocalTime result = localTimeType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value, result, () -> "Mismatch for: " + text);
        }
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(localTimeType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(localTimeType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "10:30:45";
        char[] cbuf = str.toCharArray();
        LocalTime result = localTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsLocalTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalTime expected = LocalTime.of(10, 30, 45);
        when(rs.getObject(1, LocalTime.class)).thenReturn(expected);

        LocalTime result = localTimeType.get(rs, 1);
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = Time.valueOf("10:30:45");
        when(rs.getObject(1, LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime(1)).thenReturn(time);

        LocalTime result = localTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(time.toLocalTime(), result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime(1)).thenReturn(null);

        assertNull(localTimeType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByName_AsLocalTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalTime expected = LocalTime.of(10, 30, 45);
        when(rs.getObject("time_column", LocalTime.class)).thenReturn(expected);

        LocalTime result = localTimeType.get(rs, "time_column");
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByName_AsTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = Time.valueOf("10:30:45");
        when(rs.getObject("time_column", LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime("time_column")).thenReturn(time);

        LocalTime result = localTimeType.get(rs, "time_column");
        assertNotNull(result);
        assertEquals(time.toLocalTime(), result);
    }

    @Test
    public void testSet_PreparedStatement_AsLocalTime() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);

        localTimeType.set(stmt, 1, time);
        verify(stmt).setObject(1, time);
    }

    @Test
    public void testSet_PreparedStatement_AsTime() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject(1, time);

        localTimeType.set(stmt, 1, time);
        verify(stmt).setTime(eq(1), any(Time.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        localTimeType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_CallableStatement_AsLocalTime() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);

        localTimeType.set(stmt, "param_name", time);
        verify(stmt).setObject("param_name", time);
    }

    @Test
    public void testSet_CallableStatement_AsTime() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject("param_name", time);

        localTimeType.set(stmt, "param_name", time);
        verify(stmt).setTime(eq("param_name"), any(Time.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        localTimeType.set(stmt, "param_name", null);
        verify(stmt).setObject("param_name", null);
    }

    // --- review fixes 2026-09-06 (T10-02, T10-03, T10-04) ---

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L", "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807", "12345" }) {
            final LocalTime expected = LocalTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneId.systemDefault());
            assertEquals(expected, localTimeType.valueOf(s), s);
            assertEquals(expected, localTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf("0"));
    }

    @Test
    public void reviewFixes20260906_T1004_dateTimeFormatDoesNotApply() throws IOException {
        final LocalTime v = LocalTime.of(10, 30, 45, 123456789);

        for (final DateTimeFormat f : new DateTimeFormat[] { DateTimeFormat.LONG, DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.ISO_8601_TIMESTAMP }) {
            final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();
            localTimeType.serializeTo(w, v, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("\"10:30:45.123456789\"", w.toString(), f.toString());

            final BufferedJsonWriter wn = Objectory.createBufferedJsonWriter();
            localTimeType.serializeTo(wn, null, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("null", wn.toString(), f.toString());
        }
    }

    // FINDING R05-3 (2026-09-08): r9506 made ZonedDateTimeType/OffsetDateTimeType read a Calendar in the calendar's
    // own zone, but this handler still rebuilt from getTimeInMillis() in the JVM default zone - so the displayed
    // fields, which is all a LocalTime is, silently shifted by the zone offset.
    @Test
    public void reviewFixes20260908_calendarKeepsItsOwnZone() {
        // GMT+14:00 and GMT-12:00 are 26 h apart, so whatever the JVM default zone is, at least one of them is on a
        // different calendar day from it - which is what makes this test fail against the default-zone rebuild.
        for (final String zoneName : new String[] { "Asia/Tokyo", "America/Los_Angeles", "UTC", "GMT+05:30", "GMT+14:00", "GMT-12:00" }) {
            final java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zoneName);
            final java.util.GregorianCalendar cal = new java.util.GregorianCalendar(tz);
            cal.setTimeInMillis(1703502645123L);

            // GregorianCalendar.toZonedDateTime() is the JDK's own conversion, and it keeps the calendar's zone.
            assertEquals(cal.toZonedDateTime().toLocalTime(), localTimeType.valueOf(cal), zoneName);
            // ... and the java.time family now agrees with itself.
            assertEquals(createType(java.time.ZonedDateTime.class).valueOf(cal).toLocalTime(), localTimeType.valueOf(cal), zoneName);
        }
    }

    // ---- deep review 2026-10-27 G135 begin ----
    // G135 (G017-01b): Instant, OffsetDateTime and ZonedDateTime used the text route, whose ISO LocalTime parser rejects every value
    // of these types (the text starts with a date). An Instant is now read in the default zone like a Number or Date; an
    // OffsetDateTime/ZonedDateTime keeps the time it displays in its own offset or zone, like a Calendar.
    @Test
    public void testValueOfObject_instantIsReadInDefaultZoneLikeNumberAndDate() {
        final ZoneId zone = AbstractTemporalType.DEFAULT_ZONE_ID;

        for (final Instant instant : new Instant[] { Instant.EPOCH, Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2021-03-28T01:00:00Z"),
                Instant.parse("2021-10-31T00:30:00Z"), Instant.parse("2023-12-25T11:10:45.123Z"), Instant.parse("1969-12-31T23:59:59.999Z") }) {
            final LocalTime expected = LocalTime.ofInstant(instant, zone);
            assertEquals(expected, localTimeType.valueOf((Object) instant), instant.toString());
            assertEquals(localTimeType.valueOf((Object) instant.toEpochMilli()), localTimeType.valueOf((Object) instant), instant.toString());
            assertEquals(localTimeType.valueOf(java.util.Date.from(instant)), localTimeType.valueOf((Object) instant), instant.toString());
            assertEquals(expected, com.landawn.abacus.util.N.convert(instant, LocalTime.class), instant.toString());
        }

        // Nanosecond precision is kept, as for a Timestamp.
        final Instant nanos = Instant.parse("2023-12-25T11:10:45.123456789Z");
        assertEquals(LocalTime.ofInstant(nanos, zone), localTimeType.valueOf((Object) nanos));
        assertEquals(localTimeType.valueOf(java.sql.Timestamp.from(nanos)), localTimeType.valueOf((Object) nanos));
        assertEquals(123456789, localTimeType.valueOf((Object) nanos).getNano());

        // Only the time of day is taken, so even the extreme instants convert.
        assertEquals(LocalTime.ofInstant(Instant.MAX, zone), localTimeType.valueOf((Object) Instant.MAX));
        assertEquals(LocalTime.ofInstant(Instant.MIN, zone), localTimeType.valueOf((Object) Instant.MIN));
    }

    @Test
    public void testValueOfObject_offsetAndZonedDateTimeKeepTheirOwnLocalTime() {
        // Whole-minute value: toString() drops ":00", e.g. "2020-01-01T00:00+02:00".
        final java.time.OffsetDateTime wholeMinute = java.time.OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.ofHours(2));
        assertEquals(LocalTime.MIDNIGHT, localTimeType.valueOf((Object) wholeMinute));
        assertEquals(LocalTime.MIDNIGHT, com.landawn.abacus.util.N.convert(wholeMinute, LocalTime.class));

        // Region zone: toString() appends "[Europe/Paris]".
        final ZoneId paris = ZoneId.of("Europe/Paris");
        final java.time.ZonedDateTime regionZone = java.time.ZonedDateTime.of(2020, 1, 1, 0, 0, 1, 0, paris);
        assertEquals(LocalTime.of(0, 0, 1), localTimeType.valueOf((Object) regionZone));
        assertEquals(LocalTime.of(0, 0, 1), com.landawn.abacus.util.N.convert(regionZone, LocalTime.class));
        assertEquals(LocalTime.of(10, 30), localTimeType.valueOf((Object) java.time.ZonedDateTime.of(2020, 7, 1, 10, 30, 0, 0, paris)));

        // Nanosecond precision, negative offset and UTC.
        final java.time.OffsetDateTime withNanos = java.time.OffsetDateTime.of(2020, 1, 1, 0, 0, 1, 123456789, java.time.ZoneOffset.ofHours(-5));
        assertEquals(LocalTime.of(0, 0, 1, 123456789), localTimeType.valueOf((Object) withNanos));
        final java.time.ZonedDateTime utc = java.time.ZonedDateTime.of(2021, 6, 30, 23, 59, 59, 999, java.time.ZoneOffset.UTC);
        assertEquals(LocalTime.of(23, 59, 59, 999), localTimeType.valueOf((Object) utc));

        // The same instant at +14:00 and -12:00 shows two different times (2 h apart on the clock); each value keeps its own, so at
        // least one of them differs from the time in the default zone - the result is not the instant re-read in the default zone.
        final Instant instant = Instant.parse("2023-12-25T11:10:45.123Z");
        boolean differsFromDefaultZone = false;

        for (final java.time.ZoneOffset offset : new java.time.ZoneOffset[] { java.time.ZoneOffset.ofHours(14), java.time.ZoneOffset.ofHours(-12) }) {
            final java.time.OffsetDateTime odt = instant.atOffset(offset);
            final java.time.ZonedDateTime zdt = instant.atZone(offset);
            assertEquals(odt.toLocalTime(), localTimeType.valueOf((Object) odt), offset.toString());
            assertEquals(zdt.toLocalTime(), localTimeType.valueOf((Object) zdt), offset.toString());
            differsFromDefaultZone |= !odt.toLocalTime().equals(LocalTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID));
        }

        assertEquals(LocalTime.of(1, 10, 45, 123000000), localTimeType.valueOf((Object) instant.atOffset(java.time.ZoneOffset.ofHours(14))));
        assertEquals(LocalTime.of(23, 10, 45, 123000000), localTimeType.valueOf((Object) instant.atOffset(java.time.ZoneOffset.ofHours(-12))));
        assertTrue(differsFromDefaultZone);

        // Same rule as the Calendar branch: a Calendar and its ZonedDateTime give the same LocalTime.
        final java.util.GregorianCalendar cal = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(1703502645123L);
        assertEquals(localTimeType.valueOf(cal), localTimeType.valueOf((Object) cal.toZonedDateTime()));
    }

    @Test
    public void testValueOfObject_zonedDateTimeAtDstEdgesKeepsDisplayedTime() {
        final ZoneId paris = ZoneId.of("Europe/Paris");

        // Spring gap: 02:30 does not exist on 2021-03-28 and ZonedDateTime resolves it to 03:30+02:00.
        assertEquals(LocalTime.of(3, 30), localTimeType.valueOf((Object) java.time.ZonedDateTime.of(2021, 3, 28, 2, 30, 0, 0, paris)));

        // Autumn overlap: 02:30 occurs twice on 2021-10-31; both instants display (and convert to) 02:30.
        final java.time.ZonedDateTime earlier = java.time.ZonedDateTime.of(2021, 10, 31, 2, 30, 0, 0, paris).withEarlierOffsetAtOverlap();
        final java.time.ZonedDateTime later = earlier.withLaterOffsetAtOverlap();
        assertEquals(3600, later.toEpochSecond() - earlier.toEpochSecond());
        assertEquals(LocalTime.of(2, 30), localTimeType.valueOf((Object) earlier));
        assertEquals(LocalTime.of(2, 30), localTimeType.valueOf((Object) later));
        assertEquals(LocalTime.of(2, 30), localTimeType.valueOf((Object) later.toOffsetDateTime()));
    }

    // Other java.time values keep the text route and its result (pinned, unchanged).
    @Test
    public void testValueOfObject_otherTemporalValuesKeepTextRoute() {
        assertEquals(LocalTime.of(10, 30, 45, 1), localTimeType.valueOf((Object) LocalTime.of(10, 30, 45, 1)));
        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf((Object) java.time.LocalDateTime.of(2021, 1, 1, 10, 30)));
        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf((Object) java.time.LocalDate.of(2021, 1, 1)));
        assertThrows(DateTimeParseException.class,
                () -> localTimeType.valueOf((Object) java.time.OffsetTime.of(10, 30, 0, 0, java.time.ZoneOffset.ofHours(2))));
    }
    // ---- deep review 2026-10-27 G135 end ----
}
