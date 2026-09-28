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

package com.landawn.abacus.type;

import java.io.IOException;

import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.EscapeUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.PrimitiveList;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for primitive list types in the type system.
 * <p>
 * This class provides common functionality for handling specialized list implementations
 * that store primitive values directly without boxing overhead.
 * Examples include {@code IntList}, {@code DoubleList}, {@code BooleanList}, etc.
 * </p>
 *
 * <p>Built-in formatting can read the logical prefix of a list's backing array when the destination
 * and serialization configuration cannot invoke application callbacks. Custom array handlers and
 * callback-capable destinations/configurations receive a logical-element snapshot, preserving the
 * output if a callback mutates the list during serialization. Concurrent mutation is not supported.</p>
 *
 * @param <T> the primitive list type (e.g., {@code IntList}, {@code DoubleList}, {@code BooleanList})
 */
public abstract class AbstractPrimitiveListType<T extends PrimitiveList<?, ?, ?>> extends AbstractType<T> {

    // Preserve the previous array snapshot whenever a destination/config callback can mutate the source list.
    static boolean canWriteDirectly(final Appendable destination) {
        return destination instanceof StringBuilder || destination instanceof StringBuffer
                || destination instanceof CharacterWriter writer && writer.isWriteCallbackFree();
    }

    static boolean canWriteDirectly(final CharacterWriter writer, final JsonXmlSerConfig<?> config) {
        return writer.isWriteCallbackFree() && (config == null || config.getClass() == JsonSerConfig.class || config.getClass() == XmlSerConfig.class);
    }

    /**
     * Constructs an {@code AbstractPrimitiveListType} with the specified type name.
     *
     * @param typeName the name of the primitive list type (e.g., "IntList", "DoubleList", "BooleanList")
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractPrimitiveListType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents a primitive list — a specialized
     * list implementation that stores primitive values directly without boxing overhead.
     *
     * @return {@code true}
     */
    @Override
    public boolean isPrimitiveList() {
        return true;
    }

    static String stringOf(final char[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(length, 5));

        try {
            sb.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    sb.append(ELEMENT_SEPARATOR);
                }

                sb.append(SK.SINGLE_QUOTE);

                final char ch = x[i];

                /*
                 * EscapeUtil.escapeEcmaScript leaves printable ASCII (' ' .. 0x7f) unchanged except for the four
                 * characters it rewrites (', ", \ and /); append those chars directly instead of allocating a
                 * one-char String, a StringWriter and the escaped result per element.
                 */
                if (ch >= ' ' && ch <= 0x7f && ch != '\'' && ch != '"' && ch != '\\' && ch != '/') {
                    sb.append(ch);
                } else {
                    sb.append(EscapeUtil.escapeEcmaScript(String.valueOf(ch)));
                }

                sb.append(SK.SINGLE_QUOTE);
            }

            sb.append(SK._BRACKET_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void appendTo(final Appendable appendable, final char[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i]);
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final char[] x, final int length, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (writer instanceof BufferedXmlWriter) {
            // XML text must preserve array delimiters and controls until valueOf decodes them.
            writer.writeCharacter(stringOf(x, length));
        } else {
            writer.write(SK._BRACKET_L);

            final char charQuotation = (config == null) ? SK.CHAR_ZERO : config.getCharQuotation();

            if (charQuotation > 0) {
                for (int i = 0, len = length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    writer.write(charQuotation);

                    if (x[i] == '\'' && charQuotation == '\'') {
                        writer.write('\\');
                    }

                    writer.writeCharacter(x[i]);
                    writer.write(charQuotation);
                }
            } else {
                for (int i = 0, len = length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    writer.writeCharacter(x[i]);
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final boolean[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final boolean[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i] ? TRUE_STRING : FALSE_STRING);
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final boolean[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i] ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final byte[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final byte[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final byte[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final short[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final short[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final short[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final int[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final int[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final int[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.writeInt(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final long[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final long[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final long[] x, final int length, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                if (config != null && config.isWriteLongAsString() && config.getStringQuotation() != 0) {
                    final char quotation = config.getStringQuotation();
                    writer.write(quotation);
                    writer.write(x[i]);
                    writer.write(quotation);
                } else {
                    writer.write(x[i]);
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final float[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final float[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final float[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    static String stringOf(final double[] x, final int length) {
        if (x == null) {
            return null; // NOSONAR
        } else if (length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    static void appendTo(final Appendable appendable, final double[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    static void serializeTo(final CharacterWriter writer, final double[] x, final int length) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }
}
