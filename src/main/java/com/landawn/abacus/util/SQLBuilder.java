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

import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;
import static com.landawn.abacus.util.WD._SPACE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.NonUpdatable;
import com.landawn.abacus.annotation.ReadOnly;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.condition.Between;
import com.landawn.abacus.condition.Binary;
import com.landawn.abacus.condition.Cell;
import com.landawn.abacus.condition.Condition;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.condition.Expression;
import com.landawn.abacus.condition.In;
import com.landawn.abacus.condition.Junction;
import com.landawn.abacus.condition.SubQuery;
import com.landawn.abacus.core.DirtyMarkerUtil;
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.u.Optional;

// TODO: Auto-generated Javadoc
/**
 * It's easier to write/maintain the sql by <code>SQLBuilder</code> and more efficient, comparing to write sql in plain text.
 * <br>The <code>sql()</code> or <code>pair()</code> method must be called to release resources.
 * <br />Here is a sample:
 * <p>
 * String sql = NE.insert("gui", "firstName", "lastName").into("account").sql();
 * <br />// SQL: INSERT INTO account (gui, first_name, last_name) VALUES (:gui, :firstName, :lastName)
 * </p>
 *
 * The {@code tableName} will NOT be formalized.
 * <li>{@code select(...).from(String tableName).where(...)}</li>
 * <li>{@code insert(...).into(String tableName).values(...)}</li>
 * <li>{@code update(String tableName).set(...).where(...)}</li>
 * <li>{@code deleteFrom(String tableName).where(...)}</li>
 *
 * <br />
 *
 * @author Haiyang Li
 * @see {@link com.landawn.abacus.annotation.ReadOnly}
 * @see {@link com.landawn.abacus.annotation.ReadOnlyId}
 * @see {@link com.landawn.abacus.annotation.NonUpdatable}
 * @see {@link com.landawn.abacus.annotation.Transient}
 * @see {@link com.landawn.abacus.annotation.Table}
 * @see {@link com.landawn.abacus.annotation.Column}
 * @since 0.8
 */
