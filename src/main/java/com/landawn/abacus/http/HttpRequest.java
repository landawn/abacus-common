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

// TODO: Auto-generated Javadoc
/**
 * The Class HttpRequest.
 *
 * @author Haiyang Li
 * @since 1.3
 */
public class HttpRequest extends AbstractHttpRequest<HttpRequest> {

    /**
     * Instantiates a new http request.
     *
     * @param httpClient the http client
     */
    HttpRequest(HttpClient httpClient) {
        super(httpClient);
    }

    /**
     * Creates the.
     *
     * @param httpClient the http client
     * @return the http request
     */
    public static HttpRequest create(final HttpClient httpClient) {
        return new HttpRequest(httpClient);
    }

    /**
     * Url.
     *
     * @param url the url
     * @return the http request
     */
    public static HttpRequest url(final String url) {
        return url(url, AbstractHttpClient.DEFAULT_CONNECTION_TIMEOUT, AbstractHttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     * Url.
     *
     * @param url the url
     * @param connTimeout the conn timeout
     * @param readTimeout the read timeout
     * @return the http request
     */
    public static HttpRequest url(final String url, final long connTimeout, final long readTimeout) {
        return new HttpRequest(HttpClient.create(url, 1, connTimeout, readTimeout));
    }
}
