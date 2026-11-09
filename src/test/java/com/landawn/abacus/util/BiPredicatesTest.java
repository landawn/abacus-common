package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BiPredicatesTest extends TestBase {

    @Test
    public void testBiPredicatesAlwaysFalse() {
        final java.util.function.BiPredicate<String, String> p = BiPredicates.alwaysFalse();
        assertFalse(p.test("a", "a"));
        assertFalse(p.test(null, null));
    }
}
