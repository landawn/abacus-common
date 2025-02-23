/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.exception;

import java.io.IOException;
import java.io.Serial;

public class UncheckedIOException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = -8702336402043331418L;

    /**
     * Constructor for UncheckedIOException.
     *
     * @param cause
     */
    public UncheckedIOException(final IOException cause) {
        super(cause);
    }

    /**
     * Constructor for UncheckedIOException.
     *
     * @param message
     * @param cause
     */
    public UncheckedIOException(final String message, final IOException cause) {
        super(message, cause);
    }
}
