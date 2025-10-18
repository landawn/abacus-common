/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Represents a unique identifier for an entity, consisting of property names and their values.
 * This interface provides a flexible way to create and work with composite identifiers for entities.
 * 
 * <p>EntityId supports various creation patterns including single properties, multiple properties,
 * and extraction from existing entities. It's particularly useful for database operations where
 * entities may have composite keys.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Single property ID
 * EntityId id1 = EntityId.of("User.id", 12345);
 * 
 * // Multiple property ID
 * EntityId id2 = EntityId.of("Order.customerId", 100, "Order.orderId", 500);
 * 
 * // From a map
 * Map<String, Object> props = new HashMap<>();
 * props.put("productId", "ABC123");
 * props.put("warehouseId", 5);
 * EntityId id3 = EntityId.create("Product", props);
 * 
 * // Using builder
 * EntityId id4 = EntityId.builder("Customer")
 *     .put("customerId", 1000)
 *     .put("regionCode", "US")
 *     .build();
 * }</pre>
 */
public interface EntityId {

    /**
     * Creates an EntityId with a single property.
     * The property name should include the entity name for clarity (e.g., "User.id").
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId userId = EntityId.of("User.id", 12345);
     * }</pre>
     *
     * @param propName property name with entity name, for example {@code Account.id}
     * @param propValue the property value
     * @return a new EntityId instance
     */
    static EntityId of(final String propName, final Object propValue) {
        return Seid.of(propName, propValue);
    }

    /**
     * Creates an EntityId with a single property for a specific entity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId accountId = EntityId.of("Account", "accountNumber", "AC-12345");
     * }</pre>
     *
     * @param entityName the name of the entity
     * @param propName the property name
     * @param propValue the property value
     * @return a new EntityId instance
     */
    @SuppressWarnings("deprecation")
    static EntityId of(final String entityName, final String propName, final Object propValue) {
        return Seid.of(entityName).set(propName, propValue);
    }

    /**
     * Creates an EntityId with two properties.
     * Property names should include entity names for clarity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId compositeId = EntityId.of("Order.customerId", 100, "Order.orderId", 5000);
     * }</pre>
     *
     * @param propName1 property name with entity name, for example {@code Account.id}
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @return a new EntityId instance
     */
    static EntityId of(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return Seid.of(propName1, propValue1, propName2, propValue2);
    }

    /**
     * Creates an EntityId with two properties for a specific entity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId orderLineId = EntityId.of("OrderLine", "orderId", 1000, "lineNumber", 1);
     * }</pre>
     *
     * @param entityName the name of the entity
     * @param propName1 the first property name
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @return a new EntityId instance
     */
    @SuppressWarnings("deprecation")
    static EntityId of(final String entityName, final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        return Seid.of(entityName).set(propName1, propValue1).set(propName2, propValue2);
    }

    /**
     * Creates an EntityId with three properties.
     * Property names should include entity names for clarity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId tripleId = EntityId.of(
     *     "Stock.warehouseId", 10,
     *     "Stock.productId", "PROD-123",
     *     "Stock.batchId", "BATCH-2023"
     * );
     * }</pre>
     *
     * @param propName1 property name with entity name, for example {@code Account.id}
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @param propName3 the third property name
     * @param propValue3 the third property value
     * @return a new EntityId instance
     */
    static EntityId of(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        return Seid.of(propName1, propValue1, propName2, propValue2, propName3, propValue3);
    }

    /**
     * Creates an EntityId with three properties for a specific entity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId inventoryId = EntityId.of("Inventory",
     *     "warehouseId", 5,
     *     "productId", "SKU-789",
     *     "locationId", "A-10-3"
     * );
     * }</pre>
     *
     * @param entityName the name of the entity
     * @param propName1 the first property name
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @param propName3 the third property name
     * @param propValue3 the third property value
     * @return a new EntityId instance
     */
    @SuppressWarnings("deprecation")
    static EntityId of(final String entityName, final String propName1, final Object propValue1, final String propName2, final Object propValue2,
            final String propName3, final Object propValue3) {
        return Seid.of(entityName).set(propName1, propValue1).set(propName2, propValue2).set(propName3, propValue3);
    }

