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
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

public final class TestUtil {

    private TestUtil() {
        // singleton
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param bean a bean object with getter/setter method
     */
    public static void fill(final Object bean) {
        final Class<?> beanClass = bean.getClass();

        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass); //NOSONAR

        fill(bean, ClassUtil.getPropNameList(beanClass));
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param bean a bean object with getter/setter method
     * @param propNamesToFill
     */
    public static void fill(final Object bean, final Collection<String> propNamesToFill) {
        fill(ParserUtil.getBeanInfo(bean.getClass()), bean, propNamesToFill);
    }

    private static void fill(final BeanInfo beanInfo, final Object bean, final Collection<String> propNamesToFill) {
        PropInfo propInfo = null;
        Type<Object> type = null;
        Class<?> parameterClass = null;
        Object propValue = null;

        for (String propName : propNamesToFill) {
            propInfo = beanInfo.getPropInfo(propName);
            parameterClass = propInfo.clazz;
            type = propInfo.jsonXmlType;

            if (String.class.equals(parameterClass)) {
                propValue = Strings.uuid().substring(0, 16);
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
                propValue = Integer.valueOf(N.RAND.nextInt()).byteValue(); //NOSONAR
            } else if (short.class.equals(parameterClass) || Short.class.equals(parameterClass)) {
                propValue = Integer.valueOf(N.RAND.nextInt()).shortValue(); //NOSONAR
            } else if (Number.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(N.RAND.nextInt()));
            } else if (java.util.Date.class.isAssignableFrom(parameterClass) || Calendar.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(System.currentTimeMillis()));
            } else if (ClassUtil.isBeanClass(parameterClass)) {
                propValue = fill(parameterClass);
            } else {
                propValue = type.defaultValue();
            }

            propInfo.setPropValue(bean, propValue);
        }
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @return
     */
    public static <T> T fill(final Class<? extends T> beanClass) {
        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass);

        return fill(beanClass, ClassUtil.getPropNameList(beanClass));
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @param count
     * @return
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final int count) {
        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass);

        return fill(beanClass, ClassUtil.getPropNameList(beanClass), count);
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill
     * @return
     */
    public static <T> T fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill) {
        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        final Object result = beanInfo.createBeanResult();

        fill(beanInfo, result, propNamesToFill);

        return (T) beanInfo.finishBeanResult(result);
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill
     * @param count
     * @return
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill, final int count) {
        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass);
        N.checkArgNotNegative(count, "count");

        final List<T> resultList = new ArrayList<>(count);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        Object result = null;

        for (int i = 0; i < count; i++) {
            result = beanInfo.createBeanResult();

            fill(beanInfo, result, propNamesToFill);

            resultList.add((T) beanInfo.finishBeanResult(result));
        }

        return resultList;
    }
}
