package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TypeReferenceTest extends TestBase {

    private abstract static class SecondTypeReference<A, B> extends TypeReference<B> {
    }

    private abstract static class NestedTypeReference<T> extends SecondTypeReference<String, List<T>> {
    }

    private abstract static class GenericArrayTypeReference<T> extends TypeReference<T[]> {
    }

    private abstract static class WildcardTypeReference<T> extends TypeReference<List<? extends T>> {
    }

    private static class GenericOwner<T> {
        class Member<U> {
        }
    }

    private abstract static class OwnerTypeReference<T> extends TypeReference<GenericOwner<T>.Member<String>> {
    }

    private static class ReferenceOwner<A, B> {
        class Reference<C> extends TypeReference<Map<A, List<? extends B[]>>> {
        }

        class SwappedReference extends ReferenceOwner<B, A>.Reference<Integer> {
            SwappedReference(ReferenceOwner<B, A> owner) {
                owner.super();
            }
        }

        class RecursiveReference extends ReferenceOwner<List<A>, B>.Reference<Integer> {
            RecursiveReference(ReferenceOwner<List<A>, B> owner) {
                owner.super();
            }
        }

        class NestedOwner<C> {
            class Reference extends TypeReference<Map<A, Map<B, C>>> {
            }
        }
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

    private static <T> void createUnresolvedTypeReference() {
        new TypeReference<T>() {
        };
    }

    @Test
    public void testTypeAndJavaType() {
        TypeReference<String> stringRef = new TypeReference<>() {
        };
        assertTrue(stringRef.getClass().isAnonymousClass());
        assertEquals(String.class, stringRef.javaType());
        assertEquals(String.class, stringRef.type().javaType());
        assertFalse(stringRef.javaType() instanceof ParameterizedType);
        assertSame(stringRef.javaType(), stringRef.javaType());
        assertSame(stringRef.type(), stringRef.type());

        TypeReference<String> other = new TypeReference<>() {
        };
        assertNotSame(stringRef, other);
        assertEquals(stringRef.type(), other.type());
        assertEquals(stringRef.javaType(), other.javaType());
        assertNotEquals(stringRef.type(), new TypeReference<Integer>() {
        }.type());

        TypeReference<Integer> intRef = new TypeReference<>() {
        };
        assertEquals(Integer.class, intRef.javaType());
        assertEquals(Integer.class, intRef.type().javaType());

        TypeReference<TestBean> beanRef = new TypeReference<>() {
        };
        assertEquals(TestBean.class, beanRef.type().javaType());
        TypeReference<GenericBean<String, Integer>> genericBeanRef = new TypeReference<>() {
        };
        assertEquals(GenericBean.class, genericBeanRef.type().javaType());

        TypeReference<String[]> arrayRef = new TypeReference<>() {
        };
        assertEquals(String[].class, arrayRef.javaType());
        assertEquals(String[].class, arrayRef.type().javaType());
        assertEquals(String[][].class, new TypeReference<String[][]>() {
        }.type().javaType());
        assertEquals(int[].class, new TypeReference<int[]>() {
        }.type().javaType());
        assertEquals(Integer[].class, new TypeReference<Integer[]>() {
        }.type().javaType());

        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference rawRef = new TypeReference() {
            };
        });
        assertThrows(IllegalArgumentException.class, TypeReferenceTest::createUnresolvedTypeReference);
    }

    @Test
    public void testGenericAndWildcardTypes() {
        TypeReference<List<String>> listRef = new TypeReference<>() {
        };
        assertEquals(List.class, listRef.type().javaType());
        assertTrue(listRef.javaType() instanceof ParameterizedType);
        ParameterizedType listPt = (ParameterizedType) listRef.javaType();
        assertEquals(List.class, listPt.getRawType());
        assertSame(listRef.javaType(), listRef.javaType());
        assertSame(listRef.type(), listRef.type());
        assertEquals(listRef.type(), new TypeReference<List<String>>() {
        }.type());
        assertNotEquals(listRef.type(), new TypeReference<List<Integer>>() {
        }.type());
        assertNotEquals(new TypeReference<ArrayList<String>>() {
        }.javaType(), new TypeReference<LinkedList<String>>() {
        }.javaType());

        TypeReference<Map<String, Integer>> mapRef = new TypeReference<>() {
        };
        assertEquals(Map.class, mapRef.type().javaType());
        ParameterizedType mapPt = (ParameterizedType) mapRef.javaType();
        assertEquals(Map.class, mapPt.getRawType());
        assertEquals(2, mapPt.getActualTypeArguments().length);
        assertEquals(String.class, mapPt.getActualTypeArguments()[0]);
        assertEquals(Integer.class, mapPt.getActualTypeArguments()[1]);
        assertNotEquals(new TypeReference<HashMap<String, Integer>>() {
        }.type(), new TypeReference<TreeMap<String, Integer>>() {
        }.type());

        TypeReference<Map<String, List<Set<Integer>>>> nestedRef = new TypeReference<>() {
        };
        assertEquals(Map.class, nestedRef.type().javaType());
        assertTrue(nestedRef.javaType() instanceof ParameterizedType);
        assertEquals(Map.class, ((ParameterizedType) nestedRef.javaType()).getRawType());

        TypeReference<Map<String, List<Integer>>> nestedListRef = new TypeReference<>() {
        };
        ParameterizedType nestedMap = (ParameterizedType) nestedListRef.javaType();
        assertEquals(Map.class, nestedMap.getRawType());
        assertEquals(String.class, nestedMap.getActualTypeArguments()[0]);
        assertTrue(nestedMap.getActualTypeArguments()[1] instanceof ParameterizedType);

        TypeReference<Map<String, Map<Integer, List<Set<String>>>>> deepRef = new TypeReference<>() {
        };
        assertEquals(Map.class, deepRef.type().javaType());

        TypeReference<List<? extends Number>> upper = new TypeReference<>() {
        };
        assertEquals(List.class, upper.type().javaType());
        assertTrue(upper.javaType() instanceof ParameterizedType);
        TypeReference<List<? super Integer>> lower = new TypeReference<>() {
        };
        assertTrue(lower.javaType() instanceof ParameterizedType);
    }

    @Test
    public void testResolvesReorderedAndNestedTypeParameters() {
        TypeReference<Integer> reordered = new SecondTypeReference<String, Integer>() {
        };
        assertEquals(Integer.class, reordered.javaType());
        assertEquals(Integer.class, reordered.type().javaType());

        TypeReference<List<Integer>> nested = new NestedTypeReference<>() {
        };
        assertTrue(nested.javaType() instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) nested.javaType();
        assertEquals(List.class, parameterizedType.getRawType());
        assertArrayEquals(new java.lang.reflect.Type[] { Integer.class }, parameterizedType.getActualTypeArguments());
        assertEquals(List.class, nested.type().javaType());

        TypeReference<String[]> arrayRef = new GenericArrayTypeReference<>() {
        };
        TypeReference<List<? extends Number>> wildcardRef = new WildcardTypeReference<>() {
        };
        assertEquals(String[].class, arrayRef.javaType());
        ParameterizedType listType = (ParameterizedType) wildcardRef.javaType();
        WildcardType wildcard = (WildcardType) listType.getActualTypeArguments()[0];
        assertArrayEquals(new java.lang.reflect.Type[] { Number.class }, wildcard.getUpperBounds());
        assertEquals(List.class, wildcardRef.type().javaType());

        TypeReference<GenericOwner<Integer>.Member<String>> ownerRef = new OwnerTypeReference<Integer>() {
        };
        String typeName = ownerRef.javaType().getTypeName();
        assertTrue(typeName.contains("GenericOwner<java.lang.Integer>"), typeName);
        assertTrue(typeName.endsWith("$Member<java.lang.String>"), typeName);
    }

    @Test
    public void testResolvesOwnerArguments() {
        ReferenceOwner<String, Number> owner = new ReferenceOwner<>();
        TypeReference<Map<String, List<? extends Number[]>>> actual = owner.new Reference<Integer>() {
        };
        TypeReference<Map<String, List<? extends Number[]>>> expected = new TypeReference<>() {
        };
        assertEquals(expected.javaType(), actual.javaType());
        assertEquals(expected.hashCode(), actual.hashCode());
        assertEquals(expected.type(), actual.type());

        TypeReference<Map<Number, List<? extends String[]>>> swapped = owner.new SwappedReference(new ReferenceOwner<>()) {
        };
        TypeReference<Map<Number, List<? extends String[]>>> swappedExpected = new TypeReference<>() {
        };
        assertEquals(swappedExpected.javaType(), swapped.javaType());
        assertEquals(swappedExpected.type(), swapped.type());

        ReferenceOwner<String, Integer> nestedOwnerRoot = new ReferenceOwner<>();
        ReferenceOwner<String, Integer>.NestedOwner<Long> nestedOwner = nestedOwnerRoot.new NestedOwner<>();
        TypeReference<Map<String, Map<Integer, Long>>> nested = nestedOwner.new Reference() {
        };
        TypeReference<Map<String, Map<Integer, Long>>> nestedExpected = new TypeReference<>() {
        };
        assertEquals(nestedExpected.javaType(), nested.javaType());
        assertEquals(nestedExpected.type(), nested.type());
        assertThrows(IllegalArgumentException.class, () -> nestedOwnerRoot.new Reference<Integer>());

        @SuppressWarnings({ "rawtypes", "unchecked" })
        ReferenceOwner rawOwner = new ReferenceOwner();
        assertThrows(IllegalArgumentException.class, () -> rawOwner.new SwappedReference(new ReferenceOwner()));
        assertThrows(IllegalArgumentException.class, () -> rawOwner.new RecursiveReference(new ReferenceOwner()));
    }

    @Test
    public void testTypeToken() {
        TypeReference.TypeToken<String> token = new TypeReference.TypeToken<>() {
        };
        assertTrue(token instanceof TypeReference);
        assertTrue(token.getClass().isAnonymousClass());
        assertEquals(String.class, token.type().javaType());
        TypeReference<String> ref = new TypeReference<>() {
        };
        assertEquals(ref.type(), token.type());

        TypeReference<List<String>> listRef = new TypeReference<>() {
        };
        TypeReference.TypeToken<List<String>> listToken = new TypeReference.TypeToken<>() {
        };
        assertEquals(listRef.javaType(), listToken.javaType());
        assertEquals(listRef.type(), listToken.type());
        assertEquals(List.class, listToken.type().javaType());
        assertTrue(listToken.javaType() instanceof ParameterizedType);

        TypeReference.TypeToken<Map<String, List<Integer>>> complex = new TypeReference.TypeToken<>() {
        };
        assertEquals(Map.class, complex.type().javaType());

        assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference.TypeToken rawToken = new TypeReference.TypeToken() {
            };
        });
    }

    @Test
    public void testReflectTypeIsNotInterchangeableWithTypeReflectType() {
        final TypeReference<List<String>> ref = new TypeReference<>() {
        };

        assertEquals("java.util.List<java.lang.String>", ref.reflectType().getTypeName());
        assertSame(ref.javaType(), ref.reflectType());

        // Type#reflectType() is implemented in AbstractType as javaType(), so routing through type() drops the
        // type arguments: the two reflectType() methods read alike but are NOT a one-word call chain.
        assertEquals(List.class, ref.type().reflectType());
        assertEquals(List.class, ref.type().javaType());
    }

    // ---- deep review 2026-11-02b G083 begin ----
    private static class G083DirectGenericReference<Z> extends TypeReference<Map<String, Z[]>> {
    }

    private static class G083DirectGenericToken<Z> extends TypeReference.TypeToken<List<Z>> {
    }

    private static <M> void g083CreateLocalUnresolvedReference() {
        class LocalReference extends TypeReference<List<? extends M>> {
        }

        new LocalReference();
    }

    private static void g083AssertCapturesDirectTypeArgument(final TypeReference<?> ref) {
        final java.lang.reflect.Type typeArgument = ((ParameterizedType) ref.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        assertSame(typeArgument, ref.javaType(), ref.getClass().getName());
        assertSame(ref.javaType(), ref.reflectType());
        assertEquals(typeArgument.getTypeName(), ref.toString());
    }

    // G083-07: pins capture for a direct TypeReference/TypeToken subclass (the construction fast path): the result is the
    // JDK's own type-argument object, and unresolved/raw declarations keep their exception type and message.
    @Test
    public void testDirectSubclassCapture_r116() {
        g083AssertCapturesDirectTypeArgument(new TypeReference<String>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<int[]>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<String[][]>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<List<String>>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<List<String>[]>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<Map<? extends Number, ? super Integer>>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference<Map.Entry<String, List<? extends CharSequence>>>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference.TypeToken<String>() {
        });
        g083AssertCapturesDirectTypeArgument(new TypeReference.TypeToken<Map<String, int[]>[]>() {
        });

        assertEquals(String[][].class, new TypeReference<String[][]>() {
        }.javaType());
        assertTrue(new TypeReference<List<String>[]>() {
        }.javaType() instanceof java.lang.reflect.GenericArrayType);
        assertEquals(new TypeReference<List<String>>() {
        }, new TypeReference.TypeToken<List<String>>() {
        });
        assertEquals(new SecondTypeReference<String, List<Integer>>() {
        }, new TypeReference<List<Integer>>() {
        });
        assertEquals(new SecondTypeReference<String, List<Integer>>() {
        }.hashCode(), new TypeReference<List<Integer>>() {
        }.hashCode());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new G083DirectGenericReference<String>());
        assertEquals("TypeReference constructed without concrete type information: java.util.Map<java.lang.String, Z[]>", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> new G083DirectGenericToken<String>());
        assertEquals("TypeReference constructed without concrete type information: java.util.List<Z>", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, TypeReferenceTest::g083CreateLocalUnresolvedReference);
        assertEquals("TypeReference constructed without concrete type information: java.util.List<? extends M>", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, TypeReferenceTest::createUnresolvedTypeReference);
        assertEquals("TypeReference constructed without concrete type information: T", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference rawRef = new TypeReference() {
            };
        });
        assertEquals("TypeReference constructed without actual type information", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> {
            @SuppressWarnings("rawtypes")
            TypeReference.TypeToken rawToken = new TypeReference.TypeToken() {
            };
        });
        assertEquals("TypeReference constructed without concrete type information: T", e.getMessage());
    }
    // ---- deep review 2026-11-02b G083 end ----
}
