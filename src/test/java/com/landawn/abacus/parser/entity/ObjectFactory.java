package com.landawn.abacus.parser.entity;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    private static final QName _XBean_QNAME = new QName("http://abacus.landawn.com/parser", "persons");

    public ObjectFactory() {
    }

    public PersonType createPersonType() {
        return new PersonType();
    }

    public XBean createXBean() {
        return new XBean();
    }

    @XmlElementDecl(namespace = "http://http://abacus.landawn.com/parser", name = "persons")
    public JAXBElement<XBean> createXBean(XBean value) {
        return new JAXBElement<>(_XBean_QNAME, XBean.class, null, value);
    }
}
