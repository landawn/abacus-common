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

package com.landawn.abacus.spring;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;
import org.springframework.lang.Nullable;

import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;

public class JSONHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    public JSONHttpMessageConverter() { //NOSONAR
    }

    @Override
    protected Object readInternal(final Type resolvedType, final Reader reader) {
        return N.fromJson(reader, TypeFactory.getType(resolvedType));
    }

    @Override
    protected void writeInternal(final Object obj, final @Nullable Type type, final Writer writer) {
        N.toJson(obj, writer);
    }
}
