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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.DateUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ZonedDateTimeType extends AbstractType<ZonedDateTime> {

    static final ZoneId DEFAULT_TIME_ZONE = ZoneId.systemDefault();

    static final DateTimeFormatter iso8601DateTimeFT = DateTimeFormatter.ofPattern(DateUtil.ISO_8601_DATETIME_FORMAT).withZone(DEFAULT_TIME_ZONE);

    static final DateTimeFormatter iso8601TimestampFT = DateTimeFormatter.ofPattern(DateUtil.ISO_8601_TIMESTAMP_FORMAT).withZone(DEFAULT_TIME_ZONE);

    public static final String ZONED_DATE_TIME = ZonedDateTime.class.getSimpleName();

    ZonedDateTimeType() {
        super(ZONED_DATE_TIME);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<ZonedDateTime> clazz() {
        return ZonedDateTime.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(ZonedDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public ZonedDateTime valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return ZonedDateTime.now();
        }

        if (str.charAt(4) != '-') {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_TIME_ZONE);
            } catch (NumberFormatException e) {
                // ignore;
            }
        }

        return str.length() == 20 ? ZonedDateTime.parse(str, iso8601DateTimeFT)
                : (str.length() == 24 ? ZonedDateTime.parse(str, iso8601TimestampFT) : ZonedDateTime.parse(str));
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return {@code null} if {@code ((cbuf == null) || (len == 0))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public ZonedDateTime valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (cbuf[offset + 4] != '-') {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(AbstractType.parseLong(cbuf, offset, len)), DEFAULT_TIME_ZONE);
            } catch (NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public ZonedDateTime get(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_TIME_ZONE);
    }

    /**
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public ZonedDateTime get(ResultSet rs, String columnName) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_TIME_ZONE);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, ZonedDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     *
     * @param stmt
     * @param columnName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String columnName, ZonedDateTime x) throws SQLException {
        stmt.setTimestamp(columnName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, ZonedDateTime x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(stringOf(x));
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
    public void writeCharacter(CharacterWriter writer, ZonedDateTime x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                writer.write(stringOf(x));
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.toInstant().toEpochMilli());

                        break;

                    case ISO_8601_DATETIME:
                        writer.write(x.format(iso8601DateTimeFT));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(stringOf(x));

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
