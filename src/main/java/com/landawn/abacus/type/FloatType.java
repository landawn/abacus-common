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
 * Type handler for Float wrapper type.
 * This class provides functionality to handle Float objects in database operations and type conversions.
 * It extends AbstractFloatType to inherit common float type handling behavior.
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
     * Returns the Class object representing the Float class.
     *
     * @return the Class object for Float.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Float.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Float is the wrapper class for the primitive float type.
     *
     * @return {@code true}, indicating Float is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Float value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Float.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Float> type = TypeFactory.getType(Float.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Float temperature = type.get(rs, 1);   // retrieves Float from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Float value at the specified column, or {@code null} if the column value is SQL NULL
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
     * Retrieves a Float value from a ResultSet using the specified column label.
     * This method handles various numeric types in the database and converts them to Float.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Float> type = TypeFactory.getType(Float.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Float temperature = type.get(rs, "temperature");   // retrieves Float from "temperature" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Float value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Float get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object result = rs.getObject(columnLabel);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Float) {
            return (Float) result;
        } else {
            return Numbers.toFloat(result);
        }
    }
}