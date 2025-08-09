package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MapEntity;

public class MapEntityType100Test extends TestBase {

    private MapEntityType mapEntityType;

    @BeforeEach
    public void setUp() {
        mapEntityType = (MapEntityType) createType("MapEntity");
    }

    @Test
    public void testClazz() {
        assertEquals(MapEntity.class, mapEntityType.clazz());
    }

    @Test
    public void testIsMapEntity() {
        assertTrue(mapEntityType.isMapEntity());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(mapEntityType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.MAP_ENTITY, mapEntityType.getSerializationType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(mapEntityType.stringOf(null));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(mapEntityType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        assertNull(mapEntityType.valueOf(""));
    }
}
