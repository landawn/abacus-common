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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalDouble;

public class OptionalDoubleType extends AbstractOptionalType<OptionalDouble> {

    public static final String OPTIONAL_DOUBLE = OptionalDouble.class.getSimpleName();

    protected OptionalDoubleType() {
        super(OPTIONAL_DOUBLE);
    }

    @Override
    public Class<OptionalDouble> clazz() {
        return OptionalDouble.class;
    }

    /**
     * Checks if is comparable.
     *
     * @return {@code true}, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Checks if is non quoted csv type.
     *
     * @return {@code true}, if is non quoted csv type
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final OptionalDouble x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalDouble valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalDouble.empty() : OptionalDouble.of(Numbers.toDouble(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(columnIndex, x.get());
        }
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, x.get());
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalDouble x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalDouble x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
