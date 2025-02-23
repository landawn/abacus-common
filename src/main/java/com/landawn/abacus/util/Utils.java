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

import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;

final class Utils {

    // lazy initialization to avoid: NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JSONParserImpl
    static final JSONParser jsonParser = ParserFactory.createJSONParser();

    static final XMLParser abacusXMLParser = ParserFactory.isAbacusXMLAvailable() ? ParserFactory.createAbacusXMLParser() : null;

    static final XMLParser xmlParser = ParserFactory.isXMLAvailable() ? ParserFactory.createXMLParser() : null;

    static final KryoParser kryoParser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : null;

    static final JSONSerializationConfig jsc = JSC.create().quotePropName(true).quoteMapKey(true);

    static final JSONSerializationConfig jscPrettyFormat = JSC.create().quotePropName(true).quoteMapKey(true).prettyFormat(true);

    static final XMLSerializationConfig xsc = XSC.create();

    static final XMLSerializationConfig xscPrettyFormat = XSC.create().prettyFormat(true);

    static final XMLSerializationConfig xscForClone = XSC.create().ignoreTypeInfo(false);

    static final Type<Boolean> booleanType = N.typeOf(boolean.class);

    static final Type<Character> charType = N.typeOf(char.class);

    static final Type<Byte> byteType = N.typeOf(byte.class);

    static final Type<Short> shortType = N.typeOf(short.class);

    static final Type<Integer> intType = N.typeOf(int.class);

    static final Type<Long> longType = N.typeOf(long.class);

    static final Type<Float> floatType = N.typeOf(float.class);

    static final Type<Double> doubleType = N.typeOf(double.class);

    private Utils() {
        // singleton.
    }

}
