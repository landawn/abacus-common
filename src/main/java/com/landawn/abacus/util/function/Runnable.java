/*
 * Copyright (C) 2018 HaiYang Li
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

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Throwables;

/**
 *
 *
 */
public interface Runnable extends java.lang.Runnable, Throwables.Runnable<RuntimeException> { //NOSONAR

    /**
     *
     */
    @Override
    void run();

    /**
     *
     *
     * @return
     */
    default Callable<Void> toCallable() {
        return Fn.r2c(this);
    }

    /**
     *
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.Runnable<E> toThrowable() {
        return (Throwables.Runnable<E>) this;
    }
}
