package com.landawn.abacus.annotation;

import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class EntityTest extends TestBase {

    @Entity
    static class TestEntity1 {
    }

    @Entity(name = "users")
    static class TestEntity2 {
    }

    @Entity(value = "old_value")
    @Deprecated
    static class TestEntity3 {
    }

    @Entity(name = "new_name", value = "old_value")
    @Deprecated
    static class TestEntity4 {
    }
}
