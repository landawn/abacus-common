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
 * @since 1.3.29
 * 
 * @author Haiyang Li
 */
public class RecordNotFoundException extends AbacusException {
    /**
     * Field serialVersionUID.
     */
    private static final long serialVersionUID = -8572479810647985475L;

    /**
     * Constructor for EntityNotFoundException.
     */
    public RecordNotFoundException() {
        super();
    }

    /**
     * Constructor for EntityNotFoundException.
     * 
     * @param message
     */
    public RecordNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor for EntityNotFoundException.
     * 
     * @param message
     * @param cause
     */
    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for EntityNotFoundException.
     * 
     * @param cause
     */
    public RecordNotFoundException(Throwable cause) {
        super(cause);
    }
}
