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

package com.landawn.abacus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * the sql scripts are configured in xml file and mapped to short ids referenced in program. for example: <br>
 * {@code <sqlMapper>} <br>
 * {@code <sql id="findAccountById">select * from account where id=1</sql>} <br>
 * {@code <sql id="updateAccountNameById">update account set name=? where id=?</sql>} <br>
 * {@code </sqlMapper>}
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class SQLMapper {

    public static final String SQL_MAPPER = "sqlMapper";

    public static final String SQL = "sql";

    public static final String ID = "id";

    public static final String BATCH_SIZE = "batchSize";

    public static final String FETCH_SIZE = "fetchSize";

    public static final String RESULT_SET_TYPE = "resultSetType";

    public static final ImmutableMap<String, Integer> RESULT_SET_TYPE_MAP = ImmutableMap.of("FORWARD_ONLY", ResultSet.TYPE_FORWARD_ONLY, "SCROLL_INSENSITIVE",
            ResultSet.TYPE_SCROLL_INSENSITIVE, "SCROLL_SENSITIVE", ResultSet.TYPE_SCROLL_SENSITIVE);

    public static final String TIMEOUT = "timeout";

    public static final int MAX_ID_LENGTH = 64;

    private final Map<String, ParsedSql> sqlMap = new LinkedHashMap<>();

    private final Map<String, ImmutableMap<String, String>> attrsMap = new HashMap<>();

    public SQLMapper() {
    }

    /**
     *
     * @param filePath it could be multiple file paths separated by ',' or ';'
     * @return
     */
    public static SQLMapper fromFile(String filePath) {
        String[] filePaths = Splitter.with(WD.COMMA).trimResults().splitToArray(filePath);

        if (filePaths.length == 1) {
            filePaths = Splitter.with(WD.SEMICOLON).trimResults().splitToArray(filePath);
        }

        final SQLMapper sqlMapper = new SQLMapper();

        for (String subFilePath : filePaths) {
            final File file = Configuration.formatPath(Configuration.findFile(subFilePath));

            InputStream is = null;

            try {
                is = new FileInputStream(file);

                Document doc = XMLUtil.createDOMParser(true, true).parse(is);
                NodeList sqlMapperEle = doc.getElementsByTagName(SQLMapper.SQL_MAPPER);

                if (0 == sqlMapperEle.getLength()) {
                    throw new RuntimeException("There is no 'sqlMapper' element. ");
                }

                List<Element> sqlElementList = XMLUtil.getElementsByTagName((Element) sqlMapperEle.item(0), SQL);

                for (Element sqlElement : sqlElementList) {
                    Map<String, String> attrMap = XMLUtil.readAttributes(sqlElement);

                    sqlMapper.add(attrMap.remove(ID), Configuration.getTextContent(sqlElement), attrMap);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (SAXException e) {
                throw new ParseException(e);
            } finally {
                IOUtil.close(is);
            }
        }

        return sqlMapper;
    }

    public Set<String> keySet() {
        return sqlMap.keySet();
    }

    /**
     *
     * @param id
     * @return
     */
    public ParsedSql get(String id) {
        if (N.isNullOrEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return null;
        }

        return sqlMap.get(id);
    }

    /**
     * Gets the attrs.
     *
     * @param id
     * @return
     */
    public ImmutableMap<String, String> getAttrs(String id) {
        if (N.isNullOrEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return null;
        }

        return attrsMap.get(id);
    }

    /**
     *
     * @param id
     * @param sql
     * @return
     */
    public ParsedSql add(String id, ParsedSql sql) {
        checkId(id);

        return sqlMap.put(id, sql);
    }

    /**
     *
     * @param id
     * @param sql
     * @param attrs
     */
    public void add(String id, String sql, Map<String, String> attrs) {
        checkId(id);

        sqlMap.put(id, ParsedSql.parse(sql));
        attrsMap.put(id, ImmutableMap.copyOf(attrs));
    }

    /**
     *
     * @param id
     */
    private void checkId(String id) {
        N.checkArgNotNullOrEmpty(id, "id");

        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Id: " + id + " is too long. The maximum length for id is: " + MAX_ID_LENGTH);
        }

        if (sqlMap.containsKey(id)) {
            throw new IllegalArgumentException(id + " already exists with sql: " + sqlMap.get(id));
        }
    }

    /**
     *
     * @param id
     */
    public void remove(String id) {
        if (N.isNullOrEmpty(id) || id.length() > MAX_ID_LENGTH) {
            return;
        }

        sqlMap.remove(id);
    }

    public SQLMapper copy() {
        final SQLMapper copy = new SQLMapper();

        copy.sqlMap.putAll(this.sqlMap);
        copy.attrsMap.putAll(this.attrsMap);

        return copy;
    }

    /**
     *
     * @param file
     */
    public void saveTo(File file) {
        OutputStream os = null;

        try {
            Document doc = XMLUtil.createDOMParser(true, true).newDocument();
            Element sqlMapperNode = doc.createElement(SQLMapper.SQL_MAPPER);

            for (String id : sqlMap.keySet()) {
                Element sqlNode = doc.createElement(SQL);
                sqlNode.setAttribute(ID, id);

                if (!N.isNullOrEmpty(attrsMap.get(id))) {
                    Map<String, String> attrs = attrsMap.get(id);

                    for (String key : attrs.keySet()) {
                        sqlNode.setAttribute(key, attrs.get(key));
                    }
                }

                Text sqlText = doc.createTextNode(sqlMap.get(id).sql());
                sqlNode.appendChild(sqlText);
                sqlMapperNode.appendChild(sqlNode);
            }

            doc.appendChild(sqlMapperNode);

            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);

            XMLUtil.transform(doc, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    public boolean isEmpty() {
        return sqlMap.isEmpty();
    }

    @Override
    public int hashCode() {
        return sqlMap.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof SQLMapper && N.equals(((SQLMapper) obj).sqlMap, sqlMap));
    }

    @Override
    public String toString() {
        return sqlMap.toString();
    }
}
