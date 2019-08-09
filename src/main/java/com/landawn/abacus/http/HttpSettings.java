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

import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class HttpSettings.
 *
 * @author Haiyang Li
 * @since 1.3
 */
public final class HttpSettings {

    /** The connection timeout. */
    private int connectionTimeout;

    /** The read timeout. */
    private int readTimeout;

    /** The ssl socket factory. */
    private SSLSocketFactory sslSocketFactory;

    /** The use caches. */
    private boolean useCaches = false;

    /** The do input. */
    private boolean doInput = true;

    /** The do output. */
    private boolean doOutput = true;

    /** The is one way request. */
    private boolean isOneWayRequest = false;

    /** The content format. */
    private ContentFormat contentFormat;

    /** The headers. */
    private HttpHeaders headers = null;

    /**
     * Instantiates a new http settings.
     */
    public HttpSettings() {
        super();
    }

    /**
     * Creates the.
     *
     * @return the http settings
     */
    public static HttpSettings create() {
        return new HttpSettings();
    }

    /**
     * Gets the connection timeout.
     *
     * @return the connection timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param connTimeout the conn timeout
     * @return the http settings
     */
    public HttpSettings setConnectionTimeout(int connTimeout) {
        this.connectionTimeout = connTimeout;

        return this;
    }

    /**
     * Gets the read timeout.
     *
     * @return the read timeout
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *  
     *
     * @param readTimeout the read timeout
     * @return the http settings
     */
    public HttpSettings setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Gets the SSL socket factory.
     *
     * @return the SSL socket factory
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * Sets the SSL socket factory.
     *
     * @param sslSocketFactory the ssl socket factory
     * @return the http settings
     */
    public HttpSettings setSSLSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

        return this;
    }

    /**
     * Gets the use caches.
     *
     * @return the use caches
     */
    public boolean getUseCaches() {
        return useCaches;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param useCaches the use caches
     * @return the http settings
     */
    public HttpSettings setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;

        return this;
    }

    /**
     * Do input.
     *
     * @return true, if successful
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public boolean doInput() {
        return doInput;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *  
     *
     * @param doInput the do input
     * @return the http settings
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    public HttpSettings doInput(boolean doInput) {
        this.doInput = doInput;

        return this;
    }

    /**
     * Do output.
     *
     * @return true, if successful
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public boolean doOutput() {
        return doOutput;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param doOutput the do output
     * @return the http settings
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    public HttpSettings doOutput(boolean doOutput) {
        this.doOutput = doOutput;

        return this;
    }

    /**
     * Checks if is one way request.
     *
     * @return true, if is one way request
     */
    public boolean isOneWayRequest() {
        return isOneWayRequest;
    }

    /**
     * Checks if is one way request.
     *
     * @param isOneWayRequest the is one way request
     * @return the http settings
     */
    public HttpSettings isOneWayRequest(boolean isOneWayRequest) {
        this.isOneWayRequest = isOneWayRequest;

        return this;
    }

    /**
     * Gets the content format.
     *
     * @return the content format
     */
    public ContentFormat getContentFormat() {
        if ((contentFormat == null || contentFormat == ContentFormat.NONE) && headers != null) {
            contentFormat = HTTP.getContentFormat((String) headers.get(HttpHeaders.Names.CONTENT_TYPE),
                    (String) headers.get(HttpHeaders.Names.CONTENT_ENCODING));
        }

        return contentFormat;
    }

    /**
     * Sets the content format.
     *
     * @param contentFormat the content format
     * @return the http settings
     */
    public HttpSettings setContentFormat(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;

        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            if (headers != null) {
                headers.remove(HttpHeaders.Names.CONTENT_TYPE);
                headers.remove(HttpHeaders.Names.CONTENT_ENCODING);
            }
        } else {
            final String contentType = HTTP.getContentType(contentFormat);

            if (N.isNullOrEmpty(contentType)) {
                headers.remove(HttpHeaders.Names.CONTENT_TYPE);
            } else {
                header(HttpHeaders.Names.CONTENT_TYPE, contentType);
            }

            final String contentEncoding = HTTP.getContentEncoding(contentFormat);

            if (N.isNullOrEmpty(contentEncoding)) {
                headers.remove(HttpHeaders.Names.CONTENT_ENCODING);
            } else {
                header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
            }
        }

        return this;
    }

    /**
     * Header.
     *
     * @param name the name
     * @param value the value
     * @return the http settings
     */
    public HttpSettings header(String name, Object value) {
        headers().set(name, value);

        return this;
    }

    /**
     * Headers.
     *
     * @param name1 the name 1
     * @param value1 the value 1
     * @param name2 the name 2
     * @param value2 the value 2
     * @return the http settings
     */
    public HttpSettings headers(String name1, Object value1, String name2, Object value2) {
        headers().set(name1, value1);
        headers().set(name2, value2);

        return this;
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
     * @return the http settings
     */
    public HttpSettings headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        headers().set(name1, value1);
        headers().set(name2, value2);
        headers().set(name3, value3);

        return this;
    }

    /**
     * Headers.
     *
     * @param headers the headers
     * @return the http settings
     */
    public HttpSettings headers(Map<String, Object> headers) {
        headers().setAll(headers);

        return this;
    }

    /**
     * Headers.
     *
     * @param headers the headers
     * @return the http settings
     */
    public HttpSettings headers(HttpHeaders headers) {
        if (headers == null) {
            this.headers = headers;
        } else {
            headers().setAll(headers.map);
        }

        return this;
    }

    /**
     * Headers.
     *
     * @return the http headers
     */
    public HttpHeaders headers() {
        if (headers == null) {
            headers = HttpHeaders.create();
        }

        return headers;
    }

    /**
     * Copy.
     *
     * @return the http settings
     */
    public HttpSettings copy() {
        return new HttpSettings().setConnectionTimeout(connectionTimeout)
                .setReadTimeout(readTimeout)
                .setSSLSocketFactory(sslSocketFactory)
                .setUseCaches(useCaches)
                .doInput(doInput)
                .doOutput(doOutput)
                .isOneWayRequest(isOneWayRequest)
                .setContentFormat(contentFormat)
                .headers(headers == null ? null : headers.copy());
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "{connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", useCaches="
                + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat=" + contentFormat
                + ", headers=" + headers + "}";
    }

}
