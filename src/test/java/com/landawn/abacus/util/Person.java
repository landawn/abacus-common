/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.sql.Timestamp;

public abstract class Person {
    private String firstName;
    private String middleName;
    private String lastName;
    private Timestamp birthDate;

    public Person() {
    }

    public Person(String firstName, String middleName, String lastName, Timestamp birthDate) {
        this();
        setFirstName(firstName);
        setMiddleName(middleName);
        setLastName(lastName);
        setBirthDate(birthDate);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Timestamp getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Timestamp birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(firstName);
        h = 31 * h + N.hashCode(middleName);
        h = 31 * h + N.hashCode(lastName);
        return 31 * h + N.hashCode(birthDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Person other) {
            if (N.equals(firstName, other.firstName) && N.equals(middleName, other.middleName) && N.equals(lastName, other.lastName)
                    && N.equals(birthDate, other.birthDate)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{" + "firstName=" + N.toString(firstName) + ", " + "middleName=" + N.toString(middleName) + ", " + "lastName=" + N.toString(lastName) + ", "
                + "birthDate=" + N.toString(birthDate) + "}";
    }
}
