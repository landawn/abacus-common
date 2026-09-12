/*
 * Copyright (C) 2020 HaiYang Li
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
 * Explicitly marks a field as NOT mapping to a database column. This is a documentation marker
 * with no attributes; the abacus persistence layer (notably the {@code abacus-jdbc} module) reads
 * it when generating SQL and excludes the field from column-mapping introspection. The intent is
 * to make a non-column field unmistakable when scanning a class, where {@link Transient} or the
 * Java {@code transient} modifier would be a more general "exclude from persistence" signal.
 *
 * <p><b>When to choose {@code @NonColumn}:</b></p>
 * <ul>
 *   <li>Foreign-relationship fields loaded by a separate query rather than as a column
 *       (e.g., a {@code List<Review>} alongside a {@code Product}).</li>
 *   <li>Computed or derived values calculated by getters at runtime.</li>
 *   <li>UI / presentation-layer state that lives on the bean for convenience.</li>
 *   <li>Helper or session-scoped flags carried by an otherwise persisted bean.</li>
 *   <li>Any time you want the "this is intentionally NOT a column" reading at a glance.</li>
 * </ul>
 *
 * <p><b>Comparison with related annotations:</b></p>
 * <ul>
 *   <li>{@code @NonColumn} — explicit "this is not a column" marker (ORM-only intent).</li>
 *   <li>{@link Transient} — broader "exclude from persistence" semantics; also drops the field
 *       from default JSON/XML serialization.</li>
 *   <li>{@link Column} — explicit "this IS a column" with optional name override.</li>
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
 *     private BigDecimal basePrice;
 *
 *     @NonColumn
 *     private BigDecimal discountedPrice;   // discountedPrice is computed in getDiscountedPrice().
 *
 *     @NonColumn
 *     private List<Review> reviews;         // reviews are loaded via a separate fetch / JOIN call.
 *
 *     @NonColumn
 *     private boolean inCart;               // inCart is UI-only state, never persisted.
 *
 *     public BigDecimal getDiscountedPrice() {
 *         return basePrice.multiply(getCurrentDiscount());
 *     }
 * }
 * }</pre>
 *
 * @see Transient
 * @see Column
 * @see JoinedBy
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface NonColumn {
}
