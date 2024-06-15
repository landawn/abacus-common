/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.parser;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 *
 * @author Haiyang Li
 * @param <C>
 * @since 0.8
 */
abstract class AbstractObjectMapper<C extends MapperConfig> implements ObjectMapper<C> {

    static final int POOL_SIZE = AbstractParser.POOL_SIZE;

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public String write(Object obj) {
        return write(obj, (C) null);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void write(Object obj, File output) {
        write(obj, null, output);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void write(Object obj, OutputStream output) {
        write(obj, null, output);
    }

    /**
     *
     * @param obj
     * @param output
     */
    @Override
    public void write(Object obj, Writer output) {
        write(obj, null, output);
    }

    /**
     *
     * @param source
     * @param targetType
     * @param <T>
     * @return
     */
    @Override
    public <T> T read(String source, Class<? extends T> targetType) {
        return read(source, null, targetType);
    }

    /**
     *
     * @param source
     * @param cls
     * @param <T>
     * @return
     */
    @Override
    public <T> T read(File source, Class<? extends T> cls) {
        return read(source, null, cls);
    }

    /**
     *
     * @param source
     * @param cls
     * @param <T>
     * @return
     */
    @Override
    public <T> T read(InputStream source, Class<? extends T> cls) {
        return read(source, null, cls);
    }

    /**
     *
     * @param source
     * @param cls
     * @param <T>
     * @return
     */
    @Override
    public <T> T read(Reader source, Class<? extends T> cls) {
        return read(source, null, cls);
    }

}
