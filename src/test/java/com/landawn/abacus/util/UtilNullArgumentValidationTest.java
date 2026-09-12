package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class UtilNullArgumentValidationTest extends TestBase {
    @Test
    public void requiredTypeTokensAreRejectedBeforeConversion() {
        assertThrows(IllegalArgumentException.class, () -> N.convert(null, (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> N.convert("text", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> N.convert(null, (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> N.castIfAssignable(null, (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> N.newCollection(null, 0));
        assertThrows(IllegalArgumentException.class, () -> N.newMap(null, 0));
        assertThrows(IllegalArgumentException.class, () -> N.newInstance((Class<?>) null));
        assertNull(N.convert(null, String.class));
        assertEquals(123, N.convert("123", Integer.class));
    }

    @Test
    public void metadataAndStandaloneUtilitiesValidateArguments() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getClassName(null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getDeclaredMethod(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.invokeConstructor(null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.createMethodHandle(null));
        assertThrows(IllegalArgumentException.class, () -> DataSourceUtil.executeBatch(null));
        assertThrows(IllegalArgumentException.class, () -> EntityId.create(null));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.toRuntimeException((Exception) null));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.toRuntimeException((Throwable) null));
        assertThrows(IllegalArgumentException.class, () -> ExceptionUtil.tryToGetOriginalCheckedException(null));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.JAVA_1_8.atLeast(null));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.JAVA_1_8.atMost(null));
        assertThrows(IllegalArgumentException.class, () -> LockMode.R.isXLockOf(null));
        assertThrows(IllegalArgumentException.class, () -> KahanSummation.of((double[]) null));
        assertThrows(IllegalArgumentException.class, () -> new KahanSummation().addAll(null));
        assertThrows(IllegalArgumentException.class, () -> new KahanSummation().combine(null));
    }

    @Test
    public void allPrimitiveListArrayBoundariesAgree() throws Exception {
        final Class<?>[] primitives = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class };
        final Class<?>[] lists = { BooleanList.class, ByteList.class, CharList.class, ShortList.class, IntList.class, LongList.class, FloatList.class, DoubleList.class };
        for (int i = 0; i < lists.length; i++) {
            final Class<?> arrayType = java.lang.reflect.Array.newInstance(primitives[i], 0).getClass();
            final Class<?> listType = lists[i];
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> listType.getConstructor(arrayType).newInstance((Object) null)).getCause());
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> listType.getConstructor(arrayType, int.class).newInstance(null, -1)).getCause());
            final Method copy = listType.getMethod("copyOf", arrayType, int.class, int.class);
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> copy.invoke(null, null, 0, 0)).getCause());
        }
    }

    @Test
    public void digestRejectsMissingDataBeforeMutatingItsState() throws Exception {
        final MessageDigest digest = DigestUtil.getSha256Digest();
        digest.update((byte) 1);
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.digest(digest, (byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(digest, (String) null));
        assertEquals(Hex.encodeToString(DigestUtil.sha256(new byte[] { 1 })), Hex.encodeToString(digest.digest()));
    }

    @Test
    public void mapInterfaceAndIteratorContractsRetainNullPointerException() {
        final BiMap<String, Integer> map = new BiMap<>();
        assertThrows(IllegalArgumentException.class, () -> BiMap.copyOf(null));
        assertThrows(IllegalArgumentException.class, () -> map.forcePutAll(null));
        assertThrows(NullPointerException.class, () -> ((Map<String, Integer>) map).putAll(null));
        assertThrows(NullPointerException.class, () -> IntIterator.empty().forEachRemaining((Consumer<Integer>) null));
        assertThrows(NullPointerException.class, () -> N.requireNonNull(null));
        assertThrows(NullPointerException.class, () -> MutableInt.of(1).compareTo(null));
    }
}
