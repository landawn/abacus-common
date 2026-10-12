package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import com.landawn.abacus.TestBase;

@Tag("unit")
class KryoPoolFailureReviewTest extends TestBase {
    @Test
    void encodeReturnsBorrowedOutputWhenRegistrationValidationFails() {
        withConflictingRegistration(parser -> {
            Output borrowed = KryoParser.createOutput();
            KryoParser.recycle(borrowed);
            assertThrows(IllegalArgumentException.class, () -> parser.encode("value"));
            Output next = KryoParser.createOutput();
            try {
                assertSame(borrowed, next);
            } finally {
                KryoParser.recycle(next);
            }
        });
    }

    @Test
    void decodeReturnsBorrowedInputWhenRegistrationValidationFails() {
        withConflictingRegistration(parser -> {
            Input borrowed = KryoParser.createInput();
            KryoParser.recycle(borrowed);
            assertThrows(IllegalArgumentException.class, () -> parser.decode(new byte[] { 0 }));
            Input next = KryoParser.createInput();
            try {
                assertSame(borrowed, next);
            } finally {
                KryoParser.recycle(next);
            }
        });
    }

    private static void withConflictingRegistration(java.util.function.Consumer<KryoParser> action) {
        KryoParser parser = new KryoParser();
        parser.register(LocalRegistration.class, 1_972_030);
        try {
            // A later global registration is validated when a parser next borrows its Kryo instance.
            ParserFactory.registerKryo(GlobalRegistration.class, 1_972_030);
            action.accept(parser);
        } finally {
            synchronized (ParserFactory._kryoRegistrationLock) {
                ParserFactory._kryoClassIdMap.remove(GlobalRegistration.class);
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }

    public static class LocalRegistration {
    }

    public static class GlobalRegistration {
    }
}
