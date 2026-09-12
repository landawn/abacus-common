package com.landawn.abacus.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

public abstract class SheetTestSupport extends AbstractTest {

    protected Sheet<String, String, Integer> sheet;
    protected Sheet<String, String, Integer> emptySheet;
    protected Sheet<String, String, Object> objectSheet;
    protected Sheet<String, String, Integer> intSheet;
    protected Sheet<String, String, Integer> sortSheet;
    protected List<String> rowKeys;
    protected List<String> columnKeys;
    protected List<String> upperRowKeys;
    protected List<String> colKeys;
    protected Integer[][] sampleData;
    protected StringWriter stringWriter;

    @BeforeEach
    public void setUp() {
        rowKeys = Arrays.asList("row1", "row2", "row3");
        columnKeys = Arrays.asList("col1", "col2", "col3");
        upperRowKeys = Arrays.asList("R1", "R2", "R3");
        colKeys = Arrays.asList("C1", "C2", "C3");
        sampleData = new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        sheet = Sheet.rows(rowKeys, columnKeys, sampleData);
        emptySheet = Sheet.empty();
        objectSheet = Sheet.rows(upperRowKeys, colKeys, new Object[][] { { "V11", "V12", null }, { 100, null, true }, { null, null, null } });
        intSheet = Sheet.rows(upperRowKeys, colKeys, new Integer[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
        sortSheet = Sheet.rows(Arrays.asList("B", "C", "A"), Arrays.asList("Y", "Z", "X"), new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        stringWriter = new StringWriter();
    }

    // L4390: Stream.empty() in columnMajorCells(fromColumnIndex, toColumnIndex) when rowCount == 0

    // L3083: sortRowsByColumnValues(C, Comparator) returns early when !_isInitialized

    // L3192: sortRowsByColumnValues(Collection, Comparator) returns early when !_isInitialized

    // L3423: sortColumnsByRowValues(R, Comparator) returns early when !_isInitialized

    // L3522: sortColumnsByRowValues(Collection, Comparator) returns early when !_isInitialized

    // A null comparator is rejected before sorting.

    // L4316, L4319, L4323-4326: advance() and count() in rowMajorCells() ObjIteratorEx

    // L4303: NoSuchElementException in rowMajorCells() iterator next() when exhausted

    // L4422, L4425, L4429-4432: advance() and count() in columnMajorCells() ObjIteratorEx

    // L4409: NoSuchElementException in columnMajorCells() iterator next() when exhausted

    // -------- Bug fix: equals / hashCode must treat uninitialized vs initialized-all-null as equal --------

    // -------- transpose round-trip --------

    // -------- copy independence (deep wrt nested column lists) --------

    // -------- Bug fix: setColumn should keep previously returned column views consistent --------

    // -------- Frozen sheet: defense against modification through views --------

    // -------- get/set absent key handling --------

    // -------- remove does not shrink dimensions --------

    // -------- isEmpty / size semantics --------

    // -------- putAll merge semantics --------

    // -------- cellSet equivalent: rowMajorCells yields all triples --------

    // -------- iteration order consistency --------

    // -------- regression: init() must keep row/column index maps non-null --------

    // --- regression tests for 2026-06-10 deep-review fixes ---

    // ==================================================================================================
    // Fixes from the 2026-08-31 Dataset/RowDataset/Sheet/Array review.
    // ==================================================================================================

    // -------- J5: trimToSize is allocation-only, so it is allowed on a frozen Sheet --------

    // -------- O2: order-sensitive hashCode/equals, computed without copying the key sets --------

    // -------- D6: the extracted permutation helpers handle multi-cycle orderings --------

    // -------- O4: set(rowKey, columnKey, value) still returns the replaced value --------

    // -------- B1 (cont.): keyed views track their key through insertions on both axes --------

    /** Applies one randomly chosen structural (or value) mutation, skipping any that the current shape forbids. */
    protected void applyRandomStructuralChange(final Sheet<String, String, Integer> sheet, final Random rnd, final int nextId) {
        final List<String> rows = new ArrayList<>(sheet.rowKeySet());
        final List<String> columns = new ArrayList<>(sheet.columnKeySet());

        switch (rnd.nextInt(11)) {
            case 0:
                if (rows.size() > 1) {
                    sheet.moveRow(rows.get(rnd.nextInt(rows.size())), rnd.nextInt(rows.size()));
                }
                break;
            case 1:
                if (rows.size() > 1) {
                    sheet.swapRows(rows.get(rnd.nextInt(rows.size())), rows.get(rnd.nextInt(rows.size())));
                }
                break;
            case 2:
                sheet.sortByRowKey();
                break;
            case 3:
                if (columns.size() > 1) {
                    sheet.moveColumn(columns.get(rnd.nextInt(columns.size())), rnd.nextInt(columns.size()));
                }
                break;
            case 4:
                if (columns.size() > 1) {
                    sheet.swapColumns(columns.get(rnd.nextInt(columns.size())), columns.get(rnd.nextInt(columns.size())));
                }
                break;
            case 5:
                sheet.sortByColumnKey();
                break;
            case 6: {
                final List<Integer> newRow = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    newRow.add(1000 + nextId);
                }
                sheet.addRow(rnd.nextInt(rows.size() + 1), "added-r" + nextId, newRow);
                break;
            }
            case 7: {
                final List<Integer> newColumn = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    newColumn.add(2000 + nextId);
                }
                sheet.addColumn(rnd.nextInt(columns.size() + 1), "added-c" + nextId, newColumn);
                break;
            }
            case 8:
                if (rows.size() > 1) {
                    sheet.removeRow(rows.get(rnd.nextInt(rows.size())));
                }
                break;
            case 9:
                if (columns.size() > 1) {
                    sheet.removeColumn(columns.get(rnd.nextInt(columns.size())));
                }
                break;
            default:
                if (!rows.isEmpty() && !columns.isEmpty()) {
                    sheet.set(rows.get(rnd.nextInt(rows.size())), columns.get(rnd.nextInt(columns.size())), 3000 + nextId);
                }
                break;
        }
    }
}
