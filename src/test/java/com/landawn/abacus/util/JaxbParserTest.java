package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.ObjectFactory;
import com.landawn.abacus.entity.PersonType;
import com.landawn.abacus.entity.PersonsType;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public class JaxbParserTest extends AbstractParserTest {
    private static final String contextPath = "com.landawn.abacus.parser_0.entity";
    private static final File persons_jaxb_xml = new File("./src/test/resources/persons_jaxb.xml");
    private static final File persons_abacus_xml = new File("./src/test/resources/persons_abacus.xml");

    @Test
    @Tag("slow-test")
    public void testPerformance() throws Exception {
        createXmlFile(1000);

        int threadNum = 2;
        int loopNum = 10;

        Map<String, Integer> methodLoopNumMap = new LinkedHashMap<>();
        methodLoopNumMap.put("parseWithAbacus", loopNum);
        methodLoopNumMap.put("parseJaxb", loopNum);
        methodLoopNumMap.put("parseWithWoodstox_v4", loopNum);

        for (String method : methodLoopNumMap.keySet()) {
            Profiler.run(this, method, threadNum, methodLoopNumMap.get(method), 1).printResult();
        }
        assertNotNull(methodLoopNumMap);
    }

    @AfterEach
    public void tearDown() {
        IOUtil.deleteIfExists(persons_jaxb_xml);
        IOUtil.deleteIfExists(persons_abacus_xml);
    }

    void parseWithAbacus() throws Exception {
        InputStream is = new FileInputStream(persons_abacus_xml);

        try (is) {
            PersonsType persons = abacusXmlParser.deserialize(is, PersonsType.class);

            persons.getPerson().size();

        }
    }

    void parseJaxb() throws Exception {
        InputStream is = new FileInputStream(persons_jaxb_xml);
        Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(contextPath);

        try {
            JAXBElement<PersonsType> root = (JAXBElement<PersonsType>) unmarshaller.unmarshal(is);
            root.getValue().getPerson().size();

        } finally {
            is.close();
        }
    }

    void parseWithWoodstox_v4() throws Exception {
        InputStream is = new FileInputStream(persons_jaxb_xml);
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        XMLStreamReader xmlr = xmlif.createXMLStreamReader(is);

        try {
            parse(xmlr);
        } finally {
            is.close();
            xmlr.close();
        }
    }

    private List<PersonType> parse(XMLStreamReader xmlr) throws XMLStreamException, JAXBException {
        List<PersonType> personTypeList = new ArrayList<>();

        Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(PersonType.class);

        xmlr.nextTag();
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, "persons");

        xmlr.nextTag();

        while (xmlr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            personTypeList.add(unmarshaller.unmarshal(xmlr, PersonType.class).getValue());

            if (xmlr.getEventType() == XMLStreamConstants.CHARACTERS) {
                xmlr.next();
            }
        }

        personTypeList.size();

        return personTypeList;
    }

    private void createXmlFile(int nbrElements) throws Exception {
        PersonsType personsType = new ObjectFactory().createPersonsType();
        List<PersonType> persons = personsType.getPerson();

        for (int i = 0; i < nbrElements; i++) {
            persons.add(createPerson());
        }

        if (persons_jaxb_xml.exists()) {
            persons_jaxb_xml.delete();
        }

        persons_jaxb_xml.createNewFile();

        Marshaller marshaller = XmlUtil.createMarshaller(contextPath);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        JAXBElement<PersonsType> jaxbElement = new ObjectFactory().createPersons(personsType);
        OutputStream os = new FileOutputStream(persons_jaxb_xml);

        try (os) {
            marshaller.marshal(jaxbElement, os);
            os.flush();
        }

        if (persons_abacus_xml.exists()) {
            persons_abacus_xml.delete();
        }

        abacusXmlParser.serialize(personsType, persons_abacus_xml);
    }
}
