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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JSONSerializationConfig extends SerializationConfig<JSONSerializationConfig> {

    protected static final boolean defaultQuotePropName = true;

    protected static final boolean defaultQuoteMapKey = true;

    protected static final boolean defaultWrapRootValue = false;

    protected static final boolean defaultBracketRootValue = true;

    boolean quotePropName = defaultQuotePropName;

    boolean quoteMapKey = defaultQuoteMapKey;

    boolean wrapRootValue = defaultWrapRootValue;

    boolean bracketRootValue = defaultBracketRootValue;

    public JSONSerializationConfig() {
    }

    /**
     * The default value is false if it's not set.
     *
     * @return true, if is quote prop name
     */
    public boolean isQuotePropName() {
        return quotePropName;
    }

    /**
     * Sets the quote prop name.
     *
     * @param quotePropName
     * @return
     */
    public JSONSerializationConfig setQuotePropName(boolean quotePropName) {
        this.quotePropName = quotePropName;

        return this;
    }

    /**
     * The default value is false if it's not set.
     *
     * @return true, if is quote map key
     */
    public boolean isQuoteMapKey() {
        return quoteMapKey;
    }

    /**
     * Sets the quote map key.
     *
     * @param quoteMapKey
     * @return
     */
    public JSONSerializationConfig setQuoteMapKey(boolean quoteMapKey) {
        this.quoteMapKey = quoteMapKey;

        return this;
    }

    /**
     * Checks if is wrap root value.
     *
     * @return true, if is wrap root value
     */
    public boolean isWrapRootValue() {
        return wrapRootValue;
    }

    /**
     * Sets the wrap root value.
     *
     * @param wrapRootValue
     * @return
     */
    public JSONSerializationConfig setWrapRootValue(boolean wrapRootValue) {
        this.wrapRootValue = wrapRootValue;

        return this;
    }

    /**
     * The default value is true if it's not set.
     *
     * @return true, if is bracket root value
     */
    public boolean isBracketRootValue() {
        return bracketRootValue;
    }

    /**
     * It's set to if enclose the JSON string/text with '{' and '}' or '[' and
     * ']'.
     *
     * @param bracketRootValue
     * @return
     */
    public JSONSerializationConfig setBracketRootValue(boolean bracketRootValue) {
        this.bracketRootValue = bracketRootValue;

        return this;
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public JSONSerializationConfig copy() {
    //        final JSONSerializationConfig copy = new JSONSerializationConfig();
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
    //        copy.quotePropName = this.quotePropName;
    //        copy.quoteMapKey = this.quoteMapKey;
    //        copy.wrapRootValue = this.wrapRootValue;
    //        copy.bracketRootValue = this.bracketRootValue;
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
        h = 31 * h + N.hashCode(quotePropName);
        h = 31 * h + N.hashCode(quoteMapKey);
        h = 31 * h + N.hashCode(wrapRootValue);
        h = 31 * h + N.hashCode(bracketRootValue);

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

        if (obj instanceof JSONSerializationConfig) {
            JSONSerializationConfig other = (JSONSerializationConfig) obj;

            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation())
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(isSkipTransientField(), other.isSkipTransientField())
                    && N.equals(isPrettyFormat(), other.isPrettyFormat()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain()) && N.equals(getIndentation(), other.getIndentation())
                    && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy()) && N.equals(getIgnoredPropNames(), other.getIgnoredPropNames())
                    && N.equals(quotePropName, other.quotePropName) && N.equals(quoteMapKey, other.quoteMapKey) && N.equals(wrapRootValue, other.wrapRootValue)
                    && N.equals(bracketRootValue, other.bracketRootValue)) {

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
                + N.toString(getIgnoredPropNames()) + ", quotePropName=" + N.toString(quotePropName) + ", quoteMapKey=" + N.toString(quoteMapKey)
                + ", wrapRootValue=" + N.toString(wrapRootValue) + ", bracketRootValue=" + N.toString(bracketRootValue) + "}";
    }

    /**
     * The Class JSC.
     */
    public static final class JSC extends JSONSerializationConfig {

        /**
         *
         * @return
         */
        public static JSONSerializationConfig create() {
            return new JSONSerializationConfig();
        }

        /**
         *
         * @param quotePropName
         * @param quoteMapKey
         * @return
         * @deprecated
         */
        @Deprecated
        public static JSONSerializationConfig of(boolean quotePropName, boolean quoteMapKey) {
            return create().setQuotePropName(quotePropName).setQuoteMapKey(quoteMapKey);
        }

        /**
         *
         * @param dateTimeFormat
         * @return
         * @deprecated
         */
        @Deprecated
        public static JSONSerializationConfig of(DateTimeFormat dateTimeFormat) {
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
        public static JSONSerializationConfig of(Exclusion exclusion, Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param quotePropName
         * @param quoteMapKey
         * @param dateTimeFormat
         * @param exclusion
         * @param ignoredPropNames
         * @return
         * @deprecated
         */
        @Deprecated
        public static JSONSerializationConfig of(boolean quotePropName, boolean quoteMapKey, DateTimeFormat dateTimeFormat, Exclusion exclusion,
                Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setQuotePropName(quotePropName)
                    .setQuoteMapKey(quoteMapKey)
                    .setDateTimeFormat(dateTimeFormat)
                    .setExclusion(exclusion)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
