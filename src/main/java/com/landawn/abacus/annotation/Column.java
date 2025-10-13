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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that a field represents a database column in an entity class.
 * This annotation is used in ORM (Object-Relational Mapping) contexts to explicitly define
 * the mapping between Java entity fields and database table columns.
 * 
 * <p>By default, if this annotation is not present or if the name attribute is not specified,
 * the field name is used as the column name. Use the {@link #name()} attribute to specify
 * a different column name when the database column name differs from the field name.</p>
 * 
 * <p>This annotation is typically used in conjunction with {@link Entity} annotation on the class level
 * to create complete ORM mappings.</p>
 * 
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Mapping fields to columns with different naming conventions (e.g., camelCase to snake_case)</li>
 *   <li>Handling reserved keywords as column names</li>
 *   <li>Working with legacy database schemas</li>
 *   <li>Explicitly documenting field-to-column mappings</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@literal @}Entity
 * public class User {
 *     {@literal @}Column(name = "user_id")
 *     private Long id;
 *     
 *     {@literal @}Column(name = "user_name")
 *     private String userName;
 *     
 *     {@literal @}Column  // Uses field name "email" as column name
 *     private String email;
 * }
 * </pre>
 * 
 * @since 2018
 * @see Entity
 * @see Table
 * @see Id
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface Column {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the column name value (deprecated)
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the database column this field maps to.
     * If not specified (empty string), the field name is used as the column name.
     * 
     * <p>Column names should follow the naming conventions of your database system.
     * Common conventions include:</p>
     * <ul>
     *   <li>snake_case for PostgreSQL and MySQL</li>
     *   <li>UPPER_CASE for Oracle</li>
     *   <li>PascalCase for SQL Server</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * {@literal @}Column(name = "created_date")
     * private LocalDateTime createdDate;
     * </pre>
     * 
     * @return the column name, or empty string to use the field name as default
     */
    String name() default "";
}
