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

/**
 * Runtime exceptions used by Abacus APIs.
 *
 * <h2>Unchecked wrappers</h2>
 * <p>{@link UncheckedException} wraps a checked exception so it can be thrown from lambdas and other
 * signatures that do not declare checked exceptions. Specialized subclasses preserve the original
 * exception type:</p>
 * <ul>
 *   <li>{@link UncheckedIOException} for {@link java.io.IOException}</li>
 *   <li>{@link UncheckedSQLException} for {@link java.sql.SQLException}</li>
 *   <li>{@link UncheckedInterruptedException} for {@link InterruptedException}</li>
 *   <li>{@link UncheckedExecutionException} for {@link java.util.concurrent.ExecutionException}</li>
 *   <li>{@link UncheckedParseException} for {@link java.text.ParseException}</li>
 *   <li>{@link UncheckedReflectiveOperationException} for {@link ReflectiveOperationException}</li>
 * </ul>
 *
 * <h2>Result cardinality</h2>
 * <ul>
 *   <li>{@link DuplicateResultException} &mdash; an operation that required a unique result found more than one.</li>
 *   <li>{@link TooManyElementsException} &mdash; a collection or stream exceeded an expected element count.</li>
 *   <li>{@link ObjectNotFoundException} &mdash; a lookup that was required to succeed found nothing
 *       ({@link java.util.NoSuchElementException} subclass).</li>
 * </ul>
 *
 * <h2>Parsing, retry, and HTTP</h2>
 * <ul>
 *   <li>{@link ParsingException} &mdash; a parser rejected its input (JSON, XML, CSV, and similar).</li>
 *   <li>{@link RetryExhaustedException} &mdash; a {@linkplain com.landawn.abacus.util.Retry retry} policy
 *       used every allowed attempt and the final <em>result</em> was still unacceptable. If the final
 *       attempt threw, that exception is rethrown instead.</li>
 *   <li>{@link HttpResponseException} &mdash; an HTTP call completed with a non-2xx status and the caller
 *       asked for a deserialized result rather than a raw {@link com.landawn.abacus.http.HttpResponse}.
 *       It is an {@link UncheckedIOException} that also exposes status, headers, and a bounded body prefix.</li>
 * </ul>
 *
 * @see UncheckedException
 * @see ParsingException
 * @see HttpResponseException
 */
package com.landawn.abacus.exception;
