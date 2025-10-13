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
 * Marks a field as transient, indicating it should be excluded from persistence operations.
 * Fields annotated with @Transient will be ignored during database operations such as
 * insert, update, and select when working with entity objects.
 * 
 * <p><b>Common use cases for transient fields:</b></p>
 * <ul>
 *   <li>Calculated or derived values that shouldn't be stored</li>
 *   <li>Temporary state or cache data</li>
 *   <li>Runtime-only metadata</li>
 *   <li>Fields that are populated from other sources</li>
 *   <li>Security-sensitive data that shouldn't be persisted</li>
 * </ul>
 * 
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>This annotation only affects persistence operations, not serialization</li>
 *   <li>For serialization exclusion, use framework-specific annotations (e.g., @JsonIgnore)</li>
 *   <li>Transient fields may still be included in toString(), equals(), and hashCode()</li>
 *   <li>The field will still occupy memory in the object instance</li>
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
 *     private BigDecimal price;
 *     
 *     {@literal @}Transient
 *     private BigDecimal priceWithTax;  // Calculated field
 *     
 *     {@literal @}Transient
 *     private boolean modified;          // Runtime state
 *     
 *     {@literal @}Transient
 *     private List<Review> reviews;      // Loaded separately
 * }
 * </pre>
 * 
 * @since 2018
 * @see Column
 * @see Entity
 */
@Documented
@Target(value = { FIELD /* METHOD, */ })
@Retention(RUNTIME)
public @interface Transient {
}
