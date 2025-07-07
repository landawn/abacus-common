package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.EntityId;

public class EntityIdType100Test extends TestBase {

    private EntityIdType entityIdType;

    @BeforeEach
    public void setUp() {
        entityIdType = (EntityIdType) createType(EntityId.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(EntityId.class, entityIdType.clazz());
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
        assertEquals(Type.SerializationType.ENTITY_ID, entityIdType.getSerializationType());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(entityIdType.stringOf(null));

        // Test with EntityId would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(entityIdType.valueOf(null));
        assertNull(entityIdType.valueOf(""));

        // Test with actual JSON would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }
}
