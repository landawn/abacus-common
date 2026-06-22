package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjDoublePredicateTest extends TestBase {

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
    public void testAndOrAcceptSuperTypePredicate() {
        ObjDoublePredicate<String> notEmpty = (s, value) -> s != null && !s.isEmpty();
        ObjDoublePredicate<CharSequence> longerThan = (s, value) -> s != null && s.length() > value;

        ObjDoublePredicate<String> and = notEmpty.and(longerThan);
        ObjDoublePredicate<String> or = notEmpty.or(longerThan);

        assertTrue(and.test("hello", 3.0));
        assertFalse(and.test("hi", 5.0));
        assertTrue(or.test("hi", 5.0));
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

    @Test
    public void testAndNullThrowsImmediately() {
        ObjDoublePredicate<String> instance = (a, b) -> false;
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> instance.and((ObjDoublePredicate) null));
    }
}
