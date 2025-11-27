package com.landawn.abacus.parser.entity;

import java.util.Date;
import java.util.Objects;

import com.landawn.abacus.parser.adapter.DateAdapter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonType", propOrder = { "id", "firstName", "lastName", "address1", "postCode", "city", "country", "birthday" })
@XmlRootElement
public class PersonType {

    protected int id;
    @XmlElement(required = true)
    protected String firstName;
    @XmlElement(required = true)
    protected String lastName;
    @XmlElement(required = true)
    protected String address1;
    @XmlElement(required = true)
    protected String postCode;
    @XmlElement(required = true)
    protected String city;
    @XmlElement(required = true)
    protected String country;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(DateAdapter.class)
    private java.util.Date birthday;
    @XmlAttribute(required = true)
    protected boolean active;

    public int getId() {
        return id;
    }

    public void setId(int value) {
        this.id = value;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        this.firstName = value;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String value) {
        this.lastName = value;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String value) {
        this.address1 = value;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String value) {
        this.postCode = value;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String value) {
        this.country = value;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date value) {
        this.birthday = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean value) {
        this.active = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, address1, birthday, city, country, firstName, id, lastName, postCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        PersonType other = (PersonType) obj;
        if (active != other.active) {
            return false;
        }
        if (!Objects.equals(address1, other.address1)) {
            return false;
        }
        if (!Objects.equals(birthday, other.birthday)) {
            return false;
        }
        if (!Objects.equals(city, other.city)) {
            return false;
        }
        if (!Objects.equals(country, other.country)) {
            return false;
        }
        if (!Objects.equals(firstName, other.firstName)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        if (!Objects.equals(lastName, other.lastName)) {
            return false;
        }
        if (!Objects.equals(postCode, other.postCode)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", address1=" + address1 + ", postCode=" + postCode + ", city=" + city
                + ", country=" + country + ", birthday=" + birthday + ", active=" + active + "}";
    }

}
