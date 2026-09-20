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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;

/**
 * Base configuration class for JSON and XML serialization operations.
 * This abstract class provides common configuration options that are shared between
 * JSON and XML serialization configurations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JsonSerConfig config = new JsonSerConfig()
 *         .setPrettyFormat(true)
 *         .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
 *         .setIndentation("  ");
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

    /** Whether to write {@code null} string values as empty strings. */
    boolean writeNullStringAsEmpty = false;

    /** Whether to write {@code null} numeric values as zero. */
    boolean writeNullNumberAsZero = false;

    /** Whether to write {@code null} boolean values as {@code false}. */
    boolean writeNullBooleanAsFalse = false;

    /** Whether to write {@code BigDecimal} values in plain format. */
    boolean writeBigDecimalAsPlain = defaultWriteBigDecimalAsPlain;

    /** Whether to support circular references during serialization. */
    boolean circularReferenceSupported = false;

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
     * @throws IllegalArgumentException if {@code charQuotation} is not {@code '}, {@code "} or {@code 0}.
     */
    public C setCharQuotation(final char charQuotation) throws IllegalArgumentException {
        if (charQuotation == SK.CHAR_ZERO || charQuotation == SK._SINGLE_QUOTE || charQuotation == SK._DOUBLE_QUOTE) {
            this.charQuotation = charQuotation;
        } else {
            throw new IllegalArgumentException("Only '\\'', '\"', 0(value is zero) chars are supported");
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
     * @throws IllegalArgumentException if {@code stringQuotation} is not {@code '}, {@code "} or {@code 0}.
     */
    public C setStringQuotation(final char stringQuotation) throws IllegalArgumentException {
        if (stringQuotation == SK.CHAR_ZERO || stringQuotation == SK._SINGLE_QUOTE || stringQuotation == SK._DOUBLE_QUOTE) {
            this.stringQuotation = stringQuotation;
        } else {
            throw new IllegalArgumentException("Only '\\'', '\"', 0(value is zero) chars are supported");
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
     * Unquoted strings are non-standard JSON; XML element text normally has no surrounding quotes.
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
     * Unquoted strings and characters are non-standard JSON; XML element text normally has no surrounding quotes.
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
     * <p>Note that {@code null} is a legal, distinct value (see {@link #setDateTimeFormat(DateTimeFormat)}):
     * it does <i>not</i> restore the {@code LONG} default but makes each temporal type use its own
     * textual form.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getDateTimeFormat();                                                        // returns DateTimeFormat.LONG (default)
     * config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME).getDateTimeFormat();   // returns ISO_8601_DATE_TIME
     * config.setDateTimeFormat(null).getDateTimeFormat();                                // returns null (type-specific text)
     * }</pre>
     *
     * @return the current date time format, or {@code null} if each temporal type uses its own default text
     */
    public DateTimeFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * Sets the date time format for serializing date/time values.
     * This affects how {@code Date}, {@code Calendar}, and other temporal objects are formatted.
     *
     * <p>{@code null} is permitted and makes each temporal type fall back to its own default textual
     * form (ISO-8601 text such as {@code "1970-01-01T00:00:00Z"} for {@code Date} / {@code Calendar},
     * written as a quoted string in JSON). This is NOT the same as the default {@link DateTimeFormat#LONG},
     * which writes epoch milliseconds; pass {@code DateTimeFormat.LONG} explicitly to restore the default.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
     * // Dates will be serialized as "2023-12-25T10:30:00Z"
     *
     * config.setDateTimeFormat(DateTimeFormat.LONG);
     * // Dates will be serialized as milliseconds: 1703502600000
     *
     * config.setDateTimeFormat(null);
     * // Dates will be serialized in the type's own default text: "2023-12-25T10:30:00Z" (not as milliseconds)
     * }</pre>
     *
     * @param dateTimeFormat the date time format to use, or {@code null} to let each temporal type use its own default text
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
     * <p>The string is written verbatim into the output in front of every nested line, so it may only
     * consist of the whitespace characters that both JSON and XML readers skip: space, tab, carriage
     * return and line feed. The empty string is permitted (no indentation). Any other character
     * (including {@code null}, other Unicode whitespace such as U+000C or U+2028, or markup) is rejected
     * because it would be emitted into the document and corrupt it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPrettyFormat(true).setIndentation("\t");     // uses tabs
     * config.setPrettyFormat(true).setIndentation("  ");     // uses 2 spaces
     * config.setPrettyFormat(true).setIndentation("    ");   // uses 4 spaces (default)
     * config.setPrettyFormat(true).setIndentation("");       // no indentation, line breaks only
     * }</pre>
     *
     * @param indentation the indentation string to use; only space, tab, CR and LF characters are allowed
     * @return this instance for method chaining
     * @throws IllegalArgumentException if {@code indentation} is {@code null} or contains a character other than space, tab, CR or LF.
     */
    public C setIndentation(final String indentation) throws IllegalArgumentException {
        N.checkArgNotNull(indentation, cs.indentation);

        // Only the intersection of RFC 8259 JSON whitespace and XML 1.0 'S' is safe: other characters that
        // Character.isWhitespace/Strings.isBlank accept (U+000B, U+000C, U+001C, U+2028, ...) are rejected by
        // the XML or JSON readers, and anything else is injected verbatim into the document.
        for (int i = 0, len = indentation.length(); i < len; i++) {
            final char ch = indentation.charAt(i);

            if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') {
                throw new IllegalArgumentException("'indentation' must contain only space, tab, CR or LF characters, but found: \\u"
                        + Strings.padStart(Integer.toHexString(ch), 4, '0') + " at index " + i);
            }
        }

        this.indentation = indentation;

        return (C) this;
    }

    /**
     * Gets the property naming policy used during serialization.
     *
     * <p>{@code null} (the default) does not mean "original names": it means the bean's own
     * {@code @JsonXmlConfig(namingPolicy)} annotation applies, falling back to
     * {@link NamingPolicy#CAMEL_CASE} (which leaves conventional {@code camelCase} property names
     * unchanged) when the bean is not annotated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.getPropNamingPolicy();                                                // returns null (default: the bean's @JsonXmlConfig policy, else CAMEL_CASE)
     * config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE).getPropNamingPolicy();   // returns SNAKE_CASE
     * }</pre>
     *
     * @return the property naming policy, or {@code null} (default) if the bean's own {@code @JsonXmlConfig(namingPolicy)} applies, falling back to {@link NamingPolicy#CAMEL_CASE}
     */
    public NamingPolicy getPropNamingPolicy() {
        return propNamingPolicy;
    }

    /**
     * Sets the property naming policy for serialization.
     * This determines how bean property names are transformed in the output.
     *
     * <p>A non-null policy applies to bean properties (and to the keys of {@code MapEntity} instances)
     * and <b>overrides</b> any {@code @JsonXmlConfig(namingPolicy)} declared on the bean class: an explicit
     * {@link NamingPolicy#CAMEL_CASE} therefore renames a {@code first_name} produced by a
     * {@code @JsonXmlConfig(namingPolicy = SNAKE_CASE)} bean back to {@code firstName}, whereas {@code null}
     * (the default) lets the bean's annotation win. Keys of plain {@code Map} instances are data, not
     * property names, and are always written as-is regardless of the policy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPropNamingPolicy(NamingPolicy.CAMEL_CASE);
     * // Property "firstName" remains "firstName"; a bean annotated with SNAKE_CASE is forced back to "firstName" too
     *
     * config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
     * // Property "firstName" becomes "first_name"; Map key "firstName" stays "firstName"
     *
     * config.setPropNamingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE);
     * // Property "firstName" becomes "FIRST_NAME"
     *
     * config.setPropNamingPolicy(null);
     * // Each bean's own @JsonXmlConfig(namingPolicy) applies, falling back to CAMEL_CASE
     * }</pre>
     *
     * @param propNamingPolicy the naming policy to use for bean properties, or {@code null} to defer to each bean's {@code @JsonXmlConfig(namingPolicy)} (falling back to {@link NamingPolicy#CAMEL_CASE})
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
     * Checks if {@code null} string values should be written as empty strings.
     *
     * <p>See {@link #setWriteNullStringAsEmpty(boolean)} for the exact scope of this flag: JSON only,
     * {@code null} {@code String}/{@code CharSequence} bean properties that survive the exclusion strategy and
     * {@code null} elements of typed collections/arrays - never {@code null} map values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullStringAsEmpty();                                   // returns false (default)
     * config.setWriteNullStringAsEmpty(true).isWriteNullStringAsEmpty();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} string bean properties and typed string elements are written as {@code ""} in JSON, {@code false} otherwise
     */
    public boolean isWriteNullStringAsEmpty() {
        return writeNullStringAsEmpty;
    }

    /**
     * Sets whether to write {@code null} string values as empty strings.
     *
     * <p><b>Scope (JSON):</b> the flag is consulted wherever a {@code null} is rendered through the
     * {@link com.landawn.abacus.type.Type} of a {@code String}/{@code CharSequence} slot:</p>
     * <ul>
     *   <li>{@code null} bean properties declared as {@code String}/{@code CharSequence} - but only when the
     *       property survives the exclusion strategy: the default {@link Exclusion#NULL} drops {@code null}
     *       properties before this flag is consulted, so use {@link SerializationConfig#setExclusion(Exclusion)}
     *       with {@link Exclusion#NONE} to see the effect. Properties of any other type (numbers, booleans,
     *       dates, nested beans, collections, maps) are not affected by this flag.</li>
     *   <li>{@code null} elements of typed collections/arrays, e.g. {@code List<String>} {@code ["a", null]} and
     *       {@code String[]} are written as {@code ["a", ""]}.</li>
     * </ul>
     * <p>{@code null} map values and elements of an untyped collection ({@code List<Object>}, a root {@code List})
     * are always written as {@code null}. {@link JsonSerConfig#setWriteNullToEmpty(boolean)} empties a
     * {@code null} {@code CharSequence} property even when this flag is off, so the two agree on {@code ""}
     * for strings. In XML a {@code null} bean property is written as an empty element with
     * {@code isNull="true"} whatever this flag says; a {@code null} element inside a typed <i>array</i>
     * property is still written as {@code ""}, because the array is rendered through its
     * {@link com.landawn.abacus.type.Type}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullStringAsEmpty(true).isWriteNullStringAsEmpty();    // returns true
     * config.setWriteNullStringAsEmpty(false).isWriteNullStringAsEmpty();   // returns false
     *
     * // JSON output of a bean with String name = null, Integer age = null, List<String> tags = ["a", null]:
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullStringAsEmpty(true));
     * // -> {"tags": ["a", ""]}                                  (default Exclusion.NULL dropped name and age)
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullStringAsEmpty(true).setExclusion(Exclusion.NONE));
     * // -> {"name": "", "age": null, "tags": ["a", ""]}         (only the String slots are affected)
     * }</pre>
     *
     * @param writeNullStringAsEmpty {@code true} to write {@code null} string bean properties and typed string elements as {@code ""} in JSON, {@code false} to write {@code null}
     * @return this instance for method chaining
     */
    public C setWriteNullStringAsEmpty(final boolean writeNullStringAsEmpty) {
        this.writeNullStringAsEmpty = writeNullStringAsEmpty;

        return (C) this;
    }

    /**
     * Checks if {@code null} number values should be written as zero.
     *
     * <p>See {@link #setWriteNullNumberAsZero(boolean)} for the exact scope of this flag: JSON only,
     * {@code null} numeric bean properties that survive the exclusion strategy and {@code null} elements of
     * typed collections/arrays - never {@code null} map values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullNumberAsZero();                                  // returns false (default)
     * config.setWriteNullNumberAsZero(true).isWriteNullNumberAsZero();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} numeric bean properties and typed numeric elements are written as zero in JSON, {@code false} otherwise
     */
    public boolean isWriteNullNumberAsZero() {
        return writeNullNumberAsZero;
    }

    /**
     * Sets whether to write {@code null} number values as zero.
     *
     * <p><b>Scope (JSON):</b> the flag is consulted wherever a {@code null} is rendered through the
     * {@link com.landawn.abacus.type.Type} of a numeric slot ({@code Integer}, {@code Long}, {@code Double},
     * {@code BigDecimal}, ...; the zero is written in the slot's own form, e.g. {@code 0} or {@code 0.0}):</p>
     * <ul>
     *   <li>{@code null} numeric bean properties - but only when the property survives the exclusion strategy:
     *       the default {@link Exclusion#NULL} drops {@code null} properties before this flag is consulted, so
     *       use {@link SerializationConfig#setExclusion(Exclusion)} with {@link Exclusion#NONE} to see the effect.
     *       Properties of any other type (strings, booleans, dates, nested beans, collections, maps) are not
     *       affected by this flag.</li>
     *   <li>{@code null} elements of typed collections/arrays, e.g. {@code List<Integer>} {@code [1, null]} is
     *       written as {@code [1, 0]}.</li>
     * </ul>
     * <p>{@code null} map values and elements of an untyped collection ({@code List<Object>}, a root {@code List})
     * are always written as {@code null}. {@link JsonSerConfig#setWriteNullToEmpty(boolean)} does not override
     * this flag: a numeric slot has no empty form, so a {@code null} number property is still written as
     * {@code 0} when both are on. In XML a {@code null} bean property is written as an empty element with
     * {@code isNull="true"} whatever this flag says; a {@code null} element inside a typed <i>array</i>
     * property is still written as {@code 0}, because the array is rendered through its
     * {@link com.landawn.abacus.type.Type}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullNumberAsZero(true).isWriteNullNumberAsZero();    // returns true
     * config.setWriteNullNumberAsZero(false).isWriteNullNumberAsZero();   // returns false
     *
     * // JSON output of a bean with Integer age = null, String name = null, List<Integer> nums = [1, null]:
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullNumberAsZero(true));
     * // -> {"nums": [1, 0]}                                     (default Exclusion.NULL dropped age and name)
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullNumberAsZero(true).setExclusion(Exclusion.NONE));
     * // -> {"age": 0, "name": null, "nums": [1, 0]}             (only the numeric slots are affected)
     * }</pre>
     *
     * @param writeNullNumberAsZero {@code true} to write {@code null} numeric bean properties and typed numeric elements as zero in JSON, {@code false} to write {@code null}
     * @return this instance for method chaining
     */
    public C setWriteNullNumberAsZero(final boolean writeNullNumberAsZero) {
        this.writeNullNumberAsZero = writeNullNumberAsZero;

        return (C) this;
    }

    /**
     * Checks if {@code null} boolean values should be written as {@code false}.
     *
     * <p>See {@link #setWriteNullBooleanAsFalse(boolean)} for the exact scope of this flag: JSON only,
     * {@code null} {@code Boolean} bean properties that survive the exclusion strategy and {@code null} elements
     * of typed collections/arrays - never {@code null} map values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.isWriteNullBooleanAsFalse();                                    // returns false (default)
     * config.setWriteNullBooleanAsFalse(true).isWriteNullBooleanAsFalse();   // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} boolean bean properties and typed boolean elements are written as {@code false} in JSON, {@code false} otherwise
     */
    public boolean isWriteNullBooleanAsFalse() {
        return writeNullBooleanAsFalse;
    }

    /**
     * Sets whether to write {@code null} boolean values as {@code false}.
     *
     * <p><b>Scope (JSON):</b> the flag is consulted wherever a {@code null} is rendered through the
     * {@link com.landawn.abacus.type.Type} of a {@code Boolean} slot:</p>
     * <ul>
     *   <li>{@code null} {@code Boolean} bean properties - but only when the property survives the exclusion
     *       strategy: the default {@link Exclusion#NULL} drops {@code null} properties before this flag is
     *       consulted, so use {@link SerializationConfig#setExclusion(Exclusion)} with {@link Exclusion#NONE} to
     *       see the effect. Properties of any other type (strings, numbers, dates, nested beans, collections,
     *       maps) are not affected by this flag.</li>
     *   <li>{@code null} elements of typed collections/arrays, e.g. {@code List<Boolean>} {@code [true, null]} is
     *       written as {@code [true, false]}.</li>
     * </ul>
     * <p>{@code null} map values and elements of an untyped collection ({@code List<Object>}, a root {@code List})
     * are always written as {@code null}. {@link JsonSerConfig#setWriteNullToEmpty(boolean)} does not override
     * this flag: a {@code Boolean} slot has no empty form, so a {@code null} boolean property is still written
     * as {@code false} when both are on. In XML a {@code null} bean property is written as an empty element
     * with {@code isNull="true"} whatever this flag says; a {@code null} element inside a typed <i>array</i>
     * property is still written as {@code false}, because the array is rendered through its
     * {@link com.landawn.abacus.type.Type}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setWriteNullBooleanAsFalse(true).isWriteNullBooleanAsFalse();    // returns true
     * config.setWriteNullBooleanAsFalse(false).isWriteNullBooleanAsFalse();   // returns false
     *
     * // JSON output of a bean with Boolean active = null, String name = null, List<Boolean> flags = [true, null]:
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullBooleanAsFalse(true));
     * // -> {"flags": [true, false]}                             (default Exclusion.NULL dropped active and name)
     * jsonParser.serialize(bean, new JsonSerConfig().setWriteNullBooleanAsFalse(true).setExclusion(Exclusion.NONE));
     * // -> {"active": false, "name": null, "flags": [true, false]}   (only the Boolean slots are affected)
     * }</pre>
     *
     * @param writeNullBooleanAsFalse {@code true} to write {@code null} boolean bean properties and typed boolean elements as {@code false} in JSON, {@code false} to write {@code null}
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
     * @return {@code true} if {@code BigDecimal}s are written in plain notation without an exponent, {@code false} otherwise
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
     * config.isCircularReferenceSupported();                                       // returns false (default)
     * config.setCircularReferenceSupported(true).isCircularReferenceSupported();   // returns true
     * }</pre>
     *
     * @return {@code true} if circular references are supported, {@code false} otherwise
     */
    public boolean isCircularReferenceSupported() {
        return circularReferenceSupported;
    }

    /**
     * Sets whether to support circular references during serialization.
     * When enabled, the JSON and XML parsers track objects on the current serialization path and
     * write a null representation for a repeated reference. This prevents recursion; it does not
     * encode object identities or reconstruct the original cycle when deserialized. When disabled,
     * those parsers reject excessive nesting rather than tracking identities.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig config = new JsonSerConfig();
     * config.setCircularReferenceSupported(true).isCircularReferenceSupported();    // returns true
     * config.setCircularReferenceSupported(false).isCircularReferenceSupported();   // returns false
     * }</pre>
     *
     * @param circularReferenceSupported {@code true} to support circular references, {@code false} otherwise
     * @return this instance for method chaining
     */
    public C setCircularReferenceSupported(final boolean circularReferenceSupported) {
        this.circularReferenceSupported = circularReferenceSupported;

        return (C) this;
    }

    /**
     * Renders a quotation character for {@code toString()}.
     *
     * <p>The "no quotation" marker is the char value {@code 0}; embedding it raw would put a U+0000 into
     * log lines and diff output, so it is rendered as the escape text <code>&#92;u0000</code> instead.</p>
     *
     * @param quotation the quotation character ({@code '}, {@code "}, or {@code 0})
     * @return the character itself, or the text <code>&#92;u0000</code> when the character is {@code 0}
     */
    static String quotationToString(final char quotation) {
        return quotation == SK.CHAR_ZERO ? "\\u0000" : String.valueOf(quotation);
    }

}
