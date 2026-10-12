package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Sheet;

public class SheetTemporalKeysReview20261003Test extends TestBase {
    static Stream<Arguments> configuredKeys() {
        return Stream
                .of(Instant.parse("2024-01-01T00:00:00.123456789Z"), OffsetDateTime.parse("2024-01-01T01:00:00.123456789+01:00"),
                        ZonedDateTime.parse("2024-01-01T01:00:00.123456789+01:00[Europe/Paris]"))
                .flatMap(key -> Stream.of(DateTimeFormat.LONG, DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.ISO_8601_TIMESTAMP)
                        .map(format -> Arguments.of(key, format)));
    }

    @ParameterizedTest
    @MethodSource("configuredKeys")
    public void repeatedLabelsMatchTheConfiguredTemporalKey(final Object key, final DateTimeFormat format) {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Sheet<String, Object, Integer> source = Sheet.columns(List.of("r"), List.of(key), List.of(List.of(7)));
        final String json = parser.serialize(source, JsonSerConfig.create().setWriteRowColumnKeyType(true).setDateTimeFormat(format));
        final Map<?, ?> document = parser.deserialize(json, Map.class);
        final String wireKey = ((List<?>) document.get("columnKeySet")).get(0).toString();
        final Object expectedKey = Type.of(key.getClass()).valueOf(wireKey);
        final Sheet<String, Object, Integer> copy = parser.deserialize(json, Sheet.class);

        // Selected formats intentionally change precision/zone. Both references must resolve that same key.
        assertEquals(Integer.valueOf(7), copy.get("r", expectedKey));
        assertEquals(List.of(expectedKey), List.copyOf(copy.columnKeySet()));
    }

    @Test
    public void untypedSheetReadsDefaultEpochDateColumnKeys() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Date key = new Date(1_704_067_200_123L);
        final Sheet<String, Date, Integer> source = Sheet.columns(List.of("r"), List.of(key), List.of(List.of(9)));
        final String json = parser.serialize(source);
        final Sheet<String, String, Integer> copy = parser.deserialize(json, Sheet.class);
        assertEquals(Integer.valueOf(9), copy.get("r", Long.toString(key.getTime())));
    }

    @Test
    public void naturalTemporalTextPreservesNanosecondsAndZone() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final ZonedDateTime key = ZonedDateTime.parse("2024-01-01T01:00:00.123456789+01:00[Europe/Paris]");
        final Sheet<String, ZonedDateTime, Integer> source = Sheet.columns(List.of("r"), List.of(key), List.of(List.of(7)));
        final String json = parser.serialize(source, JsonSerConfig.create().setWriteRowColumnKeyType(true).setDateTimeFormat(null));
        final Sheet<String, ZonedDateTime, Integer> copy = parser.deserialize(json, Sheet.class);
        assertEquals(Integer.valueOf(7), copy.get("r", key));
    }
}
