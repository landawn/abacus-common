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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
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
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
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

/**
 * A comprehensive utility class for Java reflection operations and class manipulation.
 * This class provides extensive functionality for working with Java classes, methods, fields,
 * and properties at runtime. It includes support for bean introspection, property access,
 * type conversion, and various reflection-based operations.
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Dynamic class loading and instantiation</li>
 *   <li>Property getter/setter method discovery and invocation</li>
 *   <li>Field access and manipulation</li>
 *   <li>Type conversion and wrapping/unwrapping of primitive types</li>
 *   <li>Bean property introspection with caching for performance</li>
 *   <li>Support for XML binding classes and JAXB annotations</li>
 *   <li>Package scanning and class discovery</li>
 *   <li>Method handle creation for improved performance</li>
 * </ul>
 * 
 * <p>This class maintains internal caches for frequently accessed metadata to improve
 * performance in reflection-heavy applications.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Load a class dynamically
 * Class<?> clazz = ClassUtil.forClass("com.example.MyClass");
 * 
 * // Get property value
 * Object bean = new MyBean();
 * String name = Beans.getPropValue(bean, "name");
 * 
 * // Set property value
 * Beans.setPropValue(bean, "name", "John");
 * 
 * // Get all property names
 * List<String> propNames = Beans.getPropNameList(MyBean.class);
 * }</pre>
 * 
 * @since 0.8
 */
@SuppressWarnings({ "java:S1942" })
public final class ClassUtil {

    private ClassUtil() {
        // singleton
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
    public static final Method METHOD_MASK = ClassUtil.lookupDeclaredMethod(ClassMask.class, "methodMask");

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

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private static final String JAR_POSTFIX = ".jar";

    private static final String CLASS_POSTFIX = ".class";

    // ... it has to be big enough to make it's safety to add an element to
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
        BUILT_IN_TYPE.put(URI.class.getCanonicalName(), URI.class);

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

        BUILT_IN_TYPE.put(Fraction.class.getCanonicalName(), Fraction.class);
        BUILT_IN_TYPE.put(Range.class.getCanonicalName(), Range.class);
        BUILT_IN_TYPE.put(Duration.class.getCanonicalName(), Duration.class);

        BUILT_IN_TYPE.put(BiMap.class.getCanonicalName(), BiMap.class);
        BUILT_IN_TYPE.put(ListMultimap.class.getCanonicalName(), ListMultimap.class);
        BUILT_IN_TYPE.put(SetMultimap.class.getCanonicalName(), SetMultimap.class);
        BUILT_IN_TYPE.put(Multimap.class.getCanonicalName(), Multimap.class);
        BUILT_IN_TYPE.put(Multiset.class.getCanonicalName(), Multiset.class);
        BUILT_IN_TYPE.put(HBaseColumn.class.getCanonicalName(), HBaseColumn.class);

        BUILT_IN_TYPE.put(ImmutableList.class.getCanonicalName(), ImmutableList.class);
        BUILT_IN_TYPE.put(ImmutableSet.class.getCanonicalName(), ImmutableSet.class);
        BUILT_IN_TYPE.put(ImmutableMap.class.getCanonicalName(), ImmutableMap.class);

        BUILT_IN_TYPE.put(Type.class.getCanonicalName(), Type.class);
        BUILT_IN_TYPE.put(Dataset.class.getCanonicalName(), Dataset.class);
        BUILT_IN_TYPE.put(RowDataset.class.getCanonicalName(), RowDataset.class);
        BUILT_IN_TYPE.put(Sheet.class.getCanonicalName(), Sheet.class);

        BUILT_IN_TYPE.put(Map.Entry.class.getCanonicalName(), Map.Entry.class);
        BUILT_IN_TYPE.put("java.util.Map.Entry", Map.Entry.class);
        BUILT_IN_TYPE.put("Map.Entry", Map.Entry.class);

        BUILT_IN_TYPE.put(java.time.Duration.class.getCanonicalName(), java.time.Duration.class);
        BUILT_IN_TYPE.put(Instant.class.getCanonicalName(), Instant.class);
        BUILT_IN_TYPE.put(LocalDate.class.getCanonicalName(), LocalDate.class);
        BUILT_IN_TYPE.put(LocalDateTime.class.getCanonicalName(), LocalDateTime.class);
        BUILT_IN_TYPE.put(LocalTime.class.getCanonicalName(), LocalTime.class);
        BUILT_IN_TYPE.put(OffsetDateTime.class.getCanonicalName(), OffsetDateTime.class);
        BUILT_IN_TYPE.put(OffsetTime.class.getCanonicalName(), OffsetTime.class);
        BUILT_IN_TYPE.put(ZonedDateTime.class.getCanonicalName(), ZonedDateTime.class);
        BUILT_IN_TYPE.put(Year.class.getCanonicalName(), Year.class);
        BUILT_IN_TYPE.put(YearMonth.class.getCanonicalName(), YearMonth.class);

        BUILT_IN_TYPE.put(java.util.Optional.class.getCanonicalName(), java.util.Optional.class);
        BUILT_IN_TYPE.put(java.util.OptionalInt.class.getCanonicalName(), java.util.OptionalInt.class);
        BUILT_IN_TYPE.put(java.util.OptionalLong.class.getCanonicalName(), java.util.OptionalLong.class);
        BUILT_IN_TYPE.put(java.util.OptionalDouble.class.getCanonicalName(), java.util.OptionalDouble.class);

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
            if (cls.getCanonicalName().startsWith("java.util.Date") || cls.getCanonicalName().startsWith("java.time.Duration")
                    || cls.getCanonicalName().startsWith("java.util.Optional")) {
                continue;
            }

            BUILT_IN_TYPE.put(cls.getSimpleName(), cls);
        }

