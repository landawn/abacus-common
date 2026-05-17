/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableChar} objects.
 * <p>
 * This class provides serialization, deserialization, and database operations for
 * {@code MutableChar} instances, which are mutable wrappers around primitive
 * {@code char} values. Values are serialized as the unwrapped character
 * (identical to {@link CharacterType}); database storage uses SQL {@code INTEGER}
 * for the character's numeric code point.
 *
 * @see com.landawn.abacus.util.MutableChar
 * @see AbstractType
 */
public class MutableCharType extends AbstractType<MutableChar> {

    /** The type name constant for MutableChar type identification, equal to {@code "MutableChar"}. */
    public static final String MUTABLE_CHAR = MutableChar.class.getSimpleName();

    /**
     * Constructor for {@code MutableCharType}.
     * Instances are created by the {@code TypeFactory}.
     */
    protected MutableCharType() {
        super(MUTABLE_CHAR);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableChar} type.
     *
     * @return {@code MutableChar.class}
     */
    @Override
    public Class<MutableChar> javaType() {
        return MutableChar.class;
    }

    /**
     * Indicates whether values of this type are comparable.
     * {@link MutableChar} implements {@link Comparable}, so this returns {@code true}.
     *
     * @return {@code true}, indicating that MutableChar values can be compared
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts a {@link MutableChar} object to its single-character string representation.
     *
     * @param x the {@code MutableChar} object to convert, may be {@code null}
     * @return the single-character string representation, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final MutableChar x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableChar} object.
     * The first character of the string is used as the value.
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableChar} containing the parsed character,
     *         or {@code null} if the input is {@code null} or empty
     */
    @Override
    public MutableChar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableChar.of(Strings.parseChar(str));
    }

    /**
     * Retrieves an integer value from the specified column in the {@link ResultSet},
     * casts it to {@code char}, and wraps it in a {@link MutableChar}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableChar} wrapping the retrieved character value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final int value = rs.getInt(columnIndex);

        return rs.wasNull() ? null : MutableChar.of((char) value);
    }

    /**
     * Retrieves an integer value from the specified column in the {@link ResultSet},
     * casts it to {@code char}, and wraps it in a {@link MutableChar}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableChar} wrapping the retrieved character value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableChar get(final ResultSet rs, final String columnName) throws SQLException {
        final int value = rs.getInt(columnName);

        return rs.wasNull() ? null : MutableChar.of((char) value);
    }

    /**
     * Sets a {@link MutableChar} parameter in a {@link PreparedStatement} at the specified index.
     * The character is stored as an integer (SQL {@code INTEGER}) value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#INTEGER}) is set.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableChar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableChar x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableChar} parameter in a {@link CallableStatement} by name.
     * The character is stored as an integer (SQL {@code INTEGER}) value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#INTEGER}) is set.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableChar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableChar x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.value());
        }
    }

    /**
     * Appends the character value of a {@link MutableChar} directly to an {@link Appendable}.
     * Writes the literal string {@code "null"} when {@code x} is {@code null}.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableChar} to append, may be {@code null}
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
     * Writes the character value of a {@link MutableChar} to a {@link CharacterWriter}.
     * If {@code config} specifies a character quotation character (via
     * {@link com.landawn.abacus.parser.JsonXmlSerConfig#getCharQuotation()}),
     * the character is wrapped in that quotation character; otherwise it is written unquoted.
     * Writes {@code NULL_CHAR_ARRAY} when {@code x} is {@code null}.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableChar} to write, may be {@code null}
     * @param config the serialization configuration; may specify a character quotation character
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableChar x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x.value());
            } else {
                writer.write(ch);

                if (x.value() == '\'' && ch == '\'') {
                    writer.write('\\');
                }

                writer.writeCharacter(x.value());
                writer.write(ch);
            }
        }
    }
}
