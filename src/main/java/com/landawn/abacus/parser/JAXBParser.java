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
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
            return N.EMPTY_STRING;
        }

        final BufferedWriter bw = Objectory.createBufferedWriter();

        try {
            write(bw, obj, config);

            return bw.toString();
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param file
     * @param obj
     * @param config
     */
    @Override
    public void serialize(File file, Object obj, XMLSerializationConfig config) {
        OutputStream os = null;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);

            serialize(os, obj, config);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param os
     * @param obj
     * @param config
     */
    @Override
    public void serialize(OutputStream os, Object obj, XMLSerializationConfig config) {
        final BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            write(bw, obj, config);

            bw.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param writer
     * @param obj
     * @param config
     */
    @Override
    public void serialize(Writer writer, Object obj, XMLSerializationConfig config) {
        boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
        final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

        try {
            write(bw, obj, config);

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
     * @param writer
     * @param obj
     * @param config
     */
    protected void write(Writer writer, Object obj, XMLSerializationConfig config) {
        if (config != null && N.notNullOrEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        if (obj == null) {
            try {
                IOUtil.write(writer, N.EMPTY_STRING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Marshaller marshaller = XMLUtil.createMarshaller(obj.getClass());

        try {
            marshaller.marshal(obj, writer);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new ParseException(e);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param st
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, String st, XMLDeserializationConfig config) {
        if (N.isNullOrEmpty(st)) {
            return N.defaultValueOf(targetClass);
        }

        final BufferedReader br = Objectory.createBufferedReader(st);

        try {
            return read(targetClass, br, config);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param file
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, File file, XMLDeserializationConfig config) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);

            return deserialize(targetClass, is, config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param is
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, InputStream is, XMLDeserializationConfig config) {
        final BufferedReader br = Objectory.createBufferedReader(is);

        try {
            return read(targetClass, br, config);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, Reader reader, XMLDeserializationConfig config) {
        //
        // BufferedReader br = ObjectFactory.createBufferedReader(reader);
        //
        // try {
        // return read(cls, br, config);
        // } finally {
        // ObjectFactory.recycle(br);
        // }
        //
        return read(targetClass, reader, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param node
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> targetClass, Node node, XMLDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param is
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, InputStream is, XMLDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param reader
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, Reader reader, XMLDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param node
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, Node node, XMLDeserializationConfig config) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(Class<T> targetClass, Reader reader, XMLDeserializationConfig config) {
        if (config != null && N.notNullOrEmpty(config.getIgnoredPropNames())) {
            throw new ParseException("'ignoredPropNames' is not supported");
        }

        Unmarshaller unmarshaller = XMLUtil.createUnmarshaller(targetClass);

        try {
            return (T) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            throw new ParseException(e);
        }
    }
}
