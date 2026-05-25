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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Forces property access on a bean to go through getter / setter methods rather than direct field
 * reflection. This is consumed by the framework's reflection layer
 * ({@code com.landawn.abacus.parser.ParserUtil}) when it builds the property metadata for a class:
 * if the annotation is present on the class, or on the individual field/method, the property's
 * {@code isFieldGettable} / {@code isFieldSettable} flags are set to {@code false} and all reads
 * and writes are routed through the corresponding accessor methods, even when the field would
 * otherwise be directly accessible.
 *
 * <p>This is useful when a getter/setter performs work that must not be bypassed, such as:</p>
 * <ul>
 *   <li>Lazy initialization triggered on first read.</li>
 *   <li>Validation, normalization, or coercion performed inside the setter.</li>
 *   <li>Defensive copying of mutable values (collections, dates).</li>
 *   <li>Wrapping/unwrapping of stored values (e.g., {@code Optional}, encrypted strings).</li>
 *   <li>Interop with frameworks that mandate accessor-based property access.</li>
 * </ul>
 *
 * <p><b>Scope of application:</b></p>
 * <ul>
 *   <li>On a {@link ElementType#TYPE TYPE}: all properties of the class are accessed via their
 *       getter/setter methods.</li>
 *   <li>On a {@link ElementType#FIELD FIELD} or {@link ElementType#METHOD METHOD}: only that
 *       single property is forced to use accessor-based access.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p><b>Class-level — every property uses its accessor:</b></p>
 * <pre>{@code
 * @AccessFieldByMethod
 * public class User {
 *     private String name;
 *     private List<Role> roles;
 *
 *     public String getName() {
 *         // Normalization applied on every read.
 *         return name == null ? "" : name.trim();
 *     }
 *
 *     public List<Role> getRoles() {
 *         // Defensive copy: callers cannot mutate the internal list.
 *         return roles == null ? Collections.emptyList() : new ArrayList<>(roles);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Field-level — only a single property is routed through its setter:</b></p>
 * <pre>{@code
 * public class Order {
 *     private Long id;
 *
 *     @AccessFieldByMethod
 *     private BigDecimal total;
 *
 *     public void setTotal(BigDecimal total) {
 *         // Validation that must run on every deserialized / programmatic write.
 *         if (total != null && total.signum() < 0) {
 *             throw new IllegalArgumentException("total must be >= 0");
 *         }
 *         this.total = total;
 *     }
 * }
 * }</pre>
 *
 * @see Column
 * @see JsonXmlField
 * @see Transient
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
public @interface AccessFieldByMethod {

    /**
     * Reserved for future configuration use; currently has no effect on the framework's behavior.
     * The mere presence of {@code @AccessFieldByMethod} is sufficient to switch the affected
     * property (or all properties on the type) to accessor-based access. This element is kept on
     * the annotation so that future extensions (e.g., naming the getter/setter pair explicitly)
     * can be added without an incompatible API change.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @AccessFieldByMethod              // Sufficient; equivalent to @AccessFieldByMethod("").
     * public class User { ... }
     * }</pre>
     *
     * @return the (currently unused) configuration value; defaults to an empty string
     */
    String value() default "";
}
