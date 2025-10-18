package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Dates200Test extends TestBase {

    private static final long TEST_EPOCH_MILLIS = 1698315330500L;
    private static final java.util.Date TEST_JU_DATE = new java.util.Date(TEST_EPOCH_MILLIS);
    private static final java.sql.Date TEST_SQL_DATE = new java.sql.Date(TEST_EPOCH_MILLIS);
    private static final java.sql.Time TEST_SQL_TIME = new java.sql.Time(TEST_EPOCH_MILLIS);
    private static final java.sql.Timestamp TEST_SQL_TIMESTAMP = new java.sql.Timestamp(TEST_EPOCH_MILLIS);

    private static Calendar testCalendar;
    private static GregorianCalendar testGregorianCalendar;
    private static XMLGregorianCalendar testXMLGregorianCalendar;
    private static DatatypeFactory datatypeFactoryInstance;

    private static Calendar getCalendarForMillis(long epochMillis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    @BeforeAll
    static void setUpAll() {
        testCalendar = Calendar.getInstance(Dates.UTC_TIME_ZONE);
        testCalendar.setTimeInMillis(TEST_EPOCH_MILLIS);

        testGregorianCalendar = new GregorianCalendar(Dates.UTC_TIME_ZONE);
        testGregorianCalendar.setTimeInMillis(TEST_EPOCH_MILLIS);

        try {
            datatypeFactoryInstance = DatatypeFactory.newInstance();
            testXMLGregorianCalendar = datatypeFactoryInstance.newXMLGregorianCalendar((GregorianCalendar) testCalendar.clone());
        } catch (DatatypeConfigurationException e) {
            System.err.println("Warning: Could not initialize DatatypeFactory or XMLGregorianCalendar for tests. " + e.getMessage());
            datatypeFactoryInstance = null;
            testXMLGregorianCalendar = null;
        }
    }

    @Nested
    public class ConstantsTests {
        @Test
        public void testTimeZonesAndZoneIds() {
            assertNotNull(Dates.DEFAULT_TIME_ZONE);
            assertEquals("UTC", Dates.UTC_TIME_ZONE.getID());
            assertEquals("GMT", Dates.GMT_TIME_ZONE.getID());

            assertNotNull(Dates.DEFAULT_ZONE_ID);
            assertEquals(Dates.UTC_TIME_ZONE.toZoneId(), Dates.UTC_ZONE_ID);
            assertEquals(Dates.GMT_TIME_ZONE.toZoneId(), Dates.GMT_ZONE_ID);
        }

        @Test
        public void testFormatStrings() {
            assertEquals("yyyy", Dates.LOCAL_YEAR_FORMAT);
            assertEquals("MM-dd", Dates.LOCAL_MONTH_DAY_FORMAT);
            assertEquals("yyyy-MM-dd", Dates.LOCAL_DATE_FORMAT);
            assertEquals("HH:mm:ss", Dates.LOCAL_TIME_FORMAT);
            assertEquals("yyyy-MM-dd HH:mm:ss", Dates.LOCAL_DATE_TIME_FORMAT);
            assertEquals("yyyy-MM-dd'T'HH:mm:ss", Dates.ISO_LOCAL_DATE_TIME_FORMAT);
            assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", Dates.ISO_OFFSET_DATE_TIME_FORMAT);
            assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", Dates.ISO_8601_DATE_TIME_FORMAT);
            assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Dates.ISO_8601_TIMESTAMP_FORMAT);
            assertEquals("EEE, dd MMM yyyy HH:mm:ss zzz", Dates.RFC_1123_DATE_TIME_FORMAT);
        }

        @Test
        public void testSemiMonth() {
            assertEquals(1001, Dates.SEMI_MONTH);
        }
    }

    public static class CustomDate extends java.util.Date {
        private boolean customConstructorCalled = false;

        public CustomDate(long date) {
            super(date);
            customConstructorCalled = true;
        }

        public boolean isCustomConstructorCalled() {
            return customConstructorCalled;
        }
    }

    public static class MyOwnDate extends java.util.Date {
        public MyOwnDate(long date) {
            super(date);
        }
    }

    public static class CustomCalendar extends GregorianCalendar {
        private boolean customConstructorCalled = false;

        public CustomCalendar() {
            super();
        }

        public void setCustomFlag(boolean flag) {
            this.customConstructorCalled = flag;
        }

        public boolean isCustomConstructorCalled() {
            return customConstructorCalled;
        }
    }

    public static class MyOwnCalendar extends GregorianCalendar {
        public MyOwnCalendar() {
            super();
        }
    }

    @Nested
    public class CurrentTimeTests {
        @Test
        public void testCurrentTimeMillis() {
            long start = System.currentTimeMillis();
            long dCurrent = Dates.currentTimeMillis();
            long end = System.currentTimeMillis();
            assertTrue(dCurrent >= start && dCurrent <= end);
        }

        @Test
        public void testCurrentTime() {
            assertNotNull(Dates.currentTime());
            assertTrue(Dates.currentTime() instanceof java.sql.Time);
        }

        @Test
        public void testCurrentDate() {
            assertNotNull(Dates.currentDate());
            assertTrue(Dates.currentDate() instanceof java.sql.Date);
        }

        @Test
        public void testCurrentTimestamp() {
            assertNotNull(Dates.currentTimestamp());
            assertTrue(Dates.currentTimestamp() instanceof java.sql.Timestamp);
        }

        @Test
        public void testCurrentJUDate() {
            assertNotNull(Dates.currentJUDate());
            assertTrue(Dates.currentJUDate() instanceof java.util.Date);
        }

        @Test
        public void testCurrentCalendar() {
            assertNotNull(Dates.currentCalendar());
            assertTrue(Dates.currentCalendar() instanceof Calendar);
        }

        @Test
        public void testCurrentGregorianCalendar() {
            assertNotNull(Dates.currentGregorianCalendar());
            assertTrue(Dates.currentGregorianCalendar() instanceof GregorianCalendar);
        }

        @Test
        public void testCurrentXMLGregorianCalendar() {
            if (datatypeFactoryInstance != null) {
                assertNotNull(Dates.currentXMLGregorianCalendar());
                assertTrue(Dates.currentXMLGregorianCalendar() instanceof XMLGregorianCalendar);
            } else {
                System.out.println("Skipping testCurrentXMLGregorianCalendar due to DatatypeFactory init failure.");
                assertThrows(NullPointerException.class, () -> Dates.currentXMLGregorianCalendar());
            }
        }
    }

    @Nested
    public class CurrentTimeRolledTests {
        @Test
        public void testCurrentTimeRolled() {
            long now = System.currentTimeMillis();
            Time rolled = Dates.currentTimeRolled(1, TimeUnit.SECONDS);
            assertTrue(Math.abs(rolled.getTime() - (now + 1000)) < 50);
        }

        @Test
        public void testCurrentDateRolled() {
            long now = System.currentTimeMillis();
            java.sql.Date rolled = Dates.currentDateRolled(1, TimeUnit.DAYS);
            Calendar calNow = Calendar.getInstance();
            calNow.setTimeInMillis(now);
            Calendar calRolled = Calendar.getInstance();
            calRolled.setTime(rolled);

            Calendar expectedCal = Calendar.getInstance();
            expectedCal.setTimeInMillis(now + TimeUnit.DAYS.toMillis(1));
            expectedCal.set(Calendar.HOUR_OF_DAY, 0);
            expectedCal.set(Calendar.MINUTE, 0);
            expectedCal.set(Calendar.SECOND, 0);
            expectedCal.set(Calendar.MILLISECOND, 0);
            calRolled.set(Calendar.HOUR_OF_DAY, 0);
            calRolled.set(Calendar.MINUTE, 0);
            calRolled.set(Calendar.SECOND, 0);
            calRolled.set(Calendar.MILLISECOND, 0);

            assertEquals(expectedCal.getTimeInMillis(), calRolled.getTimeInMillis());
        }

        @Test
        public void testCurrentTimestampRolled() {
            long now = System.currentTimeMillis();
            Timestamp rolled = Dates.currentTimestampRolled(-1, TimeUnit.HOURS);
            assertTrue(Math.abs(rolled.getTime() - (now - TimeUnit.HOURS.toMillis(1))) < 50);
        }

        @Test
        public void testCurrentJUDateRolled() {
            long now = System.currentTimeMillis();
            java.util.Date rolled = Dates.currentJUDateRolled(5, TimeUnit.MINUTES);
            assertTrue(Math.abs(rolled.getTime() - (now + TimeUnit.MINUTES.toMillis(5))) < 50);
        }

        @Test
        public void testCurrentCalendarRolled() {
            long now = System.currentTimeMillis();
            Calendar rolled = Dates.currentCalendarRolled(10, TimeUnit.MILLISECONDS);
            assertTrue(Math.abs(rolled.getTimeInMillis() - (now + 10)) < 50);
        }
    }

    @Nested
    public class CreateObjectTests {
        @Test
        public void testCreateJUDateFromCalendar() {
            assertEquals(TEST_EPOCH_MILLIS, Dates.createJUDate(testCalendar).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
        }

        @Test
        public void testCreateJUDateFromDate() {
            assertEquals(TEST_EPOCH_MILLIS, Dates.createJUDate(TEST_JU_DATE).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
        }

        @Test
        public void testCreateJUDateFromLong() {
            assertEquals(TEST_EPOCH_MILLIS, Dates.createJUDate(TEST_EPOCH_MILLIS).getTime());
        }

        @Test
        public void testCreateDateFromCalendar() {
            assertEquals(TEST_SQL_DATE.getTime(), Dates.createDate(testCalendar).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createDate((Calendar) null));
        }

        @Test
        public void testCreateDateFromDate() {
            assertEquals(TEST_SQL_DATE.getTime(), Dates.createDate(TEST_JU_DATE).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createDate((java.util.Date) null));
        }

        @Test
        public void testCreateDateFromLong() {
            assertEquals(TEST_SQL_DATE.getTime(), Dates.createDate(TEST_EPOCH_MILLIS).getTime());
        }

        @Test
        public void testCreateTimeFromCalendar() {
            assertEquals(TEST_SQL_TIME.getTime(), Dates.createTime(testCalendar).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTime((Calendar) null));
        }

        @Test
        public void testCreateTimeFromDate() {
            assertEquals(TEST_SQL_TIME.getTime(), Dates.createTime(TEST_JU_DATE).getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTime((java.util.Date) null));
        }

        @Test
        public void testCreateTimeFromLong() {
            assertEquals(TEST_SQL_TIME.getTime(), Dates.createTime(TEST_EPOCH_MILLIS).getTime());
        }

        @Test
        public void testCreateTimestampFromCalendar() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(testCalendar).getTime());
            assertEquals(TEST_SQL_TIMESTAMP.getNanos(), Dates.createTimestamp(testCalendar).getNanos());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
        }

        @Test
        public void testCreateTimestampFromDate() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(TEST_JU_DATE).getTime());
            assertEquals(TEST_SQL_TIMESTAMP.getNanos() / 1_000_000 * 1_000_000, Dates.createTimestamp(TEST_JU_DATE).getNanos());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
        }

        @Test
        public void testCreateTimestampFromLong() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(TEST_EPOCH_MILLIS).getTime());
            assertEquals(TEST_SQL_TIMESTAMP.getNanos(), Dates.createTimestamp(TEST_EPOCH_MILLIS).getNanos());
        }

        @Test
        public void testCreateCalendarFromCalendar() {
            Calendar created = Dates.createCalendar(testCalendar);
            assertEquals(testCalendar.getTimeInMillis(), created.getTimeInMillis());
            assertNotEquals(testCalendar.getTimeZone(), created.getTimeZone());
            assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((Calendar) null));
        }

        @Test
        public void testCreateCalendarFromDate() {
            Calendar created = Dates.createCalendar(TEST_JU_DATE);
            assertEquals(TEST_JU_DATE.getTime(), created.getTimeInMillis());
            assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((java.util.Date) null));
        }

        @Test
        public void testCreateCalendarFromLong() {
            Calendar created = Dates.createCalendar(TEST_EPOCH_MILLIS);
            assertEquals(TEST_EPOCH_MILLIS, created.getTimeInMillis());
        }

        @Test
        public void testCreateCalendarFromLongAndTZ() {
            TimeZone pst = TimeZone.getTimeZone("PST");
            Calendar created = Dates.createCalendar(TEST_EPOCH_MILLIS, pst);
            assertEquals(TEST_EPOCH_MILLIS, created.getTimeInMillis());
            assertEquals(pst, created.getTimeZone());
            Calendar createdDefaultTz = Dates.createCalendar(TEST_EPOCH_MILLIS, null);
            assertEquals(TimeZone.getDefault(), createdDefaultTz.getTimeZone());
        }

        @Test
        public void testCreateGregorianCalendarFromCalendar() {
            GregorianCalendar created = Dates.createGregorianCalendar(testCalendar);
            assertEquals(testCalendar.getTimeInMillis(), created.getTimeInMillis());
            assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((Calendar) null));
        }

        @Test
        public void testCreateGregorianCalendarFromDate() {
            GregorianCalendar created = Dates.createGregorianCalendar(TEST_JU_DATE);
            assertEquals(TEST_JU_DATE.getTime(), created.getTimeInMillis());
            assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((java.util.Date) null));
        }

        @Test
        public void testCreateGregorianCalendarFromLong() {
            GregorianCalendar created = Dates.createGregorianCalendar(TEST_EPOCH_MILLIS);
            assertEquals(TEST_EPOCH_MILLIS, created.getTimeInMillis());
        }

        @Test
        public void testCreateGregorianCalendarFromLongAndTZ() {
            TimeZone est = TimeZone.getTimeZone("EST");
            GregorianCalendar created = Dates.createGregorianCalendar(TEST_EPOCH_MILLIS, est);
            assertEquals(TEST_EPOCH_MILLIS, created.getTimeInMillis());
            assertEquals(est, created.getTimeZone());
        }

        @Test
        public void testCreateXMLGregorianCalendarFromCalendar() {
            if (datatypeFactoryInstance == null || testXMLGregorianCalendar == null) {
                System.out.println("Skipping testCreateXMLGregorianCalendarFromCalendar due to DatatypeFactory/TestObject init failure.");
                assertThrows(NullPointerException.class, () -> Dates.createXMLGregorianCalendar(testCalendar));
                return;
            }
            XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(testCalendar);
            assertEquals(testXMLGregorianCalendar.toGregorianCalendar().getTimeInMillis(), created.toGregorianCalendar().getTimeInMillis());
            assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((Calendar) null));
        }

        @Test
        public void testCreateXMLGregorianCalendarFromDate() {
            if (datatypeFactoryInstance == null || testXMLGregorianCalendar == null) {
                System.out.println("Skipping testCreateXMLGregorianCalendarFromDate due to DatatypeFactory/TestObject init failure.");
                assertThrows(NullPointerException.class, () -> Dates.createXMLGregorianCalendar(TEST_JU_DATE));
                return;
            }
            XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(TEST_JU_DATE);
            assertEquals(testXMLGregorianCalendar.toGregorianCalendar().getTimeInMillis(), created.toGregorianCalendar().getTimeInMillis());
            assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((java.util.Date) null));
        }

        @Test
        public void testCreateXMLGregorianCalendarFromLong() {
            if (datatypeFactoryInstance == null) {
                System.out.println("Skipping testCreateXMLGregorianCalendarFromLong due to DatatypeFactory init failure.");
                assertThrows(NullPointerException.class, () -> Dates.createXMLGregorianCalendar(TEST_EPOCH_MILLIS));
                return;
            }
            XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(TEST_EPOCH_MILLIS);
            GregorianCalendar expectedCal = new GregorianCalendar();
            expectedCal.setTimeInMillis(TEST_EPOCH_MILLIS);
            assertEquals(expectedCal.getTimeInMillis(), created.toGregorianCalendar().getTimeInMillis());
        }

        @Test
        public void testCreateXMLGregorianCalendarFromLongAndTZ() {
            if (datatypeFactoryInstance == null) {
                System.out.println("Skipping testCreateXMLGregorianCalendarFromLongAndTZ due to DatatypeFactory init failure.");
                assertThrows(NullPointerException.class, () -> Dates.createXMLGregorianCalendar(TEST_EPOCH_MILLIS, TimeZone.getTimeZone("PST")));
                return;
            }
            TimeZone pst = TimeZone.getTimeZone("PST");
            XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(TEST_EPOCH_MILLIS, pst);
            GregorianCalendar expectedCal = Dates.createGregorianCalendar(TEST_EPOCH_MILLIS, pst);
            assertEquals(expectedCal.getTimeInMillis(), created.toGregorianCalendar().getTimeInMillis());
            assertNotEquals(pst.getRawOffset(), created.toGregorianCalendar().getTimeZone().getRawOffset());
        }
    }

    @Nested
    public class ParseStringTests {
        private final String isoTimestampStr = "2023-10-26T10:15:30.500Z";
        private final String isoDateTimeStr = "2023-10-26T10:15:30Z";
        private final String localDateStr = "2023-10-26";
        private final String localTimeStr = "10:15:30";
        private final String localDateTimeStr = "2023-10-26 10:15:30";
        private final String rfc1123Str = "Thu, 26 Oct 2023 10:15:30 GMT";

        @Test
        public void testParseJUDate() {
            assertNull(Dates.parseJUDate(null));
            assertNull(Dates.parseJUDate(""));
            assertNull(Dates.parseJUDate("null"));

            assertEquals(TEST_EPOCH_MILLIS, Dates.parseJUDate(isoTimestampStr).getTime());
            assertEquals(TEST_EPOCH_MILLIS - 500, Dates.parseJUDate(isoDateTimeStr).getTime());
            assertEquals(TEST_EPOCH_MILLIS, Dates.parseJUDate(Long.toString(TEST_EPOCH_MILLIS)).getTime());

            java.util.Date parsedDate = Dates.parseJUDate(localDateStr, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedDate);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
            assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));

            assertThrows(IllegalArgumentException.class, () -> Dates.parseJUDate("invalid date"));
            assertThrows(RuntimeException.class, () -> Dates.parseJUDate(isoTimestampStr, Dates.ISO_8601_TIMESTAMP_FORMAT, TimeZone.getTimeZone("PST")));
        }

        @Test
        public void testParseDate() {
            assertNull(Dates.parseDate(null));
            java.sql.Date parsedSqlDate = Dates.parseDate(localDateStr, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedSqlDate);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
            assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testParseTime() {
            assertNull(Dates.parseTime(null));
            java.sql.Time parsedSqlTime = Dates.parseTime(localTimeStr, Dates.LOCAL_TIME_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedSqlTime);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(15, cal.get(Calendar.MINUTE));
            assertEquals(30, cal.get(Calendar.SECOND));

            java.sql.Time parsedFromDateTime = Dates.parseTime(isoDateTimeStr);
            Calendar calFromDT = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            calFromDT.setTime(parsedFromDateTime);
            assertEquals(10, calFromDT.get(Calendar.HOUR_OF_DAY));
            assertEquals(15, calFromDT.get(Calendar.MINUTE));
            assertEquals(30, calFromDT.get(Calendar.SECOND));
            assertEquals(2023, calFromDT.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, calFromDT.get(Calendar.MONTH));
            assertEquals(26, calFromDT.get(Calendar.DAY_OF_MONTH));

        }

        @Test
        public void testParseTimestamp() {
            assertNull(Dates.parseTimestamp(null));
            assertEquals(TEST_EPOCH_MILLIS, Dates.parseTimestamp(isoTimestampStr).getTime());
            assertEquals(500_000_000, Dates.parseTimestamp(isoTimestampStr).getNanos());
        }

        @Test
        public void testParseCalendar() {
            assertNull(Dates.parseCalendar(null));
            Calendar parsed = Dates.parseCalendar(isoDateTimeStr, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
            assertEquals(TEST_EPOCH_MILLIS - 500, parsed.getTimeInMillis());
            assertEquals(Dates.UTC_TIME_ZONE, parsed.getTimeZone());
        }

        @Test
        public void testParseGregorianCalendar() {
            assertNull(Dates.parseGregorianCalendar(null));
            GregorianCalendar parsed = Dates.parseGregorianCalendar(isoDateTimeStr, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
            assertEquals(TEST_EPOCH_MILLIS - 500, parsed.getTimeInMillis());
            assertEquals(Dates.UTC_TIME_ZONE.getID(), parsed.getTimeZone().getID());
        }

        @Test
        public void testParseXMLGregorianCalendar() {
            if (datatypeFactoryInstance == null) {
                System.out.println("Skipping testParseXMLGregorianCalendar due to DatatypeFactory init failure.");
                assertThrows(NullPointerException.class,
                        () -> Dates.parseXMLGregorianCalendar(isoDateTimeStr, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE));
                return;
            }
            assertNull(Dates.parseXMLGregorianCalendar(null));
            XMLGregorianCalendar parsed = Dates.parseXMLGregorianCalendar(isoDateTimeStr, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
            assertNotNull(parsed);
            assertEquals(TEST_EPOCH_MILLIS - 500, parsed.toGregorianCalendar().getTimeInMillis());
        }
    }

    @Nested
    public class FormatObjectTests {

        @Test
        public void testFormatLocalDate() {
            String formatted = Dates.formatLocalDate();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
        }

        @Test
        public void testFormatLocalDateTime() {
            String formatted = Dates.formatLocalDateTime();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        public void testFormatCurrentDateTime() {
            String formatted = Dates.formatCurrentDateTime();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
        }

        @Test
        public void testFormatCurrentTimestamp() {
            String formatted = Dates.formatCurrentTimestamp();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
        }

        @Test
        public void testFormatDate() {
            assertEquals("2023-10-26T10:15:30.500Z", Dates.format(TEST_SQL_TIMESTAMP));
            assertEquals("2023-10-26T10:15:30Z", Dates.format(TEST_JU_DATE));

            assertEquals("2023-10-26", Dates.format(TEST_JU_DATE, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
            assertEquals("10:15:30", Dates.format(TEST_JU_DATE, Dates.LOCAL_TIME_FORMAT, Dates.UTC_TIME_ZONE));
        }

        @Test
        public void testFormatCalendar() {
            assertEquals("2023-10-26T10:15:30Z", Dates.format(testCalendar));
            assertEquals("2023/10/26", Dates.format(testCalendar, "yyyy/MM/dd", Dates.UTC_TIME_ZONE));
        }

        @Test
        public void testFormatXMLGregorianCalendar() {
            if (testXMLGregorianCalendar == null) {
                System.out.println("Skipping testFormatXMLGregorianCalendar due to TestObject init failure.");
                return;
            }
            assertEquals("2023-10-26T10:15:30Z", Dates.format(testXMLGregorianCalendar));
            assertEquals("26/10/2023", Dates.format(testXMLGregorianCalendar, "dd/MM/yyyy", Dates.UTC_TIME_ZONE));
        }

        @Test
        public void testFormatTo() {
            StringBuilder sb = new StringBuilder();
            Dates.formatTo(TEST_JU_DATE, sb);
            assertEquals("2023-10-26T10:15:30Z", sb.toString());

            sb.setLength(0);
            Dates.formatTo(TEST_SQL_TIMESTAMP, Dates.LOCAL_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE, sb);
            assertEquals("2023-10-26 10:15:30", sb.toString());

            sb.setLength(0);
            Dates.formatTo((java.util.Date) null, sb);
            assertEquals("null", sb.toString());
        }

        @Test
        public void testFormatToCalendar() {
            StringBuilder sb = new StringBuilder();
            Dates.formatTo(testCalendar, sb);
            assertEquals("2023-10-26T10:15:30Z", sb.toString());

            sb.setLength(0);
            Dates.formatTo(testCalendar, "yyyyMMdd", Dates.UTC_TIME_ZONE, sb);
            assertEquals("20231026", sb.toString());

            sb.setLength(0);
            Dates.formatTo((Calendar) null, sb);
            assertEquals("null", sb.toString());
        }

        @Test
        public void testFormatToXMLGregorianCalendar() {
            if (testXMLGregorianCalendar == null) {
                System.out.println("Skipping testFormatToXMLGregorianCalendar due to TestObject init failure.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            Dates.formatTo(testXMLGregorianCalendar, sb);
            assertEquals("2023-10-26T10:15:30Z", sb.toString());

            sb.setLength(0);
            Dates.formatTo(testXMLGregorianCalendar, "HH:mm", Dates.UTC_TIME_ZONE, sb);
            assertEquals("10:15", sb.toString());

            sb.setLength(0);
            Dates.formatTo((XMLGregorianCalendar) null, sb);
            assertEquals("null", sb.toString());
        }

    }

    @Nested
    public class ManipulationSetTests {
        private java.util.Date baseDate() {
            return new java.util.Date(TEST_EPOCH_MILLIS);
        }

        @Test
        public void testSetYears() {
            java.util.Date newDate = Dates.setYears(baseDate(), 2025);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(2025, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
        }

        @Test
        public void testSetMonths() {
            java.util.Date newDate = Dates.setMonths(baseDate(), Calendar.JANUARY);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        }

        @Test
        public void testSetDays() {
            java.util.Date newDate = Dates.setDays(baseDate(), 15);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        public void testSetHours() {
            java.util.Date newDate = Dates.setHours(baseDate(), 5);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            cal.setTimeZone(Dates.DEFAULT_TIME_ZONE);
            assertEquals(5, cal.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testSetMinutes() {
            java.util.Date newDate = Dates.setMinutes(baseDate(), 45);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(45, cal.get(Calendar.MINUTE));
        }

        @Test
        public void testSetSeconds() {
            java.util.Date newDate = Dates.setSeconds(baseDate(), 10);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.SECOND));
        }

        @Test
        public void testSetMilliseconds() {
            java.util.Date newDate = Dates.setMilliseconds(baseDate(), 123);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(123, cal.get(Calendar.MILLISECOND));
        }

        @Test
        public void testSetNullDate() {
            assertThrows(IllegalArgumentException.class, () -> Dates.setYears(null, 2023));
        }
    }

    @Nested
    public class ManipulationRollAddTests {
        private java.util.Date baseDate() {
            return new java.util.Date(TEST_EPOCH_MILLIS);
        }

        private Calendar baseCalendar() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTimeInMillis(TEST_EPOCH_MILLIS);
            return cal;
        }

        @Test
        public void testRollDateWithTimeUnit() {
            java.util.Date rolled = Dates.roll(baseDate(), 1, TimeUnit.DAYS);
            assertEquals(TEST_EPOCH_MILLIS + TimeUnit.DAYS.toMillis(1), rolled.getTime());
        }

        @Test
        public void testRollDateWithCalendarField() {
            java.util.Date rolledDay = Dates.roll(baseDate(), 2, CalendarField.DAY_OF_MONTH);
            assertEquals(TEST_EPOCH_MILLIS + 2 * 24 * 60 * 60 * 1000L, rolledDay.getTime());

            Date baseDate = baseDate();
            Calendar cal = baseCalendar();
            cal.setTimeZone(Dates.DEFAULT_TIME_ZONE);
            N.println("baseDate: " + baseDate);
            N.println("baseCalendar: " + cal.getTime());
            assertEquals(cal.getTimeInMillis(), baseDate.getTime());
            java.util.Date rolledMonth = Dates.roll(baseDate, 1, CalendarField.MONTH);
            cal.add(Calendar.MONTH, 1);
            N.println("Rolled Month: " + rolledMonth);
            N.println("Expected Month: " + cal.getTime());
            assertEquals(cal.getTimeInMillis(), rolledMonth.getTime());
        }

        @Test
        public void testRollCalendarWithTimeUnit() {
            Calendar rolled = Dates.roll(baseCalendar(), -1, TimeUnit.HOURS);
            assertEquals(TEST_EPOCH_MILLIS - TimeUnit.HOURS.toMillis(1), rolled.getTimeInMillis());
        }

        @Test
        public void testRollCalendarWithCalendarField() {
            Calendar rolled = Dates.roll(baseCalendar(), 3, CalendarField.YEAR);
            Calendar expected = baseCalendar();
            expected.add(Calendar.YEAR, 3);
            assertEquals(expected.getTimeInMillis(), rolled.getTimeInMillis());
        }

        @Test
        public void testAddYearsDate() {
            Dates.addYears(baseDate(), 2);
            Calendar cal = baseCalendar();
            cal.add(Calendar.YEAR, 2);
            assertEquals(cal.getTimeInMillis(), Dates.addYears(baseDate(), 2).getTime());
        }

        @Test
        public void testAddMonthsDate() {
            Calendar cal = baseCalendar();
            cal.add(Calendar.MONTH, -3);
            assertEquals(cal.getTimeInMillis(), Dates.addMonths(baseDate(), -3).getTime());
        }

        @Test
        public void testAddWeeksDate() {
            Calendar cal = baseCalendar();
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            assertEquals(TEST_EPOCH_MILLIS + 7 * 24 * 60 * 60 * 1000L, Dates.addWeeks(baseDate(), 1).getTime());
        }

        @Test
        public void testAddDaysDate() {
            assertEquals(TEST_EPOCH_MILLIS + 5 * 24 * 60 * 60 * 1000L, Dates.addDays(baseDate(), 5).getTime());
        }

        @Test
        public void testAddHoursDate() {
            assertEquals(TEST_EPOCH_MILLIS - 3 * 60 * 60 * 1000L, Dates.addHours(baseDate(), -3).getTime());
        }

        @Test
        public void testAddMinutesDate() {
            assertEquals(TEST_EPOCH_MILLIS + 30 * 60 * 1000L, Dates.addMinutes(baseDate(), 30).getTime());
        }

        @Test
        public void testAddSecondsDate() {
            assertEquals(TEST_EPOCH_MILLIS + 90 * 1000L, Dates.addSeconds(baseDate(), 90).getTime());
        }

        @Test
        public void testAddMillisecondsDate() {
            assertEquals(TEST_EPOCH_MILLIS + 500L, Dates.addMilliseconds(baseDate(), 500).getTime());
        }

        @Test
        public void testAddYearsCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.YEAR, -1);
            assertEquals(expected.getTimeInMillis(), Dates.addYears(baseCalendar(), -1).getTimeInMillis());
        }

        @Test
        public void testAddMonthsCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.MONTH, 6);
            assertEquals(expected.getTimeInMillis(), Dates.addMonths(baseCalendar(), 6).getTimeInMillis());
        }

        @Test
        public void testAddWeeksCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.WEEK_OF_YEAR, -2);
            assertEquals(expected.getTimeInMillis(), Dates.addWeeks(baseCalendar(), -2).getTimeInMillis());
        }

        @Test
        public void testAddDaysCalendar() {
            Calendar expected = baseCalendar();
            expected.setTimeZone(Dates.DEFAULT_TIME_ZONE);
            expected.add(Calendar.DAY_OF_MONTH, 10);
            Calendar c = baseCalendar();
            c.setTimeZone(Dates.DEFAULT_TIME_ZONE);
            Calendar r = Dates.addDays(c, 10);
            N.println("Expected: " + Dates.format(expected));
            N.println("Expected: " + Dates.format(r));
            assertEquals(expected.getTimeInMillis(), r.getTimeInMillis());
        }

        @Test
        public void testAddHoursCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.HOUR_OF_DAY, -5);
            assertEquals(expected.getTimeInMillis(), Dates.addHours(baseCalendar(), -5).getTimeInMillis());
        }

        @Test
        public void testAddMinutesCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.MINUTE, 120);
            assertEquals(expected.getTimeInMillis(), Dates.addMinutes(baseCalendar(), 120).getTimeInMillis());
        }

        @Test
        public void testAddSecondsCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.SECOND, -30);
            assertEquals(expected.getTimeInMillis(), Dates.addSeconds(baseCalendar(), -30).getTimeInMillis());
        }

        @Test
        public void testAddMillisecondsCalendar() {
            Calendar expected = baseCalendar();
            expected.add(Calendar.MILLISECOND, 750);
            assertEquals(expected.getTimeInMillis(), Dates.addMilliseconds(baseCalendar(), 750).getTimeInMillis());
        }

        @Test
        public void testRollNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 1, TimeUnit.DAYS));
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 1, CalendarField.DAY_OF_MONTH));
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 1, TimeUnit.DAYS));
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 1, CalendarField.DAY_OF_MONTH));
        }
    }

    @Nested
    public class ManipulationRoundTruncateCeilingTests {
        private java.util.Date baseDate() {
            return new java.util.Date(TEST_EPOCH_MILLIS);
        }

        private Calendar baseCalendar() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTimeInMillis(TEST_EPOCH_MILLIS);
            return cal;
        }

        @Test
        public void testRoundDate() {
            java.util.Date roundedHour = Dates.round(baseDate(), Calendar.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(roundedHour.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
            assertEquals(0, cal.get(Calendar.MILLISECOND));

            java.util.Date roundedMin = Dates.round(baseDate(), Calendar.MINUTE);
            cal.setTime(roundedMin);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(16, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
        }

        @Test
        public void testRoundDateWithCalendarField() {
            java.util.Date rounded = Dates.round(baseDate(), CalendarField.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(rounded.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
        }

        @Test
        public void testRoundCalendar() {
            Calendar roundedCal = Dates.round(baseCalendar(), Calendar.DAY_OF_MONTH);
            assertEquals(2023, roundedCal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, roundedCal.get(Calendar.MONTH));
            assertEquals(26, roundedCal.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, roundedCal.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testRoundCalendarWithCalendarField() {
            Calendar rounded = Dates.round(baseCalendar(), CalendarField.DAY_OF_MONTH);
            assertEquals(0, rounded.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testTruncateDate() {
            java.util.Date truncated = Dates.truncate(baseDate(), Calendar.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(truncated.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
            assertEquals(0, cal.get(Calendar.MILLISECOND));
        }

        @Test
        public void testTruncateDateWithCalendarField() {
            java.util.Date truncated = Dates.truncate(baseDate(), CalendarField.MINUTE);
            Calendar cal = getCalendarForMillis(truncated.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(15, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
        }

        @Test
        public void testTruncateCalendar() {
            Calendar truncated = Dates.truncate(baseCalendar(), Calendar.MONTH);
            assertEquals(2023, truncated.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, truncated.get(Calendar.MONTH));
            assertEquals(1, truncated.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, truncated.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testTruncateCalendarWithCalendarField() {
            Calendar truncated = Dates.truncate(baseCalendar(), CalendarField.YEAR);
            assertEquals(2023, truncated.get(Calendar.YEAR));
            assertEquals(Calendar.JANUARY, truncated.get(Calendar.MONTH));
            assertEquals(1, truncated.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        public void testCeilingDate() {
            java.util.Date ceiled = Dates.ceiling(baseDate(), Calendar.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(ceiled.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
        }

        @Test
        public void testCeilingDateWithCalendarField() {
            java.util.Date ceiled = Dates.ceiling(baseDate(), CalendarField.MINUTE);
            Calendar cal = getCalendarForMillis(ceiled.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(16, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
        }

        @Test
        public void testCeilingCalendar() {
            Calendar ceiled = Dates.ceiling(baseCalendar(), Calendar.DAY_OF_MONTH);
            assertEquals(2023, ceiled.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, ceiled.get(Calendar.MONTH));
            assertEquals(27, ceiled.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, ceiled.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testCeilingCalendarWithCalendarField() {
            Calendar base = baseCalendar();
            base.set(Calendar.HOUR_OF_DAY, 0);
            base.set(Calendar.MINUTE, 0);
            base.set(Calendar.SECOND, 0);
            base.set(Calendar.MILLISECOND, 0);
            Calendar ceiled = Dates.ceiling(base, CalendarField.DAY_OF_MONTH);
            assertEquals(2023, ceiled.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, ceiled.get(Calendar.MONTH));
            assertEquals(27, ceiled.get(Calendar.DAY_OF_MONTH));
        }

        @Test
        public void testRoundTruncateCeilingNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.HOUR));
            assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, Calendar.MONTH));
            assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, CalendarField.DAY_OF_MONTH));
        }
    }

    @Nested
    public class ComparisonTests {
        Calendar cal1 = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE);
        Calendar cal2 = getCalendarForMillis(TEST_EPOCH_MILLIS + 100, Dates.UTC_TIME_ZONE);
        Calendar cal3 = getCalendarForMillis(TEST_EPOCH_MILLIS + TimeUnit.HOURS.toMillis(1), Dates.UTC_TIME_ZONE);
        Calendar cal4 = getCalendarForMillis(TEST_EPOCH_MILLIS, TimeZone.getTimeZone("PST"));
        java.util.Date date1 = new java.util.Date(cal1.getTimeInMillis());
        java.util.Date date2 = new java.util.Date(cal2.getTimeInMillis());
        java.util.Date date3 = new java.util.Date(cal3.getTimeInMillis());

        @Test
        public void testTruncatedEquals() {
            assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND));
            assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.MILLISECOND));
            assertTrue(Dates.truncatedEquals(cal1, cal3, Calendar.DAY_OF_MONTH));
            assertFalse(Dates.truncatedEquals(cal1, cal3, Calendar.HOUR_OF_DAY));

            assertTrue(Dates.truncatedEquals(date1, date2, CalendarField.SECOND));
            assertFalse(Dates.truncatedEquals(date1, date3, CalendarField.HOUR_OF_DAY));
        }

        @Test
        public void testTruncatedCompareTo() {
            assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, Calendar.SECOND));
            assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.MILLISECOND) < 0);
            assertEquals(0, Dates.truncatedCompareTo(cal1, cal3, CalendarField.DAY_OF_MONTH));
            assertTrue(Dates.truncatedCompareTo(cal1, cal3, CalendarField.HOUR_OF_DAY) < 0);

            assertEquals(0, Dates.truncatedCompareTo(date1, date2, Calendar.SECOND));
            assertTrue(Dates.truncatedCompareTo(date1, date3, Calendar.HOUR_OF_DAY) < 0);
        }

        @Test
        public void testIsSameDay() {
            assertTrue(Dates.isSameDay(date1, date2));
            assertTrue(Dates.isSameDay(date1, date3));
            java.util.Date nextDayDate = Dates.addDays(date1, 1);
            assertFalse(Dates.isSameDay(date1, nextDayDate));

            assertTrue(Dates.isSameDay(cal1, cal3));
            Calendar nextDayCal = Dates.addDays(cal1, 1);
            assertFalse(Dates.isSameDay(cal1, nextDayCal));
        }

        @Test
        public void testIsSameMonth() {
            assertTrue(Dates.isSameMonth(date1, date3));
            java.util.Date nextMonthDate = Dates.addMonths(date1, 1);
            assertFalse(Dates.isSameMonth(date1, nextMonthDate));

            assertTrue(Dates.isSameMonth(cal1, cal3));
            Calendar nextMonthCal = Dates.addMonths(cal1, 1);
            assertFalse(Dates.isSameMonth(cal1, nextMonthCal));
        }

        @Test
        public void testIsSameYear() {
            assertTrue(Dates.isSameYear(date1, date3));
            java.util.Date nextYearDate = Dates.addYears(date1, 1);
            assertFalse(Dates.isSameYear(date1, nextYearDate));

            assertTrue(Dates.isSameYear(cal1, cal3));
            Calendar nextYearCal = Dates.addYears(cal1, 1);
            assertFalse(Dates.isSameYear(cal1, nextYearCal));
        }

        @Test
        public void testIsSameInstant() {
            assertTrue(Dates.isSameInstant(date1, new java.util.Date(TEST_EPOCH_MILLIS)));
            assertFalse(Dates.isSameInstant(date1, date2));

            assertTrue(Dates.isSameInstant(cal1, getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE)));
            assertFalse(Dates.isSameInstant(cal1, cal2));
            assertTrue(Dates.isSameInstant(cal1, cal4));
        }

        @Test
        public void testIsSameLocalTime() {
            Calendar cal1Local = Calendar.getInstance();
            cal1Local.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            cal1Local.set(Calendar.MILLISECOND, 500);

            Calendar cal2Local = Calendar.getInstance();
            cal2Local.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            cal2Local.set(Calendar.MILLISECOND, 500);
            if (cal1Local.getClass() == cal2Local.getClass()) {
                assertTrue(Dates.isSameLocalTime(cal1Local, cal2Local));
            }

            Calendar cal3Local = Calendar.getInstance();
            cal3Local.set(2023, Calendar.OCTOBER, 26, 11, 15, 30);
            cal3Local.set(Calendar.MILLISECOND, 500);
            assertFalse(Dates.isSameLocalTime(cal1Local, cal3Local));

            Calendar cal1Utc = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE);
            Calendar calPstSameLocal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
            calPstSameLocal.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            calPstSameLocal.set(Calendar.MILLISECOND, 500);

            assertFalse(Dates.isSameLocalTime(cal1, cal4));

            Calendar c1 = Calendar.getInstance();
            c1.set(2023, 0, 1, 10, 0, 0);
            c1.set(Calendar.MILLISECOND, 0);
            Calendar c2 = Calendar.getInstance();
            c2.set(2023, 0, 1, 10, 0, 0);
            c2.set(Calendar.MILLISECOND, 0);
            if (c1.getClass() == c2.getClass()) {
                assertTrue(Dates.isSameLocalTime(c1, c2));
            }
        }

        @Test
        public void testComparisonNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(null, cal2, Calendar.SECOND));
            assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(cal1, null, Calendar.SECOND));
            assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(null, date2));
            assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(cal1, null));
        }
    }

    @Nested
    public class FragmentTests {
        private final java.util.Date d = Dates.parseJUDate("2023-10-26 10:15:30.500", Dates.LOCAL_TIMESTAMP_FORMAT);
        private final Calendar c = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE);

        @Test
        public void testGetFragmentInMilliseconds() {
            assertEquals(500, Dates.getFragmentInMilliseconds(d, CalendarField.SECOND));
            assertEquals(30 * 1000 + 500, Dates.getFragmentInMilliseconds(d, CalendarField.MINUTE));
            assertEquals(15 * 60 * 1000 + 30 * 1000 + 500, Dates.getFragmentInMilliseconds(d, CalendarField.HOUR_OF_DAY));

            assertEquals(500, Dates.getFragmentInMilliseconds(c, CalendarField.SECOND));
        }

        @Test
        public void testGetFragmentInSeconds() {
            assertEquals(30, Dates.getFragmentInSeconds(d, CalendarField.MINUTE));
            assertEquals(15 * 60 + 30, Dates.getFragmentInSeconds(d, CalendarField.HOUR_OF_DAY));
            long expectedSecondsInDay = (10 * 3600) + (15 * 60) + 30;
            assertEquals(expectedSecondsInDay, Dates.getFragmentInSeconds(d, CalendarField.DAY_OF_MONTH));

            assertEquals(30, Dates.getFragmentInSeconds(c, CalendarField.MINUTE));
        }

        @Test
        public void testGetFragmentInMinutes() {
            assertEquals(15, Dates.getFragmentInMinutes(d, CalendarField.HOUR_OF_DAY));
            long expectedMinutesInDay = (10 * 60) + 15;
            assertEquals(expectedMinutesInDay, Dates.getFragmentInMinutes(d, CalendarField.DAY_OF_MONTH));

            assertEquals(15, Dates.getFragmentInMinutes(c, CalendarField.HOUR_OF_DAY));
        }

        @Test
        public void testGetFragmentInHours() {
            assertEquals(10, Dates.getFragmentInHours(d, CalendarField.DAY_OF_MONTH));
            assertEquals((c.get(Calendar.DAY_OF_MONTH) - 1) * 24 + 10, Dates.getFragmentInHours(d, CalendarField.MONTH));
            assertEquals(10, Dates.getFragmentInHours(c, CalendarField.DAY_OF_MONTH));
        }

        @Test
        public void testGetFragmentInDays() {
            assertEquals(c.get(Calendar.DAY_OF_MONTH), Dates.getFragmentInDays(d, CalendarField.MONTH));
            assertEquals(c.get(Calendar.DAY_OF_YEAR), Dates.getFragmentInDays(d, CalendarField.YEAR));

            assertEquals(c.get(Calendar.DATE), Dates.getFragmentInDays(c, CalendarField.MONTH));
        }

        @Test
        public void testFragmentNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInMilliseconds((Calendar) null, CalendarField.SECOND));
            assertThrows(NullPointerException.class, () -> Dates.getFragmentInSeconds(c, null));
        }
    }

    @Nested
    public class UtilityMethodsTests {
        java.util.Date date1 = Dates.parseJUDate("2023-02-28", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);
        java.util.Date date2 = Dates.parseJUDate("2024-02-29", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);
        java.util.Date date3 = Dates.parseJUDate("2023-03-30", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);
        java.util.Date date4 = Dates.parseJUDate("2023-12-31", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);
        java.util.Date date5 = Dates.parseJUDate("2023-12-30", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);

        @Test
        public void testIsLastDateOfMonth() {
            assertTrue(Dates.isLastDateOfMonth(date1));
            assertTrue(Dates.isLastDateOfMonth(date2));
            assertFalse(Dates.isLastDateOfMonth(date3));
            assertTrue(Dates.isLastDateOfMonth(date4));
            assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfMonth(null));
        }

        @Test
        public void testIsLastDateOfYear() {
            assertFalse(Dates.isLastDateOfYear(date1));
            assertTrue(Dates.isLastDateOfYear(date4));
            assertFalse(Dates.isLastDateOfYear(date5));
            assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfYear(null));
        }

        @Test
        public void testGetLastDateOfMonth() {
            assertEquals(28, Dates.getLastDateOfMonth(date1));
            assertEquals(29, Dates.getLastDateOfMonth(date2));
            assertEquals(31, Dates.getLastDateOfMonth(date3));
            assertEquals(31, Dates.getLastDateOfMonth(date4));
            assertThrows(IllegalArgumentException.class, () -> Dates.getLastDateOfMonth(null));
        }

        @Test
        public void testGetLastDateOfYear() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(date1);
            assertEquals(365, Dates.getLastDateOfYear(date1));
            cal.setTime(date2);
            assertEquals(366, Dates.getLastDateOfYear(date2));
            assertThrows(IllegalArgumentException.class, () -> Dates.getLastDateOfYear(null));
        }

        @Test
        public void testIsOverlap() {
            java.util.Date s1 = Dates.parseJUDate("2023-01-01");
            java.util.Date e1 = Dates.parseJUDate("2023-01-10");
            java.util.Date s2 = Dates.parseJUDate("2023-01-05");
            java.util.Date e2 = Dates.parseJUDate("2023-01-15");
            java.util.Date s3 = Dates.parseJUDate("2023-01-12");
            java.util.Date e3 = Dates.parseJUDate("2023-01-20");

            assertTrue(Dates.isOverlap(s1, e1, s2, e2));
            assertTrue(Dates.isOverlap(s2, e2, s1, e1));
            assertFalse(Dates.isOverlap(s1, e1, s3, e3));
            assertTrue(Dates.isOverlap(s1, e1, s1, e1));
            assertTrue(Dates.isOverlap(s1, e2, s2, e1));

            java.util.Date s4 = Dates.parseJUDate("2023-01-10");
            java.util.Date e4 = Dates.parseJUDate("2023-01-15");
            assertFalse(Dates.isOverlap(s1, e1, s4, e4));

            assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(null, e1, s2, e2));
            assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(e1, s1, s2, e2));
        }

        @Test
        public void testIsBetween() {
            java.util.Date date = Dates.parseJUDate("2023-01-05");
            java.util.Date start = Dates.parseJUDate("2023-01-01");
            java.util.Date end = Dates.parseJUDate("2023-01-10");

            assertTrue(Dates.isBetween(date, start, end));
            assertTrue(Dates.isBetween(start, start, end));
            assertTrue(Dates.isBetween(end, start, end));

            java.util.Date before = Dates.parseJUDate("2022-12-31");
            java.util.Date after = Dates.parseJUDate("2023-01-11");
            assertFalse(Dates.isBetween(before, start, end));
            assertFalse(Dates.isBetween(after, start, end));

            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, start, end));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, null, end));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, start, null));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, end, start));
        }
    }

    @Nested
    public class DTFClassTests {
        private final ZonedDateTime testZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.UTC_ZONE_ID);
        private final LocalDateTime testLocalDateTime = testZonedDateTime.toLocalDateTime();
        private final LocalDate testLocalDate = testZonedDateTime.toLocalDate();
        private final LocalTime testLocalTime = testZonedDateTime.toLocalTime();
        private final OffsetDateTime testOffsetDateTime = testZonedDateTime.toOffsetDateTime();

        @Test
        public void testDTFFormatDate() {
            assertEquals("2023-10-26T10:15:30Z", Dates.DTF.ISO_8601_DATE_TIME.format(TEST_JU_DATE));
            assertEquals("2023-10-26T10:15:30.500Z", Dates.DTF.ISO_8601_TIMESTAMP.format(TEST_SQL_TIMESTAMP));
            assertNull(Dates.DTF.ISO_LOCAL_DATE_TIME.format((java.util.Date) null));
        }

        @Test
        public void testDTFFormatCalendar() {
            assertEquals("2023-10-26", Dates.DTF.LOCAL_DATE.format(testCalendar));
            assertNull(Dates.DTF.ISO_LOCAL_DATE_TIME.format((Calendar) null));
        }

        @Test
        public void testDTFFormatTemporalAccessor() {
            assertEquals("2023-10-26", Dates.DTF.LOCAL_DATE.format(testLocalDate));
            assertEquals("10:15:30", Dates.DTF.LOCAL_TIME.format(testLocalTime.withNano(0)));
            assertEquals("2023-10-26T10:15:30", Dates.DTF.ISO_LOCAL_DATE_TIME.format(testLocalDateTime.withNano(0)));
            assertNull(Dates.DTF.ISO_LOCAL_DATE_TIME.format((TemporalAccessor) null));
        }

        @Test
        public void testDTFFormatTo() {
            StringBuilder sb = new StringBuilder();
            Dates.DTF.RFC_1123_DATE_TIME.formatTo(TEST_JU_DATE, sb);
            sb.setLength(0);
            Dates.DTF.ISO_8601_DATE_TIME.formatTo(TEST_JU_DATE, sb);
            assertEquals("2023-10-26T10:15:30Z", sb.toString());

            sb.setLength(0);
            Dates.DTF.ISO_LOCAL_DATE_TIME.formatTo((TemporalAccessor) null, sb);
            assertEquals("null", sb.toString());
        }

        @Test
        public void testDTFParseToLocalDate() {
            assertEquals(testLocalDate, Dates.DTF.LOCAL_DATE.parseToLocalDate("2023-10-26"));
            assertEquals(testLocalDate, Dates.DTF.ISO_8601_DATE_TIME.parseToLocalDate("2023-10-26T10:15:30Z"));
            assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.LOCAL_DATE.parseToLocalDate(Long.toString(TEST_EPOCH_MILLIS)));
            assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(""));
            assertThrows(DateTimeParseException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDate("invalid"));
        }

        @Test
        public void testDTFParseToLocalTime() {
            assertEquals(testLocalTime.withNano(0), Dates.DTF.LOCAL_TIME.parseToLocalTime("10:15:30"));
            assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.LOCAL_TIME.parseToLocalTime(Long.toString(TEST_EPOCH_MILLIS)));
            assertNull(Dates.DTF.LOCAL_TIME.parseToLocalTime(null));
        }

        @Test
        public void testDTFParseToLocalDateTime() {
            assertEquals(testLocalDateTime.withNano(0), Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2023-10-26 10:15:30"));
            assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime(Long.toString(TEST_EPOCH_MILLIS)));
        }

        @Test
        public void testDTFParseToOffsetDateTime() {
            String offsetStr = "2023-10-26T10:15:30+02:00";
            OffsetDateTime expectedODT = OffsetDateTime.of(2023, 10, 26, 10, 15, 30, 0, ZoneOffset.ofHours(2));
            assertEquals(expectedODT, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetStr));
            assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(Long.toString(TEST_EPOCH_MILLIS)));
        }

        @Test
        public void testDTFParseToZonedDateTime() {
            String zonedStr = "2023-10-26T10:15:30Z[UTC]";
            ZonedDateTime expectedZDT_UTC = ZonedDateTime.of(2023, 10, 26, 10, 15, 30, 0, ZoneId.of("UTC"));

            assertEquals(expectedZDT_UTC, Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedStr));
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(Long.toString(TEST_EPOCH_MILLIS)));
            assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.ISO_8601_TIMESTAMP.parseToZonedDateTime("2023-10-26T10:15:30.500Z"));
        }

        @Test
        public void testDTFParseToInstant() {
            assertEquals(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DTF.ISO_8601_TIMESTAMP.parseToInstant("2023-10-26T10:15:30.500Z"));
            assertEquals(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DTF.ISO_8601_TIMESTAMP.parseToInstant(Long.toString(TEST_EPOCH_MILLIS)));
        }

        @Test
        public void testDTFParseToJUDate() {
            assertEquals(TEST_EPOCH_MILLIS, Dates.DTF.ISO_8601_TIMESTAMP.parseToJUDate("2023-10-26T10:15:30.500Z").getTime());
            assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_8601_TIMESTAMP.parseToJUDate(Long.toString(TEST_EPOCH_MILLIS)).getTime());
            java.util.Date parsedWithTZ = Dates.DTF.ISO_LOCAL_DATE_TIME.parseToJUDate("2023-10-26T10:15:30", TimeZone.getTimeZone("PST"));
            Calendar calPST = Calendar.getInstance(TimeZone.getTimeZone("PST"));
            calPST.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            calPST.set(Calendar.MILLISECOND, 0);
            assertEquals(calPST.getTimeInMillis(), parsedWithTZ.getTime());
        }

        @Test
        public void testDTFParseToSqlDate() {
            java.sql.Date sqlDate = Dates.DTF.LOCAL_DATE.parseToDate("2023-10-26");
            Calendar cal = Calendar.getInstance();
            cal.set(2023, Calendar.OCTOBER, 26, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTimeInMillis(), sqlDate.getTime());
        }

        @Test
        public void testDTFParseToSqlTime() {
            java.sql.Time sqlTime = Dates.DTF.LOCAL_TIME.parseToTime("10:15:30");
            Calendar cal = Calendar.getInstance();
            cal.set(1970, Calendar.JANUARY, 1, 10, 15, 30);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTimeInMillis(), sqlTime.getTime());
        }

        @Test
        public void testDTFParseToSqlTimestamp() {
            java.sql.Timestamp ts = Dates.DTF.ISO_8601_TIMESTAMP.parseToTimestamp("2023-10-26T10:15:30.500Z");
            assertEquals(TEST_EPOCH_MILLIS, ts.getTime());
            assertEquals(500_000_000, ts.getNanos());
        }

        @Test
        public void testDTFParseToCalendar() {
            Calendar cal = Dates.DTF.ISO_8601_DATE_TIME.parseToCalendar("2023-10-26T10:15:30Z");
            assertEquals(TEST_EPOCH_MILLIS - 500, cal.getTimeInMillis());
            assertEquals(Dates.DEFAULT_TIME_ZONE.getID(), cal.getTimeZone().getID());
        }

        @Test
        public void testDTFToString() {
            assertEquals(Dates.LOCAL_DATE_FORMAT, Dates.DTF.LOCAL_DATE.toString());
            assertEquals(Dates.ISO_8601_TIMESTAMP_FORMAT, Dates.DTF.ISO_8601_TIMESTAMP.toString());
        }
    }
}
