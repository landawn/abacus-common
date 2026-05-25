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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Designates the constructor or static factory method that should be invoked when an object is
 * reconstructed from a single serialized value (JSON, XML, or persistence). This is the abacus
 * counterpart of Jackson's {@code @JsonCreator}.
 *
 * <p>It pairs with {@link JsonXmlValue}: a class that exposes one "wrapped" value via
 * {@code @JsonXmlValue} (on a field or no-arg method) also needs a way to be rebuilt from that
 * value during deserialization. The framework's {@code com.landawn.abacus.type.SingleValueType}
 * scans for a single-arg constructor or single-arg static factory method annotated with
 * {@code @JsonXmlCreator} and uses it to materialize instances from their wrapped value. If the
 * matching {@code @JsonXmlCreator} cannot be found (or its parameter type does not match the
 * {@code @JsonXmlValue} type), the type is rejected at registration time.</p>
 *
 * <p><b>Placement rules:</b></p>
 * <ul>
 *   <li>On a {@link ElementType#CONSTRUCTOR}: that constructor (typically taking exactly the
 *       wrapped value type) is used by the framework instead of the no-arg constructor.</li>
 *   <li>On a static {@link ElementType#METHOD}: the method is used as a factory; it must be
 *       {@code static} and return an instance of the enclosing class.</li>
 *   <li>At most one creator should be marked per class.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p><b>Constructor as creator (paired with {@link JsonXmlValue}):</b></p>
 * <pre>{@code
 * public final class EmailAddress {
 *     @JsonXmlValue
 *     private final String value;
 *
 *     @JsonXmlCreator
 *     public EmailAddress(String value) {
 *         if (!value.contains("@")) {
 *             throw new IllegalArgumentException("Invalid email: " + value);
 *         }
 *         this.value = value;
 *     }
 * }
 *
 * // Serializes to: "alice@example.com"
 * // Deserializes by invoking the @JsonXmlCreator constructor.
 * }</pre>
 *
 * <p><b>Static factory as creator:</b></p>
 * <pre>{@code
 * public final class Money {
 *     @JsonXmlValue
 *     private final String value;       // e.g. "USD 19.99"
 *
 *     private Money(String value) { this.value = value; }
 *
 *     @JsonXmlCreator
 *     public static Money parse(String value) {
 *         // Custom parsing logic.
 *         return new Money(value);
 *     }
 * }
 * }</pre>
 *
 * @see JsonXmlValue
 * @see JsonXmlField
 * @see com.landawn.abacus.type.SingleValueType
 */
@Documented
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlCreator {

}
