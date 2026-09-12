package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-30g review cycle.
 *
 * <ul>
 * <li>C-001 - the three {@code .SSS} timestamp constants were never shape-checked, so
 * {@code SimpleDateFormat}'s variable-width numeric reading silently produced a different instant,
 * and the auto-detected JDBC branch accepted wrong separators.</li>
 * <li>C-002 - the width-rejection message always suggested {@code "yyyy-M-d"}.</li>
 * <li>C-004 - {@code checkDateFormat} returned the deprecated HTTP-date alias.</li>
 * </ul>
 */
public class DatesTimestampWidthTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // ------------------------------------------------------------------ C-001: non-canonical widths

    @Test
    public void localTimestampFormat_rejectsNonCanonicalFieldWidths() {
        for (final String text : new String[] { "2025-001-5 10:30:45.123", "2025-01-015 0:30:45.123", "2025-01-15 010:0:45.123", "2025-01-15 10:030:5.123",
                "2025-1-015 10:30:45.123", "2025-012-5 10:30:45.123" }) {
            assertEveryLegacyTargetRejects(text, Dates.LOCAL_TIMESTAMP_FORMAT);
            assertEveryLegacyTargetRejects(text, null);
        }
    }

    @Test
    public void isoLocalTimestampFormat_rejectsNonCanonicalFieldWidths() {
        for (final String text : new String[] { "2025-001-5T10:30:45.123", "2025-01-015T0:30:45.123", "2025-01-15T010:0:45.123", "2025-01-15T10:030:5.123" }) {
            assertEveryLegacyTargetRejects(text, Dates.ISO_LOCAL_TIMESTAMP_FORMAT);
            assertEveryLegacyTargetRejects(text, null);
        }
    }

    @Test
    public void iso8601TimestampFormat_rejectsNonCanonicalFieldWidths() {
        for (final String text : new String[] { "2025-001-5T10:30:45.123Z", "2025-01-015T0:30:45.123Z", "2025-01-15T010:0:45.123Z",
                "2025-01-15T10:030:5.123Z" }) {
            assertEveryLegacyTargetRejects(text, Dates.ISO_8601_TIMESTAMP_FORMAT);
            assertEveryLegacyTargetRejects(text, null);
        }
    }

    /** The auto-detected JDBC branch read fixed digit positions without checking the separators. */
    @Test
    public void autoDetectedJdbcTimestamp_rejectsWrongSeparators() {
        for (final String text : new String[] { "2025-01x15 10:30:45.123", "2025-01-15 10x30:45.123", "2025-01-15 10:30x45.123" }) {
            assertEveryLegacyTargetRejects(text, null);
        }
    }

    /** A caller-visible symptom of C-001: two targets disagreed on the same auto-detected text. */
    @Test
    public void autoDetectedNonCanonicalText_isRejectedByEveryTargetAlike() {
        final String text = "2025-001-5T10:30:45.123";

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(text));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime(text));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(text));
    }

    // ------------------------------------------------------------------ C-001: nothing valid was lost

    @Test
    public void canonicalTimestampTextStillParses() {
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
    }

    /** Auto-detection must keep the JDBC escape grammar: 1-9 fraction digits, trailing zeros trimmed. */
    @Test
    public void autoDetectedVariableFractionStillParses() {
        final TimeZone original = TimeZone.getDefault();

        try {
            TimeZone.setDefault(UTC);

            assertEquals(1736937045500L, Dates.parseToTimestamp("2025-01-15 10:30:45.5").getTime());
            assertEquals(1736937045010L, Dates.parseToTimestamp("2025-01-15 10:30:45.01").getTime());
            assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789").getTime());
            assertEquals(123456789, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789").getNanos());
            assertEquals(1736937045500L, Dates.parseToJUDate("2025-01-15 10:30:45.5").getTime());
            assertEquals(1736937045500L, Dates.parseToTimestamp("2025-01-15T10:30:45.5").getTime());
            assertEquals(1736937045500L, Dates.parseToTimestamp("2025-01-15T10:30:45.5Z").getTime());
            assertEquals(1736937045500L, Dates.parseToJUDate("2025-01-15T10:30:45.5").getTime());
            assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.123").getTime());

            // what Timestamp.toString() writes must always round-trip
            final Timestamp ts = new Timestamp(1736937045000L);
            ts.setNanos(500_000_000);
            assertEquals(ts.getTime(), Dates.parseToTimestamp(ts.toString()).getTime());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void formatOutputAlwaysReparses() {
        final Timestamp value = new Timestamp(1736937045123L);

        for (final String format : new String[] { Dates.LOCAL_TIMESTAMP_FORMAT, Dates.ISO_LOCAL_TIMESTAMP_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT }) {
            final String text = Dates.format(value, format, UTC);
            assertEquals(value.getTime(), Dates.parseToTimestamp(text, format, UTC).getTime(), format);
        }
    }

    // ------------------------------------------------------------------ C-001: null / empty / boundary / Unicode

    @Test
    public void nullEmptyAndMarkerContractUnchanged() {
        assertNull(Dates.parseToTimestamp(null, Dates.LOCAL_TIMESTAMP_FORMAT));
        assertNull(Dates.parseToTimestamp("null", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertNull(Dates.parseToTimestamp("NULL", Dates.ISO_8601_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("   ", Dates.LOCAL_TIMESTAMP_FORMAT));
    }

    @Test
    public void boundaryYearsAreStillAcceptedAndRejectedAsBefore() {
        assertNotNull(Dates.parseToTimestamp("0001-01-01T00:00:00.000Z", Dates.ISO_8601_TIMESTAMP_FORMAT));
        assertNotNull(Dates.parseToTimestamp("9999-12-31T23:59:59.999Z", Dates.ISO_8601_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("0000-01-01T00:00:00.000Z", Dates.ISO_8601_TIMESTAMP_FORMAT));
    }

    /** Non-ASCII digits are not digits for these grammars, whatever SimpleDateFormat's locale data says. */
    @Test
    public void nonAsciiDigitsAreRejected() {
        assertEveryLegacyTargetRejects("2025-01-15 10:30:4٥.123", Dates.LOCAL_TIMESTAMP_FORMAT);
        assertEveryLegacyTargetRejects("2025-01-15T10:30:4٥.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT);
        assertEveryLegacyTargetRejects("٢025-01-15 10:30:45.123", null);
    }

    // ------------------------------------------------------------------ C-002: the suggested pattern

    @Test
    public void widthRejectionMessageNamesAWorkablePattern() {
        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put(Dates.LOCAL_DATE_FORMAT, "yyyy-M-d");
        expected.put(Dates.LOCAL_TIME_FORMAT, "H:m:s");
        expected.put(Dates.LOCAL_DATE_TIME_FORMAT, "yyyy-M-d H:m:s");
        expected.put(Dates.ISO_LOCAL_DATE_TIME_FORMAT, "yyyy-M-d'T'H:m:s");
        // XXX, not a quoted 'Z': in a custom pattern a quoted 'Z' is a plain literal with no zone
        // semantics, so the old suggestion resolved the text in the default zone and silently shifted the
        // instant by that zone's offset. These tests pass UTC explicitly, where the two spellings agree,
        // which is why the defect survived - see
        // DatesTest.test20260906c3_widthRejectionSuggestsAPatternThatKeepsTheUtcDesignator.
        expected.put(Dates.ISO_8601_DATE_TIME_FORMAT, "yyyy-M-d'T'H:m:sXXX");
        expected.put(Dates.LOCAL_TIMESTAMP_FORMAT, "yyyy-M-d H:m:s.SSS");
        expected.put(Dates.ISO_LOCAL_TIMESTAMP_FORMAT, "yyyy-M-d'T'H:m:s.SSS");
        expected.put(Dates.ISO_8601_TIMESTAMP_FORMAT, "yyyy-M-d'T'H:m:s.SSSXXX");

        final Map<String, String> badText = new LinkedHashMap<>();
        badText.put(Dates.LOCAL_DATE_FORMAT, "2025-1-15");
        badText.put(Dates.LOCAL_TIME_FORMAT, "1:30:45");
        badText.put(Dates.LOCAL_DATE_TIME_FORMAT, "2025-1-15 10:30:45");
        badText.put(Dates.ISO_LOCAL_DATE_TIME_FORMAT, "2025-1-15T10:30:45");
        badText.put(Dates.ISO_8601_DATE_TIME_FORMAT, "2025-1-15T10:30:45Z");
        badText.put(Dates.LOCAL_TIMESTAMP_FORMAT, "2025-001-5 10:30:45.123");
        badText.put(Dates.ISO_LOCAL_TIMESTAMP_FORMAT, "2025-001-5T10:30:45.123");
        badText.put(Dates.ISO_8601_TIMESTAMP_FORMAT, "2025-001-5T10:30:45.123Z");

        for (final Map.Entry<String, String> e : expected.entrySet()) {
            final String format = e.getKey();
            final String text = badText.get(format);
            // a time-only pattern has no complete date, so it must go through parseToTime to reach the
            // width check at all
            final boolean timeOnly = Dates.LOCAL_TIME_FORMAT.equals(format);
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                if (timeOnly) {
                    Dates.parseToTime(text, format, UTC);
                } else {
                    Dates.parseToJUDate(text, format, UTC);
                }
            }, format);
            final String message = ex.getMessage();
            assertTrue(message.contains("\"" + e.getValue() + "\""),
                    "message for '" + format + "' should suggest \"" + e.getValue() + "\" but was: " + message);
        }
    }

    /** The suggested pattern must actually parse the text that was rejected. */
    @Test
    public void suggestedPatternParsesTheRejectedText() {
        assertNotNull(Dates.parseToTime("1:30:45", "H:m:s", UTC));
        assertNotNull(Dates.parseToJUDate("2025-1-15", "yyyy-M-d", UTC));
        assertNotNull(Dates.parseToJUDate("2025-1-15 10:30:45", "yyyy-M-d H:m:s", UTC));
        assertNotNull(Dates.parseToJUDate("2025-1-15T10:30:45", "yyyy-M-d'T'H:m:s", UTC));
        assertNotNull(Dates.parseToJUDate("2025-1-15T10:30:45Z", "yyyy-M-d'T'H:m:sXXX", UTC));
        assertNotNull(Dates.parseToTimestamp("2025-1-5 10:30:45.123", "yyyy-M-d H:m:s.SSS", UTC));
        assertNotNull(Dates.parseToTimestamp("2025-1-5T10:30:45.123", "yyyy-M-d'T'H:m:s.SSS", UTC));
        assertNotNull(Dates.parseToTimestamp("2025-1-5T10:30:45.123Z", "yyyy-M-d'T'H:m:s.SSSXXX", UTC));

        // The two UTC-designator suggestions must be exercised in a NON-UTC zone as well: that is the whole
        // point of spelling them XXX rather than a quoted 'Z', and asserting them only under UTC is exactly
        // how the quoted-'Z' defect survived here in the first place.
        final TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");

        assertEquals(Dates.parseToJUDate("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, UTC).getTime(),
                Dates.parseToJUDate("2025-1-15T10:30:45Z", "yyyy-M-d'T'H:m:sXXX", la).getTime());
        assertEquals(Dates.parseToTimestamp("2025-01-05T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT, UTC).getTime(),
                Dates.parseToTimestamp("2025-1-5T10:30:45.123Z", "yyyy-M-d'T'H:m:s.SSSXXX", la).getTime());
    }

    // ------------------------------------------------------------------ C-004: HTTP-date detection

    @Test
    public void httpDateAutoDetectionUnchanged() {
        final java.util.Date parsed = Dates.parseToJUDate("Mon, 25 Dec 2023 14:30:45 GMT");
        assertNotNull(parsed);
        assertEquals(1703514645000L, parsed.getTime());
        assertEquals("Mon, 25 Dec 2023 14:30:45 GMT", Dates.format(parsed, Dates.HTTP_DATE_FORMAT));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Every legacy (instant-bearing) target and every {@code java.time} target must agree that the text
     * is malformed. A split verdict is exactly the defect C-001 describes.
     */
    private static void assertEveryLegacyTargetRejects(final String text, final String format) {
        final List<String> accepted = new ArrayList<>();

        record(accepted, "parseToJUDate", () -> Dates.parseToJUDate(text, format, UTC));
        record(accepted, "parseToDate", () -> Dates.parseToDate(text, format, UTC));
        record(accepted, "parseToTime", () -> Dates.parseToTime(text, format, UTC));
        record(accepted, "parseToTimestamp", () -> Dates.parseToTimestamp(text, format, UTC));
        record(accepted, "parseToCalendar", () -> Dates.parseToCalendar(text, format, UTC));
        record(accepted, "parseToGregorianCalendar", () -> Dates.parseToGregorianCalendar(text, format, UTC));
        record(accepted, "parseToXMLGregorianCalendar", () -> Dates.parseToXMLGregorianCalendar(text, format, UTC));
        record(accepted, "parseToLocalDateTime", () -> Dates.parseToLocalDateTime(text, format));
        record(accepted, "parseToInstant", () -> Dates.parseToInstant(text, format, UTC));

        if (!accepted.isEmpty()) {
            fail("text \"" + text + "\" with format " + (format == null ? "<auto-detected>" : "'" + format + "'") + " was accepted by " + accepted);
        }
    }

    private interface Call {
        Object run();
    }

    private static void record(final List<String> accepted, final String name, final Call call) {
        try {
            final Object value = call.run();
            accepted.add(name + "=" + describe(value));
        } catch (final RuntimeException expected) {
            // rejected, as required
        }
    }

    private static String describe(final Object value) {
        if (value instanceof java.util.Date) {
            return String.valueOf(((java.util.Date) value).getTime());
        }

        if (value instanceof Calendar) {
            return String.valueOf(((Calendar) value).getTimeInMillis());
        }

        return String.valueOf(value);
    }

    // ------------------------------------------------ the rejection message names the whole canonical shape

    /**
     * A narrow field moves the fraction off index 19, so the head check opts out and the fraction check is
     * the only diagnostic left - and its message named the fraction width for text whose fraction is
     * exactly three digits. The message now names the whole shape its test actually enforces.
     */
    @Test
    public void testNarrowFieldsWithThreeFractionDigitsAreRejectedAsAShape() {
        for (final String text : new String[] { "2025-1-5 1:2:3.123", "2025-01-15 1:30:45.123" }) {
            final String message = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, Dates.LOCAL_TIMESTAMP_FORMAT, UTC))
                    .getMessage();

            assertFalse(message.contains("requires exactly three fractional-second digits"), message);
            assertTrue(message.contains("requires its exact canonical shape"), message);
            assertTrue(message.contains("23 characters"), message);
            assertTrue(message.contains("yyyy-M-d H:m:s.SSS"), message);
        }
    }

    @Test
    public void testZuluDesignatorFaultsAreRejectedAsAShape() {
        final String missingZ = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-01-15T10:30:45.123", Dates.ISO_8601_TIMESTAMP_FORMAT, UTC)).getMessage();

        assertTrue(missingZ.contains("24 characters"), missingZ);
        assertTrue(missingZ.contains("and a trailing 'Z'"), missingZ);
        assertTrue(missingZ.contains("yyyy-M-d'T'H:m:s.SSSXXX"), missingZ);
        assertFalse(missingZ.contains("requires exactly three fractional-second digits"), missingZ);

        final String extraZ = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-01-15T10:30:45.123Z", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, UTC)).getMessage();

        assertTrue(extraZ.contains("23 characters"), extraZ);
        assertTrue(extraZ.contains("yyyy-M-d'T'H:m:s.SSS"), extraZ);
        assertFalse(extraZ.contains("and a trailing 'Z'"), extraZ);
        assertFalse(extraZ.contains("requires exactly three fractional-second digits"), extraZ);
    }

    @Test
    public void testWrongFractionWidthStillNamesTheThreeDigitRequirement() {
        for (final String text : new String[] { "2025-01-15 10:30:45.12", "2025-01-15 10:30:45.1234", "2025-01-15 10:30:45" }) {
            final String message = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, Dates.LOCAL_TIMESTAMP_FORMAT, UTC))
                    .getMessage();

            assertTrue(message.contains("exactly three fractional-second digits"), message);
        }

        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT, UTC).getTime());
    }
}
