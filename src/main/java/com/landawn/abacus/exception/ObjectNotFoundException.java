/*
 * Copyright (C) 2019 HaiYang Li
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
 * @since 1.3.29
 */
public class ObjectNotFoundException extends IllegalStateException {

    /** serialVersionUID. */
    private static final long serialVersionUID = -1806452586200243492L;

    /**
     * Constructor for ObjectNotFoundException.
     */
    public ObjectNotFoundException() {
    }

    /**
     * Constructor for ObjectNotFoundException.
     *
     * @param message
     */
    public ObjectNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor for ObjectNotFoundException.
     *
     * @param message
     * @param cause
     */
    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for ObjectNotFoundException.
     *
     * @param cause
     */
    public ObjectNotFoundException(Throwable cause) {
        super(cause);
    }
}
