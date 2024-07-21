/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.Tuple.Tuple8;
import com.landawn.abacus.util.Tuple.Tuple9;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

/**
 * A factory for creating Type objects.
 *
 * @author Haiyang Li
 * @since 0.8
 * @see com.landawn.abacus.util.TypeReference
 * @see com.landawn.abacus.util.TypeReference.TypeToken
 */
@SuppressWarnings({ "java:S1192", "java:S2160" })
public final class TypeFactory {

    private static final Logger logger = LoggerFactory.getLogger(TypeFactory.class);

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<Class<?>, Type<?>> classTypePool = new ObjectPool<>(POOL_SIZE);

    static final Map<String, Type<?>> typePool = new ObjectPool<>(POOL_SIZE);

    static {
        // initializing built-in types

        // String pkgName = Type.class.getPackage().getName();
        // List<Class<?>> classes = PackageUtil.getClassesByPackage(pkgName, true, false);

        // for Android.
        final List<Class<?>> classes = new ArrayList<>();
        {
            classes.add(com.landawn.abacus.type.AbstractArrayType.class);
            classes.add(com.landawn.abacus.type.AbstractAtomicType.class);
            classes.add(com.landawn.abacus.type.AbstractBooleanType.class);
            classes.add(com.landawn.abacus.type.AbstractByteType.class);
            classes.add(com.landawn.abacus.type.AbstractCalendarType.class);
            classes.add(com.landawn.abacus.type.AbstractCharacterType.class);
            classes.add(com.landawn.abacus.type.AbstractDateType.class);
            classes.add(com.landawn.abacus.type.AbstractDoubleType.class);
            classes.add(com.landawn.abacus.type.AbstractFloatType.class);
            classes.add(com.landawn.abacus.type.AbstractIntegerType.class);
            classes.add(com.landawn.abacus.type.AbstractLongType.class);
            classes.add(com.landawn.abacus.type.AbstractPrimaryType.class);
            classes.add(com.landawn.abacus.type.AbstractPrimitiveArrayType.class);
            classes.add(com.landawn.abacus.type.AbstractPrimitiveListType.class);
            classes.add(com.landawn.abacus.type.AbstractShortType.class);
            classes.add(com.landawn.abacus.type.AbstractStringType.class);
            classes.add(com.landawn.abacus.type.AbstractType.class);
            classes.add(com.landawn.abacus.type.AsciiStreamType.class);
            classes.add(com.landawn.abacus.type.AtomicBooleanType.class);
            classes.add(com.landawn.abacus.type.AtomicIntegerType.class);
            classes.add(com.landawn.abacus.type.AtomicLongType.class);
            classes.add(com.landawn.abacus.type.Base64EncodedType.class);
            classes.add(com.landawn.abacus.type.BigDecimalType.class);
            classes.add(com.landawn.abacus.type.BigIntegerType.class);
            classes.add(com.landawn.abacus.type.BinaryStreamType.class);
            classes.add(com.landawn.abacus.type.BlobInputStreamType.class);
            classes.add(com.landawn.abacus.type.BlobType.class);
            classes.add(com.landawn.abacus.type.BooleanArrayType.class);
            classes.add(com.landawn.abacus.type.BooleanType.class);
            classes.add(com.landawn.abacus.type.ByteArrayType.class);
            classes.add(com.landawn.abacus.type.ByteBufferType.class);
            classes.add(com.landawn.abacus.type.BytesType.class);
            classes.add(com.landawn.abacus.type.ByteType.class);
            classes.add(com.landawn.abacus.type.CalendarType.class);
            classes.add(com.landawn.abacus.type.CharacterArrayType.class);
            classes.add(com.landawn.abacus.type.CharacterStreamType.class);
            classes.add(com.landawn.abacus.type.CharacterType.class);
            classes.add(com.landawn.abacus.type.ClazzType.class);
            classes.add(com.landawn.abacus.type.ClobAsciiStreamType.class);
            classes.add(com.landawn.abacus.type.ClobReaderType.class);
            classes.add(com.landawn.abacus.type.ClobType.class);
            classes.add(com.landawn.abacus.type.CollectionType.class);
            classes.add(com.landawn.abacus.type.CurrencyType.class);
            classes.add(com.landawn.abacus.type.DataSetType.class);
            classes.add(com.landawn.abacus.type.SheetType.class);
            classes.add(com.landawn.abacus.type.DateType.class);
            classes.add(com.landawn.abacus.type.DoubleArrayType.class);
            classes.add(com.landawn.abacus.type.DoubleType.class);
            classes.add(com.landawn.abacus.type.DurationType.class);
            classes.add(com.landawn.abacus.type.EntityIdType.class);
            classes.add(com.landawn.abacus.type.BeanType.class);
            classes.add(com.landawn.abacus.type.EnumType.class);
            classes.add(com.landawn.abacus.type.FloatArrayType.class);
            classes.add(com.landawn.abacus.type.FloatType.class);
            classes.add(com.landawn.abacus.type.FractionType.class);
            classes.add(com.landawn.abacus.type.GregorianCalendarType.class);
            classes.add(com.landawn.abacus.type.HBaseColumnType.class);
            classes.add(com.landawn.abacus.type.ImmutableListType.class);
            classes.add(com.landawn.abacus.type.ImmutableSetType.class);
            classes.add(com.landawn.abacus.type.InputStreamType.class);
            classes.add(com.landawn.abacus.type.IntegerArrayType.class);
            classes.add(com.landawn.abacus.type.IntegerType.class);
            classes.add(com.landawn.abacus.type.JSONType.class);
            classes.add(com.landawn.abacus.type.JUDateType.class);
            classes.add(com.landawn.abacus.type.LongArrayType.class);
            classes.add(com.landawn.abacus.type.LongType.class);
            classes.add(com.landawn.abacus.type.MapEntityType.class);
            classes.add(com.landawn.abacus.type.MapType.class);
            classes.add(com.landawn.abacus.type.ImmutableMapEntryType.class);
            classes.add(com.landawn.abacus.type.MapEntryType.class);
            classes.add(com.landawn.abacus.type.PairType.class);
            classes.add(com.landawn.abacus.type.Tuple1Type.class);
            classes.add(com.landawn.abacus.type.Tuple2Type.class);
            classes.add(com.landawn.abacus.type.Tuple3Type.class);
            classes.add(com.landawn.abacus.type.Tuple4Type.class);
            classes.add(com.landawn.abacus.type.Tuple5Type.class);
            classes.add(com.landawn.abacus.type.Tuple6Type.class);
            classes.add(com.landawn.abacus.type.Tuple7Type.class);
            classes.add(com.landawn.abacus.type.Tuple8Type.class);
            classes.add(com.landawn.abacus.type.Tuple9Type.class);
            classes.add(com.landawn.abacus.type.IndexedType.class);
            classes.add(com.landawn.abacus.type.TimedType.class);
            classes.add(com.landawn.abacus.type.MillisCalendarType.class);
            classes.add(com.landawn.abacus.type.MillisDateType.class);
            classes.add(com.landawn.abacus.type.MillisTimestampType.class);
            classes.add(com.landawn.abacus.type.MillisTimeType.class);
            classes.add(com.landawn.abacus.type.MutableBooleanType.class);
            classes.add(com.landawn.abacus.type.MutableCharType.class);
            classes.add(com.landawn.abacus.type.MutableByteType.class);
            classes.add(com.landawn.abacus.type.MutableShortType.class);
            classes.add(com.landawn.abacus.type.MutableIntType.class);
            classes.add(com.landawn.abacus.type.MutableLongType.class);
            classes.add(com.landawn.abacus.type.MutableFloatType.class);
            classes.add(com.landawn.abacus.type.MutableDoubleType.class);
            classes.add(com.landawn.abacus.type.NCharacterStreamType.class);
            classes.add(com.landawn.abacus.type.NClobReaderType.class);
            classes.add(com.landawn.abacus.type.NClobType.class);
            classes.add(com.landawn.abacus.type.NStringType.class);
            classes.add(com.landawn.abacus.type.NumberType.class);
            classes.add(com.landawn.abacus.type.ObjectArrayType.class);
            classes.add(com.landawn.abacus.type.ObjectType.class);
            classes.add(com.landawn.abacus.type.JdkOptionalIntType.class);
            classes.add(com.landawn.abacus.type.JdkOptionalLongType.class);
            classes.add(com.landawn.abacus.type.JdkOptionalDoubleType.class);
            classes.add(com.landawn.abacus.type.JdkOptionalType.class);
            classes.add(com.landawn.abacus.type.OptionalBooleanType.class);
            classes.add(com.landawn.abacus.type.OptionalCharType.class);
            classes.add(com.landawn.abacus.type.OptionalByteType.class);
            classes.add(com.landawn.abacus.type.OptionalShortType.class);
            classes.add(com.landawn.abacus.type.OptionalIntType.class);
            classes.add(com.landawn.abacus.type.OptionalLongType.class);
            classes.add(com.landawn.abacus.type.OptionalFloatType.class);
            classes.add(com.landawn.abacus.type.OptionalDoubleType.class);
            classes.add(com.landawn.abacus.type.OptionalType.class);
            classes.add(com.landawn.abacus.type.NullableType.class);
            classes.add(com.landawn.abacus.type.PasswordType.class);
            classes.add(com.landawn.abacus.type.PatternType.class);
            classes.add(com.landawn.abacus.type.PrimitiveBooleanArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveBooleanListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveBooleanType.class);
            classes.add(com.landawn.abacus.type.PrimitiveByteArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveByteListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveByteType.class);
            classes.add(com.landawn.abacus.type.PrimitiveCharArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveCharListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveCharType.class);
            classes.add(com.landawn.abacus.type.PrimitiveDoubleArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveDoubleListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveDoubleType.class);
            classes.add(com.landawn.abacus.type.PrimitiveFloatArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveFloatListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveFloatType.class);
            classes.add(com.landawn.abacus.type.PrimitiveIntArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveIntListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveIntType.class);
            classes.add(com.landawn.abacus.type.PrimitiveLongArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveLongListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveLongType.class);
            classes.add(com.landawn.abacus.type.PrimitiveShortArrayType.class);
            classes.add(com.landawn.abacus.type.PrimitiveShortListType.class);
            classes.add(com.landawn.abacus.type.PrimitiveShortType.class);
            // classes.add(com.landawn.abacus.type.RangeType.class);
            classes.add(com.landawn.abacus.type.ReaderType.class);
            classes.add(com.landawn.abacus.type.RefType.class);
            classes.add(com.landawn.abacus.type.RowIdType.class);
            classes.add(com.landawn.abacus.type.ShortArrayType.class);
            classes.add(com.landawn.abacus.type.ShortType.class);
            classes.add(com.landawn.abacus.type.SQLArrayType.class);
            classes.add(com.landawn.abacus.type.SQLXMLType.class);
            classes.add(com.landawn.abacus.type.StringType.class);
            classes.add(com.landawn.abacus.type.StringBuilderType.class);
            classes.add(com.landawn.abacus.type.StringBufferType.class);
            classes.add(com.landawn.abacus.type.TimestampType.class);
            classes.add(com.landawn.abacus.type.TimeType.class);
            classes.add(com.landawn.abacus.type.Type.SerializationType.class);
            classes.add(com.landawn.abacus.type.Type.class);
            classes.add(com.landawn.abacus.type.TypeType.class);
            classes.add(com.landawn.abacus.type.URIType.class);
            classes.add(com.landawn.abacus.type.URLType.class);
            classes.add(com.landawn.abacus.type.UUIDType.class);
            classes.add(com.landawn.abacus.type.XMLGregorianCalendarType.class);
            classes.add(com.landawn.abacus.type.XMLType.class);
            classes.add(com.landawn.abacus.type.MultisetType.class);
            classes.add(com.landawn.abacus.type.SetMultimapType.class);
            classes.add(com.landawn.abacus.type.MultimapType.class);

            classes.add(com.landawn.abacus.type.BooleanCharType.class);
        }

        try {
            if (Class.forName("java.time.ZonedDateTime") != null) {
                classes.add(com.landawn.abacus.type.InstantType.class);
                classes.add(com.landawn.abacus.type.ZonedDateTimeType.class);
                classes.add(com.landawn.abacus.type.LocalDateType.class);
                classes.add(com.landawn.abacus.type.LocalTimeType.class);
                classes.add(com.landawn.abacus.type.LocalDateTimeType.class);
            }
        } catch (Throwable e) {
            // ignore.
        }

        // initialize external types
        {
            try {
                if (Class.forName("org.bson.types.ObjectId") != null) {
                    classes.add(com.landawn.abacus.type.BSONObjectIdType.class);
                }
            } catch (Throwable e) {
                // ignore.
            }

            try {
                if (Class.forName("org.joda.time.DateTime") != null) {
                    classes.add(com.landawn.abacus.type.JodaInstantType.class);
                    classes.add(com.landawn.abacus.type.JodaDateTimeType.class);
                    classes.add(com.landawn.abacus.type.JodaMutableDateTimeType.class);
                }
            } catch (Throwable e) {
                // ignore.
            }

            try {
                if (Class.forName("android.net.Uri") != null) {
                    classes.add(Class.forName("com.landawn.abacus.type.AndroidUriType"));
                }
            } catch (Throwable e) {
                // ignore.
            }
        }

        final List<Class<?>> delayInitializedTypeClasses = new ArrayList<>();

        for (Class<?> cls : classes) {
            int mod = cls.getModifiers();

            if (Type.class.isAssignableFrom(cls) && !Modifier.isAbstract(mod) && (ClassUtil.getDeclaredConstructor(cls) != null)) {
                if (AbstractPrimitiveListType.class.isAssignableFrom(cls)
                        || AbstractArrayType.class.isAssignableFrom(cls) /* || RangeType.class.equals(cls) */) {
                    delayInitializedTypeClasses.add(cls);

                    continue;
                }

                try {
                    Type<?> type = (Type<?>) cls.getDeclaredConstructor().newInstance();
                    typePool.put(type.name(), type);

                    if (!(type.clazz().equals(String.class) || type.clazz().equals(InputStream.class) || type.clazz().equals(Reader.class)
                            || type instanceof MillisCalendarType || type instanceof MillisDateType || type instanceof MillisTimeType
                            || type instanceof MillisTimestampType || type instanceof BytesType || type instanceof BooleanCharType)
                            || (StringType.class.equals(type.getClass()) || InputStreamType.class.equals(type.getClass())
                                    || CharacterStreamType.class.equals(type.getClass()))) {
                        if (!(type instanceof JUDateType || type instanceof JdkOptionalIntType || type instanceof JdkOptionalLongType
                                || type instanceof JdkOptionalDoubleType || type instanceof JdkOptionalType)) { // conflict with DateType.
                            typePool.put(type.clazz().getSimpleName(), type);
                        }

                        typePool.put(type.clazz().getCanonicalName(), type);
                    }
                } catch (Throwable e) {
                    if (logger.isInfoEnabled()) {
                        logger.info(getClassName(cls) + " is not initilized as built-in type.");
                    }
                }
            }
        }

        for (Class<?> cls : delayInitializedTypeClasses) {
            try {
                Type<?> type = (Type<?>) cls.getDeclaredConstructor().newInstance();

                typePool.put(type.name(), type);

                typePool.put(type.clazz().getSimpleName(), type);

                typePool.put(type.clazz().getCanonicalName(), type);
            } catch (Throwable e) {
                if (logger.isInfoEnabled()) {
                    logger.info(getClassName(cls) + " is not initilized as built-in type.");
                }
            }
        }

        // special cases:
        typePool.put(PrimitiveBooleanType.BOOL, typePool.get(PrimitiveBooleanType.BOOLEAN));

        Type<?> typeType = typePool.get(TypeType.TYPE);

        for (Type<?> type : N.newHashSet(typePool.values())) {
            typePool.put(type.getClass().getSimpleName(), typeType);
            typePool.put(type.getClass().getCanonicalName(), typeType);
        }

        final Set<Class<?>> builtinType = N.asSet(StringType.class, PrimitiveByteArrayType.class, DateType.class, TimeType.class, TimestampType.class,
                CalendarType.class, BooleanType.class, ReaderType.class, InputStreamType.class);
        final Multiset<Class<?>> typeClassMultiset = N.newMultiset(typePool.size());

        for (Type<?> type : typePool.values()) {
            typeClassMultiset.add(type.clazz());
        }

        for (Type<?> type : typePool.values()) {
            if (typeClassMultiset.getCount(type.clazz()) > 1 && !builtinType.contains(type.getClass())) {
                if (type.getClass().getPackage() == null || !type.getClass().getPackageName().startsWith("com.landawn.abacus.type")) {
                    logger.info("More than one types are defined for class: " + getClassName(type.clazz()) + ". Ignore type: " + type.name());
                }

                continue;
            }

            if (type.isGenericType()) {
                continue;
            }

            classTypePool.put(type.clazz(), type);
        }
    }

