/*
 * Copyright (C) 2018 HaiYang Li
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

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * Dynamic SQL builder. Must remember to call {@code build()} to generate target sql and release resources.
 * 
 * @author haiyangl
 *
 */
public class DynamicSQLBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DynamicSQLBuilder.class);

    /** The select. */
    private Select select = new Select(Objectory.createStringBuilder());

    /** The from. */
    private From from = new From(Objectory.createStringBuilder());

    /** The where. */
    private Where where;

    /** The group by. */
    private GroupBy groupBy;

    /** The having. */
    private Having having;

    /** The order by. */
    private OrderBy orderBy;

    /** The limit cond. */
    private String limitCond;

    /** The more parts. */
    private StringBuilder moreParts = null;

    /**
     * Instantiates a new dynamic SQL builder.
     */
    private DynamicSQLBuilder() {

    }

    /**
     *
     * @return
     */
    public static DynamicSQLBuilder create() {
        return new DynamicSQLBuilder();
    }

    /**
     *
     * @return
     */
    public Select select() {
        return select;
    }

    /**
     *
     * @return
     */
    public From from() {
        return from;
    }

    /**
     *
     * @return
     */
    public Where where() {
        if (where == null) {
            where = new Where(Objectory.createStringBuilder());
        }

        return where;
    }

    /**
     *
     * @return
     */
    public GroupBy groupBy() {
        if (groupBy == null) {
            groupBy = new GroupBy(Objectory.createStringBuilder());
        }

        return groupBy;
    }

    /**
     *
     * @return
     */
    public Having having() {
        if (having == null) {
            having = new Having(Objectory.createStringBuilder());
        }

        return having;
    }

    /**
     *
     * @return
     */
    public OrderBy orderBy() {
        if (orderBy == null) {
            orderBy = new OrderBy(Objectory.createStringBuilder());
        }

        return orderBy;
    }

    /**
     *
     * @param limitCond
     * @return
     */
    public DynamicSQLBuilder limit(String limitCond) {
        this.limitCond = limitCond;

        return this;
    }

    /**
     *
     * @param count
     * @return
     */
    public DynamicSQLBuilder limit(int count) {
        return limit("LIMIT " + count);
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public DynamicSQLBuilder limit(int offset, int count) {
        return limit("LIMIT " + offset + ", " + count);
    }

    /**
     * Limit by row num.
     *
     * @param count
     * @return
     */
    public DynamicSQLBuilder limitByRowNum(int count) {
        return limit("ROWNUM < " + count);
    }

    /**
     *
     * @param query
     * @return
     */
    public DynamicSQLBuilder union(final String query) {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        moreParts.append(" UNION ").append(query);

        return this;
    }

    /**
     *
     * @param query
     * @return
     */
    public DynamicSQLBuilder unionAll(final String query) {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        moreParts.append(" UNION ALL ").append(query);

        return this;
    }

    /**
     *
     * @param query
     * @return
     */
    public DynamicSQLBuilder intersect(final String query) {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        moreParts.append(" INTERSECT ").append(query);

        return this;
    }

    /**
     *
     * @param query
     * @return
     */
    public DynamicSQLBuilder except(final String query) {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        moreParts.append(" EXCEPT ").append(query);

        return this;
    }

    /**
     *
     * @param query
     * @return
     */
    public DynamicSQLBuilder minus(final String query) {
        if (moreParts == null) {
            moreParts = Objectory.createStringBuilder();
        }

        moreParts.append(" MINUS ").append(query);

        return this;
    }

    /**
     *
     * @return
     */
    public String build() {
        select.sb.append(" ").append(from.sb);

        if (where != null) {
            select.sb.append(" ").append(where.sb);
            Objectory.recycle(where.sb);
            where = null;
        }

        if (groupBy != null) {
            select.sb.append(" ").append(groupBy.sb);
            Objectory.recycle(groupBy.sb);
            groupBy = null;
        }

        if (having != null) {
            select.sb.append(" ").append(having.sb);
            Objectory.recycle(having.sb);
            having = null;
        }

        if (orderBy != null) {
            select.sb.append(" ").append(orderBy.sb);
            Objectory.recycle(orderBy.sb);
            orderBy = null;
        }

        if (N.notNullOrEmpty(limitCond)) {
            select.sb.append(" ").append(limitCond);
        }

        if (moreParts != null) {
            select.sb.append(moreParts);
            Objectory.recycle(moreParts);
        }

        final String sql = select.sb.toString();
        Objectory.recycle(from.sb);
        Objectory.recycle(select.sb);

        select = null;
        from = null;

        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }

        return sql;
    }

    /**
     * The Class Select.
     */
    public static class Select {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new select.
         *
         * @param sb
         */
        Select(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param column
         * @return
         */
        public Select append(String column) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(column);

            return this;
        }

        /**
         *
         * @param column
         * @param alias
         * @return
         */
        public Select append(String column, String alias) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("SELECT ");
            }

            sb.append(column).append(" AS ").append(alias);

            return this;
        }
    }

    /**
     * The Class From.
     */
    public static class From {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new from.
         *
         * @param sb
         */
        From(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param table
         * @return
         */
        public From append(String table) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("FROM ");
            }

            sb.append(table);

            return this;
        }

        /**
         *
         * @param table
         * @param alias
         * @return
         */
        public From append(String table, String alias) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("FROM");
            }

            sb.append(table).append(" ").append(alias);

            return this;
        }

        /**
         *
         * @param table
         * @param on
         * @return
         */
        public From join(String table, String on) {
            sb.append(" JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         *
         * @param table
         * @param on
         * @return
         */
        public From innerJoin(String table, String on) {
            sb.append(" INNER JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         *
         * @param table
         * @param on
         * @return
         */
        public From leftJoin(String table, String on) {
            sb.append(" LEFT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         *
         * @param table
         * @param on
         * @return
         */
        public From rightJoin(String table, String on) {
            sb.append(" RIGHT JOIN ").append(table).append(" ON ").append(on);

            return this;
        }

        /**
         *
         * @param table
         * @param on
         * @return
         */
        public From fullJoin(String table, String on) {
            sb.append(" FULL JOIN ").append(table).append(" ON ").append(on);

            return this;
        }
    }

    /**
     * The Class Where.
     */
    public static class Where {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new where.
         *
         * @param sb
         */
        Where(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Where append(String cond) {
            if (sb.length() > 0) {
                sb.append(" ");
            } else {
                sb.append("WHERE ");
            }

            sb.append(cond);

            return this;
        }

        /**
         * Append question mark {@code ?} {@code n} times.
         *
         * @param n
         * @return
         */
        public Where repeatQM(int n) {
            N.checkArgNotNegative(n, "n");

            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    sb.append(", ?");
                } else {
                    sb.append('?');
                }
            }

            return this;
        }

        /**
         * Append question mark {@code ?} {@code n} times.
         *
         * @param n
         * @param prefix
         * @param postfix
         * @return
         */
        public Where repeatQM(int n, String prefix, String postfix) {
            N.checkArgNotNegative(n, "n");

            sb.append(prefix);

            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    sb.append(", ?");
                } else {
                    sb.append('?');
                }
            }

            sb.append(postfix);

            return this;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Where and(String cond) {
            sb.append(" AND ").append(cond);

            return this;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Where or(String cond) {
            sb.append(" OR ").append(cond);

            return this;
        }
    }

    /**
     * The Class GroupBy.
     */
    public static class GroupBy {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new group by.
         *
         * @param sb
         */
        GroupBy(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param column
         * @return
         */
        public GroupBy append(String column) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("GROUP BY ");
            }

            sb.append(column);

            return this;
        }
    }

    /**
     * The Class Having.
     */
    public static class Having {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new having.
         *
         * @param sb
         */
        Having(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Having append(String cond) {
            if (sb.length() > 0) {
                sb.append(" ");
            } else {
                sb.append("HAVING ");
            }

            sb.append(cond);

            return this;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Having and(String cond) {
            sb.append(" AND ").append(cond);

            return this;
        }

        /**
         *
         * @param cond
         * @return
         */
        public Having or(String cond) {
            sb.append(" OR ").append(cond);

            return this;
        }
    }

    /**
     * The Class OrderBy.
     */
    public static class OrderBy {

        /** The sb. */
        final StringBuilder sb;

        /**
         * Instantiates a new order by.
         *
         * @param sb
         */
        OrderBy(final StringBuilder sb) {
            this.sb = sb;
        }

        /**
         *
         * @param column
         * @return
         */
        public OrderBy append(String column) {
            if (sb.length() > 0) {
                sb.append(", ");
            } else {
                sb.append("ORDER BY ");
            }

            sb.append(column);

            return this;
        }
    }

    /**
     * The Class DSB.
     */
    public static final class DSB extends DynamicSQLBuilder {

        /**
         * Instantiates a new dsb.
         */
        private DSB() {
            super();
        }
    }
}
