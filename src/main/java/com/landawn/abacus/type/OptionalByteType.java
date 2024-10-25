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
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalByte;

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
     * @return {@code true}, if is comparable
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
    public String stringOf(final OptionalByte x) {
        return x == null || x.isEmpty() ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalByte valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalByte.empty() : OptionalByte.of(Numbers.toByte(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalByte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalByte get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
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
    public void set(final CallableStatement stmt, final String parameterName, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.get());
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalByte x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(String.valueOf(x.get()));
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
    public void writeCharacter(final CharacterWriter writer, final OptionalByte x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
