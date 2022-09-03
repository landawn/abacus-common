/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.u.OptionalDouble;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
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
     * @return true, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(OptionalDouble x) {
        return x == null || x.isEmpty() ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalDouble valueOf(String str) {
        return N.isNullOrEmpty(str) ? OptionalDouble.empty() : OptionalDouble.of(Numbers.toDouble(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalDouble get(ResultSet rs, int columnIndex) throws SQLException {
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
    public OptionalDouble get(ResultSet rs, String columnLabel) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, OptionalDouble x) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, x.get());
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, OptionalDouble x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.get());
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
    public void writeCharacter(CharacterWriter writer, OptionalDouble x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
