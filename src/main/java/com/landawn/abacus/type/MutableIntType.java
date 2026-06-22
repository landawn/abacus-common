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
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableInt} objects.
 * <p>
 * This class provides serialization, deserialization, and database operations for
 * {@code MutableInt} instances, which are mutable wrappers around primitive
 * {@code int} values. Values are serialized as the unwrapped numeric integer
 * (identical to {@link IntegerType}).
 *
 * @see com.landawn.abacus.util.MutableInt
 * @see NumberType
 */
public class MutableIntType extends NumberType<MutableInt> {

    /** The type name constant for MutableInt type identification, equal to {@code "MutableInt"}. */
    public static final String MUTABLE_INT = MutableInt.class.getSimpleName();

    /**
     * Constructor for {@code MutableIntType}.
     * Instances are created by the {@code TypeFactory}.
     */
    protected MutableIntType() {
        super(MUTABLE_INT);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableInt} type.
     *
     * @return {@code MutableInt.class}
     */
    @Override
    public Class<MutableInt> javaType() {
        return MutableInt.class;
    }

    /**
     * Converts a {@link MutableInt} object to its decimal string representation.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code MutableInt} object to convert, may be {@code null}
     * @return the decimal string representation of the integer value,
     *         or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final MutableInt x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableInt} object.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableInt} containing the parsed integer value,
     *         or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as an integer
     * @see #valueOf(Object)
     * @see #stringOf(MutableInt)
     */
    @Override
    public MutableInt valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableInt.of(Numbers.toInt(str));
    }

    /**
     * Retrieves an integer value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableInt}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableInt} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        final int value = rs.getInt(columnIndex);

        return rs.wasNull() ? null : MutableInt.of(value);
    }

    /**
     * Retrieves an integer value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableInt}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableInt} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableInt get(final ResultSet rs, final String columnName) throws SQLException {
        final int value = rs.getInt(columnName);

        return rs.wasNull() ? null : MutableInt.of(value);
    }

    /**
     * Sets a {@link MutableInt} parameter in a {@link PreparedStatement} at the specified index.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#INTEGER}) is set;
     * otherwise the wrapped integer value is stored.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableInt} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableInt x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableInt} parameter in a {@link CallableStatement} by name.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#INTEGER}) is set;
     * otherwise the wrapped integer value is stored.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableInt} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableInt x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.value());
        }
    }

    /**
     * Appends the decimal string representation of a {@link MutableInt} to an {@link Appendable}.
     * Writes {@code "null"} when {@code x} is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableInt} to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final MutableInt x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the integer value of a {@link MutableInt} to a {@link CharacterWriter}
     * using the optimized {@code writeInt} method.
     * Writes the {@code NULL_CHAR_ARRAY} when {@code x} is {@code null}.
     * The {@code config} parameter is not used for integer values.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableInt} to write, may be {@code null}
     * @param config the serialization configuration (unused for integer values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final MutableInt x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            if (config != null && config.isWriteNullNumberAsZero()) {
                writer.writeInt(0);
            } else {
                writer.write(NULL_CHAR_ARRAY);
            }
        } else {
            writer.writeInt(x.value());
        }
    }
}
