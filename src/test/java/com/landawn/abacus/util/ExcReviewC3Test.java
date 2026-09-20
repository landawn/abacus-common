package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Exception-review group C3 (CommonUtil.java 10328-15532): behaviour-visible changes.
 */
public class ExcReviewC3Test extends TestBase {

    /** A source class that is not built in, so only the null checks are exercised. */
    private static final class C3Source {
    }

    @Test
    public void registerConverter_nullSrcClass_isReportedAsSrcClass() {
        // Validation is now in parameter order: srcClass (p1) before converter (p2).
        final BiFunction<Object, Class<?>, Object> converter = (v, t) -> v;
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(null, converter));
        assertTrue(e.getMessage().contains("srcClass"), e.getMessage());
    }

    @Test
    public void registerConverter_bothNull_isReportedAsSrcClass() {
        // Baseline checked 'converter' first, so (null, null) named the second parameter.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(null, null));
        assertTrue(e.getMessage().contains("srcClass"), e.getMessage());
        assertEquals(false, e.getMessage().contains("converter"), e.getMessage());
    }

    @Test
    public void registerConverter_nullConverter_isStillReportedAsConverter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(C3Source.class, null));
        assertTrue(e.getMessage().contains("converter"), e.getMessage());
    }
}
