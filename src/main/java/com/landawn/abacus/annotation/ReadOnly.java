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
 * Fields annotated with {@code @ReadOnly} will be excluded from {@code INSERT} and {@code UPDATE} operations
 * when working with entity objects, ensuring they cannot be modified through standard
 * persistence methods.
 *
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Database-generated values (timestamps, computed columns).</li>
 *   <li>Audit fields populated by triggers or database functions.</li>
 *   <li>Derived or calculated fields that should only be read.</li>
 *   <li>System-managed metadata that shouldn't be directly modified.</li>
 *   <li>Fields populated through database views or joins.</li>
 * </ul>
 *
 * <p><b>Behavior with persistence operations:</b></p>
 * <ul>
 *   <li>{@code INSERT}: Field is excluded from insert statements.</li>
 *   <li>{@code UPDATE}: Field is excluded from update statements.</li>
 *   <li>{@code SELECT}: Field is included normally (can be read).</li>
 *   <li>The field can still be set programmatically in the Java object.</li>
 * </ul>
 *
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>Different from {@link Transient} — read-only fields are still mapped to columns and are
 *       still loaded by {@code SELECT}; only writes are suppressed.</li>
 *   <li>The annotation only affects persistence operations, not serialization.</li>
 *   <li>Database-level constraints (such as triggers) can still modify these fields server-side.</li>
 *   <li>{@code @Id} combined with {@code @ReadOnly} is treated by the framework's reflection layer
 *       ({@code com.landawn.abacus.parser.ParserUtil}) as equivalent to {@link ReadOnlyId} — the
 *       property becomes part of the read-only ID set ({@code isMarkedAsReadOnlyId == true}).</li>
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
 *     private Timestamp createdTime;     // createdTime is set by database DEFAULT or trigger.
 *
 *     @ReadOnly
 *     @Column(name = "last_modified")
 *     private Timestamp lastModified;    // lastModified is set by database trigger.
 *
 *     @ReadOnly
 *     @Column(name = "view_count")
 *     private Integer viewCount;         // viewCount is updated by stored procedure.
 * }
 *
 * // Equivalent to @ReadOnlyId for the id property:
 * @Entity
 * public class AuditEntry {
 *     @Id @ReadOnly
 *     private Long id;     // id is a read-only primary key (DB-generated).
 *     ...
 * }
 * }</pre>
 *
 * @see Transient
 * @see NonUpdatable
 * @see Column
 * @see ReadOnlyId
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface ReadOnly {

}
