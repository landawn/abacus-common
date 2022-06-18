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
import com.landawn.abacus.util.u.OptionalByte;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class OptionalByteType extends AbstractOptionalType<OptionalByte> {

    public static final String OPTIONAL_BYTE = OptionalByte.class.getSimpleName();

    protected OptionalByteType() {
        super(OPTIONAL_BYTE);
    }

    @Override
    public Class<OptionalByte> clazz() {
        return OptionalByte.class;
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
    public String stringOf(OptionalByte x) {
        return x == null || !x.isPresent() ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalByte valueOf(String str) {
        return N.isNullOrEmpty(str) ? OptionalByte.empty() : OptionalByte.of(Numbers.toByte(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalByte get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj.toString()));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalByte get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj.toString()));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, OptionalByte x) throws SQLException {
        if (x == null || !x.isPresent()) {
            stmt.setNull(columnIndex, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(columnIndex, x.get());
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
    public void set(CallableStatement stmt, String parameterName, OptionalByte x) throws SQLException {
        if (x == null || !x.isPresent()) {
            stmt.setNull(parameterName, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.get());
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, OptionalByte x) throws IOException {
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
    public void writeCharacter(CharacterWriter writer, OptionalByte x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
