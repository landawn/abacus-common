package com.landawn.abacus.parser;

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
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.entity.PersonType;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@Tag("old-test")
public abstract class AbstractParserTest extends AbstractTest {

    static final String NULL_STRING = "null".intern();
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();
    static final String TRUE = Boolean.TRUE.toString().intern();
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();
    static final String FALSE = Boolean.FALSE.toString().intern();
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    protected static final Random rand = new Random();
    protected static final JSONParser jsonParser = ParserFactory.createJSONParser();
    protected static final AvroParser avroParser = ParserFactory.createAvroParser();
    public static final XMLParser abacusXMLParser = ParserFactory.createAbacusXMLParser();
    public static final XMLParser abacusXMLSAXParser = ParserFactory.createAbacusXMLSAXParser();
    public static final XMLParser abacusXMLStAXParser = ParserFactory.createAbacusXMLStAXParser();
    public static final XMLParser abacusXMLDOMParser = ParserFactory.createAbacusXMLDOMParser();
    public static final XMLParser xmlParser = ParserFactory.createXMLParser();
    public static final XMLParser xmlDOMParser = ParserFactory.createXMLDOMParser();
    protected static final XMLParser jaxbXMLParser = ParserFactory.createJAXBParser();
    protected static final KryoParser kryoParser = ParserFactory.createKryoParser();
    protected static final JSONSerializationConfig jsc = JSC.of(true, true);
    protected static final XBean xBean = createXBean();
    protected static final XBean bigXBean = createXBean(100);
    static final ObjectMapper objMapper = new ObjectMapper();
    static final List<Gson> gsonPool = new ArrayList<>(100);

    static {
        Beans.registerXMLBindingClass(PersonType.class);
        Beans.registerXMLBindingClass(XBean.class);
    }

    static {
        try {
            N.println("xml======================================================================");
            N.println(abacusXMLParser.serialize(xBean));

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

    @Test
    public void testSerialize_00() throws Exception {
        XBean xBean = createXBean();
        String str = parser.serialize(xBean);

        XBean xBean2 = parser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_01() throws Exception {
        XBean xBean = createXBean();

        File file = getFile();

        parser.serialize(xBean, file);

        N.println(IOUtil.readAllToString(file));

        XBean xBean2 = parser.deserialize(file, XBean.class);

        N.println(xBean);
        N.println(xBean2);

        IOUtil.deleteAllIfExists(file);
    }

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

        IOUtil.deleteAllIfExists(file);
    }

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

        IOUtil.deleteAllIfExists(file);
    }

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
