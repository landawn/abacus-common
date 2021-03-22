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

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class XMLSerializationConfig extends SerializationConfig<XMLSerializationConfig> {

    protected static final boolean defaultTagByPropertyName = true;

    protected static final boolean defaultIgnoreTypeInfo = true;

    boolean tagByPropertyName = defaultTagByPropertyName;

    boolean ignoreTypeInfo = defaultIgnoreTypeInfo;

    public XMLSerializationConfig() {
        setCharQuotation(WD.CHAR_0);
        setStringQuotation(WD.CHAR_0);
    }

    /**
     * Checks if is tag by property name.
     *
     * @return true, if is tag by property name
     */
    public boolean isTagByPropertyName() {
        return tagByPropertyName;
    }

    /**
     * Sets the tag by property name.
     *
     * @param tagByPropertyName
     * @return
     */
    public XMLSerializationConfig setTagByPropertyName(boolean tagByPropertyName) {
        this.tagByPropertyName = tagByPropertyName;

        return this;
    }

    /**
     * Checks if is ignore type info.
     *
     * @return true, if is ignore type info
     */
    public boolean isIgnoreTypeInfo() {
        return ignoreTypeInfo;
    }

    /**
     * Sets the ignore type info.
     *
     * @param ignoreTypeInfo
     * @return
     */
    public XMLSerializationConfig setIgnoreTypeInfo(boolean ignoreTypeInfo) {
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
        h = 31 * h + N.hashCode(supportCircularReference());
        h = 31 * h + N.hashCode(writeBigDecimalAsPlain());
        h = 31 * h + N.hashCode(getIndentation());
        h = 31 * h + N.hashCode(getPropNamingPolicy());
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(tagByPropertyName);
        h = 31 * h + N.hashCode(ignoreTypeInfo);

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof XMLSerializationConfig) {
            XMLSerializationConfig other = (XMLSerializationConfig) obj;

            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation())
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(isSkipTransientField(), other.isSkipTransientField())
                    && N.equals(isPrettyFormat(), other.isPrettyFormat()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain()) && N.equals(getIndentation(), other.getIndentation())
                    && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy()) && N.equals(getIgnoredPropNames(), other.getIgnoredPropNames())
                    && N.equals(tagByPropertyName, other.tagByPropertyName) && N.equals(ignoreTypeInfo, other.ignoreTypeInfo)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(getCharQuotation()) + ", stringQuotation="
                + N.toString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(isSkipTransientField()) + ", prettyFormat=" + N.toString(isPrettyFormat())
                + ", supportCircularReference=" + N.toString(supportCircularReference()) + ", writeBigDecimalAsPlain=" + N.toString(writeBigDecimalAsPlain())
                + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy=" + N.toString(getPropNamingPolicy()) + ", ignoredPropNames="
                + N.toString(getIgnoredPropNames()) + ", tagByPropertyName=" + N.toString(tagByPropertyName) + ", ignoreTypeInfo=" + N.toString(ignoreTypeInfo)
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
         * @deprecated
         */
        @Deprecated
        public static XMLSerializationConfig of(boolean tagByPropertyName, boolean ignoreTypeInfo) {
            return create().setTagByPropertyName(tagByPropertyName).setIgnoreTypeInfo(ignoreTypeInfo);
        }

        /**
         *
         * @param dateTimeFormat
         * @return
         * @deprecated
         */
        @Deprecated
        public static XMLSerializationConfig of(DateTimeFormat dateTimeFormat) {
            return create().setDateTimeFormat(dateTimeFormat);
        }

        /**
         *
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated
         */
        @Deprecated
        public static XMLSerializationConfig of(Exclusion exclusion, Map<Class<?>, Collection<String>> ignoredPropNames) {
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
         * @deprecated
         */
        @Deprecated
        public static XMLSerializationConfig of(boolean tagByPropertyName, boolean ignoreTypeInfo, DateTimeFormat dateTimeFormat, Exclusion exclusion,
                Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setTagByPropertyName(tagByPropertyName)
                    .setIgnoreTypeInfo(ignoreTypeInfo)
                    .setDateTimeFormat(dateTimeFormat)
                    .setExclusion(exclusion)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
