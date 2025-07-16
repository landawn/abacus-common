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

package com.landawn.abacus.util.function;

/**
 * Represents an operation that accepts an int-valued argument and an IntConsumer, 
 * and returns no result. This functional interface is used to implement a one-to-many
 * transformation operation, similar to flatMap. The IntConsumer parameter can be 
 * invoked multiple times to pass multiple values downstream.
 *
 * @see java.util.stream.DoubleStream.IntMapMultiConsumer
 */
@FunctionalInterface
public interface IntMapMultiConsumer extends java.util.stream.IntStream.IntMapMultiConsumer { //NOSONAR

    /**
     * Performs this operation on the given argument, providing zero or more result values to the
     * given {@code IntConsumer}.
     *
     * @param value the input value
     * @param ic the {@code IntConsumer} to which the mapped values should be passed
     */
    @Override
    void accept(int value, java.util.function.IntConsumer ic);
}