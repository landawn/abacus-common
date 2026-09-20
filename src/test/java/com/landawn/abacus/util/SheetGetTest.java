package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.Sheet.Point;

public class SheetGetTest extends SheetTestSupport {
    @Test
    public void testGetWithKeys() {
        assertEquals(Integer.valueOf(1), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(5), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(9), sheet.get("row3", "col3"));
    }

    @Test
    public void testGetWithIndices() {
        assertEquals(Integer.valueOf(1), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(5), sheet.getAt(1, 1));
        assertEquals(Integer.valueOf(9), sheet.getAt(2, 2));
    }

    @Test
    public void testGetWithPoint() {
        Point point = Point.of(0, 0);
        assertEquals(Integer.valueOf(1), sheet.get(point));

        Point point2 = Point.of(1, 1);
        assertEquals(Integer.valueOf(5), sheet.get(point2));
    }

    @Test
    public void testGetWithPoint_Valid() {
        Point p = Point.of(0, 0);
        assertEquals(Integer.valueOf(1), sheet.get(p));
        assertEquals(Integer.valueOf(5), sheet.get(Point.of(1, 1)));
        assertEquals(Integer.valueOf(9), sheet.get(Point.of(2, 2)));
    }

    @Test
    public void testGetUninitializedSheet() {
        Sheet<String, String, Integer> s = new Sheet<>(rowKeys, columnKeys);
        assertNull(s.get("row1", "col1"));
        assertNull(s.getAt(0, 0));
    }

    @Test
    public void testGetRow() {
        List<Integer> row = sheet.rowValues("row1");
        assertNotNull(row);
        assertTrue(row instanceof ImmutableList);
        assertTrue(((ImmutableList<?>) row).list instanceof java.util.AbstractList);
        assertFalse(((ImmutableList<?>) row).list instanceof ArrayList);
        assertEquals(3, row.size());
        assertEquals(Integer.valueOf(1), row.get(0));
        assertEquals(Integer.valueOf(2), row.get(1));
        assertEquals(Integer.valueOf(3), row.get(2));

        sheet.set("row1", "col2", 20);
        assertEquals(Integer.valueOf(20), row.get(1));
    }

    @Test
    public void testGetColumn() {
        ImmutableList<Integer> column = sheet.columnValues("col1");
        assertNotNull(column);
        assertEquals(3, column.size());
        assertEquals(Integer.valueOf(1), column.get(0));
        assertEquals(Integer.valueOf(4), column.get(1));
        assertEquals(Integer.valueOf(7), column.get(2));
    }

    @Test
    public void testGetByKeys() {
        assertEquals("V11", objectSheet.get("R1", "C1"));
        assertEquals(100, objectSheet.get("R2", "C1"));
        assertNull(objectSheet.get("R1", "C3"));
    }

