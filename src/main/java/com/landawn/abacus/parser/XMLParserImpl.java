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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.BufferedReader;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.XMLUtil;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class XMLParserImpl extends AbstractXMLParser {

    private final XMLParserType parserType;

    XMLParserImpl(final XMLParserType parserType) {
        this.parserType = parserType;
    }

    XMLParserImpl(final XMLParserType parserType, final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        super(xsc, xdc);
        this.parserType = parserType;
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

        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();
        final IdentityHashSet<Object> serializedObjects = config != null && config.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, obj, config, false, null, serializedObjects);

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
    public void serialize(final File file, final Object obj, final XMLSerializationConfig config) {
        OutputStream os = null;

        try {
            createNewFileIfNotExists(file);

            os = IOUtil.newFileOutputStream(file);

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
    public void serialize(final OutputStream os, final Object obj, final XMLSerializationConfig config) {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(os);
        final IdentityHashSet<Object> serializedObjects = config != null && config.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, obj, config, true, null, serializedObjects);
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
    public void serialize(final Writer writer, final Object obj, final XMLSerializationConfig config) {
        final boolean isBufferedWriter = writer instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) writer : Objectory.createBufferedXMLWriter(writer);
        final IdentityHashSet<Object> serializedObjects = config != null && config.supportCircularReference() ? new IdentityHashSet<>() : null;

        try {
            write(bw, obj, config, true, null, serializedObjects);
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
     * @param obj
     * @param config
     * @param flush
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void write(final BufferedXMLWriter bw, final Object obj, final XMLSerializationConfig config, final boolean flush, final String indentation,
            final IdentityHashSet<Object> serializedObjects) throws IOException {
        final XMLSerializationConfig configToUse = check(config);

        if (obj == null) {
            IOUtil.write(bw, Strings.EMPTY_STRING);
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                if (type.isObjectArray()) {
                    writeArray(bw, type, obj, configToUse, indentation, serializedObjects);
                } else if (type.isCollection()) {
                    writeCollection(bw, type, (Collection<?>) obj, configToUse, indentation, serializedObjects);
                } else {
                    type.writeCharacter(bw, obj, configToUse);
                }

                break;

            case ENTITY:
                writeBean(bw, type, obj, configToUse, indentation, serializedObjects);

                break;

            case MAP:
                writeMap(bw, type, (Map<?, ?>) obj, configToUse, indentation, serializedObjects);

                break;

            case MAP_ENTITY:
                writeMapEntity(bw, type, (MapEntity) obj, configToUse, indentation, serializedObjects);

                break;

            case ARRAY:
                writeArray(bw, type, obj, configToUse, indentation, serializedObjects);

                break;

            case COLLECTION:
                writeCollection(bw, type, (Collection<?>) obj, configToUse, indentation, serializedObjects);

                break;

            default:
                throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(cls)
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
        }

        if (flush) {
            bw.flush();
        }
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param obj
     * @param config
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeBean(final BufferedXMLWriter bw, final Type<Object> type, final Object obj, final XMLSerializationConfig config,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, obj, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParseException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (tagByPropertyName) {
            if (ignoreTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStart);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStartWithType);
            }
        } else {
            if (ignoreTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStart);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStartWithType);
            }
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        writeProperties(bw, type, obj, config, propIndentation, serializedObjects);

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (tagByPropertyName) {
            bw.write(beanInfo.xmlNameTags[nameTagIdx].namedEnd);
        } else {
            bw.write(beanInfo.xmlNameTags[nameTagIdx].epEnd);
        }
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param obj
     * @param config
     * @param propIndentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeProperties(final BufferedXMLWriter bw, final Type<Object> type, final Object obj, final XMLSerializationConfig config,
            final String propIndentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, obj, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        final Exclusion exlusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exlusion == Exclusion.NULL) || (exlusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exlusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final String nextIndentation = isPrettyFormat ? ((propIndentation == null ? Strings.EMPTY_STRING : propIndentation) + config.getIndentation()) : null;
        final PropInfo[] propInfoList = config.skipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        for (PropInfo element : propInfoList) {
            propInfo = element;
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

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(propIndentation);
            }

            if (propValue == null) {
                if (tagByPropertyName) {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNull);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNullWithType);
                    }
                } else {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNull);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNullWithType);
                    }
                }
            } else {
                if (tagByPropertyName) {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStart);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStartWithType);
                    }
                } else {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStart);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStartWithType);
                    }
                }

                if (propInfo.hasFormat) {
                    propInfo.writePropValue(bw, propValue, config);
                } else {
                    writeValue(bw, propInfo.jsonXmlType, propInfo, propValue, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects);
                }

                if (tagByPropertyName) {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].namedEnd);
                } else {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].epEnd);
                }
            }
        }
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param m
     * @param config
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeMap(final BufferedXMLWriter bw, final Type<Object> type, final Map<?, ?> m, final XMLSerializationConfig config,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, m, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (ignoreTypeInfo) {
            bw.write(XMLConstants.MAP_ELE_START);
        } else {
            bw.write(XMLConstants.START_MAP_ELE_WITH_TYPE);
            bw.write(N.typeOf(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final String nextIndentation = propIndentation + config.getIndentation();

        String strKey = null;
        Type<Object> valueType = null;
        Object key = null;
        Object value = null;

        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            strKey = key == null ? NULL_STRING : key.toString();
            value = entry.getValue();

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(propIndentation);
            }

            if (value == null) {
                bw.write(WD._LESS_THAN);
                bw.write(strKey);
                bw.write(XMLConstants.IS_NULL_ATTR);
                bw.write(XMLConstants.END_ELEMENT);
            } else {
                valueType = N.typeOf(value.getClass());

                bw.write(WD._LESS_THAN);
                bw.write(strKey);

                if (ignoreTypeInfo) {
                    bw.write(WD._GREATER_THAN);
                } else {
                    bw.write(XMLConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                }

                writeValue(bw, valueType, null, value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects);

                bw.write(WD._LESS_THAN);
                bw.write(WD._SLASH);
                bw.write(strKey);
                bw.write(WD._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.MAP_ELE_END);
    }

    /**
     * Write map bean.
     *
     * @param bw
     * @param type TODO
     * @param mapEntity
     * @param config
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeMapEntity(final BufferedXMLWriter bw, final Type<Object> type, final MapEntity mapEntity, final XMLSerializationConfig config,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, mapEntity, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        bw.write(WD._LESS_THAN);
        bw.write(mapEntity.entityName());

        if (ignoreTypeInfo) {
            bw.write(WD._GREATER_THAN);
        } else {
            bw.write(XMLConstants.START_TYPE_ATTR);
            bw.write(N.typeOf(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final String nextIndentation = propIndentation + config.getIndentation();

        Object value = null;
        Type<Object> valueType = null;

        for (String key : mapEntity.keySet()) {
            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            value = mapEntity.get(key);

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(propIndentation);
            }

            key = jsonXmlNamingPolicy == null ? key : jsonXmlNamingPolicy.convert(key);

            if (value == null) {
                bw.write(WD._LESS_THAN);
                bw.write(key);
                bw.write(XMLConstants.IS_NULL_ATTR);
                bw.write(XMLConstants.END_ELEMENT);
            } else {
                valueType = N.typeOf(value.getClass());

                bw.write(WD._LESS_THAN);
                bw.write(key);

                if (ignoreTypeInfo) {
                    bw.write(WD._GREATER_THAN);
                } else {
                    bw.write(XMLConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                }

                writeValue(bw, valueType, null, value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects);

                bw.write(WD._LESS_THAN);
                bw.write(WD._SLASH);
                bw.write(key);
                bw.write(WD._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(WD._LESS_THAN);
        bw.write(WD._SLASH);
        bw.write(mapEntity.entityName());
        bw.write(WD._GREATER_THAN);
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param obj
     * @param config
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeArray(final BufferedXMLWriter bw, final Type<Object> type, final Object obj, final XMLSerializationConfig config,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, obj, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (ignoreTypeInfo) {
            bw.write(XMLConstants.ARRAY_ELE_START);
        } else {
            bw.write(XMLConstants.START_ARRAY_ELE_WITH_TYPE);
            bw.write(N.typeOf(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final Object[] a = (Object[]) obj;
        final boolean isSerializableByJSON = isSerializableByJSON(a);

        if (isSerializableByJSON) {
            // jsonParser.serialize(bw, a);

            strType.writeCharacter(bw, jsonParser.serialize(a, getJSC(config)), config);
        } else {
            for (Object e : a) {
                if (e == null) {
                    bw.write(XMLConstants.NULL_NULL_ELE);
                } else {
                    write(bw, e, config, false, nextIndentation, serializedObjects);
                }
            }
        }

        if (isPrettyFormat && !isSerializableByJSON) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.ARRAY_ELE_END);
    }

    /**
     *
     * @param bw
     * @param type TODO
     * @param c
     * @param config
     * @param indentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeCollection(final BufferedXMLWriter bw, final Type<Object> type, final Collection<?> c, final XMLSerializationConfig config,
            final String indentation, final IdentityHashSet<Object> serializedObjects) throws IOException {
        if (hasCircularReference(bw, c, serializedObjects)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (type.isList()) {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.LIST_ELE_START);
            } else {
                bw.write(XMLConstants.START_LIST_ELE_WITH_TYPE);
                bw.write(N.typeOf(cls).xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        } else if (type.isSet()) {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.SET_ELE_START);
            } else {
                bw.write(XMLConstants.START_SET_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        } else {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.COLLECTION_ELE_START);
            } else {
                bw.write(XMLConstants.START_COLLECTION_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final boolean isSerializableByJSON = isSerializableByJSON(c);

        if (isSerializableByJSON) {
            // jsonParser.serialize(bw, c);

            strType.writeCharacter(bw, jsonParser.serialize(c, getJSC(config)), config);
        } else {
            for (Object e : c) {
                if (e == null) {
                    bw.write(XMLConstants.NULL_NULL_ELE);
                } else {
                    write(bw, e, config, false, nextIndentation, serializedObjects);
                }
            }
        }

        if (isPrettyFormat && !isSerializableByJSON) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (type.isList()) {
            bw.write(XMLConstants.LIST_ELE_END);
        } else if (type.isSet()) {
            bw.write(XMLConstants.SET_ELE_END);
        } else {
            bw.write(XMLConstants.COLLECTION_ELE_END);
        }
    }

    /**
     *
     * @param bw
     * @param valueType
     * @param propInfo
     * @param value
     * @param config
     * @param isPrettyFormat
     * @param propIndentation
     * @param nextIndentation
     * @param serializedObjects
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeValue(final BufferedXMLWriter bw, final Type<Object> valueType, final PropInfo propInfo, final Object value,
            final XMLSerializationConfig config, final boolean isPrettyFormat, final String propIndentation, final String nextIndentation,
            final IdentityHashSet<Object> serializedObjects) throws IOException {
        //    if (hasCircularReference(value, serializedObjects)) {
        //        return;
        //    }

        if (valueType.isSerializable()) {
            if (valueType.isObjectArray() || valueType.isCollection()) {
                // jsonParser.serialize(bw, value);

                strType.writeCharacter(bw, jsonParser.serialize(value, getJSC(config)), config);
            } else {
                if (propInfo != null && propInfo.hasFormat) {
                    propInfo.writePropValue(bw, value, config);
                } else {
                    valueType.writeCharacter(bw, value, config);
                }
            }
        } else if (valueType.isObjectArray()) {
            final Object[] a = (Object[]) value;
            boolean isSerializableByJSON = isSerializableByJSON(a);

            if (isSerializableByJSON) {
                // jsonParser.serialize(bw, a);

                strType.writeCharacter(bw, jsonParser.serialize(a, getJSC(config)), config);
            } else {
                for (Object e : a) {
                    if (e == null) {
                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR);
                            bw.write(nextIndentation);
                        }

                        bw.write(XMLConstants.NULL_NULL_ELE);
                    } else {
                        write(bw, e, config, false, nextIndentation, serializedObjects);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                    bw.write(propIndentation);
                }
            }

        } else if (valueType.isCollection()) {
            final Collection<?> c = (Collection<?>) value;
            final boolean isSerializableByJSON = isSerializableByJSON(c);

            if (isSerializableByJSON) {
                // jsonParser.serialize(bw, c);

                strType.writeCharacter(bw, jsonParser.serialize(c, getJSC(config)), config);
            } else {
                for (Object e : c) {
                    if (e == null) {
                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR);
                            bw.write(nextIndentation);
                        }

                        bw.write(XMLConstants.NULL_NULL_ELE);
                    } else {
                        write(bw, e, config, false, nextIndentation, serializedObjects);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR);
                    bw.write(propIndentation);
                }
            }

        } else {
            write(bw, value, config, false, nextIndentation, serializedObjects);

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(propIndentation);
            }
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
    private boolean hasCircularReference(final BufferedXMLWriter bw, final Object obj, final IdentityHashSet<Object> serializedObjects) throws IOException {
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
     * Checks if is serializable by JSON.
     *
     * @param a
     * @return true, if is serializable by JSON
     */
    protected boolean isSerializableByJSON(final Object[] a) {
        if (N.typeOf(a.getClass().getComponentType()).isSerializable()) {
            return true;
        } else {
            for (Object e : a) {
                if (e != null && N.typeOf(e.getClass()).isSerializable()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if is serializable by JSON.
     *
     * @param c
     * @return true, if is serializable by JSON
     */
    protected boolean isSerializableByJSON(final Collection<?> c) {
        for (Object e : c) {
            if (e != null && N.typeOf(e.getClass()).isSerializable()) {
                return true;
            }
        }
        return false;
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
    public <T> T deserialize(Class<? extends T> targetClass, String st, final XMLDeserializationConfig config) {
        if (Strings.isEmpty(st)) {
            return N.defaultValueOf(targetClass);
        }

        final BufferedReader br = Objectory.createBufferedReader(st);

        try {
            return read(null, targetClass, br, config);
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
    public <T> T deserialize(Class<? extends T> targetClass, File file, final XMLDeserializationConfig config) {
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
    public <T> T deserialize(Class<? extends T> targetClass, InputStream is, final XMLDeserializationConfig config) {
        final BufferedReader br = Objectory.createBufferedReader(is);

        try {
            return read(null, targetClass, br, config);
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
    public <T> T deserialize(Class<? extends T> targetClass, Reader reader, final XMLDeserializationConfig config) {
        // BufferedReader? will the target parser create the BufferedReader
        // internally.
        return read(null, targetClass, reader, config);
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
    public <T> T deserialize(Class<? extends T> targetClass, Node node, final XMLDeserializationConfig config) {
        return readByDOMParser(targetClass, node, config);
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
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, InputStream is, final XMLDeserializationConfig config) {
        final BufferedReader br = Objectory.createBufferedReader(is);

        try {
            return read(nodeClasses, null, br, config);
        } finally {
            Objectory.recycle(br);
        }
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
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, Reader reader, final XMLDeserializationConfig config) {
        return read(nodeClasses, null, reader, config);
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
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Map<String, Class<?>> nodeClasses, Node node, final XMLDeserializationConfig config) {
        Class<? extends T> targetClass = null;

        if (N.notEmpty(nodeClasses)) {
            String nodeName = XMLUtil.getAttribute(node, XMLConstants.NAME);

            if (Strings.isEmpty(nodeName)) {
                nodeName = node.getNodeName();
            }

            targetClass = (Class<T>) nodeClasses.get(nodeName);
        }

        if (targetClass == null) {
            throw new ParseException("No target class is specified"); //NOSONAR
        }

        return readByDOMParser(targetClass, node, config);
    }

    /**
     *
     * @param <T>
     * @param nodeClasses
     * @param targetClass
     * @param br
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(Map<String, Class<?>> nodeClasses, Class<? extends T> targetClass, Reader br, final XMLDeserializationConfig config) {
        final XMLDeserializationConfig configToUse = check(config);

        switch (parserType) {
            case StAX:
                try {
                    XMLStreamReader xmlReader = createXMLStreamReader(br);

                    for (int event = xmlReader.next(); event != XMLStreamConstants.START_ELEMENT && xmlReader.hasNext(); event = xmlReader.next()) {
                        // do nothing.
                    }

                    if (targetClass == null && N.notEmpty(nodeClasses)) {
                        String nodeName = null;

                        if (xmlReader.getAttributeCount() > 0) {
                            nodeName = xmlReader.getAttributeValue(null, XMLConstants.NAME);
                        }

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = xmlReader.getLocalName();
                        }

                        targetClass = (Class<T>) nodeClasses.get(nodeName);
                    }

                    if (targetClass == null) {
                        throw new ParseException("No target class is specified");
                    }

                    return readByStreamParser(targetClass, xmlReader, configToUse);
                } catch (XMLStreamException e) {
                    throw new ParseException(e);
                }

            case DOM: //NOSONAR
                DocumentBuilder docBuilder = XMLUtil.createContentParser();

                try {
                    Document doc = docBuilder.parse(new InputSource(br));
                    Node node = doc.getFirstChild();

                    if (targetClass == null && N.notEmpty(nodeClasses)) {
                        String nodeName = XMLUtil.getAttribute(node, XMLConstants.NAME);

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = node.getNodeName();
                        }

                        targetClass = (Class<T>) nodeClasses.get(nodeName);
                    }

                    if (targetClass == null) {
                        throw new ParseException("No target class is specified");
                    }

                    return readByDOMParser(targetClass, node, configToUse);
                } catch (SAXException e) {
                    throw new ParseException(e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    XMLUtil.recycleContentParser(docBuilder);
                }

            default:
                throw new ParseException("Unsupported parser: " + parserType);
        }
    }

    /**
     * Read by stream parser.
     *
     * @param <T>
     * @param targetClass
     * @param xmlReader
     * @param config
     * @return
     * @throws XMLStreamException the XML stream exception
     */
    protected <T> T readByStreamParser(Class<? extends T> targetClass, XMLStreamReader xmlReader, final XMLDeserializationConfig config)
            throws XMLStreamException {
        return readByStreamParser(targetClass, xmlReader, config, null, null);
    }

    /**
     * Read by stream parser.
     *
     * @param <T>
     * @param targetClass
     * @param xmlReader
     * @param config
     * @param propType
     * @param propInfo
     * @return
     * @throws XMLStreamException the XML stream exception
     */
    @SuppressWarnings({ "null", "fallthrough" })
    protected <T> T readByStreamParser(Class<?> targetClass, final XMLStreamReader xmlReader, final XMLDeserializationConfig config, Type<?> propType,
            PropInfo propInfo) throws XMLStreamException {

        if (targetClass.equals(Object.class)) {
            targetClass = MapEntity.class;
        }

        final XMLDeserializationConfig configToUse = check(config);
        boolean hasPropTypes = N.notEmpty(configToUse.getPropTypes());

        if (hasPropTypes && xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT && configToUse.hasPropType(xmlReader.getLocalName())) {
            targetClass = configToUse.getPropType(xmlReader.getLocalName()).clazz();
        }

        final SerializationType serializationType = getDeserializationType(targetClass);

        String propName = null;
        Object propValue = null;
        String text = null;
        StringBuilder sb = null;

        switch (serializationType) {
            case ENTITY: {
                final boolean ignoreUnmatchedProperty = configToUse.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(targetClass);
                BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
                final Object result = beanInfo.createBeanResult();
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propInfo = beanInfo.getPropInfo(propName);

                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    continue;
                                }

                                if (propInfo == null) {
                                    if (ignoreUnmatchedProperty) {
                                        continue;
                                    } else {
                                        throw new ParseException("Unknown property element: " + propName + " for class: " + targetClass.getCanonicalName());
                                    }
                                }

                                propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                                if (propType == null) {
                                    if (propInfo.jsonXmlType.isSerializable()) {
                                        propType = propInfo.jsonXmlType;
                                    } else {
                                        attrCount = xmlReader.getAttributeCount();

                                        if (attrCount == 1) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                                propType = N.typeOf(xmlReader.getAttributeValue(0));
                                            }
                                        } else if (attrCount > 1) {
                                            for (int i = 0; i < attrCount; i++) {
                                                if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                    propType = N.typeOf(xmlReader.getAttributeValue(i));

                                                    break;
                                                }
                                            }
                                        }

                                        if (propType == null) {
                                            propType = propInfo.jsonXmlType;
                                        }
                                    }
                                }

                            } else {
                                if (propInfo == null || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(propType.isObjectType() ? Map.class : propType.clazz(), xmlReader, configToUse, propType,
                                                propInfo);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? (Collection<Object>) N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(propEleType.clazz(), xmlReader, configToUse, propType, null));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.clazz().isArray() ? collection2Array(propType.clazz(), c) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
                                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                        // ignore.
                                    } else {
                                        propInfo.setPropValue(result, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName()); //NOSONAR
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            if (propInfo != null) {
                                text = xmlReader.getText();

                                if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                    while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.length() == 0) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    }

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                propValue = propInfo.hasFormat ? propInfo.readPropValue(text) : propType.valueOf(text);

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                        // ignore;
                                    } else {
                                        propInfo.setPropValue(result, propValue == null ? propType.defaultValue() : propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return beanInfo.finishBeanResult(result);
                            } else {
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                        || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    // ignore;
                                } else {
                                    propInfo.setPropValue(result, propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                                propInfo = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error"); //NOSONAR
            }

            case MAP: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[0].isObjectType()) {
                    keyType = propInfo.jsonXmlType.getParameterTypes()[0];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObjectType()) {
                        keyType = configToUse.getMapKeyType();
                    }
                }

                final boolean isStringKey = keyType.clazz() == String.class;
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                    valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                @SuppressWarnings("rawtypes")
                final Map<Object, Object> map = N.newMap((Class<Map>) targetClass);
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = N.typeOf(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = N.typeOf(xmlReader.getAttributeValue(i));

                                                break;
                                            }
                                        }
                                    }

                                    if (propType == null) {
                                        propType = valueType;
                                    }
                                }
                            } else {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(propType.isObjectType() ? Map.class : propType.clazz(), xmlReader, configToUse, propType,
                                                null);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? (Collection<Object>) N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(propEleType.clazz(), xmlReader, configToUse, propType, null));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.clazz().isArray() ? collection2Array(propType.clazz(), c) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
                                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        map.put(isStringKey ? propName : keyType.valueOf(propName), propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.length() == 0) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                }

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    map.put(isStringKey ? propName : keyType.valueOf(propName), propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return (T) map;
                            } else {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    map.put(isStringKey ? propName : keyType.valueOf(propName), propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            case MAP_ENTITY: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                    valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                final MapEntity mapEntity = N.newBean(MapEntity.class, xmlReader.getLocalName());
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = N.typeOf(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = N.typeOf(xmlReader.getAttributeValue(i));

                                                break;
                                            }
                                        }
                                    }

                                    if (propType == null) {
                                        propType = valueType;
                                    }
                                }
                            } else {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(propType.isObjectType() ? MapEntity.class : propType.clazz(), xmlReader, configToUse,
                                                propType, null);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? (Collection<Object>) N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(propEleType.clazz(), xmlReader, defaultXMLDeserializationConfig, propType, null));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.clazz().isArray() ? collection2Array(propType.clazz(), c) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
                                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        mapEntity.set(propName, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.length() == 0) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                }

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propName != null) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore;
                                    } else {
                                        mapEntity.set(propName, propValue == null ? propType.defaultValue() : propValue);
                                    }
                                } else {
                                    // should never happen.
                                    throw new RuntimeException("Unexpected error");
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return (T) mapEntity;
                            } else {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    mapEntity.set(propName, propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            case ARRAY: {
                Type<?> eleType = null;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = N.typeOf(propInfo.clazz.getComponentType());
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = N.typeOf(targetClass.isArray() ? targetClass.getComponentType() : String.class);
                    }
                }

                final List<Object> list = Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                    list.add(null);
                                } else if (String.class == eleType.clazz() || Object.class == eleType.clazz()) {
                                    list.add(readByStreamParser(Map.class, xmlReader, configToUse, eleType, null));
                                } else {
                                    list.add(readByStreamParser(eleType.clazz(), xmlReader, configToUse, eleType, null));
                                }

                                break;
                            }

                            // simple array with sample format <array>[1, 2,
                            // 3...]</array>
                            case XMLStreamConstants.CHARACTERS: {
                                text = xmlReader.getText();

                                if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                    while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.length() == 0) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    }

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                    propValue = N.typeOf(targetClass).valueOf(text);
                                } else {
                                    propValue = jsonParser.deserialize(targetClass, text, JDC.create().setElementType(eleType.clazz()));
                                }

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collection2Array(targetClass, list);
                                    }
                                }
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return collection2Array(targetClass, list);
                                }
                            }

                            default:
                                // continue;
                        }
                    }

                } finally {
                    Objectory.recycle(list);
                }

                throw new ParseException("Unknown parser error");
            }

            case COLLECTION: {
                Type<?> eleType = defaultValueType;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = N.typeOf(propInfo.clazz.getComponentType());
                } else if (propType != null && propType.getParameterTypes().length == 1 && Collection.class.isAssignableFrom(propType.clazz())
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    eleType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    }
                }

                @SuppressWarnings("rawtypes")
                Collection<Object> result = N.newCollection((Class<Collection>) targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                result.add(null);
                            } else if (String.class == eleType.clazz() || Object.class == eleType.clazz()) {
                                result.add(readByStreamParser(Map.class, xmlReader, configToUse, eleType, null));
                            } else {
                                result.add(readByStreamParser(eleType.clazz(), xmlReader, configToUse, eleType, null));
                            }

                            break;
                        }

                        // simple list with sample format <list>[1, 2, 3...]</list>
                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.length() == 0) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                }

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                propValue = N.typeOf(targetClass).valueOf(text);
                            } else {
                                propValue = jsonParser.deserialize(targetClass, text, JDC.create().setElementType(eleType.clazz()));
                            }

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return (T) result;
                                }
                            }
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propValue != null) {
                                return (T) propValue;
                            } else {
                                return (T) result;
                            }
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            default:
                throw new ParseException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only array, collection, map and bean types are supported");
        }
    }

    /**
     * Read by DOM parser.
     *
     * @param <T>
     * @param targetClass
     * @param node
     * @param config
     * @return
     */
    protected <T> T readByDOMParser(Class<? extends T> targetClass, Node node, final XMLDeserializationConfig config) {
        final XMLDeserializationConfig configToUse = check(config);

        return readByDOMParser(targetClass, node, configToUse, null, configToUse.getElementType(), false, false, false, true);
    }

    /**
     * Read by DOM parser.
     *
     * @param <T>
     * @param inputClass
     * @param node
     * @param configToUse
     * @param propName
     * @param propType
     * @param checkedAttr
     * @param isTagByPropertyName
     * @param ignoreTypeInfo
     * @param isFirstCall
     * @return
     */
    @SuppressWarnings({ "unchecked", "null" })
    protected <T> T readByDOMParser(Class<T> inputClass, final Node node, final XMLDeserializationConfig config, String propName, Type<?> propType,
            boolean checkedAttr, boolean isTagByPropertyName, boolean ignoreTypeInfo, boolean isFirstCall) {
        if (node.getNodeType() == Document.TEXT_NODE) {
            return null;
        }

        if (inputClass.equals(Object.class)) {
            inputClass = (Class<T>) MapEntity.class;
        }

        final XMLDeserializationConfig configToUse = check(config);

        boolean hasPropTypes = N.notEmpty(configToUse.getPropTypes());
        Class<?> targetClass = null;

        if (isFirstCall) {
            targetClass = inputClass;
        } else {
            if (propType == null || String.class.equals(propType.clazz()) || propType.isObjectType()) {
                String nodeName = null;
                if (checkedAttr) {
                    nodeName = isTagByPropertyName ? node.getNodeName() : XMLUtil.getAttribute(node, XMLConstants.NAME);
                } else {
                    String nameAttr = XMLUtil.getAttribute(node, XMLConstants.NAME);
                    nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : node.getNodeName();
                }

                targetClass = hasPropTypes && configToUse.hasPropType(nodeName) ? configToUse.getPropType(nodeName).clazz() : null;
            } else {
                targetClass = propType.clazz();
            }

            if (targetClass == null || String.class.equals(targetClass) || Object.class.equals(targetClass)) {
                // if (isOneNode(node)) {
                // targetClass = Map.class;
                // } else {
                // targetClass = List.class;
                // }
                //
                targetClass = List.class;
            }
        }

        final Class<?> typeClass = checkedAttr ? (ignoreTypeInfo ? targetClass : getConcreteClass(targetClass, node)) : getConcreteClass(targetClass, node);
        TypeFactory.getType(typeClass);
        final SerializationType deserializationType = getDeserializationType(targetClass);

        PropInfo propInfo = null;
        Node propNode = null;
        Object propValue = null;
        NodeList propNodes = node.getChildNodes();
        int propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();

        switch (deserializationType) {
            case ENTITY: {
                final boolean ignoreUnmatchedProperty = configToUse.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(typeClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(typeClass);
                final Object result = beanInfo.createBeanResult();

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XMLUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR
                    propInfo = beanInfo.getPropInfo(propName);

                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new ParseException("Unknown property element: " + propName + " for class: " + typeClass.getCanonicalName());
                        }
                    }

                    propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                    if (propType == null) {
                        if (propInfo.jsonXmlType.isSerializable()) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : N.typeOf(getConcreteClass(propInfo.jsonXmlType.clazz(), propNode));
                        }
                    }

                    propValue = getPropValue(inputClass, propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                            true);

                    if (propInfo.jsonXmlExpose != JsonXmlField.Expose.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObjectType()) {
                        keyType = configToUse.getMapKeyType();
                    }
                }

                boolean isStringKey = keyType.clazz() == String.class;

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                final Map<Object, Object> mResult = newPropInstance(typeClass, node);

                propNodes = node.getChildNodes();
                propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
                propNode = null;
                propType = null;
                propValue = null;

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XMLUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR

                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : N.typeOf(getConcreteClass(valueType.clazz(), propNode));
                    }

                    if (propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    propValue = getPropValue(inputClass, propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                            true);

                    mResult.put(isStringKey ? propName : keyType.valueOf(propName), propValue);
                }

                return (T) mResult;
            }

            case MAP_ENTITY: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> valueType = null;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    } else {
                        valueType = objType;
                    }
                }

                MapEntity mResult = N.newBean(MapEntity.class, node.getNodeName());

                propNodes = node.getChildNodes();
                propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
                propNode = null;
                propType = null;
                propValue = null;

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XMLUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XMLUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : N.typeOf(getConcreteClass(valueType.clazz(), propNode));
                    }

                    if (propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    propValue = getPropValue(inputClass, propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                            true);

                    mResult.set(propName, propValue);
                }

                return (T) mResult;
            }

            case ARRAY: { //NOSONAR
                Type<?> eleType = null;

                if (propType != null && (propType.isArray() || propType.isCollection()) && propType.getElementType() != null
                        && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = N.typeOf(typeClass.isArray() ? typeClass.getComponentType() : Object.class);
                    }
                }

                if (XMLUtil.isTextElement(node)) {
                    if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                        return (T) N.typeOf(typeClass).valueOf(XMLUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(typeClass, XMLUtil.getTextContent(node), JDC.create().setElementType(eleType.clazz()));
                    }
                }

                final List<Object> c = Objectory.createList();

                try {
                    NodeList eleNodes = node.getChildNodes();
                    Node eleNode = null;

                    for (int i = 0; i < eleNodes.getLength(); i++) {
                        eleNode = eleNodes.item(i);

                        if (eleNode.getNodeType() == Document.TEXT_NODE) {
                            continue;
                        }

                        if (!checkedAttr) {
                            isTagByPropertyName = Strings.isEmpty(XMLUtil.getAttribute(eleNode, XMLConstants.NAME));
                            ignoreTypeInfo = Strings.isEmpty(XMLUtil.getAttribute(eleNode, XMLConstants.TYPE));
                            checkedAttr = true;
                        }

                        propName = isTagByPropertyName ? eleNode.getNodeName() : XMLUtil.getAttribute(eleNode, XMLConstants.NAME); //NOSONAR

                        propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                        if (propType == null) {
                            propType = ignoreTypeInfo ? eleType : N.typeOf(getConcreteClass(eleType.clazz(), eleNode));
                        }

                        if (propType.clazz() == Object.class) {
                            propType = defaultValueType;
                        }

                        propValue = getPropValue(inputClass, eleNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName,
                                ignoreTypeInfo, false);

                        c.add(propValue);
                    }

                    return collection2Array(typeClass, c);
                } finally {
                    Objectory.recycle(c);
                }
            }

            case COLLECTION: {
                Type<?> eleType = null;

                if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = objType;
                    }
                }

                if (XMLUtil.isTextElement(node)) {
                    if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                        return (T) N.typeOf(typeClass).valueOf(XMLUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(typeClass, XMLUtil.getTextContent(node), JDC.create().setElementType(eleType.clazz()));
                    }
                }

                Collection<Object> result = newPropInstance(typeClass, node);

                NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int i = 0; i < eleNodes.getLength(); i++) {
                    eleNode = eleNodes.item(i);

                    if (eleNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XMLUtil.getAttribute(eleNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XMLUtil.getAttribute(eleNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? eleNode.getNodeName() : XMLUtil.getAttribute(eleNode, XMLConstants.NAME); //NOSONAR

                    propType = hasPropTypes ? configToUse.getPropType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? eleType : N.typeOf(getConcreteClass(eleType.clazz(), eleNode));
                    }

                    if (eleType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    propValue = getPropValue(inputClass, eleNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                            false);

                    result.add(propValue);
                }

                return (T) result;
            }

            default:
                throw new ParseException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only array, collection, map and bean types are supported");
        }
    }

    /**
     * Gets the deserialization type.
     *
     * @param targetClass
     * @return
     */
    protected SerializationType getDeserializationType(Class<?> targetClass) {
        final Type<?> type = N.typeOf(targetClass);

        SerializationType serializationType = type.getSerializationType();

        if (type.isSerializable()) {
            if (type.isObjectArray()) {
                serializationType = SerializationType.ARRAY;
            } else if (type.isCollection()) {
                serializationType = SerializationType.COLLECTION;
            }
        }

        return serializationType;
    }

    /**
     * Gets the prop value.
     *
     * @param <T>
     * @param inputClass
     * @param propNode
     * @param config
     * @param propName
     * @param propType
     * @param propInfo
     * @param checkedAttr
     * @param isTagByPropertyName
     * @param ignoreTypeInfo
     * @param isProp
     * @return
     */
    private <T> Object getPropValue(Class<T> inputClass, Node propNode, final XMLDeserializationConfig config, String propName, Type<?> propType,
            PropInfo propInfo, boolean checkedAttr, boolean isTagByPropertyName, boolean ignoreTypeInfo, boolean isProp) {
        Object propValue = null;

        if (XMLUtil.isTextElement(propNode)) {
            propValue = getPropValue(propName, propType, propInfo, propNode);
        } else {
            if (propType.isMap() || propType.isBean() || propType.isMapEntity()) {
                if (isProp) {
                    propNode = checkOneNode(propNode);
                }

                propType = propType.isObjectType() ? (MapEntity.class.equals(inputClass) ? N.typeOf(MapEntity.class) : N.typeOf(Map.class)) : propType;

                propValue = readByDOMParser(inputClass, propNode, config, propName, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false);
            } else {
                @SuppressWarnings("rawtypes")
                Collection<Object> coll = Collection.class.isAssignableFrom(propType.clazz())
                        ? (Collection<Object>) N.newCollection((Class<Collection>) propType.clazz())
                        : new ArrayList<>();

                Type<?> propEleType = getPropEleType(propType);

                NodeList subPropNodes = propNode.getChildNodes();
                int subPropNodeLength = (subPropNodes == null) ? 0 : subPropNodes.getLength();
                Node subPropNode = null;
                for (int k = 0; k < subPropNodeLength; k++) {
                    subPropNode = subPropNodes.item(k);
                    if (subPropNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    coll.add(readByDOMParser(inputClass, subPropNode, config, propName, propEleType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false));
                }

                propValue = propType.clazz().isArray() ? collection2Array(propType.clazz(), coll) : coll;
            }
        }

        return propValue;
    }

    /**
     * Gets the prop ele type.
     *
     * @param propType
     * @return
     */
    private Type<?> getPropEleType(Type<?> propType) {
        Type<?> propEleType = null;

        if (propType.clazz().isArray()
                && (ClassUtil.isBeanClass(propType.getElementType().clazz()) || Map.class.isAssignableFrom(propType.getElementType().clazz()))) {
            propEleType = propType.getElementType();
        } else if (propType.getParameterTypes().length == 1
                && (ClassUtil.isBeanClass(propType.getParameterTypes()[0].clazz()) || Map.class.isAssignableFrom(propType.getParameterTypes()[0].clazz()))) {
            propEleType = propType.getParameterTypes()[0];
        } else {
            propEleType = N.typeOf(Map.class);
        }

        return propEleType;
    }
}
