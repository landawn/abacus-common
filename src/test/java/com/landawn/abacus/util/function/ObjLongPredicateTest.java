package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjLongPredicateTest extends TestBase {

    @Test
    public void testAndOrAcceptSuperTypePredicate() {
        ObjLongPredicate<String> notEmpty = (s, value) -> s != null && !s.isEmpty();
        ObjLongPredicate<CharSequence> longerThan = (s, value) -> s != null && s.length() > value;

        ObjLongPredicate<String> and = notEmpty.and(longerThan);
        ObjLongPredicate<String> or = notEmpty.or(longerThan);

        assertTrue(and.test("hello", 3L));
        assertFalse(and.test("hi", 5L));
        assertTrue(or.test("hi", 5L));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjLongPredicate.class.getAnnotation(FunctionalInterface.class));
    }

    @Test
    public void testAndOrAllCombinations() {
        final ObjLongPredicate<String> lenEq = (s, n) -> s.length() == n;
        final ObjLongPredicate<String> positive = (s, n) -> n > 0;

        final ObjLongPredicate<String> and = lenEq.and(positive);
        assertTrue(and.test("ab", 2));
        assertFalse(and.test("", 0));
        assertFalse(and.test("ab", 3));

        final ObjLongPredicate<String> or = lenEq.or(positive);
        assertTrue(or.test("ab", 2));
        assertTrue(or.test("ab", 5));
        assertTrue(or.test("", 0));
        assertFalse(or.test("ab", -1));
    }
}
