/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ConcurrentCacheMap;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
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
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

/**
 * A factory class for creating, registering, and retrieving Type objects in the abacus-common type system.
 * This final class serves as the central entry point for all type-related operations, providing
 * comprehensive type management including built-in type registration, custom type registration,
 * and efficient type lookup with caching mechanisms.
 *
 * <p>TypeFactory is the core component that bridges Java's reflection system with Abacus's type abstraction,
 * enabling type-safe serialization, deserialization, and data conversion operations across the entire framework.
 * It maintains a registry of all available types and provides factory methods for obtaining Type instances
 * from various sources including Class objects, type name strings, and java.lang.reflect.Type instances.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Registry:</b> Central registry for all built-in and custom Type implementations</li>
 *   <li><b>Performance Optimization:</b> Efficient caching mechanisms for Type instances</li>
 *   <li><b>Flexible Type Creation:</b> Support for creating types from classes, strings, and reflection types</li>
 *   <li><b>Custom Type Registration:</b> APIs for registering custom Type implementations</li>
 *   <li><b>Generic Type Support:</b> Full support for parameterized types and complex generic structures</li>
 *   <li><b>Built-in Type Library:</b> Comprehensive set of pre-registered types for common Java classes</li>
 *   <li><b>Thread Safety:</b> All operations are thread-safe with concurrent access support</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Type Registration:</b>
 * <ul>
 *   <li>Type names must be <b>unique</b> - duplicate registrations throw IllegalArgumentException</li>
 *   <li>Built-in types cannot be overridden: registering by class for {@code String}, {@code Object},
 *       {@code Number}, {@code Map}, {@code HashMap}, {@code List}, {@code java.util.Date},
 *       {@code java.util.Optional}, arrays, ... throws IllegalArgumentException whether or not the class has
 *       been looked up yet</li>
 *   <li>Any lookup caches a default type: once a class has been resolved by {@code getType(Class)}, bean
 *       introspection or serialization, {@code registerType(Class, ...)} for it throws IllegalArgumentException
 *       ("already registered") even though nothing was explicitly registered - register custom types before the
 *       class is first used</li>
 *   <li>Custom types are cached permanently and cannot be unregistered</li>
 *   <li>Type registration is thread-safe but should ideally be done during application startup</li>
 * </ul>
 *
 * <p><b>Common Use Cases:</b>
 * <ul>
 *   <li><b>Type Retrieval:</b> Getting Type instances for serialization/deserialization operations</li>
 *   <li><b>Custom Type Registration:</b> Registering custom types for domain-specific classes</li>
 *   <li><b>Generic Type Handling:</b> Working with parameterized types like List&lt;String&gt;, Map&lt;K,V&gt;</li>
 *   <li><b>Framework Integration:</b> Integrating with ORM, JSON libraries, and data conversion frameworks</li>
 *   <li><b>Configuration Management:</b> Type-safe configuration parsing and validation</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic type retrieval
 * Type<String> stringType = TypeFactory.getType(String.class);
 * Type<Integer> intType = TypeFactory.getType(Integer.class);
 * Type<List> listType = TypeFactory.getType(List.class);
 *
 * // Generic type retrieval using type names
 * Type<List<String>> listStringType = TypeFactory.getType("List<String>");
 * Type<Map<String, Integer>> mapType = TypeFactory.getType("Map<String, Integer>");
 * Type<Optional<Person>> optionalType = TypeFactory.getType("Optional<Person>");
 *
 * // Custom type registration with simple functions (the class must not already have a built-in/registered type)
 * TypeFactory.registerType(
 *     EmailAddress.class,
 *     email -> email.getValue(),                 // Serialization function
 *     str -> new EmailAddress(str)               // Deserialization function
 * );
 *
 * // Custom type registration with JsonParser support
 * TypeFactory.registerType(
 *     MyCustomClass.class,
 *     (obj, parser) -> obj.toJson(),                                // Serialization with parser
 *     (str, parser) -> parser.deserialize(str, MyCustomClass.class) // Deserialization with parser
 * );
 *
 * // Named type registration for specialized handling
 * TypeFactory.registerType(
 *     "ISODateTime",
 *     LocalDateTime.class,
 *     dt -> dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
 *     str -> LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
 * );
 *
 * // Using registered types
 * Type<LocalDate> dateType = TypeFactory.getType(LocalDate.class);
 * String serialized = dateType.stringOf(LocalDate.now());
 * LocalDate deserialized = dateType.valueOf(serialized);
 * }</pre>
 *
 * <p><b>Built-in Type Categories:</b>
 * <ul>
 *   <li><b>Primitive Types:</b> boolean, byte, char, short, int, long, float, double</li>
 *   <li><b>Wrapper Types:</b> Boolean, Byte, Character, Short, Integer, Long, Float, Double</li>
 *   <li><b>String Types:</b> String, StringBuilder, StringBuffer</li>
 *   <li><b>Date/Time Types:</b> Date, Time, Timestamp, Calendar, LocalDate, LocalTime, etc.</li>
 *   <li><b>Collection Types:</b> List, Set, Queue, Deque and their implementations</li>
 *   <li><b>Map Types:</b> Map, SortedMap, NavigableMap and their implementations</li>
 *   <li><b>Array Types:</b> All primitive and object array types</li>
 *   <li><b>Optional Types:</b> Optional, OptionalInt, OptionalLong, OptionalDouble, Nullable</li>
 *   <li><b>Utility Types:</b> Pair, Triple, Tuple types, Indexed, Timed</li>
 *   <li><b>Database Types:</b> Blob, Clob, Array, SQLXML, ResultSet parameter types</li>
 * </ul>
 *
 * <p><b>Type Name Formats:</b>
 * The factory supports various type name formats:
 * <ul>
 *   <li>Simple names: "String", "Integer", "List"</li>
 *   <li>Fully qualified names: "java.lang.String", "java.util.List"</li>
 *   <li>Generic types: "List&lt;String&gt;", "Map&lt;String,Integer&gt;"</li>
 *   <li>Nested generics: "List&lt;Map&lt;String,Integer&gt;&gt;"</li>
 *   <li>Array types: "String[]", "int[][]", "List&lt;String&gt;[]"</li>
 *   <li>Special types: "JSON&lt;Person&gt;", "XML&lt;Order&gt;"</li>
 * </ul>
 *
 * @see com.landawn.abacus.util.TypeReference
 * @see com.landawn.abacus.util.TypeReference.TypeToken
 */
@SuppressWarnings({ "java:S1192", "java:S2160" })
public final class TypeFactory {

    private static final Logger logger = LoggerFactory.getLogger(TypeFactory.class);

    private static final Set<String> mutablePrimitiveSimpleClassName = Set.of("MutableBoolean", "MutableChar", "MutableByte", "MutableShort", "MutableInt",
            "MutableLong", "MutableFloat", "MutableDouble");

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<java.lang.reflect.Type, Type<?>> javaType2TypeCache = new ConcurrentCacheMap<>(POOL_SIZE);

    private static final Map<String, Type<?>> typePool = new ConcurrentCacheMap<>(POOL_SIZE);

    /** Spelling {@link #getJavaTypeName(java.lang.reflect.Type)} gives an upper-bounded wildcard argument. */
    private static final String WILDCARD_UPPER_BOUND_PREFIX = "? extends ";

    static final Class<?> guavaMultisetClass; // could be null if Guava library is not in the classpath.
    static final Class<?> guavaMultimapClass; // could be null if Guava library is not in the classpath.

    static {
        Class<?> multisetClass = null;
        Class<?> multimapClass = null;

        try {
            multisetClass = Class.forName("com.google.common.collect.Multiset");
            multimapClass = Class.forName("com.google.common.collect.Multimap");
        } catch (final Throwable e) {
            // ignore.
        }

        guavaMultisetClass = multisetClass;
        guavaMultimapClass = multimapClass;
    }

    static {
        // initializing built-in types

        // String pkgName = Type.class.getPackage().getName();
        // List<Class<?>> classes = PackageUtil.findClassesInPackage(pkgName, true, false);

        // For Android.
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
            classes.add(com.landawn.abacus.type.CharacterArrayType.class);
            classes.add(com.landawn.abacus.type.CharacterStreamType.class);
            classes.add(com.landawn.abacus.type.CharacterType.class);
            classes.add(com.landawn.abacus.type.ClazzType.class);
            classes.add(com.landawn.abacus.type.ClobAsciiStreamType.class);
            classes.add(com.landawn.abacus.type.ClobReaderType.class);
            classes.add(com.landawn.abacus.type.ClobType.class);
            classes.add(com.landawn.abacus.type.CollectionType.class);
            classes.add(com.landawn.abacus.type.CurrencyType.class);
            classes.add(com.landawn.abacus.type.DatasetType.class);
            classes.add(com.landawn.abacus.type.SheetType.class);
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
            classes.add(com.landawn.abacus.type.ImmutableMapType.class);
            classes.add(com.landawn.abacus.type.InputStreamType.class);
            classes.add(com.landawn.abacus.type.IntegerArrayType.class);
            classes.add(com.landawn.abacus.type.IntegerType.class);
            classes.add(com.landawn.abacus.type.JSONType.class);
            classes.add(com.landawn.abacus.type.JUDateType.class);
            classes.add(com.landawn.abacus.type.DateType.class);
            classes.add(com.landawn.abacus.type.TimeType.class);
            classes.add(com.landawn.abacus.type.TimestampType.class);
            classes.add(com.landawn.abacus.type.TimedType.class);
            classes.add(com.landawn.abacus.type.LocalDateType.class);
            classes.add(com.landawn.abacus.type.LocalTimeType.class);
            classes.add(com.landawn.abacus.type.LocalDateTimeType.class);
            classes.add(com.landawn.abacus.type.OffsetDateTimeType.class);
            classes.add(com.landawn.abacus.type.ZonedDateTimeType.class);
            classes.add(com.landawn.abacus.type.CalendarType.class);
            classes.add(com.landawn.abacus.type.XMLGregorianCalendarType.class);
            classes.add(com.landawn.abacus.type.MillisCalendarType.class);
            classes.add(com.landawn.abacus.type.MillisDateType.class);
            classes.add(com.landawn.abacus.type.MillisTimeType.class);
            classes.add(com.landawn.abacus.type.MillisTimestampType.class);
            classes.add(com.landawn.abacus.type.InstantType.class);
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
            classes.add(com.landawn.abacus.type.JdkDurationType.class);
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
            classes.add(com.landawn.abacus.type.HolderType.class);
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
            classes.add(com.landawn.abacus.type.Type.SerializationType.class);
            classes.add(com.landawn.abacus.type.Type.class);
            classes.add(com.landawn.abacus.type.TypeType.class);
            classes.add(com.landawn.abacus.type.URIType.class);
            classes.add(com.landawn.abacus.type.URLType.class);
            classes.add(com.landawn.abacus.type.UUIDType.class);
            classes.add(com.landawn.abacus.type.XMLType.class);
            classes.add(com.landawn.abacus.type.MultisetType.class);
            classes.add(com.landawn.abacus.type.SetMultimapType.class);
            classes.add(com.landawn.abacus.type.MultimapType.class);

            classes.add(com.landawn.abacus.type.BooleanCharType.class);
            classes.add(com.landawn.abacus.type.BooleanIntType.class);
        }

