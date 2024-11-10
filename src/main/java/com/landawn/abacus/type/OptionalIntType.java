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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalInt;

public class OptionalIntType extends AbstractOptionalType<OptionalInt> {

    public static final String OPTIONAL_INT = OptionalInt.class.getSimpleName();

    protected OptionalIntType() {
        super(OPTIONAL_INT);
    }

    @Override
    public Class<OptionalInt> clazz() {
        return OptionalInt.class;
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
    public String stringOf(final OptionalInt x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalInt valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalInt.empty() : OptionalInt.of(Numbers.toInt(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalInt get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
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
    public void set(final CallableStatement stmt, final String parameterName, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.get());
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalInt x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
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
    public void writeCharacter(final CharacterWriter writer, final OptionalInt x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.get());
        }
    }
}
