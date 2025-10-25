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
 * A functional interface that represents an operation that accepts an object-valued argument
 * and a char-valued argument, and returns no result. This is a specialization of BiConsumer
 * for the case where the second argument is a primitive char.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, char)}.
 *
 * @param <T> the type of the object argument to the operation
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface ObjCharConsumer<T> extends Throwables.ObjCharConsumer<T, RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a char value, performing some
     * side-effect operation without returning any result. Common use cases include
     * updating the object's state based on the char value, logging, or storing
     * the char value within the object.
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive char value if the operation cannot be completed
     */
    @Override
    void accept(T t, char value);
}