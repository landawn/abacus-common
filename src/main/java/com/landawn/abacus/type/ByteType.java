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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for Byte (wrapper class) values.
 * This class provides database operations and type information for Byte objects.
 * It handles the conversion between database values and Java Byte objects, supporting null values.
 */
public final class ByteType extends AbstractByteType {

    public static final String BYTE = Byte.class.getSimpleName();

    ByteType() {
        super(BYTE);
    }

    /**
     * Returns the Java class type handled by this type handler.
     * Note: The method uses raw types for compatibility reasons.
     *
     * @return The Class object representing Byte.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Byte.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Since this handles the Byte wrapper class (not the primitive byte), this returns true.
     *
     * @return true, indicating this is a primitive wrapper type
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Byte value from a ResultSet at the specified column index.
     * This method handles various numeric types in the database and converts them to Byte.
     * If the database value is NULL, this method returns null.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the byte value
     * @return The Byte value at the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @MayReturnNull
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
     * If the database value is NULL, this method returns null.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the byte value
     * @return The Byte value in the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @MayReturnNull
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