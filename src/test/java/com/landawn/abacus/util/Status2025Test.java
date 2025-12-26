package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Status2025Test extends TestBase {
    @Test
    public void testEnumName() {
        assertEquals("BLANK", UnifiedStatus.BLANK.name());
        assertEquals("ACTIVE", UnifiedStatus.ACTIVE.name());
        assertEquals("PROCESSING", UnifiedStatus.PROCESSING.name());
        assertEquals("READ_ONLY", UnifiedStatus.READ_ONLY.name());
        assertEquals("DESTROYED", UnifiedStatus.DESTROYED.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLANK", UnifiedStatus.BLANK.toString());
        assertEquals("ACTIVE", UnifiedStatus.ACTIVE.toString());
        assertEquals("PROCESSING", UnifiedStatus.PROCESSING.toString());
        assertEquals("READ_ONLY", UnifiedStatus.READ_ONLY.toString());
        assertEquals("DESTROYED", UnifiedStatus.DESTROYED.toString());
    }
}
