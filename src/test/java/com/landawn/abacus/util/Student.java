/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Student extends Person implements Serializable, Cloneable {
    private static final long serialVersionUID = 15607345755L;

    private String clazz;
    private List<List<com.landawn.abacus.util.Person>> list;
    private Map<List<List<com.landawn.abacus.util.Person>>, List<List<com.landawn.abacus.util.Person>>> map;
    private Map<com.landawn.abacus.util.Person, List<List<com.landawn.abacus.util.Person>>> map1;
    private Map<List<List<com.landawn.abacus.util.Person>>, com.landawn.abacus.util.Person> map2;

    public Student() {
    }

    public Student(String clazz, List<List<com.landawn.abacus.util.Person>> list,
            Map<List<List<com.landawn.abacus.util.Person>>, List<List<com.landawn.abacus.util.Person>>> map,
            Map<com.landawn.abacus.util.Person, List<List<com.landawn.abacus.util.Person>>> map1,
            Map<List<List<com.landawn.abacus.util.Person>>, com.landawn.abacus.util.Person> map2) {
        this();
        setClazz(clazz);
        setList(list);
        setMap(map);
        setMap1(map1);
        setMap2(map2);
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public List<List<com.landawn.abacus.util.Person>> getList() {
        return list;
    }

    public void setList(List<List<com.landawn.abacus.util.Person>> list) {
        this.list = list;
    }

    public Map<List<List<com.landawn.abacus.util.Person>>, List<List<com.landawn.abacus.util.Person>>> getMap() {
        return map;
    }

    public void setMap(Map<List<List<com.landawn.abacus.util.Person>>, List<List<com.landawn.abacus.util.Person>>> map) {
        this.map = map;
    }

    public Map<com.landawn.abacus.util.Person, List<List<com.landawn.abacus.util.Person>>> getMap1() {
        return map1;
    }

    public void setMap1(Map<com.landawn.abacus.util.Person, List<List<com.landawn.abacus.util.Person>>> map1) {
        this.map1 = map1;
    }

    public Map<List<List<com.landawn.abacus.util.Person>>, com.landawn.abacus.util.Person> getMap2() {
        return map2;
    }

    public void setMap2(Map<List<List<com.landawn.abacus.util.Person>>, com.landawn.abacus.util.Person> map2) {
        this.map2 = map2;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(list);
        h = 31 * h + N.hashCode(map);
        h = 31 * h + N.hashCode(map1);
        h = 31 * h + N.hashCode(map2);
        h = 31 * h + N.hashCode(getFirstName());
        h = 31 * h + N.hashCode(getMiddleName());
        h = 31 * h + N.hashCode(getLastName());
        return 31 * h + N.hashCode(getBirthDate());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Student other) {
            if (N.equals(list, other.list) && N.equals(map, other.map) && N.equals(map1, other.map1) && N.equals(map2, other.map2)
                    && N.equals(getFirstName(), other.getFirstName()) && N.equals(getMiddleName(), other.getMiddleName())
                    && N.equals(getLastName(), other.getLastName()) && N.equals(getBirthDate(), other.getBirthDate())) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{" + "firstName=" + N.toString(getFirstName()) + ", " + "middleName=" + N.toString(getMiddleName()) + ", " + "lastName="
                + N.toString(getLastName()) + ", " + "birthDate=" + N.toString(getBirthDate()) + ", " + "map=" + N.toString(map) + ", " + "map1="
                + N.toString(map1) + ", " + "map2=" + N.toString(map2) + ", " + "}";
    }
}
