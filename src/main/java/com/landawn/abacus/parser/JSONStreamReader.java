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

import java.io.IOException;
import java.io.Reader;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

class JSONStreamReader extends JSONStringReader {

    JSONStreamReader(final Reader reader, final char[] rbuf, final char[] cbuf) {
        super(rbuf, 0, 0, cbuf, reader);
    }

    JSONStreamReader(final Reader reader, final char[] rbuf, final int beginIndex, final int endIndex, final char[] cbuf) {
        super(rbuf, beginIndex, endIndex, cbuf, reader);
    }

    /**
     * Creates a JSON reader that parses JSON content from a character stream.
     * This method provides streaming JSON parsing capabilities for efficient processing of large JSON documents.
     *
     * <pre>{@code
     * Reader reader = new FileReader("data.json");
     * JSONReader jsonReader = JSONStreamReader.parse(reader, new char[8192], new char[8192]);
     * }</pre>
     *
     * @param reader the character stream containing JSON content to parse
     * @param rbuf the read buffer for streaming input (recommended size: 8192 or larger)
     * @param cbuf the character buffer for token processing (recommended size: 8192 or larger)
     * @return a JSONReader instance configured for streaming JSON parsing
     * @throws UncheckedIOException if an I/O error occurs during initial setup
     */
    public static JSONReader parse(final Reader reader, final char[] rbuf, final char[] cbuf) {
        // Warning. There is a bug in below code ---> empty value is returned if the input source is InputStream/Reader.
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
     * Reads and returns the next token from the JSON input.
     * This method advances the reader position and identifies the next
     * structural token or value in the JSON stream.
     * 
     * <p>The method handles:</p>
     * <ul>
     *   <li>Quoted strings (double and single quotes)</li>
     *   <li>Numbers (integers and decimals)</li>
     *   <li>Boolean values (true/false)</li>
     *   <li>Null values</li>
     *   <li>Structural tokens (braces, brackets, colons, commas)</li>
     * </ul>
     *
     * @return the token identifier, or -1 if no next token is found
     * @param nextTokenValueType the expected type of the next token value
     * @throws UncheckedIOException if an I/O error occurs during reading
     */
    @Override
    public int nextToken(final Type<?> nextTokenValueType) throws UncheckedIOException {
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

        lastEvent = nextEvent;

        text = null;
        numValue = null;
        nextChar = 0;
        startIndexForText = strBeginIndex;

        if (nextEvent == START_QUOTATION_D || nextEvent == START_QUOTATION_S) {
            final char quoteChar = nextEvent == START_QUOTATION_D ? WD._QUOTATION_D : WD._QUOTATION_S;

            for (int ch = 0; strBeginIndex < strEndIndex;) {
                ch = strValue[strBeginIndex++];

                if (ch == quoteChar) {
                    endIndexForText = strBeginIndex - 1;
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
                        saveToBuffer();

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

                if (ch < 128 && (nextEvent = charEvents[ch]) > 0) {
                    if (nextEvent < 32) { //
                        endIndexForText = strBeginIndex - 1;

                        return nextEvent;
                    }

                    saveChar(ch);

                    if (nextChar == 0 && strBeginIndex - startIndexForText == 1) {
                        boolean isNumber = false;

                        if (nextEvent == 'f') { // false
                            if (saveChar(nextChar()) == 'a' && saveChar(nextChar()) == 'l' && saveChar(nextChar()) == 's' && saveChar(nextChar()) == 'e') {
                                text = FALSE;
                            }
                        } else if (nextEvent == 't') { // true
                            if (saveChar(nextChar()) == 'r' && saveChar(nextChar()) == 'u' && saveChar(nextChar()) == 'e') {
                                text = TRUE;
                            }
                        } else if (nextEvent == 'n') { // null
                            if (saveChar(nextChar()) == 'u' && saveChar(nextChar()) == 'l' && saveChar(nextChar()) == 'l') { //NOSONAR
                                text = NULL;
                            }
                        } else if ((nextEvent >= '0' && nextEvent <= '9') || nextEvent == '-' || nextEvent == '+') { // number.
                            isNumber = true;
                            readNumber(ch, nextTokenValueType);
                            //    } else if (nextEvent == 'F') { // "False", "FALSE" // possible? TODO
                            //    } else if (nextEvent == 'T') { // "True", "TRUE" // possible? TODO
                            //    } else if (nextEvent == 'N') { // "Null", "NULL" // possible? TODO
                        }

                        if (isNumber) {
                            // done in readNumber...
                        } else {
                            if (strBeginIndex >= strEndIndex) {
                                refill();
                            }

                            while (strBeginIndex < strEndIndex) {
                                ch = strValue[strBeginIndex++];

                                if (ch < 128) {
                                    nextEvent = charEvents[ch];

                                    if (nextEvent > 0 && nextEvent < 32) {
                                        endIndexForText = strBeginIndex - 1;
                                        return nextEvent;
                                    }
                                }

                                if (saveChar(ch) > 32) {
                                    text = null;
                                }

                                if (strBeginIndex >= strEndIndex) {
                                    refill();
                                }
                            }

                            endIndexForText = strBeginIndex;
                            nextEvent = -1;
                        }

                        return nextEvent;
                    }
                } else {
                    saveChar(ch);
                }

                if (strBeginIndex >= strEndIndex) {
                    refill();
                }
            }
        }

        endIndexForText = strBeginIndex;
        nextEvent = -1;

        return nextEvent;
    }

    @Override
    protected void readNumber(final int firstChar, final Type<?> nextTokenValueType) {
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

        final boolean negative = firstChar == '-';
        long ret = firstChar == '-' || firstChar == '+' ? 0 : (firstChar - '0');

        int pointPosition = -1;
        int cnt = ret == 0 ? 0 : 1;
        int ch = 0;
        int typeFlag = 0;

        while ((ch = nextChar()) >= 0) {
            if (ch >= '0' && ch <= '9') {
                if (cnt < MAX_PARSABLE_NUM_LEN || (cnt == MAX_PARSABLE_NUM_LEN && ret <= (Long.MAX_VALUE - (ch - '0')) / 10)) {
                    ret = ret * 10 + (ch - '0');

                    if (ret > 0 || pointPosition > 0) {
                        cnt++;
                    }
                } else {
                    cnt += 2; // So cnt will > MAX_PARSABLE_NUM_LEN + 1 to skip the result.
                }
            } else if (ch == '.' && pointPosition < 0) {
                if (cnt == 0) {
                    cnt = 1;
                }

                pointPosition = cnt;
            } else {
                do {
                    if (ch < 128) {
                        nextEvent = charEvents[ch];

                        if (nextEvent > 0 && nextEvent < 32) {
                            break;
                        }
                    }

                    ch = saveChar(ch);

                    if (nextEvent > 0 && typeFlag == 0 && (ch == 'l' || ch == 'L' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D')) {
                        typeFlag = ch;
                    } else if (ch > 32) { // ignore <= 32 whitespace chars.
                        cnt = -1; // TODO can't parse here. leave it Numbers.createNumber(...).
                    }
                } while ((ch = nextChar()) >= 0);

                break;
            }
            // why need this? For example, "1233993E323" or "8888..." (a very long big integer with 100_000 digits, longer than buffer size),
            // what will happen to prefix "1233993" before "E"? They won't be saved if nextChar > 0
            if (nextChar > 0) {
                // saveChar(ch);

                // for better performance
                if (nextChar >= cbufLen) {
                    enlargeCharBuffer();
                }

                cbuf[nextChar++] = (char) ch;
            }
        }

        if (nextEvent > 0 && nextEvent < 32) {
            endIndexForText = strBeginIndex - 1;
        } else {
            endIndexForText = strBeginIndex;
            nextEvent = -1;
        }

        if (cnt >= 0 && cnt <= MAX_PARSABLE_NUM_LEN + 1 && pointPosition != cnt) {
            if (negative) {
                ret = -ret;
            }

            if (nextTokenValueType.isNumber() || typeFlag > 0) {
                if (pointPosition > 0) {
                    if (nextTokenValueType.isFloat() || typeFlag == 'f' || typeFlag == 'F') {
                        numValue = (float) (((double) ret) / POWERS_OF_TEN[cnt - pointPosition]);
                    } else { // ignore 'l' or 'L' if it's specified.
                        numValue = ((double) ret) / POWERS_OF_TEN[cnt - pointPosition];
                    }
                } else if (nextTokenValueType.isFloat() || typeFlag == 'f' || typeFlag == 'F') {
                    numValue = (float) ret;
                } else if (nextTokenValueType.isDouble() || typeFlag == 'd' || typeFlag == 'D') {
                    numValue = (double) ret;
                } else { // typeFlag == 'l' or 'L'.
                    numValue = ret;
                }
            } else {
                if (pointPosition > 0) {
                    numValue = ((double) ret) / POWERS_OF_TEN[cnt - pointPosition];
                } else if (ret >= Integer.MIN_VALUE && ret <= Integer.MAX_VALUE) {
                    numValue = (int) ret;
                } else {
                    numValue = ret;
                }
            }
        }
        //    else { // for debug
        //        logger.warn("#######: " + getText());
        //        System.out.println("#######: " + getText());
        //    }
    }

    @Override
    protected int saveChar(final int ch) {
        if (ch < 0) {
            return ch;
        }

        return super.saveChar(ch);
    }

    protected int nextChar() {
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

        if (strBeginIndex >= strEndIndex) {
            return -1;
        }

        return strValue[strBeginIndex++];
    }

    @Override
    protected char readEscapeCharacter() {
        if (strBeginIndex >= strEndIndex) {
            refill();
        }

        final int escaped = strValue[strBeginIndex++];

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
                        result += (char) (c - '0');
                    } else if ((c >= 'a') && (c <= 'f')) {
                        result += (char) (c - 'a' + 10);
                    } else if ((c >= 'A') && (c <= 'F')) {
                        result += (char) (c - 'A' + 10);
                    } else {
                        throw new ParseException("Number format exception: \\u" + String.valueOf(cbuf));
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

    protected void refill() {
        if (strBeginIndex >= strEndIndex) {
            if (nextChar == 0) {
                endIndexForText = strBeginIndex;

                if (endIndexForText > startIndexForText) {
                    N.copy(strValue, startIndexForText, cbuf, 0, endIndexForText - startIndexForText);
                    nextChar = endIndexForText - startIndexForText;
                } else {
                    startIndexForText = 0;
                }
            }

            try {
                final int n = IOUtil.read(reader, strValue, 0, strValue.length);

                if (n > 0) {
                    strBeginIndex = 0;
                    strEndIndex = n;
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
