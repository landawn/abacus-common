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
 * Specifies a join relationship between entities in ORM mapping.
 * This annotation defines how entities are related through foreign key relationships,
 * supporting one-to-one, one-to-many, and many-to-many associations.
 * 
 * <p>The join conditions are specified as strings in the format "sourceField=targetField"
 * where sourceField is from the current entity and targetField is from the related entity.</p>
 * 
 * <h3>One-to-One or One-to-Many Join Example:</h3>
 * <pre>
 * <code>
 * public class Account {
 *   private long id;
 *
 *   @JoinedBy("id=accountId")
 *   private List&lt;Device&gt; devices;
 *
 *   //...
 * }
 *
 * public class Device {
 *   private long accountId;
 *   // ...
 * }
 * </code>
 * </pre>
 *
 * <h3>Many-to-Many Join Example:</h3>
 * <pre>
 * <code>
 * public class Employee {
 *   private long id;
 *
 *   @JoinedBy({ "id=EmployeeProject.employeeId", "EmployeeProject.projectId = id" })
 *   private List&lt;Project&gt; projects;
 *
 *   //...
 * }
 *
 * public class Project {
 *   private long id;
 *   // ...
 * }
 *
 * public class EmployeeProject {
 *   private long employeeId;
 *   private long projectId;
 *   // ...
 * }
 * </code>
 * </pre>
 */
@Documented
@Target(value = { FIELD /*, METHOD */ })
@Retention(RUNTIME)
public @interface JoinedBy {

    /**
     * Specifies the join conditions as an array of strings.
     * Each string defines a join condition in the format "sourceField=targetField".
     * For many-to-many relationships, multiple conditions can be specified to define
     * the join through an intermediate table.
     * 
     * @return an array of join condition strings, empty array by default
     */
    String[] value() default {};

    // OnDeleteAction onDelete() default OnDeleteAction.NO_ACTION; // TODO it's very complicated to support it.
}
