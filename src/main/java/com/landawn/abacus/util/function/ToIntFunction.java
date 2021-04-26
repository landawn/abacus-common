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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ToIntFunction<T> extends Throwables.ToIntFunction<T, RuntimeException>, java.util.function.ToIntFunction<T> {

    static final ToIntFunction<Integer> UNBOX = new ToIntFunction<Integer>() {
        @Override
        public int applyAsInt(Integer value) {
            return value == null ? 0 : value.intValue();
        }
    };

    static final ToIntFunction<Number> FROM_NUM = new ToIntFunction<Number>() {
        @Override
        public int applyAsInt(Number value) {
            return value == null ? 0 : value.intValue();
        }
    };

    /**
     * @deprecated replaced by {@code FROM_NUM}.
     */
    @Deprecated
    static final ToIntFunction<Number> NUM = FROM_NUM;

    @Override
    int applyAsInt(T value);
}
