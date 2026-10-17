package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Tuple;

/** Bulk operations must leave the same suffix as individually consuming elements. */
public class BulkObjectIteratorContractTest extends TestBase {
    private static Stream<Integer> source(boolean iterator) {
        Integer[] values = { 1, 2, 3, 4, 5 };
        return iterator ? Stream.of(Arrays.asList(values).iterator()) : Stream.of(values);
    }

    private static Arguments pipeline(String name, Supplier<? extends Stream<?>> factory, Object... expected) {
        return Arguments.of(name, factory, Arrays.asList(expected));
    }

    static java.util.stream.Stream<Arguments> pipelines() {
        List<Arguments> cases = new ArrayList<>();
        cases.add(pipeline("jdk/stream", () -> Stream.from(java.util.stream.Stream.of(1, 2, 3)), 1, 2, 3));
        cases.add(pipeline("boxed/boolean", () -> Stream.of(new boolean[] { false, true, false }), false, true, false));
        cases.add(pipeline("boxed/char", () -> Stream.of(new char[] { 'a', 'b', 'c' }), 'a', 'b', 'c'));
        cases.add(pipeline("boxed/byte", () -> Stream.of(new byte[] { 1, 2, 3 }), (byte) 1, (byte) 2, (byte) 3));
        cases.add(pipeline("boxed/short", () -> Stream.of(new short[] { 1, 2, 3 }), (short) 1, (short) 2, (short) 3));
        cases.add(pipeline("boxed/int", () -> Stream.of(new int[] { 1, 2, 3 }), 1, 2, 3));
        cases.add(pipeline("boxed/long", () -> Stream.of(new long[] { 1, 2, 3 }), 1L, 2L, 3L));
        cases.add(pipeline("boxed/float", () -> Stream.of(new float[] { 1, 2, 3 }), 1F, 2F, 3F));
        cases.add(pipeline("boxed/double", () -> Stream.of(new double[] { 1, 2, 3 }), 1D, 2D, 3D));
        for (boolean iterator : new boolean[] { false, true }) {
            String prefix = iterator ? "iterator/" : "array/";
            cases.add(pipeline(prefix + "mapFirst", () -> source(iterator).mapFirst(v -> -v), -1, 2, 3, 4, 5));
            cases.add(pipeline(prefix + "mapLast", () -> source(iterator).mapLast(v -> -v), 1, 2, 3, 4, -5));
            cases.add(pipeline(prefix + "split", () -> source(iterator).split(2), List.of(1, 2), List.of(3, 4), List.of(5)));
            cases.add(pipeline(prefix + "splitFactory", () -> source(iterator).split(2, ArrayList::new), List.of(1, 2), List.of(3, 4), List.of(5)));
            cases.add(pipeline(prefix + "splitCollector", () -> source(iterator).split(2, Collectors.toList()), List.of(1, 2), List.of(3, 4), List.of(5)));
            cases.add(pipeline(prefix + "splitAt", () -> source(iterator).splitAt(2), List.of(1, 2), List.of(3, 4, 5)));
            cases.add(pipeline(prefix + "splitAtEnd", () -> source(iterator).splitAt(5), List.of(1, 2, 3, 4, 5), List.of()));
            cases.add(pipeline(prefix + "slidingFactory", () -> source(iterator).sliding(3, 2, ArrayList::new), List.of(1, 2, 3), List.of(3, 4, 5)));
            cases.add(pipeline(prefix + "slidingCollector", () -> source(iterator).sliding(2, 3, Collectors.toList()), List.of(1, 2), List.of(4, 5)));
            cases.add(pipeline(prefix + "slidingPartial", () -> source(iterator).sliding(3, 3, ArrayList::new), List.of(1, 2, 3), List.of(4, 5)));
        }
        cases.add(pipeline("seq/split", () -> Seq.of(1, 2, 3, 4, 5).split(2).stream(), List.of(1, 2), List.of(3, 4), List.of(5)));
        cases.add(pipeline("seq/splitFactory", () -> Seq.of(1, 2, 3, 4, 5).split(2, ArrayList::new).stream(), List.of(1, 2), List.of(3, 4), List.of(5)));
        cases.add(pipeline("seq/splitCollector", () -> Seq.of(1, 2, 3, 4, 5).split(2, Collectors.toList()).stream(), List.of(1, 2), List.of(3, 4), List.of(5)));
        cases.add(pipeline("seq/slidingFactory", () -> Seq.of(1, 2, 3, 4, 5).sliding(3, 2, ArrayList::new).stream(), List.of(1, 2, 3), List.of(3, 4, 5)));
        cases.add(pipeline("seq/slidingCollector", () -> Seq.of(1, 2, 3, 4, 5).sliding(3, 3, Collectors.toList()).stream(), List.of(1, 2, 3), List.of(4, 5)));

        cases.add(pipeline("dataset/column", () -> dataset().stream("a"), 1, 2, 3));
        cases.add(pipeline("dataset/arrayRows", () -> dataset().stream(Object[].class), List.of(1, 4, 7), List.of(2, 5, 8), List.of(3, 6, 9)));
        cases.add(pipeline("dataset/suppliedRows", () -> dataset().stream(ArrayList::new), List.of(1, 4, 7), List.of(2, 5, 8), List.of(3, 6, 9)));
        cases.add(pipeline("dataset/indexedRows", () -> dataset().stream((i, row) -> i + ":" + row.get(0)), "0:1", "1:2", "2:3"));
        cases.add(pipeline("dataset/twoColumns", () -> dataset().stream(Tuple.of("a", "b"), (Integer a, Integer b) -> a + b), 5, 7, 9));
        cases.add(
                pipeline("dataset/threeColumns", () -> dataset().stream(Tuple.of("a", "b", "c"), (Integer a, Integer b, Integer c) -> a + b + c), 12, 15, 18));
        cases.add(pipeline("dataset/split", () -> dataset().split(2), List.of(List.of(1, 4, 7), List.of(2, 5, 8)), List.of(List.of(3, 6, 9))));

        cases.add(pipeline("sheet/rowMajor", () -> sheet().rowMajorStream(), 1, 2, 3, 4, 5, 6));
        cases.add(pipeline("sheet/columnMajor", () -> sheet().columnMajorStream(), 1, 4, 2, 5, 3, 6));
        cases.add(pipeline("sheet/rows", () -> sheet().rowStreams(), List.of(1, 2, 3), List.of(4, 5, 6)));
        cases.add(pipeline("sheet/columns", () -> sheet().columnStreams(), List.of(1, 4), List.of(2, 5), List.of(3, 6)));
        var r1 = List.of(Sheet.Cell.of("r1", "a", 1), Sheet.Cell.of("r1", "b", 2), Sheet.Cell.of("r1", "c", 3));
        var r2 = List.of(Sheet.Cell.of("r2", "a", 4), Sheet.Cell.of("r2", "b", 5), Sheet.Cell.of("r2", "c", 6));
        cases.add(pipeline("sheet/rowCells", () -> sheet().rowCells(), r1, r2));
        cases.add(pipeline("sheet/columnCells", () -> sheet().columnCells(), List.of(r1.get(0), r2.get(0)), List.of(r1.get(1), r2.get(1)),
                List.of(r1.get(2), r2.get(2))));
        cases.add(pipeline("sheet/rowMajorCells", () -> sheet().rowMajorCells(), r1.get(0), r1.get(1), r1.get(2), r2.get(0), r2.get(1), r2.get(2)));
        cases.add(pipeline("sheet/columnMajorCells", () -> sheet().columnMajorCells(), r1.get(0), r2.get(0), r1.get(1), r2.get(1), r1.get(2), r2.get(2)));
        var p00 = Sheet.Point.of(0, 0);
        var p01 = Sheet.Point.of(0, 1);
        var p02 = Sheet.Point.of(0, 2);
        var p10 = Sheet.Point.of(1, 0);
        var p11 = Sheet.Point.of(1, 1);
        var p12 = Sheet.Point.of(1, 2);
        cases.add(pipeline("sheet/rowMajorPoints", () -> sheet().rowMajorPoints(), p00, p01, p02, p10, p11, p12));
        cases.add(pipeline("sheet/columnMajorPoints", () -> sheet().columnMajorPoints(), p00, p10, p01, p11, p02, p12));
        cases.add(pipeline("sheet/rowPoints", () -> sheet().rowPoints(), List.of(p00, p01, p02), List.of(p10, p11, p12)));
        cases.add(pipeline("sheet/columnPoints", () -> sheet().columnPoints(), List.of(p00, p10), List.of(p01, p11), List.of(p02, p12)));
        return cases.stream();
    }

