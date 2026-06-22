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
 * <p>When applied to a type, or when the value array contains multiple property names,
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
 * @Id({"companyId", "employeeId"})
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
@Target(value = { FIELD, TYPE })
@Retention(RUNTIME)
public @interface Id {

    /**
     * Specifies the property names that form the primary key when {@code @Id} is applied to a type
     * (class) to declare a composite or explicit primary key.
     *
     * <p>Usage patterns:</p>
     * <ul>
     *   <li>For type-level usage: list one or more property names that form the primary key
     *       (the array must be non-empty at the type level).</li>
     *   <li>For field-level usage: this element is typically left at its default (empty array);
     *       the annotated field itself identifies the primary key, and any column-name mapping
     *       should be supplied via {@link Column}.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Field-level: the annotated field is the primary key.
     * @Id
     * private Long id;
     *
     * // Type-level composite primary key.
     * @Id({"deptId", "empId"})
     * public class Employee { }
     * }</pre>
     *
     * @return the property names forming the primary key at the type level; an empty array (the
     *         default) is the conventional value for field-level usage
     */
    String[] value() default {};
}
