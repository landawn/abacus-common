package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Sheet200Test extends TestBase {

    private List<String> rowKeys;
    private List<String> colKeys;
    private Sheet<String, String, Object> sheet;
    private Sheet<String, String, Integer> intSheet;

    @BeforeEach
    public void setUp() {
        rowKeys = Arrays.asList("R1", "R2", "R3");
        colKeys = Arrays.asList("C1", "C2", "C3");
        sheet = new Sheet<>(rowKeys, colKeys);
        intSheet = new Sheet<>(rowKeys, colKeys);

        sheet.set("R1", "C1", "V11");
        sheet.set("R1", "C2", "V12");
        sheet.set("R2", "C1", 100);
        sheet.set("R2", "C3", true);

        intSheet.set("R1", "C1", 11);
        intSheet.set("R1", "C2", 12);
        intSheet.set("R1", "C3", 13);
        intSheet.set("R2", "C1", 21);
        intSheet.set("R2", "C2", 22);
        intSheet.set("R2", "C3", 23);
        intSheet.set("R3", "C1", 31);
        intSheet.set("R3", "C2", 32);
        intSheet.set("R3", "C3", 33);
    }

    @Nested
    @DisplayName("Constructors and Factory Methods")
    public class ConstructorsAndFactoryMethods {

        @Test
        public void testDefaultConstructor() {
            Sheet<String, String, String> emptySheet = new Sheet<>();
            assertTrue(emptySheet.isEmpty());
            assertEquals(0, emptySheet.rowCount());
            assertEquals(0, emptySheet.columnCount());
            assertTrue(emptySheet.rowKeySet().isEmpty());
            assertTrue(emptySheet.columnKeySet().isEmpty());
        }

        @Test
        public void testConstructorWithKeys() {
            Sheet<String, String, String> newSheet = new Sheet<>(rowKeys, colKeys);
            assertFalse(newSheet.isEmpty());
            assertEquals(3, newSheet.rowCount());
            assertEquals(3, newSheet.columnCount());
            assertEquals(new LinkedHashSet<>(rowKeys), new LinkedHashSet<>(newSheet.rowKeySet()));
            assertEquals(new LinkedHashSet<>(colKeys), new LinkedHashSet<>(newSheet.columnKeySet()));
            assertNull(newSheet.get("R1", "C1"));
        }

        @Test
        public void testConstructorWithKeys_nullInKeysThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> new Sheet<>(Arrays.asList("R1", null), colKeys));
            assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rowKeys, Arrays.asList("C1", null)));
        }

        @Test
        public void testConstructorWithKeysAndDataArray() {
            Object[][] data = { { "V11", "V12" }, { "V21", "V22" } };
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, Object> dataSheet = new Sheet<>(rk, ck, data);

            assertEquals(2, dataSheet.rowCount());
            assertEquals(2, dataSheet.columnCount());
            assertEquals("V11", dataSheet.get("R1", "C1"));
            assertEquals("V12", dataSheet.get("R1", "C2"));
            assertEquals("V21", dataSheet.get("R2", "C1"));
            assertEquals("V22", dataSheet.get("R2", "C2"));
        }

        @Test
        public void testConstructorWithKeysAndEmptyDataArray() {
            Object[][] data = {};
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, Object> dataSheet = new Sheet<>(rk, ck, data);
            assertEquals(2, dataSheet.rowCount());
            assertEquals(2, dataSheet.columnCount());
            assertNull(dataSheet.get("R1", "C1"));
        }

        @Test
        public void testConstructorWithKeysAndDataArray_mismatchDimensions() {
            Object[][] dataMismatchRow = { { "V11", "V12" } };
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rk, ck, dataMismatchRow));

            Object[][] dataMismatchCol = { { "V11" }, { "V21" } };
            assertThrows(IllegalArgumentException.class, () -> new Sheet<>(rk, ck, dataMismatchCol));
        }

        @Test
        public void testEmptyFactory() {
            Sheet<String, String, String> emptySheet = Sheet.empty();
            assertTrue(emptySheet.isEmpty());
            assertTrue(emptySheet.isFrozen());
            assertEquals(0, emptySheet.rowCount());
            assertEquals(0, emptySheet.columnCount());
        }

        @Test
        public void testRowsFactory_fromArray() {
            Object[][] data = { { "V11", "V12" }, { "V21", "V22" } };
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, Object> dataSheet = Sheet.rows(rk, ck, data);

            assertEquals("V11", dataSheet.get("R1", "C1"));
            assertEquals("V22", dataSheet.get("R2", "C2"));
        }

        @Test
        public void testRowsFactory_fromCollection() {
            List<List<String>> rowsData = Arrays.asList(Arrays.asList("V11", "V12"), Arrays.asList("V21", "V22"));
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, String> dataSheet = Sheet.rows(rk, ck, rowsData);

            assertEquals("V11", dataSheet.get("R1", "C1"));
            assertEquals("V22", dataSheet.get("R2", "C2"));
        }

        @Test
        public void testRowsFactory_fromEmptyCollection() {
            List<List<String>> rowsData = Collections.emptyList();
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, String> dataSheet = Sheet.rows(rk, ck, rowsData);
            assertEquals(2, dataSheet.rowCount());
            assertEquals(2, dataSheet.columnCount());
            assertNull(dataSheet.get("R1", "C1"));
        }

        @Test
        public void testRowsFactory_fromCollection_mismatchDimensions() {
            List<List<String>> rowsDataMismatchRow = Arrays.asList(Arrays.asList("V11", "V12"));
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchRow));

            List<List<String>> rowsDataMismatchCol = Arrays.asList(Arrays.asList("V11"), Arrays.asList("V21"));
            assertThrows(IllegalArgumentException.class, () -> Sheet.rows(rk, ck, rowsDataMismatchCol));
        }

        @Test
        public void testColumnsFactory_fromArray() {
            Object[][] data = { { "V11", "V21" }, { "V12", "V22" } };
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, Object> dataSheet = Sheet.columns(rk, ck, data);

            assertEquals("V11", dataSheet.get("R1", "C1"));
            assertEquals("V22", dataSheet.get("R2", "C2"));
        }

        @Test
        public void testColumnsFactory_fromCollection() {
            List<List<String>> colsData = Arrays.asList(Arrays.asList("V11", "V21"), Arrays.asList("V12", "V22"));
            List<String> rk = Arrays.asList("R1", "R2");
            List<String> ck = Arrays.asList("C1", "C2");
            Sheet<String, String, String> dataSheet = Sheet.columns(rk, ck, colsData);

            assertEquals("V11", dataSheet.get("R1", "C1"));
            assertEquals("V22", dataSheet.get("R2", "C2"));
        }
    }

    @Nested
    @DisplayName("Key Set Methods")
    public class KeySetMethods {
        @Test
        public void testRowKeySet() {
            assertEquals(new LinkedHashSet<>(rowKeys), new LinkedHashSet<>(sheet.rowKeySet()));
            assertThrows(UnsupportedOperationException.class, () -> sheet.rowKeySet().add("R4"));
        }

        @Test
        public void testColumnKeySet() {
            assertEquals(new LinkedHashSet<>(colKeys), new LinkedHashSet<>(sheet.columnKeySet()));
            assertThrows(UnsupportedOperationException.class, () -> sheet.columnKeySet().add("C4"));
        }
    }

    @Nested
    @DisplayName("Get Methods")
    public class GetMethods {
        @Test
        public void testGetByKeys() {
            assertEquals("V11", sheet.get("R1", "C1"));
            assertEquals(100, sheet.get("R2", "C1"));
            assertNull(sheet.get("R1", "C3"));
        }

        @Test
        public void testGetByKeys_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertNull(uninitializedSheet.get("R1", "C1"));
        }

        @Test
        public void testGetByKeys_invalidKeys() {
            assertThrows(IllegalArgumentException.class, () -> sheet.get("RX", "C1"));
            assertThrows(IllegalArgumentException.class, () -> sheet.get("R1", "CX"));
        }

        @Test
        public void testGetByIndices() {
            assertEquals("V11", sheet.get(0, 0));
            assertEquals(100, sheet.get(1, 0));
            assertNull(sheet.get(0, 2));
        }

        @Test
        public void testGetByIndices_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertNull(uninitializedSheet.get(0, 0));
        }

        @Test
        public void testGetByIndices_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(5, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(0, 5));
        }

        @Test
        public void testGetByPoint() {
            assertEquals("V11", sheet.get(Sheet.Point.of(0, 0)));
            assertNull(sheet.get(Sheet.Point.of(0, 2)));
        }

        @Test
        public void testGetByPoint_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.get(Sheet.Point.of(5, 0)));
        }
    }

    @Nested
    @DisplayName("Put Methods")
    public class PutMethods {
        @Test
        public void testPutByKeys() {
            Object prev = sheet.set("R3", "C3", "V33");
            assertNull(prev);
            assertEquals("V33", sheet.get("R3", "C3"));

            Object prevUpdate = sheet.set("R1", "C1", "NewV11");
            assertEquals("V11", prevUpdate);
            assertEquals("NewV11", sheet.get("R1", "C1"));
        }

        @Test
        public void testPutByKeys_invalidKeys() {
            assertThrows(IllegalArgumentException.class, () -> sheet.set("RX", "C1", "Val"));
            assertThrows(IllegalArgumentException.class, () -> sheet.set("R1", "CX", "Val"));
        }

        @Test
        public void testPutByKeys_frozenSheet() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.set("R1", "C1", "Val"));
        }

        @Test
        public void testPutByIndices() {
            Object prev = sheet.set(2, 2, "V33");
            assertNull(prev);
            assertEquals("V33", sheet.get(2, 2));

            Object prevUpdate = sheet.set(0, 0, "NewV11");
            assertEquals("V11", prevUpdate);
            assertEquals("NewV11", sheet.get(0, 0));
        }

        @Test
        public void testPutByIndices_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.set(5, 0, "Val"));
        }

        @Test
        public void testPutByPoint() {
            Object prev = sheet.set(Sheet.Point.of(2, 2), "V33");
            assertNull(prev);
            assertEquals("V33", sheet.get(2, 2));
        }

        @Test
        public void testPutAll() {
            Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
            sourceSheet.set("R1", "C1", "SourceV11");
            sourceSheet.set("R2", "C2", "SourceV22");

            sheet.putAll(sourceSheet);
            assertEquals("SourceV11", sheet.get("R1", "C1"));
            assertEquals(null, sheet.get("R1", "C2"));
            assertEquals("SourceV22", sheet.get("R2", "C2"));
        }

        @Test
        public void testPutAll_2() {
            Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
            sourceSheet.set("R1", "C1", "SourceV11");
            sourceSheet.set("R2", "C2", "SourceV22");

            sheet.putAll(sourceSheet, (a, b) -> Iterables.firstNonNull(b, a));
            assertEquals("SourceV11", sheet.get("R1", "C1"));
            assertEquals("V12", sheet.get("R1", "C2"));
            assertEquals("SourceV22", sheet.get("R2", "C2"));
        }

        @Test
        public void testPutAll_keyMismatch() {
            Sheet<String, String, Object> sourceSheetBadRow = new Sheet<>(Arrays.asList("R1", "RX"), Arrays.asList("C1", "C2"));
            sourceSheetBadRow.set("R1", "C1", "V");
            sourceSheetBadRow.set("RX", "C1", "V");
            assertThrows(IllegalArgumentException.class, () -> sheet.putAll(sourceSheetBadRow));

            Sheet<String, String, Object> sourceSheetBadCol = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "CX"));
            sourceSheetBadCol.set("R1", "C1", "V");
            sourceSheetBadCol.set("R1", "CX", "V");
            assertThrows(IllegalArgumentException.class, () -> sheet.putAll(sourceSheetBadCol));
        }

        @Test
        public void testPutAll_frozenSheet() {
            sheet.freeze();
            Sheet<String, String, Object> sourceSheet = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1"));
            sourceSheet.set("R1", "C1", "V");
            assertThrows(IllegalStateException.class, () -> sheet.putAll(sourceSheet));
        }
    }

    @Nested
    @DisplayName("Remove Methods")
    public class RemoveMethods {
        @Test
        public void testRemoveByKeys() {
            Object removed = sheet.remove("R1", "C1");
            assertEquals("V11", removed);
            assertNull(sheet.get("R1", "C1"));
        }

        @Test
        public void testRemoveByKeys_nonExistentValueWasNull() {
            Object removed = sheet.remove("R1", "C3");
            assertNull(removed);
            assertNull(sheet.get("R1", "C3"));
        }

        @Test
        public void testRemoveByKeys_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertNull(uninitializedSheet.remove("R1", "C1"));
        }

        @Test
        public void testRemoveByKeys_invalidKeys() {
            assertThrows(IllegalArgumentException.class, () -> sheet.remove("RX", "C1"));
        }

        @Test
        public void testRemoveByIndices() {
            Object removed = sheet.remove(0, 1);
            assertEquals("V12", removed);
            assertNull(sheet.get(0, 1));
        }

        @Test
        public void testRemoveByIndices_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertNull(uninitializedSheet.remove(0, 0));
        }

        @Test
        public void testRemoveByPoint() {
            Object removed = sheet.remove(Sheet.Point.of(1, 0));
            assertEquals(100, removed);
            assertNull(sheet.get(1, 0));
        }

        @Test
        public void testRemove_frozenSheet() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.remove("R1", "C1"));
            assertThrows(IllegalStateException.class, () -> sheet.remove(0, 0));
        }
    }

    @Nested
    @DisplayName("Contains Methods")
    public class ContainsMethods {
        @Test
        public void testContains_keyPair() {
            assertTrue(sheet.containsCell("R1", "C1"));
            assertTrue(sheet.containsCell("R3", "C3"));
            assertFalse(sheet.containsCell("RX", "C1"));
            assertFalse(sheet.containsCell("R1", "CX"));
        }

        @Test
        public void testContains_keyPairAndValue() {
            assertTrue(sheet.containsValueAt("R1", "C1", "V11"));
            assertTrue(sheet.containsValueAt("R1", "C3", null));
            assertFalse(sheet.containsValueAt("R1", "C1", "WrongValue"));
            assertFalse(sheet.containsValueAt("R1", "C3", "NotNull"));
        }

        @Test
        public void testContainsValue() {
            assertTrue(sheet.containsValue("V11"));
            assertTrue(sheet.containsValue(100));
            assertTrue(sheet.containsValue(true));
            assertTrue(sheet.containsValue(null));
            assertFalse(sheet.containsValue("NonExistentValue"));

            Sheet<String, String, String> emptyValSheet = new Sheet<>(Arrays.asList("R"), Arrays.asList("C"));
            assertTrue(emptyValSheet.containsValue(null));
            emptyValSheet.set("R", "C", "V");
            assertFalse(emptyValSheet.containsValue(null));
        }

        @Test
        public void testContainsRow() {
            assertTrue(sheet.containsRow("R1"));
            assertFalse(sheet.containsRow("RX"));
        }

        @Test
        public void testContainsColumn() {
            assertTrue(sheet.containsColumn("C1"));
            assertFalse(sheet.containsColumn("CX"));
        }
    }

    @Nested
    @DisplayName("Row Manipulation Methods")
    public class RowManipulationMethods {
        @Test
        public void testGetRow() {
            ImmutableList<Object> row1_immutable = sheet.rowValues("R1");
            List<Object> row1 = new ArrayList<>(row1_immutable);
            assertEquals(Arrays.asList("V11", "V12", null), row1);
            assertThrows(UnsupportedOperationException.class, () -> row1_immutable.add("test"));
        }

        @Test
        public void testGetRow_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.rowValues("R1")));
        }

        @Test
        public void testGetRow_invalidKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.rowValues("RX"));
        }

        @Test
        public void testSetRow() {
            List<Object> newRowData = Arrays.asList("New1", "New2", "New3");
            sheet.setRow("R1", newRowData);
            assertEquals(newRowData, new ArrayList<>(sheet.rowValues("R1")));
        }

        @Test
        public void testSetRow_sizeMismatch() {
            List<Object> newRowDataShort = Arrays.asList("New1", "New2");
            assertThrows(IllegalArgumentException.class, () -> sheet.setRow("R1", newRowDataShort));
        }

        @Test
        public void testSetRow_emptyCollectionToSetNulls() {
            sheet.setRow("R1", Collections.emptyList());
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(sheet.rowValues("R1")));
        }

        @Test
        public void testSetRow_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.setRow("R1", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddRow() {
            List<Object> newRowData = Arrays.asList("V41", "V42", "V43");
            sheet.addRow("R4", newRowData);
            assertTrue(sheet.containsRow("R4"));
            assertEquals(4, sheet.rowCount());
            assertEquals(newRowData, new ArrayList<>(sheet.rowValues("R4")));
        }

        @Test
        public void testAddRow_emptyData() {
            sheet.addRow("R4", Collections.emptyList());
            assertTrue(sheet.containsRow("R4"));
            assertEquals(4, sheet.rowCount());
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(sheet.rowValues("R4")));
        }

        @Test
        public void testAddRow_duplicateKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.addRow("R1", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddRow_sizeMismatch() {
            assertThrows(IllegalArgumentException.class, () -> sheet.addRow("R4", Arrays.asList("a", "b")));
        }

        @Test
        public void testAddRow_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.addRow("R4", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddRow_atIndex() {
            List<Object> newRowData = Arrays.asList("VNew1", "VNew2", "VNew3");
            sheet.addRow(1, "RNew", newRowData);

            assertEquals(4, sheet.rowCount());
            assertTrue(sheet.containsRow("RNew"));
            assertEquals(Arrays.asList("R1", "RNew", "R2", "R3"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(newRowData, new ArrayList<>(sheet.rowValues("RNew")));
            assertEquals("V11", sheet.get("R1", "C1"));
            assertEquals(100, sheet.get("R2", "C1"));
        }

        @Test
        public void testAddRow_atIndex_end() {
            List<Object> newRowData = Arrays.asList("V41", "V42", "V43");
            sheet.addRow(3, "R4", newRowData);
            assertEquals(Arrays.asList("R1", "R2", "R3", "R4"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(newRowData, new ArrayList<>(sheet.rowValues("R4")));
        }

        @Test
        public void testAddRow_atIndex_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.addRow(4, "R5", Arrays.asList("a", "b", "c")));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.addRow(-1, "R0", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testUpdateRow() {
            intSheet.updateRow("R1", val -> val == null ? 0 : val + 100);
            assertEquals(Arrays.asList(111, 112, 113), new ArrayList<>(intSheet.rowValues("R1")));
            assertEquals(Arrays.asList(21, 22, 23), new ArrayList<>(intSheet.rowValues("R2")));

            sheet.set("R3", "C1", 50);
            sheet.updateRow("R3", val -> val == null ? "NULL_UPDATED" : ((Integer) val) * 2);
            assertEquals(Arrays.asList(100, "NULL_UPDATED", "NULL_UPDATED"), new ArrayList<>(sheet.rowValues("R3")));
        }

        @Test
        public void testUpdateRow_frozen() {
            intSheet.freeze();
            assertThrows(IllegalStateException.class, () -> intSheet.updateRow("R1", v -> v + 1));
        }

        @Test
        public void testRemoveRow() {
            List<Object> r1Data = new ArrayList<>(sheet.rowValues("R1"));
            List<Object> r3Data = new ArrayList<>(sheet.rowValues("R3"));

            sheet.removeRow("R2");
            assertFalse(sheet.containsRow("R2"));
            assertEquals(2, sheet.rowCount());
            assertEquals(Arrays.asList("R1", "R3"), new ArrayList<>(sheet.rowKeySet()));
            assertThrows(IllegalArgumentException.class, () -> sheet.rowValues("R2"));
            assertEquals(r1Data, new ArrayList<>(sheet.rowValues("R1")));
            assertEquals(r3Data, new ArrayList<>(sheet.rowValues("R3")));
        }

        @Test
        public void testRemoveRow_invalidKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.removeRow("RX"));
        }

        @Test
        public void testRemoveRow_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.removeRow("R1"));
        }

        @Test
        public void testMoveRow() {
            List<Object> r1Data = new ArrayList<>(sheet.rowValues("R1"));
            List<Object> r2Data = new ArrayList<>(sheet.rowValues("R2"));
            List<Object> r3Data = new ArrayList<>(sheet.rowValues("R3"));

            sheet.moveRow("R1", 2);
            assertEquals(Arrays.asList("R2", "R3", "R1"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(r2Data, new ArrayList<>(sheet.rowValues("R2")));
            assertEquals(r3Data, new ArrayList<>(sheet.rowValues("R3")));
            assertEquals(r1Data, new ArrayList<>(sheet.rowValues("R1")));
        }

        @Test
        public void testMoveRow_toBeginning() {
            List<Object> r1Data = new ArrayList<>(sheet.rowValues("R1"));
            List<Object> r2Data = new ArrayList<>(sheet.rowValues("R2"));
            List<Object> r3Data = new ArrayList<>(sheet.rowValues("R3"));

            sheet.moveRow("R3", 0);
            assertEquals(Arrays.asList("R3", "R1", "R2"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(r3Data, new ArrayList<>(sheet.rowValues("R3")));
            assertEquals(r1Data, new ArrayList<>(sheet.rowValues("R1")));
            assertEquals(r2Data, new ArrayList<>(sheet.rowValues("R2")));
        }

        @Test
        public void testMoveRow_toSamePosition() {
            List<String> initialRowOrder = new ArrayList<>(sheet.rowKeySet());
            List<Object> r2Data = new ArrayList<>(sheet.rowValues("R2"));
            sheet.moveRow("R2", 1);
            assertEquals(initialRowOrder, new ArrayList<>(sheet.rowKeySet()));
            assertEquals(r2Data, new ArrayList<>(sheet.rowValues("R2")));
        }

        @Test
        public void testMoveRow_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.moveRow("R1", 1));
        }

        @Test
        public void testSwapRowPosition() {
            List<Object> r1Data = new ArrayList<>(sheet.rowValues("R1"));
            List<Object> r2Data = new ArrayList<>(sheet.rowValues("R2"));
            List<Object> r3Data = new ArrayList<>(sheet.rowValues("R3"));

            sheet.swapRows("R1", "R2");

            assertEquals(Arrays.asList("R2", "R1", "R3"), new ArrayList<>(sheet.rowKeySet()));
            assertEquals(r2Data, new ArrayList<>(sheet.rowValues("R2")));
            assertEquals(r1Data, new ArrayList<>(sheet.rowValues("R1")));
            assertEquals(r3Data, new ArrayList<>(sheet.rowValues("R3")));
        }

        @Test
        public void testSwapRowPosition_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.swapRows("R1", "R2"));
        }

        @Test
        public void testRenameRow() {
            List<Object> r1Data = new ArrayList<>(sheet.rowValues("R1"));
            sheet.renameRow("R1", "R1_New");

            assertFalse(sheet.containsRow("R1"));
            assertTrue(sheet.containsRow("R1_New"));
            assertEquals(r1Data, new ArrayList<>(sheet.rowValues("R1_New")));
            assertEquals(Arrays.asList("R1_New", "R2", "R3"), new ArrayList<>(sheet.rowKeySet()));
        }

        @Test
        public void testRenameRow_newNameExists() {
            assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("R1", "R2"));
        }

        @Test
        public void testRenameRow_oldNameNotFound() {
            assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("RX_NonExistent", "R_New"));
        }

        @Test
        public void testRenameRow_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.renameRow("R1", "RNew"));
        }

        @Test
        public void testRow_asMap() {
            Map<String, Object> row1Map = sheet.rowAsMap("R1");
            assertEquals("V11", row1Map.get("C1"));
            assertEquals("V12", row1Map.get("C2"));
            assertNull(row1Map.get("C3"));
            assertEquals(3, row1Map.size());
            assertTrue(row1Map instanceof LinkedHashMap);
        }

        @Test
        public void testRow_asMap_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            Map<String, String> row1Map = uninitializedSheet.rowAsMap("R1");
            assertNull(row1Map.get("C1"));
            assertEquals(3, row1Map.size());
        }

        @Test
        public void testRowMap() {
            Map<String, Map<String, Object>> map = sheet.rowsMap();
            assertEquals(3, map.size());
            assertTrue(map.containsKey("R1"));
            assertEquals("V11", map.get("R1").get("C1"));
            assertEquals(true, map.get("R2").get("C3"));
            assertTrue(map instanceof LinkedHashMap);
        }
    }

    @Nested
    @DisplayName("Column Manipulation Methods")
    public class ColumnManipulationMethods {
        @Test
        public void testGetColumn() {
            ImmutableList<Object> col1_immutable = sheet.columnValues("C1");
            List<Object> col1 = new ArrayList<>(col1_immutable);
            assertEquals(Arrays.asList("V11", 100, null), col1);
            assertThrows(UnsupportedOperationException.class, () -> col1_immutable.add("test"));
        }

        @Test
        public void testGetColumn_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(uninitializedSheet.columnValues("C1")));
        }

        @Test
        public void testGetColumn_invalidKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.columnValues("CX"));
        }

        @Test
        public void testSetColumn() {
            List<Object> newColData = Arrays.asList("NewR1C1", "NewR2C1", "NewR3C1");
            sheet.setColumn("C1", newColData);
            assertEquals(newColData, new ArrayList<>(sheet.columnValues("C1")));
        }

        @Test
        public void testSetColumn_sizeMismatch() {
            List<Object> newColDataShort = Arrays.asList("New1", "New2");
            assertThrows(IllegalArgumentException.class, () -> sheet.setColumn("C1", newColDataShort));
        }

        @Test
        public void testSetColumn_emptyCollectionToSetNulls() {
            sheet.setColumn("C1", Collections.emptyList());
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(sheet.columnValues("C1")));
        }

        @Test
        public void testSetColumn_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.setColumn("C1", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddColumn() {
            List<Object> newColData = Arrays.asList("V14", "V24", "V34");
            sheet.addColumn("C4", newColData);
            assertTrue(sheet.containsColumn("C4"));
            assertEquals(4, sheet.columnCount());
            assertEquals(newColData, new ArrayList<>(sheet.columnValues("C4")));
        }

        @Test
        public void testAddColumn_emptyData() {
            sheet.addColumn("C4", Collections.emptyList());
            assertTrue(sheet.containsColumn("C4"));
            assertEquals(4, sheet.columnCount());
            assertEquals(Arrays.asList(null, null, null), new ArrayList<>(sheet.columnValues("C4")));
        }

        @Test
        public void testAddColumn_duplicateKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.addColumn("C1", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddColumn_sizeMismatch() {
            assertThrows(IllegalArgumentException.class, () -> sheet.addColumn("C4", Arrays.asList("a", "b")));
        }

        @Test
        public void testAddColumn_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.addColumn("C4", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testAddColumn_atIndex() {
            List<Object> newColData = Arrays.asList("NR1New", "NR2New", "NR3New");
            sheet.addColumn(1, "CNew", newColData);

            assertEquals(4, sheet.columnCount());
            assertTrue(sheet.containsColumn("CNew"));
            assertEquals(Arrays.asList("C1", "CNew", "C2", "C3"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(newColData, new ArrayList<>(sheet.columnValues("CNew")));
            assertEquals("V11", sheet.get("R1", "C1"));
            assertEquals("V12", sheet.get("R1", "C2"));
        }

        @Test
        public void testAddColumn_atIndex_end() {
            List<Object> newColData = Arrays.asList("V14", "V24", "V34");
            sheet.addColumn(3, "C4", newColData);
            assertEquals(Arrays.asList("C1", "C2", "C3", "C4"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(newColData, new ArrayList<>(sheet.columnValues("C4")));
        }

        @Test
        public void testAddColumn_atIndex_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.addColumn(4, "C5", Arrays.asList("a", "b", "c")));
            assertThrows(IndexOutOfBoundsException.class, () -> sheet.addColumn(-1, "C0", Arrays.asList("a", "b", "c")));
        }

        @Test
        public void testUpdateColumn() {
            intSheet.updateColumn("C1", val -> val == null ? 0 : val * 2);
            assertEquals(Arrays.asList(22, 42, 62), new ArrayList<>(intSheet.columnValues("C1")));
            assertEquals(Arrays.asList(12, 22, 32), new ArrayList<>(intSheet.columnValues("C2")));

            sheet.set("R1", "C3", 50);
            sheet.updateColumn("C3", val -> val == null ? "NULL_UPDATED" : (val instanceof Boolean ? "BOOL_UPDATED" : ((Integer) val) * 3));
            assertEquals(Arrays.asList(150, "BOOL_UPDATED", "NULL_UPDATED"), new ArrayList<>(sheet.columnValues("C3")));
        }

        @Test
        public void testUpdateColumn_frozen() {
            intSheet.freeze();
            assertThrows(IllegalStateException.class, () -> intSheet.updateColumn("C1", v -> v + 1));
        }

        @Test
        public void testRemoveColumn() {
            List<Object> c1Data = new ArrayList<>(sheet.columnValues("C1"));
            List<Object> c3Data = new ArrayList<>(sheet.columnValues("C3"));

            sheet.removeColumn("C2");
            assertFalse(sheet.containsColumn("C2"));
            assertEquals(2, sheet.columnCount());
            assertEquals(Arrays.asList("C1", "C3"), new ArrayList<>(sheet.columnKeySet()));
            assertThrows(IllegalArgumentException.class, () -> sheet.columnValues("C2"));
            assertEquals(c1Data, new ArrayList<>(sheet.columnValues("C1")));
            assertEquals(c3Data, new ArrayList<>(sheet.columnValues("C3")));
        }

        @Test
        public void testRemoveColumn_invalidKey() {
            assertThrows(IllegalArgumentException.class, () -> sheet.removeColumn("CX"));
        }

        @Test
        public void testRemoveColumn_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.removeColumn("C1"));
        }

        @Test
        public void testMoveColumn() {
            List<Object> c1Data = new ArrayList<>(sheet.columnValues("C1"));
            List<Object> c2Data = new ArrayList<>(sheet.columnValues("C2"));
            List<Object> c3Data = new ArrayList<>(sheet.columnValues("C3"));

            sheet.moveColumn("C1", 2);
            assertEquals(Arrays.asList("C2", "C3", "C1"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(c2Data, new ArrayList<>(sheet.columnValues("C2")));
            assertEquals(c3Data, new ArrayList<>(sheet.columnValues("C3")));
            assertEquals(c1Data, new ArrayList<>(sheet.columnValues("C1")));
        }

        @Test
        public void testMoveColumn_toBeginning() {
            List<Object> c1Data = new ArrayList<>(sheet.columnValues("C1"));
            List<Object> c2Data = new ArrayList<>(sheet.columnValues("C2"));
            List<Object> c3Data = new ArrayList<>(sheet.columnValues("C3"));

            sheet.moveColumn("C3", 0);
            assertEquals(Arrays.asList("C3", "C1", "C2"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(c3Data, new ArrayList<>(sheet.columnValues("C3")));
            assertEquals(c1Data, new ArrayList<>(sheet.columnValues("C1")));
            assertEquals(c2Data, new ArrayList<>(sheet.columnValues("C2")));
        }

        @Test
        public void testMoveColumn_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.moveColumn("C1", 1));
        }

        @Test
        public void testSwapColumnPosition() {
            List<Object> c1Data = new ArrayList<>(sheet.columnValues("C1"));
            List<Object> c2Data = new ArrayList<>(sheet.columnValues("C2"));
            List<Object> c3Data = new ArrayList<>(sheet.columnValues("C3"));

            sheet.swapColumns("C1", "C2");

            assertEquals(Arrays.asList("C2", "C1", "C3"), new ArrayList<>(sheet.columnKeySet()));
            assertEquals(c2Data, new ArrayList<>(sheet.columnValues("C2")));
            assertEquals(c1Data, new ArrayList<>(sheet.columnValues("C1")));
            assertEquals(c3Data, new ArrayList<>(sheet.columnValues("C3")));
        }

        @Test
        public void testSwapColumnPosition_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.swapColumns("C1", "C2"));
        }

        @Test
        public void testRenameColumn() {
            List<Object> c1Data = new ArrayList<>(sheet.columnValues("C1"));
            sheet.renameColumn("C1", "C1_New");

            assertFalse(sheet.containsColumn("C1"));
            assertTrue(sheet.containsColumn("C1_New"));
            assertEquals(c1Data, new ArrayList<>(sheet.columnValues("C1_New")));
            assertEquals(Arrays.asList("C1_New", "C2", "C3"), new ArrayList<>(sheet.columnKeySet()));
        }

        @Test
        public void testRenameColumn_newNameExists() {
            assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("C1", "C2"));
        }

        @Test
        public void testRenameColumn_oldNameNotFound() {
            assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("CX_NonExistent", "C_New"));
        }

        @Test
        public void testRenameColumn_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.renameColumn("C1", "CNew"));
        }

        @Test
        public void testColumn_asMap() {
            Map<String, Object> col1Map = sheet.columnAsMap("C1");
            assertEquals("V11", col1Map.get("R1"));
            assertEquals(100, col1Map.get("R2"));
            assertNull(col1Map.get("R3"));
            assertEquals(3, col1Map.size());
            assertTrue(col1Map instanceof LinkedHashMap);
        }

        @Test
        public void testColumn_asMap_uninitializedSheet() {
            Sheet<String, String, String> uninitializedSheet = new Sheet<>(rowKeys, colKeys);
            Map<String, String> col1Map = uninitializedSheet.columnAsMap("C1");
            assertNull(col1Map.get("R1"));
            assertEquals(3, col1Map.size());
        }

        @Test
        public void testColumnMap() {
            Map<String, Map<String, Object>> map = sheet.columnsMap();
            assertEquals(3, map.size());
            assertTrue(map.containsKey("C1"));
            assertEquals("V11", map.get("C1").get("R1"));
            assertEquals(true, map.get("C3").get("R2"));
            assertTrue(map instanceof LinkedHashMap);
        }
    }

    @Nested
    @DisplayName("Dimension Methods")
    public class DimensionMethods {
        @Test
        public void testRowLength() {
            assertEquals(3, sheet.rowCount());
            Sheet<String, String, String> emptySheet = new Sheet<>();
            assertEquals(0, emptySheet.rowCount());
        }

        @Test
        public void testColumnLength() {
            assertEquals(3, sheet.columnCount());
            Sheet<String, String, String> emptySheet = new Sheet<>();
            assertEquals(0, emptySheet.columnCount());
        }
    }

    @Nested
    @DisplayName("Update/Replace Methods")
    public class UpdateReplaceMethods {
        @Test
        public void testUpdateAll_byValue() {
            intSheet.updateAll(val -> val == null ? -1 : val + 10);
            assertEquals(21, intSheet.get("R1", "C1"));
            assertEquals(43, intSheet.get("R3", "C3"));

            Sheet<String, String, Integer> sheetWithNulls = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1"));
            sheetWithNulls.updateAll(val -> val == null ? -1 : val + 10);
            assertEquals(-1, sheetWithNulls.get("R1", "C1"));
        }

        @Test
        public void testUpdateAll_byIndices() {
            intSheet.updateAll((rIdx, cIdx) -> (rIdx + 1) * 100 + (cIdx + 1) * 10);
            assertEquals(110, intSheet.get(0, 0));
            assertEquals(330, intSheet.get(2, 2));
        }

        @Test
        public void testUpdateAll_byKeysAndValue() {
            intSheet.updateAll((rKey, cKey, val) -> {
                int rNum = Integer.parseInt(rKey.substring(1));
                int cNum = Integer.parseInt(cKey.substring(1));
                return (val == null ? 0 : val) + rNum * 10 + cNum;
            });
            assertEquals(22, intSheet.get("R1", "C1"));
            assertEquals(66, intSheet.get("R3", "C3"));
        }

        @Test
        public void testReplaceIf_byValuePredicate() {
            intSheet.replaceIf(val -> val != null && val > 20 && val < 30, 999);
            assertEquals(11, intSheet.get("R1", "C1"));
            assertEquals(999, intSheet.get("R2", "C1"));
            assertEquals(999, intSheet.get("R2", "C2"));
            assertEquals(999, intSheet.get("R2", "C3"));
            assertEquals(31, intSheet.get("R3", "C1"));
        }

        @Test
        public void testReplaceIf_byValuePredicate_withNulls() {
            sheet.replaceIf(Objects::isNull, "REPLACED_NULL");
            assertEquals("V11", sheet.get("R1", "C1"));
            assertEquals("REPLACED_NULL", sheet.get("R1", "C3"));
            assertEquals("REPLACED_NULL", sheet.get("R2", "C2"));
        }

        @Test
        public void testReplaceIf_byIndexPredicate() {
            intSheet.replaceIf((rIdx, cIdx) -> rIdx == 1, 777);
            assertEquals(11, intSheet.get("R1", "C1"));
            assertEquals(777, intSheet.get("R2", "C1"));
            assertEquals(777, intSheet.get("R2", "C2"));
            assertEquals(777, intSheet.get("R2", "C3"));
            assertEquals(31, intSheet.get("R3", "C1"));
        }

        @Test
        public void testReplaceIf_byKeyAndValuePredicate() {
            intSheet.replaceIf((rKey, cKey, val) -> "R2".equals(rKey) && val != null && val > 21, 888);
            assertEquals(21, intSheet.get("R2", "C1"));
            assertEquals(888, intSheet.get("R2", "C2"));
            assertEquals(888, intSheet.get("R2", "C3"));
        }

        @Test
        public void testUpdateAll_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.updateAll(v -> v));
            assertThrows(IllegalStateException.class, () -> sheet.updateAll((r, c) -> "v"));
            assertThrows(IllegalStateException.class, () -> sheet.updateAll((r, c, v) -> v));
        }

        @Test
        public void testReplaceIf_frozen() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.replaceIf(v -> true, "new"));
            assertThrows(IllegalStateException.class, () -> sheet.replaceIf((r, c) -> true, "new"));
            assertThrows(IllegalStateException.class, () -> sheet.replaceIf((r, c, v) -> true, "new"));
        }
    }

    @Nested
    @DisplayName("Sort Methods")
    public class SortMethods {
        Sheet<String, String, Integer> sortSheet;

        @BeforeEach
        public void setUpSortSheet() {
            sortSheet = new Sheet<>(Arrays.asList("B", "C", "A"), Arrays.asList("Y", "Z", "X"));
            sortSheet.set("B", "Y", 1);
            sortSheet.set("B", "Z", 2);
            sortSheet.set("B", "X", 3);
            sortSheet.set("C", "Y", 4);
            sortSheet.set("C", "Z", 5);
            sortSheet.set("C", "X", 6);
            sortSheet.set("A", "Y", 7);
            sortSheet.set("A", "Z", 8);
            sortSheet.set("A", "X", 9);
        }

        @Test
        public void testSortByRowKey_natural() {
            sortSheet.sortByRowKey();
            assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(sortSheet.rowKeySet()));
            assertEquals(7, sortSheet.get("A", "Y"));
            assertEquals(8, sortSheet.get("A", "Z"));
            assertEquals(9, sortSheet.get("A", "X"));
            assertEquals(1, sortSheet.get("B", "Y"));
        }

        @Test
        public void testSortByRowKey_customComparator() {
            sortSheet.sortByRowKey(Comparator.reverseOrder());
            assertEquals(Arrays.asList("C", "B", "A"), new ArrayList<>(sortSheet.rowKeySet()));
            assertEquals(4, sortSheet.get("C", "Y"));
            assertEquals(1, sortSheet.get("B", "Y"));
            assertEquals(7, sortSheet.get("A", "Y"));
        }

        @Test
        public void testSortByRow_valuesInARow() {
            sortSheet.sortColumnsByRowValues("A", Comparator.naturalOrder());
            assertEquals(Arrays.asList("Y", "Z", "X"), new ArrayList<>(sortSheet.columnKeySet()));
            assertEquals(1, sortSheet.get("B", "Y"));
            assertEquals(2, sortSheet.get("B", "Z"));
            assertEquals(3, sortSheet.get("B", "X"));

            sortSheet.sortColumnsByRowValues("A", Comparator.reverseOrder());
            assertEquals(Arrays.asList("X", "Z", "Y"), new ArrayList<>(sortSheet.columnKeySet()));
            assertEquals(3, sortSheet.get("B", "X"));
            assertEquals(2, sortSheet.get("B", "Z"));
            assertEquals(1, sortSheet.get("B", "Y"));
        }

        @Test
        public void testSortByRows_multipleRowsCriteria() {
            sortSheet.sortColumnsByRowValues(Arrays.asList("B", "A"), (arr1, arr2) -> {
                int cmp = ((Integer) arr1[0]).compareTo((Integer) arr2[0]);
                if (cmp == 0) {
                    return ((Integer) arr1[1]).compareTo((Integer) arr2[1]);
                }
                return cmp;
            });
            assertEquals(Arrays.asList("Y", "Z", "X"), new ArrayList<>(sortSheet.columnKeySet()));
        }

        @Test
        public void testSortByColumnKey_natural() {
            sortSheet.sortByColumnKey();
            assertEquals(Arrays.asList("X", "Y", "Z"), new ArrayList<>(sortSheet.columnKeySet()));
            assertEquals(3, sortSheet.get("B", "X"));
            assertEquals(1, sortSheet.get("B", "Y"));
            assertEquals(2, sortSheet.get("B", "Z"));
        }

        @Test
        public void testSortByColumnKey_customComparator() {
            sortSheet.sortByColumnKey(Comparator.reverseOrder());
            assertEquals(Arrays.asList("Z", "Y", "X"), new ArrayList<>(sortSheet.columnKeySet()));
            assertEquals(2, sortSheet.get("B", "Z"));
        }

        @Test
        public void testSortByColumn_valuesInAColumn() {
            sortSheet.sortRowsByColumnValues("X", Comparator.naturalOrder());
            assertEquals(Arrays.asList("B", "C", "A"), new ArrayList<>(sortSheet.rowKeySet()));
            assertEquals(1, sortSheet.get("B", "Y"));
            assertEquals(4, sortSheet.get("C", "Y"));
            assertEquals(7, sortSheet.get("A", "Y"));

            sortSheet.sortRowsByColumnValues("X", Comparator.reverseOrder());
            assertEquals(Arrays.asList("A", "C", "B"), new ArrayList<>(sortSheet.rowKeySet()));
            assertEquals(7, sortSheet.get("A", "Y"));
            assertEquals(4, sortSheet.get("C", "Y"));
            assertEquals(1, sortSheet.get("B", "Y"));
        }

        @Test
        public void testSortByColumns_multipleColsCriteria() {
            sortSheet.sortRowsByColumnValues(Arrays.asList("Y", "X"), (arr1, arr2) -> {
                int cmp = ((Integer) arr1[0]).compareTo((Integer) arr2[0]);
                if (cmp == 0) {
                    return ((Integer) arr1[1]).compareTo((Integer) arr2[1]);
                }
                return cmp;
            });
            assertEquals(Arrays.asList("B", "C", "A"), new ArrayList<>(sortSheet.rowKeySet()));
        }

        @Test
        public void testSort_frozenSheet() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.sortByRowKey());
            assertThrows(IllegalStateException.class, () -> sheet.sortByColumnKey());
            assertThrows(IllegalStateException.class, () -> sheet.sortColumnsByRowValues("R1", (Comparator) Comparator.naturalOrder()));
            assertThrows(IllegalStateException.class, () -> sheet.sortRowsByColumnValues("C1", (Comparator) Comparator.naturalOrder()));
            assertThrows(IllegalStateException.class, () -> sheet.sortColumnsByRowValues(Collections.singletonList("R1"), (Comparator) Comparator.naturalOrder()));
            assertThrows(IllegalStateException.class, () -> sheet.sortRowsByColumnValues(Collections.singletonList("C1"), (Comparator) Comparator.naturalOrder()));
        }
    }

    @Nested
    @DisplayName("Copy and Clone Methods")
    public class CopyCloneMethods {
        @Test
        public void testCopy() {
            Sheet<String, String, Object> copy = sheet.copy();
            assertNotSame(sheet, copy);
            assertEquals(sheet, copy);
            assertEquals("V11", copy.get("R1", "C1"));

            copy.set("R1", "C1", "CopiedV11");
            assertEquals("CopiedV11", copy.get("R1", "C1"));
            assertEquals("V11", sheet.get("R1", "C1"));
        }

        @Test
        public void testCopy_withSpecificKeys() {
            List<String> subRowKeys = Arrays.asList("R1", "R2");
            List<String> subColKeys = Arrays.asList("C1", "C2");
            Sheet<String, String, Object> subCopy = sheet.copy(subRowKeys, subColKeys);

            assertEquals(2, subCopy.rowCount());
            assertEquals(2, subCopy.columnCount());
            assertTrue(new LinkedHashSet<>(subCopy.rowKeySet()).containsAll(subRowKeys));
            assertTrue(new LinkedHashSet<>(subCopy.columnKeySet()).containsAll(subColKeys));

            assertEquals("V11", subCopy.get("R1", "C1"));
            assertEquals("V12", subCopy.get("R1", "C2"));
            assertEquals(100, subCopy.get("R2", "C1"));
            assertNull(subCopy.get("R2", "C2"));

            assertThrows(IllegalArgumentException.class, () -> subCopy.get("R3", "C1"));
        }

        @Test
        public void testCopy_withSpecificKeys_invalidSubset() {
            assertThrows(IllegalArgumentException.class, () -> sheet.copy(Arrays.asList("R1", "RX"), colKeys));
            assertThrows(IllegalArgumentException.class, () -> sheet.copy(rowKeys, Arrays.asList("C1", "CX")));
        }

        @Test
        @Disabled("Kryo dependency: Test requires Kryo on classpath and setup. Will throw RuntimeException if Kryo not available.")
        public void testClone_default() {
            try {
                Sheet<String, String, Object> clone = sheet.clone();
                assertNotSame(sheet, clone);
                assertEquals(sheet, clone);
                assertEquals(sheet.isFrozen(), clone.isFrozen());

                clone.set("R1", "C1", "ClonedV11");
                assertEquals("ClonedV11", clone.get("R1", "C1"));
                assertEquals("V11", sheet.get("R1", "C1"));

            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Kryo is required")) {
                    System.err.println("Skipping clone test as Kryo is not available: " + e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        @Test
        @Disabled("Kryo dependency: Test requires Kryo on classpath and setup. Will throw RuntimeException if Kryo not available.")
        public void testClone_withFreezeOption() {
            try {
                Sheet<String, String, Object> frozenClone = sheet.clone(true);
                assertTrue(frozenClone.isFrozen());
                assertThrows(IllegalStateException.class, () -> frozenClone.set("R1", "C1", "fail"));

                Sheet<String, String, Object> unfrozenClone = sheet.clone(false);
                assertFalse(unfrozenClone.isFrozen());
                assertDoesNotThrow(() -> unfrozenClone.set("R1", "C1", "ok"));
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Kryo is required")) {
                    System.err.println("Skipping clone(freeze) test as Kryo is not available: " + e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    @Nested
    @DisplayName("Merge and Transpose Methods")
    public class MergeTransposeMethods {
        @Test
        public void testMerge() {
            Sheet<String, String, Integer> sheetA = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C2"));
            sheetA.set("R1", "C1", 10);
            sheetA.set("R1", "C2", 20);
            sheetA.set("R2", "C1", 30);

            Sheet<String, String, Integer> sheetB = new Sheet<>(Arrays.asList("R1", "R3"), Arrays.asList("C1", "C3"));
            sheetB.set("R1", "C1", 5);
            sheetB.set("R1", "C3", 50);
            sheetB.set("R3", "C1", 60);

            Sheet<String, String, String> merged = sheetA.merge(sheetB, (valA, valB) -> {
                String sA = valA == null ? "nA" : valA.toString();
                String sB = valB == null ? "nB" : valB.toString();
                return sA + "+" + sB;
            });

            assertEquals(new LinkedHashSet<>(Arrays.asList("R1", "R2", "R3")), new LinkedHashSet<>(merged.rowKeySet()));
            assertEquals(new LinkedHashSet<>(Arrays.asList("C1", "C2", "C3")), new LinkedHashSet<>(merged.columnKeySet()));

            assertEquals("10+5", merged.get("R1", "C1"));
            assertEquals("20+nB", merged.get("R1", "C2"));
            assertEquals("nA+50", merged.get("R1", "C3"));
            assertEquals("30+nB", merged.get("R2", "C1"));
            assertEquals("nA+nB", merged.get("R2", "C2"));
            assertEquals("nA+nB", merged.get("R2", "C3"));
            assertEquals("nA+60", merged.get("R3", "C1"));
            assertEquals("nA+nB", merged.get("R3", "C2"));
            assertEquals("nA+nB", merged.get("R3", "C3"));
        }

        @Test
        public void testTranspose() {
            Sheet<String, String, Object> transposed = sheet.transpose();

            assertEquals(new LinkedHashSet<>(sheet.columnKeySet()), new LinkedHashSet<>(transposed.rowKeySet()));
            assertEquals(new LinkedHashSet<>(sheet.rowKeySet()), new LinkedHashSet<>(transposed.columnKeySet()));

            assertEquals("V11", transposed.get("C1", "R1"));
            assertEquals("V12", transposed.get("C2", "R1"));
            assertEquals(100, transposed.get("C1", "R2"));
            assertNull(transposed.get("C3", "R1"));
            assertEquals(true, transposed.get("C3", "R2"));
            assertNull(transposed.get("C2", "R2"));
        }

        @Test
        public void testTranspose_emptySheet() {
            Sheet<String, String, String> empty = new Sheet<>();
            Sheet<String, String, String> transposedEmpty = empty.transpose();
            assertTrue(transposedEmpty.isEmpty());
            assertEquals(0, transposedEmpty.rowCount());
            assertEquals(0, transposedEmpty.columnCount());
        }

        @Test
        public void testTranspose_uninitializedSheet() {
            Sheet<String, String, String> uninitialized = new Sheet<>(rowKeys, colKeys);
            Sheet<String, String, String> transposed = uninitialized.transpose();
            assertEquals(colKeys, new ArrayList<>(transposed.rowKeySet()));
            assertEquals(rowKeys, new ArrayList<>(transposed.columnKeySet()));
            assertNull(transposed.get("C1", "R1"));
        }

    }

    @Nested
    @DisplayName("State Methods")
    public class StateMethods {
        @Test
        public void testFreezeAndIsFrozen() {
            assertFalse(sheet.isFrozen());
            sheet.freeze();
            assertTrue(sheet.isFrozen());
            assertThrows(IllegalStateException.class, () -> sheet.set("R1", "C1", "Fail"));
        }

        @Test
        public void testClear() {
            sheet.clear();
            assertNull(sheet.get("R1", "C1"));
            assertNull(sheet.get("R2", "C1"));
            assertEquals(3, sheet.rowCount());
            assertEquals(3, sheet.columnCount());
            sheet.forEachH((r, c, v) -> assertNull(v));
        }

        @Test
        public void testClear_frozenSheet() {
            sheet.freeze();
            assertThrows(IllegalStateException.class, () -> sheet.clear());
        }

        @Test
        public void testTrimToSize() {
            assertDoesNotThrow(() -> sheet.trimToSize());

            sheet.clear();
            assertDoesNotThrow(() -> sheet.trimToSize());

            Sheet<String, String, String> s = new Sheet<>(CommonUtil.asSet("R"), CommonUtil.asSet("C"));
            assertDoesNotThrow(() -> s.trimToSize());

            s.set("R", "C", "V");
            assertDoesNotThrow(() -> s.trimToSize());
        }

        @Test
        public void testCountOfNonNullValue() {
            assertEquals(4, sheet.countOfNonNullValues());
            sheet.set("R3", "C3", "V33");
            assertEquals(5, sheet.countOfNonNullValues());
            sheet.clear();
            assertEquals(0, sheet.countOfNonNullValues());
            Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
            assertEquals(0, uninit.countOfNonNullValues());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(sheet.isEmpty());

            Sheet<String, String, String> emptyRows = new Sheet<>(Collections.emptyList(), colKeys);
            assertTrue(emptyRows.isEmpty());

            Sheet<String, String, String> emptyCols = new Sheet<>(rowKeys, Collections.emptyList());
            assertTrue(emptyCols.isEmpty());

            Sheet<String, String, String> fullyEmpty = new Sheet<>();
            assertTrue(fullyEmpty.isEmpty());
        }
    }

    @Nested
    @DisplayName("Iteration and Stream Methods: Detailed and Edge Cases")
    public class IterationStreamMethodsDetailed {

        @Test
        public void testForEachH_exceptionPropagation() {
            IOException thrown = assertThrows(IOException.class, () -> {
                sheet.forEachH((r, c, v) -> {
                    if (r.equals("R2") && c.equals("C1")) {
                        throw new IOException("Test Exception");
                    }
                });
            });
            assertEquals("Test Exception", thrown.getMessage());
        }

        @Test
        public void testForEachV_exceptionPropagation() {
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                sheet.forEachV((r, c, v) -> {
                    if (c.equals("C2") && r.equals("R1")) {
                        throw new RuntimeException("Test Exception V");
                    }
                });
            });
            assertEquals("Test Exception V", thrown.getMessage());
        }

        @Test
        public void testForEachNonNullH_exceptionPropagation() {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                sheet.forEachNonNullH((r, c, v) -> {
                    if (r.equals("R2") && c.equals("C3")) {
                        throw new IllegalArgumentException("Test Exception NonNullH");
                    }
                });
            });
            assertEquals("Test Exception NonNullH", thrown.getMessage());
        }

        @Test
        public void testForEachNonNullV_exceptionPropagation() {
            UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> {
                sheet.forEachNonNullV((r, c, v) -> {
                    if (c.equals("C1") && r.equals("R1")) {
                        throw new UnsupportedOperationException("Test Exception NonNullV");
                    }
                });
            });
            assertEquals("Test Exception NonNullV", thrown.getMessage());
        }

        @Test
        public void testCellsH_emptyRange() {
            assertTrue(sheet.cellsH(1, 1).toList().isEmpty());
        }

        @Test
        public void testCellsV_emptyRange() {
            assertTrue(sheet.cellsV(1, 1).toList().isEmpty());
        }

        @Test
        public void testCellsR_emptyRange() {
            assertTrue(sheet.cellsR(1, 1).toList().isEmpty());
        }

        @Test
        public void testCellsC_emptyRange() {
            assertTrue(sheet.cellsC(1, 1).toList().isEmpty());
        }

        @Test
        public void testPointsH_fullRangeAndSubRange() {
            assertEquals(9, sheet.pointsH().count());
            assertEquals(3, sheet.pointsH(0, 1).count());
            assertEquals(0, sheet.pointsH(1, 1).count());
            List<Sheet.Point> r1Points = sheet.pointsH(0, 1).toList();
            assertEquals(Sheet.Point.of(0, 0), r1Points.get(0));
            assertEquals(Sheet.Point.of(0, 1), r1Points.get(1));
            assertEquals(Sheet.Point.of(0, 2), r1Points.get(2));
        }

        @Test
        public void testPointsV_fullRangeAndSubRange() {
            assertEquals(9, sheet.pointsV().count());
            assertEquals(3, sheet.pointsV(0, 1).count());
            assertEquals(0, sheet.pointsV(1, 1).count());
            List<Sheet.Point> c1Points = sheet.pointsV(0, 1).toList();
            assertEquals(Sheet.Point.of(0, 0), c1Points.get(0));
            assertEquals(Sheet.Point.of(1, 0), c1Points.get(1));
            assertEquals(Sheet.Point.of(2, 0), c1Points.get(2));
        }

        @Test
        public void testPointsR_fullRangeAndSubRange() {
            assertEquals(3, sheet.pointsR().count());
            assertEquals(1, sheet.pointsR(1, 2).count());
            assertEquals(0, sheet.pointsR(1, 1).count());

            List<Sheet.Point> r2Points = sheet.pointsR(1, 2).first().get().toList();
            assertEquals(3, r2Points.size());
            assertEquals(Sheet.Point.of(1, 0), r2Points.get(0));
        }

        @Test
        public void testPointsC_fullRangeAndSubRange_ACTUAL_BEHAVIOR() {
            sheet.println();
            List<Stream<Sheet.Point>> pointsCStreams = sheet.pointsC().toList();
            assertEquals(sheet.columnCount(), pointsCStreams.size());

            for (int i = 0; i < sheet.columnCount(); i++) {
                List<Sheet.Point> streamContent = pointsCStreams.get(i).toList();
                assertEquals(sheet.rowCount(), streamContent.size());
                for (int j = 0; j < sheet.rowCount(); j++) {
                    assertEquals(Sheet.Point.of(j, i), streamContent.get(j));
                }
            }

            List<Stream<Sheet.Point>> actualPointsCStreams = sheet.pointsC(0, sheet.columnCount()).toList();
            assertEquals(sheet.columnCount(), actualPointsCStreams.size());
            List<Sheet.Point> firstColPoints = actualPointsCStreams.get(0).toList();
            assertEquals(sheet.rowCount(), firstColPoints.size());
            assertEquals(Sheet.Point.of(0, 0), firstColPoints.get(0));
            assertEquals(Sheet.Point.of(1, 0), firstColPoints.get(1));
            assertEquals(Sheet.Point.of(2, 0), firstColPoints.get(2));
        }

        @Test
        public void testPointsC_overload_emptyRange() {
            assertTrue(sheet.pointsC(1, 1).toList().isEmpty());
        }

        @Test
        public void testStreamH_emptyRange() {
            assertTrue(sheet.streamH(1, 1).toList().isEmpty());
        }

        @Test
        public void testStreamV_emptyRange() {
            assertTrue(sheet.streamV(1, 1).toList().isEmpty());
        }

        @Test
        public void testStreamR_emptyRange() {
            assertTrue(sheet.streamR(1, 1).toList().isEmpty());
        }

        @Test
        public void testStreamC_emptyRange() {
            assertTrue(sheet.streamC(1, 1).toList().isEmpty());
        }

        @Test
        public void testRows_pairStream_emptyRange() {
            assertTrue(sheet.rows(1, 1).toList().isEmpty());
        }

        @Test
        public void testColumns_pairStream_emptyRange() {
            assertTrue(sheet.columns(1, 1).toList().isEmpty());
        }

        @Test
        public void testForEachH_Original() {
            Map<String, Object> collected = new LinkedHashMap<>();
            sheet.forEachH((r, c, v) -> collected.put(r + "-" + c, v));

            assertEquals("V11", collected.get("R1-C1"));
            assertEquals(true, collected.get("R2-C3"));
            assertNull(collected.get("R1-C3"));
            assertEquals(9, collected.size());
            List<String> expectedOrder = Arrays.asList("R1-C1", "R1-C2", "R1-C3", "R2-C1", "R2-C2", "R2-C3", "R3-C1", "R3-C2", "R3-C3");
            assertEquals(expectedOrder, new ArrayList<>(collected.keySet()));
        }
    }

    @Nested
    @DisplayName("Conversion Methods")
    public class ConversionMethods {
        @Test
        public void testToDatasetH() {
            Dataset ds = sheet.toDatasetH();
            assertEquals(CommonUtil.asList("C1", "C2", "C3"), ds.columnNames());
            assertEquals(3, ds.size());
            assertEquals("V11", ds.moveToRow(0).get("C1"));
            assertEquals(true, ds.moveToRow(1).get("C3"));
            assertNull(ds.moveToRow(2).get("C1"));
        }

        @Test
        public void testToDatasetH_uninitialized() {
            Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
            Dataset ds = uninit.toDatasetH();
            assertEquals(CommonUtil.asList("C1", "C2", "C3"), ds.columnNames());
            assertEquals(3, ds.size());
            assertNull(ds.moveToRow(0).get("C1"));
        }

        @Test
        public void testToDatasetV() {
            Dataset ds = sheet.toDatasetV();
            assertEquals(CommonUtil.asList("R1", "R2", "R3"), ds.columnNames());
            assertEquals(3, ds.size());
            assertEquals("V11", ds.moveToRow(0).get("R1"));
            assertEquals(true, ds.moveToRow(2).get("R2"));
            assertNull(ds.moveToRow(0).get("R3"));
        }

        @Test
        public void testToArrayH() {
            Object[][] arr = sheet.toArrayH();
            assertEquals(3, arr.length);
            assertEquals(3, arr[0].length);
            assertEquals("V11", arr[0][0]);
            assertEquals(true, arr[1][2]);
            assertNull(arr[2][2]);
        }

        @Test
        public void testToArrayH_uninitialized() {
            Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
            Object[][] arr = uninit.toArrayH();
            assertEquals(3, arr.length);
            assertEquals(3, arr[0].length);
            assertNull(arr[0][0]);
        }

        @Test
        public void testToArrayH_typed() {
            Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1"), Arrays.asList("C1", "C2"));
            stringSheet.set("R1", "C1", "S11");
            stringSheet.set("R1", "C2", "S12");

            String[][] arr = stringSheet.toArrayH(String.class);
            assertEquals(1, arr.length);
            assertEquals(2, arr[0].length);
            assertEquals("S11", arr[0][0]);

            assertThrows(ArrayStoreException.class, () -> {
                sheet.toArrayH(Integer.class);
            });
        }

        @Test
        public void testToArrayV() {
            Object[][] arr = sheet.toArrayV();
            assertEquals(3, arr.length);
            assertEquals(3, arr[0].length);
            assertEquals("V11", arr[0][0]);
            assertEquals(true, arr[2][1]);
            assertNull(arr[2][2]);
        }

        @Test
        public void testToArrayV_uninitialized() {
            Sheet<String, String, String> uninit = new Sheet<>(rowKeys, colKeys);
            Object[][] arr = uninit.toArrayV();
            assertEquals(3, arr.length);
            assertEquals(3, arr[0].length);
            assertNull(arr[0][0]);
        }

        @Test
        public void testToArrayV_typed() {
            Sheet<String, String, String> stringSheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList("C1"));
            stringSheet.set("R1", "C1", "S11");
            stringSheet.set("R2", "C1", "S21");

            String[][] arr = stringSheet.toArrayV(String.class);
            assertEquals(1, arr.length);
            assertEquals(2, arr[0].length);
            assertEquals("S11", arr[0][0]);
            assertEquals("S21", arr[0][1]);

            assertThrows(ArrayStoreException.class, () -> {
                sheet.toArrayV(UUID.class);
            });
        }
    }

    @Nested
    @DisplayName("Functional Interface Methods")
    public class FunctionalInterfaceMethods {
        @Test
        public void testApply() {
            Integer totalNonNull = sheet.apply(s -> (int) s.countOfNonNullValues());
            assertEquals(4, totalNonNull);
        }

        @Test
        public void testApplyIfNotEmpty_nonEmptySheet() {
            u.Optional<Integer> result = sheet.applyIfNotEmpty(s -> (int) s.countOfNonNullValues());
            assertTrue(result.isPresent());
            assertEquals(4, result.get());
        }

        @Test
        public void testApplyIfNotEmpty_emptySheet() {
            Sheet<String, String, String> emptyS = Sheet.empty();
            u.Optional<Integer> result = emptyS.applyIfNotEmpty(s -> (int) s.countOfNonNullValues());
            assertFalse(result.isPresent());
        }

        @Test
        public void testAccept() {
            List<String> temp = new ArrayList<>();
            sheet.accept(s -> temp.add(s.get("R1", "C1").toString()));
            assertEquals(Arrays.asList("V11"), temp);
        }

        @Test
        public void testAcceptIfNotEmpty_nonEmptySheet() {
            List<String> temp = new ArrayList<>();
            If.OrElse result = sheet.acceptIfNotEmpty(s -> temp.add(s.get("R1", "C1").toString()));
            assertEquals(Arrays.asList("V11"), temp);
            assertSame(If.OrElse.TRUE, result);
        }

        @Test
        public void testAcceptIfNotEmpty_emptySheet() {
            Sheet<String, String, String> emptyS = Sheet.empty();
            List<String> temp = new ArrayList<>();
            If.OrElse result = emptyS.acceptIfNotEmpty(s -> temp.add("should_not_run"));
            assertTrue(temp.isEmpty());
            assertSame(If.OrElse.FALSE, result);
        }
    }

    @Nested
    @DisplayName("Print Methods")
    public class PrintMethods {
        private StringWriter stringWriter;

        @BeforeEach
        public void setUpWriter() {
            stringWriter = new StringWriter();
        }

        @Test
        public void testPrintln_defaultToStdout() {
            assertDoesNotThrow(() -> sheet.println());
        }

        @Test
        public void testPrintln_keysToStdout() {
            assertDoesNotThrow(() -> sheet.println(Arrays.asList("R1"), Arrays.asList("C1")));
        }

        @Test
        public void testPrintln_toWriter_full() {
            assertDoesNotThrow(() -> sheet.println(stringWriter));
            String output = stringWriter.toString();
            assertTrue(output.length() > 0);
            assertTrue(output.contains("R1"));
            assertTrue(output.contains("C1"));
            assertTrue(output.contains("V11"));
            assertTrue(output.contains("true"));
            assertTrue(output.contains("null"));
        }

        @Test
        public void testPrintln_toWriter_subset() {
            assertDoesNotThrow(() -> sheet.println(Arrays.asList("R1", "R2"), Arrays.asList("C1", "C3"), stringWriter));
            String output = stringWriter.toString();
            assertTrue(output.length() > 0);
            assertTrue(output.contains("R1"));
            assertTrue(output.contains("R2"));
            assertFalse(output.contains("R3"));
            assertTrue(output.contains("C1"));
            assertTrue(output.contains("C3"));
            assertFalse(output.contains("C2"));
            assertTrue(output.contains("V11"));
            assertTrue(output.contains("true"));
            assertFalse(output.contains("V12"));
        }

        @Test
        public void testPrintln_toWriter_emptySheet() {
            Sheet<String, String, String> emptySheet = new Sheet<>();
            assertDoesNotThrow(() -> emptySheet.println(stringWriter));
            String output = stringWriter.toString();
            assertTrue(output.contains("+---+"));
        }

        @Test
        public void testPrintln_toWriter_emptyKeySetsButDataExists() {
            assertDoesNotThrow(() -> sheet.println(Collections.emptyList(), Collections.emptyList(), stringWriter));
            String output = stringWriter.toString();
            assertTrue(output.contains("+---+"));
        }

        @Test
        public void testPrintln_withInvalidKeysToWriter() {
            assertThrows(IllegalArgumentException.class, () -> sheet.println(Arrays.asList("RX"), colKeys, stringWriter));
            assertThrows(IllegalArgumentException.class, () -> sheet.println(rowKeys, Arrays.asList("CX"), stringWriter));
        }

        @Test
        public void testPrintln_nullWriter() {
            assertThrows(IllegalArgumentException.class, () -> sheet.println((Writer) null));
            assertThrows(IllegalArgumentException.class, () -> sheet.println(rowKeys, colKeys, null));
        }
    }

    @Nested
    @DisplayName("Object Methods (hashCode, equals, toString)")
    public class ObjectMethods {
        @Test
        public void testHashCode() {
            Sheet<String, String, Object> sheet2 = sheet.copy();
            assertEquals(sheet.hashCode(), sheet2.hashCode());

            Sheet<String, String, Object> differentSheet = new Sheet<>(rowKeys, colKeys);
            differentSheet.set("R1", "C1", "Different");
            assertNotEquals(sheet.hashCode(), differentSheet.hashCode());

            Sheet<String, String, Object> empty1 = new Sheet<>();
            Sheet<String, String, Object> empty2 = new Sheet<>();
            assertEquals(empty1.hashCode(), empty2.hashCode());
        }

        @Test
        public void testEquals() {
            Sheet<String, String, Object> sheet2 = sheet.copy();
            assertEquals(sheet, sheet2);
            assertTrue(sheet.equals(sheet2));

            Sheet<String, String, Object> differentRowKeys = new Sheet<>(Arrays.asList("X1", "X2", "X3"), colKeys);
            differentRowKeys.set("X1", "C1", sheet.get("R1", "C1"));
            differentRowKeys.set("X1", "C2", sheet.get("R1", "C2"));
            differentRowKeys.set("X1", "C3", sheet.get("R1", "C3"));
            differentRowKeys.set("X2", "C1", sheet.get("R2", "C1"));
            assertNotEquals(sheet, differentRowKeys);

            Sheet<String, String, Object> differentColKeys = new Sheet<>(rowKeys, Arrays.asList("Y1", "Y2", "Y3"));
            differentColKeys.set("R1", "Y1", sheet.get("R1", "C1"));
            assertNotEquals(sheet, differentColKeys);

            Sheet<String, String, Object> differentValues = sheet.copy();
            differentValues.set("R1", "C1", "SomethingElse");
            assertNotEquals(sheet, differentValues);

            assertFalse(sheet.equals(null));
            assertFalse(sheet.equals("NotASheet"));

            Sheet<String, String, Object> uninit1 = new Sheet<>(rowKeys, colKeys);
            Sheet<String, String, Object> uninit2 = new Sheet<>(rowKeys, colKeys);
            assertEquals(uninit1, uninit2);

            uninit1.set("R1", "C1", "V");
            assertNotEquals(uninit1, uninit2);

            uninit2.set("R1", "C1", "V");
            assertEquals(uninit1, uninit2);

            uninit2.set("R1", "C1", "X");
            assertNotEquals(uninit1, uninit2);

        }

        @Test
        public void testToString() {
            String str = sheet.toString();
            assertNotNull(str);
            assertTrue(str.contains("rowKeySet=" + new ArrayList<>(rowKeys).toString()));
            assertTrue(str.contains("columnKeySet=" + new ArrayList<>(colKeys).toString()));
            assertTrue(str.contains("V11"));
            assertTrue(str.contains("100"));
            assertTrue(str.contains("true"));

            Sheet<String, String, String> emptySheet = new Sheet<>();
            assertNotNull(emptySheet.toString());
            assertTrue(emptySheet.toString().contains("rowKeySet=[]"));
            assertTrue(emptySheet.toString().contains("columnKeySet=[]"));

            Sheet<String, String, String> uninitSheet = new Sheet<>(rowKeys, colKeys);
            String uninitStr = uninitSheet.toString();
            assertTrue(uninitStr.contains("columns={}"));
        }
    }

    @Nested
    @DisplayName("Cell Record")
    public class CellRecord {
        @Test
        public void testCellOf() {
            Sheet.Cell<String, String, Integer> cell = Sheet.Cell.of("R", "C", 123);
            assertEquals("R", cell.rowKey());
            assertEquals("C", cell.columnKey());
            assertEquals(123, cell.value());
        }

        @Test
        public void testCellEqualityAndHashCode() {
            Sheet.Cell<String, String, Integer> cell1 = Sheet.Cell.of("R", "C", 123);
            Sheet.Cell<String, String, Integer> cell2 = Sheet.Cell.of("R", "C", 123);
            Sheet.Cell<String, String, Integer> cell3 = Sheet.Cell.of("R", "X", 123);
            Sheet.Cell<String, String, Integer> cell4 = Sheet.Cell.of("R", "C", 456);

            assertEquals(cell1, cell2);
            assertNotEquals(cell1, cell3);
            assertNotEquals(cell1, cell4);
            assertEquals(cell1.hashCode(), cell2.hashCode());
            assertNotEquals(cell1.hashCode(), cell3.hashCode());
        }

        @Test
        public void testCellToString() {
            Sheet.Cell<String, String, Integer> cell = Sheet.Cell.of("MyRow", "MyCol", 789);
            String str = cell.toString();
            assertTrue(str.contains("rowKey=MyRow"));
            assertTrue(str.contains("columnKey=MyCol"));
            assertTrue(str.contains("value=789"));
        }
    }

    @Nested
    @DisplayName("Point Record")
    public class PointRecord {
        @Test
        public void testPointOf_cached() {
            Sheet.Point p1 = Sheet.Point.of(0, 0);
            Sheet.Point p2 = Sheet.Point.of(0, 0);
            assertSame(p1, p2, "Points (0,0) should be cached and thus the same instance");
            assertEquals(0, p1.rowIndex());
            assertEquals(0, p1.columnIndex());

            Sheet.Point p3 = Sheet.Point.of(Sheet.Point.ZERO.rowIndex(), Sheet.Point.ZERO.columnIndex());
            assertSame(Sheet.Point.ZERO, p3, "Point.ZERO should be cached");

            Sheet.Point p_max_cache = Sheet.Point.of(127, 127);
            Sheet.Point p_max_cache_again = Sheet.Point.of(127, 127);
            assertSame(p_max_cache, p_max_cache_again, "Points at edge of cache should be cached");
        }

        @Test
        public void testPointOf_notCached() {
            Sheet.Point p_outside_cache1 = Sheet.Point.of(128, 128);
            Sheet.Point p_outside_cache2 = Sheet.Point.of(128, 128);
            assertNotSame(p_outside_cache1, p_outside_cache2, "Points outside cache range should be new instances");
            assertEquals(p_outside_cache1, p_outside_cache2, "Points outside cache should still be equal by value");

            Sheet.Point p_mixed_cache = Sheet.Point.of(0, 128);
            Sheet.Point p_mixed_cache_again = Sheet.Point.of(0, 128);
            assertNotSame(p_mixed_cache, p_mixed_cache_again);
            assertEquals(p_mixed_cache, p_mixed_cache_again);

        }

        @Test
        public void testPointEqualityAndHashCode() {
            Sheet.Point p1 = Sheet.Point.of(10, 20);
            Sheet.Point p2 = Sheet.Point.of(10, 20);
            Sheet.Point p3 = Sheet.Point.of(10, 21);
            Sheet.Point p4 = Sheet.Point.of(200, 300);
            Sheet.Point p5 = Sheet.Point.of(200, 300);

            assertEquals(p1, p2);
            assertNotEquals(p1, p3);
            assertEquals(p4, p5);
            assertNotEquals(p1, p4);

            assertEquals(p1.hashCode(), p2.hashCode());
            assertNotEquals(p1.hashCode(), p3.hashCode());
            assertEquals(p4.hashCode(), p5.hashCode());
        }

        @Test
        public void testPointToString() {
            Sheet.Point p = Sheet.Point.of(5, 15);
            String str = p.toString();
            assertTrue(str.contains("rowIndex=5"));
            assertTrue(str.contains("columnIndex=15"));
        }
    }
}
