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
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MutableFloat} objects. This class provides serialization,
 * deserialization, and database operations for MutableFloat instances, which are
 * mutable wrappers around primitive float values.
 */
public class MutableFloatType extends NumberType<MutableFloat> {

    /**
     * The type name identifier for MutableFloat type.
     */
    public static final String MUTABLE_FLOAT = MutableFloat.class.getSimpleName();

    /**
     * Constructs a MutableFloatType.
     * This constructor initializes the type handler for MutableFloat objects.
     */
    protected MutableFloatType() {
        super(MUTABLE_FLOAT);
    }

    /**
     * Returns the Class object representing the MutableFloat type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * Class<MutableFloat> clazz = type.clazz();
     * // clazz equals MutableFloat.class
     * }</pre>
     *
     * @return the Class object for MutableFloat
     */
    @Override
    public Class<MutableFloat> clazz() {
        return MutableFloat.class;
    }

    /**
     * Converts a MutableFloat object to its string representation.
     * Returns the string representation of the wrapped float value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * MutableFloat mf = MutableFloat.of(3.14f);
     * String str = type.stringOf(mf);
     * // str equals "3.14"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * @param x the MutableFloat object to convert
     * @return the string representation of the float value, or {@code null} if x is null
     */
    @Override
    public String stringOf(final MutableFloat x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Creates a MutableFloat object from its string representation.
     * Parses the string as a float value and wraps it in a MutableFloat.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * MutableFloat mf = type.valueOf("123.45");
     * // mf.value() equals 123.45f
     *
     * MutableFloat nullMf = type.valueOf(null);
     * // nullMf equals null
     *
     * MutableFloat emptyMf = type.valueOf("");
     * // emptyMf equals null
     * }</pre>
     *
     * @param str the string to parse
     * @return a MutableFloat containing the parsed value, or {@code null} if str is empty
     * @throws NumberFormatException if the string cannot be parsed as a float
     */
    @Override
    public MutableFloat valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableFloat.of(Numbers.toFloat(str));
    }

    /**
     * Retrieves a MutableFloat value from the specified column in the ResultSet.
     * Reads a float value from the database and wraps it in a MutableFloat object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * ResultSet rs = statement.executeQuery("SELECT price FROM products");
     * if (rs.next()) {
     *     MutableFloat price = type.get(rs, 1);
     *     // price contains the float value from the first column
     * }
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return a MutableFloat containing the retrieved float value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public MutableFloat get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableFloat.of(rs.getFloat(columnIndex));
    }

    /**
     * Retrieves a MutableFloat value from the specified column in the ResultSet.
     * Reads a float value from the database and wraps it in a MutableFloat object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * ResultSet rs = statement.executeQuery("SELECT price FROM products");
     * if (rs.next()) {
     *     MutableFloat price = type.get(rs, "price");
     *     // price contains the float value from the "price" column
     * }
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnLabel the label of the column to retrieve
     * @return a MutableFloat containing the retrieved float value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public MutableFloat get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableFloat.of(rs.getFloat(columnLabel));
    }

    /**
     * Sets a MutableFloat value at the specified parameter index in the PreparedStatement.
     * Extracts the float value from the MutableFloat and sets it in the statement.
     * If the MutableFloat is {@code null}, sets 0.0f as the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (price) VALUES (?)");
     * MutableFloat price = MutableFloat.of(99.99f);
     * type.set(stmt, 1, price);
     * stmt.executeUpdate();
     *
     * // For null value
     * type.set(stmt, 1, null);
     * // This sets the parameter to 0.0f
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the MutableFloat value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableFloat x) throws SQLException {
        stmt.setFloat(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableFloat value for the specified parameter name in the CallableStatement.
     * Extracts the float value from the MutableFloat and sets it in the statement.
     * If the MutableFloat is {@code null}, sets 0.0f as the value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * CallableStatement cstmt = conn.prepareCall("{call update_price(?, ?)}");
     * MutableFloat newPrice = MutableFloat.of(199.99f);
     * type.set(cstmt, "price", newPrice);
     * cstmt.execute();
     *
     * // For null value
     * type.set(cstmt, "price", null);
     * // This sets the parameter to 0.0f
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableFloat value to set, may be null
     * @throws SQLException if a database access error occurs or the parameterName is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableFloat x) throws SQLException {
        stmt.setFloat(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableFloat to the given Appendable.
     * Writes "null" if the MutableFloat is {@code null}, otherwise writes the string
     * representation of the float value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * StringBuilder sb = new StringBuilder();
     * MutableFloat mf = MutableFloat.of(42.5f);
     * type.appendTo(sb, mf);
     * // sb.toString() equals "42.5"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the MutableFloat value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableFloat x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a MutableFloat to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes {@code null} characters if the
     * MutableFloat is {@code null}, otherwise writes the float value directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableFloat> type = TypeFactory.getType(MutableFloat.class);
     * CharacterWriter writer = new CharacterWriter();
     * MutableFloat mf = MutableFloat.of(3.14159f);
     * type.writeCharacter(writer, mf, null);
     * // Writes: 3.14159
     *
     * type.writeCharacter(writer, null, null);
     * // Writes: null
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the MutableFloat value to write
     * @param config the serialization configuration (may be used for formatting)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableFloat x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
