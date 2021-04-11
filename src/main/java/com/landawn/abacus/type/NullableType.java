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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Nullable;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public class NullableType<T> extends AbstractOptionalType<Nullable<T>> {

    public static final String NULLABLE = Nullable.class.getSimpleName();

    private final String declaringName;

    private final Type<T>[] parameterTypes;

    private final Type<T> elementType;

    protected NullableType(String parameterTypeName) {
        super(NULLABLE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN);

        this.declaringName = NULLABLE + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        this.parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        this.elementType = parameterTypes[0];
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class<Nullable<T>> clazz() {
        return (Class) Nullable.class;
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
    public String stringOf(Nullable<T> x) {
        return (x == null || x.isPresent() == false || x.get() == null) ? null : N.stringOf(x.get()); // elementType.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Nullable<T> valueOf(String str) {
        return str == null ? (Nullable<T>) Nullable.empty() : Nullable.of(elementType.valueOf(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Nullable<T> get(ResultSet rs, int columnIndex) throws SQLException {
        final Object obj = getColumnValue(elementType.clazz(), rs, columnIndex);

        return obj == null ? (Nullable<T>) Nullable.empty()
                : Nullable.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, elementType));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Nullable<T> get(ResultSet rs, String columnLabel) throws SQLException {
        final Object obj = getColumnValue(elementType.clazz(), rs, columnLabel);

        return obj == null ? (Nullable<T>) Nullable.empty()
                : Nullable.of(elementType.clazz().isAssignableFrom(obj.getClass()) ? (T) obj : N.convert(obj, elementType));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Nullable<T> x) throws SQLException {
        stmt.setObject(columnIndex, (x == null || x.isPresent() == false || x.get() == null) ? null : x.get());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Nullable<T> x) throws SQLException {
        stmt.setObject(parameterName, (x == null || x.isPresent() == false || x.get() == null) ? null : x.get());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, Nullable<T> x) throws IOException {
        if (x == null || x.isPresent() == false || x.get() == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.write(writer, x.get());
            N.typeOf(x.get().getClass()).write(writer, x.get());
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
    public void writeCharacter(CharacterWriter writer, Nullable<T> x, SerializationConfig<?> config) throws IOException {
        if (x == null || x.isPresent() == false || x.get() == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // elementType.writeCharacter(writer, x.get(), config);
            N.typeOf(x.get().getClass()).writeCharacter(writer, x.get(), config);
        }
    }
}
