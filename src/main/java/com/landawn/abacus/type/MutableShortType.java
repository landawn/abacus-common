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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableShortType extends MutableType<MutableShort> {

    public static final String MUTABLE_SHORT = MutableShort.class.getSimpleName();

    protected MutableShortType() {
        super(MUTABLE_SHORT);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<MutableShort> clazz() {
        return MutableShort.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(MutableShort x) {
        return x == null ? null : N.stringOf(x.shortValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableShort valueOf(String str) {
        return Strings.isEmpty(str) ? null : MutableShort.of(Numbers.toShort(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableShort get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableShort.of(rs.getShort(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableShort get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableShort.of(rs.getShort(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableShort x) throws SQLException {
        stmt.setShort(columnIndex, (x == null) ? 0 : x.shortValue());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableShort x) throws SQLException {
        stmt.setShort(parameterName, (x == null) ? 0 : x.shortValue());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, MutableShort x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.shortValue());
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
    public void writeCharacter(CharacterWriter writer, MutableShort x, JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.shortValue());
        }
    }
}
