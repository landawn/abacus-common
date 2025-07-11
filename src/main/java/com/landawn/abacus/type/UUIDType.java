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

public class UUIDType extends AbstractType<UUID> {

    public static final String UUID = "UUID";

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
     * If the input UUID is null, this method returns null.
     * </p>
     *
     * @param x the UUID instance to convert to string
     * @return the string representation of the UUID, or null if the input is null
     */
    @Override
    public String stringOf(final UUID x) {
        return x == null ? null : x.toString();
    }

    /**
     * Converts a string to a UUID instance.
     * <p>
     * This method creates a UUID instance from the provided string using {@link UUID#fromString(String)}.
     * The string must be in the standard UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     * where 'x' is a hexadecimal digit. If the string is null or empty, this method returns null.
     * </p>
     *
     * @param str the string to convert to a UUID
     * @return a UUID instance created from the string, or null if the string is empty
     * @throws IllegalArgumentException if the string is not in the correct UUID format
     */
    @Override
    public UUID valueOf(final String str) {
        return Strings.isEmpty(str) ? null : java.util.UUID.fromString(str); // NOSONAR
    }
}