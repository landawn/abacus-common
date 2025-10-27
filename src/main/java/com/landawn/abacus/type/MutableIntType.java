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
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class MutableIntType extends NumberType<MutableInt> {

    public static final String MUTABLE_INT = MutableInt.class.getSimpleName();

    protected MutableIntType() {
        super(MUTABLE_INT);
    }

    /**
     * Returns the Class object representing the MutableInt type.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * Class&lt;MutableInt&gt; clazz = type.clazz();
     * // clazz equals MutableInt.class
     * }</pre>
     *
     * @return The Class object for MutableInt
     */
    @Override
    public Class<MutableInt> clazz() {
        return MutableInt.class;
    }

    /**
     * Converts a MutableInt object to its string representation.
     * The integer value is converted to a decimal string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * MutableInt mi = MutableInt.of(42);
     * String str = type.stringOf(mi);
     * // str equals "42"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * @param x The MutableInt object to convert
     * @return The string representation of the integer value, or {@code null} if the input is null
     @MayReturnNull
     */
    @Override
    public String stringOf(final MutableInt x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableInt object.
     * The string is parsed as an integer value.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * MutableInt mi = type.valueOf("123");
     * // mi.value() equals 123
     *
     * MutableInt nullMi = type.valueOf(null);
     * // nullMi equals null
     *
     * MutableInt emptyMi = type.valueOf("");
     * // emptyMi equals null
     * }</pre>
     *
     * @param str The string to parse
     * @return A MutableInt containing the parsed value, or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as an integer
     @MayReturnNull
     */
    @Override
    public MutableInt valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableInt.of(Numbers.toInt(str));
    }

    /**
     * Retrieves a MutableInt value from a ResultSet at the specified column index.
     * The database integer value is wrapped in a MutableInt object.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * ResultSet rs = statement.executeQuery("SELECT age FROM users");
     * if (rs.next()) {
     *     MutableInt age = type.get(rs, 1);
     *     // age contains the integer value from the first column
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableInt containing the retrieved value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableInt.of(rs.getInt(columnIndex));
    }

    /**
     * Retrieves a MutableInt value from a ResultSet using the specified column label.
     * The database integer value is wrapped in a MutableInt object.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * ResultSet rs = statement.executeQuery("SELECT age FROM users");
     * if (rs.next()) {
     *     MutableInt age = type.get(rs, "age");
     *     // age contains the integer value from the named column
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableInt containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableInt get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableInt.of(rs.getInt(columnLabel));
    }

    /**
     * Sets a MutableInt parameter in a PreparedStatement at the specified position.
     * If the MutableInt is {@code null}, 0 is stored. Otherwise, the wrapped integer value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (age) VALUES (?)");
     * MutableInt age = MutableInt.of(25);
     * type.set(stmt, 1, age);
     * stmt.executeUpdate();
     *
     * // For {@code null} value
     * type.set(stmt, 1, null);
     * // This sets the parameter to 0
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableInt value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableInt x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableInt parameter in a CallableStatement using the specified parameter name.
     * If the MutableInt is {@code null}, 0 is stored. Otherwise, the wrapped integer value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * CallableStatement stmt = conn.prepareCall("{call update_counter(?)}");
     * MutableInt counter = MutableInt.of(100);
     * type.set(stmt, "counter", counter);
     * stmt.execute();
     *
     * // For {@code null} value
     * type.set(stmt, "counter", null);
     * // This sets the parameter to 0
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableInt value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableInt x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableInt to an Appendable.
     * The value is written as a decimal string or "null".
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * StringBuilder sb = new StringBuilder();
     * MutableInt mi = MutableInt.of(42);
     * type.appendTo(sb, mi);
     * // sb.toString() equals "42"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The MutableInt to append
     * @throws IOException if an I/O error occurs while appending
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
     * Writes the character representation of a MutableInt to a CharacterWriter.
     * The value is written as numeric characters or the {@code null} character array.
     * This method is optimized for character-based writing and uses a specialized writeInt method.
     *
     * <p><b>Usage Examples:</b></p>
     * MutableIntType type = new MutableIntType();
     * CharacterWriter writer = new CharacterWriter();
     * MutableInt mi = MutableInt.of(12345);
     * type.writeCharacter(writer, mi, null);
     * // Writes: 12345
     *
     * type.writeCharacter(writer, {@code null}, null);
     * // Writes: null
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableInt to write
     * @param config The serialization configuration (currently unused for integer values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableInt x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.value());
        }
    }
}
