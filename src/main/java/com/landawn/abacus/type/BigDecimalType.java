/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.math.BigDecimal} values.
 * Provides serialization, deserialization, and JDBC operations for {@code BigDecimal} instances
 * with unlimited precision ({@link java.math.MathContext#UNLIMITED}).
 *
 * <p>String representation: produced by {@link java.math.BigDecimal#toString()}, which may use
 * scientific notation. Plain string format (no scientific notation) can be requested via
 * {@link com.landawn.abacus.parser.JsonXmlSerConfig#isWriteBigDecimalAsPlain()} in
 * {@link #serializeTo(CharacterWriter, BigDecimal, com.landawn.abacus.parser.JsonXmlSerConfig)}.</p>
 * <p>JDBC mapping: stored and retrieved using
 * {@link java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)} /
 * {@link java.sql.ResultSet#getBigDecimal(int)}.</p>
 *
 * @see java.math.BigDecimal
 */
public final class BigDecimalType extends NumberType<BigDecimal> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "BigDecimal"}).
     */
    public static final String BIG_DECIMAL = BigDecimal.class.getSimpleName();

    /**
     * Package-private constructor for {@code BigDecimalType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BigDecimalType() {
        super(BIG_DECIMAL);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code BigDecimal.class}
     */
    @Override
    public Class<BigDecimal> javaType() {
        return BigDecimal.class;
    }

    /**
     * Converts a {@link java.math.BigDecimal} to its string representation.
     * Delegates to {@link java.math.BigDecimal#toString()}, which may use scientific notation
     * for very large or very small values.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code BigDecimal} to convert; may be {@code null}
     * @return the string representation of the value (potentially in scientific notation),
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final BigDecimal x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Parses a string and returns a new {@link java.math.BigDecimal} with unlimited precision.
     * Leading and trailing whitespace is trimmed before parsing.
     * Parsing uses {@link java.math.MathContext#UNLIMITED}, so no rounding occurs.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return a new {@code BigDecimal} with unlimited precision,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if {@code str} cannot be parsed as a valid {@code BigDecimal}
     * @see #valueOf(Object)
     * @see #stringOf(BigDecimal)
     */
    @Override
    public BigDecimal valueOf(final String str) throws NumberFormatException {
        return Strings.isEmpty(str) ? null : new BigDecimal(str.trim(), MathContext.UNLIMITED);
    }

    /**
     * Parses a sub-sequence of a character array and returns a new {@link java.math.BigDecimal}
     * with unlimited precision ({@link java.math.MathContext#UNLIMITED}).
     * Leading and trailing whitespace within the range is ignored, using the same rule as
     * {@link String#trim()}, so a padded value parses exactly as it does through {@link #valueOf(String)}.
     *
     * @param cbuf the character array containing the decimal digits; may be {@code null}
     * @param offset the 0-based start position within {@code cbuf}
     * @param len the number of characters to parse
     * @return a new {@code BigDecimal} constructed from the specified characters,
     *         or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws NumberFormatException if the character sequence cannot be parsed as a valid {@code BigDecimal}, including a range that contains only
     *         whitespace (which trims to the empty string, exactly as {@code valueOf("   ")} does)
     */
    @Override
    public BigDecimal valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, NumberFormatException {
        if (cbuf == null || len == 0) {
            return null;
        }

        // Same trimming rule as String.trim() (used by valueOf(String)), applied without copying.
        int from = offset;
        int to = offset + len;

        while (from < to && cbuf[from] <= ' ') {
            from++;
        }

        while (to > from && cbuf[to - 1] <= ' ') {
            to--;
        }

        // A whitespace-only range is NOT null: valueOf(String) trims it to "" and rejects it, and so must this
        // overload, or the same padded text would parse from JSON and throw from XML again.
        return new BigDecimal(cbuf, from, to - from, MathContext.UNLIMITED);
    }

    /**
     * Retrieves a {@link java.math.BigDecimal} from a {@link java.sql.ResultSet} at the specified column index.
     * Delegates to {@link java.sql.ResultSet#getBigDecimal(int)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the decimal value
     * @return the {@code BigDecimal} value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public BigDecimal get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    /**
     * Retrieves a {@link java.math.BigDecimal} from a {@link java.sql.ResultSet} using the specified column label.
     * Delegates to {@link java.sql.ResultSet#getBigDecimal(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code BigDecimal} value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public BigDecimal get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getBigDecimal(columnName);
    }

    /**
     * Sets a {@link java.math.BigDecimal} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Delegates to {@link java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code BigDecimal} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigDecimal x) throws NullPointerException, SQLException {
        stmt.setBigDecimal(columnIndex, x);
    }

    /**
     * Sets a named {@link java.math.BigDecimal} parameter on a {@link java.sql.CallableStatement}.
     * Delegates to {@link java.sql.CallableStatement#setBigDecimal(String, java.math.BigDecimal)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code BigDecimal} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigDecimal x) throws NullPointerException, SQLException {
        stmt.setBigDecimal(parameterName, x);
    }

    /**
     * Writes a {@link java.math.BigDecimal} value to a {@link CharacterWriter}.
     * Writes {@code 0} for a {@code null} value when the config requests {@code writeNullNumberAsZero};
     * otherwise writes the literal {@code "null"} character array if {@code x} is {@code null}.
     * When {@code config} is non-{@code null} and
     * {@link com.landawn.abacus.parser.JsonXmlSerConfig#isWriteBigDecimalAsPlain()} returns {@code true},
     * the value is written using {@link java.math.BigDecimal#toPlainString()} (no scientific notation);
     * otherwise {@link java.math.BigDecimal#toString()} is used.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes an unquoted numeric literal to the
     * {@code CharacterWriter}, using plain notation when requested by config.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML numeric output, whereas
     * {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code BigDecimal} value to write; may be {@code null}
     * @param config the serialization configuration controlling output format; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final BigDecimal x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            if (config != null && config.isWriteNullNumberAsZero()) {
                writer.write('0');
            } else {
                writer.write(NULL_CHAR_ARRAY);
            }
        } else {
            if (config != null && config.isWriteBigDecimalAsPlain()) {
                writer.writeCharacter(x.toPlainString());
            } else {
                writer.writeCharacter(stringOf(x));
            }
        }
    }
}
