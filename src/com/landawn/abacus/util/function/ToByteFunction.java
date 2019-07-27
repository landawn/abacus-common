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
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface ToByteFunction<T> extends Try.ToByteFunction<T, RuntimeException> {

    static final ToByteFunction<Byte> UNBOX = new ToByteFunction<Byte>() {
        @Override
        public byte applyAsByte(Byte value) {
            return value == null ? 0 : value.byteValue();
        }
    };

    static final ToByteFunction<Number> FROM_NUM = new ToByteFunction<Number>() {
        @Override
        public byte applyAsByte(Number value) {
            return value == null ? 0 : value.byteValue();
        }
    };

    /**
     * @deprecated replaced with {@code FROM_NUM}.
     */
    @Deprecated
    static final ToByteFunction<Number> NUM = FROM_NUM;

    @Override
    byte applyAsByte(T value);
}
