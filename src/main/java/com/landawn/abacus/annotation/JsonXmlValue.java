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
 * Marks the single property whose value represents the entire object when serialized. This is the
 * abacus counterpart of Jackson's {@code @JsonValue}.
 *
 * <p>When the framework sees {@code @JsonXmlValue} on a field or no-arg method, the
 * {@code com.landawn.abacus.type.SingleValueType} machinery treats the host class as a "wrapper"
 * around that single value: serialization writes only the wrapped value (not a JSON object with a
 * named property), and deserialization rebuilds the instance by feeding the wrapped value back
 * through the paired {@link JsonXmlCreator}.</p>
 *
 * <p><b>Placement rules:</b></p>
 * <ul>
 *   <li>Exactly one {@code @JsonXmlValue} should appear per class.</li>
 *   <li>When applied to a {@link ElementType#METHOD}, the method must be a no-arg "getter" that
 *       returns the wrapped value.</li>
 *   <li>When applied to a {@link ElementType#FIELD}, the field holds the wrapped value directly.</li>
 *   <li>The wrapped value's type must match the parameter of the class's
 *       {@link JsonXmlCreator}-annotated constructor or factory method.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p><b>Wrapper type with one meaningful value:</b></p>
 * <pre>{@code
 * public final class CustomerId {
 *     @JsonXmlValue
 *     private final long value;
 *
 *     @JsonXmlCreator
 *     public CustomerId(long value) { this.value = value; }
 *
 *     public long value() { return value; }
 * }
 *
 * // CustomerId(42)  ->  serialized as: 42
 * //                 ->  NOT as: {"value": 42}
 * }</pre>
 *
 * <p><b>Method-style value (e.g., to apply a transformation before serialization):</b></p>
 * <pre>{@code
 * public final class Amount {
 *     private final BigDecimal raw;
 *     private final String currency;
 *
 *     private Amount(BigDecimal raw, String currency) { this.raw = raw; this.currency = currency; }
 *
 *     @JsonXmlCreator
 *     public static Amount parse(String s) {
 *         String[] parts = s.split(" ");
 *         return new Amount(new BigDecimal(parts[1]), parts[0]);
 *     }
 *
 *     @JsonXmlValue
 *     public String toWireFormat() {
 *         return currency + " " + raw.toPlainString();
 *     }
 * }
 * // Amount(19.99, "USD")  ->  "USD 19.99"
 * }</pre>
 *
 * @see JsonXmlCreator
 * @see JsonXmlField
 * @see com.landawn.abacus.type.SingleValueType
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlValue {

}
