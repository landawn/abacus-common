/*
 * Copyright (C) 2021 HaiYang Li
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a class as a persistent entity that maps to a database table.
 * This annotation is the primary marker for ORM (Object-Relational Mapping) frameworks
 * to identify classes that represent database entities and should be managed by the persistence layer.
 * 
 * <p>An entity class typically represents a table in a relational database, where:
 * <ul>
 *   <li>Each instance of the entity corresponds to a row in the table</li>
 *   <li>Each field (marked with {@link Column}) corresponds to a column in the table</li>
 *   <li>The entity name (if specified) or class name determines the table name</li>
 * </ul>
 *
 * <p>Entity classes should follow these best practices:</p>
 * <ul>
 *   <li>Have a no-argument constructor (can be private)</li>
 *   <li>Define at least one field marked with {@link Id} as the primary key</li>
 *   <li>Override equals() and hashCode() based on the entity's identity</li>
 *   <li>Be serializable if they need to be passed across network boundaries</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Entity(name = "users")
 * public class User {
 *     @Id
 *     private Long id;
 *     
 *     @Column(name = "user_name")
 *     private String username;
 *     
 *     @Column
 *     private String email;
 *     
 *     // Constructors, getters, setters, etc.
 * }
 * }</pre>
 * 
 * <p><strong>Note:</strong> This annotation is marked as {@link Beta}, indicating it may
 * undergo changes in future versions.</p>
 * 
 * @see Table
 * @see Column
 * @see Id
 */
@Beta
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RUNTIME)
public @interface Entity {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the entity name value (deprecated)
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the entity, which typically maps to the database table name.
     * If not specified (empty string), the simple class name is used as the entity name.
     * 
     * <p>The entity name is used in:</p>
     * <ul>
     *   <li>SQL generation for table references</li>
     *   <li>JPQL/HQL queries to reference the entity</li>
     *   <li>Cache keys and other framework internals</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Entity(name = "app_users")  // Maps to 'app_users' table
     * public class User { }
     * 
     * @Entity  // Maps to 'Customer' table (class name)
     * public class Customer { }
     * }</pre>
     * 
     * @return the entity name, or empty string to use the simple class name as default
     */
    String name() default "";
}