        BUILT_IN_TYPE.put("JUDate", java.util.Date.class);

        BUILT_IN_TYPE.put("JdkDuration", java.time.Duration.class);

        BUILT_IN_TYPE.put("JdkOptional", java.util.Optional.class);
        BUILT_IN_TYPE.put("JdkOptionalInt", java.util.OptionalInt.class);
        BUILT_IN_TYPE.put("JdkOptionalLong", java.util.OptionalLong.class);
        BUILT_IN_TYPE.put("JdkOptionalDouble", java.util.OptionalDouble.class);

        //
        // N.println("#########################################Builtin types================================");
        // N.println("size = " + BUILT_IN_TYPE.size());
        //
        // for (Map.Entry<String, Class<?>> entry : BUILT_IN_TYPE.entrySet()) {
        // N.println(entry.getKey() + " = " + entry.getValue());
        // }
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

    // Superclasses/Superinterfaces. Copied from Apache Commons Lang under Apache License v2.
    // ----------------------------------------------------------------------

    /**
     * Returns the Class object associated with the class or interface with the given string name.
     * This method supports primitive types (boolean, char, byte, short, int, long, float, double)
     * and array types with format {@code java.lang.String[]}.
     * 
     * <p>The method also handles:</p>
     * <ul>
     *   <li>Fully qualified class names</li>
     *   <li>Simple class names (attempts to load from java.lang package)</li>
     *   <li>Array notation (e.g., "String[]", "int[][]")</li>
     *   <li>Inner class notation (with $ separator)</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Class<?> strClass = ClassUtil.forClass("java.lang.String");
     * Class<?> intClass = ClassUtil.forClass("int");
     * Class<?> arrClass = ClassUtil.forClass("String[]");
     * }</pre>
     *
     * @param <T> the type of the class
     * @param clsName the fully qualified name of the desired class
     * @return the Class object for the class with the specified name
     * @throws IllegalArgumentException if the class cannot be located
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
    private static <T> Class<T> forClass(final String clsName, final boolean cacheResult) throws IllegalArgumentException {
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

    // Superclasses/Superinterfaces. Copied from Apache Commons Lang under Apache License v2.
    // ----------------------------------------------------------------------

    /**
     * Returns the formatted type name of the specified type.
     * This method retrieves the type name and formats it using {@link #formatParameterizedTypeName(String)}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Type type = MyClass.class.getGenericSuperclass();
     * String typeName = ClassUtil.getTypeName(type);
     * }</pre>
     *
     * @param type the type whose name is to be retrieved
     * @return the formatted name of the specified type
     */
    public static String getTypeName(final java.lang.reflect.Type type) {
        return formatParameterizedTypeName(type.getTypeName());
    }

