/*
 * Copyright (C) 2019 HaiYang Li
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
 * Declares a join relationship between entities for ORM mapping and lazy / eager loading of
 * related data. This marker is read by the {@code abacus-jdbc} module's DAO/query layer when it
 * fetches related rows for a property that itself is not a column on the owning table; the
 * {@code abacus-core} library only ships the annotation as a runtime-retained marker and does
 * not introspect it directly.
 *
 * <p>Supported cardinalities are inferred from the field type:</p>
 * <ul>
 *   <li>A scalar bean field — one-to-one.</li>
 *   <li>A {@link java.util.Collection} (typically {@code List<T>}) — one-to-many.</li>
 *   <li>A {@link java.util.Collection} with a join-table condition (two pairs) — many-to-many.</li>
 * </ul>
 *
 * <p><b>Join condition format:</b></p>
 * <p>Each entry in {@link #value()} is a string of the form {@code "sourceField=targetField"} where:</p>
 * <ul>
 *   <li>{@code sourceField} — property name on the current (owning) entity, or
 *       {@code "JoinTable.column"} when traversing a join table.</li>
 *   <li>{@code targetField} — property name on the related entity, or {@code "JoinTable.column"}.</li>
 *   <li>For many-to-many: supply two conditions — first owning-entity → join-table, then
 *       join-table → related-entity.</li>
 * </ul>
 *
 * <p><b>Relationship types supported:</b></p>
 * <ul>
 *   <li>One-to-One: Single related entity</li>
 *   <li>One-to-Many: Collection of related entities</li>
 *   <li>Many-to-Many: Collection through intermediate join table</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p><b>One-to-One Join Example:</b></p>
 * <pre>{@code
 * @Entity
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     @JoinedBy("id=userId")
 *     private UserProfile profile;  // Single related profile
 * }
 *
 * @Entity
 * public class UserProfile {
 *     @Id
 *     private Long id;
 *     private Long userId;  // Foreign key to User
 *     private String bio;
 * }
 * }</pre>
 *
 * <p><b>One-to-Many Join Example:</b></p>
 * <pre>{@code
 *
 * @Entity
 * public class Account {
 *     @Id
 *     private Long id;
 *
 *     @JoinedBy("id=accountId")
 *     private List<Device> devices;  // Multiple related devices
 * }
 *
 * @Entity
 * public class Device {
 *     @Id
 *     private Long id;
 *     private Long accountId;  // Foreign key to Account
 *     private String deviceName;
 * }
 * }</pre>
 *
 * <p><b>Many-to-Many Join Example:</b></p>
 * <pre>{@code
 *
 * @Entity
 * public class Employee {
 *     @Id
 *     private Long id;
 *
 *     // Join through intermediate table EmployeeProject
 *     @JoinedBy({ "id=EmployeeProject.employeeId", "EmployeeProject.projectId=id" })
 *     private List<Project> projects;
 * }
 *
 * @Entity
 * public class Project {
 *     @Id
 *     private Long id;
 *     private String name;
 * }
 *
 * @Entity
 * public class EmployeeProject {
 *     private Long employeeId;  // FK to Employee
 *     private Long projectId;  // FK to Project
 *     private Date assignedDate;
 * }
 * }</pre>
 *
 * @see Entity
 * @see Column
 * @see Id
 */
@Documented
@Target(value = { FIELD /*, METHOD */ })
@Retention(RUNTIME)
public @interface JoinedBy {

    /**
     * Specifies the join conditions that define how entities are related.
     * Each condition is a string in the format {@code "sourceField=targetField"}.
     *
     * <p><b>For simple joins (one-to-one, one-to-many):</b></p>
     * <ul>
     *   <li>Use a single condition like {@code "id=parentId"}</li>
     *   <li>Source field is from the current entity</li>
     *   <li>Target field is from the related entity</li>
     * </ul>
     *
     * <p><b>For many-to-many joins:</b></p>
     * <ul>
     *   <li>Use multiple conditions to traverse the join table</li>
     *   <li>First condition: current entity to join table</li>
     *   <li>Second condition: join table to target entity</li>
     *   <li>Use {@code "TableName.fieldName"} for join table references</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple one-to-many
     * @JoinedBy("id=customerId")
     * private List<Order> orders;
     *
     * // Many-to-many with join table
     * @JoinedBy({"id=UserRole.userId", "UserRole.roleId=id"})
     * private List<Role> roles;
     * }</pre>
     *
     * @return an array of join condition strings defining the relationship
     */
    String[] value() default {};

    // OnDeleteAction onDelete() default OnDeleteAction.NO_ACTION; // TODO it's very complicated to support it.
}
