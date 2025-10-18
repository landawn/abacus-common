package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class EntityId2025Test extends TestBase {

    @Test
    public void test_of_singleProperty() {
        EntityId id = EntityId.of("User.id", 12345);

        Assertions.assertNotNull(id);
        Assertions.assertEquals("User", id.entityName());
        Assertions.assertEquals(12345, (Integer) id.get("id"));
        Assertions.assertEquals(1, id.size());
    }

    @Test
    public void test_of_singleProperty_withEntityName() {
        EntityId id = EntityId.of("Account", "accountNumber", "AC-12345");

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Account", id.entityName());
        Assertions.assertEquals("AC-12345", id.get("accountNumber"));
        Assertions.assertEquals(1, id.size());
    }

    @Test
    public void test_of_twoProperties() {
        EntityId id = EntityId.of("Order.customerId", 100, "Order.orderId", 5000);

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Order", id.entityName());
        Assertions.assertEquals(100, (Integer) id.get("customerId"));
        Assertions.assertEquals(5000, (Integer) id.get("orderId"));
        Assertions.assertEquals(2, id.size());
    }

    @Test
    public void test_of_twoProperties_withEntityName() {
        EntityId id = EntityId.of("OrderLine", "orderId", 1000, "lineNumber", 1);

        Assertions.assertNotNull(id);
        Assertions.assertEquals("OrderLine", id.entityName());
        Assertions.assertEquals(1000, (Integer) id.get("orderId"));
        Assertions.assertEquals(1, (Integer) id.get("lineNumber"));
        Assertions.assertEquals(2, id.size());
    }

    @Test
    public void test_of_threeProperties() {
        EntityId id = EntityId.of(
            "Stock.warehouseId", 10,
            "Stock.productId", "PROD-123",
            "Stock.batchId", "BATCH-2023"
        );

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Stock", id.entityName());
        Assertions.assertEquals(10, (Integer) id.get("warehouseId"));
        Assertions.assertEquals("PROD-123", id.get("productId"));
        Assertions.assertEquals("BATCH-2023", id.get("batchId"));
        Assertions.assertEquals(3, id.size());
    }

    @Test
    public void test_of_threeProperties_withEntityName() {
        EntityId id = EntityId.of("Inventory",
            "warehouseId", 5,
            "productId", "SKU-789",
            "locationId", "A-10-3"
        );

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Inventory", id.entityName());
        Assertions.assertEquals(5, (Integer) id.get("warehouseId"));
        Assertions.assertEquals("SKU-789", id.get("productId"));
        Assertions.assertEquals("A-10-3", id.get("locationId"));
        Assertions.assertEquals(3, id.size());
    }

    @Test
    public void test_create_fromMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("User.id", 12345);
        props.put("User.name", "John Doe");

        EntityId id = EntityId.create(props);

        Assertions.assertNotNull(id);
        Assertions.assertEquals(12345, (Integer) id.get("id"));
        Assertions.assertEquals("John Doe", id.get("name"));
    }

    @Test
    public void test_create_fromMap_withEntityName() {
        Map<String, Object> props = new HashMap<>();
        props.put("firstName", "John");
        props.put("lastName", "Doe");
        props.put("email", "john.doe@example.com");

        EntityId id = EntityId.create("Person", props);

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Person", id.entityName());
        Assertions.assertEquals("John", id.get("firstName"));
        Assertions.assertEquals("Doe", id.get("lastName"));
        Assertions.assertEquals("john.doe@example.com", id.get("email"));
        Assertions.assertEquals(3, id.size());
    }

    @Test
    public void test_create_fromMap_empty() {
        Map<String, Object> props = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> EntityId.create(props));
    }

    @Test
    public void test_getInt() {
        EntityId id = EntityId.of("Product.quantity", 50);

        int qty = id.getInt("quantity");

        Assertions.assertEquals(50, qty);
    }

    @Test
    public void test_getLong() {
        EntityId id = EntityId.of("Transaction.amount", 1000000L);

        long amount = id.getLong("amount");

        Assertions.assertEquals(1000000L, amount);
    }

    @Test
    public void test_get_withTargetType() {
        EntityId id = EntityId.of("User.id", 12345);

        Integer value = id.get("id", Integer.class);

        Assertions.assertNotNull(value);
        Assertions.assertEquals(12345, value);
    }

    @Test
    public void test_containsKey() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");

        Assertions.assertTrue(id.containsKey("id"));
        Assertions.assertTrue(id.containsKey("name"));
        Assertions.assertFalse(id.containsKey("email"));
        Assertions.assertFalse(id.containsKey("nonExistent"));
    }

    @Test
    public void test_keySet() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");

        Set<String> keys = id.keySet();

        Assertions.assertNotNull(keys);
        Assertions.assertEquals(2, keys.size());
        Assertions.assertTrue(keys.contains("id"));
        Assertions.assertTrue(keys.contains("name"));
    }

    @Test
    public void test_entrySet() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");

        Set<Map.Entry<String, Object>> entries = id.entrySet();

        Assertions.assertNotNull(entries);
        Assertions.assertEquals(2, entries.size());
    }

    @Test
    public void test_size() {
        EntityId id1 = EntityId.of("User.id", 100);
        Assertions.assertEquals(1, id1.size());

        EntityId id2 = EntityId.of("User.id", 100, "User.name", "John");
        Assertions.assertEquals(2, id2.size());

        EntityId id3 = EntityId.of("Stock.warehouseId", 10, "Stock.productId", "PROD-123", "Stock.batchId", "BATCH-2023");
        Assertions.assertEquals(3, id3.size());
    }

    @Test
    public void test_isEmpty() {
        EntityId emptyId = EntityId.builder().build();
        Assertions.assertTrue(emptyId.isEmpty());

        EntityId nonEmptyId = EntityId.of("User.id", 100);
        Assertions.assertFalse(nonEmptyId.isEmpty());
    }

    @Test
    public void test_builder_basic() {
        EntityId id = EntityId.builder()
            .put("userId", 100)
            .put("accountId", 200)
            .build();

        Assertions.assertNotNull(id);
        Assertions.assertEquals(100, (Integer) id.get("userId"));
        Assertions.assertEquals(200, (Integer) id.get("accountId"));
        Assertions.assertEquals(2, id.size());
    }

    @Test
    public void test_builder_withEntityName() {
        EntityId id = EntityId.builder("Customer")
            .put("customerId", 1000)
            .put("regionCode", "US")
            .build();

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Customer", id.entityName());
        Assertions.assertEquals(1000, (Integer) id.get("customerId"));
        Assertions.assertEquals("US", id.get("regionCode"));
        Assertions.assertEquals(2, id.size());
    }

    @Test
    public void test_builder_empty() {
        EntityId id = EntityId.builder().build();

        Assertions.assertNotNull(id);
        Assertions.assertTrue(id.isEmpty());
        Assertions.assertEquals(0, id.size());
    }

    @Test
    public void test_builder_multipleProperties() {
        EntityId id = EntityId.builder("Order")
            .put("orderId", 5000)
            .put("customerId", 100)
            .put("orderDate", "2025-01-01")
            .put("status", "PENDING")
            .build();

        Assertions.assertNotNull(id);
        Assertions.assertEquals("Order", id.entityName());
        Assertions.assertEquals(5000, (Integer) id.get("orderId"));
        Assertions.assertEquals(100, (Integer) id.get("customerId"));
        Assertions.assertEquals("2025-01-01", id.get("orderDate"));
        Assertions.assertEquals("PENDING", id.get("status"));
        Assertions.assertEquals(4, id.size());
    }

    @Test
    public void test_nullPropertyValue() {
        EntityId id = EntityId.of("User.id", null);

        Assertions.assertNotNull(id);
        Assertions.assertEquals(1, id.size());
        Assertions.assertNull(id.get("id"));
        Assertions.assertTrue(id.containsKey("id"));
    }

    @Test
    public void test_differentPropertyTypes() {
        EntityId id = EntityId.builder("Test")
            .put("intVal", 123)
            .put("longVal", 123456789L)
            .put("stringVal", "test")
            .put("boolVal", true)
            .build();

        Assertions.assertEquals(123, (Integer) id.get("intVal"));
        Assertions.assertEquals(123456789L, (Long) id.get("longVal"));
        Assertions.assertEquals("test", id.get("stringVal"));
        Assertions.assertEquals(true, id.get("boolVal"));
    }

    @Test
    public void test_entityName_extraction() {
        EntityId id1 = EntityId.of("User.id", 100);
        Assertions.assertEquals("User", id1.entityName());

        EntityId id2 = EntityId.of("com.example.Customer.customerId", 200);
        Assertions.assertEquals("com.example.Customer", id2.entityName());
    }

    @Test
    public void test_get_nonExistentProperty() {
        EntityId id = EntityId.of("User.id", 100);

        Object value = id.get("nonExistent");

        Assertions.assertNull(value);
    }

    @Test
    public void test_getInt_nonExistentProperty() {
        EntityId id = EntityId.of("User.name", "John");

        int value = id.getInt("nonExistent");

        Assertions.assertEquals(0, value);
    }

    @Test
    public void test_getLong_nonExistentProperty() {
        EntityId id = EntityId.of("User.name", "John");

        long value = id.getLong("nonExistent");

        Assertions.assertEquals(0L, value);
    }
}
