package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.util.EntityId;

public class InheritedIdentifierMetadataTest extends TestBase {
    @Id({ "accountKey", "tenantKey" })
    public static class CompositeBase {
        public long accountKey = 7;
        public long tenantKey = 9;
        public long replacementKey = 11;
    }

    public static class CompositeChild extends CompositeBase {
    }

    @Id("replacementKey")
    public static class CompositeOverride extends CompositeBase {
    }

    @ReadOnlyId("accountKey")
    public static class ReadOnlyBase {
        public long accountKey = 7;
        public long replacementKey = 11;
    }

    public static class ReadOnlyChild extends ReadOnlyBase {
    }

    @ReadOnlyId("replacementKey")
    public static class ReadOnlyOverride extends ReadOnlyBase {
    }

    @Id("accountKey")
    public interface Identified {
        long getAccountKey();
    }

    @ReadOnlyId("tenantKey")
    public interface ReadOnlyIdentified {
        long getTenantKey();
    }

    public static class InterfaceBean implements Identified, ReadOnlyIdentified {
        public long accountKey = 7;
        public long tenantKey = 9;

        @Override
        public long getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(final long accountKey) {
            this.accountKey = accountKey;
        }

        @Override
        public long getTenantKey() {
            return tenantKey;
        }

        public void setTenantKey(final long tenantKey) {
            this.tenantKey = tenantKey;
        }
    }

    @Id("replacementKey")
    public static class MixedChild extends ReadOnlyBase {
    }

    @Id
    public static class EmptyOverride extends CompositeBase {
    }

    @Test
    public void superclassCompositeIdsRemainAvailableToEntityId() {
        final var info = ParserUtil.getBeanInfo(CompositeChild.class);
        assertNotNull(info.getAnnotation(Id.class));
        assertEquals(List.of("accountKey", "tenantKey"), info.idPropNameList);
        assertTrue(info.getPropInfo("accountKey").isMarkedAsId);
        assertTrue(info.getPropInfo("tenantKey").isMarkedAsId);
        final EntityId id = EntityId.create(new CompositeChild());
        assertEquals(Long.valueOf(7), id.get("accountKey"));
        assertEquals(Long.valueOf(9), id.get("tenantKey"));
    }

    @Test
    public void superclassReadOnlyIdRetainsBothIdentifierFlags() {
        final var info = ParserUtil.getBeanInfo(ReadOnlyChild.class);
        assertNotNull(info.getAnnotation(ReadOnlyId.class));
        assertEquals(List.of("accountKey"), info.idPropNameList);
        assertEquals(List.of("accountKey"), info.readOnlyIdPropNameList);
        assertTrue(info.getPropInfo("accountKey").isMarkedAsId);
        assertTrue(info.getPropInfo("accountKey").isMarkedAsReadOnlyId);
        assertEquals(Long.valueOf(7), EntityId.create(new ReadOnlyChild()).get("accountKey"));
    }

    @Test
    public void interfaceIdentifierAnnotationsContributeMetadata() {
        final var info = ParserUtil.getBeanInfo(InterfaceBean.class);
        assertEquals(Set.of("accountKey", "tenantKey"), Set.copyOf(info.idPropNameList));
        assertEquals(List.of("tenantKey"), info.readOnlyIdPropNameList);
        assertTrue(info.getPropInfo("accountKey").isMarkedAsId);
        assertFalse(info.getPropInfo("accountKey").isMarkedAsReadOnlyId);
        assertTrue(info.getPropInfo("tenantKey").isMarkedAsReadOnlyId);
    }

    @Test
    public void subclassDeclarationReplacesTheSameAnnotationOnItsSuperclass() {
        final var idInfo = ParserUtil.getBeanInfo(CompositeOverride.class);
        assertEquals(List.of("replacementKey"), idInfo.idPropNameList);
        assertFalse(idInfo.getPropInfo("accountKey").isMarkedAsId);
        final var readOnlyInfo = ParserUtil.getBeanInfo(ReadOnlyOverride.class);
        assertEquals(List.of("replacementKey"), readOnlyInfo.idPropNameList);
        assertEquals(List.of("replacementKey"), readOnlyInfo.readOnlyIdPropNameList);
        assertFalse(readOnlyInfo.getPropInfo("accountKey").isMarkedAsReadOnlyId);
    }

    @Test
    public void idAndReadOnlyIdRemainIndependentAnnotationKinds() {
        final var info = ParserUtil.getBeanInfo(MixedChild.class);
        assertEquals(Set.of("accountKey", "replacementKey"), Set.copyOf(info.idPropNameList));
        assertEquals(List.of("accountKey"), info.readOnlyIdPropNameList);
        assertFalse(info.getPropInfo("replacementKey").isMarkedAsReadOnlyId);
        assertTrue(info.getPropInfo("accountKey").isMarkedAsReadOnlyId);
    }

    @Test
    public void emptySubclassDeclarationStillFailsValidation() {
        assertThrows(IllegalArgumentException.class, () -> ParserUtil.getBeanInfo(EmptyOverride.class));
    }
}
