package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Review fixes 2026-09-24 for {@link Fnn}: N1-06 (Seq examples), C-060 (Throwable-bounded constants, doc-only lock),
 * C-059 (r2jr wraps Error, doc-only lock).
 */
public class FnnReview20260924Test extends TestBase {

    // N1-06: the corrected javadoc examples compile against Seq and produce the documented results.
    @Test
    public void n106_seqExamplesFromJavadoc() throws Exception {
        assertEquals(Arrays.asList("a", "b"), Seq.<String, IOException> of("a", "b").map(Fnn.identity()).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<String, IOException> of("a", "b").filter(Fnn.alwaysTrue()).toList());
        assertEquals(Collections.emptyList(), Seq.<String, IOException> of("a", "b").filter(Fnn.alwaysFalse()).toList());
        assertEquals(Collections.singletonList(null), Seq.<String, IOException> of("a", null, "b").filter(Fnn.isNull()).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<String, IOException> of("a", "", "b").filter(Fnn.<String, IOException> isEmpty().negate()).toList());
        assertEquals(Collections.emptyList(), Seq.<String, IOException> of().filter(Fnn.alwaysTrue()).toList());
        assertEquals(Arrays.asList("中"), Seq.<String, IOException> of("中", "").filter(Fnn.<String, IOException> isEmpty().negate()).toList());
    }

    // C-060: the eight Throwable-bounded constants listed in the class javadoc are usable with E = Throwable.
    @Test
    public void c060_throwableBoundedConstantsCompileWithThrowable() throws Throwable {
        final Throwables.BinaryOperator<String, Throwable> first = Fnn.selectFirst();
        final Throwables.BinaryOperator<String, Throwable> second = Fnn.selectSecond();
        final Throwables.BinaryOperator<Integer, Throwable> min = Fnn.min();
        final Throwables.BinaryOperator<Integer, Throwable> max = Fnn.max();
        final Throwables.BinaryOperator<Map.Entry<Integer, String>, Throwable> minByKey = Fnn.minByKey();
        final Throwables.BinaryOperator<Map.Entry<String, Integer>, Throwable> minByValue = Fnn.minByValue();
        final Throwables.BinaryOperator<Map.Entry<Integer, String>, Throwable> maxByKey = Fnn.maxByKey();
        final Throwables.BinaryOperator<Map.Entry<String, Integer>, Throwable> maxByValue = Fnn.maxByValue();

        assertEquals("a", first.apply("a", "b"));
        assertEquals("b", second.apply("a", "b"));
        assertEquals(1, min.apply(1, 2));
        assertEquals(2, max.apply(1, 2));
        assertEquals(1, minByKey.apply(new AbstractMap.SimpleEntry<>(1, "x"), new AbstractMap.SimpleEntry<>(2, "y")).getKey());
        assertEquals(1, minByValue.apply(new AbstractMap.SimpleEntry<>("x", 1), new AbstractMap.SimpleEntry<>("y", 2)).getValue());
        assertEquals(2, maxByKey.apply(new AbstractMap.SimpleEntry<>(1, "x"), new AbstractMap.SimpleEntry<>(2, "y")).getKey());
        assertEquals(2, maxByValue.apply(new AbstractMap.SimpleEntry<>("x", 1), new AbstractMap.SimpleEntry<>("y", 2)).getValue());

        // ignoringMerger/replacingMerger behave like selectFirst/selectSecond (with an Exception bound)
        assertEquals("a", Fnn.<String, Exception> ignoringMerger().apply("a", "b"));
        assertEquals("b", Fnn.<String, Exception> replacingMerger().apply("a", "b"));
    }

    // C-059: r2jr wraps an Error in a RuntimeException, like Throwables.Runnable.unchecked(); Fn.rr lets it through.
    @Test
    public void c059_r2jrWrapsErrorsLikeUnchecked() {
        final AssertionError ae = new AssertionError("boom");
        final Throwables.Runnable<RuntimeException> throwsError = () -> {
            throw ae;
        };

        final RuntimeException viaR2jr = assertThrows(RuntimeException.class, () -> Fnn.r2jr(throwsError).run());
        assertSame(ae, viaR2jr.getCause());
        final RuntimeException viaUnchecked = assertThrows(RuntimeException.class, () -> throwsError.unchecked().run());
        assertSame(ae, viaUnchecked.getCause());
        assertEquals(viaUnchecked.getClass(), viaR2jr.getClass());

        assertSame(ae, assertThrows(AssertionError.class, () -> Fn.rr(() -> {
            throw ae;
        }).run()));

        final IllegalStateException ise = new IllegalStateException("plain");
        assertSame(ise, assertThrows(IllegalStateException.class, () -> Fnn.r2jr(() -> {
            throw ise;
        }).run()));

        final RuntimeException io = assertThrows(RuntimeException.class, () -> Fnn.r2jr(() -> {
            throw new IOException("io");
        }).run());
        assertInstanceOf(UncheckedIOException.class, io);
        assertTrue(io.getCause() instanceof IOException);
    }
}
