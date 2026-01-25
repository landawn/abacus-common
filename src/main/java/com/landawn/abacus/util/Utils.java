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
import com.landawn.abacus.parser.JsonSerializationConfig;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.parser.XmlSerializationConfig;
import com.landawn.abacus.parser.XmlSerializationConfig.XSC;
import com.landawn.abacus.type.Type;

/**
 * Internal utility class that provides commonly used static instances of parsers, serialization configurations,
 * and type objects for the abacus-common framework. This class serves as a centralized registry for frequently
 * accessed objects to improve performance and reduce object creation overhead.
 * 
 * <p>This class is package-private and intended for internal use only within the util package.
 * It provides lazy initialization of parsers to avoid potential NoClassDefFoundError issues
 * when certain parser implementations may not be available on the classpath.</p>
 * 
 * <p>Key components provided:</p>
 * <ul>
 *   <li><strong>Parsers:</strong> Pre-configured instances of JSON, XML, and Kryo parsers</li>
 *   <li><strong>Serialization Configs:</strong> Common configurations for JSON and XML serialization</li>
 *   <li><strong>Type Objects:</strong> Cached Type instances for primitive types</li>
 * </ul>
 * 
 * <p>The parser instances are conditionally initialized based on availability:</p>
 * <ul>
 *   <li>JSON parser is always available</li>
 *   <li>XML parsers are only initialized if XML libraries are present</li>
 *   <li>Kryo parser is only initialized if Kryo library is present</li>
 * </ul>
 * 
 * @see ParserFactory
 * @see JsonSerializationConfig
 * @see XmlSerializationConfig
 */
final class Utils {

    // lazy initialization to avoid: NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JsonParserImpl
    static final JsonParser jsonParser = ParserFactory.createJsonParser();

    static final XmlParser abacusXmlParser = ParserFactory.isAbacusXmlParserAvailable() ? ParserFactory.createAbacusXmlParser() : null;

    static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    static final KryoParser kryoParser = ParserFactory.isAvroParserAvailable() ? ParserFactory.createKryoParser() : null;

    static final JsonSerializationConfig jsc = JSC.create().quotePropName(true).quoteMapKey(true);

    static final JsonSerializationConfig jscPrettyFormat = JSC.create().quotePropName(true).quoteMapKey(true).prettyFormat(true);

    static final XmlSerializationConfig xsc = XSC.create();

    static final XmlSerializationConfig xscPrettyFormat = XSC.create().prettyFormat(true);

    static final XmlSerializationConfig xscForClone = XSC.create().writeTypeInfo(true);

    static final Type<Boolean> booleanType = Type.of(boolean.class);

    static final Type<Character> charType = Type.of(char.class);

    static final Type<Byte> byteType = Type.of(byte.class);

    static final Type<Short> shortType = Type.of(short.class);

    static final Type<Integer> intType = Type.of(int.class);

    static final Type<Long> longType = Type.of(long.class);

    static final Type<Float> floatType = Type.of(float.class);

    static final Type<Double> doubleType = Type.of(double.class);

    private Utils() {
        // singleton.
    }

}
