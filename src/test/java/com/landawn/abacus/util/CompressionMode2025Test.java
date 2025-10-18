package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CompressionMode2025Test extends TestBase {

    @Test
    public void testValueOf_withStringName() {
        assertEquals(CompressionMode.NONE, CompressionMode.valueOf("NONE"));
        assertEquals(CompressionMode.LZ4, CompressionMode.valueOf("LZ4"));
        assertEquals(CompressionMode.SNAPPY, CompressionMode.valueOf("SNAPPY"));
        assertEquals(CompressionMode.GZIP, CompressionMode.valueOf("GZIP"));
    }

    @Test
    public void testValues() {
        CompressionMode[] values = CompressionMode.values();
        assertEquals(4, values.length);
        assertEquals(CompressionMode.NONE, values[0]);
        assertEquals(CompressionMode.LZ4, values[1]);
        assertEquals(CompressionMode.SNAPPY, values[2]);
        assertEquals(CompressionMode.GZIP, values[3]);
    }

    @Test
    public void testEnumName() {
        assertEquals("NONE", CompressionMode.NONE.name());
        assertEquals("LZ4", CompressionMode.LZ4.name());
        assertEquals("SNAPPY", CompressionMode.SNAPPY.name());
        assertEquals("GZIP", CompressionMode.GZIP.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("NONE", CompressionMode.NONE.toString());
        assertEquals("LZ4", CompressionMode.LZ4.toString());
        assertEquals("SNAPPY", CompressionMode.SNAPPY.toString());
        assertEquals("GZIP", CompressionMode.GZIP.toString());
    }

    @Test
    public void testOrdinal() {
        assertEquals(0, CompressionMode.NONE.ordinal());
        assertEquals(1, CompressionMode.LZ4.ordinal());
        assertEquals(2, CompressionMode.SNAPPY.ordinal());
        assertEquals(3, CompressionMode.GZIP.ordinal());
    }
}
