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
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class MutableCharType extends AbstractType<MutableChar> {

    public static final String MUTABLE_CHAR = MutableChar.class.getSimpleName();

    protected MutableCharType() {
        super(MUTABLE_CHAR);
    }

    /**
     * Returns the Class object representing the MutableChar type.
     *
     * @return The Class object for MutableChar
     */
    @Override
    public Class<MutableChar> clazz() {
        return MutableChar.class;
    }

    /**
     * Converts a MutableChar object to its string representation.
     * The character value is converted to a single-character string.
     *
     * @param x The MutableChar object to convert
     * @return The string representation of the character, or null if the input is null
     */
    @Override
    public String stringOf(final MutableChar x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableChar object.
     * The string is parsed to extract the first character.
     *
     * @param str The string to parse
     * @return A MutableChar containing the parsed character, or null if the input is null or empty
     */
    @Override
    public MutableChar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableChar.of(Strings.parseChar(str));
    }

    /**
     * Retrieves a MutableChar value from a ResultSet at the specified column index.
     * The database integer value is cast to char and wrapped in a MutableChar object.
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableChar containing the retrieved character value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableChar.of((char) rs.getInt(columnIndex));
    }

    /**
     * Retrieves a MutableChar value from a ResultSet using the specified column label.
     * The database integer value is cast to char and wrapped in a MutableChar object.
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableChar containing the retrieved character value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableChar get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableChar.of((char) rs.getInt(columnLabel));
    }

    /**
     * Sets a MutableChar parameter in a PreparedStatement at the specified position.
     * The character is stored as an integer value. If the MutableChar is null, 0 is stored.
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableChar value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableChar x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableChar parameter in a CallableStatement using the specified parameter name.
     * The character is stored as an integer value. If the MutableChar is null, 0 is stored.
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableChar value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableChar x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableChar to an Appendable.
     * The character is written directly or "null" if the value is null.
     *
     * @param appendable The Appendable to write to
     * @param x The MutableChar to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableChar x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.value());
        }
    }

    /**
     * Writes the character representation of a MutableChar to a CharacterWriter.
     * The character may be quoted based on the configuration. If null, writes the null character array.
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableChar to write
     * @param config The serialization configuration that may specify character quotation
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableChar x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x.value());
            } else {
                writer.write(ch);
                writer.writeCharacter(x.value());
                writer.write(ch);
            }
        }
    }
}