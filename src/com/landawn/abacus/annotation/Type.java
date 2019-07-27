/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

@Documented
@Target({ FIELD, METHOD })
@Retention(RUNTIME)

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public @interface Type {
    /**
     * 
     * @return
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    String name() default "";

    EnumType enumerated() default EnumType.STRING;

    Scope scope() default Scope.ALL;

    public static enum EnumType {
        /** Persist enumerated type property or field as an integer. */
        ORDINAL,

        /** Persist enumerated type property or field as a string. */
        STRING
    }

    public static enum Scope {
        /**
         * Used for json/xml/... serialization/deserialization.
         */
        PARSER,

        /**
         * Used for database column value getter/setter.
         */
        DB,

        /**
         * Used for all scenarios.
         */
        ALL
    }
}
