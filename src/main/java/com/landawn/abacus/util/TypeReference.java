/*
 * Copyright (C) 2017 HaiYang Li
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

package com.landawn.abacus.util;

import java.lang.reflect.ParameterizedType;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

/**
 * A utility class for capturing and representing generic type information at runtime.
 * This abstract class provides a workaround for Java's type erasure by requiring
 * subclasses to specify their generic type parameter, which can then be retrieved
 * using reflection.
 * 
 * <p>Due to Java's type erasure, generic type information is normally lost at runtime.
 * This class captures that information by analyzing the generic superclass of concrete
 * subclasses, allowing frameworks to access complete type information including
 * generic parameters.
 * 
 * <p>To use this class, create an anonymous subclass with the desired generic type:
 * <pre>{@code
 * // Capture type information for List<String>
 * TypeReference<List<String>> listType = new TypeReference<List<String>>() {};
 * 
 * // Capture type information for Map<String, Integer>
 * TypeReference<Map<String, Integer>> mapType = new TypeReference<Map<String, Integer>>() {};
 * 
 * // Use with JSON parsing or other frameworks
 * List<String> list = jsonParser.parse(json, listType);
 * }</pre>
 * 
 * <p>Note: This approach cannot capture types with wildcard parameters such as
 * {@code Class<?>} or {@code List<? extends CharSequence>}. Attempting to do so
 * will result in a runtime exception.
 * 
 * <p>Common use cases include:
 * <ul>
 *   <li>JSON/XML deserialization with generic types</li>
 *   <li>Dependency injection frameworks</li>
 *   <li>ORM frameworks for mapping generic collections</li>
 *   <li>Any framework that needs runtime access to generic type information</li>
 * </ul>
 *
 * @param <T> the type to be captured and represented
 * @see Type
 * @see TypeFactory
 * @since 1.0
 */
@SuppressWarnings({ "java:S1694" })
public abstract class TypeReference<T> {

    /**
     * The Type instance representing the captured generic type information.
     * This field is initialized in the constructor by analyzing the generic
     * superclass of the concrete subclass.
     */
    protected final Type<T> type;

    /**
     * Constructs a new TypeReference by capturing the generic type parameter
     * from the concrete subclass. This constructor uses reflection to extract
     * the actual type argument from the parameterized superclass.
     * 
     * <p>This constructor must be called from a concrete subclass that specifies
     * the generic type parameter. Direct instantiation of TypeReference is not
     * possible as it is abstract.
     * 
     * <p>Example:
     * <pre>{@code
     * // This creates an anonymous subclass that captures List<String>
     * TypeReference<List<String>> ref = new TypeReference<List<String>>() {};
     * }</pre>
     * 
     * @throws ClassCastException if the superclass is not parameterized
     * @throws IllegalStateException if type information cannot be extracted
     */
    protected TypeReference() {
        type = TypeFactory.getType(((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    /**
     * Returns the Type instance representing the captured generic type.
     * This Type object contains complete information about the generic type,
     * including any nested type parameters.
     * 
     * <p>The returned Type can be used by frameworks for deserialization,
     * dependency injection, or any other purpose requiring runtime type information.
     * 
     * <p>Example usage:
     * <pre>{@code
     * TypeReference<List<Map<String, Integer>>> complexType = 
     *     new TypeReference<List<Map<String, Integer>>>() {};
     * 
     * Type<List<Map<String, Integer>>> type = complexType.type();
     * // Use the type for deserialization or other purposes
     * Object result = deserializer.deserialize(data, type);
     * }</pre>
     *
     * @return the Type instance representing the generic type T
     */
    public Type<T> type() {
        return type;
    }

    /**
     * An alternative abstract base class for capturing type information.
     * This class serves the same purpose as TypeReference but uses a different
     * naming convention that may be more familiar to developers coming from
     * other frameworks.
     * 
     * <p>TypeToken functions identically to TypeReference - it captures generic
     * type information through subclassing. The name "TypeToken" is used in
     * some popular libraries like Guava and Gson.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Using TypeToken instead of TypeReference
     * TypeToken<List<String>> token = new TypeToken<List<String>>() {};
     * Type<List<String>> type = token.type();
     * }</pre>
     * 
     * @param <T> the type to be captured and represented
     * @since 1.0
     */
    public abstract static class TypeToken<T> extends TypeReference<T> {
        // Constructor is implicitly defined and delegates to TypeReference
    }
}