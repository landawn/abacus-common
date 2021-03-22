/*
 * Copyright (C) 2020 HaiYang Li
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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.landawn.abacus.util.Enumerations;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

public class HttpServletRequestHeaderWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> headers = new HashMap<>();
    private final HttpServletRequest _httpServletRequest;

    public HttpServletRequestHeaderWrapper(HttpServletRequest request) {
        super(request);
        this._httpServletRequest = request;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setHeader(String name, Object value) {
        headers.put(name, N.stringOf(value));
    }

    @Override
    public String getHeader(String name) {
        final String value = headers.get(name);

        if (N.notNullOrEmpty(value)) {
            return value;
        } else {
            return _httpServletRequest.getHeader(name);
        }
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        final String value = headers.get(name);

        if (N.notNullOrEmpty(value)) {
            return Enumerations.just(value);
        } else {
            return _httpServletRequest.getHeaders(name);
        }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Enumerations.concat(Enumerations.of(headers.keySet()), _httpServletRequest.getHeaderNames());
    }

    @Override
    public long getDateHeader(String name) {
        final String value = headers.get(name);

        if (N.notNullOrEmpty(value)) {
            return Numbers.toLong(value);
        } else {
            return _httpServletRequest.getDateHeader(name);
        }
    }

    @Override
    public int getIntHeader(String name) {
        final String value = headers.get(name);

        if (N.notNullOrEmpty(value)) {
            return Numbers.toInt(value);
        } else {
            return _httpServletRequest.getIntHeader(name);
        }
    }
}