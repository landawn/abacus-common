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
 * (identical to {@link CharacterType}); database storage uses SQL {@code VARCHAR}
 * for the single-character string value.
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
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code MutableChar} object to convert, may be {@code null}
     * @return the single-character string representation, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final MutableChar x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableChar} object.
     * A single-character string yields that character; a longer string is parsed as an
     * integer and cast to the corresponding {@code char} (e.g. {@code "65"} yields {@code 'A'}).
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableChar} containing the parsed character,
     *         or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string has more than one character and cannot be parsed as an integer
     * @throws IllegalArgumentException if the string represents an integer outside the valid char range [0, 65535]
     * @see #valueOf(Object)
     * @see #stringOf(MutableChar)
     */
    @SuppressWarnings("deprecation")
    @Override
    public MutableChar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableChar.of(Strings.parseChar(str));
    }

    /**
     * Retrieves a string value from the specified column in the {@link ResultSet}
     * and wraps its first character in a {@link MutableChar}.
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableChar} wrapping the retrieved character value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String result = rs.getString(columnIndex);

        return Strings.isEmpty(result) ? null : MutableChar.of(result.charAt(0));
    }

    /**
     * Retrieves a string value from the specified column in the {@link ResultSet}
     * and wraps its first character in a {@link MutableChar}.
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableChar} wrapping the retrieved character value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableChar get(final ResultSet rs, final String columnName) throws SQLException {
        final String result = rs.getString(columnName);

        return Strings.isEmpty(result) ? null : MutableChar.of(result.charAt(0));
    }

    /**
     * Sets a {@link MutableChar} parameter in a {@link PreparedStatement} at the specified index.
     * The character is stored as a single-character string (SQL {@code VARCHAR}) value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#VARCHAR}) is set.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableChar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableChar x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.VARCHAR);
        } else {
            stmt.setString(columnIndex, String.valueOf(x.value()));
        }
    }

    /**
     * Sets a {@link MutableChar} parameter in a {@link CallableStatement} by name.
     * The character is stored as a single-character string (SQL {@code VARCHAR}) value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#VARCHAR}) is set.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableChar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableChar x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.VARCHAR);
        } else {
            stmt.setString(parameterName, String.valueOf(x.value()));
        }
    }

    /**
     * Appends the character value of a {@link MutableChar} directly to an {@link Appendable}
     * (unquoted, as a single character).
     * Writes the literal string {@code "null"} when {@code x} is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableChar} to append, may be {@code null}
     * @throws IOException if an I/O error occurs while appending
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
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
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableChar} to write, may be {@code null}
     * @param config the serialization configuration; may specify a character quotation character
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final MutableChar x, final JsonXmlSerConfig<?> config) throws IOException {
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