        // initialize external types
        {
            try {
                //noinspection ConstantValue
                if (Class.forName("org.bson.types.ObjectId") != null) {
                    classes.add(com.landawn.abacus.type.BSONObjectIdType.class);
                }
            } catch (final Throwable e) {
                // ignore.
            }

            try {
                //noinspection ConstantValue
                if (Class.forName("org.joda.time.DateTime") != null) {
                    classes.add(com.landawn.abacus.type.JodaInstantType.class);
                    classes.add(com.landawn.abacus.type.JodaDateTimeType.class);
                    classes.add(com.landawn.abacus.type.JodaMutableDateTimeType.class);
                }
            } catch (final Throwable e) {
                // ignore.
            }

            try {
                //noinspection ConstantValue
                if (Class.forName("android.net.Uri") != null) {
                    classes.add(Class.forName("com.landawn.abacus.type.AndroidUriType"));
                }
            } catch (final Throwable e) {
                // ignore.
            }
        }

        final List<Class<?>> delayInitializedTypeClasses = new ArrayList<>();

        for (final Class<?> cls : classes) {
            final int mod = cls.getModifiers();

            if (Type.class.isAssignableFrom(cls) && !Modifier.isAbstract(mod) && (ClassUtil.getDeclaredConstructor(cls) != null)) {
                if (AbstractPrimitiveListType.class.isAssignableFrom(cls)
                        || AbstractArrayType.class.isAssignableFrom(cls) /* || RangeType.class.equals(cls) */) {
                    delayInitializedTypeClasses.add(cls);

                    continue;
                }

                try {
                    final Type<?> type = (Type<?>) cls.getDeclaredConstructor().newInstance();
                    typePool.put(type.name(), type);

                    if (!(type.javaType().equals(String.class) || type.javaType().equals(InputStream.class) || type.javaType().equals(Reader.class)
                            || type instanceof MillisCalendarType || type instanceof MillisDateType || type instanceof MillisTimeType
                            || type instanceof MillisTimestampType || type instanceof BytesType || type instanceof BooleanCharType
                            || type instanceof BooleanIntType)
                            || (StringType.class.equals(type.getClass()) || InputStreamType.class.equals(type.getClass())
                                    || CharacterStreamType.class.equals(type.getClass()))) {
                        if (!(type instanceof JUDateType || type instanceof JdkOptionalIntType || type instanceof JdkOptionalLongType
                                || type instanceof JdkOptionalDoubleType || type instanceof JdkOptionalType || type instanceof JdkDurationType
                                || type.getClass().getSimpleName().startsWith("Joda"))) { // conflict with DateType.
                            typePool.put(type.javaType().getSimpleName(), type);
                        }

                        typePool.put(type.javaType().getCanonicalName(), type);
                    }
                } catch (final Throwable e) {
                    if (logger.isInfoEnabled()) {
                        logger.info(getClassName(cls) + " is not initialized as built-in type. Reason: " + e);
                    }
                }
            }
        }

        for (final Class<?> cls : delayInitializedTypeClasses) {
            try {
                final Type<?> type = (Type<?>) cls.getDeclaredConstructor().newInstance();

                typePool.put(type.name(), type);

                typePool.put(type.javaType().getSimpleName(), type);

                typePool.put(type.javaType().getCanonicalName(), type);
            } catch (final Throwable e) {
                if (logger.isInfoEnabled()) {
                    logger.info(getClassName(cls) + " is not initialized as built-in type. Reason: " + e);
                }
            }
        }

        // special cases:
        final Type<?> booleanType = typePool.get(PrimitiveBooleanType.BOOLEAN);

        if (booleanType != null) {
            typePool.put(PrimitiveBooleanType.BOOL, booleanType);
        }

        final Type<?> typeType = typePool.get(TypeType.TYPE);

        for (final Type<?> type : N.newHashSet(typePool.values())) {
            typePool.put(type.getClass().getSimpleName(), typeType);
            typePool.put(type.getClass().getCanonicalName(), typeType);
        }

        final Set<Class<?>> builtinType = N.asSet(StringType.class, PrimitiveByteArrayType.class, DateType.class, TimeType.class, TimestampType.class,
                CalendarType.class, BooleanType.class, ReaderType.class, InputStreamType.class);
        final Multiset<Class<?>> typeClassMultiset = N.newMultiset(typePool.size());

        for (final Type<?> type : typePool.values()) {
            typeClassMultiset.add(type.javaType());
        }

