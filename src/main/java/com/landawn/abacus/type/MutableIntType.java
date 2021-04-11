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
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableIntType extends MutableType<MutableInt> {

    public static final String MUTABLE_INT = MutableInt.class.getSimpleName();

    protected MutableIntType() {
        super(MUTABLE_INT);
    }

    @Override
    public Class<MutableInt> clazz() {
        return MutableInt.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(MutableInt x) {
        return x == null ? null : N.stringOf(x.intValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableInt valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : MutableInt.of(Numbers.toInt(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableInt get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableInt.of(rs.getInt(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableInt get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableInt.of(rs.getInt(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableInt x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.intValue());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableInt x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.intValue());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, MutableInt x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.intValue());
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
    public void writeCharacter(CharacterWriter writer, MutableInt x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.intValue());
        }
    }
}
