/*
 * Copyright (C) 2022 HaiYang Li
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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class TooManyElementsException extends IllegalStateException {

    /**
     * Field serialVersionUID.
     */
    private static final long serialVersionUID = 4230938963102900489L;

    /**
     * Constructor for EntityNotFoundException.
     */
    public TooManyElementsException() {
    }

    /**
     * Constructor for EntityNotFoundException.
     *
     * @param message
     */
    public TooManyElementsException(String message) {
        super(message);
    }

    /**
     * Constructor for EntityNotFoundException.
     *
     * @param message
     * @param cause
     */
    public TooManyElementsException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for EntityNotFoundException.
     *
     * @param cause
     */
    public TooManyElementsException(Throwable cause) {
        super(cause);
    }
}