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
import com.landawn.abacus.util.u.OptionalBoolean;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class OptionalBooleanType extends AbstractOptionalType<OptionalBoolean> {

    public static final String OPTIONAL_BOOLEAN = OptionalBoolean.class.getSimpleName();

    protected OptionalBooleanType() {
        super(OPTIONAL_BOOLEAN);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<OptionalBoolean> clazz() {
        return OptionalBoolean.class;
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
    public String stringOf(OptionalBoolean x) {
        return x == null || x.isEmpty() ? null : String.valueOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public OptionalBoolean valueOf(String str) {
        return Strings.isEmpty(str) ? OptionalBoolean.empty() : OptionalBoolean.of(Strings.parseBoolean(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalBoolean get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.convert(obj, Boolean.class));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public OptionalBoolean get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.convert(obj, Boolean.class));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, OptionalBoolean x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x.get());
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
    public void set(CallableStatement stmt, String parameterName, OptionalBoolean x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x.get());
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, OptionalBoolean x) throws IOException {
        appendable.append((x == null || x.isEmpty()) ? NULL_STRING : (x.get() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, OptionalBoolean x, JSONXMLSerializationConfig<?> config) throws IOException {
        writer.write((x == null || x.isEmpty()) ? NULL_CHAR_ARRAY : (x.get() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
