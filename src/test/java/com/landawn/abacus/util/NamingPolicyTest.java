package com.landawn.abacus.util;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AclUser;

public class NamingPolicyTest extends AbstractParserTest {

    @Test
    public void test_01() {
        Account bean = createAccountWithContact(Account.class);

        Map<String, Object> props = Beans.beanToMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.beanToMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.beanToMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, Account.class));
    }

    @Test
    public void test_02() {
        AclUser bean = createAclUserWithAclGroup(AclUser.class);

        Map<String, Object> props = Beans.beanToMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.beanToMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.beanToMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.deepBeanToMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.CAMEL_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));

        props = Beans.beanToFlatMap(bean, true, null, NamingPolicy.SCREAMING_SNAKE_CASE);
        N.println(props);
        N.println(Beans.mapToBean(props, AclUser.class));
    }

}
