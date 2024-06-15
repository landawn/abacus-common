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
import com.landawn.abacus.util.XMLUtil;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
abstract class AbstractXMLParser extends AbstractParser<XMLSerializationConfig, XMLDeserializationConfig> implements XMLParser {

    private static final Logger logger = LoggerFactory.getLogger(AbstractXMLParser.class);

    protected static final int TEXT_SIZE_TO_READ_MORE = 256;

    protected static final JSONParser jsonParser = ParserFactory.createJSONParser();

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jsc = JSC.create().setCharQuotation(WD.CHAR_ZERO);

    @SuppressWarnings("deprecation")
    protected static final JSONSerializationConfig jscWithCircularRefSupported = JSC.create().setCharQuotation(WD.CHAR_ZERO).supportCircularReference(true);

    protected static final Type<?> defaultKeyType = objType;

    protected static final Type<?> defaultValueType = objType;

    protected final XMLSerializationConfig defaultXMLSerializationConfig;

    protected final XMLDeserializationConfig defaultXMLDeserializationConfig;

    protected AbstractXMLParser() {
        this(null, null);
    }

    protected AbstractXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        this.defaultXMLSerializationConfig = xsc != null ? xsc : new XMLSerializationConfig();
        this.defaultXMLDeserializationConfig = xdc != null ? xdc : new XMLDeserializationConfig();
    }

    /**
     *
     * @param source
     * @param targetClass
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(Node source, Class<? extends T> targetClass) {
        return deserialize(source, null, targetClass);
    }

    /**
     * Creates the XML stream reader.
     *
     * @param br
     * @return
     */
    protected XMLStreamReader createXMLStreamReader(Reader br) {
        return XMLUtil.createFilteredStreamReader(XMLUtil.createXMLStreamReader(br),
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
        String txtValue = XMLUtil.getTextContent(propNode);

        if (Strings.isEmpty(txtValue)) {
            Node attrNode = propNode.getAttributes().getNamedItem(XMLConstants.IS_NULL);

            if ((attrNode != null) && Boolean.valueOf(attrNode.getNodeValue())) { //NOSONAR
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

    /**
     *
     * @param config
     * @return
     */
    protected XMLSerializationConfig check(XMLSerializationConfig config) {
        if (config == null) {
            config = defaultXMLSerializationConfig;
        }

        return config;
    }

    /**
     *
     * @param config
     * @return
     */
    protected XMLDeserializationConfig check(XMLDeserializationConfig config) {
        if (config == null) {
            config = defaultXMLDeserializationConfig;
        }

        return config;
    }

    /**
     * New prop instance.
     *
     * @param <T>
     * @param propClass
     * @param node
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(Class<?> propClass, Node node) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        Class<?> attribeTypeClass = getAttributeTypeClass(node);

        return newPropInstance(propClass, attribeTypeClass);
    }

    /**
     * New prop instance.
     *
     * @param <T>
     * @param propClass
     * @param atts
     * @return
     */
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(Class<?> propClass, Attributes atts) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        Class<?> attribeTypeClass = getAttributeTypeClass(atts);

        return newPropInstance(propClass, attribeTypeClass);
    }

    /**
     * Gets the attribute.
     *
     * @param xmlReader
     * @param attrName
     * @return
     */
    protected static String getAttribute(XMLStreamReader xmlReader, String attrName) {
        int attrCount = xmlReader.getAttributeCount();
        if (attrCount == 0) {
            // continue;
        } else if (attrCount == 1) {
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
    protected static Class<?> getAttributeTypeClass(Node node) {
        String typeAttr = XMLUtil.getAttribute(node, XMLConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return N.typeOf(typeAttr).clazz();
    }

    /**
     * Gets the attribute type class.
     *
     * @param atts
     * @return
     */
    protected static Class<?> getAttributeTypeClass(Attributes atts) {
        if (atts == null) {
            return null;
        }

        String typeAttr = atts.getValue(XMLConstants.TYPE);

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
    protected static Class<?> getAttributeTypeClass(XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        String typeAttr = getAttribute(xmlReader, XMLConstants.TYPE);

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
    protected static Class<?> getConcreteClass(Class<?> targetClass, Node node) {
        if (node == null) {
            return targetClass;
        }

        Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param atts
     * @return
     */
    protected static Class<?> getConcreteClass(Class<?> targetClass, Attributes atts) {
        if (atts == null) {
            return targetClass;
        }

        Class<?> typeClass = getAttributeTypeClass(atts);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Gets the concrete class.
     *
     * @param targetClass
     * @param xmlReader
     * @return
     */
    protected static Class<?> getConcreteClass(Class<?> targetClass, XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return targetClass;
        }

        Class<?> typeClass = getAttributeTypeClass(xmlReader);

        return getConcreteClass(targetClass, typeClass);
    }

    /**
     * Check one node.
     *
     * @param eleNode
     * @return
     */
    protected static Node checkOneNode(Node eleNode) {
        NodeList subEleNodes = eleNode.getChildNodes();
        Node subEleNode = null;

        if (subEleNodes.getLength() == 1) {
            subEleNode = subEleNodes.item(0);
        } else {
            for (int j = 0; j < subEleNodes.getLength(); j++) {
                if (subEleNodes.item(j).getNodeType() == Document.TEXT_NODE) {
                    continue; //NOSONAR
                } else if (subEleNode == null) {
                    subEleNode = subEleNodes.item(j);
                } else {
                    throw new ParseException("Only one child node is supported");
                }
            }
        }

        return subEleNode;
    }

    /**
     * Gets the jsc.
     *
     * @param config
     * @return
     */
    protected JSONSerializationConfig getJSC(XMLSerializationConfig config) {
        return config == null || !config.supportCircularReference ? jsc : jscWithCircularRefSupported;
    }

    enum NodeType {
        ENTITY, PROPERTY, ARRAY, ELEMENT, COLLECTION, MAP, ENTRY, KEY, VALUE;
    }
}
