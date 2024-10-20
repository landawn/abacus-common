/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <T>
 */
@SuppressWarnings("java:S2160")
public class JdkOptionalType<T> extends AbstractOptionalType<Optional<T>> {

    public static final String OPTIONAL = "JdkOptional";

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    protected JdkOptionalType(final String parameterTypeName) {
        super(OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        declaringName = OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class<Optional<T>> clazz() {
        return (Class) Optional.class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<T> getElementType() {
        return elementType;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<T>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks if is generic type.
     *
     * @return {@code true}, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Optional<T> x) {
        return (x == null || x.isEmpty()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get()); //NOSONAR
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Optional<T> valueOf(final String str) {
        return str == null ? (Optional<T>) Optional.empty() : Optional.ofNullable(elementType.valueOf(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Optional<T> get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = getColumnValue(rs, columnIndex, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, elementType));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Optional<T> get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = getColumnValue(rs, columnLabel, elementType.clazz());

        return obj == null ? (Optional<T>) Optional.empty()
                : Optional.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, elementType));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Optional<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Optional<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isEmpty()) ? null : x.get()); //NOSONAR
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Optional<T> x) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            appendable.append(NULL_STRING);
        } else {
            // elementType.write(writer, x.get());
            N.typeOf(x.get().getClass()).appendTo(appendable, x.get());
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
    public void writeCharacter(final CharacterWriter writer, final Optional<T> x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) { //NOSONAR
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            N.typeOf(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
