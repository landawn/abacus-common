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

/**
 * Configuration class for JSON serialization operations.
 * This class provides various settings to control how objects are serialized to JSON format.
 * All configuration methods support method chaining for convenient setup.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JSONSerializationConfig config = new JSONSerializationConfig()
 *     .quotePropName(true)
 *     .quoteMapKey(false)
 *     .prettyFormat(true)
 *     .setDateTimeFormat(DateTimeFormat.ISO_8601);
 * }</pre>
 *
 * <p>Default configuration values:</p>
 * <ul>
 *   <li>quotePropName: {@code true} - Property names are quoted by default</li>
 *   <li>quoteMapKey: {@code true} - Map keys are quoted by default</li>
 *   <li>bracketRootValue: {@code true} - Root values are bracketed by default</li>
 *   <li>wrapRootValue: {@code false} - Root values are not wrapped with type name by default</li>
 *   <li>writeNullToEmpty: {@code false} - Null values remain {@code null} by default</li>
 *   <li>writeDatasetByRow: {@code false} - Datasets are written by columns by default</li>
 * </ul>
 *
 * @see JSONParser
 * @see JSONDeserializationConfig
 */
public class JSONSerializationConfig extends JSONXMLSerializationConfig<JSONSerializationConfig> {

    protected static final boolean defaultQuotePropName = true;

    protected static final boolean defaultQuoteMapKey = true;

    protected static final boolean defaultBracketRootValue = true;

    protected static final boolean defaultWrapRootValue = false;

    private boolean writeNullToEmpty = false;
    private boolean writeDatasetByRow = false;
    private boolean writeRowColumnKeyType = false; // for Sheet;
    private boolean writeColumnType = false; // for Dataset and Sheet

    private boolean quotePropName = defaultQuotePropName;

    private boolean quoteMapKey = defaultQuoteMapKey;

    private boolean bracketRootValue = defaultBracketRootValue;

    private boolean wrapRootValue = defaultWrapRootValue;

    /**
     * Creates a new instance of JSONSerializationConfig with default settings.
     * 
     * <p>Default settings:</p>
     * <ul>
     *   <li>quotePropName: true</li>
     *   <li>quoteMapKey: true</li>
     *   <li>bracketRootValue: true</li>
     *   <li>wrapRootValue: false</li>
     *   <li>writeNullToEmpty: false</li>
     *   <li>writeDatasetByRow: false</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig();
     * String json = parser.serialize(object, config);
     * }</pre>
     */
    public JSONSerializationConfig() { //NOSONAR
    }

    /**
     * Checks if {@code null} values should be written as empty values.
     * When enabled, {@code null} values will be serialized as empty strings, empty arrays, or empty objects
     * depending on the expected type.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.writeNullToEmpty(true);
     * // null string → ""
     * // null array → []
     * // null object → {}
     * }</pre>
     * 
     * @return {@code true} if {@code null} values should be written as empty strings/arrays/objects, {@code false} otherwise
     */
    public boolean writeNullToEmpty() {
        return writeNullToEmpty;
    }

    /**
     * Sets whether {@code null} values should be written as empty values during serialization.
     * When enabled, {@code null} values will be serialized as empty strings, empty arrays, or empty objects
     * depending on the expected type. This is useful when the consuming system cannot handle {@code null} values.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .writeNullToEmpty(true);
     * 
     * class Person {
     *     String name = null;
     *     List<String> hobbies = null;
     * }
     * // Output: {"name": "", "hobbies": []}
     * }</pre>
     *
     * @param writeNullToEmpty {@code true} to write {@code null} as empty, {@code false} to write as null
     * @return this instance for method chaining
     */
    public JSONSerializationConfig writeNullToEmpty(final boolean writeNullToEmpty) {
        this.writeNullToEmpty = writeNullToEmpty;

        return this;
    }

    /**
     * Checks if Dataset should be written row by row.
     * When enabled, Dataset will be serialized as an array of row objects.
     * When disabled (default), Dataset will be serialized with column-based structure.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.writeDatasetByRow(true);
     * // Dataset will be serialized as: [{"col1": val1, "col2": val2}, ...]
     * }</pre>
     * 
     * @return {@code true} if Dataset is written by rows, {@code false} if by columns
     */
    public boolean writeDatasetByRow() {
        return writeDatasetByRow;
    }

    /**
     * Sets whether Dataset should be serialized row by row.
     * When enabled, Dataset will be serialized as an array of row objects where each object
     * represents a row with column names as keys. When disabled (default), Dataset will be 
     * serialized with a column-based structure.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .writeDatasetByRow(true);
     * 
     * // Row-based output: [{"id": 1, "name": "John"}, {"id": 2, "name": "Jane"}]
     * // Column-based output: {"id": [1, 2], "name": ["John", "Jane"]}
     * }</pre>
     *
     * @param writeDatasetByRow {@code true} to write by rows, {@code false} to write by columns
     * @return this instance for method chaining
     */
    public JSONSerializationConfig writeDatasetByRow(final boolean writeDatasetByRow) {
        this.writeDatasetByRow = writeDatasetByRow;

        return this;
    }

