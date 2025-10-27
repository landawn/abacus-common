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
 * Type handler for standard Boolean operations.
 * This class provides handling for java.lang.Boolean values in database operations,
 * with automatic type conversion for non-Boolean database values.
 */
public final class BooleanType extends AbstractBooleanType {

    /**
     * The type name constant for Boolean type identification.
     */
    public static final String BOOLEAN = Boolean.class.getSimpleName();

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
     * Determines whether this type represents a primitive wrapper class.
     * Boolean is the wrapper class for the primitive boolean type.
     *
     * @return {@code true} indicating Boolean is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Boolean value from a ResultSet at the specified column index.
     * This method handles type conversion for non-Boolean database columns,
     * automatically converting numeric and string values to Boolean.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the value
     * @return the Boolean value, {@code null} if SQL NULL, or converted value if not Boolean type
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null || ret instanceof Boolean) {
            return (Boolean) ret;
        } else {
            return N.convert(ret, Boolean.class);
        }
    }

    /**
     * Retrieves a Boolean value from a ResultSet using the specified column label.
     * This method handles type conversion for non-Boolean database columns,
     * automatically converting numeric and string values to Boolean.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return the Boolean value, {@code null} if SQL NULL, or converted value if not Boolean type
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null || ret instanceof Boolean) {
            return (Boolean) ret;
        } else {
            return N.convert(ret, Boolean.class);
        }
    }
}
