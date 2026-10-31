package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Correctness regressions for the RowDataset optimizations of the 2026-10-19 performance review (restored 2026-10-29):
 * short duplicate-selection checks, one-pass column removal, batch row normalization, bulk slice copies, single
 * resolution of a row selection, the JSON/XML export scans and matched-right-key tracking in every full-join branch.
 * They compare results with naive oracles and pin exceptions, so they belong to the unit suite rather than to the
 * slow-test DatasetPerformanceMemoryTest they were first restored into.
 */
public class DatasetReviewFollowupTest extends TestBase {
    // ---- perf review 2026-10-19 G201 begin ----
    @Nested
    class R1019G201 {
        private Dataset wide(final int columnCount, final int rowCount) {
            final List<String> names = new ArrayList<>();
            final List<List<Object>> columns = new ArrayList<>();

            for (int c = 0; c < columnCount; c++) {
                names.add("c" + c);
                final List<Object> column = new ArrayList<>();

                for (int r = 0; r < rowCount; r++) {
                    column.add(r * 1000 + c);
                }

                columns.add(column);
            }

            return new RowDataset(names, columns);
        }

        private List<List<Object>> rowsOf(final Dataset ds) {
            final List<List<Object>> rows = new ArrayList<>();

            for (int r = 0; r < ds.size(); r++) {
                final List<Object> row = new ArrayList<>();

                for (int c = 0; c < ds.columnCount(); c++) {
                    row.add(ds.get(r, c));
                }

                rows.add(row);
            }

            return rows;
        }

        public static class IdName {
            private int id;
            private String name;

            public IdName() {
            }

            public IdName(final int id, final String name) {
                this.id = id;
                this.name = name;
            }

            public int getId() {
                return id;
            }

            public void setId(final int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(final String name) {
                this.name = name;
            }
        }

        // G201-01: short selections are checked pairwise; the first entry repeating an earlier one is still reported,
        // with the same message, on both sides of the pairwise/boolean[] threshold (8).
        @Test
        void testCheckColumnNames_duplicateSelection_pairwiseAndBooleanPaths() {
            final Random random = new Random(201);

            for (final int columnCount : new int[] { 3, 10, 40, 300 }) {
                final Dataset ds = wide(columnCount, 2);

                for (int round = 0; round < 400; round++) {
                    final int length = 2 + random.nextInt(Math.min(columnCount + 4, 14));
                    final List<String> selection = new ArrayList<>();

                    for (int i = 0; i < length; i++) {
                        selection.add("c" + random.nextInt(Math.min(columnCount, round % 2 == 0 ? columnCount : 20)));
                    }

                    String firstRepeat = null;

                    for (int j = 1; j < length && firstRepeat == null; j++) {
                        if (selection.subList(0, j).contains(selection.get(j))) {
                            firstRepeat = selection.get(j);
                        }
                    }

                    if (firstRepeat == null) {
                        final Dataset copy = ds.copy(selection);
                        assertEquals(selection, copy.columnNames());
                        assertEquals((Object) ds.get(1, ds.getColumnIndex(selection.get(length - 1))), (Object) copy.get(1, length - 1));
                    } else {
                        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.copy(selection));
                        assertEquals("Duplicated column names in the selection: " + selection + " (" + firstRepeat + " is listed more than once)",
                                e.getMessage());
                    }
                }
            }

