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
 * Type handler for Long wrapper type.
 * This class provides functionality to handle Long objects in database operations and type conversions.
 * It extends AbstractLongType to inherit common long type handling behavior.
 */
public final class LongType extends AbstractLongType {

    public static final String LONG = Long.class.getSimpleName();

    LongType() {
        super(LONG);
    }

    /**
     * Returns the Class object representing the Long wrapper type.
     *
     * @return The Class object for Long
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Long.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * For LongType, this always returns {@code true} since Long is the wrapper class for the primitive long type.
     *
     * @return {@code true}, indicating that Long is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Long value from a ResultSet at the specified column index.
     * The method handles various data types that can be converted to Long:
     * - If the value is already a Long, it is returned directly
     * - If the value is any other Number type, it is converted to Long
     * - If the value is a String, it is parsed as a Long
     * - If the value is NULL in the database, {@code null} is returned
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The Long value from the ResultSet, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     * @throws NumberFormatException if the value is a String that cannot be parsed as a Long
     */
    @MayReturnNull
    @Override

    public Long get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Long) {
            return (Long) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).longValue();
        } else {
            return Numbers.toLong(ret.toString());
        }
    }

    /**
     * Retrieves a Long value from a ResultSet using the specified column label.
     * The method handles various data types that can be converted to Long:
     * - If the value is already a Long, it is returned directly
     * - If the value is any other Number type, it is converted to Long
     * - If the value is a String, it is parsed as a Long
     * - If the value is NULL in the database, {@code null} is returned
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return The Long value from the ResultSet, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     * @throws NumberFormatException if the value is a String that cannot be parsed as a Long
     */
    @MayReturnNull
    @Override

    public Long get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Long) {
            return (Long) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).longValue();
        } else {
            return Numbers.toLong(ret.toString());
        }
    }
}
