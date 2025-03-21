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

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

interface JSONReader {
    // < 32 = ' ' (Space) White space
    int EOF = -1;

    int UNDEFINED = 0;

    int START_BRACE = 1;

    int END_BRACE = 2;

    int START_BRACKET = 3;

    int END_BRACKET = 4;

    int START_QUOTATION_D = 5;

    int END_QUOTATION_D = 6;

    int START_QUOTATION_S = 7;

    int END_QUOTATION_S = 8;

    int COLON = 9;

    int COMMA = 10;

    // > 32 = ' ' (Space)

    /**
     *
     * @return - 1 is returned if no next symbol is found.
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    int nextToken() throws UncheckedIOException;

    // int nextNameToken() throws UncheckedIOException;

    /**
     * Checks for text.
     *
     * @return {@code true}, if successful
     */
    boolean hasText();

    /**
     * Gets the text.
     *
     * @return
     */
    String getText();

    /**
     *
     * @param <T>
     * @param type
     * @return
     */
    <T> T readValue(Type<? extends T> type);

    /**
     * Read prop info.
     *
     * @param symbolReader
     * @return
     */
    PropInfo readPropInfo(SymbolReader symbolReader);

    void close() throws UncheckedIOException;

    /**
     * Added for performance tuning.
     *
     *
     */
    interface SymbolReader {
        /**
         *
         * @param propName
         * @return
         */
        PropInfo getPropInfo(String propName);

        /**
         * Read prop info.
         *
         * @param cbuf
         * @param fromIndex
         * @param toIndex
         * @return
         */
        PropInfo readPropInfo(char[] cbuf, int fromIndex, int toIndex);

        // PropInfo readPropInfo(String str, int fromIndex, int toIndex);
    }
}
