package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MergeResult;

@Tag("unit")
class DeepReview20261004PrimitiveMergeTest extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void byteMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final ByteStream buffered = ByteStream.of(new byte[] { 1, 2 }).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final ByteStream other = ByteStream.of(new byte[] { 0 });

        assertArrayEquals(new byte[] { 0, 1 }, ByteStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void charMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final CharStream buffered = CharStream.of(new char[] { 1, 2 }).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final CharStream other = CharStream.of(new char[] { 0 });

        assertArrayEquals(new char[] { 0, 1 }, CharStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void shortMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final ShortStream buffered = ShortStream.of(new short[] { 1, 2 }).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final ShortStream other = ShortStream.of(new short[] { 0 });

        assertArrayEquals(new short[] { 0, 1 }, ShortStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void intMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final IntStream buffered = IntStream.of(1, 2).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final IntStream other = IntStream.of(0);

        assertArrayEquals(new int[] { 0, 1 }, IntStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void longMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final LongStream buffered = LongStream.of(1L, 2L).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final LongStream other = LongStream.of(0L);

        assertArrayEquals(new long[] { 0, 1 }, LongStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void floatMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final FloatStream buffered = FloatStream.of(1f, 2f).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final FloatStream other = FloatStream.of(0f);

        assertArrayEquals(new float[] { 0, 1 }, FloatStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void doubleMergeEmitsBufferedHeadWithoutReadingTail(final boolean bufferedFirst) {
        final DoubleStream buffered = DoubleStream.of(1d, 2d).filter(DeepReview20261004PrimitiveMergeTest::rejectTail);
        final DoubleStream other = DoubleStream.of(0d);

        assertArrayEquals(new double[] { 0, 1 }, DoubleStream
                .merge(bufferedFirst ? buffered : other, bufferedFirst ? other : buffered, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .limit(2)
                .toArray());
    }

    private static boolean rejectTail(final double value) {
        if (value == 2) {
            throw new AssertionError("The buffered head must be emitted before probing the unneeded source tail");
        }

        return true;
    }
}
