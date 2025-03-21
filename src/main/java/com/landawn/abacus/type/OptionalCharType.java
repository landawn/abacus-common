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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalChar;

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
    public String stringOf(final OptionalChar x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalChar valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalChar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        if (obj instanceof Character) {
            return OptionalChar.of((Character) obj);
        } else if (obj instanceof Integer) {
            return OptionalChar.of((char) ((Integer) obj).intValue());
        } else {
            return obj == null ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(obj.toString()));
        }
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalChar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        if (obj instanceof Character) {
            return OptionalChar.of((Character) obj);
        } else if (obj instanceof Integer) {
            return OptionalChar.of((char) ((Integer) obj).intValue());
        } else {
            return obj == null ? OptionalChar.empty() : OptionalChar.of(Strings.parseChar(obj.toString()));
        }
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalChar x) throws SQLException {
        if (x == null || x.isEmpty()) {
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
    public void set(final CallableStatement stmt, final String parameterName, final OptionalChar x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.CHAR);
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
    public void appendTo(final Appendable appendable, final OptionalChar x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.get());
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
    public void writeCharacter(final CharacterWriter writer, final OptionalChar x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
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
