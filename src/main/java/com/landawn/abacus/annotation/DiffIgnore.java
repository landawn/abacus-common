/*
 * Copyright (C) 2025 HaiYang Li
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be excluded from bean difference comparison operations.
 * When comparing two bean instances for differences, fields marked with this annotation
 * will be completely ignored, allowing you to focus on meaningful business data changes.
 * 
 * <p><b>Common use cases for ignored fields:</b></p>
 * <ul>
 *   <li>Timestamps (created, modified dates) that change on every update</li>
 *   <li>Version numbers or revision fields used for optimistic locking</li>
 *   <li>Internal state or cache fields not relevant to business equality</li>
 *   <li>Transient calculation results</li>
 *   <li>System-generated metadata</li>
 *   <li>Fields that are expected to differ but aren't meaningful differences</li>
 * </ul>
 * 
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Cleaner difference reports focusing on actual data changes</li>
 *   <li>More accurate change detection for audit trails</li>
 *   <li>Simplified testing by ignoring volatile fields</li>
 *   <li>Better performance by skipping complex field comparisons</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class User {
 *     private Long id;
 *     private String username;
 *     private String email;
 *
 *     @DiffIgnore
 *     private Timestamp lastModified;  // Changes on every update
 *
 *     @DiffIgnore
 *     private Integer version;  // Optimistic locking field
 *
 *     @DiffIgnore
 *     private String sessionToken;  // Temporary runtime data
 * }
 *
 * // Usage with Difference.BeanDifference:
 * User user1 = getUserFromDatabase();
 * User user2 = getUserAfterUpdate();
 *
 * // Comparison will ignore lastModified, version, and sessionToken
 * // Fields annotated with @DiffIgnore are excluded from comparison
 * Difference.BeanDifference<?, ?, ?> diff = Difference.BeanDifference.of(user1, user2);
 * }</pre>
 * 
 * @see com.landawn.abacus.util.Difference.BeanDifference
 * @see com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD })
public @interface DiffIgnore {

}
