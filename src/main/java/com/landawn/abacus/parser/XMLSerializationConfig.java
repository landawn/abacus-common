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

import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 * Configuration class for XML serialization settings.
 * 
 * <p>This class extends {@link JSONXMLSerializationConfig} to provide XML-specific
 * serialization options such as tag naming strategies and type information handling.</p>
 * 
 * <p>Note: XML serialization does not use quotation marks for values, so the
 * quotation-related methods inherited from the parent class should not be used.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XMLSerializationConfig config = new XMLSerializationConfig()
 *     .tagByPropertyName(true)
 *     .writeTypeInfo(false)
 *     .prettyFormat(true)
 *     .setDateTimeFormat(DateTimeFormat.ISO_8601);
 * }</pre>
 * 
 * @see JSONXMLSerializationConfig
 * @see XMLParser
 */
public class XMLSerializationConfig extends JSONXMLSerializationConfig<XMLSerializationConfig> {

    protected static final boolean defaultTagByPropertyName = true;

    protected static final boolean defaultWriteTypeInfo = false;

    private boolean tagByPropertyName = defaultTagByPropertyName;

    private boolean writeTypeInfo = defaultWriteTypeInfo;

    /**
     * Creates a new XMLSerializationConfig with default settings.
     * 
     * <p>Default settings:</p>
     * <ul>
     *   <li>tagByPropertyName: true</li>
     *   <li>writeTypeInfo: false</li>
     *   <li>No quotation marks (set to {@code null} character)</li>
     *   <li>Inherits other defaults from JSONXMLSerializationConfig</li>
     * </ul>
     */
    public XMLSerializationConfig() {
        setCharQuotation(WD.CHAR_ZERO); // NOSONAR
        setStringQuotation(WD.CHAR_ZERO); // NOSONAR
    }

    @Deprecated
    @Override
    public XMLSerializationConfig setStringQuotation(final char stringQuotation) {
        super.setStringQuotation(stringQuotation);

        return this;
    }

    @Deprecated
    @Override
    public XMLSerializationConfig setCharQuotation(final char charQuotation) {
        super.setCharQuotation(charQuotation);

        return this;
    }

    @Deprecated
    @Override
    public XMLSerializationConfig noCharQuotation() {
        super.noCharQuotation();

        return this;
    }

    @Deprecated
    @Override
    public XMLSerializationConfig noStringQuotation() {
        super.noStringQuotation();

        return this;
    }

    @Deprecated
    @Override
    public XMLSerializationConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * Checks if XML tags should be named after property names.
     *
     * <p>When {@code true}, XML elements will use the property name as the tag name.
     * When {@code false}, a generic tag structure is used with name attributes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <p>With tagByPropertyName = true:</p>
     * <pre>{@code
     * <person>
     *   <name>John</name>
     *   <age>30</age>
     * </person>
     * }</pre>
     *
     * <p>With tagByPropertyName = false:</p>
     * <pre>{@code
     * <bean name="person">
     *   <property name="name">John</property>
     *   <property name="age">30</property>
     * </bean>
     * }</pre>
     *
     * @return {@code true} if tags are named after properties, {@code false} otherwise
     */
    public boolean tagByPropertyName() {
        return tagByPropertyName;
    }

    /**
     * Sets whether XML tags should be named after property names.
     * 
     * <p>This setting affects the structure of the generated XML:</p>
     * <ul>
     *   <li>true: Uses property names as XML element names (more readable)</li>
     *   <li>false: Uses generic element names with attributes (more flexible)</li>
     * </ul>
     *
     * @param tagByPropertyName {@code true} to use property names as tags
     * @return this configuration instance for method chaining
     */
    public XMLSerializationConfig tagByPropertyName(final boolean tagByPropertyName) {
        this.tagByPropertyName = tagByPropertyName;

        return this;
    }

    /**
     * Checks whether type information should be included during XML serialization.
     *
     * <p>Type information includes class names and type hints that help with
     * accurate deserialization. When enabled, the XML output will contain type
     * attributes that specify the Java class types of objects.</p>
     *
     * <p>Example with writeTypeInfo=false (no type information):</p>
     * <pre>{@code
     * <items>
     *   <item>...</item>
     * </items>
     * }</pre>
     *
     * <p>Example with writeTypeInfo=true (type information included):</p>
     * <pre>{@code
     * <property name="items" type="java.util.ArrayList">
     *   <item type="com.example.Product">...</item>
     * </property>
     * }</pre>
     *
     * @return {@code true} if type information should be written, {@code false} otherwise
     * @see #writeTypeInfo(boolean)
     */
    public boolean writeTypeInfo() {
        return writeTypeInfo;
    }

