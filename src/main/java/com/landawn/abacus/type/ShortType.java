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

public final class ShortType extends AbstractShortType {

    public static final String SHORT = Short.class.getSimpleName();

    ShortType() {
        super(SHORT);
    }

    /**
     * Returns the Class object representing the Short wrapper type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortType type = new ShortType();
     * Class<Short> clazz = type.clazz(); // Returns Short.class
     * }</pre>
     *
     * @return the Class object for Short.class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Short.class;
    }

    /**
     * Indicates whether this type represents a primitive wrapper class.
     * For ShortType, this always returns {@code true} since it represents the Short wrapper class
     * for the primitive short type.
     * <p>
     * Usage example:
     * <pre>{@code
     * ShortType type = new ShortType();
     * boolean isWrapper = type.isPrimitiveWrapper(); // Returns true
     * }</pre>
     *
     * @return true, indicating this is a primitive wrapper type
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }
}