package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests verifying stream-related bug fixes found during deep code review (2026).
 *
 * <p>Bugs fixed:
 * <ul>
 *   <li>LongStream.advance() - long overflow in n * by without detection</li>
 *   <li>ByteStream/ShortStream/CharStream advance() - next updated even after stream exhaustion</li>
 *   <li>StreamBase.isClosed - not volatile, visibility issue in parallel streams</li>
 * </ul>
 */
@Tag("2025")
public class BugFixStreamDeepReviewTest extends TestBase {

    // ============================================================
    // Bug 2: LongStream.advance() - uses Math.multiplyExact for overflow safety
    // Note: overflow is unreachable through public API since range() splits
    // large ranges, but the fix is defensive. Tests verify normal behavior.
    // ============================================================

    @Test
    @DisplayName("LongStream.range advance should work correctly with step > 1")
    public void testLongStream_advanceWithStep() {
        long[] result = LongStream.range(0, 10, 2).skip(2).toArray();
        // range(0, 10, 2) = [0, 2, 4, 6, 8], skip(2) = [4, 6, 8]
        assertArrayEquals(new long[] { 4, 6, 8 }, result);
    }

    @Test
    @DisplayName("LongStream.rangeClosed advance should work correctly with step > 1")
    public void testLongStream_rangeClosedAdvanceWithStep() {
        long[] result = LongStream.rangeClosed(0, 10, 2).skip(2).toArray();
        // rangeClosed(0, 10, 2) = [0, 2, 4, 6, 8, 10], skip(2) = [4, 6, 8, 10]
        assertArrayEquals(new long[] { 4, 6, 8, 10 }, result);
    }

    @Test
    @DisplayName("LongStream advance past end should return empty")
    public void testLongStream_advancePastEnd() {
        long count = LongStream.range(0, 5).skip(10).count();
        assertEquals(0, count);
    }

    @Test
    @DisplayName("LongStream.range skip all but last element")
    public void testLongStream_skipAllButLast() {
        long[] result = LongStream.range(0, 100, 10).skip(9).toArray();
        // range(0, 100, 10) = [0, 10, 20, ..., 90], skip(9) = [90]
        assertArrayEquals(new long[] { 90 }, result);
    }

    @Test
    @DisplayName("LongStream.range with large step and skip")
    public void testLongStream_largeStepAndSkip() {
        long[] result = LongStream.range(0, 1_000_000, 100_000).skip(3).toArray();
        // [0, 100000, 200000, ..., 900000], skip(3) = [300000, 400000, ..., 900000]
        assertArrayEquals(new long[] { 300_000, 400_000, 500_000, 600_000, 700_000, 800_000, 900_000 }, result);
    }

    // ============================================================
    // Bugs 3-5: ByteStream/ShortStream/CharStream advance past end
    // ============================================================

    @Test
    @DisplayName("ByteStream advance past end should return empty and not corrupt state")
    public void testByteStream_advancePastEnd() {
        // After fix: when n >= cnt, cnt=0 and early return without modifying next
        long count = ByteStream.range((byte) 0, (byte) 5).skip(10).count();
        assertEquals(0, count);
    }

    @Test
    @DisplayName("ByteStream advance should work normally")
    public void testByteStream_advanceNormalCase() {
        byte[] result = ByteStream.range((byte) 0, (byte) 10, (byte) 2).skip(2).toArray();
        assertArrayEquals(new byte[] { 4, 6, 8 }, result);
    }

    @Test
    @DisplayName("ShortStream advance past end should return empty and not corrupt state")
    public void testShortStream_advancePastEnd() {
        long count = ShortStream.range((short) 0, (short) 5).skip(10).count();
        assertEquals(0, count);
    }

    @Test
    @DisplayName("ShortStream advance should work normally")
    public void testShortStream_advanceNormalCase() {
        short[] result = ShortStream.range((short) 0, (short) 10, (short) 2).skip(2).toArray();
        assertArrayEquals(new short[] { 4, 6, 8 }, result);
    }

    @Test
    @DisplayName("CharStream advance past end should return empty and not corrupt state")
    public void testCharStream_advancePastEnd() {
        long count = CharStream.range('a', 'e').skip(10).count();
        assertEquals(0, count);
    }

    @Test
    @DisplayName("CharStream advance should work normally")
    public void testCharStream_advanceNormalCase() {
        char[] result = CharStream.range('a', 'g', 2).skip(1).toArray();
        // range('a', 'g', 2) = ['a', 'c', 'e'], skip(1) = ['c', 'e']
        assertArrayEquals(new char[] { 'c', 'e' }, result);
    }

    // ============================================================
    // Bug 6: StreamBase.isClosed volatile visibility
    // ============================================================

    @Test
    @DisplayName("Stream should throw after being consumed (isClosed visibility)")
    public void testStream_isClosedAfterTerminalOp() {
        Stream<Integer> stream = Stream.of(1, 2, 3);
        stream.count(); // terminal op closes the stream

        // After fix, isClosed is volatile so assertNotClosed() should always see the updated value
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    @DisplayName("LongStream should throw after being consumed")
    public void testLongStream_isClosedAfterTerminalOp() {
        LongStream stream = LongStream.of(1, 2, 3);
        stream.count();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    @DisplayName("IntStream should throw after being consumed")
    public void testIntStream_isClosedAfterTerminalOp() {
        IntStream stream = IntStream.of(1, 2, 3);
        stream.count();
        assertThrows(IllegalStateException.class, () -> stream.count());
    }
}
