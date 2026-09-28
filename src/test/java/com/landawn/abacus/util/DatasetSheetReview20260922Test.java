package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

/**
 * Regressions for the 2026-09-22 Dataset/RowDataset/Sheet review (ledger
 * {@code scripts/cross_review/Dataset_RowDataset_Sheet_ledger_2026-09-22.md}, C-021..).
 */
public class DatasetSheetReview20260922Test extends TestBase {

    private static final String EMOJI = "😀";

    private static Dataset dupRows() {
        // three distinct (a, b) keys over four rows, one of them repeated; a null and a supplementary character
        return Dataset.rows(List.of("a", "b", "v"),
                new Object[][] { { 1, "x", 10 }, { null, EMOJI, 20 }, { 1, "x", 30 }, { 3, "z", 40 } });
    }

    // ---- C-021: a keyExtractor returning its reused argument ----------------------------------------------

    @Test
    public void c021_distinctBy_jdkIdentityAndLambdaKeyByRowContent() {
        final List<String> keys = List.of("a", "b");
        final Function<DisposableObjArray, Object> lambda = r -> r;

        assertEquals(3, dupRows().distinctBy(keys, Fn.identity()).size());
        assertEquals(3, dupRows().distinctBy(keys, Function.identity()).size());
        assertEquals(3, dupRows().distinctBy(keys, lambda).size());
        // the backing array returned through apply(..) is snapshotted too
        assertEquals(3, dupRows().distinctBy(keys, r -> r.apply(arr -> arr)).size());

        final Dataset distinct = dupRows().distinctBy(keys, Function.identity());
        assertEquals(Arrays.asList(1, null, 3), distinct.getColumn("a"));
        assertEquals(Arrays.asList(10, 20, 40), distinct.getColumn("v"));
    }

    @Test
    public void c021_backingArrayKeysWithCollidingHashesStayDistinct() {
        // "Aa" and "BB" have the same hashCode; a stored key that aliases the reused array made them equal
        final Dataset ds = Dataset.rows(List.of("s"), new Object[][] { { "Aa" }, { "BB" }, { "Aa" } });

        assertEquals(2, ds.distinctBy(List.of("s"), r -> r.apply(arr -> arr)).size());
        assertEquals(2, ds.groupBy(List.of("s"), r -> r.apply(arr -> arr)).size());
    }

    @Test
    public void c021_removeDuplicateRowsBy_jdkIdentityNoLongerDeletesDistinctRows() {
        final Dataset ds = dupRows();

        ds.removeDuplicateRowsBy(List.of("a", "b"), Function.identity());

        assertEquals(3, ds.size());
        assertEquals(Arrays.asList(10, 20, 40), ds.getColumn("v"));

        final Dataset lambda = dupRows();
        lambda.removeDuplicateRowsBy(List.of("a", "b"), (Function<DisposableObjArray, Object>) r -> r);
        assertEquals(3, lambda.size());
    }

    @Test
    public void c021_groupByOverloads_jdkIdentity() {
        final List<String> keys = List.of("a", "b");

        assertEquals(3, dupRows().groupBy(keys, Function.identity()).size());

        final Dataset summed = dupRows().groupBy(keys, Function.identity(), "v", "sum", Collectors.summingInt(o -> (Integer) o));
        assertEquals(Arrays.asList(40, 20, 40), summed.getColumn("sum"));

        final Dataset listed = dupRows().groupBy(keys, Function.identity(), List.of("v"), "rows", Object[].class);
        assertEquals(3, listed.size());
        assertEquals(2, ((List<?>) listed.get(0, 2)).size());

        final Dataset mapped = dupRows().groupBy(keys, Function.identity(), List.of("v"), "vs", r -> r.get(0), Collectors.toList());
        assertEquals(Arrays.asList(Arrays.asList(10, 30), Arrays.asList(20), Arrays.asList(40)), mapped.getColumn("vs"));
    }

    @Test
    public void c021_rollupAndCube_jdkIdentity() {
        final List<Dataset> rollup = dupRows().rollup(List.of("a", "b"), Function.identity()).toList();
        assertEquals(List.of(3, 3, 1), rollup.stream().map(Dataset::size).toList());

        final List<Dataset> cube = dupRows().cube(List.of("a", "b"), Function.identity()).toList();
        assertEquals(List.of(3, 3, 3, 1), cube.stream().map(Dataset::size).toList());
    }

    @Test
    public void c021_emptyDataset() {
        final Dataset empty = Dataset.rows(List.of("a", "b"), new Object[0][]);

        assertEquals(0, empty.distinctBy(List.of("a", "b"), Function.identity()).size());
        assertEquals(0, empty.groupBy(List.of("a", "b"), Function.identity()).size());
        empty.removeDuplicateRowsBy(List.of("a", "b"), Function.identity());
        assertEquals(0, empty.size());
    }

