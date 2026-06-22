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
 * Marks a field as non-updatable, excluding it from {@code UPDATE} operations while allowing {@code INSERT}s.
 * Fields annotated with {@code @NonUpdatable} can be set during entity creation but cannot be
 * modified afterwards through standard update operations.
 *
 * <p><b>Common use cases:</b></p>
 * <ul>
 *   <li>Creation timestamps that should never change after insert.</li>
 *   <li>Creator/owner information that remains constant.</li>
 *   <li>Immutable business identifiers (non-primary keys).</li>
 *   <li>Fields that establish permanent relationships.</li>
 *   <li>Historical data that must be preserved.</li>
 * </ul>
 *
 * <p><b>Behavior comparison:</b></p>
 * <ul>
 *   <li>{@code @NonUpdatable}: Can {@code INSERT}, cannot {@code UPDATE}.</li>
 *   <li>{@link ReadOnly}: Cannot {@code INSERT} or {@code UPDATE}.</li>
 *   <li>{@link Transient}: Not persisted at all.</li>
 * </ul>
 *
 * <p><b>Important notes:</b></p>
 * <ul>
 *   <li>The field can still be modified on the Java object (the annotation does not enforce
 *       immutability of the bean itself).</li>
 *   <li>Direct SQL updates issued outside the ORM layer can still modify the column.</li>
 *   <li>Affects only ORM-generated {@code UPDATE} statements; the column still participates in
 *       {@code INSERT} and {@code SELECT}.</li>
 *   <li>The marker is read by the {@code abacus-jdbc} module when generating SQL; the
 *       {@code abacus-core} library only declares it as a runtime-retained marker.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Entity
 * public class Order {
 *     @Id
 *     private Long id;
 *
 *     @NonUpdatable
 *     @Column(name = "order_number")
 *     private String orderNumber;  // orderNumber is set once at creation
 *
 *     @NonUpdatable
 *     @Column(name = "customer_id")
 *     private Long customerId;  // customerId is fixed after creation
 *
 *     @NonUpdatable
 *     @Column(name = "created_date")
 *     private Date createdDate;  // createdDate is an immutable creation timestamp
 *
 *     @Column(name = "status")
 *     private String status;  // status is updatable
 *
 *     @Column(name = "total_amount")
 *     private BigDecimal totalAmount;  // totalAmount is updatable
 * }
 * }</pre>
 *
 * @see ReadOnly
 * @see Transient
 * @see Column
 */
@Documented
@Target(value = { FIELD })
@Retention(RUNTIME)
public @interface NonUpdatable {

}
