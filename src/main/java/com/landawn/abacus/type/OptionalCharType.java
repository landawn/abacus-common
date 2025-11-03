/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalChar;

/**
 * Type handler for {@link OptionalChar} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional character values. This handler manages
 * the conversion between database character/integer values and OptionalChar wrapper objects.
 */
class OptionalCharType extends AbstractOptionalType<OptionalChar> {

    public static final String OPTIONAL_CHAR = OptionalChar.class.getSimpleName();

    protected OptionalCharType() {
        super(OPTIONAL_CHAR);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalChar} class object
     */
    @Override
    public Class<OptionalChar> clazz() {
        return OptionalChar.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalChar values support comparison operations.
     *
     * @return {@code true}, as OptionalChar values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Returns the default value for OptionalChar type, which is an empty OptionalChar.
     *
     * @return OptionalChar.empty()
     */
    @Override
    public OptionalChar defaultValue() {
        return OptionalChar.empty();
    }

    /**
     * Converts an {@link OptionalChar} object to its string representation.
     *
     * @param x the OptionalChar object to convert
     * @return a single-character string, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalChar x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalChar} object.
     * The string should contain exactly one character or be convertible to a character.
     *
     * @param str the string to convert
     * @return an OptionalChar containing the parsed character value, or empty if the input is empty or null
     * @throws IllegalArgumentException if the string cannot be parsed as a single character
     */
    @Override
    public OptionalChar valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(str));
    }

    /**
     * Retrieves a character value from a ResultSet at the specified column index and wraps it in an {@link OptionalChar}.
     * Handles multiple data types: Character objects, Integer values (converted to char), and strings.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalChar containing the character value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        if (obj instanceof Character) {
            return OptionalChar.of((Character) obj);
        } else if (obj instanceof Integer) {
            return OptionalChar.of((char) ((Integer) obj).intValue());
        } else {
            return obj == null ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(obj.toString()));
        }
    }

    /**
     * Retrieves a character value from a ResultSet using the specified column label and wraps it in an {@link OptionalChar}.
     * Handles multiple data types: Character objects, Integer values (converted to char), and strings.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalChar containing the character value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalChar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        if (obj instanceof Character) {
            return OptionalChar.of((Character) obj);
        } else if (obj instanceof Integer) {
            return OptionalChar.of((char) ((Integer) obj).intValue());
        } else {
            return obj == null ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(obj.toString()));
        }
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalChar}.
     * Characters are stored as integers in the database. If the OptionalChar is {@code null} or empty,
     * sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalChar value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalChar x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.CHAR);
        } else {
            stmt.setInt(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalChar}.
     * Characters are stored as integers in the database. If the OptionalChar is {@code null} or empty,
     * sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalChar value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalChar x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.CHAR);
        } else {
            stmt.setInt(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalChar} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalChar value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalChar x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.get());
        }
    }

    /**
     * Writes the character representation of an {@link OptionalChar} to a CharacterWriter.
     * Optionally quotes the character based on the serialization configuration.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalChar value to write
     * @param config the serialization configuration specifying character quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalChar x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x.get());
            } else {
                writer.write(ch);
                writer.writeCharacter(x.get());
                writer.write(ch);
            }
        }
    }
}
