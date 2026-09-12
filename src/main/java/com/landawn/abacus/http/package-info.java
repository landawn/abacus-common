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
 * HTTP clients, fluent request builders, headers, content conversion, and cURL/HAR helpers.
 *
 * <h2>HttpURLConnection client</h2>
 * <p>{@link HttpClient} is a thread-safe client on {@link java.net.HttpURLConnection}. It serializes
 * request bodies, deserializes responses, and optionally compresses payloads according to
 * {@link ContentFormat}. {@link HttpRequest} is the fluent builder for a single request; it can
 * create a dedicated client per URL or reuse a shared {@link HttpClient}. Asynchronous execution
 * returns {@link com.landawn.abacus.util.ContinuableFuture}.</p>
 *
 * <p>{@link java.net.HttpURLConnection} cannot issue {@link HttpMethod#PATCH} or {@link HttpMethod#CONNECT};
 * those methods are rejected before a connection is opened. For HTTP/2, PATCH, and the JDK
 * {@link java.net.http.HttpClient}, use {@link com.landawn.abacus.http.v2.HttpRequest}.</p>
 *
 * <h2>OkHttp</h2>
 * <p>{@link OkHttpRequest} is an optional fluent builder on OkHttp. It is available when OkHttp is
 * on the classpath. {@link WebUtil#createCurlLoggingOkHttpRequest} attaches cURL logging of outgoing
 * requests.</p>
 *
 * <h2>Shared types</h2>
 * <ul>
 *   <li>{@link HttpResponse} &mdash; status, headers, and body of a completed call</li>
 *   <li>{@link HttpHeaders} / {@link HttpSettings} &mdash; headers and per-request options (timeouts, proxy, SSL)</li>
 *   <li>{@link HttpMethod} / {@link ContentFormat} &mdash; verbs and content-type plus content-encoding pairs
 *       (JSON/XML with optional LZ4, Snappy, GZIP; Brotli on the decode path only)</li>
 *   <li>{@link HttpUtil} &mdash; content-type detection, stream wrapping, and shared HTTP helpers</li>
 *   <li>{@link WebUtil} &mdash; cURL &harr; Java request conversion and cURL command building</li>
 *   <li>{@link HARUtil} &mdash; parse HTTP Archive files and replay captured requests</li>
 * </ul>
 *
 * <p>Non-2xx responses throw {@link com.landawn.abacus.exception.HttpResponseException} when the
 * caller asked for a deserialized result type rather than a raw {@link HttpResponse}.</p>
 *
 * @see HttpClient
 * @see HttpRequest
 * @see OkHttpRequest
 * @see com.landawn.abacus.http.v2.HttpRequest
 */
package com.landawn.abacus.http;
