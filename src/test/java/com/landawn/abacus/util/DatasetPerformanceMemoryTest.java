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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Dataset. */
@Tag("slow-test")
public class DatasetPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G059 end ----
    // ---- perf review 2026-10-08 G060 begin ----
    @Nested
    class G060 {

        private static final String INVALID_NAME_PREFIX = "Invalid XML element name for ";

        private Dataset oneCell() {
            return Dataset.rows(Arrays.asList("c"), new Object[][] { { Arrays.asList("a", "b") } });
        }

        private String read(final java.io.File file) throws java.io.IOException {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        }

        // G060-01: plain ASCII element names are accepted without building a DOM document; output unchanged.
        @Test
        void testToXml_plainElementNamesWrittenAsBefore(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path dir) throws Exception {
            final Dataset ds = Dataset.rows(Arrays.asList("id", "a.b-c_1", "_x", "Z9"), new Object[][] { { 1, "v", 2.5, true } });
            final String expected = "<dataset><row_1><id>1</id><a.b-c_1>v</a.b-c_1><_x>2.5</_x><Z9>true</Z9></row_1></dataset>";

            assertEquals(expected, ds.toXml("row_1"));
            assertEquals(expected, ds.toXml(0, 1, Arrays.asList("id", "a.b-c_1", "_x", "Z9"), "row_1"));

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toXml("row_1", writer);
            assertEquals(expected, writer.toString());

            final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            ds.toXml("row_1", bytes);
            assertEquals(expected, bytes.toString(java.nio.charset.StandardCharsets.UTF_8));

            final java.io.File file = dir.resolve("plain.xml").toAbsolutePath().toFile();
            ds.toXml("row_1", file);
            assertEquals(expected, read(file));

            assertEquals("<dataset><row><id>1</id><Z9>true</Z9></row></dataset>", ds.toXml(0, 1, Arrays.asList("id", "Z9")));
        }

        // G060-01: names outside the plain ASCII subset (incl. the reserved "xml" prefix) are still decided by the DOM.
        @Test
        void testToXml_nonPlainElementNamesStillValidatedByDom() {
            for (final String name : new String[] { "xml", "XmlData", "xmlnsx", "\u540d\u524d", "a\u00b7b" }) {
                assertEquals("<dataset><row><" + name + ">1</" + name + "><v>2</v></row></dataset>",
                        Dataset.rows(Arrays.asList(name, "v"), new Object[][] { { 1, 2 } }).toXml(), name);
                assertEquals("<dataset><" + name + "><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></" + name + "></dataset>", oneCell().toXml(name), name);
            }

            final String[] rejected = { "xmlns", "a:b", ":a", "a:", "1a", "-a", ".a", "a b", "\uD800", "a\u0000" };
            final short[] codes = { 14, 14, 14, 14, 5, 5, 5, 5, 5, 5 };

            for (int i = 0; i < rejected.length; i++) {
                final String name = rejected[i];
                final Dataset ds = Dataset.rows(Arrays.asList("ok", name), new Object[][] { { 1, 2 } });

                IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ds::toXml);
                assertEquals(INVALID_NAME_PREFIX + "columnName: " + name, e.getMessage());
                assertTrue(e.getCause() instanceof org.w3c.dom.DOMException, name);
                assertEquals(codes[i], ((org.w3c.dom.DOMException) e.getCause()).code, name);

                e = assertThrows(IllegalArgumentException.class, () -> oneCell().toXml(name));
                assertEquals(INVALID_NAME_PREFIX + "rowElementName: " + name, e.getMessage());
                assertEquals(codes[i], ((org.w3c.dom.DOMException) e.getCause()).code, name);

                // An unselected column is not validated.
                assertEquals("<dataset><row><ok>1</ok></row></dataset>", ds.toXml(0, 1, Arrays.asList("ok")));
            }
        }

        // G060-01: validation still happens before anything is written, in the same order (row element name first).
        @Test
        void testToXml_invalidNameFailsBeforeAnyOutput(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path dir) throws Exception {
            final Dataset ds = Dataset.rows(Arrays.asList("ok", "bad name", "1st"), new Object[][] { { 1, 2, 3 } });
            final java.io.StringWriter writer = new java.io.StringWriter();

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(writer));
            assertEquals(INVALID_NAME_PREFIX + "columnName: bad name", e.getMessage());
            assertEquals("", writer.toString());

            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml("bad row", writer));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: bad row", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(0, 1, Collections.emptyList(), "bad row", writer));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: bad row", e.getMessage());
            assertEquals("", writer.toString());

            ds.toXml(0, 1, Collections.emptyList(), "row", writer);
            assertEquals("<dataset></dataset>", writer.toString());

            final java.io.File file = dir.resolve("out.xml").toAbsolutePath().toFile();
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(file));
            assertEquals(INVALID_NAME_PREFIX + "columnName: bad name", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(0, 1, Arrays.asList("ok", "1st"), "row", file));
            assertEquals(INVALID_NAME_PREFIX + "columnName: 1st", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml("x y", file));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: x y", e.getMessage());
            assertFalse(file.exists());

            ds.toXml(0, 1, Arrays.asList("ok"), "row", file);
            assertEquals("<dataset><row><ok>1</ok></row></dataset>", read(file));
        }

        public enum Color {
            RED, GREEN
        }

        /** A bean whose serialization fails, so the export falls back to its toString(). */
        // Accessor-based on purpose: this fixture observes getter/setter calls, and properties are read and written
        // through their fields unless @AccessFieldByMethod says otherwise (in both the reflective and the ASM mode).
        @com.landawn.abacus.annotation.AccessFieldByMethod
        public static class ThrowingBean {
            private int x;

            public int getX() {
                throw new IllegalStateException("boom");
            }

            public void setX(final int x) {
                this.x = x;
            }

            @Override
            public String toString() {
                return "TB";
            }
        }

        private Dataset mixedCells() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", 1);
            map.put("q\"", "v<&>");

            return Dataset.rows(Arrays.asList("d", "l", "e", "m"), new Object[][] {
                    { new java.util.Date(1700000000000L), Arrays.asList("a", "b"), Color.RED, map },
                    { null, Arrays.asList(1, 2, 3), Color.GREEN, java.time.LocalDate.of(2026, 10, 8) },
                    { new ThrowingBean(), java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), new ThrowingBean(), Arrays.asList("x") } });
        }

        // G060-02: one scratch writer per export call; non-simple cells (incl. parser fallbacks) serialize exactly as before.
        @Test
        void testToJson_nonSimpleCellsWithFallbacks() {
            final String expected = "[{\"d\":\"2023-11-14T22:13:20.000Z\", \"l\":[\"a\", \"b\"], \"e\":\"RED\", \"m\":{\"k\": 1, \"q\\\"\": \"v<&>\"}}, "
                    + "{\"d\":null, \"l\":[1, 2, 3], \"e\":\"GREEN\", \"m\":\"2026-10-08\"}, "
                    + "{\"d\":\"TB\", \"l\":\"123e4567-e89b-12d3-a456-426614174000\", \"e\":\"TB\", \"m\":[\"x\"]}]";
            final Dataset ds = mixedCells();

            assertEquals(expected, ds.toJson());

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toJson(writer);
            assertEquals(expected, writer.toString());
            assertEquals("[{\"c\":[\"a\", \"b\"]}]", oneCell().toJson());

            // Column names are escaped through the same scratch writer as the cells.
            assertEquals("[{\"q\\\"uote\":1, \"back\\\\slash\":2, \"tab\\tx\":3, \"\u540d\":4, \"plain\":5}]",
                    Dataset.rows(Arrays.asList("q\"uote", "back\\slash", "tab\tx", "\u540d", "plain"), new Object[][] { { 1, 2, 3, 4, 5 } }).toJson());
        }

        // G060-02: XML counterpart, including a map whose keys are not XML names (parser failure -> string fallback mid-row).
        @Test
        void testToXml_nonSimpleCellsWithFallbacks() {
            final String expected = "<dataset><row><d>2023-11-14T22:13:20.000Z</d><l><list>[&quot;a&quot;, &quot;b&quot;]</list></l><e>RED</e>"
                    + "<m>{k=1, q&quot;=v&lt;&amp;&gt;}</m></row><row><d>null</d><l><list>[1, 2, 3]</list></l><e>GREEN</e><m>2026-10-08</m></row>"
                    + "<row><d>TB</d><l>123e4567-e89b-12d3-a456-426614174000</l><e>TB</e><m><list>[&quot;x&quot;]</list></m></row></dataset>";
            final Dataset ds = mixedCells();

            assertEquals(expected, ds.toXml());

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toXml(writer);
            assertEquals(expected, writer.toString());
            assertEquals("<dataset><row><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></row></dataset>", oneCell().toXml());
        }

        // G060-02: a long cell followed by short ones leaves nothing behind; every multi-row export equals the
        // composition of single-cell exports (each of which starts with a new scratch writer).
        @Test
        void testToJsonToXml_reusedScratchMatchesSingleCellExports() {
            final List<Object> longList = new ArrayList<>();

            for (int i = 0; i < 300; i++) {
                longList.add("element-" + i);
            }

            // Serializes to more characters than a pooled buffer holds, so the scratch buffer has to grow.
            final List<Object> hugeList = new ArrayList<>();

            for (int i = 0; i < 3000; i++) {
                hugeList.add("huge-element-" + i);
            }

            final Object[] cells = { hugeList, longList, Arrays.asList("s"), new java.util.Date(1700000000000L), null, "plain", 7, Color.GREEN,
                    java.time.LocalDate.of(2026, 1, 2), Collections.singletonMap("k", Arrays.asList(1, 2)), new ThrowingBean(), Arrays.asList(),
                    java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000") };
            final int columnCount = 3;
            final int rowCount = 40;
            final List<String> columnNames = Arrays.asList("c0", "c1", "c2");
            final Object[][] rows = new Object[rowCount][columnCount];
            final Random random = new Random(60);

            for (final Object[] row : rows) {
                for (int c = 0; c < columnCount; c++) {
                    row[c] = cells[random.nextInt(cells.length)];
                }
            }

            final StringBuilder json = new StringBuilder("[");
            final StringBuilder xml = new StringBuilder("<dataset>");

            for (int r = 0; r < rowCount; r++) {
                json.append(r > 0 ? ", {" : "{");
                xml.append("<row>");

                for (int c = 0; c < columnCount; c++) {
                    final Dataset single = Dataset.rows(Arrays.asList("x"), new Object[][] { { rows[r][c] } });
                    final String singleJson = single.toJson();
                    final String singleXml = single.toXml();
                    final String jsonValue = singleJson.substring("[{\"x\":".length(), singleJson.length() - "}]".length());
                    final String xmlValue = singleXml.substring("<dataset><row><x>".length(), singleXml.length() - "</x></row></dataset>".length());

                    json.append(c > 0 ? ", " : "").append('"').append(columnNames.get(c)).append("\":").append(jsonValue);
                    xml.append('<').append(columnNames.get(c)).append('>').append(xmlValue).append("</").append(columnNames.get(c)).append('>');
                }

                json.append('}');
                xml.append("</row>");
            }

            json.append(']');
            xml.append("</dataset>");

            final Dataset ds = Dataset.rows(columnNames, rows);

            assertEquals(json.toString(), ds.toJson());
            assertEquals(xml.toString(), ds.toXml());
            assertTrue(ds.toJson().length() > 300, "sanity");

            final Dataset shortAfterLong = Dataset.rows(Arrays.asList("x"),
                    new Object[][] { { longList }, { hugeList }, { Arrays.asList("s") }, { Arrays.asList() } });
            assertTrue(shortAfterLong.toJson().endsWith("}, {\"x\":[\"s\"]}, {\"x\":[]}]"), shortAfterLong.toJson());
            assertTrue(shortAfterLong.toXml().endsWith("<row><x><list>[&quot;s&quot;]</list></x></row><row><x><list>[]</list></x></row></dataset>"),
                    shortAfterLong.toXml());
        }

        // G060-02: a cell that fails validation after earlier non-simple cells fails at the same cell with the same
        // partial output (the failing cell's text is never written).
        @Test
        void testToJsonToXml_validationFailureAfterReusedScratch() {
            final Dataset nan = Dataset.rows(Arrays.asList("a", "b"),
                    new Object[][] { { Arrays.asList(1.0), "x" }, { Arrays.asList(2.0), Arrays.asList(Double.NaN) } });
            final java.io.StringWriter jsonWriter = new java.io.StringWriter();
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> nan.toJson(jsonWriter));
            assertEquals("Cannot export JSON: invalid value at row 1, column 'b'", e.getMessage());
            assertEquals("[{\"a\":[1.0], \"b\":\"x\"}, {\"a\":[2.0], \"b\":", jsonWriter.toString());

            final Dataset bad = Dataset.rows(Arrays.asList("a", "b"),
                    new Object[][] { { Arrays.asList("ok"), "x" }, { Arrays.asList("y"), Collections.singletonMap("nested", "\u0000") } });
            final java.io.StringWriter xmlWriter = new java.io.StringWriter();
            e = assertThrows(IllegalArgumentException.class, () -> bad.toXml(xmlWriter));
            assertEquals("Cannot export XML: invalid value at row 1, column 'b'", e.getMessage());
            assertEquals("<dataset><row><a><list>[&quot;ok&quot;]</list></a><b>x</b></row><row><a><list>[&quot;y&quot;]</list></a><b>", xmlWriter.toString());

            // The export after a failed one is unaffected.
            assertEquals("[{\"c\":[\"a\", \"b\"]}]", oneCell().toJson());
            assertEquals("<dataset><row><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></row></dataset>", oneCell().toXml());
        }

        // G060-03: the serialized-XML scan tests its markers only at '<', ']', '-' and '&'; references, escaped
        // markers and supplementary characters are accepted or rejected exactly as before.
        @Test
        void testToXml_serializedTextScan() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("tab", "a\tb");
            map.put("nl", "c\nd");
            map.put("del", "e\u007ff");
            map.put("marks", "<![CDATA[x]]> <!-- y --> &#0; &amp; ]]> -->");
            map.put("dash", "2026-10-08");

            final Dataset ok = Dataset.rows(Arrays.asList("id", "m", "l"),
                    new Object[][] { { 1, map, Arrays.asList("p]]>q", "r-->s", "&#1;") }, { 2, Collections.singletonMap("k", "\u00e9\uD83D\uDE00"), null } });

            assertEquals("<dataset><row><id>1</id><m><map><tab>a&#x9;b</tab><nl>c&#xa;d</nl><del>e&#x7f;f</del>"
                    + "<marks>&lt;![CDATA[x]]&gt; &lt;!-- y --&gt; &amp;#0; &amp;amp; ]]&gt; --&gt;</marks><dash>2026-10-08</dash></map></m>"
                    + "<l><list>[&quot;p]]&gt;q&quot;, &quot;r--&gt;s&quot;, &quot;&amp;#1;&quot;]</list></l></row>"
                    + "<row><id>2</id><m><map><k>\u00e9\uD83D\uDE00</k></map></m><l>null</l></row></dataset>", ok.toXml());

            for (final String bad : new String[] { "\u0001", "x\u001fy", "\u0000" }) {
                final Dataset ds = Dataset.rows(Arrays.asList("a", "b"),
                        new Object[][] { { Collections.singletonMap("k", "fine-text"), 1 }, { 2, Collections.singletonMap("k", bad) } });
                final java.io.StringWriter writer = new java.io.StringWriter();
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(writer));

                assertEquals("Cannot export XML: invalid value at row 1, column 'b'", e.getMessage());
                assertEquals("<dataset><row><a><map><k>fine-text</k></map></a><b>1</b></row><row><a>2</a><b>", writer.toString());
            }
        }

        private Dataset abc() {
            return Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });
        }

        // G060-04: append/prepend snapshot each source column once; aliasing sources (self, own slices) still see the
        // pre-write rows, column order is mapped by name, and the appended rows are independent copies.
        @Test
        void testAppendPrepend_snapshotOfAliasingSources() {
            final Dataset self = abc();
            self.append(self);
            assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), self.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "x", "y", "z"), self.getColumn("b"));

            final Dataset prepended = abc();
            prepended.prepend(prepended.slice(1, 3));
            assertEquals(Arrays.asList(2, 3, 1, 2, 3), prepended.getColumn("a"));
            assertEquals(Arrays.asList("y", "z", "x", "y", "z"), prepended.getColumn("b"));

            final Dataset reordered = abc();
            final Dataset other = Dataset.rows(Arrays.asList("b", "a"), new Object[][] { { "p", 9 }, { "q", 8 } });
            reordered.append(other);
            assertEquals(Arrays.asList(1, 2, 3, 9, 8), reordered.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "p", "q"), reordered.getColumn("b"));
            other.set(0, 0, "changed");
            assertEquals("p", reordered.get(3, 1));

            final Dataset empty = abc();
            empty.append(Dataset.rows(Arrays.asList("a", "b"), new Object[0][]));
            assertEquals(3, empty.size());

            final Dataset stale = abc();
            final Dataset slice = stale.slice(0, 2);
            stale.swapRows(0, 1);
            assertThrows(java.util.ConcurrentModificationException.class, () -> stale.append(slice));
            assertEquals(Arrays.asList(2, 1, 3), stale.getColumn("a"));
            assertEquals(Arrays.asList("y", "x", "z"), stale.getColumn("b"));
        }

        // G060-04: merge snapshots the selected source range once; new columns are added and missing ones null-filled.
        @Test
        void testMerge_snapshotOfSelectedRange() {
            final Dataset target = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" } });
            target.merge(Dataset.rows(Arrays.asList("b", "c"), new Object[][] { { "q", true }, { "r", false } }));
            assertEquals(Arrays.asList("a", "b", "c"), target.columnNames());
            assertEquals(Arrays.asList(1, null, null), target.getColumn("a"));
            assertEquals(Arrays.asList("x", "q", "r"), target.getColumn("b"));
            assertEquals(Arrays.asList(null, true, false), target.getColumn("c"));

            final Dataset self = abc();
            self.merge(self, 1, 3, Arrays.asList("b"));
            assertEquals(Arrays.asList(1, 2, 3, null, null), self.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "y", "z"), self.getColumn("b"));

            final Dataset viaSlice = abc();
            viaSlice.merge(viaSlice.slice(0, 2));
            assertEquals(Arrays.asList(1, 2, 3, 1, 2), viaSlice.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "x", "y"), viaSlice.getColumn("b"));
        }

        // G060-04: moveRows copies the moved block once; results for forward, backward, overlapping and no-op moves.
        @Test
        void testMoveRows_blockCopiedOnce() {
            final Object[][] rows = new Object[6][];

            for (int i = 0; i < rows.length; i++) {
                rows[i] = new Object[] { i, "r" + i };
            }

            final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), rows);

            ds.moveRows(0, 2, 4);
            assertEquals(Arrays.asList(2, 3, 4, 5, 0, 1), ds.getColumn("a"));
            assertEquals(Arrays.asList("r2", "r3", "r4", "r5", "r0", "r1"), ds.getColumn("b"));

            ds.moveRows(4, 6, 0);
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), ds.getColumn("a"));

            ds.moveRows(1, 4, 2);
            assertEquals(Arrays.asList(0, 4, 1, 2, 3, 5), ds.getColumn("a"));
            assertEquals(Arrays.asList("r0", "r4", "r1", "r2", "r3", "r5"), ds.getColumn("b"));

            final Dataset slice = ds.slice(0, 2);
            ds.moveRows(2, 2, 0);
            ds.moveRows(0, 6, 0);
            assertEquals(Arrays.asList(0, 4), slice.getColumn("a"));
            assertEquals(Arrays.asList(0, 4, 1, 2, 3, 5), ds.getColumn("a"));

            ds.moveRows(5, 6, 0);
            assertEquals(Arrays.asList(5, 0, 4, 1, 2, 3), ds.getColumn("a"));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getColumn("a").get(0));
        }
    }

    // ---- perf review 2026-10-08 G060 end ----
    // ---- perf review 2026-10-08 G061 begin ----
    @Nested
    class G061 {
        private final Random random = new Random(20261008L);

        private Object randomKey1() {
            switch (random.nextInt(6)) {
                case 0:
                    return null;
                case 1:
                    return "a";
                case 2:
                    return "b";
                case 3:
                    // A fresh array per cell: rows must still match by content (deep equality).
                    return new int[] { 1 };
                case 4:
                    return new int[] { 2 };
                default:
                    return "c";
            }
        }

        private Object randomKey2() {
            final int value = random.nextInt(4);
            return value == 0 ? null : (Object) Integer.valueOf(value);
        }

        private Dataset joinSide(final String idName, final String valueName, final int rows, final int idBase, final boolean uniqueKeys, final int keyOffset) {
            final Object[][] data = new Object[rows][];

            for (int i = 0; i < rows; i++) {
                final Object key1 = uniqueKeys ? "key" + (i + keyOffset) : randomKey1();
                final Object key2 = uniqueKeys ? (Object) Integer.valueOf((i + keyOffset) % 7) : randomKey2();
                data[i] = new Object[] { idBase + i, key1, key2, valueName + i };
            }

            return Dataset.rows(Arrays.asList(idName, "k1", "k2", valueName), data);
        }

        private Map<String, String> on2() {
            final Map<String, String> on = new LinkedHashMap<>();
            on.put("k1", "k1");
            on.put("k2", "k2");
            return on;
        }

        private boolean sameKey(final Dataset left, final int leftRow, final Dataset right, final int rightRow) {
            // Object locals: an inferred get() type would select N.deepEquals(Object[], Object[]).
            final Object left1 = left.get(leftRow, 1);
            final Object right1 = right.get(rightRow, 1);
            final Object left2 = left.get(leftRow, 2);
            final Object right2 = right.get(rightRow, 2);
            return CommonUtil.deepEquals(left1, right1) && CommonUtil.deepEquals(left2, right2);
        }

        private List<Object> rowValues(final Dataset ds, final int row) {
            final List<Object> values = new ArrayList<>();

            for (int c = 0; c < ds.columnCount(); c++) {
                values.add(ds.get(row, c));
            }

            return values;
        }

        private List<List<Object>> newColumns(final int count) {
            final List<List<Object>> columns = new ArrayList<>();

            for (int c = 0; c < count; c++) {
                columns.add(new ArrayList<>());
            }

            return columns;
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

        /** mode 0: fullJoin(right, on); 1: fullJoin(right, on, "rr", List.class); 2: ... with ArrayList::new per left row. */
        private List<List<Object>> expectedFullJoin(final Dataset left, final Dataset right, final int mode) {
            final List<Object> nullLeft = Arrays.asList(null, null, null, null);
            final List<Object> nullRight = mode == 0 ? Arrays.asList(null, null, null, null) : Collections.singletonList(null);
            final List<List<Object>> columns = newColumns(mode == 0 ? 8 : 5);

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

                    addRow(columns, nullLeft, Collections.singletonList(group));
                } else {
                    addRow(columns, nullLeft, mode == 0 ? rowValues(right, j) : Collections.singletonList(rowValues(right, j)));
                }
            }

            return columns;
        }

        private void assertFullJoin(final Dataset left, final Dataset right, final List<String> expectedNames) {
            final Dataset plain = left.fullJoin(right, on2());
            assertEquals(expectedNames, plain.columnNames());
            assertColumns(expectedFullJoin(left, right, 0), plain);

            final Dataset typed = left.fullJoin(right, on2(), "rr", List.class);
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "rr"), typed.columnNames());
            assertColumns(expectedFullJoin(left, right, 1), typed);

            final Dataset collected = left.fullJoin(right, on2(), "rr", List.class, ArrayList::new);
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "rr"), collected.columnNames());
            assertColumns(expectedFullJoin(left, right, 2), collected);
        }

        private void assertColumns(final List<List<Object>> expected, final Dataset actual) {
            assertEquals(expected.size(), actual.columnCount());

            for (int c = 0; c < expected.size(); c++) {
                assertEquals(expected.get(c), new ArrayList<>(actual.getColumn(c)), "column " + actual.columnNames().get(c));
            }
        }

        // G061-01: multi-key fullJoin (plain, row-typed column, one collection per left row) against a naive nested-loop
        // oracle: duplicate, null and content-equal array keys, unmatched rows on both sides, empty sides.
        @Test
        void testFullJoin_multiKeyMatchesNaiveOracle() {
            final List<String> names = Arrays.asList("id", "k1", "k2", "v", "rid", "k1_2", "k2_2", "r");
            final int[] sizes = { 0, 1, 2, 5, 17, 40 };

            for (final int leftSize : sizes) {
                for (final int rightSize : sizes) {
                    final Dataset left = joinSide("id", "v", leftSize, 0, false, 0);
                    final Dataset right = joinSide("rid", "r", rightSize, 1000, false, 0);
                    assertFullJoin(left, right, names);
                }
            }

            // Hundreds of distinct keys (the left key set grows past its initial table), 10% unmatched on each side.
            final Dataset left = joinSide("id", "v", 300, 0, true, 0);
            final Dataset right = joinSide("rid", "r", 300, 1000, true, 30);
            assertFullJoin(left, right, names);
            assertEquals(330, left.fullJoin(right, on2()).size());
        }

        // G061-01: self join and an all-duplicate left side (every left row after the first finds its key already recorded).
        @Test
        void testFullJoin_multiKeySelfJoinAndDuplicateLeftKeys() {
            final Dataset left = joinSide("id", "v", 30, 0, false, 0);
            final Dataset self = left.fullJoin(left, on2());
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "id_2", "k1_2", "k2_2", "v_2"), self.columnNames());
            assertColumns(expectedFullJoin(left, left, 0), self);

            final Object[][] same = new Object[6][];

            for (int i = 0; i < same.length; i++) {
                same[i] = new Object[] { i, new int[] { 7 }, null, "v" + i };
            }

            final Dataset duplicates = Dataset.rows(Arrays.asList("id", "k1", "k2", "v"), same);
            final Dataset right = Dataset.rows(Arrays.asList("rid", "k1", "k2", "r"),
                    new Object[][] { { 100, new int[] { 7 }, null, "r0" }, { 101, "x", 1, "r1" }, { 102, new int[] { 7 }, null, "r2" } });
            assertFullJoin(duplicates, right, Arrays.asList("id", "k1", "k2", "v", "rid", "k1_2", "k2_2", "r"));
            assertEquals(Arrays.asList(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, null), new ArrayList<>(duplicates.fullJoin(right, on2()).getColumn("id")));
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, null),
                    new ArrayList<>(duplicates.fullJoin(right, on2(), "rr", List.class, ArrayList::new).getColumn("id")));
        }

        @SuppressWarnings("unchecked")
        private List<String> keyColumnNames(final Dataset ds, final Dataset other) throws Exception {
            final java.lang.reflect.Method method = RowDataset.class.getDeclaredMethod("getKeyColumnNames", Dataset.class);
            method.setAccessible(true);

            try {
                return (List<String>) method.invoke(ds, other);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw e;
            }
        }

        private Dataset oneRow(final List<String> names) {
            final Object[] row = new Object[names.size()];

            for (int i = 0; i < row.length; i++) {
                row[i] = names.get(i).hashCode() % 5;
            }

            return Dataset.rows(names, new Object[][] { row });
        }

        private List<String> retainedNames(final Dataset ds, final Dataset other) {
            // The pre-change algorithm, as an oracle.
            final List<String> expected = new ArrayList<>(ds.columnNames());
            expected.retainAll(new ArrayList<>(other.columnNames()));
            return expected;
        }

        // G061-02: the key columns of the key-less set operations are this Dataset's names that other also has, in this
        // Dataset's order, in a fresh mutable ArrayList; no overlap keeps the same message.
        @Test
        void testGetKeyColumnNames_orderOverlapErrorsAndColumnIndexSync() throws Exception {
            final Dataset abcd = oneRow(Arrays.asList("a", "b", "c", "d"));
            final Dataset dxby = oneRow(Arrays.asList("d", "x", "b", "y"));
            final List<String> common = keyColumnNames(abcd, dxby);
            assertEquals(Arrays.asList("b", "d"), common);
            assertEquals(ArrayList.class, common.getClass());
            common.add("z");
            assertEquals(Arrays.asList("b", "d"), keyColumnNames(abcd, dxby));
            assertEquals(Arrays.asList("d", "b"), keyColumnNames(dxby, abcd));
            assertEquals(Arrays.asList("a", "b", "c", "d"), keyColumnNames(abcd, abcd));

            final Dataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
            assertEquals(Collections.emptyList(), keyColumnNames(noColumns, new RowDataset(new ArrayList<>(), new ArrayList<>())));
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], [x]",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, oneRow(Arrays.asList("x")))).getMessage());
            assertEquals("These two Datasets do not have any common column names: [], [a, b, c, d]",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(noColumns, abcd)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], []",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, noColumns)).getMessage());
            assertEquals("'other' cannot be null", assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, null)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], [x]",
                    assertThrows(IllegalArgumentException.class, () -> abcd.semiJoin(oneRow(Arrays.asList("x")))).getMessage());

            // other's column index must follow every kind of column change (patched in place or rebuilt).
            final Dataset other = oneRow(Arrays.asList("d", "x", "b", "y"));
            assertEquals(retainedNames(abcd, other), keyColumnNames(abcd, other));
            other.renameColumn("b", "z");
            assertEquals(Arrays.asList("d"), keyColumnNames(abcd, other));
            other.addColumn("a", Arrays.asList(1));
            assertEquals(Arrays.asList("a", "d"), keyColumnNames(abcd, other));
            other.addColumn(0, "c", Arrays.asList(1));
            assertEquals(Arrays.asList("a", "c", "d"), keyColumnNames(abcd, other));
            other.swapColumns("c", "z");
            other.moveColumn("a", 0);
            assertEquals(Arrays.asList("a", "c", "d"), keyColumnNames(abcd, other));
            other.removeColumn("d");
            assertEquals(Arrays.asList("a", "c"), keyColumnNames(abcd, other));
            other.renameColumn("z", "b");
            assertEquals(Arrays.asList("a", "b", "c"), keyColumnNames(abcd, other));
            assertEquals(retainedNames(abcd, other), keyColumnNames(abcd, other));
            assertEquals(Arrays.asList("b", "c"), keyColumnNames(abcd, other.slice(0, 1, Arrays.asList("x", "c", "b"))));

            // Wide: 300 columns against a reversed subset with extra names.
            final List<String> wide = new ArrayList<>();
            final List<String> partial = new ArrayList<>();

            for (int i = 0; i < 300; i++) {
                wide.add("c" + i);

                if (i % 7 != 3) {
                    partial.add(0, "c" + i);
                }
            }

            partial.add(150, "extra1");
            partial.add("extra2");
            final Dataset wideDs = oneRow(wide);
            final Dataset partialDs = oneRow(partial);
            assertEquals(retainedNames(wideDs, partialDs), keyColumnNames(wideDs, partialDs));
            assertEquals(retainedNames(partialDs, wideDs), keyColumnNames(partialDs, wideDs));
        }

        private Dataset table(final List<String> names, final Object[][] rows) {
            return Dataset.rows(names, rows);
        }

        // G061-02: every key-less set operation equals its explicit-key overload with the common columns (reordered and
        // partially overlapping schemas, duplicates, nulls).
        @Test
        void testKeylessSetOperations_matchExplicitKeyOverloads() {
            final Dataset left = table(Arrays.asList("id", "k", "v", "onlyLeft"), new Object[][] { { 1, "a", 10, "l1" }, { 2, "b", null, "l2" },
                    { 2, "b", null, "l3" }, { 3, null, 30, "l4" }, { 4, "d", 40, "l5" }, { 1, "a", 10, "l6" } });
            final Dataset right = table(Arrays.asList("v", "x", "k", "id"),
                    new Object[][] { { 10, "x1", "a", 1 }, { null, "x2", "b", 2 }, { 30, "x3", null, 3 }, { 99, "x4", "z", 9 }, { 10, "x5", "a", 1 } });
            final List<String> keys = Arrays.asList("id", "k", "v");

            assertEquals(left.semiJoin(right, keys), left.semiJoin(right));
            assertEquals(left.antiJoin(right, keys), left.antiJoin(right));
            assertEquals(left.unionBy(right, keys, false), left.union(right, false));
            assertEquals(left.intersectBy(right, keys, false), left.intersect(right, false));
            assertEquals(left.intersectAllBy(right, keys, false), left.intersectAll(right, false));
            assertEquals(left.exceptBy(right, keys, false), left.except(right, false));
            assertEquals(left.exceptAllBy(right, keys, false), left.exceptAll(right, false));
            assertEquals(Arrays.asList(1, 2, 2, 3, 1), new ArrayList<>(left.semiJoin(right).getColumn("id")));
            assertEquals(Arrays.asList(4), new ArrayList<>(left.antiJoin(right).getColumn("id")));
            assertEquals(Arrays.asList(1, 2, 3, 4, 9), new ArrayList<>(left.union(right, false).getColumn("id")));

            // Same column set in another order: the requiresSameColumns=true overloads accept it.
            final Dataset reordered = table(Arrays.asList("onlyLeft", "v", "id", "k"), new Object[][] { { "l1", 10, 1, "a" }, { "zz", 40, 4, "d" } });
            final List<String> all = Arrays.asList("id", "k", "v", "onlyLeft");
            assertEquals(left.unionBy(reordered, all, true), left.union(reordered));
            assertEquals(left.intersectBy(reordered, all, true), left.intersect(reordered));
            assertEquals(left.intersectAllBy(reordered, all, true), left.intersectAll(reordered));
            assertEquals(left.exceptBy(reordered, all, true), left.except(reordered));
            assertEquals(left.exceptAllBy(reordered, all, true), left.exceptAll(reordered));
            assertEquals(Arrays.asList("l1"), new ArrayList<>(left.intersectAll(reordered).getColumn("onlyLeft")));
            assertEquals(Arrays.asList("l2", "l3", "l4", "l5", "l6"), new ArrayList<>(left.exceptAll(reordered).getColumn("onlyLeft")));
        }

        // G061-03: the same-column-set check of append/prepend/merge(.., true) and of the requiresSameColumns set operations:
        // order-insensitive, count-sensitive, same message, and following every change of this Dataset's columns.
        @Test
        void testRequiresSameColumns_appendPrependMergeAndSetOperations() {
            final Dataset target = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            target.append(table(Arrays.asList("c", "a", "b"), new Object[][] { { 30, 10, 20 } }));
            target.prepend(table(Arrays.asList("b", "c", "a"), new Object[][] { { -2, -3, -1 } }));
            target.merge(table(Arrays.asList("b", "a", "c"), new Object[][] { { 200, 100, 300 } }), true);
            assertEquals(Arrays.asList(-1, 1, 10, 100), new ArrayList<>(target.getColumn("a")));
            assertEquals(Arrays.asList(-3, 3, 30, 300), new ArrayList<>(target.getColumn("c")));

            final Dataset abd = table(Arrays.asList("a", "b", "d"), new Object[][] { { 1, 2, 4 } });
            final Dataset ab = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 } });
            final Dataset abcd = table(Arrays.asList("a", "b", "c", "d"), new Object[][] { { 1, 2, 3, 4 } });
            final String prefix = "These two Datasets do not have the same column names: [a, b, c], ";
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.append(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.prepend(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.merge(abd, true)).getMessage());
            assertEquals(prefix + "[a, b]", assertThrows(IllegalArgumentException.class, () -> target.append(ab)).getMessage());
            assertEquals(prefix + "[a, b, c, d]", assertThrows(IllegalArgumentException.class, () -> target.append(abcd)).getMessage());
            assertEquals(4, target.size());

            final Dataset abc = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.union(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.intersect(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.except(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.intersectAll(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.exceptAll(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.unionBy(abd, Arrays.asList("a"), true)).getMessage());
            abc.merge(abd, false);
            assertEquals(Arrays.asList("a", "b", "c", "d"), abc.columnNames());

            // This Dataset's column index must follow renames, swaps, moves, appends, inserts and removals.
            final Dataset ds = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            assertTrue(ds.containsColumn("a"));
            ds.renameColumn("b", "z");
            ds.append(table(Arrays.asList("z", "a", "c"), new Object[][] { { 21, 11, 31 } }));
            assertThrows(IllegalArgumentException.class, () -> ds.append(table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } })));
            ds.swapColumns("a", "c");
            ds.moveColumn("z", 0);
            assertTrue(ds.containsColumn("z"));
            ds.addColumn("e", Arrays.asList(51, 52));
            ds.append(table(Arrays.asList("e", "c", "a", "z"), new Object[][] { { 53, 32, 12, 22 } }));
            ds.addColumn(1, "f", Arrays.asList(61, 62, 63));
            ds.prepend(table(Arrays.asList("f", "e", "c", "a", "z"), new Object[][] { { 60, 50, 30, 10, 20 } }));
            ds.removeColumn("c");
            assertThrows(IllegalArgumentException.class, () -> ds.append(table(Arrays.asList("f", "e", "c", "a"), new Object[][] { { 6, 5, 3, 1 } })));
            ds.append(table(Arrays.asList("a", "e", "f", "z"), new Object[][] { { 13, 54, 64, 23 } }));
            assertEquals(Arrays.asList("z", "f", "a", "e"), ds.columnNames());
            assertEquals(Arrays.asList(10, 1, 11, 12, 13), new ArrayList<>(ds.getColumn("a")));
            assertEquals(Arrays.asList(20, 2, 21, 22, 23), new ArrayList<>(ds.getColumn("z")));
            assertEquals(Arrays.asList(50, 51, 52, 53, 54), new ArrayList<>(ds.getColumn("e")));
            assertEquals(Arrays.asList(60, 61, 62, 63, 64), new ArrayList<>(ds.getColumn("f")));

            // Wide schemas, reversed: values land in the right columns; one differing name still fails.
            final List<String> wide = new ArrayList<>();
            final Object[] wideRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                wide.add("c" + i);
                wideRow[i] = i;
            }

            final List<String> reversed = new ArrayList<>(wide);
            Collections.reverse(reversed);
            final Object[] reversedRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                reversedRow[i] = 1000 + 299 - i;
            }

            final Dataset wideDs = table(wide, new Object[][] { wideRow });
            wideDs.append(table(reversed, new Object[][] { reversedRow }));
            assertEquals(Arrays.asList(0, 1000), new ArrayList<>(wideDs.getColumn("c0")));
            assertEquals(Arrays.asList(299, 1299), new ArrayList<>(wideDs.getColumn("c299")));
            final List<String> almost = new ArrayList<>(reversed);
            almost.set(299, "other");
            assertThrows(IllegalArgumentException.class, () -> wideDs.append(table(almost, new Object[][] { reversedRow })));
            assertEquals(2, wideDs.size());
        }

        // G061-02/03: an invalidated slice on either side still fails with ConcurrentModificationException.
        @Test
        void testColumnNameChecks_invalidatedSliceStillFailsFast() {
            final Dataset parent = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
            final Dataset slice = parent.slice(0, 1);
            final Dataset other = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 } });
            assertEquals(1, other.semiJoin(slice).size());
            parent.addRow(new Object[] { 5, 6 });

            assertThrows(java.util.ConcurrentModificationException.class, () -> other.semiJoin(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.antiJoin(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.union(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.intersect(slice, false));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.exceptAll(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.semiJoin(other));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.union(other));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.append(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.merge(slice, true));
            final Dataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
            assertThrows(java.util.ConcurrentModificationException.class, () -> noColumns.semiJoin(slice));
            assertEquals(1, other.size());
        }
    }
}
