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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
class JSONStringReader extends AbstractJSONReader {

    static final Map<Class<?>, Object> defaultOptionals = new HashMap<>(16);

    static {
        defaultOptionals.put(OptionalBoolean.class, OptionalBoolean.empty());
        defaultOptionals.put(OptionalChar.class, OptionalChar.empty());
        defaultOptionals.put(OptionalByte.class, OptionalByte.empty());
        defaultOptionals.put(OptionalShort.class, OptionalShort.empty());
        defaultOptionals.put(OptionalInt.class, OptionalInt.empty());
        defaultOptionals.put(OptionalLong.class, OptionalLong.empty());
        defaultOptionals.put(OptionalFloat.class, OptionalFloat.empty());
        defaultOptionals.put(OptionalDouble.class, OptionalDouble.empty());
        defaultOptionals.put(Optional.class, Optional.empty());
        defaultOptionals.put(Nullable.class, Nullable.empty());
    }

    final Reader reader;

    final char[] strValue;

    int strEndIndex = 0;

    int strBeginIndex = 0;

    int strStart = 0;

    int strEnd = 0;

    char[] cbuf;

    int cbufLen = 0;

    int nextEvent = -1;

    int nextChar = 0;

    JSONStringReader(final String str, final char[] cbuf) {
        this(str, 0, str.length(), cbuf);
    }

    @SuppressWarnings("deprecation")
    JSONStringReader(final String str, final int beginIndex, final int toIndex, final char[] cbuf) {
        this(com.landawn.abacus.util.InternalUtil.getCharsForReadOnly(str), beginIndex, toIndex, cbuf, null);
    }

    JSONStringReader(final char[] strValue, final int beginIndex, final int endIndex, final char[] cbuf, final Reader reader) {
        if (beginIndex < 0 || endIndex < 0 || endIndex < beginIndex) {
            throw new IllegalArgumentException("Invalid beginIndex or endIndex: " + beginIndex + ", " + endIndex);
        }

        this.reader = reader;

        this.strValue = strValue;
        this.strBeginIndex = beginIndex;
        this.strEndIndex = endIndex;
        this.cbuf = cbuf;
        this.cbufLen = this.cbuf.length;
    }

    /**
     *
     * @param str
     * @param cbuf
     * @return
     */
    public static JSONReader parse(final String str, final char[] cbuf) {
        //        return new JSONStreamReader(new StringReader(str), new char[1], cbuf);

        return new JSONStringReader(str, cbuf);
    }

    /**
     *
     * @param str
     * @param beginIndex
     * @param endIndex
     * @param cbuf
     * @return
     */
    public static JSONReader parse(final String str, int beginIndex, int endIndex, final char[] cbuf) {
        return new JSONStringReader(str, beginIndex, endIndex, cbuf);
    }

