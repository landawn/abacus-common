package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

/** Regressions for C-013: column-name ownership across transformation callbacks. */
public class DatasetSheetIterativeRegressionBTest extends TestBase {
    private static Dataset source() {
        return Dataset.rows(List.of("a", "b", "\u6D77"), new Object[][] { { 1, 2, "\u6D77" }, { null, 3, "\uD83D\uDE00" } });
    }

    private static Collection<String> unreadableNames() {
        return new AbstractCollection<>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public Iterator<String> iterator() {
                return List.of("x", "y").iterator();
            }

            @Override
            public Object[] toArray() {
                throw new IllegalStateException("names read failed");
            }
        };
    }

    private static void divide(Dataset data, String column, Collection<String> names, boolean consumer, Function<Object, List<?>> mapper) {
        if (consumer) {
            data.divideColumn(column, names, (value, output) -> {
                List<?> values = mapper.apply(value);
                for (int i = 0; i < values.size(); i++) {
                    output[i] = values.get(i);
                }
            });
        } else {
            data.divideColumn(column, names, mapper);
        }
    }

    private static Dataset map(Dataset data, Collection<String> names, int kind, Function<Object, Object> mapper) {
        return switch (kind) {
            case 0 -> data.mapColumn("b", "out", names, mapper);
            case 1 -> data.mapColumns(Tuple.of("b", "a"), "out", names, (b, a) -> mapper.apply(b));
            case 2 -> data.mapColumns(Tuple.of("b", "a", "\u6D77"), "out", names, (b, a, c) -> mapper.apply(b));
            case 3 -> data.mapColumns(List.of("b", "a", "\u6D77"), "out", names, row -> mapper.apply(row.get(0)));
            default -> throw new AssertionError(kind);
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void divideCopiesNamesBeforeReadingCanFail(boolean consumer) {
        Dataset data = source();
        Dataset before = data.copy();
        List<Object> column = data.getColumn("b");
        assertEquals(1, data.getColumnIndex("b"));
        var pending = data.stream("b");
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> divide(data, "b", unreadableNames(), consumer, value -> {
            calls.incrementAndGet();
            return Arrays.asList(value, null);
        }));
        assertEquals("names read failed", error.getMessage());
        assertEquals(0, calls.get());
        assertEquals(before, data);
        assertEquals(1, data.getColumnIndex("b"));
        assertEquals(List.of(2, 3), column);
        assertEquals(List.of(2, 3), pending.toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void divideUsesSnapshotAtEveryColumnPosition(boolean consumer) {
        for (int position = 0; position < 3; position++) {
            Dataset data = source();
            String oldName = data.columnNames().get(position);
            List<Object> oldValues = new ArrayList<>(data.getColumn(oldName));
            List<String> names = new ArrayList<>(List.of("\uD83D\uDE00", "\uD800"));
            List<String> expectedNames = new ArrayList<>(data.columnNames());
            expectedNames.remove(position);
            expectedNames.addAll(position, names);
            divide(data, oldName, names, consumer, value -> {
                names.clear();
                names.add("changed");
                return Arrays.asList(value, null);
            });
            assertEquals(expectedNames, data.columnNames());
            assertEquals(oldValues, data.getColumn(position));
            assertEquals(Arrays.asList(null, null), data.getColumn(position + 1));
            assertEquals(List.of("changed"), names);
            assertEquals(2, data.size());
            assertEquals(position, data.getColumnIndex("\uD83D\uDE00"));
            assertFalse(data.containsColumn(oldName));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void divideFailureDuringTransformationIsAtomic(boolean consumer) {
        Dataset data = source();
        Dataset before = data.copy();
        var pending = data.stream("b");
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("second row");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> divide(data, "b", List.of("x", "y"), consumer, value -> {
            if (calls.incrementAndGet() == 2) {
                throw failure;
            }
            return Arrays.asList(value, null);
        })));
        assertEquals(2, calls.get());
        assertEquals(before, data);
        assertEquals(List.of(2, 3), pending.toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void divideValidatesNamesAndHandlesEmptyRows(boolean consumer) {
        for (boolean empty : new boolean[] { false, true }) {
            Dataset data = source();
            if (empty) {
                data.clear();
            }
            Dataset before = data.copy();
            List<Collection<String>> invalid = Arrays.asList(null, List.of(), Arrays.asList("x", null), List.of("x", ""), List.of("x", "x"), List.of("a", "x"));
            for (Collection<String> names : invalid) {
                assertThrows(IllegalArgumentException.class, () -> divide(data, "b", names, consumer, value -> {
                    throw new AssertionError("invalid names reached callback");
                }));
                assertEquals(before, data);
            }
            assertThrows(IllegalArgumentException.class, () -> divide(data, "missing", List.of("x"), consumer, value -> List.of(value)));
            List<String> names = new ArrayList<>(List.of("x", "y"));
            divide(data, "b", names, consumer, value -> Arrays.asList(value, null));
            assertEquals(List.of("x", "y"), names);
            assertEquals(List.of("a", "x", "y", "\u6D77"), data.columnNames());
            assertEquals(empty ? 0 : 2, data.size());
            data.freeze();
            assertThrows(IllegalStateException.class, () -> divide(data, "x", List.of("z"), consumer, value -> Arrays.asList(value)));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 1, 3 })
    void divideRejectsMalformedFunctionResults(int length) {
        Dataset data = source();
        Dataset before = data.copy();
        assertThrows(IllegalArgumentException.class,
                () -> data.divideColumn("b", List.of("x", "y"), value -> length < 0 ? null : Arrays.asList(new Object[length])));
        assertEquals(before, data);
    }

    @Test
    void divideClearsOutputAndRejectsNullCallbacks() {
        Dataset data = source();
        assertThrows(IllegalArgumentException.class, () -> data.divideColumn("b", List.of("x"), (Function<Object, List<?>>) null));
        assertThrows(IllegalArgumentException.class, () -> data.divideColumn("b", List.of("x"), (java.util.function.BiConsumer<Object, Object[]>) null));
        data.divideColumn("b", List.of("x", "y"), (value, output) -> {
            assertNull(output[0]);
            assertNull(output[1]);
            output[0] = value;
            if (value.equals(2)) {
                output[1] = "\uD83D\uDE00";
            }
        });
        assertEquals(Arrays.asList("\uD83D\uDE00", null), data.getColumn("y"));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void mapKeepsCopiedLabelsAlignedWithValues(int kind) {
        for (boolean clear : new boolean[] { false, true }) {
            Dataset data = source();
            Dataset before = data.copy();
            List<String> names = new ArrayList<>(List.of("a", "\u6D77"));
            Dataset result = map(data, names, kind, value -> {
                if (clear) {
                    names.clear();
                } else {
                    names.set(0, "b");
                }
                return value;
            });
            assertEquals(List.of("a", "\u6D77", "out"), result.columnNames());
            assertEquals(Arrays.asList(1, null), result.getColumn("a"));
            assertEquals(List.of("\u6D77", "\uD83D\uDE00"), result.getColumn("\u6D77"));
            assertEquals(List.of(2, 3), result.getColumn("out"));
            assertEquals(before, data);
            result.set(0, 0, 99);
            assertEquals(before, data);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void mapReadsNamesBeforeInvokingMapper(int kind) {
        Dataset data = source();
        Dataset before = data.copy();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> map(data, unreadableNames(), kind, value -> {
            calls.incrementAndGet();
            return value;
        }));
        assertEquals(0, calls.get());
        assertEquals(before, data);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    void mapHandlesNullEmptyAndInvalidCopiedSelections(int kind) {
        for (boolean empty : new boolean[] { false, true }) {
            Dataset data = source();
            if (empty) {
                data.clear();
            }
            for (Collection<String> names : Arrays.<Collection<String>> asList(null, List.of())) {
                Dataset result = map(data, names, kind, value -> value);
                assertEquals(List.of("out"), result.columnNames());
                assertEquals(empty ? List.of() : List.of(2, 3), result.getColumn("out"));
            }
            for (Collection<String> names : Arrays.<Collection<String>> asList(Arrays.asList((String) null), List.of(""), List.of("missing"), List.of("out"))) {
                assertThrows(IllegalArgumentException.class, () -> map(data, names, kind, value -> {
                    throw new AssertionError("invalid names reached mapper");
                }));
            }
            List<String> names = new ArrayList<>(List.of("a"));
            Dataset result = map(data, names, kind, value -> null);
            assertEquals(List.of("a"), names);
            assertEquals(List.of("a", "out"), result.columnNames());
            assertEquals(empty ? List.of() : Arrays.asList(null, null), result.getColumn("out"));
        }
    }
}
