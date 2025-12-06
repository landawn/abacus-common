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
 * Type handler for Byte (wrapper class) values.
 * This class provides database operations and type information for Byte objects.
 * It handles the conversion between database values and Java Byte objects, supporting {@code null} values.
 */
public final class ByteType extends AbstractByteType {

    /**
     * The type name constant for Byte type identification.
     */
    public static final String BYTE = Byte.class.getSimpleName();

    /**
     * Package-private constructor for ByteType.
     * This constructor is called by the TypeFactory to create Byte type instances.
     */
    ByteType() {
        super(BYTE);
    }

    /**
     * Returns the Class object representing the Byte class.
     *
     * @return the Class object for Byte.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Byte.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Byte is the wrapper class for the primitive byte type.
     *
     * @return {@code true}, indicating Byte is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Byte value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Byte.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteType type = TypeFactory.getType(Byte.class);
     * ResultSet rs = ...;  // from SQL query
     * Byte status = type.get(rs, 1);   // retrieves Byte from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the Byte value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Byte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) { // NOSONAR
            return null; // NOSONAR
        } else if (ret instanceof Number) {
            return ((Number) ret).byteValue();
        } else {
            return Numbers.toByte(ret.toString());
        }
    }

    /**
     * Retrieves a Byte value from a ResultSet using the specified column label.
     * This method handles various numeric types in the database and converts them to Byte.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteType type = TypeFactory.getType(Byte.class);
     * ResultSet rs = ...;  // from SQL query
     * Byte status = type.get(rs, "status");   // retrieves Byte from "status" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the Byte value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Byte get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Number) {
            return ((Number) ret).byteValue();
        } else {
            return Numbers.toByte(ret.toString());
        }
    }
}
