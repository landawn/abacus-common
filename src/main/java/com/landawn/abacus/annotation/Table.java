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
 * This annotation provides metadata for entity-to-table mapping,
 * including table naming, aliases, and optional field-inclusion hints for consumers that support them.
 *
 * <p><b>Key features:</b></p>
 * <ul>
 *   <li>Explicit table name mapping</li>
 *   <li>Table aliases for query optimization</li>
 *   <li>Selective field inclusion/exclusion metadata for mapping consumers</li>
 *   <li>Support for legacy database schemas</li>
 * </ul>
 *
 * <p><b>Field mapping metadata:</b></p>
 * <ul>
 *   <li>If both {@code columnFields} and {@code nonColumnFields} are empty: no field filter metadata is supplied.</li>
 *   <li>If {@code columnFields} is specified: it supplies a whitelist for mapping consumers.</li>
 *   <li>If {@code nonColumnFields} is specified: it supplies a blacklist for mapping consumers.</li>
 *   <li>{@code columnFields} takes precedence over {@code nonColumnFields} for consumers that apply both.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
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
 *     private transient String tempData;  // tempData is marked for exclusion by mapping consumers
 *     private Map cache;  // cache is marked for exclusion by mapping consumers
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
     * Deprecated alias for {@link #name()}.
     *
     * @return the table name
     * @deprecated Use {@link #name()} to specify the table name explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the database table this class maps to.
     * If not specified (empty string), the simple class name is used as the table name.
     *
     * <p><b>Naming considerations:</b></p>
     * <ul>
     *   <li>Follow your database's naming conventions (snake_case, UpperCamelCase, etc.).</li>
     *   <li>Consider using prefixes for table grouping (e.g., "app_users", "sys_config").</li>
     *   <li>Be consistent with plural/singular naming across your schema.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "user_accounts")  // maps to 'user_accounts' table
     * public class User { }
     * }</pre>
     *
     * @return the table name, or an empty string to use the simple class name as default
     */
    String name() default "";

    /**
     * An alias for the table that can be used in SQL queries.
     * This provides a shorter reference name for the table, useful in complex queries
     * with joins, subqueries, or when the table name is long.
     *
     * <p><b>Common uses:</b></p>
     * <ul>
     *   <li>Simplifying complex JOIN queries.</li>
     *   <li>Avoiding naming conflicts in self-joins.</li>
     *   <li>Creating more readable SQL statements.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "customer_order_details", alias = "cod")
     * public class CustomerOrderDetail { }
     * // Enables queries like: SELECT cod.* FROM customer_order_details cod WHERE ...
     * }</pre>
     *
     * @return the table alias, or an empty string if no alias is defined
     */
    String alias() default "";

    /**
     * Specifies an explicit whitelist of fields for mapping consumers that support table-field metadata.
     * When applied by a consumer, only these fields should be considered for column mapping.
     *
     * <p><b>Use cases:</b></p>
     * <ul>
     *   <li>Working with classes that have many transient fields.</li>
     *   <li>Creating different table mappings for the same class.</li>
     *   <li>Strict control over database schema.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "users", columnFields = {"id", "username", "email"})
     * public class User {
     *     private Long id;
     *     private String username;
     *     private String email;
     *     private String tempToken;  // tempToken is not included in the whitelist
     *     private List<Role> roles;  // roles is not included in the whitelist
     * }
     * }</pre>
     *
     * @return an array of field names to include as columns for supporting consumers, an empty array means no whitelist metadata
     */
    String[] columnFields() default {};

    /**
     * Specifies a blacklist of fields for mapping consumers that support table-field metadata.
     * These fields should be excluded from column mapping by supporting consumers,
     * useful for transient data, calculated fields, or framework-specific properties.
     *
     * <p><b>Common exclusions:</b></p>
     * <ul>
     *   <li>Calculated or derived fields.</li>
     *   <li>Temporary state or cache fields.</li>
     *   <li>Framework-specific metadata.</li>
     *   <li>Fields mapped to other tables (relationships).</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Table(name = "products", nonColumnFields = {"displayPrice", "cache", "dirty"})
     * public class Product {
     *     private Long id;                    // id is mapped
     *     private String name;                // name is mapped
     *     private BigDecimal price;           // price is mapped
     *     private String displayPrice;        // displayPrice is marked for exclusion - calculated field
     *     private Map<String, Object> cache;  // cache is marked for exclusion - temporary cache
     *     private boolean dirty;              // dirty is marked for exclusion - state tracking
     * }
     * }</pre>
     *
     * <p><b>Note:</b> If both {@code columnFields} and {@code nonColumnFields} are specified,
     * supporting consumers should let {@code columnFields} take precedence (whitelist wins over blacklist).</p>
     *
     * @return an array of field names to exclude from column mapping for supporting consumers, an empty array means no blacklist metadata
     */
    String[] nonColumnFields() default {};
}
