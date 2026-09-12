package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

import testfixtures.entity.extendDirty.basic.Account;

public class DatasetRemoveTest extends DatasetTestSupport {
    @Test
    public void testRemoveDuplicateRows3() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "IT" }, { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"));
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));

    }

    @Test
    public void testRemoveColumn() {
        Dataset ds = dataset.copy();
        List<String> removed = ds.removeColumn("name");

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("name"));
        assertEquals(5, removed.size());
        assertEquals("John", removed.get(0));
    }

    @Test
    public void test_removeColumn() throws Exception {
        final List<Account> accountList = createAccountList(Account.class, 9);
        final Dataset ds = CommonUtil.newDataset(accountList);

        assertTrue(ds.getColumnIndex("firstName") >= 0);
        assertTrue(ds.getColumnIndex("lastName") >= 0);
        assertTrue(ds.getColumnIndex("birthDate") >= 0);

        ds.removeColumns(CommonUtil.toList("firstName", "lastName", "birthDate"));

        assertFalse(ds.containsColumn("firstName"));
        assertFalse(ds.containsColumn("lastName"));
    }

    @Test
    public void removeColumn() {
        List<Object> removedCol = sampleDataset.removeColumn("Name");
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), removedCol);
        assertEquals(2, sampleDataset.columnCount());
        assertFalse(sampleDataset.columnNames().contains("Name"));
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.removeColumn("NonExistent"));
    }

    @Test
    public void testRemoveColumns() {
        Dataset ds = dataset.copy();
        ds.removeColumns(Arrays.asList("name", "age"));

        assertEquals(2, ds.columnCount());
        assertTrue(ds.containsColumn("id"));
        assertTrue(ds.containsColumn("salary"));
        assertFalse(ds.containsColumn("name"));
        assertFalse(ds.containsColumn("age"));
    }

    @Test
    public void testRemoveColumns_WithPredicate() {
        Dataset ds = dataset.copy();
        ds.removeColumns(col -> col.startsWith("s"));

        assertEquals(3, ds.columnCount());
        assertFalse(ds.containsColumn("salary"));
    }

    @Test
    public void removeColumns_collection() {
        sampleDataset.removeColumns(Arrays.asList("ID", "Age"));
        assertEquals(1, sampleDataset.columnCount());
        assertEquals(Collections.singletonList("Name"), sampleDataset.columnNames());
    }

    @Test
    public void removeColumns_predicate() {
        assertThrows(IllegalArgumentException.class, () -> sampleDataset.removeColumns(name -> name.equals("ID") || name.endsWith("e")));
        sampleDataset.clear();
        sampleDataset.removeColumns(name -> name.equals("ID") || name.endsWith("e"));
        assertEquals(0, sampleDataset.columnCount());
        assertTrue(sampleDataset.columnNames().isEmpty());
    }

    @Test
    public void testRemoveRow() {
        Dataset ds = dataset.copy();
        ds.removeRow(0);

        assertEquals(4, ds.size());
        assertEquals(Integer.valueOf(2), ds.get(0, 0));
    }

    @Test
    public void testRemoveRowRange() {
        Dataset ds = dataset.copy();
        ds.removeRows(1, 3);
        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 1));
        assertEquals("Alice", ds.get(1, 1));
    }

    @Test
    public void test_removeRowRange() {
        final Dataset ds = CommonUtil.newDataset(CommonUtil.toList(createAccount(Account.class), createAccount(Account.class), createAccount(Account.class),
                createAccount(Account.class), createAccount(Account.class), createAccount(Account.class)));

        final Dataset ds1 = ds.copy();

        ds1.removeRows(1, 5);

        final Dataset ds2 = ds.copy();

        ds2.removeRowsAt(1, 3, 5);

        final Dataset ds3 = ds.copy();

        ds3.removeRowsAt(0, 2, 4, 5);
        assertNotNull(ds3);
    }

    @Test
    public void testRemoveRowRange_EdgeCases() {
        Dataset ds = dataset.copy();

        ds.removeRows(0, 2);
        assertEquals(3, ds.size());
        assertEquals(Integer.valueOf(3), ds.get(0, 0));

        ds.removeRows(0, 1);
        assertEquals(2, ds.size());
        assertEquals(Integer.valueOf(4), ds.get(0, 0));
    }

    @Test
    public void removeRow() {
        sampleDataset.removeRow(1);
        assertEquals(2, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(1).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRow(5));
    }

    @Test
    public void removeRowRange() {
        sampleDataset.removeRows(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Charlie", sampleDataset.moveToRow(0).get("Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> sampleDataset.removeRows(0, 5));
    }

    @Test
    public void removeRows_indices() {
        sampleDataset.removeRowsAt(0, 2);
        assertEquals(1, sampleDataset.size());
        assertEquals("Bob", sampleDataset.moveToRow(0).get("Name"));
    }

    @Test
    public void testRemoveRows() {
        Dataset ds = dataset.copy();
        ds.removeRowsAt(1, 2);
        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 1));
        assertEquals("Alice", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name"), new Object[][] { { 1, "A" }, { 2, "B" }, { 1, "C" } });
        ds.removeDuplicateRowsBy("id");
        assertEquals(2, ds.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumns() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsByColumnsWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "Alice", 30, 70000.0 });

        dataset.removeDuplicateRowsBy(Arrays.asList("name", "age"), (DisposableObjArray row) -> row.get(0).toString() + "_" + row.get(1));

        assertEquals(4, dataset.size());
    }

    @Test
    public void testRemoveDuplicateRowsBy() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John", "IT" }, { 2, "Jane", "HR" }, { 3, "John", "Finance" }, { 4, "Bob", "IT" } });
        dataset.removeDuplicateRowsBy("name");
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy2() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "Finance" }, { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy("name", name -> ((String) name).split(" ")[1]);
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy4() {
        Dataset dataset = Dataset.rows(Arrays.asList("id", "name", "department"),
                new Object[][] { { 1, "John Doe", "IT" }, { 2, "Jane Smith", "HR" }, { 3, "Johnathan Doe", "IT" }, { 4, "Bob Brown", "IT" } });
        dataset.removeDuplicateRowsBy(Arrays.asList("name", "department"), row -> ((String) row.get(0)).split(" ")[1] + "|" + row.get(1));
        assertEquals(3, dataset.size());
        assertEquals((Integer) 1, dataset.get(0, 0));
        assertEquals((Integer) 2, dataset.get(1, 0));
        assertEquals((Integer) 4, dataset.get(2, 0));
    }

    @Test
    public void testRemoveDuplicateRowsBy_SingleColumn() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "type"),
                new Object[][] { { 1, "Alice", "A" }, { 2, "Bob", "B" }, { 3, "Charlie", "A" }, { 4, "Diana", "B" }, { 5, "Eve", "A" } });

        ds.removeDuplicateRowsBy("type");

        assertEquals(2, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_WithKeyExtractor() {
        Dataset ds = Dataset.rows(Arrays.asList("id", "name", "email"), new Object[][] { { 1, "Alice", "alice@test.com" }, { 2, "Bob", "bob@test.com" },
                { 3, "Charlie", "charlie@test.com" }, { 4, "Diana", "alice@test.com" } });

        ds.removeDuplicateRowsBy("email", (String email) -> email.toLowerCase());

        assertEquals(3, ds.size());
        assertEquals("Alice", ds.get(0, 1));
        assertEquals("Bob", ds.get(1, 1));
        assertEquals("Charlie", ds.get(2, 1));
    }

    @Test
    public void testRemoveDuplicateRowsBy_MultipleColumns() {
        Dataset ds = Dataset.rows(Arrays.asList("firstName", "lastName", "age"),
                new Object[][] { { "John", "Doe", 25 }, { "Jane", "Smith", 30 }, { "John", "Doe", 25 }, { "John", "Smith", 28 } });

        ds.removeDuplicateRowsBy(Arrays.asList("firstName", "lastName"));

        assertEquals(3, ds.size());
        assertEquals("John", ds.get(0, 0));
        assertEquals("Doe", ds.get(0, 1));
        assertEquals("Jane", ds.get(1, 0));
        assertEquals("Smith", ds.get(1, 1));
        assertEquals("John", ds.get(2, 0));
        assertEquals("Smith", ds.get(2, 1));
    }

    @Test
    public void testRemoveDuplicateRowsByColumnWithExtractor() {
        Dataset dataset = testDataset.copy();
        dataset.addRow(new Object[] { 5, "ALICE", 40, 70000.0 });

        dataset.removeDuplicateRowsBy("name", (String name) -> name.toLowerCase());

        assertEquals(4, dataset.size());
    }

}
