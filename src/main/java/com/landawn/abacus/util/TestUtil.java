/*
 * Copyright (c) 2019, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

public final class TestUtil {

    private TestUtil() {
        // singleton
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param entity an entity object with getter/setter method
     */
    public static void fill(final Object entity) {
        final Class<?> entityClass = entity.getClass();

        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);

        fill(entity, ClassUtil.getPropNameList(entityClass));
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @return
     */
    public static <T> T fill(final Class<T> entityClass) {
        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);

        return fill(entityClass, ClassUtil.getPropNameList(entityClass));
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @param count
     * @return
     */
    public static <T> List<T> fill(final Class<T> entityClass, final int count) {
        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);

        return fill(entityClass, ClassUtil.getPropNameList(entityClass), count);
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param entity an entity object with getter/setter method
     * @param propNamesToFill
     */
    public static void fill(final Object entity, final Collection<String> propNamesToFill) {
        final Class<?> entityClass = entity.getClass();

        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entityClass);
        PropInfo propInfo = null;
        Type<Object> type = null;
        Class<?> parameterClass = null;
        Object propValue = null;

        for (String propName : propNamesToFill) {
            propInfo = entityInfo.getPropInfo(propName);
            parameterClass = propInfo.clazz;
            type = propInfo.jsonXmlType;

            if (String.class.equals(parameterClass)) {
                propValue = N.uuid().substring(0, 16);
            } else if (boolean.class.equals(parameterClass) || Boolean.class.equals(parameterClass)) {
                propValue = N.RAND.nextInt() % 2 == 0 ? false : true;
            } else if (char.class.equals(parameterClass) || Character.class.equals(parameterClass)) {
                propValue = (char) ('a' + N.RAND.nextInt() % 26);
            } else if (int.class.equals(parameterClass) || Integer.class.equals(parameterClass)) {
                propValue = N.RAND.nextInt();
            } else if (long.class.equals(parameterClass) || Long.class.equals(parameterClass)) {
                propValue = N.RAND.nextLong();
            } else if (float.class.equals(parameterClass) || Float.class.equals(parameterClass)) {
                propValue = N.RAND.nextFloat();
            } else if (double.class.equals(parameterClass) || Double.class.equals(parameterClass)) {
                propValue = N.RAND.nextDouble();
            } else if (byte.class.equals(parameterClass) || Byte.class.equals(parameterClass)) {
                propValue = Integer.valueOf(N.RAND.nextInt()).byteValue();
            } else if (short.class.equals(parameterClass) || Short.class.equals(parameterClass)) {
                propValue = Integer.valueOf(N.RAND.nextInt()).shortValue();
            } else if (Number.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(N.RAND.nextInt()));
            } else if (java.util.Date.class.isAssignableFrom(parameterClass) || Calendar.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(System.currentTimeMillis()));
            } else if (ClassUtil.isEntity(parameterClass)) {
                propValue = fill(parameterClass);
            } else {
                propValue = type.defaultValue();
            }

            propInfo.setPropValue(entity, propValue);
        }
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @param propNamesToFill
     * @return
     */
    public static <T> T fill(final Class<T> entityClass, final Collection<String> propNamesToFill) {
        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);

        final T entity = N.newInstance(entityClass);

        fill(entity, propNamesToFill);

        return entity;
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @param propNamesToFill
     * @param count
     * @return
     */
    public static <T> List<T> fill(final Class<T> entityClass, final Collection<String> propNamesToFill, final int count) {
        N.checkArgument(ClassUtil.isEntity(entityClass), "{} is not a valid entity class with property getter/setter method", entityClass);
        N.checkArgNotNegative(count, "count");

        final List<T> resultList = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final T entity = N.newInstance(entityClass);
            fill(entity, propNamesToFill);
            resultList.add(entity);
        }

        return resultList;
    }
}
