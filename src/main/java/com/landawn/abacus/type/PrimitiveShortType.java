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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Numbers;

@SuppressWarnings("java:S2160")
public final class PrimitiveShortType extends AbstractShortType {

    public static final String SHORT = short.class.getSimpleName();

    private static final Short DEFAULT_VALUE = 0;

    PrimitiveShortType() {
        super(SHORT);
    }

    /**
     * Returns the Class object representing the primitive short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * Class clazz = type.clazz();
     * System.out.println(clazz.getName());       // Output: short
     * System.out.println(clazz.isPrimitive());   // Output: true
     * }</pre>
     *
     * @return the Class object for short.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return short.class;
    }

    /**
     * Indicates whether this type represents a primitive type.
     * For PrimitiveShortType, this always returns {@code true} since it represents the primitive short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * boolean isPrimitive = type.isPrimitiveType();
     * System.out.println(isPrimitive);  // Output: true
     * }</pre>
     *
     * @return {@code true}, indicating this is a primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    /**
     * Returns the default value for the primitive short type.
     * The default value for primitive short is 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * Short defaultVal = type.defaultValue();
     * System.out.println(defaultVal);  // Output: 0
     * }</pre>
     *
     * @return Short object containing the value 0
     */
    @Override
    public Short defaultValue() {
        return DEFAULT_VALUE;
    }

    /**
     * Retrieves a short value from the specified column in the ResultSet.
     * This method handles various data types that can be converted to short,
     * including direct short values, other numeric types, and string representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * // Assuming rs is a ResultSet with a short value in column 1
     * Short value = type.get(rs, 1);
     * System.out.println(value);  // Output: the short value from the database
     *
     * // If the column contains NULL
     * Short nullValue = type.get(rs, 2);
     * System.out.println(nullValue);  // Output: null
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the short value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     * @throws NumberFormatException if the column value is a string that cannot be parsed as a short
     */
    @Override
    public Short get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Short) {
            return (Short) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).shortValue();
        } else {
            return Numbers.toShort(ret.toString());
        }
    }

    /**
     * Retrieves a short value from the specified column in the ResultSet.
     * This method handles various data types that can be converted to short,
     * including direct short values, other numeric types, and string representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(short.class);
     * // Assuming rs is a ResultSet with a short value in column "age"
     * Short age = type.get(rs, "age");
     * System.out.println(age);  // Output: the short value from the database
     *
     * // If the column contains NULL
     * Short nullValue = type.get(rs, "missing_column");
     * System.out.println(nullValue);  // Output: null
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the short value from the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     * @throws NumberFormatException if the column value is a string that cannot be parsed as a short
     */
    @Override
    public Short get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Short) {
            return (Short) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).shortValue();
        } else {
            return Numbers.toShort(ret.toString());
        }
    }
}
