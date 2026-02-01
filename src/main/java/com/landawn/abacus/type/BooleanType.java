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

import com.landawn.abacus.util.N;

/**
 * Type handler for Boolean (wrapper class) values.
 * This class provides database operations and type information for Boolean objects.
 * It handles the conversion between database values and Java Boolean objects,
 * with automatic type conversion for non-Boolean database values, supporting {@code null} values.
 */
public final class BooleanType extends AbstractBooleanType {

    /**
     * The type name constant for Boolean type identification.
     */
    public static final String BOOLEAN = Boolean.class.getSimpleName();

    /**
     * Package-private constructor for BooleanType.
     * This constructor is called by the TypeFactory to create Boolean type instances.
     */
    BooleanType() {
        super(BOOLEAN);
    }

    /**
     * Returns the Class object representing the Boolean class.
     *
     * @return the Class object for Boolean.class
     */
    @Override
    public Class<Boolean> clazz() {
        return Boolean.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Boolean is the wrapper class for the primitive boolean type.
     *
     * @return {@code true}, indicating Boolean is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Boolean value from a ResultSet at the specified column index.
     * This method handles various data types in the database and converts them to Boolean,
     * automatically converting numeric and string values to Boolean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean> type = TypeFactory.getType(Boolean.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Boolean isActive = type.get(rs, 1);   // retrieves Boolean from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Boolean value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        if (result == null || result instanceof Boolean) {
            return (Boolean) result;
        } else {
            return N.convert(result, Boolean.class);
        }
    }

    /**
     * Retrieves a Boolean value from a ResultSet using the specified column label.
     * This method handles various data types in the database and converts them to Boolean,
     * automatically converting numeric and string values to Boolean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean> type = TypeFactory.getType(Boolean.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Boolean isActive = type.get(rs, "is_active");   // retrieves Boolean from "is_active" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Boolean value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object result = rs.getObject(columnLabel);

        if (result == null || result instanceof Boolean) {
            return (Boolean) result;
        } else {
            return N.convert(result, Boolean.class);
        }
    }
}