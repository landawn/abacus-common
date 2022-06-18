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

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
class JSONStreamReader extends JSONStringReader {

    JSONStreamReader(Reader reader, char[] rbuf, char[] cbuf) {
        super(rbuf, 0, 0, cbuf, reader);
    }

    JSONStreamReader(Reader reader, char[] rbuf, int beginIndex, int endIndex, char[] cbuf) {
        super(rbuf, beginIndex, endIndex, cbuf, reader);
    }

    /**
     *
     * @param reader
     * @param rbuf
     * @param cbuf
     * @return
     */
    public static JSONReader parse(Reader reader, char[] rbuf, char[] cbuf) {
        // Warning. There is a bug in below code ---> empty value is returned if input source is InputStream/Reader.
        //    int n = 0;
        //
        //    try {
        //        n = IOUtil.read(reader, rbuf, 0, rbuf.length);
        //    } catch (IOException e) {
        //        throw new UncheckedIOException(e);
        //    }
        //
        //    if (n <= 0) {
        //        return new JSONStringReader(rbuf, 0, 0, cbuf, reader);
        //    } else if (n < rbuf.length) {
        //        return new JSONStringReader(rbuf, 0, n, cbuf, reader);
        //    } else {
        //        return new JSONStreamReader(reader, rbuf, 0, n, cbuf);
        //    }

        return new JSONStreamReader(reader, rbuf, cbuf);
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
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

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

                if (strBeginIndex >= strEndIndex) {
                    refill();
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

                if (strBeginIndex >= strEndIndex) {
                    refill();
                }
            }
        }

        strEnd = strBeginIndex;
        nextEvent = -1;

        return nextEvent;
    }

    //
    //    @Override
    //    public int nextNameToken() throws IOException {
    //        if (strPosition >= strLength) {
    //            refill();
    //        }
    //
    //        nextChar = 0;
    //        strStart = strPosition;
    //
    //        if (nextEvent == START_QUOTATION_D) {
    //            for (int ch = 0; strPosition < strLength;) {
    //                ch = strValue[strPosition++];
    //
    //                if (ch == '"') {
    //                    strEnd = strPosition - 1;
    //                    nextEvent = END_QUOTATION_D;
    //
    //                    return nextEvent;
    //                }
    //
    //                if (nextChar > 0) {
    //                    cbuf[nextChar++] = (char) ch;
    //                }
    //
    //                if (strPosition >= strLength) {
    //                    refill();
    //                }
    //            }
    //        } else if (nextEvent == START_QUOTATION_S) {
    //            for (int ch = 0; strPosition < strLength;) {
    //                ch = strValue[strPosition++];
    //
    //                if (ch == '\'') {
    //                    strEnd = strPosition - 1;
    //                    nextEvent = END_QUOTATION_S;
    //
    //                    return nextEvent;
    //                }
    //
    //                if (nextChar > 0) {
    //                    cbuf[nextChar++] = (char) ch;
    //                }
    //
    //                if (strPosition >= strLength) {
    //                    refill();
    //                }
    //            }
    //        } else {
    //            for (int ch = 0; strPosition < strLength;) {
    //                ch = strValue[strPosition++];
    //
    //                if (ch < 128) {
    //                    nextEvent = charEvents[ch];
    //
    //                    if (nextEvent > 0) {
    //                        strEnd = strPosition - 1;
    //
    //                        return nextEvent;
    //                    }
    //                }
    //
    //                if (nextChar > 0) {
    //                    cbuf[nextChar++] = (char) ch;
    //
    //                    // skip whitespace char.
    //                } else if ((ch < 33) && (strStart == (strPosition - 1))) {
    //                    strStart++;
    //                }
    //
    //                if (strPosition >= strLength) {
    //                    refill();
    //                }
    //            }
    //        }
    //
    //        strEnd = strPosition - 1;
    //        nextEvent = -1;
    //
    //        return nextEvent;
    //    }
    //

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

    /**
     * Unescapes the character identified by the character or characters that immediately follow a backslash. The
     * backslash '\' should have already been read. This supports both unicode escapes "u000A" and two-character escapes
     * "\n".
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NumberFormatException             if any unicode escape sequences are malformed.
     */
    @Override
    protected char readEscapeCharacter() throws IOException {
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

        int escaped = strValue[strBeginIndex++];

        switch (escaped) {
            case 'u':

                // Equivalent to Integer.parseInt(stringPool.get(buffer, pos,
                // 4), 16);
                char result = 0;

                for (int i = 0, c = 0; i < 4; i++) {
                    if (strBeginIndex >= strEndIndex) {
                        refill();
                    }

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

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void refill() throws IOException {
        if (strBeginIndex >= strEndIndex) {
            if (nextChar == 0) {
                strEnd = strBeginIndex;

                if (strEnd > strStart) {
                    N.copy(strValue, strStart, cbuf, 0, strEnd - strStart);
                    nextChar = strEnd - strStart;
                } else {
                    strStart = 0;
                }
            }

            int n = IOUtil.read(reader, strValue, 0, strValue.length);

            if (n > 0) {
                strBeginIndex = 0;
                strEndIndex = n;
            }
        }
    }
}