@SuppressWarnings("deprecation")
public abstract class SQLBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SQLBuilder.class);

    /** The Constant ALL. */
    public static final String ALL = WD.ALL;

    /** The Constant TOP. */
    public static final String TOP = WD.TOP;

    /** The Constant UNIQUE. */
    public static final String UNIQUE = WD.UNIQUE;

    /** The Constant DISTINCT. */
    public static final String DISTINCT = WD.DISTINCT;

    /** The Constant DISTINCTROW. */
    public static final String DISTINCTROW = WD.DISTINCTROW;

    /** The Constant ASTERISK. */
    public static final String ASTERISK = WD.ASTERISK;

    /** The Constant COUNT_ALL. */
    public static final String COUNT_ALL = "count(*)";

    /** The Constant _1. */
    public static final String _1 = "1";

    /** The Constant _1_list. */
    public static final List<String> _1_list = ImmutableList.of(_1);

    /** The Constant _INSERT. */
    static final char[] _INSERT = WD.INSERT.toCharArray();

    /** The Constant _SPACE_INSERT_SPACE. */
    static final char[] _SPACE_INSERT_SPACE = (WD.SPACE + WD.INSERT + WD.SPACE).toCharArray();

    /** The Constant _INTO. */
    static final char[] _INTO = WD.INTO.toCharArray();

    /** The Constant _SPACE_INTO_SPACE. */
    static final char[] _SPACE_INTO_SPACE = (WD.SPACE + WD.INTO + WD.SPACE).toCharArray();

    /** The Constant _VALUES. */
    static final char[] _VALUES = WD.VALUES.toCharArray();

    /** The Constant _SPACE_VALUES_SPACE. */
    static final char[] _SPACE_VALUES_SPACE = (WD.SPACE + WD.VALUES + WD.SPACE).toCharArray();

    /** The Constant _SELECT. */
    static final char[] _SELECT = WD.SELECT.toCharArray();

    /** The Constant _SPACE_SELECT_SPACE. */
    static final char[] _SPACE_SELECT_SPACE = (WD.SPACE + WD.SELECT + WD.SPACE).toCharArray();

    /** The Constant _FROM. */
    static final char[] _FROM = WD.FROM.toCharArray();

    /** The Constant _SPACE_FROM_SPACE. */
    static final char[] _SPACE_FROM_SPACE = (WD.SPACE + WD.FROM + WD.SPACE).toCharArray();

    /** The Constant _UPDATE. */
    static final char[] _UPDATE = WD.UPDATE.toCharArray();

    /** The Constant _SPACE_UPDATE_SPACE. */
    static final char[] _SPACE_UPDATE_SPACE = (WD.SPACE + WD.UPDATE + WD.SPACE).toCharArray();

    /** The Constant _SET. */
    static final char[] _SET = WD.SET.toCharArray();

    /** The Constant _SPACE_SET_SPACE. */
    static final char[] _SPACE_SET_SPACE = (WD.SPACE + WD.SET + WD.SPACE).toCharArray();

    /** The Constant _DELETE. */
    static final char[] _DELETE = WD.DELETE.toCharArray();

    /** The Constant _SPACE_DELETE_SPACE. */
    static final char[] _SPACE_DELETE_SPACE = (WD.SPACE + WD.DELETE + WD.SPACE).toCharArray();

    /** The Constant _JOIN. */
    static final char[] _JOIN = WD.JOIN.toCharArray();

    /** The Constant _SPACE_JOIN_SPACE. */
    static final char[] _SPACE_JOIN_SPACE = (WD.SPACE + WD.JOIN + WD.SPACE).toCharArray();

    /** The Constant _LEFT_JOIN. */
    static final char[] _LEFT_JOIN = WD.LEFT_JOIN.toCharArray();

    /** The Constant _SPACE_LEFT_JOIN_SPACE. */
    static final char[] _SPACE_LEFT_JOIN_SPACE = (WD.SPACE + WD.LEFT_JOIN + WD.SPACE).toCharArray();

    /** The Constant _RIGHT_JOIN. */
    static final char[] _RIGHT_JOIN = WD.RIGHT_JOIN.toCharArray();

    /** The Constant _SPACE_RIGHT_JOIN_SPACE. */
    static final char[] _SPACE_RIGHT_JOIN_SPACE = (WD.SPACE + WD.RIGHT_JOIN + WD.SPACE).toCharArray();

    /** The Constant _FULL_JOIN. */
    static final char[] _FULL_JOIN = WD.FULL_JOIN.toCharArray();

    /** The Constant _SPACE_FULL_JOIN_SPACE. */
    static final char[] _SPACE_FULL_JOIN_SPACE = (WD.SPACE + WD.FULL_JOIN + WD.SPACE).toCharArray();

    /** The Constant _CROSS_JOIN. */
    static final char[] _CROSS_JOIN = WD.CROSS_JOIN.toCharArray();

    /** The Constant _SPACE_CROSS_JOIN_SPACE. */
    static final char[] _SPACE_CROSS_JOIN_SPACE = (WD.SPACE + WD.CROSS_JOIN + WD.SPACE).toCharArray();

    /** The Constant _INNER_JOIN. */
    static final char[] _INNER_JOIN = WD.INNER_JOIN.toCharArray();

    /** The Constant _SPACE_INNER_JOIN_SPACE. */
    static final char[] _SPACE_INNER_JOIN_SPACE = (WD.SPACE + WD.INNER_JOIN + WD.SPACE).toCharArray();

    /** The Constant _NATURAL_JOIN. */
    static final char[] _NATURAL_JOIN = WD.NATURAL_JOIN.toCharArray();

    /** The Constant _SPACE_NATURAL_JOIN_SPACE. */
    static final char[] _SPACE_NATURAL_JOIN_SPACE = (WD.SPACE + WD.NATURAL_JOIN + WD.SPACE).toCharArray();

    /** The Constant _ON. */
    static final char[] _ON = WD.ON.toCharArray();

    /** The Constant _SPACE_ON_SPACE. */
    static final char[] _SPACE_ON_SPACE = (WD.SPACE + WD.ON + WD.SPACE).toCharArray();

    /** The Constant _USING. */
    static final char[] _USING = WD.USING.toCharArray();

    /** The Constant _SPACE_USING_SPACE. */
    static final char[] _SPACE_USING_SPACE = (WD.SPACE + WD.USING + WD.SPACE).toCharArray();

    /** The Constant _WHERE. */
    static final char[] _WHERE = WD.WHERE.toCharArray();

    /** The Constant _SPACE_WHERE_SPACE. */
    static final char[] _SPACE_WHERE_SPACE = (WD.SPACE + WD.WHERE + WD.SPACE).toCharArray();

    /** The Constant _GROUP_BY. */
    static final char[] _GROUP_BY = WD.GROUP_BY.toCharArray();

    /** The Constant _SPACE_GROUP_BY_SPACE. */
    static final char[] _SPACE_GROUP_BY_SPACE = (WD.SPACE + WD.GROUP_BY + WD.SPACE).toCharArray();

    /** The Constant _HAVING. */
    static final char[] _HAVING = WD.HAVING.toCharArray();

    /** The Constant _SPACE_HAVING_SPACE. */
    static final char[] _SPACE_HAVING_SPACE = (WD.SPACE + WD.HAVING + WD.SPACE).toCharArray();

    /** The Constant _ORDER_BY. */
    static final char[] _ORDER_BY = WD.ORDER_BY.toCharArray();

    /** The Constant _SPACE_ORDER_BY_SPACE. */
    static final char[] _SPACE_ORDER_BY_SPACE = (WD.SPACE + WD.ORDER_BY + WD.SPACE).toCharArray();

    /** The Constant _LIMIT. */
    static final char[] _LIMIT = (WD.SPACE + WD.LIMIT + WD.SPACE).toCharArray();

    /** The Constant _SPACE_LIMIT_SPACE. */
    static final char[] _SPACE_LIMIT_SPACE = (WD.SPACE + WD.LIMIT + WD.SPACE).toCharArray();

    /** The Constant _OFFSET. */
    static final char[] _OFFSET = WD.OFFSET.toCharArray();

    /** The Constant _SPACE_OFFSET_SPACE. */
    static final char[] _SPACE_OFFSET_SPACE = (WD.SPACE + WD.OFFSET + WD.SPACE).toCharArray();

    /** The Constant _AND. */
    static final char[] _AND = WD.AND.toCharArray();

    /** The Constant _SPACE_AND_SPACE. */
    static final char[] _SPACE_AND_SPACE = (WD.SPACE + WD.AND + WD.SPACE).toCharArray();

    /** The Constant _OR. */
    static final char[] _OR = WD.OR.toCharArray();

    /** The Constant _SPACE_OR_SPACE. */
    static final char[] _SPACE_OR_SPACE = (WD.SPACE + WD.OR + WD.SPACE).toCharArray();

    /** The Constant _UNION. */
    static final char[] _UNION = WD.UNION.toCharArray();

    /** The Constant _SPACE_UNION_SPACE. */
    static final char[] _SPACE_UNION_SPACE = (WD.SPACE + WD.UNION + WD.SPACE).toCharArray();

    /** The Constant _UNION_ALL. */
    static final char[] _UNION_ALL = WD.UNION_ALL.toCharArray();

    /** The Constant _SPACE_UNION_ALL_SPACE. */
    static final char[] _SPACE_UNION_ALL_SPACE = (WD.SPACE + WD.UNION_ALL + WD.SPACE).toCharArray();

    /** The Constant _INTERSECT. */
    static final char[] _INTERSECT = WD.INTERSECT.toCharArray();

    /** The Constant _SPACE_INTERSECT_SPACE. */
    static final char[] _SPACE_INTERSECT_SPACE = (WD.SPACE + WD.INTERSECT + WD.SPACE).toCharArray();

    /** The Constant _EXCEPT. */
    static final char[] _EXCEPT = WD.EXCEPT.toCharArray();

    /** The Constant _SPACE_EXCEPT_SPACE. */
    static final char[] _SPACE_EXCEPT_SPACE = (WD.SPACE + WD.EXCEPT + WD.SPACE).toCharArray();

    /** The Constant _EXCEPT2. */
    static final char[] _EXCEPT2 = WD.EXCEPT2.toCharArray();

    /** The Constant _SPACE_EXCEPT2_SPACE. */
    static final char[] _SPACE_EXCEPT2_SPACE = (WD.SPACE + WD.EXCEPT2 + WD.SPACE).toCharArray();

    /** The Constant _AS. */
    static final char[] _AS = WD.AS.toCharArray();

    /** The Constant _SPACE_AS_SPACE. */
    static final char[] _SPACE_AS_SPACE = (WD.SPACE + WD.AS + WD.SPACE).toCharArray();

    /** The Constant _SPACE_EQUAL_SPACE. */
    static final char[] _SPACE_EQUAL_SPACE = (WD.SPACE + WD.EQUAL + WD.SPACE).toCharArray();

    /** The Constant _SPACE_FOR_UPDATE. */
    static final char[] _SPACE_FOR_UPDATE = (WD.SPACE + WD.FOR_UPDATE).toCharArray();

    /** The Constant _COMMA_SPACE. */
    static final char[] _COMMA_SPACE = WD.COMMA_SPACE.toCharArray();

    /** The Constant SPACE_AS_SPACE. */
    static final String SPACE_AS_SPACE = WD.SPACE + WD.AS + WD.SPACE;

    /** The Constant entityTablePropColumnNameMap. */
    private static final Map<Class<?>, Map<String, String>> entityTablePropColumnNameMap = new ObjectPool<>(1024);

    /** The Constant subEntityPropNamesPool. */
    private static final Map<Class<?>, Set<String>> subEntityPropNamesPool = new ObjectPool<>(1024);

    /** The Constant nonSubEntityPropNamesPool. */
    private static final Map<Class<?>, Set<String>> nonSubEntityPropNamesPool = new ObjectPool<>(1024);

    /** The Constant defaultPropNamesPool. */
    private static final Map<Class<?>, Set<String>[]> defaultPropNamesPool = new ObjectPool<>(1024);

    /** The Constant tableDeleteFrom. */
    private static final Map<String, char[]> tableDeleteFrom = new ConcurrentHashMap<>();

    /** The Constant activeStringBuilderCounter. */
    private static final AtomicInteger activeStringBuilderCounter = new AtomicInteger();

    /** The naming policy. */
    private final NamingPolicy namingPolicy;

    /** The sql policy. */
    private final SQLPolicy sqlPolicy;

    /** The parameters. */
    private final List<Object> parameters = new ArrayList<>();

    /** The sb. */
    private StringBuilder sb;

    /** The entity class. */
    private Class<?> entityClass;

    /** The op. */
    private OperationType op;

    /** The table name. */
    private String tableName;

    /** The predicates. */
    private String predicates;

    /** The column names. */
    private String[] columnNames;

    /** The column name list. */
    private Collection<String> columnNameList;

    /** The column aliases. */
    private Map<String, String> columnAliases;

    /** The props. */
    private Map<String, Object> props;

    /** The props list. */
    private Collection<Map<String, Object>> propsList;

    /**
     * Instantiates a new SQL builder.
     *
     * @param namingPolicy
     * @param sqlPolicy
     */
    SQLBuilder(final NamingPolicy namingPolicy, final SQLPolicy sqlPolicy) {
        if (activeStringBuilderCounter.incrementAndGet() > 1024) {
            logger.error("Too many(" + activeStringBuilderCounter.get()
                    + ") StringBuilder instances are created in SQLBuilder. The method sql()/pair() must be called to release resources and close SQLBuilder");
        }

        this.sb = Objectory.createStringBuilder();

        this.namingPolicy = namingPolicy == null ? NamingPolicy.LOWER_CASE_WITH_UNDERSCORE : namingPolicy;
        this.sqlPolicy = sqlPolicy == null ? SQLPolicy.SQL : sqlPolicy;
    }

    /**
     * Register entity prop column name map.
     *
     * @param entityClass annotated with @Table, @Column
     */
    static void registerEntityPropColumnNameMap(final Class<?> entityClass) {
        N.checkArgNotNull(entityClass);

        final Set<Field> allFields = N.newHashSet();

        for (Class<?> superClass : ClassUtil.getAllSuperclasses(entityClass)) {
            allFields.addAll(Array.asList(superClass.getDeclaredFields()));
        }

        allFields.addAll(Array.asList(entityClass.getDeclaredFields()));

        final Map<String, String> propColumnNameMap = new HashMap<>();
        Method getterMethod = null;

        for (Field field : allFields) {
            getterMethod = ClassUtil.getPropGetMethod(entityClass, field.getName());

            if (getterMethod != null) {
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
                    propColumnNameMap.put(ClassUtil.getPropNameByMethod(getterMethod), columnName);
                }
            }
        }

        final Map<String, String> tmp = entityTablePropColumnNameMap.get(entityClass);

        if (N.notNullOrEmpty(tmp)) {
            propColumnNameMap.putAll(tmp);
        }

        if (N.isNullOrEmpty(propColumnNameMap)) {
            entityTablePropColumnNameMap.put(entityClass, N.<String, String> emptyMap());
        } else {
            entityTablePropColumnNameMap.put(entityClass, propColumnNameMap);
        }
    }

    /**
     * Register non sub entity prop names.
     *
     * @param entityClass
     * @param nonSubEntityPropNames
     */
    public static void registerNonSubEntityPropNames(final Class<?> entityClass, final Collection<String> nonSubEntityPropNames) {
        final Set<String> set = ImmutableSet.copyOf(nonSubEntityPropNames);

        synchronized (entityClass) {
            nonSubEntityPropNamesPool.put(entityClass, set);

            subEntityPropNamesPool.remove(entityClass);
            defaultPropNamesPool.remove(entityClass);
        }
    }

    /** The Constant classTableNameMap. */
    private static final Map<Class<?>, String[]> classTableNameMap = new ConcurrentHashMap<>();

    /**
     * Gets the table name.
     *
     * @param entityClass
     * @param namingPolicy
     * @return
     */
    static String getTableName(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        String[] entityTableNames = classTableNameMap.get(entityClass);

        if (entityTableNames == null) {
            if (entityClass.isAnnotationPresent(Table.class)) {
                entityTableNames = Array.repeat(entityClass.getAnnotation(Table.class).value(), 3);
            } else {
                try {
                    if (entityClass.isAnnotationPresent(javax.persistence.Table.class)) {
                        entityTableNames = Array.repeat(entityClass.getAnnotation(javax.persistence.Table.class).name(), 3);
                    }
                } catch (Throwable e) {
                    logger.warn("To support javax.persistence.Table/Column, please add dependence javax.persistence:persistence-api");
                }
            }

            if (entityTableNames == null) {
                final String simpleClassName = ClassUtil.getSimpleClassName(entityClass);
                entityTableNames = new String[] { ClassUtil.toLowerCaseWithUnderscore(simpleClassName), ClassUtil.toUpperCaseWithUnderscore(simpleClassName),
                        ClassUtil.toCamelCase(simpleClassName) };
            }

            classTableNameMap.put(entityClass, entityTableNames);
        }

        switch (namingPolicy) {
            case LOWER_CASE_WITH_UNDERSCORE:
                return entityTableNames[0];

            case UPPER_CASE_WITH_UNDERSCORE:
                return entityTableNames[1];

            default:
                return entityTableNames[2];
        }
    }

    /**
     * Gets the select prop names by class.
     *
     * @param entityClass
     * @param includeSubEntityProperties
     * @param excludedPropNames
     * @return
     */
    static Collection<String> getSelectPropNamesByClass(final Class<?> entityClass, final boolean includeSubEntityProperties,
            final Set<String> excludedPropNames) {
        final Collection<String>[] val = loadPropNamesByClass(entityClass);
        final Collection<String> propNames = includeSubEntityProperties ? val[0] : val[1];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        } else {
            final List<String> tmp = new ArrayList<>(propNames);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }
    }

    /**
     * Gets the insert prop names by class.
     *
     * @param entity
     * @param excludedPropNames
     * @return
     */
    static Collection<String> getInsertPropNamesByClass(final Object entity, final Set<String> excludedPropNames) {
        final Class<?> entityClass = entity.getClass();
        final Collection<String>[] val = loadPropNamesByClass(entityClass);

        if (N.isNullOrEmpty(excludedPropNames)) {
            final Collection<String> idPropNames = ClassUtil.getIdFieldNames(entityClass);

            if (N.isNullOrEmpty(idPropNames)) {
                return val[2];
            } else {
                for (String idPropName : idPropNames) {
                    if (JdbcUtil.isDefaultIdPropValue(ClassUtil.getPropValue(entity, idPropName))) {
                        return val[3];
                    }
                }

                return val[2];
            }
        } else {
            final List<String> tmp = new ArrayList<>(val[2]);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }
    }

    /**
     * Gets the insert prop names by class.
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    static Collection<String> getInsertPropNamesByClass(final Class<?> entityClass, final Set<String> excludedPropNames) {
        final Collection<String>[] val = loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[2];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        } else {
            final List<String> tmp = new ArrayList<>(propNames);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }
    }

    /**
     * Gets the update prop names by class.
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    static Collection<String> getUpdatePropNamesByClass(final Class<?> entityClass, final Set<String> excludedPropNames) {
        final Collection<String>[] val = loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[4];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        } else {
            final List<String> tmp = new ArrayList<>(propNames);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }
    }

    /**
     * Gets the delete prop names by class.
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    static Collection<String> getDeletePropNamesByClass(final Class<?> entityClass, final Set<String> excludedPropNames) {
        if (N.isNullOrEmpty(excludedPropNames)) {
            return N.emptyList();
        }

        final Collection<String>[] val = loadPropNamesByClass(entityClass);
        final Collection<String> propNames = val[0];

        if (N.isNullOrEmpty(excludedPropNames)) {
            return propNames;
        } else {
            final List<String> tmp = new ArrayList<>(propNames);
            tmp.removeAll(excludedPropNames);
            return tmp;
        }
    }

    /**
     * Load prop names by class.
     *
     * @param entityClass
     * @return
     */
    static Collection<String>[] loadPropNamesByClass(final Class<?> entityClass) {
        Set<String>[] val = defaultPropNamesPool.get(entityClass);

        if (val == null) {
            synchronized (entityClass) {
                final Set<String> entityPropNames = N.newLinkedHashSet(ClassUtil.getPropGetMethodList(entityClass).keySet());
                final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

                if (N.notNullOrEmpty(subEntityPropNames)) {
                    entityPropNames.removeAll(subEntityPropNames);
                }

                val = new Set[5];
                val[0] = N.newLinkedHashSet(entityPropNames);
                val[1] = N.newLinkedHashSet(entityPropNames);
                val[2] = N.newLinkedHashSet(entityPropNames);
                val[3] = N.newLinkedHashSet(entityPropNames);
                val[4] = N.newLinkedHashSet(entityPropNames);

                Method method = null;
                Class<?> subEntityClass = null;
                String subEntityClassName = null;
                Set<String> subEntityPropNameList = null;

                for (String subEntityPropName : subEntityPropNames) {
                    method = ClassUtil.getPropGetMethod(entityClass, subEntityPropName);
                    subEntityClass = ClassUtil.isEntity(method.getReturnType()) ? method.getReturnType() : ClassUtil.getTypeArgumentsByMethod(method)[0];
                    subEntityClassName = ClassUtil.getSimpleClassName(subEntityClass);

                    subEntityPropNameList = N.newLinkedHashSet(ClassUtil.getPropGetMethodList(subEntityClass).keySet());
                    subEntityPropNameList.removeAll(getSubEntityPropNames(subEntityClass));

                    for (String pn : subEntityPropNameList) {
                        val[0].add(StringUtil.concat(subEntityClassName, WD.PERIOD, pn));
                    }
                }

                final Set<String> readOnlyPropNames = N.newHashSet();
                final Set<String> nonUpdatablePropNames = N.newHashSet();
                final Set<String> transientPropNames = N.newHashSet();

                final Set<Field> allFields = N.newHashSet();

                for (Class<?> superClass : ClassUtil.getAllSuperclasses(entityClass)) {
                    allFields.addAll(Array.asList(superClass.getDeclaredFields()));
                }

                allFields.addAll(Array.asList(entityClass.getDeclaredFields()));

                for (Field field : allFields) {
                    if (ClassUtil.getPropGetMethod(entityClass, field.getName()) == null
                            && ClassUtil.getPropGetMethod(entityClass, ClassUtil.formalizePropName(field.getName())) == null) {
                        continue;
                    }

                    if (field.isAnnotationPresent(ReadOnly.class) || field.isAnnotationPresent(ReadOnlyId.class)) {
                        readOnlyPropNames.add(field.getName());
                    }

                    if (field.isAnnotationPresent(NonUpdatable.class)) {
                        nonUpdatablePropNames.add(field.getName());
                    }

                    if (field.isAnnotationPresent(Transient.class) || Modifier.isTransient(field.getModifiers())) {
                        readOnlyPropNames.add(field.getName());

                        transientPropNames.add(field.getName());
                        transientPropNames.add(ClassUtil.formalizePropName(field.getName()));
                    }
                }

                nonUpdatablePropNames.addAll(readOnlyPropNames);

                val[0].removeAll(transientPropNames);
                val[1].removeAll(transientPropNames);
                val[2].removeAll(readOnlyPropNames);
                val[3].removeAll(readOnlyPropNames);
                val[4].removeAll(nonUpdatablePropNames);

                for (String idPropName : ClassUtil.getIdFieldNames(entityClass)) {
                    val[3].remove(idPropName);
                    val[3].remove(ClassUtil.getPropNameByMethod(ClassUtil.getPropGetMethod(entityClass, idPropName)));
                }

                val[0] = ImmutableSet.of(val[0]);
                val[1] = ImmutableSet.of(val[1]);
                val[2] = ImmutableSet.of(val[2]);
                val[3] = ImmutableSet.of(val[3]);
                val[4] = ImmutableSet.of(val[4]);

                defaultPropNamesPool.put(entityClass, val);
            }
        }

        return val;
    }

    /**
     * Gets the sub entity prop names.
     *
     * @param entityClass
     * @return
     */
    static Set<String> getSubEntityPropNames(final Class<?> entityClass) {
        synchronized (entityClass) {
            Set<String> subEntityPropNames = subEntityPropNamesPool.get(entityClass);

            if (subEntityPropNames == null) {
                subEntityPropNames = N.newLinkedHashSet();
                final Map<String, Method> propMethods = ClassUtil.getPropGetMethodList(entityClass);
                final Set<String> nonSubEntityPropNames = nonSubEntityPropNamesPool.get(entityClass);
                Class<?>[] typeParameterClasses = null;

                for (Map.Entry<String, Method> entry : propMethods.entrySet()) {
                    if ((ClassUtil.isEntity(entry.getValue().getReturnType())
                            || (N.notNullOrEmpty(typeParameterClasses = ClassUtil.getTypeArgumentsByMethod(entry.getValue()))
                                    && ClassUtil.isEntity(typeParameterClasses[0])))
                            && (nonSubEntityPropNames == null || !nonSubEntityPropNames.contains(entry.getKey()))) {
                        subEntityPropNames.add(entry.getKey());
                    }
                }
            }

            subEntityPropNamesPool.put(entityClass, subEntityPropNames);
            return subEntityPropNames;
        }
    }

    /**
     * Gets the select table names.
     *
     * @param entityClass
     * @param namingPolicy
     * @return
     */
    private static List<String> getSelectTableNames(final Class<?> entityClass, final NamingPolicy namingPolicy) {
        final Set<String> subEntityPropNames = getSubEntityPropNames(entityClass);

        if (N.isNullOrEmpty(subEntityPropNames)) {
            return N.emptyList();
        }

        final List<String> res = new ArrayList<>(subEntityPropNames.size() + 1);
        res.add(getTableName(entityClass, namingPolicy));

        Method method = null;
        Class<?> subEntityClass = null;

        for (String subEntityPropName : subEntityPropNames) {
            method = ClassUtil.getPropGetMethod(entityClass, subEntityPropName);
            subEntityClass = ClassUtil.isEntity(method.getReturnType()) ? method.getReturnType() : ClassUtil.getTypeArgumentsByMethod(method)[0];
            res.add(ClassUtil.getSimpleClassName(subEntityClass));
        }

        return res;
    }

    //    /**
    //     * Register the irregular column names which can not be converted from property name by naming policy.
    //     *
    //     * @param propNameTableInterface the interface generated by <code>com.landawn.abacus.util.CodeGenerator</code>
    //     */
    //    public static void registerColumnName(final Class<?> propNameTableInterface) {
    //        final String PCM = "_PCM";
    //
    //        try {
    //            final Map<String, String> _pcm = (Map<String, String>) propNameTableInterface.getField(PCM).get(null);
    //
    //            for (Class<?> cls : propNameTableInterface.getDeclaredClasses()) {
    //                final String entityName = (String) cls.getField(D.UNDERSCORE).get(null);
    //                final Map<String, String> entityPCM = (Map<String, String>) cls.getField(PCM).get(null);
    //
    //                final Map<String, String> propColumnNameMap = new HashMap<>(_pcm);
    //                propColumnNameMap.putAll(entityPCM);
    //
    //                registerColumnName(entityName, propColumnNameMap);
    //            }
    //        } catch (Exception e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }

    //    /**
    //     * Returns an immutable list of the property name by the specified entity class.
    //     *
    //     * @param entityClass
    //     * @return
    //     */
    //    public static List<String> propNameList(final Class<?> entityClass) {
    //        List<String> propNameList = classPropNameListPool.get(entityClass);
    //
    //        if (propNameList == null) {
    //            synchronized (classPropNameListPool) {
    //                propNameList = classPropNameListPool.get(entityClass);
    //
    //                if (propNameList == null) {
    //                    propNameList = N.asImmutableList(new ArrayList<>(N.getPropGetMethodList(entityClass).keySet()));
    //                    classPropNameListPool.put(entityClass, propNameList);
    //                }
    //            }
    //        }
    //
    //        return propNameList;
    //    }

    //    /**
    //     * Returns an immutable set of the property name by the specified entity class.
    //     *
    //     * @param entityClass
    //     * @return
    //     */
    //    public static Set<String> propNameSet(final Class<?> entityClass) {
    //        Set<String> propNameSet = classPropNameSetPool.get(entityClass);
    //
    //        if (propNameSet == null) {
    //            synchronized (classPropNameSetPool) {
    //                propNameSet = classPropNameSetPool.get(entityClass);
    //
    //                if (propNameSet == null) {
    //                    propNameSet = N.asImmutableSet(N.newLinkedHashSet(N.getPropGetMethodList(entityClass).keySet()));
    //                    classPropNameSetPool.put(entityClass, propNameSet);
    //                }
    //            }
    //        }
    //
    //        return propNameSet;
    //    }

    /**
     *
     * @param propNames
     * @return
     */
    @Beta
    static Map<String, Expression> named(final String... propNames) {
        final Map<String, Expression> m = new LinkedHashMap<>(N.initHashCapacity(propNames.length));

        for (String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /**
     *
     * @param propNames
     * @return
     */
    @Beta
    static Map<String, Expression> named(final Collection<String> propNames) {
        final Map<String, Expression> m = new LinkedHashMap<>(N.initHashCapacity(propNames.size()));

        for (String propName : propNames) {
            m.put(propName, CF.QME);
        }

        return m;
    }

    /** The Constant QM_CACHE. */
    private static final Map<Integer, String> QM_CACHE = new HashMap<>();

    static {
        for (int i = 0; i <= 30; i++) {
            QM_CACHE.put(i, StringUtil.repeat("?", i, ", "));
        }

        QM_CACHE.put(100, StringUtil.repeat("?", 100, ", "));
        QM_CACHE.put(200, StringUtil.repeat("?", 200, ", "));
        QM_CACHE.put(300, StringUtil.repeat("?", 300, ", "));
        QM_CACHE.put(500, StringUtil.repeat("?", 500, ", "));
        QM_CACHE.put(1000, StringUtil.repeat("?", 1000, ", "));
    }

    /**
     * Repeat question mark({@code ?}) {@code n} times with delimiter {@code ", "}.
     * <br />
     * It's designed for batch SQL builder.
     *
     * @param n
     * @return
     */
    public static String repeatQM(int n) {
        N.checkArgNotNegative(n, "count");

        String result = QM_CACHE.get(n);

        if (result == null) {
            result = StringUtil.repeat("?", n, ", ");
        }

        return result;
    }

    /**
     *
     * @param tableName
     * @return
     */
    public SQLBuilder into(final String tableName) {
        if (op != OperationType.ADD) {
            throw new AbacusException("Invalid operation: " + op);
        }

        if (N.isNullOrEmpty(columnNames) && N.isNullOrEmpty(columnNameList) && N.isNullOrEmpty(props) && N.isNullOrEmpty(propsList)) {
            throw new AbacusException("Column names or props must be set first by insert");
        }

        this.tableName = tableName;

        sb.append(_INSERT);
        sb.append(_SPACE_INTO_SPACE);

        sb.append(tableName);

        sb.append(WD._SPACE);
        sb.append(WD._PARENTHESES_L);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();

        if (N.notNullOrEmpty(columnNames)) {
            if (columnNames.length == 1 && columnNames[0].indexOf(WD._SPACE) > 0) {
                sb.append(columnNames[0]);
            } else {
                for (int i = 0, len = columnNames.length; i < len; i++) {
                    if (i > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));
                }
            }
        } else if (N.notNullOrEmpty(columnNameList)) {
            int i = 0;
            for (String columnName : columnNameList) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, columnName));
            }
        } else {
            final Map<String, Object> props = N.isNullOrEmpty(this.props) ? propsList.iterator().next() : this.props;

            int i = 0;
            for (String columnName : props.keySet()) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, columnName));
            }
        }

        sb.append(WD._PARENTHESES_R);

        sb.append(_SPACE_VALUES_SPACE);

        sb.append(WD._PARENTHESES_L);

        if (N.notNullOrEmpty(columnNames)) {
            switch (sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(WD._QUESTION_MARK);
                    }

                    break;
                }

                case NAMED_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(":");
                        sb.append(columnNames[i]);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append("#{");
                        sb.append(columnNames[i]);
                        sb.append('}');
                    }

                    break;
                }

                default:
                    throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
            }
        } else if (N.notNullOrEmpty(columnNameList)) {
            switch (sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, size = columnNameList.size(); i < size; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(WD._QUESTION_MARK);
                    }

                    break;
                }

                case NAMED_SQL: {
                    int i = 0;
                    for (String columnName : columnNameList) {
                        if (i++ > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(":");
                        sb.append(columnName);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    int i = 0;
                    for (String columnName : columnNameList) {
                        if (i++ > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append("#{");
                        sb.append(columnName);
                        sb.append('}');
                    }

                    break;
                }

                default:
                    throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
            }
        } else if (N.notNullOrEmpty(props)) {
            appendInsertProps(props);
        } else {
            int i = 0;
            for (Map<String, Object> props : propsList) {
                if (i++ > 0) {
                    sb.append(WD._PARENTHESES_R);
                    sb.append(_COMMA_SPACE);
                    sb.append(WD._PARENTHESES_L);
                }

                appendInsertProps(props);
            }
        }

        sb.append(WD._PARENTHESES_R);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder into(final Class<?> entityClass) {
        if (this.entityClass != null) {
            this.entityClass = entityClass;
        }

        return into(getTableName(entityClass, namingPolicy));
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder from(String expr) {
        expr = expr.trim();
        String tableName = expr.indexOf(WD._COMMA) > 0 ? StringUtil.substring(expr, 0, WD._COMMA).get() : expr;

        if (tableName.indexOf(WD._SPACE) > 0) {
            tableName = StringUtil.substring(tableName, 0, WD._SPACE).get();
        }

        return from(tableName.trim(), expr);
    }

    /**
     *
     * @param tableNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder from(final String... tableNames) {
        if (tableNames.length == 1) {
            return from(tableNames[0].trim());
        }

        final String tableName = tableNames[0].trim();
        return from(tableName, StringUtil.join(tableNames, WD.COMMA_SPACE));
    }

    /**
     *
     * @param tableNames
     * @return
     */
    public SQLBuilder from(final Collection<String> tableNames) {
        if (tableNames.size() == 1) {
            return from(tableNames.iterator().next().trim());
        }

        final String tableName = tableNames.iterator().next().trim();
        return from(tableName, StringUtil.join(tableNames, WD.COMMA_SPACE));
    }

    /**
     *
     * @param tableAliases
     * @return
     */
    public SQLBuilder from(final Map<String, String> tableAliases) {
        final String tableName = tableAliases.keySet().iterator().next().trim();

        return from(tableName, StringUtil.joinEntries(tableAliases, WD.COMMA_SPACE, " "));
    }

    /**
     *
     * @param tableName
     * @param fromCause
     * @return
     */
    private SQLBuilder from(final String tableName, final String fromCause) {
        if (op != OperationType.QUERY) {
            throw new AbacusException("Invalid operation: " + op);
        }

        if (N.isNullOrEmpty(columnNames) && N.isNullOrEmpty(columnNameList) && N.isNullOrEmpty(columnAliases)) {
            throw new AbacusException("Column names or props must be set first by select");
        }

        this.tableName = tableName;

        sb.append(_SELECT);
        sb.append(WD._SPACE);

        if (N.notNullOrEmpty(predicates)) {
            sb.append(predicates);
            sb.append(WD._SPACE);
        }

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();

        if (N.notNullOrEmpty(columnNames)) {
            if (columnNames.length == 1) {
                final String columnName = StringUtil.trim(columnNames[0]);
                int idx = columnName.indexOf(' ');

                if (idx < 0) {
                    idx = columnName.indexOf(',');
                }

                if (idx > 0) {
                    sb.append(columnName);
                } else {
                    sb.append(formalizeColumnName(propColumnNameMap, columnName));

                    if (namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && !WD.ASTERISK.equals(columnName)) {
                        sb.append(_SPACE_AS_SPACE);

                        sb.append(WD._QUOTATION_D);
                        sb.append(columnName);
                        sb.append(WD._QUOTATION_D);
                    }
                }
            } else {
                String columnName = null;

                for (int i = 0, len = columnNames.length; i < len; i++) {
                    columnName = StringUtil.trim(columnNames[i]);

                    if (i > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    int idx = columnName.indexOf(' ');

                    if (idx > 0) {
                        int idx2 = columnName.indexOf(" AS ", idx);

                        if (idx2 < 0) {
                            idx2 = columnName.indexOf(" as ", idx);
                        }

                        sb.append(formalizeColumnName(propColumnNameMap, columnName.substring(0, idx).trim()));

                        sb.append(_SPACE_AS_SPACE);

                        sb.append(WD._QUOTATION_D);
                        sb.append(columnName.substring(idx2 > 0 ? idx2 + 4 : idx + 1).trim());
                        sb.append(WD._QUOTATION_D);
                    } else {
                        sb.append(formalizeColumnName(propColumnNameMap, columnName));

                        if (namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && !WD.ASTERISK.equals(columnName)) {
                            sb.append(_SPACE_AS_SPACE);

                            sb.append(WD._QUOTATION_D);
                            sb.append(columnName);
                            sb.append(WD._QUOTATION_D);
                        }
                    }
                }
            }
        } else if (N.notNullOrEmpty(columnNameList)) {
            int i = 0;
            for (String columnName : columnNameList) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, columnName));

                if (namingPolicy != NamingPolicy.LOWER_CAMEL_CASE && !WD.ASTERISK.equals(columnName)) {
                    sb.append(_SPACE_AS_SPACE);

                    sb.append(WD._QUOTATION_D);
                    sb.append(columnName);
                    sb.append(WD._QUOTATION_D);
                }
            }
        } else {
            int i = 0;
            for (Map.Entry<String, String> entry : columnAliases.entrySet()) {
                if (i++ > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

                if (N.notNullOrEmpty(entry.getValue())) {
                    sb.append(_SPACE_AS_SPACE);

                    sb.append(WD._QUOTATION_D);
                    sb.append(entry.getValue());
                    sb.append(WD._QUOTATION_D);
                }
            }
        }

        sb.append(_SPACE_FROM_SPACE);

        sb.append(fromCause);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder from(final Class<?> entityClass) {
        if (this.entityClass != null) {
            this.entityClass = entityClass;
        }

        return from(getTableName(entityClass, namingPolicy));
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder join(final String expr) {
        sb.append(_SPACE_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder join(final Class<?> entityClass) {
        sb.append(_SPACE_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder innerJoin(final String expr) {
        sb.append(_SPACE_INNER_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder innerJoin(final Class<?> entityClass) {
        sb.append(_SPACE_INNER_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder leftJoin(final String expr) {
        sb.append(_SPACE_LEFT_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder leftJoin(final Class<?> entityClass) {
        sb.append(_SPACE_LEFT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder rightJoin(final String expr) {
        sb.append(_SPACE_RIGHT_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder rightJoin(final Class<?> entityClass) {
        sb.append(_SPACE_RIGHT_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder fullJoin(final String expr) {
        sb.append(_SPACE_FULL_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder fullJoin(final Class<?> entityClass) {
        sb.append(_SPACE_FULL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder crossJoin(final String expr) {
        sb.append(_SPACE_CROSS_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder crossJoin(final Class<?> entityClass) {
        sb.append(_SPACE_CROSS_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder naturalJoin(final String expr) {
        sb.append(_SPACE_NATURAL_JOIN_SPACE);

        sb.append(expr);

        return this;
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder naturalJoin(final Class<?> entityClass) {
        sb.append(_SPACE_NATURAL_JOIN_SPACE);

        sb.append(getTableName(entityClass, namingPolicy));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder on(final String expr) {
        sb.append(_SPACE_ON_SPACE);

        appendStringExpr(expr);

        return this;
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     */
    public SQLBuilder on(final Condition cond) {
        sb.append(_SPACE_ON_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder using(final String expr) {
        sb.append(_SPACE_USING_SPACE);

        sb.append(formalizeColumnName(expr));

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder where(final String expr) {
        init(true);

        sb.append(_SPACE_WHERE_SPACE);

        appendStringExpr(expr);

        return this;
    }

    /**
     * Append string expr.
     *
     * @param expr
     */
    private void appendStringExpr(final String expr) {
        final Map<String, String> propColumnNameMap = getPropColumnNameMap();
        final List<String> words = SQLParser.parse(expr);

        String word = null;
        for (int i = 0, len = words.size(); i < len; i++) {
            word = words.get(i);

            if (!StringUtil.isAsciiAlpha(word.charAt(0))) {
                sb.append(word);
            } else if (SQLParser.isFunctionName(words, len, i)) {
                sb.append(word);
            } else {
                sb.append(formalizeColumnName(propColumnNameMap, word));
            }
        }
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     */
    public SQLBuilder where(final Condition cond) {
        init(true);

        sb.append(_SPACE_WHERE_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder groupBy(final String expr) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        if (expr.indexOf(WD._SPACE) > 0) {
            // sb.append(columnNames[0]);
            appendStringExpr(expr);
        } else {
            sb.append(formalizeColumnName(expr));
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder groupBy(final String... columnNames) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        if (columnNames.length == 1) {
            if (columnNames[0].indexOf(WD._SPACE) > 0) {
                // sb.append(columnNames[0]);
                appendStringExpr(columnNames[0]);
            } else {
                sb.append(formalizeColumnName(columnNames[0]));
            }
        } else {
            final Map<String, String> propColumnNameMap = getPropColumnNameMap();

            for (int i = 0, len = columnNames.length; i < len; i++) {
                if (i > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));
            }
        }

        return this;
    }

    /**
     *
     * @param columnName
     * @param direction
     * @return
     */
    public SQLBuilder groupBy(final String columnName, final SortDirection direction) {
        groupBy(columnName);

        sb.append(WD._SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder groupBy(final Collection<String> columnNames) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();
        int i = 0;
        for (String columnName : columnNames) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(formalizeColumnName(propColumnNameMap, columnName));
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @param direction
     * @return
     */
    public SQLBuilder groupBy(final Collection<String> columnNames, final SortDirection direction) {
        groupBy(columnNames);

        sb.append(WD._SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder groupBy(final Map<String, SortDirection> orders) {
        sb.append(_SPACE_GROUP_BY_SPACE);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();
        int i = 0;
        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

            sb.append(WD._SPACE);
            sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder having(final String expr) {
        sb.append(_SPACE_HAVING_SPACE);

        appendStringExpr(expr);

        return this;
    }

    /**
     *
     * @param cond any literal written in <code>Expression</code> condition won't be formalized
     * @return
     */
    public SQLBuilder having(final Condition cond) {
        sb.append(_SPACE_HAVING_SPACE);

        appendCondition(cond);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder orderBy(final String expr) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        if (expr.indexOf(WD._SPACE) > 0) {
            // sb.append(columnNames[0]);
            appendStringExpr(expr);
        } else {
            sb.append(formalizeColumnName(expr));
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder orderBy(final String... columnNames) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        if (columnNames.length == 1) {
            if (columnNames[0].indexOf(WD._SPACE) > 0) {
                // sb.append(columnNames[0]);
                appendStringExpr(columnNames[0]);
            } else {
                sb.append(formalizeColumnName(columnNames[0]));
            }
        } else {
            final Map<String, String> propColumnNameMap = getPropColumnNameMap();

            for (int i = 0, len = columnNames.length; i < len; i++) {
                if (i > 0) {
                    sb.append(_COMMA_SPACE);
                }

                sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));
            }
        }

        return this;
    }

    /**
     *
     * @param columnName
     * @param direction
     * @return
     */
    public SQLBuilder orderBy(final String columnName, final SortDirection direction) {
        orderBy(columnName);

        sb.append(WD._SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder orderBy(final Collection<String> columnNames) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();
        int i = 0;
        for (String columnName : columnNames) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(formalizeColumnName(propColumnNameMap, columnName));
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @param direction
     * @return
     */
    public SQLBuilder orderBy(final Collection<String> columnNames, final SortDirection direction) {
        orderBy(columnNames);

        sb.append(WD._SPACE);
        sb.append(direction.toString());

        return this;
    }

    /**
     *
     * @param orders
     * @return
     */
    public SQLBuilder orderBy(final Map<String, SortDirection> orders) {
        sb.append(_SPACE_ORDER_BY_SPACE);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();
        int i = 0;

        for (Map.Entry<String, SortDirection> entry : orders.entrySet()) {
            if (i++ > 0) {
                sb.append(_COMMA_SPACE);
            }

            sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

            sb.append(WD._SPACE);
            sb.append(entry.getValue().toString());
        }

        return this;
    }

    /**
     *
     * @param count
     * @return
     */
    public SQLBuilder limit(final int count) {
        sb.append(_SPACE_LIMIT_SPACE);

        sb.append(count);

        return this;
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public SQLBuilder limit(final int offset, final int count) {
        sb.append(_SPACE_LIMIT_SPACE);

        sb.append(offset);

        sb.append(_COMMA_SPACE);

        sb.append(count);

        return this;
    }

    /**
     *
     * @param offset
     * @return
     */
    public SQLBuilder offset(final int offset) {
        sb.append(_SPACE_OFFSET_SPACE);

        sb.append(offset);

        return this;
    }

    /**
     * Limit by row num.
     *
     * @param count
     * @return
     */
    public SQLBuilder limitByRowNum(final int count) {
        sb.append(" ROWNUM ");

        sb.append(count);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder union(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            parameters.addAll(sqlBuilder.parameters());
        }

        return union(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder union(final String query) {
        return union(N.asArray(query));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder union(final String... columnNames) {
        op = OperationType.QUERY;

        this.columnNames = columnNames;
        this.columnNameList = null;
        this.columnAliases = null;

        sb.append(_SPACE_UNION_SPACE);

        // it's sub query
        if (isSubQuery(columnNames)) {
            sb.append(columnNames[0]);

            this.columnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder union(final Collection<String> columnNames) {
        op = OperationType.QUERY;

        this.columnNames = null;
        this.columnNameList = columnNames;
        this.columnAliases = null;

        sb.append(_SPACE_UNION_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder unionAll(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            parameters.addAll(sqlBuilder.parameters());
        }

        return unionAll(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder unionAll(final String query) {
        return unionAll(N.asArray(query));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder unionAll(final String... columnNames) {
        op = OperationType.QUERY;

        this.columnNames = columnNames;
        this.columnNameList = null;
        this.columnAliases = null;

        sb.append(_SPACE_UNION_ALL_SPACE);

        // it's sub query
        if (isSubQuery(columnNames)) {
            sb.append(columnNames[0]);

            this.columnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder unionAll(final Collection<String> columnNames) {
        op = OperationType.QUERY;

        this.columnNames = null;
        this.columnNameList = columnNames;
        this.columnAliases = null;

        sb.append(_SPACE_UNION_ALL_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder intersect(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            parameters.addAll(sqlBuilder.parameters());
        }

        return intersect(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder intersect(final String query) {
        return intersect(N.asArray(query));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder intersect(final String... columnNames) {
        op = OperationType.QUERY;

        this.columnNames = columnNames;
        this.columnNameList = null;
        this.columnAliases = null;

        sb.append(_SPACE_INTERSECT_SPACE);

        // it's sub query
        if (isSubQuery(columnNames)) {
            sb.append(columnNames[0]);

            this.columnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder intersect(final Collection<String> columnNames) {
        op = OperationType.QUERY;

        this.columnNames = null;
        this.columnNameList = columnNames;
        this.columnAliases = null;

        sb.append(_SPACE_INTERSECT_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder except(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            parameters.addAll(sqlBuilder.parameters());
        }

        return except(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder except(final String query) {
        return except(N.asArray(query));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder except(final String... columnNames) {
        op = OperationType.QUERY;

        this.columnNames = columnNames;
        this.columnNameList = null;
        this.columnAliases = null;

        sb.append(_SPACE_EXCEPT_SPACE);

        // it's sub query
        if (isSubQuery(columnNames)) {
            sb.append(columnNames[0]);

            this.columnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder except(final Collection<String> columnNames) {
        op = OperationType.QUERY;

        this.columnNames = null;
        this.columnNameList = columnNames;
        this.columnAliases = null;

        sb.append(_SPACE_EXCEPT_SPACE);

        return this;
    }

    /**
     *
     * @param sqlBuilder
     * @return
     */
    public SQLBuilder minus(final SQLBuilder sqlBuilder) {
        final String sql = sqlBuilder.sql();

        if (N.notNullOrEmpty(sqlBuilder.parameters())) {
            parameters.addAll(sqlBuilder.parameters());
        }

        return minus(sql);
    }

    /**
     *
     * @param query
     * @return
     */
    public SQLBuilder minus(final String query) {
        return minus(N.asArray(query));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder minus(final String... columnNames) {
        op = OperationType.QUERY;

        this.columnNames = columnNames;
        this.columnNameList = null;
        this.columnAliases = null;

        sb.append(_SPACE_EXCEPT2_SPACE);

        // it's sub query
        if (isSubQuery(columnNames)) {
            sb.append(columnNames[0]);

            this.columnNames = null;
        } else {
            // build in from method.
        }

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder minus(final Collection<String> columnNames) {
        op = OperationType.QUERY;

        this.columnNames = null;
        this.columnNameList = columnNames;
        this.columnAliases = null;

        sb.append(_SPACE_EXCEPT2_SPACE);

        return this;
    }

    /**
     *
     * @return
     */
    public SQLBuilder forUpdate() {
        sb.append(_SPACE_FOR_UPDATE);

        return this;
    }

    /**
     *
     * @param expr
     * @return
     */
    public SQLBuilder set(final String expr) {
        return set(N.asArray(expr));
    }

    /**
     *
     * @param columnNames
     * @return
     */
    @SafeVarargs
    public final SQLBuilder set(final String... columnNames) {
        init(false);

        if (columnNames.length == 1 && columnNames[0].contains(WD.EQUAL)) {
            appendStringExpr(columnNames[0]);
        } else {
            final Map<String, String> propColumnNameMap = getPropColumnNameMap();

            switch (sqlPolicy) {
                case SQL:
                case PARAMETERIZED_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));

                        sb.append(_SPACE_EQUAL_SPACE);

                        sb.append(WD._QUESTION_MARK);
                    }

                    break;
                }

                case NAMED_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));

                        sb.append(_SPACE_EQUAL_SPACE);

                        sb.append(":");
                        sb.append(columnNames[i]);
                    }

                    break;
                }

                case IBATIS_SQL: {
                    for (int i = 0, len = columnNames.length; i < len; i++) {
                        if (i > 0) {
                            sb.append(_COMMA_SPACE);
                        }

                        sb.append(formalizeColumnName(propColumnNameMap, columnNames[i]));

                        sb.append(_SPACE_EQUAL_SPACE);

                        sb.append("#{");
                        sb.append(columnNames[i]);
                        sb.append('}');
                    }

                    break;
                }

                default:
                    throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
            }
        }

        columnNameList = null;

        return this;
    }

    /**
     *
     * @param columnNames
     * @return
     */
    public SQLBuilder set(final Collection<String> columnNames) {
        init(false);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();

        switch (sqlPolicy) {
            case SQL:
            case PARAMETERIZED_SQL: {
                int i = 0;
                for (String columnName : columnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, columnName));

                    sb.append(_SPACE_EQUAL_SPACE);

                    sb.append(WD._QUESTION_MARK);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (String columnName : columnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, columnName));

                    sb.append(_SPACE_EQUAL_SPACE);

                    sb.append(":");
                    sb.append(columnName);
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (String columnName : columnNames) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, columnName));

                    sb.append(_SPACE_EQUAL_SPACE);

                    sb.append("#{");
                    sb.append(columnName);
                    sb.append('}');
                }

                break;
            }

            default:
                throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
        }

        columnNameList = null;

        return this;
    }

    /**
     *
     * @param props
     * @return
     */
    public SQLBuilder set(final Map<String, Object> props) {
        init(false);

        final Map<String, String> propColumnNameMap = getPropColumnNameMap();

        switch (sqlPolicy) {
            case SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForSQL(entry.getValue());
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForRawSQL(entry.getValue());
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                for (Map.Entry<String, Object> entry : props.entrySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    sb.append(formalizeColumnName(propColumnNameMap, entry.getKey()));

                    sb.append(_SPACE_EQUAL_SPACE);

                    setParameterForIbatisNamedSQL(entry.getKey(), entry.getValue());
                }

                break;
            }

            default:
                throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
        }

        columnNameList = null;

        return this;
    }

    /**
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     *
     * @param entity
     * @return
     */
    public SQLBuilder set(final Object entity) {
        return set(entity, null);
    }

    /**
     * Only the dirty properties will be set into the result SQL if the specified entity is a dirty marker entity.
     *
     * @param entity
     * @param excludedPropNames
     * @return
     */
    public SQLBuilder set(final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            return set(N.asArray((String) entity));
        } else if (entity instanceof Map) {
            if (N.isNullOrEmpty(excludedPropNames)) {
                return set((Map<String, Object>) entity);
            } else {
                final Map<String, Object> props = new LinkedHashMap<>((Map<String, Object>) entity);
                Maps.removeKeys(props, excludedPropNames);
                return set(props);
            }
        } else {
            final Class<?> entityClass = entity.getClass();
            this.entityClass = entityClass;
            final Collection<String> propNames = getUpdatePropNamesByClass(entityClass, excludedPropNames);
            final Set<String> dirtyPropNames = DirtyMarkerUtil.isDirtyMarker(entityClass) ? DirtyMarkerUtil.dirtyPropNames((DirtyMarker) entity) : null;
            final Map<String, Object> props = N.newHashMap(N.initHashCapacity(N.isNullOrEmpty(dirtyPropNames) ? propNames.size() : dirtyPropNames.size()));

            for (String propName : propNames) {
                if (dirtyPropNames == null || dirtyPropNames.contains(propName)) {
                    props.put(propName, ClassUtil.getPropValue(entity, propName));
                }
            }

            return set(props);
        }
    }

    /**
     *
     * @param entityClass
     * @return
     */
    public SQLBuilder set(Class<?> entityClass) {
        this.entityClass = entityClass;

        return set(entityClass, null);
    }

    /**
     *
     * @param entityClass
     * @param excludedPropNames
     * @return
     */
    public SQLBuilder set(Class<?> entityClass, final Set<String> excludedPropNames) {
        this.entityClass = entityClass;

        return set(getUpdatePropNamesByClass(entityClass, excludedPropNames));
    }

    /**
     * This SQLBuilder will be closed after <code>sql()</code> is called.
     *
     * @return
     */
    public String sql() {
        if (sb == null) {
            throw new AbacusException("This SQLBuilder has been closed after sql() was called previously");
        }

        init(true);

        String sql = null;

        try {
            sql = sb.toString();
        } finally {
            Objectory.recycle(sb);
            sb = null;

            activeStringBuilderCounter.decrementAndGet();
        }

        if (logger.isDebugEnabled()) {
            logger.debug(sql);
        }

        return sql;
    }

    /**
     *
     * @return
     */
    public List<Object> parameters() {
        return parameters;
    }

    /**
     *  This SQLBuilder will be closed after <code>pair()</code> is called.
     *
     * @return
     */
    public SP pair() {
        return new SP(sql(), parameters);
    }

    /**
     *
     * @param <T>
     * @param <EX>
     * @param func
     * @return
     * @throws EX the ex
     */
    public <T, EX extends Exception> T apply(final Try.Function<? super SP, T, EX> func) throws EX {
        return func.apply(this.pair());
    }

    /**
     *
     * @param <EX>
     * @param consumer
     * @throws EX the ex
     */
    public <EX extends Exception> void accept(final Try.Consumer<? super SP, EX> consumer) throws EX {
        consumer.accept(this.pair());
    }

    /**
     *
     * @param setForUpdate
     */
    void init(boolean setForUpdate) {
        if (sb.length() > 0) {
            return;
        }

        if (op == OperationType.UPDATE) {
            sb.append(_UPDATE);

            sb.append(WD._SPACE);
            sb.append(tableName);

            sb.append(_SPACE_SET_SPACE);

            if (setForUpdate && N.notNullOrEmpty(columnNameList)) {
                set(columnNameList);
            }
        } else if (op == OperationType.DELETE) {
            final String newTableName = tableName;

            char[] deleteFromTableChars = tableDeleteFrom.get(newTableName);

            if (deleteFromTableChars == null) {
                deleteFromTableChars = (WD.DELETE + WD.SPACE + WD.FROM + WD.SPACE + newTableName).toCharArray();
                tableDeleteFrom.put(newTableName, deleteFromTableChars);
            }

            sb.append(deleteFromTableChars);
        }
    }

    /**
     * Sets the parameter for SQL.
     *
     * @param propValue the new parameter for SQL
     */
    private void setParameterForSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append(WD._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append(Expression.formalize(propValue));
        }
    }

    /**
     * Sets the parameter for raw SQL.
     *
     * @param propValue the new parameter for raw SQL
     */
    private void setParameterForRawSQL(final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append(WD._QUESTION_MARK);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append(WD._QUESTION_MARK);

            parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for ibatis named SQL.
     *
     * @param propName
     * @param propValue
     */
    private void setParameterForIbatisNamedSQL(final String propName, final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append("#{");
            sb.append(propName);
            sb.append('}');
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append("#{");
            sb.append(propName);
            sb.append('}');

            parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter for named SQL.
     *
     * @param propName
     * @param propValue
     */
    private void setParameterForNamedSQL(final String propName, final Object propValue) {
        if (CF.QME.equals(propValue)) {
            sb.append(":");
            sb.append(propName);
        } else if (propValue instanceof Condition) {
            appendCondition((Condition) propValue);
        } else {
            sb.append(":");
            sb.append(propName);

            parameters.add(propValue);
        }
    }

    /**
     * Sets the parameter.
     *
     * @param propName
     * @param propValue
     */
    private void setParameter(final String propName, final Object propValue) {
        switch (sqlPolicy) {
            case SQL: {
                setParameterForSQL(propValue);

                break;
            }

            case PARAMETERIZED_SQL: {
                setParameterForRawSQL(propValue);

                break;
            }

            case NAMED_SQL: {
                setParameterForNamedSQL(propName, propValue);

                break;
            }

            case IBATIS_SQL: {
                setParameterForIbatisNamedSQL(propName, propValue);

                break;
            }

            default:
                throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
        }
    }

    /**
     * Append insert props.
     *
     * @param props
     */
    private void appendInsertProps(final Map<String, Object> props) {
        switch (sqlPolicy) {
            case SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForSQL(propValue);
                }

                break;
            }

            case PARAMETERIZED_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForRawSQL(propValue);
                }

                break;
            }

            case NAMED_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForNamedSQL(propName, propValue);
                }

                break;
            }

            case IBATIS_SQL: {
                int i = 0;
                Object propValue = null;
                for (String propName : props.keySet()) {
                    if (i++ > 0) {
                        sb.append(_COMMA_SPACE);
                    }

                    propValue = props.get(propName);

                    setParameterForIbatisNamedSQL(propName, propValue);
                }

                break;
            }

            default:
                throw new AbacusException("Not supported SQL policy: " + sqlPolicy);
        }
    }

    /**
     *
     * @param cond
     */
    private void appendCondition(final Condition cond) {
        if (cond instanceof Binary) {
            final Binary binary = (Binary) cond;
            final String propName = binary.getPropName();

            sb.append(formalizeColumnName(propName));

            sb.append(WD._SPACE);
            sb.append(binary.getOperator().toString());
            sb.append(WD._SPACE);

            Object propValue = binary.getPropValue();
            setParameter(propName, propValue);
        } else if (cond instanceof Between) {
            final Between bt = (Between) cond;
            final String propName = bt.getPropName();

            sb.append(formalizeColumnName(propName));

            sb.append(WD._SPACE);
            sb.append(bt.getOperator().toString());
            sb.append(WD._SPACE);

            Object minValue = bt.getMinValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("min" + StringUtil.capitalize(propName), minValue);
            } else {
                setParameter(propName, minValue);
            }

            sb.append(WD._SPACE);
            sb.append(WD.AND);
            sb.append(WD._SPACE);

            Object maxValue = bt.getMaxValue();
            if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                setParameter("max" + StringUtil.capitalize(propName), maxValue);
            } else {
                setParameter(propName, maxValue);
            }
        } else if (cond instanceof In) {
            final In in = (In) cond;
            final String propName = in.getPropName();
            final List<Object> parameters = in.getParameters();

            sb.append(formalizeColumnName(propName));

            sb.append(WD._SPACE);
            sb.append(in.getOperator().toString());
            sb.append(WD.SPACE_PARENTHESES_L);

            for (int i = 0, len = parameters.size(); i < len; i++) {
                if (i > 0) {
                    sb.append(WD.COMMA_SPACE);
                }

                if (sqlPolicy == SQLPolicy.NAMED_SQL || sqlPolicy == SQLPolicy.IBATIS_SQL) {
                    setParameter(propName + (i + 1), parameters.get(i));
                } else {
                    setParameter(propName, parameters.get(i));
                }
            }

            sb.append(WD._PARENTHESES_R);
        } else if (cond instanceof Cell) {
            final Cell cell = (Cell) cond;

            sb.append(WD._SPACE);
            sb.append(cell.getOperator().toString());
            sb.append(WD._SPACE);

            sb.append(_PARENTHESES_L);
            appendCondition(cell.getCondition());
            sb.append(_PARENTHESES_R);
        } else if (cond instanceof Junction) {
            final Junction junction = (Junction) cond;
            final List<Condition> conditionList = junction.getConditions();

            if (N.isNullOrEmpty(conditionList)) {
                throw new IllegalArgumentException("The junction condition(" + junction.getOperator().toString() + ") doesn't include any element.");
            }

            if (conditionList.size() == 1) {
                appendCondition(conditionList.get(0));
            } else {
                // TODO ((id = :id) AND (gui = :gui)) is not support in Cassandra.
                // only (id = :id) AND (gui = :gui) works.
                // sb.append(_PARENTHESES_L);

                for (int i = 0, size = conditionList.size(); i < size; i++) {
                    if (i > 0) {
                        sb.append(_SPACE);
                        sb.append(junction.getOperator().toString());
                        sb.append(_SPACE);
                    }

                    sb.append(_PARENTHESES_L);

                    appendCondition(conditionList.get(i));

                    sb.append(_PARENTHESES_R);
                }

                // sb.append(_PARENTHESES_R);
            }
        } else if (cond instanceof SubQuery) {
            final SubQuery subQuery = (SubQuery) cond;

            if (N.notNullOrEmpty(subQuery.getSql())) {
                sb.append(subQuery.getSql());
            } else {
                if (this instanceof SCSB) {
                    sb.append(SCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof PSC) {
                    sb.append(PSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof MSC) {
                    sb.append(MSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof NSC) {
                    sb.append(NSC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof ACSB) {
                    sb.append(ACSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof PAC) {
                    sb.append(PAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof MAC) {
                    sb.append(MAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof NAC) {
                    sb.append(NAC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof LCSB) {
                    sb.append(LCSB.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof PLC) {
                    sb.append(PLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof MLC) {
                    sb.append(MLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else if (this instanceof NLC) {
                    sb.append(NLC.select(subQuery.getSelectPropNames()).from(subQuery.getEntityName()).where(subQuery.getCondition()).sql());
                } else {
                    throw new AbacusException("Unsupproted subQuery condition: " + cond);
                }
            }
        } else if (cond instanceof Expression) {
            sb.append(cond.toString());
        } else {
            throw new IllegalArgumentException("Unsupported condtion: " + cond.toString());
        }
    }

    /**
     * Formalize column name.
     *
     * @param propName
     * @return
     */
    private String formalizeColumnName(final String propName) {
        return formalizeColumnName(getPropColumnNameMap(), propName);
    }

    /**
     * Formalize column name.
     *
     * @param propColumnNameMap
     * @param propName
     * @return
     */
    private String formalizeColumnName(final Map<String, String> propColumnNameMap, final String propName) {
        String columnName = propColumnNameMap == null ? null : propColumnNameMap.get(propName);

        if (columnName != null) {
            return columnName;
        }

        switch (namingPolicy) {
            case LOWER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toLowerCaseWithUnderscore(propName);

            case UPPER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toUpperCaseWithUnderscore(propName);

            case LOWER_CAMEL_CASE:
                return ClassUtil.formalizePropName(propName);

            default:
                return propName;
        }
    }

    /**
     * Gets the prop column name map.
     *
     * @return
     */
    private Map<String, String> getPropColumnNameMap() {
        if (entityClass == null || Map.class.isAssignableFrom(entityClass)) {
            return N.emptyMap();
        }

        final Map<String, String> result = entityTablePropColumnNameMap.get(entityClass);

        if (result == null) {
            registerEntityPropColumnNameMap(entityClass);
        }

        return entityTablePropColumnNameMap.get(entityClass);
    }

    /**
     * Checks if is sub query.
     *
     * @param columnNames
     * @return true, if is sub query
     */
    private boolean isSubQuery(final String... columnNames) {
        if (columnNames.length == 1) {
            int index = SQLParser.indexWord(columnNames[0], WD.SELECT, 0, false);

            if (index >= 0) {
                index = SQLParser.indexWord(columnNames[0], WD.FROM, index, false);

                return index >= 1;
            }
        }

        return false;
    }

    //    @Override
    //    public int hashCode() {
    //        return sb.hashCode();
    //    }
    //
    //    @Override
    //    public boolean equals(Object obj) {
    //        if (obj == this) {
    //            return true;
    //        }
    //
    //        if (obj instanceof SQLBuilder) {
    //            final SQLBuilder other = (SQLBuilder) obj;
    //
    //            return N.equals(this.sb, other.sb) && N.equals(this.parameters, other.parameters);
    //        }
    //
    //        return false;
    //    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return sql();
    }

    /**
     * Parses the insert entity.
     *
     * @param instance
     * @param entity
     * @param excludedPropNames
     */
    private static void parseInsertEntity(final SQLBuilder instance, final Object entity, final Set<String> excludedPropNames) {
        if (entity instanceof String) {
            instance.columnNames = N.asArray((String) entity);
        } else if (entity instanceof Map) {
            if (N.isNullOrEmpty(excludedPropNames)) {
                instance.props = (Map<String, Object>) entity;
            } else {
                instance.props = new LinkedHashMap<>((Map<String, Object>) entity);
                Maps.removeKeys(instance.props, excludedPropNames);
            }
        } else {
            final Collection<String> propNames = getInsertPropNamesByClass(entity, excludedPropNames);
            final Map<String, Object> map = N.newHashMap(N.initHashCapacity(propNames.size()));

            for (String propName : propNames) {
                map.put(propName, ClassUtil.getPropValue(entity, propName));
            }

            instance.props = map;
        }
    }

    /**
     * To insert props list.
     *
     * @param propsList
     * @return
     */
    private static Collection<Map<String, Object>> toInsertPropsList(final Collection<?> propsList) {
        final Optional<?> first = N.firstNonNull(propsList);

        if (first.isPresent() && first.get() instanceof Map) {
            return (List<Map<String, Object>>) propsList;
        } else {
            final Class<?> entityClass = first.get().getClass();
            final Collection<String> propNames = getInsertPropNamesByClass(entityClass, null);
            final List<Map<String, Object>> newPropsList = new ArrayList<>(propsList.size());

            for (Object entity : propsList) {
                final Map<String, Object> props = N.newHashMap(N.initHashCapacity(propNames.size()));

                for (String propName : propNames) {
                    props.put(propName, ClassUtil.getPropValue(entity, propName));
                }

                newPropsList.add(props);
            }

            return newPropsList;
        }
    }

    /**
     * The Enum SQLPolicy.
     */
    enum SQLPolicy {

        /** The sql. */
        SQL,
        /** The parameterized sql. */
        PARAMETERIZED_SQL,
        /** The named sql. */
        NAMED_SQL,
        /** The ibatis sql. */
        IBATIS_SQL;
    }

    /**
     * Un-parameterized SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * SCSB.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql();
     * // Output: SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PSC or NSC} is preferred.
     */
    @Deprecated
    public static class SCSB extends SQLBuilder {

        /**
         * Instantiates a new scsb.
         */
        SCSB() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static SCSB createInstance() {
            return new SCSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Un-parameterized SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(ACSB.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // Output: SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PAC or NAC} is preferred.
     */
    @Deprecated
    public static class ACSB extends SQLBuilder {

        /**
         * Instantiates a new acsb.
         */
        ACSB() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static ACSB createInstance() {
            return new ACSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Un-parameterized SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(LCSB.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = 1
     * </code>
     * </pre>
     *
     * @deprecated {@code PLC or NLC} is preferred.
     */
    @Deprecated
    public static class LCSB extends SQLBuilder {

        /**
         * Instantiates a new lcsb.
         */
        LCSB() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static LCSB createInstance() {
            return new LCSB();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }
    }

    /**
     * Parameterized('?') SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PSC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = ?
     * </code>
     * </pre>
     */
    public static class PSC extends SQLBuilder {

        /**
         * Instantiates a new psc.
         */
        PSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static PSC createInstance() {
            return new PSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Parameterized('?') SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PAC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = ?
     * </code>
     * </pre>
     */
    public static class PAC extends SQLBuilder {

        /**
         * Instantiates a new pac.
         */
        PAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static PAC createInstance() {
            return new PAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Parameterized('?') SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(PLC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = ?
     * </code>
     * </pre>
     */
    public static class PLC extends SQLBuilder {

        /**
         * Instantiates a new plc.
         */
        PLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.PARAMETERIZED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static PLC createInstance() {
            return new PLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }
    }

    /**
     * Named SQL builder with snake case (lower case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NSC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = :id
     * </code>
     * </pre>
     */
    public static class NSC extends SQLBuilder {

        /**
         * Instantiates a new nsc.
         */
        NSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static NSC createInstance() {
            return new NSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Named SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NAC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = :id
     * </code>
     * </pre>
     */
    public static class NAC extends SQLBuilder {

        /**
         * Instantiates a new nac.
         */
        NAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static NAC createInstance() {
            return new NAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * Named SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(NLC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = :id
     * </code>
     * </pre>
     */
    public static class NLC extends SQLBuilder {

        /**
         * Instantiates a new nlc.
         */
        NLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.NAMED_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static NLC createInstance() {
            return new NLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(MLC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT first_name AS "firstName", last_name AS "lastName" FROM account WHERE id = #{id}
     * </code>
     * </pre>
     */
    @Deprecated
    public static class MSC extends SQLBuilder {

        /**
         * Instantiates a new msc.
         */
        MSC() {
            super(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static MSC createInstance() {
            return new MSC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with all capitals case (upper case with underscore) field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(MAC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT FIRST_NAME AS "firstName", LAST_NAME AS "lastName" FROM ACCOUNT WHERE ID = #{id}
     * </code>
     * </pre>
     */
    @Deprecated
    public static class MAC extends SQLBuilder {

        /**
         * Instantiates a new mac.
         */
        MAC() {
            super(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static MAC createInstance() {
            return new MAC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);

            return instance;
        }
    }

    /**
     * MyBatis-style SQL builder with lower camel case field/column naming strategy.
     *
     * For example:
     * <pre>
     * <code>
     * N.println(MLC.select("firstName", "lastName").from("account").where(L.eq("id", 1)).sql());
     * // SELECT firstName, lastName FROM account WHERE id = #{id}
     * </code>
     * </pre>
     */
    @Deprecated
    public static class MLC extends SQLBuilder {

        /**
         * Instantiates a new mlc.
         */
        MLC() {
            super(NamingPolicy.LOWER_CAMEL_CASE, SQLPolicy.IBATIS_SQL);
        }

        /**
         * Creates the instance.
         *
         * @return
         */
        static MLC createInstance() {
            return new MLC();
        }

        /**
         *
         * @param expr
         * @return
         */
        public static SQLBuilder insert(final String expr) {
            return insert(N.asArray(expr));
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder insert(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder insert(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param props
         * @return
         */
        public static SQLBuilder insert(final Map<String, Object> props) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.props = props;

            return instance;
        }

        /**
         *
         * @param entity
         * @return
         */
        public static SQLBuilder insert(final Object entity) {
            return insert(entity, null);
        }

        /**
         *
         * @param entity
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Object entity, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entity.getClass();

            parseInsertEntity(instance, entity, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass) {
            return insert(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insert(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            instance.entityClass = entityClass;
            instance.columnNameList = getInsertPropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass) {
            return insertInto(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder insertInto(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return insert(entityClass, excludedPropNames).into(entityClass);
        }

        /**
         * Generate the MySQL style batch insert sql.
         *
         * @param propsList list of entity or properties map.
         * @return
         */
        @Beta
        public static SQLBuilder batchInsert(final Collection<?> propsList) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.ADD;
            final Optional<?> first = N.firstNonNull(propsList);

            if (N.firstNonNull(propsList).isPresent()) {
                instance.entityClass = first.getClass().getClass();
            }

            instance.propsList = toInsertPropsList(propsList);

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        @SafeVarargs
        public static SQLBuilder select(final String... columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final String[] columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNames = columnNames;

            return instance;
        }

        /**
         *
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnNames
         * @return
         */
        public static SQLBuilder select(final String expr, final Collection<String> columnNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnNameList = columnNames;

            return instance;
        }

        /**
         *
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param expr <code>ALL | DISTINCT | DISTINCTROW...</code>
         * @param columnAliases
         * @return
         */
        public static SQLBuilder select(final String expr, final Map<String, String> columnAliases) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.predicates = expr;
            instance.columnAliases = columnAliases;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass) {
            return select(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return select(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return select(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder select(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.QUERY;
            instance.entityClass = entityClass;
            instance.columnNameList = getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass) {
            return selectFrom(entityClass, false);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties) {
            return selectFrom(entityClass, includeSubEntityProperties, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final Set<String> excludedPropNames) {
            return selectFrom(entityClass, false, excludedPropNames);
        }

        /**
         *
         * @param entityClass
         * @param includeSubEntityProperties
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder selectFrom(final Class<?> entityClass, final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
            if (includeSubEntityProperties) {
                final List<String> selectTableNames = getSelectTableNames(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

                if (N.isNullOrEmpty(selectTableNames) || selectTableNames.size() == 1) {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
                } else {
                    return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(selectTableNames);
                }
            } else {
                return select(getSelectPropNamesByClass(entityClass, includeSubEntityProperties, excludedPropNames)).from(entityClass);
            }
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder update(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass) {
            return update(entityClass, null);
        }

        /**
         *
         * @param entityClass
         * @param excludedPropNames
         * @return
         */
        public static SQLBuilder update(final Class<?> entityClass, final Set<String> excludedPropNames) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.UPDATE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);
            instance.columnNameList = getUpdatePropNamesByClass(entityClass, excludedPropNames);

            return instance;
        }

        /**
         *
         * @param tableName
         * @return
         */
        public static SQLBuilder deleteFrom(final String tableName) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.tableName = tableName;

            return instance;
        }

        /**
         *
         * @param entityClass
         * @return
         */
        public static SQLBuilder deleteFrom(final Class<?> entityClass) {
            final SQLBuilder instance = createInstance();

            instance.op = OperationType.DELETE;
            instance.entityClass = entityClass;
            instance.tableName = getTableName(entityClass, NamingPolicy.LOWER_CAMEL_CASE);

            return instance;
        }
    }

    /**
     * The Class SP.
     */
    public static final class SP {

        /** The sql. */
        public final String sql;

        /** The parameters. */
        public final List<Object> parameters;

        /**
         * Instantiates a new sp.
         *
         * @param sql
         * @param parameters
         */
        SP(final String sql, final List<Object> parameters) {
            this.sql = sql;
            this.parameters = ImmutableList.of(parameters);
        }

        /**
         *
         * @return
         */
        public Pair<String, List<Object>> __() {
            return Pair.of(sql, parameters);
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return N.hashCode(sql) * 31 + N.hashCode(parameters);
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof SP) {
                SP other = (SP) obj;

                return N.equals(other.sql, sql) && N.equals(other.parameters, parameters);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{sql=" + sql + ", parameters=" + N.toString(parameters) + "}";
        }
    }
}
