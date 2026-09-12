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

/**
 * Configuration class for JSON serialization operations.
 * This class provides various settings to control how objects are serialized to JSON format.
 * All configuration methods support method chaining for convenient setup.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JsonSerConfig config = new JsonSerConfig()
 *     .setQuotePropName(true)
 *     .setQuoteMapKey(false)
 *     .setPrettyFormat(true)
 *     .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
 * }</pre>
 *
 * <p>Default configuration values:</p>
 * <ul>
 *   <li>quotePropName: {@code true} - Property names are quoted by default</li>
 *   <li>quoteMapKey: {@code true} - Map keys are quoted by default</li>
 *   <li>bracketRootValue: {@code true} - Root values are bracketed by default</li>
 *   <li>wrapRootValue: {@code false} - Root values are not wrapped with type name by default</li>
 *   <li>writeNullToEmpty: {@code false} - {@code null} values remain {@code null} by default</li>
 *   <li>writeDatasetAsRows: {@code false} - Datasets are written by columns by default</li>
 *   <li>writeRowColumnKeyType: {@code false} - Sheet row/column key types are not written by default</li>
 *   <li>writeColumnType: {@code false} - Dataset/Sheet column types are not written by default</li>
 * </ul>
 *
 * @see JsonParser
 * @see JsonDeserConfig
 */
public class JsonSerConfig extends JsonXmlSerConfig<JsonSerConfig> {

    /** Default value for {@link #isQuotePropName()} ({@code true}). */
    protected static final boolean defaultQuotePropName = true;

    /** Default value for {@link #isQuoteMapKey()} ({@code true}). */
    protected static final boolean defaultQuoteMapKey = true;

    /** Default value for {@link #isBracketRootValue()} ({@code true}). */
    protected static final boolean defaultBracketRootValue = true;

    /** Default value for {@link #isWrapRootValue()} ({@code false}). */
    protected static final boolean defaultWrapRootValue = false;

    private boolean writeNullToEmpty = false;
    private boolean writeDatasetAsRows = false;
    private boolean writeRowColumnKeyType = false; // for Sheet;
    private boolean writeColumnType = false; // for Dataset and Sheet

    private boolean quotePropName = defaultQuotePropName;

    private boolean quoteMapKey = defaultQuoteMapKey;

    private boolean bracketRootValue = defaultBracketRootValue;

    private boolean wrapRootValue = defaultWrapRootValue;

    /**
     * Creates a new instance of JsonSerConfig with default settings.
     *
     * <p>Default settings:</p>
     * <ul>
     *   <li>quotePropName: {@code true}</li>
     *   <li>quoteMapKey: {@code true}</li>
     *   <li>bracketRootValue: {@code true}</li>
     *   <li>wrapRootValue: {@code false}</li>
     *   <li>writeNullToEmpty: {@code false}</li>
     *   <li>writeDatasetAsRows: {@code false}</li>
     *   <li>writeRowColumnKeyType: {@code false}</li>
     *   <li>writeColumnType: {@code false}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * String json = parser.serialize(object, config);
     * }</pre>
     *
     */
    public JsonSerConfig() { //NOSONAR
    }

    /**
     * Checks if {@code null} bean properties should be written as empty values.
     *
     * <p>See {@link #setWriteNullToEmpty(boolean)} for the exact scope: only bean properties that survive
     * the exclusion strategy are affected, and only {@code CharSequence}, collection/array and {@code Map}
     * properties get an empty value; other {@code null} properties are still written as {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullToEmpty();                  // returns false (default)
     * config.setWriteNullToEmpty(true);
     * config.isWriteNullToEmpty();                  // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} CharSequence/collection/array/Map bean properties should be written as {@code ""}/{@code []}/<code>{}</code>, {@code false} otherwise
     */
    public boolean isWriteNullToEmpty() {
        return writeNullToEmpty;
    }

