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

package com.landawn.abacus.http;

/**
 * Enum representing standard HTTP request methods.
 * 
 * <p>This enum defines the common HTTP methods used in RESTful web services
 * and HTTP communication. Each method has specific semantics defined by the
 * HTTP specification.</p>
 * 
 * <p>Common HTTP methods:</p>
 * <ul>
 *   <li>{@link #GET} - Retrieve a resource</li>
 *   <li>{@link #POST} - Create a new resource or submit data</li>
 *   <li>{@link #PUT} - Update or replace a resource</li>
 *   <li>{@link #DELETE} - Remove a resource</li>
 *   <li>{@link #HEAD} - Retrieve headers only (no body)</li>
 *   <li>{@link #OPTIONS} - Retrieve allowed methods for a resource</li>
 *   <li>{@link #PATCH} - Partially update a resource</li>
 *   <li>{@link #TRACE} - Echo the request for debugging</li>
 *   <li>{@link #CONNECT} - Establish a tunnel (typically for HTTPS through proxy)</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * HttpMethod method = HttpMethod.GET;
 * String methodName = method.name(); // "GET"
 * 
 * // Using with HTTP clients
 * HttpRequest.url("https://api.example.com/users")
 *     .execute(HttpMethod.POST, requestBody);
 * }</pre>
 * 
 * <p>Note: The PATCH method is not supported by {@link java.net.HttpURLConnection}
 * in the standard Java HTTP client implementation.</p>
 */
public enum HttpMethod {
    GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, CONNECT, PATCH
}