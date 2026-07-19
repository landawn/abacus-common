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
 * Type handler for {@link Double} (wrapper class) values.
 * This class provides database read operations and type information for {@link Double} objects.
 *
 * <p>When reading from a database, the column value is retrieved via
 * {@link java.sql.ResultSet#getObject(int) ResultSet.getObject} to preserve SQL {@code NULL}:
 * a {@code null} result returns {@code null}, a {@code Double} result is returned directly, and
 * any other numeric type is converted via {@link com.landawn.abacus.util.Numbers#toDouble(Object)}.
 *
 * @see AbstractDoubleType
 */
public final class DoubleType extends AbstractDoubleType {

    /**
     * The type name constant for Double type identification, equal to {@code "Double"}.
     */
    public static final String DOUBLE = Double.class.getSimpleName();

    /**
     * Package-private constructor for {@code DoubleType}.
     * Instances are created by the {@code TypeFactory}.
     */
    DoubleType() {
        super(DOUBLE);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Double.class}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return Double.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * {@link Double} is the wrapper for the primitive {@code double} type.
     *
     * @return {@code true}, always, because {@link Double} is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a {@link Double} value from a {@link java.sql.ResultSet} at the specified column index.
     * The column is read via {@link java.sql.ResultSet#getObject(int)} to preserve SQL {@code NULL}.
     * If the returned object is already a {@link Double} it is returned directly; any other numeric
     * type is converted via {@link com.landawn.abacus.util.Numbers#toDouble(Object)}.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnIndex the 1-based column index
     * @return the converted {@code Double} value
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws NumberFormatException if a non-numeric value cannot be converted to {@code double}
     */
    @Override
    public Double get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Double) {
            return (Double) result;
        } else {
            return Numbers.toDouble(result);
        }
    }

    /**
     * Retrieves a {@link Double} value from a {@link java.sql.ResultSet} using the specified column label.
     * The column is read via {@link java.sql.ResultSet#getObject(String)} to preserve SQL {@code NULL}.
     * If the returned object is already a {@link Double} it is returned directly; any other numeric
     * type is converted via {@link com.landawn.abacus.util.Numbers#toDouble(Object)}.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnName the label of the column to retrieve
     * @return the converted {@code Double} value
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     * @throws NumberFormatException if a non-numeric value cannot be converted to {@code double}
     */
    @Override
    public Double get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Double) {
            return (Double) result;
        } else {
            return Numbers.toDouble(result);
        }
    }
}
