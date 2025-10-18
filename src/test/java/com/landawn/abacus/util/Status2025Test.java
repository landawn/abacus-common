package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Status2025Test extends TestBase {
    @Test
    public void testEnumName() {
        assertEquals("BLANK", Status.BLANK.name());
        assertEquals("ACTIVE", Status.ACTIVE.name());
        assertEquals("PROCESSING", Status.PROCESSING.name());
        assertEquals("READ_ONLY", Status.READ_ONLY.name());
        assertEquals("DESTROYED", Status.DESTROYED.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLANK", Status.BLANK.toString());
        assertEquals("ACTIVE", Status.ACTIVE.toString());
        assertEquals("PROCESSING", Status.PROCESSING.toString());
        assertEquals("READ_ONLY", Status.READ_ONLY.toString());
        assertEquals("DESTROYED", Status.DESTROYED.toString());
    }
}
