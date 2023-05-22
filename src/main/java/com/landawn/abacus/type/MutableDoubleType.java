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
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class MutableDoubleType extends MutableType<MutableDouble> {

    public static final String MUTABLE_DOUBLE = MutableDouble.class.getSimpleName();

    protected MutableDoubleType() {
        super(MUTABLE_DOUBLE);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<MutableDouble> clazz() {
        return MutableDouble.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(MutableDouble x) {
        return x == null ? null : N.stringOf(x.doubleValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableDouble valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : MutableDouble.of(Numbers.toDouble(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableDouble get(ResultSet rs, int columnIndex) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableDouble get(ResultSet rs, String columnLabel) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableDouble x) throws SQLException {
        stmt.setDouble(columnIndex, (x == null) ? 0 : x.doubleValue());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableDouble x) throws SQLException {
        stmt.setDouble(parameterName, (x == null) ? 0 : x.doubleValue());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, MutableDouble x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.doubleValue());
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
    public void writeCharacter(CharacterWriter writer, MutableDouble x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.doubleValue());
        }
    }
}
