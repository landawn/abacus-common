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

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@FunctionalInterface
public interface LongConsumer extends Throwables.LongConsumer<RuntimeException>, java.util.function.LongConsumer { //NOSONAR

    /**
     *
     * @param t
     */
    @Override
    void accept(long t);

    /**
     *
     * @param after
     * @return
     */
    @Override
    default LongConsumer andThen(final java.util.function.LongConsumer after) {
        return (final long t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
