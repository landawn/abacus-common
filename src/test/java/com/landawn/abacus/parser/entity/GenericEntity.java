/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.landawn.abacus.entity.extendDirty.basic.Account;

public class GenericEntity {
    private List<Boolean> booleanList;
    private List<Character> charList;
    private List<Integer> intList;
    private List<String> stringList;
    private List<Account> accountList;

    private Map<String, Boolean> booleanMap;
    private Map<String, Character> charMap;
    private Map<String, Integer> intMap;
    private Map<String, String> stringMap;
    private Map<String, Account> accountMap;

    public List<Boolean> getBooleanList() {
        return booleanList;
    }

    public void setBooleanList(List<Boolean> booleanList) {
        this.booleanList = booleanList;
    }

    public List<Character> getCharList() {
        return charList;
    }

    public void setCharList(List<Character> charList) {
        this.charList = charList;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public void setIntList(List<Integer> intList) {
        this.intList = intList;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    public Map<String, Boolean> getBooleanMap() {
        return booleanMap;
    }

    public void setBooleanMap(Map<String, Boolean> booleanMap) {
        this.booleanMap = booleanMap;
    }

    public Map<String, Character> getCharMap() {
        return charMap;
    }

    public void setCharMap(Map<String, Character> charMap) {
        this.charMap = charMap;
    }

    public Map<String, Integer> getIntMap() {
        return intMap;
    }

    public void setIntMap(Map<String, Integer> intMap) {
        this.intMap = intMap;
    }

    public Map<String, String> getStringMap() {
        return stringMap;
    }

    public void setStringMap(Map<String, String> stringMap) {
        this.stringMap = stringMap;
    }

    public Map<String, Account> getAccountMap() {
        return accountMap;
    }

    public void setAccountMap(Map<String, Account> accountMap) {
        this.accountMap = accountMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountList, accountMap, booleanList, booleanMap, charList, charMap, intList, intMap, stringList, stringMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        GenericEntity other = (GenericEntity) obj;
        if (!Objects.equals(accountList, other.accountList)) {
            return false;
        }
        if (!Objects.equals(accountMap, other.accountMap)) {
            return false;
        }
        if (!Objects.equals(booleanList, other.booleanList)) {
            return false;
        }
        if (!Objects.equals(booleanMap, other.booleanMap)) {
            return false;
        }
        if (!Objects.equals(charList, other.charList)) {
            return false;
        }
        if (!Objects.equals(charMap, other.charMap)) {
            return false;
        }
        if (!Objects.equals(intList, other.intList)) {
            return false;
        }
        if (!Objects.equals(intMap, other.intMap)) {
            return false;
        }
        if (!Objects.equals(stringList, other.stringList)) {
            return false;
        }
        if (!Objects.equals(stringMap, other.stringMap)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{booleanList=" + booleanList + ", charList=" + charList + ", intList=" + intList + ", stringList=" + stringList + ", accountList=" + accountList
                + ", booleanMap=" + booleanMap + ", charMap=" + charMap + ", intMap=" + intMap + ", stringMap=" + stringMap + ", accountMap=" + accountMap
                + "}";
    }

}
