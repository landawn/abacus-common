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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.w3c.dom.Node;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

final class JAXBParser extends AbstractXMLParser {

    JAXBParser() {
    }

    JAXBParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        super(xsc, xdc);
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String serialize(final Object obj, final XMLSerializationConfig config) {
        if (obj == null) {
            return Strings.EMPTY_STRING;
        }

        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            write(obj, config, bw);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final File output) {
        Writer writer = null;

        try {
            createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            serialize(obj, config, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final OutputStream output) {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final Writer output) {
        final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    protected void write(final Object obj, final XMLSerializationConfig config, final Writer output) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY_STRING, output);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Marshaller marshaller = XmlUtil.createMarshaller(obj.getClass());

        try {
            marshaller.marshal(obj, output);

            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final JAXBException e) {
            throw new ParseException(e);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        if (Strings.isEmpty(source)) {
            return N.defaultValueOf(targetClass);
        }

        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetClass);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        //
        // BufferedReader br = ObjectFactory.createBufferedReader(reader);
        //
        // try {
        // return read(cls, br, config);
        // } finally {
        // ObjectFactory.recycle(br);
        // }
        //
        return read(source, config, targetClass);
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(targetClass);

        try {
            return (T) unmarshaller.unmarshal(source);
        } catch (final JAXBException e) {
            throw new ParseException(e);
        }
    }
}
