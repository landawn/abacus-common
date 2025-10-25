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

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation that accepts an object-valued argument and a boolean-valued argument,
 * and returns no result. This is a two-arity specialization of {@code Consumer}.
 * Unlike most other functional interfaces, {@code ObjBooleanConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(Object, boolean)}.
 * 
 * <p>The interface extends {@code Throwables.ObjBooleanConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 * 
 * <p>Note: Unlike some other primitive specializations, this interface does not provide default methods
 * for composition as the JDK does not provide a standard ObjBooleanConsumer interface.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBooleanConsumer<String> conditionalPrinter = (message, shouldPrint) -> {
 *     if (shouldPrint) {
 *         System.out.println(message);
 *     }
 * };
 * conditionalPrinter.accept("Hello World", true);  // prints "Hello World"
 * conditionalPrinter.accept("Hidden", false);      // prints nothing
 * 
 * ObjBooleanConsumer<User> setActiveStatus = (user, isActive) -> {
 *     user.setActive(isActive);
 *     user.setLastModified(new Date());
 * };
 * 
 * ObjBooleanConsumer<File> setReadOnly = (file, readOnly) -> {
 *     if (readOnly) {
 *         file.setReadOnly();
 *     } else {
 *         file.setWritable(true);
 *     }
 * };
 * }</pre>
 * 
 * @param <T> the type of the object argument to the operation
 * 
 * @see java.util.function.Consumer
 * @see java.util.function.ObjIntConsumer
 * @see java.util.function.ObjLongConsumer
 * @see java.util.function.ObjDoubleConsumer
 */
@FunctionalInterface
public interface ObjBooleanConsumer<T> extends Throwables.ObjBooleanConsumer<T, RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     * 
     * <p>This method processes an object of type T and a boolean value, typically producing
     * side effects such as modifying the object based on the boolean flag, conditional logging,
     * or toggling states.
     * 
     * <p>Common use cases include:
     * <ul>
     *   <li>Setting boolean properties or flags on objects</li>
     *   <li>Conditional processing based on a boolean parameter</li>
     *   <li>Enabling/disabling features or states in an object</li>
     *   <li>Logging or output operations controlled by a boolean flag</li>
     *   <li>Toggling visibility, permissions, or access rights</li>
     * </ul>
     *
     * @param t the object input argument
     * @param value the boolean input argument
     */
    @Override
    void accept(T t, boolean value);
}