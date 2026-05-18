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

/**
 * Type handler for {@link Object} (and arbitrary subtype) values, providing basic
 * serialization, deserialization, and type conversion capabilities. This is the most
 * general fallback {@link Type} implementation, used when no more specific handler is
 * available for the runtime class. It inherits its behavior from {@link SingleValueType}.
 *
 * @param <T> the specific Java class this type handler manages; defaults to {@link Object}
 *            when constructed via the no-arg constructor, but may be parameterized to any
 *            subclass via the protected constructors
 */
public final class ObjectType<T> extends SingleValueType<T> {

    /** The type name constant for Object type identification, equal to {@code "Object"}. */
    public static final String OBJECT = Object.class.getSimpleName();

    /**
     * Constructs an ObjectType for the generic Object class.
     * This constructor creates a type handler for java.lang.Object.
     */
    ObjectType() {
        this((Class<T>) Object.class);
    }

    /**
     * Constructs an ObjectType with a specific class.
     * This constructor creates a type handler for a specific class type.
     *
     * @param cls the class to create a type handler for
     */
    protected ObjectType(final Class<T> cls) {
        super(cls);
    }

    /**
     * Constructs an ObjectType with a custom type name and specific class.
     *
     * @param typeName the custom name for this type
     * @param cls the class to create a type handler for
     */
    protected ObjectType(final String typeName, final Class<T> cls) {
        super(typeName, cls);
    }
}
