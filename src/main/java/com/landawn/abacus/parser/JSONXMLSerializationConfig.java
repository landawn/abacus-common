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

import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <C>
 */
public abstract class JSONXMLSerializationConfig<C extends JSONXMLSerializationConfig<C>> extends SerializationConfig<C> {

    protected static final DateTimeFormat defaultDateTimeFormat = DateTimeFormat.LONG;

    protected static final boolean defaultPrettyFormat = false;

    protected static final boolean defaultWriteBigDecimalAsPlain = false;

    protected static final String defaultIndentation = "    ";

    char charQuotation = WD._QUOTATION_D;

    char stringQuotation = WD._QUOTATION_D;

    DateTimeFormat dateTimeFormat = defaultDateTimeFormat;

    boolean prettyFormat = defaultPrettyFormat;

    boolean writeLongAsString = false;
    boolean writeNullStringAsEmpty = false;
    boolean writeNullNumberAsZero = false;
    boolean writeNullBooleanAsFalse = false;

    boolean writeBigDecimalAsPlain = defaultWriteBigDecimalAsPlain;

    boolean failOnEmptyBean = true;
    boolean supportCircularReference = false;

    String indentation = defaultIndentation;

    NamingPolicy propNamingPolicy = null;

    /**
     * Gets the char quotation.
     *
     * @return
     */
    public char getCharQuotation() {
        return charQuotation;
    }

    /**
     * Sets the char quotation.
     *
     * @param charQuotation
     * @return
     */
    public C setCharQuotation(final char charQuotation) {
        if (charQuotation == 0 || charQuotation == WD._QUOTATION_S || charQuotation == WD._QUOTATION_D) {
            this.charQuotation = charQuotation;
        } else {
            throw new IllegalArgumentException("Only ''', '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    /**
     * Gets the string quotation.
     *
     * @return
     */
    public char getStringQuotation() {
        return stringQuotation;
    }

    /**
     * Sets the string quotation.
     *
     * @param stringQuotation
     * @return
     */
    public C setStringQuotation(final char stringQuotation) {
        if (stringQuotation == 0 || stringQuotation == WD._QUOTATION_S || stringQuotation == WD._QUOTATION_D) {
            this.stringQuotation = stringQuotation;
        } else {
            throw new IllegalArgumentException("Only '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public C noCharQuotation() {
        return setCharQuotation((char) 0);
    }

    @SuppressWarnings("UnusedReturnValue")
    public C noStringQuotation() {
        return setStringQuotation((char) 0);
    }

    @SuppressWarnings("UnusedReturnValue")
    public C noQuotation() {
        return setCharQuotation((char) 0).setStringQuotation((char) 0);
    }

    /**
     * The default format is: {@code LONG}.
     *
     * @return
     */
    public DateTimeFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Sets the date time format.
     *
     * @param dateTimeFormat
     * @return
     */
    public C setDateTimeFormat(final DateTimeFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;

        return (C) this;
    }

    /**
     * Checks if is pretty format.
     *
     * @return {@code true}, if is pretty format
     */
    public boolean prettyFormat() {
        return prettyFormat;
    }

    /**
     * Sets the pretty format.
     *
     * @param prettyFormat
     * @return
     */
    public C prettyFormat(final boolean prettyFormat) {
        this.prettyFormat = prettyFormat;

        return (C) this;
    }

    /**
     * Gets the indentation.
     *
     * @return
     */
    public String getIndentation() {
        return indentation;
    }

    /**
     * Sets the indentation.
     *
     * @param indentation
     * @return
     */
    public C setIndentation(final String indentation) {
        this.indentation = indentation;

        return (C) this;
    }

    /**
     * Gets the prop naming policy.
     *
     * @return
     */
    public NamingPolicy getPropNamingPolicy() {
        return propNamingPolicy;
    }

    /**
     * Sets the prop naming policy.
     *
     * @param propNamingPolicy
     * @return
     */
    public C setPropNamingPolicy(final NamingPolicy propNamingPolicy) {
        this.propNamingPolicy = propNamingPolicy;

        return (C) this;
    }

    public boolean writeLongAsString() {
        return writeLongAsString;
    }

    /**
     *
     * @param writeLongAsString
     * @return
     */
    public C writeLongAsString(final boolean writeLongAsString) {
        this.writeLongAsString = writeLongAsString;

        return (C) this;
    }

    public boolean writeNullStringAsEmpty() {
        return writeNullStringAsEmpty;
    }

    /**
     *
     * @param writeNullStringAsEmpty
     * @return
     */
    public C writeNullStringAsEmpty(final boolean writeNullStringAsEmpty) {
        this.writeNullStringAsEmpty = writeNullStringAsEmpty;

        return (C) this;
    }

    public boolean writeNullNumberAsZero() {
        return writeNullNumberAsZero;
    }

    /**
     *
     * @param writeNullNumberAsZero
     * @return
     */
    public C writeNullNumberAsZero(final boolean writeNullNumberAsZero) {
        this.writeNullNumberAsZero = writeNullNumberAsZero;

        return (C) this;
    }

    public boolean writeNullBooleanAsFalse() {
        return writeNullBooleanAsFalse;
    }

    /**
     *
     * @param writeNullBooleanAsFalse
     * @return
     */
    public C writeNullBooleanAsFalse(final boolean writeNullBooleanAsFalse) {
        this.writeNullBooleanAsFalse = writeNullBooleanAsFalse;

        return (C) this;
    }

    public boolean writeBigDecimalAsPlain() {
        return writeBigDecimalAsPlain;
    }

    /**
     *
     * @param writeBigDecimalAsPlain
     * @return
     */
    public C writeBigDecimalAsPlain(final boolean writeBigDecimalAsPlain) {
        this.writeBigDecimalAsPlain = writeBigDecimalAsPlain;

        return (C) this;
    }

    public boolean failOnEmptyBean() {
        return failOnEmptyBean;
    }

    /**
     *
     * @param failOnEmptyBean
     * @return
     */
    public C failOnEmptyBean(final boolean failOnEmptyBean) {
        this.failOnEmptyBean = failOnEmptyBean;

        return (C) this;
    }

    public boolean supportCircularReference() {
        return supportCircularReference;
    }

    /**
     *
     * @param supportCircularReference
     * @return
     */
    public C supportCircularReference(final boolean supportCircularReference) {
        this.supportCircularReference = supportCircularReference;

        return (C) this;
    }
}