    /**
     * Checks if row and column key types should be written for Sheet objects.
     * This is useful for preserving type information during round-trip serialization.
     * 
     * @return {@code true} if key types should be written, {@code false} otherwise
     */
    public boolean writeRowColumnKeyType() {
        return writeRowColumnKeyType;
    }

    /**
     * Sets whether to include row and column key type information when serializing Sheet objects.
     * This is useful for preserving type information during round-trip serialization, allowing
     * proper deserialization of complex key types.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .writeRowColumnKeyType(true);
     * 
     * // Output will include type information for row/column keys
     * // Useful when keys are not simple strings
     * }</pre>
     *
     * @param writeRowColumnKeyType {@code true} to include key type information, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig writeRowColumnKeyType(final boolean writeRowColumnKeyType) {
        this.writeRowColumnKeyType = writeRowColumnKeyType;

        return this;
    }

    /**
     * Checks if column type information should be written for Dataset and Sheet objects.
     * When enabled, the type of each column will be included in the serialized output.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.writeColumnType(true);
     * // Output will include: "columnTypes": ["String", "Integer", "Date"]
     * }</pre>
     * 
     * @return {@code true} if column types should be written, {@code false} otherwise
     */
    public boolean writeColumnType() {
        return writeColumnType;
    }

    /**
     * Sets whether to include column type information when serializing Dataset and Sheet objects.
     * When enabled, the type of each column will be included in the serialized output, which
     * helps preserve type information for proper deserialization.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .writeColumnType(true);
     * 
     * // Output will include metadata:
     * // {
     * //   "columnTypes": ["String", "Integer", "Date"],
     * //   "data": {...}
     * // }
     * }</pre>
     *
     * @param writeColumnType {@code true} to include column type information, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig writeColumnType(final boolean writeColumnType) {
        this.writeColumnType = writeColumnType;

        return this;
    }

    /**
     * Sets the character quotation for char values.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * JSON specification requires double quotes for all string values.
     *
     * @param charQuotation the character to use for quoting char values
     * @return this instance for method chaining
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             JSON always uses double quotes for string values.
     */
    @Deprecated
    @Override
    public JSONSerializationConfig setCharQuotation(final char charQuotation) {
        super.setCharQuotation(charQuotation);

        return this;
    }

    /**
     * Sets the string quotation character.
     * Note: This method is deprecated as JSON requires double quotes for strings.
     * The JSON specification mandates the use of double quotes for all string values.
     *
     * @param stringQuotation the character to use for quoting strings
     * @return this instance for method chaining
     * @deprecated this method should not be called as JSON requires double quotes for strings.
     *             Calling this method with any value other than '"' may result in invalid JSON.
     */
    @Deprecated
    @Override
    public JSONSerializationConfig setStringQuotation(final char stringQuotation) {
        super.setStringQuotation(stringQuotation);

        return this;
    }

    /**
     * Disables character quotation.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * Characters in JSON must be represented as quoted strings.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             Disabling quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JSONSerializationConfig noCharQuotation() {
        super.noCharQuotation();

        return this;
    }

    /**
     * Disables string quotation.
     * Note: This method is deprecated as JSON requires quotes for strings.
     * The JSON specification requires all string values to be quoted.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as JSON requires quotes for strings.
     *             Disabling string quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JSONSerializationConfig noStringQuotation() {
        super.noStringQuotation();

        return this;
    }

    /**
     * Disables all quotation.
     * Note: This method is deprecated as JSON has specific quotation requirements.
     * JSON requires proper quotation for strings and property names.
     *
     * @return this instance for method chaining
     * @deprecated this method should not be called as JSON has specific quotation requirements.
     *             Disabling all quotation will result in invalid JSON output.
     */
    @Deprecated
    @Override
    public JSONSerializationConfig noQuotation() {
        super.noQuotation();

        return this;
    }

    /**
     * Checks if property names should be quoted in the JSON output.
     * The default value is {@code true} if not set, which conforms to the JSON specification.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.quotePropName(false);
     * // Output: {name: "John"} instead of {"name": "John"}
     * }</pre>
     *
     * @return {@code true} if property names should be quoted, {@code false} otherwise
     */
    public boolean quotePropName() {
        return quotePropName;
    }

