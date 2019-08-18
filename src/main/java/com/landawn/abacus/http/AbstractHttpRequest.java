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
 * @param <S>
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
     * @param httpClient
     */
    AbstractHttpRequest(AbstractHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Header.
     *
     * @param name
     * @param value
     * @return
     */
    public S header(String name, Object value) {
        checkSettings();

        settings.header(name, value);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @return
     */
    public S headers(String name1, Object value1, String name2, Object value2) {
        checkSettings();

        settings.headers(name1, value1, name2, value2);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param name3
     * @param value3
     * @return
     */
    public S headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        checkSettings();

        settings.headers(name1, value1, name2, value2, name3, value3);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param headers
     * @return
     */
    public S headers(Map<String, Object> headers) {
        checkSettings();

        settings.headers(headers);

        return (S) this;
    }

    /**
     * Headers.
     *
     * @param headers
     * @return
     */
    public S headers(HttpHeaders headers) {
        checkSettings();

        settings.headers(headers);

        return (S) this;
    }

    /**
     * Gets the.
     *
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    /**
     * Gets the.
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T get(Class<T> resultClass) throws UncheckedIOException {
        return get(resultClass, null);
    }

    /**
     * Gets the.
     *
     * @param query
     * @return
     */
    public String get(Object query) {
        return get(String.class, query);
    }

    /**
     * Gets the.
     *
     * @param <T>
     * @param resultClass
     * @param query
     * @return
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
     * @param body
     * @return
     */
    public String post(Object body) {
        return post(String.class, body);
    }

    /**
     * Post.
     *
     * @param <T>
     * @param resultClass
     * @param body
     * @return
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
     * @param body
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String put(Object body) throws UncheckedIOException {
        return put(String.class, body);
    }

    /**
     * Put.
     *
     * @param <T>
     * @param resultClass
     * @param body
     * @return
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
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    /**
     * Delete.
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public <T> T delete(Class<T> resultClass) throws UncheckedIOException {
        return delete(resultClass, null);
    }

    /**
     * Delete.
     *
     * @param query
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public String delete(Object query) throws UncheckedIOException {
        return delete(String.class, query);
    }

    /**
     * Delete.
     *
     * @param <T>
     * @param resultClass
     * @param query
     * @return
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
     * @return
     */
    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     * Async get.
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(Class<T> resultClass) {
        return asyncGet(resultClass, null);
    }

    /**
     * Async get.
     *
     * @param query
     * @return
     */
    public ContinuableFuture<String> asyncGet(Object query) {
        return asyncGet(String.class, query);
    }

    /**
     * Async get.
     *
     * @param <T>
     * @param resultClass
     * @param query
     * @return
     */
    public <T> ContinuableFuture<T> asyncGet(Class<T> resultClass, Object query) {
        this.httpMethod = HttpMethod.GET;
        this.request = query;

        return asyncExecute(resultClass);
    }

    /**
     * Async post.
     *
     * @param body
     * @return
     */
    public ContinuableFuture<String> asyncPost(Object body) {
        return asyncPost(String.class, body);
    }

    /**
     * Async post.
     *
     * @param <T>
     * @param resultClass
     * @param body
     * @return
     */
    public <T> ContinuableFuture<T> asyncPost(Class<T> resultClass, Object body) {
        this.httpMethod = HttpMethod.POST;
        this.request = body;

        return asyncExecute(resultClass);
    }

    /**
     * Async put.
     *
     * @param body
     * @return
     */
    public ContinuableFuture<String> asyncPut(Object body) {
        return asyncPut(String.class, body);
    }

    /**
     * Async put.
     *
     * @param <T>
     * @param resultClass
     * @param body
     * @return
     */
    public <T> ContinuableFuture<T> asyncPut(Class<T> resultClass, Object body) {
        this.httpMethod = HttpMethod.PUT;
        this.request = body;

        return asyncExecute(resultClass);
    }

    /**
     * Async delete.
     *
     * @return
     */
    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     * Async delete.
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(Class<T> resultClass) {
        return asyncDelete(resultClass, null);
    }

    /**
     * Async delete.
     *
     * @param query
     * @return
     */
    public ContinuableFuture<String> asyncDelete(Object query) {
        return asyncDelete(String.class, query);
    }

    /**
     * Async delete.
     *
     * @param <T>
     * @param resultClass
     * @param query
     * @return
     */
    public <T> ContinuableFuture<T> asyncDelete(Class<T> resultClass, Object query) {
        this.httpMethod = HttpMethod.DELETE;
        this.request = query;

        return asyncExecute(resultClass);
    }

    /**
     * Execute.
     *
     * @param <T>
     * @param resultClass
     * @return
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
     * @param <T>
     * @param resultClass
     * @return
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
