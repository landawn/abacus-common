package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

public class SheetUpdateTest extends SheetTestSupport {
    @Test
    public void testUpdateRow() {
        sheet.updateRow("row1", v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(20), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(30), sheet.get("row1", "col3"));
    }

    @Test
    public void testUpdateRow_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateRow("row1", v -> v == null ? 42 : v);
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col2"));
    }

    @Test
    public void testUpdateRowInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.updateRow("invalidRow", v -> v);
        });
    }

    @Test
    public void testUpdateRowOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateRow("row1", v -> v);
        });
    }

    @Test
    public void testUpdateRow_frozen() {
        intSheet.freeze();
        assertThrows(IllegalStateException.class, () -> intSheet.updateRow("R1", v -> v + 1));
    }

    @Test
    public void testUpdateColumn() {
        sheet.updateColumn("col1", v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(40), sheet.get("row2", "col1"));
        assertEquals(Integer.valueOf(70), sheet.get("row3", "col1"));
    }

    @Test
    public void testUpdateColumn_UninitializedSheet() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateColumn("col1", v -> v == null ? 42 : v);
        assertEquals(Integer.valueOf(42), uninitSheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(42), uninitSheet.get("row2", "col1"));
    }

    @Test
    public void testUpdateColumnInvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            sheet.updateColumn("invalidCol", v -> v);
        });
    }

    @Test
    public void testUpdateColumnOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateColumn("col1", v -> v);
        });
    }

    @Test
    public void testUpdateColumn_frozen() {
        intSheet.freeze();
        assertThrows(IllegalStateException.class, () -> intSheet.updateColumn("C1", v -> v + 1));
    }

    @Test
    public void testUpdateAllWithIntBiFunction() {
        sheet.updateAll((rowIdx, colIdx) -> rowIdx * 10 + colIdx);
        assertEquals(Integer.valueOf(0), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(11), sheet.getAt(1, 1));
        assertEquals(Integer.valueOf(22), sheet.getAt(2, 2));
    }

    @Test
    public void testUpdateAllWithTriFunction() {
        sheet.updateAll((rowKey, colKey, value) -> {
            if (rowKey.equals("row1")) {
                return value * 100;
            }
            return value;
        });
        assertEquals(Integer.valueOf(100), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(200), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(300), sheet.get("row1", "col3"));
        assertEquals(Integer.valueOf(4), sheet.get("row2", "col1"));
    }

    @Test
    public void testUpdateAll_byIndices() {
        intSheet.updateAll((rIdx, cIdx) -> (rIdx + 1) * 100 + (cIdx + 1) * 10);
        assertEquals(110, intSheet.getAt(0, 0));
        assertEquals(330, intSheet.getAt(2, 2));
    }

    @Test
    public void testUpdateAllWithIndices() {
        sheet.updateAll((rowIndex, columnIndex) -> rowIndex * 10 + columnIndex);

        assertEquals(Integer.valueOf(0), sheet.getAt(0, 0));
        assertEquals(Integer.valueOf(11), sheet.getAt(1, 1));
        assertEquals(Integer.valueOf(22), sheet.getAt(2, 2));
    }

    @Test
    public void testUpdateAllWithKeys() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.updateAll((rowKey, columnKey, value) -> {
            int rowNum = Integer.parseInt(rowKey.substring(1));
            int colNum = Integer.parseInt(columnKey.substring(1));
            return rowNum * colNum;
        });

        assertEquals(Integer.valueOf(1), uninitSheet.get("R1", "C1"));
        assertEquals(Integer.valueOf(4), uninitSheet.get("R2", "C2"));
        assertEquals(Integer.valueOf(9), uninitSheet.get("R3", "C3"));
    }

    @Test
    public void testUpdateAll_UninitializedWithIntBiFunction() {
        Sheet<String, String, Integer> uninitSheet = new Sheet<>(rowKeys, columnKeys);
        uninitSheet.updateAll((rowIdx, colIdx) -> rowIdx * 10 + colIdx);
        assertEquals(Integer.valueOf(0), uninitSheet.getAt(0, 0));
        assertEquals(Integer.valueOf(11), uninitSheet.getAt(1, 1));
        assertEquals(Integer.valueOf(22), uninitSheet.getAt(2, 2));
    }

    @Test
    public void testUpdateAll() {
        sheet.updateAll(v -> v == null ? 0 : v * 10);
        assertEquals(Integer.valueOf(10), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(50), sheet.get("row2", "col2"));
        assertEquals(Integer.valueOf(90), sheet.get("row3", "col3"));
    }

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
    public void testUpdateAllOnEmptySheet() {
        Sheet<String, String, Integer> emptySheet = new Sheet<>();
        emptySheet.updateAll(v -> 100);

        Sheet<String, String, Integer> uninitSheet = new Sheet<>(upperRowKeys, colKeys);
        uninitSheet.updateAll(v -> 100);
        assertEquals(Integer.valueOf(100), uninitSheet.getAt(0, 0));
    }

    @Test
    public void testUpdateAll_TriFunction_New() {
        sheet.updateAll((rowKey, colKey, val) -> val == null ? -1 : val * 2);
        assertEquals(Integer.valueOf(2), sheet.get("row1", "col1"));
        assertEquals(Integer.valueOf(4), sheet.get("row1", "col2"));
        assertEquals(Integer.valueOf(18), sheet.get("row3", "col3"));
    }

    @Test
    public void testUpdateAllOnFrozenSheet() {
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> {
            sheet.updateAll(v -> v);
        });
    }

    @Test
    public void testUpdateAll_frozen() {
        objectSheet.freeze();
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll(v -> v));
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll((r, c) -> "v"));
        assertThrows(IllegalStateException.class, () -> objectSheet.updateAll((r, c, v) -> v));
    }

    /**
     * A frozen Sheet cannot honour any mutator, whatever its arguments are, so {@code checkFrozen()} runs
     * before argument validation in every one of them. This is the order the whole class had before the
     * argument null-checks were introduced; those were inserted above {@code checkFrozen()} in 16 methods,
     * which made a frozen Sheet report {@code IllegalArgumentException} from the update, replaceIf, sort and
     * putAll families but {@code IllegalStateException} from the add and rename ones.
     */
    @Test
    public void testFrozenCheckPrecedesArgumentValidation() {
        sheet.freeze();

        assertThrows(IllegalStateException.class, () -> sheet.updateRow("row1", null));
        assertThrows(IllegalStateException.class, () -> sheet.updateColumn("col1", null));
        assertThrows(IllegalStateException.class, () -> sheet.updateAll((Function<Integer, Integer>) null));
        assertThrows(IllegalStateException.class, () -> sheet.updateAll((IntBiFunction<Integer>) null));
        assertThrows(IllegalStateException.class, () -> sheet.updateAll((TriFunction<String, String, Integer, Integer>) null));
        assertThrows(IllegalStateException.class, () -> sheet.replaceIf((Predicate<Integer>) null, 0));
        assertThrows(IllegalStateException.class, () -> sheet.replaceIf((IntBiPredicate) null, 0));
        assertThrows(IllegalStateException.class, () -> sheet.replaceIf((TriPredicate<String, String, Integer>) null, 0));

        assertThrows(IllegalStateException.class, () -> sheet.putAll(null));
        assertThrows(IllegalStateException.class, () -> sheet.putAll(null, (BiFunction<Integer, Integer, Integer>) null));

        assertThrows(IllegalStateException.class, () -> sheet.sortByRowKey(null));
        assertThrows(IllegalStateException.class, () -> sheet.sortByColumnKey(null));
        assertThrows(IllegalStateException.class, () -> sheet.sortRowsByColumnValues("col1", null));
        assertThrows(IllegalStateException.class, () -> sheet.sortRowsByColumnValues((Collection<String>) null, (Comparator<Object[]>) null));
        assertThrows(IllegalStateException.class, () -> sheet.sortColumnsByRowValues("row1", null));
        assertThrows(IllegalStateException.class, () -> sheet.sortColumnsByRowValues((Collection<String>) null, (Comparator<Object[]>) null));

        // add*/rename* already checked the frozen state first; they must keep doing so.
        assertThrows(IllegalStateException.class, () -> sheet.addRow(null, null));
        assertThrows(IllegalStateException.class, () -> sheet.addColumn(null, null));
        assertThrows(IllegalStateException.class, () -> sheet.renameRow("row1", null));
        assertThrows(IllegalStateException.class, () -> sheet.renameColumn("col1", null));
    }

    @Test
    public void testNullArgumentsStillRejectedWhenNotFrozen() {
        assertThrows(IllegalArgumentException.class, () -> sheet.updateRow("row1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.updateColumn("col1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.updateAll((Function<Integer, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.updateAll((IntBiFunction<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.updateAll((TriFunction<String, String, Integer, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.replaceIf((Predicate<Integer>) null, 0));
        assertThrows(IllegalArgumentException.class, () -> sheet.replaceIf((IntBiPredicate) null, 0));
        assertThrows(IllegalArgumentException.class, () -> sheet.replaceIf((TriPredicate<String, String, Integer>) null, 0));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(null));
        assertThrows(IllegalArgumentException.class, () -> sheet.putAll(null, (BiFunction<Integer, Integer, Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortByRowKey(null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortByColumnKey(null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues("col1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues((Collection<String>) null, (Comparator<Object[]>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues("row1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues((Collection<String>) null, (Comparator<Object[]>) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addRow(null, null));
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(null, null));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameRow("row1", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.renameColumn("col1", null));
    }

}
