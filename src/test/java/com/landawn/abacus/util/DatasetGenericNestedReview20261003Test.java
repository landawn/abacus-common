package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DatasetGenericNestedReview20261003Test extends TestBase {
    enum Conversion {
        GET_ROW, FIRST_ROW, LAST_ROW, LIST, MAP, ENTITIES, MERGED, STREAM;

        <T> T read(Dataset dataset, Class<T> type) {
            return switch (this) {
                case GET_ROW -> dataset.getRow(0, type);
                case FIRST_ROW -> dataset.firstRow(type).orElseThrow();
                case LAST_ROW -> dataset.lastRow(type).orElseThrow();
                case LIST -> dataset.toList(type).get(0);
                case MAP -> dataset.<Integer, T> toMap("id", dataset.columnNames(), type).get(1);
                case ENTITIES -> dataset.toEntities(Map.of(), type).get(0);
                case MERGED -> dataset.toMergedEntities("id", type).get(0);
                case STREAM -> dataset.stream(type).toList().get(0);
            };
        }
    }

    @ParameterizedTest
    @EnumSource(Conversion.class)
    void convertsGenericBeanAndCollectionElement(Conversion conversion) {
        Dataset dataset = Dataset.rows(List.of("id", "child.value", "children.value"), new Object[][] { { 1, "42", "43" } });
        Parent result = conversion.read(dataset, Parent.class);
        assertEquals(Integer.valueOf(42), result.child.value);
        assertEquals(Integer.valueOf(43), result.children.get(0).value);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "GET_ROW", "LIST" })
    void resolvesMultipleGenericLevels(Conversion conversion) {
        Dataset dataset = Dataset.rows(List.of("id", "child.value.value"), new Object[][] { { 1, "42" } });
        DeepParent result = conversion.read(dataset, DeepParent.class);
        assertEquals(Integer.valueOf(42), result.child.value.value);
    }

    @ParameterizedTest
    @EnumSource(value = Conversion.class, names = { "GET_ROW", "LIST" })
    void resolvesGenericRecordComponents(Conversion conversion) {
        Dataset dataset = Dataset.rows(List.of("id", "child.value", "children.value"), new Object[][] { { 1, "42", "43" } });
        RecordParent result = conversion.read(dataset, RecordParent.class);
        assertEquals(Integer.valueOf(42), result.child.value());
        assertEquals(Integer.valueOf(43), result.children.get(0).value());
    }

    @Test
    void retainsInheritedTypeArguments() {
        Dataset dataset = Dataset.rows(List.of("id", "child.value"), new Object[][] { { 1, "42" } });
        assertEquals(Integer.valueOf(42), dataset.getRow(0, IntegerParent.class).child.value);
        assertEquals(Integer.valueOf(42), dataset.toList(IntegerParent.class).get(0).child.value);
    }

    @Test
    void retainsCompatibleValuesAndReferenceNulls() {
        Integer value = Integer.valueOf(12345);
        Dataset dataset = Dataset.rows(List.of("id", "child.value", "children.value"), new Object[][] { { 1, value, null } });
        for (Conversion conversion : List.of(Conversion.GET_ROW, Conversion.LIST)) {
            Parent result = conversion.read(dataset, Parent.class);
            assertSame(value, result.child.value);
            assertNull(result.children.get(0).value);
        }
    }

    @Test
    void rawAndObjectGenericPropertiesKeepTheirValues() {
        Dataset dataset = Dataset.rows(List.of("id", "raw.value", "object.value"), new Object[][] { { 1, "42", "43" } });
        for (Conversion conversion : List.of(Conversion.GET_ROW, Conversion.LIST)) {
            RawParent result = conversion.read(dataset, RawParent.class);
            assertEquals("42", result.raw.value);
            assertEquals("43", result.object.value);
        }
    }

    @Test
    void ordinaryNestedPropertiesStillConvert() {
        Dataset dataset = Dataset.rows(List.of("id", "child.value"), new Object[][] { { 1, "42" } });
        assertEquals(Integer.valueOf(42), dataset.getRow(0, OrdinaryParent.class).child.value);
        assertEquals(Integer.valueOf(42), dataset.toList(OrdinaryParent.class).get(0).child.value);
    }

    @Test
    void prefixMappingAndMissingPropertyPoliciesRemainEffective() {
        Dataset dataset = Dataset.rows(List.of("id", "c.value", "c.unknown"), new Object[][] { { 1, "42", "ignored" } });
        assertEquals(Integer.valueOf(42), dataset.toEntities(Map.of("c", "child"), Parent.class).get(0).child.value);
        Dataset strict = dataset.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        assertThrows(IllegalArgumentException.class, () -> strict.toEntities(Map.of("c", "child"), Parent.class));
        Dataset valid = Dataset.rows(List.of("id", "c.value"), new Object[][] { { 1, "42" } }).withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        assertEquals(Integer.valueOf(42), valid.toEntities(Map.of("c", "child"), Parent.class).get(0).child.value);
    }

    @Test
    void scalarNestedMissingPropertyPolicyRemainsEffective() {
        Dataset dataset = Dataset.rows(List.of("id", "child.unknown"), new Object[][] { { 1, "ignored" } });
        assertNull(dataset.getRow(0, Parent.class).child.value);
        Dataset strict = dataset.withMissingPropertyPolicy(Dataset.MissingPropertyPolicy.ERROR);
        assertThrows(IllegalArgumentException.class, () -> strict.getRow(0, Parent.class));
    }

    @Test
    void mergedIdsUseStoredValuesAndRetainExistingCollection() {
        Dataset dataset = Dataset.rows(List.of("id", "children.id", "children.value"), new Object[][] { { 1, "7", "42" }, { 1, "7", "43" }, { 1, 7, "44" } });
        MergeParent result = dataset.toMergedEntities("id", MergeParent.class).get(0);
        assertEquals(3, result.children.size());
        assertSame(result.initial, result.children.get(0));
        assertEquals(Integer.valueOf(7), result.children.get(1).id);
        assertEquals(Integer.valueOf(7), result.children.get(2).id);
        assertEquals(Integer.valueOf(43), result.children.get(1).value);
        assertEquals(Integer.valueOf(44), result.children.get(2).value);
    }

    public static class Box<T> {
        public T value;
    }

    public static class Parent {
        public int id;
        public Box<Integer> child;
        public List<Box<Integer>> children;
    }

    public static class DeepParent {
        public int id;
        public Box<Box<Integer>> child;
    }

    public record Value<T>(T value) {
    }

    public static class RecordParent {
        public int id;
        public Value<Integer> child;
        public List<Value<Integer>> children;
    }

    public static class GenericParent<T> {
        public int id;
        public Box<T> child;
    }

    public static class IntegerParent extends GenericParent<Integer> {
    }

    @SuppressWarnings("rawtypes")
    public static class RawParent {
        public int id;
        public Box raw;
        public Box<Object> object;
    }

    public static class OrdinaryChild {
        public Integer value;
    }

    public static class OrdinaryParent {
        public int id;
        public OrdinaryChild child;
    }

    public static class IdBox<T> {
        public Integer id;
        public T value;
    }

    public static class MergeParent {
        public int id;
        public final IdBox<Integer> initial = new IdBox<>();
        public List<IdBox<Integer>> children = new java.util.ArrayList<>(List.of(initial));
    }
}
