/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.util.function.LongSupplier;

/**
 * Utility class providing commonly used {@code LongSupplier} instances.
 *
 * <p>This class contains factory methods and constants for creating and accessing
 * standard long suppliers, such as suppliers for current time.</p>
 */
public final class LongSuppliers {
    private LongSuppliers() {
        // utility class
    }

    private static final LongSupplier CURRENT_TIME = System::currentTimeMillis;

    /**
     * Returns a LongSupplier that supplies the current time in milliseconds.
     *
     * <p>This supplier returns the current time in milliseconds since the Unix epoch
     * (January 1, 1970, 00:00:00 GMT) each time it is called.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongSuppliers.ofCurrentTimeMillis().getAsLong();        // returns current time millis
     * }</pre>
     *
     * @return a LongSupplier that returns System.currentTimeMillis()
     */
    public static LongSupplier ofCurrentTimeMillis() {
        return CURRENT_TIME;
    }
}
