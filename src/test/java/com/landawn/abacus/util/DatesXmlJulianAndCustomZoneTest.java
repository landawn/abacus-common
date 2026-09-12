package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

import testfixtures.dates.CountingCreatorTypes.CountingCalendar;
import testfixtures.dates.CountingCreatorTypes.CountingDate;

/**
 * Review pass 8 (2026-09-01).
 *
 * <ul>
 *   <li><b>B1</b> &mdash; {@code createXMLGregorianCalendar} copied the fields of a default-cutover
 *       {@code GregorianCalendar}, so an instant before 1582-10-15 was written with Julian fields
 *       ({@code 1500-03-01T00:00Z} came out as {@code 1500-02-20}) and, because an
 *       {@code XMLGregorianCalendar} converts itself back on a proleptic calendar, read back ten days
 *       early. {@code parseToXMLGregorianCalendar} wrote the proleptic fields for the same instant.</li>
 *   <li><b>B2</b> &mdash; the auto-detected {@code yyyy-MM-dd'T'HH:mm:ss.SSS} shape went through a
 *       {@code DTF} instant parser on {@code parseToTimestamp}/{@code parseToXMLGregorianCalendar},
 *       which has no {@code Calendar} fallback, so a custom-rules {@code TimeZone} was rejected there
 *       while every other target accepted it.</li>
 *   <li><b>B3</b> &mdash; {@code DTF} converted the fallback zone to a {@code ZoneId} up front, so text
 *       that carries its own zone or offset still required the fallback to be representable, and
 *       {@code parseToCalendar} rejected zoned text {@code parseToJUDate} accepted under the same
 *       default zone.</li>
 *   <li><b>B4</b> &mdash; {@code toZoneId}'s fixed-offset fast path recognised only
 *       {@code SimpleTimeZone}; any other fixed-offset {@code TimeZone} subclass with an unregistered ID
 *       was rejected as having daylight-saving rules.</li>
 *   <li><b>B5</b> &mdash; auto-detection accepted the compact {@code +HHmm} offset only when the text had
 *       no fraction.</li>
 *   <li><b>D2</b> &mdash; the {@code truncated*} comparisons built two result objects through the creator
 *       machinery to compare two longs.</li>
 *   <li><b>D1/J1/J2/O2</b> &mdash; the default-zone policy, the {@code "null"} marker on the locale
 *       overloads, the {@code format(Calendar)} zone rejection and the auto-detected failure wording,
 *       pinned so the javadoc cannot drift from them.</li>
 * </ul>
 */
public class DatesXmlJulianAndCustomZoneTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final long MODERN = 1736937045123L; // 2025-01-15T10:30:45.123Z

    private TimeZone originalTimeZone;

    @BeforeEach
    public void rememberDefaults() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(UTC);
    }

    @AfterEach
    public void restoreDefaults() {
        TimeZone.setDefault(originalTimeZone);
    }

    /** Daylight-saving rules under an ID java.time does not know: no ZoneId can express this zone. */
    private static SimpleTimeZone customDstZone() {
        return new SimpleTimeZone(3_600_000, "Custom/Dst", Calendar.MARCH, -1, Calendar.SUNDAY, 7_200_000, Calendar.OCTOBER, -1, Calendar.SUNDAY, 7_200_000,
                3_600_000);
    }

    /** A fixed-offset zone that is neither a SimpleTimeZone nor registered anywhere. */
    private static TimeZone fixedOffsetSubclass(final int offsetMillis, final String id, final boolean daylight) {
        final TimeZone zone = new TimeZone() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int milliseconds) {
                return offsetMillis;
            }

            @Override
            public void setRawOffset(final int offset) {
                // fixed
            }

            @Override
            public int getRawOffset() {
                return offsetMillis;
            }

            @Override
            public boolean useDaylightTime() {
                return daylight;
            }

            @Override
            public boolean inDaylightTime(final Date date) {
                return false;
            }
        };
        zone.setID(id);
        return zone;
    }

    // ---------------------------------------------------------------------------------------------
    // B1 - createXMLGregorianCalendar is proleptic Gregorian
    // ---------------------------------------------------------------------------------------------

    @Test
    public void createXMLGregorianCalendar_isProlepticBefore1582() {
        for (final String text : new String[] { "1000-06-15T12:00:00Z", "1500-03-01T00:00:00Z", "1582-10-04T00:00:00Z", "1582-10-14T00:00:00Z",
                "1582-10-15T00:00:00Z", "1969-12-31T23:59:59Z", "2025-01-15T10:30:45.123Z" }) {
            final long millis = Dates.parseToJUDate(text).getTime();
            final XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(millis, UTC);

            // the value names the instant it was built from, on its own terms
            assertEquals(millis, created.toGregorianCalendar().getTimeInMillis(), text);
            // and is XML-equal to what the parse side writes for that instant (the parse side may carry
            // the fraction at nanosecond scale, so the lexical forms are compared only below)
            assertEquals(DatatypeConstants.EQUAL, created.compare(Dates.parseToXMLGregorianCalendar(text)), text);
            assertEquals(Dates.format(new Date(millis), Dates.LOCAL_DATE_TIME_FORMAT, UTC), Dates.format(created, Dates.LOCAL_DATE_TIME_FORMAT, UTC), text);

            if (!text.contains(".")) {
                assertEquals(Dates.parseToXMLGregorianCalendar(text).toXMLFormat(), created.toXMLFormat(), text);
            }
        }

        final long m1500 = Dates.parseToJUDate("1500-03-01T00:00:00Z").getTime();
        assertEquals("1500-03-01T00:00:00.000Z", Dates.createXMLGregorianCalendar(m1500, UTC).toXMLFormat());
        assertEquals("1500-03-01T00:00:00.000Z", Dates.format(Dates.createXMLGregorianCalendar(m1500, UTC)));

        // every factory shares the one construction path
        assertEquals("1500-03-01T00:00:00.000Z", Dates.createXMLGregorianCalendar(Dates.createCalendar(m1500, UTC)).toXMLFormat());
        assertEquals("1500-03-01T00:00:00.000Z", Dates.createXMLGregorianCalendar(new Date(m1500)).toXMLFormat());
        assertEquals("1500-03-01T00:00:00.000Z", Dates.createXMLGregorianCalendar(m1500).toXMLFormat());

        // a default-cutover calendar as the source no longer leaks its Julian view
        final GregorianCalendar julianView = new GregorianCalendar(UTC);
        julianView.setTimeInMillis(m1500);
        assertEquals(Calendar.FEBRUARY, julianView.get(Calendar.MONTH)); // the legacy calendar itself still says 20 February
        assertEquals("1500-03-01T00:00:00.000Z", Dates.createXMLGregorianCalendar(julianView).toXMLFormat());
    }

    @Test
    public void createXMLGregorianCalendar_before1582_inANonUtcZone() {
        final long m1500 = Dates.parseToJUDate("1500-03-01T00:00:00Z").getTime();
        final TimeZone plusFive = TimeZone.getTimeZone("Etc/GMT-5");
        final XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(m1500, plusFive);

        assertEquals("1500-03-01T05:00:00.000+05:00", created.toXMLFormat());
        assertEquals(m1500, created.toGregorianCalendar().getTimeInMillis());
        assertEquals(300, created.getTimezone());
    }

    /**
     * XML Schema 1.0 writes 1 BCE as year {@code -0001}; a proleptic source gives exactly that, and the
     * value converts back to the instant it came from. (The predefined text patterns stay limited to
     * 0001 through 9999; this documents the factory alone.)
     */
    @Test
    public void createXMLGregorianCalendar_bce_roundTripsAsAnInstant() {
        final long lastDayOf1Bce = Instant.parse("0001-01-01T00:00:00Z").minusSeconds(86_400).toEpochMilli();
        final XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(lastDayOf1Bce, UTC);

        assertEquals("-0001-12-31T00:00:00.000Z", created.toXMLFormat());
        assertEquals(lastDayOf1Bce, created.toGregorianCalendar().getTimeInMillis());
    }

    @Test
    public void createXMLGregorianCalendar_modernValuesAreUnchanged() {
        assertEquals("2025-01-15T10:30:45.123Z", Dates.createXMLGregorianCalendar(MODERN, UTC).toXMLFormat());
        assertEquals("2025-01-15T16:00:45.123+05:30", Dates.createXMLGregorianCalendar(MODERN, TimeZone.getTimeZone("Asia/Kolkata")).toXMLFormat());
        assertEquals(MODERN, Dates.createXMLGregorianCalendar(MODERN, NEW_YORK).toGregorianCalendar().getTimeInMillis());
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(0L, TimeZone.getTimeZone("Africa/Monrovia")));
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - the auto-detected T-separated fraction shape accepts a custom-rules zone on every target
    // ---------------------------------------------------------------------------------------------

    @Test
    public void autoDetectedIsoLocalTimestamp_customZone_agreesAcrossTargets() {
        final SimpleTimeZone custom = customDstZone();
        final String text = "2025-01-15T10:30:45.123";
        final long expected = Dates.parseToJUDate(text, null, custom).getTime();

        assertEquals(Instant.parse("2025-01-15T09:30:45.123Z").toEpochMilli(), expected); // raw +01:00, no DST in January

        final Timestamp timestamp = Dates.parseToTimestamp(text, null, custom);
        assertEquals(expected, timestamp.getTime());
        assertEquals(123_000_000, timestamp.getNanos());
        assertEquals(expected, Dates.parseToXMLGregorianCalendar(text, null, custom).toGregorianCalendar().getTimeInMillis());
        assertEquals(expected, Dates.parseToCalendar(text, null, custom).getTimeInMillis());
        assertEquals(expected, Dates.parseToDate(text, null, custom).getTime());
        assertEquals(expected, Dates.parseToTime(text, null, custom).getTime());

        // the explicit constant was already accepted everywhere, and still is
        assertEquals(expected, Dates.parseToTimestamp(text, Dates.ISO_LOCAL_TIMESTAMP_FORMAT, custom).getTime());
        assertEquals(expected, Dates.parseToJUDate(text, Dates.ISO_LOCAL_TIMESTAMP_FORMAT, custom).getTime());

        // the rerouted path keeps the nanosecond fraction
        final Timestamp nanos = Dates.parseToTimestamp("2025-01-15T10:30:45.123456789", null, custom);
        assertEquals(expected, nanos.getTime());
        assertEquals(123_456_789, nanos.getNanos());
        assertEquals(123_456_789,
                Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789", null, custom).getFractionalSecond().movePointRight(9).intValueExact());

        // the custom daylight-saving rules are applied, exactly as parseToJUDate applies them
        final String summer = "2025-07-15T10:30:45.5";
        assertEquals(Dates.parseToJUDate("2025-07-15T10:30:45", null, custom).getTime() + 500, Dates.parseToTimestamp(summer, null, custom).getTime());
        assertEquals(Instant.parse("2025-07-15T08:30:45.500Z").toEpochMilli(), Dates.parseToTimestamp(summer, null, custom).getTime());
    }

    @Test
    public void autoDetectedIsoLocalTimestamp_registeredZone_isStillStrict() {
        assertEquals(Instant.parse("2025-01-15T15:30:45.123Z"), Dates.parseToTimestamp("2025-01-15T10:30:45.123", null, NEW_YORK).toInstant());
        assertEquals(Instant.parse("2025-01-15T15:30:45.123Z"),
                Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123", null, NEW_YORK).toGregorianCalendar().toInstant());

        final IllegalArgumentException gap = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp("2025-03-09T02:30:00.123", null, NEW_YORK));
        assertTrue(gap.getMessage().contains("Nonexistent local date-time"), gap.getMessage());
        assertTrue(gap.getMessage().contains("the auto-detected format"), gap.getMessage());

        final IllegalArgumentException overlap = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp("2025-11-02T01:30:00.5", null, NEW_YORK));
        assertTrue(overlap.getMessage().contains("Ambiguous local date-time"), overlap.getMessage());

        // a malformed value is still a parse failure, not a zone failure
        final IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123x", null, NEW_YORK));
        assertFalse(malformed.getMessage().contains("ZoneId"), malformed.getMessage());

        // the auto-detected fixed-'Z' sibling shape: the textual 'Z' is data and wins over the fallback
        // zone (2026-09-02); the explicitly supplied constant keeps its conflict check
        assertEquals(MODERN, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", null, NEW_YORK).getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT, NEW_YORK));
        assertEquals(MODERN, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", null, UTC).getTime());
        assertEquals(MODERN, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // B3 - the fallback zone is not consulted when the text carries its own
    // ---------------------------------------------------------------------------------------------

    @Test
    public void fallbackZone_isNotResolvedWhenTheTextCarriesItsOwn() {
        final SimpleTimeZone custom = customDstZone();
        final String zoned = "2025-01-15T10:30:45+05:30[Asia/Kolkata]";
        final String offset = "2025-01-15T10:30:45+05:30";
        final Instant expected = Instant.parse("2025-01-15T05:00:45Z");

        assertEquals(expected, Dates.parseToInstant(zoned, null, custom));
        assertEquals(expected, Dates.parseToInstant(offset, null, custom));
        assertEquals(expected, Dates.parseToOffsetDateTime(offset, null, custom).toInstant());
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), Dates.parseToOffsetDateTime(offset, null, custom).getOffset());
        assertEquals(ZoneId.of("Asia/Kolkata"), Dates.parseToZonedDateTime(zoned, null, custom).getZone());

        assertEquals(expected.toEpochMilli(), Dates.parseToJUDate(zoned, null, custom).getTime());
        assertEquals(expected.toEpochMilli(), Dates.parseToTimestamp(zoned, null, custom).getTime());
        assertEquals(expected.toEpochMilli(), Dates.parseToCalendar(zoned, null, custom).getTimeInMillis());
        assertEquals("Asia/Kolkata", Dates.parseToCalendar(zoned, null, custom).getTimeZone().getID());
        assertEquals(expected.toEpochMilli(), Dates.parseToXMLGregorianCalendar(zoned, null, custom).toGregorianCalendar().getTimeInMillis());

        assertEquals(expected, DTF.ISO_ZONED_DATE_TIME.parseToInstant(zoned, custom));
        assertEquals(expected, DTF.ISO_OFFSET_DATE_TIME.parseToJUDate(offset, custom).toInstant());
        assertEquals(expected, DTF.ISO_OFFSET_DATE_TIME.parseToTimestamp(offset, custom).toInstant());
        assertEquals(expected.toEpochMilli(), DTF.ISO_OFFSET_DATE_TIME.parseToDate(offset, custom).getTime());
        assertEquals(expected.toEpochMilli(), DTF.ISO_OFFSET_DATE_TIME.parseToTime(offset, custom).getTime());
        assertEquals(expected, DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offset, custom).toInstant());
        assertEquals(expected, DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zoned, custom).toInstant());
        assertEquals("GMT+05:30", DTF.ISO_OFFSET_DATE_TIME.parseToCalendar(offset, custom).getTimeZone().getID());
        assertEquals(Instant.parse("1970-01-01T05:00:45Z").toEpochMilli(), DTF.of("HH:mm:ssXXX").parseToTime("10:30:45+05:30", custom).getTime());

        // zone-less text does need the fallback, and the same zone is still rejected there
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45", null, custom));
        assertThrows(IllegalArgumentException.class, () -> DTF.LOCAL_DATE_TIME.parseToInstant("2025-01-15 10:30:45", custom));
        assertThrows(IllegalArgumentException.class, () -> DTF.LOCAL_DATE_TIME.parseToCalendar("2025-01-15 10:30:45", custom));
        assertThrows(IllegalArgumentException.class, () -> DTF.LOCAL_TIME.parseToTime("10:30:45", custom));
        assertThrows(IllegalArgumentException.class, () -> DTF.of("HH:mm:ss").parseToTime("10:30:45", custom));
    }

    @Test
    public void fallbackZone_underAnUnrepresentableDefault_zonedTextParsesOnEveryTarget() {
        TimeZone.setDefault(customDstZone());

        final String zoned = "2025-01-15T10:30:45.123+05:30[Asia/Kolkata]";
        final long expected = Instant.parse("2025-01-15T05:00:45.123Z").toEpochMilli();

        assertEquals(expected, Dates.parseToJUDate(zoned).getTime());
        assertEquals(expected, Dates.parseToDate(zoned).getTime());
        assertEquals(expected, Dates.parseToTime(zoned).getTime());
        assertEquals(expected, Dates.parseToTimestamp(zoned).getTime());
        assertEquals(expected, Dates.parseToCalendar(zoned).getTimeInMillis());
        assertEquals("Asia/Kolkata", Dates.parseToCalendar(zoned).getTimeZone().getID());
        assertEquals(expected, Dates.parseToGregorianCalendar(zoned).getTimeInMillis());
        assertEquals(expected, Dates.parseToXMLGregorianCalendar(zoned).toGregorianCalendar().getTimeInMillis());
        assertEquals(expected, Dates.parseToInstant(zoned).toEpochMilli());
        assertEquals(expected, Dates.parseToOffsetDateTime(zoned).toInstant().toEpochMilli());
        assertEquals(expected, Dates.parseToZonedDateTime(zoned).toInstant().toEpochMilli());

        final String noFraction = "2025-01-15T10:30:45+05:30[Asia/Kolkata]";
        assertEquals(Instant.parse("2025-01-15T05:00:45Z").toEpochMilli(), DTF.ISO_ZONED_DATE_TIME.parseToCalendar(noFraction).getTimeInMillis());
        assertEquals(Instant.parse("2025-01-15T05:00:45Z").toEpochMilli(), Dates.parseToCalendar(noFraction).getTimeInMillis());
        assertEquals(Instant.parse("2025-01-15T05:00:45Z").toEpochMilli(),
                Dates.parseToXMLGregorianCalendar(noFraction).toGregorianCalendar().getTimeInMillis());

        // the fixed-'Z' shape never needed the default either
        assertEquals(MODERN, Dates.parseToCalendar("2025-01-15T10:30:45.123Z").getTimeInMillis());
        assertEquals(Instant.ofEpochMilli(MODERN), Dates.parseToInstant("2025-01-15T10:30:45.123Z"));

        // zone-less text under that default: the legacy engine follows the custom rules, the java.time
        // targets reject the zone - the documented policy
        assertEquals(Instant.parse("2025-01-15T09:30:45Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15 10:30:45").getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15 10:30:45"));
    }

    @Test
    public void fixedZoneFormatters_stillCheckASuppliedZoneEagerly() {
        assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_TIMESTAMP.parseToInstant("2025-01-15T10:30:45.123Z", NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_DATE_TIME.parseToJUDate("2025-01-15T10:30:45Z", NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> DTF.HTTP_DATE.parseToZonedDateTime("Mon, 25 Dec 2023 14:25:30 GMT", NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_TIMESTAMP.parseToCalendar("2025-01-15T10:30:45.123Z", NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT, NEW_YORK));
        // with auto-detection the 'Z' the text carries is data and wins over the fallback zone (2026-09-02)
        assertEquals(Instant.ofEpochMilli(MODERN), Dates.parseToInstant("2025-01-15T10:30:45.123Z", null, NEW_YORK));

        // UTC-equivalent zones of every spelling are accepted, including a fixed zero offset under any ID
        for (final TimeZone utcLike : new TimeZone[] { UTC, TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("Etc/UTC"), new SimpleTimeZone(0, "Any"),
                fixedOffsetSubclass(0, "Also/Any", false) }) {
            assertEquals(Instant.ofEpochMilli(MODERN), DTF.ISO_8601_TIMESTAMP.parseToInstant("2025-01-15T10:30:45.123Z", utcLike), utcLike.getID());
            assertEquals(MODERN, DTF.ISO_8601_TIMESTAMP.parseToCalendar("2025-01-15T10:30:45.123Z", utcLike).getTimeInMillis(), utcLike.getID());
        }

        // and a null zone is never a conflict
        assertEquals(Instant.ofEpochMilli(MODERN), DTF.ISO_8601_TIMESTAMP.parseToInstant("2025-01-15T10:30:45.123Z", (TimeZone) null));
    }

    // ---------------------------------------------------------------------------------------------
    // B4 - any fixed-offset TimeZone is a fixed offset, whatever its class or ID
    // ---------------------------------------------------------------------------------------------

    @Test
    public void fixedOffsetTimeZoneSubclass_isAcceptedAsAFixedOffset() {
        final TimeZone subclass = fixedOffsetSubclass(3_600_000, "My/Fixed", false);
        final TimeZone simple = new SimpleTimeZone(3_600_000, "My/Fixed");
        final Date value = new Date(MODERN);

        assertTrue(Dates.isSameDay(value, value, subclass));
        assertEquals(Dates.format(value, Dates.ISO_ZONED_DATE_TIME_FORMAT, simple), Dates.format(value, Dates.ISO_ZONED_DATE_TIME_FORMAT, subclass));
        assertEquals(Dates.format(Dates.createCalendar(MODERN, simple)), Dates.format(Dates.createCalendar(MODERN, subclass)));
        assertEquals(Dates.truncate(Dates.createCalendar(MODERN, simple), Calendar.DATE).getTimeInMillis(),
                Dates.truncate(Dates.createCalendar(MODERN, subclass), Calendar.DATE).getTimeInMillis());
        assertEquals(Instant.parse("2025-01-15T09:30:45Z"), Dates.parseToInstant("2025-01-15 10:30:45", null, subclass));
        assertEquals(Instant.parse("2025-01-15T09:30:45Z"), DTF.LOCAL_DATE_TIME.parseToInstant("2025-01-15 10:30:45", subclass));
        assertEquals(Instant.parse("2025-01-15T09:30:45Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15 10:30:45", null, subclass).getTime());
        assertEquals(Instant.parse("2025-01-15T09:30:45Z").toEpochMilli(), Dates.parseToTimestamp("2025-01-15 10:30:45.000", null, subclass).getTime());

        // a subclass with daylight-saving rules is still rejected, and the message says why
        final IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> Dates.isSameDay(value, value, fixedOffsetSubclass(3_600_000, "My/Dst", true)));
        assertTrue(rejected.getMessage().contains("daylight-saving rules under an ID java.time does not know"), rejected.getMessage());

        // as is a fixed-offset subclass that reuses a registered ID whose rules differ
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(value, value, fixedOffsetSubclass(-5 * 3_600_000, "America/New_York", false)));

        // and a sub-second fixed offset cannot become a ZoneOffset either way
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(value, value, fixedOffsetSubclass(3_600_500, "My/Odd", false)));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(value, value, new SimpleTimeZone(3_600_500, "My/Odd")));
    }

    @Test
    public void fixedOffsetDefaultZone_isAcceptedEverywhereWhateverItsId() {
        TimeZone.setDefault(new SimpleTimeZone(3_600_000, "Made/Up"));
        final Date value = new Date(MODERN);

        assertTrue(Dates.isSameDay(value, value));
        assertTrue(Dates.isSameMonth(value, value));
        assertTrue(Dates.isSameYear(value, value));
        assertFalse(Dates.isLastDayOfMonth(value));
        assertEquals(31, Dates.lengthOfMonth(value));
        assertEquals(Instant.parse("2025-01-14T23:00:00Z").toEpochMilli(), Dates.truncate(value, Calendar.DATE).getTime());
        assertEquals("2025-01-15 11:30:45", DTF.LOCAL_DATE_TIME.format(value));
        assertEquals("2025-01-15 11:30:45", Dates.format(value, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals(Instant.parse("2025-01-15T09:30:45Z"), Dates.parseToInstant("2025-01-15 10:30:45"));
        assertEquals("2025-01-15T11:30:45+01:00", Dates.format(value, null, TimeZone.getDefault()));

        TimeZone.setDefault(fixedOffsetSubclass(3_600_000, "Made/Up/Too", false));
        assertTrue(Dates.isSameDay(value, value));
        assertEquals(Instant.parse("2025-01-14T23:00:00Z").toEpochMilli(), Dates.truncate(value, Calendar.DATE).getTime());
        assertEquals(Instant.parse("2025-01-15T09:30:45Z"), Dates.parseToInstant("2025-01-15 10:30:45"));
    }

    // ---------------------------------------------------------------------------------------------
    // B5 - the compact +HHmm offset is accepted with a fraction on auto-detection
    // ---------------------------------------------------------------------------------------------

    @Test
    public void compactOffset_isAcceptedWithAFraction_onAutoDetection() {
        final String compact = "2025-01-15T10:30:45.123+0530";
        final String colon = "2025-01-15T10:30:45.123+05:30";
        final long expected = Instant.parse("2025-01-15T05:00:45.123Z").toEpochMilli();

        assertEquals(expected, Dates.parseToJUDate(colon).getTime());
        assertEquals(expected, Dates.parseToJUDate(compact).getTime());
        assertEquals(expected, Dates.parseToDate(compact).getTime());
        assertEquals(expected, Dates.parseToTime(compact).getTime());
        assertEquals(expected, Dates.parseToTimestamp(compact).getTime());
        assertEquals(123_000_000, Dates.parseToTimestamp(compact).getNanos());
        assertEquals(expected, Dates.parseToCalendar(compact).getTimeInMillis());
        assertEquals("GMT+05:30", Dates.parseToCalendar(compact).getTimeZone().getID());
        assertEquals(expected, Dates.parseToGregorianCalendar(compact).getTimeInMillis());
        assertEquals(expected, Dates.parseToXMLGregorianCalendar(compact).toGregorianCalendar().getTimeInMillis());
        assertEquals(330, Dates.parseToXMLGregorianCalendar(compact).getTimezone());
        assertEquals(Instant.ofEpochMilli(expected), Dates.parseToInstant(compact));
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), Dates.parseToOffsetDateTime(compact).getOffset());
        assertEquals(Instant.ofEpochMilli(expected), Dates.parseToZonedDateTime(compact).toInstant());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 45, 123_000_000), Dates.parseToLocalDateTime(compact));
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 45, 123_000_000).toLocalTime(), Dates.parseToLocalTime(compact));

        // shortest and longest fractions, negative offset, nanosecond precision
        assertEquals(Instant.parse("2025-01-15T05:00:45.100Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30:45.1+0530").getTime());
        final Timestamp nanos = Dates.parseToTimestamp("2025-01-15T10:30:45.123456789-0530");
        assertEquals(Instant.parse("2025-01-15T16:00:45.123Z").toEpochMilli(), nanos.getTime());
        assertEquals(123_456_789, nanos.getNanos());
        assertEquals(Instant.parse("2025-01-15T16:00:45.123456789Z"), Dates.parseToInstant("2025-01-15T10:30:45.123456789-0530"));

        // the explicit constant already accepted the compact form; unchanged
        assertEquals(expected, Dates.parseToJUDate(compact, Dates.ISO_OFFSET_TIMESTAMP_FORMAT).getTime());
        assertEquals(expected, Dates.parseToInstant(compact, Dates.ISO_OFFSET_TIMESTAMP_FORMAT).toEpochMilli());

        // offsets that are neither form, or out of range, are still rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45.123+053"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45.123+05300"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45.123+1900"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45.123+05x0"));
    }

    // ---------------------------------------------------------------------------------------------
    // D2 - truncated comparisons work on the boundary instants directly
    // ---------------------------------------------------------------------------------------------

    /**
     * The counting types live in {@code testfixtures.dates}: creators cannot be registered for classes in
     * {@code com.landawn.abacus.*}.
     */
    @Test
    public void truncatedComparisons_doNotBuildResultObjects() {
        final AtomicInteger dateCreations = new AtomicInteger();
        final AtomicInteger calendarCreations = new AtomicInteger();
        Dates.unregisterDateCreator(CountingDate.class);
        Dates.unregisterCalendarCreator(CountingCalendar.class);
        assertTrue(Dates.registerDateCreator(CountingDate.class, millis -> {
            dateCreations.incrementAndGet();
            return new CountingDate(millis);
        }));
        assertTrue(Dates.registerCalendarCreator(CountingCalendar.class, (millis, template) -> {
            calendarCreations.incrementAndGet();
            return new CountingCalendar(millis, template.getTimeZone());
        }));

        try {
            final CountingDate a = new CountingDate(MODERN);
            final CountingDate b = new CountingDate(MODERN + 1_000);

            assertTrue(Dates.truncatedEquals(a, b, CalendarField.MINUTE));
            assertTrue(Dates.truncatedEquals(a, b, Calendar.HOUR_OF_DAY));
            assertFalse(Dates.truncatedEquals(a, b, CalendarField.SECOND));
            assertTrue(Dates.truncatedCompareTo(a, b, Calendar.SECOND) < 0);
            assertTrue(Dates.truncatedCompareTo(b, a, CalendarField.MILLISECOND) > 0);
            assertEquals(0, Dates.truncatedCompareTo(a, b, Calendar.DATE));
            assertEquals(0, dateCreations.get());

            assertEquals(CountingDate.class, Dates.truncate(a, Calendar.MINUTE).getClass());
            assertEquals(1, dateCreations.get());

            final CountingCalendar c = new CountingCalendar(MODERN, NEW_YORK);
            final CountingCalendar d = new CountingCalendar(MODERN + 1_000, NEW_YORK);

            assertTrue(Dates.truncatedEquals(c, d, CalendarField.MINUTE));
            assertFalse(Dates.truncatedEquals(c, d, Calendar.SECOND));
            assertTrue(Dates.truncatedCompareTo(c, d, CalendarField.SECOND) < 0);
            assertEquals(0, Dates.truncatedCompareTo(d, c, Calendar.YEAR));
            assertEquals(0, calendarCreations.get());

            assertEquals(CountingCalendar.class, Dates.truncate(c, Calendar.MINUTE).getClass());
            assertEquals(1, calendarCreations.get());
        } finally {
            Dates.unregisterDateCreator(CountingDate.class);
            Dates.unregisterCalendarCreator(CountingCalendar.class);
        }
    }

    @Test
    public void truncatedComparisons_agreeWithTruncateThenCompare() {
        final long[] instants = { MODERN, MODERN + 1, MODERN - 1, 1741501800000L /* 2025-03-09T06:30Z, the NY spring-forward hour */, 1741503600000L,
                1762061400000L /* 2025-11-02T05:30Z, the first pass of the NY overlap */, 1762065000000L, 0L, -1L,
                Dates.parseToJUDate("1899-06-15T12:00:00Z").getTime() };
        final int[] fields = { Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY, Calendar.AM_PM, Calendar.DATE, Dates.SEMI_MONTH,
                Calendar.MONTH, Calendar.YEAR };

        for (final TimeZone zone : new TimeZone[] { UTC, NEW_YORK, TimeZone.getTimeZone("Australia/Lord_Howe") }) {
            TimeZone.setDefault(zone);

            for (final long left : instants) {
                for (final long right : instants) {
                    for (final int field : fields) {
                        final Date d1 = new Date(left);
                        final Date d2 = new Date(right);
                        final Calendar c1 = Dates.createCalendar(left, zone);
                        final Calendar c2 = Dates.createCalendar(right, zone);
                        final String label = zone.getID() + " " + left + " vs " + right + " field " + field;

                        assertEquals(Integer.signum(Dates.truncate(d1, field).compareTo(Dates.truncate(d2, field))),
                                Integer.signum(Dates.truncatedCompareTo(d1, d2, field)), label);
                        assertEquals(Integer.signum(Dates.truncate(c1, field).compareTo(Dates.truncate(c2, field))),
                                Integer.signum(Dates.truncatedCompareTo(c1, c2, field)), label);
                        assertEquals(Dates.truncate(d1, field).equals(Dates.truncate(d2, field)), Dates.truncatedEquals(d1, d2, field), label);
                        assertEquals(Dates.truncate(c1, field).getTimeInMillis() == Dates.truncate(c2, field).getTimeInMillis(),
                                Dates.truncatedEquals(c1, c2, field), label);
                    }
                }
            }
        }
    }

    @Test
    public void truncatedComparisons_keepTheirContracts() {
        final Date epoch = new Date(0L);

        // a sub-millisecond Timestamp fraction never changes a truncated comparison
        final Timestamp fine = new Timestamp(MODERN);
        fine.setNanos(123_999_999);
        assertTrue(Dates.truncatedEquals(fine, new Timestamp(MODERN), Calendar.MILLISECOND));
        assertEquals(0, Dates.truncatedCompareTo(new Timestamp(MODERN), fine, CalendarField.MILLISECOND));
        assertTrue(Dates.truncatedEquals(fine, new Date(MODERN), CalendarField.SECOND));

        // each calendar in its own zone, compared as instants
        final Calendar utc = Dates.createCalendar(MODERN, UTC);
        final Calendar tokyo = Dates.createCalendar(MODERN, TimeZone.getTimeZone("Asia/Tokyo"));
        assertFalse(Dates.truncatedEquals(utc, tokyo, Calendar.DATE));
        assertTrue(Dates.truncatedCompareTo(utc, tokyo, CalendarField.DAY_OF_MONTH) > 0);
        assertTrue(Dates.truncatedEquals(utc, tokyo, Calendar.MILLISECOND));

        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals((Date) null, epoch, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(epoch, (Date) null, CalendarField.DAY_OF_MONTH));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo((Calendar) null, utc, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(utc, (Calendar) null, CalendarField.DAY_OF_MONTH));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(utc, utc, (CalendarField) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(epoch, epoch, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(utc, utc, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(epoch, epoch, Calendar.ERA));

        final Calendar custom = Dates.createCalendar(MODERN, customDstZone());
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(custom, custom, Calendar.DATE));
        assertTrue(Dates.truncatedEquals(custom, custom, Calendar.MILLISECOND)); // no zone needed for a millisecond boundary

        assertThrows(ArithmeticException.class, () -> Dates.truncatedEquals(new Date(Long.MAX_VALUE), epoch, Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.truncatedCompareTo(new Date(Long.MIN_VALUE), epoch, Calendar.DATE));
        assertThrows(ArithmeticException.class, () -> Dates.truncatedCompareTo(Dates.createCalendar(Long.MAX_VALUE, UTC), utc, Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.truncatedEquals(Dates.createCalendar(Long.MIN_VALUE, UTC), utc, CalendarField.MILLISECOND));
    }

    @Test
    public void dateBoundaryOverloads_matchTheCalendarOverloads() {
        final long[] instants = { MODERN, 1741501800000L, 1741503599999L, 1741503600000L, 1762061400000L, 1762065000000L, 1762065000001L, 0L, -1L,
                Dates.parseToJUDate("1899-06-15T12:00:00Z").getTime(), Dates.parseToJUDate("1500-03-01T12:00:00Z").getTime() };
        final int[] fields = { Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.HOUR_OF_DAY, Calendar.AM_PM, Calendar.DATE,
                Dates.SEMI_MONTH, Calendar.MONTH, Calendar.YEAR };

        for (final TimeZone zone : new TimeZone[] { UTC, NEW_YORK, TimeZone.getTimeZone("Pacific/Chatham"), TimeZone.getTimeZone("America/Sao_Paulo") }) {
            TimeZone.setDefault(zone);

            for (final long millis : instants) {
                final Date date = new Date(millis);
                final Calendar calendar = Dates.createCalendar(millis, zone);

                for (final int field : fields) {
                    final String label = zone.getID() + " " + millis + " field " + field;
                    assertEquals(Dates.truncate(calendar, field).getTimeInMillis(), Dates.truncate(date, field).getTime(), label);
                    assertEquals(Dates.round(calendar, field).getTimeInMillis(), Dates.round(date, field).getTime(), label);
                    assertEquals(Dates.ceiling(calendar, field).getTimeInMillis(), Dates.ceiling(date, field).getTime(), label);
                }
            }
        }

        TimeZone.setDefault(UTC);

        // the Timestamp sub-millisecond rules are unchanged
        final Timestamp half = new Timestamp(MODERN);
        half.setNanos(123_500_000);
        assertEquals(MODERN + 1, Dates.round(half, Calendar.MILLISECOND).getTime());
        assertEquals(0, Dates.round(half, Calendar.MILLISECOND).getNanos() % 1_000_000);
        assertEquals(MODERN, Dates.truncate(half, Calendar.MILLISECOND).getTime());
        assertEquals(MODERN + 1, Dates.ceiling(half, Calendar.MILLISECOND).getTime());
        assertEquals(MODERN, Dates.ceiling(new Timestamp(MODERN), Calendar.MILLISECOND).getTime());

        // and so are the guards
        assertThrows(ArithmeticException.class, () -> Dates.round(new Date(Long.MAX_VALUE), Calendar.MINUTE));
        assertThrows(ArithmeticException.class, () -> Dates.truncate(new Date(Long.MIN_VALUE), Calendar.MILLISECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(new Date(MODERN), Calendar.WEEK_OF_YEAR));
        TimeZone.setDefault(customDstZone());
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(new Date(MODERN), Calendar.DATE));
        assertEquals(MODERN, Dates.truncate(new Date(MODERN), Calendar.MILLISECOND).getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // Documentation pinned
    // ---------------------------------------------------------------------------------------------

    @Test
    public void nullMarker_returnsNullOnTheLocaleOverloads() {
        assertNull(Dates.parseToDate("null", "yyyy-MM-dd", UTC, Locale.US));
        assertNull(Dates.parseToDate("NULL", null, UTC, Locale.FRENCH));
        assertNull(Dates.parseToTime("null", "HH:mm:ss", UTC, Locale.US));
        assertNull(Dates.parseToTime("Null", null, null, Locale.US));
        assertNull(Dates.parseToJUDate("null", "yyyy-MM-dd", UTC, Locale.US));
        assertNull(Dates.parseToTimestamp("null", null, UTC, Locale.US));
        assertNull(Dates.parseToCalendar("null", null, UTC, Locale.US));

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("null", "yyyy-MM-dd", UTC, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("", "yyyy-MM-dd", UTC, Locale.US));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("", "HH:mm:ss", UTC, Locale.US));
    }

    @Test
    public void formatCalendar_defaultRejectsAZoneNoZoneIdCanExpress() {
        final Calendar calendar = new GregorianCalendar(customDstZone());
        calendar.setTimeInMillis(MODERN);

        assertThrows(IllegalArgumentException.class, () -> Dates.format(calendar));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(calendar, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(calendar, Dates.ISO_ZONED_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(calendar, new StringBuilder()));

        assertEquals("2025-01-15 11:30:45", Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-15T11:30:45+01:00", Dates.format(calendar, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(calendar, Dates.ISO_8601_DATE_TIME_FORMAT));
        assertEquals("2025-01-15T10:30:45.123Z[UTC]", Dates.format(calendar, null, UTC)); // an explicit representable zone overrides
    }

    @Test
    public void autoDetectedFailures_nameTheAutoDetectedFormat() {
        final IllegalArgumentException jdbc = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp("2025-03-09 02:30:00.5", null, NEW_YORK));
        assertTrue(jdbc.getMessage().contains("the auto-detected format"), jdbc.getMessage());
        assertFalse(jdbc.getMessage().contains("fffffffff"), jdbc.getMessage());
        assertTrue(jdbc.getMessage().contains("Nonexistent local date-time"), jdbc.getMessage());

        final IllegalArgumentException xml = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToXMLGregorianCalendar("2025-03-09 02:30:00.5", null, NEW_YORK));
        assertTrue(xml.getMessage().contains("the auto-detected format"), xml.getMessage());

        // an explicit constant is still named
        final IllegalArgumentException explicit = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp("2025-03-09 02:30:00", Dates.LOCAL_DATE_TIME_FORMAT, NEW_YORK));
        assertTrue(explicit.getMessage().contains("Nonexistent local date-time"), explicit.getMessage());
    }

    @Test
    public void defaultZonePolicy_isTheDocumentedOne() {
        final Date value = new Date(MODERN);

        // a registered ID whose rules were customised
        TimeZone.setDefault(
                new SimpleTimeZone(-5 * 3_600_000, "America/New_York", Calendar.APRIL, 1, 0, 7_200_000, Calendar.SEPTEMBER, 1, 0, 7_200_000, 3_600_000));
        assertTrue(Dates.isSameDay(value, value)); // the ID's rules
        assertFalse(Dates.isLastDayOfMonth(value));
        assertNotNull(Dates.format(value, Dates.LOCAL_DATE_TIME_FORMAT)); // Calendar, custom rules
        assertNotNull(Dates.parseToJUDate("2025-01-15 10:30:45"));
        assertNotNull(Dates.setHours(value, 3));
        assertNotNull(Dates.addDays(value, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(value, Calendar.DATE)); // java.time, rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.round(value, Calendar.HOUR_OF_DAY));
        assertThrows(IllegalArgumentException.class, () -> DTF.LOCAL_DATE_TIME.format(value));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15 10:30:45"));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(value, Dates.ISO_ZONED_DATE_TIME_FORMAT));

        // an ID java.time does not know, with daylight-saving rules
        TimeZone.setDefault(customDstZone());
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(value, value));
        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDayOfMonth(value));
        assertNotNull(Dates.format(value, Dates.LOCAL_DATE_TIME_FORMAT));
        assertNotNull(Dates.parseToJUDate("2025-01-15 10:30:45"));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(value, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15 10:30:45"));

        // text that carries its own zone is never subject to any of this
        assertEquals(MODERN, Dates.parseToInstant("2025-01-15T10:30:45.123Z").toEpochMilli());
        assertEquals(MODERN, Dates.parseToCalendar("2025-01-15T16:00:45.123+05:30[Asia/Kolkata]").getTimeInMillis());
    }
}
