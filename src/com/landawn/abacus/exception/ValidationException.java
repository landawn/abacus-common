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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class ValidationException extends AbacusException {
    /**
     * Field serialVersionUID. (value is 5885166649874336199L)
     */
    private static final long serialVersionUID = 5885166649874336199L;

    /**
     * Constructor for IllegalArgumentException.
     */
    public ValidationException() {
        super();
    }

    /**
     * Constructor for IllegalArgumentException.
     * 
     * @param message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructor for IllegalArgumentException.
     * 
     * @param message
     * @param cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for IllegalArgumentException.
     * 
     * @param cause
     */
    public ValidationException(Throwable cause) {
        super(cause);
    }
}
