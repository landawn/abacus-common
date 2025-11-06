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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element gracefully handles {@code null} values without throwing exceptions.
 * This annotation documents that the code has been specifically designed to be defensive against
 * {@code null} inputs, providing predictable behavior even when {@code null} values are encountered.
 * 
 * <p><b>When applied to methods or constructors:</b></p>
 * <ul>
 *   <li>The method/constructor can accept {@code null} arguments without throwing NullPointerException</li>
 *   <li>Null inputs are handled with sensible default behavior or early returns</li>
 *   <li>The implementation includes proper {@code null} checks and defensive programming</li>
 * </ul>
 * 
 * <p><b>When applied to parameters:</b></p>
 * <ul>
 *   <li>Null is an acceptable and expected value for this parameter</li>
 *   <li>The method logic properly handles the {@code null} case</li>
 *   <li>No NullPointerException will occur from passing null</li>
 * </ul>
 * 
 * <p><b>Common null-safe patterns:</b></p>
 * <ul>
 *   <li>Returning empty collections instead of null</li>
 *   <li>Using default values when {@code null} is provided</li>
 *   <li>Gracefully skipping operations on {@code null} inputs</li>
 *   <li>Implementing the Null Object pattern</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * import java.util.Arrays;
 * import java.util.Collections;
 * import java.util.List;
 * import java.util.Optional;
 *
 * public class StringUtils {
 *     @NullSafe
 *     public static String trim(@NullSafe String input) {
 *         return input == null ? "" : input.trim();
 *     }
 *
 *     @NullSafe
 *     public static boolean isEmpty(@NullSafe String str) {
 *         return str == null || str.length() == 0;
 *     }
 *
 *     @NullSafe
 *     public static List<String> parseTokens(@NullSafe String input) {
 *         if (input == null) {
 *             return Collections.emptyList();
 *         }
 *         return Arrays.asList(input.split(","));
 *     }
 *
 *     @NullSafe
 *     public static Optional<String> toOptional(@NullSafe String value) {
 *         return Optional.ofNullable(value);
 *     }
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER })
public @interface NullSafe {
}
