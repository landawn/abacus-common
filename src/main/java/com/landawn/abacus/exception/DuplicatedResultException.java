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

public class DuplicatedResultException extends IllegalStateException {
    /**
     * Field serialVersionUID.
     */
    private static final long serialVersionUID = -8407459420058648924L;

    /**
     * Constructor for DuplicatedResultException.
     */
    public DuplicatedResultException() {
    }

    /**
     * Constructor for DuplicatedResultException.
     *
     * @param message
     */
    public DuplicatedResultException(final String message) {
        super(message);
    }

    /**
     * Constructor for DuplicatedResultException.
     *
     * @param message
     * @param cause
     */
    public DuplicatedResultException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for DuplicatedResultException.
     *
     * @param cause
     */
    public DuplicatedResultException(final Throwable cause) {
        super(cause);
    }
}
