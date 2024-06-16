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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.w3c.dom.Node;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XMLUtil;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
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
    public String serialize(Object obj, XMLSerializationConfig config) {
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
    public void serialize(Object obj, XMLSerializationConfig config, File output) {
        OutputStream os = null;

        try {
            createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            serialize(obj, config, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(Object obj, XMLSerializationConfig config, OutputStream output) {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (IOException e) {
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
    public void serialize(Object obj, XMLSerializationConfig config, Writer output) {
        boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);

        try {
            write(obj, config, bw);

            bw.flush();
        } catch (IOException e) {
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
    protected void write(Object obj, XMLSerializationConfig config, Writer output) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        if (obj == null) {
            try {
                IOUtil.write(Strings.EMPTY_STRING, output);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Marshaller marshaller = XMLUtil.createMarshaller(obj.getClass());

        try {
            marshaller.marshal(obj, output);

            output.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new ParseException(e);
        }
    }

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(String source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
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
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(File source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return deserialize(is, config, targetClass);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(InputStream source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(Reader source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
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
     * @param source
     * @param config
     * @param targetClass
     * @param <T>
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(Node source, XMLDeserializationConfig config, Class<? extends T> targetClass) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param config
     * @param nodeClasses
     * @param source
     * @param <T>
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(InputStream source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param config
     * @param nodeClasses
     * @param source
     * @param <T>
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(Reader source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param source
     * @param config
     * @param nodeClasses
     * @param <T>
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public <T> T deserialize(Node source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses) throws UnsupportedOperationException {
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
    protected <T> T read(Reader source, XMLDeserializationConfig config, Class<? extends T> targetClass) {
        if (config != null && N.notEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        Unmarshaller unmarshaller = XMLUtil.createUnmarshaller(targetClass);

        try {
            return (T) unmarshaller.unmarshal(source);
        } catch (JAXBException e) {
            throw new ParseException(e);
        }
    }
}
