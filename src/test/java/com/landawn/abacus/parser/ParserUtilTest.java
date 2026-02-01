package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity2;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity3;
import com.landawn.abacus.parser.entity.ImmutableEntity4;
import com.landawn.abacus.parser.entity.ImmutableEntity5;
import com.landawn.abacus.parser.entity.ImmutableEntity6;
import com.landawn.abacus.parser.entity.RecordB;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;

import lombok.Value;
import lombok.experimental.Accessors;

@Tag("old-test")
public class ParserUtilTest extends AbstractTest {

    @Test
    public void test_setPropValue() {
        final Account account = Beans.newRandom(Account.class);
        N.println(account);
        Beans.setPropValue(account, Beans.getPropSetter(Account.class, "id"), 1);
        assertEquals(1, account.getId());

        Beans.setPropValue(account, Beans.getPropSetter(Account.class, "id"), "101");
        assertEquals(101, account.getId());

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        beanInfo.getPropInfo("id").setPropValue(account, 102);
        assertEquals(102, account.getId());
        beanInfo.getPropInfo("id").setPropValue(account, "103");
        assertEquals(103, account.getId());

        beanInfo.getPropInfo("lastName").setPropValue(account, 201);
        assertEquals("201", account.getLastName());
        beanInfo.getPropInfo("lastName").setPropValue(account, "202");
        assertEquals("202", account.getLastName());
    }

    @Test
    public void test_setGetNullBeanProperty() {
        final Account account = Beans.newRandom(Account.class);
        N.println(account);
        account.setContact(null);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);

        N.println(beanInfo.getPropValue(account, "contact.email"));
        assertEquals((String) null, beanInfo.getPropValue(account, "contact.email"));
        N.println(beanInfo.getPropValue(account, "contact.status"));
        assertEquals(Integer.valueOf(0), beanInfo.getPropValue(account, "contact.status"));

        N.println(account.getContact());
        beanInfo.setPropValue(account, "contact.email", "test@email.com");
        N.println(account.getContact());
        assertEquals("test@email.com", account.getContact().getEmail());

        beanInfo.setPropValue(account, "contact.status", "2");
        N.println(account.getContact());
        assertEquals(2, account.getContact().getStatus());
    }

    @Test
    public void test_underscorePropName() {
        assertEquals("a", Beans.toSnakeCase("A"));
        assertEquals("a", Beans.toSnakeCase("a"));
        assertEquals("name", Beans.toSnakeCase("name"));
        assertEquals("name", Beans.toSnakeCase("Name"));
        assertEquals("name", Beans.toSnakeCase("NAME"));
        assertEquals("first_name", Beans.toSnakeCase("firstName"));
        assertEquals("first_name", Beans.toSnakeCase("FIRSTName"));
        assertEquals("gui", Beans.toSnakeCase("GUI"));
    }

    @Test
    public void test_screamingSnakeCase() {
        assertEquals("A", Beans.toScreamingSnakeCase("A"));
        assertEquals("A", Beans.toScreamingSnakeCase("a"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("name"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("Name"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("NAME"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("FIRSTName"));
        assertEquals("GUI", Beans.toScreamingSnakeCase("GUI"));
    }

    @Test
    public void test_toScreamingSnakeCase() {
        final Account account = createAccount(Account.class);

        Map<String, Object> props = Beans.beanToMap(account);

        Beans.replaceKeysWithSnakeCase(props);

        N.println(props);

        props = Beans.beanToMap(account);

        Beans.replaceKeysWithScreamingSnakeCase(props);

        N.println(props);
    }

    @Test
    public void test_ImmutableBuilderEntity() {
        {
            final ImmutableBuilderEntity bean = ImmutableBuilderEntity.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity bean2 = N.fromJson(json, ImmutableBuilderEntity.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableBuilderEntity2 bean = ImmutableBuilderEntity2.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity2 bean2 = N.fromJson(json, ImmutableBuilderEntity2.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableBuilderEntity3 bean = ImmutableBuilderEntity3.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity3 bean2 = N.fromJson(json, ImmutableBuilderEntity3.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity4 bean = new ImmutableEntity4().id(111).firstName("fn").lastName("ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity4 bean2 = N.fromJson(json, ImmutableEntity4.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity5 bean = new ImmutableEntity5(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity5 bean2 = N.fromJson(json, ImmutableEntity5.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity6 bean = new ImmutableEntity6(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity6 bean2 = N.fromJson(json, ImmutableEntity6.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity7 bean = new ImmutableEntity7(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity7 bean2 = N.fromJson(json, ImmutableEntity7.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity8 bean = new ImmutableEntity8(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity8 bean2 = N.fromJson(json, ImmutableEntity8.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity9 bean = new ImmutableEntity9(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity9 bean2 = N.fromJson(json, ImmutableEntity9.class);

            assertEquals(bean, bean2);
        }

        {
            final RecordA bean = new RecordA(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final RecordA bean2 = N.fromJson(json, RecordA.class);

            assertEquals(bean, bean2);
        }

        {
            final RecordB bean = new RecordB(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final RecordB bean2 = N.fromJson(json, RecordB.class);

            assertEquals(bean, bean2);
        }

    }

    @Value
    public static class ImmutableEntity7 {
        private int id;
        private String firstName;
        private String lastName;
    }

    @Value
    @Accessors(fluent = true)
    public static class ImmutableEntity8 {
        private int id;
        private String firstName;
        private String lastName;
    }

    @Value
    public static class ImmutableEntity9 {
        private int id;
        private String firstName;
        private String lastName;

        public static class ImmutableEntity9Builder {
            private int id;
            private String firstName;
            private String lastName;

            public static ImmutableEntity9Builder createBuilder() {
                return new ImmutableEntity9Builder();
            }

            public ImmutableEntity9Builder id(final int id) {
                this.id = id;

                return this;
            }

            public ImmutableEntity9Builder firstName(final String firstName) {
                this.firstName = firstName;

                return this;
            }

            public ImmutableEntity9Builder lastName(final String lastName) {
                this.lastName = lastName;

                return this;
            }

            public ImmutableEntity9 create() {
                return new ImmutableEntity9(id, firstName, lastName);
            }
        }
    }

    public static record RecordA(int id, String firstName, String lastName) {

    }
}
