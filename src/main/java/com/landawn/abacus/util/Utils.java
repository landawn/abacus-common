/*
 * Copyright (C) 2018 HaiYang Li
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

package com.landawn.abacus.util;

import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;

/**
 * The Class Utils.
 *
 * @author Haiyang Li
 * @since 1.2
 */
final class Utils {

    /** The Constant jsonParser. */
    // lazy initialization to avoid: NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JSONParserImpl
    static final JSONParser jsonParser = ParserFactory.createJSONParser();

    /** The Constant abacusXMLParser. */
    static final XMLParser abacusXMLParser = ParserFactory.isAbacusXMLAvailable() ? ParserFactory.createAbacusXMLParser() : null;

    /** The Constant xmlParser. */
    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    /** The Constant kryoParser. */
    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    /** The Constant jsc. */
    static final JSONSerializationConfig jsc = JSC.create().setQuotePropName(true).setQuoteMapKey(true);

    /** The Constant jscPrettyFormat. */
    static final JSONSerializationConfig jscPrettyFormat = JSC.create().setQuotePropName(true).setQuoteMapKey(true).setPrettyFormat(true);

    /** The Constant xsc. */
    static final XMLSerializationConfig xsc = XSC.create();

    /** The Constant xscPrettyFormat. */
    static final XMLSerializationConfig xscPrettyFormat = XSC.create().setPrettyFormat(true);

    /** The Constant xscForClone. */
    static final XMLSerializationConfig xscForClone = XSC.create().setIgnoreTypeInfo(false);

    /** The Constant booleanType. */
    static final Type<Boolean> booleanType = N.typeOf(boolean.class);

    /** The Constant charType. */
    static final Type<Character> charType = N.typeOf(char.class);

    /** The Constant byteType. */
    static final Type<Byte> byteType = N.typeOf(byte.class);

    /** The Constant shortType. */
    static final Type<Short> shortType = N.typeOf(short.class);

    /** The Constant intType. */
    static final Type<Integer> intType = N.typeOf(int.class);

    /** The Constant longType. */
    static final Type<Long> longType = N.typeOf(long.class);

    /** The Constant floatType. */
    static final Type<Float> floatType = N.typeOf(float.class);

    /** The Constant doubleType. */
    static final Type<Double> doubleType = N.typeOf(double.class);

    /**
     * Instantiates a new utils.
     */
    private Utils() {
        // singleton.
    }

}
