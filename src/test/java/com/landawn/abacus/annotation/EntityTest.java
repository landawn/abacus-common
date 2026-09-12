package com.landawn.abacus.annotation;

import com.landawn.abacus.TestBase;

public class EntityTest extends TestBase {

    @Entity
    public static class TestEntity1 {
    }

    @Entity(name = "users")
    public static class TestEntity2 {
    }

    @Entity(value = "old_value")
    @Deprecated
    public static class TestEntity3 {
    }

    @Entity(name = "new_name", value = "old_value")
    @Deprecated
    public static class TestEntity4 {
    }
}
