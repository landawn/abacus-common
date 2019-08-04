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

/**
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
public class HttpRequest extends AbstractHttpRequest<HttpRequest> {

    HttpRequest(HttpClient httpClient) {
        super(httpClient);
    }

    public static HttpRequest create(final HttpClient httpClient) {
        return new HttpRequest(httpClient);
    }

    public static HttpRequest url(final String url) {
        return url(url, AbstractHttpClient.DEFAULT_CONNECTION_TIMEOUT, AbstractHttpClient.DEFAULT_READ_TIMEOUT);
    }

    public static HttpRequest url(final String url, final long connTimeout, final long readTimeout) {
        return new HttpRequest(HttpClient.create(url, 1, connTimeout, readTimeout));
    }
}
