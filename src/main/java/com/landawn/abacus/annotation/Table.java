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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated class is mapped to a database table.
 * This annotation provides configuration options for table mapping including
 * table name, alias, and field inclusion/exclusion rules.
 * 
 * <p>The table name can be specified explicitly using {@link #name()}.
 * If not specified, the class name is typically used as the table name.</p>
 * 
 * @author HaiYang Li
 * @since 2018
 */
@Documented
@Target(value = { TYPE })
@Retention(RUNTIME)
public @interface Table {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the table name value (deprecated)
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the database table this class maps to.
     * If not specified, the class name is used as the table name.
     * 
     * @return the table name, empty string to use class name as default
     */
    String name() default "";

    /**
     * An alias for the table that can be used in queries.
     * This provides a shorter or alternative name for the table in SQL operations.
     * 
     * @return the table alias, empty string if no alias is defined
     */
    String alias() default "";

    /**
     * Specifies which fields should be treated as table columns.
     * When specified, only these fields will be considered for column mapping.
     * If empty, all non-excluded fields are considered columns.
     * 
     * @return an array of field names to include as columns, empty array by default
     */
    String[] columnFields() default {};

    /**
     * Specifies which fields should NOT be treated as table columns.
     * These fields will be excluded from column mapping operations.
     * 
     * @return an array of field names to exclude from column mapping, empty array by default
     */
    String[] nonColumnFields() default {};
}
