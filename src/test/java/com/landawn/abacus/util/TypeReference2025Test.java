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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class TypeReference2025Test extends TestBase {

    @Test
    public void testGetType_SimpleType() {
        TypeReference<String> ref = new TypeReference<String>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type);
    }

    @Test
    public void testGetType_GenericList() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(List.class, pt.getRawType());
    }

    @Test
    public void testGetType_GenericMap() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<Map<String, Integer>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(Map.class, pt.getRawType());
        Assertions.assertEquals(2, pt.getActualTypeArguments().length);
    }

    @Test
    public void testGetType_ComplexNestedGeneric() {
        TypeReference<Map<String, List<Set<Integer>>>> ref = new TypeReference<Map<String, List<Set<Integer>>>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);

        ParameterizedType pt = (ParameterizedType) type;
        Assertions.assertEquals(Map.class, pt.getRawType());
    }

    @Test
    public void testGetType_ArrayType() {
        TypeReference<String[]> ref = new TypeReference<String[]>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String[].class, type);
    }

    @Test
    public void testGetType_PrimitiveWrapper() {
        TypeReference<Integer> ref = new TypeReference<Integer>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Integer.class, type);
    }

    @Test
    public void testGetType_WildcardUpperBound() {
        TypeReference<List<? extends Number>> ref = new TypeReference<List<? extends Number>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testGetType_WildcardLowerBound() {
        TypeReference<List<? super Integer>> ref = new TypeReference<List<? super Integer>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testGetType_MultipleTypeParameters() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<Map<String, Integer>>() {
        };
        java.lang.reflect.Type type = ref.getType();

        Assertions.assertNotNull(type);
        ParameterizedType pt = (ParameterizedType) type;
        java.lang.reflect.Type[] args = pt.getActualTypeArguments();

        Assertions.assertEquals(2, args.length);
        Assertions.assertEquals(String.class, args[0]);
        Assertions.assertEquals(Integer.class, args[1]);
    }

    @Test
    public void testGetType_DifferentImplementations() {
        TypeReference<ArrayList<String>> ref1 = new TypeReference<ArrayList<String>>() {
        };
        TypeReference<LinkedList<String>> ref2 = new TypeReference<LinkedList<String>>() {
        };

        java.lang.reflect.Type type1 = ref1.getType();
        java.lang.reflect.Type type2 = ref2.getType();

        Assertions.assertNotEquals(type1, type2);
    }

    @Test
    public void testGetType_ConsistentAcrossCalls() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };

        java.lang.reflect.Type type1 = ref.getType();
        java.lang.reflect.Type type2 = ref.getType();

        Assertions.assertSame(type1, type2);
    }

    @Test
    public void testType_SimpleType() {
        TypeReference<String> ref = new TypeReference<String>() {
        };
        Type<String> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type.clazz());
    }

    @Test
    public void testType_GenericList() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };
        Type<List<String>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.clazz());
    }

    @Test
    public void testType_GenericMap() {
        TypeReference<Map<String, Integer>> ref = new TypeReference<Map<String, Integer>>() {
        };
        Type<Map<String, Integer>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.clazz());
    }

    @Test
    public void testType_ComplexNestedGeneric() {
        TypeReference<Map<String, List<Integer>>> ref = new TypeReference<Map<String, List<Integer>>>() {
        };
        Type<Map<String, List<Integer>>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.clazz());
    }

    @Test
    public void testType_ArrayType() {
        TypeReference<Integer[]> ref = new TypeReference<Integer[]>() {
        };
        Type<Integer[]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Integer[].class, type.clazz());
    }

    @Test
    public void testType_NotNull() {
        TypeReference<String> ref = new TypeReference<String>() {
        };
        Type<String> type = ref.type();

        Assertions.assertNotNull(type);
    }

    @Test
    public void testType_ConsistentAcrossCalls() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };

        Type<List<String>> type1 = ref.type();
        Type<List<String>> type2 = ref.type();

        Assertions.assertSame(type1, type2);
    }

    @Test
    public void testType_EqualForSameGenericType() {
        TypeReference<List<String>> ref1 = new TypeReference<List<String>>() {
        };
        TypeReference<List<String>> ref2 = new TypeReference<List<String>>() {
        };

        Type<List<String>> type1 = ref1.type();
        Type<List<String>> type2 = ref2.type();

        Assertions.assertEquals(type1, type2);
    }

    @Test
    public void testType_NotEqualForDifferentGenericType() {
        TypeReference<List<String>> ref1 = new TypeReference<List<String>>() {
        };
        TypeReference<List<Integer>> ref2 = new TypeReference<List<Integer>>() {
        };

        Type<List<String>> type1 = ref1.type();
        Type<List<Integer>> type2 = ref2.type();

        Assertions.assertNotEquals(type1, type2);
    }

    @Test
    public void testType_WithBounds() {
        TypeReference<List<? extends Number>> ref = new TypeReference<List<? extends Number>>() {
        };
        Type<List<? extends Number>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.clazz());
    }

    @Test
    public void testTypeToken_SimpleType() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };
        Type<String> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type.clazz());
    }

    @Test
    public void testTypeToken_GenericType() {
        TypeReference.TypeToken<List<String>> token = new TypeReference.TypeToken<List<String>>() {
        };
        Type<List<String>> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(List.class, type.clazz());
    }

    @Test
    public void testTypeToken_ExtendsTypeReference() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };

        Assertions.assertTrue(token instanceof TypeReference);
    }

    @Test
    public void testTypeToken_GetType() {
        TypeReference.TypeToken<List<Integer>> token = new TypeReference.TypeToken<List<Integer>>() {
        };
        java.lang.reflect.Type type = token.getType();

        Assertions.assertNotNull(type);
        Assertions.assertTrue(type instanceof ParameterizedType);
    }

    @Test
    public void testTypeToken_IsAnonymousClass() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };

        Assertions.assertTrue(token.getClass().isAnonymousClass());
    }

    @Test
    public void testTypeToken_ComplexType() {
        TypeReference.TypeToken<Map<String, List<Integer>>> token = new TypeReference.TypeToken<Map<String, List<Integer>>>() {
        };
        Type<Map<String, List<Integer>>> type = token.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(Map.class, type.clazz());
    }

    @Test
    public void testTypeToken_EqualityWithTypeReference() {
        TypeReference<String> ref = new TypeReference<String>() {
        };
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<String>() {
        };

        Assertions.assertEquals(ref.type(), token.type());
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
    public void testTypeReference_WithCustomClass() {
        TypeReference<TestBean> ref = new TypeReference<TestBean>() {
        };
        Type<TestBean> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(TestBean.class, type.clazz());
    }

    @Test
    public void testTypeReference_WithGenericCustomClass() {
        TypeReference<GenericBean<String, Integer>> ref = new TypeReference<GenericBean<String, Integer>>() {
        };
        Type<GenericBean<String, Integer>> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(GenericBean.class, type.clazz());
    }

    @Test
    public void testTypeReference_DifferentMapImplementations() {
        TypeReference<HashMap<String, Integer>> ref1 = new TypeReference<HashMap<String, Integer>>() {
        };
        TypeReference<TreeMap<String, Integer>> ref2 = new TypeReference<TreeMap<String, Integer>>() {
        };

        Assertions.assertNotEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testTypeReference_MultiDimensionalArray() {
        TypeReference<String[][]> ref = new TypeReference<String[][]>() {
        };
        Type<String[][]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(String[][].class, type.clazz());
    }

    @Test
    public void testTypeReference_PrimitiveArray() {
        TypeReference<int[]> ref = new TypeReference<int[]>() {
        };
        Type<int[]> type = ref.type();

        Assertions.assertNotNull(type);
        Assertions.assertEquals(int[].class, type.clazz());
    }

    @Test
    public void testTypeReference_IsAnonymousClass() {
        TypeReference<String> ref = new TypeReference<String>() {
        };

        Assertions.assertTrue(ref.getClass().isAnonymousClass());
    }

    @Test
    public void testTypeReference_MultipleInstances() {
        TypeReference<String> ref1 = new TypeReference<String>() {
        };
        TypeReference<String> ref2 = new TypeReference<String>() {
        };

        Assertions.assertNotSame(ref1, ref2);

        Assertions.assertEquals(ref1.type(), ref2.type());
    }

    @Test
    public void testGetType_AndType_Consistency() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };

        java.lang.reflect.Type rawType = ref.getType();
        Type<List<String>> abacusType = ref.type();

        Assertions.assertNotNull(rawType);
        Assertions.assertNotNull(abacusType);
    }

    @Test
    public void testTypeToken_AndTypeReference_BothWork() {
        TypeReference<List<String>> ref = new TypeReference<List<String>>() {
        };
        TypeReference.TypeToken<List<String>> token = new TypeReference.TypeToken<List<String>>() {
        };

        Assertions.assertEquals(ref.getType(), token.getType());
        Assertions.assertEquals(ref.type(), token.type());
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
