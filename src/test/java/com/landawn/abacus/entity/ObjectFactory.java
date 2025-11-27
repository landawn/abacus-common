package com.landawn.abacus.entity;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    private static final QName _Persons_QNAME = new QName("http://uk.co.jemos.integration.xml/large-file", "persons");

    public ObjectFactory() {
    }

    public PersonType createPersonType() {
        return new PersonType();
    }

    public PersonsType createPersonsType() {
        return new PersonsType();
    }

    @XmlElementDecl(namespace = "http://uk.co.jemos.integration.xml/large-file", name = "persons")
    public JAXBElement<PersonsType> createPersons(PersonsType value) {
        return new JAXBElement<>(_Persons_QNAME, PersonsType.class, null, value);
    }

}
