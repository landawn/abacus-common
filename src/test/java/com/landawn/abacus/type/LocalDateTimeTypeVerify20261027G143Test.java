package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Beans;

public class LocalDateTimeTypeVerify20261027G143Test extends TestBase {

    // ---- deep review 2026-10-27 verify G143 begin ----

    public static class Times {
        private LocalDateTime dateTime;
        private LocalDate date;
        private LocalTime time;

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(final LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(final LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(final LocalTime time) {
            this.time = time;
        }
    }

    public static class Source {
        private Object dateTime;
        private Object date;
        private Object time;

        public Object getDateTime() {
            return dateTime;
        }

        public void setDateTime(final Object dateTime) {
            this.dateTime = dateTime;
        }

        public Object getDate() {
            return date;
        }

        public void setDate(final Object date) {
            this.date = date;
        }

        public Object getTime() {
            return time;
        }

        public void setTime(final Object time) {
            this.time = time;
        }
    }

    private static Map<String, Object> allProperties(final Object value) {
        final Map<String, Object> map = new HashMap<>();
        map.put("dateTime", value);
        map.put("date", value);
        map.put("time", value);
        return map;
    }

    private static Source source(final Object value) {
        final Source source = new Source();
        source.setDateTime(value);
        source.setDate(value);
        source.setTime(value);
        return source;
    }

    private static void assertTimes(final LocalDateTime expected, final Times actual, final String message) {
        assertEquals(expected, actual.getDateTime(), message);
        assertEquals(expected.toLocalDate(), actual.getDate(), message);
        assertEquals(expected.toLocalTime(), actual.getTime(), message);
    }

    // Bean binding (Beans.mapToBean / Beans.copyAs) converts through valueOf(Object), so an Instant / OffsetDateTime / ZonedDateTime value
    // now binds to LocalDateTime, LocalDate and LocalTime properties (it failed with DateTimeParseException before).
    @Test
    public void beanBindingAcceptsInstantOffsetAndZonedDateTime() {
        final Instant instant = Instant.parse("2023-12-25T11:10:45.123456789Z");
        final LocalDateTime instantInDefaultZone = LocalDateTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID);
        assertTimes(instantInDefaultZone, Beans.mapToBean(allProperties(instant), Times.class), "mapToBean Instant");
        assertTimes(instantInDefaultZone, Beans.copyAs(source(instant), Times.class), "copyAs Instant");

        final OffsetDateTime offsetDateTime = OffsetDateTime.of(2020, 1, 1, 23, 30, 0, 0, ZoneOffset.ofHours(14));
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(2021, 10, 31, 2, 30, 0, 5, ZoneId.of("Europe/Paris")).withLaterOffsetAtOverlap();

        for (final Object value : new Object[] { offsetDateTime, offsetDateTime.toZonedDateTime(), zonedDateTime, zonedDateTime.toOffsetDateTime() }) {
            final LocalDateTime ownFields = value instanceof OffsetDateTime odt ? odt.toLocalDateTime() : ((ZonedDateTime) value).toLocalDateTime();
            assertTimes(ownFields, Beans.mapToBean(allProperties(value), Times.class), "mapToBean " + value);
            assertTimes(ownFields, Beans.copyAs(source(value), Times.class), "copyAs " + value);
        }
    }

    // Unchanged neighbours of the bean paths: a LocalDateTime binds through the text route as before, and an OffsetTime is still rejected by
    // the LocalTime property.
    @Test
    public void beanBindingOfOtherTemporalValuesIsUnchanged() {
        final LocalDateTime localDateTime = LocalDateTime.of(2021, 1, 1, 10, 30, 45, 1);
        final Map<String, Object> map = new HashMap<>();
        map.put("dateTime", localDateTime);
        assertEquals(localDateTime, Beans.mapToBean(map, Times.class).getDateTime());

        final Map<String, Object> offsetTime = new HashMap<>();
        offsetTime.put("time", java.time.OffsetTime.of(10, 30, 0, 0, ZoneOffset.ofHours(2)));
        assertThrows(DateTimeParseException.class, () -> Beans.mapToBean(offsetTime, Times.class));
    }

    // ---- deep review 2026-10-27 verify G143 end ----
}
