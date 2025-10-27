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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Sheet, which represents a two-dimensional table structure with row keys,
 * column keys, and values. This class handles serialization and deserialization of Sheet instances.
 *
 * @param <R> the row key type
 * @param <C> the column key type
 * @param <E> the element/value type
 */
@SuppressWarnings("java:S2160")
public class SheetType<R, C, E> extends AbstractType<Sheet<R, C, E>> {
    private final String declaringName;

    private final Class<Sheet<R, C, E>> typeClass;

    private final Type<?>[] parameterTypes;

    /**
     * Constructs a SheetType with the specified type names for row keys, column keys, and values.
     *
     * @param rowKeyTypeName the type name for row keys
     * @param columnKeyTypeName the type name for column keys
     * @param elementTypeName the type name for values/elements
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SheetType(final String rowKeyTypeName, final String columnKeyTypeName, final String elementTypeName) {
        super(getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, false));

        declaringName = getTypeName(Sheet.class, rowKeyTypeName, columnKeyTypeName, elementTypeName, true);

        typeClass = (Class) Sheet.class;
        parameterTypes = new Type[] { TypeFactory.getType(rowKeyTypeName), TypeFactory.getType(columnKeyTypeName), TypeFactory.getType(elementTypeName) };
    }

    /**
     * Returns the declaring name of this type, which includes the Sheet class name
     * and its parameterized types in angle brackets using declaring names.
     *
     * @return the declaring name in the format "Sheet&lt;RowType, ColumnType, ElementType&gt;"
     @MayReturnNull
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Sheet type.
     *
     * @return the Class object for Sheet.class
     */
    @Override
    public Class<Sheet<R, C, E>> clazz() {
        return typeClass;
    }

    /**
     * Returns an array containing the Type instances for the parameter types of this Sheet.
     * The array contains three elements: row key type, column key type, and element type.
     *
     * @return an array with Type instances for row key, column key, and element types
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type.
     * SheetType is always generic as it is parameterized with three type parameters.
     *
     * @return {@code true}, indicating this is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether this type is serializable.
     * SheetType does not support standard serialization.
     *
     * @return {@code false}, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    @MayReturnNull

    @Override
    public SerializationType getSerializationType() {
        return SerializationType.SHEET;
    }

    /**
     * Converts a Sheet to its JSON string representation.
     * The sheet structure is serialized maintaining the row-column-value relationships.
     *
     * @param x the Sheet to convert to string
     * @return the JSON string representation of the sheet, or {@code null} if the input is null
     */
    @MayReturnNull
    @SuppressWarnings("rawtypes")
    @Override
    public String stringOf(final Sheet x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string representation and returns the corresponding Sheet object.
     * The string should represent a valid sheet structure with row keys, column keys, and values.
     *
     * @param str the JSON string to parse
     * @return the parsed Sheet object, or {@code null} if the input string is {@code null} or empty
     * @throws IllegalArgumentException if the string cannot be parsed as a valid Sheet structure
     */
    @MayReturnNull
    @SuppressWarnings("rawtypes")
    @Override
    public Sheet valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, typeClass);
    }

    /**
     * Generates the type name for a Sheet with the specified parameter types.
     * This method supports both simple class names (for declaring names) and
     * canonical class names (for full type names).
     *
     * @param typeClass the Sheet class
     * @param rowKeyTypeName the type name for row keys
     * @param columnKeyTypeName the type name for column keys
     * @param elementTypeName the type name for values/elements
     * @param isDeclaringName whether to use declaring names (true) or full names (false)
     * @return the formatted type name string
     */
    protected static String getTypeName(final Class<?> typeClass, final String rowKeyTypeName, final String columnKeyTypeName, final String elementTypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).declaringName() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).declaringName()
                    + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(rowKeyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(elementTypeName).name() + WD.GREATER_THAN;

        }
    }
}
