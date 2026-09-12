package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Pair;

public class HolderTypeTest extends TestBase {

    // R05 review (2026-09-08): OptionalType, JdkOptionalType and NullableType were given the Object-slot runtime
    // dispatch in appendTo; HolderType - the fourth single-slot wrapper, whose serializeTo was rewritten onto
    // AbstractTupleType.serializeSlot in the same commit - was missed, so a Holder<Object> holding a map or a
    // collection appended ObjectType's JSON stringOf form ({"k": 1}) instead of the toString()-style form ({k:1})
    // its own serializeTo, its three siblings, and the bare value's handler all produce.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void reviewFixes20260908_appendToObjectSlotUsesRuntimeType() throws IOException {
        final Type objectSlot = Type.of("Holder<Object>");
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        map.put("s", "v");
        final List<Object> list = new ArrayList<>(Arrays.asList(1, "a"));

        assertEquals("{k:1, s:v}", appendToString(objectSlot, Holder.of(map)));
        assertEquals("[1, a]", appendToString(objectSlot, Holder.of(list)));

        // an Object slot must append exactly what the bare value's own handler appends
        for (final Object value : new Object[] { map, list, new int[] { 1, 2 }, 7, 1.5d, true, "q", Pair.of(1, "a") }) {
            final Type runtimeType = Type.of(value.getClass());

            assertEquals(appendToString(runtimeType, value), appendToString(objectSlot, Holder.of(value)), "value " + value);
        }

        // ... and it agrees with the three sibling single-slot wrappers on the same value
        for (final String wrapper : new String[] { "Optional<Object>", "Nullable<Object>", "JdkOptional<Object>" }) {
            assertEquals(appendToString(objectSlot, Holder.of(map)), appendToString(Type.of(wrapper), wrap(wrapper, map)), wrapper);
        }

        // a declared (non-Object) element type keeps its own handler
        assertEquals("{k:1, s:v}", appendToString(Type.of("Holder<Map<String, Object>>"), Holder.of(map)));
        assertEquals("3", appendToString(Type.of("Holder<Integer>"), Holder.of(3)));

        // null holder / null value still write the null literal
        assertEquals("null", appendToString(objectSlot, Holder.of(null)));
        assertEquals("null", appendToString(objectSlot, null));
    }

    private static Object wrap(final String wrapper, final Object value) {
        if (wrapper.startsWith("JdkOptional")) {
            return java.util.Optional.of(value);
        } else if (wrapper.startsWith("Nullable")) {
            return com.landawn.abacus.util.u.Nullable.of(value);
        }

        return com.landawn.abacus.util.u.Optional.of(value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String appendToString(final Type type, final Object value) throws IOException {
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, value);
        return sb.toString();
    }
}
