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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalChar;

/**
 * Type handler for {@link OptionalChar} objects from the {@code com.landawn.abacus.util.u} package,
 * providing serialization, deserialization, and database interaction capabilities for optional character values.
 * Note: this handles the abacus-specific {@code OptionalChar}, which has no direct JDK equivalent.
 * Character values are stored in the database as VARCHAR (single-character string), not as integer code points.
 */
public class OptionalCharType extends AbstractOptionalType<OptionalChar> {

    /** The type name constant for OptionalChar type identification, equal to {@code "OptionalChar"}. */
    public static final String OPTIONAL_CHAR = OptionalChar.class.getSimpleName();

    /**
     * Constructs a new OptionalCharType instance.
     * Instances are normally obtained via the TypeFactory rather than constructed directly.
     */
    protected OptionalCharType() {
        super(OPTIONAL_CHAR);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     * Class<OptionalChar> clazz = type.javaType();
     * // Returns: OptionalChar.class
     * }</pre>
     *
     * @return the {@link OptionalChar} class object
     */
    @Override
    public Class<OptionalChar> javaType() {
        return OptionalChar.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalChar values support comparison operations.
     *
     * @return {@code true}, as OptionalChar values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * Unlike the numeric and boolean optional siblings (which return {@code false}), an optional
     * character value may itself be a CSV delimiter or quote character (such as {@code ','} or
     * {@code '"'}), so it must be quoted to round-trip safely. This returns the same value as the
     * inherited interface default; it is overridden explicitly so the abacus {@code Optional*Type}
     * family uniformly and intentionally declares this predicate.
     *
     * @return {@code true}, always, because a character value may collide with CSV delimiters/quotes
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return true;
    }

    /**
     * Returns the default value for OptionalChar type, which is an empty OptionalChar.
     *
     * @return OptionalChar.empty()
     */
    @Override
    public OptionalChar defaultValue() {
        return OptionalChar.empty();
    }

