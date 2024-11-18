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

public interface BooleanToIntFunction {

    /**
     * Converts a boolean value to an integer: {@code true} to {@code 1} and {@code false} to {@code 0}.
     */
    BooleanToIntFunction DEFAULT = value -> value ? 1 : 0;

    /**
     *
     * @param value
     * @return
     */
    int applyAsInt(boolean value);
}
