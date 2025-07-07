/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Utility class for generating test data by filling bean objects with random values.
 * 
 * <p>This class provides convenient methods to populate JavaBean objects with random test data,
 * which is particularly useful for unit testing, prototyping, and generating sample data.
 * It supports primitive types, wrapper types, strings, dates, and nested bean objects.</p>
 * 
 * <p><b>Usage examples:</b></p>
 * <pre>{@code
 * // Fill an existing bean instance
 * User user = new User();
 * TestUtil.fill(user);
 * 
 * // Create and fill a new bean instance
 * User filledUser = TestUtil.fill(User.class);
 * 
 * // Create multiple filled instances
 * List<User> users = TestUtil.fill(User.class, 10);
 * 
 * // Fill only specific properties
 * TestUtil.fill(user, Arrays.asList("name", "email", "age"));
 * }</pre>
 * 
 * @author HaiYang Li
 */
public final class TestUtil {

    private TestUtil() {
        // singleton
    }

    /**
     * Fills all properties of the specified bean with random values.
     * 
     * <p>This method uses reflection to discover all properties of the bean and fills them
     * with appropriate random values based on their types. Nested bean properties are also
     * filled recursively.</p>
     * 
     * <p><b>Supported types:</b></p>
     * <ul>
     *   <li>Primitives and their wrappers (int, boolean, etc.)</li>
     *   <li>String (random UUID substring)</li>
     *   <li>Date and Calendar (current timestamp)</li>
     *   <li>Number subclasses</li>
     *   <li>Nested bean objects (filled recursively)</li>
     * </ul>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * Person person = new Person();
     * TestUtil.fill(person);
     * // person now has all properties filled with random values
     * System.out.println(person.getName()); // e.g., "a1b2c3d4-e5f6-78"
     * }</pre>
     *
     * @param bean a bean object with getter/setter methods
     * @throws IllegalArgumentException if bean is null or not a valid bean class
     */
    public static void fill(final Object bean) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);
        final Class<?> beanClass = checkBeanClass(bean.getClass());

        fill(bean, ClassUtil.getPropNameList(beanClass));
    }

    /**
     * Fills the specified properties of the bean with random values.
     * 
     * <p>Only the properties whose names are contained in the provided collection will be filled.
     * This is useful when you want to test specific scenarios with only certain fields populated.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * User user = new User();
     * TestUtil.fill(user, Arrays.asList("username", "email"));
     * // Only username and email are filled, other properties remain unchanged
     * }</pre>
     *
     * @param bean a bean object with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @throws IllegalArgumentException if bean is null or not a valid bean class
     */
    public static void fill(final Object bean, final Collection<String> propNamesToFill) {
        N.checkArgNotNull(bean, cs.bean);
        checkBeanClass(bean.getClass());

        fill(ParserUtil.getBeanInfo(bean.getClass()), bean, propNamesToFill);
    }

    private static void fill(final BeanInfo beanInfo, final Object bean, final Collection<String> propNamesToFill) {
        PropInfo propInfo = null;
        Type<Object> type = null;
        Class<?> parameterClass = null;
        Object propValue = null;

        for (final String propName : propNamesToFill) {
            propInfo = beanInfo.getPropInfo(propName);
            parameterClass = propInfo.clazz;
            type = propInfo.jsonXmlType;

            if (String.class.equals(parameterClass)) {
                propValue = Strings.uuid().substring(0, 16);
            } else if (boolean.class.equals(parameterClass) || Boolean.class.equals(parameterClass)) {
                propValue = N.RAND.nextInt() % 2 != 0;
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
     * Creates a new instance of the specified bean class and fills all its properties with random values.
     * 
     * <p>This is a convenience method that combines object creation and property filling in one step.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // Create a fully populated test object
     * Customer customer = TestUtil.fill(Customer.class);
     * // Use the customer object in tests
     * assertNotNull(customer.getName());
     * assertNotNull(customer.getAddress());
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @return a new instance with all properties filled with random values
     * @throws IllegalArgumentException if beanClass is null or not a valid bean class
     */
    public static <T> T fill(final Class<? extends T> beanClass) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        return fill(beanClass, ClassUtil.getPropNameList(beanClass));
    }

    /**
     * Creates multiple instances of the specified bean class, each filled with random values.
     * 
     * <p>This method is useful for generating test data sets or when you need multiple
     * test objects with varying random data.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // Generate 50 test users
     * List<User> testUsers = TestUtil.fill(User.class, 50);
     * // Use in batch operations or performance tests
     * userService.saveAll(testUsers);
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param count number of instances to create
     * @return a list containing the specified number of filled bean instances
     * @throws IllegalArgumentException if beanClass is null, not a valid bean class, or count is negative
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final int count) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        return fill(beanClass, ClassUtil.getPropNameList(beanClass), count);
    }

    /**
     * Creates a new instance of the specified bean class and fills only the specified properties.
     * 
     * <p>Properties not included in the collection will retain their default values.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // Create a user with only required fields filled
     * User user = TestUtil.fill(User.class, Arrays.asList("id", "username", "email"));
     * // Other fields like address, phone, etc. remain null/default
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @return a new instance with specified properties filled with random values
     * @throws IllegalArgumentException if beanClass is null or not a valid bean class
     */
    public static <T> T fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        final Object result = beanInfo.createBeanResult();

        fill(beanInfo, result, propNamesToFill);

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Creates multiple instances of the specified bean class with only the specified properties filled.
     * 
     * <p>Each instance will have the same set of properties filled but with different random values.</p>
     * 
     * <p><b>Usage example:</b></p>
     * <pre>{@code
     * // Create test data with only essential fields
     * List<Product> products = TestUtil.fill(
     *     Product.class, 
     *     Arrays.asList("id", "name", "price"), 
     *     100
     * );
     * // Each product has random id, name, and price, but other fields are default
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @param count number of instances to create
     * @return a list containing the specified number of partially filled bean instances
     * @throws IllegalArgumentException if beanClass is null, not a valid bean class, or count is negative
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill, final int count)
            throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);
        N.checkArgNotNegative(count, cs.count);

        final List<T> resultList = new ArrayList<>(count);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        Object result = null;

        for (int i = 0; i < count; i++) {
            result = beanInfo.createBeanResult();

            fill(beanInfo, result, propNamesToFill);

            resultList.add(beanInfo.finishBeanResult(result));
        }

        return resultList;
    }

    private static <T> Class<T> checkBeanClass(final Class<T> beanClass) {
        N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class with property getter/setter method", beanClass);

        return beanClass;
    }
}