    //
    //    @Override
    //    public int nextNameToken() throws IOException {
    //        nextChar = 0;
    //        strStart = strPosition;
    //
    //        if (nextEvent == START_QUOTATION_D) {
    //            for (; strPosition < strLength;) {
    //                if (strValue[strPosition++] == '"') {
    //                    strEnd = strPosition - 1;
    //                    nextEvent = END_QUOTATION_D;
    //
    //                    return nextEvent;
    //                }
    //            }
    //        } else if (nextEvent == START_QUOTATION_S) {
    //            for (; strPosition < strLength;) {
    //                if (strValue[strPosition++] == '\'') {
    //                    strEnd = strPosition - 1;
    //                    nextEvent = END_QUOTATION_S;
    //
    //                    return nextEvent;
    //                }
    //            }
    //        } else {
    //            for (int ch = 0; strPosition < strLength;) {
    //                ch = strValue[strPosition++];
    //
    //                // skip whitespace char.
    //                if ((ch < 33) && (strStart == (strPosition - 1))) {
    //                    strStart++;
    //                } else if (ch < 128) {
    //                    nextEvent = charEvents[ch];
    //
    //                    if (nextEvent > 0) {
    //                        strEnd = strPosition - 1;
    //
    //                        return nextEvent;
    //                    }
    //                }
    //            }
    //        }
    //
    //        strEnd = strPosition - 1;
    //        nextEvent = -1;
    //
    //        return nextEvent;
    //    }
    /**
     * Checks for text.
     *
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    //
    @Override
    public boolean hasText() throws IOException {
        return (nextChar > 0) || (strEnd > strStart);
    }

    /**
     * TODO performance improvement: Refer to the test above. TODO limitation: the maximum length of property value is
     * the buffer size.
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int nextToken() throws IOException {
        nextChar = 0;
        strStart = strBeginIndex;

        if (nextEvent == START_QUOTATION_D || nextEvent == START_QUOTATION_S) {
            final char quoteChar = nextEvent == START_QUOTATION_D ? WD._QUOTATION_D : WD._QUOTATION_S;

            for (int ch = 0; strBeginIndex < strEndIndex;) {
                ch = strValue[strBeginIndex++];

                if (ch == quoteChar) {
                    strEnd = strBeginIndex - 1;
                    nextEvent = quoteChar == WD._QUOTATION_D ? END_QUOTATION_D : END_QUOTATION_S;

                    return nextEvent;
                }

                if (nextChar > 0) {
                    if (nextChar >= cbufLen) {
                        enlargeCharBuffer();
                    }

                    cbuf[nextChar++] = (ch == WD._BACKSLASH) ? readEscapeCharacter() : (char) ch;
                } else {
                    if (ch == WD._BACKSLASH) {
                        strEnd = strBeginIndex - 1;

                        if (strEnd - strStart + 1 >= cbufLen) {
                            enlargeCharBuffer();
                        }

                        N.copy(strValue, strStart, cbuf, 0, strEnd - strStart);

                        nextChar = strEnd - strStart;
                        // strStart++;
                        cbuf[nextChar++] = readEscapeCharacter();
                    }
                }
            }
        } else {
            for (int ch = 0; strBeginIndex < strEndIndex;) {
                ch = strValue[strBeginIndex++];

                if (ch < 128) {
                    nextEvent = charEvents[ch];

                    if (nextEvent > 0) {
                        strEnd = strBeginIndex - 1;

                        return nextEvent;
                    }
                }

                if (nextChar > 0) {
                    if (ch == WD._BACKSLASH) {
                        ch = readEscapeCharacter();
                    }

                    if (ch < 33) {
                        // skip whitespace char.
                    } else {
                        if (nextChar >= cbufLen) {
                            enlargeCharBuffer();
                        }

                        cbuf[nextChar++] = (char) ch;
                    }
                } else {
                    if (ch < 33) {
                        if (strStart == (strBeginIndex - 1)) {
                            strStart++;
                        } else {
                            // skip whitespace char.

                            strEnd = strBeginIndex - 1;

                            if (strEnd - strStart + 1 >= cbufLen) {
                                enlargeCharBuffer();
                            }

                            N.copy(strValue, strStart, cbuf, 0, strEnd - strStart);

                            nextChar = strEnd - strStart;
                        }
                    } else if (ch == WD._BACKSLASH) {
                        strEnd = strBeginIndex - 1;

                        if (strEnd - strStart + 1 >= cbufLen) {
                            enlargeCharBuffer();
                        }

                        N.copy(strValue, strStart, cbuf, 0, strEnd - strStart);

                        nextChar = strEnd - strStart;
                        // strStart++;
                        ch = readEscapeCharacter();

                        if (ch < 33) {
                            // skip whitespace char.
                        } else {
                            cbuf[nextChar++] = (char) ch;
                        }
                    }
                }

                //                if ((nextChar == 0) && (ch < 33)) {
                //                    // skip the starting white characters.
                //                } else {
                //                    cbuf[nextChar++] = (ch == '\\') ? readEscapeCharacter() : (char) ch;
                //                }
            }
        }

        strEnd = strBeginIndex;
        nextEvent = -1;

        return nextEvent;
    }

    /**
     * Gets the text.
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public String getText() throws IOException {
        return (nextChar > 0) ? String.valueOf(cbuf, 0, nextChar) : String.valueOf(strValue, strStart, strEnd - strStart);
    }

    /**
     *
     * @param <T>
     * @param type
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(Type<T> type) throws IOException {
        //        if (((nextEvent != END_QUOTATION_D) && (nextEvent != END_QUOTATION_S))
        //                && (((nextChar >= 4) && (cbuf[0] == 'n') && (cbuf[1] == 'u') && (cbuf[2] == 'l') && (cbuf[3] == 'l')) || (((strEnd - strStart) >= 4)
        //                        && (strValue[strStart] == 'n') && (strValue[strStart + 1] == 'u') && (strValue[strStart + 2] == 'l') && (strValue[strStart + 3] == 'l')))) {
        //
        //            if (nextChar == 4 || (strEnd - strStart) == 4) {
        //                return null;
        //            } else {
        //                boolean isNull = true;
        //                if (nextChar > 4) {
        //                    for (int i = 4; i < nextChar; i++) {
        //                        if (cbuf[i] > 32) {
        //                            isNull = false;
        //                            break;
        //                        }
        //                    }
        //                } else {
        //                    for (int i = strStart + 4; i < strEnd; i++) {
        //                        if (strValue[i] > 32) {
        //                            isNull = false;
        //                            break;
        //                        }
        //                    }
        //                }
        //
        //                if (isNull) {
        //                    return null;
        //                }
        //            }
        //
        //        }
        //

        if (nextEvent != END_QUOTATION_D && nextEvent != END_QUOTATION_S) {
            if (((nextChar == 4) && (cbuf[0] == 'n') && (cbuf[1] == 'u') && (cbuf[2] == 'l') && (cbuf[3] == 'l')) || (((strEnd - strStart) == 4)
                    && (strValue[strStart] == 'n') && (strValue[strStart + 1] == 'u') && (strValue[strStart + 2] == 'l') && (strValue[strStart + 3] == 'l'))) {
                return type.isOptionalOrNullable() ? (T) defaultOptionals.get(type.clazz()) : null;
            }

            if (type.clazz().equals(Object.class)) {
                if (((nextChar == 4) && (cbuf[0] == 't') && (cbuf[1] == 'r') && (cbuf[2] == 'u') && (cbuf[3] == 'e'))
                        || (((strEnd - strStart) == 4) && (strValue[strStart] == 't') && (strValue[strStart + 1] == 'r') && (strValue[strStart + 2] == 'u')
                                && (strValue[strStart + 3] == 'e'))) {
                    return (T) Boolean.TRUE;
                } else if (((nextChar == 5) && (cbuf[0] == 'f') && (cbuf[1] == 'a') && (cbuf[2] == 'l') && (cbuf[3] == 's') && (cbuf[4] == 'e'))
                        || (((strEnd - strStart) == 5) && (strValue[strStart] == 'f') && (strValue[strStart + 1] == 'a') && (strValue[strStart + 2] == 'l')
                                && (strValue[strStart + 3] == 's') && (strValue[strStart + 4] == 'e'))) {
                    return (T) Boolean.FALSE;
                } else {
                    final String str = new String(nextChar > 0 ? N.copyOfRange(cbuf, 0, nextChar) : N.copyOfRange(strValue, strStart, strEnd));

                    try {
                        return (T) Numbers.createNumber(str).get();
                    } catch (Exception e) {
                        // ignore;
                    }

                    return type.valueOf(str);
                }
            }
        }

        //        if (type == null) {
        //            return (T) (((nextChar > 0) || (str == null)) ? String.valueOf(cbuf, 0, nextChar) : str.substring(strStart, strEnd));
        //        } else {
        //            return ((nextChar > 0) || (str == null)) ? type.valueOf(cbuf, 0, nextChar) : type.valueOf(strValue, strStart, strEnd - strStart);
        //        }

        if (nextChar > 0) {
            return type.valueOf(cbuf, 0, nextChar);
        } else {
            return type.valueOf(strValue, strStart, strEnd - strStart);
        }
    }

    /**
     * Read prop info.
     *
     * @param symbolReader
     * @return
     */
    @Override
    public PropInfo readPropInfo(SymbolReader symbolReader) {
        return (nextChar > 0) ? symbolReader.readPropInfo(cbuf, 0, nextChar) : symbolReader.readPropInfo(strValue, strStart, strEnd);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /*
     * Copyright (C) 2010 Google Inc.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
     * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
     * the specific language governing permissions and limitations under the License.
     */

    void enlargeCharBuffer() {
        cbuf = N.copyOf(cbuf, (int) (cbufLen * 1.75));
        cbufLen = cbuf.length;
    }

    /**
     * Unescapes the character identified by the character or characters that immediately follow a backslash. The
     * backslash '\' should have already been read. This supports both unicode escapes "u000A" and two-character escapes
     * "\n".
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NumberFormatException             if any unicode escape sequences are malformed.
     */
    protected char readEscapeCharacter() throws IOException {
        int escaped = strValue[strBeginIndex++];

        switch (escaped) {
            case 'u':

                // Equivalent to Integer.parseInt(stringPool.get(buffer, pos,
                // 4), 16);
                char result = 0;

                for (int i = 0, c = 0; i < 4; i++) {
                    c = strValue[strBeginIndex++];

                    result <<= 4;

                    if ((c >= '0') && (c <= '9')) {
                        result += (c - '0');
                    } else if ((c >= 'a') && (c <= 'f')) {
                        result += (c - 'a' + 10);
                    } else if ((c >= 'A') && (c <= 'F')) {
                        result += (c - 'A' + 10);
                    } else {
                        throw new ParseException("Number format fxception: \\u" + String.valueOf(cbuf));
                    }
                }

                return result;

            case 't':
                return '\t';

            case 'b':
                return '\b';

            case 'n':
                return '\n';

            case 'r':
                return '\r';

            case 'f':
                return '\f';

            // // fall-through
            // case '\'':
            // case '"':
            // case '\\':
            default:
                return (char) escaped;
        }
    }
}
