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
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
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

class JSONStringReader extends AbstractJSONReader {
    private static final Logger logger = LoggerFactory.getLogger(JSONStringReader.class);

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

        defaultOptionals.put(java.util.Optional.class, java.util.Optional.empty());
        defaultOptionals.put(java.util.OptionalInt.class, java.util.OptionalInt.empty());
        defaultOptionals.put(java.util.OptionalLong.class, java.util.OptionalLong.empty());
        defaultOptionals.put(java.util.OptionalDouble.class, java.util.OptionalDouble.empty());
    }

    final Reader reader;

    final char[] strValue;

    int strEndIndex = 0;

    int strBeginIndex = 0;

    int startIndexForText = 0;

    int endIndexForText = 0;

    char[] cbuf;

    int cbufLen = 0;

    int nextEvent = -1;

    int nextChar = 0;

    String text = null;

    Number numValue = null;

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
        strBeginIndex = beginIndex;
        strEndIndex = endIndex;
        this.cbuf = cbuf;
        cbufLen = this.cbuf.length;
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
    public static JSONReader parse(final String str, final int beginIndex, final int endIndex, final char[] cbuf) {
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
     * TODO performance improvement: Refer to the test above. TODO limitation: the maximum length of property value is
     * the buffer size.
     *
     * @return
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    @Override
    public int nextToken() throws UncheckedIOException {
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

                        if (nextEvent == 'f' && strEndIndex - strBeginIndex > 3) { // false
                            if (saveChar(strValue[strBeginIndex++]) == 'a' && saveChar(strValue[strBeginIndex++]) == 'l'
                                    && saveChar(strValue[strBeginIndex++]) == 's' && saveChar(strValue[strBeginIndex++]) == 'e') {
                                text = FALSE;
                            }
                        } else if (nextEvent == 't' && strEndIndex - strBeginIndex > 2) { // true
                            if (saveChar(strValue[strBeginIndex++]) == 'r' && saveChar(strValue[strBeginIndex++]) == 'u'
                                    && saveChar(strValue[strBeginIndex++]) == 'e') {
                                text = TRUE;
                            }
                        } else if (nextEvent == 'n' && strEndIndex - strBeginIndex > 2) { // null
                            if (saveChar(strValue[strBeginIndex++]) == 'u' && saveChar(strValue[strBeginIndex++]) == 'l'
                                    && saveChar(strValue[strBeginIndex++]) == 'l') { //NOSONAR
                                text = NULL;
                            }
                        } else if ((nextEvent >= '0' && nextEvent <= '9') || nextEvent == '-' || nextEvent == '+') { // number.
                            isNumber = true;
                            readNumber(ch);
                        }

                        //    } else if (nextEvent == 'F') { // "False", "FALSE" // possible? TODO
                        //    } else if (nextEvent == 'T') { // "True", "TRUE" // possible? TODO
                        //    } else if (nextEvent == 'N') { // "Null", "NULL" // possible? TODO
                        //    }

                        if (isNumber) {
                            // done in readNumber...
                        } else {
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
                            }

                            endIndexForText = strBeginIndex;
                            nextEvent = -1;
                        }

                        return nextEvent;
                    }
                } else {
                    saveChar(ch);
                }
            }
        }

        endIndexForText = strBeginIndex;
        nextEvent = -1;

        return nextEvent;
    }

    /**
     * Checks for text.
     *
     * @return {@code true}, if successful
     */
    //
    @Override
    public boolean hasText() {
        return text != null || numValue != null || (nextChar > 0) || (endIndexForText > startIndexForText);
    }

    protected void readNumber(final int firstChar) {
        final boolean negative = firstChar == '-';
        long ret = firstChar == '-' || firstChar == '+' ? 0 : (firstChar - '0');

        int pointPositoin = -1;
        int cnt = ret == 0 ? 0 : 1;
        int ch = 0;
        int typeFlag = 0;

        while (strBeginIndex < strEndIndex) {
            ch = strValue[strBeginIndex++];

            if (ch >= '0' && ch <= '9') {
                if (cnt < MAX_PARSABLE_NUM_LEN || (cnt == MAX_PARSABLE_NUM_LEN && ret <= (Long.MAX_VALUE - (ch - '0')) / 10)) {
                    ret = ret * 10 + (ch - '0');

                    if (ret > 0 || pointPositoin > 0) {
                        cnt++;
                    }
                } else {
                    cnt += 2; // So cnt will > MAX_PARSABLE_NUM_LEN + 1 to skip result.
                }
            } else if (ch == '.' && pointPositoin < 0) {
                if (cnt == 0) {
                    cnt = 1;
                }

                pointPositoin = cnt;
            } else {
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

                while (strBeginIndex < strEndIndex) {
                    ch = strValue[strBeginIndex++];

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
                }

                break;
            }
        }

        if (nextEvent > 0 && nextEvent < 32) {
            endIndexForText = strBeginIndex - 1;
        } else {
            endIndexForText = strBeginIndex;
            nextEvent = -1;
        }

        if (cnt >= 0 && cnt <= MAX_PARSABLE_NUM_LEN + 1 && pointPositoin != cnt) {
            if (negative) {
                ret = -ret;
            }

            if (typeFlag > 0) {
                if (pointPositoin > 0) {
                    if (typeFlag == 'f' || typeFlag == 'F') {
                        numValue = (float) (((double) ret) / POWERS_OF_TEN[cnt - pointPositoin]);
                    } else { // ignore 'l' or 'L' if it's specified.
                        numValue = ((double) ret) / POWERS_OF_TEN[cnt - pointPositoin];
                    }
                } else if (typeFlag == 'f' || typeFlag == 'F') {
                    numValue = (float) ret;
                } else if (typeFlag == 'd' || typeFlag == 'D') {
                    numValue = (double) ret;
                } else { // typeFlag == 'l' or 'L'.
                    numValue = ret;
                }
            } else {
                if (pointPositoin > 0) {
                    numValue = ((double) ret) / POWERS_OF_TEN[cnt - pointPositoin];
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

    protected int saveChar(int ch) {
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
                if (startIndexForText == (strBeginIndex - 1)) {
                    startIndexForText++;
                } else {
                    // skip whitespace char.

                    saveToBuffer();
                }
            } else if (ch == WD._BACKSLASH) {
                saveToBuffer();
                // strStart++;
                ch = readEscapeCharacter();

                if (ch < 33) {
                    // skip whitespace char.
                } else {
                    cbuf[nextChar++] = (char) ch;
                }
            }
        }

        return ch;
    }

    protected void saveToBuffer() {
        endIndexForText = strBeginIndex - 1;

        if (endIndexForText - startIndexForText + 1 >= cbufLen) {
            enlargeCharBuffer();
        }

        N.copy(strValue, startIndexForText, cbuf, 0, endIndexForText - startIndexForText);

        nextChar = endIndexForText - startIndexForText;
    }

    protected void throwExceptionDueToUnexpectedNonStringToken() {
        throw new ParseException(
                "\"false\", \"true\", \"null\" or a number is expected in or before \"" + (nextChar > 0 ? String.valueOf(cbuf, 0, N.min(32, nextChar))
                        : String.valueOf(strValue, strBeginIndex - 1, N.min(32, strEndIndex - strBeginIndex + 1))));
    }

    /**
     * Gets the text.
     *
     * @return
     */
    @Override
    public String getText() {
        if (text != null) {
            return text;
        }

        return (nextChar > 0) ? String.valueOf(cbuf, 0, nextChar) : String.valueOf(strValue, startIndexForText, endIndexForText - startIndexForText);
    }

    /**
     *
     * @param <T>
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(final Type<? extends T> type) {
        if (nextEvent != END_QUOTATION_D && nextEvent != END_QUOTATION_S) {
            if (numValue != null) {
                if (type.isObjectType() || type.clazz().equals(numValue.getClass())) {
                    return (T) numValue;
                } else if (type.isNumber()) {
                    return (T) Numbers.convert(numValue, (Type<Number>) type);
                } else if (type.isDate() || type.isCalendar() || type.isJodaDateTime()) {
                    return type.valueOf(numValue);
                } else {
                    if (text != null) {
                        return type.valueOf(text);
                    } else {
                        return N.convert(numValue, type);
                    }
                }
            } else if (text != null) {
                if (text.equals(NULL)) {
                    return type.isOptionalOrNullable() ? (T) defaultOptionals.get(type.clazz()) : null;
                } else if ((text.equals(FALSE) || text.equals(TRUE)) && (type.isBoolean() || type.isObjectType())) {
                    return (T) (text.equals(FALSE) ? Boolean.FALSE : Boolean.TRUE);
                } else {
                    return type.valueOf(text);
                }
            }

            if (type.isObjectType()) {
                final String str = String
                        .valueOf(nextChar > 0 ? N.copyOfRange(cbuf, 0, nextChar) : N.copyOfRange(strValue, startIndexForText, endIndexForText));

                if (Strings.isEmpty(str)) {
                    return (T) str;
                }

                try {
                    final Number num = Numbers.createNumber(str);

                    if (num instanceof Float) {
                        final char lastChar = str.charAt(str.length() - 1);

                        if (!(lastChar == 'f' || lastChar == 'F')) {
                            return (T) Double.valueOf(Numbers.toDouble(num));
                        }
                    }

                    return (T) num;
                } catch (final Exception e) {
                    // ignore;
                    if (logger.isWarnEnabled()) {
                        logger.warn("Failed to parse: " + str + " to Number");
                    }
                }

                return (T) str;
            }
        }

        if (nextChar > 0) {
            return type.valueOf(cbuf, 0, nextChar);
        } else {
            return type.valueOf(strValue, startIndexForText, endIndexForText - startIndexForText);
        }
    }

    /**
     * Read prop info.
     *
     * @param symbolReader
     * @return
     */
    @Override
    public PropInfo readPropInfo(final SymbolReader symbolReader) {
        return (nextChar > 0) ? symbolReader.readPropInfo(cbuf, 0, nextChar) : symbolReader.readPropInfo(strValue, startIndexForText, endIndexForText);
    }

    /**
     *
     */
    @Override
    public void close() throws UncheckedIOException {
        if (reader != null) {
            try {
                reader.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
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
    protected char readEscapeCharacter() {
        final int escaped = strValue[strBeginIndex++];

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