    /**
     * Sets whether to include type information during XML serialization.
     *
     * <p>Type information includes class names and type hints that help with
     * accurate deserialization. When enabled, the XML output will contain type
     * attributes that specify the Java class types of objects.</p>
     *
     * <p>Example with writeTypeInfo=false (no type information):</p>
     * <pre>{@code
     * <items>
     *   <item>...</item>
     * </items>
     * }</pre>
     * 
     * <p>Example with writeTypeInfo=true (type information included):</p>
     * <pre>{@code
     * <property name="items" type="java.util.ArrayList">
     *   <item type="com.example.Product">...</item>
     * </property>
     * }</pre>
     *
     * @param writeTypeInfo {@code true} to include type information, {@code false} to omit it
     * @return this configuration instance for method chaining
     * @see #writeTypeInfo()
     */
    public XMLSerializationConfig writeTypeInfo(final boolean writeTypeInfo) {
        this.writeTypeInfo = writeTypeInfo;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getCharQuotation());
        h = 31 * h + N.hashCode(getStringQuotation());
        h = 31 * h + N.hashCode(getDateTimeFormat());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(skipTransientField());
        h = 31 * h + N.hashCode(prettyFormat());
        h = 31 * h + N.hashCode(writeLongAsString());
        h = 31 * h + N.hashCode(writeNullStringAsEmpty);
        h = 31 * h + N.hashCode(writeNullNumberAsZero);
        h = 31 * h + N.hashCode(writeNullBooleanAsFalse);
        h = 31 * h + N.hashCode(writeBigDecimalAsPlain());
        h = 31 * h + N.hashCode(failOnEmptyBean());
        h = 31 * h + N.hashCode(supportCircularReference());
        h = 31 * h + N.hashCode(getIndentation());
        h = 31 * h + N.hashCode(getPropNamingPolicy());
        h = 31 * h + N.hashCode(tagByPropertyName);
        return 31 * h + N.hashCode(writeTypeInfo);
    }

    /**
     * Determines whether this configuration is equal to another object.
     * 
     * <p>Two XMLSerializationConfig instances are considered equal if they have
     * the same values for all configuration settings.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if the configurations are equal, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof XMLSerializationConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(skipTransientField(), other.skipTransientField())
                    && N.equals(prettyFormat(), other.prettyFormat()) && N.equals(writeLongAsString(), other.writeLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse) && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain())
                    && N.equals(failOnEmptyBean(), other.failOnEmptyBean()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(getIndentation(), other.getIndentation()) && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy())
                    && N.equals(tagByPropertyName, other.tagByPropertyName) && N.equals(writeTypeInfo, other.writeTypeInfo);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(getCharQuotation()) + ", stringQuotation="
                + N.toString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(skipTransientField()) + ", prettyFormat=" + N.toString(prettyFormat()) + ", writeLongAsString="
                + N.toString(writeLongAsString()) + ", writeNullStringAsEmpty=" + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero="
                + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse=" + N.toString(writeNullBooleanAsFalse) + ", writeBigDecimalAsPlain="
                + N.toString(writeBigDecimalAsPlain()) + ", failOnEmptyBean=" + N.toString(failOnEmptyBean()) + ", supportCircularReference="
                + N.toString(supportCircularReference()) + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy="
                + N.toString(getPropNamingPolicy()) + ", tagByPropertyName=" + N.toString(tagByPropertyName) + ", writeTypeInfo=" + N.toString(writeTypeInfo)
                + "}";
    }

    /**
     * Factory class for creating XMLSerializationConfig instances.
     * 
     * <p>Provides static factory methods for convenient configuration creation.
     * This inner class is named XSC for brevity.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLSerializationConfig config = XSC.create()
     *     .tagByPropertyName(true)
     *     .writeTypeInfo(false)
     *     .prettyFormat(true);
     * }</pre>
     */
    public static final class XSC extends XMLSerializationConfig {

        /**
         * Constructs a new XSC factory instance.
         *
         * <p>This constructor is provided to satisfy javadoc requirements.
         * The XSC class is intended to be used through its static factory methods
         * rather than instantiation.</p>
         */
        public XSC() {
            super();
        }

        /**
         * Creates a new XMLSerializationConfig instance with default settings.
         *
         * @return a new XMLSerializationConfig instance
         */
        public static XMLSerializationConfig create() {
            return new XMLSerializationConfig();
        }

        /**
         * Creates a new XMLSerializationConfig with specified tag naming and type info settings.
         * 
         * @param tagByPropertyName whether to use property names as XML tags
         * @param writeTypeInfo whether to write type information
         * @return a new configured XMLSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLSerializationConfig of(final boolean tagByPropertyName, final boolean writeTypeInfo) {
            return create().tagByPropertyName(tagByPropertyName).writeTypeInfo(writeTypeInfo);
        }

        /**
         * Creates a new XMLSerializationConfig with the specified date/time format.
         * 
         * @param dateTimeFormat the date/time format to use
         * @return a new configured XMLSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLSerializationConfig of(final DateTimeFormat dateTimeFormat) {
            return create().setDateTimeFormat(dateTimeFormat);
        }

        /**
         * Creates a new XMLSerializationConfig with exclusion and ignored properties.
         * 
         * @param exclusion the exclusion strategy
         * @param ignoredPropNames map of ignored property names by class
         * @return a new configured XMLSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new XMLSerializationConfig with all specified settings.
         * 
         * @param tagByPropertyName whether to use property names as XML tags
         * @param writeTypeInfo whether to write type information
         * @param dateTimeFormat the date/time format to use
         * @param exclusion the exclusion strategy
         * @param ignoredPropNames map of ignored property names by class
         * @return a new configured XMLSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLSerializationConfig of(final boolean tagByPropertyName, final boolean writeTypeInfo, final DateTimeFormat dateTimeFormat,
                final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().tagByPropertyName(tagByPropertyName)
                    .writeTypeInfo(writeTypeInfo)
                    .setDateTimeFormat(dateTimeFormat)
                    .setExclusion(exclusion)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
