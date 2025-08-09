/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.AclGroup;
import com.landawn.abacus.entity.extendDirty.basic.AclUser;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Strings;

@Tag("old-test")
public abstract class AbstractTest {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);
    protected static final String FIRST_NAME = "firstName";
    public static final String MIDDLE_NAME = "MN";
    protected static final String LAST_NAME = "lastName";
    protected static final String UPDATED_FIRST_NAME = "updatedFirstName";
    protected static final String UPDATED_LAST_NAME = "updatedLastName";
    protected static final String ACL_USER_NAME = "aclUserName";
    protected static final String ACL_GROUP_NAME = "aclGroupName";
    protected static final String ACL_DESCRIPTION = "I don't know";
    protected static final String ADDRESS = "ca, US";
    protected static final String CITY = "sunnyvale";
    protected static final String STATE = "CA";
    protected static final String COUNTRY = "U.S.";

    static {
        N.println(IOUtil.JAVA_VERSION);

        final boolean suspendPerformanceTest = true;

        if (suspendPerformanceTest) {
            N.println("Performane tests by Profiler have been suspended!!!");
            Profiler.suspend(suspendPerformanceTest);
        }
    }

    public static void println(final Object obj) {
        N.println(obj);
    }

    public static Map<String, Object> createAccountProps() {
        return createAccountProps(FIRST_NAME, LAST_NAME);
    }

    public static Map<String, Object> createAccountProps(final String firstName, final String lastName) {
        final String uuid = Strings.uuid();

        return N.asProps(Account.GUI, uuid, Account.FIRST_NAME, firstName, Account.LAST_NAME, lastName, Account.MIDDLE_NAME, MIDDLE_NAME, Account.EMAIL_ADDRESS,
                getEmail(uuid), Account.BIRTH_DATE, Dates.currentTimestamp(), Account.STATUS, 0);
    }

    public static List<Map<String, Object>> createAccountPropsList(final int size) {
        return createAccountPropsList(FIRST_NAME, LAST_NAME, size);
    }

    public static List<Map<String, Object>> createAccountPropsList(final String firstName, final String lastName, final int size) {
        final List<Map<String, Object>> propsList = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            final String uuid = Strings.uuid();
            propsList.add(N.asProps(Account.GUI, uuid, Account.FIRST_NAME, firstName + i, Account.LAST_NAME, lastName + i, Account.MIDDLE_NAME, MIDDLE_NAME,
                    Account.EMAIL_ADDRESS, getEmail(uuid), Account.BIRTH_DATE, Dates.currentTimestamp(), Account.STATUS, 0));
        }

        return propsList;
    }

    public static <T> T createAccount(final Class<T> cls) {
        return createAccount(cls, FIRST_NAME, LAST_NAME);
    }

    public static <T> List<T> createAccountList(final Class<T> cls, final int size) {
        final List<T> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(createAccount(cls, FIRST_NAME, LAST_NAME));
        }

        return list;
    }

    public static <T> T createAccountContact(final Class<T> cls) {
        final Object bean = Beans.newBean(cls);

        if (bean instanceof MapEntity mapEntity) {
            mapEntity.set(AccountContact.ADDRESS, ADDRESS);
            mapEntity.set(AccountContact.CITY, CITY);
            mapEntity.set(AccountContact.STATE, STATE);
            mapEntity.set(AccountContact.COUNTRY, COUNTRY);
            mapEntity.set(AccountContact.CREATE_TIME, Dates.currentTimestamp());
        } else {
            Beans.setPropValue(bean, AccountContact.ADDRESS, ADDRESS);
            Beans.setPropValue(bean, AccountContact.CITY, CITY);
            Beans.setPropValue(bean, AccountContact.STATE, STATE);
            Beans.setPropValue(bean, AccountContact.COUNTRY, COUNTRY);
            Beans.setPropValue(bean, AccountContact.CREATE_TIME, Dates.currentTimestamp());
        }

        return (T) bean;
    }

    public static <T> T createAccount(final Class<T> cls, final String firstName, final String lastName) {
        final Object bean = Beans.newBean(cls);
        final String uuid = Strings.uuid();

        if (bean instanceof MapEntity mapEntity) {
            mapEntity.set(Account.GUI, Strings.uuid());
            mapEntity.set(Account.FIRST_NAME, firstName);
            mapEntity.set(Account.MIDDLE_NAME, MIDDLE_NAME);
            mapEntity.set(Account.LAST_NAME, lastName);
            mapEntity.set(Account.BIRTH_DATE, Dates.currentTimestamp());
            mapEntity.set(Account.EMAIL_ADDRESS, getEmail(uuid));
            mapEntity.set(Account.LAST_UPDATE_TIME, Dates.currentTimestamp());
            mapEntity.set(Account.CREATE_TIME, Dates.currentTimestamp());
        } else {
            Beans.setPropValue(bean, Account.GUI, Strings.uuid());

            Beans.setPropValue(bean, Account.FIRST_NAME, firstName);
            Beans.setPropValue(bean, Account.MIDDLE_NAME, MIDDLE_NAME);
            Beans.setPropValue(bean, Account.LAST_NAME, lastName);

            Beans.setPropValue(bean, Account.BIRTH_DATE, Dates.currentTimestamp());
            Beans.setPropValue(bean, Account.EMAIL_ADDRESS, getEmail(uuid));
            Beans.setPropValue(bean, Account.LAST_UPDATE_TIME, Dates.currentTimestamp());
            Beans.setPropValue(bean, Account.CREATE_TIME, Dates.currentTimestamp());
        }

        return (T) bean;
    }

    public static <T> T createAccountWithContact(final Class<T> cls) {
        final T account = createAccount(cls);
        final Method propSetMethod = Beans.getPropSetMethod(cls, Account.CONTACT);
        Beans.setPropValue(account, propSetMethod, createAccountContact(propSetMethod.getParameterTypes()[0]));

        return account;
    }

    public static <T> List<T> createAccountWithContact(final Class<T> cls, final int size) {
        final List<T> accounts = new ArrayList<>();

        final Method propSetMethod = Beans.getPropSetMethod(cls, Account.CONTACT);

        T account = null;

        for (int i = 0; i < size; i++) {
            account = createAccount(cls, FIRST_NAME + i, LAST_NAME + i);
            Beans.setPropValue(account, propSetMethod, createAccountContact(propSetMethod.getParameterTypes()[0]));

            accounts.add(account);
        }

        return accounts;
    }

    public static Map<String, Object> createAclUserProps() {
        return createAclUserProps(ACL_USER_NAME);
    }

    public static Map<String, Object> createAclUserProps(final String name) {
        return N.asProps(AclUser.GUI, Strings.uuid(), AclUser.NAME, name, AclUser.DESCRIPTION, ACL_DESCRIPTION);
    }

    public static List<Map<String, Object>> createAclUserPropsList(final int size) {
        return createAclUserPropsList(ACL_USER_NAME, size);
    }

    public static List<Map<String, Object>> createAclUserPropsList(final String name, final int size) {
        final List<Map<String, Object>> propsList = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            propsList.add(createAclUserProps(name));
        }

        return propsList;
    }

    public static <T> T createAclUser(final Class<T> cls) {
        return createAclUser(cls, ACL_USER_NAME);
    }

    public static <T> T createAclUser(final Class<T> cls, final String name) {
        final AclUser aclUser = new AclUser();
        aclUser.setGUI(Strings.uuid());
        aclUser.setName(name);
        aclUser.setDescription(ACL_DESCRIPTION);

        return Beans.copy(aclUser, cls);
    }

    public static <T> T createAclGroup(final Class<T> cls) {
        return createAclGroup(cls, ACL_GROUP_NAME);
    }

    public static <T> T createAclGroup(final Class<T> cls, final String name) {
        final AclGroup aclGroup = new AclGroup();
        aclGroup.setGUI(Strings.uuid());
        aclGroup.setName(name);
        aclGroup.setDescription(ACL_DESCRIPTION);

        return Beans.copy(aclGroup, cls);
    }

    public static <T> T createAclUserWithAclGroup(final Class<T> cls) {
        final T aclUser = createAclUser(cls);

        final Method propSetMethod = Beans.getPropSetMethod(cls, AclUser.GROUP_LIST);
        final Class aclGroupClass = (Class) ((ParameterizedType) propSetMethod.getGenericParameterTypes()[0]).getActualTypeArguments()[0];

        Beans.setPropValue(aclUser, propSetMethod, N.asList(createAclGroup(aclGroupClass)));

        return aclUser;
    }

    public static <T> List<T> createAclUserWithAclGroup(final Class<T> cls, final int size) {
        final List<T> aclUsers = new ArrayList<>();

        final Method propSetMethod = Beans.getPropSetMethod(cls, AclUser.GROUP_LIST);
        final Class aclGroupClass = (Class) ((ParameterizedType) propSetMethod.getGenericParameterTypes()[0]).getActualTypeArguments()[0];

        T aclUser = null;

        for (int i = 0; i < size; i++) {
            aclUser = createAclUser(cls, ACL_USER_NAME + i);
            Beans.setPropValue(aclUser, propSetMethod, N.asList(createAclGroup(aclGroupClass, ACL_GROUP_NAME + i)));

            aclUsers.add(aclUser);
        }

        return aclUsers;
    }

    public static String getEmail(final String uuid) {
        return uuid + "@earth.com";
    }

    public static void unmap(MappedByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        try {
            buffer.force(); // flush changes to disk

            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);

            Method cleanMethod = cleaner.getClass().getMethod("clean");
            cleanMethod.setAccessible(true);
            cleanMethod.invoke(cleaner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmap the buffer", e);
        }
    }
}
