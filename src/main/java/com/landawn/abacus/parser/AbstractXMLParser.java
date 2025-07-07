/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.parser;

import java.io.Reader;
import java.lang.reflect.Modifier;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.XmlUtil;

abstract class AbstractXMLParser extends AbstractParser<XMLSerializationConfig, XMLDeserializationConfig> implements XMLParser {

    private static final Logger logger = LoggerFactory.getLogger(AbstractXMLParser.class);

    // protected static final int TEXT_SIZE_TO_READ_MORE = 256;

    protected static final JSONParser jsonParser = ParserFactory.createJSONParser();

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jsc = JSC.create().setCharQuotation(WD.CHAR_ZERO);

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jscWithEmptyBeanSupported = JSC.create().setCharQuotation(WD.CHAR_ZERO).failOnEmptyBean(false);

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jscWithCircularRefSupported = JSC.create().setCharQuotation(WD.CHAR_ZERO).supportCircularReference(true);

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jscWithCircularRefAndEmptyBeanSupported = JSC.create()
            .setCharQuotation(WD.CHAR_ZERO)
            .failOnEmptyBean(false)
            .supportCircularReference(true);

    protected static final Type<?> defaultKeyType = objType;

    protected static final Type<?> defaultValueType = objType;

    protected final XMLSerializationConfig defaultXMLSerializationConfig;

    protected final XMLDeserializationConfig defaultXMLDeserializationConfig;

    protected AbstractXMLParser() {
        this(null, null);
    }

    protected AbstractXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        defaultXMLSerializationConfig = xsc != null ? xsc : new XMLSerializationConfig();
        defaultXMLDeserializationConfig = xdc != null ? xdc : new XMLDeserializationConfig();
    }

    /**
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Node source, final Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    /**
     * Creates the XML stream reader.
     *
     * @param br
     * @return
     */
    protected XMLStreamReader createXMLStreamReader(final Reader br) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(br),
                reader -> !(reader.isWhiteSpace() || reader.getEventType() == XMLStreamConstants.COMMENT));
    }

    /**
     * Gets the prop value.
     *
     * @param propName
     * @param propType
     * @param propInfo
     * @param propNode
     * @return
     */
    protected Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode) {
        final String txtValue = XmlUtil.getTextContent(propNode);

        if (Strings.isEmpty(txtValue)) {
            final Node attrNode = propNode.getAttributes().getNamedItem(XMLConstants.IS_NULL);

            if ((attrNode != null) && Boolean.parseBoolean(attrNode.getNodeValue())) { //NOSONAR
                return null;
            }
        }

        if (propType == null) {
            throw new ParseException("Can't parse property " + propName + " with value: " + txtValue);
        }

        if (propInfo != null && propInfo.hasFormat) {
            return propInfo.readPropValue(txtValue);
        } else {
            return propType.valueOf(txtValue);
        }
    }

    protected XMLSerializationConfig check(XMLSerializationConfig config) {
        return config == null ? defaultXMLSerializationConfig : config;
    }

    protected XMLDeserializationConfig check(XMLDeserializationConfig config) {
        return config == null ? defaultXMLDeserializationConfig : config;
    }

    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Node node) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        final Class<?> attribeTypeClass = getAttributeTypeClass(node);

        return newPropInstance(propClass, attribeTypeClass);
    }

    /**
     * New prop instance.
     *
     * @param <T>
     * @param propClass
     * @param attrs
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Attributes attrs) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        final Class<?> attribeTypeClass = getAttributeTypeClass(attrs);

        return newPropInstance(propClass, attribeTypeClass);
    }

    /**
     * Gets the attribute.
     *
     * @param xmlReader
     * @param attrName
     * @return
     */
    protected static String getAttribute(final XMLStreamReader xmlReader, final String attrName) {
        final int attrCount = xmlReader.getAttributeCount();
        //noinspection StatementWithEmptyBody
        if (attrCount == 0) {
            // continue;
        } else if (attrCount == 1) {
            //noinspection StatementWithEmptyBody
            if (attrName.equals(xmlReader.getAttributeLocalName(0))) {
                return xmlReader.getAttributeValue(0);
            } else {
                // continue
            }
        } else {
            for (int i = 0; i < attrCount; i++) {
                if (attrName.equals(xmlReader.getAttributeLocalName(i))) {
                    return xmlReader.getAttributeValue(i);
                }
            }
        }

        return null;
    }

    /**
     * Gets the attribute type class.
     *
     * @param node
     * @return
     */
    protected static Class<?> getAttributeTypeClass(final Node node) {
        final String typeAttr = XmlUtil.getAttribute(node, XMLConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return N.typeOf(typeAttr).clazz();
    }

    /**
     * Gets the attribute type class.
     *
     * @param attrs
     * @return
     */
    protected static Class<?> getAttributeTypeClass(final Attributes attrs) {
        if (attrs == null) {
            return null;
        }

        final String typeAttr = attrs.getValue(XMLConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return N.typeOf(typeAttr).clazz();
    }

    /**
     * Gets the attribute type class.
     *
     * @param xmlReader
     * @return
     */
    protected static Class<?> getAttributeTypeClass(final XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        final String typeAttr = getAttribute(xmlReader, XMLConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return N.typeOf(typeAttr).clazz();
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param node
     * @return
     */
    protected static Class<?> getConcreteClass(final Class<?> targetClass, final Node node) {
        if (node == null) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param attrs
     * @return
     */
    protected static Class<?> getConcreteClass(final Class<?> targetClass, final Attributes attrs) {
        if (attrs == null) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(attrs);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param xmlReader
     * @return
     */
    protected static Class<?> getConcreteClass(final Class<?> targetClass, final XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(xmlReader);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Check one node.
     *
     * @param eleNode
     * @return
     */
    protected static Node checkOneNode(final Node eleNode) {
        final NodeList subEleNodes = eleNode.getChildNodes();
        Node subEleNode = null;

        if (subEleNodes.getLength() == 1) {
            subEleNode = subEleNodes.item(0);
        } else {
            for (int j = 0; j < subEleNodes.getLength(); j++) {
                //noinspection StatementWithEmptyBody
                if (subEleNodes.item(j).getNodeType() == Document.TEXT_NODE) {
                    //NOSONAR
                } else if (subEleNode == null) {
                    subEleNode = subEleNodes.item(j);
                } else {
                    throw new ParseException("Only one child node is supported");
                }
            }
        }

        return subEleNode;
    }

    protected static int getNodeLength(final NodeList nodeList) {
        return (nodeList == null) ? 0 : nodeList.getLength();
    }

    /**
     * Gets the jsc.
     *
     * @param config
     * @return
     */
    protected JSONSerializationConfig getJSC(final XMLSerializationConfig config) {
        if (config == null) {
            return jsc;
        }

        if (config.supportCircularReference()) {
            if (!config.failOnEmptyBean()) {
                return jscWithCircularRefAndEmptyBeanSupported;
            } else {
                return jscWithCircularRefSupported;
            }
        } else if (!config.failOnEmptyBean()) {
            return jscWithEmptyBeanSupported;
        }

        return jsc;
    }

    enum NodeType {
        ENTITY, PROPERTY, ARRAY, ELEMENT, COLLECTION, MAP, ENTRY, KEY, VALUE
    }
}
