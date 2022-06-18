/*
 * Copyright (C) 2017 HaiYang Li
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
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
import com.landawn.abacus.util.u.Holder;
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
import com.landawn.abacus.util.u.R;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @since 0.9
 */
public final class ClassUtil {

    /**
     * The Class ClassMask.
     */
    static final class ClassMask {

        /** The Constant FIELD_MASK. */
        @SuppressWarnings("hiding")
        static final String FIELD_MASK = "FIELD_MASK";

        /**
         * Method mask.
         */
        static void methodMask() {
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private static final String JAR_POSTFIX = ".jar";

    private static final String CLASS_POSTFIX = ".class";

    // ...
    private static final String PROP_NAME_SEPARATOR = ".";

    // ...
    private static final String GET = "get".intern();

    private static final String SET = "set".intern();

    private static final String IS = "is".intern();

    private static final String HAS = "has".intern();

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
        BUILT_IN_TYPE.put(R.class.getCanonicalName(), R.class);

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

        BUILT_IN_TYPE.put(ArrayHashMap.class.getCanonicalName(), ArrayHashMap.class);
        BUILT_IN_TYPE.put(LinkedArrayHashMap.class.getCanonicalName(), LinkedArrayHashMap.class);
        BUILT_IN_TYPE.put(ArrayHashSet.class.getCanonicalName(), ArrayHashSet.class);
        BUILT_IN_TYPE.put(LinkedArrayHashSet.class.getCanonicalName(), LinkedArrayHashSet.class);
        BUILT_IN_TYPE.put(BiMap.class.getCanonicalName(), BiMap.class);
        BUILT_IN_TYPE.put(ListMultimap.class.getCanonicalName(), ListMultimap.class);
        BUILT_IN_TYPE.put(SetMultimap.class.getCanonicalName(), SetMultimap.class);
        BUILT_IN_TYPE.put(Multimap.class.getCanonicalName(), Multimap.class);
        BUILT_IN_TYPE.put(Multiset.class.getCanonicalName(), Multiset.class);
        BUILT_IN_TYPE.put(LongMultiset.class.getCanonicalName(), LongMultiset.class);
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
        for (Class<?> cls : classes) {
            Class<?> arrayClass = cls;

            for (int i = 0; i < 7; i++) {
                arrayClass = java.lang.reflect.Array.newInstance(arrayClass, 0).getClass();

                BUILT_IN_TYPE.put(arrayClass.getCanonicalName(), arrayClass);
            }
        }

        classes = new ArrayList<>(BUILT_IN_TYPE.values());
        for (Class<?> cls : classes) {
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

    private static Map<String, String> SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME = new HashMap<>();

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

    private static final Map<Class<?>, ImmutableList<String>> entityDeclaredPropNameListPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Field>> entityDeclaredPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Field>> entityPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> entityDeclaredPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> entityDeclaredPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> entityPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> entityPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, List<Method>>> entityInlinePropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    //    /** The Constant entityInlinePropSetMethodPool. */
    //    private static final Map<Class<?>, Map<String, List<Method>>> entityInlinePropSetMethodPool = new ObjectPool<>(POOL_SIZE);

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

    private static final Map<Class<?>, Map<Class<?>[], Constructor<?>>> classDeclaredConstructorPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> classNoArgDeclaredMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Map<Class<?>[], Method>>> classDeclaredMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Class<?>> registeredNonEntityClass = new ObjectPool<>(POOL_SIZE);

    static {
        registeredNonEntityClass.put(Object.class, Object.class);
        registeredNonEntityClass.put(Class.class, Class.class);
        registeredNonEntityClass.put(Calendar.class, Calendar.class);
        registeredNonEntityClass.put(java.util.Date.class, java.util.Date.class);
        registeredNonEntityClass.put(java.sql.Date.class, java.sql.Date.class);
        registeredNonEntityClass.put(java.sql.Time.class, java.sql.Time.class);
        registeredNonEntityClass.put(java.sql.Timestamp.class, java.sql.Timestamp.class);
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
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
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

        for (String key : new ArrayList<>(builtinTypeNameMap.keySet())) {
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
     * Register non entity class.
     *
     * @param cls
     */
    @SuppressWarnings("deprecation")
    public static void registerNonEntityClass(final Class<?> cls) {
        registeredNonEntityClass.put(cls, cls);

        synchronized (entityDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, false);

            if (entityDeclaredPropGetMethodPool.containsKey(cls)) {
                entityDeclaredPropGetMethodPool.remove(cls);
                entityDeclaredPropSetMethodPool.remove(cls);

                entityPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshEntityPropInfo(cls);
        }
    }

    /**
     * Register non prop get set method.
     *
     * @param cls
     * @param propName
     */
    @SuppressWarnings("deprecation")
    public static void registerNonPropGetSetMethod(final Class<?> cls, final String propName) {
        synchronized (registeredNonPropGetSetMethodPool) {
            Set<String> set = registeredNonPropGetSetMethodPool.get(cls);

            if (set == null) {
                set = N.newHashSet();
                registeredNonPropGetSetMethodPool.put(cls, set);
            }

            set.add(propName);

            ParserUtil.refreshEntityPropInfo(cls);
        }
    }

    /**
     * Register prop get set method.
     *
     * @param propName
     * @param method
     */
    @SuppressWarnings("deprecation")
    public static void registerPropGetSetMethod(final String propName, final Method method) {
        Class<?> cls = method.getDeclaringClass();

        synchronized (entityDeclaredPropGetMethodPool) {
            if (isGetMethod(method)) {
                Map<String, Method> propMethodMap = entityPropGetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = entityPropGetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been regiestered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else if (isSetMethod(method)) {
                Map<String, Method> propMethodMap = entityPropSetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = entityPropSetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been regiestered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else {
                throw new IllegalArgumentException("The name of property getter/setter method must start with 'get/is/has' or 'set': " + method.getName());
            }

            ParserUtil.refreshEntityPropInfo(cls);
        }
    }

    /**
     * The property maybe only has get method if its type is collection or map by xml binding specification
     * Otherwise, it will be ignored if not registered by calling this method.
     *
     * @param cls
     */
    @SuppressWarnings("deprecation")
    public static void registerXMLBindingClass(final Class<?> cls) {
        if (registeredXMLBindingClassList.containsKey(cls)) {
            return;
        }

        synchronized (entityDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, false);

            if (entityDeclaredPropGetMethodPool.containsKey(cls)) {
                entityDeclaredPropGetMethodPool.remove(cls);
                entityDeclaredPropSetMethodPool.remove(cls);

                entityPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshEntityPropInfo(cls);
        }
    }

    public static boolean isRegisteredXMLBindingClass(final Class<?> cls) {
        return registeredXMLBindingClassList.containsKey(cls);
    }

    public static MethodHandle createMethodHandle(final Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();

        try {
            return MethodHandles.lookup().in(declaringClass).unreflectSpecial(method, declaringClass);
        } catch (Exception e) {
            try {
                final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
                ClassUtil.setAccessible(constructor, true);

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
     * <p>Returns the number of inheritance hops between two classes.</p>
     *
     * @param child the child class, may be {@code null}
     * @param parent the parent class, may be {@code null}
     * @return the number of generations between the child and parent; 0 if the same class;
     * -1 if the classes are not related as child and parent (includes where either class is null)
     * @since 3.2
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
    private static String filePath2PackageName(String entryName) {
        String pkgName = entryName.replace('/', '.').replace('\\', '.');
        return pkgName.endsWith(".") ? pkgName.substring(0, pkgName.length() - 1) : pkgName;
    }

    // Superclasses/Superinterfaces. Copied from Apache Commons Lang under Apache License v2.
    // ----------------------------------------------------------------------

    /**
     * Supports primitive types: boolean, char, byte, short, int, long, float, double. And array type with format {@code java.lang.String[]}
     *
     * @param <T>
     * @param clsName
     * @return
     * @throws IllegalArgumentException if class not found.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> forClass(final String clsName) throws IllegalArgumentException {
        return (Class<T>) forClass(clsName, true);
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
                    cls = Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    String newClassName = clsName;

                    if (newClassName.indexOf(WD._PERIOD) < 0) {
                        int index = newClassName.indexOf("[]");

                        if (((index < 0) && !SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.containsKey(newClassName))
                                || ((index > 0) && !SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.containsKey(newClassName.substring(0, index)))) {
                            newClassName = "java.lang." + newClassName;

                            try {
                                cls = Class.forName(newClassName);

                                BUILT_IN_TYPE.put(clsName, cls);
                            } catch (ClassNotFoundException e1) {
                                // ignore.
                            }
                        }
                    }

                    if (cls == null) {
                        newClassName = clsName;
                        int index = newClassName.indexOf("[]");

                        if (index > 0) {
                            String componentTypeName = newClassName.substring(0, index);
                            String temp = newClassName.replaceAll("\\[\\]", "");

                            if (componentTypeName.equals(temp)) {
                                int dimensions = (newClassName.length() - temp.length()) / 2;
                                String prefixOfArray = "";

                                while (dimensions-- > 0) {
                                    prefixOfArray += "[";
                                }

                                String symbolOfPrimitiveArraryClassName = SYMBOL_OF_PRIMITIVE_ARRAY_CLASS_NAME.get(componentTypeName);

                                if (symbolOfPrimitiveArraryClassName != null) {
                                    try {
                                        cls = Class.forName(prefixOfArray + symbolOfPrimitiveArraryClassName);

                                        BUILT_IN_TYPE.put(clsName, cls);
                                    } catch (ClassNotFoundException e2) {
                                        // ignore.
                                    }
                                } else {
                                    try {
                                        final Type<?> componentType = N.typeOf(componentTypeName);

                                        if (componentType.clazz().equals(Object.class) && !componentType.name().equals(ObjectType.OBJECT)) {
                                            throw new IllegalArgumentException("No Class found by name: " + clsName);
                                        }

                                        cls = Class.forName(prefixOfArray + "L" + componentType.clazz().getCanonicalName() + ";");
                                    } catch (ClassNotFoundException e3) {
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
                                    cls = Class.forName(newClassName);
                                    break;
                                } catch (ClassNotFoundException e3) {
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
     * It's designed for field/method/class/column/table names. and source and target Strings will be cached.
     *
     * @param propName
     * @return
     */
    public static String formalizePropName(final String propName) {
        String newPropName = formalizedPropNamePool.get(propName);

        if (newPropName == null) {
            newPropName = toCamelCase(propName);

            for (String keyWord : keyWordMapper.keySet()) {
                if (keyWord.equalsIgnoreCase(newPropName)) {
                    newPropName = keyWordMapper.get(keyWord);

                    break;
                }
            }

            formalizedPropNamePool.put(propName, newPropName);
        }

        return newPropName;
    }

    /**
     * Format parameterized type name.
     *
     * @param parameterizedTypeName
     * @return
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

        res = res.replaceAll("java.lang.", "").replaceAll("class ", "").replaceAll("interface ", "");

        final int idx = res.lastIndexOf('$');

        if (idx > 0) {
            final StringBuilder sb = new StringBuilder();

            for (int len = res.length(), i = len - 1; i >= 0; i--) {
                char ch = res.charAt(i);
                sb.append(ch);

                if (ch == '$') {
                    int j = i;
                    char x = 0;
                    while (--i >= 0 && (Character.isLetterOrDigit(x = res.charAt(i)) || x == '_' || x == '.')) {
                    }

                    final String tmp = res.substring(i + 1, j);

                    if (tmp.substring(0, tmp.length() / 2).equals(tmp.substring(tmp.length() / 2 + 1))) {
                        sb.append(StringUtil.reverse(tmp.substring(0, tmp.length() / 2)));
                    } else {
                        sb.append(StringUtil.reverse(tmp));
                    }

                    i++;
                }
            }

            res = sb.reverse().toString();
        }

        return res;
    }

    /**
     * Gets the canonical class name.
     *
     * @param cls
     * @return
     */
    public static String getCanonicalClassName(final Class<?> cls) {
        String clsName = canonicalClassNamePool.get(cls);

        if (clsName == null) {
            clsName = cls.getCanonicalName();

            if (clsName == null) {
                clsName = cls.getName();
            }

            if (clsName != null) {
                canonicalClassNamePool.put(cls, clsName);
            }
        }

        return clsName;
    }

    /**
     * Gets the class name.
     *
     * @param cls
     * @return
     */
    public static String getClassName(final Class<?> cls) {
        String clsName = nameClassPool.get(cls);

        if (clsName == null) {
            clsName = cls.getName();
            nameClassPool.put(cls, clsName);
        }

        return clsName;
    }

    //    private static Class[] getTypeArguments(Class cls) {
    //        java.lang.reflect.Type[] typeArgs = null;
    //        java.lang.reflect.Type[] genericInterfaces = cls.getGenericInterfaces();
    //
    //        if (notNullOrEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notNullOrEmpty(typeArgs)) {
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
    //        if (notNullOrEmpty(typeArgs)) {
    //            Class[] clses = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                clses[i] = (Class) typeArgs[i];
    //            }
    //
    //            return clses;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case insensitive
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
     * Gets the simple class name.
     *
     * @param cls
     * @return
     */
    public static String getSimpleClassName(final Class<?> cls) {
        String clsName = simpleClassNamePool.get(cls);

        if (clsName == null) {
            clsName = cls.getSimpleName();
            simpleClassNamePool.put(cls, clsName);
        }

        return clsName;
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

            Class<?> superclass = cls.getSuperclass();

            if (superclass != null && !superclass.equals(Object.class) && superTypesFound.add(superclass)) {
                getAllSuperTypes(superclass, superTypesFound);
            }

            cls = cls.getSuperclass();
        }
    }

    /**
     * Gets the package.
     *
     * @param cls
     * @return <code>null</code> if it's primitive type or no package defined for the class.
     */
    public static Package getPackage(final Class<?> cls) {
        Package pkg = packagePool.get(cls);

        if (pkg == null) {
            if (N.isPrimitiveType(cls)) {
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
     * Gets the package name.
     *
     * @param cls
     * @return <code>null</code> if it's primitive type or no package defined for the class.
     */
    public static String getPackageName(final Class<?> cls) {
        String pkgName = packageNamePool.get(cls);

        if (pkgName == null) {
            Package pkg = ClassUtil.getPackage(cls);
            pkgName = pkg == null ? "" : pkg.getName();
            packageNamePool.put(cls, pkgName);
        }

        return pkgName;
    }

    /**
     * Gets the classes by package.
     *
     * @param pkgName
     * @param isRecursive
     * @param skipClassLoaddingException
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<Class<?>> getClassesByPackage(String pkgName, boolean isRecursive, boolean skipClassLoaddingException) throws UncheckedIOException {
        return getClassesByPackage(pkgName, isRecursive, skipClassLoaddingException, Fn.alwaysTrue());
    }

    /**
     * Gets the classes by package.
     *
     * @param <E>
     * @param pkgName
     * @param isRecursive
     * @param skipClassLoaddingException
     * @param predicate
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> List<Class<?>> getClassesByPackage(String pkgName, boolean isRecursive, boolean skipClassLoaddingException,
            Throwables.Predicate<? super Class<?>, E> predicate) throws UncheckedIOException, E {
        if (logger.isInfoEnabled()) {
            logger.info("Looking for classes in package: " + pkgName);
        }

        String pkgPath = packageName2FilePath(pkgName);

        List<URL> resourceList = getResources(pkgName);

        if (N.isNullOrEmpty(resourceList)) {
            throw new IllegalArgumentException("No resource found by package " + pkgName);
        }

        List<Class<?>> classes = new ArrayList<>();
        for (URL resource : resourceList) {
            // Get a File object for the package
            String fullPath = resource.getPath().replace("%20", " ").replaceFirst("[.]jar[!].*", JAR_POSTFIX).replaceFirst("file:", "");

            if (logger.isInfoEnabled()) {
                logger.info("ClassDiscovery: FullPath = " + fullPath);
            }

            File file = new File(fullPath);

            if (file.exists() && file.isDirectory()) {
                // Get the list of the files contained in the package
                File[] files = file.listFiles();

                if (N.isNullOrEmpty(files)) {
                    continue;
                }

                for (File file2 : files) {
                    if (file2 == null) {
                        continue;
                    }

                    // we are only interested in .class files
                    if (file2.isFile() && file2.getName().endsWith(CLASS_POSTFIX)) {
                        // removes the .class extension
                        String className = pkgName + '.' + file2.getName().substring(0, file2.getName().length() - CLASS_POSTFIX.length());

                        try {
                            Class<?> clazz = ClassUtil.forClass(className, false);

                            if (clazz.getCanonicalName() != null && predicate.test(clazz)) {
                                classes.add(clazz);
                            }
                        } catch (Throwable e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(e, "Failed to load class: " + className);
                            }

                            if (!skipClassLoaddingException) {
                                throw new RuntimeException("ClassNotFoundException loading " + className);
                            }
                        }
                    } else if (file2.isDirectory() && isRecursive) {
                        String subPkgName = pkgName + WD._PERIOD + file2.getName();
                        classes.addAll(getClassesByPackage(subPkgName, isRecursive, skipClassLoaddingException, predicate));
                    }
                }
            } else if (file.exists() && file.getName().endsWith(JAR_POSTFIX)) {
                JarFile jarFile = null;

                try {
                    jarFile = new JarFile(file.getPath());

                    Enumeration<JarEntry> entries = jarFile.entries();
                    JarEntry entry = null;
                    String entryName = null;

                    while (entries.hasMoreElements()) {
                        entry = entries.nextElement();
                        entryName = entry.getName();

                        if (entryName.startsWith(pkgPath)) {
                            if (entryName.endsWith(CLASS_POSTFIX) && (entryName.indexOf("/", pkgPath.length()) < 0)) {
                                String className = filePath2PackageName(entryName).replace(CLASS_POSTFIX, "");

                                try {
                                    Class<?> clazz = ClassUtil.forClass(className, false);

                                    if ((clazz.getCanonicalName() != null) && (clazz.getPackage().getName().equals(pkgName)
                                            || (clazz.getPackage().getName().startsWith(pkgName) && isRecursive)) && predicate.test(clazz)) {
                                        classes.add(clazz);
                                    }
                                } catch (Throwable e) {
                                    if (logger.isWarnEnabled()) {
                                        logger.warn("ClassNotFoundException loading " + className);
                                    }

                                    if (!skipClassLoaddingException) {
                                        IOUtil.close(jarFile);
                                        jarFile = null;
                                        throw new RuntimeException("ClassNotFoundException loading " + className);
                                    }
                                }
                            } else if (entry.isDirectory() && (entryName.length() > (pkgPath.length() + 1)) && isRecursive) {
                                String subPkgName = filePath2PackageName(entryName);
                                classes.addAll(getClassesByPackage(subPkgName, isRecursive, skipClassLoaddingException, predicate));
                            }
                        }
                    }
                } catch (IOException e) {
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
    //        if (notNullOrEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notNullOrEmpty(typeArgs)) {
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
    //        if (notNullOrEmpty(typeArgs)) {
    //            Class[] clses = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                clses[i] = (Class) typeArgs[i];
    //            }
    //
    //            return clses;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case insensitive
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
     * Gets the enclosing class.
     *
     * @param cls
     * @return
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
     *
     * @param <T>
     * @param cls
     * @param parameterTypes
     * @return {@code null} if no constructor is found
     */
    @SafeVarargs
    public static <T> Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?>... parameterTypes) {
        Constructor<?> constructor = null;

        if (parameterTypes == null || parameterTypes.length == 0) {
            constructor = classNoArgDeclaredConstructorPool.get(cls);

            if (constructor == null) {
                try {
                    constructor = cls.getDeclaredConstructor(parameterTypes);

                    // SHOULD NOT set it true here.
                    // ClassUtil.setAccessible(constructor, true);
                } catch (NoSuchMethodException e) {
                    // ignore.
                }

                if (constructor != null) {
                    classNoArgDeclaredConstructorPool.put(cls, constructor);
                }
            }
        } else {
            Map<Class<?>[], Constructor<?>> constructorPool = classDeclaredConstructorPool.get(cls);

            if (constructorPool != null) {
                constructor = constructorPool.get(parameterTypes);
            }

            if (constructor == null) {
                try {
                    constructor = cls.getDeclaredConstructor(parameterTypes);

                    // SHOULD NOT set it true here.
                    // ClassUtil.setAccessible(constructor, true);
                } catch (NoSuchMethodException e) {
                    // ignore.
                }

                if (constructor != null) {
                    if (constructorPool == null) {
                        constructorPool = new ArrayHashMap<>(ConcurrentHashMap.class);
                        classDeclaredConstructorPool.put(cls, constructorPool);
                    }

                    constructorPool.put(parameterTypes.clone(), constructor);
                }
            }

        }

        return (Constructor<T>) constructor;
    }

    /**
     * Gets the declared field.
     *
     * @param cls
     * @param fieldName
     * @return
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
     *
     * @param cls
     * @param methodName
     * @param parameterTypes
     * @return {@code null} if no method is found
     */
    @SafeVarargs
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
            Map<String, Map<Class<?>[], Method>> methodNamePool = classDeclaredMethodPool.get(cls);
            Map<Class<?>[], Method> methodPool = methodNamePool == null ? null : methodNamePool.get(methodName);

            if (methodPool != null) {
                method = methodPool.get(parameterTypes);
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
                        methodPool = new ArrayHashMap<>(ConcurrentHashMap.class);
                        methodNamePool.put(methodName, methodPool);
                    }

                    methodPool.put(parameterTypes.clone(), method);
                }
            }
        }

        return method;
    }

    /**
     * Gets the parameterized type name by method.
     *
     * @param field
     * @return
     */
    public static String getParameterizedTypeNameByField(final Field field) {
        return formatParameterizedTypeName(field.getGenericType().getTypeName());
    }

    /**
     * Gets the parameterized type name by method.
     *
     * @param method
     * @return
     */
    public static String getParameterizedTypeNameByMethod(final Method method) {
        final java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();

        if (N.notNullOrEmpty(genericParameterTypes)) {
            return formatParameterizedTypeName(genericParameterTypes[0].getTypeName());
        }
        return formatParameterizedTypeName(method.getGenericReturnType().getTypeName());
    }

    /**
     * Gets the prop name by method.
     *
     * @param getSetMethod
     * @return
     */
    public static String getPropNameByMethod(final Method getSetMethod) {
        String propName = methodPropNamePool.get(getSetMethod);

        if (propName == null) {
            final String methodName = getSetMethod.getName();
            final Class<?>[] paramTypes = getSetMethod.getParameterTypes();
            final Class<?> targetType = N.isNullOrEmpty(paramTypes) ? getSetMethod.getReturnType() : paramTypes[0];

            Field field = getDeclaredField(getSetMethod.getDeclaringClass(), methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            if (N.isNullOrEmpty(propName) && ((methodName.startsWith(IS) && methodName.length() > 2)
                    || ((methodName.startsWith(GET) || methodName.startsWith(SET) || methodName.startsWith(HAS)) && methodName.length() > 3))) {
                final String newName = methodName.substring(methodName.startsWith(IS) ? 2 : 3);
                field = getDeclaredField(getSetMethod.getDeclaringClass(), StringUtil.uncapitalize(newName));

                if (field != null && field.getType().isAssignableFrom(targetType)) {
                    propName = field.getName();
                }

                if (N.isNullOrEmpty(propName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + StringUtil.uncapitalize(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (N.isNullOrEmpty(propName)) {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (N.isNullOrEmpty(propName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (N.isNullOrEmpty(propName)) {
                    propName = formalizePropName(newName);
                }
            }

            if (N.isNullOrEmpty(propName)) {
                propName = methodName;
            }

            methodPropNamePool.put(getSetMethod, propName);
        }

        return propName;
    }

    /**
     * Returns an immutable entity property name List by the specified class.
     *
     * @param cls
     * @return
     */
    public static ImmutableList<String> getPropNameList(final Class<?> cls) {
        ImmutableList<String> propNameList = entityDeclaredPropNameListPool.get(cls);

        if (propNameList == null) {
            loadPropGetSetMethodList(cls);
            propNameList = entityDeclaredPropNameListPool.get(cls);
        }

        return propNameList;
    }

    /**
     *
     * @param <E>
     * @param entity
     * @param propValueFilter first parameter is property value, second parameter is property type.
     * @return
     * @throws E
     */
    public static <E extends Exception> List<String> getPropNames(final Object entity, Throwables.BiPredicate<Object, Type<Object>, E> propValueFilter)
            throws E {
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());
        final int size = entityInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (PropInfo propInfo : entityInfo.propInfoList) {
            if (propValueFilter.test(propInfo.getPropValue(result), propInfo.type)) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param entity
     * @param propValueFilter
     * @return
     * @throws E
     */
    public static <E extends Exception> List<String> getPropNames(final Object entity, Throwables.Predicate<Object, E> propValueFilter) throws E {
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());
        final int size = entityInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (PropInfo propInfo : entityInfo.propInfoList) {
            if (propValueFilter.test(propInfo.getPropValue(result))) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     * Gets the prop name list exclusively.
     *
     * @param cls
     * @param propNameToExcluded
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static List<String> getPropNamesExclusively(final Class<?> cls, final Collection<String> propNameToExcluded) {
        if (N.isNullOrEmpty(propNameToExcluded)) {
            return new ArrayList<>(getPropNameList(cls));
        }
        if (propNameToExcluded instanceof Set) {
            return getPropNamesExclusively(cls, (Set) propNameToExcluded);
        }
        return getPropNamesExclusively(cls, N.newHashSet(propNameToExcluded));
    }

    /**
     * Gets the prop name list exclusively.
     *
     * @param cls
     * @param propNameToExcluded
     * @return
     */
    public static List<String> getPropNamesExclusively(final Class<?> cls, final Set<String> propNameToExcluded) {
        final List<String> propNameList = getPropNameList(cls);

        if (N.isNullOrEmpty(propNameToExcluded)) {
            return new ArrayList<>(propNameList);
        }
        final List<String> result = new ArrayList<>(propNameList.size() - propNameToExcluded.size());

        for (String propName : propNameList) {
            if (!propNameToExcluded.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }

    /**
     *
     * @param entity
     * @return
     */
    public static List<String> getNonNullPropNames(final Object entity) {
        return getPropNames(entity, Fn.<Object> notNull());
    }

    /**
     * Gets the prop field.
     *
     * @param cls
     * @param propName
     * @return
     */
    public static Field getPropField(final Class<?> cls, final String propName) {
        Map<String, Field> propFieldMap = entityPropFieldPool.get(cls);

        if (propFieldMap == null) {
            loadPropGetSetMethodList(cls);
            propFieldMap = entityPropFieldPool.get(cls);
        }

        Field field = propFieldMap.get(propName);

        if (field == null) {
            if (!ClassUtil.isEntity(cls)) {
                throw new IllegalArgumentException(
                        "No property getter/setter method or public field found in the specified entity: " + ClassUtil.getCanonicalClassName(cls));
            }

            synchronized (entityDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = ClassUtil.getPropGetMethods(cls);

                for (String key : getterMethodList.keySet()) {
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

    public static ImmutableMap<String, Field> getPropFields(final Class<?> cls) {
        ImmutableMap<String, Field> getterMethodList = entityDeclaredPropFieldPool.get(cls);

        if (getterMethodList == null) {
            loadPropGetSetMethodList(cls);
            getterMethodList = entityDeclaredPropFieldPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property get method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     *
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB
     * specification
     *
     * @param cls
     * @param propName
     * @return
     */
    public static Method getPropGetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propGetMethodMap = entityPropGetMethodPool.get(cls);

        if (propGetMethodMap == null) {
            loadPropGetSetMethodList(cls);
            propGetMethodMap = entityPropGetMethodPool.get(cls);
        }

        Method method = propGetMethodMap.get(propName);

        if (method == null) {
            synchronized (entityDeclaredPropGetMethodPool) {
                Map<String, Method> getterMethodList = getPropGetMethods(cls);

                for (String key : getterMethodList.keySet()) {
                    if (isPropName(cls, propName, key)) {
                        method = getterMethodList.get(key);

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
     * Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB
     * specification.
     *
     * @param cls
     * @return
     */
    public static ImmutableMap<String, Method> getPropGetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> getterMethodList = entityDeclaredPropGetMethodPool.get(cls);

        if (getterMethodList == null) {
            loadPropGetSetMethodList(cls);
            getterMethodList = entityDeclaredPropGetMethodPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property set method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     *
     * @param cls
     * @param propName
     * @return
     */
    public static Method getPropSetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propSetMethodMap = entityPropSetMethodPool.get(cls);

        if (propSetMethodMap == null) {
            loadPropGetSetMethodList(cls);
            propSetMethodMap = entityPropSetMethodPool.get(cls);
        }

        Method method = propSetMethodMap.get(propName);

        if (method == null) {
            synchronized (entityDeclaredPropGetMethodPool) {
                Map<String, Method> setterMethodList = getPropSetMethods(cls);

                for (String key : setterMethodList.keySet()) {
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
     * Gets the prop set method list.
     *
     * @param cls
     * @return
     */
    public static ImmutableMap<String, Method> getPropSetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> setterMethodList = entityDeclaredPropSetMethodPool.get(cls);

        if (setterMethodList == null) {
            loadPropGetSetMethodList(cls);
            setterMethodList = entityDeclaredPropSetMethodPool.get(cls);
        }

        return setterMethodList;
    }

    /**
     * Return the specified {@code propValue} got by the specified method
     * {@code propSetMethod} in the specified {@code entity}.
     *
     * @param <T>
     * @param entity MapEntity is not supported
     * @param propGetMethod
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPropValue(final Object entity, final Method propGetMethod) {
        try {
            return (T) propGetMethod.invoke(entity);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     * Refer to getPropValue(Method, Object).
     *
     * @param <T>
     * @param entity
     * @param propName is case insensitive
     * @return {@link #getPropValue(Object, Method)}
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPropValue(final Object entity, final String propName) {
        return getPropValue(entity, propName, false);
    }

    /**
     * Gets the prop value.
     *
     * @param <T>
     * @param entity
     * @param propName
     * @param ignoreUnmatchedProperty
     * @return
     * @throws IllegalArgumentException if the specified property can't be gotten and ignoreUnmatchedProperty is false.
     */
    public static <T> T getPropValue(final Object entity, final String propName, final boolean ignoreUnmatchedProperty) {
        final Class<?> cls = entity.getClass();
        final PropInfo propInfo = ParserUtil.getEntityInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return propInfo.getPropValue(entity);
        }
        Map<String, List<Method>> inlinePropGetMethodMap = entityInlinePropGetMethodPool.get(cls);
        List<Method> inlinePropGetMethodQueue = null;

        if (inlinePropGetMethodMap == null) {
            inlinePropGetMethodMap = new ObjectPool<>(ClassUtil.getPropNameList(cls).size());
            entityInlinePropGetMethodPool.put(cls, inlinePropGetMethodMap);
        } else {
            inlinePropGetMethodQueue = inlinePropGetMethodMap.get(propName);
        }

        if (inlinePropGetMethodQueue == null) {
            inlinePropGetMethodQueue = new ArrayList<>();

            final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);

            if (strs.length > 1) {
                Class<?> targetClass = cls;

                for (String str : strs) {
                    Method method = getPropGetMethod(targetClass, str);

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
        Object propEntity = entity;

        for (int i = 0, len = inlinePropGetMethodQueue.size(); i < len; i++) {
            propEntity = ClassUtil.getPropValue(propEntity, inlinePropGetMethodQueue.get(i));

            if (propEntity == null) {
                return (T) N.defaultValueOf(inlinePropGetMethodQueue.get(len - 1).getReturnType());
            }
        }

        return (T) propEntity;
    }

    /**
     * Set the specified {@code propValue} to {@code entity} by the specified
     * method {@code propSetMethod}. This method will try to convert
     * {@code propValue} to appropriate type and set again if fails to set in
     * the first time. The final value which is set to the property will be
     * returned if property is set successfully finally. it could be the input
     * {@code propValue} or converted property value, otherwise, exception will
     * be threw if the property value is set unsuccessfully.
     *
     * @param entity MapEntity is not supported
     * @param propSetMethod
     * @param propValue
     */
    public static void setPropValue(final Object entity, final Method propSetMethod, Object propValue) {
        if (propValue == null) {
            try {
                propSetMethod.invoke(entity, N.defaultValueOf(propSetMethod.getParameterTypes()[0]));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        } else {
            try {
                propSetMethod.invoke(entity, propValue);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e, "Failed to set property value by method: " + propSetMethod);
                }

                propValue = N.convert(propValue, ParserUtil.getEntityInfo(entity.getClass()).getPropInfo(propSetMethod.getName()).jsonXmlType);

                try {
                    propSetMethod.invoke(entity, propValue);
                } catch (IllegalAccessException | InvocationTargetException e2) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }
    }

    /**
     * Refer to setPropValue(Method, Object, Object).
     *
     * @param entity
     * @param propName is case insensitive
     * @param propValue
     * @deprecated replaced by {@link EntityInfo#setPropValue(Object, String, Object)}
     */
    @Deprecated
    public static void setPropValue(final Object entity, final String propName, final Object propValue) {
        setPropValue(entity, propName, propValue, false);
    }

    /**
     * Sets the prop value.
     *
     * @param entity
     * @param propName
     * @param propValue
     * @param ignoreUnmatchedProperty
     * @return true if the property value has been set.
     * @throws IllegalArgumentException if the specified property can't be set and ignoreUnmatchedProperty is false.
     * @deprecated replaced by {@link EntityInfo#setPropValue(Object, String, Object, boolean)}
     */
    @Deprecated
    public static boolean setPropValue(final Object entity, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {
        //    final Class<?> cls = entity.getClass();
        //    final PropInfo propInfo = ParserUtil.getEntityInfo(cls).getPropInfo(propName);
        //
        //    if (propInfo != null) {
        //        propInfo.setPropValue(entity, propValue);
        //    } else {
        //        Method getMethod = getPropGetMethod(cls, propName);
        //
        //        if (getMethod == null) {
        //            Map<String, List<Method>> inlinePropSetMethodMap = entityInlinePropSetMethodPool.get(cls);
        //            List<Method> inlinePropSetMethodQueue = null;
        //
        //            if (inlinePropSetMethodMap == null) {
        //                inlinePropSetMethodMap = new ObjectPool<>(ClassUtil.getPropNameList(cls).size());
        //                entityInlinePropSetMethodPool.put(cls, inlinePropSetMethodMap);
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
        //                inlinePropSetMethodMap.put(propName, N.isNullOrEmpty(inlinePropSetMethodQueue) ? N.<Method> emptyList() : inlinePropSetMethodQueue);
        //            }
        //
        //            if (inlinePropSetMethodQueue.size() == 0) {
        //                if (ignoreUnmatchedProperty) {
        //                    return false;
        //                } else {
        //                    throw new IllegalArgumentException("No property method found with property name: " + propName + " in class " + cls.getCanonicalName());
        //                }
        //            } else {
        //                Object propEntity = entity;
        //                Method method = null;
        //
        //                for (int i = 0, len = inlinePropSetMethodQueue.size(); i < len; i++) {
        //                    method = inlinePropSetMethodQueue.get(i);
        //
        //                    if (i == (len - 1)) {
        //                        if (N.isNullOrEmpty(method.getParameterTypes())) {
        //                            setPropValueByGet(propEntity, method, propValue);
        //                        } else {
        //                            setPropValue(propEntity, method, propValue);
        //                        }
        //                    } else {
        //                        Object tmp = ClassUtil.getPropValue(propEntity, method);
        //
        //                        if (tmp == null) {
        //                            tmp = N.newInstance(method.getReturnType());
        //                            ClassUtil.setPropValue(propEntity, ClassUtil.getPropNameByMethod(method), tmp);
        //                        }
        //
        //                        propEntity = tmp;
        //                    }
        //                }
        //            }
        //        } else {
        //            setPropValueByGet(entity, getMethod, propValue);
        //        }
        //    }
        //
        //    return true;

        return ParserUtil.getEntityInfo(entity.getClass()).setPropValue(entity, propName, propValue, ignoreUnmatchedProperty);
    }

    /**
     * Sets the prop value by get.
     *
     * @param entity
     * @param propGetMethod
     * @param propValue
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPropValueByGet(final Object entity, final Method propGetMethod, final Object propValue) {
        if (propValue == null) {
            return;
        }

        final Object rt = invokeMethod(entity, propGetMethod);

        if (rt instanceof Collection) {
            final Collection<?> c = (Collection<?>) rt;
            c.clear();
            c.addAll((Collection) propValue);
        } else if (rt instanceof Map) {
            final Map<?, ?> m = (Map<?, ?>) rt;
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
        final Map<String, String> statisFinalFields = new HashMap<>();

        for (Field field : cls.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && String.class.equals(field.getType())) {
                String value;

                try {
                    value = (String) field.get(null);
                    statisFinalFields.put(value, value);
                } catch (Exception e) {
                    // ignore. should never happen
                }
            }
        }

        return statisFinalFields;
    }

    /**
     * Gets the resources.
     *
     * @param pkgName
     * @return
     */
    private static List<URL> getResources(String pkgName) {
        List<URL> resourceList = new ArrayList<>();
        String pkgPath = packageName2FilePath(pkgName);
        ClassLoader localClassLoader = ClassUtil.class.getClassLoader();
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

        try {
            Enumeration<URL> resources = localClassLoader.getResources(pkgPath);

            while (resources != null && resources.hasMoreElements()) {
                resourceList.add(resources.nextElement());
            }

            if (N.isNullOrEmpty(resourceList)) {
                resources = sysClassLoader.getResources(pkgPath);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

            if (N.isNullOrEmpty(resourceList)) {
                resources = localClassLoader.getResources(pkgName);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

            if (N.isNullOrEmpty(resourceList)) {
                resources = sysClassLoader.getResources(pkgName);

                while (resources != null && resources.hasMoreElements()) {
                    resourceList.add(resources.nextElement());
                }
            }

        } catch (IOException e) {
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
     * @param declaringClass
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

    private static final Map<Class<?>, Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>>> builderMap = new ConcurrentHashMap<>();

    public static Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> getBuilderInfo(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> builderInfo = builderMap.get(cls);

        if (builderInfo == null) {
            Method buildMethod = null;
            Class<?> builderClass = null;
            Method builderMethod = getBuilderMethod(cls);

            if (builderMethod == null) {
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

                    final Supplier<Object> builderSupplier = () -> ClassUtil.invokeMethod(finalBuilderMethod);
                    final Function<Object, Object> buildFunc = instance -> ClassUtil.invokeMethod(instance, finalBuildMethod);

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
        } catch (Exception e) {
            // ignore
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("newBuilder");
            } catch (Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("createBuilder");
            } catch (Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            return null;
        }

        return builderMethod;
    }

    private static Method getBuildMethod(final Class<?> builderClass, final Class<?> entityClass) {
        Method buildMethod = null;

        try {
            buildMethod = builderClass.getDeclaredMethod("build");
        } catch (Exception e) {
            // ignore
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !entityClass.isAssignableFrom(buildMethod.getReturnType())) {
            try {
                buildMethod = builderClass.getDeclaredMethod("create");
            } catch (Exception e) {
                // ignore
            }
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !entityClass.isAssignableFrom(buildMethod.getReturnType())) {
            return null;
        }

        return buildMethod;
    }

    public static String getTypeName(final java.lang.reflect.Type type) {
        return formatParameterizedTypeName(type.getTypeName());
    }

    /**
     * Gets an {@link Iterator} that can iterate over a class hierarchy in ascending (subclass to superclass) order,
     * excluding interfaces.
     *
     * @param type the type to get the class hierarchy from
     * @return Iterator an Iterator over the class hierarchy of the given class
     * @since 3.2
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
     * @since 3.2
     */
    public static ObjIterator<Class<?>> hierarchy(final Class<?> type, final boolean includeInterface) {
        final ObjIterator<Class<?>> superClassesIter = new ObjIterator<>() {
            private final u.Holder<Class<?>> next = new u.Holder<>(type);

            @Override
            public boolean hasNext() {
                return next.value() != null;
            }

            @Override
            public Class<?> next() {
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

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void walkInterfaces(final Set<Class<?>> addTo, final Class<?> c) {
                for (final Class<?> iface : c.getInterfaces()) {
                    if (!seenInterfaces.contains(iface)) {
                        addTo.add(iface);
                    }

                    walkInterfaces(addTo, iface);
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
        } catch (NoSuchMethodException e) {
            // ignore.
        }

        if (method == null) {
            Method[] methods = cls.getDeclaredMethods();

            for (Method m : methods) {
                if (m.getName().equalsIgnoreCase(methodName) && N.equals(parameterTypes, m.getParameterTypes())) {
                    method = m;

                    break;
                }
            }
        }

        return method;
    }

    /**
     *
     * @param <T>
     * @param constructor
     * @param args
     * @return
     */
    @SafeVarargs
    public static <T> T invokeConstructor(final Constructor<T> constructor, final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param <T>
     * @param method
     * @param args
     * @return
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> T invokeMethod(final Method method, final Object... args) {
        return invokeMethod(null, method, args);
    }

    /**
     *
     * @param <T>
     * @param instance
     * @param method
     * @param args
     * @return
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> T invokeMethod(final Object instance, final Method method, final Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    private static final Map<Class<?>, Boolean> entityClassPool = new ObjectPool<>(POOL_SIZE);

    /**
     * Checks if is entity.
     *
     * @param cls
     * @return true, if is entity
     */
    public static boolean isEntity(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        Boolean ret = entityClassPool.get(cls);

        if (ret == null) {
            ret = annotatedWithEntity(cls) || isRecordClass(cls) || N.notNullOrEmpty(ClassUtil.getPropNameList(cls));
            entityClassPool.put(cls, ret);
        }

        return ret;
    }

    private static boolean annotatedWithEntity(final Class<?> cls) {
        if (cls.getAnnotation(Entity.class) != null) {
            return true;
        }

        final Annotation[] annotations = cls.getAnnotations();

        if (N.notNullOrEmpty(annotations)) {
            for (Annotation annotation : annotations) {
                if (ClassUtil.getCanonicalClassName(annotation.getClass()).equals("javax.persistence.Entity")) {
                    return true;
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
     * @return true, if is field get method
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
     * @return true, if is gets the method
     */
    private static boolean isGetMethod(final Method method) {
        if (Object.class.equals(method.getDeclaringClass())) {
            return false;
        }

        String mn = method.getName();

        return (mn.startsWith(GET) || mn.startsWith(IS) || mn.startsWith(HAS) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && (N.isNullOrEmpty(method.getParameterTypes())) && !void.class.equals(method.getReturnType()) && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Checks if is JAXB get method.
     *
     * @param instance
     * @param method
     * @return true, if is JAXB get method
     */
    static boolean isJAXBGetMethod(final Object instance, final Method method) {
        try {
            return (instance != null) && (Collection.class.isAssignableFrom(method.getReturnType()) || Map.class.isAssignableFrom(method.getReturnType()))
                    && (invokeMethod(instance, method) != null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if is prop name.
     *
     * @param cls
     * @param inputPropName
     * @param propNameByMethod
     * @return true, if is prop name
     */
    static boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
        if (inputPropName.length() > 128) {
            throw new IllegalArgumentException("The property name execeed 128: " + inputPropName);
        }

        inputPropName = inputPropName.trim();

        return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(WD.UNDERSCORE, N.EMPTY_STRING).equalsIgnoreCase(propNameByMethod)
                || inputPropName.equalsIgnoreCase(getSimpleClassName(cls) + WD._PERIOD + propNameByMethod)
                || (inputPropName.startsWith(GET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(SET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(IS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(HAS) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod));
    }

    private static boolean isSetMethod(final Method method) {
        String mn = method.getName();

        return (mn.startsWith(SET) || getDeclaredField(method.getDeclaringClass(), mn) != null) && N.len(method.getParameterTypes()) == 1
                && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(method.getDeclaringClass()))
                && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Load prop get set method list.
     *
     * @param cls
     */
    private static void loadPropGetSetMethodList(final Class<?> cls) {
        synchronized (entityDeclaredPropGetMethodPool) {
            if (entityDeclaredPropGetMethodPool.containsKey(cls)) {
                return;
            }

            Object instance = null;

            if (registeredXMLBindingClassList.containsKey(cls)) {
                try {
                    instance = cls.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Failed to new instance of class: " + cls.getCanonicalName() + " to check setter method by getter method");
                    }
                }

                registeredXMLBindingClassList.put(cls, true);
            }

            final List<Class<?>> allClasses = new ArrayList<>();
            allClasses.add(cls);
            Class<?> superClass = null;

            while ((superClass = allClasses.get(allClasses.size() - 1).getSuperclass()) != null && !superClass.equals(Object.class)) {
                allClasses.add(superClass);
            }

            final Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> builderInfo = getBuilderInfo(cls);
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

                if (registeredNonEntityClass.containsKey(clazz)) {
                    continue;
                }

                final Map<String, String> staticFinalFields = getPublicStaticStringFields(clazz);

                final List<Tuple2<Field, Method>> fieldGetMethodList = new ArrayList<>();

                // sort the methods by the order of declared fields
                for (Field field : clazz.getDeclaredFields()) {
                    for (Method method : clazz.getMethods()) {
                        if (isFieldGetMethod(method, field)) {
                            fieldGetMethodList.add(Tuple.of(field, method));
                        } else if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                                && !Modifier.isFinal(field.getModifiers())) {
                            fieldGetMethodList.add(Tuple.of(field, null));
                        }
                    }
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
                    for (Tuple2<Field, Method> tp : fieldGetMethodList) {
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

                            if (isJAXBGetMethod(instance, method) || annotatedWithEntity(cls) || isRecordClass(clazz) || builderClass != null || isImmutable) {
                                // ClassUtil.setAccessibleQuietly(field, true);
                                ClassUtil.setAccessibleQuietly(method, true);

                                propFieldMap.put(propName, field);
                                propGetMethodMap.put(propName, method);

                                continue;
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

                for (Method method : clazz.getMethods()) {
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

                        if ((isJAXBGetMethod(instance, method) || annotatedWithEntity(cls) || isRecordClass(clazz))
                                && !propGetMethodMap.containsValue(method)) {
                            ClassUtil.setAccessibleQuietly(method, true);

                            propGetMethodMap.put(propName, method);

                            continue;
                        }
                    }
                }
            }

            for (Class<?> key : registeredNonPropGetSetMethodPool.keySet()) {
                if (key.isAssignableFrom(cls)) {
                    final Set<String> set = registeredNonPropGetSetMethodPool.get(key);
                    final List<String> methodNames = new ArrayList<>(propGetMethodMap.keySet());

                    for (String nonPropName : set) {
                        for (String propName : methodNames) {
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
            final ImmutableMap<String, Field> unmodifiableFieldMap = ImmutableMap.of(propFieldMap);
            unmodifiableFieldMap.keySet();
            entityDeclaredPropFieldPool.put(cls, unmodifiableFieldMap);

            // put it into map.
            final Map<String, Field> tempFieldMap = new ObjectPool<>(N.max(64, propFieldMap.size()));
            tempFieldMap.putAll(propFieldMap);
            entityPropFieldPool.put(cls, tempFieldMap);

            final ImmutableMap<String, Method> unmodifiableGetMethodMap = ImmutableMap.of(propGetMethodMap);
            unmodifiableGetMethodMap.keySet();
            entityDeclaredPropGetMethodPool.put(cls, unmodifiableGetMethodMap);

            if (entityPropGetMethodPool.get(cls) == null) {
                Map<String, Method> tmp = new ObjectPool<>(N.max(64, propGetMethodMap.size()));
                tmp.putAll(propGetMethodMap);
                entityPropGetMethodPool.put(cls, tmp);
            } else {
                entityPropGetMethodPool.get(cls).putAll(propGetMethodMap);
            }

            // for Double-Checked Locking is Broke initialize it before
            // put it into map.
            final ImmutableMap<String, Method> unmodifiableSetMethodMap = ImmutableMap.of(propSetMethodMap);
            unmodifiableSetMethodMap.keySet();
            entityDeclaredPropSetMethodPool.put(cls, unmodifiableSetMethodMap);

            if (entityPropSetMethodPool.get(cls) == null) {
                Map<String, Method> tmp = new ObjectPool<>(N.max(64, propSetMethodMap.size()));
                tmp.putAll(propSetMethodMap);
                entityPropSetMethodPool.put(cls, tmp);
            } else {
                entityPropSetMethodPool.get(cls).putAll(propSetMethodMap);
            }

            final List<String> propNameList = new ArrayList<>(propFieldMap.keySet());

            for (String propName : propGetMethodMap.keySet()) {
                if (!propNameList.contains(propName)) {
                    propNameList.add(propName);
                }
            }

            entityDeclaredPropNameListPool.put(cls, ImmutableList.of(propNameList));

            if (builderClass != null) {
                String propName = null;

                final Map<String, Method> builderPropSetMethodMap = new LinkedHashMap<>();

                for (Method method : builderClass.getMethods()) {
                    if (Modifier.isPublic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass()) && method.getParameterCount() == 1
                            && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(builderClass))) {
                        propName = getPropNameByMethod(method);
                        builderPropSetMethodMap.put(propName, method);
                    }

                    final ImmutableMap<String, Method> unmodifiableBuilderPropSetMethodMap = ImmutableMap.of(builderPropSetMethodMap);
                    unmodifiableBuilderPropSetMethodMap.keySet();
                    entityDeclaredPropSetMethodPool.put(builderClass, unmodifiableBuilderPropSetMethodMap);

                    Map<String, Method> tmp = new ObjectPool<>(N.max(64, builderPropSetMethodMap.size()));
                    tmp.putAll(builderPropSetMethodMap);
                    entityPropSetMethodPool.put(builderClass, tmp);
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

        String classFilePath = (pkgName == null) ? srcPath : (srcPath + pkgName.replace('.', File.separatorChar) + File.separator);
        File classFileFolder = new File(classFilePath);

        if (!classFileFolder.exists()) {
            classFileFolder.mkdirs();
        }

        return classFilePath;
    }

    /**
     * Package name 2 file path.
     *
     * @param pkgName
     * @return
     */
    private static String packageName2FilePath(String pkgName) {
        String pkgPath = pkgName.replace('.', '/');
        return pkgPath.endsWith("/") ? pkgPath : (pkgPath + "/");
    }

    /**
     *
     * @param accessibleObject
     * @param flag
     */
    @SuppressWarnings("deprecation")
    public static void setAccessible(final AccessibleObject accessibleObject, final boolean flag) {
        if (accessibleObject != null && accessibleObject.isAccessible() != flag) {
            accessibleObject.setAccessible(flag);
        }
    }

    /**
     *
     * @param accessibleObject
     * @param flag
     * @return {@code true} if no error happens, otherwise {@code false} is returned.
     */
    @SuppressWarnings("deprecation")
    public static boolean setAccessibleQuietly(final AccessibleObject accessibleObject, final boolean flag) {
        if (accessibleObject == null) {
            return false;
        }

        if (accessibleObject.isAccessible() == flag) {
            return true;
        }

        try {
            accessibleObject.setAccessible(flag);
        } catch (Exception e) {
            logger.warn("Failed to set accessible for : " + accessibleObject + " with flag: " + flag + " due to error: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * To camel case.
     *
     * @param props
     */
    public static void toCamelCase(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(ClassUtil.toCamelCase(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * It's designed for field/method/class/column/table names. and source and target Strings will be cached.
     *
     * @param propName
     * @return
     */
    public static String toCamelCase(final String propName) {
        String newPropName = camelCasePropNamePool.get(propName);

        if (newPropName == null) {
            newPropName = StringUtil.toCamelCase(propName);
            newPropName = NameUtil.getCachedName(newPropName);
            camelCasePropNamePool.put(propName, newPropName);
        }

        return newPropName;
    }

    /**
     * To lower case key with underscore.
     *
     * @param props
     */
    public static void toLowerCaseKeyWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(ClassUtil.toLowerCaseWithUnderscore(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    //    private static Class[] getTypeArguments(Class cls) {
    //        java.lang.reflect.Type[] typeArgs = null;
    //        java.lang.reflect.Type[] genericInterfaces = cls.getGenericInterfaces();
    //
    //        if (notNullOrEmpty(genericInterfaces)) {
    //            for (java.lang.reflect.Type type : genericInterfaces) {
    //                typeArgs = ((ParameterizedType) type).getActualTypeArguments();
    //
    //                if (notNullOrEmpty(typeArgs)) {
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
    //        if (notNullOrEmpty(typeArgs)) {
    //            Class[] clses = new Class[typeArgs.length];
    //
    //            for (int i = 0; i < typeArgs.length; i++) {
    //                clses[i] = (Class) typeArgs[i];
    //            }
    //
    //            return clses;
    //        } else {
    //            return null;
    //        }
    //    }

    //    /**
    //     * Returns the method declared in the specified {@code cls} with the specified method name.
    //     *
    //     * @param cls
    //     * @param methodName is case insensitive
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
     * It's designed for field/method/class/column/table names. and source and target Strings will be cached.
     *
     * @param str
     * @return
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        String result = lowerCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = StringUtil.toLowerCaseWithUnderscore(str);
            lowerCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * It's designed for field/method/class/column/table names. and source and target Strings will be cached.
     *
     * @param str
     * @return
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (N.isNullOrEmpty(str)) {
            return str;
        }

        String result = upperCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = StringUtil.toUpperCaseWithUnderscore(str);
            upperCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * To upper case key with underscore.
     *
     * @param props
     */
    public static void toUpperCaseKeyWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (Map.Entry<String, Object> entry : props.entrySet()) {
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
        } catch (ClassNotFoundException e) {
            // ignore.
        }

        recordClass = cls;
    }

    private static final Map<Class<?>, Boolean> recordClassPool = new ObjectPool<>(POOL_SIZE);

    public static boolean isRecordClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        Boolean ret = recordClassPool.get(cls);

        if (ret == null) {
            ret = (recordClass != null && recordClass.isAssignableFrom(cls)) || cls.getAnnotation(Record.class) != null;
            recordClassPool.put(cls, ret);
        }

        return ret;
    }

    private static final Map<Class<?>, Boolean> anonymousClassMap = new ConcurrentHashMap<>();

    public static boolean isAnonymousClass(final Class<?> cls) {
        Boolean v = anonymousClassMap.get(cls);

        if (v == null) {
            v = cls.isAnonymousClass();
            anonymousClassMap.put(cls, v);
        }

        return v;
    }

    private static final Map<Class<?>, Boolean> memberClassMap = new ConcurrentHashMap<>();

    public static boolean isMemberClass(final Class<?> cls) {
        Boolean v = memberClassMap.get(cls);

        if (v == null) {
            v = cls.isMemberClass();
            memberClassMap.put(cls, v);
        }

        return v;
    }

    public static boolean isAnonymousOrMemeberClass(final Class<?> cls) {
        Boolean v = anonymousClassMap.get(cls);

        if (v == null) {
            v = cls.isAnonymousClass();
            anonymousClassMap.put(cls, v);
        }

        if (v.booleanValue() == false) {
            v = memberClassMap.get(cls);

            if (v == null) {
                v = cls.isMemberClass();
                memberClassMap.put(cls, v);
            }
        }

        return v;
    }

    private ClassUtil() {
        // singleton
    }
}
