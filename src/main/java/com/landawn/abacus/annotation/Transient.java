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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as transient — i.e., excluded from persistence operations. The
 * {@code @Transient} annotation and the Java {@code transient} field modifier are treated as
 * equivalent by the framework's reflection layer ({@code com.landawn.abacus.parser.ParserUtil}),
 * which sets {@code PropInfo.isTransient = true} for either signal. The property is then dropped
 * from the {@code nonTransientSeriPropInfos} array used to drive serialization and persistence.
 *
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Calculated or derived values not stored in the database.</li>
 *   <li>Temporary state or cache data populated at runtime.</li>
 *   <li>Runtime-only metadata (e.g., loaded-from-cache flag).</li>
 *   <li>Related entities loaded separately rather than as a column.</li>
 *   <li>Security-sensitive values that must not be persisted.</li>
 * </ul>
 *
 * <p><b>Interaction with JSON/XML serialization:</b></p>
 * <ul>
 *   <li>A transient property is excluded from serialization by default.</li>
 *   <li>A transient property must leave {@link JsonXmlField#direction() @JsonXmlField(direction = ...)}
 *       at its default ({@code Direction.BOTH}); combining {@code @Transient} (or the {@code transient}
 *       modifier) with an explicit non-default direction causes an {@code IllegalArgumentException}
 *       when the bean is first introspected. To include transient fields in serialized output,
 *       configure the serializer with {@code setSkipTransientField(false)}.</li>
 *   <li>To exclude a non-transient field from serialization only, prefer
 *       {@link JsonXmlField#ignore() @JsonXmlField(ignore = true)} rather than {@code @Transient}.</li>
 * </ul>
 *
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>{@code @Transient} fields still occupy memory in the Java object and may still appear in
 *       {@link Object#toString()}, {@link Object#equals(Object)}, and {@link Object#hashCode()}
 *       depending on the class's own implementation.</li>
 *   <li>For "this is not a column" semantics that still allows the bean property to be visible
 *       to other layers, see {@link NonColumn}.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Entity
 * public class Product {
 *     @Id
 *     private Long id;
 *
 *     @Column
 *     private String name;
 *
 *     @Column
 *     private BigDecimal price;
 *
 *     @Transient
 *     private BigDecimal priceWithTax;   // priceWithTax is computed at runtime.
 *
 *     @Transient
 *     private boolean modified;          // modified is UI/runtime state.
 *
 *     @Transient
 *     private List<Review> reviews;      // reviews are loaded by a separate query.
 * }
 * }</pre>
 *
 * @see Column
 * @see Entity
 * @see NonColumn
 * @see JsonXmlField#ignore()
 * @see JsonXmlField#direction()
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface Transient {
}
