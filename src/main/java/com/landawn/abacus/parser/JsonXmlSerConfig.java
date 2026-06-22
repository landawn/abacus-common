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
import com.landawn.abacus.util.SK;

/**
 * Base configuration class for JSON and XML serialization operations.
 * This abstract class provides common configuration options that are shared between
 * JSON and XML serialization configurations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * config.setPrettyFormat(true)
 *       .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
 *       .setIndentation("  ");
 * }</pre>
 *
 * <p>Common configuration options include:</p>
 * <ul>
 *   <li>Character and string quotation settings</li>
 *   <li>Date/time formatting</li>
 *   <li>Pretty printing with customizable indentation</li>
 *   <li>Number and {@code null} value handling</li>
 *   <li>Property naming policies</li>
 *   <li>Support for circular references</li>
 * </ul>
 *
 * @param <C> the concrete configuration type for method chaining
 * @see JsonSerConfig
 * @see XmlSerConfig
 */
public abstract class JsonXmlSerConfig<C extends JsonXmlSerConfig<C>> extends SerializationConfig<C> {

    /**
     * Protected constructor for subclasses.
     */
    protected JsonXmlSerConfig() {
    }

    /** The default date time format. */
    protected static final DateTimeFormat defaultDateTimeFormat = DateTimeFormat.LONG;

    /** The default value for pretty format. */
    protected static final boolean defaultPrettyFormat = false;

    /** The default value for whether to write {@code BigDecimal} as plain string. */
    protected static final boolean defaultWriteBigDecimalAsPlain = false;

    /** The default indentation string. */
    protected static final String defaultIndentation = "    ";

    /** The character used for quoting char values. */
    char charQuotation = SK._DOUBLE_QUOTE;

    /** The character used for quoting string values. */
    char stringQuotation = SK._DOUBLE_QUOTE;

    /** The format for date/time values. */
    DateTimeFormat dateTimeFormat = defaultDateTimeFormat;

    /** Whether to enable pretty formatting. */
    boolean prettyFormat = defaultPrettyFormat;

    /** Whether to write long values as strings. */
    boolean writeLongAsString = false;

    /** Whether to write null string values as empty strings. */
    boolean writeNullStringAsEmpty = false;

    /** Whether to write null numeric values as zero. */
    boolean writeNullNumberAsZero = false;

    /** Whether to write null boolean values as false. */
    boolean writeNullBooleanAsFalse = false;

    /** Whether to write {@code BigDecimal} values in plain format. */
    boolean writeBigDecimalAsPlain = defaultWriteBigDecimalAsPlain;

    /** Whether to support circular references during serialization. */
    boolean supportCircularReference = false;

    /** Whether to fail when an empty bean is encountered. */
    boolean failOnEmptyBean = true;

    /** The indentation string for pretty printing. */
    String indentation = defaultIndentation;

    /** The naming policy for properties. */
    NamingPolicy propNamingPolicy = null;

    /**
     * Gets the character used for quoting char values.
     *
     * <p><b>Note on deprecation:</b> The concrete subclasses {@link JsonSerConfig} and {@link XmlSerConfig}
     * deprecate the quotation setters because JSON mandates double quotes and XML emits values as element
     * text. These accessors are intentionally <i>not</i> deprecated on this shared base: the serialization
     * engine reads {@code getStringQuotation()} at runtime (see {@code ParserUtil}) as a generic quoting
     * mechanism, so the base-level accessors remain part of the live, non-deprecated contract.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getCharQuotation();                          // returns '"' (default)
     * config.setCharQuotation('\'').getCharQuotation();   // returns '\''
     * }</pre>
     *
     * @return the char quotation character
     */
    public char getCharQuotation() {
        return charQuotation;
    }

