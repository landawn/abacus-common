/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.landawn.abacus.util.Properties;

public class AbacusConfig extends Properties<String, Object> {
    private Slog slog;
    private String properties;
    private String initializerOnStartup;
    private EntityManager entityManager;
    private List<EntityManager> entityManagerList = Collections.synchronizedList(new ArrayList<EntityManager>());

    public Slog getSlog() {
        return slog;
    }

    public void setSlog(Slog slog) {
        set("slog", slog);
        this.slog = slog;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        set("properties", properties);
        this.properties = properties;
    }

    public String getInitializerOnStartup() {
        return initializerOnStartup;
    }

    public void setInitializerOnStartup(String initializerOnStartup) {
        set("initializerOnStartup", initializerOnStartup);
        this.initializerOnStartup = initializerOnStartup;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        set("entityManager", entityManager);
        this.entityManager = entityManager;
        set("entityManagerList", entityManagerList);
        this.entityManagerList.add(entityManager);
    }

    public List<EntityManager> getEntityManagerList() {
        return entityManagerList;
    }

    public void setEntityManagerList(List<EntityManager> entityManagerList) {
        set("entityManagerList", entityManagerList);
        this.entityManagerList = entityManagerList;
    }

    public static class Slog extends Properties<String, Object> {
        private String open;
        private String batchSize;
        private Domain domain;
        private List<Domain> domainList = Collections.synchronizedList(new ArrayList<Domain>());
        private Table table;

        public String getOpen() {
            return open;
        }

        public void setOpen(String open) {
            set("open", open);
            this.open = open;
        }

        public String getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(String batchSize) {
            set("batchSize", batchSize);
            this.batchSize = batchSize;
        }

        public Domain getDomain() {
            return domain;
        }

        public void setDomain(Domain domain) {
            set("domain", domain);
            this.domain = domain;
            set("domainList", domainList);
            this.domainList.add(domain);
        }

        public List<Domain> getDomainList() {
            return domainList;
        }

        public void setDomainList(List<Domain> domainList) {
            set("domainList", domainList);
            this.domainList = domainList;
        }

        public Table getTable() {
            return table;
        }

        public void setTable(Table table) {
            set("table", table);
            this.table = table;
        }

        public static class Domain extends Properties<String, Object> {
            private String operationCode;

            public String getOperationCode() {
                return operationCode;
            }

            public void setOperationCode(String operationCode) {
                set("operationCode", operationCode);
                this.operationCode = operationCode;
            }
        }

        public static class Table extends Properties<String, Object> {
            private String url;
            private String driver;
            private String user;
            private String password;
            private String initialSize;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                set("url", url);
                this.url = url;
            }

            public String getDriver() {
                return driver;
            }

            public void setDriver(String driver) {
                set("driver", driver);
                this.driver = driver;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                set("user", user);
                this.user = user;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                set("password", password);
                this.password = password;
            }

            public String getInitialSize() {
                return initialSize;
            }