    // ---- C-022: file exports -------------------------------------------------------------------------------

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    @Test
    public void c022_exportFollowsASymbolicLinkAndKeepsIt(@TempDir final Path dir) throws Exception {
        final Path target = dir.resolve("target.csv");
        Files.writeString(target, "OLD");
        final Path link = dir.resolve("link.csv");

        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (final Exception e) {
            assumeTrue(false, "symbolic links are not available: " + e);
        }

        Dataset.rows(List.of("id"), new Object[][] { { 1 } }).toCsv(link.toFile());

        assertTrue(Files.isSymbolicLink(link));
        assertEquals("\"id\"\n1", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    public void c022_readOnlyDestinationIsRefusedAndKept(@TempDir final Path dir) throws Exception {
        final File file = dir.resolve("ro.json").toFile();
        Files.writeString(file.toPath(), "OLD");
        assumeTrue(file.setWritable(false) && !Files.isWritable(file.toPath()), "cannot make a file read-only here (e.g. running as root)");

        try {
            final Dataset ds = Dataset.rows(List.of("id"), new Object[][] { { 1 } });
            assertThrows(UncheckedIOException.class, () -> ds.toJson(file));
            assertThrows(UncheckedIOException.class, () -> ds.toXml(file));
            assertThrows(UncheckedIOException.class, () -> ds.toCsv(file));
            assertEquals("OLD", Files.readString(file.toPath()));
            try (var files = Files.list(dir)) {
                assertEquals(1, files.count()); // no temporary sibling left behind
            }
        } finally {
            file.setWritable(true);
        }
    }

    @Test
    public void c022_newExportGetsDefaultPermissionsAndReplacementKeepsTheOldOnes(@TempDir final Path dir) throws Exception {
        assumeTrue(posix());

        final File reference = dir.resolve("reference.txt").toFile();
        try (Writer w = new FileWriter(reference)) {
            w.write("x");
        }

        final Dataset ds = Dataset.rows(List.of("id", "name"), new Object[][] { { 1, "é" + EMOJI } });
        final File fresh = dir.resolve("fresh.csv").toFile();
        ds.toCsv(fresh);
        assertEquals(Files.getPosixFilePermissions(reference.toPath()), Files.getPosixFilePermissions(fresh.toPath()));

        final Path existing = dir.resolve("existing.json");
        Files.writeString(existing, "OLD");
        final Set<PosixFilePermission> groupWritable = PosixFilePermissions.fromString("rw-rw-r--");
        Files.setPosixFilePermissions(existing, groupWritable);
        ds.toJson(existing.toFile());
        assertEquals(groupWritable, Files.getPosixFilePermissions(existing));
        assertTrue(Files.readString(existing, StandardCharsets.UTF_8).contains(EMOJI));
    }

    // ---- C-023: columnNames() is a fail-fast live view -------------------------------------------------------

    @Test
    public void c023_removingColumnsWhileIteratingColumnNamesFailsFast() {
        final Dataset ds = Dataset.rows(List.of("tmp1", "tmp2", "x", "tmp3", "y"), new Object[][] { { 1, 2, 3, 4, 5 } });

        assertThrows(ConcurrentModificationException.class, () -> {
            for (final String c : ds.columnNames()) {
                if (c.startsWith("tmp")) {
                    ds.removeColumn(c);
                }
            }
        });

        final Dataset ds2 = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 2 } });
        assertThrows(ConcurrentModificationException.class, () -> {
            for (final String c : ds2.columnNames()) {
                ds2.addColumn(c + "_x", List.of(0));
            }
        });

        final Dataset ds3 = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        assertThrows(ConcurrentModificationException.class, () -> ds3.columnNames().forEach(c -> {
            if ("a".equals(c)) {
                ds3.removeColumn("c");
            }
        }));

