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
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Optional;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class OptionalType<T> extends AbstractOptionalType<Optional<T>> {

    public static final String OPTIONAL = Optional.class.getSimpleName();

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    protected OptionalType(String parameterTypeName) {
        super(OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        this.declaringName = OPTIONAL + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        this.parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        this.elementType = parameterTypes[0];
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
     * @return true, if is generic type
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
    public String stringOf(Optional<T> x) {
        return (x == null || x.isEmpty()) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Optional<T> valueOf(String str) {
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
    public Optional<T> get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = getColumnValue(elementType.clazz(), rs, columnIndex);

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
    public Optional<T> get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = getColumnValue(elementType.clazz(), rs, columnLabel);

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
    public void set(PreparedStatement stmt, int columnIndex, Optional<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Optional<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isEmpty()) ? null : x.get());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, Optional<T> x) throws IOException {
        if (x == null || x.isEmpty()) {
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
    public void writeCharacter(CharacterWriter writer, Optional<T> x, JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            N.typeOf(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
