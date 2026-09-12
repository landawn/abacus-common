/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for {@link Integer} (wrapper class) values.
 * This class provides database read operations and type information for {@link Integer} objects.
 *
 * <p>When reading from a database, the column value is retrieved via
 * {@link java.sql.ResultSet#getObject(int) ResultSet.getObject} to preserve SQL {@code NULL}:
 * a {@code null} result returns {@code null}, an {@link Integer} result is returned directly,
 * any other {@link Number} is narrowed via {@link com.landawn.abacus.util.Numbers#toInt(Object)}, and
 * non-numeric values are parsed via {@link com.landawn.abacus.util.Numbers#toInt(String)}. Following that method's
 * contract, an empty string column value is read as {@code 0}, not {@code null} (unlike {@code valueOf("")}); a
 * whitespace-only or otherwise non-numeric string throws {@link NumberFormatException}.</p>
 *
 * <p>String serialization and JDBC write operations are inherited from
 * {@link AbstractIntegerType}.</p>
 *
 * <p>JDBC numeric coercion truncates finite fractional values toward zero, then checks the target range.
 * NaN, infinities and out-of-range integer parts throw {@link ArithmeticException}; no wraparound occurs.</p>
 *
 * @see AbstractIntegerType
 */
public final class IntegerType extends AbstractIntegerType {

    /**
     * The type name constant for Integer type identification, equal to {@code "Integer"}.
     */
    public static final String INTEGER = Integer.class.getSimpleName();

    /**
     * Package-private constructor for IntegerType.
     * This constructor is called by the TypeFactory to create Integer type instances.
     */
    IntegerType() {
        super(INTEGER);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Integer.class}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class javaType() {
        return Integer.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * {@link Integer} is the wrapper for the primitive {@code int} type.
     *
     * @return {@code true}, always, because {@link Integer} is a primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     * Retrieves an {@link Integer} value from a {@link java.sql.ResultSet} at the specified column index.
     * The column is read via {@link java.sql.ResultSet#getObject(int)} to preserve SQL {@code NULL}.
     * If the returned object is already an {@link Integer} it is returned directly; any other
     * {@link Number} is narrowed via {@link com.landawn.abacus.util.Numbers#toInt(Object)}; non-numeric values are parsed via
     * {@link com.landawn.abacus.util.Numbers#toInt(String)}.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnIndex the 1-based column index
     * @return the {@code Integer} value of the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws NumberFormatException if a non-numeric string value cannot be parsed as an {@code int}
     * @throws ArithmeticException if the numeric value is nonfinite or its integer part is out of range
     */
    @MayReturnNull
    @Override
    public Integer get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, NumberFormatException, ArithmeticException {
        final Object result = rs.getObject(columnIndex);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Integer) {
            return (Integer) result;
        } else if (result instanceof Number) {
            return Numbers.toInt(result);
        } else {
            return Numbers.toInt(result.toString());
        }
    }

    /**
     * Retrieves an {@link Integer} value from a {@link java.sql.ResultSet} using the specified column label.
     * The column is read via {@link java.sql.ResultSet#getObject(String)} to preserve SQL {@code NULL}.
     * If the returned object is already an {@link Integer} it is returned directly; any other
     * {@link Number} is narrowed via {@link com.landawn.abacus.util.Numbers#toInt(Object)}; non-numeric values are parsed via
     * {@link com.landawn.abacus.util.Numbers#toInt(String)}.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from; must not be {@code null}
     * @param columnName the label of the column to retrieve
     * @return the {@code Integer} value of the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws NumberFormatException if a non-numeric string value cannot be parsed as an {@code int}
     * @throws ArithmeticException if the numeric value is nonfinite or its integer part is out of range
     */
    @MayReturnNull
    @Override
    public Integer get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, NumberFormatException, ArithmeticException {
        final Object result = rs.getObject(columnName);

        if (result == null) {
            return null; // NOSONAR
        } else if (result instanceof Integer) {
            return (Integer) result;
        } else if (result instanceof Number) {
            return Numbers.toInt(result);
        } else {
            return Numbers.toInt(result.toString());
        }
    }
}
