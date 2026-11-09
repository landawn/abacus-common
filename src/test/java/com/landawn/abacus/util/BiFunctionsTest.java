package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BiFunctionsTest extends TestBase {

    @Test
    public void testOfMerge() {
        final BiFunction<Joiner, Joiner, Joiner> merge = BiFunctions.ofMerge();
        final Joiner a = Joiner.with(",").append("x");
        final Joiner b = Joiner.with(",").append("y");
        assertSame(a, merge.apply(a, b));
        assertEquals("x,y", a.toString());
    }

    @Test
    public void testOfAppend() {
        final BiFunction<StringBuilder, Integer, StringBuilder> append = BiFunctions.ofAppend();
        final StringBuilder sb = new StringBuilder("n=");
        assertSame(sb, append.apply(sb, 5));
        assertEquals("n=5", sb.toString());
    }
}
