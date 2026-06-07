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
 * <p><b>Important caveat:</b> {@code @DiffIgnore} is consulted only when
 * {@link com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)} compares
 * <i>all</i> properties of the two beans. When an explicit {@code propNamesToCompare} collection
 * is passed to {@code BeanDifference.of(bean1, bean2, propNamesToCompare)}, the annotation is
 * ignored — the caller has stated which properties to compare, and that list wins.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class User {
 *     private Long id;
 *     private String username;
 *     private String email;
 *
 *     @DiffIgnore
 *     private Timestamp lastModified;  // lastModified is changed on every update.
 *
 *     @DiffIgnore
 *     private Integer version;         // version is an optimistic locking field.
 *
 *     @DiffIgnore
 *     private String sessionToken;     // sessionToken holds transient runtime data.
 * }
 *
 * User user1 = repository.findById(42L);
 * User user2 = repository.findById(42L);   // user2 is the same row, re-loaded later.
 * user2.setEmail("new@example.com");
 *
 * // Compares every property except lastModified, version, sessionToken.
 * BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff
 *         = Difference.BeanDifference.of(user1, user2);
 *
 * diff.inCommon();        // {id=42, username=..., ...}
 * diff.differentValues(); // {email=Pair.of("old@example.com", "new@example.com")}
 *
 * // Explicit property list: @DiffIgnore is NOT honored here.
 * Difference.BeanDifference.of(user1, user2, List.of("email", "lastModified"));
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
