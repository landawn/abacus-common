/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.parser;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
abstract class AbstractJSONReader implements JSONReader {

    protected static final int[] charEvents = new int[128];

    static {
        charEvents[','] = COMMA;
        charEvents[':'] = COLON;
        charEvents['"'] = START_QUOTATION_D;
        charEvents['\''] = START_QUOTATION_S;
        charEvents['{'] = START_BRACE;
        charEvents['}'] = END_BRACE;
        charEvents['['] = START_BRACKET;
        charEvents[']'] = END_BRACKET;
    }

    protected static final char[] eventChars = new char[11];

    static {
        eventChars[START_BRACE] = '{';
        eventChars[END_BRACE] = '}';
        eventChars[START_BRACKET] = '[';
        eventChars[END_BRACKET] = ']';
        eventChars[START_QUOTATION_D] = '"';
        eventChars[END_QUOTATION_D] = '"';
        eventChars[START_QUOTATION_S] = '\'';
        eventChars[END_QUOTATION_S] = '\'';
        eventChars[COLON] = ':';
        eventChars[COMMA] = ',';
    }
}
