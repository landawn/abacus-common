package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MediaType2025Test extends TestBase {

    @Test
    public void testIntValue_BINARY() {
        assertEquals(0, MediaType.BINARY.intValue());
    }

    @Test
    public void testIntValue_AUDIO() {
        assertEquals(1, MediaType.AUDIO.intValue());
    }

    @Test
    public void testIntValue_VIDEO() {
        assertEquals(2, MediaType.VIDEO.intValue());
    }

    @Test
    public void testIntValue_IMAGE() {
        assertEquals(3, MediaType.IMAGE.intValue());
    }

    @Test
    public void testIntValue_TEXT() {
        assertEquals(4, MediaType.TEXT.intValue());
    }

    @Test
    public void testIntValue_RECORD() {
        assertEquals(5, MediaType.RECORD.intValue());
    }

    @Test
    public void testValueOf_0() {
        MediaType type = MediaType.valueOf(0);
        assertEquals(MediaType.BINARY, type);
    }

    @Test
    public void testValueOf_1() {
        MediaType type = MediaType.valueOf(1);
        assertEquals(MediaType.AUDIO, type);
    }

    @Test
    public void testValueOf_2() {
        MediaType type = MediaType.valueOf(2);
        assertEquals(MediaType.VIDEO, type);
    }

    @Test
    public void testValueOf_3() {
        MediaType type = MediaType.valueOf(3);
        assertEquals(MediaType.IMAGE, type);
    }

    @Test
    public void testValueOf_4() {
        MediaType type = MediaType.valueOf(4);
        assertEquals(MediaType.TEXT, type);
    }

    @Test
    public void testValueOf_5() {
        MediaType type = MediaType.valueOf(5);
        assertEquals(MediaType.RECORD, type);
    }

    @Test
    public void testValueOf_invalid_negative() {
        assertThrows(IllegalArgumentException.class, () -> MediaType.valueOf(-1));
    }

    @Test
    public void testValueOf_invalid_tooLarge() {
        assertThrows(IllegalArgumentException.class, () -> MediaType.valueOf(6));
    }

    @Test
    public void testValueOf_invalid_100() {
        assertThrows(IllegalArgumentException.class, () -> MediaType.valueOf(100));
    }

    @Test
    public void testValueOf_roundTrip_BINARY() {
        MediaType original = MediaType.BINARY;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_AUDIO() {
        MediaType original = MediaType.AUDIO;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_VIDEO() {
        MediaType original = MediaType.VIDEO;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_IMAGE() {
        MediaType original = MediaType.IMAGE;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_TEXT() {
        MediaType original = MediaType.TEXT;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_RECORD() {
        MediaType original = MediaType.RECORD;
        MediaType converted = MediaType.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_byName_BINARY() {
        MediaType type = MediaType.valueOf("BINARY");
        assertEquals(MediaType.BINARY, type);
    }

    @Test
    public void testValueOf_byName_AUDIO() {
        MediaType type = MediaType.valueOf("AUDIO");
        assertEquals(MediaType.AUDIO, type);
    }

    @Test
    public void testValueOf_byName_VIDEO() {
        MediaType type = MediaType.valueOf("VIDEO");
        assertEquals(MediaType.VIDEO, type);
    }

    @Test
    public void testValueOf_byName_IMAGE() {
        MediaType type = MediaType.valueOf("IMAGE");
        assertEquals(MediaType.IMAGE, type);
    }

    @Test
    public void testValueOf_byName_TEXT() {
        MediaType type = MediaType.valueOf("TEXT");
        assertEquals(MediaType.TEXT, type);
    }

    @Test
    public void testValueOf_byName_RECORD() {
        MediaType type = MediaType.valueOf("RECORD");
        assertEquals(MediaType.RECORD, type);
    }

    @Test
    public void testValues() {
        MediaType[] types = MediaType.values();
        assertNotNull(types);
        assertEquals(6, types.length);
    }

    @Test
    public void testValues_order() {
        MediaType[] types = MediaType.values();
        assertEquals(MediaType.BINARY, types[0]);
        assertEquals(MediaType.AUDIO, types[1]);
        assertEquals(MediaType.VIDEO, types[2]);
        assertEquals(MediaType.IMAGE, types[3]);
        assertEquals(MediaType.TEXT, types[4]);
        assertEquals(MediaType.RECORD, types[5]);
    }

    @Test
    public void testIntValue_uniqueness() {
        MediaType[] types = MediaType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                if (types[i].intValue() == types[j].intValue()) {
                    throw new AssertionError("Duplicate int value: " + types[i] + " and " + types[j]);
                }
            }
        }
    }

    @Test
    public void testSwitchStatement() {
        MediaType type = MediaType.VIDEO;
        String result = switch (type) {
            case BINARY -> "binary";
            case AUDIO -> "audio";
            case VIDEO -> "video";
            case IMAGE -> "image";
            case TEXT -> "text";
            case RECORD -> "record";
        };
        assertEquals("video", result);
    }

    @Test
    public void testIntegration_serializationDeserialization() {
        MediaType[] types = MediaType.values();
        for (MediaType type : types) {
            int serialized = type.intValue();
            MediaType deserialized = MediaType.valueOf(serialized);
            assertEquals(type, deserialized);
        }
    }
}
