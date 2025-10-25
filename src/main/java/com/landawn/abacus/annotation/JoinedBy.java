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
 * Defines join relationships between entities for ORM mapping and data loading.
 * This annotation specifies how entities are connected through foreign key relationships,
 * enabling automatic loading of related data in one-to-one, one-to-many, and many-to-many associations.
 * 
 * <p><b>Join condition format:</b></p>
 * <p>Each join condition is specified as "sourceField=targetField" where:</p>
 * <ul>
 *   <li>sourceField: Field name from the current entity</li>
 *   <li>targetField: Field name from the related entity or join table</li>
 *   <li>For join tables: use "TableName.fieldName" notation</li>
 * </ul>
 * 
 * <p><b>Relationship types supported:</b></p>
 * <ul>
 *   <li>One-to-One: Single related entity</li>
 *   <li>One-to-Many: Collection of related entities</li>
 *   <li>Many-to-Many: Collection through intermediate join table</li>
 * </ul>
 * 
 * <h3>One-to-One Join Example:</h3>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * {@literal @}Entity
 * public class User {
 *     {@literal @}Id
 *     private Long id;
 *     
 *     {@literal @}JoinedBy("id=userId")
 *     private UserProfile profile;  // Single related profile
 * }
 * 
 * {@literal @}Entity
 * public class UserProfile {
 *     {@literal @}Id
 *     private Long id;
 *     private Long userId;  // Foreign key to User
 *     private String bio;
 * }
 * }</pre>
 *
 * <h3>One-to-Many Join Example:</h3>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * {@literal @}Entity
 * public class Account {
 *     {@literal @}Id
 *     private Long id;
 *
 *     {@literal @}JoinedBy("id=accountId")
 *     private List&lt;Device&gt; devices;  // Multiple related devices
 * }
 *
 * {@literal @}Entity
 * public class Device {
 *     {@literal @}Id
 *     private Long id;
 *     private Long accountId;  // Foreign key to Account
 *     private String deviceName;
 * }
 * }</pre>
 *
 * <h3>Many-to-Many Join Example:</h3>
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * {@literal @}Entity
 * public class Employee {
 *     {@literal @}Id
 *     private Long id;
 *
 *     // Join through intermediate table EmployeeProject
 *     {@literal @}JoinedBy({ "id=EmployeeProject.employeeId", "EmployeeProject.projectId=id" })
 *     private List&lt;Project&gt; projects;
 * }
 *
 * {@literal @}Entity
 * public class Project {
 *     {@literal @}Id
 *     private Long id;
 *     private String name;
 * }
 *
 * {@literal @}Entity
 * public class EmployeeProject {
 *     private Long employeeId;  // FK to Employee
 *     private Long projectId;   // FK to Project
 *     private Date assignedDate;
 * }
 * }</pre>
 * 
 * @since 2019
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
     * Each condition is a string in the format "sourceField=targetField".
     * 
     * <p><b>For simple joins (one-to-one, one-to-many):</b></p>
     * <ul>
     *   <li>Use a single condition like "id=parentId"</li>
     *   <li>Source field is from the current entity</li>
     *   <li>Target field is from the related entity</li>
     * </ul>
     * 
     * <p><b>For many-to-many joins:</b></p>
     * <ul>
     *   <li>Use multiple conditions to traverse the join table</li>
     *   <li>First condition: current entity to join table</li>
     *   <li>Second condition: join table to target entity</li>
     *   <li>Use "TableName.fieldName" for join table references</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple one-to-many
     * {@literal @}JoinedBy("id=customerId")
     * private List&lt;Order&gt; orders;
     * 
     * // Many-to-many with join table
     * {@literal @}JoinedBy({"id=UserRole.userId", "UserRole.roleId=id"})
     * private List&lt;Role&gt; roles;
     * }</pre>
     * 
     * @return array of join condition strings defining the relationship
     */
    String[] value() default {};

    // OnDeleteAction onDelete() default OnDeleteAction.NO_ACTION; // TODO it's very complicated to support it.
}
