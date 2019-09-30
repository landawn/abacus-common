/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import static com.landawn.abacus.dataSource.DataSourceConfiguration.DRIVER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PASSWORD;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.URL;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.USER;
import static com.landawn.abacus.util.IOUtil.DEFAULT_QUEUE_SIZE_FOR_ROW_PARSER;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.DataSource;
import com.landawn.abacus.DataSourceManager;
import com.landawn.abacus.DataSourceSelector;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.EntityId;
import com.landawn.abacus.IsolationLevel;
import com.landawn.abacus.SliceSelector;
import com.landawn.abacus.Transaction;
import com.landawn.abacus.Transaction.Status;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.core.DirtyMarkerUtil;
import com.landawn.abacus.core.RowDataSet;
import com.landawn.abacus.dataSource.DataSourceConfiguration;
import com.landawn.abacus.dataSource.DataSourceManagerConfiguration;
import com.landawn.abacus.dataSource.NonSliceSelector;
import com.landawn.abacus.dataSource.SQLDataSource;
import com.landawn.abacus.dataSource.SQLDataSourceManager;
import com.landawn.abacus.dataSource.SimpleSourceSelector;
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.ExceptionalStream.ExceptionalIterator;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.SQLBuilder.NAC;
import com.landawn.abacus.util.SQLBuilder.NLC;
import com.landawn.abacus.util.SQLBuilder.NSC;
import com.landawn.abacus.util.SQLBuilder.PAC;
import com.landawn.abacus.util.SQLBuilder.PLC;
import com.landawn.abacus.util.SQLBuilder.PSC;
import com.landawn.abacus.util.SQLBuilder.SP;
import com.landawn.abacus.util.SQLExecutor.JdbcSettings;
import com.landawn.abacus.util.SQLExecutor.StatementSetter;
import com.landawn.abacus.util.SQLTransaction.CreatedBy;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try.BiFunction;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.IntStream.IntStreamEx;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.stream.Stream.StreamEx;

// TODO: Auto-generated Javadoc
/**
 * The Class JdbcUtil.
 *
 * @author Haiyang Li
 * @see {@link com.landawn.abacus.annotation.ReadOnly}
 * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
 * @see {@link com.landawn.abacus.annotation.NonUpdatable}
 * @see {@link com.landawn.abacus.annotation.Transient}
 * @see {@link com.landawn.abacus.annotation.Table}
 * @see {@link com.landawn.abacus.annotation.Column}
 * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html</a>
 * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html</a>
 * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html</a>
 * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html">http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html</a>
 * @since 0.8
 */
