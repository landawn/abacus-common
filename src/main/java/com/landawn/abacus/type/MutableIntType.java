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
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class MutableIntType extends NumberType<MutableInt> {

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
    public String stringOf(final MutableInt x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableInt valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableInt.of(Numbers.toInt(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableInt get(final ResultSet rs, final int columnIndex) throws SQLException {
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
    public MutableInt get(final ResultSet rs, final String columnLabel) throws SQLException {
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
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableInt x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableInt x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableInt x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
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
    public void writeCharacter(final CharacterWriter writer, final MutableInt x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.value());
        }
    }
}
