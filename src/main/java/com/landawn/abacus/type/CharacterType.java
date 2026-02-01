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
 * Type handler for Character (wrapper class) values.
 * This class provides database operations and type information for Character objects.
 * It handles the conversion between database string values and Java Character objects,
 * extracting the first character from string values when available.
 */
public final class CharacterType extends AbstractCharacterType {

    /**
     * The type name constant for Character type identification.
     */
    public static final String CHARACTER = Character.class.getSimpleName();

    /**
     * Package-private constructor for CharacterType.
     * This constructor is called by the TypeFactory to create Character type instances.
     */

    CharacterType() {
        super(CHARACTER);
    }

    /**
     * Returns the Class object representing the Character class.
     *
     * @return the Class object for Character.class
     */
    @Override
    public Class<Character> clazz() {
        return Character.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * Character is the wrapper class for the primitive char type.
     *
     * @return {@code true}, indicating Character is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a Character value from a ResultSet at the specified column index.
     * The method reads the value as a String and returns the first character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Character> type = TypeFactory.getType(Character.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Character initial = type.get(rs, 1);   // retrieves Character from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the first character of the string value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String result = rs.getString(columnIndex);

        if (result == null || result.isEmpty()) {
            return null; // NOSONAR
        } else {
            return result.charAt(0);
        }
    }

    /**
     * Retrieves a Character value from a ResultSet using the specified column label.
     * The method reads the value as a String and returns the first character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Character> type = TypeFactory.getType(Character.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Character initial = type.get(rs, "initial");   // retrieves Character from "initial" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnLabel the label of the column to retrieve the value from, must not be {@code null}
     * @return the first character of the string value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(final ResultSet rs, final String columnLabel) throws SQLException {
        final String result = rs.getString(columnLabel);

        if (result == null || result.isEmpty()) {
            return null; // NOSONAR
        } else {
            return result.charAt(0);
        }
    }
}