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
 * Type handler for Integer wrapper type.
 * This class provides functionality to handle Integer objects in database operations and type conversions.
 * It extends AbstractIntegerType to inherit common integer type handling behavior.
 */
final class IntegerType extends AbstractIntegerType {

    public static final String INTEGER = Integer.class.getSimpleName();

    IntegerType() {
        super(INTEGER);
    }

    /**
     * Returns the Class object representing the Integer class.
     *
     * @return the Class object for Integer.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Integer.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Integer is the wrapper class for the primitive int type.
     *
     * @return {@code true}, indicating Integer is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves an Integer value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Integer> type = TypeFactory.getType(Integer.class);
     * ResultSet rs = ...; // from SQL query
     * Integer age = type.get(rs, 1); // retrieves Integer from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Integer value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
     * Retrieves an Integer value from a ResultSet using the specified column label.
     * This method handles various numeric types in the database and converts them to Integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Integer> type = TypeFactory.getType(Integer.class);
     * ResultSet rs = ...; // from SQL query
     * Integer age = type.get(rs, "age"); // retrieves Integer from "age" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Integer value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
