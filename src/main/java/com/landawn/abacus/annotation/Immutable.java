/*
 * Copyright (C) 2020 HaiYang Li
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated type or method represents immutable data or operations.
 * Immutable types are those whose state cannot be modified after construction,
 * and immutable methods are those that do not modify the state of the object.
 * 
 * <p>This annotation serves as documentation and can be used by static analysis tools
 * to verify immutability constraints and detect potential violations.</p>
 * 
 * <p>When applied to a type, it indicates that all instances of this type are immutable.
 * When applied to a method, it indicates that the method does not modify the object's state.</p>
 * 
 * @author HaiYang Li
 * @since 2020
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface Immutable {
}
