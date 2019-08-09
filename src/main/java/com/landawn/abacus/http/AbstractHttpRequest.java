/*
 * Copyright (C) 2019 HaiYang Li
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

package com.landawn.abacus.http;

import java.util.Map;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ContinuableFuture;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractHttpRequest.
 *
 * @author Haiyang Li
 * @param <S> the generic type
 * @since 1.3
 */
abstract class AbstractHttpRequest<S extends AbstractHttpRequest<S>> {

    /** The http client. */
    final AbstractHttpClient httpClient;

    /** The http method. */
    HttpMethod httpMethod;

    /** The settings. */
    HttpSettings settings;

    /** The request. */
    Object request;

    /**
     * Instantiates a new abstract http request.
     *
     * @param httpClient the http client
     */
    AbstractHttpRequest(AbstractHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Header.
     *
     * @param name the name
     * @param value the value
     * @return the s
     */
    public S header(String name, Object value) {
        checkSettings();

        settings.header(name, value);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param name1 the name 1
     * @param value1 the value 1
     * @param name2 the name 2
     * @param value2 the value 2
     * @return the s
     */
    public S headers(String name1, Object value1, String name2, Object value2) {
        checkSettings();

        settings.headers(name1, value1, name2, value2);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param name1 the name 1
     * @param value1 the value 1
     * @param name2 the name 2
     * @param value2 the value 2
     * @param name3 the name 3
     * @param value3 the value 3
     * @return the s
     */
    public S headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        checkSettings();

        settings.headers(name1, value1, name2, value2, name3, value3);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param headers the headers
     * @return the s
     */
    public S headers(Map<String, Object> headers) {
        checkSettings();

        settings.headers(headers);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param headers the headers
     * @return the s
     */
    public S headers(HttpHeaders headers) {
        checkSettings();

        settings.headers(headers);

        return (S) this;
    }

    /**
     * Gets the.
     *
     * @return the string
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    /**
     * Gets the.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(Class<T> resultClass) throws UncheckedIOException {
        return get(resultClass, null);
    }

    /**
     * Gets the.
     *
     * @param query the query
     * @return the string
     */
    public String get(Object query) {
        return get(String.class, query);
    }

    /**
     * Gets the.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param query the query
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(Class<T> resultClass, Object query) throws UncheckedIOException {
        this.httpMethod = HttpMethod.GET;
        this.request = query;

        return execute(resultClass);
    }

    /**
     * Post.
     *
     * @param body the body
     * @return the string
     */
    public String post(Object body) {
        return post(String.class, body);
    }

    /**
     * Post.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param body the body
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T post(Class<T> resultClass, Object body) throws UncheckedIOException {
        this.httpMethod = HttpMethod.POST;
        this.request = body;

        return execute(resultClass);
    }

    /**
     * Put.
     *
     * @param body the body
     * @return the string
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(Object body) throws UncheckedIOException {
        return put(String.class, body);
    }

    /**
     * Put.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param body the body
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T put(Class<T> resultClass, Object body) throws UncheckedIOException {
        this.httpMethod = HttpMethod.PUT;
        this.request = body;

        return execute(resultClass);
    }

    /**
     * Delete.
     *
     * @return the string
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    /**
     * Delete.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(Class<T> resultClass) throws UncheckedIOException {
        return delete(resultClass, null);
    }

    /**
     * Delete.
     *
     * @param query the query
     * @return the string
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(Object query) throws UncheckedIOException {
        return delete(String.class, query);
    }

    /**
     * Delete.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param query the query
     * @return the t
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(Class<T> resultClass, Object query) throws UncheckedIOException {
        this.httpMethod = HttpMethod.DELETE;
        this.request = query;

        return execute(resultClass);
    }

    /**
     * Async get.
     *
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     * Async get.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncGet(Class<T> resultClass) {
        return asyncGet(resultClass, null);
    }

    /**
     * Async get.
     *
     * @param query the query
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncGet(Object query) {
        return asyncGet(String.class, query);
    }

    /**
     * Async get.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param query the query
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncGet(Class<T> resultClass, Object query) {
        this.httpMethod = HttpMethod.GET;
        this.request = query;

        return asyncExecute(resultClass);
    }

    /**
     * Async post.
     *
     * @param body the body
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncPost(Object body) {
        return asyncPost(String.class, body);
    }

    /**
     * Async post.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param body the body
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncPost(Class<T> resultClass, Object body) {
        this.httpMethod = HttpMethod.POST;
        this.request = body;

        return asyncExecute(resultClass);
    }

    /**
     * Async put.
     *
     * @param body the body
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncPut(Object body) {
        return asyncPut(String.class, body);
    }

    /**
     * Async put.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param body the body
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncPut(Class<T> resultClass, Object body) {
        this.httpMethod = HttpMethod.PUT;
        this.request = body;

        return asyncExecute(resultClass);
    }

    /**
     * Async delete.
     *
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     * Async delete.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncDelete(Class<T> resultClass) {
        return asyncDelete(resultClass, null);
    }

    /**
     * Async delete.
     *
     * @param query the query
     * @return the continuable future
     */
    public ContinuableFuture<String> asyncDelete(Object query) {
        return asyncDelete(String.class, query);
    }

    /**
     * Async delete.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @param query the query
     * @return the continuable future
     */
    public <T> ContinuableFuture<T> asyncDelete(Class<T> resultClass, Object query) {
        this.httpMethod = HttpMethod.DELETE;
        this.request = query;

        return asyncExecute(resultClass);
    }

    /**
     * Execute.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the t
     */
    protected <T> T execute(final Class<T> resultClass) {
        if (httpMethod == null) {
            throw new RuntimeException("HTTP method is not set");
        }

        switch (httpMethod) {
            case GET:
                return httpClient.get(resultClass, request, settings);

            case POST:
                return httpClient.post(resultClass, request, settings);

            case PUT:
                return httpClient.put(resultClass, request, settings);

            case DELETE:
                return httpClient.delete(resultClass, request, settings);

            default:
                throw new RuntimeException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Async execute.
     *
     * @param <T> the generic type
     * @param resultClass the result class
     * @return the continuable future
     */
    protected <T> ContinuableFuture<T> asyncExecute(final Class<T> resultClass) {
        if (httpMethod == null) {
            throw new RuntimeException("HTTP method is not set");
        }

        switch (httpMethod) {
            case GET:
                return httpClient.asyncGet(resultClass, request, settings);

            case POST:
                return httpClient.asyncPost(resultClass, request, settings);

            case PUT:
                return httpClient.asyncPut(resultClass, request, settings);

            case DELETE:
                return httpClient.asyncDelete(resultClass, request, settings);

            default:
                throw new RuntimeException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Check settings.
     */
    protected void checkSettings() {
        if (settings == null) {
            settings = new HttpSettings();
        }
    }
}
