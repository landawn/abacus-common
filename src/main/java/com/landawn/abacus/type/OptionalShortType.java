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
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Type handler for {@link OptionalShort} objects from the {@code com.landawn.abacus.util.u} package,
 * providing serialization, deserialization, and database interaction capabilities for optional short integer values.
 * Note: this handles the abacus-specific {@code OptionalShort}, which has no direct JDK equivalent.
 * This handler manages the conversion between database short values and {@link OptionalShort} wrapper objects,
 * mapping to the SQL {@code SMALLINT} type.
 */
public class OptionalShortType extends AbstractOptionalType<OptionalShort> {

    /** The type name constant for OptionalShort type identification, equal to {@code "OptionalShort"}. */
    public static final String OPTIONAL_SHORT = OptionalShort.class.getSimpleName();

    /**
     * Constructs a new OptionalShortType instance.
     * Instances are normally obtained via the TypeFactory rather than constructed directly.
     */
    protected OptionalShortType() {
        super(OPTIONAL_SHORT);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     * Class<OptionalShort> clazz = type.javaType();
     * // Returns: OptionalShort.class
     * }</pre>
     *
     * @return the {@link OptionalShort} class object
     */
    @Override
    public Class<OptionalShort> javaType() {
        return OptionalShort.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalShort values support comparison operations.
     *
     * @return {@code true}, as OptionalShort values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * OptionalShort values are numeric and do not require quotes in CSV.
     *
     * @return {@code false}, as OptionalShort values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Returns the default value for OptionalShort type, which is an empty OptionalShort.
     *
     * @return OptionalShort.empty()
     */
    @Override
    public OptionalShort defaultValue() {
        return OptionalShort.empty();
    }

    /**
     * Converts an {@link OptionalShort} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     *
     * OptionalShort opt = OptionalShort.of((short) 100);
     * String result = type.stringOf(opt);
     * // Returns: "100"
     *
     * opt = OptionalShort.of((short) 0);
     * result = type.stringOf(opt);
     * // Returns: "0"
     *
     * opt = OptionalShort.empty();
     * result = type.stringOf(opt);
     * // Returns: null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OptionalShort object to convert
     * @return the string representation of the short value, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OptionalShort x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalShort} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     *
     * OptionalShort result = type.valueOf("100");
     * // Returns: OptionalShort.of((short) 100)
     *
     * result = type.valueOf("0");
     * // Returns: OptionalShort.of((short) 0)
     *
     * result = type.valueOf(null);
     * // Returns: OptionalShort.empty()
     *
     * result = type.valueOf("");
     * // Returns: OptionalShort.empty()
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return an OptionalShort containing the parsed short value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a short
     * @see #valueOf(Object)
     * @see #stringOf(OptionalShort)
     */
    @Override
    public OptionalShort valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalShort.empty() : OptionalShort.of(Numbers.toShort(str));
    }

    /**
     * Retrieves a short value from a ResultSet at the specified column index and wraps it in an {@link OptionalShort}.
     * Handles type conversion if the database column is not a short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     * ResultSet rs = Mockito.mock(ResultSet.class);
     * Mockito.when(rs.getObject(1)).thenReturn((short) 100);
     * Mockito.when(rs.getObject(2)).thenReturn(null);
     *
     * // Column contains short value 100
     * OptionalShort opt = type.get(rs, 1);
     * // Returns: OptionalShort.of((short) 100)
     *
     * // Column contains SQL NULL
     * opt = type.get(rs, 2);
     * // Returns: OptionalShort.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalShort containing the short value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalShort get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalShort.empty()
                : OptionalShort.of(result instanceof Short num ? num : (result instanceof Number num ? num.shortValue() : Numbers.toShort(result.toString())));
    }

    /**
     * Retrieves a short value from a ResultSet using the specified column label and wraps it in an {@link OptionalShort}.
     * Handles type conversion if the database column is not a short type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     * ResultSet rs = Mockito.mock(ResultSet.class);
     * Mockito.when(rs.getObject("quantity")).thenReturn((short) 50);
     * Mockito.when(rs.getObject("level")).thenReturn(null);
     *
     * // Column "quantity" contains short value 50
     * OptionalShort opt = type.get(rs, "quantity");
     * // Returns: OptionalShort.of((short) 50)
     *
     * // Column "level" contains SQL NULL
     * opt = type.get(rs, "level");
     * // Returns: OptionalShort.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return an OptionalShort containing the short value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public OptionalShort get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        return result == null ? OptionalShort.empty()
                : OptionalShort.of(result instanceof Short num ? num : (result instanceof Number num ? num.shortValue() : Numbers.toShort(result.toString())));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalShort}.
     * If the OptionalShort is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     * PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
     *
     * OptionalShort opt = OptionalShort.of((short) 50);
     * type.set(stmt, 2, opt);
     * // Sets parameter to 50
     *
     * opt = OptionalShort.empty();
     * type.set(stmt, 2, opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalShort value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalShort x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.SMALLINT);
        } else {
            stmt.setShort(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalShort}.
     * If the OptionalShort is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalShort> type = TypeFactory.getType(OptionalShort.class);
     * CallableStatement stmt = Mockito.mock(CallableStatement.class);
     *
     * OptionalShort opt = OptionalShort.of((short) 75);
     * type.set(stmt, "p_quantity", opt);
     * // Sets parameter to 75
     *
     * opt = OptionalShort.empty();
     * type.set(stmt, "p_level", opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalShort value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalShort x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.SMALLINT);
        } else {
            stmt.setShort(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalShort} to an Appendable.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalShort value to append
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
    public void appendTo(final Appendable appendable, final OptionalShort x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalShort} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalShort value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final OptionalShort x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
