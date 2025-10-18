package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Gender2025Test extends TestBase {

    @Test
    public void testIntValue() {
        assertEquals(0, Gender.BLANK.intValue());
        assertEquals(1, Gender.FEMALE.intValue());
        assertEquals(2, Gender.MALE.intValue());
        assertEquals(3, Gender.X.intValue());
    }

    @Test
    public void testValueOf_withValidIntValues() {
        assertEquals(Gender.BLANK, Gender.valueOf(0));
        assertEquals(Gender.FEMALE, Gender.valueOf(1));
        assertEquals(Gender.MALE, Gender.valueOf(2));
        assertEquals(Gender.X, Gender.valueOf(3));
    }

    @Test
    public void testValueOf_withInvalidIntValue() {
        assertThrows(IllegalArgumentException.class, () -> Gender.valueOf(-1));
        assertThrows(IllegalArgumentException.class, () -> Gender.valueOf(4));
        assertThrows(IllegalArgumentException.class, () -> Gender.valueOf(100));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(Gender.BLANK, Gender.valueOf("BLANK"));
        assertEquals(Gender.FEMALE, Gender.valueOf("FEMALE"));
        assertEquals(Gender.MALE, Gender.valueOf("MALE"));
        assertEquals(Gender.X, Gender.valueOf("X"));
    }

    @Test
    public void testValues() {
        Gender[] values = Gender.values();
        assertEquals(4, values.length);
        assertEquals(Gender.BLANK, values[0]);
        assertEquals(Gender.FEMALE, values[1]);
        assertEquals(Gender.MALE, values[2]);
        assertEquals(Gender.X, values[3]);
    }

    @Test
    public void testEnumName() {
        assertEquals("BLANK", Gender.BLANK.name());
        assertEquals("FEMALE", Gender.FEMALE.name());
        assertEquals("MALE", Gender.MALE.name());
        assertEquals("X", Gender.X.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLANK", Gender.BLANK.toString());
        assertEquals("FEMALE", Gender.FEMALE.toString());
        assertEquals("MALE", Gender.MALE.toString());
        assertEquals("X", Gender.X.toString());
    }
}
