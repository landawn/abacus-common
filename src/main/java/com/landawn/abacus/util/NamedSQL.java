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

// TODO: Auto-generated Javadoc
/**
 * The Class NamedSQL.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class NamedSQL {

    /** The Constant EVICT_TIME. */
    private static final int EVICT_TIME = 60 * 1000;

    /** The Constant LIVE_TIME. */
    private static final int LIVE_TIME = 24 * 60 * 60 * 1000;

    /** The Constant MAX_IDLE_TIME. */
    private static final int MAX_IDLE_TIME = 24 * 60 * 60 * 1000;

    /** The Constant namedSQLPrefixSet. */
    private static final Set<String> namedSQLPrefixSet = N.asSet(WD.INSERT, WD.SELECT, WD.UPDATE, WD.DELETE, WD.WITH);

    /** The Constant factor. */
    private static final int factor = Math.min(Math.max(1, IOUtil.MAX_MEMORY_IN_MB / 1024), 8);

    /** The Constant pool. */
    private static final KeyedObjectPool<String, PoolableWrapper<NamedSQL>> pool = PoolFactory.createKeyedObjectPool(1000 * factor, EVICT_TIME);

    /** The Constant PREFIX_OF_NAMED_PARAMETER. */
    private static final String PREFIX_OF_NAMED_PARAMETER = ":";

    private static final char _PREFIX_OF_NAMED_PARAMETER = PREFIX_OF_NAMED_PARAMETER.charAt(0);

    /** The Constant LEFT_OF_IBATIS_NAMED_PARAMETER. */
    private static final String LEFT_OF_IBATIS_NAMED_PARAMETER = "#{";

    /** The Constant RIGHT_OF_IBATIS_NAMED_PARAMETER. */
    private static final String RIGHT_OF_IBATIS_NAMED_PARAMETER = "}";

    /** The Constant PREFIX_OF_COUCHBASE_NAMED_PARAMETER. */
    private static final String PREFIX_OF_COUCHBASE_NAMED_PARAMETER = "$";

    /** The Constant PREFIX_OF_COUCHBASE_NAMED_PARAMETER. */
    private static final char _PREFIX_OF_COUCHBASE_NAMED_PARAMETER = PREFIX_OF_COUCHBASE_NAMED_PARAMETER.charAt(0);

    /** The named SQL. */
    private final String namedSQL;

    /** The pure SQL. */
    private final String pureSQL;

    /** The couchbase pure SQL. */
    private String couchbasePureSQL;

    /** The named parameters. */
    private final List<String> namedParameters;

    /** The couchbase named parameters. */
    private List<String> couchbaseNamedParameters;

    /** The parameter count. */
    private int parameterCount;

    /** The couchbase parameter count. */
    private int couchbaseParameterCount;

    /**
     * Instantiates a new named SQL.
     *
     * @param sql
     */
    @SuppressWarnings({ "unchecked" })
    private NamedSQL(String sql) {
        this.namedSQL = sql.trim();

        final List<String> words = SQLParser.parse(namedSQL);

        boolean isNamedSQLPrefix = false;
        for (String word : words) {
            if (N.notNullOrEmpty(word)) {
                isNamedSQLPrefix = namedSQLPrefixSet.contains(word.toUpperCase());
                break;
            }
        }

        final List<String> namedParameterList = new ArrayList<>();

        if (isNamedSQLPrefix) {
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

            pureSQL = sb.toString();
            namedParameters = ImmutableList.of(namedParameterList);

            Objectory.recycle(sb);
        } else {
            pureSQL = sql;
            namedParameters = ImmutableList.empty();
        }
    }

    /**
     *
     * @param sql
     * @return
     */
    public static NamedSQL parse(String sql) {
        NamedSQL result = null;
        PoolableWrapper<NamedSQL> w = pool.get(sql);

        if ((w == null) || (w.value() == null)) {
            synchronized (pool) {
                result = new NamedSQL(sql);
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
    public String getNamedSQL() {
        return namedSQL;
    }

    /**
     * Gets the parameterized SQL.
     *
     * @return
     */
    public String getParameterizedSQL() {
        return pureSQL;
    }

    /**
     * Gets the parameterized SQL.
     *
     * @param isForCouchbase
     * @return
     */
    public String getParameterizedSQL(boolean isForCouchbase) {
        if (isForCouchbase) {
            if (N.isNullOrEmpty(couchbasePureSQL)) {
                parseForCouchbase();
            }

            return couchbasePureSQL;
        } else {
            return pureSQL;
        }
    }

    /**
     * Gets the named parameters.
     *
     * @return
     */
    public List<String> getNamedParameters() {
        return namedParameters;
    }

    /**
     * Gets the named parameters.
     *
     * @param isForCouchbase
     * @return
     */
    public List<String> getNamedParameters(boolean isForCouchbase) {
        if (isForCouchbase) {
            if (N.isNullOrEmpty(couchbasePureSQL)) {
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
            if (N.isNullOrEmpty(couchbasePureSQL)) {
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

        final List<String> words = SQLParser.parse(namedSQL);

        boolean isNamedSQLPrefix = false;
        for (String word : words) {
            if (N.notNullOrEmpty(word)) {
                isNamedSQLPrefix = namedSQLPrefixSet.contains(word.toUpperCase());
                break;
            }
        }

        if (isNamedSQLPrefix) {
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
                    if (N.parseInt(couchbaseNamedParameterList.get(i)) != i + 1) {
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

            couchbasePureSQL = sb.toString();
            couchbaseNamedParameters = ImmutableList.of(couchbaseNamedParameterList);
            couchbaseParameterCount = countOfParameter;

            Objectory.recycle(sb);
        } else {
            couchbasePureSQL = namedSQL;
            couchbaseNamedParameters = ImmutableList.empty();
            couchbaseParameterCount = 0;
        }
    }

    private static boolean isValidNamedParameterChar(final char ch) {
        // https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
        return !(ch < '0' || (ch > '9' && ch < 'A') || (ch > 'Z' && ch < 'a') || (ch > 'z' && ch < 128));
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((namedSQL == null) ? 0 : namedSQL.hashCode());

        return result;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NamedSQL) {
            NamedSQL other = (NamedSQL) obj;

            return N.equals(namedSQL, other.namedSQL);
        }

        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "[NamedSQL] " + namedSQL + " [PureSQL] " + pureSQL;
    }
}
