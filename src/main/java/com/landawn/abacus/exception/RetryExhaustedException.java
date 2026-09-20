/*
 * Copyright (C) 2026 HaiYang Li
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

import java.io.Serial;

/**
 * Exception thrown when a retry policy ran every attempt it was allowed and the final attempt still
 * produced an unacceptable <i>result</i>.
 *
 * <p>This signals result rejection, not operation failure. If the final attempt threw, that exception is
 * rethrown directly instead and this type never appears. Catch it to distinguish "the operation kept
 * succeeding but never produced anything acceptable" from "the operation kept failing".</p>
 *
 * <p>The rejected result is deliberately <b>not</b> carried by this exception, and never appears in its
 * message: it may be large, or hold credentials or personal data that would then be copied into every log
 * that records the failure. The attempt counts are carried instead, and the most recent exception thrown by
 * an <i>earlier</i> attempt - if any attempt threw at all - is attached as a {@linkplain #getSuppressed()
 * suppressed} exception rather than as the cause, because an earlier failure did not cause the final
 * attempt's result to be rejected.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Retry<Response> retry = Retry.withFixedDelay(3, 500, (result, ex) -> result == null || result.isStale());
 *
 * try {
 *     Response response = retry.call(() -> httpClient.get(url));
 *     use(response);
 * } catch (RetryExhaustedException e) {
 *     // The final attempt returned a rejected result; earlier attempts may have thrown.
 *     log.warn("gave up after {} attempts ({} retries)", e.attempts(), e.retries());
 *     use(cachedFallback());
 * }
 * }</pre>
 *
 * @see com.landawn.abacus.util.Retry
 * @see IllegalStateException
 */
public class RetryExhaustedException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = -8035743191874923116L;

    /** The total number of operation invocations, including the first attempt. */
    private final long attempts;

    /** The number of retries configured by the retry policy. */
    private final int retries;

    /**
     * Constructs a new {@code RetryExhaustedException} with the given attempt counts.
     * The total uses {@code long} so a policy with {@link Integer#MAX_VALUE} retries can report
     * all 2,147,483,648 attempts. Serialized instances from the former int-count format are incompatible.
     *
     * @param message the detail message; it must not include the rejected result
     * @param attempts the total number of times the operation was invoked, including the first attempt
     * @param retries the number of retries the policy was configured with ({@code attempts - 1})
     */
    public RetryExhaustedException(final String message, final long attempts, final int retries) {
        super(message);

        this.attempts = attempts;
        this.retries = retries;
    }

    /**
     * Returns the total number of times the operation was invoked, including the first attempt.
     *
     * <p>For instances created by {@link com.landawn.abacus.util.Retry}, this equals
     * {@code (long) retries() + 1}. The constructor stores caller-supplied counts without validation.</p>
     *
     * @return the number of invocations made before the policy gave up
     */
    public long attempts() {
        return attempts;
    }

    /**
     * Returns the number of retries the policy was configured with, that is, the number of invocations after
     * the first one.
     *
     * @return the configured retry count
     */
    public int retries() {
        return retries;
    }
}
