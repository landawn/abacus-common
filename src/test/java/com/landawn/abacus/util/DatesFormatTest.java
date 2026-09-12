package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesFormatTest extends TestBase {

    // ===== formatCurrentLocalDate / formatCurrentLocalDateTime =====

    @Test
    public void testFormatCurrentLocalDate_matchesPattern() {
        String formatted = Dates.formatCurrentLocalDate();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatCurrentLocalDateTime_matchesPattern() {
        String formatted = Dates.formatCurrentLocalDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    // ===== formatCurrentDateTime =====

    @Test
    public void testFormatCurrentDateTime() {
        String formatted = Dates.formatCurrentDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    // ===== formatCurrentTimestamp =====

    @Test
    public void testFormatCurrentTimestamp_matchesPattern() {
        String formatted = Dates.formatCurrentTimestamp();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    @Test
    public void testDTF_LOCAL_TIME() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        String result = Dates.DTF.LOCAL_TIME.format(cal.getTime());
        assertEquals("13:45:30", result);
    }

    @Test
    public void testDTF_LOCAL_DATE_TIME() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.LOCAL_DATE_TIME.format(cal.getTime());
        assertTrue(result.contains("2022-01-01"));
        assertTrue(result.contains("13:45:30"));
    }

    @Test
    public void testDTF_customFormat() {
        Dates.DTF dtf = Dates.DTF.of("dd/MM/yyyy");
        LocalDate ld = LocalDate.of(2025, 10, 4);
        String formatted = dtf.format(ld);
        assertEquals("04/10/2025", formatted);
    }

    // ===== format(Date) =====

    @Test
    public void testFormat_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_date_withFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_date_withFormatAndTimeZone() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_date_withTimestampFormat() {
        Timestamp ts = new Timestamp(1000000123L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    public void testFormat_date_isoOffsetFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_timestamp_default() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
        assertTrue(formatted.endsWith("Z"));
    }

    // ===== format(Calendar) =====

    @Test
    public void testFormat_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_calendar_withFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_calendar_withFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    // ===== format(XMLGregorianCalendar) =====

    @Test
    public void testFormat_xmlGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_withFormat() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_withFormatAndTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_null_returnsNull() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertNull(Dates.format((java.util.Date) null));
        assertNull(Dates.format((java.util.Date) null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.format((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, utc));
        assertNull(Dates.format((Calendar) null));
        assertNull(Dates.format((Calendar) null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.format((Calendar) null, Dates.LOCAL_DATE_FORMAT, utc));
        assertNull(Dates.format((XMLGregorianCalendar) null));
        assertNull(Dates.format((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.format((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, utc));
        assertNull(Dates.DTF.LOCAL_DATE.format((java.util.Date) null));
        assertNull(Dates.DTF.LOCAL_DATE.format((Calendar) null));
        assertNull(Dates.DTF.LOCAL_DATE.format((TemporalAccessor) null));
    }

    // ===== DTF class =====

    @Test
    public void testDTF_format_date() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = dtf.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDTF_format_calendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = dtf.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDTF_format_temporalAccessor() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        LocalDate localDate = LocalDate.of(2025, 10, 4);
        String formatted = dtf.format(localDate);
        assertNotNull(formatted);
        assertEquals("2025-10-04", formatted);
    }

    @Test
    public void testDTF_ISO_OFFSET_DATE_TIME_format() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 10, 4, 14, 30, 45, 0, ZoneOffset.UTC);
        String formatted = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertNotNull(formatted);
        assertTrue(formatted.contains("+00:00") || formatted.contains("Z"));
    }

    // ===== Missing tests: format with null Date =====

    @Test
    public void testFormat_timestamp() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts);
        assertNotNull(formatted);
        assertTrue(formatted.length() > 0);
    }

    @Test
    public void testFormat_timestamp_withFormat() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.contains("."));
    }

    @Test
    public void testFormat_timestamp_withFormatAndTimeZone() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
    }

    // ===== format(Calendar, String, TimeZone) - with format/timezone combinations =====

    @Test
    public void testFormat_calendar_withBothFormatAndTimeZone_UTC() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testFormat_calendar_nullFormatNullTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000000L);
        String formatted = Dates.format(cal, null, null);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    // ===== format with null date returns null =====

    // ===== formatTo =====

    @Test
    public void testFormatTo_timestamp() {
        Timestamp ts = new Timestamp(1000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(ts, sb);
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_withFormatAndTimeZone_UTC() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"), sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testFormatTo_calendar_withFormatAndTimeZone_UTC() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"), sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    // ===== formatTo with Timestamp appended to StringBuilder =====

    @Test
    public void testFormatTo_timestamp_toStringBuilder() {
        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(ts, sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().endsWith("Z"));
    }

    @Test
    public void testFormatTo_null_writesNull() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        StringBuilder sb = new StringBuilder();
        Dates.formatTo((java.util.Date) null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, utc, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((Calendar) null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((Calendar) null, Dates.LOCAL_DATE_FORMAT, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((Calendar) null, Dates.LOCAL_DATE_FORMAT, utc, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((XMLGregorianCalendar) null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, utc, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.formatTo((java.util.Date) null, null, null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.DTF.LOCAL_DATE.formatTo((java.util.Date) null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.DTF.LOCAL_DATE.formatTo((Calendar) null, sb);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        Dates.DTF.LOCAL_DATE.formatTo((TemporalAccessor) null, sb);
        assertEquals("null", sb.toString());
    }

    // ===== formatTo(XMLGregorianCalendar, String, TimeZone, Appendable) additional coverage =====

    @Test
    public void testFormatTo_xmlGregorianCalendar_nullFormatNullTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, null, null, sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().contains("T"));
    }

    // ===== formatTo(Calendar, String, TimeZone, Appendable) additional coverage =====

    @Test
    public void testFormatTo_calendar_nullFormatNullTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, null, null, sb);
        assertTrue(sb.length() > 0);
        assertTrue(sb.toString().contains("T"));
    }

    @Test
    public void testFormatTo_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(date, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb);
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb);
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(cal, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb);
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testDTF_formatTo() {
        Dates.DTF dtf = Dates.DTF.LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        dtf.formatTo(new java.util.Date(1000000000L), sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        dtf.formatTo(cal, sb);
        assertTrue(sb.length() > 0);

        sb.setLength(0);
        dtf.formatTo(LocalDate.of(2025, 10, 4), sb);
        assertEquals("2025-10-04", sb.toString());
    }

    @Test
    public void testFormatDate_withSpecificFormat_andTimeZone() {
        java.util.Date date = new java.util.Date(1000000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    // ===== fastDateFormat: year >= 10000 throws IllegalArgumentException (L2967) =====

    @Test
    public void testFormat_yearOutOfRange_throws() {
        // The fixed four-digit ISO contract cannot represent years >= 10000 without producing text
        // that its parser cannot consume.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, 10001);
        java.util.Date futureDate = cal.getTime();

        assertThrows(IllegalArgumentException.class, () -> Dates.format(futureDate));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(futureDate, Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    // ===== DTF toString =====

    @Test
    public void testDTF_toString() {
        assertEquals("uuuu-MM-dd", Dates.DTF.LOCAL_DATE.toString());
        assertEquals("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Dates.DTF.ISO_8601_TIMESTAMP.toString());
    }

    // ===== DTF constant instances =====

    @Test
    public void testDTF_LOCAL_DATE() {
        assertNotNull(Dates.DTF.LOCAL_DATE);
    }

    @Test
    public void testDtfFormatTemporalConvertsToUtcForZFormats() {
        // regression: the quoted 'Z' patterns printed the temporal's LOCAL wall-clock fields and
        // stamped 'Z' on them, silently corrupting the instant for non-UTC zoned/offset temporals
        final ZonedDateTime zdt = ZonedDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneId.of("America/New_York")); // = 2023-12-25T20:30:45Z

        assertEquals("2023-12-25T20:30:45Z", Dates.DTF.ISO_8601_DATE_TIME.format(zdt));
        assertEquals("2023-12-25T20:30:45.000Z", Dates.DTF.ISO_8601_TIMESTAMP.format(zdt));

        // zone-less temporals are rejected: appending 'Z' to local wall-clock fields would
        // mislabel them as an absolute UTC instant
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_8601_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 15, 30, 45)));
    }

    @Test
    public void testDtfParseToLocalForZFormatsReturnsValueAsWritten() {
        // regression: the default branch rendered the UTC instant in the JVM default zone,
        // diverging from the ZONED/OFFSET siblings which return the value as written
        assertEquals(LocalDate.of(2023, 12, 25), Dates.DTF.ISO_8601_DATE_TIME.parseToLocalDate("2023-12-25T20:30:45Z"));
        assertEquals(LocalTime.of(20, 30, 45), Dates.DTF.ISO_8601_DATE_TIME.parseToLocalTime("2023-12-25T20:30:45Z"));
        assertEquals(LocalDateTime.of(2023, 12, 25, 20, 30, 45), Dates.DTF.ISO_8601_DATE_TIME.parseToLocalDateTime("2023-12-25T20:30:45Z"));
    }

    @Test
    public void testFormatYear10000IsRejectedByAllFixedFourDigitIsoPaths() {
        final java.util.Date d = new java.util.Date(253402304400000L); // year 10000 in UTC

        assertThrows(IllegalArgumentException.class, () -> Dates.format(d));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(d, Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    @Test
    public void testCallerControlledFormatterPoolsAreBounded() throws Exception {
        final Field dateFormatPoolField = Dates.class.getDeclaredField("dfPool");
        dateFormatPoolField.setAccessible(true);
        final Map<?, ?> dateFormatPool = (Map<?, ?>) dateFormatPoolField.get(null);
        final java.util.Set<?> originalFormats = new java.util.HashSet<>(dateFormatPool.keySet());

        try {
            for (int i = 0; i < 128; i++) {
                final String pattern = "yyyy-MM-dd 'pool-" + i + "'";
                assertNotNull(Dates.format(new java.util.Date(0L), pattern, TimeZone.getTimeZone("GMT")));

                final TimeZone zone = new SimpleTimeZone(0, "DatesTest-pool-zone-" + i);
                assertNotNull(Dates.parseToJUDate("2024-01-02 03:04:05", Dates.LOCAL_DATE_TIME_FORMAT, zone));
            }

            assertTrue(dateFormatPool.size() <= 64, "caller-supplied patterns must not grow the process-wide pool without bound");
        } finally {
            dateFormatPool.keySet().removeIf(pattern -> !originalFormats.contains(pattern));
        }
    }

    // ===== formatTo: the default-format fast path writes directly through Writer.write(char[]) =====

    @Test
    public void testFormatTo_writerFastPath() {
        final java.io.StringWriter tsWriter = new java.io.StringWriter();
        Dates.formatTo(new Timestamp(1736937045123L), tsWriter);
        assertEquals("2025-01-15T10:30:45.123Z", tsWriter.toString());

        final java.io.StringWriter dateWriter = new java.io.StringWriter();
        Dates.formatTo(new java.util.Date(1736937045123L), dateWriter);
        assertEquals("2025-01-15T10:30:45Z", dateWriter.toString());
    }

    // ===== locale digit shapes: the legacy engine uses the locale's digits, java.time uses ASCII =====

    /**
     * The class javadoc's digit-shape paragraph: a custom pattern keeps the caller's locale on the legacy
     * engine, so its numbers come out in that locale's digits (Arabic-Indic for {@code ar-EG}), while a
     * predefined constant stays US/ASCII and {@link Dates.DTF} always writes ASCII. The expected digits are
     * derived from the locale itself, so the pin holds whatever numbering system a JDK's CLDR data picks.
     */
    @Test
    public void testCustomPatternWritesTheLocaleDigitsWhileDtfStaysAscii() {
        final java.util.Date date = new java.util.Date(1736899200000L);
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final Locale arabicEgypt = Locale.forLanguageTag("ar-EG");
        final char zero = DecimalFormatSymbols.getInstance(arabicEgypt).getZeroDigit();

        final StringBuilder expected = new StringBuilder();

        for (final char c : "2025/01/15".toCharArray()) {
            expected.append(c == '/' ? c : (char) (zero + c - '0'));
        }

        assertEquals(expected.toString(), Dates.format(date, "yyyy/MM/dd", utc, arabicEgypt));

        final SimpleDateFormat raw = new SimpleDateFormat("yyyy/MM/dd", arabicEgypt);
        raw.setTimeZone(utc);

        assertEquals(raw.format(date), Dates.format(date, "yyyy/MM/dd", utc, arabicEgypt));
        assertEquals(1736899200000L, Dates.parseToJUDate(expected.toString(), "yyyy/MM/dd", utc, arabicEgypt).getTime());

        // A predefined constant is US/ASCII whatever the locale, and the java.time engine always is.
        assertEquals("2025-01-15 00:00:00", Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, utc, arabicEgypt));
        assertEquals("2025/01/15", Dates.DTF.of("uuuu/MM/dd", arabicEgypt).format(LocalDate.of(2025, 1, 15)));
    }
}
