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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * Configuration class for XML serialization settings.
 *
 * <p>This class extends {@link JsonXmlSerConfig} to provide XML-specific
 * serialization options such as tag naming strategies and type information handling.</p>
 *
 * <p>Note: XML serialization does not use quotation marks for values, so the
 * quotation-related methods inherited from the parent class should not be used.</p>
 *
 * <p><b>Note — intentional omission of the JSON-only Dataset knobs:</b> the
 * {@code writeDatasetAsRows} / {@code writeColumnType} / {@code writeRowColumnKeyType}
 * options live on {@link JsonSerConfig} on purpose: they are read only by the JSON
 * serializer ({@code JsonParserImpl}). The XML serializers ({@code XmlParserImpl} /
 * {@code AbacusXmlParserImpl}) do not consume them, so exposing them here would create
 * dead configuration. They are therefore deliberately NOT provided on {@code XmlSerConfig}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XmlSerConfig config = new XmlSerConfig()
 *     .setTagByPropertyName(true)
 *     .setWriteTypeInfo(false)
 *     .setPrettyFormat(true)
 *     .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
 * }</pre>
 *
 * @see JsonXmlSerConfig
 * @see XmlParser
 */
public class XmlSerConfig extends JsonXmlSerConfig<XmlSerConfig> {

    /** Default value for {@link #isTagByPropertyName()} ({@code true}). */
    protected static final boolean defaultTagByPropertyName = true;

    /** Default value for {@link #isWriteTypeInfo()} ({@code false}). */
    protected static final boolean defaultWriteTypeInfo = false;

    private boolean tagByPropertyName = defaultTagByPropertyName;

    private boolean writeTypeInfo = defaultWriteTypeInfo;

    /**
     * Creates a new XmlSerConfig with default settings.
     *
     * <p>Default settings:</p>
     * <ul>
     *   <li>tagByPropertyName: {@code true}</li>
     *   <li>writeTypeInfo: {@code false}</li>
     *   <li>Both char and string quotation are disabled (set to character {@code 0}),
     *       since XML encodes values as element text content rather than quoted strings</li>
     *   <li>Inherits other defaults from {@link JsonXmlSerConfig}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true)
     *     .setTagByPropertyName(true);
     * }</pre>
     *
     */
    public XmlSerConfig() {
        // Assign the inherited (package-private) quotation fields directly instead of routing through the
        // @Deprecated setCharQuotation/setStringQuotation overrides below. The deprecated setters document
        // "should not be called"; invoking them from our own constructor would contradict that. SK.CHAR_ZERO
        // is one of the values those setters would accept, so the resulting field values are identical (char 0).
        charQuotation = SK.CHAR_ZERO; // NOSONAR
        stringQuotation = SK.CHAR_ZERO; // NOSONAR
    }

    /**
     * Sets the string quotation character.
     * Note: This method is deprecated as XML does not use quotation for string values.
     *
     * @param stringQuotation the character to use for quoting strings
     * @return this instance for method chaining
     * @deprecated this method should not be called as XML does not quote string values.
     */
    @Deprecated
    @Override
    public XmlSerConfig setStringQuotation(final char stringQuotation) {
        super.setStringQuotation(stringQuotation);

        return this;
    }

    /**
     * Sets the character quotation character for char values.
     * Note: This method is deprecated as XML does not use quotation for char values.
     *
     * @param charQuotation the character to use for quoting char values
     * @return this instance for method chaining
     * @deprecated this method should not be called as XML does not quote char values.
     */
    @Deprecated
    @Override
    public XmlSerConfig setCharQuotation(final char charQuotation) {
        super.setCharQuotation(charQuotation);

        return this;
    }

    /**
     * Disables character quotation.
     * Note: This method is deprecated as XML does not use quotation for char values.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as XML does not quote char values.
     */
    @Deprecated
    @Override
    public XmlSerConfig noCharQuotation() {
        super.noCharQuotation();

        return this;
    }

    /**
     * Disables string quotation.
     * Note: This method is deprecated as XML does not use quotation for string values.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as XML does not quote string values.
     */
    @Deprecated
    @Override
    public XmlSerConfig noStringQuotation() {
        super.noStringQuotation();

        return this;
    }

