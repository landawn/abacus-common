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
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableLongType extends MutableType<MutableLong> {

    public static final String MUTABLE_LONG = MutableLong.class.getSimpleName();

    protected MutableLongType() {
        super(MUTABLE_LONG);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<MutableLong> clazz() {
        return MutableLong.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(MutableLong x) {
        return x == null ? null : N.stringOf(x.longValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableLong valueOf(String str) {
        return Strings.isEmpty(str) ? null : MutableLong.of(Numbers.toLong(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableLong get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableLong.of(rs.getLong(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableLong get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableLong.of(rs.getLong(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableLong x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.longValue());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableLong x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.longValue());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, MutableLong x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(String.valueOf(x.longValue()));
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
    public void writeCharacter(CharacterWriter writer, MutableLong x, JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.longValue());
        }
    }
}
