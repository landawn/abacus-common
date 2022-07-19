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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 *
 * @author Haiyang Li
 * @since 1.3
 */
public class HttpResponse {

    private final long sentRequestAtMillis;

    private final long receivedResponseAtMillis;

    private final int code;

    private final String message;

    private final Map<String, List<String>> headers;

    private final byte[] body;

    private final ContentFormat bodyFormat;

    private final Charset respCharset;

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
        return HttpUtil.isSuccessfulResponseCode(code);
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

    public int code() {
        return code;
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
     */
    public <T> T body(Class<T> resultClass) {
        N.checkArgNotNull(resultClass, "resultClass");

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
     * @param <T>
     * @param resultType
     * @return
     */
    public <T> T body(Type<T> resultType) {
        N.checkArgNotNull(resultType, "resultType");

        if (resultType == null || resultType.clazz().equals(String.class)) {
            return (T) new String(body, respCharset);
        } else if (resultType.clazz().equals(byte[].class)) {
            return (T) body;
        } else {
            if (bodyFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                return HttpUtil.kryoParser.deserialize(resultType.clazz(), new ByteArrayInputStream(body));
            } else if (bodyFormat == ContentFormat.FormUrlEncoded) {
                return URLEncodedUtil.decode(resultType.clazz(), new String(body, respCharset));
            } else if (bodyFormat != null && bodyFormat.name().contains("JSON")) {
                return N.fromJSON(resultType, new String(body, respCharset));
            } else if (bodyFormat != null && bodyFormat.name().contains("XML")) {
                return N.fromXML(resultType, new String(body, respCharset));
            } else {
                return HttpUtil.getParser(bodyFormat).deserialize(resultType.clazz(), new String(body, respCharset));
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result + ((bodyFormat == null) ? 0 : bodyFormat.hashCode());
        return prime * result + ((body == null) ? 0 : N.hashCode(body));
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof HttpResponse other) {
            return code == other.code && N.equals(message, other.message) && N.equals(headers, other.headers) && N.equals(bodyFormat, other.bodyFormat)
                    && N.equals(body, other.body);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{sentRequestAtMillis=" + sentRequestAtMillis + ", receivedResponseAtMillis=" + receivedResponseAtMillis + ", code=" + code + ", message="
                + message + ", headers=" + headers + ", bodyFormat=" + bodyFormat + ", body=" + body(String.class) + "}";
    }
}