            final Dataset ds = wide(12, 1);
            final List<String> eight = Arrays.asList("c0", "c1", "c2", "c3", "c4", "c5", "c6", "c0");
            final List<String> nine = Arrays.asList("c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c3");
            assertEquals("Duplicated column names in the selection: " + eight + " (c0 is listed more than once)",
                    assertThrows(IllegalArgumentException.class, () -> ds.copy(eight)).getMessage());
            assertEquals("Duplicated column names in the selection: " + nine + " (c3 is listed more than once)",
                    assertThrows(IllegalArgumentException.class, () -> ds.copy(nine)).getMessage());
            assertEquals(8, ds.copy(Arrays.asList("c0", "c1", "c2", "c3", "c4", "c5", "c6", "c7")).columnCount());
            assertEquals(9, ds.copy(Arrays.asList("c8", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c0")).columnCount());
            // An unknown name still wins over a repeated one (resolved first).
            assertThrows(IllegalArgumentException.class, () -> ds.copy(Arrays.asList("c0", "c0", "zz")));
        }

        // G201-02: removeColumns compacts the name and column lists in one pass; same result as removing one by one.
        @Test
        void testRemoveColumns_compactionMatchesOneByOne() {
            final Random random = new Random(2012);

            for (final int columnCount : new int[] { 2, 5, 17, 120 }) {
                for (int round = 0; round < 150; round++) {
                    final Dataset ds = wide(columnCount, round % 3);
                    final int toRemove = 1 + random.nextInt(columnCount + 2);
                    final List<String> names = new ArrayList<>();

                    for (int i = 0; i < toRemove; i++) {
                        names.add("c" + random.nextInt(columnCount));
                    }

                    final List<String> expectedNames = new ArrayList<>(ds.columnNames());
                    expectedNames.removeAll(names);

                    if (expectedNames.isEmpty() && ds.size() > 0) {
                        assertThrows(IllegalArgumentException.class, () -> ds.removeColumns(names));
                        assertEquals(columnCount, ds.columnCount());
                        continue;
                    }

                    final List<List<Object>> expectedColumns = new ArrayList<>();

                    for (final String name : expectedNames) {
                        expectedColumns.add(new ArrayList<>(ds.getColumn(name)));
                    }

                    ds.removeColumns(names);

                    assertEquals(expectedNames, ds.columnNames());

                    for (int i = 0; i < expectedNames.size(); i++) {
                        assertEquals(i, ds.getColumnIndex(expectedNames.get(i)));
                        assertEquals(expectedColumns.get(i), ds.getColumn(i));
                    }

                    for (final String name : names) {
                        assertFalse(ds.containsColumn(name));
                    }

                    // Still a normal mutable dataset afterwards.
                    if (!expectedNames.isEmpty()) {
                        ds.addColumn("extra", Collections.nCopies(ds.size(), "x"));
                        assertEquals(expectedNames.size(), ds.getColumnIndex("extra"));
                    }
                }
            }

            final Dataset ds = wide(4, 2);
            assertThrows(IllegalArgumentException.class, () -> ds.removeColumns(Arrays.asList("c1", "nope")));
            assertEquals(4, ds.columnCount());
            ds.removeColumns(Arrays.asList("c3", "c1", "c3"));
            assertEquals(Arrays.asList("c0", "c2"), ds.columnNames());
            ds.removeColumns(name -> name.equals("c0"));
            assertEquals(Arrays.asList("c2"), ds.columnNames());
            assertEquals(Arrays.asList(2, 1002), ds.getColumn(0));
        }

        // G201-03: addRows reads every row into one scratch array; rows of mixed types must not alias each other and a
        // bad row still leaves the dataset unchanged.
        @Test
        void testAddRows_reusedScratchRow_mixedTypesAndAtomicity() {
            final Dataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                    new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "m");
            map.put("id", 3);
            ds.addRows(Arrays.asList(new Object[] { 1, "a" }, Arrays.asList(2, "b"), map, new IdName(4, "bean"), new Object[] { null, null },
                    Arrays.asList(6, null)));
            ds.addRows(1, Arrays.asList(new Object[] { 10, "x" }, new IdName(11, "y")));
            ds.addRow(new Object[] { 7, "z" });

            assertEquals(Arrays.asList(Arrays.asList(1, "a"), Arrays.asList(10, "x"), Arrays.asList(11, "y"), Arrays.asList(2, "b"), Arrays.asList(3, "m"),
                    Arrays.asList(4, "bean"), Arrays.asList(null, null), Arrays.asList(6, null), Arrays.asList(7, "z")), rowsOf(ds));

            final List<List<Object>> before = rowsOf(ds);
            final Map<String, Object> missing = new LinkedHashMap<>();
            missing.put("id", 9);
            assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 8, "ok" }, missing)));
            assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 8, "ok" }, new Object[] { 1 })));
            assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 8, "ok" }, Arrays.asList(1, 2, 3))));
            assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 8, "ok" }, null)));
            assertEquals(before, rowsOf(ds));

            final Dataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
            assertThrows(IllegalArgumentException.class, () -> noColumns.addRows(Collections.singletonList((Object) new Object[0])));

            final List<Object[]> many = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                many.add(new Object[] { i, "n" + i });
            }

            final Dataset big = ds.copy();
            big.addRows(many);
            assertEquals(before.size() + 1000, big.size());

            for (int i = 0; i < 1000; i++) {
                assertEquals(i, (Object) big.get(before.size() + i, 0));
                assertEquals("n" + i, big.get(before.size() + i, 1));
            }
        }

        // G201-04: SliceColumn.toArray copies the viewed range in bulk: same contents, Object[] class, same
        // ConcurrentModificationException for a column view whose parent changed its rows.
        @Test
        void testSliceColumn_toArray_copySplitPagesAndStaleView() {
            final Dataset parent = wide(4, 50);
            final Dataset slice = parent.slice(5, 45);
            final Dataset nested = slice.slice(10, 20);

            final Dataset sliceCopy = slice.copy();
            assertEquals(40, sliceCopy.size());
            assertEquals(rowsOf(slice), rowsOf(sliceCopy));
            assertEquals(5000 + 3, (Object) sliceCopy.get(0, 3));
            assertEquals(rowsOf(parent.copy(15, 25)), rowsOf(nested.copy()));
            assertEquals(0, parent.slice(7, 7).copy().size());
            sliceCopy.addRow(new Object[] { 1, 2, 3, 4 });
            assertEquals(41, sliceCopy.size());
            assertEquals(40, slice.size());

            final Object[] array = slice.getColumn(2).toArray();
            assertEquals(Object[].class, array.getClass());
            assertEquals(40, array.length);
            assertEquals(5000 + 2, array[0]);
            assertEquals(44000 + 2, array[39]);
            assertEquals(Arrays.asList(array), new ArrayList<>(slice.getColumn(2)));

            final List<Object> target = new ArrayList<>(Arrays.asList("head"));
            target.addAll(nested.getColumn(1));
            assertEquals(11, target.size());
            assertEquals(15000 + 1, target.get(1));

            final Paginated<Dataset> pages = parent.paginate(15);
            assertEquals(rowsOf(parent.copy(45, 50)), rowsOf(pages.getPage(3).copy()));
            assertEquals(rowsOf(parent.copy(15, 30)), rowsOf(pages.getPage(1).copy()));

            final List<Dataset> parts = slice.splitToList(7);
            assertEquals(6, parts.size());
            assertEquals(rowsOf(parent.copy(40, 45)), rowsOf(parts.get(5)));

            final ImmutableList<Object> staleColumn = slice.getColumn(0);
            parent.addRow(new Object[] { 1, 2, 3, 4 });
            assertThrows(java.util.ConcurrentModificationException.class, staleColumn::toArray);
            assertThrows(java.util.ConcurrentModificationException.class, () -> new ArrayList<>(staleColumn));
            assertThrows(java.util.ConcurrentModificationException.class, slice::copy);
            assertEquals(41, sliceCopy.size());
        }
    }

    // ---- perf review 2026-10-19 G201 end ----
    // ---- perf review 2026-10-19 G202 begin ----
    @Nested
    class R1019G202 {

        private java.lang.invoke.MethodHandle privateStatic(final String name, final Class<?>... parameterTypes) throws Exception {
            final java.lang.reflect.Method method = RowDataset.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return java.lang.invoke.MethodHandles.lookup().unreflect(method);
        }

        /** The scan as it was written before the first-character guard. */
        private boolean referenceHasJsonNonFiniteMarker(final String text) {
            boolean quoted = false;
            for (int i = 0; i < text.length(); i++) {
                final char ch = text.charAt(i);
                if (quoted && ch == '\\') {
                    i++;
                } else if (ch == '"') {
                    quoted = !quoted;
                } else if (!quoted && (text.startsWith("NaN", i) || text.startsWith("Infinity", i))) {
                    return true;
                }
            }
            return false;
        }

        /** The predicate in its original test order. */
        private boolean referenceIsXmlCharacter(final int cp) {
            return cp == 9 || cp == 10 || cp == 13 || cp >= 0x20 && cp <= 0xD7FF || cp >= 0xE000 && cp <= 0xFFFD || cp >= 0x10000 && cp <= 0x10FFFF;
        }

        private void assertJsonScan(final java.lang.invoke.MethodHandle scan, final String text) throws Throwable {
            final boolean expectedFailure = referenceHasJsonNonFiniteMarker(text);
            try {
                scan.invoke(text, 7, "c");
                assertFalse(expectedFailure, text);
            } catch (final IllegalArgumentException e) {
                assertTrue(expectedFailure, text);
                assertEquals("Cannot export JSON: invalid value at row 7, column 'c'", e.getMessage());
            }
        }

        // G202-01: the first-character guard of checkJsonNumbers rejects exactly the texts the unguarded scan rejected.
        @Test
        void testCheckJsonNumbers_guardedScanMatchesUnguardedScan() throws Throwable {
            final java.lang.invoke.MethodHandle scan = privateStatic("checkJsonNumbers", String.class, int.class, String.class);
            final char[] alphabet = { 'N', 'a', 'I', 'n', 'f', 'x', '"', '\\', '1' };
            final StringBuilder sb = new StringBuilder();

            for (int length = 0; length <= 5; length++) {
                final int[] digits = new int[length];
                while (true) {
                    sb.setLength(0);
                    for (final int d : digits) {
                        sb.append(alphabet[d]);
                    }
                    assertJsonScan(scan, sb.toString());

                    int k = length - 1;
                    while (k >= 0 && ++digits[k] == alphabet.length) {
                        digits[k--] = 0;
                    }
                    if (k < 0) {
                        break;
                    }
                }
            }

            final String[] tokens = { "NaN", "Infinity", "-Infinity", "N", "I", "Na", "Infinit", "nan", "infinity", "\"", "\\", "\\\"", "x", "1.5", ",", "[",
                    "]", "{", "}", ":", " ", "null", "true" };
            final Random random = new Random(20261020L);
            for (int n = 0; n < 50_000; n++) {
                sb.setLength(0);
                for (int t = random.nextInt(12); t >= 0; t--) {
                    sb.append(tokens[random.nextInt(tokens.length)]);
                }
                assertJsonScan(scan, sb.toString());
            }
        }

        // G202-01: end to end, non-finite numbers inside non-simple cells still fail and quoted look-alikes are still written.
        @Test
        void testToJson_nonFiniteMarkersInNonSimpleCells() {
            for (final Object bad : Arrays.asList(Arrays.asList(1.0, Double.NaN), Arrays.asList(Double.POSITIVE_INFINITY),
                    Arrays.asList(Double.NEGATIVE_INFINITY), new double[] { 2.0, Double.NaN }, Collections.singletonMap("k", Float.NaN))) {
                final Dataset ds = Dataset.rows(Arrays.asList("id", "c"), new Object[][] { { 1, Arrays.asList(1, 2) }, { 2, bad } });
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ds::toJson, String.valueOf(bad));
                assertEquals("Cannot export JSON: invalid value at row 1, column 'c'", e.getMessage());

                final java.io.StringWriter writer = new java.io.StringWriter();
                assertThrows(IllegalArgumentException.class, () -> ds.toJson(writer));
            }

            final Dataset ok = Dataset.rows(Arrays.asList("id", "c"), new Object[][] { { 1, Arrays.asList("NaN", "Infinity", "a\"NaN", "N", "I") },
                    { 2, Collections.singletonMap("NaN", Arrays.asList(1.5, -0.0, 1e300)) }, { 3, new int[] { 1, 2, 3 } } });
            assertEquals(EXPECTED_JSON, ok.toJson());
        }

        private static final String EXPECTED_JSON = "[{\"id\":1, \"c\":[\"NaN\", \"Infinity\", \"a\\\"NaN\", \"N\", \"I\"]}, "
                + "{\"id\":2, \"c\":{\"NaN\": [1.5, -0.0, 1.0E300]}}, {\"id\":3, \"c\":[1, 2, 3]}]";

        // G202-02: isXmlCharacter with the common range tested first answers every code point exactly as before.
        @Test
        void testIsXmlCharacter_allCodePoints() throws Throwable {
            final java.lang.invoke.MethodHandle predicate = privateStatic("isXmlCharacter", int.class);

            for (int cp = -2; cp <= Character.MAX_CODE_POINT + 2; cp++) {
                if ((boolean) predicate.invoke(cp) != referenceIsXmlCharacter(cp)) {
                    throw new AssertionError("code point " + cp);
                }
            }
            for (final int cp : new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE, 0x7FFFFFF }) {
                assertEquals(referenceIsXmlCharacter(cp), (boolean) predicate.invoke(cp));
            }
        }

        // G202-02: end to end, XML text cells with control, CJK, supplementary and invalid characters export or fail as before.
        @Test
        void testToXml_textCellCharacterValidation() {
            final String valid = "tab\tline\nret\r end \u4e2d\u6587 \ud83d\ude00 \ue000\ufffd ~ \u007f";
            final Dataset ok = Dataset.rows(Arrays.asList("c"), new Object[][] { { valid }, { 'x' }, { "" } });
            assertEquals(EXPECTED_XML, ok.toXml());

            for (final Object bad : Arrays.asList("a\u0000", "\u0001", "x\u001fy", "\ufffe", "\uffff", "lone \ud800 high", "lone \udc00 low", "end\ud83d",
                    '\u0008')) {
                final Dataset ds = Dataset.rows(Arrays.asList("id", "c"), new Object[][] { { 1, "fine" }, { 2, bad } });
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ds::toXml, String.valueOf(bad));
                assertEquals("Cannot export XML: invalid value at row 1, column 'c'", e.getMessage());
            }
        }

        private static final String EXPECTED_XML = "<dataset><row><c>tab&#x9;line&#xa;ret&#xd; end \u4e2d\u6587 \ud83d\ude00 \ue000\ufffd ~ &#x7f;</c></row>"
                + "<row><c>x</c></row><row><c></c></row></dataset>";

        private Dataset wide() {
            final List<String> names = new ArrayList<>();
            for (int c = 0; c < 6; c++) {
                names.add("c" + c);
            }
            final Object[][] rows = new Object[4][6];
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 6; c++) {
                    rows[r][c] = c == 3 ? null : r * 10 + c;
                }
            }
            return Dataset.rows(names, rows);
        }

        // G202-03: getRow(int, Collection, IntFunction) resolves the selection once; rows, order and supplier calls are unchanged.
        @Test
        void testGetRowWithSupplier_selectionResolvedOnce() {
            final Dataset ds = wide();
            final List<Integer> lengths = new ArrayList<>();

            final Object[] row = ds.getRow(2, Arrays.asList("c5", "c0", "c3"), (java.util.function.IntFunction<Object[]>) length -> {
                lengths.add(length);
                return new Object[length + 1];
            });
            assertEquals(Arrays.asList(25, 20, null, null), Arrays.asList(row));
            assertEquals(Arrays.asList(3), lengths);

            final Map<String, Object> map = ds.getRow(1, Arrays.asList("c4", "c1"),
                    (java.util.function.IntFunction<Map<String, Object>>) length -> new LinkedHashMap<>());
            assertEquals("{c4=14, c1=11}", map.toString());

            final List<Object> list = ds.getRow(3, new java.util.LinkedHashSet<>(Arrays.asList("c2", "c3")),
                    (java.util.function.IntFunction<List<Object>>) ArrayList::new);
            assertEquals(Arrays.asList(32, null), list);

            final Object[] all = ds.getRow(0, (java.util.function.IntFunction<Object[]>) Object[]::new);
            assertEquals(Arrays.asList(0, 1, 2, null, 4, 5), Arrays.asList(all));
            assertEquals(Arrays.asList(0, 1, 2, null, 4, 5),
                    Arrays.asList(ds.getRow(0, new ArrayList<>(ds.columnNames()), (java.util.function.IntFunction<Object[]>) Object[]::new)));

            assertEquals(Arrays.asList(5, 0),
                    Arrays.asList(ds.firstRow(Arrays.asList("c5", "c0"), (java.util.function.IntFunction<Object[]>) Object[]::new).get()));
            assertEquals(Arrays.asList(35, 30),
                    Arrays.asList(ds.lastRow(Arrays.asList("c5", "c0"), (java.util.function.IntFunction<Object[]>) Object[]::new).get()));

            final Dataset empty = Dataset.rows(Arrays.asList("a"), new Object[0][]);
            assertFalse(empty.firstRow(Arrays.asList("a"), (java.util.function.IntFunction<Object[]>) Object[]::new).isPresent());
        }

        // G202-03: validation order and messages of getRow(int, Collection, IntFunction) are unchanged.
        @Test
        void testGetRowWithSupplier_validationOrder() {
            final Dataset ds = wide();
            final List<Integer> calls = new ArrayList<>();
            final java.util.function.IntFunction<Object[]> supplier = length -> {
                calls.add(length);
                return new Object[length];
            };

            assertThrows(IndexOutOfBoundsException.class, () -> ds.getRow(4, Arrays.asList("nope"), (java.util.function.IntFunction<Object[]>) null));
            assertThrows(IndexOutOfBoundsException.class, () -> ds.getRow(-1, Arrays.asList("c0"), supplier));

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> ds.getRow(0, Arrays.asList("c0", "nope"), (java.util.function.IntFunction<Object[]>) null));
            assertEquals("The specified column: nope is not included in this Dataset: [c0, c1, c2, c3, c4, c5]", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> ds.getRow(0, Arrays.asList("c1", "c1"), supplier));
            assertEquals("Duplicated column names in the selection: [c1, c1] (c1 is listed more than once)", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> ds.getRow(0, new ArrayList<String>(), supplier));
            assertEquals("The specified columnNames is null or empty", e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> ds.getRow(0, (List<String>) null, supplier));

            assertThrows(IllegalArgumentException.class, () -> ds.getRow(0, Arrays.asList("c0"), (java.util.function.IntFunction<Object[]>) null));
            assertTrue(calls.isEmpty());

            final NullPointerException npe = assertThrows(NullPointerException.class,
                    () -> ds.getRow(0, Arrays.asList("c0"), (java.util.function.IntFunction<Object[]>) length -> null));
            assertEquals("rowSupplier returned null", npe.getMessage());

            e = assertThrows(IllegalArgumentException.class,
                    () -> ds.getRow(0, Arrays.asList("c0", "c2"), (java.util.function.IntFunction<Object[]>) length -> new Object[1]));
            assertEquals("rowSupplier returned an array of length 1, but at least 2 elements are required", e.getMessage());

            final Dataset parent = wide();
            final Dataset slice = parent.slice(0, 2);
            assertEquals(Arrays.asList(11), Arrays.asList(slice.getRow(1, Arrays.asList("c1"), supplier)));
            parent.addRow(new Object[] { 1, 2, 3, 4, 5, 6 });
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getRow(0, Arrays.asList("c1"), supplier));
            assertEquals(Arrays.asList(1), calls);
        }
    }

    // ---- perf review 2026-10-19 G202 end ----
    // ---- perf review 2026-10-19 G204 begin ----
    @Nested
    class R1019G204 {
        private final Random random = new Random(20261019L);

        private Object randomKey(final int space) {
            final int value = random.nextInt(space);

            switch (random.nextInt(6)) {
                case 0:
                    return null;
                case 1:
                    // A fresh array per cell: rows must still match by content (hashKey wraps arrays).
                    return new int[] { value % 3 };
                case 2:
                    // Long 1 and Integer 1 are different keys.
                    return Long.valueOf(value);
                default:
                    return Integer.valueOf(value);
            }
        }

        private Dataset side(final String idName, final String valueName, final int rows, final int idBase, final int space) {
            final Object[][] data = new Object[rows][];

            for (int i = 0; i < rows; i++) {
                data[i] = new Object[] { idBase + i, space <= 0 ? (Object) ("key" + (i - space)) : randomKey(space), valueName + i };
            }

            return Dataset.rows(Arrays.asList(idName, "k", valueName), data);
        }

        private boolean sameKey(final Dataset left, final int leftRow, final Dataset right, final int rightRow) {
            final Object leftKey = left.get(leftRow, 1);
            final Object rightKey = right.get(rightRow, 1);
            return CommonUtil.deepEquals(leftKey, rightKey);
        }

        private List<Object> rowValues(final Dataset ds, final int row) {
            final List<Object> values = new ArrayList<>();

            for (int c = 0; c < ds.columnCount(); c++) {
                values.add(ds.get(row, c));
            }

            return values;
        }

        private void addRow(final List<List<Object>> columns, final List<Object> leftValues, final List<Object> rightValues) {
            for (int c = 0; c < leftValues.size(); c++) {
                columns.get(c).add(leftValues.get(c));
            }

            for (int c = 0; c < rightValues.size(); c++) {
                columns.get(leftValues.size() + c).add(rightValues.get(c));
            }
        }

        private boolean leftHasKey(final Dataset left, final Dataset right, final int rightRow) {
            for (int i = 0; i < left.size(); i++) {
                if (sameKey(left, i, right, rightRow)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Naive nested-loop oracle of the single-key fullJoin. mode 0: fullJoin(right, on); 1: fullJoin(right, on, "rr", List.class);
         * 2: fullJoin(right, on, "rr", List.class, collectionSupplier). {@code supplierSizes} receives the expected collectionSupplier
         * arguments in call order (mode 2).
         */
        private List<List<Object>> expectedFullJoin(final Dataset left, final Dataset right, final int mode, final List<Integer> supplierSizes) {
            final List<Object> nullLeft = Arrays.asList(null, null, null);
            final List<Object> nullRight = mode == 0 ? Arrays.asList(null, null, null) : Collections.singletonList(null);
            final List<List<Object>> columns = new ArrayList<>();

            for (int c = 0, count = mode == 0 ? 6 : 4; c < count; c++) {
                columns.add(new ArrayList<>());
            }

            for (int i = 0; i < left.size(); i++) {
                final List<Object> matches = new ArrayList<>();

                for (int j = 0; j < right.size(); j++) {
                    if (sameKey(left, i, right, j)) {
                        matches.add(rowValues(right, j));
                    }
                }

                if (matches.isEmpty()) {
                    addRow(columns, rowValues(left, i), nullRight);
                } else if (mode == 2) {
                    supplierSizes.add(matches.size());
                    addRow(columns, rowValues(left, i), Collections.singletonList(matches));
                } else {
                    for (final Object match : matches) {
                        addRow(columns, rowValues(left, i), mode == 0 ? (List<Object>) match : Collections.singletonList(match));
                    }
                }
            }

            final boolean[] done = new boolean[right.size()];

            for (int j = 0; j < right.size(); j++) {
                if (done[j] || leftHasKey(left, right, j)) {
                    continue;
                }

                if (mode == 2) {
                    // One row per unmatched key, in the order the key first occurs on the right.
                    final List<Object> group = new ArrayList<>();

                    for (int k = j; k < right.size(); k++) {
                        if (sameKey(right, j, right, k)) {
                            group.add(rowValues(right, k));
                            done[k] = true;
                        }
                    }

                    supplierSizes.add(group.size());
                    addRow(columns, nullLeft, Collections.singletonList(group));
                } else {
                    // Unmatched right rows in right-row order.
                    addRow(columns, nullLeft, mode == 0 ? rowValues(right, j) : Collections.singletonList(rowValues(right, j)));
                }
            }

            return columns;
        }

        private void assertColumns(final List<List<Object>> expected, final Dataset actual) {
            assertEquals(expected.size(), actual.columnCount());

            for (int c = 0; c < expected.size(); c++) {
                assertEquals(expected.get(c), new ArrayList<>(actual.getColumn(c)), "column " + actual.columnNames().get(c));
            }
        }

        private void assertSingleKeyFullJoin(final Dataset left, final Dataset right) {
            final Map<String, String> on = Collections.singletonMap("k", "k");
            final List<Integer> ignored = new ArrayList<>();

            final Dataset plain = left.fullJoin(right, on);
            assertEquals(left == right ? Arrays.asList("id", "k", "v", "id_2", "k_2", "v_2") : Arrays.asList("id", "k", "v", "rid", "k_2", "r"),
                    plain.columnNames());
            assertColumns(expectedFullJoin(left, right, 0, ignored), plain);

            final Dataset typed = left.fullJoin(right, on, "rr", List.class);
            assertEquals(Arrays.asList("id", "k", "v", "rr"), typed.columnNames());
            assertColumns(expectedFullJoin(left, right, 1, ignored), typed);

            final List<Integer> expectedSizes = new ArrayList<>();
            final List<List<Object>> expected = expectedFullJoin(left, right, 2, expectedSizes);
            final List<Integer> actualSizes = new ArrayList<>();
            final Dataset collected = left.fullJoin(right, on, "rr", List.class, size -> {
                actualSizes.add(size);
                return new ArrayList<>(size);
            });
            assertEquals(Arrays.asList("id", "k", "v", "rr"), collected.columnNames());
            assertColumns(expected, collected);
            assertEquals(expectedSizes, actualSizes);
        }

        // G204-01: single-key fullJoin (plain, row-typed column, one collection per left row) against a naive nested-loop
        // oracle: duplicate, null, Integer/Long and content-equal array keys, unmatched rows on both sides, empty sides,
        // and the collectionSupplier call sequence.
        @Test
        void testFullJoin_singleKeyMatchesNaiveOracle() {
            final int[] sizes = { 0, 1, 2, 5, 17, 40 };

            for (final int leftSize : sizes) {
                for (final int rightSize : sizes) {
                    for (final int space : new int[] { 1, 4, 30 }) {
                        assertSingleKeyFullJoin(side("id", "v", leftSize, 0, space), side("rid", "r", rightSize, 1000, space));
                    }
                }
            }

            // Hundreds of distinct keys, 10% unmatched on each side.
            final Dataset left = side("id", "v", 300, 0, 0);
            final Dataset right = side("rid", "r", 300, 1000, -30);
            assertSingleKeyFullJoin(left, right);
            assertEquals(330, left.fullJoin(right, Collections.singletonMap("k", "k")).size());
        }

        // G204-01: self join, and duplicate keys on both sides (only the first right row index of a key carries its mark).
        @Test
        void testFullJoin_singleKeySelfJoinAndDuplicates() {
            final Dataset self = side("id", "v", 30, 0, 5);
            assertSingleKeyFullJoin(self, self);

            final Dataset left = Dataset.rows(Arrays.asList("id", "k", "v"),
                    new Object[][] { { 0, "b", "v0" }, { 1, new int[] { 7 }, "v1" }, { 2, "b", "v2" }, { 3, null, "v3" }, { 4, "z", "v4" } });
            final Dataset right = Dataset.rows(Arrays.asList("rid", "k", "r"), new Object[][] { { 100, "a", "r0" }, { 101, "b", "r1" }, { 102, "a", "r2" },
                    { 103, new int[] { 7 }, "r3" }, { 104, "c", "r4" }, { 105, "b", "r5" }, { 106, null, "r6" }, { 107, "c", "r7" } });
            assertSingleKeyFullJoin(left, right);

            final Map<String, String> on = Collections.singletonMap("k", "k");
            assertEquals(Arrays.asList(0, 0, 1, 2, 2, 3, 4, null, null, null, null), new ArrayList<>(left.fullJoin(right, on).getColumn("id")));
            assertEquals(Arrays.asList(101, 105, 103, 101, 105, 106, null, 100, 102, 104, 107), new ArrayList<>(left.fullJoin(right, on).getColumn("rid")));
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, null, null), new ArrayList<>(left.fullJoin(right, on, "rr", List.class, ArrayList::new).getColumn("id")));
        }

        // G204-01: the multi-key branches with a key array reused for every left row: many distinct left keys (the reused
        // array must never be stored), keys equal only by content, and a self join.
        @Test
        void testFullJoin_multiKeyReusedLeftKeyArray() {
            final Object[][] leftRows = new Object[200][];
            final Object[][] rightRows = new Object[150][];

            for (int i = 0; i < leftRows.length; i++) {
                leftRows[i] = new Object[] { i, i % 50 == 0 ? null : new int[] { i % 70 }, i % 3, "v" + i };
            }

            for (int i = 0; i < rightRows.length; i++) {
                rightRows[i] = new Object[] { 1000 + i, new int[] { (i * 7) % 90 }, i % 3, "r" + i };
            }

            final Dataset left = Dataset.rows(Arrays.asList("id", "k1", "k2", "v"), leftRows);
            final Dataset right = Dataset.rows(Arrays.asList("rid", "k1", "k2", "r"), rightRows);
            final Map<String, String> on = new LinkedHashMap<>();
            on.put("k1", "k1");
            on.put("k2", "k2");

            final Dataset plain = left.fullJoin(right, on);
            final Dataset typed = left.fullJoin(right, on, "rr", Object[].class);
            final Dataset collected = left.fullJoin(right, on, "rr", Object[].class, ArrayList::new);

            int matchedRows = 0;
            int expectedCollectedRows = 0;
            int selfRows = 0;
            final boolean[] matched = new boolean[rightRows.length];

            for (final Object[] l : leftRows) {
                int count = 0;

                for (int j = 0; j < rightRows.length; j++) {
                    if (CommonUtil.deepEquals(l[1], rightRows[j][1]) && CommonUtil.deepEquals(l[2], rightRows[j][2])) {
                        count++;
                        matched[j] = true;
                    }
                }

                for (final Object[] other : leftRows) {
                    if (CommonUtil.deepEquals(l[1], other[1]) && CommonUtil.deepEquals(l[2], other[2])) {
                        selfRows++;
                    }
                }

                matchedRows += Math.max(count, 1);
                expectedCollectedRows++;
            }

            final List<Object> expectedTail = new ArrayList<>();
            final java.util.Set<String> unmatchedKeys = new java.util.LinkedHashSet<>();

            for (int j = 0; j < rightRows.length; j++) {
                if (!matched[j]) {
                    expectedTail.add(1000 + j);
                    unmatchedKeys.add(Arrays.toString((int[]) rightRows[j][1]) + "/" + rightRows[j][2]);
                }
            }

            final int expectedRows = matchedRows + expectedTail.size();
            expectedCollectedRows += unmatchedKeys.size();

            assertTrue(unmatchedKeys.size() > 10);
            assertEquals(expectedRows, plain.size());
            assertEquals(expectedRows, typed.size());
            assertEquals(expectedCollectedRows, collected.size());

            // The unmatched tail is the unmatched right rows in right-row order; the left part keeps every left row.
            assertEquals(expectedTail, new ArrayList<>(plain.getColumn("rid")).subList(matchedRows, expectedRows));
            assertEquals(Collections.nCopies(expectedTail.size(), null), new ArrayList<>(typed.getColumn("id")).subList(matchedRows, expectedRows));
            assertEquals(Collections.nCopies(unmatchedKeys.size(), null),
                    new ArrayList<>(collected.getColumn("id")).subList(leftRows.length, expectedCollectedRows));

            for (int i = 0; i < matchedRows; i++) {
                final int leftId = (Integer) plain.get(i, 0);
                final Object rightId = plain.get(i, 4);
                final Object[] typedRow = (Object[]) typed.get(i, 4);

                if (rightId == null) {
                    assertEquals(null, typedRow);
                } else {
                    assertTrue(CommonUtil.deepEquals(leftRows[leftId][1], rightRows[(Integer) rightId - 1000][1]));
                    assertEquals(rightId, typedRow[0]);
                }
            }

            // Self join: every left row matches at least itself, so there is no unmatched tail.
            assertEquals(selfRows, left.fullJoin(left, on).size());
            assertEquals(left.size(), left.fullJoin(left, on, "rr", Object[].class, ArrayList::new).size());
        }
    }

    // ---- perf review 2026-10-19 G204 end ----
    // ---- perf review 2026-10-19 G211 begin ----
    @Nested
    class R1019G211 {
        private Dataset grid(final int columnCount, final int rowCount) {
            final List<String> names = new ArrayList<>();
            final List<List<Object>> columns = new ArrayList<>();

            for (int c = 0; c < columnCount; c++) {
                names.add("c" + c);
                final List<Object> column = new ArrayList<>();

                for (int r = 0; r < rowCount; r++) {
                    column.add(r % 7 == 3 ? null : r * 1000 + c);
                }

                columns.add(column);
            }

            return new RowDataset(names, columns);
        }

        private List<List<Object>> rowsOf(final Dataset ds) {
            final List<List<Object>> rows = new ArrayList<>();

            for (int r = 0; r < ds.size(); r++) {
                final List<Object> row = new ArrayList<>();

                for (int c = 0; c < ds.columnCount(); c++) {
                    row.add(ds.get(r, c));
                }

                rows.add(row);
            }

            return rows;
        }

        // G211 (G203-01/G201-05): copy(from, to) of a slice narrows the slice column with slice(..) instead of
        // subList(..): same rows, independent mutable copies, same exceptions for a bad range and a stale slice.
        @Test
        void testSliceCopyRange_matchesParentCopyAndKeepsExceptions() {
            final Dataset parent = grid(3, 60);
            final Dataset slice = parent.slice(5, 55);
            final Dataset nested = slice.slice(10, 40);

            assertEquals(rowsOf(parent.copy(15, 45)), rowsOf(slice.copy(10, 40)));
            assertEquals(rowsOf(parent.copy(5, 6)), rowsOf(slice.copy(0, 1)));
            assertEquals(rowsOf(parent.copy(54, 55)), rowsOf(slice.copy(49, 50)));
            assertEquals(0, slice.copy(20, 20).size());
            assertEquals(rowsOf(parent.copy(20, 30)), rowsOf(nested.copy(5, 15)));
            assertEquals(rowsOf(parent.copy(20, 30, Arrays.asList("c2", "c0"))), rowsOf(nested.copy(5, 15, Arrays.asList("c2", "c0"))));

            final List<Dataset> parts = slice.splitToList(7);
            assertEquals(8, parts.size());
            assertEquals(rowsOf(parent.copy(5, 12)), rowsOf(parts.get(0)));
            assertEquals(rowsOf(parent.copy(54, 55)), rowsOf(parts.get(7)));

            final Dataset copy = slice.copy(10, 20);
            copy.addRow(new Object[] { 1, 2, 3 });
            copy.set(0, 0, -1);
            assertEquals(11, copy.size());
            assertEquals(50, slice.size());
            assertEquals(15000, (Object) slice.get(10, 0));

            assertThrows(IndexOutOfBoundsException.class, () -> slice.copy(-1, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> slice.copy(4, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> slice.copy(0, 51));

            parent.addRow(new Object[] { 1, 2, 3 });
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.copy(1, 3));
            assertThrows(java.util.ConcurrentModificationException.class, () -> nested.copy(1, 3));
        }
    }
    // ---- perf review 2026-10-19 G211 end ----

    // ---- review follow-up 2026-10-29 begin ----
    @Nested
    class ReviewFollowup20261029 {
        private List<List<Object>> rowsOf(final Dataset ds) {
            final List<List<Object>> rows = new ArrayList<>();

            for (int r = 0; r < ds.size(); r++) {
                final List<Object> row = new ArrayList<>();

                for (int c = 0; c < ds.columnCount(); c++) {
                    row.add(ds.get(r, c));
                }

                rows.add(row);
            }

            return rows;
        }

        // An object-array row is now read in place instead of being copied into a scratch row. The dataset still copies
        // each value into its columns, so the caller's array is never retained: changing it afterwards changes nothing,
        // also when one array is passed several times in a batch together with other row types.
        @Test
        void objectArrayRowsAreReadInPlaceWithoutBeingRetained() {
            final Dataset ds = new RowDataset(new ArrayList<>(Arrays.asList("id", "name")),
                    new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
            final Object[] row = { 1, "a" };
            ds.addRow(row);
            ds.addRow(0, row);
            row[0] = 99;
            row[1] = "z";

            final Object[] shared = { 2, "b" };
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "m");
            map.put("id", 4);
            ds.addRows(Arrays.asList(shared, Arrays.asList(3, "c"), shared, map, new Object[] { null, null }));
            shared[0] = 98;
            ds.addRows(1, Collections.singletonList(shared));
            shared[1] = "y";

            assertEquals(Arrays.asList(Arrays.asList(1, "a"), Arrays.asList(98, "b"), Arrays.asList(1, "a"), Arrays.asList(2, "b"), Arrays.asList(3, "c"),
                    Arrays.asList(2, "b"), Arrays.asList(4, "m"), Arrays.asList(null, null)), rowsOf(ds));

            // A wrong-length array is still rejected before any column changes.
            final List<List<Object>> before = rowsOf(ds);
            assertThrows(IllegalArgumentException.class, () -> ds.addRow(new Object[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> ds.addRow(new Object[] { 1, "x", "extra" }));
            assertThrows(IllegalArgumentException.class, () -> ds.addRows(Arrays.asList(new Object[] { 5, "ok" }, new Object[] { 1 })));
            assertEquals(before, rowsOf(ds));
        }

        // The matched right keys are bits now. A skewed join - a few left rows against a right side spanning many bit
        // words - must still emit every unmatched right row once, in right-row order, in all three fullJoin shapes.
        @Test
        void fullJoinMarksMatchedRightKeysAcrossManyBitWords() {
            final int rightSize = 1000;
            final Object[][] rightRows = new Object[rightSize][];

            for (int i = 0; i < rightSize; i++) {
                rightRows[i] = new Object[] { i % 250, "r" + i };
            }

            final Dataset left = Dataset.rows(Arrays.asList("k", "v"),
                    new Object[][] { { 0, "l0" }, { 63, "l63" }, { 64, "l64" }, { 249, "l249" }, { 999, "l999" } });
            final Dataset right = Dataset.rows(Arrays.asList("k", "r"), rightRows);
            final Map<String, String> on = Collections.singletonMap("k", "k");
            final List<Integer> matchedKeys = Arrays.asList(0, 63, 64, 249);

            final Dataset plain = left.fullJoin(right, on);
            // Each matched key appears 4 times on the right; 999 has no match.
            assertEquals(4 * 4 + 1 + (rightSize - 16), plain.size());
            final List<Object> tail = new ArrayList<>(plain.getColumn("r")).subList(17, plain.size());
            final List<Object> expectedTail = new ArrayList<>();

            for (int i = 0; i < rightSize; i++) {
                if (!matchedKeys.contains(i % 250)) {
                    expectedTail.add("r" + i);
                }
            }

            assertEquals(expectedTail, tail);

            final Dataset typed = left.fullJoin(right, on, "rr", Object[].class);
            assertEquals(plain.size(), typed.size());

            final Dataset collected = left.fullJoin(right, on, "rr", List.class, ArrayList::new);
            // One row per left row, then one row per unmatched right key, in first-occurrence order.
            assertEquals(left.size() + (250 - matchedKeys.size()), collected.size());
            assertEquals(4, ((List<?>) collected.get(0, 2)).size());
            assertEquals(null, (Object) collected.get(4, 2));
            assertEquals(4, ((List<?>) collected.get(5, 2)).size());
        }
    }
    // ---- review follow-up 2026-10-29 end ----
}
