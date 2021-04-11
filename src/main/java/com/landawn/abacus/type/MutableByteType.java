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
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableByteType extends MutableType<MutableByte> {

    public static final String MUTABLE_BYTE = MutableByte.class.getSimpleName();

    protected MutableByteType() {
        super(MUTABLE_BYTE);
    }

    @Override
    public Class<MutableByte> clazz() {
        return MutableByte.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(MutableByte x) {
        return x == null ? null : N.stringOf(x.byteValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableByte valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : MutableByte.of(Numbers.toByte(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableByte get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableByte.of(rs.getByte(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableByte get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableByte.of(rs.getByte(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableByte x) throws SQLException {
        stmt.setByte(columnIndex, (x == null) ? 0 : x.byteValue());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableByte x) throws SQLException {
        stmt.setByte(parameterName, (x == null) ? 0 : x.byteValue());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, MutableByte x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.byteValue());
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
    public void writeCharacter(CharacterWriter writer, MutableByte x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.byteValue());
        }
    }
}
