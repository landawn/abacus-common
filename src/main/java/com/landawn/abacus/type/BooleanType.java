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
 * Type handler for {@link Boolean} (boxed wrapper) values.
 * Provides JDBC read operations for {@code Boolean} objects, supporting {@code null} values
 * (SQL NULL maps to Java {@code null}).
 *
 * <p>Retrieval is performed via {@link java.sql.ResultSet#getObject(int)}, and non-Boolean
 * database values (e.g. numeric types or strings) are automatically converted to {@code Boolean}
 * using {@link com.landawn.abacus.util.N#convert(Object, Class)}.</p>
 *
 * <p>String serialization and JDBC write operations are inherited from
 * {@link AbstractBooleanType}. The SQL type used for {@code null} writes is
 * {@link java.sql.Types#BOOLEAN}.</p>
 *
 * @see AbstractBooleanType
 */
public final class BooleanType extends AbstractBooleanType {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "Boolean"}).
     */
    public static final String BOOLEAN = Boolean.class.getSimpleName();

    /**
     * Package-private constructor for {@code BooleanType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BooleanType() {
        super(BOOLEAN);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Boolean.class}
     */
    @Override
    public Class<Boolean> javaType() {
        return Boolean.class;
    }

    /**
     * Indicates that {@link Boolean} is the wrapper class for the primitive {@code boolean} type.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read via {@link java.sql.ResultSet#getObject(int)}. If the result is
     * already a {@code Boolean} (or {@code null}), it is returned directly; otherwise it is
     * converted using {@link com.landawn.abacus.util.N#convert(Object, Class)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the boolean value
     * @return the {@code Boolean} value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
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
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read via {@link java.sql.ResultSet#getObject(String)}. If the result is
     * already a {@code Boolean} (or {@code null}), it is returned directly; otherwise it is
     * converted using {@link com.landawn.abacus.util.N#convert(Object, Class)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code Boolean} value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        if (result == null || result instanceof Boolean) {
            return (Boolean) result;
        } else {
            return N.convert(result, Boolean.class);
        }
    }
}
