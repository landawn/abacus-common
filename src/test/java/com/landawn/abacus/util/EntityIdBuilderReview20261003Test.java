package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class EntityIdBuilderReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(ints = { 1, 3 })
    void emptySnapshotsPreserveFirstPropertyNameInference(final int snapshotCount) {
        final EntityId.EntityIdBuilder builder = EntityId.builder();
        EntityId previous = null;

        for (int i = 0; i < snapshotCount; i++) {
            final EntityId empty = builder.build();
            assertNotSame(previous, empty);
            assertTrue(empty.isEmpty());
            assertEquals("", empty.entityName());
            previous = empty;
        }

        final EntityId result = builder.put("User.id", 1).build();
        assertEquals(EntityId.of("User.id", 1), result);
        assertEquals("User", result.entityName());
        assertEquals(Set.of("id"), result.keySet());
        assertEquals(1, result.getInt("id"));
        assertTrue(previous.isEmpty());
        assertEquals("", previous.entityName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3 })
    void rejectedFirstPropertyDoesNotChooseAnEntityName(final int snapshotCount) {
        final EntityId.EntityIdBuilder builder = EntityId.builder();

        for (int i = 0; i < snapshotCount; i++) {
            assertTrue(builder.build().isEmpty());
        }

        assertThrows(IllegalArgumentException.class, () -> builder.put(null, 9));
        final EntityId result = builder.put("Account.key", 7).build();
        assertEquals(EntityId.of("Account.key", 7), result);
        assertEquals("Account", result.entityName());
        assertEquals(Set.of("key"), result.keySet());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "User")
    void explicitEntityNameRemainsChosenAfterEmptySnapshots(final String entityName) {
        final EntityId.EntityIdBuilder builder = EntityId.builder(entityName);
        final EntityId empty = builder.build();
        assertTrue(empty.isEmpty());
        assertNotSame(empty, builder.build());

        final EntityId result = builder.put("User.id", 3).build();
        assertEquals(EntityId.of(entityName, "User.id", 3), result);
        assertEquals(entityName == null ? "" : entityName, result.entityName());
        assertTrue(empty.isEmpty());
    }

    @Test
    void populatedSnapshotsRemainIndependent() {
        final EntityId.EntityIdBuilder builder = EntityId.builder().put("User.id", 1);
        final EntityId first = builder.build();
        final EntityId second = builder.put("User.id", 2).put("User.name", "Ada").build();

        assertNotSame(first, second);
        assertEquals(1, first.getInt("id"));
        assertFalse(first.containsKey("name"));
        assertEquals(2, second.getInt("id"));
        assertEquals("Ada", second.get("name"));
        assertEquals("User", second.entityName());
    }

    @Test
    void simpleFirstPropertyStillChoosesAnEmptyEntityName() {
        final EntityId.EntityIdBuilder builder = EntityId.builder();
        final EntityId empty = builder.build();
        final EntityId first = builder.put("id", 1).build();
        final EntityId second = builder.put("User.other", 2).build();

        assertEquals("", first.entityName());
        assertEquals(Set.of("id"), first.keySet());
        assertEquals("", second.entityName());
        assertEquals(Set.of("id", "User.other"), second.keySet());
        assertNull(second.get("other"));
        assertTrue(empty.isEmpty());
    }
}
