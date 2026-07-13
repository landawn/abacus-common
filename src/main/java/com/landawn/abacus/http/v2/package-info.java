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
 * Fluent HTTP request support built on the Java HTTP Client introduced in Java 11.
 *
 * <p>{@link com.landawn.abacus.http.v2.HttpRequest} provides request construction, body conversion,
 * synchronous execution, and asynchronous execution while retaining access to the underlying
 * {@link java.net.http.HttpClient} response model.</p>
 */
package com.landawn.abacus.http.v2;