    /**
     * Retrieves the canonical name of the specified class.
     * If the canonical name is not available (e.g., for anonymous classes), it returns the class name instead.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String name = ClassUtil.getCanonicalClassName(String.class); // Returns "java.lang.String"
     * }</pre>
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
     * Retrieves the fully qualified name of the specified class.
     * This method returns the name as defined by {@link Class#getName()}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String name = ClassUtil.getClassName(String.class); // Returns "java.lang.String"
     * }</pre>
     *
     * @param cls the class whose name is to be retrieved
     * @return the fully qualified name of the class
     */
    public static String getClassName(final Class<?> cls) {

        return nameClassPool.computeIfAbsent(cls, k -> cls.getName());
    }

    //    /**

    /**
     * Retrieves the simple name of the specified class as returned by {@link Class#getSimpleName()}.
     * The simple name is the name of the class without the package prefix.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * String name = ClassUtil.getSimpleClassName(String.class); // Returns "String"
     * }</pre>
     *
     * @param cls the class whose simple name is to be retrieved
     * @return the simple name of the class
     */
    public static String getSimpleClassName(final Class<?> cls) {

        return simpleClassNamePool.computeIfAbsent(cls, k -> cls.getSimpleName());
    }

    /**
     * Retrieves the package of the specified class.
     * Returns {@code null} if the class is a primitive type or if no package is defined.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Package pkg = ClassUtil.getPackage(String.class); // Returns java.lang package
     * }</pre>
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
     * <p>Example usage:</p>
     * <pre>{@code
     * String pkgName = ClassUtil.getPackageName(String.class); // Returns "java.lang"
     * }</pre>
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
     * This method scans the classpath to discover classes within the given package.
     *
     * <p><b>Note:</b> This method does not work for JDK packages (e.g., java.lang, java.util).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<Class<?>> classes = ClassUtil.getClassesByPackage("com.example.myapp", true, false);
     * }</pre>
     *
     * @param pkgName the name of the package to search for classes
     * @param isRecursive if {@code true}, searches recursively in sub-packages
     * @param skipClassLoadingException if {@code true}, skips classes that cannot be loaded and continues scanning
     * @return a list of classes in the specified package
     * @throws IllegalArgumentException if no resources are found for the specified package (e.g., package does not exist or JDK packages)
     * @throws UncheckedIOException if an I/O error occurs during package scanning
     */
    public static List<Class<?>> getClassesByPackage(final String pkgName, final boolean isRecursive, final boolean skipClassLoadingException)
            throws IllegalArgumentException, UncheckedIOException {
        return getClassesByPackage(pkgName, isRecursive, skipClassLoadingException, Fn.alwaysTrue());
    }

