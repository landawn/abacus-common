package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

import testfixtures.ScalarContainerFactoryReview20261003Fixtures.AliasList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.Bean;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.ConstrainedList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.FallbackMap;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.FreshList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.FreshMap;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.PrefilledList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.PrefilledMap;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.RetryBean;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.ReusedList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.SuppliedList;
import testfixtures.ScalarContainerFactoryReview20261003Fixtures.SuppliedMap;

@Tag("unit")
class ScalarContainerFactoryReview20261003Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void prefilledCollectionConstructorIsRejected(boolean descriptor) {
        assertThrows(IllegalArgumentException.class, () -> {
            if (descriptor) {
                CommonUtil.convert("value", CommonUtil.typeOf(PrefilledList.class));
            } else {
                CommonUtil.convert("value", PrefilledList.class);
            }
        });
    }

    @Test
    void registeredPrefilledCollectionIsRejectedBeforeMutation() {
        SuppliedList destination = new SuppliedList();
        destination.add("foreign");
        assertTrue(IntFunctions.registerForCollection(SuppliedList.class, capacity -> destination));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert("value", SuppliedList.class));
        assertEquals(List.of("foreign"), destination);
    }

    @Test
    void constrainedCollectionRejectsBeforeAddingTheScalar() {
        ConstrainedList<String> destination = new ConstrainedList<>();
        destination.add("foreign");
        assertTrue(IntFunctions.registerForCollection(ConstrainedList.class, capacity -> destination));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert("value", new TypeReference<ConstrainedList<String>>() {
        }.type()));
        assertEquals(List.of("foreign"), destination);
    }

    @Test
    void nestedScalarCannotBeAddedToAnInputContainerReturnedByFactory() {
        AliasList input = new AliasList();
        input.add("value");
        assertTrue(IntFunctions.registerForCollection(AliasList.class, capacity -> input));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(input, new TypeReference<List<AliasList>>() {
        }.type()));
        assertEquals(List.of("value"), input);
    }

    @Test
    void factoryCannotReuseEarlierScalarDestination() {
        ReusedList destination = new ReusedList();
        assertTrue(IntFunctions.registerForCollection(ReusedList.class, capacity -> destination));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(List.of("first", "second"), new TypeReference<List<ReusedList>>() {
        }.type()));
        assertEquals(List.of("first"), destination);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void prefilledBeanMapConstructorIsRejected(boolean descriptor) {
        assertThrows(IllegalArgumentException.class, () -> {
            if (descriptor) {
                CommonUtil.convert(new Bean(), CommonUtil.typeOf(PrefilledMap.class));
            } else {
                CommonUtil.convert(new Bean(), PrefilledMap.class);
            }
        });
    }

    @Test
    void registeredPrefilledBeanMapIsRejectedBeforeMutation() {
        SuppliedMap destination = new SuppliedMap();
        destination.put("foreign", 9);
        assertTrue(Suppliers.registerForMap(SuppliedMap.class, () -> destination));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new Bean(), SuppliedMap.class));
        assertEquals(Map.of("foreign", 9), destination);
    }

    @Test
    void freshFactoriesAndLibraryStandInsKeepScalarIdentity() {
        Object value = new StringBuilder("value");
        assertSame(value, CommonUtil.convert(value, FreshList.class).get(0));
        assertSame(value, CommonUtil.convert(value, List.class).get(0));
        assertSame(value, CommonUtil.convert(value, ImmutableList.class).get(0));
        assertSame(value, CommonUtil.convert(value, java.util.Set.class).iterator().next());
        assertEquals(Map.of("value", 1), CommonUtil.convert(new Bean(), FreshMap.class));
    }

    @Test
    void assignableRawContainersStillRetainIdentity() {
        PrefilledList source = new PrefilledList();
        PrefilledMap map = new PrefilledMap();
        assertSame(source, CommonUtil.convert(source, PrefilledList.class));
        assertSame(map, CommonUtil.convert(map, PrefilledMap.class));
        assertNull(CommonUtil.convert(null, PrefilledList.class));
    }

    @Test
    void failedMapFactoryStillUsesTheExistingParserFallback() {
        assertTrue(Suppliers.registerForMap(FallbackMap.class, () -> {
            throw new IllegalStateException("factory fails");
        }));
        assertEquals(Map.of("value", 1), CommonUtil.convert(new Bean(), FallbackMap.class));
    }

    @Test
    void failedBeanExtractionStillUsesTheExistingParserFallback() {
        RetryBean bean = new RetryBean();
        assertEquals(Map.of("value", 1), CommonUtil.convert(bean, FreshMap.class));
        assertEquals(2, bean.calls());
    }
}
