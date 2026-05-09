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
 * Type handler for {@code byte[]} values that are serialized as Base64-encoded strings.
 * Converts between raw byte arrays and their standard Base64 string representations,
 * enabling storage and transmission of binary data in text-based formats.
 *
 * <p>The underlying Java type is {@code byte[]}. Unlike {@link BytesType}, which also uses Base64
 * encoding, this type is registered under the name {@code "Base64Encoded"} and treats a
 * {@code null} input to {@link #stringOf(byte[])} as an empty string rather than {@code null}.</p>
 *
 * @see BytesType
 */
public class Base64EncodedType extends AbstractType<byte[]> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "Base64Encoded"}).
     */
    public static final String BASE64_ENCODED = "Base64Encoded";

    /**
     * Package-private constructor for {@code Base64EncodedType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    Base64EncodedType() {
        super(BASE64_ENCODED);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code byte[].class}
     */
    @Override
    public Class<byte[]> javaType() {
        return byte[].class;
    }

    /**
     * Encodes a byte array as a standard Base64 string.
     * Uses {@link com.landawn.abacus.util.Strings#base64Encode(byte[])} internally.
     *
     * <p>If {@code x} is {@code null}, the behavior is governed by
     * {@code Strings.base64Encode} (typically returns an empty string).
     * If {@code x} is an empty array, an empty string is returned.</p>
     *
     * @param x the byte array to encode; may be {@code null}
     * @return the Base64-encoded string representation of the byte array;
     *         an empty string if {@code x} is {@code null} or empty
     */
    @Override
    public String stringOf(final byte[] x) {
        return Strings.base64Encode(x);
    }

    /**
     * Decodes a Base64-encoded string back to a byte array.
     * Uses {@link com.landawn.abacus.util.Strings#base64Decode(String)} internally.
     *
     * @param base64String the Base64-encoded string to decode; may be {@code null} or empty
     * @return the decoded byte array; an empty byte array if the input is {@code null} or empty
     * @throws IllegalArgumentException if {@code base64String} contains characters outside the Base64 alphabet
     */
    @Override
    public byte[] valueOf(final String base64String) {
        return Strings.base64Decode(base64String);
    }
}