    private static RowDataset dataset() {
        return new RowDataset(List.of("a", "b", "c"), List.of(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8, 9)));
    }

    private static Sheet<String, String, Integer> sheet() {
        return Sheet.rows(List.of("r1", "r2"), List.of("a", "b", "c"), new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
    }

    private static Object snapshot(Object value) {
        if (value instanceof Stream<?> nested) {
            try (nested) {
                return snapshot(nested.toList());
            }
        }
        if (value instanceof Dataset table) {
            return snapshot(table.stream(Object[].class));
        }
        if (value instanceof Object[] array) {
            return snapshot(Arrays.asList(array));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(BulkObjectIteratorContractTest::snapshot).toList();
        }
        return value;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void advancePreservesSuffixAndCountDrainsIt(String name, Supplier<? extends Stream<?>> factory, List<?> expected) {
        for (long skip : new long[] { 0, 1, 2, expected.size(), Long.MAX_VALUE }) {
            int offset = (int) Math.min(skip, expected.size());
            try (Stream<?> stream = factory.get()) {
                ObjIteratorEx<?> iter = stream.iteratorEx();
                iter.advance(0);
                iter.advance(skip);
                List<Object> actual = new ArrayList<>();
                while (iter.hasNext()) {
                    actual.add(snapshot(iter.next()));
                }
                assertEquals(expected.subList(offset, expected.size()), actual, "skip=" + skip);
                assertExhausted(iter);
            }
            try (Stream<?> stream = factory.get()) {
                ObjIteratorEx<?> iter = stream.iteratorEx();
                iter.advance(skip);
                assertEquals(expected.size() - offset, iter.count(), "skip=" + skip);
                assertExhausted(iter);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void nextAndBulkConsumptionShareOneCursor(String name, Supplier<? extends Stream<?>> factory, List<?> expected) {
        try (Stream<?> stream = factory.get()) {
            ObjIteratorEx<?> iter = stream.iteratorEx();
            assertEquals(expected.get(0), snapshot(iter.next()));
            assertEquals(expected.subList(1, expected.size()), snapshot(iter.toList()));
            assertExhausted(iter);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelines")
    void arrayConsumptionDrainsExactlyOnce(String name, Supplier<? extends Stream<?>> factory, List<?> expected) {
        for (boolean started : new boolean[] { false, true }) {
            try (Stream<?> stream = factory.get()) {
                ObjIteratorEx<?> iter = stream.iteratorEx();
                int offset = 0;
                if (started) {
                    assertEquals(expected.get(0), snapshot(iter.next()));
                    offset = 1;
                }
                assertEquals(expected.subList(offset, expected.size()), snapshot(iter.toArray()));
                assertArrayEquals(new Object[0], iter.toArray());
                assertExhausted(iter);
            }
            try (Stream<?> stream = factory.get()) {
                ObjIteratorEx<?> iter = stream.iteratorEx();
                Object[] target = new Object[expected.size() + 2];
                Arrays.fill(target, "sentinel");
                assertSame(target, iter.toArray(target));
                assertEquals(expected, snapshot(Arrays.asList(target).subList(0, expected.size())));
                assertNull(target[expected.size()]);
                assertEquals("sentinel", target[expected.size() + 1]);
                assertExhausted(iter);
            }
        }
    }

    private static void assertExhausted(ObjIteratorEx<?> iter) {
        assertFalse(iter.hasNext());
        iter.advance(0);
        iter.advance(Long.MAX_VALUE);
        assertEquals(0, iter.count());
        assertThrows(NoSuchElementException.class, iter::next);
        assertEquals(List.of(), iter.toList());
    }
}
