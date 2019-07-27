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

import java.sql.SQLException;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class AbacusException extends RuntimeException {
    /**
     * Field serialVersionUID
     */
    private static final long serialVersionUID = -1206509526015359475L;

    /**
     * Constructs a new Coat exception with <code>null</code> as its detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause}.
     */
    public AbacusException() {
        super();
    }

    /**
     * Constructs a new Coat exception with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     * 
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     */
    public AbacusException(String message) {
        super(message);
    }

    /**
     * Constructs a new Coat exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>). This constructor is useful for Coat exceptions that are little more than wrappers for other
     * throwables.
     * 
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public AbacusException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new Coat exception with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this
     * Coat exception's detail message.
     * 
     * @param message
     *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public AbacusException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Method toString.
     * 
     * @return String
     */
    @Override
    public String toString() {
        return getMessage();
    }

    public static String getErrorMsg(Throwable e) {
        if (e instanceof SQLException) {
            return e.getClass().getSimpleName() + "|" + ((SQLException) e).getErrorCode() + "|" + ((e.getMessage() == null) ? e.getCause() : e.getMessage());
        } else {
            return e.getClass().getSimpleName() + "|" + ((e.getMessage() == null) ? e.getCause() : e.getMessage());
        }
    }
}
