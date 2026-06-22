/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalByte;

/**
 * Type handler for {@link OptionalByte} objects from the {@code com.landawn.abacus.util.u} package,
 * providing serialization, deserialization, and database interaction capabilities for optional byte values.
 * Note: this handles the abacus-specific {@code OptionalByte}, which has no direct JDK equivalent.
 * This handler manages the conversion between database byte/numeric values and {@link OptionalByte} wrapper objects.
 */
public class OptionalByteType extends AbstractOptionalType<OptionalByte> {

    /** The type name constant for OptionalByte type identification, equal to {@code "OptionalByte"}. */
    public static final String OPTIONAL_BYTE = OptionalByte.class.getSimpleName();

    /**
     * Constructs a new OptionalByteType instance.
     * Instances are normally obtained via the TypeFactory rather than constructed directly.
     */
    protected OptionalByteType() {
        super(OPTIONAL_BYTE);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * Class<OptionalByte> clazz = type.javaType();
     * // clazz equals OptionalByte.class
     * }</pre>
     *
     * @return the {@link OptionalByte} class object
     */
    @Override
    public Class<OptionalByte> javaType() {
        return OptionalByte.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalByte values support comparison operations.
     *
     * @return {@code true}, as OptionalByte values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * OptionalByte values are numeric and do not require quotes in CSV.
     *
     * @return {@code false}, as OptionalByte values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Returns the default value for OptionalByte type, which is an empty OptionalByte.
     *
     * @return OptionalByte.empty()
     */
    @Override
    public OptionalByte defaultValue() {
        return OptionalByte.empty();
    }

    /**
     * Converts an {@link OptionalByte} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * OptionalByte opt = OptionalByte.of((byte) 42);
     * String str = type.stringOf(opt);
     * // str equals "42"
     *
     * opt = OptionalByte.empty();
     * str = type.stringOf(opt);
     * // str equals null
     *
     * str = type.stringOf(null);
     * // str equals null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OptionalByte object to convert
     * @return the string representation of the byte value, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OptionalByte x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalByte} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * OptionalByte opt = type.valueOf("127");
     * // opt.get() equals (byte) 127
     *
     * opt = type.valueOf("-128");
     * // opt.get() equals (byte) -128
     *
     * opt = type.valueOf(null);
     * // opt.isEmpty() returns true
     *
     * opt = type.valueOf("");
     * // opt.isEmpty() returns true
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return an OptionalByte containing the parsed byte value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a byte
     * @see #valueOf(Object)
     * @see #stringOf(OptionalByte)
     */
    @Override
    public OptionalByte valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalByte.empty() : OptionalByte.of(Numbers.toByte(str));
    }

    /**
     * Retrieves a byte value from a ResultSet at the specified column index and wraps it in an {@link OptionalByte}.
     * Handles type conversion if the database column is not a byte type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * ResultSet rs = statement.executeQuery("SELECT status_code FROM records");
     * if (rs.next()) {
     *     OptionalByte statusCode = type.get(rs, 1);
     *     if (statusCode.isPresent()) {
     *         byte code = statusCode.get();
     *         // Process the status code
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalByte containing the byte value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalByte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalByte.empty()
                : OptionalByte.of(result instanceof Byte num ? num : (result instanceof Number num ? num.byteValue() : Numbers.toByte(result.toString())));
    }

    /**
     * Retrieves a byte value from a ResultSet using the specified column label and wraps it in an {@link OptionalByte}.
     * Handles type conversion if the database column is not a byte type.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return an OptionalByte containing the byte value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public OptionalByte get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        return result == null ? OptionalByte.empty()
                : OptionalByte.of(result instanceof Byte num ? num : (result instanceof Number num ? num.byteValue() : Numbers.toByte(result.toString())));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalByte}.
     * If the OptionalByte is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO records (status_code) VALUES (?)");
     *
     * OptionalByte statusCode = OptionalByte.of((byte) 1);
     * type.set(stmt, 1, statusCode);
     * stmt.executeUpdate();
     * // Sets status_code to 1
     *
     * statusCode = OptionalByte.empty();
     * type.set(stmt, 1, statusCode);
     * // Sets status_code to SQL NULL
     *
     * type.set(stmt, 1, null);
     * // Sets status_code to SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalByte value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalByte}.
     * If the OptionalByte is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalByte value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalByte} to an Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * StringBuilder sb = new StringBuilder();
     *
     * OptionalByte opt = OptionalByte.of((byte) 100);
     * type.appendTo(sb, opt);
     * // sb.toString() equals "100"
     *
     * sb = new StringBuilder();
     * type.appendTo(sb, OptionalByte.empty());
     * // sb.toString() equals "null"
     * }</pre>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalByte value to append
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
    public void appendTo(final Appendable appendable, final OptionalByte x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalByte} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalByte> type = TypeFactory.getType(OptionalByte.class);
     * BufferedJsonWriter writer = new BufferedJsonWriter();
     *
     * OptionalByte opt = OptionalByte.of((byte) 127);
     * type.serializeTo(writer, opt, null);
     * // Writes: 127
     *
     * writer = new BufferedJsonWriter();
     * type.serializeTo(writer, OptionalByte.empty(), null);
     * // Writes: null
     * }</pre>
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalByte value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final OptionalByte x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
