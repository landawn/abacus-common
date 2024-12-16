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

public class JSONSerializationConfig extends JSONXMLSerializationConfig<JSONSerializationConfig> {

    protected static final boolean defaultQuotePropName = true;

    protected static final boolean defaultQuoteMapKey = true;

    protected static final boolean defaultBracketRootValue = true;

    protected static final boolean defaultWrapRootValue = false;

    private boolean writeNullToEmpty = false;
    private boolean writeDataSetByRow = false;
    private boolean writeRowColumnKeyType = false; // for Sheet;
    private boolean writeColumnType = false; // for DataSet and Sheet

    private boolean quotePropName = defaultQuotePropName;

    private boolean quoteMapKey = defaultQuoteMapKey;

    private boolean bracketRootValue = defaultBracketRootValue;

    private boolean wrapRootValue = defaultWrapRootValue;

    public JSONSerializationConfig() { //NOSONAR
    }

    public boolean writeNullToEmpty() {
        return writeNullToEmpty;
    }

    /**
     *
     * @param writeNullToEmpty
     * @return
     */
    public JSONSerializationConfig writeNullToEmpty(final boolean writeNullToEmpty) {
        this.writeNullToEmpty = writeNullToEmpty;

        return this;
    }

    public boolean writeDataSetByRow() {
        return writeDataSetByRow;
    }

    /**
     *
     * @param writeDataSetByRow
     * @return
     */
    public JSONSerializationConfig writeDataSetByRow(final boolean writeDataSetByRow) {
        this.writeDataSetByRow = writeDataSetByRow;

        return this;
    }

    public boolean writeRowColumnKeyType() {
        return writeRowColumnKeyType;
    }

    /**
     *
     * @param writeRowColumnKeyType
     * @return
     */
    public JSONSerializationConfig writeRowColumnKeyType(final boolean writeRowColumnKeyType) {
        this.writeRowColumnKeyType = writeRowColumnKeyType;

        return this;
    }

    public boolean writeColumnType() {
        return writeColumnType;
    }

