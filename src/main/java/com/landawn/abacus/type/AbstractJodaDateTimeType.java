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

import org.joda.time.base.AbstractInstant;
import org.joda.time.format.DateTimeFormatter;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 *
 * @param <T>
 */
public abstract class AbstractJodaDateTimeType<T extends AbstractInstant> extends AbstractType<T> {

    protected static final DateTimeFormatter jodaISO8601DateTimeFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_DATE_TIME_FORMAT);

    protected static final DateTimeFormatter jodaISO8601TimestampFT = org.joda.time.format.DateTimeFormat.forPattern(Dates.ISO_8601_TIMESTAMP_FORMAT);

    protected AbstractJodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if is joda date time.
     *
     * @return {@code true}, if is joda date time
     */
    @Override
    public boolean isJodaDateTime() {
        return true;
    }

    /**
     * Checks if is comparable.
     *
     * @return {@code true}, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Checks if is non quoted csv type.
     *
     * @return {@code true}, if is non quoted csv type
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : jodaISO8601TimestampFT.print(x);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            jodaISO8601TimestampFT.printTo(appendable, x);
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("null")
    @Override
    public void writeCharacter(final CharacterWriter writer, final T x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                jodaISO8601TimestampFT.printTo(writer, x);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.getMillis());

                        break;

                    case ISO_8601_DATE_TIME:
                        jodaISO8601DateTimeFT.printTo(writer, x);

                        break;

                    case ISO_8601_TIMESTAMP:
                        jodaISO8601TimestampFT.printTo(writer, x);

                        break;

                    default:
                        throw new RuntimeException("unsupported operation");
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
