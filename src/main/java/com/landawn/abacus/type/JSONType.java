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

package com.landawn.abacus.type;

import java.util.List;
import java.util.Map;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for JSON-wrapped types.
 * This type handler wraps an arbitrary Java type and ensures that values are always
 * serialized to and deserialized from JSON strings. It is primarily used when a field
 * or column should be stored as a JSON string in the database rather than as its
 * native type representation.
 *
 * <p>Special handling is provided for {@code "Map"} and {@code "List"} as short aliases
 * for {@code java.util.Map} and {@code java.util.List} respectively.</p>
 *
 * @param <T> the type of object this JSONType wraps and serializes
 */
@SuppressWarnings("java:S2160")
public class JSONType<T> extends AbstractType<T> {

    /** The type name constant used as the {@code JSON<...>} prefix for JSON-wrapped type identification. */
    public static final String JSON = "JSON";

    private final String declaringName;

    private final Class<T> typeClass;
    //    private final Type<T>[] parameterTypes;
    //    private final Type<T> elementType;

    /**
     * Package-private constructor for JSONType.
     * Creates a JSONType that wraps the specified class for JSON serialization and deserialization.
     * The short aliases {@code "Map"} and {@code "List"} (case-insensitive) are recognized as
     * {@link java.util.Map} and {@link java.util.List} respectively; all other names must be
     * fully qualified class names resolvable by {@link com.landawn.abacus.util.ClassUtil}.
     *
     * @param clsName the class name or short alias ({@code "Map"}, {@code "List"}) for which
     *                to create the JSON type handler
     */
    @SuppressWarnings("unchecked")
    JSONType(final String clsName) {
        super(JSON + SK.LESS_THAN + TypeFactory.getType(clsName).name() + SK.GREATER_THAN);

        declaringName = JSON + SK.LESS_THAN + TypeFactory.getType(clsName).declaringName() + SK.GREATER_THAN;
        typeClass = (Class<T>) ("Map".equalsIgnoreCase(clsName) ? Map.class : ("List".equalsIgnoreCase(clsName) ? List.class : ClassUtil.forName(clsName)));
        //        this.parameterTypes = new Type[] { TypeFactory.getType(clsName) };
        //        this.elementType = parameterTypes[0];
    }

    /**
     * Returns the declaring name of this JSONType.
     * The declaring name wraps the underlying type's declaring name with the {@code JSON<>} notation,
     * for example {@code "JSON<java.util.Map>"} or {@code "JSON<com.example.MyBean>"}.
     *
     * @return the declaring name in the format {@code "JSON<TypeDeclaringName>"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object for the underlying type handled by this JSONType.
     *
     * @return the Class object representing type {@code T}
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Converts an object to its JSON string representation.
     * Uses the default JSON parser and serialization configuration.
     *
     * @param x the object to serialize; may be {@code null}
     * @return the JSON string representation of the object, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Deserializes a JSON string into an object of type {@code T}.
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return the deserialized object of type {@code T}, or {@code null} if {@code str} is {@code null} or empty
     */
    @Override
    public T valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}
