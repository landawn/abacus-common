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
 * A test annotation used to mark classes as record-like entities, particularly for testing purposes.
 * This annotation is primarily used to identify entity classes created by Lombok that simulate
 * the behavior of Java 16+ record types in environments where records are not available.
 * 
 * <p>This annotation serves as a marker for testing frameworks and tools to understand that
 * the annotated class represents a data-only structure similar to a Java record, even when
 * implemented using traditional class syntax with Lombok annotations.</p>
 * 
 * <p><b>Primary use cases:</b></p>
 * <ul>
 *   <li>Testing record-like behavior in pre-Java 16 environments</li>
 *   <li>Marking Lombok-generated classes that simulate records</li>
 *   <li>Providing metadata for tools that need to distinguish record-like classes</li>
 *   <li>Supporting backward compatibility testing for record migration</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * @Record
 * @Data
 * @AllArgsConstructor
 * public class PersonRecord {
 *     private final String name;
 *     private final int age;
 *     private final String email;
 *     // Lombok generates constructor, getters, equals, hashCode, toString
 * }
 * 
 * // This class behaves similarly to a Java 16+ record:
 * // public record PersonRecord(String name, int age, String email) {}
 * }</pre>
 * 
 * <p><b>Testing implications:</b></p>
 * <ul>
 *   <li>Classes marked with this annotation should be treated as immutable</li>
 *   <li>All fields should be effectively final</li>
 *   <li>Equality should be based on field values, not identity</li>
 *   <li>String representation should include all field values</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This annotation is marked as {@link Test}, indicating it's intended
 * for testing purposes and may not be suitable for production code.</p>
 * 
 * @since 2021
 * @see Test
 * @see java.lang.Record
 */
@Test
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RUNTIME)
public @interface Record {

}
