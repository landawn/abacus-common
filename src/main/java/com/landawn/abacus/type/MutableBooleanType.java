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
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableBoolean} objects.
 * <p>
 * This class provides serialization, deserialization, and database operations for
 * {@code MutableBoolean} instances, which are mutable wrappers around primitive
 * {@code boolean} values. Values are serialized as the unwrapped {@code boolean}
 * (i.e., {@code "true"} or {@code "false"}, identical to {@link BooleanType}).
 *
 * @see com.landawn.abacus.util.MutableBoolean
 * @see AbstractType
 */
public class MutableBooleanType extends AbstractType<MutableBoolean> {

    /** The type name constant for MutableBoolean type identification, equal to {@code "MutableBoolean"}. */
    public static final String MUTABLE_BOOLEAN = MutableBoolean.class.getSimpleName();

    /**
     * Constructor for {@code MutableBooleanType}.
     * Instances are created by the {@code TypeFactory}.
     */
    protected MutableBooleanType() {
        super(MUTABLE_BOOLEAN);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableBoolean} type.
     *
     * @return {@code MutableBoolean.class}
     */
    @Override
    public Class<MutableBoolean> javaType() {
        return MutableBoolean.class;
    }

    /**
     * Indicates whether values of this type are comparable.
     * {@link MutableBoolean} implements {@link Comparable}, so this always returns {@code true}.
     *
     * @return {@code true}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts a {@link MutableBoolean} object to its string representation ({@code "true"} or {@code "false"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code MutableBoolean} object to convert, may be {@code null}
     * @return {@code "true"} or {@code "false"}, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final MutableBoolean x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableBoolean} object. Single-character inputs {@code "Y"}, {@code "y"}
     * and {@code "1"} are parsed as {@code true}; other inputs follow {@link Boolean#valueOf(String)} semantics,
     * consistent with the other boolean type handlers.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableBoolean} containing the parsed value,
     *         or {@code null} if the input is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(MutableBoolean)
     */
    @Override
    public MutableBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableBoolean.of(parseBoolean(str));
    }

    /**
     * Retrieves a boolean value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableBoolean}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableBoolean} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final boolean value = rs.getBoolean(columnIndex);

        return rs.wasNull() ? null : MutableBoolean.of(value);
    }

    /**
     * Retrieves a boolean value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableBoolean}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableBoolean} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableBoolean get(final ResultSet rs, final String columnName) throws SQLException {
        final boolean value = rs.getBoolean(columnName);

        return rs.wasNull() ? null : MutableBoolean.of(value);
    }

    /**
     * Sets a {@link MutableBoolean} parameter in a {@link PreparedStatement} at the specified index.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BOOLEAN}) is set;
     * otherwise the wrapped boolean value is stored.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableBoolean} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableBoolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableBoolean} parameter in a {@link CallableStatement} by name.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BOOLEAN}) is set;
     * otherwise the wrapped boolean value is stored.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableBoolean} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableBoolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x.value());
        }
    }

    /**
     * Appends the string representation of a {@link MutableBoolean} to an {@link Appendable}.
     * Writes {@code "true"}, {@code "false"}, or {@code "null"} (when {@code x} is {@code null}).
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableBoolean} to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final MutableBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.value() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes the character representation of a {@link MutableBoolean} to a {@link CharacterWriter}.
     * Writes the pre-allocated {@code TRUE_CHAR_ARRAY}, {@code FALSE_CHAR_ARRAY}, or {@code NULL_CHAR_ARRAY}.
     * The {@code config} parameter is not used for boolean values.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableBoolean} to write, may be {@code null}
     * @param config the serialization configuration (unused for boolean values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final MutableBoolean x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(config != null && config.isWriteNullBooleanAsFalse() ? FALSE_CHAR_ARRAY : NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY);
        }
    }
}
