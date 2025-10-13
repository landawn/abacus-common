package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.EntityId;

@Tag("new-test")
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
        assertNull(entityIdType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(entityIdType.valueOf(null));
        assertNull(entityIdType.valueOf(""));

    }
}
