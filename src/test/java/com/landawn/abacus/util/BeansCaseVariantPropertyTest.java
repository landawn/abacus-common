package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;

/**
 * Properties whose names differ only in case ({@code userName}/{@code username}, {@code url}/{@code URL}) must each bind
 * their own exact-case accessors. Field/getter pairing used to accept any case-insensitive getter and keep whichever
 * one {@code Class.getMethods()} listed last, an order that varies between JVM runs, so one property read and wrote the
 * other's field. Because that order is unspecified, the old code failed these tests in some runs and passed in others.
 */
@Tag("unit")
class BeansCaseVariantPropertyTest extends TestBase {

    @Test
    void caseVariantPropertiesBindTheirOwnAccessors() {
        assertAccessors(CaseVariant.class, "userName", "getUserName", "setUserName");
        assertAccessors(CaseVariant.class, "username", "getUsername", "setUsername");
        assertEquals(List.of("userName", "username"), Beans.getPropNameList(CaseVariant.class));

        // Declaration order does not decide the pairing either.
        assertAccessors(ReversedCaseVariant.class, "userName", "getUserName", "setUserName");
        assertAccessors(ReversedCaseVariant.class, "username", "getUsername", "setUsername");

        assertAccessors(Acronym.class, "url", "getUrl", "setUrl");
        assertAccessors(Acronym.class, "URL", "getURL", "setURL");
    }

    @Test
    void readsReturnEachPropertysOwnValue() {
        final CaseVariant bean = caseVariant("a", "b");

        assertEquals("a", Beans.getPropValue(bean, "userName"));
        assertEquals("b", Beans.getPropValue(bean, "username"));

        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("userName", "a");
        expected.put("username", "b");
        assertEquals(expected, Beans.beanToMap(bean));
        assertEquals("{\"userName\": \"a\", \"username\": \"b\"}", N.toJson(bean));

        final Acronym acronym = new Acronym();
        acronym.setUrl("a");
        acronym.setURL("b");
        assertEquals("{\"url\": \"a\", \"URL\": \"b\"}", N.toJson(acronym));
    }

    @Test
    void writesReachEachPropertysOwnField() {
        final CaseVariant parsed = N.fromJson("{\"userName\":\"a\",\"username\":\"b\"}", CaseVariant.class);
        assertEquals("a", parsed.getUserName());
        assertEquals("b", parsed.getUsername());

        final CaseVariant copy = Beans.copy(caseVariant("a", "b"));
        assertEquals("a", copy.getUserName());
        assertEquals("b", copy.getUsername());

        final CaseVariant bean = new CaseVariant();
        Beans.setPropValue(bean, "userName", "x");
        assertEquals("x", bean.getUserName());
        assertNull(bean.getUsername());

        Beans.setPropValue(bean, "username", "y");
        assertEquals("x", bean.getUserName());
        assertEquals("y", bean.getUsername());

        final ReversedCaseVariant reversed = N.fromJson("{\"userName\":\"a\",\"username\":\"b\"}", ReversedCaseVariant.class);
        assertEquals("a", reversed.getUserName());
        assertEquals("b", reversed.getUsername());

        final Acronym acronym = N.fromJson("{\"url\":\"a\",\"URL\":\"b\"}", Acronym.class);
        assertEquals("a", acronym.getUrl());
        assertEquals("b", acronym.getURL());
    }

    @Test
    void anotherFieldsExactGetterIsNotBorrowed() {
        // userName has no accessor of its own: it must not reuse username's getter and setter.
        assertEquals(List.of("username"), Beans.getPropNameList(OnlyLowerCaseAccessors.class));
        assertAccessors(OnlyLowerCaseAccessors.class, "username", "getUsername", "setUsername");
    }

    @Test
    void anotherPropertysExactSetterIsNotBorrowed() {
        // getUserName() has no setter of its own; setUsername() belongs to getUsername(). On an ordinary mutable bean
        // a getter-only accessor is not a property, so userName is absent rather than writing username's field.
        assertEquals(List.of("username"), Beans.getPropNameList(MissingUpperCaseSetter.class));
        assertAccessors(MissingUpperCaseSetter.class, "username", "getUsername", "setUsername");
    }

    @Test
    void caseInsensitivePairingStillAppliesWithoutAConflict() {
        final UniqueCaseInsensitive bean = new UniqueCaseInsensitive();
        bean.setUserName("z");
        assertAccessors(UniqueCaseInsensitive.class, "username", "getUserName", "setUserName");
        assertEquals("z", Beans.getPropValue(bean, "username"));

        final DifferentTypes typed = new DifferentTypes();
        typed.setUserName("a");
        typed.setUsername(7);
        assertAccessors(DifferentTypes.class, "userName", "getUserName", "setUserName");
        assertAccessors(DifferentTypes.class, "username", "getUsername", "setUsername");
        assertEquals("a", Beans.getPropValue(typed, "userName"));
        assertEquals(7, (Integer) Beans.getPropValue(typed, "username"));

        assertAccessors(UnderscoreField.class, "_name", "getName", "setName");
    }

    @Test
    void publicFieldsWithCaseVariantNamesRemainIndependent() {
        final PublicFields bean = new PublicFields();
        bean.userName = "a";
        bean.username = "b";
        assertEquals("a", Beans.getPropValue(bean, "userName"));
        assertEquals("b", Beans.getPropValue(bean, "username"));
    }

    private static void assertAccessors(final Class<?> beanClass, final String propName, final String getterName, final String setterName) {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        PropInfo propInfo = null;

        for (final PropInfo candidate : beanInfo.propInfoList) {
            if (candidate.name.equals(propName)) {
                propInfo = candidate;
            }
        }

        assertEquals(propName, propInfo == null ? null : propInfo.name, beanClass.getSimpleName());
        assertEquals(getterName, propInfo.getMethod.getName(), beanClass.getSimpleName() + "." + propName);
        assertEquals(setterName, propInfo.setMethod.getName(), beanClass.getSimpleName() + "." + propName);
    }

    private static CaseVariant caseVariant(final String userName, final String username) {
        final CaseVariant bean = new CaseVariant();
        bean.setUserName(userName);
        bean.setUsername(username);
        return bean;
    }

    public static class CaseVariant {
        private String userName;
        private String username;

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    public static class ReversedCaseVariant {
        private String username;
        private String userName;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }
    }

    public static class Acronym {
        private String url;
        private String URL; //NOSONAR

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getURL() {
            return URL;
        }

        public void setURL(final String url) {
            this.URL = url;
        }
    }

    public static class OnlyLowerCaseAccessors {
        @SuppressWarnings("unused")
        private String userName;
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    public static class MissingUpperCaseSetter {
        private String userName;
        private String username;

        public String getUserName() {
            return userName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    public static class UniqueCaseInsensitive {
        private String username;

        public String getUserName() {
            return username;
        }

        public void setUserName(final String username) {
            this.username = username;
        }
    }

    public static class DifferentTypes {
        private String userName;
        private Integer username;

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public Integer getUsername() {
            return username;
        }

        public void setUsername(final Integer username) {
            this.username = username;
        }
    }

    public static class UnderscoreField {
        private String _name; //NOSONAR

        public String getName() {
            return _name;
        }

        public void setName(final String name) {
            this._name = name;
        }
    }

    public static class PublicFields {
        public String userName;
        public String username;
    }
}
