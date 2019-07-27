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
public class InvalidResultHandleException extends AbacusException {
    /**
     * Field serialVersionUID. (value is -1143484757845245789)
     */
    private static final long serialVersionUID = -1143484757845245789L;

    /**
     * Constructor for InvalidResultHandleException.
     */
    public InvalidResultHandleException() {
        super();
    }

    /**
     * Constructor for InvalidResultHandleException.
     * 
     * @param message
     */
    public InvalidResultHandleException(String message) {
        super(message);
    }

    /**
     * Constructor for InvalidResultHandleException.
     * 
     * @param message
     * @param cause
     */
    public InvalidResultHandleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for InvalidResultHandleException.
     * 
     * @param cause
     */
    public InvalidResultHandleException(Throwable cause) {
        super(cause);
    }
}
