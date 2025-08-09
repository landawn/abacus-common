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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a supplier of {@code boolean}-valued results. This is the {@code boolean}-producing
 * primitive specialization of {@link java.util.function.Supplier}.
 * 
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 * 
 * <p>This is a functional interface whose functional method is {@link #getAsBoolean()}.
 * 
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BooleanSupplier extends Throwables.BooleanSupplier<RuntimeException>, java.util.function.BooleanSupplier { //NOSONAR

    /**
     * A supplier that always returns {@code true}.
     */
    BooleanSupplier TRUE = () -> true;

    /**
     * A supplier that always returns {@code false}.
     */
    BooleanSupplier FALSE = () -> false;

    /**
     * A supplier that returns a random {@code boolean} value.
     * Each invocation has approximately equal probability of returning {@code true} or {@code false}.
     */
    BooleanSupplier RANDOM = () -> Util.RAND_BOOLEAN.nextBoolean();

    /**
     * Gets a result.
     *
     * @return a {@code boolean} value
     */
    @Override
    boolean getAsBoolean();
}