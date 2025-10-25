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
 * Marks a method or constructor as the preferred creator for JSON/XML deserialization.
 * This annotation indicates that the annotated method or constructor should be used
 * when deserializing objects from JSON or XML formats.
 * 
 * <p>When applied to a constructor, it designates that constructor as the primary
 * way to create instances during deserialization. When applied to a static factory method,
 * it indicates that method should be used instead of the default constructor.</p>
 * 
 * <p>This annotation is particularly useful when:</p>
 * <ul>
 *   <li>A class has multiple constructors and you need to specify which one to use</li>
 *   <li>You want to use a static factory method instead of a constructor</li>
 *   <li>Special initialization logic is required during deserialization</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public class Person {
 *     private final String name;
 *     private final int age;
 *     
 *     // This constructor will be used for JSON/XML deserialization
 *     @JsonXmlCreator
 *     public Person(String name, int age) {
 *         this.name = name;
 *         this.age = age;
 *     }
 *     
 *     // Or use with a static factory method
 *     @JsonXmlCreator
 *     public static Person create(String name, int age) {
 *         return new Person(name, age);
 *     }
 * }
 * }</pre>
 * 
 * @see JsonXmlField
 * @see JsonXmlValue
 */
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonXmlCreator {

}
