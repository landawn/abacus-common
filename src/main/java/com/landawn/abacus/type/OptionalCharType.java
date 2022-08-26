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
import com.landawn.abacus.util.u.OptionalChar;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class OptionalCharType extends AbstractOptionalType<OptionalChar> {

    public static final String OPTIONAL_CHAR = OptionalChar.class.getSimpleName();

    protected OptionalCharType() {
        super(OPTIONAL_CHAR);
    }

    @Override
    public Class<OptionalChar> clazz() {
        return OptionalChar.class;
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
    public String stringOf(OptionalChar x) {
        return x == null || !x.isPresent() ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalChar valueOf(String str) {
        return N.isNullOrEmpty(str) ? OptionalChar.empty() : OptionalChar.of(N.parseChar(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalChar get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalChar.empty()
                : OptionalChar.of(obj instanceof Character ? (Character) obj
                        : (obj instanceof Integer ? (char) ((Integer) obj).intValue() : N.parseChar(obj.toString())));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalChar get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalChar.empty()
                : OptionalChar.of(obj instanceof Character ? (Character) obj
                        : (obj instanceof Integer ? (char) ((Integer) obj).intValue() : N.parseChar(obj.toString())));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, OptionalChar x) throws SQLException {
        if (x == null || !x.isPresent()) {
            stmt.setNull(columnIndex, java.sql.Types.CHAR);
        } else {
            stmt.setInt(columnIndex, x.get());
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
    public void set(CallableStatement stmt, String parameterName, OptionalChar x) throws SQLException {
        if (x == null || !x.isPresent()) {
            stmt.setNull(parameterName, java.sql.Types.CHAR);
        } else {
            stmt.setInt(parameterName, x.get());
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, OptionalChar x) throws IOException {
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
    public void writeCharacter(CharacterWriter writer, OptionalChar x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x.get());
            } else {
                writer.write(ch);
                writer.writeCharacter(x.get());
                writer.write(ch);
            }
        }
    }
}
