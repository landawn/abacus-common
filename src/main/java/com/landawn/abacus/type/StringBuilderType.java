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

public class StringBuilderType extends AbstractCharSequenceType<StringBuilder> {

    /**
     * The type name identifier for StringBuilder type.
     */
    public static final String STRING_BUILDER = StringBuilder.class.getSimpleName();

    /**
     * Constructs a StringBuilderType instance.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    StringBuilderType() {
        super(STRING_BUILDER);
    }

    /**
     * Returns the Class object representing the StringBuilder type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuilder> type = TypeFactory.getType(StringBuilder.class);
     * Class<StringBuilder> clazz = type.clazz();   // Returns StringBuilder.class
     * }</pre>
     *
     * @return the Class object for StringBuilder.class
     */
    @Override
    public Class<StringBuilder> clazz() {
        return StringBuilder.class;
    }

    /**
     * Converts a StringBuilder object to its string representation.
     * This method returns the string content of the StringBuilder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuilder> type = TypeFactory.getType(StringBuilder.class);
     * StringBuilder sb = new StringBuilder("Hello World");
     * String str = type.stringOf(sb);   // Returns "Hello World"
     * }</pre>
     *
     * @param x the StringBuilder object to convert
     * @return the string representation of the StringBuilder's content, or {@code null} if x is null
     */
    @Override
    public String stringOf(final StringBuilder x) {
        return x == null ? null : x.toString();
    }

    /**
     * Creates a StringBuilder object from its string representation.
     * This method creates a new StringBuilder containing the provided string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<StringBuilder> type = TypeFactory.getType(StringBuilder.class);
     * StringBuilder sb = type.valueOf("Hello World");   // Creates StringBuilder with "Hello World"
     * }</pre>
     *
     * @param str the string to convert to a StringBuilder
     * @return a new StringBuilder containing the string content, or {@code null} if str is null
     */
    @Override
    public StringBuilder valueOf(final String str) {
        return str == null ? null : new StringBuilder(str);
    }

    /**
     * Indicates whether instances of this type are immutable.
     * StringBuilder instances are mutable, so this returns {@code false}.
     *
     * @return {@code false}, as StringBuilder objects are mutable
     */
    @Override
    public boolean isImmutable() {
        return false;
    }
}