package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

@Tag("unit")
class StreamAppendIfEmptyCleanupReview20261009Test extends TestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("appendPaths")
    void bufferedShortCircuitClosesLateSuppliedStream(final AppendPath path) throws InterruptedException {
        final CountDownLatch supplierStarted = new CountDownLatch(1);
        final CountDownLatch releaseSupplier = new CountDownLatch(1);
        final CountDownLatch suppliedObserved = new CountDownLatch(1);
        final AtomicInteger suppliedCloses = new AtomicInteger();
        final Stream<?> appended = append(path, () -> {
            supplierStarted.countDown();
            await(releaseSupplier);
            return child(path.type(), suppliedObserved::countDown, () -> {
                suppliedCloses.incrementAndGet();
                suppliedObserved.countDown();
            });
        });
        final Stream<?> source = Stream.concat(Stream.of(1), appended);

        try {
            assertEquals(1, source.buffered(1).onEach(ignored -> await(supplierStarted)).first().orElseThrow());
        } finally {
            releaseSupplier.countDown();
            source.close();
        }

        assertTrue(suppliedObserved.await(5, TimeUnit.SECONDS), "the buffered reader did not finish its supplier");
        assertEquals(1, suppliedCloses.get(), "a supplied stream returned after pipeline closure must be closed");
        source.close();
        assertEquals(1, suppliedCloses.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("appendPaths")
    void normalTraversalClosesTheSuppliedStreamExactlyOnce(final AppendPath path) {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final AtomicInteger suppliedCloses = new AtomicInteger();
        final Stream<?> appended = append(path, () -> {
            supplierCalls.incrementAndGet();
            return child(path.type(), () -> {
            }, suppliedCloses::incrementAndGet);
        });

        assertEquals(List.of(2), appended.map(value -> value instanceof Character character ? (int) character : ((Number) value).intValue()).toList());
        assertEquals(1, supplierCalls.get());
        assertEquals(1, suppliedCloses.get());
        appended.close();
        assertEquals(1, suppliedCloses.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("appendPaths")
    void closingBeforeTraversalDoesNotInvokeTheSupplier(final AppendPath path) {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final Stream<?> appended = append(path, () -> {
            supplierCalls.incrementAndGet();
            return child(path.type(), () -> {
            }, () -> {
            });
        });

        appended.close();
        assertEquals(0, supplierCalls.get());
    }

    private enum Type {
        OBJECT, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE
    }

    private record AppendPath(Type type, boolean iteratorSource) {
        @Override
        public String toString() {
            return type + (iteratorSource ? " iterator" : " array");
        }
    }

    private static List<AppendPath> appendPaths() {
        final List<AppendPath> paths = new ArrayList<>();
        for (final Type type : Type.values()) {
            paths.add(new AppendPath(type, false));
            paths.add(new AppendPath(type, true));
        }
        return paths;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Stream<?> append(final AppendPath path, final Supplier<BaseStream> supplier) {
        final boolean iterator = path.iteratorSource();
        final BaseStream empty = switch (path.type()) {
            case OBJECT -> iterator ? Stream.of(List.of().iterator()) : Stream.empty();
            case CHAR -> iterator ? CharStream.of(CharIterator.empty()) : CharStream.empty();
            case BYTE -> iterator ? ByteStream.of(ByteIterator.empty()) : ByteStream.empty();
            case SHORT -> iterator ? ShortStream.of(ShortIterator.empty()) : ShortStream.empty();
            case INT -> iterator ? IntStream.of(IntIterator.empty()) : IntStream.empty();
            case LONG -> iterator ? LongStream.of(LongIterator.empty()) : LongStream.empty();
            case FLOAT -> iterator ? FloatStream.of(FloatIterator.empty()) : FloatStream.empty();
            case DOUBLE -> iterator ? DoubleStream.of(DoubleIterator.empty()) : DoubleStream.empty();
        };
        final BaseStream appended = empty.appendIfEmpty(supplier);
        return switch (path.type()) {
            case OBJECT -> (Stream<?>) appended;
            case CHAR -> ((CharStream) appended).boxed();
            case BYTE -> ((ByteStream) appended).boxed();
            case SHORT -> ((ShortStream) appended).boxed();
            case INT -> ((IntStream) appended).boxed();
            case LONG -> ((LongStream) appended).boxed();
            case FLOAT -> ((FloatStream) appended).boxed();
            case DOUBLE -> ((DoubleStream) appended).boxed();
        };
    }

    @SuppressWarnings("rawtypes")
    private static BaseStream child(final Type type, final Runnable observed, final Runnable close) {
        return switch (type) {
            case OBJECT -> Stream.of(2).onEach(ignored -> observed.run()).onClose(close);
            case CHAR -> CharStream.of((char) 2).onEach(ignored -> observed.run()).onClose(close);
            case BYTE -> ByteStream.of((byte) 2).onEach(ignored -> observed.run()).onClose(close);
            case SHORT -> ShortStream.of((short) 2).onEach(ignored -> observed.run()).onClose(close);
            case INT -> IntStream.of(2).onEach(ignored -> observed.run()).onClose(close);
            case LONG -> LongStream.of(2L).onEach(ignored -> observed.run()).onClose(close);
            case FLOAT -> FloatStream.of(2f).onEach(ignored -> observed.run()).onClose(close);
            case DOUBLE -> DoubleStream.of(2d).onEach(ignored -> observed.run()).onClose(close);
        };
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "timed out coordinating the buffered reader");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
