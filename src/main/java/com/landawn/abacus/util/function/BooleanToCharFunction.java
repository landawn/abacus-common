/*
 * Copyright (C) 2024 HaiYang Li
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

public interface BooleanToCharFunction {

    /**
     * Converts a boolean value to a char: {@code true} to 'Y' and {@code false} to 'N'.
     */
    BooleanToCharFunction DEFAULT = value -> value ? 'Y' : 'N';

    /**
     *
     * @param value
     * @return
     */
    char applyAsChar(boolean value);
}