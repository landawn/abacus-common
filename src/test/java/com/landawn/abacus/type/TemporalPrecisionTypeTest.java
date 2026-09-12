package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class TemporalPrecisionTypeTest extends TestBase {
    @Test
    void instantTextPreservesNanosecondsAndExtremeYears() throws Exception {
        Type<Instant> type = TypeFactory.getType(Instant.class);
        for (Instant value : new Instant[] { Instant.MIN, Instant.MAX, Instant.EPOCH, Instant.ofEpochSecond(-1, 999999999),
                Instant.parse("2023-12-25T10:30:45.123456789Z") }) {
            assertRoundTrip(type, value);
        }
        assertNull(type.stringOf(null));
        assertNull(type.valueOf(""));
        Instant value = Instant.ofEpochSecond(1, 123456789);
        assertEquals("1123", serialized(type, value, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG)));
        assertEquals("\"1970-01-01T00:00:01.123Z\"", serialized(type, value, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)));
    }

    @Test
    void offsetTextPreservesNanosecondsAndOffsetSeconds() throws Exception {
        Type<OffsetDateTime> type = TypeFactory.getType(OffsetDateTime.class);
        for (OffsetDateTime value : new OffsetDateTime[] { OffsetDateTime.MIN, OffsetDateTime.MAX,
                OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 123456789, ZoneOffset.ofHoursMinutesSeconds(5, 30, 15)),
                OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC) }) {
            assertRoundTrip(type, value);
        }
        assertNull(type.stringOf(null));
        assertNull(type.valueOf(""));
    }

    @Test
    void timestampObjectConversionsPreserveNanoseconds() {
        for (Instant instant : new Instant[] { Instant.EPOCH, Instant.ofEpochSecond(-1, 999999999), Instant.ofEpochSecond(1700000000, 123456789) }) {
            Timestamp timestamp = Timestamp.from(instant);
            assertEquals(instant, TypeFactory.getType(Instant.class).valueOf(timestamp));
            assertEquals(OffsetDateTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID), TypeFactory.getType(OffsetDateTime.class).valueOf(timestamp));
            assertEquals(ZonedDateTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID), TypeFactory.getType(ZonedDateTime.class).valueOf(timestamp));
            assertEquals(LocalDateTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID), TypeFactory.getType(LocalDateTime.class).valueOf(timestamp));
            assertEquals(LocalTime.ofInstant(instant, AbstractTemporalType.DEFAULT_ZONE_ID), TypeFactory.getType(LocalTime.class).valueOf(timestamp));
        }
    }

    @Test
    void durationTextPreservesItsFullDomainAndReadsLegacyMillis() throws Exception {
        Type<Duration> type = TypeFactory.getType(Duration.class);
        for (Duration value : new Duration[] { Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(-1), Duration.ofSeconds(Long.MIN_VALUE),
                Duration.ofSeconds(Long.MAX_VALUE, 999999999) }) {
            assertRoundTrip(type, value);
        }
        for (long millis : new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE }) {
            assertEquals(Duration.ofMillis(millis), type.valueOf(Long.toString(millis)));
        }
        assertNull(type.stringOf(null));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertThrows(RuntimeException.class, () -> type.valueOf("P\uD83D\uDE00"));
        assertEquals("[\"PT0.000000001S\"]", Utils.jsonParser.serialize(new Duration[] { Duration.ofNanos(1) }));
        Duration[] restored = Utils.jsonParser.deserialize("[\"PT0.000000001S\"]", Duration[].class);
        assertEquals(Duration.ofNanos(1), restored[0]);
    }

    @Test
    void durationJdbcRejectsPrecisionLossBeforeBinding() throws Exception {
        Type<Duration> type = TypeFactory.getType(Duration.class);
        for (Duration value : new Duration[] { Duration.ofNanos(1), Duration.ofNanos(-1), Duration.ofSeconds(Long.MAX_VALUE) }) {
            PreparedStatement stmt = mock(PreparedStatement.class);
            CallableStatement call = mock(CallableStatement.class);
            assertThrows(ArithmeticException.class, () -> type.set(stmt, 1, value));
            assertThrows(ArithmeticException.class, () -> type.set(call, "duration", value));
            verifyNoInteractions(stmt, call);
        }
        for (long millis : new long[] { Long.MIN_VALUE, -1, 0, Long.MAX_VALUE }) {
            PreparedStatement stmt = mock(PreparedStatement.class);
            CallableStatement call = mock(CallableStatement.class);
            type.set(stmt, 1, Duration.ofMillis(millis));
            type.set(call, "duration", Duration.ofMillis(millis));
            verify(stmt).setLong(1, millis);
            verify(call).setLong("duration", millis);
        }
    }

    private static <T> void assertRoundTrip(Type<T> type, T value) throws Exception {
        assertEquals(value, type.valueOf(type.stringOf(value)));
        StringBuilder text = new StringBuilder();
        type.appendTo(text, value);
        assertEquals(value, type.valueOf(text.toString()));
        assertEquals(type.stringOf(value), serialized(type, value, null));
        JsonSerConfig config = JsonSerConfig.create().setDateTimeFormat(null);
        assertEquals("\"" + type.stringOf(value) + "\"", serialized(type, value, config));
    }

    private static <T> String serialized(Type<T> type, T value, JsonSerConfig config) throws Exception {
        StringWriter text = new StringWriter();
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(text);
        try {
            type.serializeTo(writer, value, config);
            writer.flush();
            return text.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }
}
