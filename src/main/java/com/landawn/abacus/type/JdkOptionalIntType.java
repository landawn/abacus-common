/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalInt;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.OptionalInt}, the JDK primitive-int optional wrapper.
 * This class provides serialization, deserialization, and database access capabilities for
 * {@link OptionalInt} instances. {@link OptionalInt} is a container that may or may not contain
 * an {@code int} value; this handler unboxes the underlying primitive on read/write.
 * Empty optionals (and {@code null} references) are represented as {@code null} in serialized form
 * and as SQL {@code NULL} in database form.
 */
public class JdkOptionalIntType extends AbstractOptionalType<OptionalInt> {

    /** The type name constant for {@link OptionalInt} (JDK) type identification. */
    public static final String OPTIONAL_INT = "JdkOptionalInt";

    /**
     * Constructor for JdkOptionalIntType.
     * This constructor is called by the TypeFactory to create OptionalInt type instances.
     */
    protected JdkOptionalIntType() {
        super(OPTIONAL_INT);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code OptionalInt.class}
     */
    @Override
    public Class<OptionalInt> javaType() {
        return OptionalInt.class;
    }

    /**
     * Indicates whether instances of this type implement the Comparable interface.
     * OptionalInt values can be compared when both are present.
     *
     * @return {@code true}, as OptionalInt values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    @Override
    public int compare(final OptionalInt x, final OptionalInt y) {
        if (x == null) {
            return y == null ? 0 : -1;
        } else if (y == null) {
            return 1;
        } else if (x.isEmpty()) {
            return y.isEmpty() ? 0 : -1;
        } else if (y.isEmpty()) {
            return 1;
        }

        return Integer.compare(x.getAsInt(), y.getAsInt());
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * OptionalInt values are numeric and do not require quotes.
     *
     * @return {@code false}, as OptionalInt values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Returns the default value for OptionalInt type, which is an empty OptionalInt.
     *
     * @return OptionalInt.empty()
     */
    @Override
    public OptionalInt defaultValue() {
        return OptionalInt.empty();
    }

    /**
     * Converts an OptionalInt to its string representation.
     * If the optional is empty or {@code null}, returns {@code null}.
     * Otherwise, returns the string representation of the contained int value.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OptionalInt to convert to string
     * @return the string representation of the int value, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OptionalInt x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.getAsInt());
    }

    /**
     * Parses a string representation into an OptionalInt.
     * Empty or {@code null} strings result in an empty OptionalInt.
     * Non-empty strings are parsed as int values and wrapped in OptionalInt.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return OptionalInt.empty() if the string is {@code null} or empty, otherwise OptionalInt containing the parsed value
     * @see #valueOf(Object)
     * @see #stringOf(OptionalInt)
     */
    @Override
    public OptionalInt valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalInt.empty() : OptionalInt.of(Numbers.toInt(str));
    }

    /**
     * Retrieves an OptionalInt value from the specified column in a ResultSet.
     * If the column value is {@code null}, returns an empty OptionalInt.
     * Otherwise, converts the value to int and wraps it in OptionalInt.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return OptionalInt.empty() if the column is {@code null}, otherwise OptionalInt containing the value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalInt.empty()
                : OptionalInt.of(result instanceof Integer num ? num : (result instanceof Number num ? num.intValue() : Numbers.toInt(result.toString())));
    }

    /**
     * Retrieves an OptionalInt value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty OptionalInt.
     * Otherwise, converts the value to int and wraps it in OptionalInt.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to read
     * @return OptionalInt.empty() if the column is {@code null}, otherwise OptionalInt containing the value
     * @throws SQLException if a database access error occurs or the columnName is not found
     */
    @Override
    public OptionalInt get(final ResultSet rs, final String columnName) throws SQLException {
        final Object result = rs.getObject(columnName);

        return result == null ? OptionalInt.empty()
                : OptionalInt.of(result instanceof Integer num ? num : (result instanceof Number num ? num.intValue() : Numbers.toInt(result.toString())));
    }

    /**
     * Sets an OptionalInt parameter in a PreparedStatement.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the int value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the OptionalInt to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.getAsInt());
        }
    }

    /**
     * Sets an OptionalInt parameter in a CallableStatement using a parameter name.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the int value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalInt to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.getAsInt());
        }
    }

    /**
     * Appends the string representation of an OptionalInt to an Appendable.
     * Empty optionals are written as "null".
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalInt to append
     * @throws IOException if an I/O error occurs during writing
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
    public void appendTo(final Appendable appendable, final OptionalInt x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.getAsInt()));
        }
    }

    /**
     * Writes the character representation of an OptionalInt to a CharacterWriter.
     * Empty optionals are written as {@code null}.
     * Present values are written as numeric values without quotes using the optimized writeInt method.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalInt to write
     * @param config the serialization configuration (not used for numeric values)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final OptionalInt x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.getAsInt());
        }
    }
}
