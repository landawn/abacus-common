/*
 * Copyright (C) 2021 HaiYang Li
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
 * Marks a class as a record-like entity, treating it as equivalent to a Java record for ORM
 * and reflection-based operations. This annotation is primarily used to identify classes
 * (for example, those backed by Lombok) that simulate the behavior of Java 14+ record types
 * when native record syntax is unavailable or impractical.
 *
 * <p>A class annotated with {@code @Record} is treated as a record class by framework utilities
 * such as {@code Beans}, enabling record-specific introspection and mapping behavior even when
 * the class does not extend {@link java.lang.Record}.</p>
 *
 * <p><b>Primary use cases:</b></p>
 * <ul>
 *   <li>Marking Lombok-generated classes (e.g., using {@code @Value} or {@code @Data} with final
 *       fields) that simulate Java records.</li>
 *   <li>Providing metadata for frameworks that distinguish record-like data classes from
 *       regular mutable beans.</li>
 *   <li>Enabling backward-compatible record semantics in codebases that cannot yet use Java 14+
 *       native records.</li>
 * </ul>
 *
 * <p><b>Implied semantics for annotated classes:</b></p>
 * <ul>
 *   <li>Instances should be effectively immutable.</li>
 *   <li>All fields should be effectively final.</li>
 *   <li>Equality is based on field values, not object identity.</li>
 *   <li>String representation includes all field values.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Record
 * @Value           // Lombok: generates all-args constructor, getters, equals, hashCode, toString
 * public class PersonRecord {
 *     String name;
 *     int age;
 *     String email;
 * }
 *
 * // This class is treated equivalently to a Java 14+ record:
 * // public record PersonRecord(String name, int age, String email) {}
 * }</pre>
 *
 * @see Immutable
 * @see java.lang.Record
 */
@Test
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RUNTIME)
public @interface Record {

}
