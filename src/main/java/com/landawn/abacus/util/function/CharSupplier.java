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
public interface CharSupplier extends Try.CharSupplier<RuntimeException> {

    static final CharSupplier ZERO = new CharSupplier() {
        @Override
        public char getAsChar() {
            return 0;
        }
    };

    static final CharSupplier RANDOM = new CharSupplier() {
        @Override
        public char getAsChar() {
            return (char) Math.abs(Util.RAND_CHAR.nextInt() % Util.CHAR_MOD);
        }
    };

    @Override
    char getAsChar();
}