    /**
     * Creates an EntityId from a map of property names to values.
     * The entity name is inferred from the property names if they contain dots.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("customerId", 1000);
     * props.put("orderDate", LocalDate.now());
     * EntityId id = EntityId.create(props);
     * }</pre>
     *
     * @param nameValues a map of property names to their values
     * @return a new EntityId instance
     * @throws IllegalArgumentException if the map is empty or null
     */
    static EntityId create(final Map<String, Object> nameValues) {
        return Seid.create(nameValues);
    }

    /**
     * Creates an EntityId from a map of property names to values for a specific entity.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("firstName", "John");
     * props.put("lastName", "Doe");
     * props.put("email", "john.doe@example.com");
     * EntityId id = EntityId.create("Person", props);
     * }</pre>
     *
     * @param entityName the name of the entity
     * @param nameValues a map of property names to their values
     * @return a new EntityId instance
     */
    @SuppressWarnings("deprecation")
    static EntityId create(final String entityName, final Map<String, Object> nameValues) {
        final Seid seid = Seid.of(entityName);
        seid.set(nameValues);
        return seid;
    }

    /**
     * Creates an EntityId by extracting ID properties from an entity object.
     * The implementation will use reflection to find properties marked as IDs.
     *
     * <p>Example:</p>
     * <pre>{@code
     * User user = new User();
     * user.setUserId(12345);
     * user.setName("John");
     * EntityId id = EntityId.create(user); // Extracts userId if marked as @Id
     * }</pre>
     *
     * @param entity the entity object to extract ID from
     * @return a new EntityId instance
     */
    static EntityId create(final Object entity) {
        return Seid.create(entity);
    }

    /**
     * Creates an EntityId by extracting specific properties from an entity object.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Product product = new Product();
     * product.setProductCode("ABC");
     * product.setWarehouseId(5);
     * product.setDescription("Example product");
     * 
     * EntityId id = EntityId.create(product, Arrays.asList("productCode", "warehouseId"));
     * // Creates ID with only productCode and warehouseId
     * }</pre>
     *
     * @param entity the entity object to extract properties from
     * @param idPropNames the collection of property names to use as ID
     * @return a new EntityId instance
     */
    static EntityId create(final Object entity, final Collection<String> idPropNames) {
        return Seid.create(entity, idPropNames);
    }

    /**
     * Returns the entity name associated with this EntityId.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User", "userId", 100);
     * String name = id.entityName(); // Returns "User"
     * }</pre>
     *
     * @return the entity name as a String
     */
    String entityName();

    /**
     * Gets the value of a property by name.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.id", 12345);
     * Integer userId = id.get("id"); // Returns 12345
     * }</pre>
     *
     * @param <T> the expected type of the property value
     * @param propName the property name
     * @return the property value, or null if not found
     */
    <T> T get(String propName);

    /**
     * Gets the value of a property as an int.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("Product.quantity", 50);
     * int qty = id.getInt("quantity"); // Returns 50
     * }</pre>
     *
     * @param propName the property name
     * @return the property value as int, or 0 if not found or not a number
     */
    int getInt(String propName);

    /**
     * Gets the value of a property as a long.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("Transaction.amount", 1000000L);
     * long amount = id.getLong("amount"); // Returns 1000000
     * }</pre>
     *
     * @param propName the property name
     * @return the property value as long, or 0 if not found or not a number
     */
    long getLong(String propName);

    /**
     * Gets the value of a property and converts it to the specified type.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.birthDate", "1990-01-01");
     * LocalDate date = id.get("birthDate", LocalDate.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param propName the property name
     * @param targetType the class to convert the value to
     * @return the property value converted to the target type, or null if not found
     */
    <T> T get(String propName, Class<? extends T> targetType);

