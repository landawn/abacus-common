package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DatasetSuppliedNestedReview20261003Test extends TestBase {
    enum Conversion {
        MAP, MAP_RANGE, MAP_FACTORY, MAP_RANGE_FACTORY, MULTI, MULTI_RANGE, MULTI_FACTORY, MULTI_RANGE_FACTORY;

        <T> T read(Dataset data, List<String> columns, IntFunction<T> rows) {
            return switch (this) {
                case MAP -> data.<Integer, T> toMap("id", columns, rows).get(1);
                case MAP_RANGE -> data.<Integer, T> toMap(0, data.size(), "id", columns, rows).get(1);
                case MAP_FACTORY -> data.<Integer, T, Map<Integer, T>> toMap("id", columns, rows, n -> new LinkedHashMap<>()).get(1);
                case MAP_RANGE_FACTORY -> data.<Integer, T, Map<Integer, T>> toMap(0, data.size(), "id", columns, rows, n -> new LinkedHashMap<>()).get(1);
                case MULTI -> data.<Integer, T> toMultimap("id", columns, rows).get(1).get(0);
                case MULTI_RANGE -> data.<Integer, T> toMultimap(0, data.size(), "id", columns, rows).get(1).get(0);
                case MULTI_FACTORY -> data
                        .<Integer, T, List<T>, ListMultimap<Integer, T>> toMultimap("id", columns, rows, n -> CommonUtil.newLinkedListMultimap())
                        .get(1)
                        .get(0);
                case MULTI_RANGE_FACTORY -> data
                        .<Integer, T, List<T>, ListMultimap<Integer, T>> toMultimap(0, data.size(), "id", columns, rows,
                                n -> CommonUtil.newLinkedListMultimap())
                        .get(1)
                        .get(0);
            };
        }
    }

    @ParameterizedTest
    @EnumSource(Conversion.class)
    void replacesNestedCollectionsWithoutMutatingTheirOldElements(Conversion conversion) {
        Dataset data = Dataset.rows(List.of("id", "children.value"), new Object[][] { { 1, "42" } });
        Parent supplied = new Parent();
        List<Child> previous = supplied.children;
        Parent result = conversion.read(data, List.of("children.value"), n -> supplied);
        assertSame(supplied, result);
        assertNotSame(previous, result.children);
        assertEquals(1, result.children.size());
        assertEquals(42, result.children.get(0).value);
        assertEquals(2, previous.size());
        assertEquals(-1, previous.get(0).value);
        assertEquals(-1, previous.get(1).value);
        assertEquals("retained", result.unselected);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "MAP", "MULTI" })
    void createsNestedRecordsInSuppliedMutableBeans(Conversion conversion) {
        Dataset data = Dataset.rows(List.of("id", "child.value", "children.value"), new Object[][] { { 1, "42", "43" } });
        RecordParent result = conversion.read(data, List.of("child.value", "children.value"), n -> new RecordParent());
        assertEquals(new ChildRecord(42), result.child);
        assertEquals(List.of(new ChildRecord(43)), result.children);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "MAP", "MULTI" })
    void replacesAnEmptyImmutableCollection(Conversion conversion) {
        Dataset data = Dataset.rows(List.of("id", "children.value"), new Object[][] { { 1, "42" } });
        Parent supplied = new Parent();
        supplied.children = List.of();
        assertEquals(42, conversion.read(data, List.of("children.value"), n -> supplied).children.get(0).value);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "MAP", "MULTI" })
    void preservesSupplierIdentityCountsAndPlainValueConversion(Conversion conversion) {
        Dataset data = Dataset.rows(List.of("id", "value"), new Object[][] { { 1, "42" }, { 2, 43 } });
        AtomicInteger calls = new AtomicInteger();
        List<Child> supplied = new ArrayList<>();
        Child first = conversion.read(data, List.of("value"), n -> {
            assertEquals(1, n);
            calls.incrementAndGet();
            Child value = new Child();
            supplied.add(value);
            return value;
        });
        assertEquals(2, calls.get());
        assertSame(supplied.get(0), first);
        assertEquals(42, first.value);
        assertEquals(43, supplied.get(1).value);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "MAP", "MULTI" })
    void preservesMissingPropertyPolicy(Conversion conversion) {
        Dataset data = Dataset.rows(List.of("id", "unknown", "value"), new Object[][] { { 1, "ignored", "42" } });
        assertEquals(42, conversion.read(data, List.of("unknown", "value"), n -> new Child()).value);
        Dataset strict = data.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        assertThrows(IllegalArgumentException.class, () -> conversion.read(strict, List.of("unknown", "value"), n -> new Child()));
    }

    @Test
    void probesTheRowSupplierOnceForEmptyRanges() {
        Dataset data = Dataset.rows(List.of("id", "value"), new Object[0][]);
        AtomicInteger calls = new AtomicInteger();
        IntFunction<Child> rows = n -> {
            calls.incrementAndGet();
            return new Child();
        };
        assertTrue(data.toMap("id", List.of("value"), rows).isEmpty());
        assertEquals(1, calls.get());
        assertTrue(data.toMultimap("id", List.of("value"), rows).isEmpty());
        assertEquals(2, calls.get());
        assertThrows(NullPointerException.class, () -> data.toMap("id", List.of("value"), (IntFunction<Child>) n -> null));
        assertThrows(NullPointerException.class, () -> data.toMultimap("id", List.of("value"), (IntFunction<Child>) n -> null));
    }

    @Test
    void retainsMapDuplicateAndMultimapEncounterOrder() {
        Dataset data = Dataset.rows(List.of("id", "value"), new Object[][] { { 1, "42" }, { 1, "43" } });
        assertEquals(43, data.<Integer, Child> toMap("id", List.of("value"), n -> new Child()).get(1).value);
        List<Child> values = data.<Integer, Child> toMultimap("id", List.of("value"), n -> new Child()).get(1);
        assertEquals(2, values.size());
        assertEquals(42, values.get(0).value);
        assertEquals(43, values.get(1).value);
    }

    public static class Child {
        public int value = -1;
    }

    public static class Parent {
        public String unselected = "retained";
        public List<Child> children = new ArrayList<>(List.of(new Child(), new Child()));
    }

    public record ChildRecord(int value) {
    }

    public static class RecordParent {
        public ChildRecord child;
        public List<ChildRecord> children;
    }
}
