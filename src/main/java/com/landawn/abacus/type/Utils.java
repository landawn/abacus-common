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

package com.landawn.abacus.type;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;

/**
 * Internal utility class providing shared parser instances and configurations for the type system.
 * This class provides centralized access to commonly used parsers and serialization
 * configurations throughout the type package.
 *
 * <p>This class is package-private and intended for internal use only within the type system.
 * It provides singleton instances of:</p>
 * <ul>
 *   <li>JSON parser for serialization/deserialization</li>
 *   <li>XML parser (if available in classpath)</li>
 *   <li>Default JSON serialization configuration</li>
 *   <li>Default JSON deserialization configuration</li>
 * </ul>
 */
final class Utils {

    // Parsers are created eagerly via ParserFactory. xmlParser is guarded by isXmlParserAvailable() so a
    // missing XML library yields null instead of a NoClassDefFoundError when this class initializes.
    /**
     * Shared JSON parser instance for use throughout the type system.
     */
    static final JsonParser jsonParser = ParserFactory.createJsonParser();

    /**
     * Shared XML parser instance for use throughout the type system.
     * Will be {@code null} if XML parsing libraries are not available in the classpath.
     */
    static final XmlParser xmlParser = ParserFactory.isXmlParserAvailable() ? ParserFactory.createXmlParser() : null;

    /**
     * Default JSON serialization configuration used by type converters.
     * Created with standard settings suitable for most type conversions.
     */
    static final JsonSerConfig jsc = JsonSerConfig.create();

    /**
     * Default JSON deserialization configuration used by type converters.
     * Created with standard settings suitable for most type conversions.
     */
    static final JsonDeserConfig jdc = JsonDeserConfig.create();

    private Utils() {
        // Utility class - prevent instantiation
    }
}
