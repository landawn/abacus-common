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
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ToFloatFunction<T> extends Throwables.ToFloatFunction<T, RuntimeException> {

    static final ToFloatFunction<Float> UNBOX = new ToFloatFunction<Float>() {
        @Override
        public float applyAsFloat(Float value) {
            return value == null ? 0 : value.floatValue();
        }
    };

    static final ToFloatFunction<Number> FROM_NUM = new ToFloatFunction<Number>() {
        @Override
        public float applyAsFloat(Number value) {
            return value == null ? 0 : value.floatValue();
        }
    };

    /**
     * @deprecated replaced with {@code FROM_NUM}.
     */
    @Deprecated
    static final ToFloatFunction<Number> NUM = FROM_NUM;

    @Override
    float applyAsFloat(T value);
}
