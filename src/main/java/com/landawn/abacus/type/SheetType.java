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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Sheet}, which represents a two-dimensional table structure with row keys,
 * column keys, and cell values. This class handles JSON-based serialization and deserialization of
 * Sheet instances. Although {@link #stringOf(Sheet)} and {@link #valueOf(String)} provide an explicit JSON
 * round-trip, {@link #isSerializable()} returns {@code false} so general serializers use the structured
 * {@link SerializationType#SHEET} path instead of treating a sheet as a scalar string value.
 *
 * @param <R> the row key type
 * @param <C> the column key type
 * @param <E> the element/value type
 */
@SuppressWarnings("java:S2160")
public class SheetType<R, C, E> extends AbstractType<Sheet<R, C, E>> {
    private final String declaringName;

    private final Class<Sheet<R, C, E>> typeClass;

    private final List<Type<?>> parameterTypes;

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
        parameterTypes = List.of(TypeFactory.getType(rowKeyTypeName), TypeFactory.getType(columnKeyTypeName), TypeFactory.getType(elementTypeName));
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
     * Class<Sheet<String, String, Object>> clazz = type.javaType();
     * // clazz represents Sheet.class
     * }</pre>
     *
     * @return the Class object for Sheet.class
     */
    @Override
    public Class<Sheet<R, C, E>> javaType() {
        return typeClass;
    }

    /**
     * Returns an immutable list containing the Type instances for the parameter types of this Sheet.
     * The list contains three elements: row key type, column key type, and element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, Integer, Double> type = new SheetType<>("String", "Integer", "Double");
     * List<Type<?>> paramTypes = type.parameterTypes();
     * // paramTypes.get(0) is Type for String (row keys)
     * // paramTypes.get(1) is Type for Integer (column keys)
     * // paramTypes.get(2) is Type for Double (elements)
     * }</pre>
     *
     * @return an immutable list with Type instances for row key, column key, and element types
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a parameterized type.
     * SheetType is always parameterized as it has three type parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Object> type = new SheetType<>("String", "String", "Object");
     * boolean isParameterized = type.isParameterizedType();
     * // isParameterized is true
     * }</pre>
     *
     * @return {@code true}, indicating this is a parameterized type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether this type is directly serializable to a primitive string representation.
     * {@code SheetType} is not directly serializable; it uses the specialized
     * {@link SerializationType#SHEET} serialization path instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Object> type = new SheetType<>("String", "String", "Object");
     * boolean serializable = type.isSerializable();
     * // serializable is false
     * }</pre>
     *
     * @return {@code false}, indicating this type uses specialized sheet-based serialization
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
     * SerializationType serType = type.serializationType();   // Returns SerializationType.SHEET
     * }</pre>
     *
     * @return {@link SerializationType#SHEET}, indicating this type uses sheet-based serialization
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.SHEET;
    }

    /**
     * Converts a Sheet to its JSON string representation.
     * The sheet structure is serialized maintaining the row-column-value relationships.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * Sheet<String, String, Integer> sheet = Sheet.rows(List.of("row1"), List.of("col1", "col2"),
     *         new Integer[][] {{100, 200}});
     * String json = type.stringOf(sheet);
     * // Returns JSON representation of the sheet
     *
     * String nullStr = type.stringOf(null);
     * // Returns null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the Sheet to convert to string
     * @return the JSON string representation of the sheet, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Sheet<R, C, E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string representation and returns the corresponding Sheet object.
     * The string should represent a valid sheet structure with row keys, column keys, and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetType<String, String, Integer> type = new SheetType<>("String", "String", "Integer");
     * String json = "{\"rowKeySet\": [\"row1\"], \"columnKeySet\": [\"col1\", \"col2\"], \"columns\": {\"col1\": [100], \"col2\": [200]}}";
     * Sheet<String, String, Integer> sheet = type.valueOf(json);
     * // sheet contains the parsed structure
     *
     * Sheet<String, String, Integer> nullSheet = type.valueOf(null);
     * // nullSheet is null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse
     * @return the parsed Sheet object with its declared row, column, and element types preserved,
     *         or {@code null} if the input string is {@code null} or blank
     * @see #valueOf(Object)
     * @see #stringOf(Sheet)
     */
    @Override
    public Sheet<R, C, E> valueOf(final String str) {
        return Strings.isBlank(str) ? null : Utils.jsonParser.deserialize(str, this);
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
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(rowKeyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).declaringName() + SK.COMMA_SPACE + TypeFactory.getType(elementTypeName).declaringName()
                    + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(rowKeyTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(columnKeyTypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(elementTypeName).name() + SK.GREATER_THAN;

        }
    }
}
