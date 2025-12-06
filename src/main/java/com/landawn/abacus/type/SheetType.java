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
     * This constructor initializes a parameterized Sheet type handler for a specific combination
     * of row key, column key, and element types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a SheetType for Sheet<String, String, Integer>
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * String declName = type.declaringName();
     * // Returns: "Sheet<String, String, Integer>"
     * }</pre>
     *
     * @return the declaring name in the format "Sheet&lt;RowType, ColumnType, ElementType&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Sheet type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Object> type = new SheetType<>("String", "String", "Object");
     * Class<Sheet<String, String, Object>> clazz = type.clazz();
     * // clazz represents Sheet.class
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, Integer, Double> type = new SheetType<>("String", "Integer", "Double");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // paramTypes[0] is Type for String (row keys)
     * // paramTypes[1] is Type for Integer (column keys)
     * // paramTypes[2] is Type for Double (elements)
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Object> type = new SheetType<>("String", "String", "Object");
     * boolean isGeneric = type.isGenericType();
     * // isGeneric is true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Object> type = new SheetType<>("String", "String", "Object");
     * boolean serializable = type.isSerializable();
     * // serializable is false
     * }</pre>
     *
     * @return {@code false}, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type classification for Sheet objects.
     * This identifies the type as SHEET, which indicates specialized sheet-based serialization handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Sheet<String, String, Object>> type = TypeFactory.getType("Sheet<String, String, Object>");
     * SerializationType serType = type.getSerializationType();   // Returns SerializationType.SHEET
     * }</pre>
     *
     * @return {@link SerializationType#SHEET}, indicating this type uses sheet-based serialization
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.SHEET;
    }

    /**
     * Converts a Sheet to its JSON string representation.
     * The sheet structure is serialized maintaining the row-column-value relationships.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * Sheet<String, String, Integer> sheet = new Sheet<>();
     * sheet.put("row1", "col1", 100);
     * sheet.put("row1", "col2", 200);
     * String json = type.stringOf(sheet);
     * // Returns JSON representation of the sheet
     *
     * String nullStr = type.stringOf(null);
     * // Returns null
     * }</pre>
     *
     * @param x the Sheet to convert to string
     * @return the JSON string representation of the sheet, or {@code null} if the input is null
     */
    @SuppressWarnings("rawtypes")
    @Override
    public String stringOf(final Sheet x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string representation and returns the corresponding Sheet object.
     * The string should represent a valid sheet structure with row keys, column keys, and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * String json = "{\"row1\":{\"col1\":100,\"col2\":200}}";
     * Sheet<String, String, Integer> sheet = type.valueOf(json);
     * // sheet contains the parsed structure
     *
     * Sheet<String, String, Integer> nullSheet = type.valueOf(null);
     * // nullSheet is null
     * }</pre>
     *
     * @param str the JSON string to parse
     * @return the parsed Sheet object, or {@code null} if the input string is {@code null} or empty
     * @throws IllegalArgumentException if the string cannot be parsed as a valid Sheet structure
     */
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
