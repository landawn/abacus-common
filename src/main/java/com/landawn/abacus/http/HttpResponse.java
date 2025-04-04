/*
 * Copyright (C) 2019 HaiYang Li
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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.cs;

public class HttpResponse {
    private final String requestUrl;

    private final long requestSentAtMillis;

    private final long responseReceivedAtMillis;

    private final int statusCode;

    private final String message;

    private final Map<String, List<String>> headers;

    private final byte[] body;

    private final ContentFormat bodyFormat;

    private final Charset respCharset;

    HttpResponse(final String requestUrl, final long requestSentAtMillis, final long responseReceivedAtMillis, final int statusCode, final String message,
            final Map<String, List<String>> headers, final byte[] body, final ContentFormat bodyFormat, final Charset respCharset) {
        this.requestUrl = requestUrl;
        this.requestSentAtMillis = requestSentAtMillis;
        this.responseReceivedAtMillis = responseReceivedAtMillis;
        this.statusCode = statusCode;
        this.message = message;
        this.headers = headers;
        this.body = body;
        this.bodyFormat = bodyFormat == null ? ContentFormat.NONE : bodyFormat;
        this.respCharset = respCharset;
    }

    /**
     * Returns {@code true} if the code is in [200..300), which means the request was successfully received,
     * understood, and accepted.
     *
     * @return {@code true}, if is successful
     */
    public boolean isSuccessful() {
        return HttpUtil.isSuccessfulResponseCode(statusCode);
    }

    public String requestUrl() {
        return requestUrl;
    }

    /**
     * Sent request at millis.
     *
     * @return
     */
    public long requestSentAtMillis() {
        return requestSentAtMillis;
    }

    /**
     * Received response at millis.
     *
     * @return
     */
    public long responseReceivedAtMillis() {
        return responseReceivedAtMillis;
    }

    //    /**
    //     *
    //     * @return
    //     * @deprecated replaced with {@code status}
    //     * @see #status()
    //     */
    //    @Deprecated
    //    public int code() {
    //        return status;
    //    }

    public int statusCode() {
        return statusCode;
    }

    public String message() {
        return message;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public byte[] body() {
        return body;
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     * @throws IllegalArgumentException
     */
    public <T> T body(final Class<T> resultClass) throws IllegalArgumentException {
        N.checkArgNotNull(resultClass, cs.resultClass);

        if (resultClass == null || resultClass.equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (byte[].class.equals(resultClass)) {
            return (T) body;
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(new ByteArrayInputStream(body), resultClass);
            } else if (bodyFormat == ContentFormat.FormUrlEncoded) {
                return URLEncodedUtil.decode(new String(body, respCharset), resultClass);
            } else {
                return HttpUtil.getParser(bodyFormat).deserialize(new String(body, respCharset), resultClass);
            }
        }
    }

    /**
     *
     * @param <T>
     * @param resultType
     * @return
     * @throws IllegalArgumentException
     */
    public <T> T body(final Type<T> resultType) throws IllegalArgumentException {
        N.checkArgNotNull(resultType, cs.resultType);

        if (resultType == null || resultType.clazz().equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (resultType.clazz().equals(byte[].class)) {
            return (T) body;
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(new ByteArrayInputStream(body), resultType.clazz());
            } else if (bodyFormat == ContentFormat.FormUrlEncoded) {
                return URLEncodedUtil.decode(new String(body, respCharset), resultType.clazz());
            } else if (bodyFormat.name().contains("JSON")) {
                return N.fromJson(new String(body, respCharset), resultType);
            } else if (bodyFormat.name().contains("XML")) {
                return N.fromXml(new String(body, respCharset), resultType);
            } else {
                return HttpUtil.getParser(bodyFormat).deserialize(new String(body, respCharset), resultType.clazz());
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((requestUrl == null) ? 0 : requestUrl.hashCode());
        result = prime * result + statusCode;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result + bodyFormat.hashCode();
        return prime * result + ((body == null) ? 0 : N.hashCode(body));
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HttpResponse other) {
            return N.equals(requestUrl, other.requestUrl) && statusCode == other.statusCode && N.equals(message, other.message)
                    && N.equals(headers, other.headers) && N.equals(bodyFormat, other.bodyFormat) && N.equals(body, other.body);
        }

        return false;
    }

    @Override
    public String toString() {
        return "HttpResponse{statusCode=" + statusCode + ", message=" + message + ", url=" + requestUrl + ", elapsedTime="
                + (responseReceivedAtMillis - requestSentAtMillis) + '}';
    }
}
