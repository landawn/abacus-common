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
 * Type handler for {@link Character} (wrapper class) values.
 * This class provides database operations and type information for {@link Character} objects.
 *
 * <p>When reading from a database, the column value is retrieved as a {@link String} and the first
 * character of that string is returned. An empty or {@code NULL} column value yields {@code null}.</p>
 *
 * @see AbstractCharacterType
 */
public final class CharacterType extends AbstractCharacterType {

    /**
     * The type name constant for Character type identification, equal to {@code "Character"}.
     */
    public static final String CHARACTER = Character.class.getSimpleName();

    /**
     * Package-private constructor for {@code CharacterType}.
     * Instances are created by the {@code TypeFactory}.
     */
    CharacterType() {
        super(CHARACTER);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Character.class}
     */
    @Override
    public Class<Character> javaType() {
        return Character.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * {@link Character} is the wrapper for the primitive {@code char} type.
     *
     * @return {@code true}, always, because {@link Character} is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves a {@link Character} value from a {@link ResultSet} at the specified column index.
     * The column is read as a {@link String}; the first character of that string is returned.
     *
     * @param rs          the {@link ResultSet} to read from; must not be {@code null}
     * @param columnIndex the 1-based column index
     * @return the first character of the column's string value,
     *         or {@code null} if the column value is SQL {@code NULL} or an empty string
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
     * Retrieves a {@link Character} value from a {@link ResultSet} using the specified column label.
     * The column is read as a {@link String}; the first character of that string is returned.
     *
     * @param rs         the {@link ResultSet} to read from; must not be {@code null}
     * @param columnName the label of the column to retrieve
     * @return the first character of the column's string value,
     *         or {@code null} if the column value is SQL {@code NULL} or an empty string
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(final ResultSet rs, final String columnName) throws SQLException {
        final String result = rs.getString(columnName);

        if (result == null || result.isEmpty()) {
            return null; // NOSONAR
        } else {
            return result.charAt(0);
        }
    }
}
