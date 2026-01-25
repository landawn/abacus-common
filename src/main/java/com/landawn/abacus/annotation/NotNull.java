/*
 * Copyright (C) 2024 HaiYang Li
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
 * Indicates that the annotated element must not be {@code null}.
 * This annotation documents and enforces null-safety contracts, helping prevent
 * NullPointerExceptions and making code more robust and self-documenting.
 * 
 * <p><b>When applied to parameters:</b></p>
 * <ul>
 *   <li>The parameter must not be {@code null} when the method/constructor is called</li>
 *   <li>Passing {@code null} will likely result in NullPointerException or IllegalArgumentException</li>
 *   <li>Callers should validate {@code non-null} before invoking</li>
 * </ul>
 * 
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method guarantees it will never return null</li>
 *   <li>Callers can safely use the return value without {@code null} checks</li>
 *   <li>The method must ensure a {@code non-null} return value in all code paths</li>
 * </ul>
 * 
 * <p><b>When applied to constructors:</b></p>
 * <ul>
 *   <li>Documents null-safety requirements for constructor parameters</li>
 *   <li>May indicate that the constructor validates against {@code null} inputs</li>
 * </ul>
 * 
 * <p><b>Tool support:</b></p>
 * <ul>
 *   <li>IDEs can warn about potential {@code null} pointer issues</li>
 *   <li>Static analysis tools can verify null-safety contracts</li>
 *   <li>Some frameworks may perform runtime {@code null} checks</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class UserService {
 *     @NotNull
 *     public User findUser(@NotNull String username) {
 *         // Method guarantees non-null return and requires non-null parameter
 *         User user = userRepository.find(username);
 *         return user != null ? user : User.anonymous();
 *     }
 *     
 *     public UserService(@NotNull DataSource dataSource) {
 *         this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
 *     }
 * }
 * }</pre>
 * 
 * @see MayReturnNull
 * @see NullSafe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER })
public @interface NotNull {
}
