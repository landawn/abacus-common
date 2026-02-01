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
 * Type handler for {@link String} objects. This class provides the standard
 * implementation for String type handling in the type system. It extends
 * AbstractStringType which provides the core functionality for string operations.
 *
 * <p>StringType handles String serialization/deserialization, database operations,
 * and type conversions. It is automatically registered in the type system and can be
 * retrieved using {@link com.landawn.abacus.type.TypeFactory}.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Retrieve the String type from TypeFactory
 * Type<String> stringType = TypeFactory.getType(String.class);
 *
 * // Convert various objects to String
 * String str1 = stringType.valueOf("hello");      // returns "hello"
 * String str2 = stringType.valueOf((Object) 123); // returns "123"
 * String str3 = stringType.valueOf((String) null); // returns null
 *
 * // Use with database operations (assuming conn is a valid Connection)
 * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name) VALUES (?)")) {
 *     stringType.set(stmt, 1, "John Doe");
 *     stmt.executeUpdate();
 * }
 *
 * try (PreparedStatement stmt2 = conn.prepareStatement("SELECT name FROM users");
 *      ResultSet rs = stmt2.executeQuery()) {
 *     if (rs.next()) {
 *         String name = stringType.get(rs, 1);   // or get(rs, "name")
 *     }
 * }
 * }</pre>
 */
public class StringType extends AbstractStringType {

    /**
     * The type name identifier for String type.
     */
    public static final String STRING = String.class.getSimpleName();

    /**
     * Package-private constructor for StringType.
     * This constructor is called by the TypeFactory to create String type instances.
     */
    StringType() {
        super(STRING);
    }
}