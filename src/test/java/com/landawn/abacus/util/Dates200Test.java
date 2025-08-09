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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Dates200Test extends TestBase {

    private static final long TEST_EPOCH_MILLIS = 1698315330500L; // 2023-10-26 10:15:30.500 GMT
    private static final java.util.Date TEST_JU_DATE = new java.util.Date(TEST_EPOCH_MILLIS);
    private static final java.sql.Date TEST_SQL_DATE = new java.sql.Date(TEST_EPOCH_MILLIS);
    private static final java.sql.Time TEST_SQL_TIME = new java.sql.Time(TEST_EPOCH_MILLIS); // Note: Time discards date part
    private static final java.sql.Timestamp TEST_SQL_TIMESTAMP = new java.sql.Timestamp(TEST_EPOCH_MILLIS);

    private static Calendar testCalendar;
    private static GregorianCalendar testGregorianCalendar;
    private static XMLGregorianCalendar testXMLGregorianCalendar;
    private static DatatypeFactory datatypeFactoryInstance;

    // Helper method to get a Calendar instance for a specific epoch millis and timezone
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
            assertEquals("EEE, dd MMM yyyy HH:mm:ss zzz", Dates.RFC_1123_DATE_TIME_FORMAT); // Note: year pattern in file is yyyy not yyyy
        }

        @Test
        public void testSemiMonth() {
            assertEquals(1001, Dates.SEMI_MONTH);
        }
    }

    // Helper classes for registration tests
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
        } // Default constructor needed for some reflection paths
          // public CustomCalendar(long millis) { super(millis); this.customConstructorCalled = true;} // Not directly used by BiFunction

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

    //    @Nested
    //    public class CreatorRegistrationTests {
    //
    //        @Test
    //        public void testRegisterDateCreator() {
    //            LongFunction<CustomDate> creator = CustomDate::new;
    //            assertTrue(Dates.registerDateCreator(CustomDate.class, creator));
    //
    //            // Test that the custom creator is used (indirectly, by checking if the creation works)
    //            // The 'createDate(long, Class)' method is private, so we can't directly test its usage.
    //            // We assume it's used by public methods if registration affects them.
    //            // For now, we just test registration success.
    //            // We also test the condition for not overwriting existing non-java/javax/com.landawn.abacus creators
    //            LongFunction<MyOwnDate> myOwnCreator = MyOwnDate::new;
    //            assertTrue(Dates.registerDateCreator(MyOwnDate.class, myOwnCreator)); // First time should be true
    //            assertFalse(Dates.registerDateCreator(MyOwnDate.class, MyOwnDate::new)); // Second time for custom non-standard should be false
    //
    //            // Clean up (if possible, though static map modification is tricky to isolate)
    //            // This might require a way to unregister or reset, or run tests in separate JVMs.
    //            // For now, accept potential side-effects for subsequent tests using these custom types.
    //        }
    //
    //        @Test
    //        public void testRegisterCalendarCreator() {
    //            BiFunction<Long, Calendar, CustomCalendar> creator = (millis, template) -> {
    //                CustomCalendar cc = new CustomCalendar();
    //                cc.setTimeInMillis(millis);
    //                if (template != null)
    //                    cc.setTimeZone(template.getTimeZone());
    //                cc.setCustomFlag(true);
    //                return cc;
    //            };
    //            assertTrue(Dates.registerCalendarCreator(CustomCalendar.class, creator));
    //
    //            BiFunction<Long, Calendar, MyOwnCalendar> myOwnCreator = (m, t) -> new MyOwnCalendar();
    //            assertTrue(Dates.registerCalendarCreator(MyOwnCalendar.class, myOwnCreator));
    //            assertFalse(Dates.registerCalendarCreator(MyOwnCalendar.class, (m, t) -> new MyOwnCalendar()));
    //        }
    //    }

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
            assertTrue(Math.abs(rolled.getTime() - (now + 1000)) < 50); // Allow small delta
        }

        @Test
        public void testCurrentDateRolled() {
            long now = System.currentTimeMillis();
            java.sql.Date rolled = Dates.currentDateRolled(1, TimeUnit.DAYS);
            // Date part should be one day ahead, time part will be 00:00:00 for sql.Date
            Calendar calNow = Calendar.getInstance();
            calNow.setTimeInMillis(now);
            Calendar calRolled = Calendar.getInstance();
            calRolled.setTime(rolled);

            Calendar expectedCal = Calendar.getInstance();
            expectedCal.setTimeInMillis(now + TimeUnit.DAYS.toMillis(1));
            // Normalize to date part for sql.Date comparison
            expectedCal.set(Calendar.HOUR_OF_DAY, 0);
            expectedCal.set(Calendar.MINUTE, 0);
            expectedCal.set(Calendar.SECOND, 0);
            expectedCal.set(Calendar.MILLISECOND, 0);
            calRolled.set(Calendar.HOUR_OF_DAY, 0); // SQL Date only cares about date part
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
        // java.util.Date
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

        // java.sql.Date
        @Test
        public void testCreateDateFromCalendar() {
            assertEquals(TEST_SQL_DATE.getTime(), Dates.createDate(testCalendar).getTime()); // SQL Date normalizes time part
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

        // java.sql.Time
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

        // java.sql.Timestamp
        @Test
        public void testCreateTimestampFromCalendar() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(testCalendar).getTime());
            assertEquals(TEST_SQL_TIMESTAMP.getNanos(), Dates.createTimestamp(testCalendar).getNanos());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
        }

        @Test
        public void testCreateTimestampFromDate() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(TEST_JU_DATE).getTime());
            // JU Date doesn't have nanos beyond millis, so nanos part will be based on millis.
            assertEquals(TEST_SQL_TIMESTAMP.getNanos() / 1_000_000 * 1_000_000, Dates.createTimestamp(TEST_JU_DATE).getNanos());
            assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
        }

        @Test
        public void testCreateTimestampFromLong() {
            assertEquals(TEST_SQL_TIMESTAMP.getTime(), Dates.createTimestamp(TEST_EPOCH_MILLIS).getTime());
            assertEquals(TEST_SQL_TIMESTAMP.getNanos(), Dates.createTimestamp(TEST_EPOCH_MILLIS).getNanos());
        }

        // Calendar
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

        // GregorianCalendar (similar tests as Calendar)
        @Test
        public void testCreateGregorianCalendarFromCalendar() {
            GregorianCalendar created = Dates.createGregorianCalendar(testCalendar);
            assertEquals(testCalendar.getTimeInMillis(), created.getTimeInMillis());
            // assertEquals(testCalendar.getTimeZone(), created.getTimeZone()); // This might differ by default if testCalendar is not Gregorian
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

        // XMLGregorianCalendar
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
            assertNotEquals(pst.getRawOffset(), created.toGregorianCalendar().getTimeZone().getRawOffset()); // Check timezone by offset
        }
    }

    @Nested
    public class ParseStringTests {
        // Test Date: 2023-10-26T10:15:30.500Z (from TEST_EPOCH_MILLIS)
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
            assertEquals(TEST_EPOCH_MILLIS - 500, Dates.parseJUDate(isoDateTimeStr).getTime()); // No millis in string
            assertEquals(TEST_EPOCH_MILLIS, Dates.parseJUDate(Long.toString(TEST_EPOCH_MILLIS)).getTime());

            java.util.Date parsedDate = Dates.parseJUDate(localDateStr, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedDate);
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
            assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));

            assertThrows(IllegalArgumentException.class, () -> Dates.parseJUDate("invalid date"));
            assertThrows(RuntimeException.class, () -> Dates.parseJUDate(isoTimestampStr, Dates.ISO_8601_TIMESTAMP_FORMAT, TimeZone.getTimeZone("PST"))); // Mismatch Z and PST
        }

        @Test
        public void testParseDate() { // java.sql.Date
            assertNull(Dates.parseDate(null));
            java.sql.Date parsedSqlDate = Dates.parseDate(localDateStr, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedSqlDate); // sql.Date will have time part zeroed
            assertEquals(2023, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH));
            assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testParseTime() { // java.sql.Time
            assertNull(Dates.parseTime(null));
            // For java.sql.Time, the date components are set to January 1, 1970.
            // The parse method uses the full date string if provided, then extracts time.
            java.sql.Time parsedSqlTime = Dates.parseTime(localTimeStr, Dates.LOCAL_TIME_FORMAT, Dates.UTC_TIME_ZONE);
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(parsedSqlTime);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(15, cal.get(Calendar.MINUTE));
            assertEquals(30, cal.get(Calendar.SECOND));

            // Test parsing a full datetime string into a Time object
            java.sql.Time parsedFromDateTime = Dates.parseTime(isoDateTimeStr); // Assumes UTC
            Calendar calFromDT = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            calFromDT.setTime(parsedFromDateTime);
            assertEquals(10, calFromDT.get(Calendar.HOUR_OF_DAY)); // Check time parts
            assertEquals(15, calFromDT.get(Calendar.MINUTE));
            assertEquals(30, calFromDT.get(Calendar.SECOND));
            assertEquals(2023, calFromDT.get(Calendar.YEAR)); // Date part should be epoch
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
        // Test Date: 2023-10-26T10:15:30.500Z (from TEST_EPOCH_MILLIS)
        // Corresponding local date in UTC: 2023-10-26
        // Corresponding local date/time in UTC: 2023-10-26 10:15:30

        @Test
        public void testFormatLocalDate() { // This formats current date, so tricky to assert exact string
            String formatted = Dates.formatLocalDate();
            assertNotNull(formatted);
            // Expected format yyyy-MM-dd
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
        }

        @Test
        public void testFormatLocalDateTime() { // This formats current date/time
            String formatted = Dates.formatLocalDateTime();
            assertNotNull(formatted);
            // Expected format yyyy-MM-dd HH:mm:ss
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        public void testFormatCurrentDateTime() { // yyyy-MM-dd'T'HH:mm:ss'Z'
            String formatted = Dates.formatCurrentDateTime();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
        }

        @Test
        public void testFormatCurrentTimestamp() { // yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
            String formatted = Dates.formatCurrentTimestamp();
            assertNotNull(formatted);
            assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
        }

        @Test
        public void testFormatDate() {
            assertEquals("2023-10-26T10:15:30.500Z", Dates.format(TEST_SQL_TIMESTAMP)); // Default for Timestamp
            assertEquals("2023-10-26T10:15:30Z", Dates.format(TEST_JU_DATE)); // Default for Date (no millis)

            assertEquals("2023-10-26", Dates.format(TEST_JU_DATE, Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
            assertEquals("10:15:30", Dates.format(TEST_JU_DATE, Dates.LOCAL_TIME_FORMAT, Dates.UTC_TIME_ZONE));
        }

        @Test
        public void testFormatCalendar() {
            // Default format for Calendar should be ISO_8601_DATE_TIME_FORMAT (fastDateFormat)
            assertEquals("2023-10-26T10:15:30Z", Dates.format(testCalendar));
            assertEquals("2023/10/26", Dates.format(testCalendar, "yyyy/MM/dd", Dates.UTC_TIME_ZONE));
        }

        @Test
        public void testFormatXMLGregorianCalendar() {
            if (testXMLGregorianCalendar == null) {
                System.out.println("Skipping testFormatXMLGregorianCalendar due to TestObject init failure.");
                return;
            }
            // Default format for XMLGregorianCalendar should be ISO_8601_DATE_TIME_FORMAT (fastDateFormat)
            assertEquals("2023-10-26T10:15:30Z", Dates.format(testXMLGregorianCalendar)); // Assuming fastDateFormat is used
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
            assertEquals("2023-10-26T10:15:30Z", sb.toString()); // Uses fastDateFormat

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
            assertEquals("2023-10-26T10:15:30Z", sb.toString()); // Uses fastDateFormat

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
        // Base date for set operations: 2023-10-26 10:15:30.500 GMT
        private java.util.Date baseDate() {
            return new java.util.Date(TEST_EPOCH_MILLIS);
        }

        @Test
        public void testSetYears() {
            java.util.Date newDate = Dates.setYears(baseDate(), 2025);
            Calendar cal = getCalendarForMillis(newDate.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(2025, cal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH)); // Check other fields remain
        }

        @Test
        public void testSetMonths() { // Month is 0-indexed for Calendar.set, but amounts here are 0-11 for January-December
            java.util.Date newDate = Dates.setMonths(baseDate(), Calendar.JANUARY); // Set to January
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
            java.util.Date newDate = Dates.setHours(baseDate(), 5); // 5 AM
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
            // return Dates.parseJUDate("2023-10-26 10:15:30.500", Dates.LOCAL_TIMESTAMP_FORMAT);
        } // 2023-10-26 10:15:30.500 GMT

        private Calendar baseCalendar() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTimeInMillis(TEST_EPOCH_MILLIS);
            return cal;
        }

        // Roll with TimeUnit
        @Test
        public void testRollDateWithTimeUnit() {
            java.util.Date rolled = Dates.roll(baseDate(), 1, TimeUnit.DAYS);
            assertEquals(TEST_EPOCH_MILLIS + TimeUnit.DAYS.toMillis(1), rolled.getTime());
        }

        // Roll with CalendarField
        @Test
        public void testRollDateWithCalendarField() {
            // DAY, WEEK, HOUR, MINUTE, SECOND, MILLISECOND are millisecond based
            java.util.Date rolledDay = Dates.roll(baseDate(), 2, CalendarField.DAY);
            assertEquals(TEST_EPOCH_MILLIS + 2 * 24 * 60 * 60 * 1000L, rolledDay.getTime());

            // MONTH and YEAR use Calendar.add

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

        // Add methods (delegate to roll with CalendarField)
        @Test
        public void testAddYearsDate() {
            Dates.addYears(baseDate(), 2); // Should be equivalent to roll by 2 years
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
            cal.add(Calendar.WEEK_OF_YEAR, 1); // Or use direct milli calculation
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

        // Calendar add methods
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
            assertEquals(expected.getTimeInMillis(), Dates.addDays(baseCalendar(), 10).getTimeInMillis());
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
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 1, CalendarField.DAY));
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 1, TimeUnit.DAYS));
            assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 1, CalendarField.DAY));
        }
    }

    @Nested
    public class ManipulationRoundTruncateCeilingTests {
        // Date: 2023-10-26 10:15:30.500 GMT
        private java.util.Date baseDate() {
            return new java.util.Date(TEST_EPOCH_MILLIS);
        }

        private Calendar baseCalendar() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTimeInMillis(TEST_EPOCH_MILLIS);
            return cal;
        }

        // Round
        @Test
        public void testRoundDate() {
            // Round to nearest hour: 2023-10-26 10:00:00.000 (since 15 min is less than 30)
            java.util.Date roundedHour = Dates.round(baseDate(), Calendar.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(roundedHour.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
            assertEquals(0, cal.get(Calendar.MILLISECOND));

            // Round to nearest minute: 2023-10-26 10:16:00.000 (since 30.500 sec rounds up)
            java.util.Date roundedMin = Dates.round(baseDate(), Calendar.MINUTE);
            cal.setTime(roundedMin);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(16, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
        }

        @Test
        public void testRoundDateWithCalendarField() {
            java.util.Date rounded = Dates.round(baseDate(), CalendarField.HOUR);
            Calendar cal = getCalendarForMillis(rounded.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
        }

        @Test
        public void testRoundCalendar() {
            Calendar roundedCal = Dates.round(baseCalendar(), Calendar.DAY_OF_MONTH);
            // 2023-10-26 00:00:00.000 (10h is less than 12h)
            assertEquals(2023, roundedCal.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, roundedCal.get(Calendar.MONTH));
            assertEquals(26, roundedCal.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, roundedCal.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testRoundCalendarWithCalendarField() {
            Calendar rounded = Dates.round(baseCalendar(), CalendarField.DAY);
            assertEquals(0, rounded.get(Calendar.HOUR_OF_DAY)); // Assuming day means start of day
        }

        // Truncate
        @Test
        public void testTruncateDate() {
            // Truncate to hour: 2023-10-26 10:00:00.000
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
            // 2023-10-01 00:00:00.000
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

        // Ceiling
        @Test
        public void testCeilingDate() {
            // Ceiling to hour: 2023-10-26 11:00:00.000 (since 10:15 -> 11:00)
            java.util.Date ceiled = Dates.ceiling(baseDate(), Calendar.HOUR_OF_DAY);
            Calendar cal = getCalendarForMillis(ceiled.getTime(), Dates.UTC_TIME_ZONE);
            assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(0, cal.get(Calendar.MINUTE));
        }

        @Test
        public void testCeilingDateWithCalendarField() {
            java.util.Date ceiled = Dates.ceiling(baseDate(), CalendarField.MINUTE);
            Calendar cal = getCalendarForMillis(ceiled.getTime(), Dates.UTC_TIME_ZONE); // 10:15:30.500 -> 10:16:00.000
            assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(16, cal.get(Calendar.MINUTE));
            assertEquals(0, cal.get(Calendar.SECOND));
        }

        @Test
        public void testCeilingCalendar() {
            Calendar ceiled = Dates.ceiling(baseCalendar(), Calendar.DAY_OF_MONTH);
            // 2023-10-26 10:15... -> 2023-10-27 00:00:00.000 (next day if not already start of day)
            assertEquals(2023, ceiled.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, ceiled.get(Calendar.MONTH));
            assertEquals(27, ceiled.get(Calendar.DAY_OF_MONTH));
            assertEquals(0, ceiled.get(Calendar.HOUR_OF_DAY));
        }

        @Test
        public void testCeilingCalendarWithCalendarField() {
            Calendar base = baseCalendar(); // 2023-10-26 10:15:30.500
            base.set(Calendar.HOUR_OF_DAY, 0);
            base.set(Calendar.MINUTE, 0);
            base.set(Calendar.SECOND, 0);
            base.set(Calendar.MILLISECOND, 0); // Start of 26th
            Calendar ceiled = Dates.ceiling(base, CalendarField.DAY); // Ceiling start of day is start of next day
            assertEquals(2023, ceiled.get(Calendar.YEAR));
            assertEquals(Calendar.OCTOBER, ceiled.get(Calendar.MONTH));
            assertEquals(27, ceiled.get(Calendar.DAY_OF_MONTH)); // Should be 27th if original was not start of day.
                                                                 // If original IS start of day, it means "ceiling to the end of current day + 1ms" then add.
                                                                 // The logic of ceiling is effectively truncate + add 1 unit if not already at boundary.
                                                                 // So 26th 00:00:00 ceilings to 27th 00:00:00
        }

        @Test
        public void testRoundTruncateCeilingNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.HOUR));
            assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, Calendar.MONTH));
            assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, CalendarField.DAY));
        }
    }

    @Nested
    public class ComparisonTests {
        // Base date: 2023-10-26 10:15:30.500 GMT
        Calendar cal1 = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE); // 2023-10-26 10:15:30.500
        Calendar cal2 = getCalendarForMillis(TEST_EPOCH_MILLIS + 100, Dates.UTC_TIME_ZONE); // 2023-10-26 10:15:30.600
        Calendar cal3 = getCalendarForMillis(TEST_EPOCH_MILLIS + TimeUnit.HOURS.toMillis(1), Dates.UTC_TIME_ZONE); // 2023-10-26 11:15:30.500
        Calendar cal4 = getCalendarForMillis(TEST_EPOCH_MILLIS, TimeZone.getTimeZone("PST")); // Same instant, different TZ
        java.util.Date date1 = new java.util.Date(cal1.getTimeInMillis());
        java.util.Date date2 = new java.util.Date(cal2.getTimeInMillis());
        java.util.Date date3 = new java.util.Date(cal3.getTimeInMillis());

        @Test
        public void testTruncatedEquals() {
            assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND)); // Equal up to second
            assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.MILLISECOND));
            assertTrue(Dates.truncatedEquals(cal1, cal3, Calendar.DAY_OF_MONTH));
            assertFalse(Dates.truncatedEquals(cal1, cal3, Calendar.HOUR_OF_DAY));

            assertTrue(Dates.truncatedEquals(date1, date2, CalendarField.SECOND));
            assertFalse(Dates.truncatedEquals(date1, date3, CalendarField.HOUR));
        }

        @Test
        public void testTruncatedCompareTo() {
            assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, Calendar.SECOND));
            assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.MILLISECOND) < 0);
            assertEquals(0, Dates.truncatedCompareTo(cal1, cal3, CalendarField.DAY));
            assertTrue(Dates.truncatedCompareTo(cal1, cal3, CalendarField.HOUR) < 0);

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
            assertTrue(Dates.isSameInstant(cal1, cal4)); // Same instant, different TZ
        }

        @Test
        public void testIsSameLocalTime() {
            Calendar cal1Local = Calendar.getInstance();
            cal1Local.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            cal1Local.set(Calendar.MILLISECOND, 500);

            Calendar cal2Local = Calendar.getInstance(); // Same local time fields
            cal2Local.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            cal2Local.set(Calendar.MILLISECOND, 500);
            // Must be same class, Calendar.getInstance() might return GregorianCalendar
            if (cal1Local.getClass() == cal2Local.getClass()) {
                assertTrue(Dates.isSameLocalTime(cal1Local, cal2Local));
            }

            Calendar cal3Local = Calendar.getInstance(); // Different hour
            cal3Local.set(2023, Calendar.OCTOBER, 26, 11, 15, 30);
            cal3Local.set(Calendar.MILLISECOND, 500);
            assertFalse(Dates.isSameLocalTime(cal1Local, cal3Local));

            // Test with different timezones but same local fields (should be false as TZ is part of instant check)
            // isSameLocalTime compares fields, so TZ of calendar object itself is not directly compared,
            // but it can affect fields like ERA if it's different significantly.
            // The method compares ERA, YEAR, DAY_OF_YEAR, HOUR_OF_DAY, MINUTE, SECOND, MILLISECOND and class.
            // So, if cal1 and cal4 have same fields (when printed), it should be true
            // Let's setup cal4 to have same local representation of cal1, even if it means different instant
            Calendar cal1Utc = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE); // 2023-10-26 10:15:30.500 UTC
            Calendar calPstSameLocal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
            calPstSameLocal.set(2023, Calendar.OCTOBER, 26, 10, 15, 30); // 10:15:30.500 PST
            calPstSameLocal.set(Calendar.MILLISECOND, 500);

            // They will have different DAY_OF_YEAR if the date crosses midnight due to TZ.
            // If local field values are same, it will be true.
            // Our cal1 and cal4 from setup are same instant, so local time fields will be different.
            assertFalse(Dates.isSameLocalTime(cal1, cal4)); // cal4 is 2023-10-26 03:15:30.500 PST locally

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
        // Date: 2023-10-26 10:15:30.500 GMT (TEST_EPOCH_MILLIS)
        private final java.util.Date d = Dates.parseJUDate("2023-10-26 10:15:30.500", Dates.LOCAL_TIMESTAMP_FORMAT);
        private final Calendar c = getCalendarForMillis(TEST_EPOCH_MILLIS, Dates.UTC_TIME_ZONE);

        @Test
        public void testGetFragmentInMilliseconds() {
            assertEquals(500, Dates.getFragmentInMilliseconds(d, CalendarField.SECOND)); // Millis of second
            assertEquals(30 * 1000 + 500, Dates.getFragmentInMilliseconds(d, CalendarField.MINUTE)); // Millis of minute
            assertEquals(15 * 60 * 1000 + 30 * 1000 + 500, Dates.getFragmentInMilliseconds(d, CalendarField.HOUR)); // Millis of hour

            assertEquals(500, Dates.getFragmentInMilliseconds(c, CalendarField.SECOND));
        }

        @Test
        public void testGetFragmentInSeconds() {
            assertEquals(30, Dates.getFragmentInSeconds(d, CalendarField.MINUTE)); // Seconds of minute
            assertEquals(15 * 60 + 30, Dates.getFragmentInSeconds(d, CalendarField.HOUR)); // Seconds of hour
            // For DAY_OF_YEAR: 10 hours, 15 minutes, 30 seconds
            long expectedSecondsInDay = (10 * 3600) + (15 * 60) + 30;
            assertEquals(expectedSecondsInDay, Dates.getFragmentInSeconds(d, CalendarField.DAY));

            assertEquals(30, Dates.getFragmentInSeconds(c, CalendarField.MINUTE));
        }

        @Test
        public void testGetFragmentInMinutes() {
            assertEquals(15, Dates.getFragmentInMinutes(d, CalendarField.HOUR)); // Minutes of hour
            // For DAY_OF_YEAR: 10 hours, 15 minutes
            long expectedMinutesInDay = (10 * 60) + 15;
            assertEquals(expectedMinutesInDay, Dates.getFragmentInMinutes(d, CalendarField.DAY));

            assertEquals(15, Dates.getFragmentInMinutes(c, CalendarField.HOUR));
        }

        @Test
        public void testGetFragmentInHours() {
            assertEquals(10, Dates.getFragmentInHours(d, CalendarField.DAY)); // Hours of day
            // For MONTH: Day 26, 10 hours. (Day is 1-based, fragment is 0-based for calculation for days)
            // Day 26 means 25 full days passed + 10 hours of current day.
            // The method calculates hours within the current day if fragment is DAY.
            // If fragment is MONTH, it calculates hours from start of month.
            // c.get(Calendar.DAY_OF_MONTH) = 26. So, (26-1)*24 + 10 hours
            assertEquals((c.get(Calendar.DAY_OF_MONTH) - 1) * 24 + 10, Dates.getFragmentInHours(d, CalendarField.MONTH));
            assertEquals(10, Dates.getFragmentInHours(c, CalendarField.DAY));
        }

        @Test
        public void testGetFragmentInDays() {
            // Day of month is 26. So fragment in days within month is 26-1 = 25 (0-indexed days passed)
            assertEquals(c.get(Calendar.DAY_OF_MONTH), Dates.getFragmentInDays(d, CalendarField.MONTH));
            // Day of year.
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
        java.util.Date date1 = Dates.parseJUDate("2023-02-28", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE); // Last day of Feb (non-leap)
        java.util.Date date2 = Dates.parseJUDate("2024-02-29", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE); // Last day of Feb (leap)
        java.util.Date date3 = Dates.parseJUDate("2023-03-30", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE);
        java.util.Date date4 = Dates.parseJUDate("2023-12-31", Dates.LOCAL_DATE_FORMAT, Dates.DEFAULT_TIME_ZONE); // Last day of year
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
            assertEquals(31, Dates.getLastDateOfMonth(date3)); // March has 31
            assertEquals(31, Dates.getLastDateOfMonth(date4));
            assertThrows(IllegalArgumentException.class, () -> Dates.getLastDateOfMonth(null));
        }

        @Test
        public void testGetLastDateOfYear() {
            Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
            cal.setTime(date1); // 2023 (non-leap)
            assertEquals(365, Dates.getLastDateOfYear(date1));
            cal.setTime(date2); // 2024 (leap)
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

            assertTrue(Dates.isOverlap(s1, e1, s2, e2)); // Overlap
            assertTrue(Dates.isOverlap(s2, e2, s1, e1)); // Commutative
            assertFalse(Dates.isOverlap(s1, e1, s3, e3)); // No overlap
            assertTrue(Dates.isOverlap(s1, e1, s1, e1)); // Self overlap
            assertTrue(Dates.isOverlap(s1, e2, s2, e1)); // Inner contains outer (partial)

            // Edge cases
            java.util.Date s4 = Dates.parseJUDate("2023-01-10"); // e1 == s4 (touching)
            java.util.Date e4 = Dates.parseJUDate("2023-01-15");
            // assertTrue(Dates.isOverlap(s1, e1, s4, e4)); // s1 < e4 (true), s4 < e1 (false) -> this depends on strict inequality in definition
            // Original: startTimeOne.before(endTimeTwo) && startTimeTwo.before(endTimeOne)
            // s1 (Jan1) < e4 (Jan15) = true
            // s4 (Jan10) < e1 (Jan10) = false. Result: false
            // If definition is "touching is not overlapping" this is correct.
            // If definition is "touching IS overlapping" then need <=
            // Current implementation: touching is NOT overlapping.
            assertFalse(Dates.isOverlap(s1, e1, s4, e4));

            assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(null, e1, s2, e2));
            assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(e1, s1, s2, e2)); // Start after end
        }

        @Test
        public void testIsBetween() {
            java.util.Date date = Dates.parseJUDate("2023-01-05");
            java.util.Date start = Dates.parseJUDate("2023-01-01");
            java.util.Date end = Dates.parseJUDate("2023-01-10");

            assertTrue(Dates.isBetween(date, start, end));
            assertTrue(Dates.isBetween(start, start, end)); // Inclusive start
            assertTrue(Dates.isBetween(end, start, end)); // Inclusive end

            java.util.Date before = Dates.parseJUDate("2022-12-31");
            java.util.Date after = Dates.parseJUDate("2023-01-11");
            assertFalse(Dates.isBetween(before, start, end));
            assertFalse(Dates.isBetween(after, start, end));

            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, start, end));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, null, end));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, start, null));
            assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, end, start)); // start after end
        }
    }

    @Nested
    public class DTFClassTests {
        // Test Date: 2023-10-26T10:15:30.500Z (TEST_EPOCH_MILLIS)
        private final ZonedDateTime testZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.UTC_ZONE_ID);
        private final LocalDateTime testLocalDateTime = testZonedDateTime.toLocalDateTime(); // 2023-10-26T10:15:30.500
        private final LocalDate testLocalDate = testZonedDateTime.toLocalDate(); // 2023-10-26
        private final LocalTime testLocalTime = testZonedDateTime.toLocalTime(); // 10:15:30.500
        private final OffsetDateTime testOffsetDateTime = testZonedDateTime.toOffsetDateTime();

        // Format methods
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
            assertEquals("10:15:30", Dates.DTF.LOCAL_TIME.format(testLocalTime.withNano(0))); // LOCAL_TIME format has no millis
            assertEquals("2023-10-26T10:15:30", Dates.DTF.ISO_LOCAL_DATE_TIME.format(testLocalDateTime.withNano(0))); // .withNano(0) to match format
            assertNull(Dates.DTF.ISO_LOCAL_DATE_TIME.format((TemporalAccessor) null));
        }

        @Test
        public void testDTFFormatTo() {
            StringBuilder sb = new StringBuilder();
            Dates.DTF.RFC_1123_DATE_TIME.formatTo(TEST_JU_DATE, sb);
            // Expected: Thu, 26 Oct 2023 10:15:30 GMT (using default TZ in SimpleDateFormat if not UTC specified)
            // Dates.format uses a SimpleDateFormat with specified TZ or default.
            // For RFC_1123, 'zzz' implies timezone name.
            // Dates.format(date, format, timeZone)
            // Here DTF uses Dates.format(date, format) which means default timezone
            // To make it predictable, let's use a DTF that has UTC implied
            sb.setLength(0);
            Dates.DTF.ISO_8601_DATE_TIME.formatTo(TEST_JU_DATE, sb);
            assertEquals("2023-10-26T10:15:30Z", sb.toString()); // TEST_JU_DATE is GMT/UTC

            sb.setLength(0);
            Dates.DTF.ISO_LOCAL_DATE_TIME.formatTo((TemporalAccessor) null, sb);
            assertEquals("null", sb.toString());
        }

        // Parse methods
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
            // Test with format that includes offset
            String offsetStr = "2023-10-26T10:15:30+02:00";
            OffsetDateTime expectedODT = OffsetDateTime.of(2023, 10, 26, 10, 15, 30, 0, ZoneOffset.ofHours(2));
            assertEquals(expectedODT, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetStr));
            assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(TEST_EPOCH_MILLIS), Dates.DEFAULT_ZONE_ID),
                    Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(Long.toString(TEST_EPOCH_MILLIS)));
        }

        @Test
        public void testDTFParseToZonedDateTime() {
            String zonedStr = "2023-10-26T10:15:30Z[UTC]"; // DTF.ISO_ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'"
                                                           // For 'Z', XXX is Z, VV is UTC.
            ZonedDateTime expectedZDT_UTC = ZonedDateTime.of(2023, 10, 26, 10, 15, 30, 0, ZoneId.of("UTC"));

            // The parse method in DTF has specific checks for Z, +, - at end of string.
            // For ISO_ZONED_DATE_TIME, it should use its own formatter.
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
            // Test with TimeZone
            java.util.Date parsedWithTZ = Dates.DTF.ISO_LOCAL_DATE_TIME.parseToJUDate("2023-10-26T10:15:30", TimeZone.getTimeZone("PST"));
            Calendar calPST = Calendar.getInstance(TimeZone.getTimeZone("PST"));
            calPST.set(2023, Calendar.OCTOBER, 26, 10, 15, 30);
            calPST.set(Calendar.MILLISECOND, 0);
            assertEquals(calPST.getTimeInMillis(), parsedWithTZ.getTime());
        }

        @Test
        public void testDTFParseToSqlDate() {
            java.sql.Date sqlDate = Dates.DTF.LOCAL_DATE.parseToDate("2023-10-26");
            Calendar cal = Calendar.getInstance(); // Default TZ for parsing if not in string
            cal.set(2023, Calendar.OCTOBER, 26, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            assertEquals(cal.getTimeInMillis(), sqlDate.getTime());
        }

        @Test
        public void testDTFParseToSqlTime() {
            java.sql.Time sqlTime = Dates.DTF.LOCAL_TIME.parseToTime("10:15:30");
            Calendar cal = Calendar.getInstance(); // Date part is 1970-01-01
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
            Calendar cal = Dates.DTF.ISO_8601_DATE_TIME.parseToCalendar("2023-10-26T10:15:30Z"); // Assumes UTC
            assertEquals(TEST_EPOCH_MILLIS - 500, cal.getTimeInMillis()); // String has no millis
            assertEquals(Dates.DEFAULT_TIME_ZONE.getID(), cal.getTimeZone().getID()); // From 'Z'
        }

        @Test
        public void testDTFToString() {
            assertEquals(Dates.LOCAL_DATE_FORMAT, Dates.DTF.LOCAL_DATE.toString());
            assertEquals(Dates.ISO_8601_TIMESTAMP_FORMAT, Dates.DTF.ISO_8601_TIMESTAMP.toString());
        }
    }
}
