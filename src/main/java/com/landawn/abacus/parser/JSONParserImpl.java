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

import static com.landawn.abacus.parser.JSONReader.COLON;
import static com.landawn.abacus.parser.JSONReader.COMMA;
import static com.landawn.abacus.parser.JSONReader.END_BRACE;
import static com.landawn.abacus.parser.JSONReader.END_BRACKET;
import static com.landawn.abacus.parser.JSONReader.END_QUOTATION_D;
import static com.landawn.abacus.parser.JSONReader.END_QUOTATION_S;
import static com.landawn.abacus.parser.JSONReader.EOR;
import static com.landawn.abacus.parser.JSONReader.START_BRACE;
import static com.landawn.abacus.parser.JSONReader.START_BRACKET;
import static com.landawn.abacus.parser.JSONReader.START_QUOTATION_D;
import static com.landawn.abacus.parser.JSONReader.START_QUOTATION_S;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.ExceptionalStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.RowDataSet;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.Tuple.Tuple8;
import com.landawn.abacus.util.Tuple.Tuple9;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class JSONParserImpl extends AbstractJSONParser {

    private static final String ENTITY_NAME = "beanName";

    private static final String ENTITY_TYPE = "beanType";

    private static final String COLUMN_NAMES = "columnNames";

    private static final String COLUMN_TYPES = "columnTypes";

    private static final String PROPERTIES = "properties";

    private static final String IS_FROZEN = "isFrozen";

    private static final Map<String, Integer> dataSetPropOrder = new HashMap<>();

    static {
        dataSetPropOrder.put(ENTITY_NAME, 1);
        dataSetPropOrder.put(ENTITY_TYPE, 2);
        dataSetPropOrder.put(COLUMN_NAMES, 3);
        dataSetPropOrder.put(COLUMN_TYPES, 4);
        dataSetPropOrder.put(PROPERTIES, 5);
        dataSetPropOrder.put(IS_FROZEN, 6);
    }

    private static final JSONDeserializationConfig jdcForStringElement = JDC.create().setElementType(String.class);

    private static final JSONDeserializationConfig jdcForTypeElement = JDC.create().setElementType(Type.class);

    private static final JSONDeserializationConfig jdcForPropertiesElement = JDC.create().setElementType(String.class).setMapKeyType(String.class);

    JSONParserImpl() {
    }

    JSONParserImpl(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        super(jsc, jdc);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param config
     * @return
     */
    @Override
    public <T> T readString(final Class<? extends T> targetClass, final String str, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);
        final Type<T> type = N.typeOf(targetClass);

        if ((N.isNullOrEmpty(str) && configToUse.readNullToEmpty()) || (str != null && str.length() == 0)) {
            return emptyOrDefault(type);
        } else if (str == null) {
            return type.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, cbuf);

            return readString(null, targetClass, str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    @Override
    public void readString(Object[] outResult, String str, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);

        //    if (N.isNullOrEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (str == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, cbuf);

            readString(outResult, outResult.getClass(), str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    @Override
    public void readString(final Collection<?> outResult, final String str, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);

        //    if (N.isNullOrEmpty(str)) { // TODO ?
        //        return;
        //    }

        if (str == null) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, cbuf);

            readString(outResult, outResult.getClass(), str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     *
     * @param outResult
     * @param str
     * @param config
     */
    @Override
    public void readString(final Map<?, ?> outResult, final String str, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);

        if (N.isNullOrEmpty(str)) {
            return;
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, cbuf);

            readString(outResult, outResult.getClass(), str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     *
     * @param <T>
     * @param outResult
     * @param targetClass
     * @param str
     * @param jr
     * @param config
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    protected <T> T readString(final Object outResult, final Class<? extends T> targetClass, final String str, final JSONReader jr,
            final JSONDeserializationConfig config) throws IOException {
        final Type<T> type = (Type<T>) (outResult == null ? N.typeOf(targetClass) : N.typeOf(outResult.getClass()));
        final Object[] a = (outResult instanceof Object[]) ? (Object[]) outResult : null;
        final Collection<Object> c = (outResult instanceof Collection) ? (Collection<Object>) outResult : null;
        final Map<Object, Object> m = (outResult instanceof Map) ? (Map<Object, Object>) outResult : null;

        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                if (type.isArray()) {
                    return readArray(a, targetClass, jr, config, null, true);
                } else if (type.isCollection()) {
                    return readCollection(c, targetClass, jr, config, null, true);
                } else {
                    return (T) readNullToEmpty(type, type.valueOf(str), config.readNullToEmpty());
                }

            case ENTITY:
                return readBean(type, targetClass, jr, config, true);

            case MAP:
                return readMap(m, targetClass, jr, config, null, true);

            case ARRAY:
                return readArray(a, targetClass, jr, config, null, true);

            case COLLECTION:
                return readCollection(c, targetClass, jr, config, null, true);

            case DATA_SET:
                return readDataSet(targetClass, jr, config, true);

            case ENTITY_ID:
                return readEntityId(targetClass, jr, config, true);

            default:
                final int firstToken = jr.nextToken();

                if (Object.class.equals(targetClass)) {
                    if (firstToken == START_BRACE) {
                        return (T) readMap(null, Map.class, jr, config, null, false);
                    } else if (firstToken == START_BRACKET) {
                        return (T) readCollection(null, List.class, jr, config, null, false);
                    }
                }

                throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(type.clazz()) //NOSONAR
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported"); //NOSONAR
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String serialize(final Object obj, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = check(config);

        if (obj == null) {
            return null;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            return type.stringOf(obj);
        }

        final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
        final IdentityHashSet<Object> serializedObjects = configToUse.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, type, obj, configToUse, false, serializedObjects);

            return bw.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
    public void serialize(final File file, final Object obj, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(file, N.EMPTY_STRING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            try {
                IOUtil.write(file, type.stringOf(obj));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        OutputStream os = null;

        try {
            createNewFileIfNotExists(file);

            os = IOUtil.newFileOutputStream(file);

            serialize(os, obj, configToUse);

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
    public void serialize(final OutputStream os, final Object obj, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(os, N.EMPTY_STRING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            try {
                IOUtil.write(os, type.stringOf(obj), true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final BufferedJSONWriter bw = Objectory.createBufferedJSONWriter(os);
        final IdentityHashSet<Object> serializedObjects = configToUse.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, type, obj, configToUse, true, serializedObjects);
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
    public void serialize(final Writer writer, final Object obj, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = check(config);

        if (obj == null) {
            try {
                IOUtil.write(writer, N.EMPTY_STRING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        if (type.isSerializable() && !(type.isCollection() || type.isArray() || type.clazz().isEnum())) {
            try {
                IOUtil.write(writer, type.stringOf(obj), true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }

        boolean isBufferedWriter = writer instanceof BufferedJSONWriter;
        final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) writer : Objectory.createBufferedJSONWriter(writer);
        final IdentityHashSet<Object> serializedObjects = configToUse.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, type, obj, configToUse, true, serializedObjects);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param obj
     * @param config
     * @param flush
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void write(final BufferedJSONWriter bw, final Type<Object> type, final Object obj, final JSONSerializationConfig config, final boolean flush,
            final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (config.bracketRootValue()) {
            write(bw, type, obj, config, flush, true, null, serializedObjects);
        } else {
            if (type.isSerializable()) {
                if (type.isObjectArray()) {
                    writeArray(bw, type, obj, config, true, null, serializedObjects);
                } else if (type.isCollection()) {
                    writeCollection(bw, type, (Collection<?>) obj, config, true, null, serializedObjects);
                } else if (type.isPrimitiveArray()) {
                    writeArray(bw, type, obj, config, true, null, serializedObjects);
                } else {
                    write(bw, type, obj, config, flush, true, null, serializedObjects);
                }
            } else {
                write(bw, type, obj, config, flush, true, null, serializedObjects);
            }
        }
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param obj
     * @param config
     * @param flush
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void write(final BufferedJSONWriter bw, final Type<Object> type, final Object obj, final JSONSerializationConfig config, final boolean flush,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (obj == null) {
            return;
        }

        write(bw, obj, config, isFirstCall, indentation, serializedObjects);

        if (flush) {
            bw.flush();
        }
    }

    /**
     *
     * @param bw
     * @param obj
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void write(final BufferedJSONWriter bw, final Object obj, final JSONSerializationConfig config, final boolean isFirstCall,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        final Type<Object> type = N.typeOf(obj.getClass());

        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                type.writeCharacter(bw, obj, config);

                break;

            case ENTITY:
                writeBean(bw, type, obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case MAP:
                writeMap(bw, type, (Map<?, ?>) obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case ARRAY:
                writeArray(bw, type, obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case COLLECTION:
                writeCollection(bw, type, (Collection<?>) obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case MAP_ENTITY:
                writeMapEntity(bw, type, (MapEntity) obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case ENTITY_ID:
                writeEntityId(bw, type, (EntityId) obj, config, isFirstCall, indentation, serializedObjects);

                break;

            case DATA_SET:
                writeDataSet(bw, type, (DataSet) obj, config, isFirstCall, indentation, serializedObjects);

                break;

            default:
                throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(type.clazz())
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
        }
    }

    /**
     *
     * @param bw
     * @param type
     * @param obj
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeBean(final BufferedJSONWriter bw, final Type<Object> type, final Object obj, final JSONSerializationConfig config, boolean isFirstCall,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, obj, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        if (N.isNullOrEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParseException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = config.getExclusion() == Exclusion.DEFAULT;
        final boolean writeNullToEmpty = config.writeNullToEmpty();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final PropInfo[] propInfoList = config.skipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        String nextIndentation = isPrettyFormat ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        if (config.wrapRootValue()) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }

            if (config.quotePropName()) {
                bw.write(_D_QUOTATION);
                bw.write(ClassUtil.getSimpleClassName(cls));
                bw.write(_D_QUOTATION);
            } else {
                bw.write(ClassUtil.getSimpleClassName(cls));
            }

            bw.write(_COLON);
            bw.write(_BRACE_L);

            nextIndentation += config.getIndentation();
        }

        for (int k = 0, i = 0, len = propInfoList.length; i < len; i++) {
            propInfo = propInfoList[i];
            propName = propInfo.name;

            if (propInfo.jsonXmlExpose == JsonXmlField.Expose.DESERIALIZE_ONLY
                    || ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(propName))) {
                continue;
            }

            propValue = propInfo.getPropValue(obj);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitiveType() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
                continue;
            }

            if (k++ > 0) {
                if (isPrettyFormat) {
                    bw.write(',');
                } else {
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(nextIndentation);
            }

            if (propValue == null) {
                if (writeNullToEmpty) {
                    if (quotePropName) {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameWithColon);
                    } else {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].nameWithColon);
                    }

                    writeNullToEmpy(bw, propInfo.type);

                } else {
                    if (quotePropName) {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameNull);
                    } else {
                        bw.write(propInfo.jsonNameTags[nameTagIdx].nameNull);
                    }
                }
            } else {
                if (quotePropName) {
                    bw.write(propInfo.jsonNameTags[nameTagIdx].quotedNameWithColon);
                } else {
                    bw.write(propInfo.jsonNameTags[nameTagIdx].nameWithColon);
                }

                if (propInfo.jsonXmlType.isSerializable()) {
                    propInfo.writePropValue(bw, propValue, config);
                } else {
                    write(bw, propValue, config, false, nextIndentation, serializedObjects);
                }
            }
        }

        if (config.wrapRootValue()) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }

            bw.write(_BRACE_R);
        }

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    /**
     *
     * @param bw
     * @param type
     * @param m
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void writeMap(final BufferedJSONWriter bw, final Type<Object> type, final Map<?, ?> m, final JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, m, serializedObjects)) {
            return;
        }

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean isQuoteMapKey = config.quoteMapKey();
        final boolean isPrettyFormat = config.prettyFormat();

        Type<Object> keyType = null;
        int i = 0;

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        Object key = null;
        Object value = null;

        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            if (key != null && (ignoredClassPropNames != null) && ignoredClassPropNames.contains(key.toString())) {
                continue;
            }

            value = entry.getValue();

            // ignoreNullProperty only for
            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (i++ > 0) {
                if (isPrettyFormat) {
                    bw.write(',');
                } else {
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(nextIndentation);
            }

            if (key == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                keyType = N.typeOf(key.getClass());

                if (keyType.isSerializable() && !(keyType.isArray() || keyType.isCollection() || keyType.clazz().isEnum())) {
                    if (isQuoteMapKey || !(keyType.isNumber() || keyType.isBoolean())) {
                        bw.write(_D_QUOTATION);
                        bw.writeCharacter(keyType.stringOf(key));
                        bw.write(_D_QUOTATION);
                    } else {
                        bw.writeCharacter(keyType.stringOf(key));
                    }
                } else {
                    write(bw, key, config, false, nextIndentation, serializedObjects);
                }
            }

            bw.write(_COLON);

            if (value == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(bw, value, config, false, nextIndentation, serializedObjects);
            }
        }

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    /**
     *
     * @param bw
     * @param type
     * @param obj
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeArray(final BufferedJSONWriter bw, final Type<Object> type, final Object obj, final JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, obj, serializedObjects)) {
            return;
        }

        final boolean isPrimitiveArray = type.isPrimitiveArray();
        final boolean isPrettyFormat = config.prettyFormat();

        // TODO what to do if it's primitive array(e.g: int[]...)
        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACKET_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        final Object[] a = isPrimitiveArray ? null : (Object[]) obj;
        final int len = isPrimitiveArray ? Array.getLength(obj) : a.length;
        Object val = null;
        for (int i = 0; i < len; i++) {
            val = isPrimitiveArray ? Array.get(obj, i) : a[i];

            if (i > 0) {
                if (isPrettyFormat) {
                    bw.write(',');
                } else {
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(nextIndentation);
            }

            if (val == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(bw, val, config, false, nextIndentation, serializedObjects);
            }
        }

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACKET_R);
        }
    }

    /**
     *
     * @param bw
     * @param type
     * @param c
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void writeCollection(final BufferedJSONWriter bw, final Type<Object> type, final Collection<?> c, final JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, c, serializedObjects)) {
            return;
        }

        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACKET_L);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        int i = 0;

        for (Object e : c) {
            if (i++ > 0) {
                if (isPrettyFormat) {
                    bw.write(',');
                } else {
                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(nextIndentation);
            }

            if (e == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                write(bw, e, config, false, nextIndentation, serializedObjects);
            }
        }

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACKET_R);
        }
    }

    /**
     * Write map bean.
     *
     * @param bw
     * @param type
     * @param mapEntity
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void writeMapEntity(final BufferedJSONWriter bw, final Type<Object> type, final MapEntity mapEntity, final JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, mapEntity, serializedObjects)) {
            return;
        }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_D_QUOTATION);
            bw.write(mapEntity.entityName());
            bw.write(_D_QUOTATION);
        } else {
            bw.write(mapEntity.entityName());
        }

        bw.write(_COLON);

        bw.write(_BRACE_L);

        if (!mapEntity.isEmpty()) {
            final String nextIndentation = isPrettyFormat
                    ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation() + config.getIndentation())
                    : null;
            int i = 0;

            for (String propName : mapEntity.keySet()) {
                if (i++ > 0) {
                    if (isPrettyFormat) {
                        bw.write(',');
                    } else {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                    bw.write(nextIndentation);
                }

                if (quotePropName) {
                    bw.write(_D_QUOTATION);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_D_QUOTATION);
                } else {
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                }

                bw.write(_COLON);

                write(bw, mapEntity.get(propName), config, false, nextIndentation, serializedObjects);
            }
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    /**
     * Write bean id.
     *
     * @param bw
     * @param type
     * @param entityId
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void writeEntityId(final BufferedJSONWriter bw, final Type<Object> type, final EntityId entityId, JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, entityId, serializedObjects)) {
            return;
        }

        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_D_QUOTATION);
            bw.write(entityId.entityName());
            bw.write(_D_QUOTATION);
        } else {
            bw.write(entityId.entityName());
        }

        bw.write(_COLON);

        bw.write(_BRACE_L);

        if (entityId.size() > 0) {
            final String nextIndentation = isPrettyFormat
                    ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation() + config.getIndentation())
                    : null;
            int i = 0;

            for (String propName : entityId.keySet()) {
                if (i++ > 0) {
                    if (isPrettyFormat) {
                        bw.write(',');
                    } else {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                    bw.write(nextIndentation);
                }

                if (quotePropName) {
                    bw.write(_D_QUOTATION);
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                    bw.write(_D_QUOTATION);
                } else {
                    bw.write(jsonXmlNamingPolicy == null ? propName : jsonXmlNamingPolicy.convert(propName));
                }

                bw.write(_COLON);

                write(bw, entityId.get(propName), config, false, nextIndentation, serializedObjects);
            }
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        bw.write(_BRACE_R);

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    /**
     * Write data set.
     *
     * @param bw
     * @param type
     * @param ds
     * @param config
     * @param isFirstCall
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected void writeDataSet(final BufferedJSONWriter bw, final Type<Object> type, final DataSet ds, final JSONSerializationConfig config,
            final boolean isFirstCall, final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, ds, serializedObjects)) {
            return;
        }

        final boolean quotePropName = config.quotePropName();
        final boolean isPrettyFormat = config.prettyFormat();

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? N.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        if (config.bracketRootValue() || !isFirstCall) {
            bw.write(_BRACE_L);
        }

        //        {
        //            if (isPrettyFormat) {
        //                bw.write(IOUtil.LINE_SEPARATOR);
        //
        //                if (indentation != null) {
        //                    bw.write(indentation);
        //                }
        //
        //                bw.write(config.getIndentation());
        //            }
        //        }
        //
        //        if (quotePropName) {
        //            bw.write(_D_QUOTATION);
        //            bw.write(ENTITY_NAME);
        //            bw.write(_D_QUOTATION);
        //        } else {
        //            bw.write(ENTITY_NAME);
        //        }
        //
        //        bw.write(_COLON);
        //
        //        strType.writeCharacter(bw, rs.beanName(), config);
        //
        //        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
        //
        //        {
        //            if (isPrettyFormat) {
        //                bw.write(N.LINE_SEPARATOR);
        //
        //                if (indentation != null) {
        //                    bw.write(indentation);
        //                }
        //
        //                bw.write(config.getIndentation());
        //            }
        //        }
        //
        //        if (quotePropName) {
        //            bw.write(_D_QUOTATION);
        //            bw.write(ENTITY_TYPE);
        //            bw.write(_D_QUOTATION);
        //        } else {
        //            bw.write(ENTITY_TYPE);
        //        }
        //
        //        bw.write(_COLON);
        //
        //        if (rs.beanClass() == null) {
        //            bw.write(NULL_CHAR_ARRAY);
        //        } else {
        //            strType.writeCharacter(bw, N.getCanonicalClassName(rs.beanClass()), config);
        //        }
        //
        //        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);

        final List<String> columnNames = ds.columnNameList();

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_D_QUOTATION);
            bw.write(COLUMN_NAMES);
            bw.write(_D_QUOTATION);
        } else {
            bw.write(COLUMN_NAMES);
        }

        bw.write(_COLON);

        write(bw, columnNames, config, false, nextIndentation, serializedObjects);

        if (isPrettyFormat) {
            bw.write(',');
        } else {
            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
        }

        {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }

                bw.write(config.getIndentation());
            }
        }

        if (quotePropName) {
            bw.write(_D_QUOTATION);
            bw.write(COLUMN_TYPES);
            bw.write(_D_QUOTATION);
        } else {
            bw.write(COLUMN_TYPES);
        }

        bw.write(_COLON);

        final List<String> types = Objectory.createList();
        String typeName = null;
        List<Object> column = null;
        for (int i = 0, len = columnNames.size(); i < len; i++) {
            typeName = null;
            column = ds.getColumn(i);

            for (Object value : column) {
                if (value != null) {
                    typeName = N.typeOf(value.getClass()).name();
                    break;
                }
            }

            types.add(typeName);
        }

        write(bw, types, config, false, nextIndentation, serializedObjects);

        Objectory.recycle(types);

        if (N.notNullOrEmpty(ds.properties())) {
            if (isPrettyFormat) {
                bw.write(',');
            } else {
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            }

            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_D_QUOTATION);
                bw.write(PROPERTIES);
                bw.write(_D_QUOTATION);
            } else {
                bw.write(PROPERTIES);
            }

            bw.write(_COLON);

            write(bw, ds.properties(), config, false, nextIndentation, serializedObjects);
        }

        if (ds.isFrozen()) {
            if (isPrettyFormat) {
                bw.write(',');
            } else {
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            }

            {
                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);

                    if (indentation != null) {
                        bw.write(indentation);
                    }

                    bw.write(config.getIndentation());
                }
            }

            if (quotePropName) {
                bw.write(_D_QUOTATION);
                bw.write(IS_FROZEN);
                bw.write(_D_QUOTATION);
            } else {
                bw.write(IS_FROZEN);
            }

            bw.write(_COLON);

            bw.write(ds.isFrozen());
        }

        if (columnNames.size() > 0) {
            if (isPrettyFormat) {
                bw.write(',');
            } else {
                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
            }

            String columnName = null;
            for (int i = 0, len = columnNames.size(); i < len; i++) {
                columnName = columnNames.get(i);
                column = ds.getColumn(i);

                if (i > 0) {
                    if (isPrettyFormat) {
                        bw.write(',');
                    } else {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                    bw.write(nextIndentation);
                }

                if (quotePropName) {
                    bw.write(_D_QUOTATION);
                    bw.write(columnName);
                    bw.write(_D_QUOTATION);
                } else {
                    bw.write(columnName);
                }

                bw.write(_COLON);

                write(bw, column, config, false, nextIndentation, serializedObjects);
            }
        }

        if (config.bracketRootValue() || !isFirstCall) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);

                if (indentation != null) {
                    bw.write(indentation);
                }
            }

            bw.write(_BRACE_R);
        }
    }

    /**
     * Checks for circular reference.
     *
     * @param bw
     * @param obj
     * @param serializedObjects
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private boolean hasCircularReference(final BufferedJSONWriter bw, final Object obj, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (obj != null && serializedObjects != null) {
            if (serializedObjects.contains(obj)) {
                bw.write("null");
                return true;
            } else {
                serializedObjects.add(obj);
            }
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(final Class<? extends T> targetClass, final String str, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);

        final Type<T> type = N.typeOf(targetClass);

        if ((N.isNullOrEmpty(str) && configToUse.readNullToEmpty()) || (str != null && str.length() == 0)) {
            return emptyOrDefault(type);
        } else if (str == null) {
            return type.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, cbuf);

            return read(type, targetClass, str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param config
     * @return
     */
    @Override
    public <T> T deserialize(final Class<? extends T> targetClass, final String str, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));
        final JSONDeserializationConfig configToUse = check(config);
        final Type<T> type = N.typeOf(targetClass);

        if ((N.isNullOrEmpty(str) && configToUse.readNullToEmpty()) || (str != null && fromIndex == toIndex)) {
            return emptyOrDefault(type);
        } else if (str == null) {
            return type.defaultValue();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final JSONReader jr = JSONStringReader.parse(str, fromIndex, toIndex, cbuf);

            return read(type, targetClass, str, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
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
    public <T> T deserialize(final Class<? extends T> targetClass, final File file, final JSONDeserializationConfig config) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(file);

            return deserialize(targetClass, is, config);
        } finally {
            IOUtil.closeQuietly(is);
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
    public <T> T deserialize(final Class<? extends T> targetClass, final InputStream is, final JSONDeserializationConfig config) {
        // BufferedReader br = ObjectFactory.createBufferedReader(is);
        //
        // try {
        // return read(cls, br, config);
        // } finally {
        // ObjectFactory.recycle(br);
        // }
        //

        // No need to close the reader because the InputStream will/should be
        // closely externally.
        Reader reader = new InputStreamReader(is);

        return deserialize(targetClass, reader, config);
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
    public <T> T deserialize(final Class<? extends T> targetClass, final Reader reader, final JSONDeserializationConfig config) {
        // boolean isBufferedReader = reader instanceof BufferedReader;
        // BufferedReader br = isBufferedReader ? (BufferedReader) reader :
        // ObjectFactory.createBufferedReader(reader);
        //
        // try {
        // return read(cls, br, config);
        // } finally {
        // if (!isBufferedReader) {
        // ObjectFactory.recycle(br);
        // }
        // }

        return read(targetClass, reader, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param reader
     * @param config
     * @return
     */
    protected <T> T read(final Class<? extends T> targetClass, final Reader reader, final JSONDeserializationConfig config) {
        final JSONDeserializationConfig configToUse = check(config);
        final Type<T> type = N.typeOf(targetClass);
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            JSONReader jr = JSONStreamReader.parse(reader, rbuf, cbuf);

            return read(type, targetClass, reader, jr, configToUse);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(cbuf);
            Objectory.recycle(rbuf);
        }
    }

    protected <T> T read(final Type<? extends T> type, Class<? extends T> targetClass, Object source, final JSONReader jr,
            final JSONDeserializationConfig config) throws IOException {
        return read(type, targetClass, source, jr, config, true, 0);
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(final Type<? extends T> type, Class<? extends T> targetClass, final Object source, final JSONReader jr,
            final JSONDeserializationConfig config, boolean isFirstCall, final int firstToken) throws IOException {
        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                if (type.isArray()) {
                    return readArray(null, targetClass, jr, config, null, isFirstCall);
                } else if (type.isCollection()) {
                    return readCollection(null, targetClass, jr, config, null, isFirstCall);
                } else {
                    return (T) readNullToEmpty(type, type.valueOf(source instanceof String ? (String) source : IOUtil.readAllToString(((Reader) source))),
                            config.readNullToEmpty());
                }

            case ENTITY:
                return readBean(type, targetClass, jr, config, isFirstCall);

            case MAP:
                return readMap(null, targetClass, jr, config, null, isFirstCall);

            case ARRAY:
                return readArray(null, targetClass, jr, config, null, isFirstCall);

            case COLLECTION:
                return readCollection(null, targetClass, jr, config, null, isFirstCall);

            case MAP_ENTITY:
                return readMapEntity(targetClass, jr, config, isFirstCall);

            case DATA_SET:
                return readDataSet(targetClass, jr, config, isFirstCall);

            case ENTITY_ID:
                return readEntityId(targetClass, jr, config, isFirstCall);

            default:
                final int firstTokenToUse = isFirstCall ? jr.nextToken() : firstToken;

                if (Object.class.equals(targetClass)) {
                    if (firstTokenToUse == START_BRACE) {
                        return (T) readMap(null, Map.class, jr, config, null, false);
                    } else if (firstTokenToUse == START_BRACKET) {
                        return (T) readCollection(null, List.class, jr, config, null, false);
                    }
                }

                throw new ParseException(firstTokenToUse, "Unsupported class: " + ClassUtil.getCanonicalClassName(type.clazz())
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
        }
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param jr
     * @param config
     * @param propType
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected <T> T readBean(final Type<? extends T> type, final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config,
            final boolean isFirstCall) throws IOException {
        final boolean hasPropTypes = N.notNullOrEmpty(config.getPropTypes());
        final boolean ignoreUnmatchedProperty = config.ignoreUnmatchedProperty();
        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
        final Object result = beanInfo.createBeanResult();

        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;
        boolean isPropName = true;
        Type<Object> propType = null;

        int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOR) {
            if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText()); //NOSONAR
            }

            return null; // result;
        }

        // for (int token = firstToken == START_BRACE ? jr.nextToken() :
        // firstToken;; token = isPropName ? jr.nextNameToken() :
        // jr.nextToken()) { // TODO .Why it's even slower by jr.nextNameToken
        // which has less comparison. Fuck???!!!...
        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:

                    if (isPropName) {
                        // propName = jr.getText();
                        // propName = jr.readPropName(beanInfo);
                        // propInfo = beanInfo.getPropInfo(propName);

                        propInfo = jr.readPropInfo(beanInfo);

                        if (propInfo == null) {
                            propName = jr.getText();
                            propInfo = beanInfo.getPropInfo(propName);
                        } else {
                            propName = propInfo.name;
                        }

                        if (propInfo == null) {
                            propType = null;
                        } else {
                            propType = hasPropTypes ? config.getPropType(propName) : null;

                            if (propType == null) {
                                propType = propInfo.jsonXmlType;
                            }
                        }

                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            break;
                        }

                        if (propInfo == null) {
                            if (ignoreUnmatchedProperty) {
                                break;
                            } else {
                                throw new ParseException("Unknown property: " + propName);
                            }
                        }
                    } else {
                        if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                            // ignore.
                        } else {
                            propValue = readPropValue(propInfo.jsonXmlType, propInfo, jr, readNullToEmpty);
                            setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                        }
                    }

                    break;

                case COLON:

                    if (isPropName) {
                        isPropName = false;

                        if (jr.hasText()) {
                            propName = jr.getText();
                            propInfo = beanInfo.getPropInfo(propName);

                            if (propInfo == null) {
                                propType = null;
                            } else {
                                propType = hasPropTypes ? config.getPropType(propName) : null;

                                if (propType == null) {
                                    propType = propInfo.jsonXmlType;
                                }
                            }

                            if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                break;
                            }

                            if (propInfo == null) {
                                if (ignoreUnmatchedProperty) {
                                    break;
                                } else {
                                    throw new ParseException("Unknown property: " + propName);
                                }
                            }
                        }
                    } else {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:

                    if (isPropName) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        isPropName = true;

                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readPropValue(propInfo.jsonXmlType, propInfo, jr, readNullToEmpty);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    break;

                case START_BRACE:

                    if (isPropName) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        propValue = readMap(null, Map.class, jr, defaultJSONDeserializationConfig, null, false);
                    } else {
                        if (propInfo.isJsonRawValue) {
                            final StringBuilder sb = Objectory.createStringBuilder();
                            sb.append('{');

                            try {
                                int startBraceCount = 1;
                                int nextToken = 0;

                                while (startBraceCount > 0) {
                                    nextToken = jr.nextToken();

                                    if (nextToken == START_BRACE) {
                                        startBraceCount++;
                                    } else if (nextToken == END_BRACE) {
                                        startBraceCount--;
                                    }

                                    sb.append(jr.getText());

                                    if (nextToken == EOR) {
                                        break;
                                    } else if (nextToken == COMMA) {
                                        sb.append(AbstractJSONReader.eventChars[nextToken]);
                                        sb.append(' ');
                                    } else {
                                        sb.append(AbstractJSONReader.eventChars[nextToken]);
                                    }
                                }

                                propValue = sb.toString();
                            } finally {
                                Objectory.recycle(sb);
                            }
                        } else {
                            propValue = readBracedValue(propType, jr, config);
                        }

                        setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                    }

                    break;

                case START_BRACKET:

                    if (isPropName) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                        propValue = readCollection(null, List.class, jr, defaultJSONDeserializationConfig, null, false);
                    } else {

                        if (propInfo.isJsonRawValue) {
                            final StringBuilder sb = Objectory.createStringBuilder();
                            sb.append('[');

                            try {
                                int startBracketCount = 1;
                                int nextToken = 0;

                                while (startBracketCount > 0) {
                                    nextToken = jr.nextToken();

                                    if (nextToken == START_BRACKET) {
                                        startBracketCount++;
                                    } else if (nextToken == END_BRACKET) {
                                        startBracketCount--;
                                    }

                                    sb.append(jr.getText());

                                    if (nextToken == EOR) {
                                        break;
                                    } else if (nextToken == COMMA) {
                                        sb.append(AbstractJSONReader.eventChars[nextToken]);
                                        sb.append(' ');
                                    } else {
                                        sb.append(AbstractJSONReader.eventChars[nextToken]);
                                    }
                                }

                                propValue = sb.toString();
                            } finally {
                                Objectory.recycle(sb);
                            }
                        } else {
                            propValue = readBracketedValue(propType, jr, config);
                        }

                        setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                    }

                    break;

                case END_BRACE:
                case EOR:

                    if (isPropName && propInfo != null /*
                                                        * check for empty json text
                                                        * {}
                                                        */) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else if ((firstToken == START_BRACE && token != END_BRACE) || (firstToken != START_BRACE && token == END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\""); //NOSONAR
                    } else {
                        if (jr.hasText()) {
                            if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                    || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // ignore.
                            } else {
                                propValue = readPropValue(propInfo.jsonXmlType, propInfo, jr, readNullToEmpty);
                                setPropValue(propInfo, propValue, result, ignoreNullOrEmpty);
                            }
                        }
                    }

                    return beanInfo.finishBeanResult(result);
                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }

    }

    <T> void setPropValue(PropInfo propInfo, Object propValue, T result, boolean ignoreNullOrEmpty) {
        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(propInfo.jsonXmlType, propValue)) {
            propInfo.setPropValue(result, propValue);
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isNullOrEmptyValue(final Type<?> type, final Object value) {
        if (value == null) {
            return true;
        } else if (type.isCharSequence()) {
            return value instanceof CharSequence && ((CharSequence) value).length() == 0;
        } else if (type.isCollection()) {
            return value instanceof Collection && ((Collection) value).size() == 0;
        } else if (type.isArray()) {
            return value.getClass().isArray() && Array.getLength(value) == 0;
        } else if (type.isMap()) {
            return value instanceof Map && ((Map) value).size() == 0;
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param outResult
     * @param targetClass
     * @param jr
     * @param config
     * @param propType
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    protected <T> T readMap(Map<Object, Object> outResult, final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config,
            Type<?> propType, boolean isFirstCall) throws IOException {
        Type<?> keyType = defaultKeyType;

        if (propType != null && propType.isMap() && !propType.getParameterTypes()[0].isObjectType()) {
            keyType = propType.getParameterTypes()[0];
        } else if ((propType == null || !propType.isObjectType()) && (config.getMapKeyType() != null && !config.getMapKeyType().isObjectType())) {
            keyType = config.getMapKeyType();
        }

        boolean isStringKey = String.class == keyType.clazz();

        Type<?> valueType = defaultValueType;

        if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
            valueType = propType.getParameterTypes()[1];
        } else if ((propType == null || !propType.isObjectType()) && (config.getMapValueType() != null && !config.getMapValueType().isObjectType())) {
            valueType = config.getMapValueType();
        }

        final boolean hasPropTypes = N.notNullOrEmpty(config.getPropTypes());
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConvertor = getCreatorAndConvertorForTargetType(targetClass, null);

        @SuppressWarnings("rawtypes")
        final Map<Object, Object> result = outResult == null
                ? (Map.class.isAssignableFrom(targetClass) ? (Map<Object, Object>) creatorAndConvertor._1.apply(targetClass)
                        : N.newMap(Map.class.equals(targetClass) ? config.getMapInstanceType() : (Class<Map>) targetClass))
                : outResult;

        String propName = null;
        boolean isKey = true;
        propType = null;

        Object key = null;
        Object value = null;

        int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOR) {
            //    if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) result;

            return (T) creatorAndConvertor._2.apply(result);
        }

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:

                    if (isKey) {
                        key = readPropValue(keyType, jr, readNullToEmpty);
                        propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                        propType = hasPropTypes ? config.getPropType(propName) : null;

                        if (propType == null) {
                            propType = valueType;
                        }
                    } else {
                        if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                            // ignore.
                        } else {
                            value = readPropValue(propType, jr, readNullToEmpty);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case COLON:

                    if (isKey) {
                        isKey = false;

                        if (jr.hasText()) {
                            key = readPropValue(keyType, jr, readNullToEmpty);
                            propName = isStringKey ? (String) key : (key == null ? "null" : key.toString());
                            propType = hasPropTypes ? config.getPropType(propName) : null;
                        }

                        if (propType == null) {
                            propType = valueType;
                        }
                    } else {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:

                    if (isKey) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        isKey = true;

                        if (jr.hasText()) {
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                value = readPropValue(propType, jr, readNullToEmpty);

                                if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                    result.put(key, value);
                                }
                            }
                        }
                    }

                    break;

                case START_BRACE:

                    if (isKey) {
                        key = readBracedValue(keyType, jr, config);
                        propType = valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readMap(null, Map.class, jr, defaultJSONDeserializationConfig, null, false);
                        } else {
                            value = readBracedValue(propType, jr, config);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case START_BRACKET:

                    if (isKey) {
                        key = readBracketedValue(keyType, jr, config);
                        propType = valueType;
                    } else {
                        if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                            readCollection(null, List.class, jr, defaultJSONDeserializationConfig, null, false);
                        } else {
                            value = readBracketedValue(propType, jr, config);

                            if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                result.put(key, value);
                            }
                        }
                    }

                    break;

                case END_BRACE:
                case EOR:

                    if (isKey && key != null /* check for empty json text {} */) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else if ((firstToken == START_BRACE && token != END_BRACE) || (firstToken != START_BRACE && token == END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    } else {
                        if (jr.hasText()) {
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                value = readPropValue(propType, jr, readNullToEmpty);

                                if (!ignoreNullOrEmpty || (!isNullOrEmptyValue(keyType, key) && !isNullOrEmptyValue(propType, value))) {
                                    result.put(key, value);
                                }
                            }
                        }
                    }

                    return (T) creatorAndConvertor._2.apply(result);

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    /**
     *
     * @param <T>
     * @param a
     * @param targetClass
     * @param jr
     * @param config
     * @param propType
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    protected <T> T readArray(Object[] a, final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config, Type<?> propType,
            final boolean isFirstCall) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isArray() || propType.isCollection()) && !propType.getElementType().isObjectType()) {
            eleType = propType.getElementType();
        } else if (propType == null || !propType.isObjectType()) {
            if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                eleType = config.getElementType();
            } else {
                eleType = N
                        .typeOf(targetClass.isArray() && !Object.class.equals(targetClass.getComponentType()) ? targetClass.getComponentType() : Object.class);
            }
        }

        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();

        int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOR) {
            //    if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) (a == null ? N.newArray(targetClass.getComponentType(), 0) : a);

            final Object propValue = readPropValue(eleType, jr, readNullToEmpty);

            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                if (a == null || a.length == 0) {
                    a = N.newArray(targetClass.getComponentType(), 1);
                }

                a[0] = propValue;
            } else if (a == null) {
                a = N.newArray(targetClass.getComponentType(), 0);
            }

            return (T) a;
        }

        if (a == null) {
            final List<Object> c = Objectory.createList();
            Object propValue = null;

            try {
                for (int preToken = firstToken, token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken()) {
                    switch (token) {
                        case START_QUOTATION_D:
                        case START_QUOTATION_S:

                            break;

                        case END_QUOTATION_D:
                        case END_QUOTATION_S:

                            propValue = readPropValue(eleType, jr, readNullToEmpty);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                c.add(propValue);
                            }

                            break;

                        case COMMA:

                            if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && c.size() == 0)) {
                                propValue = readPropValue(eleType, jr, readNullToEmpty);

                                if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                    c.add(propValue);
                                }
                            }

                            break;

                        case START_BRACE:

                            propValue = readBracedValue(eleType, jr, config);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                c.add(propValue);
                            }

                            break;

                        case START_BRACKET:

                            propValue = readBracketedValue(eleType, jr, config);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                c.add(propValue);
                            }

                            break;

                        case END_BRACKET:
                        case EOR:

                            if ((firstToken == START_BRACKET && token != END_BRACKET) || (firstToken != START_BRACKET && token == END_BRACKET)) {
                                throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                            } else if (jr.hasText() || preToken == COMMA) {
                                propValue = readPropValue(eleType, jr, readNullToEmpty);

                                if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                    c.add(propValue);
                                }
                            }

                            return collection2Array(targetClass, c);

                        default:
                            throw new ParseException(token, getErrorMsg(jr, token));
                    }
                }
            } finally {
                Objectory.recycle(c);
            }
        } else {
            int idx = 0;
            Object propValue = null;

            for (int preToken = firstToken, token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken()) {
                switch (token) {
                    case START_QUOTATION_D:
                    case START_QUOTATION_S:

                        break;

                    case END_QUOTATION_D:
                    case END_QUOTATION_S:

                        propValue = readPropValue(eleType, jr, readNullToEmpty);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                            a[idx++] = propValue;
                        }

                        break;

                    case COMMA:

                        if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && idx == 0)) {
                            propValue = readPropValue(eleType, jr, readNullToEmpty);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                a[idx++] = propValue;
                            }
                        }

                        break;

                    case START_BRACE:

                        propValue = readBracedValue(eleType, jr, config);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                            a[idx++] = propValue;
                        }

                        break;

                    case START_BRACKET:

                        propValue = readBracketedValue(eleType, jr, config);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                            a[idx++] = propValue;
                        }

                        break;

                    case END_BRACKET:
                    case EOR:

                        if ((firstToken == START_BRACKET && token != END_BRACKET) || (firstToken != START_BRACKET && token == END_BRACKET)) {
                            throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                        } else if (jr.hasText() || preToken == COMMA) {
                            propValue = readPropValue(eleType, jr, readNullToEmpty);

                            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                                a[idx++] = propValue;
                            }
                        }

                        return (T) a;

                    default:
                        throw new ParseException(token, getErrorMsg(jr, token));
                }
            }
        }
    }

    /**
     *
     * @param <T>
     * @param outResult
     * @param targetClass
     * @param jr
     * @param config
     * @param propType
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    protected <T> T readCollection(final Collection<Object> outResult, final Class<? extends T> targetClass, final JSONReader jr,
            final JSONDeserializationConfig config, Type<?> propType, boolean isFirstCall) throws IOException {
        Type<?> eleType = defaultValueType;

        if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.getElementType().isObjectType()) {
            eleType = propType.getElementType();
        } else if ((propType == null || !propType.isObjectType()) && (config.getElementType() != null && !config.getElementType().isObjectType())) {
            eleType = config.getElementType();
        }

        final boolean ignoreNullOrEmpty = config.ignoreNullOrEmpty();
        final boolean readNullToEmpty = config.readNullToEmpty();

        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConvertor = getCreatorAndConvertorForTargetType(targetClass, null);

        final Collection<Object> result = outResult == null
                ? (Collection.class.isAssignableFrom(targetClass) ? (Collection<Object>) creatorAndConvertor._1.apply(targetClass) : new ArrayList<>())
                : outResult;

        Object propValue = null;

        int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOR) {
            //    if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
            //        throw new ParseException("Can't parse: " + jr.getText());
            //    }
            //
            //    return null; // (T) result;

            propValue = readPropValue(eleType, jr, readNullToEmpty);

            if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                result.add(propValue);
            }

            return (T) creatorAndConvertor._2.apply(result);
        }

        for (int preToken = firstToken, token = firstToken == START_BRACKET ? jr.nextToken() : firstToken;; preToken = token, token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:

                    propValue = readPropValue(eleType, jr, readNullToEmpty);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                        result.add(propValue);
                    }

                    break;

                case COMMA:

                    if (jr.hasText() || preToken == COMMA || (preToken == START_BRACKET && result.size() == 0)) {
                        propValue = readPropValue(eleType, jr, readNullToEmpty);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                            result.add(propValue);
                        }
                    }

                    break;

                case START_BRACE:

                    propValue = readBracedValue(eleType, jr, config);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                        result.add(propValue);
                    }

                    break;

                case START_BRACKET:

                    propValue = readBracketedValue(eleType, jr, config);

                    if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                        result.add(propValue);
                    }

                    break;

                case END_BRACKET:
                case EOR:

                    if ((firstToken == START_BRACKET && token != END_BRACKET) || (firstToken != START_BRACKET && token == END_BRACKET)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    } else if (jr.hasText() || preToken == COMMA) {
                        propValue = readPropValue(eleType, jr, readNullToEmpty);

                        if (!ignoreNullOrEmpty || !isNullOrEmptyValue(eleType, propValue)) {
                            result.add(propValue);
                        }
                    }

                    return (T) creatorAndConvertor._2.apply(result);

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    /**
     * Read map bean.
     *
     * @param <T>
     * @param targetClass
     * @param jr
     * @param config
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected <T> T readMapEntity(final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall)
            throws IOException {
        int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOR) {
            if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
            }

            return null;
        }

        MapEntity mapEntity = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:
                case COLON:

                    if (jr.hasText()) {
                        if (mapEntity == null) {
                            mapEntity = new MapEntity(jr.getText());
                        } else {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    } else {
                        if (mapEntity == null) {
                            throw new ParseException(token, "Bean name can't be null or empty");
                        }
                    }

                    break;

                case START_BRACE:
                    Map<String, Object> props = readMap(null, Map.class, jr, config, null, false);

                    mapEntity.set(props);

                    break;

                case END_BRACE:
                case EOR:

                    if ((firstToken == START_BRACE && token != END_BRACE) || (firstToken != START_BRACE && token == END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    }

                    return (T) mapEntity;

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    /**
     * Read bean id.
     *
     * @param <T>
     * @param targetClass
     * @param jr
     * @param config
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings({ "deprecation", "unused" })
    protected <T> T readEntityId(final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall)
            throws IOException {
        int firstToken = isFirstCall ? jr.nextToken() : START_BRACKET;

        if (firstToken == EOR) {
            if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
            }

            return null;
        }

        Seid entityId = null;

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:
                case COLON:

                    if (jr.hasText()) {
                        if (entityId == null) {
                            entityId = Seid.of(jr.getText());
                        } else {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    } else {
                        if (entityId == null) {
                            throw new ParseException(token, "Bean name can't be null or empty");
                        }
                    }

                    break;

                case START_BRACE:
                    Map<String, Object> props = readMap(null, Map.class, jr, config, null, false);

                    entityId.set(props);

                    break;

                case END_BRACE:
                case EOR:

                    if ((firstToken == START_BRACE && token != END_BRACE) || (firstToken != START_BRACE && token == END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    }

                    return (T) entityId;

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    /**
     * Read data set.
     *
     * @param <T>
     * @param targetClass
     * @param jr
     * @param config
     * @param isFirstCall
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unused")
    protected <T> T readDataSet(final Class<? extends T> targetClass, final JSONReader jr, final JSONDeserializationConfig config, final boolean isFirstCall)
            throws IOException {
        DataSet rs = null;

        //        String beanName = null;
        //        Class<?> beanClass = null;
        List<String> columnNameList = null;
        List<List<Object>> columnList = null;
        Properties<String, Object> properties = null;
        boolean isFrozen = false;

        List<Type<?>> columnTypeList = null;

        String propName = null;
        Type<?> propValueType = defaultValueType;
        boolean isKey = true;

        int firstToken = isFirstCall ? jr.nextToken() : START_BRACE;

        if (firstToken == EOR) {
            if (isFirstCall && N.notNullOrEmpty(jr.getText())) {
                throw new ParseException(firstToken, "Can't parse: " + jr.getText());
            }

            return null;
        }

        for (int token = firstToken == START_BRACE ? jr.nextToken() : firstToken;; token = jr.nextToken()) {
            switch (token) {
                case START_QUOTATION_D:
                case START_QUOTATION_S:

                    break;

                case END_QUOTATION_D:
                case END_QUOTATION_S:

                    if (isKey) {
                        propName = jr.getText();
                    } else {
                        Integer order = dataSetPropOrder.get(propName);
                        if (order == null) {
                            throw new ParseException(token, getErrorMsg(jr, token));
                        }

                        switch (order) { //NOSONAR
                            //    case 1:
                            //        beanName = jr.readValue(strType);
                            //        break;
                            //
                            //    case 2:
                            //        String str = jr.readValue(strType);
                            //        if (N.isNullOrEmpty(str)) {
                            //            beanClass = Map.class;
                            //        } else {
                            //            try {
                            //                beanClass = N.forClass(str);
                            //            } catch (Exception e) {
                            //                beanClass = Map.class;
                            //            }
                            //        }
                            //
                            //        break;

                            case 6:
                                isFrozen = jr.readValue(boolType);
                                break;

                            default:
                                throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    }

                    break;

                case COLON:
                    if (isKey) {
                        isKey = false;

                        if (jr.hasText()) {
                            propName = jr.getText();
                        }
                    } else {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    }

                    break;

                case COMMA:

                    if (isKey) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        isKey = true;

                        if (jr.hasText()) {
                            Integer order = dataSetPropOrder.get(propName);
                            if (order == null) {
                                throw new ParseException(token, getErrorMsg(jr, token));
                            }

                            switch (order) { //NOSONAR
                                //    case 1:
                                //        beanName = jr.readValue(strType);
                                //        break;
                                //
                                //    case 2:
                                //        String str = jr.readValue(strType);
                                //        if (N.isNullOrEmpty(str)) {
                                //            beanClass = Map.class;
                                //        } else {
                                //            try {
                                //                beanClass = N.forClass(str);
                                //            } catch (Exception e) {
                                //                beanClass = Map.class;
                                //            }
                                //        }
                                //
                                //        break;

                                case 6:
                                    isFrozen = jr.readValue(boolType);
                                    break;

                                default:
                                    throw new ParseException(token, getErrorMsg(jr, token));
                            }
                        }
                    }

                    break;

                case START_BRACKET:
                    Integer order = dataSetPropOrder.get(propName);

                    if (order == null || (columnNameList != null && columnNameList.contains(propName))) {
                        int index = N.indexOf(columnNameList, propName);

                        propValueType = columnTypeList.get(index);

                        if (propValueType == null) {
                            propValueType = defaultValueType;
                        }

                        List<Object> column = readCollection(null, List.class, jr, JDC.create().setElementType(propValueType.clazz()), null, false);
                        if (columnList == null) {
                            columnList = new ArrayList<>(columnNameList.size());
                            N.fill(columnList, 0, columnNameList.size(), null);
                        }

                        columnList.set(index, column);

                    } else {
                        switch (order) {
                            case 3:
                                columnNameList = readCollection(null, List.class, jr, jdcForStringElement, null, false);
                                break;

                            case 4:
                                columnTypeList = readCollection(null, List.class, jr, jdcForTypeElement, null, false);
                                break;

                            default:
                                throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    }

                    break;

                case START_BRACE:
                    if (!PROPERTIES.equals(propName)) {
                        throw new ParseException(token, getErrorMsg(jr, token) + ". Key: " + propName + ",  Value: " + jr.getText());
                    }

                    properties = Properties.from(readMap(null, Map.class, jr, jdcForPropertiesElement, null, false));

                    break;

                case END_BRACE:
                case EOR:

                    if ((firstToken == START_BRACE && token != END_BRACE) || (firstToken != START_BRACE && token == END_BRACE)) {
                        throw new ParseException(token, "The JSON text should be wrapped or unwrapped with \"[]\" or \"{}\"");
                    } else if (isKey && propName != null) {
                        throw new ParseException(token, getErrorMsg(jr, token));
                    } else {
                        if (jr.hasText()) {
                            // it should not happen.

                            // order = resultSetPropOrder.get(propName);
                            // if (order == null) {
                            // throw new ParseException("Unsupported event type: " +
                            // token + " with " + jr.getText());
                            // }
                            //
                            // switch (order) {
                            // case 1:
                            // beanName = jr.getText();
                            // break;
                            //
                            // case 2:
                            // String str = jr.getText();
                            // if (N.isNullOrEmpty(str)) {
                            // beanClass = Map.class;
                            // } else {
                            // try {
                            // beanClass = N.forClass(str);
                            // } catch (Exception e) {
                            // beanClass = Map.class;
                            // }
                            // }
                            //
                            // break;
                            //
                            // case 6:
                            // isFrozen = N.parseBoolean(jr.getText());
                            // break;
                            //
                            // default:
                            // throw new ParseException("Unsupported event type: " +
                            // token + " with " + jr.getText());
                            //
                            // }

                            throw new ParseException(token, getErrorMsg(jr, token));
                        }
                    }

                    // rs = new RowDataSet(beanName, beanClass, columnNameList, columnList, properties);
                    if (columnNameList == null) {
                        columnNameList = new ArrayList<>();
                    }

                    if (columnList == null) {
                        columnList = new ArrayList<>();
                    }

                    rs = new RowDataSet(columnNameList, columnList, properties);

                    if (isFrozen) {
                        rs.freeze();
                    }

                    return (T) rs;

                default:
                    throw new ParseException(token, getErrorMsg(jr, token));
            }
        }
    }

    //    static final int START_BRACE = 1;
    //    static final int END_BRACE = 2;
    //    static final int START_BRACKET = 3;
    //    static final int END_BRACKET = 4;
    //    static final int START_QUOTATION_D = 5;
    //    static final int END_QUOTATION_D = 6;
    //    static final int START_QUOTATION_S = 7;
    //    static final int END_QUOTATION_S = 8;
    //    static final int COLON = 9;
    //    static final int COMMA = 10;

    protected Object readBracketedValue(final Type<?> type, final JSONReader jr, JSONDeserializationConfig config) throws IOException {
        if (N.len(type.getParameterTypes()) == 1) {
            config = config.copy();
            config.setElementType(type.getParameterTypes()[0]);
        }

        if (type.isArray()) {
            return readArray(null, type.clazz(), jr, config, type, false);
        } else if (type.isCollection()) {
            return readCollection(null, type.clazz(), jr, config, type, false);
        } else {
            final List<?> list = readCollection(null, List.class, jr, config, type, false);
            final BiFunction<List<?>, Type<?>, Object> converter = list2PairTripleConverterMap.get(type.clazz());

            return converter == null ? list : converter.apply(list, type);
        }
    }

    protected Object readBracedValue(final Type<?> type, final JSONReader jr, JSONDeserializationConfig config) throws IOException {
        if (N.len(type.getParameterTypes()) == 2) {
            config = config.copy();
            config.setMapKeyType(type.getParameterTypes()[0]);
            config.setMapValueType(type.getParameterTypes()[1]);
        }

        if (type.isBean()) {
            return readBean((Type<Object>) type, (Class<Object>) type.clazz(), jr, config, false);
        } else if (type.isMap()) {
            return readMap(null, type.clazz(), jr, config, type, false);
        } else if (type.isDataSet()) {
            return readDataSet(DataSet.class, jr, config, false);
        } else if (type.isMapEntity()) {
            return readMapEntity(MapEntity.class, jr, config, false);
        } else if (type.isEntityId()) {
            return readEntityId(EntityId.class, jr, config, false);
        } else {
            final Map<Object, Object> map = readMap(null, Map.class, jr, config, type, false);
            final Function<Map<Object, Object>, ?> converter = map2TargetTypeConverterMap.get(type.clazz());

            if (converter == null) {
                if (AbstractMap.SimpleImmutableEntry.class.isAssignableFrom(type.clazz())) {
                    return map2TargetTypeConverterMap.get(AbstractMap.SimpleImmutableEntry.class).apply(map);
                } else if (Map.Entry.class.isAssignableFrom(type.clazz())) {
                    return map2TargetTypeConverterMap.get(Map.Entry.class).apply(map);
                } else {
                    return map;
                }
            } else {
                return converter.apply(map);
            }
        }
    }

    /**
     * Read prop value.
     *
     * @param propType
     * @param jr
     * @param nullToEmpty
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Object readPropValue(final Type<?> propType, final JSONReader jr, final boolean nullToEmpty) throws IOException {
        return readNullToEmpty(propType, jr.readValue(propType), nullToEmpty);
    }

    /**
     * Read prop value.
     *
     * @param propType
     * @param propInfo
     * @param jr
     * @param nullToEmpty
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Object readPropValue(final Type<?> propType, final PropInfo propInfo, final JSONReader jr, final boolean nullToEmpty) throws IOException {
        return readNullToEmpty(propType, propInfo != null && propInfo.hasFormat ? propInfo.readPropValue(jr.readValue(strType)) : jr.readValue(propType),
                nullToEmpty);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param json
     * @param config
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(final Class<? extends T> elementClass, final String json, final JSONDeserializationConfig config) {
        final Type<T> eleType = checkStreamSupportedType(elementClass);
        final JSONDeserializationConfig configToUse = check(config);

        if (N.isNullOrEmpty(json) || "[]".equals(json)) {
            return ExceptionalStream.<T, IOException> empty();
        }

        final char[] cbuf = Objectory.createCharArrayBuffer();
        ExceptionalStream<T, IOException> result = null;

        try {
            final JSONReader jr = JSONStringReader.parse(json, cbuf);

            result = stream(eleType, elementClass, json, jr, configToUse).onClose(() -> Objectory.recycle(cbuf));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (result == null) {
                Objectory.recycle(cbuf);
            }
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param file
     * @param config
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(final Class<? extends T> elementClass, final File file, final JSONDeserializationConfig config) {
        ExceptionalStream<T, IOException> result = null;
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(file);

            result = stream(elementClass, is, true, config);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(is);
            }
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param is
     * @param closeInputStreamWhenStreamIsClosed
     * @param config
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(final Class<? extends T> elementClass, final InputStream is,
            final boolean closeInputStreamWhenStreamIsClosed, final JSONDeserializationConfig config) {
        final Reader reader = new InputStreamReader(is);

        return stream(elementClass, reader, closeInputStreamWhenStreamIsClosed, config);
    }

    /**
     *
     *
     * @param <T>
     * @param elementClass
     * @param reader
     * @param closeReaderWhenStreamIsClosed
     * @param config
     * @return
     */
    @Override
    public <T> ExceptionalStream<T, IOException> stream(final Class<? extends T> elementClass, final Reader reader, final boolean closeReaderWhenStreamIsClosed,
            final JSONDeserializationConfig config) {
        N.checkArgNotNull(reader, "reader");
        ExceptionalStream<T, IOException> result = null;
        final char[] rbuf = Objectory.createCharArrayBuffer();
        final char[] cbuf = Objectory.createCharArrayBuffer();

        try {
            final Type<T> eleType = checkStreamSupportedType(elementClass);
            final JSONDeserializationConfig configToUse = check(config);

            final JSONReader jr = JSONStreamReader.parse(reader, rbuf, cbuf);

            result = stream(eleType, elementClass, reader, jr, configToUse).onClose(() -> {
                Objectory.recycle(rbuf);
                Objectory.recycle(cbuf);

                if (closeReaderWhenStreamIsClosed) {
                    IOUtil.closeQuietly(reader);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (result == null) {
                Objectory.recycle(rbuf);
                Objectory.recycle(cbuf);

                if (closeReaderWhenStreamIsClosed) {
                    IOUtil.closeQuietly(reader);
                }
            }
        }

        return result;
    }

    private <T> Type<T> checkStreamSupportedType(final Class<? extends T> elementClass) {
        final Type<T> eleType = N.typeOf(elementClass);

        switch (eleType.getSerializationType()) {
            case ENTITY:
            case MAP:
            case ARRAY:
            case COLLECTION:
            case MAP_ENTITY:
            case DATA_SET:
            case ENTITY_ID:
                break;

            default:
                if (!(eleType.isBean() || eleType.isMap() || eleType.isCollection() || eleType.isArray())) {
                    throw new IllegalArgumentException("Only Bean/Map/Collection/Array/DataSet element types are supported by stream methods at present");
                }
        }

        return eleType;
    }

    private <T> ExceptionalStream<T, IOException> stream(final Type<? extends T> eleType, final Class<? extends T> elementClass, final Object source,
            final JSONReader jr, final JSONDeserializationConfig configToUse) throws IOException {
        final int firstToken = jr.nextToken();

        if (firstToken == EOR) {
            return ExceptionalStream.<T, IOException> empty();
        } else if (firstToken != START_BRACKET) {
            throw new UnsupportedOperationException("Only Collection/Array JSON are supported by stream Methods");
        }

        final MutableBoolean hasNextFlag = MutableBoolean.of(false);
        final MutableInt tokenHolder = MutableInt.of(START_BRACKET);

        final Throwables.BooleanSupplier<IOException> hasNext = () -> {
            if (hasNextFlag.isTrue()) {
                return true;
            }

            if (tokenHolder.value() == START_BRACKET) {
                if (tokenHolder.setAndGet(jr.nextToken()) != END_BRACKET) {
                    hasNextFlag.setTrue();

                    return true;
                }
            } else {
                if (tokenHolder.setAndGet(jr.nextToken()) == COMMA) {
                    tokenHolder.setAndGet(jr.nextToken());
                }

                if (tokenHolder.value() != END_BRACKET) {
                    hasNextFlag.setTrue();

                    return true;
                }
            }

            return false;
        };

        final Throwables.Supplier<T, IOException> next = () -> {
            hasNextFlag.setFalse();

            if (tokenHolder.value() == COMMA) {
                return jr.readValue(eleType);
            } else {
                return read(eleType, elementClass, source, jr, configToUse, false, tokenHolder.value());
            }
        };

        return ExceptionalStream.<T, IOException> iterate(hasNext, next);
    }

    <T> T emptyOrDefault(final Type<? extends T> type) {
        if (type.isCollection() || type.isArray()) {
            return type.valueOf("[]");
        } else if (type.isMap()) {
            return type.valueOf("{}");
        } else if (type.isCharSequence()) {
            return type.valueOf("");
        } else {
            return type.defaultValue();
        }
    }

    private void writeNullToEmpy(final BufferedJSONWriter bw, final Type<?> type) throws IOException {
        if (type.isCollection() || type.isArray()) {
            bw.write("[]");
        } else if (type.isMap()) {
            bw.write("{}");
        } else if (type.isCharSequence()) {
            bw.write("");
        } else {
            bw.write(NULL_CHAR_ARRAY);
        }
    }

    Object readNullToEmpty(final Type<?> type, final Object value, final boolean readNullToEmpty) {
        if (value == null && readNullToEmpty) {
            if (type.isCollection() || type.isArray()) {
                return type.valueOf("[]");
            } else if (type.isMap()) {
                return type.valueOf("{}");
            } else if (type.isCharSequence()) {
                return type.valueOf("");
            }
        }

        return value;
    }

    private static final Map<Class<?>, Function<Map<Object, Object>, ?>> map2TargetTypeConverterMap = new HashMap<>();

    static {
        map2TargetTypeConverterMap.put(Map.Entry.class, t -> N.isNullOrEmpty(t) ? null : t.entrySet().iterator().next());

        map2TargetTypeConverterMap.put(AbstractMap.SimpleEntry.class,
                t -> N.isNullOrEmpty(t) ? null : new AbstractMap.SimpleEntry<>(t.entrySet().iterator().next()));

        map2TargetTypeConverterMap.put(AbstractMap.SimpleImmutableEntry.class,
                t -> N.isNullOrEmpty(t) ? null : new AbstractMap.SimpleImmutableEntry<>(t.entrySet().iterator().next()));

        map2TargetTypeConverterMap.put(ImmutableEntry.class, t -> N.isNullOrEmpty(t) ? null : ImmutableEntry.copyOf(t.entrySet().iterator().next()));
    }

    private static final Map<Class<?>, BiFunction<List<?>, Type<?>, Object>> list2PairTripleConverterMap = new HashMap<>();

    static {
        list2PairTripleConverterMap.put(Pair.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Pair.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Triple.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Triple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple1.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]));
        });

        list2PairTripleConverterMap.put(Tuple2.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]));
        });

        list2PairTripleConverterMap.put(Tuple3.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]));
        });

        list2PairTripleConverterMap.put(Tuple4.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]));
        });

        list2PairTripleConverterMap.put(Tuple5.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]));
        });

        list2PairTripleConverterMap.put(Tuple6.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]));
        });

        list2PairTripleConverterMap.put(Tuple7.class, (list, eleType) -> {
            final Type<?>[] paramTypes = eleType.getParameterTypes();
            return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                    N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                    N.convert(list.get(6), paramTypes[6]));
        });

        list2PairTripleConverterMap.put(Tuple8.class, new BiFunction<List<?>, Type<?>, Object>() {
            @SuppressWarnings("deprecation")
            @Override
            public Object apply(List<?> list, Type<?> eleType) {
                final Type<?>[] paramTypes = eleType.getParameterTypes();
                return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                        N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                        N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]));
            }
        });

        list2PairTripleConverterMap.put(Tuple9.class, new BiFunction<List<?>, Type<?>, Object>() {
            @SuppressWarnings("deprecation")
            @Override
            public Object apply(List<?> list, Type<?> eleType) {
                final Type<?>[] paramTypes = eleType.getParameterTypes();
                return Tuple.of(N.convert(list.get(0), paramTypes[0]), N.convert(list.get(1), paramTypes[1]), N.convert(list.get(2), paramTypes[2]),
                        N.convert(list.get(3), paramTypes[3]), N.convert(list.get(4), paramTypes[4]), N.convert(list.get(5), paramTypes[5]),
                        N.convert(list.get(6), paramTypes[6]), N.convert(list.get(7), paramTypes[7]), N.convert(list.get(8), paramTypes[8]));
            }
        });
    }

    //    static final int START_BRACE = 1;
    //    static final int END_BRACE = 2;
    //    static final int START_BRACKET = 3;
    //    static final int END_BRACKET = 4;
    //    static final int START_QUOTATION_D = 5;
    //    static final int END_QUOTATION_D = 6;
    //    static final int START_QUOTATION_S = 7;
    //    static final int END_QUOTATION_S = 8;
    //    static final int COLON = 9;
    //    static final int COMMA = 10;

    /**
     * Gets the error msg.
     *
     * @param jr
     * @param token
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String getErrorMsg(JSONReader jr, int token) throws IOException {
        switch (token) {
            case START_BRACE:
                return "Error on parsing at '{' with " + jr.getText();

            case END_BRACE:
                return "Error on parsing at '}' with " + jr.getText();

            case START_BRACKET:
                return "Error on parsing at '[' with " + jr.getText();

            case END_BRACKET:
                return "Error on parsing at ']' with " + jr.getText();

            case START_QUOTATION_D:
                return "Error on parsing at starting '\"' with " + jr.getText();

            case END_QUOTATION_D:
                return "Error on parsing at ending '\"' with " + jr.getText();

            case START_QUOTATION_S:
                return "Error on parsing at starting '\'' with " + jr.getText();

            case END_QUOTATION_S:
                return "Error on parsing at ending '\'' with " + jr.getText();

            case COLON:
                return "Error on parsing at ':' with " + jr.getText();

            case COMMA:
                return "Error on parsing at ',' with " + jr.getText();

            default:
                return "Unknown error on event : " + ((char) token) + " with " + jr.getText();
        }
    }
}
