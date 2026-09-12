package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for cycle 2 of the 2026-09-02 iterative review of {@code Dataset}, {@code RowDataset} and
 * {@code Sheet} (ledger {@code scripts/cross_review/Dataset_RowDataset_Sheet_ledger_2026-09-02.md}, C-043).
 *
 * <p>Unless its javadoc says it only pins already-correct behaviour, every test here fails against the pre-fix
 * classes.</p>
 */
public class DatasetSheetRegressionCTest extends TestBase {

    public static class Order {
        private Integer id;
        private String name;
        private List<Item> items;
        private Address address;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(final List<Item> items) {
            this.items = items;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(final Address address) {
            this.address = address;
        }
    }

    public static class Item {
        private Integer id;
        private String sku;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(final String sku) {
            this.sku = sku;
        }
    }

    public static class Address {
        private String city;
        private String zip;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(final String zip) {
            this.zip = zip;
        }
    }

    private static final List<String> ITEM_COLUMNS = Arrays.asList("id", "name", "items.id", "items.sku");

    private static List<Integer> itemIds(final Order o) {
        if (o.getItems() == null) {
            return null;
        }

        return Stream.of(o.getItems()).map(Item::getId).toList();
    }

    @Nested
    public class C043_AllNullNestedRowsNeverBecomePhantomChildren {

        /** The differential-probe input: three parents without children do not affect nested merging. */
        @Test
        public void twoOrMoreParentsWithoutChildrenDoNotGetEmptyItems() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", null, null }, { 0, "n0", 2, "s2" }, { null, "nnull", null, null },
                    { null, "nnull", 1, "s1" }, { 2, "n2", null, null }, { 1, "n1", 2, "s2" } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(3, orders.size()); // ids 1, 0, 2 in first-occurrence order; null-id rows dropped
            assertEquals(Integer.valueOf(1), orders.get(0).getId());
            assertEquals(Arrays.asList(2), itemIds(orders.get(0))); // used to be [null, 2]
            assertEquals("s2", orders.get(0).getItems().get(0).getSku());
            assertEquals(Integer.valueOf(0), orders.get(1).getId());
            assertEquals(Arrays.asList(2), itemIds(orders.get(1)));
            assertEquals(Integer.valueOf(2), orders.get(2).getId());
            assertTrue(CommonUtil.isEmpty(orders.get(2).getItems()), String.valueOf(itemIds(orders.get(2)))); // used to be [null]
        }

        /** A single parent without children follows the same missing-child rule. */
        @Test
        public void aSingleParentWithoutChildrenBehavesTheSameWay() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", 5, "s5" }, { 2, "n2", null, null }, { 1, "n1", 6, "s6" } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(2, orders.size());
            assertEquals(Arrays.asList(5, 6), itemIds(orders.get(0)));
            assertTrue(CommonUtil.isEmpty(orders.get(1).getItems()));
        }

        @Test
        public void wholeDatasetOfParentsWithoutChildren() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", null, null }, { 2, "n2", null, null }, { 1, "n1", null, null } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(2, orders.size());
            assertTrue(CommonUtil.isEmpty(orders.get(0).getItems()));
            assertTrue(CommonUtil.isEmpty(orders.get(1).getItems()));
        }

        /** Populated children without IDs are preserved without changing deduplication elsewhere. */
        @Test
        public void nestedRowsWithANullIdArePreservedIndependently() {
            final Dataset one = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", null, "loose" }, { 2, "n2", null, null }, { 3, "n3", null, null } });
            final List<Order> orders = one.toMergedEntities("id", Order.class);
            assertEquals(3, orders.size());
            assertEquals(Arrays.asList((Integer) null), itemIds(orders.get(0)));
            assertTrue(CommonUtil.isEmpty(orders.get(1).getItems()));
            assertTrue(CommonUtil.isEmpty(orders.get(2).getItems()));

            final Dataset two = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", null, "loose1" }, { 2, "n2", null, null }, { 3, "n3", null, "loose3" },
                    { 1, "n1", 5, "s5" }, { 1, "n1", 5, "s5" } });
            final List<Order> orders2 = two.toMergedEntities("id", Order.class);
            assertEquals(3, orders2.size());
            assertEquals(Arrays.asList(null, 5), itemIds(orders2.get(0))); // missing IDs do not disable merging of valid IDs
            assertTrue(CommonUtil.isEmpty(orders2.get(1).getItems()));
            assertEquals(Arrays.asList((Integer) null), itemIds(orders2.get(2)));
        }

        /** Childless parents and dropped (null-id) rows must not switch de-duplication off for the parents that have children. */
        @Test
        public void childlessParentsDoNotDisableNestedMerging() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 0, "n0", 1, "s1" }, { 0, "n0", 1, "s1" }, { 0, "n0", 3, "s3" },
                    { null, "nnull", null, null }, { null, "nnull", null, null }, { 2, "n2", 3, "s3" }, { 4, "n4", null, null }, { 5, "n5", null, null } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(4, orders.size());
            assertEquals(Arrays.asList(1, 3), itemIds(orders.get(0))); // used to be [1, 1, 3]
            assertEquals(Arrays.asList(3), itemIds(orders.get(1)));
            assertTrue(CommonUtil.isEmpty(orders.get(2).getItems()));
            assertTrue(CommonUtil.isEmpty(orders.get(3).getItems()));
        }

        /**
         * Pins the same rule for a single nested bean property. {@code Address} has no id property, so its nested
         * merge keys are all of its columns and an all-null row was already dropped; the fix must keep it that way.
         */
        @Test
        public void singleNestedBeanPropertyStaysNullForAllNullRows() {
            final List<String> columns = Arrays.asList("id", "name", "address.city", "address.zip");
            final Dataset ds = Dataset.rows(columns, new Object[][] { { 1, "n1", null, null }, { 2, "n2", "Berlin", "10115" }, { 3, "n3", null, null } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(3, orders.size());
            assertNull(orders.get(0).getAddress());
            assertNotNull(orders.get(1).getAddress());
            assertEquals("Berlin", orders.get(1).getAddress().getCity());
            assertNull(orders.get(2).getAddress());
        }

        /** Pins the unchanged non-merging conversion: toList(Bean.class) still materialises an empty nested bean per row. */
        @Test
        public void nonMergingConversionsAreUnchanged() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS, new Object[][] { { 1, "n1", null, null }, { 2, "n2", null, null } });

            final List<Order> orders = ds.toList(Order.class);

            assertEquals(2, orders.size());
            assertEquals(1, orders.get(0).getItems().size());
            assertNull(orders.get(0).getItems().get(0).getId());

            final List<Order> entities = ds.toEntities(null, Order.class);
            assertEquals(1, entities.get(1).getItems().size());
        }

        /** Pins the documented example shape: children are merged by id and duplicates collapse within each parent. */
        @Test
        public void mergedChildrenWithTrustedIdsAreUnchanged() {
            final Dataset ds = Dataset.rows(ITEM_COLUMNS,
                    new Object[][] { { 1, "n1", 5, "s5" }, { 1, "n1", 6, "s6" }, { 1, "n1", 5, "s5" }, { 2, "n2", 7, "s7" } });

            final List<Order> orders = ds.toMergedEntities("id", Order.class);

            assertEquals(2, orders.size());
            assertEquals(Arrays.asList(5, 6), itemIds(orders.get(0)));
            assertEquals(Arrays.asList(7), itemIds(orders.get(1)));
        }
    }
}
