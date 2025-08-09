package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NameUtil100Test extends TestBase {

    @Test
    public void testIsCachedName() {
        String testName = "testName";
        
        // Initially not cached
        Assertions.assertFalse(NameUtil.isCachedName(testName));
        
        // Cache it
        NameUtil.getCachedName(testName);
        
        // Now it should be cached
        Assertions.assertTrue(NameUtil.isCachedName(testName));
    }

    @Test
    public void testGetCachedName() {
        String name1 = "firstName";
        String name2 = "firstName";
        
        String cached1 = NameUtil.getCachedName(name1);
        String cached2 = NameUtil.getCachedName(name2);
        
        // Should return the same reference (interned)
        Assertions.assertSame(cached1, cached2);
        Assertions.assertEquals(name1, cached1);
    }

    @Test
    public void testCacheName() {
        String name = "lastName";
        
        // Cache with force = false
        String cached1 = NameUtil.cacheName(name, false);
        Assertions.assertEquals(name, cached1);
        
        // Cache again with force = false (should not re-cache)
        String cached2 = NameUtil.cacheName(name, false);
        Assertions.assertSame(cached1, cached2);
        
        // Cache with force = true
        String cached3 = NameUtil.cacheName("forcedName", true);
        Assertions.assertEquals("forcedName", cached3);
    }

    @Test
    public void testIsCanonicalName() {
        // Test valid canonical names
        Assertions.assertTrue(NameUtil.isCanonicalName("com.example", "com.example.Person"));
        Assertions.assertTrue(NameUtil.isCanonicalName("com.example.Person", "com.example.Person.firstName"));
        
        // Test invalid canonical names
        Assertions.assertFalse(NameUtil.isCanonicalName("com.example", "com.other.Person"));
        Assertions.assertFalse(NameUtil.isCanonicalName("com.example.Person", "com.example"));
        Assertions.assertFalse(NameUtil.isCanonicalName("com.example", "com.example"));
    }

    @Test
    public void testGetSimpleName() {
        // Test with canonical names
        Assertions.assertEquals("firstName", NameUtil.getSimpleName("com.example.Person.firstName"));
        Assertions.assertEquals("Person", NameUtil.getSimpleName("com.example.Person"));
        
        // Test with simple names (no dots)
        Assertions.assertEquals("simpleName", NameUtil.getSimpleName("simpleName"));
        
        // Test with empty parent
        Assertions.assertEquals("hidden", NameUtil.getSimpleName(".hidden"));
    }

    @Test
    public void testGetParentName() {
        // Test with canonical names
        Assertions.assertEquals("com.example.Person", NameUtil.getParentName("com.example.Person.firstName"));
        Assertions.assertEquals("com.example", NameUtil.getParentName("com.example.Person"));
        
        // Test with simple names (no dots)
        Assertions.assertEquals("", NameUtil.getParentName("simpleName"));
        
        // Test with leading dot
        Assertions.assertEquals("", NameUtil.getParentName(".hidden"));
        
        // Test with single character before dot
        Assertions.assertEquals("a", NameUtil.getParentName("a.b"));
        Assertions.assertEquals("ab", NameUtil.getParentName("ab.c"));
    }

    @Test
    public void testCachingBehavior() {
        // Test that multiple calls return the same cached instance
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
        
        // Test getting simple name
        Assertions.assertEquals("name", NameUtil.getSimpleName(fullName));
        
        // Test getting parent name
        String parent = NameUtil.getParentName(fullName);
        Assertions.assertEquals("com.landawn.abacus.util.Person.address.street", parent);
        
        // Test canonical relationship
        Assertions.assertTrue(NameUtil.isCanonicalName(parent, fullName));
        
        // Navigate up the hierarchy
        String grandParent = NameUtil.getParentName(parent);
        Assertions.assertEquals("com.landawn.abacus.util.Person.address", grandParent);
        
        String greatGrandParent = NameUtil.getParentName(grandParent);
        Assertions.assertEquals("com.landawn.abacus.util.Person", greatGrandParent);
    }

    @Test
    public void testEdgeCases() {
        // Empty string
        Assertions.assertEquals("", NameUtil.getSimpleName(""));
        Assertions.assertEquals("", NameUtil.getParentName(""));
        
        // Only dots
        Assertions.assertEquals("", NameUtil.getSimpleName("..."));
        Assertions.assertEquals("..", NameUtil.getParentName("..."));
        
        // Trailing dot
        Assertions.assertEquals("", NameUtil.getSimpleName("com.example."));
        Assertions.assertEquals("com.example", NameUtil.getParentName("com.example."));
    }
}