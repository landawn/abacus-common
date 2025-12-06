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

public class StringBufferType extends AbstractCharSequenceType<StringBuffer> {

    /**
     * The type name identifier for StringBuffer type.
     */
    public static final String STRING_BUFFER = StringBuffer.class.getSimpleName();

    /**
     * Constructs a StringBufferType instance.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    StringBufferType() {
        super(STRING_BUFFER);
    }

    /**
     * Returns the Class object representing the StringBuffer type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuffer> type = TypeFactory.getType(StringBuffer.class);
     * Class<StringBuffer> clazz = type.clazz();   // Returns StringBuffer.class
     * }</pre>
     *
     * @return the Class object for StringBuffer.class
     */
    @Override
    public Class<StringBuffer> clazz() {
        return StringBuffer.class;
    }

    /**
     * Converts a StringBuffer object to its string representation.
     * This method returns the string content of the StringBuffer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuffer> type = TypeFactory.getType(StringBuffer.class);
     * StringBuffer sb = new StringBuffer("Hello World");
     * String str = type.stringOf(sb);   // Returns "Hello World"
     * }</pre>
     *
     * @param x the StringBuffer object to convert
     * @return the string representation of the StringBuffer's content, or {@code null} if x is null
     */
    @Override
    public String stringOf(final StringBuffer x) {
        return x == null ? null : x.toString();
    }

    /**
     * Creates a StringBuffer object from its string representation.
     * This method creates a new StringBuffer containing the provided string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuffer> type = TypeFactory.getType(StringBuffer.class);
     * StringBuffer sb = type.valueOf("Hello World");   // Creates StringBuffer with "Hello World"
     * }</pre>
     *
     * @param str the string to convert to a StringBuffer
     * @return a new StringBuffer containing the string content, or {@code null} if str is null
     */
    @Override
    public StringBuffer valueOf(final String str) {
        return str == null ? null : new StringBuffer(str);
    }

    /**
     * Indicates whether instances of this type are immutable.
     * StringBuffer instances are mutable, so this returns {@code false}.
     *
     * @return {@code false}, as StringBuffer objects are mutable
     */
    @Override
    public boolean isImmutable() {
        return false;
    }
}
