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
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableByte} objects.
 * <p>
 * This class provides serialization, deserialization, and database operations for
 * {@code MutableByte} instances, which are mutable wrappers around primitive
 * {@code byte} values. Values are serialized as the unwrapped numeric byte
 * (identical to {@link ByteType}).
 *
 * @see com.landawn.abacus.util.MutableByte
 * @see NumberType
 */
public class MutableByteType extends NumberType<MutableByte> {

    /** The type name constant for MutableByte type identification, equal to {@code "MutableByte"}. */
    public static final String MUTABLE_BYTE = MutableByte.class.getSimpleName();

    /**
     * Constructor for {@code MutableByteType}.
     * Instances are created by the {@code TypeFactory}.
     */
    protected MutableByteType() {
        super(MUTABLE_BYTE);
    }

    /**
     * Returns the {@link Class} object representing the {@link MutableByte} type.
     *
     * @return {@code MutableByte.class}
     */
    @Override
    public Class<MutableByte> javaType() {
        return MutableByte.class;
    }

    /**
     * Converts a {@link MutableByte} object to its decimal string representation.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code MutableByte} object to convert, may be {@code null}
     * @return the decimal string representation of the byte value, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final MutableByte x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a {@link MutableByte} object.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, may be {@code null} or empty
     * @return a {@code MutableByte} containing the parsed byte value,
     *         or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a byte
     * @see #valueOf(Object)
     * @see #stringOf(MutableByte)
     */
    @Override
    public MutableByte valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableByte.of(Numbers.toByte(str));
    }

    /**
     * Retrieves a byte value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableByte}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code MutableByte} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public MutableByte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final byte value = rs.getByte(columnIndex);

        return rs.wasNull() ? null : MutableByte.of(value);
    }

    /**
     * Retrieves a byte value from the specified column in the {@link ResultSet}
     * and wraps it in a {@link MutableByte}.
     * Returns {@code null} if the column value is SQL {@code NULL} (detected via {@link ResultSet#wasNull()}).
     *
     * @param rs the {@code ResultSet} containing the data
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code MutableByte} wrapping the retrieved value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public MutableByte get(final ResultSet rs, final String columnName) throws SQLException {
        final byte value = rs.getByte(columnName);

        return rs.wasNull() ? null : MutableByte.of(value);
    }

    /**
     * Sets a {@link MutableByte} parameter in a {@link PreparedStatement} at the specified index.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#TINYINT}) is set;
     * otherwise the wrapped byte value is stored.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code MutableByte} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableByte x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.TINYINT);
        } else {
            stmt.setByte(columnIndex, x.value());
        }
    }

    /**
     * Sets a {@link MutableByte} parameter in a {@link CallableStatement} by name.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#TINYINT}) is set;
     * otherwise the wrapped byte value is stored.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code MutableByte} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableByte x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.value());
        }
    }

    /**
     * Appends the decimal string representation of a {@link MutableByte} to an {@link Appendable}.
     * Writes {@code "null"} when {@code x} is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to write to
     * @param x the {@code MutableByte} to append, may be {@code null}
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
    public void appendTo(final Appendable appendable, final MutableByte x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the byte value of a {@link MutableByte} to a {@link CharacterWriter}.
     * Writes the {@code NULL_CHAR_ARRAY} when {@code x} is {@code null}.
     * The {@code config} parameter is not used for byte values.
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
     * @param x the {@code MutableByte} to write, may be {@code null}
     * @param config the serialization configuration (unused for byte values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final MutableByte x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
