package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.util.Dataset.MissingPropertyPolicy;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.IntBiObjPredicate;
import com.landawn.abacus.util.stream.Stream;

/** Behavioral regressions for Dataset/Sheet mutation, conversion, export, and relational contracts. */
public class DatasetSheetContractTest extends TestBase {
    @TempDir
    Path temporary;

    private static Dataset dataset() {
        return Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 10 }, { 2, 20 }, { 3, 30 } });
    }

    private static Sheet<String, String, Integer> sheet() {
        return Sheet.rows(List.of("r1", "r2", "r3"), List.of("c1", "c2", "c3"), new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
    }

    @ParameterizedTest
    @ValueSource(strings = { "append", "prepend", "merge" })
    void datasetAcceptsItsOwnSliceAsInput(final String operation) {
        for (boolean full : new boolean[] { false, true }) {
            final Dataset target = dataset();
            final Dataset source = target.slice(0, full ? 3 : 2);
            final var stream = target.stream(Object[].class);
            switch (operation) {
                case "append" -> target.append(source);
                case "prepend" -> target.prepend(source);
                default -> target.merge(source);
            }
            final List<Integer> expected = full ? List.of(1, 2, 3, 1, 2, 3) : operation.equals("prepend") ? List.of(1, 2, 1, 2, 3) : List.of(1, 2, 3, 1, 2);
            assertEquals(expected, target.getColumn("a"));
            assertEquals(expected.stream().map(v -> v * 10).toList(), target.getColumn("b"));
            assertThrows(ConcurrentModificationException.class, () -> stream.iterator().next());
            assertThrows(ConcurrentModificationException.class, source::size);
        }
    }

    @Test
    void mergeSnapshotsSelectionsAndPreservesMissingColumns() {
        final Dataset target = dataset();
        target.merge(target, 1, 3, target.columnNames());
        assertEquals(List.of(1, 2, 3, 2, 3), target.getColumn("a"));
        final Dataset other = Dataset.rows(List.of("b", "c"), new Object[][] { { 40, "x" } });
        target.merge(other);
        assertEquals(Arrays.asList(1, 2, 3, 2, 3, null), target.getColumn("a"));
        assertEquals(Arrays.asList(null, null, null, null, null, "x"), target.getColumn("c"));
        assertEquals(6, target.getColumn("b").size());
        assertEquals(1, other.size());
    }

    @Test
    void invalidIncomingDatasetDoesNotPartiallyModifyTarget() {
        final Dataset target = dataset();
        final Dataset before = target.copy();
        final Dataset stale = target.slice(0, 2);
        target.swapRows(0, 1);
        final Dataset reordered = target.copy();
        assertThrows(ConcurrentModificationException.class, () -> target.merge(stale));
        assertEquals(reordered, target);
        target.swapRows(0, 1);
        assertEquals(before, target);
    }

    @Test
    void emptyAppendsDoNotInvalidateStreams() {
        final Dataset target = dataset();
        final var stream = target.stream(Object[].class);
        target.append(target.slice(0, 0));
        target.prepend(target.slice(0, 0));
        target.merge(target.slice(0, 0));
        assertEquals(3, stream.count());
    }

    @Test
    void sheetSnapshotsCrossedAndSameAxisViews() {
        final var rows = sheet();
        rows.setRow("r2", rows.columnValues("c1"));
        assertEquals(List.of(1, 4, 7), rows.rowValues("r2"));
        rows.setRow("r1", rows.rowValues("r1"));
        assertEquals(List.of(1, 2, 3), rows.rowValues("r1"));
        final var columns = sheet();
        columns.setColumn("c2", columns.rowValues("r1"));
        assertEquals(List.of(1, 2, 3), columns.columnValues("c2"));
        columns.setColumn("c1", columns.columnValues("c1"));
        assertEquals(List.of(1, 4, 7), columns.columnValues("c1"));
    }

    @Test
    void sheetSnapshotsBeforeAddingOrInsertingKeys() {
        final var appended = sheet();
        appended.addColumn("c4", appended.rowValues("r1"));
        assertEquals(List.of(1, 2, 3), appended.columnValues("c4"));
        final var inserted = sheet();
        inserted.addRow(0, "r0", inserted.rowValues("r3"));
        assertEquals(List.of(7, 8, 9), inserted.rowValues("r0"));
        assertEquals(List.of(7, 1, 4, 7), inserted.columnValues("c1"));
        final var insertedColumn = sheet();
        insertedColumn.addColumn(0, "c0", insertedColumn.columnValues("c3"));
        assertEquals(List.of(3, 6, 9), insertedColumn.columnValues("c0"));
        final var appendedRow = sheet();
        appendedRow.addRow("r4", appendedRow.columnValues("c1"));
        assertEquals(List.of(1, 4, 7), appendedRow.rowValues("r4"));
    }

    @Test
    void failingSheetInputIterationLeavesStructureUnchanged() {
        final var target = sheet();
        final var before = target.copy();
        final List<Integer> broken = new java.util.AbstractList<>() {
            @Override
            public int size() {
                return 3;
            }

            @Override
            public Integer get(int index) {
                if (index == 1) {
                    throw new IllegalStateException("unreadable");
                }
                return 1;
            }
        };
        assertThrows(IllegalStateException.class, () -> target.addColumn("x", broken));
        assertThrows(IllegalStateException.class, () -> target.addRow(0, "x", broken));
        assertThrows(IllegalStateException.class, () -> target.setRow("r1", broken));
        assertThrows(IllegalStateException.class, () -> target.setColumn("c1", broken));
        assertEquals(before, target);
    }

    public static class Parent {
        private Integer id;
        private List<Child> children;

        public Integer getId() {
            return id;
        }

        public void setId(Integer value) {
            id = value;
        }

        public List<Child> getChildren() {
            return children;
        }

        public void setChildren(List<Child> value) {
            children = value;
        }
    }

    public static class Child {
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer value) {
            id = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }
    }

    public static class InitializedParent extends Parent {
        public InitializedParent() {
            setChildren(new ArrayList<>());
        }
    }

    public static class SeededParent extends Parent {
        public SeededParent() {
            final Child seed = new Child();
            seed.setId(-1);
            setChildren(new ArrayList<>(List.of(seed, seed)));
        }
    }

    @Test
    void mergedChildrenAreDeduplicatedInBeanInitializedLists() {
        final Dataset data = Dataset.rows(List.of("id", "children.id", "children.name"),
                new Object[][] { { 1, 10, "A" }, { 1, 10, "A" }, { 1, 20, "B" }, { 2, 10, "A" }, { 2, 10, "A" } });
        final List<InitializedParent> parents = data.toMergedEntities("id", InitializedParent.class);

        assertEquals(2, parents.size());
        assertEquals(List.of(10, 20), parents.get(0).getChildren().stream().map(Child::getId).toList());
        assertEquals(List.of(10), parents.get(1).getChildren().stream().map(Child::getId).toList());
        assertNotSame(parents.get(0).getChildren().get(0), parents.get(1).getChildren().get(0));
    }

    @Test
    void mergingChildrenPreservesBeanInitializedContents() {
        final Dataset data = Dataset.rows(List.of("id", "children.id", "children.name"), new Object[][] { { 1, 10, "A" }, { 1, 10, "A" } });
        final List<Child> children = data.toMergedEntities("id", SeededParent.class).get(0).getChildren();

        assertEquals(List.of(-1, -1, 10), children.stream().map(Child::getId).toList());
        assertSame(children.get(0), children.get(1));
    }

    @Test
    void missingChildIdsDoNotChangeOtherParentsOrDisappear() {
        for (int count = 0; count <= 3; count++) {
            final List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[] { 1, 10, "A" });
            rows.add(new Object[] { 1, 10, "A" });
            rows.add(new Object[] { 3, null, null });
            rows.add(new Object[] { null, 10, "dropped" });
            for (int i = 0; i < count; i++) {
                rows.add(new Object[] { 2, null, "N" + i });
            }
            final Dataset data = Dataset.rows(List.of("id", "children.id", "children.name"), rows.toArray(Object[][]::new));
            final List<Parent> parents = data.toMergedEntities("id", Parent.class);
            assertEquals(1, parents.get(0).getChildren().size());
            assertEquals("A", parents.get(0).getChildren().get(0).getName());
            assertNull(parents.get(1).getChildren());
            if (count > 0) {
                assertEquals(count, parents.get(2).getChildren().size());
                assertEquals("N0", parents.get(2).getChildren().get(0).getName());
            }
        }
    }

    @Test
    void equalChildIdsAreScopedToTheParent() {
        final Dataset data = Dataset.rows(List.of("id", "children.id", "children.name"),
                new Object[][] { { 1, 0, "first" }, { 2, 0, "second" }, { 1, 0, "updated" } });
        final List<Parent> parents = data.toMergedEntities("id", Parent.class);
        assertEquals("updated", parents.get(0).getChildren().get(0).getName());
        assertEquals("second", parents.get(1).getChildren().get(0).getName());
        assertNotSame(parents.get(0).getChildren().get(0), parents.get(1).getChildren().get(0));
    }

    public static class CompositeParent {
        private Integer id;
        private List<CompositeChild> children;

        public Integer getId() {
            return id;
        }

        public void setId(Integer value) {
            id = value;
        }

        public List<CompositeChild> getChildren() {
            return children;
        }

        public void setChildren(List<CompositeChild> value) {
            children = value;
        }
    }

    public static class CompositeChild {
        @Id
        private Integer part1;
        @Id
        private Integer part2;
        private String name;

        public Integer getPart1() {
            return part1;
        }

        public void setPart1(Integer value) {
            part1 = value;
        }

        public Integer getPart2() {
            return part2;
        }

        public void setPart2(Integer value) {
            part2 = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }
    }

    @Test
    void compositeChildIdsIncludePartialNullAndDefaultComponents() {
        final Dataset data = Dataset.rows(List.of("id", "children.part1", "children.part2", "children.name"),
                new Object[][] { { 1, null, 1, "A" }, { 1, null, 1, "B" }, { 1, null, 2, "C" }, { 1, 0, 0, "D" }, { 1, 0, 0, "E" } });
        final List<CompositeParent> parents = data.toMergedEntities("id", CompositeParent.class);
        assertEquals(3, parents.get(0).getChildren().size());
        assertEquals(List.of("B", "C", "E"), parents.get(0).getChildren().stream().map(CompositeChild::getName).toList());
    }

    private static void export(final Dataset data, final String format, final Path output, final int from, final List<String> columns) {
        switch (format) {
            case "json" -> data.toJson(from, data.size(), columns, output.toFile());
            case "xml" -> data.toXml(from, data.size(), columns, output.toFile());
            default -> data.toCsv(from, data.size(), columns, output.toFile());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "xml", "csv" })
    void fileExportPreservesDestinationOnInvalidArgumentsAndWritesUtf8(final String format) throws Exception {
        final Path output = temporary.resolve("export." + format);
        Files.writeString(output, "KEEP");
        assertThrows(IndexOutOfBoundsException.class, () -> export(dataset(), format, output, -1, List.of("a")));
        assertEquals("KEEP", Files.readString(output));
        assertThrows(IllegalArgumentException.class, () -> export(dataset(), format, output, 0, List.of("missing")));
        assertEquals("KEEP", Files.readString(output));
        final Dataset unicode = Dataset.rows(List.of("text"), new Object[][] { { "海😀" } });
        export(unicode, format, output, 0, List.of("text"));
        assertTrue(Files.readString(output).contains("海😀"));
        try (var files = Files.list(temporary)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void failedValueSerializationPreservesExistingExport() throws Exception {
        final Path json = temporary.resolve("data.json");
        Files.writeString(json, "KEEP");
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(List.of("x"), new Object[][] { { 1 }, { Double.NaN } }).toJson(json.toFile()));
        assertEquals("KEEP", Files.readString(json));
        final Path xml = temporary.resolve("data.xml");
        Files.writeString(xml, "KEEP");
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(List.of("x"), new Object[][] { { "ok" }, { "bad\u0000" } }).toXml(xml.toFile()));
        assertEquals("KEEP", Files.readString(xml));
        assertThrows(IllegalArgumentException.class, () -> dataset().toXml("bad name", xml.toFile()));
        assertEquals("KEEP", Files.readString(xml));
    }

    @Test
    void jsonRejectsNonFiniteNumbersAtEveryNestingLevel() {
        for (Object value : List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN, new double[] { 1, Double.NaN },
                List.of(Float.POSITIVE_INFINITY), Map.of("nested", Double.NaN))) {
            final Dataset data = Dataset.rows(List.of("x"), new Object[][] { { value } });
            final var failure = assertThrows(IllegalArgumentException.class, data::toJson);
            assertTrue(failure.getMessage().contains("row 0"));
            assertTrue(failure.getMessage().contains("'x'"));
        }
        final String json = Dataset.rows(List.of("x"), new Object[][] { { -0.0 }, { Double.MAX_VALUE }, { "NaN Infinity \"quoted\"" } }).toJson();
        assertTrue(json.contains("-0.0"));
        assertTrue(json.contains("NaN Infinity"));
    }

    @Test
    void xmlRejectsIllegalCharactersAndAcceptsLegalUnicodeAndLiteralReferences() throws Exception {
        for (Object value : List.of("\u0000", "\u0001", "\uD800", "\uDC00", Map.of("nested", "\u0000"))) {
            assertThrows(IllegalArgumentException.class, () -> Dataset.rows(List.of("x"), new Object[][] { { value } }).toXml());
        }
        // Character arrays have a type-defined escaped representation: validate the emitted XML
        // and its exact round-trip instead of rejecting code units that are never emitted raw.
        final char[] chars = { '\u0000', '\u0001', '\uD800', '\uDC00', '\uFFFE', '\uFFFF', ' ', ',', '\\', '\'' };
        final String arrayXml = Dataset.rows(List.of("x"), new Object[][] { { chars } }).toXml();
        final var arrayDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arrayXml)));
        final com.landawn.abacus.type.Type<char[]> charsType = CommonUtil.typeOf(char[].class);
        assertArrayEquals(chars, charsType.valueOf(arrayDocument.getElementsByTagName("x").item(0).getTextContent()));
        final String text = "海😀\t\n\r&#x0;";
        final String xml = Dataset.rows(List.of("x"), new Object[][] { { text } }).toXml();
        final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        assertTrue(document.getDocumentElement().getTextContent().contains("&#x0;"));
        assertTrue(document.getDocumentElement().getTextContent().contains("海😀"));
    }

    @Test
    void pivotAndSheetUseDeepArrayKeysThroughout() {
        final Dataset data = Dataset.rows(List.of("r", "c", "v"), new Object[][] { { new int[] { 1 }, new String[] { "x" }, 10 },
                { new int[] { 1 }, new String[] { "y" }, 20 }, { new int[] { 1 }, new String[] { "x" }, 5 } });
        final Sheet<int[], String[], Integer> result = data.pivot("r", "c", "v", java.util.stream.Collectors.summingInt(v -> (Integer) v));
        assertEquals(1, result.rowCount());
        assertEquals(2, result.columnCount());
        assertEquals(Integer.valueOf(15), result.get(new int[] { 1 }, new String[] { "x" }));
        assertTrue(result.containsRow(new int[] { 1 }));
        assertTrue(result.containsColumn(new String[] { "y" }));
        assertThrows(IllegalArgumentException.class, () -> result.addRow(new int[] { 1 }, List.of(1, 2)));
        final var equal = Sheet.rows(List.of(new int[] { 1 }), Arrays.asList(new String[] { "x" }, new String[] { "y" }), new Integer[][] { { 15, 20 } });
        assertEquals(equal, result);
        assertEquals(Integer.valueOf(20), result.copy(List.of(new int[] { 1 }), Collections.singletonList(new String[] { "y" })).getAt(0, 0));
        assertEquals(Integer.valueOf(30), result.merge(equal, Integer::sum).get(new int[] { 1 }, new String[] { "x" }));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(Arrays.asList(new int[] { 1 }, new int[] { 1 }), List.of("x")));
        assertEquals(equal.hashCode(), result.hashCode());
        assertEquals(new java.util.HashSet<>(result.rowKeySet()).hashCode(), result.rowKeySet().hashCode());
        result.set(new int[] { 1 }, new String[] { "y" }, 30);
        assertEquals(Integer.valueOf(30), result.getAt(0, 1));
        result.moveColumn(new String[] { "y" }, 0);
        assertEquals(Integer.valueOf(30), result.getAt(0, 0));
        result.removeColumn(new String[] { "x" });
        assertEquals(1, result.columnCount());
        assertEquals(Integer.valueOf(30), result.get(new int[] { 1 }, new String[] { "y" }));
    }

    @Test
    void typedStreamSnapshotsSelectionAndMapping() {
        final Dataset data = dataset();
        final List<String> selection = new ArrayList<>(List.of("a"));
        final var iteratorStream = data.stream(selection, Object[].class);
        final var arrayStream = data.stream(selection, Object[].class);
        selection.set(0, "b");
        final Dataset nested = Dataset.rows(List.of("id", "c.id", "c.name"), new Object[][] { { 1, 10, "child" } });
        final Map<String, String> prefixes = new java.util.HashMap<>(Map.of("c", "children"));
        final var nestedIterator = nested.stream(prefixes, Parent.class);
        final var nestedArray = nested.stream(prefixes, Parent.class);
        prefixes.put("c", "absent");
        assertEquals("child", nestedIterator.iterator().next().getChildren().get(0).getName());
        assertEquals("child", ((Parent) nestedArray.toArray()[0]).getChildren().get(0).getName());
        assertArrayEquals(new Object[] { 1 }, iteratorStream.iterator().next());
        assertArrayEquals(new Object[] { 1 }, (Object[]) arrayStream.toArray()[0]);
        assertArrayEquals(new Object[] { 10, 1 }, data.toList(List.of("b", "a"), Object[].class).get(0));
        assertTrue(data.toJson(0, 1, List.of("b", "a")).indexOf("\"b\"") < data.toJson(0, 1, List.of("b", "a")).indexOf("\"a\""));
    }

    @Test
    void slicesTrackLogicalRowsButNotStorageOrColumnMetadata() {
        for (Consumer<Dataset> change : List.<Consumer<Dataset>> of(d -> d.swapRows(0, 2), d -> d.moveRow(0, 2), d -> d.addRow(new Object[] { 4, 40 }),
                d -> d.removeRow(0), Dataset::clear)) {
            final Dataset parent = dataset();
            final Dataset full = parent.slice(0, 3);
            final Dataset partial = full.slice(0, 2);
            change.accept(parent);
            assertThrows(ConcurrentModificationException.class, full::size);
            assertThrows(ConcurrentModificationException.class, partial::size);
            assertThrows(ConcurrentModificationException.class, () -> partial.get(0, 0));
        }
        final Dataset parent = dataset();
        final Dataset view = parent.slice(0, 2);
        parent.renameColumn("a", "renamed");
        parent.moveColumn("renamed", 1);
        parent.set(0, 1, 99);
        parent.freeze();
        parent.trimToSize();
        assertEquals((Object) 99, view.get(0, 0));
        assertEquals(List.of("a", "b"), view.columnNames());
    }

    @Test
    void emptySlicesAndDeepClonesRespectViewBoundaries() {
        final Dataset source = dataset();
        final Dataset empty = source.slice(0, 0, List.of());
        final Dataset nestedEmpty = empty.slice(0, 0, List.of());
        final Dataset clone = source.slice(1, 3).clone(false);
        source.addRow(new Object[] { 4, 40 });
        assertThrows(ConcurrentModificationException.class, empty::size);
        assertThrows(ConcurrentModificationException.class, nestedEmpty::size);
        assertEquals(List.of(2, 3), clone.getColumn("a"));
        clone.addRow(new Object[] { 5, 50 });
        assertEquals(3, clone.size());
    }

    @Test
    void deepClonesPreserveCyclesAndArrayKeyEquivalence() {
        final Sheet<int[], String, Object> original = Sheet.rows(List.of(new int[] { 1 }), List.of("cell"), new Object[][] { { null } });
        original.set(new int[] { 1 }, "cell", original);
        final Sheet<int[], String, Object> clone = original.clone(false);
        assertSame(clone, clone.get(new int[] { 1 }, "cell"));
        assertNotSame(original.rowKeySet().iterator().next(), clone.rowKeySet().iterator().next());
        assertTrue(clone.containsRow(new int[] { 1 }));
        assertTrue(clone.rowKeySet().contains(clone.rowKeySet().iterator().next()));
        assertFalse(clone.rowKeySet().contains(new int[] { 1 }));
        clone.addRow(new int[] { 2 }, List.of("added"));
        clone.swapRows(new int[] { 1 }, new int[] { 2 });
        assertSame(clone, clone.get(new int[] { 1 }, "cell"));
        final int[] emptyKey = new int[0];
        final Sheet<int[], String, Object> emptyArrayKey = Sheet.rows(List.of(emptyKey), List.of("cell"), new Object[][] { { original } });
        assertSame(emptyKey, emptyArrayKey.rowMajorCells().iterator().next().rowKey());
        final Sheet<int[], String, Object> nestedClone = emptyArrayKey.clone(false);
        final Sheet<?, ?, ?> clonedCell = (Sheet<?, ?, ?>) nestedClone.getAt(0, 0);
        assertSame(clonedCell, clonedCell.getAt(0, 0));
        final Dataset data = Dataset.rows(List.of("self"), new Object[][] { { null } });
        data.set(0, 0, data);
        final Dataset copy = data.clone(false);
        assertSame(copy, copy.get(0, 0));
    }

    @Test
    void paginationAndSplittingHandleBoundaryArithmetic() {
        final Dataset virtual = new RowDataset(new ArrayList<>(List.of("x")), new ArrayList<>(List.of(Collections.nCopies(Integer.MAX_VALUE, (Object) 1))),
                null, true);
        virtual.freeze();
        assertEquals(647_483_647, virtual.paginate(1_500_000_000).getPage(1).size());
        assertEquals(1, virtual.paginate(Integer.MAX_VALUE - 1).getPage(1).size());
        assertEquals(3, dataset().splitToList(Integer.MAX_VALUE).get(0).size());
        assertEquals(List.of(2, 1), dataset().splitToList(2).stream().map(Dataset::size).toList());
    }

    @Test
    void integerKeysAndCoordinatesHaveDistinctMethods() {
        final Sheet<Integer, Integer, String> data = Sheet.rows(List.of(1, 0), List.of(1, 0), new String[][] { { "11", "10" }, { "01", "00" } });
        assertEquals("00", data.get(0, 0));
        assertEquals("11", data.getAt(0, 0));
        data.set(0, 0, "key");
        data.setAt(0, 0, "position");
        assertEquals("key", data.remove(0, 0));
        assertEquals("position", data.removeAt(0, 0));
        assertTrue(data.isNull(0, 0));
        assertTrue(data.isNullAt(0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "rows", "columns", "constructor" })
    void arrayFactoriesRejectIncorrectGenericValueTypesAtCompileTime(final String factory) throws Exception {
        final String expression = factory.equals("constructor") ? "new Sheet<>(List.of(1),List.of(1),new String[][]{{\"bad\"}})"
                : "Sheet." + factory + "(List.of(1),List.of(1),new String[][]{{\"bad\"}})";
        final String source = "import java.util.*; import com.landawn.abacus.util.Sheet; class TypeCheck { Sheet<Integer,Integer,Integer> value = " + expression
                + "; }";
        final JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///TypeCheck.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignored) {
                return source;
            }
        };
        final var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            assertFalse(compiler
                    .getTask(new StringWriter(), files, null,
                            List.of("-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", temporary.toString()), null, List.of(unit))
                    .call());
        }
    }

    public static class OnlyA {
        private Integer a;

        public Integer getA() {
            return a;
        }

        public void setA(Integer value) {
            a = value;
        }
    }

    @Test
    void beanMappingPolicyDoesNotDependOnSelectionIdentity() {
        final Dataset data = dataset();
        for (List<String> names : List.of(data.columnNames(), new ArrayList<>(data.columnNames()), List.of("a", "b"))) {
            assertEquals(3, data.toList(names, OnlyA.class).size());
            assertEquals(Integer.valueOf(1), data.getRow(0, names, OnlyA.class).getA());
            assertEquals(3, data.stream(names, OnlyA.class).toArray().length);
            final Dataset strict = data.withMissingPropertyPolicy(MissingPropertyPolicy.ERROR);
            assertThrows(IllegalArgumentException.class, () -> strict.toList(names, OnlyA.class));
            assertThrows(IllegalArgumentException.class, () -> strict.getRow(0, names, OnlyA.class));
            assertThrows(IllegalArgumentException.class, () -> strict.stream(names, OnlyA.class).toArray());
            assertThrows(IllegalArgumentException.class, () -> strict.toMap("a", names, OnlyA.class));
            assertThrows(IllegalArgumentException.class, () -> strict.toMultimap("a", names, OnlyA.class));
            assertThrows(IllegalArgumentException.class, () -> strict.copy().toList(names, OnlyA.class));
            assertThrows(IllegalArgumentException.class, () -> strict.clone().toList(names, OnlyA.class));
            assertTrue(strict.isFrozen());
        }
        assertThrows(IllegalArgumentException.class, () -> data.withMissingPropertyPolicy(null));
        final Dataset nested = Dataset.rows(List.of("id", "children.unknown"), new Object[][] { { 1, "value" } });
        assertEquals(1, nested.toMergedEntities("id", Parent.class).size());
        assertThrows(IllegalArgumentException.class, () -> nested.withMissingPropertyPolicy(MissingPropertyPolicy.ERROR).toMergedEntities("id", Parent.class));
    }

    @Test
    void setOperationsUseBagsAndJoinsUseMembership() {
        final Dataset left = Dataset.rows(List.of("x"), new Object[][] { { "a" }, { "a" }, { "a" }, { "b" } });
        final Dataset right = Dataset.rows(List.of("x"), new Object[][] { { "a" }, { "b" }, { "b" } });
        assertEquals(List.of("a", "b"), left.intersectAll(right).getColumn("x"));
        assertEquals(List.of("a", "a"), left.exceptAll(right).getColumn("x"));
        assertEquals(left, left.semiJoin(right));
        assertTrue(left.antiJoin(right).isEmpty());
        assertEquals(List.of("a", "b"), left.union(right).getColumn("x"));
        assertEquals(7, left.unionAll(right).size());
        final Dataset narrow = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        assertThrows(IllegalArgumentException.class, () -> dataset().union(narrow));
        assertThrows(IllegalArgumentException.class, () -> dataset().intersect(narrow));
        assertThrows(IllegalArgumentException.class, () -> dataset().exceptAll(narrow));
        assertEquals(1, dataset().intersectBy(narrow, List.of("a")).size());
        final Dataset empty = Dataset.empty();
        assertTrue(empty.union(empty).isEmpty());
        assertTrue(empty.unionAll(empty).isEmpty());
        assertTrue(empty.intersect(empty).isEmpty());
        assertTrue(empty.intersectAll(empty).isEmpty());
        assertTrue(empty.except(empty).isEmpty());
        assertTrue(empty.exceptAll(empty).isEmpty());
    }

    @Test
    void setOperationsCompareCompleteTuplesAndDeepArrayKeys() {
        final Dataset left = Dataset.rows(List.of("a", "b"), new Object[][] { { new int[] { 1 }, null }, { new int[] { 1 }, null }, { new int[] { 1 }, "x" } });
        final Dataset right = Dataset.rows(List.of("b", "a"), new Object[][] { { null, new int[] { 1 } } });
        assertEquals(1, left.intersectAll(right).size());
        assertEquals(2, left.exceptAll(right).size());
        assertEquals(2, left.semiJoin(right).size());
        assertEquals(2, left.union(right).size());
        assertEquals(4, left.unionAll(right).size());
    }

    @Test
    void zeroColumnOperationsCannotSilentlyDiscardRows() {
        final Dataset data = Dataset.rows(List.of("x"), new Object[][] { { 1 }, { 2 } });
        assertThrows(IllegalArgumentException.class, () -> data.removeColumn("x"));
        assertThrows(IllegalArgumentException.class, () -> data.removeColumns(data.columnNames()));
        assertThrows(IllegalArgumentException.class, () -> data.slice(0, 1, List.of()));
        assertEquals(2, data.size());
        data.clear();
        data.removeColumn("x");
        assertThrows(IllegalArgumentException.class, () -> data.addRow(new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> data.addRow(null));
        assertThrows(IllegalArgumentException.class, () -> data.addRows(List.of(new Object[0], new Object[0])));
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(List.of("row"), List.of()).toDataset());
        assertThrows(IllegalArgumentException.class, () -> new Sheet<>(List.of(), List.of("column")).toTransposedDataset());
        assertEquals(0, data.slice(0, 0, List.of()).size());
    }

    @Test
    void conversionRangeFailuresRemainAtomicAndFractionalNarrowingIsExplicit() {
        for (Object bad : List.of(300, Double.NaN, Double.POSITIVE_INFINITY)) {
            final Dataset data = Dataset.rows(List.of("x"), new Object[][] { { 1 }, { bad } });
            final Dataset before = data.copy();
            assertThrows(ArithmeticException.class, () -> data.convertColumn("x", Byte.class));
            assertEquals(before, data);
        }
        final Dataset data = Dataset.rows(List.of("x"), new Object[][] { { 1.5 }, { -1.5 } });
        data.convertColumn("x", Byte.class);
        assertEquals(List.of((byte) 1, (byte) -1), data.getColumn("x"));
    }

    @Test
    void cubeProducesFirstResultWithoutMaterializingThePowerSet() {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            names.add("c" + i);
        }
        final Dataset data = Dataset.rows(names, new Object[0][]);
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertEquals(1, data.cube(names).limit(1).toList().size()));
        final Dataset small = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        assertEquals(
                List.of(List.of("a", "b", "c"), List.of("a", "b"), List.of("a", "c"), List.of("b", "c"), List.of("a"), List.of("b"), List.of("c"), List.of()),
                small.cube(small.columnNames()).map(d -> new ArrayList<>(d.columnNames().subList(0, d.columnCount() - 1))).toList());
    }
    // ------------------------------------------------------------------------------------------------------
    // Contract pins for the Dataset javadoc: each of these asserts behaviour the interface javadoc now states
    // explicitly, so the documented contract cannot silently drift back.
    // ------------------------------------------------------------------------------------------------------

    private static Dataset pinData() {
        return Dataset.rows(List.of("id", "name", "age"), new Object[][] { { 1, "Alice", 25 }, { 2, "Bob", 30 }, { 3, "Carol", 35 } });
    }

    /** G18-001: {@code updateAll}/{@code replaceIf}/{@code updateColumn} reject a null callback with IAE, before the frozen check. */
    @Test
    public void testUpdateAllAndReplaceIfRejectNullCallback() {
        assertThrows(IllegalArgumentException.class, () -> pinData().updateAll((Function<?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> pinData().updateAll((IntBiObjFunction<String, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> pinData().replaceIf((Predicate<?>) null, 9));
        assertThrows(IllegalArgumentException.class, () -> pinData().replaceIf((IntBiObjPredicate<String, ?>) null, 9));
        assertThrows(IllegalArgumentException.class, () -> pinData().updateColumn("name", null));

        // The null check precedes checkFrozen(), so the IAE wins over the documented IllegalStateException.
        final Dataset frozen = pinData();
        frozen.freeze();
        assertThrows(IllegalArgumentException.class, () -> frozen.updateAll((Function<?, ?>) null));
        assertThrows(IllegalStateException.class, () -> frozen.updateAll(v -> v));
    }

    /** G18-002: every cursor-relative accessor throws IndexOutOfBoundsException on a Dataset with no rows. */
    @Test
    public void testCursorRelativeAccessorsThrowWhenDatasetHasNoRows() {
        final Dataset empty = Dataset.rows(List.of("a", "b"), new Object[0][]);
        assertEquals(0, empty.size());
        assertEquals(2, empty.columnCount());
        assertEquals(0, empty.currentRowIndex());

        final List<Consumer<Dataset>> cursorReads = List.of(d -> d.get("a"), d -> d.get(0), d -> d.getBoolean("a"), d -> d.getBoolean(0),
                d -> d.getChar("a"), d -> d.getChar(0), d -> d.getByte("a"), d -> d.getByte(0), d -> d.getShort("a"), d -> d.getShort(0),
                d -> d.getInt("a"), d -> d.getInt(0), d -> d.getLong("a"), d -> d.getLong(0), d -> d.getFloat("a"), d -> d.getFloat(0),
                d -> d.getDouble("a"), d -> d.getDouble(0), d -> d.isNull("a"), d -> d.isNull(0), d -> d.set("a", 1), d -> d.set(0, 1));
        assertEquals(22, cursorReads.size());
        for (final Consumer<Dataset> read : cursorReads) {
            assertThrows(IndexOutOfBoundsException.class, () -> read.accept(empty));
        }

        // Same on a Dataset that becomes empty at runtime, and there is no legal moveToRow that repairs it.
        final Dataset emptied = pinData();
        emptied.clear();
        assertThrows(IndexOutOfBoundsException.class, () -> emptied.get("id"));
        assertThrows(IndexOutOfBoundsException.class, () -> emptied.moveToRow(0));
    }

    /** G18-003: the {@code getRow(int)} view is bound to the Dataset's current column list, not to the columns it was obtained with. */
    @Test
    public void testGetRowViewIsBoundToCurrentColumnList() {
        final Dataset data = pinData();
        final ImmutableList<Object> row = data.getRow(0);
        assertEquals(3, row.size());
        assertEquals(List.of(1, "Alice", 25), row);

        data.removeColumn("id");
        assertEquals(2, row.size());
        assertEquals("Alice", row.get(0));

        data.addColumn("zz", List.of(7, 8, 9));
        assertEquals(3, row.size());
        assertEquals(List.of("Alice", 25, 7), row);

        data.moveColumn("age", 0);
        assertEquals(List.of(25, "Alice", 7), row);

        // renameColumn changes no index, so the view is unaffected by it.
        data.renameColumn("age", "years");
        assertEquals(List.of(25, "Alice", 7), row);
    }

    /** G18-004: {@code columns()}, {@code rollup()} and {@code cube()} are fail-fast lazy sources; a cell write is not a structural change. */
    @Test
    public void testColumnsRollupAndCubeAreFailFastLazySources() {
        final Dataset forColumns = pinData();
        final Stream<ImmutableList<Object>> columns = forColumns.columns();
        forColumns.addColumn("x", List.of(7, 8, 9));
        assertThrows(ConcurrentModificationException.class, columns::count);

        final Dataset forRollup = pinData();
        final Stream<Dataset> rollup = forRollup.rollup(List.of("id", "name"));
        forRollup.addColumn("x", List.of(7, 8, 9));
        assertThrows(ConcurrentModificationException.class, rollup::count);

        final Dataset forCube = pinData();
        final Stream<Dataset> cube = forCube.cube(List.of("id", "name"));
        forCube.addColumn("x", List.of(7, 8, 9));
        assertThrows(ConcurrentModificationException.class, cube::count);

        // A cell write is not a structural modification: the stream stays valid and observes the new value.
        final Dataset written = pinData();
        final Stream<ImmutableList<Object>> live = written.columns();
        written.set(0, 0, 99);
        assertEquals(List.of(99, 2, 3), live.first().orElseThrow());
    }

    /** G18-005: removing rows clamps the {@code moveToRow} cursor; clearing resets it to 0; reordering leaves it alone. */
    @Test
    public void testCurrentRowIndexIsClampedByRowRemoval() {
        final Dataset data = Dataset.rows(List.of("id"), new Object[][] { { 0 }, { 1 }, { 2 }, { 3 }, { 4 }, { 5 }, { 6 }, { 7 } });
        data.moveToRow(5);
        assertEquals(5, data.currentRowIndex());
        assertEquals(5, (int) data.get("id"));

        data.removeRows(3, 6);
        assertEquals(5, data.size());
        assertEquals(4, data.currentRowIndex());
        assertEquals(7, (int) data.get("id"));

        data.clear();
        assertEquals(0, data.currentRowIndex());

        // Reordering leaves the index alone but changes which row it designates.
        final Dataset reordered = pinData();
        reordered.moveToRow(2);
        reordered.sortBy("id", Comparator.reverseOrder());
        assertEquals(2, reordered.currentRowIndex());
        assertEquals(1, (int) reordered.get("id"));
    }

    /** G18-006: {@code renameColumn} rejects a null or empty new name with IAE; an equal name is still the documented no-op. */
    @Test
    public void testRenameColumnRejectsNullOrEmptyNewName() {
        assertThrows(IllegalArgumentException.class, () -> pinData().renameColumn("id", null));
        assertThrows(IllegalArgumentException.class, () -> pinData().renameColumn("id", ""));
        assertThrows(IllegalArgumentException.class, () -> pinData().renameColumn("id", "name"));

        final Dataset noOp = pinData();
        noOp.renameColumn("id", "id");
        assertEquals(List.of("id", "name", "age"), noOp.columnNames());

        // checkFrozen() runs first here, so the documented IllegalStateException wins.
        final Dataset frozen = pinData();
        frozen.freeze();
        assertThrows(IllegalStateException.class, () -> frozen.renameColumn("id", null));
    }

    /** G18-007: all ten {@code addColumn} overloads reject a null or empty new column name with IAE. */
    @Test
    public void testAddColumnRejectsNullOrEmptyNewColumnName() {
        final List<BiConsumer<Dataset, String>> adders = List.of( //
                (d, n) -> d.addColumn(n, List.of(7, 8, 9)), //
                (d, n) -> d.addColumn(0, n, List.of(7, 8, 9)), //
                (d, n) -> d.addColumn(n, "age", v -> v), //
                (d, n) -> d.addColumn(0, n, "age", v -> v), //
                (d, n) -> d.addColumn(n, List.of("id", "age"), (DisposableObjArray a) -> a.get(0)), //
                (d, n) -> d.addColumn(0, n, List.of("id", "age"), (DisposableObjArray a) -> a.get(0)), //
                (d, n) -> d.addColumn(n, Tuple.of("id", "age"), (x, y) -> x), //
                (d, n) -> d.addColumn(0, n, Tuple.of("id", "age"), (x, y) -> x), //
                (d, n) -> d.addColumn(n, Tuple.of("id", "name", "age"), (x, y, z) -> x), //
                (d, n) -> d.addColumn(0, n, Tuple.of("id", "name", "age"), (x, y, z) -> x));
        assertEquals(10, adders.size());
        for (final BiConsumer<Dataset, String> adder : adders) {
            assertThrows(IllegalArgumentException.class, () -> adder.accept(pinData(), null));
            assertThrows(IllegalArgumentException.class, () -> adder.accept(pinData(), ""));
            assertThrows(IllegalArgumentException.class, () -> adder.accept(pinData(), "age"));
        }

        // checkFrozen() runs before the name check in addColumn, so the documented IllegalStateException wins.
        final Dataset frozen = pinData();
        frozen.freeze();
        assertThrows(IllegalStateException.class, () -> frozen.addColumn(null, List.of(7, 8, 9)));
    }

    /** G18-008: the Collection-based {@code combineColumns} overloads reject a null/empty new name and a null type/function. */
    @Test
    public void testCombineColumnsRejectsNullOrEmptyNewColumnNameAndNullCombiner() {
        final List<String> names = List.of("id", "name");
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, null, Map.class));
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, "", Map.class));
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, null, (DisposableObjArray a) -> a.get(0)));
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, "", (DisposableObjArray a) -> a.get(0)));
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, "combined", (Class<?>) null));
        assertThrows(IllegalArgumentException.class,
                () -> pinData().combineColumns(names, "combined", (Function<? super DisposableObjArray, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> pinData().combineColumns(names, "age", Map.class));

        final Dataset combined = pinData();
        combined.combineColumns(names, "combined", Map.class);
        assertEquals(List.of("combined", "age"), combined.columnNames());
    }

    /** G18-009: all ten {@code mapColumn}/{@code flatMapColumn} overloads reject a null mapper, a null/empty new name, and a copied name. */
    @Test
    public void testMapColumnFamilyRejectsInvalidNewColumnNameAndNullMapper() {
        final List<String> copying = List.of("id", "name");
        final List<BiConsumer<Dataset, String>> mappers = List.of( //
                (d, n) -> d.mapColumn("age", n, "id", v -> v), //
                (d, n) -> d.mapColumn("age", n, copying, v -> v), //
                (d, n) -> d.mapColumns(Tuple.of("id", "age"), n, copying, (x, y) -> x), //
                (d, n) -> d.mapColumns(Tuple.of("id", "name", "age"), n, copying, (x, y, z) -> x), //
                (d, n) -> d.mapColumns(List.of("id", "age"), n, copying, (DisposableObjArray a) -> a.get(0)), //
                (d, n) -> d.flatMapColumn("age", n, "id", v -> List.of(v)), //
                (d, n) -> d.flatMapColumn("age", n, copying, v -> List.of(v)), //
                (d, n) -> d.flatMapColumns(Tuple.of("id", "age"), n, copying, (x, y) -> List.of(x)), //
                (d, n) -> d.flatMapColumns(Tuple.of("id", "name", "age"), n, copying, (x, y, z) -> List.of(x)), //
                (d, n) -> d.flatMapColumns(List.of("id", "age"), n, copying, (DisposableObjArray a) -> List.of(a.get(0))));
        assertEquals(10, mappers.size());
        for (final BiConsumer<Dataset, String> mapper : mappers) {
            assertThrows(IllegalArgumentException.class, () -> mapper.accept(pinData(), null));
            assertThrows(IllegalArgumentException.class, () -> mapper.accept(pinData(), ""));
            // "id" is one of the copied columns, which is rejected even though the column exists.
            assertThrows(IllegalArgumentException.class, () -> mapper.accept(pinData(), "id"));
        }

        assertThrows(IllegalArgumentException.class, () -> pinData().mapColumn("age", "mapped", copying, null));
        assertThrows(IllegalArgumentException.class, () -> pinData().flatMapColumn("age", "mapped", copying, null));

        // The constraint is "not one of the COPIED columns", not "not an existing column".
        assertEquals(List.of("name", "age"), pinData().mapColumn("age", "age", List.of("name"), v -> v).columnNames());
    }

    /** G18-010: {@code removeColumns(Predicate)} rejects a null filter with IAE, ahead of the frozen check. */
    @Test
    public void testRemoveColumnsRejectsNullFilter() {
        assertThrows(IllegalArgumentException.class, () -> pinData().removeColumns((Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> pinData().removeColumns(c -> true));

        final Dataset frozen = pinData();
        frozen.freeze();
        assertThrows(IllegalArgumentException.class, () -> frozen.removeColumns((Predicate<String>) null));

        // The documented zero-row carve-out: removing every column is allowed when no rows remain.
        final Dataset noRows = Dataset.rows(List.of("a", "b"), new Object[0][]);
        noRows.removeColumns(c -> true);
        assertEquals(0, noRows.columnCount());
    }

    /** G18-011: both {@code distinctBy(.., Function)} overloads reject a null keyExtractor with IAE, ahead of the column check. */
    @Test
    public void testDistinctByRejectsNullKeyExtractor() {
        assertThrows(IllegalArgumentException.class, () -> pinData().distinctBy("id", null));
        assertThrows(IllegalArgumentException.class, () -> pinData().distinctBy(List.of("id", "name"), null));
        assertThrows(IllegalArgumentException.class, () -> pinData().distinctBy("nosuch", null));
        assertEquals(3, pinData().distinctBy("id", v -> v).size());

        // The two removeDuplicateRowsBy(.., Function) overloads are the @see siblings of these two and reject a
        // null keyExtractor identically, ahead of both the column check and checkFrozen().
        assertThrows(IllegalArgumentException.class, () -> pinData().removeDuplicateRowsBy("id", null));
        assertThrows(IllegalArgumentException.class, () -> pinData().removeDuplicateRowsBy(List.of("id", "name"), null));
        assertThrows(IllegalArgumentException.class, () -> pinData().removeDuplicateRowsBy("nosuch", null));
        final Dataset frozen = pinData();
        frozen.freeze();
        assertThrows(IllegalArgumentException.class, () -> frozen.removeDuplicateRowsBy("id", null));

        final Dataset deduped = pinData();
        deduped.removeDuplicateRowsBy("id", v -> v);
        assertEquals(3, deduped.size());
    }

    /** G18-012: {@code println(Appendable)} rejects a null output with IAE; {@code println(String)} accepts a null prefix. */
    @Test
    public void testPrintlnRejectsNullAppendable() {
        assertThrows(IllegalArgumentException.class, () -> pinData().println((Appendable) null));
        assertThrows(IllegalArgumentException.class, () -> Dataset.rows(List.of("a"), new Object[0][]).println((Appendable) null));

        final StringWriter writer = new StringWriter();
        pinData().println(writer);
        assertTrue(writer.toString().contains("Alice"));
    }

    /** G18-013: {@code getColumnIndexes} accepts a null or empty selection and answers an empty array, unlike the other selection methods. */
    @Test
    public void testGetColumnIndexesAcceptsNullOrEmptySelection() {
        final Dataset data = pinData();
        assertArrayEquals(new int[0], data.getColumnIndexes(null));
        assertArrayEquals(new int[0], data.getColumnIndexes(List.of()));
        assertArrayEquals(new int[0], Dataset.rows(List.of("a"), new Object[0][]).getColumnIndexes(null));

        // A repeated name still resolves repeatedly, in the order given; an unknown name is still rejected.
        assertArrayEquals(new int[] { 0, 0, 2 }, data.getColumnIndexes(List.of("id", "id", "age")));
        assertArrayEquals(new int[] { 2, 0 }, data.getColumnIndexes(List.of("age", "id")));
        assertThrows(IllegalArgumentException.class, () -> data.getColumnIndexes(List.of("nosuch")));

        // Contrast: a selection-taking method rejects the same null.
        assertThrows(IllegalArgumentException.class, () -> data.copy(null));
    }
}
