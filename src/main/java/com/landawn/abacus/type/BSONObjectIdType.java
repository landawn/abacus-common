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

import org.bson.types.ObjectId;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for BSON {@link org.bson.types.ObjectId} values used with MongoDB.
 * Converts between {@code ObjectId} instances and their 24-character hexadecimal string
 * representations, enabling storage and transmission of MongoDB ObjectIds in text-based formats.
 *
 * <p>String representation: a 24-character lowercase hexadecimal string produced by
 * {@link org.bson.types.ObjectId#toHexString()}.</p>
 *
 * @see org.bson.types.ObjectId
 */
public class BSONObjectIdType extends AbstractType<ObjectId> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "BSONObjectId"}).
     */
    public static final String BSON_OBJECT_ID = "BSONObjectId";

    /**
     * Package-private constructor for {@code BSONObjectIdType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BSONObjectIdType() {
        super(BSON_OBJECT_ID);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code ObjectId.class}
     */
    @Override
    public Class<ObjectId> javaType() {
        return ObjectId.class;
    }

    /**
     * Converts an {@link org.bson.types.ObjectId} to its 24-character hexadecimal string representation.
     * Delegates to {@link org.bson.types.ObjectId#toHexString()}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code ObjectId} to convert; may be {@code null}
     * @return the 24-character lowercase hexadecimal string representing the 12-byte ObjectId,
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final ObjectId x) {
        return x == null ? null : x.toHexString();
    }

    /**
     * Parses a 24-character hexadecimal string and returns a new {@link org.bson.types.ObjectId}.
     * Leading and trailing whitespace is trimmed before parsing.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the 24-character hexadecimal string to parse; may be {@code null} or blank
     * @return a new {@code ObjectId} created from the hexadecimal string,
     *         or {@code null} if {@code str} is {@code null}, empty, or blank
     * @throws IllegalArgumentException if {@code str} is not a valid 24-character hexadecimal string
     * @see #valueOf(Object)
     * @see #stringOf(ObjectId)
     */
    @Override
    public ObjectId valueOf(final String str) {
        return Strings.isBlank(str) ? null : new ObjectId(str.trim());
    }
}