    /**
     * Sets whether property names should be quoted in the JSON output.
     * According to strict JSON specification, property names should always be quoted.
     * Setting this to {@code false} produces non-standard JSON that may not be parseable by all JSON parsers.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .quotePropName(false);
     * 
     * // Standard JSON (quotePropName = true): {"name": "John", "age": 30}
     * // Non-standard (quotePropName = false): {name: "John", age: 30}
     * }</pre>
     *
     * @param quotePropName {@code true} to quote property names (standard), {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig quotePropName(final boolean quotePropName) {
        this.quotePropName = quotePropName;

        return this;
    }

    /**
     * Checks if map keys should be quoted in the JSON output.
     * The default value is {@code true} if not set.
     *
     * @return {@code true} if map keys should be quoted, {@code false} otherwise
     */
    public boolean quoteMapKey() {
        return quoteMapKey;
    }

    /**
     * Sets whether map keys should be quoted in the JSON output.
     * When serializing Map objects, this determines if the keys should be surrounded by quotes.
     * Note that setting this to {@code false} may produce non-standard JSON for non-string keys.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Integer, String> map = Map.of(1, "one", 2, "two");
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .quoteMapKey(false);
     * 
     * // Output with quoteMapKey(true): {"1": "one", "2": "two"}
     * // Output with quoteMapKey(false): {1: "one", 2: "two"}
     * }</pre>
     *
     * @param quoteMapKey {@code true} to quote map keys, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig quoteMapKey(final boolean quoteMapKey) {
        this.quoteMapKey = quoteMapKey;

        return this;
    }

    /**
     * Checks if the root value should be enclosed with brackets.
     * The default value is {@code true} if not set.
     *
     * @return {@code true} if root value should be bracketed, {@code false} otherwise
     */
    public boolean bracketRootValue() {
        return bracketRootValue;
    }

    /**
     * Sets whether to enclose the JSON string/text with '{' and '}' or '[' and ']'.
     * This determines if the root element should be wrapped with brackets when it's a 
     * single value that would not normally be bracketed.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .bracketRootValue(false);
     * 
     * // For a string value "test":
     * // With bracketRootValue(true): ["test"]
     * // With bracketRootValue(false): "test"
     * }</pre>
     *
     * @param bracketRootValue {@code true} to bracket root value, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig bracketRootValue(final boolean bracketRootValue) {
        this.bracketRootValue = bracketRootValue;

        return this;
    }

    /**
     * Checks if the root value should be wrapped with its type name.
     *
     * @return {@code true} if root value should be wrapped, {@code false} otherwise
     */
    public boolean wrapRootValue() {
        return wrapRootValue;
    }

