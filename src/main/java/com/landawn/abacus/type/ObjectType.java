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
 * Type handler for generic {@link Object} types, providing basic serialization,
 * deserialization, and type conversion capabilities. This is the most general
 * type handler that can handle any Java object.
 *
 * @param <T> the specific type, defaults to Object but can be parameterized
 */
public final class ObjectType<T> extends SingleValueType<T> {

    public static final String OBJECT = Object.class.getSimpleName();

    /**
     * Constructs an ObjectType for the generic Object class.
     * This constructor creates a type handler for java.lang.Object.
     */
    ObjectType() {
        this((Class<T>) Object.class);
    }

    /**
     * Constructs an ObjectType for a specific class.
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