    /**
     * Sets whether {@code null} bean properties should be written as empty values during serialization.
     *
     * <p>Only bean properties are affected, and only when they survive the exclusion strategy: the default
     * exclusion ({@link Exclusion#NULL}, applied when no exclusion is set on the config or on the bean's
     * {@code @JsonXmlConfig}) drops {@code null} properties before this flag is consulted, so the flag has
     * no visible effect unless {@link #setExclusion(Exclusion)} is set to {@link Exclusion#NONE}.
     * For a surviving {@code null} property, a {@code CharSequence} is written as {@code ""}, a
     * collection/array as {@code []}, and a {@code Map} as <code>{}</code>; any other {@code null}
     * (numbers, booleans, dates, nested beans, ...) is left to the other flags and, with all of them off, is
     * written as {@code null} - a nested bean is NOT written as <code>{}</code>. {@code null} map values and
     * {@code null} collection elements are not affected by this flag (see
     * {@link #setWriteNullStringAsEmpty(boolean)} and friends for typed elements).
     * This flag only wins over {@link #setWriteNullStringAsEmpty(boolean)}, for the slots that have an empty
     * form: a surviving {@code null} {@code CharSequence}/collection/array/{@code Map} property is emptied even
     * when that flag is off. A {@code null} number or boolean has no empty form, so
     * {@link #setWriteNullNumberAsZero(boolean)} / {@link #setWriteNullBooleanAsFalse(boolean)} still apply to
     * such a property when they are on, writing {@code 0} / {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setWriteNullToEmpty(true);
     * config.isWriteNullToEmpty();                  // returns true
     * config.setWriteNullToEmpty(false);
     * config.isWriteNullToEmpty();                  // returns false
     *
     * // bean with String s = null, List<String> l = null, Map<String, Integer> m = null, Integer i = null, Address a = null:
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullToEmpty(true));
     * // -> {}                                                          (default Exclusion.NULL drops them first)
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullToEmpty(true).setExclusion(Exclusion.NONE));
     * // -> {"s": "", "l": [], "m": {}, "i": null, "a": null}          (number and nested bean stay null)
     * }</pre>
     *
     * @param writeNullToEmpty {@code true} to write surviving {@code null} CharSequence/collection/array/Map bean properties as {@code ""}/{@code []}/<code>{}</code>, {@code false} to write them as {@code null}
     * @return {@code this} instance for method chaining
     * @see #setExclusion(Exclusion)
     */
    public JsonSerConfig setWriteNullToEmpty(final boolean writeNullToEmpty) {
        this.writeNullToEmpty = writeNullToEmpty;

        return this;
    }

    /**
     * Checks if Dataset should be written as rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteDatasetAsRows();                // returns false (default, writes by columns)
     * config.setWriteDatasetAsRows(true);
     * config.isWriteDatasetAsRows();                // returns true
     * }</pre>
     *
     * @return {@code true} if Dataset is written by rows, {@code false} if by columns
     */
    public boolean isWriteDatasetAsRows() {
        return writeDatasetAsRows;
    }

    /**
     * Sets whether Dataset should be serialized as rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setWriteDatasetAsRows(true);
     * config.isWriteDatasetAsRows();                // returns true
     * config.setWriteDatasetAsRows(false);
     * config.isWriteDatasetAsRows();                // returns false
     * }</pre>
     *
     * @param writeDatasetAsRows {@code true} to write as rows, {@code false} to write by columns
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setWriteDatasetAsRows(final boolean writeDatasetAsRows) {
        this.writeDatasetAsRows = writeDatasetAsRows;

        return this;
    }

    /**
     * Checks if row and column key types should be written for Sheet objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteRowColumnKeyType();             // returns false (default)
     * config.setWriteRowColumnKeyType(true);
     * config.isWriteRowColumnKeyType();             // returns true
     * }</pre>
     *
     * @return {@code true} if key types should be written, {@code false} otherwise
     */
    public boolean isWriteRowColumnKeyType() {
        return writeRowColumnKeyType;
    }

