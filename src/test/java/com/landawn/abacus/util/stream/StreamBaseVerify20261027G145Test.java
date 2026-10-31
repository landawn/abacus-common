package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Verification tests for the memoizing supplier of the nine {@code defer} factories.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class StreamBaseVerify20261027G145Test extends TestBase {

    // ---- deep review 2026-10-27 verify G145 begin ----

    private static final List<String> VARIANTS = Arrays.asList("Stream", "EntryStream", "ByteStream", "CharStream", "ShortStream", "IntStream", "LongStream",
            "FloatStream", "DoubleStream");

    private static BaseStream deferOf(final String variant, final Supplier<BaseStream> body) {
        switch (variant) {
            case "Stream":
                return Stream.defer(() -> (Stream) body.get());
            case "EntryStream":
                return EntryStream.defer(() -> (EntryStream) body.get());
            case "ByteStream":
                return ByteStream.defer(() -> (ByteStream) body.get());
            case "CharStream":
                return CharStream.defer(() -> (CharStream) body.get());
            case "ShortStream":
                return ShortStream.defer(() -> (ShortStream) body.get());
            case "IntStream":
                return IntStream.defer(() -> (IntStream) body.get());
            case "LongStream":
                return LongStream.defer(() -> (LongStream) body.get());
            case "FloatStream":
                return FloatStream.defer(() -> (FloatStream) body.get());
            case "DoubleStream":
                return DoubleStream.defer(() -> (DoubleStream) body.get());
            default:
                throw new IllegalArgumentException(variant);
        }
    }

    private static BaseStream sample(final String variant, final Runnable onClose) {
        switch (variant) {
            case "Stream":
                return Stream.of(1, 2, 3).onClose(onClose);
            case "EntryStream":
                return EntryStream.of("a", 1, "b", 2, "c", 3).onClose(onClose);
            case "ByteStream":
                return ByteStream.of((byte) 1, (byte) 2, (byte) 3).onClose(onClose);
            case "CharStream":
                return CharStream.of('a', 'b', 'c').onClose(onClose);
            case "ShortStream":
                return ShortStream.of((short) 1, (short) 2, (short) 3).onClose(onClose);
            case "IntStream":
                return IntStream.of(1, 2, 3).onClose(onClose);
            case "LongStream":
                return LongStream.of(1L, 2L, 3L).onClose(onClose);
            case "FloatStream":
                return FloatStream.of(1f, 2f, 3f).onClose(onClose);
            default:
                return DoubleStream.of(1d, 2d, 3d).onClose(onClose);
        }
    }

    // a stage derived from s (filter), of the same family
    private static BaseStream derived(final String variant, final BaseStream s) {
        switch (variant) {
            case "Stream":
                return ((Stream) s).filter(x -> true);
            case "EntryStream":
                return ((EntryStream) s).filter(e -> true);
            case "ByteStream":
                return ((ByteStream) s).filter(x -> true);
            case "CharStream":
                return ((CharStream) s).filter(x -> true);
            case "ShortStream":
                return ((ShortStream) s).filter(x -> true);
            case "IntStream":
                return ((IntStream) s).filter(x -> true);
            case "LongStream":
                return ((LongStream) s).filter(x -> true);
            case "FloatStream":
                return ((FloatStream) s).filter(x -> true);
            default:
                return ((DoubleStream) s).filter(x -> true);
        }
    }

    // A terminal operation on a derived stage of a deferred stream whose supplier fails: the supplier runs once (r108: the
    // derived stage's close ran it again), and closing the deferred stream itself afterwards neither runs it nor throws.
    @Test
    public void testDefer_derivedStageOfFailingSupplierCallsItOnceForEveryVariant() {
        for (final String variant : VARIANTS) {
            final AtomicInteger calls = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("supplier failure");
            final BaseStream deferred = deferOf(variant, () -> {
                calls.incrementAndGet();
                throw failure;
            });

            final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> derived(variant, deferred).count(), variant);
            assertSame(failure, thrown, variant);
            assertEquals(1, calls.get(), variant);
            assertDoesNotThrow(() -> deferred.close(), variant);
            assertEquals(1, calls.get(), variant);
        }
    }

    // Nested defer: the inner supplier fails; the outer and the inner supplier each run once (r108: the inner one twice).
    @Test
    public void testDefer_nestedFailingSupplierIsCalledOnceForEveryVariant() {
        for (final String variant : VARIANTS) {
            final AtomicInteger outer = new AtomicInteger();
            final AtomicInteger inner = new AtomicInteger();
            final IllegalArgumentException failure = new IllegalArgumentException("inner failure");

            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> deferOf(variant, () -> {
                outer.incrementAndGet();
                return deferOf(variant, () -> {
                    inner.incrementAndGet();
                    throw failure;
                });
            }).count(), variant);

            assertSame(failure, thrown, variant);
            assertEquals(0, thrown.getSuppressed().length, variant);
            assertEquals(1, outer.get(), variant);
            assertEquals(1, inner.get(), variant);

            // Unchanged: a nested successful defer supplies, and closes, its stream once.
            final AtomicInteger outerOk = new AtomicInteger();
            final AtomicInteger innerOk = new AtomicInteger();
            final AtomicInteger closes = new AtomicInteger();
            assertEquals(3, deferOf(variant, () -> {
                outerOk.incrementAndGet();
                return deferOf(variant, () -> {
                    innerOk.incrementAndGet();
                    return sample(variant, closes::incrementAndGet);
                });
            }).count(), variant);
            assertEquals(1, outerOk.get(), variant);
            assertEquals(1, innerOk.get(), variant);
            assertEquals(1, closes.get(), variant);
        }
    }

    // An Error thrown by the supplier is memoized like an exception: one call, the same Error, nothing suppressed (r108:
    // two calls, the second Error suppressed on the first).
    @Test
    public void testDefer_errorFromSupplierIsFinalForEveryVariant() {
        for (final String variant : VARIANTS) {
            final AtomicInteger calls = new AtomicInteger();
            final AssertionError thrown = assertThrows(AssertionError.class, () -> deferOf(variant, () -> {
                calls.incrementAndGet();
                throw new AssertionError("supplier error " + calls.get());
            }).count(), variant);

            assertEquals("supplier error 1", thrown.getMessage(), variant);
            assertEquals(0, thrown.getSuppressed().length, variant);
            assertEquals(1, calls.get(), variant);
        }
    }

    // Unchanged: a null supplier result is an empty stream for every variant, also through a derived stage and when the
    // stream is closed before traversal.
    @Test
    public void testDefer_nullResultForEveryVariant() {
        for (final String variant : VARIANTS) {
            assertEquals(0, deferOf(variant, () -> null).count(), variant);
            assertEquals(0, derived(variant, deferOf(variant, () -> null)).count(), variant);

            final AtomicInteger calls = new AtomicInteger();
            final BaseStream untraversed = deferOf(variant, () -> {
                calls.incrementAndGet();
                return null;
            });
            assertDoesNotThrow(() -> untraversed.close(), variant);
            assertEquals(1, calls.get(), variant);
        }
    }

    // Thread-safety: the close of a deferred stream can come from another thread than the traversal. The supplier runs once
    // and the supplied stream is closed once - after a partial traversal on this thread, for a parallel traversal on worker
    // threads, and when close() races the first traversal.
    @Test
    public void testDefer_closeFromAnotherThreadClosesTheSuppliedStreamOnce() throws Exception {
        for (final String variant : VARIANTS) {
            final AtomicInteger calls = new AtomicInteger();
            final AtomicInteger closes = new AtomicInteger();
            final BaseStream deferred = deferOf(variant, () -> {
                calls.incrementAndGet();
                return sample(variant, closes::incrementAndGet);
            });
            final Iterator<?> iter = deferred.iterator();
            assertTrue(iter.hasNext(), variant);

            final Thread closer = new Thread(deferred::close);
            closer.start();
            closer.join();

            assertEquals(1, calls.get(), variant);
            assertEquals(1, closes.get(), variant);
        }

        for (int run = 0; run < 200; run++) {
            final AtomicInteger calls = new AtomicInteger();
            final AtomicInteger closes = new AtomicInteger();
            final List<Integer> result = Stream.defer(() -> {
                calls.incrementAndGet();
                return Stream.of(1, 2, 3, 4, 5, 6, 7, 8).onClose(closes::incrementAndGet);
            }).parallel(4).map(x -> x * 2).toList();

            assertEquals(8, result.size());
            assertEquals(1, calls.get());
            assertEquals(1, closes.get());
        }

        for (int run = 0; run < 200; run++) {
            final AtomicInteger calls = new AtomicInteger();
            final AtomicInteger closes = new AtomicInteger();
            final Stream<Integer> deferred = Stream.defer(() -> {
                calls.incrementAndGet();
                Thread.yield();
                return Stream.of(1, 2, 3).onClose(closes::incrementAndGet);
            });

            final Thread traverser = new Thread(() -> {
                try {
                    deferred.iterator().hasNext();
                } catch (final IllegalStateException e) {
                    // the close won the race: the stream is already closed
                }
            });
            traverser.start();
            deferred.close();
            traverser.join();

            assertEquals(1, calls.get());
            assertEquals(1, closes.get());
        }
    }

    // ---- deep review 2026-10-27 verify G145 end ----
}
