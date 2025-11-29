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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or field represents the value of an object
 * for JSON/XML serialization and deserialization purposes. This annotation is used
 * to designate a single property that contains the essential value of the object.
 * 
 * <p>When an object is annotated with this annotation on one of its properties,
 * that property's value will be used as the serialized representation of the entire object.
 * During deserialization, the value will be used to reconstruct the object.</p>
 * 
 * <p>This annotation is particularly useful for:</p>
 * <ul>
 *   <li>Wrapper objects that contain a single meaningful value</li>
 *   <li>Value objects where one field represents the primary content</li>
 *   <li>Custom types that need simplified JSON/XML representation</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *
 * public class Amount {
 *     @JsonXmlValue
 *     private BigDecimal value;
 *
 *     private String currency;  // This won't be serialized as the main value
 *
 *     public Amount(BigDecimal value, String currency) {
 *         this.value = value;
 *         this.currency = currency;
 *     }
 *
 *     // When serialized to JSON, only the 'value' field will be used
 *     // Instead of: {"value": 100.50, "currency": "USD"}
 *     // Result will be: 100.50
 * }
 * }</pre>
 * 
 * @see JsonXmlField
 * @see JsonXmlCreator
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlValue {

}
