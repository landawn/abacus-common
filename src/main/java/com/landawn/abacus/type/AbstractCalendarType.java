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

package com.landawn.abacus.type;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.DateUtil;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractCalendarType<T extends Calendar> extends AbstractType<T> {

    protected AbstractCalendarType(String typeName) {
        super(typeName);
    }

    /**
     * Checks if is calendar.
     *
     * @return true, if is calendar
     */
    @Override
    public boolean isCalendar() {
        return true;
    }

    /**
     * Checks if is comparable.
     *
     * @return true, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     *
     * @param calendar
     * @return
     */
    @Override
    public String stringOf(Calendar calendar) {
        return (calendar == null) ? null : DateUtil.format(calendar);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Calendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : DateUtil.createTimestamp(x));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Calendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : DateUtil.createTimestamp(x));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, T x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            DateUtil.format(writer, x, null, null);
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
    public void writeCharacter(CharacterWriter writer, T x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                DateUtil.format(writer, x);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.getTimeInMillis());

                        break;

                    case ISO_8601_DATETIME:
                        DateUtil.format(writer, x, DateUtil.ISO_8601_DATETIME_FORMAT, null);

                        break;

                    case ISO_8601_TIMESTAMP:
                        DateUtil.format(writer, x, DateUtil.ISO_8601_TIMESTAMP_FORMAT, null);

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
