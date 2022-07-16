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

import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <C>
 * @since 0.8
 */
public abstract class SerializationConfig<C extends SerializationConfig<C>> extends ParserConfig<C> {

    protected static final Exclusion defaultExclusion = Exclusion.NULL;

    protected static final DateTimeFormat defaultDateTimeFormat = DateTimeFormat.LONG;

    protected static final boolean defaultSkipTransientField = true;

    protected static final boolean defaultPrettyFormat = false;

    protected static final boolean defaultSupportCircularReference = false;

    protected static final boolean defaultWriteBigDecimalAsPlain = false;

    protected static final String defaultIndentation = "    ";

    char charQuotation = WD._QUOTATION_D;

    char stringQuotation = WD._QUOTATION_D;

    DateTimeFormat dateTimeFormat = defaultDateTimeFormat;

    Exclusion exclusion = defaultExclusion;

    boolean skipTransientField = defaultSkipTransientField;

    boolean prettyFormat = defaultPrettyFormat;

    boolean supportCircularReference = defaultSupportCircularReference;

    boolean writeBigDecimalAsPlain = defaultWriteBigDecimalAsPlain;

    String indentation = defaultIndentation;

    NamingPolicy propNamingPolicy = null;

    /**
     * Gets the exclusion.
     *
     * @return
     */
    public Exclusion getExclusion() {
        return exclusion;
    }

    /**
     * Sets the exclusion.
     *
     * @param exclusion
     * @return
     */
    public C setExclusion(Exclusion exclusion) {
        this.exclusion = exclusion;

        return (C) this;
    }

    /**
     * Checks if is skip transient field.
     *
     * @return true, if is skip transient field
     */
    public boolean isSkipTransientField() {
        return skipTransientField;
    }

    /**
     * Sets the skip transient field.
     *
     * @param skipTransientField
     * @return
     */
    public C setSkipTransientField(boolean skipTransientField) {
        this.skipTransientField = skipTransientField;

        return (C) this;
    }

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
    public C setCharQuotation(char charQuotation) {
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
    public C setStringQuotation(char stringQuotation) {
        if (stringQuotation == 0 || stringQuotation == WD._QUOTATION_S || stringQuotation == WD._QUOTATION_D) {
            this.stringQuotation = stringQuotation;
        } else {
            throw new IllegalArgumentException("Only '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    public C noCharQuotation() {
        return setCharQuotation((char) 0);
    }

    public C noStringQuotation() {
        return setStringQuotation((char) 0);
    }

    public C noQuotation() {
        return setCharQuotation((char) 0).setStringQuotation((char) 0);
    }

    /**
     * The default format is: <code>LONG</code>.
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
    public C setDateTimeFormat(DateTimeFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;

        return (C) this;
    }

    /**
     * Checks if is pretty format.
     *
     * @return true, if is pretty format
     */
    public boolean isPrettyFormat() {
        return prettyFormat;
    }

    /**
     * Sets the pretty format.
     *
     * @param prettyFormat
     * @return
     */
    public C setPrettyFormat(boolean prettyFormat) {
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
    public C setIndentation(String indentation) {
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
    public C setPropNamingPolicy(NamingPolicy propNamingPolicy) {
        this.propNamingPolicy = propNamingPolicy;

        return (C) this;
    }

    /**
     * Support circular reference.
     *
     * @return true, if successful
     */
    public boolean supportCircularReference() {
        return supportCircularReference;
    }

    /**
     * Support circular reference.
     *
     * @param supportCircularReference
     * @return
     */
    public C supportCircularReference(boolean supportCircularReference) {
        this.supportCircularReference = supportCircularReference;

        return (C) this;
    }

    /**
     *
     * @return
     */
    public boolean writeBigDecimalAsPlain() {
        return writeBigDecimalAsPlain;
    }

    /**
     *
     * @param writeBigDecimalAsPlain
     * @return
     */
    public C writeBigDecimalAsPlain(boolean writeBigDecimalAsPlain) {
        this.writeBigDecimalAsPlain = writeBigDecimalAsPlain;

        return (C) this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(charQuotation);
        h = 31 * h + N.hashCode(stringQuotation);
        h = 31 * h + N.hashCode(dateTimeFormat);
        h = 31 * h + N.hashCode(exclusion);
        h = 31 * h + N.hashCode(skipTransientField);
        h = 31 * h + N.hashCode(prettyFormat);
        h = 31 * h + N.hashCode(supportCircularReference);
        h = 31 * h + N.hashCode(writeBigDecimalAsPlain);
        h = 31 * h + N.hashCode(indentation);
        return 31 * h + N.hashCode(propNamingPolicy);
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

        if (obj instanceof SerializationConfig) {
            SerializationConfig<C> other = (SerializationConfig<C>) obj;

            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(charQuotation, other.charQuotation)
                    && N.equals(stringQuotation, other.stringQuotation) && N.equals(dateTimeFormat, other.dateTimeFormat)
                    && N.equals(exclusion, other.exclusion) && N.equals(skipTransientField, other.skipTransientField)
                    && N.equals(prettyFormat, other.prettyFormat) && N.equals(supportCircularReference, other.supportCircularReference)
                    && N.equals(writeBigDecimalAsPlain, other.writeBigDecimalAsPlain) && N.equals(indentation, other.indentation)
                    && N.equals(propNamingPolicy, other.propNamingPolicy)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + N.toString(charQuotation) + ", stringQuotation="
                + N.toString(stringQuotation) + ", dateTimeFormat=" + N.toString(dateTimeFormat) + ", exclusion=" + N.toString(exclusion)
                + ", skipTransientField=" + N.toString(skipTransientField) + ", prettyFormat=" + N.toString(prettyFormat) + ", supportCircularReference="
                + N.toString(supportCircularReference) + ", writeBigDecimalAsPlain=" + N.toString(writeBigDecimalAsPlain) + ", indentation="
                + N.toString(indentation) + ", propNamingPolicy=" + N.toString(propNamingPolicy) + "}";
    }
}
