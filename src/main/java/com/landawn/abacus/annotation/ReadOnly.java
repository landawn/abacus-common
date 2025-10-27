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
 * Marks a field as read-only for database persistence operations.
 * Fields annotated with @ReadOnly will be excluded from INSERT and UPDATE operations
 * when working with entity objects, ensuring they cannot be modified through standard
 * persistence methods.
 * 
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Database-generated values (timestamps, computed columns)</li>
 *   <li>Audit fields populated by triggers or database functions</li>
 *   <li>Derived or calculated fields that should only be read</li>
 *   <li>System-managed metadata that shouldn't be directly modified</li>
 *   <li>Fields populated through database views or joins</li>
 * </ul>
 * 
 * <p><b>Behavior with persistence operations:</b></p>
 * <ul>
 *   <li>INSERT: Field is excluded from insert statements</li>
 *   <li>UPDATE: Field is excluded from update statements</li>
 *   <li>SELECT: Field is included normally (can be read)</li>
 *   <li>The field can still be set programmatically in the Java object</li>
 * </ul>
 * 
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>Different from @Transient - read-only fields are still mapped to columns</li>
 *   <li>The annotation only affects persistence operations, not serialization</li>
 *   <li>Database-level constraints (like triggers) can still modify these fields</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Entity
 * public class Article {
 *     @Id
 *     private Long id;
 *     
 *     @Column
 *     private String title;
 *     
 *     @Column
 *     private String content;
 *     
 *     @ReadOnly
 *     @Column(name = "created_time")
 *     private Timestamp createdTime;  // Set by database DEFAULT or trigger
 *     
 *     @ReadOnly
 *     @Column(name = "last_modified")
 *     private Timestamp lastModified;  // Set by database trigger
 *     
 *     @ReadOnly
 *     @Column(name = "view_count")
 *     private Integer viewCount;       // Updated by stored procedure
 * }
 * }</pre>
 * 
 * @since 2018
 * @see Transient
 * @see NonUpdatable
 * @see Column
 */
@Documented
@Target(value = { FIELD /* METHOD, */ })
@Retention(RUNTIME)
public @interface ReadOnly {

}
