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
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableLong} objects, providing
 * serialization, deserialization, and database interaction capabilities for
 * mutable {@code long} wrapper objects. Values are serialized as the unwrapped
 * numeric long (identical to {@link LongType}).
 *
 * @see com.landawn.abacus.util.MutableLong
 * @see NumberType
 */
public class MutableLongType extends NumberType<MutableLong> {

    /** The type name constant for MutableLong type identification, equal to {@code "MutableLong"}. */
    public static final String MUTABLE_LONG = MutableLong.class.getSimpleName();

    /**
     * Protected constructor for MutableLongType.
     * This constructor is invoked by the type system (typically via {@link TypeFactory})
     * when registering the {@link MutableLong} type handler.
     */
    protected MutableLongType() {
        super(MUTABLE_LONG);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableLong} type.
     *
     * @return {@code MutableLong.class}
     */
    @Override
    public Class<MutableLong> javaType() {
        return MutableLong.class;
    }

    /**
     * Converts a {@link MutableLong} object to its decimal string representation.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code MutableLong} object to convert, may be {@code null}
     * @return the string representation of the long value, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final MutableLong x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableLong} object.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableLong} containing the parsed long value,
     *         or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a long
     * @see #valueOf(Object)
     * @see #stringOf(MutableLong)
     */
    @Override
    public MutableLong valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableLong.of(Numbers.toLong(str));
    }

    /**
     * Retrieves a long value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableLong}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableLong} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long value = rs.getLong(columnIndex);

        return rs.wasNull() ? null : MutableLong.of(value);
    }

    /**
     * Retrieves a long value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableLong}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableLong} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableLong get(final ResultSet rs, final String columnName) throws SQLException {
        final long value = rs.getLong(columnName);

        return rs.wasNull() ? null : MutableLong.of(value);
    }

    /**
     * Sets a {@link MutableLong} parameter in a {@link PreparedStatement} at the specified index.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BIGINT}) is set;
     * otherwise the wrapped long value is stored.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableLong} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableLong x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableLong} parameter in a {@link CallableStatement} by name.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BIGINT}) is set;
     * otherwise the wrapped long value is stored.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableLong} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableLong x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.value());
        }
    }

    /**
     * Appends the decimal string representation of a {@link MutableLong} to an {@link Appendable}.
     * Writes {@code "null"} when {@code x} is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableLong} to append, may be {@code null}
     * @throws IOException if an I/O error occurs during the append operation
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
    public void appendTo(final Appendable appendable, final MutableLong x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the long value of a {@link MutableLong} to a {@link CharacterWriter}.
     * A {@code null} wrapper is written as zero when
     * {@link JsonXmlSerConfig#isWriteNullNumberAsZero()} is enabled, and as {@code null} otherwise.
     * A non-null value (or configured null-as-zero value) is quoted when
     * {@link JsonXmlSerConfig#isWriteLongAsString()} is enabled with a non-zero string quotation.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String escaping options do not otherwise affect the numeric value.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code MutableLong} to write, may be {@code null}
     * @param config the serialization configuration controlling null-number and long-as-string output; may be {@code null}
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final MutableLong x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null && !(config != null && config.isWriteNullNumberAsZero())) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final long value = x == null ? 0L : x.value();

            if (config != null && config.isWriteLongAsString() && config.getStringQuotation() != 0) {
                final char ch = config.getStringQuotation();

                writer.write(ch);
                writer.write(value);
                writer.write(ch);
            } else {
                writer.write(value);
            }
        }
    }
}
