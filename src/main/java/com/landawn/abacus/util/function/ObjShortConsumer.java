/*
 * Copyright (C) 2016 HaiYang Li
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

import com.landawn.abacus.util.Throwables;

@FunctionalInterface
public interface ObjShortConsumer<T> extends Throwables.ObjShortConsumer<T, RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method consumes an object of type T and a short value, performing some
     * side-effect operation without returning any result. Common use cases include
     * updating the object's state based on the short value, working with small numeric
     * ranges, or processing data where memory efficiency is important and values fit
     * within the short range (-32,768 to 32,767).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjShortConsumer<ShortBuffer> addToBuffer = (buffer, value) -> buffer.put(value);
     * ObjShortConsumer<Config> setPort = (config, port) -> config.setPort(port);
     * 
     * addToBuffer.accept(myBuffer, (short) 42);
     * setPort.accept(myConfig, (short) 8080);
     * }</pre>
     *
     * @param t the first input argument of type T
     * @param value the second input argument, a primitive short value if the operation cannot be completed
     */
    @Override
    void accept(T t, short value);
}
