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

import com.landawn.abacus.util.Strings;

/**
 * Type handler for Base64-encoded byte arrays.
 * This class provides conversion between byte arrays and their Base64 string representations,
 * enabling storage and transmission of binary data as text.
 */
public class Base64EncodedType extends AbstractType<byte[]> {

    /**
     * The type name constant for Base64-encoded type identification.
     */
    public static final String BASE64_ENCODED = "Base64Encoded";

    Base64EncodedType() {
        super(BASE64_ENCODED);
    }

    /**
     * Returns the Class object representing the byte array class.
     *
     * @return the Class object for byte[].class
     */
    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    /**
     * Converts a byte array to its Base64-encoded string representation.
     * Uses standard Base64 encoding to convert binary data to a text format
     * suitable for storage or transmission in text-based protocols.
     *
     * @param x the byte array to encode
     * @return the Base64-encoded string representation of the byte array,
     *         or an empty string if the input is null
     */
    @Override
    public String stringOf(final byte[] x) {
        return Strings.base64Encode(x);
    }

    /**
     * Converts a Base64-encoded string back to its original byte array.
     * Decodes the Base64 string to recover the original binary data.
     *
     * @param base64String the Base64-encoded string to decode
     * @return the decoded byte array, or an empty byte array if the input is null
     * @throws IllegalArgumentException if the input string is not valid Base64
     */
    @Override
    public byte[] valueOf(final String base64String) {
        return Strings.base64Decode(base64String);
    }
}