    /**
     * Disables all quotation.
     * Note: This method is deprecated as XML does not use quotation for values.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as XML does not quote values.
     */
    @Deprecated
    @Override
    public XmlSerConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * Checks if XML tags should be named after property names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = new XmlSerConfig();
     * boolean byProp = config.isTagByPropertyName(); // returns true (default)
     *
     * config.setTagByPropertyName(false);
     * boolean disabled = config.isTagByPropertyName(); // returns false
     * }</pre>
     *
     * @return {@code true} if tags are named after properties, {@code false} otherwise
     */
    public boolean isTagByPropertyName() {
        return tagByPropertyName;
    }

    /**
     * Sets whether XML tags should be named after property names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = new XmlSerConfig();
     * config.setTagByPropertyName(false);                // returns this (config)
     * boolean byProp = config.isTagByPropertyName();     // returns false
     *
     * config.setTagByPropertyName(true);                 // returns this (config)
     * boolean restored = config.isTagByPropertyName();   // returns true (default)
     * }</pre>
     *
     * @param tagByPropertyName {@code true} to use property names as tags
     * @return this configuration instance for method chaining
     */
    public XmlSerConfig setTagByPropertyName(final boolean tagByPropertyName) {
        this.tagByPropertyName = tagByPropertyName;

        return this;
    }

    /**
     * Checks whether type information should be included during XML serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = new XmlSerConfig();
     * boolean writeType = config.isWriteTypeInfo(); // returns false (default)
     *
     * config.setWriteTypeInfo(true);
     * boolean enabled = config.isWriteTypeInfo(); // returns true
     * }</pre>
     *
     * @return {@code true} if type information should be written, {@code false} otherwise
     */
    public boolean isWriteTypeInfo() {
        return writeTypeInfo;
    }

    /**
     * Sets whether to include type information during XML serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = new XmlSerConfig();
     * config.setWriteTypeInfo(true);                 // returns this (config)
     * boolean enabled = config.isWriteTypeInfo();    // returns true
     *
     * config.setWriteTypeInfo(false);                // returns this (config)
     * boolean disabled = config.isWriteTypeInfo();   // returns false (default)
     * }</pre>
     *
     * @param writeTypeInfo {@code true} to include type information, {@code false} to omit it
     * @return this configuration instance for method chaining
     */
    public XmlSerConfig setWriteTypeInfo(final boolean writeTypeInfo) {
        this.writeTypeInfo = writeTypeInfo;

        return this;
    }

    /**
     * Calculates the hash code for this configuration object.
     * The hash code is based on all configuration settings.
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(getCharQuotation());
        h = 31 * h + N.hashCode(getStringQuotation());
        h = 31 * h + N.hashCode(getDateTimeFormat());
        h = 31 * h + N.hashCode(getExclusion());
        h = 31 * h + N.hashCode(isSkipTransientField());
        h = 31 * h + N.hashCode(isPrettyFormat());
        h = 31 * h + N.hashCode(isWriteLongAsString());
        h = 31 * h + N.hashCode(writeNullStringAsEmpty);
        h = 31 * h + N.hashCode(writeNullNumberAsZero);
        h = 31 * h + N.hashCode(writeNullBooleanAsFalse);
        h = 31 * h + N.hashCode(isWriteBigDecimalAsPlain());
        h = 31 * h + N.hashCode(isFailOnEmptyBean());
        h = 31 * h + N.hashCode(isCircularReferenceSupported());
        h = 31 * h + N.hashCode(getIndentation());
        h = 31 * h + N.hashCode(getPropNamingPolicy());
        h = 31 * h + N.hashCode(tagByPropertyName);
        return 31 * h + N.hashCode(writeTypeInfo);
    }

    /**
     * Determines whether this configuration is equal to another object.
     *
     * <p>Two XmlSerConfig instances are considered equal if they have
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

        if (obj instanceof XmlSerConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(isSkipTransientField(), other.isSkipTransientField())
                    && N.equals(isPrettyFormat(), other.isPrettyFormat()) && N.equals(isWriteLongAsString(), other.isWriteLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse)
                    && N.equals(isWriteBigDecimalAsPlain(), other.isWriteBigDecimalAsPlain()) && N.equals(isFailOnEmptyBean(), other.isFailOnEmptyBean())
                    && N.equals(isCircularReferenceSupported(), other.isCircularReferenceSupported()) && N.equals(getIndentation(), other.getIndentation())
                    && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy()) && N.equals(tagByPropertyName, other.tagByPropertyName)
                    && N.equals(writeTypeInfo, other.writeTypeInfo);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration object.
     * The string contains all configuration settings in a readable format.
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(getCharQuotation()) + ", stringQuotation="
                + N.toString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(isSkipTransientField()) + ", prettyFormat=" + N.toString(isPrettyFormat()) + ", writeLongAsString="
                + N.toString(isWriteLongAsString()) + ", writeNullStringAsEmpty=" + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero="
                + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse=" + N.toString(writeNullBooleanAsFalse) + ", writeBigDecimalAsPlain="
                + N.toString(isWriteBigDecimalAsPlain()) + ", failOnEmptyBean=" + N.toString(isFailOnEmptyBean()) + ", circularReferenceSupported="
                + N.toString(isCircularReferenceSupported()) + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy="
                + N.toString(getPropNamingPolicy()) + ", tagByPropertyName=" + N.toString(tagByPropertyName) + ", writeTypeInfo=" + N.toString(writeTypeInfo)
                + "}";
    }

    /**
     * Creates a new XmlSerConfig instance with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig config = XmlSerConfig.create()
     *     .setWriteTypeInfo(false)
     *     .setPrettyFormat(true);
     * }</pre>
     *
     * @return a new XmlSerConfig instance
     */
    public static XmlSerConfig create() {
        return new XmlSerConfig();
    }

}