    @Test
    public void testGetByKeys_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.get("R1", "C1"));
    }

    @Test
    public void testGetByIndices() {
        assertEquals("V11", objectSheet.getAt(0, 0));
        assertEquals(100, objectSheet.getAt(1, 0));
        assertNull(objectSheet.getAt(0, 2));
    }

    @Test
    public void testGetByIndices_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitializedSheet.getAt(0, 0));
    }

    @Test
    public void testGetByPoint() {
        assertEquals("V11", objectSheet.get(Sheet.Point.of(0, 0)));
        assertNull(objectSheet.get(Sheet.Point.of(0, 2)));
    }

    @Test
    public void testGetRow_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(upperRowKeys, colKeys);
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.rowValues("R1")));
    }

    @Test
    public void testGetColumn_uninitializedSheet() {
        Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
        assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.columnValues("C1")));
    }

    @Test
    public void testGetPut() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitSheet.get("R1", "C1"));
        uninitSheet.set("R1", "C1", 100);
        assertEquals(Integer.valueOf(100), uninitSheet.get("R1", "C1"));
    }

    @Test
    public void testGetPutByIndex() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        assertNull(uninitSheet.getAt(0, 0));
        uninitSheet.setAt(0, 0, 100);
        assertEquals(Integer.valueOf(100), uninitSheet.getAt(0, 0));
    }

    @Test
    public void testGetPutByPoint() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        Point point = Point.of(1, 1);
        assertNull(uninitSheet.get(point));
        uninitSheet.set(point, 200);
        assertEquals(Integer.valueOf(200), uninitSheet.get(point));
    }

    @Test
    public void testGetWithInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("invalidRow", "col1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.get("row1", "invalidCol");
        });
    }

    @Test
    public void testGetWithInvalidIndices() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.getAt(-1, 0);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            sheet.getAt(0, 10);
        });
    }

    @Test
    public void testGetRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.rowValues("invalidRow");
        });

        Sheet<String, String, Integer> uninitializedSheet = new Sheet<>(rowKeys, columnKeys);
        assertThrows(IllegalArgumentException.class, () -> {
            uninitializedSheet.rowValues("invalidRow");
        });
    }

    @Test
    public void testGetColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.columnValues("invalidCol");
        });
    }

    @Test
    public void testGetByKeys_invalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.get("RX", "C1"));
        assertThrows(IllegalArgumentException.class, () -> objectSheet.get("R1", "CX"));
    }

    @Test
    public void testGetByIndices_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.getAt(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.getAt(0, 5));
    }

    @Test
    public void testGetByPoint_outOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> objectSheet.get(Sheet.Point.of(5, 0)));
    }

    @Test
    public void testGetRow_invalidKey() {
        assertThrows(IllegalArgumentException.class, () -> objectSheet.rowValues("RX"));
    }

    @Test
    public void testGetWithInvalidRowKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("InvalidRow", "C1"));
    }

    @Test
    public void testGetWithInvalidColumnKey() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("R1", "InvalidColumn"));
    }

    @Test
    public void testGetWithInvalidRowIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.getAt(10, 0));
    }

    @Test
    public void testGetWithInvalidColumnIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.getAt(0, 10));
    }

    @Test
    public void testGet_AbsentRowKey_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("missing", "col1"));
    }

    @Test
    public void testGet_AbsentColumnKey_ThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> sheet.get("row1", "missing"));
    }

    @Test
    public void testMissingKeyMessageRendersArrayKeys() {
        // Missing raw arrays need both readable contents and their identity-based matching rule.
        final int[] absentKey = { 9, 9 };
        final String absentDescription = "[9, 9]@" + Integer.toHexString(System.identityHashCode(absentKey));
        final Sheet<int[], int[], Integer> initialized = Sheet.rows(Arrays.asList(new int[] { 1, 2 }), Arrays.asList(new int[] { 7, 8 }),
                new Integer[][] { { 1 } });

        // Keep the canonical missing-key form once; both initialized and uninitialized paths must carry the same information.
        assertEquals("No row found by key: " + absentDescription + " (array keys match by identity)",
                assertThrows(IllegalArgumentException.class, () -> initialized.get(absentKey, new int[] { 7, 8 })).getMessage());
        assertArrayKeyDiagnostic(
                assertThrows(IllegalArgumentException.class, () -> initialized.get(initialized.rowKeySet().iterator().next(), absentKey)).getMessage(),
                "column", absentDescription);

        // checkRowKey / checkColumnKey - the uninitialized path.
        final Sheet<int[], int[], Integer> uninitialized = new Sheet<>(Arrays.asList(new int[] { 1, 2 }), Arrays.asList(new int[] { 7, 8 }));
        assertArrayKeyDiagnostic(assertThrows(IllegalArgumentException.class, () -> uninitialized.get(absentKey, new int[] { 7, 8 })).getMessage(), "row",
                absentDescription);
        assertArrayKeyDiagnostic(
                assertThrows(IllegalArgumentException.class, () -> uninitialized.get(uninitialized.rowKeySet().iterator().next(), absentKey)).getMessage(),
                "column", absentDescription);

        // A plain key still renders exactly as it did.
        assertEquals("No row found by key: missing", assertThrows(IllegalArgumentException.class, () -> sheet.get("missing", "col1")).getMessage());
        assertEquals("No column found by key: missing", assertThrows(IllegalArgumentException.class, () -> sheet.get("row1", "missing")).getMessage());
    }

    @Test
    public void testMissingKeyMessageIsBoundedForHugeKeys() {
        // Companion to testMissingKeyMessageRendersArrayKeys: rendering the key element by element made the
        // message unbounded in the size of the key on the most frequent failure path in the class, so the
        // rendering is capped - a prefix plus a count of the elements dropped, then a hard character cap.
        final int[] hugeArrayKey = new int[200_000];

        for (int i = 0; i < hugeArrayKey.length; i++) {
            hugeArrayKey[i] = i;
        }

        final Sheet<int[], String, Integer> arrayKeyed = Sheet.rows(Arrays.asList(new int[] { 1, 2 }), Arrays.asList("c1"), new Integer[][] { { 1 } });
        final String arrayMessage = assertThrows(IllegalArgumentException.class, () -> arrayKeyed.get(hugeArrayKey, "c1")).getMessage();
        assertTrue(arrayMessage.startsWith("No row found by key: [0, 1, 2, "), arrayMessage);
        assertTrue(arrayMessage.contains(" more)]@" + Integer.toHexString(System.identityHashCode(hugeArrayKey))), arrayMessage);
        assertTrue(arrayMessage.endsWith(" (array keys match by identity)"), arrayMessage);
        assertTrue(arrayMessage.length() < 300, "message length " + arrayMessage.length());

        // A key that is huge without being a long array is caught by the character cap instead.
        final String hugeStringKey = "k".repeat(100_000);
        final String stringMessage = assertThrows(IllegalArgumentException.class, () -> sheet.get(hugeStringKey, "col1")).getMessage();
        assertTrue(stringMessage.startsWith("No row found by key: kkk"), stringMessage.substring(0, 40));
        assertTrue(stringMessage.endsWith("..."), stringMessage.substring(stringMessage.length() - 40));
        assertTrue(stringMessage.length() < 600, "message length " + stringMessage.length());

        // The hard character cap must not remove the identity hint from a short array with a huge element.
        final Object[] hugeElement = { hugeStringKey };
        final Sheet<Object, String, Integer> generic = new Sheet<>(List.of("row"), List.of("column"));
        final String nestedMessage = assertThrows(IllegalArgumentException.class, () -> generic.get(hugeElement, "column")).getMessage();
        assertTrue(nestedMessage.contains("...@" + Integer.toHexString(System.identityHashCode(hugeElement))), nestedMessage);
        assertTrue(nestedMessage.endsWith(" (array keys match by identity)"), nestedMessage);
        assertTrue(nestedMessage.length() < 600, "message length " + nestedMessage.length());
    }

    @Test
    public void testKeySetMessagesRenderArrayKeys() {
        // Set diagnostics must distinguish keys whose contents are identical but whose identities differ.
        final int[] rowKey = { 1, 2 };
        final int[] columnKey = { 7, 8 };
        final int[] absent = { 1, 2 };
        final String rowDescription = "[1, 2]@" + Integer.toHexString(System.identityHashCode(rowKey));
        final String columnDescription = "[7, 8]@" + Integer.toHexString(System.identityHashCode(columnKey));
        final String absentDescription = "[1, 2]@" + Integer.toHexString(System.identityHashCode(absent));
        final String identityNote = " (array keys match by identity)";
        final Sheet<int[], int[], Integer> s = Sheet.rows(Arrays.asList(rowKey), Arrays.asList(columnKey), new Integer[][] { { 1 } });

        // copy and println share this diagnostic family; pin its full format only once.
        assertEquals("Row keys: [" + absentDescription + "] are not included in this sheet row keys: [" + rowDescription + "]" + identityNote,
                assertThrows(IllegalArgumentException.class, () -> s.copy(Arrays.asList(absent), Arrays.asList(columnKey))).getMessage());
        assertArrayKeyDiagnostic(assertThrows(IllegalArgumentException.class, () -> s.copy(Arrays.asList(rowKey), Arrays.asList(absent))).getMessage(),
                "column", absentDescription, columnDescription);

        assertArrayKeyDiagnostic(
                assertThrows(IllegalArgumentException.class, () -> s.println(Arrays.asList(absent), Arrays.asList(columnKey), new StringBuilder()))
                        .getMessage(),
                "row", absentDescription, rowDescription);
        assertArrayKeyDiagnostic(
                assertThrows(IllegalArgumentException.class, () -> s.println(Arrays.asList(rowKey), Arrays.asList(absent), new StringBuilder())).getMessage(),
                "column", absentDescription, columnDescription);

        // Plain keys still render exactly as they did.
        assertEquals("Row keys: [nope] are not included in this sheet row keys: [row1, row2, row3]",
                assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("nope"), Arrays.asList("col1"))).getMessage());
        assertEquals("Column keys: [nope] are not included in this sheet Column keys: [col1, col2, col3]",
                assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("row1"), Arrays.asList("nope"))).getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 32, 40 })
    public void testKeySetMessagesExplainArrayIdentityOnceAndKeepTheElementCap(final int keyCount) {
        final List<Object> keys = new ArrayList<>();
        for (int i = 0; i < keyCount; i++) {
            keys.add(new int[] { i });
        }
        final Object absent = new int[] { 0 };
        for (final boolean rows : new boolean[] { true, false }) {
            final Sheet<Object, Object, Integer> target = new Sheet<>(rows ? keys : List.of("r"), rows ? List.of("c") : keys);
            final String message = assertThrows(IllegalArgumentException.class,
                    () -> target.copy(rows ? List.of(absent) : List.of("r"), rows ? List.of("c") : List.of(absent))).getMessage();
            final String note = " (array keys match by identity)";
            assertTrue(message.endsWith(note), message);
            assertEquals(message.indexOf(note), message.lastIndexOf(note), message);
            assertTrue(message.contains("[0]@" + Integer.toHexString(System.identityHashCode(absent))), message);
            for (int i = 0; i < Math.min(32, keyCount); i++) {
                assertTrue(message.contains("[" + i + "]@" + Integer.toHexString(System.identityHashCode(keys.get(i)))), message);
            }
            if (keyCount > 32) {
                assertTrue(message.contains("...(8 more)"), message);
                assertFalse(message.contains("[32]@"), message);
            }
            assertTrue(message.length() < 1_024, "message length " + message.length());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testKeySetMessagesKeepIdentityAndCharacterBoundsWithArraysOnEitherSide(final boolean requestedArray) {
        final Object arrayKey = new Object[] { "x".repeat(100_000) };
        final Object requested = requestedArray ? arrayKey : "missing";
        final Object existing = requestedArray ? "existing" : arrayKey;
        final Sheet<Object, String, Integer> target = new Sheet<>(List.of(existing), List.of("c"));
        final String message = assertThrows(IllegalArgumentException.class, () -> target.copy(List.of(requested), List.of("c"))).getMessage();
        final String note = " (array keys match by identity)";
        assertTrue(message.contains("...@" + Integer.toHexString(System.identityHashCode(arrayKey))), message);
        assertTrue(message.endsWith(note), message);
        assertEquals(message.indexOf(note), message.lastIndexOf(note), message);
        assertTrue(message.length() < 650, "message length " + message.length());
    }
}
