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
 * Base configuration class for JSON and XML serialization operations.
 * This abstract class provides common configuration options that are shared between
 * JSON and XML serialization configurations.
 * 
 * <p>This class supports method chaining for easy configuration setup:</p>
 * <pre>{@code
 * config.prettyFormat(true)
 *       .setDateTimeFormat(DateTimeFormat.ISO_8601)
 *       .setIndentation("  ");
 * }</pre>
 * 
 * <p>Common configuration options include:</p>
 * <ul>
 *   <li>Character and string quotation settings</li>
 *   <li>Date/time formatting</li>
 *   <li>Pretty printing with customizable indentation</li>
 *   <li>Number and null value handling</li>
 *   <li>Property naming policies</li>
 *   <li>Support for circular references</li>
 * </ul>
 *
 * @param <C> the concrete configuration type for method chaining
 * @since 0.8
 * @see JSONSerializationConfig
 * @see XMLSerializationConfig
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
     * Gets the character used for quoting char values.
     *
     * @return the char quotation character
     */
    public char getCharQuotation() {
        return charQuotation;
    }

    /**
     * Sets the character to use for quoting char values.
     * Supported values are single quote ('), double quote ("), or 0 (no quotation).
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.setCharQuotation('\'');  // Use single quotes
     * config.setCharQuotation('"');   // Use double quotes
     * config.setCharQuotation((char)0); // No quotes
     * }</pre>
     *
     * @param charQuotation the character to use (', ", or 0)
     * @return this instance for method chaining
     * @throws IllegalArgumentException if an unsupported character is provided
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
     * Gets the character used for quoting string values.
     *
     * @return the string quotation character
     */
    public char getStringQuotation() {
        return stringQuotation;
    }

    /**
     * Sets the character to use for quoting string values.
     * Supported values are single quote ('), double quote ("), or 0 (no quotation).
     * Note that JSON standard requires double quotes for strings.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.setStringQuotation('"');   // Standard JSON
     * config.setStringQuotation('\'');  // Single quotes (non-standard)
     * config.setStringQuotation((char)0); // No quotes (non-standard)
     * }</pre>
     *
     * @param stringQuotation the character to use (', ", or 0)
     * @return this instance for method chaining
     * @throws IllegalArgumentException if an unsupported character is provided
     */
    public C setStringQuotation(final char stringQuotation) {
        if (stringQuotation == 0 || stringQuotation == WD._QUOTATION_S || stringQuotation == WD._QUOTATION_D) {
            this.stringQuotation = stringQuotation;
        } else {
            throw new IllegalArgumentException("Only '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    /**
     * Disables character quotation by setting the quotation character to 0.
     * Characters will be serialized without surrounding quotes.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.noCharQuotation();
     * // Character 'A' will be serialized as: A
     * }</pre>
     *
     * @return this instance for method chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public C noCharQuotation() {
        return setCharQuotation((char) 0);
    }

    /**
     * Disables string quotation by setting the quotation character to 0.
     * Strings will be serialized without surrounding quotes.
     * Warning: This produces non-standard JSON/XML.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.noStringQuotation();
     * // String "hello" will be serialized as: hello
     * }</pre>
     *
     * @return this instance for method chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public C noStringQuotation() {
        return setStringQuotation((char) 0);
    }

    /**
     * Disables both character and string quotation.
     * All string and character values will be serialized without quotes.
     * Warning: This produces non-standard JSON/XML.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.noQuotation();
     * // All strings and chars will be unquoted
     * }</pre>
     *
     * @return this instance for method chaining
     */
    @SuppressWarnings("UnusedReturnValue")
    public C noQuotation() {
        return setCharQuotation((char) 0).setStringQuotation((char) 0);
    }

    /**
     * Gets the date time format used for serializing date/time values.
     * The default format is {@link DateTimeFormat#LONG}.
     *
     * @return the current date time format
     */
    public DateTimeFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Sets the date time format for serializing date/time values.
     * This affects how Date, Calendar, and other temporal objects are formatted.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.setDateTimeFormat(DateTimeFormat.ISO_8601);
     * // Dates will be serialized as "2023-12-25T10:30:00Z"
     * 
     * config.setDateTimeFormat(DateTimeFormat.LONG);
     * // Dates will be serialized as milliseconds: 1703502600000
     * }</pre>
     *
     * @param dateTimeFormat the date time format to use
     * @return this instance for method chaining
     */
    public C setDateTimeFormat(final DateTimeFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;

        return (C) this;
    }

    /**
     * Checks if pretty formatting is enabled.
     * When enabled, the output will include line breaks and indentation for better readability.
     *
     * @return {@code true} if pretty format is enabled, {@code false} otherwise
     */
    public boolean prettyFormat() {
        return prettyFormat;
    }

    /**
     * Sets whether to enable pretty formatting.
     * When enabled, the output will be formatted with line breaks and indentation,
     * making it more human-readable but larger in size.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.prettyFormat(true).setIndentation("  ");
     * // Output will be formatted:
     * // {
     * //   "name": "John",
     * //   "age": 30
     * // }
     * }</pre>
     *
     * @param prettyFormat {@code true} to enable pretty formatting, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C prettyFormat(final boolean prettyFormat) {
        this.prettyFormat = prettyFormat;

        return (C) this;
    }

    /**
     * Gets the indentation string used for pretty formatting.
     * Default is four spaces ("    ").
     *
     * @return the indentation string
     */
    public String getIndentation() {
        return indentation;
    }

    /**
     * Sets the indentation string used for pretty formatting.
     * This is only used when pretty formatting is enabled.
     * Common values are spaces or tabs.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.prettyFormat(true).setIndentation("\t");  // Use tabs
     * config.prettyFormat(true).setIndentation("  ");  // Use 2 spaces
     * config.prettyFormat(true).setIndentation("    "); // Use 4 spaces (default)
     * }</pre>
     *
     * @param indentation the indentation string to use
     * @return this instance for method chaining
     */
    public C setIndentation(final String indentation) {
        this.indentation = indentation;

        return (C) this;
    }

    /**
     * Gets the property naming policy used during serialization.
     *
     * @return the property naming policy, or null if using default naming
     */
    public NamingPolicy getPropNamingPolicy() {
        return propNamingPolicy;
    }

    /**
     * Sets the property naming policy for serialization.
     * This determines how property names are transformed in the output.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.setPropNamingPolicy(NamingPolicy.LOWER_CAMEL_CASE);
     * // Property "firstName" remains "firstName"
     * 
     * config.setPropNamingPolicy(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // Property "firstName" becomes "first_name"
     * 
     * config.setPropNamingPolicy(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
     * // Property "firstName" becomes "FIRST_NAME"
     * }</pre>
     *
     * @param propNamingPolicy the naming policy to use
     * @return this instance for method chaining
     */
    public C setPropNamingPolicy(final NamingPolicy propNamingPolicy) {
        this.propNamingPolicy = propNamingPolicy;

        return (C) this;
    }

    /**
     * Checks if long values should be written as strings.
     *
     * @return {@code true} if longs are written as strings, {@code false} otherwise
     */
    public boolean writeLongAsString() {
        return writeLongAsString;
    }

    /**
     * Sets whether to write long values as strings.
     * This is useful for JavaScript compatibility where large numbers may lose precision.
     * JavaScript can only safely represent integers up to 2^53-1.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.writeLongAsString(true);
     * // Long value 9007199254740993L will be written as "9007199254740993"
     * // This prevents precision loss in JavaScript
     * }</pre>
     *
     * @param writeLongAsString {@code true} to write longs as strings, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C writeLongAsString(final boolean writeLongAsString) {
        this.writeLongAsString = writeLongAsString;

        return (C) this;
    }

    /**
     * Checks if null strings should be written as empty strings.
     *
     * @return {@code true} if null strings are written as empty, {@code false} otherwise
     */
    public boolean writeNullStringAsEmpty() {
        return writeNullStringAsEmpty;
    }

    /**
     * Sets whether to write null string values as empty strings.
     * When enabled, null string properties will be serialized as "" instead of null.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.writeNullStringAsEmpty(true);
     * // Object: {name: null} 
     * // Output: {"name": ""} instead of {"name": null}
     * }</pre>
     *
     * @param writeNullStringAsEmpty {@code true} to write null as empty string, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C writeNullStringAsEmpty(final boolean writeNullStringAsEmpty) {
        this.writeNullStringAsEmpty = writeNullStringAsEmpty;

        return (C) this;
    }

    /**
     * Checks if null numbers should be written as zero.
     *
     * @return {@code true} if null numbers are written as zero, {@code false} otherwise
     */
    public boolean writeNullNumberAsZero() {
        return writeNullNumberAsZero;
    }

    /**
     * Sets whether to write null number values as zero.
     * When enabled, null numeric properties will be serialized as 0 (or 0.0 for decimals).
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.writeNullNumberAsZero(true);
     * // Object: {count: null, price: null}
     * // Output: {"count": 0, "price": 0.0}
     * }</pre>
     *
     * @param writeNullNumberAsZero {@code true} to write null as zero, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C writeNullNumberAsZero(final boolean writeNullNumberAsZero) {
        this.writeNullNumberAsZero = writeNullNumberAsZero;

        return (C) this;
    }

    /**
     * Checks if null booleans should be written as false.
     *
     * @return {@code true} if null booleans are written as false, {@code false} otherwise
     */
    public boolean writeNullBooleanAsFalse() {
        return writeNullBooleanAsFalse;
    }

    /**
     * Sets whether to write null boolean values as false.
     * When enabled, null Boolean properties will be serialized as false instead of null.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.writeNullBooleanAsFalse(true);
     * // Object: {active: null}
     * // Output: {"active": false} instead of {"active": null}
     * }</pre>
     *
     * @param writeNullBooleanAsFalse {@code true} to write null as false, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C writeNullBooleanAsFalse(final boolean writeNullBooleanAsFalse) {
        this.writeNullBooleanAsFalse = writeNullBooleanAsFalse;

        return (C) this;
    }

    /**
     * Checks if BigDecimal values should be written in plain format.
     *
     * @return {@code true} if BigDecimals are written as plain strings, {@code false} otherwise
     */
    public boolean writeBigDecimalAsPlain() {
        return writeBigDecimalAsPlain;
    }

    /**
     * Sets whether to write BigDecimal values in plain format (without scientific notation).
     * When enabled, BigDecimal values will always be written in plain decimal notation.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.writeBigDecimalAsPlain(true);
     * // BigDecimal("1.23E+5") will be written as "123000" 
     * // instead of "1.23E+5"
     * }</pre>
     *
     * @param writeBigDecimalAsPlain {@code true} to write in plain format, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C writeBigDecimalAsPlain(final boolean writeBigDecimalAsPlain) {
        this.writeBigDecimalAsPlain = writeBigDecimalAsPlain;

        return (C) this;
    }

    /**
     * Checks if serialization should fail when encountering empty beans.
     *
     * @return {@code true} if should fail on empty beans, {@code false} otherwise
     */
    public boolean failOnEmptyBean() {
        return failOnEmptyBean;
    }

    /**
     * Sets whether serialization should fail when encountering empty beans.
     * An empty bean is one with no serializable properties.
     * When disabled, empty beans will be serialized as empty objects ({}).
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.failOnEmptyBean(false);
     * // Empty beans will be serialized as {} instead of throwing exception
     * 
     * config.failOnEmptyBean(true);
     * // Empty beans will cause a serialization exception
     * }</pre>
     *
     * @param failOnEmptyBean {@code true} to fail on empty beans, {@code false} to allow
     * @return this instance for method chaining
     */
    public C failOnEmptyBean(final boolean failOnEmptyBean) {
        this.failOnEmptyBean = failOnEmptyBean;

        return (C) this;
    }

    /**
     * Checks if circular references are supported during serialization.
     *
     * @return {@code true} if circular references are supported, {@code false} otherwise
     */
    public boolean supportCircularReference() {
        return supportCircularReference;
    }

    /**
     * Sets whether to support circular references during serialization.
     * When enabled, circular references will be handled gracefully instead of causing stack overflow.
     * The implementation details of how circular references are represented depend on the serializer.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * config.supportCircularReference(true);
     * // Objects with circular references will serialize without infinite recursion
     * 
     * Person parent = new Person("Parent");
     * Person child = new Person("Child");
     * parent.setChild(child);
     * child.setParent(parent); // Circular reference
     * }</pre>
     *
     * @param supportCircularReference {@code true} to support circular references, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C supportCircularReference(final boolean supportCircularReference) {
        this.supportCircularReference = supportCircularReference;

        return (C) this;
    }
}