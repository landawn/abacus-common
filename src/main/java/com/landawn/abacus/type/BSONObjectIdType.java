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
 * Type handler for BSON ObjectId operations.
 * This class provides conversion between org.bson.types.ObjectId instances
 * and their hexadecimal string representations, enabling storage and
 * transmission of MongoDB ObjectIds in text formats.
 */
public class BSONObjectIdType extends AbstractType<ObjectId> {

    /**
     * The type name constant for BSON ObjectId type identification.
     */
    public static final String BSON_OBJECT_ID = "BSONObjectId";

    BSONObjectIdType() {
        super(BSON_OBJECT_ID);
    }

    /**
     * Returns the Class object representing the ObjectId class.
     *
     * @return the Class object for org.bson.types.ObjectId
     */
    @Override
    public Class<ObjectId> clazz() {
        return ObjectId.class;
    }

    /**
     * Converts an ObjectId to its hexadecimal string representation.
     * The resulting string is a 24-character hexadecimal representation
     * of the 12-byte ObjectId value.
     *
     * @param x the ObjectId to convert
     * @return the 24-character hexadecimal string representation of the ObjectId,
     *         or null if the input is null
     */
    @Override
    public String stringOf(final ObjectId x) {
        return x == null ? null : x.toHexString();
    }

    /**
     * Converts a hexadecimal string representation to an ObjectId.
     * Parses a 24-character hexadecimal string to create a new ObjectId instance.
     *
     * @param str the hexadecimal string to parse (must be 24 characters)
     * @return a new ObjectId created from the hexadecimal string,
     *         or null if str is null or empty
     * @throws IllegalArgumentException if the string is not a valid 24-character hex string
     */
    @Override
    public ObjectId valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new ObjectId(str);
    }
}