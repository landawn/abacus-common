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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class XMLGregorianCalendarType extends AbstractType<XMLGregorianCalendar> {

    public static final String XML_GREGORIAN_CALENDAR = XMLGregorianCalendar.class.getSimpleName();

    XMLGregorianCalendarType() {
        super(XML_GREGORIAN_CALENDAR);
    }

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
    public XMLGregorianCalendar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentXMLGregorianCalendar() : Dates.parseXMLGregorianCalendar(str));
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @MayReturnNull
    @Override
    public XMLGregorianCalendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createXMLGregorianCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
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
    public String stringOf(final XMLGregorianCalendar x) {
        return (x == null) ? null : Dates.format(x);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public XMLGregorianCalendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Dates.createXMLGregorianCalendar(rs.getTimestamp(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public XMLGregorianCalendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        return Dates.createXMLGregorianCalendar(rs.getTimestamp(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(columnIndex, (x == null) ? null : Dates.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final XMLGregorianCalendar x) throws SQLException {
        stmt.setTimestamp(parameterName, (x == null) ? null : Dates.createTimestamp(x.toGregorianCalendar()));
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final XMLGregorianCalendar x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            Dates.formatTo(x, null, null, appendable);
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
    public void writeCharacter(final CharacterWriter writer, final XMLGregorianCalendar x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                Dates.formatTo(x, null, null, writer);
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(String.valueOf(x.toGregorianCalendar().getTimeInMillis()));

                        break;

                    case ISO_8601_DATE_TIME:
                        Dates.formatTo(x, Dates.ISO_8601_DATE_TIME_FORMAT, null, writer);

                        break;

                    case ISO_8601_TIMESTAMP:
                        Dates.formatTo(x, Dates.ISO_8601_TIMESTAMP_FORMAT, null, writer);

                        break;

                    default:
                        throw new RuntimeException("Unsupported operation");
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
