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
 * Type handler for Double (wrapper class) values.
 * This class provides database operations and type information for Double objects.
 * It handles the conversion between database values and Java Double objects, supporting {@code null} values.
 */
public final class DoubleType extends AbstractDoubleType {

    /**
     * The type name constant for Double type identification.
     */
    public static final String DOUBLE = Double.class.getSimpleName();
    /**
     * Package-private constructor for DoubleType.
     * This constructor is called by the TypeFactory to create Double type instances.
     */
    DoubleType() {
        super(DOUBLE);
    }

    /**
     * Returns the Class object representing the Double class.
     *
     * @return the Class object for Double.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Double.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Double is the wrapper class for the primitive double type.
     *
     * @return {@code true}, indicating Double is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Double value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Double.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Double> type = TypeFactory.getType(Double.class);
     * ResultSet rs = ...;  // from SQL query
     * Double price = type.get(rs, 1);   // retrieves Double from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Double value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Double> type = TypeFactory.getType(Double.class);
     * ResultSet rs = ...;  // from SQL query
     * Double price = type.get(rs, "price");   // retrieves Double from "price" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Double value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
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
