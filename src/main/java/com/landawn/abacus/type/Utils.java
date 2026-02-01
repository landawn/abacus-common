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

import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerializationConfig;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;

/**
 * Internal utility class providing shared parser instances and configurations for the type system.
 * This class uses lazy initialization to avoid class loading issues and provides centralized
 * access to commonly used parsers throughout the type package.
 *
 * <p>The parsers and configurations are initialized lazily to:</p>
 * <ul>
 *   <li>Avoid NoClassDefFoundError during class loading</li>
 *   <li>Reduce memory footprint when parsers are not needed</li>
 *   <li>Ensure parsers are only created when actually used</li>
 * </ul>
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
public final class Utils {

    // lazy initialization to avoid: NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JsonParserImpl
    /**
     * Shared JSON parser instance for use throughout the type system.
     * Lazily initialized to avoid class loading issues.
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
    static final JsonSerializationConfig jsc = JSC.create();

    /**
     * Default JSON deserialization configuration used by type converters.
     * Created with standard settings suitable for most type conversions.
     */
    static final JsonDeserializationConfig jdc = JDC.create();

    private Utils() {
        // singleton.
    }
}