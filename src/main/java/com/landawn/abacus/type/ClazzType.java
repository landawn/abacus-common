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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Class objects.
 * This class provides serialization and deserialization for Java Class instances.
 * It handles the conversion between Class objects and their canonical string names.
 * Note: Uses raw types for compatibility with generic Class handling.
 */
@SuppressWarnings({ "rawtypes", "java:S2160" })
public class ClazzType extends AbstractType<Class> {

    public static final String CLAZZ = "Clazz"; //NOSONAR

    private final Class clazz; //NOSONAR

    protected ClazzType(final String typeName) {
        super("Clazz<" + typeName + ">");

        clazz = ClassUtil.forClass(typeName);
    }

    /**
     * Returns the Java class type handled by this type handler.
     * This represents the specific Class type this handler manages.
     *
     * @return The Class object loaded from the type name provided in constructor
     */
    @Override
    public Class<Class> clazz() {
        return clazz;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * Class objects are immutable in Java.
     *
     * @return {@code true}, indicating Class objects are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts a Class object to its string representation.
     * Uses the canonical class name for serialization.
     *
     * @param x the Class object to convert. Can be {@code null}.
     * @return The canonical name of the class, or {@code null} if input is null
     */
    @Override
    public String stringOf(final Class x) {
        return x == null ? null : ClassUtil.getCanonicalClassName(x);
    }

    /**
     * Converts a string representation back to a Class object.
     * The string should be a fully qualified class name.
     * Uses ClassUtil.forClass to load the class, which handles primitive types
     * and array notations appropriately.
     *
     * @param str the fully qualified class name. Can be {@code null} or empty.
     * @return The Class object for the specified name, or {@code null} if input is null/empty
     * @throws RuntimeException if the class cannot be found or loaded
     */
    @Override
    public Class valueOf(final String str) {
        return Strings.isEmpty(str) ? null : ClassUtil.forClass(str);
    }
}
