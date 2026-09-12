package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MapEntity;

public class MapEntityTypeTest extends TestBase {

    private MapEntityType mapEntityType;

    @BeforeEach
    public void setUp() {
        mapEntityType = (MapEntityType) createType("MapEntity");
    }

    @Test
    public void testClazz() {
        assertEquals(MapEntity.class, mapEntityType.javaType());
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
        assertEquals(Type.SerializationType.MAP_ENTITY, mapEntityType.serializationType());
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

    // ---- review fixes 2026-09-06, T2-09: the empty entity name round-trips; "{}" is null ----

    @Test
    public void reviewFixes20260906_emptyNameSerializesAsEmptyKeyAndRoundTrips() {
        assertEquals("{\"\": {}}", mapEntityType.stringOf(new MapEntity("")));
        // T2-09 fix: the parser accepts the empty entity name, so an unnamed MapEntity round-trips.
        assertEquals("", mapEntityType.valueOf("{\"\": {}}").entityName());
        assertTrue(mapEntityType.valueOf(mapEntityType.stringOf(new MapEntity(""))).isEmpty());
    }

    @Test
    public void reviewFixes20260906_emptyObjectIsNullAndNamedEntityKeepsItsName() {
        assertNull(mapEntityType.valueOf("{}"));
        assertNull(mapEntityType.valueOf("  "));

        final MapEntity named = mapEntityType.valueOf("{\"E\": {}}");
        assertEquals("E", named.entityName());
        assertTrue(named.isEmpty());

        final MapEntity withProps = mapEntityType.valueOf("{\"E\": {\"id\": 3}}");
        assertEquals("E", withProps.entityName());
        assertEquals(3, (int) withProps.get("id"));
    }

    @Test
    public void reviewFixes20260906_missingEntityNameIsStillAParsingException() {
        // An empty QUOTED name is legal (above); a missing name token is not - the guard the parser kept.
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> mapEntityType.valueOf("{{\"id\": 3}}"));
    }
}
