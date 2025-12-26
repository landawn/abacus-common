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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field or type as representing the primary key (identifier) of an entity.
 * This annotation is essential in ORM (Object-Relational Mapping) frameworks to identify
 * which field(s) uniquely identify each entity instance in the database.
 * 
 * <p><b>For single primary keys:</b></p>
 * <p>When applied to a field, it indicates that the field is the primary key of the entity.
 * The field type can be any serializable type, but common types include:</p>
 * <ul>
 *   <li>Numeric types: Long, Integer, BigInteger</li>
 *   <li>String (for natural keys or UUIDs)</li>
 *   <li>UUID</li>
 *   <li>Custom ID types</li>
 * </ul>
 * 
 * <p><b>For composite primary keys:</b></p>
 * <p>When applied to a type, or when the value array contains multiple column names,
 * it defines a composite primary key consisting of multiple fields.</p>
 * 
 * <p><b>Best practices:</b></p>
 * <ul>
 *   <li>Every entity should have exactly one @Id field or composite key definition</li>
 *   <li>ID fields should be immutable after entity creation</li>
 *   <li>Consider using generated IDs for better performance</li>
 *   <li>Natural keys should be truly unique and unchanging</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single primary key
 * @Entity
 * public class User {
 *     @Id
 *     private Long id;
 * }
 * 
 * // Composite primary key
 * @Entity
 * @Id({"company_id", "employee_id"})
 * public class Employee {
 *     private Long companyId;
 *     private Long employeeId;
 * }
 * }</pre>
 * 
 * @see Entity
 * @see Column
 * @see ReadOnlyId
 */
@Documented
@Target(value = { FIELD, /* METHOD, */ TYPE })
@Retention(RUNTIME)
public @interface Id {

    /**
     * Specifies the column names that form the primary key.
     * 
     * <p>Usage patterns:</p>
     * <ul>
     *   <li>Empty array (default): For single primary keys, the annotated field name is used</li>
     *   <li>Single element array: Explicitly names the primary key column</li>
     *   <li>Multiple elements: Defines a composite primary key with multiple columns</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Id  // Uses field name as column name
     * private Long id;
     * 
     * @Id({"user_id"})  // Maps to 'user_id' column
     * private Long id;
     * 
     * @Id({"dept_id", "emp_id"})  // Composite key
     * public class Employee { }
     * }</pre>
     * 
     * @return an array of column names forming the primary key; empty array uses the field name for field-level usage
     */
    String[] value() default {};
}
