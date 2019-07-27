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

import java.security.SecureRandom;
import java.util.Random;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
class Util {
    private Util() {
        // singleton.
    }

    static final Random RAND_BOOLEAN = new SecureRandom();

    static final Random RAND_CHAR = new SecureRandom();

    static final Random RAND_BYTE = new SecureRandom();

    static final Random RAND_SHORT = new SecureRandom();

    static final Random RAND_INT = new SecureRandom();

    static final Random RAND_LONG = new SecureRandom();

    static final Random RAND_FLOAT = new SecureRandom();

    static final Random RAND_DOUBLE = new SecureRandom();

    static final int CHAR_MOD = Character.MAX_VALUE + 1;
}
