package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class NameUtil100Test extends TestBase {

    @Test
    public void testIsCachedName() {
        String testName = "testName";

        Assertions.assertFalse(NameUtil.isCachedName(testName));

        NameUtil.getCachedName(testName);

        Assertions.assertTrue(NameUtil.isCachedName(testName));
    }

    @Test
    public void testGetCachedName() {
        String name1 = "firstName";
        String name2 = "firstName";

        String cached1 = NameUtil.getCachedName(name1);
        String cached2 = NameUtil.getCachedName(name2);

        Assertions.assertSame(cached1, cached2);
        Assertions.assertEquals(name1, cached1);
    }

    @Test
    public void testCacheName() {
        String name = "lastName";

        String cached1 = NameUtil.cacheName(name, false);
        Assertions.assertEquals(name, cached1);

        String cached2 = NameUtil.cacheName(name, false);
        Assertions.assertSame(cached1, cached2);

        String cached3 = NameUtil.cacheName("forcedName", true);
        Assertions.assertEquals("forcedName", cached3);
    }

    @Test
    public void testIsCanonicalName() {
        Assertions.assertTrue(NameUtil.isCanonicalName("com.example", "com.example.Person"));
        Assertions.assertTrue(NameUtil.isCanonicalName("com.example.Person", "com.example.Person.firstName"));

        Assertions.assertFalse(NameUtil.isCanonicalName("com.example", "com.other.Person"));
        Assertions.assertFalse(NameUtil.isCanonicalName("com.example.Person", "com.example"));
        Assertions.assertFalse(NameUtil.isCanonicalName("com.example", "com.example"));
    }

    @Test
    public void testGetSimpleName() {
        Assertions.assertEquals("firstName", NameUtil.getSimpleName("com.example.Person.firstName"));
        Assertions.assertEquals("Person", NameUtil.getSimpleName("com.example.Person"));

        Assertions.assertEquals("simpleName", NameUtil.getSimpleName("simpleName"));

        Assertions.assertEquals("hidden", NameUtil.getSimpleName(".hidden"));
    }

    @Test
    public void testGetParentName() {
        Assertions.assertEquals("com.example.Person", NameUtil.getParentName("com.example.Person.firstName"));
        Assertions.assertEquals("com.example", NameUtil.getParentName("com.example.Person"));

        Assertions.assertEquals("", NameUtil.getParentName("simpleName"));

        Assertions.assertEquals("", NameUtil.getParentName(".hidden"));

        Assertions.assertEquals("a", NameUtil.getParentName("a.b"));
        Assertions.assertEquals("ab", NameUtil.getParentName("ab.c"));
    }

    @Test
    public void testCachingBehavior() {
        String name = "com.test.CachedName";
        String simple1 = NameUtil.getSimpleName(name);
        String simple2 = NameUtil.getSimpleName(name);
        Assertions.assertSame(simple1, simple2);

        String parent1 = NameUtil.getParentName(name);
        String parent2 = NameUtil.getParentName(name);
        Assertions.assertSame(parent1, parent2);
    }

    @Test
    public void testComplexHierarchy() {
        String fullName = "com.landawn.abacus.util.Person.address.street.name";

        Assertions.assertEquals("name", NameUtil.getSimpleName(fullName));

        String parent = NameUtil.getParentName(fullName);
        Assertions.assertEquals("com.landawn.abacus.util.Person.address.street", parent);

        Assertions.assertTrue(NameUtil.isCanonicalName(parent, fullName));

        String grandParent = NameUtil.getParentName(parent);
        Assertions.assertEquals("com.landawn.abacus.util.Person.address", grandParent);

        String greatGrandParent = NameUtil.getParentName(grandParent);
        Assertions.assertEquals("com.landawn.abacus.util.Person", greatGrandParent);
    }

    @Test
    public void testEdgeCases() {
        Assertions.assertEquals("", NameUtil.getSimpleName(""));
        Assertions.assertEquals("", NameUtil.getParentName(""));

        Assertions.assertEquals("", NameUtil.getSimpleName("..."));
        Assertions.assertEquals("..", NameUtil.getParentName("..."));

        Assertions.assertEquals("", NameUtil.getSimpleName("com.example."));
        Assertions.assertEquals("com.example", NameUtil.getParentName("com.example."));
    }
}
