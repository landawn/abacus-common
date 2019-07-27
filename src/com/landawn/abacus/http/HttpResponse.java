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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
public class HttpResponse {
    private final long sentRequestAtMillis;
    private final long receivedResponseAtMillis;
    private final int code;
    private final String message;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final ContentFormat bodyFormat;

    HttpResponse(long sentRequestAtMillis, long receivedResponseAtMillis, int code, String message, Map<String, List<String>> headers, byte[] body,
            ContentFormat bodyFormat) {
        this.sentRequestAtMillis = sentRequestAtMillis;
        this.receivedResponseAtMillis = receivedResponseAtMillis;
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.body = body;
        this.bodyFormat = bodyFormat;
    }

    /**
     * Returns true if the code is in [200..300), which means the request was successfully received,
     * understood, and accepted.
     */
    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public long sentRequestAtMillis() {
        return sentRequestAtMillis;
    }

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

    public <T> T body(Class<T> resultClass) {
        N.checkArgNotNull(resultClass, "resultClass");

        if (byte[].class.equals(resultClass)) {
            return (T) body;
        }

        final Type<Object> type = N.typeOf(resultClass);
        final Charset charset = HTTP.getCharset(headers);
        final String str = new String(body, charset);

        if (resultClass.equals(String.class)) {
            return (T) str;
        } else if (type.isSerializable()) {
            return (T) type.valueOf(str);
        } else {
            return HTTP.getParser(bodyFormat).deserialize(resultClass, str);
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
        result = prime * result + ((body == null) ? 0 : N.hashCode(body));
        return result;
    }

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

    @Override
    public String toString() {
        return "{sentRequestAtMillis=" + sentRequestAtMillis + ", receivedResponseAtMillis=" + receivedResponseAtMillis + ", code=" + code + ", message="
                + message + ", headers=" + headers + ", bodyFormat=" + bodyFormat + ", body=" + body(String.class) + "}";
    }
}
