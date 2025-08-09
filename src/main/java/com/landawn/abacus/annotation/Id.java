/*
 * Copyright (C) 2018 HaiYang Li
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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field or type as representing a primary key or identifier.
 * This annotation is used in ORM frameworks to identify primary key fields
 * and in other contexts where unique identification is required.
 * 
 * <p>When applied to a field, it indicates that the field is a primary key.
 * When applied to a type, it can specify composite primary key information.</p>
 * 
 * <p>The {@link #value()} attribute can be used to specify multiple column names
 * for composite keys or additional key configuration.</p>
 * 
 * @author HaiYang Li
 * @since 2018
 */
@Documented
@Target(value = { FIELD, /* METHOD, */ TYPE })
@Retention(RUNTIME)
public @interface Id {

    /**
     * Specifies the column names that form the primary key.
     * For simple primary keys, this is typically empty and the field name is used.
     * For composite keys, this can contain multiple column names.
     * 
     * @return an array of column names, empty array by default
     */
    String[] value() default {};
}
