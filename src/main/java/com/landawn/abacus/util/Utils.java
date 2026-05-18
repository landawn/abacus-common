/*
 * Copyright (C) 2018 HaiYang Li
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

package com.landawn.abacus.util;

import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;

/**
 * Internal utility class that provides commonly used static instances of parsers, serialization configurations,
 * and type objects for the abacus-common framework. This class serves as a centralized registry for frequently
 * accessed objects to improve performance and reduce object creation overhead.
 *
 * <p>This class is package-private and intended for internal use only within the util package.
 * Optional parsers (XML, Kryo) are conditionally created via {@link ParserFactory} availability
 * checks, so a missing dependency on the classpath leaves the corresponding field as {@code null}
 * rather than throwing a {@link NoClassDefFoundError} at class-loading time.</p>
 *
 * <p>Key components provided:</p>
 * <ul>
 *   <li><strong>Parsers:</strong> Pre-configured instances of JSON, XML, and Kryo parsers</li>
 *   <li><strong>Serialization Configs:</strong> Common configurations for JSON and XML serialization</li>
 *   <li><strong>Type Objects:</strong> Cached {@link Type} instances for primitive types</li>
 * </ul>
 *
 * <p>The parser instances are conditionally initialized based on availability:</p>
 * <ul>
 *   <li>JSON parser is always available</li>
 *   <li>XML parsers ({@code abacusXmlParser}, {@code xmlParser}) are non-{@code null} only if the
 *       corresponding XML libraries are present on the classpath</li>
 *   <li>{@code kryoParser} is non-{@code null} only if the Kryo library is present on the classpath</li>
 * </ul>
 *
 * @see ParserFactory
 * @see JsonSerConfig
 * @see XmlSerConfig
 */
final class Utils {

    /** Shared JSON parser instance; always non-{@code null}. */
    // Eagerly initialized; ParserFactory.createJsonParser() must always be available.
    static final JsonParser jsonParser = ParserFactory.createJsonParser();

    /**
     * Shared Abacus XML parser instance; {@code null} if the Abacus XML parser library
     * is not present on the classpath.
     */
    static final XmlParser abacusXmlParser = ParserFactory.isAbacusXmlParserAvailable() ? ParserFactory.createAbacusXmlParser() : null;

    /**
     * Shared general XML parser instance; {@code null} if the XML parser library
     * is not present on the classpath.
     */
    static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    /**
     * Shared Kryo parser instance; {@code null} if the Kryo library
     * is not present on the classpath.
     */
    static final KryoParser kryoParser = ParserFactory.isKryoParserAvailable() ? ParserFactory.createKryoParser() : null;

    /** Default JSON serialization configuration: quoted property names and quoted map keys. */
    static final JsonSerConfig jsc = JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(true);

    /** JSON serialization configuration with pretty-printing enabled, quoted property names, and quoted map keys. */
    static final JsonSerConfig jscPrettyFormat = JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(true).setPrettyFormat(true);

    /** Default XML serialization configuration. */
    static final XmlSerConfig xsc = XmlSerConfig.create();

    /** XML serialization configuration with pretty-printing enabled. */
    static final XmlSerConfig xscPrettyFormat = XmlSerConfig.create().setPrettyFormat(true);

    /** XML serialization configuration used for object cloning; includes type information in the output. */
    static final XmlSerConfig xscForClone = XmlSerConfig.create().setWriteTypeInfo(true);

    /** Cached {@link Type} descriptor for the primitive {@code boolean} type. */
    static final Type<Boolean> booleanType = Type.of(boolean.class);

    /** Cached {@link Type} descriptor for the primitive {@code char} type. */
    static final Type<Character> charType = Type.of(char.class);

    /** Cached {@link Type} descriptor for the primitive {@code byte} type. */
    static final Type<Byte> byteType = Type.of(byte.class);

    /** Cached {@link Type} descriptor for the primitive {@code short} type. */
    static final Type<Short> shortType = Type.of(short.class);

    /** Cached {@link Type} descriptor for the primitive {@code int} type. */
    static final Type<Integer> intType = Type.of(int.class);

    /** Cached {@link Type} descriptor for the primitive {@code long} type. */
    static final Type<Long> longType = Type.of(long.class);

    /** Cached {@link Type} descriptor for the primitive {@code float} type. */
    static final Type<Float> floatType = Type.of(float.class);

    /** Cached {@link Type} descriptor for the primitive {@code double} type. */
    static final Type<Double> doubleType = Type.of(double.class);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Utils() {
        // Utility class - prevent instantiation
    }

}
