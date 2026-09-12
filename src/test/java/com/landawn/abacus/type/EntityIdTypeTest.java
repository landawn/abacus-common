package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.EntityId;

public class EntityIdTypeTest extends TestBase {

    private EntityIdType entityIdType;

    @BeforeEach
    public void setUp() {
        entityIdType = (EntityIdType) createType(EntityId.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(EntityId.class, entityIdType.javaType());
    }

    @Test
    public void testIsEntityId() {
        assertTrue(entityIdType.isEntityId());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(entityIdType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.ENTITY_ID, entityIdType.serializationType());
    }

    @Test
    public void testStringOf() {
        assertNull(entityIdType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(entityIdType.valueOf(null));
        assertNull(entityIdType.valueOf(""));

    }

    // ---- review fixes 2026-09-06, T2-09: the empty entity name round-trips through the empty key ----

    @Test
    public void reviewFixes20260906_emptyEntityNameSerializesAsEmptyKeyAndRoundTrips() {
        assertEquals("{\"\": {}}", entityIdType.stringOf(EntityId.builder().build()));
        assertEquals("{\"\": {\"id\": 1}}", entityIdType.stringOf(EntityId.of("id", 1)));
        assertEquals("{\"\": {\"id\": 1}}", entityIdType.stringOf(EntityId.create(com.landawn.abacus.util.N.asMap("id", 1))));

        // T2-09 fix: the parser accepts the empty entity name, so the documented factories round-trip.
        assertTrue(entityIdType.valueOf("{\"\": {}}").isEmpty());
        assertEquals("", entityIdType.valueOf("{\"\": {}}").entityName());
        assertEquals(Integer.valueOf(1), entityIdType.valueOf("{\"\": {\"id\": 1}}").get("id"));
        assertEquals(EntityId.of("id", 1), entityIdType.valueOf(entityIdType.stringOf(EntityId.of("id", 1))));
    }

    @Test
    public void reviewFixes20260906_emptyObjectIsNullAndNamedIdsRoundTrip() {
        assertNull(entityIdType.valueOf("{}"));
        assertNull(entityIdType.valueOf("  "));

        final EntityId named = entityIdType.valueOf("{\"Acct\": {\"id\": 7}}");
        assertEquals("Acct", named.entityName());
        assertEquals(7, (int) named.get("id"));
        assertEquals(EntityId.of("Acct.id", 7), entityIdType.valueOf(entityIdType.stringOf(EntityId.of("Acct.id", 7))));
    }

    @Test
    public void reviewFixes20260906_missingEntityNameIsStillAParsingException() {
        // An empty QUOTED name is legal (above); a missing name token is not - the guard the parser kept.
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> entityIdType.valueOf("{{\"id\": 1}}"));
    }
}
