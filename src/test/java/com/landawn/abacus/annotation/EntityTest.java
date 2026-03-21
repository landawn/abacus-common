package com.landawn.abacus.annotation;

import com.landawn.abacus.TestBase;

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
