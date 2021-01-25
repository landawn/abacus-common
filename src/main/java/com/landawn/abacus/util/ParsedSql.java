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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.PoolableWrapper;

/**
 * 
 * @author haiyangl
 *
 */
public final class ParsedSql {

    private static final int EVICT_TIME = 60 * 1000;

    private static final int LIVE_TIME = 24 * 60 * 60 * 1000;

    private static final int MAX_IDLE_TIME = 24 * 60 * 60 * 1000;

    private static final Set<String> opSqlPrefixSet = N.asSet(WD.INSERT, WD.SELECT, WD.UPDATE, WD.DELETE, WD.WITH, WD.MERGE);

    private static final int factor = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    private static final KeyedObjectPool<String, PoolableWrapper<ParsedSql>> pool = PoolFactory.createKeyedObjectPool(1000 * factor, EVICT_TIME);

    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    private static final String RIGHT_OF_IBATIS_NAMED_PARAMETER = "}";

    private static final String PREFIX_OF_COUCHBASE_NAMED_PARAMETER = "$";

    private static final char _PREFIX_OF_COUCHBASE_NAMED_PARAMETER = PREFIX_OF_COUCHBASE_NAMED_PARAMETER.charAt(0);

    private final String sql;

    private final String parameterizedSql;

    private String couchbaseParameterizedSql;

    private final ImmutableList<String> namedParameters;

    private ImmutableList<String> couchbaseNamedParameters;

    private int parameterCount;

    private int couchbaseParameterCount;

