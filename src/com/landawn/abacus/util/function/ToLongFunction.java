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

import com.landawn.abacus.util.Try;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ToLongFunction<T> extends java.util.function.ToLongFunction<T>, Try.ToLongFunction<T, RuntimeException> {

    static final ToLongFunction<Long> UNBOX = new ToLongFunction<Long>() {
        @Override
        public long applyAsLong(Long value) {
            return value == null ? 0 : value.longValue();
        }
    };

    static final ToLongFunction<Number> FROM_NUM = new ToLongFunction<Number>() {
        @Override
        public long applyAsLong(Number value) {
            return value == null ? 0 : value.longValue();
        }
    };

    /**
     * @deprecated replaced with {@code FROM_NUM}.
     */
    @Deprecated
    static final ToLongFunction<Number> NUM = FROM_NUM;

    @Override
    long applyAsLong(T value);
}
