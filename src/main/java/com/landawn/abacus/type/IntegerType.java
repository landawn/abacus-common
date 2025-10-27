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
 * Type handler for Integer wrapper type.
 * This class provides functionality to handle Integer objects in database operations and type conversions.
 * It extends AbstractIntegerType to inherit common integer type handling behavior.
 */
public final class IntegerType extends AbstractIntegerType {

    public static final String INTEGER = Integer.class.getSimpleName();

    IntegerType() {
        super(INTEGER);
    }

    /**
     * Returns the Class object representing the Integer wrapper type.
     *
     * @return Integer.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Integer.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * For IntegerType, this always returns {@code true} as Integer is the wrapper class for the primitive int type.
     *
     * @return {@code true}, indicating Integer is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves an Integer value from the specified column in a ResultSet.
     * This method handles {@code null} values and type conversions from the database.
     * If the column value is {@code null}, returns {@code null}.
     * If the value is already an Integer, returns it directly.
     * If the value is another Number type, converts it to Integer.
     * Otherwise, parses the string representation of the value.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Integer value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @MayReturnNull
    @Override

    public Integer get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Integer) {
            return (Integer) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).intValue();
        } else {
            return Numbers.toInt(ret.toString());
        }
    }

    /**
     * Retrieves an Integer value from the specified column in a ResultSet using the column label.
     * This method handles {@code null} values and type conversions from the database.
     * If the column value is {@code null}, returns {@code null}.
     * If the value is already an Integer, returns it directly.
     * If the value is another Number type, converts it to Integer.
     * Otherwise, parses the string representation of the value.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return the Integer value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @MayReturnNull
    @Override

    public Integer get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Integer) {
            return (Integer) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).intValue();
        } else {
            return Numbers.toInt(ret.toString());
        }
    }
}
