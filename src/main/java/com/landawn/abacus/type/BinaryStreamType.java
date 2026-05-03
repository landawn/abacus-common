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

/**
 * Type handler for binary {@link java.io.InputStream} values mapped to database binary stream columns.
 * This class extends {@link InputStreamType}, inheriting all database and I/O operations,
 * while registering itself under the distinct type name {@code "BinaryStream"}.
 *
 * <p>Binary streams are typically used with SQL {@code BINARY}, {@code VARBINARY}, or
 * {@code LONGVARBINARY} columns. JDBC operations are delegated to
 * {@link java.sql.PreparedStatement#setBinaryStream} and
 * {@link java.sql.ResultSet#getBinaryStream}.</p>
 *
 * @see InputStreamType
 */
public class BinaryStreamType extends InputStreamType {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "BinaryStream"}).
     */
    public static final String BINARY_STREAM = "BinaryStream";

    /**
     * Package-private constructor for {@code BinaryStreamType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BinaryStreamType() {
        super(BINARY_STREAM);
    }

}
