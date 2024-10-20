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

import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 */
public class XMLSerializationConfig extends JSONXMLSerializationConfig<XMLSerializationConfig> {

    protected static final boolean defaultTagByPropertyName = true;

    protected static final boolean defaultIgnoreTypeInfo = true;

    private boolean tagByPropertyName = defaultTagByPropertyName;

    private boolean ignoreTypeInfo = defaultIgnoreTypeInfo;

    /**
     *
     */
    public XMLSerializationConfig() {
        setCharQuotation(WD.CHAR_ZERO); // NOSONAR
        setStringQuotation(WD.CHAR_ZERO); // NOSONAR
    }

    /**
     * Sets the string quotation.
     *
     * @param stringQuotation
     * @return
     * @deprecated this method should not be called
     */
    @Deprecated
    @Override
    public XMLSerializationConfig setStringQuotation(final char stringQuotation) {
        super.setStringQuotation(stringQuotation);

        return this;
    }

    /**
     * Sets the char quotation.
     *
     * @param charQuotation
     * @return
     * @deprecated this method should not be called
     */
    @Deprecated
    @Override
    public XMLSerializationConfig setCharQuotation(final char charQuotation) {
        super.setCharQuotation(charQuotation);

        return this;
    }

    /**
     * Sets the string quotation.
     *
     * @return
     * @deprecated this method should not be called
     */
    @Deprecated
    @Override
    public XMLSerializationConfig noCharQuotation() {
        super.noCharQuotation();

        return this;
    }

    /**
     * Sets the string quotation.
     *
     * @return
     * @deprecated this method should not be called
     */
    @Deprecated
    @Override
    public XMLSerializationConfig noStringQuotation() {
        super.noStringQuotation();

        return this;
    }

    /**
     * Sets the string quotation.
     *
     * @return
     * @deprecated this method should not be called
     */
    @Deprecated
    @Override
    public XMLSerializationConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * Checks if is tag by property name.
     *
     * @return {@code true}, if is tag by property name
     */
    public boolean tagByPropertyName() {
        return tagByPropertyName;
    }

    /**
     * Sets the tag by property name.
     *
     * @param tagByPropertyName
     * @return
     */
    public XMLSerializationConfig tagByPropertyName(final boolean tagByPropertyName) {
        this.tagByPropertyName = tagByPropertyName;

        return this;
    }

    /**
     * Checks if is ignore type info.
     *
     * @return {@code true}, if is ignore type info
     */
    public boolean ignoreTypeInfo() {
        return ignoreTypeInfo;
    }

    /**
     * Sets the ignore type info.
     *
     * @param ignoreTypeInfo
     * @return
     */
    public XMLSerializationConfig ignoreTypeInfo(final boolean ignoreTypeInfo) {
        this.ignoreTypeInfo = ignoreTypeInfo;

        return this;
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public XMLSerializationConfig copy() {
    //        final XMLSerializationConfig copy = new XMLSerializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.setCharQuotation(this.getCharQuotation());
    //        copy.setStringQuotation(this.getStringQuotation());
    //        copy.setDateTimeFormat(this.getDateTimeFormat());
    //        copy.setExclusion(this.getExclusion());
    //        copy.setSkipTransientField(this.isSkipTransientField());
    //        copy.setPrettyFormat(this.isPrettyFormat());
    //        copy.supportCircularReference(this.supportCircularReference());
    //        copy.writeBigDecimalAsPlain(this.writeBigDecimalAsPlain());
    //        copy.setIndentation(this.getIndentation());
    //        copy.setPropNamingPolicy(this.getPropNamingPolicy());
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.tagByPropertyName = this.tagByPropertyName;
    //        copy.ignoreTypeInfo = this.ignoreTypeInfo;
    //
    //        return copy;
    //    }

    /**
     *
     *
     * @return
     */
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
        return 31 * h + N.hashCode(ignoreTypeInfo);
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final XMLSerializationConfig other) {
            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(skipTransientField(), other.skipTransientField())
                    && N.equals(prettyFormat(), other.prettyFormat()) && N.equals(writeLongAsString(), other.writeLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse) && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain())
                    && N.equals(failOnEmptyBean(), other.failOnEmptyBean()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(getIndentation(), other.getIndentation()) && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy())
                    && N.equals(tagByPropertyName, other.tagByPropertyName) && N.equals(ignoreTypeInfo, other.ignoreTypeInfo)) {

                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(getCharQuotation()) + ", stringQuotation="
                + N.toString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(skipTransientField()) + ", prettyFormat=" + N.toString(prettyFormat()) + ", writeLongAsString="
                + N.toString(writeLongAsString()) + ", writeNullStringAsEmpty=" + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero="
                + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse=" + N.toString(writeNullBooleanAsFalse) + ", writeBigDecimalAsPlain="
                + N.toString(writeBigDecimalAsPlain()) + ", failOnEmptyBean=" + N.toString(failOnEmptyBean()) + ", supportCircularReference="
                + N.toString(supportCircularReference()) + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy="
                + N.toString(getPropNamingPolicy()) + ", tagByPropertyName=" + N.toString(tagByPropertyName) + ", ignoreTypeInfo=" + N.toString(ignoreTypeInfo)
                + "}";
    }

    /**
     * The Class XSC.
     */
    public static final class XSC extends XMLSerializationConfig {

        /**
         *
         * @return
         */
        public static XMLSerializationConfig create() {
            return new XMLSerializationConfig();
        }

        /**
         *
         * @param tagByPropertyName
         * @param ignoreTypeInfo
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static XMLSerializationConfig of(final boolean tagByPropertyName, final boolean ignoreTypeInfo) {
            return create().tagByPropertyName(tagByPropertyName).ignoreTypeInfo(ignoreTypeInfo);
        }

        /**
         *
         * @param dateTimeFormat
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static XMLSerializationConfig of(final DateTimeFormat dateTimeFormat) {
            return create().setDateTimeFormat(dateTimeFormat);
        }

        /**
         *
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static XMLSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param tagByPropertyName
         * @param ignoreTypeInfo
         * @param dateTimeFormat
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static XMLSerializationConfig of(final boolean tagByPropertyName, final boolean ignoreTypeInfo, final DateTimeFormat dateTimeFormat,
                final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().tagByPropertyName(tagByPropertyName)
                    .ignoreTypeInfo(ignoreTypeInfo)
                    .setDateTimeFormat(dateTimeFormat)
                    .setExclusion(exclusion)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
