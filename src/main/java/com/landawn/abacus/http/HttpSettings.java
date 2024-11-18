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

import java.net.Proxy;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Strings;

public final class HttpSettings {

    private long connectionTimeout;

    private long readTimeout;

    private boolean useCaches = false;

    private boolean doInput = true;

    private boolean doOutput = true;

    private boolean isOneWayRequest = false;

    private ContentFormat contentFormat;

    private HttpHeaders headers = null;

    private SSLSocketFactory sslSocketFactory;

    private Proxy proxy;

    public HttpSettings() { //NOSONAR
    }

    public static HttpSettings create() {
        return new HttpSettings();
    }

    /**
     * Gets the connection timeout.
     *
     * @return
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param connectionTimeout
     * @return
     */
    public HttpSettings setConnectionTimeout(final long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;

        return this;
    }

    /**
     * Gets the read timeout.
     *
     * @return
     */
    public long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     *
     * @param readTimeout
     * @return
     */
    public HttpSettings setReadTimeout(final long readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Gets the SSL socket factory.
     *
     * @return
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
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

    public Proxy getProxy() {
        return proxy;
    }

    /**
     *
     * @param proxy
     * @return
     */
    public HttpSettings setProxy(final Proxy proxy) {
        this.proxy = proxy;

        return this;
    }

    /**
     * Gets the use caches.
     *
     * @return
     */
    public boolean getUseCaches() { // NOSONAR
        return useCaches;
    }

    /**
     * Note: Only for {@code HttpClient}, not for {@code OKHttpClient}.
     *
     * @param useCaches
     * @return
     */
    public HttpSettings setUseCaches(final boolean useCaches) {
        this.useCaches = useCaches;

        return this;
    }

    /**
     *
     * @return {@code true}, if successful
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
    public HttpSettings doInput(final boolean doInput) {
        this.doInput = doInput;

        return this;
    }

    /**
     *
     * @return {@code true}, if successful
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
    public HttpSettings doOutput(final boolean doOutput) {
        this.doOutput = doOutput;

        return this;
    }

    /**
     * Checks if is one way request.
     *
     * @return {@code true}, if is one way request
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
    public HttpSettings isOneWayRequest(final boolean isOneWayRequest) {
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
            contentFormat = HttpUtil.getContentFormat(HttpUtil.getContentType(headers), HttpUtil.getContentEncoding(headers));
        }

        return contentFormat;
    }

    /**
     * Sets the content format.
     *
     * @param contentFormat
     * @return
     */
    public HttpSettings setContentFormat(final ContentFormat contentFormat) {
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
        //            if (N.isEmpty(newContentType)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_TYPE);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_TYPE, newContentType);
        //            }
        //
        //            final String newContentEncoding = HTTP.getContentEncoding(contentFormat);
        //
        //            if (N.isEmpty(newContentEncoding)) {
        //                headers().remove(HttpHeaders.Names.CONTENT_ENCODING);
        //            } else {
        //                header(HttpHeaders.Names.CONTENT_ENCODING, newContentEncoding);
        //            }
        //        }
        //    }

        return this;
    }

    public String getContentType() {
        String contentType = HttpUtil.getContentType(headers);

        if (Strings.isEmpty(contentType) && contentFormat != null) {
            contentType = HttpUtil.getContentType(contentFormat);

            if (Strings.isNotEmpty(contentType)) {
                header(HttpHeaders.Names.CONTENT_TYPE, contentType);
            }
        }

        return contentType;
    }

    /**
     *
     * @param contentType
     * @return
     */
    public HttpSettings setContentType(final String contentType) {
        header(HttpHeaders.Names.CONTENT_TYPE, contentType);

        return this;
    }

    public String getContentEncoding() {
        String contentEncoding = HttpUtil.getContentEncoding(headers);

        if (Strings.isEmpty(contentEncoding) && contentFormat != null) {
            contentEncoding = HttpUtil.getContentEncoding(contentFormat);

            if (Strings.isNotEmpty(contentEncoding)) {
                header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
            }
        }

        return contentEncoding;
    }

    /**
     *
     * @param contentEncoding
     * @return
     */
    public HttpSettings setContentEncoding(final String contentEncoding) {
        header(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);

        return this;
    }

    /**
     *
     * @param user
     * @param password
     * @return
     */
    public HttpSettings basicAuth(final String user, final Object password) {
        return header(HttpHeaders.Names.AUTHORIZATION, "Basic " + Strings.base64Encode((user + ":" + password).getBytes(Charsets.UTF_8)));
    }

    /**
     * Set http header specified by {@code name/value}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name
     * @param value
     * @return
     * @see HttpHeaders
     */
    public HttpSettings header(final String name, final Object value) {
        headers().set(name, value);

        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @return
     * @see HttpHeaders
     */
    public HttpSettings headers(final String name1, final Object value1, final String name2, final Object value2) {
        headers().set(name1, value1);
        headers().set(name2, value2);

        return this;
    }

    /**
     * Set http headers specified by {@code name1/value1}, {@code name2/value2}, {@code name3/value3}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param name1
     * @param value1
     * @param name2
     * @param value2
     * @param name3
     * @param value3
     * @return
     * @see HttpHeaders
     */
    public HttpSettings headers(final String name1, final Object value1, final String name2, final Object value2, final String name3, final Object value3) {
        headers().set(name1, value1);
        headers().set(name2, value2);
        headers().set(name3, value3);

        return this;
    }

    /**
     * Set http headers specified by the key/value entities from {@code Map}.
     * If this {@code HttpSettings} already has any headers with that name, they are all replaced.
     *
     * @param headers
     * @return
     * @see HttpHeaders
     */
    public HttpSettings headers(final Map<String, ?> headers) {
        headers().setAll(headers);

        return this;
    }

    /**
     * Removes all headers on this {@code HttpSettings} and adds {@code headers}.
     *
     * @param headers
     * @return
     * @see HttpHeaders
     */
    public HttpSettings headers(final HttpHeaders headers) {
        headers().clear();

        if (headers != null) {
            headers().setAll(headers.map);
        }

        return this;
    }

    /**
     *
     * @return
     * @see HttpHeaders
     */
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
                .setProxy(proxy)
                .setUseCaches(useCaches)
                .doInput(doInput)
                .doOutput(doOutput)
                .isOneWayRequest(isOneWayRequest)
                .setContentFormat(contentFormat)
                .headers(headers == null ? null : headers.copy());
    }

    @Override
    public String toString() {
        return "{connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", sslSocketFactory=" + sslSocketFactory + ", proxy=" + proxy
                + ", useCaches=" + useCaches + ", doInput=" + doInput + ", doOutput=" + doOutput + ", isOneWayRequest=" + isOneWayRequest + ", contentFormat="
                + contentFormat + ", headers=" + headers + "}";
    }

}
