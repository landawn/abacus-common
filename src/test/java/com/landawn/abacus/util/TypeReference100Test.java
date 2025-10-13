package com.landawn.abacus.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("new-test")
public class TypeReference100Test extends TestBase {

    @Test
    public void testTypeReferenceSimpleType() {
        TypeReference<String> stringRef = new TypeReference<String>() {
        };
        Type<String> stringType = stringRef.type();

        Assertions.assertNotNull(stringType);
        Assertions.assertEquals(String.class, stringType.clazz());
    }

    @Test
    public void testTypeReferenceGenericList() {
        TypeReference<List<String>> listRef = new TypeReference<List<String>>() {
        };
        Type<List<String>> listType = listRef.type();

        Assertions.assertNotNull(listType);
        Assertions.assertEquals(List.class, listType.clazz());
    }

    @Test
    public void testTypeReferenceGenericMap() {
        TypeReference<Map<String, Integer>> mapRef = new TypeReference<Map<String, Integer>>() {
        };
        Type<Map<String, Integer>> mapType = mapRef.type();

        Assertions.assertNotNull(mapType);
        Assertions.assertEquals(Map.class, mapType.clazz());
    }

    @Test
    public void testTypeReferenceNestedGenerics() {
        TypeReference<Map<String, List<Set<Integer>>>> complexRef = new TypeReference<Map<String, List<Set<Integer>>>>() {
        };
        Type<Map<String, List<Set<Integer>>>> complexType = complexRef.type();

        Assertions.assertNotNull(complexType);
        Assertions.assertEquals(Map.class, complexType.clazz());
    }

    @Test
    public void testTypeReferenceArray() {
        TypeReference<String[]> arrayRef = new TypeReference<String[]>() {
        };
        Type<String[]> arrayType = arrayRef.type();

        Assertions.assertNotNull(arrayType);
        Assertions.assertEquals(String[].class, arrayType.clazz());
    }

    @Test
    public void testTypeReferencePrimitive() {
        TypeReference<Integer> intRef = new TypeReference<Integer>() {
        };
        Type<Integer> intType = intRef.type();

        Assertions.assertNotNull(intType);
        Assertions.assertEquals(Integer.class, intType.clazz());
    }

    @Test
    public void testTypeReferenceCustomClass() {
        TypeReference<TestBean> beanRef = new TypeReference<TestBean>() {
        };
        Type<TestBean> beanType = beanRef.type();

        Assertions.assertNotNull(beanType);
        Assertions.assertEquals(TestBean.class, beanType.clazz());
    }

    @Test
    public void testTypeReferenceGenericCustomClass() {
        TypeReference<GenericBean<String, Integer>> genericBeanRef = new TypeReference<GenericBean<String, Integer>>() {
        };
        Type<GenericBean<String, Integer>> genericBeanType = genericBeanRef.type();

        Assertions.assertNotNull(genericBeanType);
        Assertions.assertEquals(GenericBean.class, genericBeanType.clazz());
    }

    @Test
    public void testTypeTokenSimpleType() {
        TypeReference.TypeToken<String> stringToken = new TypeReference.TypeToken<String>() {
        };
        Type<String> stringType = stringToken.type();

        Assertions.assertNotNull(stringType);
        Assertions.assertEquals(String.class, stringType.clazz());
    }

    @Test
    public void testTypeTokenGenericType() {
        TypeReference.TypeToken<List<String>> listToken = new TypeReference.TypeToken<List<String>>() {
        };
        Type<List<String>> listType = listToken.type();

        Assertions.assertNotNull(listType);
        Assertions.assertEquals(List.class, listType.clazz());
    }

    @Test
    public void testTypeTokenIsTypeReference() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };
        Assertions.assertTrue(token instanceof TypeReference);
    }

    @Test
    public void testMultipleInstancesAreDifferent() {
        TypeReference<List<String>> ref1 = new TypeReference<List<String>>() {
        };
        TypeReference<List<String>> ref2 = new TypeReference<List<String>>() {
        };

        Assertions.assertNotSame(ref1, ref2);

        Assertions.assertEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testTypeReferenceWithBounds() {
        TypeReference<List<? extends Number>> boundedRef = new TypeReference<List<? extends Number>>() {
        };
        Type<List<? extends Number>> boundedType = boundedRef.type();

        Assertions.assertNotNull(boundedType);
        Assertions.assertEquals(List.class, boundedType.clazz());
    }

    @Test
    public void testTypeReferenceEquals() {
        TypeReference<String> ref1 = new TypeReference<String>() {
        };
        TypeReference<String> ref2 = new TypeReference<String>() {
        };
        TypeReference<Integer> ref3 = new TypeReference<Integer>() {
        };

        Assertions.assertEquals(ref1.type(), ref2.type());

        Assertions.assertNotEquals(ref1.type(), ref3.type());
    }

    @Test
    public void testComplexNestedType() {
        TypeReference<Map<String, Map<Integer, List<Set<String>>>>> complexRef = new TypeReference<Map<String, Map<Integer, List<Set<String>>>>>() {
        };

        Type<Map<String, Map<Integer, List<Set<String>>>>> complexType = complexRef.type();

        Assertions.assertNotNull(complexType);
        Assertions.assertEquals(Map.class, complexType.clazz());
    }

    @Test
    public void testTypeReferenceNotInstantiatedDirectly() {

        TypeReference<String> ref = new TypeReference<String>() {
        };
        Assertions.assertNotNull(ref);

        Assertions.assertTrue(ref.getClass().isAnonymousClass());
    }

    @Test
    public void testTypeTokenNotInstantiatedDirectly() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };
        Assertions.assertNotNull(token);

        Assertions.assertTrue(token.getClass().isAnonymousClass());
    }

    public static class TestBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class GenericBean<T, U> {
        private T first;
        private U second;

        public T getFirst() {
            return first;
        }

        public void setFirst(T first) {
            this.first = first;
        }

        public U getSecond() {
            return second;
        }

        public void setSecond(U second) {
            this.second = second;
        }
    }
}
