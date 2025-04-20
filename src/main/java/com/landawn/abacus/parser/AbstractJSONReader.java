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

import com.landawn.abacus.util.Strings;

abstract class AbstractJSONReader implements JSONReader { //NOSONAR
    static final int MAX_PARSABLE_NUM_LEN = Long.toString(Long.MAX_VALUE, 10).length() - 1;

    static final long[] POWERS_OF_TEN = { 1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L,
            100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
            100_000_000_000_000_000L, 1_000_000_000_000_000_000L };

    static final String NULL = Strings.NULL;
    static final String FALSE = Boolean.FALSE.toString().intern();
    static final String TRUE = Boolean.TRUE.toString().intern();

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
        charEvents['n'] = 'n';
        charEvents['N'] = 'N';
        charEvents['f'] = 'f';
        charEvents['F'] = 'F';
        charEvents['t'] = 't';
        charEvents['T'] = 'T';
        charEvents['-'] = '-';
        charEvents['+'] = '+';
        charEvents['0'] = '0';
        charEvents['1'] = '1';
        charEvents['2'] = '2';
        charEvents['3'] = '3';
        charEvents['4'] = '4';
        charEvents['5'] = '5';
        charEvents['6'] = '6';
        charEvents['7'] = '7';
        charEvents['8'] = '8';
        charEvents['9'] = '9';
    }

    static final int[] alphanumerics = charEvents.clone();

    static {
        alphanumerics['+'] = '+';
        alphanumerics['-'] = '-';
        alphanumerics['.'] = '.';
        alphanumerics['#'] = '#';

        alphanumerics['x'] = 'x';
        alphanumerics['X'] = 'X';

        //noinspection OverwrittenKey
        alphanumerics['e'] = 'e';
        alphanumerics['E'] = 'E';

        alphanumerics['a'] = 'a';
        alphanumerics['b'] = 'b';
        alphanumerics['c'] = 'c';
        //noinspection OverwrittenKey
        alphanumerics['d'] = 'd';
        //noinspection DataFlowIssue,OverwrittenKey
        alphanumerics['e'] = 'e';
        //noinspection OverwrittenKey
        alphanumerics['f'] = 'f';

        alphanumerics['l'] = 'l';
        alphanumerics['L'] = 'L';
        //noinspection DataFlowIssue,OverwrittenKey
        alphanumerics['f'] = 'f';
        alphanumerics['F'] = 'F';
        //noinspection DataFlowIssue,OverwrittenKey
        alphanumerics['d'] = 'd';
        alphanumerics['D'] = 'D';
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