public final class JdbcUtil {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JdbcUtil.class);

    /** The async executor. */
    private static final AsyncExecutor asyncExecutor = new AsyncExecutor(Math.min(8, IOUtil.CPU_CORES), 64, 180L, TimeUnit.SECONDS);

    /** The Constant CURRENT_DIR_PATH. */
    // ...
    private static final String CURRENT_DIR_PATH = "./";

    /** The Constant DEFAULT_STMT_SETTER. */
    private static final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> DEFAULT_STMT_SETTER = new JdbcUtil.BiParametersSetter<PreparedStatement, Object[]>() {
        @Override
        public void accept(PreparedStatement stmt, Object[] parameters) throws SQLException {
            for (int i = 0, len = parameters.length; i < len; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
        }
    };

    /** The Constant sqlStateForTableNotExists. */
    private static final Set<String> sqlStateForTableNotExists = N.newHashSet();

    static {
        sqlStateForTableNotExists.add("42S02"); // for MySQCF.
        sqlStateForTableNotExists.add("42P01"); // for PostgreSQCF.
        sqlStateForTableNotExists.add("42501"); // for HSQLDB.
    }

    /**
     * Instantiates a new jdbc util.
     */
    private JdbcUtil() {
        // singleton
    }

    /**
     * Gets the DB version.
     *
     * @param conn
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static DBVersion getDBVersion(final Connection conn) throws UncheckedSQLException {
        try {
            String dbProudctName = conn.getMetaData().getDatabaseProductName();
            String dbProudctVersion = conn.getMetaData().getDatabaseProductVersion();

            DBVersion dbVersion = DBVersion.OTHERS;

            String upperCaseProductName = dbProudctName.toUpperCase();
            if (upperCaseProductName.contains("H2")) {
                dbVersion = DBVersion.H2;
            } else if (upperCaseProductName.contains("HSQL")) {
                dbVersion = DBVersion.HSQLDB;
            } else if (upperCaseProductName.contains("MYSQL")) {
                if (dbProudctVersion.startsWith("5.5")) {
                    dbVersion = DBVersion.MYSQL_5_5;
                } else if (dbProudctVersion.startsWith("5.6")) {
                    dbVersion = DBVersion.MYSQL_5_6;
                } else if (dbProudctVersion.startsWith("5.7")) {
                    dbVersion = DBVersion.MYSQL_5_7;
                } else if (dbProudctVersion.startsWith("5.8")) {
                    dbVersion = DBVersion.MYSQL_5_8;
                } else if (dbProudctVersion.startsWith("5.9")) {
                    dbVersion = DBVersion.MYSQL_5_9;
                } else if (dbProudctVersion.startsWith("6")) {
                    dbVersion = DBVersion.MYSQL_6;
                } else if (dbProudctVersion.startsWith("7")) {
                    dbVersion = DBVersion.MYSQL_7;
                } else if (dbProudctVersion.startsWith("8")) {
                    dbVersion = DBVersion.MYSQL_8;
                } else if (dbProudctVersion.startsWith("9")) {
                    dbVersion = DBVersion.MYSQL_9;
                } else if (dbProudctVersion.startsWith("10")) {
                    dbVersion = DBVersion.MYSQL_10;
                } else {
                    dbVersion = DBVersion.MYSQL_OTHERS;
                }
            } else if (upperCaseProductName.contains("POSTGRESQL")) {
                if (dbProudctVersion.startsWith("9.2")) {
                    dbVersion = DBVersion.POSTGRESQL_9_2;
                } else if (dbProudctVersion.startsWith("9.3")) {
                    dbVersion = DBVersion.POSTGRESQL_9_3;
                } else if (dbProudctVersion.startsWith("9.4")) {
                    dbVersion = DBVersion.POSTGRESQL_9_4;
                } else if (dbProudctVersion.startsWith("9.5")) {
                    dbVersion = DBVersion.POSTGRESQL_9_5;
                } else if (dbProudctVersion.startsWith("10")) {
                    dbVersion = DBVersion.POSTGRESQL_10;
                } else if (dbProudctVersion.startsWith("11")) {
                    dbVersion = DBVersion.POSTGRESQL_11;
                } else if (dbProudctVersion.startsWith("12")) {
                    dbVersion = DBVersion.POSTGRESQL_12;
                } else {
                    dbVersion = DBVersion.POSTGRESQL_OTHERS;
                }
            } else if (upperCaseProductName.contains("ORACLE")) {
                dbVersion = DBVersion.ORACLE;
            } else if (upperCaseProductName.contains("DB2")) {
                dbVersion = DBVersion.DB2;
            } else if (upperCaseProductName.contains("SQL SERVER")) {
                dbVersion = DBVersion.SQL_SERVER;
            }

            return dbVersion;
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Creates the data source manager.
     *
     * @param dataSourceXmlFile
     * @return DataSourceManager
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see DataSource.xsd
     */
    public static DataSourceManager createDataSourceManager(final String dataSourceXmlFile) throws UncheckedIOException, UncheckedSQLException {
        InputStream is = null;
        try {
            is = new FileInputStream(Configuration.findFile(dataSourceXmlFile));
            return createDataSourceManager(is, dataSourceXmlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }

    }

    /**
     * Creates the data source manager.
     *
     * @param dataSourceXmlInputStream
     * @return DataSourceManager
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see DataSource.xsd
     */
    public static DataSourceManager createDataSourceManager(final InputStream dataSourceXmlInputStream) throws UncheckedIOException, UncheckedSQLException {
        return createDataSourceManager(dataSourceXmlInputStream, CURRENT_DIR_PATH);
    }

    /** The Constant PROPERTIES. */
    private static final String PROPERTIES = "properties";

    /** The Constant RESOURCE. */
    private static final String RESOURCE = "resource";

    /**
     * Creates the data source manager.
     *
     * @param dataSourceXmlInputStream
     * @param dataSourceXmlFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    private static DataSourceManager createDataSourceManager(final InputStream dataSourceXmlInputStream, final String dataSourceXmlFile)
            throws UncheckedIOException, UncheckedSQLException {
        DocumentBuilder domParser = XMLUtil.createDOMParser();
        Document doc = null;

        try {
            doc = domParser.parse(dataSourceXmlInputStream);

            Element rootElement = doc.getDocumentElement();

            final Map<String, String> props = new HashMap<>();
            List<Element> propertiesElementList = XMLUtil.getElementsByTagName(rootElement, PROPERTIES);

            if (N.notNullOrEmpty(propertiesElementList)) {
                for (Element propertiesElement : propertiesElementList) {
                    File resourcePropertiesFile = Configuration.findFileByFile(new File(dataSourceXmlFile), propertiesElement.getAttribute(RESOURCE));
                    java.util.Properties properties = new java.util.Properties();
                    InputStream is = null;

                    try {
                        is = new FileInputStream(resourcePropertiesFile);

                        if (resourcePropertiesFile.getName().endsWith(".xml")) {
                            properties.loadFromXML(is);
                        } else {
                            properties.load(is);
                        }
                    } finally {
                        IOUtil.close(is);
                    }

                    for (Object key : properties.keySet()) {
                        props.put((String) key, (String) properties.get(key));
                    }
                }
            }

            String nodeName = rootElement.getNodeName();
            if (nodeName.equals(DataSourceManagerConfiguration.DATA_SOURCE_MANAGER)) {
                DataSourceManagerConfiguration config = new DataSourceManagerConfiguration(rootElement, props);
                return new SQLDataSourceManager(config);
            } else if (nodeName.equals(DataSourceConfiguration.DATA_SOURCE)) {
                DataSourceConfiguration config = new DataSourceConfiguration(rootElement, props);
                return new SimpleDataSourceManager(new SQLDataSource(config));
            } else {
                throw new AbacusException("Unknown xml format with root element: " + nodeName);
            }
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates the data source.
     *
     * @param dataSourceFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see DataSource.xsd
     */
    public static DataSource createDataSource(final String dataSourceFile) throws UncheckedIOException, UncheckedSQLException {
        InputStream is = null;
        try {
            is = new FileInputStream(Configuration.findFile(dataSourceFile));
            return createDataSource(is, dataSourceFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Creates the data source.
     *
     * @param dataSourceInputStream
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see DataSource.xsd
     */
    public static DataSource createDataSource(final InputStream dataSourceInputStream) throws UncheckedIOException, UncheckedSQLException {
        return createDataSource(dataSourceInputStream, CURRENT_DIR_PATH);
    }

    /**
     * Creates the data source.
     *
     * @param dataSourceInputStream
     * @param dataSourceFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    private static DataSource createDataSource(final InputStream dataSourceInputStream, final String dataSourceFile)
            throws UncheckedIOException, UncheckedSQLException {
        final String dataSourceString = IOUtil.readString(dataSourceInputStream);

        if (CURRENT_DIR_PATH.equals(dataSourceFile) || dataSourceFile.endsWith(".xml")) {
            try {
                return createDataSourceManager(new ByteArrayInputStream(dataSourceString.getBytes())).getPrimaryDataSource();
            } catch (ParseException e) {
                // ignore.
            } catch (UncheckedIOException e) {
                // ignore.
            }
        }

        final Map<String, String> newProps = new HashMap<>();
        final java.util.Properties properties = new java.util.Properties();

        try {
            properties.load(new ByteArrayInputStream(dataSourceString.getBytes()));

            Object value = null;

            for (Object key : properties.keySet()) {
                value = properties.get(key);
                newProps.put(key.toString().trim(), value.toString().trim());
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new SQLDataSource(newProps);
    }

    /**
     * Creates the data source.
     *
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static DataSource createDataSource(final String url, final String user, final String password) throws UncheckedSQLException {
        return createDataSource(getDriverClasssByUrl(url), url, user, password);
    }

    /**
     * Creates the data source.
     *
     * @param driver
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static DataSource createDataSource(final String driver, final String url, final String user, final String password) throws UncheckedSQLException {
        final Class<? extends Driver> driverClass = ClassUtil.forClass(driver);

        return createDataSource(driverClass, url, user, password);
    }

    /**
     * Creates the data source.
     *
     * @param driverClass
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static DataSource createDataSource(final Class<? extends Driver> driverClass, final String url, final String user, final String password)
            throws UncheckedSQLException {
        final Map<String, Object> props = new HashMap<>();

        props.put(DRIVER, driverClass.getCanonicalName());
        props.put(URL, url);
        props.put(USER, user);
        props.put(PASSWORD, password);

        return createDataSource(props);
    }

    /**
     * Creates the data source.
     *
     * @param props refer to Connection.xsd for the supported properties.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static DataSource createDataSource(final Map<String, ?> props) throws UncheckedSQLException {
        final String driver = (String) props.get(DRIVER);

        if (N.isNullOrEmpty(driver)) {
            final String url = (String) props.get(URL);

            if (N.isNullOrEmpty(url)) {
                throw new IllegalArgumentException("Url is not specified");
            }

            final Map<String, Object> tmp = new HashMap<>(props);

            tmp.put(DRIVER, getDriverClasssByUrl(url).getCanonicalName());

            return new SQLDataSource(tmp);
        } else {
            return new SQLDataSource(props);
        }
    }

    /**
     *
     * @param sqlDataSource
     * @return
     * @deprecated
     */
    @Deprecated
    public static DataSource wrap(final javax.sql.DataSource sqlDataSource) {
        return sqlDataSource instanceof DataSource ? ((DataSource) sqlDataSource) : new SimpleDataSource(sqlDataSource);
    }

    /**
     * Creates the connection.
     *
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static Connection createConnection(final String url, final String user, final String password) throws UncheckedSQLException {
        return createConnection(getDriverClasssByUrl(url), url, user, password);
    }

    /**
     * Gets the driver classs by url.
     *
     * @param url
     * @return
     */
    private static Class<? extends Driver> getDriverClasssByUrl(final String url) {
        Class<? extends Driver> driverClass = null;
        // jdbc:mysql://localhost:3306/abacustest
        if (url.indexOf("mysql") > 0 || StringUtil.indexOfIgnoreCase(url, "mysql") > 0) {
            driverClass = ClassUtil.forClass("com.mysql.jdbc.Driver");
            // jdbc:postgresql://localhost:5432/abacustest
        } else if (url.indexOf("postgresql") > 0 || StringUtil.indexOfIgnoreCase(url, "postgresql") > 0) {
            driverClass = ClassUtil.forClass("org.postgresql.Driver");
            // jdbc:h2:hsql://<host>:<port>/<database>
        } else if (url.indexOf("h2") > 0 || StringUtil.indexOfIgnoreCase(url, "h2") > 0) {
            driverClass = ClassUtil.forClass("org.h2.Driver");
            // jdbc:hsqldb:hsql://localhost/abacustest
        } else if (url.indexOf("hsqldb") > 0 || StringUtil.indexOfIgnoreCase(url, "hsqldb") > 0) {
            driverClass = ClassUtil.forClass("org.hsqldb.jdbc.JDBCDriver");
            // jdbc.url=jdbc:oracle:thin:@localhost:1521:abacustest
        } else if (url.indexOf("oracle") > 0 || StringUtil.indexOfIgnoreCase(url, "oracle") > 0) {
            driverClass = ClassUtil.forClass("oracle.jdbc.driver.OracleDriver");
            // jdbc.url=jdbc:sqlserver://localhost:1433;Database=abacustest
        } else if (url.indexOf("sqlserver") > 0 || StringUtil.indexOfIgnoreCase(url, "sqlserver") > 0) {
            driverClass = ClassUtil.forClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // jdbc:db2://localhost:50000/abacustest
        } else if (url.indexOf("db2") > 0 || StringUtil.indexOfIgnoreCase(url, "db2") > 0) {
            driverClass = ClassUtil.forClass("com.ibm.db2.jcc.DB2Driver");
        } else {
            throw new IllegalArgumentException(
                    "Can not identity the driver class by url: " + url + ". Only mysql, postgresql, hsqldb, sqlserver, oracle and db2 are supported currently");
        }
        return driverClass;
    }

    /**
     * Creates the connection.
     *
     * @param driverClass
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static Connection createConnection(final String driverClass, final String url, final String user, final String password)
            throws UncheckedSQLException {
        Class<? extends Driver> cls = ClassUtil.forClass(driverClass);
        return createConnection(cls, url, user, password);
    }

    /**
     * Creates the connection.
     *
     * @param driverClass
     * @param url
     * @param user
     * @param password
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static Connection createConnection(final Class<? extends Driver> driverClass, final String url, final String user, final String password)
            throws UncheckedSQLException {
        try {
            DriverManager.registerDriver(N.newInstance(driverClass));

            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new UncheckedSQLException("Failed to close create connection", e);
        }
    }

    /** The is in spring. */
    private static boolean isInSpring = true;

    static {
        try {
            isInSpring = ClassUtil.forClass("org.springframework.jdbc.datasource.DataSourceUtils") != null;
        } catch (Throwable e) {
            isInSpring = false;
        }
    }

    /**
     * Spring Transaction is supported and Integrated.
     * If this method is called where a Spring transaction is started with the specified {@code DataSource},
     * the {@code Connection} started the Spring Transaction will be returned. Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be returned.
     *
     * @param ds
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static Connection getConnection(final javax.sql.DataSource ds) throws UncheckedSQLException {
        if (isInSpring) {
            try {
                return org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds);
            } catch (NoClassDefFoundError e) {
                isInSpring = false;

                try {
                    return ds.getConnection();
                } catch (SQLException e1) {
                    throw new UncheckedSQLException(e1);
                }
            }
        } else {
            try {
                return ds.getConnection();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     * Spring Transaction is supported and Integrated.
     * If this method is called where a Spring transaction is started with the specified {@code DataSource},
     * the specified {@code Connection} won't be returned to {@code DataSource}(Connection pool) until the transaction is committed or rolled back. Otherwise the specified {@code Connection} will be directly returned back to {@code DataSource}(Connection pool).
     *
     * @param conn
     * @param ds
     */
    public static void releaseConnection(final Connection conn, final javax.sql.DataSource ds) {
        if (conn == null) {
            return;
        }

        if (isInSpring && ds != null) {
            try {
                org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(conn, ds);
            } catch (NoClassDefFoundError e) {
                isInSpring = false;
                JdbcUtil.closeQuietly(conn);
            }
        } else {
            JdbcUtil.closeQuietly(conn);
        }
    }

    /**
     * Creates the close handler.
     *
     * @param conn
     * @param ds
     * @return
     */
    static Runnable createCloseHandler(final Connection conn, final javax.sql.DataSource ds) {
        return new Runnable() {
            @Override
            public void run() {
                releaseConnection(conn, ds);
            }
        };
    }

    /**
     *
     * @param rs
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final ResultSet rs) throws UncheckedSQLException {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     *
     * @param rs
     * @param closeStatement
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final ResultSet rs, final boolean closeStatement) throws UncheckedSQLException {
        close(rs, closeStatement, false);
    }

    /**
     *
     * @param rs
     * @param closeStatement
     * @param closeConnection
     * @throws IllegalArgumentException if {@code closeStatement = false} while {@code closeConnection = true}.
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final ResultSet rs, final boolean closeStatement, final boolean closeConnection)
            throws IllegalArgumentException, UncheckedSQLException {
        if (closeConnection && closeStatement == false) {
            throw new IllegalArgumentException("'closeStatement' can't be false while 'closeConnection' is true");
        }

        if (rs == null) {
            return;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            if (closeStatement || closeConnection) {
                stmt = rs.getStatement();
            }

            if (closeConnection && stmt != null) {
                conn = stmt.getConnection();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            close(rs, stmt, conn);
        }
    }

    /**
     *
     * @param stmt
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final Statement stmt) throws UncheckedSQLException {
        if (stmt != null) {
            try {
                if (stmt instanceof PreparedStatement) {
                    try {
                        ((PreparedStatement) stmt).clearParameters();
                    } catch (SQLException e) {
                        logger.error("Failed to clear parameters", e);
                    }
                }

                stmt.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     *
     * @param conn
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final Connection conn) throws UncheckedSQLException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     *
     * @param rs
     * @param stmt
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final ResultSet rs, final Statement stmt) throws UncheckedSQLException {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (stmt != null) {
                    if (stmt instanceof PreparedStatement) {
                        try {
                            ((PreparedStatement) stmt).clearParameters();
                        } catch (SQLException e) {
                            logger.error("Failed to clear parameters", e);
                        }
                    }
                    stmt.close();
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     *
     * @param stmt
     * @param conn
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final Statement stmt, final Connection conn) throws UncheckedSQLException {
        try {
            if (stmt != null) {
                if (stmt instanceof PreparedStatement) {
                    try {
                        ((PreparedStatement) stmt).clearParameters();
                    } catch (SQLException e) {
                        logger.error("Failed to clear parameters", e);
                    }
                }
                stmt.close();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     *
     * @param rs
     * @param stmt
     * @param conn
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void close(final ResultSet rs, final Statement stmt, final Connection conn) throws UncheckedSQLException {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (stmt != null) {
                    if (stmt instanceof PreparedStatement) {
                        try {
                            ((PreparedStatement) stmt).clearParameters();
                        } catch (SQLException e) {
                            logger.error("Failed to clear parameters", e);
                        }
                    }
                    stmt.close();
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            }
        }
    }

    /**
     * Unconditionally close an <code>ResultSet</code>.
     * <p>
     * Equivalent to {@link ResultSet#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param rs
     */
    public static void closeQuietly(final ResultSet rs) {
        closeQuietly(rs, null, null);
    }

    /**
     *
     * @param rs
     * @param closeStatement
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement) throws UncheckedSQLException {
        closeQuietly(rs, closeStatement, false);
    }

    /**
     *
     * @param rs
     * @param closeStatement
     * @param closeConnection
     * @throws IllegalArgumentException if {@code closeStatement = false} while {@code closeConnection = true}.
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement, final boolean closeConnection) throws IllegalArgumentException {
        if (closeConnection && closeStatement == false) {
            throw new IllegalArgumentException("'closeStatement' can't be false while 'closeConnection' is true");
        }

        if (rs == null) {
            return;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            if (closeStatement || closeConnection) {
                stmt = rs.getStatement();
            }

            if (closeConnection && stmt != null) {
                conn = stmt.getConnection();
            }
        } catch (SQLException e) {
            logger.error("Failed to get Statement or Connection by ResultSet", e);
        } finally {
            closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Unconditionally close an <code>Statement</code>.
     * <p>
     * Equivalent to {@link Statement#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param stmt
     */
    public static void closeQuietly(final Statement stmt) {
        closeQuietly(null, stmt, null);
    }

    /**
     * Unconditionally close an <code>Connection</code>.
     * <p>
     * Equivalent to {@link Connection#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param conn
     */
    public static void closeQuietly(final Connection conn) {
        closeQuietly(null, null, conn);
    }

    /**
     * Unconditionally close the <code>ResultSet, Statement</code>.
     * <p>
     * Equivalent to {@link ResultSet#close()}, {@link Statement#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param rs
     * @param stmt
     */
    public static void closeQuietly(final ResultSet rs, final Statement stmt) {
        closeQuietly(rs, stmt, null);
    }

    /**
     * Unconditionally close the <code>Statement, Connection</code>.
     * <p>
     * Equivalent to {@link Statement#close()}, {@link Connection#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param stmt
     * @param conn
     */
    public static void closeQuietly(final Statement stmt, final Connection conn) {
        closeQuietly(null, stmt, conn);
    }

    /**
     * Unconditionally close the <code>ResultSet, Statement, Connection</code>.
     * <p>
     * Equivalent to {@link ResultSet#close()}, {@link Statement#close()}, {@link Connection#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     *
     * @param rs
     * @param stmt
     * @param conn
     */
    public static void closeQuietly(final ResultSet rs, final Statement stmt, final Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                logger.error("Failed to close ResultSet", e);
            }
        }

        if (stmt != null) {
            if (stmt instanceof PreparedStatement) {
                try {
                    ((PreparedStatement) stmt).clearParameters();
                } catch (Exception e) {
                    logger.error("Failed to clear parameters", e);
                }
            }

            try {
                stmt.close();
            } catch (Exception e) {
                logger.error("Failed to close Statement", e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                logger.error("Failed to close Connection", e);
            }
        }
    }

    /**
     *
     * @param rs
     * @param n the count of row to move ahead.
     * @return
     * @throws SQLException the SQL exception
     */
    public static int skip(final ResultSet rs, int n) throws SQLException {
        return skip(rs, (long) n);
    }

    /**
     *
     * @param rs
     * @param n the count of row to move ahead.
     * @return
     * @throws SQLException the SQL exception
     * @see {@link ResultSet#absolute(int)}
     */
    public static int skip(final ResultSet rs, long n) throws SQLException {
        if (n <= 0) {
            return 0;
        } else if (n == 1) {
            return rs.next() == true ? 1 : 0;
        } else {
            final int currentRow = rs.getRow();

            if (n <= Integer.MAX_VALUE) {
                try {
                    if (n > Integer.MAX_VALUE - rs.getRow()) {
                        while (n-- > 0L && rs.next()) {
                        }
                    } else {
                        rs.absolute((int) n + rs.getRow());
                    }
                } catch (SQLException e) {
                    while (n-- > 0L && rs.next()) {
                    }
                }
            } else {
                while (n-- > 0L && rs.next()) {
                }
            }

            return rs.getRow() - currentRow;
        }
    }

    /**
     * Gets the column count.
     *
     * @param rs
     * @return
     * @throws SQLException the SQL exception
     */
    public static int getColumnCount(ResultSet rs) throws SQLException {
        return rs.getMetaData().getColumnCount();
    }

    /**
     * Gets the column name list.
     *
     * @param conn
     * @param tableName
     * @return
     * @throws SQLException the SQL exception
     */
    public static List<String> getColumnNameList(final Connection conn, final String tableName) throws SQLException {
        final String query = "SELECT * FROM " + tableName + " WHERE 1 > 2";
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = prepareStatement(conn, query);
            rs = stmt.executeQuery();

            final ResultSetMetaData metaData = rs.getMetaData();
            final int columnCount = metaData.getColumnCount();
            final List<String> columnNameList = new ArrayList<>(columnCount);

            for (int i = 1, n = columnCount + 1; i < n; i++) {
                columnNameList.add(metaData.getColumnName(i));
            }

            return columnNameList;
        } finally {
            closeQuietly(rs, stmt);
        }
    }

    /**
     * Gets the column label list.
     *
     * @param rs
     * @return
     * @throws SQLException the SQL exception
     */
    public static List<String> getColumnLabelList(ResultSet rs) throws SQLException {
        final ResultSetMetaData metaData = rs.getMetaData();
        final int columnCount = metaData.getColumnCount();
        final List<String> labelList = new ArrayList<>(columnCount);

        for (int i = 1, n = columnCount + 1; i < n; i++) {
            labelList.add(getColumnLabel(metaData, i));
        }

        return labelList;
    }

    /**
     * Gets the column label.
     *
     * @param rsmd
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    public static String getColumnLabel(final ResultSetMetaData rsmd, final int columnIndex) throws SQLException {
        final String result = rsmd.getColumnLabel(columnIndex);

        return N.isNullOrEmpty(result) ? rsmd.getColumnName(columnIndex) : result;
    }

    /**
     * Gets the column value.
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    public static Object getColumnValue(final ResultSet rs, final int columnIndex) throws SQLException {
        // Copied from JdbcUtils#getResultSetValue(ResultSet, int) in SpringJdbc under Apache License, Version 2.0.
        //    final Object obj = rs.getObject(columnIndex);
        //
        //    if (obj == null) {
        //        return obj;
        //    }
        //
        //    final String className = obj.getClass().getName();
        //
        //    if (obj instanceof Blob) {
        //        final Blob blob = (Blob) obj;
        //        return blob.getBytes(1, (int) blob.length());
        //    } else if (obj instanceof Clob) {
        //        final Clob clob = (Clob) obj;
        //        return clob.getSubString(1, (int) clob.length());
        //    } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
        //        return rs.getTimestamp(columnIndex);
        //    } else if (className.startsWith("oracle.sql.DATE")) {
        //        final String columnClassName = rs.getMetaData().getColumnClassName(columnIndex);
        //
        //        if ("java.sql.Timestamp".equals(columnClassName) || "oracle.sql.TIMESTAMP".equals(columnClassName)) {
        //            return rs.getTimestamp(columnIndex);
        //        } else {
        //            return rs.getDate(columnIndex);
        //        }
        //    } else if (obj instanceof java.sql.Date) {
        //        if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(columnIndex))) {
        //            return rs.getTimestamp(columnIndex);
        //        }
        //    }
        //
        //    return obj;

        return rs.getObject(columnIndex);
    }

    /**
     * Gets the column value.
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    public static Object getColumnValue(final ResultSet rs, final String columnLabel) throws SQLException {
        // Copied from JdbcUtils#getResultSetValue(ResultSet, int) in SpringJdbc under Apache License, Version 2.0.
        //    final Object obj = rs.getObject(columnLabel);
        //
        //    if (obj == null) {
        //        return obj;
        //    }
        //
        //    final String className = obj.getClass().getName();
        //
        //    if (obj instanceof Blob) {
        //        final Blob blob = (Blob) obj;
        //        return blob.getBytes(1, (int) blob.length());
        //    } else if (obj instanceof Clob) {
        //        final Clob clob = (Clob) obj;
        //        return clob.getSubString(1, (int) clob.length());
        //    } else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
        //        return rs.getTimestamp(columnLabel);
        //    } else if (className.startsWith("oracle.sql.DATE")) {
        //        final int columnIndex = JdbcUtil.getColumnLabelList(rs).indexOf(columnLabel);
        //
        //        if (columnIndex >= 0) {
        //            final String columnClassName = rs.getMetaData().getColumnClassName(columnIndex + 1);
        //
        //            if ("java.sql.Timestamp".equals(columnClassName) || "oracle.sql.TIMESTAMP".equals(columnClassName)) {
        //                return rs.getTimestamp(columnLabel);
        //            } else {
        //                return rs.getDate(columnLabel);
        //            }
        //        }
        //    } else if (obj instanceof java.sql.Date) {
        //        final int columnIndex = JdbcUtil.getColumnLabelList(rs).indexOf(columnLabel);
        //
        //        if (columnIndex >= 0) {
        //            if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(columnIndex + 1))) {
        //                return rs.getTimestamp(columnLabel);
        //            }
        //        }
        //    }
        //
        //    return obj;

        return rs.getObject(columnLabel);
    }

    /**
     * Gets the column value.
     *
     * @param <T>
     * @param targetClass
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    public static <T> T getColumnValue(final Class<T> targetClass, final ResultSet rs, final int columnIndex) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnIndex);
    }

    /**
     * Gets the column value.
     *
     * @param <T>
     * @param targetClass
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    public static <T> T getColumnValue(final Class<T> targetClass, final ResultSet rs, final String columnLabel) throws SQLException {
        return N.<T> typeOf(targetClass).get(rs, columnLabel);
    }

    /**
     * Refer to: {@code beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)}.
     *
     * @param dataSource
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see {@link #beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)}
     */
    public static SQLTransaction beginTransaction(final javax.sql.DataSource dataSource) throws UncheckedSQLException {
        return beginTransaction(dataSource, IsolationLevel.DEFAULT);
    }

    /**
     * Refer to: {@code beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)}.
     *
     * @param dataSource
     * @param isolationLevel
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see {@link #beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)}
     */
    public static SQLTransaction beginTransaction(final javax.sql.DataSource dataSource, final IsolationLevel isolationLevel) throws UncheckedSQLException {
        return beginTransaction(dataSource, isolationLevel, false);
    }

    /**
     * Starts a global transaction which will be shared by all in-line database query with the same {@code DataSource} in the same thread,
     * including methods: {@code JdbcUtil.beginTransaction/prepareQuery/prepareNamedQuery/prepareCallableQuery, SQLExecutor(Mapper).beginTransaction/get/insert/batchInsert/update/batchUpdate/query/list/findFirst/...}
     *
     * <br />
     * Spring Transaction is supported and Integrated.
     * If this method is called at where a Spring transaction is started with the specified {@code DataSource},
     * the {@code Connection} started the Spring Transaction will be used here.
     * That's to say the Spring transaction will have the final control on commit/roll back over the {@code Connection}.
     *
     * <br />
     * <br />
     *
     * Here is the general code pattern to work with {@code SQLTransaction}.
     *
     * <pre>
     * <code>
     * public void doSomethingA() {
     *     ...
     *     final SQLTransaction tranA = JdbcUtil.beginTransaction(dataSource1, isolation);
     *
     *     try {
     *         ...
     *         doSomethingB(); // Share the same transaction 'tranA' because they're in the same thread and start transaction with same DataSource 'dataSource1'.
     *         ...
     *         doSomethingC(); // won't share the same transaction 'tranA' although they're in the same thread but start transaction with different DataSource 'dataSource2'.
     *         ...
     *         tranA.commit();
     *     } finally {
     *         tranA.rollbackIfNotCommitted();
     *     }
     * }
     *
     * public void doSomethingB() {
     *     ...
     *     final SQLTransaction tranB = JdbcUtil.beginTransaction(dataSource1, isolation);
     *     try {
     *         // do your work with the conn...
     *         ...
     *         tranB.commit();
     *     } finally {
     *         tranB.rollbackIfNotCommitted();
     *     }
     * }
     *
     * public void doSomethingC() {
     *     ...
     *     final SQLTransaction tranC = JdbcUtil.beginTransaction(dataSource2, isolation);
     *     try {
     *         // do your work with the conn...
     *         ...
     *         tranC.commit();
     *     } finally {
     *         tranC.rollbackIfNotCommitted();
     *     }
     * }
     * </pre>
     * </code>
     *
     * It's incorrect to use flag to identity the transaction should be committed or rolled back.
     * Don't write below code:
     * <pre>
     * <code>
     * public void doSomethingA() {
     *     ...
     *     final SQLTransaction tranA = JdbcUtil.beginTransaction(dataSource1, isolation);
     *     boolean flagToCommit = false;
     *     try {
     *         // do your work with the conn...
     *         ...
     *         flagToCommit = true;
     *     } finally {
     *         if (flagToCommit) {
     *             tranA.commit();
     *         } else {
     *             tranA.rollbackIfNotCommitted();
     *         }
     *     }
     * }
     * </code>
     * </pre>
     *
     * @param dataSource
     * @param isolationLevel
     * @param isForUpdateOnly
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see {@link #getConnection(javax.sql.DataSource)}
     * @see {@link #releaseConnection(Connection, javax.sql.DataSource)}
     * @see SQLExecutor#beginTransaction(IsolationLevel, boolean, JdbcSettings)
     */
    public static SQLTransaction beginTransaction(final javax.sql.DataSource dataSource, final IsolationLevel isolationLevel, final boolean isForUpdateOnly)
            throws UncheckedSQLException {
        N.checkArgNotNull(dataSource, "dataSource");
        N.checkArgNotNull(isolationLevel, "isolationLevel");

        SQLTransaction tran = SQLTransaction.getTransaction(dataSource, CreatedBy.JDBC_UTIL);

        if (tran == null) {
            Connection conn = null;
            boolean noException = false;

            try {
                conn = getConnection(dataSource);
                tran = new SQLTransaction(dataSource, conn, isolationLevel, CreatedBy.JDBC_UTIL, true);
                tran.incrementAndGetRef(isolationLevel, isForUpdateOnly);

                noException = true;
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                if (noException == false) {
                    releaseConnection(conn, dataSource);
                }
            }

            logger.info("Create a new SQLTransaction(id={})", tran.id());
            SQLTransaction.putTransaction(tran);
        } else {
            logger.info("Reusing the existing SQLTransaction(id={})", tran.id());
            tran.incrementAndGetRef(isolationLevel, isForUpdateOnly);
        }

        return tran;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param ds
     * @param callable
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> T doInTransaction(final javax.sql.DataSource ds, final Try.Callable<T, E> callable) throws E {
        final SQLTransaction tran = JdbcUtil.beginTransaction(ds);
        T result = null;

        try {
            result = callable.call();
            tran.commit();
        } finally {
            tran.rollbackIfNotCommitted();
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param ds
     * @param func
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> T doInTransaction(final javax.sql.DataSource ds, final Try.Function<javax.sql.DataSource, T, E> func) throws E {
        final SQLTransaction tran = JdbcUtil.beginTransaction(ds);
        T result = null;

        try {
            result = func.apply(ds);
            tran.commit();
        } finally {
            tran.rollbackIfNotCommitted();
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param ds
     * @param runnable
     * @return
     * @throws E
     */
    @Beta
    public static <E extends Exception> void runInTransaction(final javax.sql.DataSource ds, final Try.Runnable<E> runnable) throws E {
        final SQLTransaction tran = JdbcUtil.beginTransaction(ds);

        try {
            runnable.run();
            tran.commit();
        } finally {
            tran.rollbackIfNotCommitted();
        }
    }

    /**
     *
     * @param <E>
     * @param ds
     * @param action
     * @return
     * @throws E
     */
    @Beta
    public static <E extends Exception> void runInTransaction(final javax.sql.DataSource ds, final Try.Consumer<javax.sql.DataSource, E> action) throws E {
        final SQLTransaction tran = JdbcUtil.beginTransaction(ds);

        try {
            action.accept(ds);
            tran.commit();
        } finally {
            tran.rollbackIfNotCommitted();
        }
    }

    /**
     * Gets the SQL operation.
     *
     * @param sql
     * @return
     */
    static SQLOperation getSQLOperation(String sql) {
        if (StringUtil.startsWithIgnoreCase(sql.trim(), "select ")) {
            return SQLOperation.SELECT;
        } else if (StringUtil.startsWithIgnoreCase(sql.trim(), "update ")) {
            return SQLOperation.UPDATE;
        } else if (StringUtil.startsWithIgnoreCase(sql.trim(), "insert ")) {
            return SQLOperation.INSERT;
        } else if (StringUtil.startsWithIgnoreCase(sql.trim(), "delete ")) {
            return SQLOperation.DELETE;
        } else {
            for (SQLOperation so : SQLOperation.values()) {
                if (StringUtil.startsWithIgnoreCase(sql.trim(), so.name())) {
                    return so;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported sql operation: " + sql.substring(0, sql.indexOf(' ')));
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param sql
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedQuery prepareQuery(final javax.sql.DataSource ds, final String sql) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareQuery(tran.connection(), sql);
        } else {
            PreparedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareQuery(conn, sql).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param sql
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedQuery prepareQuery(final javax.sql.DataSource ds, final String sql, final boolean autoGeneratedKeys) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareQuery(tran.connection(), sql, autoGeneratedKeys);
        } else {
            PreparedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareQuery(conn, sql, autoGeneratedKeys).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     *
     * @param ds
     * @param sql
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final javax.sql.DataSource ds, final String sql, final int[] returnColumnIndexes) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareQuery(tran.connection(), sql, returnColumnIndexes);
        } else {
            PreparedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareQuery(conn, sql, returnColumnIndexes).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     *
     * @param ds
     * @param sql
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final javax.sql.DataSource ds, final String sql, final String[] returnColumnNames) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareQuery(tran.connection(), sql, returnColumnNames);
        } else {
            PreparedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareQuery(conn, sql, returnColumnNames).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     *
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param sql
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code PreparedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedQuery prepareQuery(final javax.sql.DataSource ds, final String sql,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareQuery(tran.connection(), sql, stmtCreator);
        } else {
            PreparedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareQuery(conn, sql, stmtCreator).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareQuery(dataSource.getConnection(), sql);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param sql
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final Connection conn, final String sql) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        return new PreparedQuery(conn.prepareStatement(sql));
    }

    /**
     *
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareQuery(dataSource.getConnection(), sql, autoGeneratedKeys);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param sql
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final Connection conn, final String sql, final boolean autoGeneratedKeys) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        return new PreparedQuery(conn.prepareStatement(sql, autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS));
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareQuery(dataSource.getConnection(), sql, returnColumnIndexes);
     * </code>
     * </pre>
     *
     * @param conn
     * @param sql
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final Connection conn, final String sql, final int[] returnColumnIndexes) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");
        N.checkArgNotNullOrEmpty(returnColumnIndexes, "returnColumnIndexes");

        return new PreparedQuery(conn.prepareStatement(sql, returnColumnIndexes));
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareQuery(dataSource.getConnection(), sql, returnColumnNames);
     * </code>
     * </pre>
     *
     * @param conn
     * @param sql
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static PreparedQuery prepareQuery(final Connection conn, final String sql, final String[] returnColumnNames) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");
        N.checkArgNotNullOrEmpty(returnColumnNames, "returnColumnNames");

        return new PreparedQuery(conn.prepareStatement(sql, returnColumnNames));
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareQuery(dataSource.getConnection(), sql, stmtCreator);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param sql
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code PreparedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see {@link JdbcUtil#prepareStatement(Connection, String, Object...)}
     */
    public static PreparedQuery prepareQuery(final Connection conn, final String sql,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");
        N.checkArgNotNull(stmtCreator, "stmtCreator");

        return new PreparedQuery(stmtCreator.apply(conn, sql));
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final String namedSql) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, namedSql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSql);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSql).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final String namedSql, final boolean autoGeneratedKeys) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, namedSql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSql, autoGeneratedKeys);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSql, autoGeneratedKeys).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final String namedSql, final int[] returnColumnIndexes) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, namedSql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSql, returnColumnIndexes);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSql, returnColumnIndexes).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final String namedSql, final String[] returnColumnNames) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, namedSql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSql, returnColumnNames);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSql, returnColumnNames).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     *
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code NamedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final String namedSql,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, namedSql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSql, stmtCreator);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSql, stmtCreator).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSql);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final String namedSql) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSql, "namedSql");

        final NamedSQL namedSQL = createNamedSQL(namedSql);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL()), namedSQL);
    }

    /**
     *
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSql, autoGeneratedKeys);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final String namedSql, final boolean autoGeneratedKeys) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSql, "namedSql");

        final NamedSQL namedSQL = createNamedSQL(namedSql);

        return new NamedQuery(
                conn.prepareStatement(namedSQL.getParameterizedSQL(), autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS),
                namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSql);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final String namedSql, final int[] returnColumnIndexes) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSql, "namedSql");
        N.checkArgNotNullOrEmpty(returnColumnIndexes, "returnColumnIndexes");

        final NamedSQL namedSQL = createNamedSQL(namedSql);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL(), returnColumnIndexes), namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSql);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final String namedSql, final String[] returnColumnNames) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSql, "namedSql");
        N.checkArgNotNullOrEmpty(returnColumnNames, "returnColumnNames");

        final NamedSQL namedSQL = createNamedSQL(namedSql);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL(), returnColumnNames), namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSql, stmtCreator);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSql for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code NamedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see {@link JdbcUtil#prepareStatement(Connection, String, Object...)}
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final String namedSql,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSql, "namedSql");
        N.checkArgNotNull(stmtCreator, "stmtCreator");

        final NamedSQL namedSQL = createNamedSQL(namedSql);

        return new NamedQuery(stmtCreator.apply(conn, namedSQL.getParameterizedSQL()), namedSQL);
    }

    /**
     * Creates the named SQL.
     *
     * @param namedSql
     * @return
     */
    private static NamedSQL createNamedSQL(final String namedSql) {
        N.checkArgNotNullOrEmpty(namedSql, "namedSql");

        final NamedSQL namedSQL = NamedSQL.parse(namedSql);

        validateNamedSQL(namedSQL);

        return namedSQL;
    }

    private static void validateNamedSQL(final NamedSQL namedSQL) {
        if (namedSQL.getNamedParameters().size() != namedSQL.getParameterCount()) {
            throw new IllegalArgumentException("\"" + namedSQL.getNamedSQL() + "\" is not a valid named sql:");
        }
    }

    private static SQLTransaction getTransaction(final javax.sql.DataSource ds, final String sql, final CreatedBy createdBy) {
        final SQLOperation sqlOperation = JdbcUtil.getSQLOperation(sql);
        final SQLTransaction tran = SQLTransaction.getTransaction(ds, createdBy);

        if (tran != null && (tran.isForUpdateOnly() == false || sqlOperation != SQLOperation.SELECT)) {
            return tran;
        } else {
            return null;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final NamedSQL namedSQL) throws SQLException {
        validateNamedSQL(namedSQL);

        final SQLTransaction tran = getTransaction(ds, namedSQL.getParameterizedSQL(), CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSQL);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSQL).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final NamedSQL namedSQL, final boolean autoGeneratedKeys) throws SQLException {
        validateNamedSQL(namedSQL);

        final SQLTransaction tran = getTransaction(ds, namedSQL.getParameterizedSQL(), CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSQL, autoGeneratedKeys);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSQL, autoGeneratedKeys).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final NamedSQL namedSQL, final int[] returnColumnIndexes) throws SQLException {
        validateNamedSQL(namedSQL);

        final SQLTransaction tran = getTransaction(ds, namedSQL.getParameterizedSQL(), CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSQL, returnColumnIndexes);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSQL, returnColumnIndexes).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final NamedSQL namedSQL, final String[] returnColumnNames) throws SQLException {
        validateNamedSQL(namedSQL);

        final SQLTransaction tran = getTransaction(ds, namedSQL.getParameterizedSQL(), CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSQL, returnColumnNames);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSQL, returnColumnNames).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     *
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code NamedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static NamedQuery prepareNamedQuery(final javax.sql.DataSource ds, final NamedSQL namedSQL,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        validateNamedSQL(namedSQL);

        final SQLTransaction tran = getTransaction(ds, namedSQL.getParameterizedSQL(), CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareNamedQuery(tran.connection(), namedSQL, stmtCreator);
        } else {
            NamedQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareNamedQuery(conn, namedSQL, stmtCreator).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSQL);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final NamedSQL namedSQL) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSQL, "namedSQL");
        validateNamedSQL(namedSQL);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL()), namedSQL);
    }

    /**
     *
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSQL, autoGeneratedKeys);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param autoGeneratedKeys
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final NamedSQL namedSQL, final boolean autoGeneratedKeys) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSQL, "namedSQL");
        validateNamedSQL(namedSQL);

        return new NamedQuery(
                conn.prepareStatement(namedSQL.getParameterizedSQL(), autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS),
                namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSQL);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnIndexes
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final NamedSQL namedSQL, final int[] returnColumnIndexes) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSQL, "namedSQL");
        N.checkArgNotNullOrEmpty(returnColumnIndexes, "returnColumnIndexes");
        validateNamedSQL(namedSQL);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL(), returnColumnIndexes), namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSQL);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param returnColumnNames
     * @return
     * @throws SQLException the SQL exception
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final NamedSQL namedSQL, final String[] returnColumnNames) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSQL, "namedSQL");
        N.checkArgNotNullOrEmpty(returnColumnNames, "returnColumnNames");
        validateNamedSQL(namedSQL);

        return new NamedQuery(conn.prepareStatement(namedSQL.getParameterizedSQL(), returnColumnNames), namedSQL);
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareNamedQuery(dataSource.getConnection(), namedSQL, stmtCreator);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param namedSQL for example {@code SELECT first_name, last_name FROM account where id = :id}
     * @param stmtCreator the created {@code PreparedStatement} will be closed after any execution methods in {@code NamedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see {@link JdbcUtil#prepareStatement(Connection, String, Object...)}
     */
    public static NamedQuery prepareNamedQuery(final Connection conn, final NamedSQL namedSQL,
            final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(namedSQL, "namedSQL");
        N.checkArgNotNull(stmtCreator, "stmtCreator");
        validateNamedSQL(namedSQL);

        return new NamedQuery(stmtCreator.apply(conn, namedSQL.getParameterizedSQL()), namedSQL);
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param sql
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedCallableQuery prepareCallableQuery(final javax.sql.DataSource ds, final String sql) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareCallableQuery(tran.connection(), sql);
        } else {
            PreparedCallableQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareCallableQuery(conn, sql).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * If this method is called where a transaction is started by {@code JdbcUtil.beginTransaction} or in {@code Spring} with the same {@code DataSource} in the same thread,
     * the {@code Connection} started the Transaction will be used here.
     * Otherwise a {@code Connection} directly from the specified {@code DataSource}(Connection pool) will be borrowed and used.
     *
     * @param ds
     * @param sql
     * @param stmtCreator the created {@code CallableStatement} will be closed after any execution methods in {@code PreparedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedCallableQuery prepareCallableQuery(final javax.sql.DataSource ds, final String sql,
            final Try.BiFunction<Connection, String, CallableStatement, SQLException> stmtCreator) throws SQLException {
        final SQLTransaction tran = getTransaction(ds, sql, CreatedBy.JDBC_UTIL);

        if (tran != null) {
            return prepareCallableQuery(tran.connection(), sql, stmtCreator);
        } else {
            PreparedCallableQuery result = null;
            Connection conn = null;

            try {
                conn = getConnection(ds);
                result = prepareCallableQuery(conn, sql, stmtCreator).onClose(createCloseHandler(conn, ds));
            } finally {
                if (result == null) {
                    releaseConnection(conn, ds);
                }
            }

            return result;
        }
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareCallableQuery(dataSource.getConnection(), sql);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param sql
     * @return
     * @throws SQLException the SQL exception
     * @see #getConnection(javax.sql.DataSource)
     * @see #releaseConnection(Connection, javax.sql.DataSource)
     */
    public static PreparedCallableQuery prepareCallableQuery(final Connection conn, final String sql) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        return new PreparedCallableQuery(conn.prepareCall(sql));
    }

    /**
     * Never write below code because it will definitely cause {@code Connection} leak:
     * <pre>
     * <code>
     * JdbcUtil.prepareCallableQuery(dataSource.getConnection(), sql, stmtCreator);
     * </code>
     * </pre>
     *
     * @param conn the specified {@code conn} won't be close after this query is executed.
     * @param sql
     * @param stmtCreator the created {@code CallableStatement} will be closed after any execution methods in {@code PreparedQuery/PreparedCallableQuery} is called.
     * An execution method is a method which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/....
     * @return
     * @throws SQLException the SQL exception
     * @see {@link JdbcUtil#prepareCall(Connection, String, Object...)}
     */
    public static PreparedCallableQuery prepareCallableQuery(final Connection conn, final String sql,
            final Try.BiFunction<Connection, String, CallableStatement, SQLException> stmtCreator) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");
        N.checkArgNotNull(stmtCreator, "stmtCreator");

        return new PreparedCallableQuery(stmtCreator.apply(conn, sql));
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException the SQL exception
     */
    @SafeVarargs
    static PreparedStatement prepareStatement(final Connection conn, final String sql, final Object... parameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        final NamedSQL namedSQL = NamedSQL.parse(sql);
        final PreparedStatement stmt = conn.prepareStatement(namedSQL.getParameterizedSQL());

        if (N.notNullOrEmpty(parameters)) {
            StatementSetter.DEFAULT.setParameters(namedSQL, stmt, parameters);
        }

        return stmt;
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException the SQL exception
     */
    @SafeVarargs
    static CallableStatement prepareCall(final Connection conn, final String sql, final Object... parameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        final NamedSQL namedSQL = NamedSQL.parse(sql);
        final CallableStatement stmt = conn.prepareCall(namedSQL.getParameterizedSQL());

        if (N.notNullOrEmpty(parameters)) {
            StatementSetter.DEFAULT.setParameters(namedSQL, stmt, parameters);
        }

        return stmt;
    }

    /**
     * Batch prepare statement.
     *
     * @param conn
     * @param sql
     * @param parametersList
     * @return
     * @throws SQLException the SQL exception
     */
    static PreparedStatement prepareBatchStatement(final Connection conn, final String sql, final List<?> parametersList) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        final NamedSQL namedSQL = NamedSQL.parse(sql);
        final PreparedStatement stmt = conn.prepareStatement(namedSQL.getParameterizedSQL());

        for (Object parameters : parametersList) {
            StatementSetter.DEFAULT.setParameters(namedSQL, stmt, N.asArray(parameters));
            stmt.addBatch();
        }

        return stmt;
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parametersList
     * @return
     * @throws SQLException the SQL exception
     */
    static CallableStatement prepareBatchCall(final Connection conn, final String sql, final List<?> parametersList) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        final NamedSQL namedSQL = NamedSQL.parse(sql);
        final CallableStatement stmt = conn.prepareCall(namedSQL.getParameterizedSQL());

        for (Object parameters : parametersList) {
            StatementSetter.DEFAULT.setParameters(namedSQL, stmt, N.asArray(parameters));
            stmt.addBatch();
        }

        return stmt;
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException the SQL exception
     */
    @SafeVarargs
    public static DataSet executeQuery(final Connection conn, final String sql, final Object... parameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = prepareStatement(conn, sql, parameters);

            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            rs = stmt.executeQuery();

            return extractData(rs);
        } finally {
            closeQuietly(rs, stmt);
        }
    }

    /**
     *
     * @param stmt
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet executeQuery(final PreparedStatement stmt) throws SQLException {
        ResultSet rs = null;

        try {
            rs = stmt.executeQuery();

            return extractData(rs);
        } finally {
            closeQuietly(rs);
        }
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parameters
     * @return
     * @throws SQLException the SQL exception
     */
    @SafeVarargs
    public static int executeUpdate(final Connection conn, final String sql, final Object... parameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, sql, parameters);

            return stmt.executeUpdate();
        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     * Execute batch update.
     *
     * @param conn
     * @param sql
     * @param listOfParameters
     * @return
     * @throws SQLException the SQL exception
     */
    public static int executeBatchUpdate(final Connection conn, final String sql, final List<?> listOfParameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        return executeBatchUpdate(conn, sql, listOfParameters, JdbcSettings.DEFAULT_BATCH_SIZE);
    }

    /**
     * Execute batch update.
     *
     * @param conn
     * @param sql
     * @param listOfParameters
     * @param batchSize
     * @return
     * @throws SQLException the SQL exception
     */
    public static int executeBatchUpdate(final Connection conn, final String sql, final List<?> listOfParameters, final int batchSize) throws SQLException {
        N.checkArgNotNull(conn);
        N.checkArgNotNull(sql);
        N.checkArgPositive(batchSize, "batchSize");

        if (N.isNullOrEmpty(listOfParameters)) {
            return 0;
        }

        final NamedSQL namedSQL = NamedSQL.parse(sql);
        final boolean originalAutoCommit = conn.getAutoCommit();
        PreparedStatement stmt = null;
        boolean noException = false;

        try {
            if (originalAutoCommit && listOfParameters.size() > batchSize) {
                conn.setAutoCommit(false);
            }

            stmt = conn.prepareStatement(namedSQL.getParameterizedSQL());

            int res = 0;
            int idx = 0;

            for (Object parameters : listOfParameters) {
                StatementSetter.DEFAULT.setParameters(namedSQL, stmt, parameters);
                stmt.addBatch();

                if (++idx % batchSize == 0) {
                    res += N.sum(stmt.executeBatch());
                    stmt.clearBatch();
                }
            }

            if (idx % batchSize != 0) {
                res += N.sum(stmt.executeBatch());
                stmt.clearBatch();
            }

            noException = true;

            return res;
        } finally {
            if (originalAutoCommit && listOfParameters.size() > batchSize) {
                try {
                    if (noException) {
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } finally {
                        JdbcUtil.closeQuietly(stmt);
                    }
                }
            } else {
                JdbcUtil.closeQuietly(stmt);
            }
        }
    }

    /**
     *
     * @param conn
     * @param sql
     * @param parameters
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @SafeVarargs
    public static boolean execute(final Connection conn, final String sql, final Object... parameters) throws SQLException {
        N.checkArgNotNull(conn, "conn");
        N.checkArgNotNull(sql, "sql");

        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, sql, parameters);

            return stmt.execute();
        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     *
     * @param rs
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet extractData(final ResultSet rs) throws SQLException {
        return extractData(rs, false);
    }

    /**
     *
     * @param rs
     * @param closeResultSet
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet extractData(final ResultSet rs, final boolean closeResultSet) throws SQLException {
        return extractData(rs, 0, Integer.MAX_VALUE, closeResultSet);
    }

    /**
     *
     * @param rs
     * @param offset
     * @param count
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet extractData(final ResultSet rs, final int offset, final int count) throws SQLException {
        return extractData(rs, offset, count, false);
    }

    /**
     *
     * @param rs
     * @param offset
     * @param count
     * @param closeResultSet
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet extractData(final ResultSet rs, final int offset, final int count, final boolean closeResultSet) throws SQLException {
        return extractData(rs, offset, count, RowFilter.ALWAYS_TRUE, closeResultSet);
    }

    /**
     *
     * @param rs
     * @param offset
     * @param count
     * @param filter
     * @param closeResultSet
     * @return
     * @throws SQLException the SQL exception
     */
    public static DataSet extractData(final ResultSet rs, int offset, int count, final RowFilter filter, final boolean closeResultSet) throws SQLException {
        N.checkArgNotNull(rs, "ResultSet");
        N.checkArgNotNegative(offset, "offset");
        N.checkArgNotNegative(count, "count");
        N.checkArgNotNull(filter, "filter");

        try {
            // TODO [performance improvement]. it will improve performance a lot if MetaData is cached.
            final ResultSetMetaData rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            final List<String> columnNameList = new ArrayList<>(columnCount);
            final List<List<Object>> columnList = new ArrayList<>(columnCount);

            for (int i = 0; i < columnCount;) {
                columnNameList.add(JdbcUtil.getColumnLabel(rsmd, ++i));
                columnList.add(new ArrayList<>());
            }

            JdbcUtil.skip(rs, offset);

            while (count > 0 && rs.next()) {
                if (filter == null || filter.test(rs)) {
                    for (int i = 0; i < columnCount;) {
                        columnList.get(i).add(JdbcUtil.getColumnValue(rs, ++i));
                    }

                    count--;
                }
            }

            // return new RowDataSet(null, entityClass, columnNameList, columnList);
            return new RowDataSet(columnNameList, columnList);
        } finally {
            if (closeResultSet) {
                closeQuietly(rs);
            }
        }
    }

    /**
     * Does table exist.
     *
     * @param conn
     * @param tableName
     * @return true, if successful
     */
    public static boolean doesTableExist(final Connection conn, final String tableName) {
        try {
            executeQuery(conn, "SELECT 1 FROM " + tableName + " WHERE 1 > 2");

            return true;
        } catch (SQLException e) {
            if (isTableNotExistsException(e)) {
                return false;
            }

            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Returns {@code true} if succeed to create table, otherwise {@code false} is returned.
     *
     * @param conn
     * @param tableName
     * @param schema
     * @return true, if successful
     */
    public static boolean createTableIfNotExists(final Connection conn, final String tableName, final String schema) {
        if (doesTableExist(conn, tableName)) {
            return false;
        }

        try {
            execute(conn, schema);

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if succeed to drop table, otherwise {@code false} is returned.
     *
     * @param conn
     * @param tableName
     * @return true, if successful
     */
    public static boolean dropTableIfExists(final Connection conn, final String tableName) {
        try {
            if (doesTableExist(conn, tableName)) {
                execute(conn, "DROP TABLE " + tableName);

                return true;
            }
        } catch (SQLException e) {
            // ignore.
        }

        return false;
    }

    /**
     * Gets the named parameters.
     *
     * @param sql
     * @return
     */
    public static List<String> getNamedParameters(String sql) {
        return NamedSQL.parse(sql).getNamedParameters();
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Connection conn, final String insertSQL) throws UncheckedSQLException {
        return importData(dataset, dataset.columnNameList(), conn, insertSQL);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final Connection conn, final String insertSQL)
            throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, 0, dataset.size(), conn, insertSQL);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count, final Connection conn,
            final String insertSQL) throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, offset, count, conn, insertSQL, 200, 0);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count, final Connection conn,
            final String insertSQL, final int batchSize, final int batchInterval) throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, offset, count, Fn.alwaysTrue(), conn, insertSQL, batchSize, batchInterval);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final Connection conn, final String insertSQL, final int batchSize, final int batchInterval)
            throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(dataset, selectColumnNames, offset, count, filter, stmt, batchSize, batchInterval);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final Connection conn, final String insertSQL, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedSQLException {
        return importData(dataset, 0, dataset.size(), conn, insertSQL, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final int offset, final int count, final Connection conn, final String insertSQL,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException {
        return importData(dataset, offset, count, conn, insertSQL, 200, 0, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final int offset, final int count, final Connection conn, final String insertSQL, final int batchSize,
            final int batchInterval, final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException {
        return importData(dataset, offset, count, Fn.alwaysTrue(), conn, insertSQL, batchSize, batchInterval, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param offset
     * @param count
     * @param filter
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> int importData(final DataSet dataset, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(dataset, offset, count, filter, stmt, batchSize, batchInterval, columnTypeMap);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Connection conn, final String insertSQL,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, 0, dataset.size(), conn, insertSQL, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final int offset, final int count, final Connection conn, final String insertSQL,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, offset, count, conn, insertSQL, 200, 0, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final int offset, final int count, final Connection conn, final String insertSQL, final int batchSize,
            final int batchInterval, final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, offset, count, Fn.alwaysTrue(), conn, insertSQL, batchSize, batchInterval, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param offset
     * @param count
     * @param filter
     * @param conn
     * @param insertSQL the column order in the sql must be consistent with the column order in the DataSet. Here is sample about how to create the sql:
     * <pre><code>
     *         List<String> columnNameList = new ArrayList<>(dataset.columnNameList());
     *         columnNameList.retainAll(yourSelectColumnNames);
     *         String sql = RE.insert(columnNameList).into(tableName).sql();
     * </code></pre>
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> int importData(final DataSet dataset, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(dataset, offset, count, filter, stmt, batchSize, batchInterval, stmtSetter);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final PreparedStatement stmt) throws UncheckedSQLException {
        return importData(dataset, dataset.columnNameList(), stmt);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final PreparedStatement stmt) throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, 0, dataset.size(), stmt);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count,
            final PreparedStatement stmt) throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, offset, count, stmt, 200, 0);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count,
            final PreparedStatement stmt, final int batchSize, final int batchInterval) throws UncheckedSQLException {
        return importData(dataset, selectColumnNames, offset, count, Fn.alwaysTrue(), stmt, batchSize, batchInterval);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param selectColumnNames
     * @param offset
     * @param count
     * @param filter
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> int importData(final DataSet dataset, final Collection<String> selectColumnNames, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final PreparedStatement stmt, final int batchSize, final int batchInterval)
            throws UncheckedSQLException, E {
        final Type<?> objType = N.typeOf(Object.class);
        final Map<String, Type<?>> columnTypeMap = new HashMap<>();

        for (String propName : selectColumnNames) {
            columnTypeMap.put(propName, objType);
        }

        return importData(dataset, offset, count, filter, stmt, batchSize, batchInterval, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final PreparedStatement stmt, final Map<String, ? extends Type> columnTypeMap)
            throws UncheckedSQLException {
        return importData(dataset, 0, dataset.size(), stmt, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final int offset, final int count, final PreparedStatement stmt,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException {
        return importData(dataset, offset, count, stmt, 200, 0, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @SuppressWarnings("rawtypes")
    public static int importData(final DataSet dataset, final int offset, final int count, final PreparedStatement stmt, final int batchSize,
            final int batchInterval, final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException {
        return importData(dataset, offset, count, Fn.alwaysTrue(), stmt, batchSize, batchInterval, columnTypeMap);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param offset
     * @param count
     * @param filter
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @param columnTypeMap
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Exception> int importData(final DataSet dataset, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final Map<String, ? extends Type> columnTypeMap) throws UncheckedSQLException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        int result = 0;

        try {
            final int columnCount = columnTypeMap.size();
            final List<String> columnNameList = dataset.columnNameList();
            final int[] columnIndexes = new int[columnCount];
            final Type<Object>[] columnTypes = new Type[columnCount];
            final Set<String> columnNameSet = N.newHashSet(columnCount);

            int idx = 0;
            for (String columnName : columnNameList) {
                if (columnTypeMap.containsKey(columnName)) {
                    columnIndexes[idx] = dataset.getColumnIndex(columnName);
                    columnTypes[idx] = columnTypeMap.get(columnName);
                    columnNameSet.add(columnName);
                    idx++;
                }
            }

            if (columnNameSet.size() != columnTypeMap.size()) {
                final List<String> keys = new ArrayList<>(columnTypeMap.keySet());
                keys.removeAll(columnNameSet);
                throw new AbacusException(keys + " are not included in titles: " + N.toString(columnNameList));
            }

            final Object[] row = filter == null ? null : new Object[columnCount];
            for (int i = offset, size = dataset.size(); result < count && i < size; i++) {
                dataset.absolute(i);

                if (filter == null) {
                    for (int j = 0; j < columnCount; j++) {
                        columnTypes[j].set(stmt, j + 1, dataset.get(columnIndexes[j]));
                    }
                } else {
                    for (int j = 0; j < columnCount; j++) {
                        row[j] = dataset.get(columnIndexes[j]);
                    }

                    if (filter.test(row) == false) {
                        continue;
                    }

                    for (int j = 0; j < columnCount; j++) {
                        columnTypes[j].set(stmt, j + 1, row[j]);
                    }
                }

                stmt.addBatch();

                if ((++result % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((result % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        return result;
    }

    /**
     *
     * @param dataset
     * @param stmt
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final PreparedStatement stmt,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, 0, dataset.size(), stmt, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final int offset, final int count, final PreparedStatement stmt,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, offset, count, stmt, 200, 0, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param dataset
     * @param offset
     * @param count
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static int importData(final DataSet dataset, final int offset, final int count, final PreparedStatement stmt, final int batchSize,
            final int batchInterval, final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return importData(dataset, offset, count, Fn.alwaysTrue(), stmt, batchSize, batchInterval, stmtSetter);
    }

    /**
     * Imports the data from <code>DataSet</code> to database.
     *
     * @param <E>
     * @param dataset
     * @param offset
     * @param count
     * @param filter
     * @param stmt the column order in the sql must be consistent with the column order in the DataSet.
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> int importData(final DataSet dataset, final int offset, final int count,
            final Try.Predicate<? super Object[], E> filter, final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        final int columnCount = dataset.columnNameList().size();
        final Object[] row = new Object[columnCount];
        int result = 0;

        try {
            for (int i = offset, size = dataset.size(); result < count && i < size; i++) {
                dataset.absolute(i);

                for (int j = 0; j < columnCount; j++) {
                    row[j] = dataset.get(j);
                }

                if (filter != null && filter.test(row) == false) {
                    continue;
                }

                stmtSetter.accept(stmt, row);

                stmt.addBatch();

                if ((++result % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((result % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param file
     * @param conn
     * @param insertSQL
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final File file, final Connection conn, final String insertSQL,
            final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        return importData(file, 0, Long.MAX_VALUE, conn, insertSQL, 200, 0, func);
    }

    /**
     *
     * @param <E>
     * @param file
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final File file, final long offset, final long count, final Connection conn, final String insertSQL,
            final int batchSize, final int batchInterval, final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(file, offset, count, stmt, batchSize, batchInterval, func);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <E>
     * @param file
     * @param stmt
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final File file, final PreparedStatement stmt, final Try.Function<String, Object[], E> func)
            throws UncheckedSQLException, E {
        return importData(file, 0, Long.MAX_VALUE, stmt, 200, 0, func);
    }

    /**
     * Imports the data from file to database.
     *
     * @param <E>
     * @param file
     * @param offset
     * @param count
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param func convert line to the parameters for record insert. Returns a <code>null</code> array to skip the line.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final File file, final long offset, final long count, final PreparedStatement stmt, final int batchSize,
            final int batchInterval, final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        Reader reader = null;

        try {
            reader = new FileReader(file);

            return importData(reader, offset, count, stmt, batchSize, batchInterval, func);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     *
     * @param <E>
     * @param is
     * @param conn
     * @param insertSQL
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final InputStream is, final Connection conn, final String insertSQL,
            final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        return importData(is, 0, Long.MAX_VALUE, conn, insertSQL, 200, 0, func);
    }

    /**
     *
     * @param <E>
     * @param is
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final InputStream is, final long offset, final long count, final Connection conn,
            final String insertSQL, final int batchSize, final int batchInterval, final Try.Function<String, Object[], E> func)
            throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(is, offset, count, stmt, batchSize, batchInterval, func);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <E>
     * @param is
     * @param stmt
     * @param func
     * @return
     * @throws E the e
     */
    public static <E extends Exception> long importData(final InputStream is, final PreparedStatement stmt, final Try.Function<String, Object[], E> func)
            throws E {
        return importData(is, 0, Long.MAX_VALUE, stmt, 200, 0, func);
    }

    /**
     * Imports the data from file to database.
     *
     * @param <E>
     * @param is
     * @param offset
     * @param count
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param func convert line to the parameters for record insert. Returns a <code>null</code> array to skip the line.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final InputStream is, final long offset, final long count, final PreparedStatement stmt,
            final int batchSize, final int batchInterval, final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        final Reader reader = new InputStreamReader(is);

        return importData(reader, offset, count, stmt, batchSize, batchInterval, func);
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param conn
     * @param insertSQL
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final Reader reader, final Connection conn, final String insertSQL,
            final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        return importData(reader, 0, Long.MAX_VALUE, conn, insertSQL, 200, 0, func);
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final Reader reader, final long offset, final long count, final Connection conn, final String insertSQL,
            final int batchSize, final int batchInterval, final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(reader, offset, count, stmt, batchSize, batchInterval, func);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param stmt
     * @param func
     * @return
     * @throws E the e
     */
    public static <E extends Exception> long importData(final Reader reader, final PreparedStatement stmt, final Try.Function<String, Object[], E> func)
            throws E {
        return importData(reader, 0, Long.MAX_VALUE, stmt, 200, 0, func);
    }

    /**
     * Imports the data from file to database.
     *
     * @param <E>
     * @param reader
     * @param offset
     * @param count
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param func convert line to the parameters for record insert. Returns a <code>null</code> array to skip the line.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> long importData(final Reader reader, long offset, final long count, final PreparedStatement stmt, final int batchSize,
            final int batchInterval, final Try.Function<String, Object[], E> func) throws UncheckedSQLException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        long result = 0;
        final BufferedReader br = Objectory.createBufferedReader(reader);

        try {
            while (offset-- > 0 && br.readLine() != null) {
            }

            String line = null;
            Object[] row = null;

            while (result < count && (line = br.readLine()) != null) {
                row = func.apply(line);

                if (row == null) {
                    continue;
                }

                for (int i = 0, len = row.length; i < len; i++) {
                    stmt.setObject(i + 1, row[i]);
                }

                stmt.addBatch();

                if ((++result % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((result % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(br);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param conn
     * @param insertSQL
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, final Connection conn, final String insertSQL,
            final Try.Function<? super T, Object[], E> func) throws UncheckedSQLException, E {
        return importData(iter, 0, Long.MAX_VALUE, conn, insertSQL, 200, 0, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param func
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, final long offset, final long count, final Connection conn,
            final String insertSQL, final int batchSize, final int batchInterval, final Try.Function<? super T, Object[], E> func)
            throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(iter, offset, count, stmt, batchSize, batchInterval, func);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param stmt
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, final PreparedStatement stmt,
            final Try.Function<? super T, Object[], E> func) throws E {
        return importData(iter, 0, Long.MAX_VALUE, stmt, 200, 0, func);
    }

    /**
     * Imports the data from Iterator to database.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param func convert element to the parameters for record insert. Returns a <code>null</code> array to skip the line.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, long offset, final long count, final PreparedStatement stmt,
            final int batchSize, final int batchInterval, final Try.Function<? super T, Object[], E> func) throws UncheckedSQLException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        long result = 0;

        try {
            while (offset-- > 0 && iter.hasNext()) {
                iter.next();
            }

            Object[] row = null;

            while (result < count && iter.hasNext()) {
                row = func.apply(iter.next());

                if (row == null) {
                    continue;
                }

                for (int i = 0, len = row.length; i < len; i++) {
                    stmt.setObject(i + 1, row[i]);
                }

                stmt.addBatch();

                if ((++result % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((result % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param conn
     * @param insertSQL
     * @param stmtSetter
     * @return
     */
    public static <T> long importData(final Iterator<T> iter, final Connection conn, final String insertSQL,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) {
        return importData(iter, 0, Long.MAX_VALUE, conn, insertSQL, 200, 0, stmtSetter);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param offset
     * @param count
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     */
    public static <T> long importData(final Iterator<T> iter, final long offset, final long count, final Connection conn, final String insertSQL,
            final int batchSize, final int batchInterval, final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) {
        return importData(iter, offset, count, Fn.alwaysTrue(), conn, insertSQL, batchSize, batchInterval, stmtSetter);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param filter
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, final long offset, final long count,
            final Try.Predicate<? super T, E> filter, final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws UncheckedSQLException, E {
        PreparedStatement stmt = null;

        try {
            stmt = prepareStatement(conn, insertSQL);

            return importData(iter, offset, count, filter, stmt, batchSize, batchInterval, stmtSetter);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param stmt
     * @param stmtSetter
     * @return
     */
    public static <T> long importData(final Iterator<T> iter, final PreparedStatement stmt,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) {
        return importData(iter, 0, Long.MAX_VALUE, stmt, 200, 0, stmtSetter);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param offset
     * @param count
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     */
    public static <T> long importData(final Iterator<T> iter, long offset, final long count, final PreparedStatement stmt, final int batchSize,
            final int batchInterval, final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) {
        return importData(iter, offset, count, Fn.alwaysTrue(), stmt, batchSize, batchInterval, stmtSetter);
    }

    /**
     * Imports the data from Iterator to database.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param filter
     * @param stmt
     * @param batchSize
     * @param batchInterval
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <T, E extends Exception> long importData(final Iterator<T> iter, long offset, final long count, final Try.Predicate<? super T, E> filter,
            final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws UncheckedSQLException, E {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        long result = 0;

        try {
            while (offset-- > 0 && iter.hasNext()) {
                iter.next();
            }
            T next = null;
            while (result < count && iter.hasNext()) {
                next = iter.next();

                if (filter != null && filter.test(next) == false) {
                    continue;
                }

                stmtSetter.accept(stmt, next);
                stmt.addBatch();

                if ((++result % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((result % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param conn
     * @param sql
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Connection conn, final String sql, final Try.Consumer<Object[], E> rowParser)
            throws UncheckedSQLException, E {
        parse(conn, sql, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param conn
     * @param sql
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Connection conn, final String sql, final Try.Consumer<Object[], E> rowParser,
            final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(conn, sql, 0, Long.MAX_VALUE, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param conn
     * @param sql
     * @param offset
     * @param count
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Connection conn, final String sql, final long offset, final long count,
            final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(conn, sql, offset, count, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param conn
     * @param sql
     * @param offset
     * @param count
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Connection conn, final String sql, final long offset, final long count,
            final Try.Consumer<Object[], E> rowParser, final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(conn, sql, offset, count, 0, 0, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param conn
     * @param sql
     * @param offset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Connection conn, final String sql, final long offset, final long count, final int processThreadNum,
            final int queueSize, final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(conn, sql, offset, count, processThreadNum, queueSize, rowParser, Fn.emptyAction());
    }

    /**
     * Parse the ResultSet obtained by executing query with the specified Connection and sql.
     *
     * @param <E>
     * @param <E2>
     * @param conn
     * @param sql
     * @param offset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Connection conn, final String sql, final long offset, final long count,
            final int processThreadNum, final int queueSize, final Try.Consumer<Object[], E> rowParser, final Try.Runnable<E2> onComplete)
            throws UncheckedSQLException, E, E2 {
        PreparedStatement stmt = null;
        try {
            stmt = prepareStatement(conn, sql);

            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);

            stmt.setFetchSize(200);

            parse(stmt, offset, count, processThreadNum, queueSize, rowParser, onComplete);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     *
     * @param <E>
     * @param stmt
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final PreparedStatement stmt, final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(stmt, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param stmt
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final PreparedStatement stmt, final Try.Consumer<Object[], E> rowParser,
            final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(stmt, 0, Long.MAX_VALUE, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param stmt
     * @param offset
     * @param count
     * @param rowParser
     * @throws E the e
     */
    public static <E extends Exception> void parse(final PreparedStatement stmt, final long offset, final long count, final Try.Consumer<Object[], E> rowParser)
            throws E {
        parse(stmt, offset, count, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param stmt
     * @param offset
     * @param count
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final PreparedStatement stmt, final long offset, final long count,
            final Try.Consumer<Object[], E> rowParser, final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(stmt, offset, count, 0, 0, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param stmt
     * @param offset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final PreparedStatement stmt, final long offset, final long count, final int processThreadNum,
            final int queueSize, final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(stmt, offset, count, processThreadNum, queueSize, rowParser, Fn.emptyAction());
    }

    /**
     * Parse the ResultSet obtained by executing query with the specified PreparedStatement.
     *
     * @param <E>
     * @param <E2>
     * @param stmt
     * @param offset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final PreparedStatement stmt, final long offset, final long count,
            final int processThreadNum, final int queueSize, final Try.Consumer<Object[], E> rowParser, final Try.Runnable<E2> onComplete)
            throws UncheckedSQLException, E, E2 {
        ResultSet rs = null;

        try {
            rs = stmt.executeQuery();

            parse(rs, offset, count, processThreadNum, queueSize, rowParser, onComplete);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            closeQuietly(rs);
        }
    }

    /**
     *
     * @param <E>
     * @param rs
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final ResultSet rs, final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(rs, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param rs
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final ResultSet rs, final Try.Consumer<Object[], E> rowParser,
            final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(rs, 0, Long.MAX_VALUE, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param rs
     * @param offset
     * @param count
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final ResultSet rs, long offset, long count, final Try.Consumer<Object[], E> rowParser)
            throws UncheckedSQLException, E {
        parse(rs, offset, count, rowParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param rs
     * @param offset
     * @param count
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final ResultSet rs, long offset, long count, final Try.Consumer<Object[], E> rowParser,
            final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {
        parse(rs, offset, count, 0, 0, rowParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param rs
     * @param offset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param rowParser
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final ResultSet rs, long offset, long count, final int processThreadNum, final int queueSize,
            final Try.Consumer<Object[], E> rowParser) throws UncheckedSQLException, E {
        parse(rs, offset, count, processThreadNum, queueSize, rowParser, Fn.emptyAction());
    }

    /**
     * Parse the ResultSet.
     *
     * @param <E>
     * @param <E2>
     * @param rs
     * @param offset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param rowParser
     * @param onComplete
     * @throws UncheckedSQLException the unchecked SQL exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final ResultSet rs, long offset, long count, final int processThreadNum,
            final int queueSize, final Try.Consumer<Object[], E> rowParser, final Try.Runnable<E2> onComplete) throws UncheckedSQLException, E, E2 {

        final Iterator<Object[]> iter = new ObjIterator<Object[]>() {
            private final JdbcUtil.BiRowMapper<Object[]> biFunc = BiRowMapper.TO_ARRAY;
            private List<String> columnLabels = null;
            private boolean hasNext;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    try {
                        hasNext = rs.next();
                    } catch (SQLException e) {
                        throw new UncheckedSQLException(e);
                    }
                }

                return hasNext;
            }

            @Override
            public Object[] next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                try {
                    if (columnLabels == null) {
                        columnLabels = JdbcUtil.getColumnLabelList(rs);
                    }

                    return biFunc.apply(rs, columnLabels);
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            }
        };

        Iterables.parse(iter, offset, count, processThreadNum, queueSize, rowParser, onComplete);
    }

    /**
     *
     * @param sourceConn
     * @param selectSql
     * @param targetConn
     * @param insertSql
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static long copy(final Connection sourceConn, final String selectSql, final Connection targetConn, final String insertSql)
            throws UncheckedSQLException {
        return copy(sourceConn, selectSql, 200, 0, Integer.MAX_VALUE, targetConn, insertSql, DEFAULT_STMT_SETTER, 200, 0, false);
    }

    /**
     *
     * @param sourceConn
     * @param selectSql
     * @param fetchSize
     * @param offset
     * @param count
     * @param targetConn
     * @param insertSql
     * @param stmtSetter
     * @param batchSize
     * @param batchInterval
     * @param inParallel do the read and write in separated threads.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static long copy(final Connection sourceConn, final String selectSql, final int fetchSize, final long offset, final long count,
            final Connection targetConn, final String insertSql, final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter,
            final int batchSize, final int batchInterval, final boolean inParallel) throws UncheckedSQLException {
        PreparedStatement selectStmt = null;
        PreparedStatement insertStmt = null;

        int result = 0;

        try {
            insertStmt = targetConn.prepareStatement(insertSql);

            selectStmt = sourceConn.prepareStatement(selectSql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            selectStmt.setFetchSize(fetchSize);

            copy(selectStmt, offset, count, insertStmt, stmtSetter, batchSize, batchInterval, inParallel);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            closeQuietly(selectStmt);
            closeQuietly(insertStmt);
        }

        return result;
    }

    /**
     *
     * @param selectStmt
     * @param insertStmt
     * @param stmtSetter
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static long copy(final PreparedStatement selectStmt, final PreparedStatement insertStmt,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter) throws UncheckedSQLException {
        return copy(selectStmt, 0, Integer.MAX_VALUE, insertStmt, stmtSetter, 200, 0, false);
    }

    /**
     *
     * @param selectStmt
     * @param offset
     * @param count
     * @param insertStmt
     * @param stmtSetter
     * @param batchSize
     * @param batchInterval
     * @param inParallel do the read and write in separated threads.
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static long copy(final PreparedStatement selectStmt, final long offset, final long count, final PreparedStatement insertStmt,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> stmtSetter, final int batchSize, final int batchInterval,
            final boolean inParallel) throws UncheckedSQLException {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can't be negative", offset, count);
        N.checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        @SuppressWarnings("rawtypes")
        final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super Object[]> setter = (JdbcUtil.BiParametersSetter) (stmtSetter == null
                ? DEFAULT_STMT_SETTER
                : stmtSetter);
        final AtomicLong result = new AtomicLong();

        final Try.Consumer<Object[], RuntimeException> rowParser = new Try.Consumer<Object[], RuntimeException>() {
            @Override
            public void accept(Object[] row) {
                try {
                    setter.accept(insertStmt, row);

                    insertStmt.addBatch();
                    result.incrementAndGet();

                    if ((result.longValue() % batchSize) == 0) {
                        insertStmt.executeBatch();
                        insertStmt.clearBatch();

                        if (batchInterval > 0) {
                            N.sleep(batchInterval);
                        }
                    }
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            }
        };

        final Try.Runnable<RuntimeException> onComplete = new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                if ((result.longValue() % batchSize) > 0) {
                    try {
                        insertStmt.executeBatch();
                        insertStmt.clearBatch();
                    } catch (SQLException e) {
                        throw new UncheckedSQLException(e);
                    }
                }
            }
        };

        parse(selectStmt, offset, count, 0, inParallel ? DEFAULT_QUEUE_SIZE_FOR_ROW_PARSER : 0, rowParser, onComplete);

        return result.longValue();
    }

    /**
     * Checks if is table not exists exception.
     *
     * @param e
     * @return true, if is table not exists exception
     */
    static boolean isTableNotExistsException(final Throwable e) {
        if (e instanceof SQLException) {
            SQLException sqlException = (SQLException) e;

            if (sqlException.getSQLState() != null && sqlStateForTableNotExists.contains(sqlException.getSQLState())) {
                return true;
            }

            final String msg = N.defaultIfNull(e.getMessage(), "").toLowerCase();
            return N.notNullOrEmpty(msg) && (msg.contains("not exist") || msg.contains("doesn't exist") || msg.contains("not found"));
        } else if (e instanceof UncheckedSQLException) {
            UncheckedSQLException sqlException = (UncheckedSQLException) e;

            if (sqlException.getSQLState() != null && sqlStateForTableNotExists.contains(sqlException.getSQLState())) {
                return true;
            }

            final String msg = N.defaultIfNull(e.getMessage(), "").toLowerCase();
            return N.notNullOrEmpty(msg) && (msg.contains("not exist") || msg.contains("doesn't exist") || msg.contains("not found"));
        }

        return false;
    }

    /** The Constant column2FieldNameMapPool. */
    private static final Map<Class<?>, Map<String, String>> column2FieldNameMapPool = new ConcurrentHashMap<>();

    /**
     * Gets the column 2 field name map.
     *
     * @param entityClass
     * @return
     */
    static Map<String, String> getColumn2FieldNameMap(Class<?> entityClass) {
        Map<String, String> result = column2FieldNameMapPool.get(entityClass);

        if (result == null) {
            result = N.newBiMap(LinkedHashMap.class, LinkedHashMap.class);

            final Set<Field> allFields = N.newHashSet();

            for (Class<?> superClass : ClassUtil.getAllSuperclasses(entityClass)) {
                allFields.addAll(Array.asList(superClass.getDeclaredFields()));
            }

            allFields.addAll(Array.asList(entityClass.getDeclaredFields()));

            for (Field field : allFields) {
                if (ClassUtil.getPropGetMethod(entityClass, field.getName()) != null) {
                    String columnName = null;

                    if (field.isAnnotationPresent(Column.class)) {
                        columnName = field.getAnnotation(Column.class).value();
                    } else {
                        try {
                            if (field.isAnnotationPresent(javax.persistence.Column.class)) {
                                columnName = field.getAnnotation(javax.persistence.Column.class).name();
                            }
                        } catch (Throwable e) {
                            logger.warn("To support javax.persistence.Table/Column, please add dependence javax.persistence:persistence-api");
                        }
                    }

                    if (N.notNullOrEmpty(columnName)) {
                        result.put(columnName, field.getName());
                        result.put(columnName.toLowerCase(), field.getName());
                        result.put(columnName.toUpperCase(), field.getName());
                    }
                }
            }

            result = ImmutableMap.of(result);

            column2FieldNameMapPool.put(entityClass, result);
        }

        return result;
    }

    /**
     * Checks if is default id prop value.
     *
     * @param propValue
     * @return true, if is default id prop value
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static boolean isDefaultIdPropValue(final Object propValue) {
        return (propValue == null) || (propValue instanceof Number && (((Number) propValue).longValue() == 0));
    }

    /**
     *
     * @param sqlCmd
     * @throws UncheckedSQLException
     */
    @Beta
    static void run(Try.Runnable<SQLException> sqlCmd) throws UncheckedSQLException {
        try {
            sqlCmd.run();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     *
     * @param <R>
     * @param sqlCmd
     * @return
     * @throws UncheckedSQLException
     */
    @Beta
    static <R> R call(Try.Callable<R, SQLException> sqlCmd) throws UncheckedSQLException {
        try {
            return sqlCmd.call();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * The backed {@code PreparedStatement/CallableStatement} will be closed by default
     * after any execution methods(which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/...).
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * Generally, don't cache or reuse the instance of this class,
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * Remember: parameter/column index in {@code PreparedStatement/ResultSet} starts from 1, not 0.
     *
     * @author haiyangl
     * @param <S>
     * @param <Q>
     */
    static abstract class AbstractPreparedQuery<S extends PreparedStatement, Q extends AbstractPreparedQuery<S, Q>> implements AutoCloseable {

        /** The stmt. */
        final S stmt;

        /** The conn. */
        Connection conn;

        /** The is fetch direction set. */
        boolean isFetchDirectionSet = false;

        /** The is batch. */
        boolean isBatch = false;

        /** The close after execution. */
        boolean isCloseAfterExecution = true;

        /** The is closed. */
        boolean isClosed = false;

        /** The close handler. */
        Runnable closeHandler;

        /**
         * Instantiates a new abstract prepared query.
         *
         * @param stmt
         */
        AbstractPreparedQuery(S stmt) {
            this.stmt = stmt;
        }

        //        /**
        //         * It's designed to void try-catch.
        //         * This method should be called immediately after {@code JdbcUtil#prepareCallableQuery/SQLExecutor#prepareQuery}.
        //         *
        //         * @return
        //         */
        //        public Try<Q> tried() {
        //            assertNotClosed();
        //
        //            return Try.of((Q) this);
        //        }

        /**
         *
         * @param closeAfterExecution default is {@code true}.
         * @return
         */
        public Q closeAfterExecution(boolean closeAfterExecution) {
            assertNotClosed();

            this.isCloseAfterExecution = closeAfterExecution;

            return (Q) this;
        }

        /**
         *
         * @return
         */
        boolean isCloseAfterExecution() {
            return isCloseAfterExecution;
        }

        /**
         *
         * @param closeHandler A task to execute after this {@code Query} is closed
         * @return
         */
        public Q onClose(final Runnable closeHandler) {
            checkArgNotNull(closeHandler, "closeHandler");
            assertNotClosed();

            if (this.closeHandler == null) {
                this.closeHandler = closeHandler;
            } else {
                final Runnable tmp = this.closeHandler;

                this.closeHandler = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tmp.run();
                        } finally {
                            closeHandler.run();
                        }
                    }
                };
            }

            return (Q) this;
        }

        /**
         * Sets the null.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNull(int parameterIndex, int sqlType) throws SQLException {
            stmt.setNull(parameterIndex, sqlType);

            return (Q) this;
        }

        /**
         * Sets the null.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
            stmt.setNull(parameterIndex, sqlType, typeName);

            return (Q) this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBoolean(int parameterIndex, boolean x) throws SQLException {
            stmt.setBoolean(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBoolean(int parameterIndex, Boolean x) throws SQLException {
            stmt.setBoolean(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setByte(int parameterIndex, byte x) throws SQLException {
            stmt.setByte(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setByte(int parameterIndex, Byte x) throws SQLException {
            stmt.setByte(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the short.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setShort(int parameterIndex, short x) throws SQLException {
            stmt.setShort(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the short.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setShort(int parameterIndex, Short x) throws SQLException {
            stmt.setShort(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the int.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setInt(int parameterIndex, int x) throws SQLException {
            stmt.setInt(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the int.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setInt(int parameterIndex, Integer x) throws SQLException {
            stmt.setInt(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the long.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setLong(int parameterIndex, long x) throws SQLException {
            stmt.setLong(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the long.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setLong(int parameterIndex, Long x) throws SQLException {
            stmt.setLong(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the float.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setFloat(int parameterIndex, float x) throws SQLException {
            stmt.setFloat(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the float.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setFloat(int parameterIndex, Float x) throws SQLException {
            stmt.setFloat(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the double.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setDouble(int parameterIndex, double x) throws SQLException {
            stmt.setDouble(parameterIndex, N.defaultIfNull(x));

            return (Q) this;
        }

        /**
         * Sets the double.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setDouble(int parameterIndex, Double x) throws SQLException {
            stmt.setDouble(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the big decimal.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
            stmt.setBigDecimal(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the string.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setString(int parameterIndex, String x) throws SQLException {
            stmt.setString(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the date.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setDate(int parameterIndex, java.sql.Date x) throws SQLException {
            stmt.setDate(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the date.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setDate(int parameterIndex, java.util.Date x) throws SQLException {
            stmt.setDate(parameterIndex, x == null ? null : x instanceof java.sql.Date ? (java.sql.Date) x : new java.sql.Date(x.getTime()));

            return (Q) this;
        }

        /**
         * Sets the time.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setTime(int parameterIndex, java.sql.Time x) throws SQLException {
            stmt.setTime(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the time.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setTime(int parameterIndex, java.util.Date x) throws SQLException {
            stmt.setTime(parameterIndex, x == null ? null : x instanceof java.sql.Time ? (java.sql.Time) x : new java.sql.Time(x.getTime()));

            return (Q) this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException {
            stmt.setTimestamp(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setTimestamp(int parameterIndex, java.util.Date x) throws SQLException {
            stmt.setTimestamp(parameterIndex,
                    x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));

            return (Q) this;
        }

        /**
         * Sets the bytes.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBytes(int parameterIndex, byte[] x) throws SQLException {
            stmt.setBytes(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterIndex
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setAsciiStream(int parameterIndex, InputStream inputStream) throws SQLException {
            stmt.setAsciiStream(parameterIndex, inputStream);

            return (Q) this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterIndex
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setAsciiStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
            stmt.setAsciiStream(parameterIndex, inputStream, length);

            return (Q) this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterIndex
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBinaryStream(int parameterIndex, InputStream inputStream) throws SQLException {
            stmt.setBinaryStream(parameterIndex, inputStream);

            return (Q) this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterIndex
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBinaryStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
            stmt.setBinaryStream(parameterIndex, inputStream, length);

            return (Q) this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterIndex
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
            stmt.setCharacterStream(parameterIndex, reader);

            return (Q) this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterIndex
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
            stmt.setCharacterStream(parameterIndex, reader, length);

            return (Q) this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterIndex
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNCharacterStream(int parameterIndex, Reader reader) throws SQLException {
            stmt.setNCharacterStream(parameterIndex, reader);

            return (Q) this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterIndex
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
            stmt.setNCharacterStream(parameterIndex, reader, length);

            return (Q) this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBlob(int parameterIndex, java.sql.Blob x) throws SQLException {
            stmt.setBlob(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterIndex
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
            stmt.setBlob(parameterIndex, inputStream);

            return (Q) this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterIndex
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
            stmt.setBlob(parameterIndex, inputStream, length);

            return (Q) this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setClob(int parameterIndex, java.sql.Clob x) throws SQLException {
            stmt.setClob(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterIndex
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setClob(int parameterIndex, Reader reader) throws SQLException {
            stmt.setClob(parameterIndex, reader);

            return (Q) this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterIndex
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setClob(int parameterIndex, Reader reader, long length) throws SQLException {
            stmt.setClob(parameterIndex, reader, length);

            return (Q) this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNClob(int parameterIndex, java.sql.NClob x) throws SQLException {
            stmt.setNClob(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterIndex
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNClob(int parameterIndex, Reader reader) throws SQLException {
            stmt.setNClob(parameterIndex, reader);

            return (Q) this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterIndex
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
            stmt.setNClob(parameterIndex, reader, length);

            return (Q) this;
        }

        /**
         * Sets the URL.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setURL(int parameterIndex, URL x) throws SQLException {
            stmt.setURL(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the array.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setArray(int parameterIndex, java.sql.Array x) throws SQLException {
            stmt.setArray(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the SQLXML.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setSQLXML(int parameterIndex, java.sql.SQLXML x) throws SQLException {
            stmt.setSQLXML(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the ref.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setRef(int parameterIndex, java.sql.Ref x) throws SQLException {
            stmt.setRef(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the row id.
         *
         * @param parameterIndex
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setRowId(int parameterIndex, java.sql.RowId x) throws SQLException {
            stmt.setRowId(parameterIndex, x);

            return (Q) this;
        }

        /**
         * Sets the object.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setObject(int parameterIndex, Object x) throws SQLException {
            if (x == null) {
                stmt.setObject(parameterIndex, x);
            } else {
                N.typeOf(x.getClass()).set(stmt, parameterIndex, x);
            }

            return (Q) this;
        }

        /**
         * Sets the object.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setObject(int parameterIndex, Object x, int sqlType) throws SQLException {
            stmt.setObject(parameterIndex, x, sqlType);

            return (Q) this;
        }

        /**
         * Sets the object.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param x
         * @param sqlType
         * @param scaleOrLength
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setObject(int parameterIndex, Object x, int sqlType, int scaleOrLength) throws SQLException {
            stmt.setObject(parameterIndex, x, sqlType, scaleOrLength);

            return (Q) this;
        }

        /**
         * Sets the object.
         *
         * @param parameterIndex
         * @param x
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setObject(int parameterIndex, Object x, SQLType sqlType) throws SQLException {
            stmt.setObject(parameterIndex, x, sqlType);

            return (Q) this;
        }

        /**
         * Sets the object.
         *
         * @param parameterIndex
         * @param x
         * @param sqlType
         * @param scaleOrLength
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setObject(int parameterIndex, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
            stmt.setObject(parameterIndex, x, sqlType, scaleOrLength);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, String param1, String param2) throws SQLException {
            stmt.setString(startParameterIndex++, param1);
            stmt.setString(startParameterIndex++, param2);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, String param1, String param2, String param3) throws SQLException {
            stmt.setString(startParameterIndex++, param1);
            stmt.setString(startParameterIndex++, param2);
            stmt.setString(startParameterIndex++, param3);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, String param1, String param2, String param3, String param4) throws SQLException {
            stmt.setString(startParameterIndex++, param1);
            stmt.setString(startParameterIndex++, param2);
            stmt.setString(startParameterIndex++, param3);
            stmt.setString(startParameterIndex++, param4);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, String param1, String param2, String param3, String param4, String param5) throws SQLException {
            stmt.setString(startParameterIndex++, param1);
            stmt.setString(startParameterIndex++, param2);
            stmt.setString(startParameterIndex++, param3);
            stmt.setString(startParameterIndex++, param4);
            stmt.setString(startParameterIndex++, param5);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3) throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4) throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4, Object param5) throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);
            setObject(startParameterIndex++, param5);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @param param6
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6)
                throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);
            setObject(startParameterIndex++, param5);
            setObject(startParameterIndex++, param6);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @param param6
         * @param param7
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6, Object param7)
                throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);
            setObject(startParameterIndex++, param5);
            setObject(startParameterIndex++, param6);
            setObject(startParameterIndex++, param7);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @param param6
         * @param param7
         * @param param8
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6, Object param7,
                Object param8) throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);
            setObject(startParameterIndex++, param5);
            setObject(startParameterIndex++, param6);
            setObject(startParameterIndex++, param7);
            setObject(startParameterIndex++, param8);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @param param5
         * @param param6
         * @param param7
         * @param param8
         * @param param9
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6, Object param7,
                Object param8, Object param9) throws SQLException {
            setObject(startParameterIndex++, param1);
            setObject(startParameterIndex++, param2);
            setObject(startParameterIndex++, param3);
            setObject(startParameterIndex++, param4);
            setObject(startParameterIndex++, param5);
            setObject(startParameterIndex++, param6);
            setObject(startParameterIndex++, param7);
            setObject(startParameterIndex++, param8);
            setObject(startParameterIndex++, param9);

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param startParameterIndex
         * @param parameters
         * @return
         * @throws IllegalArgumentException if specified {@code parameters} or {@code type} is null.
         * @throws SQLException the SQL exception
         */
        public Q setParameters(int startParameterIndex, Collection<?> parameters) throws IllegalArgumentException, SQLException {
            checkArgNotNull(parameters, "parameters");

            for (Object param : parameters) {
                setObject(startParameterIndex++, param);
            }

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param <T>
         * @param startParameterIndex
         * @param parameters
         * @param type
         * @return
         * @throws IllegalArgumentException if specified {@code parameters} or {@code type} is null.
         * @throws SQLException the SQL exception
         */
        public <T> Q setParameters(int startParameterIndex, Collection<? extends T> parameters, Class<T> type) throws IllegalArgumentException, SQLException {
            checkArgNotNull(parameters, "parameters");
            checkArgNotNull(type, "type");

            final Type<T> setter = N.typeOf(type);

            for (T param : parameters) {
                setter.set(stmt, startParameterIndex++, param);
            }

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param paramSetter
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setParameters(ParametersSetter<? super S> paramSetter) throws SQLException {
            checkArgNotNull(paramSetter, "paramSetter");

            boolean noException = false;

            try {
                paramSetter.accept(stmt);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return (Q) this;
        }

        /**
         * Sets the parameters.
         *
         * @param <T>
         * @param parameter
         * @param paramSetter
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Q setParameters(final T parameter, final BiParametersSetter<? super S, ? super T> paramSetter) throws SQLException {
            checkArgNotNull(paramSetter, "paramSetter");

            boolean noException = false;

            try {
                paramSetter.accept(stmt, parameter);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return (Q) this;
        }

        /**
         *
         * @param paramSetter
         * @return
         * @throws SQLException the SQL exception
         */
        public Q settParameters(ParametersSetter<? super Q> paramSetter) throws SQLException {
            checkArgNotNull(paramSetter, "paramSetter");

            boolean noException = false;

            try {
                paramSetter.accept((Q) this);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return (Q) this;
        }

        /**
         *
         * @param <T>
         * @param parameter
         * @param paramSetter
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Q settParameters(final T parameter, BiParametersSetter<? super Q, ? super T> paramSetter) throws SQLException {
            checkArgNotNull(paramSetter, "paramSetter");

            boolean noException = false;

            try {
                paramSetter.accept((Q) this, parameter);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return (Q) this;
        }

        //    /**
        //     * @param <T>
        //     * @param batchParameters
        //     * @param parametersSetter
        //     * @return
        //     * @throws SQLException the SQL exception
        //     */
        //    <T> Q setBatchParameters(final Collection<T> batchParameters, BiParametersSetter<? super Q, ? super T> parametersSetter) throws SQLException {
        //        return setBatchParameters(batchParameters.iterator(), parametersSetter);
        //    }
        //
        //    /**
        //     *
        //     * @param <T>
        //     * @param batchParameters
        //     * @param parametersSetter
        //     * @return
        //     * @throws SQLException the SQL exception
        //     */
        //    <T> Q setBatchParameters(final Iterator<T> batchParameters, BiParametersSetter<? super Q, ? super T> parametersSetter) throws SQLException {
        //        checkArgNotNull(batchParameters, "batchParameters");
        //        checkArgNotNull(parametersSetter, "parametersSetter");
        //
        //        boolean noException = false;
        //
        //        try {
        //            if (isBatch) {
        //                stmt.clearBatch();
        //            }
        //
        //            final Iterator<T> iter = batchParameters;
        //
        //            while (iter.hasNext()) {
        //                parametersSetter.accept((Q) this, iter.next());
        //                stmt.addBatch();
        //                isBatch = true;
        //            }
        //
        //            noException = true;
        //        } finally {
        //            if (noException == false) {
        //                close();
        //            }
        //        }
        //
        //        return (Q) this;
        //    }

        /**
         * @param <T>
         * @param batchParameters
         * @param parametersSetter
         * @return
         * @throws SQLException the SQL exception
         */
        @Beta
        public <T> Q addBatchParameters(final Collection<T> batchParameters, BiParametersSetter<? super Q, ? super T> parametersSetter) throws SQLException {
            return addBatchParameters(batchParameters.iterator(), parametersSetter);
        }

        /**
         *
         * @param <T>
         * @param batchParameters
         * @param parametersSetter
         * @return
         * @throws SQLException the SQL exception
         */
        @Beta
        public <T> Q addBatchParameters(final Iterator<T> batchParameters, BiParametersSetter<? super Q, ? super T> parametersSetter) throws SQLException {
            checkArgNotNull(batchParameters, "batchParameters");
            checkArgNotNull(parametersSetter, "parametersSetter");

            boolean noException = false;

            try {
                final Iterator<T> iter = batchParameters;

                while (iter.hasNext()) {
                    parametersSetter.accept((Q) this, iter.next());
                    stmt.addBatch();
                    isBatch = true;
                }

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return (Q) this;
        }

        /**
         * Adds the batch.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Q addBatch() throws SQLException {
            stmt.addBatch();
            isBatch = true;

            return (Q) this;
        }

        /**
         * Sets the fetch direction.
         *
         * @param direction one of <code>ResultSet.FETCH_FORWARD</code>,
         * <code>ResultSet.FETCH_REVERSE</code>, or <code>ResultSet.FETCH_UNKNOWN</code>
         * @return
         * @throws SQLException the SQL exception
         * @see {@link java.sql.Statement#setFetchDirection(int)}
         */
        public Q setFetchDirection(FetchDirection direction) throws SQLException {
            isFetchDirectionSet = true;

            stmt.setFetchDirection(direction.intValue);

            return (Q) this;
        }

        /**
         * Sets the fetch size.
         *
         * @param rows
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setFetchSize(int rows) throws SQLException {
            stmt.setFetchSize(rows);

            return (Q) this;
        }

        /**
         * Sets the max rows.
         *
         * @param max
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setMaxRows(int max) throws SQLException {
            stmt.setMaxRows(max);

            return (Q) this;
        }

        /**
         * Sets the large max rows.
         *
         * @param max
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setLargeMaxRows(long max) throws SQLException {
            stmt.setLargeMaxRows(max);

            return (Q) this;
        }

        /**
         * Sets the max field size.
         *
         * @param max
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setMaxFieldSize(int max) throws SQLException {
            stmt.setMaxFieldSize(max);

            return (Q) this;
        }

        /**
         * Sets the query timeout.
         *
         * @param seconds
         * @return
         * @throws SQLException the SQL exception
         */
        public Q setQueryTimeout(int seconds) throws SQLException {
            stmt.setQueryTimeout(seconds);

            return (Q) this;
        }

        /**
         * Query for boolean.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalBoolean queryForBoolean() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalBoolean.of(rs.getBoolean(1)) : OptionalBoolean.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /** The Constant charType. */
        private static final Type<Character> charType = TypeFactory.getType(char.class);

        /**
         * Query for char.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalChar queryForChar() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    return OptionalChar.of(charType.get(rs, 1));
                } else {
                    return OptionalChar.empty();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for byte.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalByte queryForByte() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalByte.of(rs.getByte(1)) : OptionalByte.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for short.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalShort queryForShort() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalShort.of(rs.getShort(1)) : OptionalShort.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for int.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalInt queryForInt() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalInt.of(rs.getInt(1)) : OptionalInt.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for long.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalLong queryForLong() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for float.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalFloat queryForFloat() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalFloat.of(rs.getFloat(1)) : OptionalFloat.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for double.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public OptionalDouble queryForDouble() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? OptionalDouble.of(rs.getDouble(1)) : OptionalDouble.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for string.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Nullable<String> queryForString() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(rs.getString(1)) : Nullable.<String> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query big decimal.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Nullable<BigDecimal> queryBigDecimal() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(rs.getBigDecimal(1)) : Nullable.<BigDecimal> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for date.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Nullable<java.sql.Date> queryForDate() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(rs.getDate(1)) : Nullable.<java.sql.Date> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for time.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Nullable<java.sql.Time> queryForTime() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(rs.getTime(1)) : Nullable.<java.sql.Time> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Query for timestamp.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public Nullable<java.sql.Timestamp> queryForTimestamp() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(rs.getTimestamp(1)) : Nullable.<java.sql.Timestamp> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Returns a {@code Nullable} describing the value in the first row/column if it exists, otherwise return an empty {@code Nullable}.
         *
         * @param <V> the value type
         * @param targetClass
         * @return
         * @throws SQLException the SQL exception
         */
        public <V> Nullable<V> queryForSingleResult(Class<V> targetClass) throws SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Nullable.of(N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)) : Nullable.<V> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Returns an {@code Optional} describing the value in the first row/column if it exists, otherwise return an empty {@code Optional}.
         *
         * @param <V> the value type
         * @param targetClass
         * @return
         * @throws SQLException the SQL exception
         */
        public <V> Optional<V> queryForSingleNonNull(Class<V> targetClass) throws SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Optional.of(N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)) : Optional.<V> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Returns a {@code Nullable} describing the value in the first row/column if it exists, otherwise return an empty {@code Nullable}.
         * And throws {@code DuplicatedResultException} if more than one record found.
         *
         * @param <V> the value type
         * @param targetClass
         * @return
         * @throws DuplicatedResultException if more than one record found.
         * @throws SQLException the SQL exception
         */
        public <V> Nullable<V> queryForUniqueResult(Class<V> targetClass) throws DuplicatedResultException, SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final Nullable<V> result = rs.next() ? Nullable.of(N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)) : Nullable.<V> empty();

                if (result.isPresent() && rs.next()) {
                    throw new DuplicatedResultException(
                            "At least two results found: " + Strings.concat(result.get(), ", ", N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)));
                }

                return result;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Returns an {@code Optional} describing the value in the first row/column if it exists, otherwise return an empty {@code Optional}.
         * And throws {@code DuplicatedResultException} if more than one record found.
         *
         * @param <V> the value type
         * @param targetClass
         * @return
         * @throws DuplicatedResultException if more than one record found.
         * @throws SQLException the SQL exception
         */
        public <V> Optional<V> queryForUniqueNonNull(Class<V> targetClass) throws DuplicatedResultException, SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final Optional<V> result = rs.next() ? Optional.of(N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)) : Optional.<V> empty();

                if (result.isPresent() && rs.next()) {
                    throw new DuplicatedResultException(
                            "At least two results found: " + Strings.concat(result.get(), ", ", N.convert(JdbcUtil.getColumnValue(rs, 1), targetClass)));
                }

                return result;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param rs
         * @return
         * @throws SQLException the SQL exception
         */
        private <T> T get(Class<T> targetClass, ResultSet rs) throws SQLException {
            final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

            return BiRowMapper.to(targetClass).apply(rs, columnLabels);
        }

        /**
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public DataSet query() throws SQLException {
            return query(ResultExtractor.TO_DATA_SET);
        }

        /**
         *
         * @param <R>
         * @param resultExtrator
         * @return
         * @throws SQLException the SQL exception
         */
        public <R> R query(final ResultExtractor<R> resultExtrator) throws SQLException {
            checkArgNotNull(resultExtrator, "resultExtrator");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return resultExtrator.apply(rs);
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <R>
         * @param resultExtrator
         * @return
         * @throws SQLException the SQL exception
         */
        public <R> R query(final BiResultExtractor<R> resultExtrator) throws SQLException {
            checkArgNotNull(resultExtrator, "resultExtrator");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return resultExtrator.apply(rs, getColumnLabelList(rs));
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> get(final Class<T> targetClass) throws DuplicatedResultException, SQLException {
            return Optional.ofNullable(gett(targetClass));
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> get(RowMapper<T> rowMapper) throws DuplicatedResultException, SQLException {
            return Optional.ofNullable(gett(rowMapper));
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> get(BiRowMapper<T> rowMapper) throws DuplicatedResultException, SQLException {
            return Optional.ofNullable(gett(rowMapper));
        }

        /**
         * Gets the t.
         *
         * @param <T>
         * @param targetClass
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> T gett(final Class<T> targetClass) throws DuplicatedResultException, SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    final T result = Objects.requireNonNull(get(targetClass, rs));

                    if (rs.next()) {
                        throw new DuplicatedResultException("There are more than one record found by the query");
                    }

                    return result;
                } else {
                    return null;
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Gets the t.
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> T gett(RowMapper<T> rowMapper) throws DuplicatedResultException, SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    final T result = Objects.requireNonNull(rowMapper.apply(rs));

                    if (rs.next()) {
                        throw new DuplicatedResultException("There are more than one record found by the query");
                    }

                    return result;
                } else {
                    return null;
                }

            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Gets the t.
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws DuplicatedResultException If there are more than one record found by the query
         * @throws SQLException the SQL exception
         */
        public <T> T gett(BiRowMapper<T> rowMapper) throws DuplicatedResultException, SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    final T result = Objects.requireNonNull(rowMapper.apply(rs, JdbcUtil.getColumnLabelList(rs)));

                    if (rs.next()) {
                        throw new DuplicatedResultException("There are more than one record found by the query");
                    }

                    return result;
                } else {
                    return null;
                }

            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> findFirst(final Class<T> targetClass) throws SQLException {
            checkArgNotNull(targetClass, "targetClass");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    return Optional.of(get(targetClass, rs));
                } else {
                    return Optional.empty();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> findFirst(RowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Optional.of(rowMapper.apply(rs)) : Optional.<T> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> findFirst(final RowFilter recordFilter, RowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                while (rs.next()) {
                    if (recordFilter.test(rs)) {
                        return Optional.of(rowMapper.apply(rs));
                    }
                }

                return Optional.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> findFirst(BiRowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next() ? Optional.of(rowMapper.apply(rs, JdbcUtil.getColumnLabelList(rs))) : Optional.<T> empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> Optional<T> findFirst(final BiRowFilter recordFilter, BiRowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                while (rs.next()) {
                    if (recordFilter.test(rs, columnLabels)) {
                        return Optional.of(rowMapper.apply(rs, columnLabels));
                    }
                }

                return Optional.empty();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final Class<T> targetClass) throws SQLException {
            return list(BiRowMapper.to(targetClass));
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param maxResult
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final Class<T> targetClass, int maxResult) throws SQLException {
            return list(BiRowMapper.to(targetClass), maxResult);
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(RowMapper<T> rowMapper) throws SQLException {
            return list(rowMapper, Integer.MAX_VALUE);
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @param maxResult
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(RowMapper<T> rowMapper, int maxResult) throws SQLException {
            return list(RowFilter.ALWAYS_TRUE, rowMapper, maxResult);
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final RowFilter recordFilter, RowMapper<T> rowMapper) throws SQLException {
            return list(recordFilter, rowMapper, Integer.MAX_VALUE);
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @param maxResult
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final RowFilter recordFilter, RowMapper<T> rowMapper, int maxResult) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowMapper, "rowMapper");
            checkArg(maxResult >= 0, "'maxResult' can' be negative: " + maxResult);
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<T> result = new ArrayList<>();

                while (maxResult > 0 && rs.next()) {
                    if (recordFilter.test(rs)) {
                        result.add(rowMapper.apply(rs));
                        maxResult--;
                    }
                }

                return result;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(BiRowMapper<T> rowMapper) throws SQLException {
            return list(rowMapper, Integer.MAX_VALUE);
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @param maxResult
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(BiRowMapper<T> rowMapper, int maxResult) throws SQLException {
            return list(BiRowFilter.ALWAYS_TRUE, rowMapper, maxResult);
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final BiRowFilter recordFilter, BiRowMapper<T> rowMapper) throws SQLException {
            return list(recordFilter, rowMapper, Integer.MAX_VALUE);
        }

        /**
         *
         * @param <T>
         * @param recordFilter
         * @param rowMapper
         * @param maxResult
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> List<T> list(final BiRowFilter recordFilter, BiRowMapper<T> rowMapper, int maxResult) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowMapper, "rowMapper");
            checkArg(maxResult >= 0, "'maxResult' can' be negative: " + maxResult);
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);
                final List<T> result = new ArrayList<>();

                while (maxResult > 0 && rs.next()) {
                    if (recordFilter.test(rs, columnLabels)) {
                        result.add(rowMapper.apply(rs, columnLabels));
                        maxResult--;
                    }
                }

                return result;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> ExceptionalStream<T, SQLException> stream(final Class<T> targetClass) throws SQLException {
            return stream(BiRowMapper.to(targetClass));
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> ExceptionalStream<T, SQLException> stream(final RowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            final ExceptionalIterator<T, SQLException> lazyIter = ExceptionalIterator
                    .of(new Try.Supplier<ExceptionalIterator<T, SQLException>, SQLException>() {
                        private ExceptionalIterator<T, SQLException> internalIter;

                        @Override
                        public ExceptionalIterator<T, SQLException> get() throws SQLException {
                            if (internalIter == null) {
                                ResultSet rs = null;

                                try {
                                    rs = executeQuery();
                                    final ResultSet resultSet = rs;

                                    internalIter = new ExceptionalIterator<T, SQLException>() {
                                        private boolean hasNext;

                                        @Override
                                        public boolean hasNext() throws SQLException {
                                            if (hasNext == false) {
                                                hasNext = resultSet.next();
                                            }

                                            return hasNext;
                                        }

                                        @Override
                                        public T next() throws SQLException {
                                            if (hasNext() == false) {
                                                throw new NoSuchElementException();
                                            }

                                            hasNext = false;

                                            return rowMapper.apply(resultSet);
                                        }

                                        @Override
                                        public void skip(long n) throws SQLException {
                                            N.checkArgNotNegative(n, "n");

                                            final long m = hasNext ? n - 1 : n;
                                            hasNext = false;

                                            JdbcUtil.skip(resultSet, m);
                                        }

                                        @Override
                                        public long count() throws SQLException {
                                            long cnt = hasNext ? 1 : 0;
                                            hasNext = false;

                                            while (resultSet.next()) {
                                                cnt++;
                                            }

                                            return cnt;
                                        }

                                        @Override
                                        public void close() throws SQLException {
                                            try {
                                                JdbcUtil.closeQuietly(resultSet);
                                            } finally {
                                                closeAfterExecutionIfAllowed();
                                            }
                                        }
                                    };
                                } finally {
                                    if (internalIter == null) {
                                        try {
                                            JdbcUtil.closeQuietly(rs);
                                        } finally {
                                            closeAfterExecutionIfAllowed();
                                        }
                                    }
                                }
                            }

                            return internalIter;
                        }
                    });

            return ExceptionalStream.newStream(lazyIter).onClose(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    lazyIter.close();
                }
            });
        }

        /**
         *
         * @param <T>
         * @param rowMapper
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> ExceptionalStream<T, SQLException> stream(final BiRowMapper<T> rowMapper) throws SQLException {
            checkArgNotNull(rowMapper, "rowMapper");
            assertNotClosed();

            final ExceptionalIterator<T, SQLException> lazyIter = ExceptionalIterator
                    .of(new Try.Supplier<ExceptionalIterator<T, SQLException>, SQLException>() {
                        private ExceptionalIterator<T, SQLException> internalIter;

                        @Override
                        public ExceptionalIterator<T, SQLException> get() throws SQLException {
                            if (internalIter == null) {
                                ResultSet rs = null;

                                try {
                                    rs = executeQuery();
                                    final ResultSet resultSet = rs;

                                    internalIter = new ExceptionalIterator<T, SQLException>() {
                                        private List<String> columnLabels = null;
                                        private boolean hasNext;

                                        @Override
                                        public boolean hasNext() throws SQLException {
                                            if (hasNext == false) {
                                                hasNext = resultSet.next();
                                            }

                                            return hasNext;
                                        }

                                        @Override
                                        public T next() throws SQLException {
                                            if (hasNext() == false) {
                                                throw new NoSuchElementException();
                                            }

                                            hasNext = false;

                                            if (columnLabels == null) {
                                                columnLabels = JdbcUtil.getColumnLabelList(resultSet);
                                            }

                                            return rowMapper.apply(resultSet, columnLabels);
                                        }

                                        @Override
                                        public void skip(long n) throws SQLException {
                                            N.checkArgNotNegative(n, "n");

                                            final long m = hasNext ? n - 1 : n;
                                            hasNext = false;

                                            JdbcUtil.skip(resultSet, m);
                                        }

                                        @Override
                                        public long count() throws SQLException {
                                            long cnt = hasNext ? 1 : 0;
                                            hasNext = false;

                                            while (resultSet.next()) {
                                                cnt++;
                                            }

                                            return cnt;
                                        }

                                        @Override
                                        public void close() throws SQLException {
                                            try {
                                                JdbcUtil.closeQuietly(resultSet);
                                            } finally {
                                                closeAfterExecutionIfAllowed();
                                            }
                                        }
                                    };
                                } finally {
                                    if (internalIter == null) {
                                        try {
                                            JdbcUtil.closeQuietly(rs);
                                        } finally {
                                            closeAfterExecutionIfAllowed();
                                        }
                                    }
                                }
                            }

                            return internalIter;
                        }
                    });

            return ExceptionalStream.newStream(lazyIter).onClose(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    lazyIter.close();
                }
            });
        }

        /**
         * Note: using {@code select 1 from ...}, not {@code select count(*) from ...}.
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean exists() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                return rs.next();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void ifExists(final RowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    rowConsumer.accept(rs);
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void ifExists(final BiRowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    rowConsumer.accept(rs, JdbcUtil.getColumnLabelList(rs));
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * If exists or else.
         *
         * @param rowConsumer
         * @param orElseAction
         * @throws SQLException the SQL exception
         */
        public void ifExistsOrElse(final RowConsumer rowConsumer, Try.Runnable<SQLException> orElseAction) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            checkArgNotNull(orElseAction, "orElseAction");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    rowConsumer.accept(rs);
                } else {
                    orElseAction.run();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * If exists or else.
         *
         * @param rowConsumer
         * @param orElseAction
         * @throws SQLException the SQL exception
         */
        public void ifExistsOrElse(final BiRowConsumer rowConsumer, Try.Runnable<SQLException> orElseAction) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            checkArgNotNull(orElseAction, "orElseAction");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                if (rs.next()) {
                    rowConsumer.accept(rs, JdbcUtil.getColumnLabelList(rs));
                } else {
                    orElseAction.run();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Note: using {@code select count(*) from ...}
         *
         * @return
         * @throws SQLException the SQL exception
         * @deprecated may be misused and it's inefficient.
         */
        @Deprecated
        public int count() throws SQLException {
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                int cnt = 0;

                while (rs.next()) {
                    cnt++;
                }

                return cnt;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return
         * @throws SQLException the SQL exception
         */
        public int count(final RowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                int cnt = 0;

                while (rs.next()) {
                    if (recordFilter.test(rs)) {
                        cnt++;
                    }
                }

                return cnt;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return
         * @throws SQLException the SQL exception
         */
        public int count(final BiRowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);
                int cnt = 0;

                while (rs.next()) {
                    if (recordFilter.test(rs, columnLabels)) {
                        cnt++;
                    }
                }

                return cnt;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean anyMatch(final RowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                while (rs.next()) {
                    if (recordFilter.test(rs)) {
                        return true;
                    }
                }

                return false;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean anyMatch(final BiRowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                while (rs.next()) {
                    if (recordFilter.test(rs, columnLabels)) {
                        return true;
                    }
                }

                return false;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean allMatch(final RowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                while (rs.next()) {
                    if (recordFilter.test(rs) == false) {
                        return false;
                    }
                }

                return true;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean allMatch(final BiRowFilter recordFilter) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                while (rs.next()) {
                    if (recordFilter.test(rs, columnLabels) == false) {
                        return false;
                    }
                }

                return true;
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean noneMatch(final RowFilter recordFilter) throws SQLException {
            return anyMatch(recordFilter) == false;
        }

        /**
         *
         * @param recordFilter
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean noneMatch(final BiRowFilter recordFilter) throws SQLException {
            return anyMatch(recordFilter) == false;
        }

        /**
         *
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void forEach(final RowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {

                while (rs.next()) {
                    rowConsumer.accept(rs);
                }

            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void forEach(final RowFilter recordFilter, final RowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {

                while (rs.next()) {
                    if (recordFilter.test(rs)) {
                        rowConsumer.accept(rs);
                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void forEach(final BiRowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                while (rs.next()) {
                    rowConsumer.accept(rs, columnLabels);
                }

            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param recordFilter
         * @param rowConsumer
         * @throws SQLException the SQL exception
         */
        public void forEach(final BiRowFilter recordFilter, final BiRowConsumer rowConsumer) throws SQLException {
            checkArgNotNull(recordFilter, "recordFilter");
            checkArgNotNull(rowConsumer, "rowConsumer");
            assertNotClosed();

            try (ResultSet rs = executeQuery()) {
                final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                while (rs.next()) {
                    if (recordFilter.test(rs, columnLabels)) {
                        rowConsumer.accept(rs, columnLabels);
                    }
                }

            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @return
         * @throws SQLException the SQL exception
         */
        private ResultSet executeQuery() throws SQLException {
            if (!isFetchDirectionSet) {
                stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
            }

            return stmt.executeQuery();
        }

        /**
         * Returns the generated key if it exists.
         *
         * @param <ID>
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> Optional<ID> insert() throws SQLException {
            assertNotClosed();

            try {
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    return rs.next() ? Optional.of((ID) JdbcUtil.getColumnValue(rs, 1)) : Optional.<ID> empty();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <ID>
         * @param autoGeneratedKeyExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> Optional<ID> insert(final RowMapper<ID> autoGeneratedKeyExtractor) throws SQLException {
            assertNotClosed();

            try {
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    return rs.next() ? Optional.of(autoGeneratedKeyExtractor.apply(rs)) : Optional.<ID> empty();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <ID>
         * @param autoGeneratedKeyExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> Optional<ID> insert(final BiRowMapper<ID> autoGeneratedKeyExtractor) throws SQLException {
            assertNotClosed();

            try {
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                        return Optional.of(autoGeneratedKeyExtractor.apply(rs, columnLabels));
                    } else {
                        return Optional.<ID> empty();
                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Returns the generated key if it exists.
         *
         * @param <ID>
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> List<ID> batchInsert() throws SQLException {
            assertNotClosed();

            try {
                stmt.executeBatch();

                final List<ID> result = new ArrayList<>();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    while (rs.next()) {
                        result.add((ID) JdbcUtil.getColumnValue(rs, 1));
                    }

                    return result;
                } finally {
                    stmt.clearBatch();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <ID>
         * @param autoGeneratedKeyExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> List<ID> batchInsert(final RowMapper<ID> autoGeneratedKeyExtractor) throws SQLException {
            assertNotClosed();

            try {
                stmt.executeBatch();

                final List<ID> result = new ArrayList<>();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    while (rs.next()) {
                        result.add(autoGeneratedKeyExtractor.apply(rs));
                    }

                    return result;
                } finally {
                    stmt.clearBatch();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <ID>
         * @param autoGeneratedKeyExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        public <ID> List<ID> batchInsert(final BiRowMapper<ID> autoGeneratedKeyExtractor) throws SQLException {
            assertNotClosed();

            try {
                stmt.executeBatch();

                final List<ID> result = new ArrayList<>();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    final List<String> columnLabels = JdbcUtil.getColumnLabelList(rs);

                    while (rs.next()) {
                        result.add(autoGeneratedKeyExtractor.apply(rs, columnLabels));
                    }

                    return result;
                } finally {
                    stmt.clearBatch();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public int update() throws SQLException {
            assertNotClosed();

            try {
                return stmt.executeUpdate();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public int[] batchUpdate() throws SQLException {
            assertNotClosed();

            try {
                try {
                    return stmt.executeBatch();
                } finally {
                    stmt.clearBatch();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public long largeUpate() throws SQLException {
            assertNotClosed();

            try {
                return stmt.executeLargeUpdate();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Large batch update.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        public long[] largeBatchUpdate() throws SQLException {
            assertNotClosed();

            try {
                try {
                    return stmt.executeLargeBatch();
                } finally {
                    stmt.clearBatch();
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        public boolean execute() throws SQLException {
            assertNotClosed();

            try {
                return stmt.execute();
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Execute then apply.
         *
         * @param <R>
         * @param getter
         * @return
         * @throws SQLException the SQL exception
         */
        public <R> R executeThenApply(final Try.Function<? super S, ? extends R, SQLException> getter) throws SQLException {
            checkArgNotNull(getter, "getter");
            assertNotClosed();

            try {
                stmt.execute();

                return getter.apply(stmt);
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Execute then apply.
         *
         * @param <R>
         * @param getter
         * @return
         * @throws SQLException the SQL exception
         */
        public <R> R executeThenApply(final Try.BiFunction<Boolean, ? super S, ? extends R, SQLException> getter) throws SQLException {
            checkArgNotNull(getter, "getter");
            assertNotClosed();

            try {
                final boolean isFirstResultSet = stmt.execute();

                return getter.apply(isFirstResultSet, stmt);
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Execute then accept.
         *
         * @param consumer
         * @throws SQLException the SQL exception
         */
        public void executeThenAccept(final Try.Consumer<? super S, SQLException> consumer) throws SQLException {
            checkArgNotNull(consumer, "consumer");
            assertNotClosed();

            try {
                stmt.execute();

                consumer.accept(stmt);
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         * Execute then accept.
         *
         * @param consumer
         * @throws SQLException the SQL exception
         */
        public void executeThenAccept(final Try.BiConsumer<Boolean, ? super S, SQLException> consumer) throws SQLException {
            checkArgNotNull(consumer, "consumer");
            assertNotClosed();

            try {
                final boolean isFirstResultSet = stmt.execute();

                consumer.accept(isFirstResultSet, stmt);
            } finally {
                closeAfterExecutionIfAllowed();
            }
        }

        /**
         *
         * @param <R>
         * @param func
         * @return
         * @deprecated {@code asyncExecutor.call(() -> JdbcUtil.prepareQuery(query)...query/update/execute(...)} is recommended.
         */
        @Deprecated
        public <R> ContinuableFuture<R> asyncApply(final Try.Function<Q, R, SQLException> func) {
            checkArgNotNull(func, "func");
            assertNotClosed();

            final Q q = (Q) this;

            return asyncExecutor.execute(new Try.Callable<R, SQLException>() {
                @Override
                public R call() throws SQLException {
                    return func.apply(q);
                }
            });
        }

        /**
         *
         * @param <R>
         * @param func
         * @param executor
         * @return
         * @deprecated {@code asyncExecutor.call(() -> JdbcUtil.prepareQuery(query)...query/update/execute(...)} is recommended.
         */
        @Deprecated
        public <R> ContinuableFuture<R> asyncApply(final Try.Function<Q, R, SQLException> func, final Executor executor) {
            checkArgNotNull(func, "func");
            checkArgNotNull(executor, "executor");
            assertNotClosed();

            final Q q = (Q) this;

            return ContinuableFuture.call(new Try.Callable<R, SQLException>() {
                @Override
                public R call() throws SQLException {
                    return func.apply(q);
                }
            }, executor);
        }

        /**
         *
         * @param action
         * @return
         * @deprecated {@code asyncExecutor.call(() -> JdbcUtil.prepareQuery(query)...query/update/execute(...)} is recommended.
         */
        @Deprecated
        public ContinuableFuture<Void> asyncAccept(final Try.Consumer<Q, SQLException> action) {
            checkArgNotNull(action, "action");
            assertNotClosed();

            final Q q = (Q) this;

            return asyncExecutor.execute(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    action.accept(q);
                }
            });
        }

        /**
         *
         * @param action
         * @param executor
         * @return
         * @deprecated {@code asyncExecutor.call(() -> JdbcUtil.prepareQuery(query)...query/update/execute(...)} is recommended.
         */
        @Deprecated
        public ContinuableFuture<Void> asyncAccept(final Try.Consumer<Q, SQLException> action, final Executor executor) {
            checkArgNotNull(action, "action");
            checkArgNotNull(executor, "executor");
            assertNotClosed();

            final Q q = (Q) this;

            return ContinuableFuture.run(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    action.accept(q);
                }
            }, executor);
        }

        /**
         * Check arg not null.
         *
         * @param arg
         * @param argName
         */
        protected void checkArgNotNull(Object arg, String argName) {
            if (arg == null) {
                try {
                    close();
                } catch (Exception e) {
                    logger.error("Failed to close PreparedQuery", e);
                }

                throw new IllegalArgumentException("'" + argName + "' can't be null");
            }
        }

        /**
         *
         * @param b
         * @param errorMsg
         */
        protected void checkArg(boolean b, String errorMsg) {
            if (b == false) {
                try {
                    close();
                } catch (Exception e) {
                    logger.error("Failed to close PreparedQuery", e);
                }

                throw new IllegalArgumentException(errorMsg);
            }
        }

        /**
         * Close.
         */
        @Override
        public void close() {
            if (isClosed) {
                return;
            }

            isClosed = true;

            try {
                if (isBatch) {
                    stmt.clearBatch();
                } else {
                    stmt.clearParameters();
                }
            } catch (SQLException e) {
                logger.error("Failed to clear the parameters set in Statements", e);
            } finally {
                if (closeHandler == null) {
                    JdbcUtil.closeQuietly(stmt, conn);
                } else {
                    try {
                        closeHandler.run();
                    } finally {
                        JdbcUtil.closeQuietly(stmt, conn);
                    }
                }
            }
        }

        /**
         * Close after execution if allowed.
         *
         * @throws SQLException the SQL exception
         */
        void closeAfterExecutionIfAllowed() throws SQLException {
            if (isCloseAfterExecution) {
                close();
            }
        }

        /**
         * Assert not closed.
         */
        void assertNotClosed() {
            if (isClosed) {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * The backed {@code PreparedStatement/CallableStatement} will be closed by default
     * after any execution methods(which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/...).
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * Generally, don't cache or reuse the instance of this class,
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * The {@code ResultSet} returned by query will always be closed after execution, even {@code 'closeAfterExecution'} flag is set to {@code false}.
     *
     * <br />
     * Remember: parameter/column index in {@code PreparedStatement/ResultSet} starts from 1, not 0.
     *
     * @author haiyangl
     *
     * @see {@link com.landawn.abacus.annotation.ReadOnly}
     * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
     * @see {@link com.landawn.abacus.annotation.NonUpdatable}
     * @see {@link com.landawn.abacus.annotation.Transient}
     * @see {@link com.landawn.abacus.annotation.Table}
     * @see {@link com.landawn.abacus.annotation.Column}
     *
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html">http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html</a>
     */
    public static class PreparedQuery extends AbstractPreparedQuery<PreparedStatement, PreparedQuery> {

        /**
         * Instantiates a new prepared query.
         *
         * @param stmt
         */
        PreparedQuery(PreparedStatement stmt) {
            super(stmt);
        }
    }

    /**
     * The backed {@code PreparedStatement/CallableStatement} will be closed by default
     * after any execution methods(which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/...).
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * Generally, don't cache or reuse the instance of this class,
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * The {@code ResultSet} returned by query will always be closed after execution, even {@code 'closeAfterExecution'} flag is set to {@code false}.
     *
     * <br />
     * Remember: parameter/column index in {@code PreparedStatement/ResultSet} starts from 1, not 0.
     *
     * @author haiyangl
     *
     * @see {@link com.landawn.abacus.annotation.ReadOnly}
     * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
     * @see {@link com.landawn.abacus.annotation.NonUpdatable}
     * @see {@link com.landawn.abacus.annotation.Transient}
     * @see {@link com.landawn.abacus.annotation.Table}
     * @see {@link com.landawn.abacus.annotation.Column}
     *
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html">http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html</a>
     */
    public static class PreparedCallableQuery extends AbstractPreparedQuery<CallableStatement, PreparedCallableQuery> {

        /** The stmt. */
        final CallableStatement stmt;

        /**
         * Instantiates a new prepared callable query.
         *
         * @param stmt
         */
        PreparedCallableQuery(CallableStatement stmt) {
            super(stmt);
            this.stmt = stmt;
        }

        /**
         * Sets the null.
         *
         * @param parameterName
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNull(String parameterName, int sqlType) throws SQLException {
            stmt.setNull(parameterName, sqlType);

            return this;
        }

        /**
         * Sets the null.
         *
         * @param parameterName
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNull(String parameterName, int sqlType, String typeName) throws SQLException {
            stmt.setNull(parameterName, sqlType, typeName);

            return this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBoolean(String parameterName, boolean x) throws SQLException {
            stmt.setBoolean(parameterName, x);

            return this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBoolean(String parameterName, Boolean x) throws SQLException {
            stmt.setBoolean(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setByte(String parameterName, byte x) throws SQLException {
            stmt.setByte(parameterName, x);

            return this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setByte(String parameterName, Byte x) throws SQLException {
            stmt.setByte(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the short.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setShort(String parameterName, short x) throws SQLException {
            stmt.setShort(parameterName, x);

            return this;
        }

        /**
         * Sets the short.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setShort(String parameterName, Short x) throws SQLException {
            stmt.setShort(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the int.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setInt(String parameterName, int x) throws SQLException {
            stmt.setInt(parameterName, x);

            return this;
        }

        /**
         * Sets the int.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setInt(String parameterName, Integer x) throws SQLException {
            stmt.setInt(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the long.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setLong(String parameterName, long x) throws SQLException {
            stmt.setLong(parameterName, x);

            return this;
        }

        /**
         * Sets the long.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setLong(String parameterName, Long x) throws SQLException {
            stmt.setLong(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the float.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setFloat(String parameterName, float x) throws SQLException {
            stmt.setFloat(parameterName, x);

            return this;
        }

        /**
         * Sets the float.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setFloat(String parameterName, Float x) throws SQLException {
            stmt.setFloat(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the double.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setDouble(String parameterName, double x) throws SQLException {
            stmt.setDouble(parameterName, x);

            return this;
        }

        /**
         * Sets the double.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setDouble(String parameterName, Double x) throws SQLException {
            stmt.setDouble(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the big decimal.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
            stmt.setBigDecimal(parameterName, x);

            return this;
        }

        /**
         * Sets the string.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setString(String parameterName, String x) throws SQLException {
            stmt.setString(parameterName, x);

            return this;
        }

        /**
         * Sets the date.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setDate(String parameterName, java.sql.Date x) throws SQLException {
            stmt.setDate(parameterName, x);

            return this;
        }

        /**
         * Sets the date.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setDate(String parameterName, java.util.Date x) throws SQLException {
            stmt.setDate(parameterName, x == null ? null : x instanceof java.sql.Date ? (java.sql.Date) x : new java.sql.Date(x.getTime()));

            return this;
        }

        /**
         * Sets the time.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setTime(String parameterName, java.sql.Time x) throws SQLException {
            stmt.setTime(parameterName, x);

            return this;
        }

        /**
         * Sets the time.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setTime(String parameterName, java.util.Date x) throws SQLException {
            stmt.setTime(parameterName, x == null ? null : x instanceof java.sql.Time ? (java.sql.Time) x : new java.sql.Time(x.getTime()));

            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setTimestamp(String parameterName, java.sql.Timestamp x) throws SQLException {
            stmt.setTimestamp(parameterName, x);

            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setTimestamp(String parameterName, java.util.Date x) throws SQLException {
            stmt.setTimestamp(parameterName, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));

            return this;
        }

        /**
         * Sets the bytes.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBytes(String parameterName, byte[] x) throws SQLException {
            stmt.setBytes(parameterName, x);

            return this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterName
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setAsciiStream(String parameterName, InputStream inputStream) throws SQLException {
            stmt.setAsciiStream(parameterName, inputStream);

            return this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterName
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setAsciiStream(String parameterName, InputStream inputStream, long length) throws SQLException {
            stmt.setAsciiStream(parameterName, inputStream, length);

            return this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterName
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBinaryStream(String parameterName, InputStream inputStream) throws SQLException {
            stmt.setBinaryStream(parameterName, inputStream);

            return this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterName
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBinaryStream(String parameterName, InputStream inputStream, long length) throws SQLException {
            stmt.setBinaryStream(parameterName, inputStream, length);

            return this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterName
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setCharacterStream(String parameterName, Reader reader) throws SQLException {
            stmt.setCharacterStream(parameterName, reader);

            return this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterName
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
            stmt.setCharacterStream(parameterName, reader, length);

            return this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterName
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNCharacterStream(String parameterName, Reader reader) throws SQLException {
            stmt.setNCharacterStream(parameterName, reader);

            return this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterName
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
            stmt.setNCharacterStream(parameterName, reader, length);

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBlob(String parameterName, java.sql.Blob x) throws SQLException {
            stmt.setBlob(parameterName, x);

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param inputStream
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBlob(String parameterName, InputStream inputStream) throws SQLException {
            stmt.setBlob(parameterName, inputStream);

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param inputStream
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
            stmt.setBlob(parameterName, inputStream, length);

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setClob(String parameterName, java.sql.Clob x) throws SQLException {
            stmt.setClob(parameterName, x);

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setClob(String parameterName, Reader reader) throws SQLException {
            stmt.setClob(parameterName, reader);

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setClob(String parameterName, Reader reader, long length) throws SQLException {
            stmt.setClob(parameterName, reader, length);

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNClob(String parameterName, java.sql.NClob x) throws SQLException {
            stmt.setNClob(parameterName, x);

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param reader
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNClob(String parameterName, Reader reader) throws SQLException {
            stmt.setNClob(parameterName, reader);

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param reader
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setNClob(String parameterName, Reader reader, long length) throws SQLException {
            stmt.setNClob(parameterName, reader, length);

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setURL(String parameterName, URL x) throws SQLException {
            stmt.setURL(parameterName, x);

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setSQLXML(String parameterName, java.sql.SQLXML x) throws SQLException {
            stmt.setSQLXML(parameterName, x);

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setRowId(String parameterName, java.sql.RowId x) throws SQLException {
            stmt.setRowId(parameterName, x);

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setObject(String parameterName, Object x) throws SQLException {
            if (x == null) {
                stmt.setObject(parameterName, x);
            } else {
                N.typeOf(x.getClass()).set(stmt, parameterName, x);
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setObject(String parameterName, Object x, int sqlType) throws SQLException {
            stmt.setObject(parameterName, x, sqlType);

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @param scaleOrLength
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setObject(String parameterName, Object x, int sqlType, int scaleOrLength) throws SQLException {
            stmt.setObject(parameterName, x, sqlType, scaleOrLength);

            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param parameters
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery setParameters(Map<String, Object> parameters) throws SQLException {
            checkArgNotNull(parameters, "parameters");

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                setObject(entry.getKey(), entry.getValue());
            }

            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param parameterNames
         * @param entity
         * @return
         * @throws SQLException the SQL exception
         * @see {@link ClassUtil#getPropNameList(Class)}
         * @see {@link ClassUtil#getPropNameListExclusively(Class, Set)}
         * @see {@link ClassUtil#getPropNameListExclusively(Class, Collection)}
         * @see {@link JdbcUtil#getNamedParameters(String)}
         */
        public PreparedCallableQuery setParameters(List<String> parameterNames, Object entity) throws SQLException {
            checkArgNotNull(parameterNames, "parameterNames");
            checkArgNotNull(entity, "entity");

            final Class<?> cls = entity.getClass();
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
            PropInfo propInfo = null;

            for (String parameterName : parameterNames) {
                propInfo = entityInfo.getPropInfo(parameterName);
                propInfo.dbType.set(stmt, parameterName, propInfo.getPropValue(entity));
            }

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType, scale);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType, typeName);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, int sqlType) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType, scale);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType, typeName);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, SQLType sqlType) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, SQLType sqlType, int scale) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType, scale);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterIndex starts from 1, not 0.
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(int parameterIndex, SQLType sqlType, String typeName) throws SQLException {
            stmt.registerOutParameter(parameterIndex, sqlType, typeName);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, SQLType sqlType) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @param scale
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, SQLType sqlType, int scale) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType, scale);

            return this;
        }

        /**
         * Register out parameter.
         *
         * @param parameterName
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameter(String parameterName, SQLType sqlType, String typeName) throws SQLException {
            stmt.registerOutParameter(parameterName, sqlType, typeName);

            return this;
        }

        /**
         * Register out parameters.
         *
         * @param register
         * @return
         * @throws SQLException the SQL exception
         */
        public PreparedCallableQuery registerOutParameters(final ParametersSetter<? super CallableStatement> register) throws SQLException {
            checkArgNotNull(register, "register");

            boolean noException = false;

            try {
                register.accept(stmt);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return this;
        }

        /**
         * Register out parameters.
         *
         * @param <T>
         * @param parameter
         * @param register
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> PreparedCallableQuery registerOutParameters(final T parameter, final BiParametersSetter<? super T, ? super CallableStatement> register)
                throws SQLException {
            checkArgNotNull(register, "register");

            boolean noException = false;

            try {
                register.accept(parameter, stmt);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return this;
        }

        /**
         *
         * @param <R1>
         * @param resultExtrator1
         * @return
         * @throws SQLException the SQL exception
         */
        public <R1> Optional<R1> call(final ResultExtractor<R1> resultExtrator1) throws SQLException {
            checkArgNotNull(resultExtrator1, "resultExtrator1");
            assertNotClosed();

            try {
                if (stmt.execute()) {
                    if (stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            return Optional.of(resultExtrator1.apply(rs));
                        }
                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }

            return Optional.empty();
        }

        /**
         *
         * @param <R1>
         * @param <R2>
         * @param resultExtrator1
         * @param resultExtrator2
         * @return
         * @throws SQLException the SQL exception
         */
        public <R1, R2> Tuple2<Optional<R1>, Optional<R2>> call(final ResultExtractor<R1> resultExtrator1, final ResultExtractor<R2> resultExtrator2)
                throws SQLException {
            checkArgNotNull(resultExtrator1, "resultExtrator1");
            checkArgNotNull(resultExtrator2, "resultExtrator2");
            assertNotClosed();

            Optional<R1> result1 = Optional.empty();
            Optional<R2> result2 = Optional.empty();

            try {
                if (stmt.execute()) {
                    if (stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result1 = Optional.of(resultExtrator1.apply(rs));
                        }
                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result2 = Optional.of(resultExtrator2.apply(rs));
                        }

                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }

            return Tuple.of(result1, result2);
        }

        /**
         *
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param resultExtrator1
         * @param resultExtrator2
         * @param resultExtrator3
         * @return
         * @throws SQLException the SQL exception
         */
        public <R1, R2, R3> Tuple3<Optional<R1>, Optional<R2>, Optional<R3>> call(final ResultExtractor<R1> resultExtrator1,
                final ResultExtractor<R2> resultExtrator2, final ResultExtractor<R3> resultExtrator3) throws SQLException {
            checkArgNotNull(resultExtrator1, "resultExtrator1");
            checkArgNotNull(resultExtrator2, "resultExtrator2");
            checkArgNotNull(resultExtrator3, "resultExtrator3");
            assertNotClosed();

            Optional<R1> result1 = Optional.empty();
            Optional<R2> result2 = Optional.empty();
            Optional<R3> result3 = Optional.empty();

            try {
                if (stmt.execute()) {
                    if (stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result1 = Optional.of(resultExtrator1.apply(rs));
                        }
                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result2 = Optional.of(resultExtrator2.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result3 = Optional.of(resultExtrator3.apply(rs));
                        }

                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }

            return Tuple.of(result1, result2, result3);
        }

        /**
         *
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param resultExtrator1
         * @param resultExtrator2
         * @param resultExtrator3
         * @param resultExtrator4
         * @return
         * @throws SQLException the SQL exception
         */
        public <R1, R2, R3, R4> Tuple4<Optional<R1>, Optional<R2>, Optional<R3>, Optional<R4>> call(final ResultExtractor<R1> resultExtrator1,
                final ResultExtractor<R2> resultExtrator2, final ResultExtractor<R3> resultExtrator3, final ResultExtractor<R4> resultExtrator4)
                throws SQLException {
            checkArgNotNull(resultExtrator1, "resultExtrator1");
            checkArgNotNull(resultExtrator2, "resultExtrator2");
            checkArgNotNull(resultExtrator3, "resultExtrator3");
            checkArgNotNull(resultExtrator4, "resultExtrator4");
            assertNotClosed();

            Optional<R1> result1 = Optional.empty();
            Optional<R2> result2 = Optional.empty();
            Optional<R3> result3 = Optional.empty();
            Optional<R4> result4 = Optional.empty();

            try {
                if (stmt.execute()) {
                    if (stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result1 = Optional.of(resultExtrator1.apply(rs));
                        }
                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result2 = Optional.of(resultExtrator2.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result3 = Optional.of(resultExtrator3.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result4 = Optional.of(resultExtrator4.apply(rs));
                        }

                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }

            return Tuple.of(result1, result2, result3, result4);
        }

        /**
         *
         * @param <R1>
         * @param <R2>
         * @param <R3>
         * @param <R4>
         * @param <R5>
         * @param resultExtrator1
         * @param resultExtrator2
         * @param resultExtrator3
         * @param resultExtrator4
         * @param resultExtrator5
         * @return
         * @throws SQLException the SQL exception
         */
        public <R1, R2, R3, R4, R5> Tuple5<Optional<R1>, Optional<R2>, Optional<R3>, Optional<R4>, Optional<R5>> call(final ResultExtractor<R1> resultExtrator1,
                final ResultExtractor<R2> resultExtrator2, final ResultExtractor<R3> resultExtrator3, final ResultExtractor<R4> resultExtrator4,
                final ResultExtractor<R5> resultExtrator5) throws SQLException {
            checkArgNotNull(resultExtrator1, "resultExtrator1");
            checkArgNotNull(resultExtrator2, "resultExtrator2");
            checkArgNotNull(resultExtrator3, "resultExtrator3");
            checkArgNotNull(resultExtrator4, "resultExtrator4");
            checkArgNotNull(resultExtrator5, "resultExtrator5");
            assertNotClosed();

            Optional<R1> result1 = Optional.empty();
            Optional<R2> result2 = Optional.empty();
            Optional<R3> result3 = Optional.empty();
            Optional<R4> result4 = Optional.empty();
            Optional<R5> result5 = Optional.empty();

            try {
                if (stmt.execute()) {
                    if (stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result1 = Optional.of(resultExtrator1.apply(rs));
                        }
                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result2 = Optional.of(resultExtrator2.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result3 = Optional.of(resultExtrator3.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result4 = Optional.of(resultExtrator4.apply(rs));
                        }

                    }

                    if (stmt.getMoreResults() && stmt.getUpdateCount() == -1) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            result5 = Optional.of(resultExtrator5.apply(rs));
                        }

                    }
                }
            } finally {
                closeAfterExecutionIfAllowed();
            }

            return Tuple.of(result1, result2, result3, result4, result5);
        }

    }

    /**
     * The backed {@code PreparedStatement/CallableStatement} will be closed by default
     * after any execution methods(which will trigger the backed {@code PreparedStatement/CallableStatement} to be executed, for example: get/query/queryForInt/Long/../findFirst/list/execute/...).
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * Generally, don't cache or reuse the instance of this class,
     * except the {@code 'closeAfterExecution'} flag is set to {@code false} by calling {@code #closeAfterExecution(false)}.
     *
     * <br />
     * The {@code ResultSet} returned by query will always be closed after execution, even {@code 'closeAfterExecution'} flag is set to {@code false}.
     *
     * <br />
     * Remember: parameter/column index in {@code PreparedStatement/ResultSet} starts from 1, not 0.
     *
     * @author haiyangl
     *
     * @see {@link com.landawn.abacus.annotation.ReadOnly}
     * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
     * @see {@link com.landawn.abacus.annotation.NonUpdatable}
     * @see {@link com.landawn.abacus.annotation.Transient}
     * @see {@link com.landawn.abacus.annotation.Table}
     * @see {@link com.landawn.abacus.annotation.Column}
     *
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Connection.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/Statement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html">http://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html</a>
     * @see <a href="http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html">http://docs.oracle.com/javase/8/docs/api/java/sql/ResultSet.html</a>
     */
    public static class NamedQuery extends AbstractPreparedQuery<PreparedStatement, NamedQuery> {

        /** The named SQL. */
        private final NamedSQL namedSQL;

        /** The parameter names. */
        private final List<String> parameterNames;

        /** The parameter count. */
        private final int parameterCount;

        /** The param name index map. */
        private Map<String, IntList> paramNameIndexMap;

        /**
         * Instantiates a new named query.
         *
         * @param stmt
         * @param namedSQL
         */
        NamedQuery(final PreparedStatement stmt, final NamedSQL namedSQL) {
            super(stmt);
            this.namedSQL = namedSQL;
            this.parameterNames = namedSQL.getNamedParameters();
            this.parameterCount = namedSQL.getParameterCount();
        }

        /**
         * Inits the param name index map.
         */
        private void initParamNameIndexMap() {
            paramNameIndexMap = new HashMap<>(parameterCount);
            int index = 1;

            for (String paramName : parameterNames) {
                IntList indexes = paramNameIndexMap.get(paramName);

                if (indexes == null) {
                    indexes = new IntList(1);
                    paramNameIndexMap.put(paramName, indexes);
                }

                indexes.add(index++);
            }
        }

        /**
         * Sets the null.
         *
         * @param parameterName
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNull(String parameterName, int sqlType) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNull(i + 1, sqlType);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNull(indexes.get(0), sqlType);
                    } else if (indexes.size() == 2) {
                        setNull(indexes.get(0), sqlType);
                        setNull(indexes.get(1), sqlType);
                    } else if (indexes.size() == 3) {
                        setNull(indexes.get(0), sqlType);
                        setNull(indexes.get(1), sqlType);
                        setNull(indexes.get(2), sqlType);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNull(indexes.get(i), sqlType);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the null.
         *
         * @param parameterName
         * @param sqlType
         * @param typeName
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNull(String parameterName, int sqlType, String typeName) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNull(i + 1, sqlType, typeName);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNull(indexes.get(0), sqlType, typeName);
                    } else if (indexes.size() == 2) {
                        setNull(indexes.get(0), sqlType, typeName);
                        setNull(indexes.get(1), sqlType, typeName);
                    } else if (indexes.size() == 3) {
                        setNull(indexes.get(0), sqlType, typeName);
                        setNull(indexes.get(1), sqlType, typeName);
                        setNull(indexes.get(2), sqlType, typeName);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNull(indexes.get(i), sqlType, typeName);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBoolean(String parameterName, boolean x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBoolean(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBoolean(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBoolean(indexes.get(0), x);
                        setBoolean(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBoolean(indexes.get(0), x);
                        setBoolean(indexes.get(1), x);
                        setBoolean(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBoolean(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the boolean.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBoolean(String parameterName, Boolean x) throws SQLException {
            setBoolean(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setByte(String parameterName, byte x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setByte(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setByte(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setByte(indexes.get(0), x);
                        setByte(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setByte(indexes.get(0), x);
                        setByte(indexes.get(1), x);
                        setByte(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setByte(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the byte.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setByte(String parameterName, Byte x) throws SQLException {
            setByte(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the short.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setShort(String parameterName, short x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setShort(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setShort(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setShort(indexes.get(0), x);
                        setShort(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setShort(indexes.get(0), x);
                        setShort(indexes.get(1), x);
                        setShort(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setShort(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the short.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setShort(String parameterName, Short x) throws SQLException {
            setShort(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the int.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setInt(String parameterName, int x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setInt(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setInt(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setInt(indexes.get(0), x);
                        setInt(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setInt(indexes.get(0), x);
                        setInt(indexes.get(1), x);
                        setInt(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setInt(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the int.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setInt(String parameterName, Integer x) throws SQLException {
            setInt(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the long.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setLong(String parameterName, long x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setLong(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setLong(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setLong(indexes.get(0), x);
                        setLong(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setLong(indexes.get(0), x);
                        setLong(indexes.get(1), x);
                        setLong(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setLong(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the long.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setLong(String parameterName, Long x) throws SQLException {
            setLong(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the float.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setFloat(String parameterName, float x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setFloat(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setFloat(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setFloat(indexes.get(0), x);
                        setFloat(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setFloat(indexes.get(0), x);
                        setFloat(indexes.get(1), x);
                        setFloat(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setFloat(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the float.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setFloat(String parameterName, Float x) throws SQLException {
            setFloat(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the double.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setDouble(String parameterName, double x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setDouble(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setDouble(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setDouble(indexes.get(0), x);
                        setDouble(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setDouble(indexes.get(0), x);
                        setDouble(indexes.get(1), x);
                        setDouble(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setDouble(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the double.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setDouble(String parameterName, Double x) throws SQLException {
            setDouble(parameterName, N.defaultIfNull(x));

            return this;
        }

        /**
         * Sets the big decimal.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBigDecimal(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBigDecimal(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBigDecimal(indexes.get(0), x);
                        setBigDecimal(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBigDecimal(indexes.get(0), x);
                        setBigDecimal(indexes.get(1), x);
                        setBigDecimal(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBigDecimal(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the string.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setString(String parameterName, String x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setString(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setString(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setString(indexes.get(0), x);
                        setString(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setString(indexes.get(0), x);
                        setString(indexes.get(1), x);
                        setString(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setString(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the date.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setDate(String parameterName, java.sql.Date x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setDate(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setDate(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setDate(indexes.get(0), x);
                        setDate(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setDate(indexes.get(0), x);
                        setDate(indexes.get(1), x);
                        setDate(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setDate(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the date.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setDate(String parameterName, java.util.Date x) throws SQLException {
            setDate(parameterName, x == null ? null : x instanceof java.sql.Date ? (java.sql.Date) x : new java.sql.Date(x.getTime()));

            return this;
        }

        /**
         * Sets the time.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setTime(String parameterName, java.sql.Time x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setTime(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setTime(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setTime(indexes.get(0), x);
                        setTime(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setTime(indexes.get(0), x);
                        setTime(indexes.get(1), x);
                        setTime(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setTime(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the time.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setTime(String parameterName, java.util.Date x) throws SQLException {
            setTime(parameterName, x == null ? null : x instanceof java.sql.Time ? (java.sql.Time) x : new java.sql.Time(x.getTime()));

            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setTimestamp(String parameterName, java.sql.Timestamp x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setTimestamp(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setTimestamp(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setTimestamp(indexes.get(0), x);
                        setTimestamp(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setTimestamp(indexes.get(0), x);
                        setTimestamp(indexes.get(1), x);
                        setTimestamp(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setTimestamp(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setTimestamp(String parameterName, java.util.Date x) throws SQLException {
            setTimestamp(parameterName, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));

            return this;
        }

        /**
         * Sets the bytes.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBytes(String parameterName, byte[] x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBytes(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBytes(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBytes(indexes.get(0), x);
                        setBytes(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBytes(indexes.get(0), x);
                        setBytes(indexes.get(1), x);
                        setBytes(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBytes(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setAsciiStream(String parameterName, InputStream x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setAsciiStream(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setAsciiStream(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setAsciiStream(indexes.get(0), x);
                        setAsciiStream(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setAsciiStream(indexes.get(0), x);
                        setAsciiStream(indexes.get(1), x);
                        setAsciiStream(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setAsciiStream(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the ascii stream.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setAsciiStream(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setAsciiStream(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setAsciiStream(indexes.get(0), x, length);
                        setAsciiStream(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setAsciiStream(indexes.get(0), x, length);
                        setAsciiStream(indexes.get(1), x, length);
                        setAsciiStream(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setAsciiStream(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBinaryStream(String parameterName, InputStream x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBinaryStream(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBinaryStream(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBinaryStream(indexes.get(0), x);
                        setBinaryStream(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBinaryStream(indexes.get(0), x);
                        setBinaryStream(indexes.get(1), x);
                        setBinaryStream(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBinaryStream(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the binary stream.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBinaryStream(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBinaryStream(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setBinaryStream(indexes.get(0), x, length);
                        setBinaryStream(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setBinaryStream(indexes.get(0), x, length);
                        setBinaryStream(indexes.get(1), x, length);
                        setBinaryStream(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBinaryStream(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setCharacterStream(String parameterName, Reader x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setCharacterStream(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setCharacterStream(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setCharacterStream(indexes.get(0), x);
                        setCharacterStream(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setCharacterStream(indexes.get(0), x);
                        setCharacterStream(indexes.get(1), x);
                        setCharacterStream(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setCharacterStream(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the character stream.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setCharacterStream(String parameterName, Reader x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setCharacterStream(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setCharacterStream(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setCharacterStream(indexes.get(0), x, length);
                        setCharacterStream(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setCharacterStream(indexes.get(0), x, length);
                        setCharacterStream(indexes.get(1), x, length);
                        setCharacterStream(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setCharacterStream(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNCharacterStream(String parameterName, Reader x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNCharacterStream(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNCharacterStream(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setNCharacterStream(indexes.get(0), x);
                        setNCharacterStream(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setNCharacterStream(indexes.get(0), x);
                        setNCharacterStream(indexes.get(1), x);
                        setNCharacterStream(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNCharacterStream(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the N character stream.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNCharacterStream(String parameterName, Reader x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNCharacterStream(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNCharacterStream(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setNCharacterStream(indexes.get(0), x, length);
                        setNCharacterStream(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setNCharacterStream(indexes.get(0), x, length);
                        setNCharacterStream(indexes.get(1), x, length);
                        setNCharacterStream(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNCharacterStream(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBlob(String parameterName, java.sql.Blob x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBlob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBlob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBlob(indexes.get(0), x);
                        setBlob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBlob(indexes.get(0), x);
                        setBlob(indexes.get(1), x);
                        setBlob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBlob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBlob(String parameterName, InputStream x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBlob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBlob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setBlob(indexes.get(0), x);
                        setBlob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setBlob(indexes.get(0), x);
                        setBlob(indexes.get(1), x);
                        setBlob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBlob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the blob.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setBlob(String parameterName, InputStream x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setBlob(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setBlob(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setBlob(indexes.get(0), x, length);
                        setBlob(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setBlob(indexes.get(0), x, length);
                        setBlob(indexes.get(1), x, length);
                        setBlob(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setBlob(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setClob(String parameterName, java.sql.Clob x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setClob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setClob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setClob(indexes.get(0), x);
                        setClob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setClob(indexes.get(0), x);
                        setClob(indexes.get(1), x);
                        setClob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setClob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setClob(String parameterName, Reader x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setClob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setClob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setClob(indexes.get(0), x);
                        setClob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setClob(indexes.get(0), x);
                        setClob(indexes.get(1), x);
                        setClob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setClob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the clob.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setClob(String parameterName, Reader x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setClob(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setClob(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setClob(indexes.get(0), x, length);
                        setClob(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setClob(indexes.get(0), x, length);
                        setClob(indexes.get(1), x, length);
                        setClob(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setClob(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNClob(String parameterName, java.sql.NClob x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNClob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNClob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setNClob(indexes.get(0), x);
                        setNClob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setNClob(indexes.get(0), x);
                        setNClob(indexes.get(1), x);
                        setNClob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNClob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNClob(String parameterName, Reader x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNClob(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNClob(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setNClob(indexes.get(0), x);
                        setNClob(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setNClob(indexes.get(0), x);
                        setNClob(indexes.get(1), x);
                        setNClob(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNClob(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the N clob.
         *
         * @param parameterName
         * @param x
         * @param length
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setNClob(String parameterName, Reader x, long length) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setNClob(i + 1, x, length);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setNClob(indexes.get(0), x, length);
                    } else if (indexes.size() == 2) {
                        setNClob(indexes.get(0), x, length);
                        setNClob(indexes.get(1), x, length);
                    } else if (indexes.size() == 3) {
                        setNClob(indexes.get(0), x, length);
                        setNClob(indexes.get(1), x, length);
                        setNClob(indexes.get(2), x, length);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setNClob(indexes.get(i), x, length);
                        }
                    }
                }
            }

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setURL(String parameterName, URL x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setURL(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setURL(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setURL(indexes.get(0), x);
                        setURL(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setURL(indexes.get(0), x);
                        setURL(indexes.get(1), x);
                        setURL(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setURL(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setSQLXML(String parameterName, java.sql.SQLXML x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setSQLXML(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setSQLXML(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setSQLXML(indexes.get(0), x);
                        setSQLXML(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setSQLXML(indexes.get(0), x);
                        setSQLXML(indexes.get(1), x);
                        setSQLXML(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setSQLXML(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setRowId(String parameterName, java.sql.RowId x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setRowId(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setRowId(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setRowId(indexes.get(0), x);
                        setRowId(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setRowId(indexes.get(0), x);
                        setRowId(indexes.get(1), x);
                        setRowId(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setRowId(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setRef(String parameterName, java.sql.Ref x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setRef(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setRef(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setRef(indexes.get(0), x);
                        setRef(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setRef(indexes.get(0), x);
                        setRef(indexes.get(1), x);
                        setRef(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setRef(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         *
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setArray(String parameterName, java.sql.Array x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setArray(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setArray(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setArray(indexes.get(0), x);
                        setArray(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setArray(indexes.get(0), x);
                        setArray(indexes.get(1), x);
                        setArray(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setArray(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setObject(String parameterName, Object x) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setObject(i + 1, x);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setObject(indexes.get(0), x);
                    } else if (indexes.size() == 2) {
                        setObject(indexes.get(0), x);
                        setObject(indexes.get(1), x);
                    } else if (indexes.size() == 3) {
                        setObject(indexes.get(0), x);
                        setObject(indexes.get(1), x);
                        setObject(indexes.get(2), x);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setObject(indexes.get(i), x);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setObject(String parameterName, Object x, int sqlType) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setObject(i + 1, x, sqlType);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setObject(indexes.get(0), x, sqlType);
                    } else if (indexes.size() == 2) {
                        setObject(indexes.get(0), x, sqlType);
                        setObject(indexes.get(1), x, sqlType);
                    } else if (indexes.size() == 3) {
                        setObject(indexes.get(0), x, sqlType);
                        setObject(indexes.get(1), x, sqlType);
                        setObject(indexes.get(2), x, sqlType);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setObject(indexes.get(i), x, sqlType);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @param scaleOrLength
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setObject(String parameterName, Object x, int sqlType, int scaleOrLength) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setObject(i + 1, x, sqlType, scaleOrLength);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                    } else if (indexes.size() == 2) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                        setObject(indexes.get(1), x, sqlType, scaleOrLength);
                    } else if (indexes.size() == 3) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                        setObject(indexes.get(1), x, sqlType, scaleOrLength);
                        setObject(indexes.get(2), x, sqlType, scaleOrLength);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setObject(indexes.get(i), x, sqlType, scaleOrLength);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setObject(String parameterName, Object x, SQLType sqlType) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setObject(i + 1, x, sqlType);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setObject(indexes.get(0), x, sqlType);
                    } else if (indexes.size() == 2) {
                        setObject(indexes.get(0), x, sqlType);
                        setObject(indexes.get(1), x, sqlType);
                    } else if (indexes.size() == 3) {
                        setObject(indexes.get(0), x, sqlType);
                        setObject(indexes.get(1), x, sqlType);
                        setObject(indexes.get(2), x, sqlType);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setObject(indexes.get(i), x, sqlType);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the object.
         *
         * @param parameterName
         * @param x
         * @param sqlType
         * @param scaleOrLength
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setObject(String parameterName, Object x, SQLType sqlType, int scaleOrLength) throws SQLException {
            if (parameterCount < 5) {
                int cnt = 0;

                for (int i = 0; i < parameterCount; i++) {
                    if (parameterNames.get(i).equals(parameterName)) {
                        setObject(i + 1, x, sqlType, scaleOrLength);
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                }
            } else {
                if (paramNameIndexMap == null) {
                    initParamNameIndexMap();
                }

                final IntList indexes = paramNameIndexMap.get(parameterName);

                if (indexes == null) {
                    close();
                    throw new IllegalArgumentException("Not found named parameter: " + parameterName);
                } else {
                    if (indexes.size() == 1) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                    } else if (indexes.size() == 2) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                        setObject(indexes.get(1), x, sqlType, scaleOrLength);
                    } else if (indexes.size() == 3) {
                        setObject(indexes.get(0), x, sqlType, scaleOrLength);
                        setObject(indexes.get(1), x, sqlType, scaleOrLength);
                        setObject(indexes.get(2), x, sqlType, scaleOrLength);
                    } else {
                        for (int i = 0, size = indexes.size(); i < size; i++) {
                            setObject(indexes.get(i), x, sqlType, scaleOrLength);
                        }
                    }
                }
            }

            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param parameters
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setParameters(final Map<String, ?> parameters) throws SQLException {
            checkArgNotNull(parameters, "parameters");

            for (String paramName : parameterNames) {
                if (parameters.containsKey(paramName)) {
                    setObject(paramName, parameters.get(paramName));
                }
            }

            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param entity with getter/setter methods
         * @return
         * @throws SQLException the SQL exception
         */
        public NamedQuery setParameters(final Object entity) throws SQLException {
            checkArgNotNull(entity, "entity");

            if (entity instanceof Map) {
                return setParameters((Map<String, ?>) entity);
            }

            final Class<?> cls = entity.getClass();
            final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
            PropInfo propInfo = null;

            for (int i = 0; i < parameterCount; i++) {
                propInfo = entityInfo.getPropInfo(parameterNames.get(i));

                if (propInfo != null) {
                    propInfo.dbType.set(stmt, i + 1, propInfo.getPropValue(entity));
                }
            }

            return this;
        }

        /**
         * Sets the parameters.
         *
         * @param <T>
         * @param paramaters
         * @param parametersSetter
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> NamedQuery setParameters(final T paramaters, TriParametersSetter<NamedQuery, T> parametersSetter) throws SQLException {
            checkArgNotNull(parametersSetter, "parametersSetter");

            boolean noException = false;

            try {
                parametersSetter.accept(namedSQL, this, paramaters);

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return this;
        }

        /**
         * @param <T>
         * @param batchParameters
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> NamedQuery addBatchParameters(final Collection<T> batchParameters) throws SQLException {
            return addBatchParameters(batchParameters.iterator());
        }

        /**
         *
         * @param <T>
         * @param batchParameters
         * @return
         * @throws SQLException the SQL exception
         */
        public <T> NamedQuery addBatchParameters(final Iterator<T> batchParameters) throws SQLException {
            checkArgNotNull(batchParameters, "batchParameters");

            boolean noException = false;

            try {
                final Iterator<T> iter = batchParameters;

                while (iter.hasNext()) {
                    StatementSetter.DEFAULT.setParameters(namedSQL, stmt, iter.next());
                    stmt.addBatch();
                    isBatch = true;
                }

                noException = true;
            } finally {
                if (noException == false) {
                    close();
                }
            }

            return this;
        }
    }

    /**
     * The Class SimpleTransaction.
     */
    public static class SimpleTransaction {

        /** The Constant logger. */
        private static final Logger logger = LoggerFactory.getLogger(SimpleTransaction.class);

        /** The Constant threadTransactionMap. */
        private static final Map<String, SimpleTransaction> threadTransactionMap = new ConcurrentHashMap<>();
        // private static final Map<String, SimpleTransaction> attachedThreadTransactionMap = new ConcurrentHashMap<>();

        /** The id. */
        private final String id;

        /** The time id. */
        private final String timeId;

        /** The ds. */
        private final javax.sql.DataSource ds;

        /** The conn. */
        private final Connection conn;

        /** The close connection. */
        private final boolean closeConnection;

        /** The original auto commit. */
        private final boolean originalAutoCommit;

        /** The original isolation level. */
        private final int originalIsolationLevel;

        /** The auto shared by prepare query. */
        private final boolean autoSharedByPrepareQuery;

        /** The isolation level. */
        private IsolationLevel isolationLevel;

        /** The status. */
        private Transaction.Status status = Status.ACTIVE;

        /** The ref count. */
        private final AtomicInteger refCount = new AtomicInteger();

        /** The isolation level stack. */
        private final Stack<IsolationLevel> isolationLevelStack = new Stack<>();

        /** The is marked by commit previously. */
        private boolean isMarkedByCommitPreviously = false;

        /**
         * Instantiates a new simple transaction.
         *
         * @param ds
         * @param conn
         * @param isolationLevel
         * @param closeConnection
         * @param autoSharedByPrepareQuery
         * @throws SQLException the SQL exception
         */
        SimpleTransaction(final javax.sql.DataSource ds, final Connection conn, final IsolationLevel isolationLevel, final boolean closeConnection,
                final boolean autoSharedByPrepareQuery) throws SQLException {
            N.checkArgNotNull(conn);
            N.checkArgNotNull(isolationLevel);

            this.id = getTransactionId(ds);
            this.timeId = id + "_" + System.currentTimeMillis();
            this.ds = ds;
            this.conn = conn;
            this.isolationLevel = isolationLevel;
            this.closeConnection = closeConnection;

            this.originalAutoCommit = conn.getAutoCommit();
            this.originalIsolationLevel = conn.getTransactionIsolation();
            this.autoSharedByPrepareQuery = autoSharedByPrepareQuery;

            conn.setAutoCommit(false);

            if (isolationLevel == IsolationLevel.DEFAULT) {
                conn.setTransactionIsolation(isolationLevel.intValue());
            }
        }

        /**
         *
         * @return
         */
        public String id() {
            return timeId;
        }

        /**
         *
         * @return
         */
        public Connection connection() {
            return conn;
        }

        /**
         *
         * @return
         */
        public IsolationLevel isolationLevel() {
            return isolationLevel;
        }

        /**
         *
         * @return
         */
        public Transaction.Status status() {
            return status;
        }

        /**
         * Checks if is active.
         *
         * @return true, if is active
         */
        public boolean isActive() {
            return status == Status.ACTIVE;
        }

        //    /**
        //     * Attaches this transaction to current thread.
        //     *
        //     */
        //    public void attach() {
        //        final String currentThreadName = Thread.currentThread().getName();
        //        final String resourceId = ttid.substring(ttid.lastIndexOf('_') + 1);
        //        final String targetTTID = currentThreadName + "_" + resourceId;
        //
        //        if (attachedThreadTransactionMap.containsKey(targetTTID)) {
        //            throw new IllegalStateException("Transaction(id=" + attachedThreadTransactionMap.get(targetTTID).id()
        //                    + ") has already been attached to current thread: " + currentThreadName);
        //        } else if (threadTransactionMap.containsKey(targetTTID)) {
        //            throw new IllegalStateException(
        //                    "Transaction(id=" + threadTransactionMap.get(targetTTID).id() + ") has already been created in current thread: " + currentThreadName);
        //        }
        //
        //        attachedThreadTransactionMap.put(targetTTID, this);
        //        threadTransactionMap.put(targetTTID, this);
        //    }
        //
        //    public void detach() {
        //        final String currentThreadName = Thread.currentThread().getName();
        //        final String resourceId = ttid.substring(ttid.lastIndexOf('_') + 1);
        //        final String targetTTID = currentThreadName + "_" + resourceId;
        //
        //        if (!attachedThreadTransactionMap.containsKey(targetTTID)) {
        //            throw new IllegalStateException(
        //                    "Transaction(id=" + attachedThreadTransactionMap.get(targetTTID).id() + ") is not attached to current thread: " + currentThreadName);
        //        }
        //
        //        threadTransactionMap.remove(targetTTID);
        //        attachedThreadTransactionMap.remove(targetTTID);
        //    }

        /**
         *
         * @throws SQLException the SQL exception
         */
        public void commit() throws SQLException {
            final int refCount = decrementAndGetRef();
            isMarkedByCommitPreviously = true;

            if (refCount > 0) {
                return;
            } else if (refCount < 0) {
                logger.warn("Transaction(id={}) is already: {}. This committing is ignored", timeId, status);
                return;
            }

            if (status == Status.MARKED_ROLLBACK) {
                logger.warn("Transaction(id={}) will be rolled back because it's marked for roll back only", timeId);
                executeRollback();
                return;
            }

            if (status != Status.ACTIVE) {
                throw new IllegalArgumentException("Transaction(id=" + timeId + ") is already: " + status + ". It can not be committed");
            }

            logger.info("Committing transaction(id={})", timeId);

            status = Status.FAILED_COMMIT;

            try {
                if (originalAutoCommit) {
                    conn.commit();
                }

                status = Status.COMMITTED;
            } finally {
                if (status == Status.COMMITTED) {
                    logger.info("Transaction(id={}) has been committed successfully", timeId);

                    resetAndCloseConnection();
                } else {
                    logger.warn("Failed to commit transaction(id={}). It will automatically be rolled back ", timeId);
                    executeRollback();
                }
            }
        }

        /**
         * Rollback if not committed.
         *
         * @throws UncheckedSQLException the unchecked SQL exception
         */
        public void rollbackIfNotCommitted() throws UncheckedSQLException {
            if (isMarkedByCommitPreviously) { // Do nothing. It happened in finally block.
                isMarkedByCommitPreviously = false;
                return;
            }

            final int refCount = decrementAndGetRef();

            if (refCount > 0) {
                status = Status.MARKED_ROLLBACK;
                return;
            } else if (refCount < 0) {
                if (refCount == -1
                        && (status == Status.COMMITTED || status == Status.FAILED_COMMIT || status == Status.ROLLED_BACK || status == Status.FAILED_ROLLBACK)) {
                    // Do nothing. It happened in finally block.
                } else {
                    logger.warn("Transaction(id={}) is already: {}. This rollback is ignored", timeId, status);
                }

                return;
            }

            if (!(status == Status.ACTIVE || status == Status.MARKED_ROLLBACK || status == Status.FAILED_COMMIT || status == Status.FAILED_ROLLBACK)) {
                throw new IllegalArgumentException("Transaction(id=" + timeId + ") is already: " + status + ". It can not be rolled back");
            }

            executeRollback();
        }

        /**
         *
         * @throws UncheckedSQLException the unchecked SQL exception
         */
        private void executeRollback() throws UncheckedSQLException {
            logger.warn("Rolling back transaction(id={})", timeId);

            status = Status.FAILED_ROLLBACK;

            try {
                if (originalAutoCommit) {
                    conn.rollback();
                }

                status = Status.ROLLED_BACK;
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                if (status == Status.ROLLED_BACK) {
                    logger.warn("Transaction(id={}) has been rolled back successfully", timeId);
                } else {
                    logger.warn("Failed to roll back transaction(id={})", timeId);
                }

                resetAndCloseConnection();
            }
        }

        /**
         * Reset and close connection.
         */
        private void resetAndCloseConnection() {
            try {
                conn.setAutoCommit(originalAutoCommit);
                conn.setTransactionIsolation(originalIsolationLevel);
            } catch (SQLException e) {
                logger.warn("Failed to reset connection", e);
            } finally {
                if (closeConnection) {
                    JdbcUtil.releaseConnection(conn, ds);
                }
            }
        }

        /**
         * Increment and get ref.
         *
         * @param isolationLevel
         * @return
         * @throws UncheckedSQLException the unchecked SQL exception
         */
        synchronized int incrementAndGetRef(final IsolationLevel isolationLevel) throws UncheckedSQLException {
            if (!status.equals(Status.ACTIVE)) {
                throw new IllegalStateException("Transaction(id=" + timeId + ") is already: " + status);
            }

            isMarkedByCommitPreviously = false;

            if (refCount.get() > 0) {
                try {
                    conn.setTransactionIsolation(isolationLevel == IsolationLevel.DEFAULT ? this.originalIsolationLevel : isolationLevel.intValue());
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }

                this.isolationLevelStack.push(this.isolationLevel);
                this.isolationLevel = isolationLevel;
            }

            return refCount.incrementAndGet();
        }

        /**
         * Decrement and get ref.
         *
         * @return
         * @throws UncheckedSQLException the unchecked SQL exception
         */
        synchronized int decrementAndGetRef() throws UncheckedSQLException {
            final int res = refCount.decrementAndGet();

            if (res == 0) {
                threadTransactionMap.remove(id);
                logger.info("Finishing transaction(id={})", timeId);

                logger.debug("Remaining active transactions: {}", threadTransactionMap.values());
            } else if (res > 0) {
                this.isolationLevel = isolationLevelStack.pop();

                try {
                    conn.setTransactionIsolation(isolationLevel == IsolationLevel.DEFAULT ? this.originalIsolationLevel : isolationLevel.intValue());
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            }

            return res;
        }

        /**
         * Auto shared by prepare query.
         *
         * @return true, if successful
         */
        boolean autoSharedByPrepareQuery() {
            return autoSharedByPrepareQuery;
        }

        /**
         * Gets the transaction id.
         *
         * @param ds
         * @return
         */
        static String getTransactionId(final javax.sql.DataSource ds) {
            return Thread.currentThread().getName() + "_" + System.identityHashCode(ds);
        }

        /**
         * Gets the transaction thread id.
         *
         * @param conn
         * @return
         */
        static String getTransactionThreadId(final Connection conn) {
            return Thread.currentThread().getName() + "_" + System.identityHashCode(conn);
        }

        /**
         * Gets the transaction.
         *
         * @param ds
         * @return
         */
        static SimpleTransaction getTransaction(final javax.sql.DataSource ds) {
            return threadTransactionMap.get(getTransactionId(ds));
        }

        /**
         * Gets the transaction.
         *
         * @param conn
         * @return
         */
        static SimpleTransaction getTransaction(final Connection conn) {
            return threadTransactionMap.get(getTransactionThreadId(conn));
        }

        /**
         *
         * @param ds
         * @param tran
         * @return
         */
        static SimpleTransaction putTransaction(final javax.sql.DataSource ds, final SimpleTransaction tran) {
            return threadTransactionMap.put(getTransactionId(ds), tran);
        }

        /**
         *
         * @param conn
         * @param tran
         * @return
         */
        static SimpleTransaction putTransaction(final Connection conn, final SimpleTransaction tran) {
            return threadTransactionMap.put(getTransactionThreadId(conn), tran);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return timeId.hashCode();
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            return obj instanceof SimpleTransaction && timeId.equals(((SimpleTransaction) obj).timeId);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "SimpleTransaction={id=" + timeId + "}";
        }
    }

    /**
     * The Enum FetchDirection.
     */
    public enum FetchDirection {

        /** The forward. */
        FORWARD(ResultSet.FETCH_FORWARD),
        /** The reverse. */
        REVERSE(ResultSet.FETCH_REVERSE),
        /** The unknown. */
        UNKNOWN(ResultSet.FETCH_UNKNOWN);

        /** The int value. */
        private final int intValue;

        /**
         * Instantiates a new fetch direction.
         *
         * @param intValue
         */
        FetchDirection(int intValue) {
            this.intValue = intValue;
        }

        /**
         *
         * @param intValue
         * @return
         */
        public static FetchDirection valueOf(int intValue) {
            switch (intValue) {
                case ResultSet.FETCH_FORWARD:
                    return FORWARD;

                case ResultSet.FETCH_REVERSE:
                    return REVERSE;

                case ResultSet.FETCH_UNKNOWN:
                    return UNKNOWN;

                default:
                    throw new IllegalArgumentException("No FetchDirection mapping to int value: " + intValue);

            }
        }

        /**
         *
         * @return
         */
        public int intValue() {
            return intValue;
        }
    }

    /**
     * The Interface ParametersSetter.
     *
     * @param <QS>
     */
    public interface ParametersSetter<QS> extends Try.Consumer<QS, SQLException> {

        /**
         *
         * @param preparedQuery
         * @throws SQLException the SQL exception
         */
        @Override
        void accept(QS preparedQuery) throws SQLException;
    }

    /**
     * The Interface BiParametersSetter.
     *
     * @param <QS>
     * @param <T>
     */
    public interface BiParametersSetter<QS, T> extends Try.BiConsumer<QS, T, SQLException> {

        /**
         *
         * @param preparedQuery
         * @param t
         * @throws SQLException the SQL exception
         */
        @Override
        void accept(QS preparedQuery, T t) throws SQLException;
    }

    /**
     * The Interface TriParametersSetter.
     *
     * @param <QS>
     * @param <T>
     */
    public interface TriParametersSetter<QS, T> extends Try.TriConsumer<NamedSQL, QS, T, SQLException> {

        /**
         *
         * @param namedSQL
         * @param preparedQuery
         * @param t
         * @throws SQLException the SQL exception
         */
        @Override
        void accept(NamedSQL namedSQL, QS preparedQuery, T t) throws SQLException;
    }

    /**
     * The Interface ResultExtractor.
     *
     * @param <T>
     */
    public interface ResultExtractor<T> extends Try.Function<ResultSet, T, SQLException> {

        /** The Constant TO_DATA_SET. */
        ResultExtractor<DataSet> TO_DATA_SET = new ResultExtractor<DataSet>() {
            @Override
            public DataSet apply(final ResultSet rs) throws SQLException {
                return JdbcUtil.extractData(rs);
            }
        };

        /**
         *
         * @param rs
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        T apply(ResultSet rs) throws SQLException;

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @return
         */
        static <K, V> ResultExtractor<Map<K, V>> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor) {
            return toMap(keyExtractor, valueExtractor, Suppliers.<K, V> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param supplier
         * @return
         */
        static <K, V, M extends Map<K, V>> ResultExtractor<M> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Supplier<? extends M> supplier) {
            return toMap(keyExtractor, valueExtractor, Fnn.throwingMerger(), supplier);
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @param mergeFunction
         * @return
         * @see {@link Fn.EE#throwingMerger()}
         * @see {@link Fn.EE#replacingMerger()}
         * @see {@link Fn.EE#ignoringMerger()}
         */
        static <K, V> ResultExtractor<Map<K, V>> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Try.BinaryOperator<V, SQLException> mergeFunction) {
            return toMap(keyExtractor, valueExtractor, mergeFunction, Suppliers.<K, V> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param mergeFunction
         * @param supplier
         * @return
         * @see {@link Fn.EE#throwingMerger()}
         * @see {@link Fn.EE#replacingMerger()}
         * @see {@link Fn.EE#ignoringMerger()}
         */
        static <K, V, M extends Map<K, V>> ResultExtractor<M> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Try.BinaryOperator<V, SQLException> mergeFunction, final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(mergeFunction, "mergeFunction");
            N.checkArgNotNull(supplier, "supplier");

            return new ResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs) throws SQLException {
                    final M result = supplier.get();

                    while (rs.next()) {
                        Maps.merge(result, keyExtractor.apply(rs), valueExtractor.apply(rs), mergeFunction);
                    }

                    return result;
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <A>
         * @param <D>
         * @param keyExtractor
         * @param valueExtractor
         * @param downstream
         * @return
         */
        static <K, V, A, D> ResultExtractor<Map<K, D>> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Collector<? super V, A, D> downstream) {
            return toMap(keyExtractor, valueExtractor, downstream, Suppliers.<K, D> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <A>
         * @param <D>
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param downstream
         * @param supplier
         * @return
         */
        static <K, V, A, D, M extends Map<K, D>> ResultExtractor<M> toMap(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Collector<? super V, A, D> downstream, final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(downstream, "downstream");
            N.checkArgNotNull(supplier, "supplier");

            return new ResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs) throws SQLException {

                    final Supplier<A> downstreamSupplier = downstream.supplier();
                    final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
                    final Function<A, D> downstreamFinisher = downstream.finisher();

                    final M result = supplier.get();
                    final Map<K, A> tmp = (Map<K, A>) result;
                    K key = null;
                    A container = null;

                    while (rs.next()) {
                        key = keyExtractor.apply(rs);
                        container = tmp.get(key);

                        if (container == null) {
                            container = downstreamSupplier.get();
                            tmp.put(key, container);
                        }

                        downstreamAccumulator.accept(container, valueExtractor.apply(rs));
                    }

                    for (Map.Entry<K, D> entry : result.entrySet()) {
                        entry.setValue(downstreamFinisher.apply((A) entry.getValue()));
                    }

                    return result;
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        static <K, V> ResultExtractor<Map<K, List<V>>> groupTo(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor) throws SQLException {
            return groupTo(keyExtractor, valueExtractor, Suppliers.<K, List<V>> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param supplier
         * @return
         */
        static <K, V, M extends Map<K, List<V>>> ResultExtractor<M> groupTo(final RowMapper<K> keyExtractor, final RowMapper<V> valueExtractor,
                final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(supplier, "supplier");

            return new ResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs) throws SQLException {

                    final M result = supplier.get();
                    K key = null;
                    List<V> value = null;

                    while (rs.next()) {
                        key = keyExtractor.apply(rs);
                        value = result.get(key);

                        if (value == null) {
                            value = new ArrayList<>();
                            result.put(key, value);
                        }

                        value.add(valueExtractor.apply(rs));
                    }

                    return result;
                }
            };
        }
    }

    /**
     * The Interface BiResultExtractor.
     *
     * @param <T>
     */
    public interface BiResultExtractor<T> extends Try.BiFunction<ResultSet, List<String>, T, SQLException> {

        /**
         *
         * @param rs
         * @param columnLabels
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        T apply(ResultSet rs, List<String> columnLabels) throws SQLException;

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @return
         */
        static <K, V> BiResultExtractor<Map<K, V>> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor) {
            return toMap(keyExtractor, valueExtractor, Suppliers.<K, V> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param supplier
         * @return
         */
        static <K, V, M extends Map<K, V>> BiResultExtractor<M> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Supplier<? extends M> supplier) {
            return toMap(keyExtractor, valueExtractor, Fnn.throwingMerger(), supplier);
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @param mergeFunction
         * @return
         * @see {@link Fn.EE#throwingMerger()}
         * @see {@link Fn.EE#replacingMerger()}
         * @see {@link Fn.EE#ignoringMerger()}
         */
        static <K, V> BiResultExtractor<Map<K, V>> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Try.BinaryOperator<V, SQLException> mergeFunction) {
            return toMap(keyExtractor, valueExtractor, mergeFunction, Suppliers.<K, V> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param mergeFunction
         * @param supplier
         * @return
         * @see {@link Fn.EE#throwingMerger()}
         * @see {@link Fn.EE#replacingMerger()}
         * @see {@link Fn.EE#ignoringMerger()}
         */
        static <K, V, M extends Map<K, V>> BiResultExtractor<M> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Try.BinaryOperator<V, SQLException> mergeFunction, final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(mergeFunction, "mergeFunction");
            N.checkArgNotNull(supplier, "supplier");

            return new BiResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs, final List<String> columnLabels) throws SQLException {
                    final M result = supplier.get();

                    while (rs.next()) {
                        Maps.merge(result, keyExtractor.apply(rs, columnLabels), valueExtractor.apply(rs, columnLabels), mergeFunction);
                    }

                    return result;
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <A>
         * @param <D>
         * @param keyExtractor
         * @param valueExtractor
         * @param downstream
         * @return
         */
        static <K, V, A, D> BiResultExtractor<Map<K, D>> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Collector<? super V, A, D> downstream) {
            return toMap(keyExtractor, valueExtractor, downstream, Suppliers.<K, D> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <A>
         * @param <D>
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param downstream
         * @param supplier
         * @return
         */
        static <K, V, A, D, M extends Map<K, D>> BiResultExtractor<M> toMap(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Collector<? super V, A, D> downstream, final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(downstream, "downstream");
            N.checkArgNotNull(supplier, "supplier");

            return new BiResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs, final List<String> columnLabels) throws SQLException {

                    final Supplier<A> downstreamSupplier = downstream.supplier();
                    final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
                    final Function<A, D> downstreamFinisher = downstream.finisher();

                    final M result = supplier.get();
                    final Map<K, A> tmp = (Map<K, A>) result;
                    K key = null;
                    A container = null;

                    while (rs.next()) {
                        key = keyExtractor.apply(rs, columnLabels);
                        container = tmp.get(key);

                        if (container == null) {
                            container = downstreamSupplier.get();
                            tmp.put(key, container);
                        }

                        downstreamAccumulator.accept(container, valueExtractor.apply(rs, columnLabels));
                    }

                    for (Map.Entry<K, D> entry : result.entrySet()) {
                        entry.setValue(downstreamFinisher.apply((A) entry.getValue()));
                    }

                    return result;
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyExtractor
         * @param valueExtractor
         * @return
         * @throws SQLException the SQL exception
         */
        static <K, V> BiResultExtractor<Map<K, List<V>>> groupTo(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor) throws SQLException {
            return groupTo(keyExtractor, valueExtractor, Suppliers.<K, List<V>> ofMap());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param keyExtractor
         * @param valueExtractor
         * @param supplier
         * @return
         */
        static <K, V, M extends Map<K, List<V>>> BiResultExtractor<M> groupTo(final BiRowMapper<K> keyExtractor, final BiRowMapper<V> valueExtractor,
                final Supplier<? extends M> supplier) {
            N.checkArgNotNull(keyExtractor, "keyExtractor");
            N.checkArgNotNull(valueExtractor, "valueExtractor");
            N.checkArgNotNull(supplier, "supplier");

            return new BiResultExtractor<M>() {
                @Override
                public M apply(final ResultSet rs, List<String> columnLabels) throws SQLException {
                    final M result = supplier.get();
                    K key = null;
                    List<V> value = null;

                    while (rs.next()) {
                        key = keyExtractor.apply(rs, columnLabels);
                        value = result.get(key);

                        if (value == null) {
                            value = new ArrayList<>();
                            result.put(key, value);
                        }

                        value.add(valueExtractor.apply(rs, columnLabels));
                    }

                    return result;
                }
            };
        }
    }

    /**
     * Don't use {@code RowMapper} in {@link PreparedQuery#list(RowMapper)} or any place where multiple records will be retrieved by it, if column labels/count are used in {@link RowMapper#apply(ResultSet)}.
     * Consider using {@code BiRowMapper} instead because it's more efficient to retrieve multiple records when column labels/count are used.
     *
     * @param <T>
     */
    public interface RowMapper<T> extends Try.Function<ResultSet, T, SQLException> {

        /** The Constant GET_BOOLEAN. */
        RowMapper<Boolean> GET_BOOLEAN = new RowMapper<Boolean>() {
            @Override
            public Boolean apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getBoolean(1);
            }
        };

        /** The Constant GET_BYTE. */
        RowMapper<Byte> GET_BYTE = new RowMapper<Byte>() {
            @Override
            public Byte apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getByte(1);
            }
        };

        /** The Constant GET_SHORT. */
        RowMapper<Short> GET_SHORT = new RowMapper<Short>() {
            @Override
            public Short apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getShort(1);
            }
        };

        /** The Constant GET_INT. */
        RowMapper<Integer> GET_INT = new RowMapper<Integer>() {
            @Override
            public Integer apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getInt(1);
            }
        };

        /** The Constant GET_LONG. */
        RowMapper<Long> GET_LONG = new RowMapper<Long>() {
            @Override
            public Long apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getLong(1);
            }
        };

        /** The Constant GET_FLOAT. */
        RowMapper<Float> GET_FLOAT = new RowMapper<Float>() {
            @Override
            public Float apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getFloat(1);
            }
        };

        /** The Constant GET_DOUBLE. */
        RowMapper<Double> GET_DOUBLE = new RowMapper<Double>() {
            @Override
            public Double apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getDouble(1);
            }
        };

        /** The Constant GET_BIG_DECIMAL. */
        RowMapper<BigDecimal> GET_BIG_DECIMAL = new RowMapper<BigDecimal>() {
            @Override
            public BigDecimal apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getBigDecimal(1);
            }
        };

        /** The Constant GET_STRING. */
        RowMapper<String> GET_STRING = new RowMapper<String>() {
            @Override
            public String apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getString(1);
            }
        };

        /** The Constant GET_DATE. */
        RowMapper<Date> GET_DATE = new RowMapper<Date>() {
            @Override
            public Date apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getDate(1);
            }
        };

        /** The Constant GET_TIME. */
        RowMapper<Time> GET_TIME = new RowMapper<Time>() {
            @Override
            public Time apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getTime(1);
            }
        };

        /** The Constant GET_TIMESTAMP. */
        RowMapper<Timestamp> GET_TIMESTAMP = new RowMapper<Timestamp>() {
            @Override
            public Timestamp apply(final ResultSet rs) throws SQLException, RuntimeException {
                return rs.getTimestamp(1);
            }
        };

        /**
         *
         * @param rs
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        T apply(ResultSet rs) throws SQLException;

        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param leftType
        // * @param rightType
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <L, R> RowMapper<Pair<L, R>, RuntimeException> toPair(final Class<L> leftType, final Class<R> rightType) {
        //    N.checkArgNotNull(leftType, "leftType");
        //    N.checkArgNotNull(rightType, "rightType");
        //
        //    return new RowMapper<Pair<L, R>, RuntimeException>() {
        //        private final Type<L> leftT = N.typeOf(leftType);
        //        private final Type<R> rightT = N.typeOf(rightType);
        //
        //        @Override
        //        public Pair<L, R> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Pair.of(leftT.get(rs, 1), rightT.get(rs, 2));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param leftType
        // * @param middleType
        // * @param rightType
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <L, M, R> RowMapper<Triple<L, M, R>, RuntimeException> toTriple(final Class<L> leftType, final Class<M> middleType,
        //        final Class<R> rightType) {
        //    N.checkArgNotNull(leftType, "leftType");
        //    N.checkArgNotNull(middleType, "middleType");
        //    N.checkArgNotNull(rightType, "rightType");
        //
        //    return new RowMapper<Triple<L, M, R>, RuntimeException>() {
        //        private final Type<L> leftT = N.typeOf(leftType);
        //        private final Type<M> middleT = N.typeOf(middleType);
        //        private final Type<R> rightT = N.typeOf(rightType);
        //
        //        @Override
        //        public Triple<L, M, R> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Triple.of(leftT.get(rs, 1), middleT.get(rs, 2), rightT.get(rs, 3));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2> RowMapper<Tuple2<T1, T2>, RuntimeException> toTuple(final Class<T1> type1, final Class<T2> type2) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //
        //    return new RowMapper<Tuple2<T1, T2>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //
        //        @Override
        //        public Tuple2<T1, T2> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @param type3
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2, T3> RowMapper<Tuple3<T1, T2, T3>, RuntimeException> toTuple(final Class<T1> type1, final Class<T2> type2,
        //        final Class<T3> type3) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //    N.checkArgNotNull(type3, "type3");
        //
        //    return new RowMapper<Tuple3<T1, T2, T3>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //        private final Type<T3> t3 = N.typeOf(type3);
        //
        //        @Override
        //        public Tuple3<T1, T2, T3> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2), t3.get(rs, 3));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @param type3
        // * @param type4
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2, T3, T4> RowMapper<Tuple4<T1, T2, T3, T4>, RuntimeException> toTuple(final Class<T1> type1, final Class<T2> type2,
        //        final Class<T3> type3, final Class<T4> type4) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //    N.checkArgNotNull(type3, "type3");
        //    N.checkArgNotNull(type4, "type4");
        //
        //    return new RowMapper<Tuple4<T1, T2, T3, T4>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //        private final Type<T3> t3 = N.typeOf(type3);
        //        private final Type<T4> t4 = N.typeOf(type4);
        //
        //        @Override
        //        public Tuple4<T1, T2, T3, T4> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2), t3.get(rs, 3), t4.get(rs, 4));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @param type3
        // * @param type4
        // * @param type5
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2, T3, T4, T5> RowMapper<Tuple5<T1, T2, T3, T4, T5>, RuntimeException> toTuple(final Class<T1> type1, final Class<T2> type2,
        //        final Class<T3> type3, final Class<T4> type4, final Class<T5> type5) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //    N.checkArgNotNull(type3, "type3");
        //    N.checkArgNotNull(type4, "type4");
        //    N.checkArgNotNull(type5, "type5");
        //
        //    return new RowMapper<Tuple5<T1, T2, T3, T4, T5>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //        private final Type<T3> t3 = N.typeOf(type3);
        //        private final Type<T4> t4 = N.typeOf(type4);
        //        private final Type<T5> t5 = N.typeOf(type5);
        //
        //        @Override
        //        public Tuple5<T1, T2, T3, T4, T5> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2), t3.get(rs, 3), t4.get(rs, 4), t5.get(rs, 5));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @param type3
        // * @param type4
        // * @param type5
        // * @param type6
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2, T3, T4, T5, T6> RowMapper<Tuple6<T1, T2, T3, T4, T5, T6>, RuntimeException> toTuple(final Class<T1> type1,
        //        final Class<T2> type2, final Class<T3> type3, final Class<T4> type4, final Class<T5> type5, final Class<T6> type6) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //    N.checkArgNotNull(type3, "type3");
        //    N.checkArgNotNull(type4, "type4");
        //    N.checkArgNotNull(type5, "type5");
        //    N.checkArgNotNull(type6, "type6");
        //
        //    return new RowMapper<Tuple6<T1, T2, T3, T4, T5, T6>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //        private final Type<T3> t3 = N.typeOf(type3);
        //        private final Type<T4> t4 = N.typeOf(type4);
        //        private final Type<T5> t5 = N.typeOf(type5);
        //        private final Type<T6> t6 = N.typeOf(type6);
        //
        //        @Override
        //        public Tuple6<T1, T2, T3, T4, T5, T6> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2), t3.get(rs, 3), t4.get(rs, 4), t5.get(rs, 5), t6.get(rs, 6));
        //        }
        //    };
        //}
        //
        ///**
        // * Totally unnecessary. It's more efficient by direct call.
        // *
        // * @param type1
        // * @param type2
        // * @param type3
        // * @param type4
        // * @param type5
        // * @param type6
        // * @param type7
        // * @return
        // * @deprecated
        // */
        //@Deprecated
        //public static <T1, T2, T3, T4, T5, T6, T7> RowMapper<Tuple7<T1, T2, T3, T4, T5, T6, T7>, RuntimeException> toTuple(final Class<T1> type1,
        //        final Class<T2> type2, final Class<T3> type3, final Class<T4> type4, final Class<T5> type5, final Class<T6> type6, final Class<T7> type7) {
        //    N.checkArgNotNull(type1, "type1");
        //    N.checkArgNotNull(type2, "type2");
        //    N.checkArgNotNull(type3, "type3");
        //    N.checkArgNotNull(type4, "type4");
        //    N.checkArgNotNull(type5, "type5");
        //    N.checkArgNotNull(type6, "type6");
        //    N.checkArgNotNull(type7, "type7");
        //
        //    return new RowMapper<Tuple7<T1, T2, T3, T4, T5, T6, T7>, RuntimeException>() {
        //        private final Type<T1> t1 = N.typeOf(type1);
        //        private final Type<T2> t2 = N.typeOf(type2);
        //        private final Type<T3> t3 = N.typeOf(type3);
        //        private final Type<T4> t4 = N.typeOf(type4);
        //        private final Type<T5> t5 = N.typeOf(type5);
        //        private final Type<T6> t6 = N.typeOf(type6);
        //        private final Type<T7> t7 = N.typeOf(type7);
        //
        //        @Override
        //        public Tuple7<T1, T2, T3, T4, T5, T6, T7> apply(ResultSet rs) throws SQLException, RuntimeException {
        //            return Tuple.of(t1.get(rs, 1), t2.get(rs, 2), t3.get(rs, 3), t4.get(rs, 4), t5.get(rs, 5), t6.get(rs, 6), t7.get(rs, 7));
        //        }
        //    };
        //}
    }

    /**
     * The Interface BiRowMapper.
     *
     * @param <T>
     */
    public interface BiRowMapper<T> extends Try.BiFunction<ResultSet, List<String>, T, SQLException> {

        /** The Constant GET_BOOLEAN. */
        BiRowMapper<Boolean> GET_BOOLEAN = new BiRowMapper<Boolean>() {
            @Override
            public Boolean apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getBoolean(1);
            }
        };

        /** The Constant GET_BYTE. */
        BiRowMapper<Byte> GET_BYTE = new BiRowMapper<Byte>() {
            @Override
            public Byte apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getByte(1);
            }
        };

        /** The Constant GET_SHORT. */
        BiRowMapper<Short> GET_SHORT = new BiRowMapper<Short>() {
            @Override
            public Short apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getShort(1);
            }
        };

        /** The Constant GET_INT. */
        BiRowMapper<Integer> GET_INT = new BiRowMapper<Integer>() {
            @Override
            public Integer apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getInt(1);
            }
        };

        /** The Constant GET_LONG. */
        BiRowMapper<Long> GET_LONG = new BiRowMapper<Long>() {
            @Override
            public Long apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getLong(1);
            }
        };

        /** The Constant GET_FLOAT. */
        BiRowMapper<Float> GET_FLOAT = new BiRowMapper<Float>() {
            @Override
            public Float apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getFloat(1);
            }
        };

        /** The Constant GET_DOUBLE. */
        BiRowMapper<Double> GET_DOUBLE = new BiRowMapper<Double>() {
            @Override
            public Double apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getDouble(1);
            }
        };

        /** The Constant GET_BIG_DECIMAL. */
        BiRowMapper<BigDecimal> GET_BIG_DECIMAL = new BiRowMapper<BigDecimal>() {
            @Override
            public BigDecimal apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getBigDecimal(1);
            }
        };

        /** The Constant GET_STRING. */
        BiRowMapper<String> GET_STRING = new BiRowMapper<String>() {
            @Override
            public String apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getString(1);
            }
        };

        /** The Constant GET_DATE. */
        BiRowMapper<Date> GET_DATE = new BiRowMapper<Date>() {
            @Override
            public Date apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getDate(1);
            }
        };

        /** The Constant GET_TIME. */
        BiRowMapper<Time> GET_TIME = new BiRowMapper<Time>() {
            @Override
            public Time apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getTime(1);
            }
        };

        /** The Constant GET_TIMESTAMP. */
        BiRowMapper<Timestamp> GET_TIMESTAMP = new BiRowMapper<Timestamp>() {
            @Override
            public Timestamp apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                return rs.getTimestamp(1);
            }
        };

        /** The Constant TO_ARRAY. */
        BiRowMapper<Object[]> TO_ARRAY = new BiRowMapper<Object[]>() {
            @Override
            public Object[] apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                final int columnCount = columnLabels.size();
                final Object[] result = new Object[columnCount];

                for (int i = 1; i <= columnCount; i++) {
                    result[i - 1] = JdbcUtil.getColumnValue(rs, i);
                }

                return result;
            }
        };

        /** The Constant TO_LIST. */
        BiRowMapper<List<Object>> TO_LIST = new BiRowMapper<List<Object>>() {
            @Override
            public List<Object> apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                final int columnCount = columnLabels.size();
                final List<Object> result = new ArrayList<>(columnCount);

                for (int i = 1; i <= columnCount; i++) {
                    result.add(JdbcUtil.getColumnValue(rs, i));
                }

                return result;
            }
        };

        /** The Constant TO_MAP. */
        BiRowMapper<Map<String, Object>> TO_MAP = new BiRowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                final int columnCount = columnLabels.size();
                final Map<String, Object> result = new HashMap<>(columnCount);

                for (int i = 1; i <= columnCount; i++) {
                    result.put(columnLabels.get(i - 1), JdbcUtil.getColumnValue(rs, i));
                }

                return result;
            }
        };

        /** The Constant TO_LINKED_HASH_MAP. */
        BiRowMapper<Map<String, Object>> TO_LINKED_HASH_MAP = new BiRowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(final ResultSet rs, final List<String> columnLabels) throws SQLException, RuntimeException {
                final int columnCount = columnLabels.size();
                final Map<String, Object> result = new LinkedHashMap<>(columnCount);

                for (int i = 1; i <= columnCount; i++) {
                    result.put(columnLabels.get(i - 1), JdbcUtil.getColumnValue(rs, i));
                }

                return result;
            }
        };

        /**
         *
         * @param rs
         * @param columnLabels
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        T apply(ResultSet rs, List<String> columnLabels) throws SQLException;

        /**
         *
         * @param <T>
         * @param targetClass
         * @return
         */
        static <T> BiRowMapper<T> to(Class<? extends T> targetClass) {
            return to(targetClass, false);
        }

        /**
         * Don't cache or reuse the returned {@code BiRowMapper} instance.
         *
         * @param <T>
         * @param targetClass
         * @param ignoreNonMatchedColumns
         * @return
         */
        static <T> BiRowMapper<T> to(Class<? extends T> targetClass, final boolean ignoreNonMatchedColumns) {
            if (Object[].class.isAssignableFrom(targetClass)) {
                return new BiRowMapper<T>() {
                    @Override
                    public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                        final int columnCount = columnLabelList.size();
                        final Object[] a = Array.newInstance(targetClass.getComponentType(), columnCount);

                        for (int i = 0; i < columnCount; i++) {
                            a[i] = JdbcUtil.getColumnValue(rs, i + 1);
                        }

                        return (T) a;
                    }
                };
            } else if (List.class.isAssignableFrom(targetClass)) {
                return new BiRowMapper<T>() {
                    private boolean isListOrArrayList = targetClass.equals(List.class) || targetClass.equals(ArrayList.class);

                    @Override
                    public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                        final int columnCount = columnLabelList.size();
                        final List<Object> c = isListOrArrayList ? new ArrayList<>(columnCount) : (List<Object>) N.newInstance(targetClass);

                        for (int i = 0; i < columnCount; i++) {
                            c.add(JdbcUtil.getColumnValue(rs, i + 1));
                        }

                        return (T) c;
                    }
                };
            } else if (Map.class.isAssignableFrom(targetClass)) {
                return new BiRowMapper<T>() {
                    private boolean isMapOrHashMap = targetClass.equals(Map.class) || targetClass.equals(HashMap.class);
                    private boolean isLinkedHashMap = targetClass.equals(LinkedHashMap.class);

                    @Override
                    public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                        final int columnCount = columnLabelList.size();
                        final Map<String, Object> m = isMapOrHashMap ? new HashMap<>(columnCount)
                                : (isLinkedHashMap ? new LinkedHashMap<>(columnCount) : (Map<String, Object>) N.newInstance(targetClass));

                        for (int i = 0; i < columnCount; i++) {
                            m.put(columnLabelList.get(i), JdbcUtil.getColumnValue(rs, i + 1));
                        }

                        return (T) m;
                    }
                };
            } else if (ClassUtil.isEntity(targetClass)) {
                return new BiRowMapper<T>() {
                    private boolean isDirtyMarker = DirtyMarkerUtil.isDirtyMarker(targetClass);
                    private EntityInfo entityInfo = ParserUtil.getEntityInfo(targetClass);
                    private volatile String[] columnLabels = null;
                    private volatile PropInfo[] propInfos;
                    private volatile Type<?>[] columnTypes = null;

                    @Override
                    public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                        final int columnCount = columnLabelList.size();

                        String[] columnLabels = this.columnLabels;
                        PropInfo[] propInfos = this.propInfos;
                        Type<?>[] columnTypes = this.columnTypes;

                        if (columnLabels == null) {
                            columnLabels = columnLabelList.toArray(new String[columnLabelList.size()]);
                            this.columnLabels = columnLabels;
                        }

                        if (columnTypes == null || propInfos == null) {
                            final Map<String, String> column2FieldNameMap = getColumn2FieldNameMap(targetClass);

                            propInfos = new PropInfo[columnCount];
                            columnTypes = new Type[columnCount];

                            for (int i = 0; i < columnCount; i++) {
                                propInfos[i] = entityInfo.getPropInfo(columnLabels[i]);

                                if (propInfos[i] == null) {
                                    String fieldName = column2FieldNameMap.get(columnLabels[i]);

                                    if (N.isNullOrEmpty(fieldName)) {
                                        fieldName = column2FieldNameMap.get(columnLabels[i].toLowerCase());
                                    }

                                    if (N.notNullOrEmpty(fieldName)) {
                                        propInfos[i] = entityInfo.getPropInfo(fieldName);
                                    }
                                }

                                if (propInfos[i] == null) {
                                    if (ignoreNonMatchedColumns) {
                                        columnLabels[i] = null;
                                    } else {
                                        throw new IllegalArgumentException("No property in class: " + ClassUtil.getCanonicalClassName(targetClass)
                                                + " mapping to column: " + columnLabels[i]);
                                    }
                                } else {
                                    columnTypes[i] = entityInfo.getPropInfo(columnLabels[i]).dbType;
                                }
                            }

                            this.propInfos = propInfos;
                            this.columnTypes = columnTypes;
                        }

                        final Object entity = N.newInstance(targetClass);

                        for (int i = 0; i < columnCount; i++) {
                            if (columnLabels[i] == null) {
                                continue;
                            }

                            propInfos[i].setPropValue(entity, columnTypes[i].get(rs, i + 1));
                        }

                        if (isDirtyMarker) {
                            DirtyMarkerUtil.markDirty((DirtyMarker) entity, false);
                        }

                        return (T) entity;
                    }
                };
            } else {
                return new BiRowMapper<T>() {
                    private Type<? extends T> targetType = N.typeOf(targetClass);
                    private int columnCount = 0;

                    @Override
                    public T apply(ResultSet rs, List<String> columnLabelList) throws SQLException {
                        if (columnCount != 1 && (columnCount = columnLabelList.size()) != 1) {
                            throw new IllegalArgumentException(
                                    "It's not supported to retrieve value from multiple columns: " + columnLabelList + " for type: " + targetClass);
                        }

                        return targetType.get(rs, 1);
                    }
                };
            }
        }
    }

    /**
     * Don't use {@code RowConsumer} in {@link PreparedQuery#forEach(RowConsumer)} or any place where multiple records will be consumed by it, if column labels/count are used in {@link RowConsumer#accept(ResultSet)}.
     * Consider using {@code BiRowConsumer} instead because it's more efficient to consume multiple records when column labels/count are used.
     *
     */
    public interface RowConsumer extends Try.Consumer<ResultSet, SQLException> {

        /**
         *
         * @param rs
         * @throws SQLException the SQL exception
         */
        @Override
        void accept(ResultSet rs) throws SQLException;
    }

    /**
     * The Interface BiRowConsumer.
     */
    public interface BiRowConsumer extends Try.BiConsumer<ResultSet, List<String>, SQLException> {

        /**
         *
         * @param rs
         * @param columnLabels
         * @throws SQLException the SQL exception
         */
        @Override
        void accept(ResultSet rs, List<String> columnLabels) throws SQLException;
    }

    /**
     * Don't use {@code RowFilter} in {@link PreparedQuery#list(RowFilter, RowMapper)}, {@link PreparedQuery#forEach(RowFilter, RowConsumer)}  or any place where multiple records will be tested by it, if column labels/count are used in {@link RowFilter#test(ResultSet)}.
     * Consider using {@code BiRowConsumer} instead because it's more efficient to test multiple records when column labels/count are used.
     *
     */
    public interface RowFilter extends Try.Predicate<ResultSet, SQLException> {

        /** The Constant ALWAYS_TRUE. */
        RowFilter ALWAYS_TRUE = new RowFilter() {
            @Override
            public boolean test(ResultSet rs) throws SQLException, RuntimeException {
                return true;
            }
        };

        /** The Constant ALWAYS_FALSE. */
        RowFilter ALWAYS_FALSE = new RowFilter() {
            @Override
            public boolean test(ResultSet rs) throws SQLException, RuntimeException {
                return false;
            }
        };

        /**
         *
         * @param rs
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        boolean test(ResultSet rs) throws SQLException;
    }

    /**
     * The Interface BiRowFilter.
     */
    public interface BiRowFilter extends Try.BiPredicate<ResultSet, List<String>, SQLException> {

        /** The Constant ALWAYS_TRUE. */
        BiRowFilter ALWAYS_TRUE = new BiRowFilter() {
            @Override
            public boolean test(ResultSet rs, List<String> columnLabels) throws SQLException, RuntimeException {
                return true;
            }
        };

        /** The Constant ALWAYS_FALSE. */
        BiRowFilter ALWAYS_FALSE = new BiRowFilter() {
            @Override
            public boolean test(ResultSet rs, List<String> columnLabels) throws SQLException, RuntimeException {
                return false;
            }
        };

        /**
         *
         * @param rs
         * @param columnLabels
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        @Override
        boolean test(ResultSet rs, List<String> columnLabels) throws SQLException;
    }

    /** The Constant daoPool. */
    private static final Map<Class<?>, JdbcUtil.Dao> daoPool = new ConcurrentHashMap<>();

    /**
     * This interface is designed to share/manager SQL queries by Java APIs/methods with static parameter types and return type, while hiding the SQL scripts.
     * It's a gift from nature and created by thoughts.
     *
     * <br />
     * Note: Setting parameters by 'ParametersSetter' or Retrieving result/record by 'ResultExtractor/BiResultExtractor/RowMapper/BiRowMapper' is disabled.
     *
     * <br />
     *
     * <li>The SQL operations/methods should be annotated with SQL scripts by {@code @Select/@Insert/@Update/@Delete/@NamedSelect/@NamedInsert/@NamedUpdate/@NamedDelete}.</li>
     *
     * <li>The Order of the parameters in the method should be consistent with parameter order in SQL scripts for parameterized SQL.
     * For named parameterized SQL, the parameters must be binded with names through {@code @Bind}, or {@code Map/Entity} with getter/setter methods.</li>
     *
     * <li>SQL parameters can be set through input method parameters(by multiple parameters or a {@code Collection}, or a {@code Map/Entity} for named sql), or by {@code JdbcUtil.ParametersSetter<PreparedQuery/PreparedCallabeQuery...>}.</li>
     *
     * <li>{@code ResultExtractor/BiResultExtractor/RowMapper/BiRowMapper} can be specified by the last parameter of the method.</li>
     *
     * <li>The return type of the method must be same as the return type of {@code ResultExtractor/BiResultExtractor} if it's specified by the last parameter of the method.</li>
     *
     * <li>The return type of update/delete operations only can int/Integer/long/Long/boolean/Boolean/void. If it's long/Long, {@code PreparedQuery#largeUpdate()} will be called,
     * otherwise, {@code PreparedQuery#update()} will be called.</li>
     *
     * <li>Remember declaring {@code throws SQLException} in the method.</li>
     * <br />
     * <li>Which underline {@code PreparedQuery/PreparedCallableQuery} method to call for SQL methods/operations annotated with {@code @Select/@NamedSelect}:
     * <ul>
     *   <li>If {@code ResultExtractor/BiResultExtractor} is specified by the last parameter of the method, {@code PreparedQuery#query(ResultExtractor/BiResultExtractor)} will be called.</li>
     *   <li>Or else if {@code RowMapper/BiRowMapper} is specified by the last parameter of the method:</li>
     *      <ul>
     *          <li>If the return type of the method is {@code List} and one of below conditions is matched, {@code PreparedQuery#list(RowMapper/BiRowMapper)} will be called:</li>
     *          <ul>
     *              <li>The return type of the method is raw {@code List} without parameterized type, and the method name doesn't start with {@code "get"/"findFirst"/"findOne"}.</li>
     *          </ul>
     *          <ul>
     *              <li>The last parameter type is raw {@code RowMapper/BiRowMapper} without parameterized type, and the method name doesn't start with {@code "get"/"findFirst"/"findOne"}.</li>
     *          </ul>
     *          <ul>
     *              <li>The return type of the method is generic {@code List} with parameterized type and The last parameter type is generic {@code RowMapper/BiRowMapper} with parameterized types, but They're not same.</li>
     *          </ul>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code ExceptionalStream/Stream}, {@code PreparedQuery#stream(RowMapper/BiRowMapper)} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code Optional}, {@code PreparedQuery#findFirst(RowMapper/BiRowMapper)} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else, {@code PreparedQuery#findFirst(RowMapper/BiRowMapper).orElse(N.defaultValueOf(returnType))} will be called.</li>
     *      </ul>
     *   <li>Or else:</li>
     *      <ul>
     *          <li>If the return type of the method is {@code DataSet}, {@code PreparedQuery#query()} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code ExceptionalStream/Stream}, {@code PreparedQuery#stream(Class)} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code Map} or {@code Entity} class with {@code getter/setter} methods, {@code PreparedQuery#findFirst(Class).orNull()} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code Optional}:</li>
     *          <ul>
     *              <li>If the value type of {@code Optional} is {@code Map}, or {@code Entity} class with {@code getter/setter} methods, or {@code List}, or {@code Object[]}, {@code PreparedQuery#findFirst(Class)} will be called.</li>
     *          </ul>
     *          <ul>
     *              <li>Or else, {@code PreparedQuery#queryForSingleNonNull(Class)} will be called.</li>
     *          </ul>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code Nullable}:</li>
     *          <ul>
     *              <li>If the value type of {@code Nullable} is {@code Map}, or {@code Entity} class with {@code getter/setter} methods, or {@code List}, or {@code Object[]}, {@code PreparedQuery#findFirst(Class)} will be called.</li>
     *          </ul>
     *          <ul>
     *              <li>Or else, {@code PreparedQuery#queryForSingleResult(Class)} will be called.</li>
     *          </ul>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code OptionalBoolean/Byte/.../Double}, {@code PreparedQuery#queryForBoolean/Byte/...Double()} will called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else if the return type of the method is {@code List}, and the method name doesn't start with {@code "get"/"findFirst"/"findOne"}, {@code PreparedQuery#list(Class)} will be called.</li>
     *      </ul>
     *      <ul>
     *          <li>Or else, {@code PreparedQuery#queryForSingleResult(Class).orElse(N.defaultValueOf(returnType)} will be called.</li>
     *      </ul>
     * </ul>
     *
     * <br />
     * <br />
     *
     * Here is a simple {@code UserDao} sample.
     *
     * <pre>
     * <code>
     * public static interface UserDao extends JdbcUtil.CrudDao<User, Long, SQLBuilder.PSC> {
     *     &#064NamedInsert("INSERT INTO user (id, first_name, last_name, email) VALUES (:id, :firstName, :lastName, :email)")
     *     void insertWithId(User user) throws SQLException;
     *
     *     &#064NamedUpdate("UPDATE user SET first_name = :firstName, last_name = :lastName WHERE id = :id")
     *     int updateFirstAndLastName(@Bind("firstName") String newFirstName, @Bind("lastName") String newLastName, @Bind("id") long id) throws SQLException;
     *
     *     &#064NamedSelect("SELECT first_name, last_name FROM user WHERE id = :id")
     *     User getFirstAndLastNameBy(@Bind("id") long id) throws SQLException;
     *
     *     &#064NamedSelect("SELECT id, first_name, last_name, email FROM user")
     *     Stream<User> allUsers() throws SQLException;
     * }
     * </code>
     * </pre>
     *
     * Here is the generate way to work with transaction started by {@code SQLExecutor}.
     *
     * <pre>
     * <code>
     * static final UserDao userDao = Dao.newInstance(UserDao.class, dataSource);
     * ...
     *
     * final SQLTransaction tran = JdbcUtil.beginTransaction(dataSource, IsolationLevel.READ_COMMITTED);
     *
     * try {
     *      userDao.getById(id);
     *      userDao.update(...);
     *      // more...
     *
     *      tran.commit();
     * } finally {
     *      // The connection will be automatically closed after the transaction is committed or rolled back.
     *      tran.rollbackIfNotCommitted();
     * }
     * </code>
     * </pre>
     *
     * @see JdbcUtil#prepareQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#prepareNamedQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)
     * @see BasicDao
     * @see CrudDao
     * @see SQLExecutor.Mapper
     */
    public interface Dao {

        /**
         * The Interface Select.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface Select {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql()

            default "";

            /**
             *
             * @return
             */
            int fetchSize() default -1;

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface Insert.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface Insert {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface Update.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface Update {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface Delete.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface Delete {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface NamedSelect.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface NamedSelect {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql()

            default "";

            /**
             *
             * @return
             */
            int fetchSize() default -1;

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface NamedInsert.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface NamedInsert {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface NamedUpdate.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface NamedUpdate {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface NamedDelete.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface NamedDelete {

            /**
             *
             * @return
             * @deprecated using sql="SELECT ... FROM ..." for explicit call.
             */
            @Deprecated
            String value()

            default "";

            /**
             *
             * @return
             */
            String sql() default "";

            /**
             * Unit is seconds.
             *
             * @return
             */
            int queryTimeout() default -1;
        }

        /**
         * The Interface Bind.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        public static @interface Bind {

            /**
             *
             * @return
             */
            String value() default "";
        }

        /**
         *
         * @return
         */
        javax.sql.DataSource dataSource();

        //    /**
        //     *
        //     * @param isolationLevel
        //     * @return
        //     * @throws UncheckedSQLException
        //     */
        //    default SQLTransaction beginTransaction(final IsolationLevel isolationLevel) throws UncheckedSQLException {
        //        return beginTransaction(isolationLevel, false);
        //    }
        //
        //    /**
        //     * The connection opened in the transaction will be automatically closed after the transaction is committed or rolled back.
        //     * DON'T close it again by calling the close method.
        //     * <br />
        //     * <br />
        //     * The transaction will be shared cross the instances of {@code SQLExecutor/Dao} by the methods called in the same thread with same {@code DataSource}.
        //     *
        //     * <br />
        //     * <br />
        //     *
        //     * The general programming way with SQLExecutor/Dao is to execute sql scripts(generated by SQLBuilder) with array/list/map/entity by calling (batch)insert/update/delete/query/... methods.
        //     * If Transaction is required, it can be started:
        //     *
        //     * <pre>
        //     * <code>
        //     *   final SQLTransaction tran = someDao.beginTransaction(IsolationLevel.READ_COMMITTED);
        //     *   try {
        //     *       // sqlExecutor.insert(...);
        //     *       // sqlExecutor.update(...);
        //     *       // sqlExecutor.query(...);
        //     *
        //     *       tran.commit();
        //     *   } finally {
        //     *       // The connection will be automatically closed after the transaction is committed or rolled back.
        //     *       tran.rollbackIfNotCommitted();
        //     *   }
        //     * </code>
        //     * </pre>
        //     *
        //     * @param isolationLevel
        //     * @param forUpdateOnly
        //     * @return
        //     * @throws UncheckedSQLException
        //     * @see {@link SQLExecutor#beginTransaction(IsolationLevel, boolean)}
        //     */
        //    default SQLTransaction beginTransaction(final IsolationLevel isolationLevel, final boolean forUpdateOnly) throws UncheckedSQLException {
        //        N.checkArgNotNull(isolationLevel, "isolationLevel");
        //
        //        final javax.sql.DataSource ds = dataSource();
        //        SQLTransaction tran = SQLTransaction.getTransaction(ds);
        //
        //        if (tran == null) {
        //            Connection conn = null;
        //            boolean noException = false;
        //
        //            try {
        //                conn = getConnection(ds);
        //                tran = new SQLTransaction(ds, conn, isolationLevel, true, true);
        //                tran.incrementAndGetRef(isolationLevel, forUpdateOnly);
        //
        //                noException = true;
        //            } catch (SQLException e) {
        //                throw new UncheckedSQLException(e);
        //            } finally {
        //                if (noException == false) {
        //                    JdbcUtil.releaseConnection(conn, ds);
        //                }
        //            }
        //
        //            logger.info("Create a new SQLTransaction(id={})", tran.id());
        //            SQLTransaction.putTransaction(ds, tran);
        //        } else {
        //            logger.info("Reusing the existing SQLTransaction(id={})", tran.id());
        //            tran.incrementAndGetRef(isolationLevel, forUpdateOnly);
        //        }
        //
        //        return tran;
        //    }

        /**
         *
         * @param query
         * @return
         * @throws SQLException
         */
        default PreparedQuery prepareQuery(final String query) throws SQLException {
            return JdbcUtil.prepareQuery(dataSource(), query);
        }

        /**
         *
         * @param query
         * @param generateKeys
         * @return
         * @throws SQLException
         */
        default PreparedQuery prepareQuery(final String query, final boolean generateKeys) throws SQLException {
            return JdbcUtil.prepareQuery(dataSource(), query, generateKeys);
        }

        /**
         *
         * @param sql
         * @param stmtCreator
         * @return
         * @throws SQLException
         */
        default PreparedQuery prepareQuery(final String sql, final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator)
                throws SQLException {
            return JdbcUtil.prepareQuery(dataSource(), sql, stmtCreator);
        }

        /**
         *
         * @param namedQuery
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final String namedQuery) throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedQuery);
        }

        /**
         *
         * @param namedQuery
         * @param generateKeys
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final String namedQuery, final boolean generateKeys) throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedQuery, generateKeys);
        }

        /**
         *
         * @param namedQuery
         * @param stmtCreator
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final String namedQuery, final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator)
                throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedQuery, stmtCreator);
        }

        /**
         *
         * @param namedSQL the named query
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final NamedSQL namedSQL) throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedSQL);
        }

        /**
         *
         * @param namedSQL the named query
         * @param generateKeys
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final NamedSQL namedSQL, final boolean generateKeys) throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedSQL, generateKeys);
        }

        /**
         *
         * @param namedSQL the named query
         * @param stmtCreator
         * @return
         * @throws SQLException
         */
        default NamedQuery prepareNamedQuery(final NamedSQL namedSQL, final Try.BiFunction<Connection, String, PreparedStatement, SQLException> stmtCreator)
                throws SQLException {
            return JdbcUtil.prepareNamedQuery(dataSource(), namedSQL, stmtCreator);
        }

        /**
         *
         * @param <T>
         * @param daoInterface
         * @param ds
         * @return
         * @see SQLExecutor#createDao(Class)
         */
        static <T extends Dao> T newInstance(final Class<T> daoInterface, final javax.sql.DataSource ds) {
            N.checkArgNotNull(daoInterface, "daoInterface");
            N.checkArgNotNull(ds, "dataSource");

            T dao = (T) daoPool.get(daoInterface);

            if (dao == null) {
                dao = JdbcUtil.newInstance(daoInterface, ds);
                daoPool.put(daoInterface, dao);
            }

            return dao;
        }
    }

    /**
     * The Interface BasicDao.
     *
     * @param <T>
     * @param <SB> {@code SQLBuilder} used to generate sql scripts. Only can be {@code SQLBulider.PSC/PAC/PLC}
     * @see JdbcUtil#prepareQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#prepareNamedQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)
     * @see Dao
     * @See CrudDao
     * @see SQLExecutor.Mapper
     */
    public interface BasicDao<T, SB extends SQLBuilder> extends Dao {

        /**
         *
         * @param entityToSave
         * @return
         * @throws SQLException the SQL exception
         */
        void save(final T entityToSave) throws SQLException;

        /**
         * Insert the specified entities to database by batch.
         *
         * @param entitiesToSave
         * @return
         * @throws SQLException the SQL exception
         * @see CrudDao#batchInsert(Collection)
         */
        default void saveAll(final Collection<? extends T> entitiesToSave) throws SQLException {
            saveAll(entitiesToSave, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         * Insert the specified entities to database by batch.
         *
         * @param entitiesToSave
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         * @see CrudDao#batchInsert(Collection)
         */
        void saveAll(final Collection<? extends T> entitiesToSave, final int batchSize) throws SQLException;

        /**
         * Insert the specified entities to database by batch.
         *
         * @param namedInsertSQL
         * @param entitiesToSave
         * @return
         * @throws SQLException the SQL exception
         * @see CrudDao#batchInsert(Collection)
         */
        @Beta
        default void saveAll(final String namedInsertSQL, final Collection<? extends T> entitiesToSave) throws SQLException {
            saveAll(namedInsertSQL, entitiesToSave, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         * Insert the specified entities to database by batch.
         *
         * @param namedInsertSQL
         * @param entitiesToSave
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         * @see CrudDao#batchInsert(Collection)
         */
        @Beta
        void saveAll(final String namedInsertSQL, final Collection<? extends T> entitiesToSave, final int batchSize) throws SQLException;

        /**
         *
         * @param cond
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        boolean exists(Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        int count(Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Optional<T> findFirst(Condition cond) throws SQLException;

        /**
         *
         * @param selectPropNames
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Optional<T> findFirst(Collection<String> selectPropNames, Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        List<T> list(Condition cond) throws SQLException;

        /**
         *
         * @param selectPropNames
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        List<T> list(Collection<String> selectPropNames, Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        DataSet query(Condition cond) throws SQLException;

        /**
         *
         * @param selectPropNames
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        DataSet query(Collection<String> selectPropNames, Condition cond) throws SQLException;

        /**
         * Query for boolean.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalBoolean queryForBoolean(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for char.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalChar queryForChar(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for byte.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalByte queryForByte(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for short.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalShort queryForShort(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for int.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalInt queryForInt(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for long.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalLong queryForLong(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for float.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalFloat queryForFloat(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for double.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        OptionalDouble queryForDouble(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for string.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Nullable<String> queryForString(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for date.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Nullable<java.sql.Date> queryForDate(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for time.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Nullable<java.sql.Time> queryForTime(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for timestamp.
         *
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        Nullable<java.sql.Timestamp> queryForTimestamp(final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for single result.
         *
         * @param <V> the value type
         * @param targetValueClass
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        <V> Nullable<V> queryForSingleResult(final Class<V> targetValueClass, final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for single non null.
         *
         * @param <V> the value type
         * @param targetValueClass
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        <V> Optional<V> queryForSingleNonNull(final Class<V> targetValueClass, final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for unique result.
         *
         * @param <V> the value type
         * @param targetValueClass
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        <V> Nullable<V> queryForUniqueResult(final Class<V> targetValueClass, final String selectPropName, final Condition cond) throws SQLException;

        /**
         * Query for unique non null.
         *
         * @param <V> the value type
         * @param targetValueClass
         * @param selectPropName
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        <V> Optional<V> queryForUniqueNonNull(final Class<V> targetValueClass, final String selectPropName, final Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        ExceptionalStream<T, SQLException> stream(Condition cond) throws SQLException;

        /**
         *
         * @param selectPropNames
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        ExceptionalStream<T, SQLException> stream(Collection<String> selectPropNames, Condition cond) throws SQLException;

        /**
         *
         * @param updateProps
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        int update(Map<String, Object> updateProps, Condition cond) throws SQLException;

        /**
         *
         * @param cond
         * @return
         * @throws SQLException the SQL exception
         */
        int delete(Condition cond) throws SQLException;
    }

    /**
     * The Interface CrudDao.
     *
     * @param <T>
     * @param <ID> use {@code Void} if there is no id defined/annotated with {@code @Id} in target entity class {@code T}.
     * @param <SB> {@code SQLBuilder} used to generate sql scripts. Only can be {@code SQLBulider.PSC/PAC/PLC}
     * @see JdbcUtil#prepareQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#prepareNamedQuery(javax.sql.DataSource, String)
     * @see JdbcUtil#beginTransaction(javax.sql.DataSource, IsolationLevel, boolean)
     * @see Dao
     * @see BasicDao
     * @see SQLExecutor.Mapper
     */
    public interface CrudDao<T, ID, SB extends SQLBuilder> extends BasicDao<T, SB> {

        /**
         *
         * @param entityToSave
         * @return
         * @throws SQLException the SQL exception
         */
        ID insert(final T entityToSave) throws SQLException;

        /**
         *
         * @param entities
         * @return
         * @throws SQLException the SQL exception
         */
        default List<ID> batchInsert(final Collection<? extends T> entities) throws SQLException {
            return batchInsert(entities, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         *
         * @param entities
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         */
        List<ID> batchInsert(final Collection<? extends T> entities, final int batchSize) throws SQLException;

        /**
         *
         * @param namedInsertSQL
         * @param entities
         * @return
         * @throws SQLException the SQL exception
         */
        @Beta
        default List<ID> batchInsert(final String namedInsertSQL, final Collection<? extends T> entities) throws SQLException {
            return batchInsert(namedInsertSQL, entities, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         *
         * @param namedInsertSQL
         * @param entities
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         */
        @Beta
        List<ID> batchInsert(final String namedInsertSQL, final Collection<? extends T> entities, final int batchSize) throws SQLException;

        /**
         *
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        Optional<T> get(ID id) throws SQLException;

        /**
         *
         * @param selectPropNames
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        Optional<T> get(Collection<String> selectPropNames, ID id) throws SQLException;

        /**
         * Gets the t.
         *
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        T gett(ID id) throws SQLException;

        /**
         * Gets the t.
         *
         * @param selectPropNames
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        T gett(Collection<String> selectPropNames, ID id) throws SQLException;

        /**
         *
         * @param id
         * @return true, if successful
         * @throws SQLException the SQL exception
         */
        boolean exists(ID id) throws SQLException;

        /**
         *
         * @param entityToUpdate
         * @return
         * @throws SQLException the SQL exception
         */
        int update(T entityToUpdate) throws SQLException;

        /**
         *
         * @param entityToUpdate
         * @param propNamesToUpdate
         * @return
         * @throws SQLException the SQL exception
         */
        int update(T entityToUpdate, Collection<String> propNamesToUpdate) throws SQLException;

        /**
         *
         * @param updateProps
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        int update(Map<String, Object> updateProps, ID id) throws SQLException;

        /**
         *
         * @param entities
         * @return
         * @throws SQLException the SQL exception
         */
        default int batchUpdate(Collection<? extends T> entities) throws SQLException {
            return batchUpdate(entities, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         *
         * @param entities
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         */
        int batchUpdate(Collection<? extends T> entities, int batchSize) throws SQLException;

        /**
         *
         * @param entities
         * @param propNamesToUpdate
         * @return
         * @throws SQLException the SQL exception
         */
        default int batchUpdate(Collection<? extends T> entities, Collection<String> propNamesToUpdate) throws SQLException {
            return batchUpdate(entities, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
         *
         * @param entities
         * @param propNamesToUpdate
         * @param batchSize
         * @return
         * @throws SQLException the SQL exception
         */
        int batchUpdate(Collection<? extends T> entities, Collection<String> propNamesToUpdate, int batchSize) throws SQLException;

        /**
         * Delete by id.
         *
         * @param id
         * @return
         * @throws SQLException the SQL exception
         */
        int deleteById(ID id) throws SQLException;

        /**
         *
         * @param entity
         * @return
         * @throws SQLException the SQL exception
         */
        int delete(T entity) throws SQLException;

        /**
         *
         * @param entities
         * @return
         * @throws SQLException the SQL exception
         */
        default int batchDelete(Collection<? extends T> entities) throws SQLException {
            return batchDelete(entities, JdbcSettings.DEFAULT_BATCH_SIZE);
        }

        /**
        *
        * @param entities
        * @param batchSize
        * @return
        * @throws SQLException the SQL exception
        */
        int batchDelete(Collection<? extends T> entities, int batchSize) throws SQLException;
    }

    /**
     *
     * @param <T>
     * @param daoInterface
     * @param ds
     * @return
     */
    @SuppressWarnings({ "rawtypes", "deprecation" })
    static <T extends Dao> T newInstance(final Class<T> daoInterface, final javax.sql.DataSource ds) {
        N.checkArgNotNull(daoInterface, "daoInterface");
        N.checkArgNotNull(ds, "dataSource");

        java.lang.reflect.Type[] typeArguments = null;

        if (BasicDao.class.isAssignableFrom(daoInterface)) {
            if (N.notNullOrEmpty(daoInterface.getGenericInterfaces()) && daoInterface.getGenericInterfaces()[0] instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) daoInterface.getGenericInterfaces()[0];
                typeArguments = parameterizedType.getActualTypeArguments();

                if (typeArguments.length >= 1 && typeArguments[0] instanceof Class) {
                    if (!ClassUtil.isEntity((Class) typeArguments[0])) {
                        throw new IllegalArgumentException(
                                "Entity Type parameter must be: Object.class or entity class with getter/setter methods. Can't be: " + typeArguments[0]);
                    }
                }

                if (typeArguments.length >= 2 && SQLBuilder.class.isAssignableFrom((Class) typeArguments[1])) {
                    if (!(typeArguments[1].equals(PSC.class) || typeArguments[1].equals(PAC.class) || typeArguments[1].equals(PLC.class))) {
                        throw new IllegalArgumentException("SQLBuilder Type parameter must be: SQLBuilder.PSC/PAC/PLC. Can't be: " + typeArguments[1]);
                    }
                } else if (typeArguments.length >= 3 && SQLBuilder.class.isAssignableFrom((Class) typeArguments[2])) {
                    if (!(typeArguments[2].equals(PSC.class) || typeArguments[2].equals(PAC.class) || typeArguments[2].equals(PLC.class))) {
                        throw new IllegalArgumentException("SQLBuilder Type parameter must be: SQLBuilder.PSC/PAC/PLC. Can't be: " + typeArguments[2]);
                    }
                }

                if (CrudDao.class.isAssignableFrom(daoInterface)) {
                    final List<String> idFieldNames = ClassUtil.getIdFieldNames((Class) typeArguments[0]);

                    if (idFieldNames.size() != 1) {
                        throw new IllegalArgumentException("To support CRUD operations by extending CrudDao interface, the entity class: " + typeArguments[0]
                                + " must have one and only one field annotated with @Id");
                    }

                    if (typeArguments.length >= 2 && typeArguments[1].equals(EntityId.class)) {
                        throw new IllegalArgumentException("EntityId for id is not supported yet");
                    } else if (((Class) typeArguments[1])
                            .isAssignableFrom(ClassUtil.getPropGetMethod((Class) typeArguments[0], idFieldNames.get(0)).getReturnType())) {
                        throw new IllegalArgumentException("The id type declared in Dao type parameters: " + typeArguments[1]
                                + " is not assignable from the id property type in the entity class: "
                                + ClassUtil.getPropGetMethod((Class) typeArguments[0], idFieldNames.get(0)).getReturnType());
                    }
                }
            }
        }

        final Map<Method, Try.BiFunction<Dao, Object[], ?, Exception>> methodInvokerMap = new HashMap<>();

        final List<Method> sqlMethods = StreamEx.of(ClassUtil.getAllInterfaces(daoInterface))
                .append(daoInterface)
                .distinct()
                .flatMapp(clazz -> clazz.getDeclaredMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .toList();

        final Class<?> entityClass = N.isNullOrEmpty(typeArguments) ? null : (Class) typeArguments[0];

        final Class<?> sbc = N.isNullOrEmpty(typeArguments) ? null
                : (typeArguments.length >= 2 && SQLBuilder.class.isAssignableFrom((Class) typeArguments[1]) ? (Class) typeArguments[1]
                        : (typeArguments.length >= 3 && SQLBuilder.class.isAssignableFrom((Class) typeArguments[2]) ? (Class) typeArguments[2] : null));

        final Function<String, SQLBuilder> parameterizedSelectFunc = sbc == null ? null
                : (sbc.equals(PSC.class) ? selectPropName -> PSC.select(selectPropName)
                        : (sbc.equals(PAC.class) ? selectPropName -> PAC.select(selectPropName) : selectPropName -> PLC.select(selectPropName)));

        final Function<Collection<String>, SQLBuilder> parameterizedSelectFunc2 = sbc == null ? null
                : (sbc.equals(PSC.class) ? selectPropNames -> PSC.select(selectPropNames)
                        : (sbc.equals(PAC.class) ? selectPropNames -> PAC.select(selectPropNames) : selectPropNames -> PLC.select(selectPropNames)));

        final Function<Class<?>, SQLBuilder> parameterizedSelectFromFunc = sbc == null ? null
                : (sbc.equals(PSC.class) ? clazz -> PSC.selectFrom(clazz)
                        : (sbc.equals(PAC.class) ? clazz -> PAC.selectFrom(clazz) : clazz -> PLC.selectFrom(clazz)));

        final Function<Class<?>, SQLBuilder> parameterizedUpdateFunc = sbc == null ? null
                : (sbc.equals(PSC.class) ? clazz -> PSC.update(clazz) : (sbc.equals(PAC.class) ? clazz -> PAC.update(clazz) : clazz -> PLC.update(clazz)));

        final Function<Class<?>, SQLBuilder> parameterizedDeleteFromFunc = sbc == null ? null
                : (sbc.equals(PSC.class) ? clazz -> PSC.deleteFrom(clazz)
                        : (sbc.equals(PAC.class) ? clazz -> PAC.deleteFrom(clazz) : clazz -> PLC.deleteFrom(clazz)));

        final Function<Class<?>, SQLBuilder> namedUpdateFunc = sbc == null ? null
                : (sbc.equals(PSC.class) ? clazz -> NSC.update(clazz) : (sbc.equals(PAC.class) ? clazz -> NAC.update(clazz) : clazz -> NLC.update(clazz)));

        for (Method m : sqlMethods) {
            Try.BiFunction<Dao, Object[], ?, Exception> call = null;

            if (!Modifier.isAbstract(m.getModifiers())) {
                final MethodHandle methodHandle = createMethodHandle(m);

                call = (proxy, args) -> {
                    try {
                        return methodHandle.bindTo(proxy).invokeWithArguments(args);
                    } catch (Throwable e) {
                        throw new Exception(e);
                    }
                };
            } else {
                final Annotation sqlAnno = StreamEx.of(m.getAnnotations())
                        .filter(anno -> JdbcUtil.sqlAnnoMap.containsKey(anno.annotationType()))
                        .first()
                        .orNull();

                final Class<?>[] paramTypes = m.getParameterTypes();
                final Class<?> returnType = m.getReturnType();
                final int paramLen = paramTypes.length;

                if (m.getDeclaringClass().equals(BasicDao.class)) {
                    final List<String> idPropNames = ClassUtil.getIdFieldNames(entityClass, true);
                    final boolean isFakeId = ClassUtil.isFakeId(idPropNames);
                    final String idPropName = idPropNames.get(0);

                    String sql_insertWithId = null;
                    String sql_insertWithoutId = null;

                    if (sbc.equals(PSC.class)) {
                        sql_insertWithId = NSC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NSC.insertInto(entityClass, N.asSet(idPropName)).sql();
                    } else if (sbc.equals(PAC.class)) {
                        sql_insertWithId = NAC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NAC.insertInto(entityClass, N.asSet(idPropName)).sql();
                    } else {
                        sql_insertWithId = NLC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NLC.insertInto(entityClass, N.asSet(idPropName)).sql();
                    }

                    final NamedSQL insertWithIdSQL = NamedSQL.parse(sql_insertWithId);
                    final NamedSQL insertWithoutIdSQL = NamedSQL.parse(sql_insertWithoutId);

                    if (m.getName().equals("save") && paramLen == 1) {
                        call = (proxy, args) -> {
                            if (isFakeId) {
                                proxy.prepareNamedQuery(insertWithoutIdSQL).setParameters(args[0]).update();
                            } else if (JdbcUtil.isDefaultIdPropValue(ClassUtil.getPropValue(args[0], idPropName))) {
                                proxy.prepareNamedQuery(insertWithoutIdSQL, true)
                                        .setParameters(args[0])
                                        .insert()
                                        .ifPresent(id -> ClassUtil.setPropValue(args[0], idPropName, id));
                            } else {
                                proxy.prepareNamedQuery(insertWithIdSQL).setParameters(args[0]).update();
                            }

                            if (args[0] instanceof DirtyMarker) {
                                DirtyMarkerUtil.markDirty((DirtyMarker) args[0], false);
                            }

                            return null;
                        };
                    } else if (m.getName().equals("saveAll") && paramLen == 2 && int.class.equals(paramTypes[1])) {
                        call = (proxy, args) -> {
                            final Collection<?> entities = (Collection<Object>) args[0];
                            final int batchSize = (Integer) args[1];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            List<Object> ids = null;
                            final Object idPropValue = isFakeId || N.isNullOrEmpty(entities) ? null
                                    : ClassUtil.getPropValue(N.first(entities).get(), idPropName);
                            final boolean isDefaultIdPropValue = JdbcUtil.isDefaultIdPropValue(idPropValue);

                            if (entities.size() <= batchSize) {
                                if (isFakeId) {
                                    proxy.prepareNamedQuery(insertWithoutIdSQL).addBatchParameters(entities).batchUpdate();
                                } else if (isDefaultIdPropValue) {
                                    ids = proxy.prepareNamedQuery(insertWithoutIdSQL, true).addBatchParameters(entities).batchInsert();
                                } else {
                                    proxy.prepareNamedQuery(insertWithIdSQL).addBatchParameters(entities).batchUpdate();
                                }
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    if (isFakeId) {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(insertWithoutIdSQL).closeAfterExecution(false)) {
                                            ExceptionalStream.of(entities)
                                                    .splitToList(batchSize) //
                                                    .forEach(bp -> nameQuery.addBatchParameters(bp).batchUpdate());
                                        }
                                    } else if (isDefaultIdPropValue) {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(insertWithoutIdSQL, true).closeAfterExecution(false)) {
                                            ids = ExceptionalStream.of(entities)
                                                    .splitToList(batchSize)
                                                    .flattMap(bp -> nameQuery.addBatchParameters(bp).batchInsert())
                                                    .toList();
                                        }
                                    } else {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(insertWithIdSQL).closeAfterExecution(false)) {
                                            ExceptionalStream.of(entities)
                                                    .splitToList(batchSize) //
                                                    .forEach(bp -> nameQuery.addBatchParameters(bp).batchUpdate());
                                        }
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (N.notNullOrEmpty(ids) && N.notNullOrEmpty(entities) && ids.size() == N.size(entities)) {
                                int idx = 0;

                                for (Object e : entities) {
                                    ClassUtil.setPropValue(e, idPropName, ids.get(idx++));
                                }
                            }

                            if (N.first(entities).orNull() instanceof DirtyMarker) {
                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                                }
                            }

                            return null;
                        };
                    } else if (m.getName().equals("saveAll") && paramLen == 3 && int.class.equals(paramTypes[2])) {
                        call = (proxy, args) -> {
                            final String namedInsertSQL = (String) args[0];
                            final Collection<?> entities = (Collection<Object>) args[1];
                            final int batchSize = (Integer) args[2];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            List<Object> ids = null;

                            if (entities.size() <= batchSize) {
                                if (isFakeId) {
                                    proxy.prepareNamedQuery(namedInsertSQL).addBatchParameters(entities).batchUpdate();
                                } else {
                                    ids = proxy.prepareNamedQuery(namedInsertSQL, true).addBatchParameters(entities).batchInsert();
                                }
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    if (isFakeId) {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(namedInsertSQL).closeAfterExecution(false)) {
                                            ExceptionalStream.of(entities)
                                                    .splitToList(batchSize) //
                                                    .forEach(bp -> nameQuery.addBatchParameters(bp).batchUpdate());
                                        }
                                    } else {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(namedInsertSQL, true).closeAfterExecution(false)) {
                                            ids = ExceptionalStream.of(entities)
                                                    .splitToList(batchSize)
                                                    .flattMap(bp -> nameQuery.addBatchParameters(bp).batchInsert())
                                                    .toList();
                                        }
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (N.notNullOrEmpty(ids) && N.notNullOrEmpty(entities) && ids.size() == N.size(entities)) {
                                int idx = 0;

                                for (Object e : entities) {
                                    ClassUtil.setPropValue(e, idPropName, ids.get(idx++));
                                }
                            }

                            if (N.first(entities).orNull() instanceof DirtyMarker) {
                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                                }
                            }

                            return null;
                        };
                    } else if (m.getName().equals("exists") && paramLen == 1 && Condition.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply(SQLBuilder._1).from(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).exists();
                        };
                    } else if (m.getName().equals("count") && paramLen == 1 && Condition.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply(SQLBuilder.COUNT_ALL).from(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForInt().orZero();
                        };
                    } else if (m.getName().equals("findFirst") && paramLen == 1 && paramTypes[0].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFromFunc.apply(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).findFirst(entityClass);
                        };
                    } else if (m.getName().equals("findFirst") && paramLen == 2 && paramTypes[0].equals(Collection.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).findFirst(entityClass);
                        };
                    } else if (m.getName().equals("list") && paramLen == 1 && paramTypes[0].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFromFunc.apply(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).list(entityClass);
                        };
                    } else if (m.getName().equals("list") && paramLen == 2 && paramTypes[0].equals(Collection.class) && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).list(entityClass);
                        };
                    } else if (m.getName().equals("query") && paramLen == 1 && paramTypes[0].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFromFunc.apply(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).query();
                        };
                    } else if (m.getName().equals("query") && paramLen == 2 && paramTypes[0].equals(Collection.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).query();
                        };
                    } else if (m.getName().equals("queryForBoolean") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForBoolean();
                        };
                    } else if (m.getName().equals("queryForChar") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForChar();
                        };
                    } else if (m.getName().equals("queryForByte") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForByte();
                        };
                    } else if (m.getName().equals("queryForShort") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForShort();
                        };
                    } else if (m.getName().equals("queryForInt") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForInt();
                        };
                    } else if (m.getName().equals("queryForLong") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForLong();
                        };
                    } else if (m.getName().equals("queryForFloat") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForFloat();
                        };
                    } else if (m.getName().equals("queryForDouble") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForDouble();
                        };
                    } else if (m.getName().equals("queryForString") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForString();
                        };
                    } else if (m.getName().equals("queryForDate") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForDate();
                        };
                    } else if (m.getName().equals("queryForTime") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForTime();
                        };
                    } else if (m.getName().equals("queryForTimestamp") && paramLen == 2 && paramTypes[0].equals(String.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForTimestamp();
                        };
                    } else if (m.getName().equals("queryForSingleResult") && paramLen == 3 && paramTypes[0].equals(Class.class)
                            && paramTypes[1].equals(String.class) && paramTypes[2].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[1]).from(entityClass).where((Condition) args[2]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForSingleResult((Class) args[0]);
                        };
                    } else if (m.getName().equals("queryForSingleNonNull") && paramLen == 3 && paramTypes[0].equals(Class.class)
                            && paramTypes[1].equals(String.class) && paramTypes[2].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[1]).from(entityClass).where((Condition) args[2]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForSingleNonNull((Class) args[0]);
                        };
                    } else if (m.getName().equals("queryForUniqueResult") && paramLen == 3 && paramTypes[0].equals(Class.class)
                            && paramTypes[1].equals(String.class) && paramTypes[2].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[1]).from(entityClass).where((Condition) args[2]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForUniqueResult((Class) args[0]);
                        };
                    } else if (m.getName().equals("queryForUniqueNonNull") && paramLen == 3 && paramTypes[0].equals(Class.class)
                            && paramTypes[1].equals(String.class) && paramTypes[2].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc.apply((String) args[1]).from(entityClass).where((Condition) args[2]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).queryForUniqueNonNull((Class) args[0]);
                        };
                    } else if (m.getName().equals("stream") && paramLen == 1 && paramTypes[0].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFromFunc.apply(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).stream(entityClass);
                        };
                    } else if (m.getName().equals("stream") && paramLen == 2 && paramTypes[0].equals(Collection.class)
                            && paramTypes[1].equals(Condition.class)) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where((Condition) args[1]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).stream(entityClass);
                        };
                    } else if (m.getName().equals("update") && paramLen == 2 && Map.class.equals(paramTypes[0])
                            && Condition.class.isAssignableFrom(m.getParameterTypes()[1])) {
                        call = (proxy, args) -> {
                            final Map<String, Object> props = (Map<String, Object>) args[0];
                            final Condition cond = (Condition) args[1];
                            N.checkArgNotNullOrEmpty(props, "updateProps");

                            final SP sp = parameterizedUpdateFunc.apply(entityClass).set(props).where(cond).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).update();
                        };
                    } else if (m.getName().equals("delete") && paramLen == 1 && Condition.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        call = (proxy, args) -> {
                            final SP sp = parameterizedDeleteFromFunc.apply(entityClass).where((Condition) args[0]).pair();
                            return proxy.prepareQuery(sp.sql).setParameters(1, sp.parameters).update();
                        };
                    } else {
                        call = (proxy, args) -> {
                            throw new UnsupportedOperationException("Unsupported operation: " + m);
                        };
                    }
                } else if (m.getDeclaringClass().equals(CrudDao.class)) {
                    final List<String> idPropNames = ClassUtil.getIdFieldNames(entityClass);
                    final Set<String> idPropNameSet = N.newHashSet(idPropNames);
                    final String idPropName = idPropNames.get(0);

                    String sql_getById = null;
                    String sql_existsById = null;
                    String sql_insertWithId = null;
                    String sql_insertWithoutId = null;
                    String sql_updateById = null;
                    String sql_deleteById = null;

                    if (sbc.equals(PSC.class)) {
                        sql_getById = PSC.selectFrom(entityClass).where(CF.eq(idPropName)).sql();
                        sql_existsById = PSC.select(SQLBuilder._1).from(entityClass).where(CF.eq(idPropName)).sql();
                        sql_insertWithId = NSC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NSC.insertInto(entityClass, N.asSet(idPropName)).sql();
                        sql_updateById = NSC.update(entityClass, idPropNameSet).where(CF.eq(idPropName)).sql();
                        sql_deleteById = PSC.deleteFrom(entityClass).where(CF.eq(idPropName)).sql();
                    } else if (sbc.equals(PAC.class)) {
                        sql_getById = PAC.selectFrom(entityClass).where(CF.eq(idPropName)).sql();
                        sql_existsById = PAC.select(SQLBuilder._1).from(entityClass).where(CF.eq(idPropName)).sql();
                        sql_updateById = NAC.update(entityClass, idPropNameSet).where(CF.eq(idPropName)).sql();
                        sql_insertWithId = NAC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NAC.insertInto(entityClass, N.asSet(idPropName)).sql();
                        sql_deleteById = PAC.deleteFrom(entityClass).where(CF.eq(idPropName)).sql();
                    } else {
                        sql_getById = PLC.selectFrom(entityClass).where(CF.eq(idPropName)).sql();
                        sql_existsById = PLC.select(SQLBuilder._1).from(entityClass).where(CF.eq(idPropName)).sql();
                        sql_insertWithId = NLC.insertInto(entityClass).sql();
                        sql_insertWithoutId = NLC.insertInto(entityClass, N.asSet(idPropName)).sql();
                        sql_updateById = NLC.update(entityClass, idPropNameSet).where(CF.eq(idPropName)).sql();
                        sql_deleteById = PLC.deleteFrom(entityClass).where(CF.eq(idPropName)).sql();
                    }

                    final NamedSQL insertWithIdSQL = NamedSQL.parse(sql_insertWithId);
                    final NamedSQL insertWithoutIdSQL = NamedSQL.parse(sql_insertWithoutId);
                    final NamedSQL updateByIdSQL = NamedSQL.parse(sql_updateById);

                    if (m.getName().equals("insert")) {
                        call = (proxy, args) -> {
                            final Object idPropValue = ClassUtil.getPropValue(args[0], idPropName);

                            if (JdbcUtil.isDefaultIdPropValue(idPropValue)) {
                                final Object id = proxy.prepareNamedQuery(insertWithoutIdSQL, true)
                                        .setParameters(args[0])
                                        .insert()
                                        .ifPresent(ret -> ClassUtil.setPropValue(args[0], idPropName, ret))
                                        .orElse(N.defaultValueOf(returnType));

                                if (args[0] instanceof DirtyMarker) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) args[0], false);
                                }

                                return id;
                            } else {
                                proxy.prepareNamedQuery(insertWithIdSQL).setParameters(args[0]).update();

                                if (args[0] instanceof DirtyMarker) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) args[0], false);
                                }

                                return idPropValue;
                            }
                        };
                    } else if (m.getName().equals("batchInsert") && paramLen == 2 && int.class.equals(paramTypes[1])) {
                        call = (proxy, args) -> {
                            final Collection<?> entities = (Collection<Object>) args[0];
                            final int batchSize = (Integer) args[1];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            List<Object> ids = null;
                            final Object idPropValue = N.isNullOrEmpty(entities) ? null : ClassUtil.getPropValue(N.first(entities).get(), idPropName);
                            final boolean isDefaultIdPropValue = JdbcUtil.isDefaultIdPropValue(idPropValue);

                            if (entities.size() <= batchSize) {
                                if (isDefaultIdPropValue) {
                                    ids = proxy.prepareNamedQuery(insertWithoutIdSQL, true).addBatchParameters(entities).batchInsert();
                                } else {
                                    proxy.prepareNamedQuery(insertWithIdSQL).addBatchParameters(entities).batchUpdate();
                                }
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    if (isDefaultIdPropValue) {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(insertWithoutIdSQL, true).closeAfterExecution(false)) {
                                            ids = ExceptionalStream.of(entities)
                                                    .splitToList(batchSize)
                                                    .flattMap(bp -> nameQuery.addBatchParameters(bp).batchInsert())
                                                    .toList();
                                        }
                                    } else {
                                        try (NamedQuery nameQuery = proxy.prepareNamedQuery(insertWithIdSQL).closeAfterExecution(false)) {
                                            ExceptionalStream.of(entities)
                                                    .splitToList(batchSize) //
                                                    .forEach(bp -> nameQuery.addBatchParameters(bp).batchUpdate());
                                        }
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (isDefaultIdPropValue) {
                                if (N.notNullOrEmpty(ids) && N.notNullOrEmpty(entities) && ids.size() == N.size(entities)) {
                                    int idx = 0;

                                    for (Object e : entities) {
                                        ClassUtil.setPropValue(e, idPropName, ids.get(idx++));
                                    }
                                }
                            } else {
                                ids = StreamEx.of(entities).map(e -> ClassUtil.getPropValue(e, idPropName)).toList();
                            }

                            if (N.first(entities).orNull() instanceof DirtyMarker) {
                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                                }
                            }

                            return ids;
                        };
                    } else if (m.getName().equals("batchInsert") && paramLen == 3 && int.class.equals(paramTypes[2])) {
                        call = (proxy, args) -> {
                            final String namedInsertSQL = (String) args[0];
                            final Collection<?> entities = (Collection<Object>) args[1];
                            final int batchSize = (Integer) args[2];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            List<Object> ids = null;

                            if (entities.size() <= batchSize) {
                                ids = proxy.prepareNamedQuery(namedInsertSQL, true).addBatchParameters(entities).batchInsert();
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    try (NamedQuery nameQuery = proxy.prepareNamedQuery(namedInsertSQL, true).closeAfterExecution(false)) {
                                        ids = ExceptionalStream.of(entities)
                                                .splitToList(batchSize)
                                                .flattMap(bp -> nameQuery.addBatchParameters(bp).batchInsert())
                                                .toList();
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (N.notNullOrEmpty(ids) && N.notNullOrEmpty(entities) && ids.size() == N.size(entities)) {
                                int idx = 0;

                                for (Object e : entities) {
                                    ClassUtil.setPropValue(e, idPropName, ids.get(idx++));
                                }
                            } else {
                                ids = StreamEx.of(entities).map(e -> ClassUtil.getPropValue(e, idPropName)).toList();
                            }

                            if (N.first(entities).orNull() instanceof DirtyMarker) {
                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, false);
                                }
                            }

                            return ids;
                        };
                    } else if (m.getName().equals("get")) {
                        if (paramLen == 1) {
                            final String query = sql_getById;
                            call = (proxy, args) -> proxy.prepareQuery(query).setObject(1, args[0]).get(entityClass);
                        } else {
                            call = (proxy, args) -> proxy
                                    .prepareQuery(parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where(CF.eq(idPropName)).sql())
                                    .setObject(1, args[1])
                                    .get(entityClass);
                        }
                    } else if (m.getName().equals("gett")) {
                        if (paramLen == 1) {
                            final String query = sql_getById;
                            call = (proxy, args) -> proxy.prepareQuery(query).setObject(1, args[0]).gett(entityClass);
                        } else {
                            call = (proxy, args) -> proxy
                                    .prepareQuery(parameterizedSelectFunc2.apply((Collection<String>) args[0]).from(entityClass).where(CF.eq(idPropName)).sql())
                                    .setObject(1, args[1])
                                    .gett(entityClass);
                        }
                    } else if (m.getName().equals("exists") && paramLen == 1 && !Condition.class.isAssignableFrom(paramTypes[0])) {
                        final String query = sql_existsById;
                        call = (proxy, args) -> proxy.prepareQuery(query).setObject(1, args[0]).exists();
                    } else if (m.getName().equals("update") && paramLen == 1) {
                        if (DirtyMarker.class.isAssignableFrom(paramTypes[0])) {
                            call = (proxy, args) -> {
                                final String sql = namedUpdateFunc.apply(entityClass).set(args[0], idPropNameSet).where(CF.eq(idPropName)).sql();
                                final NamedSQL namedSQL = NamedSQL.parse(sql);

                                final int result = proxy.prepareNamedQuery(namedSQL).setParameters(args[0]).update();

                                DirtyMarkerUtil.markDirty((DirtyMarker) args[0], namedSQL.getNamedParameters(), false);

                                return result;
                            };
                        } else {
                            call = (proxy, args) -> {
                                final int result = proxy.prepareNamedQuery(updateByIdSQL).setParameters(args[0]).update();

                                if (args[0] instanceof DirtyMarker) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) args[0], updateByIdSQL.getNamedParameters(), false);
                                }

                                return result;
                            };
                        }
                    } else if (m.getName().equals("update") && paramLen == 2 && !Map.class.equals(paramTypes[0]) && Collection.class.equals(paramTypes[1])) {
                        call = (proxy, args) -> {
                            final Collection<String> propNamesToUpdate = (Collection<String>) args[1];
                            N.checkArgNotNullOrEmpty(propNamesToUpdate, "propNamesToUpdate");

                            final String sql = namedUpdateFunc.apply(entityClass).set(propNamesToUpdate).where(CF.eq(idPropName)).sql();
                            final NamedSQL namedSQL = NamedSQL.parse(sql);

                            final int result = proxy.prepareNamedQuery(namedSQL).setParameters(args[0]).update();

                            if (args[0] instanceof DirtyMarker) {
                                DirtyMarkerUtil.markDirty((DirtyMarker) args[0], namedSQL.getNamedParameters(), false);
                            }

                            return result;
                        };
                    } else if (m.getName().equals("update") && paramLen == 2 && Map.class.equals(paramTypes[0])) {
                        call = (proxy, args) -> {
                            final Map<String, Object> props = (Map<String, Object>) args[0];
                            N.checkArgNotNullOrEmpty(props, "updateProps");
                            final String query = parameterizedUpdateFunc.apply(entityClass).set(props.keySet()).where(CF.eq(idPropName)).sql();

                            return proxy.prepareQuery(query).setParameters(1, props.values()).setObject(props.size() + 1, args[1]).update();
                        };
                    } else if (m.getName().equals("batchUpdate") && paramLen == 2 && int.class.equals(paramTypes[1])) {
                        call = (proxy, args) -> {
                            final Collection<Object> entities = (Collection<Object>) args[0];
                            final int batchSize = (Integer) args[1];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            final Object entity = N.firstNonNull(entities).get();
                            final String sql = namedUpdateFunc.apply(entityClass).set(entity, idPropNameSet).where(CF.eq(idPropName)).sql();
                            final NamedSQL namedSQL = NamedSQL.parse(sql);
                            int result = 0;

                            if (entities.size() <= batchSize) {
                                result = N.sum(proxy.prepareNamedQuery(namedSQL).addBatchParameters(entities).batchUpdate());
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    try (NamedQuery nameQuery = proxy.prepareNamedQuery(namedSQL).closeAfterExecution(false)) {
                                        result = (int) ExceptionalStream.of(entities)
                                                .splitToList(batchSize) //
                                                .sumInt(bp -> N.sum(proxy.prepareNamedQuery(namedSQL).addBatchParameters(bp).batchUpdate()))
                                                .orZero();
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (entity instanceof DirtyMarker) {
                                final Collection<String> propNamesToUpdate = namedSQL.getNamedParameters();

                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, propNamesToUpdate, false);
                                }
                            }

                            return result;
                        };
                    } else if (m.getName().equals("batchUpdate") && paramLen == 3 && int.class.equals(paramTypes[2])) {
                        call = (proxy, args) -> {
                            final Collection<Object> entities = (Collection<Object>) args[0];
                            final Collection<String> propNamesToUpdate = (Collection<String>) args[1];
                            final int batchSize = (Integer) args[2];
                            N.checkArgPositive(batchSize, "batchSize");

                            N.checkArgNotNullOrEmpty(propNamesToUpdate, "propNamesToUpdate");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            final String sql = namedUpdateFunc.apply(entityClass).set(propNamesToUpdate).where(CF.eq(idPropName)).sql();
                            final NamedSQL namedSQL = NamedSQL.parse(sql);
                            int result = 0;

                            if (entities.size() <= batchSize) {
                                result = N.sum(proxy.prepareNamedQuery(namedSQL).addBatchParameters(entities).batchUpdate());
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());

                                try {
                                    try (NamedQuery nameQuery = proxy.prepareNamedQuery(namedSQL).closeAfterExecution(false)) {
                                        result = (int) ExceptionalStream.of(entities)
                                                .splitToList(batchSize) //
                                                .sumInt(bp -> N.sum(proxy.prepareNamedQuery(namedSQL).addBatchParameters(bp).batchUpdate()))
                                                .orZero();
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }
                            }

                            if (N.first(entities).orNull() instanceof DirtyMarker) {
                                for (Object e : entities) {
                                    DirtyMarkerUtil.markDirty((DirtyMarker) e, propNamesToUpdate, false);
                                }
                            }

                            return result;
                        };
                    } else if (m.getName().equals("deleteById")) {
                        final String query = sql_deleteById;
                        call = (proxy, args) -> proxy.prepareQuery(query).setObject(1, args[0]).update();
                    } else if (m.getName().equals("delete") && paramLen == 1 && !Condition.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        final String query = sql_deleteById;
                        call = (proxy, args) -> proxy.prepareQuery(query).setObject(1, ClassUtil.getPropValue(args[0], idPropName)).update();
                    } else if (m.getName().equals("batchDelete") && paramLen == 2 && int.class.equals(paramTypes[1])) {
                        final String query = sql_deleteById;

                        call = (proxy, args) -> {
                            final Collection<?> entities = (Collection<?>) args[0];
                            final int batchSize = (Integer) args[1];
                            N.checkArgPositive(batchSize, "batchSize");

                            if (N.isNullOrEmpty(entities)) {
                                return 0;
                            }

                            if (entities.size() <= batchSize) {
                                return N.sum(proxy.prepareQuery(query)
                                        .addBatchParameters(entities, (q, e) -> q.setObject(1, ClassUtil.getPropValue(e, idPropName)))
                                        .batchUpdate());
                            } else {
                                final SQLTransaction tran = JdbcUtil.beginTransaction(proxy.dataSource());
                                int result = 0;

                                try {
                                    try (PreparedQuery preparedQuery = proxy.prepareQuery(query).closeAfterExecution(false)) {
                                        result = (int) ExceptionalStream.of(entities)
                                                .splitToList(batchSize)
                                                .sumInt(bp -> N.sum(
                                                        preparedQuery.addBatchParameters(bp, (q, e) -> q.setObject(1, ClassUtil.getPropValue(e, idPropName)))
                                                                .batchUpdate()))
                                                .orZero();
                                    }

                                    tran.commit();
                                } finally {
                                    tran.rollbackIfNotCommitted();
                                }

                                return (int) result;
                            }
                        };
                    } else {
                        call = (proxy, args) -> {
                            throw new UnsupportedOperationException("Unsupported operation: " + m);
                        };
                    }
                } else if (sqlAnno == null) {
                    if (Modifier.isAbstract(m.getModifiers())) {
                        if (m.getName().equals("dataSource") && javax.sql.DataSource.class.isAssignableFrom(returnType) && paramLen == 0) {
                            call = (proxy, args) -> ds;
                        } else {
                            call = (proxy, args) -> {
                                throw new UnsupportedOperationException("Unsupported operation: " + m);
                            };
                        }
                    } else {
                        final MethodHandle methodHandle = createMethodHandle(m);

                        call = (proxy, args) -> {
                            try {
                                return methodHandle.bindTo(proxy).invokeWithArguments(args);
                            } catch (Throwable e) {
                                throw new Exception(e);
                            }
                        };
                    }
                } else {
                    final Class<?> lastParamType = paramLen == 0 ? null : paramTypes[paramLen - 1];
                    final String query = N.checkArgNotNullOrEmpty(JdbcUtil.sqlAnnoMap.get(sqlAnno.annotationType()).apply(sqlAnno),
                            "sql can't be null or empty");

                    final boolean isNamedQuery = sqlAnno.annotationType().getSimpleName().startsWith("Named");
                    final NamedSQL namedSQL = isNamedQuery ? NamedSQL.parse(query) : null;

                    if ((paramLen > 0 && JdbcUtil.ParametersSetter.class.isAssignableFrom(paramTypes[0]))
                            || (paramLen > 1 && JdbcUtil.BiParametersSetter.class.isAssignableFrom(paramTypes[1]))
                            || (paramLen > 1 && JdbcUtil.TriParametersSetter.class.isAssignableFrom(paramTypes[1]))) {
                        throw new UnsupportedOperationException(
                                "Setting parameters by 'ParametersSetter/BiParametersSetter/TriParametersSetter' is disabled. Can't use it in method: "
                                        + m.getName());
                    }

                    if (paramLen > 0 && (ResultExtractor.class.isAssignableFrom(lastParamType) || BiResultExtractor.class.isAssignableFrom(lastParamType)
                            || RowMapper.class.isAssignableFrom(lastParamType) || BiRowMapper.class.isAssignableFrom(lastParamType))) {
                        throw new UnsupportedOperationException(
                                "Retrieving result/record by 'ResultExtractor/BiResultExtractor/RowMapper/BiRowMapper' is disabled. Can't use it in method: "
                                        + m.getName());
                    }

                    if (java.util.Optional.class.isAssignableFrom(returnType) || java.util.OptionalInt.class.isAssignableFrom(returnType)
                            || java.util.OptionalLong.class.isAssignableFrom(returnType) || java.util.OptionalDouble.class.isAssignableFrom(returnType)) {
                        throw new UnsupportedOperationException("the return type of the method: " + m.getName() + " can't be: " + returnType);
                    }

                    if (StreamEx.of(m.getExceptionTypes()).noneMatch(e -> SQLException.class.equals(e))) {
                        throw new UnsupportedOperationException("'throws SQLException' is not declared in method: " + m.getName());
                    }

                    if (paramLen > 0 && (Map.class.isAssignableFrom(paramTypes[0]) || ClassUtil.isEntity(paramTypes[0])) && isNamedQuery == false) {
                        throw new IllegalArgumentException(
                                "Using named query: @NamedSelect/Update/Insert/Delete when parameter type is Map or entity with Getter/Setter methods");
                    }

                    BiParametersSetter<AbstractPreparedQuery, Object[]> parametersSetter = null;

                    if (paramLen == 0) {
                        parametersSetter = null;
                    } else if (JdbcUtil.ParametersSetter.class.isAssignableFrom(paramTypes[0])) {
                        parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                            @Override
                            public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                preparedQuery.settParameters((JdbcUtil.ParametersSetter) args[0]);
                            }
                        };
                    } else if (paramLen > 1 && JdbcUtil.BiParametersSetter.class.isAssignableFrom(paramTypes[1])) {
                        parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                            @Override
                            public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                preparedQuery.settParameters(args[0], (JdbcUtil.BiParametersSetter) args[1]);
                            }
                        };
                    } else if (paramLen > 1 && JdbcUtil.TriParametersSetter.class.isAssignableFrom(paramTypes[1])) {
                        parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                            @Override
                            public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                ((NamedQuery) preparedQuery).setParameters(args[0], (JdbcUtil.TriParametersSetter) args[1]);
                            }
                        };
                    } else if (Collection.class.isAssignableFrom(paramTypes[0])) {
                        parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                            @Override
                            public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                preparedQuery.setParameters(1, (Collection) args[0]);
                            }
                        };
                    } else {
                        final boolean isLastParameterMapperOrExtractor = ResultExtractor.class.isAssignableFrom(lastParamType)
                                || BiResultExtractor.class.isAssignableFrom(lastParamType) || RowMapper.class.isAssignableFrom(lastParamType)
                                || BiRowMapper.class.isAssignableFrom(lastParamType);

                        if (isLastParameterMapperOrExtractor && paramLen == 1) {
                            // ignore
                        } else if ((isLastParameterMapperOrExtractor && paramLen == 2) || (!isLastParameterMapperOrExtractor && paramLen == 1)) {
                            if (isNamedQuery) {
                                final Dao.Bind binder = StreamEx.of(m.getParameterAnnotations())
                                        .limit(1)
                                        .flatMapp(e -> e)
                                        .select(Dao.Bind.class)
                                        .first()
                                        .orNull();

                                if (binder != null) {
                                    parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                        @Override
                                        public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                            ((NamedQuery) preparedQuery).setObject(binder.value(), args[0]);
                                        }
                                    };
                                } else if (Map.class.isAssignableFrom(m.getParameterTypes()[0])) {
                                    parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                        @Override
                                        public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                            ((NamedQuery) preparedQuery).setParameters((Map<String, ?>) args[0]);
                                        }
                                    };
                                } else if (ClassUtil.isEntity(m.getParameterTypes()[0])) {
                                    parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                        @Override
                                        public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                            ((NamedQuery) preparedQuery).setParameters(args[0]);
                                        }
                                    };
                                } else {
                                    throw new UnsupportedOperationException("In method: " + ClassUtil.getSimpleClassName(daoInterface) + "." + m.getName()
                                            + ", parameters for named query have to be binded with names through annotation @Bind, or Map/Entity with getter/setter methods. Can not be: "
                                            + ClassUtil.getSimpleClassName(m.getParameterTypes()[0]));
                                }
                            } else {
                                parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                    @Override
                                    public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                        preparedQuery.setObject(1, args[0]);
                                    }
                                };
                            }
                        } else if (paramLen > 2 || !isLastParameterMapperOrExtractor) {
                            if (isNamedQuery) {
                                final String[] paramNames = IntStreamEx.range(0, paramLen - (isLastParameterMapperOrExtractor ? 1 : 0))
                                        .mapToObj(i -> StreamEx.of(m.getParameterAnnotations()[i])
                                                .select(Dao.Bind.class)
                                                .first()
                                                .orElseThrow(() -> new UnsupportedOperationException(
                                                        "In method: " + ClassUtil.getSimpleClassName(daoInterface) + "." + m.getName() + ", parameters[" + i
                                                                + "]: " + ClassUtil.getSimpleClassName(m.getParameterTypes()[i])
                                                                + " is not binded with parameter named through annotation @Bind")))
                                        .map(b -> b.value())
                                        .toArray(len -> new String[len]);

                                parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                    @Override
                                    public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                        final NamedQuery namedQuery = ((NamedQuery) preparedQuery);

                                        for (int i = 0, count = paramNames.length; i < count; i++) {
                                            namedQuery.setObject(paramNames[i], args[i]);
                                        }
                                    }
                                };
                            } else {
                                parametersSetter = new BiParametersSetter<AbstractPreparedQuery, Object[]>() {
                                    @Override
                                    public void accept(AbstractPreparedQuery preparedQuery, Object[] args) throws SQLException {
                                        if (isLastParameterMapperOrExtractor) {
                                            preparedQuery.setParameters(1, Arrays.asList(N.copyOfRange(args, 0, args.length - 1)));
                                        } else {
                                            preparedQuery.setParameters(1, Arrays.asList(args));
                                        }
                                    }
                                };
                            }
                        } else {
                            // ignore.
                        }
                    }

                    final BiParametersSetter<AbstractPreparedQuery, Object[]> finalParametersSetter = parametersSetter;

                    Tuple2<Integer, Integer> tp = null;

                    if (sqlAnno instanceof Dao.Select) {
                        tp = Tuple.of(((Dao.Select) sqlAnno).queryTimeout(), ((Dao.Select) sqlAnno).fetchSize());
                    } else if (sqlAnno instanceof Dao.NamedSelect) {
                        tp = Tuple.of(((Dao.NamedSelect) sqlAnno).queryTimeout(), ((Dao.NamedSelect) sqlAnno).fetchSize());
                    } else if (sqlAnno instanceof Dao.Insert) {
                        tp = Tuple.of(((Dao.Insert) sqlAnno).queryTimeout(), -1);
                    } else if (sqlAnno instanceof Dao.NamedInsert) {
                        tp = Tuple.of(((Dao.NamedInsert) sqlAnno).queryTimeout(), -1);
                    } else if (sqlAnno instanceof Dao.Update) {
                        tp = Tuple.of(((Dao.Update) sqlAnno).queryTimeout(), -1);
                    } else if (sqlAnno instanceof Dao.NamedUpdate) {
                        tp = Tuple.of(((Dao.NamedUpdate) sqlAnno).queryTimeout(), -1);
                    } else if (sqlAnno instanceof Dao.Delete) {
                        tp = Tuple.of(((Dao.Delete) sqlAnno).queryTimeout(), -1);
                    } else if (sqlAnno instanceof Dao.NamedDelete) {
                        tp = Tuple.of(((Dao.NamedDelete) sqlAnno).queryTimeout(), -1);
                    } else {
                        tp = Tuple.of(-1, -1);
                    }

                    final int queryTimeout = tp._1;
                    final int fetchSize = tp._2;

                    final boolean returnGeneratedKeys = (sqlAnno.annotationType().equals(Dao.Insert.class)
                            || sqlAnno.annotationType().equals(Dao.NamedInsert.class)) && !returnType.equals(void.class);

                    if (sqlAnno.annotationType().equals(Dao.Select.class) || sqlAnno.annotationType().equals(Dao.NamedSelect.class)) {
                        final boolean idDirtyMarker = ClassUtil.isEntity(returnType) && DirtyMarker.class.isAssignableFrom(returnType);

                        final Try.BiFunction<AbstractPreparedQuery, Object[], T, Exception> queryFunc = createQueryFunctionByMethod(m);

                        // Getting ClassCastException. Not sure why query result is being casted Dao. It seems there is a bug in JDk compiler.
                        //   call = (proxy, args) -> queryFunc.apply(JdbcUtil.prepareQuery(proxy, ds, query, isNamedQuery, fetchSize, queryTimeout, returnGeneratedKeys, args, paramSetter), args);

                        call = new Try.BiFunction<Dao, Object[], Object, Exception>() {
                            @Override
                            public Object apply(Dao proxy, Object[] args) throws Exception {
                                Object result = queryFunc.apply(JdbcUtil.prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout,
                                        returnGeneratedKeys, args, finalParametersSetter), args);

                                if (idDirtyMarker) {
                                    ((DirtyMarker) result).markDirty(false);
                                }

                                return result;
                            }
                        };
                    } else if (sqlAnno.annotationType().equals(Dao.Insert.class) || sqlAnno.annotationType().equals(Dao.NamedInsert.class)) {
                        final boolean idDirtyMarker = paramTypes.length == 1 && ClassUtil.isEntity(paramTypes[0])
                                && DirtyMarker.class.isAssignableFrom(paramTypes[0]);

                        final String idPropName = paramTypes.length == 1 && ClassUtil.isEntity(paramTypes[0])
                                && ClassUtil.getIdFieldNames(paramTypes[0]).size() == 1 ? ClassUtil.getIdFieldNames(paramTypes[0]).get(0) : null;

                        if (void.class.equals(returnType)) {
                            call = (proxy, args) -> {
                                prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args, finalParametersSetter)
                                        .update();

                                if (idDirtyMarker) {
                                    ((DirtyMarker) args[0]).markDirty(false);
                                }

                                return null;
                            };
                        } else if (Optional.class.equals(returnType)) {
                            call = (proxy, args) -> {
                                final Object result = JdbcUtil
                                        .prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args,
                                                finalParametersSetter)
                                        .insert()
                                        .ifPresent(id -> If.notNullOrEmpty(idPropName).then(() -> ClassUtil.setPropValue(args[0], idPropName, id)));

                                if (idDirtyMarker) {
                                    ((DirtyMarker) args[0]).markDirty(false);
                                }

                                return result;
                            };
                        } else {
                            call = (proxy, args) -> {
                                final Object result = JdbcUtil
                                        .prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args,
                                                finalParametersSetter)
                                        .insert()
                                        .ifPresent(id -> If.notNullOrEmpty(idPropName).then(() -> ClassUtil.setPropValue(args[0], idPropName, id)))
                                        .orElse(N.defaultValueOf(returnType));

                                if (idDirtyMarker) {
                                    ((DirtyMarker) args[0]).markDirty(false);
                                }

                                return result;
                            };
                        }
                    } else if (sqlAnno.annotationType().equals(Dao.Update.class) || sqlAnno.annotationType().equals(Dao.Delete.class)
                            || sqlAnno.annotationType().equals(Dao.NamedUpdate.class) || sqlAnno.annotationType().equals(Dao.NamedDelete.class)) {
                        if (!(returnType.equals(int.class) || returnType.equals(Integer.class) || returnType.equals(long.class) || returnType.equals(Long.class)
                                || returnType.equals(boolean.class) || returnType.equals(Boolean.class) || returnType.equals(void.class))) {
                            throw new UnsupportedOperationException(
                                    "The return type of update/delete operations can only be: int/Integer/long/Long/boolean/Boolean/void, can't be: "
                                            + returnType);
                        }

                        final boolean idDirtyMarker = paramTypes.length == 1 && ClassUtil.isEntity(paramTypes[0])
                                && DirtyMarker.class.isAssignableFrom(paramTypes[0]);

                        if (returnType.equals(int.class) || returnType.equals(Integer.class)) {
                            call = (proxy, args) -> {
                                final Object result = JdbcUtil
                                        .prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args,
                                                finalParametersSetter)
                                        .update();

                                if (idDirtyMarker) {
                                    if (isNamedQuery
                                            && (sqlAnno.annotationType().equals(Dao.Update.class) || sqlAnno.annotationType().equals(Dao.NamedUpdate.class))) {
                                        ((DirtyMarker) args[0]).markDirty(namedSQL.getNamedParameters(), false);
                                    } else {
                                        ((DirtyMarker) args[0]).markDirty(false);
                                    }
                                }

                                return result;
                            };
                        } else if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
                            call = (proxy, args) -> {
                                final Object result = JdbcUtil
                                        .prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args,
                                                finalParametersSetter)
                                        .update() > 0;

                                if (idDirtyMarker) {
                                    if (isNamedQuery
                                            && (sqlAnno.annotationType().equals(Dao.Update.class) || sqlAnno.annotationType().equals(Dao.NamedUpdate.class))) {
                                        ((DirtyMarker) args[0]).markDirty(namedSQL.getNamedParameters(), false);
                                    } else {
                                        ((DirtyMarker) args[0]).markDirty(false);
                                    }
                                }

                                return result;
                            };
                        } else if (returnType.equals(long.class) || returnType.equals(Long.class)) {
                            call = (proxy, args) -> {
                                final Object result = JdbcUtil
                                        .prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args,
                                                finalParametersSetter)
                                        .largeUpate();

                                if (idDirtyMarker) {
                                    ((DirtyMarker) args[0]).markDirty(false);
                                }

                                return result;
                            };
                        } else {
                            call = (proxy, args) -> {
                                prepareQuery(proxy, query, isNamedQuery, namedSQL, fetchSize, queryTimeout, returnGeneratedKeys, args, finalParametersSetter)
                                        .update();

                                if (idDirtyMarker) {
                                    ((DirtyMarker) args[0]).markDirty(false);
                                }

                                return null;
                            };
                        }
                    } else {
                        throw new UnsupportedOperationException("Unsupported sql annotation: " + sqlAnno.annotationType());
                    }
                }
            }

            methodInvokerMap.put(m, call);
        }

        final Try.TriFunction<Dao, Method, Object[], ?, Exception> proxyInvoker = (proxy, method, args) -> methodInvokerMap.get(method).apply(proxy, args);
        final Class<T>[] interfaceClasses = N.asArray(daoInterface);

        final InvocationHandler h = (proxy, method, args) -> {
            if (!JdbcUtil.noLogMethods.contains(method.getName())) {
                logger.debug("Invoking SQL method: {} with args: {}", method.getName(), args);
            }

            return proxyInvoker.apply((Dao) proxy, method, args);
        };

        return N.newProxyInstance(interfaceClasses, h);
    }

    /** The Constant sqlAnnoMap. */
    private static final Map<Class<? extends Annotation>, Function<Annotation, String>> sqlAnnoMap = new HashMap<>();
    static {
        sqlAnnoMap.put(Dao.Select.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.Select) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.Select) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.Insert.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.Insert) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.Insert) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.Update.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.Update) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.Update) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.Delete.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.Delete) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.Delete) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.NamedSelect.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.NamedSelect) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.NamedSelect) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.NamedInsert.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.NamedInsert) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.NamedInsert) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.NamedUpdate.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.NamedUpdate) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.NamedUpdate) anno).value());
            }

            return sql;
        });
        sqlAnnoMap.put(Dao.NamedDelete.class, (Annotation anno) -> {
            String sql = StringUtil.trim(((Dao.NamedDelete) anno).sql());

            if (N.isNullOrEmpty(sql)) {
                sql = StringUtil.trim(((Dao.NamedDelete) anno).value());
            }

            return sql;
        });
    }

    /** The Constant noLogMethods. */
    private static final Set<String> noLogMethods = N.newHashSet();

    static {
        noLogMethods.add("dataSource");
        noLogMethods.add("prepareQuery");
        noLogMethods.add("prepareNamedQuery");
        noLogMethods.add("prepareCallableQuery");
    }

    /**
     * Creates the method handle.
     *
     * @param method
     * @return
     */
    static MethodHandle createMethodHandle(final Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();

        try {
            return MethodHandles.lookup().in(declaringClass).unreflectSpecial(method, declaringClass);
        } catch (Exception e) {
            try {
                final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                constructor.setAccessible(true);

                return constructor.newInstance(declaringClass).in(declaringClass).unreflectSpecial(method, declaringClass);
            } catch (Exception ex) {
                try {
                    return MethodHandles.lookup()
                            .findSpecial(declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                                    declaringClass);
                } catch (Exception exx) {
                    throw new UnsupportedOperationException(exx);
                }
            }
        }
    }

    /**
     * Creates the query function by method.
     *
     * @param <R>
     * @param method
     * @return
     */
    @SuppressWarnings("rawtypes")
    static <R> BiFunction<AbstractPreparedQuery, Object[], R, Exception> createQueryFunctionByMethod(final Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final int paramLen = paramTypes.length;
        final Class<?> lastParamType = paramLen == 0 ? null : paramTypes[paramLen - 1];
        final boolean isListQuery = isListQuery(method);

        if (paramLen > 0 && (ResultExtractor.class.isAssignableFrom(lastParamType) || BiResultExtractor.class.isAssignableFrom(lastParamType)
                || RowMapper.class.isAssignableFrom(lastParamType) || BiRowMapper.class.isAssignableFrom(lastParamType))) {
            if (RowMapper.class.isAssignableFrom(lastParamType)) {
                if (isListQuery) {
                    return (preparedQuery, args) -> (R) preparedQuery.list((RowMapper) args[paramLen - 1]);
                } else if (Optional.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.findFirst((RowMapper) args[paramLen - 1]);
                } else if (ExceptionalStream.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.stream((RowMapper) args[paramLen - 1]);
                } else if (Stream.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.stream((RowMapper) args[paramLen - 1]).unchecked();
                } else {
                    if (Nullable.class.isAssignableFrom(returnType)) {
                        throw new UnsupportedOperationException(
                                "The return type of method: " + method.getName() + " can't be: " + returnType + " when RowMapper/BiRowMapper is specified");
                    }

                    return (preparedQuery, args) -> (R) preparedQuery.findFirst((RowMapper) args[paramLen - 1]).orElse(N.defaultValueOf(returnType));
                }
            } else if (BiRowMapper.class.isAssignableFrom(lastParamType)) {
                if (isListQuery) {
                    return (preparedQuery, args) -> (R) preparedQuery.list((BiRowMapper) args[paramLen - 1]);
                } else if (Optional.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.findFirst((BiRowMapper) args[paramLen - 1]);
                } else if (ExceptionalStream.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.stream((BiRowMapper) args[paramLen - 1]);
                } else if (Stream.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.stream((BiRowMapper) args[paramLen - 1]).unchecked();
                } else {
                    if (Nullable.class.isAssignableFrom(returnType)) {
                        throw new UnsupportedOperationException(
                                "The return type of method: " + method.getName() + " can't be: " + returnType + " when RowMapper/BiRowMapper is specified");
                    }

                    return (preparedQuery, args) -> (R) preparedQuery.findFirst((BiRowMapper) args[paramLen - 1]).orElse(N.defaultValueOf(returnType));
                }
            } else {
                if (method.getGenericParameterTypes()[paramLen - 1] instanceof ParameterizedType) {
                    final java.lang.reflect.Type resultExtractorReturnType = ((ParameterizedType) method.getGenericParameterTypes()[paramLen - 1])
                            .getActualTypeArguments()[0];
                    final Class<?> resultExtractorReturnClass = resultExtractorReturnType instanceof Class ? (Class<?>) resultExtractorReturnType
                            : (Class<?>) ((ParameterizedType) resultExtractorReturnType).getRawType();

                    if (returnType.isAssignableFrom(resultExtractorReturnClass)) {
                        throw new UnsupportedOperationException("The return type: " + returnType + " of method: " + method.getName()
                                + " is not assignable from the return type of ResultExtractor: " + resultExtractorReturnClass);
                    }
                }

                if (ResultExtractor.class.isAssignableFrom(lastParamType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.query((ResultExtractor) args[paramLen - 1]);
                } else {
                    return (preparedQuery, args) -> (R) preparedQuery.query((BiResultExtractor) args[paramLen - 1]);
                }
            }
        } else if (DataSet.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.query();
        } else if (ClassUtil.isEntity(returnType) || Map.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.findFirst(BiRowMapper.to(returnType)).orNull();
        } else if (isListQuery) {
            final ParameterizedType parameterizedReturnType = (ParameterizedType) method.getGenericReturnType();
            final Class<?> eleType = parameterizedReturnType.getActualTypeArguments()[0] instanceof Class
                    ? (Class<?>) parameterizedReturnType.getActualTypeArguments()[0]
                    : (Class<?>) ((ParameterizedType) parameterizedReturnType.getActualTypeArguments()[0]).getRawType();

            return (preparedQuery, args) -> (R) preparedQuery.list(eleType);
        } else if (Optional.class.isAssignableFrom(returnType) || Nullable.class.isAssignableFrom(returnType)) {
            final ParameterizedType parameterizedReturnType = (ParameterizedType) method.getGenericReturnType();

            final Class<?> eleType = parameterizedReturnType.getActualTypeArguments()[0] instanceof Class
                    ? (Class<?>) parameterizedReturnType.getActualTypeArguments()[0]
                    : (Class<?>) ((ParameterizedType) parameterizedReturnType.getActualTypeArguments()[0]).getRawType();

            if (ClassUtil.isEntity(eleType) || Map.class.isAssignableFrom(eleType) || List.class.isAssignableFrom(eleType)
                    || Object[].class.isAssignableFrom(eleType)) {
                if (Nullable.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) Nullable.from(preparedQuery.findFirst(BiRowMapper.to(eleType)));
                } else {
                    return (preparedQuery, args) -> (R) preparedQuery.findFirst(BiRowMapper.to(eleType));
                }
            } else {
                if (Nullable.class.isAssignableFrom(returnType)) {
                    return (preparedQuery, args) -> (R) preparedQuery.queryForSingleResult(eleType);
                } else {
                    return (preparedQuery, args) -> (R) preparedQuery.queryForSingleNonNull(eleType);
                }
            }
        } else if (OptionalBoolean.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForBoolean();
        } else if (OptionalChar.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForChar();
        } else if (OptionalByte.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForByte();
        } else if (OptionalShort.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForShort();
        } else if (OptionalInt.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForInt();
        } else if (OptionalLong.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForLong();
        } else if (OptionalFloat.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForFloat();
        } else if (OptionalDouble.class.isAssignableFrom(returnType)) {
            return (preparedQuery, args) -> (R) preparedQuery.queryForDouble();
        } else if (ExceptionalStream.class.isAssignableFrom(returnType) || Stream.class.isAssignableFrom(returnType)) {
            final ParameterizedType parameterizedReturnType = (ParameterizedType) method.getGenericReturnType();

            final Class<?> eleType = parameterizedReturnType.getActualTypeArguments()[0] instanceof Class
                    ? (Class<?>) parameterizedReturnType.getActualTypeArguments()[0]
                    : (Class<?>) ((ParameterizedType) parameterizedReturnType.getActualTypeArguments()[0]).getRawType();

            if (ExceptionalStream.class.isAssignableFrom(returnType)) {
                return (preparedQuery, args) -> (R) preparedQuery.stream(eleType);
            } else {
                return (preparedQuery, args) -> (R) preparedQuery.stream(eleType).unchecked();
            }
        } else {
            return (preparedQuery, args) -> (R) preparedQuery.queryForSingleResult(returnType).orElse(N.defaultValueOf(returnType));
        }
    }

    /**
     *
     * @param proxy
     * @param query
     * @param isNamedQuery
     * @param namedSQL TODO
     * @param fetchSize
     * @param queryTimeout
     * @param returnGeneratedKeys
     * @param args
     * @param paramSetter
     * @return
     * @throws SQLException the SQL exception
     * @throws Exception the exception
     */
    @SuppressWarnings("rawtypes")
    static AbstractPreparedQuery prepareQuery(final Dao proxy, final String query, final boolean isNamedQuery, NamedSQL namedSQL, final int fetchSize,
            final int queryTimeout, final boolean returnGeneratedKeys, final Object[] args,
            final BiParametersSetter<? super AbstractPreparedQuery, Object[]> paramSetter) throws SQLException, Exception {

        final AbstractPreparedQuery preparedQuery = isNamedQuery ? proxy.prepareNamedQuery(namedSQL, returnGeneratedKeys)
                : proxy.prepareQuery(query, returnGeneratedKeys);

        if (fetchSize > 0) {
            preparedQuery.setFetchSize(fetchSize);
        }

        if (queryTimeout >= 0) {
            preparedQuery.setQueryTimeout(queryTimeout);
        }

        if (paramSetter != null) {
            preparedQuery.settParameters(args, paramSetter);
        }

        return preparedQuery;
    }

    /**
     * Checks if is list query.
     *
     * @param method
     * @return true, if is list query
     */
    static boolean isListQuery(final Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Class<?> returnType = method.getReturnType();
        final int paramLen = paramTypes.length;

        if (List.class.isAssignableFrom(returnType)) {

            // Check if return type is generic List type.
            if (method.getGenericReturnType() instanceof ParameterizedType) {
                final ParameterizedType parameterizedReturnType = (ParameterizedType) method.getGenericReturnType();

                if (paramLen > 0 && (RowMapper.class.isAssignableFrom(paramTypes[paramLen - 1]) || BiRowMapper.class.isAssignableFrom(paramTypes[paramLen - 1]))
                        && method.getGenericParameterTypes()[paramLen - 1] instanceof ParameterizedType) {

                    // if the return type of the method is same as the return type of RowMapper/BiRowMapper parameter, return false;
                    return !parameterizedReturnType.equals(((ParameterizedType) method.getGenericParameterTypes()[paramLen - 1]).getActualTypeArguments()[0]);
                }
            }

            return !(method.getName().startsWith("get") || method.getName().startsWith("findFirst") || method.getName().startsWith("findOne"));
        } else {
            return false;
        }
    }

    /**
     * The Class SimpleDataSource.
     */
    static class SimpleDataSource implements DataSource {

        /** The Constant PRIMARY. */
        static final String PRIMARY = "primary";

        /** The sql data source. */
        private final javax.sql.DataSource sqlDataSource;

        /** The props. */
        private final Properties<String, String> props = new Properties<>();

        /** The slice selector. */
        private final SliceSelector sliceSelector = new NonSliceSelector();

        /** The close method. */
        private final Method closeMethod;

        /** The is closed. */
        private boolean isClosed = false;

        /**
         * Instantiates a new simple data source.
         *
         * @param sqlDataSource
         */
        public SimpleDataSource(final javax.sql.DataSource sqlDataSource) {
            this.sqlDataSource = sqlDataSource;

            Method method = null;

            try {
                method = ClassUtil.getDeclaredMethod(sqlDataSource.getClass(), "close");
            } catch (Exception e) {

            }

            closeMethod = method != null && Modifier.isPublic(method.getModifiers()) ? method : null;
        }

        /**
         * Gets the connection.
         *
         * @param username
         * @param password
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public Connection getConnection(final String username, final String password) throws SQLException {
            return sqlDataSource.getConnection(username, password);
        }

        /**
         * Gets the log writer.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return sqlDataSource.getLogWriter();
        }

        /**
         * Sets the log writer.
         *
         * @param out the new log writer
         * @throws SQLException the SQL exception
         */
        @Override
        public void setLogWriter(final PrintWriter out) throws SQLException {
            sqlDataSource.setLogWriter(out);
        }

        /**
         * Sets the login timeout.
         *
         * @param seconds the new login timeout
         * @throws SQLException the SQL exception
         */
        @Override
        public void setLoginTimeout(final int seconds) throws SQLException {
            sqlDataSource.setLoginTimeout(seconds);
        }

        /**
         * Gets the login timeout.
         *
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public int getLoginTimeout() throws SQLException {
            return sqlDataSource.getLoginTimeout();
        }

        /**
         * Gets the parent logger.
         *
         * @return
         * @throws SQLFeatureNotSupportedException the SQL feature not supported exception
         */
        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return sqlDataSource.getParentLogger();
        }

        /**
         *
         * @param <T>
         * @param iface
         * @return
         * @throws SQLException the SQL exception
         */
        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            return sqlDataSource.unwrap(iface);
        }

        /**
         * Checks if is wrapper for.
         *
         * @param iface
         * @return true, if is wrapper for
         * @throws SQLException the SQL exception
         */
        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return sqlDataSource.isWrapperFor(iface);
        }

        /**
         * Gets the connection.
         *
         * @return
         */
        @Override
        public Connection getConnection() {
            try {
                return sqlDataSource.getConnection();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }

        /**
         * Gets the read only connection.
         *
         * @return
         */
        @Override
        public Connection getReadOnlyConnection() {
            try {
                return sqlDataSource.getConnection();
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }

        /**
         * Gets the slice selector.
         *
         * @return
         */
        @Override
        public SliceSelector getSliceSelector() {
            return sliceSelector;
        }

        /**
         * Gets the name.
         *
         * @return
         */
        @Override
        public String getName() {
            return PRIMARY;
        }

        /**
         * Gets the properties.
         *
         * @return
         */
        @Override
        public Properties<String, String> getProperties() {
            return props;
        }

        /**
         * Gets the max active.
         *
         * @return
         */
        @Override
        public int getMaxActive() {
            throw new UnsupportedOperationException();
        }

        /**
         * Gets the current active.
         *
         * @return
         */
        @Override
        public int getCurrentActive() {
            throw new UnsupportedOperationException();
        }

        /**
         * Close.
         */
        @Override
        public void close() {
            if (isClosed) {
                return;
            }

            if (closeMethod != null) {
                try {
                    ClassUtil.invokeMethod(sqlDataSource, closeMethod);
                } catch (Exception e) {
                    // ignore.
                }
            }

            isClosed = true;
        }

        /**
         * Checks if is closed.
         *
         * @return true, if is closed
         */
        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }

    /**
     * The Class SimpleDataSourceManager.
     */
    static class SimpleDataSourceManager implements DataSourceManager {

        /** The primary data source. */
        private final DataSource primaryDataSource;

        /** The active data sources. */
        private final Map<String, DataSource> activeDataSources;

        /** The props. */
        private final Properties<String, String> props = new Properties<>();

        /** The data source selector. */
        private final DataSourceSelector dataSourceSelector = new SimpleSourceSelector();

        /** The is closed. */
        private boolean isClosed = false;

        /**
         * Instantiates a new simple data source manager.
         *
         * @param ds
         */
        public SimpleDataSourceManager(final DataSource ds) {
            this.primaryDataSource = ds;

            if (N.isNullOrEmpty(ds.getName())) {
                this.activeDataSources = N.asMap(SimpleDataSource.PRIMARY, ds);
            } else {
                this.activeDataSources = N.asMap(ds.getName(), ds);
            }
        }

        /**
         * Gets the primary data source.
         *
         * @return
         */
        @Override
        public DataSource getPrimaryDataSource() {
            return primaryDataSource;
        }

        /**
         * Gets the active data source.
         *
         * @param dataSourceName
         * @return
         */
        @Override
        public DataSource getActiveDataSource(String dataSourceName) {
            return activeDataSources.get(dataSourceName);
        }

        /**
         * Gets the active data sources.
         *
         * @return
         */
        @Override
        public Map<String, DataSource> getActiveDataSources() {
            return activeDataSources;
        }

        /**
         * Gets the data source selector.
         *
         * @return
         */
        @Override
        public DataSourceSelector getDataSourceSelector() {
            return dataSourceSelector;
        }

        /**
         * Gets the properties.
         *
         * @return
         */
        @Override
        public Properties<String, String> getProperties() {
            return props;
        }

        /**
         * Close.
         */
        @Override
        public void close() {
            if (isClosed) {
                return;
            }

            primaryDataSource.close();

            isClosed = true;
        }

        /**
         * Checks if is closed.
         *
         * @return true, if is closed
         */
        @Override
        public boolean isClosed() {
            return isClosed;
        }
    }
}
