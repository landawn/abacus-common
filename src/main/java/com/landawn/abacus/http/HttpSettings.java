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
     *
     * @return
     */
    public static HttpSettings create() {
        return new HttpSettings();
    }

    /**
     * Gets the connection timeout.
     *
     * @return
     */
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

    /**
     * Gets the read timeout.
     *
     * @return
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     *
     * @param readTimeout
     * @return
     */
    public HttpSettings setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Gets the SSL socket factory.
     *
     * @return
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return this.sslSocketFactory;
    }

    /**
     * Sets the SSL socket factory.
     *
     * @param sslSocketFactory
     * @return
     */
    public HttpSettings setSSLSocketFactory(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

        return this;
    }

    /**
     * Gets the use caches.
     *
     * @return
     */
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
     * @return true, if successful
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
     * @param isOneWayRequest
     * @return
     */
    public HttpSettings isOneWayRequest(boolean isOneWayRequest) {
        this.isOneWayRequest = isOneWayRequest;

        return this;
    }

    /**
     * Gets the content format.
     *
     * @return
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
     * @param contentFormat
     * @return
     */
    public HttpSettings setContentFormat(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;

        //    if (contentFormat == null || contentFormat == ContentFormat.NONE) {
        //        if (headers != null) {
        //            headers.remove(HttpHeaders.Names.CONTENT_TYPE);
        //            headers.remove(HttpHeaders.Names.CONTENT_ENCODING);
        //        }
        //    } else {
        //        final String contentType = N.toString(headers().get(HttpHeaders.Names.CONTENT_TYPE));
        //        final String contentEncoding = N.toString(headers().get(HttpHeaders.Names.CONTENT_ENCODING));
        //
        //        if (!contentFormat.equals(HTTP.getContentFormat(contentType, contentEncoding))) {
        //            final String newContentType = HTTP.getContentType(contentFormat);
        //
        //            if (N.isNullOrEmpty(newContentType)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_TYPE);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_TYPE, newContentType);
        //            }
        //
        //            final String newContentEncoding = HTTP.getContentEncoding(contentFormat);
        //
        //            if (N.isNullOrEmpty(newContentEncoding)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_ENCODING);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_ENCODING, newContentEncoding);
        //            }
        //        }
        //    }

        return this;
    }

    public HttpSettings setContentType(final String contentType) {
        header(HttpHeaders.Names.CONTENT_TYPE, contentType);

        return this;
    }

    public String getContentType() {
        String contentType = (String) headers().get(HttpHeaders.Names.CONTENT_TYPE);

        if (N.isNullOrEmpty(contentType) && contentFormat != null) {
            contentType = HTTP.getContentType(contentFormat);

            if (N.notNullOrEmpty(contentType)) {
                header(HttpHeaders.Names.CONTENT_TYPE, contentType);
            }
        }

        return contentType;
    }

    public HttpSettings setContentEncoding(final String contentEncoding) {
        header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);

        return this;
    }

    public String getContentEncoding() {
        String contentEncoding = (String) headers().get(HttpHeaders.Names.CONTENT_ENCODING);

        if (N.isNullOrEmpty(contentEncoding) && contentFormat != null) {
            contentEncoding = HTTP.getContentEncoding(contentFormat);

            if (N.notNullOrEmpty(contentEncoding)) {
                header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
            }
        }

        return contentEncoding;
    }

    /**
     *
     * @param name
     * @param value
     * @return
     */
    public HttpSettings header(String name, Object value) {
        headers().set(name, value);

        return this;
    }

    /**
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @return
     */
    public HttpSettings headers(String name1, Object value1, String name2, Object value2) {
        headers().set(name1, value1);
        headers().set(name2, value2);

        return this;
    }

    /**
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param name3
     * @param value3
     * @return
     */
    public HttpSettings headers(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        headers().set(name1, value1);
        headers().set(name2, value2);
        headers().set(name3, value3);

        return this;
    }

    /**
     *
     * @param headers
     * @return
     */
    public HttpSettings headers(Map<String, Object> headers) {
        headers().setAll(headers);

        return this;
    }

    /**
     *
     * @param headers
     * @return
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
     *
     * @return
     */
    public HttpHeaders headers() {
        if (headers == null) {
            headers = HttpHeaders.create();
        }

        return headers;
    }

    /**
     *
     * @return
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
     *
     * @return
     */
    @Override
    public String toString() {
        return "{connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", useCaches="
                + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat=" + contentFormat
                + ", headers=" + headers + "}";
    }

}
