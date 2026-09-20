package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.util.N;

public class TypeFactoryTest extends TestBase {

    public static class GenericArrayElement<T> {
        public T value;
    }

    public static class StringFirstArrayElement<T> {
        public T value;
    }

    public static class StringFirstArrayOwner<T> {
        public class Member<U> {
            public T ownerValue;
            public U value;
        }

        public class PlainMember {
            public T value;
        }
    }

    public static class StringFirstArrayFields {
        public StringFirstArrayElement<String>[] strings;
        public StringFirstArrayElement<List<String>>[] nested;
        public StringFirstArrayOwner<String>.Member<List<Integer>>[] members;
        public StringFirstArrayOwner<Long>.PlainMember[] plainMembers;
        public StringFirstArrayElement<?>[] unbounded;
        public StringFirstArrayElement<? extends List<String>[]>[] upper;
        public StringFirstArrayElement<? super List<String>[]>[] lower;
    }

    public static class ColdNestedArrayElement<T> {
        public T value;
    }

    public static class HandlerArgumentElement<T> {
        public T value;
    }

    @Test
    public void testColdNestedCollectionRetainsGenericBeanArrayMetadata() {
        final Type<?> type = TypeFactory.getType("List<" + ColdNestedArrayElement.class.getCanonicalName() + "<String>[]>");
        final List<?> values = (List<?>) type.valueOf("[[{\"value\":1}]]");
        assertEquals("1", ((ColdNestedArrayElement<?>[]) values.get(0))[0].value);
    }

    @Test
    public void testGenericBeanHandlerExpressionsRetainRawFallbackAndAliases() {
        final String alias = "AuditHandlerAlias_" + System.nanoTime() + "<String>";
        final Type<String> customType = new AbstractType<>("AuditHandlerIntrinsic_" + System.nanoTime()) {
            @Override
            public Class<String> javaType() {
                return String.class;
            }

            @Override
            public String stringOf(final String value) {
                return value;
            }

            @Override
            public String valueOf(final String value) {
                return value;
            }
        };
        TypeFactory.registerType(alias, customType);

        for (final String argument : new String[] { "JSON<String>", "JSON<List<String>>", "XML<String>", "Type<String>", "Clazz<String>", alias,
                "Factory(\"custom,argument\")" }) {
            final String name = HandlerArgumentElement.class.getCanonicalName() + "<" + argument + ">";
            final Type<?> type = assertDoesNotThrow(() -> TypeFactory.getType(name));
            assertEquals(HandlerArgumentElement.class, type.reflectType());
            assertSame(type, TypeFactory.getType(name));
        }

        assertSame(customType, TypeFactory.getType(alias));
    }

    @Test
    public void testGenericBeanArrayStringFirstLookupPreservesMetadata() throws Exception {
        for (final String fieldName : new String[] { "strings", "nested", "members", "plainMembers", "unbounded", "upper", "lower" }) {
            final java.lang.reflect.Type reflected = StringFirstArrayFields.class.getField(fieldName).getGenericType();
            final Type<?> fromName = TypeFactory.getType(TypeFactory.getJavaTypeName(reflected));
            final Type<?> fromReflection = TypeFactory.getType(reflected);
            final java.lang.reflect.Type component = ((java.lang.reflect.GenericArrayType) reflected).getGenericComponentType();
            assertSame(fromName, fromReflection);
            assertEquals(component, fromName.elementType().reflectType());
            assertEquals(fromName.elementType().reflectType(), component);
            assertEquals(component.hashCode(), fromName.elementType().reflectType().hashCode());
            assertEquals(component.getTypeName(), fromName.elementType().reflectType().getTypeName());

            if (fieldName.equals("strings")) {
                final StringFirstArrayElement<?>[] values = (StringFirstArrayElement<?>[]) fromName.valueOf("[{\"value\":1}]");
                assertEquals("1", values[0].value);
            } else if (fieldName.equals("nested")) {
                final StringFirstArrayElement<?>[] values = (StringFirstArrayElement<?>[]) fromName.valueOf("[{\"value\":[1]}]");
                assertEquals(List.of("1"), values[0].value);
            }
        }
    }

    @Test
    public void testGenericBeanArrayRetainsReflectionComponentMetadata() {
        final com.landawn.abacus.util.TypeReference<GenericArrayElement<String>[]> reference = new com.landawn.abacus.util.TypeReference<>() {
        };
        final GenericArrayElement<String>[] values = reference.type().valueOf("[{\"value\":1},null]");

        assertEquals("1", values[0].value);
        assertNull(values[1]);
        assertTrue(reference.type().elementType().reflectType() instanceof java.lang.reflect.ParameterizedType);
        assertEquals(String.class, reference.type().elementType().parameterTypes().get(0).javaType());

        final com.landawn.abacus.util.TypeReference<GenericArrayElement<String>[][]> nested = new com.landawn.abacus.util.TypeReference<>() {
        };
        assertEquals("2", nested.type().valueOf("[[{\"value\":2}]]")[0][0].value);
    }

