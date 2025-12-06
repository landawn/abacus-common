/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.parser;

/**
 * Enumeration defining property exclusion policies for serialization.
 * This enum controls which bean properties should be excluded during the serialization process.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Exclude properties with null values
 * SerializationConfig config = new SerializationConfig()
 *     .setExclusion(Exclusion.NULL);
 *
 * // Exclude properties with default values (including null)
 * config.setExclusion(Exclusion.DEFAULT);
 *
 * // Include all properties
 * config.setExclusion(Exclusion.NONE);
 * }</pre>
 * 
 */
public enum Exclusion {
    /**
     * Excludes bean properties with {@code null} values during serialization.
     * Properties that are {@code null} will not be included in the serialized output.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person();
     * person.setName("John");
     * person.setAge(null);   // This property will be excluded
     * 
     * // With Exclusion.NULL, output: {"name": "John"}
     * }</pre>
     */
    NULL,

    /**
     * Excludes bean properties with default values during serialization.
     * This includes {@code null} values and primitive type default values (0, false, etc.).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person();
     * person.setName("John");
     * person.setAge(0);          // Default int value, will be excluded
     * person.setActive(false);   // Default boolean value, will be excluded
     * 
     * // With Exclusion.DEFAULT, output: {"name": "John"}
     * }</pre>
     */
    DEFAULT,

    /**
     * No bean properties are excluded during serialization.
     * All properties will be included in the serialized output regardless of their values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person();
     * person.setName("John");
     * person.setAge(null);
     * 
     * // With Exclusion.NONE, output: {"name": "John", "age": null}
     * }</pre>
     */
    NONE
}
