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
 * Type handler for Float wrapper type.
 * This class provides functionality to handle Float objects in database operations and type conversions.
 * It extends AbstractFloatType to inherit common float type handling behavior.
 */
public final class FloatType extends AbstractFloatType {

    public static final String FLOAT = Float.class.getSimpleName();

    FloatType() {
        super(FLOAT);
    }

    /**
     * Returns the Class object representing the Float wrapper type.
     *
     * @return Float.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Float.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * For FloatType, this always returns {@code true} as Float is the wrapper class for the primitive float type.
     *
     * @return true, indicating Float is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Float value from the specified column in a ResultSet.
     * This method handles null values and type conversions from the database.
     * If the column value is null, returns null.
     * If the value is already a Float, returns it directly.
     * Otherwise, converts the value to Float using appropriate conversion logic.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Float value from the column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @MayReturnNull
    @Override
    public Float get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Float) {
            return (Float) ret;
        } else {
            return Numbers.toFloat(ret);
        }
    }

    /**
     * Retrieves a Float value from the specified column in a ResultSet using the column label.
     * This method handles null values and type conversions from the database.
     * If the column value is null, returns null.
     * If the value is already a Float, returns it directly.
     * Otherwise, converts the value to Float using appropriate conversion logic.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return the Float value from the column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @MayReturnNull
    @Override
    public Float get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Float) {
            return (Float) ret;
        } else {
            return Numbers.toFloat(ret);
        }
    }
}