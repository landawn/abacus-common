/*
 * Copyright (C) 2019 HaiYang Li
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
 * Type handler for {@link StringBuilder} objects. This class provides serialization
 * and deserialization support for StringBuilder instances, which are mutable
 * sequences of characters.
 */
public class StringBuilderType extends AbstractCharSequenceType<StringBuilder> {

    /**
     * The type name identifier for StringBuilder type.
     */
    public static final String STRING_BUILDER = StringBuilder.class.getSimpleName();

    StringBuilderType() {
        super(STRING_BUILDER);
    }

    /**
     * Returns the Class object representing the StringBuilder type.
     *
     * @return the Class object for StringBuilder
     */
    @Override
    public Class<StringBuilder> clazz() {
        return StringBuilder.class;
    }

    /**
     * Converts a StringBuilder object to its string representation.
     * This method returns the string content of the StringBuilder.
     *
     * @param x the StringBuilder object to convert
     * @return the string representation of the StringBuilder's content, or null if x is null
     */
    @Override
    public String stringOf(final StringBuilder x) {
        return x == null ? null : x.toString();
    }

    /**
     * Creates a StringBuilder object from its string representation.
     * This method creates a new StringBuilder containing the provided string.
     *
     * @param str the string to convert to a StringBuilder
     * @return a new StringBuilder containing the string content, or null if str is null
     */
    @Override
    public StringBuilder valueOf(final String str) {
        return str == null ? null : new StringBuilder(str);
    }

    /**
     * Indicates whether instances of this type are immutable.
     * StringBuilder instances are mutable, so this returns false.
     *
     * @return false, as StringBuilder objects are mutable
     */
    @Override
    public boolean isImmutable() {
        return false;
    }
}