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
 * The Class OKHttpRequest.
 *
 * @author Haiyang Li
 * @since 1.3
 */
public class OKHttpRequest extends AbstractHttpRequest<OKHttpRequest> {

    /**
     * Instantiates a new OK http request.
     *
     * @param okHttpClient the ok http client
     */
    OKHttpRequest(final OKHttpClient okHttpClient) {
        super(okHttpClient);
    }

    /**
     * Creates the.
     *
     * @param okHttpClient the ok http client
     * @return the OK http request
     */
    public static OKHttpRequest create(final OKHttpClient okHttpClient) {
        return new OKHttpRequest(okHttpClient);
    }

    /**
     * Url.
     *
     * @param url the url
     * @return the OK http request
     */
    public static OKHttpRequest url(String url) {
        return url(url, AbstractHttpClient.DEFAULT_CONNECTION_TIMEOUT, AbstractHttpClient.DEFAULT_READ_TIMEOUT);
    }

    /**
     * Url.
     *
     * @param url the url
     * @param connTimeout the conn timeout
     * @param readTimeout the read timeout
     * @return the OK http request
     */
    public static OKHttpRequest url(String url, long connTimeout, long readTimeout) {
        return new OKHttpRequest(OKHttpClient.create(url, 1, connTimeout, readTimeout));
    }
}
