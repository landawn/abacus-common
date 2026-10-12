package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;

@Tag("unit")
class KryoPrimitiveAliasReviewTest extends TestBase {
    @Test
    void explicitWrapperIdsKeepPrimitiveAliasesAndTypedReads() {
        Class<?>[] primitives = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class };
        Object[] values = { true, (byte) 12, 'x', (short) 23, 34, 45L, 5.5f, 6.5d };

        for (int i = 0; i < primitives.length; i++) {
            for (boolean useOriginalId : new boolean[] { true, false }) {
                KryoParser parser = new KryoParser();
                Kryo initial = parser.createKryo();
                int id;
                try {
                    id = useOriginalId ? initial.getRegistration(primitives[i]).getId() : 1_972_001 + i;
                } finally {
                    parser.recycle(initial);
                }
                parser.register(values[i].getClass(), id);
                assertEquals(values[i], parser.deserialize(parser.serialize(values[i]), null, primitives[i]));
                assertEquals(values[i], parser.decode(parser.encode(values[i])));

                Kryo configured = parser.createKryo();
                try {
                    assertSame(configured.getRegistration(primitives[i]), configured.getRegistration(values[i].getClass()));
                    assertEquals(id, configured.getRegistration(primitives[i]).getId());
                } finally {
                    parser.recycle(configured);
                }
            }
        }
    }

    @Test
    void explicitWrapperSerializerAlsoAppliesToPrimitiveTarget() {
        KryoParser parser = new KryoParser();
        parser.register(Integer.class, new Serializer<Integer>() {
            @Override
            public void write(Kryo kryo, Output output, Integer value) {
                output.writeInt(value + 100);
            }

            @Override
            public Integer read(Kryo kryo, Input input, Class<? extends Integer> type) {
                return input.readInt() - 100;
            }
        }, 1_972_020);

        assertEquals(123, parser.deserialize(parser.serialize(123), null, int.class));
        assertEquals(123, (int) parser.decode(parser.encode(123)));
    }
}
