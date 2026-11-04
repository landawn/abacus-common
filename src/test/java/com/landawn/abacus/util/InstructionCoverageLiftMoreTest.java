package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("unit")
public class InstructionCoverageLiftMoreTest extends TestBase {

    @Test
    public void transposeCoversWideSquareTallAndRejectedMatrices() {
        assertNull(Array.transpose((boolean[][]) null));
        assertNull(Array.transpose((char[][]) null));
        assertNull(Array.transpose((byte[][]) null));
        assertNull(Array.transpose((short[][]) null));
        assertNull(Array.transpose((int[][]) null));
        assertNull(Array.transpose((long[][]) null));
        assertNull(Array.transpose((float[][]) null));
        assertNull(Array.transpose((double[][]) null));
        assertNull(Array.transpose((String[][]) null));

        final boolean[][] emptyBooleans = new boolean[0][];
        assertEquals(0, Array.transpose(emptyBooleans).length);
        assertNotSame(emptyBooleans, Array.transpose(emptyBooleans));
        assertEquals(0, Array.transpose(new char[0][]).length);
        assertEquals(0, Array.transpose(new byte[0][]).length);
        assertEquals(0, Array.transpose(new short[0][]).length);
        assertEquals(0, Array.transpose(new int[0][]).length);
        assertEquals(0, Array.transpose(new long[0][]).length);
        assertEquals(0, Array.transpose(new float[0][]).length);
        assertEquals(0, Array.transpose(new double[0][]).length);
        assertEquals(0, Array.transpose(new String[0][]).length);

        assertArrayEquals(new boolean[] { true, false, true }, Array.transpose(new boolean[][] { { true }, { false }, { true } })[0]);
        assertArrayEquals(new boolean[] { true }, Array.transpose(new boolean[][] { { true, false, true } })[0]);
        assertArrayEquals(new boolean[] { true, false }, Array.transpose(new boolean[][] { { true, false }, { false, true } })[0]);

        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Array.transpose(new char[][] { { 'a' }, { 'b' }, { 'c' } })[0]);
        assertArrayEquals(new char[] { 'a' }, Array.transpose(new char[][] { { 'a', 'b', 'c' } })[0]);
        assertArrayEquals(new byte[] { 1, 2, 3 }, Array.transpose(new byte[][] { { 1 }, { 2 }, { 3 } })[0]);
        assertArrayEquals(new byte[] { 1 }, Array.transpose(new byte[][] { { 1, 2, 3 } })[0]);
        assertArrayEquals(new short[] { 1, 2, 3 }, Array.transpose(new short[][] { { 1 }, { 2 }, { 3 } })[0]);
        assertArrayEquals(new short[] { 1 }, Array.transpose(new short[][] { { 1, 2, 3 } })[0]);
        assertArrayEquals(new int[] { 1, 2, 3 }, Array.transpose(new int[][] { { 1 }, { 2 }, { 3 } })[0]);
        assertArrayEquals(new int[] { 1 }, Array.transpose(new int[][] { { 1, 2, 3 } })[0]);
        assertArrayEquals(new int[] { 1, 3 }, Array.transpose(new int[][] { { 1, 2 }, { 3, 4 } })[0]);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, Array.transpose(new long[][] { { 1L }, { 2L }, { 3L } })[0]);
        assertArrayEquals(new long[] { 1L }, Array.transpose(new long[][] { { 1L, 2L, 3L } })[0]);
        assertArrayEquals(new float[] { 1f, 2f, 3f }, Array.transpose(new float[][] { { 1f }, { 2f }, { 3f } })[0], 0f);
        assertArrayEquals(new float[] { 1f }, Array.transpose(new float[][] { { 1f, 2f, 3f } })[0], 0f);
        assertArrayEquals(new double[] { 1d, 2d, 3d }, Array.transpose(new double[][] { { 1d }, { 2d }, { 3d } })[0], 0d);
        assertArrayEquals(new double[] { 1d }, Array.transpose(new double[][] { { 1d, 2d, 3d } })[0], 0d);
        assertArrayEquals(new String[] { "a", "b", "c" }, Array.transpose(new String[][] { { "a" }, { "b" }, { "c" } })[0]);
        assertArrayEquals(new String[] { "a" }, Array.transpose(new String[][] { { "a", "b", "c" } })[0]);

        final int[][] source = { { 4, 5 }, { 6, 7 }, { 8, 9 } };
        final int[][] transposed = Array.transpose(source);
        assertArrayEquals(new int[] { 4, 6, 8 }, transposed[0]);
        assertArrayEquals(new int[] { 4, 5 }, source[0]);

        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new boolean[][] { { true }, { true, false } }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new char[][] { { 'a' }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new byte[][] { { 1 }, { 1, 2 } }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new short[][] { { 1 }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { { 1, 2 }, { 3 } }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new long[][] { { 1L }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new float[][] { { 1f }, { 1f, 2f } }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new double[][] { { 1d }, null }));
        assertThrows(IllegalArgumentException.class, () -> Array.transpose(new String[][] { { "a" }, null }));
    }

    @Test
    public void rootsAndLogsFollowEachRoundingMode() {
        assertEquals(3, Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.DOWN));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.FLOOR));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.UP));
        assertEquals(4, Numbers.sqrt(10, RoundingMode.CEILING));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.HALF_UP));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.HALF_DOWN));
        assertEquals(3, Numbers.sqrt(10, RoundingMode.HALF_EVEN));
        assertEquals(4, Numbers.sqrt(15, RoundingMode.HALF_EVEN));
        assertEquals(0, Numbers.sqrt(0, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(10, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(10, null));

        assertEquals(3L, Numbers.sqrt(10L, RoundingMode.DOWN));
        assertEquals(4L, Numbers.sqrt(10L, RoundingMode.UP));
        assertEquals(0L, Numbers.sqrt(0L, RoundingMode.FLOOR));
        assertEquals(316227L, Numbers.sqrt(100_000_000_000L, RoundingMode.DOWN));
        assertEquals(316227L, Numbers.sqrt(100_000_000_000L, RoundingMode.FLOOR));
        assertEquals(316228L, Numbers.sqrt(100_000_000_000L, RoundingMode.UP));
        assertEquals(316228L, Numbers.sqrt(100_000_000_000L, RoundingMode.CEILING));
        assertEquals(316228L, Numbers.sqrt(100_000_000_000L, RoundingMode.HALF_UP));
        assertEquals(316228L, Numbers.sqrt(100_000_000_000L, RoundingMode.HALF_DOWN));
        assertEquals(316228L, Numbers.sqrt(100_000_000_000L, RoundingMode.HALF_EVEN));
        assertEquals(100_000L, Numbers.sqrt(10_000_000_000L, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(100_000_000_000L, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(-1L, RoundingMode.DOWN));

        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.valueOf(9), RoundingMode.UNNECESSARY));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.TEN, RoundingMode.DOWN));
        assertEquals(BigInteger.valueOf(4), Numbers.sqrt(BigInteger.TEN, RoundingMode.UP));
        assertEquals(BigInteger.valueOf(3), Numbers.sqrt(BigInteger.TEN, RoundingMode.HALF_UP));
        final BigInteger largeSquareInput = new BigInteger("123456789012345678901234567890");
        final BigInteger largeFloor = new BigInteger("351364182882014");
        assertEquals(largeFloor, Numbers.sqrt(largeSquareInput, RoundingMode.DOWN));
        assertEquals(largeFloor, Numbers.sqrt(largeSquareInput, RoundingMode.FLOOR));
        assertEquals(largeFloor.add(BigInteger.ONE), Numbers.sqrt(largeSquareInput, RoundingMode.UP));
        assertEquals(largeFloor.add(BigInteger.ONE), Numbers.sqrt(largeSquareInput, RoundingMode.CEILING));
        final BigInteger largeHalf = Numbers.sqrt(largeSquareInput, RoundingMode.HALF_EVEN);
        assertTrue(largeHalf.equals(largeFloor) || largeHalf.equals(largeFloor.add(BigInteger.ONE)));
        assertEquals(BigInteger.ZERO, Numbers.sqrt(BigInteger.ZERO, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.sqrt(BigInteger.TEN, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt(BigInteger.valueOf(-1), RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.sqrt((BigInteger) null, RoundingMode.DOWN));

        assertEquals(4, Numbers.log10(10_000, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(20, RoundingMode.DOWN));
        assertEquals(1, Numbers.log10(20, RoundingMode.FLOOR));
        assertEquals(2, Numbers.log10(20, RoundingMode.UP));
        assertEquals(2, Numbers.log10(20, RoundingMode.CEILING));
        assertEquals(1, Numbers.log10(20, RoundingMode.HALF_DOWN));
        assertEquals(1, Numbers.log10(20, RoundingMode.HALF_EVEN));
        assertEquals(2, Numbers.log10(50, RoundingMode.HALF_UP));
        assertEquals(0, Numbers.log10(1, RoundingMode.DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(999, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(-5, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(20, null));

        assertEquals(2, Numbers.log10(100L, RoundingMode.UNNECESSARY));
        assertEquals(1, Numbers.log10(20L, RoundingMode.DOWN));
        assertEquals(2, Numbers.log10(20L, RoundingMode.UP));
        assertEquals(1, Numbers.log10(20L, RoundingMode.HALF_EVEN));
        assertEquals(12, Numbers.log10(1_000_000_000_000L, RoundingMode.UNNECESSARY));
        assertEquals(11, Numbers.log10(999_999_999_999L, RoundingMode.FLOOR));
        assertEquals(12, Numbers.log10(999_999_999_999L, RoundingMode.CEILING));
        assertEquals(11, Numbers.log10(50_000_000_000L, RoundingMode.HALF_UP));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(999L, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0L, RoundingMode.DOWN));

        assertEquals(2.0, Numbers.log10(100.0), 0.0);
        assertTrue(Double.isNaN(Numbers.log10(-1.0)));
        assertEquals(Double.POSITIVE_INFINITY, Numbers.log10(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(2, Numbers.log10(100.0, RoundingMode.UNNECESSARY));
        assertEquals(2, Numbers.log10(200.0, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(200.0, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(200.0, RoundingMode.UP));
        assertEquals(2, Numbers.log10(300.0, RoundingMode.HALF_UP));
        assertEquals(3, Numbers.log10(400.0, RoundingMode.HALF_DOWN));
        assertEquals(3, Numbers.log10(400.0, RoundingMode.HALF_EVEN));
        assertEquals(0, Numbers.log10(0.2, RoundingMode.DOWN));
        assertEquals(-1, Numbers.log10(0.2, RoundingMode.UP));
        assertEquals(-1, Numbers.log10(0.2, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.2, RoundingMode.CEILING));
        assertEquals(-1, Numbers.log10(0.1, RoundingMode.FLOOR));
        assertEquals(0, Numbers.log10(0.1, RoundingMode.CEILING));
        assertEquals(0, Numbers.log10(0.1, RoundingMode.DOWN));
        assertEquals(-1, Numbers.log10(0.1, RoundingMode.UP));
        assertEquals(-1, Numbers.log10(0.1, RoundingMode.HALF_EVEN));

        final double justAboveHundred = Math.nextUp(100.0);
        assertEquals(2, Numbers.log10(justAboveHundred, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(justAboveHundred, RoundingMode.CEILING));
        assertEquals(2, Numbers.log10(justAboveHundred, RoundingMode.DOWN));
        assertEquals(3, Numbers.log10(justAboveHundred, RoundingMode.UP));
        assertEquals(2, Numbers.log10(justAboveHundred, RoundingMode.HALF_UP));
        final double justBelowThousand = Math.nextDown(1000.0);
        assertEquals(2, Numbers.log10(justBelowThousand, RoundingMode.FLOOR));
        assertEquals(3, Numbers.log10(justBelowThousand, RoundingMode.CEILING));
        assertEquals(3, Numbers.log10(justBelowThousand, RoundingMode.HALF_DOWN));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(0.1, RoundingMode.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(200.0, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(0.0, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(Double.NaN, RoundingMode.FLOOR));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(Double.POSITIVE_INFINITY, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(100.0, null));

        assertEquals(2, Numbers.log10(BigInteger.valueOf(100), RoundingMode.DOWN));
        assertEquals(100, Numbers.log10(BigInteger.TEN.pow(100), RoundingMode.UNNECESSARY));
        assertEquals(100, Numbers.log10(BigInteger.TEN.pow(100), RoundingMode.DOWN));
        assertEquals(100, Numbers.log10(BigInteger.TEN.pow(100), RoundingMode.UP));
        final BigInteger fiveTimes = BigInteger.TEN.pow(40).multiply(BigInteger.valueOf(5));
        final BigInteger twoTimes = BigInteger.TEN.pow(40).multiply(BigInteger.valueOf(2));
        assertEquals(40, Numbers.log10(fiveTimes, RoundingMode.FLOOR));
        assertEquals(41, Numbers.log10(fiveTimes, RoundingMode.CEILING));
        assertEquals(41, Numbers.log10(fiveTimes, RoundingMode.UP));
        assertEquals(41, Numbers.log10(fiveTimes, RoundingMode.HALF_UP));
        assertEquals(40, Numbers.log10(twoTimes, RoundingMode.HALF_DOWN));
        assertEquals(40, Numbers.log10(twoTimes, RoundingMode.HALF_EVEN));
        assertThrows(ArithmeticException.class, () -> Numbers.log10(fiveTimes, RoundingMode.UNNECESSARY));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10(BigInteger.ZERO, RoundingMode.DOWN));
        assertThrows(IllegalArgumentException.class, () -> Numbers.log10((BigInteger) null, RoundingMode.DOWN));
    }

    @Test
    public void medianRangesSelectOneTwoAndSortedMiddles() {
        assertMedian(Median.of(new byte[] { 9, 5 }, 1, 2), (byte) 5, null);
        assertMedian(Median.of(new byte[] { 1, 3 }, 0, 2), (byte) 1, (byte) 3);
        assertMedian(Median.of(new byte[] { 3, 1 }, 0, 2), (byte) 1, (byte) 3);
        assertMedian(Median.of(new byte[] { 9, 3, 1, 2, 8 }, 1, 4), (byte) 2, null);
        assertMedian(Median.of(new byte[] { 4, 1, 3, 2 }, 0, 4), (byte) 2, (byte) 3);
        assertMedian(Median.of(new byte[] { 5, 1, 4, 2, 3 }, 0, 5), (byte) 3, null);
        assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[] { 1 }, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));

        assertCharMedian(Median.of(new char[] { 'a', 'z' }, 1, 2), 'z', null);
        assertCharMedian(Median.of(new char[] { 'c', 'a' }, 0, 2), 'a', 'c');
        assertCharMedian(Median.of(new char[] { 'c', 'a', 'b' }, 0, 3), 'b', null);
        assertCharMedian(Median.of(new char[] { 'd', 'a', 'c', 'b' }, 0, 4), 'b', 'c');
        assertCharMedian(Median.of(new char[] { 'e', 'a', 'd', 'b', 'c' }, 0, 5), 'c', null);

        assertShortMedian(Median.of(new short[] { 9, 5 }, 1, 2), (short) 5, null);
        assertShortMedian(Median.of(new short[] { 3, 1 }, 0, 2), (short) 1, (short) 3);
        assertShortMedian(Median.of(new short[] { 3, 1, 2 }, 0, 3), (short) 2, null);
        assertShortMedian(Median.of(new short[] { 4, 1, 3, 2 }, 0, 4), (short) 2, (short) 3);
        assertShortMedian(Median.of(new short[] { 5, 1, 4, 2, 3 }, 0, 5), (short) 3, null);

        assertIntMedian(Median.of(new int[] { 9, 5 }, 1, 2), 5, null);
        assertIntMedian(Median.of(new int[] { 3, 1 }, 0, 2), 1, 3);
        assertIntMedian(Median.of(new int[] { 3, 1, 2 }, 0, 3), 2, null);
        assertIntMedian(Median.of(new int[] { 4, 1, 3, 2 }, 0, 4), 2, 3);
        assertIntMedian(Median.of(new int[] { 5, 1, 4, 2, 3 }, 0, 5), 3, null);

        assertLongMedian(Median.of(new long[] { 9L, 5L }, 1, 2), 5L, null);
        assertLongMedian(Median.of(new long[] { 3L, 1L }, 0, 2), 1L, 3L);
        assertLongMedian(Median.of(new long[] { 3L, 1L, 2L }, 0, 3), 2L, null);
        assertLongMedian(Median.of(new long[] { 4L, 1L, 3L, 2L }, 0, 4), 2L, 3L);
        assertLongMedian(Median.of(new long[] { 5L, 1L, 4L, 2L, 3L }, 0, 5), 3L, null);

        assertFloatMedian(Median.of(new float[] { 9f, 5f }, 1, 2), 5f, null);
        assertFloatMedian(Median.of(new float[] { 3f, 1f }, 0, 2), 1f, 3f);
        assertFloatMedian(Median.of(new float[] { 3f, 1f, 2f }, 0, 3), 2f, null);
        assertFloatMedian(Median.of(new float[] { 4f, 1f, 3f, 2f }, 0, 4), 2f, 3f);
        assertFloatMedian(Median.of(new float[] { 5f, 1f, 4f, 2f, 3f }, 0, 5), 3f, null);

        assertDoubleMedian(Median.of(new double[] { 9d, 5d }, 1, 2), 5d, null);
        assertDoubleMedian(Median.of(new double[] { 3d, 1d }, 0, 2), 1d, 3d);
        assertDoubleMedian(Median.of(new double[] { 3d, 1d, 2d }, 0, 3), 2d, null);
        assertDoubleMedian(Median.of(new double[] { 4d, 1d, 3d, 2d }, 0, 4), 2d, 3d);
        assertDoubleMedian(Median.of(new double[] { 5d, 1d, 4d, 2d, 3d }, 0, 5), 3d, null);
    }

    @Test
    public void parallelSlidingWindowsMapTheLastElementAndFilterByKey() {
        final List<String> six = List.of("a", "b", "c", "d", "e", "f");
        assertEquals(4L, Stream.of(six.iterator()).parallel(2).slidingMap(1, false, (a, b, c) -> a + b + c).count());
        assertEquals(2L, Stream.of(six.iterator()).parallel(2).slidingMap(2, true, (a, b, c) -> a + b + c).count());
        assertEquals(2L,
                Stream.of(List.of("a", "b", "c", "d", "e", "f", "g", "h").iterator())
                        .parallel(2)
                        .slidingMap(4, false, (a, b, c) -> String.valueOf(a) + b + c)
                        .count());
        assertEquals(0L, Stream.of(List.of("a", "b").iterator()).parallel(2).slidingMap(1, true, (a, b, c) -> a + b + c).count());

        assertEquals(List.of("A", "B", "c!"), Stream.of("a", "b", "c").parallel(2).mapLastOrElse(value -> value + "!", String::toUpperCase).sorted().toList());
        assertEquals(List.of(2, 2), Stream.of(1, 2, 2, 3).parallel(2).intersection(value -> value, List.of(2, 2)).sorted().toList());
        assertEquals(List.of(1, 2, 3), Stream.of(1, 2, 2, 3).parallel(2).difference(value -> value, List.of(2)).sorted().toList());
    }

    @Test
    public void minAllContainsAnyAndCsvPersistCoverTheSequentialTails() throws Exception {
        assertTrue(Stream.<Integer> empty().minAll(Comparator.naturalOrder()).isEmpty());
        assertEquals(List.of(1, 1), Stream.of(3, 1, 3, 2, 1).minAll(Comparator.naturalOrder()));
        final Comparator<Integer> order = Comparator.naturalOrder();
        assertEquals(List.of(1, 1), Stream.of(2, 1, 1, 3).sorted(order).minAll(order));
        final Integer[] tiesThenSmaller = new Integer[18];
        Arrays.fill(tiesThenSmaller, 2);
        tiesThenSmaller[17] = 1;
        assertEquals(List.of(1), Stream.of(tiesThenSmaller).minAll(Comparator.naturalOrder()));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).minAll(null));

        assertTrue(Stream.<Integer> empty().maxAll(order).isEmpty());
        assertEquals(List.of(3, 3), Stream.of(1, 3, 2, 3, 0).maxAll(order));
        assertEquals(List.of(3, 3), Stream.of(1, 3, 3, 2).sorted(order).maxAll(order));
        final Integer[] tiesThenLarger = new Integer[18];
        Arrays.fill(tiesThenLarger, 1);
        tiesThenLarger[17] = 4;
        assertEquals(List.of(4), Stream.of(tiesThenLarger).maxAll(order));
        final List<Integer> parallelMax = Stream.of(1, 3, 2, 3).parallel(2).maxAll(order);
        assertEquals(2, parallelMax.size());
        assertTrue(parallelMax.stream().allMatch(value -> value == 3));

        assertTrue(Stream.of(1, 2, 3).kthLargest(4, order).isEmpty());
        assertEquals(Integer.valueOf(2), Stream.of(3, 1, 2).kthLargest(2, order).get());
        assertEquals(Integer.valueOf(2), Stream.of(1, 2, 3).sorted(order).kthLargest(2, order).get());

        assertFalse(Stream.of(1, 2, 3).containsAny(List.<Integer> of()));
        assertTrue(Stream.of(1, 2, 3).containsAny(List.of(2)));
        assertFalse(Stream.of(1, 2, 3).containsAny(new HashSet<>(List.of(9))));
        assertTrue(Stream.of(1, 2, 3).containsAny(new LinkedHashSet<>(List.of(9, 2))));
        assertFalse(Stream.of(1, 2, 3).containsAny(new HashSet<>(List.of(8, 9))));
        assertTrue(Stream.of(1, 2, 3).containsAny(List.of(8, 2)));

        final Map<String, String> firstRow = new LinkedHashMap<>();
        firstRow.put("name", "ada");
        firstRow.put("city", "paris");
        final Map<String, String> secondRow = new LinkedHashMap<>();
        secondRow.put("name", "bea");
        secondRow.put("city", "lyon");
        final StringWriter mapWriter = new StringWriter();
        assertEquals(2L, Stream.of(firstRow, secondRow).persistToCsv(mapWriter));
        assertTrue(mapWriter.toString().contains("ada"));
        assertTrue(mapWriter.toString().contains("lyon"));

        final StringWriter headedMapWriter = new StringWriter();
        assertEquals(1L, Stream.<Map<String, String>> of(firstRow).persistToCsv(List.of("city"), headedMapWriter));
        assertTrue(headedMapWriter.toString().contains("paris"));

        final StringWriter beanWriter = new StringWriter();
        assertEquals(2L, Stream.of(new NamedValue("ada", 1), new NamedValue("bea", 2)).persistToCsv(beanWriter));
        assertTrue(beanWriter.toString().contains("name"));
        assertTrue(beanWriter.toString().contains("bea"));

        final StringWriter collectionWriter = new StringWriter();
        assertEquals(2L, Stream.of(List.of("ada", 1), List.of("bea", 2)).persistToCsv(List.of("name", "n"), collectionWriter));
        assertTrue(collectionWriter.toString().contains("ada"));

        final StringWriter arrayWriter = new StringWriter();
        assertEquals(2L, Stream.<Object[]> of(new Object[] { "ada", 1 }, new Object[] { "bea", 2 }).persistToCsv(List.of("name", "n"), arrayWriter));
        assertTrue(arrayWriter.toString().contains("bea"));

        final StringWriter headerOnly = new StringWriter();
        assertEquals(0L, Stream.<Map<String, String>> empty().persistToCsv(List.of("name"), headerOnly));
        assertTrue(headerOnly.toString().contains("name"));
    }

    @Test
    public void zipWithDefaultsWalksUnequalSources() {
        final List<String> byNext = new ArrayList<>();
        final BiIterator<String, Integer> pairs = BiIterator.zip(List.of("a").iterator(), List.of(1, 2).iterator(), "none", -1);
        final Pair<String, Integer> first = pairs.next();
        byNext.add(first.left() + ":" + first.right());
        pairs.next((left, right) -> byNext.add(left + ":" + right));
        assertEquals(List.of("a:1", "none:2"), byNext);
        assertThrows(NoSuchElementException.class, pairs::next);

        final List<String> consumed = new ArrayList<>();
        BiIterator.zip(List.of(1, 2).iterator(), List.of("a").iterator(), -1, "none").foreachRemaining((left, right) -> consumed.add(left + ":" + right));
        assertEquals(List.of("1:a", "2:none"), consumed);

        final List<String> remaining = new ArrayList<>();
        BiIterator.zip(List.of("a", "b").iterator(), List.<Integer> of().iterator(), "none", 0)
                .forEachRemaining((left, right) -> remaining.add(left + ":" + right));
        assertEquals(List.of("a:0", "b:0"), remaining);

        final List<String> mapped = new ArrayList<>();
        BiIterator.zip((java.util.Iterator<String>) null, (java.util.Iterator<Integer>) null, "none", 0)
                .map((left, right) -> left + ":" + right)
                .forEachRemaining(mapped::add);
        assertTrue(mapped.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.zip(List.of("a").iterator(), List.of(1).iterator(), "none", 0).foreachRemaining(null));
    }

    @Test
    public void iteratorAveragesAndBeanDifferencesUseThePublicContracts() {
        assertEquals(98.0, CharStream.of(CharIterator.of('a', 'b', 'c')).average().get(), 0.0);
        assertEquals(97.0, CharStream.of(CharIterator.of('a')).average().get(), 0.0);
        assertTrue(CharStream.of(CharIterator.of()).average().isEmpty());
        assertTrue(CharStream.of((CharIterator) null).average().isEmpty());

        assertEquals(2.0, ByteStream.of(ByteIterator.of((byte) 1, (byte) 2, (byte) 3)).average().get(), 0.0);
        assertTrue(ByteStream.of(ByteIterator.of()).average().isEmpty());
        assertTrue(ByteStream.of((ByteIterator) null).average().isEmpty());

        final NamedValue left = new NamedValue("ada", 1);
        final NamedValue right = new NamedValue("bea", 1);
        final Difference.BeanDifference<?, ?, ?> different = Difference.BeanDifference.ofByProps(left, right,
                (name, one, two) -> java.util.Objects.equals(one, two));
        assertFalse(different.areEqual());
        assertTrue(Difference.BeanDifference.ofByProps(left, right, (name, one, two) -> true).areEqual());
        assertTrue(Difference.BeanDifference.ofByProps(null, null, (name, one, two) -> true).areEqual());
        assertFalse(Difference.BeanDifference.ofByProps(left, null, (name, one, two) -> true).areEqual());
        assertFalse(Difference.BeanDifference.ofByProps(null, right, List.of("name"), (name, one, two) -> true).areEqual());
        assertThrows(IllegalArgumentException.class, () -> Difference.BeanDifference.ofByProps("a", "b", (name, one, two) -> true));
        assertThrows(IllegalArgumentException.class, () -> Difference.BeanDifference.ofByProps(left, right, null));
    }

    @Test
    public void flatMapColumnsSkipsEmptyMappingsAndCopiesRequestedColumns() {
        final Dataset source = N.newDataset(List.of("a", "b", "c", "keep"), new Object[][] { { 1, 2, 3, "x" }, { 4, 5, 6, "y" }, { 7, 8, 9, "z" } });
        final Dataset withoutCopies = source.flatMapColumns(Tuple.of("a", "b", "c"), "out", List.<String> of(), (a, b, c) -> {
            final int key = (Integer) a;
            if (key == 4) {
                return List.of();
            }
            if (key == 7) {
                return null;
            }
            return List.of(a, b);
        });
        assertEquals(2, withoutCopies.size());
        assertTrue(withoutCopies.containsColumn("out"));
        assertFalse(withoutCopies.containsColumn("keep"));

        final Dataset withCopies = source.flatMapColumns(Tuple.of("a", "b", "c"), "out", List.of("keep"), (a, b, c) -> {
            final int key = (Integer) a;
            return key == 1 ? List.of(a) : null;
        });
        assertEquals(1, withCopies.size());
        assertTrue(withCopies.containsColumn("keep"));
        final List<Object> copied = new ArrayList<>();
        withCopies.forEach(Tuple.of("keep", "out"), (keep, out) -> {
            copied.add(keep);
            copied.add(out);
        });
        assertEquals(List.of("x", 1), copied);
    }

    @Test
    public void excelSheetReceivesEachSupportedCellKind() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 5, 6, 7, 8);
        final Dataset dataset = N.newDataset(
                List.of("text", "flag", "sqlDate", "utilDate", "localDate", "localDateTime", "calendar", "number", "empty", "other"),
                new Object[][] { { "ada", Boolean.TRUE, java.sql.Date.valueOf("2020-01-02"), new Date(1_577_836_800_000L), LocalDate.of(2020, 1, 3),
                        LocalDateTime.of(2020, 1, 4, 5, 6), calendar, 7, null, Map.of("k", "v") } });
        final Path output = Files.createTempFile("coverage-cells", ".xlsx");
        try {
            ExcelUtil.writeDatasetToSheet("Coverage", dataset, sheet -> assertNotNull(sheet), output);
            assertTrue(Files.size(output) > 0L);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void assertMedian(final Pair<Byte, OptionalByte> median, final byte left, final Byte right) {
        assertEquals(Byte.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.byteValue(), median.right().get());
        }
    }

    private static void assertCharMedian(final Pair<Character, OptionalChar> median, final char left, final Character right) {
        assertEquals(Character.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.charValue(), median.right().get());
        }
    }

    private static void assertShortMedian(final Pair<Short, OptionalShort> median, final short left, final Short right) {
        assertEquals(Short.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.shortValue(), median.right().get());
        }
    }

    private static void assertIntMedian(final Pair<Integer, OptionalInt> median, final int left, final Integer right) {
        assertEquals(Integer.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.intValue(), median.right().get());
        }
    }

    private static void assertLongMedian(final Pair<Long, OptionalLong> median, final long left, final Long right) {
        assertEquals(Long.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.longValue(), median.right().get());
        }
    }

    private static void assertFloatMedian(final Pair<Float, OptionalFloat> median, final float left, final Float right) {
        assertEquals(Float.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.floatValue(), median.right().get(), 0f);
        }
    }

    private static void assertDoubleMedian(final Pair<Double, OptionalDouble> median, final double left, final Double right) {
        assertEquals(Double.valueOf(left), median.left());
        if (right == null) {
            assertTrue(median.right().isEmpty());
        } else {
            assertEquals(right.doubleValue(), median.right().get(), 0d);
        }
    }

    public static class NamedValue {
        private final String name;
        private final int value;

        public NamedValue(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