    @SuppressWarnings({ "unchecked" })
    private ParsedSql(String sql) {
        this.sql = sql.trim();

        final List<String> words = SQLParser.parse(this.sql);

        boolean isOpSqlPrefix = false;
        for (String word : words) {
            if (N.notNullOrEmpty(word) && !(word.equals(" ") || word.startsWith("--") || word.startsWith("/*"))) {
                isOpSqlPrefix = opSqlPrefixSet.contains(word.toUpperCase());
                break;
            }
        }

        final List<String> namedParameterList = new ArrayList<>();

        if (isOpSqlPrefix) {
            final StringBuilder sb = Objectory.createStringBuilder();

            for (String word : words) {
                if (word.equals(WD.QUESTION_MARK)) {
                    if (namedParameterList.size() > 0) {
                        throw new IllegalArgumentException("can't mix '?' with name parameter ':propName' or '#{propName}' in the same sql script");
                    }
                    parameterCount++;
                } else if (word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) && word.endsWith(RIGHT_OF_IBATIS_NAMED_PARAMETER)) {
                    namedParameterList.add(word.substring(2, word.length() - 1));

                    word = WD.QUESTION_MARK;
                    parameterCount++;
                } else if (word.length() >= 2 && word.charAt(0) == _PREFIX_OF_NAMED_PARAMETER && isValidNamedParameterChar(word.charAt(1))) {
                    namedParameterList.add(word.substring(1));

                    word = WD.QUESTION_MARK;
                    parameterCount++;
                }

                sb.append(word);
            }

            parameterizedSql = sb.toString();
            namedParameters = ImmutableList.of(namedParameterList);

            Objectory.recycle(sb);
        } else {
            parameterizedSql = sql;
            namedParameters = ImmutableList.empty();
        }
    }

    /**
     *
     * @param sql
     * @return
     */
    public static ParsedSql parse(String sql) {
        ParsedSql result = null;
        PoolableWrapper<ParsedSql> w = pool.get(sql);

        if ((w == null) || (w.value() == null)) {
            synchronized (pool) {
                result = new ParsedSql(sql);
                pool.put(sql, PoolableWrapper.of(result, LIVE_TIME, MAX_IDLE_TIME));
            }
        } else {
            result = w.value();
        }

        return result;
    }

    /**
     * Gets the named SQL.
     *
     * @return
     */
    public String sql() {
        return sql;
    }

    /**
     * Gets the parameterized SQL.
     *
     * @return
     */
    public String getParameterizedSql() {
        return parameterizedSql;
    }

    /**
     * Gets the parameterized SQL.
     *
     * @param isForCouchbase
     * @return
     */
    public String getParameterizedSql(boolean isForCouchbase) {
        if (isForCouchbase) {
            if (N.isNullOrEmpty(couchbaseParameterizedSql)) {
                parseForCouchbase();
            }

            return couchbaseParameterizedSql;
        } else {
            return parameterizedSql;
        }
    }

    /**
     * Gets the named parameters.
     *
     * @return
     */
    public ImmutableList<String> getNamedParameters() {
        return namedParameters;
    }

    /**
     * Gets the named parameters.
     *
     * @param isForCouchbase
     * @return
     */
    public ImmutableList<String> getNamedParameters(boolean isForCouchbase) {
        if (isForCouchbase) {
            if (N.isNullOrEmpty(couchbaseParameterizedSql)) {
                parseForCouchbase();
            }

            return couchbaseNamedParameters;
        } else {
            return namedParameters;
        }
    }

    /**
     * Gets the parameter count.
     *
     * @return
     */
    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Gets the parameter count.
     *
     * @param isForCouchbase
     * @return
     */
    public int getParameterCount(boolean isForCouchbase) {
        if (isForCouchbase) {
            if (N.isNullOrEmpty(couchbaseParameterizedSql)) {
                parseForCouchbase();
            }

            return couchbaseParameterCount;
        } else {
            return parameterCount;
        }
    }

    /**
     * Parses the for couchbase.
     */
    private void parseForCouchbase() {
        List<String> couchbaseNamedParameterList = new ArrayList<>();

        final List<String> words = SQLParser.parse(sql);

        boolean isOpSqlPrefix = false;
        for (String word : words) {
            if (N.notNullOrEmpty(word)) {
                isOpSqlPrefix = opSqlPrefixSet.contains(word.toUpperCase());
                break;
            }
        }

        if (isOpSqlPrefix) {
            final StringBuilder sb = Objectory.createStringBuilder();
            int countOfParameter = 0;

            for (String word : words) {
                if (word.equals(WD.QUESTION_MARK)) {
                    if (couchbaseNamedParameterList.size() > 0) {
                        throw new IllegalArgumentException("can't mix '?' with name parameter ':propName' or '#{propName}' in the same sql script");
                    }

                    countOfParameter++;
                    word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                } else if (word.startsWith(LEFT_OF_IBATIS_NAMED_PARAMETER) && word.endsWith(RIGHT_OF_IBATIS_NAMED_PARAMETER)) {
                    couchbaseNamedParameterList.add(word.substring(2, word.length() - 1));

                    countOfParameter++;
                    word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                } else if (word.length() >= 2 && (word.charAt(0) == _PREFIX_OF_NAMED_PARAMETER || word.charAt(0) == _PREFIX_OF_COUCHBASE_NAMED_PARAMETER)
                        && isValidNamedParameterChar(word.charAt(1))) {
                    couchbaseNamedParameterList.add(word.substring(1));

                    countOfParameter++;
                    word = PREFIX_OF_COUCHBASE_NAMED_PARAMETER + countOfParameter;
                }

                sb.append(word);
            }

            boolean isNamedParametersByNum = true;

            for (int i = 0; i < countOfParameter; i++) {
                try {
                    if (Numbers.toInt(couchbaseNamedParameterList.get(i)) != i + 1) {
                        isNamedParametersByNum = false;
                        break;
                    }
                } catch (Exception e) {
                    // ignore;
                    isNamedParametersByNum = false;
                    break;
                }
            }

            if (isNamedParametersByNum) {
                couchbaseNamedParameterList.clear();
            }

            couchbaseParameterizedSql = sb.toString();
            couchbaseNamedParameters = ImmutableList.of(couchbaseNamedParameterList);
            couchbaseParameterCount = countOfParameter;

            Objectory.recycle(sb);
        } else {
            couchbaseParameterizedSql = sql;
            couchbaseNamedParameters = ImmutableList.empty();
            couchbaseParameterCount = 0;
        }
    }

    private static boolean isValidNamedParameterChar(final char ch) {
        // https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
        return !(ch < '0' || (ch > '9' && ch < 'A') || (ch > 'Z' && ch < 'a') || (ch > 'z' && ch < 128));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((sql == null) ? 0 : sql.hashCode());

        return result;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ParsedSql) {
            ParsedSql other = (ParsedSql) obj;

            return N.equals(sql, other.sql);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{sql=" + sql + ", parameterizedSql=" + parameterizedSql + "}";
    }
}
