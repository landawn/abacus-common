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
     * Returns the Class object representing the Long class.
     *
     * @return the Class object for Long.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Long.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Long is the wrapper class for the primitive long type.
     *
     * @return {@code true}, indicating Long is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Long value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Long.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * ResultSet rs = ...;  // from SQL query
     * Long userId = type.get(rs, 1);  // retrieves Long from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Long value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
     * This method handles various numeric types in the database and converts them to Long.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * ResultSet rs = ...;  // from SQL query
     * Long userId = type.get(rs, "user_id");  // retrieves Long from "user_id" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Long value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
