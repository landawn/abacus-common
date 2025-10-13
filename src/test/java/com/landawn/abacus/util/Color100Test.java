package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Color100Test extends TestBase {

    @Test
    public void testEnumValues() {
        Color[] values = Color.values();
        assertEquals(9, values.length);

        assertEquals(Color.BLACK, values[0]);
        assertEquals(Color.WHITE, values[1]);
        assertEquals(Color.RED, values[2]);
        assertEquals(Color.ORANGE, values[3]);
        assertEquals(Color.YELLOW, values[4]);
        assertEquals(Color.GREEN, values[5]);
        assertEquals(Color.CYAN, values[6]);
        assertEquals(Color.BLUE, values[7]);
        assertEquals(Color.PURPLE, values[8]);
    }

    @Test
    public void testIntValue() {
        assertEquals(0, Color.BLACK.intValue());
        assertEquals(1, Color.WHITE.intValue());
        assertEquals(2, Color.RED.intValue());
        assertEquals(3, Color.ORANGE.intValue());
        assertEquals(4, Color.YELLOW.intValue());
        assertEquals(5, Color.GREEN.intValue());
        assertEquals(6, Color.CYAN.intValue());
        assertEquals(7, Color.BLUE.intValue());
        assertEquals(8, Color.PURPLE.intValue());
    }

    @Test
    public void testValueOfInt() {
        assertEquals(Color.BLACK, Color.valueOf(0));
        assertEquals(Color.WHITE, Color.valueOf(1));
        assertEquals(Color.RED, Color.valueOf(2));
        assertEquals(Color.ORANGE, Color.valueOf(3));
        assertEquals(Color.YELLOW, Color.valueOf(4));
        assertEquals(Color.GREEN, Color.valueOf(5));
        assertEquals(Color.CYAN, Color.valueOf(6));
        assertEquals(Color.BLUE, Color.valueOf(7));
        assertEquals(Color.PURPLE, Color.valueOf(8));
    }

    @Test
    public void testValueOfIntInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Color.valueOf(-1));
        assertThrows(IllegalArgumentException.class, () -> Color.valueOf(9));
        assertThrows(IllegalArgumentException.class, () -> Color.valueOf(100));
    }

    @Test
    public void testValueOfString() {
        assertEquals(Color.BLACK, Color.valueOf("BLACK"));
        assertEquals(Color.WHITE, Color.valueOf("WHITE"));
        assertEquals(Color.RED, Color.valueOf("RED"));
        assertEquals(Color.ORANGE, Color.valueOf("ORANGE"));
        assertEquals(Color.YELLOW, Color.valueOf("YELLOW"));
        assertEquals(Color.GREEN, Color.valueOf("GREEN"));
        assertEquals(Color.CYAN, Color.valueOf("CYAN"));
        assertEquals(Color.BLUE, Color.valueOf("BLUE"));
        assertEquals(Color.PURPLE, Color.valueOf("PURPLE"));
    }

    @Test
    public void testValueOfStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Color.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> Color.valueOf("black"));
    }

    @Test
    public void testEnumName() {
        assertEquals("BLACK", Color.BLACK.name());
        assertEquals("WHITE", Color.WHITE.name());
        assertEquals("RED", Color.RED.name());
        assertEquals("ORANGE", Color.ORANGE.name());
        assertEquals("YELLOW", Color.YELLOW.name());
        assertEquals("GREEN", Color.GREEN.name());
        assertEquals("CYAN", Color.CYAN.name());
        assertEquals("BLUE", Color.BLUE.name());
        assertEquals("PURPLE", Color.PURPLE.name());
    }

    @Test
    public void testEnumOrdinal() {
        assertEquals(0, Color.BLACK.ordinal());
        assertEquals(1, Color.WHITE.ordinal());
        assertEquals(2, Color.RED.ordinal());
        assertEquals(3, Color.ORANGE.ordinal());
        assertEquals(4, Color.YELLOW.ordinal());
        assertEquals(5, Color.GREEN.ordinal());
        assertEquals(6, Color.CYAN.ordinal());
        assertEquals(7, Color.BLUE.ordinal());
        assertEquals(8, Color.PURPLE.ordinal());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLACK", Color.BLACK.toString());
        assertEquals("WHITE", Color.WHITE.toString());
        assertEquals("RED", Color.RED.toString());
        assertEquals("ORANGE", Color.ORANGE.toString());
        assertEquals("YELLOW", Color.YELLOW.toString());
        assertEquals("GREEN", Color.GREEN.toString());
        assertEquals("CYAN", Color.CYAN.toString());
        assertEquals("BLUE", Color.BLUE.toString());
        assertEquals("PURPLE", Color.PURPLE.toString());
    }

    @Test
    public void testRoundTrip() {
        for (Color color : Color.values()) {
            assertEquals(color, Color.valueOf(color.intValue()));
            assertEquals(color.intValue(), Color.valueOf(color.intValue()).intValue());
        }
    }
}