    /**
     * Converts an {@link OptionalChar} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     *
     * OptionalChar opt = OptionalChar.of('A');
     * String result = type.stringOf(opt);
     * // Returns: "A"
     *
     * opt = OptionalChar.of('z');
     * result = type.stringOf(opt);
     * // Returns: "z"
     *
     * opt = OptionalChar.empty();
     * result = type.stringOf(opt);
     * // Returns: null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OptionalChar object to convert
     * @return a single-character string, or {@code null} if empty or null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OptionalChar x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalChar} object.
     * The string should contain exactly one character or be convertible to a character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     *
     * OptionalChar result = type.valueOf("A");
     * // Returns: OptionalChar.of('A')
     *
     * result = type.valueOf("x");
     * // Returns: OptionalChar.of('x')
     *
     * result = type.valueOf(null);
     * // Returns: OptionalChar.empty()
     *
     * result = type.valueOf("");
     * // Returns: OptionalChar.empty()
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert
     * @return an OptionalChar containing the parsed character value, or empty if the input is empty or null
     * @throws NumberFormatException if the string has more than one character and cannot be parsed as an integer
     * @throws IllegalArgumentException if the string represents an integer outside the valid {@code char} range [0, 65535]
     * @see #valueOf(Object)
     * @see #stringOf(OptionalChar)
     */
    @SuppressWarnings("deprecation")
    @Override
    public OptionalChar valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(str));
    }

    /**
     * Retrieves a character value from a ResultSet at the specified column index and wraps it in an {@link OptionalChar}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     * ResultSet rs = Mockito.mock(ResultSet.class);
     * Mockito.when(rs.getString(1)).thenReturn("A");
     * Mockito.when(rs.getString(2)).thenReturn(null);
     *
     * // Column contains character 'A'
     * OptionalChar opt = type.get(rs, 1);
     * // Returns: OptionalChar.of('A')
     *
     * // Column contains SQL NULL
     * opt = type.get(rs, 2);
     * // Returns: OptionalChar.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalChar containing the character value, or empty if the column value is SQL NULL or an empty string
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        //    final Object result = rs.getObject(columnIndex);
        //
        //    if (result instanceof Character) {
        //        return OptionalChar.of((Character) result);
        //    } else if (result instanceof Integer) {
        //        return OptionalChar.of((char) ((Integer) result).intValue());
        //    } else {
        //        final String str = result == null ? null : result.toString();
        //        return Strings.isEmpty(str) ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(str));
        //    }

        final String result = rs.getString(columnIndex);

        if (result == null || result.isEmpty()) {
            return OptionalChar.empty();
        } else {
            return OptionalChar.of(result.charAt(0));
        }
    }

    /**
     * Retrieves a character value from a ResultSet using the specified column label and wraps it in an {@link OptionalChar}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     * ResultSet rs = Mockito.mock(ResultSet.class);
     * Mockito.when(rs.getString("grade")).thenReturn("A");
     * Mockito.when(rs.getString("middle_initial")).thenReturn(null);
     *
     * // Column "grade" contains character 'A'
     * OptionalChar opt = type.get(rs, "grade");
     * // Returns: OptionalChar.of('A')
     *
     * // Column "middle_initial" contains SQL NULL
     * opt = type.get(rs, "middle_initial");
     * // Returns: OptionalChar.empty()
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return an OptionalChar containing the character value, or empty if the column value is SQL NULL or an empty string
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public OptionalChar get(final ResultSet rs, final String columnName) throws SQLException {
        //    final Object result = rs.getObject(columnName);
        //
        //    if (result instanceof Character) {
        //        return OptionalChar.of((Character) result);
        //    } else if (result instanceof Integer) {
        //        return OptionalChar.of((char) ((Integer) result).intValue());
        //    } else {
        //        final String str = result == null ? null : result.toString();
        //        return Strings.isEmpty(str) ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(str));
        //    }

        final String result = rs.getString(columnName);

        if (result == null || result.isEmpty()) {
            return OptionalChar.empty();
        } else {
            return OptionalChar.of(result.charAt(0));
        }
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalChar}.
     * If the OptionalChar is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     * PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
     *
     * OptionalChar opt = OptionalChar.of('A');
     * type.set(stmt, 2, opt);
     * // Sets parameter to 'A' (stored as VARCHAR string)
     *
     * opt = OptionalChar.empty();
     * type.set(stmt, 2, opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalChar value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalChar x) throws SQLException {
        //    if (x == null || x.isEmpty()) {
        //        stmt.setNull(columnIndex, java.sql.Types.CHAR);
        //    } else {
        //        stmt.setInt(columnIndex, x.get());
        //    }

        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, Types.VARCHAR);
        } else {
            stmt.setString(columnIndex, String.valueOf(x.get()));
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalChar}.
     * If the OptionalChar is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<OptionalChar> type = TypeFactory.getType(OptionalChar.class);
     * CallableStatement stmt = Mockito.mock(CallableStatement.class);
     *
     * OptionalChar opt = OptionalChar.of('B');
     * type.set(stmt, "p_grade", opt);
     * // Sets parameter to 'B' (stored as VARCHAR string)
     *
     * opt = OptionalChar.empty();
     * type.set(stmt, "p_middle_initial", opt);
     * // Sets parameter to SQL NULL
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalChar value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalChar x) throws SQLException {
        //    if (x == null || x.isEmpty()) {
        //        stmt.setNull(parameterName, java.sql.Types.CHAR);
        //    } else {
        //        stmt.setInt(parameterName, x.get());
        //    }

        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, Types.VARCHAR);
        } else {
            stmt.setString(parameterName, String.valueOf(x.get()));
        }
    }

    /**
     * Appends the string representation of an {@link OptionalChar} to an Appendable.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalChar value to append
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
    public void appendTo(final Appendable appendable, final OptionalChar x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.get());
        }
    }

    /**
     * Writes the character representation of an {@link OptionalChar} to a CharacterWriter.
     * Writes {@code "null"} if {@code x} is {@code null} or empty. Otherwise, if the
     * serialization configuration specifies a non-zero character quotation, the contained
     * character is wrapped in that quotation; when the wrapped character is a single quote
     * and the configured quotation is also a single quote, an escaping backslash is emitted
     * before it. This method is typically used for JSON/XML serialization.
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
     * @param writer the CharacterWriter to write to
     * @param x the OptionalChar value to write
     * @param config the serialization configuration specifying character quotation; may be {@code null}
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final OptionalChar x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x.get());
            } else {
                writer.write(ch);

                if (x.get() == '\'' && ch == '\'') {
                    writer.write('\\');
                }

                writer.writeCharacter(x.get());
                writer.write(ch);
            }
        }
    }
}
