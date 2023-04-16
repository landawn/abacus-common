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

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.DateUtil;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class XMLGregorianCalendarType extends AbstractType<XMLGregorianCalendar> {

    public static final String XML_GREGORIAN_CALENDAR = XMLGregorianCalendar.class.getSimpleName();

    XMLGregorianCalendarType() {
        super(XML_GREGORIAN_CALENDAR);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<XMLGregorianCalendar> clazz() {
        return XMLGregorianCalendar.class;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public XMLGregorianCalendar valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : (N.equals(str, SYS_TIME) ? DateUtil.currentXMLGregorianCalendar() : DateUtil.parseXMLGregorianCalendar(str));
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public XMLGregorianCalendar valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null;
        }

        if (cbuf[offset + 4] != '-') {
            try {
                return DateUtil.createXMLGregorianCalendar(parseLong(cbuf, offset, len));
            } catch (NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(XMLGregorianCalendar x) {
        return (x == null) ? null : DateUtil.format(x);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public XMLGregorianCalendar get(ResultSet rs, int columnIndex) throws SQLException {
        return DateUtil.createXMLGregorianCalendar(rs.getTimestamp(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public XMLGregorianCalendar get(ResultSet rs, String columnLabel) throws SQLException {
        return DateUtil.createXMLGregorianCalendar(rs.getTimestamp(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : DateUtil.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : DateUtil.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, XMLGregorianCalendar x) throws IOException {
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
    public void writeCharacter(CharacterWriter writer, XMLGregorianCalendar x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                DateUtil.format(writer, x, null, null);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(String.valueOf(x.toGregorianCalendar().getTimeInMillis()));

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
