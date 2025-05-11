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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

import java.io.Serial;

public class UncheckedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1973552812345999717L;

    // private final Throwable checkedException;

    //    /**
    //     * Constructor for UncheckedIOException.
    //     */
    //    UncheckedException() {
    //    }
    //
    //    /**
    //     * Constructor for UncheckedIOException.
    //     *
    //     * @param message
    //     */
    //    UncheckedException(final String message) {
    //        super(message);
    //    }

    /**
     * Constructor for UncheckedIOException.
     *
     * @param checkedException
     */
    public UncheckedException(final Throwable checkedException) {
        super(getCause(checkedException));
        // this.checkedException = checkedException;
    }

    /**
     * Constructor for UncheckedIOException.
     *
     * @param message
     * @param checkedException
     */
    public UncheckedException(final String message, final Throwable checkedException) {
        super(message, getCause(checkedException));
        // this.checkedException = checkedException;
    }

    private static Throwable getCause(final Throwable checkedException) {
        N.checkArgNotNull(checkedException, cs.checkedException);

        // Refer to ExceptionUtil.tryToGetOriginalCheckedException(Throwable e). It should/must be the original checked exception.
        // return checkedException.getCause() == null ? checkedException : checkedException.getCause();

        return checkedException;
    }

    //    /**
    //     * Returns the cause of this exception.
    //     *
    //     * @return the {@code IOException} which is the cause of this exception.
    //     */
    //    @SuppressWarnings("sync-override")
    //    @Override
    //    public Throwable getCause() {
    //        return checkedException.getCause();
    //    }
    //
    //    /**
    //     * Returns the stack trace of the checked exception.
    //     *
    //     * @return the stack trace elements of the checked exception
    //     */
    //    @Override
    //    public StackTraceElement[] getStackTrace() {
    //        return checkedException.getStackTrace();
    //    }
}
