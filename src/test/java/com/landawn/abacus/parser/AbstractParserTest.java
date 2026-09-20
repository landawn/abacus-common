package com.landawn.abacus.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.PersonType;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

import testfixtures.types.WeekDay;

public abstract class AbstractParserTest extends AbstractTest {

    static final String NULL_STRING = "null".intern();
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();
    static final String TRUE = Boolean.TRUE.toString().intern();
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();
    static final String FALSE = Boolean.FALSE.toString().intern();
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    protected static final Random rand = new Random();
    protected static final JsonParser jsonParser = ParserFactory.createJsonParser();
    protected static final AvroParser avroParser = ParserFactory.createAvroParser();
    // Positive round-trip fixtures opt in explicitly. Security tests create separate restrictive parsers.
    // Utility tests share these parsers and use a distinct nested XBean; approval of parser.entity.XBean does not cover it.
    private static final Set<Class<?>> XML_FIXTURE_TYPES = Set.of(XBean.class, PersonType.class, WeekDay.class, ContainerBean.class,
            com.landawn.abacus.util.NTestSupport.XBean.class, com.landawn.abacus.parser.entity.GenericEntity.class,
            testfixtures.entity.extendDirty.basic.Account.class, testfixtures.entity.extendDirty.basic.AccountContact.class,
            testfixtures.entity.extendDirty.basic.AccountDevice.class, XmlParserImplTest.OuterBean.class, XmlParserImplTest.NullableOnlyBean.class);
    public static final XmlParser abacusXmlParser = ParserFactory.createAbacusXmlParser(null, null, XML_FIXTURE_TYPES);
    public static final XmlParser abacusXMLSAXParser = new AbacusXmlParserImpl(XmlParserType.SAX, null, null, XML_FIXTURE_TYPES);
    public static final XmlParser abacusXMLStAXParser = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, XML_FIXTURE_TYPES);
    public static final XmlParser abacusXMLDOMParser = new AbacusXmlParserImpl(XmlParserType.DOM, null, null, XML_FIXTURE_TYPES);
    public static final XmlParser xmlParser = ParserFactory.createXmlParser(null, null, XML_FIXTURE_TYPES);
    public static final XmlParser xmlDOMParser = new XmlParserImpl(XmlParserType.DOM, null, null, XML_FIXTURE_TYPES);
    protected static final XmlParser jaxbXmlParser = ParserFactory.createJaxbParser();
    protected static final KryoParser kryoParser = ParserFactory.createKryoParser();
    protected static final JsonSerConfig jsc = JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(true);
    protected static final XBean xBean = createXBean();
    protected static final XBean bigXBean = createXBean(100);
    static final ObjectMapper objMapper = new ObjectMapper();
    static final List<Gson> gsonPool = new ArrayList<>(100);

    static {
        Beans.registerXmlBindingClass(PersonType.class);
        Beans.registerXmlBindingClass(XBean.class);
    }

    static {
        try {
            N.println("xml======================================================================");
            N.println(abacusXmlParser.serialize(xBean));

            N.println("josn======================================================================");
            N.println(jsonParser.serialize(xBean, jsc));

            N.println("Jackson======================================================================");
            N.println(objMapper.writeValueAsString(xBean));

            Gson gson = getGson();
            N.println("Gson======================================================================");
            N.println(gson.toJson(xBean));
            recycle(gson);

            N.println("kryo======================================================================");
            N.println(kryoParser.serialize(xBean));

            N.println("======================================================================");
            N.println("");
            N.println("");
            N.println("");
            N.println("");
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    protected final Parser<? extends SerializationConfig, ? extends DeserializationConfig> parser = getParser();

    protected abstract Parser<? extends SerializationConfig, ? extends DeserializationConfig> getParser();

    protected static XBean createXBean() {
        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('"');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 17);
        xBean.setTypeInt(101010);
        xBean.setTypeLong(9090990909L);
        xBean.setTypeLong2(202L);
        xBean.setTypeFloat(101.09035490351f);
        xBean.setTypeDouble(39345565932.3134454d);
        xBean.setTypeString("<<>>dfe<>afe><alfeji'slfj/ei\\o;;aj//fd:///// lsaj\\\\fei { asjfei } fjeiw [fjei ]safejioae : &dakf sfeij 黎jei \\d\\tskfjei \":"
                + Strings.uuid());
        xBean.setTypeDate(Dates.currentJUDate());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());
        xBean.setWeekDay(WeekDay.FRIDAY);
        xBean.setFirstName(Strings.uuid());
        xBean.setMiddleName(Strings.uuid());
        xBean.setLastName(Strings.uuid());
        xBean.getPersons().add(createPerson());

        return xBean;
    }

    protected static XBean createXBean(int size) {
        XBean xBean = createXBean();

        for (int i = 0; i < size; i++) {
            xBean.getPersons().add(createPerson());
        }

        return xBean;
    }

    protected static PersonType createPerson() {
        PersonType personType = new PersonType();
        personType.setId(1010164891);
        personType.setActive(true);

        String st = Strings.uuid();
        personType.setFirstName(st + "><\"<//> ' \"");
        personType.setLastName(st);
        personType.setAddress1(st);
        personType.setCity(st);
        personType.setCountry(st);
        personType.setPostCode(st);
        personType.setBirthday(Dates.currentJUDate());

        return personType;
    }

    protected static Gson getGson() {
        synchronized (gsonPool) {
            if (gsonPool.size() > 0) {
                return gsonPool.remove(gsonPool.size() - 1);
            } else {
                return new Gson();
            }
        }
    }

    protected static void recycle(Gson gson) {
        synchronized (gsonPool) {
            gsonPool.add(gson);
        }
    }

    // =====================================================================
    // serialize(Object)
    // =====================================================================

    @Test
    public void testSerialize_00() throws Exception {
        XBean xBean = createXBean();
        String str = parser.serialize(xBean);

        XBean xBean2 = parser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    // =====================================================================
    // serialize(Object, File)
    // =====================================================================

    @Test
    public void testSerialize_01() throws Exception {
        XBean xBean = createXBean();

        File file = getFile();

        parser.serialize(xBean, file);

        N.println(IOUtil.readAllToString(file));

        XBean xBean2 = parser.deserialize(file, XBean.class);

        N.println(xBean);
        N.println(xBean2);

        IOUtil.deleteRecursivelyIfExists(file);
        assertNotNull(xBean2);
    }

    // =====================================================================
    // serialize(Object, OutputStream)
    // =====================================================================

    @Test
    public void testSerialize_02() throws Exception {
        XBean xBean = createXBean();
        File file = getFile();

        OutputStream os = new FileOutputStream(file);
        parser.serialize(xBean, os);
        IOUtil.close(os);

        N.println(IOUtil.readAllToString(file));

        InputStream is = new FileInputStream(file);
        parser.deserialize(is, XBean.class);
        IOUtil.close(is);

        IOUtil.deleteRecursivelyIfExists(file);
        assertNotNull(is);
    }

    // =====================================================================
    // serialize(Object, Writer)
    // =====================================================================

    @Test
    public void testSerialize_03() throws Exception {
        XBean xBean = createXBean();

        File file = getFile();

        Writer writer = new FileWriter(file);
        parser.serialize(xBean, writer);
        IOUtil.close(writer);

        N.println(IOUtil.readAllToString(file));

        Reader reader = new FileReader(file);
        parser.deserialize(reader, XBean.class);
        IOUtil.close(reader);

        IOUtil.deleteRecursivelyIfExists(file);
        assertNotNull(reader);
    }

    // =====================================================================
    // deserialize(String, Type)
    // =====================================================================

    @Test
    public void testDeserialize_stringWithType() throws Exception {
        XBean xBean = createXBean();
        String str = parser.serialize(xBean);

        XBean xBean2 = parser.deserialize(str, XBean.class);
        assertNotNull(xBean2);
    }

    // =====================================================================
    // deserialize(File, Class)
    // =====================================================================

    @Test
    public void testDeserialize_fileWithClass() throws Exception {
        XBean xBean = createXBean();
        File file = getFile();
        parser.serialize(xBean, file);

        XBean xBean2 = parser.deserialize(file, XBean.class);
        assertNotNull(xBean2);

        IOUtil.deleteRecursivelyIfExists(file);
    }

    // =====================================================================
    // deserialize(InputStream, Class)
    // =====================================================================

    @Test
    public void testDeserialize_inputStreamWithClass() throws Exception {
        XBean xBean = createXBean();
        File file = getFile();
        parser.serialize(xBean, file);

        InputStream is = new FileInputStream(file);
        XBean xBean2 = parser.deserialize(is, XBean.class);
        IOUtil.close(is);
        assertNotNull(xBean2);

        IOUtil.deleteRecursivelyIfExists(file);
    }

    // =====================================================================
    // deserialize(Reader, Class)
    // =====================================================================

    @Test
    public void testDeserialize_readerWithClass() throws Exception {
        XBean xBean = createXBean();
        File file = getFile();
        parser.serialize(xBean, file);

        Reader reader = new FileReader(file);
        XBean xBean2 = parser.deserialize(reader, XBean.class);
        IOUtil.close(reader);
        assertNotNull(xBean2);

        IOUtil.deleteRecursivelyIfExists(file);
    }

    // =====================================================================
    // serialize with null object
    // =====================================================================

    @Test
    public void testSerialize_nullObject() throws Exception {
        String str = parser.serialize(null);
        assertEquals(Strings.EMPTY, str);
    }

    // =====================================================================
    // review fixes 2026-09-07 (R5): getConcreteClass / collectionToArray
    // =====================================================================

    public static class ContainerBean {
        private Map<String, Object> map;
        private List<Object> list;

        public Map<String, Object> getMap() {
            return map;
        }

        public void setMap(Map<String, Object> map) {
            this.map = map;
        }

        public List<Object> getList() {
            return list;
        }

        public void setList(List<Object> list) {
            this.list = list;
        }
    }

    @Test
    public void reviewFixes20260907_uninstantiableContainerTypeAttributeFallsBackToTheDeclaredType() {
        // No no-argument constructor at all: the reader would fail with "No default constructor found".
        assertSame(Map.class, AbstractParser.getConcreteClass(ImmutableMap.class, Map.class));
        assertSame(List.class, AbstractParser.getConcreteClass(ImmutableList.class, List.class));
        assertSame(List.class, AbstractParser.getConcreteClass(Arrays.asList(1).getClass(), List.class));
        assertSame(List.class, AbstractParser.getConcreteClass(List.of(1).getClass(), List.class));
        assertSame(Map.class, AbstractParser.getConcreteClass(Collections.unmodifiableMap(new HashMap<>()).getClass(), Map.class));

        // A no-argument constructor that exists but is private in a java.base package that is not open:
        // N.newInstance cannot use it either, so this must fall back too.
        assertSame(Map.class, AbstractParser.getConcreteClass(Collections.emptyMap().getClass(), Map.class));
        assertSame(List.class, AbstractParser.getConcreteClass(Collections.emptyList().getClass(), List.class));

        // Instantiable containers keep the more specific class.
        assertSame(LinkedHashMap.class, AbstractParser.getConcreteClass(LinkedHashMap.class, Map.class));
        assertSame(TreeMap.class, AbstractParser.getConcreteClass(TreeMap.class, Map.class));
        assertSame(ArrayList.class, AbstractParser.getConcreteClass(ArrayList.class, List.class));

        // Scalars, records, enums, arrays, interfaces and abstract types are untouched: they are converted from
        // text or mapped to a default implementation, never instantiated through a no-argument constructor here.
        assertSame(Integer.class, AbstractParser.getConcreteClass(Integer.class, Number.class));
        assertSame(java.time.Instant.class, AbstractParser.getConcreteClass(java.time.Instant.class, Object.class));
        assertSame(java.time.LocalDate.class, AbstractParser.getConcreteClass(java.time.LocalDate.class, Object.class));
        assertSame(String.class, AbstractParser.getConcreteClass(String.class, CharSequence.class));
        assertSame(WeekDay.class, AbstractParser.getConcreteClass(WeekDay.class, Object.class));
        assertSame(int[].class, AbstractParser.getConcreteClass(int[].class, Object.class));
        assertSame(List.class, AbstractParser.getConcreteClass(List.class, Collection.class));
        assertSame(java.util.AbstractMap.class, AbstractParser.getConcreteClass(java.util.AbstractMap.class, Map.class));

        // Nothing to fall back to, and the null/incompatible rules, are unchanged.
        assertSame(ImmutableMap.class, AbstractParser.getConcreteClass(ImmutableMap.class, null));
        assertSame(Map.class, AbstractParser.getConcreteClass(null, Map.class));
        assertSame(Map.class, AbstractParser.getConcreteClass(String.class, Map.class));

        // End to end: the writer records the runtime class of an immutable/empty container, so its own output has
        // to stay readable on every backend.
        final ContainerBean bean = new ContainerBean();
        bean.setMap(ImmutableMap.of("k", (Object) "v"));
        bean.setList(ImmutableList.of((Object) "a"));

        final ContainerBean empty = new ContainerBean();
        empty.setMap(Collections.emptyMap());
        empty.setList(Collections.emptyList());

        final XmlParser[] parsers = { abacusXmlParser, abacusXMLSAXParser, abacusXMLDOMParser, xmlParser, xmlDOMParser };
        final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(true);

        for (final XmlParser xmlParser : parsers) {
            final String xml = xmlParser.serialize(bean, config);
            final ContainerBean back = xmlParser.deserialize(xml, ContainerBean.class);
            assertEquals(xml, "v", back.getMap().get("k"));
            assertEquals(xml, "a", back.getList().get(0));

            final String emptyXml = xmlParser.serialize(empty, config);
            final ContainerBean emptyBack = xmlParser.deserialize(emptyXml, ContainerBean.class);
            assertTrue(emptyBack.getMap().isEmpty(), emptyXml);
            assertTrue(emptyBack.getList().isEmpty(), emptyXml);
        }
    }

    @Test
    public void reviewFixes20260907_collectionToArrayRejectsOnlyElementsTheDeclaredArrayCannotHold() {
        final Type<String[]> stringArray = Type.of(String[].class);

        // Legitimate inputs are unchanged.
        assertEquals(null, AbstractParser.collectionToArray(null, stringArray));
        assertArrayEquals(new String[0], (String[]) AbstractParser.collectionToArray(new ArrayList<>(), stringArray));
        assertArrayEquals(new String[] { "a", "b" }, (String[]) AbstractParser.collectionToArray(Arrays.asList("a", "b"), stringArray));
        assertArrayEquals(new String[] { "a", null, "b" }, (String[]) AbstractParser.collectionToArray(Arrays.asList("a", null, "b"), stringArray));
        assertArrayEquals(new String[] { null, null }, (String[]) AbstractParser.collectionToArray(Arrays.asList(null, null), stringArray));

        // Subtype elements, Object[] targets, nested arrays and primitive arrays keep working.
        final Number[] numbers = AbstractParser.collectionToArray(Arrays.asList(1, 2.5, java.math.BigInteger.ONE), Type.of(Number[].class));
        assertSame(Number[].class, numbers.getClass());
        assertEquals(Integer.valueOf(1), numbers[0]);
        final Object[] objects = AbstractParser.collectionToArray(Arrays.asList("a", 1, new ArrayList<>()), Type.of(Object[].class));
        assertSame(Object[].class, objects.getClass());
        assertEquals(3, objects.length);
        assertArrayEquals(new String[] { "a" },
                ((String[][]) AbstractParser.collectionToArray(Arrays.asList((Object) new String[] { "a" }), Type.of(String[][].class)))[0]);
        assertArrayEquals(new int[] { 1, 2 }, (int[]) AbstractParser.collectionToArray(Arrays.asList(1, 2), Type.of(int[].class)));
        assertEquals(0, ((int[]) AbstractParser.collectionToArray(new ArrayList<>(), Type.of(int[].class))).length);
        assertEquals(1, ((List<?>[]) AbstractParser.collectionToArray(Arrays.asList((Object) new ArrayList<>()), Type.of("List<String>[]"))).length);

        // A mis-typed element is a parse error, not an ArrayList[] the caller trips over later (T7-02), and not an
        // ArrayStoreException from the array copy.
        for (final Object bad : new Object[] { new ArrayList<>(List.of(1)), new HashMap<>(), Integer.valueOf(1) }) {
            assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(bad), stringArray));
            assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList("a", bad), stringArray));
        }

        final ParsingException e = assertThrows(ParsingException.class,
                () -> AbstractParser.collectionToArray(Arrays.asList("a", null, new HashMap<>()), stringArray));
        assertTrue(e.getMessage().contains("index 2"), e.getMessage());
        assertTrue(e.getMessage().contains("java.util.HashMap"), e.getMessage());

        assertThrows(ParsingException.class, () -> AbstractParser.collectionToArray(Arrays.asList(1, 2L), Type.of(Integer[].class)));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    protected File getFile() {
        return new File("./src/test/resources/json_" + Strings.uuid() + ".json");
    }

    public static class TransientBean {
        private transient String transientField;
        private String nontransientField;

        public String getTransientField() {
            return transientField;
        }

        public void setTransientField(String transientField) {
            this.transientField = transientField;
        }

        public String getNontransientField() {
            return nontransientField;
        }

        public void setNontransientField(String nontransientField) {
            this.nontransientField = nontransientField;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nontransientField, transientField);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }
            TransientBean other = (TransientBean) obj;
            if (!Objects.equals(nontransientField, other.nontransientField) || !Objects.equals(transientField, other.transientField)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "TransientBean [transientField=" + transientField + ", nontransientField=" + nontransientField + "]";
        }

    }
}
