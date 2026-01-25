/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.landawn.abacus.entity.BigXBean;
import com.landawn.abacus.entity.PersonType;
import com.landawn.abacus.entity.PersonsType;
import com.landawn.abacus.entity.XBean;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerializationConfig;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.types.JAXBean;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@Tag("old-test")
public abstract class AbstractParserTest extends AbstractTest {
    protected static final JsonParser jsonParser = ParserFactory.createJsonParser();
    protected static final XmlParser abacusXmlParser = com.landawn.abacus.parser.AbstractParserTest.abacusXmlParser;
    protected static final XmlParser abacusXMLSAXParser = com.landawn.abacus.parser.AbstractParserTest.abacusXMLSAXParser;
    protected static final XmlParser abacusXMLStAXParser = com.landawn.abacus.parser.AbstractParserTest.abacusXMLStAXParser;
    protected static final XmlParser abacusXMLDOMParser = com.landawn.abacus.parser.AbstractParserTest.abacusXMLDOMParser;
    protected static final XmlParser xmlParser = com.landawn.abacus.parser.AbstractParserTest.xmlParser;
    protected static final XmlParser xmlDOMParser = com.landawn.abacus.parser.AbstractParserTest.xmlDOMParser;
    protected static final KryoParser kryoParser = ParserFactory.createKryoParser();
    protected static final JsonSerializationConfig jsc = JSC.of(true, true);
    protected static final XBean simpleBean = createXBean();
    protected static final BigXBean bigBean = createBigBean(100);
    static final List<ObjectMapper> objectMapperPool = new ArrayList<>(100);
    static final List<Gson> gsonPool = new ArrayList<>(100);

    static {
        Beans.registerXmlBindingClass(PersonsType.class);
        Beans.registerXmlBindingClass(BigXBean.class);
        Beans.registerXmlBindingClass(PersonType.class);
        Beans.registerXmlBindingClass(JAXBean.class);
    }

    static {
        try {
            N.println("xml======================================================================");
            N.println(abacusXmlParser.serialize(simpleBean));

            N.println("josn======================================================================");
            N.println(jsonParser.serialize(simpleBean, jsc));

            final ObjectMapper objectMapper = getObjectMapper();
            N.println("Jackson======================================================================");
            N.println(objectMapper.writeValueAsString(simpleBean));
            recycle(objectMapper);

            final Gson gson = getGson();
            N.println("Gson======================================================================");
            N.println(gson.toJson(simpleBean));
            recycle(gson);

            N.println("kryo======================================================================");
            N.println(kryoParser.serialize(simpleBean));

            N.println("======================================================================");
            N.println("");
            N.println("");
            N.println("");
            N.println("");
        } catch (final Exception e) {
            // throw new RuntimeException(e);
            e.printStackTrace();
        }
    }

    protected static XBean createXBean() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('-');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 17);
        xBean.setTypeInt(101010);
        xBean.setTypeLong(9090990909L);
        xBean.setTypeLong2(202L);
        xBean.setTypeFloat(101.09035490351f);
        xBean.setTypeDouble(39345565932.3134454d);
        xBean.setTypeString("<<>>dfe<>afe><alfeji'slfj/ei\\o;;aj//fd:///// lsaj\\\\fei { asjfei } fjeiw [fjei ]safejioae : &dakf\r sfeij 黎jei \\d\\tskfjei \":"
                + Strings.uuid());
        xBean.setTypeDate(Dates.currentJUDate());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());
        xBean.setWeekDay(WeekDay.FRIDAY);
        xBean.setFirstName(Strings.uuid());
        xBean.setMiddleName(Strings.uuid());
        xBean.setLastName(Strings.uuid());

        return xBean;
    }

    protected static BigXBean createBigBean(final int size) {
        final BigXBean bigXBean = new BigXBean();

        for (int i = 0; i < size; i++) {
            bigXBean.getXBeanList().add(createXBean());
        }

        return bigXBean;
    }

    protected static PersonType createPerson() {
        final PersonType personType = new PersonType();
        personType.setId(1010164891);
        personType.setActive(true);

        final String st = Strings.uuid();
        personType.setFirstName(st + "><\"<//> ' \"");
        personType.setLastName(st);
        personType.setAddress1(st);
        personType.setCity(st);
        personType.setCountry(st);
        personType.setPostCode(st);
        personType.setBirthday(Dates.currentJUDate());

        return personType;
    }

    protected static ObjectMapper getObjectMapper() {
        synchronized (objectMapperPool) {
            ObjectMapper mapper = null;

            if (objectMapperPool.size() > 0) {
                mapper = objectMapperPool.remove(objectMapperPool.size() - 1);
            } else {
                mapper = new ObjectMapper();
            }

            // mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            return mapper;
        }
    }

    protected static void recycle(final ObjectMapper objectMapper) {
        synchronized (objectMapperPool) {
            objectMapperPool.add(objectMapper);
        }
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

    protected static void recycle(final Gson gson) {
        synchronized (gsonPool) {
            gsonPool.add(gson);
        }
    }
}