    /**
     * Sets whether to wrap the root value with its type name as a property.
     * When enabled, the output will include the class name as a wrapper property.
     * This is useful for preserving type information in the JSON output.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * JSONSerializationConfig config = new JSONSerializationConfig()
     *     .wrapRootValue(true);
     * 
     * // Output with wrapRootValue(true): 
     * // {"Person": {"name": "John", "age": 30}}
     * // 
     * // Output with wrapRootValue(false): 
     * // {"name": "John", "age": 30}
     * }</pre>
     *
     * @param wrapRootValue {@code true} to wrap root value with type name, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONSerializationConfig wrapRootValue(final boolean wrapRootValue) {
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
        h = 31 * h + N.hashCode(skipTransientField());
        h = 31 * h + N.hashCode(prettyFormat());
        h = 31 * h + N.hashCode(writeLongAsString());
        h = 31 * h + N.hashCode(writeNullStringAsEmpty);
        h = 31 * h + N.hashCode(writeNullNumberAsZero);
        h = 31 * h + N.hashCode(writeNullBooleanAsFalse);
        h = 31 * h + N.hashCode(writeNullToEmpty);
        h = 31 * h + N.hashCode(writeDatasetByRow);
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
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if all their settings match.
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

        if (obj instanceof JSONSerializationConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(getCharQuotation(), other.getCharQuotation()) //NOSONAR
                    && N.equals(getStringQuotation(), other.getStringQuotation()) && N.equals(getDateTimeFormat(), other.getDateTimeFormat())
                    && N.equals(getExclusion(), other.getExclusion()) && N.equals(skipTransientField(), other.skipTransientField())
                    && N.equals(prettyFormat(), other.prettyFormat()) && N.equals(writeLongAsString(), other.writeLongAsString())
                    && N.equals(writeNullStringAsEmpty, other.writeNullStringAsEmpty) && N.equals(writeNullNumberAsZero, other.writeNullNumberAsZero)
                    && N.equals(writeNullBooleanAsFalse, other.writeNullBooleanAsFalse) && N.equals(writeBigDecimalAsPlain(), other.writeBigDecimalAsPlain())
                    && N.equals(failOnEmptyBean(), other.failOnEmptyBean()) && N.equals(supportCircularReference(), other.supportCircularReference())
                    && N.equals(getIndentation(), other.getIndentation()) && N.equals(getPropNamingPolicy(), other.getPropNamingPolicy())
                    && N.equals(writeNullToEmpty, other.writeNullToEmpty) && N.equals(writeDatasetByRow, other.writeDatasetByRow)
                    && N.equals(writeRowColumnKeyType, other.writeRowColumnKeyType) && N.equals(writeColumnType, other.writeColumnType)
                    && N.equals(quotePropName, other.quotePropName) && N.equals(quoteMapKey, other.quoteMapKey)
                    && N.equals(bracketRootValue, other.bracketRootValue) && N.equals(wrapRootValue, other.wrapRootValue);
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
                + ", skipTransientField=" + N.toString(skipTransientField()) + ", prettyFormat=" + N.toString(prettyFormat()) + ", writeNullStringAsEmpty="
                + N.toString(writeNullStringAsEmpty) + ", writeNullNumberAsZero=" + N.toString(writeNullNumberAsZero) + ", writeNullBooleanAsFalse="
                + N.toString(writeNullBooleanAsFalse) + ", writeNullToEmpty=" + N.toString(writeNullToEmpty) + ", writeDatasetByRow="
                + N.toString(writeDatasetByRow) + ", writeRowColumnKeyType=" + N.toString(writeRowColumnKeyType) + ", writeColumnType="
                + N.toString(writeColumnType) + ", writeBigDecimalAsPlain=" + N.toString(writeBigDecimalAsPlain()) + N.toString(failOnEmptyBean())
                + ", failOnEmptyBean=" + N.toString(failOnEmptyBean()) + ", supportCircularReference=" + N.toString(supportCircularReference())
                + ", indentation=" + N.toString(getIndentation()) + ", propNamingPolicy=" + N.toString(getPropNamingPolicy()) + ", quotePropName="
                + N.toString(quotePropName) + ", quoteMapKey=" + N.toString(quoteMapKey) + ", bracketRootValue=" + N.toString(bracketRootValue)
                + ", wrapRootValue=" + N.toString(wrapRootValue) + "}";
    }

    /**
     * Factory class for creating JSONSerializationConfig instances.
     * Provides convenient static factory methods for creating configurations.
     * This class cannot be instantiated and serves only as a container for factory methods.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig config = JSC.create()
     *     .quotePropName(true)
     *     .prettyFormat(true);
     * }</pre>
     * 
     * @see JSONSerializationConfig
     */
    public static final class JSC extends JSONSerializationConfig {

        /**
         * Constructs a new JSC instance.
         */
        public JSC() {
        }

        /**
         * Creates a new instance of JSONSerializationConfig with default settings.
         * This is the recommended way to create a new configuration instance.
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * JSONSerializationConfig config = JSC.create()
         *     .prettyFormat(true)
         *     .setDateTimeFormat(DateTimeFormat.ISO_8601);
         * }</pre>
         *
         * @return a new JSONSerializationConfig instance
         */
        public static JSONSerializationConfig create() {
            return new JSONSerializationConfig();
        }

        /**
         * Creates a new JSONSerializationConfig with specified quote settings.
         * This method allows quick configuration of property name and map key quoting behavior.
         *
         * @param quotePropName whether to quote property names
         * @param quoteMapKey whether to quote map keys
         * @return a new configured JSONSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         *             For example: {@code JSC.create().quotePropName(true).quoteMapKey(false)}
         */
        @Deprecated
        public static JSONSerializationConfig of(final boolean quotePropName, final boolean quoteMapKey) {
            return create().quotePropName(quotePropName).quoteMapKey(quoteMapKey);
        }

        /**
         * Creates a new JSONSerializationConfig with specified date time format.
         * This method provides a quick way to set the date/time formatting behavior.
         *
         * @param dateTimeFormat the date time format to use
         * @return a new configured JSONSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         *             For example: {@code JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601)}
         */
        @Deprecated
        public static JSONSerializationConfig of(final DateTimeFormat dateTimeFormat) {
            return create().setDateTimeFormat(dateTimeFormat);
        }

        /**
         * Creates a new JSONSerializationConfig with exclusion settings and ignored properties.
         * This method allows configuration of property exclusion behavior.
         *
         * @param exclusion the exclusion policy for properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         *             For example: {@code JSC.create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames)}
         */
        @Deprecated
        public static JSONSerializationConfig of(final Exclusion exclusion, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setExclusion(exclusion).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new JSONSerializationConfig with all specified settings.
         * This method allows complete configuration in a single call.
         *
         * @param quotePropName whether to quote property names
         * @param quoteMapKey whether to quote map keys
         * @param dateTimeFormat the date time format to use
         * @param exclusion the exclusion policy for properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONSerializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         *             For example: {@code JSC.create().quotePropName(true).quoteMapKey(false)...}
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
