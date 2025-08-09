package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UUIDType100Test extends TestBase {

    private UUIDType uuidType;
    private UUID testUUID;

    @BeforeEach
    public void setUp() {
        uuidType = (UUIDType) createType(UUID.class);
        testUUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    public void testClazz() {
        Class<?> clazz = uuidType.clazz();
        assertNotNull(clazz);
        assertEquals(UUID.class, clazz);
    }

    @Test
    public void testStringOf() {
        String result = uuidType.stringOf(testUUID);
        assertNotNull(result);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", result);
    }

    @Test
    public void testStringOfNull() {
        String result = uuidType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        UUID result = uuidType.valueOf("550e8400-e29b-41d4-a716-446655440000");
        assertNotNull(result);
        assertEquals(testUUID, result);
    }

    @Test
    public void testValueOfEmptyString() {
        UUID result = uuidType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        UUID result = uuidType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOfInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () -> {
            uuidType.valueOf("not-a-valid-uuid");
        });
    }

    @Test
    public void testValueOfRandomUUID() {
        String randomUUIDString = UUID.randomUUID().toString();
        UUID result = uuidType.valueOf(randomUUIDString);
        assertNotNull(result);
        assertEquals(randomUUIDString, result.toString());
    }
}
