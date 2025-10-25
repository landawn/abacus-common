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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for Double (wrapper class) values.
 * This class provides database operations and type information for Double objects.
 * It handles the conversion between database values and Java Double objects, supporting null values.
 */
public final class DoubleType extends AbstractDoubleType {

    public static final String DOUBLE = Double.class.getSimpleName();

    DoubleType() {
        super(DOUBLE);
    }

    /**
     * Returns the Java class type handled by this type handler.
     * Note: The method uses raw types for compatibility reasons.
     *
     * @return The Class object representing Double.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Double.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Since this handles the Double wrapper class (not the primitive double), this returns true.
     *
     * @return true, indicating this is a primitive wrapper type
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Double value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Double.
     * If the database value is NULL, this method returns null.
     * For optimal performance, if the value is already a Double, it's returned directly.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the double value
     * @return The Double value at the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @MayReturnNull
    @Override
    public Double get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Double) {
            return (Double) ret;
        } else {
            return Numbers.toDouble(ret);
        }
    }

    /**
     * Retrieves a Double value from a ResultSet using the specified column label.
     * This method handles various numeric types in the database and converts them to Double.
     * If the database value is NULL, this method returns null.
     * For optimal performance, if the value is already a Double, it's returned directly.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the double value
     * @return The Double value in the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @MayReturnNull
    @Override
    public Double get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Double) {
            return (Double) ret;
        } else {
            return Numbers.toDouble(ret);
        }
    }
}