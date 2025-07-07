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
 * Represents an operation that accepts a double-valued argument and a DoubleConsumer, 
 * and returns no result. This functional interface is used to implement a one-to-many
 * transformation operation, similar to flatMap. The DoubleConsumer parameter can be 
 * invoked multiple times to pass multiple values downstream.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(double, java.util.function.DoubleConsumer)}.
 *
 * @see java.util.stream.DoubleStream.DoubleMapMultiConsumer
 */
@FunctionalInterface
public interface DoubleMapMultiConsumer extends java.util.stream.DoubleStream.DoubleMapMultiConsumer { //NOSONAR

    /**
     * Performs a one-to-many transformation operation. Accepts a double value and passes
     * zero or more double values to the provided DoubleConsumer. This method can be used
     * to expand a single double value into multiple values in a stream pipeline.
     *
     * @param value the double input value to be transformed
     * @param ic the DoubleConsumer that will receive the transformed values
     */
    @Override
    void accept(double value, java.util.function.DoubleConsumer ic);
}