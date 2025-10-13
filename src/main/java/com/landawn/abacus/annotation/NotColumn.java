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
 * Explicitly marks a field as NOT being a database column.
 * This annotation is used to exclude fields from automatic column mapping in entities,
 * providing an alternative to @Transient with clearer semantic meaning.
 * 
 * <p><b>When to use @NotColumn:</b></p>
 * <ul>
 *   <li>Fields that represent relationships managed separately (not foreign keys)</li>
 *   <li>Computed or derived values not stored in the database</li>
 *   <li>UI-specific or presentation-layer data</li>
 *   <li>Temporary state or helper fields</li>
 *   <li>When you want to be explicit about non-column fields for clarity</li>
 * </ul>
 * 
 * <p><b>Difference from related annotations:</b></p>
 * <ul>
 *   <li>@NotColumn: Explicitly declares "this is not a column"</li>
 *   <li>@Transient: More general "exclude from persistence"</li>
 *   <li>@Column: Explicitly declares "this is a column"</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@literal @}Entity
 * public class Product {
 *     {@literal @}Id
 *     private Long id;
 *     
 *     {@literal @}Column
 *     private String name;
 *     
 *     {@literal @}Column
 *     private BigDecimal basePrice;
 *     
 *     {@literal @}NotColumn
 *     private BigDecimal discountedPrice;  // Calculated at runtime
 *     
 *     {@literal @}NotColumn
 *     private List<Review> reviews;         // Loaded separately via JOIN
 *     
 *     {@literal @}NotColumn
 *     private boolean inCart;               // UI state, not persisted
 *     
 *     public BigDecimal getDiscountedPrice() {
 *         // Calculate based on current promotions
 *         return basePrice.multiply(getCurrentDiscount());
 *     }
 * }
 * </pre>
 * 
 * @since 2020
 * @see Transient
 * @see Column
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface NotColumn {
}
