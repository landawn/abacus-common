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
 * Type handler for {@link Byte} (boxed wrapper) values.
 * Provides JDBC read operations for {@code Byte} objects, supporting {@code null} values
 * (SQL NULL maps to Java {@code null}).
 *
 * <p>Retrieval reads the column as a generic {@link Object} via
 * {@link java.sql.ResultSet#getObject(int)}: {@link Number} values are narrowed via
 * {@link Number#byteValue()}, and any other (non-{@link Number}) value is parsed from its
 * string form using {@link com.landawn.abacus.util.Numbers#toByte(String)}.</p>
 *
 * <p>String serialization and JDBC write operations are inherited from
 * {@link AbstractByteType}.</p>
 *
 * @see AbstractByteType
 */
public final class ByteType extends AbstractByteType {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "Byte"}).
     */
    public static final String BYTE = Byte.class.getSimpleName();

    /**
     * Package-private constructor for {@code ByteType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    ByteType() {
        super(BYTE);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Byte.class}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return Byte.class;
    }

    /**
     * Indicates that {@link Byte} is the wrapper class for the primitive {@code byte} type.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a {@link Byte} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read via {@link java.sql.ResultSet#getObject(int)}: if the result is
     * a {@link Number}, it is narrowed via {@link Number#byteValue()}; for any other (non-{@link Number})
     * value, its string representation is parsed using
     * {@link com.landawn.abacus.util.Numbers#toByte(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the byte value
     * @return the {@code Byte} value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     * @throws NumberFormatException if a non-numeric string value cannot be parsed as a {@code byte}
     */
    @Override
    public Byte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        if (result == null) { // NOSONAR
            return null; // NOSONAR
        } else if (result instanceof Number) {
            return ((Number) result).byteValue();
        } else {
            return Numbers.toByte(result.toString());
        }
    }

    /**
     * Retrieves a {@link Byte} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read via {@link java.sql.ResultSet#getObject(String)}: if the result is
     * a {@link Number}, it is narrowed via {@link Number#byteValue()}; for any other (non-{@link Number})
     * value, its string representation is parsed using
     * {@link com.landawn.abacus.util.Numbers#toByte(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code Byte} value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     * @throws NumberFormatException if a non-numeric string value cannot be parsed as a {@code byte}
     */
    @Override
    public Byte get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Number) {
            return ((Number) result).byteValue();
        } else {
            return Numbers.toByte(result.toString());
        }
    }
}
