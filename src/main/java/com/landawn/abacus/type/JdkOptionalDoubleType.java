/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalDouble;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.OptionalDouble}, the JDK primitive-double optional wrapper.
 * This class provides serialization, deserialization, and database access capabilities for
 * {@link OptionalDouble} instances. {@link OptionalDouble} is a container that may or may not
 * contain a {@code double} value; this handler unboxes the underlying primitive on read/write.
 * Empty optionals (and {@code null} references) are represented as {@code null} in serialized form
 * and as SQL {@code NULL} in database form.
 */
public class JdkOptionalDoubleType extends AbstractOptionalType<OptionalDouble> {

    /** The type name constant for {@link OptionalDouble} (JDK) type identification. */
    public static final String OPTIONAL_DOUBLE = "JdkOptionalDouble";

    /**
     * Constructor for JdkOptionalDoubleType.
     * This constructor is called by the TypeFactory to create OptionalDouble type instances.
     */
    protected JdkOptionalDoubleType() {
        super(OPTIONAL_DOUBLE);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code OptionalDouble.class}
     */
    @Override
    public Class<OptionalDouble> javaType() {
        return OptionalDouble.class;
    }

    /**
     * Indicates that this handler defines an ordering for {@link OptionalDouble} values.
     * The ordering is {@code null < empty < present}; present values are compared numerically.
     *
     * @return {@code true}, because this handler implements comparison even though
     *         {@code OptionalDouble} itself does not implement {@link Comparable}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Compares two {@link OptionalDouble} values using the ordering
     * {@code null < empty < present}, with present values compared numerically.
     *
     * @param x the first value; may be {@code null}
     * @param y the second value; may be {@code null}
     * @return a negative integer, zero, or a positive integer as {@code x} is less than, equal to,
     *         or greater than {@code y}
     */
    @Override
    public int compare(final OptionalDouble x, final OptionalDouble y) {
        if (x == null) {
            return y == null ? 0 : -1;
        } else if (y == null) {
            return 1;
        } else if (x.isEmpty()) {
            return y.isEmpty() ? 0 : -1;
        } else if (y.isEmpty()) {
            return 1;
        }

        return Double.compare(x.getAsDouble(), y.getAsDouble());
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * OptionalDouble values are numeric and do not require quotes.
     *
     * @return {@code false}, as OptionalDouble values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Returns the default value for OptionalDouble type, which is an empty OptionalDouble.
     *
     * @return OptionalDouble.empty()
     */
    @Override
    public OptionalDouble defaultValue() {
        return OptionalDouble.empty();
    }

    /**
     * Converts an OptionalDouble to its string representation.
     * If the optional is empty or {@code null}, returns {@code null}.
     * Otherwise, returns the string representation of the contained double value.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OptionalDouble to convert to string
     * @return the string representation of the double value, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OptionalDouble x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.getAsDouble());
    }

    /**
     * Parses a string representation into an OptionalDouble.
     * Empty or {@code null} strings result in an empty OptionalDouble.
     * Non-empty strings are parsed as double values and wrapped in OptionalDouble.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return OptionalDouble.empty() if the string is {@code null} or empty, otherwise OptionalDouble containing the parsed value
     * @throws NumberFormatException if a non-empty string cannot be parsed as a {@code double}
     * @see #valueOf(Object)
     * @see #stringOf(OptionalDouble)
     */
    @Override
    public OptionalDouble valueOf(final String str) throws NumberFormatException {
        return Strings.isEmpty(str) ? OptionalDouble.empty() : OptionalDouble.of(Numbers.toDouble(str));
    }

    /**
     * Retrieves an OptionalDouble value from the specified column in a ResultSet.
     * If the column value is {@code null}, returns an empty OptionalDouble.
     * Otherwise, converts the value to double and wraps it in OptionalDouble.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return OptionalDouble.empty() if the column is {@code null}, otherwise OptionalDouble containing the value. A
     *         non-{@code Number} column value is parsed with {@link Numbers#toDouble(String)}, so an empty string yields
     *         a <i>present</i> zero (the same answer {@code DoubleType.get} gives), unlike {@link #valueOf(String)}
     *         which answers empty for {@code ""}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws NumberFormatException if a non-{@code Number} column value is not a valid number token (a blank string         included)
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, NumberFormatException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalDouble.empty()
                : OptionalDouble
                        .of(result instanceof Double num ? num : (result instanceof Number num ? num.doubleValue() : Numbers.toDouble(result.toString())));
    }

    /**
     * Retrieves an OptionalDouble value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty OptionalDouble.
     * Otherwise, converts the value to double and wraps it in OptionalDouble.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to read
     * @return OptionalDouble.empty() if the column is {@code null}, otherwise OptionalDouble containing the value. A
     *         non-{@code Number} column value is parsed with {@link Numbers#toDouble(String)}, so an empty string yields
     *         a <i>present</i> zero (the same answer {@code DoubleType.get} gives), unlike {@link #valueOf(String)}
     *         which answers empty for {@code ""}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws NumberFormatException if a non-{@code Number} column value is not a valid number token (a blank string         included)
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, NumberFormatException {
        final Object result = rs.getObject(columnName);

        return result == null ? OptionalDouble.empty()
                : OptionalDouble
                        .of(result instanceof Double num ? num : (result instanceof Number num ? num.doubleValue() : Numbers.toDouble(result.toString())));
    }

    /**
     * Sets an OptionalDouble parameter in a PreparedStatement.
     * If the OptionalDouble is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the double value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the OptionalDouble to set
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalDouble x) throws NullPointerException, SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(columnIndex, x.getAsDouble());
        }
    }

    /**
     * Sets an OptionalDouble parameter in a CallableStatement using a parameter name.
     * If the OptionalDouble is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the double value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalDouble to set
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalDouble x) throws NullPointerException, SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, x.getAsDouble());
        }
    }

    /**
     * Appends the string representation of an OptionalDouble to an Appendable.
     * Empty optionals are written as "null".
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalDouble to append
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
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
    public void appendTo(final Appendable appendable, final OptionalDouble x) throws NullPointerException, IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.getAsDouble()));
        }
    }

    /**
     * Writes the character representation of an OptionalDouble to a CharacterWriter.
     * A {@code null} or empty optional is written as {@code null} unless {@code config.isWriteNullNumberAsZero()} is
     * set, in which case {@code 0.0} is written - the same substitution {@code DoubleType} applies to a {@code null}
     * value; a substituted zero reads back as a <i>present</i> {@code OptionalDouble.of(0.0d)}, which is what the flag
     * asks for. The XML serializers represent an empty optional property with the {@code isNull="true"} attribute
     * form rather than with the text written here.
     * Present values are written as numeric values without quotes.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalDouble to write
     * @param config the serialization configuration; only {@code writeNullNumberAsZero} is consulted, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final OptionalDouble x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null || x.isEmpty()) {
            if (config != null && config.isWriteNullNumberAsZero()) {
                writer.write(0.0d);
            } else {
                writer.write(NULL_CHAR_ARRAY);
            }
        } else {
            writer.write(x.getAsDouble());
        }
    }
}
