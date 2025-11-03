/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

class MutableDoubleType extends NumberType<MutableDouble> {

    public static final String MUTABLE_DOUBLE = MutableDouble.class.getSimpleName();

    protected MutableDoubleType() {
        super(MUTABLE_DOUBLE);
    }

    /**
     * Returns the Class object representing the MutableDouble type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * Class&lt;MutableDouble&gt; clazz = type.clazz();
     * // clazz equals MutableDouble.class
     * }</pre>
     *
     * @return The Class object for MutableDouble
     */
    @Override
    public Class<MutableDouble> clazz() {
        return MutableDouble.class;
    }

    /**
     * Converts a MutableDouble object to its string representation.
     * The double value is converted to a decimal string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * MutableDouble md = MutableDouble.of(3.14);
     * String str = type.stringOf(md);
     * // str equals "3.14"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * @param x The MutableDouble object to convert
     * @return The string representation of the double value, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final MutableDouble x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableDouble object.
     * The string is parsed as a double value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * MutableDouble md = type.valueOf("123.45");
     * // md.value() equals 123.45
     *
     * MutableDouble nullMd = type.valueOf(null);
     * // nullMd equals null
     *
     * MutableDouble emptyMd = type.valueOf("");
     * // emptyMd equals null
     * }</pre>
     *
     * @param str The string to parse
     * @return A MutableDouble containing the parsed value, or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a double
     */
    @Override
    public MutableDouble valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableDouble.of(Numbers.toDouble(str));
    }

    /**
     * Retrieves a MutableDouble value from a ResultSet at the specified column index.
     * The database double value is wrapped in a MutableDouble object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * ResultSet rs = statement.executeQuery("SELECT price FROM products");
     * if (rs.next()) {
     *     MutableDouble price = type.get(rs, 1);
     *     // price contains the double value from the first column
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableDouble containing the retrieved value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableDouble get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnIndex));
    }

    /**
     * Retrieves a MutableDouble value from a ResultSet using the specified column label.
     * The database double value is wrapped in a MutableDouble object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * ResultSet rs = statement.executeQuery("SELECT price FROM products");
     * if (rs.next()) {
     *     MutableDouble price = type.get(rs, "price");
     *     // price contains the double value from the named column
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableDouble containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableDouble get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnLabel));
    }

    /**
     * Sets a MutableDouble parameter in a PreparedStatement at the specified position.
     * If the MutableDouble is {@code null}, 0.0 is stored. Otherwise, the wrapped double value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (price) VALUES (?)");
     * MutableDouble price = MutableDouble.of(99.99);
     * type.set(stmt, 1, price);
     * stmt.executeUpdate();
     *
     * // For {@code null} value
     * type.set(stmt, 1, null);
     * // This sets the parameter to 0.0
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableDouble value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableDouble x) throws SQLException {
        stmt.setDouble(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableDouble parameter in a CallableStatement using the specified parameter name.
     * If the MutableDouble is {@code null}, 0.0 is stored. Otherwise, the wrapped double value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * CallableStatement stmt = conn.prepareCall("{call calculate_total(?)}");
     * MutableDouble amount = MutableDouble.of(250.75);
     * type.set(stmt, "amount", amount);
     * stmt.execute();
     *
     * // For {@code null} value
     * type.set(stmt, "amount", null);
     * // This sets the parameter to 0.0
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableDouble value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableDouble x) throws SQLException {
        stmt.setDouble(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableDouble to an Appendable.
     * The value is written as a decimal string or "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * StringBuilder sb = new StringBuilder();
     * MutableDouble md = MutableDouble.of(42.5);
     * type.appendTo(sb, md);
     * // sb.toString() equals "42.5"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The MutableDouble to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableDouble x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a MutableDouble to a CharacterWriter.
     * The value is written as numeric characters or the {@code null} character array.
     * This method is optimized for character-based writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableDouble> type = TypeFactory.getType(MutableDouble.class);
     * CharacterWriter writer = new CharacterWriter();
     * MutableDouble md = MutableDouble.of(3.14159);
     * type.writeCharacter(writer, md, null);
     * // Writes: 3.14159
     *
     * type.writeCharacter(writer, {@code null}, null);
     * // Writes: null
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableDouble to write
     * @param config The serialization configuration (currently unused for double values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableDouble x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
