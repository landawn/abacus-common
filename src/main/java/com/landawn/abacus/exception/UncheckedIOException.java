/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.exception;

import java.io.IOException;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class UncheckedIOException extends UncheckedException {

    private static final long serialVersionUID = -8702336402043331418L;

    /**
     * Constructor for UncheckedIOException.
     *
     * @param cause
     */
    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    /**
     * Constructor for UncheckedIOException.
     *
     * @param message
     * @param cause
     */
    public UncheckedIOException(String message, IOException cause) {
        super(message, cause);
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the {@code IOException} which is the cause of this exception.
     */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