    /**
     *
     * @param writeColumnType
     * @return
     */
    public JSONSerializationConfig writeColumnType(final boolean writeColumnType) {
        this.writeColumnType = writeColumnType;

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
    public JSONSerializationConfig setCharQuotation(final char charQuotation) {
        super.setCharQuotation(charQuotation);

        return this;
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
    public JSONSerializationConfig setStringQuotation(final char stringQuotation) {
        super.setStringQuotation(stringQuotation);

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
    public JSONSerializationConfig noCharQuotation() {
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
    public JSONSerializationConfig noStringQuotation() {
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
    public JSONSerializationConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * The default value is {@code false} if it's not set.
     *
     * @return {@code true}, if is quote prop name
     */
    public boolean quotePropName() {
        return quotePropName;
    }

    /**
     * Sets the quote prop name.
     *
     * @param quotePropName
     * @return
     */
    public JSONSerializationConfig quotePropName(final boolean quotePropName) {
        this.quotePropName = quotePropName;

        return this;
    }

    /**
     * The default value is {@code false} if it's not set.
     *
     * @return {@code true}, if is quote map key
     */
    public boolean quoteMapKey() {
        return quoteMapKey;
    }

    /**
     * Sets the quote map key.
     *
     * @param quoteMapKey
     * @return
     */
    public JSONSerializationConfig quoteMapKey(final boolean quoteMapKey) {
        this.quoteMapKey = quoteMapKey;

        return this;
    }

    /**
     * The default value is {@code true} if it's not set.
     *
     * @return {@code true}, if is bracket root value
     */
    public boolean bracketRootValue() {
        return bracketRootValue;
    }

    /**
     * It's set to if enclose the JSON string/text with '{' and '}' or '[' and
     * ']'.
     *
     * @param bracketRootValue
     * @return
     */
    public JSONSerializationConfig bracketRootValue(final boolean bracketRootValue) {
        this.bracketRootValue = bracketRootValue;

        return this;
    }

    /**
     * Checks if is wrap root value.
     *
     * @return {@code true}, if is wrap root value
     */
    public boolean wrapRootValue() {
        return wrapRootValue;
    }

    /**
     * Sets the wrap root value.
     *
     * @param wrapRootValue
     * @return
     */
    public JSONSerializationConfig wrapRootValue(final boolean wrapRootValue) {
        this.wrapRootValue = wrapRootValue;

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
        h = 31 * h + N.hashCode(skipTransientField());
        h = 31 * h + N.hashCode(prettyFormat());
        h = 31 * h + N.hashCode(writeLongAsString());
        h = 31 * h + N.hashCode(writeNullStringAsEmpty);
        h = 31 * h + N.hashCode(writeNullNumberAsZero);
        h = 31 * h + N.hashCode(writeNullBooleanAsFalse);
        h = 31 * h + N.hashCode(writeNullToEmpty);
        h = 31 * h + N.hashCode(writeDataSetByRow);
        h = 31 * h + N.hashCode(writeRowColumnKeyType);
        h = 31 * h + N.hashCode(writeColumnType);
        h = 31 * h + N.hashCode(writeBigDecimalAsPlain());
        h = 31 * h + N.hashCode(failOnEmptyBean());
        h = 31 * h + N.hashCode(supportCircularReference());
        h = 31 * h + N.hashCode(getIndentation());
        h = 31 * h + N.hashCode(getPropNamingPolicy());
        h = 31 * h + N.hashCode(quotePropName);
        h = 31 * h + N.hashCode(quoteMapKey);
        h = 31 * h + N.hashCode(bracketRootValue);
        return 31 * h + N.hashCode(wrapRootValue);
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

        if (obj instanceof final JSONSerializationConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(skipTransientField(), other.skipTransientField())
                    && N.equals(prettyFormat(), other.prettyFormat()) && N.equals(writeLongAsString(), other.writeLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse) && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain())
                    && N.equals(failOnEmptyBean(), other.failOnEmptyBean()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(getIndentation(), other.getIndentation()) && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy())
                    && N.equals(writeNullToEmpty, other.writeNullToEmpty) && N.equals(writeDataSetByRow, other.writeDataSetByRow)
                    && N.equals(writeRowColumnKeyType, other.writeRowColumnKeyType) && N.equals(writeColumnType, other.writeColumnType)
                    && N.equals(quotePropName, other.quotePropName) && N.equals(quoteMapKey, other.quoteMapKey)
                    && N.equals(bracketRootValue, other.bracketRootValue) && N.equals(wrapRootValue, other.wrapRootValue);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(getCharQuotation()) + ", stringQuotation="
                + N.toString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(skipTransientField()) + ", prettyFormat=" + N.toString(prettyFormat()) + ", writeNullStringAsEmpty="
                + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero=" + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse="
                + N.toString(writeNullBooleanAsFalse) + ", writeNullToEmpty=" + N.toString(writeNullToEmpty) + ", writeDataSetByRow="
                + N.toString(writeDataSetByRow) + ", writeRowColumnKeyType=" + N.toString(writeRowColumnKeyType) + ", writeColumnType="
                + N.toString(writeColumnType) + ", writeBigDecimalAsPlain=" + N.toString(writeBigDecimalAsPlain()) + N.toString(failOnEmptyBean())
                + ", failOnEmptyBean=" + N.toString(failOnEmptyBean()) + ", supportCircularReference=" + N.toString(supportCircularReference())
                + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy=" + N.toString(getPropNamingPolicy()) + ", quotePropName="
                + N.toString(quotePropName) + ", quoteMapKey=" + N.toString(quoteMapKey) + ", bracketRootValue=" + N.toString(bracketRootValue)
                + ", wrapRootValue=" + N.toString(wrapRootValue) + "}";
    }

    /**
     * The Class JSC.
     */
    public static final class JSC extends JSONSerializationConfig {

        public static JSONSerializationConfig create() {
            return new JSONSerializationConfig();
        }

        /**
         *
         * @param quotePropName
         * @param quoteMapKey
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONSerializationConfig of(final boolean quotePropName, final boolean quoteMapKey) {
            return create().quotePropName(quotePropName).quoteMapKey(quoteMapKey);
        }

        /**
         *
         * @param dateTimeFormat
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONSerializationConfig of(final DateTimeFormat dateTimeFormat) {
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
        public static JSONSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
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
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONSerializationConfig of(final boolean quotePropName, final boolean quoteMapKey, final DateTimeFormat dateTimeFormat,
                final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().quotePropName(quotePropName)
                    .quoteMapKey(quoteMapKey)
                    .setDateTimeFormat(dateTimeFormat)
                    .setExclusion(exclusion)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
