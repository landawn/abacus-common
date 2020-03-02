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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * The Class HttpResponse.
 *
 * @author Haiyang Li
 * @since 1.3
 */
public class HttpResponse {

    /** The sent request at millis. */
    private final long sentRequestAtMillis;

    /** The received response at millis. */
    private final long receivedResponseAtMillis;

    /** The code. */
    private final int code;

    /** The message. */
    private final String message;

    /** The headers. */
    private final Map<String, List<String>> headers;

    /** The body. */
    private final byte[] body;

    /** The body format. */
    private final ContentFormat bodyFormat;

    private final Charset respCharset;

    /**
     * Instantiates a new http response.
     *
     * @param sentRequestAtMillis
     * @param receivedResponseAtMillis
     * @param code
     * @param message
     * @param headers
     * @param body
     * @param bodyFormat
     * @param respCharset
     */
    HttpResponse(final long sentRequestAtMillis, final long receivedResponseAtMillis, final int code, final String message,
            final Map<String, List<String>> headers, final byte[] body, final ContentFormat bodyFormat, final Charset respCharset) {
        this.sentRequestAtMillis = sentRequestAtMillis;
        this.receivedResponseAtMillis = receivedResponseAtMillis;
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.body = body;
        this.bodyFormat = bodyFormat == null ? ContentFormat.NONE : bodyFormat;
        this.respCharset = respCharset;
    }

    /**
     * Returns true if the code is in [200..300), which means the request was successfully received,
     * understood, and accepted.
     *
     * @return true, if is successful
     */
    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    /**
     * Sent request at millis.
     *
     * @return
     */
    public long sentRequestAtMillis() {
        return sentRequestAtMillis;
    }

    /**
     * Received response at millis.
     *
     * @return
     */
    public long receivedResponseAtMillis() {
        return receivedResponseAtMillis;
    }

    /**
     *
     * @return
     */
    public int code() {
        return code;
    }

    /**
     *
     * @return
     */
    public String message() {
        return message;
    }

    /**
     *
     * @return
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     *
     * @return
     */
    public byte[] body() {
        return body;
    }

    /**
     *
     * @param <T>
     * @param resultClass
     * @return
     */
    public <T> T body(Class<T> resultClass) {
        N.checkArgNotNull(resultClass, "resultClass");

        if (byte[].class.equals(resultClass)) {
            return (T) body;
        }

        if (resultClass == null || resultClass.equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (byte[].class.equals(resultClass)) {
            return (T) body;
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(resultClass, new ByteArrayInputStream(body));
            } else if (bodyFormat == ContentFormat.FormUrlEncoded) {
                return URLEncodedUtil.decode(resultClass, new String(body, respCharset));
            } else {
                return HttpUtil.getParser(bodyFormat).deserialize(resultClass, new String(body, respCharset));
            }
        }
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result + ((bodyFormat == null) ? 0 : bodyFormat.hashCode());
        result = prime * result + ((body == null) ? 0 : N.hashCode(body));
        return result;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HttpResponse) {
            final HttpResponse other = (HttpResponse) obj;

            return code == other.code && N.equals(message, other.message) && N.equals(headers, other.headers) && N.equals(bodyFormat, other.bodyFormat)
                    && N.equals(body, other.body);
        }

        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "{sentRequestAtMillis=" + sentRequestAtMillis + ", receivedResponseAtMillis=" + receivedResponseAtMillis + ", code=" + code + ", message="
                + message + ", headers=" + headers + ", bodyFormat=" + bodyFormat + ", body=" + body(String.class) + "}";
    }
}