            public void setInitialSize(String initialSize) {
                set("initialSize", initialSize);
                this.initialSize = initialSize;
            }
        }
    }

    public static class EntityManager extends Properties<String, Object> {
        private String domainName;
        private String mode;
        private Server server;
        private EntityCache entityCache;
        private Lock lock;
        private String entityDefinition;
        private DataSourceManager dataSourceManager;

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            set("domainName", domainName);
            this.domainName = domainName;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            set("mode", mode);
            this.mode = mode;
        }

        public Server getServer() {
            return server;
        }

        public void setServer(Server server) {
            set("server", server);
            this.server = server;
        }

        public EntityCache getEntityCache() {
            return entityCache;
        }

        public void setEntityCache(EntityCache entityCache) {
            set("entityCache", entityCache);
            this.entityCache = entityCache;
        }

        public Lock getLock() {
            return lock;
        }

        public void setLock(Lock lock) {
            set("lock", lock);
            this.lock = lock;
        }

        public String getEntityDefinition() {
            return entityDefinition;
        }

        public void setEntityDefinition(String entityDefinition) {
            set("entityDefinition", entityDefinition);
            this.entityDefinition = entityDefinition;
        }

        public DataSourceManager getDataSourceManager() {
            return dataSourceManager;
        }

        public void setDataSourceManager(DataSourceManager dataSourceManager) {
            set("dataSourceManager", dataSourceManager);
            this.dataSourceManager = dataSourceManager;
        }

        public static class Server extends Properties<String, Object> {
            private String url;
            private String contentType;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                set("url", url);
                this.url = url;
            }

            public String getContentType() {
                return contentType;
            }

            public void setContentType(String contentType) {
                set("contentType", contentType);
                this.contentType = contentType;
            }
        }

        public static class EntityCache extends Properties<String, Object> {
            private String liveTime;
            private String maxIdleTime;
            private String capacity;
            private String includeEntity;
            private CustomizedEntity customizedEntity;
            private List<CustomizedEntity> customizedEntityList = Collections.synchronizedList(new ArrayList<CustomizedEntity>());

            public String getLiveTime() {
                return liveTime;
            }

            public void setLiveTime(String liveTime) {
                set("liveTime", liveTime);
                this.liveTime = liveTime;
            }

            public String getMaxIdleTime() {
                return maxIdleTime;
            }

            public void setMaxIdleTime(String maxIdleTime) {
                set("maxIdleTime", maxIdleTime);
                this.maxIdleTime = maxIdleTime;
            }

            public String getCapacity() {
                return capacity;
            }

            public void setCapacity(String capacity) {
                set("capacity", capacity);
                this.capacity = capacity;
            }

            public String getIncludeEntity() {
                return includeEntity;
            }

            public void setIncludeEntity(String includeEntity) {
                set("includeEntity", includeEntity);
                this.includeEntity = includeEntity;
            }

            public CustomizedEntity getCustomizedEntity() {
                return customizedEntity;
            }

            public void setCustomizedEntity(CustomizedEntity customizedEntity) {
                set("customizedEntity", customizedEntity);
                this.customizedEntity = customizedEntity;
                set("customizedEntityList", customizedEntityList);
                this.customizedEntityList.add(customizedEntity);
            }

            public List<CustomizedEntity> getCustomizedEntityList() {
                return customizedEntityList;
            }

            public void setCustomizedEntityList(List<CustomizedEntity> customizedEntityList) {
                set("customizedEntityList", customizedEntityList);
                this.customizedEntityList = customizedEntityList;
            }

            public static class CustomizedEntity extends Properties<String, Object> {
                private String liveTime;
                private String maxIdleTime;
                private String excludeProperty;

                public String getLiveTime() {
                    return liveTime;
                }

                public void setLiveTime(String liveTime) {
                    set("liveTime", liveTime);
                    this.liveTime = liveTime;
                }

                public String getMaxIdleTime() {
                    return maxIdleTime;
                }

                public void setMaxIdleTime(String maxIdleTime) {
                    set("maxIdleTime", maxIdleTime);
                    this.maxIdleTime = maxIdleTime;
                }

                public String getExcludeProperty() {
                    return excludeProperty;
                }

                public void setExcludeProperty(String excludeProperty) {
                    set("excludeProperty", excludeProperty);
                    this.excludeProperty = excludeProperty;
                }
            }
        }

        public static class Lock extends Properties<String, Object> {
            private String recordLockProvider;

            public String getXLockProvider() {
                return recordLockProvider;
            }

            public void setXLockProvider(String recordLockProvider) {
                set("recordLockProvider", recordLockProvider);
                this.recordLockProvider = recordLockProvider;
            }
        }

        public static class DataSourceManager extends Properties<String, Object> {
            private DataSource dataSource;

            public DataSource getDataSource() {
                return dataSource;
            }

            public void setDataSource(DataSource dataSource) {
                set("dataSource", dataSource);
                this.dataSource = dataSource;
            }

            public static class DataSource extends Properties<String, Object> {
                private String sqlLog;
                private String queryWithReadOnlyConnectionByDefault;
                private Connection connection;
                private ReadOnlyConnection readOnlyConnection;

                public String getSqlLog() {
                    return sqlLog;
                }

                public void setSqlLog(String sqlLog) {
                    set("sqlLog", sqlLog);
                    this.sqlLog = sqlLog;
                }

                public String getQueryWithReadOnlyConnectionByDefault() {
                    return queryWithReadOnlyConnectionByDefault;
                }

                public void setQueryWithReadOnlyConnectionByDefault(String queryWithReadOnlyConnectionByDefault) {
                    set("queryWithReadOnlyConnectionByDefault", queryWithReadOnlyConnectionByDefault);
                    this.queryWithReadOnlyConnectionByDefault = queryWithReadOnlyConnectionByDefault;
                }

                public Connection getConnection() {
                    return connection;
                }

                public void setConnection(Connection connection) {
                    set("connection", connection);
                    this.connection = connection;
                }

                public ReadOnlyConnection getReadOnlyConnection() {
                    return readOnlyConnection;
                }

                public void setReadOnlyConnection(ReadOnlyConnection readOnlyConnection) {
                    set("readOnlyConnection", readOnlyConnection);
                    this.readOnlyConnection = readOnlyConnection;
                }

                public static class Connection extends Properties<String, Object> {
                    private String url;
                    private String driver;
                    private String user;
                    private String password;
                    private String initialSize;
                    private String maxActive;
                    private String liveTime;
                    private String maxIdleTime;
                    private String validationQuery;

                    public String getUrl() {
                        return url;
                    }

                    public void setUrl(String url) {
                        set("url", url);
                        this.url = url;
                    }

                    public String getDriver() {
                        return driver;
                    }

                    public void setDriver(String driver) {
                        set("driver", driver);
                        this.driver = driver;
                    }

                    public String getUser() {
                        return user;
                    }

                    public void setUser(String user) {
                        set("user", user);
                        this.user = user;
                    }

                    public String getPassword() {
                        return password;
                    }

                    public void setPassword(String password) {
                        set("password", password);
                        this.password = password;
                    }

                    public String getInitialSize() {
                        return initialSize;
                    }

                    public void setInitialSize(String initialSize) {
                        set("initialSize", initialSize);
                        this.initialSize = initialSize;
                    }

                    public String getMaxActive() {
                        return maxActive;
                    }

                    public void setMaxActive(String maxActive) {
                        set("maxActive", maxActive);
                        this.maxActive = maxActive;
                    }

                    public String getLiveTime() {
                        return liveTime;
                    }

                    public void setLiveTime(String liveTime) {
                        set("liveTime", liveTime);
                        this.liveTime = liveTime;
                    }

                    public String getMaxIdleTime() {
                        return maxIdleTime;
                    }

                    public void setMaxIdleTime(String maxIdleTime) {
                        set("maxIdleTime", maxIdleTime);
                        this.maxIdleTime = maxIdleTime;
                    }

                    public String getValidationQuery() {
                        return validationQuery;
                    }

                    public void setValidationQuery(String validationQuery) {
                        set("validationQuery", validationQuery);
                        this.validationQuery = validationQuery;
                    }
                }

                public static class ReadOnlyConnection extends Properties<String, Object> {
                    private String url;
                    private String driver;
                    private String user;
                    private String password;
                    private String initialSize;
                    private String maxActive;
                    private String liveTime;
                    private String maxIdleTime;
                    private String validationQuery;

                    public String getUrl() {
                        return url;
                    }

                    public void setUrl(String url) {
                        set("url", url);
                        this.url = url;
                    }

                    public String getDriver() {
                        return driver;
                    }

                    public void setDriver(String driver) {
                        set("driver", driver);
                        this.driver = driver;
                    }

                    public String getUser() {
                        return user;
                    }

                    public void setUser(String user) {
                        set("user", user);
                        this.user = user;
                    }

                    public String getPassword() {
                        return password;
                    }

                    public void setPassword(String password) {
                        set("password", password);
                        this.password = password;
                    }

                    public String getInitialSize() {
                        return initialSize;
                    }

                    public void setInitialSize(String initialSize) {
                        set("initialSize", initialSize);
                        this.initialSize = initialSize;
                    }

                    public String getMaxActive() {
                        return maxActive;
                    }

                    public void setMaxActive(String maxActive) {
                        set("maxActive", maxActive);
                        this.maxActive = maxActive;
                    }

                    public String getLiveTime() {
                        return liveTime;
                    }

                    public void setLiveTime(String liveTime) {
                        set("liveTime", liveTime);
                        this.liveTime = liveTime;
                    }

                    public String getMaxIdleTime() {
                        return maxIdleTime;
                    }

                    public void setMaxIdleTime(String maxIdleTime) {
                        set("maxIdleTime", maxIdleTime);
                        this.maxIdleTime = maxIdleTime;
                    }

                    public String getValidationQuery() {
                        return validationQuery;
                    }

                    public void setValidationQuery(String validationQuery) {
                        set("validationQuery", validationQuery);
                        this.validationQuery = validationQuery;
                    }
                }
            }
        }
    }
}
