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
 * The Class ParseException.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ParseException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 7678894353902496315L;

    private int token = -2;

    /**
     * Constructor for ParseException.
     */
    public ParseException() {
        super();
    }

    /**
     * Constructor for ParseException.
     *
     * @param message
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructor for ParseException.
     *
     * @param message
     */
    public ParseException(int token, String message) {
        super(message);

        this.token = token;
    }

    /**
     * Constructor for ParseException.
     *
     * @param message
     * @param cause
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for ParseException.
     *
     * @param cause
     */
    public ParseException(Throwable cause) {
        super(cause);
    }

    public int getToken() {
        return token;
    }
}