    public static class NestedGenericBean<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(final T value) {
            this.value = value;
        }
    }

    public static class GenericOwner<T> {
        public class Member<U> {
            // Type declaration used only to exercise owner-type name formatting.
        }

        public class NonGenericMember {
            // A parameterized owner still makes this reflection type parameterized, with no
            // type arguments belonging to the member itself.
        }
    }

    private static class GenericNameHolder {
        NestedGenericBean<String> bean;
        GenericOwner<String>.Member<Integer> member;
        GenericOwner<Long>.Member<Integer> memberWithDifferentOwner;
        GenericOwner<String>.NonGenericMember nonGenericMember;
    }

    private static <T> Type<T> stubType(final String name, final Class<T> cls) {
        return new AbstractType<>(name) {
            @Override
            public Class<T> javaType() {
                return cls;
            }

            @Override
            public String stringOf(final T x) {
                return x == null ? null : String.valueOf(x);
            }

            @Override
            public T valueOf(final String str) {
                return null;
            }
        };
    }

    // ---- NEW TESTS targeting uncovered lines ----

    // Covers L550: typeName is null/empty so falls back to getClassName(cls)
    @Test
    public void testGetType_ReflectTypeForClass_FallsBackToClassName() {
        java.lang.reflect.Type t = Integer.class;
        Type<?> type = TypeFactory.getType(t);
        assertNotNull(type);
        assertEquals(Integer.class, type.javaType());
    }

    @Test
    public void testGetTypeWithString() {
        Type<String> type = TypeFactory.getType("String");
        assertNotNull(type);
    }

    @Test
    public void testRegisterTypeAliasConflictDoesNotRegisterIntrinsicName() {
        final String intrinsicName = "TypeFactoryAliasPartial_" + System.nanoTime();
        final Type<Object> customType = new AbstractType<>(intrinsicName) {
            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public String stringOf(final Object x) {
                return x == null ? null : x.toString();
            }

            @Override
            public Object valueOf(final String str) {
                return str;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType("String", customType));
        assertNotSame(customType, TypeFactory.getType(intrinsicName));
    }

    @Test
    public void testConcurrentClassRegistrationRollsBackRejectedTypeName() throws Exception {
        final String blockedName = "TypeFactoryRejectedConcurrent_" + System.nanoTime();
        final String winnerName = "TypeFactoryConcurrentWinner_" + System.nanoTime();
        final CountDownLatch blockedAtNameRegistration = new CountDownLatch(1);
        final CountDownLatch releaseBlockedRegistration = new CountDownLatch(1);
        final AtomicBoolean blockFirstNameCall = new AtomicBoolean(true);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        final Type<ConcurrentRegistrationClass> rejectedType = new AbstractType<>(blockedName) {
            @Override
            public String name() {
                if (blockFirstNameCall.compareAndSet(true, false)) {
                    blockedAtNameRegistration.countDown();

                    try {
                        releaseBlockedRegistration.await();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }

                return super.name();
            }

            @Override
            public Class<ConcurrentRegistrationClass> javaType() {
                return ConcurrentRegistrationClass.class;
            }

            @Override
            public String stringOf(final ConcurrentRegistrationClass x) {
                return x == null ? null : x.value;
            }

            @Override
            public ConcurrentRegistrationClass valueOf(final String str) {
                return new ConcurrentRegistrationClass(str);
            }
        };

        final Type<ConcurrentRegistrationClass> winningType = new AbstractType<>(winnerName) {
            @Override
            public Class<ConcurrentRegistrationClass> javaType() {
                return ConcurrentRegistrationClass.class;
            }

            @Override
            public String stringOf(final ConcurrentRegistrationClass x) {
                return x == null ? null : x.value;
            }

            @Override
            public ConcurrentRegistrationClass valueOf(final String str) {
                return new ConcurrentRegistrationClass(str);
            }
        };

        final Thread blocked = new Thread(() -> {
            try {
                TypeFactory.registerType(ConcurrentRegistrationClass.class, rejectedType);
            } catch (final Throwable e) {
                failure.set(e);
            }
        });

        blocked.setDaemon(true);
        blocked.start();
        assertTrue(blockedAtNameRegistration.await(5, TimeUnit.SECONDS));

        try {
            TypeFactory.registerType(ConcurrentRegistrationClass.class, winningType);
        } finally {
            releaseBlockedRegistration.countDown();
        }

        blocked.join(TimeUnit.SECONDS.toMillis(5));
        assertTrue(!blocked.isAlive());

        assertTrue(failure.get() instanceof IllegalArgumentException);
        assertSame(winningType, TypeFactory.getType(ConcurrentRegistrationClass.class));
        assertNotSame(rejectedType, TypeFactory.getType(blockedName));
    }

    private static final class ConcurrentRegistrationClass {
        final String value;

        ConcurrentRegistrationClass(final String value) {
            this.value = value;
        }
    }

    @Test
    public void testLookupPublicationDoesNotOverwriteConcurrentCustomRegistration() throws Exception {
        final String typeName = TypeFactory.getClassName(LookupPublicationRaceClass.class);
        final AtomicReference<Type<?>> lookupResult = new AtomicReference<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        TypeFactory.registerType(new BlockingLookupType("LookupPublicationRaceBase_" + System.nanoTime()));

        final Type<LookupPublicationRaceClass> customType = new AbstractType<>("LookupPublicationRaceAlias") {
            @Override
            public String name() {
                return typeName;
            }

            @Override
            public Class<LookupPublicationRaceClass> javaType() {
                return LookupPublicationRaceClass.class;
            }

            @Override
            public String stringOf(final LookupPublicationRaceClass x) {
                return x == null ? null : x.value;
            }

            @Override
            public LookupPublicationRaceClass valueOf(final String str) {
                return new LookupPublicationRaceClass(str);
            }
        };

        final Thread lookup = new Thread(() -> {
            try {
                lookupResult.set(TypeFactory.getType(LookupPublicationRaceClass.class));
            } catch (final Throwable e) {
                failure.set(e);
            }
        });
        lookup.setDaemon(true);
        lookup.start();
        assertTrue(BlockingLookupType.lookupBlocked.await(5, TimeUnit.SECONDS));

        try {
            TypeFactory.registerType(LookupPublicationRaceClass.class, customType);
        } finally {
            BlockingLookupType.releaseLookup.countDown();
        }

        lookup.join(TimeUnit.SECONDS.toMillis(5));
        assertTrue(!lookup.isAlive());
        assertNull(failure.get());
        assertSame(customType, lookupResult.get());
        assertSame(customType, TypeFactory.getType(typeName));
        assertSame(customType, TypeFactory.getType(LookupPublicationRaceClass.class));
    }

    private interface LookupPublicationRaceMarker {
        // Marker used to make the blocking Type the only assignable registered candidate.
    }

    private static final class LookupPublicationRaceClass implements LookupPublicationRaceMarker {
        final String value;

        LookupPublicationRaceClass(final String value) {
            this.value = value;
        }
    }

    private static final class BlockingLookupType extends AbstractType<Object> {
        static final CountDownLatch lookupBlocked = new CountDownLatch(1);
        static final CountDownLatch releaseLookup = new CountDownLatch(1);

        private final Class<Object> javaType;

        @SuppressWarnings("unchecked")
        BlockingLookupType(final String name) {
            super(name);
            javaType = (Class<Object>) (Class<?>) LookupPublicationRaceMarker.class;
        }

        @SuppressWarnings("unchecked")
        private BlockingLookupType(final Class<?> javaType) {
            super("LookupPublicationRaceSpeculative");
            this.javaType = (Class<Object>) javaType;
            lookupBlocked.countDown();

            try {
                releaseLookup.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }

        @Override
        public Class<Object> javaType() {
            return javaType;
        }

        @Override
        public String stringOf(final Object x) {
            return x == null ? null : x.toString();
        }

        @Override
        public Object valueOf(final String str) {
            return str;
        }
    }

    @Test
    public void testGetSheetTypeIsCached() {
        Type<?> type1 = TypeFactory.getType("Sheet<String, Integer, Long>");
        Type<?> type2 = TypeFactory.getType("Sheet<String, Integer, Long>");

        assertNotNull(type1);
        assertSame(type1, type2);
    }

    @Test
    public void testGetType_JsonAndXmlDefaultMap() {
        Type<?> jsonType = TypeFactory.getType("JSON");
        Type<?> xmlType = TypeFactory.getType("XML");

        assertNotNull(jsonType);
        assertNotNull(xmlType);
        assertTrue(jsonType.declaringName().startsWith("JSON"));
        assertTrue(xmlType.declaringName().startsWith("XML"));
    }

    @Test
    public void testGetType_IndexedAndTimedAliases() {
        Type<?> indexedType = TypeFactory.getType("Indexed");
        Type<?> timedType = TypeFactory.getType("Timed");

        assertNotNull(indexedType);
        assertNotNull(timedType);
        assertEquals("Indexed", indexedType.javaType().getSimpleName());
        assertEquals("Timed", timedType.javaType().getSimpleName());
    }

    // Covers Reader and InputStream branches
    @Test
    public void testGetType_ReaderAndInputStream() {
        assertNotNull(TypeFactory.getType(java.io.Reader.class));
        assertNotNull(TypeFactory.getType(java.io.InputStream.class));
    }

    // Covers Enum branch
    @Test
    public void testGetType_Enum() {
        assertNotNull(TypeFactory.getType(java.time.DayOfWeek.class));
    }

    // Covers java.util.Optional branch
    @Test
    public void testGetType_JdkOptional() {
        assertNotNull(TypeFactory.getType(java.util.Optional.class));
        assertNotNull(TypeFactory.getType("Optional<String>"));
    }

    // Covers Multiset branch
    @Test
    public void testGetType_Multiset() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Multiset.class));
        assertNotNull(TypeFactory.getType("Multiset<String>"));
    }

    // Covers ListMultimap branch
    @Test
    public void testGetType_ListMultimap() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.ListMultimap.class));
        assertNotNull(TypeFactory.getType("ListMultimap<String, Integer>"));
    }

    // Covers getType(String, Class, Type) - Collection/List/Set branches
    @Test
    public void testGetType_CollectionWithTypeParam() {
        Type<?> listType = TypeFactory.getType("List<String>");
        assertNotNull(listType);
        assertTrue(listType.isList());

        Type<?> setType = TypeFactory.getType("Set<Integer>");
        assertNotNull(setType);
        assertTrue(setType.isSet());

        Type<?> collType = TypeFactory.getType("Collection<Long>");
        assertNotNull(collType);
        assertTrue(collType.isCollection());
    }

    @Test
    public void testGetType_MapWithTypeParams() {
        Type<?> mapType = TypeFactory.getType("Map<String, Integer>");
        assertNotNull(mapType);
        assertTrue(mapType.isMap());

        Type<?> treeMapType = TypeFactory.getType("TreeMap<String, Integer>");
        assertNotNull(treeMapType);
        assertTrue(treeMapType.isMap());
    }

    @Test
    public void testGetType_ImmutableCollections() {
        assertNotNull(TypeFactory.getType("ImmutableList<String>"));
        assertNotNull(TypeFactory.getType("ImmutableSet<String>"));
        assertNotNull(TypeFactory.getType("ImmutableMap<String, Integer>"));
    }

    @Test
    public void testGetType_PairAndTripleAndTuple() {
        assertNotNull(TypeFactory.getType("Pair"));
        assertNotNull(TypeFactory.getType("Triple"));
        assertNotNull(TypeFactory.getType("Tuple1"));
        assertNotNull(TypeFactory.getType("Pair<String, Integer>"));
        assertNotNull(TypeFactory.getType("Triple<String, Integer, Long>"));
        assertNotNull(TypeFactory.getType("Tuple1<String>"));
        assertNotNull(TypeFactory.getType("Tuple2<String, Integer>"));
        assertNotNull(TypeFactory.getType("Tuple3<String, Integer, Long>"));
        assertNotNull(TypeFactory.getType("Tuple4<String, Integer, Long, Double>"));
        assertNotNull(TypeFactory.getType("Tuple5<String, Integer, Long, Double, Boolean>"));
        assertNotNull(TypeFactory.getType("Tuple6<String, Integer, Long, Double, Boolean, Byte>"));
        assertNotNull(TypeFactory.getType("Tuple7<String, Integer, Long, Double, Boolean, Byte, Short>"));
        assertNotNull(TypeFactory.getType("Tuple8<String, Integer, Long, Double, Boolean, Byte, Short, Float>"));
        assertNotNull(TypeFactory.getType("Tuple9<String, Integer, Long, Double, Boolean, Byte, Short, Float, Character>"));
    }

    @Test
    public void testGetType_SetMultimapAndMultimap() {
        assertNotNull(TypeFactory.getType("SetMultimap<String, Integer>"));
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.SetMultimap.class));
    }

    @Test
    public void testGetType_RangeType() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Range.class));
        assertNotNull(TypeFactory.getType("Range<Integer>"));
    }

    @Test
    public void testGetType_OptionalAndNullable() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.u.Optional.class));
        assertNotNull(TypeFactory.getType("Optional<String>"));
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.u.Nullable.class));
        assertNotNull(TypeFactory.getType("Nullable<String>"));
    }

    @Test
    public void testGetType_NullTypeNameFallsBackToClass() {
        // When typeName is empty, it falls back to class-based resolution
        Type<String> type = TypeFactory.getType(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.javaType());
    }

    @Test
    public void testGetType_UnknownClassNameReturnsObjectType() {
        // When the class name is not recognized, should return an ObjectType
        Type<?> type = TypeFactory.getType("NonExistentType_XYZ_ABC_123");
        assertNotNull(type);
    }

    @Test
    public void testGetType_TypeType() {
        Type<?> typeType = TypeFactory.getType("Type<String>");
        assertNotNull(typeType);
    }

    @Test
    public void testGetType_TypeRejectsMalformedAttributes() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Type<String, Integer>"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Type(extra)"));
    }

    @Test
    public void testGetType_JSONWithTypeParam() {
        Type<?> jsonType = TypeFactory.getType("JSON<List>");
        assertNotNull(jsonType);
    }

    @Test
    public void testGetType_XMLWithTypeParam() {
        Type<?> xmlType = TypeFactory.getType("XML<List>");
        assertNotNull(xmlType);
    }

    @Test
    public void testGetType_ClazzWithTypeParam() {
        Type<?> clazzType = TypeFactory.getType("Clazz<String>");
        assertNotNull(clazzType);
    }

    @Test
    public void testGetType_DatasetAndEntityId() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Dataset.class));
    }

    @Test
    public void testGetType_PrimitiveTypes() {
        assertNotNull(TypeFactory.getType(int.class));
        assertNotNull(TypeFactory.getType(long.class));
        assertNotNull(TypeFactory.getType(double.class));
        assertNotNull(TypeFactory.getType(boolean.class));
        assertNotNull(TypeFactory.getType(byte.class));
        assertNotNull(TypeFactory.getType(char.class));
        assertNotNull(TypeFactory.getType(float.class));
        assertNotNull(TypeFactory.getType(short.class));
    }

    @Test
    public void testGetType_ArrayTypes() {
        assertNotNull(TypeFactory.getType(int[].class));
        assertNotNull(TypeFactory.getType(String[].class));
        assertNotNull(TypeFactory.getType(Object[].class));
    }

    // Covers L623-L624: Password with no parameters returns cached type
    @Test
    public void testGetType_PasswordWithNoParam_ReturnsCachedType() {
        Type<?> t1 = TypeFactory.getType("Password");
        Type<?> t2 = TypeFactory.getType("Password");
        assertNotNull(t1);
        assertSame(t1, t2);
    }

    // Covers L632-L636: java.sql.Date/Time/Timestamp subclass branches
    @Test
    public void testGetType_SqlDateSubclasses() {
        // These are the actual sql types which should map to DateType/TimeType/TimestampType
        Type<?> dateType = TypeFactory.getType(java.sql.Date.class);
        Type<?> timeType = TypeFactory.getType(java.sql.Time.class);
        Type<?> tsType = TypeFactory.getType(java.sql.Timestamp.class);
        assertNotNull(dateType);
        assertNotNull(timeType);
        assertNotNull(tsType);
        assertEquals("Date", dateType.name());
        assertEquals("Time", timeType.name());
        assertEquals("Timestamp", tsType.name());
    }

    // Covers L637: java.util.Date branch (not sql subtype)
    @Test
    public void testGetType_JavaUtilDateBranch() {
        Type<?> type = TypeFactory.getType(java.util.Date.class);
        assertNotNull(type);
        assertEquals("JUDate", type.name());
    }

    // Covers L643: XMLGregorianCalendar branch
    @Test
    public void testGetType_XMLGregorianCalendarBranch() {
        assertNotNull(TypeFactory.getType(java.util.Calendar.class));
        Type<?> type = TypeFactory.getType(javax.xml.datatype.XMLGregorianCalendar.class);
        assertNotNull(type);
    }

    // Covers L649: ByteBuffer branch via string name
    @Test
    public void testGetType_ByteBufferByName() {
        Type<?> type = TypeFactory.getType("java.nio.ByteBuffer");
        assertNotNull(type);
        assertEquals(java.nio.ByteBuffer.class, type.javaType());
    }

    // Covers L783-L784: HBaseColumn with no type parameters returns default
    @Test
    public void testGetType_HBaseColumnNoTypeParam() {
        Type<?> type = TypeFactory.getType(com.landawn.abacus.util.HBaseColumn.class);
        assertNotNull(type);
    }

    // Covers L1075-L1079, L1083-L1084: ImmutableMapEntry
    @Test
    public void testGetType_ImmutableMapEntryNoTypeParam() {
        Type<?> type = TypeFactory.getType("Map.ImmutableEntry");
        assertNotNull(type);
    }

    // Covers L1089-L1093, L1097-L1098: Map.Entry
    @Test
    public void testGetType_MapEntryNoTypeParam() {
        Type<?> type = TypeFactory.getType("Map.Entry");
        assertNotNull(type);
    }

    // Covers L1108-L1110: Blob and Clob branches
    @Test
    public void testGetType_BlobAndClob() {
        assertNotNull(TypeFactory.getType(java.sql.Blob.class));
        assertNotNull(TypeFactory.getType(java.sql.Clob.class));
    }

    // Covers Multimap no-param and with type params
    @Test
    public void testGetType_MultimapNoParam() {
        Type<?> type = TypeFactory.getType(com.landawn.abacus.util.Multimap.class);
        assertNotNull(type);
    }

    // Covers Sheet with 3 type params
    @Test
    public void testGetType_SheetWithThreeTypeParams() {
        Type<?> type = TypeFactory.getType("Sheet<String, Integer, Long>");
        assertNotNull(type);
    }

    // Covers getType(Class) returns not null for Number subclass
    @Test
    public void testGetType_NumberSubclass() {
        assertNotNull(TypeFactory.getType(java.math.BigDecimal.class));
        assertNotNull(TypeFactory.getType(java.math.BigInteger.class));
    }

    // Covers object array via string name
    @Test
    public void testGetType_ObjectArrayViaStringName() {
        Type<?> type = TypeFactory.getType("String[]");
        assertNotNull(type);
        assertTrue(type.isArray());
    }

    @Test
    public void testGetTypeWithClassNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((Class<?>) null);
        });
    }

    @Test
    public void testGetTypeWithParameterizedType() throws NoSuchFieldException {
        class TypeHolder {
            List<String> list;
        }
        java.lang.reflect.Type paramType = TypeHolder.class.getDeclaredField("list").getGenericType();
        Type<?> type = TypeFactory.getType(paramType);
        assertNotNull(type);
    }

    @Test
    public void testParameterizedNestedClassUsesCanonicalName() throws NoSuchFieldException {
        final java.lang.reflect.Type reflectType = GenericNameHolder.class.getDeclaredField("bean").getGenericType();
        final String expectedName = NestedGenericBean.class.getCanonicalName() + "<String>";

        final Type<?> type = TypeFactory.getType(reflectType);

        assertEquals(expectedName, TypeFactory.getJavaTypeName(reflectType));
        assertEquals(expectedName, type.name());
        assertSame(type, TypeFactory.getType(expectedName));
    }

    @Test
    public void testParameterizedMemberClassIncludesGenericOwnerName() throws NoSuchFieldException {
        final java.lang.reflect.Type reflectType = GenericNameHolder.class.getDeclaredField("member").getGenericType();
        final java.lang.reflect.Type otherReflectType = GenericNameHolder.class.getDeclaredField("memberWithDifferentOwner").getGenericType();
        final String expectedName = GenericOwner.class.getCanonicalName() + "<String>.Member<Integer>";
        final String otherExpectedName = GenericOwner.class.getCanonicalName() + "<Long>.Member<Integer>";

        final Type<?> type = assertDoesNotThrow(() -> TypeFactory.getType(reflectType));
        final Type<?> otherType = assertDoesNotThrow(() -> TypeFactory.getType(otherReflectType));

        assertEquals(expectedName, TypeFactory.getJavaTypeName(reflectType));
        assertEquals(expectedName, type.name());
        assertEquals(otherExpectedName, TypeFactory.getJavaTypeName(otherReflectType));
        assertEquals(otherExpectedName, otherType.name());
        assertNotSame(type, otherType);
    }

    @Test
    public void testNonGenericMemberOfParameterizedOwnerDoesNotAddEmptyTypeArguments() throws NoSuchFieldException {
        final java.lang.reflect.Type reflectType = GenericNameHolder.class.getDeclaredField("nonGenericMember").getGenericType();
        final String expectedName = GenericOwner.class.getCanonicalName() + "<String>.NonGenericMember";

        final Type<?> type = assertDoesNotThrow(() -> TypeFactory.getType(reflectType));

        assertEquals(expectedName, TypeFactory.getJavaTypeName(reflectType));
        assertEquals(expectedName, type.name());
    }

    @Test
    public void testGetTypeWithStringNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((String) null);
        });
    }

    // M18: getType(java.lang.reflect.Type) must reject null for contract parity with getType(Class)/getType(String).
    @Test
    public void testGetTypeWithReflectTypeNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((java.lang.reflect.Type) null);
        });
    }

    // M16: concurrent registration of the SAME name must not silently overwrite; exactly one wins, others throw IAE.
    @Test
    public void testRegisterType_ConcurrentSameNameIsAtomic() throws InterruptedException {
        final int threadCount = 12;
        final String typeName = "M16_AtomicRegisterTest_" + System.nanoTime();
        final java.util.concurrent.atomic.AtomicInteger successes = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threadCount);
        final Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                final Type<Object> t = new AbstractType<>(typeName) {
                    @Override
                    public Class<Object> javaType() {
                        return Object.class;
                    }

                    @Override
                    public String stringOf(Object x) {
                        return x == null ? null : x.toString();
                    }

                    @Override
                    public Object valueOf(String str) {
                        return str;
                    }
                };
                try {
                    start.await();
                    TypeFactory.registerType(t);
                    successes.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads[i].start();
        }

        start.countDown();
        done.await();

        assertEquals(1, successes.get(), "exactly one concurrent registration should succeed");
        assertEquals(threadCount - 1, failures.get(), "all other concurrent registrations should throw IAE");
        assertNotNull(TypeFactory.getType(typeName));
    }

    @ParameterizedTest(name = "rejects invalid type declaration: {0}")
    @ValueSource(strings = { "Clazz<String, Integer>", "Clazz", "Password<String>", "Password(SHA256, MD5)", "java.time.DayOfWeek(true, false)",
            "java.util.Optional<String, Integer>", "com.landawn.abacus.util.u.Optional<String, Integer>", "com.landawn.abacus.util.u.Nullable<String, Integer>",
            "Multiset<String, Integer>", "ListMultimap<String>", "SetMultimap<String>", "Range<String, Integer>",
            "com.landawn.abacus.util.HBaseColumn<String, Integer>", "ImmutableList<String, Integer>", "ImmutableSet<String, Integer>", "ImmutableMap<String>",
            "Map<String>", "Triple<String, Integer>", "Tuple1<String, Integer>", "Tuple2<String>", "Tuple3<String, Integer>", "Tuple4<String, Integer, Long>",
            "Tuple5<String, Integer, Long, Double>", "Tuple6<String, Integer, Long, Double, Boolean>", "Tuple7<String, Integer, Long, Double, Boolean, Byte>",
            "Tuple8<String, Integer, Long, Double, Boolean, Byte, Short>", "Tuple9<String, Integer, Long, Double, Boolean, Byte, Short, Float>",
            "Indexed<String, Integer>", "Timed<String, Integer>", "Map.ImmutableEntry<String>", "Map.Entry<String>", "Clazz<String>(extra)",
            "JSON<String>(extra)", "XML<String>(extra)", "JSON<String, Integer>", "XML<String, Integer>", "com.landawn.abacus.util.Multimap<String>",
            "Sheet<String, Integer>", "java.time.DayOfWeek<String>", "Clazz<String>(foo)",
            // review fixes 2026-09-06 (T1-05): unused constructor arguments on a resolved scalar class, bracket junk
            "String(MD5)", "String(255)", "Integer(1)", "Long(x)", "BigDecimal(2)", "StringBuilder(100)", "Date(yyyy)", "java.util.Date(x)",
            "java.util.Calendar(x)", "Object(x)", "com.landawn.abacus.type.TypeFactoryTest.GenericArrayElement(x)", "String[", "String]", "String[ ]",
            "String[]extra", "   ", "\t" })
    public void testGetTypeRejectsInvalidTypeDeclaration(final String typeName) {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(typeName));
    }

    @Test
    public void testRegisterTypeWithClassAndBiFunctions() {
        assertDoesNotThrow(() -> {
            class CustomClass {
                String value = "test";
            }

            BiFunction<CustomClass, JsonParser, String> toStringFunc = (obj, parser) -> obj.value;
            BiFunction<String, JsonParser, CustomClass> fromStringFunc = (str, parser) -> {
                CustomClass obj = new CustomClass();
                obj.value = str;
                return obj;
            };

            TypeFactory.registerType(CustomClass.class, toStringFunc, fromStringFunc);
        });
    }

    @Test
    public void testRegisterTypeWithClassAndFunctions() {
        assertDoesNotThrow(() -> {
            class CustomClass2 {
                String value = "test";
            }

            Function<CustomClass2, String> toStringFunc = obj -> obj.value;
            Function<String, CustomClass2> fromStringFunc = str -> {
                CustomClass2 obj = new CustomClass2();
                obj.value = str;
                return obj;
            };

            TypeFactory.registerType(CustomClass2.class, toStringFunc, fromStringFunc);
        });
    }

    @Test
    public void testRegisterTypeWithStringAndClassAndBiFunctions() {
        assertDoesNotThrow(() -> {
            class CustomClass4 {
                String value = "test";
            }

            BiFunction<CustomClass4, JsonParser, String> toStringFunc = (obj, parser) -> obj.value;
            BiFunction<String, JsonParser, CustomClass4> fromStringFunc = (str, parser) -> {
                CustomClass4 obj = new CustomClass4();
                obj.value = str;
                return obj;
            };

            TypeFactory.registerType("CustomType4", CustomClass4.class, toStringFunc, fromStringFunc);
        });
    }

    @Test
    public void testRegisterTypeWithStringAndClassAndFunctions() {
        assertDoesNotThrow(() -> {
            class CustomClass5 {
                String value = "test";
            }

            Function<CustomClass5, String> toStringFunc = obj -> obj.value;
            Function<String, CustomClass5> fromStringFunc = str -> {
                CustomClass5 obj = new CustomClass5();
                obj.value = str;
                return obj;
            };

            TypeFactory.registerType("CustomType5", CustomClass5.class, toStringFunc, fromStringFunc);
        });
    }

    @Test
    public void testRegisterTypeRejectsNullConversionFunctionsBeforePublishing() {
        class ClassBiFunctionTarget {
            final String value;

            ClassBiFunctionTarget(final String value) {
                this.value = value;
            }
        }

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(ClassBiFunctionTarget.class,
                (BiFunction<ClassBiFunctionTarget, JsonParser, String>) null, (str, parser) -> new ClassBiFunctionTarget(str)));
        TypeFactory.registerType(ClassBiFunctionTarget.class, (value, parser) -> value.value, (str, parser) -> new ClassBiFunctionTarget(str));

        class ClassFunctionTarget {
            final String value;

            ClassFunctionTarget(final String value) {
                this.value = value;
            }
        }

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(ClassFunctionTarget.class,
                (Function<ClassFunctionTarget, String>) value -> value.value, (Function<String, ClassFunctionTarget>) null));
        TypeFactory.registerType(ClassFunctionTarget.class, value -> value.value, ClassFunctionTarget::new);

        class NamedBiFunctionTarget {
            final String value;

            NamedBiFunctionTarget(final String value) {
                this.value = value;
            }
        }

        final String namedBiFunctionType = "TypeFactoryNullNamedBiFunction_" + System.nanoTime();
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(namedBiFunctionType, NamedBiFunctionTarget.class,
                        (BiFunction<NamedBiFunctionTarget, JsonParser, String>) (value, parser) -> value.value,
                        (BiFunction<String, JsonParser, NamedBiFunctionTarget>) null));
        TypeFactory.registerType(namedBiFunctionType, NamedBiFunctionTarget.class, (value, parser) -> value.value,
                (str, parser) -> new NamedBiFunctionTarget(str));

        class NamedFunctionTarget {
            final String value;

            NamedFunctionTarget(final String value) {
                this.value = value;
            }
        }

        final String namedFunctionType = "TypeFactoryNullNamedFunction_" + System.nanoTime();
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(namedFunctionType, NamedFunctionTarget.class,
                (Function<NamedFunctionTarget, String>) null, NamedFunctionTarget::new));
        TypeFactory.registerType(namedFunctionType, NamedFunctionTarget.class, value -> value.value, NamedFunctionTarget::new);
    }

    @Test
    public void testRegisterTypeRejectsEmptyNamesBeforePublishing() {
        final String intrinsicName = "TypeFactoryEmptyAliasIntrinsic_" + System.nanoTime();
        final Type<Object> validlyNamedType = new AbstractType<>(intrinsicName) {
            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public String stringOf(final Object value) {
                return N.stringOf(value);
            }

            @Override
            public Object valueOf(final String str) {
                return str;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType("", validlyNamedType));
        assertNull(TypeFactory.getTypeIfPresent(intrinsicName));

        final Type<Object> unnamedType = new AbstractType<>("") {
            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public String stringOf(final Object value) {
                return N.stringOf(value);
            }

            @Override
            public Object valueOf(final String str) {
                return str;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(unnamedType));
    }

    @Test
    public void testRegisterTypeRejectsNullIntrinsicNamesWithIllegalArgumentException() {
        final Type<Object> directlyRegisteredType = new AbstractType<>("TypeFactoryNullIntrinsicDirect") {
            @Override
            public String name() {
                return null;
            }

            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public String stringOf(final Object value) {
                return N.stringOf(value);
            }

            @Override
            public Object valueOf(final String str) {
                return str;
            }
        };

        final RuntimeException directError = assertThrows(RuntimeException.class, () -> TypeFactory.registerType(directlyRegisteredType));
        assertTrue(directError instanceof IllegalArgumentException);

        final String alias = "TypeFactoryNullIntrinsicAlias_" + System.nanoTime();
        final Type<Object> aliasedType = new AbstractType<>("TypeFactoryNullIntrinsicAliased") {
            @Override
            public String name() {
                return null;
            }

            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public String stringOf(final Object value) {
                return N.stringOf(value);
            }

            @Override
            public Object valueOf(final String str) {
                return str;
            }
        };

        final RuntimeException aliasError = assertThrows(RuntimeException.class, () -> TypeFactory.registerType(alias, aliasedType));
        assertNull(TypeFactory.getTypeIfPresent(alias));
        assertTrue(aliasError instanceof IllegalArgumentException);
    }

    // Covers L1383/L1388: registerType(Class, BiFunction, BiFunction) lambda body execution
    @Test
    public void testRegisterType_BiFunctionLambdaBodies() {
        class BiFuncClass {
            String val;

            BiFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType(BiFuncClass.class, (obj, parser) -> obj.val, (str, parser) -> new BiFuncClass(str));
        Type<BiFuncClass> type = TypeFactory.getType(BiFuncClass.class);
        assertNotNull(type);
        BiFuncClass obj = new BiFuncClass("hello");
        assertEquals("hello", type.stringOf(obj));
        assertEquals("hello", type.valueOf("hello").val);
    }

    // Covers L1431/L1436: registerType(Class, Function, Function) lambda body execution
    @Test
    public void testRegisterType_FunctionLambdaBodies() {
        class FuncClass {
            String val;

            FuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType(FuncClass.class, obj -> obj.val, str -> new FuncClass(str));
        Type<FuncClass> type = TypeFactory.getType(FuncClass.class);
        assertNotNull(type);
        FuncClass obj = new FuncClass("world");
        assertEquals("world", type.stringOf(obj));
        assertEquals("world", type.valueOf("world").val);
    }

    // Covers L1523/L1528: registerType(String, Class, BiFunction, BiFunction) lambda body execution
    @Test
    public void testRegisterType_NamedBiFunctionLambdaBodies() {
        class NamedBiFuncClass {
            String val;

            NamedBiFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType("NamedBiFuncClass_TypeTest", NamedBiFuncClass.class,
                (BiFunction<NamedBiFuncClass, JsonParser, String>) (obj, parser) -> obj.val,
                (BiFunction<String, JsonParser, NamedBiFuncClass>) (str, parser) -> new NamedBiFuncClass(str));
        Type<NamedBiFuncClass> type = TypeFactory.getType("NamedBiFuncClass_TypeTest");
        assertNotNull(type);
        NamedBiFuncClass obj = new NamedBiFuncClass("test");
        assertEquals("test", type.stringOf(obj));
        assertEquals("test", type.valueOf("test").val);
    }

    // Covers L1582/L1587: registerType(String, Class, Function, Function) lambda body execution
    @Test
    public void testRegisterType_NamedFunctionLambdaBodies() {
        class NamedFuncClass {
            String val;

            NamedFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType("NamedFuncClass_TypeTest", NamedFuncClass.class, (Function<NamedFuncClass, String>) obj -> obj.val,
                (Function<String, NamedFuncClass>) str -> new NamedFuncClass(str));
        Type<NamedFuncClass> type = TypeFactory.getType("NamedFuncClass_TypeTest");
        assertNotNull(type);
        NamedFuncClass obj = new NamedFuncClass("test2");
        assertEquals("test2", type.stringOf(obj));
        assertEquals("test2", type.valueOf("test2").val);
    }

    @Test
    public void testRegisterTypeWithClassAndType() {
        class CustomClass3 {
        }
        assertDoesNotThrow(() -> TypeFactory.registerType(CustomClass3.class, stubType("CustomClass3", CustomClass3.class)));
    }

    @Test
    public void testRegisterType() {
        assertDoesNotThrow(() -> TypeFactory.registerType(stubType("UniqueTypeName456", Object.class)));
    }

    // Covers L1472: registerType(Class, Type) throws when class already registered
    @Test
    public void testRegisterTypeWithClass_ThrowsIfAlreadyRegistered() {
        // String.class already has a registered type
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.registerType(String.class, TypeFactory.getType(String.class));
        });
    }

    // Covers L1627: registerType(String, Type) throws when name already registered
    @Test
    public void testRegisterTypeWithName_ThrowsIfAlreadyRegistered() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.registerType("String", TypeFactory.getType(String.class));
        });
    }

    @Test
    public void testGetType_SheetWithParameters_Throws() {
        // Regression: Sheet type must reject constructor-style parameters like its siblings (#18).
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Sheet<String, Integer, Long>(foo)"));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testGetType_SimpleNameArray() {
        // regression: "UUID[]" produced a self-inconsistent type named "UUID[][]" with Object
        // (String-deserialized) elements because the fallback never consulted the type pool
        // for the COMPONENT name
        final Type<java.util.UUID[]> arrayType = TypeFactory.getType("UUID[]");

        org.junit.jupiter.api.Assertions.assertEquals("UUID[]", arrayType.name());

        final java.util.UUID uuid = java.util.UUID.randomUUID();
        final Object[] parsed = arrayType.valueOf("[\"" + uuid + "\"]");

        org.junit.jupiter.api.Assertions.assertEquals(uuid, parsed[0]);
    }

    @Test
    public void testGetType_JsonWrappedSimpleName() {
        // regression: "JSON<UUID>" threw "No class found by name: UUID" because the constructor
        // re-resolved the class via ClassUtil.forName instead of the TypeFactory
        final Type<?> jsonType = TypeFactory.getType("JSON<UUID>");

        org.junit.jupiter.api.Assertions.assertEquals(java.util.UUID.class, jsonType.javaType());
    }

    @Test
    public void testGetType_ParameterizedArrayPreservesComponentType() {
        for (final String malformed : new String[] { "Factory<String>(value)[]", "Factory<String>(value)[][]", "List<String>[]junk[]", "List<String>[3][]",
                "List<String>[]>", "List<>[]", "List<Map<String,>[]>" }) {
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(malformed), malformed);
        }

        final Type<java.util.List<String>[]> arrayType = TypeFactory.getType("List<String>[]");

        assertEquals(java.util.List[].class, arrayType.javaType());
        assertEquals(String.class, arrayType.elementType().elementType().javaType());
        assertEquals(java.util.List.of("1"), arrayType.valueOf("[[1]]")[0]);
        assertEquals(String.class, TypeFactory.getType(" List<String>[] ").elementType().elementType().javaType());

        final Type<java.util.List<String>[][]> nestedType = TypeFactory.getType("List<String>[][]");
        assertEquals(java.util.List.of("2"), nestedType.valueOf("[[[2]]]")[0][0]);
    }

    // ------------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (T1-01, T1-02, T1-03, T1-04, T1-05, T1-07, T1-11). The registries are
    // static and shared by the whole JVM, so every test below uses its own classes / names.
    // ------------------------------------------------------------------------------------------------

    public static class ReviewFixesPerson {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    public static class ReviewFixesEmployee extends ReviewFixesPerson {
        private String dept;

        public String getDept() {
            return dept;
        }

        public void setDept(final String dept) {
            this.dept = dept;
        }
    }

    public static class ReviewFixesWildcardHolder {
        private List<? extends ReviewFixesPerson> people;
        private java.util.Set<? extends ReviewFixesPerson> crowd;
        private java.util.Map<String, ? extends ReviewFixesPerson> byKey;
        private List<? extends java.math.BigDecimal> amounts;
        private List<?> any;
        private List<? super Integer> lower;

        public List<? extends ReviewFixesPerson> getPeople() {
            return people;
        }

        public void setPeople(final List<? extends ReviewFixesPerson> people) {
            this.people = people;
        }

        public java.util.Set<? extends ReviewFixesPerson> getCrowd() {
            return crowd;
        }

        public void setCrowd(final java.util.Set<? extends ReviewFixesPerson> crowd) {
            this.crowd = crowd;
        }

        public java.util.Map<String, ? extends ReviewFixesPerson> getByKey() {
            return byKey;
        }

        public void setByKey(final java.util.Map<String, ? extends ReviewFixesPerson> byKey) {
            this.byKey = byKey;
        }

        public List<? extends java.math.BigDecimal> getAmounts() {
            return amounts;
        }

        public void setAmounts(final List<? extends java.math.BigDecimal> amounts) {
            this.amounts = amounts;
        }

        public List<?> getAny() {
            return any;
        }

        public void setAny(final List<?> any) {
            this.any = any;
        }

        public List<? super Integer> getLower() {
            return lower;
        }

        public void setLower(final List<? super Integer> lower) {
            this.lower = lower;
        }
    }

    /** Registered by class with a Type whose name() differs from the canonical class name. */
    public static final class ReviewFixesCanonicalValue {
        final String value;

        ReviewFixesCanonicalValue(final String value) {
            this.value = value;
        }
    }

    public static class ReviewFixesCanonicalHolder {
        private List<ReviewFixesCanonicalValue> list;
        private java.util.Map<String, ReviewFixesCanonicalValue> map;
        private ReviewFixesCanonicalValue[] arr;
        private ReviewFixesCanonicalValue single;

        public List<ReviewFixesCanonicalValue> getList() {
            return list;
        }

        public void setList(final List<ReviewFixesCanonicalValue> list) {
            this.list = list;
        }

        public java.util.Map<String, ReviewFixesCanonicalValue> getMap() {
            return map;
        }

        public void setMap(final java.util.Map<String, ReviewFixesCanonicalValue> map) {
            this.map = map;
        }

        public ReviewFixesCanonicalValue[] getArr() {
            return arr;
        }

        public void setArr(final ReviewFixesCanonicalValue[] arr) {
            this.arr = arr;
        }

        public ReviewFixesCanonicalValue getSingle() {
            return single;
        }

        public void setSingle(final ReviewFixesCanonicalValue single) {
            this.single = single;
        }
    }

    public static class ReviewFixesYearHolder {
        private List<java.time.Year> years;
        private java.util.Map<String, java.time.Year> byKey;
        private java.time.Year[] arr;
        private java.time.Year year;

        public List<java.time.Year> getYears() {
            return years;
        }

        public void setYears(final List<java.time.Year> years) {
            this.years = years;
        }

        public java.util.Map<String, java.time.Year> getByKey() {
            return byKey;
        }

        public void setByKey(final java.util.Map<String, java.time.Year> byKey) {
            this.byKey = byKey;
        }

        public java.time.Year[] getArr() {
            return arr;
        }

        public void setArr(final java.time.Year[] arr) {
            this.arr = arr;
        }

        public java.time.Year getYear() {
            return year;
        }

        public void setYear(final java.time.Year year) {
            this.year = year;
        }
    }

    /** Registered through the named overload registerType("MoneyT...", Money.class, f, g). */
    public static final class ReviewFixesMoney {
        final String amount;

        ReviewFixesMoney(final String amount) {
            this.amount = amount;
        }
    }

    public static class ReviewFixesMoneyHolder {
        private List<ReviewFixesMoney> list;
        private java.util.Map<String, ReviewFixesMoney> map;

        public List<ReviewFixesMoney> getList() {
            return list;
        }

        public void setList(final List<ReviewFixesMoney> list) {
            this.list = list;
        }

        public java.util.Map<String, ReviewFixesMoney> getMap() {
            return map;
        }

        public void setMap(final java.util.Map<String, ReviewFixesMoney> map) {
            this.map = map;
        }
    }

    public static final class ReviewFixesLookedUpByClassFirst {
    }

    public static final class ReviewFixesLookedUpByNameFirst {
    }

    public static final class ReviewFixesNeverLookedUp {
    }

    public static final class ReviewFixesRollbackTarget {
    }

    public static final class SelfReviewFabricatedCanonicalEntry {
    }

    private static <T> Type<T> reviewFixesCustomType(final Class<T> cls, final String name, final Function<? super String, T> fromString,
            final Function<? super T, String> toString) {
        return new AbstractType<>(name) {
            @Override
            public Class<T> javaType() {
                return cls;
            }

            @Override
            public String stringOf(final T x) {
                return x == null ? null : toString.apply(x);
            }

            @Override
            public T valueOf(final String str) {
                return str == null ? null : fromString.apply(str);
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void reviewFixesAssertBuiltInCannotBeOverridden(final Class<?> cls) {
        final String name = "ReviewFixesOverride_" + cls.getSimpleName().replace("[]", "Array") + "_" + System.nanoTime();
        final Type<Object> custom = reviewFixesCustomType((Class<Object>) (Class) cls, name, s -> null, x -> null);

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType((Class<Object>) (Class) cls, custom));
        assertTrue(error.getMessage().contains("already registered with class"), error.getMessage());
        // Refused before anything was published: neither the name nor the class slot changed.
        assertNull(TypeFactory.getTypeIfPresent(name));
        assertNotSame(custom, TypeFactory.getType(cls));
        assertSame(TypeFactory.getType(cls), TypeFactory.getType(TypeFactory.getClassName(cls)));
    }

    @Test
    public void reviewFixes20260906_registerTypeByClassRefusesBuiltInsRegardlessOfLookupOrder() {
        // T1-01: never depends on whether Type.of(X.class) ran earlier in this JVM.
        for (final Class<?> cls : new Class<?>[] { java.util.Map.class, List.class, java.util.HashMap.class, java.util.ArrayList.class, java.util.Set.class,
                java.util.Collection.class, java.util.Optional.class, Object.class, java.util.Date.class, java.time.LocalDateTime.class, String[].class,
                int[].class, Number.class, String.class, Integer.class }) {
            reviewFixesAssertBuiltInCannotBeOverridden(cls);
        }
        // Enum.class itself cannot even be looked up (EnumType rejects it), so only the refusal is asserted.
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(Enum.class,
                reviewFixesCustomType(Enum.class, "ReviewFixesOverride_Enum_" + System.nanoTime(), s -> null, x -> null)));

        // The built-in handlers still answer, by class and by name, and the parser still uses them.
        final Type<java.util.Map<Object, Object>> mapType = TypeFactory.getType(java.util.Map.class);
        assertTrue(mapType.isMap());
        assertEquals(java.util.Map.of("a", 1), mapType.valueOf("{\"a\": 1}"));
        assertSame(mapType, TypeFactory.getType("java.util.Map"));
        assertEquals(java.util.Map.of("a", 1), N.fromJson("{\"a\": 1}", java.util.Map.class));
        assertEquals(List.of(1, 2), N.fromJson("[1, 2]", List.class));
        assertTrue(TypeFactory.getType(java.util.Optional.class).isOptionalOrNullable());
        assertEquals(List.of("a"), Type.ofList(String.class).valueOf("[\"a\"]"));

        // A user class that was never looked up still registers, and the registration is reachable
        // by class, by name and by canonical name.
        final String userName = "ReviewFixesNeverLookedUp_" + System.nanoTime();
        final Type<ReviewFixesNeverLookedUp> userType = reviewFixesCustomType(ReviewFixesNeverLookedUp.class, userName, s -> new ReviewFixesNeverLookedUp(),
                x -> "u");
        TypeFactory.registerType(ReviewFixesNeverLookedUp.class, userType);
        assertSame(userType, TypeFactory.getType(ReviewFixesNeverLookedUp.class));
        assertSame(userType, TypeFactory.getType(userName));
        assertSame(userType, TypeFactory.getType(ReviewFixesNeverLookedUp.class.getCanonicalName()));

        // The documented named overload (class-level javadoc: "ISODateTime", LocalDateTime.class) keeps working.
        final String isoName = "ReviewFixesISODateTime_" + System.nanoTime();
        assertDoesNotThrow(
                () -> TypeFactory.registerType(isoName, java.time.LocalDateTime.class, dt -> dt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        str -> java.time.LocalDateTime.parse(str, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        final Type<java.time.LocalDateTime> isoType = TypeFactory.getType(isoName);
        assertEquals(java.time.LocalDateTime.class, isoType.javaType());
        assertEquals(java.time.LocalDateTime.of(2020, 1, 2, 3, 4, 5), isoType.valueOf("2020-01-02T03:04:05"));
    }

    @Test
    public void selfReview20260907_builtInsWhosePoolEntryLooksFabricatedAreStillRefused() {
        // T1-01 follow-up. The built-in pool entry for java.lang.Object IS an ObjectType for Object.class, and
        // the one a name lookup caches for java.lang.Number IS a NumberType for Number.class, so the
        // "fabricated fallback" test cannot tell them from an entry a lookup fabricated. The guard is asserted
        // through refusesRegistrationByClass with alreadyCached=false so that a JVM which happens to have
        // resolved these classes earlier (which makes registerType throw for the unrelated T1-07 reason)
        // cannot make this pass by accident.
        assertTrue(TypeFactory.refusesRegistrationByClass(Object.class, false, TypeFactory.getType("java.lang.Object")));
        assertTrue(TypeFactory.refusesRegistrationByClass(Number.class, false, TypeFactory.getType("java.lang.Number")));
        assertTrue(TypeFactory.refusesRegistrationByClass(Object.class, false, null));
        assertTrue(TypeFactory.refusesRegistrationByClass(Number.class, false, null));

        // A genuinely fabricated canonical entry (a lookup by name for a class with no built-in) must still
        // be supersedable, or T1-02 would be undone.
        final Class<SelfReviewFabricatedCanonicalEntry> userClass = SelfReviewFabricatedCanonicalEntry.class;
        final Type<?> fabricated = TypeFactory.getType(userClass.getCanonicalName());
        assertFalse(TypeFactory.refusesRegistrationByClass(userClass, false, fabricated));
        assertTrue(TypeFactory.refusesRegistrationByClass(userClass, true, fabricated));

        // End to end: the registrations are refused and the built-in handlers still answer.
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(Object.class,
                reviewFixesCustomType(Object.class, "SelfReviewOverride_Object_" + System.nanoTime(), s -> null, x -> null)));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(Number.class,
                reviewFixesCustomType(Number.class, "SelfReviewOverride_Number_" + System.nanoTime(), s -> null, x -> null)));
        assertSame(TypeFactory.getType(Object.class), TypeFactory.getType("java.lang.Object"));
        assertEquals(java.util.Map.of("a", 1), N.fromJson("{\"a\": 1}", Object.class));
        assertSame(TypeFactory.getType(Number.class), TypeFactory.getType("java.lang.Number"));
        assertEquals(Number.class, TypeFactory.getType("java.lang.Number").javaType());
    }

    @Test
    public void reviewFixes20260906_priorLookupBlocksRegistrationByClass() {
        // T1-07: nothing was registered, but the lookup cached a default type for the class.
        assertTrue(
                TypeFactory.getType(ReviewFixesLookedUpByClassFirst.class).isBean() || TypeFactory.getType(ReviewFixesLookedUpByClassFirst.class).isObject());

        final IllegalArgumentException byClass = assertThrows(IllegalArgumentException.class, () -> TypeFactory
                .registerType(ReviewFixesLookedUpByClassFirst.class, x -> "x", ReviewFixesLookedUpByClassFirst -> new ReviewFixesLookedUpByClassFirst()));
        assertTrue(byClass.getMessage().contains("already registered"), byClass.getMessage());

        final String customName = "ReviewFixesLookedUpByClassFirst_" + System.nanoTime();
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(ReviewFixesLookedUpByClassFirst.class,
                reviewFixesCustomType(ReviewFixesLookedUpByClassFirst.class, customName, s -> new ReviewFixesLookedUpByClassFirst(), x -> "x")));
        assertNull(TypeFactory.getTypeIfPresent(customName));

        // A lookup by canonical NAME publishes only the fabricated name entry (the function overloads then
        // collide on the intrinsic name, which is the canonical name).
        final String canonical = ReviewFixesLookedUpByNameFirst.class.getCanonicalName();
        final Type<?> fabricated = TypeFactory.getType(canonical);
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(ReviewFixesLookedUpByNameFirst.class, x -> "x", s -> new ReviewFixesLookedUpByNameFirst()));
        assertSame(fabricated, TypeFactory.getType(canonical));
    }

    @Test
    public void reviewFixes20260906_registeredTypeIsFoundByCanonicalNameAndInsideCollections() throws Exception {
        // T1-02 (class overload, Type whose name differs from the canonical class name).
        final Class<ReviewFixesCanonicalValue> cls = ReviewFixesCanonicalValue.class;
        final String canonical = cls.getCanonicalName();
        final String customName = "ReviewFixesCanonicalAlias_" + System.nanoTime();
        final Type<ReviewFixesCanonicalValue> custom = reviewFixesCustomType(cls, customName, ReviewFixesCanonicalValue::new, x -> x.value);

        TypeFactory.registerType(cls, custom);

        assertSame(custom, TypeFactory.getType(cls));
        assertSame(custom, TypeFactory.getType(customName));
        assertSame(custom, TypeFactory.getType(canonical));
        assertSame(custom, Type.ofList(cls).elementType());
        assertSame(custom, Type.ofSet(cls).elementType());
        assertSame(custom, Type.ofMap(String.class, cls).parameterTypes().get(1));
        assertSame(custom, TypeFactory.getType(ReviewFixesCanonicalHolder.class.getDeclaredField("list").getGenericType()).elementType());
        assertSame(custom, TypeFactory.getType(ReviewFixesCanonicalHolder.class.getDeclaredField("map").getGenericType()).parameterTypes().get(1));
        assertSame(custom, TypeFactory.getType(ReviewFixesCanonicalHolder.class.getDeclaredField("arr").getGenericType()).elementType());

        final List<ReviewFixesCanonicalValue> parsed = Type.ofList(cls).valueOf("[\"a\", \"\\u00e9\\u00e8\", null]");
        assertEquals(3, parsed.size());
        assertEquals(cls, parsed.get(0).getClass());
        assertEquals("a", parsed.get(0).value);
        assertEquals("éè", parsed.get(1).value);
        assertNull(parsed.get(2));
        assertEquals(List.of(), Type.ofList(cls).valueOf("[]"));
        assertEquals("5", Type.ofMap(String.class, cls).valueOf("{\"k\": \"5\"}").get("k").value);

        final ReviewFixesCanonicalHolder holder = new ReviewFixesCanonicalHolder();
        holder.setList(List.of(new ReviewFixesCanonicalValue("1"), new ReviewFixesCanonicalValue("2")));
        holder.setMap(java.util.Map.of("k", new ReviewFixesCanonicalValue("3")));
        holder.setArr(new ReviewFixesCanonicalValue[] { new ReviewFixesCanonicalValue("4") });
        holder.setSingle(new ReviewFixesCanonicalValue("5"));

        final String json = N.toJson(holder);
        assertTrue(json.contains("\"list\": [\"1\", \"2\"]"), json);
        final ReviewFixesCanonicalHolder fromJson = N.fromJson(json, ReviewFixesCanonicalHolder.class);
        assertEquals(cls, fromJson.getList().get(0).getClass());
        assertEquals("2", fromJson.getList().get(1).value);
        assertEquals("3", fromJson.getMap().get("k").value);
        assertEquals("4", fromJson.getArr()[0].value);
        assertEquals("5", fromJson.getSingle().value);

        final ReviewFixesCanonicalHolder fromXml = N.fromXml(N.toXml(holder), ReviewFixesCanonicalHolder.class);
        assertEquals("1", fromXml.getList().get(0).value);
        assertEquals("3", fromXml.getMap().get("k").value);
    }

    @Test
    public void reviewFixes20260906_registeredJavaTimeYearIsUsedInsideCollectionsAndBeans() {
        // T1-02 with a java.time class: AbstractType shortens the intrinsic name to "Year", so before the
        // fix "java.time.Year" (what Type.ofList and bean fields ask for) fabricated an ObjectType.
        TypeFactory.registerType(java.time.Year.class, java.time.Year::toString, java.time.Year::parse);

        final Type<java.time.Year> yearType = TypeFactory.getType(java.time.Year.class);
        assertSame(yearType, TypeFactory.getType("java.time.Year"));
        assertSame(yearType, TypeFactory.getType("Year"));
        assertSame(yearType, Type.ofList(java.time.Year.class).elementType());
        assertSame(yearType, Type.ofMap(String.class, java.time.Year.class).parameterTypes().get(1));

        final List<java.time.Year> years = Type.ofList(java.time.Year.class).valueOf("[\"2020\", \"1999\"]");
        assertEquals(java.time.Year.class, years.get(0).getClass());
        assertEquals(List.of(java.time.Year.of(2020), java.time.Year.of(1999)), years);

        final ReviewFixesYearHolder holder = new ReviewFixesYearHolder();
        holder.setYears(List.of(java.time.Year.of(2020)));
        holder.setByKey(java.util.Map.of("k", java.time.Year.of(2021)));
        holder.setArr(new java.time.Year[] { java.time.Year.of(2022) });
        holder.setYear(java.time.Year.of(2023));

        final ReviewFixesYearHolder fromJson = N.fromJson(N.toJson(holder), ReviewFixesYearHolder.class);
        assertEquals(java.time.Year.of(2020), fromJson.getYears().get(0));
        assertEquals(java.time.Year.of(2021), fromJson.getByKey().get("k"));
        assertEquals(java.time.Year.of(2022), fromJson.getArr()[0]);
        assertEquals(java.time.Year.of(2023), fromJson.getYear());

        final ReviewFixesYearHolder fromXml = N.fromXml(N.toXml(holder), ReviewFixesYearHolder.class);
        assertEquals(java.time.Year.of(2020), fromXml.getYears().get(0));
        assertEquals(java.time.Year.of(2021), fromXml.getByKey().get("k"));
    }

    @Test
    public void reviewFixes20260906_namedOverloadRegistrationIsUsedForCollectionElements() {
        // T1-02 (named overload on a user class): "MoneyT" is the intrinsic name, the canonical name was never published.
        final String moneyName = "ReviewFixesMoneyT_" + System.nanoTime();
        TypeFactory.registerType(moneyName, ReviewFixesMoney.class, m -> m.amount, ReviewFixesMoney::new);

        final Type<ReviewFixesMoney> moneyType = TypeFactory.getType(moneyName);
        assertSame(moneyType, TypeFactory.getType(ReviewFixesMoney.class));
        assertSame(moneyType, TypeFactory.getType(ReviewFixesMoney.class.getCanonicalName()));
        assertSame(moneyType, Type.ofList(ReviewFixesMoney.class).elementType());

        final List<ReviewFixesMoney> parsed = Type.ofList(ReviewFixesMoney.class).valueOf("[\"5\"]");
        assertEquals(ReviewFixesMoney.class, parsed.get(0).getClass());
        assertEquals("5", parsed.get(0).amount);

        final ReviewFixesMoneyHolder holder = new ReviewFixesMoneyHolder();
        holder.setList(List.of(new ReviewFixesMoney("1.50")));
        holder.setMap(java.util.Map.of("k", new ReviewFixesMoney("2.25")));

        final String json = N.toJson(holder);
        assertEquals("{\"list\": [\"1.50\"], \"map\": {\"k\": \"2.25\"}}", json);
        final ReviewFixesMoneyHolder fromJson = N.fromJson(json, ReviewFixesMoneyHolder.class);
        assertEquals("1.50", fromJson.getList().get(0).amount);
        assertEquals("2.25", fromJson.getMap().get("k").amount);
    }

    @Test
    public void reviewFixes20260906_registrationSupersedesFallbackCachedByCanonicalNameLookup() {
        // T1-01/T1-02: a canonical-name lookup (not a class lookup) cached a fabricated fallback; a later
        // registration by class with its own name wins the class slot and retires that stale entry.
        final String canonical = ReviewFixesRollbackTarget.class.getCanonicalName();
        final Type<?> fabricated = TypeFactory.getType(canonical);
        assertTrue(fabricated.isBean() || fabricated.isObject());

        // A registration that fails on its intrinsic name publishes nothing and leaves the fallback in place.
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(ReviewFixesRollbackTarget.class,
                reviewFixesCustomType(ReviewFixesRollbackTarget.class, "String", s -> null, x -> null)));
        assertSame(fabricated, TypeFactory.getType(canonical));

        final String customName = "ReviewFixesRollbackTarget_" + System.nanoTime();
        final Type<ReviewFixesRollbackTarget> custom = reviewFixesCustomType(ReviewFixesRollbackTarget.class, customName, s -> new ReviewFixesRollbackTarget(),
                x -> "rt");
        TypeFactory.registerType(ReviewFixesRollbackTarget.class, custom);

        assertSame(custom, TypeFactory.getType(ReviewFixesRollbackTarget.class));
        assertSame(custom, TypeFactory.getType(canonical));
        assertSame(custom, Type.ofList(ReviewFixesRollbackTarget.class).elementType());
        assertEquals("[\"rt\"]", Type.ofList(ReviewFixesRollbackTarget.class).stringOf(List.of(new ReviewFixesRollbackTarget())));
    }

    @Test
    public void reviewFixes20260906_upperBoundedWildcardElementsResolveToTheirBound() throws Exception {
        // T1-03
        final String person = ReviewFixesPerson.class.getCanonicalName();
        final Type<List<ReviewFixesPerson>> listType = TypeFactory.getType("List<? extends " + person + ">");
        final Type<?> elementType = listType.elementType();

        assertEquals(ReviewFixesPerson.class, elementType.javaType());
        assertEquals(ReviewFixesPerson.class, elementType.reflectType());
        assertEquals("? extends " + person, elementType.name());
        assertTrue(listType.name().startsWith("List<? extends "), listType.name());
        assertTrue(elementType.isBean());
        assertEquals(TypeFactory.getType(person).serializationType(), elementType.serializationType());
        assertNotSame(TypeFactory.getType(person), elementType); // the bound's own instance is never pooled under the wildcard key
        assertSame(elementType, TypeFactory.getType("? extends " + person));

        final List<ReviewFixesPerson> people = listType.valueOf("[{\"name\": \"a\", \"age\": 1}, null]");
        assertEquals(ReviewFixesPerson.class, people.get(0).getClass());
        assertEquals("a", people.get(0).getName());
        assertEquals(1, people.get(0).getAge());
        assertNull(people.get(1));
        assertEquals(List.of(), listType.valueOf("[]"));
        assertEquals("[{\"name\": \"z\", \"age\": 9}]", listType.stringOf(List.of(new ReviewFixesPerson() {
            {
                setName("z");
                setAge(9);
            }
        })));

        // Set, Map value, array of the wildcard list, Optional, whitespace, nested bound, BigDecimal precision.
        assertEquals(ReviewFixesPerson.class,
                ((java.util.Set<?>) TypeFactory.getType("Set<? extends " + person + ">").valueOf("[{\"name\": \"s\"}]")).iterator().next().getClass());
        final java.util.Map<String, ReviewFixesPerson> byKey = TypeFactory.<java.util.Map<String, ReviewFixesPerson>> getType(
                "Map<String, ? extends " + person + ">").valueOf("{\"k\": {\"name\": \"m\", \"age\": 2}}");
        assertEquals("m", byKey.get("k").getName());
        assertEquals(ReviewFixesPerson.class, TypeFactory.getType("List<? extends " + person + ">[]").elementType().elementType().javaType());
        assertEquals(ReviewFixesPerson.class, TypeFactory.getType("List< ? extends " + person + " >").elementType().javaType());
        assertEquals(ReviewFixesPerson.class, TypeFactory.getType("Optional<? extends " + person + ">").parameterTypes().get(0).javaType());
        assertEquals(List.class, TypeFactory.getType("? extends List<String>").javaType());
        assertEquals(String.class, TypeFactory.getType("? extends List<String>").elementType().javaType());
        assertTrue(TypeFactory.getType("? extends List<String>[]").isArray());
        final List<?> amounts = TypeFactory.<List<?>> getType("List<? extends java.math.BigDecimal>").valueOf("[1.2300]");
        assertEquals(java.math.BigDecimal.class, amounts.get(0).getClass());
        assertEquals(new java.math.BigDecimal("1.2300"), amounts.get(0));
        assertEquals(Long.class, TypeFactory.<java.util.Map<String, ?>> getType("Map<String, ? extends Long>").valueOf("{\"a\": 1}").get("a").getClass());

        // "?" and "? super X" stay Object-capable (and keep their names for ValueTypeResolver).
        assertEquals(Object.class, TypeFactory.getType("List<?>").elementType().javaType());
        assertEquals("?", TypeFactory.getType("List<?>").elementType().name());
        assertEquals(Object.class, TypeFactory.getType("List<? super Integer>").elementType().javaType());
        assertEquals("? super Integer", TypeFactory.getType("List<? super Integer>").elementType().name());
        assertEquals(java.util.HashMap.class, TypeFactory.<List<?>> getType("List<?>").valueOf("[{\"name\": \"a\"}]").get(0).getClass());
        // an unknown bound still falls back to an ObjectType
        assertTrue(TypeFactory.getType("? extends ReviewFixesNoSuchClass").isObject());

        // Reflection path (what bean introspection uses).
        final Type<?> reflected = TypeFactory.getType(ReviewFixesWildcardHolder.class.getDeclaredField("people").getGenericType());
        assertEquals(ReviewFixesPerson.class, reflected.elementType().javaType());
        assertEquals(ReviewFixesPerson.class,
                TypeFactory.getType(ReviewFixesWildcardHolder.class.getDeclaredField("byKey").getGenericType()).parameterTypes().get(1).javaType());
        assertEquals(Object.class, TypeFactory.getType(ReviewFixesWildcardHolder.class.getDeclaredField("any").getGenericType()).elementType().javaType());
        assertEquals(Object.class, TypeFactory.getType(ReviewFixesWildcardHolder.class.getDeclaredField("lower").getGenericType()).elementType().javaType());
    }

    @Test
    public void selfReview20260907_lowerBoundedWildcardArraysStayArrays() {
        // T1-03 follow-up: the "?" / "? super X" branch returns before the shared trailing-"[]" step at the
        // tail of getType, so it has to build the array itself. Without that, "? super Integer[]" degraded
        // from an Object[] handler (r9485) to a scalar ObjectType that parses a JSON array as a raw String.
        final Type<Object[]> lower = TypeFactory.getType("? super Integer[]");
        assertTrue(lower.isArray());
        assertEquals(Object[].class, lower.javaType());
        assertEquals("? super Integer[]", lower.name());
        assertEquals("? super Integer", lower.elementType().name());

        final Object[] parsed = lower.valueOf("[1, 2]");
        assertEquals(Object[].class, parsed.getClass());
        assertEquals(2, parsed.length);
        assertEquals("[1, 2]", lower.stringOf(new Integer[] { 1, 2 }));
        assertEquals(Object[].class, TypeFactory.<List<?>> getType("List<? super Integer[]>").valueOf("[[1, 2]]").get(0).getClass());

        // The scalar spellings are unchanged, and "?[]" (which does not enter that branch) still agrees.
        assertFalse(TypeFactory.getType("? super Integer").isArray());
        assertEquals(Object.class, TypeFactory.getType("? super Integer").javaType());
        assertFalse(TypeFactory.getType("?").isArray());
        assertTrue(TypeFactory.getType("?[]").isArray());
        assertEquals("?", TypeFactory.getType("?[]").elementType().name());
    }

    @Test
    public void reviewFixes20260906_beanWithWildcardCollectionsRoundTripsThroughJsonAndXml() {
        // T1-03: subclass instances are still written with their own properties; elements are read as the bound.
        final ReviewFixesEmployee employee = new ReviewFixesEmployee();
        employee.setName("eé");
        employee.setAge(30);
        employee.setDept("d");

        final ReviewFixesWildcardHolder holder = new ReviewFixesWildcardHolder();
        holder.setPeople(List.of(employee));
        holder.setCrowd(java.util.Set.of(employee));
        holder.setByKey(java.util.Map.of("k", employee));
        holder.setAmounts(List.of(new java.math.BigDecimal("1.2300")));
        holder.setAny(List.of("s", 1));
        holder.setLower(List.of(5));

        final String json = N.toJson(holder);
        assertTrue(json.contains("\"dept\": \"d\""), json);
        final ReviewFixesWildcardHolder fromJson = N.fromJson(json, ReviewFixesWildcardHolder.class);
        assertEquals(ReviewFixesPerson.class, fromJson.getPeople().get(0).getClass());
        assertEquals("eé", fromJson.getPeople().get(0).getName());
        assertEquals(30, fromJson.getPeople().get(0).getAge());
        assertEquals(ReviewFixesPerson.class, fromJson.getCrowd().iterator().next().getClass());
        assertEquals(ReviewFixesPerson.class, fromJson.getByKey().get("k").getClass());
        assertEquals(new java.math.BigDecimal("1.2300"), fromJson.getAmounts().get(0));
        assertEquals(java.math.BigDecimal.class, fromJson.getAmounts().get(0).getClass());
        assertEquals(List.of("s", 1), fromJson.getAny());
        assertEquals(List.of(5), fromJson.getLower());

        final String xml = N.toXml(holder);
        assertTrue(xml.contains("<dept>d</dept>"), xml);
        final ReviewFixesWildcardHolder fromXml = N.fromXml(xml, ReviewFixesWildcardHolder.class);
        assertEquals(ReviewFixesPerson.class, fromXml.getPeople().get(0).getClass());
        assertEquals("eé", fromXml.getPeople().get(0).getName());
        assertEquals(ReviewFixesPerson.class, fromXml.getByKey().get("k").getClass());
        assertEquals(new java.math.BigDecimal("1.2300"), fromXml.getAmounts().get(0));
    }

    @Test
    public void reviewFixes20260906_surroundingWhitespaceResolvesToTheSameHandler() {
        // T1-04
        assertSame(TypeFactory.getType("String"), TypeFactory.getType(" String "));
        assertSame(TypeFactory.getType("String"), TypeFactory.getType("\tString\n"));
        assertSame(TypeFactory.getType("Integer"), TypeFactory.getType("Integer "));
        assertTrue(TypeFactory.getType("Integer ").isPrimitiveWrapper());
        assertSame(TypeFactory.getType("int"), TypeFactory.getType(" int"));
        assertTrue(TypeFactory.getType(" int").isPrimitive());
        assertEquals(0, TypeFactory.getType(" int").defaultValue());
        assertEquals(5, TypeFactory.getType(" int").valueOf("5"));
        assertTrue(TypeFactory.getType(" String").isString());
        assertEquals("String", TypeFactory.getType(" String ").name());
        assertTrue(TypeFactory.getType("Foo[] ").isArray());
        assertTrue(TypeFactory.getType(" Foo[]").isArray());
        assertTrue(TypeFactory.getType("String[] ").isArray());
        assertSame(TypeFactory.getType("Map<String, Integer>"), TypeFactory.getType(" Map<String, Integer>"));
        assertTrue(TypeFactory.getType(" Map<String, Integer>").isMap());
        assertTrue(TypeFactory.getType(" List<String>[] ").isArray());
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("   "));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(" \t "));
        // getTypeIfPresent stays an exact-key lookup
        assertNull(TypeFactory.getTypeIfPresent(" String "));
        assertNotNull(TypeFactory.getTypeIfPresent("String"));
        // non-ASCII whitespace is not trimmed by String.trim(): documented as-is (an unknown name)
        assertNotSame(TypeFactory.getType("String"), TypeFactory.getType(" String"));
    }

    @Test
    public void reviewFixes20260906_constructorArgumentsAreRejectedOnlyWhereNoHandlerConsumesThem() {
        // T1-05: negative shapes are in testGetTypeRejectsInvalidTypeDeclaration; here the positive pins.
        assertEquals(PasswordType.class, TypeFactory.getType("Password(MD5)").getClass());
        assertEquals(EnumType.class, TypeFactory.getType("java.time.DayOfWeek(ORDINAL)").getClass());
        assertEquals(java.time.DayOfWeek.TUESDAY, TypeFactory.getType("java.time.DayOfWeek(ORDINAL)").valueOf("1"));
        assertDoesNotThrow(() -> TypeFactory.getType(HandlerArgumentElement.class.getCanonicalName() + "<Factory(\"custom,argument\")>"));
        // an unresolvable name keeps the documented lenient fallback, even with arguments
        assertTrue(TypeFactory.getType("ReviewFixesNoSuchClass(x)").isObject());
        assertTrue(TypeFactory.getType("[I").isPrimitiveArray());
        // and the rejected spellings are not cached as degraded handlers
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Integer(1)"));
        assertNull(TypeFactory.getTypeIfPresent("Integer(1)"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("String(MD5)"));
        assertNull(TypeFactory.getTypeIfPresent("String(MD5)"));
        assertSame(TypeFactory.getType("Integer"), TypeFactory.getType("java.lang.Integer"));
    }

    @Test
    public void reviewFixes20260906_aliasNamesAreTrimmedAndValidatedBeforePublishing() {
        // T1-11
        final String intrinsic = "ReviewFixesAliasIntrinsic_" + System.nanoTime();
        final Type<Object> type = reviewFixesCustomType(Object.class, intrinsic, s -> s, String::valueOf);

        for (final String badAlias : new String[] { "  ", "\t", "X<Y", "X>", "Map<String", "<String>", "Foo<String,>" }) {
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(badAlias, type), badAlias);
            assertNull(TypeFactory.getTypeIfPresent(badAlias), badAlias);
            assertNull(TypeFactory.getTypeIfPresent(badAlias.trim()), badAlias);
        }
        assertNull(TypeFactory.getTypeIfPresent(intrinsic));

        final String alias = "ReviewFixesAlias_" + System.nanoTime();
        TypeFactory.registerType(" " + alias + " ", type);
        assertSame(type, TypeFactory.getType(alias));
        assertSame(type, TypeFactory.getType(" " + alias + " "));
        assertSame(type, TypeFactory.getTypeIfPresent(alias));
        assertNull(TypeFactory.getTypeIfPresent(" " + alias + " "));
        assertSame(type, TypeFactory.getType(intrinsic));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fix pass 2026-09-08 (G01)
    // ---------------------------------------------------------------------------------------------------------

    /** A class another handler is published for, under its canonical name, before the named overload is used. */
    public static class FixG01ClaimedByName {
    }

    /** A class nothing has ever answered for. */
    public static class FixG01FreshTarget {
    }

    /** A class whose canonical name a lookup fabricates a fallback for. */
    public static class FixG01FabricatedFallback {
        private String v;

        public String getV() {
            return v;
        }

        public void setV(final String v) {
            this.v = v;
        }
    }

    // G01-22: registerType(String, Class, ...) wrote the class cache with a bare putIfAbsent, so it could bind a
    // class another handler already answered for and leave Type.of(cls) and Type.of(name) disagreeing - the very
    // disagreement registerType(Class, Type) is guarded against.
    @Test
    public void fixG01_namedOverloadDoesNotStealAClassAnotherTypeAlreadyAnswersFor() {
        // Publish a handler under the class's CANONICAL name; nothing has looked the class up yet.
        final Class<FixG01ClaimedByName> claimedClass = FixG01ClaimedByName.class;
        final Type<FixG01ClaimedByName> claimed = reviewFixesCustomType(claimedClass, "FixG01Claimed_" + System.nanoTime(), s -> new FixG01ClaimedByName(),
                x -> "c");
        TypeFactory.registerType(claimedClass.getCanonicalName(), claimed);

        final String claimedAlias = "FixG01ClaimedAlias_" + System.nanoTime();
        TypeFactory.registerType(claimedAlias, claimedClass, x -> "a", s -> new FixG01ClaimedByName());

        // the alias is published, but the class keeps the handler that already answered for it
        assertEquals(claimedClass, TypeFactory.getType(claimedAlias).javaType());
        assertSame(claimed, TypeFactory.getType(claimedClass.getCanonicalName()));
        assertSame(claimed, TypeFactory.getType(claimedClass));
        assertSame(TypeFactory.getType(claimedClass.getCanonicalName()), TypeFactory.getType(claimedClass));

        // a built-in family class (its handler is parameterized, so it is never seeded into the class cache) is
        // refused the same way, and by name and by class still agree
        final String mapAlias = "FixG01MapAlias_" + System.nanoTime();
        TypeFactory.registerType(mapAlias, java.util.Map.class, x -> "m", s -> null);
        assertTrue(TypeFactory.getType(java.util.Map.class).isMap());
        assertSame(TypeFactory.getType("java.util.Map"), TypeFactory.getType(java.util.Map.class));

        // and a class nothing answers for is still bound, by class and by canonical name
        final String freshAlias = "FixG01FreshAlias_" + System.nanoTime();
        TypeFactory.registerType(freshAlias, FixG01FreshTarget.class, x -> "f", s -> new FixG01FreshTarget());
        assertSame(TypeFactory.getType(freshAlias), TypeFactory.getType(FixG01FreshTarget.class));
        assertSame(TypeFactory.getType(freshAlias), TypeFactory.getType(FixG01FreshTarget.class.getCanonicalName()));
    }

    // G01-22, second half: the fallback a canonical-name lookup fabricates is superseded by the named overload
    // too, so List<cls> fields stop resolving to it - registerType(Class, Type) already did this.
    @Test
    public void fixG01_namedOverloadSupersedesAFabricatedCanonicalFallback() {
        final Class<FixG01FabricatedFallback> cls = FixG01FabricatedFallback.class;
        final Type<?> fabricated = TypeFactory.getType(cls.getCanonicalName());
        assertTrue(fabricated.isBean() || fabricated.isObject());

        final String alias = "FixG01FallbackAlias_" + System.nanoTime();
        TypeFactory.registerType(alias, cls, x -> "f", s -> new FixG01FabricatedFallback());

        final Type<FixG01FabricatedFallback> registered = TypeFactory.getType(alias);
        assertSame(registered, TypeFactory.getType(cls));
        assertSame(registered, TypeFactory.getType(cls.getCanonicalName()));
        assertSame(registered, Type.ofList(cls).elementType());
    }

    /** Target of the padded-name registration through the BiFunction overload. */
    public static class FixG03BiTarget {
    }

    /** Target of the padded-name registration through the Function overload. */
    public static class FixG03FuncTarget {
    }

    /** Target of the blank-name registrations, which must be refused by both overloads. */
    public static class FixG03BlankTarget {
    }

    // G03-63: registerType(String, Type) trims the alias, but the two registerType(String, Class, ...) overloads
    // baked the RAW argument into the handler they construct, so a padded name kept its padding in
    // Type.name()/xmlName() and was published a second time under an intrinsic-name key getType(String) - which
    // trims what it looks up - could never reach.
    @Test
    public void fixG03_namedOverloadsTrimTheNameBeforeConstructingTheHandler() {
        final String biName = "FixG03BiFunc_" + System.nanoTime();
        TypeFactory.registerType("  " + biName + "  ", FixG03BiTarget.class, (x, p) -> "b", (s, p) -> new FixG03BiTarget());

        final Type<?> biType = TypeFactory.getType(biName);
        assertEquals(biName, biType.name());
        assertEquals(biName, biType.xmlName());
        assertEquals(FixG03BiTarget.class, biType.javaType());
        assertSame(biType, TypeFactory.getType("  " + biName + "  "));
        assertSame(biType, TypeFactory.getTypeIfPresent(biName));
        // the padded spelling is no longer a live pool key of its own
        assertNull(TypeFactory.getTypeIfPresent("  " + biName + "  "));
        assertSame(biType, TypeFactory.getType(FixG03BiTarget.class));

        final String funcName = "FixG03Func_" + System.nanoTime();
        TypeFactory.registerType(" " + funcName + " ", FixG03FuncTarget.class, x -> "f", s -> new FixG03FuncTarget());

        final Type<?> funcType = TypeFactory.getType(funcName);
        assertEquals(funcName, funcType.name());
        assertEquals(funcName, funcType.xmlName());
        assertEquals(FixG03FuncTarget.class, funcType.javaType());
        assertSame(funcType, TypeFactory.getType(" " + funcName + " "));
        assertSame(funcType, TypeFactory.getTypeIfPresent(funcName));
        assertNull(TypeFactory.getTypeIfPresent(" " + funcName + " "));
        assertSame(funcType, TypeFactory.getType(FixG03FuncTarget.class));

        // a name that is only whitespace is still refused by both overloads, and publishes nothing
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType("   ", FixG03BlankTarget.class, (x, p) -> "x", (s, p) -> new FixG03BlankTarget()));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(" \t ", FixG03BlankTarget.class, x -> "x", s -> new FixG03BlankTarget()));
        assertNull(TypeFactory.getTypeIfPresent("   "));
        assertNull(TypeFactory.getTypeIfPresent(" \t "));
    }

    /** Target of the duplicate-name registration. */
    public static class R04DuplicateTarget {
    }

    /** Target of the rejected registrations, which must publish nothing. */
    public static class R04RejectedTarget {
    }

    // R04 review 2026-09-08: the @throws of the two registerType(String, Class, ...) overloads listed only the
    // null/empty/blank cases, but the registerType(String, Type) call they delegate to also rejects a name that is
    // not a well-formed type declaration and a name that is already taken. Pin both, and that a rejection
    // publishes nothing.
    @Test
    public void reviewFixes20260908_namedOverloadsRejectATakenAndAMalformedName() {
        final String name = "R04Dup_" + System.nanoTime();
        TypeFactory.registerType(name, R04DuplicateTarget.class, x -> "d", s -> new R04DuplicateTarget());

        assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(name, R04RejectedTarget.class, x -> "m", s -> new R04RejectedTarget()));
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(name, R04RejectedTarget.class, (x, p) -> "m", (s, p) -> new R04RejectedTarget()));
        // the first registration is untouched, by name and by class
        assertEquals(R04DuplicateTarget.class, TypeFactory.getType(name).javaType());
        assertSame(TypeFactory.getType(name), TypeFactory.getType(R04DuplicateTarget.class));

        final String malformed = "R04Bad_" + System.nanoTime() + "<";
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(malformed, R04RejectedTarget.class, x -> "m", s -> new R04RejectedTarget()));
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(malformed, R04RejectedTarget.class, (x, p) -> "m", (s, p) -> new R04RejectedTarget()));
        assertNull(TypeFactory.getTypeIfPresent(malformed));
        assertNull(TypeFactory.getTypeIfPresent(malformed.substring(0, malformed.length() - 1)));
    }
}
