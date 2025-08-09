/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser.entity;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    private static final QName _XBean_QNAME = new QName("http://abacus.landawn.com/parser", "persons");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.landawn.abacus.parser.entity
     *
     */
    public ObjectFactory() {
    }

    public PersonType createPersonType() {
        return new PersonType();
    }

    public XBean createXBean() {
        return new XBean();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XBean }{@code >}}.
     *
     * @param value 
     * @return 
     */
    @XmlElementDecl(namespace = "http://http://abacus.landawn.com/parser", name = "persons")
    public JAXBElement<XBean> createXBean(XBean value) {
        return new JAXBElement<>(_XBean_QNAME, XBean.class, null, value);
    }
}
