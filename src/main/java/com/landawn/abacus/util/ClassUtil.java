/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.Tuple.Tuple8;
import com.landawn.abacus.util.Tuple.Tuple9;
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
import com.landawn.abacus.util.stream.Stream;

@SuppressWarnings({ "java:S1942" })
public final class ClassUtil {

    /**
     * The Class ClassMask.
     */
    static final class ClassMask {//NOSONAR

        /** The Constant FIELD_MASK. */
        static final String FIELD_MASK = "FIELD_MASK";

        /**
         * Method mask.
         */
        static void methodMask() { //NOSONAR
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private static final String JAR_POSTFIX = ".jar";

    private static final String CLASS_POSTFIX = ".class";

    // ...
    private static final String PROP_NAME_SEPARATOR = ".";

    // ...
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    // ... it has to be big enough to make it's safety to add element to
    // ArrayBlockingQueue.
    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    // formalized property name list.
    private static final Map<String, Class<?>> BUILT_IN_TYPE = new ObjectPool<>(POOL_SIZE); // new LinkedHashMap<>();

    static {
        BUILT_IN_TYPE.put(boolean.class.getCanonicalName(), boolean.class);
        BUILT_IN_TYPE.put(char.class.getCanonicalName(), char.class);
        BUILT_IN_TYPE.put(byte.class.getCanonicalName(), byte.class);
        BUILT_IN_TYPE.put(short.class.getCanonicalName(), short.class);
        BUILT_IN_TYPE.put(int.class.getCanonicalName(), int.class);
        BUILT_IN_TYPE.put(long.class.getCanonicalName(), long.class);
        BUILT_IN_TYPE.put(float.class.getCanonicalName(), float.class);
        BUILT_IN_TYPE.put(double.class.getCanonicalName(), double.class);

        BUILT_IN_TYPE.put(Boolean.class.getCanonicalName(), Boolean.class);
        BUILT_IN_TYPE.put(Character.class.getCanonicalName(), Character.class);
        BUILT_IN_TYPE.put(Byte.class.getCanonicalName(), Byte.class);
        BUILT_IN_TYPE.put(Short.class.getCanonicalName(), Short.class);
        BUILT_IN_TYPE.put(Integer.class.getCanonicalName(), Integer.class);
        BUILT_IN_TYPE.put(Long.class.getCanonicalName(), Long.class);
        BUILT_IN_TYPE.put(Float.class.getCanonicalName(), Float.class);
        BUILT_IN_TYPE.put(Double.class.getCanonicalName(), Double.class);

        BUILT_IN_TYPE.put(String.class.getCanonicalName(), String.class);

        BUILT_IN_TYPE.put(Enum.class.getCanonicalName(), Enum.class);
        BUILT_IN_TYPE.put(Class.class.getCanonicalName(), Class.class);
        BUILT_IN_TYPE.put(Object.class.getCanonicalName(), Object.class);

        BUILT_IN_TYPE.put(BigInteger.class.getCanonicalName(), BigInteger.class);
        BUILT_IN_TYPE.put(BigDecimal.class.getCanonicalName(), BigDecimal.class);

        BUILT_IN_TYPE.put(java.util.Date.class.getCanonicalName(), java.util.Date.class);
        BUILT_IN_TYPE.put(Calendar.class.getCanonicalName(), Calendar.class);
        BUILT_IN_TYPE.put(GregorianCalendar.class.getCanonicalName(), GregorianCalendar.class);
        BUILT_IN_TYPE.put(XMLGregorianCalendar.class.getCanonicalName(), XMLGregorianCalendar.class);

        BUILT_IN_TYPE.put(Collection.class.getCanonicalName(), Collection.class);
        BUILT_IN_TYPE.put(List.class.getCanonicalName(), List.class);
        BUILT_IN_TYPE.put(ArrayList.class.getCanonicalName(), ArrayList.class);
        BUILT_IN_TYPE.put(LinkedList.class.getCanonicalName(), LinkedList.class);
        BUILT_IN_TYPE.put(Stack.class.getCanonicalName(), Stack.class);
        BUILT_IN_TYPE.put(Vector.class.getCanonicalName(), Vector.class);
        BUILT_IN_TYPE.put(Set.class.getCanonicalName(), Set.class);
        BUILT_IN_TYPE.put(HashSet.class.getCanonicalName(), HashSet.class);
        BUILT_IN_TYPE.put(LinkedHashSet.class.getCanonicalName(), LinkedHashSet.class);
        BUILT_IN_TYPE.put(SortedSet.class.getCanonicalName(), SortedSet.class);
        BUILT_IN_TYPE.put(NavigableSet.class.getCanonicalName(), NavigableSet.class);
        BUILT_IN_TYPE.put(TreeSet.class.getCanonicalName(), TreeSet.class);
        BUILT_IN_TYPE.put(Queue.class.getCanonicalName(), Queue.class);
        BUILT_IN_TYPE.put(Deque.class.getCanonicalName(), Deque.class);
        BUILT_IN_TYPE.put(BlockingDeque.class.getCanonicalName(), BlockingDeque.class);
        BUILT_IN_TYPE.put(ArrayDeque.class.getCanonicalName(), ArrayDeque.class);
        BUILT_IN_TYPE.put(ArrayBlockingQueue.class.getCanonicalName(), ArrayBlockingQueue.class);
        BUILT_IN_TYPE.put(LinkedBlockingQueue.class.getCanonicalName(), LinkedBlockingQueue.class);
        BUILT_IN_TYPE.put(ConcurrentLinkedQueue.class.getCanonicalName(), ConcurrentLinkedQueue.class);
        BUILT_IN_TYPE.put(LinkedBlockingDeque.class.getCanonicalName(), LinkedBlockingDeque.class);
        BUILT_IN_TYPE.put(ConcurrentLinkedDeque.class.getCanonicalName(), ConcurrentLinkedDeque.class);
        BUILT_IN_TYPE.put(PriorityQueue.class.getCanonicalName(), PriorityQueue.class);
        BUILT_IN_TYPE.put(DelayQueue.class.getCanonicalName(), DelayQueue.class);
        BUILT_IN_TYPE.put(Map.class.getCanonicalName(), Map.class);
        BUILT_IN_TYPE.put(HashMap.class.getCanonicalName(), HashMap.class);
        BUILT_IN_TYPE.put(LinkedHashMap.class.getCanonicalName(), LinkedHashMap.class);
        BUILT_IN_TYPE.put(IdentityHashMap.class.getCanonicalName(), IdentityHashMap.class);
        BUILT_IN_TYPE.put(ConcurrentMap.class.getCanonicalName(), ConcurrentMap.class);
        BUILT_IN_TYPE.put(ConcurrentHashMap.class.getCanonicalName(), ConcurrentHashMap.class);
        BUILT_IN_TYPE.put(SortedMap.class.getCanonicalName(), SortedMap.class);
        BUILT_IN_TYPE.put(NavigableMap.class.getCanonicalName(), NavigableMap.class);
        BUILT_IN_TYPE.put(TreeMap.class.getCanonicalName(), TreeMap.class);
        BUILT_IN_TYPE.put(Iterator.class.getCanonicalName(), Iterator.class);

        BUILT_IN_TYPE.put(File.class.getCanonicalName(), File.class);
        BUILT_IN_TYPE.put(InputStream.class.getCanonicalName(), InputStream.class);
        BUILT_IN_TYPE.put(ByteArrayInputStream.class.getCanonicalName(), ByteArrayInputStream.class);
        BUILT_IN_TYPE.put(FileInputStream.class.getCanonicalName(), FileInputStream.class);
        BUILT_IN_TYPE.put(OutputStream.class.getCanonicalName(), OutputStream.class);
        BUILT_IN_TYPE.put(ByteArrayOutputStream.class.getCanonicalName(), ByteArrayOutputStream.class);
        BUILT_IN_TYPE.put(FileOutputStream.class.getCanonicalName(), FileOutputStream.class);
        BUILT_IN_TYPE.put(Reader.class.getCanonicalName(), Reader.class);
        BUILT_IN_TYPE.put(StringReader.class.getCanonicalName(), StringReader.class);
        BUILT_IN_TYPE.put(FileReader.class.getCanonicalName(), FileReader.class);
        BUILT_IN_TYPE.put(InputStreamReader.class.getCanonicalName(), InputStreamReader.class);
        BUILT_IN_TYPE.put(Writer.class.getCanonicalName(), Writer.class);
        BUILT_IN_TYPE.put(StringWriter.class.getCanonicalName(), StringWriter.class);
        BUILT_IN_TYPE.put(FileWriter.class.getCanonicalName(), FileWriter.class);
        BUILT_IN_TYPE.put(OutputStreamWriter.class.getCanonicalName(), OutputStreamWriter.class);

        BUILT_IN_TYPE.put(Date.class.getCanonicalName(), Date.class);
        BUILT_IN_TYPE.put(Time.class.getCanonicalName(), Time.class);
        BUILT_IN_TYPE.put(Timestamp.class.getCanonicalName(), Timestamp.class);

        BUILT_IN_TYPE.put(Blob.class.getCanonicalName(), Blob.class);
        BUILT_IN_TYPE.put(Clob.class.getCanonicalName(), Clob.class);
        BUILT_IN_TYPE.put(NClob.class.getCanonicalName(), NClob.class);
        BUILT_IN_TYPE.put(SQLXML.class.getCanonicalName(), SQLXML.class);
        BUILT_IN_TYPE.put(RowId.class.getCanonicalName(), RowId.class);

        BUILT_IN_TYPE.put(URL.class.getCanonicalName(), URL.class);

        BUILT_IN_TYPE.put(BooleanList.class.getCanonicalName(), BooleanList.class);
        BUILT_IN_TYPE.put(CharList.class.getCanonicalName(), CharList.class);
        BUILT_IN_TYPE.put(ByteList.class.getCanonicalName(), ByteList.class);
        BUILT_IN_TYPE.put(ShortList.class.getCanonicalName(), ShortList.class);
        BUILT_IN_TYPE.put(IntList.class.getCanonicalName(), IntList.class);
        BUILT_IN_TYPE.put(LongList.class.getCanonicalName(), LongList.class);
        BUILT_IN_TYPE.put(FloatList.class.getCanonicalName(), FloatList.class);
        BUILT_IN_TYPE.put(DoubleList.class.getCanonicalName(), DoubleList.class);

        BUILT_IN_TYPE.put(MutableBoolean.class.getCanonicalName(), MutableBoolean.class);
        BUILT_IN_TYPE.put(MutableChar.class.getCanonicalName(), MutableChar.class);
        BUILT_IN_TYPE.put(MutableByte.class.getCanonicalName(), MutableByte.class);
        BUILT_IN_TYPE.put(MutableShort.class.getCanonicalName(), MutableShort.class);
        BUILT_IN_TYPE.put(MutableInt.class.getCanonicalName(), MutableInt.class);
        BUILT_IN_TYPE.put(MutableLong.class.getCanonicalName(), MutableLong.class);
        BUILT_IN_TYPE.put(MutableFloat.class.getCanonicalName(), MutableFloat.class);
        BUILT_IN_TYPE.put(MutableDouble.class.getCanonicalName(), MutableDouble.class);

        BUILT_IN_TYPE.put(OptionalBoolean.class.getCanonicalName(), OptionalBoolean.class);
        BUILT_IN_TYPE.put(OptionalChar.class.getCanonicalName(), OptionalChar.class);
        BUILT_IN_TYPE.put(OptionalByte.class.getCanonicalName(), OptionalByte.class);
        BUILT_IN_TYPE.put(OptionalShort.class.getCanonicalName(), OptionalShort.class);
        BUILT_IN_TYPE.put(OptionalInt.class.getCanonicalName(), OptionalInt.class);
        BUILT_IN_TYPE.put(OptionalLong.class.getCanonicalName(), OptionalLong.class);
        BUILT_IN_TYPE.put(OptionalFloat.class.getCanonicalName(), OptionalFloat.class);
        BUILT_IN_TYPE.put(OptionalDouble.class.getCanonicalName(), OptionalDouble.class);
        BUILT_IN_TYPE.put(Optional.class.getCanonicalName(), Optional.class);
        BUILT_IN_TYPE.put(Nullable.class.getCanonicalName(), Nullable.class);
        BUILT_IN_TYPE.put(Holder.class.getCanonicalName(), Holder.class);

        BUILT_IN_TYPE.put(Fraction.class.getCanonicalName(), Fraction.class);
        BUILT_IN_TYPE.put(Range.class.getCanonicalName(), Range.class);
        BUILT_IN_TYPE.put(Duration.class.getCanonicalName(), Duration.class);
        BUILT_IN_TYPE.put(Pair.class.getCanonicalName(), Pair.class);
        BUILT_IN_TYPE.put(Triple.class.getCanonicalName(), Triple.class);
        BUILT_IN_TYPE.put(Tuple.class.getCanonicalName(), Tuple.class);
        BUILT_IN_TYPE.put(Tuple1.class.getCanonicalName(), Tuple1.class);
        BUILT_IN_TYPE.put(Tuple2.class.getCanonicalName(), Tuple2.class);
        BUILT_IN_TYPE.put(Tuple3.class.getCanonicalName(), Tuple3.class);
        BUILT_IN_TYPE.put(Tuple4.class.getCanonicalName(), Tuple4.class);
        BUILT_IN_TYPE.put(Tuple5.class.getCanonicalName(), Tuple5.class);
        BUILT_IN_TYPE.put(Tuple6.class.getCanonicalName(), Tuple6.class);
        BUILT_IN_TYPE.put(Tuple7.class.getCanonicalName(), Tuple7.class);
        BUILT_IN_TYPE.put(Tuple8.class.getCanonicalName(), Tuple8.class);
        BUILT_IN_TYPE.put(Tuple9.class.getCanonicalName(), Tuple9.class);

        BUILT_IN_TYPE.put(BiMap.class.getCanonicalName(), BiMap.class);
        BUILT_IN_TYPE.put(ListMultimap.class.getCanonicalName(), ListMultimap.class);
        BUILT_IN_TYPE.put(SetMultimap.class.getCanonicalName(), SetMultimap.class);
        BUILT_IN_TYPE.put(Multimap.class.getCanonicalName(), Multimap.class);
        BUILT_IN_TYPE.put(Multiset.class.getCanonicalName(), Multiset.class);
        BUILT_IN_TYPE.put(HBaseColumn.class.getCanonicalName(), HBaseColumn.class);

        BUILT_IN_TYPE.put(Type.class.getCanonicalName(), Type.class);
        BUILT_IN_TYPE.put(DataSet.class.getCanonicalName(), DataSet.class);
        BUILT_IN_TYPE.put(RowDataSet.class.getCanonicalName(), RowDataSet.class);
        BUILT_IN_TYPE.put(Sheet.class.getCanonicalName(), Sheet.class);

        BUILT_IN_TYPE.put(Map.Entry.class.getCanonicalName(), Map.Entry.class);
        BUILT_IN_TYPE.put("java.util.Map.Entry", Map.Entry.class);
        BUILT_IN_TYPE.put("Map.Entry", Map.Entry.class);

        BUILT_IN_TYPE.put(Instant.class.getCanonicalName(), Instant.class);
        BUILT_IN_TYPE.put(LocalDate.class.getCanonicalName(), LocalDate.class);
        BUILT_IN_TYPE.put(LocalDateTime.class.getCanonicalName(), LocalDateTime.class);
        BUILT_IN_TYPE.put(LocalTime.class.getCanonicalName(), LocalTime.class);
        BUILT_IN_TYPE.put(OffsetDateTime.class.getCanonicalName(), OffsetDateTime.class);
        BUILT_IN_TYPE.put(OffsetTime.class.getCanonicalName(), OffsetTime.class);
        BUILT_IN_TYPE.put(ZonedDateTime.class.getCanonicalName(), ZonedDateTime.class);
        BUILT_IN_TYPE.put(Year.class.getCanonicalName(), Year.class);
        BUILT_IN_TYPE.put(YearMonth.class.getCanonicalName(), YearMonth.class);

        BUILT_IN_TYPE.put(Type.class.getCanonicalName(), Type.class);

        List<Class<?>> classes = new ArrayList<>(BUILT_IN_TYPE.values());
        for (final Class<?> cls : classes) {
            Class<?> arrayClass = cls;

            for (int i = 0; i < 7; i++) {
                arrayClass = java.lang.reflect.Array.newInstance(arrayClass, 0).getClass();

                BUILT_IN_TYPE.put(arrayClass.getCanonicalName(), arrayClass);
            }
        }

        classes = new ArrayList<>(BUILT_IN_TYPE.values());
        for (final Class<?> cls : classes) {
            if (cls.getCanonicalName().startsWith("java.util.Date")) {
                continue;
            }

            BUILT_IN_TYPE.put(cls.getSimpleName(), cls);
        }
        //
        // N.println("#########################################Builtin types================================");
        // N.println("size = " + BUILT_IN_TYPE.size());
        //
        // for (Map.Entry<String, Class<?>> entry : BUILT_IN_TYPE.entrySet()) {
        // N.println(entry.getKey() + " = " + entry.getValue());
        // }
    }

    private static final Map<String, String> SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME = new HashMap<>();

    static {
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(boolean.class.getName(), "Z");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(char.class.getName(), "C");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(byte.class.getName(), "B");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(short.class.getName(), "S");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(int.class.getName(), "I");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(long.class.getName(), "J");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(float.class.getName(), "F");
        SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.put(double.class.getName(), "D");
    }

    private static final Map<String, String> camelCasePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> lowerCaseWithUnderscorePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> upperCaseWithUnderscorePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<Class<?>, Boolean> registeredXMLBindingClassList = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Set<String>> registeredNonPropGetSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableList<String>> beanDeclaredPropNameListPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Field>> beanDeclaredPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Field>> beanPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> beanDeclaredPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> beanDeclaredPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> beanPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> beanPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, List<Method>>> beanInlinePropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    //    /** The Constant beanInlinePropSetMethodPool. */
    //    private static final Map<Class<?>, Map<String, List<Method>>> beanInlinePropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    // ...
    private static final Map<String, String> formalizedPropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<Method, String> methodPropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    // reserved words.
    private static final Map<String, String> keyWordMapper = new HashMap<>(16);

    static {
        keyWordMapper.put("class", "clazz");
    }

    private static final Set<String> nonGetSetMethodName = N.newHashSet(16);

    static {
        nonGetSetMethodName.add("getClass");
        nonGetSetMethodName.add("hashCode");
        nonGetSetMethodName.add("toString");
    }

    private static final Map<Class<?>, Package> packagePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, String> packageNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<String, Class<?>> clsNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, String> simpleClassNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, String> nameClassPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, String> canonicalClassNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Class<?>> enclosingClassPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Constructor<?>> classNoArgDeclaredConstructorPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<List<Class<?>>, Constructor<?>>> classDeclaredConstructorPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> classNoArgDeclaredMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Map<List<Class<?>>, Method>>> classDeclaredMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Class<?>> registeredNonBeanClass = new ObjectPool<>(POOL_SIZE);

    static {
        registeredNonBeanClass.put(Object.class, Object.class);
        registeredNonBeanClass.put(Class.class, Class.class);
        registeredNonBeanClass.put(Calendar.class, Calendar.class);
        registeredNonBeanClass.put(java.util.Date.class, java.util.Date.class);
        registeredNonBeanClass.put(java.sql.Date.class, java.sql.Date.class);
        registeredNonBeanClass.put(java.sql.Time.class, java.sql.Time.class);
        registeredNonBeanClass.put(java.sql.Timestamp.class, java.sql.Timestamp.class);
    }

    /**
     * The Constant CLASS_MASK.
     *
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static final Class<?> CLASS_MASK = ClassMask.class;

    /**
     * The Constant METHOD_MASK.
     *
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static final Method METHOD_MASK = ClassUtil.internalGetDeclaredMethod(ClassMask.class, "methodMask");

    /**
     * The Constant FIELD_MASK.
     *
     * @deprecated for internal only.
     */
    @Deprecated
    @Internal
    public static final Field FIELD_MASK;

    static {
        try {
            FIELD_MASK = ClassMask.class.getDeclaredField(ClassMask.FIELD_MASK);
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    private static final Map<String, String> builtinTypeNameMap = new HashMap<>(100);

    static {
        builtinTypeNameMap.put(boolean[].class.getName(), "boolean[]");
        builtinTypeNameMap.put(char[].class.getName(), "char[]");
        builtinTypeNameMap.put(byte[].class.getName(), "byte[]");
        builtinTypeNameMap.put(short[].class.getName(), "short[]");
        builtinTypeNameMap.put(int[].class.getName(), "int[]");
        builtinTypeNameMap.put(long[].class.getName(), "long[]");
        builtinTypeNameMap.put(float[].class.getName(), "float[]");
        builtinTypeNameMap.put(double[].class.getName(), "double[]");

        builtinTypeNameMap.put(Boolean[].class.getName(), "Boolean[]");
        builtinTypeNameMap.put(Character[].class.getName(), "Character[]");
        builtinTypeNameMap.put(Byte[].class.getName(), "Byte[]");
        builtinTypeNameMap.put(Short[].class.getName(), "Short[]");
        builtinTypeNameMap.put(Integer[].class.getName(), "Integer[]");
        builtinTypeNameMap.put(Long[].class.getName(), "Long[]");
        builtinTypeNameMap.put(Float[].class.getName(), "Float[]");
        builtinTypeNameMap.put(Double[].class.getName(), "Double[]");

        builtinTypeNameMap.put(String[].class.getName(), "String[]");
        builtinTypeNameMap.put(CharSequence[].class.getName(), "CharSequence[]");
        builtinTypeNameMap.put(Number[].class.getName(), "Number[]");
        builtinTypeNameMap.put(Object[].class.getName(), "Object[]");

        for (final String key : new ArrayList<>(builtinTypeNameMap.keySet())) {
            builtinTypeNameMap.put("class " + key, builtinTypeNameMap.get(key));
        }

        builtinTypeNameMap.put(Boolean.class.getName(), "Boolean");
        builtinTypeNameMap.put(Character.class.getName(), "Character");
        builtinTypeNameMap.put(Byte.class.getName(), "Byte");
        builtinTypeNameMap.put(Short.class.getName(), "Short");
        builtinTypeNameMap.put(Integer.class.getName(), "Integer");
        builtinTypeNameMap.put(Long.class.getName(), "Long");
        builtinTypeNameMap.put(Float.class.getName(), "Float");
        builtinTypeNameMap.put(Double.class.getName(), "Double");

        builtinTypeNameMap.put(String.class.getName(), "String");
        builtinTypeNameMap.put(CharSequence.class.getName(), "CharSequence");
        builtinTypeNameMap.put(Number.class.getName(), "Number");
        builtinTypeNameMap.put(Object.class.getName(), "Object");
    }

    /**
     * Registers a class as a non-bean class.
     *
     * @param cls the class to be registered as a non-bean class
     */
    @SuppressWarnings("deprecation")
    public static void registerNonBeanClass(final Class<?> cls) {
        registeredNonBeanClass.put(cls, cls);

        synchronized (beanDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, false);

            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                beanDeclaredPropGetMethodPool.remove(cls);
                beanDeclaredPropSetMethodPool.remove(cls);

                beanPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a non-property get/set method for the specified class.
     *
     * @param cls the class for which the non-property get/set method is to be registered
     * @param propName the name of the property to be registered as a non-property get/set method
     */
    @SuppressWarnings("deprecation")
    public static void registerNonPropGetSetMethod(final Class<?> cls, final String propName) {
        synchronized (registeredNonPropGetSetMethodPool) {
            Set<String> set = registeredNonPropGetSetMethodPool.computeIfAbsent(cls, k -> N.newHashSet());

            set.add(propName);

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a property get/set method for the specified property name.
     *
     * @param propName the name of the property
     * @param method the method to be registered as a property get/set method
     */
    @SuppressWarnings("deprecation")
    public static void registerPropGetSetMethod(final String propName, final Method method) {
        final Class<?> cls = method.getDeclaringClass();

        synchronized (beanDeclaredPropGetMethodPool) {
            if (isGetMethod(method)) {
                Map<String, Method> propMethodMap = beanPropGetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = beanPropGetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been registered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else if (isSetMethod(method)) {
                Map<String, Method> propMethodMap = beanPropSetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = beanPropSetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been registered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else {
                throw new IllegalArgumentException("The name of property getter/setter method must start with 'get/is/has' or 'set': " + method.getName());
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * The property maybe only have get method if its type is collection or map by xml binding specification
     * Otherwise, it will be ignored if not registered as an XML binding class.
     *
     * @param cls the class to be registered for XML binding
     */
    @SuppressWarnings("deprecation")
    public static void registerXMLBindingClass(final Class<?> cls) {
        if (registeredXMLBindingClassList.containsKey(cls)) {
            return;
        }

        synchronized (beanDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, true);

            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                beanDeclaredPropGetMethodPool.remove(cls);
                beanDeclaredPropSetMethodPool.remove(cls);

                beanPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Checks if the specified class is registered for XML binding.
     *
     * @param cls the class to check
     * @return {@code true} if the class is registered for XML binding, {@code false} otherwise
     */
    public static boolean isRegisteredXMLBindingClass(final Class<?> cls) {
        return registeredXMLBindingClassList.containsKey(cls);
    }

    /**
     * Creates a MethodHandle for the specified method.
     *
     * @param method the method for which the MethodHandle is to be created
     * @return the MethodHandle for the specified method
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static MethodHandle createMethodHandle(final Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();

        try {
            return MethodHandles.lookup().in(declaringClass).unreflectSpecial(method, declaringClass);
        } catch (final Exception e) {
            try {
                final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                ClassUtil.setAccessible(constructor, true);

                return constructor.newInstance(declaringClass).in(declaringClass).unreflectSpecial(method, declaringClass);
            } catch (final Exception ex) {
                try {
                    return MethodHandles.lookup()
                            .findSpecial(declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                                    declaringClass);
                } catch (final Exception exx) {
                    throw new UnsupportedOperationException(exx);
                }
            }
        }
    }

    /**
     * <p>Returns the number of inheritance hops between two classes.</p>
     *
     * @param child the child class, may be {@code null}
     * @param parent the parent class, may be {@code null}
     * @return the number of generations between the child and parent; 0 if the same class;
     * -1 if the classes are not related as child and parent (includes where either class is null)
     */
    public static int distanceOfInheritance(final Class<?> child, final Class<?> parent) {
        if (child == null || parent == null) {
            return -1;
        }

        if (child.equals(parent)) {
            return 0;
        }

        final Class<?> cParent = child.getSuperclass();
        int d = parent.equals(cParent) ? 1 : 0;

        if (d == 1) {
            return d;
        }

        d += distanceOfInheritance(cParent, parent);

        return d > 0 ? d + 1 : -1;
    }

    /**
     * File path 2 package name.
     *
     * @param entryName
     * @return
     */
    private static String filePath2PackageName(final String entryName) {
        final String pkgName = entryName.replace('/', '.').replace('\\', '.');
        return pkgName.endsWith(".") ? pkgName.substring(0, pkgName.length() - 1) : pkgName;
    }

    // Superclasses/Superinterfaces. Copied from Apache Commons Lang under Apache License v2.
    // ----------------------------------------------------------------------

    /**
     * Returns the Class object associated with the class or interface with the given string name.
     * This method supports primitive types: boolean, char, byte, short, int, long, float, double. And array type with format {@code java.lang.String[]}
     *
     * @param <T>
     * @param clsName
     * @return
     * @throws IllegalArgumentException if class not found.
     */
    public static <T> Class<T> forClass(final String clsName) throws IllegalArgumentException {
        return forClass(clsName, true);
    }

    /**
     * Supports primitive types: boolean, char, byte, short, int, long, float, double. And array type with format {@code java.lang.String[]}
     *
     * @param <T>
     * @param clsName
     * @param cacheResult
     * @return
     * @throws IllegalArgumentException if class not found.
     */
    @SuppressWarnings("unchecked")
    static <T> Class<T> forClass(final String clsName, final boolean cacheResult) throws IllegalArgumentException {
        Class<?> cls = clsNamePool.get(clsName);

        if (cls == null) {
            cls = BUILT_IN_TYPE.get(clsName);

            if (cls == null) {
                try {
                    cls = Class.forName(clsName); // NOSONAR
                } catch (final ClassNotFoundException e) {
                    String newClassName = clsName;

                    if (newClassName.indexOf(WD._PERIOD) < 0) {
                        final int index = newClassName.indexOf("[]");

                        if (((index < 0) && !SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.containsKey(newClassName))
                                || ((index > 0) && !SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.containsKey(newClassName.substring(0, index)))) {
                            newClassName = "java.lang." + newClassName;

                            try {
                                cls = Class.forName(newClassName); // NOSONAR

                                BUILT_IN_TYPE.put(clsName, cls);
                            } catch (final ClassNotFoundException e1) {
                                // ignore.
                            }
                        }
                    }

                    if (cls == null) {
                        newClassName = clsName;
                        final int index = newClassName.indexOf("[]");

                        if (index > 0) {
                            final String componentTypeName = newClassName.substring(0, index);
                            final String temp = newClassName.replace("[]", ""); //NOSONAR

                            if (componentTypeName.equals(temp)) {
                                int dimensions = (newClassName.length() - temp.length()) / 2;
                                String prefixOfArray = "";

                                while (dimensions-- > 0) {
                                    //noinspection StringConcatenationInLoop
                                    prefixOfArray += "["; //NOSONAR
                                }

                                final String symbolOfPrimitiveArrayClassName = SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.get(componentTypeName);

                                if (symbolOfPrimitiveArrayClassName != null) {
                                    try {
                                        cls = Class.forName(prefixOfArray + symbolOfPrimitiveArrayClassName); // NOSONAR

                                        BUILT_IN_TYPE.put(clsName, cls);
                                    } catch (final ClassNotFoundException e2) {
                                        // ignore.
                                    }
                                } else {
                                    try {
                                        final Type<?> componentType = N.typeOf(componentTypeName);

                                        if (componentType.isObjectType() && !componentType.name().equals(ObjectType.OBJECT)) {
                                            throw new IllegalArgumentException("No Class found by name: " + clsName);
                                        }

                                        cls = Class.forName(prefixOfArray + "L" + componentType.clazz().getCanonicalName() + ";"); // NOSONAR
                                    } catch (final ClassNotFoundException e3) {
                                        // ignore.
                                    }
                                }
                            }
                        }

                        if (cls == null) {
                            newClassName = clsName;
                            int lastIndex = -1;

                            while ((lastIndex = newClassName.lastIndexOf(WD._PERIOD)) > 0) {
                                newClassName = newClassName.substring(0, lastIndex) + "$" + newClassName.substring(lastIndex + 1);

                                try {
                                    cls = Class.forName(newClassName); // NOSONAR
                                    break;
                                } catch (final ClassNotFoundException e3) {
                                    // ignore.
                                }
                            }
                        }
                    }
                }
            }

            if (cls == null) {
                throw new IllegalArgumentException("No class found by name: " + clsName);
            }

            if (cacheResult) {
                clsNamePool.put(clsName, cls);
            }
        }

        return (Class<T>) cls;
    }

    /**
     * Formalizes the given property name by converting it to camel case and replacing any reserved keywords with their mapped values.
     * It's designed for field/method/class/column/table names. and source and target Strings will be cached.
     *
     * @param propName the property name to be formalized
     * @return the formalized property name
     */
    public static String formalizePropName(final String propName) {
        String newPropName = formalizedPropNamePool.get(propName);

        if (newPropName == null) {
            newPropName = toCamelCase(propName);

            for (final Map.Entry<String, String> entry : keyWordMapper.entrySet()) { //NOSONAR
                if (entry.getKey().equalsIgnoreCase(newPropName)) {
                    newPropName = entry.getValue();

                    break;
                }
            }

            formalizedPropNamePool.put(propName, newPropName);
        }

        return newPropName;
    }

    /**
     * Formats the parameterized type name by removing unnecessary prefixes and suffixes.
     * This method handles array types and removes "class" and "interface" prefixes.
     *
     * @param parameterizedTypeName the parameterized type name to format
     * @return the formatted parameterized type name
     */
    public static String formatParameterizedTypeName(final String parameterizedTypeName) {
        String res = builtinTypeNameMap.get(parameterizedTypeName);

        if (res != null) {
            return res;
        }

        res = parameterizedTypeName;

        if (res.startsWith("class [L") && res.endsWith(";")) {
            res = res.substring("class [L".length(), res.length() - 1) + "[]";
        }

        if (res.startsWith("interface [L") && res.endsWith(";")) {
            res = res.substring("interface [L".length(), res.length() - 1) + "[]";
        }

        res = res.replaceAll("java.lang.", "").replace("class ", "").replace("interface ", ""); //NOSONAR

        final int idx = res.lastIndexOf('$');

        if (idx > 0) {
            final StringBuilder sb = new StringBuilder();

            for (int len = res.length(), i = len - 1; i >= 0; i--) {
                final char ch = res.charAt(i);
                sb.append(ch);

                if (ch == '$') {
                    final int j = i;
                    char x = 0;
                    //noinspection StatementWithEmptyBody
                    while (--i >= 0 && (Character.isLetterOrDigit(x = res.charAt(i)) || x == '_' || x == '.')) {
                        // continue
                    }

                    final String tmp = res.substring(i + 1, j);

                    if (tmp.substring(0, tmp.length() / 2).equals(tmp.substring(tmp.length() / 2 + 1))) {
                        sb.append(Strings.reverse(tmp.substring(0, tmp.length() / 2)));
                    } else {
                        sb.append(Strings.reverse(tmp));
                    }

                    i++;
                }
            }

            res = sb.reverse().toString();
        }

        return res;
    }

    /**
     * Retrieves the canonical name of the specified class.
     * If the canonical name is not available, it returns the class name.
     *
     * @param cls the class whose canonical name is to be retrieved
     * @return the canonical name of the class, or the class name if the canonical name is not available
     * @see Class#getCanonicalName()
     */
    public static String getCanonicalClassName(final Class<?> cls) {
        String clsName = canonicalClassNamePool.get(cls);

        if (clsName == null) {
            clsName = cls.getCanonicalName();

            if (clsName == null) {
                clsName = cls.getName();
            }

            canonicalClassNamePool.put(cls, clsName);
        }

        return clsName;
    }

    /**
     * Retrieves the name of the specified class.
     *
     * @param cls the class whose name is to be retrieved
     * @return the name of the class
     */

    public static String getClassName(final Class<?> cls) {

        return nameClassPool.computeIfAbsent(cls, k -> cls.getName());
    }

    //    private static Class[] getTypeArguments(Class cls) {
    //        java.lang.reflect.Type[] typeArgs = null;
    //        java.lang.reflect.Type[] genericInterfaces = cls.getGenericInterfaces();
    //
    //        if (notEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notEmpty(typeArgs)) {
    //                    break;
    //                }
    //            }
    //        } else {
    //            java.lang.reflect.Type genericSuperclass = cls.getGenericSuperclass();
    //
    //            if (genericSuperclass != null) {
    //                typeArgs = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
    //            }
    //        }
    //
    //        if (notEmpty(typeArgs)) {
    //            Class[] classes = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                classes[i] = (Class) typeArgs[i];
    //            }
    //
    //            return classes;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case-insensitive
    //     * @return {@code null} if no method is found by specified name
    //     */
    //    public static Method findDeclaredMethodByName(Class<?> cls, String methodName) {
    //        Method method = null;
    //
    //        Method[] methods = cls.getDeclaredMethods();
    //
    //        for (Method m : methods) {
    //            if (m.getName().equalsIgnoreCase(methodName)) {
    //                if ((method == null) || Modifier.isPublic(m.getModifiers())
    //                        || (Modifier.isProtected(m.getModifiers()) && (!Modifier.isProtected(method.getModifiers())))
    //                        || (!Modifier.isPrivate(m.getModifiers()) && Modifier.isPrivate(method.getModifiers()))) {
    //
    //                    method = m;
    //                }
    //
    //                if (Modifier.isPublic(method.getModifiers())) {
    //                    break;
    //                }
    //            }
    //        }
    //
    //        // SHOULD NOT set it true here.
    //        // if (method != null) {
    //        // ClassUtil.setAccessible(method, true);
    //        // }
    //
    //        return method;
    //    }

    /**
     * Retrieves the simple name of the specified class.
     *
     * @param cls the class whose simple name is to be retrieved
     * @return the simple name of the class
     */
    public static String getSimpleClassName(final Class<?> cls) {

        return simpleClassNamePool.computeIfAbsent(cls, k -> cls.getSimpleName());
    }

    /**
     * Gets the all interfaces.
     *
     * @param cls
     * @param interfacesFound
     * @return
     */
    private static void getAllInterfaces(Class<?> cls, final Set<Class<?>> interfacesFound) {
        while (cls != null) {
            final Class<?>[] interfaces = cls.getInterfaces();

            for (final Class<?> i : interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound);
                }
            }

            cls = cls.getSuperclass();
        }
    }

    /**
     * Gets the all super types.
     *
     * @param cls
     * @param superTypesFound
     * @return
     */
    private static void getAllSuperTypes(Class<?> cls, final Set<Class<?>> superTypesFound) {
        while (cls != null) {
            final Class<?>[] interfaces = cls.getInterfaces();

            for (final Class<?> i : interfaces) {
                if (superTypesFound.add(i)) {
                    getAllInterfaces(i, superTypesFound);
                }
            }

            final Class<?> superclass = cls.getSuperclass();

            if (superclass != null && !superclass.equals(Object.class) && superTypesFound.add(superclass)) {
                getAllSuperTypes(superclass, superTypesFound);
            }

            cls = cls.getSuperclass();
        }
    }

    /**
     * Retrieves the package of the specified class.
     *
     * @param cls the class whose package is to be retrieved
     * @return the package of the class, or {@code null} if the class is a primitive type or no package is defined
     */
    public static Package getPackage(final Class<?> cls) {
        Package pkg = packagePool.get(cls);

        if (pkg == null) {
            if (ClassUtil.isPrimitiveType(cls)) {
                return null;
            }

            pkg = cls.getPackage();

            if (pkg != null) {
                packagePool.put(cls, pkg);
            }
        }

        return pkg;
    }

    /**
     * Retrieves the package name of the specified class.
     * If the class is a primitive type or no package is defined, it returns an empty string.
     *
     * @param cls the class whose package name is to be retrieved
     * @return the package name of the class, or an empty string if the class is a primitive type or no package is defined
     */
    public static String getPackageName(final Class<?> cls) {
        String pkgName = packageNamePool.get(cls);

        if (pkgName == null) {
            final Package pkg = ClassUtil.getPackage(cls);
            pkgName = pkg == null ? "" : pkg.getName();
            packageNamePool.put(cls, pkgName);
        }

        return pkgName;
    }

    /**
     * Retrieves a list of classes in the specified package.
     *
     * @param pkgName the name of the package to search for classes
     * @param isRecursive if {@code true}, searches recursively in sub-packages
     * @param skipClassLoadingException if {@code true}, skips classes that cannot be loaded
     * @return a list of classes in the specified package
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static List<Class<?>> getClassesByPackage(final String pkgName, final boolean isRecursive, final boolean skipClassLoadingException)
            throws UncheckedIOException {
        return getClassesByPackage(pkgName, isRecursive, skipClassLoadingException, Fn.alwaysTrue());
    }

    /**
     * Retrieves a list of classes in the specified package.
     *
     * @param pkgName the name of the package to search for classes
     * @param isRecursive if {@code true}, searches recursively in sub-packages
     * @param skipClassLoadingException if {@code true}, skips classes that cannot be loaded
     * @param predicate a predicate to filter the classes
     * @return a list of classes in the specified package
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static List<Class<?>> getClassesByPackage(final String pkgName, final boolean isRecursive, final boolean skipClassLoadingException,
            final Predicate<? super Class<?>> predicate) throws UncheckedIOException {
        if (logger.isInfoEnabled()) {
            logger.info("Looking for classes in package: " + pkgName);
        }

        final String pkgPath = packageName2FilePath(pkgName);

        final List<URL> resourceList = getResources(pkgName);

        if (N.isEmpty(resourceList)) {
            throw new IllegalArgumentException("No resource found by package " + pkgName);
        }

        final List<Class<?>> classes = new ArrayList<>();
        for (final URL resource : resourceList) {
            // Get a File object for the package
            final String fullPath = resource.getPath().replace("%20", " ").replaceFirst("[.]jar[!].*", JAR_POSTFIX).replaceFirst("file:", "");//NOSONAR

            if (logger.isInfoEnabled()) {
                logger.info("ClassDiscovery: FullPath = " + fullPath);
            }

            final File file = new File(fullPath);

            if (file.exists() && file.isDirectory()) {
                // Get the list of the files contained in the package
                final File[] files = file.listFiles();

                if (N.isEmpty(files)) {
                    continue;
                }

                for (final File file2 : files) {
                    if (file2 == null) {
                        continue;
                    }

                    // we are only interested in .class files
                    if (file2.isFile() && file2.getName().endsWith(CLASS_POSTFIX)) {
                        // removes the .class extension
                        final String className = pkgName + '.' + file2.getName().substring(0, file2.getName().length() - CLASS_POSTFIX.length());

                        try {
                            final Class<?> clazz = ClassUtil.forClass(className, false);

                            if (clazz.getCanonicalName() != null && predicate.test(clazz)) {
                                classes.add(clazz);
                            }
                        } catch (final Throwable e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(e, "Failed to load class: " + className);
                            }

                            if (!skipClassLoadingException) {
                                throw new RuntimeException("ClassNotFoundException loading " + className); //NOSONAR
                            }
                        }
                    } else if (file2.isDirectory() && isRecursive) {
                        final String subPkgName = pkgName + WD._PERIOD + file2.getName();
                        //noinspection ConstantValue
                        classes.addAll(getClassesByPackage(subPkgName, isRecursive, skipClassLoadingException, predicate));
                    }
                }
            } else if (file.exists() && file.getName().endsWith(JAR_POSTFIX)) {
                JarFile jarFile = null;

                try { //NOSONAR
                    jarFile = new JarFile(file.getPath());

                    final Enumeration<JarEntry> entries = jarFile.entries();
                    JarEntry entry = null;
                    String entryName = null;

                    while (entries.hasMoreElements()) {
                        entry = entries.nextElement();
                        entryName = entry.getName();

                        if (entryName.startsWith(pkgPath)) {
                            if (entryName.endsWith(CLASS_POSTFIX) && (entryName.indexOf("/", pkgPath.length()) < 0)) {
                                final String className = filePath2PackageName(entryName).replace(CLASS_POSTFIX, "");

                                try { //NOSONAR
                                    final Class<?> clazz = ClassUtil.forClass(className, false);

                                    if ((clazz.getCanonicalName() != null) && (clazz.getPackage().getName().equals(pkgName)
                                            || (clazz.getPackage().getName().startsWith(pkgName) && isRecursive)) && predicate.test(clazz)) {
                                        classes.add(clazz);
                                    }
                                } catch (final Throwable e) {
                                    if (logger.isWarnEnabled()) {
                                        logger.warn("ClassNotFoundException loading " + className);
                                    }

                                    if (!skipClassLoadingException) {
                                        IOUtil.close(jarFile);
                                        jarFile = null;
                                        throw new RuntimeException("ClassNotFoundException loading " + className);
                                    }
                                }
                            } else if (entry.isDirectory() && (entryName.length() > (pkgPath.length() + 1)) && isRecursive) {
                                final String subPkgName = filePath2PackageName(entryName);
                                //noinspection ConstantValue
                                classes.addAll(getClassesByPackage(subPkgName, isRecursive, skipClassLoadingException, predicate));
                            }
                        }
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(pkgName + " (" + file + ") does not appear to be a valid package", e);
                } finally {
                    IOUtil.close(jarFile);
                }
            }

        }

        return classes;
    }

    //    private static Class[] getTypeArguments(Class cls) {
    //        java.lang.reflect.Type[] typeArgs = null;
    //        java.lang.reflect.Type[] genericInterfaces = cls.getGenericInterfaces();
    //
    //        if (notEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notEmpty(typeArgs)) {
    //                    break;
    //                }
    //            }
    //        } else {
    //            java.lang.reflect.Type genericSuperclass = cls.getGenericSuperclass();
    //
    //            if (genericSuperclass != null) {
    //                typeArgs = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
    //            }
    //        }
    //
    //        if (notEmpty(typeArgs)) {
    //            Class[] classes = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                classes[i] = (Class) typeArgs[i];
    //            }
    //
    //            return classes;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case-insensitive
    //     * @return {@code null} if no method is found by specified name
    //     */
    //    public static Method findDeclaredMethodByName(Class<?> cls, String methodName) {
    //        Method method = null;
    //
    //        Method[] methods = cls.getDeclaredMethods();
    //
    //        for (Method m : methods) {
    //            if (m.getName().equalsIgnoreCase(methodName)) {
    //                if ((method == null) || Modifier.isPublic(m.getModifiers())
    //                        || (Modifier.isProtected(m.getModifiers()) && (!Modifier.isProtected(method.getModifiers())))
    //                        || (!Modifier.isPrivate(m.getModifiers()) && Modifier.isPrivate(method.getModifiers()))) {
    //
    //                    method = m;
    //                }
    //
    //                if (Modifier.isPublic(method.getModifiers())) {
    //                    break;
    //                }
    //            }
    //        }
    //
    //        // SHOULD NOT set it true here.
    //        // if (method != null) {
    //        // ClassUtil.setAccessible(method, true);
    //        // }
    //
    //        return method;
    //    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     *
     * <p>Gets a {@code List} of all interfaces implemented by the given
     * class and its super classes.</p>
     *
     * <p>The order is determined by looking through each interface in turn as
     * declared in the source file and following its hierarchy up. Then each
     * superclass is considered in the same way. Later duplicates are ignored,
     * so the order is maintained.</p>
     *
     * @param cls the class to look up.
     * @return
     */
    public static Set<Class<?>> getAllInterfaces(final Class<?> cls) {
        final Set<Class<?>> interfacesFound = N.newLinkedHashSet();

        getAllInterfaces(cls, interfacesFound);

        return interfacesFound;
    }

    /**
     * Copied from Apache Commons Lang under Apache License v2.
     *
     * <p>Gets a {@code List} of super classes for the given class, excluding {@code Object.class}.</p>
     *
     * @param cls the class to look up.
     * @return
     */
    public static List<Class<?>> getAllSuperclasses(final Class<?> cls) {
        final List<Class<?>> classes = new ArrayList<>();
        Class<?> superclass = cls.getSuperclass();

        while (superclass != null && !superclass.equals(Object.class)) {
            classes.add(superclass);
            superclass = superclass.getSuperclass();
        }

        return classes;
    }

    /**
     * Returns all the interfaces and super classes the specified class implements or extends, excluding {@code Object.class}.
     *
     * @param cls
     * @return
     */
    public static Set<Class<?>> getAllSuperTypes(final Class<?> cls) {
        final Set<Class<?>> superTypesFound = N.newLinkedHashSet();

        getAllSuperTypes(cls, superTypesFound);

        return superTypesFound;
    }

    /**
     * Retrieves the enclosing class of the specified class.
     *
     * @param cls the class whose enclosing class is to be retrieved
     * @return the enclosing class of the specified class, or {@code null} if the class is not an inner class
     */
    public static Class<?> getEnclosingClass(final Class<?> cls) {
        Class<?> enclosingClass = enclosingClassPool.get(cls);

        if (enclosingClass == null) {
            enclosingClass = cls.getEnclosingClass();

            if (enclosingClass == null) {
                enclosingClass = CLASS_MASK;
            }

            enclosingClassPool.put(cls, enclosingClass);
        }

        return (enclosingClass == CLASS_MASK) ? null : enclosingClass;
    }

    /**
     * Returns the constructor declared in the specified {@code cls} with the specified {@code parameterTypes}.
     * {@code null} is returned if no constructor is found.
     *
     * @param <T> the type of the class
     * @param cls the class object
     * @param parameterTypes the parameter types of the constructor
     * @return the constructor declared in the specified class with the specified parameter types, or {@code null} if no constructor is found.
     */
    public static <T> Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?>... parameterTypes) {
        Constructor<?> constructor = null;

        if (parameterTypes == null || parameterTypes.length == 0) {
            constructor = classNoArgDeclaredConstructorPool.get(cls);

            if (constructor == null) {
                try {
                    constructor = cls.getDeclaredConstructor(parameterTypes);

                    // SHOULD NOT set it true here.
                    // ClassUtil.setAccessible(constructor, true);
                } catch (final NoSuchMethodException e) {
                    // ignore.
                }

                if (constructor != null) {
                    classNoArgDeclaredConstructorPool.put(cls, constructor);
                }
            }
        } else {
            final List<Class<?>> parameterTypeList = Array.asList(parameterTypes);

            Map<List<Class<?>>, Constructor<?>> constructorPool = classDeclaredConstructorPool.get(cls);

            if (constructorPool != null) {
                constructor = constructorPool.get(parameterTypeList);
            }

            if (constructor == null) {
                try {
                    constructor = cls.getDeclaredConstructor(parameterTypes);

                    // SHOULD NOT set it true here.
                    // ClassUtil.setAccessible(constructor, true);
                } catch (final NoSuchMethodException e) {
                    // ignore.
                }

                if (constructor != null) {
                    if (constructorPool == null) {
                        constructorPool = new ConcurrentHashMap<>();
                        classDeclaredConstructorPool.put(cls, constructorPool);
                    }

                    constructorPool.put(Array.asList(parameterTypes.clone()), constructor);
                }
            }

        }

        return (Constructor<T>) constructor;
    }

    /**
     * Retrieves the declared field with the specified name from the given class.
     * {@code null} is returned if no field is found by the specified name.
     *
     * @param cls the class from which the field is to be retrieved
     * @param fieldName the name of the field to retrieve
     * @return the declared field with the specified name
     */
    private static Field getDeclaredField(final Class<?> cls, final String fieldName) {
        try {
            return cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException e) {
            // ignore
        }

        return null;
    }

    /**
     * Returns the method declared in the specified {@code cls} with the specified {@code methodName} and {@code parameterTypes}.
     * {@code null} is returned if no method is found by the specified name.
     *
     * @param cls the class object
     * @param methodName the name of the method to retrieve
     * @param parameterTypes the parameter types of the method
     * @return the method declared in the specified class with the specified name and parameter types, or {@code null} if no method is found
     */
    public static Method getDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
        Method method = null;

        if (parameterTypes == null || parameterTypes.length == 0) {
            Map<String, Method> methodNamePool = classNoArgDeclaredMethodPool.get(cls);
            method = methodNamePool == null ? null : methodNamePool.get(methodName);

            if (method == null) {
                method = internalGetDeclaredMethod(cls, methodName, parameterTypes);

                // SHOULD NOT set it true here.
                // if (method != null) {
                // ClassUtil.setAccessible(method, true);
                // }

                if (method != null) {
                    if (methodNamePool == null) {
                        methodNamePool = new ConcurrentHashMap<>();
                        classNoArgDeclaredMethodPool.put(cls, methodNamePool);
                    }

                    methodNamePool.put(methodName, method);
                }
            }
        } else {
            final List<Class<?>> parameterTypeList = Array.asList(parameterTypes);
            Map<String, Map<List<Class<?>>, Method>> methodNamePool = classDeclaredMethodPool.get(cls);
            Map<List<Class<?>>, Method> methodPool = methodNamePool == null ? null : methodNamePool.get(methodName);

            if (methodPool != null) {
                method = methodPool.get(parameterTypeList);
            }

            if (method == null) {
                method = internalGetDeclaredMethod(cls, methodName, parameterTypes);

                // SHOULD NOT set it true here.
                // if (method != null) {
                // ClassUtil.setAccessible(method, true);
                // }

                if (method != null) {
                    if (methodNamePool == null) {
                        methodNamePool = new ConcurrentHashMap<>();
                        classDeclaredMethodPool.put(cls, methodNamePool);
                    }

                    if (methodPool == null) {
                        methodPool = new ConcurrentHashMap<>();
                        methodNamePool.put(methodName, methodPool);
                    }

                    methodPool.put(Array.asList(parameterTypes.clone()), method);
                }
            }
        }

        return method;
    }

    /**
     * Gets the parameterized type name of the specified field.
     *
     * @param field the field whose parameterized type name is to be retrieved
     * @return the parameterized type name of the field
     */
    public static String getParameterizedTypeNameByField(final Field field) {
        final String typeName = formatParameterizedTypeName(field.getGenericType().getTypeName());

        if (Strings.isNotEmpty(typeName) && typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0) { // NOSONAR
            try {
                final Type<Object> type = N.typeOf(typeName);

                if (field.getType().isAssignableFrom(type.clazz())) {
                    return type.name();
                }
            } catch (final Throwable e) {
                // ignore.
            }
        }

        return N.typeOf(field.getType()).name();
    }

    /**
     * Gets the parameterized type name of the specified method.
     *
     * @param method the method whose parameterized type name is to be retrieved
     * @return the parameterized type name of the method
     */
    public static String getParameterizedTypeNameByMethod(final Method method) {
        String typeName = null;

        final java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();

        if (N.notEmpty(genericParameterTypes)) {
            typeName = formatParameterizedTypeName(genericParameterTypes[0].getTypeName());
        } else {
            typeName = formatParameterizedTypeName(method.getGenericReturnType().getTypeName());
        }

        Class<?> methodType = null;

        if (Strings.isNotEmpty(typeName) && typeName.indexOf('<') > 0 && typeName.indexOf('>') > 0) { // NOSONAR
            try {
                final Type<Object> type = N.typeOf(typeName);
                methodType = N.notEmpty(genericParameterTypes) ? method.getParameterTypes()[0] : method.getReturnType();

                if (methodType.isAssignableFrom(type.clazz())) {
                    return type.name();
                }
            } catch (final Throwable e) {
                // ignore.
            }
        }

        if (methodType == null) {
            methodType = N.notEmpty(genericParameterTypes) ? method.getParameterTypes()[0] : method.getReturnType();
        }

        return N.typeOf(methodType).name();
    }

    /**
     * Retrieves the property name associated with the specified getter or setter method.
     *
     * @param getSetMethod the method whose property name is to be retrieved
     * @return the property name associated with the specified method
     */
    public static String getPropNameByMethod(final Method getSetMethod) {
        String propName = methodPropNamePool.get(getSetMethod);

        if (propName == null) {
            final String methodName = getSetMethod.getName();
            final Class<?>[] paramTypes = getSetMethod.getParameterTypes();
            final Class<?> targetType = N.isEmpty(paramTypes) ? getSetMethod.getReturnType() : paramTypes[0];

            Field field = getDeclaredField(getSetMethod.getDeclaringClass(), methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            if (Strings.isEmpty(propName) && ((methodName.startsWith(IS) && methodName.length() > 2)
                    || ((methodName.startsWith(GET) || methodName.startsWith(SET) || methodName.startsWith(HAS)) && methodName.length() > 3))) {
                final String newName = methodName.substring(methodName.startsWith(IS) ? 2 : 3);
                field = getDeclaredField(getSetMethod.getDeclaringClass(), Strings.uncapitalize(newName));

                if (field != null && field.getType().isAssignableFrom(targetType)) {
                    propName = field.getName();
                }

                if (Strings.isEmpty(propName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + Strings.uncapitalize(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName)) {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName)) {
                    propName = formalizePropName(newName);
                }
            }

            if (Strings.isEmpty(propName)) {
                propName = methodName;
            }

            methodPropNamePool.put(getSetMethod, propName);
        }

        return propName;
    }

    /**
     * Returns an immutable list of property names for the specified class.
     *
     * @param cls the class whose property names are to be retrieved
     * @return an immutable list of property names for the specified class
     */
    public static ImmutableList<String> getPropNameList(final Class<?> cls) {
        ImmutableList<String> propNameList = beanDeclaredPropNameListPool.get(cls);

        if (propNameList == null) {
            loadPropGetSetMethodList(cls);
            propNameList = beanDeclaredPropNameListPool.get(cls);
        }

        return propNameList;
    }

    /**
     * Retrieves a list of property names for the specified class, excluding the specified property names.
     *
     * @param cls the class whose property names are to be retrieved
     * @param propNameToExclude the collection of property names to exclude from the result
     * @return a list of property names for the specified class, excluding the specified property names
     * @deprecated replaced by {@link #getPropNames(Class, Set)}
     * @see #getPropNames(Class, Set)
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static List<String> getPropNames(final Class<?> cls, final Collection<String> propNameToExclude) {
        if (N.isEmpty(propNameToExclude)) {
            return new ArrayList<>(getPropNameList(cls));
        }

        if (propNameToExclude instanceof Set) {
            return getPropNames(cls, (Set) propNameToExclude);
        }

        return getPropNames(cls, N.newHashSet(propNameToExclude));
    }

    /**
     * Retrieves a list of property names for the specified class, excluding the specified property names.
     *
     * @param cls the class whose property names are to be retrieved
     * @param propNameToExclude the set of property names to exclude from the result
     * @return a list of property names for the specified class, excluding the specified property names
     */
    public static List<String> getPropNames(final Class<?> cls, final Set<String> propNameToExclude) {
        final ImmutableList<String> propNameList = getPropNameList(cls);

        if (N.isEmpty(propNameToExclude)) {
            return new ArrayList<>(propNameList);
        }

        final List<String> result = new ArrayList<>(propNameList.size() - propNameToExclude.size());

        for (final String propName : propNameList) {
            if (!propNameToExclude.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }

    /**
     * Retrieves a list of property names for the specified bean, filtered by the given predicate.
     *
     * @param bean the bean whose property names are to be retrieved
     * @param propNameFilter the predicate to filter property names
     * @return a list of property names for the specified bean, filtered by the given predicate
     */
    public static List<String> getPropNames(final Object bean, final Predicate<String> propNameFilter) {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final int size = beanInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            if (propNameFilter.test(propInfo.name)) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     * Retrieves a list of property names for the specified bean, filtered by the given BiPredicate.
     *
     * @param bean the bean whose property names are to be retrieved
     * @param propNameValueFilter the bi-predicate to filter property names and values, where the first parameter is the property name and the second parameter is the property value
     * @return a list of property names for the specified bean, filtered by the given bi-predicate
     */
    public static List<String> getPropNames(final Object bean, final BiPredicate<String, Object> propNameValueFilter) {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final int size = beanInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            if (propNameValueFilter.test(propInfo.name, propInfo.getPropValue(result))) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     * Retrieves the field associated with the specified property name from the given class.
     *
     * @param cls the class from which the field is to be retrieved
     * @param propName the name of the property whose field is to be retrieved
     * @return the field associated with the specified property name
     */
    public static Field getPropField(final Class<?> cls, final String propName) {
        Map<String, Field> propFieldMap = beanPropFieldPool.get(cls);

        if (propFieldMap == null) {
            loadPropGetSetMethodList(cls);
            propFieldMap = beanPropFieldPool.get(cls);
        }

        Field field = propFieldMap.get(propName);

        if (field == null) {
            if (!ClassUtil.isBeanClass(cls)) {
                throw new IllegalArgumentException(
                        "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(cls));
            }

            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = ClassUtil.getPropGetMethods(cls);

                for (final String key : getterMethodList.keySet()) {
                    if (isPropName(cls, propName, key)) {
                        field = propFieldMap.get(key);

                        break;
                    }
                }

                if ((field == null) && !propName.equalsIgnoreCase(formalizePropName(propName))) {
                    field = getPropField(cls, formalizePropName(propName));
                }

                // set method mask to avoid query next time.
                if (field == null) {
                    field = FIELD_MASK;
                }

                //    } else {
                //        ClassUtil.setAccessibleQuietly(field, true);
                //    }

                propFieldMap.put(propName, field);
            }
        }

        return (field == FIELD_MASK) ? null : field;
    }

    /**
     * Retrieves an immutable map of property fields for the specified class.
     *
     * @param cls the class whose property fields are to be retrieved
     * @return an immutable map of property fields for the specified class
     */
    public static ImmutableMap<String, Field> getPropFields(final Class<?> cls) {
        ImmutableMap<String, Field> getterMethodList = beanDeclaredPropFieldPool.get(cls);

        if (getterMethodList == null) {
            loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropFieldPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property get method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     * <p>
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB
     * specification
     *
     * @param cls the class from which the property get method is to be retrieved
     * @param propName the name of the property whose get method is to be retrieved
     * @return the property get method declared in the specified class, or {@code null} if no method is found
     */
    public static Method getPropGetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propGetMethodMap = beanPropGetMethodPool.get(cls);

        if (propGetMethodMap == null) {
            loadPropGetSetMethodList(cls);
            propGetMethodMap = beanPropGetMethodPool.get(cls);
        }

        Method method = propGetMethodMap.get(propName);

        if (method == null) {
            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = getPropGetMethods(cls);

                for (final Map.Entry<String, Method> entry : getterMethodList.entrySet()) { //NOSONAR
                    if (isPropName(cls, propName, entry.getKey())) {
                        method = entry.getValue();

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(formalizePropName(propName))) {
                    method = getPropGetMethod(cls, formalizePropName(propName));
                }

                // set method mask to avoid query next time.
                if (method == null) {
                    method = METHOD_MASK;
                }

                propGetMethodMap.put(propName, method);
            }
        }

        return (method == METHOD_MASK) ? null : method;
    }

    /**
     * Retrieves an immutable map of property get methods for the specified class.
     *
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.
     *
     * @param cls the class from which the property get methods are to be retrieved
     * @return an immutable map of property get methods for the specified class
     */
    public static ImmutableMap<String, Method> getPropGetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> getterMethodList = beanDeclaredPropGetMethodPool.get(cls);

        if (getterMethodList == null) {
            loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropGetMethodPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property set method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     *
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.
     *
     * @param cls the class from which the property set method is to be retrieved
     * @param propName the name of the property whose set method is to be retrieved
     * @return the property set method declared in the specified class, or {@code null} if no method is found
     */
    public static Method getPropSetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propSetMethodMap = beanPropSetMethodPool.get(cls);

        if (propSetMethodMap == null) {
            loadPropGetSetMethodList(cls);
            propSetMethodMap = beanPropSetMethodPool.get(cls);
        }

        Method method = propSetMethodMap.get(propName);

        if (method == null) {
            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> setterMethodList = getPropSetMethods(cls);

                for (final String key : setterMethodList.keySet()) {
                    if (isPropName(cls, propName, key)) {
                        method = propSetMethodMap.get(key);

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(formalizePropName(propName))) {
                    method = getPropSetMethod(cls, formalizePropName(propName));
                }

                // set method mask to avoid query next time.
                if (method == null) {
                    method = METHOD_MASK;
                }

                propSetMethodMap.put(propName, method);
            }
        }

        return (method == METHOD_MASK) ? null : method;
    }

    /**
     * Retrieves an immutable map of property set methods for the specified class.
     *
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.
     *
     * @param cls the class from which the property set methods are to be retrieved
     * @return an immutable map of property set methods for the specified class
     */
    public static ImmutableMap<String, Method> getPropSetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> setterMethodList = beanDeclaredPropSetMethodPool.get(cls);

        if (setterMethodList == null) {
            loadPropGetSetMethodList(cls);
            setterMethodList = beanDeclaredPropSetMethodPool.get(cls);
        }

        return setterMethodList;
    }

    /**
     * Returns the value of the specified property by invoking the given getter method on the provided bean.
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propGetMethod the method to be invoked to get the property value
     * @return the value of the specified property
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPropValue(final Object bean, final Method propGetMethod) {
        try {
            return (T) propGetMethod.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propName the name of the property whose value is to be retrieved
     * @return the value of the specified property
     * @see #getPropValue(Object, Method)
     */
    public static <T> T getPropValue(final Object bean, final String propName) {
        return getPropValue(bean, propName, false);
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     * If the property cannot be found and ignoreUnmatchedProperty is {@code true}, it returns {@code null}.
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propName the name of the property whose value is to be retrieved
     * @param ignoreUnmatchedProperty if {@code true}, ignores unmatched properties and returns null
     * @return the value of the specified property, or {@code null} if the property is not found and ignoreUnmatchedProperty is true
     * @throws IllegalArgumentException if the specified property cannot be retrieved and ignoreUnmatchedProperty is false
     */
    public static <T> T getPropValue(final Object bean, final String propName, final boolean ignoreUnmatchedProperty) {
        final Class<?> cls = bean.getClass();
        final PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return propInfo.getPropValue(bean);
        }
        Map<String, List<Method>> inlinePropGetMethodMap = beanInlinePropGetMethodPool.get(cls);
        List<Method> inlinePropGetMethodQueue = null;

        if (inlinePropGetMethodMap == null) {
            inlinePropGetMethodMap = new ObjectPool<>(ClassUtil.getPropNameList(cls).size());
            beanInlinePropGetMethodPool.put(cls, inlinePropGetMethodMap);
        } else {
            inlinePropGetMethodQueue = inlinePropGetMethodMap.get(propName);
        }

        if (inlinePropGetMethodQueue == null) {
            inlinePropGetMethodQueue = new ArrayList<>();

            final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);

            if (strs.length > 1) {
                Class<?> targetClass = cls;

                for (final String str : strs) {
                    final Method method = getPropGetMethod(targetClass, str);

                    if (method == null) {
                        inlinePropGetMethodQueue.clear();

                        break;
                    }

                    inlinePropGetMethodQueue.add(method);

                    targetClass = method.getReturnType();
                }
            }

            inlinePropGetMethodMap.put(propName, inlinePropGetMethodQueue);
        }

        if (inlinePropGetMethodQueue.size() == 0) {
            if (ignoreUnmatchedProperty) {
                return null;
            }
            throw new IllegalArgumentException(
                    "No property method found with property name: " + propName + " in class " + ClassUtil.getCanonicalClassName(cls));
        }
        final int len = inlinePropGetMethodQueue.size();
        Object propBean = bean;

        for (Method method : inlinePropGetMethodQueue) {
            propBean = ClassUtil.getPropValue(propBean, method);

            if (propBean == null) {
                return (T) N.defaultValueOf(inlinePropGetMethodQueue.get(len - 1).getReturnType());
            }
        }

        return (T) propBean;
    }

    /**
     * Sets the specified property value on the given bean by invoking the provided setter method.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * @param bean the object on which the property value is to be set
     * @param propSetMethod the method to be invoked to set the property value
     * @param propValue the value to be set to the property
     * @return the final value that was set to the property
     * @throws RuntimeException if the underlying method is inaccessible or the method is invoked with incorrect arguments or the underlying method throws an exception
     */
    public static Object setPropValue(final Object bean, final Method propSetMethod, Object propValue) {
        if (propValue == null) {
            propValue = N.defaultValueOf(propSetMethod.getParameterTypes()[0]);

            try {
                propSetMethod.invoke(bean, propValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } else {
            try {
                propSetMethod.invoke(bean, propValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to set value for field by method: {} in class: {} with value type {}", propSetMethod.getName(),
                            propSetMethod.getDeclaringClass().getName(), propValue.getClass().getName());
                }

                propValue = N.convert(propValue, ParserUtil.getBeanInfo(bean.getClass()).getPropInfo(propSetMethod.getName()).jsonXmlType);

                try {
                    propSetMethod.invoke(bean, propValue);
                } catch (IllegalAccessException | InvocationTargetException e2) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        }

        return propValue;
    }

    /**
     * Sets the specified property value on the given bean by invoking the setter method associated with the given property name.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * @param bean the object on which the property value is to be set
     * @param propName the name of the property whose value is to be set
     * @param propValue the value to be set to the property
     * @throws IllegalArgumentException if the specified property cannot be set
     * @deprecated replaced by {@link BeanInfo#setPropValue(Object, String, Object)}
     */
    @Deprecated
    public static void setPropValue(final Object bean, final String propName, final Object propValue) {
        setPropValue(bean, propName, propValue, false);
    }

    /**
     * Sets the specified property value on the given bean by invoking the setter method associated with the given property name.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * @param bean the object on which the property value is to be set
     * @param propName the name of the property whose value is to be set
     * @param propValue the value to be set to the property
     * @param ignoreUnmatchedProperty if {@code true}, ignores unmatched properties and returns false
     * @return {@code true} if the property value has been set, {@code false} otherwise
     * @throws IllegalArgumentException if the specified property cannot be set and ignoreUnmatchedProperty is false
     * @deprecated replaced by {@link BeanInfo#setPropValue(Object, String, Object, boolean)}
     */
    @Deprecated
    public static boolean setPropValue(final Object bean, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {
        //    final Class<?> cls = bean.getClass();
        //    final PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);
        //
        //    if (propInfo != null) {
        //        propInfo.setPropValue(bean, propValue);
        //    } else {
        //        Method getMethod = getPropGetMethod(cls, propName);
        //
        //        if (getMethod == null) {
        //            Map<String, List<Method>> inlinePropSetMethodMap = beanInlinePropSetMethodPool.get(cls);
        //            List<Method> inlinePropSetMethodQueue = null;
        //
        //            if (inlinePropSetMethodMap == null) {
        //                inlinePropSetMethodMap = new ObjectPool<>(ClassUtil.getPropNameList(cls).size());
        //                beanInlinePropSetMethodPool.put(cls, inlinePropSetMethodMap);
        //            } else {
        //                inlinePropSetMethodQueue = inlinePropSetMethodMap.get(propName);
        //            }
        //
        //            if (inlinePropSetMethodQueue == null) {
        //                inlinePropSetMethodQueue = new ArrayList<>();
        //
        //                final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);
        //
        //                if (strs.length > 1) {
        //                    Method setMethod = null;
        //                    Class<?> propClass = cls;
        //
        //                    for (int i = 0, len = strs.length; i < len; i++) {
        //                        if (i == (len - 1)) {
        //                            setMethod = getPropSetMethod(propClass, strs[i]);
        //
        //                            if (setMethod == null) {
        //                                getMethod = getPropGetMethod(propClass, strs[i]);
        //
        //                                if (getMethod == null) {
        //                                    inlinePropSetMethodQueue.clear();
        //
        //                                    break;
        //                                }
        //
        //                                inlinePropSetMethodQueue.add(getMethod);
        //                            } else {
        //                                inlinePropSetMethodQueue.add(setMethod);
        //                            }
        //                        } else {
        //                            getMethod = getPropGetMethod(propClass, strs[i]);
        //
        //                            if (getMethod == null) {
        //                                inlinePropSetMethodQueue.clear();
        //
        //                                break;
        //                            }
        //
        //                            inlinePropSetMethodQueue.add(getMethod);
        //                            propClass = getMethod.getReturnType();
        //                        }
        //                    }
        //                }
        //
        //                inlinePropSetMethodMap.put(propName, N.isEmpty(inlinePropSetMethodQueue) ? N.<Method> emptyList() : inlinePropSetMethodQueue);
        //            }
        //
        //            if (inlinePropSetMethodQueue.size() == 0) {
        //                if (ignoreUnmatchedProperty) {
        //                    return false;
        //                } else {
        //                    throw new IllegalArgumentException("No property method found with property name: " + propName + " in class " + cls.getCanonicalName());
        //                }
        //            } else {
        //                Object propBean = bean;
        //                Method method = null;
        //
        //                for (int i = 0, len = inlinePropSetMethodQueue.size(); i < len; i++) {
        //                    method = inlinePropSetMethodQueue.get(i);
        //
        //                    if (i == (len - 1)) {
        //                        if (N.isEmpty(method.getParameterTypes())) {
        //                            setPropValueByGet(propBean, method, propValue);
        //                        } else {
        //                            setPropValue(propBean, method, propValue);
        //                        }
        //                    } else {
        //                        Object tmp = ClassUtil.getPropValue(propBean, method);
        //
        //                        if (tmp == null) {
        //                            tmp = N.newInstance(method.getReturnType());
        //                            ClassUtil.setPropValue(propBean, ClassUtil.getPropNameByMethod(method), tmp);
        //                        }
        //
        //                        propBean = tmp;
        //                    }
        //                }
        //            }
        //        } else {
        //            setPropValueByGet(bean, getMethod, propValue);
        //        }
        //    }
        //
        //    return true;

        return ParserUtil.getBeanInfo(bean.getClass()).setPropValue(bean, propName, propValue, ignoreUnmatchedProperty);
    }

    /**
     * Sets the property value by invoking the getter method on the provided bean.
     * The returned type of the get method should be {@code Collection} or {@code Map}. And the specified property value and the returned value must be same type.
     *
     * @param bean the object on which the property value is to be set
     * @param propGetMethod the method to be invoked to get the property value
     * @param propValue the value to be set to the property
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPropValueByGet(final Object bean, final Method propGetMethod, final Object propValue) {
        if (propValue == null) {
            return;
        }

        final Object rt = invokeMethod(bean, propGetMethod);

        if (rt instanceof Collection<?> c) {
            c.clear();
            c.addAll((Collection) propValue);
        } else if (rt instanceof Map<?, ?> m) {
            m.clear();
            m.putAll((Map) propValue);
        } else {
            throw new IllegalArgumentException("Failed to set property value by getter method '" + propGetMethod.getName() + "'");
        }
    }

    /**
     * Gets the public static string fields.
     *
     * @param <T>
     * @param cls
     * @return
     */
    private static <T> Map<String, String> getPublicStaticStringFields(final Class<T> cls) {
        final Map<String, String> staticFinalFields = new HashMap<>();

        for (final Field field : cls.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && String.class.equals(field.getType())) {
                String value;

                try {
                    value = (String) field.get(null);
                    staticFinalFields.put(value, value);
                } catch (final Exception e) {
                    // ignore. should never happen
                }
            }
        }

        return staticFinalFields;
    }

    /**
     * Gets the resources.
     *
     * @param pkgName
     * @return
     */
    private static List<URL> getResources(final String pkgName) {
        final List<URL> resourceList = new ArrayList<>();
        final String pkgPath = packageName2FilePath(pkgName);
        final ClassLoader localClassLoader = ClassUtil.class.getClassLoader(); // NOSONAR
        final ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

        try {
            Enumeration<URL> resources = localClassLoader.getResources(pkgPath);

            while (resources != null && resources.hasMoreElements()) {
                resourceList.add(resources.nextElement());
            }

            if (N.isEmpty(resourceList)) {
                resources = sysClassLoader.getResources(pkgPath);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

            if (N.isEmpty(resourceList)) {
                resources = localClassLoader.getResources(pkgName);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

            if (N.isEmpty(resourceList)) {
                resources = sysClassLoader.getResources(pkgName);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Found resources: " + resourceList + " by package name(" + pkgName + ")");
        }

        return resourceList;
    }

    /**
     * Gets the sets the method.
     *
     * @param getMethod
     * @return
     */
    private static Method getSetMethod(final Method getMethod) {
        final Class<?> declaringClass = getMethod.getDeclaringClass();
        final String getMethodName = getMethod.getName();

        final String setMethodName = SET
                + (getMethodName.substring(getMethodName.startsWith(IS) ? 2 : ((getMethodName.startsWith(HAS) || getMethodName.startsWith(GET)) ? 3 : 0)));

        Method setMethod = internalGetDeclaredMethod(declaringClass, setMethodName, getMethod.getReturnType());

        if (setMethod == null && getDeclaredField(declaringClass, getMethodName) != null) {
            setMethod = internalGetDeclaredMethod(declaringClass, getMethodName, getMethod.getReturnType());
        }

        return ((setMethod != null)
                && (void.class.equals(setMethod.getReturnType()) || setMethod.getReturnType().isAssignableFrom(setMethod.getDeclaringClass()))) ? setMethod
                        : null;
    }

    private static final Map<Class<?>, Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>>> builderMap = new ConcurrentHashMap<>();

    /**
     * Retrieves the builder information for the specified class.
     * The builder information includes the class type, a supplier for creating {@code Builder} instances, and a function to build the target object from the {@code Builder}.
     *
     * @param cls the class for which the builder information is to be retrieved
     * @return a tuple containing the class type, a supplier for creating {@code Builder} instances, and a function to build the target object from the {@code Builder}.
     * @throws IllegalArgumentException if the specified class is {@code null} or the builder information cannot be retrieved
     */
    public static Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> getBuilderInfo(
            final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = builderMap
                .get(cls);

        if (builderInfo == null) {
            Method buildMethod = null;
            Class<?> builderClass = null;
            Method builderMethod = getBuilderMethod(cls);

            if (builderMethod == null) {
                //noinspection resource
                builderClass = Stream.of(cls.getDeclaredClasses())
                        .filter(it -> getBuilderMethod(it) != null && getBuildMethod(it, cls) != null)
                        .first()
                        .orElseNull();

                if (builderClass != null) {
                    builderMethod = getBuilderMethod(builderClass);
                }
            }

            if (builderMethod != null) {
                builderClass = builderMethod.getReturnType();
                buildMethod = getBuildMethod(builderClass, cls);

                if (buildMethod != null) {
                    final Method finalBuilderMethod = builderMethod;
                    final Method finalBuildMethod = buildMethod;

                    final com.landawn.abacus.util.function.Supplier<Object> builderSupplier = () -> ClassUtil.invokeMethod(finalBuilderMethod);
                    final com.landawn.abacus.util.function.Function<Object, Object> buildFunc = instance -> ClassUtil.invokeMethod(instance, finalBuildMethod);

                    builderInfo = Tuple.of(builderClass, builderSupplier, buildFunc);

                    builderMap.put(cls, builderInfo);

                    return builderInfo;
                }
            }

            builderInfo = Tuple.of(null, null, null);
            builderMap.put(cls, builderInfo);
        }

        return builderInfo._1 == null ? null : builderInfo;
    }

    private static Method getBuilderMethod(final Class<?> cls) {
        Method builderMethod = null;

        try {
            builderMethod = cls.getDeclaredMethod("builder");
        } catch (final Exception e) {
            // ignore
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("newBuilder");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("createBuilder");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            return null;
        }

        return builderMethod;
    }

    private static Method getBuildMethod(final Class<?> builderClass, final Class<?> beanClass) {
        Method buildMethod = null;

        try {
            buildMethod = builderClass.getDeclaredMethod("build");
        } catch (final Exception e) {
            // ignore
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !beanClass.isAssignableFrom(buildMethod.getReturnType())) {
            try {
                buildMethod = builderClass.getDeclaredMethod("create");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !beanClass.isAssignableFrom(buildMethod.getReturnType())) {
            return null;
        }

        return buildMethod;
    }

    /**
     * Returns the type name of the specified type.
     *
     * @param type the type whose name is to be retrieved
     * @return the name of the specified type
     */
    public static String getTypeName(final java.lang.reflect.Type type) {
        return formatParameterizedTypeName(type.getTypeName());
    }

    /**
     * Gets an {@link Iterator} that can iterate over a class hierarchy in ascending (subclass to superclass) order,
     * excluding interfaces.
     *
     * @param type the type to get the class hierarchy from
     * @return Iterator an Iterator over the class hierarchy of the given class
     */
    public static ObjIterator<Class<?>> hierarchy(final Class<?> type) {
        return hierarchy(type, false);
    }

    /**
     * Gets an {@link Iterator} that can iterate over a class hierarchy in ascending (subclass to superclass) order.
     *
     * @param type the type to get the class hierarchy from
     * @param includeInterface switch indicating whether to include or exclude interfaces
     * @return Iterator an Iterator over the class hierarchy of the given class
     */
    public static ObjIterator<Class<?>> hierarchy(final Class<?> type, final boolean includeInterface) {
        final ObjIterator<Class<?>> superClassesIter = new ObjIterator<>() {
            private final Holder<Class<?>> next = new Holder<>(type);

            @Override
            public boolean hasNext() {
                return next.value() != null;
            }

            @Override
            public Class<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final Class<?> result = next.value();
                next.setValue(result.getSuperclass());
                return result;
            }
        };

        if (!includeInterface) {
            return superClassesIter;
        }

        return new ObjIterator<>() {
            private final Set<Class<?>> seenInterfaces = new HashSet<>();
            private Iterator<Class<?>> interfacesIter = N.emptyIterator();

            @Override
            public boolean hasNext() {
                return interfacesIter.hasNext() || superClassesIter.hasNext();
            }

            @Override
            public Class<?> next() {
                if (interfacesIter.hasNext()) {
                    final Class<?> nextInterface = interfacesIter.next();
                    seenInterfaces.add(nextInterface);
                    return nextInterface;
                }

                final Class<?> nextSuperclass = superClassesIter.next();
                final Set<Class<?>> currentInterfaces = new LinkedHashSet<>();

                walkInterfaces(currentInterfaces, nextSuperclass);

                interfacesIter = currentInterfaces.iterator();

                return nextSuperclass;
            }

            //    @Override
            //    public void remove() {
            //        throw new UnsupportedOperationException();
            //    }

            private void walkInterfaces(final Set<Class<?>> addTo, final Class<?> c) {
                for (final Class<?> cls : c.getInterfaces()) {
                    if (!seenInterfaces.contains(cls)) {
                        addTo.add(cls);
                    }

                    walkInterfaces(addTo, cls);
                }
            }
        };
    }

    /**
     * Internal get declared method.
     *
     * @param cls
     * @param methodName
     * @param parameterTypes
     * @return
     */
    static Method internalGetDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
        Method method = null;

        try {
            method = cls.getDeclaredMethod(methodName, parameterTypes);
        } catch (final NoSuchMethodException e) {
            // ignore.
        }

        if (method == null) {
            final Method[] methods = cls.getDeclaredMethods();

            for (final Method m : methods) {
                if (m.getName().equalsIgnoreCase(methodName) && N.equals(parameterTypes, m.getParameterTypes())) {
                    method = m;

                    break;
                }
            }
        }

        return method;
    }

    /**
     * Invokes the specified constructor with the given arguments.
     *
     * @param <T> the type of the object to be created
     * @param constructor the constructor to be invoked
     * @param args the arguments to be passed to the constructor
     * @return the newly created object
     * @throws RuntimeException if the class that declares the underlying constructor represents an abstract class or the underlying constructor is inaccessible or the underlying constructor throws an exception.
     */
    public static <T> T invokeConstructor(final Constructor<T> constructor, final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Invokes the specified method with the given arguments.
     *
     * @param <T> the type of the object to be returned
     * @param method the method to be invoked
     * @param args the arguments to be passed to the method
     * @return the result of invoking the method
     * @throws RuntimeException if the underlying method is inaccessible or the method is invoked with incorrect arguments or the underlying method throws an exception
     */
    public static <T> T invokeMethod(final Method method, final Object... args) {
        return invokeMethod(null, method, args);
    }

    /**
     * Invokes the specified method on the given instance with the provided arguments.
     *
     * @param <T> the type of the object to be returned
     * @param instance the object on which the method is to be invoked, or {@code null} for static methods
     * @param method the method to be invoked
     * @param args the arguments to be passed to the method
     * @return the result of invoking the method
     * @throws RuntimeException if the underlying method is inaccessible, the method is invoked with incorrect arguments, or the underlying method throws an exception
     */
    public static <T> T invokeMethod(final Object instance, final Method method, final Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    private static final Map<Class<?>, Boolean> beanClassPool = new ObjectPool<>(POOL_SIZE);

    private static boolean annotatedWithEntity(final Class<?> cls) {
        if (cls.getAnnotation(Entity.class) != null) {
            return true;
        }

        final Annotation[] annotations = cls.getAnnotations();

        if (N.notEmpty(annotations)) {
            for (final Annotation annotation : annotations) {
                try {
                    if (annotation.annotationType().equals(javax.persistence.Entity.class)) {
                        return true;
                    }
                } catch (final Throwable e) {
                    // ignore
                }

                try {
                    if (annotation.annotationType().equals(jakarta.persistence.Entity.class)) {
                        return true;
                    }
                } catch (final Throwable e) {
                    // ignore
                }
            }
        }

        return false;
    }

    /**
     * Checks if is field get method.
     *
     * @param method
     * @param field
     * @return {@code true}, if is field get method
     */
    static boolean isFieldGetMethod(final Method method, final Field field) {
        if (!isGetMethod(method) || Object.class.equals(method.getDeclaringClass()) || !method.getReturnType().isAssignableFrom(field.getType())) {
            return false;
        }

        final String fieldName = field.getName();
        final String methodName = method.getName();

        if (fieldName.equals(methodName) && getDeclaredField(method.getDeclaringClass(), fieldName) != null) {
            return true;
        }

        final String propName = methodName
                .substring(methodName.startsWith(IS) ? 2 : ((methodName.startsWith(HAS) || methodName.startsWith(GET) || methodName.startsWith(SET)) ? 3 : 0));

        return propName.equalsIgnoreCase(fieldName) || (fieldName.charAt(0) == '_' && propName.equalsIgnoreCase(fieldName.substring(1)));
    }

    /**
     * Checks if is gets the method.
     *
     * @param method
     * @return {@code true}, if is gets the method
     */
    private static boolean isGetMethod(final Method method) {
        if (Object.class.equals(method.getDeclaringClass())) {
            return false;
        }

        final String mn = method.getName();

        return (mn.startsWith(GET) || mn.startsWith(IS) || mn.startsWith(HAS) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && (N.isEmpty(method.getParameterTypes())) && !void.class.equals(method.getReturnType()) && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Checks if is JAXB get method.
     *
     * @param instance
     * @param method
     * @return {@code true}, if is JAXB get method
     */
    static boolean isJAXBGetMethod(final Class<?> cls, final Object instance, final Method method, final Field field) {
        try {
            return (instance != null)
                    && ((registeredXMLBindingClassList.getOrDefault(cls, false) || N.anyMatch(cls.getAnnotations(), ClassUtil::isXmlTypeAnno))
                            || (N.anyMatch(method.getAnnotations(), ClassUtil::isXmlElementAnno)
                                    || (field != null && N.anyMatch(field.getAnnotations(), ClassUtil::isXmlElementAnno))))
                    && (Collection.class.isAssignableFrom(method.getReturnType()) || Map.class.isAssignableFrom(method.getReturnType()))
                    && (invokeMethod(instance, method) != null);
        } catch (final Exception e) {
            return false;
        }
    }

    private static boolean isXmlTypeAnno(final Annotation it) {
        final String simpleTypeName = it.annotationType().getSimpleName();

        return simpleTypeName.equals("XmlRootElement") || simpleTypeName.equals("XmlType");
    }

    private static boolean isXmlElementAnno(final Annotation it) {
        final String simpleTypeName = it.annotationType().getSimpleName();

        return simpleTypeName.equals("XmlElement") || simpleTypeName.equals("XmlElements");
    }

    /**
     * Checks if is prop name.
     *
     * @param cls
     * @param inputPropName
     * @param propNameByMethod
     * @return {@code true}, if is prop name
     */
    static boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
        if (inputPropName.length() > 128) {
            throw new IllegalArgumentException("The property name exceed 128: " + inputPropName);
        }

        inputPropName = inputPropName.trim();

        return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(WD.UNDERSCORE, Strings.EMPTY).equalsIgnoreCase(propNameByMethod)
                || inputPropName.equalsIgnoreCase(getSimpleClassName(cls) + WD._PERIOD + propNameByMethod)
                || (inputPropName.startsWith(GET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(SET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(IS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(HAS) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod));
    }

    private static boolean isSetMethod(final Method method) {
        final String mn = method.getName();

        return (mn.startsWith(SET) || getDeclaredField(method.getDeclaringClass(), mn) != null) && N.len(method.getParameterTypes()) == 1
                && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(method.getDeclaringClass()))
                && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Load prop get set method list.
     *
     * @param cls
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private static void loadPropGetSetMethodList(final Class<?> cls) {
        synchronized (beanDeclaredPropGetMethodPool) {
            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                return;
            }

            Object instance = null;

            if (!registeredNonBeanClass.containsKey(cls)) {
                try {
                    instance = cls.getDeclaredConstructor().newInstance();
                } catch (final Exception e) {
                    if (logger.isWarnEnabled()) {
                        //noinspection StatementWithEmptyBody
                        if (Strings.isNotEmpty(cls.getPackageName()) && cls.getPackageName().startsWith("java.")) {
                            // ignore
                        } else {
                            logger.warn("Failed to new instance of class: " + cls.getCanonicalName() + " to check setter method by getter method");
                        }
                    }

                    if (registeredXMLBindingClassList.containsKey(cls)) {
                        registeredXMLBindingClassList.put(cls, false);
                    }
                }
            }

            final List<Class<?>> allClasses = new ArrayList<>();
            allClasses.add(cls);
            Class<?> superClass = null;

            while ((superClass = allClasses.get(allClasses.size() - 1).getSuperclass()) != null && !superClass.equals(Object.class)) {
                allClasses.add(superClass);
            }

            final Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = getBuilderInfo(
                    cls);
            final Class<?> builderClass = builderInfo == null ? null : builderInfo._1;

            final Map<String, Field> propFieldMap = new LinkedHashMap<>();
            final Map<String, Method> propGetMethodMap = new LinkedHashMap<>();
            final Map<String, Method> propSetMethodMap = new LinkedHashMap<>();

            Class<?> clazz = null;
            Method setMethod = null;

            final Constructor<?> noArgConstructor = ClassUtil.getDeclaredConstructor(cls);
            Constructor<?> allArgsConstructor = null;
            boolean isImmutable = false;

            for (int i = allClasses.size() - 1; i >= 0; i--) {
                clazz = allClasses.get(i);

                if (registeredNonBeanClass.containsKey(clazz)) {
                    continue;
                }

                final Map<String, String> staticFinalFields = getPublicStaticStringFields(clazz);

                final List<Tuple2<Field, Method>> fieldGetMethodList = new ArrayList<>();

                // sort the methods by the order of declared fields
                for (final Field field : clazz.getDeclaredFields()) {
                    //noinspection resource
                    Stream.of(clazz.getMethods())
                            .filter(method -> isFieldGetMethod(method, field))
                            .sortedBy(method -> method.getName().length())
                            .last()
                            .ifPresentOrElse(method -> fieldGetMethodList.add(Tuple.of(field, method)), () -> {
                                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                                        && !Modifier.isFinal(field.getModifiers())) {
                                    fieldGetMethodList.add(Tuple.of(field, null));
                                }
                            });
                }

                if (noArgConstructor == null && clazz == cls) {
                    final Class<?>[] args = fieldGetMethodList.stream()
                            .filter(it -> it._2 != null)
                            .map(it -> it._1.getType())
                            .toArray(len -> new Class<?>[len]);

                    allArgsConstructor = ClassUtil.getDeclaredConstructor(cls, args);

                    isImmutable = allArgsConstructor != null && Modifier.isPublic(allArgsConstructor.getModifiers());
                }

                String propName = null;

                {
                    Field field = null;
                    Method method = null;

                    // sort the methods by the order of declared fields
                    for (final Tuple2<Field, Method> tp : fieldGetMethodList) {
                        field = tp._1;
                        method = tp._2;

                        if (method != null) {
                            propName = getPropNameByMethod(method);

                            if (!field.equals(getDeclaredField(clazz, propName))) {
                                propName = field.getName();
                            }

                            propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                            if (propGetMethodMap.containsKey(propName)) {
                                continue;
                            }

                            setMethod = getSetMethod(method);

                            if (setMethod != null) {
                                // ClassUtil.setAccessibleQuietly(field, true);
                                ClassUtil.setAccessibleQuietly(method, true);
                                ClassUtil.setAccessibleQuietly(setMethod, true);

                                propFieldMap.put(propName, field);
                                propGetMethodMap.put(propName, method);
                                propSetMethodMap.put(propName, setMethod);

                                continue;
                            }

                            if (isJAXBGetMethod(cls, instance, method, field) || annotatedWithEntity(cls) || isRecordClass(clazz) || builderClass != null
                                    || isImmutable) {
                                // ClassUtil.setAccessibleQuietly(field, true);
                                ClassUtil.setAccessibleQuietly(method, true);

                                propFieldMap.put(propName, field);
                                propGetMethodMap.put(propName, method);

                                //NOSONAR
                            }
                        } else if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                                && !Modifier.isFinal(field.getModifiers())) {
                            propName = field.getName();
                            propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                            if (!propGetMethodMap.containsKey(propName)) {
                                ClassUtil.setAccessibleQuietly(field, true);

                                propFieldMap.put(propName, field);
                            }
                        }
                    }
                }

                for (final Method method : clazz.getMethods()) {
                    if (isGetMethod(method)) {
                        propName = getPropNameByMethod(method);
                        propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                        if (propGetMethodMap.containsKey(propName)) {
                            continue;
                        }

                        setMethod = getSetMethod(method);

                        if (setMethod != null && !propGetMethodMap.containsValue(method)) {
                            ClassUtil.setAccessibleQuietly(method, true);
                            ClassUtil.setAccessibleQuietly(setMethod, true);

                            propGetMethodMap.put(propName, method);
                            propSetMethodMap.put(propName, setMethod);

                            continue;
                        }

                        if ((isJAXBGetMethod(cls, instance, method, null) || annotatedWithEntity(cls) || isRecordClass(clazz))
                                && !propGetMethodMap.containsValue(method)) {
                            ClassUtil.setAccessibleQuietly(method, true);

                            propGetMethodMap.put(propName, method);

                            //NOSONAR
                        }
                    }
                }
            }

            for (final Map.Entry<Class<?>, Set<String>> entry : registeredNonPropGetSetMethodPool.entrySet()) { //NOSONAR
                if (entry.getKey().isAssignableFrom(cls)) {
                    final Set<String> set = entry.getValue();
                    final List<String> methodNames = new ArrayList<>(propGetMethodMap.keySet());

                    for (final String nonPropName : set) {
                        for (final String propName : methodNames) {
                            if (propName.equalsIgnoreCase(nonPropName)) {
                                propFieldMap.remove(propName);
                                propGetMethodMap.remove(propName);
                                propSetMethodMap.remove(propName);

                                break;
                            }
                        }
                    }
                }
            }

            // for Double-Checked Locking is Broke initialize it before
            final ImmutableMap<String, Field> unmodifiableFieldMap = ImmutableMap.wrap(propFieldMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableFieldMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropFieldPool.put(cls, unmodifiableFieldMap);

            // put it into map.
            final Map<String, Field> tempFieldMap = new ObjectPool<>(N.max(64, propFieldMap.size()));
            tempFieldMap.putAll(propFieldMap);
            beanPropFieldPool.put(cls, tempFieldMap);

            final ImmutableMap<String, Method> unmodifiableGetMethodMap = ImmutableMap.wrap(propGetMethodMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableGetMethodMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropGetMethodPool.put(cls, unmodifiableGetMethodMap);

            if (beanPropGetMethodPool.get(cls) == null) {
                final Map<String, Method> tmp = new ObjectPool<>(N.max(64, propGetMethodMap.size()));
                tmp.putAll(propGetMethodMap);
                beanPropGetMethodPool.put(cls, tmp);
            } else {
                beanPropGetMethodPool.get(cls).putAll(propGetMethodMap);
            }

            // for Double-Checked Locking is Broke initialize it before
            // put it into map.
            final ImmutableMap<String, Method> unmodifiableSetMethodMap = ImmutableMap.wrap(propSetMethodMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableSetMethodMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropSetMethodPool.put(cls, unmodifiableSetMethodMap);

            if (beanPropSetMethodPool.get(cls) == null) {
                final Map<String, Method> tmp = new ObjectPool<>(N.max(64, propSetMethodMap.size()));
                tmp.putAll(propSetMethodMap);
                beanPropSetMethodPool.put(cls, tmp);
            } else {
                beanPropSetMethodPool.get(cls).putAll(propSetMethodMap);
            }

            final List<String> propNameList = new ArrayList<>(propFieldMap.keySet());

            for (final String propName : propGetMethodMap.keySet()) {
                if (!propNameList.contains(propName)) {
                    propNameList.add(propName);
                }
            }

            beanDeclaredPropNameListPool.put(cls, ImmutableList.wrap(propNameList));

            if (builderClass != null) {
                String propName = null;

                final Map<String, Method> builderPropSetMethodMap = new LinkedHashMap<>();

                for (final Method method : builderClass.getMethods()) {
                    if (Modifier.isPublic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass()) && method.getParameterCount() == 1
                            && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(builderClass))) {
                        propName = getPropNameByMethod(method);
                        builderPropSetMethodMap.put(propName, method);
                    }

                    final ImmutableMap<String, Method> unmodifiableBuilderPropSetMethodMap = ImmutableMap.wrap(builderPropSetMethodMap);
                    //noinspection ResultOfMethodCallIgnored
                    unmodifiableBuilderPropSetMethodMap.keySet(); // initialize? //NOSONAR
                    beanDeclaredPropSetMethodPool.put(builderClass, unmodifiableBuilderPropSetMethodMap);

                    final Map<String, Method> tmp = new ObjectPool<>(N.max(64, builderPropSetMethodMap.size()));
                    tmp.putAll(builderPropSetMethodMap);
                    beanPropSetMethodPool.put(builderClass, tmp);
                }
            }
        }
    }

    /**
     * Make package folder.
     *
     * @param srcPath
     * @param pkgName
     * @return
     */
    static String makePackageFolder(String srcPath, final String pkgName) {
        srcPath = (srcPath.endsWith("/") || srcPath.endsWith("\\")) ? srcPath : (srcPath + File.separator);

        final String classFilePath = (pkgName == null) ? srcPath : (srcPath + pkgName.replace('.', File.separatorChar) + File.separator);
        final File classFileFolder = new File(classFilePath);

        if (!classFileFolder.exists()) {
            if (classFileFolder.mkdirs()) {
                throw new RuntimeException("Failed to create folder: " + classFileFolder);
            }
        }

        return classFilePath;
    }

    /**
     * Package name 2 file path.
     *
     * @param pkgName
     * @return
     */
    private static String packageName2FilePath(final String pkgName) {
        final String pkgPath = pkgName.replace('.', '/');
        return pkgPath.endsWith("/") ? pkgPath : (pkgPath + "/");
    }

    /**
     * Sets the accessibility flag for the specified {@link AccessibleObject}.
     *
     * @param accessibleObject the object whose accessibility is to be set
     * @param flag the new accessibility flag
     */
    @SuppressWarnings("deprecation")
    public static void setAccessible(final AccessibleObject accessibleObject, final boolean flag) {
        if (accessibleObject != null && accessibleObject.isAccessible() != flag) {
            accessibleObject.setAccessible(flag);
        }
    }

    /**
     * Sets the accessibility flag for the specified {@link AccessibleObject} quietly.
     *
     * @param accessibleObject the object whose accessibility is to be set
     * @param flag the new accessibility flag
     * @return {@code true} if no error happens, otherwise {@code false} is returned
     */
    @SuppressWarnings({ "deprecation", "UnusedReturnValue" })
    public static boolean setAccessibleQuietly(final AccessibleObject accessibleObject, final boolean flag) {
        if (accessibleObject == null) {
            return false;
        }

        if (accessibleObject.isAccessible() == flag) {
            return true;
        }

        try {
            accessibleObject.setAccessible(flag);
        } catch (final Exception e) {
            logger.warn("Failed to set accessible for : " + accessibleObject + " with flag: " + flag + " due to error: " + e.getMessage());
            return false;
        }

        return accessibleObject.isAccessible() == flag;
    }

    //    private static Class[] getTypeArguments(Class cls) {
    //        java.lang.reflect.Type[] typeArgs = null;
    //        java.lang.reflect.Type[] genericInterfaces = cls.getGenericInterfaces();
    //
    //        if (notEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notEmpty(typeArgs)) {
    //                    break;
    //                }
    //            }
    //        } else {
    //            java.lang.reflect.Type genericSuperclass = cls.getGenericSuperclass();
    //
    //            if (genericSuperclass != null) {
    //                typeArgs = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
    //            }
    //        }
    //
    //        if (notEmpty(typeArgs)) {
    //            Class[] classes = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                classes[i] = (Class) typeArgs[i];
    //            }
    //
    //            return classes;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case-insensitive
    //     * @return {@code null} if no method is found by specified name
    //     */
    //    public static Method findDeclaredMethodByName(Class<?> cls, String methodName) {
    //        Method method = null;
    //
    //        Method[] methods = cls.getDeclaredMethods();
    //
    //        for (Method m : methods) {
    //            if (m.getName().equalsIgnoreCase(methodName)) {
    //                if ((method == null) || Modifier.isPublic(m.getModifiers())
    //                        || (Modifier.isProtected(m.getModifiers()) && (!Modifier.isProtected(method.getModifiers())))
    //                        || (!Modifier.isPrivate(m.getModifiers()) && Modifier.isPrivate(method.getModifiers()))) {
    //
    //                    method = m;
    //                }
    //
    //                if (Modifier.isPublic(method.getModifiers())) {
    //                    break;
    //                }
    //            }
    //        }
    //
    //        // SHOULD NOT set it true here.
    //        // if (method != null) {
    //        // ClassUtil.setAccessible(method, true);
    //        // }
    //
    //        return method;
    //    }

    /**
     * Converts the given property name to camel case.
     *
     * @param propName the property name to be converted
     * @return the camel case version of the property name
     */
    public static String toCamelCase(final String propName) {
        String newPropName = camelCasePropNamePool.get(propName);

        if (newPropName == null) {
            newPropName = Strings.toCamelCase(propName);
            newPropName = NameUtil.getCachedName(newPropName);
            camelCasePropNamePool.put(propName, newPropName);
        }

        return newPropName;
    }

    /**
     * Converts the given string to lower case with underscores.
     *
     * @param str the string to be converted
     * @return the lower case version of the string with underscores
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = lowerCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = Strings.toLowerCaseWithUnderscore(str);
            lowerCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts the given string to upper case with underscores.
     *
     * @param str the string to be converted
     * @return the upper case version of the string with underscores
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = upperCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = Strings.toUpperCaseWithUnderscore(str);
            upperCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts the keys in the provided map to camel case.
     *
     * @param props the map whose keys are to be converted to camel case
     */
    @SuppressWarnings("deprecation")
    public static void toCamelCase(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(ClassUtil.toCamelCase(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * Converts the keys in the provided map to lower case with underscores.
     *
     * @param props the map whose keys are to be converted to lower case with underscores
     */
    @SuppressWarnings("deprecation")
    public static void toLowerCaseWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(ClassUtil.toLowerCaseWithUnderscore(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * Converts the keys in the provided map to upper case with underscores.
     *
     * @param props the map whose keys are to be converted to upper case with underscores
     */
    @SuppressWarnings("deprecation")
    public static void toUpperCaseWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(ClassUtil.toUpperCaseWithUnderscore(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    private static final Class<?> recordClass;

    static {
        Class<?> cls = null;

        try {
            cls = Class.forName("java.lang.Record");
        } catch (final ClassNotFoundException e) {
            // ignore.
        }

        recordClass = cls;
    }

    private static final Map<Class<?>, Boolean> recordClassPool = new ObjectPool<>(POOL_SIZE);

    /**
     * Checks if the specified class is a bean class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a bean class, {@code false} otherwise
     */
    public static boolean isBeanClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        Boolean ret = beanClassPool.get(cls);

        if (ret == null) {
            ret = annotatedWithEntity(cls) || isRecordClass(cls) || (!Number.class.isAssignableFrom(cls) && N.notEmpty(ClassUtil.getPropNameList(cls)));
            beanClassPool.put(cls, ret);
        }

        return ret;
    }

    /**
     * Checks if the specified class is a record class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a record class, {@code false} otherwise
     */
    public static boolean isRecordClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        return recordClassPool.computeIfAbsent(cls, k -> (recordClass != null && recordClass.isAssignableFrom(cls)) || cls.getAnnotation(Record.class) != null);
    }

    private static final Map<Class<?>, Boolean> anonymousClassMap = new ConcurrentHashMap<>();

    /**
     * Checks if the specified class is an anonymous class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is an anonymous class, {@code false} otherwise
     */
    public static boolean isAnonymousClass(final Class<?> cls) {

        return anonymousClassMap.computeIfAbsent(cls, k -> cls.isAnonymousClass());
    }

    private static final Map<Class<?>, Boolean> memberClassMap = new ConcurrentHashMap<>();

    /**
     * Checks if the specified class is a member class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a member class, {@code false} otherwise
     */
    public static boolean isMemberClass(final Class<?> cls) {

        return memberClassMap.computeIfAbsent(cls, k -> cls.isMemberClass());
    }

    /**
     * Checks if the specified class is either an anonymous class or a member class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is either an anonymous class or a member class, {@code false} otherwise
     */
    public static boolean isAnonymousOrMemberClass(final Class<?> cls) {
        Boolean v = anonymousClassMap.computeIfAbsent(cls, k -> cls.isAnonymousClass());

        if (!v) {
            v = memberClassMap.computeIfAbsent(cls, k -> cls.isMemberClass());

        }

        return v;
    }

    private ClassUtil() {
        // singleton
    }

    /**
     * Checks if the specified class is a primitive type.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a primitive type, {@code false} otherwise
     * @throws IllegalArgumentException if the class is {@code null}
     */
    public static boolean isPrimitiveType(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        return N.typeOf(cls).isPrimitiveType();
    }

    /**
     * Checks if the specified class is a primitive wrapper type.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a primitive wrapper type, {@code false} otherwise
     * @throws IllegalArgumentException if the class is {@code null}
     */
    public static boolean isPrimitiveWrapper(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        return N.typeOf(cls).isPrimitiveWrapper();
    }

    /**
     * Checks if the specified class is a primitive array type.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a primitive array type, {@code false} otherwise
     * @throws IllegalArgumentException if the class is {@code null}
     */
    public static boolean isPrimitiveArrayType(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        return N.typeOf(cls).isPrimitiveArray();
    }

    // ...
    static final BiMap<Class<?>, Class<?>> PRIMITIVE_2_WRAPPER = new BiMap<>();

    static {
        PRIMITIVE_2_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_2_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_2_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_2_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_2_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_2_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_2_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_2_WRAPPER.put(double.class, Double.class);

        PRIMITIVE_2_WRAPPER.put(boolean[].class, Boolean[].class);
        PRIMITIVE_2_WRAPPER.put(char[].class, Character[].class);
        PRIMITIVE_2_WRAPPER.put(byte[].class, Byte[].class);
        PRIMITIVE_2_WRAPPER.put(short[].class, Short[].class);
        PRIMITIVE_2_WRAPPER.put(int[].class, Integer[].class);
        PRIMITIVE_2_WRAPPER.put(long[].class, Long[].class);
        PRIMITIVE_2_WRAPPER.put(float[].class, Float[].class);
        PRIMITIVE_2_WRAPPER.put(double[].class, Double[].class);
    }

    /**
     * Returns the corresponding wrapper type of {@code cls} if it is a primitive type; otherwise returns {@code cls} itself.
     *
     * <pre>
     *     wrap(int.class) == Integer.class
     *     wrap(Integer.class) == Integer.class
     *     wrap(String.class) == String.class
     * </pre>
     *
     * @param cls the class to be wrapped
     * @return the corresponding wrapper type if {@code cls} is a primitive type, otherwise {@code cls} itself
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     */
    public static Class<?> wrap(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        final Class<?> wrapped = PRIMITIVE_2_WRAPPER.get(cls);

        return wrapped == null ? cls : wrapped;
    }

    /**
     * Returns the corresponding primitive type of {@code cls} if it is a wrapper type; otherwise returns {@code cls} itself.
     *
     * <pre>
     *     unwrap(Integer.class) == int.class
     *     unwrap(int.class) == int.class
     *     unwrap(String.class) == String.class
     * </pre>
     *
     * @param cls the class to be unwrapped
     * @return the corresponding primitive type if {@code cls} is a wrapper type, otherwise {@code cls} itself
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     */
    public static Class<?> unwrap(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        final Class<?> unwrapped = PRIMITIVE_2_WRAPPER.getByValue(cls);

        return unwrapped == null ? cls : unwrapped;
    }

    static boolean isPossibleImmutable(final Class<?> cls) {
        return Strings.containsAnyIgnoreCase(ClassUtil.getSimpleClassName(cls), "Immutable", " Unmodifiable") //
                || ClassUtil.getAllSuperclasses(cls)
                        .stream()
                        .anyMatch(c -> Strings.containsAnyIgnoreCase(ClassUtil.getSimpleClassName(c), "Immutable", " Unmodifiable"));
    }

    /**
     * Creates and returns a new instance of the <i>None</i> class, which serves as a {@code null} mask.
     *
     * @return a new instance of the <i>None</i> class
     */
    public static Object createNullMask() {
        return new None();
    }

    static class None {
        // private static final int HASH_CODE = -2147483629; // is a prime.

        private None() {
        }

        @Override
        public int hashCode() {
            // return HASH_CODE;
            return System.identityHashCode(this);
        }

        @SuppressWarnings("RedundantMethodOverride")
        @Override
        public boolean equals(final Object obj) {
            return obj == this;
        }

        @Override
        public String toString() {
            return "NULL";
        }
    }
}
