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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for JSON serialization and deserialization of generic types.
 * This type wraps another type and handles JSON string conversion for it.
 *
 * @param <T> the type of object this JSONType handles
 */
@SuppressWarnings("java:S2160")
public class JSONType<T> extends AbstractType<T> {

    public static final String JSON = "JSON";

    private final String declaringName;

    private final Class<T> typeClass;
    //    private final Type<T>[] parameterTypes;
    //    private final Type<T> elementType;

    /**
     * Constructs a JSONType for the specified class name.
     * 
     * This constructor creates a JSONType that handles JSON serialization/deserialization
     * for the specified type. Special handling is provided for "Map" and "List" class names.
     *
     * @param clsName the name of the class to create a JSONType for.
     *                Can be "Map" for java.util.Map, "List" for java.util.List,
     *                or a fully qualified class name
     */
    @SuppressWarnings("unchecked")
    JSONType(final String clsName) {
        super(JSON + WD.LESS_THAN + TypeFactory.getType(clsName).name() + WD.GREATER_THAN);

        declaringName = JSON + WD.LESS_THAN + TypeFactory.getType(clsName).declaringName() + WD.GREATER_THAN;
        typeClass = (Class<T>) ("Map".equalsIgnoreCase(clsName) ? Map.class : ("List".equalsIgnoreCase(clsName) ? List.class : ClassUtil.forClass(clsName)));
        //        this.parameterTypes = new Type[] { TypeFactory.getType(clsName) };
        //        this.elementType = parameterTypes[0];
    }

    /**
     * Gets the declaring name of this JSONType.
     * 
     * The declaring name includes the JSON wrapper notation and the declaring name
     * of the wrapped type (e.g., "JSON<Map>", "JSON<List>").
     *
     * @return the declaring name of this type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Gets the class object for the type handled by this JSONType.
     *
     * @return the Class object representing the type T
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    //    @Override
    //    public Type<T> getElementType() {
    //        return elementType;
    //    }
    //
    //    @Override
    //    public Type<T>[] getParameterTypes() {
    //        return parameterTypes;
    //    }
    //
    //    @Override
    //    public boolean isGenericType() {
    //        return true;
    //    }

    /**
     * Converts the specified object to its JSON string representation.
     * 
     * This method serializes the object using the JSON parser with default
     * serialization configuration.
     *
     * @param x the object to convert to JSON string
     * @return the JSON string representation of the object, or null if the input is null
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string into an object of type T.
     * 
     * This method deserializes the JSON string using the JSON parser into
     * an instance of the type class handled by this JSONType.
     *
     * @param str the JSON string to parse
     * @return the deserialized object of type T, or null if the string is empty or null
     */
    @Override
    public T valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }
}