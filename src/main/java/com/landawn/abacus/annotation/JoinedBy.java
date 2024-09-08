/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * Sample for one-one or one-many join:
 *
 * <pre>
 * <code>
 * public class Account {
 *   private long id;
 *
 *   @JoinedBy("id=accountId")
 *   private List&ltDevice&gt devices;
 *
 *   //...
 * }
 *
 * public class Device {
 *   private long accountId;
 *   // ...
 * }
 *
 * </code>
 * </pre>
 *
 * Sample for many-many join:
 *
 * <pre>
 * <code>
 * public class Employee {
 *   private long id;
 *
 *   @JoinedBy({ "id=EmployeeProject.employeeId", "EmployeeProject.projectId = id" })
 *   private List&ltProject&gt projects;
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
 *
 * </code>
 * </pre>
 *
 */
@Documented
@Target(value = { FIELD /*, METHOD */ })
@Retention(RUNTIME)
public @interface JoinedBy {

    /**
     *
     *
     * @return
     */
    String[] value() default {};

    // OnDeleteAction onDelete() default OnDeleteAction.NO_ACTION; // TODO it's very complicated to support it.
}
