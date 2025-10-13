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
 * Marks a field as non-updatable, excluding it from UPDATE operations while allowing INSERTs.
 * Fields annotated with @NonUpdatable can be set during entity creation but cannot be
 * modified afterwards through standard update operations.
 * 
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Creation timestamps that should never change after insert</li>
 *   <li>Creator/owner information that remains constant</li>
 *   <li>Immutable business identifiers (non-primary keys)</li>
 *   <li>Fields that establish permanent relationships</li>
 *   <li>Historical data that must be preserved</li>
 * </ul>
 * 
 * <p><b>Behavior comparison:</b></p>
 * <ul>
 *   <li>@NonUpdatable: Can INSERT, cannot UPDATE</li>
 *   <li>@ReadOnly: Cannot INSERT or UPDATE</li>
 *   <li>@Transient: Not persisted at all</li>
 * </ul>
 * 
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>The field can still be modified in the Java object</li>
 *   <li>Direct SQL updates can still modify the database column</li>
 *   <li>Only affects ORM-generated update statements</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@literal @}Entity
 * public class Order {
 *     {@literal @}Id
 *     private Long id;
 *     
 *     {@literal @}NonUpdatable
 *     {@literal @}Column(name = "order_number")
 *     private String orderNumber;      // Set once at creation
 *     
 *     {@literal @}NonUpdatable
 *     {@literal @}Column(name = "customer_id")
 *     private Long customerId;         // Cannot change customer after creation
 *     
 *     {@literal @}NonUpdatable
 *     {@literal @}Column(name = "created_date")
 *     private Date createdDate;        // Immutable creation timestamp
 *     
 *     {@literal @}Column(name = "status")
 *     private String status;           // Can be updated
 *     
 *     {@literal @}Column(name = "total_amount")
 *     private BigDecimal totalAmount;  // Can be updated
 * }
 * </pre>
 * 
 * @since 2018
 * @see ReadOnly
 * @see Transient
 * @see Column
 */
@Documented
@Target(value = { FIELD /* METHOD, */ })
@Retention(RUNTIME)
public @interface NonUpdatable {

}
