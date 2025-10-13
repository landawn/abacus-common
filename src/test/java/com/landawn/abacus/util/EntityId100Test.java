package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

import lombok.Data;

@Tag("new-test")
public class EntityId100Test extends TestBase {

    @Test
    public void testOfSingleProperty() {
        EntityId id = EntityId.of("User.id", 12345);
        assertNotNull(id);
        assertEquals(12345, (Integer) id.get("id"));
    }

    @Test
    public void testOfSinglePropertyWithEntity() {
        EntityId id = EntityId.of("Account", "accountNumber", "AC-12345");
        assertNotNull(id);
        assertEquals("Account", id.entityName());
        assertEquals("AC-12345", id.get("accountNumber"));
    }

    @Test
    public void testOfTwoProperties() {
        EntityId id = EntityId.of("Order.customerId", 100, "Order.orderId", 5000);
        assertNotNull(id);
        assertEquals(100, (Integer) id.get("customerId"));
        assertEquals(5000, (Integer) id.get("orderId"));
    }

    @Test
    public void testOfTwoPropertiesWithEntity() {
        EntityId id = EntityId.of("OrderLine", "orderId", 1000, "lineNumber", 1);
        assertNotNull(id);
        assertEquals("OrderLine", id.entityName());
        assertEquals(1000, (Integer) id.get("orderId"));
        assertEquals(1, (Integer) id.get("lineNumber"));
    }

    @Test
    public void testOfThreeProperties() {
        EntityId id = EntityId.of("Stock.warehouseId", 10, "Stock.productId", "PROD-123", "Stock.batchId", "BATCH-2023");
        assertNotNull(id);
        assertEquals(10, (Integer) id.get("warehouseId"));
        assertEquals("PROD-123", id.get("productId"));
        assertEquals("BATCH-2023", id.get("batchId"));
    }

    @Test
    public void testOfThreePropertiesWithEntity() {
        EntityId id = EntityId.of("Inventory", "warehouseId", 5, "productId", "SKU-789", "locationId", "A-10-3");
        assertNotNull(id);
        assertEquals("Inventory", id.entityName());
        assertEquals(5, (Integer) id.get("warehouseId"));
        assertEquals("SKU-789", id.get("productId"));
        assertEquals("A-10-3", id.get("locationId"));
    }

    @Test
    public void testCreateFromMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("customerId", 1000);
        props.put("orderDate", "2023-01-01");

        EntityId id = EntityId.create(props);
        assertNotNull(id);
        assertEquals(1000, (Integer) id.get("customerId"));
        assertEquals("2023-01-01", id.get("orderDate"));
    }

    @Test
    public void testCreateFromMapWithEntity() {
        Map<String, Object> props = new HashMap<>();
        props.put("firstName", "John");
        props.put("lastName", "Doe");
        props.put("email", "john.doe@example.com");

        EntityId id = EntityId.create("Person", props);
        assertNotNull(id);
        assertEquals("Person", id.entityName());
        assertEquals("John", id.get("firstName"));
        assertEquals("Doe", id.get("lastName"));
        assertEquals("john.doe@example.com", id.get("email"));
    }

    @Data
    public class TestBean {
        private String name;
        private int id;
    }

    @Test
    public void testCreateFromEntity() {
        TestBean entity = new TestBean();
        entity.setName("TestEntity");
        entity.setId(123);

        EntityId id = EntityId.create(entity);
        assertNotNull(id);
    }

    @Test
    public void testCreateFromEntityWithIdPropNames() {

        TestBean entity = new TestBean();
        entity.setName("TestEntity");
        entity.setId(123);
        Collection<String> idPropNames = Arrays.asList("name", "id");
        EntityId id = EntityId.create(entity, idPropNames);
        assertNotNull(id);
    }

    @Test
    public void testEntityName() {
        EntityId id = EntityId.of("User", "userId", 100);
        assertEquals("User", id.entityName());
    }

    @Test
    public void testGet() {
        EntityId id = EntityId.of("User.id", 12345);
        Integer userId = id.get("id");
        assertEquals(12345, userId);
    }

    @Test
    public void testGetInt() {
        EntityId id = EntityId.of("Product.quantity", 50);
        int qty = id.getInt("quantity");
        assertEquals(50, qty);
    }

    @Test
    public void testGetLong() {
        EntityId id = EntityId.of("Transaction.amount", 1000000L);
        long amount = id.getLong("amount");
        assertEquals(1000000L, amount);
    }

    @Test
    public void testGetWithTargetType() {
        EntityId id = EntityId.of("User.birthDate", "1990-01-01");
        String date = id.get("birthDate", String.class);
        assertEquals("1990-01-01", date);
    }

    @Test
    public void testContainsKey() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");
        assertTrue(id.containsKey("id"));
        assertTrue(id.containsKey("name"));
        assertFalse(id.containsKey("email"));
    }

    @Test
    public void testKeySet() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");
        Set<String> keys = id.keySet();
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("id"));
        assertTrue(keys.contains("name"));
    }

    @Test
    public void testEntrySet() {
        EntityId id = EntityId.of("User.id", 100, "User.name", "John");
        Set<Map.Entry<String, Object>> entries = id.entrySet();
        assertNotNull(entries);
        assertEquals(2, entries.size());
    }

    @Test
    public void testSize() {
        EntityId id1 = EntityId.of("User.id", 100);
        assertEquals(1, id1.size());

        EntityId id2 = EntityId.of("User.id", 100, "User.name", "John");
        assertEquals(2, id2.size());
    }

    @Test
    public void testIsEmpty() {
        EntityId id1 = EntityId.builder().build();
        assertTrue(id1.isEmpty());

        EntityId id2 = EntityId.of("User.id", 100);
        assertFalse(id2.isEmpty());
    }

    @Test
    public void testBuilder() {
        EntityId id = EntityId.builder().put("userId", 100).put("accountId", 200).build();

        assertNotNull(id);
        assertEquals(100, (Integer) id.get("userId"));
        assertEquals(200, (Integer) id.get("accountId"));
    }

    @Test
    public void testBuilderWithEntityName() {
        EntityId id = EntityId.builder("Customer").put("customerId", 1000).put("regionCode", "US").build();

        assertNotNull(id);
        assertEquals("Customer", id.entityName());
        assertEquals(1000, (Integer) id.get("customerId"));
        assertEquals("US", id.get("regionCode"));
    }

    @Test
    public void testBuilderEmpty() {
        EntityId id = EntityId.builder().build();
        assertNotNull(id);
        assertTrue(id.isEmpty());
    }

    @Test
    public void testEntityIdBuilderPut() {
        EntityId.EntityIdBuilder builder = EntityId.builder("Product");
        builder.put("productId", "ABC123");
        builder.put("warehouseId", 5);
        EntityId id = builder.build();

        assertEquals("Product", id.entityName());
        assertEquals("ABC123", id.get("productId"));
        assertEquals(5, (Integer) id.get("warehouseId"));
    }

    @Test
    public void testEntityIdBuilderBuild() {
        EntityId.EntityIdBuilder builder = EntityId.builder();
        builder.put("id", 100);
        EntityId id = builder.build();

        assertNotNull(id);
        assertEquals(100, (Integer) id.get("id"));
    }
}
