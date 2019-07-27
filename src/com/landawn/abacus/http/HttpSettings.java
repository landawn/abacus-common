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

import javax.net.ssl.SSLSocketFactory;

/**
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
public final class HttpSettings {
    private int connectionTimeout;
    private int readTimeout;
    private SSLSocketFactory sslSocketFactory;
    private boolean useCaches = false;
    private boolean doInput = true;
    private boolean doOutput = true;
    private boolean isOneWayRequest = false;
    private ContentFormat contentFormat;
    private HttpHeaders headers = null;

    public HttpSettings() {
        super();
    }

    public static HttpSettings create() {
        return new HttpSettings();
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     * 
     * @param connTimeout
     * @return
     */
    public HttpSettings setConnectionTimeout(int connTimeout) {
        this.connectionTimeout = connTimeout;

        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *  
     * @param readTimeout
     * @return
     */
    public HttpSettings setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * 
     * @param sslSocketFactory
     * @return
     */
    public HttpSettings setSSLSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

        return this;
    }

    public boolean getUseCaches() {
        return useCaches;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     * 
     * @param useCaches
     * @return
     */
    public HttpSettings setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;

        return this;
    }

    /**
     * 
     * @return
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public boolean doInput() {
        return doInput;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *  
     * @param doInput
     * @return
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public HttpSettings doInput(boolean doInput) {
        this.doInput = doInput;

        return this;
    }

    /**
     * 
     * @return
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public boolean doOutput() {
        return doOutput;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     * 
     * @param doOutput
     * @return
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public HttpSettings doOutput(boolean doOutput) {
        this.doOutput = doOutput;

        return this;
    }

    public boolean isOneWayRequest() {
        return isOneWayRequest;
    }

    public HttpSettings isOneWayRequest(boolean isOneWayRequest) {
        this.isOneWayRequest = isOneWayRequest;

        return this;
    }

    public ContentFormat getContentFormat() {
        return contentFormat;
    }

    public HttpSettings setContentFormat(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;

        return this;
    }

    public HttpSettings header(String name, Object value) {
        headers().set(name, value);

        return this;
    }

    public HttpSettings headers(String name1, Object value1, String name2, Object value2) {
        headers().set(name1, value1);
        headers().set(name2, value2);

        return this;
    }

    public HttpSettings headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        headers().set(name1, value1);
        headers().set(name2, value2);
        headers().set(name3, value3);

        return this;
    }

    public HttpSettings headers(Map<String, Object> headers) {
        headers().setAll(headers);

        return this;
    }

    public HttpSettings headers(HttpHeaders headers) {
        if (headers == null) {
            this.headers = headers;
        } else {
            headers().setAll(headers.map);
        }

        return this;
    }

    public HttpHeaders headers() {
        if (headers == null) {
            headers = HttpHeaders.create();
        }

        return headers;
    }

    public HttpSettings copy() {
        return new HttpSettings().setConnectionTimeout(connectionTimeout)
                .setReadTimeout(readTimeout)
                .setSSLSocketFactory(sslSocketFactory)
                .setUseCaches(useCaches)
                .doInput(doInput)
                .doOutput(doOutput)
                .isOneWayRequest(isOneWayRequest)
                .setContentFormat(contentFormat)
                .headers(headers.copy());
    }

    @Override
    public String toString() {
        return "{connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", useCaches="
                + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat=" + contentFormat
                + ", headers=" + headers + "}";
    }

}
