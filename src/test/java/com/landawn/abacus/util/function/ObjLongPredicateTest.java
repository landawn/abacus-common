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
}
