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
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 */
public class MutableByteType extends NumberType<MutableByte> {

    public static final String MUTABLE_BYTE = MutableByte.class.getSimpleName();

    protected MutableByteType() {
        super(MUTABLE_BYTE);
    }

    /**
     *
     *
     * @return
     */
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
    public String stringOf(final MutableByte x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableByte valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableByte.of(Numbers.toByte(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableByte get(final ResultSet rs, final int columnIndex) throws SQLException {
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
    public MutableByte get(final ResultSet rs, final String columnLabel) throws SQLException {
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
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableByte x) throws SQLException {
        stmt.setByte(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableByte x) throws SQLException {
        stmt.setByte(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableByte x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(String.valueOf(x.value()));
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
    public void writeCharacter(final CharacterWriter writer, final MutableByte x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
