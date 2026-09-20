package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class XmlRootTypeTest extends TestBase {
    @XmlRootElement(name = "parent")
    @XmlSeeAlso(Child.class)
    public static class Parent {
        public String name;
    }

    @XmlRootElement(name = "child")
    public static class Child {
        public String name;
    }

    @Test
    void relatedUnrelatedRootIsRejectedInsideTheUtilityEvenForObjectAssignment() {
        assertThrows(ClassCastException.class, () -> {
            final Object result = XmlUtil.unmarshal(Parent.class, "<child><name>x</name></child>");
        });
        assertEquals("\uD83D\uDE00", XmlUtil.unmarshal(Parent.class, "<parent><name>\uD83D\uDE00</name></parent>").name);
        assertNull(XmlUtil.unmarshal(Parent.class, "<parent/>").name);
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.unmarshal(null, "<parent/>"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.unmarshal(Parent.class, null));
        assertThrows(RuntimeException.class, () -> XmlUtil.unmarshal(Parent.class, ""));
    }

    @Test
    void wrongTypeClosesItsReaderAndPreservesTheTypeFailure() {
        final AtomicInteger closes = new AtomicInteger();
        final var reader = new StreamReaderDelegate(XmlUtil.createXMLStreamReader(new StringReader("<child/>"))) {
            @Override
            public void close() throws XMLStreamException {
                closes.incrementAndGet();
                super.close();
                throw new XMLStreamException("cleanup");
            }
        };
        assertThrows(ClassCastException.class, () -> XmlUtil.unmarshalAndClose(Parent.class, reader));
        assertEquals(1, closes.get());
    }
}