    private static final Map<java.lang.reflect.Type, Type<?>> type2TypeCache = new ConcurrentHashMap<>();

    /**
     * Gets the class name.
     *
     * @param cls
     * @return
     */
    static String getClassName(Class<?> cls) {
        String clsName = ClassUtil.getCanonicalClassName(cls);

        if (Strings.isEmpty(clsName)) {
            clsName = cls.getName();
        }

        return clsName;
    }

    /**
     * Gets the type.
     *
     * @param <T>
     * @param cls
     * @param typeName
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> Type<T> getType(Class cls, String typeName) {
        if (Strings.isEmpty(typeName)) {
            typeName = getClassName(cls);
        }

        Type type = typePool.get(typeName);

        if (type == null) {
            TypeAttrParser attrResult = TypeAttrParser.parse(typeName);
            String[] typeParameters = attrResult.getTypeParameters();
            String[] parameters = attrResult.getParameters();
            String clsName = attrResult.getClassName();

            if (clsName.equalsIgnoreCase(ClazzType.CLAZZ)) {
                if (typeParameters.length != 1) {
                    throw new IllegalArgumentException("IncorrecT type parameters: " + typeName + ". Clazz Type can only have one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Clazz Type can only have zero parameter.");
                }

                type = new ClazzType(typeParameters[0]);
            } else if (clsName.equalsIgnoreCase(TypeType.TYPE)) {
                type = new TypeType(typeName);
            } else if (clsName.equalsIgnoreCase(JSONType.JSON)) {
                if (typeParameters.length > 1) {
                    throw new IllegalArgumentException("IncorrecT type parameters: " + typeName + ". JSON Type can only have one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". JSON Type can only have zero parameter.");
                }

                if (typeParameters.length == 0) {
                    type = new JSONType(Map.class.getSimpleName());
                } else {
                    type = new JSONType(typeParameters[0]);
                }
            } else if (clsName.equalsIgnoreCase(XMLType.XML)) {
                if (typeParameters.length > 1) {
                    throw new IllegalArgumentException("IncorrecT type parameters: " + typeName + ". JSON Type can only have one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". JSON Type can only have zero parameter.");
                }

                if (typeParameters.length == 0) {
                    type = new XMLType(Map.class.getSimpleName());
                } else {
                    type = new XMLType(typeParameters[0]);
                }
            } else {
                if (cls == null) {
                    try {
                        cls = ClassUtil.forClass(clsName);
                    } catch (Throwable e) {
                        // ignore.
                    }
                }

                if (cls == null) {
                    type = new ObjectType<>(typeName, Object.class);
                } else if (java.util.Date.class.isAssignableFrom(cls)) {
                    if (Date.class.isAssignableFrom(cls)) {
                        type = getType(DateType.DATE);
                    } else if (Time.class.isAssignableFrom(cls)) {
                        type = getType(TimeType.TIME);
                    } else if (Timestamp.class.isAssignableFrom(cls)) {
                        type = getType(TimestampType.TIMESTAMP);
                    } else {
                        type = getType(JUDateType.JU_DATE);
                    }
                } else if (Calendar.class.isAssignableFrom(cls)) {
                    type = getType(CalendarType.CALENDAR);
                } else if (XMLGregorianCalendar.class.isAssignableFrom(cls)) {
                    type = getType(XMLGregorianCalendarType.XML_GREGORIAN_CALENDAR);
                } else if (Reader.class.isAssignableFrom(cls)) {
                    type = new ReaderType(cls);
                } else if (InputStream.class.isAssignableFrom(cls)) {
                    type = new InputStreamType(cls);
                } else if (ByteBuffer.class.isAssignableFrom(cls)) {
                    type = new ByteBufferType(cls);
                } else if (cls.isEnum() || Enum.class.isAssignableFrom(cls)) {
                    if (parameters.length == 0) {
                        type = new EnumType(clsName);
                    } else if (parameters.length == 1) {
                        type = new EnumType(clsName, Boolean.parseBoolean(parameters[0]));
                    } else {
                        throw new IllegalArgumentException("Not supported paramters " + typeName + " for EnumType.");
                    }
                } else if (java.util.Optional.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Optional has one and only has one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Optional Type can only have zero parameter.");
                    }

                    type = new JdkOptionalType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Optional.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Optional has one and only has one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Optional Type can only have zero parameter.");
                    }

                    type = new OptionalType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Nullable.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Nullable has one and only has one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Nullable Type can only have zero parameter.");
                    }

                    type = new NullableType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Multiset.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". Multiset Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Multiset Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MultisetType(ObjectType.OBJECT);
                    } else {
                        type = new MultisetType(typeParameters[0]);
                    }

                } else if (ListMultimap.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". ListMultimap Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ListMultimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ListMultimapType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new ListMultimapType(typeParameters[0], typeParameters[1]);
                    }
                } else if (SetMultimap.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". SetMultimap Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". SetMultimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new SetMultimapType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new SetMultimapType(typeParameters[0], typeParameters[1]);
                    }
                } else if (Multimap.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". Multimap Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Multimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MultimapType(ObjectType.OBJECT, "List<Object>");
                    } else {
                        type = new MultimapType(typeParameters[0], typeParameters[1]);
                    }
                } else if (Range.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("IncorrecT type parameters: " + typeName + ". Range Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Range Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new RangeType(ObjectType.OBJECT);
                    } else {
                        type = new RangeType(typeParameters[0]);
                    }
                } else if (EntityId.class.isAssignableFrom(cls)) {
                    type = getType(EntityIdType.ENTITY_ID);
                } else if (DataSet.class.isAssignableFrom(cls)) {
                    type = getType(DataSetType.DATA_SET);
                } else if (Sheet.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 3) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". Sheet Type can only have zero or Three type parameters.");
                    }

                    if (typeParameters.length == 3) {
                        return new SheetType(typeParameters[0], typeParameters[1], typeParameters[2]);
                    }
                    return new SheetType(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                } else if (HBaseColumn.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("IncorrecT type parameters: " + typeName + ". HBaseColumn Type can only have one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". HBaseColumn Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new HBaseColumnType(cls, ObjectType.OBJECT);
                    } else {
                        type = new HBaseColumnType(cls, typeParameters[0]);
                    }
                } else if (ImmutableList.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". ImmutableList Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ImmutableList Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ImmutableListType(ObjectType.OBJECT);
                    } else {
                        type = new ImmutableListType(typeParameters[0]);
                    }
                } else if (ImmutableSet.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". ImmutableSet Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ImmutableSet Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ImmutableSetType(ObjectType.OBJECT);
                    } else {
                        type = new ImmutableSetType(typeParameters[0]);
                    }
                } else if (Collection.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "IncorrecT type parameters: " + typeName + ". Collection Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Collection Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new CollectionType(cls, ObjectType.OBJECT);
                    } else {
                        type = new CollectionType(cls, typeParameters[0]);
                    }
                } else if (Map.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Map Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Map Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MapType(cls, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new MapType(cls, typeParameters[0], typeParameters[1]);
                    }
                } else if (Pair.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Pair Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Pair Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new PairType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new PairType(typeParameters[0], typeParameters[1]);
                    }
                } else if (Triple.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 3) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Triple Type can only have zero or three type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Triple Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new TripleType(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new TripleType(typeParameters[0], typeParameters[1], typeParameters[2]);
                    }
                } else if (Tuple1.class.equals(cls)) {
                    if ((typeParameters.length != 1) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple1 Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple1 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple1Type(ObjectType.OBJECT);
                    } else {
                        type = new Tuple1Type(typeParameters[0]);
                    }
                } else if (Tuple2.class.equals(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple2 Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple2 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple2Type(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple2Type(typeParameters[0], typeParameters[1]);
                    }
                } else if (Tuple3.class.equals(cls)) {
                    if ((typeParameters.length != 3) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple3 Type can only have zero or three type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple3 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple3Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple3Type(typeParameters[0], typeParameters[1], typeParameters[2]);
                    }
                } else if (Tuple4.class.equals(cls)) {
                    if ((typeParameters.length != 4) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple4 Type can only have zero or four type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple4 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple4Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple4Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3]);
                    }
                } else if (Tuple5.class.equals(cls)) {
                    if ((typeParameters.length != 5) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple5 Type can only have zero or five type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple5 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple5Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple5Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3], typeParameters[4]);
                    }
                } else if (Tuple6.class.equals(cls)) {
                    if ((typeParameters.length != 6) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple6 Type can only have zero or six type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple6 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple6Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple6Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3], typeParameters[4], typeParameters[5]);
                    }
                } else if (Tuple7.class.equals(cls)) {
                    if ((typeParameters.length != 7) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple7 Type can only have zero or seven type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple7 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple7Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT,
                                ObjectType.OBJECT);
                    } else {
                        type = new Tuple7Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3], typeParameters[4], typeParameters[5],
                                typeParameters[6]);
                    }
                } else if (Tuple8.class.equals(cls)) {
                    if ((typeParameters.length != 8) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple8 Type can only have zero or eight type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple8 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple8Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT,
                                ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple8Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3], typeParameters[4], typeParameters[5],
                                typeParameters[6], typeParameters[7]);
                    }
                } else if (Tuple9.class.equals(cls)) {
                    if ((typeParameters.length != 9) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Tuple9 Type can only have zero or nine type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Tuple9 Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new Tuple9Type(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT,
                                ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new Tuple9Type(typeParameters[0], typeParameters[1], typeParameters[2], typeParameters[3], typeParameters[4], typeParameters[5],
                                typeParameters[6], typeParameters[7], typeParameters[8]);
                    }
                } else if (Indexed.class.equals(cls)) {
                    if ((typeParameters.length != 1) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Indexed Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Indexed Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new IndexedType(ObjectType.OBJECT);
                    } else {
                        type = new IndexedType(typeParameters[0]);
                    }
                } else if (Timed.class.equals(cls)) {
                    if ((typeParameters.length != 1) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Timed Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Timed Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new TimedType(ObjectType.OBJECT);
                    } else {
                        type = new TimedType(typeParameters[0]);
                    }
                } else if (AbstractMap.SimpleImmutableEntry.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Map.ImmutableEntry Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Map.ImmutableEntry Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ImmutableMapEntryType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new ImmutableMapEntryType(typeParameters[0], typeParameters[1]);
                    }
                } else if (Map.Entry.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Map.Entry Type can only have zero or two type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Map.Entry Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MapEntryType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new MapEntryType(typeParameters[0], typeParameters[1]);
                    }
                } else if (ClassUtil.isBeanClass(cls)) {
                    type = new BeanType(cls);
                } else if (Type.class.isAssignableFrom(cls)) {
                    type = (Type) TypeAttrParser.newInstance(cls, typeName);
                } else if (Object[].class.isAssignableFrom(cls)) {
                    type = new ObjectArrayType(cls);
                } else {
                    Type<?> val = null;

                    for (Map.Entry<String, Type<?>> entry : typePool.entrySet()) {
                        val = entry.getValue();

                        if (!(val.isObjectType() || val.clazz().equals(Object[].class)) && val.clazz().isAssignableFrom(cls)) {
                            try {
                                if ((val.isGenericType() || N.notEmpty(typeParameters) || N.notEmpty(parameters)) && Strings.isNotEmpty(typeName)) {
                                    final Constructor<? extends Type> constructor = ClassUtil.getDeclaredConstructor(val.getClass(), String.class);

                                    if (constructor != null) {
                                        ClassUtil.setAccessibleQuietly(constructor, true);
                                        type = ClassUtil.invokeConstructor(constructor, typeName);
                                    } else {
                                        logger.warn(getClassName(val.getClass()) + "(String typeName) {...} should be defined");
                                    }
                                } else {
                                    final Constructor<? extends Type> constructor = ClassUtil.getDeclaredConstructor(val.getClass(), Class.class);

                                    if (constructor != null) {
                                        ClassUtil.setAccessibleQuietly(constructor, true);
                                        type = ClassUtil.invokeConstructor(constructor, cls);
                                    }
                                }
                            } catch (Throwable e) {
                                // ignore.
                                // type = val;
                            }
                        }

                        if (type != null) {
                            break;
                        }
                    }

                    if (type == null) {
                        type = Strings.isEmpty(typeName) ? new ObjectType<>(cls) : new ObjectType<>(typeName, cls);
                    }
                }
            }

            if (typeName.endsWith("[]") && !type.isArray()) {
                type = new ObjectArrayType(type);
            }

            typePool.put(typeName, type);

            if (typePool.size() % 100 == 0) {
                logger.warn("Size of type pool reaches: " + typePool.size() + " with initialized pool size: " + POOL_SIZE);
            }
        }

        return type;
    }

    /**
     * Gets the type.
     *
     * @param <T>
     * @param clazzes
     * @return
     * @deprecated please using {@code Type#ofAll(Class...)}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @SafeVarargs
    static <T> List<Type<T>> getType(final Class<? extends T>... clazzes) {
        if (N.isEmpty(clazzes)) {
            return new ArrayList<>();
        }

        final List<Type<T>> result = new ArrayList<>(clazzes.length);

        Class<?> cls = null;
        for (Class<? extends T> element : clazzes) {
            cls = element;

            result.add(cls == null ? null : (Type<T>) getType(cls));
        }

        return result;
    }

    /**
     * Gets the type.
     *
     * @param <T>
     * @param cls
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Type<T> getType(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, "cls");

        Type type = classTypePool.get(cls);

        if (type == null) {
            type = getType(cls, getClassName(cls));

            if (type != null) {
                classTypePool.put(cls, type);
            }
        }

        return type;
    }

    /**
     * Gets the type.
     *
     * @param <T>
     * @param classes
     * @return
     * @deprecated please using {@code Type#ofAll(Collection)}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    static <T> List<Type<T>> getType(final Collection<Class<? extends T>> classes) {
        final List<Type<T>> result = new ArrayList<>(classes.size());

        for (Class<?> cls : classes) {
            result.add(cls == null ? null : (Type<T>) getType(cls));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Type<T> getType(final java.lang.reflect.Type type) {
        Type result = type2TypeCache.get(type);

        if (result == null) {
            if (type instanceof ParameterizedType) {
                result = getType(ClassUtil.getTypeName(type));
            } else if (type instanceof Class) {
                result = getType((Class) type);
            } else {
                result = getType(ClassUtil.getTypeName(type));
            }

            type2TypeCache.put(type, result);
        }

        return result;
    }

    /**
     * Gets the type.
     *
     * @param <T>
     * @param typeName
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Type<T> getType(String typeName) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, "typeName");

        return getType(null, typeName);
    }

    /**
     *
     *
     * @param <T>
     * @param targetClass
     * @param toStringFunc
     * @param fromStringFunc
     * @throws IllegalArgumentException
     */
    public static <T> void registerType(final Class<T> targetClass, final BiFunction<? super T, JSONParser, String> toStringFunc,
            final BiFunction<? super String, JSONParser, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(toStringFunc, "toStringFunc");
        N.checkArgNotNull(fromStringFunc, "fromStringFunc");

        registerType(targetClass, new AbstractType<T>(getClassName(targetClass)) {
            @Override
            public Class<T> clazz() {
                return targetClass;
            }

            @Override
            public String stringOf(T x) {
                return toStringFunc.apply(x, Utils.jsonParser);
            }

            @Override
            public T valueOf(String str) {
                return fromStringFunc.apply(str, Utils.jsonParser);
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param cls
     * @param toStringFunc
     * @param fromStringFunc
     * @throws IllegalArgumentException
     */
    public static <T> void registerType(final Class<T> cls, final Function<? super T, String> toStringFunc, final Function<? super String, T> fromStringFunc)
            throws IllegalArgumentException {
        N.checkArgNotNull(cls, "cls");
        N.checkArgNotNull(toStringFunc, "toStringFunc");
        N.checkArgNotNull(fromStringFunc, "fromStringFunc");

        registerType(cls, new AbstractType<T>(getClassName(cls)) {
            @Override
            public Class<T> clazz() {
                return cls;
            }

            @Override
            public String stringOf(T x) {
                return toStringFunc.apply(x);
            }

            @Override
            public T valueOf(String str) {
                return fromStringFunc.apply(str);
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param cls
     * @param type
     * @throws IllegalArgumentException
     */
    public static <T> void registerType(final Class<T> cls, final Type<T> type) throws IllegalArgumentException {
        N.checkArgNotNull(cls, "cls");
        N.checkArgNotNull(type, "type");

        if (classTypePool.containsKey(cls)) {
            throw new IllegalArgumentException("A type has already registered with class: " + cls);
        }

        registerType(type);

        classTypePool.put(cls, type);
    }

    /**
     *
     *
     * @param <T>
     * @param typeName
     * @param targetClass
     * @param toStringFunc
     * @param fromStringFunc
     * @throws IllegalArgumentException
     */
    public static <T> void registerType(final String typeName, final Class<T> targetClass, final BiFunction<? super T, JSONParser, String> toStringFunc,
            final BiFunction<? super String, JSONParser, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, "typeName");
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(toStringFunc, "toStringFunc");
        N.checkArgNotNull(fromStringFunc, "fromStringFunc");

        final Type<T> type = new AbstractType<>(typeName) {
            @Override
            public Class<T> clazz() {
                return targetClass;
            }

            @Override
            public String stringOf(T x) {
                return toStringFunc.apply(x, Utils.jsonParser);
            }

            @Override
            public T valueOf(String str) {
                return fromStringFunc.apply(str, Utils.jsonParser);
            }
        };

        registerType(typeName, type);

        if (!classTypePool.containsKey(targetClass)) {
            classTypePool.put(targetClass, type);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param typeName
     * @param targetClass
     * @param toStringFunc
     * @param fromStringFunc
     * @throws IllegalArgumentException
     */
    public static <T> void registerType(final String typeName, final Class<T> targetClass, final Function<? super T, String> toStringFunc,
            final Function<? super String, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, "typeName");
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(toStringFunc, "toStringFunc");
        N.checkArgNotNull(fromStringFunc, "fromStringFunc");

        final Type<T> type = new AbstractType<>(typeName) {
            @Override
            public Class<T> clazz() {
                return targetClass;
            }

            @Override
            public String stringOf(T x) {
                return toStringFunc.apply(x);
            }

            @Override
            public T valueOf(String str) {
                return fromStringFunc.apply(str);
            }
        };

        registerType(typeName, type);

        if (!classTypePool.containsKey(targetClass)) {
            classTypePool.put(targetClass, type);
        }
    }

    /**
     *
     *
     * @param typeName
     * @param type
     * @throws IllegalArgumentException
     */
    public static void registerType(final String typeName, final Type<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, "typeName");
        N.checkArgNotNull(type, "type");

        if (typePool.containsKey(typeName)) {
            throw new IllegalArgumentException("A type has already registered with name: " + typeName);
        }

        registerType(type);

        typePool.put(typeName, type);
    }

    /**
     *
     *
     * @param type
     * @throws IllegalArgumentException
     */
    public static void registerType(final Type<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(type, "type");

        if (typePool.containsKey(type.name())) {
            throw new IllegalArgumentException("A type has already registered with name: " + type.name());
        }

        typePool.put(type.name(), type);

        //    if (!typePool.containsKey(getClassName(type.clazz()))) {
        //        typePool.put(getClassName(type.clazz()), type);
        //    }

        //    if (!classTypePool.containsKey(type.clazz())) {
        //        classTypePool.put(type.clazz(), type);
        //    }
    }

    private TypeFactory() {
        // no instance.
    }
}
