/*
 * Copyright (C) 2021 HaiYang Li
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a class as a database entity for ORM mapping purposes.
 * This annotation indicates that the annotated class represents a database table
 * and can be used with persistence frameworks.
 * 
 * <p>The entity name can be specified explicitly using the {@link #name()} attribute.
 * If not specified, the class name is typically used as the entity name.</p>
 * 
 * <p><strong>Note:</strong> This annotation is marked as {@link Beta}, indicating it may
 * undergo changes in future versions.</p>
 * 
 * @author HaiYang Li
 * @since 2021
 */
@Beta
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RUNTIME)
public @interface Entity {

    /**
     * Use {@code name} to specify attribute explicitly
     *
     * @return the entity name value (deprecated)
     * @deprecated use {@code name} to specify attribute explicitly.
     */
    @Deprecated
    String value() default "";

    /**
     * The name of the entity. If not specified, the class name is used.
     * This name is typically used to identify the entity in database operations.
     * 
     * @return the entity name, empty string to use class name as default
     */
    String name() default "";
}
