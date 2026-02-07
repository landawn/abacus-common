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

/**
 * Type handler for Short wrapper type.
 * This class provides functionality to handle Short objects in database operations and type conversions.
 * It extends AbstractShortType to inherit common short type handling behavior.
 */
public final class ShortType extends AbstractShortType {

    /**
     * The type name constant for Short type identification.
     */
    public static final String SHORT = Short.class.getSimpleName();

    /**
     * Package-private constructor for ShortType.
     * This constructor is called by the TypeFactory to create Short type instances.
     */
    ShortType() {
        super(SHORT);
    }

    /**
     * Returns the Class object representing the Short class.
     *
     * @return the Class object for Short.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Short.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Short is the wrapper class for the primitive short type.
     *
     * @return {@code true}, indicating Short is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Short value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Short.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(Short.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Short value = type.get(rs, 1);   // retrieves Short from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Short value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Short get(ResultSet rs, int columnIndex) throws SQLException {
        return super.get(rs, columnIndex);
    }

    /**
     * Retrieves a Short value from a ResultSet using the specified column label.
     * This method handles various numeric types in the database and converts them to Short.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(Short.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Short value = type.get(rs, "value");   // retrieves Short from "value" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnName the label of the column to retrieve the value from, must not be {@code null}
     * @return the Short value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Short get(ResultSet rs, String columnName) throws SQLException {
        return super.get(rs, columnName);
    }
}