/*
 * Copyright (C) 2024 HaiYang Li
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

@FunctionalInterface
public interface ByteToBooleanFunction {

    /**
     * Converts a byte value to a boolean: {@code true} if the value is greater than 0, otherwise {@code false}.
     */
    ByteToBooleanFunction DEFAULT = value -> value > 0;

    /**
     *
     * @param value
     * @return
     */
    boolean applyAsBoolean(byte value);
}
