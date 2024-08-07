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
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableBooleanType extends MutableType<MutableBoolean> {

    public static final String MUTABLE_BOOLEAN = MutableBoolean.class.getSimpleName();

    protected MutableBooleanType() {
        super(MUTABLE_BOOLEAN);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<MutableBoolean> clazz() {
        return MutableBoolean.class;
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
    public String stringOf(MutableBoolean x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableBoolean valueOf(String str) {
        return Strings.isEmpty(str) ? null : MutableBoolean.of(Strings.parseBoolean(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableBoolean get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableBoolean.of(rs.getBoolean(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableBoolean get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableBoolean.of(rs.getBoolean(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableBoolean x) throws SQLException {
        stmt.setBoolean(columnIndex, (x == null) ? false : x.value());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableBoolean x) throws SQLException {
        stmt.setBoolean(parameterName, (x == null) ? false : x.value());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, MutableBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.value() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, MutableBoolean x, JSONXMLSerializationConfig<?> config) throws IOException {
        writer.write((x == null) ? NULL_CHAR_ARRAY : (x.value() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