    /**
     * Checks if this EntityId contains a property with the given name.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.id", 100, "User.name", "John");
     * boolean hasId = id.containsKey("id"); // Returns true
     * boolean hasEmail = id.containsKey("email"); // Returns false
     * }</pre>
     *
     * @param propName the property name to check
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    boolean containsKey(String propName);

    /**
     * Returns a set of all property names in this EntityId.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.id", 100, "User.name", "John");
     * Set<String> keys = id.keySet(); // Returns {"id", "name"}
     * }</pre>
     *
     * @return a Set containing all property names
     */
    Set<String> keySet();

    /**
     * Returns a set of all property name-value entries in this EntityId.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.id", 100, "User.name", "John");
     * for (Map.Entry<String, Object> entry : id.entrySet()) {
     *     System.out.println(entry.getKey() + " = " + entry.getValue());
     * }
     * }</pre>
     *
     * @return a Set of Map.Entry objects containing property names and values
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Returns the number of properties in this EntityId.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.of("User.id", 100, "User.name", "John");
     * int count = id.size(); // Returns 2
     * }</pre>
     *
     * @return the number of properties
     */
    int size();

    /**
     * Checks if this EntityId is empty (contains no properties).
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id1 = EntityId.builder().build();
     * boolean empty1 = id1.isEmpty(); // Returns true
     * 
     * EntityId id2 = EntityId.of("User.id", 100);
     * boolean empty2 = id2.isEmpty(); // Returns false
     * }</pre>
     *
     * @return {@code true} if this EntityId contains no properties, {@code false} otherwise
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Creates a new EntityIdBuilder for building EntityId instances.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.builder()
     *     .put("userId", 100)
     *     .put("accountId", 200)
     *     .build();
     * }</pre>
     *
     * @return a new EntityIdBuilder instance
     */
    static EntityIdBuilder builder() {
        return new EntityIdBuilder();
    }

    /**
     * Creates a new EntityIdBuilder for building EntityId instances with a specific entity name.
     *
     * <p>Example:</p>
     * <pre>{@code
     * EntityId id = EntityId.builder("Customer")
     *     .put("customerId", 1000)
     *     .put("regionCode", "US")
     *     .build();
     * }</pre>
     *
     * @param entityName the name of the entity
     * @return a new EntityIdBuilder instance
     */
    static EntityIdBuilder builder(final String entityName) {
        return new EntityIdBuilder(entityName);
    }

    /**
     * Builder class for constructing EntityId instances fluently.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * EntityId complexId = EntityId.builder("Order")
     *     .put("orderId", 5000)
     *     .put("customerId", 100)
     *     .put("orderDate", LocalDate.now())
     *     .build();
     * }</pre>
     */
    class EntityIdBuilder {
        private Seid entityId = null;

        EntityIdBuilder() {
        }

        @SuppressWarnings("deprecation")
        EntityIdBuilder(final String entityName) {
            entityId = new Seid(entityName);
        }

        /**
         * Adds a property to the EntityId being built.
         *
         * <p>Example:</p>
         * <pre>{@code
         * EntityIdBuilder builder = EntityId.builder("Product")
         *     .put("productId", "ABC123")
         *     .put("warehouseId", 5);
         * }</pre>
         *
         * @param idPropName the property name
         * @param idPropVal the property value
         * @return this builder for method chaining
         */
        @SuppressWarnings("deprecation")
        public EntityIdBuilder put(final String idPropName, final Object idPropVal) {
            if (entityId == null) {
                entityId = new Seid(idPropName, idPropVal);
            } else {
                entityId.set(idPropName, idPropVal);
            }

            return this;
        }

        /**
         * Builds and returns the EntityId instance.
         *
         * <p>Example:</p>
         * <pre>{@code
         * EntityId id = EntityId.builder()
         *     .put("id", 100)
         *     .build();
         * }</pre>
         *
         * @return the constructed EntityId
         */
        @SuppressWarnings("deprecation")
        public EntityId build() {
            if (entityId == null) {
                entityId = new Seid(Strings.EMPTY);
            }

            return entityId;
        }
    }
}