    /**
     * Sets whether to include row and column key type information when serializing Sheet objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setWriteRowColumnKeyType(true);
     * config.isWriteRowColumnKeyType();             // returns true
     * config.setWriteRowColumnKeyType(false);
     * config.isWriteRowColumnKeyType();             // returns false
     * }</pre>
     *
     * @param writeRowColumnKeyType {@code true} to include key type information, {@code false} otherwise
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setWriteRowColumnKeyType(final boolean writeRowColumnKeyType) {
        this.writeRowColumnKeyType = writeRowColumnKeyType;

        return this;
    }

    /**
     * Checks if column type information should be written for Dataset and Sheet objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteColumnType();                   // returns false (default)
     * config.setWriteColumnType(true);
     * config.isWriteColumnType();                   // returns true
     * }</pre>
     *
     * @return {@code true} if column types should be written, {@code false} otherwise
     */
    public boolean isWriteColumnType() {
        return writeColumnType;
    }

    /**
     * Sets whether to include column type information when serializing Dataset and Sheet objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setWriteColumnType(true);
     * config.isWriteColumnType();                   // returns true
     * config.setWriteColumnType(false);
     * config.isWriteColumnType();                   // returns false
     * }</pre>
     *
     * @param writeColumnType {@code true} to include column type information, {@code false} otherwise
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setWriteColumnType(final boolean writeColumnType) {
        this.writeColumnType = writeColumnType;

        return this;
    }

    /**
     * Sets the character quotation for char values.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * JSON specification requires double quotes for all string and character values.
     *
     * @param charQuotation the character to use for quoting char values
     * @return {@code this} instance for method chaining
     * @throws IllegalArgumentException if the quotation character is neither a single quote, a double quote, nor zero.
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             JSON always uses double quotes for string and character values.
     */
    @Deprecated
    @Override
    public JsonSerConfig setCharQuotation(final char charQuotation) throws IllegalArgumentException {
        super.setCharQuotation(charQuotation);

        return this;
    }

    /**
     * Sets the string quotation character.
     * Note: This method is deprecated as JSON requires double quotes for strings.
     * The JSON specification mandates the use of double quotes for all string values.
     *
     * @param stringQuotation the character to use for quoting strings
     * @return {@code this} instance for method chaining
     * @throws IllegalArgumentException if the quotation character is neither a single quote, a double quote, nor zero.
     * @deprecated this method should not be called as JSON requires double quotes for strings.
     *             Calling this method with any value other than '"' may result in invalid JSON.
     */
    @Deprecated
    @Override
    public JsonSerConfig setStringQuotation(final char stringQuotation) throws IllegalArgumentException {
        super.setStringQuotation(stringQuotation);

        return this;
    }

    /**
     * Disables character quotation.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * Characters in JSON must be represented as quoted strings.
     *
     * @return {@code this} instance for method chaining
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             Disabling quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JsonSerConfig noCharQuotation() {
        super.noCharQuotation();

        return this;
    }

    /**
     * Disables string quotation.
     * Note: This method is deprecated as JSON requires quotes for strings.
     * The JSON specification requires all string values to be quoted.
     *
     * @return {@code this} instance for method chaining
     * @deprecated this method should not be called as JSON requires quotes for strings.
     *             Disabling string quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JsonSerConfig noStringQuotation() {
        super.noStringQuotation();

        return this;
    }

    /**
     * Disables all quotation.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * JSON requires proper quotation for strings and property names.
     *
     * @return {@code this} instance for method chaining
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             Disabling all quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JsonSerConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * Checks if property names should be quoted in the JSON output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isQuotePropName();                     // returns true (default)
     * config.setQuotePropName(false);
     * config.isQuotePropName();                     // returns false
     * }</pre>
     *
     * @return {@code true} if property names should be quoted, {@code false} otherwise
     */
    public boolean isQuotePropName() {
        return quotePropName;
    }

    /**
     * Sets whether property names should be quoted in the JSON output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setQuotePropName(false);
     * config.isQuotePropName();                     // returns false
     * config.setQuotePropName(true);
     * config.isQuotePropName();                     // returns true
     * }</pre>
     *
     * @param quotePropName {@code true} to quote property names (standard), {@code false} otherwise
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setQuotePropName(final boolean quotePropName) {
        this.quotePropName = quotePropName;

        return this;
    }

    /**
     * Checks if all map keys should be quoted in the JSON output.
     *
     * <p>See {@link #setQuoteMapKey(boolean)}: {@code false} only un-quotes numeric, boolean and {@code null}
     * keys; {@link String} keys are always quoted.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isQuoteMapKey();                       // returns true (default)
     * config.setQuoteMapKey(false);
     * config.isQuoteMapKey();                       // returns false
     * }</pre>
     *
     * @return {@code true} if every map key is quoted, {@code false} if numeric/boolean/{@code null} keys are written bare
     */
    public boolean isQuoteMapKey() {
        return quoteMapKey;
    }

