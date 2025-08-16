/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies custom type information for fields or methods during serialization and persistence.
 * This annotation allows fine-grained control over how values are converted and handled
 * in different contexts such as JSON/XML serialization or database operations.
 * 
 * <p>The annotation can specify:</p>
 * <ul>
 *   <li>A custom type name or class for value conversion</li>
 *   <li>How enum values should be represented (by name or ordinal)</li>
 *   <li>The scope where the type conversion should apply</li>
 * </ul>
 */
@Documented
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface Type {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * Specifies the type name to use for type conversion.
     * If not specified, the default type handling is used.
     * 
     * @return the type name, empty string for default
     */
    String name() default "";

    /**
     * Specifies a custom Type class to use for value conversion.
     * The specified class must extend {@link com.landawn.abacus.type.Type}.
     * 
     * @return the custom Type class, defaults to base Type class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends com.landawn.abacus.type.Type> clazz() default com.landawn.abacus.type.Type.class;

    /**
     * Specifies how enum values should be represented when converted.
     * The default is EnumBy.NAME which uses the enum constant name.
     * 
     * @return the enum representation strategy
     */
    EnumBy enumerated() default EnumBy.NAME;

    /**
     * Specifies the scope where this type conversion should apply.
     * The default is Scope.ALL which applies to all contexts.
     * 
     * @return the scope of type conversion
     */
    Scope scope() default Scope.ALL;

    /**
     * Defines how enum values should be represented during conversion.
     */
    enum EnumBy {
        /** Persist enumerated type property or field as an integer. */
        ORDINAL,

        /** Persist enumerated type property or field as a string. */
        NAME
    }

    /**
     * Defines the contexts where type conversion should be applied.
     */
    enum Scope {
        /**
         * Used for json/xml/... serialization/deserialization.
         */
        SERIALIZATION,

        /**
         * Used for database column value getter/setter.
         */
        PERSISTENCE,

        /**
         * Used for all scenarios.
         */
        ALL
    }
}
