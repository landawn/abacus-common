package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjIntPredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        ObjIntPredicate<String> predicate = (t, u) -> t.length() > u;
        assertTrue(predicate.test("hello", 3));
        assertFalse(predicate.test("hi", 5));
    }

    @Test
    public void testNegate() {
        ObjIntPredicate<String> predicate = (t, u) -> t.length() > u;
        assertFalse(predicate.negate().test("hello", 3));
        assertTrue(predicate.negate().test("hi", 5));
    }

    @Test
    public void testAnd() {
        ObjIntPredicate<String> p1 = (t, u) -> t.length() > u;
        ObjIntPredicate<String> p2 = (t, u) -> u > 0;
        assertTrue(p1.and(p2).test("hello", 3));
        assertFalse(p1.and(p2).test("hi", 5));
    }

    @Test
    public void testOr() {
        ObjIntPredicate<String> p1 = (t, u) -> t.length() > 10;
        ObjIntPredicate<String> p2 = (t, u) -> u > 10;
        assertTrue(p1.or(p2).test("hi", 15));
        assertFalse(p1.or(p2).test("hi", 5));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjIntPredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
