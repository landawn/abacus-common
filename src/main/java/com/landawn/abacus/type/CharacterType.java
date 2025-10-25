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

/**
 * Type handler for Character (wrapper class) values.
 * This class provides database operations and type information for Character objects.
 * It handles the conversion between database string values and Java Character objects,
 * extracting the first character from string values when available.
 */
public final class CharacterType extends AbstractCharacterType {

    public static final String CHARACTER = Character.class.getSimpleName();

    CharacterType() {
        super(CHARACTER);
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Character.class
     */
    @Override
    public Class<Character> clazz() {
        return Character.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Since this handles the Character wrapper class (not the primitive char), this returns true.
     *
     * @return true, indicating this is a primitive wrapper type
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Character value from a ResultSet at the specified column index.
     * The method reads the value as a String and returns the first character.
     * If the database value is NULL or an empty string, this method returns null.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the character value
     * @return The first character of the string value at the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @MayReturnNull
    @Override
    public Character get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String ret = rs.getString(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else {
            return ret.charAt(0);
        }
    }

    /**
     * Retrieves a Character value from a ResultSet using the specified column label.
     * The method reads the value as a String and returns the first character.
     * If the database value is NULL or an empty string, this method returns null.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the character value
     * @return The first character of the string value in the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @MayReturnNull
    @Override
    public Character get(final ResultSet rs, final String columnLabel) throws SQLException {
        final String ret = rs.getString(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else {
            return ret.charAt(0);
        }
    }
}