package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.XmlUtil;
import com.thoughtworks.xstream.XStream;

import testfixtures.entity.PersonsType;
import testfixtures.entity.XBean;

public class JaxbBindingTest extends AbstractParserTest {
    private static final IBindingFactory xBeanBindingFact;
    private static final IBindingFactory personsTypeBindingFact;

    static {
        IBindingFactory tempXBeanBindingFact = null;
        IBindingFactory tempPersonsTypeBindingFact = null;

        try {
            tempXBeanBindingFact = BindingDirectory.getFactory(XBean.class);
            tempPersonsTypeBindingFact = BindingDirectory.getFactory(PersonsType.class);
        } catch (Exception e) {
        }

        xBeanBindingFact = tempXBeanBindingFact;
        personsTypeBindingFact = tempPersonsTypeBindingFact;
    }

    private static final XStream xstream = new XStream();
    static final String abacus_big_xml = abacusXmlParser.serialize(bigBean);
    static final String jackson_big_xml;

    static {
        try {
            jackson_big_xml = getObjectMapper().writeValueAsString(bigBean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testPerformance() {
        int threadNum = 6;
        int loopNum = 30000;
        Map<String, Integer> methodLoopNumMap = new LinkedHashMap<>();
        methodLoopNumMap.put("executeByXmlParserWithSimpleBean", loopNum);

        methodLoopNumMap.put("executeByJAXBWithSimpleBean", loopNum);
        methodLoopNumMap.put("executeByXStreamWithSimpleBean", loopNum);
        loopNum = 10000;

        methodLoopNumMap.put("executeByXmlParserWithBigBean", loopNum);

        methodLoopNumMap.put("executeByJAXBWithBigBean", loopNum);
        methodLoopNumMap.put("executeByXStreamWithBigBean", loopNum);
        for (String method : methodLoopNumMap.keySet()) {
            final String target = method;
            final int loops = methodLoopNumMap.get(method);
            Profiler.run(threadNum, loops, 3, target, () -> {
                try {
                    JaxbBindingTest.this.getClass().getMethod(target).invoke(JaxbBindingTest.this);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }).printResult();
        }
        assertNotNull(loopNum);
    }

    public void executeByJAXBWithSimpleBean() throws Exception {
        String xml = XmlUtil.marshal(simpleBean);
        XmlUtil.unmarshal(XBean.class, xml);
    }

    public void executeByJAXBWithBigBean() throws Exception {
        String xml = XmlUtil.marshal(bigBean);
        XmlUtil.unmarshal(PersonsType.class, xml);
    }

    public void executeByJIBXWithSimpleBean() throws Exception {
        String xml = marshalByJIBX(simpleBean);
        unMarshalByJIBX(xml);
    }

    public void executeByJIBXWithBigBean() throws Exception {
        IMarshallingContext mctx = personsTypeBindingFact.createMarshallingContext();
        StringWriter stringWriter = new StringWriter();
        mctx.setOutput(stringWriter);
        mctx.marshalDocument(bigBean, "UTF-8", null);

        String xml = stringWriter.toString();

        IUnmarshallingContext uctx = personsTypeBindingFact.createUnmarshallingContext();
        StringReader stringReader = new StringReader(xml);
        uctx.unmarshalDocument(stringReader, null);
    }

    public void executeByXmlParserWithSimpleBean() {
        String xml = xmlParser.serialize(simpleBean);
        xmlParser.deserialize(xml, XBean.class);
    }

    public void executeByXmlParserWithBigBean() {
        String xml = xmlParser.serialize(bigBean);

        xmlParser.deserialize(xml, PersonsType.class);
    }

    public void executeByAbacusXMLDOMParserWithSimpleBean() {
        String xml = abacusXMLDOMParser.serialize(simpleBean);
        abacusXMLDOMParser.deserialize(xml, XBean.class);
    }

    public void executeByAbacusXMLDOMParserWithBigBean() {
        String xml = abacusXMLDOMParser.serialize(bigBean);

        abacusXMLDOMParser.deserialize(xml, PersonsType.class);
    }

    public void executeByAbacusXMLSAXParserWithSimpleBean() {
        String xml = abacusXMLSAXParser.serialize(simpleBean);
        abacusXMLSAXParser.deserialize(xml, XBean.class);
    }

    public void executeByAbacusXMLSAXParserWithBigBean() {
        String xml = abacusXMLSAXParser.serialize(bigBean);

        abacusXMLSAXParser.deserialize(xml, PersonsType.class);
    }

    public void executeByAbacusXMLStAXParserWithSimpleBean() {
        String xml = abacusXMLStAXParser.serialize(simpleBean);
        abacusXMLStAXParser.deserialize(xml, XBean.class);
    }

    public void executeByAbacusXMLStAXParserWithBigBean() {
        String xml = abacusXMLStAXParser.serialize(bigBean);

        abacusXMLStAXParser.deserialize(xml, PersonsType.class);
    }

    public void executeByXStreamWithSimpleBean() {
        String xml = xstream.toXML(simpleBean);
        xstream.fromXML(xml);

    }

    public void executeByXStreamWithBigBean() {
        String xml = xstream.toXML(bigBean);
        xstream.fromXML(xml);
    }

    @Test
    public void test_jaxb() throws Exception {
        String inputXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><xBean xmlns=\"http://uk.co.jemos.integration.xml/large-file\"><typeDate>2014-01-17 10:53:06</typeDate><typeSqlDate>2014-01-17 10:53:06</typeSqlDate><typeSqlTimestamp>2014-01-17 10:53:06</typeSqlTimestamp></xBean>";

        XmlUtil.unmarshal(XBean.class, inputXml);

        XBean xBean = createXBean();
        System.out.println("XBean as XML String:" + XmlUtil.marshal(xBean));
        assertNotNull(xBean);
    }

    public void est_jibx() throws Exception {
        String inputXml = "<XBean xmlns=\"http://landawn.com/abacus/xml/bean\">\r\n" + "  <typeBoolean>true</typeBoolean>\r\n"
                + "  <typeBoolean2>false</typeBoolean2>\r\n" + "  <typeChar>60</typeChar>\r\n" + "  <typeByte>0</typeByte>\r\n"
                + "  <typeShort>2</typeShort>\r\n" + "  <typeInt>2147483647</typeInt>\r\n" + "  <typeLong>-9223372036854775808</typeLong>\r\n"
                + "  <typeLong2>9223372036854775807</typeLong2>\r\n" + "  <typeFloat>1.0903549</typeFloat>\r\n"
                + "  <typeDouble>3.934593059323134E11</typeDouble>\r\n"
                + "  <typeString>>stklasfj230jrflqsrj190	i4932ruklsadfjq2i3j krj290klsj2jlkfjring&lt; > &lt;/ &lt;//</typeString>\r\n"
                + "  <typeDate>2014-01-17T18:52:03.768Z</typeDate>\r\n" + "  <typeSqlDate>2014-01-17</typeSqlDate>\r\n"
                + "  <typeSqlTimestamp>2014-01-17T18:52:04.043Z</typeSqlTimestamp>\r\n" + "  <weekDay>FRIDAY</weekDay>\r\n" + "</XBean>";

        unMarshalByJIBX(inputXml);

        XBean xBean = createXBean();
        System.out.println("XBean as XML String:" + marshalByJIBX(xBean));
    }

    String marshalByJIBX(XBean xBean) throws Exception {
        IMarshallingContext mctx = xBeanBindingFact.createMarshallingContext();
        StringWriter stringWriter = new StringWriter();
        mctx.setOutput(stringWriter);
        mctx.marshalDocument(xBean, "UTF-8", null);

        return stringWriter.toString();
    }

    public void unMarshalByJIBX(String inputXml) throws Exception {
        IUnmarshallingContext uctx = xBeanBindingFact.createUnmarshallingContext();
        StringReader stringReader = new StringReader(inputXml);
        uctx.unmarshalDocument(stringReader, null);
    }
}
