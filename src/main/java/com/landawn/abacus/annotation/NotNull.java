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
 * This is a declarative contract: the annotation itself does not inject a runtime check.
 * Implementations must still validate inputs or otherwise ensure non-{@code null} results.
 *
 * <p><b>When applied to parameters:</b></p>
 * <ul>
 *   <li>The parameter must not be {@code null} when the method or constructor is called.</li>
 *   <li>Passing {@code null} will likely result in a {@link NullPointerException} or {@link IllegalArgumentException}.</li>
 *   <li>Callers should ensure a non-{@code null} value before invoking.</li>
 * </ul>
 *
 * <p><b>When applied to methods:</b></p>
 * <ul>
 *   <li>The method guarantees it will never return {@code null}.</li>
 *   <li>Callers can safely use the return value without {@code null} checks.</li>
 *   <li>The method must ensure a non-{@code null} return value in all code paths.</li>
 * </ul>
 *
 * <p><b>When applied to constructors:</b></p>
 * <ul>
 *   <li>Documents a constructor-wide non-null contract.</li>
 *   <li>Annotate individual parameters as well when their precise contracts need to be exposed.</li>
 * </ul>
 *
 * <p><b>Tool support:</b></p>
 * <ul>
 *   <li>IDEs can warn about potential {@code null} pointer issues.</li>
 *   <li>Static analysis tools can verify null-safety contracts.</li>
 *   <li>Frameworks may choose to perform runtime checks, but this library does not do so merely
 *       because the annotation is present.</li>
 * </ul>
 *
 * <p><b>Framework usage:</b> abacus uses this marker on parameters of widely-used utilities such
 * as {@code com.landawn.abacus.util.N}, {@code Beans}, {@code CommonUtil}, and {@code Converters}.
 * It pairs naturally with {@link MayReturnNull} on the return side of an API.</p>
 *
 * <p><b>Name collision:</b> the simple name {@code NotNull} collides with the widely used
 * {@code javax.annotation.Nonnull}, {@code jakarta.annotation.Nonnull},
 * {@code org.jetbrains.annotations.NotNull}, and {@code jakarta.validation.constraints.NotNull}.
 * These declarations are not interchangeable; when documenting an abacus API contract, ensure the
 * import resolves to this annotation.</p>
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