    /**
     * Retrieves a filtered list of classes in the specified package.
     * This method scans the classpath to discover classes within the given package and applies the specified predicate filter.
     *
     * <p><b>Note:</b> This method does not work for JDK packages (e.g., java.lang, java.util).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<Class<?>> interfaces = ClassUtil.getClassesByPackage(
     *     "com.example.myapp",
     *     true,
     *     false,
     *     cls -> cls.isInterface()
     * );
     * }</pre>
     *
     * @param pkgName the name of the package to search for classes
     * @param isRecursive if {@code true}, searches recursively in sub-packages
     * @param skipClassLoadingException if {@code true}, skips classes that cannot be loaded and continues scanning
     * @param predicate a predicate to filter the classes; only classes for which the predicate returns {@code true} are included
     * @return a list of classes in the specified package that match the predicate
     * @throws IllegalArgumentException if no resources are found for the specified package (e.g., package does not exist or JDK packages)
     * @throws UncheckedIOException if an I/O error occurs during package scanning
     */
    public static List<Class<?>> getClassesByPackage(final String pkgName, final boolean isRecursive, final boolean skipClassLoadingException,
            final Predicate<? super Class<?>> predicate) throws IllegalArgumentException, UncheckedIOException {
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

    //    /**

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
     * File path 2 package name.
     *
     * @param entryName
     * @return
     */
    private static String filePath2PackageName(final String entryName) {
        final String pkgName = entryName.replace('/', '.').replace('\\', '.');
        return pkgName.endsWith(".") ? pkgName.substring(0, pkgName.length() - 1) : pkgName;
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
     * Gets a set of all interfaces implemented by the given class and its superclasses.
     * Copied from Apache Commons Lang under Apache License v2.
     *
     * <p>The order is determined by looking through each interface in turn as
     * declared in the source file and following its hierarchy up. Then each
     * superclass is considered in the same way. Later duplicates are ignored,
     * so the order is maintained.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Set<Class<?>> interfaces = ClassUtil.getAllInterfaces(ArrayList.class);
     * // Returns: List, Collection, Iterable, RandomAccess, Cloneable, Serializable
     * }</pre>
     *
     * @param cls the class to look up
     * @return a set of all interfaces implemented by the class and its superclasses
     */
    public static Set<Class<?>> getAllInterfaces(final Class<?> cls) {
        final Set<Class<?>> interfacesFound = N.newLinkedHashSet();

        getAllInterfaces(cls, interfacesFound);

        return interfacesFound;
    }

    /**
     * Gets a list of all superclasses for the given class, excluding {@code Object.class}.
     * Copied from Apache Commons Lang under Apache License v2.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * List<Class<?>> superclasses = ClassUtil.getAllSuperclasses(ArrayList.class);
     * // Returns: AbstractList, AbstractCollection
     * }</pre>
     *
     * @param cls the class to look up
     * @return a list of all superclasses, excluding {@code Object.class}
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
     * Returns all interfaces and superclasses that the specified class implements or extends, excluding {@code Object.class}.
     * This combines the results of {@link #getAllInterfaces(Class)} and {@link #getAllSuperclasses(Class)}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(ArrayList.class);
     * // Returns: List, Collection, Iterable, RandomAccess, Cloneable, Serializable, AbstractList, AbstractCollection
     * }</pre>
     *
     * @param cls the class to look up
     * @return a set of all interfaces and superclasses, excluding {@code Object.class}
     */
    public static Set<Class<?>> getAllSuperTypes(final Class<?> cls) {
        final Set<Class<?>> superTypesFound = N.newLinkedHashSet();

        getAllSuperTypes(cls, superTypesFound);

        return superTypesFound;
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
     * Retrieves the enclosing class of the specified class.
     * Returns {@code null} if the class is not an inner class or has no enclosing class.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * class Outer {
     *     class Inner { }
     * }
     * Class<?> enclosing = ClassUtil.getEnclosingClass(Outer.Inner.class); // Returns Outer.class
     * }</pre>
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
     * Returns the constructor declared in the specified class with the given parameter types.
     * Returns {@code null} if no constructor is found.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Constructor<String> ctor = ClassUtil.getDeclaredConstructor(String.class, char[].class);
     * String str = ctor.newInstance(new char[]{'a', 'b', 'c'});
     * }</pre>
     *
     * @param <T> the type of the class
     * @param cls the class object
     * @param parameterTypes the parameter types of the constructor
     * @return the constructor declared in the specified class with the specified parameter types, or {@code null} if no constructor is found
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
     * Returns the method declared in the specified class with the given method name and parameter types.
     * Returns {@code null} if no method is found with the specified name and parameter types.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Method method = ClassUtil.getDeclaredMethod(String.class, "substring", int.class, int.class);
     * String result = (String) method.invoke("Hello", 0, 2); // Returns "He"
     * }</pre>
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
                method = lookupDeclaredMethod(cls, methodName, parameterTypes);

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
                method = lookupDeclaredMethod(cls, methodName, parameterTypes);

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
     * Gets the parameterized type name of the specified field, including generic type information.
     * This method attempts to resolve the field's generic type if available.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * class MyClass {
     *     List<String> names;
     * }
     * Field field = MyClass.class.getDeclaredField("names");
     * String typeName = ClassUtil.getParameterizedTypeNameByField(field);
     * // Returns "List<String>"
     * }</pre>
     *
     * @param field the field whose parameterized type name is to be retrieved
     * @return the parameterized type name of the field, including generic type information if available
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
     * Gets the parameterized type name of the specified method, including generic type information.
     * This method examines the method's first parameter type (if present) or the return type to extract generic type information.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * class MyClass {
     *     void setNames(List<String> names) { }
     *     List<Integer> getNumbers() { return null; }
     * }
     * Method setter = MyClass.class.getDeclaredMethod("setNames", List.class);
     * String typeName = ClassUtil.getParameterizedTypeNameByMethod(setter);
     * // Returns "List<String>"
     * }</pre>
     *
     * @param method the method whose parameterized type name is to be retrieved
     * @return the parameterized type name of the method's parameter or return type, including generic type information if available
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

    // Superclasses/Superinterfaces. Copied from Apache Commons Lang under Apache License v2.
    // ----------------------------------------------------------------------

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
     * Returns the number of inheritance hops between two classes.
     * This method calculates the distance in the inheritance hierarchy from
     * a child class to a parent class.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * int distance = ClassUtil.distanceOfInheritance(ArrayList.class, List.class);
     * // Returns 1 (ArrayList directly implements List)
     * 
     * distance = ClassUtil.distanceOfInheritance(String.class, Object.class);
     * // Returns 1 (String directly extends Object)
     * }</pre>
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
     * Gets an iterator that can iterate over a class hierarchy in ascending (subclass to superclass) order,
     * excluding interfaces.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ObjIterator<Class<?>> iter = ClassUtil.hierarchy(ArrayList.class);
     * while (iter.hasNext()) {
     *     System.out.println(iter.next()); // Prints: ArrayList, AbstractList, AbstractCollection, Object
     * }
     * }</pre>
     *
     * @param type the type to get the class hierarchy from
     * @return an iterator over the class hierarchy of the given class, excluding interfaces
     */
    public static ObjIterator<Class<?>> hierarchy(final Class<?> type) {
        return hierarchy(type, false);
    }

    /**
     * Gets an iterator that can iterate over a class hierarchy in ascending (subclass to superclass) order.
     * Optionally includes interfaces implemented by the class and its superclasses.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ObjIterator<Class<?>> iter = ClassUtil.hierarchy(ArrayList.class, true);
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     *     // Prints: ArrayList, List, Collection, Iterable, RandomAccess, Cloneable, Serializable,
     *     //         AbstractList, AbstractCollection, Object
     * }
     * }</pre>
     *
     * @param type the type to get the class hierarchy from
     * @param includeInterface if {@code true}, includes interfaces; if {@code false}, excludes interfaces
     * @return an iterator over the class hierarchy of the given class
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

    static Method lookupDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
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
     * Invokes the specified constructor with the given arguments and returns the newly created instance.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Constructor<String> ctor = ClassUtil.getDeclaredConstructor(String.class, char[].class);
     * String str = ClassUtil.invokeConstructor(ctor, new char[]{'a', 'b', 'c'});
     * }</pre>
     *
     * @param <T> the type of the object to be created
     * @param constructor the constructor to be invoked
     * @param args the arguments to be passed to the constructor
     * @return the newly created object
     * @throws RuntimeException if the class that declares the underlying constructor represents an abstract class,
     *         or the underlying constructor is inaccessible, or the underlying constructor throws an exception
     */
    public static <T> T invokeConstructor(final Constructor<T> constructor, final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Invokes the specified static method with the given arguments.
     * This is a convenience method for invoking static methods.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Method method = Integer.class.getDeclaredMethod("parseInt", String.class);
     * int result = ClassUtil.invokeMethod(method, "123"); // Returns 123
     * }</pre>
     *
     * @param <T> the type of the object to be returned
     * @param method the static method to be invoked
     * @param args the arguments to be passed to the method
     * @return the result of invoking the method
     * @throws RuntimeException if the underlying method is inaccessible, the method is invoked with incorrect arguments,
     *         or the underlying method throws an exception
     */
    public static <T> T invokeMethod(final Method method, final Object... args) {
        return invokeMethod(null, method, args);
    }

    /**
     * Invokes the specified method on the given instance with the provided arguments.
     * For static methods, pass {@code null} as the instance parameter.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Method method = String.class.getDeclaredMethod("substring", int.class, int.class);
     * String result = ClassUtil.invokeMethod("Hello World", method, 0, 5);
     * // Returns "Hello"
     * }</pre>
     *
     * @param <T> the type of the object to be returned
     * @param instance the object on which the method is to be invoked, or {@code null} for static methods
     * @param method the method to be invoked
     * @param args the arguments to be passed to the method
     * @return the result of invoking the method
     * @throws RuntimeException if the underlying method is inaccessible, the method is invoked with incorrect arguments,
     *         or the underlying method throws an exception
     */
    public static <T> T invokeMethod(final Object instance, final Method method, final Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Make package folder.
     *
     * @param srcPath
     * @param pkgName
     * @return
     */
    static String makeFolderForPackage(String srcPath, final String pkgName) {
        srcPath = (srcPath.endsWith("/") || srcPath.endsWith("\\")) ? srcPath : (srcPath + File.separator);

        final String classFilePath = (pkgName == null) ? srcPath : (srcPath + pkgName.replace('.', File.separatorChar) + File.separator);
        final File classFileFolder = new File(classFilePath);

        if (!classFileFolder.exists()) {
            if (!classFileFolder.mkdirs()) {
                throw new RuntimeException("Failed to create folder: " + classFileFolder);
            }
        }

        return classFilePath;
    }

    /**
     * Sets the accessibility flag for the specified {@link AccessibleObject}.
     * This method is typically used to enable access to private fields, methods, or constructors.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Field field = MyClass.class.getDeclaredField("privateField");
     * ClassUtil.setAccessible(field, true);
     * Object value = field.get(instance); // Can now access private field
     * }</pre>
     *
     * @param accessibleObject the object whose accessibility is to be set
     * @param flag the new accessibility flag ({@code true} to make accessible, {@code false} otherwise)
     */
    @SuppressWarnings("deprecation")
    public static void setAccessible(final AccessibleObject accessibleObject, final boolean flag) {
        if (accessibleObject != null && accessibleObject.isAccessible() != flag) {
            accessibleObject.setAccessible(flag);
        }
    }

    /**
     * Sets the accessibility flag for the specified {@link AccessibleObject} quietly, suppressing any exceptions.
     * This method attempts to set the accessibility but catches and logs any exceptions that occur.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Field field = MyClass.class.getDeclaredField("privateField");
     * boolean success = ClassUtil.setAccessibleQuietly(field, true);
     * if (success) {
     *     Object value = field.get(instance);
     * }
     * }</pre>
     *
     * @param accessibleObject the object whose accessibility is to be set
     * @param flag the new accessibility flag ({@code true} to make accessible, {@code false} otherwise)
     * @return {@code true} if the accessibility was successfully set, {@code false} if an error occurred or the object is {@code null}
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

    //    /**

    static boolean isPossibleImmutable(final Class<?> cls) {
        return Strings.containsAnyIgnoreCase(getSimpleClassName(cls), "Immutable", "Unmodifiable") //
                || getAllSuperclasses(cls).stream().anyMatch(c -> Strings.containsAnyIgnoreCase(getSimpleClassName(c), "Immutable", "Unmodifiable"));
    }

    /**
     * Checks if the specified class is a bean class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a bean class, {@code false} otherwise
     * @deprecated Use {@link Beans#isBeanClass(Class<?>)} instead
     */
    @Deprecated
    public static boolean isBeanClass(final Class<?> cls) {
        return Beans.isBeanClass(cls);
    }

    /**
     * Checks if the specified class is a record class.
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a record class, {@code false} otherwise
     * @deprecated Use {@link Beans#isRecordClass(Class<?>)} instead
     */
    @Deprecated
    public static boolean isRecordClass(final Class<?> cls) {
        return Beans.isRecordClass(cls);
    }

    private static final Map<Class<?>, Boolean> anonymousClassMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Boolean> memberClassMap = new ConcurrentHashMap<>();

    /**
     * Checks if the specified class is an anonymous class.
     * An anonymous class is a local class without a name that is defined and instantiated in a single expression.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Runnable r = new Runnable() {
     *     public void run() { }
     * };
     * boolean isAnon = ClassUtil.isAnonymousClass(r.getClass()); // Returns true
     * }</pre>
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is an anonymous class, {@code false} otherwise
     */
    public static boolean isAnonymousClass(final Class<?> cls) {

        return anonymousClassMap.computeIfAbsent(cls, k -> cls.isAnonymousClass());
    }

    /**
     * Checks if the specified class is a member class.
     * A member class is a non-static nested class that is directly enclosed within another class.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * class Outer {
     *     class Inner { }
     * }
     * boolean isMember = ClassUtil.isMemberClass(Outer.Inner.class); // Returns true
     * }</pre>
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a member class, {@code false} otherwise
     */
    public static boolean isMemberClass(final Class<?> cls) {

        return memberClassMap.computeIfAbsent(cls, k -> cls.isMemberClass());
    }

    /**
     * Checks if the specified class is either an anonymous class or a member class.
     * This is a convenience method that combines {@link #isAnonymousClass(Class)} and {@link #isMemberClass(Class)}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean result = ClassUtil.isAnonymousOrMemberClass(someClass);
     * }</pre>
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

    /**
     * Checks if the specified class is a primitive type.
     * Primitive types include: boolean, char, byte, short, int, long, float, and double.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean result = ClassUtil.isPrimitiveType(int.class); // Returns true
     * boolean result2 = ClassUtil.isPrimitiveType(Integer.class); // Returns false
     * }</pre>
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
     * Primitive wrapper types include: Boolean, Character, Byte, Short, Integer, Long, Float, and Double.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean result = ClassUtil.isPrimitiveWrapper(Integer.class); // Returns true
     * boolean result2 = ClassUtil.isPrimitiveWrapper(int.class); // Returns false
     * }</pre>
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
     * Primitive array types include: boolean[], char[], byte[], short[], int[], long[], float[], and double[].
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean result = ClassUtil.isPrimitiveArrayType(int[].class); // Returns true
     * boolean result2 = ClassUtil.isPrimitiveArrayType(Integer[].class); // Returns false
     * }</pre>
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
    private static final BiMap<Class<?>, Class<?>> PRIMITIVE_2_WRAPPER = new BiMap<>();

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
     * Returns the corresponding wrapper type of the specified class if it is a primitive type; otherwise returns the class itself.
     * This method also handles primitive array types, converting them to their wrapper array equivalents.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Class<?> wrapped = ClassUtil.wrap(int.class); // Returns Integer.class
     * Class<?> wrapped2 = ClassUtil.wrap(Integer.class); // Returns Integer.class
     * Class<?> wrapped3 = ClassUtil.wrap(String.class); // Returns String.class
     * Class<?> wrapped4 = ClassUtil.wrap(int[].class); // Returns Integer[].class
     * }</pre>
     *
     * @param cls the class to be wrapped
     * @return the corresponding wrapper type if {@code cls} is a primitive type or primitive array, otherwise {@code cls} itself
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     */
    public static Class<?> wrap(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        final Class<?> wrapped = PRIMITIVE_2_WRAPPER.get(cls);

        return wrapped == null ? cls : wrapped;
    }

    /**
     * Returns the corresponding primitive type of the specified class if it is a wrapper type; otherwise returns the class itself.
     * This method also handles wrapper array types, converting them to their primitive array equivalents.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Class<?> unwrapped = ClassUtil.unwrap(Integer.class); // Returns int.class
     * Class<?> unwrapped2 = ClassUtil.unwrap(int.class); // Returns int.class
     * Class<?> unwrapped3 = ClassUtil.unwrap(String.class); // Returns String.class
     * Class<?> unwrapped4 = ClassUtil.unwrap(Integer[].class); // Returns int[].class
     * }</pre>
     *
     * @param cls the class to be unwrapped
     * @return the corresponding primitive type if {@code cls} is a wrapper type or wrapper array, otherwise {@code cls} itself
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     */
    public static Class<?> unwrap(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        final Class<?> unwrapped = PRIMITIVE_2_WRAPPER.getByValue(cls);

        return unwrapped == null ? cls : unwrapped;
    }

    /**
     * Creates a MethodHandle for the specified method. MethodHandles provide
     * a more efficient way to invoke methods compared to standard reflection.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Method method = MyClass.class.getMethod("getValue");
     * MethodHandle handle = ClassUtil.createMethodHandle(method);
     * Object result = handle.invoke(myInstance);
     * }</pre>
     *
     * @param method the method for which the MethodHandle is to be created
     * @return the MethodHandle for the specified method
     * @throws UnsupportedOperationException if the MethodHandle cannot be created
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static MethodHandle createMethodHandle(final Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();
        MethodHandles.Lookup lookup = null;

        try {
            lookup = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup());

            return lookup.in(declaringClass).unreflectSpecial(method, declaringClass);
        } catch (final Exception e) {
            try {
                final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                ClassUtil.setAccessible(constructor, true);

                return constructor.newInstance(declaringClass).in(declaringClass).unreflectSpecial(method, declaringClass);
            } catch (final Exception ex) {
                try {
                    return lookup.findSpecial(declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                            declaringClass);
                } catch (final Exception exx) {
                    throw new UnsupportedOperationException(exx);
                }
            }
        }
    }

    /**
     * Creates and returns a new instance of the <i>None</i> class, which serves as a {@code null} mask.
     * This object can be used as a placeholder to distinguish between an explicitly set {@code null} value
     * and an absent value.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Object nullMask = ClassUtil.createNullMask();
     * // Use nullMask as a sentinel value to represent null in contexts where null itself cannot be used
     * }</pre>
     *
     * @return a new instance of the <i>None</i> class that serves as a null placeholder
     */
    public static Object createNullMask() {
        return new None();
    }

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
