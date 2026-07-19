package com.landawn.abacus.util;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class TypeReferenceTest extends TestBase {

    private abstract static class SecondTypeReference<A, B> extends TypeReference<B> {
        // Used to verify that TypeReference follows the declared mapping to B rather than
        // blindly selecting the first type argument (A) from the immediate superclass.
    }

    private abstract static class NestedTypeReference<T> extends SecondTypeReference<String, List<T>> {
        // Exercises substitution inside a nested ParameterizedType across two named levels.
    }

    private abstract static class GenericArrayTypeReference<T> extends TypeReference<T[]> {
        // Exercises a type variable used as a generic-array component.
    }

    private abstract static class WildcardTypeReference<T> extends TypeReference<List<? extends T>> {
        // Exercises a type variable used as a wildcard bound.
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

    @Test
    public void testTypeReference_DifferentMapImplementations() {
        TypeReference<HashMap<String, Integer>> ref1 = new TypeReference<>() {
        };
        TypeReference<TreeMap<String, Integer>> ref2 = new TypeReference<>() {
        };

        Assertions.assertNotEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testTypeReference_IsAnonymousClass() {
        TypeReference<String> ref = new TypeReference<>() {
        };

        Assertions.assertTrue(ref.getClass().isAnonymousClass());
    }

    @Test
    public void testTypeReferenceEquals() {
        TypeReference<String> ref1 = new TypeReference<>() {
        };
        TypeReference<String> ref2 = new TypeReference<>() {
        };
        TypeReference<Integer> ref3 = new TypeReference<>() {
        };

        Assertions.assertEquals(ref1.type(), ref2.type());

        Assertions.assertNotEquals(ref1.type(), ref3.type());
    }

    @Test
    public void testTypeReference_WithCustomClass() {
        TypeReference<TestBean> ref = new TypeReference<>() {
        };
        Type<TestBean> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(TestBean.class, type.javaType());
    }

    @Test
    public void testTypeReference_WithGenericCustomClass() {
        TypeReference<GenericBean<String, Integer>> ref = new TypeReference<>() {
        };
        Type<GenericBean<String, Integer>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(GenericBean.class, type.javaType());
    }

    @Test
    public void testTypeReferenceResolvesReorderedIntermediateTypeParameters() {
        final TypeReference<Integer> ref = new SecondTypeReference<String, Integer>() {
        };

        Assertions.assertEquals(Integer.class, ref.javaType());
        Assertions.assertEquals(Integer.class, ref.type().javaType());
    }

    @Test
    public void testTypeReferenceResolvesNestedIntermediateTypeParameters() {
        final TypeReference<List<Integer>> ref = new NestedTypeReference<Integer>() {
        };

        Assertions.assertTrue(ref.javaType() instanceof ParameterizedType);
        final ParameterizedType parameterizedType = (ParameterizedType) ref.javaType();
        Assertions.assertEquals(List.class, parameterizedType.getRawType());
        Assertions.assertArrayEquals(new java.lang.reflect.Type[] { Integer.class }, parameterizedType.getActualTypeArguments());
        Assertions.assertEquals(List.class, ref.type().javaType());
    }

    @Test
    public void testTypeReferenceResolvesIntermediateGenericArrayAndWildcardBounds() {
        final TypeReference<String[]> arrayRef = new GenericArrayTypeReference<String>() {
        };
        final TypeReference<List<? extends Number>> wildcardRef = new WildcardTypeReference<Number>() {
        };

        Assertions.assertEquals(String[].class, arrayRef.javaType());
        final ParameterizedType listType = (ParameterizedType) wildcardRef.javaType();
        final java.lang.reflect.WildcardType wildcard = (java.lang.reflect.WildcardType) listType.getActualTypeArguments()[0];
        Assertions.assertArrayEquals(new java.lang.reflect.Type[] { Number.class }, wildcard.getUpperBounds());
        Assertions.assertEquals(List.class, wildcardRef.type().javaType());
    }

    @Test
    public void testTypeReferenceRejectsUnresolvedTypeVariableWithIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, TypeReferenceTest::createUnresolvedTypeReference);
    }

    private static <T> void createUnresolvedTypeReference() {
        new TypeReference<T>() {
        };
    }

    @Test
    public void testTypeReference_MultiDimensionalArray() {
        TypeReference<String[][]> ref = new TypeReference<>() {
        };
        Type<String[][]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String[][].class, type.javaType());
    }

    @Test
    public void testTypeReference_PrimitiveArray() {
        TypeReference<int[]> ref = new TypeReference<>() {
        };
        Type<int[]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(int[].class, type.javaType());
    }

    @Test
    public void testTypeReference_MultipleInstances() {
        TypeReference<String> ref1 = new TypeReference<>() {
        };
        TypeReference<String> ref2 = new TypeReference<>() {
        };

        Assertions.assertNotSame(ref1, ref2);

        Assertions.assertEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testTypeReferenceSimpleType() {
        TypeReference<String> stringRef = new TypeReference<>() {
        };
        Type<String> stringType = stringRef.type();

        Assertions.assertNotNull(stringType);
        Assertions.assertEquals(String.class, stringType.javaType());
    }

    @Test
    public void testTypeReferenceGenericList() {
        TypeReference<List<String>> listRef = new TypeReference<>() {
        };
        Type<List<String>> listType = listRef.type();

        Assertions.assertNotNull(listType);
        Assertions.assertEquals(List.class, listType.javaType());
    }

    @Test
    public void testTypeReferenceGenericMap() {
        TypeReference<Map<String, Integer>> mapRef = new TypeReference<>() {
        };
        Type<Map<String, Integer>> mapType = mapRef.type();

        Assertions.assertNotNull(mapType);
        Assertions.assertEquals(Map.class, mapType.javaType());
    }

    @Test
    public void testTypeReferenceNestedGenerics() {
        TypeReference<Map<String, List<Set<Integer>>>> complexRef = new TypeReference<>() {
        };
        Type<Map<String, List<Set<Integer>>>> complexType = complexRef.type();

        Assertions.assertNotNull(complexType);
        Assertions.assertEquals(Map.class, complexType.javaType());
    }

    @Test
    public void testTypeReferenceArray() {
        TypeReference<String[]> arrayRef = new TypeReference<>() {
        };
        Type<String[]> arrayType = arrayRef.type();

        Assertions.assertNotNull(arrayType);
        Assertions.assertEquals(String[].class, arrayType.javaType());
    }

    @Test
    public void testTypeReferencePrimitive() {
        TypeReference<Integer> intRef = new TypeReference<>() {
        };
        Type<Integer> intType = intRef.type();

        Assertions.assertNotNull(intType);
        Assertions.assertEquals(Integer.class, intType.javaType());
    }

    @Test
    public void testTypeReferenceCustomClass() {
        TypeReference<TestBean> beanRef = new TypeReference<>() {
        };
        Type<TestBean> beanType = beanRef.type();

        Assertions.assertNotNull(beanType);
        Assertions.assertEquals(TestBean.class, beanType.javaType());
    }

    @Test
    public void testTypeReferenceGenericCustomClass() {
        TypeReference<GenericBean<String, Integer>> genericBeanRef = new TypeReference<>() {
        };
        Type<GenericBean<String, Integer>> genericBeanType = genericBeanRef.type();

        Assertions.assertNotNull(genericBeanType);
        Assertions.assertEquals(GenericBean.class, genericBeanType.javaType());
    }

    @Test
    public void testTypeReferenceWithBounds() {
        TypeReference<List<? extends Number>> boundedRef = new TypeReference<>() {
        };
        Type<List<? extends Number>> boundedType = boundedRef.type();

        Assertions.assertNotNull(boundedType);
        Assertions.assertEquals(List.class, boundedType.javaType());
    }

    @Test
    public void testTypeReferenceNotInstantiatedDirectly() {

        TypeReference<String> ref = new TypeReference<>() {
        };
        Assertions.assertNotNull(ref);

        Assertions.assertTrue(ref.getClass().isAnonymousClass());
    }

    @Test
    public void testConstructor_WithoutTypeInformation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference rawRef = new TypeReference() {
            };
        });
    }

    @Test
    public void testGetType_DifferentImplementations() {
        TypeReference<ArrayList<String>> ref1 = new TypeReference<>() {
        };
        TypeReference<LinkedList<String>> ref2 = new TypeReference<>() {
        };

        java.lang.reflect.Type type1 = ref1.javaType();
        java.lang.reflect.Type type2 = ref2.javaType();

        Assertions.assertNotEquals(type1, type2);
    }

    @Test
    public void testJavaType_returnsClassForSimpleType() {
        TypeReference<Integer> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type jt = ref.javaType();

        Assertions.assertEquals(Integer.class, jt);
        Assertions.assertFalse(jt instanceof ParameterizedType);
    }

    @Test
    public void testJavaType_matchesTypeJavaType() {
        TypeReference<String> ref = new TypeReference<>() {
        };
        Assertions.assertEquals(String.class, ref.javaType());
        Assertions.assertEquals(String.class, ref.type().javaType());
    }

    @Test
    public void testGetType_SimpleType() {
        TypeReference<String> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type);
    }

    @Test
    public void testGetType_GenericList() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(List.class, pt.getRawType());
    }

    @Test
    public void testGetType_GenericMap() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(Map.class, pt.getRawType());
        Assertions.assertEquals(2, pt.getActualTypeArguments().length);
    }

    @Test
    public void testGetType_ComplexNestedGeneric() {
        TypeReference<Map<String, List<Set<Integer>>>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(Map.class, pt.getRawType());
    }

    @Test
    public void testGetType_ArrayType() {
        TypeReference<String[]> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String[].class, type);
    }

    @Test
    public void testGetType_PrimitiveWrapper() {
        TypeReference<Integer> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Integer.class, type);
    }

    @Test
    public void testGetType_WildcardUpperBound() {
        TypeReference<List<? extends Number>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testGetType_WildcardLowerBound() {
        TypeReference<List<? super Integer>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testGetType_MultipleTypeParameters() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type type = ref.javaType();

        Assertions.assertNotNull(type);
        ParameterizedType pt = (ParameterizedType) type;
        java.lang.reflect.Type[] args = pt.getActualTypeArguments();

        Assertions.assertEquals(2, args.length);
        Assertions.assertEquals(String.class, args[0]);
        Assertions.assertEquals(Integer.class, args[1]);
    }

    @Test
    public void testGetType_ConsistentAcrossCalls() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };

        java.lang.reflect.Type type1 = ref.javaType();
        java.lang.reflect.Type type2 = ref.javaType();

        Assertions.assertSame(type1, type2);
    }

    @Test
    public void testGetType_AndType_Consistency() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };

        java.lang.reflect.Type rawType = ref.javaType();
        Type<List<String>> abacusType = ref.type();

        Assertions.assertNotNull(rawType);
        Assertions.assertNotNull(abacusType);
    }

    @Test
    public void testJavaType_returnsParameterizedTypeForGeneric() {
        TypeReference<Map<String, List<Integer>>> ref = new TypeReference<>() {
        };
        java.lang.reflect.Type jt = ref.javaType();

        Assertions.assertNotNull(jt);
        Assertions.assertTrue(jt instanceof ParameterizedType);
        ParameterizedType pt = (ParameterizedType) jt;
        Assertions.assertEquals(Map.class, pt.getRawType());
        Assertions.assertEquals(2, pt.getActualTypeArguments().length);
        Assertions.assertEquals(String.class, pt.getActualTypeArguments()[0]);
        Assertions.assertTrue(pt.getActualTypeArguments()[1] instanceof ParameterizedType);
    }

    @Test
    public void testType_NotEqualForDifferentGenericType() {
        TypeReference<List<String>> ref1 = new TypeReference<>() {
        };
        TypeReference<List<Integer>> ref2 = new TypeReference<>() {
        };

        Type<List<String>> type1 = ref1.type();
        Type<List<Integer>> type2 = ref2.type();

        Assertions.assertNotEquals(type1, type2);
    }

    @Test
    public void testTypeToken_ExtendsTypeReference() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };

        Assertions.assertTrue(token instanceof TypeReference);
    }

    @Test
    public void testTypeToken_IsAnonymousClass() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };

        Assertions.assertTrue(token.getClass().isAnonymousClass());
    }

    @Test
    public void testTypeToken_EqualityWithTypeReference() {
        TypeReference<String> ref = new TypeReference<>() {
        };
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };

        Assertions.assertEquals(ref.type(), token.type());
    }

    @Test
    public void testTypeToken_AndTypeReference_BothWork() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };
        TypeReference.TypeToken<List<String>> token = new TypeReference.TypeToken<>() {
        };

        Assertions.assertEquals(ref.javaType(), token.javaType());
        Assertions.assertEquals(ref.type(), token.type());
    }

    @Test
    public void testType_SimpleType() {
        TypeReference<String> ref = new TypeReference<>() {
        };
        Type<String> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type.javaType());
    }

    @Test
    public void testType_GenericList() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };
        Type<List<String>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.javaType());
    }

    @Test
    public void testType_GenericMap() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<>() {
        };
        Type<Map<String, Integer>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.javaType());
    }

    @Test
    public void testType_ComplexNestedGeneric() {
        TypeReference<Map<String, List<Integer>>> ref = new TypeReference<>() {
        };
        Type<Map<String, List<Integer>>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.javaType());
    }

    @Test
    public void testType_ArrayType() {
        TypeReference<Integer[]> ref = new TypeReference<>() {
        };
        Type<Integer[]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Integer[].class, type.javaType());
    }

    @Test
    public void testType_NotNull() {
        TypeReference<String> ref = new TypeReference<>() {
        };
        Type<String> type = ref.type();

        Assertions.assertNotNull(type);
    }

    @Test
    public void testType_ConsistentAcrossCalls() {
        TypeReference<List<String>> ref = new TypeReference<>() {
        };

        Type<List<String>> type1 = ref.type();
        Type<List<String>> type2 = ref.type();

        Assertions.assertSame(type1, type2);
    }

    @Test
    public void testType_EqualForSameGenericType() {
        TypeReference<List<String>> ref1 = new TypeReference<>() {
        };
        TypeReference<List<String>> ref2 = new TypeReference<>() {
        };

        Type<List<String>> type1 = ref1.type();
        Type<List<String>> type2 = ref2.type();

        Assertions.assertEquals(type1, type2);
    }

    @Test
    public void testType_WithBounds() {
        TypeReference<List<? extends Number>> ref = new TypeReference<>() {
        };
        Type<List<? extends Number>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.javaType());
    }

    @Test
    public void testTypeToken_SimpleType() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };
        Type<String> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type.javaType());
    }

    @Test
    public void testTypeToken_GenericType() {
        TypeReference.TypeToken<List<String>> token = new TypeReference.TypeToken<>() {
        };
        Type<List<String>> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.javaType());
    }

    @Test
    public void testTypeToken_GetType() {
        TypeReference.TypeToken<List<Integer>> token = new TypeReference.TypeToken<>() {
        };
        java.lang.reflect.Type type = token.javaType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testTypeToken_ComplexType() {
        TypeReference.TypeToken<Map<String, List<Integer>>> token = new TypeReference.TypeToken<>() {
        };
        Type<Map<String, List<Integer>>> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.javaType());
    }

    @Test
    public void testTypeTokenSimpleType() {
        TypeReference.TypeToken<String> stringToken = new TypeReference.TypeToken<>() {
        };
        Type<String> stringType = stringToken.type();

        Assertions.assertNotNull(stringType);
        Assertions.assertEquals(String.class, stringType.javaType());
    }

    @Test
    public void testTypeTokenGenericType() {
        TypeReference.TypeToken<List<String>> listToken = new TypeReference.TypeToken<>() {
        };
        Type<List<String>> listType = listToken.type();

        Assertions.assertNotNull(listType);
        Assertions.assertEquals(List.class, listType.javaType());
    }

    @Test
    public void testMultipleInstancesAreDifferent() {
        TypeReference<List<String>> ref1 = new TypeReference<>() {
        };
        TypeReference<List<String>> ref2 = new TypeReference<>() {
        };

        Assertions.assertNotSame(ref1, ref2);

        Assertions.assertEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testComplexNestedType() {
        TypeReference<Map<String, Map<Integer, List<Set<String>>>>> complexRef = new TypeReference<>() {
        };

        Type<Map<String, Map<Integer, List<Set<String>>>>> complexType = complexRef.type();

        Assertions.assertNotNull(complexType);
        Assertions.assertEquals(Map.class, complexType.javaType());
    }

    @Test
    public void testTypeTokenNotInstantiatedDirectly() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };
        Assertions.assertNotNull(token);

        Assertions.assertTrue(token.getClass().isAnonymousClass());
    }

    // TypeToken constructor without type information
    @Test
    public void testTypeToken_ConstructorWithoutTypeInformation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference.TypeToken rawToken = new TypeReference.TypeToken() {
            };
        });
    }

}