        final Dataset ds4 = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        assertThrows(ConcurrentModificationException.class, () -> ds4.columnNames().stream().forEach(c -> {
            if ("a".equals(c)) {
                ds4.removeColumn("c");
            }
        }));
    }

    @Test
    public void c023_renamingAndSwappingDuringIterationStayLegalAndTheViewIsLive() {
        final Dataset ds = Dataset.rows(List.of("a", "b", EMOJI), new Object[][] { { 1, 2, 3 } });
        final ImmutableList<String> names = ds.columnNames();
        final List<String> seen = new ArrayList<>();

        for (final String c : names) {
            seen.add(c);
            if (seen.size() == 1) {
                ds.renameColumn("b", "B");
                ds.swapColumns("a", EMOJI);
            }
        }

        assertEquals(List.of("a", "B", "a"), seen);
        assertEquals(List.of(EMOJI, "B", "a"), names);

        ds.addColumn("c", List.of(4));
        assertEquals(4, names.size());
        assertEquals("c", names.get(3));
        assertTrue(Arrays.equals(new int[] { 0, 1, 2, 3 }, ds.getColumnIndexes(ds.columnNames())));
    }

    @Test
    public void c023_subListIsReadOnlyAndFailFast() {
        final Dataset ds = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        final List<String> sub = ds.columnNames().subList(0, 2);

        assertEquals(List.of("a", "b"), sub);
        assertThrows(UnsupportedOperationException.class, () -> sub.set(0, "z"));

        ds.removeColumn("a");
        assertThrows(ConcurrentModificationException.class, () -> sub.get(0));
    }

    // ---- C-024 / C-029: forEach -----------------------------------------------------------------------------

    @Test
    public void c024_forEachFailsFastOnStructuralChangesButAllowsCellWrites() {
        final Dataset added = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" } });
        assertThrows(ConcurrentModificationException.class, () -> added.forEach(r -> added.addRow(new Object[] { 9, "z" })));

        final Dataset removed = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" } });
        assertThrows(ConcurrentModificationException.class, () -> removed.forEach(Tuple.of("a", "b"), (x, y) -> removed.removeRow(0)));

        final Dataset sorted = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 2, "x", 0 }, { 1, "y", 0 } });
        assertThrows(ConcurrentModificationException.class,
                () -> sorted.forEach(Tuple.of("a", "b", "c"), (x, y, z) -> sorted.sortBy("a")));

        final Dataset reverse = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 } });
        assertThrows(ConcurrentModificationException.class,
                () -> reverse.forEach(reverse.size() - 1, -1, r -> reverse.addRow(new Object[] { 3 })));

        final Dataset written = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 }, { 3 } });
        final List<Object> visited = new ArrayList<>();
        written.forEach(r -> {
            visited.add(r.get(0));
            written.updateColumn("a", v -> (Integer) v * 10);
        });
        assertEquals(List.of(1, 20, 300), visited);
    }

    @Test
    public void c029_reverseIdiomOnAnEmptyDatasetDoesNothing() {
        final Dataset empty = Dataset.rows(List.of("a", "b", "c"), new Object[0][]);

        empty.forEach(empty.size() - 1, -1, r -> fail());
        empty.forEach(empty.size() - 1, -1, List.of("a"), r -> fail());
        empty.forEach(empty.size() - 1, -1, Tuple.of("a", "b"), (x, y) -> fail());
        empty.forEach(empty.size() - 1, -1, Tuple.of("a", "b", "c"), (x, y, z) -> fail());

        final Dataset one = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        // only the empty Dataset makes (-1, -1) the reverse idiom; elsewhere it is still an invalid range
        assertThrows(IndexOutOfBoundsException.class, () -> one.forEach(-1, -1, r -> fail()));
        assertThrows(IndexOutOfBoundsException.class, () -> one.forEach(-2, -1, r -> fail()));
        assertThrows(IndexOutOfBoundsException.class, () -> one.forEach(-1, 0, r -> fail()));
    }

    // ---- C-026: explicit prefix mapping wins --------------------------------------------------------------

    public static class Address26 {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class Customer26 {
        private String address;
        private Address26 addressInfo;
        private Address26 home;

        public String getAddress() {
            return address;
        }

        public void setAddress(final String address) {
            this.address = address;
        }

        public Address26 getAddressInfo() {
            return addressInfo;
        }

        public void setAddressInfo(final Address26 addressInfo) {
            this.addressInfo = addressInfo;
        }

        public Address26 getHome() {
            return home;
        }

        public void setHome(final Address26 home) {
            this.home = home;
        }
    }

    @Test
    public void c026_explicitPrefixMappingWinsOverASameNamedProperty() {
        final Dataset ds = Dataset.rows(List.of("address.city"), new Object[][] { { "Zürich" } });

        final Customer26 c = ds.<Customer26> toEntities(Map.of("address", "addressInfo"), Customer26.class).get(0);
        assertNull(c.getAddress());
        assertEquals("Zürich", c.getAddressInfo().getCity());

        // bean-typed same-named property: the data used to land in "home" silently
        final Dataset homes = Dataset.rows(List.of("home.city"), new Object[][] { { "Oslo" } });
        final Customer26 h = homes.<Customer26> toEntities(Map.of("home", "addressInfo"), Customer26.class).get(0);
        assertNull(h.getHome());
        assertEquals("Oslo", h.getAddressInfo().getCity());

        // a mapping to a name that does not resolve still falls back to the property of the prefix's own name
        final Customer26 f = homes.<Customer26> toEntities(Map.of("home", "noSuchProperty"), Customer26.class).get(0);
        assertEquals("Oslo", f.getHome().getCity());
    }

    // ---- C-028: registered row factory returning null ----------------------------------------------------

    @Test
    public void c028_registeredRowFactoryReturningNullIsANullPointer() {
        // A registrable (non-built-in) collection type that no other test uses: the registration is JVM-global.
        IntFunctions.registerForCollection(org.apache.commons.collections4.list.CursorableLinkedList.class, n -> null);
        final Dataset ds = Dataset.rows(List.of("k", "v"), new Object[][] { { 1, "a" } });

        assertThrows(NullPointerException.class, () -> ds.toMap("k", List.of("v"), org.apache.commons.collections4.list.CursorableLinkedList.class));
        assertThrows(NullPointerException.class, () -> ds.toMultimap("k", List.of("v"), org.apache.commons.collections4.list.CursorableLinkedList.class));
        // an empty range never builds a row
        assertTrue(ds.toMap(0, 0, "k", List.of("v"), org.apache.commons.collections4.list.CursorableLinkedList.class, IntFunctions.ofMap()).isEmpty());
    }

    // ---- C-031: toCsv(Writer) null output ----------------------------------------------------------------

    @Test
    public void c031_toCsvRejectsANullWriterEvenWhenThereIsNothingToWrite() {
        assertThrows(IllegalArgumentException.class, () -> Dataset.empty().toCsv((Writer) null));

        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        assertThrows(IllegalArgumentException.class, () -> ds.toCsv(0, 1, List.of(), (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> ds.toCsv(0, 0, null, (Writer) null));

        final StringWriter out = new StringWriter();
        ds.toCsv(0, 1, List.of(), out);
        assertEquals("", out.toString());
    }

    // ---- C-033: grand total of an empty Dataset -----------------------------------------------------------

    private static Dataset last(final List<Dataset> levels) {
        return levels.get(levels.size() - 1);
    }

    @Test
    public void c033_grandTotalOfAnEmptyDatasetHasOneRow() {
        final Dataset empty = Dataset.rows(List.of("a", "b", "v"), new Object[0][]);

        final List<Dataset> counts = empty.rollup(List.of("a", "b")).toList();
        assertEquals(List.of(0, 0, 1), counts.stream().map(Dataset::size).toList());
        assertEquals(List.of("count"), last(counts).columnNames());
        assertEquals(0, (Integer) last(counts).get(0, 0));

        final Dataset sum = last(empty.rollup(List.of("a"), "v", "total", Collectors.summingInt(o -> (Integer) o)).toList());
        assertEquals(0, (Integer) sum.get(0, 0));

        final Dataset rows = last(empty.cube(List.of("a"), List.of("v", "b"), "rows", Object[].class).toList());
        assertEquals(1, rows.size());
        assertEquals(List.of(), rows.get(0, 0));

        final Dataset mapped = last(empty.cube(List.of("a", "b"), Function.identity(), List.of("v"), "vs", r -> r.get(0), Collectors.toList()).toList());
        assertEquals(1, mapped.size());
        assertEquals(List.of(), mapped.get(0, 0));
    }

    @Test
    public void c033_grandTotalOfANonEmptyDatasetIsUnchanged() {
        final Dataset total = last(dupRows().rollup(List.of("a", "b")).toList());
        assertEquals(1, total.size());
        assertEquals(4, (Integer) total.get(0, 0));

        final Dataset sum = last(dupRows().cube(List.of("a"), "v", "total", Collectors.summingInt(o -> (Integer) o)).toList());
        assertEquals(100, (Integer) sum.get(0, 0));
    }

    // ---- C-045: convertColumn range rules as documented -----------------------------------------------------

    @Test
    public void c045_floatingTargetsSaturateIntegralTargetsThrow() {
        final Dataset ds = Dataset.rows(List.of("d", "l"), new Object[][] { { 1e300, Long.MAX_VALUE } });

        ds.convertColumn("d", Float.class);
        assertEquals((Object) Float.POSITIVE_INFINITY, ds.get(0, 0));

        assertThrows(ArithmeticException.class, () -> ds.convertColumn("l", Integer.class));
        assertEquals(Long.MAX_VALUE, (Long) ds.get(0, 1));
    }

    // ---- Sheet: C-025, C-034, C-035, C-036 ----------------------------------------------------------------

    @Test
    public void c025_forEachMajorReportsCellsWrittenByTheActionOnAnUninitializedSheet() {
        final Sheet<String, String, Integer> rowMajor = new Sheet<>(List.of("r1", "r2"), List.of("c1", "c2"));
        final List<String> seen = new ArrayList<>();
        rowMajor.forEachRowMajor((r, c, v) -> {
            seen.add(r + c + "=" + v);
            if ("r1".equals(r) && "c1".equals(c)) {
                rowMajor.set("r1", "c2", 7);
            }
        });
        assertEquals(List.of("r1c1=null", "r1c2=7", "r2c1=null", "r2c2=null"), seen);

        final Sheet<String, String, Integer> columnMajor = new Sheet<>(List.of("r1", "r2"), List.of("c1"));
        final List<String> seen2 = new ArrayList<>();
        columnMajor.forEachColumnMajor((r, c, v) -> {
            seen2.add(r + c + "=" + v);
            if ("r1".equals(r)) {
                columnMajor.set("r2", "c1", 5);
            }
        });
        assertEquals(List.of("r1c1=null", "r2c1=5"), seen2);

        final List<String> none = new ArrayList<>();
        new Sheet<String, String, Integer>().forEachRowMajor((r, c, v) -> none.add(r));
        assertTrue(none.isEmpty());
    }

    @Test
    public void c034_putAllNamesOnlyTheMissingKeys() {
        final List<String> rows = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            rows.add("r" + i);
        }
        final Sheet<String, String, Integer> target = new Sheet<>(rows, List.of("c"));
        final List<String> sourceRows = new ArrayList<>(rows);
        sourceRows.add("MISSING" + EMOJI);
        final Sheet<String, String, Integer> source = new Sheet<>(sourceRows, List.of("c"));

        final String message = assertThrows(IllegalArgumentException.class, () -> target.putAll(source)).getMessage();
        assertTrue(message.startsWith("[MISSING" + EMOJI + "] are not all included"), message);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> target.putAll(source, (a, b) -> a)).getMessage().startsWith("[MISSING" + EMOJI + "]"));

        final Sheet<String, String, Integer> wideSource = new Sheet<>(List.of("r0"), List.of("c", "extra"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> target.putAll(wideSource)).getMessage().startsWith("[extra]"));
    }

    @Test
    public void c035_renamingARowOrColumnToItsOwnKeyIsANoOp() {
        final Sheet<String, String, Integer> sheet = Sheet.rows(List.of("r1", "r2"), List.of("c1", "c2"), new Integer[][] { { 1, 2 }, { 3, 4 } });

        sheet.renameRow("r1", "r1");
        sheet.renameColumn("c2", "c2");

        assertEquals(List.of("r1", "r2"), new ArrayList<>(sheet.rowKeySet()));
        assertEquals(List.of("c1", "c2"), new ArrayList<>(sheet.columnKeySet()));
        assertEquals(2, sheet.get("r1", "c2"));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("r1", "r2"));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("c1", "c2"));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("r1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("nope", "nope"));

        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.renameRow("r1", "r1"));
    }

    @Test
    public void c036_toStringListsEveryColumnAndRendersArrayKeys() {
        final Sheet<String, String, Integer> uninitialized = new Sheet<>(List.of("r1", "r2"), List.of("c1", EMOJI));
        final Sheet<String, String, Integer> initialized = Sheet.rows(List.of("r1", "r2"), List.of("c1", EMOJI), new Integer[][] { { null, null }, { null, null } });

        assertEquals(initialized, uninitialized);
        assertEquals(initialized.toString(), uninitialized.toString());
        assertEquals("{rowKeySet=[r1, r2], columnKeySet=[c1, " + EMOJI + "], columns={c1=[null, null], " + EMOJI + "=[null, null]}}", uninitialized.toString());
        assertEquals("{rowKeySet=[], columnKeySet=[], columns={}}", new Sheet<>().toString());

        final Sheet<int[], String, Integer> arrays = Sheet.rows(List.of(new int[] { 1, 2 }), List.of("c"), new Integer[][] { { 3 } });
        assertEquals("{rowKeySet=[[1, 2]], columnKeySet=[c], columns={c=[3]}}", arrays.toString());
        assertFalse(arrays.toString().contains("[I@"));
    }

    // ==== cycle 2 ============================================================================================

    @Test
    public void c055_grandTotalOfAnEmptyDatasetIsNullForACollectorWithoutAnEmptyResult() {
        final Dataset empty = Dataset.rows(List.of("k", "v"), new Object[0][]);

        final Dataset max = last(empty.rollup(List.of("k"), "v", "m", com.landawn.abacus.util.stream.Collectors.maxOrElseThrow()).toList());
        assertEquals(1, max.size());
        assertNull(max.get(0, 0));

        final Dataset jdk = last(empty.cube(List.of("k"), "v", "m",
                Collectors.collectingAndThen(Collectors.maxBy((x, y) -> ((Integer) x).compareTo((Integer) y)), java.util.Optional::get)).toList());
        assertNull(jdk.get(0, 0));

        // any other exception still propagates: the caller asked for it explicitly
        assertThrows(IllegalStateException.class, () -> empty.rollup(List.of("k"), "v", "m",
                com.landawn.abacus.util.stream.Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("empty")))
                .toList());

        // non-empty input is unaffected
        final Dataset some = Dataset.rows(List.of("k", "v"), new Object[][] { { "a", 3 }, { "b", 5 } });
        assertEquals(5, (Integer) last(some.rollup(List.of("k"), "v", "m", com.landawn.abacus.util.stream.Collectors.maxOrElseThrow()).toList()).get(0, 0));
    }

    @Test
    public void c057_columnNamesTraversalFailsFastEvenWhenRemovingTheSecondToLastColumn() {
        final Dataset ds = Dataset.rows(List.of("x", "tmp1", EMOJI), new Object[][] { { 1, 2, 3 } });
        final List<String> seen = new ArrayList<>();

        assertThrows(ConcurrentModificationException.class, () -> {
            for (final String c : ds.columnNames()) {
                seen.add(c);
                if ("tmp1".equals(c)) {
                    ds.removeColumn(c);
                }
            }
        });
        assertEquals(List.of("x", "tmp1"), seen);

        // a stream action must not see shifted names (or null) before the failure
        final Dataset streamed = Dataset.rows(List.of("tmp1", "x", "tmp2", "y"), new Object[][] { { 1, 2, 3, 4 } });
        final List<String> streamedSeen = new ArrayList<>();
        assertThrows(ConcurrentModificationException.class, () -> streamed.columnNames().stream().forEach(c -> {
            streamedSeen.add(c);
            if ("tmp1".equals(c)) {
                streamed.removeColumn(c);
            }
        }));
        assertEquals(List.of("tmp1"), streamedSeen);

        // backwards traversal, and an empty Dataset
        final var it = ds.columnNames().listIterator(ds.columnCount());
        assertEquals(EMOJI, it.previous());
        ds.addColumn("z", List.of(0));
        assertThrows(ConcurrentModificationException.class, it::hasPrevious);
        assertFalse(Dataset.empty().columnNames().iterator().hasNext());
        assertEquals(0, Dataset.empty().columnNames().stream().count());
    }

    @Test
    public void c058_addColumnAndCombineColumnsFailFastAndLeaveTheDatasetConsistent() {
        final List<java.util.function.Consumer<Dataset>> calls = List.of( //
                ds -> ds.addColumn("n", "a", v -> grow(ds)), //
                ds -> ds.addColumn(0, "n", List.of("a", "b"), r -> grow(ds)), //
                ds -> ds.addColumn("n", Tuple.of("a", "b"), (x, y) -> grow(ds)), //
                ds -> ds.addColumn("n", Tuple.of("a", "b", "c"), (x, y, z) -> grow(ds)), //
                ds -> ds.combineColumns(List.of("a", "b"), "n", r -> grow(ds)), //
                ds -> ds.combineColumns(Tuple.of("a", "b"), "n", (x, y) -> grow(ds)));

        for (final var call : calls) {
            final Dataset ds = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, "x", 0 }, { 2, "y", 0 } });
            assertThrows(ConcurrentModificationException.class, () -> call.accept(ds));
            for (final String name : ds.columnNames()) {
                assertEquals(ds.size(), ds.getColumn(name).size(), name);
            }
            assertEquals(ds.size(), ds.toList().size());
        }
    }

    private static Object grow(final Dataset ds) {
        if (ds.size() < 3) {
            ds.addRow(new Object[] { 9, "z", 0 });
        }
        return 0;
    }

    @Test
    public void c056_exportToADirectoryFailsBeforeSerializing(@TempDir final Path dir) throws Exception {
        final Path target = Files.createDirectory(dir.resolve("out.csv"));
        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { 1 } });

        assertThrows(UncheckedIOException.class, () -> ds.toCsv(target.toFile()));
        assertThrows(UncheckedIOException.class, () -> ds.toJson(target.toFile()));
        assertTrue(Files.isDirectory(target));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count()); // no temporary sibling
        }
    }

    @Test
    public void c056_exportToAFifoOrDeviceWritesInPlace(@TempDir final Path dir) throws Exception {
        assumeTrue(posix());
        final Dataset ds = Dataset.rows(List.of("id", "name"), new Object[][] { { 1, EMOJI } });

        ds.toJson(new File("/dev/null"));

        final Path fifo = dir.resolve("pipe");
        final Process mkfifo;
        try {
            mkfifo = new ProcessBuilder("mkfifo", fifo.toString()).start();
        } catch (final Exception e) {
            assumeTrue(false, "mkfifo not available: " + e);
            return;
        }
        assumeTrue(mkfifo.waitFor() == 0, "mkfifo failed");

        final String[] read = new String[1];
        final Thread reader = new Thread(() -> {
            try {
                read[0] = Files.readString(fifo, StandardCharsets.UTF_8);
            } catch (final Exception e) {
                read[0] = e.toString();
            }
        });
        reader.setDaemon(true);
        reader.start();

        ds.toCsv(fifo.toFile());
        reader.join(10_000);

        assertEquals("\"id\",\"name\"\n1,\"" + EMOJI + "\"", read[0]);
        assertFalse(Files.isRegularFile(fifo)); // still the FIFO, not replaced by a regular file
    }

    @Test
    public void c062_paginatePagesSurviveColumnChangesButNotRowChanges() {
        final Dataset ds = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });
        final Paginated<Dataset> pages = ds.paginate(2);
        final Dataset page0 = pages.getPage(0);

        ds.addColumn("c", List.of(7, 8, 9));
        assertEquals(List.of("a", "b"), page0.columnNames());
        assertEquals(2, page0.size());
        assertThrows(ConcurrentModificationException.class, () -> pages.getPage(1));

        final Dataset ds2 = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 } });
        final Dataset page = ds2.paginate(1).getPage(0);
        ds2.removeRow(0);
        assertThrows(ConcurrentModificationException.class, page::size);
    }

    @Test
    public void c060_addRowTakesBackABeanProducedByGetRow() {
        final Dataset ds = Dataset.rows(List.of("address", "addressInfo.city"), new Object[][] { { "Main St", "Zürich" + EMOJI } });
        final Customer26 bean = ds.getRow(0, Customer26.class);
        assertEquals("Zürich" + EMOJI, bean.getAddressInfo().getCity());

        final Dataset copy = Dataset.rows(List.of("address", "addressInfo.city"), new Object[0][]);
        copy.addRow(bean);
        assertEquals(ds, copy);

        final Customer26 noInfo = new Customer26();
        noInfo.setAddress("x");
        copy.addRow(noInfo); // a null link yields null
        assertNull(copy.get(1, 1));

        final Dataset unknown = Dataset.rows(List.of("addressInfo.zip"), new Object[0][]);
        assertThrows(IllegalArgumentException.class, () -> unknown.addRow(bean));
    }

    @Test
    public void c061_csvByteSinksRejectAnUnpairedSurrogateInsteadOfWritingAQuestionMark() {
        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { "x\uD800y" } });

        final StringWriter chars = new StringWriter();
        ds.toCsv(chars);
        assertTrue(chars.toString().contains("x\uD800y"));

        assertThrows(UncheckedIOException.class, () -> ds.toCsv(new java.io.ByteArrayOutputStream()));

        final java.io.ByteArrayOutputStream ok = new java.io.ByteArrayOutputStream();
        Dataset.rows(List.of("a"), new Object[][] { { EMOJI } }).toCsv(ok);
        assertEquals("\"a\"\n\"" + EMOJI + "\"", ok.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void c064_toCsvReportsABadColumnBeforeANullWriterLikeToJson() {
        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { 1 } });

        final String csv = assertThrows(IllegalArgumentException.class, () -> ds.toCsv(0, 1, List.of("nope"), (Writer) null)).getMessage();
        final String json = assertThrows(IllegalArgumentException.class, () -> ds.toJson(0, 1, List.of("nope"), (Writer) null)).getMessage();
        assertTrue(csv.contains("nope"), csv);
        assertTrue(json.contains("nope"), json);
    }

    // ==== cycle 3 ============================================================================================

    @Test
    public void c066_divideColumnFailsFastOnAStructuralChangeFromTheCallback() {
        final List<java.util.function.BiConsumer<Dataset, Runnable>> calls = List.of( //
                (ds, change) -> ds.divideColumn("v", List.of("v1", "v2"), x -> {
                    change.run();
                    return List.of(x, x);
                }), //
                (ds, change) -> ds.divideColumn("v", List.of("v1", "v2"), (Object x, Object[] out) -> change.run()), //
                (ds, change) -> ds.divideColumn("v", Tuple.of("v1", "v2"), (Object x, Pair<Object, Object> out) -> change.run()), //
                (ds, change) -> ds.divideColumn("v", Tuple.of("v1", "v2", "v3"), (Object x, Triple<Object, Object, Object> out) -> change.run()));

        for (final var call : calls) {
            // a column inserted before the divided one used to make it replace the wrong column ("id" was lost)
            final Dataset added = Dataset.rows(List.of("id", "v"), new Object[][] { { 1, "a" }, { 2, "b" }, { 3, "c" } });
            assertThrows(ConcurrentModificationException.class,
                    () -> call.accept(added, () -> {
                        if (!added.containsColumn("z")) {
                            added.addColumn(0, "z", Arrays.asList(new Object[added.size()]));
                        }
                    }));
            assertEquals(List.of("z", "id", "v"), added.columnNames());

            // a removed row used to shift the new columns by one row
            final Dataset removed = Dataset.rows(List.of("id", "v"), new Object[][] { { 1, "a" }, { 2, "b" }, { 3, EMOJI } });
            assertThrows(ConcurrentModificationException.class, () -> call.accept(removed, () -> {
                if (removed.size() == 3) {
                    removed.removeRow(0);
                }
            }));
            assertEquals(List.of("id", "v"), removed.columnNames());
            assertEquals(List.of(2, 3), removed.getColumn("id"));
        }
    }

    @Test
    public void c066_keyExtractorDedupFailsFastOnAStructuralChange() {
        final Dataset single = Dataset.rows(List.of("k", "v"), new Object[][] { { 1, "a" }, { 1, "b" } });
        assertThrows(ConcurrentModificationException.class, () -> single.removeDuplicateRowsBy("k", k -> {
            if (!single.containsColumn("z")) {
                single.addColumn("z", List.of(0, 0));
            }
            return k;
        }));
        for (final String name : single.columnNames()) {
            assertEquals(single.size(), single.getColumn(name).size(), name);
        }

        final Dataset multi = Dataset.rows(List.of("k", "v"), new Object[][] { { 1, "a" }, { 1, "b" } });
        assertThrows(ConcurrentModificationException.class, () -> multi.removeDuplicateRowsBy(List.of("k", "v"), r -> {
            if (!multi.containsColumn("z")) {
                multi.addColumn("z", List.of(0, 0));
            }
            return r.get(0);
        }));
        for (final String name : multi.columnNames()) {
            assertEquals(multi.size(), multi.getColumn(name).size(), name);
        }

        final Dataset distinct = Dataset.rows(List.of("k"), new Object[][] { { 1 }, { 2 } });
        assertThrows(ConcurrentModificationException.class, () -> distinct.distinctBy("k", k -> {
            distinct.addRow(new Object[] { 3 });
            return k;
        }));
        assertThrows(ConcurrentModificationException.class,
                () -> distinct.distinctBy(List.of("k"), r -> {
                    distinct.addRow(new Object[] { 4 });
                    return r.get(0);
                }));

        // a key extractor that only reads (or writes cells) is unaffected
        final Dataset ok = Dataset.rows(List.of("k"), new Object[][] { { 1 }, { 1 }, { 2 } });
        ok.removeDuplicateRowsBy("k", k -> {
            ok.set(0, 0, ok.get(0, 0));
            return k;
        });
        assertEquals(List.of(1, 2), ok.getColumn("k"));
    }

    @Test
    public void c067_aColumnNameStreamBindsWhenTraversedNotWhenCreated() {
        final Dataset ds = Dataset.rows(List.of("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
        final var stream = ds.columnNames().stream();
        final var spliterator = ds.columnNames().spliterator();

        ds.addColumn("d", List.of(4));
        assertEquals(List.of("a", "b", "c", "d"), stream.toList());

        ds.removeColumn("a");
        final List<String> seen = new ArrayList<>();
        spliterator.forEachRemaining(seen::add);
        assertEquals(List.of("b", "c", "d"), seen);
    }

    @Test
    public void c068_exportToAFileSystemRootIsAnIOFailure() {
        final Dataset ds = Dataset.rows(List.of("a"), new Object[][] { { 1 } });
        final File root = File.listRoots()[0];
        assertThrows(UncheckedIOException.class, () -> ds.toCsv(root));

        if (File.separatorChar == '\\') {
            final Set<Character> mapped = new java.util.HashSet<>();
            for (final File r : File.listRoots()) {
                mapped.add(Character.toUpperCase(r.getPath().charAt(0)));
            }
            for (char drive = 'Z'; drive >= 'D'; drive--) {
                if (!mapped.contains(drive)) {
                    final File unmapped = new File(drive + ":\\");
                    assertThrows(UncheckedIOException.class, () -> ds.toJson(unmapped));
                    break;
                }
            }
        }
    }

    public static class Dev69 {
        private int id;
        private String model;

        public Dev69() {
        }

        public Dev69(final int id, final String model) {
            this.id = id;
            this.model = model;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(final String model) {
            this.model = model;
        }

        @Override
        public String toString() {
            return id + ":" + model;
        }
    }

    public static class Acc69 {
        private String name;
        private List<Dev69> devices = new ArrayList<>(List.of(new Dev69(0, "preset")));

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<Dev69> getDevices() {
            return devices;
        }

        public void setDevices(final List<Dev69> devices) {
            this.devices = devices;
        }
    }

    public static class ImmutableAcc69 {
        private List<Dev69> devices = List.of();

        public List<Dev69> getDevices() {
            return devices;
        }

        public void setDevices(final List<Dev69> devices) {
            this.devices = devices;
        }
    }

    @Test
    public void c069_everyNonMergingBeanConversionReplacesAPreInitialisedCollection() {
        final Dataset ds = Dataset.rows(List.of("name", "devices.id", "devices.model"), new Object[][] { { "a" + EMOJI, 1, "phone" } });
        final String expected = "[1:phone]";

        assertEquals(expected, ds.toList(Acc69.class).get(0).getDevices().toString());
        assertEquals(expected, ds.<Acc69> toEntities(Map.of(), Acc69.class).get(0).getDevices().toString());
        assertEquals(expected, ds.stream(Acc69.class).toList().get(0).getDevices().toString());
        assertEquals(expected, ((Acc69) ds.stream(Acc69.class).toArray()[0]).getDevices().toString());
        assertEquals(expected, ds.getRow(0, Acc69.class).getDevices().toString());
        assertEquals(expected, ds.toList(n -> new Acc69()).get(0).getDevices().toString());

        // an immutable default used to fail with a bare UnsupportedOperationException on the toList path
        assertEquals(expected, ds.toList(ImmutableAcc69.class).get(0).getDevices().toString());
        assertEquals(expected, ((ImmutableAcc69) ds.stream(ImmutableAcc69.class).toArray()[0]).getDevices().toString());

        // an empty Dataset converts to nothing
        assertTrue(Dataset.rows(List.of("name", "devices.id"), new Object[0][]).toList(Acc69.class).isEmpty());
    }
}
