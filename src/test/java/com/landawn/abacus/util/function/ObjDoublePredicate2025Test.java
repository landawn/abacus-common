package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjDoublePredicate2025Test extends TestBase {

    @Test
    public void testTest() {
        ObjDoublePredicate<String> predicate = (t, u) -> t.length() > u;
        assertTrue(predicate.test("hello", 3.0));
        assertFalse(predicate.test("hi", 5.0));
    }

    @Test
    public void testNegate() {
        ObjDoublePredicate<String> predicate = (t, u) -> t.length() > u;
        assertFalse(predicate.negate().test("hello", 3.0));
        assertTrue(predicate.negate().test("hi", 5.0));
    }

    @Test
    public void testAnd() {
        ObjDoublePredicate<String> p1 = (t, u) -> t.length() > u;
        ObjDoublePredicate<String> p2 = (t, u) -> u > 0;
        assertTrue(p1.and(p2).test("hello", 3.0));
        assertFalse(p1.and(p2).test("hi", 5.0));
    }

    @Test
    public void testOr() {
        ObjDoublePredicate<String> p1 = (t, u) -> t.length() > 10;
        ObjDoublePredicate<String> p2 = (t, u) -> u > 10;
        assertTrue(p1.or(p2).test("hi", 15.0));
        assertFalse(p1.or(p2).test("hi", 5.0));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjDoublePredicate.class.getAnnotation(FunctionalInterface.class));
    }
}
