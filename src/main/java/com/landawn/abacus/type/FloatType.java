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

/**
 * Type handler for {@link Float} (wrapper class) values.
 * This class provides database read operations and type information for {@link Float} objects.
 *
 * <p>When reading from a database, the column value is retrieved via
 * {@link java.sql.ResultSet#getObject(int) ResultSet.getObject} to preserve SQL {@code NULL}:
 * a {@code null} result returns {@code null}, a {@code Float} result is returned directly, and
 * any other numeric type is converted via {@link com.landawn.abacus.util.Numbers#toFloat(Object)}.
 *
 * @see AbstractFloatType
 */
public final class FloatType extends AbstractFloatType {

    /**
     * The type name constant for Float type identification.
     */
    public static final String FLOAT = Float.class.getSimpleName();

    /**
     * Package-private constructor for FloatType.
     * This constructor is called by the TypeFactory to create Float type instances.
     */
    FloatType() {
        super(FLOAT);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Float.class}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return Float.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * {@link Float} is the wrapper for the primitive {@code float} type.
     *
     * @return {@code true}, always, because {@link Float} is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a {@link Float} value from a {@link java.sql.ResultSet} at the specified column index.
     * The column is read via {@link java.sql.ResultSet#getObject(int)} to preserve SQL {@code NULL}.
     * If the returned object is already a {@link Float} it is returned directly; any other numeric
     * type is converted via {@link com.landawn.abacus.util.Numbers#toFloat(Object)}.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnIndex the 1-based column index
     * @return the {@link Float} value at the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Float get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Float) {
            return (Float) result;
        } else {
            return Numbers.toFloat(result);
        }
    }

    /**
     * Retrieves a {@link Float} value from a {@link java.sql.ResultSet} using the specified column label.
     * The column is read via {@link java.sql.ResultSet#getObject(String)} to preserve SQL {@code NULL}.
     * If the returned object is already a {@link Float} it is returned directly; any other numeric
     * type is converted via {@link com.landawn.abacus.util.Numbers#toFloat(Object)}.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnName the label of the column to retrieve
     * @return the {@link Float} value in the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Float get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Float) {
            return (Float) result;
        } else {
            return Numbers.toFloat(result);
        }
    }
}
