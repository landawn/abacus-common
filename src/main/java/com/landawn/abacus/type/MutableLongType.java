/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MutableLong} objects, providing serialization, deserialization,
 * and database interaction capabilities for mutable long wrapper objects.
 */
public class MutableLongType extends NumberType<MutableLong> {

    public static final String MUTABLE_LONG = MutableLong.class.getSimpleName();

    /**
     * Constructs a MutableLongType.
     * This constructor initializes the type handler for MutableLong objects.
     */
    protected MutableLongType() {
        super(MUTABLE_LONG);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * Class<MutableLong> clazz = type.clazz();
     * // clazz equals MutableLong.class
     * }</pre>
     *
     * @return the {@link MutableLong} class object
     */
    @Override
    public Class<MutableLong> clazz() {
        return MutableLong.class;
    }

    /**
     * Converts a {@link MutableLong} object to its string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * MutableLong ml = MutableLong.of(9876543210L);
     * String str = type.stringOf(ml);
     * // str equals "9876543210"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * @param x the MutableLong object to convert
     * @return the string representation of the long value, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final MutableLong x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Converts a string representation to a {@link MutableLong} object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * MutableLong ml = type.valueOf("9876543210");
     * // ml.value() equals 9876543210L
     *
     * MutableLong nullMl = type.valueOf(null);
     * // nullMl equals null
     *
     * MutableLong emptyMl = type.valueOf("");
     * // emptyMl equals null
     * }</pre>
     *
     * @param str the string to convert
     * @return a MutableLong containing the parsed long value, or {@code null} if the input string is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a long
     */
    @Override
    public MutableLong valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableLong.of(Numbers.toLong(str));
    }

    /**
     * Retrieves a {@link MutableLong} value from a ResultSet at the specified column index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * ResultSet rs = statement.executeQuery("SELECT user_id FROM users");
     * if (rs.next()) {
     *     MutableLong userId = type.get(rs, 1);
     *     // userId contains the long value from the first column
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a MutableLong containing the long value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public MutableLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableLong.of(rs.getLong(columnIndex));
    }

    /**
     * Retrieves a {@link MutableLong} value from a ResultSet using the specified column label.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * ResultSet rs = statement.executeQuery("SELECT user_id FROM users");
     * if (rs.next()) {
     *     MutableLong userId = type.get(rs, "user_id");
     *     // userId contains the long value from the "user_id" column
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return a MutableLong containing the long value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public MutableLong get(final ResultSet rs, final String columnName) throws SQLException {
        return MutableLong.of(rs.getLong(columnName));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value of a {@link MutableLong}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * PreparedStatement stmt = conn.prepareStatement("UPDATE users SET user_id = ? WHERE email = ?");
     * MutableLong userId = MutableLong.of(1234567890L);
     * type.set(stmt, 1, userId);
     * stmt.executeUpdate();
     *
     * // For null value
     * type.set(stmt, 1, null);
     * // This sets the parameter to 0
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the MutableLong value to set, or null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableLong x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value of a {@link MutableLong}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * CallableStatement cstmt = conn.prepareCall("{call update_user(?, ?)}");
     * MutableLong userId = MutableLong.of(1234567890L);
     * type.set(cstmt, "user_id", userId);
     * cstmt.execute();
     *
     * // For null value
     * type.set(cstmt, "user_id", null);
     * // This sets the parameter to 0
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableLong value to set, or null
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableLong x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a {@link MutableLong} to an Appendable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * StringBuilder sb = new StringBuilder();
     * MutableLong ml = MutableLong.of(999999999L);
     * type.appendTo(sb, ml);
     * // sb.toString() equals "999999999"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the MutableLong value to append
     * @throws IOException if an I/O error occurs during the append operation
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
     * Writes the character representation of a {@link MutableLong} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableLong> type = TypeFactory.getType(MutableLong.class);
     * CharacterWriter writer = new CharacterWriter();
     * MutableLong ml = MutableLong.of(123456789012345L);
     * type.writeCharacter(writer, ml, null);
     * // Writes: 123456789012345
     *
     * type.writeCharacter(writer, null, null);
     * // Writes: null
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the MutableLong value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableLong x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}