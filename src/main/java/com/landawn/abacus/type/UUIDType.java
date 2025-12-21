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

import java.util.UUID;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.UUID} objects.
 * This class provides serialization, deserialization, and type conversion capabilities
 * for UUID instances. It handles conversion between UUID objects and their standard
 * string representation in the format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx".
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<UUID> type = TypeFactory.getType(UUID.class);
 * UUID uuid = type.valueOf("550e8400-e29b-41d4-a716-446655440000");
 * String str = type.stringOf(uuid);   // Returns the UUID string representation
 * }</pre>
 */
public class UUIDType extends AbstractType<UUID> {

    public static final String UUID = "UUID";

    /**
     * Constructs a UUIDType instance.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    UUIDType() {
        super(UUID);
    }

    /**
     * Returns the Class object representing the UUID class.
     * <p>
     * This method returns {@code UUID.class}, which is the Class object for the
     * {@link java.util.UUID} class that this UUIDType handles.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UUIDType uuidType = new UUIDType();
     * Class&lt;UUID&gt; clazz = uuidType.clazz();   // Returns UUID.class
     * }</pre>
     *
     * @return the Class object for UUID.class
     */
    @Override
    public Class<UUID> clazz() {
        return UUID.class;
    }

    /**
     * Converts a UUID instance to its string representation.
     * <p>
     * This method returns the standard string representation of the UUID in the format
     * "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" where 'x' is a hexadecimal digit.
     * If the input UUID is {@code null}, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
     * String str = uuidType.stringOf(uuid);   // Returns "550e8400-e29b-41d4-a716-446655440000"
     * }</pre>
     *
     * @param x the UUID instance to convert to string
     * @return the string representation of the UUID, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final UUID x) {
        return x == null ? null : x.toString();
    }

    /**
     * Parses a string representation to create a UUID instance.
     * <p>
     * This method creates a UUID instance from the provided string using {@link UUID#fromString(String)}.
     * The string must be in the standard UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     * where 'x' is a hexadecimal digit. If the string is {@code null} or empty, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UUID uuid = uuidType.valueOf("550e8400-e29b-41d4-a716-446655440000");
     * // uuid represents the parsed UUID object
     * }</pre>
     *
     * @param str the string to convert to a UUID
     * @return a UUID instance created from the string, or {@code null} if the string is empty
     * @throws IllegalArgumentException if the string is not in the correct UUID format
     */
    @Override
    public UUID valueOf(final String str) {
        return Strings.isBlank(str) ? null : java.util.UUID.fromString(str.trim()); // NOSONAR
    }
}