    /**
     * Sets whether map keys should be quoted in the JSON output.
     *
     * <p>When {@code false}, numeric and boolean map keys (and a {@code null} key) are written bare, e.g.
     * <code>{1: "a", true: "b", null: "c"}</code>. {@link String} keys are <b>always</b> written quoted so
     * the output stays parseable - unlike {@link #setQuotePropName(boolean)}, which un-quotes every bean
     * property name. The default {@code true} quotes every key.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setQuoteMapKey(false);
     * config.isQuoteMapKey();                       // returns false
     * config.setQuoteMapKey(true);
     * config.isQuoteMapKey();                       // returns true
     *
     * // map {"str" -> 1, 2 -> 2, true -> 3, null -> 4}
     * jsonParser.serialize(map, new JsonSerConfig().setQuoteMapKey(false));   // {"str": 1, 2: 2, true: 3, null: 4}
     * jsonParser.serialize(map, new JsonSerConfig().setQuoteMapKey(true));    // {"str": 1, "2": 2, "true": 3, "null": 4}
     * }</pre>
     *
     * @param quoteMapKey {@code true} to quote every map key (default), {@code false} to write numeric, boolean and {@code null} keys bare (String keys stay quoted)
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setQuoteMapKey(final boolean quoteMapKey) {
        this.quoteMapKey = quoteMapKey;

        return this;
    }

    /**
     * Checks if the root value should be enclosed with brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isBracketRootValue();                  // returns true (default)
     * config.setBracketRootValue(false);
     * config.isBracketRootValue();                  // returns false
     * }</pre>
     *
     * @return {@code true} if root value should be bracketed, {@code false} otherwise
     */
    public boolean isBracketRootValue() {
        return bracketRootValue;
    }

    /**
     * Sets whether the root value should be enclosed with the appropriate bracket characters
     * ({@code {}} for objects/maps, {@code []} for arrays/collections).
     * When {@code false}, the root-level brackets are omitted, producing an unwrapped value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setBracketRootValue(false);
     * config.isBracketRootValue();                  // returns false
     * config.setBracketRootValue(true);
     * config.isBracketRootValue();                  // returns true
     * }</pre>
     *
     * @param bracketRootValue {@code true} to enclose the root value with brackets (default),
     *        {@code false} to omit the enclosing brackets
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setBracketRootValue(final boolean bracketRootValue) {
        this.bracketRootValue = bracketRootValue;

        return this;
    }

    /**
     * Checks if the root value should be wrapped with its type name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWrapRootValue();                     // returns false (default)
     * config.setWrapRootValue(true);
     * config.isWrapRootValue();                     // returns true
     * }</pre>
     *
     * @return {@code true} if root value should be wrapped, {@code false} otherwise
     */
    public boolean isWrapRootValue() {
        return wrapRootValue;
    }

