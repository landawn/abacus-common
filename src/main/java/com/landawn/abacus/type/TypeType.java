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

import com.landawn.abacus.util.Strings;

@SuppressWarnings("rawtypes")
public class TypeType extends AbstractType<Type> {

    public static final String TYPE = "Type";

    TypeType() {
        super(TYPE);
    }

    TypeType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Class object representing the Type interface.
     * <p>
     * This method returns {@code Type.class}, which is the Class object for the
     * {@link Type} interface that this TypeType handles.
     * </p>
     *
     * @return the Class object for Type.class
     */
    @Override
    public Class<Type> clazz() {
        return Type.class;
    }

    /**
     * Checks if Type instances are immutable.
     * <p>
     * Type instances are considered immutable as they represent type metadata
     * that should not change once created.
     * </p>
     *
     * @return {@code true}, indicating that Type instances are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts a Type instance to its string representation.
     * <p>
     * This method returns the name of the Type instance by calling its {@code name()} method.
     * If the input Type is null, this method returns null.
     * </p>
     *
     * @param x the Type instance to convert to string
     * @return the name of the Type, or null if the input is null
     */
    @Override
    public String stringOf(final Type x) {
        return (x == null) ? null : x.name();
    }

    /**
     * Converts a string to a Type instance.
     * <p>
     * This method retrieves a Type instance from the TypeFactory using the provided string
     * as the type name. If the string is null or empty, this method returns null.
     * </p>
     * <p>
     * The string should be a valid type name that has been registered with the TypeFactory,
     * such as "String", "Integer", "List<String>", etc.
     * </p>
     *
     * @param str the type name string to convert to a Type instance
     * @return the Type instance corresponding to the type name, or null if the string is empty
     * @throws IllegalArgumentException if the type name is not recognized by TypeFactory
     */
    @Override
    public Type valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : TypeFactory.getType(str);
    }
}