    /**
     * Sets the character to use for quoting char values.
     * Supported values are single quote ({@code '}), double quote ({@code "}), or {@code 0} (no quotation).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setCharQuotation('\'');      // uses single quotes
     * config.setCharQuotation('"');       // uses double quotes
     * config.setCharQuotation((char)0);   // uses no quotes
     * }</pre>
     *
     * @param charQuotation the character to use ({@code '}, {@code "}, or {@code 0})
     * @return this instance for method chaining
     * @throws IllegalArgumentException if an unsupported character is provided
     */
    public C setCharQuotation(final char charQuotation) {
        if (charQuotation == SK.CHAR_ZERO || charQuotation == SK._SINGLE_QUOTE || charQuotation == SK._DOUBLE_QUOTE) {
            this.charQuotation = charQuotation;
        } else {
            throw new IllegalArgumentException("Only ''', '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    /**
     * Gets the character used for quoting string values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getStringQuotation();                            // returns '"' (default)
     * config.setStringQuotation('\'').getStringQuotation();   // returns '\''
     * }</pre>
     *
     * @return the string quotation character
     */
    public char getStringQuotation() {
        return stringQuotation;
    }

    /**
     * Sets the character to use for quoting string values.
     * Supported values are single quote ({@code '}), double quote ({@code "}), or {@code 0} (no quotation).
     * Note that JSON standard requires double quotes for strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setStringQuotation('"');       // uses double quotes (standard JSON)
     * config.setStringQuotation('\'');      // uses single quotes (non-standard)
     * config.setStringQuotation((char)0);   // uses no quotes (non-standard)
     * }</pre>
     *
     * @param stringQuotation the character to use ({@code '}, {@code "}, or {@code 0})
     * @return this instance for method chaining
     * @throws IllegalArgumentException if an unsupported character is provided
     */
    public C setStringQuotation(final char stringQuotation) {
        if (stringQuotation == SK.CHAR_ZERO || stringQuotation == SK._SINGLE_QUOTE || stringQuotation == SK._DOUBLE_QUOTE) {
            this.stringQuotation = stringQuotation;
        } else {
            throw new IllegalArgumentException("Only ''', '\"', 0(value is zero) chars are supported");
        }

        return (C) this;
    }

    /**
     * Disables character quotation by setting the quotation character to {@code 0}.
     * Characters will be serialized without surrounding quotes.
     *
     * <p><b>Usage Examples:</b></p>
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
     * Disables string quotation by setting the quotation character to {@code 0}.
     * Strings will be serialized without surrounding quotes.
     * Warning: This produces non-standard JSON/XML.
     *
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getDateTimeFormat();                                                        // returns DateTimeFormat.LONG (default)
     * config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME).getDateTimeFormat();   // returns ISO_8601_DATE_TIME
     * }</pre>
     *
     * @return the current date time format
     */
    public DateTimeFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Sets the date time format for serializing date/time values.
     * This affects how {@code Date}, {@code Calendar}, and other temporal objects are formatted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isPrettyFormat();                         // returns false (default)
     * config.setPrettyFormat(true).isPrettyFormat();   // returns true
     * }</pre>
     *
     * @return {@code true} if pretty format is enabled, {@code false} otherwise
     */
    public boolean isPrettyFormat() {
        return prettyFormat;
    }

    /**
     * Sets whether to enable pretty formatting.
     * When enabled, the output will be formatted with line breaks and indentation,
     * making it more human-readable but larger in size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPrettyFormat(true).setIndentation("  ");
     * }</pre>
     *
     * @param prettyFormat {@code true} to enable pretty formatting, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setPrettyFormat(final boolean prettyFormat) {
        this.prettyFormat = prettyFormat;

        return (C) this;
    }

    /**
     * Gets the indentation string used for pretty formatting.
     * Default is four spaces ({@code "    "}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getIndentation();                        // returns "    " (four spaces, default)
     * config.setIndentation("\t").getIndentation();   // returns "\t"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPrettyFormat(true).setIndentation("\t");     // uses tabs
     * config.setPrettyFormat(true).setIndentation("  ");     // uses 2 spaces
     * config.setPrettyFormat(true).setIndentation("    ");   // uses 4 spaces (default)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getPropNamingPolicy();                                                // returns null (default, uses original names)
     * config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE).getPropNamingPolicy();   // returns SNAKE_CASE
     * }</pre>
     *
     * @return the property naming policy, or {@code null} if using default naming
     */
    public NamingPolicy getPropNamingPolicy() {
        return propNamingPolicy;
    }

    /**
     * Sets the property naming policy for serialization.
     * This determines how property names are transformed in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPropNamingPolicy(NamingPolicy.CAMEL_CASE);
     * // Property "firstName" remains "firstName"
     *
     * config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
     * // Property "firstName" becomes "first_name"
     *
     * config.setPropNamingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE);
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
     * Checks if {@code long} values should be written as strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteLongAsString();                              // returns false (default)
     * config.setWriteLongAsString(true).isWriteLongAsString();   // returns true
     * }</pre>
     *
     * @return {@code true} if longs are written as strings, {@code false} otherwise
     */
    public boolean isWriteLongAsString() {
        return writeLongAsString;
    }

    /**
     * Sets whether to write {@code long} values as strings.
     * This is useful for JavaScript compatibility where large numbers may lose precision.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteLongAsString(true).isWriteLongAsString();    // returns true
     * config.setWriteLongAsString(false).isWriteLongAsString();   // returns false
     * }</pre>
     *
     * @param writeLongAsString {@code true} to write longs as strings, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setWriteLongAsString(final boolean writeLongAsString) {
        this.writeLongAsString = writeLongAsString;

        return (C) this;
    }

    /**
     * Checks if {@code null} strings should be written as empty strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullStringAsEmpty();                                   // returns false (default)
     * config.setWriteNullStringAsEmpty(true).isWriteNullStringAsEmpty();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} strings are written as empty, {@code false} otherwise
     */
    public boolean isWriteNullStringAsEmpty() {
        return writeNullStringAsEmpty;
    }

    /**
     * Sets whether to write {@code null} string values as empty strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullStringAsEmpty(true).isWriteNullStringAsEmpty();    // returns true
     * config.setWriteNullStringAsEmpty(false).isWriteNullStringAsEmpty();   // returns false
     * }</pre>
     *
     * @param writeNullStringAsEmpty {@code true} to write {@code null} as empty string, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setWriteNullStringAsEmpty(final boolean writeNullStringAsEmpty) {
        this.writeNullStringAsEmpty = writeNullStringAsEmpty;

        return (C) this;
    }

    /**
     * Checks if {@code null} numbers should be written as zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullNumberAsZero();                                  // returns false (default)
     * config.setWriteNullNumberAsZero(true).isWriteNullNumberAsZero();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} numbers are written as zero, {@code false} otherwise
     */
    public boolean isWriteNullNumberAsZero() {
        return writeNullNumberAsZero;
    }

    /**
     * Sets whether to write {@code null} number values as zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullNumberAsZero(true).isWriteNullNumberAsZero();    // returns true
     * config.setWriteNullNumberAsZero(false).isWriteNullNumberAsZero();   // returns false
     * }</pre>
     *
     * @param writeNullNumberAsZero {@code true} to write {@code null} as zero, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setWriteNullNumberAsZero(final boolean writeNullNumberAsZero) {
        this.writeNullNumberAsZero = writeNullNumberAsZero;

        return (C) this;
    }

    /**
     * Checks if {@code null} booleans should be written as {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullBooleanAsFalse();                                    // returns false (default)
     * config.setWriteNullBooleanAsFalse(true).isWriteNullBooleanAsFalse();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} booleans are written as {@code false}, {@code false} otherwise
     */
    public boolean isWriteNullBooleanAsFalse() {
        return writeNullBooleanAsFalse;
    }

    /**
     * Sets whether to write {@code null} boolean values as {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullBooleanAsFalse(true).isWriteNullBooleanAsFalse();    // returns true
     * config.setWriteNullBooleanAsFalse(false).isWriteNullBooleanAsFalse();   // returns false
     * }</pre>
     *
     * @param writeNullBooleanAsFalse {@code true} to write {@code null} as {@code false}, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setWriteNullBooleanAsFalse(final boolean writeNullBooleanAsFalse) {
        this.writeNullBooleanAsFalse = writeNullBooleanAsFalse;

        return (C) this;
    }

    /**
     * Checks if {@code BigDecimal} values should be written in plain format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteBigDecimalAsPlain();                                   // returns false (default)
     * config.setWriteBigDecimalAsPlain(true).isWriteBigDecimalAsPlain();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code BigDecimal}s are written as plain strings, {@code false} otherwise
     */
    public boolean isWriteBigDecimalAsPlain() {
        return writeBigDecimalAsPlain;
    }

    /**
     * Sets whether to write {@code BigDecimal} values in plain format (without scientific notation).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteBigDecimalAsPlain(true).isWriteBigDecimalAsPlain();    // returns true
     * config.setWriteBigDecimalAsPlain(false).isWriteBigDecimalAsPlain();   // returns false
     * }</pre>
     *
     * @param writeBigDecimalAsPlain {@code true} to write in plain format, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setWriteBigDecimalAsPlain(final boolean writeBigDecimalAsPlain) {
        this.writeBigDecimalAsPlain = writeBigDecimalAsPlain;

        return (C) this;
    }

    /**
     * Checks if serialization should fail when encountering empty beans.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isFailOnEmptyBean();                             // returns true (default)
     * config.setFailOnEmptyBean(false).isFailOnEmptyBean();   // returns false
     * }</pre>
     *
     * @return {@code true} if should fail on empty beans, {@code false} otherwise
     */
    public boolean isFailOnEmptyBean() {
        return failOnEmptyBean;
    }

    /**
     * Sets whether serialization should fail when encountering empty beans.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setFailOnEmptyBean(false).isFailOnEmptyBean();   // returns false
     * config.setFailOnEmptyBean(true).isFailOnEmptyBean();    // returns true
     * }</pre>
     *
     * @param failOnEmptyBean {@code true} to fail on empty beans, {@code false} to allow
     * @return this instance for method chaining
     */
    public C setFailOnEmptyBean(final boolean failOnEmptyBean) {
        this.failOnEmptyBean = failOnEmptyBean;

        return (C) this;
    }

    /**
     * Checks if circular references are supported during serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isSupportCircularReference();                                     // returns false (default)
     * config.setSupportCircularReference(true).isSupportCircularReference();   // returns true
     * }</pre>
     *
     * @return {@code true} if circular references are supported, {@code false} otherwise
     */
    public boolean isSupportCircularReference() {
        return supportCircularReference;
    }

    /**
     * Sets whether to support circular references during serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setSupportCircularReference(true).isSupportCircularReference();    // returns true
     * config.setSupportCircularReference(false).isSupportCircularReference();   // returns false
     * }</pre>
     *
     * @param supportCircularReference {@code true} to support circular references, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setSupportCircularReference(final boolean supportCircularReference) {
        this.supportCircularReference = supportCircularReference;

        return (C) this;
    }

}
