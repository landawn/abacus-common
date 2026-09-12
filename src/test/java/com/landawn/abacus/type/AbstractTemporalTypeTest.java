package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractTemporalTypeTest extends TestBase {

    private Type<Temporal> temporalType;

    @BeforeEach
    public void setUp() {
        temporalType = createType("LocalDateTime");
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(temporalType.isCsvQuoteRequired());
    }

    @Test
    public void test_isTemporal() {
        assertTrue(temporalType.isTemporal());
    }

    // --- review fixes 2026-09-06 (T10-01) ---

    @Test
    public void reviewFixes20260906_T1001_sharedFormattersResolveStrictly() {
        // toFormatter() defaults to SMART, which rewrote Feb 30 -> Feb 28 on the fast-path shapes
        assertEquals(ResolverStyle.STRICT, AbstractTemporalType.iso8601DateTimeDTF.getResolverStyle());
        assertEquals(ResolverStyle.STRICT, AbstractTemporalType.iso8601TimestampDTF.getResolverStyle());

        assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("2023-02-30T10:30:45Z", AbstractTemporalType.iso8601DateTimeDTF));
        assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("2023-02-30T10:30:45.123Z", AbstractTemporalType.iso8601TimestampDTF));
        assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("2023-10-15T24:00:00Z", AbstractTemporalType.iso8601DateTimeDTF));

        // valid text and formatting are unchanged (resolver style affects parsing only)
        assertEquals(OffsetDateTime.parse("2024-02-29T00:00:00Z"), OffsetDateTime.parse("2024-02-29T00:00:00Z", AbstractTemporalType.iso8601DateTimeDTF));
        final ZonedDateTime z = ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 123000000, ZoneId.of("+05:30"));
        assertEquals("2023-10-15T10:30:45+05:30", AbstractTemporalType.iso8601DateTimeDTF.format(z));
        assertEquals("2023-10-15T10:30:45.123+05:30", AbstractTemporalType.iso8601TimestampDTF.format(z));
        assertEquals("2023-10-15T05:00:45.123Z", AbstractTemporalType.iso8601TimestampDTF.format(z.toInstant().atZone(ZoneId.of("UTC"))));
    }
}
