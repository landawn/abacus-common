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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * HttpMethod method = HttpMethod.GET;
 * String methodName = method.name(); // "GET"
 *
 * // Using with HTTP clients
 * String response = HttpRequest.url("https://api.example.com/users")
 *     .body("{\"name\":\"John\"}")
 *     .execute(HttpMethod.POST, String.class);
 * }</pre>
 * 
 * <p>Note: The PATCH method is not supported by {@link java.net.HttpURLConnection}
 * in the standard Java HTTP client implementation.</p>
 */
public enum HttpMethod {
    /**
     * The GET method requests a representation of the specified resource.
     * Requests using GET should only retrieve data and should have no other effect.
     */
    GET,

    /**
     * The POST method submits data to be processed to a specified resource.
     * Often used to create new resources or submit form data.
     */
    POST,

    /**
     * The PUT method replaces all current representations of the target resource with the request payload.
     * Typically used to update or create a resource at a specific URI.
     */
    PUT,

    /**
     * The DELETE method deletes the specified resource.
     */
    DELETE,

    /**
     * The HEAD method asks for a response identical to a GET request, but without the response body.
     * Useful for retrieving metadata without transferring the entire resource.
     */
    HEAD,

    /**
     * The OPTIONS method describes the communication options for the target resource.
     * Often used to determine which HTTP methods are supported by the server.
     */
    OPTIONS,

    /**
     * The TRACE method performs a message loop-back test along the path to the target resource.
     * Used for diagnostic purposes to echo back the received request.
     */
    TRACE,

    /**
     * The CONNECT method establishes a tunnel to the server identified by the target resource.
     * Typically used to create an SSL tunnel through an HTTP proxy.
     */
    CONNECT,

    /**
     * The PATCH method applies partial modifications to a resource.
     * Unlike PUT, PATCH is used for partial updates rather than complete replacement.
     * Note: Not supported by {@link java.net.HttpURLConnection} in standard Java HTTP client.
     */
    PATCH
}
