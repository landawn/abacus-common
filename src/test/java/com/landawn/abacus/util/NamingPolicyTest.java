/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

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

        Map<String, Object> props = Beans.bean2Map(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.bean2Map(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.bean2Map(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, Account.class));
    }

    @Test
    public void test_02() {
        AclUser bean = createAclUserWithAclGroup(AclUser.class);

        Map<String, Object> props = Beans.bean2Map(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.bean2Map(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.bean2Map(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.deepBean2Map(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.LOWER_CAMEL_CASE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));

        props = Beans.bean2FlatMap(bean, true, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        N.println(props);
        N.println(Beans.map2Bean(props, AclUser.class));
    }

}
