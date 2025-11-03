/*
 * Copyright (C) 2018 HaiYang Li
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

package com.landawn.abacus.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated class is mapped to a database table.
 * This annotation provides comprehensive configuration for entity-to-table mapping,
 * including table naming, aliases, and fine-grained control over field-to-column mappings.
 * 
 * <p><b>Key features:</b></p>
 * <ul>
 *   <li>Explicit table name mapping</li>
 *   <li>Table aliases for query optimization</li>
 *   <li>Selective field inclusion/exclusion for column mapping</li>
 *   <li>Support for legacy database schemas</li>
 * </ul>
 * 
 * <p><b>Field mapping behavior:</b></p>
 * <ul>
 *   <li>If both columnFields and nonColumnFields are empty: all fields are mapped</li>
 *   <li>If columnFields is specified: only listed fields are mapped</li>
 *   <li>If nonColumnFields is specified: all fields except listed ones are mapped</li>
 *   <li>columnFields takes precedence over nonColumnFields if both are specified</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * import java.math.BigDecimal;
 * import java.util.Date;
 * import java.util.Map;
 *
 * @Table(name = "user_accounts", alias = "ua")
 * public class User {
 *     private Long id;
 *     private String username;
 *     private String password;
 *     private Date lastLogin;
 * }
 *
 * @Table(name = "products", nonColumnFields = {"tempData", "cache"})
 * public class Product {
 *     private Long id;
 *     private String name;
 *     private BigDecimal price;
 *     private transient String tempData;  // Excluded from mapping
 *     private Map cache;                   // Excluded from mapping
 * }
 * }</pre>
 * 
 * @see Entity
 * @see Column
 * @see Id
 */
@Documented
@Target(value = { TYPE })
@Retention(RUNTIME)
public @interface Table {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the table name value (deprecated)
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the database table this class maps to.
     * If not specified (empty string), the simple class name is used as the table name.
     * 
     * <p><b>Naming considerations:</b></p>
     * <ul>
     *   <li>Follow your database's naming conventions (snake_case, PascalCase, etc.)</li>
     *   <li>Consider using prefixes for table grouping (e.g., "app_users", "sys_config")</li>
     *   <li>Be consistent with plural/singular naming across your schema</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "user_accounts")  // Maps to 'user_accounts' table
     * public class User { }
     * }</pre>
     * 
     * @return the table name, or empty string to use the simple class name as default
     */
    String name() default "";

    /**
     * An alias for the table that can be used in SQL queries.
     * This provides a shorter reference name for the table, useful in complex queries
     * with joins, subqueries, or when the table name is long.
     * 
     * <p><b>Common uses:</b></p>
     * <ul>
     *   <li>Simplifying complex JOIN queries</li>
     *   <li>Avoiding naming conflicts in self-joins</li>
     *   <li>Creating more readable SQL statements</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "customer_order_details", alias = "cod")
     * public class CustomerOrderDetail { }
     * // Enables queries like: SELECT cod.* FROM customer_order_details cod WHERE ...
     * }</pre>
     * 
     * @return the table alias, or empty string if no alias is defined
     */
    String alias() default "";

    /**
     * Specifies an explicit whitelist of fields that should be mapped to table columns.
     * When specified, ONLY these fields will be considered for column mapping,
     * regardless of other annotations or conventions.
     * 
     * <p><b>Use cases:</b></p>
     * <ul>
     *   <li>Working with classes that have many transient fields</li>
     *   <li>Creating different table mappings for the same class</li>
     *   <li>Strict control over database schema</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "users", columnFields = {"id", "username", "email"})
     * public class User {
     *     private Long id;
     *     private String username;
     *     private String email;
     *     private String tempToken;        // Not mapped
     *     private List<Role> roles;        // Not mapped
     * }
     * }</pre>
     * 
     * @return an array of field names to include as columns, empty array means no whitelist filtering
     */
    String[] columnFields() default {};

    /**
     * Specifies a blacklist of fields that should NOT be mapped to table columns.
     * These fields will be excluded from column mapping operations,
     * useful for transient data, calculated fields, or framework-specific properties.
     * 
     * <p><b>Common exclusions:</b></p>
     * <ul>
     *   <li>Calculated or derived fields</li>
     *   <li>Temporary state or cache fields</li>
     *   <li>Framework-specific metadata</li>
     *   <li>Fields mapped to other tables (relationships)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "products", nonColumnFields = {"displayPrice", "cache", "dirty"})
     * public class Product {
     *     private Long id;                  // Mapped
     *     private String name;              // Mapped
     *     private BigDecimal price;         // Mapped
     *     private String displayPrice;      // Not mapped - calculated field
     *     private Map<String, Object> cache;// Not mapped - temporary cache
     *     private boolean dirty;            // Not mapped - state tracking
     * }
     * }</pre>
     * 
     * <p><b>Note:</b> If both columnFields and nonColumnFields are specified,
     * columnFields takes precedence (whitelist wins over blacklist).</p>
     * 
     * @return an array of field names to exclude from column mapping, empty array means no blacklist filtering
     */
    String[] nonColumnFields() default {};
}
