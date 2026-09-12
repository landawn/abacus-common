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
 * Fluent HTTP request support built on the Java 11 {@link java.net.http.HttpClient}.
 *
 * <p>{@link HttpRequest} constructs headers, query parameters, bodies, authentication, and timeouts,
 * then executes the request synchronously or as a {@link java.util.concurrent.CompletableFuture}.
 * It reuses {@link com.landawn.abacus.http.HttpHeaders}, {@link com.landawn.abacus.http.HttpMethod},
 * {@link com.landawn.abacus.http.ContentFormat}, and {@link com.landawn.abacus.http.HttpUtil} from the
 * parent package, and retains access to the JDK {@link java.net.http.HttpResponse} model (including
 * custom {@link java.net.http.HttpResponse.BodyHandler}s and HTTP/2).</p>
 *
 * <p>Builders are mutable and not thread-safe; the underlying JDK client is thread-safe and reused
 * when possible. Overloads that take a result {@code Class} throw
 * {@link com.landawn.abacus.exception.HttpResponseException} for non-2xx responses. Close streaming
 * {@link java.io.InputStream} bodies so request-owned clients are released.</p>
 *
 * <p>Prefer this package when HTTP/2, PATCH, or JDK body handlers are required. Prefer
 * {@link com.landawn.abacus.http.HttpRequest} / {@link com.landawn.abacus.http.HttpClient} for the
 * {@link java.net.HttpURLConnection} client, or {@link com.landawn.abacus.http.OkHttpRequest} when
 * OkHttp is already in use.</p>
 *
 * @see HttpRequest
 * @see java.net.http.HttpClient
 */
package com.landawn.abacus.http.v2;
