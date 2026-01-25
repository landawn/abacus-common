/*
 * Copyright (C) 2023 HaiYang Li
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method may return {@code null} values under certain conditions.
 * This annotation explicitly documents that {@code null} is a legitimate return value,
 * warning callers to perform {@code null} checks before using the returned value.
 * 
 * <p><b>Purpose and benefits:</b></p>
 * <ul>
 *   <li>Makes null-return contracts explicit and visible</li>
 *   <li>Helps prevent NullPointerException in calling code</li>
 *   <li>Enables better IDE support with null-safety warnings</li>
 *   <li>Documents API behavior clearly for users</li>
 *   <li>Facilitates static analysis for null-safety verification</li>
 * </ul>
 * 
 * <p><b>Best practices:</b></p>
 * <ul>
 *   <li>Always document in JavaDoc when and why {@code null} might be returned</li>
 *   <li>Consider returning Optional instead for new APIs</li>
 *   <li>Use consistently with @NotNull to establish complete null-safety contracts</li>
 *   <li>Ensure calling code handles the {@code null} case appropriately</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class UserRepository {
 *     /**
 *      * Finds a user by username.
 *      * @param username the username to search for
 *      * @return the User if found, null if not found
 *      {@literal *}/
 *     @MayReturnNull
 *     public User findByUsername(String username) {
 *         return database.query("SELECT * FROM users WHERE username = ?", username);
 *     }
 *     
 *     // Caller must check for null:
 *     User user = repository.findByUsername("john");
 *     if (user != null) {
 *         // Safe to use user
 *     }
 * }
 * }</pre>
 * 
 * @see NotNull
 * @see NullSafe
 */
@Documented
@Target(value = { METHOD })
@Retention(RUNTIME)
public @interface MayReturnNull {

}