    /**
     * Sets whether to wrap the root value with its type name as a property.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig().setWrapRootValue(true);
     * config.isWrapRootValue();                     // returns true
     * config.setWrapRootValue(false);
     * config.isWrapRootValue();                     // returns false
     * }</pre>
     *
     * @param wrapRootValue {@code true} to wrap root value with type name, {@code false} otherwise
     * @return {@code this} instance for method chaining
     */
    public JsonSerConfig setWrapRootValue(final boolean wrapRootValue) {
        this.wrapRootValue = wrapRootValue;

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
        h = 31 * h + N.hashCode(writeNullToEmpty);
        h = 31 * h + N.hashCode(writeDatasetAsRows);
        h = 31 * h + N.hashCode(writeRowColumnKeyType);
        h = 31 * h + N.hashCode(writeColumnType);
        h = 31 * h + N.hashCode(isWriteBigDecimalAsPlain());
        h = 31 * h + N.hashCode(isFailOnEmptyBean());
        h = 31 * h + N.hashCode(isCircularReferenceSupported());
        h = 31 * h + N.hashCode(getIndentation());
        h = 31 * h + N.hashCode(getPropNamingPolicy());
        h = 31 * h + N.hashCode(quotePropName);
        h = 31 * h + N.hashCode(quoteMapKey);
        h = 31 * h + N.hashCode(bracketRootValue);
        return 31 * h + N.hashCode(wrapRootValue);
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if they are of exactly the same class and all their settings match.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        // Require exact same class to keep equals symmetric, the same rule SerializationConfig#equals applies: a
        // subclass narrowing equality with its own `instanceof <OwnType>` test would otherwise be equal to a plain
        // JsonSerConfig in one direction only. Mirrors AvroSerConfig/KryoSerConfig, which get it via super.equals.
        if (obj != null && obj.getClass() == getClass()) {
            final JsonSerConfig other = (JsonSerConfig) obj;

            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(isSkipTransientField(), other.isSkipTransientField())
                    && N.equals(isPrettyFormat(), other.isPrettyFormat()) && N.equals(isWriteLongAsString(), other.isWriteLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse)
                    && N.equals(isWriteBigDecimalAsPlain(), other.isWriteBigDecimalAsPlain()) && N.equals(isFailOnEmptyBean(), other.isFailOnEmptyBean())
                    && N.equals(isCircularReferenceSupported(), other.isCircularReferenceSupported()) && N.equals(getIndentation(), other.getIndentation())
                    && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy()) && N.equals(writeNullToEmpty, other.writeNullToEmpty)
                    && N.equals(writeDatasetAsRows, other.writeDatasetAsRows) && N.equals(writeRowColumnKeyType, other.writeRowColumnKeyType)
                    && N.equals(writeColumnType, other.writeColumnType) && N.equals(quotePropName, other.quotePropName)
                    && N.equals(quoteMapKey, other.quoteMapKey) && N.equals(bracketRootValue, other.bracketRootValue)
                    && N.equals(wrapRootValue, other.wrapRootValue);
        }

        return false;
    }

    /**
     * Returns a string representation of this configuration object.
     * The string contains all configuration settings in a readable format.
     * A disabled quotation (char {@code 0}) is rendered as the text <code>&#92;u0000</code>, never as a raw NUL character.
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", charQuotation=" + quotationToString(getCharQuotation()) + ", stringQuotation="
                + quotationToString(getStringQuotation()) + ", dateTimeFormat=" + N.toString(getDateTimeFormat()) + ", exclusion=" + N.toString(getExclusion())
                + ", skipTransientField=" + N.toString(isSkipTransientField()) + ", prettyFormat=" + N.toString(isPrettyFormat()) + ", writeLongAsString="
                + N.toString(isWriteLongAsString()) + ", writeNullStringAsEmpty=" + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero="
                + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse=" + N.toString(writeNullBooleanAsFalse) + ", writeNullToEmpty="
                + N.toString(writeNullToEmpty) + ", writeDatasetAsRows=" + N.toString(writeDatasetAsRows) + ", writeRowColumnKeyType="
                + N.toString(writeRowColumnKeyType) + ", writeColumnType=" + N.toString(writeColumnType) + ", writeBigDecimalAsPlain="
                + N.toString(isWriteBigDecimalAsPlain()) + ", failOnEmptyBean=" + N.toString(isFailOnEmptyBean()) + ", circularReferenceSupported="
                + N.toString(isCircularReferenceSupported()) + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy="
                + N.toString(getPropNamingPolicy()) + ", quotePropName=" + N.toString(quotePropName) + ", quoteMapKey=" + N.toString(quoteMapKey)
                + ", bracketRootValue=" + N.toString(bracketRootValue) + ", wrapRootValue=" + N.toString(wrapRootValue) + "}";
    }

    /**
     * Creates a new instance of JsonSerConfig with default settings.
     * This is the recommended way to create a new configuration instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = JsonSerConfig.create()
     *     .setPrettyFormat(true)
     *     .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
     * }</pre>
     *
     * @return a new JsonSerConfig instance
     */
    public static JsonSerConfig create() {
        return new JsonSerConfig();
    }

}