        for (final Type<?> type : typePool.values()) {
            if (typeClassMultiset.getCount(type.javaType()) > 1 && !builtinType.contains(type.getClass())) {
                if (type.getClass().getPackage() == null || !type.getClass().getPackageName().startsWith("com.landawn.abacus.type")) {
                    logger.info("More than one type is defined for class: " + getClassName(type.javaType()) + ". Ignore type: " + type.name());
                }

                continue;
            }

            if (type.isParameterizedType()) {
                continue;
            }

            javaType2TypeCache.put(type.reflectType(), type);
        }
    }

    /**
     * Returns the canonical class name for the given class, falling back to {@link Class#getName()}
     * when no canonical name is available (e.g., for anonymous or local classes).
     *
     * @param cls the class to obtain a name for
     * @return the canonical class name, or the binary name if no canonical name exists
     */
    static String getClassName(final Class<?> cls) {
        String clsName = ClassUtil.getCanonicalClassName(cls);

        if (Strings.isEmpty(clsName)) {
            clsName = cls.getName();
        }

        return clsName;
    }

    /**
     * Returns a human-readable name for the given {@link java.lang.reflect.Type}.
     * For {@link Class} instances this delegates to {@link #getClassName(Class)};
     * Parameterized, array, wildcard, and type-variable names are assembled recursively so
     * nested classes use canonical {@code '.'} separators instead of reflection's binary
     * {@code '$'} separators.
     *
     * @param javaType the reflection type to name
     * @return a string representation of the type suitable for use as a type-pool key
     */
    static String getJavaTypeName(final java.lang.reflect.Type javaType) {
        return getJavaTypeName(javaType, true);
    }

    private static String getJavaTypeName(final java.lang.reflect.Type javaType, final boolean topLevel) {
        if (javaType instanceof Class) {
            final String className = getClassName((Class<?>) javaType);
            return topLevel ? className : ClassUtil.formatParameterizedTypeName(className);
        } else if (javaType instanceof ParameterizedType parameterizedType) {
            final java.lang.reflect.Type rawType = parameterizedType.getRawType();
            final java.lang.reflect.Type ownerType = parameterizedType.getOwnerType();
            final String rawTypeName;

            if (ownerType != null && rawType instanceof Class<?> rawClass) {
                rawTypeName = getJavaTypeName(ownerType, false) + "." + rawClass.getSimpleName();
            } else {
                rawTypeName = getJavaTypeName(rawType, false);
            }

            final java.lang.reflect.Type[] arguments = parameterizedType.getActualTypeArguments();

            if (arguments.length == 0) {
                return rawTypeName;
            }

            final StringBuilder name = new StringBuilder(rawTypeName).append('<');

            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    name.append(", ");
                }

                name.append(getJavaTypeName(arguments[i], false));
            }

            return name.append('>').toString();
        } else if (javaType instanceof GenericArrayType arrayType) {
            return getJavaTypeName(arrayType.getGenericComponentType(), false) + "[]";
        } else if (javaType instanceof WildcardType wildcardType) {
            final java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();

            if (lowerBounds.length > 0) {
                return "? super " + getJavaTypeName(lowerBounds[0], false);
            }

            final java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();
            return upperBounds.length == 0 || upperBounds[0] == Object.class ? "?" : "? extends " + getJavaTypeName(upperBounds[0], false);
        } else if (javaType instanceof TypeVariable<?> typeVariable) {
            return typeVariable.getName();
        }

        return ClassUtil.getTypeName(javaType);
    }

    /**
     * Reconstructs Java bean parameter metadata before publishing its name in the shared cache.
     * Handler expressions such as {@code JSON<String>} need not have a Java reflection equivalent;
     * a {@code null} result preserves the existing raw-bean fallback for those expressions.
     */
    private static java.lang.reflect.Type parseBeanReflectionType(final String typeName) {
        final String name = typeName.trim();

        // Array suffixes following a wildcard belong to its bound, not to the wildcard itself.
        if (name.equals("?")) {
            return new NamedWildcardType(new java.lang.reflect.Type[] { Object.class }, new java.lang.reflect.Type[0]);
        } else if (name.startsWith("? extends ")) {
            final java.lang.reflect.Type bound = parseBeanReflectionType(name.substring(10));
            return bound == null ? null : new NamedWildcardType(new java.lang.reflect.Type[] { bound }, new java.lang.reflect.Type[0]);
        } else if (name.startsWith("? super ")) {
            final java.lang.reflect.Type bound = parseBeanReflectionType(name.substring(8));
            return bound == null ? null : new NamedWildcardType(new java.lang.reflect.Type[] { Object.class }, new java.lang.reflect.Type[] { bound });
        }

        if (name.endsWith("[]")) {
            final java.lang.reflect.Type component = parseBeanReflectionType(name.substring(0, name.length() - 2));
            if (component == null) {
                return null;
            }
            return component instanceof Class<?> cls ? java.lang.reflect.Array.newInstance(cls, 0).getClass() : new NamedGenericArrayType(component);
        }

        final TypeAttrParser parsed = TypeAttrParser.parse(name);
        final String[] arguments = parsed.getTypeParameters();
        final String className = parsed.getClassName();
        if (arguments.length > 0 && (className.equalsIgnoreCase(JSONType.JSON) || className.equalsIgnoreCase(XMLType.XML)
                || className.equalsIgnoreCase(TypeType.TYPE) || className.equalsIgnoreCase(ClazzType.CLAZZ))) {
            return null;
        }
        final Type<?> rawType = getType(className);
        final Class<?> rawClass = rawType.javaType();

        if (parsed.getParameters().length > 0 || arguments.length > 0 && arguments.length != rawClass.getTypeParameters().length) {
            return null;
        }

        java.lang.reflect.Type owner = rawClass.getDeclaringClass();

        if (owner != null) {
            int depth = 0;
            int ownerDelimiter = -1;

            for (int i = 0; i < name.length(); i++) {
                final char ch = name.charAt(i);
                if (ch == '<') {
                    depth++;
                } else if (ch == '>') {
                    depth--;
                } else if (depth == 0 && ch == '(') {
                    break;
                } else if (depth == 0 && (ch == '.' || ch == '$')) {
                    ownerDelimiter = i;
                }
            }

            if (ownerDelimiter >= 0) {
                owner = parseBeanReflectionType(name.substring(0, ownerDelimiter));
                if (owner == null) {
                    return null;
                }
            }
        }

        if (arguments.length == 0 && !(owner instanceof ParameterizedType)) {
            return rawClass;
        }

        final java.lang.reflect.Type[] argumentTypes = new java.lang.reflect.Type[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = parseBeanReflectionType(arguments[i]);
            if (argumentTypes[i] == null) {
                return null;
            }
        }

        return new NamedParameterizedType(owner, rawClass, argumentTypes);
    }

    private static final class NamedParameterizedType implements ParameterizedType {
        private final java.lang.reflect.Type ownerType;
        private final java.lang.reflect.Type rawType;
        private final java.lang.reflect.Type[] typeArguments;

        NamedParameterizedType(final java.lang.reflect.Type ownerType, final java.lang.reflect.Type rawType, final java.lang.reflect.Type[] typeArguments) {
            this.ownerType = ownerType;
            this.rawType = rawType;
            this.typeArguments = typeArguments.clone();
        }

        @Override
        public java.lang.reflect.Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        @Override
        public java.lang.reflect.Type getRawType() {
            return rawType;
        }

        @Override
        public java.lang.reflect.Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof ParameterizedType other && Objects.equals(ownerType, other.getOwnerType()) && Objects.equals(rawType, other.getRawType())
                    && Arrays.equals(typeArguments, other.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
        }

        @Override
        public String getTypeName() {
            final StringBuilder result;

            if (ownerType != null && rawType instanceof Class<?> rawClass) {
                result = new StringBuilder(ownerType.getTypeName()).append('$').append(rawClass.getSimpleName());
            } else {
                result = new StringBuilder(rawType.getTypeName());
            }

            if (typeArguments.length == 0) {
                return result.toString();
            }

            result.append('<');

            for (int i = 0; i < typeArguments.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(typeArguments[i].getTypeName());
            }

            return result.append('>').toString();
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private static final class NamedGenericArrayType implements GenericArrayType {
        private final java.lang.reflect.Type componentType;

        NamedGenericArrayType(final java.lang.reflect.Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public java.lang.reflect.Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof GenericArrayType other && Objects.equals(componentType, other.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(componentType);
        }

        @Override
        public String getTypeName() {
            return componentType.getTypeName() + "[]";
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private static final class NamedWildcardType implements WildcardType {
        private final java.lang.reflect.Type[] upperBounds;
        private final java.lang.reflect.Type[] lowerBounds;

        NamedWildcardType(final java.lang.reflect.Type[] upperBounds, final java.lang.reflect.Type[] lowerBounds) {
            this.upperBounds = upperBounds.clone();
            this.lowerBounds = lowerBounds.clone();
        }

        @Override
        public java.lang.reflect.Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        @Override
        public java.lang.reflect.Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof WildcardType other && Arrays.equals(upperBounds, other.getUpperBounds())
                    && Arrays.equals(lowerBounds, other.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
        }

        @Override
        public String getTypeName() {
            if (lowerBounds.length > 0) {
                return "? super " + lowerBounds[0].getTypeName();
            }

            return upperBounds.length == 0 || upperBounds[0] == Object.class ? "?" : "? extends " + upperBounds[0].getTypeName();
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    /**
     * Handler for an upper-bounded wildcard type argument such as {@code ? extends Person}.
     * <p>
     * It keeps the wildcard spelling as its {@link #name()} (so {@code List<? extends Person>} keeps its
     * name and {@code ValueTypeResolver} can still recognise the wildcard) but forwards every other
     * operation, including {@link #serializationType()} and {@link #javaType()}, to the bound's handler
     * so that elements are read as the bound type rather than as generic {@code Object}s.
     * </p>
     */
    private static final class UpperBoundedWildcardType<T> implements Type<T> {
        private final String name;
        private final String declaringName;
        private final String xmlName;
        private final Type<T> bound;

        @SuppressWarnings("unchecked")
        UpperBoundedWildcardType(final String name, final Type<?> bound) {
            this.name = name;
            declaringName = WILDCARD_UPPER_BOUND_PREFIX + bound.declaringName();
            xmlName = name.replace("<", "&lt;").replace(">", "&gt;"); //NOSONAR
            this.bound = (Type<T>) bound;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String declaringName() {
            return declaringName;
        }

        @Override
        public String xmlName() {
            return xmlName;
        }

        @Override
        public Class<T> javaType() {
            return bound.javaType();
        }

        @Override
        public java.lang.reflect.Type reflectType() {
            return bound.reflectType();
        }

        @Override
        public boolean isPrimitive() {
            return bound.isPrimitive();
        }

        @Override
        public boolean isPrimitiveWrapper() {
            return bound.isPrimitiveWrapper();
        }

        @Override
        public boolean isPrimitiveList() {
            return bound.isPrimitiveList();
        }

        @Override
        public boolean isBoolean() {
            return bound.isBoolean();
        }

        @Override
        public boolean isCharacter() {
            return bound.isCharacter();
        }

        @Override
        public boolean isNumber() {
            return bound.isNumber();
        }

        @Override
        public boolean isByte() {
            return bound.isByte();
        }

        @Override
        public boolean isShort() {
            return bound.isShort();
        }

        @Override
        public boolean isInteger() {
            return bound.isInteger();
        }

        @Override
        public boolean isLong() {
            return bound.isLong();
        }

        @Override
        public boolean isFloat() {
            return bound.isFloat();
        }

        @Override
        public boolean isDouble() {
            return bound.isDouble();
        }

        @Override
        public boolean isString() {
            return bound.isString();
        }

        @Override
        public boolean isCharSequence() {
            return bound.isCharSequence();
        }

        @Override
        public boolean isDate() {
            return bound.isDate();
        }

        @Override
        public boolean isCalendar() {
            return bound.isCalendar();
        }

        @Override
        public boolean isJodaDateTime() {
            return bound.isJodaDateTime();
        }

        @Override
        public boolean isTemporal() {
            return bound.isTemporal();
        }

        @Override
        public boolean isPrimitiveArray() {
            return bound.isPrimitiveArray();
        }

        @Override
        public boolean isPrimitiveByteArray() {
            return bound.isPrimitiveByteArray();
        }

        @Override
        public boolean isObjectArray() {
            return bound.isObjectArray();
        }

        @Override
        public boolean isArray() {
            return bound.isArray();
        }

        @Override
        public boolean isList() {
            return bound.isList();
        }

        @Override
        public boolean isSet() {
            return bound.isSet();
        }

        @Override
        public boolean isCollection() {
            return bound.isCollection();
        }

        @Override
        public boolean isMap() {
            return bound.isMap();
        }

        @Override
        public boolean isBean() {
            return bound.isBean();
        }

        @Override
        public boolean isMapEntity() {
            return bound.isMapEntity();
        }

        @Override
        public boolean isEntityId() {
            return bound.isEntityId();
        }

        @Override
        public boolean isDataset() {
            return bound.isDataset();
        }

        @Override
        public boolean isInputStream() {
            return bound.isInputStream();
        }

        @Override
        public boolean isReader() {
            return bound.isReader();
        }

        @Override
        public boolean isByteBuffer() {
            return bound.isByteBuffer();
        }

        @Override
        public boolean isParameterizedType() {
            return bound.isParameterizedType();
        }

        @Override
        public boolean isImmutable() {
            return bound.isImmutable();
        }

        @Override
        public boolean isComparable() {
            return bound.isComparable();
        }

        @Override
        public boolean isSerializable() {
            return bound.isSerializable();
        }

        @Override
        public boolean isOptionalOrNullable() {
            return bound.isOptionalOrNullable();
        }

        @Override
        public boolean isCsvQuoteRequired() {
            return bound.isCsvQuoteRequired();
        }

        @Override
        public boolean isObject() {
            return bound.isObject();
        }

        @Override
        public SerializationType serializationType() {
            return bound.serializationType();
        }

        @Override
        public Type<?> elementType() {
            return bound.elementType();
        }

        @Override
        public List<Type<?>> parameterTypes() {
            return bound.parameterTypes();
        }

        @Override
        public T defaultValue() {
            return bound.defaultValue();
        }

        @Override
        public boolean isDefaultValue(final T value) {
            return bound.isDefaultValue(value);
        }

        @Override
        public int compare(final T x, final T y) {
            return bound.compare(x, y);
        }

        @Override
        public String stringOf(final T x) {
            return bound.stringOf(x);
        }

        @Override
        public T valueOf(final String str) {
            return bound.valueOf(str);
        }

        @Override
        public T valueOf(final Object obj) {
            return bound.valueOf(obj);
        }

        @Override
        public T valueOf(final char[] cbuf, final int offset, final int len) {
            return bound.valueOf(cbuf, offset, len);
        }

        @Override
        public T get(final ResultSet rs, final int columnIndex) throws SQLException {
            return bound.get(rs, columnIndex);
        }

        @Override
        public T get(final ResultSet rs, final String columnName) throws SQLException {
            return bound.get(rs, columnName);
        }

        @Override
        public void set(final PreparedStatement stmt, final int columnIndex, final T x) throws SQLException {
            bound.set(stmt, columnIndex, x);
        }

        @Override
        public void set(final CallableStatement stmt, final String parameterName, final T x) throws SQLException {
            bound.set(stmt, parameterName, x);
        }

        @Override
        public void set(final PreparedStatement stmt, final int columnIndex, final T x, final int sqlTypeOrLength) throws SQLException {
            bound.set(stmt, columnIndex, x, sqlTypeOrLength);
        }

        @Override
        public void set(final CallableStatement stmt, final String parameterName, final T x, final int sqlTypeOrLength) throws SQLException {
            bound.set(stmt, parameterName, x, sqlTypeOrLength);
        }

        @Override
        public void appendTo(final Appendable appendable, final T x) throws IOException {
            bound.appendTo(appendable, x);
        }

        @Override
        public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config) throws IOException {
            bound.serializeTo(writer, x, config);
        }

        @Override
        public T collectionToArray(final Collection<?> c) {
            return bound.collectionToArray(c);
        }

        @Override
        public <E> Collection<E> arrayToCollection(final T x, final Class<?> collClass) {
            return bound.arrayToCollection(x, collClass);
        }

        @Override
        public void arrayToCollection(final T x, final Collection<?> output) {
            bound.arrayToCollection(x, output);
        }

        @Override
        public int hashCode(final T x) {
            return bound.hashCode(x);
        }

        @Override
        public int deepHashCode(final T x) {
            return bound.deepHashCode(x);
        }

        @Override
        public boolean equals(final T x, final T y) {
            return bound.equals(x, y);
        }

        @Override
        public boolean deepEquals(final T x, final T y) {
            return bound.deepEquals(x, y);
        }

        @Override
        public String toString(final T x) {
            return bound.toString(x);
        }

        @Override
        public String deepToString(final T x) {
            return bound.deepToString(x);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this || (obj instanceof UpperBoundedWildcardType<?> other && name.equals(other.name) && bound.equals(other.bound));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Rejects constructor arguments ({@code "String(MD5)"}, {@code "Date(yyyy)"}) on a resolved class whose
     * handler takes none, mirroring the container branches; callers with a parameter-consuming handler
     * ({@code Password(...)}, enums, handler classes, pooled handlers with a {@code (String)} constructor) skip it.
     * @throws IllegalArgumentException if constructor parameters are supplied for a type handler that accepts none
     */
    private static void checkNoParameters(final String typeName, final String[] parameters, final Class<?> cls) throws IllegalArgumentException {
        if (parameters.length > 0) {
            throw new IllegalArgumentException(
                    "Incorrect parameters: " + typeName + ". " + ClassUtil.getSimpleClassName(cls) + " Type can only have zero parameter.");
        }
    }

    /**
     * Mirrors the class dispatch in {@link #getType(String, Class, java.lang.reflect.Type)} WITHOUT constructing a
     * handler: tells whether that dispatch hands {@code cls} to a dedicated built-in handler (Map, Collection,
     * java.util.Date, Optional, Tuple, Object[]...) rather than fabricating a generic fallback (ObjectType,
     * BeanType, EnumType, NumberType) or scanning the pool for an assignable registered handler. Registration
     * by class is refused only in the former case. Keep this list aligned with that dispatch.
     */
    private static boolean hasBuiltInType(final Class<?> cls) {
        return cls == Object.class || cls == Number.class || cls == Enum.class || java.util.Date.class.isAssignableFrom(cls)
                || Calendar.class.isAssignableFrom(cls) || XMLGregorianCalendar.class.isAssignableFrom(cls) || Reader.class.isAssignableFrom(cls)
                || InputStream.class.isAssignableFrom(cls) || ByteBuffer.class.isAssignableFrom(cls) || java.util.Optional.class.isAssignableFrom(cls)
                || Optional.class.isAssignableFrom(cls) || Nullable.class.isAssignableFrom(cls) || Holder.class.isAssignableFrom(cls)
                || Multiset.class.isAssignableFrom(cls) || Multimap.class.isAssignableFrom(cls) || Range.class.isAssignableFrom(cls)
                || EntityId.class.isAssignableFrom(cls) || Dataset.class.isAssignableFrom(cls) || Sheet.class.isAssignableFrom(cls)
                || HBaseColumn.class.isAssignableFrom(cls) || (guavaMultisetClass != null && guavaMultisetClass.isAssignableFrom(cls))
                || (guavaMultimapClass != null && guavaMultimapClass.isAssignableFrom(cls)) || Collection.class.isAssignableFrom(cls)
                || Map.class.isAssignableFrom(cls) || Pair.class.isAssignableFrom(cls) || Triple.class.isAssignableFrom(cls) || Tuple1.class.equals(cls)
                || Tuple2.class.equals(cls) || Tuple3.class.equals(cls) || Tuple4.class.equals(cls) || Tuple5.class.equals(cls) || Tuple6.class.equals(cls)
                || Tuple7.class.equals(cls) || Tuple8.class.equals(cls) || Tuple9.class.equals(cls) || Indexed.class.equals(cls) || Timed.class.equals(cls)
                || Map.Entry.class.isAssignableFrom(cls) || Type.class.isAssignableFrom(cls) || NClob.class.isAssignableFrom(cls)
                || Clob.class.isAssignableFrom(cls) || Blob.class.isAssignableFrom(cls) || Object[].class.isAssignableFrom(cls);
    }

    /**
     * Tells whether a class-name token that no class could be loaded for still carries {@code '['} / {@code ']'}
     * once its trailing {@code "[]"} pairs are removed, i.e. is malformed array syntax rather than an unknown name.
     */
    private static boolean hasMisplacedBracket(final String clsName) {
        String stripped = clsName;

        while (stripped.endsWith("[]")) {
            stripped = stripped.substring(0, stripped.length() - 2);
        }

        return stripped.indexOf('[') >= 0 || stripped.indexOf(']') >= 0;
    }

    /**
     * Tells whether a pool entry found under the canonical name of {@code cls} is a fallback that a lookup
     * fabricated for that class (as opposed to a registered handler), i.e. one a registration may legitimately
     * supersede. It cannot separate a fabricated fallback from a built-in with the same shape (the built-in
     * entry for {@code java.lang.Object} is an {@code ObjectType} for {@code Object.class}), so callers must
     * consult {@link #hasBuiltInType(Class)} first.
     */
    private static boolean isFabricatedType(final Type<?> type, final Class<?> cls) {
        return type.javaType() == cls
                && (type.getClass() == ObjectType.class || type instanceof BeanType || type instanceof EnumType || type.getClass() == NumberType.class);
    }

    /**
     * The {@link #registerType(Class, Type)} guard, factored out of its registry reads so that it can be
     * exercised for a class this JVM has already resolved ({@code alreadyCached} and
     * {@code pooledCanonicalType} are the only registry state it depends on).
     * <p>
     * {@link #hasBuiltInType(Class)} must be consulted even when a canonical pool entry exists: the built-in
     * entry for {@code java.lang.Object} (and the one a name lookup caches for {@code java.lang.Number}) is
     * itself an {@code ObjectType} / {@code NumberType} for that very class, so a pooled-entry-first rule
     * reads it as a fabricated fallback, accepts the registration and then retires the built-in.
     * </p>
     *
     * @param cls the class being registered for
     * @param alreadyCached whether a type has already been resolved and cached for {@code cls}
     * @param pooledCanonicalType the pool entry under the canonical name of {@code cls}, or {@code null}
     * @return {@code true} if the registration must be refused
     */
    static boolean refusesRegistrationByClass(final Class<?> cls, final boolean alreadyCached, final Type<?> pooledCanonicalType) {
        return alreadyCached || hasBuiltInType(cls) || (pooledCanonicalType != null && !isFabricatedType(pooledCanonicalType, cls));
    }

    /**
     * @throws IllegalArgumentException if the type name supplies an invalid number of generic arguments or constructor parameters, or its type metadata violates the selected handler contract
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> Type<T> getType(String typeName, Class cls, java.lang.reflect.Type javaType) throws IllegalArgumentException {
        if (Strings.isEmpty(typeName)) {
            typeName = getClassName(cls);
        }

        // Normalize once so the pool key, the "[]" check and the class dispatch all see one spelling:
        // TypeAttrParser trims the class token, so " int" / "Integer " / "Foo[] " used to be cached
        // under the padded key as degraded ObjectType / NumberType / non-array handlers.
        final String requestedTypeName = typeName;
        typeName = typeName.trim();

        if (typeName.isEmpty()) {
            throw new IllegalArgumentException("Malformed type attribute: missing class name in: " + requestedTypeName);
        }

        Type type = typePool.get(typeName);

        if (type == null) {
            final TypeAttrParser attrResult = TypeAttrParser.parse(typeName);

            if (typeName.startsWith(WILDCARD_UPPER_BOUND_PREFIX)) {
                // Reflection renders "List<? extends Person>" element as "? extends Person"; elements must
                // parse as Person, but the handler has to KEEP the wildcard name: ValueTypeResolver rebuilds
                // wildcard bindings from name(), and the bound's own instance must never be pooled under
                // the wildcard key. The bound gets the whole remainder (also "List<String>[]", nested "?").
                final Type<?> boundType = getType(typeName.substring(WILDCARD_UPPER_BOUND_PREFIX.length()));
                final Type<?> wildcardType = new UpperBoundedWildcardType<>(typeName, boundType);
                final Type<?> publishedType = typePool.putIfAbsent(typeName, wildcardType);
                return (Type<T>) (publishedType == null ? wildcardType : publishedType);
            } else if (typeName.equals("?") || typeName.startsWith("? super ")) {
                // Only an upper bound constrains a value; "?" / "? super X" stay Object-capable. A trailing
                // "[]" still makes it an array of those: this branch returns before the shared "[]" step at
                // the tail, so "? super X[]" would otherwise lose isArray()/Object[] and read as a String.
                type = typeName.endsWith("[]") ? new ObjectArrayType<>(getType(typeName.substring(0, typeName.length() - 2)))
                        : new ObjectType<>(typeName, Object.class);
                final Type<?> publishedType = typePool.putIfAbsent(typeName, type);
                return publishedType == null ? type : (Type<T>) publishedType;
            }

            if (typeName.endsWith("[]") && typeName.indexOf('<') >= 0) {
                // Validate the whole declaration before resolving its component so the array path
                // enforces the same grammar, including rejection of trailing constructor arguments.
                final Type<?> componentType = javaType instanceof GenericArrayType arrayType ? getType(arrayType.getGenericComponentType())
                        : getType(typeName.substring(0, typeName.length() - 2));
                final Type<?> arrayType = new ObjectArrayType<>(componentType);
                final Type<?> publishedType = typePool.putIfAbsent(typeName, arrayType);
                return (Type<T>) (publishedType == null ? arrayType : publishedType);
            }

            final String[] typeParameters = attrResult.getTypeParameters();
            final String[] parameters = attrResult.getParameters();
            final String clsName = attrResult.getClassName();

            if (clsName.equalsIgnoreCase(ClazzType.CLAZZ)) {
                if (typeParameters.length != 1) {
                    throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Clazz Type can only have one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Clazz Type can only have zero parameter.");
                }

                type = new ClazzType(typeParameters[0]);
            } else if (clsName.equalsIgnoreCase(TypeType.TYPE)) {
                if (typeParameters.length > 1) {
                    throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Type can only have zero or one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Type can only have zero parameter.");
                }

                type = new TypeType(typeName);
            } else if (clsName.equalsIgnoreCase(JSONType.JSON)) {
                if (typeParameters.length > 1) {
                    throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". JSON Type can only have zero or one type parameter.");
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
                    throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". XML Type can only have zero or one type parameter.");
                }
                if (parameters.length > 0) {
                    throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". XML Type can only have zero parameter.");
                }

                if (typeParameters.length == 0) {
                    type = new XMLType(Map.class.getSimpleName());
                } else {
                    type = new XMLType(typeParameters[0]);
                }
            } else {
                if (cls == null) {
                    try {
                        cls = ClassUtil.forName(clsName);
                    } catch (final Throwable e) {
                        if (clsName.equals(ImmutableMapEntryType.MAP_IMMUTABLE_ENTRY)) {
                            cls = AbstractMap.SimpleImmutableEntry.class;
                        } else if (clsName.equals(Indexed.class.getSimpleName())) {
                            cls = Indexed.class;
                        } else if (clsName.equals(Timed.class.getSimpleName())) {
                            cls = Timed.class;
                        }
                    }
                }

                // A handler registered by class (registerType(Class, ...)) is published under its own name()
                // only, which AbstractType shortens ("java.time.Year" -> "Year"); a canonical-name lookup
                // (Type.ofList(Year.class), a List<Year> bean field) must find it instead of fabricating an
                // ObjectType. Every built-in class key already has a canonical pool entry, so this only
                // fires for registrations. Plain class names only: "Foo<X>", "Color(NAME)" and an
                // owner-parameterized "Outer<X>.Member" (no type parameters of its own, but a distinct
                // reflect type) keep dispatching, else the RAW class entry would answer for them.
                final Type registeredType = cls != null && typeName.indexOf('<') < 0 && parameters.length == 0 && !(javaType instanceof ParameterizedType)
                        ? javaType2TypeCache.get(cls)
                        : null;

                if (registeredType != null) {
                    type = registeredType;
                } else if (cls == null) {
                    if (clsName.equals(PasswordType.PASSWORD)) {
                        if (typeParameters.length > 0) {
                            throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". PasswordType can only have zero type parameters.");
                        }
                        if (parameters.length > 1) {
                            throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". PasswordType can only have zero or one parameter.");
                        }

                        if (parameters.length == 0) {
                            type = typePool.get(PasswordType.PASSWORD);
                        } else {
                            type = new PasswordType(parameters[0]);
                        }
                    } else if (hasMisplacedBracket(clsName)) {
                        // Brackets other than trailing "[]" pairs in an UNRESOLVABLE class token ("String[ ]",
                        // "String[", "String]", "String[]extra") are broken array syntax, not an unknown name;
                        // JVM descriptors like "[I" resolve above and never get here.
                        throw new IllegalArgumentException("Malformed type attribute: misplaced '[' or ']' in: " + typeName);
                    } else {
                        type = new ObjectType<>(typeName, Object.class);
                    }
                } else if (java.util.Date.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);

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
                    checkNoParameters(typeName, parameters, cls);
                    type = getType(CalendarType.CALENDAR);
                } else if (XMLGregorianCalendar.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = getType(XMLGregorianCalendarType.XML_GREGORIAN_CALENDAR);
                } else if (Reader.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new ReaderType(cls);
                } else if (InputStream.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new InputStreamType(cls);
                } else if (ByteBuffer.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new ByteBufferType(cls);
                } else if (cls.isEnum() || Enum.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". EnumType does not support type parameters.");
                    }

                    if (parameters.length == 0) {
                        type = new EnumType(clsName);
                    } else if (parameters.length == 1) {
                        type = new EnumType(clsName, com.landawn.abacus.util.EnumType.valueOf(parameters[0]));
                    } else {
                        throw new IllegalArgumentException("Unsupported parameters for EnumType: " + typeName);
                    }
                } else if (java.util.Optional.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Optional can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Optional Type can only have zero parameter.");
                    }

                    type = new JdkOptionalType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Optional.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Optional can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Optional Type can only have zero parameter.");
                    }

                    type = new OptionalType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Nullable.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Nullable can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Nullable Type can only have zero parameter.");
                    }

                    type = new NullableType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Holder.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Holder can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Holder Type can only have zero parameter.");
                    }

                    type = new HolderType(typeParameters.length == 0 ? "Object" : typeParameters[0]);
                } else if (Multiset.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Multiset Type can only have zero or one type parameter.");
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
                                "Incorrect type parameters: " + typeName + ". ListMultimap Type can only have zero or two type parameters.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ListMultimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ListMultimapType(cls, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new ListMultimapType(cls, typeParameters[0], typeParameters[1]);
                    }
                } else if (SetMultimap.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". SetMultimap Type can only have zero or two type parameters.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". SetMultimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new SetMultimapType(cls, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new SetMultimapType(cls, typeParameters[0], typeParameters[1]);
                    }
                } else if (Multimap.class.isAssignableFrom(cls)) {
                    final int typeParamCount = typeParameters.length;

                    if (!(typeParamCount == 0 || typeParamCount == 2 || typeParamCount == 3)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Multimap Type can only have zero, two, or three type parameters.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Multimap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MultimapType(cls, ObjectType.OBJECT, ObjectType.OBJECT, "List<Object>");
                    } else if (typeParameters.length == 2) {
                        type = new MultimapType(cls, typeParameters[0], null, typeParameters[1]);
                    } else {
                        type = new MultimapType(cls, typeParameters[0], typeParameters[1], typeParameters[2]);
                    }
                } else if (Range.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Range Type can only have zero or one type parameter.");
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
                    checkNoParameters(typeName, parameters, cls);
                    type = getType(EntityIdType.ENTITY_ID);
                } else if (Dataset.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = getType(DatasetType.DATASET);
                } else if (Sheet.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 3) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Sheet Type can only have zero or three type parameters.");
                    }

                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Sheet Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 3) {
                        type = new SheetType(typeParameters[0], typeParameters[1], typeParameters[2]);
                    } else {
                        type = new SheetType(ObjectType.OBJECT, ObjectType.OBJECT, ObjectType.OBJECT);
                    }
                } else if (HBaseColumn.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". HBaseColumn Type can only have zero or one type parameter.");
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
                                "Incorrect type parameters: " + typeName + ". ImmutableList Type can only have zero or one type parameter.");
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
                                "Incorrect type parameters: " + typeName + ". ImmutableSet Type can only have zero or one type parameter.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ImmutableSet Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ImmutableSetType(cls, ObjectType.OBJECT);
                    } else {
                        type = new ImmutableSetType(cls, typeParameters[0]);
                    }
                } else if (guavaMultisetClass != null && guavaMultisetClass.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Guava Multiset Type can only have zero or one type parameter.");
                    }

                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Guava Multiset Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new GuavaMultisetType(cls, ObjectType.OBJECT);
                    } else {
                        type = new GuavaMultisetType(cls, typeParameters[0]);
                    }
                } else if (guavaMultimapClass != null && guavaMultimapClass.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Guava Multimap Type can only have zero or two type parameters.");
                    }

                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Guava Multimap can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new GuavaMultimapType(cls, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new GuavaMultimapType(cls, typeParameters[0], typeParameters[1]);
                    }
                } else if (Collection.class.isAssignableFrom(cls)) {
                    if (typeParameters.length > 1) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". Collection Type can only have zero or one type parameter.");
                    }

                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Collection Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new CollectionType(cls, ObjectType.OBJECT);
                    } else {
                        type = new CollectionType(cls, typeParameters[0]);
                    }
                } else if (ImmutableMap.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException(
                                "Incorrect type parameters: " + typeName + ". ImmutableMap Type can only have zero or two type parameters.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". ImmutableMap Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new ImmutableMapType(cls, ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new ImmutableMapType(cls, typeParameters[0], typeParameters[1]);
                    }
                } else if (Map.class.isAssignableFrom(cls)) {
                    if ((typeParameters.length != 2) && (typeParameters.length != 0)) {
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Map Type can only have zero or two type parameters.");
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
                        throw new IllegalArgumentException("Incorrect type parameters: " + typeName + ". Pair Type can only have zero or two type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Triple Type can only have zero or three type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple2 Type can only have zero or two type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple3 Type can only have zero or three type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple4 Type can only have zero or four type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple5 Type can only have zero or five type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple6 Type can only have zero or six type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple7 Type can only have zero or seven type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple8 Type can only have zero or eight type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Tuple9 Type can only have zero or nine type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Map.ImmutableEntry Type can only have zero or two type parameters.");
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
                                "Incorrect type parameters: " + typeName + ". Map.Entry Type can only have zero or two type parameters.");
                    }
                    if (parameters.length > 0) {
                        throw new IllegalArgumentException("Incorrect parameters: " + typeName + ". Map.Entry Type can only have zero parameter.");
                    }

                    if (typeParameters.length == 0) {
                        type = new MapEntryType(ObjectType.OBJECT, ObjectType.OBJECT);
                    } else {
                        type = new MapEntryType(typeParameters[0], typeParameters[1]);
                    }
                } else if (Number.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new NumberType(cls);
                } else if (Beans.isBeanClass(cls) && !mutablePrimitiveSimpleClassName.contains(ClassUtil.getSimpleClassName(cls))) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new BeanType(cls, javaType == null && typeName.indexOf('<') >= 0 ? parseBeanReflectionType(typeName) : javaType);
                } else if (Type.class.isAssignableFrom(cls)) {
                    // Handler classes take their own constructor arguments: "Factory(\"custom,argument\")".
                    type = TypeAttrParser.newInstance(((Class<?>) cls).asSubclass(Type.class), typeName);
                } else if (NClob.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new NClobType(cls);
                } else if (Clob.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new ClobType(cls);
                } else if (Blob.class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new BlobType(cls);
                } else if (Object[].class.isAssignableFrom(cls)) {
                    checkNoParameters(typeName, parameters, cls);
                    type = new ObjectArrayType(cls);
                } else {
                    Type<?> val = null;

                    for (final Map.Entry<String, Type<?>> entry : typePool.entrySet()) {
                        val = entry.getValue();

                        if (!(val.isObject() || val.javaType().equals(Object[].class)) && val.javaType().isAssignableFrom(cls)) {
                            try {
                                if ((val.isParameterizedType() || N.notEmpty(typeParameters) || N.notEmpty(parameters)) && Strings.isNotEmpty(typeName)) {
                                    final Constructor<? extends Type> constructor = ClassUtil.getDeclaredConstructor(val.getClass(), String.class);

                                    if (constructor != null) {
                                        ClassUtil.setAccessibleQuietly(constructor, true);
                                        type = ClassUtil.invokeConstructor(constructor, typeName);
                                    } else if (parameters.length == 0) {
                                        // With "(...)" arguments an IllegalArgumentException follows below anyway.
                                        logger.warn(getClassName(val.getClass()) + "(String typeName) {...} should be defined");
                                    }
                                } else {
                                    final Constructor<? extends Type> constructor = ClassUtil.getDeclaredConstructor(val.getClass(), Class.class);

                                    if (constructor != null) {
                                        ClassUtil.setAccessibleQuietly(constructor, true);
                                        type = ClassUtil.invokeConstructor(constructor, cls);
                                    }
                                }
                            } catch (final Throwable e) {
                                // ignore.
                                // type = val;
                            }
                        }

                        if (type != null) {
                            break;
                        }
                    }

                    if (type == null) {
                        // No pooled handler consumed the "(...)" arguments: "String(MD5)" / "StringBuilder(100)"
                        // used to degrade into an ObjectType (isString() false) instead of failing fast.
                        checkNoParameters(typeName, parameters, cls);
                        type = Strings.isEmpty(typeName) ? new ObjectType<>(cls) : new ObjectType<>(typeName, cls);
                    }
                }
            }

            if (typeName.endsWith("[]") && !type.isArray()) {
                // Build the array type from the COMPONENT type: pool-registered simple names like
                // "UUID" resolve correctly there, while the full-name fallback produced a doubled
                // name ("UUID[][]") with Object (String-deserialized) elements.
                type = new ObjectArrayType(getType(typeName.substring(0, typeName.length() - 2)));
            }

            // A lookup may have raced with custom registration or another lookup. Preserve and
            // return the mapping that was published first instead of overwriting it with this
            // speculative instance.
            final Type publishedType = typePool.putIfAbsent(typeName, type);

            if (publishedType == null) {
                if (typePool.size() % 100 == 0) {
                    logger.warn("Size of type pool reaches: " + typePool.size() + " with initialized pool size: " + POOL_SIZE);
                }
            } else {
                type = publishedType;
            }
        }

        return type;
    }

    /**
     * Retrieves the Type object corresponding to the specified Class object.
     * <p>
     * This method looks up and returns a Type instance that represents the given class.
     * The method first checks a cache of class-to-type mappings. If the type is not found
     * in the cache, it creates a new Type instance and atomically publishes it for future use.
     * If a custom registration or another lookup publishes a mapping concurrently, that existing
     * mapping wins and is returned.
     * </p>
     * <p>
     * The method supports built-in types, primitive types, collections, maps, optional types,
     * and custom bean types. For generic types, use {@link #getType(String)} with a type name
     * that includes type parameters.
     * </p>
     * <p>
     * This method never returns {@code null}: a class with no dedicated {@code Type} resolves to a
     * fabricated, cached {@link com.landawn.abacus.type.ObjectType ObjectType} (or a bean type)
     * rather than failing.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<String> stringType = TypeFactory.getType(String.class);
     * Type<Integer> intType = TypeFactory.getType(Integer.class);
     * Type<List> listType = TypeFactory.getType(List.class);
     * }</pre>
     *
     * <p><b>Note — the type parameter {@code <T>} is intentionally unbound (it is {@code Class<?>},
     * NOT {@code Class<? extends T>}).</b> This is deliberate and is kept on purpose: callers
     * frequently collect heterogeneous results into a single typed container (e.g.
     * {@code classes.stream().map(TypeFactory::getType).collect(toList())} assigned to a
     * {@code List<Type<Object>>}). Tightening the parameter to {@code Class<? extends T>} would make
     * {@code T} inferable per-element and break those legitimate call sites, so the looser
     * {@code Class<?>} signature is retained by design (the inconsistency with
     * {@code registerType(Class<T>, ...)} is accepted).</p>
     *
     * @param <T> the Java type represented by the requested {@code Type} object
     * @param cls the Class object for which to retrieve the Type
     * @return the Type object corresponding to the specified class (never {@code null})
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     * @see #getType(String)
     * @see #getType(java.lang.reflect.Type)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Type<T> getType(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        Type type = javaType2TypeCache.get(cls);

        if (type == null) {
            type = getType(getClassName(cls), cls, cls);

            if (type != null) {
                final Type publishedType = javaType2TypeCache.putIfAbsent(cls, type);

                if (publishedType != null) {
                    type = publishedType;
                }
            }
        }

        return type;
    }

    /**
     * Retrieves the Type object corresponding to the specified java.lang.reflect.Type.
     * <p>
     * This method handles regular Class objects, ParameterizedType instances and generic arrays.
     * For ParameterizedType instances (e.g., List&lt;String&gt;, Map&lt;String, Integer&gt;),
     * it extracts the type information including type parameters and creates the appropriate
     * Type object. Results are cached for performance. Cache publication is atomic: when another
     * lookup has already published a result for the same reflection type, that result is retained
     * and returned.
     * </p>
     *
     * <p>This method never returns {@code null}: for an unrecognized type it fabricates and caches an
     * {@link com.landawn.abacus.type.ObjectType ObjectType} fallback (the same behavior as
     * {@link #getType(Class)} / {@link #getType(String)}) rather than failing. An
     * {@link IllegalArgumentException} is thrown only for a {@code null} argument or for structurally
     * malformed generic/parameter syntax (e.g., a wrong number of type arguments).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.lang.reflect.Type listType = new TypeReference<List<String>>() {}.javaType();
     * Type<List<String>> strings = TypeFactory.getType(listType);
     *
     * java.lang.reflect.Type mapType = new TypeReference<Map<String, Integer>>() {}.javaType();
     * Type<Map<String, Integer>> mapping = TypeFactory.getType(mapType);
     * }</pre>
     *
     * @param <T> the Java type represented by the requested {@code Type} object
     * @param javaType the java.lang.reflect.Type to convert, including Class, ParameterizedType and GenericArrayType
     * @return the corresponding Type object (never {@code null})
     * @throws IllegalArgumentException if {@code javaType} is {@code null}, or if the type name format is
     *         structurally invalid.
     * @see #getType(Class)
     * @see #getType(String)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Type<T> getType(final java.lang.reflect.Type javaType) throws IllegalArgumentException {
        N.checkArgNotNull(javaType, cs.javaType);

        Type result = javaType2TypeCache.get(javaType);

        if (result == null) {
            //noinspection ConditionCoveredByFurtherCondition
            if ((javaType instanceof ParameterizedType) || !(javaType instanceof Class)) {
                final Class cls = javaType instanceof Class ? (Class) javaType
                        : javaType instanceof ParameterizedType pt && pt.getRawType() instanceof Class ? (Class) pt.getRawType() : null;

                result = getType(getJavaTypeName(javaType), cls, javaType);
            } else {
                result = getType((Class) javaType);
            }

            final Type publishedType = javaType2TypeCache.putIfAbsent(javaType, result);

            if (publishedType != null) {
                result = publishedType;
            }
        }

        return result;
    }

    /**
     * Retrieves the Type object corresponding to the specified type name string.
     * <p>
     * This method parses a type name string and returns the appropriate Type object.
     * The type name can include type parameters and special parameters for customization.
     * </p>
     * <p>
     * Supported type name formats:
     * </p>
     * <ul>
     *   <li>Simple class name: "String", "Integer" for built-in type</li>
     *   <li>Fully qualified class name: "java.lang.String", "com.example.Person"</li>
     *   <li>Generic type with parameters: "List&lt;String&gt;", "Map&lt;String,Integer&gt;"</li>
     *   <li>Array types: "String[]", "int[][]", "List&lt;String&gt;[]"</li>
     *   <li>Special types: "JSON&lt;Person&gt;", "XML&lt;Order&gt;", "Type&lt;String&gt;"</li>
     *   <li>Optional types: "Optional&lt;String&gt;", "Nullable&lt;Integer&gt;"</li>
     *   <li>Collection types: "Set&lt;String&gt;", "List&lt;Person&gt;", "ImmutableList&lt;String&gt;"</li>
     *   <li>Map types: "Map&lt;String,Person&gt;", "Multimap&lt;String,Integer&gt;"</li>
     *   <li>Tuple types: "Pair&lt;String,Integer&gt;", "Triple&lt;String,Integer,Boolean&gt;"</li>
     *   <li>Enum with parameters: "Color(NAME)", "Color(ORDINAL)", "Color(CODE)" - selects the enum representation strategy</li>
     *   <li>Wildcard type arguments as reflection renders them: "List&lt;? extends Person&gt;" reads its elements as
     *       {@code Person}; "?" and "? super X" arguments are read as {@code Object}</li>
     * </ul>
     * <p>
     * Leading and trailing whitespace is ignored: {@code " Integer "} resolves to the same instance as
     * {@code "Integer"}. A blank name is rejected like an empty one.
     * </p>
     * <p>
     * The method caches Type objects for reuse. If the type pool size reaches multiples of 100,
     * a warning is logged about the pool size.
     * </p>
     * <p>
     * This method never returns {@code null}: an unrecognized or unresolvable name does <b>not</b> throw —
     * it fabricates and caches an {@link com.landawn.abacus.type.ObjectType ObjectType} fallback
     * (a usable, generic {@code Object}-backed {@code Type}) and returns it. An
     * {@link IllegalArgumentException} is thrown only for a {@code null}, empty or blank name, or for structurally
     * malformed generic/parameter syntax: a type-parameter count that does not match the container
     * ({@code "Map<String>"}, {@code "Pair<String>"}), constructor arguments on a resolved class whose handler
     * takes none ({@code "String(MD5)"}, {@code "Integer(1)"}, {@code "Date(yyyy)"} - only {@code Password(...)},
     * enums and custom handler classes accept them), or brackets other than {@code "[]"} pairs on an
     * unresolvable class name ({@code "String[ ]"}, {@code "String[]extra"}).
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<String> t1 = TypeFactory.getType("String");
     * t1.javaType();                  // returns String.class
     *
     * Type<List<String>> t2 = TypeFactory.getType("List<String>");
     * t2.name();            // returns "List<String>"
     * t2.declaringName();   // returns "List<String>"
     * t2.javaType();        // returns List.class
     *
     * Type<?> t3 = TypeFactory.getType("CompletelyUnknownName");
     * t3.isObject();                        // returns true (ObjectType fallback, not an exception)
     *
     * boolean sameInstance = TypeFactory.getType(" Integer ") == TypeFactory.getType("Integer");   // true
     * TypeFactory.getType("List<? extends Person>").elementType().javaType();   // Person.class
     *
     * TypeFactory.getType((String) null);   // throws IllegalArgumentException
     * TypeFactory.getType("");              // throws IllegalArgumentException
     * TypeFactory.getType("String(MD5)");   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the Java type represented by the requested {@code Type} object
     * @param typeName the name of the type to retrieve, with optional type parameters; must not be {@code null},
     *        empty or blank; surrounding whitespace is ignored
     * @return the Type object corresponding to the type name (never {@code null}; an
     *         {@link com.landawn.abacus.type.ObjectType ObjectType} is returned for unresolvable names)
     * @throws IllegalArgumentException if {@code typeName} is {@code null}, empty or blank, or if the type name format
     *         is structurally invalid (a mismatched type-parameter count, unused constructor arguments on a resolved
     *         class, misplaced brackets).
     * @see #getType(Class)
     * @see #registerType(String, Type)
     */
    public static <T> Type<T> getType(final String typeName) throws IllegalArgumentException {
        // Empty name (no class to derive from either) would otherwise NPE in the private overload;
        // reject it as IAE to honor the documented contract (IAE on invalid name, never returns null).
        N.checkArgNotEmpty(typeName, cs.typeName);

        return getType(typeName, null, null);
    }

    /**
     * Returns an already registered type without attempting to load or derive a class from the supplied name.
     *
     * <p>This non-creating lookup is useful when a name comes from an untrusted source and class loading must
     * not be triggered as a side effect. It observes the same concurrent registry used by {@link #getType(String)}.</p>
     *
     * @param typeName the exact registered type name; may be {@code null} or empty
     * @return the registered type, or {@code null} when {@code typeName} is {@code null} or empty, or when
     *         the exact name has not been registered
     */
    public static Type<?> getTypeIfPresent(final String typeName) {
        return Strings.isEmpty(typeName) ? null : typePool.get(typeName);
    }

    /**
     * Registers a custom Type for the specified target class with custom serialization/deserialization functions.
     * <p>
     * This method allows you to define how objects of a specific class should be converted to and from strings.
     * The provided functions will be used by the Type system for serialization and deserialization operations.
     * A JsonParser instance is provided to the functions for complex parsing scenarios.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeFactory.registerType(
     *     MyCustomClass.class,
     *     (obj, parser) -> obj.toCustomString(),
     *     (str, parser) -> MyCustomClass.fromString(str)
     * );
     * }</pre>
     *
     * @param <T> the Java type handled by the custom type registration
     * @param targetClass the class for which to register the custom type
     * @param toStringFunc the function to convert an object of type T to a String, receives the object and a JsonParser
     * @param fromStringFunc the function to convert a String to an object of type T, receives the string and a JsonParser
     * @throws IllegalArgumentException if {@code targetClass}, {@code toStringFunc}, or {@code fromStringFunc} is
     *         {@code null}, if {@code targetClass} has a built-in type, or if a type has already been resolved and
     *         cached for it by any prior lookup ({@code getType(Class)}, {@code getType(String)} with the class's
     *         canonical name, bean introspection, serialization) - register before the class is first used.
     * @see #registerType(Class, Function, Function)
     * @see #registerType(Class, Type)
     */
    public static <T> void registerType(final Class<T> targetClass, final BiFunction<? super T, JsonParser, String> toStringFunc,
            final BiFunction<? super String, JsonParser, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(toStringFunc, cs.toStringFunc);
        N.checkArgNotNull(fromStringFunc, cs.fromStringFunc);

        registerType(targetClass, new AbstractType<>(getClassName(targetClass)) {
            @Override
            public Class<T> javaType() {
                return targetClass;
            }

            @Override
            public String stringOf(final T x) {
                return toStringFunc.apply(x, Utils.jsonParser);
            }

            @Override
            public T valueOf(final String str) {
                return fromStringFunc.apply(str, Utils.jsonParser);
            }
        });
    }

    /**
     * Registers a custom Type for the specified class with simple serialization/deserialization functions.
     * <p>
     * This method provides a simpler alternative to {@link #registerType(Class, BiFunction, BiFunction)}
     * when you don't need access to a JsonParser instance for serialization/deserialization.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeFactory.registerType(
     *     EmailAddress.class,
     *     email -> email.getValue(),
     *     str -> new EmailAddress(str)
     * );
     * }</pre>
     *
     * @param <T> the Java type handled by the custom type registration
     * @param cls the class for which to register the custom type
     * @param toStringFunc the function to convert an object of type T to a String
     * @param fromStringFunc the function to convert a String to an object of type T
     * @throws IllegalArgumentException if {@code cls}, {@code toStringFunc}, or {@code fromStringFunc} is
     *         {@code null}, if {@code cls} has a built-in type, or if a type has already been resolved and cached
     *         for it by any prior lookup ({@code getType(Class)}, {@code getType(String)} with the class's canonical
     *         name, bean introspection, serialization) - register before the class is first used.
     * @see #registerType(Class, BiFunction, BiFunction)
     * @see #registerType(Class, Type)
     */
    public static <T> void registerType(final Class<T> cls, final Function<? super T, String> toStringFunc, final Function<? super String, T> fromStringFunc)
            throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);
        N.checkArgNotNull(toStringFunc, cs.toStringFunc);
        N.checkArgNotNull(fromStringFunc, cs.fromStringFunc);

        registerType(cls, new AbstractType<>(getClassName(cls)) {
            @Override
            public Class<T> javaType() {
                return cls;
            }

            @Override
            public String stringOf(final T x) {
                return toStringFunc.apply(x);
            }

            @Override
            public T valueOf(final String str) {
                return fromStringFunc.apply(str);
            }
        });
    }

    /**
     * Registers a custom Type implementation for the specified class.
     * <p>
     * This method allows you to register a fully custom Type implementation for a specific class.
     * The Type object defines all aspects of how the class is handled by the type system,
     * including serialization, deserialization, and type metadata.
     * </p>
     * <p>
     * Note: A type cannot be registered for a class that already has a type. That is the case for every
     * class with a built-in handler ({@code String}, {@code Object}, {@code Number}, {@code Map},
     * {@code HashMap}, {@code List}, {@code java.util.Date}, {@code java.util.Optional}, arrays, ...),
     * regardless of whether the class has been looked up yet, and for any class whose {@code Type} has
     * already been resolved and cached by a prior lookup by class ({@code getType(Class)}, bean
     * introspection, serialization of an instance).
     * Attempting to do so will throw an IllegalArgumentException, so custom types should be registered
     * before the class is first used.
     * </p>
     * <p>
     * The registered type becomes reachable by {@code cls}, by {@link Type#name()} and by the canonical
     * class name, so it is also used for the elements of {@code List<T>} / {@code Map<K, T>} / {@code T[]}
     * declarations (bean fields, {@link Type#ofList(Class)}). A fallback that an earlier lookup by canonical
     * name fabricated for {@code cls} is superseded by the registration.
     * </p>
     * <p>A successfully registered mapping is retained if a concurrent lookup was already
     * constructing a default type for the same class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MyClass> customType = new MyCustomType();
     * TypeFactory.registerType(MyClass.class, customType);
     * Type<MyClass> retrieved = TypeFactory.getType(MyClass.class);
     *
     * TypeFactory.registerType(Map.class, customMapType); // throws IllegalArgumentException: built-in
     * }</pre>
     *
     * @param <T> the Java type handled by the custom type registration
     * @param cls the class for which to register the type
     * @param type the Type implementation to register for the class
     * @throws IllegalArgumentException if {@code cls} or {@code type} is {@code null}, if a type is already
     *         registered or built in for the class, if a type has already been resolved and cached for the class
     *         by a prior lookup, or if a type with the same name (as returned by {@link Type#name()}) already
     *         exists.
     * @see #registerType(String, Type)
     * @see #getType(Class)
     */
    public static <T> void registerType(final Class<T> cls, final Type<T> type) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);
        N.checkArgNotNull(type, cs.type);

        // Fast-fail (preserves single-threaded message/order) before mutating the name pool. The class
        // cache alone is not enough: multi-handler built-ins (Map, List, HashMap, java.util.Optional...)
        // are never seeded there, so acceptance used to depend on whether anybody had looked the class up
        // yet, after which Type.of(Map.class) and Type.of("Map") disagreed.
        final String canonicalName = getClassName(cls);
        final Type<?> pooledType = typePool.get(canonicalName);

        if (refusesRegistrationByClass(cls, javaType2TypeCache.containsKey(cls), pooledType)) {
            throw new IllegalArgumentException("A type has already registered with class: " + cls);
        }

        final String registeredTypeName = N.checkArgNotEmpty(type.name(), "type.name()");
        registerTypeByIntrinsicName(registeredTypeName, type);

        // Atomic check-then-put closes the check-then-act race: a concurrent registration for the
        // same class cannot silently overwrite an existing mapping.
        if (javaType2TypeCache.putIfAbsent(cls, type) != null) {
            // The intrinsic name was installed before the class mapping so it must be rolled back
            // when a concurrent registration wins the class slot. Otherwise this method throws
            // while the rejected type remains globally retrievable by name.
            typePool.remove(registeredTypeName, type);
            throw new IllegalArgumentException("A type has already registered with class: " + cls);
        }

        if (pooledType != null) {
            // The class slot is won: retire the fallback that an earlier canonical-name lookup cached
            // (that instance only), otherwise List<cls> fields keep resolving to it.
            typePool.replace(canonicalName, pooledType, type);
        }
    }

    /**
     * Registers a custom Type with a specific type name and target class, using custom serialization functions with JsonParser.
     * <p>
     * This method allows you to register a type with a custom name that may differ from the class name.
     * The type is always reachable by the custom type name. It also becomes the type of {@code targetClass}
     * when nothing already answers for that class; a class that has a built-in handler ({@code Map},
     * {@code List}, {@code java.util.Date}, {@code LocalDateTime}, ...) or whose type a prior lookup has already
     * resolved keeps the type it has, so a lookup by class and a lookup by name cannot disagree.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeFactory.registerType(
     *     "CustomDate",
     *     LocalDate.class,
     *     (date, parser) -> date.format(DateTimeFormatter.ISO_DATE),
     *     (str, parser) -> LocalDate.parse(str, DateTimeFormatter.ISO_DATE)
     * );
     * }</pre>
     *
     * @param <T> the Java type handled by the custom type registration
     * @param typeName the custom name for this type registration; surrounding whitespace is ignored
     * @param targetClass the class that this type handles
     * @param toStringFunc the function to convert an object of type T to a String, receives the object and a JsonParser
     * @param fromStringFunc the function to convert a String to an object of type T, receives the string and a JsonParser
     * @throws IllegalArgumentException if {@code typeName} is {@code null}, empty or blank, if it is not a
     *         well-formed type declaration (e.g. {@code "X<Y"}), if a type is already registered under that name,
     *         or if {@code targetClass}, {@code toStringFunc}, or {@code fromStringFunc} is {@code null}.
     * @see #registerType(String, Class, Function, Function)
     * @see #registerType(String, Type)
     */
    public static <T> void registerType(String typeName, final Class<T> targetClass, final BiFunction<? super T, JsonParser, String> toStringFunc,
            final BiFunction<? super String, JsonParser, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotEmpty(typeName, cs.typeName);
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(toStringFunc, cs.toStringFunc);
        N.checkArgNotNull(fromStringFunc, cs.fromStringFunc);

        // registerType(String, Type) below publishes the trimmed name, and getType(String) trims what it looks up, so
        // normalize before the handler bakes the name into Type.name(): a padded name would otherwise keep its padding
        // in name()/xmlName() and be published a second time under an intrinsic-name key nothing can reach.
        typeName = N.checkArgNotEmpty(typeName.trim(), cs.typeName);

        final Type<T> type = new AbstractType<>(typeName) {
            @Override
            public Class<T> javaType() {
                return targetClass;
            }

            @Override
            public String stringOf(final T x) {
                return toStringFunc.apply(x, Utils.jsonParser);
            }

            @Override
            public T valueOf(final String str) {
                return fromStringFunc.apply(str, Utils.jsonParser);
            }
        };

        registerType(typeName, type);

        bindTypeToClassIfUnclaimed(targetClass, type);
    }

    /**
     * Registers a custom Type with a specific type name and target class, using simple serialization functions.
     * <p>
     * This method provides a simpler alternative to {@link #registerType(String, Class, BiFunction, BiFunction)}
     * when you don't need access to a JsonParser instance. The type is always reachable by the custom type
     * name. It also becomes the type of {@code targetClass} when nothing already answers for that class; a class
     * that has a built-in handler or whose type a prior lookup has already resolved keeps the type it has, so a
     * lookup by class and a lookup by name cannot disagree.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeFactory.registerType(
     *     "ISODate",
     *     LocalDate.class,
     *     date -> date.toString(),
     *     LocalDate::parse
     * );
     * }</pre>
     *
     * @param <T> the Java type handled by the custom type registration
     * @param typeName the custom name for this type registration; surrounding whitespace is ignored
     * @param targetClass the class that this type handles
     * @param toStringFunc the function to convert an object of type T to a String
     * @param fromStringFunc the function to convert a String to an object of type T
     * @throws IllegalArgumentException if {@code typeName} is {@code null}, empty or blank, if it is not a
     *         well-formed type declaration (e.g. {@code "X<Y"}), if a type is already registered under that name,
     *         or if {@code targetClass}, {@code toStringFunc}, or {@code fromStringFunc} is {@code null}.
     * @see #registerType(String, Class, BiFunction, BiFunction)
     * @see #registerType(String, Type)
     */
    public static <T> void registerType(String typeName, final Class<T> targetClass, final Function<? super T, String> toStringFunc,
            final Function<? super String, T> fromStringFunc) throws IllegalArgumentException {
        N.checkArgNotEmpty(typeName, cs.typeName);
        N.checkArgNotNull(targetClass, cs.targetClass);
        N.checkArgNotNull(toStringFunc, cs.toStringFunc);
        N.checkArgNotNull(fromStringFunc, cs.fromStringFunc);

        // registerType(String, Type) below publishes the trimmed name, and getType(String) trims what it looks up, so
        // normalize before the handler bakes the name into Type.name(): a padded name would otherwise keep its padding
        // in name()/xmlName() and be published a second time under an intrinsic-name key nothing can reach.
        typeName = N.checkArgNotEmpty(typeName.trim(), cs.typeName);

        final Type<T> type = new AbstractType<>(typeName) {
            @Override
            public Class<T> javaType() {
                return targetClass;
            }

            @Override
            public String stringOf(final T x) {
                return toStringFunc.apply(x);
            }

            @Override
            public T valueOf(final String str) {
                return fromStringFunc.apply(str);
            }
        };

        registerType(typeName, type);

        bindTypeToClassIfUnclaimed(targetClass, type);
    }

    /**
     * Binds {@code type} to {@code targetClass} for lookups by class, but only when nothing already answers for
     * that class.
     * <p>
     * The two {@code registerType(String, Class, ...)} overloads publish their type by name first and bind the
     * class opportunistically. Writing that binding unguarded is what let a lookup by class and a lookup by name
     * disagree: a class whose built-in handler is parameterized ({@code Map}, {@code List}, ...) is deliberately
     * never seeded into the class cache, so {@code putIfAbsent} won the class slot while the built-in kept
     * answering by name. The refusal test is therefore the one {@link #registerType(Class, Type)} applies.
     * </p>
     *
     * @param targetClass the class to bind the type to
     * @param type the type the caller has just published under its own name
     */
    private static void bindTypeToClassIfUnclaimed(final Class<?> targetClass, final Type<?> type) {
        final String canonicalName = getClassName(targetClass);
        final Type<?> pooledType = typePool.get(canonicalName);

        if (refusesRegistrationByClass(targetClass, javaType2TypeCache.containsKey(targetClass), pooledType)) {
            // A built-in handler, an already resolved and cached type, or a registered canonical entry keeps the
            // class. The alias the caller published stays reachable by name; only the class binding is dropped.
            return;
        }

        if (javaType2TypeCache.putIfAbsent(targetClass, type) == null && pooledType != null) {
            // The class slot is won: retire the fallback an earlier canonical-name lookup fabricated (that
            // instance only), otherwise List<targetClass> fields keep resolving to it.
            typePool.replace(canonicalName, pooledType, type);
        }
    }

    /**
     * Registers a Type implementation with a specific type name.
     * <p>
     * This method allows you to register a custom Type implementation with a specific name.
     * The type will be accessible via {@link #getType(String)} using the registered name.
     * </p>
     * <p>
     * Note: A type name must be unique. Attempting to register a type with a name that
     * already exists will throw an IllegalArgumentException.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MyClass> customType = new MyCustomType();
     * TypeFactory.registerType("CustomTypeName", customType);
     * Type<MyClass> retrieved = TypeFactory.getType("CustomTypeName");
     * }</pre>
     *
     * @param typeName the name to register the type under; surrounding whitespace is ignored, and the name must be
     *        one that {@link #getType(String)} can parse (a well-formed type declaration)
     * @param type the Type implementation to register
     * @throws IllegalArgumentException if typeName is {@code null}, empty or blank, if it is not a well-formed type
     *         declaration (e.g. {@code "X<Y"}), if type is {@code null}, if a type with the given name already
     *         exists, or if a type with the same name (as returned by {@link Type#name()}) already exists.
     * @see #registerType(Type)
     * @see #getType(String)
     */
    public static void registerType(String typeName, final Type<?> type) throws IllegalArgumentException {
        N.checkArgNotEmpty(typeName, cs.typeName);
        N.checkArgNotNull(type, cs.type);

        // getType(String) trims and parses its argument, so an alias published under a padded or unparsable
        // spelling ("  ", "X<Y") could be reached only by that exact spelling, or never. Validate before publishing.
        typeName = typeName.trim();
        N.checkArgNotEmpty(typeName, cs.typeName);
        TypeAttrParser.parse(typeName);

        final String intrinsicTypeName = N.checkArgNotEmpty(type.name(), "type.name()");

        if (typeName.equals(intrinsicTypeName)) {
            // typeName IS the type's own name: a single atomic registration (no separate alias slot).
            // Delegating avoids a self-conflict where the alias put would collide with the name just
            // inserted by registerType(type).
            registerTypeByIntrinsicName(intrinsicTypeName, type);
            return;
        }

        if (typePool.putIfAbsent(typeName, type) != null) {
            throw new IllegalArgumentException("A type has already registered with name: " + typeName);
        }

        try {
            registerTypeByIntrinsicName(intrinsicTypeName, type);
        } catch (final RuntimeException e) {
            typePool.remove(typeName, type);
            throw e;
        }
    }

    /**
     * Registers a Type implementation using its built-in name.
     * <p>
     * This method registers a Type using the name returned by the type's {@code name()} method.
     * This is typically used internally when registering built-in types or when the type
     * already has an appropriate name defined.
     * </p>
     * <p>
     * Note: The type's name must be unique. Attempting to register a type whose name
     * already exists in the type pool will throw an IllegalArgumentException.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MyClass> customType = new MyCustomType();
     * TypeFactory.registerType(customType);
     * Type<MyClass> retrieved = TypeFactory.getType("MyClass");
     * }</pre>
     *
     * @param type the Type implementation to register
     * @throws IllegalArgumentException if type is {@code null} or if a type with the same name already exists.
     * @see #registerType(String, Type)
     * @see Type#name()
     */
    public static void registerType(final Type<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);

        final String intrinsicTypeName = N.checkArgNotEmpty(type.name(), "type.name()");

        registerTypeByIntrinsicName(intrinsicTypeName, type);
    }

    /**
     * @throws IllegalArgumentException if a type is already registered under the intrinsic type name
     */
    private static void registerTypeByIntrinsicName(final String intrinsicTypeName, final Type<?> type) throws IllegalArgumentException {
        // Atomic check-then-put closes the check-then-act race: a concurrent registration for the
        // same name cannot silently overwrite an existing mapping (a startup-time op in practice).
        if (typePool.putIfAbsent(intrinsicTypeName, type) != null) {
            throw new IllegalArgumentException("A type has already registered with name: " + intrinsicTypeName);
        }

        //    if (!typePool.containsKey(getClassName(type.javaType()))) {
        //        typePool.put(getClassName(type.javaType()), type);
        //    }

        //    if (!classTypePool.containsKey(type.javaType())) {
        //        classTypePool.put(type.javaType(), type);
        //    }
    }

    /**
     * Suppresses default constructor; {@code TypeFactory} is a static utility and is not instantiable.
     */
    private TypeFactory() {
        // no instance.
    }
}
