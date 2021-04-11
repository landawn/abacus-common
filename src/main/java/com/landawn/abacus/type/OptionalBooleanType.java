/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;
import java.io.Writer;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalBoolean;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class OptionalBooleanType extends AbstractOptionalType<OptionalBoolean> {

    public static final String OPTIONAL_BOOLEAN = OptionalBoolean.class.getSimpleName();

    protected OptionalBooleanType() {
        super(OPTIONAL_BOOLEAN);
    }

    @Override
    public Class<OptionalBoolean> clazz() {
        return OptionalBoolean.class;
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
     * Checks if is optional or nullable.
     *
     * @return true, if is optional or nullable
     */
    @Override
    public boolean isOptionalOrNullable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(OptionalBoolean x) {
        return x == null || x.isPresent() == false ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalBoolean valueOf(String str) {
        return N.isNullOrEmpty(str) ? OptionalBoolean.empty() : OptionalBoolean.of(N.parseBoolean(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalBoolean get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.parseBoolean(obj.toString()));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalBoolean get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.parseBoolean(obj.toString()));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, OptionalBoolean x) throws SQLException {
        if (x == null || x.isPresent() == false) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x.get());
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
    public void set(CallableStatement stmt, String parameterName, OptionalBoolean x) throws SQLException {
        if (x == null || x.isPresent() == false) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x.get());
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, OptionalBoolean x) throws IOException {
        writer.write((x == null || x.isPresent() == false) ? NULL_CHAR_ARRAY : (x.get() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, OptionalBoolean x, SerializationConfig<?> config) throws IOException {
        writer.write((x == null || x.isPresent() == false) ? NULL_CHAR_ARRAY : (x.get() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
